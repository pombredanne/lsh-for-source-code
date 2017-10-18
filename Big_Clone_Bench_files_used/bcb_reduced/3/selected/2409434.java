package quietcoffee.ssh;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.text.*;

/**
 *  A connection to the remote host using SSH.
 *  TODO: SSH1/SSH2 connections should be subclasses.
 *
 * @author  Brett Porter
 * @version $Id: SSHConnection.java,v 1.1.1.1 2002/05/13 03:49:14 brettporter Exp $
 */
public class SSHConnection {

    /** The SSH library version. */
    public static final String SSH_VERSION = "Quiet Coffee (SSH-Java) 0.5";

    /** The SSH1 protocol version. */
    public static final int PROTOCOL_MAJOR_1 = 1;

    /** The SSH1 protocol version. */
    public static final int PROTOCOL_MINOR_1 = 5;

    /** The SSH2 protocol version. */
    public static final int PROTOCOL_MAJOR_2 = 2;

    /** The SSH2 protocol version. */
    public static final int PROTOCOL_MINOR_2 = 0;

    private static final int SSH_SMSG_SUCCESS = 14;

    private static final int SSH_SMSG_PUBLIC_KEY = 2;

    private static final int SSH_CMSG_SESSION_KEY = 3;

    /**
     * Length of the session key in bytes.  (Specified as 256 bits in the
     * protocol.)
     */
    private static final int SSH_SESSION_KEY_LENGTH = 32;

    /**
     * Force host key length and server key length to differ by at least this
     * many bits.  This is to make double encryption with rsaref work.
     */
    private static final int SSH_KEY_BITS_RESERVED = 128;

    /** X11 forwarding includes screen */
    private static final int SSH_PROTOFLAG_SCREEN_NUMBER = 1;

    /** forwarding opens contain host */
    private static final int SSH_PROTOFLAG_HOST_IN_FWD_OPEN = 2;

    /** The standard SSH port. */
    private static final int SSH_DEFAULT_PORT = 22;

    /** The connection socket. */
    private Socket socket = null;

    /** The packet handlet. */
    private Packet packet = null;

    /** Session ID for the current session. */
    private byte[] sessionId = null;

    /** The flags of supported authentications. */
    private int supportedAuthentications = 0;

    /** The options used. */
    private Options options = null;

    /** The version compatibility options. */
    private Compatibility compatibility = new Compatibility();

    /** The random number generator to use. */
    private java.util.Random random = null;

    /** Creates a new SSHConnection.
     *      @param options the options
     */
    public SSHConnection(Options options) {
        this.options = options;
    }

