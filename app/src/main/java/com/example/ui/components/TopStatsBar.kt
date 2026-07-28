package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameState
import com.example.data.SocialHierarchy
import com.example.ui.theme.AgedGold
import com.example.ui.theme.InkDark
import com.example.ui.theme.MedievalCrimson
import com.example.ui.theme.ParchmentCard

@Composable
fun TopStatsBar(
    state: GameState,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFactionsDialog by remember { mutableStateOf(false) }
    var showInventoryDialog by remember { mutableStateOf(false) }

    if (showFactionsDialog) {
        FactionsDialog(
            factionsReputation = state.factionReputation,
            onDismiss = { showFactionsDialog = false }
        )
    }

    if (showInventoryDialog) {
        InventoryDialog(
            inventory = state.inventory,
            properties = state.properties,
            totalPassiveGoldIncome = state.totalPassiveGoldIncome,
            onDismiss = { showInventoryDialog = false }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stat_bar"),
        color = MedievalCrimson, // Rich burgundy header banner
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = AgedGold,
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header Row 1: Social Class Badge & Main Quick Stat Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Social Class Rank Badge (Light Parchment Pill)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ParchmentCard,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AgedGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.socialClass.icon,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = state.socialClass.title,
                            color = InkDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.testTag("social_class_text")
                        )
                    }
                }

                // Quick Action Icon Buttons: Factions, Inventory, History
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Factions Button
                    IconButton(
                        onClick = { showFactionsDialog = true },
                        modifier = Modifier.size(34.dp).testTag("factions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Realm Factions",
                            tint = Color(0xFFFFD700)
                        )
                    }

                    // Inventory & Properties Button
                    IconButton(
                        onClick = { showInventoryDialog = true },
                        modifier = Modifier.size(34.dp).testTag("inventory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = "Inventory Relics & Assets",
                            tint = Color(0xFFFFD700)
                        )
                    }

                    // Chronicle History Button
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.size(34.dp).testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View Adventure Chronicle",
                            tint = Color(0xFFFFF8EE)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Gold & Health Stats Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gold Pill with Passive Income Indicator
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF4A0000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Gold",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val passiveText = if (state.totalPassiveGoldIncome > 0) " (+${state.totalPassiveGoldIncome}/t)" else ""
                        Text(
                            text = "${state.gold}g$passiveText",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            modifier = Modifier.testTag("gold_text")
                        )
                    }
                }

                // Health Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF4A0000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6B6B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Health",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.health}/${state.maxHealth}",
                            color = Color(0xFFFFF2DC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            modifier = Modifier.testTag("health_bar")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Social Advancement Progress Bar
            val progressAnimated by animateFloatAsState(
                targetValue = (state.socialProgress.coerceIn(0, 100) / 100f),
                label = "socialProgress"
            )

            val nextRankText = SocialHierarchy.getNextRank(state.socialClass)?.title ?: "Sovereignty Reached"

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rank Progress (${state.socialProgress}%)",
                        color = Color(0xFFF4E3B2),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Next: $nextRankText",
                        color = Color(0xFFFFD700),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF380000))
                        .border(1.dp, AgedGold, CircleShape)
                        .testTag("social_progress_bar")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnimated)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFC59B27), Color(0xFFFFD700))
                                )
                            )
                    )
                }
            }
        }
    }
}
