package com.dotmarketing.webdav;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.catalina.util.MD5Encoder;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.velocity.runtime.resource.ResourceManager;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.Resource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FileCache;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.cache.IdentifierCache;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.factories.PublicUserFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.HostFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.files.factories.FileFactory;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.action.EditFolderAction;
import com.dotmarketing.portlets.folders.factories.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.DotResourceCache;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.FileUtil;

public class DotWebdavHelper {

    private String authType = PublicCompanyFactory.getDefaultCompany().getAuthType();

    private static String PRE_AUTHENTICATOR = PropsUtil.get("auth.pipeline.pre");

    private static ThreadLocal<Perl5Matcher> localP5Matcher = new ThreadLocal<Perl5Matcher>() {

        protected Perl5Matcher initialValue() {
            return new Perl5Matcher();
        }
    };

    private org.apache.oro.text.regex.Pattern tempResourcePattern;

    private java.io.File tempHolderDir;

    private String tempFolderPath = "dotwebdav";

    /**
	 * MD5 message digest provider.
	 */
    private static MessageDigest md5Helper;

    /**
	 * The MD5 helper object for this class.
	 */
    private static final MD5Encoder md5Encoder = new MD5Encoder();

    private Hashtable<String, com.bradmcevoy.http.LockInfo> resourceLocks = new Hashtable<String, com.bradmcevoy.http.LockInfo>();

    public DotWebdavHelper() {
        Perl5Compiler c = new Perl5Compiler();
        try {
            tempResourcePattern = c.compile("/\\(.*\\)|/._\\(.*\\)|/\\.|^\\.|^\\(.*\\)", Perl5Compiler.READ_ONLY_MASK);
        } catch (MalformedPatternException mfe) {
            Logger.fatal(this, "Unable to instaniate webdav servlet : " + mfe.getMessage(), mfe);
            Logger.error(this, mfe.getMessage(), mfe);
        }
        try {
            tempHolderDir = java.io.File.createTempFile("placeHolder", "dot");
            String tp = tempHolderDir.getParentFile().getPath() + java.io.File.separator + tempFolderPath;
            FileUtil.deltree(tempHolderDir);
            tempHolderDir = new java.io.File(tp);
            tempHolderDir.mkdirs();
        } catch (IOException e1) {
            Logger.error(this, "Unable to setup temp folder for webdav");
            Logger.error(this, e1.getMessage(), e1);
        }
        try {
            md5Helper = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException("No MD5", e);
        }
    }

    public boolean isAutoPub(String path) {
        if (path.startsWith("/webdav/autopub")) {
            return true;
        }
        return false;
    }

    public User authorizePrincipal(String username, String passwd) throws DotSecurityException {
        User _user;
        boolean useEmailAsLogin = true;
        Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
        if (comp.getAuthType().equals(Company.AUTH_TYPE_ID)) {
            useEmailAsLogin = false;
        }
        try {
            if (!PRE_AUTHENTICATOR.equals("") && PRE_AUTHENTICATOR != null) {
                Authenticator authenticator;
                authenticator = (Authenticator) new bsh.Interpreter().eval("new " + PRE_AUTHENTICATOR + "()");
                if (useEmailAsLogin) {
                    authenticator.authenticateByEmailAddress(comp.getCompanyId(), username, passwd);
                } else {
                    authenticator.authenticateByUserId(comp.getCompanyId(), username, passwd);
                }
            }
        } catch (AuthException ae) {
            Logger.debug(this, "Username : " + username + " failed to login", ae);
            throw new DotSecurityException(ae.getMessage(), ae);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotSecurityException(e.getMessage(), e);
        }
        if (comp.getAuthType().equals(Company.AUTH_TYPE_ID)) {
            _user = PublicUserFactory.getUserByUserId(username);
        } else {
            _user = PublicUserFactory.getUserByEmail(username);
        }
        if (PublicEncryptionFactory.digestString(passwd).equals(_user.getPassword())) {
            return _user;
        } else if (_user == null) {
            throw new DotSecurityException("The user was returned NULL");
        } else {
            Logger.debug(this, "The user's passwords didn't match");
            throw new DotSecurityException("The user's passwords didn't match");
        }
    }

    public boolean isFolder(String uriAux) throws IOException {
        Logger.debug(this, "isFolder");
        boolean returnValue = false;
        Logger.debug(this, "Method isFolder: the uri is " + uriAux);
        if (uriAux.equals("") || uriAux.equals("/")) {
            returnValue = true;
        } else {
            uriAux = stripMapping(uriAux);
            String hostName = getHostname(uriAux);
            Logger.debug(this, "Method isFolder: the hostname is " + hostName);
            Host host = HostFactory.getHostByHostName(hostName);
            if (host.getInode() != 0) {
                String path = getPath(uriAux);
                Logger.debug(this, "Method isFolder: the path is " + path);
                if (path.equals("") || path.equals("/")) {
                    returnValue = true;
                } else {
                    if (!path.endsWith("/")) path += "/";
                    if (path.contains("mvdest2")) {
                        Logger.debug(this, "is mvdest2 a folder");
                    }
                    Folder folder = FolderFactory.getFolderByPath(path, host);
                    if (folder.getInode() != 0) {
                        returnValue = true;
                    }
                }
            }
        }
        return returnValue;
    }

