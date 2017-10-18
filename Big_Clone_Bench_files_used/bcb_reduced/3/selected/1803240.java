package org.apache.catalina.realm;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.catalina.util.StringManager;

/**
 * <p>Implementation of the JAAS <code>CallbackHandler</code> interface,
 * used to negotiate delivery of the username and credentials that were
 * specified to our constructor.  No interaction with the user is required
 * (or possible).</p>
 *
 * <p>This <code>CallbackHandler</code> will pre-digest the supplied
 * password, if required by the <code>&lt;Realm&gt;</code> element in 
 * <code>server.xml</code>.</p>
 * <p>At present, <code>JAASCallbackHandler</code> knows how to handle callbacks of
 * type <code>javax.security.auth.callback.NameCallback</code> and
 * <code>javax.security.auth.callback.PasswordCallback</code>.</p>
 *
 * @author Craig R. McClanahan
 * @author Andrew R. Jaquith
 * @version $Revision: 543691 $ $Date: 2007-06-02 03:37:08 +0200 (Sat, 02 Jun 2007) $
 */
public class JAASCallbackHandler implements CallbackHandler {

    /**
     * Construct a callback handler configured with the specified values.
     * Note that if the <code>JAASRealm</code> instance specifies digested passwords,
     * the <code>password</code> parameter will be pre-digested here.
     *
     * @param realm Our associated JAASRealm instance
     * @param username Username to be authenticated with
     * @param password Password to be authenticated with
     */
    public JAASCallbackHandler(JAASRealm realm, String username, String password) {
        super();
        this.realm = realm;
        this.username = username;
        if (realm.hasMessageDigest()) {
            this.password = realm.digest(password);
        } else {
            this.password = password;
        }
    }

    /**
     * The string manager for this package.
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * The password to be authenticated with.
     */
    protected String password = null;

    /**
     * The associated <code>JAASRealm</code> instance.
     */
    protected JAASRealm realm = null;

    /**
     * The username to be authenticated with.
     */
    protected String username = null;

    /**
     * Retrieve the information requested in the provided <code>Callbacks</code>.
     * This implementation only recognizes <code>NameCallback</code> and
     * <code>PasswordCallback</code> instances.
     *
     * @param callbacks The set of <code>Callback</code>s to be processed
     *
     * @exception IOException if an input/output error occurs
     * @exception UnsupportedCallbackException if the login method requests
     *  an unsupported callback type
     */
    public void handle(Callback callbacks[]) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                if (realm.getContainer().getLogger().isTraceEnabled()) realm.getContainer().getLogger().trace(sm.getString("jaasCallback.username", username));
                ((NameCallback) callbacks[i]).setName(username);
            } else if (callbacks[i] instanceof PasswordCallback) {
                final char[] passwordcontents;
                if (password != null) {
                    passwordcontents = password.toCharArray();
                } else {
                    passwordcontents = new char[0];
                }
                ((PasswordCallback) callbacks[i]).setPassword(passwordcontents);
            } else {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }
    }
}
