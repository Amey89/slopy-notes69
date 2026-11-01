package com.example.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint as AndroidPaint
import android.graphics.Shader
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

data class ThemePresetInfo(
    val id: Int,
    val name: String,
    val description: String,
    val primaryColor: Color,
    val previewBgLight: Color,
    val previewBgDark: Color
)

data class BackgroundPatternInfo(
    val id: Int,
    val name: String,
    val description: String
)

object ThemeManager {
    val Presets = listOf(
        ThemePresetInfo(
            id = 0,
            name = "M3 Expressive Iris",
            description = "Vibrant indigo, mint & coral expressive tonal palette",
            primaryColor = Color(0xFF4F46E5),
            previewBgLight = Color(0xFFF8F9FE),
            previewBgDark = Color(0xFF0B0E17)
        ),
        ThemePresetInfo(
            id = 6,
            name = "Material You Dynamic",
            description = "Adapts to your Android 12+ wallpaper colors",
            primaryColor = Color(0xFF6366F1),
            previewBgLight = Color(0xFFF5F3FF),
            previewBgDark = Color(0xFF13111C)
        ),
        ThemePresetInfo(
            id = 1,
            name = "Warm Parchment",
            description = "Cozy cream paper with warm amber accents",
            primaryColor = Color(0xFFB45309),
            previewBgLight = Color(0xFFFDFBF7),
            previewBgDark = Color(0xFF1C1917)
        ),
        ThemePresetInfo(
            id = 2,
            name = "Midnight AMOLED",
            description = "Pure pitch black OLED with crisp sky accents",
            primaryColor = Color(0xFF38BDF8),
            previewBgLight = Color(0xFFF8FAFC),
            previewBgDark = Color(0xFF000000)
        ),
        ThemePresetInfo(
            id = 3,
            name = "Emerald Forest",
            description = "Calming botanical sage and vibrant emerald",
            primaryColor = Color(0xFF059669),
            previewBgLight = Color(0xFFF6FBF7),
            previewBgDark = Color(0xFF0D1F17)
        ),
        ThemePresetInfo(
            id = 4,
            name = "Twilight Lavender",
            description = "Deep nocturnal violet and subtle purple",
            primaryColor = Color(0xFF7C3AED),
            previewBgLight = Color(0xFFFAF7FD),
            previewBgDark = Color(0xFF171324)
        ),
        ThemePresetInfo(
            id = 5,
            name = "Ocean Navy",
            description = "Classic deep oceanic navy and crisp azure",
            primaryColor = Color(0xFF0284C7),
            previewBgLight = Color(0xFFF0F9FF),
            previewBgDark = Color(0xFF0B1728)
        )
    )

    val Patterns = listOf(
        BackgroundPatternInfo(0, "Clean Flat", "Solid uncluttered surface"),
        BackgroundPatternInfo(1, "Dotted Journal", "Subtle dot grid paper texture"),
        BackgroundPatternInfo(2, "Graph Paper", "Subtle technical grid lines"),
        BackgroundPatternInfo(3, "Atmospheric Aura", "Soft radiant top glow")
    )

