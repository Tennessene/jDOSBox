package jdos.cpu.core_dynamic;

import jdos.Dosbox;
import jdos.hardware.mame.RasterizerCompiler;

import java.io.*;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Loader {
    private static class SaveItem {
        public SaveItem(String name, byte[] byteCode, int start, byte[] opCode, String source) {
            this.name = name;
            this.byteCode = byteCode;
            this.opCode = opCode;
            this.source = source;
            this.start = start;
        }
        String source;
        String name;
        byte[] byteCode;
        byte[] opCode;
        int start;
    }
    private static class Item {
        String name;
        byte[] opCodes;
        int start;
    }

    private static Vector savedItems = new Vector();

    private static Hashtable items = new Hashtable();
    private static boolean initialized = false;

    public static boolean isLoaded() {
        if (!initialized)
            init();
        return items.size()!=0;
    }
    private static void init() {
        initialized = true;
        InputStream is = Dosbox.class.getResourceAsStream("Cache.index");
        if (is != null) {
            DataInputStream dis = new DataInputStream(is);
            try {
                int count = dis.readInt();
                for (int i=0;i<count;i++) {
                    Item item = new Item();
                    item.name = dis.readUTF();
                    item.start = dis.readInt();
                    int len = dis.readInt();
                    item.opCodes = new byte[len];
                    dis.readFully(item.opCodes);
                    Integer key = new Integer(item.start);
                    Vector bucket = (Vector)items.get(key);
                    if (bucket == null) {
                        bucket = new Vector();
                        items.put(key, bucket);
                    }
                    bucket.addElement(item);
                }
                System.out.println("Loaded " + count + " blocks");
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {dis.close();} catch (Exception e) {}
        }
    }
    public static Op load(int start, byte[] opCodes) {
        Integer key = new Integer(start);
        Vector bucket = (Vector)items.get(key);
        if (bucket != null) {
            for (int i=0;i<bucket.size();i++) {
                Item item = (Item)bucket.elementAt(i);
                if (item.start==start && Arrays.equals(item.opCodes, opCodes)) {
                    try {
                        return (Op)Class.forName(item.name).newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }
    public static void add(String className, byte[] byteCode, int start, byte[] opCode, String source) {
        savedItems.add(new SaveItem(className, byteCode, start, opCode, source));
    }
    public static void save(String fileName, boolean source) {
        source = true;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(savedItems.size());
            ByteArrayOutputStream src_bos = null;
            DataOutputStream src_dos = null;
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(fileName+".jar")));
            String root = fileName+"_src"+File.separator+ "jdos";
            String dirName = root+File.separator+"cpu"+File.separator+"core_dynamic";
            if (source) {
                src_bos = new ByteArrayOutputStream();
                src_dos = new DataOutputStream(src_bos);
                src_dos.writeInt(savedItems.size());
                File dir = new File(dirName);
                if (!dir.exists())
                    dir.mkdirs();
                File[] existing = dir.listFiles();
                for (int i=0;i<existing.length;i++) {
                    existing[i].delete();
                }
            }
            for (int i=0;i<savedItems.size();i++) {
                SaveItem item = (SaveItem)savedItems.elementAt(i);
                out.putNextEntry(new ZipEntry(item.name + ".class"));
                out.write(item.byteCode);
                dos.writeUTF(item.name);
                dos.writeInt(item.start);
                dos.writeInt(item.opCode.length);
                dos.write(item.opCode);
                if (source) {
                    FileOutputStream fos = new FileOutputStream(dirName+File.separator+item.name.substring(item.name.lastIndexOf('.')+1)+".java");
                    fos.write(item.source.getBytes());
                    fos.close();
                    src_dos.writeUTF("jdos.cpu.core_dynamic." + item.name);
                    src_dos.writeInt(item.start);
                    src_dos.writeInt(item.opCode.length);
                    src_dos.write(item.opCode);
                }
            }
            out.putNextEntry(new ZipEntry("jdos/Cache.index"));
            dos.flush();
            out.write(bos.toByteArray());
            RasterizerCompiler.save(out);
            out.flush();
            out.close();
            if (source) {
                src_dos.flush();
                FileOutputStream fos = new FileOutputStream(root+File.separator+"Cache.index");
                fos.write(src_bos.toByteArray());
                fos.close();
            }
            System.out.println("Saved "+savedItems.size()+" blocks");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
