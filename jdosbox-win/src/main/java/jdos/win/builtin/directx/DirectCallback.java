package jdos.win.builtin.directx;

import jdos.cpu.Callback;

public abstract class DirectCallback implements Callback.Handler {
    public int call() {
        onCall();
        return 0;
    }
    public abstract void onCall();
}
