package obol.format;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import obol.lang.Symbol;
import obol.lang.SymbolProperties;
import obol.lang.ObolTypes;
import obol.tools.Base64;
import obol.tools.Hex;
import obol.tools.Debug;

/** Basic format-related cryptographic functionallity.
 * @version $Id: FormatCrypto.java,v 1.6 2008/07/05 23:41:11 perm Exp $
 */
class FormatCrypto {

    public static final String __me = "obol.format.FormatCrypto";

    private static SecureRandom RNG;

    static String __status = null;

    static {
        boolean _insecureRND = false;
        __status = "\tRandom-number generator: ";
        if (_insecureRND) {
            byte[] _ba = new byte[32];
            new Random().nextBytes(_ba);
            RNG = new SecureRandom(_ba);
            __status += "INSECURELY INITIALIZED\n";
            System.err.println("*** WARNING ***\n" + "\tRandom generator insecurely initialized for speed reasons!\n" + "\tRevert to secure version by changing test in static initializer\n" + "\tof " + __me + " class and recompile!\n");
        } else {
            __status += "securely initialized\n";
            RNG = new SecureRandom();
        }
        String[][] _providers = { { "BouncyCastle", "org.bouncycastle.jce.provider.BouncyCastleProvider" }, { "Cryptix", "cryptix.jce.provider.CryptixCrypto" } };
        boolean _found = false;
        __status += "\tCryptographic libraries: JCE";
        for (int _i = 0; _i < _providers.length; _i++) {
            String _name = _providers[_i][0];
            String _class = _providers[_i][1];
            try {
                Class _provider = Class.forName(_class);
                Security.addProvider((Provider) _provider.newInstance());
                _found = true;
                __status += ", " + _name;
            } catch (ClassNotFoundException e) {
            } catch (Throwable all) {
                System.err.println("## WARNING: " + __me + ".static{}: while addProvider(" + _class + "), caught and ignored " + all);
            }
        }
        __status += "\n";
    }

    private Debug log = Debug.getInstance(__me);

    /** The JCE cipher specification associated with this format */
    protected String cipherSpec = null;

    protected Cipher cipher = null;

    protected IvParameterSpec IV = null;

    /** The JCE message digest specification associated with this format */
    protected String digestSpec = null;

    /** The JCE MAC specification associated with this format */
    protected String macSpec = null;

    /** The JCE cipher specification associated with this format's signing
     * operations */
    protected String signatureSpec = null;

    protected String signatureKeyAlg = null;

    /** Allow symbols containing keys to override the default specs. */
    protected boolean allowKeyOverride = false;

    private Stack keyStack = null;

    private volatile int inCrypto = 0;

    /** Return the (static) SecureRandom instance.
    */
    protected SecureRandom getSecureRandom() {
        return this.RNG;
    }

    /** Fill a byte array with random bits from the class' SecureRandom
     * instance.
     */
    public void nextBytes(byte[] bytes) {
        this.getSecureRandom().nextBytes(bytes);
    }

    /** Sets the three most-used crypto specification strings.
     * @param cipherspec the bulk-encryption cipher specification string.
     * @param digestspec the message digest spec.
     * @param signaturespec cipher used for signature ops.
     * @param signatureKeyAlg algorithm used for the signature keys (eg
     * "RSA", "DSA", "ECDSA" etc.)
     * @param macSpec algorithm used for MAC operations.
     * @param allowKeyOverride whether to allow the above data to derived from
     * keys and given priority over the above specs (default is <tt>false</tt>).
     */
    public void setCryptoSpecs(String cipherSpec, String digestSpec, String signatureSpec, String signatureKeyAlg, String macSpec, boolean allowKeyOverride) throws Exception {
        this.cipherSpec = cipherSpec;
        this.digestSpec = digestSpec;
        this.signatureSpec = signatureSpec;
        this.signatureKeyAlg = signatureKeyAlg;
        this.macSpec = macSpec;
        this.allowKeyOverride = allowKeyOverride;
        this.getCipher();
    }

    /** Sets the three most-used crypto specification strings.
     * @param cipherspec the bulk-encryption cipher specification string.
     * @param digestspec the message digest spec.
     * @param signaturespec cipher used for signature ops.
     * @param signatureKeyAlg Algorithm used for the signature keys (eg
     * @param macSpec algorithm used for MAC operations.
     * "RSA", "DSA", "ECDSA" etc.)
     */
    public void setCryptoSpecs(String cipherSpec, String digestSpec, String signatureSpec, String signatureKeyAlg, String macSpec) throws Exception {
        this.setCryptoSpecs(cipherSpec, digestSpec, signatureSpec, signatureKeyAlg, macSpec, false);
    }

    /** Set encryption/decryption flag.  Must be called just <em>before</em>
     * convert().  Implementations overriding this method must chain!
     * @param key The symbol associated with the key used during the
     * in-crypto operation.
     */
    public void preCrypto(Symbol key) throws FormatException {
        if (null == this.keyStack) {
            synchronized (this) {
                if (null == this.keyStack) {
                    this.keyStack = new Stack();
                }
            }
        }
        this.keyStack.push(key);
        this.inCrypto++;
    }

    /** Clear encryption/decryption flag.  Must be called just <em>after</em>
     * convert().  Implementations overriding this method must chain!
     */
    public void postCrypto() throws FormatException {
        if (this.keyStack.isEmpty()) {
            throw new RuntimeException(__me + ".postCrypto(): empty key stack, should NOT happen!");
        }
        this.keyStack.pop();
        this.inCrypto--;
    }

    /** Return encryption/decryption flag.
     * @return <tt>true</tt> if we're doing crypto operations during
     * convert, <tt>false</tt> otherwise.
     */
    public boolean inCrypto() {
        return (0 != this.inCrypto);
    }

    /** In an in-crypto context (between calls to pre-/postCrypto), return
     * the key.
     * @return Symbol containing key.
     * @throws IllegalStateException if invoked outside crypto context.
     */
    protected Symbol inCryptoPeekKey() {
        if (false == this.inCrypto()) {
            throw new IllegalStateException(__me + ".inCryptoPeekKey(): " + "invoked outside in-crypto context!");
        }
        if (this.keyStack.isEmpty()) {
            throw new RuntimeException(__me + ".inCryptoPeekKey(): " + "empty key stack, should NOT happen!");
        }
        return (Symbol) this.keyStack.peek();
    }

    /** Generate a random (symmetric) key for the given cipher algorithm and of
     * the given size.
     * @param algorithm JCE standard name of the symmetric algorithm to generate a key
     * for.
     * @param keysize size of key, in bits.
     * @throws Exception throws all kinds of exceprtions if something goes
     * wrong.
     */
    public Key generateKey(String algorithm, int keysize) throws Exception {
        KeyGenerator _kg = KeyGenerator.getInstance(algorithm);
        _kg.init(keysize, this.getSecureRandom());
        return _kg.generateKey();
    }

