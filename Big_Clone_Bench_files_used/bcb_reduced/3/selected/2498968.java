package org.ourgrid.worker.controller;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.ourgrid.common.exception.UnableToDigestFileException;
import org.ourgrid.common.util.JavaFileUtil;
import org.ourgrid.reqtrace.Req;
import org.ourgrid.worker.WorkerConfiguration;
import org.ourgrid.worker.WorkerConstants;
import org.ourgrid.worker.dao.EnvironmentDAO;
import org.ourgrid.worker.utils.RandomNumberUtil;
import br.edu.ufcg.lsd.commune.container.ContainerContext;
import br.edu.ufcg.lsd.commune.container.servicemanager.ServiceManager;
import br.edu.ufcg.lsd.commune.network.signature.Util;

/**
 * Controls this worker's environment.
 */
public class EnvironmentController {

    private static EnvironmentController instance;

    @Req("REQ079")
    public static synchronized EnvironmentController getInstance() {
        if (instance == null) {
            instance = new EnvironmentController();
        }
        return instance;
    }

    @Req("REQ079")
    public void mountPlaypen(ServiceManager servicerManager) throws UnableToCreatePlaypenException {
        String playpenDirPath = getPlaypenRoot(servicerManager.getContainerContext()) + File.separator + generatePlaypenDir();
        File playpenRoot = new File(getPlaypenRoot(servicerManager.getContainerContext()));
        if (!playpenRoot.exists()) {
            boolean rootSuccessfulCreation = playpenRoot.mkdirs();
            if (!rootSuccessfulCreation) {
                throw new UnableToCreatePlaypenException(playpenDirPath);
            }
        } else {
            if (!playpenRoot.canWrite()) {
                throw new UnableToCreatePlaypenException(playpenDirPath);
            }
        }
        File playpenDir = new File(playpenDirPath);
        if (playpenDir.exists()) {
            throw new UnableToCreatePlaypenException(playpenDirPath);
        }
        boolean dirSuccessfulCreation = playpenDir.mkdir();
        if (!dirSuccessfulCreation) {
            throw new UnableToCreatePlaypenException(playpenDirPath);
        }
        if (!playpenDir.canWrite()) {
            throw new UnableToCreatePlaypenException(playpenDirPath);
        }
        servicerManager.getDAO(EnvironmentDAO.class).setPlaypenDir(playpenDirPath);
    }

    @Req("REQ079")
    public void unmountPlaypen(ServiceManager serviceManager) {
        serviceManager.getLog().debug(WorkerControllerMessages.getCleaningWorkerMessage());
        EnvironmentDAO environmentDAO = serviceManager.getDAO(EnvironmentDAO.class);
        try {
            String playpenDir = environmentDAO.getPlaypenDir();
            environmentDAO.setPlaypenDir(null);
            if (playpenDir == null) {
                return;
            }
            boolean successfulDestruction = destroyPlaypen(playpenDir);
            if (!successfulDestruction) {
                serviceManager.getLog().error(EnvironmentControllerMessages.getUnsuccesfulPlaypenDirDeletionMessage(playpenDir));
            }
        } catch (IOException e) {
            serviceManager.getLog().error(EnvironmentControllerMessages.getUnsuccesfulPlaypenDirDeletionMessage(environmentDAO.getPlaypenDir()), e);
        }
    }

    @Req("REQ082")
    public void unmountEnvironment(ServiceManager serviceManager) {
        unmountPlaypen(serviceManager);
        serviceManager.getDAO(EnvironmentDAO.class).setStorageDir(null);
    }

    @Req({ "REQ079", "REQ082" })
    public void mountStorage(String consumerPubKey, ServiceManager serviceManager) throws UnableToCreateStorageException {
        String storageDirPath;
        try {
            storageDirPath = getStorageRoot(serviceManager.getContainerContext()) + File.separator + generateStorageDir(consumerPubKey);
        } catch (NoSuchAlgorithmException e) {
            throw new UnableToCreateStorageException(null);
        }
        File storageRoot = new File(getStorageRoot(serviceManager.getContainerContext()));
        if (!storageRoot.exists()) {
            boolean rootSuccessfulCreation = storageRoot.mkdirs();
            if (!rootSuccessfulCreation) {
                throw new UnableToCreateStorageException(storageDirPath);
            }
        } else {
            if (!storageRoot.canWrite()) {
                throw new UnableToCreateStorageException(storageDirPath);
            }
        }
        File storageDir = new File(storageDirPath);
        if (!storageDir.exists()) {
            boolean dirSuccessfulCreation = storageDir.mkdir();
            if (!dirSuccessfulCreation) {
                throw new UnableToCreateStorageException(storageDirPath);
            }
        }
        if (!storageDir.canWrite()) {
            throw new UnableToCreateStorageException(storageDirPath);
        }
        serviceManager.getDAO(EnvironmentDAO.class).setStorageDir(storageDir.getAbsolutePath());
    }

