package com.sleepycat.je.rep.impl.networkRestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.dbi.EnvironmentFailureReason;
import com.sleepycat.je.rep.impl.RepImpl;
import com.sleepycat.je.rep.impl.networkRestore.Protocol.FeederInfoReq;
import com.sleepycat.je.rep.impl.networkRestore.Protocol.FileInfoReq;
import com.sleepycat.je.rep.impl.networkRestore.Protocol.FileInfoResp;
import com.sleepycat.je.rep.impl.networkRestore.Protocol.FileReq;
import com.sleepycat.je.rep.utilint.NamedChannel;
import com.sleepycat.je.rep.utilint.RepUtils;
import com.sleepycat.je.rep.utilint.BinaryProtocol.ClientVersion;
import com.sleepycat.je.rep.utilint.BinaryProtocol.ProtocolException;
import com.sleepycat.je.rep.vlsn.VLSNRange;
import com.sleepycat.je.util.DbBackup;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.je.utilint.StoppableThread;
import com.sleepycat.je.utilint.VLSN;

/**
 * The LogFileFeeder supplies log files to a client. There is one instance of
 * this class per client that's currently active. LogFileFeeders are created by
 * the FeederManager and exist for the duration of the session with the client.
 */
public class LogFileFeeder extends StoppableThread {

    /**
     * Time to wait for the next request from the client, 5 minutes.
     */
    private static final int SOCKET_TIMEOUT_MS = 5 * 60 * 1000;

    static final int TRANSFER_BYTES = 0x2000;

    private final FeederManager feederManager;

    private final NamedChannel namedChannel;

    private int clientId;

    private DbBackup dbBackup = null;

    final MessageDigest messageDigest;

    private final Logger logger;

    public LogFileFeeder(FeederManager feederManager, SocketChannel channel) throws DatabaseException {
        super(feederManager.getEnvImpl());
        this.feederManager = feederManager;
        logger = feederManager.logger;
        this.namedChannel = new NamedChannel(channel, feederManager.nameIdPair);
        try {
            messageDigest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            LoggerUtils.severe(logger, feederManager.getEnvImpl(), "The SHA1 algorithm was not made available " + "by the security provider");
            throw EnvironmentFailureException.unexpectedException(e);
        }
    }

    public void shutdown() {
        if (shutdownDone()) {
            return;
        }
        shutdownThread(logger);
        feederManager.feeders.remove(clientId);
        LoggerUtils.info(logger, feederManager.getEnvImpl(), "Log file feeder for client:" + clientId + " is shutdown.");
    }

    @Override
    protected int initiateSoftShutdown() {
        RepUtils.shutdownChannel(namedChannel);
        return SOCKET_TIMEOUT_MS;
    }

    /**
     * The main driver loop that enforces the protocol message sequence and
     * implements it.
     */
    @Override
    public void run() {
        Protocol protocol = new Protocol(feederManager.nameIdPair, Protocol.VERSION, feederManager.getEnvImpl());
        try {
            configureChannel();
            protocol = checkProtocol(protocol);
            checkFeeder(protocol);
            sendFileList(protocol);
            sendRequestedFiles(protocol);
            dbBackup.endBackup();
            dbBackup = null;
        } catch (ClosedByInterruptException e) {
            LoggerUtils.fine(logger, feederManager.getEnvImpl(), "Ignoring ClosedByInterruptException normal shutdown");
        } catch (IOException e) {
            LoggerUtils.warning(logger, feederManager.getEnvImpl(), " IO Exception: " + e.getMessage());
        } catch (ProtocolException e) {
            LoggerUtils.severe(logger, feederManager.getEnvImpl(), " Protocol Exception: " + e.getMessage());
        } catch (Exception e) {
            throw new EnvironmentFailureException(feederManager.getEnvImpl(), EnvironmentFailureReason.UNCAUGHT_EXCEPTION, e);
        } finally {
            try {
                namedChannel.getChannel().close();
            } catch (IOException e) {
                LoggerUtils.warning(logger, feederManager.getEnvImpl(), "Log File feeder io exception on " + "channel close: " + e.getMessage());
            }
            shutdown();
            if (dbBackup != null) {
                if (feederManager.shutdown.get()) {
                    dbBackup.endBackup();
                } else {
                    feederManager.new Lease(clientId, feederManager.leaseDuration, dbBackup);
                    LoggerUtils.info(logger, feederManager.getEnvImpl(), "Lease created for node: " + clientId);
                }
            }
            LoggerUtils.info(logger, feederManager.getEnvImpl(), "Log file feeder for client: " + clientId + " exited");
        }
    }

