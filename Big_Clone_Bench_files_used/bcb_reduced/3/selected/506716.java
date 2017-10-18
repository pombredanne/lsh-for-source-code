package rtm4java.impl.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SignerService {

    private static final String UTF_8 = "UTF-8";

    private MessageDigest _digest;

    private String _secret;

    public SignerService() {
        try {
            _digest = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceUnavailableException(e);
        }
    }

    public String sign(Map<String, String> attributes) throws UnsupportedEncodingException {
        _digest.reset();
        _digest.update(_secret.getBytes(UTF_8));
        List<String> flatAttributes = new ArrayList<String>(attributes.size());
        for (Iterator<Entry<String, String>> i = attributes.entrySet().iterator(); i.hasNext(); ) {
            Entry<String, String> entry = i.next();
            flatAttributes.add(entry.getKey() + entry.getValue());
        }
        Collections.sort(flatAttributes);
        for (String param : flatAttributes) _digest.update(param.getBytes(UTF_8));
        return convertToHex(_digest.digest());
    }

    private static String convertToHex(byte[] data) {
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

    public void setSecret(String secret) {
        _secret = secret;
    }
}
