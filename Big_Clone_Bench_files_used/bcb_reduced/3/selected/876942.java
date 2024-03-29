package org.dspace.checker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;

/**
 * <p>
 * Main class for the checksum checker tool, which calculates checksums for each
 * bitstream whose ID is in the most_recent_checksum table, and compares it
 * against the last calculated checksum for that bitstream.
 * </p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 * 
 * @todo the accessor methods are currently unused - are they useful?
 * @todo check for any existing resource problems
 */
public final class CheckerCommand {

    /** Usual Log4J logger. */
    private static final Logger LOG = Logger.getLogger(CheckerCommand.class);

    /** Default digest algorithm (MD5). */
    private static final String DEFAULT_DIGEST_ALGORITHM = "MD5";

    /** 4 Meg byte array for reading file. */
    private int BYTE_ARRAY_SIZE = 4 * 1024;

    /** BitstreamInfoDAO dependency. */
    private BitstreamInfoDAO bitstreamInfoDAO = null;

    /** BitstreamDAO dependency. */
    private BitstreamDAO bitstreamDAO = null;

    /**
     * Checksum history Data access object
     */
    private ChecksumHistoryDAO checksumHistoryDAO = null;

    /** start time for current process. */
    private Date processStartDate = null;

    /**
     * Dispatcher to be used for processing run.
     */
    private BitstreamDispatcher dispatcher = null;

    /**
     * Container/logger with details about each bitstream and checksum results.
     */
    private ChecksumResultsCollector collector = null;

    /** Report all processing */
    private boolean reportVerbose = false;

    /**
     * Default constructor uses DSpace plugin manager to construct dependencies.
     */
    public CheckerCommand() {
        bitstreamInfoDAO = new BitstreamInfoDAO();
        bitstreamDAO = new BitstreamDAO();
        checksumHistoryDAO = new ChecksumHistoryDAO();
    }

    /**
     * <p>
     * Uses the options set up on this checker to determine a mode of execution,
     * and then accepts bitstream ids from the dispatcher and checks their
     * bitstreams against the db records.
     * </p>
     * 
     * <p>
     * N.B. a valid BitstreamDispatcher must be provided using
     * setBitstreamDispatcher before calling this method
     * </p>
     */
    public void process() {
        LOG.debug("Begin Checker Processing");
        if (dispatcher == null) {
            throw new IllegalStateException("No BitstreamDispatcher provided");
        }
        if (collector == null) {
            collector = new ResultsLogger(processStartDate);
        }
        bitstreamInfoDAO.updateMissingBitstreams();
        int id = dispatcher.next();
        while (id != BitstreamDispatcher.SENTINEL) {
            LOG.debug("Processing bitstream id = " + id);
            BitstreamInfo info = checkBitstream(id);
            if (reportVerbose || (info.getChecksumCheckResult() != ChecksumCheckResults.CHECKSUM_MATCH)) {
                collector.collect(info);
            }
            id = dispatcher.next();
        }
    }

    /**
     * Check a specified bitstream.
     * 
     * @param id
     *            the bitstream id
     * 
     * @return the information about the bitstream and its checksum data
     */
    private BitstreamInfo checkBitstream(final int id) {
        BitstreamInfo info = bitstreamInfoDAO.findByBitstreamId(id);
        if (info == null) {
            info = new BitstreamInfo(id);
            processNullInfoBitstream(info);
        } else if (!info.getToBeProcessed()) {
            info.setChecksumCheckResult(ChecksumCheckResults.BITSTREAM_NOT_PROCESSED);
        } else if (info.getDeleted()) {
            processDeletedBitstream(info);
        } else {
            processBitstream(info);
        }
        return info;
    }

    /**
     * Digest the stream and get the checksum value.
     * 
     * @param stream
     *            InputStream to digest.
     * @param algorithm
     *            the algorithm to use when digesting.
     * @todo Document the algorithm parameter
     * @return digest
     * 
     * @throws java.security.NoSuchAlgorithmException
     *             if the requested algorithm is not provided by the system
     *             security provider.
     * @throws java.io.IOException
     *             If an exception arises whilst reading the stream
     */
    private String digestStream(InputStream stream, String algorithm) throws java.security.NoSuchAlgorithmException, java.io.IOException {
        DigestInputStream dStream = new DigestInputStream(stream, MessageDigest.getInstance(algorithm));
        byte[] bytes = new byte[BYTE_ARRAY_SIZE];
        while (dStream.read(bytes, 0, BYTE_ARRAY_SIZE) != -1) {
        }
        return Utils.toHex(dStream.getMessageDigest().digest());
    }

    /**
     * Compares two checksums.
     * 
     * @param checksumA
     *            the first checksum
     * @param checksumB
     *            the second checksum
     * 
     * @return a result code (constants defined in Util)
     */
    private String compareChecksums(String checksumA, String checksumB) {
        String result = ChecksumCheckResults.CHECKSUM_NO_MATCH;
        if ((checksumA == null) || (checksumB == null)) {
            result = ChecksumCheckResults.CHECKSUM_PREV_NOT_FOUND;
        } else if (checksumA.equals(checksumB)) {
            result = ChecksumCheckResults.CHECKSUM_MATCH;
        }
        return result;
    }

