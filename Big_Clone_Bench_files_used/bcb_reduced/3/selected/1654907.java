package org.scicm.storage.bitstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.scicm.core.ConfigurationManager;
import org.scicm.core.Context;
import org.scicm.core.Utils;
import org.scicm.storage.rdbms.DatabaseManager;
import org.scicm.storage.rdbms.TableRow;
import org.apache.log4j.Logger;

/**
 * <P>Stores, retrieves and deletes bitstreams.</P>
 *
 * <P>Presently, asset stores are specified in <code>scicm.cfg</code>.  Since
 * Java does not offer a way of detecting free disk space, the asset store to
 * use for new bitstreams is also specified in a configuration property.  The
 * drawbacks to this are that the administrators are responsible for monitoring
 * available space in the asset stores, and SciCM (Tomcat) has to be restarted
 * when the asset store for new ('incoming') bitstreams is changed.</P>
 *
 * @author  Peter Breton, Robert Tansley
 * @version $Revision: 1.1.1.1 $
 */
public class BitstreamStorageManager {

    /** log4j log */
    private static Logger log = Logger.getLogger(BitstreamStorageManager.class);

    /** The asset store locations */
    private static File[] assetStores;

    /** The asset store to use for new bitstreams */
    private static int incoming;

    private static final int digitsPerLevel = 2;

    private static final int directoryLevels = 3;

    static {
        ArrayList stores = new ArrayList();
        stores.add(ConfigurationManager.getProperty("assetstore.dir"));
        for (int i = 1; ConfigurationManager.getProperty("assetstore.dir." + i) != null; i++) {
            stores.add(ConfigurationManager.getProperty("assetstore.dir." + i));
        }
        assetStores = new File[stores.size()];
        for (int i = 0; i < stores.size(); i++) {
            assetStores[i] = new File((String) stores.get(i));
        }
        incoming = ConfigurationManager.getIntProperty("assetstore.incoming");
    }

    /**
     * Store a stream of bits.
     *
     * <p>If this method returns successfully, the bits have been stored,
     * and RDBMS metadata entries are in place (the context still
     * needs to be completed to finalize the transaction).</p>
     *
     * <p>If this method returns successfully and the context is aborted,
     * then the bits will be stored in the asset store and the RDBMS
     * metadata entries will exist, but with the deleted flag set.</p>
     *
     * If this method throws an exception, then any of the following
     * may be true:
     *
     * <ul>
     *    <li>Neither bits nor RDBMS metadata entries have been stored.
     *    <li>RDBMS metadata entries with the deleted flag set have been
     *        stored, but no bits.
     *    <li>RDBMS metadata entries with the deleted flag set have been
     *        stored, and some or all of the bits have also been stored.
     * </ul>
     *
     * @param context The current context
     * @param is The stream of bits to store
     * @exception IOException If a problem occurs while storing the bits
     * @exception SQLException If a problem occurs accessing the RDBMS
     *
     * @return The ID of the stored bitstream
     */
    public static int store(Context context, InputStream is) throws SQLException, IOException {
        String id = Utils.generateKey();
        TableRow bitstream;
        Context tempContext = null;
        try {
            tempContext = new Context();
            bitstream = DatabaseManager.create(tempContext, "Bitstream");
            bitstream.setColumn("deleted", true);
            bitstream.setColumn("internal_id", id);
            bitstream.setColumn("store_number", incoming);
            DatabaseManager.update(tempContext, bitstream);
            tempContext.complete();
        } catch (SQLException sqle) {
            if (tempContext != null) tempContext.abort();
            throw sqle;
        }
        File file = getFile(bitstream);
        File parent = file.getParentFile();
        if (!parent.exists()) parent.mkdirs();
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        DigestInputStream dis = null;
        try {
            dis = new DigestInputStream(is, MessageDigest.getInstance("SHA-1"));
        } catch (NoSuchAlgorithmException nsae) {
            log.warn("Caught NoSuchAlgorithmException", nsae);
        }
        Utils.bufferedCopy(dis, fos);
        fos.close();
        is.close();
        bitstream.setColumn("size", (int) file.length());
        bitstream.setColumn("checksum", Utils.toHex(dis.getMessageDigest().digest()));
        bitstream.setColumn("checksum_algorithm", "SHA-1");
        bitstream.setColumn("deleted", false);
        DatabaseManager.update(context, bitstream);
        int bitstream_id = bitstream.getIntColumn("bitstream_id");
        if (log.isDebugEnabled()) log.debug("Stored bitstream " + bitstream_id + " in file " + file.getAbsolutePath());
        return bitstream_id;
    }

