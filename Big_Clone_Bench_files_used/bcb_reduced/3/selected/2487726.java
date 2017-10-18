package obol.lang;

import java.lang.reflect.Field;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import obol.format.Format;
import obol.format.FormatBase;
import obol.format.FormatListener;
import obol.format.FormatException;
import obol.format.PeerHandle;
import obol.format.MessagePool;
import obol.format.MessagePoolEntry;
import obol.parser.Parser;
import obol.parser.ObolParser;
import obol.tools.Hex;
import obol.tools.Debug;

/** Obol runtime.
 * @version $Id: Runtime.java,v 1.5 2008/06/22 20:45:57 perm Exp $
 */
public class Runtime implements API {

    public static final String __me = "obol.lang.Runtime";

    protected Debug log = Debug.getInstance(__me);

    private Hashtable loadedScripts = new Hashtable();

    private Hashtable loadedScriptsInfo = new Hashtable();

    private String scriptDigestAlgo = "SHA1";

    private static ThreadGroup receiveGroup = new ThreadGroup("ReceiveRunners");

    private static Runtime currentRuntime = new Runtime();

    private static SymbolPool currentSymbolPool = null;

    private static MessagePool currentMessagePool = new MessagePool();

    private Hashtable registeredListeners = new Hashtable();

    /** Only one instance of this class per JVM. */
    private Runtime() {
        String[] _status = { "obol.versionInfo", "obol.format.FormatBase", "obol.format.Eval" };
        StringBuffer _sb = new StringBuffer();
        for (int _i = 0; _i < _status.length; _i++) {
            try {
                _sb.append((String) Class.forName(_status[_i]).getField("__status").get(null));
            } catch (Exception e) {
                this.log.fatal("(): Runtime FUBAR: unable to read status field of " + _status[_i] + ", caused " + e, e);
                throw new RuntimeException(__me + "(): unable to read status field!");
            }
        }
        System.err.println("Obol v1.1, (C)2002-2008 Per Harald Myrvang <permyr.gmail@com>");
        System.err.println("Config: \n" + _sb.toString());
    }

    /** Return the Runtime instance associated with the currently running JVM. */
    public static Runtime getInstance() {
        return currentRuntime;
    }

    /** Return a digest of the loaded named script.
     * @param name name of script
     * @return digest of loaded script, of the form
     * "<em>digest-algo</em>:<em>digest-in-hex</em>/<em>length</em>".
     */
    protected String getLoadedScriptDigestString(String name) {
        return ((LoadedScriptInfo) loadedScriptsInfo.get(name)).getDigest();
    }

    private String makeVersionedKey(String name, String version) {
        return ((null == version) ? name : (name + " version " + version));
    }

    /** Tell the obol runtime to load and parse a script, and return the
     * script's textual name. */
    public LoadedScriptInfo loadScript(String name, URI uri) throws ObolException, IOException {
        return this.loadScript(name, uri.toURL());
    }

    public LoadedScriptInfo loadScript(String name, URL url) throws ObolException, IOException {
        return this.loadScript(name, url.openStream());
    }

    public LoadedScriptInfo loadScript(String name, File file) throws ObolException, IOException {
        return this.loadScript(name, new FileInputStream(file));
    }

    public LoadedScriptInfo loadScript(String name, String filename) throws ObolException, IOException {
        return this.loadScript(name, new FileInputStream(filename));
    }

    public synchronized LoadedScriptInfo loadScript(String name, InputStream in) throws ObolException, IOException {
        LoadedScriptInfo _retval = null;
        try {
            while (true) {
                LoadedScriptInfo _i = this.loadScript(name, null, in);
                if (null == _i) {
                    break;
                }
                if (null == _retval) {
                    _retval = _i;
                }
            }
        } catch (EOFException e) {
        }
        return _retval;
    }

    public LoadedScriptInfo loadScript(String name, String version, URI uri) throws ObolException, IOException {
        return this.loadScript(name, version, uri.toURL());
    }

    public LoadedScriptInfo loadScript(String name, String version, URL url) throws ObolException, IOException {
        return this.loadScript(name, version, url.openStream());
    }

    public LoadedScriptInfo loadScript(String name, String version, File file) throws ObolException, IOException {
        return this.loadScript(name, version, new FileInputStream(file));
    }

    public LoadedScriptInfo loadScript(String name, String version, String filename) throws ObolException, IOException {
        return this.loadScript(name, version, new FileInputStream(filename));
    }

