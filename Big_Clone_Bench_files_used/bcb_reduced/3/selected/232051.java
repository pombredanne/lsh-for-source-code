package org.cofax.cms.login;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import javax.servlet.ServletConfig;
import org.cofax.DataStore;
import org.cofax.cms.CofaxToolsDbUtils;
import org.cofax.cms.CofaxToolsUtil;

/**
 * @author Nicolas Richeton (Smile)
 */
public class CofaxDbLogin implements ILoginHandler {

    public void init(ServletConfig servletConfig) {
    }

    public HashMap getUserHash(DataStore db, String login, String password) {
        HashMap fillReq = new HashMap();
        fillReq.put("login", login);
        String tag = "";
        String md5Password = md5Password(login, password);
        tag = CofaxToolsDbUtils.fillTag(db, "getUserInfoByLoginPassword");
        fillReq.put("password", md5Password);
        HashMap userInfoHash = CofaxToolsDbUtils.getNameValuePackageHash(db, fillReq, tag);
        return userInfoHash;
    }

    /**
	 * helper method for MD5 functions 
	 ***/
    private static String toHex(byte[] digest) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            buf.append(Integer.toHexString(0x0100 + (digest[i] & 0x00ff)).substring(1));
        }
        return buf.toString();
    }

    private static String md5Password(String login, String password) {
        String hash = "";
        String md5Password = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            hash = login + ":" + password;
            byte[] rawPass = hash.getBytes();
            try {
                md.update(rawPass);
            } catch (Exception e) {
                CofaxToolsUtil.log("CofaxToolsLogin login : " + e);
            }
            md5Password = toHex(md.digest());
        } catch (NoSuchAlgorithmException nsae) {
            CofaxToolsUtil.log("CofaxToolsLogin login : " + nsae);
        }
        return md5Password;
    }
}
