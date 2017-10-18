package org.ccnx.ccn.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.logging.Level;
import org.ccnx.ccn.ContentVerifier;
import org.ccnx.ccn.KeyManager;
import org.ccnx.ccn.config.SystemConfiguration;
import org.ccnx.ccn.impl.encoding.BinaryXMLCodec;
import org.ccnx.ccn.impl.encoding.CCNProtocolDTags;
import org.ccnx.ccn.impl.encoding.GenericXMLEncodable;
import org.ccnx.ccn.impl.encoding.XMLCodecFactory;
import org.ccnx.ccn.impl.encoding.XMLDecoder;
import org.ccnx.ccn.impl.encoding.XMLEncodable;
import org.ccnx.ccn.impl.encoding.XMLEncoder;
import org.ccnx.ccn.impl.security.crypto.CCNDigestHelper;
import org.ccnx.ccn.impl.security.crypto.CCNSignatureHelper;
import org.ccnx.ccn.impl.support.DataUtils;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.NullOutputStream;
import org.ccnx.ccn.io.content.ContentDecodingException;
import org.ccnx.ccn.io.content.ContentEncodingException;
import org.ccnx.ccn.protocol.SignedInfo.ContentType;

/**
 * Represents a CCNx data packet.
 * cf. Interest
 */
public class ContentObject extends GenericXMLEncodable implements XMLEncodable, Comparable<ContentObject> {

    public static boolean DEBUG_SIGNING = false;

    protected ContentName _name;

    protected SignedInfo _signedInfo;

    protected byte[] _content;

    /**
	 * Cache of the complete ContentObject's digest. Set when first calculated.
	 * Used as the implicit last name component.
	 */
    protected byte[] _digest = null;

    protected Signature _signature;

    /**
	 * We don't specify a required publisher, and right now we don't enforce
	 * that publisherID is the digest of the key used to sign (which could actually
	 * be handy to preserve privacy); we just use the key locator and publisherID
	 * combined to look up keys in caches (though for right now we only put keys in
	 * caches by straight digest; would have to offer option to put keys in caches
	 * using some privacy-preserving function as well..
	 * 
	 * TODO evaluate when the gap between checking verifier and checking
	 * publisherID matters. Probably does; could have bogus publisherID, and
	 * then real key locator and content that uses key locator would then verify
	 * content and user might rely on publisher ID. Make that an option, though,
	 * even if it costs more time to check.
	 */
    public static class SimpleVerifier implements ContentVerifier {

        public static SimpleVerifier _defaultVerifier = null;

        PublisherPublicKeyDigest _requiredPublisher;

        KeyManager _keyManager;

        public static ContentVerifier getDefaultVerifier() {
            if (null == _defaultVerifier) {
                synchronized (SimpleVerifier.class) {
                    if (null == _defaultVerifier) {
                        _defaultVerifier = new SimpleVerifier(null);
                    }
                }
            }
            return _defaultVerifier;
        }

        public SimpleVerifier(PublisherPublicKeyDigest requiredPublisher) {
            _requiredPublisher = requiredPublisher;
            _keyManager = KeyManager.getDefaultKeyManager();
        }

        public SimpleVerifier(PublisherPublicKeyDigest publisher, KeyManager keyManager) {
            _requiredPublisher = publisher;
            _keyManager = (null != keyManager) ? keyManager : KeyManager.getDefaultKeyManager();
        }

        public boolean verify(ContentObject object) {
            if (null == object) return false;
            if (null != _requiredPublisher) {
                if (!_requiredPublisher.equals(object.signedInfo().getPublisherKeyID())) return false;
            }
            try {
                return object.verify(_keyManager);
            } catch (Exception e) {
                if (Log.isLoggable(Level.FINE)) {
                    Log.fine(e.getClass().getName() + " exception attempting to retrieve public key with key locator {0}: " + e.getMessage(), object.signedInfo().getKeyLocator());
                    Log.logStackTrace(Level.FINE, e);
                }
                return false;
            }
        }
    }

