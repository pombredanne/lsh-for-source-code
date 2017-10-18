package net.jamcache;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This strategy selects the server that is nearest to the key in the 
 * keyspace.
 */
class ProximityHashStrategy implements HashStrategy {

    private final Map<Long, Cache> cacheMap;

    ProximityHashStrategy() {
        super();
        this.cacheMap = new HashMap<Long, Cache>();
    }

    @Override
    public void add(MemcachedClient cache) {
        InetSocketAddress addr = (InetSocketAddress) cache.getSocket().getRemoteSocketAddress();
        String hostString = addr.getHostName() + ":" + addr.getPort();
        long key = apply(hostString.getBytes());
        cacheMap.put(new Long(key), cache);
    }

    @Override
    public long apply(byte[] data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return -1;
        }
        byte[] hashBytes = digest.digest(data);
        return new BigInteger(1, hashBytes).longValue();
    }

    @Override
    public Cache select(long target) {
        long c = 0;
        long key = 0;
        Iterator<Long> iter = cacheMap.keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            long k = iter.next().longValue();
            long v = k - target;
            if (v < 0) v = -v;
            if (v < c || i == 0) {
                c = v;
                key = k;
            }
        }
        return cacheMap.get(new Long(key));
    }
}
