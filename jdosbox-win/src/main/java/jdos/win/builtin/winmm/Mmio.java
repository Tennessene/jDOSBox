package jdos.win.builtin.winmm;

import jdos.cpu.CPU_Regs;
import jdos.hardware.Memory;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.kernel32.WinPath;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.system.WinFile;
import jdos.win.system.WinSystem;
import jdos.win.utils.FilePath;
import jdos.win.utils.Path;
import jdos.win.utils.StringUtil;

import java.util.Hashtable;

public class Mmio extends WinAPI {
    static final public int MMIOM_READ = MMIO_READ;       /* read */
    static final public int MMIOM_WRITE = MMIO_WRITE;       /* write */
    static final public int MMIOM_SEEK = 2;       /* seek to a new position in file */
    static final public int MMIOM_OPEN = 3;       /* open file */
    static final public int MMIOM_CLOSE = 4;       /* close file */
    static final public int MMIOM_WRITEFLUSH = 5;       /* write and flush */
    static final public int MMIOM_RENAME = 6;       /* rename specified file */

    // MMRESULT mmioAdvance(HMMIO hmmio, LPMMIOINFO lpmmioinfo, UINT wFlags)
    static public int mmioAdvance(int hmmio, int p, int uFlags) {
        WinMMIO wm = WinMMIO.get(hmmio);
        if (wm == null)
            return MMSYSERR_INVALHANDLE;
        if (wm.info.cchBuffer == 0)
            return MMIOERR_UNBUFFERED;
        if (uFlags != MMIO_READ && uFlags != MMIO_WRITE)
            return MMSYSERR_INVALPARAM;

        MMIOINFO lpmmioinfo = null;
        if (p != 0)
            lpmmioinfo = new MMIOINFO(p);

        if (uFlags == MMIO_WRITE && (lpmmioinfo.dwFlags & MMIO_DIRTY) != 0) {
            wm.ioProc.proc(wm.info, MMIOM_SEEK, lpmmioinfo.lBufOffset, SEEK_SET);
            wm.ioProc.proc(wm.info, MMIOM_WRITE, lpmmioinfo.pchBuffer, lpmmioinfo.pchNext - lpmmioinfo.pchBuffer);
            lpmmioinfo.dwFlags &= ~MMIO_DIRTY;
        }
        if (MMIO_Flush(wm, 0) != MMSYSERR_NOERROR)
            return MMIOERR_CANNOTWRITE;

        if (lpmmioinfo != null) {
            wm.dwFileSize = Math.max(wm.dwFileSize, lpmmioinfo.lBufOffset + (lpmmioinfo.pchNext - lpmmioinfo.pchBuffer));
        }
        MMIO_GrabNextBuffer(wm, BOOL(uFlags == MMIO_READ));

        if (lpmmioinfo != null) {
            lpmmioinfo.pchNext = lpmmioinfo.pchBuffer;
            lpmmioinfo.pchEndRead = lpmmioinfo.pchBuffer + (wm.info.pchEndRead - wm.info.pchBuffer);
            lpmmioinfo.pchEndWrite = lpmmioinfo.pchBuffer + (wm.info.pchEndWrite - wm.info.pchBuffer);
            lpmmioinfo.lDiskOffset = wm.info.lDiskOffset;
            lpmmioinfo.lBufOffset = wm.info.lBufOffset;
            lpmmioinfo.write(p);
        }
        return MMSYSERR_NOERROR;
    }

    // MMRESULT mmioAscend(HMMIO hmmio, LPMMCKINFO lpck, UINT wFlags)
    static public int mmioAscend(int hmmio, int p, int wFlags) {
        WinMMIO mmio = WinMMIO.get(hmmio);
        if (mmio == null)
            return MMSYSERR_INVALHANDLE;
        if (p == 0)
            return MMSYSERR_INVALPARAM;
        MMCKINFO lpck = new MMCKINFO(p);
        if ((lpck.dwFlags & MMIO_DIRTY) != 0) {
            int dwOldPos, dwNewSize;

            dwOldPos = mmioSeek(hmmio, 0, SEEK_CUR);
            dwNewSize = dwOldPos - lpck.dwDataOffset;
            if (dwNewSize != lpck.cksize) {
                lpck.cksize = dwNewSize;

                int pInt = getTempBuffer(4);

                /* pad odd size with 0 */
                if ((dwNewSize & 1) != 0) {
                    writeb(p, 0);
                    mmioWrite(hmmio, pInt, 1);
                }
                mmioSeek(hmmio, lpck.dwDataOffset - 4, SEEK_SET);
                writed(p, dwNewSize);
                mmioWrite(hmmio, pInt, 4);
            }
            lpck.dwFlags = 0;
            lpck.write(p);
        }

        mmioSeek(hmmio, lpck.dwDataOffset + ((lpck.cksize + 1) & ~1), SEEK_SET);

        return MMSYSERR_NOERROR;
    }

