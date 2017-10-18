package de.mud.ssh;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Secure Shell IO
 * @author Marcus Meissner
 * @version $Id: SshIO.java 506 2005-10-25 10:07:21Z marcus $
 */
public abstract class SshIO {

    private static MessageDigest md5;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SshIO: unable to load message digest algorithm: " + e);
            e.printStackTrace();
        }
    }

    /**
   * variables for the connection
   */
    private String idstr = "";

    private String idstr_sent = "SSH/JTA (c) Marcus Meissner, Matthias L. Jugel\n";

    /**
   * Debug level. This results in additional diagnostic messages on the
   * java console.
   */
    private static int debug = 0;

    /**
   * State variable for Ssh negotiation reader
   */
    private SshCrypto crypto = null;

    String cipher_type = "IDEA";

    private int remotemajor, remoteminor;

    private int mymajor, myminor;

    private int useprotocol;

    private String login = "", password = "";

    public String dataToSend = null;

    public String hashHostKey = null;

    byte lastPacketSentType;

    private int phase = 0;

    private final int PHASE_INIT = 0;

    private final int PHASE_SSH_RECEIVE_PACKET = 1;

    BigInteger rsa_e, rsa_n;

    private final byte SSH_MSG_DISCONNECT = 1;

    private final byte SSH_SMSG_PUBLIC_KEY = 2;

    private final byte SSH_CMSG_SESSION_KEY = 3;

    private final byte SSH_CMSG_USER = 4;

    private final byte SSH_CMSG_AUTH_PASSWORD = 9;

    private final byte SSH_CMSG_REQUEST_PTY = 10;

    private final byte SSH_CMSG_WINDOW_SIZE = 11;

    private final byte SSH_CMSG_EXEC_SHELL = 12;

    private final byte SSH_SMSG_SUCCESS = 14;

    private final byte SSH_SMSG_FAILURE = 15;

    private final byte SSH_CMSG_STDIN_DATA = 16;

    private final byte SSH_SMSG_STDOUT_DATA = 17;

    private final byte SSH_SMSG_STDERR_DATA = 18;

    private final byte SSH_SMSG_EXITSTATUS = 20;

    private final byte SSH_MSG_IGNORE = 32;

    private final byte SSH_CMSG_EXIT_CONFIRMATION = 33;

    private final byte SSH_MSG_DEBUG = 36;

    private final byte SSH2_MSG_DISCONNECT = 1;

    private final byte SSH2_MSG_IGNORE = 2;

    private final byte SSH2_MSG_SERVICE_REQUEST = 5;

    private final byte SSH2_MSG_SERVICE_ACCEPT = 6;

    private final byte SSH2_MSG_KEXINIT = 20;

    private final byte SSH2_MSG_NEWKEYS = 21;

    private final byte SSH2_MSG_KEXDH_INIT = 30;

    private final byte SSH2_MSG_KEXDH_REPLY = 31;

    private String kexalgs, hostkeyalgs, encalgs2c, encalgc2s, macalgs2c, macalgc2s, compalgc2s, compalgs2c, langc2s, langs2;

    private int outgoingseq = 0, incomingseq = 0;

    private int SSH_CIPHER_NONE = 0;

    private int SSH_CIPHER_IDEA = 1;

    private int SSH_CIPHER_DES = 2;

    private int SSH_CIPHER_3DES = 3;

    private int SSH_CIPHER_TSS = 4;

    private int SSH_CIPHER_RC4 = 5;

    private int SSH_CIPHER_BLOWFISH = 6;

    private final int SSH_AUTH_RHOSTS = 1;

    private final int SSH_AUTH_RSA = 2;

    private final int SSH_AUTH_PASSWORD = 3;

    private final int SSH_AUTH_RHOSTS_RSA = 4;

    private boolean cansenddata = false;

    /**
   * Initialise SshIO
   */
    public SshIO() {
        crypto = null;
    }

    public void setLogin(String user) {
        if (user == null) user = "";
        login = user;
    }

    public void setPassword(String password) {
        if (password == null) password = "";
        this.password = password;
    }

    SshPacket currentpacket;

    protected abstract void write(byte[] buf) throws IOException;

    public abstract String getTerminalType();

    byte[] one = new byte[1];

    private void write(byte b) throws IOException {
        one[0] = b;
        write(one);
    }

    public void disconnect() {
        idstr = "";
        login = "";
        password = "";
        phase = 0;
        crypto = null;
    }

    public void setWindowSize(int columns, int rows) throws IOException {
        if (phase == PHASE_INIT) {
            System.err.println("sshio:setWindowSize(), sizing in init phase not supported.\n");
        }
        if (debug > 1) System.err.println("SSHIO:setWindowSize(" + columns + "," + rows + ")");
        Send_SSH_CMSG_WINDOW_SIZE(columns, rows);
    }

    public synchronized void sendData(String str) throws IOException {
        if (debug > 1) System.out.println("SshIO.send(" + str + ")");
        if (dataToSend == null) dataToSend = str; else dataToSend += str;
        if (cansenddata) {
            Send_SSH_CMSG_STDIN_DATA(dataToSend);
            dataToSend = null;
        }
    }

    /**
   * Read data from the remote host. Blocks until data is available.
   *
   * Returns an array of bytes that will be displayed.
   *
   */
    public byte[] handleSSH(byte buff[]) throws IOException {
        byte[] rest;
        String result;
        if (debug > 1) System.out.println("SshIO.getPacket(" + buff + "," + buff.length + ")");
        if (phase == PHASE_INIT) {
            byte b;
            int boffset = 0;
            while (boffset < buff.length) {
                b = buff[boffset++];
                idstr += (char) b;
                if (b == '\n') {
                    if (!idstr.substring(0, 4).equals("SSH-")) {
                        if (debug > 0) System.out.print("Received data line: " + idstr);
                        idstr = "";
                        continue;
                    }
                    phase++;
                    remotemajor = Integer.parseInt(idstr.substring(4, 5));
                    String minorverstr = idstr.substring(6, 8);
                    if (!Character.isDigit(minorverstr.charAt(1))) minorverstr = minorverstr.substring(0, 1);
                    remoteminor = Integer.parseInt(minorverstr);
                    System.out.println("remotemajor " + remotemajor);
                    System.out.println("remoteminor " + remoteminor);
                    if (remotemajor == 2) {
                        mymajor = 2;
                        myminor = 0;
                        useprotocol = 2;
                    } else {
                        if (false && (remoteminor == 99)) {
                            mymajor = 2;
                            myminor = 0;
                            useprotocol = 2;
                        } else {
                            mymajor = 1;
                            myminor = 5;
                            useprotocol = 1;
                        }
                    }
                    idstr_sent = "SSH-" + mymajor + "." + myminor + "-" + idstr_sent;
                    write(idstr_sent.getBytes());
                    if (useprotocol == 2) currentpacket = new SshPacket2(null); else currentpacket = new SshPacket1(null);
                }
            }
            if (boffset == buff.length) return "".getBytes();
            return "Must not have left over data after PHASE_INIT!\n".getBytes();
        }
        result = "";
        rest = currentpacket.addPayload(buff);
        if (currentpacket.isFinished()) {
            if (useprotocol == 1) {
                result = result + handlePacket1((SshPacket1) currentpacket);
                currentpacket = new SshPacket1(crypto);
            } else {
                result = result + handlePacket2((SshPacket2) currentpacket);
                currentpacket = new SshPacket2(crypto);
            }
        }
        while (rest != null) {
            rest = currentpacket.addPayload(rest);
            if (currentpacket.isFinished()) {
                if (useprotocol == 1) {
                    result = result + handlePacket1((SshPacket1) currentpacket);
                    currentpacket = new SshPacket1(crypto);
                } else {
                    result = result + handlePacket2((SshPacket2) currentpacket);
                    currentpacket = new SshPacket2(crypto);
                }
            }
        }
        return result.getBytes();
    }

    /**
   * Handle SSH protocol Version 2
   *
   * @param p the packet we will process here.
   * @return a array of bytes
   */
    private String handlePacket2(SshPacket2 p) throws IOException {
        switch(p.getType()) {
            case SSH2_MSG_IGNORE:
                System.out.println("SSH2: SSH2_MSG_IGNORE");
                break;
            case SSH2_MSG_DISCONNECT:
                int discreason = p.getInt32();
                String discreason1 = p.getString();
                System.out.println("SSH2: SSH2_MSG_DISCONNECT(" + discreason + "," + discreason1 + "," + ")");
                return "\nSSH2 disconnect: " + discreason1 + "\n";
            case SSH2_MSG_NEWKEYS:
                {
                    System.out.println("SSH2: SSH2_MSG_NEWKEYS");
                    sendPacket2(new SshPacket2(SSH2_MSG_NEWKEYS));
                    byte[] session_key = new byte[16];
                    crypto = new SshCrypto(cipher_type, session_key);
                    SshPacket2 pn = new SshPacket2(SSH2_MSG_SERVICE_REQUEST);
                    pn.putString("ssh-userauth");
                    sendPacket2(pn);
                    break;
                }
            case SSH2_MSG_SERVICE_ACCEPT:
                {
                    System.out.println("Service Accept: " + p.getString());
                    break;
                }
            case SSH2_MSG_KEXINIT:
                {
                    byte[] fupp;
                    System.out.println("SSH2: SSH2_MSG_KEXINIT");
                    byte kexcookie[] = p.getBytes(16);
                    String kexalgs = p.getString();
                    System.out.println("- " + kexalgs);
                    String hostkeyalgs = p.getString();
                    System.out.println("- " + hostkeyalgs);
                    String encalgc2s = p.getString();
                    System.out.println("- " + encalgc2s);
                    String encalgs2c = p.getString();
                    System.out.println("- " + encalgs2c);
                    String macalgc2s = p.getString();
                    System.out.println("- " + macalgc2s);
                    String macalgs2c = p.getString();
                    System.out.println("- " + macalgs2c);
                    String compalgc2s = p.getString();
                    System.out.println("- " + compalgc2s);
                    String compalgs2c = p.getString();
                    System.out.println("- " + compalgs2c);
                    String langc2s = p.getString();
                    System.out.println("- " + langc2s);
                    String langs2c = p.getString();
                    System.out.println("- " + langs2c);
                    fupp = p.getBytes(1);
                    System.out.println("- first_kex_follows: " + fupp[0]);
                    SshPacket2 pn = new SshPacket2(SSH2_MSG_KEXINIT);
                    byte[] kexsend = new byte[16];
                    String ciphername;
                    pn.putBytes(kexsend);
                    pn.putString("diffie-hellman-group1-sha1");
                    pn.putString("ssh-rsa");
                    cipher_type = "NONE";
                    ciphername = "none";
                    pn.putString("none");
                    pn.putString("none");
                    pn.putString("hmac-md5");
                    pn.putString("hmac-md5");
                    pn.putString("none");
                    pn.putString("none");
                    pn.putString("");
                    pn.putString("");
                    pn.putByte((byte) 0);
                    pn.putInt32(0);
                    sendPacket2(pn);
                    pn = new SshPacket2(SSH2_MSG_KEXDH_INIT);
                    pn.putMpInt(BigInteger.valueOf(0xdeadbeef));
                    sendPacket2(pn);
                    break;
                }
            case SSH2_MSG_KEXDH_REPLY:
                {
                    String result;
                    System.out.println("SSH2_MSG_KEXDH_REPLY");
                    int bloblen = p.getInt32();
                    System.out.println("bloblen is " + bloblen);
                    String keytype = p.getString();
                    System.out.println("KEXDH: " + keytype);
                    if (keytype.equals("ssh-rsa")) {
                        rsa_e = p.getMpInt();
                        rsa_n = p.getMpInt();
                        result = "\n\rSSH-RSA (" + rsa_n + "," + rsa_e + ")\n\r";
                    } else {
                        return "\n\rUnsupported kexdh algorithm " + keytype + "!\n\r";
                    }
                    BigInteger dhserverpub = p.getMpInt();
                    result += "DH Server Pub: " + dhserverpub + "\n\r";
                    int siglen = p.getInt32();
                    String sigstr = p.getString();
                    result += "Signature: ktype is " + sigstr + "\r\n";
                    byte sigdata[] = p.getBytes(p.getInt32());
                    return result;
                }
            default:
                return "SSH2: handlePacket2 Unknown type " + p.getType();
        }
        return "";
    }

    private String handlePacket1(SshPacket1 p) throws IOException {
        byte b;
        if (debug > 0) System.out.println("1 packet to handle, type " + p.getType());
        switch(p.getType()) {
            case SSH_MSG_IGNORE:
                return "";
            case SSH_MSG_DISCONNECT:
                String str = p.getString();
                disconnect();
                return str;
            case SSH_SMSG_PUBLIC_KEY:
                byte[] anti_spoofing_cookie;
                byte[] server_key_bits;
                byte[] server_key_public_exponent;
                byte[] server_key_public_modulus;
                byte[] host_key_bits;
                byte[] host_key_public_exponent;
                byte[] host_key_public_modulus;
                byte[] protocol_flags;
                byte[] supported_ciphers_mask;
                byte[] supported_authentications_mask;
                anti_spoofing_cookie = p.getBytes(8);
                server_key_bits = p.getBytes(4);
                server_key_public_exponent = p.getMpInt();
                server_key_public_modulus = p.getMpInt();
                host_key_bits = p.getBytes(4);
                host_key_public_exponent = p.getMpInt();
                host_key_public_modulus = p.getMpInt();
                protocol_flags = p.getBytes(4);
                supported_ciphers_mask = p.getBytes(4);
                supported_authentications_mask = p.getBytes(4);
                String ret = Send_SSH_CMSG_SESSION_KEY(anti_spoofing_cookie, server_key_public_modulus, host_key_public_modulus, supported_ciphers_mask, server_key_public_exponent, host_key_public_exponent);
                if (ret != null) return ret;
                if (hashHostKey != null && hashHostKey.compareTo("") != 0) {
                    byte[] Md5_hostKey = md5.digest(host_key_public_modulus);
                    String hashHostKeyBis = "";
                    for (int i = 0; i < Md5_hostKey.length; i++) {
                        String hex = "";
                        int[] v = new int[2];
                        v[0] = (Md5_hostKey[i] & 240) >> 4;
                        v[1] = (Md5_hostKey[i] & 15);
                        for (int j = 0; j < 1; j++) switch(v[j]) {
                            case 10:
                                hex += "a";
                                break;
                            case 11:
                                hex += "b";
                                break;
                            case 12:
                                hex += "c";
                                break;
                            case 13:
                                hex += "d";
                                break;
                            case 14:
                                hex += "e";
                                break;
                            case 15:
                                hex += "f";
                                break;
                            default:
                                hex += String.valueOf(v[j]);
                                break;
                        }
                        hashHostKeyBis = hashHostKeyBis + hex;
                    }
                    if (hashHostKeyBis.compareTo(hashHostKey) != 0) {
                        login = password = "";
                        return "\nHash value of the host key not correct \r\n" + "login & password have been reset \r\n" + "- erase the 'hashHostKey' parameter in the Html\r\n" + "(it is used for auhentificating the server and " + "prevent you from connecting \r\n" + "to any other)\r\n";
                    }
                }
                break;
            case SSH_SMSG_SUCCESS:
                if (debug > 0) System.out.println("SSH_SMSG_SUCCESS (last packet was " + lastPacketSentType + ")");
                if (lastPacketSentType == SSH_CMSG_SESSION_KEY) {
                    Send_SSH_CMSG_USER();
                    break;
                }
                if (lastPacketSentType == SSH_CMSG_USER) {
                    Send_SSH_CMSG_REQUEST_PTY();
                    return "\nEmpty password login.\r\n";
                }
                if (lastPacketSentType == SSH_CMSG_AUTH_PASSWORD) {
                    if (debug > 0) System.out.println("login succesful");
                    Send_SSH_CMSG_REQUEST_PTY();
                    return "\nLogin & password accepted\r\n";
                }
                if (lastPacketSentType == SSH_CMSG_REQUEST_PTY) {
                    cansenddata = true;
                    if (dataToSend != null) {
                        Send_SSH_CMSG_STDIN_DATA(dataToSend);
                        dataToSend = null;
                    }
                    Send_SSH_CMSG_EXEC_SHELL();
                    break;
                }
                if (lastPacketSentType == SSH_CMSG_EXEC_SHELL) {
                }
                break;
            case SSH_SMSG_FAILURE:
                if (debug > 1) System.err.println("SSH_SMSG_FAILURE");
                if (lastPacketSentType == SSH_CMSG_AUTH_PASSWORD) {
                    System.out.println("failed to log in");
                    Send_SSH_MSG_DISCONNECT("Failed to log in.");
                    disconnect();
                    return "\nLogin & password not accepted\r\n";
                }
                if (lastPacketSentType == SSH_CMSG_USER) {
                    Send_SSH_CMSG_AUTH_PASSWORD();
                    break;
                }
                if (lastPacketSentType == SSH_CMSG_REQUEST_PTY) {
                    break;
                }
                break;
            case SSH_SMSG_STDOUT_DATA:
                return p.getString();
            case SSH_SMSG_STDERR_DATA:
                str = "Error : " + p.getString();
                System.out.println("SshIO.handlePacket : " + "STDERR_DATA " + str);
                return str;
            case SSH_SMSG_EXITSTATUS:
                int value = p.getInt32();
                Send_SSH_CMSG_EXIT_CONFIRMATION();
                System.out.println("SshIO : Exit status " + value);
                disconnect();
                break;
            case SSH_MSG_DEBUG:
                str = p.getString();
                if (debug > 0) {
                    System.out.println("SshIO.handlePacket : " + " DEBUG " + str);
                    return str;
                }
                return "";
            default:
                System.err.print("SshIO.handlePacket1: Packet Type unknown: " + p.getType());
                break;
        }
        return "";
    }

    private void sendPacket1(SshPacket1 packet) throws IOException {
        write(packet.getPayLoad(crypto));
        lastPacketSentType = packet.getType();
    }

    private void sendPacket2(SshPacket2 packet) throws IOException {
        write(packet.getPayLoad(crypto, outgoingseq));
        outgoingseq++;
        lastPacketSentType = packet.getType();
    }

    private String Send_SSH_CMSG_SESSION_KEY(byte[] anti_spoofing_cookie, byte[] server_key_public_modulus, byte[] host_key_public_modulus, byte[] supported_ciphers_mask, byte[] server_key_public_exponent, byte[] host_key_public_exponent) throws IOException {
        String str;
        int boffset;
        byte cipher_types;
        byte[] session_key;
        byte[] session_id_byte = new byte[host_key_public_modulus.length + server_key_public_modulus.length + anti_spoofing_cookie.length];
        System.arraycopy(host_key_public_modulus, 0, session_id_byte, 0, host_key_public_modulus.length);
        System.arraycopy(server_key_public_modulus, 0, session_id_byte, host_key_public_modulus.length, server_key_public_modulus.length);
        System.arraycopy(anti_spoofing_cookie, 0, session_id_byte, host_key_public_modulus.length + server_key_public_modulus.length, anti_spoofing_cookie.length);
        byte[] hash_md5 = md5.digest(session_id_byte);
        if ((supported_ciphers_mask[3] & (byte) (1 << SSH_CIPHER_BLOWFISH)) != 0) {
            cipher_types = (byte) SSH_CIPHER_BLOWFISH;
            cipher_type = "Blowfish";
        } else {
            if ((supported_ciphers_mask[3] & (1 << SSH_CIPHER_IDEA)) != 0) {
                cipher_types = (byte) SSH_CIPHER_IDEA;
                cipher_type = "IDEA";
            } else {
                if ((supported_ciphers_mask[3] & (1 << SSH_CIPHER_3DES)) != 0) {
                    cipher_types = (byte) SSH_CIPHER_3DES;
                    cipher_type = "DES3";
                } else {
                    if ((supported_ciphers_mask[3] & (1 << SSH_CIPHER_DES)) != 0) {
                        cipher_types = (byte) SSH_CIPHER_DES;
                        cipher_type = "DES";
                    } else {
                        System.err.println("SshIO: remote server does not supported IDEA, BlowFish or 3DES, support cypher mask is " + supported_ciphers_mask[3] + ".\n");
                        Send_SSH_MSG_DISCONNECT("No more auth methods available.");
                        disconnect();
                        return "\rRemote server does not support IDEA/Blowfish/3DES blockcipher, closing connection.\r\n";
                    }
                }
            }
        }
        if (debug > 0) System.out.println("SshIO: Using " + cipher_type + " blockcipher.\n");
        byte[] random_bits1 = new byte[16], random_bits2 = new byte[16];
        SecureRandom random = new java.security.SecureRandom(random_bits1);
        random.nextBytes(random_bits1);
        random.nextBytes(random_bits2);
        session_key = SshMisc.addArrayOfBytes(random_bits1, random_bits2);
        byte[] session_keyXored = SshMisc.XORArrayOfBytes(random_bits1, hash_md5);
        session_keyXored = SshMisc.addArrayOfBytes(session_keyXored, random_bits2);
        byte[] encrypted_session_key = SshCrypto.encrypteRSAPkcs1Twice(session_keyXored, server_key_public_exponent, server_key_public_modulus, host_key_public_exponent, host_key_public_modulus);
        int protocol_flags = 0;
        SshPacket1 packet = new SshPacket1(SSH_CMSG_SESSION_KEY);
        packet.putByte((byte) cipher_types);
        packet.putBytes(anti_spoofing_cookie);
        packet.putBytes(encrypted_session_key);
        packet.putInt32(protocol_flags);
        sendPacket1(packet);
        crypto = new SshCrypto(cipher_type, session_key);
        return "";
    }

    /**
   * SSH_MSG_DISCONNECT
   *   string       disconnect reason
   */
    private String Send_SSH_MSG_DISCONNECT(String reason) throws IOException {
        SshPacket1 p = new SshPacket1(SSH_MSG_DISCONNECT);
        p.putString(reason);
        sendPacket1(p);
        return "";
    }

    /**
   * SSH_CMSG_USER
   * string   user login name on server
   */
    private String Send_SSH_CMSG_USER() throws IOException {
        if (debug > 0) System.err.println("Send_SSH_CMSG_USER(" + login + ")");
        SshPacket1 p = new SshPacket1(SSH_CMSG_USER);
        p.putString(login);
        sendPacket1(p);
        return "";
    }

    /**
   * Send_SSH_CMSG_AUTH_PASSWORD
   * string   user password
   */
    private String Send_SSH_CMSG_AUTH_PASSWORD() throws IOException {
        SshPacket1 p = new SshPacket1(SSH_CMSG_AUTH_PASSWORD);
        p.putString(password);
        sendPacket1(p);
        return "";
    }

    /**
   * Send_SSH_CMSG_EXEC_SHELL
   *  (no arguments)
   *   Starts a shell (command interpreter), and enters interactive
   *   session mode.
   */
    private String Send_SSH_CMSG_EXEC_SHELL() throws IOException {
        SshPacket1 packet = new SshPacket1(SSH_CMSG_EXEC_SHELL);
        sendPacket1(packet);
        return "";
    }

    /**
   * Send_SSH_CMSG_STDIN_DATA
   *
   */
    private String Send_SSH_CMSG_STDIN_DATA(String str) throws IOException {
        SshPacket1 packet = new SshPacket1(SSH_CMSG_STDIN_DATA);
        packet.putString(str);
        sendPacket1(packet);
        return "";
    }

    /**
   * Send_SSH_CMSG_WINDOW_SIZE
   *   string       TERM environment variable value (e.g. vt100)
   *   32-bit int   terminal height, rows (e.g., 24)
   *   32-bit int   terminal width, columns (e.g., 80)
   *   32-bit int   terminal width, pixels (0 if no graphics) (e.g., 480)
   */
    private String Send_SSH_CMSG_WINDOW_SIZE(int c, int r) throws IOException {
        SshPacket1 p = new SshPacket1(SSH_CMSG_WINDOW_SIZE);
        p.putInt32(r);
        p.putInt32(c);
        p.putInt32(0);
        p.putInt32(0);
        sendPacket1(p);
        return "";
    }

    /**
   * Send_SSH_CMSG_REQUEST_PTY
   *   string       TERM environment variable value (e.g. vt100)
   *   32-bit int   terminal height, rows (e.g., 24)
   *   32-bit int   terminal width, columns (e.g., 80)
   *   32-bit int   terminal width, pixels (0 if no graphics) (e.g., 480)
   */
    private String Send_SSH_CMSG_REQUEST_PTY() throws IOException {
        SshPacket1 p = new SshPacket1(SSH_CMSG_REQUEST_PTY);
        p.putString(getTerminalType());
        p.putInt32(24);
        p.putInt32(80);
        p.putInt32(0);
        p.putInt32(0);
        p.putByte((byte) 0);
        sendPacket1(p);
        return "";
    }

    private String Send_SSH_CMSG_EXIT_CONFIRMATION() throws IOException {
        SshPacket1 packet = new SshPacket1(SSH_CMSG_EXIT_CONFIRMATION);
        sendPacket1(packet);
        return "";
    }
}
