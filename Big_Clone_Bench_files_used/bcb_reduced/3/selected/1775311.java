package de.fhg.igd.semoa.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import codec.Base64;
import codec.CorruptedCodeException;
import de.fhg.igd.semoa.security.AgentStructure;
import de.fhg.igd.util.Attributes;
import de.fhg.igd.util.ClassFile;
import de.fhg.igd.util.Digester;
import de.fhg.igd.util.Manifest;
import de.fhg.igd.util.Resource;
import de.fhg.igd.util.WhatIs;

/**
 * This is the parent class loader of the SeMoA class loaders.
 * It implements a particular class loading policy for agents.
 * This policy is as follows:
 * <ol>
 * <li> Ask the parent class loader to load the class. If it
 *   finds one then return it.
 * <li> Check if the class is already loaded. If so, return
 *   the loaded class.
 * <li> Retrieve the set of trusted digest entries of the
 *   agent's Manifest section that corresponds to the given
 *   class name. If there is no such section or the section
 *   is not signed by the agent's owner or the number of
 *   trusted digests is not equal to or greater than a given
 *   threshold then throw a <code>ClassNotFoundException</code>.
 * <li> Check if there is already an interface class known
 *   such that a trusted digest of that interface matches the
 *   corresponding trusted digest of the given class file and
 *   the name of the interface equals the given name. If such
 *   an interface is found than that interface is returned,
 *   end of story.
 * <li> Look into the agent's <code>Resource</code> for the
 *   class file. If it's there then load it and skip the next
 *   item. If there is an exception thrown while loading then
 *   throw a <code>ClassNotFoundException</code>.
 * <li> Look into the agent's properties for keys of the form
 *   &quot;agent.codebase.<i>num</i>&quot;, start with <i>
 *   num</i>=1 and increase <i>num</i> until no more matching
 *   keys are found. For each key, get the property value and
 *   try to create a URL from it; try to load the class file
 *   from that URL. If something goes wrong, try the next key.
 *   Still no luck? Throw a <code>ClassNotFoundException</code>.
 * <li> At this point, we successfully loaded the byte code of
 *   some class. With the class name and that byte code, call
 *   <code>filterByteCode(..)</code>. This method throws a
 *   <code>SecurityException</code> if it decides that something
 *   is funny about the byte code, see below for details.
 * <li> Define the class, create the class object.
 * <li> If the class is an interface then register the class
 *   object using ids based on all known trusted digests for
 *   that class as the keys.
 * <li> Return the class object.
 * </ol>
 * 
 * Most of this policy is pretty straighforward, yet two things
 * are peculiar: this interface stuff, and what the hell happens
 * in method <code>filterByteCode(..)</code>?<p>
 *
 * Two types are identical to the VM if and only if their fully
 * qualified names are equal and they are loaded by the same
 * class loader. In SeMoA, we enforce separate class loaders
 * for agents. Hence, classes brought by one agent are in
 * principle distinct from all classes brought by other agents.
 * The only means for direct interactions between agent classes
 * is to cast classes to interface types that are loaded through
 * the system class loader. This limits the flexibility of the
 * agent platform, though. However, we cannot allow agents to
 * load classes into the name space of other agents, this opens
 * up a variety of Trojan Horse and Denial of Service attacks.<p>
 *
 * But there is a compromise. Interface types are implicitly
 * declared <i>abstract</i> - they can't define methods. Moreover,
 * they do not appear on the calling stack of threads when an
 * object's method is invoked through a given interface. As a
 * consequence, interfaces can be shared in principle. In practice,
 * a malicious agent can bring a common interface class that is
 * incompatible to an interface with the same name that is used
 * by other well-behaved agents (this can also happen without evil
 * intentions). For this reason, we make interfaces available to
 * all <code>AgentClassLoader</code> instances by means of unique
 * identifiers whose computation is based on trusted cryptographic
 * digests (computed on the byte code of each interface). We do
 * not have to worry about the <code>ProtectionDomain</code> of
 * an interface type because they do not appear on a threads
 * calling stack, and thus are not considered in calls to the
 * <code>AccessController</code> or <code>SecurityManager</code>.
 * <p>
 *
 * As a result, interfaces brought by different agents appear to
 * have the same type if they have the same name and at least
 * one trusted digest is equal for the byte code of each interface.
 * This result is not perfect; interfaces might be type compatible
 * even though their byte codes differ. Secondly, interfaces can have
 * static fields with references to untrusted classes. For the time
 * being we live with that. Anybody who wants to strengthen this
 * scheme can code a canonical interface signature, and run the
 * digest computation on these instead. Interface classes can also
 * be filtered for potentially &quot;dangerous&quot; fields. What
 * brings us to the second peculiarity.<p>
 *
 * Method <code>filterByteCode(..)</code> creates a <code>ClassFile
 * </code> instance and loads the byte code of the class candidate
 * into it. <code>ClassFile</code> scans through the byte code,
 * builds an index of the constant pool of that class file, and
 * gathers further information from the byte code. The <code>
 * AgentContext</code> of the client agent and the <code>ClassFile
 * </code> are piped through a list of <code>ByteCodeFilter</code>
 * implementations. Each filter can inspect, reject, or even modify
 * the byte code of the class. The list of filters consists of all
 * <code>ByteCodeFilter</code> implementations registered in the
 * global <code>Environment</code> with a name that has a particular
 * configurable prefix. This prefix must be accessible through the
 * <code>WhatIs</code> facility using &quot;BYTECODE_FILTER&quot;
 * as the key.<p>
 *
 * Filters are executed in the lexicographic order of their names.
 * Some possible filters are:
 * <ul>
 * <li> virus filters, which scan the byte code for patterns of
 *   known malicious classes.
 * <li> Filters that reject classes which implement or call
 *   &quot;dangerous&quot; methods such as <code>finalize()</code>.
 * <li> Filters that annotate byte code with additional code for
 *   resource consumption accounting.
 * </ul>
 * Get creative! ...and paranoid...
 *
 * @author Volker Roth
 * @version $Id: AgentClassLoader.java 1913 2007-08-08 02:41:53Z jpeters $
 */
