package com.danga.memcached;

import org.apache.commons.codec.binary.Base64;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Date;

/**
 * Original please visit <a href="http://sourceforge.net/projects/memcachetaglib">memcachetaglib in sf</a><p/>
 * This is cache bean.<p>
 *
 * @author <a href="mailto:cytown@gmail.com">Cytown</a>
 * @version 1.09
 */
public class Cache<T extends Serializable> implements Serializable, Cloneable {

    /**
	 *
	 */
    private static final long serialVersionUID = 580163397798136538L;

    public static final long TIMEUNIT = 1000;

    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
        }
    }

    private String key = null;

    private String prekey = null;

    private String encodeKey = null;

    private ClassLoader classLoader = null;

    /**
	 * timeout time, unit: second, default: 300s.
	 */
    private volatile Date expireTime = new Date(new Date().getTime() + TIMEUNIT * 300);

    private volatile T object = null;

    /**
     * Construction for only key supplied.
     * @param key the key
     */
    public Cache(String key) {
        this.key = key;
    }

    /**
     * Constrction for key and object
     * @param key the cache key
     * @param o the object
     */
    public Cache(String key, T o) {
        this.key = key;
        this.object = o;
    }

    /**
     * Construction for full
     * @param key the cache key
     * @param o the object, must be serializable
     * @param timeout the timeout time in seconds.
     */
    public Cache(String key, T o, int timeout) {
        this.key = key;
        setTime(timeout);
        this.object = o;
    }

    /**
     * Construction for clone only
     * @param prekey the pre key
     * @param key the cache key
     * @param expireTime the timeout time in date.
     */
    private Cache(String prekey, String key, Date expireTime) {
        this.prekey = prekey;
        this.key = key;
        this.expireTime = expireTime;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * show the expire time in date
     * @return the date
     */
    public Date getExpireTime() {
        return expireTime;
    }

    /**
     * directly set the expire time.
     * @param expireTime the time in Date.
     */
    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * get the real key.
	 * @return the key
	 */
    public String getKey() {
        return key;
    }

    /**
     * set the real key.
	 * @param key the key to set
	 */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * get the full real key.
     * @return the full key
     */
    public String getFullKey() {
        return (prekey == null ? "" : prekey + "/") + key;
    }

    private String encode(String s) {
        String ret = "";
        try {
            byte[] b = digest.digest(s.getBytes("utf-8"));
            ret = new String((Base64.encodeBase64(b)), "utf-8").replace('/', '_');
        } catch (Exception e) {
        }
        return ret;
    }

    private String toKeyString(String key) {
        String ret = encode(key);
        return "/" + ret + "_" + encode(key + "_" + ret);
    }

    /**
     * Get the key being encoded from the real key.
     * @return the encoded key
     */
    public String getCacheKey() {
        if (encodeKey == null) encodeKey = toKeyString(key);
        return encodeKey;
    }

    /**
     * Set the timeout time in seconds.
	 * @param timeout the timeout to set
	 */
    public void setTime(int timeout) {
        expireTime = new Date(new Date().getTime() + timeout * TIMEUNIT);
    }

    /**
     * Get the cached object.
	 * @return the object
	 */
    public T getObject() {
        return object;
    }

    /**
     * Set the cached Object.
	 * @param o the object to set
	 */
    public void setObject(T o) {
        this.object = o;
    }

    /**
     * set the poolname of the cache used
     * @param name the prekey.
     */
    public void setPrekey(String name) {
        this.prekey = name;
    }

    /**
     * get the poolname of the cache used
     * @return name the prekey.
     */
    public String getPrekey() {
        return this.prekey;
    }

    /**
     * Check whether  the time is expired.
	 * @return true if timeout.
	 */
    public boolean isTimeOut() {
        return new Date().after(getExpireTime());
    }

    /**
     * Check whether the keys are same.
	 * @param o the object
	 * @return true for equals
	 */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Cache)) return false;
        Cache oo = (Cache) o;
        return key.equals(oo.getKey());
    }

    private String getObjectDesc() {
        if (object == null) return null;
        if (object instanceof String) {
            String s = object.toString();
            if (s.length() <= 20) return String.format("(%1d)%2$s", s.length(), s);
            return String.format("(%1$,d)%2$s...", s.length(), s.substring(0, 20));
        }
        return object.toString();
    }

    /**
     * The toString.
	 * @return the objcect
	 */
    public String toString() {
        return (prekey == null ? "" : prekey) + getCacheKey() + ":" + key + ":expire[" + getExpireTime() + "] object=[" + getObjectDesc() + "]";
    }

    public Cache copy() {
        return new Cache(prekey, key, expireTime);
    }
}
