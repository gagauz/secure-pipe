package com.gagauz.dso.stream.utils;

import java.io.*;

public class StreamUtils {

    private StreamUtils() {
    }

    public static interface OutputStreamOwner {
        OutputStream getOutputStream();
    }

    public static interface InputStreamOwner {
        InputStream getInputStream();
    }

    public static void writeString(OutputStream out, String string) throws IOException {
        out.write(string.getBytes());
        out.flush();
    }

    public static void writeBytes(OutputStream out, byte[] data) throws IOException {
        out.write(data);
        out.flush();
    }

    public static void writeObject(OutputStream out, Object object) throws IOException {
        ObjectOutputStream oi = new ObjectOutputStream(out);
        oi.writeUnshared(object);
        out.flush();
    }

    public static String readString(InputStream in) throws IOException {
        byte[] bytes = new byte[1024];
        int r = in.read(bytes);
        if (r >= 0) {
            return new String(bytes, 0, r);
        }
        return null;
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        int b = 0;
        int n = 0;
        while ((b = in.read()) > -1) {
            if (b == '\n') {
                if (n == '\n') {
                    return bais.toByteArray();
                } else {
                    n = b;
                }
            } else {
                if (n != 0) {
                    bais.write(n);
                    n = 0;
                }
                bais.write(b);
            }
        }
        return new byte[0];
    }

    public static Object readObject(InputStream in) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(in);
        return ois.readObject();
    }

}
