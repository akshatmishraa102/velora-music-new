package com.veloramusic;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutActivity extends Activity {
    private static final String INSTAGRAM_URL = "https://www.instagram.com/akshat.mishra102?igsi=a3l3ajF0dnRpcWRz";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(9, 10, 14));
        root.setPadding(dp(20), dp(24), dp(20), dp(24));

        TextView title = new TextView(this);
        title.setText("VELORA");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView appName = new TextView(this);
        appName.setText("Velora Music");
        appName.setTextColor(Color.WHITE);
        appName.setTextSize(22);
        appName.setTypeface(null, android.graphics.Typeface.BOLD);
        appName.setPadding(0, dp(8), 0, dp(18));
        appName.setGravity(Gravity.CENTER);
        root.addView(appName);

        TextView developer = new TextView(this);
        developer.setText("Developer: Akshat Mishra");
        developer.setTextColor(Color.argb(220, 255, 255, 255));
        developer.setTextSize(16);
        developer.setPadding(0, dp(8), 0, dp(8));
        root.addView(developer);

        TextView handle = new TextView(this);
        handle.setText("Instagram: @akshat.mishra102");
        handle.setTextColor(Color.rgb(184, 167, 255));
        handle.setTextSize(15);
        handle.setPadding(0, dp(4), 0, dp(12));
        root.addView(handle);

        TextView urlLabel = new TextView(this);
        urlLabel.setText("Instagram URL:");
        urlLabel.setTextColor(Color.argb(220, 255, 255, 255));
        urlLabel.setTextSize(14);
        root.addView(urlLabel);

        TextView url = new TextView(this);
        url.setText(INSTAGRAM_URL);
        url.setTextColor(Color.argb(210, 185, 200, 255));
        url.setTextSize(13);
        url.setPadding(0, dp(6), 0, dp(18));
        root.addView(url);

        Button open = new Button(this);
        open.setText("Open Instagram");
        open.setOnClickListener(v -> openUrl(INSTAGRAM_URL));
        root.addView(open);

        Button close = new Button(this);
        close.setText("Back");
        close.setOnClickListener(v -> finish());
        root.addView(close);

        setContentView(root);
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
