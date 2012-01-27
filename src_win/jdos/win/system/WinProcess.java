package jdos.win.system;

import jdos.win.builtin.WinAPI;
import jdos.win.controls.StaticWindow;
import jdos.win.kernel.KernelHeap;
import jdos.win.kernel.KernelMemory;
import jdos.win.loader.Loader;
import jdos.win.loader.Module;
import jdos.win.loader.NativeModule;
import jdos.win.utils.Error;
import jdos.win.utils.Heap;
import jdos.win.utils.Path;
import jdos.win.utils.StringUtil;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class WinProcess extends WaitObject {
    static public WinProcess create(String path, String commandLine, Vector paths, String workingDirectory) {
        WinProcess currentProcess = WinSystem.getCurrentProcess();
        WinProcess process = new WinProcess(nextObjectId(), WinSystem.memory, workingDirectory);
        process.switchPageDirectory();

        if (!process.load(path, commandLine, paths)) {
            process.close();
            return null;
        }
        if (currentProcess != null) {
            currentProcess.switchPageDirectory();
        }
        return process;
    }

    static public WinProcess get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinProcess))
            return null;
        return (WinProcess)object;
    }

    public static final long ADDRESS_HEAP_START =           0x0BA00000l;
    public static final long ADDRESS_HEAP_END =             0x0FFFF000l;
    public static final long ADDRESS_KHEAP_START =          0x90000000l;
    public static final long ADDRESS_KHEAP_END =            0xA0000000l;
    public static final long ADDRESS_STACK_START =          0x00100000l;
    public static final long ADDRESS_STACK_END =            0x01000000l;
    public static final long ADDRESS_CALLBACK_START =       0xA4000000l;
    public static final long ADDRESS_CALLBACK_END =         0xA4010000l;
    public static final long ADDRESS_EXTRA_START =          0xB0000000l;
    public static final long ADDRESS_VIDEO_START =          0xE0000000l;
    public static final long ADDRESS_VIDEO_BITMAP_START =   0xE8000000l;

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
    private int[] temp = new int[10];
    public int nextTempIndex = 0;

    public String currentWorkingDirectory;
    public Vector paths;
    public boolean console = true;
    public NativeModule mainModule;
    public int page_directory;
    public KernelMemory kernelMemory;
    public Heap addressSpace = new Heap(0x00100000l, 0xFFF00000l);
    public Hashtable classNames = new Hashtable();
    public WinEvent readyForInput = WinEvent.create(null, true, false);

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
        return new File(((Path)paths.elementAt(0)).nativePath+name);
    }

    public boolean load(String exe, String commandLine, Vector paths) {
        this.paths = paths;
        this.commandLine = commandLine;
        // by now we should be running in this process' memory space
        this.heap = new KernelHeap(kernelMemory, page_directory, ADDRESS_HEAP_START, ADDRESS_HEAP_START+0x1000, ADDRESS_HEAP_END, false, false);
        this.winHeap = new WinHeap(this.heap);
        loader = new Loader(this, kernelMemory, page_directory, paths);
        this.heapHandle = winHeap.createHeap(0, 0);
        memory = new WinMemory(winHeap);

        StaticWindow.registerClass(this);

        env.put("HOMEDRIVE", "C:");
        env.put("NUMBER_OF_PROCESSORS", "1");
        env.put("SystemDrive", "C:");
        env.put("SystemRoot", WinAPI.WIN32_PATH);
        env.put("TEMP", WinAPI.TEMP_PATH);
        env.put("TMP", WinAPI.TEMP_PATH);
        env.put("windir", WinAPI.WIN32_PATH);
        env.put("PATH", "C:\\;"+WinAPI.WIN32_PATH);

        if (loader.loadModule(exe) == null)
            return false;
        return true;
    }

    public WinThread createThread(long startAddress, int stackSizeCommit, int stackSizeReserve) {
        WinThread thread = WinThread.create(this, startAddress, stackSizeCommit, stackSizeReserve, false);
        threads.add(thread);
        return thread;
    }

    public int loadModule(String name) {
        Module module = loader.loadModule(name);
        if (module != null) {
            return module.getHandle();
        }
        Scheduler.getCurrentThread().setLastError(jdos.win.utils.Error.ERROR_MOD_NOT_FOUND);
        return 0;
    }

    public int getTemp(int size) {
        int index = nextTempIndex;
        nextTempIndex+=2;
        if (nextTempIndex>=temp.length) {
            int[] i = new int[temp.length*2];
            System.arraycopy(temp, 0, i, 0, temp.length);
            temp = i;
        }
        if (temp[index]<size) {
            if (temp[index+1]!=0)
                heap.free(temp[index+1]);
            temp[index+1] = heap.alloc(size, false);
            temp[index] = size;
        }
        return temp[index+1];
    }

    public void exit() {
        for (int i=1;i<temp.length;i+=2) {
            if (temp[i]!=0)
                heap.free(temp[i]);
        }
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
        Scheduler.tick();
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
        Scheduler.getCurrentThread().setLastError(Error.ERROR_MOD_NOT_FOUND);
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
