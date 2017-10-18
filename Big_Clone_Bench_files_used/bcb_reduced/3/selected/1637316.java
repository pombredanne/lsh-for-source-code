package mx.com.nyak.base.security.cipher.service.impl;

import java.security.MessageDigest;
import mx.com.nyak.base.security.cipher.exception.CadenaNoCifradaException;
import mx.com.nyak.base.security.cipher.service.Cipher;

/** 
 * 
 * Derechos Reservados (c)Jose Carlos Perez Cervantes 2009 
 * 
 * 
 * */
public class CipherSHA1Impl implements Cipher {

    private String encoding = "iso-8859-1";

    /**
	 * @param encoding
	 *            the encoding to set
	 */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getCipherString(String source) throws CadenaNoCifradaException {
        String encryptedSource = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
            byte[] sha1hash = new byte[40];
            md.update(source.getBytes(encoding), 0, source.length());
            sha1hash = md.digest();
            encryptedSource = convertToHex(sha1hash);
        } catch (Exception e) {
            throw new CadenaNoCifradaException(e);
        }
        return encryptedSource;
    }

    private String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
