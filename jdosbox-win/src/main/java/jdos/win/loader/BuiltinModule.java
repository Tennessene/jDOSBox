package jdos.win.loader;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.StringRef;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.builtin.ReturnHandlerBase;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.WinBrush;
import jdos.win.builtin.gdi32.WinGDI;
import jdos.win.kernel.WinCallback;
import jdos.win.loader.winpe.HeaderImageImportDescriptor;
import jdos.win.loader.winpe.HeaderImageOptional;
import jdos.win.system.Scheduler;
import jdos.win.system.WinSystem;
import jdos.win.utils.Ptr;
import jdos.win.utils.StringUtil;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;

public class BuiltinModule extends Module {
    private Hashtable<String, Callback.Handler> functions = new Hashtable<String, Callback.Handler>();
    private String fileName;
    private Hashtable<String, Integer> registeredCallbacks = new Hashtable<String, Integer>();
    public Loader loader;
    private Hashtable<Integer, String> ordinalToName = new Hashtable<Integer, String>();

    public BuiltinModule(Loader loader, String name, int handle) {
        super(handle);
        this.name = name.substring(0, name.lastIndexOf("."));
        this.fileName = name;
        this.loader = loader;
    }

    private static void printParam(Integer value, String desc, Integer[] fullArgs) {
        if (desc.startsWith("(HEX)")) {
            System.out.print(desc.substring(5));
            System.out.print("=");
            System.out.print("0x");
            System.out.print(Ptr.toString(value));
        } else if (desc.startsWith("(STRING)")) {
            System.out.print(desc.substring(8));
            System.out.print("=");
            if (IS_INTRESOURCE(value) || value==0) {
                System.out.print(value.toString());
            } else {
                System.out.print(StringUtil.getString(value));
                System.out.print("(0x");
                System.out.print(Ptr.toString(value));
                System.out.print(")");
            }
        } else if (desc.startsWith("(STRINGW)")) {
            System.out.print(desc.substring(9));
            System.out.print("=");
            if (IS_INTRESOURCE(value) || value==0) {
                System.out.print(value.toString());
            } else {
                System.out.print(StringUtil.getStringW(value));
                System.out.print("(0x");
                System.out.print(Ptr.toString(value));
                System.out.print(")");
            }
        } else if (desc.startsWith("(STRINGN")) {
            System.out.print(desc.substring(10));
            System.out.print("=");
            if (IS_INTRESOURCE(value) || value==0) {
                System.out.print(value.toString());
            } else {
                System.out.print(StringUtil.getString(value, fullArgs[Integer.parseInt(desc.substring(8, 9))]));
                System.out.print("(0x");
                System.out.print(Ptr.toString(value));
                System.out.print(")");
            }
        } else if (desc.startsWith("(BOOL)")) {
            System.out.print(desc.substring(6));
            System.out.print("=");
            if (value == 0)
                System.out.print("FALSE");
            else
                System.out.print("TRUE");
        } else if (desc.startsWith("(BRUSH)")) {
            System.out.print(desc.substring(7));
            System.out.print("=");
            System.out.print(value);
            WinBrush brush = WinBrush.get(value);
            if (brush == null)
                System.out.print("(INVALID)");
            else {
                System.out.print("(");
                System.out.print(brush.toString());
                System.out.print(")");
            }
        } else if (desc.startsWith("(MSG)")) {
            System.out.print(desc.substring(5));
            System.out.print("=");
            if (value == 0)
                System.out.print("NULL");
            else {
                System.out.print("(hWnd=");
                System.out.print(readd(value));
                System.out.print(" msg=0x");
                System.out.print(Ptr.toString(readd(value+4)));
                System.out.print(")@0x");
                System.out.print(Ptr.toString(value));
            }
        } else if (desc.startsWith("(GDI)")) {
            System.out.print(desc.substring(5));
            System.out.print("=");
            System.out.print(value);
            if (value != 0) {
                WinGDI gdi = WinGDI.getGDI(value);
                System.out.print("(");
                if (gdi == null)
                    System.out.print("NULL");
                else {
                    System.out.print(gdi.toString());
                }
                System.out.print(")");
            }
        } else if (desc.startsWith("(CLASS)")) {
            System.out.print(desc.substring(7));
            System.out.print("=");
            if (value == 0)
                System.out.print("NULL");
            else {
                System.out.print("(style=0x");
                System.out.print(Ptr.toString(readd(value)));
                System.out.print(" proc=0x");
                System.out.print(Ptr.toString(readd(value+4)));
                System.out.print(" name=");
                System.out.print(StringUtil.getString(value+36));
                System.out.print(")@0x");
                System.out.print(Ptr.toString(value));
            }
        } else if (desc.startsWith("(GUID)")) {
            System.out.print(desc.substring(6));
            System.out.print("=");
            System.out.print("0x");
            System.out.print(Ptr.toString(value));
        } else if (desc.startsWith("(HRESULT)")) {
            System.out.print(desc.substring(9));
            System.out.print("=");
            if (value == 0)
                System.out.print("S_OK");
            else {
                System.out.print("0x");
                System.out.print(Ptr.toString(value));
            }
        } else if (desc.startsWith("(LOGFONT)")) {
            System.out.print(desc.substring(5));
            System.out.print("=");
            if (value == 0)
                System.out.print("NULL");
            else {
                System.out.print("(height=");
                System.out.print(readd(value));
                System.out.print(" weight=");
                System.out.print(readd(value+16));
                System.out.print(" name=");
                if (readd(value+52)==0)
                    System.out.print("NULL");
                else
                    System.out.print(StringUtil.getString(value+52));
                System.out.print(")@0x");
                System.out.print(Ptr.toString(value));
            }
        } else if (desc.startsWith("(POINT)")) {
            System.out.print(desc.substring(7));
            System.out.print("=");
            if (value == 0)
                System.out.print("NULL");
            else {
                System.out.print("(");
                System.out.print(readd(value));
                System.out.print(",");
                System.out.print(readd(value+4));
                System.out.print(")@0x");
                System.out.print(Ptr.toString(value));
            }
        } else if (desc.startsWith("(SIZE)")) {
            System.out.print(desc.substring(6));
            System.out.print("=");
            if (value == 0)
                System.out.print("NULL");
            else {
                System.out.print("(");
                System.out.print(readd(value));
                System.out.print(",");
                System.out.print(readd(value+4));
                System.out.print(")@0x");
                System.out.print(Ptr.toString(value));
            }
        } else if (desc.startsWith("(RECT)")) {
            System.out.print(desc.substring(6));
            System.out.print("=");
            if (value == 0)
                System.out.print("NULL");
            else {
                System.out.print("(");
                System.out.print(readd(value));
                System.out.print(",");
                System.out.print(readd(value+4));
                System.out.print(")-");
                System.out.print("(");
                System.out.print(readd(value+8));
                System.out.print(",");
                System.out.print(readd(value+12));
                System.out.print(")@0x");
                System.out.print(Ptr.toString(value));
            }
        } else if (desc.startsWith("(TM)")) {
            System.out.print(desc.substring(4));
            System.out.print("=");
            if (value == 0)
                System.out.print("NULL");
            else {
                System.out.print("(height=");
                System.out.print(readd(value));
                System.out.print(" ascent=");
                System.out.print(readd(value+4));
                System.out.print(" descent=");
                System.out.print(readd(value+8));
                System.out.print(" aveCharWidth");
                System.out.print(readd(value+20));
                System.out.print(" maxCharWidth");
                System.out.print(readd(value+24));
                System.out.print(" weight");
                System.out.print(readd(value+28));
                System.out.print(")@0x");
                System.out.print(Ptr.toString(value));
            }
        }else {
            System.out.print(desc);
            System.out.print("=");
            System.out.print(value);
        }
    }

