package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FactionKey
import com.example.ui.theme.AgedGold
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.MedievalCrimson
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentCardBorder

@Composable
fun FactionsDialog(
    factionsReputation: Map<String, Int>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.5.dp, AgedGold, RoundedCornerShape(16.dp))
                .testTag("factions_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ParchmentCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MedievalCrimson
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Realm Factions & Standing",
                            color = InkDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MedievalTitle
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

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(FactionKey.values()) { faction ->
                        val repValue = factionsReputation[faction.displayName] ?: 50
                        val progressFraction = (repValue / 100f).coerceIn(0f, 1f)

                        val standingLabel = when {
                            repValue >= 80 -> "Exalted Ally"
                            repValue >= 60 -> "Trusted Friend"
                            repValue >= 40 -> "Neutral Standing"
                            repValue >= 20 -> "Distrusted"
                            else -> "Hostile Nemesis"
                        }

                        val progressColor = when {
                            repValue >= 60 -> Color(0xFF2ECC71)
                            repValue >= 40 -> AgedGold
                            else -> Color(0xFFFF4D4D)
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF3E7CA),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ParchmentCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = faction.icon, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = faction.displayName,
                                            color = MedievalCrimson,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            fontFamily = MedievalTitle
                                        )
                                    }

                                    Text(
                                        text = "$standingLabel ($repValue%)",
                                        color = progressColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        fontFamily = MedievalTitle
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = faction.description,
                                    color = InkDark,
                                    fontSize = 11.5.sp,
                                    fontFamily = MedievalTitle
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = progressFraction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = progressColor,
                                    trackColor = Color(0xFFDFD5C2)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
