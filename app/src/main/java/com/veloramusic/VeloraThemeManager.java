package com.veloramusic;

import android.graphics.Color;

public final class VeloraThemeManager {
    public static final String PREF_NAME = "velora_ui";
    public static final String KEY_ACCENT = "accent";
    public static final String KEY_THEME = "theme";
    public static final String KEY_DYNAMIC_THEME = "dynamic_theme";
    public static final String KEY_PURE_BLACK = "pure_black";
    public static final String KEY_HIGH_REFRESH_RATE = "high_refresh_rate";
    public static final String KEY_SHOW_QUALITY_BADGE = "show_quality_badge";
    public static final String KEY_PERSIST_QUEUE = "persistent_queue";
    public static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String KEY_AUDIO_NORMALIZATION = "audio_normalization";
    public static final String KEY_PAUSE_ON_MUTE = "pause_on_mute";
    public static final String KEY_RESUME_ON_BLUETOOTH = "resume_on_bluetooth";
    public static final String KEY_USE_NEW_PLAYER_DESIGN = "use_new_player_design";
    public static final String KEY_ENABLE_CROSSFADE = "crossfade_enabled";
    public static final String KEY_PLAYER_ARTWORK_RADIUS = "player_artwork_radius";
    public static final String KEY_PLAYER_ARTWORK_ANIMATION = "player_artwork_animation";
    public static final String KEY_PLAYER_SWIPE_GESTURE = "player_swipe_gesture";
    public static final String KEY_PLAYER_DYNAMIC_ACCENT = "player_dynamic_accent";
    public static final String KEY_PLAYER_PROGRESS_STYLE = "player_progress_style";
    public static final String KEY_PLAYER_USE_ALBUM_ACCENT = "player_use_album_accent";

    public static final int[] ACCENT_COLORS = new int[] {
            Color.rgb(184, 167, 255),
            Color.rgb(88, 188, 255),
            Color.rgb(72, 226, 187),
            Color.rgb(255, 144, 110),
            Color.rgb(255, 120, 154),
            Color.rgb(235, 103, 130),
            Color.rgb(165, 128, 255)
    };

    private VeloraThemeManager() {
    }

    public static String normalizeTheme(String theme) {
        if (theme == null) {
            return "dark";
        }
        switch (theme.toLowerCase()) {
            case "amoled":
            case "light":
            case "dark":
            case "auto":
                return theme.toLowerCase();
            default:
                return "dark";
        }
    }

    public static int resolveBackgroundColor(String theme, boolean pureBlack, boolean nightMode) {
        if (pureBlack) {
            return Color.rgb(0, 0, 0);
        }
        String safe = normalizeTheme(theme);
        switch (safe) {
            case "amoled":
                return Color.rgb(0, 0, 0);
            case "light":
                return Color.rgb(245, 245, 250);
            case "auto":
                return nightMode ? Color.rgb(9, 10, 14) : Color.rgb(245, 245, 250);
            case "dark":
            default:
                return Color.rgb(9, 10, 14);
        }
    }

    public static int resolveSurfaceColor(String theme, boolean pureBlack, boolean nightMode) {
        if (pureBlack) {
            return Color.rgb(12, 12, 15);
        }
        String safe = normalizeTheme(theme);
        switch (safe) {
            case "amoled":
                return Color.rgb(13, 13, 15);
            case "light":
                return Color.rgb(255, 255, 255);
            case "auto":
                return nightMode ? Color.rgb(18, 18, 25) : Color.rgb(255, 255, 255);
            case "dark":
            default:
                return Color.rgb(18, 18, 25);
        }
    }

    public static int resolvePrimaryTextColor(String theme, boolean nightMode) {
        String safe = normalizeTheme(theme);
        if ("light".equals(safe)) {
            return Color.rgb(17, 21, 30);
        }
        if ("auto".equals(safe)) {
            return nightMode ? Color.rgb(245, 245, 247) : Color.rgb(17, 21, 30);
        }
        return Color.rgb(245, 245, 247);
    }

    public static int resolveSecondaryTextColor(String theme, boolean nightMode) {
        String safe = normalizeTheme(theme);
        if ("light".equals(safe)) {
            return Color.rgb(90, 97, 114);
        }
        if ("auto".equals(safe)) {
            return nightMode ? Color.rgb(172, 176, 186) : Color.rgb(90, 97, 114);
        }
        return Color.rgb(172, 176, 186);
    }

    public static int normalizeArtworkRadius(int value) {
        if (value < 12) {
            return 12;
        }
        if (value > 48) {
            return 48;
        }
        return value;
    }

    public static boolean isProgressStyleMinimal(String value) {
        return "minimal".equalsIgnoreCase(value);
    }
}
