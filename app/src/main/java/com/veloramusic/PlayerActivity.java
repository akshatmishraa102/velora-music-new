package com.veloramusic;

import android.app.Activity;
import android.app.AlertDialog;
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

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerActivity extends Activity {

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;

    private FrameLayout root;
    private View darkOverlay;
    private ImageView backgroundArtwork;
    private ImageView artwork;

    private TextView title;
    private TextView artist;
    private TextView currentTime;
    private TextView totalTime;
    private TextView qualityBadge;
    private TextView favoriteButton;

    private SeekBar progress;
    private SeekBar volume;
    private ImageButton playButton;

    private boolean favorite = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable progressUpdater =
            new Runnable() {
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
                new MediaController.Builder(
                        this,
                        token
                ).buildAsync();

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
        backgroundArtwork.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );
        backgroundArtwork.setAlpha(0.30f);

        if (Build.VERSION.SDK_INT >= 31) {
            backgroundArtwork.setRenderEffect(
                    RenderEffect.createBlurEffect(
                            56f,
                            56f,
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

        darkOverlay = new View(this);

        darkOverlay.setBackground(
                new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{
                                Color.argb(125, 5, 5, 8),
                                Color.argb(205, 7, 7, 10),
                                Color.argb(248, 7, 7, 10)
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

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        content.setPadding(
                dp(20),
                dp(16),
                dp(20),
                dp(16)
        );

        content.setOnApplyWindowInsetsListener(
                (v, insets) -> {

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

                        left =
                                insets.getSystemWindowInsetLeft();

                        top =
                                insets.getSystemWindowInsetTop();

                        right =
                                insets.getSystemWindowInsetRight();

                        bottom =
                                insets.getSystemWindowInsetBottom();
                    }

                    v.setPadding(
                            dp(20) + left,
                            dp(16) + top,
                            dp(20) + right,
                            dp(16) + bottom
                    );

                    return insets;
                }
        );

        root.addView(
                content,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );

        root.post(
                () -> root.requestApplyInsets()
        );

        // TOP BAR

        LinearLayout topBar =
                new LinearLayout(this);

        topBar.setGravity(
                Gravity.CENTER_VERTICAL
        );

        ImageButton close =
                iconButton(
                        android.R.drawable
                                .ic_menu_close_clear_cancel
                );

        close.setOnClickListener(
                v -> finish()
        );

        topBar.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                )
        );

        LinearLayout heading =
                new LinearLayout(this);

        heading.setOrientation(
                LinearLayout.VERTICAL
        );

        heading.setGravity(
                Gravity.CENTER
        );

        heading.addView(
                labelText(
                        "NOW PLAYING",
                        10,
                        Color.rgb(
                                185,
                                185,
                                195
                        )
                )
        );

        heading.addView(
                labelText(
                        "VELORA",
                        9,
                        Color.rgb(
                                112,
                                112,
                                122
                        )
                )
        );

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
                        android.R.drawable.ic_menu_more
                );

        more.setOnClickListener(
                v -> showPlayerOptions()
        );

        topBar.addView(
                more,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                )
        );

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
                                .widthPixels
                                - dp(40),

                        (int) (
                                getResources()
                                        .getDisplayMetrics()
                                        .heightPixels
                                        * 0.39f
                        )
                )
        );

        artwork = new ImageView(this);

        artwork.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        artwork.setBackground(
                roundedBackground(
                        Color.rgb(
                                39,
                                39,
                                47
                        ),
                        24
                )
        );

        artwork.setClipToOutline(true);
        artwork.setOnTouchListener(new View.OnTouchListener() {
    private float downX;

    @Override
    public boolean onTouch(View v, android.view.MotionEvent event) {
        switch (event.getAction()) {
            case android.view.MotionEvent.ACTION_DOWN:
                downX = event.getX();
                return true;

            case android.view.MotionEvent.ACTION_UP:
                float deltaX = event.getX() - downX;

                if (Math.abs(deltaX) > dp(70) && controller != null) {
                    if (deltaX < 0) {
                        controller.seekToNextMediaItem();
                    } else {
                        controller.seekToPreviousMediaItem();
                    }

                    artwork.animate()
                            .scaleX(0.97f)
                            .scaleY(0.97f)
                            .setDuration(80)
                            .withEndAction(() ->
                                    artwork.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(140)
                                            .start())
                            .start();

                    return true;
                }

                return true;
        }

        return true;
    }
});

        LinearLayout.LayoutParams artworkParams =
                new LinearLayout.LayoutParams(
                        artworkSize,
                        artworkSize
                );

        artworkParams.gravity =
                Gravity.CENTER_HORIZONTAL;

        artworkParams.topMargin =
                dp(8);

        artworkParams.bottomMargin =
                dp(18);

        content.addView(
                artwork,
                artworkParams
        );

        // SONG INFORMATION

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

        title.setText(
                "Nothing Playing"
        );

        title.setTextColor(
                Color.WHITE
        );

        title.setTextSize(
                22
        );

        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        title.setSingleLine(true);

        title.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        artist = new TextView(this);

        artist.setText(
                "Velora Music"
        );

        artist.setTextColor(
                Color.rgb(
                        165,
                        165,
                        175
                )
        );

        artist.setTextSize(
                14
        );

        artist.setPadding(
                0,
                dp(2),
                0,
                0
        );

        artist.setSingleLine(true);

        artist.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );



        songInfo.addView(title);
        songInfo.addView(artist);

    

        infoRow.addView(
                songInfo,
                new LinearLayout.LayoutParams(
                        0,
                        dp(82),
                        1
                )
        );

        favoriteButton =
                labelText(
                        "♡",
                        31,
                        Color.WHITE
                );

        favoriteButton.setGravity(
                Gravity.CENTER
        );

        favoriteButton.setOnClickListener(
                v -> toggleFavorite()
        );

        infoRow.addView(
                favoriteButton,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(54)
                )
        );

        content.addView(
                infoRow
        );

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

        progress =
                new SeekBar(this);

        progress.setMax(
                1000
        );

        progress.setProgress(
                0
        );

        progress.setPadding(
                0,
                0,
                0,
                0
        );

        progress.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int value,
                            boolean fromUser
                    ) {

                        if (
                                fromUser &&
                                controller != null
                        ) {

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

        content.addView(
                timeRow
        );

        // CONTROLS

        LinearLayout controls =
                new LinearLayout(this);

        controls.setGravity(
                Gravity.CENTER
        );

        ImageButton previous =
                iconButton(
                        android.R.drawable
                                .ic_media_previous
                );

        previous.setOnClickListener(
                v -> {

                    if (controller != null) {
                        controller
                                .seekToPreviousMediaItem();
                    }
                }
        );

        playButton =
                iconButton(
                        android.R.drawable
                                .ic_media_play
                );

        playButton.setBackground(
                roundedBackground(
                        Color.rgb(
                                190,
                                169,
                                255
                        ),
                        50
                )
        );

        playButton.setColorFilter(
                Color.rgb(
                        18,
                        17,
                        23
                )
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
                        android.R.drawable
                                .ic_media_next
                );

        next.setOnClickListener(
                v -> {

                    if (controller != null) {
                        controller
                                .seekToNextMediaItem();
                    }
                }
        );

        controls.addView(
                previous,
                new LinearLayout.LayoutParams(
                        dp(64),
                        dp(64)
                )
        );

        LinearLayout.LayoutParams playParams =
                new LinearLayout.LayoutParams(
                        dp(78),
                        dp(78)
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

        controls.addView(
                next,
                new LinearLayout.LayoutParams(
                        dp(64),
                        dp(64)
                )
        );

        content.addView(
                controls,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(84)
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
                        Color.rgb(
                                155,
                                155,
                                165
                        )
                );

        TextView high =
                labelText(
                        "+",
                        18,
                        Color.rgb(
                                155,
                                155,
                                165
                        )
                );

        volume =
                new SeekBar(this);

        volume.setMax(
                100
        );

        volume.setProgress(
                100
        );

        volume.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int value,
                            boolean fromUser
                    ) {

                        if (
                                fromUser &&
                                controller != null
                        ) {

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

        content.addView(
                volumeRow
        );

        // BOTTOM ACTION PANEL

        LinearLayout glassPanel =
                new LinearLayout(this);

        glassPanel.setGravity(
                Gravity.CENTER
        );

        glassPanel.setPadding(
                dp(6),
                dp(2),
                dp(6),
                dp(2)
        );

        glassPanel.setBackground(
                roundedBackground(
                        Color.argb(
                                68,
                                255,
                                255,
                                255
                        ),
                        22
                )
        );

        TextView lyrics =
                actionText("LYRICS");

        TextView queue =
                actionText("QUEUE");

        TextView moreAction =
                actionText("MORE");

        lyrics.setOnClickListener(
                v -> showLyrics()
        );

        queue.setOnClickListener(
                v -> showQueue()
        );

        moreAction.setOnClickListener(
                v -> showPlayerOptions()
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
                moreAction,
                actionParams()
        );

        LinearLayout.LayoutParams glassParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                );

        glassParams.topMargin =
                dp(7);

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

            qualityBadge.setText(
                    "READY"
            );

            artwork.setImageDrawable(
                    null
            );

            backgroundArtwork.setImageDrawable(
                    null
            );

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

        title.setText(
                songTitle
        );

        artist.setText(
                songArtist
        );

        updateArtwork(item);
        updateQualityBadge(item);

        playButton.setImageResource(
                controller.isPlaying()
                        ? android.R.drawable
                                .ic_media_pause
                        : android.R.drawable
                                .ic_media_play
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

        if (
                bitmap == null &&
                item.localConfiguration != null
        ) {

            bitmap =
                    getEmbeddedArtwork(
                            item.localConfiguration.uri
                    );
        }

        if (bitmap != null) {

            artwork.setImageBitmap(
                    bitmap
            );

            backgroundArtwork.setImageBitmap(
                    bitmap
            );

            applyArtworkTint(
                    bitmap
            );

        } else {

            artwork.setImageDrawable(
                    null
            );

            backgroundArtwork.setImageDrawable(
                    null
            );

            resetBackgroundTint();
        }
    }

    private void applyArtworkTint(
            Bitmap bitmap
    ) {

        int stepX =
                Math.max(
                        1,
                        bitmap.getWidth() / 24
                );

        int stepY =
                Math.max(
                        1,
                        bitmap.getHeight() / 24
                );

        long r = 0;
        long g = 0;
        long b = 0;

        int count = 0;

        for (
                int y = 0;
                y < bitmap.getHeight();
                y += stepY
        ) {

            for (
                    int x = 0;
                    x < bitmap.getWidth();
                    x += stepX
            ) {

                int c =
                        bitmap.getPixel(
                                x,
                                y
                        );

                r += Color.red(c);
                g += Color.green(c);
                b += Color.blue(c);

                count++;
            }
        }

        if (count == 0) {
            return;
        }

        int rr =
                Math.min(
                        95,
                        (int) (
                                r /
                                        count *
                                        0.55f
                        )
                );

        int gg =
                Math.min(
                        95,
                        (int) (
                                g /
                                        count *
                                        0.55f
                        )
                );

        int bb =
                Math.min(
                        110,
                        (int) (
                                b /
                                        count *
                                        0.65f
                        )
                );

        darkOverlay.setBackground(
                new GradientDrawable(
                        GradientDrawable
                                .Orientation.TOP_BOTTOM,
                        new int[]{
                                Color.argb(
                                        100,
                                        rr,
                                        gg,
                                        bb
                                ),
                                Color.argb(
                                        218,
                                        7,
                                        7,
                                        10
                                ),
                                Color.argb(
                                        250,
                                        7,
                                        7,
                                        10
                                )
                        }
                )
        );
    }

    private void resetBackgroundTint() {

        darkOverlay.setBackground(
                new GradientDrawable(
                        GradientDrawable
                                .Orientation.TOP_BOTTOM,
                        new int[]{
                                Color.argb(
                                        125,
                                        5,
                                        5,
                                        8
                                ),
                                Color.argb(
                                        205,
                                        7,
                                        7,
                                        10
                                ),
                                Color.argb(
                                        248,
                                        7,
                                        7,
                                        10
                                )
                        }
                )
        );
    }

    private void updateQualityBadge(
            MediaItem item
    ) {

        String value =
                item.localConfiguration == null
                        ? ""
                        : item.localConfiguration.uri
                                .toString()
                                .toLowerCase(
                                        Locale.US
                                );

        if (
                value.endsWith(".flac") ||
                value.endsWith(".wav") ||
                value.endsWith(".alac") ||
                value.contains("flac")
        ) {

            qualityBadge.setText(
                    "LOSSLESS"
            );

        } else {

            qualityBadge.setText(
                    "HI-FI"
            );
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

        favorite =
                !favorite;

        favoriteButton.setText(
                favorite
                        ? "♥"
                        : "♡"
        );

        favoriteButton.setTextColor(
                favorite
                        ? Color.rgb(
                                205,
                                190,
                                255
                        )
                        : Color.WHITE
        );
    }

    private void showQueue() {

        if (
                controller == null ||
                controller.getMediaItemCount() == 0
        ) {

            showMessage(
                    "Queue",
                    "Your queue is empty."
            );

            return;
        }

        List<String> names =
                new ArrayList<>();

        for (
                int i = 0;
                i < controller.getMediaItemCount();
                i++
        ) {

            MediaMetadata metadata =
                    controller
                            .getMediaItemAt(i)
                            .mediaMetadata;

            String name =
                    metadata.title != null
                            ? metadata.title.toString()
                            : "Unknown Song";

            if (
                    i ==
                            controller
                                    .getCurrentMediaItemIndex()
            ) {

                name =
                        "▶  " + name;
            }

            names.add(name);
        }

        new AlertDialog.Builder(this)
                .setTitle("Queue")
                .setItems(
                        names.toArray(
                                new String[0]
                        ),
                        (dialog, which) -> {

                            controller.seekToDefaultPosition(
                                    which
                            );
                        }
                )
                .setNegativeButton(
                        "Close",
                        null
                )
                .show();
    }

    private void showLyrics() {

        if (
                controller == null ||
                controller.getCurrentMediaItem() == null
        ) {

            showMessage(
                    "Lyrics",
                    "Nothing is playing."
            );

            return;
        }

        MediaMetadata metadata =
                controller
                        .getCurrentMediaItem()
                        .mediaMetadata;

        String text =
                metadata.description != null
                        ? metadata.description.toString()
                        : "Lyrics are not available for this track yet.";

        new AlertDialog.Builder(this)
                .setTitle("Lyrics")
                .setMessage(text)
                .setPositiveButton(
                        "Done",
                        null
                )
                .show();
    }

    private void showPlayerOptions() {

        if (controller == null) {
            return;
        }

        String[] options = {
                controller.getRepeatMode()
                        == Player.REPEAT_MODE_OFF
                        ? "Repeat: Off"
                        : "Repeat: On",

                controller.getShuffleModeEnabled()
                        ? "Shuffle: On"
                        : "Shuffle: Off",

                "Close"
        };

        new AlertDialog.Builder(this)
                .setTitle("Player")
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {

                                controller.setRepeatMode(
                                        controller.getRepeatMode()
                                                == Player.REPEAT_MODE_OFF
                                                ? Player.REPEAT_MODE_ALL
                                                : Player.REPEAT_MODE_OFF
                                );

                                showPlayerOptions();

                            } else if (which == 1) {

                                controller.setShuffleModeEnabled(
                                        !controller
                                                .getShuffleModeEnabled()
                                );

                                showPlayerOptions();
                            }
                        }
                )
                .show();
    }

    private void showMessage(
            String heading,
            String message
    ) {

        new AlertDialog.Builder(this)
                .setTitle(heading)
                .setMessage(message)
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
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

            progress.setProgress(
                    (int) Math.min(
                            1000L,
                            position *
                                    1000L /
                                    duration
                    )
            );

            currentTime.setText(
                    formatTime(position)
            );

            totalTime.setText(
                    formatTime(duration)
            );

        } else {

            progress.setProgress(
                    0
            );

            currentTime.setText(
                    "0:00"
            );

            totalTime.setText(
                    "0:00"
            );
        }

        playButton.setImageResource(
                controller.isPlaying()
                        ? android.R.drawable
                                .ic_media_pause
                        : android.R.drawable
                                .ic_media_play
        );
    }

    private ImageButton iconButton(
            int icon
    ) {

        ImageButton button =
                new ImageButton(this);

        button.setImageResource(
                icon
        );

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

        view.setText(
                text
        );

        view.setTextColor(
                color
        );

        view.setTextSize(
                size
        );

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
                                190,
                                190,
                                200
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

        drawable.setColor(
                color
        );

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
