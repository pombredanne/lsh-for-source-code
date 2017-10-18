package com.integrationpath.mengine.webapp.action;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.integrationpath.mengine.model.AppUser;
import com.integrationpath.mengine.service.GenericManager;
import com.integrationpath.mengine.webapp.util.RequestUtil;
import com.opensymphony.xwork2.Preparable;

public class ConfirmAccountAction extends BaseAction implements Preparable {

    private List users;

    private String hash;

    private GenericManager<AppUser, Long> appUserManager;

    public GenericManager<AppUser, Long> getAppUserManager() {
        return appUserManager;
    }

    public void setAppUserManager(GenericManager<AppUser, Long> appUserManager) {
        this.appUserManager = appUserManager;
    }

    public String confirmAccount() {
        users = appUserManager.getAll();
        for (Iterator iterator = users.iterator(); iterator.hasNext(); ) {
            AppUser type = (AppUser) iterator.next();
            String userHash = type.getPassword();
            log.info(hash + "|" + userHash);
            if (hash.equals(userHash)) {
                log.info("User is ok to be eanbled with hash: " + hash);
                type.setAccountEnabled(true);
                appUserManager.save(type);
            }
        }
        saveMessage(getText("account.confirmed"));
        getRequest().getSession().setAttribute("confirmed", "Confirmed! ~ ");
        return SUCCESS;
    }

    public void prepare() throws Exception {
    }

    public List getUsers() {
        return users;
    }

    public void setUsers(List users) {
        this.users = users;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    WebApplicationContext getSpringContext() {
        return WebApplicationContextUtils.getWebApplicationContext(getRequest().getSession().getServletContext());
    }

    public byte[] generatePassword(String clave) {
        byte[] password = { 00 };
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(clave.getBytes());
            password = md5.digest();
            return password;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return password;
    }
}
