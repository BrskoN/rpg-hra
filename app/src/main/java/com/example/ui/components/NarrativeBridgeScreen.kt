package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AgedBone
import com.example.ui.theme.AgedGold
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.MedievalCrimson

import com.example.data.AppLanguage

@Composable
fun NarrativeBridgeScreen(
    currentChapter: Int,
    bridgeText: String,
    onContinuePath: () -> Unit,
    selectedLanguage: AppLanguage = AppLanguage.SLOVAK,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .border(2.5.dp, AgedGold, RoundedCornerShape(20.dp))
                    .testTag("narrative_bridge_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AgedBone)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Time Passing Crest
                    Text(
                        text = "⏳",
                        fontSize = 36.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MedievalCrimson,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                    ) {
                        Text(
                            text = if (selectedLanguage == AppLanguage.SLOVAK) "KAPITOLA $currentChapter: ROČNÉ OBDOBIA PLYNÚ" else "CHAPTER $currentChapter: SEASONS PASS",
                            color = AgedGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Time-Lapse Narrative Bridge Text
                    Text(
                        text = bridgeText,
                        color = DarkInk,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily.Serif,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Continue Path Button
                    Button(
                        onClick = onContinuePath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_path_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MedievalCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AgedGold)
                    ) {
                        Text(
                            text = if (selectedLanguage == AppLanguage.SLOVAK) "POKRAČOVAŤ V CESTE ➔" else "CONTINUE PATH ➔",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}
