package br.eti.mps.chiroptera.lib;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import br.eti.mps.chiroptera.constants.Constants;
import br.eti.mps.chiroptera.lib.parsers.ParseStatusDirector;

/**
 *
 * @author <a href="mailto:mpserafim[at]gmail[dot]com">Marcos Paulo Serafim</a>
 */
public class Connection {

    private Socket socket;

    private BufferedOutputStream out;

    private InputStream in;

    private boolean authenticated;

    private boolean haveMessages;

    private Map<String, Object> configuration;

    public Connection(Map<String, Object> configuration) throws ChiropteraException {
        this.configuration = configuration;
        authenticated = false;
        haveMessages = false;
        String address = (String) configuration.get(Constants.Chiroptera.ConfigurationKeys.HOST_ADDRESS);
        int port = (Integer) configuration.get(Constants.Chiroptera.ConfigurationKeys.HOST_PORT);
        String password = (String) configuration.get(Constants.Chiroptera.ConfigurationKeys.PASSWORD);
        connect(address, port);
        authenticate(password);
    }

    public void disconnect() throws ChiropteraException {
        try {
            socket.close();
        } catch (IOException e) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.IO, e.getMessage(), e);
        }
    }

    private void connect(String address, int port) throws ChiropteraException {
        try {
            socket = new Socket(address, port);
            out = new BufferedOutputStream(socket.getOutputStream());
            in = socket.getInputStream();
            socket.setKeepAlive(true);
        } catch (IOException e) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.IO, e.getMessage(), e);
        }
    }

    private void authenticate(String password) throws ChiropteraException {
        Data data = new Data(Constants.Connection.Messages.HELLO);
        Data d = sendReceive(data, false);
        String tokens[] = d.getData().split(Constants.SPACE);
        if (!tokens[0].equalsIgnoreCase(Constants.Connection.Tokens.AUTH)) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.AUTHENTICATION_FAILED, d.getMessage(), null);
        }
        if (!tokens[1].equalsIgnoreCase(Constants.Connection.Tokens.CRAM_MD5)) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.NOT_HANDLED, d.getMessage(), null);
        }
        String challenger = tokens[2];
        data = new Data(generateHash(password, challenger));
        d = sendReceive(data, false);
        if (d.getReturnCode() != Constants.Connection.ReturnCodes.SUCCESS) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.AUTHENTICATION_FAILED, d.getMessage(), null);
        }
        Random random = new Random();
        StringBuffer auth = new StringBuffer(Constants.Connection.Tokens.AUTH).append(Constants.SPACE);
        auth.append(Constants.Connection.Tokens.CRAM_MD5).append(Constants.SPACE);
        challenger = new StringBuffer(Constants.LESS_THAN).append(Math.abs(random.nextInt())).append(Constants.DOT).append(Math.abs(random.nextInt())).append(Constants.AT).append(getHostname()).append(Constants.GREATHER_THAN).toString();
        auth.append(challenger).append(Constants.SPACE);
        auth.append(Constants.Connection.Tokens.SSL).append(Constants.EQUAL).append(Constants.ZERO).append(Constants.CR);
        data = new Data(auth.toString());
        d = sendReceive(data, false);
        challenger = generateHash(password, challenger);
        if (!d.getData().equals(challenger)) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.AUTHENTICATION_FAILED, d.getMessage(), null);
        }
        data = new Data(Constants.Connection.Messages.AUTH_OK);
        d = sendReceive(data, false);
        if (d.getReturnCode() != Constants.Connection.ReturnCodes.SUCCESS) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.AUTHENTICATION_FAILED, d.getMessage(), null);
        }
        authenticated = true;
    }

    private void testConnected() throws ChiropteraException {
        if (!socket.isConnected()) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.NOT_CONNECTED, null, null);
        }
        if (!authenticated) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.NOT_AUTHENTICATED, null, null);
        }
    }

    public List<String> getClients() throws ChiropteraException {
        return getSimpleList(Constants.Connection.DotCommands.CLIENTS);
    }

    public List<String> getJobs() throws ChiropteraException {
        return getSimpleList(Constants.Connection.DotCommands.JOBS);
    }

    public List<String> getLevels() throws ChiropteraException {
        return getSimpleList(Constants.Connection.DotCommands.LEVELS);
    }

    public List<String> getFileSets() throws ChiropteraException {
        return getSimpleList(Constants.Connection.DotCommands.FILESETS);
    }

    public List<String> getPools() throws ChiropteraException {
        return getSimpleList(Constants.Connection.DotCommands.POOLS);
    }

    public List<String> getSimpleList(String command) throws ChiropteraException {
        testConnected();
        List<String> list = new ArrayList<String>();
        Data data = new Data(command);
        Data d = sendReceive(data, true);
        String lines[] = d.getData().split(Constants.CR);
        for (int i = 0; i < lines.length - 1; i++) {
            list.add(lines[i]);
        }
        if (!thereIsMessages(lines[lines.length - 1])) {
            list.add(lines[lines.length - 1]);
        }
        return list;
    }

    public String getMessages() throws ChiropteraException {
        Data data = new Data(Constants.Connection.DotCommands.MESSAGES);
        Data d = sendReceive(data, true);
        return d.getData();
    }

    public String getVersion() throws ChiropteraException {
        Data data = new Data(Constants.Connection.Commands.VERSION);
        Data d = sendReceive(data, true);
        return d.getData();
    }

    public StatusDirector getStatusDirector() throws ChiropteraException {
        Data data = new Data(Constants.Connection.Commands.STATUS_DIRECTOR);
        Data d = sendReceive(data, true);
        System.out.println(d.getData());
        return ParseStatusDirector.parse(configuration, d.getData());
    }

    private boolean thereIsMessages(String s) {
        if (s.equalsIgnoreCase(Constants.Connection.Messages.YOU_HAVE_MESSAGES)) {
            haveMessages = true;
            return true;
        }
        return false;
    }

    private String getHostname() {
        String hostname;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException uhe) {
            hostname = Constants.Chiroptera.APP_NAME;
        }
        return hostname;
    }

    private String generateHash(String key, String data) throws ChiropteraException {
        try {
            MessageDigest md = MessageDigest.getInstance(Constants.Connection.Auth.MD5);
            md.update(key.getBytes());
            byte[] raw = md.digest();
            String s = toHexString(raw);
            SecretKey skey = new SecretKeySpec(s.getBytes(), Constants.Connection.Auth.HMACMD5);
            Mac mac = Mac.getInstance(skey.getAlgorithm());
            mac.init(skey);
            byte digest[] = mac.doFinal(data.getBytes());
            String digestB64 = BaculaBase64.binToBase64(digest);
            return digestB64.substring(0, digestB64.length());
        } catch (NoSuchAlgorithmException e) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.HASH, e.getMessage(), e);
        } catch (InvalidKeyException e) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.HASH, e.getMessage(), e);
        }
    }

    public String executeCommand(String command) throws ChiropteraException {
        Data data = new Data(command);
        boolean isExit = command.equalsIgnoreCase(Constants.Connection.Commands.EXIT) || command.equalsIgnoreCase(Constants.Connection.Commands.QUIT);
        try {
            data = sendReceive(data, true);
        } catch (ChiropteraException c) {
            if (isExit && c.getErrorCode() == Constants.Chiroptera.Errors.INVALID_DATA_SIZE) {
                disconnect();
            } else {
                throw c;
            }
        }
        return data.getData();
    }

    private synchronized Data sendReceive(Data data, boolean handleSignals) throws ChiropteraException {
        if (!socket.isConnected() || socket.isClosed()) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.NOT_CONNECTED, null, null);
        }
        Data d = new Data();
        StringBuffer serverData = new StringBuffer();
        byte buffer[] = new byte[Constants.Bacula.MAX_PACKET_SIZE];
        int available, i;
        byte dataSize[] = toByteArray(data.getDataSize());
        try {
            out.write(dataSize);
            out.write(data.getData().getBytes());
            out.flush();
            while (true) {
                i = in.read(dataSize, 0, 4);
                if (i < 4) {
                    throw new ChiropteraException(Constants.Chiroptera.Errors.INVALID_DATA_SIZE, null, null);
                }
                available = toInt(dataSize);
                if (available < 0) {
                    d.setSignal(available);
                    break;
                } else if (available > buffer.length) {
                    throw new ChiropteraException(Constants.Chiroptera.Errors.INVALID_DATA_SIZE, null, null);
                }
                while (available > 0) {
                    i = in.read(buffer, 0, available);
                    serverData.append(new String(buffer, 0, i));
                    available -= i;
                }
                if (!handleSignals) {
                    break;
                }
            }
        } catch (IOException ioe) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.IO, ioe.getMessage(), ioe);
        }
        d.setData(serverData.toString());
        return d;
    }

    private byte[] toByteArray(int integer) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) ((integer >> 24) & 0xFF);
        byteArray[1] = (byte) ((integer >> 16) & 0xFF);
        byteArray[2] = (byte) ((integer >> 8) & 0xFF);
        byteArray[3] = (byte) (integer & 0xFF);
        return byteArray;
    }

    private int toInt(byte byteArray[]) throws ChiropteraException {
        if (byteArray.length != 4) {
            throw new ChiropteraException(Constants.Chiroptera.Errors.NO_INT, null, null);
        }
        short b;
        int result = 0;
        for (int i = 0; i < 4; i++) {
            b = (short) byteArray[i];
            b &= 0x00FF;
            result <<= 8;
            result |= (int) b;
        }
        return result;
    }

    private String toHexString(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; ++i) {
            sb.append(Integer.toHexString((b[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public boolean haveMessages() {
        return haveMessages;
    }
}