    // MMRESULT mmioClose(HMMIO hmmio, UINT wFlags)
    static public int mmioClose(int hmmio, int uFlags) {
        WinMMIO wm = WinMMIO.get(hmmio);
        if (wm == null)
            return MMSYSERR_INVALHANDLE;

        int result = MMIO_Flush(wm, 0);
        if (result != MMSYSERR_NOERROR)
            return result;

        result = wm.ioProc.proc(wm.info, MMIOM_CLOSE, uFlags, 0);

        MMIO_SetBuffer(wm, NULL, 0, 0);

        if (wm.bTmpIOProc)
            defaultProcs.remove(wm.info.fccIOProc);
        wm.close();
        return result;
    }

    // MMRESULT mmioDescend(HMMIO hmmio, LPMMCKINFO lpck, LPMMCKINFO lpckParent, UINT wFlags)
    static public int mmioDescend(int hmmio, int p, int lpckParent, int uFlags) {
        int dwOldPos;
        int srchCkId;
        int srchType;

        if (p == NULL)
            return MMSYSERR_INVALPARAM;
        MMCKINFO lpck = new MMCKINFO(p);

        WinMMIO mmio = WinMMIO.get(hmmio);
        if (mmio == null)
            return MMSYSERR_INVALHANDLE;

        dwOldPos = mmioSeek(hmmio, 0, SEEK_CUR);

        if (lpckParent != NULL) {
            MMCKINFO parent = new MMCKINFO(lpckParent);
            /* EPP: was dwOldPos = mmioSeek(hmmio,lpckParent.dwDataOffset,SEEK_SET); */
            if (dwOldPos < parent.dwDataOffset ||
                    dwOldPos >= parent.dwDataOffset + parent.cksize) {
                log("outside parent chunk\n");
                return MMIOERR_CHUNKNOTFOUND;
            }
        }

        /* The SDK docu says 'ckid' is used for all cases. Real World
         * examples disagree -Marcus,990216.
         */

        srchCkId = 0;
        srchType = 0;

        /* find_chunk looks for 'ckid' */
        if ((uFlags & MMIO_FINDCHUNK) != 0)
            srchCkId = lpck.ckid;

        /* find_riff and find_list look for 'fccType' */
        if ((uFlags & MMIO_FINDLIST) != 0) {
            srchCkId = FOURCC_LIST;
            srchType = lpck.fccType;
        }

        if ((uFlags & MMIO_FINDRIFF) != 0) {
            srchCkId = FOURCC_RIFF;
            srchType = lpck.fccType;
        }

        while (true) {
            long ix = mmioRead(hmmio, p, 12);
            if (ix < 8) {
                mmioSeek(hmmio, dwOldPos, SEEK_SET);
                log("return ChunkNotFound\n");
                return MMIOERR_CHUNKNOTFOUND;
            }
            lpck.read(p);
            lpck.dwDataOffset = dwOldPos + 8;
            if ((srchCkId == 0 || (srchCkId == lpck.ckid)) && (srchType == 0 || (srchType == lpck.fccType)))
                break;

            dwOldPos = lpck.dwDataOffset + ((lpck.cksize + 1) & ~1);
            mmioSeek(hmmio, dwOldPos, SEEK_SET);
        }

        lpck.dwFlags = 0;
        /* If we were looking for RIFF/LIST chunks, the final file position
         * is after the chunkid. If we were just looking for the chunk
         * it is after the cksize. So add 4 in RIFF/LIST case.
         */
        if (lpck.ckid == FOURCC_RIFF || lpck.ckid == FOURCC_LIST)
            mmioSeek(hmmio, lpck.dwDataOffset + 4, SEEK_SET);
        else {
            mmioSeek(hmmio, lpck.dwDataOffset, SEEK_SET);
            lpck.fccType = 0;
        }
        lpck.write(p);
        return MMSYSERR_NOERROR;
    }

