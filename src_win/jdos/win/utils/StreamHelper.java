package jdos.win.utils;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import java.io.IOException;
import java.io.InputStream;

public class StreamHelper {
    public static byte[] readStream(InputStream is) throws IOException {
        ByteOutputStream os = new ByteOutputStream();
        os.write(is);
        return os.getBytes();
    }
}
