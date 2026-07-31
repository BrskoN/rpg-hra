package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.theme.ManuscriptBody
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StatChanges
import com.example.ui.theme.AgedBone
import com.example.ui.theme.AgedGold
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.MedievalCrimson

import com.example.data.AppLanguage

@Composable
fun ResolutionOverlay(
    resolutionText: String,
    statChanges: StatChanges?,
    onAcceptFate: () -> Unit,
    selectedLanguage: AppLanguage = AppLanguage.SLOVAK,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .border(3.dp, AgedGold, RoundedCornerShape(20.dp))
                    .testTag("resolution_overlay_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AgedBone)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Title
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MedievalCrimson,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                    ) {
                        Text(
                            text = if (selectedLanguage == AppLanguage.SLOVAK) "⚔️ BEZPROSTREDNÝ NÁSLEDOK" else "⚔️ IMMEDIATE CONSEQUENCE",
                            color = AgedGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MedievalTitle,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resolution Narrative Text
                    Text(
                        text = resolutionText,
                        color = DarkInk,
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                        fontFamily = ManuscriptBody,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stat Changes Box
                    if (statChanges != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = DeepCharcoal,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (selectedLanguage == AppLanguage.SLOVAK) "ZMENY ŠTATISTÍK A REPUTÁCIE" else "STAT & REPUTATION SHIFTS",
                                    color = AgedGold,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MedievalTitle
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    if (statChanges.goldChange != 0) {
                                        val isPositive = statChanges.goldChange > 0
                                        Text(
                                            text = "${if (isPositive) "+" else ""}${statChanges.goldChange} 🪙 ${if (selectedLanguage == AppLanguage.SLOVAK) "Zlato" else "Gold"}",
                                            color = if (isPositive) Color(0xFFFFD700) else Color(0xFFFF6B6B),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = MedievalTitle
                                        )
                                    }

                                    if (statChanges.healthChange != 0) {
                                        val isPositive = statChanges.healthChange > 0
                                        Text(
                                            text = "${if (isPositive) "+" else ""}${statChanges.healthChange} ❤️ ${if (selectedLanguage == AppLanguage.SLOVAK) "Zdravie" else "Health"}",
                                            color = if (isPositive) Color(0xFF2ECC71) else Color(0xFFFF6B6B),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = MedievalTitle
                                        )
                                    }
                                }

                                if (statChanges.regionalTensionChange != 0 || statChanges.notorietyChange != 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        if (statChanges.regionalTensionChange != 0) {
                                            val delta = statChanges.regionalTensionChange
                                            Text(
                                                text = "${if (delta > 0) "+" else ""}$delta ⚡ ${if (selectedLanguage == AppLanguage.SLOVAK) "Napätie" else "Tension"}",
                                                color = if (delta > 0) Color(0xFFFF6B6B) else Color(0xFF2ECC71),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = MedievalTitle
                                            )
                                        }

                                        if (statChanges.notorietyChange != 0) {
                                            val delta = statChanges.notorietyChange
                                            Text(
                                                text = "${if (delta > 0) "+" else ""}$delta 👁️ ${if (selectedLanguage == AppLanguage.SLOVAK) "Hľadanosť" else "Notoriety"}",
                                                color = Color(0xFFFFB74D),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = MedievalTitle
                                            )
                                        }
                                    }
                                }

                                if (statChanges.factionChanges.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    statChanges.factionChanges.forEach { (factionName, delta) ->
                                        val isPositive = delta > 0
                                        Text(
                                            text = "${if (isPositive) "+" else ""}$delta Rep ($factionName)",
                                            color = if (isPositive) Color(0xFF2ECC71) else Color(0xFFFF6B6B),
                                            fontSize = 12.sp,
                                            fontFamily = MedievalTitle
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Accept Fate Action Button
                    Button(
                        onClick = onAcceptFate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("accept_fate_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MedievalCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AgedGold)
                    ) {
                        Text(
                            text = if (selectedLanguage == AppLanguage.SLOVAK) "PRIJAŤ OSUD" else "ACCEPT FATE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MedievalTitle,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}
