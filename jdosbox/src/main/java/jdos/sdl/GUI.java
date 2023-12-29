package jdos.sdl;

public interface GUI {
    void setSize(int cx, int cy);
    void dopaint();
    void showProgress(String msg, int percent);
    void setTitle(String title);
    void showCursor(boolean on);
    void captureMouse(boolean on);
    void fullScreenToggle();
}