    /**
	 * We copy the content when we get it. The intent is for this object to
	 * be immutable.
	 * @param digestAlgorithm
	 * @param name
	 * @param signedInfo
	 * @param content
	 * @param signature already immutable
	 */
    public ContentObject(String digestAlgorithm, ContentName name, SignedInfo signedInfo, byte[] content, Signature signature) {
        this(name, signedInfo, content, 0, ((null == content) ? 0 : content.length), signature);
    }

    public ContentObject(String digestAlgorithm, ContentName name, SignedInfo signedInfo, byte[] content, int offset, int length, Signature signature) {
        _name = name;
        _signedInfo = signedInfo;
        _content = new byte[length];
        if (null != content) System.arraycopy(content, offset, _content, 0, length);
        _signature = signature;
        if ((null != signature) && Log.isLoggable(Log.FAC_SIGNING, Level.FINEST)) {
            try {
                byte[] digest = CCNDigestHelper.digest(this.encode());
                byte[] tbsdigest = CCNDigestHelper.digest(prepareContent(name, signedInfo, content, offset, length));
                if (Log.isLoggable(Level.INFO)) {
                    Log.info("Created content object: " + name + " timestamp: " + signedInfo.getTimestamp() + " encoded digest: " + DataUtils.printBytes(digest) + " tbs content: " + DataUtils.printBytes(tbsdigest));
                    Log.info("Signature: " + this.signature());
                }
            } catch (Exception e) {
                if (Log.isLoggable(Level.WARNING)) {
                    Log.warning("Exception attempting to verify signature: " + e.getClass().getName() + ": " + e.getMessage());
                    Log.warningStackTrace(e);
                }
            }
        }
    }

    /**
	 * Minimum-copy constructor.
	 * @param digestAlgorithm
	 * @param name
	 * @param signedInfo
	 * @param contentStream a stream from which to read a block of content
	 * @param length number of bytes to try to read; will size content to this
	 * 		or to the number of bytes left in the stream, whichever is smaller. 
	 * DKS TODO -- need timeout?
	 * 
	 * Set signature with setSignature or sign once it's constructed.
	 * @throws IOException  if no bytes left in stream
	 */
    public ContentObject(String digestAlgorithm, ContentName name, SignedInfo signedInfo, InputStream contentStream, int length) throws IOException {
        _name = name;
        _signedInfo = signedInfo;
        _content = new byte[length];
        int count = contentStream.read(_content);
        if (count < _content.length) {
            if (count < 0) {
                throw new IOException("End of stream reached when building content object!");
            } else {
                byte[] newContent = new byte[count];
                System.arraycopy(_content, 0, newContent, 0, count);
                _content = newContent;
            }
        }
    }

    public ContentObject(ContentName name, SignedInfo signedInfo, InputStream contentStream, int length) throws IOException {
        this(CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM, name, signedInfo, contentStream, length);
    }

    public ContentObject(ContentName name, SignedInfo signedInfo, byte[] content, Signature signature) {
        this(CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM, name, signedInfo, content, signature);
    }

    public ContentObject(ContentName name, SignedInfo signedInfo, byte[] content, int offset, int length, Signature signature) {
        this(CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM, name, signedInfo, content, offset, length, signature);
    }

    /**
	 * Generate a signedInfo and a signature.
	 * @throws SignatureException 
	 * @throws InvalidKeyException 
	 */
    public ContentObject(ContentName name, SignedInfo signedInfo, byte[] content, int offset, int length, PrivateKey signingKey) throws InvalidKeyException, SignatureException {
        this(name, signedInfo, content, offset, length, (Signature) null);
        setSignature(sign(_name, _signedInfo, _content, 0, _content.length, signingKey));
    }

    public ContentObject(ContentName name, SignedInfo signedInfo, byte[] content, PrivateKey signingKey) throws InvalidKeyException, SignatureException {
        this(name, signedInfo, content, 0, ((null == content) ? 0 : content.length), signingKey);
    }

