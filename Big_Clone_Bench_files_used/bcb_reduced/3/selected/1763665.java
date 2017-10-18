package obol.format;

import java.io.*;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import obol.lang.Symbol;
import obol.lang.SymbolList;
import obol.lang.SymbolTable;
import obol.lang.SymbolProperties;
import obol.lang.Context;
import obol.lang.ObolTypes;
import obol.tools.Base64;
import obol.tools.Debug;
import obol.tools.Invocation;

/** Toplevel class for formats.
 * Formats are binary representations of messages.
 * Formats only have meaning when associated with script instances.
 * There should exist a default format, which we assume there will be converters
 * for.
 * <P><tt>$Id: FormatBase.java,v 1.7 2008/05/04 15:52:56 perm Exp $</tt>
 */
public class FormatBase extends FormatCrypto {

    private static final String __me = "obol.format.FormatBase";

    /** Debug output object, usable for subclasses.
     */
    protected Debug log = Debug.getInstance(__me);

    /** Name of the default format */
    protected static String defaultFormatName = "Serialization";

    protected static Hashtable registeredFormats = new Hashtable();

    protected boolean specializedGeneratePrecedence = false;

    protected static Eval eval = new Eval();

    protected static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss.SSSZ");

    public static String __status = FormatCrypto.__status;

