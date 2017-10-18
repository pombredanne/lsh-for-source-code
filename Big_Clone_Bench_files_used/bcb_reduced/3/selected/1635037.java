package org.nodevision.portal.wss4j;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.ws.security.WSPasswordCallback;
import org.hibernate.Session;
import org.nodevision.portal.hibernate.om.NvUserRoles;
import org.nodevision.portal.hibernate.om.NvUsers;
import org.nodevision.portal.utils.HibernateUtil;

public class WSS4JCallbackHandler implements CallbackHandler {

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) {
                try {
                    WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                    Session hbsession = HibernateUtil.currentSession();
                    NvUsers user = (NvUsers) hbsession.load(NvUsers.class, pc.getIdentifer());
                    Iterator roles = user.getSetOfNvUserRoles().iterator();
                    ArrayList rolesList = new ArrayList();
                    while (roles.hasNext()) {
                        NvUserRoles element = (NvUserRoles) roles.next();
                        rolesList.add(element.getId().getNvRoles().getRoleName());
                    }
                    pc.setPassword(user.getPassword());
                    if (!rolesList.contains("admins")) {
                        throw new UnsupportedCallbackException(callbacks[i], "User is not in group 'admins'.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IOException(e.toString());
                }
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }

    private final String createMD5(String pwd) throws Exception {
        MessageDigest md = (MessageDigest) MessageDigest.getInstance("MD5").clone();
        md.update(pwd.getBytes("UTF-8"));
        byte[] pd = md.digest();
        StringBuffer app = new StringBuffer();
        for (int i = 0; i < pd.length; i++) {
            String s2 = Integer.toHexString(pd[i] & 0xFF);
            app.append((s2.length() == 1) ? "0" + s2 : s2);
        }
        return app.toString();
    }
}
