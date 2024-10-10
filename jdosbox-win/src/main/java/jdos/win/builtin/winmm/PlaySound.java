package jdos.win.builtin.winmm;

import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.kernel32.KResource;
import jdos.win.system.WinSystem;
import jdos.win.utils.FilePath;
import jdos.win.utils.StringUtil;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;

public class PlaySound extends WinAPI {
    // BOOL PlaySound(LPCTSTR pszSound, HMODULE hmod, DWORD fdwSound)
    static public int PlaySoundA(int pszSound, int hmod, int fdwSound) {
        return MULTIMEDIA_PlaySound(pszSound, hmod, fdwSound, false);
    }

    static private int MULTIMEDIA_PlaySound(int pszSound, int hmod, int fdwSound, boolean bUnicode) {
        ActivePlaySound ps = null;

        Vector playSound = WinSystem.getCurrentProcess().playSound;

        /* SND_NOWAIT is ignored in w95/2k/xp. */
        if ((fdwSound & SND_NOSTOP)!=0 && playSound.size()!=0)
            return FALSE;

        /* alloc internal structure, if we need to play something */
        if (pszSound!=0 && (fdwSound & SND_PURGE)==0) {
            ps = new ActivePlaySound(pszSound, hmod, fdwSound, bUnicode);
        }

        for (int i=0;i<playSound.size();i++) {
            ((ActivePlaySound)playSound.get(i)).stop();
        }

        if (ps==null)
            return TRUE;
        return ps.start();
    }

    static private class ActivePlaySound implements Runnable {
        public ActivePlaySound(int pszSound, int hmod, int flags, boolean unicode) {
            this.pszSound = pszSound;
            this.hmod = hmod;
            this.flags = flags;
            this.unicode = unicode;
        }
        public int pszSound;
        public int hmod;
        public int flags;
        public boolean unicode;
        public boolean loop = false;
        private Thread thread = null;
        byte[] data = null;
        FilePath fileName = null;
        Clip clip = null;
        boolean bExit = false;
        Vector playSound;

        public void stop() {
            bExit = true;
            if (clip != null)
                clip.stop();
            if (thread != null) {
                try {thread.join();} catch (Exception e) {}
            }
        }

        public int start() {
            playSound = WinSystem.getCurrentProcess().playSound;
            if ((flags & SND_ASYNC)!=0) {
                loop = (flags & SND_LOOP) != 0;
                int result = buildStream();
                if (result == 0)
                    return FALSE;
                playSound.add(this);
                thread = new Thread(this);
                thread.start();
                return TRUE;
            } else {
                Win.panic("synchronous play sound not supported yet");
                // :TODO: start play thread
                // :TODO: put current thread to sleep and return so that other threads can run
                // :TODO: when done playing, wake up the calling
                return TRUE;
            }
        }

        public void run() {
            try {
                clip = AudioSystem.getClip();
                InputStream is;
                if (data != null)
                    is = new ByteArrayInputStream(data);
                else
                    is = fileName.getInputStream();
                while (!bExit) {
                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(is);
                    clip.open(inputStream);
                    clip.start();
                    if (!loop)
                        break;
                }
                clip.close();
                clip = null;
            } catch (Exception e) {

            }
            playSound.remove(this);
            thread = null;
        }

        private int buildStream() {
            int pData;

            if ((flags & SND_RESOURCE) == SND_RESOURCE) {
                int	hRes;
                int	hGlob;
                if ((hRes = KResource.FindResourceA(hmod, pszSound, StringUtil.allocateA("WAVE"))) == 0 || (hGlob = KResource.LoadResource(hmod, hRes)) == 0)
                    return FALSE;
                if ((pData = KResource.LockResource(hGlob)) == NULL) {
                    KResource.FreeResource(hGlob);
                    return FALSE;
                }
                KResource.FreeResource(hGlob);
            } else {
                pData = pszSound;
            }
            if ((flags & SND_MEMORY)!=0) { /* NOTE: SND_RESOURCE has the SND_MEMORY bit set */
                int header = readd(pData);
                int size = readd(pData+4)+8;
                int type = readd(pData+8);
                data = new byte[size];
                if (type != 0x45564157) // is this a WAVE file
                    return FALSE;
                Memory.mem_memcpy(data, 0, pData, size);
                return TRUE;
            } else if ((flags & SND_ALIAS)!=0) {
                if ((flags & SND_ALIAS_ID) == SND_ALIAS_ID) {
                    flags &= ~(SND_ALIAS_ID ^ SND_ALIAS);
                    String sound;
                    if (pszSound == SND_ALIAS_SYSTEMASTERISK)
                        sound = "SystemAsterisk";
                    else if (pszSound == SND_ALIAS_SYSTEMDEFAULT)
                        sound = "SystemDefault";
                    else if (pszSound == SND_ALIAS_SYSTEMEXCLAMATION)
                        sound = "SystemExclamation";
                    else if (pszSound == SND_ALIAS_SYSTEMEXIT)
                        sound = "SystemExit";
                    else if (pszSound == SND_ALIAS_SYSTEMHAND)
                        sound = "SystemHand";
                    else if (pszSound == SND_ALIAS_SYSTEMQUESTION)
                        sound = "SystemQuestion";
                    else if (pszSound == SND_ALIAS_SYSTEMSTART)
                        sound = "SystemStart";
                    else if (pszSound == SND_ALIAS_SYSTEMWELCOME)
                        sound = "SystemWelcome";
                    else
                        return FALSE;
                }

                //hmmio = get_mmioFromProfile(wps->fdwSound, wps->pszSound);
            }
            if ((flags & SND_FILENAME)!=0) {
                fileName = WinSystem.getCurrentProcess().getFile(StringUtil.getString(pszSound));
                if (fileName != null && !fileName.exists())
                    fileName = null;
                return BOOL(fileName != null);
            }
            if ((flags & (SND_FILENAME|SND_ALIAS|SND_MEMORY))==0) {
//                if ((hmmio = get_mmioFromProfile(flags | SND_NODEFAULT, pszSound)) == 0) {
//                    if ((hmmio = get_mmioFromFile(StringUtil.getString(pszSound))) == 0) {
//                        hmmio = get_mmioFromProfile(flags, pszSound);
//                    }
//                }
            }
            return FALSE;
        }
    }
}
