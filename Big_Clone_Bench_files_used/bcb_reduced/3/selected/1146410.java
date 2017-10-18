package org.dasein.cloud.jclouds.rackspace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Callable;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.encryption.Encryption;
import org.dasein.cloud.encryption.EncryptionException;
import org.dasein.cloud.services.storage.CloudStoreObject;
import org.dasein.cloud.services.storage.FileTransfer;
import org.dasein.cloud.services.storage.StorageServices;
import org.dasein.util.Jiterator;
import org.dasein.util.JiteratorPopulator;
import org.dasein.util.PopulatorThread;
import org.dasein.util.Retry;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.io.Payloads;
import org.jclouds.rackspace.cloudfiles.CloudFilesAsyncClient;
import org.jclouds.rackspace.cloudfiles.CloudFilesClient;
import org.jclouds.rackspace.cloudfiles.domain.CFObject;
import org.jclouds.rackspace.cloudfiles.domain.ContainerMetadata;
import org.jclouds.rackspace.cloudfiles.domain.MutableObjectInfoWithMetadata;
import org.jclouds.rackspace.cloudfiles.domain.ObjectInfo;
import org.jclouds.rackspace.cloudfiles.options.ListContainerOptions;
import org.jclouds.rest.RestContext;

public class RackspaceStorage implements StorageServices {

    private static final Logger logger = Logger.getLogger(RackspaceStorage.class);