    public boolean isResource(String uri) throws IOException {
        uri = stripMapping(uri);
        Logger.debug(this.getClass(), "In the Method isResource");
        if (uri.endsWith("/")) {
            return false;
        }
        boolean returnValue = false;
        String hostName = getHostname(uri);
        Host host = HostFactory.getHostByHostName(hostName);
        if (host == null) {
            Logger.debug(this, "isResource Method: Host is NULL");
        } else {
            Logger.debug(this, "isResource Method: host inode is " + host.getInode() + " and the host name is " + host.getHostname());
        }
        String path = getPath(uri);
        String folderName = getFolderName(path);
        Folder folder = FolderFactory.getFolderByPath(folderName, host);
        String fileName = getFileName(path);
        fileName = deleteSpecialCharacter(fileName);
        if (host.getInode() != 0) {
            try {
                returnValue = FileFactory.existsFileName(folder, fileName);
            } catch (Exception ex) {
            }
        }
        return returnValue;
    }

    public File loadFile(String url) {
        url = stripMapping(url);
        String hostName = getHostname(url);
        url = getPath(url);
        Host host = HostFactory.getHostByHostName(hostName);
        return FileFactory.getFileByURI(url, host, false);
    }

    public Folder loadFolder(String url) {
        url = stripMapping(url);
        String hostName = getHostname(url);
        url = getPath(url);
        Host host = HostFactory.getHostByHostName(hostName);
        return FolderFactory.getFolderByPath(url, host);
    }

    public java.io.File loadTempFile(String url) {
        url = stripMapping(url);
        Logger.debug(this, "Getting temp file from path " + url);
        java.io.File f = new java.io.File(tempHolderDir.getPath() + url);
        return f;
    }

    public List<Resource> getChildrenOfFolder(Folder folder, boolean isAutoPub) {
        String prePath;
        if (isAutoPub) {
            prePath = "/webdav/autopub/";
        } else {
            prePath = "/webdav/nonpub/";
        }
        Host folderHost = HostFactory.getHost(folder.getHostInode());
        List<Folder> folderListSubChildren = FolderFactory.getFoldersByParent(folder.getInode());
        List<File> filesListSubChildren = InodeFactory.getChildrenClassByCondition(folder, File.class, "(working = " + DbConnectionFactory.getDBTrue() + ") and type='file_asset'");
        List<Resource> result = new ArrayList<Resource>();
        for (File f : filesListSubChildren) {
            FileResourceImpl fr = new FileResourceImpl(f, prePath + folderHost.getHostname() + "/" + f.getPath());
            result.add(fr);
        }
        for (Folder f : folderListSubChildren) {
            FolderResourceImpl fr = new FolderResourceImpl(f, prePath + folderHost.getHostname() + "/" + f.getPath());
            result.add(fr);
        }
        String p = folder.getPath();
        if (p.contains("/")) p.replace("/", java.io.File.separator);
        java.io.File tempDir = new java.io.File(tempHolderDir.getPath() + java.io.File.separator + folderHost.getHostname() + p);
        p = folder.getPath();
        if (!p.endsWith("/")) p = p + "/";
        if (!p.startsWith("/")) p = "/" + p;
        if (tempDir.exists() && tempDir.isDirectory()) {
            java.io.File[] files = tempDir.listFiles();
            for (java.io.File file : files) {
                String tp = prePath + folderHost.getHostname() + p + file.getName();
                if (!isTempResource(tp)) {
                    continue;
                }
                if (file.isDirectory()) {
                    TempFolderResourceImpl tr = new TempFolderResourceImpl(tp, file, isAutoPub);
                    result.add(tr);
                } else {
                    TempFileResourceImpl tr = new TempFileResourceImpl(file, tp, isAutoPub);
                    result.add(tr);
                }
            }
        }
        return result;
    }

    public java.io.File getTempDir() {
        return tempHolderDir;
    }

    public String getHostName(String uri) {
        String hostname = stripMapping(uri);
        hostname = getHostname(uri);
        return hostname;
    }

    public boolean isTempResource(String path) {
        Perl5Matcher matcher = (Perl5Matcher) localP5Matcher.get();
        if (matcher.contains(path, tempResourcePattern)) return true;
        return false;
    }

