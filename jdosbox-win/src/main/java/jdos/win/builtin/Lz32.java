package jdos.win.builtin;

import jdos.util.IntRef;
import jdos.win.Win;
import jdos.win.builtin.kernel32.KFile;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.system.WinFile;
import jdos.win.system.WinObject;

public class Lz32 extends BuiltinModule {
    static final public int LZERROR_BADINHANDLE =   -1;
    static final public int LZERROR_BADOUTHANDLE =  -2;
    static final public int LZERROR_READ =          -3;
    static final public int LZERROR_WRITE =         -4;
    static final public int LZERROR_GLOBALLOC =     -5;
    static final public int LZERROR_GLOBLOCK =      -6;
    static final public int LZERROR_BADVALUE =      -7;
    static final public int LZERROR_UNKNOWNALG =    -8;
    
    public Lz32(Loader loader, int handle) {
        super(loader, "Lz32.dll", handle);
        add(Lz32.class, "LZClose", new String[] {"hFile"});
        add(Lz32.class, "LZOpenFileA", new String[] {"(STRING)lpFileName", "(HEX)lpReOpenBuf", "(HEX)wStyle"});
        add(Lz32.class, "LZRead", new String[] {"hFile", "lpBuffer", "cbRead"});
        add(Lz32.class, "LZSeek", new String[] {"hFile", "lOffset", "iOrigin"});
    }

    // void APIENTRY LZClose(INT hFile)
    public static void LZClose(int hFile) {
        WinObject obj = WinObject.getObject(hFile);
        if (obj != null)
            obj.close();
    }

    // INT WINAPI LZOpenFile(LPTSTR lpFileName, LPOFSTRUCT lpReOpenBuf, WORD wStyle)
    public static int LZOpenFileA(int lpFileName, int lpReOpenBuf, int wStyle) {
        int hFile = KFile.OpenFile(lpFileName, lpReOpenBuf, wStyle);
        if (hFile == HFILE_ERROR)
            return hFile;
        int result = LZInit(hFile);
        if (result <= 0)
            return hFile;
        return result;
    }

    // INT WINAPI LZRead(INT hFile, LPSTR lpBuffer, INT cbRead)
    public static int LZRead(int hFile, int lpBuffer, int toRead) {
        WinObject obj = WinObject.getObject(hFile);
        if (obj instanceof WinFile)
            return (int)((WinFile) obj).read(lpBuffer, toRead);
        if (!(obj instanceof LZState))
            return LZERROR_BADINHANDLE;
        LZState lzs = (LZState)obj;
        Win.panic("Currently LZRead is broken, use 7Zip to manually decompress the file: "+lzs.realfd.name);
        int	howmuch = toRead;
        IntRef b = new IntRef(0);

        /* if someone has seeked, we have to bring the decompressor
	    * to that position
         */
        if (lzs.realcurrent!=lzs.realwanted) {
            /* if the wanted position is before the current position
             * I see no easy way to unroll ... We have to restart at
             * the beginning. *sigh*
             */
            if (lzs.realcurrent>lzs.realwanted) {
                /* flush decompressor state */
                lzs.realfd.seek(LZ_HEADER_LEN, SEEK_SET);
                GET_FLUSH(lzs);
                lzs.realcurrent= 0;
                lzs.bytetype	= 0;
                lzs.stringlen	= 0;
                java.util.Arrays.fill(lzs.table, (byte) 32);
                lzs.curtabent	= 0xFF0;
            }
            while (lzs.realcurrent<lzs.realwanted) {
                if (!DECOMPRESS_ONE_BYTE(lzs, b)) {
                    return toRead-howmuch;
                }
            }
        }

        while (howmuch!=0) {
            if (!DECOMPRESS_ONE_BYTE(lzs, b))
                return toRead-howmuch;
            lzs.realwanted++;
            writeb(lpBuffer++, b.value);
            howmuch--;
        }
        return toRead;
    }

    // LONG WINAPI LZSeek(INT hFile, LONG lOffset, INT iOrigin)
    public static int LZSeek(int hFile, int lOffset, int iOrigin) {
        WinObject obj = WinObject.getObject(hFile);
        if (obj instanceof WinFile)
            return (int)((WinFile) obj).seek(lOffset, iOrigin);
        if (!(obj instanceof LZState))
            return LZERROR_BADINHANDLE;
        LZState lzs = (LZState)obj;

        int newwanted = lzs.realwanted;
        switch (iOrigin) {
        case 1:	/* SEEK_CUR */
            newwanted += lOffset;
            break;
        case 2:	/* SEEK_END */
            newwanted = lzs.reallength-lOffset;
            break;
        default:/* SEEK_SET */
            newwanted = lOffset;
            break;
        }
        if (newwanted>lzs.reallength)
            return LZERROR_BADVALUE;
        if (newwanted<0)
            return LZERROR_BADVALUE;
        lzs.realwanted = newwanted;
        return newwanted;
    }

