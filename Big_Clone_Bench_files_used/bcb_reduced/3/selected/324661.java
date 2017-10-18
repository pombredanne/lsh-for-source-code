package org.carabiner.state;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.carabiner.util.CarabinerException;
import org.carabiner.util.DataUtils;

/**
 * Provides a convineint way to deal with hash values for bytecode comparison.
 *
 * <p> Carabiner Testing Framework</p>
 * <p>Copyright: <a href="http://www.gnu.org/licenses/gpl.html">GNU Public License</a></p>
 *
 * @author John Januskey (john.januskey@gmail.com)
 *
 */
public class Hash {

    private static final int HASH_LENGTH = 16;

    private static final String DIGEST_TYPE = "MD5";

    private byte[] m_HashCode;

    public Hash(byte[] bytes) {
        try {
            m_HashCode = MessageDigest.getInstance(DIGEST_TYPE).digest(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new CarabinerException("Error creating hash function for state", ex);
        }
    }

    public Hash(String hashString) {
        m_HashCode = DataUtils.stringToByteArray(hashString);
    }

    /**
   * Returns a copy of the byte[] representation of the Hash.
   *
   * @return byte[]
   */
    public byte[] getHashBytes() {
        byte[] bytes = new byte[m_HashCode.length];
        System.arraycopy(m_HashCode, 0, bytes, 0, m_HashCode.length);
        return bytes;
    }

    public int hashCode() {
        int code = 0;
        for (int i = 0; i < m_HashCode.length; i++) {
            code += m_HashCode[i];
        }
        return code;
    }

    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof Hash) {
            if (this.m_HashCode.length == HASH_LENGTH && ((Hash) obj).m_HashCode.length == HASH_LENGTH) {
                result = java.util.Arrays.equals(this.m_HashCode, ((Hash) obj).m_HashCode);
            }
        }
        return result;
    }

    public String toString() {
        return DataUtils.byteArrayToString(m_HashCode);
    }
}
