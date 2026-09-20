package com.veloramusic;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int AUDIO_PERMISSION = 20;
    private static final String PREF_NAME = VeloraThemeManager.PREF_NAME;
    private static final String KEY_ACCENT = VeloraThemeManager.KEY_ACCENT;
    private static final String KEY_THEME = VeloraThemeManager.KEY_THEME;

    private final List<Song> songs = new ArrayList<>();
    private final List<Song> recentlyPlayed = new ArrayList<>();
    private Song currentSong;

    private MediaController controller;
    private View miniPlayerView;
    private ImageView miniPlayerArtwork;
    private TextView miniPlayerFallback;
    private TextView nowTitle;
    private TextView nowArtist;
    private Button playButton;
    private LinearLayout content;
    private View navigationView;
    private int selectedTabIndex = 0;
    private SharedPreferences preferences;
    private String currentTheme = "dark";
    private boolean pureBlack = false;
    private int searchResultsIndex = 0;
    private int libraryResultsIndex = 0;
    private String currentLibraryFilter = "Songs";
    private String currentLibraryQuery = "";
    private EditText librarySearchField;
    private final Handler searchUiHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;

    private int accent = Color.rgb(184, 167, 255);

    private int compactHeight;
    private int compactArtwork;
    private int compactHorizontalPadding;
    private int compactVerticalPadding;
    private int standardWidth;
    private int standardArtwork;
    private int featuredHeight;
    private int featuredPadding;
    private int featuredPlayButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        loadPreferences();
        loadDimensions();
        buildUi();
        requestAudioPermission();
        connectController();
    }

    private void loadPreferences() {
        accent = preferences.getInt(KEY_ACCENT, accent);
        currentTheme = VeloraThemeManager.normalizeTheme(preferences.getString(KEY_THEME, currentTheme));
        pureBlack = preferences.getBoolean(VeloraThemeManager.KEY_PURE_BLACK, false);
    }

    private void savePreferences() {
        if (preferences == null) {
            preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        }
        preferences.edit()
                .putInt(KEY_ACCENT, accent)
                .putString(KEY_THEME, currentTheme)
                .putBoolean(VeloraThemeManager.KEY_PURE_BLACK, pureBlack)
                .apply();
    }

    private boolean isSystemNightMode() {
        android.app.UiModeManager uiModeManager =
                (android.app.UiModeManager) getSystemService(android.content.Context.UI_MODE_SERVICE);
        if (uiModeManager == null) {
            return false;
        }
        return uiModeManager.getNightMode() == android.app.UiModeManager.MODE_NIGHT_YES;
    }

    private int resolveBackgroundColor() {
        return VeloraThemeManager.resolveBackgroundColor(currentTheme, pureBlack, isSystemNightMode());
    }

    private int resolveSurfaceColor() {
        return VeloraThemeManager.resolveSurfaceColor(currentTheme, pureBlack, isSystemNightMode());
    }

    private int resolveMutedColor() {
        return VeloraThemeManager.resolveSecondaryTextColor(currentTheme, isSystemNightMode());
    }

    private int resolvePrimaryTextColor() {
        return VeloraThemeManager.resolvePrimaryTextColor(currentTheme, isSystemNightMode());
    }

    private int resolveSecondaryTextColor() {
        return VeloraThemeManager.resolveSecondaryTextColor(currentTheme, isSystemNightMode());
    }

    private int resolveCardStrokeColor() {
        if (currentTheme.equals("light")) {
            return Color.argb(35, 26, 31, 44);
        }
        if (currentTheme.equals("auto")) {
            return isSystemNightMode() ? Color.argb(30, 255, 255, 255) : Color.argb(35, 26, 31, 44);
        }
        return Color.argb(30, 255, 255, 255);
    }

    private void loadDimensions() {
        compactHeight = getResources().getDimensionPixelSize(
                R.dimen.velora_card_compact_height);

        compactArtwork = getResources().getDimensionPixelSize(
                R.dimen.velora_card_compact_artwork);

        compactHorizontalPadding = getResources().getDimensionPixelSize(
                R.dimen.velora_card_compact_padding_horizontal);

        compactVerticalPadding = getResources().getDimensionPixelSize(
                R.dimen.velora_card_compact_padding_vertical);

        standardWidth = getResources().getDimensionPixelSize(
                R.dimen.velora_card_standard_width);

        standardArtwork = getResources().getDimensionPixelSize(
                R.dimen.velora_card_standard_artwork);

        featuredHeight = getResources().getDimensionPixelSize(
                R.dimen.velora_card_featured_height);

        featuredPadding = getResources().getDimensionPixelSize(
                R.dimen.velora_card_featured_padding);

        featuredPlayButton = getResources().getDimensionPixelSize(
                R.dimen.velora_card_featured_play_button);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();
        if (content != null) {
            buildUi();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(resolveBackgroundColor());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(Color.TRANSPARENT);
        content.setPadding(
                getResources().getDimensionPixelSize(
                        R.dimen.velora_screen_horizontal),
                dp(22),
                getResources().getDimensionPixelSize(
                        R.dimen.velora_screen_horizontal),
                dp(28)
        );

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.addView(content);

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        miniPlayerView = buildMiniPlayer();
        root.addView(miniPlayerView);
        navigationView = buildNavigation();
        root.addView(navigationView);

        setContentView(root);
        showHome();
    }

    private void updateNavigationSelection() {
        if (!(navigationView instanceof LinearLayout)) {
            return;
        }

        LinearLayout bar = (LinearLayout) navigationView;
        for (int i = 0; i < bar.getChildCount(); i++) {
            View childView = bar.getChildAt(i);
            if (!(childView instanceof LinearLayout)) {
                continue;
            }

            LinearLayout child = (LinearLayout) childView;
            boolean selected = i == selectedTabIndex;
            child.setBackground(selected ? round(accent, 18) : null);

            if (child.getChildCount() >= 2) {
                TextView icon = (TextView) child.getChildAt(0);
                TextView label = (TextView) child.getChildAt(1);
                icon.setTextColor(selected ? Color.WHITE : resolveSecondaryTextColor());
                label.setTextColor(selected ? Color.WHITE : resolveSecondaryTextColor());
            }
        }
    }

    private View buildNavigation() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(resolveSurfaceColor());
        navBg.setCornerRadius(dp(32));
        navBg.setStroke(dp(1), resolveCardStrokeColor());
        bar.setBackground(navBg);

        String[] labels = {"HOME", "SEARCH", "LIBRARY", "CUSTOM"};
        String[] icons = {"⌂", "⌕", "▣", "⚙"};

        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            boolean selected = i == selectedTabIndex;

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(10), dp(8), dp(10), dp(8));
            item.setBackground(selected ? round(accent, 18) : round(Color.argb(0, 0, 0, 0), 18));
            item.setOnClickListener(v -> {
                selectedTabIndex = index;
                updateNavigationSelection();
                if (index == 0) showHome();
                if (index == 1) showSearch();
                if (index == 2) showLibrary();
                if (index == 3) showCustomise();
            });

            TextView iconText = textView(icons[i], selected ? Color.WHITE : resolveSecondaryTextColor(), 18f);
            iconText.setGravity(Gravity.CENTER);
            iconText.setPadding(0, dp(4), 0, dp(2));

            TextView label = textView(labels[i], selected ? Color.WHITE : resolveSecondaryTextColor(), 10f);
            label.setTypeface(null, Typeface.BOLD);
            label.setGravity(Gravity.CENTER);

            item.addView(iconText, new LinearLayout.LayoutParams(-1, -2));
            item.addView(label, new LinearLayout.LayoutParams(-1, -2));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
            params.setMargins(dp(4), 0, dp(4), 0);
            bar.addView(item, params);
        }

        return bar;
    }

    private View buildMiniPlayer() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(10), dp(12), dp(10));
        bar.setOnClickListener(v -> startActivity(new Intent(this, PlayerActivity.class)));
        bar.setVisibility(View.GONE);

        GradientDrawable miniBg = new GradientDrawable();
        miniBg.setColor(resolveSurfaceColor());
        miniBg.setCornerRadius(dp(28));
        miniBg.setStroke(dp(1), resolveCardStrokeColor());
        bar.setBackground(miniBg);

        FrameLayout artWrap = new FrameLayout(this);
        artWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(52)));
        artWrap.setBackground(round(accent, 18));

        miniPlayerArtwork = new ImageView(this);
        miniPlayerArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        miniPlayerArtwork.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        miniPlayerFallback = textView("V", Color.WHITE, 18f);
        miniPlayerFallback.setGravity(Gravity.CENTER);
        miniPlayerFallback.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        artWrap.addView(miniPlayerArtwork);
        artWrap.addView(miniPlayerFallback);

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(12), 0, dp(10), 0);

        TextView label = textView("NOW PLAYING", resolveSecondaryTextColor(), 9f);
        label.setTypeface(null, Typeface.BOLD);
        label.setLetterSpacing(0.12f);

        nowTitle = textView("Nothing playing", resolvePrimaryTextColor(), 15f);
        nowTitle.setSingleLine(true);
        nowTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nowTitle.setTypeface(null, Typeface.BOLD);

        nowArtist = textView("Velora Music", resolveSecondaryTextColor(), 12f);
        nowArtist.setSingleLine(true);
        nowArtist.setEllipsize(android.text.TextUtils.TruncateAt.END);

        textBox.addView(label);
        textBox.addView(nowTitle);
        textBox.addView(nowArtist);

        playButton = new Button(this);
        playButton.setText("▶");
        playButton.setTextColor(Color.WHITE);
        playButton.setTextSize(18);
        playButton.setBackground(round(accent, 999));
        playButton.setOnClickListener(v -> togglePlayback());

        ImageButton queueButton = new ImageButton(this);
        queueButton.setImageResource(android.R.drawable.ic_menu_sort_by_size);
        queueButton.setBackground(round(Color.argb(22, 255, 255, 255), 999));
        queueButton.setColorFilter(resolvePrimaryTextColor());
        queueButton.setPadding(dp(8), dp(8), dp(8), dp(8));
        queueButton.setOnClickListener(v -> startActivity(new Intent(this, PlayerActivity.class)));

        bar.addView(artWrap);
        bar.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1f));
        bar.addView(playButton, new LinearLayout.LayoutParams(dp(42), dp(42)));
        bar.addView(queueButton, new LinearLayout.LayoutParams(dp(40), dp(40)));

        updateNowPlayingUi();
        return bar;
    }

    private TextView textView(String text, int color, float size) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(color);
        v.setTextSize(size);
        return v;
    }

    private void clearContent() {
        content.removeAllViews();
        content.setPadding(
                getResources().getDimensionPixelSize(R.dimen.velora_screen_horizontal),
                dp(22),
                getResources().getDimensionPixelSize(R.dimen.velora_screen_horizontal),
                dp(28)
        );
    }

    private TextView heading(String text, String subtitle) {
        TextView h = textView(text, resolvePrimaryTextColor(), 30f);
        h.setTypeface(null, Typeface.BOLD);
        h.setPadding(0, dp(4), 0, dp(6));
        content.addView(h);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = textView(subtitle, resolveSecondaryTextColor(), 13f);
            sub.setPadding(0, 0, 0, dp(14));
            content.addView(sub, new LinearLayout.LayoutParams(-1, -2));
        }

        return h;
    }

    private void addSectionTitle(String title) {
        TextView section = textView(title, resolvePrimaryTextColor(), 20f);
        section.setTypeface(null, Typeface.BOLD);
        section.setPadding(0, dp(22), 0, dp(12));
        content.addView(section);
    }

    private View featuredCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{accent, Color.rgb(17, 17, 22), Color.rgb(12, 12, 17)}
        );
        background.setCornerRadius(dp(28));
        card.setBackground(background);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, 0, dp(18), 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView eyebrow = textView("PERSONAL MIX", Color.argb(210, 255, 255, 255), 10f);
        eyebrow.setTypeface(null, Typeface.BOLD);
        eyebrow.setLetterSpacing(0.18f);

        String primaryTitle = songs.isEmpty() ? "Start listening" : "Your evening mix";
        String primarySubtitle = songs.isEmpty()
                ? "Build your home with your own library."
                : "A hand-picked mix from your music library.";

        TextView title = textView(primaryTitle, Color.WHITE, 26f);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(8), 0, dp(6));

        TextView subtitle = textView(primarySubtitle, Color.argb(205, 255, 255, 255), 13f);

        info.addView(eyebrow);
        info.addView(title);
        info.addView(subtitle);

        TextView cover = textView(songs.isEmpty() ? "♪" : "V", Color.WHITE, 30f);
        cover.setGravity(Gravity.CENTER);
        cover.setBackground(round(Color.argb(35, 255, 255, 255), 22));
        cover.setPadding(dp(18), dp(18), dp(18), dp(18));
        cover.setLayoutParams(new LinearLayout.LayoutParams(dp(90), dp(90)));

        Button play = new Button(this);
        play.setText("▶");
        play.setTextColor(Color.WHITE);
        play.setTextSize(18);
        play.setBackground(round(Color.argb(175, 255, 255, 255), 999));
        play.setOnClickListener(v -> {
            if (!songs.isEmpty()) {
                playSong(songs.get(0));
            } else {
                showLibrary();
            }
        });

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(Gravity.CENTER);
        actions.addView(cover);
        actions.addView(play, new LinearLayout.LayoutParams(dp(52), dp(52)));

        card.addView(info);
        card.addView(actions);
        return card;
    }

    private View emptyCard(String message) {
        TextView empty = textView(message, resolveSecondaryTextColor(), 13f);
        empty.setPadding(dp(16), dp(16), dp(16), dp(16));
        empty.setBackground(round(resolveSurfaceColor(), 16));
        return empty;
    }

    private void showHome() {
        selectedTabIndex = 0;
        updateNavigationSelection();
        clearContent();

        String greeting = "Good evening";
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            greeting = "Good morning";
        } else if (hour >= 18) {
            greeting = "Good evening";
        } else {
            greeting = "Good afternoon";
        }

        TextView welcome = textView(greeting, resolveSecondaryTextColor(), 12f);
        welcome.setTypeface(null, Typeface.BOLD);
        welcome.setLetterSpacing(0.08f);
        welcome.setPadding(0, dp(6), 0, dp(4));
        content.addView(welcome);

        TextView title = textView("VELORA", resolvePrimaryTextColor(), 30f);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(10));
        content.addView(title);

        LinearLayout moodBar = new LinearLayout(this);
        moodBar.setOrientation(LinearLayout.HORIZONTAL);
        moodBar.setPadding(0, 0, 0, dp(18));
        String[] moodLabels = {"Chill", "Focus", "Drive", "Night"};
        for (String mood : moodLabels) {
            TextView moodChip = textView(mood, resolvePrimaryTextColor(), 11f);
            moodChip.setBackground(round(resolveSurfaceColor(), 999));
            moodChip.setPadding(dp(12), dp(8), dp(12), dp(8));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, -2);
            chipParams.setMargins(0, 0, dp(8), 0);
            moodBar.addView(moodChip, chipParams);
        }
        content.addView(moodBar);

        Song activeSong = getCurrentSongFromPlayer();
        if (activeSong != null || !songs.isEmpty()) {
            Song heroSong = activeSong != null ? activeSong : songs.get(0);
            FrameLayout heroCard = new FrameLayout(this);
            heroCard.setPadding(0, 0, 0, dp(18));
            heroCard.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

            Bitmap heroArt = loadArtworkBitmap(heroSong.albumArtUri, dp(220));
            int gradientBase = heroArt != null ? extractProminentColor(heroArt) : accent;
            int darkBase = Color.argb(255, Math.max(10, Color.red(gradientBase) / 3), Math.max(10, Color.green(gradientBase) / 3), Math.max(18, Color.blue(gradientBase) / 3));

            GradientDrawable heroBackground = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{
                            Color.argb(230, Color.red(gradientBase), Color.green(gradientBase), Color.blue(gradientBase)),
                            Color.argb(205, 18, 18, 24),
                            Color.argb(240, 9, 9, 12)
                    }
            );
            heroBackground.setCornerRadius(dp(30));
            heroCard.setBackground(heroBackground);

            ImageView heroImage = new ImageView(this);
            heroImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            heroImage.setAlpha(0.24f);
            heroImage.setLayoutParams(new FrameLayout.LayoutParams(-1, dp(200)));
            if (heroArt != null) {
                heroImage.setImageBitmap(heroArt);
            }
            heroCard.addView(heroImage);

            LinearLayout contentWrap = new LinearLayout(this);
            contentWrap.setOrientation(LinearLayout.HORIZONTAL);
            contentWrap.setGravity(Gravity.CENTER_VERTICAL);
            contentWrap.setPadding(dp(18), dp(18), dp(18), dp(18));
            contentWrap.setLayoutParams(new FrameLayout.LayoutParams(-1, dp(200)));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            info.setPadding(0, dp(8), dp(16), 0);

            TextView heroEyebrow = textView("CONTINUE LISTENING", Color.argb(220, 255, 255, 255), 10f);
            heroEyebrow.setTypeface(null, Typeface.BOLD);
            heroEyebrow.setLetterSpacing(0.14f);

            TextView heroTitle = textView(heroSong.title, Color.WHITE, 25f);
            heroTitle.setTypeface(null, Typeface.BOLD);
            heroTitle.setSingleLine(true);
            heroTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            heroTitle.setPadding(0, dp(8), 0, dp(6));

            TextView heroSubtitle = textView(heroSong.artist, Color.argb(205, 255, 255, 255), 13f);
            heroSubtitle.setSingleLine(true);
            heroSubtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);

            info.addView(heroEyebrow);
            info.addView(heroTitle);
            info.addView(heroSubtitle);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.VERTICAL);
            actions.setGravity(Gravity.CENTER);

            FrameLayout artWrap = new FrameLayout(this);
            artWrap.setBackground(round(Color.argb(30, 255, 255, 255), 22));
            artWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(84), dp(84)));

            ImageView heroThumb = new ImageView(this);
            heroThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            heroThumb.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            if (heroArt != null) {
                heroThumb.setImageBitmap(heroArt);
            } else {
                TextView fallback = textView(String.valueOf(heroSong.title.charAt(0)).toUpperCase(Locale.US), Color.WHITE, 26f);
                fallback.setGravity(Gravity.CENTER);
                fallback.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
                artWrap.addView(fallback);
            }
            artWrap.addView(heroThumb);

            Button heroPlay = new Button(this);
            heroPlay.setText(controller != null && controller.isPlaying() ? "Ⅱ" : "▶");
            heroPlay.setTextColor(Color.WHITE);
            heroPlay.setTextSize(18);
            heroPlay.setBackground(round(Color.argb(200, 255, 255, 255), 999));
            heroPlay.setOnClickListener(v -> playSong(heroSong));
            LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(dp(52), dp(52));
            playParams.setMargins(0, dp(12), 0, 0);

            actions.addView(artWrap, new LinearLayout.LayoutParams(dp(84), dp(84)));
            actions.addView(heroPlay, playParams);

            contentWrap.addView(info);
            contentWrap.addView(actions);
            heroCard.addView(contentWrap);
            content.addView(heroCard);
        }

        if (!recentlyPlayed.isEmpty()) {
            addSectionTitle("Recently played");
            HorizontalScrollView recentScroll = new HorizontalScrollView(this);
            recentScroll.setHorizontalScrollBarEnabled(false);
            recentScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout recentRow = new LinearLayout(this);
            recentRow.setOrientation(LinearLayout.HORIZONTAL);
            recentRow.setPadding(0, 0, 0, dp(12));
            for (int i = 0; i < Math.min(8, recentlyPlayed.size()); i++) {
                recentRow.addView(buildMediaCard(recentlyPlayed.get(i), i % 2 == 0), new LinearLayout.LayoutParams(dp(170), -2));
            }
            recentScroll.addView(recentRow);
            content.addView(recentScroll, new LinearLayout.LayoutParams(-1, -2));
        }

        if (songs.size() > 0) {
            addSectionTitle("Recently added");
            HorizontalScrollView addedScroll = new HorizontalScrollView(this);
            addedScroll.setHorizontalScrollBarEnabled(false);
            addedScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout addedRow = new LinearLayout(this);
            addedRow.setOrientation(LinearLayout.HORIZONTAL);
            addedRow.setPadding(0, 0, 0, dp(12));
            for (int i = 0; i < Math.min(6, songs.size()); i++) {
                addedRow.addView(buildMediaCard(songs.get(i), i % 2 == 0), new LinearLayout.LayoutParams(dp(170), -2));
            }
            addedScroll.addView(addedRow);
            content.addView(addedScroll, new LinearLayout.LayoutParams(-1, -2));

            addSectionTitle("Albums");
            HorizontalScrollView albumScroll = new HorizontalScrollView(this);
            albumScroll.setHorizontalScrollBarEnabled(false);
            albumScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout albumRow = new LinearLayout(this);
            albumRow.setOrientation(LinearLayout.HORIZONTAL);
            albumRow.setPadding(0, 0, 0, dp(12));
            for (int i = 0; i < Math.min(6, songs.size()); i++) {
                albumRow.addView(buildMediaCard(songs.get(i), i % 2 == 0), new LinearLayout.LayoutParams(dp(170), -2));
            }
            albumScroll.addView(albumRow);
            content.addView(albumScroll, new LinearLayout.LayoutParams(-1, -2));

            addSectionTitle("Artists");
            HorizontalScrollView artistScroll = new HorizontalScrollView(this);
            artistScroll.setHorizontalScrollBarEnabled(false);
            artistScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout artistRow = new LinearLayout(this);
            artistRow.setOrientation(LinearLayout.HORIZONTAL);
            artistRow.setPadding(0, 0, 0, dp(12));
            for (int i = 0; i < Math.min(6, songs.size()); i++) {
                artistRow.addView(buildArtistCard(songs.get(i)), new LinearLayout.LayoutParams(dp(140), -2));
            }
            artistScroll.addView(artistRow);
            content.addView(artistScroll, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private void addSettingsButton(LinearLayout container, String label, View.OnClickListener listener) {
        TextView action = textView(label, resolvePrimaryTextColor(), 14f);
        action.setPadding(dp(14), dp(12), dp(14), dp(12));
        action.setBackground(round(resolveBackgroundColor(), 14));
        action.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(6));
        container.addView(action, params);
    }

    private View buildMediaCard(Song song, boolean strongAccent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(round(resolveSurfaceColor(), 20));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(170), -2);
        params.setMargins(0, 0, dp(12), 0);
        card.setLayoutParams(params);

        FrameLayout artWrap = new FrameLayout(this);
        artWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(150), dp(150)));
        artWrap.setPadding(0, 0, 0, dp(6));
        artWrap.setBackground(round(strongAccent ? accent : Color.argb(165, 255, 255, 255), 18));

        ImageView artImage = new ImageView(this);
        artImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artImage.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        Bitmap albumArt = loadArtworkBitmap(song.albumArtUri, dp(150));
        if (albumArt != null) {
            artImage.setImageBitmap(albumArt);
        }
        artWrap.addView(artImage);

        TextView fallback = textView(
                song.title != null && !song.title.isEmpty() ? String.valueOf(song.title.charAt(0)).toUpperCase(Locale.US) : "V",
                Color.WHITE,
                24f
        );
        fallback.setGravity(Gravity.CENTER);
        fallback.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        if (albumArt == null) {
            artWrap.addView(fallback);
        }

        TextView title = textView(song.title, resolvePrimaryTextColor(), 14f);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(0, dp(8), 0, dp(2));

        TextView artist = textView(song.artist, resolveSecondaryTextColor(), 12f);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);

        card.addView(artWrap);
        card.addView(title);
        card.addView(artist);
        card.setOnClickListener(v -> playSong(song));
        return card;
    }

    private View buildArtistCard(Song song) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(round(resolveSurfaceColor(), 20));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(140), -2);
        params.setMargins(0, 0, dp(10), 0);
        card.setLayoutParams(params);

        FrameLayout avatar = new FrameLayout(this);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(120), dp(120)));
        avatar.setBackground(round(accent, 18));

        TextView initial = textView(
                song.artist != null && !song.artist.isEmpty() ? String.valueOf(song.artist.charAt(0)).toUpperCase(Locale.US) : "V",
                Color.WHITE,
                26f
        );
        initial.setGravity(Gravity.CENTER);
        initial.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        avatar.addView(initial);

        TextView name = textView(song.artist, resolvePrimaryTextColor(), 13f);
        name.setTypeface(null, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(0, dp(8), 0, 0);

        card.addView(avatar);
        card.addView(name);
        card.setOnClickListener(v -> playSong(song));
        return card;
    }

    private void showSearch() {
        selectedTabIndex = 1;
        updateNavigationSelection();
        clearContent();

        TextView headingText = textView("Search", resolvePrimaryTextColor(), 30f);
        headingText.setTypeface(null, Typeface.BOLD);
        headingText.setPadding(0, dp(6), 0, dp(10));
        content.addView(headingText);

        TextView subtitle = textView("Browse your library and jump back into the sound you love.", resolveSecondaryTextColor(), 13f);
        subtitle.setPadding(0, 0, 0, dp(18));
        content.addView(subtitle);

        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setOrientation(LinearLayout.HORIZONTAL);
        searchWrap.setBackground(round(resolveSurfaceColor(), 22));
        searchWrap.setPadding(dp(14), dp(10), dp(10), dp(10));
        searchWrap.setGravity(Gravity.CENTER_VERTICAL);
        searchWrap.setElevation(dp(1));
        searchWrap.setTag("search_field_tag");

        ImageView searchIcon = new ImageView(this);
        searchIcon.setImageResource(android.R.drawable.ic_menu_search);
        searchIcon.setColorFilter(resolveSecondaryTextColor());
        searchIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));

        final EditText search = new EditText(this);
        search.setHint("Search songs, artists, albums...");
        search.setSingleLine(true);
        search.setTextColor(resolvePrimaryTextColor());
        search.setHintTextColor(resolveSecondaryTextColor());
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(12), dp(10), dp(12), dp(10));
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setTextSize(15f);

        Button clear = new Button(this);
        clear.setText("Clear");
        clear.setTextColor(accent);
        clear.setBackgroundColor(Color.TRANSPARENT);
        clear.setOnClickListener(v -> {
            search.setText("");
            search.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        searchWrap.addView(searchIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        searchWrap.addView(search, new LinearLayout.LayoutParams(0, -2, 1f));
        searchWrap.addView(clear, new LinearLayout.LayoutParams(-2, -2));
        content.addView(searchWrap, new LinearLayout.LayoutParams(-1, -2));

        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                renderSearch(v.getText().toString());
                return true;
            }
            return false;
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                final String value = s == null ? "" : s.toString();
                if (pendingSearchRunnable != null) {
                    searchUiHandler.removeCallbacks(pendingSearchRunnable);
                }
                if (value == null || value.trim().isEmpty()) {
                    renderSearch("");
                    return;
                }
                renderSearchLoading(value);
                pendingSearchRunnable = () -> renderSearch(value);
                searchUiHandler.postDelayed(pendingSearchRunnable, 180L);
            }
        });

        searchResultsIndex = content.getChildCount();
        renderSearch("");
    }

    private void showLibrary() {
        selectedTabIndex = 2;
        updateNavigationSelection();
        clearContent();

        TextView headingText = textView("Library", resolvePrimaryTextColor(), 30f);
        headingText.setTypeface(null, Typeface.BOLD);
        headingText.setPadding(0, dp(6), 0, dp(8));
        content.addView(headingText);

        TextView subtitle = textView("Your collection, sorted by what you actually listen to.", resolveSecondaryTextColor(), 13f);
        subtitle.setPadding(0, 0, 0, dp(18));
        content.addView(subtitle);

        LinearLayout statRow = new LinearLayout(this);
        statRow.setOrientation(LinearLayout.HORIZONTAL);
        statRow.setPadding(0, dp(4), 0, dp(12));

        int uniqueArtists = countUniqueArtists();
        int albumsCount = countUniqueAlbums();
        String[] labels = {"Tracks", "Artists", "Albums"};
        int[] values = {Math.max(0, songs.size()), Math.max(0, uniqueArtists), Math.max(0, albumsCount)};

        for (int i = 0; i < labels.length; i++) {
            LinearLayout stat = new LinearLayout(this);
            stat.setOrientation(LinearLayout.VERTICAL);
            stat.setBackground(round(resolveSurfaceColor(), 18));
            stat.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
            params.setMargins(i == 0 ? 0 : dp(8), 0, 0, 0);

            TextView value = textView(String.valueOf(values[i]), resolvePrimaryTextColor(), 18f);
            value.setTypeface(null, Typeface.BOLD);
            TextView label = textView(labels[i], resolveSecondaryTextColor(), 11f);

            stat.addView(value);
            stat.addView(label);
            statRow.addView(stat, params);
        }
        content.addView(statRow);

        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setOrientation(LinearLayout.HORIZONTAL);
        searchWrap.setBackground(round(resolveSurfaceColor(), 22));
        searchWrap.setPadding(dp(12), dp(8), dp(8), dp(8));
        searchWrap.setGravity(Gravity.CENTER_VERTICAL);

        ImageView searchIcon = new ImageView(this);
        searchIcon.setImageResource(android.R.drawable.ic_menu_search);
        searchIcon.setColorFilter(resolveSecondaryTextColor());
        searchIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));

        librarySearchField = new EditText(this);
        librarySearchField.setHint("Search your library");
        librarySearchField.setSingleLine(true);
        librarySearchField.setTextColor(resolvePrimaryTextColor());
        librarySearchField.setHintTextColor(resolveSecondaryTextColor());
        librarySearchField.setBackgroundColor(Color.TRANSPARENT);
        librarySearchField.setTextSize(14f);
        librarySearchField.setPadding(dp(10), dp(8), dp(10), dp(8));
        librarySearchField.setText(currentLibraryQuery);
        librarySearchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                currentLibraryQuery = s == null ? "" : s.toString();
                renderLibrary(currentLibraryFilter);
            }
        });

        Button clear = new Button(this);
        clear.setText("Clear");
        clear.setTextColor(accent);
        clear.setBackgroundColor(Color.TRANSPARENT);
        clear.setOnClickListener(v -> librarySearchField.setText(""));

        searchWrap.addView(searchIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));
        searchWrap.addView(librarySearchField, new LinearLayout.LayoutParams(0, -2, 1f));
        searchWrap.addView(clear, new LinearLayout.LayoutParams(-2, -2));
        content.addView(searchWrap, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackground(round(resolveSurfaceColor(), 18));
        toolbar.setPadding(dp(8), dp(8), dp(8), dp(8));
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        List<String> tabs = new ArrayList<>();
        tabs.add("Songs");
        tabs.add("Albums");
        tabs.add("Artists");

        for (String tab : tabs) {
            TextView item = textView(tab, tab.equals(currentLibraryFilter) ? Color.WHITE : resolvePrimaryTextColor(), 12f);
            item.setBackground(tab.equals(currentLibraryFilter) ? round(accent, 999) : round(resolveSurfaceColor(), 999));
            item.setPadding(dp(14), dp(10), dp(14), dp(10));
            item.setOnClickListener(v -> {
                currentLibraryFilter = tab;
                renderLibrary(tab);
                updateLibraryTabs(toolbar);
            });
            toolbar.addView(item, new LinearLayout.LayoutParams(-2, -2));
        }
        content.addView(toolbar);

        Button refresh = new Button(this);
        refresh.setText("Rescan local music");
        refresh.setTextColor(Color.WHITE);
        refresh.setBackground(round(accent, 16));
        refresh.setOnClickListener(v -> {
            loadLocalSongs();
            currentLibraryFilter = "Songs";
            currentLibraryQuery = "";
            if (librarySearchField != null) {
                librarySearchField.setText("");
            }
            renderLibrary("Songs");
        });
        content.addView(refresh);

        libraryResultsIndex = content.getChildCount();
        renderLibrary("Songs");
    }

    private void updateLibraryTabs(LinearLayout toolbar) {
        if (toolbar == null) {
            return;
        }
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View item = toolbar.getChildAt(i);
            if (item instanceof TextView) {
                String label = ((TextView) item).getText().toString();
                boolean selected = label.equals(currentLibraryFilter);
                item.setBackground(selected ? round(accent, 999) : round(resolveSurfaceColor(), 999));
                ((TextView) item).setTextColor(selected ? Color.WHITE : resolvePrimaryTextColor());
            }
        }
    }

    private List<Song> filterSongsForLibrary(List<Song> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        String query = currentLibraryQuery == null ? "" : currentLibraryQuery.trim();
        if (query.isEmpty()) {
            return new ArrayList<>(source);
        }
        String lower = query.toLowerCase(Locale.US);
        List<Song> matches = new ArrayList<>();
        for (Song song : source) {
            if (song.title.toLowerCase(Locale.US).contains(lower)
                    || song.artist.toLowerCase(Locale.US).contains(lower)
                    || (song.albumArtUri != null && song.albumArtUri.toLowerCase(Locale.US).contains(lower))) {
                matches.add(song);
            }
        }
        return matches;
    }

    private int countUniqueArtists() {
        List<String> names = new ArrayList<>();
        for (Song song : songs) {
            String key = song.artist == null ? "Unknown Artist" : song.artist.trim();
            if (!key.isEmpty() && !names.contains(key)) {
                names.add(key);
            }
        }
        return names.size();
    }

    private int countUniqueAlbums() {
        List<String> keys = new ArrayList<>();
        for (Song song : songs) {
            String key = song.albumArtUri != null && !song.albumArtUri.trim().isEmpty()
                    ? song.albumArtUri
                    : song.artist + "::" + song.title;
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }
        return keys.size();
    }

    private void renderLibrary(String filter) {
        while (content.getChildCount() > libraryResultsIndex) {
            content.removeViewAt(content.getChildCount() - 1);
        }

        List<Song> filtered = filterSongsForLibrary(songs);
        if (filtered.isEmpty()) {
            content.addView(libraryEmptyState(currentLibraryQuery.trim().isEmpty()
                    ? "No music is available in your library yet. Add tracks or grant access to your local music."
                    : "No results match this search in your library."));
            return;
        }

        switch (filter) {
            case "Albums":
                List<Song> albumList = new ArrayList<>();
                List<String> albumKeys = new ArrayList<>();
                for (Song song : filtered) {
                    String key = song.albumArtUri != null && !song.albumArtUri.trim().isEmpty()
                            ? song.albumArtUri
                            : song.artist + "::" + song.title;
                    if (!albumKeys.contains(key)) {
                        albumKeys.add(key);
                        albumList.add(song);
                    }
                }

                LinearLayout albumSection = new LinearLayout(this);
                albumSection.setOrientation(LinearLayout.VERTICAL);
                for (int i = 0; i < albumList.size(); i++) {
                    Song song = albumList.get(i);
                    albumSection.addView(buildLibraryAlbumRow(song, i), new LinearLayout.LayoutParams(-1, -2));
                }
                content.addView(albumSection);
                break;
            case "Artists":
                LinearLayout artistSection = new LinearLayout(this);
                artistSection.setOrientation(LinearLayout.VERTICAL);
                List<String> artistNames = new ArrayList<>();
                for (Song song : filtered) {
                    String artist = song.artist == null || song.artist.trim().isEmpty() ? "Unknown Artist" : song.artist.trim();
                    if (!artistNames.contains(artist)) {
                        artistNames.add(artist);
                    }
                }
                for (int i = 0; i < artistNames.size(); i++) {
                    final String artist = artistNames.get(i);
                    int count = 0;
                    Song sample = null;
                    for (Song song : filtered) {
                        if (song.artist != null && song.artist.equals(artist)) {
                            count++;
                            if (sample == null) {
                                sample = song;
                            }
                        }
                    }
                    if (sample != null) {
                        artistSection.addView(buildLibraryArtistRow(sample, artist, count, i), new LinearLayout.LayoutParams(-1, -2));
                    }
                }
                content.addView(artistSection);
                break;
            case "Songs":
            default:
                LinearLayout songsList = new LinearLayout(this);
                songsList.setOrientation(LinearLayout.VERTICAL);
                for (int i = 0; i < filtered.size(); i++) {
                    View row = buildLibraryTrackRow(filtered.get(i), i);
                    songsList.addView(row, new LinearLayout.LayoutParams(-1, -2));
                }
                content.addView(songsList);
                break;
        }
    }

    private View libraryEmptyState(String message) {
        LinearLayout emptyState = new LinearLayout(this);
        emptyState.setOrientation(LinearLayout.VERTICAL);
        emptyState.setPadding(dp(18), dp(18), dp(18), dp(18));
        emptyState.setBackground(round(resolveSurfaceColor(), 22));

        TextView title = textView("Nothing here yet", resolvePrimaryTextColor(), 16f);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(8));

        TextView body = textView(message, resolveSecondaryTextColor(), 13f);
        emptyState.addView(title);
        emptyState.addView(body);
        return emptyState;
    }

    private View buildLibraryTrackRow(final Song song, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(round(resolveSurfaceColor(), 18));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);
        row.setOnClickListener(v -> playSong(song));
        row.setAlpha(0f);
        row.setTranslationY(dp(8));
        row.animate().alpha(1f).translationY(0f).setDuration(120 + (index * 18)).start();

        FrameLayout artWrap = new FrameLayout(this);
        artWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(52)));
        artWrap.setBackground(round(accent, 16));

        ImageView art = new ImageView(this);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        Bitmap artBitmap = loadArtworkBitmap(song.albumArtUri, dp(52));
        if (artBitmap != null) {
            art.setImageBitmap(artBitmap);
        }
        artWrap.addView(art);

        TextView fallback = textView(
                song.title != null && !song.title.isEmpty() ? String.valueOf(song.title.charAt(0)).toUpperCase(Locale.US) : "V",
                Color.WHITE,
                18f
        );
        fallback.setGravity(Gravity.CENTER);
        fallback.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        if (artBitmap == null) {
            artWrap.addView(fallback);
        }

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        textWrap.setPadding(dp(12), 0, dp(8), 0);
        textWrap.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView title = textView(song.title, resolvePrimaryTextColor(), 15f);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView artist = textView(song.artist, resolveSecondaryTextColor(), 12f);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);

        textWrap.addView(title);
        textWrap.addView(artist);

        Button playButton = new Button(this);
        playButton.setText("▶");
        playButton.setTextColor(Color.WHITE);
        playButton.setTextSize(14f);
        playButton.setBackground(round(accent, 999));
        playButton.setOnClickListener(v -> playSong(song));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        buttonParams.setMargins(0, 0, 0, 0);
        playButton.setLayoutParams(buttonParams);

        row.addView(artWrap);
        row.addView(textWrap);
        row.addView(playButton);
        return row;
    }

    private View buildLibraryAlbumRow(final Song song, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(round(resolveSurfaceColor(), 18));
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        row.setOnClickListener(v -> playSong(song));

        FrameLayout artWrap = new FrameLayout(this);
        artWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(54), dp(54)));
        artWrap.setBackground(round(accent, 16));

        ImageView art = new ImageView(this);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        Bitmap artBitmap = loadArtworkBitmap(song.albumArtUri, dp(54));
        if (artBitmap != null) {
            art.setImageBitmap(artBitmap);
        }
        artWrap.addView(art);

        TextView fallback = textView(
                song.title != null && !song.title.isEmpty() ? String.valueOf(song.title.charAt(0)).toUpperCase(Locale.US) : "V",
                Color.WHITE,
                18f
        );
        fallback.setGravity(Gravity.CENTER);
        fallback.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        if (artBitmap == null) {
            artWrap.addView(fallback);
        }

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(10), 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView title = textView(song.title, resolvePrimaryTextColor(), 15f);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView subtitle = textView(song.artist, resolveSecondaryTextColor(), 12f);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);

        info.addView(title);
        info.addView(subtitle);

        Button play = new Button(this);
        play.setText("▶");
        play.setTextColor(Color.WHITE);
        play.setTextSize(14f);
        play.setBackground(round(accent, 999));
        play.setOnClickListener(v -> playSong(song));
        play.setLayoutParams(new LinearLayout.LayoutParams(dp(38), dp(38)));

        row.addView(artWrap);
        row.addView(info);
        row.addView(play);
        return row;
    }

    private View buildLibraryArtistRow(final Song sample, final String artist, int trackCount, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(round(resolveSurfaceColor(), 18));
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        row.setOnClickListener(v -> playSong(sample));

        FrameLayout artWrap = new FrameLayout(this);
        artWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(54), dp(54)));
        artWrap.setBackground(round(accent, 16));

        TextView letter = textView(
                artist != null && !artist.isEmpty() ? String.valueOf(artist.charAt(0)).toUpperCase(Locale.US) : "V",
                Color.WHITE,
                20f
        );
        letter.setGravity(Gravity.CENTER);
        letter.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        artWrap.addView(letter);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(10), 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView title = textView(artist, resolvePrimaryTextColor(), 15f);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView count = textView(trackCount + " track" + (trackCount == 1 ? "" : "s") + " in library", resolveSecondaryTextColor(), 12f);
        count.setSingleLine(true);
        count.setEllipsize(android.text.TextUtils.TruncateAt.END);

        info.addView(title);
        info.addView(count);

        Button play = new Button(this);
        play.setText("▶");
        play.setTextColor(Color.WHITE);
        play.setTextSize(14f);
        play.setBackground(round(accent, 999));
        play.setOnClickListener(v -> playSong(sample));
        play.setLayoutParams(new LinearLayout.LayoutParams(dp(38), dp(38)));

        row.addView(artWrap);
        row.addView(info);
        row.addView(play);
        return row;
    }

    private void showCustomise() {
        selectedTabIndex = 3;
        updateNavigationSelection();
        clearContent();

        TextView headingText = textView("Settings", resolvePrimaryTextColor(), 30f);
        headingText.setTypeface(null, Typeface.BOLD);
        headingText.setPadding(0, dp(6), 0, dp(8));
        content.addView(headingText);

        TextView subtitle = textView("Fine-tune your look, player, and listening flow.", resolveSecondaryTextColor(), 13f);
        subtitle.setPadding(0, 0, 0, dp(18));
        content.addView(subtitle);

        addSectionTitle("Appearance");
        LinearLayout appearanceCard = new LinearLayout(this);
        appearanceCard.setOrientation(LinearLayout.VERTICAL);
        appearanceCard.setBackground(round(resolveSurfaceColor(), 22));
        appearanceCard.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeRow.setGravity(Gravity.CENTER_VERTICAL);
        String[] themes = {"Dark", "AMOLED", "Light", "Auto"};
        for (String mode : themes) {
            final String value = mode.toLowerCase(Locale.US);
            boolean selected = currentTheme.equals(value);
            TextView chip = textView(mode, selected ? Color.WHITE : resolvePrimaryTextColor(), 12f);
            chip.setBackground(selected ? round(accent, 999) : round(resolveBackgroundColor(), 999));
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setOnClickListener(v -> {
                currentTheme = VeloraThemeManager.normalizeTheme(value);
                savePreferences();
                buildUi();
                connectController();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.setMargins(0, 0, dp(8), 0);
            themeRow.addView(chip, params);
        }
        appearanceCard.addView(themeRow);

        LinearLayout accentWrap = new LinearLayout(this);
        accentWrap.setOrientation(LinearLayout.HORIZONTAL);
        accentWrap.setGravity(Gravity.CENTER_VERTICAL);
        accentWrap.setPadding(0, dp(12), 0, 0);
        for (final int color : VeloraThemeManager.ACCENT_COLORS) {
            View chip = new View(this);
            chip.setBackground(round(color, 999));
            int size = dp(28);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(size, size);
            chipParams.setMargins(0, 0, dp(10), 0);
            chip.setOnClickListener(v -> {
                accent = color;
                savePreferences();
                buildUi();
                connectController();
            });
            accentWrap.addView(chip, chipParams);
        }
        appearanceCard.addView(accentWrap);

        LinearLayout pureBlackRow = new LinearLayout(this);
        pureBlackRow.setOrientation(LinearLayout.HORIZONTAL);
        pureBlackRow.setGravity(Gravity.CENTER_VERTICAL);
        pureBlackRow.setPadding(0, dp(14), 0, 0);

        TextView blackTitle = textView("Pure black mode", resolvePrimaryTextColor(), 14f);
        blackTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        boolean blackChecked = prefs != null && prefs.getBoolean(VeloraThemeManager.KEY_PURE_BLACK, false);
        TextView blackValue = textView(blackChecked ? "On" : "Off", resolveSecondaryTextColor(), 12f);
        blackValue.setPadding(dp(10), dp(4), dp(10), dp(4));
        blackValue.setBackground(round(resolveBackgroundColor(), 999));
        blackValue.setOnClickListener(v -> {
            pureBlack = !pureBlack;
            savePreferences();
            buildUi();
        });

        pureBlackRow.addView(blackTitle);
        pureBlackRow.addView(blackValue);
        appearanceCard.addView(pureBlackRow);

        content.addView(appearanceCard);

        addSectionTitle("Player");
        LinearLayout playerCard = new LinearLayout(this);
        playerCard.setOrientation(LinearLayout.VERTICAL);
        playerCard.setBackground(round(resolveSurfaceColor(), 22));
        playerCard.setPadding(dp(12), dp(10), dp(12), dp(10));

        addRealSettingsToggle(playerCard, "Animated artwork", prefs.getBoolean(VeloraThemeManager.KEY_PLAYER_ARTWORK_ANIMATION, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_PLAYER_ARTWORK_ANIMATION, checked).apply());
        addRealSettingsToggle(playerCard, "Dynamic album accent", prefs.getBoolean(VeloraThemeManager.KEY_PLAYER_DYNAMIC_ACCENT, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_PLAYER_DYNAMIC_ACCENT, checked).apply());
        addRealSettingsToggle(playerCard, "Swipe to change track", prefs.getBoolean(VeloraThemeManager.KEY_PLAYER_SWIPE_GESTURE, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_PLAYER_SWIPE_GESTURE, checked).apply());
        addRealSettingsToggle(playerCard, "Show quality badge", prefs.getBoolean(VeloraThemeManager.KEY_SHOW_QUALITY_BADGE, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_SHOW_QUALITY_BADGE, checked).apply());

        LinearLayout styleRow = new LinearLayout(this);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        styleRow.setGravity(Gravity.CENTER_VERTICAL);
        styleRow.setPadding(0, dp(8), 0, 0);

        TextView styleLabel = textView("Progress style", resolvePrimaryTextColor(), 14f);
        styleLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        styleRow.addView(styleLabel);

        String currentProgress = prefs.getString(VeloraThemeManager.KEY_PLAYER_PROGRESS_STYLE, "smooth");
        String[] styles = {"Smooth", "Minimal"};
        for (String style : styles) {
            final String value = style.toLowerCase(Locale.US);
            boolean selected = value.equals(currentProgress);
            TextView option = textView(style, selected ? Color.WHITE : resolvePrimaryTextColor(), 12f);
            option.setBackground(selected ? round(accent, 999) : round(resolveBackgroundColor(), 999));
            option.setPadding(dp(12), dp(6), dp(12), dp(6));
            option.setOnClickListener(v -> prefs.edit().putString(VeloraThemeManager.KEY_PLAYER_PROGRESS_STYLE, value).apply());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.setMargins(0, 0, dp(8), 0);
            styleRow.addView(option, params);
        }
        playerCard.addView(styleRow);

        LinearLayout artworkRow = new LinearLayout(this);
        artworkRow.setOrientation(LinearLayout.HORIZONTAL);
        artworkRow.setGravity(Gravity.CENTER_VERTICAL);
        artworkRow.setPadding(0, dp(12), 0, 0);

        TextView artworkLabel = textView("Artwork shape", resolvePrimaryTextColor(), 14f);
        artworkLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        artworkRow.addView(artworkLabel);

        int[] radiusValues = {18, 30, 42};
        String[] radiusLabels = {"Soft", "Standard", "Sharp"};
        for (int i = 0; i < radiusLabels.length; i++) {
            final int radius = radiusValues[i];
            boolean selected = prefs.getInt(VeloraThemeManager.KEY_PLAYER_ARTWORK_RADIUS, 30) == radius;
            TextView chip = textView(radiusLabels[i], selected ? Color.WHITE : resolvePrimaryTextColor(), 11f);
            chip.setBackground(selected ? round(accent, 999) : round(resolveBackgroundColor(), 999));
            chip.setPadding(dp(10), dp(6), dp(10), dp(6));
            chip.setOnClickListener(v -> prefs.edit().putInt(VeloraThemeManager.KEY_PLAYER_ARTWORK_RADIUS, radius).apply());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.setMargins(0, 0, dp(8), 0);
            artworkRow.addView(chip, params);
        }
        playerCard.addView(artworkRow);
        content.addView(playerCard);

        addSectionTitle("App");
        LinearLayout appCard = new LinearLayout(this);
        appCard.setOrientation(LinearLayout.VERTICAL);
        appCard.setBackground(round(resolveSurfaceColor(), 22));
        appCard.setPadding(dp(12), dp(12), dp(12), dp(12));

        Button sourceButton = new Button(this);
        sourceButton.setText("Open-source contribution");
        sourceButton.setTextColor(Color.WHITE);
        sourceButton.setBackground(round(accent, 16));
        sourceButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"));
            startActivity(intent);
        });
        appCard.addView(sourceButton);

        Button instaButton = new Button(this);
        instaButton.setText("Instagram");
        instaButton.setTextColor(resolvePrimaryTextColor());
        instaButton.setBackground(round(resolveBackgroundColor(), 16));
        instaButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/akshat.mishra102?igsi=a3l3ajF0dnRpcWRz"));
            startActivity(intent);
        });
        appCard.addView(instaButton);
        content.addView(appCard);

        addSectionTitle("About");
        LinearLayout aboutCard = new LinearLayout(this);
        aboutCard.setOrientation(LinearLayout.VERTICAL);
        aboutCard.setBackground(round(resolveSurfaceColor(), 22));
        aboutCard.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView veloraBrand = textView("VELORA MUSIC", resolvePrimaryTextColor(), 22f);
        veloraBrand.setTypeface(null, Typeface.BOLD);
        veloraBrand.setPadding(0, 0, 0, dp(8));

        TextView author = textView("Created by Akshat Mishra", resolveSecondaryTextColor(), 13f);
        author.setPadding(0, 0, 0, dp(4));

        TextView version = textView("Version 1.0.0", resolveSecondaryTextColor(), 13f);
        version.setPadding(0, 0, 0, dp(14));

        aboutCard.addView(veloraBrand);
        aboutCard.addView(author);
        aboutCard.addView(version);
        content.addView(aboutCard);
    }

    private void addRealSettingsToggle(LinearLayout parent, String label, boolean checked, ToggleCallback callback) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, 0);

        TextView title = textView(label, resolvePrimaryTextColor(), 14f);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(title);

        CheckBox box = new CheckBox(this);
        box.setChecked(checked);
        box.setOnCheckedChangeListener((button, isChecked) -> callback.onToggle(isChecked));
        row.addView(box);
        parent.addView(row);
    }

    private interface ToggleCallback {
        void onToggle(boolean checked);
    }

    private void renderSearch(String query) {
        if (content == null) {
            return;
        }

        while (content.getChildCount() > searchResultsIndex) {
            content.removeViewAt(content.getChildCount() - 1);
        }

        String trimmed = query == null ? "" : query.trim();
        String lower = trimmed.toLowerCase(Locale.US);

        List<String> history = getRecentSearchHistory();
        if (!history.isEmpty() && (trimmed.isEmpty() || lower.length() < 2)) {
            TextView recentHeader = textView("Recently searched", resolveSecondaryTextColor(), 12f);
            recentHeader.setTypeface(null, Typeface.BOLD);
            recentHeader.setPadding(0, dp(18), 0, dp(10));
            content.addView(recentHeader);

            HorizontalScrollView historyScroll = new HorizontalScrollView(this);
            historyScroll.setHorizontalScrollBarEnabled(false);
            historyScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout chips = new LinearLayout(this);
            chips.setOrientation(LinearLayout.HORIZONTAL);
            chips.setPadding(0, 0, 0, dp(10));
            for (final String item : history) {
                TextView chip = textView(item, resolvePrimaryTextColor(), 12f);
                chip.setBackground(round(resolveSurfaceColor(), 999));
                chip.setPadding(dp(14), dp(8), dp(14), dp(8));
                chip.setOnClickListener(v -> {
                    EditText searchField = findSearchField();
                    if (searchField != null) {
                        searchField.setText(item);
                        searchField.setSelection(item.length());
                    }
                });
                chips.addView(chip, new LinearLayout.LayoutParams(-2, -2));
            }
            historyScroll.addView(chips);
            content.addView(historyScroll, new LinearLayout.LayoutParams(-1, -2));
        }

        if (trimmed.isEmpty()) {
            if (songs.isEmpty()) {
                content.addView(emptySearchState("Search your library for songs, artists, and albums."));
            } else {
                List<Song> topMatches = new ArrayList<>();
                for (Song song : songs) {
                    if (topMatches.size() >= 6) {
                        break;
                    }
                    topMatches.add(song);
                }
                renderSearchResults(topMatches, true);
            }
            return;
        }

        if (trimmed.length() < 2) {
            renderSearchResults(new ArrayList<>(), false);
            return;
        }

        List<Song> matches = new ArrayList<>();
        for (Song song : songs) {
            if (song.title.toLowerCase(Locale.US).contains(lower) || song.artist.toLowerCase(Locale.US).contains(lower) || (song.albumArtUri != null && song.albumArtUri.toLowerCase(Locale.US).contains(lower))) {
                matches.add(song);
            }
        }

        renderSearchResults(matches, false);
    }

    private void renderSearchLoading(String query) {
        if (content == null || query == null || query.trim().isEmpty()) {
            return;
        }

        while (content.getChildCount() > searchResultsIndex) {
            content.removeViewAt(content.getChildCount() - 1);
        }

        LinearLayout loading = new LinearLayout(this);
        loading.setOrientation(LinearLayout.HORIZONTAL);
        loading.setGravity(Gravity.CENTER_VERTICAL);
        loading.setPadding(dp(16), dp(18), dp(16), dp(18));
        loading.setBackground(round(resolveSurfaceColor(), 20));

        ProgressBar spinner = new ProgressBar(this);
        spinner.getIndeterminateDrawable().setColorFilter(accent, android.graphics.PorterDuff.Mode.SRC_IN);

        TextView label = textView("Searching...", resolveSecondaryTextColor(), 13f);
        label.setPadding(dp(12), 0, 0, 0);

        loading.addView(spinner, new LinearLayout.LayoutParams(dp(28), dp(28)));
        loading.addView(label, new LinearLayout.LayoutParams(-2, -2));
        content.addView(loading);
    }

    private void renderSearchResults(List<Song> matches, boolean isQuickBrowse) {
        if (matches.isEmpty()) {
            content.addView(emptySearchState(isQuickBrowse
                    ? "No tracks in this library yet. Your search will appear here as soon as you add music."
                    : "No results found. Try another title, artist, or album name."));
            return;
        }

        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, dp(12), 0, 0);
        for (int i = 0; i < matches.size(); i++) {
            View row = buildSearchResultRow(matches.get(i), i);
            row.setAlpha(0f);
            row.setTranslationY(dp(10));
            section.addView(row, new LinearLayout.LayoutParams(-1, -2));
            row.animate().alpha(1f).translationY(0f).setDuration(150 + (i * 18)).start();
        }
        content.addView(section);
    }

    private View emptySearchState(String message) {
        LinearLayout emptyState = new LinearLayout(this);
        emptyState.setOrientation(LinearLayout.VERTICAL);
        emptyState.setPadding(dp(16), dp(18), dp(16), dp(18));
        emptyState.setBackground(round(resolveSurfaceColor(), 20));

        TextView title = textView("No matches", resolvePrimaryTextColor(), 16f);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(6));

        TextView body = textView(message, resolveSecondaryTextColor(), 13f);

        emptyState.addView(title);
        emptyState.addView(body);
        return emptyState;
    }

    private View buildSearchResultRow(Song song, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(round(resolveSurfaceColor(), 18));
        row.setOnClickListener(v -> {
            saveRecentSearch(song.title);
            playSong(song);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);

        FrameLayout artWrap = new FrameLayout(this);
        artWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(52)));
        artWrap.setBackground(round(accent, 16));

        ImageView art = new ImageView(this);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        Bitmap artBitmap = loadArtworkBitmap(song.albumArtUri, dp(52));
        if (artBitmap != null) {
            art.setImageBitmap(artBitmap);
        }
        artWrap.addView(art);

        TextView fallback = textView(
                song.title != null && !song.title.isEmpty() ? String.valueOf(song.title.charAt(0)).toUpperCase(Locale.US) : "V",
                Color.WHITE,
                18f
        );
        fallback.setGravity(Gravity.CENTER);
        fallback.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        if (artBitmap == null) {
            artWrap.addView(fallback);
        }

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(10), 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView title = textView(song.title, resolvePrimaryTextColor(), 15f);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView artist = textView(song.artist, resolveSecondaryTextColor(), 12f);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);

        info.addView(title);
        info.addView(artist);

        Button play = new Button(this);
        play.setText("▶");
        play.setTextColor(Color.WHITE);
        play.setTextSize(14f);
        play.setBackground(round(accent, 999));
        play.setOnClickListener(v -> {
            saveRecentSearch(song.title);
            playSong(song);
        });
        play.setLayoutParams(new LinearLayout.LayoutParams(dp(38), dp(38)));

        row.addView(artWrap);
        row.addView(info);
        row.addView(play);
        return row;
    }

    private EditText findSearchField() {
        if (content == null) {
            return null;
        }
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (child instanceof LinearLayout && child.getTag() != null && "search_field_tag".equals(child.getTag())) {
                return (EditText) ((LinearLayout) child).getChildAt(1);
            }
        }
        return null;
    }

    private void saveRecentSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        String value = query.trim();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String raw = prefs.getString("velora_recent_searches", "");
        List<String> history = new ArrayList<>();
        if (!raw.isEmpty()) {
            String[] parts = raw.split("\n");
            for (String part : parts) {
                String next = part.trim();
                if (!next.isEmpty() && !next.equalsIgnoreCase(value)) {
                    history.add(next);
                }
            }
        }
        history.add(0, value);
        while (history.size() > 6) {
            history.remove(history.size() - 1);
        }
        prefs.edit().putString("velora_recent_searches", android.text.TextUtils.join("\n", history)).apply();
    }

    private List<String> getRecentSearchHistory() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String raw = prefs.getString("velora_recent_searches", "");
        List<String> history = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return history;
        }
        String[] parts = raw.split("\n");
        for (String part : parts) {
            String item = part.trim();
            if (!item.isEmpty()) {
                history.add(item);
            }
        }
        return history;
    }

    private void renderSongs(String filter) {
        if (content == null) {
            return;
        }

        while (content.getChildCount() > 5) {
            content.removeViewAt(content.getChildCount() - 1);
        }

        if (songs.isEmpty()) {
            content.addView(emptyCard("No songs found. Grant music permission or add a licensed stream."));
            return;
        }

        for (Song song : songs) {
            content.addView(songRow(song), new LinearLayout.LayoutParams(-1, compactHeight));
        }
    }

    private View songRow(Song song) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(round(resolveSurfaceColor(), 18));
        row.setOnClickListener(v -> playSong(song));

        TextView icon = textView("♫", Color.WHITE, 20f);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(accent, 12));
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));

        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setPadding(dp(12), 0, dp(10), 0);

        TextView title = textView(song.title, resolvePrimaryTextColor(), 15f);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView artist = textView(song.artist, resolveSecondaryTextColor(), 12f);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);

        text.addView(title);
        text.addView(artist);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));

        Button play = new Button(this);
        play.setText("▶");
        play.setTextColor(Color.WHITE);
        play.setTextSize(14);
        play.setBackgroundColor(Color.TRANSPARENT);
        play.setOnClickListener(v -> playSong(song));
        row.addView(play, new LinearLayout.LayoutParams(dp(42), dp(42)));

        return row;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int extractProminentColor(Bitmap bitmap) {
        if (bitmap == null) {
            return accent;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int sampleX = Math.max(1, width / 12);
        int sampleY = Math.max(1, height / 12);
        long r = 0;
        long g = 0;
        long b = 0;
        int count = 0;

        for (int y = 0; y < height; y += sampleY) {
            for (int x = 0; x < width; x += sampleX) {
                int pixel = bitmap.getPixel(x, y);
                r += Color.red(pixel);
                g += Color.green(pixel);
                b += Color.blue(pixel);
                count++;
            }
        }

        if (count == 0) {
            return accent;
        }

        int avgR = (int) (r / count);
        int avgG = (int) (g / count);
        int avgB = (int) (b / count);
        return Color.rgb(clamp(avgR + 16, 20, 255), clamp(avgG + 12, 20, 255), clamp(avgB + 18, 20, 255));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void addStream() {
        final EditText url = new EditText(this);
        url.setHint("https://example.com/audio.mp3");

        final EditText title = new EditText(this);
        title.setHint("Song title");

        final EditText artist = new EditText(this);
        artist.setHint("Artist");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 0, 20, 0);

        box.addView(url);
        box.addView(title);
        box.addView(artist);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Add online stream")
                .setMessage(
                        "Use only audio URLs you own or are licensed to stream."
                )
                .setView(box)
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Add",
                        (d, which) -> {

                            String streamUrl =
                                    url.getText()
                                            .toString()
                                            .trim();

                            if (streamUrl.isEmpty()) {
                                return;
                            }

                            String songTitle =
                                    title.getText()
                                            .toString()
                                            .trim();

                            String songArtist =
                                    artist.getText()
                                            .toString()
                                            .trim();

                            songs.add(
                                    new Song(
                                            songTitle.isEmpty()
                                                    ? "Online Stream"
                                                    : songTitle,
                                            songArtist.isEmpty()
                                                    ? "Unknown Artist"
                                                    : songArtist,
                                            streamUrl,
                                            true
                                    )
                            );

                            showLibrary();
                        }
                )
                .show();
    }

    private void playSong(Song song) {
        if (song == null || song.uri == null || song.uri.isEmpty() || controller == null) {
            return;
        }

        currentSong = song;
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist);

        if (song.albumArtUri != null && !song.albumArtUri.trim().isEmpty()) {
            metadataBuilder.setArtworkUri(Uri.parse(song.albumArtUri));
        }

        MediaItem item =
                new MediaItem.Builder()
                        .setUri(Uri.parse(song.uri))
                        .setMediaMetadata(metadataBuilder.build())
                        .build();

        controller.setMediaItem(item);
        controller.prepare();
        controller.play();

        recordRecentSong(song);
        updateNowPlayingUi();
        if (selectedTabIndex == 0) {
            showHome();
        }
    }

    private void togglePlayback() {
        if (controller == null) {
            return;
        }

        if (controller.isPlaying()) {
            controller.pause();
        } else {
            controller.play();
        }
    }

    private void connectController() {
        SessionToken token =
                new SessionToken(
                        this,
                        new ComponentName(
                                this,
                                PlaybackService.class
                        )
                );

        ListenableFuture<MediaController> future =
                new MediaController.Builder(
                        this,
                        token
                ).buildAsync();

        future.addListener(
                () -> {
                    try {
                        controller = future.get();

                        controller.addListener(
                                new Player.Listener() {

                                    @Override
                                    public void onIsPlayingChanged(
                                            boolean isPlaying
                                    ) {
                                        if (playButton != null) {
                                            playButton.setText(
                                                    isPlaying
                                                            ? "Ⅱ"
                                                            : "▶"
                                            );
                                        }
                                        updateNowPlayingUi();
                                    }

                                    @Override
                                    public void onMediaItemTransition(
                                            MediaItem item,
                                            int reason
                                    ) {
                                        if (item == null) {
                                            return;
                                        }

                                        CharSequence title =
                                                item.mediaMetadata.title;

                                        CharSequence artist =
                                                item.mediaMetadata.artist;

                                        Song itemSong = new Song(
                                                title == null ? "Unknown Song" : title.toString(),
                                                artist == null ? "Unknown Artist" : artist.toString(),
                                                item.localConfiguration != null && item.localConfiguration.uri != null
                                                        ? item.localConfiguration.uri.toString()
                                                        : "",
                                                false,
                                                item.mediaMetadata.artworkUri != null
                                                        ? item.mediaMetadata.artworkUri.toString()
                                                        : null
                                        );
                                        currentSong = itemSong;
                                        recordRecentSong(itemSong);
                                        updateNowPlayingUi();
                                        if (selectedTabIndex == 0) {
                                            showHome();
                                        }
                                    }
                                }
                        );

                        updateNowPlayingUi();

                    } catch (Exception ignored) {
                        controller = null;
                    }
                },
                MoreExecutors.directExecutor()
        );
    }

    private void requestAudioPermission() {
        String permission;

        if (Build.VERSION.SDK_INT >= 33) {
            permission =
                    Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission =
                    Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{permission},
                    AUDIO_PERMISSION
            );

        } else {
            loadLocalSongs();
            if (selectedTabIndex == 0) {
                showHome();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] results
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results
        );

        if (requestCode == AUDIO_PERMISSION
                && results.length > 0
                && results[0]
                == PackageManager.PERMISSION_GRANTED) {

            loadLocalSongs();
            showLibrary();
        }
    }

    private void loadLocalSongs() {
        songs.clear();

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID
        };

        try (
                android.database.Cursor cursor =
                        getContentResolver().query(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                projection,
                                MediaStore.Audio.Media.IS_MUSIC
                                        + " != 0",
                                null,
                                MediaStore.Audio.Media.TITLE
                                        + " COLLATE NOCASE ASC"
                        )
        ) {

            if (cursor == null) {
                return;
            }

            int titleIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.TITLE
                    );

            int artistIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.ARTIST
                    );

            int idIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media._ID
                    );

            int albumIndex =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.ALBUM_ID
                    );

            while (
                    cursor.moveToNext()
                            && songs.size() < 500
            ) {

                long id =
                        cursor.getLong(idIndex);

                String title =
                        cursor.getString(titleIndex);

                String artist =
                        cursor.getString(artistIndex);

                long albumId = albumIndex >= 0 ? cursor.getLong(albumIndex) : 0L;

                if (title == null || title.isEmpty()) {
                    title = "Unknown Song";
                }

                if (artist == null || artist.isEmpty()) {
                    artist = "Unknown Artist";
                }

                String resolvedUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                ).toString();

                String albumArtUri = albumId > 0
                        ? "content://media/external/audio/albumart/" + albumId
                        : null;

                songs.add(
                        new Song(
                                title,
                                artist,
                                resolvedUri,
                                false,
                                albumArtUri
                        )
                );
            }

            if (!songs.isEmpty()) {
                refreshRecentSongsFromLibrary();
            }

        } catch (Exception ignored) {
        }
    }

    private void refreshRecentSongsFromLibrary() {
        if (recentlyPlayed.isEmpty() && !songs.isEmpty()) {
            for (int i = 0; i < Math.min(6, songs.size()); i++) {
                recordRecentSong(songs.get(i));
            }
        }
    }

    private Song getCurrentSongFromPlayer() {
        if (currentSong != null) {
            return currentSong;
        }
        if (controller == null || controller.getCurrentMediaItem() == null) {
            return recentlyPlayed.isEmpty() ? (songs.isEmpty() ? null : songs.get(0)) : recentlyPlayed.get(0);
        }

        MediaItem current = controller.getCurrentMediaItem();
        if (current == null || current.mediaMetadata == null) {
            return null;
        }

        CharSequence title = current.mediaMetadata.title;
        CharSequence artist = current.mediaMetadata.artist;
        String uri = current.localConfiguration != null && current.localConfiguration.uri != null
                ? current.localConfiguration.uri.toString()
                : "";
        currentSong = new Song(
                title == null ? "Unknown Song" : title.toString(),
                artist == null ? "Unknown Artist" : artist.toString(),
                uri,
                false
        );
        return currentSong;
    }

    private void updateNowPlayingUi() {
        Song current = getCurrentSongFromPlayer();
        boolean hasActiveTrack = current != null && controller != null && controller.getCurrentMediaItem() != null;

        if (miniPlayerView != null) {
            miniPlayerView.setVisibility(hasActiveTrack ? View.VISIBLE : View.GONE);
        }

        if (nowTitle != null) {
            if (hasActiveTrack) {
                currentSong = current;
                nowTitle.setText(current.title);
                nowArtist.setText(current.artist);
            } else {
                nowTitle.setText("Nothing playing");
                nowArtist.setText("Velora Music");
            }
        }

        if (playButton != null) {
            playButton.setText(controller != null && controller.isPlaying() ? "Ⅱ" : "▶");
        }

        if (miniPlayerArtwork != null) {
            Bitmap artBitmap = null;
            if (current != null && current.albumArtUri != null && !current.albumArtUri.isEmpty()) {
                artBitmap = loadArtworkBitmap(current.albumArtUri, dp(52));
            }
            if (artBitmap == null && controller != null && controller.getCurrentMediaItem() != null) {
                MediaMetadata metadata = controller.getCurrentMediaItem().mediaMetadata;
                if (metadata != null && metadata.artworkUri != null) {
                    artBitmap = loadArtworkBitmap(metadata.artworkUri.toString(), dp(52));
                }
            }
            if (artBitmap != null) {
                miniPlayerArtwork.setImageBitmap(artBitmap);
                miniPlayerArtwork.setVisibility(View.VISIBLE);
                if (miniPlayerFallback != null) {
                    miniPlayerFallback.setVisibility(View.GONE);
                }
            } else {
                miniPlayerArtwork.setImageDrawable(null);
                miniPlayerArtwork.setVisibility(View.GONE);
                if (miniPlayerFallback != null) {
                    miniPlayerFallback.setVisibility(View.VISIBLE);
                    char fallbackLetter = (current != null && current.title != null && !current.title.trim().isEmpty())
                            ? Character.toUpperCase(current.title.charAt(0))
                            : 'V';
                    miniPlayerFallback.setText(String.valueOf(fallbackLetter));
                }
            }
        }
    }

    private void recordRecentSong(Song song) {
        if (song == null || song.title == null || song.title.trim().isEmpty()) {
            return;
        }

        for (int i = 0; i < recentlyPlayed.size(); i++) {
            Song existing = recentlyPlayed.get(i);
            if (existing != null && existing.title.equals(song.title) && existing.artist.equals(song.artist)) {
                recentlyPlayed.remove(i);
                break;
            }
        }

        recentlyPlayed.add(0, song);
        while (recentlyPlayed.size() > 8) {
            recentlyPlayed.remove(recentlyPlayed.size() - 1);
        }
    }

    private Bitmap loadArtworkBitmap(String artUri, int size) {
        if (artUri == null || artUri.trim().isEmpty()) {
            return null;
        }

        try {
            Uri uri = Uri.parse(artUri);
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                try (java.io.InputStream stream = getContentResolver().openInputStream(uri)) {
                    if (stream == null) {
                        return null;
                    }
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    if (bitmap == null) {
                        return null;
                    }
                    return Bitmap.createScaledBitmap(bitmap, size, size, true);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static class Song {

        final String title;
        final String artist;
        final String uri;
        final boolean online;
        final String albumArtUri;

        Song(
                String title,
                String artist,
                String uri,
                boolean online
        ) {
            this(title, artist, uri, online, null);
        }

        Song(
                String title,
                String artist,
                String uri,
                boolean online,
                String albumArtUri
        ) {
            this.title = title == null ? "Unknown Song" : title;
            this.artist = artist == null ? "Unknown Artist" : artist;
            this.uri = uri == null ? "" : uri;
            this.online = online;
            this.albumArtUri = albumArtUri;
        }
    }
}
