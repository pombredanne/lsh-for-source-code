package org.ourgrid.acceptance.util.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.easymock.classextension.EasyMock;
import org.ourgrid.acceptance.util.WorkerAcceptanceUtil;
import org.ourgrid.broker.actions.ErrorOcurredMessageHandle;
import org.ourgrid.common.exception.UnableToDigestFileException;
import org.ourgrid.common.filemanager.FileInfo;
import org.ourgrid.common.interfaces.Worker;
import org.ourgrid.common.interfaces.WorkerClient;
import org.ourgrid.common.interfaces.to.GridProcessErrorTypes;
import org.ourgrid.matchers.ErrorOcurredMessageHandleMatcher;
import org.ourgrid.matchers.GetFileInfoMessageHandleMatcher;
import org.ourgrid.matchers.HereIsFileInfoMessageHandleMatcher;
import org.ourgrid.worker.WorkerComponent;
import org.ourgrid.worker.controller.GridProcessError;
import org.ourgrid.worker.controller.actions.GetFileInfoMessageHandle;
import sun.misc.BASE64Encoder;
import br.edu.ufcg.lsd.commune.container.ContainerContext;
import br.edu.ufcg.lsd.commune.container.ObjectDeployment;
import br.edu.ufcg.lsd.commune.container.logging.CommuneLogger;
import br.edu.ufcg.lsd.commune.processor.filetransfer.OutgoingTransferHandle;
import br.edu.ufcg.lsd.commune.test.AcceptanceTestUtil;

public class Req_082_Util extends WorkerAcceptanceUtil {

    public Req_082_Util(ContainerContext context) {
        super(context);
    }

    public void getFileInfoByUnknownClient(WorkerComponent component, String filePath, String senderPubKey) {
        getFileInfo(component, null, filePath, filePath, senderPubKey, false, false, null, false, null);
    }

    public void getFileInfoByClientWithoutStartingWork(WorkerComponent component, String filePath, String senderPubKey) {
        getFileInfo(component, null, filePath, filePath, senderPubKey, true, false, null, false, null);
    }

    public void getFileInfoSuccessfully(WorkerComponent component, WorkerClient workerClient, String workerClientPublicKey, String filePath, String fileDigest) {
        getFileInfo(component, workerClient, filePath, filePath, workerClientPublicKey, true, true, null, true, fileDigest);
    }

    public void getFileInfoSuccessfully(WorkerComponent component, WorkerClient workerClient, String workerClientPublicKey, String relativeFilePath, String absoluteFilePath, String fileDigest) {
        getFileInfo(component, workerClient, relativeFilePath, absoluteFilePath, workerClientPublicKey, true, true, null, true, fileDigest);
    }

    public void getNullFileInfo(WorkerComponent component, WorkerClient workerClient, String workerClientPublicKey) {
        getFileInfoWithError(component, workerClient, workerClientPublicKey, null, "File path is null.");
    }

    public void getFilePathInfoWithInvalidVariable(WorkerComponent component, WorkerClient workerClient, String workerClientPublicKey, String filePath) {
        getFileInfoWithError(component, workerClient, workerClientPublicKey, filePath, "Invalid variable found.");
    }

    public void getFileInfoForNotRelativeFilePath(WorkerComponent component, WorkerClient workerClient, String workerClientPublicKey, String filePath, String absolutePath) {
        getFileInfoWithError(component, workerClient, workerClientPublicKey, filePath, "File path is not relative to " + absolutePath + " directory.");
    }

    public void getUnreadableFileInfo(WorkerComponent component, WorkerClient workerClient, String workerClientPublicKey, String filePath) {
        getFileInfoWithError(component, workerClient, workerClientPublicKey, filePath, "File cannot be read.");
    }

    private void getFileInfoWithError(WorkerComponent component, WorkerClient workerClient, String workerClientPublicKey, String filePath, String cause) {
        getFileInfo(component, workerClient, filePath, filePath, workerClientPublicKey, true, true, cause, false, null);
    }

