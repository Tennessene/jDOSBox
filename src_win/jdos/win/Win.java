package jdos.win;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.Core_normal;
import jdos.cpu.Paging;
import jdos.dos.Dos_files;
import jdos.hardware.Keyboard;
import jdos.hardware.Memory;
import jdos.hardware.Pic;
import jdos.misc.setup.Section;
import jdos.util.StringRef;
import jdos.win.kernel.WinCallback;
import jdos.win.loader.winpe.HeaderPE;
import jdos.win.utils.CpuState;
import jdos.win.utils.Path;
import jdos.win.utils.WinSystem;

import java.util.Vector;

public class Win {
    static private void disable_umb_ems_xms() {
        Section dos_sec = Dosbox.control.GetSection("dos");
        dos_sec.ExecuteDestroy(false);
        dos_sec.HandleInputline("umb=false");
        dos_sec.HandleInputline("xms=false");
        dos_sec.HandleInputline("ems=false");
        dos_sec.ExecuteInit(false);
     }

    static private CpuState cpu = null;
    static private int[] saveDosMemory;

    public static void panic(String msg) {
        Console.out("PANIC: "+msg);
        Win.exit();
    }
    public static void exit() {
        WinCallback.doIdle();
        WinSystem.exit();
        Keyboard.KEYBOARD_Init.call(null);
        cpu.load();
        CPU.cpu.code.big = false;
        Core_normal.log = true;
        System.arraycopy(saveDosMemory, 0, Memory.direct, 0, saveDosMemory.length);
        saveDosMemory = null;
    }

    static public boolean run(String path) {
        if (!HeaderPE.fastCheckWinPE(path))
            return false;
        /*Bit8u*/char drive=(char)(Dos_files.DOS_GetDefaultDrive()+'A');
        StringRef dir = new StringRef();
        Dos_files.DOS_GetCurrentDir((short)0,dir);
        String p = String.valueOf(drive)+":\\";
        if (dir.value.length()>0) {
            p+=dir.value+"\\";
        }
        String winPath = p;
        String name;

        int pos = path.lastIndexOf("\\");
        if (pos<0)
            pos = path.lastIndexOf("/");
        if (pos>=0) {
            name=path.substring(pos+1);
            path = path.substring(0, pos+1);
        } else {
            name=path;
            path = "";
        }

        Vector paths = new Vector();
        paths.add(new Path(path, winPath));

        cpu = new CpuState();
        cpu.save();
        saveDosMemory = new int[1024*256];
        System.arraycopy(Memory.direct, 0, saveDosMemory, 0, saveDosMemory.length);

        // This references old callbacks, like video card timers, etc
        Pic.PIC_Destroy.call(null);
        Pic.PIC_Init.call(null);

        // Remove special handling of the first 1MB
        Paging.PAGING_ShutDown.call(null);
        Paging.LINK_START = 0;
        Paging.PAGING_Init.call(null);

        Memory.clear();

        Keyboard.KEYBOARD_ShutDown.call(null);
        CPU.cpu.code.big = true;

        CPU.CPU_SetSegGeneralCS(0);
        CPU.CPU_SetSegGeneralDS(0);
        CPU.CPU_SetSegGeneralES(0);
        CPU.CPU_SetSegGeneralFS(0);
        CPU.CPU_SetSegGeneralGS(0);
        CPU.CPU_SetSegGeneralSS(0);

        CPU.CPU_SET_CRX(0, CPU.cpu.cr0 |= CPU.CR0_PROTECTION);
        CPU.cpu.pmode = true;
        CPU.Segs_CSval = 0x08;

        WinSystem.start();

        if (WinSystem.createProcess(name, null, paths) > 0) {
            return true;
        }
        return true;
    }


}
