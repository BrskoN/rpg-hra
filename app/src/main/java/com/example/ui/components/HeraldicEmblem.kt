package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.MedievalTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AgedGold
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.DarkCanvasSurface
import com.example.ui.theme.DeepCharcoal

enum class HeraldicShape {
    SHIELD,
    CIRCLE,
    TAROT_FRAME
}

@Composable
fun HeraldicEmblem(
    initialLetter: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    textColor: Color = AntiqueGold,
    bgGradient: List<Color> = listOf(DarkCanvasSurface, DeepCharcoal),
    borderColor: Color = AgedGold,
    shapeType: HeraldicShape = HeraldicShape.SHIELD
) {
    val shape = when (shapeType) {
        HeraldicShape.CIRCLE -> CircleShape
        HeraldicShape.SHIELD -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        HeraldicShape.TAROT_FRAME -> RoundedCornerShape(12.dp)
    }

    val displayLetter = initialLetter.trim().take(1).uppercase().ifEmpty { "𝕸" }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(bgGradient))
            .border(2.dp, borderColor, shape)
            .padding(4.dp)
            .border(1.dp, borderColor.copy(alpha = 0.5f), shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .border(0.5.dp, borderColor.copy(alpha = 0.3f), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayLetter,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MedievalTitle,
                textAlign = TextAlign.Center
            )
        }
    }
}
