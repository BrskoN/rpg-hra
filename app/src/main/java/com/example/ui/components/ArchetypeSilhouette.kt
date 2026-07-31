package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Simple woodcut-style silhouette per NPC archetype, drawn as a low-alpha watermark behind the
 * illuminated initial in NpcPortraitView. Purely procedural (Path drawing) so it needs no bitmap
 * assets, but gives each archetype a distinct, recognizable emblem instead of every NPC reading
 * as an identical "letter in a circle".
 */
@Composable
fun ArchetypeSilhouette(
    archetype: String,
    tint: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 0.16f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (archetype.uppercase()) {
            "BISHOP", "PRIEST", "MONK" -> drawMitre(w, h, tint, alpha)
            "MERCHANT" -> drawMerchantCap(w, h, tint, alpha)
            "KNIGHT", "GUARD" -> drawHelm(w, h, tint, alpha)
            "ALCHEMIST" -> drawWizardHood(w, h, tint, alpha)
            "BANDIT", "OUTLAW", "ROGUE" -> drawOutlawHood(w, h, tint, alpha)
            "NOBLE", "BARON", "LORD", "COUNT", "DUKE" -> drawCirclet(w, h, tint, alpha)
            "MONARCH", "KING", "QUEEN" -> drawCrown(w, h, tint, alpha)
            "ELDER" -> drawElderHood(w, h, tint, alpha)
            else -> drawPeasantHood(w, h, tint, alpha)
        }
    }
}

private fun DrawScope.drawMitre(w: Float, h: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(w * 0.30f, h * 0.85f)
        lineTo(w * 0.28f, h * 0.45f)
        quadraticTo(w * 0.32f, h * 0.10f, w * 0.50f, h * 0.30f)
        quadraticTo(w * 0.68f, h * 0.10f, w * 0.72f, h * 0.45f)
        lineTo(w * 0.70f, h * 0.85f)
        close()
    }
    drawPath(path, color, alpha = alpha)
    // Center cleft of the mitre
    drawLine(color.copy(alpha = alpha * 1.3f), Offset(w * 0.50f, h * 0.30f), Offset(w * 0.50f, h * 0.85f), strokeWidth = w * 0.02f)
}

private fun DrawScope.drawMerchantCap(w: Float, h: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(w * 0.25f, h * 0.55f)
        quadraticTo(w * 0.22f, h * 0.20f, w * 0.50f, h * 0.15f)
        quadraticTo(w * 0.78f, h * 0.20f, w * 0.75f, h * 0.55f)
        quadraticTo(w * 0.50f, h * 0.68f, w * 0.25f, h * 0.55f)
        close()
    }
    drawPath(path, color, alpha = alpha)
    drawCircle(color.copy(alpha = alpha * 1.4f), radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.78f))
}

private fun DrawScope.drawHelm(w: Float, h: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(w * 0.28f, h * 0.85f)
        lineTo(w * 0.25f, h * 0.45f)
        quadraticTo(w * 0.25f, h * 0.12f, w * 0.50f, h * 0.10f)
        quadraticTo(w * 0.75f, h * 0.12f, w * 0.75f, h * 0.45f)
        lineTo(w * 0.72f, h * 0.85f)
        quadraticTo(w * 0.50f, h * 0.95f, w * 0.28f, h * 0.85f)
        close()
    }
    drawPath(path, color, alpha = alpha)
    // Visor slit
    drawRect(
        color.copy(alpha = alpha * 1.6f),
        topLeft = Offset(w * 0.44f, h * 0.42f),
        size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.28f)
    )
}

private fun DrawScope.drawWizardHood(w: Float, h: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.05f)
        lineTo(w * 0.78f, h * 0.85f)
        quadraticTo(w * 0.5f, h * 0.98f, w * 0.22f, h * 0.85f)
        close()
    }
    drawPath(path, color, alpha = alpha)
    // Small flask accent, bottom-right
    val flask = Path().apply {
        moveTo(w * 0.68f, h * 0.62f)
        lineTo(w * 0.68f, h * 0.72f)
        lineTo(w * 0.60f, h * 0.88f)
        quadraticTo(w * 0.74f, h * 0.94f, w * 0.86f, h * 0.88f)
        lineTo(w * 0.78f, h * 0.72f)
        lineTo(w * 0.78f, h * 0.62f)
        close()
    }
    drawPath(flask, color.copy(alpha = alpha * 1.3f))
}

private fun DrawScope.drawOutlawHood(w: Float, h: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.10f)
        quadraticTo(w * 0.20f, h * 0.20f, w * 0.22f, h * 0.60f)
        quadraticTo(w * 0.30f, h * 0.90f, w * 0.5f, h * 0.92f)
        quadraticTo(w * 0.70f, h * 0.90f, w * 0.78f, h * 0.60f)
        quadraticTo(w * 0.80f, h * 0.20f, w * 0.5f, h * 0.10f)
        close()
    }
    drawPath(path, color, alpha = alpha)
    drawRect(
        color.copy(alpha = alpha * 1.6f),
        topLeft = Offset(w * 0.24f, h * 0.52f),
        size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.10f)
    )
}

private fun DrawScope.drawCirclet(w: Float, h: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(w * 0.30f, h * 0.70f)
        lineTo(w * 0.28f, h * 0.45f)
        lineTo(w * 0.40f, h * 0.55f)
        lineTo(w * 0.50f, h * 0.30f)
        lineTo(w * 0.60f, h * 0.55f)
        lineTo(w * 0.72f, h * 0.45f)
        lineTo(w * 0.70f, h * 0.70f)
        close()
    }
    drawPath(path, color, alpha = alpha)
}

private fun DrawScope.drawCrown(w: Float, h: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(w * 0.22f, h * 0.75f)
        lineTo(w * 0.20f, h * 0.40f)
        lineTo(w * 0.33f, h * 0.52f)
        lineTo(w * 0.42f, h * 0.22f)
        lineTo(w * 0.50f, h * 0.42f)
        lineTo(w * 0.58f, h * 0.22f)
        lineTo(w * 0.67f, h * 0.52f)
        lineTo(w * 0.80f, h * 0.40f)
        lineTo(w * 0.78f, h * 0.75f)
        close()
    }
    drawPath(path, color, alpha = alpha)
    drawCircle(color.copy(alpha = alpha * 1.5f), radius = w * 0.035f, center = Offset(w * 0.5f, h * 0.5f))
}

private fun DrawScope.drawElderHood(w: Float, h: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(w * 0.30f, h * 0.85f)
        quadraticTo(w * 0.22f, h * 0.40f, w * 0.50f, h * 0.15f)
        quadraticTo(w * 0.78f, h * 0.40f, w * 0.70f, h * 0.85f)
        close()
    }
    drawPath(path, color, alpha = alpha)
    // Short beard triangle
    val beard = Path().apply {
        moveTo(w * 0.42f, h * 0.78f)
        lineTo(w * 0.5f, h * 0.98f)
        lineTo(w * 0.58f, h * 0.78f)
        close()
    }
    drawPath(beard, color.copy(alpha = alpha * 1.3f))
}

private fun DrawScope.drawPeasantHood(w: Float, h: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(w * 0.28f, h * 0.80f)
        quadraticTo(w * 0.24f, h * 0.45f, w * 0.50f, h * 0.20f)
        quadraticTo(w * 0.76f, h * 0.45f, w * 0.72f, h * 0.80f)
        quadraticTo(w * 0.50f, h * 0.90f, w * 0.28f, h * 0.80f)
        close()
    }
    drawPath(path, color, alpha = alpha)
}