    /**
     * Implements the message exchange used to determine whether this feeder
     * is suitable for use the client's backup needs. The feeder may be
     * unsuitable if it's already busy, or it's not current enough to service
     * the client's needs.
     */
    private void checkFeeder(Protocol protocol) throws IOException, DatabaseException {
        protocol.read(namedChannel.getChannel(), FeederInfoReq.class);
        int feeders = feederManager.getActiveFeederCount() - 1;
        VLSN rangeFirst = VLSN.NULL_VLSN;
        VLSN rangeLast = VLSN.NULL_VLSN;
        if (feederManager.getEnvImpl() instanceof RepImpl) {
            RepImpl repImpl = (RepImpl) feederManager.getEnvImpl();
            feeders += repImpl.getRepNode().feederManager().activeReplicaCount();
            VLSNRange range = repImpl.getVLSNIndex().getRange();
            rangeFirst = range.getFirst();
            rangeLast = range.getLast();
        }
        protocol.write(protocol.new FeederInfoResp(feeders, rangeFirst, rangeLast), namedChannel);
    }

    /**
     * Send files in response to request messages. The request sequence looks
     * like the following:
     *
     *  [FileReq | StatReq]+ Done
     *
     * The response sequence to a FileReq looks like:
     *
     *  FileStart <file byte stream> FileEnd
     *
     *  and that for a StatReq, is simply a StatResp
     */
    private void sendRequestedFiles(Protocol protocol) throws IOException, ProtocolException, DatabaseException {
        File envDir = feederManager.getEnvImpl().getEnvironmentHome();
        try {
            while (true) {
                FileReq fileReq = protocol.read(namedChannel.getChannel(), FileReq.class);
                final String fileName = fileReq.getFileName();
                File file = new File(envDir, fileName);
                if (!file.exists()) {
                    throw EnvironmentFailureException.unexpectedState("Log file not found: " + fileName);
                }
                final long length = file.length();
                final long lastModified = file.lastModified();
                byte digest[] = null;
                FileInfoResp resp = null;
                Protocol.FileInfoResp cachedResp = feederManager.statResponses.get(fileName);
                byte cachedDigest[] = ((cachedResp != null) && (cachedResp.getFileLength() == length) && (cachedResp.getLastModifiedTime() == lastModified)) ? cachedResp.getDigestSHA1() : null;
                if (fileReq instanceof FileInfoReq) {
                    if (cachedDigest != null) {
                        digest = cachedDigest;
                    } else if (((FileInfoReq) fileReq).getNeedSHA1()) {
                        digest = getSHA1Digest(file, length).digest();
                    } else {
                        digest = new byte[0];
                    }
                    resp = protocol.new FileInfoResp(fileName, length, lastModified, digest);
                } else {
                    protocol.write(protocol.new FileStart(fileName, length, lastModified), namedChannel);
                    digest = sendFileContents(file, length);
                    if ((cachedDigest != null) && !Arrays.equals(cachedDigest, digest)) {
                        throw EnvironmentFailureException.unexpectedState("Inconsistent cached and computed digests");
                    }
                    resp = protocol.new FileEnd(fileName, length, lastModified, digest);
                }
                if (digest.length > 0) {
                    feederManager.statResponses.put(fileName, resp);
                }
                protocol.write(resp, namedChannel);
            }
        } catch (ProtocolException pe) {
            if (pe.getUnexpectedMessage() instanceof Protocol.Done) {
                return;
            }
            throw pe;
        }
    }

