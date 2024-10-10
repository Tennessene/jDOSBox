package jdos.win.builtin.kernel32;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.cpu.Paging;
import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.win.Console;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.ReturnHandlerBase;
import jdos.win.builtin.WinAPI;
import jdos.win.kernel.WinCallback;
import jdos.win.loader.BuiltinModule;
import jdos.win.loader.Loader;
import jdos.win.loader.Module;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.system.*;
import jdos.win.utils.Error;
import jdos.win.utils.*;

import java.util.Random;
import java.util.TimeZone;

public class Kernel32 extends BuiltinModule {
    static private final int HEAP_CREATE_ENABLE_EXECUTE = 0x00040000;
    static private final int HEAP_GENERATE_EXCEPTIONS = 0x00000004;
    static private final int HEAP_ZERO_MEMORY = 0x00000008;
    static private final int HEAP_NO_SERIALIZE = 0x00000001;

    public Kernel32(Loader loader, int handle) {
        super(loader, "kernel32.dll", handle);

        add(WinProcess.class, "CloseHandle", new String[] {"hObject", "(BOOL)result"});
        add(KTime.class, "CompareFileTime", new String[] {"(HEX)lpFileTime1", "(HEX)lpFileTime1"});
        add(CompareStringA);
        add(WinPath.class, "CreateDirectoryA", new String[] {"(STRING)lpPathName", "(HEX)lpSecurityAttributes", "(BOOL)result"});
        add(CreateEventA);
        add(CreateFileA);
        add(CreateFileMappingA);
        add(CreateFileMappingW);
        add(CreateMutexA);
        add(WinProcess.class, "CreateProcessA", new String[] {"(STRING)lpApplicationName", "(STRING)lpCommandLine", "(HEX)lpProcessAttributes", "(HEX)lpThreadAttributes", "(BOOL)bInheritHandles", "(HEX)dwCreationFlags", "(HEX)lpEnvironment", "(STRING)lpCurrentDirectory", "(HEX)lpStartupInfo", "(HEX)lpProcessInformation"});
        add(CreateThread);
        add(DebugBreak);
        add(DecodePointer);
        add(DeleteCriticalSection);
        add(DeleteFileA);
        add(DisableThreadLibraryCalls);
        add(EncodePointer);
        add(EnterCriticalSection);
        add(EnumSystemLocalesA);
        add(EnumSystemLocalesW);
        add(ExitProcess);
        add(FatalAppExitA);
        add(FileTimeToLocalFileTime);
        add(FileTimeToSystemTime);
        add(FindClose);
        add(FindFirstFileA);
        add(FindNextFileA);
        add(KResource.class, "FindResourceA", new String[] {"hModule", "(STRING)lpName", "(STRINGlpType", "(HEX)result"});
        add(FormatMessageA);
        add(FreeEnvironmentStringsA);
        add(FreeEnvironmentStringsW);
        add(FreeLibrary);
        add(KResource.class, "FreeResource", new String[] {"(HEX)", "(BOOL)result"});
        add(GetACP);
        add(GetCommandLineA);
        add(GetCommandLineW);
        add(GetConsoleCP);
        add(GetConsoleMode);
        add(GetConsoleOutputCP);
        add(GetCPInfo);
        add(GetCurrentDirectoryA);
        add(GetCurrentProcess);
        add(GetCurrentProcessId);
        add(WinThread.class, "GetCurrentThread", new String[] {""});
        add(WinThread.class, "GetCurrentThreadId", new String[] {""});
        add(GetDateFormatA);
        add(GetDateFormatW);
        add(GetDiskFreeSpaceA);
        add(GetDriveTypeA);
        add(GetEnvironmentStrings);
        add(GetEnvironmentStringsA);
        add(GetEnvironmentStringsW);
        add(GetFileAttributesA);
        add(GetFileSize);
        add(GetFileType);
        add(WinPath.class, "GetFullPathNameA", new String[] {"(STRING)lpFileName", "nBufferLength", "(HEX)lpBuffer", "(HEX)lpFilePart", "result", "02(STRING)lpBuffer", "03(STRING)lpFilePart"});
        add(WinThread.class, "GetLastError", new String[0]);
        add(GetLocaleInfoA);
        add(GetLocaleInfoW);
        add(GetLocalTime);
        add(Volume.class, "GetLogicalDrives", new String[] {"(HEX)result"});
        add(GetModuleFileNameA);
        add(GetModuleFileNameW);
        add(GetModuleHandleA);
        add(GetModuleHandleW);
        add(WinLocale.class, "GetOEMCP", new String[0]);
        add(Profile.class, "GetPrivateProfileStringA", new String[] {"(STRING)lpAppName", "(STRING)lpKeyName", "(STRING)lpDefault", "(HEX)lpReturnedString", "nSize", "(STRING)lpFileName", "result", "03(STRING)lpReturnedString"});
        add(GetProcAddress);
        add(GetProcessHeap);
        add(WinProcess.class, "GetProcessVersion", new String[] {"ProcessId", "{(HEX)result"});
        add(WinPath.class, "GetShortPathNameA", new String[] {"(STRING)lpszLongPath", "(HEX)lpszShortPath", "cchBuffer", "result", "01(STRING)lpszShortPath"});
        add(GetStartupInfoA);
        add(GetStartupInfoW);
        add(GetStdHandle);
        add(GetStringTypeA);
        add(GetStringTypeW);
        add(KPath.class, "GetSystemDirectoryA", new String[] {"(HEX)lpBuffer", "uSize", "result", "00(STRING)lpBuffer"});
        add(GetSystemInfo);
        add(GetSystemTime);
        add(GetSystemTimeAsFileTime);
        add(GetTempFileNameA);
        add(GetTempPathA);
        add(GetTickCount);
        add(GetTimeFormatA);
        add(GetTimeFormatW);
        add(GetTimeZoneInformation);
        add(GetUserDefaultLCID);
        add(GetVersion);
        add(GetVersionExA);
        add(GetVersionExW);
        add(GetVolumeInformationA);
        add(GetWindowsDirectoryA);
        add(GetWindowsDirectoryW);
        add(GlobalAlloc);
        add(GlobalFree);
        add(Atom.class, "GlobalGetAtomNameA", new String[] {"nAtom", "(HEX)lpBuffer", "nSize", "result", "01(STRING)lpBuffer"});
        add(GlobalHandle);
        add(GlobalLock);
        add(GlobalReAlloc);
        add(GlobalUnlock);
        add(GlobalMemoryStatus);
        add(HeapAlloc);
        add(HeapCreate);
        add(HeapDestroy);
        add(HeapFree);
        add(HeapReAlloc);
        add(HeapSize);
        add(HeapValidate);
        add(_hread);
        add(InitializeCriticalSection);
        add(InitializeCriticalSectionAndSpinCount);
        add(InterlockedDecrement);
        add(InterlockedExchange);
        add(InterlockedIncrement);
        add(IsBadReadPtr);
        add(IsBadWritePtr);
        add(IsDebuggerPresent);
        add(IsValidCodePage);
        add(IsValidLocale);
        add(LCMapStringA);
        add(LCMapStringW);
        add(LeaveCriticalSection);
        add(_lclose);
        add(_llseek);
        add(_lread);
        add(LoadLibraryA);
        add(LoadLibraryW);
        add(KResource.class, "LoadResource", new String[] {"hModule", "(HEX)hResInfo", "(HEX)result"});
        add(KHeap.class, "LocalAlloc", new String[] {"(HEX)uFlags", "uBytes", "(HEX)result"});
        add(KHeap.class, "LocalFree", new String[] {"(HEX)hMem"});
        add(KHeap.class, "LocalReAlloc", new String[] {"(HEX)hMem", "(HEX)uFlags", "uBytes", "(HEX)result"});
        add(KResource.class, "LockResource", new String[] {"(HEX)hResData", "(HEX)result"});
        add(WinString.class, "lstrcatA", new String[] {"(STRING)lpString1", "(STRING)lpString2", "(STRING)result"});
        add(WinLocale.class, "lstrcmpA", new String[] {"(STRING)lpString1", "(STRING)lpString2"});
        add(lstrcpyA);
        add(lstrlenA);
        add(lstrlenW);
        add(WinString.class, "lstrcpynA", new String[] {"(HEX)lpString1", "(STRING)lpString2", "iMaxLength", "(STRING)result"});
        add(MapViewOfFile);
        add(WinPath.class, "MoveFileA", new String[] {"(STRING)lpExistingFileName", "(STRING)lpNewFileName", "(BOOL)result"});
        add(MulDiv);
        add(MultiByteToWideChar);
        add(KFile.class, "OpenFile", new String[] {"(STRING)lpFileName", "(HEX)lpReOpenBuf", "(HEX)wStyle"});
        add(OutputDebugStringW);
        add(OutputDebugStringA);
        add(QueryPerformanceCounter);
        add(RaiseException);
        add(ReadFile);
        add(ReleaseMutex);
        add(RtlMoveMemory);
        add(RtlUnwind);
        add(RtlZeroMemory);
        add(KPath.class, "SearchPathA", new String[] {"(STRINg)lpPath", "(STRING)lpFileName", "(STRING)lpExtension", "nBufferLength", "(HEX)lpBuffer", "(HEX)lpFilePart"});
        add(SetConsoleCtrlHandler);
        add(KPath.class, "SetCurrentDirectoryA", new String[] {"(STRING)lpPathName"});
        add(Environ.class, "SetEnvironmentVariableA", new String[] {"(STRING)lpName", "(STRING)lpValue", "(BOOL)result"});
        add(SetErrorMode);
        add(SetEvent);
        add(SetFilePointer);
        add(SetHandleCount);
        add(SetLastError);
        add(SetStdHandle);
        add(SetThreadPriority);
        add(SetUnhandledExceptionFilter);
        add(Sleep);
        add(TerminateProcess);
        add(TlsAlloc);
        add(TlsFree);
        add(TlsGetValue);
        add(TlsSetValue);
        add(UnhandledExceptionFilter);
        add(UnmapViewOfFile);
        add(VirtualAlloc);
        add(VirtualFree);
        add(VirtualQuery);
        add_wait(Sync.class, "WaitForSingleObject");
        add_wait(Sync.class, "WaitForMultipleObjects");
        add(WideCharToMultiByte);
        add(WinProcess.class, "WinExec", new String[] {"(STRING)lpCmdLine", "(HEX)uCmdShow"});
        add(WriteConsoleA);
        add(WriteConsoleW);
        add(WriteFile);
    }

