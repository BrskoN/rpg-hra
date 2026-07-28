package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.WorldState
import com.example.ui.theme.AgedBone
import com.example.ui.theme.AgedGold
import com.example.ui.theme.DarkInk
import com.example.ui.theme.MedievalCrimson

import com.example.data.AppLanguage
import com.example.data.getLocalizedTitle

@Composable
fun GameOverDialog(
    worldState: WorldState,
    onRestart: () -> Unit
) {
    val isSlovak = worldState.selectedLanguage == AppLanguage.SLOVAK

    Dialog(onDismissRequest = { /* Force explicit decision via button */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.5.dp, if (worldState.isVictory) AgedGold else MedievalCrimson, RoundedCornerShape(16.dp))
                .testTag("game_over_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AgedBone)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (worldState.isVictory) Icons.Default.EmojiEvents else Icons.Default.SentimentVeryDissatisfied,
                    contentDescription = null,
                    tint = if (worldState.isVictory) AgedGold else MedievalCrimson,
                    modifier = Modifier.height(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (worldState.isVictory) {
                        if (isSlovak) "PANOVNÍCKY VZOSTUP!" else "SOVEREIGN ASCENSION!"
                    } else {
                        if (isSlovak) "VAŠA CESTA SA SKONČILA" else "YOUR QUEST HAS ENDED"
                    },
                    color = if (worldState.isVictory) MedievalCrimson else Color(0xFFB22222),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = worldState.gameOverReason ?: if (isSlovak) "Váš príbeh v kráľovstve sa naplnil." else "Your tale in the kingdom has drawn to a close.",
                    color = DarkInk,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE2D8C8),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isSlovak) "KONEČNÝ ZÁZNAM DEDIČSTVA" else "FINAL LEGACY RECORD",
                            color = MedievalCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isSlovak) "👑 Zvolený Pôvod: ${worldState.activeOrigin.getLocalizedTitle(worldState.selectedLanguage)}" else "👑 Chosen Origin: ${worldState.activeOrigin.title}",
                            color = DarkInk,
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = if (isSlovak) "🪙 Nahromadené Bohatstvo: ${worldState.gold} Zlata" else "🪙 Wealth Amassed: ${worldState.gold} Gold",
                            color = DarkInk,
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = if (isSlovak) "📖 Konečná Kapitola: ${worldState.currentChapter}" else "📖 Final Chapter: ${worldState.currentChapter}",
                            color = DarkInk,
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = if (isSlovak) "🏷️ Získané Znaky: ${worldState.worldFlags.size}" else "🏷️ Flags Triggered: ${worldState.worldFlags.size}",
                            color = DarkInk,
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("restart_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MedievalCrimson,
                        contentColor = Color(0xFFFFF8EE)
                    )
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSlovak) "Začať Nový Príbeh" else "Begin New Origin",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }
    }
}