    public java.io.File createTempFolder(String path) {
        path = stripMapping(path);
        if (path.startsWith(tempHolderDir.getPath())) path = path.substring(tempHolderDir.getPath().length(), path.length());
        if (path.startsWith("/") || path.startsWith("\\")) {
            path = path.substring(1, path.length());
        }
        path = path.replace("/", java.io.File.separator);
        java.io.File f = new java.io.File(tempHolderDir.getPath() + java.io.File.separator + path);
        f.mkdirs();
        return f;
    }

    public void copyFolderToTemp(Folder folder, java.io.File tempFolder, String name, boolean isAutoPub) throws IOException {
        String p = folder.getPath();
        if (p.endsWith("/")) p = p + "/";
        String path = p.replace("/", java.io.File.separator);
        path = tempFolder.getPath() + java.io.File.separator + name;
        java.io.File tf = createTempFolder(path);
        List<Resource> children = getChildrenOfFolder(folder, isAutoPub);
        for (Resource resource : children) {
            if (resource instanceof CollectionResource) {
                FolderResourceImpl fr = (FolderResourceImpl) resource;
                copyFolderToTemp(fr.getFolder(), tf, fr.getFolder().getName(), isAutoPub);
            } else {
                FileResourceImpl fr = (FileResourceImpl) resource;
                copyFileToTemp(fr.getFile(), tf);
            }
        }
    }

    public java.io.File copyFileToTemp(File file, java.io.File tempFolder) throws IOException {
        java.io.File f = FileFactory.getAssetIOFile(file);
        java.io.File nf = new java.io.File(tempFolder.getPath() + java.io.File.separator + f.getName());
        FileUtil.copyFile(f, nf);
        return nf;
    }

    public java.io.File createTempFile(String path) throws IOException {
        java.io.File file = new java.io.File(tempHolderDir.getPath() + path);
        String p = file.getPath().substring(0, file.getPath().lastIndexOf(java.io.File.separator));
        java.io.File f = new java.io.File(p);
        f.mkdirs();
        file.createNewFile();
        return file;
    }

    public void copyTempDirToStorage(java.io.File fromFileFolder, String destPath, User user, boolean autoPublish) throws IOException {
        if (fromFileFolder == null || !fromFileFolder.isDirectory()) {
            throw new IOException("The temp source file must be a directory");
        }
        destPath = stripMapping(destPath);
        if (destPath.endsWith("/")) destPath = destPath + "/";
        createFolder(destPath, user);
        java.io.File[] files = fromFileFolder.listFiles();
        for (java.io.File file : files) {
            if (file.isDirectory()) {
                copyTempDirToStorage(file, destPath + file.getName(), user, autoPublish);
            } else {
                copyTempFileToStorage(file, destPath + file.getName(), user, autoPublish);
            }
        }
    }

    public void copyTempFileToStorage(java.io.File fromFile, String destPath, User user, boolean autoPublish) throws IOException {
        destPath = stripMapping(destPath);
        if (fromFile == null) {
            throw new IOException("The temp source file must exist");
        }
        InputStream in = new FileInputStream(fromFile);
        createResource(destPath, autoPublish, user);
        setResourceContent(destPath, in, null, null, new Date(fromFile.lastModified()));
    }

    public void copyResource(String fromPath, String toPath, User user, boolean autoPublish) throws IOException {
        createResource(toPath, autoPublish, user);
        setResourceContent(toPath, getResourceContent(fromPath), null, null);
    }

