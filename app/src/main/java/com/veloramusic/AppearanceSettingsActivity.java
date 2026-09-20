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

public class AppearanceSettingsActivity extends Activity {
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(VeloraThemeManager.PREF_NAME, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(9, 10, 14));
        root.setPadding(dp(18), dp(16), dp(18), dp(18));

        TextView title = new TextView(this);
        title.setText("Appearance settings");
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
        content.setPadding(0, dp(12), 0, 0);

        addToggleRow(content, "Dynamic theme", prefs.getBoolean(VeloraThemeManager.KEY_DYNAMIC_THEME, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_DYNAMIC_THEME, checked).apply());
        addToggleRow(content, "Pure black mode", prefs.getBoolean(VeloraThemeManager.KEY_PURE_BLACK, false),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_PURE_BLACK, checked).apply());
        addToggleRow(content, "High refresh rate", prefs.getBoolean(VeloraThemeManager.KEY_HIGH_REFRESH_RATE, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_HIGH_REFRESH_RATE, checked).apply());
        addToggleRow(content, "Show quality badge", prefs.getBoolean(VeloraThemeManager.KEY_SHOW_QUALITY_BADGE, false),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_SHOW_QUALITY_BADGE, checked).apply());
        addToggleRow(content, "Use new player design", prefs.getBoolean(VeloraThemeManager.KEY_USE_NEW_PLAYER_DESIGN, true),
                (checked) -> prefs.edit().putBoolean(VeloraThemeManager.KEY_USE_NEW_PLAYER_DESIGN, checked).apply());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private void addToggleRow(LinearLayout parent, String title, boolean checked, ToggleChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackgroundColor(Color.argb(22, 255, 255, 255));
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView text = new TextView(this);
        text.setText(title);
        text.setTextColor(Color.WHITE);
        text.setTextSize(15);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text);

        CheckBox checkBox = new CheckBox(this);
        checkBox.setChecked(checked);
        checkBox.setOnCheckedChangeListener((button, isChecked) -> listener.onChange(isChecked));
        row.addView(checkBox);

        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private interface ToggleChangeListener {
        void onChange(boolean checked);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