    private String generateStorageDir(String stringToBeHashed) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(stringToBeHashed.getBytes());
        byte[] hashedKey = digest.digest();
        return Util.encodeArrayToHexadecimalString(hashedKey);
    }

    @Req("REQ079")
    private boolean destroyPlaypen(String playpenPath) throws IOException {
        boolean successful = true;
        File file = new File(playpenPath);
        if (file.exists() && file.isDirectory()) {
            successful = successful && deleteFilesInDir(file);
            successful = successful && file.delete();
        }
        return successful;
    }

    /**
	 * Removes all the files in a specified directory.
	 * 
	 * @param directory The directory from which all the files will be removed.
	 * @throws IOException if any I/O problem happens in the deletion process.
	 */
    @Req("REQ079")
    private boolean deleteFilesInDir(File directory) throws IOException {
        boolean successful = true;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    successful = successful && deleteFilesInDir(file);
                }
                successful = successful && file.delete();
            }
        }
        return successful;
    }

    @Req("REQ079")
    public String getPlaypenRoot(ContainerContext containerContext) {
        return containerContext.getProperty(WorkerConfiguration.PROP_PLAYPEN_ROOT);
    }

    /**
	 * Returns the storage root.
	 * @return the storage root.
	 */
    public String getStorageRoot(ContainerContext containerContext) {
        return containerContext.getProperty(WorkerConfiguration.PROP_STORAGE_DIR);
    }

    @Req("REQ079")
    private String generatePlaypenDir() {
        long randomNumber = (long) (RandomNumberUtil.getRandom() * Long.MAX_VALUE);
        randomNumber = Long.signum(randomNumber) == -1 ? randomNumber * (-1) : randomNumber;
        return "worker-" + randomNumber;
    }

    @Req({ "REQ082" })
    public String solveStorageDir(String filePath, ServiceManager servicerManager) throws IOException {
        return solveDir(filePath, WorkerConstants.ENV_STORAGE, servicerManager.getDAO(EnvironmentDAO.class).getStorageDir());
    }

    @Req("REQ080")
    public String solvePlaypenDir(String filePath, ServiceManager servicerManager) throws IOException {
        return solveDir(filePath, WorkerConstants.ENV_PLAYPEN, servicerManager.getDAO(EnvironmentDAO.class).getPlaypenDir());
    }

    @Req("REQ082")
    public String getFileDigest(String filePath) throws UnableToDigestFileException {
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) {
            return "0";
        }
        return JavaFileUtil.getDigestRepresentation(file);
    }

    @Req("REQ080")
    public String solveDir(String filePath, ServiceManager serviceManager) throws IOException {
        if (filePath != null && filePath.contains("$" + WorkerConstants.ENV_STORAGE)) {
            return solveStorageDir(filePath, serviceManager);
        }
        return solvePlaypenDir(filePath, serviceManager);
    }

    @Req({ "REQ080", "REQ082" })
    public String solveDir(String filePath, String relativePath, String absolutePath) throws IOException {
        if (filePath == null) {
            throw new IOException(EnvironmentControllerMessages.getNullFilePathMessage());
        }
        if (!checkFilePathVar(filePath, relativePath)) {
            throw new IOException(EnvironmentControllerMessages.getInvalidVariableFoundMessage());
        }
        String solvedPath = null;
        if (filePath.indexOf("$") == 0) {
            solvedPath = filePath.replace("$" + relativePath, absolutePath);
        } else {
            solvedPath = absolutePath + File.separator + filePath;
        }
        if (!isRelativeTo(solvedPath, absolutePath)) {
            throw new IOException(EnvironmentControllerMessages.getNotRelativeFilePathMessage(absolutePath));
        }
        File file = new File(solvedPath);
        if (file.exists() && !file.canRead()) {
            throw new IOException(EnvironmentControllerMessages.getUnreadableFileInfoMessage());
        }
        return file.getCanonicalPath();
    }

    /**
	 * If file path does not contain any $VARIABLE, return <code>true</code>;
	 * If file path contains a $VARIABLE, it has to start with $ALLOWED_VARIABLE and cannot
	 * contain any additional variable.
	 */
    @Req("REQ080")
    private boolean checkFilePathVar(String filePath, String allowedVariable) {
        int firstDollar = filePath.indexOf("$");
        if (firstDollar == -1) {
            return true;
        }
        return filePath.startsWith("$" + allowedVariable) && (firstDollar == filePath.lastIndexOf("$"));
    }

    @Req("REQ080")
    private boolean isRelativeTo(String filePath, String directory) throws IOException {
        final String fileCanPath = new File(filePath).getCanonicalPath();
        final String dirCanPath = new File(directory).getCanonicalPath();
        return fileCanPath.startsWith(dirCanPath);
    }

    public boolean isCleaning(ServiceManager servicerManager) {
        File playpenRoot = new File(getPlaypenRoot(servicerManager.getContainerContext()));
        if (!playpenRoot.isDirectory()) {
            return true;
        }
        return playpenRoot.listFiles().length == 0;
    }
}