    /**
     * Retrieve the bits for the bitstream with ID. If the bitstream
     * does not exist, or is marked deleted, returns null.
     *
     * @param context The current context
     * @param id The ID of the bitstream to retrieve
     * @exception IOException If a problem occurs while retrieving the bits
     * @exception SQLException If a problem occurs accessing the RDBMS
     *
     * @return The stream of bits, or null
     */
    public static InputStream retrieve(Context context, int id) throws SQLException, IOException {
        TableRow bitstream = DatabaseManager.find(context, "bitstream", id);
        File file = getFile(bitstream);
        return (file != null) ? new FileInputStream(file) : null;
    }

    /**
     * <p>Remove a bitstream from the asset store. This method does
     * not delete any bits, but simply marks the bitstreams as deleted
     * (the context still needs to be completed to finalize the transaction).
     * </p>
     *
     * <p>If the context is aborted, the bitstreams deletion status
     * remains unchanged.</p>
     *
     * @param context The current context
     * @param id The ID of the bitstream to delete
     * @exception IOException If a problem occurs while deleting the bits
     * @exception SQLException If a problem occurs accessing the RDBMS
     */
    public static void delete(Context context, int id) throws SQLException, IOException {
        DatabaseManager.updateQuery(context, "update Bitstream set deleted = 't' where bitstream_id = " + id);
    }

    /**
     * Clean up the bitstream storage area.
     * This method deletes any bitstreams which are more than 1 hour
     * old and marked deleted. The deletions cannot be undone.
     *
     * @exception IOException If a problem occurs while cleaning up
     * @exception SQLException If a problem occurs accessing the RDBMS
     */
    public static void cleanup() throws SQLException, IOException {
        Context context = null;
        try {
            context = new Context();
            List storage = DatabaseManager.query(context, "Bitstream", "select * from Bitstream where deleted = 't'").toList();
            for (Iterator iterator = storage.iterator(); iterator.hasNext(); ) {
                TableRow row = (TableRow) iterator.next();
                int bid = row.getIntColumn("bitstream_id");
                File file = getFile(row);
                if (file == null) {
                    DatabaseManager.delete(context, "Bitstream", bid);
                    continue;
                }
                if (isRecent(file)) continue;
                DatabaseManager.delete(context, "Bitstream", bid);
                boolean success = file.delete();
                if (log.isDebugEnabled()) log.debug("Deleted bitstream " + bid + " (file " + file.getAbsolutePath() + ") with result " + success);
                deleteParents(file);
            }
            context.complete();
        } catch (SQLException sqle) {
            context.abort();
            throw sqle;
        } catch (IOException ioe) {
            context.abort();
            throw ioe;
        }
    }

    /**
     * Return true if this file is too recent to be deleted,
     * false otherwise.
     *
     * @param file The file to check
     * @return True if this file is too recent to be deleted
     */
    private static boolean isRecent(File file) {
        long lastmod = file.lastModified();
        long now = new java.util.Date().getTime();
        if (lastmod >= now) return true;
        return now - lastmod < (1 * 60 * 1000);
    }

    /**
     * Delete empty parent directories.
     *
     * @param file The file with parent directories to delete
     */
    private static synchronized void deleteParents(File file) {
        if (file == null) return;
        File tmp = file;
        for (int i = 0; i < directoryLevels; i++) {
            File directory = tmp.getParentFile();
            File[] files = directory.listFiles();
            if (files.length != 0) break;
            directory.delete();
            tmp = directory;
        }
    }

    /**
     * Return the file corresponding to a bitstream.  It's safe to pass in
     * <code>null</code>.
     *
     * @param bitstream  the database table row for the bitstream.
     *                   Can be <code>null</code>
     *
     * @return  The corresponding file in the file system, or <code>null</code>
     *
     * @exception IOException If a problem occurs while determining the file
     */
    private static File getFile(TableRow bitstream) throws IOException {
        if (bitstream == null) {
            return null;
        }
        int storeNumber = bitstream.getIntColumn("store_number");
        if (storeNumber == -1) {
            storeNumber = 0;
        }
        File store = assetStores[storeNumber];
        String id = bitstream.getStringColumn("internal_id");
        BigInteger bigint = new BigInteger(id);
        StringBuffer result = new StringBuffer().append(store.getCanonicalPath());
        for (int i = 0; i < directoryLevels; i++) {
            int digits = i * digitsPerLevel;
            result.append(File.separator).append(id.substring(digits, digits + digitsPerLevel));
        }
        String theName = result.append(File.separator).append(id).toString();
        if (log.isDebugEnabled()) log.debug("Filename for " + id + " is " + theName);
        return new File(theName);
    }
}