    private static long startTime;
    public static int indent = 0;
    public static boolean inPre = false;
    private static void preLog(String name, Integer[] args, String[] params) {
        startTime = System.currentTimeMillis();
        if (inPre)
            System.out.println();
        inPre = true;
        for (int i=0;i<indent;i++) {
            System.out.print("    ");
        }
        indent++;
        System.out.print(Ptr.toString(CPU_Regs.reg_eip));
        System.out.print(": ");
        System.out.print(name);
        for (int i=0;i<args.length;i++) {
            System.out.print(" ");
            if (params != null && i<params.length) {
                printParam(args[i], params[i], args);
            } else {
                System.out.print(args[i].toString());
            }
        }
    }

    private static void postLog(String name, Integer result, String desc, Integer[] args, String[] params) {
        indent--;
        if (!inPre) {
            for (int i=0;i<indent;i++) {
                System.out.print("    ");
            }
            System.out.print("RETURNED "+name);
        }
        inPre = false;
        if (result != null) {
            if (desc != null) {
                System.out.print(" ");
                printParam(result, desc, null);
            } else {
                System.out.print(" result="+result.toString());
                System.out.print("(");
                System.out.print(Ptr.toString(result));
                System.out.print(")");
            }
        }
        if (params != null && args != null) {
            for (int i=args.length+1;i<params.length;i++) {
                String index = params[i].substring(0, 2);
                System.out.print(" ");
                printParam(args[Integer.parseInt(index)], params[i].substring(2), args);
            }
        }
        System.out.println(" time="+(System.currentTimeMillis()-startTime));
    }
    public static class ReturnHandler extends ReturnHandlerBase {
        Method method;
        Integer[] args;
        String name;
        boolean pop;
        String[] params;

