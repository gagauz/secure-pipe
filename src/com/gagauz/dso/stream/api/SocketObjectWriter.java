package com.gagauz.dso.stream.api;

public interface SocketObjectWriter {
    void writeObject(Object obj);

    void writeBytes(byte[] bytes);
}
