package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StatChanges
import com.example.ui.theme.AgedGold
import kotlin.math.roundToInt

@Composable
fun FloatingStatPopups(
    statChanges: StatChanges?,
    turnCount: Int,
    modifier: Modifier = Modifier
) {
    if (statChanges == null) return

    val offsetY = remember(turnCount) { Animatable(0f) }
    val alpha = remember(turnCount) { Animatable(1f) }

    LaunchedEffect(turnCount) {
        offsetY.snapTo(0f)
        alpha.snapTo(1f)

        // Float upwards while fading out
        offsetY.animateTo(
            targetValue = -90f,
            animationSpec = tween(durationMillis = 1800, easing = FastOutLinearInEasing)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
    }

    if (alpha.value > 0.05f) {
        Column(
            modifier = modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (statChanges.goldChange != 0) {
                    val isPositive = statChanges.goldChange > 0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF380000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                    ) {
                        Text(
                            text = "${if (isPositive) "+" else ""}${statChanges.goldChange} Gold 🪙",
                            color = if (isPositive) Color(0xFFFFD700) else Color(0xFFFF6B6B),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (statChanges.healthChange != 0) {
                    val isPositive = statChanges.healthChange > 0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF380000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPositive) Color(0xFF2ECC71) else Color(0xFFFF4D4D))
                    ) {
                        Text(
                            text = "${if (isPositive) "+" else ""}${statChanges.healthChange} Health ❤️",
                            color = if (isPositive) Color(0xFF2ECC71) else Color(0xFFFF4D4D),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (statChanges.socialProgressChange != 0) {
                    val isPositive = statChanges.socialProgressChange > 0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF380000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                    ) {
                        Text(
                            text = "${if (isPositive) "+" else ""}${statChanges.socialProgressChange}% Rank 👑",
                            color = Color(0xFFF4E3B2),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Faction Changes Row
            if (statChanges.factionChanges.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    statChanges.factionChanges.forEach { (faction, change) ->
                        val isPositive = change > 0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2A0808),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPositive) Color(0xFF2ECC71) else Color(0xFFFF4D4D))
                        ) {
                            Text(
                                text = "${if (isPositive) "+" else ""}$change $faction Rep",
                                color = if (isPositive) Color(0xFF2ECC71) else Color(0xFFFF4D4D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Item or Asset Gained Popups
            if (statChanges.itemGained != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E3A1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2ECC71))
                ) {
                    Text(
                        text = "🎒 Item Gained: ${statChanges.itemGained.name}",
                        color = Color(0xFFE8F5E9),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (statChanges.assetGained != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF3A2E1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                ) {
                    Text(
                        text = "🏰 Property Gained: ${statChanges.assetGained.name}",
                        color = Color(0xFFFFF3E0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (!statChanges.statusEffect.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF8B0000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                ) {
                    Text(
                        text = "⚡ Status: ${statChanges.statusEffect}",
                        color = Color(0xFFFFF8EE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
