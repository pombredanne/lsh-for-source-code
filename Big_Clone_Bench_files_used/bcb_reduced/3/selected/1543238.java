package com.sun.midp.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Exception;
import java.util.Vector;
import javax.microedition.pki.CertificateException;
import com.sun.midp.log.Logging;
import com.sun.midp.log.LogChannels;
import com.sun.midp.crypto.*;
import com.sun.midp.pki.*;

class Handshake {

    /** ARCFOUR_128_SHA (0x05). */
    static final byte ARCFOUR_128_SHA = 0x05;

    /** ARCFOUR_128_MD5 (0x04). */
    static final byte ARCFOUR_128_MD5 = 0x04;

    /**  ARCFOUR_40_MD5 (0x03). */
    static final byte ARCFOUR_40_MD5 = 0x03;

    /**
     * This contains the cipher suite encoding length in the first
     * two bytes, followed by an encoding of the cipher suites followed
     * by the compression suite length in one byte and the compression
     * suite. For now, we only propose the two most commonly used
     * cipher suites.
     */
    private static final byte[] SUITES_AND_COMP = { 0x00, 0x06, 0x00, ARCFOUR_128_SHA, 0x00, ARCFOUR_128_MD5, 0x00, ARCFOUR_40_MD5, 0x01, 0x00 };

    /**
     * Array of suite names.
     */
    private static String[] suiteNames = { "", "", "", "TLS_RSA_EXPORT_WITH_RC4_40_MD5", "TLS_RSA_WITH_RC4_128_MD5", "TLS_RSA_WITH_RC4_128_SHA" };

    /**
     * Each handshake message has a four-byte header containing
     * the type (1 byte) and length (3 byte).
     */
    private static final byte HDR_SIZE = 4;

    /** Hello Request (0). */
    private static final byte HELLO_REQ = 0;

    /** Client Hello (1). */
    private static final byte C_HELLO = 1;

    /** Server Hello (2). */
    private static final byte S_HELLO = 2;

    /** Certificate (11). */
    private static final byte CERT = 11;

    /** Server Key Exchange (12). */
    private static final byte S_KEYEXCH = 12;

    /** Certificate Request (13). */
    private static final byte CERT_REQ = 13;

    /** Server Hello Done (14). */
    private static final byte S_DONE = 14;

    /** Certificate Verify (15). */
    private static final byte CERT_VRFY = 15;

    /** Client Key Exchange (16). */
    private static final byte C_KEYEXCH = 16;

    /** Finished (20). */
    private static final byte FINISH = 20;

    /** Number of bytes in an MD5 Digest (16). */
    private static final byte MD5_SIZE = 16;

    /** Number of bytes in an SHA Digest (20). */
    private static final byte SHA_SIZE = 20;

    /**
     * The Finish message contains one MD5 and one SHA hash
     * and has a length of 4+16+20 = 40 = 0x24 bytes.
     */
    private static final byte[] FINISH_PREFIX = { FINISH, 0x00, 0x00, 0x24 };

    /** Handle to trusted certificate store. */
    private CertStore certStore = null;

    /** Current record to process. */
    private Record rec;

    /** Peer host name . */
    private String peerHost;

    /** Peer port number. */
    private int peerPort;

    /** Local random number seed. */
    private SecureRandom rnd = null;

    /** Previous session context to this host and port, if there was one. */
    private Session cSession = null;

    /** Session id returned by server. */
    private byte[] sSessionId = null;

    /** Client random number. */
    private byte[] crand = null;

    /** Server random number. */
    private byte[] srand = null;

    /** Proposed SSL version. */
    private byte ver;

    /** Role (always CLIENT for now). */
    private byte role;

    /** Negotiated cipher suite. */
    byte negSuite;

    /** Name of negotiated cipher suite. */
    String negSuiteName;

    /** Flag to indicate certificate request received. */
    private byte gotCertReq = 0;