    /**
     * Opens a TCP/IP connection to the remote server on the given host.
     * The address of the remote host will be returned in hostaddr. (TODO)
     * If anonymous is zero, (TODO)
     * a privileged port will be allocated to make the connection. (TODO)
     * This requires super-user privileges if anonymous is false. (TODO)
     *
     *      @param host the host to connect to
     *      @param port the port to connect to. If port is 0, the default port will be used.
     *      @param connectionAttempts specifies the maximum number of tries (one per second).
     *      @param proxyCommand if is non-NULL, it specifies the command (with %h
     *          and %p substituted for host and port, respectively) to use to contact the daemon.
     *      @throws UnknownHostException if the host could be found
     *      @throws ConnectionAbortedException TODO
     *      @throws ConnectionRefusedException TODO
     *      @throws SocketException if a problem occurs when configuring the socket
     */
    public void connect(String host, int port, int connectionAttempts, String proxyCommand) throws UnknownHostException, ConnectionAbortedException, ConnectionRefusedException, SocketException {
        boolean fullFailure = true;
        InputStream sockIn = null;
        OutputStream sockOut = null;
        Log.getLogInstance().TODO("SSHConnection.connect(): unix guff");
        if (port == 0) {
            port = SSH_DEFAULT_PORT;
        }
        Log.getLogInstance().TODO("SSHConnection.connect(): proxy command");
        Log.getLogInstance().log("TODO: SSHConnection.java:connect() ipv6 support");
        InetAddress inetAddresses[] = InetAddress.getAllByName(host);
        int attempt;
        for (attempt = 0; ; ) {
            if (attempt > 0) {
                Log.getLogInstance().debug("Trying again...");
            }
            for (int ai = 0; ai < inetAddresses.length; ai++) {
                Log.getLogInstance().debug("Connecting to " + host + " [" + inetAddresses[ai].getHostAddress() + "] port " + port + ".");
                try {
                    Log.getLogInstance().log("TODO: SSHConnection.java:connect() socket parameters");
                    socket = new Socket(inetAddresses[ai], port);
                    sockIn = socket.getInputStream();
                    sockOut = socket.getOutputStream();
                    Log.getLogInstance().log("TODO: SSHConnection.java:connect() extra connect to host");
                } catch (java.io.IOException e) {
                    continue;
                }
            }
            if (socket != null) {
                break;
            }
            attempt++;
            if (attempt >= connectionAttempts) break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        if (attempt >= connectionAttempts) {
            if (fullFailure) {
                throw new ConnectionAbortedException();
            } else {
                throw new ConnectionRefusedException();
            }
        }
        Log.getLogInstance().debug("Connection established.");
        socket.setSoLinger(true, 5);
        socket.setKeepAlive(options.isKeepalives());
        Log.getLogInstance().TODO("SSHConnection.java:connect() different random - java.security.SecureRandom?");
        random = new java.util.Random();
        packet = new Packet(sockIn, sockOut, compatibility, random);
        packet.setPacketDebug(options.isPacketDebug());
    }

    /**
     *  Close the socket.
     */
    public void close() {
        try {
            if (packet != null) {
                packet.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a dialog with the server, and authenticates the current user on the
     * server.  This does not need any extra privileges.  The basic connection
     * to the server must already have been established before this is called.
     * If login fails, this function prints an error and never returns.
     * This function does not require super-user privileges.
     *      @param origHost the hostname to connect to
     *      @param serverUser the user to connect to on the server
     *      @throws IOException if comms fail
     *      @throws SSHException if some other failure
     */
    public void login(String origHost, String serverUser) throws SSHException, IOException {
        String localUser = System.getProperty("user.name");
        String host = origHost.toLowerCase();
        exchangeIdentification();
        if (compatibility.isCompat20()) {
            keyExchange2(host);
            Log.getLogInstance().TODO("SSHConnection.login() user auth");
        } else {
            keyExchange1(host);
            Log.getLogInstance().TODO("SSHConnection.login() params to auth");
            userAuth1(localUser, serverUser, host);
        }
    }

    /**
     * Waits for the server identification string, and sends our own
     * identification string.
     *      @throws SSHException if read fails
     *      @throws IOException if read fails
     */
    private void exchangeIdentification() throws SSHException, IOException {
        byte[] buf = new byte[256];
        int minor1 = PROTOCOL_MINOR_1;
        for (; ; ) {
            for (int i = 0; i < buf.length - 1; i++) {
                int len = packet.atomicRead(buf, i, 1);
                if (len != 1) {
                    throw new SSHException("SSHConnection:exchangeIdentification(): Connection closed by remote host");
                }
                if (buf[i] == '\r') {
                    buf[i] = '\n';
                    buf[i + 1] = 0;
                    continue;
                }
                if (buf[i] == '\n') {
                    buf[i + 1] = 0;
                    break;
                }
            }
            buf[buf.length - 1] = 0;
            String str = new String(buf, 0, 4);
            if (str.equals("SSH-")) {
                break;
            }
            Log.getLogInstance().debug("SSHConnection:exchangeIdentification(): " + new String(buf));
        }
        String serverVersionString = new String(buf);
        int remoteMajor = 0, remoteMinor = 0;
        String remoteVersion = "";
        try {
            MessageFormat fmt = new MessageFormat("SSH-{0,number,integer}.{1,number,integer}-{2}\n");
            Object[] params = fmt.parse(serverVersionString);
            remoteMajor = ((Long) params[0]).intValue();
            remoteMinor = ((Long) params[1]).intValue();
            remoteVersion = (String) params[2];
        } catch (ParseException e) {
            Log.getLogInstance().fatal("Bad remote protocol version identification: " + serverVersionString);
        }
        Log.getLogInstance().debug("Remote protocol version " + remoteMajor + "." + remoteMinor + ", remote software version " + remoteVersion);
        compatibility.dataFellows(remoteVersion);
        boolean mismatch = false;
        switch(remoteMajor) {
            case 1:
                if (remoteMinor == 99 && (options.getProtocol() & Compatibility.SSH_PROTO_2) != 0 && (options.getProtocol() & Compatibility.SSH_PROTO_1_PREFERRED) == 0) {
                    compatibility.setCompat20(true);
                    break;
                }
                if ((options.getProtocol() & Compatibility.SSH_PROTO_1) == 0) {
                    mismatch = true;
                    break;
                }
                if (remoteMinor < 3) {
                    Log.getLogInstance().fatal("Remote machine has too old SSH software version.");
                } else if (remoteMinor == 3 || remoteMinor == 4) {
                    compatibility.setCompat13(true);
                    minor1 = 3;
                    if (options.isForwardAgent()) {
                        Log.getLogInstance().log("Agent forwarding disabled for protocol 1.3");
                        options.setForwardAgent(false);
                    }
                }
                break;
            case 2:
                if ((options.getProtocol() & Compatibility.SSH_PROTO_2) != 0) {
                    compatibility.setCompat20(true);
                    break;
                }
            default:
                mismatch = true;
                break;
        }
        if (mismatch) {
            Log.getLogInstance().fatal("Protocol major versions differ: " + ((options.getProtocol() & Compatibility.SSH_PROTO_2) != 0 ? PROTOCOL_MAJOR_2 : PROTOCOL_MAJOR_1) + " vs. " + remoteMajor);
        }
        String clientVersionString = "SSH-" + (compatibility.isCompat20() ? PROTOCOL_MAJOR_2 : PROTOCOL_MAJOR_1) + "." + (compatibility.isCompat20() ? PROTOCOL_MINOR_2 : minor1) + "-" + SSH_VERSION + "\n";
        packet.atomicWrite(clientVersionString.getBytes(), 0, clientVersionString.length());
        int index = clientVersionString.indexOf("\n");
        if (index >= 0) {
            clientVersionString = clientVersionString.substring(0, index);
        }
        Log.getLogInstance().log("TODO: SSHConnection.java:exchangeIdentification() chop server string");
        Log.getLogInstance().debug("Local version string " + clientVersionString);
    }

    /**
     *  Get the packet handler.
     *      @returns the packet handler
     */
    public Packet getPacketHandler() {
        return packet;
    }

    /**
     *  Key exchange.
     *      @param host the host to exchange with
     *      @throws IOException TODO
     *      @throws SSHException TODO
     */
    private void keyExchange1(String host) throws IOException, SSHException {
        byte[] sessionKey = new byte[SSH_SESSION_KEY_LENGTH];
        int sshCipherDefault = CipherDetails.SSH_CIPHER_3DES;
        Log.getLogInstance().debug("Waiting for server public key.");
        packet.readExpect(SSH_SMSG_PUBLIC_KEY);
        byte[] cookie = new byte[8];
        for (int i = 0; i < 8; i++) {
            cookie[i] = packet.getChar();
        }
        int bits = packet.getInt();
        int startLength = packet.getIncomingPacket().getLength();
        PublicKey publicKey = new PublicKey();
        publicKey.setExponent(packet.getBigNum());
        publicKey.setModulus(packet.getBigNum());
        int sumLen = startLength - packet.getIncomingPacket().getLength();
        int pbits = publicKey.getModulus().bitLength();
        if (bits != pbits) {
            Log.getLogInstance().log("Warning: Server lies about size of server public key: " + "actual size is " + pbits + " bits vs. announced " + bits + ".");
            Log.getLogInstance().log("Warning: This may be due to an old implementation of ssh.");
        }
        PublicKey hostKey = new PublicKey();
        bits = packet.getInt();
        startLength = packet.getIncomingPacket().getLength();
        hostKey.setExponent(packet.getBigNum());
        hostKey.setModulus(packet.getBigNum());
        sumLen += (startLength - packet.getIncomingPacket().getLength());
        int hbits = hostKey.getModulus().bitLength();
        if (bits != hbits) {
            Log.getLogInstance().log("Warning: Server lies about size of server host key: " + "actual size is " + hbits + " bits vs. announced " + bits + ".");
            Log.getLogInstance().log("Warning: This may be due to an old implementation of ssh.");
        }
        int serverFlags = packet.getInt();
        packet.setProtocolFlags(serverFlags);
        int supportedCiphers = packet.getInt();
        supportedAuthentications = packet.getInt();
        packet.checkEOM();
        Log.getLogInstance().debug("Received server public key (" + pbits + " bits) and host key (" + hbits + " bits).");
        if (verifyHostKey(host, hostKey) == false) {
            Log.getLogInstance().fatal("Host key verification failed");
        }
        int clientFlags = SSH_PROTOFLAG_SCREEN_NUMBER | SSH_PROTOFLAG_HOST_IN_FWD_OPEN;
        sessionId = computeSessionId(cookie, hostKey.getModulus(), publicKey.getModulus());
        Log.getLogInstance().log("TODO: SSHConnection.java:keyExchange() arc4random stir");
        long rand = 0;
        for (int i = 0; i < 32; i++) {
            if (i % 4 == 0) {
                rand = random.nextInt();
            }
            sessionKey[i] = (byte) (rand & 0xFF);
            rand >>= 8;
        }
        byte[] newKey = new byte[SSH_SESSION_KEY_LENGTH];
        for (int i = 0; i < SSH_SESSION_KEY_LENGTH; i++) {
            if (i < 16) {
                newKey[i] = (byte) ((sessionKey[i] ^ sessionId[i]) & 0xFF);
            } else {
                newKey[i] = sessionKey[i];
            }
        }
        BigInteger key = new BigInteger(1, newKey);
        if (publicKey.getModulus().compareTo(hostKey.getModulus()) < 0) {
            if (hostKey.getModulus().bitLength() < publicKey.getModulus().bitLength() + SSH_KEY_BITS_RESERVED) {
                Log.getLogInstance().fatal("SSHConnection:keyExchange1(): host_key " + hostKey.getModulus().bitLength() + " < public_key " + publicKey.getModulus().bitLength() + " + SSH_KEY_BITS_RESERVED " + SSH_KEY_BITS_RESERVED);
            }
            key = publicKey.rsaPublicEncrypt(key, random);
            key = hostKey.rsaPublicEncrypt(key, random);
        } else {
            if (publicKey.getModulus().bitLength() < hostKey.getModulus().bitLength() + SSH_KEY_BITS_RESERVED) {
                Log.getLogInstance().fatal("SSHConncetion:keyExchange1(): public_key " + publicKey.getModulus().bitLength() + " < host_key " + hostKey.getModulus().bitLength() + " + SSH_KEY_BITS_RESERVED " + SSH_KEY_BITS_RESERVED);
            }
            key = hostKey.rsaPublicEncrypt(key, random);
            key = publicKey.rsaPublicEncrypt(key, random);
        }
        publicKey = hostKey = null;
        if (options.getCipher() == CipherDetails.SSH_CIPHER_NOT_SET) {
            if ((CipherDetails.maskSsh1(true) & supportedCiphers & (1 << sshCipherDefault)) != 0) {
                options.setCipher(sshCipherDefault);
            }
        } else if (options.getCipher() == CipherDetails.SSH_CIPHER_ILLEGAL || (CipherDetails.maskSsh1(true) & (1 << options.getCipher())) == 0) {
            Log.getLogInstance().log("No valid SSH1 cipher, using " + CipherFactory.getCipherName(sshCipherDefault) + " instead.");
            options.setCipher(sshCipherDefault);
        }
        if ((supportedCiphers & (1 << options.getCipher())) == 0) {
            Log.getLogInstance().fatal("Selected cipher type " + CipherFactory.getCipherName(options.getCipher()) + " not supported by server.");
        }
        Log.getLogInstance().debug("Encryption type: " + CipherFactory.getCipherName(options.getCipher()));
        packet.start(SSH_CMSG_SESSION_KEY);
        packet.putChar((byte) options.getCipher());
        for (int i = 0; i < 8; i++) {
            packet.putChar(cookie[i]);
        }
        packet.putBigNum(key);
        key = null;
        packet.putInt(clientFlags);
        packet.send();
        packet.writeWait();
        Log.getLogInstance().debug("Sent encrypted session key.");
        packet.setEncryptionKey(sessionKey, SSH_SESSION_KEY_LENGTH, options.getCipher());
        sessionKey = null;
        packet.readExpect(SSH_SMSG_SUCCESS);
        Log.getLogInstance().debug("Received encrypted confirmation.");
    }

    /**
     *  Authenticate user.
     *      @param localUser the user on this machine
     *      @param serverUser the user on the remote machine
     *      @param host the remote host
     */
    private void userAuth1(String localUser, String serverUser, String host) {
        Log.getLogInstance().log("TODO: SSHConnection.java:userAuth() authenticate user");
    }

    /**
     *  Verify a host key is valid.
     *      @param host the host received from
     *      @param key the key received
     *      @returns whether it is valid or not
     */
    private boolean verifyHostKey(String host, PublicKey key) {
        Log.getLogInstance().log("TODO: SSHConnection.java:verifyHostKey() verify key");
        return true;
    }

    /**
     *  Compute the session ID using MD5. TODO fn belongs elsewhere?
     *      @param cookie the session cookie
     *      @param hostKeyN the host key
     *      @param sessionKeyN the host public key
     *      @returns the session ID
     */
    private byte[] computeSessionId(byte[] cookie, BigInteger hostKeyN, BigInteger sessionKeyN) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(hostKeyN.abs().toByteArray());
            md.update(sessionKeyN.abs().toByteArray());
            md.update(cookie, 0, 8);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.getLogInstance().fatal("MD5 not supported by security provider.");
            return null;
        }
    }

    /**
     *  Key exchange.
     *      @param host the host to exchange with
     *      @throws IOException TODO
     *      @throws SSHException TODO
     */
    private void keyExchange2(String host) throws IOException, SSHException {
        if (options.getCiphers() == null) {
            Log.getLogInstance().log("No valid ciphers for protocol version 2 given, using defaults.");
        }
        Log.getLogInstance().TODO("SSHConnection.keyExhcange2() do key exchange");
    }
}
