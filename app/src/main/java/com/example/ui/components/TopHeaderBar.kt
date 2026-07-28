package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Faction
import com.example.ui.theme.AgedGold
import com.example.ui.theme.DarkCanvasSurface
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.LightInk
import com.example.ui.theme.MedievalCrimson

import com.example.data.AppLanguage
import com.example.data.getLocalizedName

@Composable
fun TopHeaderBar(
    originTitle: String,
    gold: Int,
    health: Int,
    maxHealth: Int,
    factions: Map<Faction, Int>,
    regionalTension: Int = 20,
    notoriety: Int = 10,
    environmentName: String = "Harsh Winter",
    selectedLanguage: AppLanguage = AppLanguage.SLOVAK,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFactionsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCanvasSurface)
            .border(width = 1.dp, color = AgedGold.copy(alpha = 0.5f))
    ) {
        // Main Compact Header Row (Height <= 60dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Origin Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2C241B),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold),
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = originTitle,
                        color = AgedGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Gold Stat Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF262010),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold.copy(alpha = 0.6f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("gold_stat_badge")
                ) {
                    Text(text = "🪙", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$gold",
                        color = Color(0xFFFFD700),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            // Health Stat Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2E1212),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedievalCrimson.copy(alpha = 0.8f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("health_stat_badge")
                ) {
                    Text(text = "❤️", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$health/$maxHealth",
                        color = Color(0xFFFF8888),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            // Language Toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E1E1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold.copy(alpha = 0.7f)),
                modifier = Modifier.clickable {
                    val nextLang = if (selectedLanguage == AppLanguage.SLOVAK) AppLanguage.ENGLISH else AppLanguage.SLOVAK
                    onLanguageSelected(nextLang)
                }
            ) {
                Text(
                    text = if (selectedLanguage == AppLanguage.SLOVAK) "🇸🇰 SK" else "🇬🇧 EN",
                    color = AgedGold,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                )
            }

            // Expandable Factions Toggle Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isFactionsExpanded) MedievalCrimson else Color(0xFF222222),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold),
                modifier = Modifier
                    .testTag("open_factions_button")
                    .clickable { isFactionsExpanded = !isFactionsExpanded }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (selectedLanguage == AppLanguage.SLOVAK) "🛡️ FRAKCIE ${if (isFactionsExpanded) "▲" else "▼"}" else "🛡️ FACTIONS ${if (isFactionsExpanded) "▲" else "▼"}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }

        // Expandable Faction Reputation Panel
        AnimatedVisibility(
            visible = isFactionsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepCharcoal)
                    .border(width = 1.dp, color = AgedGold)
                    .padding(10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (selectedLanguage == AppLanguage.SLOVAK) "PROSTREDIE A TLAK RÍŠE" else "ENVIRONMENT & REALM PRESSURES",
                        color = AgedGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "❄️ $environmentName",
                            color = Color(0xFF81D4FA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = if (selectedLanguage == AppLanguage.SLOVAK) "⚡ Napätie: $regionalTension/100" else "⚡ Tension: $regionalTension/100",
                            color = if (regionalTension > 70) MedievalCrimson else AgedGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = if (selectedLanguage == AppLanguage.SLOVAK) "👁️ Hľadanosť: $notoriety/100" else "👁️ Notoriety: $notoriety/100",
                            color = Color(0xFFFFB74D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (selectedLanguage == AppLanguage.SLOVAK) "REPUTÁCIA FRAKCIÍ RÍŠE" else "REALM FACTION REPUTATION",
                        color = AgedGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Faction.values().forEach { faction ->
                        val repValue = factions[faction] ?: 50
                        val factionName = faction.getLocalizedName(selectedLanguage)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${faction.iconSymbol} $factionName",
                                color = LightInk,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.width(110.dp)
                            )
                            LinearProgressIndicator(
                                progress = { repValue / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp),
                                color = when {
                                    repValue >= 70 -> Color(0xFF2ECC71)
                                    repValue >= 40 -> AgedGold
                                    else -> MedievalCrimson
                                },
                                trackColor = Color(0xFF2A2A2A)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$repValue",
                                color = LightInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.width(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
