package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EventOption
import com.example.ui.theme.AgedBone
import com.example.ui.theme.AgedGold
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.DarkCanvasSurface
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.MerchantBadgeBg
import com.example.ui.theme.MerchantBadgeText
import com.example.ui.theme.MerchantCardBorder
import com.example.ui.theme.NobleBadgeBg
import com.example.ui.theme.NobleBadgeText
import com.example.ui.theme.NobleCardBorder
import com.example.ui.theme.PeasantBadgeBg
import com.example.ui.theme.PeasantBadgeText
import com.example.ui.theme.PeasantCardBorder
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import com.example.data.AppLanguage

@Composable
fun HandOfCardsView(
    options: List<EventOption>,
    playerRankLevel: Int = 1,
    isEnabled: Boolean,
    onHoverNpc: (Boolean) -> Unit,
    onOptionSelected: (EventOption) -> Unit,
    selectedLanguage: AppLanguage = AppLanguage.SLOVAK,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = if (selectedLanguage == AppLanguage.SLOVAK) {
                        if (isEnabled) "VYBERTE KARTU (POTIAHNITE ALEBO ŤUKNITE)" else "KONZULTÁCIA S KRONIKÁROM..."
                    } else {
                        if (isEnabled) "𝕾𝖂𝕴𝖄𝕰 CARD UP TO PLAY (OR TAP)" else "CONSULTING CHRONICLER..."
                    },
                    color = AgedGold,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 1.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                options.take(3).forEachIndexed { index, option ->
                    val fanRotation = when (index) {
                        0 -> -5f
                        1 -> 0f
                        else -> 5f
                    }

                    InteractiveCardItem(
                        option = option,
                        index = index,
                        playerRankLevel = playerRankLevel,
                        fanRotation = fanRotation,
                        isEnabled = isEnabled,
                        onHoverNpc = onHoverNpc,
                        onPlayCard = {
                            onHoverNpc(false)
                            onOptionSelected(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveCardItem(
    option: EventOption,
    index: Int,
    playerRankLevel: Int,
    fanRotation: Float,
    isEnabled: Boolean,
    onHoverNpc: (Boolean) -> Unit,
    onPlayCard: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dragY = remember { Animatable(0f) }
    val dragX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val testTagStr = "choice_button_${index + 1}"

    val cardDesign = getCardDesign(playerRankLevel, option.cardArchetype, option.tag)
    val cardScale = if (isDragging) 1.08f else 1.0f

    val cardInitial = option.tag.take(1).ifBlank { option.text.take(1) }.uppercase()

    Box(
        modifier = Modifier
            .width(112.dp)
            .height(170.dp)
            .rotate(if (isDragging) 0f else fanRotation)
            .offset { IntOffset(dragX.value.roundToInt(), dragY.value.roundToInt()) }
            .scale(cardScale)
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput

                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newY = dragY.value + dragAmount.y
                            val newX = dragX.value + dragAmount.x
                            dragY.snapTo(newY)
                            dragX.snapTo(newX)

                            if (newY < -120f) {
                                onHoverNpc(true)
                            } else {
                                onHoverNpc(false)
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        if (dragY.value < -120f) {
                            coroutineScope.launch {
                                dragY.animateTo(-400f, tween(180))
                                onPlayCard()
                                dragY.snapTo(0f)
                                dragX.snapTo(0f)
                            }
                        } else {
                            onHoverNpc(false)
                            coroutineScope.launch {
                                dragY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                dragX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        onHoverNpc(false)
                        coroutineScope.launch {
                            dragY.animateTo(0f)
                            dragX.animateTo(0f)
                        }
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
                .shadow(if (isDragging) 14.dp else 6.dp, RoundedCornerShape(14.dp))
                .border(2.dp, cardDesign.borderColor, RoundedCornerShape(14.dp))
                .testTag(testTagStr),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) AgedBone else Color(0xFFC0B8AD)
            ),
            onClick = {
                if (isEnabled) {
                    coroutineScope.launch {
                        dragY.animateTo(-300f, tween(150))
                        onPlayCard()
                        dragY.snapTo(0f)
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEnabled) cardDesign.badgeBg else Color(0xFF6C6052),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = option.tag.ifBlank { "Action" },
                        color = cardDesign.badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 3.dp, horizontal = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                IlluminatedInitial(
                    letter = cardInitial,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    fontSize = 24.sp,
                    textColor = cardDesign.initialColor,
                    borderColor = cardDesign.borderColor,
                    backgroundColor = DeepCharcoal
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = option.text,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = if (isEnabled) DarkInk else Color(0xFF6A6054),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

private data class CardDesign(
    val borderColor: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val initialColor: Color
)

private fun getCardDesign(
    playerRankLevel: Int,
    cardArchetype: String,
    tag: String
): CardDesign {
    val tagLower = tag.lowercase()
    val archUpper = cardArchetype.uppercase()

    val initialColor = when {
        archUpper.contains("CHURCH") || tagLower.contains("plea") || tagLower.contains("holy") -> AntiqueGold
        archUpper.contains("UNDERWORLD") || tagLower.contains("shadow") || tagLower.contains("combat") || tagLower.contains("strike") -> AntiqueGold
        archUpper.contains("NOBLE") || tagLower.contains("royal") || tagLower.contains("seal") || tagLower.contains("challenge") -> AgedGold
        archUpper.contains("MERCHANT") || tagLower.contains("trade") || tagLower.contains("bribe") || tagLower.contains("purse") -> AgedGold
        else -> AntiqueGold
    }

    return when {
        archUpper.contains("CHURCH") -> CardDesign(
            borderColor = Color(0xFF7B2CBF),
            badgeBg = Color(0xFF5A189A),
            badgeText = Color(0xFFFFF8EE),
            initialColor = initialColor
        )
        archUpper.contains("UNDERWORLD") -> CardDesign(
            borderColor = Color(0xFF8B2600),
            badgeBg = Color(0xFF4A1A0C),
            badgeText = Color(0xFFFFD700),
            initialColor = initialColor
        )
        archUpper.contains("NOBLE") || playerRankLevel >= 5 -> CardDesign(
            borderColor = NobleCardBorder,
            badgeBg = NobleBadgeBg,
            badgeText = NobleBadgeText,
            initialColor = initialColor
        )
        archUpper.contains("MERCHANT") || playerRankLevel in 2..4 -> CardDesign(
            borderColor = MerchantCardBorder,
            badgeBg = MerchantBadgeBg,
            badgeText = MerchantBadgeText,
            initialColor = initialColor
        )
        else -> CardDesign(
            borderColor = PeasantCardBorder,
            badgeBg = PeasantBadgeBg,
            badgeText = PeasantBadgeText,
            initialColor = initialColor
        )
    }
}
