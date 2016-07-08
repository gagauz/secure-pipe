import com.gagauz.dso.socket.api.ClientConnection;
import com.gagauz.dso.socket.api.ServerConnection;
import com.gagauz.dso.socket.impl.io.IOConnectionFactory;

public class Test {
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
						Connection session = new Connection("SERVER", cc.getSocket().getInputStream(), cc.getSocket().getOutputStream());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}.start();

		new Thread() {
			@Override
			public void run() {
				try {
					ClientConnection client = connetcionFactory.createClient(host, 7777);
					System.out.println("CLIENT : " + host + ":7777");
					Connection ses = new Connection("CLIENT", client.getSocket().getInputStream(), client.getSocket().getOutputStream());

					ses.writeEncrypted("Hello string, Hello string, Hello string, Hello string, Hello string, Hello string, Hello string");

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}.start();

	}
}
