package com.veloramusic;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        requestAudioPermission();
        connectController();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 11, 15));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(buildMiniPlayer());
        root.addView(buildNavigation());

        setContentView(root);
        showHome();
    }

    private View buildNavigation() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundColor(Color.rgb(16, 16, 21));

        String[] labels = {"HOME", "SEARCH", "LIBRARY", "CUSTOM"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = new TextView(this);
            item.setText(labels[i]);
            item.setTextColor(index == 0 ? accent : Color.LTGRAY);
            item.setTextSize(11);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, 14, 0, 14);
            item.setOnClickListener(v -> {
                if (index == 0) showHome();
                if (index == 1) showSearch();
                if (index == 2) showLibrary();
                if (index == 3) showCustomise();
            });
            bar.addView(item, new LinearLayout.LayoutParams(0, 64, 1f));
        }
        return bar;
    }

    private View buildMiniPlayer() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(16, 8, 10, 8);
        bar.setBackgroundColor(Color.rgb(23, 23, 29));

        TextView art = new TextView(this);
        art.setText("♫");
        art.setTextSize(24);
        art.setTextColor(Color.WHITE);
        art.setGravity(Gravity.CENTER);
        art.setBackgroundColor(accent);
        bar.addView(art, new LinearLayout.LayoutParams(52, 52));

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(12, 0, 8, 0);

        nowTitle = textView("Nothing playing", Color.WHITE, 15);
        nowArtist = textView("Velora Music", Color.GRAY, 12);
        textBox.addView(nowTitle);
        textBox.addView(nowArtist);

        bar.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1f));

        playButton = new Button(this);
        playButton.setText("▶");
        playButton.setTextColor(Color.WHITE);
        playButton.setOnClickListener(v -> togglePlayback());
        bar.addView(playButton, new LinearLayout.LayoutParams(60, 54));

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
        content.setPadding(22, 22, 22, 22);
    }

    private TextView heading(String text, String subtitle) {
        TextView h = textView(text, Color.WHITE, 30);
        h.setTypeface(null, android.graphics.Typeface.BOLD);
        h.setPadding(0, 0, 0, 6);
        content.addView(h);
        if (subtitle != null && !subtitle.isEmpty()) {
            content.addView(textView(subtitle, Color.GRAY, 13));
        }
        return h;
    }

    private void showHome() {
        clearContent();
        heading("Velora Music", "Premium music. Lightweight performance.");

        EditText search = new EditText(this);
        search.setHint("Search your music");
        search.setSingleLine(true);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.GRAY);
        content.addView(search, new LinearLayout.LayoutParams(-1, 54));

        TextView section = textView("Your Music", Color.WHITE, 20);
        section.setTypeface(null, android.graphics.Typeface.BOLD);
        section.setPadding(0, 26, 0, 10);
        content.addView(section);

        renderSongs("All");
    }

    private void showSearch() {
        clearContent();
        heading("Search", "Find local songs or add a licensed stream.");

        EditText search = new EditText(this);
        search.setHint("Song, artist, album...");
        search.setSingleLine(true);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.GRAY);
        content.addView(search, new LinearLayout.LayoutParams(-1, 54));

        Button add = new Button(this);
        add.setText("+ Add licensed stream URL");
        add.setOnClickListener(v -> addStream());
        content.addView(add);

        search.setOnEditorActionListener((v, actionId, event) -> {
            renderSearch(search.getText().toString());
            return false;
        });

        renderSongs("All");
    }

    private void showLibrary() {
        clearContent();
        heading("Library", "Local + online music");

        Button refresh = new Button(this);
        refresh.setText("Rescan local music");
        refresh.setOnClickListener(v -> {
            loadLocalSongs();
            showLibrary();
        });
        content.addView(refresh);

        renderSongs("All");
    }

    private void showCustomise() {
        clearContent();
        heading("Customise", "Make Velora Music yours.");

        content.addView(textView("Accent theme", Color.WHITE, 18));
        String[] names = {"Midnight", "Ocean", "Neon", "Sunset", "Rose"};
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

        content.addView(textView("Animation intensity", Color.WHITE, 18));

        SeekBar intensity = new SeekBar(this);
        intensity.setMax(2);
        intensity.setProgress(1);
        content.addView(intensity);

        content.addView(textView(
                "Player layouts, artwork motion, visualizer, crossfade and advanced audio controls are planned for the next build.",
                Color.GRAY, 13
        ));
    }

    private void renderSearch(String query) {
        content.removeViews(4, Math.max(0, content.getChildCount() - 4));
        for (Song song : songs) {
            if (query.isEmpty()
                    || song.title.toLowerCase().contains(query.toLowerCase())
                    || song.artist.toLowerCase().contains(query.toLowerCase())) {
                content.addView(songRow(song));
            }
        }
    }

    private void renderSongs(String filter) {
        for (Song song : songs) {
            content.addView(songRow(song));
        }
        if (songs.isEmpty()) {
            content.addView(textView(
                    "No songs found. Grant music permission or add a licensed stream.",
                    Color.GRAY, 14
            ));
        }
    }

    private View songRow(Song song) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(8, 8, 4, 8);

        TextView icon = textView("♫", Color.WHITE, 22);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundColor(accent);
        row.addView(icon, new LinearLayout.LayoutParams(52, 52));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(12, 0, 8, 0);
        text.addView(textView(song.title, Color.WHITE, 16));
        text.addView(textView(song.artist, Color.GRAY, 12));

        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));

        Button play = new Button(this);
        play.setText("▶");
        play.setOnClickListener(v -> playSong(song));
        row.addView(play, new LinearLayout.LayoutParams(58, 52));

        return row;
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
                .setMessage("Use only audio URLs you own or are licensed to stream.")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, which) -> {
                    if (url.getText().toString().trim().isEmpty()) return;
                    songs.add(new Song(
                            title.getText().toString().trim().isEmpty()
                                    ? "Online Stream" : title.getText().toString().trim(),
                            artist.getText().toString().trim().isEmpty()
                                    ? "Unknown Artist" : artist.getText().toString().trim(),
                            url.getText().toString().trim(),
                            true
                    ));
                    showLibrary();
                })
                .show();
    }

    private void playSong(Song song) {
        if (controller == null) return;

        MediaItem item = new MediaItem.Builder()
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
        if (controller == null) return;
        if (controller.isPlaying()) controller.pause();
        else controller.play();
    }

    private void connectController() {
        SessionToken token = new SessionToken(
                this,
                new ComponentName(this, PlaybackService.class)
        );

        ListenableFuture<MediaController> future =
                new MediaController.Builder(this, token).buildAsync();

        future.addListener(() -> {
            try {
                controller = future.get();
                controller.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        playButton.setText(isPlaying ? "Ⅱ" : "▶");
                    }

                    @Override
                    public void onMediaItemTransition(MediaItem item, int reason) {
                        if (item == null) return;
                        CharSequence title = item.mediaMetadata.title;
                        CharSequence artist = item.mediaMetadata.artist;
                        nowTitle.setText(title == null ? "Nothing playing" : title);
                        nowArtist.setText(artist == null ? "Velora Music" : artist);
                    }
                });
            } catch (Exception ignored) {
                controller = null;
            }
        }, MoreExecutors.directExecutor());
    }

    private void requestAudioPermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= 33) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (Build.VERSION.SDK_INT < 33
                && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, AUDIO_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, AUDIO_PERMISSION);
        } else {
            loadLocalSongs();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == AUDIO_PERMISSION
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
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

        try (android.database.Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) return;

            int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);

            while (cursor.moveToNext() && songs.size() < 500) {
                long id = cursor.getLong(idIndex);
                songs.add(new Song(
                        cursor.getString(titleIndex),
                        cursor.getString(artistIndex),
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id,
                        false
                ));
            }
        }
    }

    private static class Song {
        final String title;
        final String artist;
        final String uri;
        final boolean online;

        Song(String title, String artist, String uri, boolean online) {
            this.title = title;
            this.artist = artist;
            this.uri = uri;
            this.online = online;
        }
    }
}