    /** Generate a shared-key using a key-agreement algorithm.
     * @param algorithm JCE standard name of the key-agreement algorithm to
     * share a secret with.
     * @param privkey private key to use
     * @param keys keys to use (at least one).
     * @throws Exception throws all kinds of exceprtions if something goes
     * wrong.
     */
    public byte[] keyAgreement(String algorithm, Key privkey, Key[] keys) throws Exception {
        KeyAgreement _ka = KeyAgreement.getInstance(algorithm);
        _ka.init(privkey, RNG);
        for (int _i = 0; _i < keys.length; _i++) {
            Key _k = keys[_i];
            boolean _lastPhase = ((keys.length - 1) == _i);
            _ka.doPhase(_k, _lastPhase);
        }
        return _ka.generateSecret();
    }

    /** Generate a random (asymmetric) keypair for the given cipher algorithm
     * and of the given size.
     * @param algorithm JCE standard name of the asymmetric algorithm to
     * generate a key for.
     * @param keysize size of keypair, in bits.
     * @param params Map of additional generation parameters (like
     * algorithmparameterspecs), or null.
     * If given, parameters must be structured like this: param, value.
     * The param must be a lowercase String.  The value can be any Object.
     * For example, if algorithm is "RSA", the map would containt the
     * following entires"keysize"=&gt;"1024", "publicexponent"=&gt;"65537", ")".
     * "Keysize" parameters may be omitted, but if given, the integer param
     * is given precedence (not the map).
     * @throws Exception throws all kinds of exceprtions if something goes
     * wrong.
     */
    public KeyPair generateKeyPair(String algorithm, int keysize, Map params) throws Exception {
        KeyPairGenerator _kpg = KeyPairGenerator.getInstance(algorithm);
        if (null != params) {
            AlgorithmParameterSpec _algspec = null;
            if ("DH".equalsIgnoreCase(algorithm)) {
                this.checkParamsAvailable(algorithm, params, "p", "g");
                _algspec = new DHParameterSpec(this.getBigInteger("p", params), this.getBigInteger("g", params), this.getInt("l", params, keysize));
            } else if ("RSA".equalsIgnoreCase(algorithm)) {
                if (params.containsKey("e")) {
                    params.put("publicexponent", params.get("e"));
                }
                this.checkParamsAvailable(algorithm, params, "publicexponent");
                _algspec = new RSAKeyGenParameterSpec(this.getInt("keysize", params, keysize), this.getBigInteger("publicexponent", params));
            } else if ("DSA".equalsIgnoreCase(algorithm)) {
                this.checkParamsAvailable(algorithm, params, "p", "q", "g");
                _algspec = new DSAParameterSpec(this.getBigInteger("p", params), this.getBigInteger("q", params), this.getBigInteger("g", params));
            } else if ("ElGamal".equalsIgnoreCase(algorithm)) {
                Class _cl = Class.forName("org.bouncycastle.jce.spec.ElGamalParameterSpec");
                this.checkParamsAvailable(algorithm, params, "p", "g");
                _algspec = (AlgorithmParameterSpec) _cl.getConstructor(BigInteger.class, BigInteger.class).newInstance(this.getBigInteger("p", params), this.getBigInteger("g", params));
            } else {
                throw new FormatException(__me + ".generateKeyPair(): " + "unsupported GenParameter algorithm \"" + algorithm + "\".");
            }
            _kpg.initialize(_algspec, this.getSecureRandom());
        } else {
            _kpg.initialize(keysize, this.getSecureRandom());
        }
        return _kpg.genKeyPair();
    }

    private BigInteger getBigInteger(String name, Map map) {
        BigInteger _retval = null;
        if (map.containsKey(name)) {
            Object _o = map.get(name);
            if (_o instanceof BigInteger) {
                _retval = (BigInteger) _o;
            } else {
                _retval = new BigInteger(_o.toString());
            }
        }
        return _retval;
    }

    private int getInt(String name, Map map, int fallback) {
        int _retval = fallback;
        if (map.containsKey(name)) {
            Object _o = map.get(name);
            if (_o instanceof Number) {
                _retval = ((Number) _o).intValue();
            } else if (_o instanceof String) {
                _retval = new Integer(_o.toString()).intValue();
            }
        }
        return _retval;
    }

    private void checkParamsAvailable(String algorithm, Map map, Object... params) throws FormatException {
        for (int _i = 0; _i < params.length; _i++) {
            if (false == map.containsKey(params[_i])) {
                throw new FormatException(__me + ".generateKeyPair(): " + algorithm + " with parameters requires  \"" + params[_i] + "\" parameter to be set!");
            }
        }
    }

    /** Generate a hash of the given source symbol, using the given hash
     * algorithm.
     * @param algorithm JCE standard name of the message-digest to use as a
     * hash, e.g. "MD5" or "SHA1" and so on.
     * @param source byte-array to hash.
     * @return byte array containing the hash result. The size will vary
     * depending on the algorithm used.
     * @throws Exception throws all kinds of exceprtions if something goes
     * wrong.
     */
    public byte[] generateHash(String algorithm, byte[] source) throws Exception {
        MessageDigest _md = MessageDigest.getInstance(algorithm);
        return _md.digest(source);
    }

    /** Generate hash of a HashSession object.
     * @param md MessageDigest instance as returned by this class'
     * initHashSession method.
     * @return byte array containing the hash result. The size will vary
     * depending on the algorithm used in initHashSession.
     * @throws Exception throws all kinds of exceprtions if something goes
     * wrong.
     */
    public byte[] generateHash(MessageDigest md) throws Exception {
        return md.digest();
    }

    /** Initialize a HashSession object.
     * The Hashsession is in this implementation a thin layer over the
     * java.security.MessageDigest class update/digest methods. It is mostly
     * used to support multiple inputs to (generate hash ...).
     * @param algorithm JCE standard name of the message-digest to use as a
     * hash, e.g. "MD5" or "SHA1" and so on.
     * @return MessageDigest instance representing hashsession.
     * @throws Exception throws all kinds of exceprtions if something goes
     * wrong.
     */
    public MessageDigest initHashSession(String algorithm) throws Exception {
        return MessageDigest.getInstance(algorithm);
    }

    /** Update a HashSession object.
     * @param md MessageDigest instance as returned by this class'
     * initHashSession method.
     * @param source byte-array to update HashSession with.
     * @throws Exception throws all kinds of exceprtions if something goes
     * wrong.
     */
    public void updateHashSession(MessageDigest md, byte[] source) throws Exception {
        md.update(source);
    }

    /** Return a new signature object associated with this format.
     * @return new Signature object ready for initialization, or
     * <tt>null</tt> if this format doesn't have a signature specification
     * configured.
     */
    public Signature getSignature() throws Exception {
        if (null == this.signatureSpec) {
            return null;
        }
        return Signature.getInstance(this.signatureSpec);
    }

