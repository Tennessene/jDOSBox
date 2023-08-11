package jdos.win.utils;

public class LittleEndian {
    byte[] buffer;
    int pos;

    public LittleEndian(byte[] buffer) {
        this.buffer = buffer;
        this.pos = 0;
    }

    public void seek(int pos) {
        this.pos = pos;
    }

    public void read(byte[] b) {
        System.arraycopy(buffer, pos, b, 0, b.length);
        pos+=b.length;
    }

    public String readCString() {
        String result = readCString(buffer, pos);
        pos+=result.length()+1;
        return result;
    }

    public String readCString(int len) {
        String result = readCString(buffer, pos, len);
        pos+=len;
        return result;
    }

    public String readCStringW() {
        String result = readCStringW(buffer, pos);
        pos+=2*(result.length()+1);
        return result;
    }

    public String readCStringW(int len) {
        String result = readCStringW(buffer, pos, len);
        pos+=2*len;
        return result;
    }

    public final short readShort() {
        short result = readShort(buffer, pos);
        pos+=2;
        return result;
    }

    public final int readUnsignedShort() {
        int result = readUnsignedShort(buffer, pos);
        pos+=2;
        return result;
    }

    public final int readInt() {
        int result = readInt(buffer, pos);
        pos+=4;
        return result;
    }

    public final long readUnsignedInt() {
        long result = readUnsignedInt(buffer, pos);
        pos+=4;
        return result;
    }

    public final short readUnsignedByte() {
        short result = readUnsignedByte(buffer, pos);
        pos+=1;
        return result;
    }

     static public String readCString(byte[] buffer, int offset) {
        StringBuffer result = new StringBuffer();
        while (offset<buffer.length) {
            char c = (char)buffer[offset++]; // :TODO: need to research converting according to 1252
            if (c == 0)
                break;
            result.append(c);
        }
        return result.toString();
    }

    static public void writeCString(byte[] buffer, int offset, String s) {
        byte[] b = s.getBytes();
        System.arraycopy(b, 0, buffer, offset, b.length);
        buffer[b.length] = 0;
    }

    static public String readCString(byte[] buffer, int offset, int len) {
        StringBuffer result = new StringBuffer();
        while (offset<buffer.length && len>0) {
            char c = (char)buffer[offset++]; // :TODO: need to research converting according to 1252
            if (c == 0)
                break;
            result.append(c);
            len--;
        }
        return result.toString();
    }

    static public String readCStringW(byte[] buffer, int offset) {
        StringBuffer result = new StringBuffer();
        while (offset<buffer.length) {
            char c = (char)readShort(buffer, offset);
            if (c == 0)
                break;
            result.append(c);
            offset+=2;
        }
        return result.toString();
    }

    static public String readCStringW(byte[] buffer, int offset, int len) {
        StringBuffer result = new StringBuffer();
        while (offset<buffer.length && len>0) {
            char c = (char)readShort(buffer, offset);
            if (c == 0)
                break;
            result.append(c);
            offset+=2;
            len--;
        }
        return result.toString();
    }
    static public final short readShort(byte[] buffer, int offset) {
        return (short)((buffer[offset] & 0xFF) | ((buffer[offset+1] & 0xFF) << 8));
    }

    static public final int readUnsignedShort(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF) | ((buffer[offset+1] & 0xFF) << 8);
    }

    static public final int readInt(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF) | ((buffer[offset+1] & 0xFF) << 8) | ((buffer[offset+2] & 0xFF) << 16) | ((buffer[offset+3] & 0xFF) << 24);
    }

    static public final long readUnsignedInt(byte[] buffer, int offset) {
        return readInt(buffer, offset) & 0xFFFFFFFFl;
    }

    static public final short readUnsignedByte(byte[] buffer, int offset) {
        return (short)(buffer[offset] & 0xFF);
    }
}
