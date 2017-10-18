package sun.tools.jar;

import java.io.*;
import java.util.*;
import java.security.*;
import sun.net.www.MessageHeader;
import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;

/**
 * This is OBSOLETE. DO NOT USE THIS. Use java.util.jar.Manifest
 * instead. It has to stay here because some apps (namely HJ and HJV)
 * call directly into it.
 *
 * @author David Brown
 * @author Benjamin Renaud
 */
public class Manifest {

    private Vector entries = new Vector();

    private byte[] tmpbuf = new byte[512];

    private Hashtable tableEntries = new Hashtable();

    static final String[] hashes = { "SHA" };

    static final byte[] EOL = { (byte) '\r', (byte) '\n' };

    static final boolean debug = false;

    static final String VERSION = "1.0";

    static final void debug(String s) {
        if (debug) System.out.println("man> " + s);
    }

    public Manifest() {
    }

    public Manifest(byte[] bytes) throws IOException {
        this(new ByteArrayInputStream(bytes), false);
    }

    public Manifest(InputStream is) throws IOException {
        this(is, true);
    }

    /**
     * Parse a manifest from a stream, optionally computing hashes
     * for the files.
     */
    public Manifest(InputStream is, boolean compute) throws IOException {
        if (!is.markSupported()) {
            is = new BufferedInputStream(is);
        }
        while (true) {
            is.mark(1);
            if (is.read() == -1) {
                break;
            }
            is.reset();
            MessageHeader m = new MessageHeader(is);
            if (compute) {
                doHashes(m);
            }
            addEntry(m);
        }
    }

    public Manifest(String[] files) throws IOException {
        MessageHeader globals = new MessageHeader();
        globals.add("Manifest-Version", VERSION);
        String jdkVersion = System.getProperty("java.version");
        globals.add("Created-By", "Manifest JDK " + jdkVersion);
        addEntry(globals);
        addFiles(null, files);
    }

    public void addEntry(MessageHeader entry) {
        entries.addElement(entry);
        String name = entry.findValue("Name");
        debug("addEntry for name: " + name);
        if (name != null) {
            tableEntries.put(name, entry);
        }
    }

    public MessageHeader getEntry(String name) {
        return (MessageHeader) tableEntries.get(name);
    }

    public MessageHeader entryAt(int i) {
        return (MessageHeader) entries.elementAt(i);
    }

    public Enumeration entries() {
        return entries.elements();
    }

    public void addFiles(File dir, String[] files) throws IOException {
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            File file;
            if (dir == null) {
                file = new File(files[i]);
            } else {
                file = new File(dir, files[i]);
            }
            if (file.isDirectory()) {
                addFiles(file, file.list());
            } else {
                addFile(file);
            }
        }
    }

    /**
     * File names are represented internally using "/";
     * they are converted to the local format for anything else
     */
    private final String stdToLocal(String name) {
        return name.replace('/', java.io.File.separatorChar);
    }

    private final String localToStd(String name) {
        name = name.replace(java.io.File.separatorChar, '/');
        if (name.startsWith("./")) name = name.substring(2); else if (name.startsWith("/")) name = name.substring(1);
        return name;
    }

    public void addFile(File f) throws IOException {
        String stdName = localToStd(f.getPath());
        if (tableEntries.get(stdName) == null) {
            MessageHeader mh = new MessageHeader();
            mh.add("Name", stdName);
            addEntry(mh);
        }
    }

    public void doHashes(MessageHeader mh) throws IOException {
        String name = mh.findValue("Name");
        if (name == null || name.endsWith("/")) {
            return;
        }
        BASE64Encoder enc = new BASE64Encoder();
        for (int j = 0; j < hashes.length; ++j) {
            InputStream is = new FileInputStream(stdToLocal(name));
            try {
                MessageDigest dig = MessageDigest.getInstance(hashes[j]);
                int len;
                while ((len = is.read(tmpbuf, 0, tmpbuf.length)) != -1) {
                    dig.update(tmpbuf, 0, len);
                }
                mh.set(hashes[j] + "-Digest", enc.encode(dig.digest()));
            } catch (NoSuchAlgorithmException e) {
                throw new JarException("Digest algorithm " + hashes[j] + " not available.");
            } finally {
                is.close();
            }
        }
    }

    public void stream(OutputStream os) throws IOException {
        PrintStream ps;
        if (os instanceof PrintStream) {
            ps = (PrintStream) os;
        } else {
            ps = new PrintStream(os);
        }
        MessageHeader globals = (MessageHeader) entries.elementAt(0);
        if (globals.findValue("Manifest-Version") == null) {
            String jdkVersion = System.getProperty("java.version");
            if (globals.findValue("Name") == null) {
                globals.prepend("Manifest-Version", VERSION);
                globals.add("Created-By", "Manifest JDK " + jdkVersion);
            } else {
                ps.print("Manifest-Version: " + VERSION + "\r\n" + "Created-By: " + jdkVersion + "\r\n\r\n");
            }
            ps.flush();
        }
        globals.print(ps);
        for (int i = 1; i < entries.size(); ++i) {
            MessageHeader mh = (MessageHeader) entries.elementAt(i);
            mh.print(ps);
        }
    }

    public static boolean isManifestName(String name) {
        if (name.charAt(0) == '/') {
            name = name.substring(1, name.length());
        }
        name = name.toUpperCase();
        if (name.equals("META-INF/MANIFEST.MF")) {
            return true;
        }
        return false;
    }
}
