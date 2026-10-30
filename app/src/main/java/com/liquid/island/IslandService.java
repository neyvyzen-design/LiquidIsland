package com.liquid.island;
import android.animation.*;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IslandService extends Service {

    WindowManager wm;
    FrameLayout root, panel;
    LinearLayout compact, expanded, info;
    TextView clock, battery, status, music, subtitle;
    Handler handler = new Handler();
    boolean open = false;
    boolean charging = false;
    int batteryLevel = 0;
    int x = 0, y = 8;

    int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    GradientDrawable shape(int radius) {
        GradientDrawable g = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{
                Color.rgb(43,45,53),
                Color.rgb(10,11,16),
                Color.rgb(18,19,25)
            }
        );
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), Color.argb(115,255,255,255));
        return g;
    }

    TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationManager nm =
            (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                "island_pro",
                "Dynamic Island Pro",
                NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(ch);
        }

        Notification.Builder nb =
            Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this,"island_pro")
            : new Notification.Builder(this);

        startForeground(1, nb
            .setContentTitle("Dynamic Island Pro")
            .setContentText("Island sedang aktif")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build());

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);

        registerReceiver(batteryReceiver,
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        createUI();
        updateClock();
    }

    BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (i == null) return;
            batteryLevel = i.getIntExtra("level",0);
            int plugged = i.getIntExtra("plugged",0);
            charging = plugged != 0;
            updateInfo();
        }
    };

    void createUI() {
        root = new FrameLayout(this);
        root.setBackground(shape(32));
        root.setElevation(dp(16));

        GradientDrawable shine = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{
                Color.argb(35,255,255,255),
                Color.argb(0,255,255,255)
            }
        );
        shine.setCornerRadius(dp(32));

        View gloss = new View(this);
        gloss.setBackground(shine);
        root.addView(gloss,new FrameLayout.LayoutParams(
            -1,dp(23),Gravity.TOP));

        compact = new LinearLayout(this);
        compact.setGravity(Gravity.CENTER);
        compact.setOrientation(LinearLayout.HORIZONTAL);

        View dot = new View(this);
        GradientDrawable led = new GradientDrawable();
        led.setShape(GradientDrawable.OVAL);
        led.setColor(Color.rgb(0,220,255));
        dot.setBackground(led);
        dot.setElevation(dp(5));

        compact.addView(dot,new LinearLayout.LayoutParams(dp(7),dp(7)));

        TextView spacer = text("  ",10,Color.WHITE);
        compact.addView(spacer);

        clock = text("--:--",13,Color.WHITE);
        compact.addView(clock);

        View gap = new View(this);
        compact.addView(gap,new LinearLayout.LayoutParams(dp(9),1));

        battery = text("●",11,Color.GREEN);
        compact.addView(battery);

        root.addView(compact,new FrameLayout.LayoutParams(
            -1,dp(43),Gravity.TOP));

        expanded = new LinearLayout(this);
        expanded.setOrientation(LinearLayout.VERTICAL);
        expanded.setPadding(dp(17),dp(49),dp(17),dp(13));
        expanded.setVisibility(View.GONE);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = text("◈  ISLAND PRO",13,Color.WHITE);
        logo.setTypeface(null,1);
        heading.addView(logo,new LinearLayout.LayoutParams(0,dp(30),1));

        status = text("● ACTIVE",10,Color.rgb(80,230,160));
        heading.addView(status);

        expanded.addView(heading);

        View line = new View(this);
        line.setBackgroundColor(Color.argb(55,255,255,255));
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(-1,dp(1));
        lp.topMargin=dp(8);
        lp.bottomMargin=dp(9);
        expanded.addView(line,lp);

        music = text("♫  Dynamic Island",17,Color.WHITE);
        music.setTypeface(null,1);
        expanded.addView(music);

        subtitle = text("Your Android • Premium Interface",11,
            Color.LTGRAY);
        LinearLayout.LayoutParams slp =
            new LinearLayout.LayoutParams(-1,dp(25));
        expanded.addView(subtitle,slp);

        info = new LinearLayout(this);
        info.setGravity(Gravity.CENTER_VERTICAL);

        TextView batteryInfo = text("BATTERY",10,Color.GRAY);
        info.addView(batteryInfo,new LinearLayout.LayoutParams(0,dp(25),1));

        expanded.addView(info);
        root.addView(expanded);

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                dp(137),dp(43),
                Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );

        params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        params.y=dp(y);

        wm.addView(root,params);

        root.setOnClickListener(v -> toggle());

        root.setOnTouchListener(new View.OnTouchListener() {
            float sx,sy;
            int startX,startY;
            boolean moved=false;

            public boolean onTouch(View v, android.view.MotionEvent e) {
                WindowManager.LayoutParams p =
                    (WindowManager.LayoutParams)root.getLayoutParams();

                switch(e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sx=e.getRawX();
                        sy=e.getRawY();
                        startX=p.x;
                        startY=p.y;
                        moved=false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx;
                        float dy=e.getRawY()-sy;
                        if(Math.abs(dx)>dp(5)||Math.abs(dy)>dp(5))
                            moved=true;
                        if(moved) {
                            p.x=startX+(int)dx;
                            p.y=startY+(int)dy;
                            wm.updateViewLayout(root,p);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if(!moved) toggle();
                        return true;
                }
                return true;
            }
        });
    }

    void toggle() {
        open=!open;
        expanded.setVisibility(View.VISIBLE);

        WindowManager.LayoutParams p =
            (WindowManager.LayoutParams)root.getLayoutParams();

        int startW=p.width;
        int startH=p.height;
        int endW=dp(open?315:137);
        int endH=dp(open?177:43);

        ValueAnimator a=ValueAnimator.ofFloat(0,1);
        a.setDuration(420);
        a.setInterpolator(new OvershootInterpolator(0.8f));

        a.addUpdateListener(v -> {
            float f=(float)v.getAnimatedValue();
            p.width=(int)(startW+(endW-startW)*f);
            p.height=(int)(startH+(endH-startH)*f);
            wm.updateViewLayout(root,p);
        });

        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(!open) expanded.setVisibility(View.GONE);
            }
        });
        a.start();

        compact.setAlpha(open?0.2f:1f);
        expanded.animate().alpha(open?1f:0f).setDuration(180).start();
    }

    void updateInfo() {
        if(battery==null) return;
        battery.setText(charging?"⚡":batteryLevel+"%");
        battery.setTextColor(charging
            ? Color.rgb(80,230,150)
            : Color.WHITE);
        if(music!=null)
            music.setText(charging
                ? "⚡  Charging"
                : "♫  Dynamic Island");
        if(subtitle!=null)
            subtitle.setText(charging
                ? "Power connected • "+batteryLevel+"%"
                : "Battery level • "+batteryLevel+"%");
    }

    void updateClock() {
        if(clock!=null) {
            clock.setText(new SimpleDateFormat(
                "HH:mm",Locale.getDefault()).format(new Date()));
        }
        handler.postDelayed(this::updateClock,1000);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(batteryReceiver); }
        catch(Exception ignored) {}
        if(wm!=null&&root!=null) {
            try { wm.removeView(root); }
            catch(Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent i,int f,int id) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
