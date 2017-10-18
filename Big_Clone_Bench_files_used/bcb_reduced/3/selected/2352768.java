package gphoto.services.impl;

import gphoto.services.ConnexionServices;
import gphoto.services.OptionServices;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

public class ConnexionServicesImpl implements ConnexionServices {

    private static ConnexionServicesImpl cs = null;

    private static final Logger logger = Logger.getLogger(ConnexionServicesImpl.class);

    private ConnexionServicesImpl() {
    }

    public static ConnexionServicesImpl getInstance() {
        if (cs == null) {
            cs = new ConnexionServicesImpl();
        }
        return cs;
    }

    public boolean isValidPassword(String password) throws Exception {
        OptionServices os = OptionServicesImpl.getInstance();
        String encodedPassBD = os.getPasswordMD5();
        String encodedPass = getEncodedPassword(password);
        logger.debug("MD5 : " + password + " --> '" + encodedPass + "'");
        if (encodedPass.equals(encodedPassBD)) {
            return true;
        }
        return false;
    }

    private String getEncodedPassword(String key) throws Exception {
        byte[] uniqueKey = key.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            logger.error("La JVM ne supporte pas de MD5");
            throw e;
        }
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }
}