    /** Pre-master secret. */
    private byte[] preMaster = null;

    /** Master secret. */
    private byte[] master = null;

    /**
     * Public key used to encrypt the appropriate 
     * usage of sKey certs in chain.
     */
    private RSAPublicKey eKey = null;

    /** Temporary storage for server certificate. */
    X509Certificate sCert = null;

    /** Accumulation of MD5 digests. */
    private MessageDigest ourMD5 = null;

    /** Accumulation of SHA digests. */
    private MessageDigest ourSHA = null;

    /** Start of message in data buffer. */
    private int start = 0;

    /** Start of next message in data buffer. */
    private int nextMsgStart = 0;

    /** Count of bytes left in the data buffer. */
    private int cnt = 0;

    /**
     * Validates a chain of certificates and returns the RSA public
     * key from the first certificate in that chain. The format of 
     * the chain is specific to the ServerCertificate payload in an
     * SSL handshake.
     *
     * @param msg  byte array containing the SSL ServerCertificate
     *             payload (this is a chain of DER-encoded X.509 
     *             certificates, in which each certificate is preceded
     *             by a 3-byte length field)
     * @param off  offset in the byte array where the cert chain begins
     * @param end  position in the byte array where the cert chain ends + 1
     *
     * @return server's certificate in the chain
     *
     * @exception IOException if the there is a binary formating error
     * @exception CertificateException if there a verification error
     */
    private X509Certificate parseChain(byte[] msg, int off, int end) throws IOException, CertificateException {
        Vector certs = new Vector();
        int len;
        while (off < (end - 3)) {
            len = ((msg[off++] & 0xff) << 16) + ((msg[off++] & 0xff) << 8) + (msg[off++] & 0xff);
            if (len < 0 || len + off > msg.length) {
                throw new IOException("SSL certificate length too long");
            }
            certs.addElement(X509Certificate.generateCertificate(msg, off, len));
            off += len;
        }
        X509Certificate.verifyChain(certs, -1, X509Certificate.SERVER_AUTH_EXT_KEY_USAGE, certStore);
        return (X509Certificate) certs.elementAt(0);
    }