    public void copyFolder(String sourcePath, String destinationPath, User user, boolean autoPublish) throws IOException, DotDataException {
        destinationPath = stripMapping(destinationPath);
        sourcePath = stripMapping(sourcePath);
        PermissionAPI perAPI = APILocator.getPermissionAPI();
        createFolder(destinationPath, user);
        Summary[] children = getChildrenData(sourcePath, user);
        for (int i = children.length - 1; i >= 0; i--) {
            try {
                if (!children[i].isFolder()) {
                    createResource(destinationPath + "/" + children[i].getName(), autoPublish, user);
                    setResourceContent(destinationPath + "/" + children[i].getName(), getResourceContent(sourcePath + "/" + children[i].getName()), null, null);
                    boolean live = false;
                    File destinationFile = FileFactory.getFileByURI(destinationPath + "/" + children[i].getName(), children[i].getHost(), live);
                    List<Permission> list = perAPI.getPermissions(destinationFile);
                    Iterator iter = list.iterator();
                    while (iter.hasNext()) {
                        Permission p = (Permission) iter.next();
                        perAPI.delete(p);
                    }
                    list = perAPI.getPermissions(children[i].getFile());
                    iter = list.iterator();
                    while (iter.hasNext()) {
                        Permission p = (Permission) iter.next();
                        Permission newPerm = new Permission();
                        newPerm.setPermission(p.getPermission());
                        newPerm.setRoleId(p.getRoleId());
                        newPerm.setInode(destinationFile.getIdentifier());
                        perAPI.save(newPerm);
                    }
                } else {
                    copyFolder(sourcePath + "/" + children[i].getName(), destinationPath + "/" + children[i].getName(), user, autoPublish);
                }
            } catch (IOException e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
        String sourceHostName = getHostname(sourcePath);
        String sourceFolderName = getPath(sourcePath);
        Host sourceHost = HostFactory.getHostByHostName(sourceHostName);
        Folder sourceFolder = FolderFactory.getFolderByPath(sourceFolderName + "/", sourceHost);
        String destinationHostName = getHostname(destinationPath);
        String destinationFolderName = getPath(destinationPath);
        Host destinationHost = HostFactory.getHostByHostName(destinationHostName);
        Folder destinationFolder = FolderFactory.getFolderByPath(destinationFolderName + "/", destinationHost);
        List<Permission> list = perAPI.getPermissions(destinationFolder);
        Iterator i = list.iterator();
        while (i.hasNext()) {
            Permission p = (Permission) i.next();
            perAPI.delete(p);
        }
        list = perAPI.getPermissions(sourceFolder);
        i = list.iterator();
        while (i.hasNext()) {
            Permission p = (Permission) i.next();
            Permission newPerm = new Permission();
            newPerm.setPermission(p.getPermission());
            newPerm.setRoleId(p.getRoleId());
            newPerm.setInode(destinationFolder.getInode());
            perAPI.save(newPerm);
        }
        return;
    }

    public void createResource(String resourceUri, boolean publish, User user) throws IOException {
        PermissionAPI perAPI = APILocator.getPermissionAPI();
        Logger.debug(this.getClass(), "createResource");
        resourceUri = stripMapping(resourceUri);
        String hostName = getHostname(resourceUri);
        String path = getPath(resourceUri);
        String folderName = getFolderName(path);
        String fileName = getFileName(path);
        fileName = deleteSpecialCharacter(fileName);
        if (fileName.startsWith(".")) {
            return;
        }
        Host host = HostFactory.getHostByHostName(hostName);
        Folder folder = FolderFactory.getFolderByPath(folderName, host);
        boolean hasPermission = perAPI.doesUserHavePermission(folder, PERMISSION_WRITE, user, false);
        if (hasPermission) {
            if (!checkFolderFilter(folder, fileName)) {
                throw new IOException("The file doesn't comply the folder's filter");
            }
            if (host.getInode() != 0 && folder.getInode() != 0) {
                File file = new File();
                file.setTitle(fileName);
                file.setFileName(fileName);
                file.setShowOnMenu(false);
                file.setLive(publish);
                file.setWorking(true);
                file.setDeleted(false);
                file.setLocked(false);
                file.setModDate(new Date());
                String mimeType = FileFactory.getMimeType(fileName);
                file.setMimeType(mimeType);
                String author = user.getFullName();
                file.setAuthor(author);
                file.setModUser(author);
                file.setSortOrder(0);
                file.setShowOnMenu(false);
                try {
                    Identifier identifier = null;
                    if (!isResource(resourceUri)) {
                        WebAssetFactory.createAsset(file, user.getUserId(), folder, publish);
                        identifier = IdentifierCache.getIdentifierFromIdentifierCache(file);
                    } else {
                        File actualFile = FileFactory.getFileByURI(path, host, false);
                        identifier = IdentifierCache.getIdentifierFromIdentifierCache(actualFile);
                        WebAssetFactory.createAsset(file, user.getUserId(), folder, identifier, false, false);
                        WebAssetFactory.publishAsset(file);
                        String assetsPath = FileFactory.getRealAssetsRootPath();
                        new java.io.File(assetsPath).mkdir();
                        java.io.File workingIOFile = FileFactory.getAssetIOFile(file);
                        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
                        vc.remove(ResourceManager.RESOURCE_TEMPLATE + workingIOFile.getPath());
                        if (file != null && file.getInode() > 0) {
                            byte[] currentData = new byte[0];
                            FileInputStream is = new FileInputStream(workingIOFile);
                            int size = is.available();
                            currentData = new byte[size];
                            is.read(currentData);
                            java.io.File newVersionFile = FileFactory.getAssetIOFile(file);
                            vc.remove(ResourceManager.RESOURCE_TEMPLATE + newVersionFile.getPath());
                            FileChannel channelTo = new FileOutputStream(newVersionFile).getChannel();
                            ByteBuffer currentDataBuffer = ByteBuffer.allocate(currentData.length);
                            currentDataBuffer.put(currentData);
                            currentDataBuffer.position(0);
                            channelTo.write(currentDataBuffer);
                            channelTo.force(false);
                            channelTo.close();
                        }
                        java.util.List<Tree> parentTrees = TreeFactory.getTreesByChild(file);
                        for (Tree tree : parentTrees) {
                            Tree newTree = TreeFactory.getTree(tree.getParent(), file.getInode());
                            if (newTree.getChild() == 0) {
                                newTree.setParent(tree.getParent());
                                newTree.setChild(file.getInode());
                                newTree.setRelationType(tree.getRelationType());
                                newTree.setTreeOrder(0);
                                TreeFactory.saveTree(newTree);
                            }
                        }
                    }
                    List<Permission> permissions = perAPI.getPermissions(folder);
                    for (Permission permission : permissions) {
                        Permission filePermission = new Permission();
                        filePermission.setPermission(permission.getPermission());
                        filePermission.setRoleId(permission.getRoleId());
                        filePermission.setInode(identifier.getInode());
                        perAPI.save(filePermission);
                    }
                } catch (Exception ex) {
                    Logger.debug(this, ex.toString());
                }
            }
        } else {
            throw new IOException("You don't have access to add that folder/host");
        }
    }

    public void setResourceContent(String resourceUri, InputStream content, String contentType, String characterEncoding, Date modifiedDate) throws IOException {
        resourceUri = stripMapping(resourceUri);
        Logger.debug(this.getClass(), "setResourceContent");
        String hostName = getHostname(resourceUri);
        String path = getPath(resourceUri);
        String folderName = getFolderName(path);
        String fileName = getFileName(path);
        fileName = deleteSpecialCharacter(fileName);
        Host host = HostFactory.getHostByHostName(hostName);
        Folder folder = FolderFactory.getFolderByPath(folderName, host);
        if (host.getInode() != 0 && folder.getInode() != 0) {
            File destinationFile = FileFactory.getFileByURI(path, host, false);
            java.io.File workingFile = FileFactory.getAssetIOFile(destinationFile);
            DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
            vc.remove(ResourceManager.RESOURCE_TEMPLATE + workingFile.getPath());
            InputStream is = content;
            ByteArrayOutputStream arrayWriter = new ByteArrayOutputStream();
            int read = -1;
            while ((read = is.read()) != -1) {
                arrayWriter.write(read);
            }
            byte[] currentData = arrayWriter.toByteArray();
            File file = FileFactory.getFileByURI(path, host, false);
            file.setSize(currentData.length);
            file.setModDate(modifiedDate);
            InodeFactory.saveInode(file);
            if (currentData != null) {
                FileChannel writeCurrentChannel = new FileOutputStream(workingFile).getChannel();
                writeCurrentChannel.truncate(0);
                ByteBuffer buffer = ByteBuffer.allocate(currentData.length);
                buffer.put(currentData);
                buffer.position(0);
                writeCurrentChannel.write(buffer);
                writeCurrentChannel.force(false);
                writeCurrentChannel.close();
                Logger.debug(this, "WEBDAV fileName:" + fileName + ":" + workingFile.getAbsolutePath());
                if (UtilMethods.isImage(fileName) && workingFile != null) {
                    try {
                        BufferedImage img = javax.imageio.ImageIO.read(workingFile);
                        if (img != null) {
                            int height = img.getHeight();
                            destinationFile.setHeight(height);
                            int width = img.getWidth();
                            destinationFile.setWidth(width);
                        }
                    } catch (Exception ioe) {
                        Logger.error(this.getClass(), ioe.getMessage(), ioe);
                    }
                }
                String folderPath = workingFile.getParentFile().getAbsolutePath();
                Identifier identifier = new Identifier();
                try {
                    identifier = IdentifierCache.getIdentifierFromIdentifierCache(destinationFile);
                } catch (DotHibernateException he) {
                    Logger.error(this, "Cannot load identifier : ", he);
                }
                java.io.File directory = new java.io.File(folderPath);
                java.io.File[] files = directory.listFiles((new FileFactory()).new ThumbnailsFileNamesFilter(identifier));
                for (java.io.File iofile : files) {
                    try {
                        iofile.delete();
                    } catch (SecurityException e) {
                        Logger.error(this, "EditFileAction._saveWorkingFileData(): " + iofile.getName() + " cannot be erased. Please check the file permissions.");
                    } catch (Exception e) {
                        Logger.error(this, "EditFileAction._saveWorkingFileData(): " + e.getMessage());
                    }
                }
            }
        }
    }

    public Folder createFolder(String folderUri, User user) throws IOException {
        Folder folder = null;
        folderUri = stripMapping(folderUri);
        PermissionAPI perAPI = APILocator.getPermissionAPI();
        Logger.debug(this.getClass(), "createFolder");
        String hostName = getHostname(folderUri);
        String path = getPath(folderUri);
        Host host = HostFactory.getHostByHostName(hostName);
        List<Permission> parentPermissions = new ArrayList<Permission>();
        boolean hasPermission = false;
        boolean validName = true;
        String parentPath = getFolderName(path);
        if (UtilMethods.isSet(parentPath) && !parentPath.equals("/")) {
            Folder parentFolder = FolderFactory.getFolderByPath(parentPath, host);
            hasPermission = perAPI.doesUserHavePermission(parentFolder, PERMISSION_WRITE, user, false);
        } else {
            if (host.getInode() > 0) {
                java.util.List<String> reservedFolderNames = new java.util.ArrayList<String>();
                String[] reservedFolderNamesArray = Config.getStringArrayProperty("RESERVEDFOLDERNAMES");
                for (String name : reservedFolderNamesArray) {
                    reservedFolderNames.add(name.toUpperCase());
                }
                validName = (!(reservedFolderNames.contains(path.substring(1).toUpperCase())));
            }
            hasPermission = perAPI.doesUserHavePermission(host, PERMISSION_WRITE, user, false);
        }
        if ((hasPermission) && (validName)) {
            if (host.getInode() != 0) {
                path = deleteSpecialCharacter(path);
                try {
                    folder = FolderFactory.createFolders(path, host);
                } catch (DotDataException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
        return folder;
    }

    public void move(String fromPath, String toPath, User user, boolean autoPublish) throws IOException {
        fromPath = stripMapping(fromPath);
        toPath = stripMapping(toPath);
        PermissionAPI perAPI = APILocator.getPermissionAPI();
        String hostName = getHostname(fromPath);
        Host host = HostFactory.getHostByHostName(hostName);
        String toParentPath = getFolderName(getPath(toPath));
        Folder toParentFolder = FolderFactory.getFolderByPath(toParentPath, host);
        if (isResource(fromPath)) {
            if (!perAPI.doesUserHavePermission(toParentFolder, PermissionAPI.PERMISSION_READ, user, false)) {
                throw new IOException("User doesn't have permissions to move file to folder");
            }
            if (toParentFolder == null || toParentFolder.getInode() < 1) {
                throw new IOException("Cannot move a file to the root of the host.");
            }
            File f = FileFactory.getFileByURI(getPath(fromPath), host, false);
            if (getFolderName(fromPath).equals(getFolderName(toPath))) {
                try {
                    String fileName = getFileName(toPath);
                    if (fileName.contains(".")) {
                        fileName = fileName.substring(0, fileName.lastIndexOf("."));
                    }
                    FileFactory.renameFile(f, fileName, user);
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            } else {
                FileFactory.moveFile(f, toParentFolder);
            }
            if (autoPublish) {
                try {
                    PublishFactory.publishAsset(f, user, false);
                } catch (Exception e) {
                    throw new IOException("Unable to publish file");
                }
            }
            FileCache.removeFile(f);
        } else {
            if (UtilMethods.isSet(toParentPath) && !toParentPath.equals("/")) {
                if (!perAPI.doesUserHavePermission(toParentFolder, PermissionAPI.PERMISSION_READ, user, false)) {
                    throw new IOException("User doesn't have permissions to move file to folder");
                }
                if (getFolderName(fromPath).equals(getFolderName(toPath))) {
                    Logger.debug(this, "Calling Folderfactory to rename " + fromPath + " to " + toPath);
                    try {
                        removeObject(toPath, user);
                        FolderCache.clearCache();
                        FileCache.clearCache();
                    } catch (Exception e) {
                        Logger.debug(this, "Unable to delete toPath " + toPath);
                    }
                    boolean renamed = FolderFactory.renameFolder(FolderFactory.getFolderByPath(getPath(fromPath), host), getFileName(toPath));
                    if (!renamed) {
                        Logger.error(this, "Unable to remame folder");
                        throw new IOException("Unable to rename folder");
                    }
                } else {
                    Logger.debug(this, "Calling folder factory to move from " + fromPath + " to " + toParentPath);
                    Folder fromFolder = FolderFactory.getFolderByPath(getPath(fromPath), host);
                    if (fromFolder != null) {
                        Logger.debug(this, "Calling folder factory to move from " + fromFolder.getPath() + " to " + toParentPath);
                        Logger.debug(this, "the from folder inode is " + fromFolder.getInode());
                    } else {
                        Logger.debug(this, "The from folder is null");
                    }
                    FolderFactory.moveFolder(fromFolder, toParentFolder);
                }
            } else {
                if (!perAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, false)) {
                    throw new IOException("User doesn't have permissions to move file to host");
                }
                if (getFolderName(fromPath).equals(getFolderName(toPath))) {
                    FolderFactory.renameFolder(FolderFactory.getFolderByPath(getPath(fromPath), host), getFileName(toPath));
                } else {
                    Folder fromFolder = FolderFactory.getFolderByPath(getPath(fromPath), host);
                    FolderFactory.moveFolder(fromFolder, host);
                }
            }
        }
        LiveCache.clearCache();
        WorkingCache.clearCache();
        FolderCache.clearCache();
        FileCache.clearCache();
        IdentifierCache.clearCache();
    }

    public void removeObject(String uri, User user) throws IOException {
        uri = stripMapping(uri);
        Logger.debug(this.getClass(), "In the removeObject Method");
        String hostName = getHostname(uri);
        String path = getPath(uri);
        String folderName = getFolderName(path);
        Host host = HostFactory.getHostByHostName(hostName);
        Folder folder = FolderFactory.getFolderByPath(folderName, host);
        if (isResource(uri)) {
            WebAsset webAsset;
            webAsset = FileFactory.getFileByURI(path, host, false);
            WebAssetFactory.deleteAsset(webAsset, user);
        } else if (isFolder(uri)) {
            if (!path.endsWith("/")) path += "/";
            folder = FolderFactory.getFolderByPath(path, host);
            if (folder.isShowOnMenu()) {
                RefreshMenus.deleteMenu(folder);
            }
            Set<Inode> objectsToRemove = new HashSet<Inode>();
            objectsToRemove.add(folder);
            EditFolderAction._deleteChildrenAssetsFromFolder(folder, objectsToRemove);
            List<Folder> subFolders = FolderFactory.getFoldersByParent(folder.getInode());
            for (Folder fold : subFolders) {
                String f = ("/" + hostName.replace("/", "") + fold.getPath());
                if (f.endsWith("/")) {
                    f = f.substring(0, f.length() - 1);
                }
                Logger.debug(this, "Recursing delete on " + f);
                removeObject(f, user);
            }
            InodeFactory.deleteInode(folder);
        }
        FolderCache.clearCache();
        FileCache.clearCache();
    }

    public LockToken lock(com.bradmcevoy.http.LockInfo lock, User user, String uniqueId) {
        LockToken lt = new LockToken();
        String lockTokenStr = lock.owner + "-" + uniqueId;
        String lockToken = md5Encoder.encode(md5Helper.digest(lockTokenStr.getBytes()));
        resourceLocks.put(lockToken, lock);
        lt.tokenId = lockToken;
        lt.info = lock;
        lt.timeout = new LockTimeout(new Long(45));
        return lt;
    }

    public LockToken refreshLock(String lockId) {
        com.bradmcevoy.http.LockInfo info = resourceLocks.get(lockId);
        if (info == null) return null;
        LockToken lt = new LockToken();
        lt.info = info;
        lt.timeout = new LockTimeout(new Long(45));
        lt.tokenId = lockId;
        return lt;
    }

    public void unlock(String lockId) {
        resourceLocks.remove(lockId);
    }

    private String getFileName(String uri) {
        int begin = uri.lastIndexOf("/") + 1;
        int end = uri.length();
        String fileName = uri.substring(begin, end);
        return fileName;
    }

    private String getFolderName(String uri) {
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        int begin = 0;
        int end = uri.lastIndexOf("/") + 1;
        String folderName = uri.substring(begin, end);
        return folderName;
    }

    private String getHostname(String uri) {
        if (uri == null || uri.equals("")) {
            return "/";
        }
        int begin = 1;
        int end = (uri.indexOf("/", 1) != -1 ? uri.indexOf("/", 1) : uri.length());
        uri = uri.substring(begin, end);
        return uri;
    }

    private String getPath(String uri) {
        int begin = (uri.indexOf("/", 1) != -1 ? uri.indexOf("/", 1) : uri.length());
        int end = uri.length();
        uri = uri.substring(begin, end);
        return uri;
    }

    private String stripMapping(String uri) {
        String r = uri;
        if (r.startsWith("/webdav")) {
            r = r.substring(7, r.length());
        }
        if (r.startsWith("/nonpub")) {
            r = r.substring(7, r.length());
        }
        if (r.startsWith("/autopub")) {
            r = r.substring(8, r.length());
        }
        return r;
    }

    private String deleteSpecialCharacter(String fileName) throws IOException {
        if (UtilMethods.isSet(fileName)) {
            fileName = fileName.replace("\\", "");
            fileName = fileName.replace(":", "");
            fileName = fileName.replace("*", "");
            fileName = fileName.replace("?", "");
            fileName = fileName.replace("\"", "");
            fileName = fileName.replace("<", "");
            fileName = fileName.replace(">", "");
            fileName = fileName.replace("|", "");
            if (!UtilMethods.isSet(fileName)) {
                throw new IOException("Please specify a name wothout special characters \\/:*?\"<>|");
            }
        }
        return fileName;
    }

    private boolean checkFolderFilter(Folder folder, String fileName) {
        boolean returnValue = false;
        returnValue = FolderFactory.matchFilter(folder, fileName);
        return returnValue;
    }

    private Summary[] getChildrenData(String folderUriAux, User user) throws IOException {
        PermissionAPI perAPI = APILocator.getPermissionAPI();
        Logger.debug(this.getClass(), "getChildrenNames");
        folderUriAux = stripMapping(folderUriAux);
        ArrayList<Summary> returnValue = new ArrayList<Summary>();
        try {
            if (folderUriAux.equals("") || folderUriAux.equals("/")) {
                List<Host> hosts = HostFactory.getAllHosts();
                hosts = HostFactory.getAllHostsByUser(user);
                for (Host host : hosts) {
                    Summary s = new Summary();
                    s.setName(host.getHostname());
                    s.setPath("/" + s.getName());
                    s.setFolder(true);
                    s.setCreateDate(host.getiDate());
                    s.setModifyDate(host.getModDate());
                    s.setHost(host);
                    returnValue.add(s);
                }
            } else {
                String hostName = getHostname(folderUriAux);
                Host host = HostFactory.getHostByHostName(hostName);
                String path = getPath(folderUriAux);
                if (path.equals("") || path.equals("/")) {
                    List<Folder> folders = (List<Folder>) InodeFactory.getChildrenClass(host, Folder.class);
                    for (Folder folderAux : folders) {
                        if (perAPI.doesUserHavePermission(folderAux, PERMISSION_READ, user, false)) {
                            Summary s = new Summary();
                            s.setName(folderAux.getName());
                            s.setPath("/" + host.getHostname() + folderAux.getPath());
                            s.setPath(s.getPath().substring(0, s.getPath().length() - 1));
                            s.setFolder(true);
                            s.setCreateDate(folderAux.getIDate());
                            s.setCreateDate(folderAux.getModDate());
                            s.setHost(host);
                            returnValue.add(s);
                        }
                    }
                } else {
                    path += "/";
                    Folder folder = FolderFactory.getFolderByPath(path, host);
                    if (folder.getInode() != 0) {
                        List<Folder> folders = new ArrayList<Folder>();
                        List<File> files = new ArrayList<File>();
                        try {
                            String dbTrue = com.dotmarketing.db.DbConnectionFactory.getDBTrue();
                            String dbFalse = com.dotmarketing.db.DbConnectionFactory.getDBFalse();
                            String conditionAsset = " working = " + dbTrue + " and deleted = " + dbFalse + "";
                            folders = (ArrayList<Folder>) InodeFactory.getChildrenClass(folder, Folder.class);
                            files = (ArrayList<File>) InodeFactory.getChildrenClassByCondition(folder, File.class, conditionAsset);
                        } catch (Exception ex) {
                            String message = ex.getMessage();
                            Logger.debug(this, ex.toString());
                        }
                        for (Folder folderAux : folders) {
                            if (perAPI.doesUserHavePermission(folderAux, PERMISSION_READ, user, false)) {
                                Summary s = new Summary();
                                s.setFolder(true);
                                s.setCreateDate(folderAux.getiDate());
                                s.setModifyDate(folderAux.getModDate());
                                s.setName(folderAux.getName());
                                s.setPath("/" + host.getHostname() + folderAux.getPath());
                                s.setPath(s.getPath().substring(0, s.getPath().length() - 1));
                                s.setHost(host);
                                returnValue.add(s);
                            }
                        }
                        for (File file : files) {
                            if (perAPI.doesUserHavePermission(file, PERMISSION_READ, user, false)) {
                                String fileUri = file.getURI();
                                int begin = fileUri.lastIndexOf("/") + 1;
                                int end = fileUri.length();
                                fileUri = fileUri.substring(begin, end);
                                Summary s = new Summary();
                                s.setFolder(false);
                                s.setName(fileUri);
                                s.setPath(s.getName());
                                s.setPath(folderUriAux + "/" + fileUri);
                                s.setCreateDate(file.getiDate());
                                s.setModifyDate(file.getModDate());
                                java.io.File workingFile = FileFactory.getAssetIOFile(file);
                                FileInputStream is = new FileInputStream(workingFile);
                                s.setLength(is.available());
                                s.setHost(host);
                                s.setFile(file);
                                returnValue.add(s);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            String exception = ex.getMessage();
            Logger.debug(this, ex.toString());
        } finally {
            return returnValue.toArray(new Summary[returnValue.size()]);
        }
    }

    private InputStream getResourceContent(String resourceUri) throws IOException {
        resourceUri = stripMapping(resourceUri);
        Logger.debug(this.getClass(), "getResourceContent");
        InputStream returnValue = null;
        String hostName = getHostname(resourceUri);
        String path = getPath(resourceUri);
        String folderName = getFolderName(path);
        Host host = HostFactory.getHostByHostName(hostName);
        Folder folder = FolderFactory.getFolderByPath(folderName, host);
        if (host.getInode() != 0 && folder.getInode() != 0) {
            File file = FileFactory.getFileByURI(path, host, false);
            java.io.File workingFile = FileFactory.getAssetIOFile(file);
            FileInputStream is = new FileInputStream(workingFile);
            returnValue = is;
        }
        return returnValue;
    }

    private void setResourceContent(String resourceUri, InputStream content, String contentType, String characterEncoding) throws IOException {
        setResourceContent(resourceUri, content, contentType, characterEncoding, Calendar.getInstance().getTime());
    }
}
