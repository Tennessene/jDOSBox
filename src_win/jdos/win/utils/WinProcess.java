package jdos.win.utils;

import jdos.win.kernel.KernelHeap;
import jdos.win.kernel.KernelMemory;
import jdos.win.loader.Loader;
import jdos.win.loader.Module;
import jdos.win.loader.NativeModule;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class WinProcess extends WaitObject {
    public static final long ADDRESS_HEAP_START =     0x80000000l;
    public static final long ADDRESS_HEAP_END =       0x90000000l;
    public static final long ADDRESS_KHEAP_START =    0x90000000l;
    public static final long ADDRESS_KHEAP_END =      0xA0000000l;
    public static final long ADDRESS_STACK_START =    0xA0000000l;
    public static final long ADDRESS_STACK_END =      0xA4000000l;
    public static final long ADDRESS_CALLBACK_START = 0xA4000000l;
    public static final long ADDRESS_CALLBACK_END =   0xA4010000l;
    public static final long ADDRESS_EXTRA_START =    0xB0000000l;

    private WinHeap winHeap;
    public KernelHeap heap;
    private int heapHandle;
    private String commandLine;
    private int commandLineA = 0;
    private int commandLineW = 0;
    private WinMemory memory;
    private int envHandle = 0;
    private int envHandleW = 0;
    private Hashtable env = new Hashtable();
    public Loader loader;
    private Vector threads = new Vector();

    public String currentWorkingDirectory;
    public Vector paths;
    public boolean console = true;
    public NativeModule mainModule;
    public int page_directory;
    public KernelMemory kernelMemory;
    public Heap addressSpace = new Heap(0x00100000l, 0xFFF00000l);
    public Hashtable classNames = new Hashtable();

    public WinProcess(int handle, KernelMemory memory, String workingDirectory) {
        super(handle);
        page_directory = memory.createNewDirectory();
        this.kernelMemory = memory;
        this.currentWorkingDirectory = workingDirectory;
    }

    public int reserveStackAddress(int size) {
        long result = addressSpace.getNextAddress(ADDRESS_STACK_START, size, true);
        addressSpace.alloc(result, size);
        return (int)result;
    }

    public int reserveAddress(int size, boolean pageAlign) {
        long result = addressSpace.getNextAddress(ADDRESS_EXTRA_START, size, pageAlign);
        addressSpace.alloc(result, size);
        return (int)result;
    }

    public void freeAddress(int p) {
        addressSpace.free(p);
    }

    public WinThread getMainThread() {
        return (WinThread)threads.elementAt(0);
    }

    public void switchPageDirectory() {
        kernelMemory.switch_page_directory(page_directory);
    }

    public File getFile(String name) {
        // :TODO: add support for relative paths
        for (int i=0;i<paths.size();i++) {
            Path path = (Path)paths.elementAt(i);
            if (name.toLowerCase().startsWith(path.winPath.toLowerCase())) {
                return new File(path.nativePath+name.substring(path.winPath.length()));
            }
        }
        return null;
    }

    public boolean load(String exe, String commandLine, Vector paths) {
        this.paths = paths;
        this.commandLine = commandLine;
        // by now we should be running in this process' memory space
        this.heap = new KernelHeap(kernelMemory, page_directory, ADDRESS_HEAP_START, ADDRESS_HEAP_START+0x1000, ADDRESS_HEAP_END, false, false);
        this.winHeap = new WinHeap(this.heap);
        loader = new Loader(kernelMemory, page_directory, paths);
        mainModule = (NativeModule)loader.loadModule(exe);
        if (mainModule == null)
            return false;

        this.heapHandle = winHeap.createHeap(0, 0);

        memory = new WinMemory(winHeap);
        env.put("HOMEDRIVE", "C:");
        env.put("NUMBER_OF_PROCESSORS", "1");
        env.put("SystemDrive", "C:");
        env.put("SystemRoot", "C:\\WINDOWS");
        env.put("TEMP", "C:\\TEMP");
        env.put("TMP", "C:\\TEMP");
        env.put("windir", "C:\\WINDOWS");
        env.put("PATH", "C:\\;C:\\WINDOWS");

        createThread(mainModule.getEntryPoint(), (int)mainModule.header.imageOptional.SizeOfStackCommit, (int)mainModule.header.imageOptional.SizeOfStackReserve);
        return true;
    }

    public WinThread createThread(long startAddress, int stackSizeCommit, int stackSizeReserve) {
        WinThread thread = WinSystem.createThread(this, startAddress, stackSizeCommit, stackSizeReserve);
        threads.add(thread);
        return thread;
    }

    public int loadModule(String name) {
        Module module = loader.loadModule(name);
        if (module != null) {
            return module.getHandle();
        }
        WinSystem.getCurrentThread().setLastError(Error.ERROR_MOD_NOT_FOUND);
        return 0;
    }

    public void exit() {
        release();
        loader.unload();
        for (int i=0;i<threads.size();i++) {
            WinThread thread = (WinThread)threads.elementAt(i);
            thread.exit(0);
        }
        winHeap.deallocate();
        close();
        // This process is down, and all threads have been removed from the scheduler
        // By scheduling a thread in another process, the page directory will change
        // which is why this has to be done last
        WinSystem.scheduler.tick(false);
    }

    private String buildEnvString() {
        StringBuffer result = new StringBuffer();
        if (env.size() == 0) {
            result.append("\0");
        } else {
            Enumeration e = env.keys();
            while (e.hasMoreElements()) {
                String key = (String)e.nextElement();
                String value = (String)env.get(key);
                result.append(key);
                result.append("=");
                result.append(value);
                result.append("\0");
            }
        }
        result.append("\0");
        return result.toString();
    }
    public int getEnvironment() {
        if (envHandle == 0) {
            String s = buildEnvString();
            envHandle = winHeap.allocateHeap(heapHandle, s.length()+1);
            StringUtil.strcpy(envHandle, s);
        }
        return envHandle;
    }

    public int validateHeap(int handle, int flags, int address) {
        return winHeap.validateHeap(handle, flags, address);
    }

    public int getEnvironmentW() {
        if (envHandleW == 0) {
            String s = buildEnvString();
            envHandleW = winHeap.allocateHeap(heapHandle, (s.length()+1)*2);
            StringUtil.strcpyW(envHandle, s);
        }
        return envHandleW;
    }
    public WinMemory getMemory() {
        return memory;
    }

    public Module getModuleByHandle(int handle) {
        if (handle == 0) handle = 1;
        return loader.getModuleByHandle(handle);
    }

    public int getModuleByName(String name) {
        Module module = loader.getModuleByName(name);
        if (module == null) {
            module = loader.getModuleByName(name+".dll");
        }
        if (module == null) {
            return 0;
        }
        return module.getHandle();
    }

    public int getProcAddress(int handle, String name) {
        Module module = getModuleByHandle(handle);
        if (module != null)
            return module.getProcAddress(name, false);
        WinSystem.getCurrentThread().setLastError(Error.ERROR_MOD_NOT_FOUND);
        return 0;
    }

    public int getCommandLine() {
        if (commandLineA == 0) {
            commandLineA = winHeap.allocateHeap(heapHandle, commandLine.length()+1);
            StringUtil.strcpy(commandLineA, commandLine);
        }
        return commandLineA;
    }

    public int getCommandLineW() {
        if (commandLineW == 0) {
            commandLineW = winHeap.allocateHeap(heapHandle, commandLine.length()+1);
            StringUtil.strcpy(commandLineW, commandLine);
        }
        return commandLineW;
    }
    public WinHeap getWinHeap() {
        return winHeap;
    }
    public int getHeapHandle() {
        return heapHandle;
    }
}
