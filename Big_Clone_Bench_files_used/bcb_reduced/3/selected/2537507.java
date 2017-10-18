package DE.FhG.IGD.util;

import java.io.*;
import java.util.*;
import java.security.NoSuchAlgorithmException;

/**
 * Parses .class files and extracts the transitive closure
 * of the classes referenced by some given initial classes.
 * Initial classes may be specified either by name or by
 * providing <code>ClassFile</code> with the Java class.
 * <p>
 *
 * Optionally, this class can export the class files in
 * the closure to a given <code>Resource</code> such as
 * a directory or a ZIP file.<p>
 *
 * Classes whose names start with a number of user-provided
 * prefixes can be excluded from computing the closure.
 * For historical reasons, the prefixes as well as class
 * names passed to instances of this class must use a slash
 * instead of a period as a separator of the components of
 * fully qualified class names.<p>
 *
 * This class can generate <code>Manifest</code> entries
 * for classes in the closure automatically. This is done
 * by setting an empty <code>Manifest</code> before running
 * the closure computation. The preferred order of method
 * calls is:
 * <pre><code>
 *   Manifest mf;
 *
 *   closure.setManifest(new Manifest());
 *   closure.push(SomeClass.getName().replace('.','/'))
 *   closure.run();
 *   mf = closure.getManifest();
 * </code></pre>
 *
 * @author Volker Roth
 * @version "$Id: ClassClosure.java 350 2001-05-17 08:32:31Z vroth $"
 * @see Resource
 */
public class ClassClosure extends Object {

    /**
     * The size of the buffer to use for reading and
     * writing on exporting class files.
     */
    public static final int BUFFER_SIZE = 1024;

    /**
     * The Resource to which classes in the closure are
     * exported.
     */
    protected Resource export_ = null;

    /**
     * The stack used for breadth first search.
     */
    protected Stack stack_ = new Stack();

    /**
     * The default exclusion list. Package and class names
     * must be provided with slashes as separators instead
     * of dots.
     */
    protected static String[] exclude_ = { "java/", "javax/", "sun/" };

    /**
     * The class path.
     */
    protected Resource[] classpath_;

    /**
     * The actual closure being computed.
     */
    protected Set closure_ = new HashSet();

    /**
     * A flag indicating whether <code>.java</code> files
     * should be included when exporting classes.
     */
    protected boolean java_ = false;

    /**
     * The <code>ClassFile</code> used to pre-process class
     * files.
     */
    protected ClassFile cf_ = new ClassFile();

    /**
     * The <code>Manifest</code> with the digests of the
     * classes in the closure.
     */
    protected Manifest mf_;

    /**
     * The <code>DigestContext</code> of the <code>Manifest
     * </code>. Used to add entries.
     */
    protected ManifestDigester mfd_;

    /**
     * Creates an instance that uses the default exclusion
     * list.
     *
     * @exception IOException if some classpath {@link
     *   Resource Resource} cannot be opened for some
     *   reason.
     */
    public ClassClosure() throws IOException {
        this(null);
    }

    /**
     * Creates an instance that excludes the classes and
     * packages with names starting with one of the given
     * names. Names are separated by colon or semicolon.
     * If <code>exclude</code> is <code>null</code> then
     * the default exclusion list is taken, which comprises
     * the classes contained in the java/, javax/ and
     * sun/ packages. Please note that class and package
     * names must be specified with slashes as separators
     * instead of dots.<p>
     *
     * The classpath from which byte code is loaded is
     * initialized from the <code>java.class.path</code>
     * system property.
     *
     * @param exclude The colon or semicolon separated
     *   list of prefixes of packages whose classes are
     *   neither parsed nor included in the closure.
     *   Please note that a slash is used as the path
     *   separator rather than a period.
     * @exception IOException if some classpath {@link
     *   Resource Resource} cannot be opened for some
     *   reason.
     */
    public ClassClosure(String exclude) throws IOException {
        if (exclude != null) {
            exclude_ = parse(exclude);
        }
        classpath_ = getClasspath(System.getProperty("java.class.path"));
    }

