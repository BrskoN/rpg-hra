#!/usr/bin/env python3
"""
Offline generator for app/src/main/assets/spice_events.json.

This is a ONE-TIME (or occasional, developer-run) tool - it is NOT part of the Android app and is
never bundled into the APK. It calls the Gemini API a bounded number of times up front to build a
curated pool of "spice" vignettes, then writes them to a static JSON file that ships inside the app.
At runtime the app (SpiceEventCache.kt / GameRepository.getSpiceEvent) reads that file and picks a
random matching entry - zero API cost and zero network dependency per player, per session.

Usage:
    export GEMINI_API_KEY="your-key-here"
    python3 tools/generate_spice_cache.py                       # generate for everything
    python3 tools/generate_spice_cache.py --origins PEASANT      # just one origin
    python3 tools/generate_spice_cache.py --phases PHASE_2 --count 10
    python3 tools/generate_spice_cache.py --langs SLOVAK

After it finishes, OPEN app/src/main/assets/spice_events.json and read through the new entries -
delete anything low-quality, off-tone, or repetitive before you ship. This script does not curate
for you; it only proposes a larger pool than you'd want to hand-write.

The script MERGES into the existing file (matched by origin+phase+lang+title) rather than
overwriting it, so it is safe to re-run incrementally as you add origins or want more variety.
"""

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

CANDIDATE_MODELS = [
    "gemini-2.5-flash",
    "gemini-2.0-flash",
    "gemini-1.5-flash",
    "gemini-flash-latest",
]

ORIGINS = ["PEASANT", "ACOLYTE", "GUILD_APPRENTICE", "LESSER_NOBLE"]
PHASES = ["PHASE_1", "PHASE_2", "PHASE_3"]
LANGS = ["SLOVAK", "ENGLISH"]

LOCATIONS = ["Forest", "Village", "Tavern", "Castle", "Cathedral", "Marketplace"]
NPC_ARCHETYPES = ["PEASANT", "MERCHANT", "KNIGHT", "BISHOP", "ALCHEMIST", "BANDIT", "NOBLE", "ELDER"]

# Mirrors GeminiApiService.originLaneGuidance() in the Kotlin app - keep these in sync so cached
# and live-generated vignettes read consistently.
ORIGIN_LANE_GUIDANCE = {
    "PEASANT": "This character is a lowly peasant. Plausible settings: village, farm, forest, tavern, the fringes of a market. Plausible company: other peasants, local clergy, huntsmen, bailiffs, traveling merchants, bandits. NEVER place them at a royal court, in a king's presence, or handling matters of state - they are far too lowborn for that.",
    "ACOLYTE": "This character is a monastery acolyte. Plausible settings: monastery, cathedral, village, crypt. Plausible company: monks, priests, the bishop, pilgrims, common villagers. NEVER place them commanding armies or presiding over royal court politics.",
    "GUILD_APPRENTICE": "This character is a city guild apprentice. Plausible settings: workshop, marketplace, tavern, city gate, docks. Plausible company: guildmasters, merchants, city guards, moneylenders, underworld contacts. NEVER place them in rural farm life or in a royal throne room as an equal to nobles.",
    "LESSER_NOBLE": "This character is an impoverished lesser noble with a small ancestral keep. Plausible settings: their own keep, neighboring estates, a county town, occasionally the fringes of a royal court (as a minor attendee, never as an equal to the king). Plausible company: other minor nobles, knights, tax collectors, tenants. Keep the scale modest - they do not command national armies.",
}

ORIGIN_TITLES = {
    "PEASANT": "Peasant",
    "ACOLYTE": "Acolyte",
    "GUILD_APPRENTICE": "Guild Apprentice",
    "LESSER_NOBLE": "Lesser Noble",
}