    // MMRESULT mmioGetInfo(HMMIO hmmio, LPMMIOINFO lpmmioinfo, UINT wFlags)
    static public int mmioGetInfo(int hmmio, int lpmmioinfo, int wFlags) {
        WinMMIO mmio = WinMMIO.get(hmmio);
        if (mmio == null)
            return MMSYSERR_INVALHANDLE;
        if (lpmmioinfo == 0)
            return MMSYSERR_INVALPARAM;
        mmio.info.write(lpmmioinfo);
        return MMSYSERR_NOERROR;
    }

    // HMMIO mmioOpen(LPTSTR szFilename, LPMMIOINFO lpmmioinfo, DWORD dwOpenFlags)
    static public int mmioOpenA(int szFilename, int lpmmioinfo, int dwOpenFlags) {
        MMIOINFO refmminfo = new MMIOINFO();
        int pos;

        if (lpmmioinfo != 0) {
            refmminfo.read(lpmmioinfo);
        }

        if ((dwOpenFlags & (MMIO_PARSE | MMIO_EXIST)) != 0) {
            if (szFilename == 0)
                return FALSE;
            String name = StringUtil.getString(szFilename);
            WinProcess process = WinSystem.getCurrentProcess();
            FilePath file = process.getFile(name);

            if ((dwOpenFlags & MMIO_EXIST) != 0 && !file.exists())
                return FALSE;
            String fullPath = file.getAbsolutePath();
            Path root = process.paths.get(0);
            fullPath = root.winPath + fullPath.substring(root.nativePath.length());
            StringUtil.strcpy(szFilename, fullPath);
            return TRUE;
        }

        WinMMIO wm = WinMMIO.create();

        /* If both params are NULL, then parse the file name if available */
        if (refmminfo.fccIOProc == 0 && refmminfo.pIOProc == NULL) {
            wm.info.fccIOProc = MMIO_ParseExtA(szFilename);
            /* Handle any unhandled/error case. Assume DOS file */
            if (wm.info.fccIOProc == 0) {
                wm.info.fccIOProc = FOURCC_DOS;
                wm.ioProc = mmioDosIOProc;
            } else {
                if ((wm.ioProc = defaultProcs.get(wm.info.fccIOProc)) == null) {
                    /* If not found, assume DOS file */
                    wm.ioProc = mmioDosIOProc;
                }
            }
            wm.bTmpIOProc = false;
        }
        /* if just the four character code is present, look up IO proc */
        else if (refmminfo.pIOProc == NULL) {
            wm.info.fccIOProc = refmminfo.fccIOProc;
            if ((wm.ioProc = defaultProcs.get(wm.info.fccIOProc)) == null) {
                wm.close();
                return 0;
            }
            wm.bTmpIOProc = false;
        }
        /* if IO proc specified, use it and specified four character code */
        else {
            wm.info.fccIOProc = refmminfo.fccIOProc;
            defaultProcs.put(wm.info.fccIOProc, new CustomIOProc(refmminfo.pIOProc));
            wm.ioProc = defaultProcs.get(wm.info.fccIOProc);
            wm.bTmpIOProc = true;
        }

        wm.info.dwFlags = dwOpenFlags;

        if ((dwOpenFlags & MMIO_ALLOCBUF) != 0) {
            refmminfo.wErrorRet = MMIO_SetBuffer(wm, refmminfo.pchBuffer, refmminfo.cchBuffer != 0 ? refmminfo.cchBuffer : MMIO_DEFAULTBUFFER, 0);
            if (refmminfo.wErrorRet != MMSYSERR_NOERROR) {
                wm.close();
                return 0;
            }
        } else {
            refmminfo.wErrorRet = MMIO_SetBuffer(wm, refmminfo.pchBuffer, refmminfo.cchBuffer, 0);
            if (refmminfo.wErrorRet != MMSYSERR_NOERROR) {
                wm.close();
                return 0;
            }
        }

        if (wm.info.fccIOProc == FOURCC_MEM && (wm.info.dwFlags & MMIO_ALLOCBUF) == 0)
            wm.bBufferLoaded = true;

        /* see mmioDosIOProc for that one */
        wm.info.adwInfo[0] = refmminfo.adwInfo[0];

        /* call IO proc to actually open file */
        refmminfo.wErrorRet = wm.ioProc.proc(wm.info, MMIOM_OPEN, szFilename, 0);

        /* grab file size, when possible */
        if (wm.info.fccIOProc != FOURCC_MEM && wm.ioProc.proc(wm.info, MMIOM_SEEK, 0, SEEK_CUR) != -1) {
            pos = wm.info.lDiskOffset;
            wm.ioProc.proc(wm.info, MMIOM_SEEK, 0, SEEK_END);
            wm.dwFileSize = wm.info.lDiskOffset;
            wm.ioProc.proc(wm.info, MMIOM_SEEK, pos, SEEK_SET);
        } else {
            wm.dwFileSize = 0;
        }

        if (refmminfo.wErrorRet == 0) {
            wm.info.hmmio = wm.handle;
            if (lpmmioinfo != 0)
                refmminfo.write(lpmmioinfo);
            return wm.info.hmmio;
        }
        wm.close();
        return 0;
    }

