package org.progeeks.extract.filter;

import java.math.BigInteger;
import java.security.*;
import org.progeeks.util.log.*;
import org.progeeks.extract.*;

/**
 *  Uses a MessageDigest to create a one-way hash from the
 *  specified source data converted to a string.
 *
 *  @version   $Revision: 3806 $
 *  @author    Paul Speed
 */
public class DigestFilter extends AbstractFilter {

    static Log log = Log.getLog();

    private String digestType = "MD5";

    public DigestFilter() {
    }

    public void setDigestType(String s) {
        this.digestType = s;
    }

    public String getDigestType() {
        return digestType;
    }

    public Object filter(ExecutionContext context, DataElement container, Object o) {
        String s = String.valueOf(o);
        try {
            MessageDigest digest = MessageDigest.getInstance(digestType);
            byte[] result = digest.digest(s.getBytes("UTF-8"));
            BigInteger bi = new BigInteger(1, result);
            String val = bi.toString(16);
            while (val.length() < result.length * 2) val = "0" + val;
            return val;
        } catch (Exception e) {
            throw new RuntimeException("Error creating digest", e);
        }
    }
}
