package edu.baylor.websocket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * HandShaker is implemented according to server requirement of RFC6544
 * @version 1.0.0 01/20/2012
 * @author Yanqing Liu
 */
public class HandShaker implements Runnable {

    private static final String GUUI = new String("258EAFA5-E914-47DA-95CA-C5AB0DC85B11");

    private InputStream in;

    private OutputStream out;

    private Exception exception = null;

    /**
     * Create HandShaker from InputStream and OutputStream
     * @param in InputStream from which get handshake message
     * @param out OutputStream to which send handshake message
     */
    public HandShaker(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public void run() {
        try {
            handShake();
        } catch (Exception e) {
            exception = e;
        }
    }

    /**
     * Receive handShake message and send back response.
     * 
     * @throws NoSuchAlgorithmException
     *          thrown when JVM doesn't support MD5 algorithm
     * @throws IOException
     *          thrown when there is a problem with protocol format error 
     */
    public void handShake() throws IOException, NoSuchAlgorithmException {
        String[] requestLine;
        String value = null;
        String name = null;
        String upgrade = null;
        String connection = null;
        String line = null;
        String host = null;
        String origin = null;
        String key = null;
        requestLine = Helper.readLine(in).split(" ");
        if ((!requestLine[0].equals("GET")) && (!requestLine[2].equals("HTTP1.1")) && (requestLine[1].charAt(0) != '\\')) {
            throw new IOException("Wrong format: " + requestLine);
        }
        while ((line = Helper.readLine(in)).length() != 0) {
            requestLine = line.split(" ");
            name = requestLine[0].toLowerCase();
            value = requestLine[1].toLowerCase();
            if (requestLine.length < 2) {
                throw new IOException("Wrong format: " + line);
            }
            if (name.equals("connection:")) {
                if (!value.equals("upgrade")) {
                    throw new IOException("Wrong format: " + line);
                } else {
                    connection = requestLine[1];
                }
            }
            if (name.equals("upgrade:")) {
                if (!value.equals("websocket")) {
                    throw new IOException("Wrong format: " + line);
                } else {
                    upgrade = requestLine[1];
                }
            }
            if (name.equals("sec-websocket-version:")) {
                if (!value.equals("13")) {
                    throw new IOException("Wrong format: " + line);
                }
            }
            if (name.equals("host:")) {
                host = requestLine[1];
            }
            if (name.equals("origin:")) {
                origin = requestLine[1];
            }
            if (name.toLowerCase().equals("sec-websocket-key:")) {
                key = requestLine[1];
            }
        }
        if ((upgrade == null) || (connection == null) || (host == null) || (origin == null) || (key == null)) {
            throw new IOException("Less arguments");
        }
        Helper.writeLine(out, "HTTP/1.1 101 Switching Protocols");
        Helper.writeLine(out, "Upgrade: websocket");
        Helper.writeLine(out, "Connection: Upgrade");
        Helper.writeLine(out, "Sec-WebSocket-Accept: " + calcReturnKey(key));
        Helper.writeLine(out, "");
    }

    /**
     * Calculate the base64 encoded key.
     * @param key string that need to be encoded
     * @return the encoded string
     * @throws UnsupportedEncodingException,NoSuchAlgorithmException
     *     thrown when there is a error in the SHA-1 encoder 
     */
    private static String calcReturnKey(String key) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        String text = new String();
        byte[] sha1hash = new byte[20];
        text = key + GUUI;
        md.update(text.getBytes(), 0, text.length());
        sha1hash = md.digest();
        return (Helper.getBASE64(sha1hash));
    }
}