    static int GET(LZState lzs, IntRef b) {
        if (lzs.getcur<lzs.getlen) {
            b.value = lzs.get[lzs.getcur++];
            return 1;
        } else {
            int ret = lzs.realfd.read(lzs.get);
            if (ret==HFILE_ERROR)
                return HFILE_ERROR;
            if (ret==0)
                return 0;
            lzs.getlen	= ret;
            lzs.getcur	= 1;
            b.value = lzs.get[0];
            return 1;
        }
    }

    static private void GET_FLUSH(LZState lzs) {
        lzs.getcur=lzs.getlen;
    }

    static private boolean DECOMPRESS_ONE_BYTE (LZState lzs, IntRef b) {
		if (lzs.stringlen!=0) {
			b.value = lzs.table[lzs.stringpos];
			lzs.stringpos = (lzs.stringpos+1)&0xFFF;
			lzs.stringlen--;
		} else {
			if ((lzs.bytetype&0x100) == 0) {
				if (1!=GET(lzs,b))
					return false;
				lzs.bytetype = b.value|0xFF00;
			}
			if ((lzs.bytetype & 1)!=0) {
				if (1!=GET(lzs,b))
					return false;
			} else {
				IntRef b1 = new IntRef(0);
                IntRef b2 = new IntRef(0);

				if (1!=GET(lzs,b1))
					return false;
				if (1!=GET(lzs,b2))
					return false;
				/* Format:
				 * b1 b2
				 * AB CD
				 * where CAB is the stringoffset in the table
				 * and D+3 is the len of the string
				 */
				lzs.stringpos	= (b1.value & 0xFF) |((b2.value & 0xf0)<<4);
				lzs.stringlen	= (b2.value & 0xf)+2;
				/* 3, but we use a  byte already below ... */
				b.value = lzs.table[lzs.stringpos];
				lzs.stringpos	= (lzs.stringpos+1)&0xFFF;
			}
			lzs.bytetype>>=1;
		}
		/* store b in table */
		lzs.table[lzs.curtabent++] = (byte)b.value;
		lzs.curtabent &= 0xFFF;
		lzs.realcurrent++;
        return true;
    }

    static private int LZ_MAGIC_LEN =  8;
    static private int LZ_HEADER_LEN = 14;

    static private class lzfileheader {
        public lzfileheader() {
        }

        public void read(WinFile file) {
            file.read(magic);
            compressiontype = file.readb();
            lastchar = file.readb();
            reallength = file.readd();
        }

	    byte[]	magic = new byte[LZ_MAGIC_LEN];
	    int compressiontype;
	    int lastchar;
	    int reallength;
    }

    static final private int LZ_TABLE_SIZE = 0x1000;
    
    static private class LZState extends WinObject {
        static public LZState create() {
            return new LZState(nextObjectId());
        }

        public LZState(int handle) {
            super(handle);
        }

        WinFile	realfd;		/* the real filedescriptor */
        int	lastchar;	/* the last char of the filename */

        int reallength;	/* the decompressed length of the file */
        int realcurrent;	/* the position the decompressor currently is */
        int realwanted;	/* the position the user wants to read from */

        byte[] table = new byte[LZ_TABLE_SIZE];	/* the rotating LZ table */
        int curtabent;	/* CURrent TABle ENTry */

        int stringlen;	/* length and position of current string */
        int stringpos;	/* from stringtable */


        int bytetype;	/* bitmask within blocks */

        byte[] get = new byte[2048];		/* GETLEN bytes */
        int getcur;		/* current read */
        int getlen;		/* length last got */

        protected void onFree() {
            realfd.close();
        }
    }

    static private byte[] LZMagic = new byte[] {(byte)'S',(byte)'Z',(byte)'D',(byte)'D',(byte)0x88,(byte)0xf0,0x27,0x33};

    /* internal function, reads lzheader
     * returns BADINHANDLE for non filedescriptors
     * return 0 for file not compressed using LZ
     * return UNKNOWNALG for unknown algorithm
     * returns lzfileheader in *head
     */
    static private int read_header(WinFile file, lzfileheader head)
    {
        file.seek(0, SEEK_SET);
        head.read(file);

        if (!java.util.Arrays.equals(head.magic, LZMagic))
            return 0;
        if (head.compressiontype!='A')
            return LZERROR_UNKNOWNALG;
        return 1;
    }

    static private int LZInit(int hFile) {
        WinFile file = WinFile.get(hFile);
        lzfileheader head = new lzfileheader();

        int ret=read_header(file, head);
        if (ret<=0) {
            file.seek(0, SEEK_SET);
            return ret!=0?ret:hFile;
        }

        LZState lzs = LZState.create();

        lzs.realfd = file;
        lzs.lastchar = head.lastchar;
        lzs.reallength = head.reallength;

        lzs.getlen	= 0;
        lzs.getcur	= 0;

        /* Yes, preinitialize with spaces */
        java.util.Arrays.fill(lzs.table, (byte)32);
        /* Yes, start 16 byte from the END of the table */
        lzs.curtabent	= 0xff0;
        return lzs.handle;
    }
}