    public static ContentObject buildContentObject(ContentName name, ContentType type, byte[] contents, PublisherPublicKeyDigest publisher, KeyLocator locator, KeyManager keyManager, Integer freshnessSeconds, byte[] finalBlockID) {
        try {
            if (null == keyManager) {
                keyManager = KeyManager.getDefaultKeyManager();
            }
            PrivateKey signingKey = keyManager.getSigningKey(publisher);
            if ((null == publisher) || (null == signingKey)) {
                signingKey = keyManager.getDefaultSigningKey();
                publisher = keyManager.getPublisherKeyID(signingKey);
            }
            if (null == locator) locator = keyManager.getKeyLocator(signingKey);
            return new ContentObject(name, new SignedInfo(publisher, null, type, locator, freshnessSeconds, finalBlockID), contents, signingKey);
        } catch (Exception e) {
            Log.warning("Cannot build content object for publisher: {0}", publisher);
            Log.infoStackTrace(e);
        }
        return null;
    }

    public static ContentObject buildContentObject(ContentName name, ContentType type, byte[] contents, PublisherPublicKeyDigest publisher, KeyLocator locator, KeyManager keyManager, byte[] finalBlockID) {
        return buildContentObject(name, type, contents, publisher, locator, keyManager, null, finalBlockID);
    }

    public static ContentObject buildContentObject(ContentName name, ContentType type, byte[] contents, PublisherPublicKeyDigest publisher, KeyManager keyManager, byte[] finalBlockID) {
        return buildContentObject(name, type, contents, publisher, null, keyManager, finalBlockID);
    }

    public static ContentObject buildContentObject(ContentName name, byte[] contents, PublisherPublicKeyDigest publisher, KeyManager keyManager, byte[] finalBlockID) {
        return buildContentObject(name, ContentType.DATA, contents, publisher, keyManager, finalBlockID);
    }

    public static ContentObject buildContentObject(ContentName name, byte[] contents) {
        return buildContentObject(name, contents, null, null, null);
    }

    public static ContentObject buildContentObject(ContentName name, ContentType type, byte[] contents) {
        return buildContentObject(name, type, contents, null, null, null);
    }

    public static ContentObject buildContentObject(ContentName name, byte[] contents, PublisherPublicKeyDigest publisher) {
        return buildContentObject(name, contents, publisher, null, null);
    }

    public ContentObject() {
    }

    public ContentObject clone() {
        return new ContentObject(_name.clone(), _signedInfo, _content, _signature);
    }

    /**
	 * DKS -- return these as final for now; stopgap till refactor that makes
	 * internal version final.
	 * @return Name of the content object - without the final implicit digest component.
	 */
    public final ContentName name() {
        return _name;
    }

    /**
	 * @return Name of the content object, complete with the final implicit digest component.
	 */
    public ContentName fullName() {
        return new ContentName(_name, digest());
    }

    public final SignedInfo signedInfo() {
        return _signedInfo;
    }

    /**
	 * Final here doesn't really make it immutable. There have been
	 * proposals to clone() the content on return, but many places use this
	 * and it would be expensive.
	 * @return
	 */
    public final byte[] content() {
        return _content;
    }

    /**
	 * Avoid problems where content().length might be expensive.
	 * @return content length in bytes
	 */
    public final int contentLength() {
        return ((null == _content) ? 0 : _content.length);
    }

    public final Signature signature() {
        return _signature;
    }

    /**
	 * Used by NetworkObject to decode the object from a network stream.
	 * @see org.ccnx.ccn.impl.encoding.XMLEncodable
	 */
    public void decode(XMLDecoder decoder) throws ContentDecodingException {
        decoder.readStartElement(getElementLabel());
        _signature = new Signature();
        _signature.decode(decoder);
        _name = new ContentName();
        _name.decode(decoder);
        _signedInfo = new SignedInfo();
        _signedInfo.decode(decoder);
        _content = decoder.readBinaryElement(CCNProtocolDTags.Content);
        decoder.readEndElement();
    }

    /**
	 * Used by NetworkObject to encode the object to a network stream.
	 * @see org.ccnx.ccn.impl.encoding.XMLEncodable
	 */
    public void encode(XMLEncoder encoder) throws ContentEncodingException {
        if (!validate()) {
            throw new ContentEncodingException("Cannot encode " + this.getClass().getName() + ": field values missing.");
        }
        encoder.writeStartElement(getElementLabel());
        signature().encode(encoder);
        name().encode(encoder);
        signedInfo().encode(encoder);
        encoder.writeElement(CCNProtocolDTags.Content, _content);
        encoder.writeEndElement();
    }

