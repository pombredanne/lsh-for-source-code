package org.xmlibrary;

import org.xmlibrary.util.LongArrayOutputStream;
import javolution.xml.*;
import javolution.xml.stream.*;
import org.apache.tools.tar.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * A customizable utility class for reading and writing objects backed up to encrypted archives
 * 
 * @author a0u
 * @version 1.0 11-12-2008
 */
public class XMLibrarian {

    /** Property key for the digest algorithm */
    public static final String PROP_DIGEST = "prop.digest";

    /** Property key for the encoding */
    public static final String PROP_CHARSET = "prop.charset";

    /** Property key for the data extension */
    public static final String PROP_DATA_EXT = "prop.ext.data";

    /** Property key for the archive algorithm */
    public static final String PROP_ARC_EXT = "prop.ext.ext";

    /** Property key for the timestamp format */
    public static final String PROP_DATE_FORM = "prop.date";

    /** Property key for the temporary directory name */
    public static final String PROP_TMP_DIRNAME = "prop.dir.tmp";

    /** Property key for the maximum preservation level for history entries */
    public static final String PROP_MAX_HIST = "prop.hist.max";

    /** Default value for the digest algorithm */
    public static final String DEFAULT_DIGEST = "MD5";

    /** Default value for the encoding */
    public static final String DEFAULT_CHARSET = "UTF-8";

    /** Default value for the data extension */
    public static final String DEFAULT_DATA_EXT = ".xml";

    /** Default value for the archive extension */
    public static final String DEFAULT_ARC_EXT = ".atr";

    /** Default value for the timestamp format */
    public static final String DEFAULT_DATE_FORM = "yyyyMMddhhmmss";

    /** Default value for the temporary directory name */
    public static final String DEFAULT_TMP_DIRNAME = ".xmlarchiver";

    /** Default value for the maximum preservation level for history entries */
    public static final String DEFAULT_MAX_HIST = "-1";

    /** Default value for verbosity */
    public static final boolean DEFAULT_VERBOSITY = true;

    private final int BUFF_SIZE = 1024;

    private final byte[] BUFFER = new byte[BUFF_SIZE];

    private XMLObjectWriter xwriter;

    private XMLObjectReader xreader;

    private Properties prop;

    private HashMap<File, byte[]> commitments;

    private boolean printVerbose;

    private PrintStream stdout;

    /** The symmetric cryptographic hash algorithm to use */
    private final String CRYPTO_ALGORITHM = "AES";

    private final String COMMIT_ALGORITHM = "SHA-256";

    public XMLibrarian() {
        xwriter = new XMLObjectWriter();
        xreader = new XMLObjectReader();
        resetProperties();
        printVerbose = XMLibrarian.DEFAULT_VERBOSITY;
        stdout = System.out;
        commitments = new HashMap<File, byte[]>();
    }

    public XMLibrarian(XMLBinding binding) {
        xwriter = new XMLObjectWriter();
        xreader = new XMLObjectReader();
        setBinding(binding);
        resetProperties();
        printVerbose = XMLibrarian.DEFAULT_VERBOSITY;
        stdout = System.out;
        commitments = new HashMap<File, byte[]>();
    }

    public XMLibrarian(XMLBinding binding, Properties prop) {
        xwriter = new XMLObjectWriter();
        xreader = new XMLObjectReader();
        setBinding(binding);
        replaceProperties(prop);
        printVerbose = XMLibrarian.DEFAULT_VERBOSITY;
        stdout = System.out;
        commitments = new HashMap<File, byte[]>();
    }

