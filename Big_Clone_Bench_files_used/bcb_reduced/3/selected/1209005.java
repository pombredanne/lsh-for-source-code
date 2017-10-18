package edu.vt.middleware.ldap.auth.handler;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import edu.vt.middleware.ldap.LdapConfig;
import edu.vt.middleware.ldap.LdapUtil;
import edu.vt.middleware.ldap.auth.AuthenticatorConfig;
import edu.vt.middleware.ldap.handler.ConnectionHandler;

/**
 * <code>CompareAuthenticationHandler</code> provides an LDAP authentication
 * implementation that leverages a compare operation against the userPassword
 * attribute. The default password scheme used is 'SHA'.
 *
 * @author  Middleware Services
 * @version  $Revision: 1330 $
 */
public class CompareAuthenticationHandler extends AbstractAuthenticationHandler {

    /** Maximum digest size. Value is {@value}. */
    private static final int DIGEST_SIZE = 256;

    /** Password scheme. Default value is {@value}. */
    private String passwordScheme = "SHA";

    /** Default constructor. */
    public CompareAuthenticationHandler() {
    }

    /**
   * Creates a new <code>CompareAuthenticationHandler</code> with the supplied
   * authenticator config.
   *
   * @param  ac  authenticator config
   */
    public CompareAuthenticationHandler(final AuthenticatorConfig ac) {
        this.setAuthenticatorConfig(ac);
    }

    /**
   * Returns the password scheme.
   *
   * @return  password scheme
   */
    public String getPasswordScheme() {
        return this.passwordScheme;
    }

    /**
   * Sets the password scheme. Must equal a known message digest algorithm.
   *
   * @param  s  password scheme
   */
    public void setPasswordScheme(final String s) {
        this.passwordScheme = s;
    }

    /** {@inheritDoc} */
    public void authenticate(final ConnectionHandler ch, final AuthenticationCriteria ac) throws NamingException {
        byte[] hash = new byte[DIGEST_SIZE];
        try {
            final MessageDigest md = MessageDigest.getInstance(this.passwordScheme);
            md.update(((String) ac.getCredential()).getBytes());
            hash = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new NamingException(e.getMessage());
        }
        ch.connect(this.config.getBindDn(), this.config.getBindCredential());
        NamingEnumeration<SearchResult> en = null;
        try {
            en = ch.getLdapContext().search(ac.getDn(), "userPassword={0}", new Object[] { String.format("{%s}%s", this.passwordScheme, LdapUtil.base64Encode(hash)).getBytes() }, LdapConfig.getCompareSearchControls());
            if (!en.hasMore()) {
                throw new AuthenticationException("Compare authentication failed.");
            }
        } finally {
            if (en != null) {
                en.close();
            }
        }
    }

    /** {@inheritDoc} */
    public CompareAuthenticationHandler newInstance() {
        return new CompareAuthenticationHandler(this.config);
    }
}