    /** Return a new signature object associated with the given key.
     * First attempts to find properties in the symbol that will tell which
     * signature digest and additional modes to use.
     * If that fails, looks at the default signature specification, as given by
     * the <tt>setCryptoSpecs()</tt> method, and uses the key's algorithm.
     * <P>
     * If the Format implementation have passed the <tt>setCryptoSpecs</tt>
     * arguement <tt>allowKeyOverride</tt> to false, the key-provided data is
     * ignored, and the default settings are used.
     * <P>
     * If no signature has been configured for the format implementation,
     * and the key contains no overriding information (SignatureDigest and
     * SignatureMask), the returned Signature object will be based on
     * <TT>SHA1with</TT><em>key-algorithm</em>, which may or may not exist.
     * @param key Symbol containing the key, or <tt>null</tt> to use the
     * format-dictated default signature-scheme.
     * @return new Signature object ready for initialization, or
     * <tt>null</tt> if this format doesn't have a signature specification
     * configured.
     * @throws Exception of some nasty sort if something is inappropriate
     * regarding the key.
     */
    public Signature getSignature(Symbol key) throws Exception {
        if (false == this.allowKeyOverride) {
            return this.getSignature();
        }
        String _spec = null;
        String _alg = ((Key) key.getValue()).getAlgorithm();
        if (key.existProperty(SymbolProperties.SignatureDigest)) {
            _spec = key.getProperty(SymbolProperties.SignatureDigest) + "with" + _alg;
            if (key.existProperty(SymbolProperties.SignatureMask)) {
                _spec = _spec + "and" + key.getProperty(SymbolProperties.SignatureMask);
            }
        }
        if (null == _spec) {
            String _defaultDigest = this.signatureSpec.substring(0, this.signatureSpec.indexOf("with"));
            if (null == _defaultDigest) {
                _defaultDigest = "SHA1";
            }
            String _defaultAnd = "";
            int _andIndex = this.signatureSpec.indexOf("and");
            if (-1 != _andIndex) {
                _defaultAnd = this.signatureSpec.substring(_andIndex);
            }
            _spec = _defaultDigest + "with" + _alg + _defaultAnd;
        }
        return Signature.getInstance(_spec);
    }

    /** Return a new MAC object associated with this format.
     * @return new MAC object ready for initialization, or <tt>null</tt> if
     * this format doesn't have a MAC specification configured.
     */
    public Mac getMac() throws Exception {
        if (null == this.macSpec) {
            return null;
        }
        return Mac.getInstance(this.macSpec);
    }

    /** Return a new MAC object associated with the given key.
     * First attempts to find properties in the symbol that will tell which
     * MAC digest and additional modes to use.
     * If that fails, looks at the default MAC specification, as given by
     * the <tt>setCryptoSpecs()</tt> method, and uses the key's algorithm.
     * <P>
     * If the Format implementation have passed the <tt>setCryptoSpecs</tt>
     * arguement <tt>allowKeyOverride</tt> to false, the key-provided data
     * is ignored, and the default settings are used.
     * <P>
     * If no MAC has been configured for the format implementation, and the
     * key contains no useful information (MACType, MACIteration or
     * MACSalt), the returned MAC object will be based on <TT>HmacMD5</TT>.
     * @param key Symbol containing the key, or <tt>null</tt> to use the
     * format-dictated default MAC-scheme.
     * @return new Mac object ready for initialization, or <tt>null</tt> if
     * this format doesn't have a MAC specification configured.
     * @throws Exception of some nasty sort if something is inappropriate
     * regarding the key.
     */
    public Mac getMac(Symbol key) throws Exception {
        if (false == this.allowKeyOverride) {
            return this.getMac();
        }
        String _spec = (String) key.getProperty(SymbolProperties.MACType);
        if (null == _spec) {
            _spec = "HmacMD5";
        }
        return Mac.getInstance(_spec);
    }

    /** Return the signature's key's algorithm name (see setCryptoSpecs).
     * Used to generate random keys for signature operations.
     * @see setCryptoSpecs
     */
    public String getSignatureKeyAlgorithm() {
        return this.signatureKeyAlg;
    }

    /** Return the cipher key's algorithm name.
     * Used to generate random keys for cipher operations.
     * @see setCryptoSpecs
     */
    public String getCipherKeyAlgorithm() {
        return getCipherKeyAlgorithm(this.getCipherAlgorithm());
    }

    /** Return the MAC specification.
     * @see setCryptoSpecs
     */
    public String getMacSpec() {
        return this.macSpec;
    }

    /** Return the cipher key's algorithm name.
     * Used to generate random keys for cipher operations.
     * @see setCryptoSpecs
     */
    public static String getCipherKeyAlgorithm(String alg) {
        return alg.split("/")[0];
    }

    /** Return all but the cipher key's algorithm name.
     * Used to figure out cipher instance from keys.
     * @see setCryptoSpecs
     */
    public static String getAllButCipherKeyAlgorithm(String alg) {
        return alg.substring(alg.indexOf('/') + 1);
    }

    /** See if the given algorithm name is recognized as the JCE name of an
     * asymmetric algorithm.
     * This method is typically used to find out if a Key is for an
     * asymmetyic or symmetric cipher.
     * Currently this implementation of this method recognizes RSA, DSA,
     * ElGamal and DiffieHellman as asymmetric names.
     * Format implementations are encouraged to override this method in
     * order to support new asymmtric algorithms.
     * @param algname the ouput from e.g. Key.getAlgorithm().
     * @return <tt>true</tt> if the algorithm name is recognized as the name
     * of an asymmetric algorithm.
     */
    public boolean isAsymmetricAlgorithm(String algname) {
        return ("RSA".equals(algname) || "DSA".equals(algname) || "ElGamal".equals(algname) || "DiffieHellman".equals(algname));
    }

    /** Return the Cipher object associated with this format.
    */
    public Cipher getCipher() throws Exception {
        if (null == this.cipher) {
            this.setCipher(this.cipherSpec);
        }
        return this.cipher;
    }

