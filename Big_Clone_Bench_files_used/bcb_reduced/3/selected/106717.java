package org.dllearner.kb.sparql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import org.dllearner.utilities.Files;
import org.dllearner.utilities.JamonMonitorLogger;
import com.jamonapi.Monitor;

/**
 * SPARQL query cache to avoid possibly expensive multiple queries. The queries
 * and their results are written to files. A cache has an associated cache
 * directory where all files are written.
 * 
 * Each SPARQL query and its result is written to one file. The name of this
 * file is a hash of the query. The result of the query is written as JSON
 * serialisation of the SPARQL XML result, see
 * http://www.w3.org/TR/rdf-sparql-json-res/.
 * 
 * Apart from the query and its result, a timestamp of the query is stored.
 * After a configurable amount of time, query results are considered outdated.
 * If a cached result of a SPARQL query exists, but is too old, the cache
 * behaves as if the cached result would not exist.
 * 
 * TODO: We are doing md5 hashing at the moment, so in rare cases different
 * SPARQL queries can be mapped to the same file. Support for such scenarios
 * needs to be included.
 * 
 * @author Sebastian Hellmann
 * @author Sebastian Knappe
 * @author Jens Lehmann
 */
public class Cache implements Serializable {

    private static Logger logger = Logger.getLogger(Cache.class);

    private boolean useDatabase = false;

    private ExtractionDBCache h2;

    private static final long serialVersionUID = 843308736471742205L;

    private transient String cacheDir = "";

    private transient String fileEnding = ".cache";

    private long freshnessSeconds = 15 * 24 * 60 * 60;

    /**
	 * A Persistant cache is stored in the folder cachePersistant.
	 * It has longer freshness 365 days and is mainly usefull for developing
	 * @return a Cache onject
	 */
    public static Cache getPersistentCache() {
        Cache c = new Cache(getPersistantCacheDir());
        c.setFreshnessInDays(365);
        return c;
    }

    /**
	 * @return the default cache object
	 */
    public static Cache getDefaultCache() {
        Cache c = new Cache(getDefaultCacheDir());
        return c;
    }

    /**
	 * the default cachedir normally is "cache".
	 * @return Default Cache Dir
	 */
    public static String getDefaultCacheDir() {
        return "cache";
    }

    /**
	 * a more persistant cache used for example generation."cachePersistant"
	 * @return persistant Cache Dir
	 */
    public static String getPersistantCacheDir() {
        return "cachePersistant";
    }

    /**
	 * Constructor for the cache itself.
	 * 
	 * @param cacheDir
	 *            Where the base path to the cache is .
	 */
    public Cache(String cacheDir) {
        this(cacheDir, false);
    }

    public Cache(String cacheDir, boolean useDatabase) {
        this.cacheDir = cacheDir + File.separator;
        this.useDatabase = useDatabase;
        if (!new File(cacheDir).exists()) {
            Files.mkdir(cacheDir);
            logger.info("Created directory: " + cacheDir + ".");
        }
        if (this.useDatabase) {
            h2 = new ExtractionDBCache(cacheDir);
        }
    }

