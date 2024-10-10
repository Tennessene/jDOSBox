package jdos.hardware.mame;

public final class tmu_state
{
    public tmu_state() {
        for (int i=0;i<ncc.length;i++) {
            ncc[i] = new VoodooCommon.ncc_table();
        }
    }
    public byte[]              ram;                    /* pointer to our RAM */
    public int                 mask;                   /* mask to apply to pointers */
    public int                 reg;                    /* pointer to our register base */
    public boolean             regdirty;               /* true if the LOD/mode/base registers have changed */

    public int                 texaddr_mask;           /* mask for texture address */
    public int                 texaddr_shift;          /* shift for texture address */

    public long                starts, startt;         /* starting S,T (14.18) */
    public long                startw;                 /* starting W (2.30) */
    public long                dsdx, dtdx;             /* delta S,T per X */
    public long                dwdx;                   /* delta W per X */
    public long                dsdy, dtdy;             /* delta S,T per Y */
    public long                dwdy;                   /* delta W per Y */

    public int                 lodmin, lodmax;         /* min, max LOD values */
    public int                 lodbias;                /* LOD bias */
    public int                 lodmask;                /* mask of available LODs */
    public int[]               lodoffset = new int[9]; /* offset of texture base for each LOD */
    public int                 detailmax;              /* detail clamp */
    public int                 detailbias;             /* detail bias */
    public int                 detailscale;            /* detail scale */

    public int                 wmask;                  /* mask for the current texture width */
    public int                 hmask;                  /* mask for the current texture height */

    public int                 bilinear_mask;          /* mask for bilinear resolution (0xf0 for V1, 0xff for V2) */

    public VoodooCommon.ncc_table[]         ncc = new VoodooCommon.ncc_table[2]; /* two NCC tables */

    public int[]               lookup;                 /* currently selected lookup */
    public int[][]             texel = new int[16][];    /* texel lookups for each format */

    public int[]               palette = new int[256]; /* palette lookup table */
    public int[]               palettea = new int[256];/* palette+alpha lookup table */
}