    /** Return the cipher object associated with this format, initialized
     * with the given key and mode.
     * Notice the following heuristics:
     * 1) If the default cipher's algorithm doesn't match the key's, then the
     * key's algorithm is given priority, whilst keeping the default
     * cipher's mode and padding scheme.
     * 2) If the algorithms have different symmetry, then the mode/sceme we
     * fall back upon are CBC/PKCS5Padding and ECB/PKCS1Padding, for
     * symmetric and asymmetric, respectively.
     * The default cipher is never overwritten, and a cipher with different
     * specs only exists temporarily (i.e. in such a case this method has no
     * side-effects).
     * <P>
     * Format implementations may want to override this in order to do
     * special IV initialization.  It should be safe to do that on the
     * Cipher instance returned from this method.
     * Format implementations that thinks this is a load of bollocks should
     * override one of the getLinkedCipher*Stream methods and do their own
     * thing instead of calling this method.
     * @param key Key to initialize cipher with. 
     * @param mode Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE. Doh!
     * @param IV The Cipher's initialization vector, if appropriate, or
     * <tt>null</tt> if not.  <b>Note for symmetric ciphers:</b> if the key
     * algorithm differs from the default cipher algorithm, and IV is
     * <tt>null</tt>, then the returned cipher will be initialized with a
     * zero-IV of the same size as the returned cipher's blocksize.  If IV
     * is not <tt>null</tt> but differs in size from the default IV, an
     * appropriate zero-IV will be created and the default IV will be
     * left-justified copied (and possibly cropped) into this.
     * @return Initialized cipher object matching the key and mode.
     * @throws Exception if something went wrong.
     */
    public Cipher getCipher(Key key, int mode, byte[] IV) throws Exception {
        String _keyAlgo = key.getAlgorithm();
        boolean _asymmetricKey = this.isAsymmetricAlgorithm(_keyAlgo);
        boolean _differentCipher = false;
        Cipher _c = this.getCipher();
        String _cipherSpec = _c.getAlgorithm();
        String _cipherAlgo = getCipherKeyAlgorithm(_cipherSpec);
        String _spec = getAllButCipherKeyAlgorithm(_cipherSpec);
        if (false == _cipherAlgo.equals(_keyAlgo)) {
            _differentCipher = true;
            if (this.isAsymmetricAlgorithm(_cipherAlgo) != _asymmetricKey) {
                if (_asymmetricKey) {
                    _spec = "ECB/PKCS1Padding";
                } else {
                    _spec = "CBC/PKCS5Padding";
                }
            }
            _c = Cipher.getInstance(_keyAlgo + "/" + _spec);
        }
        if (_asymmetricKey) {
            _c.init(mode, key);
            return _c;
        }
        IvParameterSpec _currentIV = null;
        boolean _needIV = (false == _spec.startsWith("/ECB/"));
        if (false == _differentCipher) {
            if ((null != this.IV) && (null != IV)) {
                _currentIV = new IvParameterSpec(this.normalizeIV(_c, IV));
            } else if ((null == this.IV) && (null != IV)) {
                _currentIV = new IvParameterSpec(this.normalizeIV(_c, IV));
            } else if ((null != this.IV) && (null == IV)) {
                _currentIV = this.IV;
            } else {
                if (_needIV) {
                    _currentIV = new IvParameterSpec(this.normalizeIV(_c, null));
                }
            }
        } else {
            if ((null == IV) && (null != this.IV) && _needIV) {
                _currentIV = new IvParameterSpec(this.normalizeIV(_c, this.IV.getIV()));
            } else if ((null == IV) && (null == this.IV) && _needIV) {
                _currentIV = new IvParameterSpec(this.normalizeIV(_c, null));
            } else if ((null != IV) && _needIV) {
                _currentIV = new IvParameterSpec(normalizeIV(_c, IV));
            } else {
                _currentIV = null;
            }
        }
        if (null == _currentIV) {
            _c.init(mode, key);
        } else {
            _c.init(mode, key, _currentIV);
        }
        return _c;
    }

    public Cipher getCipher(Symbol key, int mode) throws Exception {
        return this.getCipher((Key) key.getValue(), mode, this.obtainIVByProperty(key));
    }

    /** Set the cipher object associated with this format.
    */
    protected void setCipher(Cipher cipher) {
        this.cipher = cipher;
    }

    /** Return the cipher algorithm specification string.
    */
    public String getCipherAlgorithm() {
        if (null == this.cipher) {
            if (null == this.cipherSpec) {
                return null;
            }
            return this.cipherSpec;
        }
        return this.cipher.getAlgorithm();
    }

    /** Normalize IV by ensuring that it fits the cipher's block-size.
     * The normalized IV is left-adjusted and optionally zero-padded to fit
     * one cipher block.
     * @param c Cipher object to get block size from
     * @param IV IV to left-adjust and pad (may be null, smaller or larger
     * than the cipher's block-size).
     */
    private byte[] normalizeIV(Cipher c, byte[] IV) {
        byte[] _iv = new byte[c.getBlockSize()];
        Arrays.fill(_iv, (byte) 0);
        if (null != IV) {
            System.arraycopy(IV, 0, _iv, 0, Math.min(IV.length, _iv.length));
        }
        return _iv;
    }

    /** Set the default Cipher IV for the associated bulk cipher.
     * This method attempts to be smart, and will not set the IV of a cipher
     * which takes no IV (i.e. ECB or NONE).
     * @param ivdata byte array containing IV, or <tt>null</tt> to use a
     * NULL IV.
     */
    public void setDefaultCipherIV(byte[] ivdata) throws Exception {
        byte[] _iv = ivdata;
        if (null == _iv) {
            this.IV = null;
            _iv = new byte[this.cipher.getBlockSize()];
            for (int _i = 0; _i < _iv.length; _i++) {
                _iv[_i] = (byte) 0;
            }
        }
        if ((this.cipher.getBlockSize() > 0) && !this.cipher.getAlgorithm().matches("(ECB|NONE)")) {
            this.IV = new IvParameterSpec(_iv);
        }
    }

    /** Set the cipher of this format.
     * @param spec JCE cipher specification, or <tt>null</tt> for no cipher.
     */
    public void setCipher(String spec) throws Exception {
        if (null == spec) {
            this.cipher = null;
            return;
        }
        this.cipher = Cipher.getInstance(spec);
    }

    private byte[] obtainIVByProperty(Symbol key) throws FormatException {
        byte[] _retval = null;
        if (key.existProperty(SymbolProperties.CipherIV)) {
            Object _iv = key.getProperty(SymbolProperties.CipherIV);
            Symbol _dref = key.dereference(_iv.toString());
            _retval = convertToBinary((null != _dref ? _dref.getValue() : _iv), null);
        }
        return _retval;
    }

    /** Creates a linked encrypted outputstream.
     * Format implementations may want to override this to access the key
     * symbol, do their own (possibly weird) encryption on streams.
     * @param key Symbol containing key to use.
     * @param out OutputStream to write encrypted data to.
     * @return OutputStream that, when written to, will write encrypted data
     * to the provided OutputStream.
     */
    public OutputStream getLinkedCipherOutputStream(Symbol key, OutputStream out) throws Exception {
        return new CipherOutputStream(out, this.getCipher(key, Cipher.ENCRYPT_MODE));
    }

    /** Creates a linked decrypted inputstream.
     * Format implementations may want to override this to access the key
     * symbol, or do their own (possibley weird) decryption on streams.
     * @param key Symbol containing key to use.
     * @param out InputStream to read encrypted data from.
     * @return InputStream that, when read from, will output decrypted data
     * read from the provieded inputstream.
     */
    public InputStream getLinkedCipherInputStream(Symbol key, InputStream in) throws Exception {
        return new CipherInputStream(in, this.getCipher(key, Cipher.DECRYPT_MODE));
    }

    public byte[] decrypt(Symbol key, byte[] ciphertext) throws Exception {
        Cipher _c = this.getCipher(key, Cipher.DECRYPT_MODE);
        return _c.doFinal(ciphertext);
    }

    public byte[] encrypt(Symbol key, byte[] plaintext) throws Exception {
        Cipher _c = this.getCipher(key, Cipher.ENCRYPT_MODE);
        return _c.doFinal(plaintext);
    }

    /** Creates an outputstream that when written to will update a Signature
     * object.
     * This can be used for both signing and verification.
     * Format implementations may want to override this to gain access to
     * the signing key symbol, or do their own esotoric signature handling.
     * If overriden, overriders <em>must</em> call the superclass method!
     * @param sig initialized Signature object.
     * @param key Symbol containing key used to initialize signature.
     * @return Outputstream that will update signature.
     */
    public OutputStream getLinkedSigningStream(Signature sig, Symbol key) throws FormatException {
        return new SignedOutputStream(sig);
    }

