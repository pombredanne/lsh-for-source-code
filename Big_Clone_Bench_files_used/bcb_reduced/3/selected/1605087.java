package pdc.admincontrolserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import com.ice.jni.registry.NoSuchKeyException;
import com.ice.jni.registry.NoSuchValueException;
import com.ice.jni.registry.RegStringValue;
import com.ice.jni.registry.Registry;
import com.ice.jni.registry.RegistryException;
import com.ice.jni.registry.RegistryKey;

/**
 * The Class Protocol. All protocol related stuff is found here.
 * It uses the singleton pattern, by a public static final instance and a private constructor.
 * 
 */
public final class Protocol {

    /** The Constant BUFFER_SIZE. */
    public static final int BUFFER_SIZE = 10240;

    /** The Constant instance. */
    public static final Protocol instance = new Protocol();

    /** The SH a1 digest. */
    public MessageDigest SHA1Digest;

    /** The Constant UTF8Charset. */
    public final Charset UTF8Charset = Charset.forName("UTF-8");

    /** The Constant UTF8Decoder. */
    public final CharsetDecoder UTF8Decoder = UTF8Charset.newDecoder();

    /** The Constant UTF8Encoder. */
    public final CharsetEncoder UTF8Encoder = UTF8Charset.newEncoder();

    /** The Key Manager Factory. */
    private KeyManagerFactory kmf;

    /** The Trust Manager Factory. */
    private TrustManagerFactory tmf;

    /** The loaded keys. */
    private final HashSet<String> loadedKeys = new HashSet<String>(10, 0.7f);

    /** The loaded trusts. */
    private final HashSet<String> loadedTrusts = new HashSet<String>(17, 0.7f);

    /** The jaxb contexts. */
    private final HashMap<String, JAXBContext> jaxbContexts = new HashMap<String, JAXBContext>(18, 0.6f);