        public ReturnHandler(String name, Method method, boolean pop, String[] params) {
            this.method = method;
            args = new Integer[method.getParameterTypes().length];
            this.name = name;
            this.pop = pop;
            this.params = params;
        }

        public int processReturn() {
            for (int i=0;i<args.length;i++) {
                if (pop)
                    args[i] = CPU.CPU_Pop32();
                else
                    args[i] = CPU.CPU_Peek32(i);
            }
            try {
                if (LOG && params != null)
                    preLog(name, args, params);
                Integer result = (Integer)method.invoke(null, args);
                if (LOG && params != null)
                    postLog(name, result, (params != null && params.length>args.length)?params[args.length]:null, args, params);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                Win.panic(getName() + " failed to execute: " + e.getMessage());
                return 0;
            }
        }

        public String getName() {
            return name;
        }
    }

    public static class NoReturnHandler extends HandlerBase {
        Method method;
        Integer[] args;
        String name;
        boolean pop;
        String[] params;

        public NoReturnHandler(String name, Method method, boolean pop, String params[]) {
            this.method = method;
            args = new Integer[method.getParameterTypes().length];
            this.name = name;
            this.pop = pop;
            this.params = params;
        }

        public void onCall() {
            for (int i=0;i<args.length;i++) {
                if (pop)
                    args[i] = CPU.CPU_Pop32();
                else
                    args[i] = CPU.CPU_Peek32(i);
            }
            try {
                if (LOG && params != null)
                    preLog(name, args, params);
                method.invoke(null, args);
                if (LOG && params != null)
                    postLog(name, null, null, args, params);
            } catch (Exception e) {
                e.printStackTrace();
                Win.panic(getName()+" failed to execute: "+e.getMessage());
            }
        }

        public String getName() {
            return name;
        }
    }

    private static class WaitReturnHandler extends HandlerBase {
        Method method;
        Integer[] args;
        String name;
        boolean pop;
        int eip;
        int esp;
        String[] params;

        public WaitReturnHandler(String name, Method method, boolean pop, String[] params) {
            this.method = method;
            args = new Integer[method.getParameterTypes().length];
            this.name = name;
            this.pop = pop;
            this.params = params;
        }

        public boolean preCall() {
            eip = CPU_Regs.reg_eip-4; // -4 because the callback instruction called SAVEIP
            esp = CPU_Regs.reg_esp.dword;
            return true;
        }

        public void onCall() {
            for (int i=0;i<args.length;i++) {
                if (pop)
                    args[i] = CPU.CPU_Pop32();
                else
                    args[i] = CPU.CPU_Peek32(i);
            }
            try {
                wait = false;
                if (LOG && params != null)
                    preLog(name, args, params);
                Integer result = (Integer)method.invoke(null, args);
                if (wait) {
                    if (LOG && params != null) {
                        System.out.print(" THREAD PUT TO SLEEP, WILL TRY AGAIN LATER");
                        indent--;
                    }
                    CPU_Regs.reg_eip = eip;
                    CPU_Regs.reg_esp.dword = esp;
                    Scheduler.wait(Scheduler.getCurrentThread());
                } else {
                    if (LOG && params != null)
                        postLog(name, result, (params != null && params.length>args.length)?params[args.length]:null, args, params);
                    CPU_Regs.reg_eax.dword = result;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Win.panic(getName()+" failed to execute: "+e.getMessage());
            }
        }

        public String getName() {
            return name;
        }
    }

    protected void add(Class c, String methodName) {
        add(c, methodName, null);
    }

