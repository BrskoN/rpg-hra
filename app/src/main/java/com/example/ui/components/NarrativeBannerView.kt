package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AgedBone
import com.example.ui.theme.AgedGold
import com.example.ui.theme.DarkInk
import com.example.ui.theme.MedievalCrimson

@Composable
fun NarrativeBannerView(
    eventTitle: String?,
    storyText: String?,
    turnCount: Int,
    isLoading: Boolean,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = MedievalCrimson,
                        strokeWidth = 3.dp,
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "The Chronicler weaves your fate...",
                        color = MedievalCrimson,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif
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
                                fontFamily = FontFamily.Serif,
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
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.testTag("story_text")
                    )
                }
            }
        }
    }
}
