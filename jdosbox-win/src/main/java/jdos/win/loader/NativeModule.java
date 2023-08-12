package jdos.win.loader;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.StringRef;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.kernel32.WinProcess;
import jdos.win.kernel.KernelHeap;
import jdos.win.kernel.WinCallback;
import jdos.win.loader.winpe.*;
import jdos.win.system.WinFile;
import jdos.win.system.WinSystem;
import jdos.win.utils.Path;
import jdos.win.utils.Ptr;

import java.io.ByteArrayOutputStream;
import java.util.Vector;

public class NativeModule extends Module {
    public HeaderPE header = new HeaderPE();

    private Path path;
    private KernelHeap heap;
    private Loader loader;
    private int baseAddress;
    private int resourceStartAddress;

    public NativeModule(Loader loader, int handle) {
        super(handle);
        this.loader = loader;
    }

    public String getFileName(boolean fullPath) {
        if (fullPath)
            return path.winPath+name;
        return name;
    }

    static Callback.Handler DllMainReturn = new Callback.Handler() {
        public String getName() {
            return "DllMainReturn";
        }

        public int call() {
            return 1; // return from DOSBOX_RunMachine
        }
    };

    private int returnEip = 0;

    public void callDllMain(int dwReason) {
        if (header.imageOptional.AddressOfEntryPoint == 0) {
            if (WinAPI.LOG)
                System.out.println(name+" has no DllMain");
        } else {
            int esp = CPU_Regs.reg_esp.dword;
              // This code helps debug DllMain by giving the same stack pointer
//                                KernelHeap stack = new KernelHeap(WinSystem.memory, WinSystem.getCurrentProcess().page_directory, 0x100000, 0x140000, 0x140000, true, false);
//                                stack.alloc(0x40000, false);
//                                Memory.mem_zero(0x100000, 0x40000);
//                                CPU_Regs.reg_esp.dword = 0x13F9F4+3*4+4;
            CPU.CPU_Push32(0); // the spec says this is non-null, but I'm not sure what to put in here
            CPU.CPU_Push32(dwReason);
            CPU.CPU_Push32(getHandle()); // HINSTANCE
            if (returnEip == 0) {
                int callback = WinCallback.addCallback(DllMainReturn);
                returnEip = loader.registerFunction(callback);
            }
            CPU.CPU_Push32(returnEip); // return ip
            int currentEip = CPU_Regs.reg_eip;
            CPU_Regs.reg_eip = (int)getEntryPoint();
            try {
                if (WinAPI.LOG) {
                    System.out.println(name+" calling DllMain@"+ Ptr.toString(CPU_Regs.reg_eip)+" dwReason="+dwReason);
                }
                Dosbox.DOSBOX_RunMachine();
                CPU_Regs.reg_esp.dword = esp;
                if (WinAPI.LOG)
                    System.out.println(name+" calling DllMain SUCCESS");
            } catch (Exception e) {
                e.printStackTrace();
            }
            CPU_Regs.reg_eip = currentEip;
        }
    }

    public int getProcAddress(String name, boolean loadFake) {
        LongRef exportAddress = new LongRef(0);
        LongRef exportSize = new LongRef(0);
        if (RtlImageDirectoryEntryToData(HeaderImageOptional.IMAGE_DIRECTORY_ENTRY_EXPORT, exportAddress, exportSize)) {
            return (int)findNameExport(exportAddress.value, exportSize.value, name, -1);
        }
        return 0;
    }

    private HeaderImageExportDirectory exports = null;

