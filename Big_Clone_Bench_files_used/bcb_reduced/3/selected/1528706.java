package DE.FhG.IGD.util;

import codec.util.JCA;
import java.security.*;
import java.util.*;
import java.io.InputStream;
import java.io.IOException;

/**
 * This class supports easy digesting of data by mutliple
 * message digests in parallel with complete resolution
 * of algorithm names.
 *
 * @author Volker Roth
 * @version "$Id: Digester.java 117 2000-12-06 17:47:39Z vroth $"
 */
public class Digester extends Object {

    /**
     * The size of the buffer for reading data.
     */
    public static final int BUFFER_SIZE = 4096;

    /**
     * The map of trusted digest algorithms.
     */
    protected Map trusted_;

    /**
     * The threshold number of trusted digests required
     * for acceptance.
     */
    protected int threshold_;

    /**
     * Creates an instance that trusts the given digests
     * and requires at least <code>threshold</code>
     * trusted digests to accept data. The digests are
     * passed as a colon, semicolon, comma or space
     * separated list of algorithm names.
     *
     * @param trusted The trusted message digest
     *   algorithms.
     * @param threshold The minimum number of trusted
     *   digests required to accept data.
     * @exception IllegalArgumentException if the threshold
     *   exceeds the number of trusted algorithms.
     */
    public Digester(String[] trusted, int threshold) throws IllegalArgumentException, NoSuchAlgorithmException {
        int n;
        MessageDigest md;
        if (threshold > trusted.length) {
            throw new IllegalArgumentException("Threshold exceeds number of trusted algorithms!");
        }
        trusted_ = new HashMap(3);
        for (n = trusted.length - 1; n >= 0; n--) {
            try {
                md = MessageDigest.getInstance(trusted[n]);
                trusted_.put(md.getAlgorithm().toLowerCase(), md);
            } catch (NoSuchAlgorithmException e) {
            }
        }
        if (trusted_.size() == 0) {
            throw new NoSuchAlgorithmException("No trusted digest algorithm available!");
        }
        if (threshold > trusted_.size()) {
            throw new IllegalArgumentException("Threshold exceeds number of available algorithms!");
        }
        threshold_ = threshold;
    }

    /**
     * Calls reset on all digests.
     */
    public void reset() {
        Iterator i;
        for (i = trusted_.values().iterator(); i.hasNext(); ) {
            ((MessageDigest) i.next()).reset();
        }
    }

    /**
     * This method consumes and digests the data in the given
     * input stream using those of the message digests passed
     * in <code>algs</code> that are in the trusted set. If
     * the number of remaining algorithms is less then then
     * threshold value then an exception is thrown to indicate
     * that the data should not be trusted anyway.<p>
     *
     * The resulting message digests (byte arrays) are put
     * into the given map. The map is cleared before the digests
     * are put into it. Only digests of trusted digest algorithms
     * are put into the map. The key being used equals the
     * names passed in <code>algs</code>.
     *
     * @param algs The algorithms to use for digesting; only
     *   those algorithms are used that are trusted.
     * @param digests The map in which the computed digests
     *   are put with the corresponding algorithm name as
     *   the key. The keys equal the names given in <code>
     *   algs</code>.
     * @param in The input stream from which the data is read.
     */
    public void digest(String[] algs, Map digests, InputStream in) throws DigestException, IOException {
        MessageDigest md;
        byte[] buffer;
        List list;
        int i;
        int n;
        if (in == null || digests == null || algs == null) {
            throw new NullPointerException("Algs, Map, or input stream");
        }
        list = resolveAlgorithmNames(algs);
        if (threshold_ > list.size()) {
            throw new DigestException("Not enough digests of trusted algorithms!");
        }
        buffer = new byte[BUFFER_SIZE];
        while ((n = in.read(buffer)) > 0) {
            for (i = 0; i < list.size(); i += 2) {
                ((MessageDigest) list.get(i)).update(buffer, 0, n);
            }
        }
        digests.clear();
        for (i = 0; i < list.size(); i += 2) {
            md = (MessageDigest) list.get(i);
            digests.put(list.get(i + 1), md.digest());
        }
    }

    /**
     * This method digests the data in the given byte array
     * using those of the message digests passed in <code>
     * algs</code> that are in the trusted set. If the number
     * of remaining algorithms is less then then threshold
     * value then an exception is thrown to indicate that the
     * data should not be trusted anyway.<p>
     *
     * The resulting message digests (byte arrays) are put
     * into the given map. The map is cleared before the digests
     * are put into it. Only digests of trusted digest algorithms
     * are put into the map. The key being used equals the
     * names passed in <code>algs</code>.
     *
     * @param algs The algorithms to use for digesting; only
     *   those algorithms are used that are trusted.
     * @param digests The map in which the computed digests
     *   are put with the corresponding algorithm name as
     *   the key. The keys equal the names given in <code>
     *   algs</code>.
     * @param data The bytes to be digested.
     */
    public void digest(String[] algs, Map digests, byte[] data) throws DigestException, IOException {
        MessageDigest md;
        List list;
        int i;
        if (data == null || digests == null || algs == null) {
            throw new NullPointerException("Algs, Map, or data");
        }
        list = resolveAlgorithmNames(algs);
        if (threshold_ > list.size()) {
            throw new DigestException("Not enough digests of trusted algorithms!");
        }
        for (i = 0; i < list.size(); i += 2) {
            ((MessageDigest) list.get(i)).update(data);
        }
        digests.clear();
        for (i = 0; i < list.size(); i += 2) {
            md = (MessageDigest) list.get(i);
            digests.put(list.get(i + 1), md.digest());
        }
    }

    /**
     * Resolves the algorithm names to the names of the trusted
     * algorithms. If there is no direct match for an algorithm
     * name then we treat the name as an alias and resolve it
     * against the JCA registered Providers. Then we match the
     * algorithm name against the trusted ones again. If there
     * is still no match after that, the algorithm is dropped.
     *
     * @return The list of engines and algorithm names. Even
     *   indexes are engines and odd indexes are names. The
     *   names are the ones passed to this method such that
     *   clients can use their chosen names to refer to digests.
     */
    protected List resolveAlgorithmNames(String[] algs) {
        MessageDigest md;
        String alias;
        List list;
        int n;
        list = new ArrayList(4);
        for (n = algs.length - 1; n >= 0; n--) {
            md = (MessageDigest) trusted_.get(algs[n].toLowerCase());
            if (md == null) {
                alias = JCA.resolveAlias("MessageDigest", algs[n]);
                if (alias == null) {
                    continue;
                }
                alias = alias.toLowerCase();
                md = (MessageDigest) trusted_.get(alias);
                if (md == null) {
                    continue;
                }
                trusted_.put(algs[n].toLowerCase(), md);
            }
            list.add(md);
            list.add(algs[n]);
        }
        return list;
    }

    /**
     * Parses the input string into the names of the
     * digest algorithms and returns a sorted array
     * of the names.
     *
     * @param algs The string with the space, comma,
     *   colon or semicolon separated names of the
     *   digest algorithms to use.
     */
    public static String[] parseAlgorithms(String algs) {
        Set set;
        String[] md;
        StringTokenizer tok;
        tok = new StringTokenizer(algs, ",;: ");
        set = new HashSet();
        while (tok.hasMoreTokens()) {
            set.add(tok.nextToken().trim());
        }
        md = (String[]) set.toArray(new String[0]);
        Arrays.sort(md);
        return md;
    }
}
