package org.libtunesremote_se;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import android.util.Log;

public class PairingServer {

    public static final int CLOSED = -1;

    public static final int SUCCESS = 0;

    public static final int ERROR = 1;

    public static final String TAG = PairingServer.class.toString();

    protected static final byte[] CHAR_TABLE = new byte[] { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F' };

    protected static byte[] PAIRING_RAW = new byte[] { 0x63, 0x6d, 0x70, 0x61, 0x00, 0x00, 0x00, 0x3a, 0x63, 0x6d, 0x70, 0x67, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x63, 0x6d, 0x6e, 0x6d, 0x00, 0x00, 0x00, 0x16, 0x41, 0x64, 0x6d, 0x69, 0x6e, 0x69, 0x73, 0x74, 0x72, 0x61, 0x74, 0x6f, 0x72, (byte) 0xe2, (byte) 0x80, (byte) 0x99, 0x73, 0x20, 0x69, 0x50, 0x6f, 0x64, 0x63, 0x6d, 0x74, 0x79, 0x00, 0x00, 0x00, 0x04, 0x69, 0x50, 0x6f, 0x64 };

    protected ServerSocket server;

    protected final Random random = new Random();

    protected int portNumber = 0;

    protected String pairCode;

    protected String serviceGuid;

    protected boolean pairing = false;

    protected MessageDigest md5;

    private PairingDatabase pairingDatabase;

    private static PairingServer instance = new PairingServer();

    protected PairingServer() {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static PairingServer getInstance() {
        return instance;
    }

    public synchronized int pair(String code) {
        int result = ERROR;
        pairing = true;
        if (this.server == null) {
            try {
                this.server = new ServerSocket(0);
                this.portNumber = this.server.getLocalPort();
                server.setSoTimeout(100);
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
        if (this.pairingDatabase == null) {
            this.pairingDatabase = new PairingDatabase();
            this.pairCode = pairingDatabase.getPairCode();
            this.serviceGuid = pairingDatabase.getServiceGuid();
        }
        Log.i(TAG, "Started Pairing Server on Port " + portNumber);
        String expectedCode = expectedPairingCode(code);
        TunesService.getInstance().registerPairingService(serviceGuid, pairCode, portNumber);
        while (pairing && result != SUCCESS) {
            try {
                final Socket socket = server.accept();
                final String address = socket.getInetAddress().getHostAddress();
                Log.i(TAG, "accepted connection from " + address + "...");
                OutputStream output = null;
                try {
                    String serviceName = null;
                    String pairingcode = null;
                    output = socket.getOutputStream();
                    final BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    while (br.ready()) {
                        String line = br.readLine();
                        Log.d(TAG, line);
                        if (line.contains("servicename=")) {
                            line = URLDecoder.decode(line, "UTF-8");
                            String[] tokens = line.split("[ &?=]");
                            for (int i = 0; i < tokens.length - 1; i++) {
                                if (tokens[i].equals("pairingcode")) {
                                    pairingcode = tokens[i + 1];
                                    i++;
                                } else if (tokens[i].equals("servicename")) {
                                    serviceName = tokens[i + 1];
                                    i++;
                                }
                            }
                        }
                    }
                    if (serviceName != null && pairingcode != null && pairingcode.equals(expectedCode)) {
                        byte[] loginGuid = new byte[8];
                        random.nextBytes(loginGuid);
                        System.arraycopy(loginGuid, 0, PAIRING_RAW, 16, 8);
                        final String niceCode = toHex(loginGuid);
                        byte[] header = String.format("HTTP/1.1 200 OK\r\nContent-Length: %d\r\n\r\n", PAIRING_RAW.length).getBytes();
                        byte[] reply = new byte[header.length + PAIRING_RAW.length];
                        System.arraycopy(header, 0, reply, 0, header.length);
                        System.arraycopy(PAIRING_RAW, 0, reply, header.length, PAIRING_RAW.length);
                        output.write(reply);
                        Log.i(TAG, "Received pairing command");
                        Log.i(TAG, "address = " + address);
                        Log.i(TAG, "servicename = \"" + serviceName + "\"");
                        Log.i(TAG, "pairingcode = \"" + pairingcode + "\"");
                        Log.d(TAG, "niceCode = \"" + niceCode + "\"");
                        pairingDatabase.updateCode(serviceName, niceCode);
                        result = SUCCESS;
                    } else {
                        Log.i(TAG, "Wrong pairing code");
                        output.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".getBytes());
                    }
                } finally {
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
                    output = null;
                }
            } catch (java.net.SocketTimeoutException e) {
                if (!pairing) {
                    result = CLOSED;
                    break;
                }
            } catch (java.net.SocketException e) {
                Log.i(TAG, e.getMessage());
                break;
            } catch (IOException e) {
                Log.w(TAG, e);
                break;
            }
        }
        Log.i(TAG, "Unregistering Pairing Service");
        TunesService.getInstance().unregisterPairingService();
        Log.i(TAG, "Finished Pairing");
        return result;
    }

    private String expectedPairingCode(String code) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(pairCode.getBytes("UTF-8"));
            byte passcode[] = code.getBytes("UTF-8");
            for (int c = 0; c < passcode.length; c++) {
                os.write(passcode[c]);
                os.write(0);
            }
            return toHex(md5.digest(os.toByteArray()));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
            return "";
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return "";
        }
    }

    public void cancelPairing() {
        pairing = false;
    }

    public static String toHex(byte[] code) {
        byte[] result = new byte[2 * code.length];
        int index = 0;
        for (byte b : code) {
            int v = b & 0xff;
            result[index++] = CHAR_TABLE[v >>> 4];
            result[index++] = CHAR_TABLE[v & 0xf];
        }
        return new String(result);
    }
}