    // LONG mmioRead(HMMIO hmmio, HPSTR pch, LONG cch)
    static public int mmioRead(int hmmio, int pch, int cch) {
        WinMMIO wm = WinMMIO.get(hmmio);
        if (wm == null)
            return -1;

        /* unbuffered case first */
        if (wm.info.pchBuffer == 0)
            return wm.ioProc.proc(wm.info, MMIOM_READ, pch, cch);

        int count;

        /* first try from current buffer */
        if (wm.info.pchNext != wm.info.pchEndRead) {
            count = wm.info.pchEndRead - wm.info.pchNext;
            if (count > cch || count < 0)
                count = cch;
            Memory.mem_memcpy(pch, wm.info.pchNext, count);
            wm.info.pchNext += count;
            pch += count;
            cch -= count;
        } else {
            count = 0;
        }

        if (cch != 0 && (wm.info.fccIOProc != FOURCC_MEM)) {
            assert (wm.info.cchBuffer != 0);

            while (cch != 0) {
                int size;

                size = MMIO_GrabNextBuffer(wm, TRUE);
                if (size <= 0)
                    break;
                if (size > cch)
                    size = cch;
                Memory.mem_memcpy(pch, wm.info.pchBuffer, size);
                wm.info.pchNext += size;
                pch += size;
                cch -= size;
                count += size;
            }
        }
        return count;
    }


    static public int mmioSeek(int hmmio, int lOffset, int iOrigin) {
        WinMMIO wm = WinMMIO.get(hmmio);
        if (wm == null)
            return MMSYSERR_INVALHANDLE;

        /* not buffered, direct seek on file */
        if (wm.info.pchBuffer == 0)
            return wm.ioProc.proc(wm.info, MMIOM_SEEK, lOffset, iOrigin);

        int offset;
        switch (iOrigin) {
            case SEEK_SET:
                offset = lOffset;
                break;
            case SEEK_CUR:
                offset = wm.info.lBufOffset + (wm.info.pchNext - wm.info.pchBuffer) + lOffset;
                break;
            case SEEK_END:
                offset = ((wm.info.fccIOProc == FOURCC_MEM) ? wm.info.cchBuffer : wm.dwFileSize) - lOffset;
                break;
            default:
                return -1;
        }

        if (offset != 0 && offset >= wm.dwFileSize && wm.info.fccIOProc != FOURCC_MEM) {
            /* should check that write mode exists */
            if (MMIO_Flush(wm, 0) != MMSYSERR_NOERROR)
                return -1;
            wm.info.lBufOffset = offset;
            wm.info.pchEndRead = wm.info.pchBuffer;
            wm.info.pchEndWrite = wm.info.pchBuffer + wm.info.cchBuffer;
            if ((wm.info.dwFlags & MMIO_RWMODE) == MMIO_READ) {
                wm.info.lDiskOffset = wm.dwFileSize;
            }
        } else if ((wm.info.cchBuffer > 0) &&
                ((offset < wm.info.lBufOffset) ||
                        (offset >= wm.info.lBufOffset + wm.info.cchBuffer) ||
                        !wm.bBufferLoaded)) {
            /* stay in same buffer ? */
            /* some memory mapped buffers are defined with -1 as a size */

            /* condition to change buffer */
            if ((wm.info.fccIOProc == FOURCC_MEM) ||
                    MMIO_Flush(wm, 0) != MMSYSERR_NOERROR ||
                    /* this also sets the wm.info.lDiskOffset field */
                    wm.ioProc.proc(wm.info, MMIOM_SEEK, (offset / wm.info.cchBuffer) * wm.info.cchBuffer, SEEK_SET) == -1)
                return -1;
            MMIO_GrabNextBuffer(wm, TRUE);
        }

        wm.info.pchNext = wm.info.pchBuffer + (offset - wm.info.lBufOffset);
        return offset;
    }

