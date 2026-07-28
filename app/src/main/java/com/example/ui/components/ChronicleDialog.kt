package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ChronicleEntry
import com.example.ui.theme.AgedGold
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.MedievalCrimson
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentCardBorder

@Composable
fun ChronicleDialog(
    chronicle: List<ChronicleEntry>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.5.dp, AgedGold, RoundedCornerShape(16.dp))
                .testTag("chronicle_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ParchmentCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MedievalCrimson
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adventure Chronicle",
                            color = InkDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = InkMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (chronicle.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your chronicle is currently blank. Take your first action to carve your legacy!",
                            color = InkMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Serif
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chronicle) { entry ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF3E7CA),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ParchmentCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Turn ${entry.turn}: ${entry.eventTitle}",
                                            color = MedievalCrimson,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Serif
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Chosen Action: \"${entry.chosenOptionText}\"",
                                        color = InkDark,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = entry.outcomeDescription,
                                        color = InkDark,
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp,
                                        fontFamily = FontFamily.Serif
                                    )

                                    if (entry.statChangesText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Impact: ${entry.statChangesText}",
                                            color = Color(0xFF1B6E1B),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
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
}
