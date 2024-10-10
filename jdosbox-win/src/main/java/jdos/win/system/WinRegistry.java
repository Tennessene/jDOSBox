package jdos.win.system;

import jdos.hardware.Memory;
import jdos.win.Win;
import jdos.win.loader.winpe.LittleEndianFile;
import jdos.win.utils.Error;
import jdos.win.utils.StringUtil;

import java.util.Hashtable;

public class WinRegistry {
    static public final int REG_NONE =                     0;   // No value type
    static public final int REG_SZ =                       1;   // Unicode nul terminated string
    static public final int REG_EXPAND_SZ =                2;   // Unicode nul terminated string
    static public final int REG_BINARY =                   3;   // Free form binary
    static public final int REG_DWORD =                    4;   // 32-bit number
    static public final int REG_DWORD_LITTLE_ENDIAN =      4;   // 32-bit number (same as REG_DWORD)
    static public final int REG_DWORD_BIG_ENDIAN =         5;   // 32-bit number
    static public final int REG_LINK =                     6;   // Symbolic Link (unicode)
    static public final int REG_MULTI_SZ =                 7;   // Multiple Unicode strings
    static public final int REG_RESOURCE_LIST =            8;   // Resource list in the resource map
    static public final int REG_FULL_RESOURCE_DESCRIPTOR = 9;  // Resource list in the hardware description

    public static final int HKEY_CLASSES_ROOT =     0x80000000;
    public static final int HKEY_CURRENT_USER =     0x80000001;
    public static final int HKEY_LOCAL_MACHINE =    0x80000002;
    public static final int HKEY_USERS =            0x80000003;
    public static final int HKEY_PERFORMANCE_DATA = 0x80000004;
    public static final int HKEY_CURRENT_CONFIG =   0x80000005;
    public static final int HKEY_DYN_DATA =         0x80000006;

    private class Directory {
        public Directory(String name) {
            this.name = name;
        }
        public String name;

        public Hashtable children = new Hashtable();
        public Hashtable values = new Hashtable();
        public Value defaultValue;
    }

    private class Value {
        public Value(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        public byte[] getData() {
            // :TODO: repackage depending on type
            return data;
        }
        public int type;
        public byte[] data;
    }

    private class HKey {
        String[] parts;

        public HKey(String path) {
            parts = StringUtil.split(path, "\\");
        }

        public HKey(HKey parentKey, String path) {
            String[] tmp = StringUtil.split(path, "\\");
            parts = new String[parentKey.parts.length+tmp.length];
            System.arraycopy(parentKey.parts, 0, parts, 0, parentKey.parts.length);
            System.arraycopy(tmp, 0, parts, parentKey.parts.length, tmp.length);
        }
    }

    private Hashtable hKeys = new Hashtable();

    private Directory root = new Directory("root");
    private HKey currentUser = new HKey("HKEY_CURRENT_USER");
    private HKey localMachine = new HKey("HKEY_LOCAL_MACHINE");
    private int nextKey = 0x1000;

    private HKey getHKey(int hKey) {
        if (hKey<0) {
            switch (hKey) {
                case HKEY_CURRENT_USER:
                    return currentUser;
                case HKEY_LOCAL_MACHINE:
                    return localMachine;
                default:
                    Win.panic("Unsupported hKey "+hKey);
                    return null;
            }
        } else {
            return (HKey)hKeys.get(new Integer(hKey));
        }
    }

    private int nextKey() {
        return nextKey++;
    }

    private Directory getDirectory(HKey hKey) {
        Directory current = root;
        for (int i=0;i<hKey.parts.length;i++) {
            current = (Directory)current.children.get(hKey.parts[i]);
            if (current == null)
                break;
        }
        return current;
    }

    public int createKey(int hKey, int lpSubKey, int phkResult, int lpdwDisposition) {
        HKey key = new HKey(getHKey(hKey), new LittleEndianFile(lpSubKey).readCString());
        if (getDirectory(key) != null) {
            if (lpdwDisposition != 0)
                Memory.mem_writed(lpdwDisposition, 0x00000002); // REG_OPENED_EXISTING_KEY
        } else {
            if (lpdwDisposition != 0)
                Memory.mem_writed(lpdwDisposition, 0x00000001); // REG_CREATED_NEW_KEY
            Directory current = root;
            for (int i=0;i<key.parts.length;i++) {
                Directory parent = current;
                current = (Directory)current.children.get(key.parts[i]);
                if (current == null) {
                    current = new Directory(key.parts[i]);
                    parent.children.put(current.name, current);
                }
            }
        }
        int result = nextKey();
        hKeys.put(new Integer(result), key);
        if (phkResult != 0) {
            Memory.mem_writed(phkResult, result);
        }
        return Error.ERROR_SUCCESS;
    }

    public int openKey(int hKey, int lpSubKey, int phkResult) {
        HKey key = new HKey(getHKey(hKey), new LittleEndianFile(lpSubKey).readCString());
        if (getDirectory(key) != null) {
            int result = nextKey();
            hKeys.put(new Integer(result), key);
            return Error.ERROR_SUCCESS;
        } else {
            return Error.ERROR_BAD_PATHNAME;
        }
    }

    public int setValue(int hKey, int lpValue, int dwType , int lpData, int cbData) {
        Directory directory = getDirectory(getHKey(hKey));
        if (directory == null) {
            return Error.ERROR_BAD_PATHNAME;
        }
        Value value = null;
        if (lpValue == 0)
            value = directory.defaultValue;
        else
            value = (Value)directory.values.get(new LittleEndianFile(lpValue).readCString());
        if (value == null) {
            byte[] data = new byte[cbData];
            Memory.mem_memcpy(data, 0, lpData, cbData);
            value = new Value(dwType, data);
            if (lpValue == 0)
                directory.defaultValue = value;
            else
                directory.values.put(new LittleEndianFile(lpValue).readCString(), value);
        } else {
            value.data = new byte[cbData];
            value.type = dwType;
            Memory.mem_memcpy(value.data, 0, lpData, cbData);
        }
        return Error.ERROR_SUCCESS;
    }

    public int getValue(int hKey, int lpValue, int lpType, int lpData, int lpcbData) {
        if (hKey == 0 || getHKey(hKey) == null)
            return 0;
        Directory directory = getDirectory(getHKey(hKey));
        if (directory == null) {
            return Error.ERROR_BAD_PATHNAME;
        }
        Value value = null;
        if (lpValue == 0)
            value = directory.defaultValue;
        else {
            String name = new LittleEndianFile(lpValue).readCString();
            value = (Value)directory.values.get(name);
            if (value == null && name.equals("Game File Number")) {
                value = new Value(4, new byte[]{1,0,0,0});
                directory.values.put("Game File Number", value);
            }

        }
        if (value == null) {
            return Error.ERROR_FILE_NOT_FOUND;
        }
        if (lpType != 0)
            Memory.mem_writed(lpType, value.type);
        if (lpcbData != 0) {
            int size = Memory.mem_readd(lpcbData);
            byte[] data = value.getData();
            Memory.mem_writed(lpcbData, data.length);
            if (lpData!=0 && size>=data.length)
                Memory.mem_memcpy(lpData, data, 0, data.length);
        }
        return Error.ERROR_SUCCESS;
    }
}