    public synchronized LoadedScriptInfo loadScript(String name, String version, InputStream in) throws ObolException, IOException {
        ScriptInputStream _scriptIn = null;
        if (in instanceof ScriptInputStream) {
            _scriptIn = (ScriptInputStream) in;
        } else {
            _scriptIn = new ScriptInputStream(in);
        }
        byte[] _rawscript = _scriptIn.findScript(name, version);
        if (null == _rawscript) {
            log.debug(".loadScript(): returning null!");
            return null;
        }
        ByteArrayInputStream _bin = new ByteArrayInputStream(_rawscript);
        Parser _parser = ObolParser.getInstance(_bin);
        _parser.setParseOnly(true);
        DummyContext _ctx = new DummyContext();
        _parser.setCurrentContext(_ctx);
        _parser.parse("scriptHeader");
        _ctx.clear();
        _bin.close();
        String _name = _parser.getScriptName();
        String _documentation = _parser.getScriptDocumentation();
        String _version = _parser.getScriptVersion();
        if (null != version && null != _version) {
            if (false == version.equals(_version)) {
                log.debug(".loadScript(): version mismatch, returning null!");
                return null;
            }
        }
        byte[] _digestBinary = null;
        try {
            MessageDigest _md = MessageDigest.getInstance(this.scriptDigestAlgo);
            _digestBinary = _md.digest(_rawscript);
        } catch (GeneralSecurityException e) {
            throw (ObolException) new ObolException(__me + ".loadScript():" + " error generating " + this.scriptDigestAlgo + " digest of loaded script!").initCause(e);
        }
        String _digest = (this.scriptDigestAlgo + ":" + Hex.toString(_digestBinary) + "/" + _rawscript.length);
        LoadedScriptInfo _info = new LoadedScriptInfoImpl(_name, _version, _documentation, _digest, _scriptIn.getScriptLineNumber());
        boolean _checkDuplicates = true;
        if (false == this.loadedScripts.containsKey(_name)) {
            this.loadedScripts.put(_name, _rawscript);
            this.loadedScriptsInfo.put(_name, _info);
            _checkDuplicates = false;
        }
        String _versionedKey = _name;
        if (null != _version) {
            _versionedKey = this.makeVersionedKey(_name, _version);
            if (false == this.loadedScripts.containsKey(_versionedKey)) {
                this.loadedScripts.put(_versionedKey, _rawscript);
                this.loadedScriptsInfo.put(_versionedKey, _info);
                _checkDuplicates = false;
            }
        }
        if (_checkDuplicates && this.loadedScripts.containsKey(_versionedKey)) {
            LoadedScriptInfo _existing = (LoadedScriptInfo) loadedScriptsInfo.get(_versionedKey);
            if (false == _digest.equals(_existing.getDigest())) {
                throw new ObolException(__me + ".loadScript(): **ERROR** " + "script named \"" + _name + "\" and version \"" + _version + "\" already exists with  digest=" + _existing.getDigest() + ", while loaded script has DIFFERENT digest=" + _digest);
            }
        }
        return _info;
    }

    public LoadedScriptInfo[] loadScripts(InputStream in) throws ObolException, IOException {
        ArrayList _list = new ArrayList();
        try {
            ScriptInputStream _scriptIn = new ScriptInputStream(in);
            LoadedScriptInfo _info = this.loadScript(null, null, _scriptIn);
            while (_info != null) {
                _list.add(_info);
                _info = this.loadScript(null, null, _scriptIn);
            }
        } catch (EOFException e) {
            log.warn(".loadScripts(): caught exception ", e);
        }
        if (_list.isEmpty()) {
            return null;
        }
        return (LoadedScriptInfo[]) _list.toArray(new LoadedScriptInfo[0]);
    }

    public LoadedScriptInfo[] loadScripts(File file) throws ObolException, IOException {
        return this.loadScripts(new FileInputStream(file));
    }

    public LoadedScriptInfo[] loadScripts(String filename) throws ObolException, IOException {
        return this.loadScripts(new FileInputStream(filename));
    }

    public LoadedScriptInfo[] loadScripts(URL url) throws ObolException, IOException {
        return this.loadScripts(url.openStream());
    }

    public LoadedScriptInfo[] loadScripts(URI uri) throws ObolException, IOException {
        return this.loadScripts(uri.toURL());
    }

    /** get a script instance of the first loaded version of the named
     * script.  */
    public ScriptHandle getScriptInstance(String scriptname) throws ObolException {
        return this.getScriptInstance(scriptname, null);
    }

