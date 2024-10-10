package jdos.win.system;

public class WinUser extends WinObject {
    static public WinUser create() {
        return new WinUser(nextObjectId());
    }

    static public WinUser get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinUser))
            return null;
        return (WinUser)object;
    }

    private WinUser(int id) {
        super(id);
    }
}