    /**
     * Returns the SHA1 has associated with the file.
     *
     * @param file
     * @param length
     * @return
     * @throws IOException
     * @throws DatabaseException
     */
    static MessageDigest getSHA1Digest(File file, long length) throws IOException, DatabaseException {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw EnvironmentFailureException.unexpectedException(e);
        }
        final FileInputStream fileStream = new FileInputStream(file);
        try {
            ByteBuffer buffer = ByteBuffer.allocate(TRANSFER_BYTES);
            for (long bytes = length; bytes > 0; ) {
                int readSize = (int) Math.min(TRANSFER_BYTES, bytes);
                int readBytes = fileStream.read(buffer.array(), 0, readSize);
                if (readBytes == -1) {
                    throw new IOException("Premature EOF. Was expecting: " + readSize);
                }
                messageDigest.update(buffer.array(), 0, readBytes);
                bytes -= readBytes;
            }
        } finally {
            fileStream.close();
        }
        return messageDigest;
    }

    /**
     * Sends over the contents of the file and computes the SHA-1 hash. Note
     * that the method does not rely on EOF detection, but rather on the
     * promised file size, since the final log file might be growing while the
     * transfer is in progress. The client uses the length sent in the FileResp
     * message to maintain its position in the network stream. It expects to
     * see a StatResp once it has read the agreed upon number of bytes.
     *
     * Since JE log files are append only, there is no danger that we will send
     * over any uninitialized file blocks.
     *
     * @param file the log file to be sent.
     * @param the number of bytes to send
     * @return the digest associated with the file that was sent
     *
     * @throws IOException
     */
    private byte[] sendFileContents(File file, long length) throws IOException {
        final FileInputStream fileStream = new FileInputStream(file);
        messageDigest.reset();
        try {
            ByteBuffer buffer = ByteBuffer.allocate(TRANSFER_BYTES);
            for (long bytes = length; bytes > 0; ) {
                int readSize = (int) Math.min(TRANSFER_BYTES, bytes);
                int readBytes = fileStream.read(buffer.array(), 0, readSize);
                if (readBytes == -1) {
                    throw new IOException("Premature EOF. Was expecting: " + readSize);
                }
                bytes -= readBytes;
                buffer.position(0);
                buffer.limit(readBytes);
                buffer.mark();
                namedChannel.getChannel().write(buffer);
                buffer.reset();
                messageDigest.update(buffer);
            }
            LoggerUtils.fine(logger, feederManager.getEnvImpl(), "Sent file: " + file + " Length: " + length + " bytes");
        } finally {
            fileStream.close();
        }
        return messageDigest.digest();
    }

    /**
     * Processes the request for the list of files that constitute a valid
     * backup. If a leased DbBackup instance is available, it uses it,
     * otherwise it creates a new instance and uses it instead.
     *
     */
    private void sendFileList(Protocol protocol) throws IOException, ProtocolException, DatabaseException {
        protocol.read(namedChannel.getChannel(), Protocol.FileListReq.class);
        if (dbBackup == null) {
            dbBackup = new DbBackup(feederManager.getEnvImpl());
            dbBackup.startBackup();
        } else {
            feederManager.leaseRenewalCount++;
        }
        String[] files = dbBackup.getLogFilesInBackupSet();
        protocol.write(protocol.new FileListResp(files), namedChannel);
    }

    /**
     * Verify that the protocols are compatible, switch to a different protocol
     * version, if we need to.
     */
    private Protocol checkProtocol(Protocol protocol) throws IOException, ProtocolException {
        ClientVersion clientVersion = protocol.read(namedChannel.getChannel(), Protocol.ClientVersion.class);
        clientId = clientVersion.getNodeId();
        FeederManager.Lease lease = feederManager.leases.get(clientId);
        if (lease != null) {
            dbBackup = lease.terminate();
        }
        feederManager.feeders.put(clientId, this);
        if (clientVersion.getVersion() != protocol.getVersion()) {
            String message = "Client requested protocol version: " + clientVersion.getVersion() + " but the server version is " + protocol.getVersion();
            LoggerUtils.warning(logger, feederManager.getEnvImpl(), message);
        }
        protocol.write(protocol.new ServerVersion(), namedChannel);
        return protocol;
    }

    /**
     * Sets up the channel to facilitate efficient transfer of large log files.
     */
    private SocketChannel configureChannel() throws IOException {
        namedChannel.getChannel().configureBlocking(true);
        LoggerUtils.fine(logger, feederManager.getEnvImpl(), "Log File Feeder accepted connection from " + namedChannel);
        namedChannel.getChannel().socket().setSoTimeout(SOCKET_TIMEOUT_MS);
        namedChannel.getChannel().socket().setTcpNoDelay(false);
        return namedChannel.getChannel();
    }

    /**
     * @see StoppableThread#getLogger
     */
    @Override
    protected Logger getLogger() {
        return logger;
    }
}
