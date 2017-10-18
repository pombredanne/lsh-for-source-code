package org.mitre.rt.client.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlObject;
import org.mitre.cpe.language.x20.PlatformSpecificationType;
import org.mitre.rt.client.core.DataManager;
import org.mitre.rt.rtclient.ApplicationType;
import org.mitre.rt.rtclient.FileType;
import org.mitre.rt.client.core.IDManager;
import org.mitre.rt.common.util.files.CopyFile;
import org.mitre.rt.client.properties.RTClientProperties;
import org.mitre.rt.client.properties.RTClientSyncProperties;
import org.mitre.rt.rtclient.ApplicationType.Files;
import org.mitre.rt.rtclient.ChangeTypeEnum;
import org.mitre.rt.rtclient.RTDocument;
import org.mitre.rt.rtclient.RTDocument.RT;

/**
 *
 * @author bworrell
 */
public class FileTypeHelper extends AbsHelper<FileType, ApplicationType> {

    private Logger logger = Logger.getLogger(FileTypeHelper.class.getPackage().getName());

    public FileTypeHelper() {
        super("FileRef");
    }

    /**
   * Moves a file to <rt_install_dir>/tmp/<app_id>.
   *
   * Deletion of files only occurs during the synchronization process, so we
   * need to be able to rollback file deletions if an error occurs at some point
   * during the sync as the database and xml instance will be rolled back to the
   * state prior to syncing.
   * @param path
   * @return
   */
    public boolean removeSystemFile(String appId, String path) {
        File srcFile = new File(path);
        boolean moved = true;
        if (srcFile.exists()) {
            File tmpDir = this.getTmpFileDir(appId);
            if (tmpDir.exists() == false) tmpDir.mkdir();
            String destFile = tmpDir.getPath() + File.separator + srcFile.getName();
            try {
                this.moveSystemFile(srcFile, destFile);
            } catch (IOException ex) {
                moved = false;
                logger.warn(ex);
            }
        }
        return moved;
    }

    /**
   * Deletes a file or a directory
   * @param file
   * @return true if the file/directory was deleted, false otherwise
   */
    private boolean deleteSystemFile(File file) {
        if (file.isDirectory()) {
            for (File toDelete : file.listFiles()) {
                this.deleteSystemFile(toDelete);
            }
        }
        return file.delete();
    }

