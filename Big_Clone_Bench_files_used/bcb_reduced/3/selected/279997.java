package org.jecars;

import com.google.gdata.util.common.base.StringUtil;
import java.security.MessageDigest;
import java.util.logging.Logger;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.LoginModule;
import java.util.*;
import javax.jcr.Session;
import org.jecars.jaas.CARS_PasswordService;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.jecars.apps.CARS_AccountsApp;
import org.jecars.jaas.CARS_Credentials;
import org.jecars.servlets.JeCARS_RESTServlet;

/**
 * CARS_LoginModule
 *
 * @version $Id: CARS_LoginModule.java,v 1.10 2009/05/26 09:21:41 weertj Exp $
 */
public class CARS_LoginModule implements LoginModule {

    private static final Logger LOG = Logger.getLogger("org.jecars");

    private static final String OPT_ANONYMOUS = "anonymousId";

    private static final String OPT_DEFAULT = "defaultUserId";

    private static final String DEFAULT_ANONYMOUS_ID = "anonymous";

    private Subject mSubject;

    private CallbackHandler mCallbackHandler;

    private final Set mPrincipals = new HashSet();

    private String mAnonymousUserId = DEFAULT_ANONYMOUS_ID;

    private String mDefaultUserId = null;

    public static boolean gSuperuserAllowed = false;

    public CARS_LoginModule() {
    }

    /**
   * Returns the anonymous user id.
   *
   * @return anonymous user id
   */
    public String getAnonymousId() {
        return mAnonymousUserId;
    }

    /**
   * Sets the default user id to be used when no login credentials
   * are presented.
   * 
   * @param pAnonymousId user id
   */
    public void setAnonymousId(String pAnonymousId) {
        mAnonymousUserId = pAnonymousId;
        return;
    }

    /**
   * Returns the default user id.
   *
   * @return default user id
   */
    public String getDefaultUserId() {
        return mDefaultUserId;
    }

