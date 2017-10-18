package org.webdocwf.util.smime.cms;

import org.webdocwf.util.smime.exception.SMIMEException;
import org.webdocwf.util.smime.exception.ErrorStorage;
import java.security.MessageDigest;
import org.webdocwf.util.smime.der.DERSet;
import org.webdocwf.util.smime.der.DEROctetString;

/**
 * MessageDigestAttribute is Signed Attribute and is used for creating CMS object
 * for signed messages. It defines the type of digest used in CMS object in
 * information about particular signing.
 */
public class MessageDigestAttribute extends Attribute {

    /**
     * Performes construction and digesting of message in the same time
     * @param message0 input massage for digest algorithm
     * @param digestAlg0 object identifier name for digest algorithm (for
     * example "SHA1")
     * @exception SMIMEException caused by non SMIME exception which is
     * NoSuchAlgorithmException if invalid digestAlg0 parameter is passed to the
     * constructor. Also, exception could be thrown by super class constructor or
     * by super class addContent method.
     */
    public MessageDigestAttribute(byte[] message0, String digestAlg0) throws SMIMEException {
        super("ID_MESSAGEDIGEST", "NAME_STRING");
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(digestAlg0);
        } catch (Exception e) {
            throw SMIMEException.getInstance(this, e, "constructor");
        }
        md.update(message0);
        DEROctetString dig = new DEROctetString(md.digest());
        DERSet digValue = new DERSet();
        digValue.addContent(dig.getDEREncoded());
        super.addContent(digValue.getDEREncoded());
    }
}
