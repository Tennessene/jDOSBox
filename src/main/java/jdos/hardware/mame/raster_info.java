package jdos.hardware.mame;

public class raster_info
{
    public void copy(raster_info info) {
        this.next = info.next;
        this.callback = info.callback;
        this.is_generic = info.is_generic;
        this.display = info.display;
        this.hits = info.hits;
        this.polys = info.polys;
        this.eff_color_path = info.eff_color_path;
        this.eff_alpha_mode = info.eff_alpha_mode;
        this.eff_fog_mode = info.eff_fog_mode;
        this.eff_fbz_mode = info.eff_fbz_mode;
        this.eff_tex_mode_0 = info.eff_tex_mode_0;
        this.eff_tex_mode_1 = info.eff_tex_mode_1;
    }
    raster_info next;                   /* pointer to next entry with the same hash */
    public poly_draw_scanline_func callback;           /* callback pointer */
    public boolean             is_generic;             /* TRUE if this is one of the generic rasterizers */
    public int                 display;                /* display index */
    public int                 hits;                   /* how many hits (pixels) we've used this for */
    public int                 polys;                  /* how many polys we've used this for */
    public int                 eff_color_path;         /* effective fbzColorPath value */
    public int                 eff_alpha_mode;         /* effective alphaMode value */
    public int                 eff_fog_mode;           /* effective fogMode value */
    public int                 eff_fbz_mode;           /* effective fbzMode value */
    public int                 eff_tex_mode_0;         /* effective textureMode value for TMU #0 */
    public int                 eff_tex_mode_1;         /* effective textureMode value for TMU #1 */
}
