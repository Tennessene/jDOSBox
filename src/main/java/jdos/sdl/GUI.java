package jdos.sdl;

public interface GUI {
    public void setSize(int cx, int cy);
    public void dopaint();
    public void showProgress(String msg, int percent);
    public void setTitle(String title);
    public void showCursor(boolean on);
    public void captureMouse(boolean on);
    public void fullScreenToggle();
}