    /**
     * Process bitstream that was marked 'deleted' in bitstream table. A deleted
     * bitstream should only be checked once afterwards it should be marked
     * 'to_be_processed=false'. Note that to_be_processed must be manually
     * updated in db to allow for future processing.
     * 
     * @param info
     *            a deleted bitstream.
     */
    private void processDeletedBitstream(BitstreamInfo info) {
        info.setProcessStartDate(new Date());
        info.setChecksumCheckResult(ChecksumCheckResults.BITSTREAM_MARKED_DELETED);
        info.setProcessStartDate(new Date());
        info.setProcessEndDate(new Date());
        info.setToBeProcessed(false);
        bitstreamInfoDAO.update(info);
        checksumHistoryDAO.insertHistory(info);
    }

    /**
     * Process bitstream whose ID was not found in most_recent_checksum or
     * bitstream table. No updates can be done. The missing bitstream is output
     * to the log file.
     * 
     * @param info
     *            A not found BitStreamInfo
     * @todo is this method required?
     */
    private void processNullInfoBitstream(BitstreamInfo info) {
        info.setInfoFound(false);
        info.setProcessStartDate(new Date());
        info.setProcessEndDate(new Date());
        info.setChecksumCheckResult(ChecksumCheckResults.BITSTREAM_INFO_NOT_FOUND);
    }

    /**
     * <p>
     * Process general case bistream.
     * </p>
     * 
     * <p>
     * Note: bitstream will have timestamp indicating it was "checked", even if
     * actual checksumming never took place.
     * </p>
     * 
     * @todo Why does bitstream have a timestamp indicating it's checked if
     *       checksumming doesn't occur?
     * 
     * @param info
     *            BitstreamInfo to handle
     */
    private void processBitstream(BitstreamInfo info) {
        info.setProcessStartDate(new Date());
        if (info.getChecksumAlgorithm() == null) {
            info.setChecksumAlgorithm(DEFAULT_DIGEST_ALGORITHM);
        }
        try {
            InputStream bitstream = bitstreamDAO.getBitstream(info.getBitstreamId());
            info.setBitstreamFound(true);
            String checksum = digestStream(bitstream, info.getChecksumAlgorithm());
            info.setCalculatedChecksum(checksum);
            info.setChecksumCheckResult(compareChecksums(info.getStoredChecksum(), info.getCalculatedChecksum()));
        } catch (IOException e) {
            info.setChecksumCheckResult(ChecksumCheckResults.BITSTREAM_NOT_FOUND);
            info.setToBeProcessed(false);
            LOG.error("Error retrieving bitstream ID " + info.getBitstreamId() + " from " + "asset store.", e);
        } catch (SQLException e) {
            info.setChecksumCheckResult(ChecksumCheckResults.BITSTREAM_INFO_NOT_FOUND);
            LOG.error("Error retrieving metadata for bitstream ID " + info.getBitstreamId(), e);
        } catch (NoSuchAlgorithmException e) {
            info.setChecksumCheckResult(ChecksumCheckResults.CHECKSUM_ALGORITHM_INVALID);
            info.setToBeProcessed(false);
            LOG.error("Invalid digest algorithm type for bitstream ID" + info.getBitstreamId(), e);
        } finally {
            info.setProcessEndDate(new Date());
            bitstreamInfoDAO.update(info);
            checksumHistoryDAO.insertHistory(info);
        }
    }

    /**
     * Get dispatcher being used by this run of the checker.
     * 
     * @return the dispatcher being used by this run.
     */
    public BitstreamDispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * Set the dispatcher to be used by this run of the checker.
     * 
     * @param dispatcher
     *            Dispatcher to use.
     */
    public void setDispatcher(BitstreamDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Get the collector that holds/logs the results for this process run.
     * 
     * @return The ChecksumResultsCollecter being used.
     */
    public ChecksumResultsCollector getCollector() {
        return collector;
    }

    /**
     * Set the collector that holds/logs the results for this process run.
     * 
     * @param collector
     *            the collector to be used for this run
     */
    public void setCollector(ChecksumResultsCollector collector) {
        this.collector = collector;
    }

    /**
     * Get time at which checker process began.
     * 
     * @return start time
     */
    public Date getProcessStartDate() {
        return processStartDate;
    }

    /**
     * Set time at which checker process began.
     * 
     * @param startDate
     *            start time
     */
    public void setProcessStartDate(Date startDate) {
        processStartDate = startDate;
    }

    /**
     * Determine if any errors are reported
     * 
     * @return true if only errors reported
     */
    public boolean isReportVerbose() {
        return reportVerbose;
    }

    /**
     * Set report errors only
     * 
     * @param reportErrorsOnly
     *            true to report only errors in the logs.
     */
    public void setReportVerbose(boolean reportVerbose) {
        this.reportVerbose = reportVerbose;
    }
}
