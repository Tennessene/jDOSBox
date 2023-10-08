package jdos.win.system;

import jdos.win.builtin.kernel32.WinThread;
import jdos.win.builtin.user32.WinWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

public class WinTimer {
    final int hWnd;

    public WinTimer(int hWnd) {
        this.hWnd = hWnd;
    }

    final ArrayList<TimerItem> itemsByTime = new ArrayList<>();
    final Hashtable<Integer, TimerItem> itemsById = new Hashtable<>();

    static private class TimerItem implements Comparable {
        public TimerItem(int id, int eip, int elapse) {
            this.id = id;
            this.eip = eip;
            this.elapse = elapse;
            this.nextRun = WinSystem.getTickCount()+elapse;
        }
        final int id;
        final int eip;
        int nextRun;
        final int elapse;

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

    public void killTimer(int id) {
        TimerItem item = itemsById.remove(id);
        if (item != null) {
            itemsByTime.remove(item);
        }
    }

    public int getNextTimerTime() {
        if (!itemsByTime.isEmpty())
            return itemsByTime.get(0).nextRun;
        return Integer.MAX_VALUE;
    }

    public boolean getNextTimerMsg(int msgAddress, int time, boolean reset) {
        if (!itemsByTime.isEmpty()) {
            TimerItem item = itemsByTime.get(0);
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
