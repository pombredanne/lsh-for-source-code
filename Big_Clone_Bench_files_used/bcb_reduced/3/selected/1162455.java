package cu.ftpd.filesystem;

import cu.ftpd.filesystem.filters.ForbiddenFilesFilter;
import cu.ftpd.filesystem.metadata.Directory;
import cu.ftpd.filesystem.metadata.Metadata;
import cu.ftpd.filesystem.permissions.PermissionDeniedException;
import cu.ftpd.filesystem.permissions.ActionPermission;
import cu.ftpd.logging.Logging;
import cu.ftpd.user.User;
import cu.ftpd.user.groups.NoSuchGroupException;
import cu.ftpd.user.userbases.Hex;
import cu.ftpd.user.userbases.NoSuchUserException;
import cu.ftpd.FtpdSettings;
import cu.ftpd.Server;
import cu.ftpd.ServiceManager;
import cu.ftpd.events.Event;
import cu.settings.Settings;
import cu.settings.ConfigurationException;
import java.io.*;
import java.nio.channels.FileChannel;
import java.security.AccessControlException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.CRC32;

/**
 * @author Markus Jevring
 * @since 2007-05-07 - 21:38:57
 * @version $Id: FileSystem.java 313 2011-10-30 12:37:39Z jevring $
 */
public class FileSystem {

    public static File root;

    public static String rcp;

    public static Map<String, Section> sections;

    public static Section defaultSection;

    private File pwdFile;

    private String pwd = "/";

    protected long offset = 0;

    protected final User user;

    protected Section section;

    protected File renameSource;

    protected String type = "ASCII";

    protected String mode = null;