    /** Creates an outputstream that when written to will update a Mac
     * object.
     * This can be used for both signing and verification.
     * Format implementations may want to override this to gain access to
     * the key symbol, or do their own esotoric MAC handling.
     * If overriden, overriders <em>must</em> call the superclass method!
     * @param mac initialized Mac object.
     * @param key Symbol containing key used to initialize MAC.
     * @return Outputstream that will update signature.
     */
    public OutputStream getLinkedMACStream(Mac mac, Symbol key) throws FormatException {
        return new MacOutputStream(mac);
    }

    /** Convert an object to a byte array. 
     * This method can convert byte[] (a no-op), Strings (encoded in
     * Base64), * Number (not floats or doubles!), or shared-keys (by means
     * of getEncoded()).
     * @param Symbol whose value is either a byte[], a String containing
     * Base64-encoded data, or a Number.
     * @param encoding String encoding object is a string with dicoding
     * different than "ISO8859_1", or <tt>null</tt>.
     * @return byte [].
     * @throws FormatException if something went wrong.
     */
    public static byte[] convertToBinary(Object obj, String encoding) throws FormatException {
        byte[] _retval = null;
        Class _cl = obj.getClass();
        if (_cl.isArray()) {
            if (false == _cl.getComponentType().equals(byte.class)) {
                throw new FormatException(__me + ".convertToBinary() Cannot " + "convert array of " + _cl.getComponentType() + " to byte[] for binary type!");
            }
            _retval = (byte[]) obj;
        } else if (obj instanceof String) {
            String _s = (String) obj;
            if ('|' == _s.charAt(0) && '|' == _s.charAt(_s.length() - 1)) {
                try {
                    byte[] _ba = _s.getBytes("US-ASCII");
                    _retval = Base64.decode(_ba, 1, _ba.length - 2);
                } catch (UnsupportedEncodingException e) {
                    throw (RuntimeException) new RuntimeException("Platform does not support default " + "US-ASCII encoding!").initCause(e);
                }
            } else if (_s.startsWith("0x") || _s.startsWith("0X")) {
                _retval = Hex.fromString(_s.substring(2));
            } else if (null != encoding) {
                try {
                    _retval = _s.getBytes(encoding);
                } catch (UnsupportedEncodingException e) {
                    throw (FormatException) new FormatException(__me + ".convertToBinary(): failure " + "converting String using \"" + encoding + "\" encoding!").initCause(e);
                }
            } else {
                try {
                    _retval = _s.getBytes("ISO8859_1");
                } catch (UnsupportedEncodingException e) {
                    throw (FormatException) new FormatException(__me + ".convertToBinary(): failure " + "converting String using default ISO8859_1 " + "encoding").initCause(e);
                }
            }
        } else if (obj instanceof Number || obj instanceof Date) {
            if (obj instanceof BigInteger) {
                _retval = ((BigInteger) obj).toByteArray();
            } else if (obj instanceof Integer) {
                int _i = ((Integer) obj).intValue();
                _retval = new byte[4];
                _retval[0] = (byte) (_i >> 24 & 0x0ff);
                _retval[1] = (byte) (_i >> 16 & 0x0ff);
                _retval[2] = (byte) (_i >> 8 & 0x0ff);
                _retval[3] = (byte) (_i & 0x0ff);
            } else if (obj instanceof Long || obj instanceof Date) {
                long _l = (obj instanceof Date) ? ((Date) obj).getTime() : ((Long) obj).longValue();
                _retval = new byte[8];
                _retval[0] = (byte) (_l >> 56 & 0x0ff);
                _retval[1] = (byte) (_l >> 48 & 0x0ff);
                _retval[2] = (byte) (_l >> 40 & 0x0ff);
                _retval[3] = (byte) (_l >> 32 & 0x0ff);
                _retval[4] = (byte) (_l >> 24 & 0x0ff);
                _retval[5] = (byte) (_l >> 16 & 0x0ff);
                _retval[6] = (byte) (_l >> 8 & 0x0ff);
                _retval[7] = (byte) (_l & 0x0ff);
            }
        } else if (obj instanceof SecretKey) {
            _retval = ((SecretKey) obj).getEncoded();
        }
        return _retval;
    }

