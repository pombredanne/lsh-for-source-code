package DE.FhG.IGD.semoa.security;

import codec.asn1.*;
import codec.util.*;
import codec.pkcs7.*;
import codec.pkcs7.Signer;
import codec.x501.Attribute;
import codec.x501.BadNameException;
import DE.FhG.IGD.util.*;
import java.security.cert.*;
import java.security.*;
import java.util.*;
import java.io.*;

/**
 * This class provides the default signing capability for
 * signing SeMoA mobile agents. Signatures are written to
 * the agent structure resource passed to the constructor
 * of this class. Signatures consist of two files:
 * <dl>
 * <dt> <i>alias</i><code>.SF</code>
 * <dd> The <code>MANIFEST</code>-like file storing the
 *   digests of the corresponding <code>MANIFEST</code>
 *   file.
 * <dt> <i>alias</i><code>.(DSA|RSA|SIG)</code>
 * <dd> The actual signature file according to PKCS#7.
 *   The extension is guessed from the name of the signature
 *   engine passed for signing. If the algorithm cannot be
 *   determined then the <code>SIG</code> extension is used.
 * </dl>
 * Before any signatures can be computed, the agent's structure
 * must be <i>locked</i>. This means that the <code>MANIFEST
 * </code> is computed for the contents of the agent.
 * Signatures are computed on the <code>MANIFEST</code> and
 * not on the data itself. Locking is done by calling the
 * {@link #lock lock} method. After that, any changes to the
 * agent's data invalidates any signatures that are computed
 * on the entries of the modified data. The sign methods are
 * locking the structure if this has not already been done.
 * <p>
 * Two aliases are reserved for identifying roles in the
 * agent model. <code>OWNER</code> denotes the signature
 * of the entity that is assumed the rightful owner of the
 * agent. <code>SENDER</code> denotes the last sponsor of
 * the agent's state changes, in general the entity that
 * hosted and executed the agent last.<p>
 *
 * One instance may be used to compute an arbitrary number
 * of varying signatures on an agent's structure. Just call
 * the sign methods as desired after locking the agent's
 * structure.
 *
 * @author Volker Roth
 * @version "$Id: AgentSigner.java 462 2001-08-21 18:21:00Z vroth $"
 */
public class AgentSigner extends Object {

    /**
     * The size of the input buffer that is used to compute
     * implicit names of agents.
     */
    public static final int BUF_SIZE = 1024;

    /**
     * A bit mask where a set bit at position <i>n</i>
     * denotes a RSA signature algorithm OID of the form
     * 1.3.14.3.2.<i>n</i> is defined.
     */
    public static int ISO_SECSIG_RSA_MASK = (1 << 2) | (1 << 3) | (1 << 4) | (1 << 11) | (1 << 14) | (1 << 15) | (1 << 24) | (1 << 25) | (1 << 29);

    /**
     * A bit mask where a set bit at position <i>n</i>
     * denotes a DSA signature algorithm OID of the form
     * 1.3.14.3.2.<i>n</i> is defined.
     */
    public static int ISO_SECSIG_DSA_MASK = (1 << 20) | (1 << 21) | (1 << 27) | (1 << 28);

    /**
     * The default digest algorithms used for locking
     * and signing the structure of the agent. These
     * default algorithms are SHA1 and MD5.
     */
    public static final String DEFAULT_DIGESTS = "SHA,MD5";

    /**
     * The resource that holds the agent's structure.
     */
    protected Resource struct_;

    /**
     * The MANIFEST file of the structure is kept
     * here after the agent is locked.
     */
    protected Manifest mf_;

    /**
     * The preset MANIFEST file.
     */
    protected Manifest preset_;

    /**
     * The trusted digest algorithms to use for
     * computing digests.
     */
    protected String dAlgs_;

    /**
     * The extension thatwas used to store the &quot;OWNER&quot;
     * signature. This information is used when implicit names
     * are generated, in order to quickly find the name of the
     * file that holds the signature.
     */
    protected String ext_;

