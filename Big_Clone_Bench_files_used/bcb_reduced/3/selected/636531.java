package com.timk.goserver.server.model;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import com.timk.goserver.client.model.ClientUserInfo;
import com.timk.goserver.server.util.HibernateUtil;
import com.timk.goserver.server.util.RatingManager;

/**
 * User object
 * @author TKington
 *
 */
public class ServerUserInfo {

    private int userId;

    private String username;

    private String password;

    private String fullname;

    private String email;

    private String rank;

    private String salt;

    private int eloRating;

    private int notification;

    private List ratingHistory;

    private int imageSize = 21;

    /**
	 * Creates a User
	 *
	 */
    public ServerUserInfo() {
        ratingHistory = new ArrayList();
    }

    /**
	 * Creates a User
	 * @param username the username
	 * @param password the password
	 * @param fullname the full name
	 * @param email the email address
	 * @param eloRating the rating
	 * @param notification email notification setting
	 * @throws NoSuchAlgorithmException if the hashing algorithm is not found
	 * @throws IOException if the base64 conversion fails
	 */
    public ServerUserInfo(String username, String password, String fullname, String email, int eloRating, int notification) throws NoSuchAlgorithmException, IOException {
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.eloRating = eloRating;
        this.rank = RatingManager.ratingToString(eloRating);
        this.notification = notification;
        ratingHistory = new ArrayList();
        ratingHistory.add(new RatingEntry(eloRating));
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        byte[] bSalt = new byte[8];
        random.nextBytes(bSalt);
        this.salt = byteToBase64(bSalt);
        updatePassword(password);
    }

    /**
	 * Changes the password for this user
	 * @param newPassword the new password
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
    public final void updatePassword(String newPassword) throws NoSuchAlgorithmException, IOException {
        this.password = hashPassword(newPassword, base64ToByte(salt));
    }

    private String hashPassword(String pwd, byte[] bSalt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(bSalt);
        byte[] hash = digest.digest(pwd.getBytes());
        return byteToBase64(hash);
    }

    private String byteToBase64(byte[] data) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }

    private byte[] base64ToByte(String data) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(data);
    }

    /**
	 * Checks a password against the hashed password in this object
	 * @param pwd the password
	 * @return true if the password is correct
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
    public boolean checkPassword(String pwd) throws NoSuchAlgorithmException, IOException {
        byte[] bSalt = base64ToByte(salt);
        return password.equals(hashPassword(pwd, bSalt));
    }

    /**
	 * Creates a ClientUserInfo from this User
	 * @return the ClientUserInfo
	 */
    public ClientUserInfo toClientUserInfo() {
        ClientUserInfo info = new ClientUserInfo();
        info.setUsername(username);
        info.setFullname(fullname);
        info.setEmail(email);
        info.setRank(rank);
        info.setNotification(notification);
        info.setImageSize(imageSize);
        return info;
    }

    /**
	 * Updates rating and rank string
	 * @param newRating the new rating
	 */
    public void updateRating(int newRating) {
        eloRating = newRating;
        rank = RatingManager.ratingToString(eloRating);
        ratingHistory.add(new RatingEntry(eloRating));
    }

    /**
	 * Finds a user in the db
	 * @param username the username
	 * @param activeSession the active session, may be null
	 * @return the User
	 */
    public static ServerUserInfo findUser(String username, Session activeSession) {
        Session session;
        boolean newTransaction;
        if (activeSession == null) {
            session = HibernateUtil.startTrans();
            newTransaction = true;
        } else {
            session = activeSession;
            newTransaction = false;
        }
        ServerUserInfo user = (ServerUserInfo) session.createQuery("from ServerUserInfo as user where user.username = ?").setString(0, username).uniqueResult();
        if (user != null && !username.equals(user.getUsername())) {
            user = null;
        }
        if (newTransaction) {
            session.getTransaction().commit();
        }
        return user;
    }

    /**
	 * Returns the id
	 * @return the id
	 */
    public int getUserId() {
        return userId;
    }

    /**
	 * Sets the id
	 * @param userId the id
	 */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
	 * Returns the password
	 * @return the password
	 */
    public String getPassword() {
        return password;
    }

    /**
	 * Sets the password
	 * @param password the password
	 */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
	 * Returns the username
	 * @return the username
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * Sets the username
	 * @param username the username
	 */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
	 * Returns the salt
	 * @return the salt
	 */
    public String getSalt() {
        return salt;
    }

    /**
	 * Sets the salt
	 * @param salt
	 */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
	 * Returns the Elo rating
	 * @return the Elo rating
	 */
    public int getEloRating() {
        return eloRating;
    }

    /**
	 * Sets the Elo rating
	 * @param eloRating
	 */
    public void setEloRating(int eloRating) {
        this.eloRating = eloRating;
    }

    /**
	 * Returns the email address
	 * @return the email address
	 */
    public String getEmail() {
        return email;
    }

    /**
	 * Sets the email address
	 * @param email
	 */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
	 * Returns the full name
	 * @return the full name
	 */
    public String getFullname() {
        return fullname;
    }

    /**
	 * Sets the full name
	 * @param fullname
	 */
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    /**
	 * Returns the rank
	 * @return the rank
	 */
    public String getRank() {
        return rank;
    }

    /**
	 * Sets the rank
	 * @param rating
	 */
    public void setRank(String rating) {
        this.rank = rating;
    }

    /**
	 * Returns email notification setting
	 * @return email notification setting
	 */
    public int getNotification() {
        return notification;
    }

    /**
	 * Sets email notification setting
	 * @param notification
	 */
    public void setNotification(int notification) {
        this.notification = notification;
    }

    /**
	 * Returns the rating history
	 * @return List&ltRatingEntry&gt;
	 */
    public List getRatingHistory() {
        return ratingHistory;
    }

    /**
	 * Sets the rating history
	 * @param ratingHistory List&lt;RatingEntry&gt;
	 */
    public void setRatingHistory(List ratingHistory) {
        this.ratingHistory = ratingHistory;
    }

    /**
	 * Returns size of board images
	 * @return size of board images
	 */
    public int getImageSize() {
        return imageSize;
    }

    /**
	 * Sets size of board images
	 * @param imageSize
	 */
    public void setImageSize(int imageSize) {
        this.imageSize = imageSize;
    }
}
