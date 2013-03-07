package jdos.gui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.*;
import jdos.sdl.GUI;

public class MyActivity extends Activity {
    static Monitor monitor;
    static public MyActivity activity;

    public class Monitor extends SurfaceView {
        SurfaceHolder holder;
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        public Monitor(Context context) {
            super(context);
            holder = getHolder();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (Main.formattedPixels != null) {
                float scaleWidth = canvas.getWidth()/(float)Main.width;
                float scaleHeight = canvas.getHeight()/(float)Main.height;
                if (scaleWidth>scaleHeight)
                    scaleWidth = scaleHeight;
                else
                    scaleHeight = scaleWidth;
                if (scaleWidth<1)
                    canvas.scale(scaleWidth, scaleHeight);
                canvas.drawBitmap(Main.formattedPixels, 0, Main.pitch, 0, 0, Main.width, Main.height, false, paint);
            }
        }

        public void redraw() {
            try {
                Canvas c = holder.lockCanvas(null);
                try {
                    onDraw(c);
                } finally {
                    holder.unlockCanvasAndPost(c);
                }
            } catch (Exception e) {
                // can throw an exception while rotating
            }
        }
    }

    public static Thread mainThread;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity = this;

        ActivityManager am = ((ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE));
        int memory = am.getMemoryClass();
        int largeMemory = am.getLargeMemoryClass();
        long maxMemory = Runtime.getRuntime().maxMemory();
        System.out.println("Normal heap size: " + memory + "\nLarge heap size: " + largeMemory);

        monitor = new Monitor(this);
        setContentView(monitor);
        if (mainThread == null) {
            final GUI gui = new GUI() {
                public void setSize(int cx, int cy) {}
                public void dopaint() {}
                public void showProgress(String msg, int percent) {}
                public void setTitle(String title) {}
                public void showCursor(boolean on) {}
                public void captureMouse(boolean on) {}
                public void fullScreenToggle() {}
            };
            mainThread = new Thread() {
                public void run() {
                    MainBase.main(gui, new String[]{"-applet", "-c",  "imgmount c jar_tmp://doom19s.img", "-c", "c:", "-c", "cd pcpbench", "-c", "pcpbench"});
                }
            };
            mainThread.start();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        Main.addKeyEvent(event);
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event)  {
        Main.addKeyEvent(event);
        return true;
    }
}
