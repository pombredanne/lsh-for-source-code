package purej.cache.web;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import purej.cache.base.AbstractCacheAdministrator;
import purej.cache.base.Cache;
import purej.cache.base.CacheEntry;
import purej.cache.base.Config;
import purej.cache.base.EntryRefreshPolicy;
import purej.cache.base.NeedsRefreshException;
import purej.cache.base.events.CacheEventListener;
import purej.cache.base.events.ScopeEvent;
import purej.cache.base.events.ScopeEventListener;
import purej.cache.base.events.ScopeEventType;
import purej.logging.Logger;
import purej.logging.LoggerFactory;

/**
 * A ServletCacheAdministrator creates, flushes and administers the cache.
 * <p>
 * This is a "servlet Singleton". This means it's not a Singleton in the
 * traditional sense, that is stored in a static instance. It's a Singleton _per
 * web app context_.
 * <p>
 * Once created it manages the cache path on disk through the oscache.properties
 * file, and also keeps track of the flush times.
 * 
 * @author <a href="mailto:mike@atlassian.com">Mike Cannon-Brookes</a>
 * @author <a href="mailto:tgochenour@peregrine.com">Todd Gochenour</a>
 * @author <a href="mailto:fbeauregard@pyxis-tech.com">Francois Beauregard</a>
 * @author <a href="mailto:abergevin@pyxis-tech.com">Alain Bergevin</a>
 * @author <a
 *         href="&#109;a&#105;&#108;&#116;&#111;:chris&#64;swebtec.&#99;&#111;&#109;">Chris
 *         Miller</a>
 * @version $Revision: 1.2 $
 */
