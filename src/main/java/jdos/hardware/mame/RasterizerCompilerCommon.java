package jdos.hardware.mame;

import jdos.Dosbox;

import java.io.DataInputStream;
import java.io.InputStream;

public class RasterizerCompilerCommon {
    public static boolean saveClasses = false;

    static public void load() {
        InputStream is = Dosbox.class.getResourceAsStream("Rasterizer.index");
        if (is != null) {
            DataInputStream dis = new DataInputStream(is);
            try {
                int version = dis.readInt();
                int count = dis.readInt();
                for (int i=0;i<count;i++) {
                    raster_info info = new raster_info();
                    String name = dis.readUTF();
                    info.eff_color_path = dis.readInt();
                    info.eff_alpha_mode = dis.readInt();
                    info.eff_fog_mode = dis.readInt();
                    info.eff_fbz_mode = dis.readInt();
                    info.eff_tex_mode_0 = dis.readInt();
                    info.eff_tex_mode_1 = dis.readInt();
                    info.callback = (poly_draw_scanline_func)Class.forName(name).newInstance();
                    VoodooCommon.voodoo.add_rasterizer(info);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
