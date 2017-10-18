package database;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PasswordCol extends DatabaseCol {

    String password = null;

    /**
	 * Create new col with an inital value
	 * @param name string with name to use in database
	 * @param value string with initial value
	 */
    protected PasswordCol(String name, String value) {
        super(name);
        setValue(value);
    }

    /**
	 * Create blank col
	 * @param name string with name to use in database
	 */
    public PasswordCol(String name) {
        super(name);
    }

    /**
	 * Set an new value
	 * @param value string with the new value
	 */
    public void setValue(String value) {
        String newPass;
        if (value != null && value.trim().length() == 0) newPass = null; else newPass = SHA(value);
        if (!isEqual(newPass, password)) setChanged(true);
        password = newPass;
    }

    /**
	 * Check if the password is set
	 * @return Guess twice! ;)
	 */
    public boolean isNull() {
        return (password == null);
    }

    /**
	 * Check if a password matches the password from the database
	 * @param password string with password to check
	 * @return true if the passwords are identical, else false
	 */
    public boolean checkPassword(String password) {
        if (password == null || this.password == null) return (password == this.password); else return this.password.equals(SHA(password));
    }

    public void populateCol(ResultSet resultSet, String name) throws SQLException {
        password = resultSet.getString(name);
    }

    public void setStatement(PreparedStatement stmt, int index) throws SQLException {
        stmt.setString(index, password);
    }

    /**
	 * Get the SHA has of the supplied data
	 * @param data string with data to get hash of
	 * @return string with the has
	 */
    private String SHA(String data) {
        if (data == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return toHexString(md.digest(data.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return data;
        }
    }

    /**
	 * Get a hex-string from the byte[]
	 * @param bytes byte[] to use
	 * @return string with the hex values of each byte
	 */
    private String toHexString(byte[] bytes) {
        StringBuilder str = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i];
            if (value < 0) value += 256;
            str.append(toHexChar(value >>> 4));
            str.append(toHexChar(value & 15));
        }
        return str.toString();
    }

    /**
	 * Get the hex value for the value
	 * @param value the value
	 * @return string with the hex representation
	 */
    private String toHexChar(int value) {
        if (value == 0) return "0"; else return Integer.toHexString(value);
    }
}