public class AgentClassLoader extends java.security.SecureClassLoader {

    /**
     * The size of the buffer to use for reading
     * external class files.
     */
    public static final int BUFFER_SIZE = 1024;

    /**
     * The <code>HashMap</code> in which interface classes are
     * kept, indexed by their trusted ids which are computed
     * based on digest algorithms.
     */
    private static Map interfaces_ = new HashMap();

    /**
     * The trusted digest algorithm names.
     */
    private static Set TRUSTED_;

    static {
        String[] algs;
        Set set;
        int n;
        algs = Digester.parseAlgorithms(Manifest.DEFAULT_TRUSTED);
        set = new HashSet();
        for (n = 0; n < algs.length; n++) {
            set.add(algs[n].toUpperCase());
        }
        TRUSTED_ = set;
    }

    /**
     * A private lock object to synchronize on.
     */
    private Object lock_ = new Object();

    /**
     * The last time the list of <code>ByteCodeFilter</code>
     * instances was fetched.
     */
    private long filterUpdate_ = -1;

    /**
     * The <code>SortedMap</code> of <code>ByteCodeFilter</code>
     * instances registered in the <code>Environment</code>.
     */
    private SortedMap filters_;

    /**
     * The context of the agent that is served by this class
     * loader.
     */
    private AgentContext context_;

    /**
     * The properties of the agent served by this class loader.
     * These properties contain any codesource URLs set for
     * that agent.
     */
    private Properties props_;

    /**
     * The {@link Resource Resource} that stores the agent's
     * data.
     */
    private Resource struct_;

    /**
     * Stores the <code>MANIFEST</code> entries that are assumed to
     * be in the static part of the agent. Any classes defined
     * in this list are given the privileges granted to the
     * agent's owner.
     */
    private Map static_;

    /**
     * The <code>Environment</code> of this class loader.
     */
    private Environment env_;

    /**
     * The protection domain for the classes of the agent that
     * are authorised by means of the agent owner's signature.
     */
    private ProtectionDomain auth_;

