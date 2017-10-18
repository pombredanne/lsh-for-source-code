package de.sicari.kernel.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import codec.Base64;
import codec.CorruptedCodeException;

/**
 * This class represents a password hash, e.g. as provided by an <tt>LDAP</tt>
 * database. The digest must be provided in one of the following formats:
 * <p>
 * <nobreak><tt>'{'&lt;algorithm&gt;'}'base64(&lt;hash&gt;[+&lt;salt&gt;])</tt></nobreak>
 * <p>
 * or
 * <p>
 * <nobreak><tt>base64('{'&lt;algorithm&gt;'}'base64(&lt;hash&gt;[+&lt;salt&gt;]))</tt></nobreak>
 * <p>
 * The {@link #verify(char[])} method may be used to check a given password
 * against a <code>PasswordHash</code> instance.
 * <p>
 * See also: ManPage of the OpenLDAP tool 'slappasswd'; RFCs 2256, 2307, 3112.
 *
 * @author Matthias Pressfreund
 * @author Jan Peters
 * @version $Id: PasswordHash.java 204 2007-07-11 19:26:55Z jpeters $
 */
public class PasswordHash {

    /**
     * The type identifier for the <i>SHA</i> algorithm
     */
    public static final int TYPE_SHA = 10;

    /**
     * The type identifier for the <i>Salted SHA</i> algorithm
     */
    public static final int TYPE_SSHA = 11;

    /**
     * The type identifier for the <i>MD5</i> algorithm
     */
    public static final int TYPE_MD5 = 12;

    /**
     * The type identifier for the <i>Salted MD5</i> algorithm
     */
    public static final int TYPE_SMD5 = 13;

    /**
     * The input hash (if required pre-decoded to
     * <nobreak><tt>'{'&lt;algorithm&gt;'}'base64(&lt;hash&gt;[+&lt;salt&gt;])</tt></nobreak>
     * format)
     */
    protected String str_;

    /**
     * The appropriate message digester
     */
    protected MessageDigest md_;

    /**
     * The actual password hash
     */
    protected byte[] hash_;

    /**
     * The salt, if existent
     */
    protected byte[] salt_;

    /**
     * The type identifier
     */
    protected int type_;

    /**
     * Creates an instance of the class <code>PasswordHash</code>.
     *
     * @param digest The digest, e.g. as provided from an <tt>LDAP</tt> database
     *
     * @throws CorruptedCodeException in case the <code>digest</code> contains
     *   invalid code
     * @throws NoSuchAlgorithmException in case the algorithm used for creating
     *   the <code>digest</code> is unknown or not supported
     * @throws NullPointerException if <code>digest</code> is <code>null</code>
     */
    public PasswordHash(byte[] digest) throws CorruptedCodeException, NoSuchAlgorithmException {
        if (digest == null) {
            throw new NullPointerException("digest");
        }
        str_ = new String(digest);
        if (!str_.startsWith("{") || (str_.indexOf('}') < 0)) {
            str_ = new String(Base64.decode(digest));
        }
        if (str_.regionMatches(true, 0, "{SHA}", 0, 5)) {
            type_ = TYPE_SHA;
            md_ = MessageDigest.getInstance("SHA-1");
            hash_ = Base64.decode(str_.substring(5));
            salt_ = new byte[0];
        } else if (str_.regionMatches(true, 0, "{SSHA}", 0, 6)) {
            byte[] hs;
            type_ = TYPE_SSHA;
            md_ = MessageDigest.getInstance("SHA-1");
            hs = Base64.decode(str_.substring(6));
            hash_ = new byte[20];
            salt_ = new byte[hs.length - hash_.length];
            System.arraycopy(hs, 0, hash_, 0, hash_.length);
            System.arraycopy(hs, hash_.length, salt_, 0, salt_.length);
        } else if (str_.regionMatches(true, 0, "{MD5}", 0, 5)) {
            type_ = TYPE_MD5;
            md_ = MessageDigest.getInstance("MD5");
            hash_ = Base64.decode(str_.substring(5));
            salt_ = new byte[0];
        } else if (str_.regionMatches(true, 0, "{SMD5}", 0, 6)) {
            byte[] hs;
            type_ = TYPE_SMD5;
            md_ = MessageDigest.getInstance("MD5");
            hs = Base64.decode(str_.substring(6));
            hash_ = new byte[16];
            salt_ = new byte[hs.length - hash_.length];
            System.arraycopy(hs, 0, hash_, 0, hash_.length);
            System.arraycopy(hs, hash_.length, salt_, 0, salt_.length);
        } else {
            throw new NoSuchAlgorithmException("Unrecognized algorithm");
        }
    }