    // int CompareString(LCID Locale, DWORD dwCmpFlags, LPCTSTR lpString1, int cchCount1, LPCTSTR lpString2, int cchCount2)
    private Callback.Handler CompareStringA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.CompareStringA";
        }
        public void onCall() {
            int Locale = CPU.CPU_Pop32();
            int dwCmpFlags = CPU.CPU_Pop32();
            int lpString1 = CPU.CPU_Pop32();
            int cchCount1 = CPU.CPU_Pop32();
            int lpString2 = CPU.CPU_Pop32();
            int cchCount2 = CPU.CPU_Pop32();
            String s1;
            String s2;

            if (cchCount1<0)
                s1 = new LittleEndianFile(lpString1).readCString();
            else
                s1 = new LittleEndianFile(lpString1).readCString(cchCount1);
            if (cchCount2<0)
                s2 = new LittleEndianFile(lpString2).readCString();
            else
                s2 = new LittleEndianFile(lpString2).readCString(cchCount2);
            int result;
            if (dwCmpFlags == 1) { // NORM_IGNORECASE
                result = s1.compareToIgnoreCase(s2);
            } else if (dwCmpFlags == 0) {
                result = s1.compareTo(s2);
            } else {
                Win.panic(getName()+" does not support 0x"+Ptr.toString(dwCmpFlags)+" flag yet.");
                return;
            }
            if (result < 0)
                result = 1; // CSTR_LESS_THAN
            else if (result > 0)
                result = 3; // CSTR_GREATER_THAN
            else
                result = 2; // CSTR_EQUAL
            CPU_Regs.reg_eax.dword = result;
        }
    };

    // HANDLE WINAPI CreateEvent(LPSECURITY_ATTRIBUTES lpEventAttributes, BOOL bManualReset, BOOL bInitialState, LPCTSTR lpName)
    private Callback.Handler CreateEventA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.CreateEventA";
        }
        public void onCall() {
            int lpEventAttributes = CPU.CPU_Pop32();
            int bManualReset = CPU.CPU_Pop32();
            int bInitialState = CPU.CPU_Pop32();
            int lpName = CPU.CPU_Pop32();
            String name = null;

            if (lpName != 0) {
                name = new LittleEndianFile(lpName).readCString();
                WinObject object = WinObject.getNamedObject(name);
                if (object != null) {
                    if (object instanceof WinEvent) {
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_ALREADY_EXISTS);
                        object.open();
                        CPU_Regs.reg_eax.dword = object.getHandle();
                    } else {
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                        CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                    }
                    return;
                }
            }
            WinEvent event = WinEvent.create(name, bManualReset!=0, bInitialState!=0);
            CPU_Regs.reg_eax.dword = event.getHandle();
        }
    };

    // HANDLE WINAPI CreateFile(LPCTSTR lpFileName, DWORD dwDesiredAccess, DWORD dwShareMode, LPSECURITY_ATTRIBUTES lpSecurityAttributes, DWORD dwCreationDisposition, DWORD dwFlagsAndAttributes, HANDLE hTemplateFile)
    private Callback.Handler CreateFileA = new HandlerBase() {
        private boolean create(FilePath file) {
            try {
                if (!file.createNewFile()) {
                    CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_ACCESS_DENIED);
                    return false;
                }
            } catch (Exception e) {
                CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_ACCESS_DENIED);
                return false;
            }
            return true;
        }

        public java.lang.String getName() {
            return "Kernel32.CreateFileA";
        }
        public void onCall() {
            int lpFileName = CPU.CPU_Pop32();
            int dwDesiredAccess = CPU.CPU_Pop32();
            int dwShareMode = CPU.CPU_Pop32();
            int lpSecurityAttributes = CPU.CPU_Pop32();
            int dwCreationDisposition = CPU.CPU_Pop32();
            int dwFlagsAndAttributes = CPU.CPU_Pop32();
            int hTemplateFile = CPU.CPU_Pop32();
            String name = new LittleEndianFile(lpFileName).readCString();
            boolean write = (dwDesiredAccess & WinAPI.GENERIC_WRITE) != 0;
            boolean read = (dwDesiredAccess & WinAPI.GENERIC_READ) != 0;

            FilePath file = WinSystem.getCurrentProcess().getFile(name);
            WinThread thread = Scheduler.getCurrentThread();

            if (!file.getParentFile().exists()) {
                if (file.getParentFile().getName().equalsIgnoreCase("windows") || (file.getParentFile().getName().equalsIgnoreCase("temp") && file.getParentFile().getParentFile().getName().equalsIgnoreCase("windows"))) {
                    file.getParentFile().mkdirs();
                }
                if (!file.getParentFile().exists()) {
                    CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                    thread.setLastError(Error.ERROR_PATH_NOT_FOUND);
                    return;
                }
            }

            switch (dwCreationDisposition) {
                case 1: // CREATE_NEW
                    if (file.exists()) {
                        CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                        thread.setLastError(Error.ERROR_FILE_EXISTS);
                        return;
                    }
                    if (!create(file)) {
                        return;
                    }
                    break;
                case 2: // CREATE_ALWAYS
                    if (file.exists()) {
                        thread.setLastError(Error.ERROR_ALREADY_EXISTS);
                        file.delete();
                    }
                    if (!create(file)) {
                        return;
                    }
                    break;
                case 3: // OPEN_EXISTING
                    if (!file.exists()) {
                        thread.setLastError(Error.ERROR_FILE_NOT_FOUND);
                        CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                        return;
                    }
                    break;
                case 4: // OPEN_ALWAYS
                    if (file.exists())
                        thread.setLastError(Error.ERROR_ALREADY_EXISTS);
                    else if (!create(file)) {
                        return;
                    }
                    break;
                case 5: // TRUNCATE_EXISTING
                    if (!file.exists()) {
                        thread.setLastError(Error.ERROR_FILE_NOT_FOUND);
                        CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                    }
                    file.delete();
                    if (!create(file)) {
                        return;
                    }
                    break;
            }
            WinFile winFile = WinFile.create(file, write, dwShareMode, dwFlagsAndAttributes);
            if (winFile != null)
                CPU_Regs.reg_eax.dword = winFile.getHandle();
            else {
                CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_ACCESS_DENIED);
            }
        }
    };

    // HANDLE WINAPI CreateFileMapping(HANDLE hFile, LPSECURITY_ATTRIBUTES lpAttributes, DWORD flProtect, DWORD dwMaximumSizeHigh, DWORD dwMaximumSizeLow, LPCTSTR lpName)
    private Callback.Handler CreateFileMappingA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.CreateFileMappingA";
        }
        public void onCall() {
            int hFile = CPU.CPU_Pop32();
            int addAtributes = CPU.CPU_Pop32();
            int flags = CPU.CPU_Pop32();
            int sizeHigh = CPU.CPU_Pop32();
            int sizeLow = CPU.CPU_Pop32();
            int addName = CPU.CPU_Pop32();
            String name = null;
            if (addName != 0)
                name = new LittleEndianFile(addName).readCString();
            if (name != null) {
                WinObject object = WinObject.getNamedObject(name);
                if (object != null) {
                    if (object instanceof WinFileMapping) {
                        WinFileMapping mapping = (WinFileMapping)object;
                        CPU_Regs.reg_eax.dword = mapping.handle;
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_ALREADY_EXISTS);
                        return;
                    }
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                    CPU_Regs.reg_eax.dword = 0;
                    return;
                }
            }
            WinFileMapping mapping = WinFileMapping.create(hFile, name, sizeLow | (((long) sizeHigh) << 32));
            CPU_Regs.reg_eax.dword = mapping.handle;
        }
    };
    private Callback.Handler CreateFileMappingW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.CreateFileMappingW";
        }
        public void onCall() {
            int hFile = CPU.CPU_Pop32();
            int addAtributes = CPU.CPU_Pop32();
            int flags = CPU.CPU_Pop32();
            int sizeHigh = CPU.CPU_Pop32();
            int sizeLow = CPU.CPU_Pop32();
            int name = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HANDLE WINAPI CreateMutex(LPSECURITY_ATTRIBUTES lpMutexAttributes, BOOL bInitialOwner, LPCTSTR lpName)
    private Callback.Handler CreateMutexA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.CreateMutexA";
        }
        public void onCall() {
            int lpMutexAttributes = CPU.CPU_Pop32();
            int bInitialOwner = CPU.CPU_Pop32();
            int lpName = CPU.CPU_Pop32();
            String name = null;
            if (lpName != 0)
                name = new LittleEndianFile(lpName).readCString();
            if (name != null) {
                WinObject object = WinObject.getNamedObject(name);
                if (object != null) {
                    if (object instanceof WinMutex) {
                        WinMutex mapping = (WinMutex)object;
                        CPU_Regs.reg_eax.dword = mapping.handle;
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_ALREADY_EXISTS);
                        return;
                    }
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                    CPU_Regs.reg_eax.dword = 0;
                    return;
                }
            }
            WinMutex mutex = WinMutex.create(name);
            if (bInitialOwner == 0)
                mutex.owner = null;
            CPU_Regs.reg_eax.dword = mutex.handle;
        }
    };

    private Callback.Handler CreateThreadCleanup = new HandlerBase() {
        public String getName() {
            return "Kernel32.CreateThread - Cleanup";
        }
        public void onCall() {
            int handle = CPU.CPU_Pop32();
            WinThread thread = WinThread.get(handle);
            thread.exit(CPU_Regs.reg_eax.dword);
        }
    };

    private long threadCleanup = 0;

    // HANDLE WINAPI CreateThread(LPSECURITY_ATTRIBUTES lpThreadAttributes, SIZE_T dwStackSize, LPTHREAD_START_ROUTINE lpStartAddress, LPVOID lpParameter, DWORD dwCreationFlags, LPDWORD lpThreadId)
    private Callback.Handler CreateThread = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.CreateThread";
        }
        public void onCall() {
            int attributes = CPU.CPU_Pop32();
            int stackSizeCommit = CPU.CPU_Pop32();
            int stackSizeReserved = stackSizeCommit;
            int start = CPU.CPU_Pop32();
            int params = CPU.CPU_Pop32();
            int flags = CPU.CPU_Pop32();
            int id = CPU.CPU_Pop32();

            if ((flags & 0x00010000)!=0) {
                stackSizeCommit = 0;
            }
            if ((flags & 0x00000004)!=0) {
                System.out.println("CreateThread with suspend flags not supported yet");
                Win.exit();
            }
            if (attributes != 0) {
                System.out.println("***WARNING*** attributes are not supported for CreateThread");
            }
            WinThread thread = WinSystem.getCurrentProcess().createThread(start, stackSizeCommit, stackSizeReserved);
            if (threadCleanup==0) {
                int cb = WinCallback.addCallback(CreateThreadCleanup);
                threadCleanup =  loader.registerFunction(cb);
            }
            thread.pushStack32(thread.handle);
            thread.pushStack32(0);  // what's this?
            thread.pushStack32(params);
            thread.pushStack32((int)threadCleanup);

            if (id != 0) {
                Memory.mem_writed(id, thread.getHandle());
            }
            CPU_Regs.reg_eax.dword = thread.getHandle();
        }
    };

    // void WINAPI DebugBreak(void)
    private Callback.Handler DebugBreak = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.DebugBreak";
        }
        public void onCall() {
            Console.out("DebugBreak was called\n");
            Win.exit();
        }
    };

    // PVOID DecodePointer(PVOID Ptr)
    static private Callback.Handler DecodePointer = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.DecodePointer";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = CPU.CPU_Pop32() ^ pointerObfuscator;
        }
    };

    // void WINAPI DeleteCriticalSection(LPCRITICAL_SECTION lpCriticalSection)
    static private Callback.Handler DeleteCriticalSection = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.DeleteCriticalSection";
        }
        public void onCall() {
            WinCriticalException.delete(CPU.CPU_Pop32());
        }
    };

    // BOOL WINAPI DeleteFile(LPCTSTR lpFileName)
    static private Callback.Handler DeleteFileA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.DeleteFileA";
        }
        public void onCall() {
            int lpFileName = CPU.CPU_Pop32();
            if (lpFileName == 0) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_PATH_NOT_FOUND);
                return;
            }
            String name = new LittleEndianFile(lpFileName).readCString();
            FilePath file = WinSystem.getCurrentProcess().getFile(name);
            if (file.exists()) {
                file.delete();
                if (file.exists()) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_ACCESS_DENIED); // :TODO: is this right
                    return;
                }
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            } else {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_PATH_NOT_FOUND);
            }
        }
    };

    // BOOL WINAPI DisableThreadLibraryCalls(HMODULE hModule)
    static private Callback.Handler DisableThreadLibraryCalls = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.DisableThreadLibraryCalls";
        }
        public void onCall() {
            int hModule = CPU.CPU_Pop32();
            Module module = WinSystem.getCurrentProcess().loader.getModuleByHandle(hModule);
            if (module == null) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                module.disableThreadLibraryCalls();
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };
    static int pointerObfuscator = new Random().nextInt();

    // PVOID EncodePointer(PVOID Ptr)
    static private Callback.Handler EncodePointer = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.EncodePointer";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = CPU.CPU_Pop32() ^ pointerObfuscator;
        }
    };

    // void WINAPI EnterCriticalSection(LPCRITICAL_SECTION lpCriticalSection)
    private Callback.Handler EnterCriticalSection = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.EnterCriticalSection";
        }
        public void onCall() {
            WinCriticalException.enter(CPU.CPU_Pop32());
        }
    };

    // BOOL EnumSystemLocales(LOCALE_ENUMPROC lpLocaleEnumProc, DWORD dwFlags)
    static private Callback.Handler EnumSystemLocalesA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.EnumSystemLocalesA";
        }
        public void onCall() {
            notImplemented();
        }
    };
    static private Callback.Handler EnumSystemLocalesW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.EnumSystemLocalesW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // VOID WINAPI ExitProcess(UINT uExitCode)
    private Callback.Handler ExitProcess = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.ExitProcess";
        }
        public void onCall() {
            int exitCode = CPU.CPU_Pop32();
            System.out.println("Win32 Process has exited (PID "+WinSystem.getCurrentProcess().getHandle()+"): code = " + exitCode);
            WinSystem.memory.printInfo();
            WinSystem.getCurrentProcess().exit();
            System.out.print(" -> ");
            WinSystem.memory.printInfo();
            System.out.println();
        }
    };

    // VOID WINAPI ExitThread(DWORD dwExitCode)
    private Callback.Handler ExitThread = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.ExitThread";
        }
        public void onCall() {
            int exitCode = CPU.CPU_Pop32();
            Scheduler.getCurrentThread().exit(exitCode);
        }
    };

    // void WINAPI FatalAppExit(UINT uAction, LPCTSTR lpMessageText)
    static private Callback.Handler FatalAppExitA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FatalAppExitA";
        }
        public void onCall() {
            notImplemented();
        }
    };
    static private Callback.Handler FatalAppExitW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FatalAppExitW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL WINAPI FileTimeToLocalFileTime(const FILETIME *lpFileTime, LPFILETIME lpLocalFileTime)
    private Callback.Handler FileTimeToLocalFileTime = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FileTimeToLocalFileTime";
        }
        public void onCall() {
            int lpFileTime = CPU.CPU_Pop32();
            int lpLocalFileTime = CPU.CPU_Pop32();
            long time = WinFile.readFileTime(lpFileTime);
            time = time + TimeZone.getDefault().getRawOffset()*10;
            WinFile.writeFileTime(lpLocalFileTime, time);
        }
    };

    // BOOL WINAPI FileTimeToSystemTime(const FILETIME *lpFileTime, LPSYSTEMTIME lpSystemTime)
    static private Callback.Handler FileTimeToSystemTime = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FileTimeToSystemTime";
        }
        public void onCall() {
            int lpFileTime = CPU.CPU_Pop32();
            int lpSystemTime = CPU.CPU_Pop32();
            if (lpFileTime == 0 || lpSystemTime == 0) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else {
                SystemTime.write(lpSystemTime, TimeZone.getTimeZone("UTC"), WinFile.filetimeToMillis(WinFile.readFileTime(lpFileTime)));
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // BOOL WINAPI FindClose(HANDLE hFindFile)
    static private Callback.Handler FindClose = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FindClose";
        }
        public void onCall() {
            int hFindFile = CPU.CPU_Pop32();
            WinFindFile object = WinFindFile.get(hFindFile);
            if (hFindFile == 0) {
                CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else if (object == null) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                object.close();
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // HANDLE WINAPI FindFirstFile(LPCTSTR lpFileName, LPWIN32_FIND_DATA lpFindFileData)
    private Callback.Handler FindFirstFileA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FindFirstFileA";
        }
        public void onCall() {
            int lpFileName = CPU.CPU_Pop32();
            int lpFindFileData = CPU.CPU_Pop32();
            if (lpFileName == 0 || lpFindFileData==0) {
                CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else {
                String fileName = new LittleEndianFile(lpFileName).readCString();
                boolean ok = true;
                if (fileName.contains("*") || fileName.contains("?")) {
                    int pos = fileName.indexOf("\\");
                    FilePath dir;
                    String search;
                    if (pos < 0) {
                        dir = new FilePath(WinSystem.getCurrentProcess().currentWorkingDirectory);
                        search = fileName;
                    } else {
                        dir = WinSystem.getCurrentProcess().getFile(fileName.substring(0, pos));
                        search = fileName.substring(pos+1);
                    }
                    if (!dir.exists()) {
                        ok = false;
                    } else {
                        FilePath[] result = dir.listFiles(new WinFile.WildCardFileFilter(search));
                        if (result.length == 0) {
                            ok = false;
                        } else {
                            WinFindFile findFile = WinFindFile.create(result);
                            CPU_Regs.reg_eax.dword = findFile.getHandle();
                            findFile.getNextResult(lpFindFileData);
                        }
                    }
                } else {
                    FilePath file = WinSystem.getCurrentProcess().getFile(fileName);
                    if (!file.exists()) {
                        ok = false;
                    } else {
                        WinFindFile findFile = WinFindFile.create(new FilePath[]{file});
                        CPU_Regs.reg_eax.dword = findFile.getHandle();
                        findFile.getNextResult(lpFindFileData);
                    }
                }
                if (!ok) {
                    CPU_Regs.reg_eax.dword = 0;
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_FILE_NOT_FOUND);
                }
            }
        }
    };

    // BOOL WINAPI FindNextFile(HANDLE hFindFile, LPWIN32_FIND_DATA lpFindFileData)
    static private Callback.Handler FindNextFileA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FindNextFileA";
        }
        public void onCall() {
            int hFindFile = CPU.CPU_Pop32();
            int lpFindFileData = CPU.CPU_Pop32();
            WinFindFile object = WinFindFile.get(hFindFile);
            if (hFindFile == 0 || lpFindFileData==0) {
                CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            } else if (object == null) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = object.getNextResult(lpFindFileData);
            }
        }
    };

    // DWORD WINAPI FormatMessage(DWORD dwFlags, LPCVOID lpSource, DWORD dwMessageId, DWORD dwLanguageId, LPTSTR lpBuffer, DWORD nSize, va_list *Arguments)
    static private Callback.Handler FormatMessageA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FormatMessageA";
        }
        public void onCall() {
            int dwFlags = CPU.CPU_Pop32();
            int lpSource = CPU.CPU_Pop32();
            int dwMessageId = CPU.CPU_Pop32();
            int dwLanguageId = CPU.CPU_Pop32();
            int lpBuffer = CPU.CPU_Pop32();
            int nSize = CPU.CPU_Pop32();
            int Arguments = CPU.CPU_Pop32();
            if (dwFlags == 0x1000) {
                String msg = Error.getError(dwMessageId);
                if (msg == null) {
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_MR_MID_NOT_FOUND);
                    CPU_Regs.reg_eax.dword = 0;
                } else {
                    StringUtil.strncpy(lpBuffer, msg, nSize);
                    CPU_Regs.reg_eax.dword = msg.length();
                }
            } else {
                Console.out("FormatMessage currently only supports the FORMAT_MESSAGE_FROM_SYSTEM flags");
                notImplemented();
            }
        }
    };

    // BOOL WINAPI FreeEnvironmentStrings(LPTCH lpszEnvironmentBlock)
    static private Callback.Handler FreeEnvironmentStringsA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FreeEnvironmentStringsA";
        }
        public void onCall() {
            int address = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };
    static private Callback.Handler FreeEnvironmentStringsW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FreeEnvironmentStringsW";
        }
        public void onCall() {
            int address = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // BOOL WINAPI FreeLibrary(HMODULE hModule)
    static private Callback.Handler FreeLibrary = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.FreeLibrary";
        }
        public void onCall() {
            int hModule = CPU.CPU_Pop32();
            Module module = WinSystem.getCurrentProcess().getModuleByHandle(hModule);
            if (module == null) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                System.out.println(getName()+" faked: "+module.name);
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // UINT GetACP(void)
    static private Callback.Handler GetACP = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetACP";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = 1252; // ANSI Latin 1; Western European (Windows)
        }
    };

    // LPTSTR WINAPI GetCommandLine(void)
    private Callback.Handler GetCommandLineA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetCommandLineA";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getCommandLine();
        }
    };
    private Callback.Handler GetCommandLineW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetCommandLineW";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getCommandLineW();
        }
    };

    // UINT WINAPI GetConsoleCP(void)
    private Callback.Handler GetConsoleCP = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetConsoleCP";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL WINAPI GetConsoleMode(HANDLE hConsoleHandle, LPDWORD lpMode)
    private Callback.Handler GetConsoleMode = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetConsoleMode";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // UINT WINAPI GetConsoleOutputCP(void)
    private Callback.Handler GetConsoleOutputCP = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetConsoleOutputCP";
        }
        public void onCall() {
            notImplemented();
        }
    };

    /*
     typedef struct _cpinfo {
      UINT MaxCharSize;
      BYTE DefaultChar[MAX_DEFAULTCHAR];
      BYTE LeadByte[MAX_LEADBYTES];
    }
    */

    // BOOL GetCPInfo(UINT CodePage, LPCPINFO lpCPInfo)
    private Callback.Handler GetCPInfo = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetCPInfo";
        }
        public void onCall() {
            int CodePage = CPU.CPU_Pop32();
            int add = CPU.CPU_Pop32();
            if (CodePage == 1252 || CodePage == 437) {
                Memory.mem_writed(add, 1); add+=4;// MaxCharSize
                Memory.mem_writeb(add, 63); add+=1; // DefaultChar ?
                Memory.mem_writeb(add, 0); add+=1; //
                Memory.mem_zero(add, 12); // LeadByte
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            } else {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            }
        }
    };

    // DWORD WINAPI GetCurrentDirectory(DWORD nBufferLength, LPTSTR lpBuffer)
    static private Callback.Handler GetCurrentDirectoryA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetCurrentDirectoryA";
        }
        public void onCall() {
            int nBufferLength = CPU.CPU_Pop32();
            int lpBuffer = CPU.CPU_Pop32();
            String cwd = WinSystem.getCurrentProcess().currentWorkingDirectory;
            int len = cwd.getBytes().length;

            if (lpBuffer == 0 || nBufferLength<len+1)
                CPU_Regs.reg_eax.dword = len+1;
            else {
                StringUtil.strcpy(lpBuffer, cwd);
                CPU_Regs.reg_eax.dword = len;
            }
        }
    };

    // HANDLE WINAPI GetCurrentProcess(void)
    static private Callback.Handler GetCurrentProcess = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetCurrentProcess";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getHandle();
        }
    };

    // DWORD WINAPI GetCurrentProcessId(void)
    private Callback.Handler GetCurrentProcessId = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetCurrentProcessId";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getHandle();
        }
    };

    // int GetDateFormat(LCID Locale, DWORD dwFlags, const SYSTEMTIME *lpDate, LPCTSTR lpFormat, LPTSTR lpDateStr, int cchDate)
    static private Callback.Handler GetDateFormatA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetDateFormatA";
        }
        public void onCall() {
            notImplemented();
        }
    };
    static private Callback.Handler GetDateFormatW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetDateFormatW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL WINAPI GetDiskFreeSpace(LPCTSTR lpRootPathName, LPDWORD lpSectorsPerCluster, LPDWORD lpBytesPerSector, LPDWORD lpNumberOfFreeClusters, LPDWORD lpTotalNumberOfClusters)
    static private Callback.Handler GetDiskFreeSpaceA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetDiskFreeSpaceA";
        }
        public void onCall() {
            int lpRootPathName = CPU.CPU_Pop32();
            int lpSectorsPerCluster = CPU.CPU_Pop32();
            int lpBytesPerSector = CPU.CPU_Pop32();
            int lpNumberOfFreeClusters = CPU.CPU_Pop32();
            int lpTotalNumberOfClusters = CPU.CPU_Pop32();
            String path = new LittleEndianFile(lpRootPathName).readCString();
            Memory.mem_writed(lpSectorsPerCluster, 8);
            Memory.mem_writed(lpBytesPerSector, 512);
            // Maybe when Java 6 is required we can do this, for now just fake it
            Memory.mem_writed(lpNumberOfFreeClusters, 32746501);
            Memory.mem_writed(lpTotalNumberOfClusters, 33551744);
        }
    };

    // UINT WINAPI GetDriveType(LPCTSTR lpRootPathName)
    private Callback.Handler GetDriveTypeA = new HandlerBase() {
            public java.lang.String getName() {
                return "Kernel32.GetDriveTypeA";
            }
            public void onCall() {
                int lpFileName = CPU.CPU_Pop32();
                String dir;
                if (lpFileName == 0) {
                    dir = WinSystem.getCurrentProcess().currentWorkingDirectory;
                } else {
                    dir = new LittleEndianFile(lpFileName).readCString();
                }
                FilePath file = WinSystem.getCurrentProcess().getFile(dir);
                if (file.exists()) {
                    CPU_Regs.reg_eax.dword = 3; // DRIVE_FIXED
                } else {
                    CPU_Regs.reg_eax.dword = 1; // DRIVE_NO_ROOT_DIR
                }
            }
    };

    // LPTCH WINAPI GetEnvironmentStrings(void)
    private Callback.Handler GetEnvironmentStrings = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetEnvironmentStrings";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getEnvironment();
        }
    };
    private Callback.Handler GetEnvironmentStringsA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetEnvironmentStringsA";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getEnvironment();
        }
    };
    private Callback.Handler GetEnvironmentStringsW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetEnvironmentStringsW";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getEnvironmentW();
        }
    };

    // DWORD WINAPI GetFileAttributes(LPCTSTR lpFileName)
    private Callback.Handler GetFileAttributesA = new HandlerBase() {
            public java.lang.String getName() {
                return "Kernel32.GetFileAttributesA";
            }
            public void onCall() {
                int lpFileName = CPU.CPU_Pop32();
                if (lpFileName == 0) {
                    CPU_Regs.reg_eax.dword = Error.INVALID_FILE_ATTRIBUTES;
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                } else {
                    String fileName = new LittleEndianFile(lpFileName).readCString();
                    FilePath file = WinSystem.getCurrentProcess().getFile(fileName);
                    if (file.exists()) {
                        if (file.isDirectory())
                            CPU_Regs.reg_eax.dword = WinFile.FILE_ATTRIBUTE_DIRECTORY;
                        else
                            CPU_Regs.reg_eax.dword = WinFile.FILE_ATTRIBUTE_NORMAL;
                    } else {
                        CPU_Regs.reg_eax.dword = Error.INVALID_FILE_ATTRIBUTES;
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_BAD_PATHNAME);
                    }
                }
            }
    };

    // DWORD WINAPI GetFileSize(HANDLE hFile, LPDWORD lpFileSizeHigh)
    private Callback.Handler GetFileSize = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetFileSize";
        }
        public void onCall() {
            int hFile = CPU.CPU_Pop32();
            int lpFileSizeHigh = CPU.CPU_Pop32();
            WinFile file = WinFile.get(hFile);
            if (file == null) {
                CPU_Regs.reg_eax.dword = -1;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                long size = file.size();
                CPU_Regs.reg_eax.dword = (int)size;
                if (lpFileSizeHigh != 0) {
                    Memory.mem_writed(lpFileSizeHigh, (int)(size >>> 32));
                }
            }
        }
    };

    // DWORD WINAPI GetFileType(HANDLE hFile)
    private Callback.Handler GetFileType = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetFileType";
        }
        public void onCall() {
            int hFile = CPU.CPU_Pop32();
            WinFile file = WinFile.get(hFile);
            if (file == null) {
                CPU_Regs.reg_eax.dword = 0;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = file.type;
            }
        }
    };

    // int GetLocaleInfo(LCID Locale, LCTYPE LCType, LPTSTR lpLCData, int cchData)
    static private Callback.Handler GetLocaleInfoA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetLocaleInfoA";
        }
        public void onCall() {
            notImplemented();
        }
    };
    static private Callback.Handler GetLocaleInfoW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetLocaleInfoW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // void WINAPI GetLocalTime(LPSYSTEMTIME lpSystemTime)
     private Callback.Handler GetLocalTime = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetLocalTime";
        }
        public void onCall() {
            int lpSystemTime = CPU.CPU_Pop32();
            SystemTime.write(lpSystemTime, TimeZone.getDefault(), System.currentTimeMillis());
        }
    };

    //DWORD WINAPI GetModuleFileName(HMODULE hModule, LPTSTR lpFilename, DWORD nSize)
    private Callback.Handler GetModuleFileNameA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetModuleFileNameA";
        }
        public void onCall() {
            int handle = CPU.CPU_Pop32();
            int buffer = CPU.CPU_Pop32();
            int cb = CPU.CPU_Pop32();
            Module module = WinSystem.getCurrentProcess().getModuleByHandle(handle);
            if (module == null) {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = 0;
            } else {
                String path = module.getFileName(true);
                if (cb<path.length()+1) {
                    StringUtil.strncpy(buffer, path, cb);
                    CPU_Regs.reg_eax.dword = cb;
                } else {
                    StringUtil.strcpy(buffer, path);
                    CPU_Regs.reg_eax.dword = path.length();
                }
            }
        }
    };
    private Callback.Handler GetModuleFileNameW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetModuleFileNameW";
        }
        public void onCall() {
            int handle = CPU.CPU_Pop32();
            int buffer = CPU.CPU_Pop32();
            int cb = CPU.CPU_Pop32();
            Module module = WinSystem.getCurrentProcess().getModuleByHandle(handle);
            if (module == null) {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = 0;
            } else {
                String path = module.getFileName(true);
                if (cb<path.length()+1) {
                    StringUtil.strncpyW(buffer, path, cb);
                    CPU_Regs.reg_eax.dword = cb;
                } else {
                    StringUtil.strcpyW(buffer, path);
                    CPU_Regs.reg_eax.dword = path.length();
                }
            }
        }
    };

    // HMODULE WINAPI GetModuleHandle(LPCTSTR lpModuleName)
    private Callback.Handler GetModuleHandleA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetModuleHandleA";
        }
        public void onCall() {
            int add = CPU.CPU_Pop32();
            if (add == 0) {
                CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().mainModule.getHandle();
            } else {
                String name = new LittleEndianFile(add).readCString();
                CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getModuleByName(name);
                if (CPU_Regs.reg_eax.dword == 0)
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_MOD_NOT_FOUND);
            }
        }
    };
    static private Callback.Handler GetModuleHandleW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetModuleHandleW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // FARPROC WINAPI GetProcAddress(HMODULE hModule, LPCSTR lpProcName)
     private Callback.Handler GetProcAddress = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetProcAddress";
        }
        public void onCall() {
            int handle = CPU.CPU_Pop32();
            int procName = CPU.CPU_Pop32();
            String name = new LittleEndianFile(procName).readCString();
            System.out.println("GetProcAddress "+name);
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getProcAddress(handle, name);
        }
    };
    
    // HANDLE WINAPI GetProcessHeap(void)
    private Callback.Handler GetProcessHeap = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetProcessHeap";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getHeapHandle();
        }
    };

    /*
    typedef struct _STARTUPINFO {
      DWORD  cb;
      LPTSTR lpReserved;
      LPTSTR lpDesktop;
      LPTSTR lpTitle;
      DWORD  dwX;
      DWORD  dwY;
      DWORD  dwXSize;
      DWORD  dwYSize;
      DWORD  dwXCountChars;
      DWORD  dwYCountChars;
      DWORD  dwFillAttribute;
      DWORD  dwFlags;
      WORD   wShowWindow;
      WORD   cbReserved2;
      LPBYTE lpReserved2;
      HANDLE hStdInput;
      HANDLE hStdOutput;
      HANDLE hStdError;
    }
     */
    // VOID WINAPI GetStartupInfo(LPSTARTUPINFO lpStartupInfo)
    private Callback.Handler GetStartupInfoA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetStartupInfoA";
        }
        public void onCall() {
            int add = CPU.CPU_Pop32();
            int cb = Memory.mem_readd(add);
            Memory.mem_writed(add, 68); add+=4; // cb
            Memory.mem_writed(add, 0); add+=4; // lpReserved
            Memory.mem_writed(add, 0); add+=4; // lpDesktop
            Memory.mem_writed(add, 0); add+=4; // lpTitle
            Memory.mem_writed(add, 0); add+=4; // dwX
            Memory.mem_writed(add, 0); add+=4; // dwY
            Memory.mem_writed(add, 0); add+=4; // dwXSize
            Memory.mem_writed(add, 0); add+=4; // dwYSize
            if (WinSystem.getCurrentProcess().console) {
                Memory.mem_writed(add, 80); add+=4; // dwXCountChars
                Memory.mem_writed(add, 25); add+=4; // dwYCountChars
            } else {
                Memory.mem_writed(add, 0); add+=4; // dwXCountChars
                Memory.mem_writed(add, 0); add+=4; // dwYCountChars
            }
            Memory.mem_writed(add, 0); add+=4; // dwFillAttribute
            Memory.mem_writed(add, 0); add+=4; // dwFlags
            Memory.mem_writew(add, 0); add+=2; // wShowWindow
            Memory.mem_writew(add, 0); add+=2; // cbReserved2
            Memory.mem_writed(add, 0); add+=4; // lpReserved2
            Memory.mem_writed(add, WinFile.STD_IN); add+=4; // hStdInput
            Memory.mem_writed(add, WinFile.STD_OUT); add+=4; // hStdOutput
            Memory.mem_writed(add, WinFile.STD_ERROR); add+=4; // hStdError
        }
    };
    static private Callback.Handler GetStartupInfoW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetStartupInfoW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // HANDLE WINAPI GetStdHandle(DWORD nStdHandle)
    static private Callback.Handler GetStdHandle = new HandlerBase() {
        final int STD_INPUT_HANDLE = -10;
        final int STD_OUTPUT_HANDLE = -11;
        final int STD_ERROR_HANDLE = -12;

        public java.lang.String getName() {
            return "Kernel32.GetStdHandle";
        }
        public void onCall() {
            int param = CPU.CPU_Pop32();
            switch (param) {
                case STD_INPUT_HANDLE:
                    CPU_Regs.reg_eax.dword = WinFile.STD_IN;
                    break;
                case STD_OUTPUT_HANDLE:
                    CPU_Regs.reg_eax.dword = WinFile.STD_OUT;
                    break;
                case STD_ERROR_HANDLE:
                    CPU_Regs.reg_eax.dword = WinFile.STD_ERROR;
                    break;
                default:
                    CPU_Regs.reg_eax.dword = WinAPI.INVALID_HANDLE_VALUE;
                    break;
            }
        }
    };

    // BOOL GetStringTypeA(LCID Locale, DWORD dwInfoType, LPCSTR lpSrcStr, int cchSrc, LPWORD lpCharType)
    static private Callback.Handler GetStringTypeA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetStringTypeA";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL GetStringTypeW(DWORD dwInfoType, LPCWSTR lpSrcStr, int cchSrc, LPWORD lpCharType)
    private Callback.Handler GetStringTypeW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetStringTypeW";
        }
        public void onCall() {
            int type = CPU.CPU_Pop32();
            int src = CPU.CPU_Pop32();
            int count = CPU.CPU_Pop32();
            int lpCharType = CPU.CPU_Pop32();

            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            if (count == -1) count = StringUtil.strlenW(src) + 1;
            switch(type)
            {
            case WinAPI.CT_CTYPE1:
                while (count-- > 0) {
                    char c = (char)Memory.mem_readw(src);
                    Memory.mem_writew(lpCharType, Unicode.get_char_typeW(c));
                    lpCharType+=2;
                    src+=2;
                }
                break;
            case WinAPI.CT_CTYPE2:
                while (count-- > 0) {
                    char c = (char)Memory.mem_readw(src);
                    Memory.mem_writew(lpCharType, Unicode.get_char_directionW(c));
                    lpCharType+=2;
                    src+=2;
                }
                break;
            case WinAPI.CT_CTYPE3:
                Console.out(getName()+" flag CT_CTYPE3 not implemented yet");
                notImplemented();
                break;
            default:
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
            }
        }
    };

    /*
    typedef struct _SYSTEM_INFO {
      WORD wProcessorArchitecture;
      WORD wReserved;
      DWORD     dwPageSize;
      LPVOID    lpMinimumApplicationAddress;
      LPVOID    lpMaximumApplicationAddress;
      DWORD_PTR dwActiveProcessorMask;
      DWORD     dwNumberOfProcessors;
      DWORD     dwProcessorType;
      DWORD     dwAllocationGranularity;
      WORD      wProcessorLevel;
      WORD      wProcessorRevision;
    }
    */

    // void WINAPI GetSystemInfo(LPSYSTEM_INFO lpSystemInfo)
    static private Callback.Handler GetSystemInfo = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetSystemInfo";
        }
        public void onCall() {
            int add = CPU.CPU_Pop32();
            Memory.mem_writew(add, 0); // PROCESSOR_ARCHITECTURE_INTEL
            Memory.mem_writew(add + 2, 0); // Reserved
            Memory.mem_writed(add + 4, 4096); // Page Size
            Memory.mem_writed(add+8, 0x0001000); // :TODO: not sure if this matter, this is just what I say Windows 7 return
            Memory.mem_writed(add+12, 0x7ffeffff); // :TODO: not sure if this matter, this is just what I say Windows 7 return
            Memory.mem_writed(add+16, 1);
            Memory.mem_writed(add+20, 1); // Processor count
            Memory.mem_writed(add+24, 586); // Processor Type
            Memory.mem_writed(add+28, 4096); // Allocation Granulatiry Win 7 64-bit said 65536, but I think this might be better for here
            Memory.mem_writew(add+32, 6); // :TODO: no idea
            Memory.mem_writew(add+34, 6660); // :TODO: no idea
        }
    };

    // void WINAPI GetSystemTime(LPSYSTEMTIME lpSystemTime)
    private Callback.Handler GetSystemTime = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetSystemTime";
        }
        public void onCall() {
            int lpSystemTime = CPU.CPU_Pop32();
            SystemTime.write(lpSystemTime, TimeZone.getTimeZone("UTC"), System.currentTimeMillis());
        }
    };

    /*
        typedef struct _FILETIME {
            DWORD dwLowDateTime;
            DWORD dwHighDateTime;
        }
     */
    // void WINAPI GetSystemTimeAsFileTime(LPFILETIME lpSystemTimeAsFileTime)
    static private Callback.Handler GetSystemTimeAsFileTime = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetSystemTimeAsFileTime";
        }
        public void onCall() {
            int add = CPU.CPU_Pop32();
            long time = WinFile.millisToFiletime(System.currentTimeMillis());
            WinFile.writeFileTime(add, time);
        }
    };

    // UINT WINAPI GetTempFileName(LPCTSTR lpPathName, LPCTSTR lpPrefixString, UINT uUnique, LPTSTR lpTempFileName)
    private Callback.Handler GetTempFileNameA = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetTempFileNameA";
        }
        public int processReturn() {
            int lpPathName = CPU.CPU_Pop32();
            int lpPrefixString = CPU.CPU_Pop32();
            int uUnique = CPU.CPU_Pop32();
            int lpTempFileName = CPU.CPU_Pop32();
            String path = StringUtil.getString(lpPathName);
            String prefix = StringUtil.getString(lpPrefixString);
            if (path.equals("."))
                path = WinSystem.getCurrentProcess().currentWorkingDirectory;

            if (uUnique != 0) {
                String result = path+prefix+Ptr.toString(uUnique)+".TMP";
                StringUtil.strcpy(lpTempFileName, result);
                return uUnique;
            } else {
                while (true) {
                    String name = path+prefix+Ptr.toString(uUnique)+".TMP";
                    FilePath file = WinSystem.getCurrentProcess().getFile(name);
                    if (file.exists()) {
                        uUnique++;
                        continue;
                    }
                    try {
                        file.createNewFile();
                    } catch (Exception e) {

                    }
                    StringUtil.strcpy(lpTempFileName, name);
                    return uUnique;
                }
            }
        }
    };

    // DWORD WINAPI GetTempPath(DWORD nBufferLength, LPTSTR lpBuffer)
    private Callback.Handler GetTempPathA = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetTempPathA";
        }
        public int processReturn() {
            int nBufferLength = CPU.CPU_Pop32();
            int lpBuffer = CPU.CPU_Pop32();
            StringUtil.strncpy(lpBuffer, WinAPI.TEMP_PATH, nBufferLength);
            return WinAPI.TEMP_PATH.length();
        }
    };

    // DWORD WINAPI GetTickCount(void)
    private Callback.Handler GetTickCount = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetTickCount";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinSystem.getTickCount();
        }
    };

    // int GetTimeFormat(LCID Locale, DWORD dwFlags, const SYSTEMTIME *lpTime, LPCTSTR lpFormat, LPTSTR lpTimeStr, int cchTime)
    static private Callback.Handler GetTimeFormatA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetTimeFormatA";
        }
        public void onCall() {
            notImplemented();
        }
    };
    static private Callback.Handler GetTimeFormatW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetTimeFormatW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // DWORD WINAPI GetTimeZoneInformation(LPTIME_ZONE_INFORMATION lpTimeZoneInformation);
    static private Callback.Handler GetTimeZoneInformation = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetTimeZoneInformation";
        }
        public void onCall() {
            int lpTimeZoneInformation = CPU.CPU_Pop32();
            Memory.mem_zero(lpTimeZoneInformation, 172);
            Memory.mem_writed(lpTimeZoneInformation, 480);                          // 0 Bias
            StringUtil.strcpy(lpTimeZoneInformation+4, "Pacific Standard Time");    // 4 StandardName
                                                                                    // 68 StandardDate
            Memory.mem_writed(lpTimeZoneInformation+84, 0);                         // 84 StandardBias
            StringUtil.strcpy(lpTimeZoneInformation+88, "Pacific Daylight Time");   // 88 DaylightName
                                                                                    // 152 DaylightDate
            Memory.mem_writed(lpTimeZoneInformation+168, 0);                        // 168 DaylightBias
            CPU_Regs.reg_eax.dword = 1; //TIME_ZONE_ID_STANDARD
        }
    };

    // LCID GetUserDefaultLCID(void)
    static private Callback.Handler GetUserDefaultLCID = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetUserDefaultLCID";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // DWORD WINAPI GetVersion(void)
    static private Callback.Handler GetVersion = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetVersion";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = 0x0A280105; // 5.2.2600 WinXP SP2
        }
    };

    /*
    typedef struct _OSVERSIONINFO {
      DWORD dwOSVersionInfoSize;
      DWORD dwMajorVersion;
      DWORD dwMinorVersion;
      DWORD dwBuildNumber;
      DWORD dwPlatformId;
      TCHAR szCSDVersion[128];
    } OSVERSIONINFO;

    typedef struct _OSVERSIONINFOEX {
      DWORD dwOSVersionInfoSize;
      DWORD dwMajorVersion;
      DWORD dwMinorVersion;
      DWORD dwBuildNumber;
      DWORD dwPlatformId;
      TCHAR szCSDVersion[128];
      WORD  wServicePackMajor;
      WORD  wServicePackMinor;
      WORD  wSuiteMask;
      BYTE  wProductType;
      BYTE  wReserved;
    }
    */

    // BOOL WINAPI GetVersionEx(LPOSVERSIONINFO lpVersionInfo)
    static private Callback.Handler GetVersionExA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetVersionExA";
        }
        public void onCall() {
            int add = CPU.CPU_Pop32();
            int size = Memory.mem_readd(add);
            if (size == 148 || size == 156) {
                add+=4; // dwOSVersionInfoSize
                Memory.mem_writed(add, 5);add+=4; // dwMajorVersion
                Memory.mem_writed(add, 1);add+=4; // dwMinorVersion
                Memory.mem_writed(add, 2600);add+=4; // dwBuildNumber
                Memory.mem_writed(add, 2);add+=4; // dwPlatformId
                byte[] sp = "Service Pack 2".getBytes();
                Memory.mem_memcpy(add, sp, 0, sp.length);
                Memory.mem_writeb(add+sp.length, 0); add+=128;
                if (size == 156) {
                    Memory.mem_writew(add, 2);add+=2; // wServicePackMajor
                    Memory.mem_writew(add, 0);add+=2; // wServicePackMinor
                    Memory.mem_writew(add, 0);add+=2; // wSuiteMask
                    Memory.mem_writeb(add, 1);add+=1; // wProductType - VER_NT_WORKSTATION
                    Memory.mem_writeb(add, 0);add+=1; // wReserved
                }
            } else {
                Console.out(getName()+" was passed an unexpected size of "+size);
                Win.exit();
            }
        }
    };
    static private Callback.Handler GetVersionExW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetVersionExW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL WINAPI GetVolumeInformation(LPCTSTR lpRootPathName, LPTSTR lpVolumeNameBuffer, DWORD nVolumeNameSize, LPDWORD lpVolumeSerialNumber, LPDWORD lpMaximumComponentLength, LPDWORD lpFileSystemFlags, LPTSTR lpFileSystemNameBuffer, DWORD nFileSystemNameSize)
    static private Callback.Handler GetVolumeInformationA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetVolumeInformationA";
        }
        public void onCall() {
            int lpRootPathName = CPU.CPU_Pop32();
            int lpVolumeNameBuffer = CPU.CPU_Pop32();
            int nVolumeNameSize = CPU.CPU_Pop32();
            int lpVolumeSerialNumber = CPU.CPU_Pop32();
            int lpMaximumComponentLength = CPU.CPU_Pop32();
            int lpFileSystemFlags = CPU.CPU_Pop32();
            int lpFileSystemNameBuffer = CPU.CPU_Pop32();
            int nFileSystemNameSize = CPU.CPU_Pop32();
            String dir;
            if (lpRootPathName == 0)
                dir = WinSystem.getCurrentProcess().currentWorkingDirectory;
            else
                dir = new LittleEndianFile(lpRootPathName).readCString();
            if (!WinSystem.getCurrentProcess().getFile(dir).exists()) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_BAD_PATHNAME);
                return;
            }
            if (lpVolumeNameBuffer != 0) {
                Memory.mem_writed(lpVolumeNameBuffer, 0);
            }
            if (lpVolumeSerialNumber != 0) {
                Memory.mem_writed(lpVolumeNameBuffer, 0xC409F45A);
            }
            if (lpMaximumComponentLength != 0) {
                Memory.mem_writed(lpMaximumComponentLength, 0xFF);
            }
            if (lpFileSystemFlags != 0) {
                Memory.mem_writed(lpFileSystemFlags, 0x700FF);
            }
            if (lpFileSystemNameBuffer != 0) {
                StringUtil.strcpy(lpFileSystemNameBuffer, "NTFS");
            }
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // UINT WINAPI GetWindowsDirectory(LPTSTR lpBuffer, UINT uSize)
    static private Callback.Handler GetWindowsDirectoryA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetWindowsDirectoryA";
        }
        public void onCall() {
            int lpBuffer = CPU.CPU_Pop32();
            int uSize = CPU.CPU_Pop32();
            StringUtil.strncpy(lpBuffer, WinAPI.WIN32_PATH, uSize);
            CPU_Regs.reg_eax.dword = WinAPI.WIN32_PATH.length()+1;
        }
    };
    static private Callback.Handler GetWindowsDirectoryW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GetWindowsDirectoryW";
        }
        public void onCall() {
            int lpBuffer = CPU.CPU_Pop32();
            int uSize = CPU.CPU_Pop32();
            String result = "C:\\WINDOWS";
            StringUtil.strncpyW(lpBuffer, result, uSize);
            CPU_Regs.reg_eax.dword = result.length()+1;
        }
    };

    // HGLOBAL WINAPI GlobalAlloc(UINT uFlags, SIZE_T dwBytes)
    static private Callback.Handler GlobalAlloc = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GlobalAlloc";
        }
        public int processReturn() {
            int uFlags = CPU.CPU_Pop32();
            int dwBytes = CPU.CPU_Pop32();
            int result = WinSystem.getCurrentProcess().heap.alloc(dwBytes, false);
            if ((uFlags & 0x0040)!=0) // GMEM_ZEROINIT
                Memory.mem_zero(result, dwBytes);
            return result;
        }
    };

    // HGLOBAL WINAPI GlobalFree(HGLOBAL hMem)
    static private Callback.Handler GlobalFree = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GlobalFree";
        }
        public int processReturn() {
            int hMem = CPU.CPU_Pop32();
            WinSystem.getCurrentProcess().heap.free(hMem);
            return 0;
        }
    };

    // HGLOBAL WINAPI GlobalHandle(LPCVOID pMem)
    static private Callback.Handler GlobalHandle = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GlobalHandle";
        }
        public int processReturn() {
            int hMem = CPU.CPU_Pop32();
            return hMem;
        }
    };

    // LPVOID WINAPI GlobalLock(HGLOBAL hMem)
    static private Callback.Handler GlobalLock = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GlobalLock";
        }
        public int processReturn() {
            int hMem = CPU.CPU_Pop32();
            return hMem;
        }
    };

    // HGLOBAL WINAPI GlobalReAlloc(HGLOBAL hMem, SIZE_T dwBytes, UINT uFlags)
    static private Callback.Handler GlobalReAlloc = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GlobalReAlloc";
        }
        public int processReturn() {
            int hMem = CPU.CPU_Pop32();
            int dwBytes = CPU.CPU_Pop32();
            int uFlags = CPU.CPU_Pop32();
            if ((uFlags & 0x0080)!=0) // GMEM_MODIFY
                return hMem;
            return WinSystem.getCurrentProcess().heap.realloc(hMem, dwBytes, (uFlags & 0x0040)!=0);
        }
    };

    // BOOL WINAPI GlobalUnlock(HGLOBAL hMem)
    static private Callback.Handler GlobalUnlock = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GlobalUnlock";
        }
        public int processReturn() {
            int hMem = CPU.CPU_Pop32();
            return 0;
        }
    };

    // void WINAPI GlobalMemoryStatus(LPMEMORYSTATUS lpBuffer)
    static private Callback.Handler GlobalMemoryStatus = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.GlobalMemoryStatus";
        }
        public void onCall() {
            int lpBuffer = CPU.CPU_Pop32();
            IntRef free = new IntRef(0);
            IntRef used = new IntRef(0);
            WinSystem.memory.getInfo(free, used); // in 4k pages
            int physTotal = (free.value+used.value)*4096;
            int physFree = free.value*4096;

            Memory.mem_writed(lpBuffer, 32); lpBuffer+=4; // dwLength
            Memory.mem_writed(lpBuffer, physFree*100/physTotal); lpBuffer+=4; // dwMemoryLoad
            Memory.mem_writed(lpBuffer, physTotal); lpBuffer+=4; // dwTotalPhys
            Memory.mem_writed(lpBuffer, physFree); lpBuffer+=4; // dwAvailPhys
            Memory.mem_writed(lpBuffer, physTotal); lpBuffer+=4; // dwTotalPageFile
            Memory.mem_writed(lpBuffer, physFree); lpBuffer+=4; // dwAvailPageFile
            Memory.mem_writed(lpBuffer, 2147483644); lpBuffer+=4; // dwTotalVirtual
            Memory.mem_writed(lpBuffer, 2147483644-used.value*4096); lpBuffer+=4; // dwAvailVirtual
        }
    };

    // LPVOID WINAPI HeapAlloc(HANDLE hHeap, DWORD dwFlags, SIZE_T dwBytes)
    private Callback.Handler HeapAlloc = new HandlerBase() {
        static final int HEAP_GENERATE_EXCEPTIONS = 0x00000004;
        static final int HEAP_NO_SERIALIZE = 0x00000001;
        static final int HEAP_ZERO_MEMORY = 0x00000008;

        public java.lang.String getName() {
            return "Kernel32.HeapAlloc";
        }
        public void onCall() {
            int hHeap = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int dwBytes = CPU.CPU_Pop32();
            if ((dwFlags & HEAP_GENERATE_EXCEPTIONS)!=0) {
                System.out.println(getName()+" option HEAP_GENERATE_EXCEPTIONS not implemented yet");
                //Win.exit();
            }
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getWinHeap().allocateHeap(hHeap, dwBytes);
            if ((dwFlags & HEAP_ZERO_MEMORY)!=0) {
                Memory.mem_zero(CPU_Regs.reg_eax.dword, dwBytes);
            }
        }
    };

    // HANDLE WINAPI HeapCreate(DWORD flOptions, SIZE_T dwInitialSize, SIZE_T dwMaximumSize)
    private Callback.Handler HeapCreate = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.HeapCreate";
        }
        public void onCall() {
            int flOptions = CPU.CPU_Pop32();
            int dwInitialSize = (CPU.CPU_Pop32() + Paging.MEM_PAGE_SIZE) & (Paging.MEM_PAGE_SIZE-1);
            int dwMaximumSize = (CPU.CPU_Pop32() + Paging.MEM_PAGE_SIZE) & (Paging.MEM_PAGE_SIZE-1);
            if ((flOptions & HEAP_GENERATE_EXCEPTIONS)!=0) {
                System.out.println(getName()+" option HEAP_GENERATE_EXCEPTIONS not implemented yet");
            }
            if (dwInitialSize==0)
                dwInitialSize = Paging.MEM_PAGE_SIZE;
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getWinHeap().createHeap(dwInitialSize, dwMaximumSize);
        }
    };

    // BOOL WINAPI HeapDestroy(HANDLE hHeap)
    static private Callback.Handler HeapDestroy = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.HeapDestroy";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL WINAPI HeapFree(HANDLE hHeap, DWORD dwFlags, LPVOID lpMem)
    private Callback.Handler HeapFree = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.HeapFree";
        }
        public void onCall() {
            int hHeap = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpMem = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getWinHeap().freeHeap(hHeap, lpMem);
        }
    };

    //LPVOID WINAPI HeapReAlloc(HANDLE hHeap, DWORD dwFlags, LPVOID lpMem, SIZE_T dwBytes)
    static private Callback.Handler HeapReAlloc = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.HeapReAlloc";
        }
        public void onCall() {
            int hHeap = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpMem = CPU.CPU_Pop32();
            int dwBytes = CPU.CPU_Pop32();
            if ((dwFlags & HEAP_GENERATE_EXCEPTIONS)!=0) {
                System.out.println(getName()+" option HEAP_GENERATE_EXCEPTIONS not implemented yet");
            }
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getWinHeap().realloc(hHeap, lpMem, dwBytes, (dwFlags & HEAP_ZERO_MEMORY) != 0);
        }
    };

    // SIZE_T WINAPI HeapSize(HANDLE hHeap, DWORD dwFlags, LPCVOID lpMem)
    static private Callback.Handler HeapSize = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.HeapSize";
        }
        public void onCall() {
            int hHeap = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpMem = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().getWinHeap().heapSize(hHeap, lpMem);
        }
    };

    // BOOL WINAPI HeapValidate(HANDLE hHeap, DWORD dwFlags, LPCVOID lpMem)
    private Callback.Handler HeapValidate = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.HeapValidate";
        }
        public void onCall() {
            int heap = CPU.CPU_Pop32();
            int flags = CPU.CPU_Pop32();
            int address = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().validateHeap(heap, flags, address);
        }
    };

    // LONG WINAPI _hread( HFILE hFile, LPVOID buffer, LONG count)
    private Callback.Handler _hread = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32._hread";
        }
        public int processReturn() {
            int handle = CPU.CPU_Pop32();
            int buffer = CPU.CPU_Pop32();
            int count = CPU.CPU_Pop32();
            WinFile file = WinFile.get(handle);
            if (file == null) {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return -1;
            }
            return file.read(buffer, count);
        }
    };

    // void WINAPI InitializeCriticalSection(LPCRITICAL_SECTION lpCriticalSection)
    private Callback.Handler InitializeCriticalSection = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.InitializeCriticalSection";
        }
        public void onCall() {
            WinCriticalException.initialize(CPU.CPU_Pop32(), 0);
        }
    };

    // BOOL WINAPI InitializeCriticalSectionAndSpinCount(LPCRITICAL_SECTION lpCriticalSection, DWORD dwSpinCount)
    private Callback.Handler InitializeCriticalSectionAndSpinCount = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.InitializeCriticalSectionAndSpinCount";
        }
        public void onCall() {
            int address = CPU.CPU_Pop32();
            int spinCount = CPU.CPU_Pop32();
            WinCriticalException.initialize(address, spinCount);
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // LONG InterlockedDecrement(LONG volatile *Addend)
    static private Callback.Handler InterlockedDecrement = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.InterlockedDecrement";
        }
        public void onCall() {
            int address = CPU.CPU_Pop32();
            int value = Memory.mem_readd(address);
            value--;
            Memory.mem_writed(address, value);
            CPU_Regs.reg_eax.dword = value;
        }
    };

    // LONG InterlockedExchange(LONG volatile *Target, LONG Value)
    static private Callback.Handler InterlockedExchange = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.InterlockedExchange";
        }
        public int processReturn() {
            int Target = CPU.CPU_Pop32();
            int Value = CPU.CPU_Pop32();
            int result = readd(Target);
            writed(Target, Value);
            return result;
        }
    };

    // LONG InterlockedIncrement(LONG volatile *Addend)
    static private Callback.Handler InterlockedIncrement = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.InterlockedIncrement";
        }
        public void onCall() {
            int address = CPU.CPU_Pop32();
            int value = Memory.mem_readd(address);
            value++;
            Memory.mem_writed(address, value);
            CPU_Regs.reg_eax.dword = value;
        }
    };

    // BOOL WINAPI IsBadReadPtr(const VOID *lp, UINT_PTR ucb)
    static private Callback.Handler IsBadReadPtr = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.IsBadReadPtr";
        }
        public int processReturn() {
            int lp = CPU.CPU_Pop32();
            int ucb = CPU.CPU_Pop32();
            int size = lp & 0xFFF;
            while (ucb>0) {
                int page  = WinSystem.getCurrentProcess().kernelMemory.get_page(lp & 0xFFFFF000, false, WinSystem.getCurrentProcess().page_directory);
                if (page == 0)
                    return TRUE;
                lp+=size;
                ucb-=size;
                size=0xFFF;
            }
            return FALSE;
        }
    };
    static private Callback.Handler IsBadWritePtr = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.IsBadWritePtr";
        }
        public int processReturn() {
            int lp = CPU.CPU_Pop32();
            int ucb = CPU.CPU_Pop32();
            int size = lp & 0xFFF;
            while (ucb>0) {
                int page  = WinSystem.getCurrentProcess().kernelMemory.get_page(lp & 0xFFFFF000, false, WinSystem.getCurrentProcess().page_directory);
                if (page == 0)
                    return TRUE;
                lp+=size;
                ucb-=size;
                size=0xFFF;
            }
            return FALSE;
        }
    };

    // BOOL WINAPI IsDebuggerPresent(void)
    static private Callback.Handler IsDebuggerPresent = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.IsDebuggerPresent";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = WinAPI.FALSE;
        }
    };

    // BOOL IsValidCodePage(UINT CodePage)
    static private Callback.Handler IsValidCodePage = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.IsValidCodePage";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL IsValidLocale(LCID Locale, DWORD dwFlags)
    static private Callback.Handler IsValidLocale = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.IsValidLocale";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // Direct port of Wine's function
    //
    // int LCMapString(LCID Locale, DWORD dwMapFlags, LPCTSTR lpSrcStr, int cchSrc, LPTSTR lpDestStr, int cchDest)
    static private Callback.Handler LCMapStringA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.LCMapStringA";
        }
        public void onCall() {
            notImplemented();
        }
    };
    private Callback.Handler LCMapStringW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.LCMapStringW";
        }
        public void onCall() {
            int lcid = CPU.CPU_Pop32();
            int flags = CPU.CPU_Pop32();
            int src = CPU.CPU_Pop32();
            int srclen = CPU.CPU_Pop32();
            int dst = CPU.CPU_Pop32();
            int dstlen = CPU.CPU_Pop32();
            int dst_ptr;

            if (src==0 || srclen==0 || dstlen < 0)
            {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }

            /* mutually exclusive flags */
            if ((flags & (Locale.LCMAP_LOWERCASE | Locale.LCMAP_UPPERCASE)) == (Locale.LCMAP_LOWERCASE | Locale.LCMAP_UPPERCASE) ||
                (flags & (Locale.LCMAP_HIRAGANA | Locale.LCMAP_KATAKANA)) == (Locale.LCMAP_HIRAGANA | Locale.LCMAP_KATAKANA) ||
                (flags & (Locale.LCMAP_HALFWIDTH | Locale.LCMAP_FULLWIDTH)) == (Locale.LCMAP_HALFWIDTH | Locale.LCMAP_FULLWIDTH) ||
                (flags & (Locale.LCMAP_TRADITIONAL_CHINESE | Locale.LCMAP_SIMPLIFIED_CHINESE)) == (Locale.LCMAP_TRADITIONAL_CHINESE | Locale.LCMAP_SIMPLIFIED_CHINESE))
            {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_FLAGS);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }

            if (dstlen==0) dst = 0;

            if ((flags & Locale.LCMAP_SORTKEY)!=0)
            {
                int ret = 0;
                if (src == dst)
                {
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_FLAGS);
                    CPU_Regs.reg_eax.dword = 0;
                    return;
                }

                if (srclen < 0) srclen = StringUtil.strlenW(src);

                Console.out(getName()+" LCMAP_SORTKEY not implemented yet");
                notImplemented();
                // ret = wine_get_sortkey(flags, src, srclen, (char *)dst, dstlen);
                if (ret == 0) {
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_INSUFFICIENT_BUFFER);
                } else {
                    ret++;
                }
                CPU_Regs.reg_eax.dword = 0;
                return;
            }

            /* SORT_STRINGSORT must be used exclusively with LCMAP_SORTKEY */
            if ((flags & Locale.SORT_STRINGSORT)!=0)
            {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_FLAGS);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }

            if (srclen < 0) srclen = StringUtil.strlenW(src) + 1;

            if (dst==0) /* return required string length */
            {
                int len;

                for (len = 0; srclen!=0; src+=2, srclen--)
                {
                    char wch = (char)Memory.mem_readw(src);
                    /* tests show that win2k just ignores NORM_IGNORENONSPACE,
                     * and skips white space and punctuation characters for
                     * NORM_IGNORESYMBOLS.
                     */
                    if ((flags & Locale.NORM_IGNORESYMBOLS)!=0 && (Unicode.get_char_typeW(wch) & (Unicode.C1_PUNCT | Unicode.C1_SPACE))!=0)
                        continue;
                    len++;
                }
                CPU_Regs.reg_eax.dword = len;
                return;
            }

            if ((flags & Locale.LCMAP_UPPERCASE)!=0)
            {
                for (dst_ptr = dst; srclen!=0 && dstlen!=0; src+=2, srclen--)
                {
                    char wch = (char)Memory.mem_readw(src);
                    if ((flags & Locale.NORM_IGNORESYMBOLS)!=0 && (Unicode.get_char_typeW(wch) & (Unicode.C1_PUNCT | Unicode.C1_SPACE))!=0)
                        continue;
                    Memory.mem_writew(dst_ptr, StringUtil.toupperW(wch));
                    dst_ptr+=2;
                    dstlen--;
                }
            }
            else if ((flags & Locale.LCMAP_LOWERCASE)!=0)
            {
                for (dst_ptr = dst; srclen!=0 && dstlen!=0; src+=2, srclen--)
                {
                    char wch = (char)Memory.mem_readw(src);
                    if ((flags & Locale.NORM_IGNORESYMBOLS)!=0 && (Unicode.get_char_typeW(wch) & (Unicode.C1_PUNCT | Unicode.C1_SPACE))!=0)
                        continue;

                    Memory.mem_writew(dst_ptr, StringUtil.tolowerW(wch));
                    dst_ptr+=2;
                    dstlen--;
                }
            }
            else
            {
                if (src == dst)
                {
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_FLAGS);
                    CPU_Regs.reg_eax.dword = 0;
                    return;
                }
                for (dst_ptr = dst; srclen!=0 && dstlen!=0; src+=2, srclen--)
                {
                    char wch = (char)Memory.mem_readw(src);
                    if ((flags & Locale.NORM_IGNORESYMBOLS)!=0 && (Unicode.get_char_typeW(wch) & (Unicode.C1_PUNCT | Unicode.C1_SPACE))!=0)
                        continue;
                    Memory.mem_writew(dst_ptr, wch);
                    dst_ptr+=2;
                    dstlen--;
                }
            }

            if (srclen>0)
            {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INSUFFICIENT_BUFFER);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }

            CPU_Regs.reg_eax.dword = dst_ptr - dst;
        }
    };

    // void WINAPI LeaveCriticalSection(LPCRITICAL_SECTION lpCriticalSection)
    private Callback.Handler LeaveCriticalSection = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.LeaveCriticalSection";
        }
        public void onCall() {
            WinCriticalException.leave(CPU.CPU_Pop32());
        }
    };

    // HFILE WINAPI _lclose(HFILE hFile)
    private Callback.Handler _lclose = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32._lclose";
        }
        public int processReturn() {
            int hFile = CPU.CPU_Pop32();
            WinFile object = WinFile.get(hFile);
            if (object == null) {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return -1;
            }
            object.close();
            return 0;
        }
    };

    // LONG _llseek(HFile hFile, LONG lOffset, int nOrigin)
    private Callback.Handler _llseek = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32._llseek";
        }
        public int processReturn() {
            int hFile = CPU.CPU_Pop32();
            int lOffset = CPU.CPU_Pop32();
            int nOrigin = CPU.CPU_Pop32();

            WinFile file = WinFile.get(hFile);
            if (file == null) {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return -1;
            }
            return (int)file.seek(lOffset, nOrigin);
        }
    };

    // UINT WINAPI _lread( HFILE handle, LPVOID buffer, UINT count )
    private Callback.Handler _lread = new ReturnHandlerBase() {
        public java.lang.String getName() {
            return "Kernel32._lread";
        }
        public int processReturn() {
            int handle = CPU.CPU_Pop32();
            int buffer = CPU.CPU_Pop32();
            int count = CPU.CPU_Pop32();
            WinFile file = WinFile.get(handle);
            if (file == null) {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                return -1;
            }
            return file.read(buffer, count);
        }
    };

    // HMODULE WINAPI LoadLibrary(LPCTSTR lpFileName)
    private Callback.Handler LoadLibraryA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.LoadLibraryA";
        }
        public void onCall() {
            String name = new LittleEndianFile(CPU.CPU_Pop32()).readCString();
            log("name="+name);
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().loadModule(name);
        }
    };

    static private Callback.Handler LoadLibraryW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.LoadLibraryW";
        }
        public void onCall() {
            String name = new LittleEndianFile(CPU.CPU_Pop32()).readCStringW();
            CPU_Regs.reg_eax.dword = WinSystem.getCurrentProcess().loadModule(name);
        }
    };

    // LPTSTR WINAPI lstrcpy(LPTSTR lpString1, LPTSTR lpString2)
    static private Callback.Handler lstrcpyA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.lstrcpyA";
        }
        public void onCall() {
            int lpString1 = CPU.CPU_Pop32();
            int lpString2 = CPU.CPU_Pop32();
            StringUtil.strcpy(lpString1, lpString2);
            CPU_Regs.reg_eax.dword = lpString1;
        }
    };

    // int WINAPI lstrlen(LPCTSTR lpString)
    static private Callback.Handler lstrlenA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.lstrlenA";
        }
        public void onCall() {
            int lpString = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = StringUtil.strlenA(lpString);
        }
    };
    static private Callback.Handler lstrlenW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.lstrlenW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // LPVOID WINAPI MapViewOfFile(HANDLE hFileMappingObject, DWORD dwDesiredAccess, DWORD dwFileOffsetHigh, DWORD dwFileOffsetLow, SIZE_T dwNumberOfBytesToMap)
    private Callback.Handler MapViewOfFile = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.MapViewOfFile";
        }
        public void onCall() {
            int hFileMappingObject = CPU.CPU_Pop32();
            int dwDesiredAccess = CPU.CPU_Pop32(); // 0x01 Query, 0x02 Write, 0x04 Read, 0x08 Write, 0x10 Extend
            int dwFileOffsetHigh = CPU.CPU_Pop32();
            int dwFileOffsetLow = CPU.CPU_Pop32();
            int dwNumberOfBytesToMap = CPU.CPU_Pop32();
            WinFileMapping mapping = WinFileMapping.get(hFileMappingObject);
            if (mapping == null) {
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                CPU_Regs.reg_eax.dword = 0;
                return;
            }
            CPU_Regs.reg_eax.dword = mapping.map((int)(dwFileOffsetLow | (long)dwFileOffsetHigh << 32), dwNumberOfBytesToMap, (dwDesiredAccess & 0x02) != 0);
        }
    };

    // int MulDiv(int nNumber, int nNumerator, int nDenominator)
    private Callback.Handler MulDiv = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.MulDiv";
        }
        public void onCall() {
            int nNumber = CPU.CPU_Pop32();
            int nNumerator = CPU.CPU_Pop32();
            int nDenominator = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = (int)((long)nNumber*nNumerator/nDenominator);
        }
    };

    // int MultiByteToWideChar(UINT CodePage, DWORD dwFlags, LPCSTR lpMultiByteStr, int cbMultiByte, LPWSTR lpWideCharStr, int cchWideChar)
    private Callback.Handler MultiByteToWideChar = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.MultiByteToWideChar";
        }
        public void onCall() {
            int CodePage = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpMultiByteStr = CPU.CPU_Pop32();
            int cbMultiByte = CPU.CPU_Pop32();
            int lpWideCharStr = CPU.CPU_Pop32();
            int cchWideChar = CPU.CPU_Pop32();
            switch (CodePage) {
                case 0:
                case 1252:
                    LittleEndianFile file = new LittleEndianFile(lpMultiByteStr, cbMultiByte);
                    java.lang.String result = file.readCString();
                    char[] c = result.toCharArray();
                    if (cchWideChar == 0) {
                        CPU_Regs.reg_eax.dword = c.length+1;
                    } else if (cchWideChar < c.length+1) {
                        CPU_Regs.reg_eax.dword = 0;
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_INSUFFICIENT_BUFFER);
                    } else if (lpWideCharStr == 0) {
                        CPU_Regs.reg_eax.dword = 0;
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                    } else {
                        CPU_Regs.reg_eax.dword = c.length+1;
                        for (int i=0;i<c.length;i++) {
                            Memory.mem_writew(lpWideCharStr, c[i]);
                            lpWideCharStr+=2;
                        }
                        Memory.mem_writew(lpWideCharStr, 0);
                    }
                    break;
                default:
                    Console.out(getName()+" CodePage "+CodePage+" not implemented yet");
                    notImplemented();
                    return;
            }
        }
    };
    
    // void WINAPI OutputDebugString(LPCTSTR lpOutputString)
    static private Callback.Handler OutputDebugStringW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.OutputDebugStringW";
        }
        public void onCall() {
            int lpOutputString = CPU.CPU_Pop32();
            System.out.println(new LittleEndianFile(lpOutputString).readCStringW());
        }
    };
    static private Callback.Handler OutputDebugStringA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.OutputDebugStringA";
        }
        public void onCall() {
            int lpOutputString = CPU.CPU_Pop32();
            System.out.println(new LittleEndianFile(lpOutputString).readCString());
        }
    };

    // BOOL WINAPI QueryPerformanceCounter(LARGE_INTEGER *lpPerformanceCount)
    private Callback.Handler QueryPerformanceCounter = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.QueryPerformanceCounter";
        }
        public void onCall() {
            int add = CPU.CPU_Pop32();
            long time = System.nanoTime() * 21 / 17600; // 1GHz to 1.193182 MHz
            int low = (int)time;
            int high = (int)(time >> 32);
            Memory.mem_writed(add, low);
            Memory.mem_writed(add + 4, high);
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // void WINAPI RaiseException(DWORD dwExceptionCode, DWORD dwExceptionFlags, DWORD nNumberOfArguments, const ULONG_PTR *lpArguments)
    private Callback.Handler RaiseException = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.RaiseException";
        }
        public void onCall() {
            Console.out("RaiseException was called\n");
            Win.exit();
        }
    };

    // BOOL WINAPI ReadFile(HANDLE hFile,LPVOID lpBuffer, DWORD nNumberOfBytesToRead, LPDWORD lpNumberOfBytesRead, LPOVERLAPPED lpOverlapped)
    private Callback.Handler ReadFile = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.ReadFile";
        }
        public void onCall() {
            int hFile = CPU.CPU_Pop32();
            int lpBuffer = CPU.CPU_Pop32();
            int nNumberOfBytesToRead = CPU.CPU_Pop32();
            int lpNumberOfBytesRead = CPU.CPU_Pop32();
            int lpOverlapped = CPU.CPU_Pop32();

            if (lpOverlapped != 0) {
                Win.panic(getName()+" does not support overlapped reads");
            }
            int read = 0;
            WinFile file = WinFile.get(hFile);
            if (file == null) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                read = file.read(lpBuffer, nNumberOfBytesToRead);
                if (read == -1)
                    read = 0;
                if (lpNumberOfBytesRead != 0)
                    Memory.mem_writed(lpNumberOfBytesRead, read);
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // BOOL WINAPI ReleaseMutex(HANDLE hMutex)
    private Callback.Handler ReleaseMutex = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.ReleaseMutex";
        }
        public void onCall() {
            int hMutex = CPU.CPU_Pop32();
            WinMutex mutex = WinMutex.get(hMutex);
            if (mutex == null) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                mutex.release();
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // VOID RtlMoveMemory(VOID UNALIGNED *Destination, const VOID UNALIGNED *Source, SIZE_T Length)
    private Callback.Handler RtlMoveMemory = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.RtlMoveMemory";
        }
        public void onCall() {
            int Destination = CPU.CPU_Pop32();
            int Source = CPU.CPU_Pop32();
            int Length = CPU.CPU_Pop32();
            Memory.mem_memcpy(Destination, Source, Length);
        }
    };

    // void WINAPI RtlUnwind(PVOID TargetFrame, PVOID TargetIp, PEXCEPTION_RECORD ExceptionRecord, PVOID ReturnValue)
    static private Callback.Handler RtlUnwind = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.RtlUnwind";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // VOID RtlZeroMemory(VOID UNALIGNED *Destination, SIZE_T Length)
    private Callback.Handler RtlZeroMemory = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.RtlZeroMemory";
        }
        public void onCall() {
            int Destination = CPU.CPU_Pop32();
            int Length = CPU.CPU_Pop32();
            Memory.mem_zero(Destination, Length);
        }
    };

    // BOOL WINAPI SetConsoleCtrlHandler(PHANDLER_ROUTINE HandlerRoutine, BOOL Add)
    private Callback.Handler SetConsoleCtrlHandler = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.SetConsoleCtrlHandler";
        }
        public void onCall() {
            int HandlerRoutine = CPU.CPU_Pop32();
            int Add = CPU.CPU_Pop32();
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = WinAPI.TRUE;
        }
    };

    // UINT WINAPI SetErrorMode(UINT uMode)
    private Callback.Handler SetErrorMode = new HandlerBase() {
        private int mode = 0;

        public java.lang.String getName() {
            return "Kernel32.SetErrorMode";
        }
        public void onCall() {
            int uMode = CPU.CPU_Pop32();
            int old = mode;
            mode = uMode;
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = old;
        }
    };

    // BOOL WINAPI SetEvent(HANDLE hEvent)
    private Callback.Handler SetEvent = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.SetEvent";
        }
        public void onCall() {
            int hEvent = CPU.CPU_Pop32();
            WinEvent event = WinEvent.get(hEvent);
            if (event == null) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                CPU_Regs.reg_eax.dword = event.set();
            }
        }
    };

    // DWORD WINAPI SetFilePointer(HANDLE hFile, LONG lDistanceToMove, PLONG lpDistanceToMoveHigh, DWORD dwMoveMethod)
    private Callback.Handler SetFilePointer = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.SetFilePointer";
        }
        public void onCall() {
            int hFile = CPU.CPU_Pop32();
            int lDistanceToMove = CPU.CPU_Pop32();
            int lpDistanceToMoveHigh = CPU.CPU_Pop32();
            int dwMoveMethod = CPU.CPU_Pop32();
            WinFile file = WinFile.get(hFile);
            if (file == null) {
                CPU_Regs.reg_eax.dword = -1;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                long pos = lDistanceToMove;
                if (lpDistanceToMoveHigh != 0)
                    pos|=(long)Memory.mem_readd(lpDistanceToMoveHigh) << 32;
                long result = file.seek(pos, dwMoveMethod);
                if (result == -1) {
                    CPU_Regs.reg_eax.dword = -1;
                } else {
                    CPU_Regs.reg_eax.dword = (int)result;
                    if (lpDistanceToMoveHigh != 0) {
                        Memory.mem_writed(lpDistanceToMoveHigh, (int)(result >>> 32));
                    }
                }
            }
        }
    };

    // UINT SetHandleCount(UINT uNumber)
    private Callback.Handler SetHandleCount = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.SetHandleCount";
        }
        public void onCall() {
            // This only did something interesting on Win32s
            CPU_Regs.reg_eax.dword = CPU.CPU_Pop32();
        }
    };

    // void WINAPI SetLastError(DWORD dwErrCode)
    private Callback.Handler SetLastError = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.SetLastError";
        }
        public void onCall() {
            int error = CPU.CPU_Pop32();
            Scheduler.getCurrentThread().setLastError(error);
        }
    };

    // BOOL WINAPI SetStdHandle(DWORD nStdHandle, HANDLE hHandle)
    private Callback.Handler SetStdHandle = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.SetStdHandle";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL WINAPI SetThreadPriority(HANDLE hThread, int nPriority)
    private Callback.Handler SetThreadPriority = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.SetThreadPriority";
        }
        public void onCall() {
            int hThread = CPU.CPU_Pop32();
            int nPriority = CPU.CPU_Pop32();
            WinThread thread = WinThread.get(hThread);
            if (thread == null) {
                CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
            } else {
                thread.setPriority(nPriority);
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            }
        }
    };

    // LPTOP_LEVEL_EXCEPTION_FILTER WINAPI SetUnhandledExceptionFilter(LPTOP_LEVEL_EXCEPTION_FILTER lpTopLevelExceptionFilter)
    private Callback.Handler SetUnhandledExceptionFilter = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.SetUnhandledExceptionFilter";
        }
        public void onCall() {
            CPU.CPU_Pop32();
            System.out.println(getName()+" faked");
            CPU_Regs.reg_eax.dword = 0;
        }
    };

    // VOID WINAPI Sleep(DWORD dwMilliseconds)
    private Callback.Handler Sleep = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.Sleep";
        }
        public void onCall() {
            int dwMilliseconds = CPU.CPU_Pop32();
            Scheduler.getCurrentThread().sleep(dwMilliseconds);
        }
    };

    // BOOL WINAPI TerminateProcess(HANDLE hProcess, UINT uExitCode)
    static private Callback.Handler TerminateProcess = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.TerminateProcess";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // DWORD WINAPI TlsAlloc(void)
    private Callback.Handler TlsAlloc = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.TlsAlloc";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = Scheduler.getCurrentThread().tlsAlloc();
        }
    };

    // BOOL WINAPI TlsFree(DWORD dwTlsIndex)
    private Callback.Handler TlsFree = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.TlsFree";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = Scheduler.getCurrentThread().tlsFree(CPU.CPU_Pop32());
        }
    };

    // LPVOID WINAPI TlsGetValue(DWORD dwTlsIndex)
    private Callback.Handler TlsGetValue = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.TlsGetValue";
        }
        public void onCall() {
            CPU_Regs.reg_eax.dword = Scheduler.getCurrentThread().tlsGetValue(CPU.CPU_Pop32());
        }
    };

    // BOOL WINAPI TlsSetValue(DWORD dwTlsIndex, LPVOID lpTlsValue)
    private Callback.Handler TlsSetValue = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.TlsSetValue";
        }
        public void onCall() {
            int index = CPU.CPU_Pop32();
            int value = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = Scheduler.getCurrentThread().tlsSetValue(index, value);
        }
    };

    // LONG WINAPI UnhandledExceptionFilter(struct _EXCEPTION_POINTERS *ExceptionInfo)
    private Callback.Handler UnhandledExceptionFilter = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.UnhandledExceptionFilter";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL WINAPI UnmapViewOfFile(LPCVOID lpBaseAddress)
    private Callback.Handler UnmapViewOfFile = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.UnmapViewOfFile";
        }
        public void onCall() {
            int lpBaseAddress = CPU.CPU_Pop32();
            CPU_Regs.reg_eax.dword = WinFileMapping.unmap(lpBaseAddress)?WinAPI.TRUE:WinAPI.FALSE;
        }
    };

    // LPVOID WINAPI VirtualAlloc(LPVOID lpAddress, SIZE_T dwSize, DWORD flAllocationType, DWORD flProtect)
    private Callback.Handler VirtualAlloc = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.VirtualAlloc";
        }
        public void onCall() {
            long address = CPU.CPU_Pop32() & 0xFFFFFFFFl;
            int size = CPU.CPU_Pop32();
            int flags = CPU.CPU_Pop32();
            int protect = CPU.CPU_Pop32();

            if (address == 1) {
                Win.panic("VirtualAlloc requested DOS memory area, this is not supported yet");
            }
            if (address != 0 && (address < 0x1000 || address + size < address)) {
                SetLastError(Error.ERROR_INVALID_PARAMETER);
                CPU_Regs.reg_eax.dword = 0;
            } else {
                if ((flags & MEM_RESERVE)!=0 || address == 0) {
                    address &= ~0xFFF;
                    if (address == 0)
                        address = (int)WinProcess.ADDRESS_EXTRA_START;
                    long result = WinSystem.getCurrentProcess().addressSpace.getNextAddress(address & 0xFFFFFFFFl, size, true);
                    if (result == 0) {
                        CPU_Regs.reg_eax.dword = 0;
                        return;
                    }
                    if (WinSystem.getCurrentProcess().addressSpace.alloc(result, size) != result) {
                        CPU_Regs.reg_eax.dword = 0;
                        return;
                    }
                    address = result;
                    WinSystem.getCurrentProcess().virtualMemory.add(new VirtualMemory(address, size));
                }
                if ((flags & MEM_COMMIT)!=0) {
                    address &= ~0xFFF;
                    // Must be reserved first
                    VirtualMemory memory = WinSystem.getCurrentProcess().getVirtualMemory(address);
                    if (memory == null) {
                        SetLastError(Error.ERROR_INVALID_ADDRESS);
                        CPU_Regs.reg_eax.dword = 0;
                        return;
                    }
                    memory.commit(address, size);
                }
            }
            CPU_Regs.reg_eax.dword = (int)address;
        }
    };

    // BOOL WINAPI VirtualFree(LPVOID lpAddress, SIZE_T dwSize, DWORD dwFreeType)
    private Callback.Handler VirtualFree = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.VirtualFree";
        }
        public void onCall() {
            int address = CPU.CPU_Pop32();
            int dwSize = CPU.CPU_Pop32();
            int dwFreeType = CPU.CPU_Pop32();
            VirtualMemory memory = WinSystem.getCurrentProcess().getVirtualMemory(address);
            if (memory == null) {
                SetLastError(Error.ERROR_INVALID_ADDRESS);
                CPU_Regs.reg_eax.dword = FALSE;
                return;
            }
            if ((dwFreeType & MEM_RELEASE)!=0) {
                memory.free();
                WinSystem.getCurrentProcess().addressSpace.free(address);
                WinSystem.getCurrentProcess().virtualMemory.remove(memory);
            } else {
                memory.decommit(address, dwSize);
            }
            CPU_Regs.reg_eax.dword = TRUE;
        }
    };

    // SIZE_T WINAPI VirtualQuery(LPCVOID lpAddress, PMEMORY_BASIC_INFORMATION lpBuffer, SIZE_T dwLength)
    private Callback.Handler VirtualQuery = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.VirtualQuery";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL WINAPI WriteConsole(HANDLE hConsoleOutput, const VOID *lpBuffer, DWORD nNumberOfCharsToWrite, LPDWORD lpNumberOfCharsWritten, LPVOID lpReserved)
    private Callback.Handler WriteConsoleA = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.WriteConsoleA";
        }
        public void onCall() {
            notImplemented();
        }
    };
    private Callback.Handler WriteConsoleW = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.WriteConsoleW";
        }
        public void onCall() {
            notImplemented();
        }
    };

    // BOOL WINAPI WriteFile(HANDLE hFile, LPCVOID lpBuffer, DWORD nNumberOfBytesToWrite, LPDWORD lpNumberOfBytesWritten, LPOVERLAPPED lpOverlapped)
    static private Callback.Handler WriteFile = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.WriteFile";
        }
        public void onCall() {
            int hFile = CPU.CPU_Pop32();
            int lpBuffer = CPU.CPU_Pop32();
            int nNumberOfBytesToWrite = CPU.CPU_Pop32();
            int lpNumberOfBytesWritten = CPU.CPU_Pop32();
            int lpOverlapped = CPU.CPU_Pop32();
            if (hFile == WinFile.STD_OUT || hFile == WinFile.STD_ERROR) {
                byte[] buffer = new byte[nNumberOfBytesToWrite];
                Memory.mem_memcpy(buffer, 0, lpBuffer, nNumberOfBytesToWrite);
                Console.out(new java.lang.String(buffer));
                if (lpNumberOfBytesWritten != 0)
                    Memory.mem_writed(lpNumberOfBytesWritten, nNumberOfBytesToWrite);
                CPU_Regs.reg_eax.dword = WinAPI.TRUE;
            } else {
                WinFile file = WinFile.get(hFile);
                if (file == null) {
                    CPU_Regs.reg_eax.dword = WinAPI.FALSE;
                    Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_HANDLE);
                } else {
                    if (lpOverlapped!=0)
                        Win.panic(getName()+" overlapped option not supported yet");
                    int written = file.write(lpBuffer, nNumberOfBytesToWrite);
                    if (lpNumberOfBytesWritten != 0)
                        Memory.mem_writed(lpNumberOfBytesWritten, written);
                    CPU_Regs.reg_eax.dword = WinAPI.TRUE;
                }
            }
        }
    };

    static private boolean[] c1252 = new boolean[] {true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,

                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,

                                                    false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, true,
                                                    true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,

                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
                                                    true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true};

    // int WideCharToMultiByte(UINT CodePage, DWORD dwFlags, LPCWSTR lpWideCharStr, int cchWideChar, LPSTR lpMultiByteStr, int cbMultiByte, LPCSTR lpDefaultChar, LPBOOL lpUsedDefaultChar)
    private Callback.Handler WideCharToMultiByte = new HandlerBase() {
        public java.lang.String getName() {
            return "Kernel32.WideCharToMultiByte";
        }
        public void onCall() {
            int CodePage = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpWideCharStr = CPU.CPU_Pop32();
            int cchWideChar = CPU.CPU_Pop32();
            int lpMultiByteStr = CPU.CPU_Pop32();
            int cbMultiByte = CPU.CPU_Pop32();
            int lpDefaultChar = CPU.CPU_Pop32();
            int lpUsedDefaultChar = CPU.CPU_Pop32();
            switch (CodePage) {
                case 0: // CP_ACP
                case 1252:
                    if (cbMultiByte == 0) {
                        CPU_Regs.reg_eax.dword = cchWideChar;
                    } else if (cchWideChar == 0 || lpWideCharStr == 0) {
                        CPU_Regs.reg_eax.dword = 0;
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_INVALID_PARAMETER);
                    } else if (cbMultiByte < cchWideChar) {
                        CPU_Regs.reg_eax.dword = 0;
                        Scheduler.getCurrentThread().setLastError(Error.ERROR_INSUFFICIENT_BUFFER);
                    } else {
                        byte defaultChar = 63;
                        int defaultCount = 0;
                        if (lpDefaultChar != 0)
                            defaultChar = (byte)Memory.mem_readb(lpDefaultChar);
                        for (int i=0;i<cchWideChar;i++) {
                            int c = Memory.mem_readw(lpWideCharStr);
                            lpWideCharStr+=2;
                            if (c>=c1252.length || !c1252[c])  {
                                c = defaultChar;
                                defaultCount++;
                            }
                            Memory.mem_writeb(lpMultiByteStr, c);
                            lpMultiByteStr+=1;
                        }
                        if (lpUsedDefaultChar != 0)
                            Memory.mem_writed(lpUsedDefaultChar, defaultCount==0?WinAPI.FALSE:WinAPI.TRUE);
                        CPU_Regs.reg_eax.dword = cchWideChar;
                    }
                    break;
                default:
                    Console.out(getName()+" CodePage "+CodePage+" not implemented yet");
                    notImplemented();
            }
        }
    };
}