    /** Examines the given symbol, and attempts to convert the value into the
     * specified crypto-related type.
     * This method modifies the provided symbol's value.
     * This attempts to convert a byte-array into the JCE key object that
     * corresponds to one of <tt>shared-key</tt> <tt>pubic-key</tt>
     * <tt>private-key</tt>.
     * Also, if the keyword property ":serialized" exists, this method will
     * try to treat the key symbol-value key material (i.e. byte array) as a
     * serialized object, and try to deserialize it.
     * <P>
     * Obviously this method can only handle cryptographic types supported
     * by the runtime (i.e whatever JCE and the installed providers support).
     * It depends on the following property entries:
     * <UL>
     *     <LI><tt>alg</tt>algorithm name, i.e. for "key" types, the JCE
     *     standard name for the key-related algorithm (eg "RSA", "DESede",
     *     "DiffieHellman", etc).
     *     <LI><tt>number-of-bits | number-of-bytes</tt>(optional) key size
     *     parameter. If not provided, this value is derived from the raw
     *     data. Provide if derived value is wrong.
     * </UL>
     */
    public void convertToCryptoTypes(Symbol targetsym) throws Exception {
        if (this.isCryptoType(targetsym.getValue())) {
            return;
        }
        String _alg = (String) targetsym.getProperty(SymbolProperties.AlgorithmName);
        String _type = targetsym.getType();
        if (null == _alg) {
            Object _v = targetsym.getValue();
            if (_v instanceof PublicKey || _v instanceof PrivateKey) {
                _alg = ((PublicKey) targetsym.getValue()).getAlgorithm();
                targetsym.setProperty(SymbolProperties.AlgorithmName, _alg);
            } else {
                throw new FormatException(__me + ".convertToCryptoTypes(): Symbol \"" + targetsym + "\" property-list entry \"" + SymbolProperties.AlgorithmName + "\" missing - cannot convert \"" + _type + "\" type of class " + _v.getClass().getName() + " to " + "internal JCE equivalent!");
            }
        }
        Class _vc = targetsym.getValue().getClass();
        if (KeySpec.class.isAssignableFrom(_vc)) {
            KeySpec _spec = (KeySpec) targetsym.getValue();
            if (ObolTypes.SharedKey.equals(_type)) {
                targetsym.setValue(SecretKeyFactory.getInstance(_alg).generateSecret(_spec));
                return;
            }
            KeyFactory _factory = KeyFactory.getInstance(_alg);
            if (ObolTypes.PublicKey.equals(_type)) {
                targetsym.setValue(_factory.generatePublic(_spec));
            } else if (ObolTypes.PrivateKey.equals(_type)) {
                targetsym.setValue(_factory.generatePrivate(_spec));
            } else {
                throw new FormatException(__me + ".convertToCryptoTypes(): Symbol \"" + targetsym + "\"'s value is an KeySpec (" + _spec.getClass().getName() + "), but type is \"" + _type + "\", not shared-key, public- or private-key!");
            }
            return;
        }
        if (false == (_vc.isArray() && _vc.getComponentType().isAssignableFrom(byte.class))) {
            throw new FormatException(__me + ".convertToCryptoTypes(): expected byte [] as Symbol \"" + targetsym + "\"'s value, not " + _vc.toString() + " - " + "cannot convert to internal JCE equivalent!");
        }
        byte[] _ba = (byte[]) targetsym.getValue();
        int _newsize = -1;
        if (targetsym.existProperty(SymbolProperties.NumberOfBits)) {
            _newsize = Integer.parseInt((String) targetsym.getProperty(SymbolProperties.NumberOfBits));
        } else if (targetsym.existProperty(SymbolProperties.NumberOfBytes)) {
            _newsize = Integer.parseInt((String) targetsym.getProperty(SymbolProperties.NumberOfBytes)) * 8;
        }
        if (-1 != _newsize) {
            _newsize = (_newsize >> 3) + ((_newsize & 03) != 0 ? 1 : 0);
            if (_newsize < _ba.length) {
                byte[] _trunc = new byte[_newsize];
                System.arraycopy(_ba, 0, _trunc, 0, _newsize);
                _ba = _trunc;
            } else if (_newsize > _ba.length) {
                byte[] _expand = new byte[_newsize];
                for (int _i = _ba.length; _i < _expand.length; _i++) {
                    _expand[_i] = (byte) 0;
                }
                System.arraycopy(_ba, 0, _expand, 0, _ba.length);
                _ba = _expand;
            }
        }
        if (targetsym.existProperty(":serialized")) {
            try {
                ObjectInputStream _oin = new ObjectInputStream(new ByteArrayInputStream(_ba));
                Object _o = _oin.readObject();
                _oin.close();
                Class _oc = _o.getClass();
                if (false == (ObolTypes.PublicKey.equals(_type) && PublicKey.class.isAssignableFrom(_oc)) || (ObolTypes.PrivateKey.equals(_type) && PrivateKey.class.isAssignableFrom(_oc)) || (ObolTypes.SharedKey.equals(_type) && Key.class.isAssignableFrom(_oc))) {
                    throw new FormatException(__me + ".convertToCryptoTypes(): cannot obtain type \"" + _type + "\" from serialized data of type " + _o.getClass().toString());
                }
                targetsym.setValue(_o);
                return;
            } catch (Exception e) {
                throw (FormatException) new FormatException(__me + ".convertToCryptoTypes(): unable to convert " + "serialized type \"" + _type + "\" to decerialized object!").initCause(e);
            }
        }
        if (ObolTypes.SharedKey.equals(_type)) {
            targetsym.setValue(new SecretKeySpec(_ba, _alg));
            return;
        } else if (ObolTypes.PublicKey.equals(_type)) {
            KeyFactory _factory = KeyFactory.getInstance(_alg);
            KeySpec _spec = null;
            String[] _try = new String[] { "java.security.spec." + _alg + "PublicKeySpec", "java.security.spec." + _alg + "EncodedKeySpec", "java.security.spec." + _alg + "KeySpec", "javax.crypto.spec." + _alg + "PublicKeySpec", "javax.crypto.spec." + _alg + "EncodedKeySpec", "javax.crypto.spec." + _alg + "KeySpec" };
            _spec = findAppropriateKeySpec(_alg, _type, _try, _ba);
            if (null == _spec) {
                throw new FormatException(__me + ".convertToCryptoTypes(): cannot find publickey-" + "type KeySpec for algorithm \"" + _alg + "\"!");
            }
            targetsym.setValue(_factory.generatePublic(_spec));
        } else if (ObolTypes.PrivateKey.equals(_type)) {
            KeyFactory _factory = KeyFactory.getInstance(_alg);
            KeySpec _spec = null;
            String[] _try = new String[] { "java.security.spec." + _alg + "PrivateKeySpec", "java.security.spec." + _alg + "EncodedKeySpec", "java.security.spec." + _alg + "KeySpec", "javax.crypto.spec." + _alg + "PrivateKeySpec", "javax.crypto.spec." + _alg + "EncodedKeySpec", "javax.crypto.spec." + _alg + "KeySpec" };
            _spec = findAppropriateKeySpec(_alg, _type, _try, _ba);
            if (null == _spec) {
                throw new FormatException(__me + ".convertToCryptoTypes(): cannot find privatekey-" + "type KeySpec for algorithm \"" + _alg + "\"!");
            }
            targetsym.setValue(_factory.generatePrivate(_spec));
        } else {
            throw new FormatException(__me + ".convertToCryptoTypes(): Symbol " + targetsym + " contains value of unknown type \"" + _type + "\"!");
        }
    }

    /** Attempt to figure out appropriate KeySpec class for an algorithm.
     * All names refer to JCE names.
     * <P>By default this class calls this method with the following
     * <i>combos</i> parameter:
     * <pre>
     * 	String [] combos = new String [] {
     *		"java.security.spec." + algname + keyType + "KeySpec",
     *		"java.security.spec." + algname +"EncodedKeySpec",
     *		"java.security.spec." + algname + "KeySpec",
     *		"javax.crypto.spec." + algname + keyType + "KeySpec",
     *		"javax.crypto.spec." + algname + "EncodedKeySpec",
     *		"javax.crypto.spec." + algname + "KeySpec", };
     * </pre>
     * Overriding classes could use their own knowledge of KeySpec clases to
     * manipulate this list accordingly.
     * @param algName Algorithm name, eg "RSA", "DH", etc.
     * @param keyType Key Oboltype, i.e. either "Public" or "Private".
     * @param combos String array of potential class names, based on the
     * algorithm.
     * @param rawdata byte array containing the key raw material (encoded)
     * @return valid KeySpec instance, or <tt>null</tt> if none were found.
     */
    public KeySpec findAppropriateKeySpec(String algName, String keyType, String[] combos, byte[] rawdata) {
        KeySpec _spec = null;
        byte[] _bytearray = { 0 };
        Class _constructorSig = _bytearray.getClass();
        Class[] _constructorSpec = new Class[] { _constructorSig };
        Object[] _args = new Object[] { rawdata };
        for (int _i = 0; _i < combos.length; _i++) {
            try {
                Class _c = Class.forName(combos[_i]);
                Constructor _con = _c.getConstructor(_constructorSig);
                _spec = (KeySpec) _con.newInstance(_args);
                break;
            } catch (Exception e) {
            }
        }
        if (null != _spec) {
            log.debug("findAppropriateKeySpec(): found " + _spec.getClass().getName());
        }
        return _spec;
    }

    /** Test to see if the given object is some JCE object instance.
     * In this class implementation this method only tests for JCE Key
     * membership.
     * Subclasses would probably like to override this one.
     * @param obj Object to test.
     * @return <tt>true</tt> if the object is some JCE instance,
     * <tt>false</tt> if it's not.
     */
    public boolean isCryptoType(Object obj) {
        return Key.class.isAssignableFrom(obj.getClass());
    }

