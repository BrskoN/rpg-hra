package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AgedGold
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.BlackletterDisplay
import com.example.ui.theme.DarkCanvasSurface
import com.example.ui.theme.DeepCharcoal

@Composable
fun IlluminatedInitial(
    letter: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    textColor: Color = AntiqueGold,
    borderColor: Color = AgedGold,
    backgroundColor: Color = DeepCharcoal
) {
    val initial = letter.trim().take(1).uppercase().ifEmpty { "𝕸" }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(DarkCanvasSurface, backgroundColor)
                )
            )
            .border(2.dp, borderColor, shape)
            .padding(3.dp)
            .border(1.dp, borderColor.copy(alpha = 0.5f), shape)
            .drawBehind {
                val w = size.width
                val h = size.height
                val accentLen = 6.dp.toPx()
                val color = borderColor.copy(alpha = 0.7f)
                
                // Corner ornate accent lines
                drawLine(color, Offset(0f, 0f), Offset(accentLen, 0f), strokeWidth = 2f)
                drawLine(color, Offset(0f, 0f), Offset(0f, accentLen), strokeWidth = 2f)
                
                drawLine(color, Offset(w, 0f), Offset(w - accentLen, 0f), strokeWidth = 2f)
                drawLine(color, Offset(w, 0f), Offset(w, accentLen), strokeWidth = 2f)
                
                drawLine(color, Offset(0f, h), Offset(accentLen, h), strokeWidth = 2f)
                drawLine(color, Offset(0f, h), Offset(0f, h - accentLen), strokeWidth = 2f)
                
                drawLine(color, Offset(w, h), Offset(w - accentLen, h), strokeWidth = 2f)
                drawLine(color, Offset(w, h), Offset(w, h - accentLen), strokeWidth = 2f)
            },
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
                text = initial,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = BlackletterDisplay,
                textAlign = TextAlign.Center
            )
        }
    }
}
