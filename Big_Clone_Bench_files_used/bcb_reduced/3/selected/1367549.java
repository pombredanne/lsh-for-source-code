package edu.fudan.software.CWFE.user.impl;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import com.fs.model.common.persistence.BaseModelObject;
import edu.fudan.software.CWFE.user.IUserGroup;
import edu.fudan.software.CWFE.user.IUser;

@Entity
public class User extends BaseModelObject implements IUser {

    @Basic
    String userID;

    String passwdMD5;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, targetEntity = UserGroup.class)
    @JoinColumn(name = "ORG_ID")
    IUserGroup userGroup;

    public User() {
    }

    public User(String userId, String passwd, IUserGroup userGroup) {
        this.userID = userId;
        this.passwdMD5 = digestMD5(passwd);
        this.userGroup = userGroup;
    }

    @Override
    public IUserGroup getUserGroup() {
        return userGroup;
    }

    public void setOrganization(IUserGroup userGroup) {
        this.userGroup = userGroup;
    }

    @Override
    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    @Override
    public String getObjectDescription() {
        return userID;
    }

    @Override
    public String getPasswdMD5() {
        return passwdMD5;
    }

    public static String digestMD5(String sourceStr) {
        String s = null;
        byte[] source = sourceStr.getBytes();
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(source);
            byte tmp[] = md.digest();
            char str[] = new char[16 * 2];
            int k = 0;
            for (int i = 0; i < 16; i++) {
                byte byte0 = tmp[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            s = new String(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    @Override
    public boolean changePasswd(String oldPasswd, String newPasswd) {
        if (passwdMD5.equals(digestMD5(oldPasswd))) {
            passwdMD5 = digestMD5(newPasswd);
            return true;
        } else return false;
    }
}
