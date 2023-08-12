package jdos.win.builtin.directx.ddraw;

import jdos.hardware.Memory;

public class DDSurfaceDesc {
    static public final int SIZE = 0x6C;
    static public final int SIZE2 = 0x7C;

    static public final int DDSD_CAPS =             0x00000001;
    static public final int	DDSD_HEIGHT =           0x00000002;
    static public final int	DDSD_WIDTH =            0x00000004;
    static public final int	DDSD_PITCH =            0x00000008;
    static public final int	DDSD_BACKBUFFERCOUNT =  0x00000020;
    static public final int	DDSD_ZBUFFERBITDEPTH =  0x00000040;
    static public final int	DDSD_ALPHABITDEPTH =    0x00000080;
    static public final int	DDSD_LPSURFACE =        0x00000800;
    static public final int	DDSD_PIXELFORMAT =      0x00001000;
    static public final int	DDSD_CKDESTOVERLAY =    0x00002000;
    static public final int	DDSD_CKDESTBLT =        0x00004000;
    static public final int	DDSD_CKSRCOVERLAY =     0x00008000;
    static public final int	DDSD_CKSRCBLT =         0x00010000;
    static public final int	DDSD_MIPMAPCOUNT =      0x00020000;
    static public final int	DDSD_REFRESHRATE =      0x00040000;
    static public final int	DDSD_LINEARSIZE =       0x00080000;
    static public final int DDSD_TEXTURESTAGE =     0x00100000;
    static public final int DDSD_FVF =              0x00200000;
    static public final int DDSD_SRCVBHANDLE =      0x00400000;
    static public final int DDSD_DEPTH =            0x00800000;
    static public final int DDSD_ALL =              0x00fff9ee;

    public int dwSize;		        /* 0: size of the DDSURFACEDESC structure*/
	public int dwFlags;	            /* 4: determines what fields are valid*/
	public int dwHeight;	        /* 8: height of surface to be created*/
	public int dwWidth;	            /* C: width of input surface*/
	// union
    public int lPitch;	            /* 10: distance to start of next line (return value only) or dwLinearSize*/
	public int dwBackBufferCount;   /* 14: number of back buffers requested*/
	// union
    public int dwMipMapCount;      /* 18:number of mip-map levels requested or dwZBufferBitDepth or dwRefreshRate*/
	public int dwAlphaBitDepth;     /* 1C:depth of alpha buffer requested*/
	public int dwReserved;	        /* 20:reserved*/
	public int lpSurface;	        /* 24:pointer to the associated surface memory*/
	public DDColorKey ddckCKDestOverlay;    /* 28: CK for dest overlay use*/
	public DDColorKey ddckCKDestBlt;	    /* 30: CK for destination blt use*/
	public DDColorKey ddckCKSrcOverlay;     /* 38: CK for source overlay use*/
	public DDColorKey ddckCKSrcBlt;	        /* 40: CK for source blt use*/
	public DDPixelFormat ddpfPixelFormat;   /* 48: pixel format description of the surface*/
	public int ddsCaps;	                /* 68: direct draw surface caps */

    // DDSurfaceDesc2
        // DDSCAPS2
        public int dwCaps2; /* additional capabilities */
        public int dwCaps3; /* reserved capabilities */
        public int dwCaps4; /* more reserved capabilities */
    public int dwTextureStage;              /* 78: stage in multitexture cascade */

    public DDSurfaceDesc(int address, boolean version2) {
        dwSize = Memory.mem_readd(address);
        dwFlags = Memory.mem_readd(address+0x4);
        dwHeight = Memory.mem_readd(address+0x8);
        dwWidth = Memory.mem_readd(address+0xC);
        lPitch = Memory.mem_readd(address+0x10);
        dwBackBufferCount = Memory.mem_readd(address+0x14);
        dwMipMapCount = Memory.mem_readd(address+0x18);
        dwAlphaBitDepth = Memory.mem_readd(address+0x1C);
        dwReserved = Memory.mem_readd(address+0x20);
        lpSurface = Memory.mem_readd(address+0x24);
        ddckCKDestOverlay = new DDColorKey(address+0x28);
        ddckCKDestBlt = new DDColorKey(address+0x30);
        ddckCKSrcOverlay = new DDColorKey(address+0x38);
        ddckCKSrcBlt = new DDColorKey(address+0x40);
        ddpfPixelFormat = new DDPixelFormat(address+0x48);
        ddsCaps = Memory.mem_readd(address+0x68);
        if (version2) {
            dwCaps2 = Memory.mem_readd(address+0x6C);
            dwCaps3 = Memory.mem_readd(address+0x70);
            dwCaps4 = Memory.mem_readd(address+0x74);
            dwTextureStage = Memory.mem_readd(address+0x78);
        }
    }
}