    /**
	 * Instantiates a new protocol.
	 */
    private Protocol() {
        try {
            SHA1Digest = MessageDigest.getInstance("SHA1");
            kmf = KeyManagerFactory.getInstance("SunX509");
            tmf = TrustManagerFactory.getInstance("SunX509");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Deserialize object from a stream by reading from an input stream (usually a socket stream)
	 * It uses the JAXB mechanism. The context is defined as an input parameter.
	 * 
	 * @param inputstream
	 *            the inputstream
	 * @param context
	 *            the context
	 * 
	 * @return the object
	 * 
	 * @throws JAXBException
	 *             the JAXB exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final Object deserializeMessageObject(final InputStream inputstream, final String context) throws JAXBException, IOException {
        String line = deserializeMessageReader(inputstream).getDataStream().readLine();
        return getUnmarshaller(context).unmarshal(new StringReader(line));
    }

    /**
	 * Deserialize message. Leaves open the mode of processing
	 * 
	 * ATTENTION: There is still data on the stream
	 * 
	 * @param inputstream
	 *            the inputstream
	 * 
	 * @return the protocol message
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final ProtocolMessage deserializeMessageReader(final InputStream inputstream) throws IOException {
        final BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(inputstream, UTF8Decoder), BUFFER_SIZE);
        final String cmd_string = bufferedreader.readLine();
        final String type_string = bufferedreader.readLine();
        final ProtocolCommand cmd = ProtocolCommand.valueOf(cmd_string);
        final ProtocolMessageType type = ProtocolMessageType.valueOf(type_string);
        return new ProtocolMessage(cmd, type, bufferedreader);
    }

    /**
	 * Deserialize object.
	 * 
	 * @param reader
	 *            the reader
	 * @param context
	 *            the context
	 * 
	 * @return the object
	 * 
	 * @throws JAXBException
	 *             the JAXB exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final Object deserializeObject(final BufferedReader reader, final String context) throws JAXBException, IOException {
        String line = reader.readLine();
        return getUnmarshaller(context).unmarshal(new StringReader(line));
    }

    /**
	 * Encrypt a cleartext with SHA1.
	 * 
	 * @param cleartext
	 *            the cleartext
	 * 
	 * @return the string
	 */
    public final String encryptSHA1(final String cleartext) {
        synchronized (SHA1Digest) {
            SHA1Digest.reset();
            return new BigInteger(SHA1Digest.digest(cleartext.getBytes(UTF8Charset))).toString(16);
        }
    }

    /**
	 * Generate email from name.
	 * 
	 * @param student_name
	 *            the student_name
	 * 
	 * @return the string
	 */
    public String generateEmailFromName(String student_name) {
        String[] split = student_name.split(" ");
        StringBuilder gen = new StringBuilder(student_name.length() + 10);
        if (split.length >= 2) {
            for (int i = 0; i < split.length - 2; i++) {
                gen.append(split[i]);
                gen.append('-');
            }
            gen.append(split[split.length - 2]);
            gen.append('.');
            gen.append(split[split.length - 1]);
            gen.append("@i-u.de");
        } else gen.append("Invalid student name supplied!");
        return gen.toString().toLowerCase();
    }

    /**
	 * Gets the JAXB context object for a given package. This just serves as a
	 * Instance-cache (hash-map based)
	 * 
	 * Example: pdc.xml.logincommand
	 * 
	 * @param context_pgk
	 *            the context_pgk
	 * 
	 * @return the jAXB context
	 * 
	 * @throws JAXBException
	 *             the JAXB exception
	 */
    public final JAXBContext getJAXBContext(final String context_pgk) throws JAXBException {
        JAXBContext context = jaxbContexts.get(context_pgk);
        if (context == null) {
            context = JAXBContext.newInstance(context_pgk);
            jaxbContexts.put(context_pgk, context);
        }
        return context;
    }

    /**
	 * Gets the key manager factory.
	 * 
	 * @return the key manager factory
	 */
    public final KeyManagerFactory getKeyManagerFactory() {
        return kmf;
    }

    /**
	 * Gets the marshaller from the given instance out of the HashMap instance
	 * cache.
	 * 
	 * @param context_pgk
	 *            the context_pgk
	 * 
	 * @return the marshaller
	 * 
	 * @throws JAXBException
	 *             the JAXB exception
	 */
    public final Marshaller getMarshaller(final String context_pgk) throws JAXBException {
        return getJAXBContext(context_pgk).createMarshaller();
    }

    /**
	 * Gets the trust manager factory.
	 * 
	 * @return the trust manager factory
	 */
    public final TrustManagerFactory getTrustManagerFactory() {
        return tmf;
    }

    /**
	 * Gets the unmarshaller from the given instance out of the HashMap instance
	 * cache.
	 * 
	 * @param context_pgk
	 *            the context_pgk
	 * 
	 * @return the unmarshaller
	 * 
	 * @throws JAXBException
	 *             the JAXB exception
	 */
    public final Unmarshaller getUnmarshaller(final String context_pgk) throws JAXBException {
        return getJAXBContext(context_pgk).createUnmarshaller();
    }

    /**
	 * Load key store.
	 * 
	 * @param cert
	 *            the cert
	 * @param passphrase
	 *            the passphrase
	 * 
	 * @return the key store
	 * 
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws CertificateException
	 *             the certificate exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws KeyStoreException
	 *             the key store exception
	 */
    public final KeyStore loadKeyStore(final String cert, final String passphrase) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream stream = new FileInputStream(new File("cert/" + cert + ".jks"));
        char[] pass = passphrase.toCharArray();
        keyStore.load(stream, pass);
        return keyStore;
    }

    /**
	 * Populate key manager.
	 * 
	 * @param cert
	 *            the cert
	 * @param passphrase
	 *            the passphrase
	 * 
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws CertificateException
	 *             the certificate exception
	 * @throws KeyStoreException
	 *             the key store exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws UnrecoverableKeyException
	 *             the unrecoverable key exception
	 */
    public final void populateKeyManager(final String cert, final String passphrase) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableKeyException {
        if (!loadedKeys.contains(cert)) {
            final KeyStore ks = loadKeyStore(cert, passphrase);
            kmf.init(ks, passphrase.toCharArray());
        }
    }

    /**
	 * Populate trust manager.
	 * 
	 * @param cert
	 *            the cert
	 * @param passphrase
	 *            the passphrase
	 * 
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws CertificateException
	 *             the certificate exception
	 * @throws KeyStoreException
	 *             the key store exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final void populateTrustManager(final String cert, final String passphrase) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        if (!loadedTrusts.contains(cert)) {
            final KeyStore ks = loadKeyStore(cert, passphrase);
            tmf.init(ks);
        }
    }

    /**
	 * Read admin password.
	 * 
	 * @return the string
	 * 
	 * @throws NoSuchKeyException
	 *             the no such key exception
	 * @throws RegistryException
	 *             the registry exception
	 */
    public final String readAdminPassword() throws NoSuchKeyException, RegistryException {
        RegistryKey regkey = Registry.HKEY_LOCAL_MACHINE.openSubKey("SOFTWARE");
        regkey = regkey.createSubKey("IUAutoConfig", "Key", RegistryKey.ACCESS_ALL);
        String value = "2bf5c1e4a6ccbb1ff833911fd05b20588ef3a6ed";
        final String adminPWD = "adminPWD";
        try {
            value = regkey.getStringValue(adminPWD);
        } catch (NoSuchValueException nsve) {
            regkey.setValue(new RegStringValue(regkey, adminPWD, value));
        }
        regkey.closeKey();
        return value;
    }

    /**
	 * Read out xml.
	 * 
	 * @param br
	 *            the br
	 * 
	 * @return the string builder
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final StringBuilder readOutXML(final BufferedReader br) throws IOException {
        final StringBuilder xml = new StringBuilder(100);
        String line = null;
        while ((line = br.readLine()) != null && line.length() > 0) {
            System.out.println("L: " + line);
            xml.append(line);
        }
        return xml;
    }

    /**
	 * Serialize object with header.
	 * 
	 * @param output
	 *            the output
	 * @param cmd
	 *            the cmd
	 * @param type
	 *            the type
	 * @param obj
	 *            the obj
	 * @param context
	 *            the context
	 * 
	 * @throws JAXBException
	 *             the JAXB exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final void serializeMessageObject(final OutputStream output, final ProtocolCommand cmd, final ProtocolMessageType type, Object obj, String context) throws JAXBException, IOException {
        final BufferedWriter bw = writeProtocolHeader(output, cmd, type);
        getMarshaller(context).marshal(obj, bw);
        bw.write('\n');
        bw.flush();
    }

    /**
	 * Serialize message query.
	 * @param <T> The database column class. See @see pdc.admincontrolserver.SQLConnection
	 * 
	 * @param output
	 *            the output
	 * @param cmd
	 *            the cmd
	 * @param type
	 *            the type
	 * @param sql
	 *            the sql
	 * @param cols
	 *            the cols
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final <T extends Enum<T>> void serializeMessageQuery(final OutputStream output, final ProtocolCommand cmd, final ProtocolMessageType type, final String sql, final EnumSet<T> cols) throws IOException {
        final BufferedWriter bw = writeProtocolHeader(output, cmd, type);
        bw.write(sql);
        bw.write('\n');
        bw.write(SQLConnection.ColumnSetToColumnList(cols));
        bw.write('\n');
        bw.write('\n');
        bw.flush();
    }

    /**
	 * Serialize a string message. Writes a protocol header to the given
	 * outputstream and pushes a string to the outputstream terminated with a
	 * newline and flushes the outputstream
	 * 
	 * @param output
	 *            the output
	 * @param cmd
	 *            the cmd
	 * @param type
	 *            the type
	 * @param data
	 *            the data
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * 
	 * @see #writeProtocolHeader(OutputStream, ProtocolCommand,
	 *      ProtocolMessageType)
	 */
    public final void serializeMessageString(final OutputStream output, final ProtocolCommand cmd, final ProtocolMessageType type, final String data) throws IOException {
        final BufferedWriter bw = writeProtocolHeader(output, cmd, type);
        bw.write(data);
        bw.write('\n');
        bw.write('\n');
        bw.flush();
    }

    /**
	 * Serialize message writer. Writes a protocol header to the given
	 * outputstream and returns the buffered writer for writing to the
	 * outputstream
	 * 
	 * @param output
	 *            the outputstream for writing (usually an OutputStream of a
	 *            Socket)
	 * @param cmd
	 *            the cmd
	 * @param type
	 *            the type
	 * 
	 * @return the buffered writer open for writing to the stream
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * 
	 * @see #writeProtocolHeader(OutputStream, ProtocolCommand,
	 *      ProtocolMessageType)
	 * 
	 * ATTENTION: writer MUST be flushed and has to contain a empty line at the
	 * end writer HAS NOT to be closed.
	 */
    public final BufferedWriter serializeMessageWriter(final OutputStream output, final ProtocolCommand cmd, final ProtocolMessageType type) throws IOException {
        return writeProtocolHeader(output, cmd, type);
    }

    /**
	 * Sets the admin password.
	 * 
	 * @param sha1string
	 *            the new admin password
	 * 
	 * @throws RegistryException
	 *             the registry exception
	 */
    public final void setAdminPassword(String sha1string) throws RegistryException {
        RegistryKey regkey = Registry.HKEY_LOCAL_MACHINE.openSubKey("SOFTWARE");
        regkey = regkey.createSubKey("IUAutoConfig", "Key", RegistryKey.ACCESS_ALL);
        regkey.setValue(new RegStringValue(regkey, "adminPWD", sha1string));
        regkey.closeKey();
    }

    /**
	 * Write protocol header.
	 * 
	 * @param output
	 *            the output
	 * @param cmd
	 *            the cmd
	 * @param type
	 *            the type
	 * 
	 * @return the buffered writer
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    private final BufferedWriter writeProtocolHeader(final OutputStream output, final ProtocolCommand cmd, final ProtocolMessageType type) throws IOException {
        final BufferedWriter bufferedwriter = new BufferedWriter(new OutputStreamWriter(output, UTF8Encoder), BUFFER_SIZE);
        bufferedwriter.write(cmd.name());
        bufferedwriter.write('\n');
        bufferedwriter.write(type.name());
        bufferedwriter.write('\n');
        return bufferedwriter;
    }
}
