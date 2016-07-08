package com.gagauz.dso.stream.impl.io;

import com.gagauz.dso.stream.api.SocketObjectReader;
import com.gagauz.dso.stream.utils.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class SocketReaderImpl implements SocketObjectReader {

    private final InputStream rStream;

    public SocketReaderImpl(Socket socket) {
        System.out.println("*************************************************************");
        System.out.println("****** Create SocketReader " + socket.getInetAddress() + ":" + socket.getPort() + " ******");
        System.out.println("*************************************************************");
        try {
            this.rStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized Object readObject() throws Exception {
        return StreamUtils.readObject(rStream);
    }

    @Override
    public byte[] readBytes() throws Exception {
        return StreamUtils.readBytes(rStream);
    }
}
