package jdos.dos;

import javazoom.jl.decoder.*;
import jdos.dos.drives.Drive_local;
import jdos.hardware.Memory;
import jdos.hardware.Mixer;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Vector;

public class CDROM_Interface_Image implements Dos_cdrom.CDROM_Interface {
	private static interface TrackFile {
		public boolean read(/*Bit8u*/byte[] buffer, int offset, long seek, int count);
		public long getLength();
        public void close();
	}

	static private class BinaryFile implements TrackFile {
		public BinaryFile(String filename, BooleanRef error) {
            try {
                file = FileIOFactory.open(filename, FileIOFactory.MODE_READ);
                error.value = false;
            } catch (Exception e) {
                error.value = true;
            }
        }
		public boolean read(/*Bit8u*/byte[] buffer, int offset, long seek, int count) {
            try {
                file.seek(seek);
                file.read(buffer, offset, count);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
		public long getLength() {
            try {
                return file.length();
            } catch (Exception e) {
                return -1;
            }
        }
        public void close() {
            try {file.close();} catch (Exception e){}
        }
		private FileIO file;
	}

    static private class AudioFile implements TrackFile {
        Decoder decoder;
        Bitstream in;
        Header currFrame;
        int frameCount=-1;
        String filename;
		private long lastSeek;
        int framePos = 0;
        short[] frame = new short[0];

        public AudioFile(String filename, BooleanRef error) {
            decoder = new Decoder();
            try {
                in = new Bitstream(new FileInputStream(filename));
                this. filename = filename;
                error.value = false;
            } catch (Exception e) {
                error.value = true;
            }
        }

        private void reopen() {
            try {
                in = new Bitstream(new FileInputStream(filename));
                frameCount = -1;
                currFrame = null;
            } catch (Exception e) {
                // This shouldn't happen
                e.printStackTrace();
            }
        }
        protected short[] decodeFrame() throws JavaLayerException {
            try{
                if(decoder==null)return null;
                if(in==null)return null;

                currFrame = in.readFrame();
                if(currFrame==null)return null;
                SampleBuffer output = (SampleBuffer)decoder.decodeFrame(currFrame, in);
                short[] samps = output.getBuffer();
                in.closeFrame();
                frameCount++;
                return samps;
            } catch (RuntimeException e){
                throw new JavaLayerException("Exception decoding audio frame", e);
            }
        }

        protected boolean seek(int ms) throws JavaLayerException {
            int gotoFrame = (int)(ms / currFrame.ms_per_frame());
            if (gotoFrame<frameCount) {
                reopen();
            }
            while (gotoFrame>frameCount) {
                currFrame = in.readFrame();
                if (currFrame == null)
                    return false;
                in.closeFrame();
                frameCount++;
            }
            return true;
        }
        public boolean read(byte[] buffer, int offset, long seek, int count) {
            try {
                if (currFrame == null) {
                    frame = decodeFrame();
                    framePos = 0;
                }
                if (lastSeek != (seek - count)) {
                    if (!seek((int)((double)(seek) / 176.4f)))
                        return false;
                }
                lastSeek = seek;
                // kind of a bummer to go from short[] to byte[] just so we can go back to short[] for the mixer
                while (count>0) {
                    if (framePos<frame.length) {
                        buffer[offset++] = (byte)(frame[framePos] & 0xFF);
                        buffer[offset++] = (byte)((frame[framePos] >> 8) & 0xFF);
                        framePos++;
                        count-=2;
                    } else {
                        frame = decodeFrame();
                        framePos = 0;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public long getLength() {
            return -1;
        }

        public void close() {
            try {in.close();} catch (Exception e) {}
        }
    }
//    // :TODO: research this class, probably SDL
//    private static class Sound_Sample {
//    }
//	static private class AudioFile implements TrackFile {
//		public AudioFile(String filename, BooleanRef error) {
//            Sound_AudioInfo desired = {AUDIO_S16, 2, 44100};
//            sample = Sound_NewSampleFromFile(filename, &desired, RAW_SECTOR_SIZE);
//            lastCount = RAW_SECTOR_SIZE;
//            lastSeek = 0;
//            error = (sample == NULL);
//        }
//		public boolean read(/*Bit8u*/byte[] buffer, long seek, int count) {
//            if (lastCount != count) {
//                int success = Sound_SetBufferSize(sample, count);
//                if (!success) return false;
//            }
//            if (lastSeek != (seek - count)) {
//                int success = Sound_Seek(sample, (int)((double)(seek) / 176.4f));
//                if (!success) return false;
//            }
//            lastSeek = seek;
//            int bytes = Sound_Decode(sample);
//            if (bytes < count) {
//                memcpy(buffer, sample->buffer, bytes);
//                memset(buffer + bytes, 0, count - bytes);
//            } else {
//                memcpy(buffer, sample->buffer, count);
//            }
//
//            return !(sample->flags & SOUND_SAMPLEFLAG_ERROR);
//        }
//		public long getLength() {
//            int time = 1;
//            int shift = 0;
//            if (!(sample->flags & SOUND_SAMPLEFLAG_CANSEEK)) return -1;
//
//            while (true) {
//                int success = Sound_Seek(sample, (unsigned int)(shift + time));
//                if (!success) {
//                    if (time == 1) return lround((double)shift * 176.4f);
//                    shift += time >> 1;
//                    time = 1;
//                } else {
//                    if (time > ((numeric_limits<int>::max() - shift) / 2)) return -1;
//                    time = time << 1;
//                }
//            }
//        }
//
//		private Sound_Sample sample;
//		private int lastCount;
//		private int lastSeek;
//	}

	static private class Track {
        public Track() {
        }
        public Track(Track t) {
            this.copy(t);
        }
		int number;
		int attr;
		int start;
		int length;
		int skip;
		int sectorSize;
		boolean mode2;
		TrackFile file;
        public void copy(Track t) {
            this.number = t.number;
            this.attr = t.attr;
            this.start = t.start;
            this.length = t.length;
            this.skip = t.skip;
            this.sectorSize = t.sectorSize;
            this.mode2 = t.mode2;
            this.file = t.file;
        }
	}

    static private class imagePlayer {
		CDROM_Interface_Image cd;
		Mixer.MixerChannel channel;
		final Object mutex = new Object();
		/*Bit8u*/byte[] buffer = new byte[8192];
		int     bufLen;
		int     currFrame;
		int     targetFrame;
		boolean    isPlaying;
		boolean    isPaused;
        boolean    ctrlUsed;
		Dos_cdrom.TCtrl ctrlData = new Dos_cdrom.TCtrl();
	}
    private static imagePlayer player = new imagePlayer();
    private static int refCount;
    public static CDROM_Interface_Image[] images = new CDROM_Interface_Image[26];

    private Vector tracks = new Vector();
	private String mcn;
	private /*Bit8u*/short	subUnit;

	public CDROM_Interface_Image(/*Bit8u*/short subUnit) {
        images[subUnit] = this;
        if (refCount == 0) {
            if (player.channel == null) {
                player.channel = Mixer.MIXER_AddChannel(CDAudioCallBack, 44100, "CDAUDIO");
            }
            player.channel.Enable(true);
        }
        refCount++;
    }

	public void close() {
        refCount--;
        if (player.cd == this) player.cd = null;
        ClearTracks();
        if (refCount == 0) {
            player.channel.Enable(false);
        }
    }
	public void InitNewMedia() {

    }
	public boolean SetDevice(String path, int forceCD) {
        if (LoadCueSheet(path)) return true;
        if (LoadIsoFile(path)) return true;

        byte[] b = new String("Could not load image file: " + path + "\n").getBytes();
        IntRef size = new IntRef(b.length);
        Dos_files.DOS_WriteFile(Dos_files.STDOUT, b, size);
        return false;
    }

	public boolean GetUPC(ShortRef attr, StringRef upc) {
        attr.value = 0;
        upc.value = mcn;
        return true;
    }

	public boolean GetAudioTracks(IntRef stTrack, IntRef end, Dos_cdrom.TMSF leadOut) {
        stTrack.value = 1;
        end.value = tracks.size() - 1;

        int value = ((Track)tracks.elementAt(tracks.size() - 1)).start + 150;
        leadOut.fr = value%Dos_cdrom.CD_FPS;
        value /= Dos_cdrom.CD_FPS;
        leadOut.sec = value%60;
        value /= 60;
        leadOut.min = value;
        return true;
    }

	public boolean GetAudioTrackInfo(int track, Dos_cdrom.TMSF start, ShortRef attr) {
        if (track < 1 || track > (int)tracks.size()) return false;

        int value = ((Track)tracks.elementAt(track - 1)).start + 150;
        start.fr = value%Dos_cdrom.CD_FPS;
        value /= Dos_cdrom.CD_FPS;
        start.sec = value%60;
        value /= 60;
        start.min = value;

        attr.value = ((short)((Track)tracks.elementAt(track - 1)).attr);
        return true;
    }

	public boolean GetAudioSub(ShortRef attr, ShortRef track, ShortRef index, Dos_cdrom.TMSF relPos, Dos_cdrom.TMSF absPos) {
        int cur_track = GetTrack(player.currFrame);
        if (cur_track < 1) return false;
        track.value = (short)cur_track;
        attr.value = (short)((Track)tracks.elementAt(track.value - 1)).attr;
        index.value = 1;
        int value = player.currFrame + 150;
        absPos.fr = value%Dos_cdrom.CD_FPS;
        value /= Dos_cdrom.CD_FPS;
        absPos.sec = value%60;
        value /= 60;
        absPos.min = value;

        value = player.currFrame - ((Track)tracks.elementAt(track.value - 1)).start + 150;
        relPos.fr = value%Dos_cdrom.CD_FPS;
        value /= Dos_cdrom.CD_FPS;
        relPos.sec = value%60;
        value /= 60;
        relPos.min = value;

        return true;
    }
	public boolean GetAudioStatus(BooleanRef playing, BooleanRef pause) {
        playing.value = player.isPlaying;
	    pause.value = player.isPaused;
	    return true;
    }
	public boolean GetMediaTrayStatus(BooleanRef mediaPresent, BooleanRef mediaChanged, BooleanRef trayOpen) {
        mediaPresent.value = true;
	    mediaChanged.value = false;
	    trayOpen.value = false;
	    return true;
    }

	public boolean PlayAudioSector(long start,long len) {
        // We might want to do some more checks. E.g valid start and length
        synchronized (player.mutex) {
            player.cd = this;
            player.currFrame = (int)start;
            player.targetFrame = (int)(start + len);
            int track = GetTrack((int)start) - 1;
            if(track >= 0 && ((Track)tracks.elementAt(track)).attr == 0x40) {
                Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_WARN,"Game tries to play the data track. Not doing this");
                player.isPlaying = false;
                //Unclear wether return false should be here.
                //specs say that this function returns at once and games should check the status wether the audio is actually playing
                //Real drives either fail or succeed as well
            } else player.isPlaying = true;
            player.isPaused = false;
        }
        return true;
    }
	public boolean PauseAudio(boolean resume) {
        player.isPaused = !resume;
        return true;
    }
	public boolean StopAudio() {
        player.isPlaying = false;
        player.isPaused = false;
        return true;
    }
    public void ChannelControl(Dos_cdrom.TCtrl ctrl) {
	    player.ctrlUsed = (ctrl.out[0]!=0 || ctrl.out[1]!=1 || ctrl.vol[0]<0xfe || ctrl.vol[1]<0xfe);
	    player.ctrlData.copy(ctrl);
    }

	public boolean ReadSectors(/*PhysPt*/int buffer, boolean raw, long sector, long num) {
        int sectorSize = raw ? Dos_cdrom.RAW_SECTOR_SIZE : Dos_cdrom.COOKED_SECTOR_SIZE;
        /*Bitu*/int buflen = (int)(num * sectorSize);
        /*Bit8u*/byte[] buf = new /*Bit8u*/byte[buflen];

        boolean success = true; //Gobliiins reads 0 sectors
        for(int i = 0; i < num; i++) {
            success = ReadSector(buf, i * sectorSize, raw, (int)sector + i);
            if (!success) break;
        }
        Memory.MEM_BlockWrite(buffer, buf, buflen);
        return success;
    }

	public boolean LoadUnloadMedia(boolean unload) {
        return true;
    }

	public boolean ReadSector(/*Bit8u*/byte[] buffer, int offset, boolean raw, int sector) {
        int track = GetTrack(sector) - 1;
        if (track < 0) return false;

        Track t = (Track)tracks.elementAt(track);
        int seek = t.skip + (sector - t.start) * t.sectorSize;
        int length = (raw ? Dos_cdrom.RAW_SECTOR_SIZE : Dos_cdrom.COOKED_SECTOR_SIZE);
        if (t.sectorSize != Dos_cdrom.RAW_SECTOR_SIZE && raw) return false;
        if (t.sectorSize == Dos_cdrom.RAW_SECTOR_SIZE && !t.mode2 && !raw) seek += 16;
        if (t.mode2 && !raw) seek += 24;

        return t.file.read(buffer, offset, seek, length);
    }
	public boolean HasDataTrack() {
        //Data track has attribute 0x40
        for (int i=0;i<tracks.size();i++) {
            Track it = (Track)tracks.elementAt(i);
            if (it.attr == 0x40) return true;
        }
        return false;
    }

    private static Mixer.MIXER_Handler CDAudioCallBack = new Mixer.MIXER_Handler() {
        public void call(/*Bitu*/int len) {
            len *= 4;       // 16 bit, stereo
            if (len==0) return;
            if (!player.isPlaying || player.isPaused) {
                player.channel.AddSilence();
                return;
            }

            synchronized (player.mutex) {
                while (player.bufLen < len) {
                    boolean success;
                    if (player.targetFrame > player.currFrame)
                        success = player.cd.ReadSector(player.buffer, player.bufLen, true, player.currFrame);
                    else success = false;

                    if (success) {
                        player.currFrame++;
                        player.bufLen += Dos_cdrom.RAW_SECTOR_SIZE;
                    } else {
                        java.util.Arrays.fill(player.buffer, player.bufLen, player.bufLen, (byte)0);
                        player.bufLen = len;
                        player.isPlaying = false;
                    }
                }
            }
            int tIndex = 0;
            int pIndex = 0;
            for (int i=0;i<len/4;i++) {
                if (player.ctrlUsed) {
                    Mixer.MixTemp16[tIndex++] = (short)(((player.buffer[pIndex] & 0xFF) | (player.buffer[pIndex+1] << 8))*player.ctrlData.vol[0]/255);
                    Mixer.MixTemp16[tIndex++] = (short)(((player.buffer[pIndex+2] & 0xFF) | (player.buffer[pIndex+3] << 8))*player.ctrlData.vol[0]/255);
                } else {
                    Mixer.MixTemp16[tIndex++] = (short)((player.buffer[pIndex] & 0xFF) | (player.buffer[pIndex+1] << 8));
                    Mixer.MixTemp16[tIndex++] = (short)((player.buffer[pIndex+2] & 0xFF) | (player.buffer[pIndex+3] << 8));
                }
                pIndex+=4;
            }
            player.channel.AddSamples_s16(len/4,Mixer.MixTemp16);
            for (int i=0;i<player.bufLen - len;i++)
                player.buffer[i] = player.buffer[len+i];
            player.bufLen -= len;
        }
    };

	private int	GetTrack(int sector) {
        for (int i=0;i<tracks.size()-1;i++) {
            Track curr = (Track)tracks.elementAt(i);
            Track next = (Track)tracks.elementAt(i+1);
            if (curr.start <= sector && sector < next.start) return curr.number;
        }
        return -1;
    }



	private void ClearTracks() {
        TrackFile last = null;
        for (int i=0;i<tracks.size();i++) {
            Track curr = (Track)tracks.elementAt(i);
            if (curr.file!=null)
                curr.file.close();
        }
    	tracks.clear();
    }

	private boolean LoadIsoFile(String filename) {
        tracks.clear();

        // data track
        Track track = new Track();
        BooleanRef error = new BooleanRef();
        track.file = new BinaryFile(filename, error);
        if (error.value) {
            track.file.close();
            return false;
        }
        track.number = 1;
        track.attr = 0x40;//data

        // try to detect iso type
        if (CanReadPVD(track.file, Dos_cdrom.COOKED_SECTOR_SIZE, false)) {
            track.sectorSize = Dos_cdrom.COOKED_SECTOR_SIZE;
            track.mode2 = false;
        } else if (CanReadPVD(track.file, Dos_cdrom.RAW_SECTOR_SIZE, false)) {
            track.sectorSize = Dos_cdrom.RAW_SECTOR_SIZE;
            track.mode2 = false;
        } else if (CanReadPVD(track.file, 2336, true)) {
            track.sectorSize = 2336;
            track.mode2 = true;
        } else if (CanReadPVD(track.file, Dos_cdrom.RAW_SECTOR_SIZE, true)) {
            track.sectorSize = Dos_cdrom.RAW_SECTOR_SIZE;
            track.mode2 = true;
        } else return false;

        track.length = (int)track.file.getLength() / track.sectorSize;
        tracks.add(track);
        track = new Track(track);
        
        // leadout track
        track.number = 2;
        track.attr = 0;
        track.start = track.length;
        track.length = 0;
        track.file = null;
        tracks.add(track);

        return true;
    }
	private boolean CanReadPVD(TrackFile file, int sectorSize, boolean mode2) {
        /*Bit8u*/byte[] pvd = new byte[Dos_cdrom.COOKED_SECTOR_SIZE];
        int seek = 16 * sectorSize;	// first vd is located at sector 16
        if (sectorSize == Dos_cdrom.RAW_SECTOR_SIZE && !mode2) seek += 16;
        if (mode2) seek += 24;
        file.read(pvd, 0, seek, Dos_cdrom.COOKED_SECTOR_SIZE);
        // pvd[0] = descriptor type, pvd[1..5] = standard identifier, pvd[6] = iso version
        return (pvd[0] == 1 && StringHelper.strncmp(pvd,1, "CD001".getBytes(), 0, 5)==0 && pvd[6] == 1);
    }
	// cue sheet processing
	private boolean LoadCueSheet(String cuefile) {
        Track track = new Track();
        tracks.clear();
        IntRef shift = new IntRef(0);
        int currPregap = 0;
        IntRef totalPregap = new IntRef(0);
        int prestart = 0;
        boolean success = true;
        boolean canAddTrack = false;
        File f = new File(cuefile);
        try {
            BufferedReader in = new BufferedReader(new FileReader(cuefile));
            String line;

            while((line=in.readLine())!=null) {
                // get next line
                String[] parts = StringHelper.splitWithQuotes(line.trim(), ' ');

                if (parts[0].equals("TRACK")) {
                    if (canAddTrack) success = AddTrack(track, shift, prestart, totalPregap, currPregap);
                    else success = true;

                    track.start = 0;
                    track.skip = 0;
                    currPregap = 0;
                    prestart = 0;

                    track.number = Integer.parseInt(parts[1]);
                    if (parts[2].equals("AUDIO")) {
                        track.sectorSize = Dos_cdrom.RAW_SECTOR_SIZE;
                        track.attr = 0;
                        track.mode2 = false;
                    } else if (parts[2].equals("MODE1/2048")) {
                        track.sectorSize = Dos_cdrom.COOKED_SECTOR_SIZE;
                        track.attr = 0x40;
                        track.mode2 = false;
                    } else if (parts[2].equals("MODE1/2352")) {
                        track.sectorSize = Dos_cdrom.RAW_SECTOR_SIZE;
                        track.attr = 0x40;
                        track.mode2 = false;
                    } else if (parts[2].equals("MODE2/2336")) {
                        track.sectorSize = 2336;
                        track.attr = 0x40;
                        track.mode2 = true;
                    } else if (parts[2].equals("MODE2/2352")) {
                        track.sectorSize = Dos_cdrom.RAW_SECTOR_SIZE;
                        track.attr = 0x40;
                        track.mode2 = true;
                    } else success = false;

                    canAddTrack = true;
                }
                else if (parts[0].equals("INDEX")) {
                    int index = Integer.parseInt(parts[1]);
                    int frame = GetCueFrame(parts[2]);
                    if (frame<0)
                        success = false;
                    if (index == 1) track.start = frame;
                    else if (index == 0) prestart = frame;
                    // ignore other indices
                }
                else if (parts[0].equals("FILE")) {
                    if (canAddTrack) success = AddTrack(track, shift, prestart, totalPregap, currPregap);
                    else success = true;
                    canAddTrack = false;
                    StringRef filename = new StringRef(parts[1]);
                    GetRealFileName(filename, new File(cuefile).getAbsoluteFile().getParent());

                    track.file = null;
                    BooleanRef error = new BooleanRef(true);
                    if (parts[2].equals("BINARY")) {
                        track.file = new BinaryFile(filename.value, error);
                    }
                    //The next if has been surpassed by the else, but leaving it in as not
                    //to break existing cue sheets that depend on this.(mine with OGG tracks specifying MP3 as type)
                     else if (/*parts[2].equals("WAVE") || parts[2].equals("AIFF") ||*/ parts[2].equals("MP3")) {
                        track.file = new AudioFile(filename.value, error);
                    }
                    if (error.value) {
                        if (track.file != null)
                            track.file.close();
                        success = false;
                    }
                }
                else if (parts[0].equals("PREGAP")) {
                    currPregap = GetCueFrame(parts[1]);
                    success = currPregap>=0;
                } else if (parts[0].equals("CATALOG")) mcn = parts[1];
                // ignored commands
                else if (parts[0].equals("CDTEXTFILE") || parts[0].equals("FLAGS") || parts[0].equals("ISRC")
                    || parts[0].equals("PERFORMER") || parts[0].equals("POSTGAP") || parts[0].equals("REM")
                    || parts[0].equals("SONGWRITER") || parts[0].equals("TITLE") || parts[0].equals("")) success = true;
                // failure
                else success = false;

                if (!success)
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        // add last track
        if (!AddTrack(track, shift, prestart, totalPregap, currPregap)) return false;

        // add leadout track
        track.number++;
        track.attr = 0;//sync with load iso
        track.start = 0;
        track.length = 0;
        track.file = null;
        if(!AddTrack(track, shift, 0, totalPregap, 0)) return false;

        return true;
    }
	private boolean	GetRealFileName(StringRef filename, String pathname) {
        if (new File(filename.value).exists()) return true;

        // check if file with path relative to cue file exists
        String tmpstr = pathname + "/" + filename.value;
        if (new File(tmpstr).exists()) {
            filename.value = tmpstr;
            return true;
        }
        // finally check if file is in a dosbox local drive
        StringRef fullname = new StringRef();
        /*Bit8u*/ShortRef drive = new ShortRef(0);
        if (!Dos_files.DOS_MakeName(filename.value, fullname, drive)) return false;

        if (Dos_files.Drives[drive.value]!=null && Dos_files.Drives[drive.value] instanceof Drive_local) {
            Drive_local ldp = (Drive_local)Dos_files.Drives[drive.value];
            ldp.GetSystemFilename(fullname, filename.value);
            if (new File(fullname.value).exists()) {
                filename.value = fullname.value;
                return true;
            }
        }
        return false;
    }

	private int GetCueFrame(String part) {
        String[] parts = StringHelper.split(part, ":");
        if (parts.length == 3) {
            try {
                int min = Integer.parseInt(parts[0]);
                int sec = Integer.parseInt(parts[1]);
                int fr = Integer.parseInt(parts[2]);
                return Dos_cdrom.CD_FPS*60*min+Dos_cdrom.CD_FPS*sec+fr;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }
	boolean	AddTrack(Track c, IntRef shift, int prestart, IntRef totalPregap, int currPregap) {
        Track curr = new Track();
        curr.copy(c);
        // frames between index 0(prestart) and 1(curr.start) must be skipped
        int skip;
        if (prestart > 0) {
            if (prestart > curr.start) return false;
            skip = curr.start - prestart;
        } else skip = 0;

        // first track (track number must be 1)
        if (tracks.size()==0) {
            if (curr.number != 1) return false;
            curr.skip = skip * curr.sectorSize;
            curr.start += currPregap;
            totalPregap.value = currPregap;
            tracks.add(curr);
            return true;
        }

        Track prev = (Track)tracks.lastElement();

        // current track consumes data from the same file as the previous
        if (prev.file == curr.file) {
            curr.start += shift.value;
            prev.length = curr.start + totalPregap.value - prev.start - skip;
            curr.skip += prev.skip + prev.length * prev.sectorSize + skip * curr.sectorSize;
            totalPregap.value += currPregap;
            curr.start += totalPregap.value;
        // current track uses a different file as the previous track
        } else {
            int tmp = (int)(prev.file.getLength() - prev.skip);
            prev.length = tmp / prev.sectorSize;
            if (tmp % prev.sectorSize != 0) prev.length++; // padding

            curr.start += prev.start + prev.length + currPregap;
            curr.skip = skip * curr.sectorSize;
            shift.value += prev.start + prev.length;
            totalPregap.value = currPregap;
        }

        // error checks
        if (curr.number <= 1) return false;
        if (prev.number + 1 != curr.number) return false;
        if (curr.start < prev.start + prev.length) return false;
        if (curr.length < 0) return false;

        tracks.add(curr);
        return true;
    }

    void CDROM_Image_Destroy(Section sec) {
//    #if defined(C_SDL_SOUND)
//        Sound_Quit();
//    #endif
    }

    void CDROM_Image_Init(Section section) {
//    #if defined(C_SDL_SOUND)
//        Sound_Init();
//        section->AddDestroyFunction(CDROM_Image_Destroy, false);
//    #endif
    }
}
