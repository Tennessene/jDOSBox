package jdos.win.system;

import jdos.win.Win;
import jdos.win.utils.LittleEndian;
import jdos.win.utils.Pixel;
import jdos.win.utils.StreamHelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

public class WinCursor extends WinObject {
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
            BufferedImage src = Pixel.createImage(image, bitCount, false, palette, data[i].width, data[i].height, true);

            is.read(image);
            palette[0] |= 0xFF000000;
            BufferedImage mask = Pixel.createImage(image, bitCount, true, palette, data[i].width, data[i].height, true);
            BufferedImage result = new BufferedImage(data[i].width, data[i].height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = (Graphics2D)result.getGraphics();
            graphics.drawImage(src, 0, 0, null);
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.DST_IN, 1.0F);
            graphics.setComposite(ac);
            graphics.drawImage(mask, 0, 0, null);
            graphics.dispose();
            images[i] = result;
            hotspots.add(new Point(data[i].xHotspot, data[i].yHotspot));
        }
        return images;
    }

    public WinCursor(int handle, int instance, int name) {
        super(handle);
        if (name<0xFFFF) {
            cursor = loadSystemCursor(name);
        } else {
            int ii=0;
        }
    }

    public static Cursor loadSystemCursor(int id) {
        String res = null;
        switch (id) {
            case 32650: // IDC_APPSTARTING - Standard arrow and small hourglass
                res = "ocr_appstarting.cur";
                break;
            case 32512: // IDC_ARROW - Standard arrow
                res = "ocr_normal.cur";
                break;
            case 32515: // IDC_CROSS - Crosshair
                res = "ocr_cross.cur";
                break;
            case 32649: // IDC_HAND - Hand
                res = "ocr_hand.cur";
                break;
            case 32651: // IDC_HELP - Arrow and question mark
                res = "ocr_help.cur";
                break;
            case 32513: // IDC_IBEAM - I-beam
                res = "ocr_ibeam.cur";
                break;
            case 32648: // IDC_NO - Slashed circle
                res = "ocr_no.cur";
                break;
            case 32646: // IDC_SIZEALL - Four-pointed arrow pointing north, south, east, and west
                res = "ocr_sizeall.cur";
                break;
            case 32643: // IDC_SIZENESW - Double-pointed arrow pointing northeast and southwest
                res = "ocr_sizenesw.cur";
                break;
            case 32645: // IDC_SIZENS - Double-pointed arrow pointing north and south
                res = "ocr_sizens.cur";
                break;
            case 32642: // IDC_SIZENWSE - Double-pointed arrow pointing northwest and southeast
                res = "ocr_sizenwse.cur";
                break;
            case 32644: // IDC_SIZEWE - Double-pointed arrow pointing west and east
                res = "ocr_sizewe.cur";
                break;
            case 32516: // IDC_UPARROW - Vertical arrow
                res = "ocr_up.cur";
                break;
            case 32514: // IDC_WAIT - Hourglass
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
