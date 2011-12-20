package jdos.win.utils;

import jdos.hardware.Memory;
import jdos.win.loader.Loader;
import jdos.win.loader.Module;
import jdos.win.loader.NativeModule;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class WinProcess {
    private Heap heap;
    private int heapHandle;
    private String[] args;
    private int commandLine = 0;
    private int commandLineW = 0;
    private WinMemory memory;
    private int envHandle = 0;
    private int envHandleW = 0;
    private Hashtable env = new Hashtable();
    private Loader loader;

    public boolean console = true;
    private int handle;
    private NativeModule mainModule;

    public boolean load(int handle, String exe, String[] args, Vector paths) {
        loader = new Loader(paths);
        mainModule = (NativeModule)loader.loadModule(exe);
        if (mainModule == null)
            return false;

        this.heap = new Heap(loader.topAddress, Memory.MEM_SIZE*1024*1024-loader.topAddress); // :TODO: we really need virtual memory
        this.handle = handle;
        this.args = args;
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

        WinSystem.createThread(this, mainModule.getEntryPoint());
        return true;
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
        loader.unload();
    }

    public int getHandle() {
        return handle;
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
        if (commandLine == 0) {
            String path = "'"+mainModule.getFileName(true)+"'";
            commandLine = heap.allocateHeap(heapHandle, path.length()+1);
            StringUtil.strcpy(commandLine, path);
        }
        return commandLine;
    }

    public int getCommandLineW() {
        if (commandLineW == 0) {
            String path = mainModule.getFileName(true);
            commandLineW = heap.allocateHeap(heapHandle, path.length()+1);
            StringUtil.strcpy(commandLineW, path);
        }
        return commandLine;
    }
    public Heap getHeap() {
        return heap;
    }
    public int getHeapHandle() {
        return heapHandle;
    }
}
