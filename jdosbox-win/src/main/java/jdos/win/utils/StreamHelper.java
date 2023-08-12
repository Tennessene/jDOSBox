package jdos.win.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamHelper {
    public static byte[] readStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (true) {
            int len = is.read(buffer);
            if (len>0) {
                os.write(buffer, 0, len);
            } else {
                break;
            }
        }
        return os.toByteArray();
    }
}
