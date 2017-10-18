package gnu.saw.server.connection;

import gnu.saw.SAW;
import gnu.saw.stream.SAWLittleEndianInputStream;
import gnu.saw.stream.SAWLittleEndianOutputStream;
import gnu.saw.stream.SAWMultiplexingInputStream;
import gnu.saw.stream.SAWMultiplexingOutputStream;
import gnu.saw.terminal.SAWTerminal;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;

public class SAWServerConnection {

    private static byte[] serverCheckString;

    private static byte[] clientCheckString;

    static {
        try {
            serverCheckString = "SATAN-ANYWHERE SERVER".getBytes("UTF-8");
            clientCheckString = "SATAN-ANYWHERE CLIENT".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
    }

    private int encryptionType;

    private int remaining;

    private byte[] encryptionKey;

    private byte[] digestedKey;

    private byte[] digestedIv;

    private byte[] localNonce = new byte[512];

    private byte[] remoteNonce = new byte[512];

    private MessageDigest sha256Digester;

    private SecureRandom secureRandom;

    private Cipher encryptionCipher;

    private Cipher decryptionCipher;

    private SecretKeySpec encryptionKeySpec;

    private SecretKeySpec decryptionKeySpec;

    private IvParameterSpec encryptionIvParameterSpec;

    private IvParameterSpec decryptionIvParameterSpec;

    private Socket connectionSocket;

    private SAWMultiplexingOutputStream multiplexedConnectionOutputStream;

    private SAWMultiplexingInputStream multiplexedConnectionInputStream;

    private InputStream connectionSocketInputStream;

    private InputStream connectionInputStream;

    private InputStream authenticationInputStream;

    private InputStream shellInputStream;

    private InputStream fileTransferControlInputStream;

    private InputStream fileTransferDataInputStream;

    private InputStream graphicsControlInputStream;

    private InputStream graphicsImageInputStream;

    private InputStream graphicsClipboardInputStream;

    private OutputStream connectionSocketOutputStream;

    private OutputStream connectionOutputStream;

    private OutputStream authenticationOutputStream;

    private OutputStream shellOutputStream;

    private OutputStream fileTransferControlOutputStream;

    private OutputStream fileTransferDataOutputStream;

    private OutputStream graphicsControlOutputStream;

    private OutputStream graphicsImageOutputStream;

    private OutputStream graphicsClipboardOutputStream;

    private ZInputStream zShellInflater;

    private ZInputStream zImageInflater;

    private ZInputStream zClipboardInflater;

    private ZOutputStream zShellDeflater;

    private ZOutputStream zImageDeflater;

    private ZOutputStream zClipboardDeflater;

    private BufferedReader authenticationReader;

    private BufferedWriter authenticationWriter;

    private BufferedReader commandReader;

    private BufferedWriter resultWriter;

    private SAWLittleEndianInputStream fileTransferControlDataInputStream;

    private SAWLittleEndianOutputStream fileTransferControlDataOutputStream;

    private SAWLittleEndianInputStream graphicsControlDataInputStream;

    private SAWLittleEndianOutputStream graphicsControlDataOutputStream;

    public SAWServerConnection() {
    }

    public byte[] getLocalNonce() {
        return localNonce;
    }

    public byte[] getRemoteNonce() {
        return remoteNonce;
    }

    public void setEncryptionType(int encryptionType) {
        this.encryptionType = encryptionType;
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public Socket getConnectionSocket() {
        return connectionSocket;
    }

    public void setConnectionSocket(Socket connectionSocket) {
        this.connectionSocket = connectionSocket;
    }

    public InputStream getAuthenticationInputStream() {
        return authenticationInputStream;
    }

    public InputStream getShellInputStream() {
        return shellInputStream;
    }

    public InputStream getFileTransferControlInputStream() {
        return fileTransferControlInputStream;
    }

    public InputStream getFileTransferDataInputStream() {
        return fileTransferDataInputStream;
    }

    public InputStream getGraphicsControlInputStream() {
        return graphicsControlInputStream;
    }

    public InputStream getGraphicsImageInputStream() {
        return graphicsImageInputStream;
    }

    public OutputStream getAuthenticationOutputStream() {
        return authenticationOutputStream;
    }

    public OutputStream getShellOutputStream() {
        return shellOutputStream;
    }

    public OutputStream getFileTransferControlOutputStream() {
        return fileTransferControlOutputStream;
    }

    public OutputStream getFileTransferDataOutputStream() {
        return fileTransferDataOutputStream;
    }

    public OutputStream getGraphicsControlOutputStream() {
        return graphicsControlOutputStream;
    }

    public OutputStream getGraphicsImageOutputStream() {
        return graphicsImageOutputStream;
    }

    public BufferedReader getCommandReader() {
        return commandReader;
    }

    public BufferedReader getAuthenticationReader() {
        return authenticationReader;
    }

    public BufferedWriter getResultWriter() {
        return resultWriter;
    }

    public BufferedWriter getAuthenticationWriter() {
        return authenticationWriter;
    }

    public SAWLittleEndianInputStream getFileTransferControlDataInputStream() {
        return fileTransferControlDataInputStream;
    }

    public SAWLittleEndianOutputStream getFileTransferControlDataOutputStream() {
        return fileTransferControlDataOutputStream;
    }

    public SAWLittleEndianInputStream getGraphicsControlDataInputStream() {
        return graphicsControlDataInputStream;
    }

    public SAWLittleEndianOutputStream getGraphicsControlDataOutputStream() {
        return graphicsControlDataOutputStream;
    }

    public InputStream getGraphicsImageDataInputStream() {
        return zImageInflater;
    }

    public OutputStream getGraphicsImageDataOutputStream() {
        return zImageDeflater;
    }

    public InputStream getGraphicsClipboardInputStream() {
        return graphicsClipboardInputStream;
    }

    public OutputStream getGraphicsClipboardOutputStream() {
        return graphicsClipboardOutputStream;
    }

    public InputStream getGraphicsClipboardDataInputStream() {
        return zClipboardInflater;
    }

    public OutputStream getGraphicsClipboardDataOutputStream() {
        return zClipboardDeflater;
    }

    public void closeSockets() {
        if (connectionSocket != null) {
            try {
                connectionSocket.close();
            } catch (IOException e) {
            }
        }
        if (multiplexedConnectionInputStream != null) {
            try {
                multiplexedConnectionInputStream.stopPacketReader();
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }
        }
        if (multiplexedConnectionOutputStream != null) {
            try {
                multiplexedConnectionOutputStream.close();
            } catch (IOException e) {
            }
        }
    }

    public void closeConnection() {
        SAWTerminal.print("\rSAW>SAWSERVER:Connection with client closed!\nSAW>");
        closeSockets();
    }

    public boolean isConnected() {
        return connectionSocket.isConnected();
    }

    public void exchangeNonces() throws IOException {
        connectionSocketInputStream = connectionSocket.getInputStream();
        connectionSocketOutputStream = connectionSocket.getOutputStream();
        secureRandom = new SecureRandom();
        secureRandom.nextBytes(localNonce);
        connectionSocket.setSoTimeout(10000);
        connectionSocketOutputStream.write(localNonce);
        connectionSocketOutputStream.flush();
        remaining = remoteNonce.length;
        while (remaining > 0) {
            remaining -= connectionSocketInputStream.read(remoteNonce, 0, remaining);
        }
    }

    public void setSocketStreams() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        if (encryptionType == SAW.SAW_CONNECTION_ENCRYPT_NONE) {
            connectionInputStream = connectionSocketInputStream;
            connectionOutputStream = connectionSocketOutputStream;
        } else if (encryptionType == SAW.SAW_CONNECTION_ENCRYPT_RC4) {
            sha256Digester = MessageDigest.getInstance("SHA-256");
            encryptionCipher = Cipher.getInstance("RC4");
            decryptionCipher = Cipher.getInstance("RC4");
            sha256Digester.update(localNonce);
            sha256Digester.update(remoteNonce);
            digestedKey = sha256Digester.digest(encryptionKey);
            encryptionKeySpec = new SecretKeySpec(digestedKey, 0, 16, "RC4");
            decryptionKeySpec = new SecretKeySpec(digestedKey, 16, 16, "RC4");
            encryptionCipher.init(Cipher.ENCRYPT_MODE, encryptionKeySpec);
            decryptionCipher.init(Cipher.DECRYPT_MODE, decryptionKeySpec);
            connectionInputStream = new CipherInputStream(connectionSocketInputStream, decryptionCipher);
            connectionOutputStream = new CipherOutputStream(connectionSocketOutputStream, encryptionCipher);
        } else if (encryptionType == SAW.SAW_CONNECTION_ENCRYPT_AES) {
            sha256Digester = MessageDigest.getInstance("SHA-256");
            encryptionCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            decryptionCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            sha256Digester.update(localNonce);
            sha256Digester.update(remoteNonce);
            digestedKey = sha256Digester.digest(encryptionKey);
            encryptionKeySpec = new SecretKeySpec(digestedKey, 0, 16, "AES");
            decryptionKeySpec = new SecretKeySpec(digestedKey, 16, 16, "AES");
            digestedIv = sha256Digester.digest(digestedKey);
            encryptionIvParameterSpec = new IvParameterSpec(digestedIv, 0, 16);
            decryptionIvParameterSpec = new IvParameterSpec(digestedIv, 16, 16);
            encryptionCipher.init(Cipher.ENCRYPT_MODE, encryptionKeySpec, encryptionIvParameterSpec);
            decryptionCipher.init(Cipher.DECRYPT_MODE, decryptionKeySpec, decryptionIvParameterSpec);
            connectionInputStream = new CipherInputStream(connectionSocketInputStream, decryptionCipher);
            connectionOutputStream = new CipherOutputStream(connectionSocketOutputStream, encryptionCipher);
        }
        multiplexedConnectionInputStream = new SAWMultiplexingInputStream(connectionInputStream, 7, 1024, 8192, false);
        multiplexedConnectionOutputStream = new SAWMultiplexingOutputStream(connectionOutputStream, 7, 1024, false, true, false);
        authenticationInputStream = multiplexedConnectionInputStream.getInputStream(0);
        authenticationOutputStream = multiplexedConnectionOutputStream.getOutputStream(0);
        shellInputStream = multiplexedConnectionInputStream.getInputStream(1);
        shellOutputStream = multiplexedConnectionOutputStream.getOutputStream(1);
        fileTransferControlInputStream = multiplexedConnectionInputStream.getInputStream(2);
        fileTransferControlOutputStream = multiplexedConnectionOutputStream.getOutputStream(2);
        fileTransferDataInputStream = multiplexedConnectionInputStream.getInputStream(3);
        fileTransferDataOutputStream = multiplexedConnectionOutputStream.getOutputStream(3);
        graphicsControlInputStream = multiplexedConnectionInputStream.getInputStream(4);
        graphicsControlOutputStream = multiplexedConnectionOutputStream.getOutputStream(4);
        graphicsImageInputStream = multiplexedConnectionInputStream.getInputStream(5);
        graphicsImageOutputStream = multiplexedConnectionOutputStream.getOutputStream(5);
        graphicsClipboardInputStream = multiplexedConnectionInputStream.getInputStream(6);
        graphicsClipboardOutputStream = multiplexedConnectionOutputStream.getOutputStream(6);
        authenticationReader = new BufferedReader(new InputStreamReader(authenticationInputStream, "UTF-8"));
        authenticationWriter = new BufferedWriter(new OutputStreamWriter(authenticationOutputStream, "UTF-8"));
        zShellDeflater = new ZOutputStream(shellOutputStream, JZlib.Z_DEFAULT_COMPRESSION, true, 4096);
        zShellDeflater.setFlushMode(JZlib.Z_SYNC_FLUSH);
        resultWriter = new BufferedWriter(new OutputStreamWriter(zShellDeflater, "UTF-8"));
        zShellInflater = new ZInputStream(shellInputStream, true, 4096);
        zShellInflater.setFlushMode(JZlib.Z_SYNC_FLUSH);
        commandReader = new BufferedReader(new InputStreamReader(zShellInflater, "UTF-8"));
        zImageInflater = new ZInputStream(graphicsImageInputStream, true, 4096);
        zImageInflater.setFlushMode(JZlib.Z_SYNC_FLUSH);
        zImageDeflater = new ZOutputStream(graphicsImageOutputStream, JZlib.Z_DEFAULT_COMPRESSION, true, 4096);
        zImageDeflater.setFlushMode(JZlib.Z_SYNC_FLUSH);
        zClipboardInflater = new ZInputStream(graphicsClipboardInputStream, true, 4096);
        zClipboardInflater.setFlushMode(JZlib.Z_SYNC_FLUSH);
        zClipboardDeflater = new ZOutputStream(graphicsClipboardOutputStream, JZlib.Z_DEFAULT_COMPRESSION, true, 4096);
        zClipboardDeflater.setFlushMode(JZlib.Z_SYNC_FLUSH);
        fileTransferControlDataInputStream = new SAWLittleEndianInputStream(new BufferedInputStream(fileTransferControlInputStream));
        fileTransferControlDataOutputStream = new SAWLittleEndianOutputStream(new BufferedOutputStream(fileTransferControlOutputStream));
        graphicsControlDataInputStream = new SAWLittleEndianInputStream(new BufferedInputStream(graphicsControlInputStream));
        graphicsControlDataOutputStream = new SAWLittleEndianOutputStream(new BufferedOutputStream(graphicsControlOutputStream));
    }

    public boolean verifyConnection() throws IOException {
        connectionOutputStream.write(serverCheckString);
        connectionOutputStream.flush();
        for (int i = 0; i < clientCheckString.length; i++) {
            if (connectionInputStream.read() != clientCheckString[i]) {
                connectionSocket.setSoTimeout(0);
                return false;
            }
        }
        connectionSocket.setSoTimeout(0);
        return true;
    }

    public void startConnection() {
        multiplexedConnectionInputStream.startPacketReader();
    }

    public void resetGraphicsModeStreams() throws IOException {
        multiplexedConnectionOutputStream.open(5);
        zImageDeflater = new ZOutputStream(graphicsImageOutputStream, JZlib.Z_DEFAULT_COMPRESSION, true, 4096);
        zImageDeflater.setFlushMode(JZlib.Z_SYNC_FLUSH);
        resetClipboardStreams();
    }

    public void resetClipboardStreams() throws IOException {
        multiplexedConnectionInputStream.open(6);
        multiplexedConnectionOutputStream.open(6);
        zClipboardInflater = new ZInputStream(graphicsClipboardInputStream, true, 4096);
        zClipboardInflater.setFlushMode(JZlib.Z_SYNC_FLUSH);
        zClipboardDeflater = new ZOutputStream(graphicsClipboardOutputStream, JZlib.Z_DEFAULT_COMPRESSION, true, 4096);
        zClipboardDeflater.setFlushMode(JZlib.Z_SYNC_FLUSH);
    }

    public void resetFileTransferStreams() throws IOException {
        multiplexedConnectionInputStream.open(3);
        multiplexedConnectionOutputStream.open(3);
    }
}
