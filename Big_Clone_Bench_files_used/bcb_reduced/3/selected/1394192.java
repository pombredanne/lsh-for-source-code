package edu.vt.middleware.ldap.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import edu.vt.middleware.ldap.CompareOperation;
import edu.vt.middleware.ldap.CompareRequest;
import edu.vt.middleware.ldap.Connection;
import edu.vt.middleware.ldap.LdapAttribute;
import edu.vt.middleware.ldap.LdapException;
import edu.vt.middleware.ldap.LdapUtil;
import edu.vt.middleware.ldap.Response;

/**
 * Provides implementation common to compare authentication handlers.
 *
 * @author  Middleware Services
 * @version  $Revision: 2197 $
 */
public abstract class AbstractCompareAuthenticationHandler extends AbstractAuthenticationHandler {

    /** Maximum digest size. Value is {@value}. */
    protected static final int DIGEST_SIZE = 256;

    /** Default password scheme. Value is {@value}. */
    protected static final String DEFAULT_SCHEME = "SHA";

    /** Password scheme. */
    private String passwordScheme = DEFAULT_SCHEME;

    /**
   * Returns the password scheme.
   *
   * @return  password scheme
   */
    public String getPasswordScheme() {
        return passwordScheme;
    }

    /**
   * Sets the password scheme. Must equal a known message digest algorithm.
   *
   * @param  s  password scheme
   */
    public void setPasswordScheme(final String s) {
        passwordScheme = s;
    }

    /** {@inheritDoc} */
    @Override
    protected AuthenticationHandlerResponse authenticateInternal(final Connection c, final AuthenticationCriteria criteria) throws LdapException {
        byte[] hash = new byte[DIGEST_SIZE];
        try {
            final MessageDigest md = MessageDigest.getInstance(passwordScheme);
            md.update(criteria.getCredential().getBytes());
            hash = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new LdapException(e);
        }
        final LdapAttribute la = new LdapAttribute("userPassword", String.format("{%s}%s", passwordScheme, LdapUtil.base64Encode(hash)).getBytes());
        final CompareOperation compare = new CompareOperation(c);
        final CompareRequest request = new CompareRequest(criteria.getDn(), la);
        request.setControls(getAuthenticationControls());
        final Response<Boolean> compareResponse = compare.execute(request);
        return new AuthenticationHandlerResponse(compareResponse.getResult(), compareResponse.getResultCode(), c, null, compareResponse.getControls());
    }

    /**
   * Returns a connection that the compare operation should be performed on.
   *
   * @return  connection
   *
   * @throws  LdapException  if an error occurs provisioning the connection
   */
    protected abstract Connection getConnection() throws LdapException;
}