    /** Extracts all key-relevant data from the given key and add these as
     * properties to the given symbol, or create a new symbol.
     * These data includes type and algorithm name.
     * @param sym Symbol to update properties on, or null to create a new
     * symbol.
     * @param key Key to extract data from, or null to use symbol's value.
     * @return updated, possibly new, symbol.
     */
    public Symbol keyToSymbol(Symbol sym, Key key) {
        if (null == sym) {
            sym = new Symbol(null, key);
        }
        if (null == key) {
            key = (Key) sym.getValue();
        }
        Class _kc = key.getClass();
        if (PublicKey.class.isAssignableFrom(_kc)) {
            sym.setProperty(SymbolProperties.Type, ObolTypes.PublicKey);
        } else if (PrivateKey.class.isAssignableFrom(_kc)) {
            sym.setProperty(SymbolProperties.Type, ObolTypes.PrivateKey);
        } else if (Key.class.isAssignableFrom(_kc)) {
            sym.setProperty(SymbolProperties.Type, ObolTypes.SharedKey);
        }
        sym.setProperty(SymbolProperties.AlgorithmName, key.getAlgorithm());
        return sym;
    }

    public Symbol keyToSymbol(Symbol sym) {
        return this.keyToSymbol(sym, null);
    }

    public Symbol keyToSymbol(Key key) {
        return this.keyToSymbol(null, key);
    }

    /** Returns a Map key,value decomposition of the given object, which must
     * implement exactly one of the PublicKey or PrivateKey interfaces.
     * This is a tool method to aid format implementations with converting
     * public/private keys between format- and JCE-representation.
     * <P>
     * Uses reflection to dismember the key, so that all methods that
     * returns a numeric arguemnt (BigInteger, long, int, short and byte),
     * takes no arguments, and have a method name that begins with
     * "<tt>get</tt>" are invoked, and the result is stored in a Map where
     * the key is derived from the  method name.
     * E.g. for an RSA public key, the key,value mappings returned are:
     * (e,BigInteger)(n,BigInteger).
     * Useful for storing unknown key objects.
     * @return Map containing decomposed key, or <tt>null</tt> if
     * decomposition weren't possible.
     * @throws IllegalArgumentException if the given key object couldn't be
     * cast to a PrivateKey or PublicKey interface.
     */
    public Map destructure(Object key) {
        Class _target = null;
        if (key instanceof PublicKey) {
            _target = PublicKey.class;
        } else if (key instanceof PrivateKey) {
            _target = PrivateKey.class;
        } else if (key instanceof Symbol) {
            return destructure(((Symbol) key).getValue());
        } else {
            throw new IllegalArgumentException(__me + ".destructure(): " + "provided object of class " + key.getClass() + " not assignable to PublicKey or PrivateKey!");
        }
        LinkedHashMap _map = new LinkedHashMap();
        this.destructure(key, _target, _map);
        if (_map.isEmpty()) {
            log.warn("**WARNING** empty destructuring map for key of class " + key.getClass());
            _map.clear();
            _map = null;
        }
        return _map;
    }