    /**
 *  Checks each file in the permanent files dir to see if it is referenced in
 * the RT. If not, the physical file is deleted.
 */
    public void cleanupUnreferencedFiles() throws Exception {
        final ApplicationHelper helper = new ApplicationHelper();
        RT rt = DataManager.instance().getRTDocument().getRT();
        RTClientProperties props = RTClientProperties.instance();
        String filesDirName = props.getFilesDir();
        File dir = new File(filesDirName);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.warn("Couldn't cleanup excess files");
            return;
        }
        for (File appDir : dir.listFiles()) {
            ApplicationType app = (rt.isSetApplications()) ? helper.getItem(rt.getApplications().getApplicationList(), appDir.getName()) : null;
            if (app != null) {
                String ovalFileId = null;
                String dictionaryFileId = null;
                if (app.isSetPlatformReferences()) {
                    ovalFileId = app.getPlatformReferences().getOVALFile();
                    dictionaryFileId = app.getPlatformReferences().getDictionaryFile();
                }
                for (File checkFile : appDir.listFiles()) {
                    String id = checkFile.getName();
                    int dot = id.indexOf(".");
                    if (dot >= 0) id = id.substring(0, dot);
                    boolean isOvalFile = ovalFileId != null && ovalFileId.equals(id);
                    boolean isDictionaryFile = dictionaryFileId != null && dictionaryFileId.equals(id);
                    if (!super.isReferenced(id, app) && (!isOvalFile) && (!isDictionaryFile)) {
                        boolean inPS = false;
                        if (app.isSetPlatformSpecification()) {
                            PlatformSpecificationType ps = app.getPlatformSpecification();
                            String xPathCheckFact = "$this//*:check-fact-ref/@href=" + "'" + id + "'";
                            XmlObject[] xmlObjCF = ps.selectPath(xPathCheckFact);
                            if (xmlObjCF.length > 0) {
                                XmlBoolean b = (XmlBoolean) xmlObjCF[0];
                                inPS = b.getBooleanValue();
                            }
                        }
                        if (!inPS) {
                            if (checkFile.delete()) {
                                logger.info("cleanupUnreferencedFiles: removed unreferenced file: " + checkFile.getPath());
                            } else {
                                logger.warn("cleanupUnreferencedFiles: unable to remove unreferenced file: " + checkFile.getPath());
                            }
                        }
                    }
                }
                if (appDir.listFiles().length == 0) {
                    appDir.delete();
                }
            } else {
                this.deleteSystemFile(appDir);
            }
        }
    }

    /**
 * Copies a file from the source to destination and then deletes the source file, effectively moving the file
 * @param sourceFile
 * @param destFile
 * @return
 * @throws java.io.IOException
 */
    public boolean moveSystemFile(File sourceFile, String destFile) throws IOException {
        File movedFile = CopyFile.copyFile(sourceFile, destFile);
        return (sourceFile.getPath().equals(movedFile.getPath()) == false) ? sourceFile.delete() : true;
    }

    /**
   * Deletes the given Application's temp file store directory and all of its contained files/subdirectories
   * @param app
   * @return
   */
    public boolean deleteTmpAppDir(String appId) {
        File tmpDir = this.getTmpFileDir(appId);
        return (tmpDir.exists() == true) ? this.deleteSystemFile(tmpDir) : true;
    }

    /**
   * Returns a File object pointing to an Application's temp directory for file storage during sync/update deletes
   * @param appId
   * @return
   */
    private File getTmpFileDir(String appId) {
        RTClientProperties props = RTClientProperties.instance();
        return new File(props.getTempDir() + File.separator + appId);
    }

    /**
   * Copies files from <rt_install_dir>/tmp/<app_id> back to <rt_install_dir>/files/<app_id>
   * The temp directory should be wiped out after this is called if the rollback completes
   * successfully.
   * @param app
   * @return true if all temp files were copied back successfully, false if an IOException occurred
   */
    public boolean rollbackSystemFiles(String appId) {
        logger.debug("Rolling back files for Application Id: " + appId);
        boolean rolledBack = true;
        String destDir = RTClientProperties.instance().getFilesDir() + File.separator + appId;
        File tmpDir = this.getTmpFileDir(appId);
        if (tmpDir.exists() == true && tmpDir.isDirectory() == true) {
            try {
                for (File file : tmpDir.listFiles()) {
                    String dest = destDir + File.separator + file.getName();
                    CopyFile.copyFile(file, dest);
                }
            } catch (IOException ex) {
                rolledBack = false;
                logger.warn(ex);
            }
        } else logger.debug("The temp directory for: " + appId + " was not found or not a directory");
        return rolledBack;
    }

    /**
   * A wrapper for this.removeFile(...): moves a file from the files directory to the tmp directory
   * @param app
   * @param file
   * @return
   */
    public boolean removeSystemFile(ApplicationType app, FileType file) {
        RTClientProperties props = RTClientProperties.instance();
        String path = this.getFilePath(file);
        return this.removeSystemFile(app.getId(), path);
    }

    public void createFilesDirectory(String appId) throws Exception {
        RTClientProperties props = RTClientProperties.instance();
        String strFilesDir = props.getFilesDir(), strAppDir = strFilesDir + File.separator + appId;
        File fileAppDir = new File(strAppDir);
        if (fileAppDir.exists() == false) {
            if (fileAppDir.mkdir() == false) throw new Exception("Error creating application file storage directory: " + strAppDir);
        }
    }

    /**
   * Returns the extension of the file: foo.zip returns ".zip"
   * @param fileName
   * @return
   */
    public String getFileExtension(String fileName) {
        String extension = "";
        int period = fileName.lastIndexOf('.');
        if (period != -1) extension = fileName.substring(period);
        return extension;
    }

    /**
   * Returns the the directory containing the file denoted by the @filePath parameter
   * @param filePath
   * @return
   */
    public String getFileDir(String filePath) {
        String path = "";
        int lastSeparator = filePath.lastIndexOf(File.separator);
        if (lastSeparator != -1) {
            path = filePath.substring(0, lastSeparator);
        }
        return path;
    }

    public boolean isValidFile(String path) {
        File testFile = new File(path);
        return (path.isEmpty() == false && testFile.exists() == true && testFile.canRead() == true);
    }

    public String getMimeType(File file) throws Exception {
        String mimeType = "TODO";
        return mimeType;
    }

    /**
   * Copies a file from its original location to the <rt_files_dir>/<app_id> (usually <rt_install_dir>/files/<app_id>).
   * The file is renamed to reflect the FileType id it was given: foobar.zip -> 2-15.zip
   * @param app
   * @param fileType
   * @param file
   * @param changeFileTypeMetaData
   * @throws java.lang.Exception
   */
    public void relocateFile(ApplicationType app, FileType fileType, File file, boolean changeFileTypeMetaData) throws Exception {
        RTClientProperties props = RTClientProperties.instance();
        String files = props.getFilesDir(), origPath = file.getCanonicalPath(), extension = this.getFileExtension(fileType.getOrigFileName()), newPath = File.separator + app.getId(), newName = fileType.getId() + extension, absNewPath = files + newPath + File.separator + newName;
        this.createFilesDirectory(app.getId());
        CopyFile.copyFile(origPath, absNewPath, true);
        if (changeFileTypeMetaData == true) {
            fileType.setPath(newPath);
            fileType.setFileName(newName);
            fileType.setMimeType(this.getMimeType(file));
            fileType.setSize(file.length());
            super.markModified(fileType);
        }
    }

    public void relocateFile(ApplicationType app, FileType fileType, File file) throws Exception {
        this.relocateFile(app, fileType, file, true);
    }

    /**
   * Returns the path to the file contained by the FileType parameter
   * @param file
   * @return
   */
    public String getFilePath(FileType file) {
        RTClientProperties props = RTClientProperties.instance();
        StringBuffer path = new StringBuffer();
        if (file != null) {
            path.append(props.getFilesDir());
            path.append(File.separator);
            path.append(file.getPath());
            path.append(File.separator);
            path.append(file.getFileName());
        }
        return path.toString();
    }

    /**
   * Saves a temp file returned from the server RT root/files/app_id/filetype_id.ext
   */
    public void saveTempFile(ApplicationType app, FileType fileType, File sourceFile) throws Exception {
        RTClientProperties props = RTClientProperties.instance();
        StringBuilder absNewPath = new StringBuilder();
        absNewPath.append(props.getFilesDir());
        absNewPath.append(File.separator);
        absNewPath.append(app.getId());
        absNewPath.append(File.separator);
        absNewPath.append(fileType.getId());
        absNewPath.append(this.getFileExtension(fileType.getOrigFileName()));
        this.createFilesDirectory(app.getId());
        File file = CopyFile.copyFile(sourceFile, absNewPath.toString());
        file.setLastModified(fileType.getModified().getTimeInMillis());
    }

    /**
  * Computes the MD5 hash sum for a FileType's corresponding (on-disc) file
  * @param file
  * @return
  */
    public byte[] getMD5(FileType file) {
        byte[] hash = null;
        InputStream input = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            File appFile = new File(this.getFilePath(file));
            if (appFile.exists() && appFile.canRead()) {
                int read = 0;
                byte[] buffer = new byte[4096];
                input = new FileInputStream(appFile);
                while ((read = input.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
                hash = digest.digest();
            }
        } catch (Exception ex) {
            logger.warn(ex);
        } finally {
            try {
                if (input != null) input.close();
            } catch (Exception ex) {
                logger.warn(ex);
            }
        }
        return hash;
    }

    /**
     * Returns the last modified date for the file referenced by the FileType object.
     * This is similar to stat-ing a file and pulling the last modified date out.
     * @param file
     * @return
     */
    public long getLastModified(FileType file) {
        File appFile = new File(this.getFilePath(file));
        return (appFile.exists()) ? appFile.lastModified() : -1;
    }

    /**
     * Returns the last modified date/time for a system file referenced by a FileType object
     * with an id of @id
     * @param app
     * @param id
     * @return
     */
    public long getLastModified(ApplicationType app, String id) {
        long modified = 0L;
        try {
            if (app.isSetFiles()) {
                FileType file = super.getItem(app.getFiles().getFileList(), id);
                modified = this.getLastModified(file);
            }
        } catch (Exception ex) {
            logger.warn(ex);
        }
        return modified;
    }

    /**
     * Files are named according to their Ids. The first half of file name is
     * the client id. When a user connects to a server for the first time, it is
     * assigned a new client id. All the ids in the RT document are changed
     * accordingly and the file names and directories need to be changed as well.
     * This method changes the names of the files and directories, but not the ids,
     * which is done by RTHelper.relaceClientId()
     *
     * @param rtDoc
     * @param oldId the old client id
     * @param newId the new client id
     */
    public void renameFilesClientId(RTDocument rtDoc, String oldId, String newId) {
        if (oldId.equals(newId)) {
            return;
        }
        String oldIdWithHyphen = oldId + "-";
        String newIdWithHyphen = newId + "-";
        RTClientProperties clientProps = RTClientProperties.instance();
        File filesDir = new File(clientProps.getFilesDir());
        if (!filesDir.exists() || !filesDir.isDirectory()) {
            logger.info("renameFilesClientId: the files directory doesn't exist");
            return;
        }
        File[] appDirs = filesDir.listFiles();
        for (File dir : appDirs) {
            String dirName = dir.getName();
            if (dirName.startsWith(oldIdWithHyphen)) {
                String newDirName = filesDir.getPath() + File.separatorChar + dirName.replace(oldIdWithHyphen, newIdWithHyphen);
                boolean didRename = dir.renameTo(new File(newDirName));
                if (didRename) {
                    logger.debug("renameFilesClientId: renamed directory " + dirName + " to " + newDirName);
                } else {
                    logger.warn("renameFilesClientId: failed to renamed directory " + dirName + " to " + newDirName);
                }
            }
        }
        String xPath = "//*:File";
        XmlObject[] xmlObjs = rtDoc.selectPath(xPath);
        for (XmlObject current : xmlObjs) {
            if (current instanceof FileType) {
                FileType fileType = (FileType) current;
                String oldPath = fileType.getPath();
                int index = oldPath.startsWith(File.separator) ? 1 : 0;
                if (oldPath.indexOf(oldIdWithHyphen, index) == index) {
                    fileType.setPath(oldPath.replace(oldIdWithHyphen, newIdWithHyphen));
                }
                String oldName = fileType.getFileName();
                if (oldName.startsWith(oldIdWithHyphen)) {
                    File physicalFile = new File(this.getFilePath(fileType));
                    String newName = oldName.replace(oldIdWithHyphen, newIdWithHyphen);
                    fileType.setFileName(newName);
                    File newPhysicalFile = new File(this.getFilePath(fileType));
                    boolean didRename = physicalFile.renameTo(newPhysicalFile);
                    if (didRename) {
                        logger.debug("renameFilesClientId: renamed file: " + oldName + " to " + newName);
                    } else {
                        logger.warn("renameFilesClientId: failed to renamed file: " + oldName + " to " + newName);
                    }
                }
            }
        }
    }

    /**
     * Checks all the FileType elements in the RT document to see if the user
     * modified the disc file represented by the FileType element. If so, the
     * FileType element is marked as modified.
     *
     * The FileType element must have a change type of NONE and the modification
     * date on the file has to be newer than the last sync date for it to be
     * considered modified.
     *
     * @param rt
     */
    public void checkForUpdatedFiles(RT rt) {
        if (!rt.isSetApplications()) return;
        RTClientSyncProperties syncProps = RTClientSyncProperties.instance();
        long lastGlobalSync = syncProps.getLastGlobalUpdateSystemDate();
        for (ApplicationType app : rt.getApplications().getApplicationList()) {
            if (app.isSetFiles()) {
                Date appSyncDate = syncProps.getLastUpdateDate(app);
                long lastAppSync = -1;
                if (appSyncDate != null) {
                    lastAppSync = appSyncDate.getTime();
                }
                long lastSync = lastGlobalSync > lastAppSync ? lastGlobalSync : lastAppSync;
                for (FileType currentFileType : app.getFiles().getFileList()) {
                    if (currentFileType.getChangeType().equals(ChangeTypeEnum.NONE)) {
                        long lastFileMod = this.getLastModified(currentFileType);
                        long fileTypeModTime = currentFileType.getModified().getTimeInMillis();
                        if ((lastFileMod > fileTypeModTime) && (lastFileMod > lastSync)) {
                            markModified(currentFileType);
                            logger.info("checkForUpdatedFiles: " + currentFileType.getFileName() + ": syncDate: " + lastSync + " : file last_mod : " + lastFileMod + " : filetype mod: " + fileTypeModTime);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getNewId(ApplicationType app) {
        String newId = IDManager.getNextGeneralId(app.getId());
        if (app.isSetFiles()) {
            List<FileType> fileList = app.getFiles().getFileList();
            boolean found = false;
            do {
                for (FileType mt : fileList) {
                    if (mt.getId() != null && mt.getId().equals(newId)) {
                        newId = IDManager.getNextGeneralId(app.getId());
                        logger.debug("getNewFileType: new id is already in use. Trying a new new Id " + newId);
                        found = true;
                        break;
                    }
                    found = false;
                }
            } while (found);
        }
        return newId;
    }

    public FileType getFileByName(ApplicationType application, String filename) {
        FileType foundFile = null;
        if (application.isSetFiles()) {
            for (FileType f : application.getFiles().getFileList()) {
                if (f.getOrigFileName().equals(filename)) {
                    foundFile = f;
                    break;
                }
            }
        }
        return foundFile;
    }

    public FileType makeNewFileType(ApplicationType application, String filename, String location) {
        FileType ft = null;
        String filePath = location + File.separator + filename;
        File file = new File(filePath);
        if (isValidFile(filePath)) {
            try {
                Files files = (application.isSetFiles() == true) ? application.getFiles() : application.addNewFiles();
                FileType newFile = getNewItem(application);
                ft = files.addNewFile();
                newFile.setOrigFileName(file.getName());
                newFile.setOrigPath(location);
                ft.set(newFile);
                relocateFile(application, ft, file);
            } catch (Exception ex) {
                logger.error(ex, ex);
            }
        }
        return ft;
    }

    @Override
    protected FileType getInstance() {
        return FileType.Factory.newInstance();
    }
}