# Real authored cards pulled straight from EventDeck.kt, one per origin/phase, used as few-shot
# tone/scale grounding - the same purpose as EventDeck.fewShotSamples() at runtime, just supplied
# by hand here since this script does not parse the Kotlin file.
FEW_SHOT_EXAMPLES = {
    ("PEASANT", "PHASE_1"): ("The Winter Tithe and the Rigged Measure",
        "The manor official measures your grain with his own, tampered bushel. If you hand over what he demands, your family starves before spring."),
    ("PEASANT", "PHASE_2"): ("The Wandering Flagellant Procession",
        "A procession of bloodied, hooded men has arrived in the settlement. They scourge themselves with nail-studded straps, singing psalms, blaming the local bailiff for bringing God's wrath and plague upon the land."),
    ("PEASANT", "PHASE_3"): ("The Plague Doctor with the Bird Mask",
        "A rider in a leather coat and beaked bird mask has appeared on the horizon. He burns healing herbs at the village entrance and declares a strict quarantine - no one may leave the estate."),
    ("ACOLYTE", "PHASE_1"): ("The Forbidden Manuscript Beneath the Floor",
        "Sweeping the vaulted library, you find a loose tile. Beneath it lies a dust-caked parchment volume bound in human skin, marked with heretical symbols. Footsteps of Bishop Alistair echo in the corridor."),
    ("ACOLYTE", "PHASE_2"): ("The Arrival of the Grand Inquisitor",
        "A black carriage bearing Inquisitor Malachai arrives in the courtyard. He hunts for traces of black magic and free thought, questioning every brother in turn."),
    ("ACOLYTE", "PHASE_3"): ("The Inquisitorial Trial and the Torture Chamber",
        "You stand shackled in the cathedral's underground cell. The Inquisitor prepares heated tongs. He demands the names of your accomplices."),
    ("GUILD_APPRENTICE", "PHASE_1"): ("The Missing Silk Shipment",
        "Guildmaster Corvus has discovered expensive Oriental goods missing from the warehouse. City guards pound on the workshop door. If no culprit is found, blame falls on you as the youngest apprentice."),
    ("GUILD_APPRENTICE", "PHASE_2"): ("The Golden Contract for the Manor Court",
        "The manor chamberlain seeks an exclusive supplier of luxury goods for the castle. Guildmaster Corvus lies feverish, and has entrusted you with negotiating the terms."),
    ("GUILD_APPRENTICE", "PHASE_3"): ("The Inquisitorial Court Raid on the City Hall",
        "The Inquisitor and the city judge have seized the guild ledgers. They found your name tied to smuggling, usury, and coin debasement."),
    ("LESSER_NOBLE", "PHASE_1"): ("The Crumbling Keep and the Ducal Levy",
        "The Duke's tax collector arrives with three men-at-arms at your courtyard. He demands 40 gold in annual liege tax, or two armed horses for the ducal guard. Your treasury is empty and the tower roof is failing."),
    ("LESSER_NOBLE", "PHASE_2"): ("The Secret Messenger of the Fled Count",
        "A rider in a dark cloak arrives at your keep at midnight, bearing the seal of a rebel noble league plotting a coup against the king. They offer you the title of Baron if you open your gates to their army."),
    ("LESSER_NOBLE", "PHASE_3"): ("The Siege of the Ancestral Keep",
        "The royal army - or Baron Ironhand's own host - has surrounded your keep. Catapults batter the walls and a battering ram splinters the gate."),
}


def build_prompt(origin: str, phase: str, lang: str) -> str:
    is_slovak = lang == "SLOVAK"
    example_title, example_text = FEW_SHOT_EXAMPLES[(origin, phase)]

    lines = []
    if is_slovak:
        lines.append("CRITICAL LANGUAGE RULE: Write the ENTIRE response in flawless, high-register Slovak (Slovencina). Use rich medieval vocabulary. Do not output any English text.\n")
    else:
        lines.append("CRITICAL LANGUAGE RULE: Write the ENTIRE response in ENGLISH.\n")

    lines.append(
        "You are a ruthless medieval Game Master writing ONE small, self-contained bonus vignette "
        "to add per-playthrough variety to an otherwise fixed storyline. This is NOT part of the "
        "main plot - it is a short, flavorful side-encounter with no lasting story obligations.\n"
    )
    lines.append(ORIGIN_LANE_GUIDANCE[origin] + "\n")
    lines.append(
        f"REFERENCE TONE - this is a REAL authored card from this exact storyline, phase {phase} "
        f"(do NOT reuse its plot, only match its tone, scale, and rough length):\n"
        f"- \"{example_title}\": {example_text}\n"
    )
    lines.append(f"CURRENT STATE: Player class: {ORIGIN_TITLES[origin]}. Story phase: {phase}.\n")
    lines.append("DIRECTIVES:")
    lines.append("1. Invent a brand new minor NPC and a small, self-contained situation appropriate to this character's exact social station - nothing that requires the wider story to change.")
    lines.append("2. Any new world-flag you introduce MUST be prefixed with 'SPICE_' so it never collides with the main storyline's flags.")
    lines.append("3. CONCISE CARD CHOICES: each option 'text' MUST be 4-6 words max, 'tag' MUST be 1-2 words max.")
    lines.append(f"4. location MUST be one of: {', '.join(repr(l) for l in LOCATIONS)} - choose only ones plausible for this character.")
    lines.append(f"5. npcArchetype MUST be one of: {', '.join(repr(a) for a in NPC_ARCHETYPES)}.")
    lines.append("6. Return ONLY valid JSON, no markdown backticks:")
    lines.append("{")
    lines.append('  "title": "Short dramatic title",')
    lines.append('  "text": "Concise self-contained setup (max 2-3 sentences).",')
    lines.append('  "location": "Village",')
    lines.append('  "npcName": "Name",')
    lines.append('  "npcTitle": "Title",')
    lines.append('  "npcArchetype": "PEASANT",')
    lines.append('  "flag": "SPICE_something_short",')
    lines.append('  "options": [')
    lines.append('    {"id": 1, "text": "Short option text", "tag": "Tag", "cardArchetype": "Peasant_Action"},')
    lines.append('    {"id": 2, "text": "Short option text", "tag": "Tag", "cardArchetype": "Peasant_Action"},')
    lines.append('    {"id": 3, "text": "Short option text", "tag": "Tag", "cardArchetype": "Peasant_Action"}')
    lines.append("  ]")
    lines.append("}")
    return "\n".join(lines)