    /**
     * Creates an instance that signs the agent whose
     * structure is represented by the given Resource.
     * The Resource must be rooted at the directory in
     * which all the agent's data is kept. The default
     * digest algorithms are used.
     *
     * @param struct The structure of the agent.
     */
    public AgentSigner(Resource struct) {
        if (struct == null) {
            throw new NullPointerException("Need a Resource!");
        }
        struct_ = struct;
        dAlgs_ = DEFAULT_DIGESTS;
    }

    /**
     * Creates an instance that signs the agent whose
     * structure is represented by the given Resource.
     * The Resource must be rooted at the directory in
     * which all the agent's data is kept.
     *
     * @param struct The structure of the agent.
     * @param digestAlgs The message digest algorithms
     *   to use for locking and signing the structure.
     */
    public AgentSigner(Resource struct, String digestAlgs) {
        if (struct == null || digestAlgs == null) {
            throw new NullPointerException("Struct or algs!");
        }
        struct_ = struct;
        dAlgs_ = digestAlgs;
    }

    /**
     * Presets the Manifest to the given one. The entries in
     * the given Manifest will be amended with entries for
     * the files in the agent's <code>Resource</code> when
     * <code>lock()</code> is called.<p>
     *
     * Entries in the preset Manifest that refer to files in
     * the <code>Resource</code> of this instance will be
     * replaced.<p>
     *
     * Presetting a Manifest is useful for adding class entries
     * to the agent's Manifest even though the classes are not
     * included in the agent. The class entries will be signed
     * along with the static part of the agent. This allows to
     * check the integrity of classes loaded by an agent server
     * on-demand (e.g. over the network from a class server).
     *
     * @param manifest The <code>Manifest</code> with the
     *   precomputed entries that shall be signed along with
     *   the agent. Any entries that refer to <code>.class</code>
     *   files or start with the prefix of the agent's static
     *   part will be included in the agent owner's signature.
     */
    public void preset(Manifest manifest) {
        preset_ = manifest;
    }

    /**
     * Locks the agent. Before a signature can be computed,
     * the agent's MANIFEST file must be computed in order
     * to create digests of all the files in the agent.<p>
     *
     * The locking policy is as follows. If the structure is
     * already locked then the structure is digested again
     * using the <code>Manifest</code> that resulted from
     * the previous call as a basis. This should never happen
     * because this method is called only if the structure is
     * not yet locked. Consider the structure is not yet
     * locked. If no <code>Manifest</code> is preset but
     * the structure contains a <code>Manifest</code> then
     * this <code>Manifest</code> is loaded as the preset.
     *
     * What happens if there is a preset? After digesting
     * the structure, all entries in the resulting <code>
     * Manifest</code> that refer to files in the static
     * part of the agent are replaced by the corresponding
     * entries in the preset <code>Manifest</code>.<p>
     *
     * Why all this? Preset <code>Manifest</code> instances
     * allow to include digests of class files required by
     * the agent when the agent is created. However, the class
     * files need not be bundled with the agent, but can be
     * loaded on demand from class servers in the network. For
     * security reasons, the agent's owner has to authorize all
     * non-local classes to be used with the agent before
     * the agent is sent away. This is done by means of a
     * digital signature. We have to take care that no
     * entries in the <code>Manifest</code> are modified
     * which are signed by the agent's owner. Therefor,
     * the previous <code>Manifest</code> is used as a
     * preset when the agent migrates.<p>
     *
     * Is there even more to it? Yes, servers can in
     * principle delete class files from the agent, e.g.
     * when some LRU statistics stored in the agent's
     * structure says some class is hardly ever used.
     * This reduces the size of the agent dynamically,
     * depending on the actual usage pattern of each class.
     *
     * @exception IOException if guess what...
     * @exception NoSuchAlgorithmException if some digest
     *   algorithms are not available locally.
     */
    public void lock() throws IOException, NoSuchAlgorithmException {
        ManifestDigester digester;
        OutputStream out;
        InputStream in;
        Map.Entry entry;
        Iterator i;
        String name;
        List list;
        Map dest;
        Map src;
        if (mf_ != null) {
            return;
        }
        if (preset_ == null) {
            in = struct_.getInputStream(AgentStructure.MANIFEST);
            if (in != null) {
                try {
                    preset_ = new Manifest(dAlgs_, dAlgs_, 0);
                    preset_.load(in);
                } finally {
                    in.close();
                }
            }
        }
        mf_ = new Manifest(dAlgs_, dAlgs_, 0);
        digester = new ManifestDigester(mf_);
        digester.digest(struct_);
        if (preset_ != null) {
            src = preset_.entryMap();
            list = preset_.list(new StaticFilter());
            dest = mf_.entryMap();
            for (i = list.iterator(); i.hasNext(); ) {
                name = (String) i.next();
                dest.put(name, src.get(name));
            }
        }
        out = struct_.getOutputStream(AgentStructure.MANIFEST);
        if (out == null) {
            throw new IOException("Cannot get output stream for manifest!");
        }
        try {
            mf_.store(out);
        } finally {
            out.close();
        }
    }