    /**
     * Writes an XML representation of an object to an encrypted archive file and adds the archive to the commitments table<br/>
     * If the archive file already exists and is present in the commitments table,
     * an attempt is made to transfer historical data files to the new archive version<br/>
     * Otherwise, any previous files will be overwritten iff the operation is completely successful
     * 
     * @param   obj     the object to write
     * @param   arc     the archive file to which to write
     * @param   newpass the passphrase for the new version of the encrypted archive
     * @param   oldpass the previous passphrase needed to decrypt the existing version of the archive; does not matter if the archive file does not exist
     * @return  true iff the object is successfully archived; false otherwise
     */
    public boolean shelve(Object obj, File arc, String newpass, String oldpass) {
        File tmpFile = getTmpFile(arc);
        TarInputStream in = null;
        TarOutputStream out = null;
        try {
            String timestamp = generateTimestamp();
            out = new TarOutputStream(new CipherOutputStream(new FileOutputStream(tmpFile), generateCipher(newpass, Cipher.ENCRYPT_MODE)));
            MessageDigest md = MessageDigest.getInstance(getProperty(XMLibrarian.PROP_DIGEST));
            LongArrayOutputStream tmp = new LongArrayOutputStream();
            xwriter.setOutput(new DigestOutputStream(tmp, md), getProperty(XMLibrarian.PROP_CHARSET));
            Class cls = obj.getClass();
            xwriter.write(obj, cls.getName(), cls);
            xwriter.flush();
            TarEntry entry;
            entry = new TarEntry(getDataFile(timestamp));
            entry.setSize(tmp.size());
            out.putNextEntry(entry);
            tmp.writeTo(out);
            out.closeEntry();
            int max = getHistoryPreservationLevel();
            if (arc.exists() && max != 0 && commitments.containsKey(arc)) {
                if (!equalCommitHashes(arc, oldpass)) {
                    printMsg("Unlock passphrase incorrect");
                    return false;
                }
                int i = 0;
                in = new TarInputStream(new CipherInputStream(new FileInputStream(arc), generateCipher(oldpass, Cipher.DECRYPT_MODE)));
                while ((entry = in.getNextEntry()) != null && (max <= -1 || i < max)) {
                    out.putNextEntry(entry);
                    dumpStream(in, out, BUFFER);
                    out.closeEntry();
                    i++;
                }
                arc.delete();
            }
            tmpFile.renameTo(arc);
        } catch (XMLStreamException e) {
            return handleException(e, "XML conversion failed");
        } catch (IOException e) {
            return handleException(e, (new StringBuffer("Write operation failed: check ")).append(getFilePath(arc)).append(" for write permissions").toString());
        } catch (NoSuchAlgorithmException e) {
            return handleException(e, (new StringBuffer("Checksum algorithm ")).append(getProperty(XMLibrarian.PROP_DIGEST)).append(" not supported").toString());
        } catch (Exception e) {
            return handleException(e, null);
        } finally {
            close(xwriter);
            close(out);
        }
        commitFile(arc, newpass);
        return true;
    }

