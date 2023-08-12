package jdos.dos;

import jdos.util.BooleanRef;
import jdos.util.IntRef;
import jdos.util.ShortRef;
import jdos.util.StringRef;

public class CDROM_Interface_Fake implements Dos_cdrom.CDROM_Interface {
    public void close() {
    }

    public boolean SetDevice(String path, int forceCD) {
        return true;
    }

    public boolean GetUPC(ShortRef attr, StringRef upc) {
        attr.value = 0; upc.value="UPC"; return true;
    }

    public boolean GetAudioTracks(IntRef stTrack, IntRef end, Dos_cdrom.TMSF leadOut) {
        stTrack.value = end.value = 1;
        leadOut.min	= 60;
        leadOut.sec = leadOut.fr = 0;
        return true;
    }

    public boolean GetAudioTrackInfo(int track, Dos_cdrom.TMSF start, ShortRef attr) {
        if (track>1) return false;
        start.min = start.fr = 0;
        start.sec = 2;
        attr.value = 0x60; // data / permitted
        return true;
    }

    public boolean GetAudioSub(ShortRef attr, ShortRef track, ShortRef index, Dos_cdrom.TMSF relPos, Dos_cdrom.TMSF absPos) {
        attr.value = 0;
        track.value = index.value = 1;
        relPos.min = relPos.fr = 0; relPos.sec = 2;
        absPos.min = absPos.fr = 0; absPos.sec = 2;
        return true;
    }

    public boolean GetAudioStatus(BooleanRef playing, BooleanRef pause) {
        playing.value = pause.value = false;
	    return true;
    }

    public boolean GetMediaTrayStatus(BooleanRef mediaPresent, BooleanRef mediaChanged, BooleanRef trayOpen) {
        mediaPresent.value = true;
        mediaChanged.value = false;
        trayOpen.value     = false;
        return true;
    }

    public boolean PlayAudioSector(long start, long len) {
        return true;
    }

    public boolean PauseAudio(boolean resume) {
        return true;
    }

    public boolean StopAudio() {
        return true;
    }

    public void ChannelControl(Dos_cdrom.TCtrl ctrl) {
    }

    public boolean ReadSectors(int buffer, boolean raw, long sector, long num) {
        return true;
    }

    public boolean LoadUnloadMedia(boolean unload) {
        return true;
    }

    public void InitNewMedia() {
    }
}