    /**
     * Creates an instance that excludes the classes and
     * packages with names starting with one of the given
     * names. Names are separated by colon or semicolon.
     * If <code>exclude</code> is <code>null</code> then
     * the default exclusion list is taken, which comprises
     * the classes contained in the java/, javax/ and
     * sun/ packages. Please note that class and package
     * names must be specified with slashes as separators
     * instead of dots.<p>
     *
     * @param exclude The colon or semicolon separated
     *   list of prefixes of packages whose classes are
     *   neither parsed nor included in the closure.
     *   Please note that a slash is used as the path
     *   separator rather than a period.
     * @param path The colon or semicolon separated list
     *   of resources from which byte code is loaded.
     *   Both directories and ZIP archives are supported.
     * @exception IOException if some classpath {@link
     *   Resource Resource} cannot be opened for some
     *   reason.
     */
    public ClassClosure(String exclude, String path) throws IOException {
        if (exclude != null) {
            exclude_ = parse(exclude);
        }
        classpath_ = getClasspath(path);
    }

    /**
     * Sets the {@link Resource Resource} to which the
     * classes are exported that are in the closure.
     * Setting the export Resource also clears the closure
     * such that no classes are ommitted accidently because
     * classes are only exported when they are added to the
     * closure. If the given Resource is <code>null</code>
     * then the closure is not cleared and no classes are
     * exported any more until this method is called with
     * a valid Resource. Initially, the export Resource
     * is <code>null</code>. The processing stack remains
     * untouched, though.
     *
     * @param resource The Resource to which class files
     *   should be exported.
     */
    public void setExportResource(Resource resource) {
        export_ = resource;
        if (resource != null) {
            closure_.clear();
        }
    }

    /**
     * Sets the <code>Manifest</code> file that is used to
     * collect digest information on the classes in the
     * closure. The list of digest algorithms being used
     * is the list of default algorithms as defined in
     * <code>Manifest</code>.
     *
     * @param mf The <code>Manifest</code> to which <code>
     *   Attributes</code> entries are added for each class
     *   included in the closure.
     */
    public void setManifest(Manifest mf) throws NoSuchAlgorithmException {
        mf_ = mf;
        mfd_ = new ManifestDigester(mf);
    }

    /**
     * Returns the <code>Manifest</code> with entries for all
     * classes in the closure if a <code>Manifest</code> was
     * set before computing the closure.
     *
     * @return The <code>Manifest</code> with the digest
     *   entries of the classes in the closure, or <code>
     *   null</code> if no <code>Manifest</code> was set
     *   before computing the closure.
     */
    public Manifest getManifest() {
        return mf_;
    }

    /**
     * The flag set by this method indicates whether Java
     * source code is exported with the class files or not.
     * The default is <code>false</code>. This class expects
     * the <code>.class</code> files and corresponding <code>
     * .java</code> files are in the same folder and have
     * identical basenames.
     */
    public void setExportJava(boolean t) {
        java_ = t;
    }

    /**
     * Returns the set of names of the classes in the closure
     * computed so far. You can always push new classes and
     * class names onto the stack and run the computation
     * again. Only new classes are added to the closure and
     * only new classes are processed consecutively. Please
     * note that class names use slashes as separators and
     * not dots.
     *
     * @return The set of names of the classes in the closure.
     */
    public Set getClosure() {
        return closure_;
    }

