package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.MedievalTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EventOption
import com.example.ui.theme.AgedGold
import com.example.ui.theme.InkDark
import com.example.ui.theme.LeatherButton
import com.example.ui.theme.LeatherButtonBorder
import com.example.ui.theme.LeatherButtonDisabled
import com.example.ui.theme.MedievalCrimson
import com.example.ui.theme.ParchmentBg

@Composable
fun ActionBar(
    options: List<EventOption>,
    isEnabled: Boolean,
    onOptionSelected: (EventOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ParchmentBg, // Warm Light Parchment footer anchor
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AgedGold),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CHOOSE YOUR PATH",
                color = MedievalCrimson,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MedievalTitle,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            // Render up to 3 choice buttons styled as light leather/parchment strips
            options.take(3).forEachIndexed { index, option ->
                val testTagStr = "choice_button_${index + 1}"

                Button(
                    onClick = { onOptionSelected(option) },
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(
                            width = 1.5.dp,
                            color = if (isEnabled) LeatherButtonBorder else Color(0xFFC8B9A6),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag(testTagStr),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeatherButton, // Light leather strip
                        contentColor = InkDark,
                        disabledContainerColor = LeatherButtonDisabled,
                        disabledContentColor = Color(0xFF8C7B6B)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 1.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Choice Tag Badge (Crimson leather tag with Gold text)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isEnabled) MedievalCrimson else Color(0xFF8A7969)
                        ) {
                            Text(
                                text = option.tag.ifBlank { "${index + 1}" },
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Choice Description Text in Deep Ink
                        Text(
                            text = option.text,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MedievalTitle,
                            color = if (isEnabled) InkDark else Color(0xFF7C6C5E),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select option",
                            tint = if (isEnabled) MedievalCrimson else Color(0xFF8A7969),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