    public boolean load(WinProcess process, int page_directory, String name, Path path) {
        WinFile fis = null;
        this.path = path;
        this.name = name;
        try {
            fis = WinFile.createNoHandle(process.getFile(name), false, 0, 0);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if (!header.load(os, fis))
                return false;
            baseAddress = (int)header.imageOptional.ImageBase;
            byte[] headerImage = os.toByteArray();
            int allocated = headerImage.length;
            allocated = (allocated + 0xFFF) & ~0xFFF;
            long imageSize = header.imageSections[header.imageSections.length-1].VirtualAddress+header.imageSections[header.imageSections.length-1].SizeOfRawData;
            long reserved = process.addressSpace.alloc(baseAddress, imageSize);
            int oldbase = 0;
            if (reserved != baseAddress) {
                oldbase = baseAddress;
                baseAddress = (int)process.addressSpace.getNextAddress(baseAddress, imageSize, true);
                reserved = process.addressSpace.alloc(baseAddress, imageSize);
                if (reserved != baseAddress) {
                    Win.panic("NativeModule.load wasn't expecting this");
                }
                System.out.println();
                System.out.println("Relocating "+name+" from 0x"+Integer.toString(oldbase, 16)+" to 0x"+Integer.toString(baseAddress, 16));
            }
            heap = new KernelHeap(WinSystem.memory, page_directory, baseAddress, baseAddress+allocated, baseAddress+0x1000000, false, false);
            heap.alloc(allocated, false);
            Memory.mem_memcpy(baseAddress, headerImage, 0, headerImage.length);
            System.out.println("Loaded "+name+" at 0x"+Integer.toHexString(baseAddress)+" - 0x"+Integer.toHexString(baseAddress+headerImage.length));
            // Load code, data, import, etc sections
            for (int i=0;i<header.imageSections.length;i++) {
                int address = (int)header.imageSections[i].VirtualAddress+baseAddress;
                byte[] buffer = new byte[0];
                if (header.imageSections[i].PointerToRawData>0) {
                    fis.seek(header.imageSections[i].PointerToRawData, SEEK_SET);
                    buffer = new byte[(int)header.imageSections[i].SizeOfRawData];
                }
                String segmentName = new String(header.imageSections[i].Name);
                if (segmentName.startsWith(".rsrc")) {
                    resourceStartAddress = address;
                }
                System.out.println("   "+segmentName+" segment at 0x"+Integer.toHexString(address)+" - 0x"+Long.toHexString(address+header.imageSections[i].PhysicalAddress_or_VirtualSize)+"("+Long.toHexString(address+buffer.length)+")");
                if (buffer.length>0)
                    fis.read(buffer);
                int size = buffer.length;
                if (header.imageSections[i].PhysicalAddress_or_VirtualSize>size)
                    size = (int)header.imageSections[i].PhysicalAddress_or_VirtualSize;
                if (size == 0)
                    size = (int)header.imageSections[i].SizeOfRawData;
                if (address-baseAddress+size>allocated) {
                    int add = address-baseAddress+size - allocated;
                    add = (add + 0xFFF) & ~0xFFF;
                    allocated+=add;
                    heap.alloc(add, false);
                }
                Memory.mem_memcpy(address, buffer, 0, buffer.length);
                if (size>buffer.length)
                    Memory.mem_zero(address+buffer.length, size-buffer.length);
            }
            if (oldbase != 0) {
                LongRef relocAddress = new LongRef(0);
                LongRef relocSize = new LongRef(0);
                if (!RtlImageDirectoryEntryToData(HeaderImageOptional.IMAGE_DIRECTORY_ENTRY_BASERELOC, relocAddress, relocSize)) {
                    Win.panic("Dll needed to be relocated but could not find .reloc section");
                }
                int delta = baseAddress - oldbase;
                LittleEndianFile is = new LittleEndianFile((int)relocAddress.value+baseAddress, (int)relocSize.value);
                while (is.available()>0) {
                    int page = baseAddress + is.readInt();
                    int count = (is.readInt() - 8) / 2; // 8 is the size of the IMAGE_BASE_RELOCATION header and 2 is for the size of an USHORT
                    if (count == 0) {
                        break;
                    }
                    for (int i=0;i<count;i++) {
                        int value = is.readUnsignedShort();
                        int offset = value & 0xFFF;
                        int type = value >> 12;
                        switch (type) {
                            case 0: // IMAGE_REL_BASED_ABSOLUTE
                                break;
                            case 1: // IMAGE_REL_BASED_HIGH
                                {
                                    int s = Memory.mem_readw(page+offset);
                                    s+=delta >>> 16;
                                    Memory.mem_writew(page+offset, s);
                                }
                                break;
                            case 2: // IMAGE_REL_BASED_LOW
                                {
                                    int s = Memory.mem_readw(page+offset);
                                    s+=delta & 0xFFFF;
                                    Memory.mem_writew(page+offset, s);
                                }
                                break;
                            case 3: // IMAGE_REL_BASED_HIGHLOW
                                {
                                    int s = Memory.mem_readd(page+offset);
                                    s+=delta;
                                    Memory.mem_writed(page+offset, s);
                                }
                                break;
                            default:
                                Win.panic(name+"Unknown relocation type: "+type);
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
        } finally {
            if (fis != null) try {fis.close();} catch (Exception e) {}
        }
        return false;
    }

    static public final int RT_CURSOR = 1;
    static public final int RT_BITMAP = 2;
    static public final int RT_ICON = 3;
    static public final int RT_MENU = 4;
    static public final int RT_DIALOG = 5;
    static public final int RT_STRING = 6;
    static public final int RT_FONTDIR = 7;
    static public final int RT_FONT = 8;
    static public final int RT_ACCELERATOR = 9;
    static public final int RT_RCDATA = 10;
    static public final int RT_MESSAGETABLE = 11;
    static public final int RT_GROUP_CURSOR = 12;
    static public final int RT_VERSION = 16;

    static public class ResourceDirectory {
        public static final int SIZE = 16;

        public ResourceDirectory(int address) {
            LittleEndianFile is = new LittleEndianFile(address);
            Characteristics = is.readInt();
            TimeDateStamp = is.readInt();
            MajorVersion = is.readUnsignedShort();
            MinorVersion = is.readUnsignedShort();
            NumberOfNamedEntries = is.readUnsignedShort();
            NumberOfIdEntries = is.readUnsignedShort();
        }
        public int Characteristics;
        public int TimeDateStamp;
        public int MajorVersion;
        public int MinorVersion;
        public int NumberOfNamedEntries;
        public int NumberOfIdEntries;
    }
    public int getAddressOfResource(int type, int id) {
        return getAddressOfResource(type, id, null);
    }
    public int getAddressOfResource(int type, int id, IntRef size) {
        if (resourceStartAddress == 0)
            return 0;

        ResourceDirectory root = new ResourceDirectory(resourceStartAddress);
        if (type > 0xFFFF) {
            int address = resourceStartAddress +ResourceDirectory.SIZE;
            String strId = new LittleEndianFile(type).readCString();
            for (int i=0;i<root.NumberOfIdEntries;i++) {
                String itemName = getResourceName(address);
                address+=4;
                int offset = Memory.mem_readd(address);
                address+=4;
                if (strId.equalsIgnoreCase(itemName)) {
                    if (offset<0)
                        return getResourceById(resourceStartAddress + (offset & 0x7FFFFFFF), id, size);
                    return getResource(resourceStartAddress + offset, size);
                }
            }
        } else {
            int address = resourceStartAddress +ResourceDirectory.SIZE+root.NumberOfNamedEntries*8;

            for (int i=0;i<root.NumberOfIdEntries;i++) {
                int name = Memory.mem_readd(address);
                address+=4;
                int offset = Memory.mem_readd(address);
                address+=4;
                if (name == type) {
                    if (offset<0)
                        return getResourceById(resourceStartAddress + (offset & 0x7FFFFFFF), id, size);
                    return  getResource(resourceStartAddress + offset, size);
                }
            }
        }
        return 0;
    }
    private int getResource(int address, IntRef size) {
        int offset = Memory.mem_readd(address);
        if (size != null)
            size.value = Memory.mem_readd(address+4);
        return offset + baseAddress;
        //int CodePage = Memory.mem_readd(address+8);
        //int Reserved = Memory.mem_readd(address+12);
    }

    private String getResourceName(int address) {
        int name = Memory.mem_readd(address);
        return new LittleEndianFile(resourceStartAddress+ (name & 0x7FFFFFFF)+2).readCStringW(Memory.mem_readw(resourceStartAddress+ (name & 0x7FFFFFFF)));
    }

    private int getResourceById(int resourceAddress, int id, IntRef size) {
        ResourceDirectory root = new ResourceDirectory(resourceAddress);

        int address = resourceAddress+ResourceDirectory.SIZE;
        if (id>0xFFFF) {
            String strId = new LittleEndianFile(id).readCString();
            for (int i=0;i<root.NumberOfNamedEntries;i++) {
                String itemName = getResourceName(address);
                address+=4;
                int offset = Memory.mem_readd(address);
                address+=4;
                if (itemName.equalsIgnoreCase(strId)) {
                    if (offset<0)
                        return getResourceByCodePage(resourceStartAddress + (offset & 0x7FFFFFFF), size);
                    return getResource(resourceStartAddress + offset, size);
                }
            }
        } else {
            address = resourceAddress+ResourceDirectory.SIZE+root.NumberOfNamedEntries*8;
            for (int i=0;i<root.NumberOfIdEntries;i++) {
                int name = Memory.mem_readd(address);
                address+=4;
                int offset = Memory.mem_readd(address);
                address+=4;
                if (name == id) {
                    if (offset<0)
                        return getResourceByCodePage(resourceStartAddress + (offset & 0x7FFFFFFF), size);
                    return getResource(resourceStartAddress + offset, size);
                }
            }
        }
        return 0;
    }

    private int getResourceByCodePage(int resourceAddress, IntRef size) {
         ResourceDirectory root = new ResourceDirectory(resourceAddress);
        int address = resourceAddress+ResourceDirectory.SIZE+root.NumberOfNamedEntries*8;
        int defaultOffset = 0;

        for (int i=0;i<root.NumberOfIdEntries;i++) {
            int name = Memory.mem_readd(address);
            address+=4;
            int offset = Memory.mem_readd(address);
            address+=4;
            if (defaultOffset == 0)
                defaultOffset = offset;
            if (name == 1033) {
                return getResource(resourceStartAddress + offset, size);
            }
        }
        if (defaultOffset != 0)
            return getResource(resourceStartAddress + defaultOffset, size);
        return 0;
    }

    public long getEntryPoint() {
        return baseAddress+header.imageOptional.AddressOfEntryPoint;
    }

    public boolean RtlImageDirectoryEntryToData(int dir, LongRef address, LongRef size) {
        if (dir >= header.imageOptional.NumberOfRvaAndSizes)
            return false;
        if (header.imageOptional.DataDirectory[dir].VirtualAddress == 0)
            return false;
        address.value = header.imageOptional.DataDirectory[dir].VirtualAddress;
        size.value = header.imageOptional.DataDirectory[dir].Size;
        return true;
    }

    public Vector getImportDescriptors(long address) {
        Vector importDescriptors = new Vector();
        try {
            LittleEndianFile file = new LittleEndianFile((int)(baseAddress+address));
            while (true) {
                HeaderImageImportDescriptor desc = new HeaderImageImportDescriptor();
                desc.load(file);
                if (desc.Name != 0 && desc.FirstThunk!=0)
                    importDescriptors.add(desc);
                else
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return importDescriptors;
    }

    public String getVirtualString(long address) {
        LittleEndianFile file = new LittleEndianFile(baseAddress+(int)address);
        return file.readCString();
    }

    public void writeThunk(HeaderImageImportDescriptor desc, int index, long value) {
        Memory.mem_writed((int)(baseAddress + desc.FirstThunk) + 4 * index, (int) value);
    }

    public void unload() {

    }

    public long[] getImportList(HeaderImageImportDescriptor desc) {
        try {
            long import_list = desc.FirstThunk;
            if (desc.Characteristics_or_OriginalFirstThunk!=0) {
                import_list = desc.Characteristics_or_OriginalFirstThunk;
            }
            LittleEndianFile file = new LittleEndianFile(baseAddress+(int)import_list);
            Vector importOrdinals = new Vector();
            while (true) {
                long ord = file.readUnsignedInt();
                if (ord == 0) {
                    break;
                }
                importOrdinals.addElement(new Long(ord));
            }
            long[] result = new long[importOrdinals.size()];
            for (int i=0;i<result.length;i++) {
                result[i] = ((Long)importOrdinals.elementAt(i)).longValue();
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loadExports(long address) {
        if (exports == null) {
            exports = new HeaderImageExportDirectory();
            try {
                LittleEndianFile file = new LittleEndianFile(baseAddress+(int)address);
                exports.load(file);
            } catch (Exception e) {
                exports = null;
                e.printStackTrace();
            }
        }
    }

    public long findNameExport(long exportAddress, long exportsSize, String name, int hint) {
        loadExports(exportAddress);
        if (hint>=0 && hint<exports.NumberOfFunctions) {
            int address = (int)(baseAddress+exports.AddressOfNames+4*hint);
            int nameAddress = Memory.mem_readd(address)+baseAddress;
            String possibleMatch = new LittleEndianFile(nameAddress).readCString();
            if (possibleMatch.equalsIgnoreCase(name)) {
                int ordinal = Memory.mem_readw((int) (baseAddress + exports.AddressOfNameOrdinals + 2 * hint));
                return findOrdinalExport(exportAddress, exportsSize, ordinal+(int)exports.Base);
            }
        }
        int min = 0;
        int max = (int)exports.NumberOfFunctions-1;
        while (min <= max) {
            int res, pos = (min + max) / 2;
            int address = (int)(baseAddress+exports.AddressOfNames+4*pos);
            int nameAddress = Memory.mem_readd(address)+baseAddress;
            String possibleMatch = new LittleEndianFile(nameAddress).readCString();
            if ((res = possibleMatch.compareTo(name))==0) {
                int ordinal = Memory.mem_readw((int)(baseAddress + exports.AddressOfNameOrdinals + 2 * pos));
                return findOrdinalExport(exportAddress, exportsSize, ordinal+(int)exports.Base);
            }
            if (res > 0)
                max = pos - 1;
            else
                min = pos + 1;
        }
        return 0;
    }

    private long findForwardExport(long proc) {
        String mod = new LittleEndianFile((int)proc+baseAddress).readCString();
        System.out.println("Tried to foward export "+mod+".  This is not supported yet.");
        return 0;

    }
    public long findOrdinalExport(long exportAddress, long exportsSize, int ordinal) {
        loadExports(exportAddress);
        if (ordinal >= exports.NumberOfFunctions+exports.Base) {
            System.out.println("Error: tried to look up ordinal "+ordinal+" in "+name+" but only "+exports.NumberOfFunctions+" functions are available.");
            return 0;
        }
        long proc = Memory.mem_readd((int)(baseAddress+exports.AddressOfFunctions+4*(ordinal-exports.Base)));
        if (proc>=exportAddress && proc<exportAddress+exportsSize) {
            return findForwardExport(proc);
        }
        return proc+baseAddress;
    }

    public void getImportFunctionName(long address, StringRef name, IntRef hint) {
        try {
            LittleEndianFile file = new LittleEndianFile(baseAddress+(int)address);
            hint.value = file.readUnsignedShort();
            name.value = file.readCString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
