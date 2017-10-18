package com.corratech.ws.sugarcrm.util;

import java.security.MessageDigest;
import javax.xml.rpc.Stub;
import com.corratech.ws.sugarcrm.Set_entry_result;
import com.corratech.ws.sugarcrm.Sugarsoap;
import com.corratech.ws.sugarcrm.SugarsoapPortType;
import com.corratech.ws.sugarcrm.Sugarsoap_Impl;
import com.corratech.ws.sugarcrm.User_auth;

@SuppressWarnings("unused")
public class BaseService {

    private static SugarsoapPortType service = null;

    protected String sessionId;

    protected static SugarsoapPortType getService() {
        if (service == null) {
            service = new Sugarsoap_Impl().getSugarsoapPort();
            ((Stub) service)._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, SugarUtil.loadProperties().getProperty("ENDPOINT_ADDRESS"));
        }
        return service;
    }

    protected BaseService(String login, String password) throws Exception {
        User_auth user = new User_auth();
        user.setUser_name(login);
        user.setPassword(getMD5Code(password));
        user.setVersion("0.1");
        Set_entry_result result = null;
        result = getService().login(user, "none");
        this.sessionId = result.getId();
    }

    protected String getMD5Code(String text) {
        String md5 = null;
        try {
            StringBuffer code = new StringBuffer();
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte bytes[] = text.getBytes();
            byte digest[] = messageDigest.digest(bytes);
            for (int k = 0; k < digest.length; ++k) {
                code.append(Integer.toHexString(0x0100 + (digest[k] & 0x00FF)).substring(1));
            }
            md5 = code.toString();
        } catch (Exception e) {
            System.out.println("Error convert to md5 password.");
        }
        return md5;
    }
}
