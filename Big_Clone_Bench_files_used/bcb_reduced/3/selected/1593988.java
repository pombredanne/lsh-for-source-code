package ca.ucalgary.apollo.authentication.simple;

import ca.ucalgary.apollo.core.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.net.*;
import java.security.*;
import java.io.*;

public class SimpleAuthentication implements CXAuthSource {

    private static final String SimpleAuthenticationModelName = "SimpleAuthentication.eomodeld";

    private static final String[] _qualifierKeys = new String[] { "username", "password" };

    private static final String[] _qualifierKeysSecondary = new String[] { "username", "temppassword" };

    private static final String[] _qualifierKeysAdminPassthrough = new String[] { "username" };

    private EOObjectStoreCoordinator _osc;

    private EOEditingContext _ec;

    private EOModel _model;

    private MessageDigest _md;

    private String _authRecordEntityName;

    private String _realm;

    private static final String SimpleAuthSourcesKey = "SimpleAuthSources";

    private static final String URIKey = "uri";

    public SimpleAuthentication(URI uri) {
        super();
        _initWithURI(uri);
    }

    private void _initWithURI(URI uri) {
        _realm = _parseRealmFromURI(uri);
        NSDictionary cd = _parseConnectionDictionaryFromURI(uri);
        EOModel model = _createModelWithConnectionDictionary(cd, _realm);
        EOModelGroup.defaultGroup().addModel(model);
        _initWithModel(model);
    }

    private void _initWithModel(EOModel model) {
        _model = model;
        _authRecordEntityName = "AuthRecord" + _unique();
        _osc = new EOObjectStoreCoordinator();
        _ec = new EOEditingContext(_osc);
        try {
            _md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new NSForwardException(nsae, getClass().getName() + " could not obtain MessageDigest instance with MD5 algorithm.");
        }
    }

    public String realm() {
        return _realm;
    }

