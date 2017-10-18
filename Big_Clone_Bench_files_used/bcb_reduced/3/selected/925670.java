package org.dbe.dss.service.store.localdir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.apache.axis.encoding.Base64;
import org.apache.log4j.Logger;
import org.dbe.dss.DSSException;
import org.dbe.dss.service.store.StoreConnector;
import org.dbe.servent.ServiceContext;

/**
 * The implementation class of a DSS Service Store that persists content on the local file-system.
 *
 * @author Intel Ireland Ltd.
 * @version 0.5.0
 */
public class LocalDirStoreConnector implements StoreConnector {

    /** Log4j logger for this object */
    private static Logger logger = Logger.getLogger("org.dbe.dss.service.store.localdir.LocalDirStoreConnector");

    /** Default relative path for store directory */
    private static final String DEFAULT_RELATIVE_STORE_DIRECTORY = File.separator + "store";

    /** Prefix for the store ID */
    private static final String STORE_ID_PREFIX = "DS-";

    /** Default unconfigured store ID  */
    private static final String DEFAULT_STORE_ID = "__UNCONFIGURED_DSS_STORE_ID__";

    private String storeDirectory = System.getProperty("user.home") + DEFAULT_RELATIVE_STORE_DIRECTORY;

    /** The unique ID of this store */
    private String storeID = DEFAULT_STORE_ID;

    /**
     * Default constructor.
     */
    public LocalDirStoreConnector() {
        super();
    }

    /**
     * Constructs a DSS Service Store with the supplied configuration.
     *
     * @param sc the service context including the configuration parameters for this store
     * @throws DSSException if an error occurs during invocation
     */
    public LocalDirStoreConnector(final ServiceContext sc) throws DSSException {
        super();
        configure(sc);
    }

    /**
     * @see org.dbe.dss.service.store.StoreConnector#configure(org.dbe.servent.ServiceContext)
     */
    public final String configure(final ServiceContext sc) throws DSSException {
        logger.debug("configure(...)");
        logger.debug("initial storeDirectory: " + storeDirectory);
        logger.debug("initital storeID: " + storeID);
        storeDirectory = sc.getParameter("dss.store.directory");
        storeID = sc.getParameter("dss.store.id");
        logger.debug("loaded storeDirectory: " + storeDirectory);
        logger.debug("loaded storeID: " + storeID);
        if (storeDirectory == null || storeDirectory.length() == 0) {
            storeDirectory = sc.getHome().getAbsolutePath() + DEFAULT_RELATIVE_STORE_DIRECTORY;
            logger.info("Default DSS store directory has been assigned: " + storeDirectory);
        }
        if (storeID == null || storeID.length() == 0 || storeID.equals(DEFAULT_STORE_ID)) {
            storeID = STORE_ID_PREFIX + UUID.randomUUID().toString();
            logger.warn("DSS store ID was not configured correctly. A new random store ID is being assigned: " + storeID);
        }
        logger.debug("cleaned storeDirectory: " + storeDirectory);
        logger.debug("cleaned storeID: " + storeID);
        File s = new File(storeDirectory);
        if (!s.exists()) {
            String errMsg = "The configured DSS store Directory does not exist: " + storeDirectory;
            logger.fatal(errMsg);
            throw new DSSException(errMsg);
        }
        return storeID;
    }

    /**
     * @see org.dbe.dss.service.store.StoreConnector#write(byte[])
     */
    public final String write(final byte[] content) throws DSSException {
        logger.debug("write(...)");
        if (content.length == 0) {
            String errMsg = "Cannot write an empty block";
            logger.error(errMsg);
            throw new DSSException(errMsg);
        } else {
            byte[] hashcode = null;
            String strHashcode = "";
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                md.update(content);
                hashcode = md.digest();
            } catch (NoSuchAlgorithmException e) {
                String errMsg = "Could not calculate hashcode: " + e.getMessage();
                logger.error(errMsg);
                throw new DSSException(errMsg);
            }
            strHashcode = Base64.encode(hashcode);
            String name = strHashcode.replace('/', '_');
            logger.debug("New block name: " + name);
            if (exists(name)) {
                logger.debug("block already exists");
                return name;
            } else {
                logger.debug("storing block");
                String absoluteBlockPath = storeDirectory + File.separator + name;
                File f = new File(absoluteBlockPath);
                FileOutputStream fos = null;
                try {
                    if (f.createNewFile()) {
                        fos = new FileOutputStream(f);
                        fos.write(content);
                    } else {
                        String errMsg = "Could not create a new file when attempting to write block.";
                        logger.error(errMsg);
                        throw new DSSException(errMsg);
                    }
                } catch (FileNotFoundException e) {
                    String errMsg = "Could not open FileOutputStream to write block: " + e.getMessage();
                    logger.error(errMsg);
                    throw new DSSException(errMsg);
                } catch (IOException e) {
                    String errMsg = "Could not write block: " + e.getMessage();
                    logger.error(errMsg);
                    throw new DSSException(errMsg);
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            logger.error("Could not close FileOutputStream: " + e.getMessage());
                        }
                    }
                }
                return name;
            }
        }
    }

    /**
     * @see org.dbe.dss.service.store.StoreConnector#read(java.lang.String)
     */
    public final byte[] read(final String name) throws DSSException {
        logger.debug("read(" + name + ")");
        File block = new File(storeDirectory + File.separator + name);
        return read(name, 0, (int) block.length());
    }

    /**
     * @see org.dbe.dss.service.store.StoreConnector#read(java.lang.String, int, int)
     */
    public final byte[] read(final String name, final int offset, final int len) throws DSSException {
        logger.debug("read(" + name + "," + offset + "," + len + ")");
        byte[] result = null;
        if (len < 1) {
            String errMsg = "Cannot read a zero or negative length: " + len;
            logger.error(errMsg);
            throw new DSSException(errMsg);
        }
        if (!exists(name)) {
            String errMsg = "Block " + name + " does not exist locally";
            logger.error(errMsg);
            throw new DSSException(errMsg);
        }
        File block = new File(storeDirectory + File.separator + name);
        byte[] readBytes = null;
        FileInputStream is = null;
        try {
            is = new FileInputStream(block);
            readBytes = new byte[(int) len];
            int numBytesRead = is.read(readBytes, offset, len);
            result = readBytes;
        } catch (FileNotFoundException e) {
            String errMsg = "Could not find block " + block.getAbsolutePath() + ": " + e.getMessage();
            logger.error(errMsg);
            throw new DSSException(errMsg);
        } catch (IOException e) {
            String errMsg = "Could not read block " + block.getAbsolutePath() + ": " + e.getMessage();
            logger.error(errMsg);
            throw new DSSException(errMsg);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("Could not close FileInputStream: " + e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * @see org.dbe.dss.service.store.StoreConnector#exists(java.lang.String)
     */
    public final boolean exists(final String name) throws DSSException {
        logger.debug("exists(" + name + ")");
        if (name == null || name.length() == 0) {
            String errMsg = "The name supplied was empty";
            logger.error(errMsg);
            throw new DSSException(errMsg);
        } else {
            File block = new File(storeDirectory + File.separator + name);
            return block.exists();
        }
    }

    /**
     * @see org.dbe.dss.service.store.StoreConnector#update(java.lang.String, int)
     */
    public final boolean update(final String name, final int timeToLive) throws DSSException {
        logger.debug("update(" + name + ", " + timeToLive + ")");
        return false;
    }
}
