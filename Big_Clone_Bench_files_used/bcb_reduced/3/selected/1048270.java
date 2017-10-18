package net.sf.j2eeslashcode;

import java.util.Date;
import net.sf.j2eeslashcode.db.DatabaseException;
import net.sf.j2eeslashcode.db.UserDB;

public class User {

    private boolean updated = false;

    private int uid = 0;

    private String nickname = "";

    private String realemail = "";

    private String fakeemail = null;

    private String homepage = null;

    private String passwd = "";

    private String sig = null;

    private int seclev = 0;

    private String newpasswd = null;

    private boolean author = false;

    private Date journalLastEntryDate = null;

    public User() {
    }

    public static User load(int userId) throws DatabaseException {
        return UserDB.load(userId);
    }

    public void create() throws DatabaseException {
        UserDB.create(this);
    }

    public void store() throws DatabaseException {
        UserDB.store(this);
    }

    public void remove() throws DatabaseException {
        UserDB.remove(this);
    }

    public boolean exists() throws DatabaseException {
        return UserDB.exists(this);
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int theUid) {
        uid = theUid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String theNickname) {
        nickname = theNickname;
    }

    public String getRealEmail() {
        return realemail;
    }

    public void setRealEmail(String theRealEmail) {
        realemail = theRealEmail;
    }

    public String getFakeEmail() {
        return fakeemail;
    }

    public void setFakeEmail(String theFakeEmail) {
        fakeemail = theFakeEmail;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String theHomepage) {
        homepage = theHomepage;
    }

    public String getEncryptedPassword() {
        return passwd;
    }

    public void setEncryptedPassword(String theEncryptedPassword) {
        passwd = theEncryptedPassword;
    }

    public void setPassword(String plaintext) throws java.security.NoSuchAlgorithmException {
        StringBuffer encrypted = new StringBuffer();
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(plaintext.getBytes());
        byte[] digestArray = digest.digest();
        for (int i = 0; i < digestArray.length; i++) {
            encrypted.append(byte2hex(digestArray[i]));
        }
        setEncryptedPassword(encrypted.toString());
    }

    private static String byte2hex(byte theByte) {
        int unsignedByte = unsignedByte(theByte);
        String fourBytes = Integer.toHexString(unsignedByte);
        String twoHex = "";
        int length = fourBytes.length();
        switch(length) {
            case 0:
                twoHex = "00";
                break;
            case 1:
                twoHex = "0" + fourBytes;
                break;
            case 2:
                twoHex = fourBytes;
                break;
            default:
                twoHex = fourBytes.substring(fourBytes.length() - 2);
        }
        return twoHex;
    }

    private static int unsignedByte(byte theByte) {
        if (theByte < 0) {
            return (128 + theByte) + 128;
        }
        return theByte;
    }

    public String getSignature() {
        return sig;
    }

    public void setSignature(String theSignature) {
        sig = theSignature;
    }

    public int getSecurityLevel() {
        return seclev;
    }

    public void setSecurityLevel(int theSecurityLevel) {
        seclev = theSecurityLevel;
    }

    public String getNewPassword() {
        return newpasswd;
    }

    public void setNewPassword(String theNewPassword) {
        newpasswd = theNewPassword;
    }

    public Date getJournalLastEntryDate() {
        return journalLastEntryDate;
    }

    public void setJournalLastEntryDate(Date theDate) {
        journalLastEntryDate = theDate;
    }

    public boolean isAuthor() {
        return author;
    }

    public void isAuthor(boolean isAuthor) {
        author = isAuthor;
    }

    public boolean equals(Object theUser) {
        boolean equality = false;
        if (theUser != null) {
            User user = (User) theUser;
            if (this.getEncryptedPassword().equals(user.getEncryptedPassword())) {
                equality = true;
            }
        }
        return equality;
    }
}