    private String getHash(String string) {
        Monitor hashTime = JamonMonitorLogger.getTimeMonitor(Cache.class, "HashTime").start();
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md5.reset();
        md5.update(string.getBytes());
        byte[] result = md5.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            hexString.append(Integer.toHexString(0xFF & result[i]));
        }
        String str = hexString.toString();
        hashTime.stop();
        return str;
    }

    private String getFilename(String sparqlQuery) {
        return cacheDir + getHash(sparqlQuery) + fileEnding;
    }

    /**
	 * Gets a result for a query if it is in the cache.
	 * 
	 * @param sparqlQuery
	 *            SPARQL query to check.
	 * @return Query result as JSON or null if no result has been found or it is
	 *         outdated.
	 */
    @SuppressWarnings({ "unchecked" })
    private String getCacheEntry(String sparqlQuery) {
        String filename = getFilename(sparqlQuery);
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        LinkedList<Object> entry = null;
        try {
            FileInputStream fos = new FileInputStream(filename);
            ObjectInputStream o = new ObjectInputStream(fos);
            entry = (LinkedList<Object>) o.readObject();
            o.close();
        } catch (IOException e) {
            e.printStackTrace();
            if (Files.debug) {
                System.exit(0);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            if (Files.debug) {
                System.exit(0);
            }
        }
        long timestamp = (Long) entry.get(0);
        boolean fresh = checkFreshness(timestamp);
        if (!fresh) {
            file.delete();
            return null;
        }
        return (String) entry.get(2);
    }

    /**
	 * Adds an entry to the cache.
	 * 
	 * @param sparqlQuery
	 *            The SPARQL query.
	 * @param result
	 *            Result of the SPARQL query.
	 */
    private void addToCache(String sparqlQuery, String result) {
        String filename = getFilename(sparqlQuery);
        long timestamp = System.currentTimeMillis();
        LinkedList<Object> list = new LinkedList<Object>();
        list.add(timestamp);
        list.add(sparqlQuery);
        list.add(result);
        FileOutputStream fos = null;
        ObjectOutputStream o = null;
        try {
            fos = new FileOutputStream(filename, false);
            o = new ObjectOutputStream(fos);
            o.writeObject(list);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
                o.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private boolean checkFreshness(long timestamp) {
        return ((System.currentTimeMillis() - timestamp) <= (freshnessSeconds * 1000));
    }

    /**
	 * Takes a SPARQL query (which has not been evaluated yet) as argument and
	 * returns a JSON result set. The result set is taken from this cache if the
	 * query is stored here. Otherwise the query is send and its result added to
	 * the cache and returned. Convenience method.
	 * 
	 * @param query
	 *            The SPARQL query.
	 * @return Jena result set in JSON format
	 */
    public String executeSparqlQuery(SparqlQuery query) {
        if (useDatabase) {
            return h2.executeSelectQuery(query.getSparqlEndpoint(), query.getSparqlQueryString());
        }
        Monitor totaltime = JamonMonitorLogger.getTimeMonitor(Cache.class, "TotalTimeExecuteSparqlQuery").start();
        JamonMonitorLogger.increaseCount(Cache.class, "TotalQueries");
        Monitor readTime = JamonMonitorLogger.getTimeMonitor(Cache.class, "ReadTime").start();
        String result = getCacheEntry(query.getSparqlQueryString());
        readTime.stop();
        if (result != null) {
            JamonMonitorLogger.increaseCount(Cache.class, "SuccessfulHits");
        } else {
            query.send();
            String json = query.getJson();
            if (json != null) {
                addToCache(query.getSparqlQueryString(), json);
                logger.debug("result added to SPARQL cache: " + json);
                result = json;
            } else {
                json = "";
                result = "";
                logger.warn(Cache.class.getSimpleName() + "empty result: " + query.getSparqlQueryString());
            }
        }
        totaltime.stop();
        return result;
    }

    public boolean executeSparqlAskQuery(SparqlQuery query) {
        String str = getCacheEntry(query.getSparqlQueryString());
        JamonMonitorLogger.increaseCount(Cache.class, "TotalQueries");
        if (str != null) {
            JamonMonitorLogger.increaseCount(Cache.class, "SuccessfulHits");
            return Boolean.parseBoolean(str);
        } else {
            Boolean result = query.sendAsk();
            addToCache(query.getSparqlQueryString(), result.toString());
            return result;
        }
    }

    /**
	 * deletes all Files in the cacheDir, does not delete the cacheDir itself, 
	 * and can thus still be used without creating a new Cache Object
	 */
    public void clearCache() {
        File f = new File(cacheDir);
        String[] files = f.list();
        for (int i = 0; i < files.length; i++) {
            Files.deleteFile(new File(cacheDir + "/" + files[i]));
        }
    }

    /**
	 * Changes how long cached results will stay fresh (default 15 days).
	 * @param days number of days
	 */
    public void setFreshnessInDays(int days) {
        freshnessSeconds = days * 24 * 60 * 60;
    }
}
