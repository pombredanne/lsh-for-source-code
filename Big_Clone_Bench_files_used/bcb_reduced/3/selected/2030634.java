package fb4java.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 * 
 * fb4java<br />
 * fb4java.util
 * 
 * @author Choongsan Ro
 * @version 1.0 2010. 2. 26.
 */
public class Utility {

    private static final String HASHING_METHOD = "MD5";

    private static final String DEFAULT_CHARSET = "UTF-8";

    /**
	 * Converts key-value pair to appropriate signature for Facebook.
	 * 
	 * @param keyVal
	 *            list of key-value pairs that user has supplied
	 * @param apiSecret
	 *            Facebook api secret
	 * @return MD5 hashed signature
	 */
    public static String convetToSignature(Map<String, String> keyVal, String apiSecret) {
        if (keyVal == null || apiSecret == null || keyVal.size() <= 0 || apiSecret.trim().equals("")) {
            throw new IllegalArgumentException("keyVal or api secret is not valid. Please Check it again.");
        }
        Iterator<Entry<String, String>> iterator = keyVal.entrySet().iterator();
        StringBuffer rslt = new StringBuffer();
        byte[] signature = null;
        while (iterator.hasNext()) {
            Entry<String, String> entry = iterator.next();
            rslt.append(entry.getKey());
            rslt.append("=");
            rslt.append(entry.getValue());
        }
        rslt.append(apiSecret);
        try {
            MessageDigest md5 = MessageDigest.getInstance(HASHING_METHOD);
            md5.reset();
            md5.update(rslt.toString().getBytes());
            rslt.delete(0, rslt.length());
            signature = md5.digest();
            for (int i = 0; i < signature.length; i++) {
                String hex = Integer.toHexString(0xff & signature[i]);
                if (hex.length() == 1) {
                    rslt.append('0');
                }
                rslt.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return rslt.toString();
    }

    /**
	 * 
	 * @param keyVal
	 *            list of key value pair which contains REST method's parameter
	 *            and it's value.
	 * @param protocol
	 *            connection protocol such as http, https, etc.
	 * @param server
	 *            server address <br>
	 *            e.g.<br>
	 *            Facebook:api.facebook.com, MySpace:api.myspace.com
	 * @param rscURL
	 *            resource url <br>
	 *            e.g.<br>
	 *            Facebook:/restserver.php MySpace:/v1/users/{personId}
	 * @param charset
	 *            char set to be used for this URI. If it is set to null or an
	 *            empty string, then it considers it's char set as UTF-8 as
	 *            default.
	 * @return Created URI with supplied parameters
	 * @throws URISyntaxException
	 */
    public static URI createURI(Map<String, String> keyVal, String protocol, String server, String rscURL, String signature, String charset) throws URISyntaxException {
        URI rslt = null;
        if (keyVal == null || keyVal.size() <= 0) {
            throw new IllegalArgumentException("keyVal is invalid. Please provide with proper key value.");
        }
        if (charset == null || charset.equals("")) {
            charset = DEFAULT_CHARSET;
        }
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        Set<String> keys = keyVal.keySet();
        for (String key : keys) {
            qparams.add(new BasicNameValuePair(key, keyVal.get(key)));
        }
        qparams.add(new BasicNameValuePair("sig", signature.toLowerCase()));
        rslt = URIUtils.createURI(protocol, server, -1, rscURL, URLEncodedUtils.format(qparams, charset), null);
        return rslt;
    }
}