    /** get a specific version of a script instance. */
    public synchronized ScriptHandle getScriptInstance(String scriptname, String version) throws ObolException {
        String _key = this.makeVersionedKey(scriptname, version);
        if (false == this.loadedScripts.containsKey(_key)) {
            throw new ObolException(__me + ".getScriptInstance() Script named \"" + scriptname + "\" version " + version + " not loaded!");
        }
        ByteArrayInputStream _bin = new ByteArrayInputStream((byte[]) this.loadedScripts.get(_key));
        LoadedScriptInfo _info = (LoadedScriptInfo) this.loadedScriptsInfo.get(_key);
        return new ScriptHandleImpl(_info, _bin, this.currentRuntime);
    }

    /** get a specific version of a script instance. */
    public ScriptHandle getScriptInstance(LoadedScriptInfo scriptinfo) throws ObolException {
        return this.getScriptInstance(scriptinfo.getName(), scriptinfo.getVersion());
    }

    /** Return the information about a particular version of a loaded
     * script.
     * The information returned is static information about a loaded script,
     * not a script instance.
     * @param scriptname name of loaded script to return info about.
     * @param version version string of loaded script to return info about,
     * or <tt>null</tt> to indicate the default version (i.e. the one first
     * encountered during script loading).
     * @return LoadedScriptInfo instance describing the loaded script, or
     * <tt>null<tt> if no such script were found.
     */
    public LoadedScriptInfo getInfo(String scriptname, String version) {
        LoadedScriptInfo _info = null;
        if (null != scriptname) {
            String _key = this.makeVersionedKey(scriptname, version);
            if (this.loadedScriptsInfo.containsKey(_key)) {
                _info = (LoadedScriptInfo) this.loadedScriptsInfo.get(_key);
            }
        }
        return _info;
    }

    /** Return the information about a particular loaded script.
     * The information returned is static information about a loaded script,
     * not a script instance.
     * The version of the named script is the default/canonical version,
     * i.e. the first loaded script of that name.
     * @param scriptname name of loaded script to return info about.
     * @return LoadedScriptInfo instance describing the loaded script, or
     * <tt>null<tt> if no such script were found.
     */
    public LoadedScriptInfo getInfo(String scriptname) {
        return this.getInfo(scriptname, null);
    }

    /** Return an iteration containing LoadedScriptInfo instanced for all
     * loaded scripts.
     */
    public Iterator getInfoIterator() {
        return this.getInfoIterator(null, null);
    }

    /** Return an iteration containing LoadedScriptInfo instanced for all
     * scripts that exactly matches the name and version criteria.
     * @param scriptname name of scripts to include, or <tt>null</tt> for all
     * scripts.
     * @param version version string of scripts to include, or <tt>null</tt>
     * for all versions of that script.
     * @return Iterator that gives the scripts that matched the above
     * criteria.
     */
    public Iterator getInfoIterator(String scriptname, String version) {
        return this.getInfoIterator(scriptname, version, false, false);
    }

    /** Return an iteration containing LoadedScriptInfo instanced for all
     * scripts that, ignoring case, matches the name and version criteria.
     * @param scriptname name of scripts to include, or <tt>null</tt> for all
     * scripts.
     * @param version version string of scripts to include, or <tt>null</tt>
     * for all versions of that script.
     * @return Iterator that gives the scripts that case-insensitively
     * matched the above criteria.
     */
    public Iterator getInfoIteratorIgnoreCase(String scriptname, String version) {
        return this.getInfoIterator(scriptname, version, true, false);
    }

    /** Return an iteration containing LoadedScriptInfo instanced for all
     * scripts that matches the name and version regular expressions.
     * @param scriptname regular expression to match name of scripts to
     * include, or <tt>null</tt> for all scripts. 
     * @param version regular expression to match version string of scripts
     * to include, or <tt>null</tt> for all versions of that script.
     * @return Iterator that gives the scripts that matched the above
     * regular expressions.
     */
    public Iterator getMatchingInfoIterator(String scriptname, String version) {
        return this.getInfoIterator(scriptname, version, false, true);
    }

    /** Return an iteration containing LoadedScriptInfo instanced for all
     * scripts that case-insensitively matches the name and version regular
     * expressions.
     * @param scriptname regular expression to match name of scripts to
     * include, or <tt>null</tt> for all scripts. 
     * @param version regular expression to match version string of scripts
     * to include, or <tt>null</tt> for all versions of that script.
     * @return Iterator that gives the scripts that case-insensitively
     * matched the above regular expressions.
     */
    public Iterator getMatchingInfoIteratorIgnoreCase(String scriptname, String version) {
        return this.getInfoIterator(scriptname, version, true, true);
    }