    // MMRESULT mmioSetInfo( HMMIO hmmio, LPMMIOINFO lpmmioinfo, UINT wFlags)    
    static public int mmioSetInfo(int hmmio, int p, int uFlags) {
        WinMMIO wm = WinMMIO.get(hmmio);
        if (wm == null)
            return MMSYSERR_INVALHANDLE;

        MMIOINFO lpmmioinfo = new MMIOINFO(p);

        /* check pointers coherence */
        if (lpmmioinfo.pchNext < wm.info.pchBuffer || lpmmioinfo.pchNext > wm.info.pchBuffer + wm.info.cchBuffer ||
                lpmmioinfo.pchEndRead < wm.info.pchBuffer || lpmmioinfo.pchEndRead > wm.info.pchBuffer + wm.info.cchBuffer ||
                lpmmioinfo.pchEndWrite < wm.info.pchBuffer || lpmmioinfo.pchEndWrite > wm.info.pchBuffer + wm.info.cchBuffer)
            return MMSYSERR_INVALPARAM;

        wm.info.pchNext = lpmmioinfo.pchNext;
        wm.info.pchEndRead = lpmmioinfo.pchEndRead;
        //Core_normal.start = 1;
        return MMSYSERR_NOERROR;
    }

    static public int mmioWrite(int hmmio, int pch, int cch) {
        WinMMIO wm = WinMMIO.get(hmmio);
        if (wm == null)
            return -1;

        int count;
        if (wm.info.cchBuffer != 0) {
            int bytesW = 0;

            count = 0;
            while (cch != 0) {
                if (wm.info.pchNext != wm.info.pchEndWrite) {
                    count = wm.info.pchEndWrite - wm.info.pchNext;
                    if (count > cch || count < 0)
                        count = cch;
                    Memory.mem_memcpy(wm.info.pchNext, pch, count);
                    wm.info.pchNext += count;
                    pch += count;
                    cch -= count;
                    bytesW += count;
                    wm.info.dwFlags |= MMIO_DIRTY;
                } else {
                    if (wm.info.fccIOProc == FOURCC_MEM) {
                        if (wm.info.adwInfo[0] != 0) {
                            /* from where would we get the memory handle? */
                            warn("memory file expansion not implemented!\n");
                            break;
                        } else {
                            break;
                        }
                    }
                }

                if (wm.info.pchNext == wm.info.pchEndWrite) {
                    MMIO_Flush(wm, MMIO_EMPTYBUF);
                    MMIO_GrabNextBuffer(wm, FALSE);
                } else break;
            }
            count = bytesW;
        } else {
            count = wm.ioProc.proc(wm.info, MMIOM_WRITE, pch, cch);
            wm.info.lBufOffset = wm.info.lDiskOffset;
        }
        return count;
    }

    /**
     * ***********************************************************************
     * MMIO_ParseExtA 		        [internal]
     * <p/>
     * Parses a filename for the extension.
     * <p/>
     * RETURNS
     * The FOURCC code for the extension if found, else 0.
     */
    static int MMIO_ParseExtA(int szFileName) {
        if (szFileName == 0)
            return 0;

        String str = StringUtil.getString(szFileName);
        int posPlus = str.lastIndexOf("+");
        if (posPlus < 0)
            return 0;
        int posDot = str.lastIndexOf(".", posPlus);
        if (posDot < 0) {
            warn("No extension in szFileName");
            return 0;
        }
        String ext = str.substring(posDot, posPlus);
        if (ext.length() > 4)
            warn("Extension length > 4");

        /* FOURCC codes identifying file-extensions must be uppercase */
        return mmioStringToFOURCCA(ext.toUpperCase());
    }