    /**
     * Creates an instance with the given parent ClassLoader. This
     * class loader will load classes as determined by the given
     * <code>AgentContext</code>.<p>
     *
     * @param parent The ClassLoader to be set as parent. If
     *   parent is null then the ClassLoader returned by
     *   ClassLoader.getBaseClassLoader() is set as the parent.
     * @param ctx The <code>AgentContext</code> of the agent
     *   on whose behalf classes are loaded. Classes are loaded
     *   either from the agent's <code>Resource</code> or from
     *   a list of URLs taken from the agent's properties.
     * @exception NullPointerException if <code>ctx</code> is
     *   <code>null</code>.
     */
    protected AgentClassLoader(ClassLoader parent, AgentContext ctx) {
        super(parent);
        init(ctx);
    }

    /**
     * Creates an instance that loads classes on behalf of the
     * agent represented by the given <code>AgentContext</code>.
     *
     * @param ctx The {@link AgentContext AgentContext} of the agent
     *   this class loader belongs to.
     * @exception NullPointerException if <code>ctx</code> is
     *   <code>null</code>.
     */
    protected AgentClassLoader(AgentContext ctx) {
        super();
        init(ctx);
    }

    /**
     * This method loads a class of the agent.
     *
     * @param cn The name of the class to be loaded.
     */
    protected Class findClass(String cn) throws ClassNotFoundException {
        InputStream in;
        ClassFile file;
        String[] id;
        byte[] buf;
        String url;
        String fn;
        Class co;
        int i;
        fn = cn.replace('.', '/') + ".class";
        id = trustedIDs(fn);
        buf = null;
        if (id == null || id.length < Manifest.DEFAULT_THRESHOLD) {
            throw new ClassNotFoundException("Class " + cn + " is not a trusted class!");
        }
        co = findInterface(id, cn);
        if (co != null) {
            return co;
        }
        if (struct_.exists(fn)) {
            try {
                in = struct_.getInputStream(fn);
                buf = loadByteCode(in);
            } catch (Exception e) {
                throw new ClassNotFoundException(e.getMessage());
            }
            try {
                in.close();
            } catch (Exception e) {
                throw new ClassNotFoundException(e.getMessage());
            }
        } else {
            i = 1;
            while (true) {
                url = props_.getProperty(AgentStructure.PROP_CODESOURCE + i);
                if (url == null) {
                    throw new ClassNotFoundException("Cannot locate class: " + cn);
                }
                if (url.endsWith("/")) {
                    url = url + fn;
                } else {
                    url = url + "/" + fn;
                }
                try {
                    buf = loadByteCode(url);
                    checkByteCode(fn, buf);
                    break;
                } catch (Exception e) {
                    i++;
                    continue;
                }
            }
        }
        buf = filterByteCode(cn, buf);
        co = defineClass(cn, buf, 0, buf.length, auth_);
        registerInterface(id, co);
        return co;
    }

    /**
     * Registers the given interface (if that class is an
     * interface) in the global <code>Map</code> of known
     * interfaces. All interfaces are registered by means
     * of their digests. The interface is registered only
     * if there are trusted digests for that class in the
     * Manifest.
     *
     * @param id The array of trusted digest ids as computed
     *   by <code>trustedIDs(..)</code>.
     * @param co The interface class to register.
     */
    protected void registerInterface(String[] id, Class co) {
        WeakReference ref;
        int n;
        if (!co.isInterface()) {
            return;
        }
        synchronized (interfaces_) {
            for (n = 0; n < id.length; n++) {
                ref = (WeakReference) interfaces_.get(id[n]);
                if (ref != null && ref.get() != null) {
                    continue;
                }
                interfaces_.put(id[n], new WeakReference(co));
            }
        }
    }

