package jdos.hardware.mame;

public final class stats_block
{
    static public stats_block[] create(int count) {
        stats_block[] result = new stats_block[count];
        for (int i=0;i<result.length;i++)
            result[i] = new stats_block();
        return result;
    }
    public int               pixels_in;              /* pixels in statistic */
    public int               pixels_out;             /* pixels out statistic */
    public int               chroma_fail;            /* chroma test fail statistic */
    public int               zfunc_fail;             /* z function test fail statistic */
    public int               afunc_fail;             /* alpha function test fail statistic */
    public int               clip_fail;              /* clipping fail statistic */
    public int               stipple_count;          /* stipple statistic */
    //int               filler[64/4 - 7];       /* pad this structure to 64 bytes */

    public void clear() {
        pixels_in = 0;
        pixels_out = 0;
        chroma_fail = 0;
        zfunc_fail = 0;
        afunc_fail = 0;
        clip_fail = 0;
        stipple_count = 0;
    }
}