    /**
     * Retrieves a list of data files from an archive file<br/>
     * Note that, for security reasons, the archive must first be present in the commitments table before decryption is allowed
     * 
     * @param   arc     the archive file to scan
     * @param   pass    the passphrase for decrypting the archive
     * @return  an array of filenames iff successful; otherwise null
     */
    public String[] scan(File arc, String pass) {
        if (!meetsSecurityPrerequisites(arc, pass)) return null;
        TarInputStream in = null;
        ArrayList<String> list = new ArrayList<String>();
        try {
            in = new TarInputStream(new CipherInputStream(new FileInputStream(arc), generateCipher(pass, Cipher.DECRYPT_MODE)));
            TarEntry entry;
            while ((entry = in.getNextEntry()) != null) list.add(entry.getName());
        } catch (FileNotFoundException e) {
            handleException(e, (new StringBuffer("Expected file ")).append(getFilePath(arc)).append(" does not exist").toString());
            return null;
        } catch (IOException e) {
            handleException(e, (new StringBuffer("Read operation failed: check ")).append(getFilePath(arc)).append(" for read permissions").toString());
            return null;
        } catch (SecurityException e) {
            handleException(e, (new StringBuffer("A Java Security Manager is preventing access")).toString());
            return null;
        } catch (NoSuchAlgorithmException e) {
            handleException(e, (new StringBuffer("Decryption algorithm ")).append(CRYPTO_ALGORITHM).append(" not supported").toString());
            return null;
        } catch (Exception e) {
            handleException(e, null);
            return null;
        } finally {
            close(in);
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Retrieves an object contained in a data file stored in an archive<br/>
     * Note that, for security reasons, the archive must first be present in the commitments table before decryption is allowed
     * 
     * @param   arc     the archive file
     * @param   pass    the passphrase for decrypting the archive
     * @param   fname   the filename of the data file containing the target object
     * @return  the target object iff successful; null otherwise
     */
    public <T> T read(File arc, String pass, String fname) {
        if (!meetsSecurityPrerequisites(arc, pass)) return null;
        TarInputStream in = null;
        try {
            in = new TarInputStream(new CipherInputStream(new FileInputStream(arc), generateCipher(pass, Cipher.DECRYPT_MODE)));
            TarEntry entry;
            while ((entry = in.getNextEntry()) != null && !fname.equals(entry.getName())) ;
            if (entry == null) return null;
            xreader.setInput(in);
            return xreader.read();
        } catch (XMLStreamException e) {
            handleException(e, "XML parsing failed");
            return null;
        } catch (FileNotFoundException e) {
            handleException(e, (new StringBuffer("Expected file ")).append(getFilePath(arc)).append(" does not exist").toString());
            return null;
        } catch (IOException e) {
            handleException(e, (new StringBuffer("Read operation failed: check ")).append(getFilePath(arc)).append(" for read permissions").toString());
            return null;
        } catch (SecurityException e) {
            handleException(e, (new StringBuffer("A Java Security Manager is preventing access")).toString());
            return null;
        } catch (NoSuchAlgorithmException e) {
            handleException(e, (new StringBuffer("Decryption algorithm ")).append(CRYPTO_ALGORITHM).append(" not supported").toString());
            return null;
        } catch (Exception e) {
            handleException(e, null);
            return null;
        } finally {
            close(in);
        }
    }

    private String generateTimestamp() {
        return (new SimpleDateFormat(getProperty(XMLibrarian.PROP_DATE_FORM))).format(Calendar.getInstance().getTime());
    }

    private File getArchiveFile(File f) {
        return new File(f.getParentFile(), (new StringBuffer(f.getName())).append(getProperty(XMLibrarian.PROP_ARC_EXT)).toString());
    }

    private String getDataFile(String timestamp) {
        return (new StringBuffer(timestamp)).append(getProperty(XMLibrarian.PROP_DATA_EXT)).toString();
    }

    private File getTmpFile(File f) {
        return new File(getTmpDir(), f.getName());
    }

    private int getHistoryPreservationLevel() {
        int level = -1;
        try {
            level = Integer.parseInt(getProperty(XMLibrarian.PROP_MAX_HIST));
        } catch (NumberFormatException e) {
            handleException(e, "Invalid history preservation level value; defaulting to unlimited");
        } finally {
            return level;
        }
    }

    private String getFilePath(File f) {
        String filepath = "(n/a)";
        try {
            filepath = f.getCanonicalPath();
        } catch (IOException e) {
            handleException(e, null);
        } finally {
            return filepath;
        }
    }

    private File getTmpDir() {
        File tmp = new File(System.getProperty("java.io.tmpdir"), getProperty(XMLibrarian.PROP_TMP_DIRNAME));
        try {
            if (!tmp.exists()) tmp.mkdir();
        } catch (SecurityException e) {
            handleException(e, (new StringBuffer("Unable to create tmp directory in ")).append(getFilePath(tmp)).toString());
            return null;
        }
        return tmp;
    }

    /**
      * Loads a preexisting commitments table from an XML file<br/>
      * Replaces a previous value if the key is already present in the current table
      * 
      * @param  f   The XML file containing the commitments
      * @return true iff commitments are successfully loaded; false otherwise
      */
    public boolean importCommitments(File f) {
        try {
            if (!f.exists()) return false;
        } catch (SecurityException e) {
            return handleException(e, null);
        }
        FileInputStream fin = null;
        try {
            Properties table = new Properties();
            fin = new FileInputStream(f);
            table.loadFromXML(fin);
            Iterator<String> iter = table.stringPropertyNames().iterator();
            String fname, str;
            String[] tmpStr;
            byte[] tmpHash;
            final String regex = ", ";
            while (iter.hasNext()) {
                fname = iter.next();
                str = table.getProperty(fname);
                tmpStr = str.substring(1, str.length() - 1).split(regex);
                tmpHash = new byte[tmpStr.length];
                for (int i = 0; i < tmpStr.length; i++) tmpHash[i] = Byte.parseByte(tmpStr[i]);
                commitments.put(new File(fname), tmpHash);
            }
            filterCommitments();
        } catch (InvalidPropertiesFormatException e) {
            return handleException(e, "XML parsing failed");
        } catch (NumberFormatException e) {
            return handleException(e, "XML parsing failed: invalid String format");
        } catch (FileNotFoundException e) {
            return handleException(e, (new StringBuffer("Expected file ")).append(getFilePath(f)).append(" does not exist").toString());
        } catch (IOException e) {
            return handleException(e, (new StringBuffer("Read operation failed: check ")).append(getFilePath(f)).append(" for read permissions").toString());
        } catch (SecurityException e) {
            return handleException(e, (new StringBuffer("A Java SecurityManager is denying access to ")).append(getFilePath(f)).toString());
        } finally {
            close(fin);
        }
        return true;
    }

    private void filterCommitments() {
        Set<File> files = commitments.keySet();
        Iterator<File> iter = files.iterator();
        File f;
        while (iter.hasNext()) {
            f = iter.next();
            try {
                if (!f.exists()) commitments.remove(f);
            } catch (SecurityException e) {
                handleException(e, null);
            }
        }
    }

    /**
      * Exports the commitments table to an XML file
      * 
      * @param  f   the output file
      * @return true iff commitments are successfully written; false otherwise
      */
    public boolean exportCommitments(File f) {
        FileOutputStream fout = null;
        try {
            Properties table = new Properties();
            Iterator<File> iter = commitments.keySet().iterator();
            while (iter.hasNext()) {
                File tmp = iter.next();
                table.setProperty(tmp.toString(), Arrays.toString(commitments.get(tmp)));
            }
            fout = new FileOutputStream(f);
            table.storeToXML(fout, "commitments", getProperty(XMLibrarian.PROP_CHARSET));
        } catch (IOException e) {
            return handleException(e, (new StringBuffer("Write operation failed: check ")).append(getFilePath(f)).append(" for write permissions").toString());
        } finally {
            close(fout);
        }
        return true;
    }

    private void commitFile(File f, String pass) {
        byte[] hash = generateCommitHash(pass);
        if (hash != null) commitments.put(f, hash);
    }

    /**
     * Retrieves a sorted list of all currently committed files
     * @return  the file list
     */
    public File[] getCommittedFileList() {
        Set<File> files = commitments.keySet();
        File[] list = files.toArray(new File[files.size()]);
        Arrays.sort(list);
        return list;
    }

    private byte[] generateCommitHash(String pass) {
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance(COMMIT_ALGORITHM).digest(pass.getBytes());
        } catch (NoSuchAlgorithmException e) {
            handleException(e, null);
        } finally {
            return hash;
        }
    }

    private boolean equalCommitHashes(File f, String pass) {
        byte[] hash1 = commitments.get(f), hash2 = generateCommitHash(pass);
        return hash1 != null && hash2 != null && MessageDigest.isEqual(hash1, hash2);
    }

    private boolean meetsSecurityPrerequisites(File f, String pass) {
        if (!commitments.containsKey(f)) {
            printMsg((new StringBuffer(getFilePath(f))).append(" not tracked by commitments table; aborting read operation").toString());
            return false;
        } else if (!equalCommitHashes(f, pass)) {
            printMsg("Unlock passphrase incorrect");
            return false;
        } else return true;
    }

    private Cipher generateCipher(String pass, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        byte[] key = MessageDigest.getInstance("MD5").digest(pass.getBytes());
        SecretKeySpec secretkey = new SecretKeySpec(key, CRYPTO_ALGORITHM);
        Cipher ci = Cipher.getInstance(CRYPTO_ALGORITHM);
        ci.init(mode, secretkey);
        return ci;
    }

    /**
     * Resets properties to default values
     * @return  the previous Properties
     */
    public Properties resetProperties() {
        Properties old = prop;
        prop = new Properties();
        prop.setProperty(XMLibrarian.PROP_DIGEST, XMLibrarian.DEFAULT_DIGEST);
        prop.setProperty(XMLibrarian.PROP_CHARSET, XMLibrarian.DEFAULT_CHARSET);
        prop.setProperty(XMLibrarian.PROP_DATA_EXT, XMLibrarian.DEFAULT_DATA_EXT);
        prop.setProperty(XMLibrarian.PROP_ARC_EXT, XMLibrarian.DEFAULT_ARC_EXT);
        prop.setProperty(XMLibrarian.PROP_DATE_FORM, XMLibrarian.DEFAULT_DATE_FORM);
        prop.setProperty(XMLibrarian.PROP_TMP_DIRNAME, XMLibrarian.DEFAULT_TMP_DIRNAME);
        prop.setProperty(XMLibrarian.PROP_MAX_HIST, XMLibrarian.DEFAULT_MAX_HIST);
        return old;
    }

    /**
     * Completely replaces the current properties<br/>
     * If a necessary key is not present, its default value is used
     * 
     * @param   newProperties    the new Properties
     */
    public void replaceProperties(Properties newProperties) {
        prop = newProperties;
        if (!prop.containsKey(XMLibrarian.PROP_DIGEST)) prop.setProperty(XMLibrarian.PROP_DIGEST, XMLibrarian.DEFAULT_DIGEST);
        if (!prop.containsKey(XMLibrarian.PROP_CHARSET)) prop.setProperty(XMLibrarian.PROP_CHARSET, XMLibrarian.DEFAULT_CHARSET);
        if (!prop.containsKey(XMLibrarian.PROP_DATA_EXT)) prop.setProperty(XMLibrarian.PROP_DATA_EXT, XMLibrarian.DEFAULT_DATA_EXT);
        if (!prop.containsKey(XMLibrarian.PROP_ARC_EXT)) prop.setProperty(XMLibrarian.PROP_ARC_EXT, XMLibrarian.DEFAULT_ARC_EXT);
        if (!prop.containsKey(XMLibrarian.PROP_DATE_FORM)) prop.setProperty(XMLibrarian.PROP_DATE_FORM, XMLibrarian.DEFAULT_DATE_FORM);
        if (!prop.containsKey(XMLibrarian.PROP_TMP_DIRNAME)) prop.setProperty(XMLibrarian.PROP_TMP_DIRNAME, XMLibrarian.DEFAULT_TMP_DIRNAME);
        if (!prop.containsKey(XMLibrarian.PROP_MAX_HIST)) prop.setProperty(XMLibrarian.PROP_MAX_HIST, XMLibrarian.DEFAULT_MAX_HIST);
    }

    /**
     * Sets the value of a property to be associated with this XMLibrarian instance
     * @return  the previous value of the specified key, or null if the key does not exist
     */
    public Object setProperty(String key, String value) throws IllegalArgumentException {
        Object oldValue = prop.setProperty(key, value);
        return ((oldValue == null) ? null : (String) oldValue);
    }

    /**
     * Retrieves the setting of a property associated with this XMLibrarian instance
     * @return  the current value of the specified key, or null if the key does not exist
     */
    public String getProperty(String key) {
        Object value = prop.getProperty(key);
        return ((value == null) ? null : (String) value);
    }

    /**
     * Sets the XML binding for the underlying XML reader and writer, replacing the previous binding
     * @param   binding     the new XML binding
     */
    public void setBinding(XMLBinding binding) {
        xwriter.setBinding(binding);
        xreader.setBinding(binding);
    }

    /**
     * Sets the output stream for printing diagnostic messages<br/>
     * By default, stdout is System.out
     * 
     * @param   stream  the new output stream
     */
    public void setStdOut(PrintStream stream) {
        stdout = ((stream == null) ? stdout : stream);
    }

    /**
     * Sets the verbosity mode for diagnostic messages
     * @param   val     true to turn on; false to turn off
     */
    public void setVerboseOn(boolean val) {
        printVerbose = val;
    }

    private boolean handleException(Exception e, String msg) {
        printMsg(msg);
        e.printStackTrace();
        return false;
    }

    private void printMsg(String msg) {
        if (printVerbose && msg != null) stdout.println(msg);
    }

    private void dumpStream(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int numRead = 0;
        while ((numRead = in.read(buffer)) > 0) if (out != null) out.write(buffer, 0, numRead);
    }

    private void readStream(InputStream in, byte[] buffer) throws IOException {
        dumpStream(in, null, buffer);
    }

    private void close(XMLObjectWriter target) {
        if (target != null) {
            try {
                target.close();
            } catch (XMLStreamException e) {
                handleException(e, "Warning: XML writer failed to properly close");
            }
        }
    }

    private void close(XMLObjectReader target) {
        if (target != null) {
            try {
                target.close();
            } catch (XMLStreamException e) {
                handleException(e, "Warning: XML reader failed to properly close");
            }
        }
    }

    private void close(InputStream target) {
        if (target != null) {
            try {
                target.close();
            } catch (IOException e) {
                handleException(e, "Warning: input stream failed to properly close");
            }
        }
    }

    private void close(OutputStream target) {
        if (target != null) {
            try {
                target.close();
            } catch (IOException e) {
                handleException(e, "Warning: output stream failed to properly close");
            }
        }
    }

    private File getChecksumFile(File f) {
        String mdFileName = (new StringBuffer(f.getName())).append('.').append(getProperty(XMLibrarian.PROP_DIGEST)).toString();
        return new File(f.getParentFile(), mdFileName);
    }

    private byte[] calculateChecksum(InputStream input) {
        DigestInputStream in = null;
        byte[] digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance(getProperty(XMLibrarian.PROP_DIGEST));
            in = new DigestInputStream(input, md);
            readStream(in, BUFFER);
            digest = md.digest();
        } catch (Exception e) {
            handleException(e, null);
        } finally {
            close(in);
            return digest;
        }
    }

    private boolean isWritableFile(File f) throws SecurityException {
        return f != null && f.isFile() && f.canWrite();
    }

    private boolean isReadableFile(File f) throws SecurityException {
        return f != null && f.isFile() && f.canRead();
    }
}
