package net.sf.catchup.common.credentials;

import net.sf.catchup.common.crypto.CryptoHelper;
import net.sf.catchup.exception.CatchupException;
import net.sf.catchup.server.auth.SecureDigestAuthenticator;

/**
 * This implementation of {@link Credentials} doesn't store the password as
 * clear text. Instead it stores the <b>SHA</b> digest of the password. This
 * method of sending credentials is significantly more secure since the password
 * itself will not be sent over the network
 * 
 * Note that this type of credential is supported by
 * {@link SecureDigestAuthenticator} only
 * 
 * @author Ramachandra
 */
public class DigestedCredentials implements Credentials {

    private String username;

    private transient String password;

    private byte[] pwdChecksum;

    public String getUsername() {
        return username;
    }

    /**
	 * @return The SHA digest form of the password
	 */
    public byte[] getPasswdChecksum() {
        return pwdChecksum;
    }

    public void setPassword(String password) {
        this.password = password;
        pwdChecksum = CryptoHelper.digest(password);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void validate() throws CatchupException {
        if (username.equals("") || password.equals("") || !username.matches("[a-zA-Z0-9\\._]+")) {
            throw new CatchupException("Credentials validation failed");
        }
    }
}