    private void getFileInfo(WorkerComponent component, WorkerClient workerClient, String relativeFilePath, String absoluteFilePath, String senderPubKey, boolean isClientKnown, boolean hasClientStartedWork, String errorCause, boolean isFilePathValid, String fileDigest) {
        CommuneLogger logger = component.getLogger();
        if (workerClient != null) {
            EasyMock.reset(workerClient);
        }
        if (!isClientKnown) {
            logger.warn("An unknown client tried to get info about the file [" + relativeFilePath + "]. " + "This message was ignored. Unknown client public key: [" + senderPubKey + "].");
        } else {
            if (!hasClientStartedWork) {
                logger.debug("The client requested info about the file [" + relativeFilePath + "], " + "but this Worker was not commanded to start the work yet. " + "This message was ignored. Client public key: [" + senderPubKey + "].");
            } else {
                if (isFilePathValid) {
                    try {
                        logger.debug("Client successfully got file info. " + "File: [" + new File(absoluteFilePath).getCanonicalPath() + "]. " + "Client public key: [" + senderPubKey + "].");
                    } catch (Exception e) {
                    }
                    workerClient.sendMessage(HereIsFileInfoMessageHandleMatcher.eqMatcher(null, new FileInfo(relativeFilePath, fileDigest)));
                } else {
                    logger.warn("Error occurred while trying to get file INFO. " + "File: [" + relativeFilePath + "]. Client public key: [" + senderPubKey + "]. " + "Cause: [" + errorCause + "].");
                    workerClient.sendMessage(ErrorOcurredMessageHandleMatcher.eqMatcher(new ErrorOcurredMessageHandle(new GridProcessError(GridProcessErrorTypes.APPLICATION_ERROR))));
                }
                EasyMock.replay(workerClient);
            }
        }
        EasyMock.replay(logger);
        Worker worker = getWorker();
        ObjectDeployment workerOD = getWorkerDeployment();
        AcceptanceTestUtil.setExecutionContext(component, workerOD, senderPubKey);
        OutgoingTransferHandle handle = getOutgoingTransferHandle(component, relativeFilePath);
        if (handle != null) {
            GetFileInfoMessageHandle getFileInfoMessageHandle = new GetFileInfoMessageHandle(handle.getId(), 0, relativeFilePath);
            worker.sendMessage(GetFileInfoMessageHandleMatcher.eqMatcher(getFileInfoMessageHandle));
        } else {
            worker.sendMessage(new GetFileInfoMessageHandle(0, 0, relativeFilePath));
        }
        EasyMock.verify(logger);
        if (workerClient != null) {
            EasyMock.verify(workerClient);
            EasyMock.reset(workerClient);
            EasyMock.replay(workerClient);
        }
        EasyMock.reset(logger);
    }

    public static String getFileDigest(String filePath) throws UnableToDigestFileException {
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) {
            return "0";
        }
        return getDigestRepresentation(file);
    }

    /**
	 * That utility method get a File object in applying a Message Digest
	 * Filter, the result is a digest string representation of the file contents
	 * 
	 * @param fileToDigest The File object abstraction that denotes a file to be
	 *        digested
	 * @return The digest string representation of the file contents. Or null if
	 *         some exception occurs,
	 * @throws UnableToDigestFileException If there is any problem on the digest
	 *         generation, like the file is not found, I/O errors or the digest
	 *         algorithm is not valid.
	 */
    private static String getDigestRepresentation(File fileToDigest) throws UnableToDigestFileException {
        MessageDigest messageDigest;
        FileInputStream inputStream = null;
        byte[] buffer = new byte[8129];
        int numberOfBytes;
        byte[] digestValue;
        BASE64Encoder encoder;
        String fileHash = new String();
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            inputStream = new FileInputStream(fileToDigest.getAbsoluteFile());
            numberOfBytes = inputStream.read(buffer);
            while (numberOfBytes != -1) {
                messageDigest.update(buffer, 0, numberOfBytes);
                numberOfBytes = inputStream.read(buffer);
            }
            digestValue = messageDigest.digest();
            encoder = new BASE64Encoder();
            fileHash = encoder.encode(digestValue);
        } catch (IOException exception) {
            throw new UnableToDigestFileException(fileToDigest.getAbsolutePath(), exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new UnableToDigestFileException(fileToDigest.getAbsolutePath(), exception);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return fileHash;
    }
}