    /**
     * Signs the agent and stores the signature under the
     * given name. The signature consists of two files. A
     * <code>SF</code> file and a <code>P7</code> file.
     * The <code>SF</code> file is a MANIFEST style file
     * with the hased entries of the agent's MANIFEST.
     * The <code>P7</code> file contains a PKCS#7 Signed
     * Data structure enveloped in a PKCS#7 ContentInfo.
     * <p>
     * If the agent's structure is not already locked then
     * it is locked by this method.
     *
     * @param key The private key to use for signing.
     * @param cert The certificate that matches the private
     *   key used for signing.
     * @param sigAlg The signature algorithm name.
     * @param alias The alias name to use for storing the
     *   signature files. This alias should be upper case
     *   and at most 8 characters long. However, these
     *   restrictions are <i>not</i> enforced by this
     *   method.
     * @exception IOException Guess what...
     * @exception GeneralSecurityException if something goes
     *   wrong with the creation of the signature or the
     *   corresponding structures.
     */
    public void sign(PrivateKey key, X509Certificate cert, String sigAlg, String alias) throws IOException, GeneralSecurityException {
        ManifestDigester digester;
        Manifest sf;
        try {
            if (mf_ == null) {
                lock();
            }
            sf = new Manifest(dAlgs_, dAlgs_, 0);
            digester = new ManifestDigester(sf);
            digester.digest(mf_);
            signSF(key, cert, sigAlg, alias, sf);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException("Digest or signature algorithm not available!");
        }
    }

    /**
     * Signs the given files in the agent and stores the
     * signature under the given name. The signature
     * consists of a <code>SF</code> file and a <code>P7
     * </code> file.<p>
     *
     * If the agent's structure is not already locked then
     * it is locked by this method.
     *
     * @param key The private key to use for signing.
     * @param cert The certificate that matches the private
     *   key used for signing.
     * @param sigAlg The signature algorithm name.
     * @param alias The alias name to use for storing the
     *   signature files. This alias should be upper case
     *   and at most 8 characters long. However, these
     *   restrictions are <i>not</i> enforced by this
     *   method.
     * @param names The names of the files in the agent
     *   that shall be signed. Names must be relative
     *   to the agent's main directory and separated with
     *   slashes ('/').
     * @exception IOException Guess what...
     * @exception SignatureException if something goes
     *   wrong with the creation of the signature or the
     *   corresponding structures.
     * @exception NoSuchAlgorithmException if well...
     */
    public void sign(PrivateKey key, X509Certificate cert, String sigAlg, String alias, Collection names) throws IOException, GeneralSecurityException {
        ManifestDigester digester;
        Manifest sf;
        if (mf_ == null) {
            lock();
        }
        sf = new Manifest(dAlgs_, dAlgs_, 0);
        digester = new ManifestDigester(sf);
        digester.digest(names, mf_);
        signSF(key, cert, sigAlg, alias, sf);
    }

