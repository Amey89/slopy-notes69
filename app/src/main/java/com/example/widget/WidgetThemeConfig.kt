package com.example.widget

import android.content.Context
import android.graphics.Color
import com.example.R

enum class WidgetTheme(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val isLight: Boolean,
    val bgWidgetRes: Int,
    val bgItemRes: Int,
    val titleTextColor: Int,
    val contentTextColor: Int,
    val metaTextColor: Int,
    val countBadgeTextColor: Int,
    val previewColorHex: String
) {
    LIGHT_CLEAN(
        key = "LIGHT_CLEAN",
        displayName = "Pure White (M3 Expressive)",
        subtitle = "Crisp white surface with deep slate text & indigo accents",
        isLight = true,
        bgWidgetRes = R.drawable.bg_widget_light,
        bgItemRes = R.drawable.bg_widget_item_light,
        titleTextColor = Color.parseColor("#0F172A"),
        contentTextColor = Color.parseColor("#1E293B"),
        metaTextColor = Color.parseColor("#475569"),
        countBadgeTextColor = Color.parseColor("#4F46E5"),
        previewColorHex = "#FFFFFF"
    ),
    WARM_CREAM(
        key = "WARM_CREAM",
        displayName = "Warm Paper",
        subtitle = "Soft ivory paper background for comfortable reading",
        isLight = true,
        bgWidgetRes = R.drawable.bg_widget_cream,
        bgItemRes = R.drawable.bg_widget_item_cream,
        titleTextColor = Color.parseColor("#1C1917"),
        contentTextColor = Color.parseColor("#292524"),
        metaTextColor = Color.parseColor("#57534E"),
        countBadgeTextColor = Color.parseColor("#B45309"),
        previewColorHex = "#FBF9F5"
    ),
    DEEP_SLATE(
        key = "DEEP_SLATE",
        displayName = "Deep Slate Dark",
        subtitle = "Rich dark charcoal surface with crisp white text",
        isLight = false,
        bgWidgetRes = R.drawable.bg_widget_dark,
        bgItemRes = R.drawable.bg_widget_item_dark,
        titleTextColor = Color.WHITE,
        contentTextColor = Color.parseColor("#E2E8F0"),
        metaTextColor = Color.parseColor("#94A3B8"),
        countBadgeTextColor = Color.parseColor("#38BDF8"),
        previewColorHex = "#12161C"
    ),
    OLED_BLACK(
        key = "OLED_BLACK",
        displayName = "OLED Pure Black",
        subtitle = "True #000000 black background for AMOLED screens",
        isLight = false,
        bgWidgetRes = R.drawable.bg_widget_oled,
        bgItemRes = R.drawable.bg_widget_item_oled,
        titleTextColor = Color.WHITE,
        contentTextColor = Color.parseColor("#E2E8F0"),
        metaTextColor = Color.parseColor("#94A3B8"),
        countBadgeTextColor = Color.parseColor("#818CF8"),
        previewColorHex = "#000000"
    );

    companion object {
        private const val PREFS_NAME = "widget_prefs"
        private const val PREF_KEY_THEME = "pref_widget_theme"

        fun getSelectedTheme(context: Context): WidgetTheme {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedKey = prefs.getString(PREF_KEY_THEME, LIGHT_CLEAN.key)
            return entries.find { it.key == savedKey } ?: LIGHT_CLEAN
        }

        fun setSelectedTheme(context: Context, theme: WidgetTheme) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_KEY_THEME, theme.key).apply()
            NotesTasksAppWidget.notifyWidgetUpdate(context)
        }
    }
}