    protected void add(Class c, String methodName, String[] params, int ordinal) {
        add(c, methodName, params);
        ordinalToName.put(ordinal, methodName);
    }

    protected void add(Class c, String methodName, String[] params) {
        Method[] methods = c.getMethods();
        for (Method method: methods) {
            if (method.getName().equals(methodName)) {
                if (method.getReturnType() == Integer.TYPE) {
                    add(new ReturnHandler(methodName, method, true, params));
                } else {
                    add(new NoReturnHandler(methodName, method, true, params));
                }
                return;
            }
        }
        Win.panic("Failed to find "+methodName);
    }

    protected void add_wait(Class c, String methodName) {
        add_wait(c, methodName, null);
    }

    protected void add_wait(Class c, String methodName, String[] params) {
        Method[] methods = c.getMethods();
        for (Method method: methods) {
            if (method.getName().equals(methodName)) {
                if (method.getReturnType() == Integer.TYPE) {
                    add(new WaitReturnHandler(methodName, method, true, params));
                } else {
                    Win.panic("WaitNoReturnHandler not implemented");
                    //add(new WaitNoReturnHandler(methodName, method, true));
                }
                return;
            }
        }
        Win.panic("Failed to find "+methodName);
    }

    protected void add_cdecl(Class c, String methodName) {
        add_cdecl(c, methodName, null);
    }

    protected void add_cdecl(Class c, String methodName, String[] params) {
        Method[] methods = c.getMethods();
        for (Method method: methods) {
            if (method.getName().equals(methodName)) {
                if (method.getReturnType() == Integer.TYPE) {
                    add(new ReturnHandler(methodName, method, false, params));
                } else {
                    add(new NoReturnHandler(methodName, method, false, params));
                }
                return;
            }
        }
        Win.panic("Failed to find "+methodName);
    }
    protected void add(Callback.Handler handler) {
        if (handler.getName().toLowerCase().startsWith(name.toLowerCase()))
            functions.put(handler.getName().substring(name.length() + 1), handler);
        else
            functions.put(handler.getName(), handler);
    }

    protected void add(Callback.Handler handler, int ordinal) {
        String name = handler.getName().substring(this.name.length() + 1);
        functions.put(name, handler);
        ordinalToName.put(ordinal, name);
    }
    protected int addData(String name, int size) {
        int result = WinSystem.getCurrentProcess().heap.alloc(size, false);
        registeredCallbacks.put(name, result);
        return result;
    }

    public int getProcAddress(final String functionName, boolean loadFake) {
        Integer result = registeredCallbacks.get(functionName);
        if (result != null)
            return result;

        Callback.Handler handler = functions.get(functionName);
        if (handler == null) {
            System.out.println("Unknown "+name+" function: "+functionName);
            if (loadFake) {
                handler = new HandlerBase() {
                    public void onCall() {
                        notImplemented();
                    }

                    public String getName() {
                        return name+" -> "+functionName;
                    }
                };
            }
        }
        if (handler != null) {
            int cb = WinCallback.addCallback(handler);
            int address =  loader.registerFunction(cb);
            registeredCallbacks.put(functionName, address);
            return address;
        }
        return 0;
    }

    public String getFileName(boolean fullPath) {
        if (fullPath)
            return WinAPI.SYSTEM32_PATH+fileName;
        return fileName;
    }

    public void callDllMain(int dwReason) {
    }

    public void unload() {
    }

    public boolean RtlImageDirectoryEntryToData(int dir, LongRef address, LongRef size) {
        if (dir == HeaderImageOptional.IMAGE_DIRECTORY_ENTRY_EXPORT)
            return true;
        return false;
    }

    public Vector getImportDescriptors(long address) {
        return null;
    }

    public String getVirtualString(long address) {
        return null;
    }

    public long[] getImportList(HeaderImageImportDescriptor desc) {
        return null;
    }

    public long findNameExport(long exportAddress, long exportsSize, String name, int hint) {
        return getProcAddress(name, true);
    }

    public long findOrdinalExport(long exportAddress, long exportsSize, int ordinal) {
        String name = ordinalToName.get(new Integer(ordinal));
        if (name != null)
            return getProcAddress(name, true);
        return 0;
    }

    public void getImportFunctionName(long address, StringRef name, IntRef hint) {
    }

    public void writeThunk(HeaderImageImportDescriptor desc, int index, long value) {
    }
}
