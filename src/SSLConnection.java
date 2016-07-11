import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
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
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class SSLConnection implements Runnable {

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

    private final KeyPair thisKeyPair;
    private final InputStream reader;
    private final OutputStream writer;
    private final Socket socket;
    private PublicKey theirPublicKey;
    private Predicate<byte[]> handler;
    private Logger log;

    Object waiter = new Object();
    boolean initiator;

    public SSLConnection(String name, Socket socket) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tm/%1$td %1$tH:%1$tM:%1$tS [%3$s] %4$s %2$s%n%5$s%n");
        log = Logger.getLogger(name);
        this.socket = socket;
        try {
            this.reader = socket.getInputStream();
            this.writer = socket.getOutputStream();
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITM);
            kpg.initialize(2048, rnd);
            thisKeyPair = kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        tpe.execute(this);
    }

    public void handleReceive(Predicate<byte[]> handler) {
        this.handler = handler;
    }

    public void send(byte[] data) {
        if (null == theirPublicKey) {
            initiator = true;
            requestKeys();
        }
        log.info("send ssl: " + new String(data));
        try {
            byte[] encoded = encode(data, theirPublicKey);
            log.info("encoded: " + new String(encoded));
            writeBytes(encoded);
            writeBytes('\0');
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    // in separate thread
    private void readBytes() {
        try {
            ByteArrayOutputStream bais = new ByteArrayOutputStream();
            byte[] bytes = new byte[4024];
            int len = 0;
            while (!socket.isClosed() && socket.isBound() && socket.isConnected() && (len = reader.read(bytes)) > -1) {
                if (bytes[len - 1] == '\0') {
                    bais.write(bytes, 0, len - 1);
                    bais.flush();
                    handleLine(bais.toByteArray());
                    bais.reset();
                } else {
                    bais.write(bytes, 0, len);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeBytes(byte[] bytes) {
        try {
            writer.write(bytes);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeBytes(String string) {
        writeBytes(string.getBytes(latin));
    }

    private void writeBytes(char... string) {
        writeBytes(new String(string));
    }

    private void requestKeys() {
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

    private void handleLine(byte[] bytes) {
        if (null == theirPublicKey) {
            synchronized (waiter) {
                String str = new String(bytes, latin);
                int i = str.indexOf(' ');
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
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            log.info("handle data?");
            try {
                log.info("recv ssl: " + new String(bytes));
                byte[] decoded = decode(bytes, thisKeyPair.getPrivate());
                log.info("decoded: " + new String(decoded));
                if (null != handler) {
                    handler.test(decoded);
                }
            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                    | BadPaddingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static byte[] encode(byte[] input, PublicKey pub) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            BadPaddingException {
        return Base64.getEncoder().encode(blockCipher(pub, input, Cipher.ENCRYPT_MODE));
    }

    private static byte[] decode(byte[] input, PrivateKey priv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            BadPaddingException {
        return blockCipher(priv, Base64.getDecoder().decode(input), Cipher.DECRYPT_MODE);
    }

    static final Pattern BSIZE_PATTERN = Pattern.compile("Data must not be longer than ([0-9]+) bytes");

    private static byte[] blockCipher(Key key, byte[] inputBytes, int mode)
            throws BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(mode, key);
        byte[] processedChunk = new byte[0];
        // byte[] outputBytes = new byte[0];
        System.out.println("Input bytes length " + inputBytes.length);
        int blockLength = 2245;
        int i = 0;

        ByteArrayOutputStream bais = new ByteArrayOutputStream();

        do {

            int copyLength = i + blockLength >= inputBytes.length
                    ? inputBytes.length - i
                    : blockLength;

            try {
                byte[] chunk = getChunk(inputBytes, i, blockLength, copyLength);
                processedChunk = cipher.doFinal(chunk);
                System.out.println("chunk i=" + i + ", len=" + chunk.length + " , copy=" + copyLength);
            } catch (IllegalBlockSizeException e) {
                Matcher m = BSIZE_PATTERN.matcher(e.getMessage());
                if (m.find()) {
                    final int mustHave = Integer.parseInt(m.group(1));
                    if (mustHave == blockLength) {
                        throw new IllegalStateException(e);
                    }
                    blockLength = mustHave;
                    // cipher = Cipher.getInstance("RSA");
                    cipher.init(mode, key);
                    continue;
                }
            }
            bais.write(processedChunk, 0, processedChunk.length);
            i += blockLength;
        } while (i < inputBytes.length);
        // System.out.println("Output bytes length " + outputBytes.length);
        // return outputBytes;
        return bais.toByteArray();
    }

    private static byte[] getChunk(byte[] source, int from, int length, int copylength) {
        byte[] toReturn = new byte[length];
        System.arraycopy(source, from, toReturn, 0, copylength);
        return toReturn;
    }

    private static byte[] append(byte[] destination, byte[] appendix) {
        byte[] toReturn = new byte[destination.length + appendix.length];
        System.arraycopy(destination, 0, toReturn, 0, destination.length);
        System.arraycopy(appendix, 0, toReturn, destination.length, appendix.length);
        return toReturn;
    }
}
