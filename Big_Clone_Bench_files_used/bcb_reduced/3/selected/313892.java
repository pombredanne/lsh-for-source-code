package org.ikasan.connector.basefiletransfer.outbound.command;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.resource.ResourceException;
import org.apache.log4j.Logger;
import org.ikasan.common.Payload;
import org.ikasan.connector.base.command.ExecutionContext;
import org.ikasan.connector.base.command.ExecutionOutput;
import org.ikasan.connector.basefiletransfer.net.BaseFileTransferMappedRecord;
import org.ikasan.connector.basefiletransfer.net.ChecksumFailedException;

/**
 * Command for comparing the calculated checksum of a delivered file with that
 * provided by the remote system, if any
 * 
 * @author Ikasan Development Team 
 */
public class ChecksumDeliveredCommand extends AbstractBaseFileTransferTransactionalResourceCommand {

    /** The logger instance. */
    private static Logger logger = Logger.getLogger(FileDiscoveryCommand.class);

    /** 
     * Constructor 
     * 
     * TODO Check that we should actually be calling super as that reloads state
     * to be initial and we might not want that on reload of command from DB
     */
    public ChecksumDeliveredCommand() {
        super();
    }

    @Override
    protected void doCommit() {
        logger.info("commit called on this command: [" + this + "]");
    }

    @Override
    protected ExecutionOutput performExecute() throws ResourceException {
        String deliveredPath = executionContext.getRequiredString(ExecutionContext.DELIVERED_FILE_PATH_PARAM);
        logger.debug("checksum delivered got deliveredPath: [" + deliveredPath + "]");
        Payload payload = (Payload) executionContext.getRequired(ExecutionContext.PAYLOAD);
        try {
            BaseFileTransferMappedRecord file = getFile(deliveredPath);
            if (file == null) {
                throw new ChecksumFailedException("Could not retrieve delivered file!");
            }
            String generatedChecksum = getChecksum(payload.getContent());
            String reloadedChecksum = getChecksum(file.getContent());
            if (!generatedChecksum.equals(reloadedChecksum)) {
                throw new ChecksumFailedException("Checksums didn't match!");
            }
        } catch (ChecksumFailedException e) {
            logger.warn("A checksum failed related exception occurred!", e);
            throw new ResourceException(e);
        }
        logger.info("checksum on file matched that from foreign system");
        return new ExecutionOutput();
    }

    @Override
    protected void doRollback() {
        logger.info("rollback called on this command: [" + this + "]");
    }

    /**
     * Get the MD5 checksum given an ImputStream
     * 
     * @param byte array for which to calculate the checksum
     * @return <code>String</code> representing the checksum
     */
    public static String getChecksum(byte[] input) {
        MessageDigest checksum;
        try {
            checksum = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        checksum.update(input);
        byte[] byteDigest = checksum.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteDigest.length; i++) {
            String hex = Integer.toHexString(0xff & byteDigest[i]);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return new String(sb.toString());
    }
}
