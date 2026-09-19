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

    private LinearLayout content;
    private LinearLayout playerScreen;
    private FrameLayout modeOverlay;
    private LinearLayout lyricsSheet;
    private LinearLayout queueSheet;
    private LinearLayout queueListContainer;
    private TextView lyricsBody;

    private void buildPlayerUi() {

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8, 8, 11));

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

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(18), dp(12), dp(18), dp(12));
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
                        left = insets.getSystemWindowInsetLeft();
                        top = insets.getSystemWindowInsetTop();
                        right = insets.getSystemWindowInsetRight();
                        bottom = insets.getSystemWindowInsetBottom();
                    }

                    v.setPadding(
                            dp(18) + left,
                            dp(12) + top,
                            dp(18) + right,
                            dp(12) + bottom
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

        root.post(() -> root.requestApplyInsets());

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, dp(4), 0, 0);

        ImageButton close = iconButton(android.R.drawable.ic_menu_close_clear_cancel);
        close.setOnClickListener(v -> finish());

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setGravity(Gravity.CENTER);
        TextView liveBadge = labelText("NOW PLAYING", 10, Color.rgb(188, 188, 202));
        TextView brandBadge = labelText("VELORA", 9, Color.rgb(118, 118, 128));
        heading.addView(liveBadge);
        heading.addView(brandBadge);

        ImageButton more = iconButton(android.R.drawable.ic_menu_more);
        more.setOnClickListener(v -> showPlayerOptions());

        topBar.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        topBar.addView(heading, new LinearLayout.LayoutParams(0, dp(52), 1));
        topBar.addView(more, new LinearLayout.LayoutParams(dp(48), dp(48)));

        content.addView(topBar, new LinearLayout.LayoutParams(-1, dp(52)));

        playerScreen = new LinearLayout(this);
        playerScreen.setOrientation(LinearLayout.VERTICAL);
        playerScreen.setGravity(Gravity.CENTER_HORIZONTAL);

        int artworkSize = Math.min(
                dp(320),
                Math.min(
                        getResources().getDisplayMetrics().widthPixels - dp(42),
                        (int) (getResources().getDisplayMetrics().heightPixels * 0.42f)
                )
        );

        FrameLayout artworkWrap = new FrameLayout(this);
        artworkWrap.setLayoutParams(new LinearLayout.LayoutParams(artworkSize, artworkSize));
        artworkWrap.setPadding(dp(10), dp(10), dp(10), dp(10));

        artwork = new ImageView(this);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artwork.setBackground(roundedBackground(Color.rgb(36, 36, 44), 26));
        artwork.setClipToOutline(true);
        artwork.setOnTouchListener(new View.OnTouchListener() {
            private float downX;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getActionMasked()) {
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
                                    .scaleX(0.96f)
                                    .scaleY(0.96f)
                                    .setDuration(90)
                                    .withEndAction(() -> artwork.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(150)
                                            .start())
                                    .start();
                            return true;
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        FrameLayout.LayoutParams artworkParams =
                new FrameLayout.LayoutParams(artworkSize - dp(20), artworkSize - dp(20));
        artworkParams.gravity = Gravity.CENTER;
        artworkWrap.addView(artwork, artworkParams);

        playerScreen.addView(artworkWrap, new LinearLayout.LayoutParams(artworkSize, artworkSize));

        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        metaRow.setPadding(0, dp(10), 0, dp(4));

        LinearLayout songInfo = new LinearLayout(this);
        songInfo.setOrientation(LinearLayout.VERTICAL);

        title = new TextView(this);
        title.setText("Nothing Playing");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);

        artist = new TextView(this);
        artist.setText("Velora Music");
        artist.setTextColor(Color.rgb(169, 169, 180));
        artist.setTextSize(14);
        artist.setPadding(0, dp(2), 0, 0);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);

        songInfo.addView(title);
        songInfo.addView(artist);

        metaRow.addView(songInfo, new LinearLayout.LayoutParams(0, -2, 1));

        qualityBadge = labelText("HI-FI", 11, Color.rgb(206, 199, 255));
        qualityBadge.setBackground(roundedBackground(Color.argb(55, 190, 169, 255), 20));
        qualityBadge.setPadding(dp(10), dp(5), dp(10), dp(5));
        qualityBadge.setGravity(Gravity.CENTER);

        favoriteButton = labelText("♡", 30, Color.WHITE);
        favoriteButton.setGravity(Gravity.CENTER);
        favoriteButton.setPadding(dp(10), 0, dp(8), 0);
        favoriteButton.setOnClickListener(v -> toggleFavorite());

        metaRow.addView(qualityBadge, new LinearLayout.LayoutParams(-2, -2));
        metaRow.addView(favoriteButton, new LinearLayout.LayoutParams(dp(52), dp(52)));

        playerScreen.addView(metaRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        timeRow.setPadding(0, dp(10), 0, 0);

        currentTime = timeText("0:00");
        totalTime = timeText("0:00");

        progress = new SeekBar(this);
        progress.setMax(1000);
        progress.setProgress(0);
        progress.setPadding(0, 0, 0, 0);
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser && controller != null) {
                    long duration = controller.getDuration();
                    if (duration > 0) {
                        currentTime.setText(formatTime(duration * value / 1000L));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (controller != null) {
                    long duration = controller.getDuration();
                    if (duration > 0) {
                        controller.seekTo(duration * seekBar.getProgress() / 1000L);
                    }
                }
            }
        });

        timeRow.addView(currentTime, new LinearLayout.LayoutParams(dp(42), dp(32)));
        timeRow.addView(progress, new LinearLayout.LayoutParams(0, dp(42), 1));
        timeRow.addView(totalTime, new LinearLayout.LayoutParams(dp(42), dp(32)));

        playerScreen.addView(timeRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, dp(22), 0, dp(12));

        ImageButton previous = iconButton(android.R.drawable.ic_media_previous);
        previous.setOnClickListener(v -> {
            if (controller != null) {
                controller.seekToPreviousMediaItem();
            }
        });

        playButton = iconButton(android.R.drawable.ic_media_play);
        playButton.setBackground(roundedBackground(Color.rgb(190, 169, 255), 50));
        playButton.setColorFilter(Color.rgb(18, 17, 23));
        playButton.setPadding(dp(18), dp(18), dp(18), dp(18));
        playButton.setOnClickListener(v -> togglePlayback());

        ImageButton next = iconButton(android.R.drawable.ic_media_next);
        next.setOnClickListener(v -> {
            if (controller != null) {
                controller.seekToNextMediaItem();
            }
        });

        controls.addView(previous, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(dp(82), dp(82));
        playParams.setMargins(dp(12), 0, dp(12), 0);
        controls.addView(playButton, playParams);
        controls.addView(next, new LinearLayout.LayoutParams(dp(64), dp(64)));

        playerScreen.addView(controls, new LinearLayout.LayoutParams(-1, dp(96)));

        LinearLayout volumeRow = new LinearLayout(this);
        volumeRow.setGravity(Gravity.CENTER_VERTICAL);
        volumeRow.setPadding(0, dp(4), 0, dp(8));

        TextView low = labelText("−", 18, Color.rgb(155, 155, 165));
        TextView high = labelText("+", 18, Color.rgb(155, 155, 165));

        volume = new SeekBar(this);
        volume.setMax(100);
        volume.setProgress(100);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser && controller != null) {
                    controller.setVolume(value / 100f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        volumeRow.addView(low, new LinearLayout.LayoutParams(dp(28), dp(40)));
        volumeRow.addView(volume, new LinearLayout.LayoutParams(0, dp(40), 1));
        volumeRow.addView(high, new LinearLayout.LayoutParams(dp(28), dp(40)));

        playerScreen.addView(volumeRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout glassPanel = new LinearLayout(this);
        glassPanel.setGravity(Gravity.CENTER);
        glassPanel.setPadding(dp(6), dp(4), dp(6), dp(4));
        glassPanel.setBackground(roundedBackground(Color.argb(68, 255, 255, 255), 22));

        TextView lyrics = actionText("LYRICS");
        TextView queue = actionText("QUEUE");
        TextView moreAction = actionText("MORE");

        lyrics.setOnClickListener(v -> showLyrics());
        queue.setOnClickListener(v -> showQueue());
        moreAction.setOnClickListener(v -> showPlayerOptions());

        glassPanel.addView(lyrics, actionParams());
        glassPanel.addView(queue, actionParams());
        glassPanel.addView(moreAction, actionParams());

        LinearLayout.LayoutParams glassParams = new LinearLayout.LayoutParams(-1, dp(54));
        glassParams.topMargin = dp(14);
        playerScreen.addView(glassPanel, glassParams);

        content.addView(playerScreen, new LinearLayout.LayoutParams(-1, -1));

        modeOverlay = new FrameLayout(this);
        modeOverlay.setBackgroundColor(Color.argb(245, 8, 8, 11));
        modeOverlay.setVisibility(View.GONE);
        modeOverlay.setAlpha(0f);

        lyricsSheet = buildLyricsSheet();
        queueSheet = buildQueueSheet();

        modeOverlay.addView(lyricsSheet, new FrameLayout.LayoutParams(-1, -1));
        modeOverlay.addView(queueSheet, new FrameLayout.LayoutParams(-1, -1));
        root.addView(modeOverlay, new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);
    }

    private LinearLayout buildLyricsSheet() {
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(20), dp(16), dp(20), dp(16));
        sheet.setBackgroundColor(Color.argb(248, 8, 8, 11));
        sheet.setVisibility(View.GONE);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton close = iconButton(android.R.drawable.ic_menu_close_clear_cancel);
        close.setOnClickListener(v -> hideModeOverlay());

        LinearLayout labelGroup = new LinearLayout(this);
        labelGroup.setOrientation(LinearLayout.VERTICAL);
        labelGroup.setGravity(Gravity.CENTER);

        TextView panelTag = labelText("LYRICS", 10, Color.rgb(190, 190, 200));
        TextView panelTitle = labelText("VELORA", 9, Color.rgb(115, 115, 125));
        labelGroup.addView(panelTag);
        labelGroup.addView(panelTitle);

        topBar.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        topBar.addView(labelGroup, new LinearLayout.LayoutParams(0, dp(52), 1));

        sheet.addView(topBar, new LinearLayout.LayoutParams(-1, dp(58)));

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        lyricsBody = new TextView(this);
        lyricsBody.setTextColor(Color.rgb(242, 242, 246));
        lyricsBody.setTextSize(21);
        lyricsBody.setGravity(Gravity.CENTER_HORIZONTAL);
        lyricsBody.setLineSpacing(dp(7), 1.12f);
        lyricsBody.setPadding(dp(12), dp(35), dp(12), dp(60));
        lyricsBody.setText("Lyrics are not available for this track yet.");

        scroll.addView(lyricsBody, new android.widget.ScrollView.LayoutParams(-1, -2));
        sheet.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView playingTag = labelText("NOW PLAYING", 9, Color.rgb(190, 170, 245));
        playingTag.setPadding(0, dp(5), 0, dp(5));
        sheet.addView(playingTag, new LinearLayout.LayoutParams(-1, dp(32)));

        return sheet;
    }

    private LinearLayout buildQueueSheet() {
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(20), dp(16), dp(20), dp(16));
        sheet.setBackgroundColor(Color.argb(248, 8, 8, 11));
        sheet.setVisibility(View.GONE);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton close = iconButton(android.R.drawable.ic_menu_close_clear_cancel);
        close.setOnClickListener(v -> hideModeOverlay());

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setGravity(Gravity.CENTER);
        heading.addView(labelText("QUEUE", 10, Color.rgb(190, 190, 200)));
        heading.addView(labelText("VELORA", 9, Color.rgb(115, 115, 125)));

        topBar.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        topBar.addView(heading, new LinearLayout.LayoutParams(0, dp(52), 1));
        sheet.addView(topBar, new LinearLayout.LayoutParams(-1, dp(58)));

        queueListContainer = new LinearLayout(this);
        queueListContainer.setOrientation(LinearLayout.VERTICAL);
        queueListContainer.setPadding(0, dp(8), 0, dp(8));

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.addView(queueListContainer, new android.widget.ScrollView.LayoutParams(-1, -2));
        sheet.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        return sheet;
    }

    private void hideModeOverlay() {
        if (modeOverlay == null) {
            return;
        }

        modeOverlay.animate()
                .alpha(0f)
                .translationY(dp(12))
                .setDuration(180)
                .withEndAction(() -> {
                    modeOverlay.setVisibility(View.GONE);
                    lyricsSheet.setVisibility(View.GONE);
                    queueSheet.setVisibility(View.GONE);
                    playerScreen.setAlpha(1f);
                    playerScreen.setTranslationY(0f);
                })
                .start();
    }

    private void showLyricsSheet() {
        if (controller == null || controller.getCurrentMediaItem() == null) {
            showMessage("Lyrics", "Nothing is playing.");
            return;
        }

        if (lyricsBody == null) {
            return;
        }

        MediaMetadata metadata = controller.getCurrentMediaItem().mediaMetadata;
        String lyricsText = metadata.description != null
                ? metadata.description.toString()
                : "Lyrics are not available for this track yet.";
        lyricsBody.setText(lyricsText);

        lyricsSheet.setVisibility(View.VISIBLE);
        queueSheet.setVisibility(View.GONE);
        modeOverlay.setVisibility(View.VISIBLE);
        modeOverlay.setAlpha(0f);
        modeOverlay.setTranslationY(dp(18));
        modeOverlay.animate().alpha(1f).translationY(0f).setDuration(220).start();
        playerScreen.animate().alpha(0.18f).translationY(dp(12)).setDuration(180).start();
    }

    private void showQueueSheet() {
        if (controller == null || controller.getMediaItemCount() == 0) {
            showMessage("Queue", "Your queue is empty.");
            return;
        }

        refreshQueueOverlay();
        queueSheet.setVisibility(View.VISIBLE);
        lyricsSheet.setVisibility(View.GONE);
        modeOverlay.setVisibility(View.VISIBLE);
        modeOverlay.setAlpha(0f);
        modeOverlay.setTranslationY(dp(18));
        modeOverlay.animate().alpha(1f).translationY(0f).setDuration(220).start();
        playerScreen.animate().alpha(0.18f).translationY(dp(12)).setDuration(180).start();
    }

    private void refreshQueueOverlay() {
        if (queueListContainer == null || controller == null) {
            return;
        }

        queueListContainer.removeAllViews();

        for (int i = 0; i < controller.getMediaItemCount(); i++) {
            MediaItem item = controller.getMediaItemAt(i);
            MediaMetadata metadata = item.mediaMetadata;
            String trackName = metadata.title != null ? metadata.title.toString() : "Unknown Song";
            String artistName = metadata.artist != null ? metadata.artist.toString() : "Velora Music";
            boolean current = i == controller.getCurrentMediaItemIndex();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(10), dp(10), dp(10));
            row.setBackground(roundedBackground(current ? Color.argb(55, 190, 169, 255) : Color.argb(28, 255, 255, 255), 18));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(8);
            row.setLayoutParams(rowParams);

            ImageView thumb = new ImageView(this);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackground(roundedBackground(Color.rgb(34, 34, 42), 14));
            thumb.setLayoutParams(new LinearLayout.LayoutParams(dp(50), dp(50)));
            Bitmap art = resolveArtwork(item);
            if (art != null) {
                thumb.setImageBitmap(art);
            }
            row.addView(thumb);

            LinearLayout textWrap = new LinearLayout(this);
            textWrap.setOrientation(LinearLayout.VERTICAL);
            textWrap.setPadding(dp(12), 0, 0, 0);
            textWrap.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

            TextView rowTitle = labelText(current ? "▶  " + trackName : trackName, 15, Color.WHITE);
            rowTitle.setSingleLine(true);
            rowTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            rowTitle.setGravity(Gravity.START);

            TextView rowArtist = labelText(artistName, 12, Color.rgb(162, 162, 175));
            rowArtist.setSingleLine(true);
            rowArtist.setEllipsize(android.text.TextUtils.TruncateAt.END);
            rowArtist.setGravity(Gravity.START);

            textWrap.addView(rowTitle);
            textWrap.addView(rowArtist);
            row.addView(textWrap);

            row.setOnClickListener(v -> {
                if (controller != null) {
                    controller.seekToDefaultPosition(i);
                    hideModeOverlay();
                }
            });

            queueListContainer.addView(row);
        }
    }

    private Bitmap resolveArtwork(MediaItem item) {
        if (item == null) {
            return null;
        }

        MediaMetadata metadata = item.mediaMetadata;
        if (metadata != null && metadata.artworkData != null) {
            Bitmap art = BitmapFactory.decodeByteArray(metadata.artworkData, 0, metadata.artworkData.length);
            if (art != null) {
                return art;
            }
        }

        if (item.localConfiguration != null) {
            return getEmbeddedArtwork(item.localConfiguration.uri);
        }

        return null;
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
        showQueueSheet();
    }

    private void showLyrics() {
        showLyricsSheet();
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
