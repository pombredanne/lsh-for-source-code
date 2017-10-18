package org.jsmtpd.plugins.smtpExtension;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.jsmtpd.core.common.PluginInitException;
import org.jsmtpd.tools.Base64Helper;
import org.jsmtpd.tools.ByteArrayTool;

/**
 * @author Jean-Francois POUX
 */
public class BasicSmtpAuth extends SmtpAuthenticator {

    private Map<String, byte[]> users = new HashMap<String, byte[]>();

    private MessageDigest md;

    protected boolean performAuth(String login, byte[] password) {
        if (!users.containsKey(login)) return false;
        byte[] hash = (byte[]) users.get(login);
        return ByteArrayTool.compare(hash, md.digest(password));
    }

    private void addPlainUser(String user, String password) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        users.put(user, md5.digest(password.getBytes()));
    }

    private void addMD5User(String user, String md5base64password) {
        users.put(user, Base64Helper.decode(md5base64password));
    }

    public void setPlainUser(String in) throws Exception {
        String[] tmp = in.split(",");
        addPlainUser(tmp[0], tmp[1]);
    }

    public void setMD5User(String in) {
        String[] tmp = in.split(",");
        addMD5User(tmp[0], tmp[1]);
    }

    @Override
    public void initPlugin() throws PluginInitException {
        try {
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new PluginInitException("md5 not available");
        }
        super.initPlugin();
    }
}