    @Override
    public long getElementLabel() {
        return CCNProtocolDTags.ContentObject;
    }

    @Override
    public boolean validate() {
        return ((null != name()) && (null != signedInfo()) && (null != signature()));
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((_name == null) ? 0 : _name.hashCode());
        result = PRIME * result + ((_signedInfo == null) ? 0 : _signedInfo.hashCode());
        result = PRIME * result + ((_signature == null) ? 0 : _signature.hashCode());
        result = PRIME * result + Arrays.hashCode(_content);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final ContentObject other = (ContentObject) obj;
        if (_name == null) {
            if (other.name() != null) return false;
        } else if (!_name.equals(other.name())) return false;
        if (_signedInfo == null) {
            if (other.signedInfo() != null) return false;
        } else if (!_signedInfo.equals(other.signedInfo())) return false;
        if (_signature == null) {
            if (other.signature() != null) return false;
        } else if (!_signature.equals(other.signature())) return false;
        if (!Arrays.equals(_content, other._content)) return false;
        return true;
    }

    /**
	 * External function to set signature if generating it some special way
	 * (e.g. with a bulk signer).
	 * @param signature
	 */
    public void setSignature(Signature signature) {
        if (null != _signature) {
            if (Log.isLoggable(Log.FAC_SIGNING, Level.FINE)) Log.fine(Log.FAC_SIGNING, "Setting signature on content object: " + name() + " after signature already set!");
        }
        if (null == signature) {
            if (Log.isLoggable(Log.FAC_SIGNING, Level.FINE)) Log.fine(Log.FAC_SIGNING, "Setting signature to null on content object: " + name());
        }
        _signature = signature;
    }

    public void sign(PrivateKey signingKey) throws InvalidKeyException, SignatureException {
        setSignature(sign(this.name(), this.signedInfo(), this._content, 0, this._content.length, signingKey));
    }

