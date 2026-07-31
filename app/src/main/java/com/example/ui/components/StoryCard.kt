package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.MedievalTitle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EventResponse
import com.example.data.GameState
import com.example.ui.theme.AgedGold
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.MedievalCrimson
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentCardBorder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoryCard(
    event: EventResponse?,
    state: GameState,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .border(2.5.dp, AgedGold, RoundedCornerShape(16.dp))
            .testTag("story_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ParchmentCard // Warm Light Parchment (#FDF8EB)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, ParchmentCardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Illuminated Header Badge: Chapter / Turn & AI Chronicler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MedievalCrimson,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.height(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chapter ${state.turnCount}",
                                color = Color(0xFFFFF8EE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MedievalTitle
                            )
                        }
                    }

                    // AI Chronicler Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MedievalCrimson,
                            modifier = Modifier.height(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Royal Chronicler",
                            color = InkMedium,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MedievalCrimson,
                            strokeWidth = 3.5.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Consulting the Royal Chronicler...",
                            color = MedievalCrimson,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic,
                            fontFamily = MedievalTitle
                        )
                    }
                } else if (event != null) {
                    // Event Title in Deep Dark Ink
                    Text(
                        text = event.title,
                        color = InkDark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MedievalTitle,
                        modifier = Modifier.testTag("story_title")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Narrative Text in Dark Ink for high contrast readability
                    Text(
                        text = event.text,
                        color = InkDark,
                        fontSize = 16.sp,
                        lineHeight = 25.sp,
                        fontFamily = MedievalTitle,
                        modifier = Modifier.testTag("story_text")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stat Changes Banner (Consequences Box)
                    val statChanges = event.statChanges
                    val hasChanges = statChanges.goldChange != 0 ||
                            statChanges.healthChange != 0 ||
                            statChanges.socialProgressChange != 0 ||
                            !statChanges.statusEffect.isNullOrBlank()

                    if (hasChanges) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF3E7CA), // Warm parchment inlay box
                            border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "CONSEQUENCES OF CHOICE",
                                    color = MedievalCrimson,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (statChanges.goldChange != 0) {
                                        val isPositive = statChanges.goldChange > 0
                                        Text(
                                            text = "${if (isPositive) "+" else ""}${statChanges.goldChange} Gold",
                                            color = if (isPositive) Color(0xFF8B6B00) else Color(0xFFB22222),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (statChanges.healthChange != 0) {
                                        val isPositive = statChanges.healthChange > 0
                                        Text(
                                            text = "${if (isPositive) "+" else ""}${statChanges.healthChange} Health",
                                            color = if (isPositive) Color(0xFF1B6E1B) else Color(0xFFB22222),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (statChanges.socialProgressChange != 0) {
                                        val isPositive = statChanges.socialProgressChange > 0
                                        Text(
                                            text = "${if (isPositive) "+" else ""}${statChanges.socialProgressChange}% Rank",
                                            color = if (isPositive) Color(0xFF1B6E1B) else Color(0xFFB22222),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (!statChanges.statusEffect.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚡ Status Acquired: ${statChanges.statusEffect}",
                                        color = MedievalCrimson,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Active Status Flags (Styled like wax seal tags)
                    if (state.flags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.flags.forEach { flag ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEFE2C2),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ParchmentCardBorder)
                                ) {
                                    Text(
                                        text = "🏷️ $flag",
                                        color = InkDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