    private Iterator getInfoIterator(String scriptname, String version, boolean ignoreCase, boolean regexp) {
        Iterator _master = this.loadedScriptsInfo.values().iterator();
        if (null == scriptname && null == version) {
            return _master;
        }
        int _flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
        Pattern _name = null;
        Pattern _ver = null;
        if (regexp) {
            if (null != scriptname) {
                _name = Pattern.compile(scriptname, _flags);
            }
            if (null != version) {
                _ver = Pattern.compile(version, _flags);
            }
        }
        ArrayList _list = new ArrayList();
        while (_master.hasNext()) {
            LoadedScriptInfo _info = (LoadedScriptInfo) _master.next();
            boolean _match = true;
            if (regexp) {
                if (null != _name) {
                    _match = _name.matcher(_info.getName()).matches();
                }
                if (null != _ver) {
                    _match = _ver.matcher(_info.getVersion()).matches();
                }
            } else {
                if (ignoreCase) {
                    if (null != scriptname) {
                        _match = scriptname.equalsIgnoreCase(_info.getName());
                    }
                    if (null != version) {
                        _match = version.equalsIgnoreCase(_info.getVersion());
                    }
                } else {
                    if (null != scriptname) {
                        _match = scriptname.equals(_info.getName());
                    }
                    if (null != version) {
                        _match = version.equals(_info.getVersion());
                    }
                }
            }
            if (_match) {
                _list.add(_info);
            }
        }
        return _list.iterator();
    }

    /****************************************************************************
 * Receive pool implementation
 ***************************************************************************/
    private static void checkRuntimeInitialization(String callerid) {
        if (null == currentRuntime) {
            throw new RuntimeException(__me + "." + callerid + ": runtime not initialized!");
        }
    }

    public SymbolPool getSymbolPoolInstance() {
        checkRuntimeInitialization("getSymbolPoolInstance");
        return currentSymbolPool;
    }

    public MessagePool getMessagePoolInstance() {
        checkRuntimeInitialization("getMessagePoolInstance");
        return currentMessagePool;
    }

    public Format getListenerFormat(String key) {
        Format _retval = null;
        synchronized (this.registeredListeners) {
            if (this.registeredListeners.containsKey(key)) {
                _retval = (Format) this.registeredListeners.get(key);
            }
        }
        return _retval;
    }

    public boolean addListener(int localport, String formatSpec, Map listenerProperties) throws IOException, ObolException {
        return this.addListener(new InetSocketAddress(localport), formatSpec, listenerProperties);
    }

    public boolean addListener(SocketAddress local, String formatSpec, Map listenerProperties) throws IOException, ObolException {
        return this.addListener(local.toString(), formatSpec, listenerProperties);
    }

    /** Add a listener thread.
     * @param local communication-channel to bind listener to
     * @param formatSpec the format to use
     * @param listenerType Either "message", "symbol" or <tt>null</tt> (i.e.
     * whatever's the default).  "message" is supported, and "symbol" is
     * experimental.
     * @return <tt>true</tt> if a new listener thread was created, or
     * <tt>false</tt> if there already was an existing one bound to the
     * given address.
     */
    public boolean addListener(String bindkey, String formatSpec, Map listenerProperties) throws IOException, ObolException {
        boolean _retval = false;
        synchronized (this.registeredListeners) {
            if (false == this.registeredListeners.containsKey(bindkey)) {
                FormatListener _worker = new FormatListener(bindkey, formatSpec, listenerProperties);
                _worker.start();
                Object[] _entry = new Object[2];
                _entry[0] = formatSpec;
                _entry[1] = _worker;
                this.registeredListeners.put(bindkey, _entry);
                _retval = true;
            }
        }
        return _retval;
    }

    class LoadedScriptInfoImpl implements LoadedScriptInfo {

        private String name, doc, digest, version;

        private int offset = 0;

        public LoadedScriptInfoImpl(String name, String version, String doc, String digest, int offset) {
            this.name = name;
            this.doc = doc;
            this.digest = digest;
            this.version = version;
            this.offset = offset;
        }

        public int getLineReportOffset() {
            return this.offset;
        }

        public String toString() {
            return this.getName();
        }

        public String getName() {
            return this.name;
        }

        public String getDocumentation() {
            return this.doc;
        }

        public String getVersion() {
            return this.version;
        }

        public String getDigest() {
            return this.digest;
        }

        public String dumpAll() {
            return ("\tScript name:    \"" + this.name + "\"" + "\n\tVersion:  " + (null != this.version ? this.version : "n/a") + "\n\tDigest: " + (this.digest) + "\n\tDocumentation:  " + (null != this.doc ? ("\"" + this.doc + "\"") : "n/a"));
        }
    }
}
