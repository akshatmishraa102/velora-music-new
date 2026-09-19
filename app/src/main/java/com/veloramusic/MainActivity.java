package com.veloramusic;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int AUDIO_PERMISSION = 20;
    private static final String PREF_NAME = "velora_ui";
    private static final String KEY_ACCENT = "accent";
    private static final String KEY_THEME = "theme";

    private final List<Song> songs = new ArrayList<>();

    private MediaController controller;
    private TextView nowTitle;
    private TextView nowArtist;
    private Button playButton;
    private LinearLayout content;
    private View navigationView;
    private int selectedTabIndex = 0;
    private SharedPreferences preferences;
    private String currentTheme = "dark";
    private int searchResultsIndex = 0;
    private int libraryResultsIndex = 0;

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
        currentTheme = preferences.getString(KEY_THEME, currentTheme);
    }

    private void savePreferences() {
        if (preferences == null) {
            preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        }
        preferences.edit().putInt(KEY_ACCENT, accent).putString(KEY_THEME, currentTheme).apply();
    }

    private int resolveBackgroundColor() {
        switch (currentTheme) {
            case "amoled":
                return Color.rgb(0, 0, 0);
            case "light":
                return Color.rgb(245, 245, 250);
            case "auto":
                return Color.rgb(10, 11, 15);
            case "dark":
            default:
                return Color.rgb(9, 10, 14);
        }
    }

    private int resolveSurfaceColor() {
        switch (currentTheme) {
            case "amoled":
                return Color.rgb(13, 13, 15);
            case "light":
                return Color.rgb(255, 255, 255);
            case "auto":
                return Color.rgb(17, 18, 23);
            case "dark":
            default:
                return Color.rgb(18, 18, 25);
        }
    }

    private int resolveMutedColor() {
        switch (currentTheme) {
            case "light":
                return Color.rgb(102, 108, 126);
            case "amoled":
            case "auto":
            case "dark":
            default:
                return Color.rgb(157, 161, 176);
        }
    }

    private int resolvePrimaryTextColor() {
        return currentTheme.equals("light") ? Color.rgb(17, 21, 30) : Color.rgb(245, 245, 247);
    }

    private int resolveSecondaryTextColor() {
        return currentTheme.equals("light") ? Color.rgb(90, 97, 114) : Color.rgb(172, 176, 186);
    }

    private int resolveCardStrokeColor() {
        return currentTheme.equals("light") ? Color.argb(35, 26, 31, 44) : Color.argb(30, 255, 255, 255);
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
        root.addView(buildMiniPlayer());
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
            View child = bar.getChildAt(i);
            if (!(child instanceof LinearLayout)) {
                continue;
            }

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
        bar.setPadding(dp(12), dp(8), dp(12), dp(12));

        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(resolveSurfaceColor());
        navBg.setCornerRadius(dp(28));
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
            item.setPadding(0, dp(8), 0, dp(8));
            item.setBackground(selected ? round(accent, 18) : null);
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

        GradientDrawable miniBg = new GradientDrawable();
        miniBg.setColor(resolveSurfaceColor());
        miniBg.setCornerRadius(dp(24));
        miniBg.setStroke(dp(1), resolveCardStrokeColor());
        bar.setBackground(miniBg);

        TextView art = textView("♫", Color.WHITE, 22f);
        art.setGravity(Gravity.CENTER);
        art.setBackground(round(accent, 16));
        art.setPadding(dp(10), dp(10), dp(10), dp(10));

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(12), 0, dp(10), 0);

        nowTitle = textView("Nothing playing", resolvePrimaryTextColor(), 15f);
        nowTitle.setSingleLine(true);
        nowTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nowTitle.setTypeface(null, Typeface.BOLD);

        nowArtist = textView("Velora Music", resolveSecondaryTextColor(), 12f);
        nowArtist.setSingleLine(true);
        nowArtist.setEllipsize(android.text.TextUtils.TruncateAt.END);

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

        bar.addView(art, new LinearLayout.LayoutParams(dp(52), dp(52)));
        bar.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1f));
        bar.addView(playButton, new LinearLayout.LayoutParams(dp(42), dp(42)));
        bar.addView(queueButton, new LinearLayout.LayoutParams(dp(40), dp(40)));

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

        TextView welcome = textView(greeting, resolveSecondaryTextColor(), 13f);
        welcome.setTypeface(null, Typeface.BOLD);
        welcome.setPadding(0, dp(4), 0, 0);
        content.addView(welcome);

        TextView title = textView("VELORA", resolvePrimaryTextColor(), 30f);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(10));
        content.addView(title);

        addSectionTitle("Featured");
        content.addView(featuredCard(), new LinearLayout.LayoutParams(-1, dp(210)));

        addSectionTitle("Recently played");
        if (songs.isEmpty()) {
            content.addView(emptyCard("Your recent listens will appear here."));
        } else {
            HorizontalScrollView recentScroll = new HorizontalScrollView(this);
            recentScroll.setHorizontalScrollBarEnabled(false);
            recentScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout recentRow = new LinearLayout(this);
            recentRow.setOrientation(LinearLayout.HORIZONTAL);
            recentRow.setPadding(0, 0, 0, dp(10));
            for (int i = 0; i < Math.min(7, songs.size()); i++) {
                recentRow.addView(buildMediaCard(songs.get(i), i % 2 == 0), new LinearLayout.LayoutParams(dp(170), -2));
            }
            recentScroll.addView(recentRow);
            content.addView(recentScroll, new LinearLayout.LayoutParams(-1, -2));
        }

        addSectionTitle("Albums");
        if (songs.isEmpty()) {
            content.addView(emptyCard("Add music to fill your album shelf."));
        } else {
            HorizontalScrollView albumScroll = new HorizontalScrollView(this);
            albumScroll.setHorizontalScrollBarEnabled(false);
            albumScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout albumRow = new LinearLayout(this);
            albumRow.setOrientation(LinearLayout.HORIZONTAL);
            albumRow.setPadding(0, 0, 0, dp(10));
            for (int i = 0; i < Math.min(6, songs.size()); i++) {
                albumRow.addView(buildMediaCard(songs.get(i), i % 2 == 0), new LinearLayout.LayoutParams(dp(170), -2));
            }
            albumScroll.addView(albumRow);
            content.addView(albumScroll, new LinearLayout.LayoutParams(-1, -2));
        }

        addSectionTitle("Quick picks");
        HorizontalScrollView quickScroll = new HorizontalScrollView(this);
        quickScroll.setHorizontalScrollBarEnabled(false);
        quickScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setPadding(0, 0, 0, dp(8));
        if (songs.isEmpty()) {
            quickRow.addView(emptyCard("Add tracks to build your quick mix."), new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (int i = 0; i < Math.min(5, songs.size()); i++) {
                quickRow.addView(buildMediaCard(songs.get(i), i % 2 == 0), new LinearLayout.LayoutParams(dp(170), -2));
            }
        }
        quickScroll.addView(quickRow);
        content.addView(quickScroll, new LinearLayout.LayoutParams(-1, -2));
    }

    private View buildMediaCard(Song song, boolean strongAccent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(round(resolveSurfaceColor(), 20));
        card.setBackgroundDrawable(round(resolveSurfaceColor(), 20));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(170), -2);
        params.setMargins(0, 0, dp(12), 0);
        card.setLayoutParams(params);

        TextView art = textView(song.title.substring(0, 1).toUpperCase(Locale.US), Color.WHITE, 24f);
        art.setGravity(Gravity.CENTER);
        art.setBackground(round(strongAccent ? accent : Color.argb(165, 255, 255, 255), 18));
        art.setPadding(dp(12), dp(12), dp(12), dp(12));
        art.setLayoutParams(new LinearLayout.LayoutParams(dp(150), dp(150)));

        TextView title = textView(song.title, resolvePrimaryTextColor(), 14f);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(0, dp(8), 0, dp(2));

        TextView artist = textView(song.artist, resolveSecondaryTextColor(), 12f);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);

        card.addView(art);
        card.addView(title);
        card.addView(artist);
        card.setOnClickListener(v -> playSong(song));
        return card;
    }

    private void showSearch() {
        selectedTabIndex = 1;
        updateNavigationSelection();
        clearContent();
        heading("Search", "Browse your library and recent picks.");

        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setOrientation(LinearLayout.HORIZONTAL);
        searchWrap.setBackground(round(resolveSurfaceColor(), 18));
        searchWrap.setPadding(dp(14), dp(8), dp(10), dp(8));

        final EditText search = new EditText(this);
        search.setHint("Song, artist, album...");
        search.setSingleLine(true);
        search.setTextColor(resolvePrimaryTextColor());
        search.setHintTextColor(resolveSecondaryTextColor());
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(10), dp(10), dp(10), dp(10));

        Button clear = new Button(this);
        clear.setText("Clear");
        clear.setTextColor(accent);
        clear.setBackgroundColor(Color.TRANSPARENT);
        clear.setOnClickListener(v -> search.setText(""));

        searchWrap.addView(search, new LinearLayout.LayoutParams(0, -2, 1f));
        searchWrap.addView(clear, new LinearLayout.LayoutParams(-2, -2));
        content.addView(searchWrap, new LinearLayout.LayoutParams(-1, -2));

        TextView suggHeader = textView("Suggestions", resolveSecondaryTextColor(), 12f);
        suggHeader.setTypeface(null, Typeface.BOLD);
        suggHeader.setPadding(0, dp(18), 0, dp(10));
        content.addView(suggHeader);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, 0, 0, dp(10));
        String[] chipLabels = {"All", "Trending", "Favorites", "Night drive", "Acoustic"};
        for (final String chip : chipLabels) {
            TextView item = textView(chip, resolvePrimaryTextColor(), 12f);
            item.setBackground(round(resolveSurfaceColor(), 999));
            item.setPadding(dp(14), dp(8), dp(14), dp(8));
            item.setOnClickListener(v -> search.setText(chip.equals("All") ? "" : chip));
            chips.addView(item, new LinearLayout.LayoutParams(-2, -2));
        }
        content.addView(chips);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                renderSearch(s.toString());
            }
        });

        searchResultsIndex = content.getChildCount();
        renderSearch("");
    }

    private void showLibrary() {
        selectedTabIndex = 2;
        updateNavigationSelection();
        clearContent();

        heading("Library", "Local + licensed online music.");

        LinearLayout statRow = new LinearLayout(this);
        statRow.setOrientation(LinearLayout.HORIZONTAL);
        statRow.setPadding(0, dp(4), 0, dp(12));

        String[] labels = {"Tracks", "Artists", "Playlists"};
        int[] values = {Math.max(1, songs.size()), Math.max(1, Math.min(12, songs.size())), 4};

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

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(0, dp(6), 0, dp(12));

        String[] tabs = {"Songs", "Albums", "Artists", "Playlists"};
        for (String tab : tabs) {
            TextView item = textView(tab, resolvePrimaryTextColor(), 12f);
            item.setBackground(round(resolveSurfaceColor(), 999));
            item.setPadding(dp(14), dp(10), dp(14), dp(10));
            item.setOnClickListener(v -> renderLibrary(tab));
            toolbar.addView(item, new LinearLayout.LayoutParams(-2, -2));
        }

        content.addView(toolbar);

        Button refresh = new Button(this);
        refresh.setText("Rescan local music");
        refresh.setTextColor(Color.WHITE);
        refresh.setBackground(round(accent, 16));
        refresh.setOnClickListener(v -> {
            loadLocalSongs();
            renderLibrary("Songs");
        });

        content.addView(refresh);
        libraryResultsIndex = content.getChildCount();
        renderLibrary("Songs");
    }

    private void renderLibrary(String filter) {
        while (content.getChildCount() > libraryResultsIndex) {
            content.removeViewAt(content.getChildCount() - 1);
        }

        if (songs.isEmpty()) {
            content.addView(emptyCard("No local tracks found yet. Grant music access or add a licensed stream."));
            return;
        }

        switch (filter) {
            case "Albums":
                LinearLayout albums = new LinearLayout(this);
                albums.setOrientation(LinearLayout.VERTICAL);
                for (int i = 0; i < Math.min(8, songs.size()); i++) {
                    Song song = songs.get(i);
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(12), dp(8), dp(12), dp(8));
                    row.setBackground(round(resolveSurfaceColor(), 18));
                    row.setOnClickListener(v -> playSong(song));

                    TextView art = textView("◉", resolvePrimaryTextColor(), 20f);
                    art.setBackground(round(accent, 14));
                    art.setGravity(Gravity.CENTER);
                    art.setPadding(dp(12), dp(12), dp(12), dp(12));
                    row.addView(art, new LinearLayout.LayoutParams(dp(42), dp(42)));

                    LinearLayout info = new LinearLayout(this);
                    info.setOrientation(LinearLayout.VERTICAL);
                    info.setPadding(dp(12), 0, 0, 0);
                    TextView title = textView(song.artist + " Collection", resolvePrimaryTextColor(), 15f);
                    title.setTypeface(null, Typeface.BOLD);
                    TextView subtitle = textView(song.title, resolveSecondaryTextColor(), 12f);
                    info.addView(title);
                    info.addView(subtitle);
                    row.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
                    albums.addView(row, new LinearLayout.LayoutParams(-1, -2));
                }
                content.addView(albums);
                break;
            case "Artists":
                LinearLayout artists = new LinearLayout(this);
                artists.setOrientation(LinearLayout.VERTICAL);
                for (int i = 0; i < Math.min(8, songs.size()); i++) {
                    Song song = songs.get(i);
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(12), dp(8), dp(12), dp(8));
                    row.setBackground(round(resolveSurfaceColor(), 18));
                    row.setOnClickListener(v -> playSong(song));

                    TextView art = textView(song.artist.substring(0, 1).toUpperCase(Locale.US), Color.WHITE, 18f);
                    art.setBackground(round(accent, 14));
                    art.setGravity(Gravity.CENTER);
                    art.setPadding(dp(12), dp(12), dp(12), dp(12));
                    row.addView(art, new LinearLayout.LayoutParams(dp(42), dp(42)));

                    LinearLayout info = new LinearLayout(this);
                    info.setOrientation(LinearLayout.VERTICAL);
                    info.setPadding(dp(12), 0, 0, 0);
                    TextView name = textView(song.artist, resolvePrimaryTextColor(), 15f);
                    name.setTypeface(null, Typeface.BOLD);
                    TextView count = textView("1 track in library", resolveSecondaryTextColor(), 12f);
                    info.addView(name);
                    info.addView(count);
                    row.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
                    artists.addView(row, new LinearLayout.LayoutParams(-1, -2));
                }
                content.addView(artists);
                break;
            case "Playlists":
                LinearLayout playlists = new LinearLayout(this);
                playlists.setOrientation(LinearLayout.VERTICAL);
                String[] playlistNames = {"Favorites", "Fresh Finds", "Offline Mix", "Night Drive"};
                for (int i = 0; i < playlistNames.length; i++) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(12), dp(10), dp(12), dp(10));
                    row.setBackground(round(resolveSurfaceColor(), 18));
                    TextView art = textView(String.valueOf(i + 1), Color.WHITE, 18f);
                    art.setBackground(round(accent, 14));
                    art.setGravity(Gravity.CENTER);
                    art.setPadding(dp(12), dp(12), dp(12), dp(12));
                    row.addView(art, new LinearLayout.LayoutParams(dp(42), dp(42)));
                    LinearLayout info = new LinearLayout(this);
                    info.setOrientation(LinearLayout.VERTICAL);
                    info.setPadding(dp(12), 0, 0, 0);
                    TextView name = textView(playlistNames[i], resolvePrimaryTextColor(), 15f);
                    name.setTypeface(null, Typeface.BOLD);
                    TextView count = textView(Math.min(10, songs.size()) + " tracks", resolveSecondaryTextColor(), 12f);
                    info.addView(name);
                    info.addView(count);
                    row.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
                    playlists.addView(row, new LinearLayout.LayoutParams(-1, -2));
                }
                content.addView(playlists);
                break;
            case "Songs":
            default:
                LinearLayout songsList = new LinearLayout(this);
                songsList.setOrientation(LinearLayout.VERTICAL);
                for (Song song : songs) {
                    songsList.addView(songRow(song), new LinearLayout.LayoutParams(-1, compactHeight));
                }
                content.addView(songsList);
                break;
        }
    }

    private void showCustomise() {
        selectedTabIndex = 3;
        updateNavigationSelection();
        clearContent();
        heading("Customise", "Tune the app to your listening mood.");

        addSectionTitle("Appearance");
        LinearLayout themeCard = new LinearLayout(this);
        themeCard.setOrientation(LinearLayout.VERTICAL);
        themeCard.setBackground(round(resolveSurfaceColor(), 20));
        themeCard.setPadding(dp(12), dp(12), dp(12), dp(12));

        String[] modes = {"Dark", "AMOLED", "Light", "Auto"};
        for (String mode : modes) {
            TextView option = textView(mode, resolvePrimaryTextColor(), 14f);
            option.setPadding(dp(10), dp(10), dp(10), dp(10));
            option.setBackground(round(resolveBackgroundColor(), 12));
            option.setOnClickListener(v -> {
                currentTheme = mode.toLowerCase(Locale.US);
                if (currentTheme.equals("auto")) currentTheme = "dark";
                savePreferences();
                buildUi();
                connectController();
            });
            themeCard.addView(option, new LinearLayout.LayoutParams(-1, -2));
        }
        content.addView(themeCard);

        addSectionTitle("Accent");
        LinearLayout accentWrap = new LinearLayout(this);
        accentWrap.setOrientation(LinearLayout.HORIZONTAL);
        accentWrap.setGravity(Gravity.CENTER_VERTICAL);
        accentWrap.setPadding(0, 0, 0, dp(8));

        int[] colors = {
                Color.rgb(184, 167, 255),
                Color.rgb(88, 188, 255),
                Color.rgb(72, 226, 187),
                Color.rgb(255, 144, 110),
                Color.rgb(255, 120, 154)
        };

        for (int color : colors) {
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
        content.addView(accentWrap);

        addSectionTitle("Playback");
        TextView playback = textView("Modern player interactions, artwork navigation, and premium dark surfaces remain active in the player screen.", resolveSecondaryTextColor(), 13f);
        playback.setPadding(0, 0, 0, dp(12));
        content.addView(playback);

        addSectionTitle("About Velora");
        TextView about = textView("Velora Music is a lightweight Android music player built for local library playback, licensed streams, and premium dark-mode listening.", resolveSecondaryTextColor(), 13f);
        about.setPadding(0, 0, 0, dp(12));
        content.addView(about);

        Button sourceButton = new Button(this);
        sourceButton.setText("Open-source contribution");
        sourceButton.setTextColor(Color.WHITE);
        sourceButton.setBackground(round(accent, 16));
        sourceButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"));
            startActivity(intent);
        });
        content.addView(sourceButton);

        Button instaButton = new Button(this);
        instaButton.setText("Akshat Mishra on Instagram");
        instaButton.setTextColor(Color.WHITE);
        instaButton.setBackground(round(resolveSurfaceColor(), 16));
        instaButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/akshatmishra/"));
            startActivity(intent);
        });
        content.addView(instaButton);
    }

    private void renderSearch(String query) {
        if (content == null) {
            return;
        }

        while (content.getChildCount() > searchResultsIndex) {
            content.removeViewAt(content.getChildCount() - 1);
        }

        String lower = query == null ? "" : query.toLowerCase(Locale.US);
        List<Song> matches = new ArrayList<>();
        for (Song song : songs) {
            if (query == null || query.isEmpty() || song.title.toLowerCase(Locale.US).contains(lower) || song.artist.toLowerCase(Locale.US).contains(lower)) {
                matches.add(song);
            }
        }

        if (matches.isEmpty()) {
            TextView empty = textView("No matches found in your library.", resolveSecondaryTextColor(), 13f);
            empty.setPadding(dp(8), dp(12), 0, dp(12));
            content.addView(empty);
            return;
        }

        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        for (Song song : matches) {
            section.addView(songRow(song), new LinearLayout.LayoutParams(-1, compactHeight));
        }
        content.addView(section);
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
        if (controller == null) {
            return;
        }

        MediaItem item =
                new MediaItem.Builder()
                        .setUri(Uri.parse(song.uri))
                        .setMediaMetadata(
                                new MediaMetadata.Builder()
                                        .setTitle(song.title)
                                        .setArtist(song.artist)
                                        .build()
                        )
                        .build();

        controller.setMediaItem(item);
        controller.prepare();
        controller.play();

        nowTitle.setText(song.title);
        nowArtist.setText(song.artist);
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
                                        playButton.setText(
                                                isPlaying
                                                        ? "Ⅱ"
                                                        : "▶"
                                        );
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

                                        nowTitle.setText(
                                                title == null
                                                        ? "Nothing playing"
                                                        : title
                                        );

                                        nowArtist.setText(
                                                artist == null
                                                        ? "Velora Music"
                                                        : artist
                                        );
                                    }
                                }
                        );

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
                MediaStore.Audio.Media._ID
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

                if (title == null || title.isEmpty()) {
                    title = "Unknown Song";
                }

                if (artist == null || artist.isEmpty()) {
                    artist = "Unknown Artist";
                }

                songs.add(
                        new Song(
                                title,
                                artist,
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                        + "/"
                                        + id,
                                false
                        )
                );
            }

        } catch (Exception ignored) {
        }
    }

    private static class Song {

        final String title;
        final String artist;
        final String uri;
        final boolean online;

        Song(
                String title,
                String artist,
                String uri,
                boolean online
        ) {
            this.title = title;
            this.artist = artist;
            this.uri = uri;
            this.online = online;
        }
    }
}
