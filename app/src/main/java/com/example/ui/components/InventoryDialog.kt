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
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Work
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
import com.example.ui.theme.MedievalTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.InventoryItem
import com.example.data.PropertyAsset
import com.example.ui.theme.AgedGold
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.MedievalCrimson
import com.example.ui.theme.ParchmentCard
import com.example.ui.theme.ParchmentCardBorder

@Composable
fun InventoryDialog(
    inventory: List<InventoryItem>,
    properties: List<PropertyAsset>,
    totalPassiveGoldIncome: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.5.dp, AgedGold, RoundedCornerShape(16.dp))
                .testTag("inventory_dialog"),
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
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = MedievalCrimson
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inventory & Properties",
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

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Section 1: Owned Properties / Assets
                    item {
                        Text(
                            text = "🏰 OWNED PROPERTIES (+${totalPassiveGoldIncome} GOLD/TURN)",
                            color = MedievalCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MedievalTitle,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (properties.isEmpty()) {
                        item {
                            Text(
                                text = "No properties owned yet. Acquire fiefs, shops, or taverns to earn passive gold!",
                                color = InkMedium,
                                fontSize = 12.sp,
                                fontFamily = MedievalTitle
                            )
                        }
                    } else {
                        items(properties) { asset ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF3E7CA),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ParchmentCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = asset.icon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = asset.name,
                                            color = InkDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            fontFamily = MedievalTitle
                                        )
                                        Text(
                                            text = asset.description,
                                            color = InkMedium,
                                            fontSize = 11.5.sp,
                                            fontFamily = MedievalTitle
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+${asset.passiveGoldIncome}g/turn",
                                        color = Color(0xFF1B6E1B),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // Section 2: Inventory Relics & Items
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🎒 INVENTORY RELICS (${inventory.size})",
                            color = MedievalCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MedievalTitle,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (inventory.isEmpty()) {
                        item {
                            Text(
                                text = "Your travel pack is empty.",
                                color = InkMedium,
                                fontSize = 12.sp,
                                fontFamily = MedievalTitle
                            )
                        }
                    } else {
                        items(inventory) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF3E7CA),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ParchmentCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.icon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            color = InkDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            fontFamily = MedievalTitle
                                        )
                                        Text(
                                            text = item.description,
                                            color = InkMedium,
                                            fontSize = 11.5.sp,
                                            fontFamily = MedievalTitle
                                        )
                                    }
                                    if (item.statBonus.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.statBonus,
                                            color = AgedGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
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
