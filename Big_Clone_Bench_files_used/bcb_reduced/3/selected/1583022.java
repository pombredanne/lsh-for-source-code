package org.apache.ws.security.message.token;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Kerberos Security extends the Binary Security class to provide
 * additional properties required to process a kerberos binary security token.
 * 
 * @author Anthony Bull (antsbull@gmail.com)
 */
public class KerberosSecurity extends BinarySecurity {

    private static final String type = WSConstants.KERBEROS_NS + "#" + WSConstants.KERBEROS_LN;

    /**
     * This constructor creates a new Kerberos security token object and 
     * initializes it from the data contained in the element.
     *
     * @param elem the element containing the Kerberos token data
     * @throws WSSecurityException
     */
    public KerberosSecurity(Element elem) throws WSSecurityException {
        super(elem);
        if (!getValueType().equals(type)) {
            throw new WSSecurityException(WSSecurityException.INVALID_SECURITY_TOKEN, "invalidValueType", new Object[] { type, getValueType() });
        }
    }

    /**
     * This constructor creates a new Kerberos security token element.
     *
     * @param doc
     */
    public KerberosSecurity(Document doc) {
        super(doc);
        setValueType(type);
    }

    public void setKerberosToken(byte[] tokenData) {
        setToken(tokenData);
    }

    public static String getType() {
        return type;
    }

    /**
     * Get the SHA-1 thumbprint of the security token. The thumbprint is base 64
     * encoded.
     * @return the base 64 encoded, SHA-1 thumbprint
     * @throws java.security.NoSuchAlgorithmException if the hash algorithm cannot
     * be found
     */
    public String getThumbprint() throws NoSuchAlgorithmException {
        byte[] token = getToken();
        MessageDigest digest = MessageDigest.getInstance("SHA");
        digest.update(token);
        byte[] thumbPrintBytes = digest.digest();
        return Base64.encode(thumbPrintBytes);
    }
}