    private void destructure(Object key, Class target, Map map) {
        Class _cl = key.getClass();
        Class[] _interfaces = _cl.getInterfaces();
        while (null == _interfaces || 0 == _interfaces.length) {
            _cl = _cl.getSuperclass();
            log.debug(".destructure(3): using superclass " + _cl);
            if (null == _cl) {
                String _err = (__me + ".destructure(): key object of " + "class " + key.getClass() + " cannot be " + "destructured, since neither it nor its super" + "classes implements any interfaces!");
                log.fatal("\n***** FATAL INTERNAL ERROR ****\n\t" + _err);
                throw new RuntimeException(_err);
            }
            _interfaces = _cl.getInterfaces();
        }
        Class _bigint = BigInteger.class;
        for (int _i = 0; _i < _interfaces.length; _i++) {
            Class _candidate = _interfaces[_i];
            if (target.isAssignableFrom(_candidate)) {
                Method[] _methodlist = _candidate.getMethods();
                for (int _im = 0; _im < _methodlist.length; _im++) {
                    Method _method = _methodlist[_im];
                    String _methodName = _method.getName();
                    if (0 != _method.getParameterTypes().length) {
                        continue;
                    }
                    if ("getParams".equals(_methodName)) {
                        try {
                            this.destructure(_method.invoke(key, (Object[]) null), _method.getReturnType(), map);
                        } catch (Exception e) {
                            log.warn(".destructure(3): caught (and ignored) " + "exception while recursively attempting " + "to invoke method " + _methodName + " on object of class " + _cl.getName() + ": ", e);
                        }
                    }
                    if (false == _methodName.startsWith("get") || "get".equals(_methodName)) {
                        continue;
                    }
                    Class _returnType = _method.getReturnType();
                    if ((false == _returnType.equals(_bigint)) && (false == _returnType.equals(long.class)) && (false == _returnType.equals(int.class)) && (false == _returnType.equals(short.class)) && (false == _returnType.equals(byte.class))) {
                        if (_returnType.isArray()) {
                            if (false == _returnType.getComponentType().equals(byte.class)) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                    try {
                        map.put(_methodName.substring(3), _method.invoke(key, (Object[]) null));
                    } catch (Exception e) {
                        log.warn(".destructure(3): caught (and ignored) exception " + "while attempting to invoke method " + _methodName + " on object of class " + _cl.getName() + ": ", e);
                    }
                }
            }
        }
    }

    /** Given a map from destructure, an algorithm name and a key type,
     * attempt to find and reinitialize a KeySpec class that satisfies the
     * map.
     * This is a tool method to aid format implementations with converting
     * public/private keys between format- and JCE-representation.
     * Use the <tt>findAppropriateKeySpec()</tt> method for non-public-key
     * algorithms, or if a destructure map is unavailable.
     * <P>
     * Formats implementations can override to provide their own candidate
     * class discovery functions, then call <tt>restructure(Map,Class)</tt>
     * to do the actual reflective initialization.
     * @param destructure Map of key,values that came from this class'
     * <tt>destructure()</tt> method.
     * @param algName the algorithm name
     * @param keyType type of key, either ObolTypes.PublicKey or
     * ObolTypes.PrivateKey.
     * @return Initialized keyspec object (or derivative) which can be used
     * with KeyFactory to generate a key, or <tt>null</tt> if this wasn't
     * possible.
     */
    public Object restructure(Map destructure, String algName, String keyType) {
        try {
            String[][] _list = this.getRestructureCandidateNames();
            for (int _p = 0; _p < _list.length; _p++) {
                String _s = _list[_p][0].toLowerCase();
                if (-1 != _s.indexOf(algName.toLowerCase())) {
                    if (-1 != _s.indexOf(keyType.toLowerCase())) {
                        String[] _sa = new String[_list[_p].length - 1];
                        System.arraycopy(_list[_p], 1, _sa, 0, _sa.length);
                        Object _o = null;
                        try {
                            String _classname = _list[_p][0];
                            _o = this.restructure(destructure, Class.forName(_classname), _sa);
                        } catch (ClassNotFoundException e) {
                        }
                        if (null != _o) {
                            return _o;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn(".restructure(): caught & ignoring " + e);
        }
        return null;
    }

    /** Attempt to map the destructured contents of the map to constructor
     * or setters of the given candidate class.
     * Does not (yet) support parameterized classes, even if
     * <tt>destructure()</tt> includes key parameterizations.
     * @param destructure Map from destructure()
     * @param candidate Class to attempt to initialize
     * @param order String-array giving the order the map keys should be
     * given in the candidate class' constructor.
     * @return Initialized object, or <tt>null</tt> if the candidate class
     * couldn't be initialized, for some reason.
     */
    protected Object restructure(Map destructure, Class candidate, String[] order) {
        Constructor[] _constructors = candidate.getDeclaredConstructors();
        if (0 == _constructors.length) {
            return null;
        }
        int _size = order.length;
        Object[] _args = new Object[_size];
        for (int _i = 0; _i < _size; _i++) {
            _args[_i] = destructure.get(order[_i]);
        }
        outer: for (int _i = 0; _i < _constructors.length; _i++) {
            Constructor _const = _constructors[_i];
            Class[] _params = _const.getParameterTypes();
            if (_params.length == _size) {
                boolean _found = true;
                for (int _j = 0; _j < _size; _j++) {
                    if (null == _args[_j]) {
                        try {
                            String _c = order[_j];
                            String[][] _list = this.getRestructureCandidateNames();
                            String[] _sa = null;
                            for (int _p = 0; _p < _list.length; _p++) {
                                if (_list[_p][0].equals(_c)) {
                                    _sa = new String[_list[_p].length - 1];
                                    System.arraycopy(_list[_p], 1, _sa, 0, _sa.length);
                                    break;
                                }
                            }
                            if (null != _sa) {
                                _args[_j] = this.restructure(destructure, Class.forName(_c), _sa);
                            }
                        } catch (Exception e) {
                            log.warn(".restructure(): while getting " + order[_j] + ", caught & ignoring ", e);
                        }
                        if (null == _args[_j]) {
                            _found = false;
                            break outer;
                        } else {
                            continue;
                        }
                    }
                    if (false == _params[_j].equals(_args[_j].getClass())) {
                        _found = false;
                        break;
                    }
                }
                if (_found) {
                    try {
                        return _const.newInstance((Object[]) _args);
                    } catch (Exception e) {
                        log.warn(".restructure() ignoring ", e);
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    /** Returns the standard list of names of candidate classes for the
 * <tt>restructure(Map,Class)</tt> method.
 * The list elements is an array of Strings, where the first element are
 * class names, and the rest the order of constructor arguments, by
 * names as provided by getters (e.g. if the Spec has a method "getN",
 * then the name would be "N").  The arguments can also refer to
 * parameterclasses.
 * <P>
 * Format implementations may override to  exclude the standard, but
 * should use the <tt>getRestructureCandidateNames(String[])</tt> method
 * to add their own candidates to the standard list.
 */
    protected String[][] getRestructureCandidateNames() {
        synchronized (defaultCandidates) {
            return restructureCandidates;
        }
    }

    private static String[][] defaultCandidates = { { "java.security.spec.DSAPrivateKeySpec", "X", "P", "Q", "G" }, { "java.security.spec.DSAPublicKeySpec", "Y", "P", "Q", "G" }, { "java.security.spec.RSAPrivateCRTKeySpec", "Modulus", "PublicExponent", "PrivateExponent", "PrimeP", "PrimeQ", "PrimeExponentP", "PrimeExponentQ", "CrtCoefficient" }, { "java.security.spec.RSAPrivateKeySpec", "Modulus", "PrivateExponent" }, { "java.security.spec.RSAPublicKeySpec", "Modulus", "PublicExponent" }, { "java.security.spec.X509EncodedKeySpec", "Encoded" }, { "java.security.spec.PKCS8EncodedKeySpec", "Encoded" }, { "javax.crypto.spec.DHPrivateKeySpec", "X", "P", "G" }, { "javax.crypto.spec.DHPublicKeySpec", "Y", "P", "G" }, { "javax.crypto.spec.DHPublicKeySpec", "Y", "P", "G" }, { "org.bouncycastle.jce.spec.ElGamalPrivateKeySpec", "X", "org.bouncycastle.jce.spec.ElGamalParameterSpec" }, { "org.bouncycastle.jce.spec.ElGamalPublicKeySpec", "X", "org.bouncycastle.jce.spec.ElGamalParameterSpec" }, { "org.bouncycastle.jce.spec.ElGamalParameterSpec", "P", "G" } };

    private String[][] restructureCandidates = defaultCandidates;

    /** Adds a list of candidate classes for the
 * <tt>restructure(Map,Class)</tt> methods, with the standard candidates
 * appended.
 * This is a tool method to aid format implementations with converting
 * public/private keys between format- and JCE-representation.
 * <P>
 * See <tt>getRestructureCandidateNames() for a structure description.
 * <P>
 * Format implementations may override to add their own candidates.
 * @param moreCandidates array of string  arrays giving the candidate
 * class specification, or <tt>null</tt> to reset back to the default
 * candidates.
 */
    protected void setRestructureCandidateNames(String[][] moreCandidates) {
        if (null == moreCandidates) {
            this.restructureCandidates = defaultCandidates;
        } else {
            String[][] _default = this.getRestructureCandidateNames();
            String[][] _sa = new String[moreCandidates.length + _default.length][];
            System.arraycopy(moreCandidates, 0, _sa, 0, moreCandidates.length);
            System.arraycopy(_default, 0, _sa, moreCandidates.length, _default.length);
            _default = null;
            synchronized (this.defaultCandidates) {
                this.restructureCandidates = _sa;
            }
        }
    }
}

/** Support class that links an OutputStream to a Signature object, so that
 * writes to the stream updates the signature.
 */
class SignedOutputStream extends OutputStream {

    private Signature sig = null;

    public SignedOutputStream(Signature sig) {
        this.sig = sig;
    }

    public void close() throws IOException {
        this.sig = null;
    }

    public void write(int i) throws IOException {
        try {
            this.sig.update((byte) i);
        } catch (SignatureException e) {
            this.close();
            throw (IOException) new IOException("format.SignedOutputStream.write(): caught " + e).initCause(e);
        }
    }
}

/** Support class that links an OutputStream to a Mac object, so that
 * writes to the stream updates the MAC.
 */
class MacOutputStream extends OutputStream {

    private Mac mac = null;

    public MacOutputStream(Mac mac) {
        this.mac = mac;
    }

    public void close() throws IOException {
        this.mac = null;
    }

    public void write(int i) throws IOException {
        try {
            this.mac.update((byte) i);
        } catch (IllegalStateException e) {
            this.close();
            throw (IOException) new IOException("format.MacOutputStream.write(): caught " + e).initCause(e);
        }
    }
}
