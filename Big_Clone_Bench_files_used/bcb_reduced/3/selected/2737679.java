package de.fhg.igd.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import codec.Base64;
import codec.CorruptedCodeException;

/**
 * Verifies a <code>Manifest</code> against a <code>Resource
 * </code>. Instances of this class are initialized with a
 * <code>Resource</code> and support verification of multiple
 * <code>Manifest</code> instances against this <code>Resource
 * </code>. Pleasen note the <code>Manifest</code> implements
 * <code>Resource</code>. Hence, said feature in particular
 * allows to check on <code>Manifest</code> against a number
 * of signature files, which can also respresented by the
 * <code>Manifest</code> class.
 *
 * @author Volker Roth
 * @version "$Id: ManifestVerifier.java 1913 2007-08-08 02:41:53Z jpeters $"
 */
public class ManifestVerifier extends Object {

    /**
     * The <code>Manifest</code> that keeps the attributes
     * verified by this instance.
     */
    protected Manifest mf_;

    /**
     * The <code>Resource</code> against which a <code>
     * Manifest</code> is verified.
     */
    protected Resource source_;

    /**
     * The <code>Set</code> of names of Manifest sections
     * that failed verification.
     */
    protected Set failed_ = new HashSet();

    /**
     * The <code>Set</code> of names of Manifest sections
     * for which no data to digest was found.
     */
    protected Set missing_ = new HashSet();

    /**
     * The <code>Set</code> of names of <code>Resource</code>
     * files for which no Manifest section existed.
     */
    protected Set garbage_ = new HashSet();

    /**
     * Creates an instance. The given <code>Resource</code>
     * can be checked against some <code>Manifest</code>
     * instances. The effect of the <code>Manifest</code>
     * instances is cumulative and e.g. reduces the number
     * of entries assigned to the garbage set. On the
     * other hand, multiple verifications might increase
     * the set of failed entries. In principle, this is to
     * support verification of <code>Resource</code> instances
     * against multiple Manifests.<p>
     *
     * The entries in the <code>META-INF</code> directory
     * of the given <code>Resource</code> are ignored. This
     * is done by applying a <code>MetaInfFilter</code> to
     * the listing of files in the given <code>Resource</code>.
     *
     * @param source The <code>Resource</code> that is
     *   verified against some <code>Manifest</code>
     *   instances.
     * @exception IOException if the given <code>Resource</code>
     *   cannot be listed due to some I/O error.
     */
    public ManifestVerifier(Resource source) throws IOException {
        if (source == null) {
            throw new NullPointerException("Source");
        }
        source_ = source;
        garbage_.addAll(source.list(new MetaInfFilter()));
    }

    /**
     * Returns the set of names of Manifest sections that
     * failed the verification. A flawless verification is
     * signaled by means of an empty set.
     *
     * @return The <code>Set</code> of names of Manifest
     *   sections where a digest mismatch with the digested
     *   data is detected.
     */
    public Set getFailed() {
        return failed_;
    }

    /**
     * Returns the set of names of Manifest sections that
     * could not be verified because the corresponding data
     * was not found.
     *
     * @return The <code>Set</code> of names of Manifest
     *   sections for which no corresponding data could
     *   be found.
     */
    public Set getMissing() {
        return missing_;
    }

    /**
     * Returns the names of <code>Resource</code> files for
     * which no sections exist in the Manifest.
     *
     * @return The <code>Set</code> of names of files in the
     *   <code>Resource</code> (passed to the <code>init(..)
     *   </code> method) that have no corresponding Manifest
     *   section.
     */
    public Set getGarbage() {
        return garbage_;
    }

    /**
     * Verifies the digests stored in the attribute sections
     * of the given <code>Manifest</code> against the <code>
     * Resource</code> of this instance. All names in the
     * given <code>Manifest</code> are removed from the list
     * of &quot;garbage&quot; names, which initially contains
     * all names of files in the <code>Resource</code>.
     * The more Manifests (or signature files represented by
     * Manifests) are verified the greater the chance to reduce
     * the garbage set to the empty set. A non-empty garbage
     * set means that there are entries in the <code>Resource
     * </code> which are not covered by a signature/Manifest
     * entries.<p>
     *
     * Pleas bear in mind that <code>Manifest</code> is dual
     * use: for Manifest files as well as for SF (signature
     * files) used in Java Archives.<p>
     *
     * The threshold and trusted digest algorithms are taken
     * from the given <code>Manifest</code>.
     *
     * @param mf The <code>Manifest</code> that is verified
     *   against the <code>Resource</code> of this instance.
     * @exception NoSuchAlgorithmException if none of the
     *   trusted algorithms defined in the <code>Manifest
     *   </code> is installed locally.
     * @exception IllegalArgumentException if the threshold
     *   specified in the <code>Manifest</code> exceeds the
     *   number of installed trusted algorithms.
     */
    public void verify(Manifest mf) throws IOException, NoSuchAlgorithmException {
        InputStream in;
        Attributes attributes;
        Map.Entry entry;
        Digester digester;
        String[] algs;
        Iterator i;
        String name;
        String s;
        byte[] b;
        Map digests;
        int n;
        if (mf == null) {
            throw new NullPointerException("Manifest");
        }
        digests = new HashMap();
        digester = new Digester(mf.trusted_, mf.threshold_);
        garbage_.removeAll(mf.entries_.keySet());
        for (i = mf.entries_.entrySet().iterator(); i.hasNext(); ) {
            entry = (Map.Entry) i.next();
            attributes = (Attributes) entry.getValue();
            name = (String) entry.getKey();
            in = source_.getInputStream(name);
            if (in == null) {
                missing_.add(name);
                continue;
            }
            algs = Digester.parseAlgorithms(attributes.getDigestAlgorithms());
            try {
                digester.digest(algs, digests, in);
                for (n = algs.length - 1; n >= 0; n--) {
                    if (!digests.containsKey(algs[n])) {
                        continue;
                    }
                    s = attributes.get(algs[n] + "-Digest");
                    if (s == null) {
                        failed_.add(name);
                        break;
                    }
                    b = (byte[]) digests.get(algs[n]);
                    if (!Arrays.equals(b, Base64.decode(s))) {
                        failed_.add(name);
                        break;
                    }
                }
            } catch (DigestException e) {
                failed_.add(name);
            } catch (CorruptedCodeException e) {
                failed_.add(name);
            } finally {
                in.close();
            }
        }
    }
}
