package jdos.win.builtin.user32;

import java.util.Iterator;

public class DesktopWindow {
    static public void invalidate() {
        WinWindow desktop = WinWindow.get(WinWindow.GetDesktopWindow());
        Iterator<WinWindow> children = desktop.getChildren();
        while (children.hasNext()) {
            WinWindow child = children.next();
            child.invalidate(null);
        }
    }
}
