package com.liquid.island;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import android.animation.ValueAnimator;

public class IslandService extends Service {

    WindowManager wm;
    LinearLayout island;
    LinearLayout content;
    TextView title, subtitle;
    boolean expanded = false;
    int widthClosed, widthOpen;
    int heightClosed = 48, heightOpen = 125;

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationManager nm =
            (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                "island_service",
                "Liquid Island",
                NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(channel);
        }

        Notification.Builder builder = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT >= 26)
            builder.setChannelId("island_service");

        Notification notification = builder
            .setContentTitle("Liquid Island")
            .setContentText("Dynamic Island sedang aktif")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build();

        startForeground(1, notification);

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);

        widthClosed = dp(125);
        widthOpen = dp(300);

        createIsland();
    }

    int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density);
    }

    GradientDrawable glass(int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(25,27,34));
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), Color.argb(95,255,255,255));
        return bg;
    }

    void createIsland() {
        island = new LinearLayout(this);
        island.setOrientation(1);
        island.setGravity(Gravity.CENTER);
        island.setBackground(glass(35));
        island.setElevation(dp(12));

        TextView dot = new TextView(this);
        dot.setText("●");
        dot.setTextColor(Color.rgb(0,220,255));
        dot.setTextSize(13);
        island.addView(dot);

        content = new LinearLayout(this);
        content.setOrientation(1);
        content.setPadding(dp(18),dp(12),dp(18),dp(12));
        content.setVisibility(View.GONE);

        title = new TextView(this);
        title.setText("Liquid Island");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(null,Typeface.BOLD);

        subtitle = new TextView(this);
        subtitle.setText("System • Ready");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(12);

        content.addView(title);
        content.addView(subtitle);
        island.addView(content);

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                widthClosed,
                dp(heightClosed),
                Build.VERSION.SDK_INT >= 26
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = dp(8);

        wm.addView(island,params);

        island.setOnClickListener(v -> toggleIsland());
    }

    void toggleIsland() {
        expanded = !expanded;

        content.setVisibility(expanded ? View.VISIBLE : View.GONE);

        title.setText(expanded ? "Liquid Island" : "");
        subtitle.setText(expanded
            ? "Dynamic system • Running"
            : "");

        WindowManager.LayoutParams p =
            (WindowManager.LayoutParams)island.getLayoutParams();

        int startW = p.width;
        int startH = p.height;
        int endW = expanded ? widthOpen : widthClosed;
        int endH = dp(expanded ? heightOpen : heightClosed);

        ValueAnimator anim = ValueAnimator.ofFloat(0f,1f);
        anim.setDuration(380);
        anim.setInterpolator(new DecelerateInterpolator());

        anim.addUpdateListener(a -> {
            float f = (float)a.getAnimatedValue();
            p.width = (int)(startW + (endW-startW)*f);
            p.height = (int)(startH + (endH-startH)*f);
            wm.updateViewLayout(island,p);
        });

        anim.start();
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int id) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (wm != null && island != null) {
            try { wm.removeView(island); }
            catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
