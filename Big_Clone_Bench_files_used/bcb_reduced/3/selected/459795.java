package com.worldware.ichabod;

import com.worldware.mail.MailAddress;
import java.util.Date;
import java.text.*;
import java.security.*;

/**
 * DynamicAddresses are email addresses that contain expiration 
 * dates, and other conditions that must be met, for the address
 * to be considered valid. 
 * <P>
 * DynamicAddresses may be cryptographically signed, to prevent tampering.
 * <P>
 * DynamicAddresses currently contain four parts: The destination name. (Not currently
 * hidden, that should change before too long), the expiration date, the required sender and
 * the cryptographic signature (MD5 hash of the other data, plus a secret.)
*/
public class DynamicAddress {

    static final String PREFIX = "..";

    static final String SEP = PREFIX;

    private static final String formatDateString = "MM/dd/yyyy HH:mm:ss";

    private static final SimpleDateFormat formatDate = new SimpleDateFormat(formatDateString);

    /** The name this address is an alias for
	 */
    private String m_baseName;

    /** May be null, if no expiration date.
	 */
    private Date m_expirationDate;

    /** May be null, if no required sender.
	 * @note Later, we may want to generalize this as "header, value" pairs.
	 */
    private MailAddress m_requiredSender;

    /** We may want to be able to limit by host name, or host IP address
	 */
    private String hostName;

    /** The MD5 hash value, of the rest of the data, converted to a dynamic
	 * address, then converted to a string.
	 */
    private String m_checksum;

    /** The private key, used to initialized the MD5 hash.
	 */
    private String m_key;

    /** This is a version of the constructor for parsing a dynamic address
	 * that is currently in String form.<P>
	 * This is typically used when you receive a DynamicAddress in an email
	 * message, and want to convert it back to Object form.
	 * 
	 * @param dyn A string representing a DynamicAddress. 
	 */
    public DynamicAddress(String dyn) {
        this(dyn, null);
    }

    /** I'm not sure this is needed. You can't know the key for the dynamic address, until
	 * you have parsed the dynamic address, and determined what the base name is.
	 */
    private DynamicAddress(String dyn, String key) {
        if (dyn.length() < SEP.length() * 2) throw new IllegalArgumentException("Dynamic Address is '" + dyn + "' is too short to be valid");
        int lastStart = PREFIX.length();
        int cur = lastStart + 1;
        int nextDelim = dyn.indexOf(SEP, cur);
        while (nextDelim != -1) {
            String tok = dyn.substring(lastStart, nextDelim);
            processTok(tok, dyn);
            lastStart = nextDelim + SEP.length();
            cur = lastStart + 1;
            nextDelim = dyn.indexOf(SEP, cur);
        }
        m_key = key;
    }

    private static final char KEY_ADDRESS = 'A';

    private static final char KEY_EXPIRE = 'X';

    private static final char KEY_SENDER = 'S';

    private static final char KEY_CHECKSUM = 'C';

    protected void processTok(String token, String dynForErrorMsg) {
        char key = token.charAt(0);
        token = token.substring(1);
        switch(key) {
            case KEY_ADDRESS:
                m_baseName = decode(token);
                break;
            case KEY_EXPIRE:
                {
                    m_expirationDate = new Date(Long.parseLong(token, 36));
                    break;
                }
            case KEY_SENDER:
                m_requiredSender = new MailAddress(decode(token));
                break;
            case KEY_CHECKSUM:
                m_checksum = token;
                break;
            default:
                throw new RuntimeException("Invalid token '" + token + "' in DynamicAddress '" + dynForErrorMsg + "'");
        }
    }

    /** Create a dynamic address from the specified paramters.
	 * @param name The name of the account that this address maps to. (Not address (foo@demo.listsite.com), just the account part (foo)).
	 * @param expires If not null, the date that you want the email address to expire. If null, it never expires
	 * @param sender If not null, the required sender of the address. If null, no required sender.
	 */
    public DynamicAddress(String name, Date expires, MailAddress sender) {
        this(name, expires, sender, null);
    }

    /**
	 * @param name The name of the account that this address maps to. (Not address (foo@demo.listsite.com), just the account part (foo)).
	 * @param expires If not null, the date that you want the email address to expire. If null, it never expires
	 * @param sender If not null, the required sender of the address. If null, no required sender.
	 * @param key A secret key used to generate the cryptographic signature. If null, no
	 * signature is generated.
	 */
    public DynamicAddress(String name, Date expires, MailAddress sender, String key) {
        m_baseName = name;
        m_expirationDate = expires;
        m_requiredSender = sender;
        m_key = key;
        m_checksum = generateHashString();
    }

