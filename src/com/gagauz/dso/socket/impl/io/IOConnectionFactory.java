package com.gagauz.dso.socket.impl.io;

import com.gagauz.dso.socket.api.ClientConnection;
import com.gagauz.dso.socket.api.ConnectionFactory;
import com.gagauz.dso.socket.api.ServerConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class IOConnectionFactory implements ConnectionFactory {

    @Override
    public ClientConnection createClient(final String host, final int port) throws IOException {
        return new ClientConnection() {
            final Socket socket;
            {
                socket = new Socket(InetAddress.getByName(host), port);
            }

            @Override
            public Socket getSocket() {
                return socket;
            }
        };
    }

    @Override
    public ServerConnection createServer(final int port) throws IOException {
        return new ServerConnection() {
            private final ServerSocket server;
            {
                server = new ServerSocket(port);
            }

            @Override
            public ClientConnection accept() throws IOException {
                final Socket socket = server.accept();
                System.out.println("Accepted client connection");
                return new ClientConnection() {
                    @Override
                    public Socket getSocket() {
                        return socket;
                    }
                };
            }

            @Override
            public String getAddress() {
                return server.getInetAddress().getHostName();
            }

            @Override
            public void close() throws IOException {
                server.close();
            }
        };
    }

}