    /**
     * Computes the set of IDs used for registering interfaces
     * based on the trusted digests in the Manifest section of
     * the given class file. A result array of zero length
     * means that there are no trusted digest entries or no
     * Manifest section at all.
     *
     * @param name The name of the class file with slashes as
     *   separators and the &quot;.class&quot; extension.
     * @return The array of digest IDs that should be used to
     *   register the class with the given file name (if it
     *   is an interface).
     */
    protected String[] trustedIDs(String name) {
        Attributes attr;
        String[] algs;
        String alg;
        String key;
        List res;
        int n;
        int k;
        attr = (Attributes) static_.get(name);
        if (attr == null) {
            return new String[0];
        }
        algs = Digester.parseAlgorithms(attr.getDigestAlgorithms());
        res = new ArrayList(algs.length);
        for (k = 0, n = 0; n < algs.length; n++) {
            alg = algs[n].toUpperCase();
            if (!TRUSTED_.contains(alg)) {
                continue;
            }
            key = attr.get(algs[n] + "-Digest");
            if (key != null && key.length() > 0) {
                key = alg + "#" + key;
                res.add(key);
                k++;
            }
        }
        return (String[]) res.toArray(new String[k]);
    }

    /**
     * Checks if there is already an <i>interface</i> that
     * matches the given name and the digests of the class
     * with that name as defined in the agent's Manifest.
     * If so, that interface's class object is returned,
     * and <code>null</code> otherwise. If no digest info
     * is available then <code>null</code> is returned as
     * well.<p>
     *
     * This procedure allows agents to share types in a
     * reasonably safe manner. With classes that's too
     * risky, but interfaces are implicitly <i>abstract
     * </i> and may not declare method implementations.
     *
     * @param name The name of the class.
     * @return The class object or <code>null</code> if
     *   the class was not found.
     */
    protected Class findInterface(String[] id, String name) {
        WeakReference ref;
        Class co;
        int n;
        synchronized (interfaces_) {
            for (n = 0; n < id.length; n++) {
                ref = (WeakReference) interfaces_.get(id[n]);
                if (ref == null) {
                    continue;
                }
                co = (Class) ref.get();
                if (co == null) {
                    interfaces_.remove(id[n]);
                    continue;
                }
                if (co.getName().equals(name)) {
                    return co;
                }
            }
        }
        return null;
    }

    /**
     * Pipes the given byte code through a filter pipeline.
     * Each filter can reject the class.
     *
     * @param name The name of the class.
     * @param code The byte code of a class that shall be
     *   loaded.
     * @return The filtered byte code. Byte code filters
     *   may also transform the byte code, e.g. by adding
     *   additional code to perform resource control in a
     *   way comparable to JRes.
     */
    protected byte[] filterByteCode(String name, byte[] code) throws IllegalByteCodeException {
        ByteCodeFilter filter;
        Map.Entry entry;
        ClassFile cf;
        Iterator i;
        Map filters;
        try {
            filters = getByteCodeFilters();
            cf = new ClassFile();
            cf.load(code);
            for (i = filters.entrySet().iterator(); i.hasNext(); ) {
                entry = (Map.Entry) i.next();
                filter = (ByteCodeFilter) entry.getValue();
                cf = filter.filterByteCode(context_, cf);
                if (cf == null || cf.isCleared()) {
                    throw new IllegalByteCodeException("Class " + name + " rejected by filter \"" + entry.getKey() + "\"");
                }
            }
        } catch (RuntimeException e) {
            throw new IllegalByteCodeException(e.getMessage());
        }
        return cf.clear();
    }

    /**
     * Checks the given byte code against the hash values
     * in entry <code>name</code> of the static part.
     *
     * @param name The name of the <code>MANIFEST</code>
     *   entry to check against.
     * @param buf The byte code to check.
     * @exception GeneralSecurityException if the byte code
     *   does not match some hash value, or no entry with
     *   the given name is in the static part.
     */
    protected void checkByteCode(String name, byte[] buf) throws GeneralSecurityException {
        Attributes attr;
        Digester dig;
        String[] algs;
        String s;
        Map md;
        int n;
        algs = Digester.parseAlgorithms(Manifest.DEFAULT_TRUSTED);
        dig = new Digester(algs, Manifest.DEFAULT_THRESHOLD);
        attr = (Attributes) static_.get(name);
        if (attr == null) {
            throw new DigestException("No digests available for \"" + name + "\"");
        }
        algs = Digester.parseAlgorithms(attr.getDigestAlgorithms());
        md = new HashMap();
        try {
            dig.digest(algs, md, buf);
        } catch (IOException e) {
            throw new DigestException("Unexpected IOException!");
        }
        for (n = algs.length - 1; n >= 0; n--) {
            if (!md.containsKey(algs[n])) {
                continue;
            }
            s = attr.get(algs[n] + "-Digest");
            if (s == null) {
                throw new DigestException("Missing " + algs[n] + " digest in entry of class \"" + name + "\"");
            }
            buf = (byte[]) md.get(algs[n]);
            try {
                if (!Arrays.equals(buf, Base64.decode(s))) {
                    throw new DigestException(algs[n] + " digest mismatch of class \"" + name + "\"");
                }
            } catch (CorruptedCodeException e) {
                throw new DigestException("Bad Base64 encoding of " + algs[n] + " digest in entry of class \"" + name + "\"");
            }
        }
    }