    /**
     * This method signed the given <code>SF</code> instance
     * and stores it as well as the resulting <code>P7</code>
     * file in the agent's structure.
     *
     * @param key The private key to use for signing.
     * @param cert The certificate that matches the private
     *   key used for signing.
     * @param sigAlg The signature algorithm name.
     * @param alias The alias name to use for storing the
     *   signature files. This alias should be upper case
     *   and at most 8 characters long. However, these
     *   restrictions are <i>not</i> enforced by this
     *   method.
     * @param sf The <code>SF</code> instance.
     * @exception IOException Guess what...
     * @exception GeneralSecurityException if something goes
     *   wrong with the creation of the signature or the
     *   corresponding structures.
     */
    public void signSF(PrivateKey key, X509Certificate cert, String sigAlg, String alias, Manifest sf) throws IOException, GeneralSecurityException {
        ByteArrayOutputStream bos;
        OutputStream out;
        ContentInfo ci;
        DEREncoder enc;
        SignerInfo si;
        SignedData sd;
        Signer signer;
        String oid;
        String ext;
        byte[] b;
        try {
            bos = new ByteArrayOutputStream();
            sf.store(bos);
            b = bos.toByteArray();
            bos.close();
            si = new SignerInfo(cert, sigAlg);
            sd = new SignedData();
            si.addAuthenticatedAttribute(new Attribute(new ASN1ObjectIdentifier("1.2.840.113549.1.9.5"), new ASN1UTCTime()));
            sd.setDataContentType();
            signer = new Signer(sd, si, key);
            signer.update(b);
            signer.sign();
            sd.addCertificate(cert);
            out = struct_.getOutputStream(AgentStructure.META_INF + alias + ".SF");
            if (out == null) {
                throw new IOException("Cannot get output stream for SF file!");
            }
            try {
                out.write(b);
            } finally {
                out.close();
            }
            ci = new ContentInfo(sd);
            oid = JCA.getOID(sigAlg, "Signature");
            if (oid == null) {
                throw new SignatureException("Cannot determine OID for " + sigAlg);
            }
            ext = extension(oid);
            out = struct_.getOutputStream(AgentStructure.META_INF + alias + ext);
            if (out == null) {
                throw new IOException("Can't write \"" + alias + "\" signature");
            }
            if (AgentStructure.OWNER.equals(alias)) {
                ext_ = ext;
            }
            enc = new DEREncoder(out);
            try {
                ci.encode(enc);
            } finally {
                enc.close();
            }
            return;
        } catch (ASN1Exception e) {
            throw new SignatureException("Caught " + e.getClass().getName() + "(\"" + e.getMessage() + "\")");
        } catch (BadNameException e) {
            throw new SignatureException("Caught " + e.getClass().getName() + "(\"" + e.getMessage() + "\")");
        }
    }

    /**
     * This method signs all data in the agent's structure
     * that is stored in a subdirectory <code>static/</code>
     * with the given parameters. The alias is set to <code>
     * OWNER</code>.<p>
     *
     * In addition to the files in <code>static/</code>, the
     * file <code>SEAL-INF/INSTALL.MF</code> is signed if it
     * exists. This file holds the access policy of encrypted
     * parts of the agent's structure, which must be protected
     * against tampering.<p>
     *
     * Please note that this default behaviour may not fit
     * your needs. For instance, if some encrypted parts should
     * be static then this method is not what you want to call.
     * Use the sign method that takes a list of names instead,
     * and add to this list all the files you wish to be static.
     *
     * @param key The private key to use for signing.
     * @param cert The certificate that matches the private
     *   key used for signing.
     * @param sigAlg The signature algorithm name.
     * @exception IOException Guess what...
     * @exception GeneralSecurityException if something goes
     *   wrong with the creation of the signature or the
     *   corresponding structures.
     */
    public void signStaticData(PrivateKey key, X509Certificate cert, String sigAlg) throws IOException, GeneralSecurityException {
        List list;
        if (mf_ == null) {
            lock();
        }
        list = mf_.list(new StaticFilter());
        sign(key, cert, sigAlg, AgentStructure.OWNER, list);
    }

