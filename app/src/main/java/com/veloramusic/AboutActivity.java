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
        title.setText("Velora Music");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("Lightweight local-player experience built with Java + Media3, inspired by Vivi’s premium layout and stateful player flow.");
        description.setTextColor(Color.argb(210, 236, 236, 240));
        description.setTextSize(15);
        description.setPadding(0, dp(10), 0, dp(18));
        description.setLineSpacing(0, 1.25f);
        root.addView(description);

        TextView developer = new TextView(this);
        developer.setText("Developer: Akshat Mishra");
        developer.setTextColor(Color.WHITE);
        developer.setTextSize(16);
        root.addView(developer);

        TextView handle = new TextView(this);
        handle.setText("Instagram: @akshat.mishra102");
        handle.setTextColor(Color.rgb(184, 167, 255));
        handle.setTextSize(15);
        handle.setPadding(0, dp(8), 0, dp(22));
        root.addView(handle);

        Button ig = new Button(this);
        ig.setText("Open Instagram");
        ig.setOnClickListener(v -> openUrl(INSTAGRAM_URL));
        root.addView(ig);

        Button source = new Button(this);
        source.setText("Open source / project info");
        source.setOnClickListener(v -> openUrl("https://github.com/"));
        root.addView(source);

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
