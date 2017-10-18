package com.m4f.utils.cache;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cache.Cache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.memcache.InvalidValueException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.m4f.utils.StackTraceUtil;
import com.m4f.utils.cache.annotations.Cacheable;
import com.m4f.utils.cache.annotations.Cacheflush;

public class CacheInterceptor {

    protected static final Logger LOGGER = Logger.getLogger(CacheInterceptor.class.getName());

    protected static Cache cache;

    public CacheInterceptor() {
    }

    public Object cacheAble(ProceedingJoinPoint pjp) throws Throwable {
        String cacheName = "";
        Method method = null;
        try {
            method = this.getInterceptedMethod(pjp);
            Cacheable annotation = method.getAnnotation(Cacheable.class);
            cacheName = annotation != null ? annotation.cacheName() : "default";
        } catch (Exception e) {
            LOGGER.severe(StackTraceUtil.getStackTrace(e));
            cacheName = "default";
        } finally {
        }
        Object key = this.getMethodKey(method.toGenericString(), pjp.getArgs());
        Object result = null;
        MemcacheService syncCache = this.getCache(cacheName);
        if (syncCache == null) {
            LOGGER.severe("NO CACHE!!!!! Executing the method with no cache!!");
        } else {
            try {
                result = syncCache.get(key);
            } catch (InvalidValueException e) {
                LOGGER.severe(StackTraceUtil.getStackTrace(e));
            }
        }
        if (result != null) {
            LOGGER.info("Returning value with key: " + key + " from cache.");
            return result;
        } else {
            try {
                LOGGER.info("NO CACHE! Invoking the method!");
                result = pjp.proceed();
                LOGGER.info("NO CACHE! Inserting result into internal cache with key: " + key);
                syncCache.put(key, result);
                LOGGER.info("(PutCache) Current Namespace: " + syncCache.getNamespace());
                LOGGER.info("NO CACHE! Adding to cache internal cache with name: " + cacheName);
            } catch (Exception e) {
                LOGGER.severe(StackTraceUtil.getStackTrace(e));
                LOGGER.severe("Memcache KBs: " + syncCache.getStatistics().getTotalItemBytes() / 1024);
            }
        }
        return result;
    }

    public void cacheFlush(ProceedingJoinPoint pjp) throws Throwable {
        String cacheName = "";
        Method method = null;
        try {
            method = this.getInterceptedMethod(pjp);
            Cacheflush annotation = method.getAnnotation(Cacheflush.class);
            cacheName = annotation != null ? annotation.cacheName() : "default";
        } catch (Exception e) {
            LOGGER.severe(StackTraceUtil.getStackTrace(e));
            cacheName = "default";
        }
        MemcacheService syncCache = this.getCache(cacheName);
        syncCache.clearAll();
        pjp.proceed();
    }

    protected MemcacheService getCache(String name) {
        InternalCache cache = null;
        String oldNameSpace = NamespaceManager.get() != null ? NamespaceManager.get() : "";
        String namespace = !"".equals(oldNameSpace) ? oldNameSpace + "." + name : name;
        LOGGER.info("(getCache)Cache namespace: " + namespace);
        MemcacheService syncCache = null;
        try {
            syncCache = MemcacheServiceFactory.getMemcacheService(namespace);
        } catch (Exception e) {
            LOGGER.severe("Error getting cache with namespace: " + namespace);
            LOGGER.severe(StackTraceUtil.getStackTrace(e));
        } finally {
            NamespaceManager.set(oldNameSpace);
        }
        return syncCache;
    }

    protected Method getInterceptedMethod(ProceedingJoinPoint pjp) throws SecurityException, NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        Object target = pjp.getTarget();
        Class clazz = target.getClass();
        Method m = clazz.getMethod(method.getName(), method.getParameterTypes());
        return m;
    }

    protected Object getMethodKey(String methodName, Object[] args) {
        StringBuffer key = new StringBuffer(methodName.trim().replace(" ", ".")).append(".");
        for (Object o : args) {
            if (o != null) key.append(o.hashCode());
        }
        LOGGER.info("Generation key ->" + key.toString());
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            messageDigest.reset();
            messageDigest.update(key.toString().getBytes(Charset.forName("UTF8")));
            final byte[] resultByte = messageDigest.digest();
            String hex = null;
            for (int i = 0; i < resultByte.length; i++) {
                hex = Integer.toHexString(0xFF & resultByte[i]);
                if (hex.length() < 2) {
                    key.append("0");
                }
                key.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("No hash generated for method key! " + StackTraceUtil.getStackTrace(e));
        }
        LOGGER.info("Generation key ->" + key.toString());
        return new String(key);
    }
}
