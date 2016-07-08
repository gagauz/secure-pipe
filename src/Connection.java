import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Connection implements Runnable {

	private static final String ALGORITM = "RSA";
	private static final SecureRandom rnd = new SecureRandom(
			ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(System.currentTimeMillis()).array());

	private static Charset latin = Charset.defaultCharset();

	static {
		latin = Charset.forName("latin1");
	}

	@Override
	public void run() {
		while (true) {
			readBytes();
		}
	}

	private ExecutorService tpe = Executors.newCachedThreadPool();

	final KeyPair thisKeyPair;
	final InputStream reader;
	final OutputStream writer;

	PublicKey theirPublicKey;

	String other;

	Logger log;

	Object waiter = new Object();
	boolean initiator;

	Connection(String name, InputStream reader, OutputStream writer) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tm/%1$td %1$tH:%1$tM:%1$tS [%3$s] %4$s %2$s%n%5$s%n");
		log = Logger.getLogger(name);
		this.reader = reader;
		this.writer = writer;
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITM);
			kpg.initialize(1024, rnd);
			thisKeyPair = kpg.generateKeyPair();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		tpe.execute(this);
	}

	// in separate thread
	void readBytes() {
		try {
			ByteArrayOutputStream bais = new ByteArrayOutputStream();
			byte[] bytes = new byte[4024];
			int len = 0;

			while ((len = reader.read(bytes)) > -1) {
				bais.write(bytes, 0, len);
				if (bytes[len - 1] == '\0') {
					bais.flush();
					handleLine(bais.toByteArray());
					bais.reset();
				}
			}
			System.out.println("exit cycle");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void writeBytes(byte[] bytes) {
		try {
			writer.write(bytes);
			writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void writeBytes(String string) {
		writeBytes(string.getBytes(latin));
	}

	void writeBytes(char... string) {
		writeBytes(new String(string));
	}

	public void writeEncrypted(String string) {
		if (null == theirPublicKey) {
			initiator = true;
			requestKeys();
		}
		log.info("send ssl: " + string);
		try {
			byte[] encoded = encode(string.getBytes(latin), theirPublicKey);
			System.out.println(new String(encoded, latin));
			writeBytes(encoded);
			writeBytes('\0');
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException e) {
			e.printStackTrace();
		}
	}

	void requestKeys() {
		synchronized (waiter) {
			PublicKey key = thisKeyPair.getPublic();
			byte[] encoded = key.getEncoded();
			log.info("init handshake");
			writeBytes(key.getFormat());
			writeBytes(' ');
			writeBytes(encoded);
			writeBytes('\0');
			if (null == theirPublicKey) {
				try {
					waiter.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	void handleLine(byte[] bytes) {
		if (null == theirPublicKey) {
			synchronized (waiter) {
				String str = new String(bytes, latin);
				int i = str.indexOf(' ');
				String format = str.substring(0, i);
				str = str.substring(i + 1);
				byte[] key = str.getBytes(latin);
				EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
				KeyFactory fact;
				try {
					fact = KeyFactory.getInstance(ALGORITM);
					theirPublicKey = fact.generatePublic(keySpec);
					log.info("handshake success!");
					waiter.notify();
					if (!initiator) {
						requestKeys();
					}
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					e.printStackTrace();
				}
			}
		} else {
			log.info("handle data?");
			try {
				byte[] decoded = decode(bytes, thisKeyPair.getPrivate());
				log.info("recv ssl: " + new String(decoded, latin));
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
					| BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	static byte[] encode(byte[] input, PublicKey pub) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, pub);
		return cipher.doFinal(input);
	}

	static byte[] decode(byte[] input, PrivateKey priv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
		cipher.init(Cipher.DECRYPT_MODE, priv);
		byte[] dst = new byte[input.length - 1];
		System.arraycopy(input, 0, dst, 0, input.length - 1);
		return cipher.doFinal(dst);
	}
}
