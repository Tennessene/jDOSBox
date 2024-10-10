package jdos.win.system;

import jdos.win.utils.FilePath;

import javax.sound.midi.*;

public class WinMidi extends WinMCI {
    static public WinMidi create() {
        return new WinMidi(nextObjectId());
    }

    private FilePath file;
    private Sequence sequence;
    private Sequencer sequencer;

    public WinMidi(int id) {
        super(id);
    }

    public void play(int from, int to, int hWndCallback, boolean wait) {
        hWnd = hWndCallback;
        sequencer.start();
    }

    public void stop(int hWndCallback, boolean wait) {
        if (sequencer != null)
            sequencer.stop();
        hWnd = hWndCallback;
        if (hWnd != 0)
            sendNotification(MCI_NOTIFY_SUCCESSFUL);
    }

    public void close(int hWndCallback, boolean wait) {
        if (sequencer != null)
            sequencer.close();
        hWnd = hWndCallback;
        if (hWnd != 0)
            sendNotification(MCI_NOTIFY_SUCCESSFUL);
    }

    public boolean setFile(FilePath file) {
        this.file = file;
        try {
            sequence = MidiSystem.getSequence(file.getInputStream());
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.setSequence(sequence);
            sequencer.addMetaEventListener(new MetaEventListener() {
                public void meta(MetaMessage meta) {
                    if ( meta.getType() == 47 ) {
                        if (hWnd != 0)
                            sendNotification(MCI_NOTIFY_SUCCESSFUL);
                    }
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
