package net.sourceforge.epoint;

import java.security.*;
import net.sourceforge.epoint.util.*;

/**
 * ePoint bills are represented by this class.
 * 
 * @version 0.3
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 * @see EPointCertificate
 */
public class EPoint {

    private byte[] code;

    /**
     * new ePoint bill from an existing one
     * @param e ePoint bill to copy
     */
    public EPoint(EPoint e) {
        code = e.code;
    }

    /**
     * new ePoint bill from a <code>byte</code> buffer
     * @param c buffer containing the binary sequence
     */
    public EPoint(byte[] c) {
        setCode(c);
    }

    /**
     * new ePoint bill from a <code>byte</code> buffer
     * @param c buffer containing the binary sequence
     * @param offset of the sequence
     * @param length of the sequence
     */
    public EPoint(byte[] c, int offset, int length) {
        setCode(c, offset, length);
    }

    /**
     * new ePoint bill from a <code>String</code>
     * @param c base64 representation of the bill
     */
    public EPoint(String c) throws java.io.IOException {
        setCode(c);
    }

    /**
     * set code from a <code>byte</code> buffer
     * @param c buffer containing the binary sequence
     */
    public void setCode(byte[] c) throws NullPointerException, ArrayStoreException, IndexOutOfBoundsException {
        setCode(c, 0, c.length);
    }

    /**
     * set code from a <code>byte</code> buffer
     * @param c buffer containing the binary sequence
     * @param offset of the sequence
     * @param length of the sequence
     */
    public void setCode(byte[] c, int offset, int length) throws NullPointerException, ArrayStoreException, IndexOutOfBoundsException {
        byte[] b = new byte[length];
        System.arraycopy(c, offset, b, 0, length);
        code = b;
    }

    /**
     * set the code of the bill from a <code>String</code>
     * @param c base64 representation of the bill
     */
    public void setCode(String c) throws java.io.IOException {
        code = Base64.decode(c);
    }

    /**
     * get the MD of the bill
     * @return binary representation of the SHA1 digest
     */
    public byte[] getMD() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA");
        return md.digest(code);
    }

    /**
     * get the human-readable MD of the bill
     * @return tabulated hexadecimal representation of the SHA1 digest
     */
    public String getMDText() throws NoSuchAlgorithmException {
        return Splitter.group(Base16.encode(getMD()), 4, 5);
    }

    /**
     * get the code of the bill
     * @return <code>byte</code> buffer with the binary sequence
     */
    public byte[] getCode() {
        byte b[] = new byte[code.length];
        try {
            System.arraycopy(code, 0, b, 0, code.length);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * get the code of the bill
     * @param b target buffer
     * @param offset
     */
    public void getCode(byte[] b, int offset) throws NullPointerException, ArrayStoreException, IndexOutOfBoundsException {
        System.arraycopy(code, 0, b, offset, code.length);
    }

    /**
     * just in case
     */
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * compare for equality
     */
    public boolean equals(Object x) {
        if (getClass().isInstance(x)) return toString().equals(x.toString());
        return false;
    }

    /**
     * get the base64 code of the bill
     * @return canonical base64 representation of the ePoint bill
     */
    public String toString() {
        return Base64.encode(code);
    }
}
