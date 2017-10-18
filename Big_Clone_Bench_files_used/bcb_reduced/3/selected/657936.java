package openadmin.jaas;

import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import openadmin.dao.exception.DataException;
import openadmin.dao.operation.DaoJpaHibernate;
import openadmin.dao.operation.DaoOperationFacade;
import openadmin.dao.operation.LogDao;
import openadmin.dao.operation.LogOperationFacade;
import openadmin.jaas.exception.ExcepcioJaas;
import openadmin.model.control.User;

public class LoginAyto implements LoginModule {

    private static final int ERROR_AUTENTIFICACIO = 1;

    private Subject subject;

    private CallbackHandler handler;

    private Map sharedState;

    private Map options;

    private boolean autentificacio;

    private boolean commitAutentificacio;

    private PrincipalAyto usuariPr;

    private String user;

    private char[] password;

    private DaoOperationFacade conn = null;

    @Override
    public void initialize(Subject subject, CallbackHandler handler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.handler = handler;
        this.sharedState = sharedState;
        this.options = options;
        autentificacio = false;
        commitAutentificacio = false;
    }

    @Override
    public boolean login() throws LoginException {
        if (handler == null) {
            throw new LoginException("Error: no CallbackHandler available");
        }
        try {
            Callback[] callbacks = new Callback[] { new NameCallback("User: "), new PasswordCallback("Password: ", false) };
            handler.handle(callbacks);
            user = ((NameCallback) callbacks[0]).getName().trim();
            password = ((PasswordCallback) callbacks[1]).getPassword();
            ((PasswordCallback) callbacks[1]).clearPassword();
            autentificacio = validate();
            callbacks[0] = null;
            callbacks[1] = null;
            if (!autentificacio) {
                throw new ExcepcioJaas(ERROR_AUTENTIFICACIO, conn.getLanguage().getString("ERROR_AUTENTIFICACIO"));
            }
            return true;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            autentificacio = false;
            throw new LoginException(e.getMessage());
        }
    }

    @Override
    public boolean abort() throws LoginException {
        if (autentificacio == false) {
            return false;
        } else if (autentificacio == true && commitAutentificacio == false) {
            autentificacio = false;
            user = null;
            usuariPr = null;
        } else {
            logout();
        }
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (autentificacio == false) {
            return false;
        }
        usuariPr = new PrincipalAyto(user);
        if (!subject.getPrincipals().contains(usuariPr)) subject.getPrincipals().add(usuariPr);
        user = null;
        commitAutentificacio = true;
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(usuariPr);
        autentificacio = false;
        commitAutentificacio = false;
        try {
            conn.finalize();
            if (usuariPr != null) {
                ContextSessioI ctx = new ContextSessio();
                ctx.baixaSessio(ctx.consultaSessio(usuariPr.getName()));
            }
        } catch (DataException e) {
        }
        user = null;
        usuariPr = null;
        return true;
    }

    private boolean validate() throws Exception {
        boolean valid = false;
        LogOperationFacade log = null;
        Digest digest = new Digest();
        User pUser = new User();
        pUser.setDescription(user);
        if (conn == null) {
            DaoOperationFacade connLog = new DaoJpaHibernate(pUser, "log_post", null);
            log = new LogDao(connLog, "login");
            conn = new DaoJpaHibernate(pUser, "ayto_post", log);
        }
        conn.begin();
        pUser = (User) conn.findObjectDescription(pUser);
        conn.commit();
        if (pUser == null) {
            return false;
        }
        String hash = pUser.getPassword();
        if (hash != null && password != null && password.length > 0 && pUser.isActive()) {
            valid = hash.equals(digest.digest(new String(password)));
        }
        if (valid) {
            Context ctx = new Context();
            ctx.setPerson(pUser);
            ctx.setConn(conn);
            ctx.setLog(log);
            ContextSessioI ctxS = new ContextSessio();
            ctxS.altaSessio(ctx);
        }
        return valid;
    }
}
