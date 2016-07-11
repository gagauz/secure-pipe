import com.gagauz.dso.socket.api.ClientConnection;
import com.gagauz.dso.socket.impl.io.IOConnectionFactory;

public class TestClient {
    private static String host;

    public static void main(String[] args) {
        final IOConnectionFactory connetcionFactory = new IOConnectionFactory();

        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        ClientConnection client = connetcionFactory.createClient(host, 7777);
                        System.out.println("CLIENT : " + host + ":7777");
                        SSLConnection ses = new SSLConnection("CLIENT", client.getSocket());
                        ses.send(
                                ("Hello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello stringHello string, Hello strinstring, Hello string")
                                        .getBytes());
                        client.getSocket().close();
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();

    }
}
