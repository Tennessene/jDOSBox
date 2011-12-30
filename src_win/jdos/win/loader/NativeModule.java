package jdos.win.loader;

import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.util.LongRef;
import jdos.util.StringRef;
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
                System.out.println("   "+new String(header.imageSections[i].Name)+" segment at 0x"+Integer.toHexString(address)+" - 0x"+Long.toHexString(address+header.imageSections[i].PhysicalAddress_or_VirtualSize));
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
                return findOrdinalExport(exportAddress, exportsSize, ordinal);
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
                return findOrdinalExport(exportAddress, exportsSize, ordinal);
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
        if (ordinal >= exports.NumberOfFunctions) {
            System.out.println("Error: tried to look up ordinal "+ordinal+" in "+name+" but only "+exports.NumberOfFunctions+" functions are available.");
            return 0;
        }
        long proc = Memory.mem_readd((int)(baseAddress+exports.AddressOfFunctions+4*ordinal));
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
