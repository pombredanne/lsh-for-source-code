package com.croftsoft.core.util.cache.secure;

import java.io.*;
import java.security.*;
import java.util.*;
import com.croftsoft.core.util.SoftHashMap;
import com.croftsoft.core.util.cache.Cache;
import com.croftsoft.core.util.cache.ContentAccessor;
import com.croftsoft.core.util.cache.WeakCache;
import com.croftsoft.core.util.id.Id;

/*********************************************************************
     * Cache implementation that securely stores and retrieves content
     * using digests.
     *
     * <P>
     *
     * Applications where a SecureCache may be useful include when
     *
     * <UL>
     *
     * <LI> it is desired to be able to retrieve content from the cache
     *      even though originally copied and stored from an unknown and
     *      potentially untrusted source, so long as the content is
     *      bit-for-bit identical to what is currently required;
     *
     * <P>
     *
     * <LI> multiple entities want to share the same cache without the need
     *      to worry that the activities of another, potentially untrusted,
     *      entity might modify the stored content to be something other
     *      than what is expected;
     *
     * <P>
     *
     * <LI> the cache is stored on an insecure medium which may
     *      be accessed directly and corrupted by another entity; and
     *
     * <P>
     *
     * <LI> it is desired to detect changes to content stored elsewhere
     *      by comparing the digests and then, if necessary, updating the
     *      copy in the cache to match.
     *
     * </UL>
     *
     * <P>
     *
     * The security is based upon the use of cryptographically secure
     * content digests which have the property that it is computationally
     * infeasible to find two non-identical instances of content that
     * generate digests with the same value.
     *
     * <P>
     *
     * The SecureCache wraps around a delegate Cache, which itself may
     * or may not be secure.  Multiple digest algorithms may be
     * simultaneously used by "chaining" SecureCache objects.
     *
     * <P>
     *
     * Note further that an insecure implementation of the Cache interface
     * which validates content using the more traditional mechanisms, e.g.,
     * comparing values of content length, freshness date, version
     * number, semi-unique identifier, etc., may be secured by using a
     * SecureCache as a delegate.  For example, an insecure Cache that
     * validates content using URL or path/filename, content length,
     * and/or last modified date may map an Id object storing those values
     * to a SecureId object to be passed to a delegate SecureCache.
     *
     * <P>
     *
     * The Serializable interface is implemented to support persistence.
     * To use this feature, the delegateCache and delegateIdMap must also
     * be Serializable.
     *
     * <B>
     * Reference
     * </B>
     *
     * <P>
     &
     * Sun Microsystems, "Java Cryptography Architecture API Specification
     *   & Reference",
     * <A HREF="http://www.javasoft.com/products/jdk/1.2/docs/guide/security/CryptoSpec.html#MessageDigest">
     * "Message Digest"</A>, 1998-10-30.
     *
     * <P>
     *
     * @see
     *   java.security.MessageDigest
     * @see
     *   SecureId
     * @see
     *   com.orbs.open.a.mpl.util.id.Id
     * @see
     *   com.orbs.open.a.mpl.util.cache.Cache
     * @see
     *   com.orbs.open.a.mpl.util.cache.ContentAccessor
     * @see
     *   com.orbs.open.a.mpl.util.cache.WeakCache
     * @see
     *   com.orbs.open.a.mpl.util.SoftHashMap
     *
     * @version
     *   1999-04-20
     * @author
     *   <a href="http://www.CroftSoft.com/">David Wallace Croft</a>
     *********************************************************************/
public final class SecureCache implements Cache, Serializable {

    private final Cache delegateCache;

    private final Map delegateIdMap;

    private final String algorithm;

    private final boolean doVerification;

    /*********************************************************************
     *
     * Creates a new SecureCache that stores content to a delegate Cache.
     *
     * @param  delegateCache
     *
     *   Where the content is actually relayed to and from.  This may be
     *   an insecure Cache or another chained SecureCache.
     *
     * @param  delegateIdMap
     *
     *   A Map of delegateCache Id objects keyed by SecureId objects
     *   generated by this SecureCache.
     *
     * @param  algorithm
     *
     *   The secure message digest algorithm to use.
     *
     * @param  doVerification
     *
     *   If true, the content digest will be recalculated upon retrieval
     *   to confirm that it has not been altered while in the
     *   delegateCache.  This is especially recommended if the
     *   delegateCache is stored on an insecure medium such as a disk
     *   drive.
     *
     *   <P>
     *
     *   If false, it is up to the caller to verify that the digest of the
     *   returned content is the same.  If the delegateCache medium is
     *   reasonably secure, such as a shared memory area with
     *   exclusive access only through this SecureCache, the caller may
     *   be reasonably secure without needing to take the extra
     *   verification step.
     *
     *   <P>
     *
     *   Note that the doVerification parameter only applies to retrieval;
     *   the digest is always calculated from the content during storage.
     *
     *********************************************************************/
    public SecureCache(Cache delegateCache, Map delegateIdMap, String algorithm, boolean doVerification) throws NoSuchAlgorithmException {
        this.delegateCache = delegateCache;
        this.delegateIdMap = delegateIdMap;
        this.algorithm = algorithm;
        this.doVerification = doVerification;
        MessageDigest.getInstance(algorithm);
    }

