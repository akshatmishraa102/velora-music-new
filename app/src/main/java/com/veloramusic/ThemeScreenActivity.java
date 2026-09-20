package com.veloramusic;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ThemeScreenActivity extends Activity {
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
        title.setText("Theme screen");
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

        addModeRow(content, "Dark", "dark");
        addModeRow(content, "AMOLED", "amoled");
        addModeRow(content, "Light", "light");
        addModeRow(content, "Auto", "auto");

        TextView accentLabel = new TextView(this);
        accentLabel.setText("Accent colors");
        accentLabel.setTextColor(Color.argb(220, 255, 255, 255));
        accentLabel.setTextSize(16);
        accentLabel.setPadding(dp(12), dp(20), dp(12), dp(10));
        content.addView(accentLabel);

        LinearLayout swatches = new LinearLayout(this);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        swatches.setGravity(Gravity.CENTER_VERTICAL);
        swatches.setPadding(dp(12), dp(0), dp(12), dp(16));
        for (int color : VeloraThemeManager.ACCENT_COLORS) {
            View chip = new View(this);
            chip.setBackgroundColor(color);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(36), dp(36));
            p.setMargins(0, 0, dp(10), 0);
            chip.setOnClickListener(v -> {
                prefs.edit().putInt(VeloraThemeManager.KEY_ACCENT, color).apply();
                finish();
            });
            swatches.addView(chip, p);
        }
        content.addView(swatches);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private void addModeRow(LinearLayout parent, String label, String theme) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(Color.WHITE);
        row.setTextSize(15);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackgroundColor(Color.argb(26, 255, 255, 255));
        row.setOnClickListener(v -> {
            prefs.edit().putString(VeloraThemeManager.KEY_THEME, theme).apply();
            finish();
        });
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
