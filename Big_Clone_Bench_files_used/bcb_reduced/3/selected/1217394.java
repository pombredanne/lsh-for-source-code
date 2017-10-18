package ru.adv.security.filter;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.UnavailableException;
import org.apache.commons.logging.Log;
import org.springframework.util.StringUtils;

/**
 * 
 */
public abstract class Realm {

    public static final String JDBC_REALM = "jdbc";

    public static final String JNDI_REALM = "jndi";

    public static final String INLIE_REALM = "memory";

    protected static final String CACHE_SIZE_PROP = "cacheSize";

    protected static final String CACHE_TTL_PROP = "cacheTTL";

    protected static final int DEFAULT_CACHE_SIZE = 10;

    protected static final int DEFAULT_CACHE_TTL = 60;

    private int cacheSize = DEFAULT_CACHE_SIZE;

    private int cacheTTL = DEFAULT_CACHE_TTL;

    private CacheRealm cache;

    private Log logger;

    private DigestType digestType;

    protected Realm(Log logger) {
        this.logger = logger;
    }

    public static Realm create(AuthConfig config) throws SecurityFilterInitializationException {
        Realm result = null;
        final String type = config.getRealmType();
        if (type != null) {
            if (JDBC_REALM.equalsIgnoreCase(type)) {
                result = new JDBCRealm(config.getLogger());
            } else if (JNDI_REALM.equalsIgnoreCase(type)) {
                result = new JNDIRealm(config.getLogger());
            } else if (INLIE_REALM.equalsIgnoreCase(type)) {
                result = new MemoryRealm(config.getLogger());
            } else {
                throw new SecurityFilterInitializationException("Unknown realm type: " + type);
            }
            if (result != null) {
                try {
                    result.init(config);
                } catch (Throwable e) {
                    config.getLogger().info("Can't init realm: ");
                    config.getLogger().error(e);
                    result.destroy();
                    result = null;
                    throw new SecurityFilterInitializationException("Can't init realm: " + e.getMessage());
                }
            }
        } else {
            throw new SecurityFilterInitializationException("Realm type is not set in " + config.getConfigFileName());
        }
        return result;
    }

    public boolean isHasMessageDigest() {
        return this.digestType != null;
    }

    protected DigestType getDigestType() {
        return digestType;
    }

    protected void setDigestType(DigestType digestType) {
        this.digestType = digestType;
    }

    protected void log(String messg) {
        logger.debug(messg);
    }

    protected void log(String messg, Exception e) {
        logger.warn(messg, e);
    }

    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     * 
     * @param username
     *            Username of the Principal to look up
     * @param credentials
     *            Password or other credentials to use in authenticating this
     *            username
     * @throws UnavailableException 
     */
    public abstract ADVPrincipal authenticate(String username, String credentials);

    /**
     * Return a short name for this Realm implementation, for use in log
     * messages.
     */
    protected abstract String getName();

    protected void init(AuthConfig config) throws Exception {
        initCacheParams(config);
        initCache();
        parseConfig(config);
    }

    protected abstract void parseConfig(AuthConfig config);

    protected void initCache() {
        destroyCache();
        if (getCacheSize() > 0) {
            cache = new CacheRealm(getCacheSize(), getCacheTTL(), this);
        }
    }

    private void destroyCache() {
        if (cache != null) {
            cache.destory();
            cache = null;
        }
    }

    public String digest(String credentials) {
        return md5(credentials);
    }

    public static String md5(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuilder result = new StringBuilder(new BigInteger(1, md.digest(value.toString().getBytes())).toString(16));
            while (result.length() < 32) {
                result.insert(0, "0");
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected void destroy() {
        destroyCache();
    }

    protected void setCacheSize(int size) {
        cacheSize = size;
    }

    protected void setCacheTTL(int seconds) {
        cacheTTL = seconds;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int getCacheTTL() {
        return cacheTTL;
    }

    ADVPrincipal getFromCache(String username, String credential) {
        if (cache != null) {
            return cache.get(username, credential);
        }
        return null;
    }

    void putToCache(String username, String credential, ADVPrincipal principal) {
        if (cache != null) {
            cache.put(username, credential, principal);
        }
    }

    protected void initCacheParams(AuthConfig config) {
        setCacheSize(DEFAULT_CACHE_SIZE);
        setCacheTTL(DEFAULT_CACHE_TTL);
        final String cacheSize = config.getRealmProperty(CACHE_SIZE_PROP);
        try {
            if (null != cacheSize && cacheSize.length() > 0) {
                setCacheSize(Integer.parseInt(cacheSize));
            }
        } catch (NumberFormatException e) {
            log("Bad " + CACHE_SIZE_PROP + " value: " + cacheSize, e);
        }
        final String cacheTtl = config.getRealmProperty(CACHE_TTL_PROP);
        try {
            if (null != cacheTtl && cacheTtl.length() > 0) {
                setCacheTTL(Integer.parseInt(cacheTtl));
            }
        } catch (NumberFormatException e) {
            log("Bad " + CACHE_TTL_PROP + " value: " + cacheTtl, e);
        }
    }

    void logDebug(String message) {
        logger.debug(message);
    }

    void logInfo(String message) {
        logger.info(message);
    }

    void logWarn(String message) {
        logger.warn(message);
    }

    void logErr(String message) {
        logger.error(message);
    }

    protected void initDigestProperty(AuthConfig elem) {
        String digestStr = elem.getRealmProperty("digest");
        if (StringUtils.hasLength(digestStr)) {
            try {
                setDigestType(DigestType.valueOf(digestStr.toUpperCase()));
            } catch (RuntimeException e) {
                String msg = "Unknown digest type '" + digestStr + "'";
                log(msg);
                throw new RuntimeException(msg);
            }
        }
    }

    enum DigestType {

        MD5
    }
}
