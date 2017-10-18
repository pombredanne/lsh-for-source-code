package net.sourceforge.rconed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.sourceforge.rconed.exception.BadRcon;
import net.sourceforge.rconed.exception.ResponseEmpty;

/**
 * BF2Rcon is a simple Java library for issuing RCON commands to BF2 game servers.
 * <p/>
 * This has been used with default and BF2CC managed servers.
 * <p/>
 * Example:
 * <p/>
 * String response = BF2Rcon.send("127.0.0.1", 6711, "admin", "game.sayAll \"ploppers\"");
 * <p/>
 * @author DeadEd
 */
public class BF2Rcon {

    static final int RESPONSE_TIMEOUT = 2000;

    static Socket rconSocket = null;

    static InputStream in = null;

    static OutputStream out = null;

    /**
     * Send the RCON command to the game server
     *
     * @param ipStr     The IP (as a String) of the machine where the RCON command will go.
     * @param port      The port of the machine where the RCON command will go.
     * @param password  The RCON password.
     * @param command   The RCON command (without the rcon prefix).
     * @return The reponse text from the server after trying the RCON command.
     * @throws SocketTimeoutException when there is any problem communicating with the server.
     * @throws BadRcon when authentication fails
     * @throws ResponseEmpty when the response is empty
     */
    public static String send(String ipStr, int port, String password, String command) throws SocketTimeoutException, BadRcon, ResponseEmpty {
        return send(ipStr, port, password, command, 0);
    }

    /**
     * Send the RCON command to the game server (must have been previously authed with the correct rcon_password)
     *
     * @param ipStr     The IP (as a String) of the machine where the RCON command will go.
     * @param port      The port of the machine where the RCON command will go.
     * @param password  The RCON password.
     * @param command   The RCON command (without the rcon prefix).
     * @param localPort The port of the local machine to use for sending out the RCON request.
     * @return The reponse text from the server after trying the RCON command.
     * @throws SocketTimeoutException when there is any problem communicating with the server.
     * @throws BadRcon when authentication fails
     * @throws ResponseEmpty when the response is empty
     */
    public static String send(String ipStr, int port, String password, String command, int localPort) throws SocketTimeoutException, BadRcon, ResponseEmpty {
        return send(ipStr, port, password, command, null, localPort);
    }

    /**
     * Send the RCON command to the game server (must have been previously authed with the correct rcon_password)
     *
     * @param ipStr     The IP (as a String) of the machine where the RCON command will go.
     * @param port      The port of the machine where the RCON command will go.
     * @param password  The RCON password.
     * @param command   The RCON command (without the rcon prefix).
     * @param localhost The IP of the local machine to use for sending out the RCON request.
     * @param localPort The port of the local machine to use for sending out the RCON request.
     * @return The reponse text from the server after trying the RCON command.
     * @throws SocketTimeoutException when there is any problem communicating with the server.
     * @throws BadRcon when authentication fails
     * @throws ResponseEmpty when the response is empty
     */
    public static String send(String ipStr, int port, String password, String command, InetAddress localhost, int localPort) throws SocketTimeoutException, BadRcon, ResponseEmpty {
        StringBuffer response = new StringBuffer();
        try {
            rconSocket = new Socket();
            rconSocket.bind(new InetSocketAddress(localhost, localPort));
            rconSocket.connect(new InetSocketAddress(ipStr, port), RESPONSE_TIMEOUT);
            out = rconSocket.getOutputStream();
            in = rconSocket.getInputStream();
            BufferedReader buffRead = new BufferedReader(new InputStreamReader(in));
            rconSocket.setSoTimeout(RESPONSE_TIMEOUT);
            String digestSeed = "";
            boolean loggedIn = false;
            boolean keepGoing = true;
            while (keepGoing) {
                String receivedContent = buffRead.readLine();
                if (receivedContent.startsWith("### Digest seed: ")) {
                    digestSeed = receivedContent.substring(17, receivedContent.length());
                    try {
                        MessageDigest md5 = MessageDigest.getInstance("MD5");
                        md5.update(digestSeed.getBytes());
                        md5.update(password.getBytes());
                        String digestStr = "login " + digestedToHex(md5.digest()) + "\n";
                        out.write(digestStr.getBytes());
                    } catch (NoSuchAlgorithmException e1) {
                        response.append("MD5 algorithm not available - unable to complete RCON request.");
                        keepGoing = false;
                    }
                } else if (receivedContent.startsWith("error: not authenticated: you can only invoke 'login'")) {
                    throw new BadRcon();
                } else if (receivedContent.startsWith("Authentication failed.")) {
                    throw new BadRcon();
                } else if (receivedContent.startsWith("Authentication successful, rcon ready.")) {
                    keepGoing = false;
                    loggedIn = true;
                }
            }
            if (loggedIn) {
                String cmd = "exec " + command + "\n";
                out.write(cmd.getBytes());
                readResponse(buffRead, response);
                if (response.length() == 0) {
                    throw new ResponseEmpty();
                }
            }
        } catch (SocketTimeoutException timeout) {
            throw timeout;
        } catch (UnknownHostException e) {
            response.append("UnknownHostException: " + e.getMessage());
        } catch (IOException e) {
            response.append("Couldn't get I/O for the connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (rconSocket != null) {
                    rconSocket.close();
                }
            } catch (IOException e1) {
            }
        }
        return response.toString();
    }

    /**
     * @param buffRead where to read from
     * @param sb where to put read data
     * @throws IOException
     */
    private static void readResponse(BufferedReader buffRead, StringBuffer sb) throws IOException {
        int ch;
        while (true) {
            ch = buffRead.read();
            if (ch == -1 || ch == 4) {
                return;
            }
            sb.append((char) ch);
        }
    }

    private static String digestedToHex(byte digest[]) {
        StringBuffer store = new StringBuffer();
        for (int x = 0; x < digest.length; x++) {
            byte bite = digest[x];
            String val = Integer.toHexString(bite & 255);
            if (val.length() == 1) {
                store.append("0");
            }
            store.append(val);
        }
        return store.toString();
    }
}
