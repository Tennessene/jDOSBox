package jdos.win.builtin.kernel32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.system.Scheduler;

public class Sync extends WinAPI {
    // DWORD WINAPI WaitForMultipleObjects(DWORD nCount, const HANDLE *lpHandles, BOOL bWaitAll, DWORD dwMilliseconds)
    public static int WaitForMultipleObjects(int nCount, int lpHandles, int bWaitAll, int dwMilliseconds) {
        if (nCount == 1)
            return WaitForSingleObject(readd(lpHandles), dwMilliseconds);
        Win.panic("WaitForMultipleObjects not implemented yet");
//        WaitObject object = WaitObject.getWait(hHandle);
//        if (object == null) {
//            return WAIT_FAILED;
//        } else {
//            return object.wait(Scheduler.getCurrentThread(), dwMilliseconds);
//        }
        return 0;
    }

    // DWORD WINAPI WaitForSingleObject(HANDLE hHandle, DWORD dwMilliseconds)
    public static int WaitForSingleObject(int hHandle, int dwMilliseconds) {
        WaitObject object = WaitObject.getWait(hHandle);
        if (object == null) {
            return WAIT_FAILED;
        } else {
            return object.wait(Scheduler.getCurrentThread(), dwMilliseconds);
        }
    }
}
