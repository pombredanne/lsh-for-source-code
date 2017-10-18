package net.sourceforge.buildprocess.autodeploy;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * FileManipulation use VFS to do various actions on files
 * 
 * @author <a href="mailto:jb@nanthrax.net">Jean-Baptiste Onofr�</a>
 */
public class FileManipulator {

    private static final Log log = LogFactory.getLog(FileManipulator.class);

    private static final String BASE_DIR = SystemUtils.USER_DIR;

    private static final String WORKING_DIRECTORY = "work";

    private static final String PROTOCOL_REGEXP = "(.+)://(.+)";

    private static final String VFS_JAR_SUFFIX = ".jar";

    private static final String VFS_ZIP_SUFFIX = ".zip";

    private static final String VFS_TGZ_SUFFIX = ".tgz";

    private static final String VFS_TARGZ_SUFFIX = ".tar.gz";

    private static final String VFS_TBZ2_SUFFIX = ".tbz2";

    private static final String VFS_JAR_PREFIX = "jar:";

    private static final String VFS_ZIP_PREFIX = "zip:";

    private static final String VFS_TGZ_PREFIX = "tgz:";

    private static final String VFS_TARGZ_PREFIX = "tgz:";

    private static final String VFS_TBZ2_PREFIX = "tbz2:";

    private static FileManipulator _singleton = null;

    private FileSystemManager fileSystemManager;

    /**
    * Private constructor used for the singleton initialization
    */
    private FileManipulator() throws FileManipulatorException {
        try {
            this.fileSystemManager = VFS.getManager();
            log.debug("Creating a VFS FileSystemManager : " + this.fileSystemManager);
            ((DefaultFileSystemManager) this.fileSystemManager).setReplicator(new AutoDeployFileReplicator());
            log.debug("Load the AutoDeploy file replicator");
        } catch (Exception e) {
            log.error("Error during the FileManipulator creation : " + e.getMessage());
            throw new FileManipulatorException(e.getMessage());
        }
    }

    /**
    * Return a single instance of FileManipulator
    * 
    * @return the uniq FileManipulator instance
    */
    public static FileManipulator getInstance() throws FileManipulatorException {
        try {
            if (_singleton == null) {
                _singleton = new FileManipulator();
                log.debug("FileManipulator Initialised : " + _singleton);
            }
            return _singleton;
        } catch (Exception e) {
            log.error("FileManipulator initialisation error : " + e.getMessage());
            throw new FileManipulatorException(e.getMessage());
        }
    }

    /**
    * Return the current basedir full path
    * 
    * @return the full path of the local basedir directory
    */
    public static String getBasedir() throws FileManipulatorException {
        try {
            log.debug("Create the file to map the local basedir");
            File baseDir = new File(FileManipulator.BASE_DIR);
            log.debug("Return the path");
            return baseDir.getPath();
        } catch (Exception e) {
            log.error("Basedir getter error : " + e.getMessage());
            throw new FileManipulatorException(e.getMessage());
        }
    }

    /**
    * Override the resolveFile of VFS to add filename regexp selector
    * 
    * @param the file to resolve (can looks like /tmp/folder/file*)
    * @return the corresponding file object
    */
    public FileObject resolveFile(String vfsPath) throws FileSystemException, FileManipulatorException {
        log.debug("Resolve VFS Path : " + vfsPath);
        log.debug("Check if the file name regexp selector is required");
        if ((vfsPath.indexOf("/") == -1) || (vfsPath.indexOf("*") == -1)) {
            log.debug("The file name regexp selector is not required, no / or * in the file name");
            return fileSystemManager.resolveFile(vfsPath);
        }
        log.debug("Isolate the path end");
        log.debug("Find the last index of the separator /");
        int separatorIndex = vfsPath.lastIndexOf("/");
        int tokenIndex = vfsPath.lastIndexOf("*");
        if (tokenIndex < separatorIndex) {
            log.error("Wildcard is only supported on the filename, not directories");
            throw new FileManipulatorException("Wildcard is only supported on the filename, not directories");
        }
        log.debug("Get the substring at index " + separatorIndex);
        String pattern = vfsPath.substring(separatorIndex);
        log.debug(pattern + " pattern found");
        String baseName = vfsPath.substring(0, separatorIndex);
        log.debug("Get the base name " + baseName);
        log.debug("Looking for the file (first found is returned)");
        FileObject baseUrl = fileSystemManager.resolveFile(baseName);
        FileObject[] fileObjects = baseUrl.findFiles(new FileNameRegexpSelector(pattern));
        if (fileObjects.length < 1) {
            log.debug("No child file found, resolve using the file system manager");
            return fileSystemManager.resolveFile(vfsPath);
        }
        log.debug("Return the first child");
        return fileObjects[0];
    }