    /**
     * Create a <code>PashwordHash</code> from the given password <code>pw</code>,
     * the with the given hash function <code>type</code>, and the given
     * <code>salt</code>.
     *
     * Valid hash <code>type</code>s are:
     * <ul>
     * <li> <code>TYPE_SHA</code>
     * <li> <code>TYPE_SSHA</code>
     * <li> <code>TYPE_MD5</code>
     * <li> <code>TYPE_SMD5</code>
     * </ul>
     *
     * The <code>salt</code> is only needed if a salted hash function
     * is selected with <code>type</code>.
     *
     * @param pw The password to generate the password hash from.
     * @param type The type of the hash function to use.
     * @param salt The salt to use, when generating a salted password hash.
     * @throws NoSuchAlgorithmException in case the hash function for creating
     *   the <code>PasswordHash</code> is unknown or not supported.
     */
    public PasswordHash(byte[] pw, int type, byte[] salt) throws NoSuchAlgorithmException {
        byte[] binary;
        if (pw == null) {
            throw new NullPointerException("pw");
        }
        type_ = type;
        switch(type_) {
            case TYPE_SHA:
                md_ = MessageDigest.getInstance("SHA-1");
                md_.update(pw);
                hash_ = md_.digest();
                md_.reset();
                salt_ = new byte[0];
                str_ = "{SHA}" + Base64.encode(hash_);
                break;
            case TYPE_SSHA:
                if (salt == null) {
                    throw new NullPointerException("salt");
                }
                salt_ = salt;
                md_ = MessageDigest.getInstance("SHA-1");
                md_.update(pw);
                md_.update(salt);
                hash_ = md_.digest();
                md_.reset();
                binary = new byte[hash_.length + salt_.length];
                System.arraycopy(hash_, 0, binary, 0, hash_.length);
                System.arraycopy(salt_, 0, binary, hash_.length, salt_.length);
                str_ = "{SSHA}" + Base64.encode(binary);
                break;
            case TYPE_MD5:
                md_ = MessageDigest.getInstance("MD5");
                md_.update(pw);
                hash_ = md_.digest();
                md_.reset();
                salt_ = new byte[0];
                str_ = "{MD5}" + Base64.encode(hash_);
                break;
            case TYPE_SMD5:
                if (salt == null) {
                    throw new NullPointerException("salt");
                }
                salt_ = salt;
                md_ = MessageDigest.getInstance("MD5");
                md_.update(pw);
                md_.update(salt);
                hash_ = md_.digest();
                md_.reset();
                binary = new byte[hash_.length + salt_.length];
                System.arraycopy(hash_, 0, binary, 0, hash_.length);
                System.arraycopy(salt_, 0, binary, hash_.length, salt_.length);
                str_ = "{SMD5}" + Base64.encode(binary);
                break;
            default:
                throw new NoSuchAlgorithmException("Unrecognized algorithm");
        }
    }

    /**
     * Returns the password hash.
     *
     * @return the password hash.
     */
    public byte[] getHash() {
        return hash_;
    }

    /**
     * Returns the password salt. If no salt is used
     * to generate the <code>PasswordHash</code>, this
     * method returns and empty byte array.
     *
     * @return the password salt.
     */
    public byte[] getSalt() {
        return salt_;
    }

    /**
     * Returns the hash function type
     * used to generate the <code>PasswordHash</code>.
     *
     * @return the hash function type.
     */
    public int getType() {
        return type_;
    }

    /**
     * Check if the given <code>password</code> has a matching hash.
     * The given password is transformed to a byte array using
     * <code>String.valueOf(password).getBytes()</code>.
     *
     * @param password The password to verify
     *
     * @return Whether or not the given <code>password</code> matches this
     *   <code>PasswordHash</code>
     */
    public boolean verify(char[] password) {
        if (password == null) return false;
        md_.reset();
        md_.update(String.valueOf(password).getBytes());
        md_.update(salt_);
        return MessageDigest.isEqual(hash_, md_.digest());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof PasswordHash) {
            PasswordHash pwh = (PasswordHash) obj;
            return (Arrays.equals(hash_, pwh.hash_) && Arrays.equals(salt_, pwh.salt_) && (type_ == pwh.type_));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (hash_.hashCode() + salt_.hashCode() + type_);
    }

    @Override
    public String toString() {
        return str_;
    }

    /**
     * Main method generates a PasswordHash from the
     * given command line parameters.
     *
     * <code>USAGE: java PasswordHash &lt;password&gt; [&lt;type&gt;] [&lt;salt&gt;]</code>
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        PasswordHash pwh;
        String password;
        String salt;
        int type;
        if (args.length < 1) {
            System.out.println("\n  USAGE: java " + PasswordHash.class.getName() + " <password> [<type>] [<salt>]\n");
            System.out.println("with <type>: " + TYPE_SHA + " (= SHA-1), " + TYPE_SSHA + " (= Salted SHA-1), " + TYPE_MD5 + " (= MD5), " + TYPE_SMD5 + " (= Salted MD5)\n");
            System.exit(0);
        }
        password = args[0];
        if (args.length > 1) {
            type = Integer.parseInt(args[1]);
        } else {
            type = TYPE_SSHA;
        }
        if (args.length > 2) {
            salt = args[2];
        } else {
            salt = Long.toString(System.currentTimeMillis());
        }
        pwh = new PasswordHash(password.getBytes(), type, salt.getBytes());
        System.out.println("PasswordHash = " + pwh);
    }
}