def call_gemini(api_key: str, prompt: str, temperature: float = 0.95) -> str | None:
    body = json.dumps({
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "responseMimeType": "application/json",
            "temperature": temperature,
            "maxOutputTokens": 512,
        },
    }).encode("utf-8")

    for model in CANDIDATE_MODELS:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
        req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"}, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                payload = json.loads(resp.read().decode("utf-8"))
            candidates = payload.get("candidates", [])
            if not candidates:
                continue
            parts = candidates[0].get("content", {}).get("parts", [])
            if not parts:
                continue
            text = parts[0].get("text", "").strip()
            text = re.sub(r"^```json", "", text)
            text = re.sub(r"^```", "", text)
            text = re.sub(r"```$", "", text)
            return text.strip()
        except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, json.JSONDecodeError) as exc:
            print(f"  [warn] model {model} failed: {exc}", file=sys.stderr)
            continue
    return None


def validate_entry(parsed: dict) -> bool:
    required = ["title", "text", "location", "npcName", "npcTitle", "npcArchetype", "options"]
    if not all(k in parsed for k in required):
        return False
    if parsed["location"] not in LOCATIONS:
        return False
    if parsed["npcArchetype"] not in NPC_ARCHETYPES:
        return False
    options = parsed.get("options")
    if not isinstance(options, list) or len(options) != 3:
        return False
    for opt in options:
        if not all(k in opt for k in ("id", "text", "tag", "cardArchetype")):
            return False
    return True


def entry_key(entry: dict) -> tuple:
    return (entry["origin"], entry["phase"], entry["lang"], entry["title"])


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api-key", default=os.environ.get("GEMINI_API_KEY"), help="Gemini API key (or set GEMINI_API_KEY env var)")
    parser.add_argument("--origins", default=",".join(ORIGINS), help="Comma-separated origins to generate for")
    parser.add_argument("--phases", default=",".join(PHASES), help="Comma-separated phases to generate for")
    parser.add_argument("--langs", default=",".join(LANGS), help="Comma-separated languages to generate for")
    parser.add_argument("--count", type=int, default=6, help="How many variants to generate per origin/phase/lang combo")
    parser.add_argument("--out", default="app/src/main/assets/spice_events.json", help="Output JSON path")
    parser.add_argument("--sleep", type=float, default=1.0, help="Seconds to sleep between API calls (rate limiting)")
    args = parser.parse_args()

    if not args.api_key:
        print("ERROR: no API key. Pass --api-key or set GEMINI_API_KEY.", file=sys.stderr)
        sys.exit(1)

    origins = [o.strip() for o in args.origins.split(",") if o.strip()]
    phases = [p.strip() for p in args.phases.split(",") if p.strip()]
    langs = [l.strip() for l in args.langs.split(",") if l.strip()]

    existing: list[dict] = []
    if os.path.exists(args.out):
        with open(args.out, "r", encoding="utf-8") as f:
            try:
                existing = json.load(f)
            except json.JSONDecodeError:
                existing = []

    by_key = {entry_key(e): e for e in existing if all(k in e for k in ("origin", "phase", "lang", "title"))}

    total_new = 0
    total_failed = 0

    for origin in origins:
        for phase in phases:
            for lang in langs:
                print(f"Generating {args.count}x for {origin} / {phase} / {lang}...")
                prompt = build_prompt(origin, phase, lang)
                for i in range(args.count):
                    raw = call_gemini(args.api_key, prompt)
                    time.sleep(args.sleep)
                    if raw is None:
                        total_failed += 1
                        print(f"  [{i + 1}/{args.count}] FAILED (no response)")
                        continue
                    try:
                        parsed = json.loads(raw)
                    except json.JSONDecodeError:
                        total_failed += 1
                        print(f"  [{i + 1}/{args.count}] FAILED (invalid JSON)")
                        continue
                    if not validate_entry(parsed):
                        total_failed += 1
                        print(f"  [{i + 1}/{args.count}] FAILED (missing/invalid fields)")
                        continue

                    entry = {
                        "origin": origin,
                        "phase": phase,
                        "lang": lang,
                        "title": parsed["title"],
                        "text": parsed["text"],
                        "location": parsed["location"],
                        "npcName": parsed["npcName"],
                        "npcTitle": parsed["npcTitle"],
                        "npcArchetype": parsed["npcArchetype"],
                        "flag": parsed.get("flag", ""),
                        "options": parsed["options"],
                    }
                    key = entry_key(entry)
                    if key in by_key:
                        print(f"  [{i + 1}/{args.count}] duplicate title, skipped: {entry['title']}")
                        continue
                    by_key[key] = entry
                    total_new += 1
                    print(f"  [{i + 1}/{args.count}] OK: {entry['title']}")

    merged = list(by_key.values())
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(merged, f, ensure_ascii=False, indent=2)

    print(f"\nDone. {total_new} new entries added, {total_failed} failed calls, {len(merged)} total entries in {args.out}.")
    print("IMPORTANT: read through the new entries and delete anything low-quality or off-tone before you ship.")


if __name__ == "__main__":
    main()
