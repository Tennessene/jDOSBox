package jdos.dos;

import jdos.util.BooleanRef;
import jdos.util.IntRef;
import jdos.util.ShortRef;
import jdos.util.StringRef;

import java.io.File;

public class Dos_cdrom {
    static final public int RAW_SECTOR_SIZE = 2352;
    static final public int COOKED_SECTOR_SIZE = 2048;
    static final public int CD_FPS = 75;

    public static int CDROM_GetMountType(String path, int forceCD) {
    // 0 - physical CDROM
    // 1 - Iso file
    // 2 - subdirectory
        // 1. Smells like a real cdrom
        // if ((strlen(path)<=3) && (path[2]=='\\') && (strchr(path,'\\')==strrchr(path,'\\')) && 	(GetDriveType(path)==DRIVE_CDROM)) return 0;

        // :CDROM:
//        const char* cdName;
//        char buffer[512];
//        strcpy(buffer,path);
//    #if defined (WIN32) || defined(OS2)
//        upcase(buffer);
//    #endif
//
//        int num = SDL_CDNumDrives();
//        // If cd drive is forced then check if its in range and return 0
//        if ((forceCD>=0) && (forceCD<num)) {
//            LOG(LOG_ALL,LOG_ERROR)("CDROM: Using drive %d",forceCD);
//            return 0;
//        }
//
//        // compare names
//        for (int i=0; i<num; i++) {
//            cdName = SDL_CDName(i);
//            if (strcmp(buffer,cdName)==0) return 0;
//        };

        // Detect ISO
        if (new File(path).isFile())
            return 1;
        return 2;
    }

    static public class TMSF {
        public int min;
        public int sec;
        public int fr;
        public void clear() {
            min = 0;
            sec = 0;
            fr = 0;
        }
    }

    static public class TCtrl {
        public void copy(TCtrl t) {
            System.arraycopy(t.out, 0, out, 0, out.length);
            System.arraycopy(t.vol, 0, vol, 0, vol.length);
        }
	    /*Bit8u*/final int[] out = new int[4]; // output channel
	    /*Bit8u*/final int[] vol = new int[4]; // channel volume
    }

    public interface CDROM_Interface {
    //	CDROM_Interface						(void);
    void close();
        boolean SetDevice(String path, int forceCD);

        boolean GetUPC(ShortRef attr, StringRef upc);

        boolean GetAudioTracks(IntRef stTrack, IntRef end, TMSF leadOut);
        boolean GetAudioTrackInfo(int track, TMSF start, ShortRef attr);
        boolean GetAudioSub(ShortRef attr, ShortRef track, ShortRef index, TMSF relPos, TMSF absPos);
        boolean GetAudioStatus(BooleanRef playing, BooleanRef pause);
        boolean GetMediaTrayStatus(BooleanRef mediaPresent, BooleanRef mediaChanged, BooleanRef trayOpen);

        boolean PlayAudioSector(long start, long len);
        boolean PauseAudio(boolean resume);
        boolean StopAudio();
        void ChannelControl(TCtrl ctrl);

        boolean ReadSectors(/*PhysPt*/int buffer, boolean raw, long sector, long num);

        boolean LoadUnloadMedia(boolean unload);

        void	InitNewMedia();
    }
}