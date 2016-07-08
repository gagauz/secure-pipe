package com.gagauz.dso.socket.api;

import java.io.IOException;

public interface ServerConnection {
    ClientConnection accept() throws IOException;

    String getAddress() throws IOException;

    void close() throws IOException;
}