    /**
    * Compare the content of two files
    * 
    * @return true if the two files have exactly the same contents, false else
    * @param from
    *           the first file to compare with
    * @param to
    *           the second file to compare with
    */
    public boolean contentCompare(String from, String to) throws FileManipulatorException {
        try {
            log.debug("Compare the content of " + from + " and " + to);
            FileObject fromFile = this.resolveFile(from);
            FileObject toFile = this.resolveFile(to);
            if (!fromFile.exists() || !toFile.exists()) {
                log.debug(to + " not found : return false");
                return false;
            }
            return IOUtils.contentEquals(fromFile.getContent().getInputStream(), toFile.getContent().getInputStream());
        } catch (Exception e) {
            log.error("Content compare error : " + e.getMessage());
            throw new FileManipulatorException(e.getMessage());
        }
    }

    /**
    * Compare two files based on the MD5SUM
    * 
    * @return true if the two files are different, false else
    * @param from
    *           the first file
    * @param to
    *           the second file
    */
    public boolean fileCompare(String from, String to) throws FileManipulatorException {
        try {
            log.debug("Generate md5sum for " + from + " and " + to + " and compare it");
            FileObject fromFile = this.resolveFile(from);
            FileObject toFile = this.resolveFile(to);
            if (!fromFile.exists() || !toFile.exists()) {
                return true;
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            log.debug("MD5 message digest created");
            DigestInputStream fromStream = new DigestInputStream(fromFile.getContent().getInputStream(), md);
            byte[] fromBuffer = new byte[8192];
            while (fromStream.read(fromBuffer) != -1) ;
            byte[] fromMD5 = md.digest();
            md.reset();
            DigestInputStream toStream = new DigestInputStream(toFile.getContent().getInputStream(), md);
            byte[] toBuffer = new byte[8192];
            while (toStream.read(toBuffer) != -1) ;
            byte[] toMD5 = md.digest();
            return !MessageDigest.isEqual(fromMD5, toMD5);
        } catch (Exception e) {
            log.error("Files compare using MD5SUM error : " + e.getMessage());
            throw new FileManipulatorException(e.getMessage());
        }
    }

    /**
    * Copy a file to another
    * 
    * @param from
    *           the origin file
    * @param to
    *           the destination file
    * @param control
    *           if true, overwrite the destination file only if the destination
    *           doen't exist or if it older than the origin file or if file size
    *           are different
    * @return true if the copy is effective, false else
    */
    public boolean copy(String from, String to, boolean control) throws FileManipulatorException {
        try {
            if (control && fileCompare(from, to)) {
                log.debug("File control activated and from/to files different");
                copy(from, to);
                return true;
            } else if (!control) {
                log.debug("File control desactivated");
                copy(from, to);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Copy error : " + e.getMessage());
            throw new FileManipulatorException(e.getMessage());
        }
    }

    /**
    * Copy a file to another
    * 
    * @param from
    *           the origin file
    * @param to
    *           the destination file
    */
    public void copy(String from, String to) throws FileManipulatorException {
        try {
            FileObject toFile = this.resolveFile(to);
            log.debug("Create the destination file object : " + to);
            FileObject fromFile = this.resolveFile(from);
            log.debug("Create the origin file object : " + from);
            toFile.copyFrom(fromFile, Selectors.SELECT_ALL);
        } catch (Exception e) {
            log.error("Copy error : " + e.getMessage());
            throw new FileManipulatorException(e.getMessage());
        }
    }

    /**
    * Read a file and return the input stream
    * 
    * @param file
    *           the file VFS URI
    * @return the input stream on the file
    */
    public InputStream readFile(String file) throws FileManipulatorException {
        try {
            FileObject fileObject = this.resolveFile(file);
            log.debug("Create the file object : " + fileObject);
            return fileObject.getContent().getInputStream();
        } catch (Exception e) {
            log.error("File read error : " + e.getMessage());
            throw new FileManipulatorException("File read error : " + e.getMessage());
        }
    }

    /**
    * Return the output stream on a file
    * 
    * @param file
    *           the file VFS URI
    * @return the output stream on the file
    */
    public OutputStream writeFile(String file) throws FileManipulatorException {
        try {
            FileObject fileObject = this.resolveFile(file);
            log.debug("Create the file object : " + fileObject);
            return fileObject.getContent().getOutputStream();
        } catch (Exception e) {
            log.error("File write error : " + e.getMessage());
            throw new FileManipulatorException("File write error : " + e.getMessage());
        }
    }

    /**
    * Init (create) the working directory of autodeploy
    * 
    * @param projectDirectory
    *           the project directory name
    */
    public void initWorkingDirectory(String projectDirectory) throws FileManipulatorException {
        this.createDirectory(FileManipulator.getBasedir() + "/" + FileManipulator.WORKING_DIRECTORY + "/" + projectDirectory);
    }

    /**
    * Create directory
    * 
    * @param directory
    *           the directory to create
    */
    public void createDirectory(String directory) throws FileManipulatorException {
        try {
            FileObject folder = this.resolveFile(directory);
            log.debug("Get the folder file object");
            folder.createFolder();
            log.debug("Folder created");
        } catch (Exception e) {
            log.error("Relative directory creation error : " + e.getMessage());
            throw new FileManipulatorException(e.getMessage());
        }
    }

    /**
    * Test if a file or folder exist
    * 
    * @param file
    *           the file/folder to test
    */
    public boolean exists(String file) throws FileManipulatorException {
        try {
            FileObject fileObject = this.resolveFile(file);
            return fileObject.exists();
        } catch (Exception e) {
            log.error("Test file exist error : " + e.getMessage());
            throw new FileManipulatorException(e.getMessage());
        }
    }

    /**
    * Return the working directory of a environment
    * 
    * @return the working directory
    */
    public static String getWorkingDirectory(String environment) throws FileManipulatorException {
        return FileManipulator.getBasedir() + "/" + FileManipulator.WORKING_DIRECTORY + "/" + environment;
    }

    /**
    * Check if a given path begins with a protocol definition (file:, http:,
    * ...)
    * 
    * @param path
    *           the give path to check
    * @return true if the path begins with a protocol definition, false else
    */
    public static boolean checkPathProtocolExists(String path) {
        log.debug("Initialise ORO regexp objects to check the path protocol");
        PatternMatcher matcher = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();
        Pattern pattern = null;
        try {
            pattern = compiler.compile(FileManipulator.PROTOCOL_REGEXP);
            log.debug("The ORO regexp pattern is " + PROTOCOL_REGEXP);
        } catch (MalformedPatternException patternException) {
            log.error("Path protocol check failed due to a malformed regexp pattern : " + patternException.getMessage());
            patternException.printStackTrace();
        }
        PatternMatcherInput input = new PatternMatcherInput(path);
        log.debug("Initialise the ORO regexp input matcher with the path : " + path);
        if (matcher.contains(input, pattern)) {
            log.debug("The path match a protocol regexp");
            return true;
        }
        return false;
    }

    /**
    * Format a URI to the VFS format depending of the suffix
    * 
    * @param uri
    *           the input uri
    * @return the VFS formatted uri
    */
    public static String formatVFSUri(String uri) {
        String formattedString = uri.trim();
        if (formattedString.endsWith(VFS_JAR_SUFFIX) && !formattedString.startsWith(VFS_JAR_PREFIX)) return VFS_JAR_PREFIX + formattedString;
        if (formattedString.endsWith(VFS_ZIP_SUFFIX) && !formattedString.startsWith(VFS_ZIP_PREFIX)) return VFS_ZIP_PREFIX + formattedString;
        if (formattedString.endsWith(VFS_TGZ_SUFFIX) && !formattedString.startsWith(VFS_TGZ_PREFIX)) return VFS_TGZ_PREFIX + formattedString;
        if (formattedString.endsWith(VFS_TARGZ_SUFFIX) && !formattedString.startsWith(VFS_TARGZ_PREFIX)) return VFS_TARGZ_PREFIX + formattedString;
        if (formattedString.endsWith(VFS_TBZ2_SUFFIX) && !formattedString.startsWith(VFS_TBZ2_PREFIX)) return VFS_TBZ2_PREFIX + formattedString;
        return formattedString;
    }

    /**
    * Delete a file or directory
    * 
    * @param fileOrDirectory
    *           the file or directory
    */
    public void delete(String fileOrDirectory) throws FileManipulatorException {
        try {
            FileObject fileObject = this.resolveFile(fileOrDirectory);
            fileObject.delete(Selectors.SELECT_ALL);
        } catch (FileSystemException fileSystemException) {
            log.error("FileSystemException : " + fileSystemException.getMessage());
            throw new FileManipulatorException(fileSystemException.getMessage());
        }
    }

    /**
    * Move a file to another
    * 
    * @param from
    *           the original file
    * @param to
    *           the new filename file
    */
    public void move(String from, String to) throws FileManipulatorException {
        try {
            FileObject fromObject = this.resolveFile(from);
            FileObject toObject = this.resolveFile(to);
            fromObject.moveTo(toObject);
        } catch (FileSystemException fileSystemException) {
            log.error("Can move the file " + from + " to " + to + " due to a filesystem error : " + fileSystemException.getMessage());
            throw new FileManipulatorException("Can move the file " + from + " to " + to + " due to a filesystem error : " + fileSystemException.getMessage());
        }
    }
}
