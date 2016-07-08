package com.gagauz.dso.socket.api;

import java.io.IOException;

public interface ConnectionFactory {
    ClientConnection createClient(String host, int port) throws IOException;

    ServerConnection createServer(int port) throws IOException;
}
