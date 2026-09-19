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
        topBar.setPadding(dp(8), dp(12), dp(8), dp(8));

        ImageButton close = iconButton(android.R.drawable.ic_menu_close_clear_cancel);
        close.setOnClickListener(v -> finish());

        LinearLayout dragWrap = new LinearLayout(this);
        dragWrap.setGravity(Gravity.CENTER);
        dragWrap.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

        View dragHandle = new View(this);
        dragHandle.setBackground(roundedBackground(Color.argb(170, 255, 255, 255), 999));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(42), dp(5));
        handleParams.gravity = Gravity.CENTER;
        dragWrap.addView(dragHandle, handleParams);

        TextView brandBadge = labelText("VELORA", 9, Color.argb(180, 255, 255, 255));
        brandBadge.setGravity(Gravity.CENTER);
        brandBadge.setPadding(dp(8), 0, dp(8), 0);

        ImageButton more = iconButton(android.R.drawable.ic_menu_more);
        more.setOnClickListener(v -> showPlayerOptions());

        topBar.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        topBar.addView(dragWrap, new LinearLayout.LayoutParams(0, -2, 1));
        topBar.addView(brandBadge, new LinearLayout.LayoutParams(-2, -2));
        topBar.addView(more, new LinearLayout.LayoutParams(dp(48), dp(48)));

        content.addView(topBar, new LinearLayout.LayoutParams(-1, dp(60)));

        playerScreen = new LinearLayout(this);
        playerScreen.setOrientation(LinearLayout.VERTICAL);
        playerScreen.setGravity(Gravity.CENTER_HORIZONTAL);
        playerScreen.setPadding(0, dp(4), 0, dp(24));

        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        int artworkSize = Math.min(
                dp(360),
                Math.max(
                        dp(270),
                        displayWidth - dp(62)
                )
        );

        FrameLayout artworkWrap = new FrameLayout(this);
        artworkWrap.setLayoutParams(new LinearLayout.LayoutParams(artworkSize, artworkSize));
        artworkWrap.setPadding(dp(10), dp(10), dp(10), dp(10));

        View artworkShadow = new View(this);
        artworkShadow.setBackground(roundedBackground(Color.argb(55, 0, 0, 0), 30));
        artworkShadow.setElevation(dp(18));
        FrameLayout.LayoutParams shadowParams = new FrameLayout.LayoutParams(-1, -1);
        shadowParams.setMargins(dp(14), dp(18), dp(14), dp(8));
        artworkWrap.addView(artworkShadow, shadowParams);

        artwork = new ImageView(this);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artwork.setBackground(roundedBackground(Color.rgb(31, 30, 40), 30));
        artwork.setClipToOutline(true);
        artwork.setImageDrawable(null);
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

        FrameLayout.LayoutParams artworkParams = new FrameLayout.LayoutParams(-1, -1);
        artworkParams.setMargins(dp(10), dp(8), dp(10), dp(10));
        artworkWrap.addView(artwork, artworkParams);

        playerScreen.addView(artworkWrap, new LinearLayout.LayoutParams(artworkSize, artworkSize));

        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        metaRow.setPadding(dp(6), dp(18), dp(6), dp(4));

        LinearLayout songInfo = new LinearLayout(this);
        songInfo.setOrientation(LinearLayout.VERTICAL);
        songInfo.setPadding(0, 0, dp(8), 0);

        title = new TextView(this);
        title.setText("Nothing Playing");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);

        artist = new TextView(this);
        artist.setText("Velora Music");
        artist.setTextColor(Color.argb(200, 234, 234, 242));
        artist.setTextSize(14);
        artist.setPadding(0, dp(4), 0, 0);
        artist.setSingleLine(true);
        artist.setEllipsize(android.text.TextUtils.TruncateAt.END);

        songInfo.addView(title);
        songInfo.addView(artist);

        metaRow.addView(songInfo, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout infoActions = new LinearLayout(this);
        infoActions.setOrientation(LinearLayout.VERTICAL);
        infoActions.setGravity(Gravity.CENTER_HORIZONTAL);

        qualityBadge = labelText("HI-FI", 10, Color.argb(220, 238, 236, 255));
        qualityBadge.setBackground(roundedBackground(Color.argb(38, 219, 214, 255), 18));
        qualityBadge.setPadding(dp(10), dp(6), dp(10), dp(6));
        qualityBadge.setGravity(Gravity.CENTER);

        favoriteButton = labelText("♡", 26, Color.WHITE);
        favoriteButton.setBackground(roundedBackground(Color.argb(20, 255, 255, 255), 18));
        favoriteButton.setGravity(Gravity.CENTER);
        favoriteButton.setPadding(dp(12), dp(8), dp(12), dp(8));
        favoriteButton.setOnClickListener(v -> toggleFavorite());

        infoActions.addView(qualityBadge, new LinearLayout.LayoutParams(-2, -2));
        infoActions.addView(favoriteButton, new LinearLayout.LayoutParams(dp(52), dp(52)));

        metaRow.addView(infoActions, new LinearLayout.LayoutParams(-2, -2));

        playerScreen.addView(metaRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        timeRow.setPadding(dp(4), dp(18), dp(4), 0);

        currentTime = timeText("0:00");
        totalTime = timeText("0:00");

        progress = new SeekBar(this);
        progress.setMax(1000);
        progress.setProgress(0);
        progress.setPadding(0, 0, 0, 0);
        progress.setThumbOffset(dp(6));
        progress.setBackground(null);
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

        timeRow.addView(currentTime, new LinearLayout.LayoutParams(dp(44), dp(32)));
        timeRow.addView(progress, new LinearLayout.LayoutParams(0, dp(38), 1));
        timeRow.addView(totalTime, new LinearLayout.LayoutParams(dp(44), dp(32)));

        playerScreen.addView(timeRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, dp(24), 0, dp(10));

        ImageButton previous = iconButton(android.R.drawable.ic_media_previous);
        previous.setBackground(roundedBackground(Color.argb(24, 255, 255, 255), 24));
        previous.setPadding(dp(18), dp(18), dp(18), dp(18));
        previous.setOnClickListener(v -> {
            if (controller != null) {
                controller.seekToPreviousMediaItem();
            }
        });

        playButton = iconButton(android.R.drawable.ic_media_play);
        playButton.setBackground(roundedBackground(Color.WHITE, 30));
        playButton.setColorFilter(Color.rgb(18, 17, 23));
        playButton.setPadding(dp(22), dp(22), dp(22), dp(22));
        playButton.setOnClickListener(v -> togglePlayback());

        ImageButton next = iconButton(android.R.drawable.ic_media_next);
        next.setBackground(roundedBackground(Color.argb(24, 255, 255, 255), 24));
        next.setPadding(dp(18), dp(18), dp(18), dp(18));
        next.setOnClickListener(v -> {
            if (controller != null) {
                controller.seekToNextMediaItem();
            }
        });

        controls.addView(previous, new LinearLayout.LayoutParams(dp(68), dp(68)));
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(dp(90), dp(90));
        playParams.setMargins(dp(16), 0, dp(16), 0);
        controls.addView(playButton, playParams);
        controls.addView(next, new LinearLayout.LayoutParams(dp(68), dp(68)));

        playerScreen.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout volumeRow = new LinearLayout(this);
        volumeRow.setGravity(Gravity.CENTER_VERTICAL);
        volumeRow.setPadding(dp(4), dp(18), dp(4), dp(8));

        TextView low = labelText("−", 18, Color.argb(200, 255, 255, 255));
        low.setGravity(Gravity.CENTER);
        TextView high = labelText("+", 18, Color.argb(200, 255, 255, 255));
        high.setGravity(Gravity.CENTER);

        volume = new SeekBar(this);
        volume.setMax(100);
        volume.setProgress(100);
        volume.setPadding(0, 0, 0, 0);
        volume.setThumbOffset(dp(6));
        volume.setBackground(null);
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

        LinearLayout utilityBar = new LinearLayout(this);
        utilityBar.setGravity(Gravity.CENTER);
        utilityBar.setPadding(dp(6), dp(18), dp(6), 0);

        TextView lyrics = actionText("LYRICS");
        lyrics.setBackground(roundedBackground(Color.argb(26, 255, 255, 255), 18));
        lyrics.setGravity(Gravity.CENTER);
        lyrics.setPadding(dp(12), dp(12), dp(12), dp(12));
        lyrics.setOnClickListener(v -> showLyrics());

        TextView queue = actionText("QUEUE");
        queue.setBackground(roundedBackground(Color.argb(26, 255, 255, 255), 18));
        queue.setGravity(Gravity.CENTER);
        queue.setPadding(dp(12), dp(12), dp(12), dp(12));
        queue.setOnClickListener(v -> showQueue());

        TextView moreAction = actionText("MORE");
        moreAction.setBackground(roundedBackground(Color.argb(26, 255, 255, 255), 18));
        moreAction.setGravity(Gravity.CENTER);
        moreAction.setPadding(dp(12), dp(12), dp(12), dp(12));
        moreAction.setOnClickListener(v -> showPlayerOptions());

        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, -2, 1);
        actionParams.setMargins(dp(8), 0, dp(8), 0);
        utilityBar.addView(lyrics, actionParams);
        utilityBar.addView(queue, actionParams);
        utilityBar.addView(moreAction, actionParams);

        playerScreen.addView(utilityBar, new LinearLayout.LayoutParams(-1, -2));

        content.addView(playerScreen, new LinearLayout.LayoutParams(-1, 0, 1));

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
            final int queueIndex = i;
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
                    controller.seekToDefaultPosition(queueIndex);
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

        int avgR = (int) (r / count);
        int avgG = (int) (g / count);
        int avgB = (int) (b / count);

        int accent = Color.rgb(
                clamp(avgR + 30, 0, 255),
                clamp(avgG + 12, 0, 255),
                clamp(avgB + 16, 0, 255)
        );

        int deep = Color.rgb(
                clamp(avgR / 3, 0, 64),
                clamp(avgG / 3, 0, 64),
                clamp(avgB / 3, 0, 72)
        );

        if (playButton != null) {
            playButton.setBackground(
                    roundedBackground(
                            Color.argb(235, Color.red(accent), Color.green(accent), Color.blue(accent)),
                            30
                    )
            );
            playButton.setColorFilter(Color.rgb(18, 17, 23));
        }

        darkOverlay.setBackground(
                new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{
                                Color.argb(120, Color.red(accent), Color.green(accent), Color.blue(accent)),
                                Color.argb(220, 10, 10, 18),
                                Color.argb(245, 6, 6, 12)
                        }
                )
        );

        if (volume != null) {
            volume.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
            if (Build.VERSION.SDK_INT >= 21) {
                volume.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
            }
        }

        if (progress != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
                progress.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
            }
        }

        if (root != null) {
            root.setBackgroundColor(
                    Color.argb(255, Math.min(14, Color.red(deep)), Math.min(14, Color.green(deep)), Math.min(20, Color.blue(deep)))
            );
        }
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

        if (root != null) {
            root.setBackgroundColor(Color.rgb(8, 8, 11));
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
