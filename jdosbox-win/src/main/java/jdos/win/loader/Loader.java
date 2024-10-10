package jdos.win.loader;

import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.StringRef;
import jdos.win.Console;
import jdos.win.Win;
import jdos.win.builtin.*;
import jdos.win.builtin.Msacm32.Msacm32;
import jdos.win.builtin.comctl32.Comctl32;
import jdos.win.builtin.directx.DDraw;
import jdos.win.builtin.directx.DInput;
import jdos.win.builtin.directx.DSound;
import jdos.win.builtin.directx.Dplayx;
import jdos.win.builtin.gdi32.Gdi32;
import jdos.win.builtin.kernel32.Kernel32;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.builtin.kernel32.WinThread;
import jdos.win.builtin.user32.User32;
import jdos.win.builtin.winmm.WinMM;
import jdos.win.kernel.KernelHeap;
import jdos.win.kernel.KernelMemory;
import jdos.win.loader.winpe.HeaderImageImportDescriptor;
import jdos.win.loader.winpe.HeaderImageOptional;
import jdos.win.system.WinSystem;
import jdos.win.utils.Path;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class Loader {
    long nextFunctionAddress = WinProcess.ADDRESS_CALLBACK_START;
    long maxFunctionAddress = WinProcess.ADDRESS_CALLBACK_END;

    public int registerFunction(int cb) {
        if (nextFunctionAddress >= maxFunctionAddress) {
            Log.exit("Need to increase maximum number of function lookups to more than " + (nextFunctionAddress - maxFunctionAddress));
        }
        Memory.mem_writed((int) nextFunctionAddress, (cb << 16) + 0x38FE);
        long result = nextFunctionAddress;
        nextFunctionAddress += 4;
        return (int) result;
    }

    private Hashtable modulesByName = new Hashtable();
    private Hashtable modulesByHandle = new Hashtable();
    private Vector paths;
    public NativeModule main = null;
    private int page_directory;
    private KernelHeap callbackHeap;
    private int nextModuleHandle = 1;
    private WinProcess process;

    public Loader(WinProcess process, KernelMemory memory, int page_directory, Vector paths) {
        this.paths = paths;
        this.process = process;
        this.page_directory = page_directory;
        callbackHeap = new KernelHeap(memory, page_directory, nextFunctionAddress, maxFunctionAddress, maxFunctionAddress, false, true);
    }

    public void unload() {
        Enumeration e = modulesByName.elements();
        while (e.hasMoreElements()) {
            Module module = (Module) e.nextElement();
            module.unload();
        }
        callbackHeap.deallocate();
    }

    private int getNextModuleHandle() {
        return nextModuleHandle++;
    }

    public void attachThread() {
        Enumeration e = modulesByHandle.elements();
        while (e.hasMoreElements()) {
            Module module = (Module)e.nextElement();
            if (module != main &&  module.threadLibraryCalls) {
                module.callDllMain(Module.DLL_THREAD_ATTACH);
            }
        }
    }

    public void detachThread() {
        Enumeration e = modulesByHandle.elements();
        while (e.hasMoreElements()) {
            Module module = (Module)e.nextElement();
            if (module != main && module.threadLibraryCalls) {
                module.callDllMain(Module.DLL_THREAD_DETACH);
            }
        }
    }

    private Module load_native_module(String name) {
        try {
            NativeModule module = new NativeModule(this, getNextModuleHandle());

            for (int i = 0; i < paths.size(); i++) {
                Path path = (Path) paths.elementAt(i);
                if (module.load(process, page_directory, name, path)) {
                    if (main == null) {
                        main = module;
                        // we need to create the main thread as soon as possible so that DllMain can run
                        WinThread thread = WinThread.create(process, module.getEntryPoint(), (int) module.header.imageOptional.SizeOfStackCommit, (int) module.header.imageOptional.SizeOfStackReserve, true);
                        process.threads.add(thread);
                        WinSystem.getCurrentProcess().mainModule = module;
                    }
                    // :TODO: reloc dll
                    modulesByName.put(name.toLowerCase(), module);
                    modulesByHandle.put(new Integer(module.getHandle()), module);
                    if (resolveImports(module)) {
                        if (main != module) {
                            module.callDllMain(Module.DLL_PROCESS_ATTACH);
                        }
                        return module;
                    }
                    modulesByName.remove(name);
                    modulesByHandle.remove(new Integer(module.getHandle()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Module load_builtin_module(String name) {
        BuiltinModule module = null;
        if (name.equalsIgnoreCase("kernel32.dll")) {
            module = new Kernel32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("advapi32.dll")) {
            module = new Advapi32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("user32.dll")) {
            module = new User32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("gdi32.dll")) {
            module = new Gdi32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("shell32.dll")) {
            module = new Shell32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("comdlg32.dll")) {
            module = new Comdlg32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("version.dll")) {
            module = new Version(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("crtdll.dll")) {
            module = new Crtdll(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("ddraw.dll")) {
            module = new DDraw(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("winmm.dll")) {
            module = new WinMM(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("dsound.dll")) {
            module = new DSound(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("dinput.dll")) {
            module = new DInput(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("ole32.dll")) {
            module = new Ole32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("dplayx.dll")) {
            module = new Dplayx(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("imm32.dll")) {
            module = new Imm32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("msvfw32.dll")) {
            module = new Msvfw32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("wsock32.dll")) {
            module = new Wsock32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("comctl32.dll")) {
            module = new Comctl32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("msacm32.dll")) {
            module = new Msacm32(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("winspool.drv")) {
            module = new Winspool(this, getNextModuleHandle());
        } else if (name.equalsIgnoreCase("lz32.dll")) {
            module = new Lz32(this, getNextModuleHandle());
        }
        if (module != null) {
            modulesByName.put(name.toLowerCase(), module);
            modulesByHandle.put(new Integer(module.getHandle()), module);
        }
        return module;
    }

    public Module getModuleByName(String name) {
        return (Module) modulesByName.get(name.toLowerCase());
    }

    public Module getModuleByHandle(int handle) {
        return (Module) modulesByHandle.get(new Integer(handle));
    }

    private Module internalLoadModule(String name) {
        Module result = (Module) modulesByName.get(name.toLowerCase());
        if (result == null)
            result = load_native_module(name);
        if (result == null)
            result = load_builtin_module(name);
        return result;
    }

    public Module loadModule(String name) {
        String path = null;
        int pos = name.lastIndexOf("\\");
        if (pos>=0) {
            path = name.substring(0, pos+1);
            name = name.substring(pos+1);
        }
        // :TODO: currently we only support modules in the path
        return internalLoadModule(name);
    }

    private boolean resolveImports(Module module) throws IOException {
        LongRef address = new LongRef(0);
        LongRef size = new LongRef(0);
        if (module.RtlImageDirectoryEntryToData(HeaderImageOptional.IMAGE_DIRECTORY_ENTRY_IMPORT, address, size)) {
            Vector importDescriptors = module.getImportDescriptors(address.value);
            for (int i = 0; i < importDescriptors.size(); i++) {
                boolean result = importDll(module, (HeaderImageImportDescriptor) importDescriptors.elementAt(i));
                if (!result)
                    return false;
            }

        }
        return true;
    }

    private boolean importDll(Module module, HeaderImageImportDescriptor importDescriptor) throws IOException {
        String name = module.getVirtualString(importDescriptor.Name);
        System.out.println("ImportDll: " + name);
        Module import_module = loadModule(name);
        if (import_module == null) {
            Win.panic("Could not find import: " + name);
            return false;
        }
        LongRef exportAddress = new LongRef(0);
        LongRef exportSize = new LongRef(0);
        if (!import_module.RtlImageDirectoryEntryToData(HeaderImageOptional.IMAGE_DIRECTORY_ENTRY_EXPORT, exportAddress, exportSize)) {
            Console.out(name + ": could not find exports.\n\n");
            return false;
        }
        long[] import_list = module.getImportList(importDescriptor);
        for (int i = 0; i < import_list.length; i++) {
            if ((import_list[i] & 0x80000000l) != 0) {
                int ordinal = (int) import_list[i] & 0xFFFF;
                long thunk = import_module.findOrdinalExport(exportAddress.value, exportSize.value, ordinal);
                if (thunk == 0) {
                    Console.out("Could not find ordinal function " + ordinal + " in " + name + "\n");
                    return false;
                } else {
                    module.writeThunk(importDescriptor, i, thunk);
                }
            } else {
                StringRef functionName = new StringRef();
                IntRef hint = new IntRef(0);
                module.getImportFunctionName(import_list[i], functionName, hint);
                long thunk = import_module.findNameExport(exportAddress.value, exportSize.value, functionName.value, hint.value);
                if (thunk == 0) {
                    Console.out("Could not find " + functionName.value + " in " + name + "\n");
                    return false;
                } else {
                    module.writeThunk(importDescriptor, i, thunk);
                }
            }
        }
        return true;
    }
}
