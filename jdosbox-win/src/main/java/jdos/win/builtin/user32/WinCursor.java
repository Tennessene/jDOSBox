package jdos.win.builtin.user32;

import jdos.gui.Main;
import jdos.util.IntRef;
import jdos.win.Win;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Module;
import jdos.win.loader.NativeModule;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.system.StaticData;
import jdos.win.system.WinObject;
import jdos.win.system.WinSystem;
import jdos.win.utils.LittleEndian;
import jdos.win.utils.Pixel;
import jdos.win.utils.StreamHelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

public class WinCursor extends WinObject {
    static public WinCursor create(int instance, int name) {
        WinCursor cursor = new WinCursor(nextObjectId(), instance, name);
        return cursor;
    }

    static public WinCursor get(int handle) {
        WinObject object = getObject(handle);
        if (object == null && (handle>=32512 && handle<=32651)) {
            object = new WinCursor(handle, 0, handle);
        }
        if (object == null || !(object instanceof WinCursor))
            return null;
        return (WinCursor)object;
    }

    // HCURSOR WINAPI LoadCursor(HINSTANCE hInstance, LPCTSTR lpCursorName)
    static public int LoadCursorA(int hInstance, int lpCursorName) {
        return create(hInstance, lpCursorName).handle;
    }

    // HCURSOR WINAPI SetCursor(HCURSOR hCursor)
    static public int SetCursor(int hCursor) {
        int result = StaticData.hCursor;
        StaticData.hCursor = hCursor;
        if (hCursor == 0)
            Main.GFX_SetCursor(null);
        else {
            if (StaticData.showCursorCount>=0)
                Main.GFX_SetCursor(WinCursor.get(hCursor).cursor);
        }
        return hCursor;
    }

    // int WINAPI ShowCursor(BOOL bShow)
    static public int ShowCursor(int bShow) {
        if (bShow != 0) {
            StaticData.showCursorCount++;
            if (StaticData.showCursorCount == 0) {
                SetCursor(StaticData.hCursor);
            }
        } else {
            StaticData.showCursorCount--;
            if (StaticData.showCursorCount == -1) {
                Main.GFX_SetCursor(null);
            }
        }
        return StaticData.showCursorCount;
    }

    static private Hashtable cursors = new Hashtable();

    Cursor cursor = null;

    static private class Data {
        public int width;
        public int height;
        public int colorCount;
        public int xHotspot;
        public int yHotspot;
        public int size;
        public int fileOffset;
    }

