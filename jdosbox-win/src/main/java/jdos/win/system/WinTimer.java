package jdos.win.system;

import jdos.win.builtin.WinAPI;
import jdos.win.builtin.kernel32.WinThread;
import jdos.win.builtin.user32.WinWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

public class WinTimer {
    int hWnd;

    public WinTimer(int hWnd) {
        this.hWnd = hWnd;
    }

    ArrayList<TimerItem> itemsByTime = new ArrayList<TimerItem>();
    Hashtable<Integer, TimerItem> itemsById = new Hashtable<Integer, TimerItem>();

    static private class TimerItem implements Comparable {
        public TimerItem(int id, int eip, int elapse) {
            this.id = id;
            this.eip = eip;
            this.elapse = elapse;
            this.nextRun = WinSystem.getTickCount()+elapse;
        }
        int id;
        int eip;
        int nextRun;
        int elapse;

        public int compareTo(Object o) {
            return nextRun - ((TimerItem)o).nextRun;
        }
    }

    TimerItem getItem(int id) {
        return itemsById.get(id);
    }

    public int addTimer(int time, int id, int timerProc) {
        if (id == 0) {
            while (true) {
                id+=7;
                if (getItem(id)==null)
                    break;
            }
        }
        TimerItem item = getItem(id);
        if (item != null) {
            killTimer(id);
        }
        item = new TimerItem(id, timerProc, time);
        itemsById.put(id+1, item);
        itemsByTime.add(item);
        Collections.sort(itemsByTime);
        return id;
    }

    public int killTimer(int id) {
        TimerItem item = (TimerItem)itemsById.remove(new Integer(id));
        if (item != null) {
            itemsByTime.remove(item);
            return WinAPI.TRUE;
        }
        return WinAPI.FALSE;
    }

    public int getNextTimerTime() {
        if (itemsByTime.size()>0)
            return itemsByTime.get(0).nextRun;
        return Integer.MAX_VALUE;
    }

    public boolean getNextTimerMsg(int msgAddress, int time, boolean reset) {
        if (itemsByTime.size()>0) {
            TimerItem item = (TimerItem)itemsByTime.get(0);
            if (item.nextRun<time) {
                WinThread.setMessage(msgAddress, hWnd, WinWindow.WM_TIMER, item.id, 0, time, StaticData.currentPos.x, StaticData.currentPos.y);
                if (reset) {
                    item.nextRun = time+item.elapse;
                    Collections.sort(itemsByTime);
                }
                return true;
            }
        }
        return false;
    }

    public void execute(int id) {
        TimerItem item = getItem(id);

        if (item != null && item.eip != 0) {
            // VOID CALLBACK TimerProc(HWND hwnd, UINT uMsg, UINT_PTR idEvent, DWORD dwTime)
            WinSystem.call(item.eip, hWnd, WinWindow.WM_TIMER, id, WinSystem.getTickCount());
        }
    }
}