    static private int mmioStringToFOURCCA(String s) {
        while (s.length() < 4) {
            s = s + " ";
        }
        byte[] data = s.getBytes();
        return mmioFOURCC(data[0], data[1], data[2], data[3]);
    }

    static public interface IOProc {
        public int proc(MMIOINFO lpmmioinfo, int uMessage, int lParam1, int lParam2);
    }

    static private Hashtable<Integer, IOProc> defaultProcs = new Hashtable<Integer, IOProc>();

    static private class CustomIOProc implements IOProc {
        int pIOProc;

        public CustomIOProc(int pIOProc) {
            this.pIOProc = pIOProc;
        }

        public int proc(MMIOINFO lpmmioinfo, int uMessage, int lParam1, int lParam2) {
            int p = lpmmioinfo.allocTemp();
            WinSystem.call(p, uMessage, lParam1, lParam2);
            lpmmioinfo.read(p);
            return CPU_Regs.reg_eax.dword;
        }
    }

    static private IOProc mmioDosIOProc = new IOProc() {
        public int proc(MMIOINFO lpmmioinfo, int uMessage, int lParam1, int lParam2) {
            int ret = MMSYSERR_NOERROR;

            switch (uMessage) {
                case MMIOM_OPEN: {
                    /* Parameters:
                    * lParam1 = szFileName parameter from mmioOpen
                    * lParam2 = reserved
                    * Returns: zero on success, error code on error
                    * NOTE: lDiskOffset automatically set to zero
                    */
                    int szFileName = lParam1;

                    if ((lpmmioinfo.dwFlags & MMIO_GETTEMP) != 0) {
                        warn("MMIO_GETTEMP not implemented\n");
                        return MMIOERR_CANNOTOPEN;
                    }

                    /* if filename NULL, assume open file handle in adwInfo[0] */
                    if (szFileName != 0) {
                        String name = StringUtil.getString(szFileName);
                        FilePath file = WinSystem.getCurrentProcess().getFile(name);
                        lpmmioinfo.adwInfo[0] = HFILE_ERROR;
                        if (file.exists()) {
                            try {
                                WinFile winFile = WinFile.create(file, false, 0, 0);
                                if (winFile != null)
                                    lpmmioinfo.adwInfo[0] = winFile.handle;
                            } catch (Exception e) {
                            }
                        }
                    }
                    if (lpmmioinfo.adwInfo[0] == HFILE_ERROR)
                        return MMIOERR_FILENOTFOUND;
                }
                break;

                case MMIOM_CLOSE:
                    /* Parameters:
                    * lParam1 = wFlags parameter from mmioClose
                    * lParam2 = unused
                    * Returns: zero on success, error code on error
                    */
                    if ((lParam1 & MMIO_FHOPEN) == 0) {
                        WinFile file = WinFile.get(lpmmioinfo.adwInfo[0]);
                        if (file != null) {
                            file.close();
                        }
                    }
                    break;

                case MMIOM_READ:
                    /* Parameters:
                    * lParam1 = huge pointer to read buffer
                    * lParam2 = number of bytes to read
                    * Returns: number of bytes read, 0 for EOF, -1 for error (error code
                    *	   in wErrorRet)
                    */
                {
                    WinFile file = WinFile.get(lpmmioinfo.adwInfo[0]);
                    if (file != null) {
                        ret = file.read(lParam1, lParam2);
                        if (ret != -1)
                            lpmmioinfo.lDiskOffset += ret;
                    }
                }
                break;

                case MMIOM_WRITE:
                case MMIOM_WRITEFLUSH:
                    /* no internal buffering, so WRITEFLUSH handled same as WRITE */

                    /* Parameters:
                    * lParam1 = huge pointer to write buffer
                    * lParam2 = number of bytes to write
                    * Returns: number of bytes written, -1 for error (error code in
                    *		wErrorRet)
                    */
                {
                    WinFile file = WinFile.get(lpmmioinfo.adwInfo[0]);
                    if (file != null) {
                        ret = file.write(lParam1, lParam2);
                        if (ret != -1)
                            lpmmioinfo.lDiskOffset += ret;
                    }
                }
                break;

                case MMIOM_SEEK:
                    /* Parameters:
                    * lParam1 = new position
                    * lParam2 = from whence to seek (SEEK_SET, SEEK_CUR, SEEK_END)
                    * Returns: new file postion, -1 on error
                    */
                {
                    WinFile file = WinFile.get(lpmmioinfo.adwInfo[0]);
                    if (file != null) {
                        ret = (int) file.seek(lParam1, lParam2);
                        if (ret != -1)
                            lpmmioinfo.lDiskOffset = ret;
                    }
                }
                return ret;

                case MMIOM_RENAME:
                    /* Parameters:
                    * lParam1 = old name
                    * lParam2 = new name
                    * Returns: zero on success, non-zero on failure
                    */
                    if (WinPath.MoveFileA(lParam1, lParam2) == 0)
                        ret = MMIOERR_FILENOTFOUND;
                    break;

                default:
                    warn("unexpected MMIO message " + uMessage);
                    return 0;
            }

            return ret;
        }
    };