    fun getColorScheme(presetId: Int, isDark: Boolean): ColorScheme {
        return when (presetId) {
            1 -> { // Warm Parchment
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFFFBBF24),
                        onPrimary = Color(0xFF2E1C05),
                        primaryContainer = Color(0xFF452B0F),
                        onPrimaryContainer = Color(0xFFFDE68A),
                        secondary = Color(0xFFD97706),
                        onSecondary = Color(0xFF1C1917),
                        background = Color(0xFF1A1613),
                        surface = Color(0xFF221E1A),
                        onSurface = Color(0xFFF5EFEB),
                        surfaceVariant = Color(0xFF2E2822),
                        onSurfaceVariant = Color(0xFFD6C8BA),
                        outline = Color(0xFF4A4036)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFFB45309),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFFFEF3C7),
                        onPrimaryContainer = Color(0xFF78350F),
                        secondary = Color(0xFF92400E),
                        onSecondary = Color(0xFFFFFFFF),
                        background = Color(0xFFFDFBF7),
                        surface = Color(0xFFFFFFFF),
                        onSurface = Color(0xFF292524),
                        surfaceVariant = Color(0xFFF7F2EB),
                        onSurfaceVariant = Color(0xFF786F66),
                        outline = Color(0xFFE5DDD0)
                    )
                }
            }
            2 -> { // Midnight AMOLED
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFF38BDF8),
                        onPrimary = Color(0xFF000000),
                        primaryContainer = Color(0xFF0C2438),
                        onPrimaryContainer = Color(0xFFBAE6FD),
                        secondary = Color(0xFF7DD3FC),
                        onSecondary = Color(0xFF000000),
                        background = Color(0xFF000000),
                        surface = Color(0xFF080808),
                        onSurface = Color(0xFFF8FAFC),
                        surfaceVariant = Color(0xFF141414),
                        onSurfaceVariant = Color(0xFF94A3B8),
                        outline = Color(0xFF242424)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF0284C7),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFFE0F2FE),
                        onPrimaryContainer = Color(0xFF0369A1),
                        background = Color(0xFFF8FAFC),
                        surface = Color(0xFFFFFFFF),
                        onSurface = Color(0xFF0F172A),
                        surfaceVariant = Color(0xFFF1F5F9),
                        onSurfaceVariant = Color(0xFF64748B),
                        outline = Color(0xFFE2E8F0)
                    )
                }
            }
            3 -> { // Emerald Forest
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFF34D399),
                        onPrimary = Color(0xFF062316),
                        primaryContainer = Color(0xFF113825),
                        onPrimaryContainer = Color(0xFFA7F3D0),
                        secondary = Color(0xFF10B981),
                        onSecondary = Color(0xFF062316),
                        background = Color(0xFF0B1A13),
                        surface = Color(0xFF12241C),
                        onSurface = Color(0xFFECFDF5),
                        surfaceVariant = Color(0xFF1A3328),
                        onSurfaceVariant = Color(0xFFA7D3BD),
                        outline = Color(0xFF284D3C)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF059669),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFFD1FAE5),
                        onPrimaryContainer = Color(0xFF065F46),
                        secondary = Color(0xFF10B981),
                        onSecondary = Color(0xFFFFFFFF),
                        background = Color(0xFFF6FBF7),
                        surface = Color(0xFFFFFFFF),
                        onSurface = Color(0xFF064E3B),
                        surfaceVariant = Color(0xFFE8F5EC),
                        onSurfaceVariant = Color(0xFF4B725C),
                        outline = Color(0xFFCEE7D6)
                    )
                }
            }
            4 -> { // Twilight Lavender
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFFA78BFA),
                        onPrimary = Color(0xFF1E1038),
                        primaryContainer = Color(0xFF311E54),
                        onPrimaryContainer = Color(0xFFDDD6FE),
                        secondary = Color(0xFF8B5CF6),
                        onSecondary = Color(0xFF1E1038),
                        background = Color(0xFF120E1E),
                        surface = Color(0xFF1A152B),
                        onSurface = Color(0xFFF5F3FF),
                        surfaceVariant = Color(0xFF261F3C),
                        onSurfaceVariant = Color(0xFFC4B5FD),
                        outline = Color(0xFF3B2F5C)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF7C3AED),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFFEDE9FE),
                        onPrimaryContainer = Color(0xFF5B21B6),
                        secondary = Color(0xFF8B5CF6),
                        onSecondary = Color(0xFFFFFFFF),
                        background = Color(0xFFFAF7FD),
                        surface = Color(0xFFFFFFFF),
                        onSurface = Color(0xFF2E1065),
                        surfaceVariant = Color(0xFFF3EEFC),
                        onSurfaceVariant = Color(0xFF6B5B95),
                        outline = Color(0xFFE2D7F7)
                    )
                }
            }
            5 -> { // Ocean Navy
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFF38BDF8),
                        onPrimary = Color(0xFF082032),
                        primaryContainer = Color(0xFF102E48),
                        onPrimaryContainer = Color(0xFFBAE6FD),
                        secondary = Color(0xFF0EA5E9),
                        onSecondary = Color(0xFF082032),
                        background = Color(0xFF0B1420),
                        surface = Color(0xFF111E2E),
                        onSurface = Color(0xFFF0F9FF),
                        surfaceVariant = Color(0xFF182A40),
                        onSurfaceVariant = Color(0xFF93C5FD),
                        outline = Color(0xFF253D5C)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF0284C7),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFFE0F2FE),
                        onPrimaryContainer = Color(0xFF0369A1),
                        secondary = Color(0xFF0369A1),
                        onSecondary = Color(0xFFFFFFFF),
                        background = Color(0xFFF0F9FF),
                        surface = Color(0xFFFFFFFF),
                        onSurface = Color(0xFF0C4A6E),
                        surfaceVariant = Color(0xFFE2F0FB),
                        onSurfaceVariant = Color(0xFF4A6B8A),
                        outline = Color(0xFFC7E2F7)
                    )
                }
            }
            else -> { // 0: Modern Slate (Default)
                if (isDark) {
                    darkColorScheme(
                        primary = PrimaryDark,
                        onPrimary = OnPrimaryDark,
                        primaryContainer = PrimaryContainerDark,
                        onPrimaryContainer = OnPrimaryContainerDark,
                        secondary = SecondaryDark,
                        onSecondary = OnSecondaryDark,
                        secondaryContainer = SecondaryContainerDark,
                        onSecondaryContainer = OnSecondaryContainerDark,
                        tertiary = TertiaryDark,
                        onTertiary = OnTertiaryDark,
                        tertiaryContainer = TertiaryContainerDark,
                        onTertiaryContainer = OnTertiaryContainerDark,
                        background = BackgroundDark,
                        onBackground = OnBackgroundDark,
                        surface = SurfaceDark,
                        onSurface = OnSurfaceDark,
                        surfaceVariant = SurfaceVariantDark,
                        onSurfaceVariant = OnSurfaceVariantDark,
                        outline = OutlineDark
                    )
                } else {
                    lightColorScheme(
                        primary = PrimaryLight,
                        onPrimary = OnPrimaryLight,
                        primaryContainer = PrimaryContainerLight,
                        onPrimaryContainer = OnPrimaryContainerLight,
                        secondary = SecondaryLight,
                        onSecondary = OnSecondaryLight,
                        secondaryContainer = SecondaryContainerLight,
                        onSecondaryContainer = OnSecondaryContainerLight,
                        tertiary = TertiaryLight,
                        onTertiary = OnTertiaryLight,
                        tertiaryContainer = TertiaryContainerLight,
                        onTertiaryContainer = OnTertiaryContainerLight,
                        background = BackgroundLight,
                        onBackground = OnBackgroundLight,
                        surface = SurfaceLight,
                        onSurface = OnSurfaceLight,
                        surfaceVariant = SurfaceVariantLight,
                        onSurfaceVariant = OnSurfaceVariantLight,
                        outline = OutlineLight
                    )
                }
            }
        }
    }
}

