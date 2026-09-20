package com.veloramusic;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class PlayerSettingsActivity extends Activity {
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(VeloraThemeManager.PREF_NAME, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(9, 10, 14));
        root.setPadding(dp(18), dp(16), dp(18), dp(16));

        TextView title = new TextView(this);
        title.setText("Player settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(16));
        root.addView(title);

        Button close = new Button(this);
        close.setText("Back");
        close.setOnClickListener(v -> finish());
        root.addView(close);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView artworkTitle = sectionTitle("Artwork");
        content.addView(artworkTitle);
        addRadiusSelector(content);
        addToggle(content, "Animated artwork", prefs.getBoolean(VeloraThemeManager.KEY_PLAYER_ARTWORK_ANIMATION, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_PLAYER_ARTWORK_ANIMATION, checked).apply());
        addToggle(content, "Dynamic album accent", prefs.getBoolean(VeloraThemeManager.KEY_PLAYER_DYNAMIC_ACCENT, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_PLAYER_DYNAMIC_ACCENT, checked).apply());

        TextView interactionTitle = sectionTitle("Interaction");
        content.addView(interactionTitle);
        addToggle(content, "Swipe to change track", prefs.getBoolean(VeloraThemeManager.KEY_PLAYER_SWIPE_GESTURE, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_PLAYER_SWIPE_GESTURE, checked).apply());
        addToggle(content, "Persistent queue", prefs.getBoolean(VeloraThemeManager.KEY_PERSIST_QUEUE, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_PERSIST_QUEUE, checked).apply());

        TextView styleTitle = sectionTitle("Progress and layout");
        content.addView(styleTitle);
        addProgressStyleSelector(content);
        addToggle(content, "Keep screen on while playing", prefs.getBoolean(VeloraThemeManager.KEY_KEEP_SCREEN_ON, false),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_KEEP_SCREEN_ON, checked).apply());
        addToggle(content, "Audio normalization", prefs.getBoolean(VeloraThemeManager.KEY_AUDIO_NORMALIZATION, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_AUDIO_NORMALIZATION, checked).apply());
        addToggle(content, "Pause on mute", prefs.getBoolean(VeloraThemeManager.KEY_PAUSE_ON_MUTE, false),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_PAUSE_ON_MUTE, checked).apply());
        addToggle(content, "Resume on Bluetooth", prefs.getBoolean(VeloraThemeManager.KEY_RESUME_ON_BLUETOOTH, false),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_RESUME_ON_BLUETOOTH, checked).apply());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private void addRadiusSelector(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackgroundColor(Color.argb(22, 255, 255, 255));

        TextView label = new TextView(this);
        label.setText("Artwork radius");
        label.setTextColor(Color.WHITE);
        label.setTextSize(15);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(label);

        String[] choices = {"Soft", "Standard", "Sharp"};
        int[] values = {18, 30, 42};
        for (int i = 0; i < choices.length; i++) {
            final int value = values[i];
            Button chip = new Button(this);
            chip.setText(choices[i]);
            chip.setOnClickListener(v -> prefs.edit().putInt(VeloraThemeManager.KEY_PLAYER_ARTWORK_RADIUS, value).apply());
            row.addView(chip, new LinearLayout.LayoutParams(-2, -2));
        }
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addProgressStyleSelector(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackgroundColor(Color.argb(22, 255, 255, 255));

        TextView label = new TextView(this);
        label.setText("Progress style");
        label.setTextColor(Color.WHITE);
        label.setTextSize(15);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(label);

        Button smooth = new Button(this);
        smooth.setText("Smooth");
        smooth.setOnClickListener(v -> prefs.edit().putString(VeloraThemeManager.KEY_PLAYER_PROGRESS_STYLE, "smooth").apply());
        row.addView(smooth, new LinearLayout.LayoutParams(-2, -2));

        Button minimal = new Button(this);
        minimal.setText("Minimal");
        minimal.setOnClickListener(v -> prefs.edit().putString(VeloraThemeManager.KEY_PLAYER_PROGRESS_STYLE, "minimal").apply());
        row.addView(minimal, new LinearLayout.LayoutParams(-2, -2));

        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private TextView sectionTitle(String title) {
        TextView text = new TextView(this);
        text.setText(title);
        text.setTextColor(Color.argb(225, 255, 255, 255));
        text.setTextSize(16);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setPadding(dp(12), dp(20), dp(12), dp(8));
        return text;
    }

    private void addToggle(LinearLayout parent, String label, boolean checked, ToggleChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackgroundColor(Color.argb(22, 255, 255, 255));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(15);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text);

        CheckBox checkbox = new CheckBox(this);
        checkbox.setChecked(checked);
        checkbox.setOnCheckedChangeListener((button, isChecked) -> listener.onChange(isChecked));
        row.addView(checkbox);

        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private interface ToggleChangeListener {
        void onChange(boolean checked);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
