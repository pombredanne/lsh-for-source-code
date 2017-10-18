package net.ontopia.topicmaps.nav2.realm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.security.Principal;
import java.security.MessageDigest;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import net.ontopia.net.Base64Encoder;
import net.ontopia.topicmaps.entry.SharedStoreRegistry;
import net.ontopia.topicmaps.entry.TopicMapReferenceIF;
import net.ontopia.topicmaps.entry.TopicMapRepositoryIF;
import net.ontopia.topicmaps.entry.TopicMaps;
import net.ontopia.topicmaps.core.TopicIF;
import net.ontopia.topicmaps.core.TopicMapIF;
import net.ontopia.topicmaps.core.TopicMapStoreIF;
import net.ontopia.topicmaps.core.TMObjectIF;
import net.ontopia.topicmaps.nav2.utils.NavigatorUtils;
import net.ontopia.topicmaps.nav2.impl.basic.NavigatorApplication;
import net.ontopia.topicmaps.query.core.QueryProcessorIF;
import net.ontopia.topicmaps.query.core.QueryResultIF;
import net.ontopia.topicmaps.query.core.InvalidQueryException;
import net.ontopia.topicmaps.query.utils.QueryUtils;
import net.ontopia.utils.OntopiaRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * INTERNAL: TMLoginModule provides authentication to web applications by 
 * checking user credentials against information stored in a topicmap.
 */
public class TMLoginModule implements LoginModule {

    private static Logger log = LoggerFactory.getLogger(TMLoginModule.class.getName());

    private Subject subject;

    private CallbackHandler callbackHandler;

    private Map<String, ?> sharedState;

    private Map<String, ?> options;

    private boolean loginSucceeded;

    private boolean commitSucceeded;

    private String username;

    private String password;

    private String hashMethod;

    private Principal userPrincipal;

    private List<RolePrincipal> rolePrincipals;

    private String jndiname;

    protected String topicmapId;

    private String repositoryId;

    public TMLoginModule() {
        log.debug("TMLoginModule: constructor");
        rolePrincipals = new ArrayList<RolePrincipal>();
    }

    public boolean abort() throws LoginException {
        if (!loginSucceeded) {
            return false;
        } else if (commitSucceeded == false) {
            loginSucceeded = false;
            username = null;
            password = null;
            userPrincipal = null;
            rolePrincipals.clear();
        } else {
            logout();
        }
        return true;
    }