    /*********************************************************************
     *
     * This example zero argument constructor caches the content in memory,
     * dumps the content when memory runs low, identifies the content
     * using the NIST Secure Hash Algorithm (SHA), and verifies the content
     * digest upon retrieval.
     *
     * <PRE>
     *
     * this ( new WeakCache ( ), new SoftHashMap ( ), "SHA", true );
     *
     * </PRE>
     *
     *********************************************************************/
    public SecureCache() throws NoSuchAlgorithmException {
        this(new WeakCache(), new SoftHashMap(), "SHA", true);
    }

    /*********************************************************************
     *
     * "Validates" the content by
     *
     * <OL>
     *
     * <LI> confirming that identical content already exists in the cache;
     *      or, if otherwise necessary,
     *
     * <LI> storing a new copy of the content in the cache.
     *
     * </OL>
     *
     * @param  secureId
     *
     *   The content identifier passed to isAvailable() to determine if
     *   the content is already valid.  The parameter may be any SecureId
     *   with potentially matching algorithm and digest values.
     *
     * @param  contentAccessor
     *
     *   An object capable of making content accessible via an InputStream.
     *   For example, a ContentAccessor might retrieve content from a
     *   website via a URL, a database or file storage, a remote object
     *   such as another cache, or even dynamically generate the content
     *   upon demand.  As yet another possibility, a ContentAccessor object
     *   may potentially attempt to access the content from several
     *   different sources sequentially until it is successful.
     *
     * @return
     *
     *   Returns a SecureId object for the validated content which may be
     *   used later for retrieval.
     *
     *   <P>
     *
     *   If valid content was already available in the cache, the returned
     *   SecureId object will be the secureId parameter.
     *
     *   <P>
     *
     *   If valid content was not already available and the content could
     *   not be accessed and stored via the contentAccessor, the returned
     *   value will be null.
     *
     *   <P>
     *
     *   If valid content was not already available and the content could
     *   be accessed and stored via the contentAccessor, the returned
     *   value will be a new SecureId object with a digest that may or may
     *   not match that of the secureId object parameter, depending on
     *   the actual content available via the contentAccessor.
     *
     *********************************************************************/
    public Id validate(Id secureId, ContentAccessor contentAccessor) throws IOException {
        if (isAvailable(secureId)) return secureId;
        InputStream inputStream = contentAccessor.getInputStream();
        if (inputStream == null) return null;
        return store(inputStream);
    }

    /*********************************************************************
     *
     * Stores the content to the delegateCache and returns a SecureId
     * object which may be used to retrieve it.
     *
     * @param  inputStream
     *
     *   Any finite ordered sequence of bits.  The inputStream will be
     *   read until completion by the delegateCache before return.
     *
     * @return
     *
     *   Returns a SecureId object with a digest calculated from the
     *   content inputStream.
     *
     *********************************************************************/
    public Id store(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("null inputStream");
        }
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
        DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest);
        Id insecureId = delegateCache.store(digestInputStream);
        SecureId secureId = new SecureId(algorithm, messageDigest.digest());
        delegateIdMap.put(secureId, insecureId);
        return secureId;
    }

    /*********************************************************************
     *
     * Retrieves content with a matching content digest from the
     * delegateCache.
     *
     * <P>
     *
     * If the SecureCache instance variable doVerification is set to true,
     * the digest will be recalculated via a SecureInputStream while the
     * content is being read from the delegateCache.  When the last byte is
     * read, the newly calculated digest will be compared to that of the
     * SecureId parameter object.  If the digests differ, an IOException
     * will be thrown to prevent use of the corrupted content.
     *
     * @param  secureId
     *
     *   Any SecureId object with a potentially matching content digest.
     *
     * @return
     *
     *   Returns null if the content was not or is no longer available.
     *
     *********************************************************************/
    public InputStream retrieve(Id secureId) throws IOException {
        if (secureId == null) {
            throw new IllegalArgumentException("null secureId");
        }
        Id insecureId = (Id) delegateIdMap.get(secureId);
        if (insecureId == null) return null;
        InputStream inputStream = delegateCache.retrieve(insecureId);
        if (inputStream == null) return null;
        if (doVerification) {
            try {
                return new SecureInputStream(inputStream, algorithm, ((SecureId) secureId).getDigest());
            } catch (NoSuchAlgorithmException ex) {
                return null;
            }
        } else {
            return inputStream;
        }
    }

    /*********************************************************************
     *
     * Determines if the content with a matching digest is available in
     * the delegateCache.
     *
     * @return
     *
     *   Returns false if the content was not or is no longer available.
     *
     *********************************************************************/
    public boolean isAvailable(Id secureId) {
        if (secureId == null) return false;
        Id insecureId = (Id) delegateIdMap.get(secureId);
        if (insecureId == null) return false;
        return delegateCache.isAvailable(insecureId);
    }
}