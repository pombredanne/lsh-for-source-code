package org.mineground.player;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mineground.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods which deal with password security. This includes
 * generating salts for hashes, but also computing salted hashes.
 */
public class PasswordHandler {

    private static final Logger ExceptionLogger = LoggerFactory.getLogger(PasswordHandler.class);

    private static final SecureRandom RANDOM_GENERATOR = new SecureRandom();

    /**
     * Creates a random salt for us. This will be used to salt encrypted or
     * hashed things, such as passwords.
     *
     * @param length The length of the salt we want.
     * @return A random string with the given length.
     * @throws NoSuchAlgorithmException
     */
    public static String createSalt(int length) throws NoSuchAlgorithmException {
        byte[] bytes = new byte[40];
        RANDOM_GENERATOR.nextBytes(bytes);
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        sha1.reset();
        byte[] digest = sha1.digest(bytes);
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest)).substring(0, length);
    }

    /**
     * Returns the hashed password with the given salt.
     *
     * @param password The password to hash.
     * @param salt The salt to use.
     * @return The hashed and salted password.
     * @throws NoSuchAlgorithmException
     */
    public static String getPasswordHash(String password, String salt) throws NoSuchAlgorithmException {
        return getSha256(getSha256(password) + salt);
    }

    /**
     * Creates an unsalted hash for the given input with the given algorithm.
     *
     * @param algorithm The algorithm to use, see {@link MessageDigest.getInstance()}.
     * @param input The string to hash.
     * @return The hashed input.
     * @throws NoSuchAlgorithmException
     */
    private static String getUnsaltedHash(String algorithm, String input) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        messageDigest.reset();
        messageDigest.update(input.getBytes(Main.DEFAULT_CHARSET));
        byte[] digest = messageDigest.digest();
        return String.format(Main.DEFAULT_LOCALE, "%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }

    /**
     * Convenience method to hash with the SHA-256 algorithm.
     *
     * @param message The message to hash.
     * @return The hashed message.
     * @throws NoSuchAlgorithmException
     */
    private static String getSha256(String message) throws NoSuchAlgorithmException {
        return getUnsaltedHash("SHA-256", message);
    }

    /**
     * Re-sets the player password
     * 
     * @param playerName The player's ingame name
     * @param oldPassword The password the player used before
     * @param newPassword The new password to set
     * @return true on success, false otherwise
     */
    public static boolean changePassword(String playerName, String oldPassword, String newPassword) {
        try {
            PreparedStatement queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT player_id, salt FROM lvm_players WHERE login_name = ?");
            queryStatement.setString(1, playerName);
            queryStatement.execute();
            ResultSet queryResult = queryStatement.getResultSet();
            if (!queryResult.next()) {
                return false;
            }
            int profileId = queryResult.getInt(1);
            String passwordSalt = queryResult.getString(2);
            queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT password FROM lvm_player_settings WHERE player_id = ?");
            queryStatement.setInt(1, profileId);
            queryStatement.execute();
            queryResult = queryStatement.getResultSet();
            if (!queryResult.next()) {
                return false;
            }
            if (!getPasswordHash(oldPassword, passwordSalt).equals(queryResult.getString(1))) {
                return false;
            }
            String newPasswordHash = getPasswordHash(newPassword, passwordSalt);
            queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("UPDATE lvm_player_settings SET password = ? WHERE player_id = ?");
            queryStatement.setString(1, newPasswordHash);
            queryStatement.setInt(2, profileId);
            queryStatement.execute();
            return true;
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
        return false;
    }

    public static boolean forceChangePassword(String playerName, String newPassword) {
        try {
            PreparedStatement queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("SELECT player_id, salt FROM lvm_players WHERE login_name = ?");
            queryStatement.setString(1, playerName);
            queryStatement.execute();
            ResultSet queryResult = queryStatement.getResultSet();
            if (!queryResult.next()) {
                return false;
            }
            int profileId = queryResult.getInt(1);
            String passwordSalt = queryResult.getString(2);
            String newPasswordHash = getPasswordHash(newPassword, passwordSalt);
            queryStatement = Main.getInstance().getDatabaseHandler().getConnection().prepareStatement("UPDATE lvm_player_settings SET password = ? WHERE player_id = ?");
            queryStatement.setString(1, newPasswordHash);
            queryStatement.setInt(2, profileId);
            queryStatement.execute();
            return true;
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
        return false;
    }
}