public class ServletCacheAdministrator extends AbstractCacheAdministrator implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final transient Logger log = LoggerFactory.getLogger(ServletCacheAdministrator.class, Logger.FRAMEWORK);

    private static final String PROPERTIES_FILENAME = "/com/terastring/eaf/cache/web/servlet-cache.properties";

    /**
     * Constants for properties read/written from/to file
     */
    private static final String CACHE_USE_HOST_DOMAIN_KEY = "cache.use.host.domain.in.key";

    private static final String CACHE_KEY_KEY = "cache.key";

    /**
     * The default cache key that is used to store the cache in context.
     */
    private static final String DEFAULT_CACHE_KEY = "__LSF_WEB_CACHE";

    /**
     * Constants for scope's name
     */
    public static final String SESSION_SCOPE_NAME = "session";

    public static final String APPLICATION_SCOPE_NAME = "application";

    /**
     * The key under which the CacheAdministrator will be stored in the
     * ServletContext
     */
    private static final String CACHE_ADMINISTRATOR_KEY = "__LSF_WEB_CACHE_ADMIN";

    /**
     * Key used to store the current scope in the configuration. This is a hack
     * to let the scope information get passed through to the
     * DiskPersistenceListener, and will be removed in a future release.
     */
    public static final String HASH_KEY_SCOPE = "scope";

    /**
     * Key used to store the current session ID in the configuration. This is a
     * hack to let the scope information get passed through to the
     * DiskPersistenceListener, and will be removed in a future release.
     */
    public static final String HASH_KEY_SESSION_ID = "sessionId";

    /**
     * Key used to store the servlet container temporary directory in the
     * configuration. This is a hack to let the scope information get passed
     * through to the DiskPersistenceListener, and will be removed in a future
     * release.
     */
    public static final String HASH_KEY_CONTEXT_TMPDIR = "context.tempdir";

    /**
     * The string to use as a file separator.
     */
    private static final String FILE_SEPARATOR = "/";

    /**
     * The character to use as a file separator.
     */
    private static final char FILE_SEPARATOR_CHAR = FILE_SEPARATOR.charAt(0);

    /**
     * Constant for Key generation.
     */
    private static final short AVERAGE_KEY_LENGTH = 30;

    /**
     * Usable caracters for key generation
     */
    private static final String m_strBase64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * Map containing the flush times of different scopes
     */
    private Map<Integer, Date> flushTimes;

    /**
     * Required so we can look up the app scope cache without forcing a session
     * creation.
     */
    private transient ServletContext context;

    /**
     * Key to use for storing and retrieving Object in contexts (Servlet,
     * session).
     */
    private String cacheKey;

    /**
     * Set property cache.use.host.domain.in.key=true to add domain information
     * to key generation for hosting multiple sites.
     */
    private boolean useHostDomainInKey = false;

    /**
     * Create the cache administrator.
     * 
     * This will reset all the flush times and load the properties file.
     */
    private ServletCacheAdministrator(ServletContext context, Properties p) {
        super(p);
        config.set(HASH_KEY_CONTEXT_TMPDIR, context.getAttribute("javax.servlet.context.tempdir"));
        flushTimes = new HashMap<Integer, Date>();
        initHostDomainInKey();
        this.context = context;
    }

    /**
     * Obtain an instance of the CacheAdministrator
     * 
     * @param context
     *                The ServletContext that this CacheAdministrator is a
     *                Singleton under
     * @return Returns the CacheAdministrator instance for this context
     */
    public static ServletCacheAdministrator getInstance(ServletContext context) {
        return getInstance(context, null);
    }

    /**
     * Obtain an instance of the CacheAdministrator
     * 
     * @param context
     *                The ServletContext that this CacheAdministrator is a
     *                Singleton under
     * @param p
     *                the properties to use for the cache if the cache
     *                administrator has not been created yet. Once the
     *                administrator has been created, the properties parameter
     *                is ignored for all future invocations. If a null value is
     *                passed in, then the properties are loaded from the
     *                oscache.properties file in the classpath.
     * @return Returns the CacheAdministrator instance for this context
     */
    public static synchronized ServletCacheAdministrator getInstance(ServletContext context, Properties p) {
        ServletCacheAdministrator admin = (ServletCacheAdministrator) context.getAttribute(CACHE_ADMINISTRATOR_KEY);
        if (admin == null) {
            p = getProps();
            admin = new ServletCacheAdministrator(context, p);
            context.setAttribute(CACHE_ADMINISTRATOR_KEY, admin);
            if (true) {
                log.info("Created new instance of ServletCacheAdministrator");
            }
            admin.getAppScopeCache(context);
        }
        if (admin.context == null) {
            admin.context = context;
        }
        return admin;
    }

    /**
     * Shuts down the cache administrator. This should usually only be called
     * when the controlling application shuts down.
     */
    public static void destroyInstance(ServletContext context) {
        ServletCacheAdministrator admin;
        admin = (ServletCacheAdministrator) context.getAttribute(CACHE_ADMINISTRATOR_KEY);
        if (admin != null) {
            Cache cache = (Cache) context.getAttribute(admin.getCacheKey());
            if (cache != null) {
                admin.finalizeListeners(cache);
                context.removeAttribute(admin.getCacheKey());
                context.removeAttribute(CACHE_ADMINISTRATOR_KEY);
                cache = null;
                if (true) {
                    log.info("Shut down the ServletCacheAdministrator");
                }
            }
            admin = null;
        }
    }

    /**
     * Grabs the cache for the specified scope
     * 
     * @param request
     *                The current request
     * @param scope
     *                The scope of this cache (<code>PageContext.APPLICATION_SCOPE</code>
     *                or <code>PageContext.SESSION_SCOPE</code>)
     * @return The cache
     */
    public Cache getCache(HttpServletRequest request, int scope) {
        if (scope == PageContext.APPLICATION_SCOPE) {
            return getAppScopeCache(context);
        }
        if (scope == PageContext.SESSION_SCOPE) {
            return getSessionScopeCache(request.getSession(true));
        }
        throw new RuntimeException("The supplied scope value of " + scope + " is invalid. Acceptable values are PageContext.APPLICATION_SCOPE and PageContext.SESSION_SCOPE");
    }

    /**
     * A convenience method to retrieve the application scope cache
     * 
     * @param context
     *                the current <code>ServletContext</code>
     * @return the application scope cache. If none is present, one will be
     *         created.
     */
    public Cache getAppScopeCache(ServletContext context) {
        Cache cache;
        Object obj = context.getAttribute(getCacheKey());
        if ((obj == null) || !(obj instanceof Cache)) {
            if (true) {
                log.info("Created new application-scoped cache at key: " + getCacheKey());
            }
            cache = createCache(PageContext.APPLICATION_SCOPE, null);
            context.setAttribute(getCacheKey(), cache);
        } else {
            cache = (Cache) obj;
        }
        return cache;
    }

    /**
     * A convenience method to retrieve the session scope cache
     * 
     * @param session
     *                the current <code>HttpSession</code>
     * @return the session scope cache for this session. If none is present, one
     *         will be created.
     */
    public Cache getSessionScopeCache(HttpSession session) {
        Cache cache;
        Object obj = session.getAttribute(getCacheKey());
        if ((obj == null) || !(obj instanceof Cache)) {
            if (true) {
                log.info("Created new session-scoped cache in session " + session.getId() + " at key: " + getCacheKey());
            }
            cache = createCache(PageContext.SESSION_SCOPE, session.getId());
            session.setAttribute(getCacheKey(), cache);
        } else {
            cache = (Cache) obj;
        }
        return cache;
    }

    /**
     * Get the cache key from the properties. Set it to a default value if it is
     * not present in the properties
     * 
     * @return The cache.key property or the DEFAULT_CACHE_KEY
     */
    public String getCacheKey() {
        if (cacheKey == null) {
            cacheKey = getProperty(CACHE_KEY_KEY);
            if (cacheKey == null) {
                cacheKey = DEFAULT_CACHE_KEY;
            }
        }
        return cacheKey;
    }

    /**
     * Set the flush time for a specific scope to a specific time
     * 
     * @param date
     *                The time to flush the scope
     * @param scope
     *                The scope to be flushed
     */
    public void setFlushTime(Date date, int scope) {
        if (true) {
            log.info("Flushing scope " + scope + " at " + date);
        }
        synchronized (flushTimes) {
            if (date != null) {
                dispatchScopeEvent(ScopeEventType.SCOPE_FLUSHED, scope, date, null);
                flushTimes.put(new Integer(scope), date);
            } else {
                logError("setFlushTime called with a null date.");
                throw new IllegalArgumentException("setFlushTime called with a null date.");
            }
        }
    }

    /**
     * Set the flush time for a specific scope to the current time.
     * 
     * @param scope
     *                The scope to be flushed
     */
    public void setFlushTime(int scope) {
        setFlushTime(new Date(), scope);
    }

    /**
     * Get the flush time for a particular scope.
     * 
     * @param scope
     *                The scope to get the flush time for.
     * @return A date representing the time this scope was last flushed. Returns
     *         null if it has never been flushed.
     */
    public Date getFlushTime(int scope) {
        synchronized (flushTimes) {
            return flushTimes.get(new Integer(scope));
        }
    }

    /**
     * Retrieve an item from the cache
     * 
     * @param scope
     *                The cache scope
     * @param request
     *                The servlet request
     * @param key
     *                The key of the object to retrieve
     * @param refreshPeriod
     *                The time interval specifying if an entry needs refresh
     * @return The requested object
     * @throws NeedsRefreshException
     */
    public Object getFromCache(int scope, HttpServletRequest request, String key, int refreshPeriod) throws NeedsRefreshException {
        Cache cache = getCache(request, scope);
        key = this.generateEntryKey(key, request, scope);
        return cache.getFromCache(key, refreshPeriod);
    }

    /**
     * Checks if the given scope was flushed more recently than the CacheEntry
     * provided. Used to determine whether to refresh the particular CacheEntry.
     * 
     * @param cacheEntry
     *                The cache entry which we're seeing whether to refresh
     * @param scope
     *                The scope we're checking
     * 
     * @return Whether or not the scope has been flushed more recently than this
     *         cache entry was updated.
     */
    public boolean isScopeFlushed(CacheEntry cacheEntry, int scope) {
        Date flushDateTime = getFlushTime(scope);
        if (flushDateTime != null) {
            long lastUpdate = cacheEntry.getLastUpdate();
            return (flushDateTime.getTime() >= lastUpdate);
        } else {
            return false;
        }
    }

    /**
     * Register a listener for Cache Map events.
     * 
     * @param listener
     *                The object that listens to events.
     */
    public void addScopeEventListener(ScopeEventListener listener) {
        listenerList.add(ScopeEventListener.class, listener);
    }

    /**
     * Cancels a pending cache update. This should only be called by a thread
     * that received a {@link NeedsRefreshException} and was unable to generate
     * some new cache content.
     * 
     * @param scope
     *                The cache scope
     * @param request
     *                The servlet request
     * @param key
     *                The cache entry key to cancel the update of.
     */
    public void cancelUpdate(int scope, HttpServletRequest request, String key) {
        Cache cache = getCache(request, scope);
        key = this.generateEntryKey(key, request, scope);
        cache.cancelUpdate(key);
    }

    /**
     * Flush all scopes at a particular time
     * 
     * @param date
     *                The time to flush the scope
     */
    public void flushAll(Date date) {
        synchronized (flushTimes) {
            setFlushTime(date, PageContext.APPLICATION_SCOPE);
            setFlushTime(date, PageContext.SESSION_SCOPE);
            setFlushTime(date, PageContext.REQUEST_SCOPE);
            setFlushTime(date, PageContext.PAGE_SCOPE);
        }
        dispatchScopeEvent(ScopeEventType.ALL_SCOPES_FLUSHED, -1, date, null);
    }

    /**
     * Flush all scopes instantly.
     */
    public void flushAll() {
        flushAll(new Date());
    }

    /**
     * Generates a cache entry key.
     * 
     * If the string key is not specified, the HTTP request URI and QueryString
     * is used. Operating systems that have a filename limitation less than 255
     * or have filenames that are case insensitive may have issues with key
     * generation where two distinct pages map to the same key.
     * <p>
     * POST Requests (which have no distinguishing query string) may also
     * generate identical keys for what is actually different pages. In these
     * cases, specify an explicit key attribute for the CacheTag.
     * 
     * @param key
     *                The key entered by the user
     * @param request
     *                The current request
     * @param scope
     *                The scope this cache entry is under
     * @return The generated cache key
     */
    public String generateEntryKey(String key, HttpServletRequest request, int scope) {
        return generateEntryKey(key, request, scope, null, null);
    }

    /**
     * Generates a cache entry key.
     * 
     * If the string key is not specified, the HTTP request URI and QueryString
     * is used. Operating systems that have a filename limitation less than 255
     * or have filenames that are case insensitive may have issues with key
     * generation where two distinct pages map to the same key.
     * <p>
     * POST Requests (which have no distinguishing query string) may also
     * generate identical keys for what is actually different pages. In these
     * cases, specify an explicit key attribute for the CacheTag.
     * 
     * @param key
     *                The key entered by the user
     * @param request
     *                The current request
     * @param scope
     *                The scope this cache entry is under
     * @param language
     *                The ISO-639 language code to distinguish different pages
     *                in application scope
     * @return The generated cache key
     */
    public String generateEntryKey(String key, HttpServletRequest request, int scope, String language) {
        return generateEntryKey(key, request, scope, language, null);
    }

    /**
     * Generates a cache entry key.
     * <p>
     * If the string key is not specified, the HTTP request URI and QueryString
     * is used. Operating systems that have a filename limitation less than 255
     * or have filenames that are case insensitive may have issues with key
     * generation where two distinct pages map to the same key.
     * <p>
     * POST Requests (which have no distinguishing query string) may also
     * generate identical keys for what is actually different pages. In these
     * cases, specify an explicit key attribute for the CacheTag.
     * 
     * @param key
     *                The key entered by the user
     * @param request
     *                The current request
     * @param scope
     *                The scope this cache entry is under
     * @param language
     *                The ISO-639 language code to distinguish different pages
     *                in application scope
     * @param suffix
     *                The ability to put a suffix at the end of the key
     * @return The generated cache key
     */
    public String generateEntryKey(String key, HttpServletRequest request, int scope, String language, String suffix) {
        StringBuffer cBuffer = new StringBuffer(AVERAGE_KEY_LENGTH);
        if (language != null) {
            cBuffer.append(FILE_SEPARATOR).append(language);
        }
        if (useHostDomainInKey) {
            cBuffer.append(FILE_SEPARATOR).append(request.getServerName());
        }
        if (key != null) {
            cBuffer.append(FILE_SEPARATOR).append(key);
        } else {
            String generatedKey = request.getRequestURI();
            if (generatedKey.charAt(0) != FILE_SEPARATOR_CHAR) {
                cBuffer.append(FILE_SEPARATOR_CHAR);
            }
            cBuffer.append(generatedKey);
            cBuffer.append("_").append(request.getMethod()).append("_");
            generatedKey = getSortedQueryString(request);
            if (generatedKey != null) {
                try {
                    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
                    byte[] b = digest.digest(generatedKey.getBytes());
                    cBuffer.append("_");
                    cBuffer.append(toBase64(b).replace('/', '_'));
                } catch (Exception e) {
                }
            }
        }
        if ((suffix != null) && (suffix.length() > 0)) {
            cBuffer.append(suffix);
        }
        return cBuffer.toString();
    }

    /**
     * Creates a string that contains all of the request parameters and their
     * values in a single string. This is very similar to
     * <code>HttpServletRequest.getQueryString()</code> except the parameters
     * are sorted by name, and if there is a <code>jsessionid</code> parameter
     * it is filtered out.
     * <p>
     * If the request has no parameters, this method returns <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    protected String getSortedQueryString(HttpServletRequest request) {
        Map paramMap = request.getParameterMap();
        if (paramMap.isEmpty()) {
            return null;
        }
        Set paramSet = new TreeMap(paramMap).entrySet();
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (Iterator it = paramSet.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String[] values = (String[]) entry.getValue();
            for (int i = 0; i < values.length; i++) {
                String key = (String) entry.getKey();
                if ((key.length() != 10) || !"jsessionid".equals(key)) {
                    if (first) {
                        first = false;
                    } else {
                        buf.append('&');
                    }
                    buf.append(key).append('=').append(values[i]);
                }
            }
        }
        if (buf.length() == 0) {
            return null;
        } else {
            return buf.toString();
        }
    }

    /**
     * Log error messages to commons logging.
     * 
     * @param message
     *                Message to log.
     */
    public void logError(String message) {
        log.error("[oscache]: " + message);
    }

    /**
     * Put an object in the cache
     * 
     * @param scope
     *                The cache scope
     * @param request
     *                The servlet request
     * @param key
     *                The object key
     * @param content
     *                The object to add
     */
    public void putInCache(int scope, HttpServletRequest request, String key, Object content) {
        putInCache(scope, request, key, content, null);
    }

    /**
     * Put an object in the cache
     * 
     * @param scope
     *                The cache scope
     * @param request
     *                The servlet request
     * @param key
     *                The object key
     * @param content
     *                The object to add
     * @param policy
     *                The refresh policy
     */
    public void putInCache(int scope, HttpServletRequest request, String key, Object content, EntryRefreshPolicy policy) {
        Cache cache = getCache(request, scope);
        key = this.generateEntryKey(key, request, scope);
        cache.putInCache(key, content, policy);
    }

    /**
     * Sets the cache capacity (number of items). If the cache contains more
     * than <code>capacity</code> items then items will be removed to bring
     * the cache back down to the new size.
     * 
     * @param scope
     *                The cache scope
     * @param request
     *                The servlet request
     * @param capacity
     *                The new capacity
     */
    public void setCacheCapacity(int scope, HttpServletRequest request, int capacity) {
        setCacheCapacity(capacity);
        getCache(request, scope).setCapacity(capacity);
    }

    /**
     * Unregister a listener for Cache Map events.
     * 
     * @param listener
     *                The object that currently listens to events.
     */
    public void removeScopeEventListener(ScopeEventListener listener) {
        listenerList.remove(ScopeEventListener.class, listener);
    }

    /**
     * Finalizes all the listeners that are associated with the given cache
     * object
     */
    @Override
    protected void finalizeListeners(Cache cache) {
        super.finalizeListeners(cache);
    }

    /**
     * Convert a byte array into a Base64 string (as used in mime formats)
     */
    private static String toBase64(byte[] aValue) {
        int byte1;
        int byte2;
        int byte3;
        int iByteLen = aValue.length;
        StringBuffer tt = new StringBuffer();
        for (int i = 0; i < iByteLen; i += 3) {
            boolean bByte2 = (i + 1) < iByteLen;
            boolean bByte3 = (i + 2) < iByteLen;
            byte1 = aValue[i] & 0xFF;
            byte2 = (bByte2) ? (aValue[i + 1] & 0xFF) : 0;
            byte3 = (bByte3) ? (aValue[i + 2] & 0xFF) : 0;
            tt.append(m_strBase64Chars.charAt(byte1 / 4));
            tt.append(m_strBase64Chars.charAt((byte2 / 16) + ((byte1 & 0x3) * 16)));
            tt.append(((bByte2) ? m_strBase64Chars.charAt((byte3 / 64) + ((byte2 & 0xF) * 4)) : '='));
            tt.append(((bByte3) ? m_strBase64Chars.charAt(byte3 & 0x3F) : '='));
        }
        return tt.toString();
    }

    /**
     * Create a cache
     * 
     * @param scope
     *                The cache scope
     * @param sessionId
     *                The sessionId for with the cache will be created
     * @return A new cache
     */
    private ServletCache createCache(int scope, String sessionId) {
        ServletCache newCache = new ServletCache(this, algorithmClass, cacheCapacity, scope);
        config.set(HASH_KEY_SCOPE, "" + scope);
        config.set(HASH_KEY_SESSION_ID, sessionId);
        newCache = (ServletCache) configureStandardListeners(newCache);
        if (config.getProperty(CACHE_ENTRY_EVENT_LISTENERS_KEY) != null) {
            CacheEventListener[] listeners = getCacheEventListeners();
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] instanceof ScopeEventListener) {
                    newCache.addCacheEventListener(listeners[i], ScopeEventListener.class);
                }
            }
        }
        return newCache;
    }

    /**
     * Dispatch a scope event to all registered listeners.
     * 
     * @param eventType
     *                The type of event
     * @param scope
     *                Scope that was flushed (Does not apply for FLUSH_ALL
     *                event)
     * @param date
     *                Date of flushing
     * @param origin
     *                The origin of the event
     */
    private void dispatchScopeEvent(ScopeEventType eventType, int scope, Date date, String origin) {
        ScopeEvent event = new ScopeEvent(eventType, scope, date, origin);
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ScopeEventListener.class) {
                ((ScopeEventListener) listeners[i + 1]).scopeFlushed(event);
            }
        }
    }

    /**
     * Set property cache.use.host.domain.in.key=true to add domain information
     * to key generation for hosting multiple sites
     */
    private void initHostDomainInKey() {
        String propStr = getProperty(CACHE_USE_HOST_DOMAIN_KEY);
        useHostDomainInKey = "true".equalsIgnoreCase(propStr);
    }

    private static Properties getProps() {
        Properties properties = new Properties();
        InputStream in = null;
        try {
            in = Config.class.getResourceAsStream(PROPERTIES_FILENAME);
            properties.load(in);
            log.debug("Servlet cache properties : " + properties);
        } catch (Exception e) {
            log.error("Error reading [" + PROPERTIES_FILENAME + "] in ServletCacheAdministrator.loadProps() " + e);
            log.error("Ensure the [" + PROPERTIES_FILENAME + "] file is readable and in your classpath.");
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return properties;
    }
}
