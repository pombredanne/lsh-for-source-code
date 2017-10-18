package org.nkumar.immortal.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public final class ImmortalAntTask extends Task {

    private static final String IMMORTAL_MAPPINGS_XML = "immortal_mappings.xml";

    private static final String IMMORTAL_FOLDER_NAME = "i_m_m_o_r_t_a_l";

    private File genAppFolder;

    private final Set<String> gzipSet = new HashSet<String>();

    private final List<FileSet> fileSets = new ArrayList<FileSet>();

    private String extResourceBase;

    private static final String EXT_RESOURCE_BASE_KEY = "extResourceBase";

    public ImmortalAntTask() {
    }

    /**
     * Set the name of the folder where the generated webapp root is located.
     * @param genAppFolder
     */
    public void setGenAppFolder(final File genAppFolder) {
        this.genAppFolder = getCanonicalFile(genAppFolder);
    }

    /**
     * set the comma separated list of extensions for which the content should be gzipped.
     * for example you can set the same to "css,js,txt".
     * do not pass in extensions that are already compressed like jpg or gif etc.
     * @param gzipExts
     */
    public void setGzipExts(final String gzipExts) {
        if (gzipExts != null) {
            final String[] exts = gzipExts.split("[,]");
            for (final String ext : exts) {
                final String ext1 = ext.trim().toLowerCase();
                if (ext1.length() > 0) {
                    gzipSet.add(ext1);
                }
            }
        }
    }

    public void setExtResourceBase(final String extResourceBase) {
        this.extResourceBase = extResourceBase;
    }

    public void addFileset(final FileSet fileSet) {
        fileSets.add(fileSet);
    }

    @Override
    public void execute() throws BuildException {
        if (genAppFolder == null) {
            throw new BuildException("genAppFolder was not set");
        }
        if (fileSets.isEmpty()) {
            throw new BuildException("no resource filesets were added");
        }
        if (gzipSet.isEmpty()) {
            log("gzip is disabled", Project.MSG_VERBOSE);
        } else {
            log("gzip enabled for the extensions : " + gzipSet, Project.MSG_VERBOSE);
        }
        final File immortalDir = new File(genAppFolder, IMMORTAL_FOLDER_NAME);
        if (!immortalDir.exists()) {
            delete(immortalDir);
        }
        immortalDir.mkdirs();
        final Map<String, Object> mappingConfig = new HashMap<String, Object>();
        if (extResourceBase != null) {
            mappingConfig.put(EXT_RESOURCE_BASE_KEY, extResourceBase);
        }
        final Map<String, String> mappings = new HashMap<String, String>();
        mappingConfig.put("mappings", mappings);
        try {
            for (final FileSet fileSet : fileSets) {
                final File srcRoot = fileSet.getDir().getCanonicalFile();
                final Iterator itr = fileSet.iterator();
                while (itr.hasNext()) {
                    final FileResource fr = (FileResource) itr.next();
                    final File srcFile = fr.getFile().getCanonicalFile();
                    if (srcFile.isFile() && srcFile.canRead()) {
                        final String fileName = srcFile.getName();
                        final int extIndex = fileName.lastIndexOf('.');
                        if (extIndex == -1) {
                            log(srcFile.getAbsolutePath() + " does not have an extension, hence ignoring", Project.MSG_VERBOSE);
                            continue;
                        }
                        final String extn = fileName.substring(extIndex + 1).toLowerCase();
                        createDestinationFile(srcRoot, srcFile, immortalDir, gzipSet.contains(extn), mappings, getProject());
                    }
                }
            }
            log("file mappings has " + mappings.size() + " entries", Project.MSG_VERBOSE);
            serializeMap(mappingConfig, genAppFolder);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private static void serializeMap(final Map<String, Object> map, final File webappRoot) throws FileNotFoundException {
        final File mappingFileFolder = new File(webappRoot, "WEB-INF/classes");
        mappingFileFolder.mkdirs();
        final File mappingFile = new File(mappingFileFolder, IMMORTAL_MAPPINGS_XML);
        final XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(mappingFile)));
        e.writeObject(map);
        e.close();
    }

    /**
     * Say the following params were passed. srcRoot - "/.../resources".
     * srcFile - "/.../resources/css/main.css".
     * dstRoot - "/.../i_m_m_o_r_t_a_l".
     * gzipContent - true.
     *
     * In this case MD5 checksum of the main.css file will be calculated.
     * say it is XYZ. A file with the name "/.../i_m_m_o_r_t_a_l/css/main_XYZ.css" is created.
     * Content of "/.../resources/css/main.css" is copied to
     * "/.../i_m_m_o_r_t_a_l/css/main_XYZ.css", gzipped in the process if gzipContent is true.
     * Finally a file mapping is added to the fileMappings map.
     * the entry would be "/css/main.css" -> "/i_m_m_o_r_t_a_l/css/main_XYZ.css".
     */
    private static void createDestinationFile(final File srcRoot, final File srcFile, final File dstRoot, final boolean gzipContent, final Map<String, String> fileMappings, final Project project) throws IOException {
        final String srcRootPath = srcRoot.getAbsolutePath();
        final String srcFilePath = srcFile.getAbsolutePath();
        if (!srcFilePath.startsWith(srcRootPath)) {
            throw new IllegalArgumentException(srcFilePath + " is not within " + srcRootPath);
        }
        if (!srcFile.exists() || !srcFile.canRead()) {
            throw new IllegalArgumentException(srcFilePath + " does not exist or is not readable");
        }
        final String fileMD5 = calculateMD5ChecksumOfFile(srcFile);
        final String srcRelativePath = srcFilePath.substring(srcRootPath.length());
        final int extensionIndex = srcRelativePath.lastIndexOf('.');
        if (extensionIndex == -1) {
            throw new IllegalArgumentException("no extension found in " + srcRelativePath);
        }
        final String dstRelativePath = srcRelativePath.substring(0, extensionIndex) + "_" + fileMD5 + srcRelativePath.substring(extensionIndex);
        final String dstRootPath = dstRoot.getAbsolutePath();
        final String dstFilePath = dstRootPath + dstRelativePath;
        copyContent(srcFile, new File(dstFilePath).getCanonicalFile(), gzipContent);
        final String srcRelativePath1 = canonicalizeRelativePath(srcRelativePath);
        final String dstRelativePath1 = "/" + dstRoot.getName() + canonicalizeRelativePath(dstRelativePath);
        project.log("'" + srcRelativePath1 + "' -> '" + dstRelativePath1 + "'", Project.MSG_VERBOSE);
        fileMappings.put(srcRelativePath1, dstRelativePath1);
    }

    private static String canonicalizeRelativePath(final String path) {
        return path.replace('\\', '/');
    }

    /**
     * Copies the content of the srcFile to dstFile, gzipping the content if necessary.
     * The parent folders of the dstFile is created if they do not exist.
     * This method assumes that all the passed files are canonicalized.
     */
    private static void copyContent(final File srcFile, final File dstFile, final boolean gzipContent) throws IOException {
        final File dstFolder = dstFile.getParentFile();
        dstFolder.mkdirs();
        if (!dstFolder.exists()) {
            throw new RuntimeException("Unable to create the folder " + dstFolder.getAbsolutePath());
        }
        final InputStream in = new FileInputStream(srcFile);
        OutputStream out = new FileOutputStream(dstFile);
        if (gzipContent) {
            out = new GZIPOutputStream(out);
        }
        try {
            final byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    private static void delete(final File file) {
        final File[] files = file.listFiles();
        if (files != null) {
            for (final File child : files) {
                if (child.isDirectory()) {
                    delete(child);
                } else {
                    child.delete();
                }
            }
        }
        file.delete();
    }

    private static File getCanonicalFile(final File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final char[] HEXADECIMAL = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private static String encodeToHex(final byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        final char[] hexChars = new char[data.length * 2];
        for (int i = 0, j = 0; i < data.length; i++) {
            hexChars[j++] = HEXADECIMAL[(data[i] & 0xf0) >> 4];
            hexChars[j++] = HEXADECIMAL[data[i] & 0x0f];
        }
        return new String(hexChars);
    }

    private static byte[] calculateMD5Checksum(final InputStream in) throws IOException {
        final MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 checksum not available", e);
        }
        final InputStream cin = new DigestInputStream(in, md5);
        final byte[] buffer = new byte[512];
        try {
            while (cin.read(buffer) != -1) {
            }
        } finally {
            cin.close();
        }
        return md5.digest();
    }

    private static String calculateMD5ChecksumOfFile(final File file) throws IOException {
        return encodeToHex(calculateMD5Checksum(new FileInputStream(file)));
    }
}
