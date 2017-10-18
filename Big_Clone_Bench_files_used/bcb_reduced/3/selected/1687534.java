package org.nakedobjects.plugins.hibernate.authentication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DatabaseMetaData;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.nakedobjects.commons.ensure.Assert;
import org.nakedobjects.commons.exceptions.NakedObjectException;
import org.nakedobjects.metamodel.config.NakedObjectConfiguration;
import org.nakedobjects.plugins.hibernate.objectstore.util.HibernateUtil;
import org.nakedobjects.runtime.authentication.AuthenticationRequest;
import org.nakedobjects.runtime.authentication.AuthenticationRequestPassword;
import org.nakedobjects.runtime.authentication.standard.AuthenticatorAbstract;
import org.nakedobjects.runtime.context.NakedObjectsContext;

/**
 * example properties for various database types:
 * <pre>
 * nakedobjects.component.authenticator.algorithm= MD5
 * 
 * nakedobjects.component.authenticator.selectusersql.default = \
 *   select count(*) \
 *     from user u  \
 *    where username = ? and password = ?
 *    
 * nakedobjects.component.authenticator.selectrolessql.default = \
 *   select r.rolename as rr \ 
 *     from role r, user u, user_role ur \ 
 *    where r.id = ur.role and ur.user = u.id and u.username = ?
 *    
 * nakedobjects.component.authenticator.selectusersql.hsqldb = \
 *   select count(*) \
 *     from user u \
 *    where username = ? and password = ?
 *     
 * nakedobjects.component.authenticator.selectrolessql.hsqldb = \
 *   select r.rolename as rr \
 *     from role r, user u, user_role ur \
 *    where r.id = ur.role and ur.user = u.id \
 *      and u.username = ?
 *       
 * nakedobjects.component.authenticator.selectusersql.mysql = 
 *   select count(*) from `user` u \
 *    where `username` = ? and `password` = ?
 *     
 * nakedobjects.component.authenticator.selectrolessql.mysql = \
 *   select r.`rolename` as rr \
 *     from `role` r, `user` u, `user_role` ur \
 *    where r.`id` = ur.`role` and ur.`user` = u.`id` and u.`username` = ?
 *     
 * nakedobjects.component.authenticator.selectusersql.postgresql = \
 *   select count(*) \
 *     from "user" u \
 *    where "username" = ? and "password" = ?
 *    
 * nakedobjects.component.authenticator.selectrolessql.postgresql = \
 *   select r."rolename" as rr \
 *     from "role" r, "user" u, "user_role" ur \
 *   where r."id" = ur."role" and ur."user" = u."id" and u."username" = ?
 * </pre>
 */
public class DatabaseAuthenticator extends AuthenticatorAbstract {

    private static final boolean FAILED_AUTHENTICATION = false;

    private static MessageDigest messageDigest;

    private static String ALGORITHM = "MD5";

    private static final String NO_ALGORITHM = "none";

    private static String SELECT_USER_SQL = "select count(*) from user u where username = ? and password = ?";

    private static String SELECT_ROLES_SQL = "select r.rolename as rr from role r, user u, user_role ur where r.id = ur.role and ur.user = u.id and u.username = ?";

    private static final String AUTH_CFG = "nakedobjects.authentication.";

    private static final String SELECT_USER_CFG = AUTH_CFG + "selectusersql.";

    private static final String SELECT_ROLES_CFG = AUTH_CFG + "selectrolessql.";

    private static final String DEFAULT_CFG = "default";

    private static final String ALGORITHM_CFG = AUTH_CFG + "algorithm";

    private static boolean initializedSQL;

    private static boolean initializedAlgorithm;

    private static String getDBType(final String dbURL) {
        if (dbURL != null && dbURL.toLowerCase().startsWith("jdbc:")) {
            final int indexOfFirstColon = dbURL.indexOf(":");
            final int indexOfSecondColon = dbURL.indexOf(":", indexOfFirstColon + 1);
            if (indexOfSecondColon > indexOfFirstColon) {
                return dbURL.substring(indexOfFirstColon + 1, indexOfSecondColon).trim();
            }
        }
        return DEFAULT_CFG;
    }

    public DatabaseAuthenticator(NakedObjectConfiguration configuration) {
        super(configuration);
    }

