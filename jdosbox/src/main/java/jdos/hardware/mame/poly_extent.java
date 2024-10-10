package jdos.hardware.mame;

/* poly_extent describes start/end points for a scanline, along with per-scanline parameters */
public final class poly_extent
{
    static public poly_extent[] create(int count) {
        poly_extent[] result = new poly_extent[count];
        for (int i=0;i<result.length;i++)
            result[i] = new poly_extent();
        return result;
    }

    public poly_extent() {
        for (int i=0;i<param.length;i++) {
            param[i] = new Poly.poly_param_extent();
        }
    }
    public int       startx;                     /* starting X coordinate (inclusive) */
    public int       stopx;                      /* ending X coordinate (exclusive) */
    public Poly.poly_param_extent[] param = new Poly.poly_param_extent[Poly.MAX_VERTEX_PARAMS]; /* starting and dx values for each parameter */
}
