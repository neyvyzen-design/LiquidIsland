package com.liquid.island;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
import android.graphics.Color;

public class MainActivity extends Activity {

    Button start, stop;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setPadding(35,60,35,35);
        layout.setBackgroundColor(Color.rgb(9,10,16));

        TextView title = new TextView(this);
        title.setText("LIQUID ISLAND");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setPadding(0,0,0,15);
        layout.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Native Dynamic Island Experience");
        desc.setTextColor(Color.LTGRAY);
        layout.addView(desc);

        start = new Button(this);
        start.setText("AKTIFKAN ISLAND");
        layout.addView(start);

        stop = new Button(this);
        stop.setText("MATIKAN ISLAND");
        layout.addView(stop);

        setContentView(layout);

        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent i = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(i);
            } else {
                Intent service = new Intent(this, IslandService.class);
                if (Build.VERSION.SDK_INT >= 26)
                    startForegroundService(service);
                else startService(service);
                Toast.makeText(this,"Island aktif!",Toast.LENGTH_SHORT).show();
            }
        });

        stop.setOnClickListener(v -> {
            stopService(new Intent(this, IslandService.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