    private void justInTimeInitialiseSQL() {
        if (!initializedSQL) {
            try {
                HibernateUtil.startTransaction();
                final DatabaseMetaData dbm = HibernateUtil.getCurrentSession().connection().getMetaData();
                HibernateUtil.commitTransaction();
                final String databaseType = getDBType(dbm.getURL());
                SELECT_USER_SQL = NakedObjectsContext.getConfiguration().getString(SELECT_USER_CFG + DEFAULT_CFG, SELECT_USER_SQL);
                SELECT_ROLES_SQL = getConfiguration().getString(SELECT_ROLES_CFG + DEFAULT_CFG, SELECT_ROLES_SQL);
                SELECT_USER_SQL = NakedObjectsContext.getConfiguration().getString(SELECT_USER_CFG + databaseType, SELECT_USER_SQL);
                SELECT_ROLES_SQL = NakedObjectsContext.getConfiguration().getString(SELECT_ROLES_CFG + databaseType, SELECT_ROLES_SQL);
            } catch (final Exception e) {
                HibernateUtil.rollbackTransaction();
                throw new NakedObjectException(e);
            }
            initializedSQL = true;
        }
    }

    private void justInTimeInitialiseAlgorithm() {
        if (!initializedAlgorithm) {
            try {
                ALGORITHM = getConfiguration().getString(ALGORITHM_CFG, ALGORITHM);
                if (!ALGORITHM.equalsIgnoreCase(NO_ALGORITHM)) {
                    messageDigest = MessageDigest.getInstance(ALGORITHM);
                }
            } catch (final NoSuchAlgorithmException nsa) {
                throw new NakedObjectException(nsa);
            }
            initializedAlgorithm = true;
        }
    }

    private final void setRoles(final AuthenticationRequest request) {
        try {
            HibernateUtil.startTransaction();
            final SQLQuery sq = HibernateUtil.getCurrentSession().createSQLQuery(SELECT_ROLES_SQL);
            sq.setString(0, request.getName());
            sq.addScalar("rr", Hibernate.STRING);
            final List<String> roles = sq.list();
            HibernateUtil.commitTransaction();
            request.setRoles(roles);
        } catch (final Exception e) {
            HibernateUtil.rollbackTransaction();
            throw new NakedObjectException(e);
        }
    }

    public String generateHash(final String key) {
        justInTimeInitialiseAlgorithm();
        if (ALGORITHM.equalsIgnoreCase(NO_ALGORITHM)) {
            return key;
        }
        synchronized (messageDigest) {
            messageDigest.reset();
            messageDigest.update(key.getBytes());
            final byte[] bytes = messageDigest.digest();
            final StringBuffer buff = new StringBuffer();
            for (int l = 0; l < bytes.length; l++) {
                final String hx = Integer.toHexString(0xFF & bytes[l]);
                if (hx.length() == 1) {
                    buff.append("0");
                }
                buff.append(hx);
            }
            return buff.toString().trim();
        }
    }

    private int count(final Object countValue) {
        if (countValue == null) {
            return 0;
        }
        if (countValue instanceof Number) {
            return ((Number) countValue).intValue();
        }
        throw new NakedObjectException("Unexpected type");
    }

    public final boolean isValidUser(final AuthenticationRequest request) {
        final AuthenticationRequestPassword passwordRequest = (AuthenticationRequestPassword) request;
        final String username = passwordRequest.getName();
        if (username == null || username.equals("")) {
            return FAILED_AUTHENTICATION;
        }
        final String password = passwordRequest.getPassword();
        Assert.assertNotNull(password);
        try {
            HibernateUtil.startTransaction();
            final SQLQuery sq = HibernateUtil.getCurrentSession().createSQLQuery(SELECT_USER_SQL);
            sq.setString(0, username);
            sq.setString(1, generateHash(password));
            final Object result = sq.uniqueResult();
            HibernateUtil.commitTransaction();
            return count(result) > 0;
        } catch (final Exception e) {
            HibernateUtil.rollbackTransaction();
        }
        return FAILED_AUTHENTICATION;
    }

    public final boolean isValid(final AuthenticationRequest request) {
        justInTimeInitialiseSQL();
        final boolean valid = isValidUser(request);
        if (valid) {
            setRoles(request);
        }
        return valid;
    }

    public final boolean canAuthenticate(final AuthenticationRequest request) {
        return request instanceof AuthenticationRequestPassword;
    }
}