    /**
     * Loads byte code from the given URL.
     *
     * @param u The URL string that points to the remote
     *   class that shall be loaded.
     * @return The class bytes.
     * @exception IOException if an error occurs while loading
     *   the byte code.
     */
    protected byte[] loadByteCode(String u) throws IOException {
        InputStream in;
        URL url;
        in = null;
        try {
            url = new URL(u);
            in = url.openStream();
            return loadByteCode(in);
        } catch (MalformedURLException e) {
            throw new IOException("Bad URL syntax: \"" + u + "\"");
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Loads byte code from the given input stream. The stream
     * is not closed after use.
     *
     * @param in The <code>InputStream</code> from which the
     *   class file is loaded.
     * @return The byte code.
     * @exception IOException if an error occurs while reading.
     */
    protected byte[] loadByteCode(InputStream in) throws IOException {
        ByteArrayOutputStream bos;
        byte[] buf;
        int n;
        if (in == null) {
            throw new NullPointerException("Need an input stream!");
        }
        buf = new byte[BUFFER_SIZE];
        bos = new ByteArrayOutputStream();
        try {
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            bos.close();
        }
    }

    /**
     * @return The <code>Map</code> of byte code filters.
     */
    protected SortedMap getByteCodeFilters() {
        Environment env;
        Iterator i;
        long millis;
        final String key = WhatIs.stringValue(ByteCodeFilter.WHATIS);
        synchronized (lock_) {
            millis = env_.lastChange(key);
            if (millis != filterUpdate_) {
                filterUpdate_ = millis;
                filters_ = (SortedMap) AccessController.doPrivileged(new PrivilegedAction() {

                    public Object run() {
                        return env_.lookupAll(key + "/-");
                    }
                });
                for (i = filters_.values().iterator(); i.hasNext(); ) {
                    if (!(i.next() instanceof ByteCodeFilter)) {
                        i.remove();
                    }
                }
            }
            return filters_;
        }
    }

    /**
     * This method implements the bare bones policy for granting
     * permissions to agent classes. It creates a protection
     * domain {@link #auth_ auth_} that holds the permissions for
     * classes authorised by the agent's owner (which are in the
     * agent's static part).<p>
     *
     * @exception NullPointerException if the given name
     *   is <code>null</code>.
     */
    private void init(AgentContext context) {
        AgentPermissions perms;
        CodeSource cs;
        if (context == null) {
            throw new NullPointerException("Need an agent context!");
        }
        context_ = context;
        static_ = (Map) context.get(FieldType.STATIC_PART);
        struct_ = (Resource) context.get(FieldType.RESOURCE);
        props_ = (Properties) context.get(FieldType.PROPERTIES);
        perms = (AgentPermissions) context.get(FieldType.PERMISSIONS);
        env_ = Environment.getEnvironment();
        if (static_ == null || struct_ == null) {
            throw new NullPointerException("static part or resource");
        }
        if (props_ == null || perms == null) {
            throw new NullPointerException("properties or permissions");
        }
        env_.addWatch(WhatIs.stringValue(ByteCodeFilter.WHATIS));
        cs = new CodeSource((URL) null, (Certificate[]) null);
        auth_ = new ProtectionDomain(cs, perms);
    }
}
