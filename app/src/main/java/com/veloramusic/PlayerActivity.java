package com.veloramusic;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;

public class PlayerActivity extends Activity {

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;

    private FrameLayout root;
    private ImageView backgroundArtwork;
    private ImageView artwork;
    private TextView title;
    private TextView artist;
    private TextView currentTime;
    private TextView totalTime;
    private SeekBar progress;
    private SeekBar volume;
    private ImageButton playButton;
    private TextView favoriteButton;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(8, 8, 11));
        window.setNavigationBarColor(Color.rgb(8, 8, 11));

        buildPlayerUi();

        SessionToken token = new SessionToken(
                this,
                new android.content.ComponentName(
                        this,
                        PlaybackService.class
                )
        );

        controllerFuture =
                new MediaController.Builder(this, token).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                refreshPlayer();
                handler.post(progressUpdater);
            } catch (Exception ignored) {
            }
        }, command -> handler.post(command));
    }

    private void buildPlayerUi() {

        root = new FrameLayout(this);

        backgroundArtwork = new ImageView(this);
        backgroundArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backgroundArtwork.setAlpha(0.22f);

        if (Build.VERSION.SDK_INT >= 31) {
            backgroundArtwork.setRenderEffect(
                    RenderEffect.createBlurEffect(
                            45f,
                            45f,
                            Shader.TileMode.CLAMP
                    )
            );
        }

        root.addView(
                backgroundArtwork,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );

        View darkOverlay = new View(this);
        darkOverlay.setBackground(
                new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{
                                Color.argb(120, 5, 5, 8),
                                Color.argb(210, 7, 7, 10),
                                Color.argb(245, 7, 7, 10)
                        }
                )
        );

        root.addView(
                darkOverlay,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(
                dp(20),
                dp(16),
                dp(20),
                dp(16)
        );

        content.setOnApplyWindowInsetsListener((v, insets) -> {

            int left;
            int top;
            int right;
            int bottom;

            if (Build.VERSION.SDK_INT >= 30) {

                android.graphics.Insets bars =
                        insets.getInsets(
                                android.view.WindowInsets.Type.systemBars()
                        );

                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;

            } else {

                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }

            v.setPadding(
                    dp(20) + left,
                    dp(16) + top,
                    dp(20) + right,
                    dp(16) + bottom
            );

            return insets;
        });

        root.addView(
                content,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );

        root.post(() -> root.requestApplyInsets());

        // TOP BAR

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton close = iconButton(
                android.R.drawable.ic_menu_close_clear_cancel,
                48
        );

        close.setOnClickListener(v -> finish());

        topBar.addView(close);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setGravity(Gravity.CENTER);

        TextView nowPlaying =
                labelText(
                        "NOW PLAYING",
                        10,
                        Color.rgb(180, 180, 190)
                );

        TextView velora =
                labelText(
                        "VELORA",
                        9,
                        Color.rgb(105, 105, 115)
                );

        heading.addView(nowPlaying);
        heading.addView(velora);

        topBar.addView(
                heading,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );

        ImageButton more =
                iconButton(
                        android.R.drawable.ic_menu_more,
                        48
                );

        more.setOnClickListener(
                v -> showPlayerOptions()
        );

        topBar.addView(more);

        content.addView(
                topBar,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                )
        );

        // ARTWORK

        int artworkSize = Math.min(
                dp(310),
                Math.min(
                        getResources()
                                .getDisplayMetrics()
                                .widthPixels - dp(40),
                        (int) (
                                getResources()
                                        .getDisplayMetrics()
                                        .heightPixels * 0.39f
                        )
                )
        );

        artwork = new ImageView(this);
        artwork.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );
        artwork.setBackground(
                roundedBackground(
                        Color.rgb(39, 39, 47),
                        24
                )
        );
        artwork.setClipToOutline(true);

        LinearLayout.LayoutParams artworkParams =
                new LinearLayout.LayoutParams(
                        artworkSize,
                        artworkSize
                );

        artworkParams.gravity =
                Gravity.CENTER_HORIZONTAL;

        artworkParams.topMargin = dp(10);
        artworkParams.bottomMargin = dp(18);

        content.addView(
                artwork,
                artworkParams
        );

        // SONG INFO

        LinearLayout infoRow =
                new LinearLayout(this);

        infoRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout songInfo =
                new LinearLayout(this);

        songInfo.setOrientation(
                LinearLayout.VERTICAL
        );

        title = new TextView(this);
        title.setText("Nothing Playing");
        title.setTextColor(Color.WHITE);
        title.setTextSize(23);
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        title.setSingleLine(true);

        artist = new TextView(this);
        artist.setText("Velora Music");
        artist.setTextColor(
                Color.rgb(160, 160, 170)
        );
        artist.setTextSize(14);
        artist.setPadding(
                0,
                dp(4),
                0,
                0
        );
        artist.setSingleLine(true);

        songInfo.addView(title);
        songInfo.addView(artist);

        infoRow.addView(
                songInfo,
                new LinearLayout.LayoutParams(
                        0,
                        dp(62),
                        1
                )
        );

        favoriteButton =
                labelText(
                        "♡",
                        30,
                        Color.WHITE
                );

        favoriteButton.setOnClickListener(
                v -> toggleFavorite()
        );

        infoRow.addView(
                favoriteButton,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        content.addView(infoRow);

        // PROGRESS

        LinearLayout timeRow =
                new LinearLayout(this);

        timeRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        currentTime =
                timeText("0:00");

        totalTime =
                timeText("0:00");

        progress = new SeekBar(this);
        progress.setMax(1000);
        progress.setProgress(0);
        progress.setPadding(0, 0, 0, 0);

        progress.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int value,
                            boolean fromUser
                    ) {

                        if (fromUser &&
                                controller != null) {

                            long duration =
                                    controller.getDuration();

                            if (duration > 0) {

                                currentTime.setText(
                                        formatTime(
                                                duration *
                                                        value /
                                                        1000L
                                        )
                                );
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {

                        if (controller != null) {

                            long duration =
                                    controller.getDuration();

                            if (duration > 0) {

                                controller.seekTo(
                                        duration *
                                                seekBar.getProgress()
                                                / 1000L
                                );
                            }
                        }
                    }
                }
        );

        timeRow.addView(
                currentTime,
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(42)
                )
        );

        timeRow.addView(
                progress,
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1
                )
        );

        timeRow.addView(
                totalTime,
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(42)
                )
        );

        content.addView(timeRow);

        // PLAYBACK CONTROLS

        LinearLayout controls =
                new LinearLayout(this);

        controls.setGravity(
                Gravity.CENTER
        );

        ImageButton previous =
                iconButton(
                        android.R.drawable.ic_media_previous,
                        64
                );

        previous.setOnClickListener(v -> {

            if (controller != null) {
                controller.seekToPreviousMediaItem();
            }
        });

        playButton =
                iconButton(
                        android.R.drawable.ic_media_play,
                        76
                );

        playButton.setBackground(
                roundedBackground(
                        Color.rgb(190, 169, 255),
                        50
                )
        );

        playButton.setColorFilter(
                Color.rgb(18, 17, 23)
        );

        playButton.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
        );

        playButton.setOnClickListener(
                v -> togglePlayback()
        );

        ImageButton next =
                iconButton(
                        android.R.drawable.ic_media_next,
                        64
                );

        next.setOnClickListener(v -> {

            if (controller != null) {
                controller.seekToNextMediaItem();
            }
        });

        controls.addView(previous);

        LinearLayout.LayoutParams playParams =
                new LinearLayout.LayoutParams(
                        dp(76),
                        dp(76)
                );

        playParams.setMargins(
                dp(12),
                0,
                dp(12),
                0
        );

        controls.addView(
                playButton,
                playParams
        );

        controls.addView(next);

        content.addView(
                controls,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(82)
                )
        );

        // VOLUME

        LinearLayout volumeRow =
                new LinearLayout(this);

        volumeRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView low =
                labelText(
                        "−",
                        18,
                        Color.rgb(155, 155, 165)
                );

        TextView high =
                labelText(
                        "+",
                        18,
                        Color.rgb(155, 155, 165)
                );

        volume = new SeekBar(this);
        volume.setMax(100);
        volume.setProgress(100);

        volume.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int value,
                            boolean fromUser
                    ) {

                        if (fromUser &&
                                controller != null) {

                            controller.setVolume(
                                    value / 100f
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }
                }
        );

        volumeRow.addView(
                low,
                new LinearLayout.LayoutParams(
                        dp(28),
                        dp(40)
                )
        );

        volumeRow.addView(
                volume,
                new LinearLayout.LayoutParams(
                        0,
                        dp(40),
                        1
                )
        );

        volumeRow.addView(
                high,
                new LinearLayout.LayoutParams(
                        dp(28),
                        dp(40)
                )
        );

        content.addView(volumeRow);

        // LOWER GLASS PANEL

        LinearLayout glassPanel =
                new LinearLayout(this);

        glassPanel.setGravity(
                Gravity.CENTER
        );

        glassPanel.setPadding(
                dp(8),
                dp(2),
                dp(8),
                dp(2)
        );

        glassPanel.setBackground(
                roundedBackground(
                        Color.argb(55, 255, 255, 255),
                        22
                )
        );

        TextView lyrics =
                actionText("LYRICS");

        TextView queue =
                actionText("QUEUE");

        TextView lossless =
                actionText("LOSSLESS");

        lossless.setTextColor(
                Color.rgb(198, 181, 255)
        );

        lyrics.setOnClickListener(
                v -> Toast.makeText(
                        this,
                        "Lyrics will appear when available.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        queue.setOnClickListener(
                v -> Toast.makeText(
                        this,
                        "Queue controls coming from your library.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        glassPanel.addView(
                lyrics,
                actionParams()
        );

        glassPanel.addView(
                queue,
                actionParams()
        );

        glassPanel.addView(
                lossless,
                actionParams()
        );

        LinearLayout.LayoutParams glassParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                );

        glassParams.topMargin = dp(7);

        content.addView(
                glassPanel,
                glassParams
        );

        setContentView(root);
    }

    private void refreshPlayer() {

        if (controller == null) {
            return;
        }

        MediaItem item =
                controller.getCurrentMediaItem();

        if (item == null) {

            title.setText(
                    "Nothing Playing"
            );

            artist.setText(
                    "Velora Music"
            );

            artwork.setImageDrawable(null);
            backgroundArtwork.setImageDrawable(null);

            return;
        }

        MediaMetadata metadata =
                item.mediaMetadata;

        String songTitle =
                metadata.title != null
                        ? metadata.title.toString()
                        : "Unknown Song";

        String songArtist =
                metadata.artist != null
                        ? metadata.artist.toString()
                        : "Unknown Artist";

        title.setText(songTitle);
        artist.setText(songArtist);

        updateArtwork(item);

        playButton.setImageResource(
                controller.isPlaying()
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play
        );

        volume.setProgress(
                (int) (
                        controller.getVolume()
                                * 100
                )
        );
    }

    private void updateArtwork(
            MediaItem item
    ) {

        Bitmap bitmap = null;

        MediaMetadata metadata =
                item.mediaMetadata;

        if (metadata.artworkData != null) {

            bitmap =
                    BitmapFactory.decodeByteArray(
                            metadata.artworkData,
                            0,
                            metadata.artworkData.length
                    );
        }

        if (bitmap == null &&
                item.localConfiguration != null) {

            Uri uri =
                    item.localConfiguration.uri;

            bitmap =
                    getEmbeddedArtwork(uri);
        }

        if (bitmap != null) {

            artwork.setImageBitmap(bitmap);
            backgroundArtwork.setImageBitmap(bitmap);

        } else {

            artwork.setImageDrawable(null);
            backgroundArtwork.setImageDrawable(null);
        }
    }

    private Bitmap getEmbeddedArtwork(
            Uri uri
    ) {

        MediaMetadataRetriever retriever =
                new MediaMetadataRetriever();

        try {

            retriever.setDataSource(
                    this,
                    uri
            );

            byte[] data =
                    retriever.getEmbeddedPicture();

            if (data != null) {

                return BitmapFactory.decodeByteArray(
                        data,
                        0,
                        data.length
                );
            }

        } catch (Exception ignored) {

        } finally {

            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }

        return null;
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

        refreshPlayer();
    }

    private void toggleFavorite() {

        if ("♡".contentEquals(
                favoriteButton.getText()
        )) {

            favoriteButton.setText("♥");
            favoriteButton.setTextColor(
                    Color.rgb(205, 190, 255)
            );

        } else {

            favoriteButton.setText("♡");
            favoriteButton.setTextColor(
                    Color.WHITE
            );
        }
    }

    private void showPlayerOptions() {

        Toast.makeText(
                this,
                "Player options",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void updateProgress() {

        if (controller == null) {
            return;
        }

        long duration =
                controller.getDuration();

        long position =
                controller.getCurrentPosition();

        if (duration > 0) {

            int value =
                    (int) (
                            position *
                                    1000L /
                                    duration
                    );

            progress.setProgress(value);

            currentTime.setText(
                    formatTime(position)
            );

            totalTime.setText(
                    formatTime(duration)
            );

        } else {

            progress.setProgress(0);

            currentTime.setText(
                    "0:00"
            );

            totalTime.setText(
                    "0:00"
            );
        }

        playButton.setImageResource(
                controller.isPlaying()
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play
        );
    }

    private ImageButton iconButton(
            int icon,
            int size
    ) {

        ImageButton button =
                new ImageButton(this);

        button.setImageResource(icon);
        button.setBackgroundColor(
                Color.TRANSPARENT
        );

        button.setColorFilter(
                Color.WHITE
        );

        button.setScaleType(
                ImageView.ScaleType.CENTER
        );

        button.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        button.setContentDescription(
                "Player control"
        );

        return button;
    }

    private TextView labelText(
            String text,
            int size,
            int color
    ) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setGravity(
                Gravity.CENTER
        );

        return view;
    }

    private TextView timeText(
            String text
    ) {

        return labelText(
                text,
                11,
                Color.rgb(
                        145,
                        145,
                        155
                )
        );
    }

    private TextView actionText(
            String text
    ) {

        TextView view =
                labelText(
                        text,
                        10,
                        Color.rgb(
                                185,
                                185,
                                195
                        )
                );

        view.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        return view;
    }

    private LinearLayout.LayoutParams actionParams() {

        return new LinearLayout.LayoutParams(
                0,
                dp(46),
                1
        );
    }

    private GradientDrawable roundedBackground(
            int color,
            int radiusDp
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);

        drawable.setCornerRadius(
                dp(radiusDp)
        );

        return drawable;
    }

    private String formatTime(
            long milliseconds
    ) {

        long totalSeconds =
                Math.max(
                        0,
                        milliseconds
                ) / 1000;

        long minutes =
                totalSeconds / 60;

        long seconds =
                totalSeconds % 60;

        return String.format(
                Locale.US,
                "%d:%02d",
                minutes,
                seconds
        );
    }

    private int dp(
            int value
    ) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (controller != null) {
            refreshPlayer();
        }

        handler.post(
                progressUpdater
        );
    }

    @Override
    protected void onPause() {

        super.onPause();

        handler.removeCallbacks(
                progressUpdater
        );
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(
                progressUpdater
        );

        if (controllerFuture != null) {
            MediaController.releaseFuture(
                    controllerFuture
            );
        }

        controller = null;

        super.onDestroy();
    }
}
