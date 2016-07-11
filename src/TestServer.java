import com.gagauz.dso.socket.api.ClientConnection;
import com.gagauz.dso.socket.api.ServerConnection;
import com.gagauz.dso.socket.impl.io.IOConnectionFactory;

public class TestServer {
    private static String host;

    public static void main(String[] args) {
        final IOConnectionFactory connetcionFactory = new IOConnectionFactory();

        new Thread() {

            @Override
            public void run() {
                try {
                    ServerConnection server = connetcionFactory.createServer(7777);
                    System.out.println("SERVER : " + server.getAddress() + ":7777");
                    host = server.getAddress();
                    ClientConnection cc = null;

                    while ((cc = server.accept()) != null) {
                        SSLConnection session = new SSLConnection("SERVER", cc.getSocket());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }
}
