package jdos.win.builtin.user32;

import jdos.hardware.Memory;
import jdos.win.Console;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.WinBitmap;
import jdos.win.loader.Module;
import jdos.win.loader.NativeModule;
import jdos.win.system.WinSystem;
import jdos.win.utils.Ptr;
import jdos.win.utils.StreamHelper;
import jdos.win.utils.StringUtil;

import java.io.InputStream;

public class Resource extends WinAPI {
    // HACCEL WINAPI LoadAccelerators(HINSTANCE hInstance, LPCTSTR lpTableName)
    public static int LoadAcceleratorsA(int hInstance, int lpTableName) {
        faked();
        return 0xAAAA;
    }

    // HANDLE WINAPI LoadImage(HINSTANCE hinst, LPCTSTR lpszName, UINT uType, int cxDesired, int cyDesired, UINT fuLoad)
    static public int LoadImageA(int hinst, int lpszName, int uType, int cxDesired, int cyDesired, int fuLoad) {
        if (fuLoad != 0 && fuLoad != 0x2000) {
            Win.panic("LoadImage fuLoad flags are not currently supported: fuLoad = 0x" + Ptr.toString(fuLoad));
        }
        if (uType == 0) { // IMAGE_BITMAP
            Module m = WinSystem.getCurrentProcess().loader.getModuleByHandle(hinst);
            if (m instanceof NativeModule) {
                NativeModule module = (NativeModule)m;
                int bitmapAddress = module.getAddressOfResource(NativeModule.RT_BITMAP, lpszName);
                if (bitmapAddress != 0) {
                    return WinBitmap.create(bitmapAddress, false).getHandle();
                } else {
                    // :TODO: what should the error be
                    return 0;
                }
            } else {
                String res = null;
                switch (lpszName) {
                    case OBM_CHECKBOXES:
                        res = "obm_checkboxes.bmp";
                        break;
                }
                if (res == null)
                    Win.panic("LoadImage currently does not support builtin image: "+lpszName);
                InputStream is = WinCursor.class.getResourceAsStream("/jdos/win/builtin/res/" + res);
                try {
                    byte[] data = StreamHelper.readStream(is);
                    // 14 is the file header length
                    int address = WinSystem.getCurrentProcess().heap.alloc(data.length-14, false);
                    Memory.mem_memcpy(address, data, 14, data.length-14);
                    return WinBitmap.create(address, true).handle;
                } catch (Exception e) {
                    e.printStackTrace();
                    Win.panic("LoadImage could not find "+res);
                }
            }
        } else {
            Console.out("LoadImage type=" + uType + " faked");
        }
        return 0;
    }

    // int WINAPI LoadString(HINSTANCE hInstance, UINT uID, LPTSTR lpBuffer, int nBufferMax)
    static public int LoadStringA(int hInstance, int uID, int lpBuffer, int nBufferMax) {
        Module m = WinSystem.getCurrentProcess().loader.getModuleByHandle(hInstance);
        if (m instanceof NativeModule) {
            NativeModule module = (NativeModule)m;
            int stringAddress = module.getAddressOfResource(NativeModule.RT_STRING, (uID >> 4)+1);
            if (stringAddress != 0) {
                int index = uID & 0xf;
                for (int i = 0; i < index; i++)
                    stringAddress += readw(stringAddress)*2 + 2;
                int len = readw(stringAddress);
                stringAddress+=2;
                String result = StringUtil.getStringW(stringAddress, len);
                StringUtil.strncpy(lpBuffer, result, nBufferMax);
                return Math.min(result.length(), nBufferMax);
            }
        }
        if (lpBuffer != 0 && nBufferMax>0)
            writeb(lpBuffer, 0);
        return 0;
    }
}