    private static String verifyName(String name) throws CloudException {
        if (name == null) {
            return null;
        }
        StringBuilder str = new StringBuilder();
        name = name.toLowerCase().trim();
        if (name.length() > 255) {
            String extra = name.substring(255);
            int idx = extra.indexOf(".");
            if (idx > -1) {
                throw new CloudException("S3 names are limited to 255 characters.");
            }
            name = name.substring(0, 255);
        }
        while (name.indexOf("--") != -1) {
            name = name.replaceAll("--", "-");
        }
        while (name.indexOf("..") != -1) {
            name = name.replaceAll("\\.\\.", ".");
        }
        while (name.indexOf(".-") != -1) {
            name = name.replaceAll("\\.-", ".");
        }
        while (name.indexOf("-.") != -1) {
            name = name.replaceAll("-\\.", ".");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                str.append(c);
            } else {
                if (i > 0) {
                    if (c == '/') {
                        c = '.';
                    } else if (c != '.' && c != '-') {
                        c = '-';
                    }
                    str.append(c);
                }
            }
        }
        name = str.toString();
        while (name.indexOf("..") != -1) {
            name = name.replaceAll("\\.\\.", ".");
        }
        if (name.length() < 1) {
            return "000";
        }
        while (name.charAt(name.length() - 1) == '-' || name.charAt(name.length() - 1) == '.') {
            name = name.substring(0, name.length() - 1);
            if (name.length() < 1) {
                return "000";
            }
        }
        if (name.length() < 1) {
            return "000";
        } else if (name.length() == 1) {
            name = name + "00";
        } else if (name.length() == 2) {
            name = name + "0";
        }
        return name;
    }

    private RackspaceCloud provider = null;

    public RackspaceStorage(RackspaceCloud provider) {
        this.provider = provider;
    }

    public void clear(String directoryName) throws CloudException, InternalException {
        logger.debug("enter - clear(String)");
        try {
            for (CloudStoreObject item : listFiles(directoryName)) {
                if (item.isContainer()) {
                    clear(item.getDirectory() + "." + item.getName());
                } else {
                    removeFile(directoryName, item.getName(), false);
                }
            }
            RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
            try {
                if (!getClient(ctx).deleteContainerIfEmpty(directoryName)) {
                    throw new CloudException("Could not remove container, possibly some straggling files.");
                }
            } finally {
                ctx.close();
            }
        } finally {
            logger.debug("exit - clear(String)");
        }
    }

    public CloudStoreObject copy(CloudStoreObject file, CloudStoreObject toDirectory, String copyName) throws InternalException, CloudException {
        if (file.isContainer()) {
            CloudStoreObject directory = new CloudStoreObject();
            String pathName;
            int idx;
            directory.setContainer(true);
            directory.setCreationDate(new Date());
            directory.setSize(0);
            if (file.getDirectory() != null) {
                pathName = createDirectory(file.getDirectory() + "." + copyName, true);
            } else {
                pathName = createDirectory(copyName, true);
            }
            idx = pathName.lastIndexOf('.');
            String tmp = pathName;
            while (idx > -1 && idx == tmp.length() - 1) {
                tmp = tmp.substring(0, idx);
                idx = tmp.lastIndexOf('.');
            }
            if (idx == -1) {
                directory.setDirectory(null);
                directory.setName(pathName);
            } else {
                directory.setDirectory(pathName.substring(0, idx));
                directory.setName(pathName.substring(idx + 1));
            }
            for (CloudStoreObject f : listFiles(file.getDirectory())) {
                copy(f, directory, f.getName());
            }
            return directory;
        } else {
            return copyFile(file, toDirectory, copyName);
        }
    }

    private void copy(InputStream input, OutputStream output, FileTransfer xfer) throws IOException {
        try {
            byte[] bytes = new byte[10240];
            long total = 0L;
            int count;
            if (xfer != null) {
                xfer.setBytesTransferred(0L);
            }
            while ((count = input.read(bytes, 0, 10240)) != -1) {
                if (count > 0) {
                    output.write(bytes, 0, count);
                    total = total + count;
                    if (xfer != null) {
                        xfer.setBytesTransferred(total);
                    }
                }
            }
            output.flush();
        } finally {
            input.close();
            output.close();
        }
    }

    private CloudStoreObject copyFile(CloudStoreObject file, CloudStoreObject toDirectory, String newName) throws InternalException, CloudException {
        CloudStoreObject replacement;
        File tmp;
        newName = verifyName(newName);
        try {
            tmp = File.createTempFile("file", ".tmp");
        } catch (IOException e) {
            logger.error("Could not create temp file: " + e.getMessage());
            e.printStackTrace();
            throw new InternalException(e);
        }
        try {
            get(file.getDirectory(), file.getName(), tmp, null);
            put(toDirectory.getDirectory() + "." + toDirectory.getName(), newName, tmp);
        } catch (IOException e) {
            logger.error("Failed to copy file: " + e.getMessage());
            e.printStackTrace();
            throw new InternalException(e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Encryption configuration error: " + e.getMessage());
            e.printStackTrace();
            throw new InternalException(e);
        } finally {
            tmp.delete();
        }
        replacement = new CloudStoreObject();
        replacement.setContainer(false);
        replacement.setCreationDate(new Date());
        replacement.setDirectory(toDirectory.getDirectory() + "." + toDirectory.getName());
        replacement.setLocation(replacement.getDirectory() + "/" + newName);
        replacement.setName(newName);
        replacement.setProviderRegionId(provider.getContext().getRegionId());
        replacement.setSize(file.getSize());
        return replacement;
    }

    public String createDirectory(String directoryName, boolean findFreeName) throws InternalException, CloudException {
        logger.debug("enter - createDirectory(String, boolean)");
        try {
            int idx;
            directoryName = verifyName(directoryName);
            idx = directoryName.lastIndexOf(".");
            if (idx > -1) {
                String dir = directoryName.substring(0, idx);
                directoryName = createDirectory(dir, true) + "." + directoryName.substring(idx + 1);
            }
            if (createDirectory(directoryName)) {
                return directoryName;
            }
            throw new CloudException("Failed to create directory: " + directoryName);
        } finally {
            logger.debug("exit - createDirectory(String, boolean)");
        }
    }

    public FileTransfer download(CloudStoreObject cloudFile, File diskFile) throws CloudException, InternalException {
        logger.debug("enter - download(CloudStoreObject, File)");
        try {
            final FileTransfer transfer = new FileTransfer();
            final CloudStoreObject source = cloudFile;
            final File target = diskFile;
            transfer.setBytesToTransfer(exists(cloudFile.getDirectory(), cloudFile.getName(), false));
            if (transfer.getBytesToTransfer() == -1L) {
                throw new CloudException("No such file: " + cloudFile.getDirectory() + "." + cloudFile.getName());
            }
            Thread t = new Thread() {

                public void run() {
                    Callable<Object> operation = new Callable<Object>() {

                        public Object call() throws Exception {
                            boolean success = false;
                            try {
                                get(source.getDirectory(), source.getName(), target, transfer);
                                success = true;
                                return null;
                            } finally {
                                if (!success) {
                                    if (target.exists()) {
                                        target.delete();
                                    }
                                }
                            }
                        }
                    };
                    try {
                        (new Retry<Object>()).retry(5, operation);
                        transfer.complete(null);
                    } catch (CloudException e) {
                        transfer.complete(e);
                    } catch (InternalException e) {
                        transfer.complete(e);
                    } catch (Throwable t) {
                        logger.error(t);
                        t.printStackTrace();
                        transfer.complete(t);
                    }
                }
            };
            t.setDaemon(true);
            t.start();
            return transfer;
        } finally {
            logger.debug("exit - download(CloudStoreObject, File)");
        }
    }

    public FileTransfer download(String directory, String fileName, File toFile, Encryption encryption) throws InternalException, CloudException {
        logger.debug("enter - download(String, String, File, Encryption)");
        try {
            final FileTransfer transfer = new FileTransfer();
            final Encryption enc = encryption;
            final String dname = directory;
            final String fname = fileName;
            final File target = toFile;
            Thread t = new Thread() {

                public void run() {
                    try {
                        Callable<Object> operation = new Callable<Object>() {

                            public Object call() throws Exception {
                                boolean success = false;
                                try {
                                    downloadMultipartFile(dname, fname, target, transfer, enc);
                                    success = true;
                                    return null;
                                } finally {
                                    if (!success) {
                                        if (target.exists()) {
                                            target.delete();
                                        }
                                    }
                                }
                            }
                        };
                        try {
                            (new Retry<Object>()).retry(5, operation);
                            transfer.complete(null);
                        } catch (CloudException e) {
                            transfer.complete(e);
                        } catch (InternalException e) {
                            transfer.complete(e);
                        } catch (Throwable t) {
                            logger.error(t);
                            t.printStackTrace();
                            transfer.complete(t);
                        }
                    } finally {
                        if (enc != null) {
                            enc.clear();
                        }
                    }
                }
            };
            t.setDaemon(true);
            t.start();
            return transfer;
        } finally {
            logger.debug("exit - download(String, String, File, Encryption)");
        }
    }

    public boolean exists(String directory) throws InternalException, CloudException {
        logger.debug("enter - exists(String)");
        try {
            return (exists(directory, null, false) != -1L);
        } finally {
            logger.debug("exit - exists(String)");
        }
    }

    public long exists(String directory, String object, boolean multiPart) throws InternalException, CloudException {
        logger.debug("enter - exists(String, String, boolean)");
        try {
            if (object == null) {
                RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
                return (ctx.getApi().containerExists(directory) ? 0 : -1L);
            } else if (!multiPart) {
                MutableObjectInfoWithMetadata md;
                try {
                    RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
                    try {
                        md = getClient(ctx).getObjectInfo(directory, object);
                    } finally {
                        ctx.close();
                    }
                } catch (RuntimeException e) {
                    logger.error("Could not retrieve file info for " + directory + "." + object + ": " + e.getMessage());
                    e.printStackTrace();
                    throw new CloudException(e);
                }
                if (md == null) {
                    return -1;
                }
                return md.getBytes();
            } else {
                if (exists(directory, object + ".properties", false) == -1L) {
                    return -1L;
                }
                Properties properties = new Properties();
                String str;
                try {
                    RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
                    try {
                        CFObject propsFile = getClient(ctx).getObject(directory, object + ".properties");
                        if (propsFile == null) {
                            throw new CloudException("File was modified while we were reading it.");
                        }
                        try {
                            properties.load(propsFile.getPayload().getInput());
                        } catch (IOException e) {
                            logger.error("IO error loading file data for " + directory + "." + object + ": " + e.getMessage());
                            e.printStackTrace();
                            throw new InternalException(e);
                        }
                    } finally {
                        ctx.close();
                    }
                } catch (RuntimeException e) {
                    logger.error("Could not retrieve file info for " + directory + "." + object + ": " + e.getMessage());
                    e.printStackTrace();
                    throw new CloudException(e);
                }
                str = properties.getProperty("length");
                if (str == null) {
                    return 0L;
                } else {
                    return Long.parseLong(str);
                }
            }
        } finally {
            logger.debug("exit - exists(String, String, boolean)");
        }
    }

    private CloudFilesClient getClient(RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx) {
        return ctx.getApi();
    }

    public long getMaxFileSizeInBytes() {
        return 5000000000L;
    }

    public String getProviderTermForDirectory(Locale locale) {
        return "container";
    }

    public String getProviderTermForFile(Locale locale) {
        return "object";
    }

    public Iterable<CloudStoreObject> listFiles(String parentDirectory) throws CloudException, InternalException {
        logger.debug("enter - listFiles(String)");
        try {
            PopulatorThread<CloudStoreObject> populator;
            final String dir = parentDirectory;
            populator = new PopulatorThread<CloudStoreObject>(new JiteratorPopulator<CloudStoreObject>() {

                public void populate(Jiterator<CloudStoreObject> iterator) throws CloudException, InternalException {
                    listFiles(dir, iterator);
                }
            });
            populator.populate();
            return populator.getResult();
        } finally {
            logger.debug("exit - listFiles(String)");
        }
    }

    private void listFiles(String bucket, Jiterator<CloudStoreObject> iterator) throws CloudException, InternalException {
        loadDirectories(bucket, iterator);
        if (bucket != null) {
            loadFiles(bucket, iterator);
        }
    }

    private void loadDirectories(String directory, Jiterator<CloudStoreObject> iterator) throws CloudException, InternalException {
        Collection<ContainerMetadata> containers;
        try {
            RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
            try {
                if (directory != null) {
                    containers = getClient(ctx).listContainers(ListContainerOptions.Builder.withPrefix(directory + "."));
                } else {
                    containers = getClient(ctx).listContainers();
                }
            } finally {
                ctx.close();
            }
        } catch (RuntimeException e) {
            logger.error("Could not load directories in " + directory + ": " + e.getMessage());
            e.printStackTrace();
            throw new CloudException(e);
        }
        for (ContainerMetadata container : containers) {
            String name = container.getName();
            if (directory == null) {
                if (name.indexOf(".") > -1) {
                    continue;
                }
            } else if (name.equals(directory)) {
                continue;
            } else if (!name.startsWith(directory + ".")) {
                continue;
            }
            if (directory != null) {
                String tmp = name.substring(directory.length() + 1);
                int idx = tmp.indexOf(".");
                if (idx > 0 && idx < (tmp.length() - 1)) {
                    continue;
                }
            }
            CloudStoreObject file = new CloudStoreObject();
            String[] parts = name.split("\\.");
            if (parts == null || parts.length < 2) {
                file.setName(name);
                file.setDirectory(null);
            } else {
                StringBuilder dirName = new StringBuilder();
                file.setName(parts[parts.length - 1]);
                for (int part = 0; part < parts.length - 1; part++) {
                    if (dirName.length() > 0) {
                        dirName.append(".");
                    }
                    dirName.append(parts[part]);
                }
                file.setDirectory(dirName.toString());
            }
            file.setContainer(true);
            file.setProviderRegionId(provider.getContext().getRegionId());
            file.setSize(0L);
            file.setCreationDate(new Date());
            iterator.push(file);
        }
    }

    private void loadFiles(String directory, Jiterator<CloudStoreObject> iterator) throws CloudException, InternalException {
        PageSet<ObjectInfo> files;
        try {
            RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
            try {
                files = getClient(ctx).listObjects(directory);
            } finally {
                ctx.close();
            }
        } catch (RuntimeException e) {
            logger.error("Could not list files in " + directory + ": " + e.getMessage());
            e.printStackTrace();
            throw new CloudException(e);
        }
        for (ObjectInfo info : files) {
            CloudStoreObject file = new CloudStoreObject();
            file.setContainer(false);
            file.setDirectory(directory);
            file.setName(info.getName());
            file.setLocation(directory + "/" + info.getName());
            file.setProviderRegionId(provider.getContext().getRegionId());
            file.setSize(info.getBytes().longValue());
            file.setCreationDate(info.getLastModified());
            iterator.push(file);
        }
    }

    public void makePublic(String fileName) throws InternalException, CloudException {
        throw new OperationNotSupportedException();
    }

    public void makePublic(String directory, String fileName) throws InternalException, CloudException {
        throw new OperationNotSupportedException();
    }

    public void moveFile(String fromDirectory, String fileName, String targetRegionId, String toDirectory) throws InternalException, CloudException {
        logger.debug("enter - moveFile(String, String, String, String)");
        try {
            moveFile(fromDirectory, fileName, toDirectory);
        } finally {
            logger.debug("exit - moveFile(String, String, String, String)");
        }
    }

    @Override
    public void moveFile(String sourceDirectory, String object, String toDirectory) throws InternalException, CloudException {
        logger.debug("enter - moveFile(String, String, String)");
        try {
            CloudStoreObject directory = new CloudStoreObject();
            CloudStoreObject file = new CloudStoreObject();
            String[] parts = toDirectory.split("\\.");
            String dirPath, dirName;
            if (parts == null || parts.length < 2) {
                dirPath = null;
                dirName = toDirectory;
            } else {
                StringBuilder str = new StringBuilder();
                dirName = parts[parts.length - 1];
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) {
                        str.append(".");
                    }
                    str.append(parts[i]);
                }
                dirPath = str.toString();
            }
            file.setContainer(false);
            file.setDirectory(sourceDirectory);
            file.setName(object);
            file.setProviderRegionId(provider.getContext().getRegionId());
            directory.setContainer(true);
            directory.setDirectory(dirPath);
            directory.setName(dirName);
            directory.setProviderRegionId(provider.getContext().getRegionId());
            copy(file, directory, object);
            removeFile(sourceDirectory, object, false);
        } finally {
            logger.debug("exit - moveFile(String, String, String)");
        }
    }

    public void removeDirectory(String directory) throws CloudException, InternalException {
        logger.debug("enter - removeDirectory(String)");
        try {
            RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
            try {
                if (!getClient(ctx).deleteContainerIfEmpty(directory)) {
                    throw new CloudException("Failed to remove directory " + directory);
                }
            } finally {
                ctx.close();
            }
        } finally {
            logger.debug("exit - removeDirectory(String)");
        }
    }

    public void removeFile(String directory, String name, boolean multipartFile) throws CloudException, InternalException {
        logger.debug("enter - removeFile(String, String, boolean)");
        try {
            if (!multipartFile) {
                removeFile(directory, name);
            } else {
                removeMultipart(directory, name);
            }
        } finally {
            logger.debug("exit - removeFile(String, String, boolean)");
        }
    }

    private void removeFile(String directory, String name) throws CloudException, InternalException {
        logger.debug("enter - removeFile(String, String)");
        try {
            try {
                RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
                try {
                    getClient(ctx).removeObject(directory, name);
                } finally {
                    ctx.close();
                }
            } catch (RuntimeException e) {
                logger.error("Failed to remove file " + directory + "." + name + ": " + e.getMessage());
                e.printStackTrace();
                throw new CloudException(e);
            }
        } finally {
            logger.debug("exit - removeFile(String,String)");
        }
    }

    private void removeMultipart(String bucket, String object) throws InternalException, CloudException {
        try {
            File propsFile = File.createTempFile("props", ".properties");
            Properties props = new Properties();
            String str;
            int parts;
            get(bucket, object + ".properties", propsFile, null);
            props.load(new FileInputStream(propsFile));
            str = props.getProperty("parts");
            parts = (str == null ? 1 : Integer.parseInt(str));
            removeFile(bucket, object + ".properties");
            for (int i = 1; i <= parts; i++) {
                removeFile(bucket, object + ".part." + i);
            }
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
            throw new InternalException(e);
        }
    }

    public String renameDirectory(String oldName, String newName, boolean findFreeName) throws CloudException, InternalException {
        logger.debug("enter - renameDirectory(String, String, boolean)");
        try {
            String nd = createDirectory(newName, findFreeName);
            for (CloudStoreObject f : listFiles(oldName)) {
                moveFile(oldName, f.getName(), null, nd);
            }
            removeDirectory(oldName);
            return nd;
        } finally {
            logger.debug("exit - renameDirectory(String, String, boolean)");
        }
    }

    public void renameFile(String directory, String oldName, String newName) throws CloudException, InternalException {
        File tmp = null;
        try {
            tmp = File.createTempFile(newName, "tmp");
            get(directory, oldName, tmp, null);
            upload(tmp, directory, newName, false, null);
            removeFile(directory, oldName, false);
        } catch (CloudException e) {
            logger.error(e);
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
            throw new CloudException(e);
        } catch (RuntimeException e) {
            logger.error(e);
            e.printStackTrace();
            throw new InternalException(e);
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
    }

    public void upload(File source, String directory, String fileName, boolean multipart, Encryption encryption) throws CloudException, InternalException {
        try {
            if (multipart) {
                try {
                    uploadMultipartFile(source, directory, fileName, encryption);
                } catch (InterruptedException e) {
                    logger.error(e);
                    e.printStackTrace();
                    throw new CloudException(e.getMessage());
                }
            } else {
                try {
                    put(directory, fileName, source);
                } catch (NoSuchAlgorithmException e) {
                    logger.error(e);
                    e.printStackTrace();
                    throw new InternalException(e);
                } catch (IOException e) {
                    logger.error(e);
                    e.printStackTrace();
                    throw new CloudException(e.getMessage());
                }
            }
        } finally {
            if (encryption != null) {
                encryption.clear();
            }
        }
    }

    private byte[] computeMD5Hash(InputStream is) throws NoSuchAlgorithmException, IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[16384];
            int bytesRead = -1;
            while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            return messageDigest.digest();
        } finally {
            try {
                bis.close();
            } catch (Exception e) {
                System.err.println("Unable to close input stream of hash candidate: " + e);
            }
        }
    }

    /**
	 * Creates the provided <code>path</code> in Rackspace CloudFiles.  If the <code>path</code>
	 * is not the root container, it will add an empty placeholder file.
	 * 
	 * @param path The container/object to create.
	 * @return Whether the <code>path</code> was created or already existed.
	 */
    private boolean createDirectory(String path) throws InternalException, CloudException {
        logger.debug("enter - createDirectory(String)");
        try {
            try {
                RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
                try {
                    return getClient(ctx).createContainer(path);
                } finally {
                    ctx.close();
                }
            } catch (RuntimeException e) {
                logger.error("Could not create directory: " + e.getMessage());
                e.printStackTrace();
                throw new CloudException(e);
            }
        } finally {
            logger.debug("exit - createDirectory(String)");
        }
    }

    private void downloadMultipartFile(String directory, String fileName, File restoreFile, FileTransfer transfer, Encryption encryption) throws CloudException, InternalException {
        try {
            File f;
            String str;
            int parts;
            if (restoreFile.exists()) {
                if (!restoreFile.delete()) {
                    throw new InternalException("Unable to delete restore file: " + restoreFile.getAbsolutePath());
                }
            }
            f = File.createTempFile("download", ".dl");
            f.deleteOnExit();
            Properties props = new Properties();
            try {
                get(directory, fileName + ".properties", f, transfer);
                props.load(new FileInputStream(f));
            } finally {
                f.delete();
            }
            try {
                boolean newEncryption;
                str = props.getProperty("encrypted");
                newEncryption = (str != null && str.equalsIgnoreCase("true"));
                if (newEncryption) {
                    str = props.getProperty("encryptionVersion");
                    if (str != null) {
                        try {
                            encryption = (Encryption) Class.forName(str).newInstance();
                        } catch (Exception e) {
                            if (encryption == null) {
                                throw new CloudException("Encryption mismatch: " + str);
                            }
                        }
                    }
                }
                str = props.getProperty("parts");
                parts = (str == null ? 1 : Integer.parseInt(str));
                String checksum = props.getProperty("checksum");
                File encFile = null;
                if (encryption != null) {
                    encFile = new File(restoreFile.getAbsolutePath() + ".enc");
                    if (encFile.exists()) {
                        encFile.delete();
                    }
                }
                for (int i = 1; i <= parts; i++) {
                    FileOutputStream out;
                    FileInputStream in;
                    if (f.exists()) {
                        f.delete();
                    }
                    f = File.createTempFile("part", "." + i);
                    get(directory, fileName + ".part." + i, f, transfer);
                    in = new FileInputStream(f);
                    if (encryption != null) {
                        out = new FileOutputStream(encFile, true);
                    } else {
                        out = new FileOutputStream(restoreFile, true);
                    }
                    copy(in, out, transfer);
                }
                if (encryption != null) {
                    try {
                        try {
                            try {
                                if (!getChecksum(encFile).equals(checksum)) {
                                    throw new IOException("Checksum mismatch.");
                                }
                            } catch (NoSuchAlgorithmException e) {
                                logger.error(e);
                                e.printStackTrace();
                                throw new InternalException(e.getMessage());
                            }
                            encryption.decrypt(new FileInputStream(encFile), new FileOutputStream(restoreFile));
                        } finally {
                            if (encFile.exists()) {
                                encFile.delete();
                            }
                        }
                    } catch (EncryptionException e) {
                        logger.error(e);
                        e.printStackTrace();
                        throw new InternalException(e);
                    }
                } else {
                    try {
                        if (!getChecksum(restoreFile).equals(checksum)) {
                            throw new IOException("Checksum mismatch.");
                        }
                    } catch (NoSuchAlgorithmException e) {
                        logger.error(e);
                        e.printStackTrace();
                        throw new InternalException(e.getMessage());
                    }
                }
            } finally {
                if (f != null && f.exists()) {
                    f.delete();
                }
            }
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
            throw new InternalException(e);
        }
    }

    private String getChecksum(File file) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        return toBase64(computeMD5Hash(new FileInputStream(file)));
    }

    private void get(String directory, String location, File toFile, FileTransfer transfer) throws IOException, CloudException {
        logger.debug("enter - get(String, String, File, FileTransfer)");
        try {
            if (toFile.exists()) {
                if (!toFile.delete()) {
                    throw new IOException("File already exists that cannot be overwritten.");
                }
            }
            RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
            try {
                CFObject object = getClient(ctx).getObject(directory, location);
                if (object == null) {
                    throw new IOException("No such file: " + directory + "/" + location);
                }
                copy(object.getPayload().getInput(), new FileOutputStream(toFile), transfer);
            } finally {
                ctx.close();
            }
        } finally {
            logger.debug("exit - get(String, String, File, FileTransfer)");
        }
    }

    private void put(String directory, String fileName, File file) throws NoSuchAlgorithmException, IOException, CloudException {
        logger.debug("enter - put(String, String, File)");
        try {
            RestContext<CloudFilesClient, CloudFilesAsyncClient> ctx = provider.getCloudFilesContext();
            try {
                CloudFilesClient client = getClient(ctx);
                CFObject object = client.newCFObject();
                object.getPayload().getContentMetadata().setContentLength(file.length());
                object.getInfo().setName(fileName);
                object.setPayload(file);
                Payloads.calculateMD5(object.getPayload());
                object.getInfo().setContentType("application/octet-stream");
                client.putObject(directory, object);
            } finally {
                ctx.close();
            }
        } finally {
            logger.debug("exit - put(String, String, File)");
        }
    }

    private void put(String directory, String fileName, String content) throws NoSuchAlgorithmException, IOException, CloudException {
        logger.debug("enter - put(String, String, String)");
        try {
            File tmp = File.createTempFile(fileName, ".txt");
            PrintWriter writer;
            try {
                writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tmp)));
                writer.print(content);
                writer.flush();
                writer.close();
                put(directory, fileName, tmp);
            } finally {
                tmp.delete();
            }
        } finally {
            logger.debug("exit - put(String, String, String)");
        }
    }

    private String toBase64(byte[] data) {
        byte[] b64 = Base64.encodeBase64(data);
        return new String(b64);
    }

    private void uploadMultipartFile(File sourceFile, String directory, String fileName, Encryption encryption) throws InterruptedException, InternalException, CloudException {
        logger.debug("enter - uploadMultipatFile(File, String, String, Encryption)");
        try {
            String checksum;
            File toUpload;
            if (encryption == null) {
                toUpload = sourceFile;
            } else {
                try {
                    File encryptedFile = File.createTempFile(sourceFile.getName(), ".enc");
                    FileInputStream input = new FileInputStream(sourceFile);
                    FileOutputStream output;
                    encryptedFile.deleteOnExit();
                    output = new FileOutputStream(encryptedFile);
                    encryption.encrypt(input, output);
                    input.close();
                    output.flush();
                    output.close();
                    toUpload = encryptedFile;
                } catch (EncryptionException e) {
                    logger.error(e);
                    e.printStackTrace();
                    throw new InternalException(e);
                } catch (IOException e) {
                    logger.error(e);
                    e.printStackTrace();
                    throw new InternalException(e);
                }
            }
            try {
                try {
                    checksum = getChecksum(toUpload);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    throw new InternalException("Unable to generate checksum.");
                }
                try {
                    BufferedOutputStream output;
                    BufferedInputStream input;
                    byte[] buffer = new byte[1024];
                    long count = 0;
                    int b, partNumber = 1;
                    File part;
                    input = new BufferedInputStream(new FileInputStream(toUpload));
                    String fname = toUpload.getParent();
                    if (fname != null) {
                        part = new File(fname + "/" + fileName + ".part." + partNumber);
                    } else {
                        part = new File(fileName + ".part." + partNumber);
                    }
                    output = new BufferedOutputStream(new FileOutputStream(part));
                    while ((b = input.read(buffer, 0, 1024)) > 0) {
                        count += b;
                        output.write(buffer, 0, b);
                        if (count >= 2000000000L) {
                            int tries = 5;
                            output.flush();
                            output.close();
                            while (true) {
                                tries--;
                                try {
                                    put(directory, null, part);
                                    break;
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                    if (tries < 1) {
                                        throw new InternalException("Unable to complete upload for part " + partNumber + " of " + part.getAbsolutePath());
                                    }
                                }
                            }
                            part.delete();
                            partNumber++;
                            part = new File(toUpload.getParent() + "/" + fileName + ".part." + partNumber);
                            output = new BufferedOutputStream(new FileOutputStream(part));
                            count = 0L;
                        }
                    }
                    if (count > 0L) {
                        int tries = 5;
                        output.flush();
                        output.close();
                        while (true) {
                            tries--;
                            try {
                                put(directory, fileName + ".part." + partNumber, part);
                                break;
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                                if (tries < 1) {
                                    throw new InternalException("Unable to complete upload for part " + partNumber + " of " + part.getAbsolutePath());
                                }
                            }
                        }
                        part.delete();
                    }
                    String content = "parts=" + partNumber + "\nchecksum=" + checksum + "\n";
                    if (encryption != null) {
                        content = content + "encrypted=true\n";
                        content = content + "encryptionVersion=" + encryption.getClass().getName() + "\n";
                    } else {
                        content = content + "encrypted=false\n";
                    }
                    int tries = 5;
                    while (true) {
                        tries--;
                        try {
                            put(directory, fileName + ".properties", content);
                            break;
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                            if (tries < 1) {
                                throw new InternalException("Unable to complete upload for properties of " + part.getAbsolutePath());
                            }
                        }
                    }
                } finally {
                    toUpload.delete();
                }
            } catch (IOException e) {
                logger.error(e);
                e.printStackTrace();
                throw new InternalException(e);
            }
        } finally {
            logger.debug("exit - uploadMultipatFile(File, String, String, Encryption)");
        }
    }

    @Override
    public boolean isPublic(String directory, String file) throws CloudException, InternalException {
        return false;
    }
}