    public void sign(String digestAlgorithm, PrivateKey signingKey) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        setSignature(sign(this.name(), this.signedInfo(), this._content, 0, this._content.length, digestAlgorithm, signingKey));
    }

    public static Signature sign(ContentName name, SignedInfo signedInfo, byte[] content, int offset, int length, PrivateKey signingKey) throws SignatureException, InvalidKeyException {
        try {
            return sign(name, signedInfo, content, offset, length, CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM, signingKey);
        } catch (NoSuchAlgorithmException e) {
            if (Log.isLoggable(Level.WARNING)) Log.warning("Cannot find default digest algorithm: " + CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM);
            Log.warningStackTrace(e);
            throw new SignatureException(e);
        }
    }

    /**
	 * Generate a signature on a name-content mapping. This
	 * signature is specific to both this content signedInfo
	 * and this name. The SignedInfo no longer contains
	 * a proxy for the content, so we sign the content itself
	 * directly.  This is used with simple algorithms that don't
	 * generate a witness.
	 * @throws SignatureException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
    public static Signature sign(ContentName name, SignedInfo signedInfo, byte[] content, int offset, int length, String digestAlgorithm, PrivateKey signingKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        byte[] signature = null;
        try {
            byte[] toBeSigned = prepareContent(name, signedInfo, content, offset, length);
            signature = CCNSignatureHelper.sign(digestAlgorithm, toBeSigned, signingKey);
        } catch (ContentEncodingException e) {
            Log.logException("Exception encoding internally-generated XML name!", e);
            throw new SignatureException(e);
        }
        return new Signature(digestAlgorithm, null, signature);
    }

    /**
	 * @see ContentObject#verify(ContentObject, PublicKey)
	 */
    public boolean verify(PublicKey publicKey) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, ContentEncodingException {
        return verify(this, publicKey);
    }

    public boolean verify(KeyManager keyManager) throws SignatureException, NoSuchAlgorithmException, ContentEncodingException, InvalidKeyException {
        return verify(this, keyManager);
    }

    /**
	 * Want to verify a content object. First compute the 
	 * witness result (e.g. Merkle path root, or possibly content
	 * proxy), and make it available to the caller if caller just
	 * needs to check whether it matches a previous round. Then
	 * verify the actual signature.
	 * 
	 * @param verifySignature If we have a collection of blocks
	 * 	 all authenticated by the public key signature, we may
	 * 	 only need to verify that signature once. If verifySignature
	 *   is true, we do that work. If it is false, we simply verify
	 *   that this piece of content matches that signature; assuming 
	 *   that the caller has already verified that signature. If you're
	 *   not sure what all this means, you shouldn't be calling this
	 *   one; use the simple verify above.
	 * @param publicKey If the caller already knows a public key
	 *   that should be used to verify the signature, they can
	 *   pass it in. Otherwise, the key locator in the object
	 *   will be used to find the key.
	 * @throws SignatureException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
    public static boolean verify(ContentObject object, PublicKey publicKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, ContentEncodingException {
        if (null == publicKey) {
            throw new SignatureException("Cannot verify object without public key -- public key cannot be null!");
        }
        byte[] contentProxy = null;
        try {
            contentProxy = object.computeProxy();
        } catch (CertificateEncodingException e) {
            if (Log.isLoggable(Level.INFO)) Log.info("Encoding exception attempting to verify content digest for object: " + object.name() + ". Signature verification fails.");
            return false;
        }
        boolean result;
        if (null != contentProxy) {
            result = CCNSignatureHelper.verify(contentProxy, object.signature().signature(), object.signature().digestAlgorithm(), publicKey);
        } else {
            result = verify(object.name(), object.signedInfo(), object.content(), object.signature(), publicKey);
        }
        if ((!result) && Log.isLoggable(Log.FAC_VERIFY, Level.WARNING)) {
            Log.info("VERIFICATION FAILURE: " + object.name() + " timestamp: " + object.signedInfo().getTimestamp() + " content length: " + object.contentLength() + " ephemeral digest: " + DataUtils.printBytes(object.digest()) + " to be signed sha256 digest: " + DataUtils.printHexBytes(CCNDigestHelper.digest(object.prepareContent())));
            SystemConfiguration.outputDebugObject(object);
        }
        return result;
    }

    public static boolean verify(ContentObject object, KeyManager keyManager) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, ContentEncodingException {
        try {
            if (null == keyManager) keyManager = KeyManager.getDefaultKeyManager();
            PublicKey publicKey = keyManager.getPublicKey(object.signedInfo().getPublisherKeyID(), object.signedInfo().getKeyLocator());
            if (null == publicKey) {
                throw new SignatureException("Cannot obtain public key to verify object. Publisher: " + object.signedInfo().getPublisherKeyID() + " Key locator: " + object.signedInfo().getKeyLocator());
            }
            return verify(object, publicKey);
        } catch (IOException e) {
            throw new SignatureException("Cannot obtain public key to verify object. Key locator: " + object.signedInfo().getKeyLocator() + " exception: " + e.getMessage(), e);
        }
    }

    /**
	 * Verify the public key signature on a content object.
	 * Does not verify that the content matches the signature,
	 * merely that the signature over the name and content
	 * signedInfo is correct and was performed with the
	 * indicated public key.
	 * @param contentProxy the proxy for the content that was signed. This could
	 * 	be the content itself, a digest of the content, or the root of a Merkle hash tree.
	 * @return
	 * @throws SignatureException 
	 * @throws NoSuchAlgorithmException 
	 * @throws ContentEncodingException
	 * @throws InvalidKeyException 
	 */
    public static boolean verify(ContentName name, SignedInfo signedInfo, byte[] content, Signature signature, PublicKey publicKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, ContentEncodingException {
        if (null == publicKey) {
            throw new SignatureException("Cannot verify object without public key -- public key cannot be null!");
        }
        byte[] preparedContent = prepareContent(name, signedInfo, content);
        boolean result = CCNSignatureHelper.verify(preparedContent, signature.signature(), (signature.digestAlgorithm() == null) ? CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM : signature.digestAlgorithm(), publicKey);
        return result;
    }

    public static boolean verify(byte[] proxy, byte[] signature, SignedInfo signedInfo, String digestAlgorithm, PublicKey publicKey) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        if (null == publicKey) {
            throw new SignatureException("Cannot verify object without public key -- public key cannot be null!");
        }
        boolean result = CCNSignatureHelper.verify(proxy, signature, (digestAlgorithm == null) ? CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM : digestAlgorithm, publicKey);
        return result;
    }

    public static boolean verify(byte[] proxy, byte[] signature, SignedInfo signedInfo, String digestAlgorithm, KeyManager keyManager) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        try {
            if (null == keyManager) keyManager = KeyManager.getDefaultKeyManager();
            PublicKey publicKey = keyManager.getPublicKey(signedInfo.getPublisherKeyID(), signedInfo.getKeyLocator());
            if (null == publicKey) {
                throw new SignatureException("Cannot obtain public key to verify object. Publisher: " + signedInfo.getPublisherKeyID() + " Key locator: " + signedInfo.getKeyLocator());
            }
            return verify(proxy, signature, signedInfo, digestAlgorithm, publicKey);
        } catch (IOException e) {
            throw new SignatureException("IOException attempting to  obtain public key to verify object. Key locator: " + signedInfo.getKeyLocator() + " exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    public byte[] computeProxy() throws CertificateEncodingException, ContentEncodingException {
        if (null == content()) return null;
        if ((null == signature()) || (null == signature().witness())) {
            return null;
        }
        byte[] blockDigest = CCNDigestHelper.digest(prepareContent(name(), signedInfo(), content()));
        return signature().computeProxy(blockDigest, true);
    }

    public byte[] prepareContent() throws ContentEncodingException {
        return prepareContent(name(), signedInfo(), content());
    }

    public static byte[] prepareContent(ContentName name, SignedInfo signedInfo, byte[] content) throws ContentEncodingException {
        return prepareContent(name, signedInfo, content, 0, ((null == content) ? 0 : content.length));
    }

    /**
	 * Prepare digest for signature.
	 * DKS TODO -- limit extra copies -- shouldn't be returning a byte array
	 * that is just digested.
	 * @return
	 */
    public static byte[] prepareContent(ContentName name, SignedInfo signedInfo, byte[] content, int offset, int length) throws ContentEncodingException {
        if ((null == name) || (null == signedInfo)) {
            Log.info("Name and signedInfo must not be null.");
            throw new ContentEncodingException("prepareContent: name, signedInfo must not be null.");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLEncoder encoder = XMLCodecFactory.getEncoder(BinaryXMLCodec.CODEC_NAME);
        encoder.beginEncoding(baos);
        name.encode(encoder);
        signedInfo.encode(encoder);
        encoder.writeElement(CCNProtocolDTags.Content, content, offset, length);
        encoder.endEncoding();
        return baos.toByteArray();
    }

    /**
	 * Encode this object and calculate the digest.
	 */
    protected byte[] calcDigest() {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM);
            DigestOutputStream dos = new DigestOutputStream(new NullOutputStream(), md);
            encode(dos);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ContentEncodingException e) {
            throw new RuntimeException(e);
        }
        return md.digest();
    }

    /**
	 * Calculates a digest of the wire representation of this ContentObject.
	 * This is used as the implicit final name component.
	 * Note: the value is cached, so subsequent calls are fast.
	 */
    public byte[] digest() {
        if (_digest == null) _digest = calcDigest();
        return _digest;
    }

    public int compareTo(ContentObject o) {
        return name().compareTo(o.name());
    }

    public boolean isType(ContentType type) {
        return signedInfo().getType().equals(type);
    }

    public boolean isData() {
        return isType(ContentType.DATA);
    }

    public boolean isLink() {
        return isType(ContentType.LINK);
    }

    public boolean isGone() {
        return isType(ContentType.GONE);
    }

    public boolean isNACK() {
        return isType(ContentType.NACK);
    }

    public boolean isKey() {
        return isType(ContentType.KEY);
    }

    /**
	 * To aid debugging we output a human readable summary of this object here.
	 */
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append(String.format("CObj: name=%s, digest=%s, SI:%s len=%d, data=", _name, DataUtils.printHexBytes(digest()), _signedInfo, _content.length));
        int len = _content.length;
        if (len > 16) len = 16;
        s.append(ContentName.componentPrintURI(_content, 0, len));
        return s.toString();
    }
}
