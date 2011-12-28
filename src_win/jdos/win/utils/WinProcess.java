package jdos.win.utils;

import jdos.win.kernel.KernelHeap;
import jdos.win.kernel.KernelMemory;
import jdos.win.loader.Loader;
import jdos.win.loader.Module;
import jdos.win.loader.NativeModule;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class WinProcess extends WaitObject {
    private int nextStackAddress = 0xE0000000;
    private Heap heap;
    private int heapHandle;
    private String commandLine;
    private int commandLineA = 0;
    private int commandLineW = 0;
    private WinMemory memory;
    private int envHandle = 0;
    private int envHandleW = 0;
    private Hashtable env = new Hashtable();
    private Loader loader;
    private Vector threads = new Vector();
    public String currentWorkingDirectory;
    public Vector paths;

    public boolean console = true;
    private NativeModule mainModule;
    public int page_directory;
    public KernelMemory kernelMemory;
    public WinProcess(int handle, KernelMemory memory, String workingDirectory) {
        super(handle);
        page_directory = memory.createNewDirectory();
        this.kernelMemory = memory;
        this.currentWorkingDirectory = workingDirectory;
    }

    public WinThread getMainThread() {
        return (WinThread)threads.elementAt(0);
    }

    public int getStackAddress(int size) {
        nextStackAddress+=size;
        return nextStackAddress;
    }

    public void switchPageDirectory() {
        kernelMemory.switch_page_directory(page_directory);
    }

    public boolean load(String exe, String commandLine, Vector paths) {
        this.paths = paths;
        this.commandLine = commandLine;
        // by now we should be running in this process' memory space
        this.heap = new Heap(new KernelHeap(kernelMemory, page_directory, 0x70000000, 0x70001000, 0x80000000, false, false));
        loader = new Loader(kernelMemory, page_directory, paths);
        mainModule = (NativeModule)loader.loadModule(exe);
        if (mainModule == null)
            return false;

        this.heapHandle = heap.createHeap(0, 0);

        memory = new WinMemory(heap);
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
        heap.deallocate();
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
            envHandle = heap.allocateHeap(heapHandle, s.length()+1);
            StringUtil.strcpy(envHandle, s);
        }
        return envHandle;
    }

    public int validateHeap(int handle, int flags, int address) {
        return heap.validateHeap(handle, flags, address);
    }

    public int getEnvironmentW() {
        if (envHandleW == 0) {
            String s = buildEnvString();
            envHandleW = heap.allocateHeap(heapHandle, (s.length()+1)*2);
            StringUtil.strcpyW(envHandle, s);
        }
        return envHandleW;
    }
    public WinMemory getMemory() {
        return memory;
    }

    public Module getModuleByHandle(int handle) {
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
            commandLineA = heap.allocateHeap(heapHandle, commandLine.length()+1);
            StringUtil.strcpy(commandLineA, commandLine);
        }
        return commandLineA;
    }

    public int getCommandLineW() {
        if (commandLineW == 0) {
            commandLineW = heap.allocateHeap(heapHandle, commandLine.length()+1);
            StringUtil.strcpy(commandLineW, commandLine);
        }
        return commandLineW;
    }
    public Heap getHeap() {
        return heap;
    }
    public int getHeapHandle() {
        return heapHandle;
    }
}