    static int MMIO_SetBuffer(WinMMIO wm, int pchBuffer, int cchBuffer, int uFlags) {
        if (cchBuffer > 0xFFFF)
            warn("Untested handling of huge mmio buffers (" + cchBuffer + " >= 64k)\n");

        if (MMIO_Flush(wm, 0) != MMSYSERR_NOERROR)
            return MMIOERR_CANNOTWRITE;

        /* free previous buffer if allocated */
        if ((wm.info.dwFlags & MMIO_ALLOCBUF) != 0) {
            WinSystem.getCurrentProcess().heap.free(wm.info.pchBuffer);
            wm.info.pchBuffer = NULL;
            wm.info.dwFlags &= ~MMIO_ALLOCBUF;
        }

        if (pchBuffer != 0) {
            wm.info.pchBuffer = pchBuffer;
        } else if (cchBuffer != 0) {
            if ((wm.info.pchBuffer = WinSystem.getCurrentProcess().heap.alloc(cchBuffer, false)) == 0)
                return MMIOERR_OUTOFMEMORY;
            wm.info.dwFlags |= MMIO_ALLOCBUF;
        } else {
            wm.info.pchBuffer = NULL;
        }

        wm.info.cchBuffer = cchBuffer;
        wm.info.pchNext = wm.info.pchBuffer;
        wm.info.pchEndRead = wm.info.pchBuffer;
        wm.info.pchEndWrite = wm.info.pchBuffer + cchBuffer;
        wm.info.lBufOffset = wm.info.lDiskOffset;
        wm.bBufferLoaded = false;
        return MMSYSERR_NOERROR;
    }

    static int MMIO_GrabNextBuffer(WinMMIO wm, int for_read) {
        int size = wm.info.cchBuffer;

        wm.info.lBufOffset = wm.info.lDiskOffset;
        wm.info.pchNext = wm.info.pchBuffer;
        wm.info.pchEndRead = wm.info.pchBuffer;
        wm.info.pchEndWrite = wm.info.pchBuffer + wm.info.cchBuffer;

        wm.bBufferLoaded = true;
        if (for_read != 0) {
            size = wm.ioProc.proc(wm.info, MMIOM_READ, wm.info.pchBuffer, size);
            if (size > 0)
                wm.info.pchEndRead += size;
            else
                wm.bBufferLoaded = false;
        }
        return size;
    }

    static private int MMIO_Flush(WinMMIO wm, int uFlags) {
        if (wm.info.cchBuffer != 0 && (wm.info.fccIOProc != FOURCC_MEM)) {
            /* not quite sure what to do here, but I'll guess */
            if ((wm.info.dwFlags & MMIO_DIRTY) != 0) {
                /* FIXME: error handling */
                wm.ioProc.proc(wm.info, MMIOM_SEEK, wm.info.lBufOffset, SEEK_SET);
                wm.ioProc.proc(wm.info, MMIOM_WRITE, wm.info.pchBuffer, wm.info.pchNext - wm.info.pchBuffer);
            }
            if ((uFlags & MMIO_EMPTYBUF) != 0)
                wm.info.pchNext = wm.info.pchEndRead = wm.info.pchBuffer;
        }
        wm.info.dwFlags &= ~MMIO_DIRTY;
        return MMSYSERR_NOERROR;
    }

    static {
        defaultProcs.put(FOURCC_DOS, mmioDosIOProc);
    }
}
