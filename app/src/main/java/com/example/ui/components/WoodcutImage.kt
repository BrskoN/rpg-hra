package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AgedGold

@Composable
fun WoodcutImage(
    letter: String = "𝕸",
    modifier: Modifier = Modifier,
    textColor: Color = AgedGold
) {
    IlluminatedInitial(
        letter = letter,
        modifier = modifier,
        fontSize = 28.sp,
        textColor = textColor
    )
}