    /**
   * Add relevant Principals to the subject.
   */
    public boolean commit() throws LoginException {
        if (!loginSucceeded) return false;
        userPrincipal = new UserPrincipal(username);
        if (!subject.getPrincipals().contains(userPrincipal)) subject.getPrincipals().add(userPrincipal);
        processRoles();
        Iterator<RolePrincipal> iter = rolePrincipals.iterator();
        while (iter.hasNext()) {
            Principal rolePrincipal = iter.next();
            if (!subject.getPrincipals().contains(rolePrincipal)) subject.getPrincipals().add(rolePrincipal);
        }
        log.debug("TMLoginModule: committed");
        commitSucceeded = true;
        username = null;
        password = null;
        return true;
    }

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        log.debug("TMLoginModule: initialize");
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        jndiname = (String) options.get("jndi_repository");
        if (jndiname == null) jndiname = (String) options.get("jndiname");
        topicmapId = (String) options.get("topicmap");
        repositoryId = (String) options.get("repository");
        if (topicmapId == null) throw new OntopiaRuntimeException("'topicmap' option is not provided to the JAAS module. Check jaas.config file.");
        hashMethod = (String) options.get("hashmethod");
        if (hashMethod == null) hashMethod = "plaintext";
    }

    /** 
   * Prompt the user for username and password, and verify those.
   */
    public boolean login() throws LoginException {
        log.debug("TMLoginModule: login");
        if (callbackHandler == null) throw new LoginException("Error: no CallbackHandler available " + "to garner authentication information from the user");
        NameCallback nameCallback = new NameCallback("user name: ");
        PasswordCallback passwordCallback = new PasswordCallback("password: ", false);
        try {
            callbackHandler.handle(new Callback[] { nameCallback, passwordCallback });
            this.username = nameCallback.getName();
            char[] charpassword = passwordCallback.getPassword();
            password = (charpassword == null ? "" : new String(charpassword));
            passwordCallback.clearPassword();
        } catch (java.io.IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException("Error: " + uce.getCallback() + " not available to garner authentication information " + "from the user");
        }
        loginSucceeded = verifyUsernamePassword(username, password);
        return loginSucceeded;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(userPrincipal);
        Iterator<RolePrincipal> iter = rolePrincipals.iterator();
        while (iter.hasNext()) {
            Principal rolePrincipal = iter.next();
            if (!subject.getPrincipals().contains(rolePrincipal)) subject.getPrincipals().remove(rolePrincipal);
        }
        log.debug("TMLoginModule: logout");
        loginSucceeded = false;
        commitSucceeded = false;
        username = null;
        password = null;
        userPrincipal = null;
        rolePrincipals.clear();
        return true;
    }

    private static String getName(TopicIF topic) {
        return net.ontopia.topicmaps.utils.TopicStringifiers.getDefaultStringifier().toString(topic);
    }

    private static String getId(Object that) {
        if (that instanceof TMObjectIF) return NavigatorUtils.getStableId((TMObjectIF) that); else if (that instanceof TopicMapReferenceIF) return ((TopicMapReferenceIF) that).getId(); else return null;
    }

    protected TopicMapIF getTopicMap() {
        TopicMapStoreIF store;
        boolean readonly = true;
        if (jndiname != null) {
            SharedStoreRegistry ssr = NavigatorApplication.lookupSharedStoreRegistry(jndiname);
            TopicMapRepositoryIF repository = ssr.getTopicMapRepository();
            TopicMapReferenceIF ref = repository.getReferenceByKey(topicmapId);
            try {
                store = ref.createStore(readonly);
            } catch (java.io.IOException e) {
                throw new OntopiaRuntimeException("Unable to create store for '" + topicmapId + "'", e);
            }
        } else {
            if (repositoryId == null) store = TopicMaps.createStore(topicmapId, readonly); else store = TopicMaps.createStore(topicmapId, readonly, repositoryId);
        }
        log.debug("TMLoginModule Initialised Correctly");
        return store.getTopicMap();
    }

    public static String hashPassword(String username, String password, String hashMethod) {
        String encodedPassword;
        if (hashMethod.equals("base64")) {
            try {
                encodedPassword = Base64Encoder.encode(username + password);
            } catch (Exception e) {
                throw new OntopiaRuntimeException("Problem occurred when attempting to hash password", e);
            }
        } else if (hashMethod.equals("md5")) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                byte[] digest = messageDigest.digest((username + password).getBytes("ISO-8859-1"));
                encodedPassword = Base64Encoder.encode(new String(digest, "ISO-8859-1"));
            } catch (Exception e) {
                throw new OntopiaRuntimeException("Problems occurrend when attempting to hash password", e);
            }
        } else if (hashMethod.equals("plaintext")) {
            encodedPassword = password;
        } else {
            throw new OntopiaRuntimeException("Invalid password encoding: " + hashMethod);
        }
        return encodedPassword;
    }

    /**
    * Query the topicmap for any roles played by the USER.
    * Create RolePrincipals for each of those roles.
    */
    private void processRoles() {
        TopicMapIF topicMap = getTopicMap();
        QueryResultIF queryResult = null;
        try {
            QueryProcessorIF queryProcessor = QueryUtils.getQueryProcessor(topicMap);
            log.info("Processing roles for user '" + username + "'");
            String query = "using um for i\"http://psi.ontopia.net/userman/\"" + "select $ROLE, $PRIVILEGE from " + "instance-of($USER, um:user), " + "occurrence($USER, $O1), type($O1, um:username), value($O1, %USERNAME%), " + "um:plays-role($USER : um:user, $ROLE : um:role), " + "{ um:has-privilege($ROLE : um:receiver, $PRIVILEGE : um:privilege) }?";
            Map<String, String> params = Collections.singletonMap("USERNAME", username);
            queryResult = queryProcessor.execute(query, params);
            Collection<Object> visited = new HashSet<Object>();
            while (queryResult.next()) {
                TopicIF r = (TopicIF) queryResult.getValue(0);
                if (!visited.contains(r)) {
                    String rolename = getName(r);
                    if (rolename != null) rolePrincipals.add(new RolePrincipal(rolename));
                    visited.add(r);
                    log.info("Added role-principal from user-group '" + rolename + "' for user '" + username + "'");
                }
                TopicIF p = (TopicIF) queryResult.getValue(1);
                if (p != null) {
                    if (!visited.contains(p)) {
                        String rolename = getName(p);
                        if (rolename != null) rolePrincipals.add(new RolePrincipal(rolename));
                        visited.add(p);
                        log.info("Added role-principal from privilege '" + rolename + "' for user '" + username + "'");
                    }
                }
            }
            log.info("Added implicit role-principal 'user' for user '" + username + "'");
            rolePrincipals.add(new RolePrincipal("user"));
        } catch (InvalidQueryException e) {
            throw new OntopiaRuntimeException(e);
        } finally {
            if (queryResult != null) queryResult.close();
            if (topicMap != null) topicMap.getStore().close();
        }
    }

    private boolean verifyUsernamePassword(String username, String password) {
        if (username == null || password == null) return false;
        TopicMapIF topicMap = getTopicMap();
        QueryResultIF queryResult = null;
        try {
            log.debug("Topic map: " + topicMap);
            QueryProcessorIF queryProcessor = QueryUtils.getQueryProcessor(topicMap);
            String query = "using um for i\"http://psi.ontopia.net/userman/\" " + "select $USER from " + "instance-of($USER, um:user), " + "occurrence($USER, $O1), type($O1, um:username), value($O1, %USERNAME%), " + "occurrence($USER, $O2), type($O2, um:password), value($O2, %PASSWORD%)?";
            Map<String, String> params = new HashMap<String, String>(2);
            params.put("USERNAME", username);
            params.put("PASSWORD", hashPassword(username, password, hashMethod));
            queryResult = queryProcessor.execute(query, params);
            if (queryResult.next()) {
                TopicIF user = (TopicIF) queryResult.getValue(0);
                log.info("Authenticated user: " + user);
                return true;
            } else {
                log.info("User '" + username + "' not authenticated");
                return false;
            }
        } catch (InvalidQueryException e) {
            throw new OntopiaRuntimeException(e);
        } finally {
            if (queryResult != null) queryResult.close();
            if (topicMap != null) topicMap.getStore().close();
        }
    }
}