    /**
     * Creates an Handshake object that is used to negotiate a
     * version 3 handshake with an SSL peer.
     *
     * @param host hostname of the peer
     * @param port port number of the peer
     * @param r    Record instance through which handshake
     *             will occur
     * @param tcs  trusted certificate store containing certificates
     *
     * @exception RuntimeException if SHA-1 or MD5 is not available
     */
    Handshake(String host, int port, Record r, CertStore tcs) {
        peerHost = new String(host);
        peerPort = port;
        rec = r;
        certStore = tcs;
        eKey = null;
        gotCertReq = 0;
        start = 0;
        cnt = 0;
        try {
            ourMD5 = MessageDigest.getInstance("MD5");
            ourSHA = MessageDigest.getInstance("SHA-1");
            rnd = SecureRandom.getInstance(SecureRandom.ALG_SECURE_RANDOM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Obtains the next available handshake message.
     * <p>
     * The message returned has the header plus the number of
     * bytes indicated in the handshake message header.</p>
     * 
     * @param type the desired handshake message type
     * @return number of bytes in the next handshake message
     * of the desired type or -1 if the next message is not of
     * the desired type
     * @exception IOException if there is a problem reading the
     * next handshake message
     */
    private int getNextMsg(byte type) throws IOException {
        if (cnt == 0) {
            rec.rdRec(true, Record.HNDSHK);
            if (rec.plainTextLength < HDR_SIZE) {
                throw new IOException("getNextMsg refill failed");
            }
            cnt = rec.plainTextLength;
            nextMsgStart = 0;
        }
        if (rec.inputData[nextMsgStart] == type) {
            int len = ((rec.inputData[nextMsgStart + 1] & 0xff) << 16) + ((rec.inputData[nextMsgStart + 2] & 0xff) << 8) + (rec.inputData[nextMsgStart + 3] & 0xff) + HDR_SIZE;
            if (cnt < len) {
                throw new IOException("Refill got short msg " + "c=" + cnt + " l=" + len);
            }
            start = nextMsgStart;
            nextMsgStart += len;
            cnt -= len;
            return len;
        } else {
            return -1;
        }
    }

    /**
     * Sends an SSL version 3.0 Client hello handshake message.
     * <P />
     * @exception IOException if there is a problem writing to 
     * the record layer
     */
    private void sndHello3() throws IOException {
        cSession = Session.get(peerHost, peerPort);
        int len = (cSession == null) ? 0 : cSession.id.length;
        byte[] msg = new byte[39 + len + SUITES_AND_COMP.length];
        int idx = 0;
        msg[idx++] = C_HELLO;
        int mlen = msg.length - HDR_SIZE;
        msg[idx++] = (byte) (mlen >>> 16);
        msg[idx++] = (byte) (mlen >>> 8);
        msg[idx++] = (byte) (mlen & 0xff);
        msg[idx++] = (byte) (ver >>> 4);
        msg[idx++] = (byte) (ver & 0x0f);
        crand = new byte[32];
        rnd.nextBytes(crand, 0, 32);
        System.arraycopy(crand, 0, msg, idx, crand.length);
        idx += crand.length;
        msg[idx++] = (byte) (len & 0xff);
        if (cSession != null) {
            System.arraycopy(cSession.id, 0, msg, idx, cSession.id.length);
            idx += cSession.id.length;
        }
        System.arraycopy(SUITES_AND_COMP, 0, msg, idx, SUITES_AND_COMP.length);
        ourMD5.update(msg, 0, msg.length);
        ourSHA.update(msg, 0, msg.length);
        rec.wrRec(Record.HNDSHK, msg, 0, msg.length);
    }

    /**
     * Receives a Server hello handshake message.
     * <P />
     * @return 0 on success, -1 on failure
     * @exception IOException if there is a problem reading the
     * message
     */
    private int rcvSrvrHello() throws IOException {
        int msgLength = getNextMsg(S_HELLO);
        int idx = start + HDR_SIZE;
        int endOfMsg = start + msgLength;
        if (msgLength < 42) {
            return -1;
        }
        if ((rec.inputData[start + idx++] != (ver >>> 4)) || (rec.inputData[start + idx++] != (ver & 0x0f))) {
            return -1;
        }
        srand = new byte[32];
        System.arraycopy(rec.inputData, idx, srand, 0, 32);
        idx += 32;
        int slen = rec.inputData[idx++] & 0xff;
        if (slen != 0) {
            if (endOfMsg < idx + slen) {
                return -1;
            }
            sSessionId = new byte[slen];
            System.arraycopy(rec.inputData, idx, sSessionId, 0, slen);
            idx += slen;
        }
        idx++;
        negSuite = rec.inputData[idx++];
        if ((negSuite != ARCFOUR_128_SHA) && (negSuite != ARCFOUR_128_MD5) && (negSuite != ARCFOUR_40_MD5) && (rec.inputData[idx++] != (byte) 0x00)) {
            return -1;
        }
        ourMD5.update(rec.inputData, start, msgLength);
        ourSHA.update(rec.inputData, start, msgLength);
        negSuiteName = suiteNames[negSuite];
        if (Logging.REPORT_LEVEL <= Logging.INFORMATION) {
            Logging.report(Logging.INFORMATION, LogChannels.LC_SECURITY, "Negotiated " + negSuiteName);
        }
        return 0;
    }

    /**
     * Receives a Server certificate message containing a certificate
     * chain starting with the server certificate.
     * <P />
     * @return 0 if a trustworthy server certificate is found, -1 otherwise
     * @exception IOException if there is a problem reading the message
     */
    private int rcvCert() throws IOException {
        int msgLength;
        int endOfMsg;
        int idx;
        int len;
        msgLength = getNextMsg(CERT);
        endOfMsg = start + msgLength;
        if (msgLength < 7) {
            return -1;
        }
        idx = start + HDR_SIZE;
        len = 0;
        len = ((rec.inputData[idx++] & 0xff) << 16) + ((rec.inputData[idx++] & 0xff) << 8) + (rec.inputData[idx++] & 0xff);
        if ((idx + len) > endOfMsg) return -1;
        sCert = parseChain(rec.inputData, idx, endOfMsg);
        ourMD5.update(rec.inputData, start, msgLength);
        ourSHA.update(rec.inputData, start, msgLength);
        return 0;
    }

    /**
     * Receives a Server key exchange message. For now only RSA key
     * exchange is supported and this message includes temporary
     * RSA public key parameters signed by the server's long-term
     * private key. This message is optional.
     * <P />
     * @return 0 on success, -1 on failure
     * @exception IOException if there is a problem reading the
     * message
     * @exception RuntimeException if SHA-1 or MD5 is not available
     */
    private int rcvSrvrKeyExch() throws IOException {
        int msgLength = getNextMsg(S_KEYEXCH);
        int idx = start + HDR_SIZE;
        int endOfMsg = start + msgLength;
        RSAPublicKey sKey = (RSAPublicKey) sCert.getPublicKey();
        int keyUsage = sCert.getKeyUsage();
        if (msgLength == -1) {
            eKey = sKey;
            if (keyUsage != -1 && (keyUsage & X509Certificate.KEY_ENCIPHER_KEY_USAGE) != X509Certificate.KEY_ENCIPHER_KEY_USAGE) {
                if (Logging.REPORT_LEVEL <= Logging.ERROR) {
                    Logging.report(Logging.ERROR, LogChannels.LC_SECURITY, "The keyEncipherment was bit is " + "set in server certificate key " + "usage extension.");
                }
                throw new CertificateException(sCert, CertificateException.INAPPROPRIATE_KEY_USAGE);
            }
            return 0;
        }
        if (endOfMsg < (idx + 4)) {
            return -1;
        }
        int len = ((rec.inputData[idx++] & 0xff) << 16) + (rec.inputData[idx++] & 0xff);
        if (endOfMsg < (idx + len + 2)) {
            return -1;
        }
        int modulusPos;
        int modulusLen;
        int exponentPos;
        int exponentLen;
        if ((len == 65) && (rec.inputData[idx] == (byte) 0x00)) {
            modulusPos = idx + 1;
            modulusLen = 64;
        } else {
            modulusPos = idx;
            modulusLen = len;
        }
        idx += len;
        len = ((rec.inputData[idx++] & 0xff) << 16) + (rec.inputData[idx++] & 0xff);
        if (endOfMsg < (idx + len)) {
            return -1;
        }
        exponentPos = idx;
        exponentLen = len;
        eKey = new RSAPublicKey(rec.inputData, modulusPos, modulusLen, rec.inputData, exponentPos, exponentLen);
        idx += len;
        int end = idx;
        len = ((rec.inputData[idx++] & 0xff) << 16) + (rec.inputData[idx++] & 0xff);
        if (endOfMsg < (idx + len)) {
            return -1;
        }
        byte[] sig = new byte[len];
        System.arraycopy(rec.inputData, idx, sig, 0, sig.length);
        idx += len;
        if (endOfMsg != idx) {
            return -1;
        }
        byte[] dat = new byte[MD5_SIZE + SHA_SIZE];
        try {
            MessageDigest di = MessageDigest.getInstance("MD5");
            di.update(crand, 0, crand.length);
            di.update(srand, 0, srand.length);
            di.update(rec.inputData, HDR_SIZE, end - HDR_SIZE);
            di.digest(dat, 0, MD5_SIZE);
            di = MessageDigest.getInstance("SHA-1");
            di.update(crand, 0, crand.length);
            di.update(srand, 0, srand.length);
            di.update(rec.inputData, HDR_SIZE, end - HDR_SIZE);
            di.digest(dat, MD5_SIZE, SHA_SIZE);
        } catch (Exception e) {
            throw new RuntimeException("No MD5 or SHA");
        }
        try {
            Cipher rsa = Cipher.getInstance("RSA");
            rsa.init(Cipher.DECRYPT_MODE, sKey);
            byte[] res = new byte[sKey.getModulusLen()];
            int val = rsa.doFinal(sig, 0, sig.length, res, 0);
            if (!Utils.byteMatch(res, 0, dat, 0, dat.length)) {
                if (Logging.REPORT_LEVEL <= Logging.ERROR) {
                    Logging.report(Logging.ERROR, LogChannels.LC_SECURITY, "RSA params failed verification");
                }
                return -1;
            }
        } catch (Exception e) {
            throw new IOException("RSA decryption caught " + e);
        }
        ourMD5.update(rec.inputData, start, msgLength);
        ourSHA.update(rec.inputData, start, msgLength);
        return 0;
    }

    /**
     * Receives a Certificate request message. This message is optional.
     * <P />
     * @return 0 (this method always completes successfully)
     * @exception IOException if there is a problem reading the
     * message
     */
    private int rcvCertReq() throws IOException {
        int msgLength = getNextMsg(CERT_REQ);
        if (msgLength == -1) {
            return 0;
        }
        gotCertReq = (byte) 1;
        ourMD5.update(rec.inputData, start, msgLength);
        ourSHA.update(rec.inputData, start, msgLength);
        return 0;
    }

    /**
     * Receives a Server hello done message.
     * <P />
     * @return 0 on success, -1 on error
     * @exception IOException if there is a problem reading the
     * message
     */
    private int rcvSrvrHelloDone() throws IOException {
        int msgLength = getNextMsg(S_DONE);
        if (msgLength != HDR_SIZE) {
            return -1;
        }
        ourMD5.update(rec.inputData, start, msgLength);
        ourSHA.update(rec.inputData, start, msgLength);
        return 0;
    }

    /**
     * Sends a Client key exchange message. For now, only RSA key 
     * exchange is supported and this message contains a pre-master
     * secret encrypted with the RSA public key of the server.
     * <P />
     * @exception IOException if there is a problem writing to the 
     * record layer
     */
    private void sndKeyExch() throws IOException {
        if (gotCertReq == 1) {
            rec.alert(Record.FATAL, Record.NO_CERT);
            throw new IOException("No client cert");
        } else {
            preMaster = new byte[48];
            rnd.nextBytes(preMaster, 0, 48);
            preMaster[0] = (byte) (ver >>> 4);
            preMaster[1] = (byte) (ver & 0x0f);
            int modLen = eKey.getModulusLen();
            byte[] msg = new byte[HDR_SIZE + modLen];
            int idx = 0;
            msg[idx++] = C_KEYEXCH;
            msg[idx++] = (byte) (modLen >>> 16);
            msg[idx++] = (byte) (modLen >>> 8);
            msg[idx++] = (byte) (modLen & 0xff);
            try {
                Cipher rsa = Cipher.getInstance("RSA");
                rsa.init(Cipher.ENCRYPT_MODE, eKey);
                int val = rsa.doFinal(preMaster, 0, 48, msg, idx);
                if (val != modLen) throw new IOException("RSA result too short");
            } catch (Exception e) {
                throw new IOException("premaster encryption caught " + e);
            }
            ourMD5.update(msg, 0, msg.length);
            ourSHA.update(msg, 0, msg.length);
            rec.wrRec(Record.HNDSHK, msg, 0, msg.length);
        }
    }

    /**
     * Derives the master key based on the pre-master secret and
     * random values exchanged in the client and server hello messages.
     * <P />
     * @exception IOException if there is a problem during the computation
     */
    private void mkMaster() throws IOException {
        byte[] expansion[] = { { (byte) 0x41 }, { (byte) 0x42, (byte) 0x42 }, { (byte) 0x43, (byte) 0x43, (byte) 0x43 } };
        MessageDigest md = null;
        MessageDigest sd = null;
        byte[] tmp = new byte[preMaster.length + crand.length + srand.length];
        System.arraycopy(preMaster, 0, tmp, 0, preMaster.length);
        System.arraycopy(crand, 0, tmp, preMaster.length, crand.length);
        System.arraycopy(srand, 0, tmp, preMaster.length + crand.length, srand.length);
        try {
            md = MessageDigest.getInstance("MD5");
            sd = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No MD5 or SHA");
        }
        master = new byte[48];
        try {
            for (int i = 0; i < 3; i++) {
                md.update(preMaster, 0, preMaster.length);
                sd.update(expansion[i], 0, expansion[i].length);
                byte[] res = new byte[SHA_SIZE];
                sd.update(tmp, 0, tmp.length);
                sd.digest(res, 0, res.length);
                md.update(res, 0, res.length);
                md.digest(master, i << 4, MD5_SIZE);
            }
        } catch (DigestException e) {
            throw new RuntimeException("digest exception");
        }
    }

    /**
     * Sends a ChangeCipherSpec protocol message (this is not really
     * a handshake protocol message).
     * <P />
     * @exception IOException if there is a problem writing to the 
     * record layer
     */
    private void sndChangeCipher() throws IOException {
        byte[] msg = new byte[1];
        msg[0] = (byte) 0x01;
        rec.wrRec(Record.CCS, msg, 0, 1);
    }

    /**
     * Computes the content of a Finished message.
     * <P />
     * @param who the role (either Record.CLIENT or
     * Record.SERVER) for which the finish message is computed
     * @return a byte array containing the hash of all handshake 
     * messages seen so far
     * @exception IOException if handshake digests could not be computed
     */
    private byte[] computeFinished(byte who) throws IOException {
        byte[] sender[] = { { 0x53, 0x52, 0x56, 0x52 }, { 0x43, 0x4c, 0x4e, 0x54 } };
        byte[] msg = new byte[MD5_SIZE + SHA_SIZE];
        byte[] tmp = null;
        try {
            MessageDigest d = (MessageDigest) ourMD5.clone();
            d.update(sender[who], 0, 4);
            d.update(master, 0, master.length);
            tmp = new byte[MD5_SIZE];
            d.update(MAC.PAD1, 0, 48);
            d.digest(tmp, 0, tmp.length);
            d.update(master, 0, master.length);
            d.update(MAC.PAD2, 0, 48);
            d.update(tmp, 0, tmp.length);
            d.digest(msg, 0, MD5_SIZE);
            d = (MessageDigest) ourSHA.clone();
            d.update(sender[who], 0, 4);
            d.update(master, 0, master.length);
            tmp = new byte[SHA_SIZE];
            d.update(MAC.PAD1, 0, 40);
            d.digest(tmp, 0, tmp.length);
            d.update(master, 0, master.length);
            d.update(MAC.PAD2, 0, 40);
            d.update(tmp, 0, tmp.length);
            d.digest(msg, MD5_SIZE, SHA_SIZE);
            return msg;
        } catch (Exception e) {
            throw new IOException("MessageDigest not cloneable");
        }
    }

    /**
     * Sends a Finished message.
     * <P />
     * @exception IOException if there is a problem writing to the 
     * record layer
     */
    private void sndFinished() throws IOException {
        byte[] msg = new byte[40];
        System.arraycopy(FINISH_PREFIX, 0, msg, 0, 4);
        System.arraycopy(computeFinished(role), 0, msg, 4, 36);
        ourMD5.update(msg, 0, msg.length);
        ourSHA.update(msg, 0, msg.length);
        rec.wrRec(Record.HNDSHK, msg, 0, msg.length);
    }

    /**
     * Receives a ChangeCipherSpec protocol message (this is
     * not a handshake message).
     * <P />
     * @return 0 on success, -1 on error
     * @exception IOException if there is a problem reading the
     * message
     */
    private int rcvChangeCipher() throws IOException {
        if (cnt != 0) {
            if (Logging.REPORT_LEVEL <= Logging.ERROR) {
                Logging.report(Logging.ERROR, LogChannels.LC_SECURITY, "Unread handshake mesg in store");
            }
            return -1;
        }
        rec.rdRec(true, Record.CCS);
        if ((rec.inputData == null) || (rec.inputData.length != 1) || (rec.inputData[0] != (byte) 0x01)) {
            return -1;
        }
        return 0;
    }

    /**
     * Receives a Finished message and verifies that it contains
     * the correct hash of handshake messages.
     * <P />
     * @return 0 on success, -1 on error
     * @exception IOException if there is a problem reading the
     * message
     */
    private int rcvFinished() throws IOException {
        int msgLength = getNextMsg(FINISH);
        if (msgLength != 40) {
            return -1;
        }
        byte[] expected = computeFinished((byte) (1 - role));
        if (!Utils.byteMatch(rec.inputData, start + HDR_SIZE, expected, 0, expected.length)) {
            return -1;
        } else {
            ourMD5.update(rec.inputData, start, msgLength);
            ourSHA.update(rec.inputData, start, msgLength);
            return 0;
        }
    }

    void doHandShake(byte aswho) throws IOException {
        long t1 = System.currentTimeMillis();
        int code = 0;
        ver = (byte) 0x30;
        role = aswho;
        byte val = 0;
        sndHello3();
        if (rcvSrvrHello() < 0) {
            complain("Bad ServerHello");
        }
        ;
        if ((sSessionId == null) || (cSession == null) || (sSessionId.length != cSession.id.length) || !Utils.byteMatch(sSessionId, 0, cSession.id, 0, sSessionId.length)) {
            try {
                code = rcvCert();
            } catch (CertificateException e) {
                complain(e);
            }
            if (code < 0) {
                complain("Corrupt server certificate message");
            }
            try {
                code = rcvSrvrKeyExch();
            } catch (CertificateException e) {
                complain(e);
            }
            if (code < 0) {
                complain("Bad ServerKeyExchange");
            }
            rcvCertReq();
            if (rcvSrvrHelloDone() < 0) {
                complain("Bad ServerHelloDone");
            }
            sndKeyExch();
            mkMaster();
            try {
                rec.init(role, crand, srand, negSuite, master);
            } catch (Exception e) {
                complain("Record.init() caught " + e);
            }
            sndChangeCipher();
            sndFinished();
            if (rcvChangeCipher() < 0) {
                complain("Bad ChangeCipherSpec");
            }
            if (rcvFinished() < 0) {
                complain("Bad Finished");
            }
        } else {
            master = cSession.master;
            sCert = cSession.cert;
            try {
                rec.init(role, crand, srand, negSuite, master);
            } catch (Exception e) {
                complain("Record.init() caught " + e);
            }
            if (rcvChangeCipher() < 0) {
                complain("Bad ChangeCipherSpec");
            }
            if (rcvFinished() < 0) {
                complain("Bad Finished");
            }
            sndChangeCipher();
            sndFinished();
        }
        Session.add(peerHost, peerPort, sSessionId, master, sCert);
        if (preMaster != null) {
            for (int i = 0; i < preMaster.length; i++) {
                preMaster[i] = 0;
            }
        }
        for (int i = 0; i < master.length; i++) {
            master[i] = 0;
        }
    }

    /**
     * Sends a fatal alert indicating handshake_failure and marks
     * the corresponding SSL session is non-resumable.
     * <p />
     * @param msg string containing the exception message to be reported
     * @exception IOException with the specified string
     */
    private void complain(String msg) throws IOException {
        complain(new IOException(msg));
    }

    /**
     * Sends a fatal alert indicating handshake_failure and marks
     * the corresponding SSL session is non-resumable.
     * <p />
     * @param e the IOException to be reported
     * @exception IOException 
     */
    private void complain(IOException e) throws IOException {
        try {
            rec.alert(Record.FATAL, Record.HNDSHK_FAIL);
            if (sSessionId != null) {
                Session.del(peerHost, peerPort, sSessionId);
            }
        } catch (Throwable t) {
        }
        throw e;
    }
}

class Session {

    /**  Maximum number of cached resumable sessions. */
    private static final byte MAX_SESSIONS = 4;

    /**
     * Stores the last index where a session was overwritten, we
     * try to do a round-robin selection of places to overwrite
     */
    private static int delIdx = 0;

    /** Target host name. */
    String host;

    /** Target port number. */
    int port;

    /** Session identifier. */
    byte[] id;

    /** Master secret. */
    byte[] master;

    /** Target Certificate. */
    X509Certificate cert;

    /** A cache of currently resumable sessions. */
    private static Session[] sessions = new Session[MAX_SESSIONS];

    /**
     * Gets the master secret associated with a resumable session.
     * The session is uniquely identified by the combination of the
     * host, port.
     *
     * @param h host name of peer
     * @param p port number of peer
     *
     * @return matching session
     */
    static synchronized Session get(String h, int p) {
        for (int i = 0; i < MAX_SESSIONS; i++) {
            if ((sessions[i] == null) || (sessions[i].id == null)) continue;
            if (sessions[i].host.compareTo(h) == 0 && sessions[i].port == p) {
                return sessions[i];
            }
        }
        return null;
    }

    /**
     * Adds a new session with the specified parameters to the cache
     * of resumable sessions. At any given time, this class maintains
     * at most one resusumable session for any host/port pair.
     * <P />
     * @param h host name of peer
     * @param p port number of peer
     * @param id session identifier
     * @param mas master secret
     * @param cert certificate of peer
     */
    static synchronized void add(String h, int p, byte[] id, byte[] mas, X509Certificate cert) {
        int idx = MAX_SESSIONS;
        for (int i = 0; i < MAX_SESSIONS; i++) {
            if ((sessions[i] == null) || (sessions[i].id == null)) {
                idx = i;
                continue;
            }
            if ((sessions[i].host.compareTo(h) == 0) && (sessions[i].port == p)) {
                idx = i;
                break;
            }
        }
        if (idx == MAX_SESSIONS) {
            idx = delIdx;
            delIdx++;
            if (delIdx == MAX_SESSIONS) delIdx = 0;
        }
        if (sessions[idx] == null) {
            sessions[idx] = new Session();
        }
        sessions[idx].id = id;
        sessions[idx].master = new byte[mas.length];
        System.arraycopy(mas, 0, sessions[idx].master, 0, mas.length);
        sessions[idx].host = new String(h);
        sessions[idx].port = p;
        sessions[idx].cert = cert;
    }

    /**
     * Deletes the session identified by the specfied parameters
     * from the cache of resumable sessions.
     * <P />
     * @param h host name of peer
     * @param p port number of peer
     * @param sid session identifier
     */
    static synchronized void del(String h, int p, byte[] sid) {
        for (int i = 0; i < MAX_SESSIONS; i++) {
            if ((sessions[i] == null) || (sessions[i].id == null)) continue;
            if (Utils.byteMatch(sessions[i].id, 0, sid, 0, sid.length) && (sessions[i].host.compareTo(h) == 0) && (sessions[i].port == p)) {
                sessions[i].id = null;
                sessions[i].master = null;
                sessions[i].host = null;
                sessions[i].cert = null;
                break;
            }
        }
    }
}
