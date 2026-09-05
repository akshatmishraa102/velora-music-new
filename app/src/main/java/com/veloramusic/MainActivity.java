package com.veloramusic;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class MainActivity extends Activity {

    private static final int AUDIO_PERMISSION = 20;

    private final List<Song> songs = new ArrayList<>();

    private MediaController controller;
    private TextView nowTitle;
    private TextView nowArtist;
    private Button playButton;
    private LinearLayout content;

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

        loadDimensions();
        buildUi();
        requestAudioPermission();
        connectController();
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
        root.setBackgroundColor(Color.rgb(11, 11, 15));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                getResources().getDimensionPixelSize(
                        R.dimen.velora_screen_horizontal),
                22,
                getResources().getDimensionPixelSize(
                        R.dimen.velora_screen_horizontal),
                28
        );

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f
                )
        );

        root.addView(buildMiniPlayer());
        root.addView(buildNavigation());

        setContentView(root);

        showHome();
    }

    private View buildNavigation() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(8, 4, 8, 4);
        bar.setBackgroundColor(Color.rgb(18, 18, 23));

        String[] labels = {
                "HOME",
                "SEARCH",
                "LIBRARY",
                "CUSTOM"
        };

        for (int i = 0; i < labels.length; i++) {
            final int index = i;

            TextView item = new TextView(this);
            item.setText(labels[i]);
            item.setTextColor(
                    index == 0 ? accent : Color.rgb(160, 160, 168)
            );
            item.setTextSize(11);
            item.setGravity(Gravity.CENTER);
            item.setTypeface(null, Typeface.BOLD);
            item.setPadding(0, 12, 0, 12);

            item.setOnClickListener(v -> {
                if (index == 0) showHome();
                if (index == 1) showSearch();
                if (index == 2) showLibrary();
                if (index == 3) showCustomise();
            });

            bar.addView(
                    item,
                    new LinearLayout.LayoutParams(
                            0,
                            56,
                            1f
                    )
            );
        }

        return bar;
    }

    private View buildMiniPlayer() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(12, 8, 12, 8);
        bar.setBackgroundColor(Color.rgb(22, 22, 28));

        TextView art = new TextView(this);
        art.setText("♫");
        art.setTextSize(22);
        art.setTextColor(Color.WHITE);
        art.setGravity(Gravity.CENTER);
        art.setBackground(round(accent, 8));

        int miniArtwork = getResources().getDimensionPixelSize(
                R.dimen.velora_mini_player_artwork
        );

        bar.addView(
                art,
                new LinearLayout.LayoutParams(
                        miniArtwork,
                        miniArtwork
                )
        );

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(12, 0, 8, 0);

        nowTitle = textView(
                "Nothing playing",
                Color.rgb(245, 245, 247),
                15
        );

        nowArtist = textView(
                "Velora Music",
                Color.rgb(150, 150, 158),
                12
        );

        nowTitle.setSingleLine(true);
        nowArtist.setSingleLine(true);

        textBox.addView(nowTitle);
        textBox.addView(nowArtist);

        bar.addView(
                textBox,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1f
                )
        );

        playButton = new Button(this);
        playButton.setText("▶");
        playButton.setTextColor(Color.WHITE);
        playButton.setTextSize(18);
        playButton.setBackgroundColor(Color.TRANSPARENT);
        playButton.setOnClickListener(v -> togglePlayback());

        bar.addView(
                playButton,
                new LinearLayout.LayoutParams(
                        54,
                        54
                )
        );

        return bar;
    }

    private TextView textView(
            String text,
            int color,
            float size
    ) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(color);
        v.setTextSize(size);
        return v;
    }

    private void clearContent() {
        content.removeAllViews();

        content.setPadding(
                getResources().getDimensionPixelSize(
                        R.dimen.velora_screen_horizontal),
                22,
                getResources().getDimensionPixelSize(
                        R.dimen.velora_screen_horizontal),
                28
        );
    }

    private TextView heading(
            String text,
            String subtitle
    ) {
        TextView h = textView(
                text,
                Color.rgb(245, 245, 247),
                30
        );

        h.setTypeface(null, Typeface.BOLD);
        h.setPadding(0, 0, 0, 5);

        content.addView(h);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = textView(
                    subtitle,
                    Color.rgb(145, 145, 153),
                    13
            );

            content.addView(
                    sub,
                    new LinearLayout.LayoutParams(
                            -1,
                            -2
                    )
            );
        }

        return h;
    }

    private void showHome() {
        clearContent();

        heading(
                "VELORA",
                "Your music, beautifully organized."
        );

        addSectionTitle("Listen Now");

        content.addView(
                featuredCard(),
                new LinearLayout.LayoutParams(
                        -1,
                        featuredHeight
                )
        );

        addSectionTitle("Recently Played");

        if (songs.isEmpty()) {
            content.addView(
                    emptyCard(
                            "Your recently played music will appear here."
                    )
            );
        } else {
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);

            int count = Math.min(5, songs.size());

            for (int i = 0; i < count; i++) {
                list.addView(
                        songRow(songs.get(i)),
                        new LinearLayout.LayoutParams(
                                -1,
                                compactHeight
                        )
                );
            }

            content.addView(list);
        }

        addSectionTitle("Your Library");

        Button library = new Button(this);
        library.setText("Open Library");
        library.setTextColor(Color.WHITE);
        library.setOnClickListener(v -> showLibrary());

        content.addView(library);
    }

    private void addSectionTitle(String title) {
        TextView section = textView(
                title,
                Color.rgb(245, 245, 247),
                20
        );

        section.setTypeface(null, Typeface.BOLD);

        int topGap = getResources().getDimensionPixelSize(
                R.dimen.velora_section_gap
        );

        int bottomGap = getResources().getDimensionPixelSize(
                R.dimen.velora_section_content_gap
        );

        section.setPadding(
                0,
                topGap,
                0,
                bottomGap
        );

        content.addView(section);
    }

    private View featuredCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.BOTTOM);
        card.setPadding(
                featuredPadding,
                featuredPadding,
                featuredPadding,
                featuredPadding
        );

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(48, 44, 63),
                        Color.rgb(20, 20, 26)
                }
        );

        background.setCornerRadius(
                getResources().getDimension(
                        R.dimen.velora_card_featured_radius
                )
        );

        card.setBackground(background);

        TextView eyebrow = textView(
                "MADE FOR YOU",
                Color.rgb(205, 205, 213),
                11
        );

        eyebrow.setTypeface(null, Typeface.BOLD);

        TextView title = textView(
                songs.isEmpty()
                        ? "Start listening"
                        : "Your music mix",
                Color.WHITE,
                22
        );

        title.setTypeface(null, Typeface.BOLD);

        TextView subtitle = textView(
                songs.isEmpty()
                        ? "Add music to build your personal space."
                        : "A selection from your music library.",
                Color.rgb(205, 205, 213),
                13
        );

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);

        labels.addView(eyebrow);
        labels.addView(title);
        labels.addView(subtitle);

        bottom.addView(
                labels,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1f
                )
        );

        Button play = new Button(this);
        play.setText("▶");
        play.setTextColor(Color.WHITE);
        play.setTextSize(18);
        play.setBackground(round(accent, 999));

        play.setOnClickListener(v -> {
            if (!songs.isEmpty()) {
                playSong(songs.get(0));
            } else {
                showLibrary();
            }
        });

        bottom.addView(
                play,
                new LinearLayout.LayoutParams(
                        featuredPlayButton,
                        featuredPlayButton
                )
        );

        card.addView(bottom);

        return card;
    }

    private View emptyCard(String message) {
        TextView empty = textView(
                message,
                Color.rgb(150, 150, 158),
                13
        );

        empty.setPadding(
                14,
                14,
                14,
                14
        );

        empty.setBackground(
                round(Color.rgb(22, 22, 28), 16)
        );

        return empty;
    }

    private void showSearch() {
        clearContent();

        heading(
                "Search",
                "Find music in your library."
        );

        EditText search = new EditText(this);
        search.setHint("Song, artist, album...");
        search.setSingleLine(true);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.rgb(130, 130, 138));
        search.setPadding(16, 0, 16, 0);
        search.setBackground(
                round(Color.rgb(24, 24, 30), 16)
        );

        content.addView(
                search,
                new LinearLayout.LayoutParams(
                        -1,
                        54
                )
        );

        Button add = new Button(this);
        add.setText("+ Add licensed stream URL");
        add.setTextColor(Color.WHITE);
        add.setOnClickListener(v -> addStream());

        content.addView(add);

        search.setOnEditorActionListener(
                (v, actionId, event) -> {
                    renderSearch(
                            search.getText().toString()
                    );
                    return true;
                }
        );

        addSectionTitle("Results");

        renderSongs("All");
    }

    private void showLibrary() {
        clearContent();

        heading(
                "Library",
                "Local + licensed online music."
        );

        Button refresh = new Button(this);
        refresh.setText("Rescan local music");
        refresh.setTextColor(Color.WHITE);

        refresh.setOnClickListener(v -> {
            loadLocalSongs();
            showLibrary();
        });

        content.addView(refresh);

        addSectionTitle("Songs");

        renderSongs("All");
    }

    private void showCustomise() {
        clearContent();

        heading(
                "Customise",
                "Make Velora yours."
        );

        addSectionTitle("Accent Theme");

        String[] names = {
                "Midnight",
                "Ocean",
                "Neon",
                "Sunset",
                "Rose"
        };

        int[] colors = {
                Color.rgb(184, 167, 255),
                Color.rgb(89, 195, 255),
                Color.rgb(125, 255, 138),
                Color.rgb(255, 155, 114),
                Color.rgb(255, 127, 167)
        };

        for (int i = 0; i < names.length; i++) {
            Button b = new Button(this);
            b.setText(names[i]);
            b.setTextColor(Color.WHITE);

            final int c = colors[i];

            b.setOnClickListener(v -> {
                accent = c;
                buildUi();
                connectController();
            });

            content.addView(b);
        }

        addSectionTitle("Animation");

        SeekBar intensity = new SeekBar(this);
        intensity.setMax(2);
        intensity.setProgress(1);

        content.addView(intensity);

        TextView info = textView(
                "Velora keeps animations lightweight so the interface stays smooth on lower-end Android devices.",
                Color.rgb(145, 145, 153),
                13
        );

        info.setPadding(0, 8, 0, 0);

        content.addView(info);
    }

    private void renderSearch(String query) {
        while (content.getChildCount() > 5) {
            content.removeViewAt(5);
        }

        String lower = query.toLowerCase();

        for (Song song : songs) {
            if (query.isEmpty()
                    || song.title.toLowerCase().contains(lower)
                    || song.artist.toLowerCase().contains(lower)) {

                content.addView(
                        songRow(song),
                        new LinearLayout.LayoutParams(
                                -1,
                                compactHeight
                        )
                );
            }
        }
    }

    private void renderSongs(String filter) {
        for (Song song : songs) {
            content.addView(
                    songRow(song),
                    new LinearLayout.LayoutParams(
                            -1,
                            compactHeight
                    )
            );
        }

        if (songs.isEmpty()) {
            content.addView(
                    emptyCard(
                            "No songs found. Grant music permission or add a licensed stream."
                    )
            );
        }
    }

    private View songRow(Song song) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.setPadding(
                compactHorizontalPadding,
                compactVerticalPadding,
                compactHorizontalPadding,
                compactVerticalPadding
        );

        row.setBackground(
                round(Color.rgb(20, 20, 26), 16)
        );

        TextView icon = textView(
                "♫",
                Color.WHITE,
                21
        );

        icon.setGravity(Gravity.CENTER);
        icon.setBackground(
                round(accent, 12)
        );

        row.addView(
                icon,
                new LinearLayout.LayoutParams(
                        compactArtwork,
                        compactArtwork
                )
        );

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setPadding(
                getResources().getDimensionPixelSize(
                        R.dimen.velora_artwork_title_gap
                ),
                0,
                8,
                0
        );

        TextView title = textView(
                song.title,
                Color.rgb(245, 245, 247),
                15
        );

        title.setTextAppearance(
                this,
                R.style.VeloraCardTitle
        );

        title.setSingleLine(true);

        TextView artist = textView(
                song.artist,
                Color.rgb(150, 150, 158),
                12
        );

        artist.setTextAppearance(
                this,
                R.style.VeloraCardArtist
        );

        artist.setSingleLine(true);

        text.addView(title);
        text.addView(artist);

        row.addView(
                text,
                new LinearLayout.LayoutParams(
                        0,
                        -1,
                        1f
                )
        );

        Button play = new Button(this);
        play.setText("▶");
        play.setTextColor(Color.WHITE);
        play.setTextSize(14);
        play.setBackgroundColor(Color.TRANSPARENT);

        play.setOnClickListener(
                v -> playSong(song)
        );

        row.addView(
                play,
                new LinearLayout.LayoutParams(
                        48,
                        48
                )
        );

        row.setOnClickListener(
                v -> playSong(song)
        );

        return row;
    }

    private GradientDrawable round(
            int color,
            int radiusDp
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);

        drawable.setCornerRadius(
                radiusDp * getResources()
                        .getDisplayMetrics()
                        .density
        );

        return drawable;
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
