package ultramc;

import javax.crypto.*;
import java.security.*;
import java.util.*;
import java.nio.charset.Charset;

public class ConsistentHash {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static class WeightedKey {

        private double m_weight;

        private String m_key;

        public WeightedKey(String key, double weight) {
            m_key = key;
            m_weight = weight;
        }

        public String getKey() {
            return (m_key);
        }

        public double getWeight() {
            return (m_weight);
        }
    }

    private int[] m_lookupTable;

    private List<WeightedKey> m_keys;

    private MessageDigest m_md5;

    private int m_hashCount;

    public ConsistentHash(int tableSize, int hashCount, String[] keys) {
        List<WeightedKey> wkeys = new ArrayList<WeightedKey>();
        for (String s : keys) wkeys.add(new WeightedKey(s, 1.0));
        init(tableSize, hashCount, wkeys);
    }

    public ConsistentHash(int tableSize, int hashCount, List<WeightedKey> keys) {
        init(tableSize, hashCount, keys);
    }

    private void init(int tableSize, int hashCount, List<WeightedKey> keys) {
        m_hashCount = hashCount;
        m_lookupTable = new int[tableSize];
        m_keys = new ArrayList<WeightedKey>();
        m_keys.addAll(keys);
        try {
            m_md5 = MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }
        reHash();
    }

    public String lookupKey(String lookupString) {
        long value = hash(lookupString);
        return (m_keys.get(m_lookupTable[(int) (value % m_lookupTable.length)]).getKey());
    }

    int[] getLookupTable() {
        return (m_lookupTable);
    }

    private long hash(String str) {
        byte[] hash;
        long tmp = 0L;
        long value = 0L;
        m_md5.update(UTF8.encode(str));
        hash = m_md5.digest();
        tmp = hash[0] & 0xff;
        value |= tmp << (6 * 8);
        tmp = hash[1] & 0xff;
        value |= tmp << (5 * 8);
        tmp = hash[2] & 0xff;
        value |= tmp << (4 * 8);
        tmp = hash[3] & 0xff;
        value |= tmp << (3 * 8);
        tmp = hash[4] & 0xff;
        value |= tmp << (2 * 8);
        tmp = hash[5] & 0xff;
        value |= tmp << (1 * 8);
        tmp = hash[6] & 0xff;
        value |= tmp;
        return (value);
    }

    private Set<Integer> createHashSet(String key, int count) {
        Set<Integer> ret = new HashSet<Integer>();
        for (int I = 0; I < count; I++) {
            long value = hash(key + I);
            int hashVal = (int) (value % m_lookupTable.length);
            ret.add(hashVal);
        }
        return (ret);
    }

    private void reHash() {
        Map<Integer, Integer> primaryEntryMap = new HashMap<Integer, Integer>();
        for (int I = 0; I < m_keys.size(); I++) {
            int count = (int) ((double) m_hashCount * m_keys.get(I).getWeight());
            Set<Integer> hashSet = createHashSet(m_keys.get(I).getKey(), count);
            for (Integer i : hashSet) {
                if (primaryEntryMap.put(i, I) != null) primaryEntryMap.put(i, -1);
            }
        }
        int lastValue = -1;
        for (int I = 0; I < m_lookupTable.length; I++) {
            Integer value = primaryEntryMap.get(I);
            if ((value == null) || (value == -1)) {
                m_lookupTable[I] = lastValue;
            } else {
                m_lookupTable[I] = value;
                lastValue = value;
            }
        }
        for (int I = 0; (I < m_lookupTable.length) && (m_lookupTable[I] == -1); I++) m_lookupTable[I] = lastValue;
    }
}
