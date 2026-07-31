package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Global illuminated-manuscript atmosphere: a static film-grain speckle (aged parchment texture)
 * plus a soft vignette darkening the frame edges. Applied once at the app root so every screen -
 * origin selection, the game itself, dialogs, the legacy screen - reads as one physical object
 * instead of flat Compose surfaces.
 *
 * The grain point list is generated once per canvas size via drawWithCache (not per frame), so it
 * reads as a fixed paper texture rather than animated static.
 */
fun Modifier.medievalAtmosphere(): Modifier = this.drawWithCache {
    val random = Random(1207) // fixed seed: identical grain pattern every launch, no animation cost
    val grainCount = (size.width * size.height / 850f).toInt().coerceIn(300, 4000)
    val grainDots = List(grainCount) {
        Triple(
            Offset(random.nextFloat() * size.width, random.nextFloat() * size.height),
            random.nextFloat() * 1.6f + 0.4f, // radius
            random.nextFloat() * 0.05f + 0.015f // alpha
        )
    }

    val vignetteBrush = Brush.radialGradient(
        colors = listOf(
            Color.Transparent,
            Color.Transparent,
            Color.Black.copy(alpha = 0.5f)
        ),
        center = Offset(size.width / 2f, size.height / 2f),
        radius = size.width.coerceAtLeast(size.height) * 0.8f
    )

    onDrawWithContent {
        drawContent()

        grainDots.forEach { (offset, radius, alpha) ->
            drawCircle(
                color = Color.Black,
                radius = radius,
                center = offset,
                alpha = alpha
            )
        }

        drawRect(brush = vignetteBrush)
    }
}
