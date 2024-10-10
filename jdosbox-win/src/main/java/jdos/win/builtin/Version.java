package jdos.win.builtin;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.loader.Module;
import jdos.win.loader.NativeModule;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.system.Scheduler;
import jdos.win.system.WinSystem;
import jdos.win.utils.Error;

public class Version extends BuiltinModule {
    public Version(Loader loader, int handle) {
        super(loader, "Version.dll", handle);
        add(GetFileVersionInfoA);
        add(GetFileVersionInfoSizeA);
        add(VerQueryValueA);
    }

    // BOOL WINAPI GetFileVersionInfo(LPCTSTR lptstrFilename, DWORD dwHandle, DWORD dwLen, LPVOID lpData)
    static private Callback.Handler GetFileVersionInfoA = new HandlerBase() {
        public java.lang.String getName() {
            return "Version.GetFileVersionInfoA";
        }
        public void onCall() {
            int lptstrFilename = CPU.CPU_Pop32();
            int dwHandle = CPU.CPU_Pop32();
            int dwLen = CPU.CPU_Pop32();
            int lpData = CPU.CPU_Pop32();

            if (lptstrFilename == 0) {
                CPU_Regs.reg_eax.dword = 0;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else {
                String name = new LittleEndianFile(lptstrFilename).readCString();
                Module module = WinSystem.getCurrentProcess().loader.loadModule(name);
                if (module == null) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_MOD_NOT_FOUND);
                } else {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    if (module instanceof NativeModule) {
                        IntRef size = new IntRef(0);
                        int address = ((NativeModule)module).getAddressOfResource(NativeModule.RT_VERSION, 1, size);
                        if (address != 0) {
                            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
                            Memory.mem_memcpy(lpData, address, Math.min(size.value, dwLen));
                        }
                    } else {
                        System.out.println(getName()+" tried to get version of builtin dll, this is not supported yet");
                    }
                    if (CPU_Regs.reg_eax.dword == WinAPI.FALSE) {
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_RESOURCE_DATA_NOT_FOUND);
                    }
                }
            }
        }
    };

    // DWORD WINAPI GetFileVersionInfoSize(LPCTSTR lptstrFilename, LPDWORD lpdwHandle)
    static private Callback.Handler GetFileVersionInfoSizeA = new HandlerBase() {
        public java.lang.String getName() {
            return "Version.GetFileVersionInfoSizeA";
        }
        public void onCall() {
            int lptstrFilename = CPU.CPU_Pop32();
            int lpdwHandle = CPU.CPU_Pop32();
            if (lpdwHandle != 0)
                Memory.mem_writed(lpdwHandle, 0);
            if (lptstrFilename == 0) {
                CPU_Regs.reg_eax.dword = 0;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else {
                String name = new LittleEndianFile(lptstrFilename).readCString();
                Module module = WinSystem.getCurrentProcess().loader.loadModule(name);
                if (module == null) {
                    CPU_Regs.reg_eax.dword = 0;
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_MOD_NOT_FOUND);
                } else {
                    CPU_Regs.reg_eax.dword = 0;
                    if (module instanceof NativeModule) {
                        IntRef size = new IntRef(0);
                        ((NativeModule) module).getAddressOfResource(NativeModule.RT_VERSION, 1, size);
                        CPU_Regs.reg_eax.dword = size.value;
                    } else {
                        System.out.println(getName()+" tried to get version of builtin dll, this is not supported yet");
                    }
                    if (CPU_Regs.reg_eax.dword == 0) {
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_RESOURCE_DATA_NOT_FOUND);
                    }
                }
            }
        }
    };

    // Direct port from Wine
    //
    // BOOL WINAPI VerQueryValue(LPCVOID pBlock, LPCTSTR lpSubBlock, LPVOID *lplpBuffer, PUINT puLen)
    private Callback.Handler VerQueryValueA = new HandlerBase() {
        class VersionInfo {
            public VersionInfo(int address) {
                wLength = Memory.mem_readw(address);
                wValueLength = Memory.mem_readw(address+2);
                wType = Memory.mem_readw(address+4);
                szKey = new LittleEndianFile(address+6).readCStringW();
            }
            int wLength;
            int wValueLength;
            int wType;
            String szKey; // WCHAR
        }
        static final String rootA = "\\";
        static final String varfileinfoA = "\\VarFileInfo\\Translation";
        public java.lang.String getName() {
            return "Version.VerQueryValueA";
        }

        // DWORD_ALIGN( (ver), (ver)->szKey + strlenW((ver)->szKey) + 1 )
        public int value(int address) {
            VersionInfo info = new VersionInfo(address);
            int startOfValue = 6; //offset to szKey
            startOfValue+=info.szKey.length()*2; // *2 because unicode
            startOfValue+=2; // Unicode null terminator;
            startOfValue = (startOfValue + 3) & ~3; // DWORD align as per spec
            return address+startOfValue;
        }

        // VersionInfo32_Children( ver )  (const VS_VERSION_INFO_STRUCT32 *)( VersionInfo32_Value( ver ) + ( ( (ver)->wValueLength * ((ver)->wType? 2 : 1) + 3 ) & ~3 ) )
        public int children(int address) {
            VersionInfo info = new VersionInfo(address);
            return value(address) + ((info.wValueLength * (info.wType!=0?2:1) + 3) & ~3);
        }

        // (VS_VERSION_INFO_STRUCT32 *)( (LPBYTE)ver + (((ver)->wLength + 3) & ~3) )
        public int next(int address) {
            VersionInfo info = new VersionInfo(address);
            return address+((info.wLength+3) & ~3);
        }

        public int findChild(int address, String key) {
            VersionInfo info = new VersionInfo(address);
            int pChild = children(address);
            while (pChild < address +info.wLength) {
                VersionInfo child = new VersionInfo(pChild);
                if (child.szKey.equalsIgnoreCase(key)) {
                    return pChild;
                }
                if (child.wLength == 0)
                    return 0;
                pChild = next(pChild);
            }
            return 0;
        }
        public void onCall() {
            int pBlock = CPU.CPU_Pop32();
            int lpSubBlock = CPU.CPU_Pop32();
            int lplpBuffer = CPU.CPU_Pop32();
            int puLen = CPU.CPU_Pop32();

            String subBlock = null;
            if (lpSubBlock != 0)
                subBlock = new LittleEndianFile(lpSubBlock).readCString();
            if (subBlock == null || subBlock.length()==0)
                subBlock = rootA;
            int info = pBlock;
            while (subBlock.length()>0) {
                int pos = subBlock.indexOf("\\");
                if (pos>=0) {
                    if (pos == 0) {
                        subBlock = subBlock.substring(pos+1);
                        continue;
                    }
                }
                info = findChild(info, subBlock.substring(0, pos));
                if (info == 0) {
                    if (puLen != 0)
                        Memory.mem_writed(puLen, 0);
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_RESOURCE_TYPE_NOT_FOUND);
                    CPU_Regs.reg_eax.dword = WinAPI.TRUE;
                    return;
                }
                subBlock = subBlock.substring(pos+1);
            }
            VersionInfo ver = new VersionInfo(info);
            Memory.mem_writed(lplpBuffer, value(info));
            if (puLen != 0)
                Memory.mem_writed(puLen, ver.wValueLength);
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };
}
