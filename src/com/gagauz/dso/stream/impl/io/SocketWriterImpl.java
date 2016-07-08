package com.gagauz.dso.stream.impl.io;

import com.gagauz.dso.stream.api.SocketObjectWriter;
import com.gagauz.dso.stream.utils.StreamUtils;

import java.io.OutputStream;
import java.net.Socket;

public class SocketWriterImpl implements SocketObjectWriter {

    private final OutputStream wStream;

    public SocketWriterImpl(Socket socket) {
        System.out.println("*************************************************************");
        System.out.println("****** Create SocketWriter " + socket.getInetAddress() + ":" + socket.getPort() + " ************");
        System.out.println("*************************************************************");
        try {
            wStream = socket.getOutputStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void writeObject(Object obj) {
        try {
            StreamUtils.writeObject(wStream, obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeBytes(byte[] bytes) {
        try {
            StreamUtils.writeBytes(wStream, bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