    /**
     * This method should be called only after having signed
     * the agent, because it locks the structure if it isn't
     * already. After locking, the structure cannot changed
     * anymore without breaking the integrity of signatures.
     *
     * @return A <code>Map</code> that contains the <code>
     *   Manifest</code> entries of the files in the agent's
     *   static part.
     * @exception IOException if an I/O error occurs while
     *   locking the structure.
     * @exception NoSuchAlgorithmException if one of the digest
     *   algorithms required for locking the structure is not
     *   installed.
     */
    public Map getStaticPart() throws IOException, NoSuchAlgorithmException {
        StaticFilter filter;
        Map.Entry entry;
        Iterator i;
        String name;
        Map map;
        if (mf_ == null) {
            lock();
        }
        filter = new StaticFilter();
        map = new HashMap();
        for (i = mf_.entryMap().entrySet().iterator(); i.hasNext(); ) {
            entry = (Map.Entry) i.next();
            name = (String) entry.getKey();
            if (filter.accept(name)) {
                map.put(name, entry.getValue());
            }
        }
        return map;
    }

    /**
     * This method can be called only on signed agents.
     *
     * @return The <i>implicit name</code> of the agent.
     *   The implicit name is computed as a SHA1 hash of
     *   the &quot;OWNER&quot; signature.
     * @exception IllegalStateException if the <code>
     *   Resource</code> of this <code>AgentSigner</code>
     *   does not contain an &quot;OWNER&quot; signature.
     * @exception NoSuchAlgorithmException if no engine
     *   for the SHA1 digest is installed.
     */
    public byte[] getImplicitName() throws IOException, NoSuchAlgorithmException {
        MessageDigest md;
        InputStream in;
        byte[] buf;
        int n;
        if (ext_ == null) {
            throw new IllegalStateException("The static part of the agent must be signed first!");
        }
        md = MessageDigest.getInstance(AgentStructure.IMPLICIT_NAME_ALG);
        buf = new byte[BUF_SIZE];
        in = struct_.getInputStream(AgentStructure.META_INF + AgentStructure.OWNER + ext_);
        if (in == null) {
            throw new IllegalStateException("Cannot find the OWNER signature!");
        }
        try {
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
            return md.digest();
        } finally {
            in.close();
        }
    }

    /**
     * This method calls {@link Resource#flush() flush} on
     * the Resource that stores the agent's structure. The
     * flush method should be called if the Resource is no
     * persistent storage but flushes to persistent storage.
     * If you are in doupt then call it anyway. It does not
     * hurt and you are on the safe side.
     *
     * @exception IOException guess when...
     */
    public void flush() throws IOException {
        struct_.flush();
    }

    public class StaticFilter extends Object implements ResourceFilter {

        public boolean accept(String name) {
            if (name.startsWith(AgentStructure.PATH_STATIC)) {
                return true;
            }
            if (name.equals(AgentStructure.INSTALL_MF)) {
                return true;
            }
            if (name.endsWith(".class")) {
                return true;
            }
            return false;
        }
    }

    /**
     * Returns the correct extension to use for SF (Signature Files)
     * for signatures identified by the given OID. This method knows
     * about the OIDs of RSA and DSA based algorithms as given below:
     * <ul>
     * <li> 1.2.840.113549.1.1.<i>n</i> (PKCS-1)
     * <li> 1.2.840.10040.4.3 (ISO, member-body, us)
     * <li> 1.3.14.3.2.<i>n</i> (ISO, identified-organisation)
     * </ul>
     * If a given OID is not known then &quot;SIG&quot; is returned.
     * 
     * @return The extension to use for the given OID when
     *   saving SF files to JARs.
     * @exception NumberFormatException if the given OID has a
     *   bad number format.
     */
    public static String extension(String oid) throws NumberFormatException {
        String s;
        int n;
        if (oid.startsWith("1.2.840.113549.1.1.")) {
            return ".RSA";
        }
        if (oid.equals("1.2.840.10040.4.3")) {
            return ".DSA";
        }
        if (oid.startsWith("1.3.14.3.2.")) {
            s = oid.substring(11);
            n = Integer.parseInt(s);
            if ((n < 0) || (n > 29)) {
                return ".SIG";
            }
            n = (1 << n);
            if ((n & ISO_SECSIG_RSA_MASK) != 0) {
                return ".RSA";
            }
            if ((n & ISO_SECSIG_DSA_MASK) != 0) {
                return ".DSA";
            }
        }
        return ".SIG";
    }
}
