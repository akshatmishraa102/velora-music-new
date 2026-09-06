package com.veloramusic;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;

public class PlayerActivity extends Activity {

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;

    private ImageView artwork;
    private TextView title;
    private TextView artist;
    private TextView currentTime;
    private TextView totalTime;
    private SeekBar progress;
    private SeekBar volume;
    private ImageButton playButton;

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

        SessionToken token =
                new SessionToken(this, new android.content.ComponentName(
                        this,
                        PlaybackService.class
                ));

        controllerFuture = new MediaController.Builder(this, token).buildAsync();

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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));

root.setOnApplyWindowInsetsListener((v, insets) -> {
    int left;
    int top;
    int right;
    int bottom;

    if (android.os.Build.VERSION.SDK_INT >= 30) {
        android.graphics.Insets bars =
                insets.getInsets(android.view.WindowInsets.Type.systemBars());

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
            dp(20) + top,
            dp(20) + right,
            dp(20) + bottom
    );

    return insets;
});

root.requestApplyInsets();

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.rgb(24, 23, 31),
                        Color.rgb(8, 8, 11)
                }
        );
        root.setBackground(background);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(15));

        ImageButton close = new ImageButton(this);
        close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        close.setColorFilter(Color.WHITE);
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setOnClickListener(v -> finish());

        topBar.addView(close, new LinearLayout.LayoutParams(
                dp(48),
                dp(48)
        ));

        TextView nowPlaying = new TextView(this);
        nowPlaying.setText("NOW PLAYING");
        nowPlaying.setTextColor(Color.rgb(175, 175, 185));
        nowPlaying.setTextSize(11);
        nowPlaying.setGravity(Gravity.CENTER);

        topBar.addView(nowPlaying, new LinearLayout.LayoutParams(
                0,
                dp(48),
                1
        ));

        ImageButton more = new ImageButton(this);
        more.setImageResource(android.R.drawable.ic_menu_more);
        more.setColorFilter(Color.WHITE);
        more.setBackgroundColor(Color.TRANSPARENT);

        topBar.addView(more, new LinearLayout.LayoutParams(
                dp(48),
                dp(48)
        ));

        root.addView(topBar);

        // Artwork
        artwork = new ImageView(this);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);

        GradientDrawable artworkBackground = new GradientDrawable();
        artworkBackground.setColor(Color.rgb(38, 38, 45));
        artworkBackground.setCornerRadius(dp(24));
        artwork.setBackground(artworkBackground);
        artwork.setClipToOutline(true);