    /** Checks to see is the specified name appears to be a DynamicAddress
	 * @param name The name to check (does not include the "@hostname.com", just the name).
	 */
    public static boolean isDynamicAddress(String name) {
        if (!name.startsWith(PREFIX)) return false;
        return true;
    }

    /** Dump this object for debugging.
	 * @see #createString
	 */
    public String toString() {
        String delim = ", ";
        return "[DynamicAddress: " + m_baseName + delim + "exp = " + ((m_expirationDate == null) ? "null" : formatDate.format(m_expirationDate)) + delim + "Hash = " + m_checksum + delim + "requiredSender = " + m_requiredSender + ((m_key != null) ? (delim + "Key used") : (delim + "No key used")) + "]";
    }

    /** @return true if this address has expired (true == bad!)
	 */
    public boolean expired() {
        if (m_expirationDate == null) return false;
        Date d = new Date();
        if (d.after(m_expirationDate)) return true;
        return false;
    }

    /** Convert the dynamic address to usable account name (not address).
	 * @return A DynamicAddress as a String. Note that the address does not include the @hostname part, it's 
	 * only the username.
	 * @see #createAddress
	 */
    public String createString() {
        boolean includeChecksum = ((m_key != null) && (m_key.trim().length() > 0));
        return createString(includeChecksum);
    }

    /** 
	 * Convert the dynamic address to an email address
	 * @see #createString
	 */
    public MailAddress createAddress(String hostName) {
        return new MailAddress(hostName, createString());
    }

    /** 
	 * Convert the dynamic address to usable (mailable) string form
	 * <B>Note that the address does not include the @hostname part, it's 
	 * only the username.</B>
	 * @param includeChecksum true means include the checksum in the email address.
	 * This makes tampering more difficult, and pretty much prevents it completely
	 * if a key is used.
	 */
    protected String createString(boolean includeChecksum) {
        StringBuffer sb = new StringBuffer(64);
        sb.append(PREFIX);
        sb.append(this.KEY_ADDRESS + encode(m_baseName));
        if (m_expirationDate != null) {
            long l = m_expirationDate.getTime();
            String base36 = Long.toString(l, 36);
            sb.append(SEP + this.KEY_EXPIRE + base36);
        }
        if (m_requiredSender != null) sb.append(SEP + this.KEY_SENDER + encode(m_requiredSender.toString()));
        if (includeChecksum && (null != m_checksum)) {
            sb.append(SEP + this.KEY_CHECKSUM + encode(m_checksum));
        }
        sb.append(SEP);
        return sb.toString();
    }

    /** Converts a byte array to characters that we can use in an email address.
	 * @param base The byte array to convert.
	 */
    String convertNumericBytesToString(byte[] base) {
        String converted = com.worldware.misc.md5.byteToHex(base);
        return converted;
    }

    /** @return true if the checksum is correct.
	 * If there was no checksum, it is always valid.
	 */
    public boolean isValid() {
        if (m_checksum == null) {
            return true;
        }
        String s = generateHashString();
        if (s.equals(m_checksum)) return true;
        return false;
    }

    /** Generates a hash code for this address, so we can tell if it has
	 * been tampered with. 
	 * @return A String containing the hash
	 * @see #generateHashBytes
	 */
    protected String generateHashString() {
        byte[] hash = generateHashBytes();
        String hashed = convertNumericBytesToString(hash);
        return hashed;
    }

