package jdos.gui;

import android.view.KeyEvent;
import jdos.util.IntRef;

public class Main extends MainBase {
    static public void GFX_Events() {
        while (events.size()>0) {
            Object event = events.elementAt(0);
            events.removeElementAt(0);
            if (event == null)
                throw new ShutdownException();
            if (event instanceof KeyEvent)
                handle((KeyEvent)event);
        }
    }

    static public void handle(KeyEvent key) {
        KeyboardKey.CheckEvent(key);
    }

    static public void addKeyEvent(KeyEvent key) {
        if (paused) {
            if (key.getKeyCode() == KeyEvent.KEYCODE_BREAK && key.getAction()==KeyEvent.ACTION_DOWN) {
                synchronized (pauseMutex) {
                    try {pauseMutex.notify();} catch (Exception e){}
                }
            }
        } else {
            // what about allowing the key release for the keys that where down when we paused
            events.add(key);
        }
    }

    static public void GFX_EndUpdate() {
        if (bpp != 32) {
            if (bpp == 8) {
                int j=0;
                for (int i=0;i<pixels.length;i++) {
                    formattedPixels[j++] = cmap[pixels[i] & 0xFF];
                    formattedPixels[j++] = cmap[(pixels[i] >> 8) & 0xFF];
                    formattedPixels[j++] = cmap[(pixels[i] >> 16) & 0xFF];
                    formattedPixels[j++] = cmap[(pixels[i] >>> 24) & 0xFF];
                }
            }
        }
        MyActivity.monitor.redraw();
    }

    static public boolean GFX_StartUpdate(int[][] pixels,/*Bitu*/IntRef pitch) {
        pixels[0] = Main.pixels;
        pitch.value = Main.pitch;
        return true;
    }

    static public int[] formattedPixels;
    static private int[] pixels;
    static public int pitch;
    static public int height;
    static public int width;
    static public int bpp;
    static public boolean dblh;
    static public boolean dblw;

    static public /*Bitu*/void GFX_SetSize(/*Bitu*/int width,/*Bitu*/int height,boolean aspect, boolean dblh, boolean dblw, int bpp) {
        Main.height = height;
        Main.width = width;
        Main.dblh = dblh;
        Main.dblw = dblw;
        Main.bpp = bpp;

        if (dblh)
            height/=2;
        if (dblw)
            width/=2;
        switch (bpp) {
            case 8:
                pixels = new int[width*height/4];
                break;
            case 15:
            case 16:
                pixels = new int[width*height/2];
                break;
            case 32:
                pixels = new int[width*height];
                break;
        }
        if (bpp == 32)
            formattedPixels = pixels;
        else
            formattedPixels = new int[width*height];
        Main.pitch = width;
    }

    static int[] cmap = new int[256];

    static {
        int i=0;
        for (int r=0; r < 256; r += 51) {
            for (int g=0; g < 256; g += 51) {
                for (int b=0; b < 256; b += 51) {
                    cmap[i++] = (r<<16)|(g<<8)|b;
                }
            }
        }
        // And populate the rest of the cmap with gray values
        int grayIncr = 256/(256-i);

        // The gray ramp will be between 18 and 252
        int gray = grayIncr*3;
        for (; i < 256; i++) {
            cmap[i] = (gray<<16)|(gray<<8)|gray;
            gray += grayIncr;
        }
    }

    static public void GFX_SetPalette(/*Bitu*/int start,/*Bitu*/int count, Render.RenderPal_t.RGB[] entries, int entriesOffset, int bpp) {
        for (int i=start;i<start+count;i++) {
            cmap[i] = GFX_GetRGB(entries[i].red, entries[i].green, entries[i].blue);
        }
    }
}
