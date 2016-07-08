package com.gagauz.dso.stream.api;

public interface SocketObjectReader {
    Object readObject() throws Exception;

    byte[] readBytes() throws Exception;
}
