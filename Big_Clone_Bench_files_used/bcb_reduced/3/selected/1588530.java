package com.oscwave.web.api.delicious;

import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import com.oscwave.utils.StringUtilities;

/**
 * Some delicious specific helper functions.
 * @author dan
 */
public final class Utils {

    private Utils() {
    }

    /**
	 * Calculates a MD5 digest over an url.
	 * @param fetchUrl The url to fetch
	 * @return MD5 digest string
	 * @throws DeliciousException
	 */
    public static String urlToMD5(URL fetchUrl) throws DeliciousException {
        MessageDigest md5;
        byte[] dig;
        URL url;
        try {
            if ((fetchUrl.getPath() == null || fetchUrl.getPath().length() == 0) && (fetchUrl.getQuery() == null && fetchUrl.getRef() == null)) {
                String tmp = "";
                tmp += fetchUrl.getProtocol() + "://";
                tmp += fetchUrl.getAuthority() + "/";
                url = new URL(tmp);
            } else {
                url = fetchUrl;
            }
            md5 = MessageDigest.getInstance("MD5");
            dig = md5.digest(url.toString().getBytes(Charset.forName("UTF-8")));
            return StringUtilities.toHexString(dig);
        } catch (Exception e) {
            throw new DeliciousException("Calculating MD5 over url failed", e);
        }
    }
}
