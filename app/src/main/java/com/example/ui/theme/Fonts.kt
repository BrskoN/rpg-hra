package com.example.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import com.example.R

/** Big single-character illuminated manuscript capitals (IlluminatedInitial only). */
val BlackletterDisplay = FontFamily(Font(R.font.unifraktur_maguntia))

/** Headers, titles, buttons, badges and short UI labels across the game. */
val MedievalTitle = FontFamily(Font(R.font.medieval_sharp))

/** Long-form narrative paragraphs where extended readability matters. */
val ManuscriptBody = FontFamily(
    Font(R.font.im_fell_english_regular),
    Font(R.font.im_fell_english_italic, style = FontStyle.Italic)
)