private var cachedPattern1Paint: AndroidPaint? = null
private var cachedPattern1IsDark: Boolean? = null

private var cachedPattern2Paint: AndroidPaint? = null
private var cachedPattern2IsDark: Boolean? = null

private fun getPattern1Paint(isDark: Boolean, density: Float): AndroidPaint {
    val existing = cachedPattern1Paint
    if (existing != null && cachedPattern1IsDark == isDark) {
        return existing
    }
    val sizePx = (36f * density).toInt().coerceAtLeast(16)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val dotPaint = AndroidPaint().apply {
        isAntiAlias = true
        color = if (isDark) 0x20FFFFFF.toInt() else 0x14000000.toInt()
        style = AndroidPaint.Style.FILL
    }
    val r = (1f * density).coerceAtLeast(1f)
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, r, dotPaint)

    val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    val paint = AndroidPaint().apply {
        this.shader = shader
    }
    cachedPattern1IsDark = isDark
    cachedPattern1Paint = paint
    return paint
}

private fun getPattern2Paint(isDark: Boolean, density: Float): AndroidPaint {
    val existing = cachedPattern2Paint
    if (existing != null && cachedPattern2IsDark == isDark) {
        return existing
    }
    val sizePx = (40f * density).toInt().coerceAtLeast(16)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val linePaint = AndroidPaint().apply {
        isAntiAlias = false
        color = if (isDark) 0x12FFFFFF.toInt() else 0x0C000000.toInt()
        style = AndroidPaint.Style.STROKE
        strokeWidth = 1f
    }
    canvas.drawLine(0f, 0f, sizePx.toFloat(), 0f, linePaint)
    canvas.drawLine(0f, 0f, 0f, sizePx.toFloat(), linePaint)

    val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    val paint = AndroidPaint().apply {
        this.shader = shader
    }
    cachedPattern2IsDark = isDark
    cachedPattern2Paint = paint
    return paint
}

/**
 * High-performance background drawing with zero overhead when pattern is clean (0).
 * Uses GPU-accelerated BitmapShader textures for instant 60 FPS rendering.
 */
fun Modifier.appBackground(pattern: Int, isDark: Boolean, primaryColor: Color = Color.Unspecified): Modifier {
    if (pattern <= 0) return this

    return this.drawBehind {
        when (pattern) {
            1 -> {
                // GPU-accelerated Dotted Journal Paper texture
                val paint = getPattern1Paint(isDark, density)
                drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
            }
            2 -> {
                // GPU-accelerated Technical Graph Grid texture
                val paint = getPattern2Paint(isDark, density)
                drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
            }
            3 -> {
                // Subtle Atmospheric Aura (fast gradient)
                val auraColor = if (primaryColor != Color.Unspecified) {
                    primaryColor.copy(alpha = if (isDark) 0.14f else 0.08f)
                } else {
                    if (isDark) Color(0x183B82F6) else Color(0x103B82F6)
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(auraColor, Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.4f
                    )
                )
            }
            else -> {}
        }
    }
}

@Composable
fun Modifier.appBackground(themePreset: Int, backgroundPattern: Int): Modifier {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    return this.appBackground(backgroundPattern, isDark, primaryColor)
}