    /**
     * Runs the closure computation. The class files are
     * loaded from the classpath. Classes that are not
     * found are ignored.
     */
    public void run() throws IOException {
        InputStream in;
        String cl;
        String fn;
        int n;
        while (!stack_.empty()) {
            cl = (String) stack_.pop();
            if (!closure_.contains(cl)) {
                closure_.add(cl);
                fn = cl + ".class";
                for (n = 0; n < classpath_.length; n++) {
                    in = classpath_[n].getInputStream(fn);
                    if (in == null) {
                        continue;
                    }
                    try {
                        cf_.clear();
                        cf_.load(in);
                        push(cf_);
                    } finally {
                        in.close();
                    }
                    if (mfd_ != null) {
                        in = cf_.toInputStream();
                        mfd_.digest(fn, in);
                        in.close();
                    }
                    if (export_ == null) {
                        break;
                    }
                    in = cf_.toInputStream();
                    try {
                        export(in, fn);
                    } finally {
                        in.close();
                    }
                    if (java_) {
                        fn = cl + ".java";
                        in = classpath_[n].getInputStream(fn);
                        if (in != null) {
                            try {
                                export(in, fn);
                            } finally {
                                in.close();
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Pushes the given class name onto the processing
     * stack. This method might be called multiple times.
     * Names pushed onto the stack using this method are
     * not subject to exclusion.
     *
     * @param name The name of the class whose closure
     *   should be included.
     */
    public void push(String name) {
        if (name != null) {
            stack_.push(name);
        }
    }

    /**
     * Pushes a class including all classes referenced by
     * it that are not excluded on the processing stack.
     *
     * @param cf The <code>ClassFile</code> with the class
     *   that shall be parsed and whose closure shall be
     *   included.
     */
    public void push(ClassFile cf) {
        Iterator i;
        String name;
        Set refs;
        refs = cf.classRefs();
        for (i = refs.iterator(); i.hasNext(); ) {
            name = (String) i.next();
            if (!isExcluded(name)) {
                stack_.push(name);
            }
        }
    }

    private int u2(int h, int l) {
        return ((h & 0xff) << 8) | (l & 0xff);
    }

    /**
     * This method checks whether the class with the given
     * name is excluded from the closure. Classes in the
     * java and javax package, array and primitive types
     * are always excluded.
     */
    public boolean isExcluded(String cl) {
        if (cl.startsWith("java/") || cl.startsWith("javax/")) {
            return true;
        }
        int i;
        for (i = 0; i < exclude_.length; i++) {
            if (cl.startsWith(exclude_[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method dumps the closure to the stdout.
     */
    public void dump() {
        Iterator i;
        for (i = closure_.iterator(); i.hasNext(); ) {
            System.out.println((String) i.next());
        }
    }

    /**
     * This method takes a classpath and decomposes it into
     * an array of <code>Resource</code> instances.  It does
     * so in a system-independent way.
     *
     * @param path The colon or semicolon separated path
     *   from which classes are loaded.
     * @return The array of {@link Resource Resource} instances.
     */
    public Resource[] getClasspath(String path) throws IOException {
        Resource[] res;
        String p[];
        String s;
        String q;
        List l;
        File f;
        int i;
        if (path == null) {
            return new Resource[0];
        }
        p = parse(path);
        l = new ArrayList(p.length);
        for (i = 0; i < p.length; i++) {
            f = new File(p[i]);
            if (!f.exists()) {
                continue;
            }
            if (p[i].endsWith(".zip") || p[i].endsWith(".jar") || p[i].endsWith(".ZIP") || p[i].endsWith(".JAR")) {
                l.add(new ZipResource(p[i]));
            } else {
                l.add(new FileResource(p[i]));
            }
        }
        return (Resource[]) l.toArray(new Resource[l.size()]);
    }

    /**
     * Exports the given class file using the given name to
     * the {@link #export_ export Resource} if it is set.
     * The input stream is closed after all its contents are
     * have been read.
     *
     * @param in The input stream to read the class file
     *   from.
     * @param name The name to usefor storing the class.
     */
    public void export(InputStream in, String name) throws IOException {
        if (export_ == null) {
            return;
        }
        int n;
        byte[] buffer;
        OutputStream out;
        buffer = new byte[BUFFER_SIZE];
        out = export_.getOutputStream(name);
        if (out == null) {
            in.close();
            throw new IOException("Can't get output stream for " + name);
        }
        try {
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
        } finally {
            out.close();
            in.close();
        }
    }

    /**
     * This method decomposes a string consisting of colon
     * or semicolon-separated paths into a list of strings.
     *
     * @param paths The string to decompose.
     * @return The list of strings.
     */
    public String[] parse(String paths) {
        StringTokenizer tok;
        ArrayList list;
        String s;
        list = new ArrayList(16);
        tok = new StringTokenizer(paths, ":;");
        while (tok.hasMoreTokens()) {
            s = tok.nextToken();
            if (s.length() > 0) {
                list.add(s);
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }
}