    static {
        String[][] _formats = { { "Serialization", "obol.format.SerializationFormat" }, { "SPKI", "obol.format.SPKIFormat" } };
        int _formatName = 0, _formatClassName = 1;
        for (int _i = 0; _i < _formats.length; _i++) {
            try {
                registerFormat(_formats[_i][_formatName], Class.forName(_formats[_i][_formatClassName]));
            } catch (Exception e) {
                throw (RuntimeException) new RuntimeException(__me + ".static: caught " + e).initCause(e);
            }
        }
        __status += "\tRegistered Formats: " + getRegisteredFormatNames() + "\n";
        isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /** Return an instance of the given format specification.
     * Currently, only "default" is supported.
     * @param formatspec Format specification (case is insignificant)
     * @return Format instance representing the given format specification.
     * @throws NoSuchFormatException if the format couldn't be found.
     * @throws FormatException if the format couldn't be instantiated for
     * some reason (see thrown exception's cause)
     */
    public static Format getInstance(String formatspec) throws FormatException {
        formatspec = formatspec.toLowerCase();
        if ("default".equals(formatspec)) {
            return getDefaultInstance();
        } else if (registeredFormats.containsKey(formatspec)) {
            try {
                return (Format) ((Class) registeredFormats.get(formatspec)).newInstance();
            } catch (Exception e) {
                throw (FormatException) new FormatException(__me + ".getInstance(): instantiation of \"" + formatspec + "\" failed!").initCause(e);
            }
        } else {
            throw new NoSuchFormatException(__me + ".getInstance(): unknown format \"" + formatspec + "\" (currently only supports \"" + getRegisteredFormatNames() + "\")!");
        }
    }

    /** Return an instance of the default format specification.
     * @return Format instance representing the default format specification.
     * @throws NoSuchFormatException if the format couldn't be found.
     */
    public static Format getDefaultInstance() throws FormatException {
        return getInstance(getDefaultFormatName());
    }

    /** Returns the name of the default format.
     * @return String containing the default format name.
     */
    public static String getDefaultFormatName() {
        return defaultFormatName;
    }

    /** Return encryption/decryption flag.
     * @return <tt>true</tt> if we're doing crypto operations during
     * convert, <tt>false</tt> otherwise.
     */
    public boolean inCrypto() {
        return super.inCrypto();
    }

    /** Returns an enumeration of the provided format names.
     * @return enumeration of format names, as strings.
     */
    public static Enumeration getRegisteredFormatNamesEnumeration() {
        return registeredFormats.keys();
    }

    public static String getRegisteredFormatNames() {
        StringBuffer _sb = new StringBuffer();
        for (Enumeration _e = getRegisteredFormatNamesEnumeration(); _e.hasMoreElements(); ) {
            _sb.append((String) _e.nextElement());
            if (_e.hasMoreElements()) {
                _sb.append(", ");
            }
        }
        return _sb.toString();
    }

    /** Register a format with the factory.
     * @param name name of format to be registered. Will be converted to
     * lowercase.
     * @param format the new format class to register.
     */
    protected static synchronized void registerFormat(String name, Class format) throws FormatException {
        if (Format.class.isAssignableFrom(format)) {
            registeredFormats.put(name.toLowerCase(), format);
        } else {
            throw new FormatException(__me + ".registerFormat(\"" + name + "\", " + format.getName() + "): provided format not a subclass of format.Format!");
        }
    }

    /** Tool method that makes an outputstream buffered, if it isn't
     * already.
     * @param out OutputStream to make buffered.
     * @return Buffered OutputStream;
     */
    public static OutputStream makeBuffered(OutputStream out) {
        if (null == out) {
            return null;
        }
        if (!BufferedOutputStream.class.isAssignableFrom(out.getClass())) {
            return new BufferedOutputStream(out);
        }
        return out;
    }

    /** Tool method that makes an inputstream buffered, if it isn't already.
     * @param in InputStream to make buffered.
     * @return Buffered InputStream;
     */
    public static InputStream makeBuffered(InputStream in) {
        if (null == in) {
            return null;
        }
        if (!BufferedInputStream.class.isAssignableFrom(in.getClass())) {
            return new BufferedInputStream(in);
        }
        return in;
    }

    /** Convert a symbol-value to a Number.
     * This method may update the given symbol's property list!
     * This method can convert Number (a no-op), byte[] (via BigInteger),
     * Strings (in Base8,10,16-encoding).
     * @param Symbol whose value is either a Number, a byte[], or a String
     * containing a Base8, 10 or 16 number.
     * @return byte [].
     * @throws FormatException if something went wrong.
     */
    public static Number convertSymbolValueToNumber(Symbol sym) throws FormatException {
        return convertSymbolValueToNumber(sym, true);
    }

    public static Number convertSymbolValueToNumber(Symbol sym, boolean update) throws FormatException {
        Object _value = sym.getValue();
        if (_value instanceof Number) {
            return (Number) _value;
        }
        String _s = null;
        Class _vc = _value.getClass();
        if (_value instanceof String) {
            _s = (String) _value;
        } else if (_value instanceof Date) {
            return new Long(((Date) _value).getTime());
        } else if ((_vc.isArray() && _vc.getComponentType().isAssignableFrom(byte.class))) {
            BigInteger _bi = new BigInteger((byte[]) _value);
            if (update) {
                sym.setProperty(SymbolProperties.NumberOfBits, String.valueOf(_bi.bitLength()));
            }
            return _bi;
        }
        int _radix = 10;
        int _bits = 32;
        int _slen = _s.length();
        Number _n = null;
        if ('0' == _s.charAt(0) && _slen > 1) {
            if ('x' == _s.charAt(1) || 'X' == _s.charAt(1)) {
                if (_slen <= 2) {
                    throw new FormatException(__me + ".convertSymbolValueToNumber(): " + "truncated hexadecimal value \"" + _s + "\"!");
                }
                _radix = 16;
                _s = _s.substring(2);
                _bits = _slen * 4;
            } else {
                _radix = 8;
                _bits = _slen * 3;
            }
        } else {
            if (_slen > 10) {
                _bits = 64;
                if (_slen > 19) {
                    _bits = 512;
                }
            }
        }
        try {
            if (_bits <= 32) {
                _n = new Integer((int) Long.parseLong(_s, _radix));
                _bits = 32;
            } else if (_bits <= 64) {
                _n = Long.valueOf(_s, _radix);
                _bits = 64;
            } else {
                _n = new BigInteger(_s, _radix);
                _bits = ((BigInteger) _n).bitLength();
            }
        } catch (NumberFormatException e) {
            if (_bits <= 32) {
                _n = Long.valueOf(_s, _radix);
                _bits = 64;
            } else {
                _n = new BigInteger(_s, _radix);
                _bits = ((BigInteger) _n).bitLength();
            }
        }
        if (update) {
            sym.setProperty(SymbolProperties.NumberOfBits, String.valueOf(_bits));
        }
        return _n;
    }

    /** Convert a symbol-value to a byte array. 
     * This method can convert byte[] (a no-op), Strings (encoded in Base64)
     * or Number (not floats!).
     * @param Symbol whose value is either a byte[], a String containing
     * Base64-encoded data, or a Number.
     * @return byte [].
     * @throws FormatException if something went wrong.
     */
    public byte[] convertSymbolValueToBinary(Symbol sym) throws FormatException {
        byte[] _retval = FormatCrypto.convertToBinary(sym.getValue(), (String) sym.getProperty(SymbolProperties.StringEncoding));
        if (null == _retval) {
            throw new FormatException(__me + ".convertSymbolValueToBinary(): symbol \"" + sym.getName() + "\" value has unsupported type " + sym.getValue().getClass().getName());
        }
        return _retval;
    }

    private void checkArgNum(String what, int wanted, int actual) throws FormatException {
        actual--;
        if (actual != wanted) {
            throw new FormatException("generate " + what + " requires " + wanted + " argument" + (wanted > 1 ? "s" : "") + ", not " + actual);
        }
    }

    private void checkArgNum(String what, int wanted, String cmp, int actual) throws FormatException {
        actual--;
        boolean ok = false;
        if ("==".equals(cmp) || "=".equals(cmp)) {
            ok = (wanted == actual);
        } else if ("<".equals(cmp)) {
            ok = (wanted < actual);
        } else if (">".equals(cmp)) {
            ok = (wanted > actual);
        } else if ("<=".equals(cmp)) {
            ok = (wanted <= actual);
        } else if (">=".equals(cmp)) {
            ok = (wanted >= actual);
        } else if ("!=".equals(cmp)) {
            ok = (wanted != actual);
        } else {
            throw new FormatException(__me + ".checkArgNum(): bogus comparison \"" + cmp + "\"!");
        }
        if (false == ok) {
            throw new FormatException("generate " + what + " requires " + wanted + " argument" + (wanted > 1 ? "s" : "") + ", to be " + cmp + " than " + actual);
        }
    }

    private void checkArgType(Object arg, Object wantedType) throws FormatException {
        if (false == wantedType.getClass().isInstance(arg)) {
            throw new FormatException("expected " + "argument of type " + wantedType.getClass() + ", not " + arg.getClass());
        }
    }

    private int parseInteger(String generateWhat, Object arg) throws FormatException {
        try {
            return Integer.parseInt(arg.toString());
        } catch (NumberFormatException e) {
            throw (FormatException) new FormatException(__me + ".generate() " + generateWhat + ": failed parsing \"" + arg + "\" as integer").initCause(e);
        }
    }

    /** Default least-precedence entry-point for data generation.
     * Format implementations should override this to handle their own
     * generation types.
     * Any class overriding this method <em>must</em> forward unhandled
     * calls to their super-class.
     * @param specs Variable number of specification data (strings, numbers).  A
     * zero-spec is illegal.
     * @return Array with three elements, index 0 being Object generated
     * according to spec, index 1 a String naming its type, and index 2
     * a Map containing properties, or null if none (or an empty Map).
     * @throws FormatException if something went wrong
     */
    public Object[] generate(Object[] specs, Context ctx, boolean symLookup) throws FormatException {
        if (null == specs) {
            throw new FormatException(__me + ".generate(): null spec!");
        }
        if (0 == specs.length) {
            throw new FormatException(__me + ".generate(): zero-length spec!");
        }
        SymbolTable _symtab = ctx.getSymbolTable();
        Object[] _retval = new Object[3];
        _retval[2] = null;
        String _what = specs[0].toString();
        if (ObolTypes.Nonce.equals(_what)) {
            this.checkArgNum(_what, 1, specs.length);
            int _bits = parseInteger(_what, specs[1]);
            int _baLen = (_bits >> 3) + (0 != (_bits & 7) ? 1 : 0);
            byte[] _ba = new byte[_baLen];
            this.getSecureRandom().nextBytes(_ba);
            if (0 != (_bits & 7)) {
                _ba[0] &= (0x0ff >> (8 - (_bits & 7)));
            }
            _retval[0] = _ba;
            _retval[1] = ObolTypes.Nonce;
            HashMap _prop = new HashMap(2);
            _retval[2] = _prop;
            _prop.put(SymbolProperties.NumberOfBits, Integer.toString(_bits));
            return _retval;
        } else if (ObolTypes.SharedKey.equals(_what)) {
            this.checkArgNum(_what, 2, specs.length);
            String _alg = specs[1].toString();
            int _size = parseInteger(_what, specs[2]);
            try {
                _retval[0] = this.generateKey(_alg, _size);
                _retval[1] = ObolTypes.SharedKey;
                HashMap _prop = new HashMap(2);
                _retval[2] = _prop;
                _prop.put(SymbolProperties.NumberOfBits, specs[2]);
                _prop.put(SymbolProperties.AlgorithmName, _alg);
                return _retval;
            } catch (Exception e) {
                throw (FormatException) new FormatException(__me + ".generate() " + _what + " key \"" + _alg + "\" of size " + _size + ".").initCause(e);
            }
        } else if (ObolTypes.KeyPair.equals(_what)) {
            this.checkArgNum(_what, 2, "<=", specs.length);
            String _alg = specs[1].toString();
            int _size = parseInteger(_what, specs[2]);
            HashMap _genparams = null;
            if (specs.length > 3) {
                Object[] _params = new Object[specs.length - 3];
                for (int _i = 0; _i < _params.length; _i++) {
                    _params[_i] = specs[3 + _i];
                }
                _genparams = new HashMap();
                if (_params.length % 4 != 0) {
                    throw new FormatException(__me + ".generate(): " + _what + ", unbalanced bogus GenParameter specification (" + _params.length + ")!");
                }
                for (int _i = 0; _i < _params.length; _i++) {
                    Object _o = _params[_i];
                    if (_o.toString().equals("(")) {
                        String _key = _params[++_i].toString().toLowerCase();
                        Object _val = _params[++_i];
                        if (symLookup && _symtab.exists(_val.toString())) {
                            _val = _symtab.getSymbol(_val.toString()).getValue();
                        }
                        _genparams.put(_key, _val);
                        _i++;
                        if (false == _params[_i].toString().equals(")")) {
                            throw new FormatException(__me + ".generate(): " + _what + ", missing terminating parens of GenParameter " + "specification \"" + _key + "\" (got \"" + _params[_i] + "\" instead).");
                        }
                    } else {
                        throw new FormatException(__me + ".generate(): " + _what + ", bogus GenParameter specification \"" + _o.toString() + "\".");
                    }
                }
            }
            try {
                _retval[0] = this.generateKeyPair(_alg, _size, _genparams);
                _retval[1] = ObolTypes.KeyPair;
                HashMap _prop = new HashMap(2);
                _retval[2] = _prop;
                _prop.put(SymbolProperties.NumberOfBits, specs[2]);
                _prop.put(SymbolProperties.AlgorithmName, _alg);
                return _retval;
            } catch (Exception e) {
                throw (FormatException) new FormatException(__me + ".generate() " + _what + " keypair \"" + _alg + "\" of size " + _size + ".").initCause(e);
            }
        } else if (ObolTypes.KeyAgreement.equals(_what)) {
            this.checkArgNum(_what, 3, "<=", specs.length);
            String _alg = specs[1].toString();
            Key[] _keys = new Key[specs.length - 3];
            try {
                for (int _i = 0; _i < _keys.length; _i++) {
                    _keys[_i] = (Key) specs[_i + 3];
                }
                byte[] _agreed = this.keyAgreement(_alg, (Key) specs[2], _keys);
                _retval[0] = _agreed;
                _retval[1] = ObolTypes.Binary;
                HashMap _prop = new HashMap(2);
                _retval[2] = _prop;
                _prop.put(SymbolProperties.NumberOfBits, String.valueOf(_agreed.length * 8));
                _prop.put(SymbolProperties.AlgorithmName, _alg);
                return _retval;
            } catch (Exception e) {
                throw (FormatException) new FormatException(__me + ".generate() " + _what + " \"" + _alg + "\".").initCause(e);
            }
        } else if (ObolTypes.Timestamp.equals(_what)) {
            this.checkArgNum(_what, 0, "<=", specs.length);
            if (0 == (specs.length - 1)) {
                _retval[0] = new Date();
                _retval[1] = ObolTypes.Timestamp;
            } else if (1 == (specs.length - 1)) {
                String _kind = specs[1].toString().toLowerCase();
                if ("ns".equals(_kind) || "nsec".equals(_kind) || "nano".equals(_kind) || "nanosec".equals(_kind) || "nanosecond".equals(_kind)) {
                    _retval[0] = new Long(System.nanoTime());
                    _retval[1] = ObolTypes.Number;
                } else if ("ms".equals(_kind) || "msec".equals(_kind) || "milli".equals(_kind) || "millis".equals(_kind) || "millisec".equals(_kind) || "millisecond".equals(_kind)) {
                    _retval[0] = new Long(System.currentTimeMillis());
                    _retval[1] = ObolTypes.Number;
                } else if ("iso".equals(_kind) || "isodate".equals(_kind)) {
                    _retval[0] = isoDateFormat.format(new Date());
                    _retval[1] = ObolTypes.Timestamp;
                } else {
                    throw (FormatException) new FormatException(__me + ".generate() " + _what + " timestamp kind \"" + _kind + "\", only supports ms/ns/iso");
                }
            } else {
                throw (FormatException) new FormatException(__me + ".generate() " + _what + " unsupported number of arguments (" + (specs.length - 1) + ", support 0 or 1)!");
            }
            return _retval;
        } else if (ObolTypes.Hash.equals(_what)) {
            this.checkArgNum(_what, 2, "<=", specs.length);
            String _alg = specs[1].toString();
            try {
                MessageDigest _hashSession = this.initHashSession(_alg);
                for (int _i = 0; _i < specs.length - 2; _i++) {
                    Symbol _sym = null;
                    String _obj = specs[_i + 2].toString();
                    if (symLookup && _symtab.exists(_obj)) {
                        _sym = _symtab.getSymbol(_obj);
                    } else {
                        _sym = new Symbol(null, _obj);
                        this.guessValueType(_sym);
                    }
                    this.updateHashSession(_hashSession, convertSymbolValueToBinary(_sym));
                }
                byte[] _md = this.generateHash(_hashSession);
                _retval[0] = _md;
                _retval[1] = ObolTypes.Binary;
                HashMap _prop = new HashMap(2);
                _prop.put(SymbolProperties.NumberOfBits, String.valueOf(_md.length * 8));
                _retval[2] = _prop;
                return _retval;
            } catch (Exception e) {
                e.printStackTrace();
                StringBuffer _sb = new StringBuffer();
                for (int _i = 2; _i <= specs.length; _i++) {
                    if (0 != _sb.length()) {
                        _sb.append(", ");
                    }
                    _sb.append(specs[_i]);
                }
                throw (FormatException) new FormatException(__me + ".generate() " + _what + " algorithm=\"" + _alg + "\" of \"" + _sb + "\".").initCause(e);
            }
        } else if (ObolTypes.Eval.equals(_what)) {
            this.checkArgNum(_what, specs.length, ">=", 2);
            try {
                String _evalName = this.eval.lookupActualName(specs[1].toString());
                String _statement = specs[2].toString();
                Symbol[] _args = null;
                int _argNum = specs.length - 3;
                int _argStart = 3;
                if (_argNum > 0) {
                    _args = new Symbol[_argNum];
                    for (int _i = 0; _i < _argNum; _i++) {
                        String _symName = specs[_argStart + _i].toString();
                        if (_symtab.exists(_symName)) {
                            _args[_i] = _symtab.getSymbol(_symName);
                        } else {
                            throw new FormatException(__me + ".generate(): " + "arguments contain unknown symbol \"" + _symName + "\"!");
                        }
                    }
                }
                if (true) {
                    if (_symtab.exists(_evalName)) {
                        this.log.info("generate(): symbol eval-interpreter, " + "will dereference symbol \"" + _evalName + "\" as interpreter name.");
                        _evalName = _symtab.getSymbol(_evalName).getValue().toString();
                    }
                    if (_symtab.exists(_statement)) {
                        this.log.info("generate(): symbol eval-statement, " + "will dereference symbol \"" + _statement + "\" as eval-statement.");
                        _statement = _symtab.getSymbol(_statement).getValue().toString();
                    }
                }
                Symbol _sym = this.eval.eval(this.eval.lookupActualName(_evalName), _statement, _args);
                _retval[0] = _sym.getValue();
                _retval[1] = _sym.getType();
                _retval[2] = _sym.getProperties();
                return _retval;
            } catch (Exception e) {
                this.log.debug("generate(): caught ", e);
                e.printStackTrace();
                throw (FormatException) new FormatException(__me + ".generate() eval threw " + e).initCause(e);
            }
        }
        StringBuffer _sb = new StringBuffer();
        for (int _i = 0; _i < specs.length; _i++) {
            _sb.append(specs[_i]).append(' ');
        }
        throw new UnknownGenerationSpecificationException(__me + ".generateDefault(): unknown specification \"" + _sb + "\"!");
    }

    private static String[] generatesupportedtypes = { ObolTypes.Nonce, ObolTypes.KeyPair, ObolTypes.SharedKey, ObolTypes.Timestamp, ObolTypes.Hash };

    /** Returns an array of Strings listing the supported types of this
     * Format's generate implementation.
     * @return String array of supported belief types.
     */
    public String[] generateSupportedTypes() {
        return generatesupportedtypes;
    }

    /** Loads data from a File into the given Symbol-value.
     * This method is called by the parser before the believe() method.
     * The argument Symbol <em>must</em> have a symbol-value that is a
     * java.io.File object (provided by the parser), which contents then
     * replaces the symbol-value.
     * The original File object is placed in the symbol property
     * SymbolProperties.ValueFromFile.
     * <P>This method is called by the parser as part of obtaining data
     * for the symbol being <tt>believe</tt>'d (the datasource).
     * If the symbol has the <tt>:raw</tt> property (a monoide), 
     * the data is assumed to be binary and read into a byte-array in the
     * symbol-value.
     * Otherwise, the data is assumed to be of the current format,
     * optionally of the format specified by the <tt>:format</tt> property.
     * Format implementations <em>can</em> override this method, in order to
     * do their own preprocessing on the byte array or symbol properties,
     * but the symbol-value <em>must</em> a byte-array (for subsequent
     * typing by believe).
     * <P>
     * The number of bytes read are determined by first examining the
     * symbol's SymbolProperties.NumberOfBytes property, then the symbol's 
     * SymbolProperties.NumberOfBits property, the latter rounded upwards to
     * get a whole number of bytes.
     * If neither properties are present, the file is read until EOF.
     * <B>NB:</B> Skipping data (indexed start of read) is not supported.
     * @param sym symbol instance to examine and load data into.
     * @throws FormatException if something went wrong.
     */
    public void loadFile(Symbol sym) throws FormatException {
        Object _o = sym.getValue();
        if (false == (_o instanceof File)) {
            throw new FormatException(__me + ".loadFile(): Symbol \"" + sym + "\" does not contain File value!");
        }
        File _f = (File) _o;
        sym.setProperty(SymbolProperties.ValueFromFile, _f);
        try {
            FileInputStream _fin = new FileInputStream(_f);
            this.loadStream(sym, _fin, determineReadLength(sym));
            _fin.close();
        } catch (IOException e) {
            throw (FormatException) new FormatException(__me + ".loadfile(): " + "Failure loading file \"" + _f + "\" into symbol \"" + sym + "\"!").initCause(e);
        }
    }

    /** Like loadFile(), but expects the symbol's value to be an instance of
     * java.io.InputStream, and read from it.
     * The number of bytes read are determined by first examining the
     * symbol's SymbolProperties.NumberOfBytes property, then the symbol's 
     * SymbolProperties.NumberOfBits property, the latter rounded upwards to
     * get a whole number of bytes.
     * If neither properties are present, the stream is read until EOF.
     */
    public void loadStream(Symbol sym) throws FormatException {
        Object _o = sym.getValue();
        if (false == (_o instanceof InputStream)) {
            throw new FormatException(__me + ".loadStream(): Symbol \"" + sym + "\"'s value expected to be of InputStream, not " + _o.getClass() + "!");
        }
        try {
            this.loadStream(sym, (InputStream) _o, this.determineReadLength(sym));
        } catch (IOException e) {
            throw (FormatException) new FormatException(__me + ".loadStream(): " + "Failure loading from stream into symbol \"" + sym + "\"").initCause(e);
        }
    }

    private int determineReadLength(Symbol sym) {
        int _l = -1;
        if (sym.existProperty(SymbolProperties.NumberOfBytes)) {
            _l = Integer.parseInt((String) sym.getProperty(SymbolProperties.NumberOfBytes));
        } else if (sym.existProperty(SymbolProperties.NumberOfBits)) {
            int _i = Integer.parseInt((String) sym.getProperty(SymbolProperties.NumberOfBits));
            _l = _i >> 3;
            if (0 != (_i & 0x03)) {
                _l++;
            }
        }
        return _l;
    }

    /** Read data from an InputStream into the given symbol's value, in the
     * manner specified by the symbol's properties.
     * The possible properties are FormatKeyword or RawKeyword.
     * @param targetSym symbol to place data in.
     * @param in InputStream to read from
     * @param len number of bytes to read, or -1 to read to EOF.
     * @throws FormatException if something went wrong, format-wise
     * @throws IOException if something went wrong, IO-wise.
     */
    public void loadStream(Symbol targetSym, InputStream in, int len) throws FormatException, IOException {
        boolean _raw = targetSym.existProperty(SymbolProperties.RawKeyword);
        Format _fmt = null;
        if (targetSym.existProperty(SymbolProperties.FormatKeyword)) {
            _fmt = (Format) targetSym.getProperty(SymbolProperties.FormatKeyword);
            if (_raw) {
                throw new FormatException(__me + ".loadStream(): Symbol \"" + targetSym.getName() + "\" has both " + SymbolProperties.FormatKeyword + " and " + SymbolProperties.RawKeyword + " properties, but can have only one!");
            }
        }
        if (_raw) {
            this.loadBinaryStream(targetSym, in, len);
        } else if (null != _fmt) {
            this.loadFormattedStream(targetSym, in, _fmt);
        } else {
            throw new RuntimeException(__me + ".loadStream(): Symbol \"" + targetSym.getName() + "\" has neither" + SymbolProperties.FormatKeyword + " nor" + SymbolProperties.RawKeyword + " properties, so this method shouldn't be called!!");
        }
    }

    /** Read data from an InputStream into a byte array, which will be the the
     * provided symbol's value.
     * No typing is performed, just the loading.
     * @param targetSym symbol to place data in.
     * @param in InputStream to read from
     * @param len number of bytes to read, or -1 to read to EOF.
     * @throws FormatException if something went wrong, format-wise
     * @throws IOException if something went wrong, IO-wise.
     */
    public void loadBinaryStream(Symbol targetSym, InputStream in, int len) throws FormatException, IOException {
        in = makeBuffered(in);
        ByteArrayOutputStream _bout = new ByteArrayOutputStream();
        int _i = 0;
        int _cnt = 0;
        while (true) {
            if (-1 != len && _cnt == len) {
                break;
            }
            _i = in.read();
            if (-1 == _i) {
                if (len != -1) {
                    throw new FormatException(__me + ".loadStream(): Symbol \"" + targetSym + "\"'s input " + "prematurely exhausted " + "(read " + _cnt + " bytes of " + len + ")");
                }
                break;
            }
            _bout.write(_i);
            _cnt++;
        }
        targetSym.setValue(_bout.toByteArray());
        _bout.close();
    }

    /** Read data from an InputStream into the given symbol's value, using
     * the given format (reading a single format-dictated data component).
     * Actually reads all symbols stored in the stream, but only acts on the
     * first one (index 0).
     * @param targetSym symbol to place data in.
     * @param in InputStream to read from
     * @param format Format to use.
     * @throws FormatException if something went wrong, format-wise
     * @throws IOException if something went wrong, IO-wise.
     */
    public void loadFormattedStream(Symbol targetSym, InputStream in, Format format) throws FormatException, IOException {
        SymbolList _symbols = format.convert(in);
        Symbol _readsymbol = _symbols.getSymbol(0);
        targetSym.setValue(_readsymbol.getValue());
        _readsymbol.mergePropertiesTo(targetSym);
    }

    /** Default least-precedence entry-point for believing something about a
     * symbol.
     * Typically used to type-promote a symbol's value.
     * This method modifies the argument symbol's values.
     * promote String symbol-values into Java Number, String, or byte[].
     * <P>
     * The following types are supported (with Java counterparts in parens):
     * <tt>string</tt> (String), <tt>number</tt> (Integer, Long,
     * BigInteger), <tt>binary</tt> (byte[]).
     * Also, <tt>shared-key</tt> (Key or SecretKey), <tt>public-key</tt>
     * (PublicKey), and <tt>private-key</tt>(PrivteKey) are supported, with
     * the optional somewhat magical <tt>:serialized</tt> symbol property
     * keyword which will allow serialized Java key types.
     * <P>
     * Format implementations should override this method to handle their
     * own types, or formats on types.
     * Any class overriding this method <em>must</em> forward unhandled
     * calls to their super-class.
     * <P><B>TODO:</B> XXX Determine how to deal with modified symbols: is
     * that legal, or should callers always use the returned symbol
     * reference (which might be a new symbol)?
     * @param ctx the symbol's Context (ie assoiates symbol table, etc)
     * @param sym Symbol to believe something about.
     * @param spec variable number of arguments to believe about symbol, or
     * <tt>null</tt> if the symbol's value and type should be examined and
     * acted upon (eg if value is a File, load that file and process
     * according to what the SymbolProperties.Type property says. <B>NOTE</B>
     * This default implmementation of Format.believe ignores this parameter.
     * @return Symbol reference to the symbol we believed something about
     */
    public Symbol believe(Context ctx, Symbol sym, Object[] specs) throws FormatException {
        String _type = sym.getType();
        if (null == _type) {
            throw new FormatException(__me + ".believe(): Symbol " + sym + " has no type!");
        }
        Object _value = sym.getValue();
        if (null == _value) {
            if (sym.isAnonymous()) {
                if (sym.existProperty(SymbolProperties.PreBelieved)) {
                    return sym;
                }
            }
            throw new FormatException(__me + ".believe(): Symbol " + sym + " has no value!");
        }
        Class _vc = _value.getClass();
        if (ObolTypes.LiteralString.equals(_type)) {
            sym = this.believeLiteralstring(ctx, sym);
        } else if (ObolTypes.HostPort.equals(_type) || ObolTypes.FileOut.equals(_type) || ObolTypes.FileIn.equals(_type) || ObolTypes.FileInOut.equals(_type) || ObolTypes.URI.equals(_type)) {
            sym = this.believeNetwork(ctx, sym);
        } else if (ObolTypes.SharedKey.equals(_type) || ObolTypes.PrivateKey.equals(_type) || ObolTypes.PublicKey.equals(_type)) {
            sym = this.believeKey(ctx, sym);
        } else if (ObolTypes.Binary.equals(_type)) {
            sym = this.believeBinary(ctx, sym);
        } else if (ObolTypes.Number.equals(_type)) {
            sym = this.believeNumber(ctx, sym);
        } else if (ObolTypes.Nonce.equals(_type)) {
            sym = this.believeBinary(ctx, sym);
            sym.setProperty(SymbolProperties.Type, ObolTypes.Nonce);
        } else if (ObolTypes.Timestamp.equals(_type)) {
            if (false == sym.getValue() instanceof Date) {
                Date _d = null;
                if (sym.getValue() instanceof String) {
                    String _s = (String) sym.getValue();
                    try {
                        _d = DateFormat.getDateInstance().parse(_s);
                    } catch (Exception e) {
                        _d = null;
                    }
                    if (null == _d) {
                        try {
                            _d = isoDateFormat.parse(_s);
                        } catch (Exception e) {
                            _d = null;
                        }
                    }
                }
                if (null == _d) {
                    sym = this.believeNumber(ctx, sym);
                    sym.setValue(new Date(((Number) sym.getValue()).longValue()));
                }
                sym.setProperty(SymbolProperties.Type, ObolTypes.Timestamp);
            }
        } else {
            throw new FormatException(__me + ".believe(): " + "unknown or unsupported type \"" + _type + "\"!");
        }
        sym.setProperty(SymbolProperties.Context, ctx);
        return sym;
    }

    /** Called  by believe.  Type of symbol must be ObolTypes.Binary.
     * @param ctx the symbol's Context (ie assoiates symbol table, etc)
     * @param sym Symbol to believe something about.
     */
    protected Symbol believeBinary(Context ctx, Symbol sym) throws FormatException {
        sym.setValue(convertSymbolValueToBinary(sym));
        return sym;
    }

    /** Called  by believe.  Type of symbol must be ObolTypes.Number.
     * @param ctx the symbol's Context (ie assoiates symbol table, etc)
     * @param sym Symbol to believe something about.
     */
    protected Symbol believeNumber(Context ctx, Symbol sym) throws FormatException {
        if (false == sym.getValue() instanceof Number) {
            sym.setValue(convertSymbolValueToNumber(sym));
        }
        return sym;
    }

    /** Called  by believe.  Type of symbol must be ObolTypes.LiteralString.
     * @param ctx the symbol's Context (ie assoiates symbol table, etc)
     * @param sym Symbol to believe something about.
     */
    protected Symbol believeLiteralstring(Context ctx, Symbol sym) throws FormatException {
        Object _value = sym.getValue();
        Class _vc = _value.getClass();
        String _s = null;
        if ((_vc.isArray() && _vc.getComponentType().isAssignableFrom(byte.class))) {
            String _enc = (String) sym.getProperty(SymbolProperties.StringEncoding);
            if (null == _enc) {
                _enc = "ISO8859_1";
            }
            try {
                _s = new String((byte[]) _value, _enc);
            } catch (UnsupportedEncodingException e) {
                throw new FormatException(__me + ".believe(): failure converting symbol \"" + sym + "\"'s binary value to String using " + "encoding \"" + _enc + "\"!");
            }
        } else {
            _s = _value.toString();
        }
        sym.setValue(_s);
        return sym;
    }

    /** Called  by believe.  Type of symbol must be ObolTypes.SharedKey,
     * ObolTypes.PrivateKey or ObolTypes.PublicKey.
     * @param ctx the symbol's Context (ie assoiates symbol table, etc)
     * @param sym Symbol to believe something about.
     */
    protected Symbol believeKey(Context ctx, Symbol sym) throws FormatException {
        Object _value = sym.getValue();
        Class _vc = _value.getClass();
        String _type = sym.getType();
        try {
            if (_value instanceof Number || _value instanceof String) {
                sym.setValue(convertSymbolValueToBinary(sym));
            }
            this.convertToCryptoTypes(sym);
        } catch (Exception e) {
            throw (FormatException) new FormatException(__me + ".believeKey(): unable to convert type \"" + _type + "\" to corresponding internal " + "JCE object instance!").initCause(e);
        }
        return sym;
    }

    /** Called  by believe.  Type of symbol must be ObolTypes.HostPort,
     * ObolTypes.FileOut, ObolTypes.FileIn, ObolTypes.FileInOut, or ObolTypes.URI.
     * @param ctx the symbol's Context (ie assoiates symbol table, etc)
     * @param sym Symbol to believe something about.
     */
    protected Symbol believeNetwork(Context ctx, Symbol sym) throws FormatException {
        try {
            sym.setProperty(SymbolProperties.PeerHandle, PeerHandle.getInstance(sym, ctx.getCurrentFormat()));
        } catch (Exception e) {
            throw (FormatException) new FormatException(__me + ".believe(): unable to initialize symbol \"" + sym.getName() + "\" of type \"" + sym.getType() + "\"!").initCause(e);
        }
        return sym;
    }

    private static String[] believesupportetypes = { ObolTypes.LiteralString, ObolTypes.Number, ObolTypes.Binary, ObolTypes.PublicKey, ObolTypes.PrivateKey, ObolTypes.SharedKey, ObolTypes.HostPort, ObolTypes.URI, ObolTypes.FileOut, ObolTypes.FileIn, ObolTypes.FileInOut };

    public String[] believeSupportedTypes() {
        return (String[]) believesupportetypes.clone();
    }

    /** Sign message components using the given signature key.
     * The signature key can be a private (asymmetric) key, for generating
     * digital signatures; a shared (symmetric) key, for generating a MAC;
     * or a string, for generating a hash of the message compontents in the
     * current format.
     * @param signkey key used to sign messages.
     * @param msgparts message components to sign.
     * @return byte array containing binary signature value.
     */
    public byte[] sign(Symbol signkey, SymbolList msgparts) throws FormatException {
        OutputStream _out = null;
        try {
            Object _key = signkey.getValue();
            if (_key instanceof PrivateKey) {
                Signature _sig = this.getSignature(signkey);
                _sig.initSign((PrivateKey) _key, this.getSecureRandom());
                _out = this.getLinkedSigningStream(_sig, signkey);
                ((Format) this).convert(msgparts, _out);
                return _sig.sign();
            } else if (_key instanceof SecretKey) {
                Mac _mac = this.getMac(signkey);
                _mac.init((SecretKey) _key);
                _out = this.getLinkedMACStream(_mac, signkey);
                ((Format) this).convert(msgparts, _out);
                return _mac.doFinal();
            } else if (_key instanceof String) {
                MessageDigest _md = MessageDigest.getInstance((String) _key);
                ByteArrayOutputStream _bout = new ByteArrayOutputStream();
                _out = new DigestOutputStream(_bout, _md);
                ((Format) this).convert(msgparts, _out);
                _out.flush();
                _bout.close();
                return _md.digest();
            } else {
                throw new FormatException(__me + ".sign(): Bogus key " + "value type " + _key.getClass().getName());
            }
        } catch (Exception e) {
            throw (FormatException) new FormatException().initCause(e);
        } finally {
            try {
                if (null != _out) {
                    _out.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /** Verify a signature on message components.
     * The signature key can be a public (asymmetric) key, for verifying
     * digital signatures; a shared (symmetric) key, for verifying a MAC;
     * or a string, for verifying the a hash of the message compontents in the
     * current format.
     * @param signature byte array containing binary signature value
     * @param verifykey key used to verify signature.
     * @param msgparts Message components to verify signature on.
     * @returns <tt>true</tt> if the signature verifies, <tt>false</tt> if
     * not.
     */
    public boolean verify(byte[] signature, Symbol verifykey, SymbolList msgparts) throws FormatException {
        OutputStream _out = null;
        try {
            Object _key = verifykey.getValue();
            if (_key instanceof PublicKey) {
                Signature _sig = this.getSignature(verifykey);
                _sig.initVerify((PublicKey) _key);
                _out = this.getLinkedSigningStream(_sig, verifykey);
                ((Format) this).convert(msgparts, _out);
                return _sig.verify(signature);
            } else if (_key instanceof SecretKey) {
                Mac _mac = this.getMac(verifykey);
                _mac.init((SecretKey) _key);
                _out = this.getLinkedMACStream(_mac, verifykey);
                ((Format) this).convert(msgparts, _out);
                return Arrays.equals(signature, _mac.doFinal());
            } else if (_key instanceof String) {
                return Arrays.equals(signature, this.sign(verifykey, msgparts));
            } else {
                throw new FormatException(__me + ".verify(): Bogus key " + "value type " + _key.getClass().getName());
            }
        } catch (Exception e) {
            throw (FormatException) new FormatException().initCause(e);
        } finally {
            try {
                if (null != _out) {
                    _out.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /** Guess the symbol value's type.
     * The default implementation does the guessing based on the value's
     * object class.
     */
    public String guessValueType(Symbol sym) throws FormatException {
        Object _value = sym.getValue();
        Class _vClass = _value.getClass();
        String _type = null;
        if (_vClass.isArray()) {
            if ((_vClass.isArray() && _vClass.getComponentType().isAssignableFrom(byte.class))) {
                _type = ObolTypes.Binary;
            }
        } else if (_value instanceof Number) {
            _type = ObolTypes.Number;
        } else if (_value instanceof String) {
            _type = ObolTypes.LiteralString;
        } else if (_value instanceof Key) {
            if (_value instanceof SecretKey) {
                _type = ObolTypes.SharedKey;
            } else if (_value instanceof PublicKey) {
                _type = ObolTypes.PublicKey;
            } else if (_value instanceof PrivateKey) {
                _type = ObolTypes.PrivateKey;
            }
        } else if (_value instanceof InetAddress) {
            _type = ObolTypes.HostPort;
        }
        log.debug("guessValueType(): guessed type of " + sym + "'s value (" + _vClass + ") = " + _type);
        return _type;
    }

    /** Default identicalElements method just forwards the comparison to
     * <tt>Symbol.equal()</tt>.
     */
    public boolean identicalElements(Symbol a, Symbol b) {
        return a.equal(b);
    }
}