    static private BufferedImage loadCursor(LittleEndian is) {
        int headerSize = is.readInt();
        int bitmapWidth = is.readInt();
        int bitmapHeight = is.readInt();
        int planes = is.readUnsignedShort();
        int bitCount = is.readUnsignedShort();
        int compression = is.readInt();
        int imageSize = is.readInt();
        is.readInt(); // XpixelsPerM
        is.readInt(); // YpixelsPerM
        int colorsUsed = is.readInt();
        int colorsImportant = is.readInt();
        int[] palette = new int[2];
        for (int j=0;j<2;j++) {
            palette[j] = is.readInt();
        }
        byte[] image = new byte[imageSize/2];
        is.read(image);
        int height = bitmapHeight / 2;
        BufferedImage src = Pixel.createImage(image, bitCount, false, palette, bitmapWidth, height, true);

        is.read(image);
        palette[0] |= 0xFF000000;
        BufferedImage mask = Pixel.createImage(image, bitCount, true, palette, bitmapWidth, height, true);
        BufferedImage result = new BufferedImage(bitmapWidth, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D)result.getGraphics();
        graphics.drawImage(src, 0, 0, null);
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.DST_IN, 1.0F);
        graphics.setComposite(ac);
        graphics.drawImage(mask, 0, 0, null);
        graphics.dispose();
        return result;
    }

    static public Image[] loadCursorFromStream(InputStream input, Vector hotspots) {
        byte[] buffer = null;
        try {
            buffer = StreamHelper.readStream(input);
        } catch (Exception e) {
            return null;
        }
        LittleEndian is = new LittleEndian(buffer);
        int reserved = is.readUnsignedShort();
        int type = is.readUnsignedShort();
        if (reserved != 0 || type != 2) {
            Win.panic("Stream was not a cursor");
            return null;
        }
        int count = is.readUnsignedShort();
        Data[] data = new Data[count];

        for (int i=0;i<count;i++) {
            data[i] = new Data();
            data[i].width = is.readUnsignedByte();
            data[i].height = is.readUnsignedByte();
            data[i].colorCount = is.readUnsignedByte();
            is.readUnsignedByte();
            data[i].xHotspot = is.readUnsignedShort();
            data[i].yHotspot = is.readUnsignedShort();
            data[i].size = is.readInt();
            data[i].fileOffset = is.readInt();
        }
        Image[] images = new Image[count];

        for (int i=0;i<data.length;i++) {
            is.seek(data[i].fileOffset);

            images[i] = loadCursor(is);
            hotspots.add(new Point(data[i].xHotspot, data[i].yHotspot));
        }
        return images;
    }

    public WinCursor(int handle, int instance, int name) {
        super(handle);
        if (name<0xFFFF) {
            if (instance == 0)
                cursor = loadSystemCursor(name);
            else
                cursor = loadCursorFromResource(instance, name);
        } else {
            Win.panic("Loading a non system cursor has not been implemented yet");
        }
    }

    public Cursor getCursor() {
        return cursor;
    }

    public static Cursor loadCursorFromResource(int instance, int id) {
        String name = "CURSOR"+instance+"-"+id;
        Cursor cursor = (Cursor)cursors.get(name);
        if (cursor == null) {
            Module m = WinSystem.getCurrentProcess().getModuleByHandle(instance);
            if (m instanceof BuiltinModule) {
                return null;
            }
            NativeModule module = (NativeModule)m;
            IntRef size = new IntRef(0);
            int address = module.getAddressOfResource(NativeModule.RT_GROUP_CURSOR, id, size);
            if (address == 0) {
                Win.panic("Cursor not found in resource: "+module.name+" id: "+id);
                return null;
            }
            LittleEndianFile is = new LittleEndianFile(address);
            int reserved = is.readShort();
            int type = is.readShort();
            if (type != 2) {
                Win.panic("Wasn't expecting type: "+type+" in cursor resource");
            }
            int count = is.readShort();
            int bytesInRes = 0;
            for (int i=0;i<count;i++) {
                int width = is.readShort();
                int heigh = is.readShort();
                int planes = is.readShort();
                int bitCount = is.readShort();
                bytesInRes = is.readInt();
                int ordinal = is.readShort();
                address = module.getAddressOfResource(NativeModule.RT_CURSOR, ordinal, size);
                if (address == 0) {
                    Win.panic("Cursor not found in resource: "+module.name+" id: "+id);
                    return null;
                }
            }
            is = new LittleEndianFile(address);
            int xHotspot = is.readShort();
            int yHotspot = is.readShort();

            byte[] data = new byte[bytesInRes-4];
            is.read(data);

            Toolkit toolkit = Toolkit.getDefaultToolkit();
            cursor = toolkit.createCustomCursor(loadCursor(new LittleEndian(data)), new Point(xHotspot, yHotspot), name);
            cursors.put(name, cursor);
        }
        return cursor;
    }

    public static Cursor loadSystemCursor(int id) {
        String res = null;
        switch (id) {
            case IDC_APPSTARTING: // Standard arrow and small hourglass
                res = "ocr_appstarting.cur";
                break;
            case IDC_ARROW: // Standard arrow
                res = "ocr_normal.cur";
                break;
            case IDC_CROSS: // Crosshair
                res = "ocr_cross.cur";
                break;
            case IDC_HAND: // Hand
                res = "ocr_hand.cur";
                break;
            case IDC_HELP: // Arrow and question mark
                res = "ocr_help.cur";
                break;
            case IDC_IBEAM: // I-beam
                res = "ocr_ibeam.cur";
                break;
            case IDC_NO: // Slashed circle
                res = "ocr_no.cur";
                break;
            case IDC_SIZEALL: // Four-pointed arrow pointing north, south, east, and west
                res = "ocr_sizeall.cur";
                break;
            case IDC_SIZENESW: // - Double-pointed arrow pointing northeast and southwest
                res = "ocr_sizenesw.cur";
                break;
            case IDC_SIZENS: // Double-pointed arrow pointing north and south
                res = "ocr_sizens.cur";
                break;
            case IDC_SIZENWSE: // Double-pointed arrow pointing northwest and southeast
                res = "ocr_sizenwse.cur";
                break;
            case IDC_SIZEWE: // Double-pointed arrow pointing west and east
                res = "ocr_sizewe.cur";
                break;
            case IDC_UPARROW: // Vertical arrow
                res = "ocr_up.cur";
                break;
            case IDC_WAIT: // Hourglass
                res = "ocr_wait.cur";
                break;
            default:
                Win.panic("Unknown cursor resource id: "+id);
        }
        if (res != null) {
            Cursor cursor = (Cursor)cursors.get(res);
            if (cursor == null) {
                InputStream is = WinCursor.class.getResourceAsStream("/jdos/win/builtin/res/" + res);
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Vector hotspots = new Vector();
                Image[] images = loadCursorFromStream(is, hotspots);
                cursor = toolkit.createCustomCursor(images[0], (Point)hotspots.elementAt(0), res);
                cursors.put(res, cursor);
            }
            return cursor;
        }
        return null;
    }
}
