package com.dsrsc.impl.auth;

import com.sun.sgs.auth.Identity;
import com.sun.sgs.auth.IdentityAuthenticator;
import com.sun.sgs.auth.IdentityCredentials;
import com.sun.sgs.impl.auth.IdentityImpl;
import com.sun.sgs.impl.auth.NamePasswordAuthenticator;
import com.sun.sgs.impl.auth.NamePasswordCredentials;
import com.sun.sgs.impl.kernel.StandardProperties;
import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.CredentialException;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author Lothy
 */
public class CreateUserAuthenticator implements IdentityAuthenticator {

    /**
     * The property used to define the password file location.
     */
    public static final String PASSWORD_FILE_PROPERTY = "com.sun.sgs.impl.auth.NamePasswordAuthenticator.PasswordFile";

    /**
     * The default name for the password file, relative to the app root.
     */
    public static final String DEFAULT_FILE_NAME = "passwords";

    private final HashMap<String, byte[]> passwordMap;

    private final MessageDigest digest;

    private final File passwordFile;

    /**
     * Creates an instance of <code>NamePasswordAuthenticator</code>.
     *
     * @param properties the application's configuration properties
     * @throws FileNotFoundException    if the password file cannot be found
     * @throws IOException              if any error occurs reading the password file
     * @throws NoSuchAlgorithmException if SHA-256 is not supported
     */
    public CreateUserAuthenticator(Properties properties) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        if (properties == null) throw new NullPointerException("Null properties not allowed");
        String passFile = properties.getProperty(PASSWORD_FILE_PROPERTY);
        if (passFile == null) {
            String root = properties.getProperty(StandardProperties.APP_ROOT);
            passFile = root + File.separator + DEFAULT_FILE_NAME;
        }
        passwordFile = new File(passFile);
        FileInputStream in = new FileInputStream(passwordFile);
        StreamTokenizer stok = new StreamTokenizer(new InputStreamReader(in));
        stok.eolIsSignificant(false);
        passwordMap = new HashMap<String, byte[]>();
        while (stok.nextToken() != StreamTokenizer.TT_EOF) {
            String name = stok.sval;
            if (stok.nextToken() == StreamTokenizer.TT_EOF) throw new IOException("Unexpected EOL at line " + stok.lineno());
            byte[] password = decodeBytes(stok.sval.getBytes("UTF-8"));
            passwordMap.put(name, password);
        }
        digest = MessageDigest.getInstance("SHA-256");
    }

    /**
     * Decodes an array of bytes that has been encoded by a call to
     * <code>encodeBytes</code>. This results in the original binary
     * representation. This is used to decode a hashed password from the
     * password file.
     *
     * @param bytes an encoded array of bytes as provided by a call
     *              to <code>encodePassword</code>
     * @return the original binary representation
     */
    public static byte[] decodeBytes(byte[] bytes) {
        byte[] decoded = new byte[bytes.length / 2];
        for (int i = 0; i < decoded.length; i++) {
            int encodedIndex = i * 2;
            decoded[i] = (byte) (((bytes[encodedIndex] - 'a') << 4) + (bytes[encodedIndex + 1] - 'a'));
        }
        return decoded;
    }

    /**
     * Encodes an array of bytes in a form suitable for including in a text
     * file. In this case, a very simple base-16 encoding is used. The
     * original binary representation can be resolved by calling
     * <code>decodeBytes</code>. This is used to turn a hashed password
     * into a form suitable for the password file.
     *
     * @param bytes an array of bytes
     * @return an encoding of the bytes in a form suitable for use in text
     */
    public static byte[] encodeBytes(byte[] bytes) {
        byte[] encoded = new byte[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int encodedIndex = i * 2;
            encoded[encodedIndex] = (byte) (((bytes[i] & 0xF0) >> 4) + 'a');
            encoded[encodedIndex + 1] = (byte) ((bytes[i] & 0x0F) + 'a');
        }
        return encoded;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSupportedCredentialTypes() {
        return new String[] { NamePasswordCredentials.TYPE_IDENTIFIER };
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The provided <code>IdentityCredentials</code> must be an instance
     * of <code>NamePasswordCredentials</code>.
     *
     * @throws AccountNotFoundException if the identity is unknown
     * @throws CredentialException      if the credentials are invalid
     */
    public Identity authenticateIdentity(IdentityCredentials credentials) throws AccountNotFoundException, CredentialException {
        if (!(credentials instanceof NamePasswordCredentials)) throw new CredentialException("unsupported credentials");
        NamePasswordCredentials npc = (NamePasswordCredentials) credentials;
        String name = npc.getName();
        byte[] validPass = passwordMap.get(name);
        if (validPass == null) {
            byte[] pass;
            synchronized (digest) {
                digest.reset();
                try {
                    pass = digest.digest((new String(npc.getPassword())).getBytes("UTF-8"));
                } catch (IOException e) {
                    throw new CredentialException("Could not calculate " + "new account password: " + e.getMessage());
                }
            }
            passwordMap.put(name, pass);
            try {
                FileOutputStream out = new FileOutputStream(passwordFile);
                out.write(name.getBytes("UTF-8"));
                out.write("\t".getBytes("UTF-8"));
                out.write(NamePasswordAuthenticator.encodeBytes(pass));
                out.write("\n".getBytes("UTF-8"));
                out.close();
            } catch (IOException e) {
                throw new CredentialException("Could not save new account " + "credentials: " + e.getMessage());
            }
            return new IdentityImpl(name);
        }
        byte[] pass;
        synchronized (digest) {
            digest.reset();
            try {
                pass = digest.digest((new String(npc.getPassword())).getBytes("UTF-8"));
            } catch (IOException ioe) {
                throw new CredentialException("Could not get password: " + ioe.getMessage());
            }
        }
        if (!Arrays.equals(validPass, pass)) throw new CredentialException("Invalid credentials");
        return new IdentityImpl(name);
    }
}
