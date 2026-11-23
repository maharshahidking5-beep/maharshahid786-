package com.example.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold),
)

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

val NotoNastaliqUrduFamily = FontFamily(
    Font(R.font.noto_nastaliq_urdu, FontWeight.Normal),
    Font(R.font.noto_nastaliq_urdu, FontWeight.Bold),
)

val NotoSansDevanagariFamily = FontFamily(
    Font(R.font.noto_sans_devanagari, FontWeight.Normal),
    Font(R.font.noto_sans_devanagari, FontWeight.Bold),
)

val PlayfairDisplayFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Bold),
)

val PacificoFamily = FontFamily(
    Font(R.font.pacifico, FontWeight.Normal),
)

val BungeeFamily = FontFamily(
    Font(R.font.bungee, FontWeight.Normal),
)

data class StudioFontOption(
    val key: String,
    val label: String,
    val languageHint: String,
    val fontFamily: FontFamily,
    @FontRes val fontResId: Int
)

object StudioFontCatalog {
    val options: List<StudioFontOption> = listOf(
        StudioFontOption("display", "Studio Bold", "EN", SpaceGroteskFamily, R.font.space_grotesk),
        StudioFontOption("nastaliq", "نستعلیق اردو", "UR", NotoNastaliqUrduFamily, R.font.noto_nastaliq_urdu),
        StudioFontOption("hindi", "देवनागरी हिंदी", "HI", NotoSansDevanagariFamily, R.font.noto_sans_devanagari),
        StudioFontOption("serif", "Calligraphy", "EN", PlayfairDisplayFamily, R.font.playfair_display),
        StudioFontOption("script", "Brush Script", "EN", PacificoFamily, R.font.pacifico),
        StudioFontOption("bold3d", "Poster 3D", "EN", BungeeFamily, R.font.bungee),
        StudioFontOption("mono", "Tech Mono", "EN", JetBrainsMonoFamily, R.font.jetbrains_mono)
    )

    fun optionForKey(key: String): StudioFontOption {
        return options.find { it.key == key } ?: options.first()
    }

    fun defaultKeyForLanguage(languageTag: String): String = when (languageTag.uppercase()) {
        "UR" -> "nastaliq"
        "HI" -> "hindi"
        else -> "display"
    }
}

val StudioTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)