    public final DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);

    protected final DateFormat time = new SimpleDateFormat("MMM d HH:mm", Locale.ENGLISH);

    protected final DateFormat year = new SimpleDateFormat("MMM d yyyy", Locale.ENGLISH);

    protected final MessageFormat listline = new MessageFormat("{0}r{6}xr{6}xr{6}x    1 {1}     {2} {3} {4} {5}");

    public static final ForbiddenFilesFilter forbiddenFiles = new ForbiddenFilesFilter();

    public FileSystem(User user) {
        this.user = user;
        section = defaultSection;
        pwdFile = root;
    }

    public static void initialize(FtpdSettings settings) throws IOException, ConfigurationException {
        sections = new HashMap<String, Section>();
        loadSections(settings);
        root = new File(defaultSection.getPath());
        if (!root.exists() || !root.isDirectory()) {
            throw new FileNotFoundException("The root directory of the default section either does not exist or is not a directory.");
        } else {
            try {
                root.list();
            } catch (AccessControlException e) {
                throw new IOException("Permission to list root dir denied.", e);
            }
        }
        rcp = root.getCanonicalPath();
    }

    private static void loadSections(Settings settings) throws IOException, ConfigurationException {
        String name = "default";
        String path = settings.get("/filesystem/default/path");
        String owner = settings.get("/filesystem/default/owner");
        String group = settings.get("/filesystem/default/group");
        int ratio = settings.getInt("/filesystem/default/ratio");
        Section section = new Section(name, path, owner, group, ratio);
        defaultSection = section;
        File sectionDir;
        int i = 1;
        while (true) {
            name = settings.get("/filesystem/sections/section[" + i + "]/name");
            if (name == null || "".equals(name)) {
                break;
            }
            path = settings.get("/filesystem/sections/section[" + i + "]/path");
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException("Sections cannot have paths that do not start with '/'");
            }
            owner = settings.get("/filesystem/sections/section[" + i + "]/owner");
            if ("".equals(owner) || owner == null) {
                owner = defaultSection.getOwner();
            }
            group = settings.get("/filesystem/sections/section[" + i + "]/group");
            if ("".equals(group) || group == null) {
                group = defaultSection.getGroup();
            }
            String ratioString = settings.get("/filesystem/sections/section[" + i + "]/ratio");
            if ("".equals(ratioString) || ratioString == null) {
                ratio = defaultSection.getRatio();
            } else {
                ratio = Integer.parseInt(ratioString);
            }
            i++;
            try {
                section = new Section(name, path, owner, group, ratio);
                sections.put(path, section);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Error in section: " + name, e);
            }
            sectionDir = new File(defaultSection.getPath(), path);
            if (!sectionDir.exists()) {
                throw new FileNotFoundException("Section dir not found for section: " + name + ": " + sectionDir.getAbsolutePath());
            }
            ServiceManager.getServices().getMetadataHandler().setOwnership(sectionDir, owner, group);
        }
    }

    /**
     * Accepts a path in one of the following forms:
     * - /path/to/some/dir
     * - /path/to/some/dir/
     * - /path/to/some/file.txt
     *
     * and extracts the name, i.e. "dir" in the first two cases and "file.txt" in the last.
     *
     * @param path the path from which the name is extracted.
     * @return the name if the file or directory, the last token.
     */
    public static String getNameFromPath(String path) {
        String name = path;
        if ("/".equals(path)) {
            return path;
        } else {
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            name = name.substring(name.lastIndexOf("/") + 1);
            return name;
        }
    }

    /**
     * Accepts a path in one of the following forms:
     * - /path/to/some/dir
     * - /path/to/some/dir/
     * - /path/to/some/file.txt
     *
     * and extracts the parent directory, i.e. "/path/to/some" in all of the cases above
     *
     * @param path the path from which the parent directory is extracted.
     * @return the parent directory.
     */
    public static String getParentDirectoryFromPath(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        path = path.substring(0, path.lastIndexOf("/"));
        return path;
    }

    /**
     * Resolves a <code>File</code> object to a path usable in the ftp. Also checks the legality of the file.
     * If the file cannot be resolved for some reason, it will return null.
     *
     * @param file the file to be translated. Can be either directory or file.
     * @return A string representation of the path for the ftp or null of resolution fails or the file is illegal.
     */
    public static String resolvePath(File file) {
        if (isLegalPath(file)) {
            String s;
            try {
                s = file.getCanonicalPath();
                s = s.substring(rcp.length());
                String t;
                if (s.length() == 0) {
                    t = "/";
                } else {
                    t = s;
                }
                if (File.separatorChar != '/') {
                    t = t.replace(File.separatorChar, '/');
                }
                return t;
            } catch (IOException e) {
                Logging.getErrorLog().reportCritical("We were unable to get the canonical path for " + file.getAbsolutePath());
                e.printStackTrace();
                return null;
            }
        } else {
            return "/";
        }
    }

    /**
     * Takes a specified path and resolves it against the current directory. Any path beginning with '/'
     * will be resolved against the root.<br>
     * If the specified path resolved to be outside the ftp root, the ftp root itself is returned.<br>
     * NOTE: The path must be an ftp-path, and NOT a system path
     * 
     * @param path the FTP path to be resolved.
     * @return A file indicated by the path as specified above.
     */
    public File resolveFile(String path) {
        File file;
        if (path.startsWith("/")) {
            file = new File(root, path);
        } else {
            file = new File(pwdFile, path);
        }
        if (isLegalPath(file)) {
            return file;
        } else {
            return root;
        }
    }

    /**
     * Takes an ftpd path and returns the 'real' path of that path.
     * I.e. in the default case, passing "/tmp" to this function would return "/cuftpd/site/tmp".
     *
     * @param ftppath the ftpd path we want resolved
     * @return the "real" path that the provided ftpd path represents, resolved as decribed above.
     */
    public String resolveRealPath(String ftppath) {
        return resolveFile(ftppath).getAbsolutePath();
    }

    /**
     * Takes an ftp path, absolute or relative, and resolves it to the absolute ftp path.
     *
     * @param ftppath the path to be resolved.
     * @return the absolute ftp path of the path specified.
     */
    public String resolveFtpPath(String ftppath) {
        return resolvePath(resolveFile(ftppath));
    }

    /**
     * Checks whether a file is legal or not. A legal file is a file that is anywhere within the chroot of the ftpd.
     * @param file the file to be checked.
     * @return true if it lies within the path, false otherwise.
     */
    private static boolean isLegalPath(File file) {
        if (file != null) {
            try {
                String s = file.getCanonicalPath();
                return s.startsWith(rcp);
            } catch (IOException e) {
                Logging.getErrorLog().reportCritical("We were unable to get the canonical path for " + file.getAbsolutePath());
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isEmpty(File directory) {
        return (directory.list(forbiddenFiles).length == 0);
    }

    public boolean isOwner(User user, File file) {
        return ServiceManager.getServices().getMetadataHandler().isOwner(user.getUsername(), file);
    }

    public List<String> nlst(String path) throws PermissionDeniedException, AccessControlException, FileNotFoundException {
        File dir;
        if (path != null && !"".equals(path)) {
            dir = resolveFile(path);
        } else {
            dir = pwdFile;
        }
        if (!ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.LIST, resolvePath(dir), user)) {
            throw new PermissionDeniedException(ActionPermission.LIST, resolvePath(dir), user);
        }
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles(forbiddenFiles);
            List<String> listOfFiles = new LinkedList<String>();
            if (files != null && pwdFile.exists()) {
                for (File file : files) {
                    if (!ServiceManager.getServices().getPermissions().isVisible(user, file.getName(), pwd, file.isDirectory())) {
                        continue;
                    }
                    listOfFiles.add(file.getName());
                }
            }
            return listOfFiles;
        } else {
            throw new FileNotFoundException("The specified directory does not exist.");
        }
    }

    /**
     * Returns an ftp-compatible listing the specified directory.
     * @param path a directory.
     * @return a list containing lines that represent the file entities in this directory,
     * formatted as a "ls -la" output.
     * @throws AccessControlException if the policy files forbid access to this directory.
     * @throws FileNotFoundException if the specified directory could not be found.
     * @throws PermissionDeniedException if the permission system denies us entry to this directory.
     */
    public List<String> list(String path) throws AccessControlException, FileNotFoundException, PermissionDeniedException {
        String pwd = this.pwd;
        File pwdFile = this.pwdFile;
        if (path != null) {
            pwdFile = resolveFile(path);
            pwd = resolvePath(pwdFile);
        }
        if (!ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.LIST, pwd, user)) {
            throw new PermissionDeniedException(ActionPermission.LIST, pwd, user);
        }
        File[] files = pwdFile.listFiles(forbiddenFiles);
        LinkedList<String> lines;
        if (files == null || !pwdFile.exists()) {
            throw new FileNotFoundException("Directory not found: " + pwd);
        } else {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            lines = new LinkedList<String>();
            Calendar c = Calendar.getInstance();
            String dateline;
            Directory metadataDirectory = ServiceManager.getServices().getMetadataHandler().getDirectory(pwdFile);
            String group;
            String owner;
            for (File file : files) {
                if (!ServiceManager.getServices().getPermissions().isVisible(user, file.getName(), pwd, file.isDirectory())) {
                    continue;
                }
                c.setTimeInMillis(file.lastModified());
                if (c.get(Calendar.YEAR) == currentYear) {
                    dateline = time.format(c.getTime());
                } else {
                    dateline = year.format(c.getTime());
                }
                owner = "cuftpd";
                group = "cuftpd";
                if (section != null) {
                    owner = section.getOwner();
                    group = section.getGroup();
                }
                if (metadataDirectory != null) {
                    Metadata metadata = metadataDirectory.getMetadata(file.getName());
                    if (metadata != null) {
                        owner = metadata.getUsername();
                        if (!"".equals(metadata.getGroupname()) && metadata.getGroupname() != null) {
                            group = metadata.getGroupname();
                        }
                    } else {
                    }
                } else {
                }
                lines.add(listline.format(new String[] { (file.isDirectory() ? "d" : "-"), owner, group, String.valueOf(file.length()), dateline, file.getName(), (file.canWrite() ? "w" : "-") }));
            }
        }
        return lines;
    }

    public List<String> mlsd(String path) throws FileNotFoundException, PermissionDeniedException {
        File dir = resolveFile(path);
        if (!ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.LIST, resolvePath(dir), user)) {
            throw new PermissionDeniedException(ActionPermission.LIST, resolvePath(dir), user);
        }
        File[] files = dir.listFiles(forbiddenFiles);
        LinkedList<String> lines;
        if (files == null || !dir.exists()) {
            throw new FileNotFoundException("Directory not found: " + pwd);
        } else {
            lines = new LinkedList<String>();
            String type = "unknown";
            Section section = getSection(dir);
            Directory metadataDirectory = ServiceManager.getServices().getMetadataHandler().getDirectory(dir);
            lines.add(mlstResolved(dir, true, section, "cdir", metadataDirectory));
            try {
                File parent = dir.getParentFile();
                if (parent != null) {
                    lines.add(mlstResolved(dir.getParentFile(), true, section, "pdir", metadataDirectory));
                } else {
                    lines.add(mlstResolved(dir, true, section, "pdir", metadataDirectory));
                }
            } catch (AccessControlException e) {
            }
            for (File file : files) {
                if (!ServiceManager.getServices().getPermissions().isVisible(user, file.getName(), pwd, file.isDirectory())) {
                    continue;
                }
                if (file.isFile()) {
                    type = "file";
                } else if (file.isDirectory()) {
                    type = "dir";
                }
                lines.add(mlstResolved(file, false, section, type, metadataDirectory));
            }
        }
        return lines;
    }

    public String mlst(String filename) throws FileNotFoundException {
        File file = resolveFile(filename);
        String type = "unknown";
        if (file.exists()) {
            if (file.isFile()) {
                type = "file";
            } else if (file.isDirectory()) {
                type = "dir";
            }
            Directory metadataDirectory;
            try {
                metadataDirectory = ServiceManager.getServices().getMetadataHandler().getDirectory(file.getParentFile());
            } catch (Exception e) {
                metadataDirectory = ServiceManager.getServices().getMetadataHandler().getDirectory(file);
            }
            return mlstResolved(file, true, getSection(file), type, metadataDirectory);
        } else {
            throw new FileNotFoundException(resolvePath(file));
        }
    }

    private String mlstResolved(File file, boolean displayAbsolutePathname, Section section, String type, Directory metadataDirectory) {
        String owner = "cuftpd";
        String group = "cuftpd";
        if (section != null) {
            owner = section.getOwner();
            group = section.getGroup();
        }
        if (metadataDirectory != null) {
            Metadata metadata = metadataDirectory.getMetadata(file.getName());
            if (metadata != null) {
                owner = metadata.getUsername();
                if (!"".equals(metadata.getGroupname()) && metadata.getGroupname() != null) {
                    group = metadata.getGroupname();
                }
            } else {
            }
        } else {
        }
        StringBuilder sb = new StringBuilder(250);
        sb.append("Type=").append(type).append(";");
        sb.append("Size=").append(file.length()).append(";");
        sb.append("Modify=").append(DATETIME_FORMAT.format(new Date(file.lastModified()))).append(";");
        sb.append("UNIX.owner=").append(owner).append(";");
        sb.append("UNIX.group=").append(group).append(";");
        if (displayAbsolutePathname) {
            sb.append(" ").append(resolvePath(file));
        } else {
            sb.append(" ").append(file.getName());
        }
        return sb.toString();
    }

    public String getFtpParentWorkingDirectory() {
        return pwd;
    }

    /**
     * Returns the string representation of the absolute path of the file representing the current directory
     * @return
     */
    public String getRealParentWorkingDirectoryPath() {
        return pwdFile.getAbsolutePath();
    }

    public String getDotMessageForCurrentDir() {
        try {
            return readTextFile(resolveFile(".message"));
        } catch (Exception e) {
            return null;
        }
    }

    public void shutdown() {
    }

    public void cwd(String directory) throws PermissionDeniedException, AccessControlException, IOException {
        if (".".equals(directory)) {
        } else {
            File t;
            if ("..".equals(directory)) {
                t = pwdFile.getParentFile();
            } else {
                t = resolveFile(directory);
            }
            if (t == null) {
                throw new FileNotFoundException("No such directory.");
            } else if (!t.exists()) {
                throw new FileNotFoundException("No such directory: " + directory);
            } else if (!t.isDirectory()) {
                throw new IOException("Not a directory: " + directory);
            } else if (!t.canExecute()) {
                throw new IOException("Cannot enter directory: " + directory);
            } else if (!isLegalPath(t)) {
                throw new FileNotFoundException("No such directory: " + directory);
            } else {
                String path = resolvePath(t);
                if (!ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.CWD, path, user)) {
                    throw new PermissionDeniedException(ActionPermission.CWD, path, user);
                }
                pwdFile = t;
                if (path.length() == 0) {
                    pwd = "/";
                } else {
                    pwd = path;
                }
                section = getSection(pwd);
            }
        }
    }

    public String mkd(String directory) throws PermissionDeniedException, AccessControlException, IOException {
        File mkd = resolveFile(directory);
        String ftpPathToDir = resolvePath(mkd);
        if (ftpPathToDir == null || !ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.MKDIR, ftpPathToDir, user)) {
            throw new PermissionDeniedException(ActionPermission.MKDIR, ftpPathToDir, user);
        }
        if (!mkd.exists()) {
            boolean success = mkd.mkdirs();
            if (success) {
                ServiceManager.getServices().getMetadataHandler().setOwnership(mkd, user.getUsername(), user.getPrimaryGroup());
                try {
                    if (mkd != null) {
                        File f = mkd.getParentFile();
                        while (f != null && !ServiceManager.getServices().getMetadataHandler().hasOwner(f)) {
                            ServiceManager.getServices().getMetadataHandler().setOwnership(f, user.getUsername(), user.getPrimaryGroup());
                            f = f.getParentFile();
                        }
                    }
                } catch (AccessControlException e) {
                }
                return resolvePath(mkd);
            } else {
                throw new IOException("Could not create directory.");
            }
        } else {
            throw new IOException("Could not create directory: directory exists.");
        }
    }

    public void rmd(String path) throws PermissionDeniedException, AccessControlException, IOException {
        File directory = resolveFile(path);
        String ftpPathToDir = resolvePath(directory);
        if (ftpPathToDir == null || !ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.RMDIR, ftpPathToDir, user)) {
            throw new PermissionDeniedException(ActionPermission.RMDIR, ftpPathToDir, user);
        }
        if (directory.exists()) {
            if (directory.isDirectory()) {
                if (isEmpty(directory)) {
                    File f = new File(directory, ".raceinfo");
                    f.delete();
                    f = new File(directory, ".metadata");
                    f.delete();
                    if (!directory.delete()) {
                        throw new IOException("Failed to remove directory.");
                    }
                    ServiceManager.getServices().getMetadataHandler().delete(directory);
                } else {
                    throw new IOException("Failed to remove directory: directory was not empty.");
                }
            } else {
                throw new IOException("Failed to remove directory: path specified is not a directory.");
            }
        } else {
            throw new FileNotFoundException("No such directory: " + path);
        }
    }

    /**
     * Removes the directory or name specified by name without removing the credits awarded for uploading the directory.
     *
     * @param path the file to be wiped.
     * @return true if the wipe was successful, false otherwise.
     * @throws java.io.FileNotFoundException if the file corresponding to the path did not exist.
     */
    public boolean wipe(String path) throws FileNotFoundException {
        File file = resolveFile(path);
        boolean wasDir = file.isDirectory();
        long length = file.length();
        boolean fileMissing = false;
        if (!file.exists()) {
            fileMissing = true;
        }
        boolean deleted = recursiveDelete(file);
        if (deleted) {
            ServiceManager.getServices().getMetadataHandler().delete(file);
            Event event;
            if (wasDir) {
                event = new Event(Event.REMOVE_DIRECTORY, user, getRealParentWorkingDirectoryPath(), getFtpParentWorkingDirectory());
                event.setProperty("dirlog.deleteRecursively", "true");
            } else {
                event = new Event(Event.DELETE_FILE, user, getRealParentWorkingDirectoryPath(), getFtpParentWorkingDirectory());
                event.setProperty("file.size", String.valueOf(length));
            }
            event.setProperty("file.path.real", resolveRealPath(path));
            event.setProperty("file.path.ftp", resolveFtpPath(path));
            event.setProperty("site.section", getSection(path).getName());
            ServiceManager.getServices().getEventHandler().handleAfterEvent(event);
        } else {
            if (fileMissing) {
                throw new FileNotFoundException("File not deleted: file not found: " + resolvePath(file));
            }
            if (wasDir) {
                file.setWritable(true);
            }
        }
        return deleted;
    }

    /**
     * Deletes all files and folders in the specified dir, recursively.
     *
     * @param dir the dir in which to perform the recursive deletes.
     * @return true if the recursive delete was succesful, false otherwise.
     */
    private boolean recursiveDelete(File dir) {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                boolean ok = recursiveDelete(f);
                if (!ok) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public void delete(String path) throws PermissionDeniedException, AccessControlException, IOException {
        File file = resolveFile(path);
        String ftpPathToFile = resolvePath(file);
        int permission;
        if (isOwner(user, file)) {
            permission = ActionPermission.DELETEOWN;
        } else {
            permission = ActionPermission.DELETE;
        }
        if (!ServiceManager.getServices().getPermissions().hasPermission(permission, ftpPathToFile, user)) {
            throw new PermissionDeniedException(permission, ftpPathToFile, user);
        }
        if (!file.exists()) {
            throw new FileNotFoundException("File not deleted: file not found.");
        } else if (file.isDirectory()) {
            throw new IOException("File not deleted: filename indicates a directory");
        }
        long filesize = file.length();
        if (!file.delete()) {
            throw new IOException("File not deleted: unknown error.");
        }
        if (!user.hasLeech()) {
            Section section = getSection(file);
            int ratio = section.getRatio();
            user.takeCredits(filesize * ratio);
        }
        ServiceManager.getServices().getMetadataHandler().delete(file);
    }

    public void rest(long offset) {
        this.offset = offset;
    }

    public long getOffset() {
        return offset;
    }

    private String readTextFile(File file) throws AccessControlException, IOException {
        StringBuilder sb = new StringBuilder(250);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1"));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return sb.toString();
    }

    /**
     * This allows reading files outside the ftp root. Use a policy file to restrict access to the directory structure.
     * @param path the (full) path of the file. Relative paths are relative from the running dir.
     * @return the contens of the file as a string with newline characters.
     * @throws java.security.AccessControlException throws this exception if reading from that specified file is not permitted.
     * @throws java.io.IOException thrown if the underlying I/O system causes an error.
     */
    public String readExternalTextFile(String path) throws AccessControlException, IOException {
        return readTextFile(new File(path));
    }

    public Section getCurrentSection() {
        return section;
    }

    /**
     * Returns the section with the specified name.
     * If no section matched, the default section is returned.
     *
     * @param name the name of the section in question.
     * @return the section with the name matching. If none is found, a default section is returned.
     * @author [h0D] <d0h@linux.nu>
     */
    public Section getSectionByName(String name) {
        Section section = defaultSection;
        if (name == null) {
            return section;
        }
        for (Map.Entry<String, Section> entry : sections.entrySet()) {
            if (name.equals(entry.getValue().getName())) {
                return entry.getValue();
            }
        }
        return section;
    }

    private void checkRenamePermissions(File file) throws PermissionDeniedException {
        if (isOwner(user, file)) {
            if (!ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.RENAMEOWN, resolvePath(file), user)) {
                throw new PermissionDeniedException(ActionPermission.RENAMEOWN, resolvePath(file), user);
            }
        } else {
            if (!ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.RENAME, resolvePath(file), user)) {
                throw new PermissionDeniedException(ActionPermission.RENAME, resolvePath(file), user);
            }
        }
    }

    public String getRealRenameSource() {
        return renameSource.getAbsolutePath();
    }

    public String getFtpRenameSource() {
        return resolvePath(renameSource);
    }

    public void rnfr(String source) throws PermissionDeniedException, AccessControlException, IOException {
        File t = resolveFile(source);
        if (!t.exists()) {
            throw new FileNotFoundException(FileSystem.resolvePath(t));
        }
        checkRenamePermissions(t);
        renameSource = resolveFile(source);
    }

    public boolean rnto(String target) throws PermissionDeniedException, AccessControlException, IOException {
        if (renameSource != null) {
            File renameTarget = resolveFile(target);
            if ((renameSource.isDirectory() && renameTarget.isFile()) || (renameTarget.exists())) {
                throw new IllegalArgumentException("Can't rename/move a directory to a file, or the target already exists");
            }
            checkRenamePermissions(renameTarget);
            boolean success = renameSource.renameTo(renameTarget);
            if (success) {
                ServiceManager.getServices().getMetadataHandler().move(renameSource, renameTarget, user.getUsername(), user.getPrimaryGroup());
            }
            return success;
        } else {
            throw new IOException("No rename source specified, please issue RNFR first");
        }
    }

    /**
     * Creates a copy of the specified file as "filename.bak" in the same directory.
     * @param file the file to be copied.
     */
    public static void fastBackup(File file) {
        FileChannel in = null;
        FileChannel out = null;
        FileInputStream fin = null;
        FileOutputStream fout = null;
        try {
            in = (fin = new FileInputStream(file)).getChannel();
            out = (fout = new FileOutputStream(file.getAbsolutePath() + ".bak")).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            Logging.getErrorLog().reportError("Fast backup failure (" + file.getAbsolutePath() + "): " + e.getMessage());
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    Logging.getErrorLog().reportException("Failed to close file input stream", e);
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    Logging.getErrorLog().reportException("Failed to close file output stream", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Logging.getErrorLog().reportException("Failed to close file channel", e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Logging.getErrorLog().reportException("Failed to close file channel", e);
                }
            }
        }
    }

    /**
     * Finds a unique filename and returns the corresponding File object.
     * @return a unique file.
     * @param prefix a prefix to the filename to make it mroe difficult to get collisions.
     */
    public File createUniqueFile(String prefix) {
        File f = new File(pwdFile, prefix + Integer.toString(new Random().nextInt() & 0xffff));
        while (f.exists()) {
            System.err.println("we tried to create a unique file that already existed, what are the odds!");
            f = new File(pwdFile, prefix + Integer.toString(new Random().nextInt() & 0xffff));
        }
        return f;
    }

    public Section getDefaultSection() {
        return defaultSection;
    }

    /**
     * Returns the most qualified (longest path name) section for the specified path.
     * If the ftp root is in "/cubnc/site", and the ftp directory we are accessing is "/mp3" (which is physically situated in "/cubnc/site/mp3/", the string that must be passed to this method is "/mp3"
     *
     * @param path an absolute path, with its root in the ftp-root.
     * @return the best matched section. If none is found, a default section is returned.
     */
    public Section getSection(String path) {
        int sizeOfMatch = 0;
        Section section = defaultSection;
        for (Map.Entry<String, Section> entry : sections.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                if (entry.getKey().length() > sizeOfMatch) {
                    sizeOfMatch = entry.getKey().length();
                    section = entry.getValue();
                }
            }
        }
        return section;
    }

    /**
     * Returns the section that the file indicated belongs to.<br>
     * It does this by resolving the file to a path and then invokes #getSection(String)
     * @param file the file in question
     * @return the section the file belongs to
     */
    public Section getSection(File file) {
        return getSection(resolvePath(file));
    }

    /**
     * Returns the length of the file specified byt the ftp path.
     *
     * @param file FTP path of the file we want to know the length of.
     * @return the file length
     * @throws FileNotFoundException if the file was not found
     */
    public long length(String file) throws FileNotFoundException {
        File f = resolveFile(file);
        if (!f.exists()) {
            throw new FileNotFoundException(resolvePath(f));
        }
        return f.length();
    }

    public long lastModified(String file) throws FileNotFoundException {
        File f = resolveFile(file);
        if (!f.exists()) {
            throw new FileNotFoundException(resolvePath(f));
        }
        return f.lastModified();
    }

    public String xmd5(String filename, long start, long end) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        return digest(filename, digest, start, end);
    }

    public String xsha1(String filename, long start, long end) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return digest(filename, digest, start, end);
    }

    private String digest(String filename, MessageDigest digest, long start, long end) throws IOException {
        File file = resolveFile(filename);
        if (!file.exists() && filename.startsWith("\"") && filename.endsWith("\"")) {
            file = resolveFile(filename.substring(1, filename.length() - 1));
        }
        FileInputStream in = null;
        try {
            byte[] buf = new byte[8192];
            int len;
            if (start == 0 && end == 0) {
                in = new FileInputStream(file);
            } else {
                in = new LimitedFileInputStream(file, start, end);
            }
            while ((len = in.read(buf, 0, buf.length)) != -1) {
                digest.update(buf, 0, len);
            }
            return new String(Hex.bytesToHex(digest.digest()));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Logging.getErrorLog().reportException("Failed to close file input stream", e);
                }
            }
        }
    }

    public String xcrc(String filename, long start, long end) throws IOException {
        File file = resolveFile(filename);
        if (!file.exists() && filename.startsWith("\"") && filename.endsWith("\"")) {
            file = resolveFile(filename.substring(1, filename.length() - 1));
        }
        CRC32 crc = new CRC32();
        FileInputStream in = null;
        try {
            byte[] buf = new byte[8192];
            int len;
            if (start == 0 && end == 0) {
                in = new FileInputStream(file);
            } else {
                in = new LimitedFileInputStream(file, start, end);
            }
            while ((len = in.read(buf, 0, buf.length)) != -1) {
                crc.update(buf, 0, len);
            }
            return Long.toHexString(crc.getValue()).toUpperCase();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public void setLastModified(String filename, long time) throws PermissionDeniedException, FileNotFoundException {
        File file = resolveFile(filename);
        if (file.exists()) {
            if (!ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.SETTIME, resolvePath(file), user)) {
                throw new PermissionDeniedException(ActionPermission.SETTIME, resolvePath(file), user);
            }
            file.setLastModified(time);
        } else {
            throw new FileNotFoundException(resolvePath(file));
        }
    }

    public void chown(String path, String username, String groupname, boolean recursive) throws NoSuchGroupException, NoSuchUserException {
        if (username != null) {
            ServiceManager.getServices().getUserbase().getUser(username);
        }
        if (groupname != null) {
            ServiceManager.getServices().getUserbase().getGroup(groupname);
        }
        File file = resolveFile(path);
        ServiceManager.getServices().getMetadataHandler().setOwnership(file, username, groupname, recursive);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public long getFreeSpaceInParentWorkingDirectory() {
        return pwdFile.getFreeSpace();
    }

    public long getTotalSpaceInParentWorkingDirectory() {
        return pwdFile.getTotalSpace();
    }

    /**
     * Accepts a real filesystem path and determines whether or not this is a directory.
     * @param realPath The path to be checked.
     * @return true if the path represents a directory, false otherwise
     */
    public boolean isDirectory(String realPath) {
        File f = new File(realPath);
        return f.isDirectory();
    }
}
