# Spice event cache — how to fill it

## What this is

`app/src/main/assets/spice_events.json` is a static, curated pool of AI-written "spice" vignettes
(one-off flavor encounters). At runtime the app (`SpiceEventCache.kt` → `GameRepository.getSpiceEvent()`)
picks a random matching entry from this file instead of calling Gemini live. This means the AI call
only happens once, when the pool is generated - not once per player, per session.

The file currently ships as an empty `[]`. Until it's filled in per origin/phase/language,
`getSpiceEvent()` transparently falls back to the old live-API behavior, so nothing breaks - it just
costs nothing once the pool exists.

## Option A — run the generator script yourself

```bash
export GEMINI_API_KEY="your-key-here"
python3 tools/generate_spice_cache.py
```

See `python3 tools/generate_spice_cache.py --help` for flags (per-origin, per-phase, count, etc.).
It merges into the existing file, so it's safe to re-run.

## Option B — ask Google AI Studio to generate it directly

Paste the prompt below into the AI Studio chat for this project. It has live Gemini access and can
edit the file directly, so it can do the whole job without you running anything locally.

---
**Paste this into Google AI Studio:**

> I need you to populate `app/src/main/assets/spice_events.json` with a curated pool of "spice
> event" content for my game. Do not touch any other file, and do not change the schema.
>
> The file is a JSON array. Each entry has this exact shape:
> ```json
> {
>   "origin": "PEASANT",
>   "phase": "PHASE_2",
>   "lang": "SLOVAK",
>   "title": "Short dramatic title",
>   "text": "Concise self-contained setup, 2-3 sentences.",
>   "location": "Village",
>   "npcName": "Name",
>   "npcTitle": "Title",
>   "npcArchetype": "PEASANT",
>   "flag": "SPICE_something_short",
>   "options": [
>     {"id": 1, "text": "Short option text", "tag": "Tag", "cardArchetype": "Peasant_Action"},
>     {"id": 2, "text": "Short option text", "tag": "Tag", "cardArchetype": "Peasant_Action"},
>     {"id": 3, "text": "Short option text", "tag": "Tag", "cardArchetype": "Peasant_Action"}
>   ]
> }
> ```
> Rules:
> - `origin` must be one of: PEASANT, ACOLYTE, GUILD_APPRENTICE, LESSER_NOBLE
> - `phase` must be one of: PHASE_1, PHASE_2, PHASE_3
> - `lang` must be SLOVAK or ENGLISH — generate the `title`/`text`/`options` text in that actual
>   language (Slovak entries must be written in flawless Slovak, not English)
> - `location` must be one of: Forest, Village, Tavern, Castle, Cathedral, Marketplace
> - `npcArchetype` must be one of: PEASANT, MERCHANT, KNIGHT, BISHOP, ALCHEMIST, BANDIT, NOBLE, ELDER
> - `flag` must start with `SPICE_` so it never collides with the main story's flags
> - Each option's `text` is 4-6 words max, `tag` is 1-2 words max
> - These are self-contained one-off side-encounters, NOT part of the main plot — no lasting story
>   obligations, no NPCs from the main storyline
> - Stay in this character's social lane: a peasant never attends a royal court, a lesser noble
>   doesn't command national armies, an acolyte doesn't lead armies, a guild apprentice isn't
>   equal to nobles at court — check `EventDeck.kt`'s `originLaneGuidance` logic if you want the
>   exact per-origin constraints already used elsewhere in the app
> - Match the tone/scale of the existing authored `EventNode` cards in `EventDeck.kt` for the same
>   origin/phase — read a couple of real ones first for reference before generating
>
> Generate 6 entries for EVERY combination of origin × phase × lang (4 origins × 3 phases × 2
> languages = 24 combinations, so 144 entries total). Make sure every entry is a genuinely
> different scenario — no repeats. Append them to the existing array in the file (it currently
> starts as `[]`). Once done, tell me how many entries you added.

---

## After either option — curate before shipping

Open `app/src/main/assets/spice_events.json` and skim it. Delete anything:
- off-tone (too modern, too silly, breaks the dark medieval voice)
- repetitive with another entry
- factually/logically broken (e.g. an option that doesn't make sense for the setup)

This step matters more than the generation step — a bad vignette shown to a player is much more
noticeable than one missing from the pool.

## If you want to automate this with a separate Claude Desktop session

Claude Desktop with browser/computer-use control can drive the Google AI Studio web UI for you.
Give it a prompt along these lines:

> Open Google AI Studio at [your project URL]. Paste the following instruction into its chat and
> send it: [paste the Option B prompt above]. Wait for it to finish editing
> `app/src/main/assets/spice_events.json`, then tell me how many entries it added.

That session would need its own access to your AI Studio project (logged in, project open in the
browser) - it can't reach this repository or this conversation, so hand it the full prompt text
above verbatim, not a summary.