int artworkSize = Math.min(
        dp(310),
        Math.min(
                getResources().getDisplayMetrics().widthPixels - dp(40),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.40f)
        )
);
        
        LinearLayout.LayoutParams artworkParams =
                new LinearLayout.LayoutParams(
                        artworkSize,
                        artworkSize
                );

        artworkParams.gravity = Gravity.CENTER_HORIZONTAL;
        artworkParams.topMargin = dp(8);

        root.addView(artwork, artworkParams);

        // Song information
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(4), dp(22), dp(4), dp(5));

        title = new TextView(this);
        title.setText("Nothing Playing");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);

        artist = new TextView(this);
        artist.setText("Velora Music");
        artist.setTextColor(Color.rgb(160, 160, 170));
        artist.setTextSize(14);
        artist.setGravity(Gravity.CENTER);
        artist.setPadding(0, dp(5), 0, 0);
        artist.setSingleLine(true);

        info.addView(title);
        info.addView(artist);

        root.addView(info, new LinearLayout.LayoutParams(
                -1,
                dp(75)
        ));

        // Progress
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);

        currentTime = timeText("0:00");
        totalTime = timeText("0:00");

        progress = new SeekBar(this);
        progress.setMax(1000);
        progress.setProgress(0);

        progress.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progressValue,
                            boolean fromUser
                    ) {
                        if (fromUser && controller != null) {
                            long duration = controller.getDuration();

                            if (duration > 0) {
                                long position =
                                        duration * progressValue / 1000L;
                                currentTime.setText(formatTime(position));
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
                                long position =
                                        duration * seekBar.getProgress() / 1000L;
                                controller.seekTo(position);
                            }
                        }
                    }
                }
        );

        timeRow.addView(currentTime);

        timeRow.addView(progress, new LinearLayout.LayoutParams(
                0,
                dp(45),
                1
        ));

        timeRow.addView(totalTime);

        root.addView(timeRow);

        // Main controls
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, dp(5), 0, dp(5));

        ImageButton previous = controlButton(
                android.R.drawable.ic_media_previous
        );

        playButton = controlButton(
                android.R.drawable.ic_media_play
        );

        ImageButton next = controlButton(
                android.R.drawable.ic_media_next
        );

        LinearLayout.LayoutParams sideParams =
                new LinearLayout.LayoutParams(dp(64), dp(64));

        controls.addView(previous, sideParams);

        LinearLayout.LayoutParams playParams =
                new LinearLayout.LayoutParams(dp(78), dp(78));
        playParams.setMargins(dp(12), 0, dp(12), 0);

        controls.addView(playButton, playParams);
        controls.addView(next, sideParams);

        previous.setOnClickListener(v -> {
            if (controller != null) {
                controller.seekToPreviousMediaItem();
            }
        });

        next.setOnClickListener(v -> {
            if (controller != null) {
                controller.seekToNextMediaItem();
            }
        });

        playButton.setOnClickListener(v -> togglePlayback());

        root.addView(controls);

        // Volume
        LinearLayout volumeRow = new LinearLayout(this);
        volumeRow.setGravity(Gravity.CENTER_VERTICAL);
        volumeRow.setPadding(dp(4), dp(5), dp(4), 0);

        TextView volumeIcon = new TextView(this);
        volumeIcon.setText("−");
        volumeIcon.setTextColor(Color.rgb(170, 170, 180));
        volumeIcon.setTextSize(20);
        volumeIcon.setGravity(Gravity.CENTER);

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
                }
        );

        TextView volumeIcon2 = new TextView(this);
        volumeIcon2.setText("+");
        volumeIcon2.setTextColor(Color.rgb(170, 170, 180));
        volumeIcon2.setTextSize(20);
        volumeIcon2.setGravity(Gravity.CENTER);

        volumeRow.addView(volumeIcon, new LinearLayout.LayoutParams(
                dp(30),
                dp(45)
        ));

        volumeRow.addView(volume, new LinearLayout.LayoutParams(
                0,
                dp(45),
                1
        ));

        volumeRow.addView(volumeIcon2, new LinearLayout.LayoutParams(
                dp(30),
                dp(45)
        ));

        root.addView(volumeRow);

        // Bottom actions
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(10), 0, 0);

        TextView lyrics = actionText("LYRICS");
        TextView queue = actionText("QUEUE");
        TextView lossless = actionText("LOSSLESS");

        actions.addView(lyrics, actionParams());
        actions.addView(queue, actionParams());
        actions.addView(lossless, actionParams());

        root.addView(actions);

        setContentView(root);
    }

    private void refreshPlayer() {
        if (controller == null) {
            return;
        }

        MediaItem item = controller.getCurrentMediaItem();

        if (item == null) {
            title.setText("Nothing Playing");
            artist.setText("Velora Music");
            return;
        }

        MediaMetadata metadata = item.mediaMetadata;

        String songTitle = metadata.title != null
                ? metadata.title.toString()
                : "Unknown Song";

        String songArtist = metadata.artist != null
                ? metadata.artist.toString()
                : "Unknown Artist";

        title.setText(songTitle);
        artist.setText(songArtist);

        updateArtwork(item);

        if (controller.isPlaying()) {
            playButton.setImageResource(
                    android.R.drawable.ic_media_pause
            );
        } else {
            playButton.setImageResource(
                    android.R.drawable.ic_media_play
            );
        }

        float currentVolume = controller.getVolume();
        volume.setProgress((int) (currentVolume * 100));
    }

    private void updateArtwork(MediaItem item) {

        Bitmap bitmap = null;

        MediaMetadata metadata = item.mediaMetadata;

        if (metadata.artworkData != null) {
            bitmap = BitmapFactory.decodeByteArray(
                    metadata.artworkData,
                    0,
                    metadata.artworkData.length
            );
        }

        if (bitmap == null &&
                item.localConfiguration != null) {

            Uri uri = item.localConfiguration.uri;

            bitmap = getEmbeddedArtwork(uri);
        }

        if (bitmap != null) {
            artwork.setImageBitmap(bitmap);
        } else {
            artwork.setImageResource(
                    android.R.drawable.ic_media_play
            );
        }
    }

    private Bitmap getEmbeddedArtwork(Uri uri) {

        MediaMetadataRetriever retriever =
                new MediaMetadataRetriever();

        try {
            retriever.setDataSource(
                    this,
                    uri
            );

            byte[] data = retriever.getEmbeddedPicture();

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

    private void updateProgress() {

        if (controller == null) {
            return;
        }

        long duration = controller.getDuration();
        long position = controller.getCurrentPosition();

        if (duration > 0) {

            int value = (int)
                    ((position * 1000L) / duration);

            progress.setProgress(value);

            currentTime.setText(
                    formatTime(position)
            );

            totalTime.setText(
                    formatTime(duration)
            );
        }

        if (controller.isPlaying()) {
            playButton.setImageResource(
                    android.R.drawable.ic_media_pause
            );
        } else {
            playButton.setImageResource(
                    android.R.drawable.ic_media_play
            );
        }
    }

    private ImageButton controlButton(int icon) {

        ImageButton button = new ImageButton(this);

        button.setImageResource(icon);
        button.setColorFilter(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setScaleType(ImageView.ScaleType.CENTER);

        return button;
    }

    private TextView timeText(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(Color.rgb(145, 145, 155));
        view.setTextSize(11);
        view.setGravity(Gravity.CENTER);

        return view;
    }

    private TextView actionText(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(Color.rgb(175, 175, 185));
        view.setTextSize(10);
        view.setGravity(Gravity.CENTER);

        return view;
    }

    private LinearLayout.LayoutParams actionParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        dp(40),
                        1
                );

        return params;
    }

    private String formatTime(long milliseconds) {

        long totalSeconds = milliseconds / 1000;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format(
                Locale.US,
                "%d:%02d",
                minutes,
                seconds
        );
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (controller != null) {
            refreshPlayer();
        }

        handler.post(progressUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(progressUpdater);
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(progressUpdater);

        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }

        controller = null;

        super.onDestroy();
    }
}
