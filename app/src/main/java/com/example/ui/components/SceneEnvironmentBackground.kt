package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun SceneEnvironmentBackground(
    location: String,
    modifier: Modifier = Modifier
) {
    val normalizedLoc = location.lowercase()

    Crossfade(
        targetState = normalizedLoc,
        animationSpec = tween(700),
        label = "scene_background_transition",
        modifier = modifier.fillMaxSize()
    ) { loc ->
        when {
            loc.contains("forest") || loc.contains("wood") -> ForestBackground()
            loc.contains("tavern") || loc.contains("inn") -> TavernBackground()
            loc.contains("castle") || loc.contains("keep") || loc.contains("palace") -> CastleBackground()
            else -> VillageBackground() // Default to Village
        }
    }
}

@Composable
private fun ForestBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B3B2B),
                        Color(0xFF132B1F),
                        Color(0xFF0C1B13)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sunbeam light rays through canopy
            drawPath(
                path = Path().apply {
                    moveTo(w * 0.7f, 0f)
                    lineTo(w, 0f)
                    lineTo(w * 0.4f, h)
                    lineTo(w * 0.1f, h)
                    close()
                },
                color = Color(0x1DFDF2B3)
            )

            // Silhouetted Pine Trees
            val treePath = Path().apply {
                // Tree 1
                moveTo(w * 0.15f, h * 0.3f)
                lineTo(w * 0.25f, h * 0.8f)
                lineTo(w * 0.05f, h * 0.8f)
                close()

                // Tree 2
                moveTo(w * 0.85f, h * 0.25f)
                lineTo(w * 0.98f, h * 0.85f)
                lineTo(w * 0.72f, h * 0.85f)
                close()

                // Background ridge
                moveTo(0f, h * 0.7f)
                quadraticTo(w * 0.5f, h * 0.6f, w, h * 0.75f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(treePath, Color(0xFF09140E))
        }
    }
}

@Composable
private fun VillageBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3D2817),
                        Color(0xFF2B1A0D),
                        Color(0xFF1C1007)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Warm Village Square Ambient Glow
            drawCircle(
                color = Color(0x35FFB84D),
                center = Offset(w * 0.5f, h * 0.35f),
                radius = w * 0.55f
            )

            // Cobblestone path and roof silhouettes
            val roofPath = Path().apply {
                // Left house roof
                moveTo(0f, h * 0.45f)
                lineTo(w * 0.35f, h * 0.25f)
                lineTo(w * 0.45f, h * 0.5f)
                lineTo(0f, h * 0.55f)
                close()

                // Right house roof
                moveTo(w, h * 0.4f)
                lineTo(w * 0.65f, h * 0.28f)
                lineTo(w * 0.55f, h * 0.52f)
                lineTo(w, h * 0.6f)
                close()

                // Cobblestone bottom floor
                moveTo(0f, h * 0.75f)
                quadraticTo(w * 0.5f, h * 0.7f, w, h * 0.78f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(roofPath, Color(0xFF140B05))
        }
    }
}

@Composable
private fun TavernBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF5E220D),
                        Color(0xFF381206),
                        Color(0xFF1A0702)
                    ),
                    center = Offset(300f, 400f),
                    radius = 1200f
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Fireplace Ember Glow
            drawCircle(
                color = Color(0x40FF6A00),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = w * 0.6f
            )

            // Wooden Tavern Beams Arch
            val beamPath = Path().apply {
                // Top Arch Beam
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.15f)
                quadraticTo(w * 0.5f, h * 0.25f, 0f, h * 0.15f)
                close()

                // Side Pillars
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w * 0.12f, h))
                addRect(androidx.compose.ui.geometry.Rect(w * 0.88f, 0f, w, h))
            }
            drawPath(beamPath, Color(0xFF120401))
        }
    }
}

@Composable
private fun CastleBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2A2838),
                        Color(0xFF1C1A28),
                        Color(0xFF100F1A)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Stained Glass Light Beam
            drawPath(
                path = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    lineTo(w * 0.8f, h)
                    lineTo(w * 0.2f, h)
                    close()
                },
                color = Color(0x208B0000)
            )

            // Gothic Castle Pillars & Arches
            val archPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.22f)
                cubicTo(w * 0.8f, h * 0.05f, w * 0.2f, h * 0.05f, 0f, h * 0.22f)
                close()

                // Left Pillar
                moveTo(0f, 0f)
                lineTo(w * 0.15f, 0f)
                lineTo(w * 0.15f, h)
                lineTo(0f, h)
                close()

                // Right Pillar
                moveTo(w * 0.85f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(w * 0.85f, h)
                close()
            }
            drawPath(archPath, Color(0xFF0B0A12))
        }
    }
}
