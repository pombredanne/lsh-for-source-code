package org.pachyderm.apollo.authentication.simple;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import org.pachyderm.apollo.authentication.simple.eof.AuthRecordEO;
import org.pachyderm.apollo.core.CXAuthContext;
import org.pachyderm.apollo.core.CXAuthServices;
import org.pachyderm.apollo.core.CXAuthenticator;
import org.pachyderm.apollo.core.CXDefaults;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSSelector;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXQ;

@SuppressWarnings("rawtypes")
public class SimpleAuthentication implements CXAuthenticator {

    private static Logger LOG = Logger.getLogger(SimpleAuthentication.class.getName());

    private EOEditingContext _xec;

    private MessageDigest _md;

    private String _authRealm;

    static {
        StaticInitializer();
    }

    private static void StaticInitializer() {
        LOG.info("[-STATIC-]");
        NSNotificationCenter.defaultCenter().addObserver(SimpleAuthentication.class, new NSSelector("appWillLaunch", new Class[] { NSNotification.class }), com.webobjects.appserver.WOApplication.ApplicationWillFinishLaunchingNotification, null);
    }

    public static void appWillLaunch(NSNotification notification) {
        LOG.info("[-NOTIFY-] appWillLaunch");
        NSArray<String> authRealmArray = (NSArray<String>) CXDefaults.sharedDefaults().getObject("AuthenticationRealms");
        if (authRealmArray != null) {
            for (String authRealm : authRealmArray) {
                SimpleAuthentication simpleAuth;
                try {
                    simpleAuth = new SimpleAuthentication(authRealm);
                } catch (Exception x) {
                    LOG.error("attempt to create <SimpleAuthentication> class failed", x);
                    simpleAuth = null;
                }
                if (simpleAuth != null) {
                    CXAuthServices.getSharedServices().appendAuthenticator(simpleAuth);
                } else {
                    LOG.error("Unable to create simple authentication source for realm: " + authRealm);
                }
            }
        }
        NSNotificationCenter.defaultCenter().removeObserver(SimpleAuthentication.class);
    }

    public SimpleAuthentication(String authRealm) {
        super();
        LOG.info("[CONSTRUCT] for REALM: " + (_authRealm = authRealm));
        _xec = ERXEC.newEditingContext();
        try {
            _md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new NSForwardException(nsae, "SimpleAuthentication: Could not obtain MessageDigest instance with MD5 algorithm.");
        }
    }

    public String authenticateWithContext(CXAuthContext context) {
        String username = context.getUsername();
        if (username == null || username.length() == 0) {
            LOG.warn("performAuthenticationInContext: blank username rejected ..");
            return null;
        }
        if (context.getNewPerson() == null) {
            _md.reset();
            try {
                _md.update(context.getPassword().getBytes("UTF-8"));
            } catch (Exception x) {
                LOG.warn("performAuthenticationInContext: " + "Undigestable password rejected ..");
                return null;
            }
            NSData passHash = new NSData(_md.digest());
            EOQualifier userAndPW1 = ERXQ.and(ERXQ.equals(AuthRecordEO.USERNAME_KEY, username), ERXQ.equals(AuthRecordEO.PASSWORD_KEY, passHash));
            AuthRecordEO authenticationRecord = AuthRecordEO.fetchAuthRecord(_xec, userAndPW1);
            if (authenticationRecord == null) {
                LOG.warn("performAuthenticationInContext: " + "Not primary password.");
                EOQualifier userAndPW2 = ERXQ.and(ERXQ.equals(AuthRecordEO.USERNAME_KEY, username), ERXQ.equals(AuthRecordEO.TEMPPASSWORD_KEY, passHash));
                authenticationRecord = AuthRecordEO.fetchAuthRecord(_xec, userAndPW2);
                if (authenticationRecord == null) {
                    LOG.warn("performAuthenticationInContext: " + "Not secondary password.");
                    return null;
                }
            }
            return username + "@" + _authRealm;
        }
        if (context.getOldPerson().isAdministrator()) {
            LOG.info("performAuthenticationInContext: " + "admin now impersonating ..");
            return username + "@" + _authRealm;
        }
        LOG.error("performAuthenticationInContext: " + "non-admin trying to impersonate .. FAIL");
        return null;
    }

    public String getRealm() {
        return _authRealm;
    }

    public NSDictionary<String, Object> propertiesForPersonInContext(CXAuthContext context) {
        return NSDictionary.EmptyDictionary;
    }

    public boolean existsAuthRecord(String username) {
        if ((username == null) || (username.equals(""))) return false;
        try {
            return (AuthRecordEO.fetchAuthRecord(_xec, ERXQ.and(ERXQ.equals(AuthRecordEO.USERNAME_KEY, username), ERXQ.equals(AuthRecordEO.REALM_KEY, _authRealm))) != null);
        } catch (Exception e) {
            return false;
        }
    }

    public void insertAuthRecord(String username, String pass) {
        _md.reset();
        try {
            _md.update(pass.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            return;
        }
        try {
            AuthRecordEO.createAuthRecord(_xec, new NSData(_md.digest()), _authRealm, username);
            _xec.saveChanges();
            LOG.info("insertAuthRecord: SUCCESS");
        } catch (Exception exc) {
            _xec.revert();
            LOG.warn("insertAuthRecord: FAILURE");
        }
    }

    public void removeAuthRecord(String username) {
        try {
            _xec.deleteObject(AuthRecordEO.fetchAuthRecord(_xec, ERXQ.and(ERXQ.equals(AuthRecordEO.USERNAME_KEY, username), ERXQ.equals(AuthRecordEO.REALM_KEY, _authRealm))));
            _xec.saveChanges();
            LOG.info("removeAuthRecord: SUCCESS");
        } catch (Exception exc) {
            _xec.revert();
            LOG.warn("removeAuthRecord: FAILURE");
        }
    }

    public boolean updateAuthRecord(NSDictionary userInfoDictionary) {
        String username = (String) userInfoDictionary.valueForKey("username");
        if ((username == null) || (username.equals(""))) {
            LOG.warn("updateAuthRecord: NO USERNAME");
            return false;
        }
        if (!existsAuthRecord(username)) {
            LOG.warn("updateAuthRecord: " + username + " doesn't exist!");
            return false;
        }
        try {
            EOQualifier userAndRealm = ERXQ.and(ERXQ.equals(AuthRecordEO.USERNAME_KEY, username), ERXQ.equals(AuthRecordEO.REALM_KEY, _authRealm));
            AuthRecordEO authenticationRecord = AuthRecordEO.fetchAuthRecord(_xec, userAndRealm);
            LOG.info("updateAuthRecord: attempting to update AuthRecordEO: " + authenticationRecord.snapshot());
            String pass = (String) userInfoDictionary.valueForKey("password");
            if (pass != null) {
                _md.reset();
                try {
                    _md.update(pass.getBytes("UTF-8"));
                } catch (Exception x) {
                    LOG.error("updateAuthRecord: undigestable password rejected ..", x);
                    return false;
                }
                authenticationRecord.setPassword(new NSData(_md.digest()));
            }
            try {
                _xec.saveChanges();
                LOG.info("updateAuthRecord: SUCCESS");
            } catch (Exception exc) {
                _xec.revert();
                LOG.warn("updateAuthRecord: FAILURE");
            }
            return true;
        } catch (Exception x) {
            _xec.unlock();
            LOG.error("updateAuthRecord: - EXCEPTION SAVING USER RECORD", x);
            return false;
        }
    }
}
