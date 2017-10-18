package oxygen.manager;

import java.io.File;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import oxygen.util.StringUtils;
import sun.misc.BASE64Encoder;

public class FSUserPasswordManager implements UserPasswordManager {

    private File file;

    private Map mapping = new HashMap();

    private long lastmod = -1;

    private String encoding = null;

    private String algorithm = null;

    private boolean doEncrypt = false;

    private MessageDigest md = null;

    public void init(Properties p) throws Exception {
        String s = null;
        s = p.getProperty(PROPS_PREFIX + "password.digest.encoding");
        encoding = (StringUtils.isBlank(s)) ? encoding : s;
        s = p.getProperty(PROPS_PREFIX + "password.digest.algorithm");
        algorithm = (StringUtils.isBlank(s)) ? algorithm : s;
        doEncrypt = !(StringUtils.isBlank(algorithm));
        if (doEncrypt) {
            md = (MessageDigest) MessageDigest.getInstance(algorithm).clone();
        }
        s = p.getProperty(PROPS_PREFIX + "password.file");
        s = StringUtils.replacePropertyReferencesInString(s, p);
        file = new File(s);
        sync();
    }

    public void setPassword(String username, char[] passwd) throws Exception {
        String encPasswd = encrypt(new String(passwd));
        mapping.put(username, encPasswd);
    }

    public boolean checkPassword(String username, char[] passwd) throws Exception {
        boolean b = false;
        String savedEncPasswd = (String) mapping.get(username);
        if (savedEncPasswd != null) {
            String encPasswd = encrypt(new String(passwd));
            b = savedEncPasswd.equals(encPasswd);
        }
        return b;
    }

    public char[] getEncryptedPassword(String username) throws Exception {
        return ((String) mapping.get(username)).toCharArray();
    }

    public void save(Properties metadata) throws Exception {
        ManagerUtils.saveMapForManagers(mapping, file, null, ':');
    }

    public void close() {
    }

    public void sync() throws Exception {
        long lastmod2 = file.lastModified();
        if (lastmod2 > lastmod) {
            mapping.clear();
            ManagerUtils.loadMapForManagers(mapping, file, null);
            lastmod = lastmod2;
        }
    }

    public String encrypt(String s) throws Exception {
        String s2 = s;
        if (doEncrypt) {
            synchronized (md) {
                md.update(s.getBytes(encoding));
                s2 = (new BASE64Encoder()).encode(md.digest());
                md.reset();
            }
        }
        return s2;
    }

    private static String encrypt(String s, String alg, String enc) throws Exception {
        MessageDigest md = (MessageDigest) MessageDigest.getInstance(alg).clone();
        md.update(s.getBytes(enc));
        return (new BASE64Encoder()).encode(md.digest());
    }

    public static void main(String[] args) throws Exception {
        System.out.println(encrypt("ugorji", "MD5", "UTF-8"));
    }
}