    /** Generates a hash code for this address, so we can tell if it has
	 * been tampered with. 
	 * @return The bytes of the hash.
	 * @see #generateHashString
	 */
    protected byte[] generateHashBytes() {
        String s = createString(false);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsa) {
            System.out.println("Can't get MD5 implementation " + nsa);
            throw new RuntimeException("DynanmicAddress2: Can't get MD5 implementation");
        }
        if (m_key != null) md.update(m_key.getBytes(), 0, m_key.length());
        md.update(s.getBytes(), 0, s.length());
        byte[] hash = md.digest();
        return hash;
    }

    /** Compares the name to the base name of this DynamicAddress
	 * @param name The name to check against.
	 */
    public boolean matchesName(String name) {
        return m_baseName.equalsIgnoreCase(name);
    }

    /** Checks to see if the specified address matches the required address specified for 
	 * this DynamicAddress.
	 * <P>
	 * Right now this checks for equality of the whole email address. Probably more 
	 * useful would be to match the end, so you could specify only a domain, or
	 * address + domain. 
	 * @param sender The address we are checking against the required address
	 * @return true if no required address was specified, or if the addresses match, else false.
	 * @see com.worldware.mail.MailAddress
	 */
    public boolean matchesSender(MailAddress sender) {
        if (m_requiredSender == null) return true;
        if (sender == null) return false;
        String req = m_requiredSender.toString();
        if (req.indexOf(MailAddress.hostSeperator) == -1) {
            String sen = sender.toString();
            return sen.endsWith(req);
        }
        return m_requiredSender.equals(sender);
    }

    /** Compares the expiration date of this DynamicAddress against a specified date.
	 * @param otherDate The date to check against.
	 * @return true if otherDate matches the expiration date of this address
	 */
    public boolean matchesExpiration(Date otherDate) {
        if ((otherDate == null) && (m_expirationDate == null)) return true;
        if (m_expirationDate == null) return false;
        return m_expirationDate.equals(otherDate);
    }

    /** Checks to see if this DynamicAddress matches the specified DynamicAddress.
	 * @param da The address to compare to.
	 */
    public boolean equals(DynamicAddress da) {
        if (!matchesName(da.getName())) return false;
        if (!matchesExpiration(da.m_expirationDate)) return false;
        if (!matchesSender(da.m_requiredSender)) return false;
        return true;
    }

    /** Inverse of encode
	 * @see #encode
	 */
    String decode(String token) {
        StringBuffer sb = new StringBuffer(token.length());
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (ch == escape) {
                if (i == token.length() - 1) return sb.toString();
                char ch2 = token.charAt(++i);
                if (ch2 == 'A') ch = '@'; else if (ch2 == '.') ch = '.'; else if (ch2 == escape) ch = escape; else throw new RuntimeException("Invalid escape sequence '" + ch + "' followed by '" + ch2 + "'. ");
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /** Used as an escape character when encoding dynamic addresses 
	 * Similar to backslash in C style strings.
	 */
    private static final char escape = '_';

    /** Makes sure that all of the characters in the token are legal for an email address
	 * and do not conflict with the markers used by this class
	 * @see #decode
	 */
    String encode(String token) {
        StringBuffer sb = new StringBuffer(token.length() + 20);
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (ch == '.') {
                sb.append(escape);
                sb.append('.');
                continue;
            }
            if (ch == '@') {
                sb.append(escape);
                sb.append('A');
                continue;
            }
            if (ch == escape) {
                sb.append(escape);
                sb.append(escape);
                continue;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /** Gets the name of the underlying account
	 */
    public String getName() {
        return m_baseName;
    }

    /** main is used for testing/debugging.
	 */
    public static void main(String args[]) {
        if (args.length == 0) {
            System.out.println("usage da name [date [ sender [key] ]");
            System.exit(1);
        }
        int i = 0;
        String name = args[i++];
        String key = null;
        Date expires = null;
        MailAddress sender = null;
        if (args.length > i) expires = new Date(args[i++]);
        if (args.length > i) sender = new MailAddress(args[i++]);
        if (args.length > i) key = args[i++];
        Date now = new Date();
        DynamicAddress da = new DynamicAddress(name, expires, sender, key);
        System.out.println("Name = '" + name + "'");
        System.out.println("String form = '" + da.createString() + "'");
        System.out.println("toString() = '" + da.toString() + "'");
        System.out.println("MD5 hash  = '" + da.generateHashString() + "'");
        System.out.println("isValid() = " + da.isValid());
        System.out.println();
        DynamicAddress da2 = new DynamicAddress(da.createString(), key);
        System.out.println("Recreating...");
        System.out.println("String form = '" + da2.createString() + "'");
        System.out.println("toString() = '" + da2.toString() + "'");
        System.out.println("MD5 hash  = '" + da2.generateHashString() + "'");
        System.out.println("isValid() = " + da.isValid());
        System.out.println();
        if (!da2.createString().equals(da.createString())) throw new RuntimeException("Recreated DynamicAddress does not match");
        if (!da2.equals(da)) throw new RuntimeException("Recreated DynamicAddress is not equals()");
        if (!da2.isValid()) throw new RuntimeException("Recreated DynamicAddress is not valid");
        da2.m_expirationDate = new Date();
        if (da2.isValid()) {
            System.out.println("****ERROR*****\nCheck for tampering did NOT detect deliberate tampering.");
            System.out.println("Note that if you did not specify a key, then no checksum");
            System.out.println("is appended, so there is no way to detect tampering.");
        } else System.out.println("Check for tampering detection passed.");
        System.out.println(Character.MAX_RADIX);
    }
}