    /**
   * Sets the default user id to be used when no login credentials
   * are presented.
   * 
   * @param pDefaultUserId default user id
   */
    public void setDefaultUserId(String pDefaultUserId) {
        mDefaultUserId = pDefaultUserId;
        return;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public void initialize(Subject pSubject, CallbackHandler pCallbackHandler, Map pSharedState, Map pOptions) {
        mSubject = pSubject;
        mCallbackHandler = pCallbackHandler;
        String userId = (String) pOptions.get(OPT_ANONYMOUS);
        if (userId != null) {
            mAnonymousUserId = userId;
        }
        if (pOptions.containsKey(OPT_DEFAULT)) {
            mDefaultUserId = (String) pOptions.get(OPT_DEFAULT);
        }
        return;
    }

    /** getKeyNodeName
   *
   * @param pKey
   * @return
   * @throws RepositoryException
   */
    protected static String getKeyNodeName(final String pKey) throws RepositoryException {
        final Session s = CARS_Factory.getSystemCarsSession();
        synchronized (s) {
            final Node akp = s.getRootNode().getNode(CARS_AccessManager.ACCOUNTKEYSPATH);
            if (akp.hasNode(pKey)) {
                final Node an = akp.getNode(pKey);
                return an.getProperty(CARS_ActionContext.gDefTitle).getString();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean login() throws LoginException {
        if (mCallbackHandler == null) {
            throw new LoginException("No CallbackHandler available");
        }
        boolean authenticated = false;
        mPrincipals.clear();
        try {
            final CredentialsCallback ccb = new CredentialsCallback();
            mCallbackHandler.handle(new Callback[] { ccb });
            final Credentials creds = ccb.getCredentials();
            if (creds != null) {
                if (creds instanceof CARS_Credentials) {
                    UserPrincipal userP = null;
                    final CARS_Credentials sc = (CARS_Credentials) creds;
                    String userName = CARS_Utils.decode(sc.getUserID());
                    final Object attr = sc.getAttribute(SecurityConstants.IMPERSONATOR_ATTRIBUTE);
                    if (attr != null && attr instanceof Subject) {
                    } else {
                        if (!userName.equals(CARS_AccessManager.gSuperuserName)) {
                            final Session loginSession = CARS_Factory.getSystemLoginSession();
                            if (loginSession != null) {
                                synchronized (loginSession) {
                                    if (userName.startsWith(CARS_AccountsApp.AUTHKEY_PREFIX)) {
                                        final String authKey = userName;
                                        userName = getKeyNodeName(authKey);
                                        final Node users = CARS_Factory.getSystemLoginSession().getRootNode().getNode(CARS_AccessManager.gUsersPath);
                                        if ((userName != null) && users.hasNode(userName)) {
                                            final Node user = users.getNode(userName);
                                            if (user.hasProperty(CARS_AccessManager.gPasswordProperty)) {
                                                userP = new UserPrincipal(userName);
                                                authenticated = true;
                                            }
                                        } else {
                                            userName = CARS_AccountsApp.checkCircleOfTrust(sc, authKey.substring(CARS_AccountsApp.AUTHKEY_PREFIX.length()));
                                            if ((userName != null) && users.hasNode(userName)) {
                                                final Node user = users.getNode(userName);
                                                if (user.hasProperty(CARS_AccessManager.gPasswordProperty)) {
                                                    userP = new UserPrincipal(userName);
                                                    authenticated = true;
                                                }
                                            } else {
                                                throw new LoginException("Login error for: " + authKey);
                                            }
                                        }
                                    } else {
                                        final Node users = CARS_Factory.getSystemLoginSession().getRootNode().getNode(CARS_AccessManager.gUsersPath);
                                        if (users.hasNode(userName)) {
                                            final Node user = users.getNode(userName);
                                            if ((user.hasProperty("jecars:Suspended")) && (user.getProperty("jecars:Suspended").getBoolean())) {
                                                throw new AccountLockedException(userName + " is suspended");
                                            }
                                            final String password = new String(sc.getPassword());
                                            if (password.startsWith(JeCARS_RESTServlet.AUTH_TYPE.DIGEST.toString())) {
                                                if ((user.isNodeType("jecars:digestauth") && (user.hasProperty("jecars:HA1")))) {
                                                    final String checkPass = password.substring(JeCARS_RESTServlet.AUTH_TYPE.DIGEST.toString().length() + 1);
                                                    final String[] pass = checkPass.split("\n");
                                                    final String response = pass[0];
                                                    final MessageDigest md = MessageDigest.getInstance("MD5");
                                                    final byte[] md5 = md.digest((user.getProperty("jecars:HA1").getString() + pass[1]).getBytes());
                                                    if (StringUtil.bytesToHexString(md5).equals(response)) {
                                                        authenticated = true;
                                                    }
                                                }
                                            } else {
                                                if (user.hasProperty(CARS_AccessManager.gPasswordProperty)) {
                                                    if (user.getProperty(CARS_AccessManager.gPasswordProperty).getString().equals(CARS_PasswordService.getInstance().encrypt(password))) {
                                                        authenticated = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            authenticated = gSuperuserAllowed;
                        }
                    }
                    if (mAnonymousUserId.equals(userName)) {
                        mPrincipals.add(new AnonymousPrincipal());
                    } else {
                        if (userP == null) {
                            mPrincipals.add(new UserPrincipal(userName));
                        } else {
                            mPrincipals.add(userP);
                        }
                    }
                }
            } else if (mDefaultUserId == null) {
                mPrincipals.add(new AnonymousPrincipal());
                authenticated = true;
            } else {
                mPrincipals.add(new UserPrincipal(mDefaultUserId));
                authenticated = true;
            }
        } catch (RepositoryException re) {
            throw new LoginException(re.getMessage());
        } catch (java.io.IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getCallback().toString() + " not available");
        } catch (AccountLockedException ale) {
            throw ale;
        } catch (Exception ne) {
            throw new LoginException(ne.getMessage());
        }
        if (authenticated) {
            return !mPrincipals.isEmpty();
        } else {
            mPrincipals.clear();
            throw new FailedLoginException();
        }
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public boolean commit() throws LoginException {
        if (mPrincipals.isEmpty()) {
            return false;
        } else {
            mSubject.getPrincipals().addAll(mPrincipals);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean abort() throws LoginException {
        if (mPrincipals.isEmpty()) {
            return false;
        } else {
            logout();
        }
        return true;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public boolean logout() throws LoginException {
        mSubject.getPrincipals().removeAll(mPrincipals);
        mPrincipals.clear();
        return true;
    }
}
