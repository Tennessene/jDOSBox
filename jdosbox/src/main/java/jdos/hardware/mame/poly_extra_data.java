package jdos.hardware.mame;

public final class poly_extra_data
{
    public static poly_extra_data[] create(int count) {
        poly_extra_data[] result = new poly_extra_data[count];
        for (int i=0;i<result.length;i++)
            result[i] = new poly_extra_data();
        return result;
    }
    public VoodooCommon        state;                  /* pointer back to the voodoo state */
    public raster_info         info;                   /* pointer to rasterizer information */

    public int                 ax, ay;                 /* vertex A x,y (12.4) */
    public int               startr, startg, startb, starta; /* starting R,G,B,A (12.12) */
    public int               startz;                 /* starting Z (20.12) */
    public long               startw;                 /* starting W (16.32) */
    public int               drdx, dgdx, dbdx, dadx; /* delta R,G,B,A per X */
    public int               dzdx;                   /* delta Z per X */
    public long               dwdx;                   /* delta W per X */
    public int               drdy, dgdy, dbdy, dady; /* delta R,G,B,A per Y */
    public int               dzdy;                   /* delta Z per Y */
    public long               dwdy;                   /* delta W per Y */

    public long               starts0, startt0;       /* starting S,T (14.18) */
    public long               startw0;                /* starting W (2.30) */
    public long               ds0dx, dt0dx;           /* delta S,T per X */
    public long               dw0dx;                  /* delta W per X */
    public long               ds0dy, dt0dy;           /* delta S,T per Y */
    public long               dw0dy;                  /* delta W per Y */
    public int               lodbase0;               /* used during rasterization */

    public long               starts1, startt1;       /* starting S,T (14.18) */
    public long               startw1;                /* starting W (2.30) */
    public long               ds1dx, dt1dx;           /* delta S,T per X */
    public long               dw1dx;                  /* delta W per X */
    public long               ds1dy, dt1dy;           /* delta S,T per Y */
    public long               dw1dy;                  /* delta W per Y */
    public int               lodbase1;               /* used during rasterization */

    public int[]              dither;             /* dither matrix, for fastfill */
}
