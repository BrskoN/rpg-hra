package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.MedievalTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AgedBone
import com.example.ui.theme.AgedGold
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.MedievalCrimson

@Composable
fun NpcPortraitView(
    npcName: String,
    npcTitle: String,
    npcArchetype: String,
    isHoveredByCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "portrait_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isHoveredByCard) 1.06f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val archetypeInfo = getArchetypeDetails(npcArchetype)

    Box(
        modifier = modifier
            .testTag("npc_portrait_frame"),
        contentAlignment = Alignment.Center
    ) {
        if (isHoveredByCard) {
            Surface(
                modifier = Modifier
                    .size(230.dp)
                    .scale(pulseScale),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.35f)
            ) {}
        }

        Surface(
            modifier = Modifier
                .width(210.dp)
                .height(230.dp)
                .scale(if (isHoveredByCard) pulseScale else 1f)
                .shadow(16.dp, RoundedCornerShape(20.dp))
                .border(
                    width = 3.dp,
                    color = if (isHoveredByCard) Color(0xFFFFD700) else AgedGold,
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            color = AgedBone
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .border(1.dp, Color(0xFFA89F95), RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE8E2D9),
                                archetypeInfo.bgTint
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Class Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = archetypeInfo.badgeColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                    ) {
                        Text(
                            text = archetypeInfo.classBadgeText,
                            color = Color(0xFFFFF8EE),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MedievalTitle,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    // Illuminated Initial Capital Frame
                    IlluminatedInitial(
                        letter = npcName.take(1),
                        modifier = Modifier.size(88.dp),
                        fontSize = 44.sp,
                        textColor = archetypeInfo.initialColor,
                        borderColor = AgedGold,
                        backgroundColor = DeepCharcoal
                    )

                    // NPC Name & Title
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = npcName,
                            color = DarkInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MedievalTitle,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = npcTitle,
                            color = MedievalCrimson,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MedievalTitle,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                if (isHoveredByCard) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xD98B0000), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "⚔️",
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "RELEASE TO PLAY",
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MedievalTitle
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ArchetypeDetails(
    val initialColor: Color,
    val classBadgeText: String,
    val badgeColor: Color,
    val bgTint: Color
)

private fun getArchetypeDetails(archetype: String): ArchetypeDetails {
    return when (archetype.uppercase()) {
        "BISHOP", "PRIEST", "MONK" -> ArchetypeDetails(
            initialColor = AntiqueGold,
            classBadgeText = "HOLY SEE",
            badgeColor = Color(0xFF4B0082),
            bgTint = Color(0xFFEADBCE)
        )
        "MERCHANT" -> ArchetypeDetails(
            initialColor = AgedGold,
            classBadgeText = "MERCHANT GUILD",
            badgeColor = Color(0xFF635200),
            bgTint = Color(0xFFE8DFC8)
        )
        "KNIGHT", "GUARD" -> ArchetypeDetails(
            initialColor = MedievalCrimson,
            classBadgeText = "ROYAL GUARD",
            badgeColor = MedievalCrimson,
            bgTint = Color(0xFFE3D3C5)
        )
        "ALCHEMIST" -> ArchetypeDetails(
            initialColor = AntiqueGold,
            classBadgeText = "ALCHEMIST",
            badgeColor = Color(0xFF2E6F40),
            bgTint = Color(0xFFDCE2D8)
        )
        "BANDIT", "OUTLAW", "ROGUE" -> ArchetypeDetails(
            initialColor = MedievalCrimson,
            classBadgeText = "OUTLAW SYNDICATE",
            badgeColor = Color(0xFF4A1A0C),
            bgTint = Color(0xFFE2D1C8)
        )
        "NOBLE", "BARON", "LORD", "COUNT", "DUKE" -> ArchetypeDetails(
            initialColor = AntiqueGold,
            classBadgeText = "HIGH NOBLE",
            badgeColor = Color(0xFF5C0000),
            bgTint = Color(0xFFE8D4D6)
        )
        "MONARCH", "KING", "QUEEN" -> ArchetypeDetails(
            initialColor = AgedGold,
            classBadgeText = "SOVEREIGN MONARCH",
            badgeColor = Color(0xFF8B0000),
            bgTint = Color(0xFFEADBC8)
        )
        "ELDER" -> ArchetypeDetails(
            initialColor = AntiqueGold,
            classBadgeText = "VILLAGE ELDER",
            badgeColor = Color(0xFF4A3A2C),
            bgTint = Color(0xFFE2D8C8)
        )
        else -> ArchetypeDetails(
            initialColor = AntiqueGold,
            classBadgeText = "LOCAL INHABITANT",
            badgeColor = Color(0xFF4A3A2C),
            bgTint = Color(0xFFE2D8C8)
        )
    }
}
