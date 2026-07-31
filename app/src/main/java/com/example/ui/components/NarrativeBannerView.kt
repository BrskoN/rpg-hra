package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.MedievalTitle
import com.example.ui.theme.ManuscriptBody
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.ui.theme.AgedBone
import com.example.ui.theme.AgedGold
import com.example.ui.theme.DarkInk
import com.example.ui.theme.MedievalCrimson
import kotlinx.coroutines.delay

@Composable
fun NarrativeBannerView(
    eventTitle: String?,
    storyText: String?,
    turnCount: Int,
    isLoading: Boolean,
    selectedLanguage: AppLanguage = AppLanguage.SLOVAK,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("story_card"),
        shape = RoundedCornerShape(18.dp),
        color = AgedBone,
        border = androidx.compose.foundation.BorderStroke(2.dp, AgedGold),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            if (isLoading) {
                var messageIndex by remember { mutableIntStateOf(0) }
                val loadingMessages = if (selectedLanguage == AppLanguage.SLOVAK) listOf(
                    "📜 Kráľovský kronikár píše váš nový osud...",
                    "🏰 Špióni prinášajú správy z okolitých panstiev...",
                    "⚔️ Miestni páni rešpektujú vaše rozhodnutie...",
                    "👑 Kráľovský dvor vyhodnocuje následky..."
                ) else listOf(
                    "📜 The Royal Chronicler weaves your fate...",
                    "🏰 Spies bring news from neighboring fiefs...",
                    "⚔️ Local lords react to your choices...",
                    "👑 The Royal Court calculates consequences..."
                )

                LaunchedEffect(isLoading) {
                    while (isLoading) {
                        delay(1400)
                        messageIndex = (messageIndex + 1) % loadingMessages.size
                    }
                }

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = loadingMessages[messageIndex],
                        color = MedievalCrimson,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontFamily = MedievalTitle,
                        modifier = Modifier.alpha(pulseAlpha)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(4.dp),
                        color = MedievalCrimson,
                        trackColor = AgedGold.copy(alpha = 0.3f)
                    )
                }
            } else if (storyText != null) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MedievalCrimson
                        ) {
                            Text(
                                text = "Turn $turnCount",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (!eventTitle.isNullOrBlank()) {
                            Text(
                                text = eventTitle,
                                color = DarkInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MedievalTitle,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("story_title")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = storyText,
                        color = DarkInk,
                        fontSize = 15.5.sp,
                        lineHeight = 23.sp,
                        fontFamily = ManuscriptBody,
                        modifier = Modifier.testTag("story_text")
                    )
                }
            }
        }
    }
}