    private boolean isAdmin(CXDirectoryPerson person) {
        if (person == null) {
            return false;
        }
        NSArray groups = CXDirectoryServices.sharedServices().groups();
        java.util.Enumeration groupEnumerator = groups.objectEnumerator();
        while (groupEnumerator.hasMoreElements()) {
            CXDirectoryGroup group = (CXDirectoryGroup) groupEnumerator.nextElement();
            if (group.valueForProperty("name").equals("Admin")) {
                NSArray members = group.members();
                java.util.Enumeration memberEnumerator = members.objectEnumerator();
                while (memberEnumerator.hasMoreElements()) {
                    CXDirectoryPerson aPerson = (CXDirectoryPerson) memberEnumerator.nextElement();
                    if (aPerson == person) {
                        return true;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public String performAuthenticationInContext(CXAuthContext context) {
        String user = (String) context.infoForKey(CXAuthContext.UsernameKey);
        String pass = (String) context.infoForKey(CXAuthContext.PasswordKey);
        CXDirectoryPerson person = (CXDirectoryPerson) context.infoForKey("person");
        EOEnterpriseObject record;
        if ((person != null) && (isAdmin(person))) {
            _ec.lock();
            try {
                record = EOUtilities.objectMatchingValues(_ec, _authRecordEntityName, new NSDictionary(new Object[] { user }, _qualifierKeysAdminPassthrough));
            } catch (Exception e) {
                record = null;
            }
            _ec.unlock();
        } else if (user == null || pass == null) {
            return null;
        } else {
            _ec.lock();
            _md.reset();
            try {
                _md.update(pass.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException uee) {
                _ec.unlock();
                return null;
            }
            byte[] md5 = _md.digest();
            NSData passHash = new NSData(md5);
            try {
                record = EOUtilities.objectMatchingValues(_ec, _authRecordEntityName, new NSDictionary(new Object[] { user, passHash }, _qualifierKeys));
            } catch (Exception e) {
                record = null;
            }
            if (record == null) {
                try {
                    record = EOUtilities.objectMatchingValues(_ec, _authRecordEntityName, new NSDictionary(new Object[] { user, passHash }, _qualifierKeysSecondary));
                } catch (Exception e2) {
                    record = null;
                }
            }
            _ec.unlock();
        }
        return (record != null) ? user + "@" + _realm : null;
    }

    public NSDictionary propertiesForPersonInContext(CXAuthContext context) {
        return NSDictionary.EmptyDictionary;
    }

    public boolean _checkUserExists(String username) {
        if ((username != null) && (!username.equals(""))) {
            try {
                _ec.lock();
                NSMutableDictionary matchingDict = new NSMutableDictionary();
                matchingDict.takeValueForKey(username, "username");
                matchingDict.takeValueForKey(_realm, "realm");
                EOEnterpriseObject eo = EOUtilities.objectMatchingValues(_ec, _authRecordEntityName, matchingDict.immutableClone());
                if (eo != null) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public boolean _updateUser(NSDictionary userInfo) {
        String name = (String) userInfo.valueForKey("username");
        if ((name == null) || (name.equals(""))) {
            System.out.println("SimpleAuthentication._updateUser() - NO USERNAME");
            return false;
        }
        if (!_checkUserExists(name)) {
            System.out.println("SimpleAuthentication._updateUser(): " + name + " doesn't exist!\n");
            return false;
        }
        try {
            _ec.lock();
            _md.reset();
            NSMutableDictionary matchingDict = new NSMutableDictionary();
            matchingDict.takeValueForKey(name, "username");
            matchingDict.takeValueForKey(_realm, "realm");
            EOEnterpriseObject eo = EOUtilities.objectMatchingValues(_ec, _authRecordEntityName, matchingDict.immutableClone());
            System.out.println("attempting to update EO: " + eo.snapshot());
            String pass = (String) userInfo.valueForKey("password");
            String tempPass = (String) userInfo.valueForKey("temppassword");
            if (pass != null) {
                try {
                    _md.update(pass.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException uee) {
                    _ec.unlock();
                    System.out.println("SimpleAuthentication._updateUser() - UNSUPPORTEDENCODINGEXCEPTION");
                    return false;
                }
                byte[] md5 = _md.digest();
                NSData passHash = new NSData(md5);
                eo.takeStoredValueForKey(passHash, "password");
                eo.takeStoredValueForKey(null, "temppassword");
            } else if (tempPass != null) {
                try {
                    _md.update(tempPass.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException uee) {
                    _ec.unlock();
                    System.out.println("SimpleAuthentication._updateUser() - UNSUPPORTED ENCODINGEXCEPTION");
                    return false;
                }
                byte[] md5 = _md.digest();
                NSData tempPassHash = new NSData(md5);
                eo.takeStoredValueForKey(tempPassHash, "temppassword");
            }
            _ec.saveChanges();
            _ec.unlock();
            return true;
        } catch (Exception e) {
            _ec.unlock();
            System.out.println("SimpleAuthentication._updateUser() - EXCEPTION SAVING USER RECORD");
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * If the user identified by username exists, that record will have the password
	 * updated with the md5 hash of the provided password. If there are any problems,
	 * false will be returned.
	 *
	 * @param username		the existing username (non-null)
	 * @param password		the new password (non-null)
	 * @return				true if and only if the user password was successfully updated
	 */
    public boolean _updateUserWithNewPassword(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        _ec.lock();
        _md.reset();
        try {
            _md.update(password.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            _ec.unlock();
            return false;
        }
        byte[] md5 = _md.digest();
        NSData passHash = new NSData(md5);
        EOEnterpriseObject eo;
        try {
            eo = EOUtilities.objectMatchingValues(_ec, _authRecordEntityName, new NSDictionary(new Object[] { username }, new String[] { _qualifierKeys[0] }));
        } catch (Exception e) {
            _ec.unlock();
            return false;
        }
        if (eo == null) return false;
        eo.takeStoredValueForKey(passHash, "password");
        eo.takeStoredValueForKey(null, "temppassword");
        _ec.saveChanges();
        _ec.unlock();
        return true;
    }

    /**
	 * _updateUserWithNewTempPassword
	 * If the user identified by username exists, that record will have the temp password
	 * updated with the md5 hash of the provided password. If there are any problems,
	 * false will be returned.
	 *
	 * @param username		the existing username (non-null)
	 * @param password		the new password (non-null)
	 * @return				true if and only if the user password was successfully updated
	 */
    public boolean _updateUserWithNewTempPassword(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        _ec.lock();
        _md.reset();
        try {
            _md.update(password.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            _ec.unlock();
            return false;
        }
        byte[] md5 = _md.digest();
        NSData passHash = new NSData(md5);
        EOEnterpriseObject eo;
        try {
            eo = EOUtilities.objectMatchingValues(_ec, _authRecordEntityName, new NSDictionary(new Object[] { username }, new String[] { _qualifierKeysSecondary[0] }));
        } catch (Exception e) {
            _ec.unlock();
            return false;
        }
        if (eo == null) return false;
        eo.takeStoredValueForKey(passHash, "temppassword");
        _ec.saveChanges();
        _ec.unlock();
        return true;
    }

    /**
	 *	_clearTempPasswordForUser
	 *	This method was created in order to get rid of the temp password field.
	 *
	 *	@param username		The existing username (not null)
	 *	@return				true if successful
	 */
    public boolean _clearTempPasswordForUser(String username) {
        if (username == null) {
            return false;
        }
        _ec.lock();
        EOEnterpriseObject eo;
        try {
            eo = EOUtilities.objectMatchingValues(_ec, _authRecordEntityName, new NSDictionary(new Object[] { username }, new String[] { _qualifierKeysSecondary[0] }));
        } catch (Exception e) {
            _ec.unlock();
            return false;
        }
        if (eo == null) return false;
        eo.takeStoredValueForKey(null, "temppassword");
        _ec.saveChanges();
        _ec.unlock();
        return true;
    }

    public void _addUser(String name, String pass) {
        _ec.lock();
        _md.reset();
        try {
            _md.update(pass.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            _ec.unlock();
            return;
        }
        byte[] md5 = _md.digest();
        NSData passHash = new NSData(md5);
        EOEnterpriseObject eo = EOUtilities.createAndInsertInstance(_ec, _authRecordEntityName);
        eo.takeStoredValueForKey(name, "username");
        eo.takeStoredValueForKey(passHash, "password");
        eo.takeStoredValueForKey(_realm, "realm");
        _ec.saveChanges();
        _ec.unlock();
    }

    private String _parseRealmFromURI(URI uri) {
        String path = uri.getPath();
        NSArray components = NSArray.componentsSeparatedByString(path, "/");
        int count = components.count();
        String name;
        if (count < 2) {
            name = "default";
        } else {
            name = (String) components.objectAtIndex(0);
            if (name.length() == 0) {
                if (count > 2) {
                    name = (String) components.objectAtIndex(2);
                } else {
                    name = "default";
                }
            } else {
                name = (String) components.objectAtIndex(1);
            }
        }
        return name;
    }

    private NSDictionary _parseConnectionDictionaryFromURI(URI uri) {
        String userInfo = uri.getUserInfo();
        String host = uri.getHost();
        String path = uri.getPath();
        String username;
        String password;
        String dbname;
        if (userInfo != null && userInfo.length() > 0) {
            int idx = userInfo.indexOf(':');
            if (idx != -1) {
                username = userInfo.substring(0, idx);
                password = userInfo.substring(idx + 1, userInfo.length());
            } else {
                username = userInfo;
                password = "";
            }
        } else {
            username = password = "";
        }
        if (host == null || host.length() == 0) {
            NSLog.err.appendln("The host was not specified in the uri. " + getClass().getName() + " will use localhost.\n" + uri);
            host = "localhost";
        }
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("The uri " + uri + " does not provide a database name to connect to. The database name should be provided as the first path component.");
        }
        NSArray components = NSArray.componentsSeparatedByString(path, "/");
        dbname = (String) components.objectAtIndex(0);
        if (dbname.length() == 0 && components.count() > 1) {
            dbname = (String) components.objectAtIndex(1);
        }
        String url = "jdbc:mysql://" + host + "/" + dbname;
        return new NSDictionary(new String[] { username, password, url }, new String[] { "username", "password", "URL" });
    }

    private EOModel _createModelWithConnectionDictionary(NSDictionary connectionDictionary, String realm) {
        NSBundle bundle = NSBundle.bundleForName("SimpleAuthenticationSupport");
        String resourcePath = bundle.resourcePathForLocalizedResourceNamed(SimpleAuthenticationModelName, null);
        URL pathURL = bundle.pathURLForResourcePath(resourcePath);
        EOModel model = new EOModel(pathURL);
        EOEntity entity = model.entityNamed("AuthRecord");
        entity.setName(entity.name() + _unique());
        entity.setRestrictingQualifier(new EOKeyValueQualifier("realm", EOQualifier.QualifierOperatorEqual, realm));
        NSDictionary cd = model.connectionDictionary();
        if (cd != null) {
            NSMutableDictionary mutable = cd.mutableClone();
            mutable.addEntriesFromDictionary(connectionDictionary);
            connectionDictionary = mutable;
        }
        model.setConnectionDictionary(connectionDictionary);
        model.setName(model.name() + _unique());
        return model;
    }

    private String _unique() {
        return String.valueOf(hashCode());
    }

    static {
        NSNotificationCenter.defaultCenter().addObserver(SimpleAuthentication.class, new NSSelector("_applicationWillFinishLaunching", new Class[] { NSNotification.class }), com.webobjects.appserver.WOApplication.ApplicationWillFinishLaunchingNotification, null);
    }

    public static void _applicationWillFinishLaunching(NSNotification notification) {
        CXDefaults defaults = CXDefaults.sharedDefaults();
        NSArray definitions = (NSArray) defaults.copyValue(SimpleAuthSourcesKey, CXDefaults.CurrentApplication, CXDefaults.AnyUser, CXDefaults.CurrentHost);
        if (definitions != null) {
            int i, count = definitions.count();
            NSDictionary definition;
            SimpleAuthentication source;
            CXAuthServices authServices = CXAuthServices.sharedServices();
            for (i = 0; i < count; i++) {
                definition = (NSDictionary) definitions.objectAtIndex(i);
                source = _sourceWithDictionary(definition);
                if (source != null) {
                    authServices.registerAuthenticationSource(source);
                } else {
                    NSLog.err.appendln("Unable to create simple authentication source with dictionary: " + definition);
                }
            }
        }
        NSNotificationCenter.defaultCenter().removeObserver(SimpleAuthentication.class);
    }

    private static SimpleAuthentication _sourceWithDictionary(NSDictionary definition) {
        try {
            String uri = (String) definition.objectForKey(URIKey);
            return new SimpleAuthentication(new URI(uri));
        } catch (Exception e) {
            return null;
        }
    }
}
