package jdos.win.loader;

import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.StringRef;
import jdos.win.Win;
import jdos.win.kernel.KernelHeap;
import jdos.win.loader.winpe.HeaderImageExportDirectory;
import jdos.win.loader.winpe.HeaderImageImportDescriptor;
import jdos.win.loader.winpe.HeaderPE;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.utils.Path;
import jdos.win.utils.WinSystem;

import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.util.Vector;

public class NativeModule extends Module {
    public HeaderPE header = new HeaderPE();

    private Path path;
    private String name;
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

    public int getProcAddress(String name, boolean loadFake) {
        return 0;
    }

    private HeaderImageExportDirectory exports = null;

    public boolean load(int page_directory, String name, Path path) {
        RandomAccessFile fis = null;
        this.path = path;
        this.name = name;
        try {
            fis = new RandomAccessFile(path.nativePath+name, "r");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if (!header.load(os, fis))
                return false;
            baseAddress = (int)header.imageOptional.ImageBase;
            byte[] headerImage = os.toByteArray();
            int allocated = headerImage.length;
            allocated = (allocated + 0xFFF) & ~0xFFF;
            heap = new KernelHeap(WinSystem.memory, page_directory, baseAddress, baseAddress+allocated, baseAddress+0x1000000, false, false);
            heap.alloc(allocated, false);
            Memory.mem_memcpy(baseAddress, headerImage, 0, headerImage.length);
            System.out.println("Loaded "+name+" at 0x"+Integer.toHexString(baseAddress)+" - 0x"+Integer.toHexString(baseAddress+headerImage.length));
            // Load code, data, import, etc sections
            for (int i=0;i<header.imageSections.length;i++) {
                int address = (int)header.imageSections[i].VirtualAddress+baseAddress;
                fis.seek(header.imageSections[i].PointerToRawData);
                byte[] buffer = new byte[(int)header.imageSections[i].SizeOfRawData];
                String segmentName = new String(header.imageSections[i].Name);
                if (segmentName.startsWith(".rsrc")) {
                    resourceStartAddress = address;
                }
                System.out.println("   "+segmentName+" segment at 0x"+Integer.toHexString(address)+" - 0x"+Long.toHexString(address+header.imageSections[i].PhysicalAddress_or_VirtualSize)+"("+Long.toHexString(address+buffer.length)+")");
                fis.read(buffer);
                int size = buffer.length;
                if (header.imageSections[i].PhysicalAddress_or_VirtualSize>size)
                    size = (int)header.imageSections[i].PhysicalAddress_or_VirtualSize;
                if (address-baseAddress+size>allocated) {
                    int add = address-baseAddress+size - allocated;
                    add = (add + 0xFFF) & ~0xFFF;
                    allocated+=add;
                    heap.alloc(add, false);
                }
                Memory.mem_memcpy(address, buffer, 0, buffer.length);
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
        if (resourceStartAddress == 0)
            return 0;

        ResourceDirectory root = new ResourceDirectory(resourceStartAddress);
        if (type > 0xFFFF) {
            Win.panic("Loading a resource by type name is not supported yet");
        } else {
            int address = resourceStartAddress +ResourceDirectory.SIZE+root.NumberOfNamedEntries*8;

            for (int i=0;i<root.NumberOfIdEntries;i++) {
                int name = Memory.mem_readd(address);
                address+=4;
                int offset = Memory.mem_readd(address);
                address+=4;
                if (name == type) {
                    if (offset<0)
                        return getResourceById(resourceStartAddress + (offset & 0x7FFFFFFF), id);
                    return  getResource(resourceStartAddress + offset);
                }
            }
        }
        int ii=0;
        return 0;
    }
    private int getResource(int address) {
        int offset = Memory.mem_readd(address);
        int size = Memory.mem_readd(address+4);
        return offset + baseAddress;
        //int CodePage = Memory.mem_readd(address+8);
        //int Reserved = Memory.mem_readd(address+12);
    }

    private int getResourceById(int resourceAddress, int id) {
        ResourceDirectory root = new ResourceDirectory(resourceAddress);

        int address = resourceAddress+ResourceDirectory.SIZE;
        if (id>0xFFFF) {
            String strId = new LittleEndianFile(id).readCString();
            for (int i=0;i<root.NumberOfNamedEntries;i++) {
                int name = Memory.mem_readd(address);
                String itemName = new LittleEndianFile(resourceStartAddress+ (name & 0x7FFFFFFF)+2).readCStringW(Memory.mem_readw(resourceStartAddress+ (name & 0x7FFFFFFF)));
                address+=4;
                int offset = Memory.mem_readd(address);
                address+=4;
                if (itemName.equalsIgnoreCase(strId)) {
                    if (offset<0)
                        return getResourceByCodePage(resourceStartAddress + (offset & 0x7FFFFFFF));
                    return getResource(resourceStartAddress + offset);
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
                        return getResourceByCodePage(resourceStartAddress + (offset & 0x7FFFFFFF));
                    return getResource(resourceStartAddress + offset);
                }
            }
        }
        return 0;
    }

    private int getResourceByCodePage(int resourceAddress) {
         ResourceDirectory root = new ResourceDirectory(resourceAddress);
        int address = resourceAddress+ResourceDirectory.SIZE+root.NumberOfNamedEntries*8;

        for (int i=0;i<root.NumberOfIdEntries;i++) {
            int name = Memory.mem_readd(address);
            address+=4;
            int offset = Memory.mem_readd(address);
            address+=4;
            if (name == 1033) {
                return getResource(resourceStartAddress + offset);
            }
        }
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
