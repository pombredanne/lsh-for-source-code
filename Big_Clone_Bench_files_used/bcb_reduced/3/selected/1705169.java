package org.metastatic.rsync.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.ChecksumPair;
import org.metastatic.rsync.Delta;
import org.metastatic.rsync.DeltaEncoder;
import org.metastatic.rsync.ListenerException;
import org.metastatic.rsync.MatcherEvent;
import org.metastatic.rsync.MatcherListener;
import org.metastatic.rsync.MatcherStream;
import org.metastatic.rsync.Offsets;
import org.metastatic.rsync.Util;
import org.apache.log4j.Logger;

public class Sender implements Constants, MatcherListener {

    private static Logger logger = Logger.getLogger(Sender.class.getName());

    private final Configuration config;

    private final MultiplexedInputStream in;

    private final MultiplexedOutputStream out;

    private Statistics stats;

    private final int remoteVersion;

    private DeltaEncoder deltasOut;

    private int count, n, remainder;

    /**
   * Create a new Sender object.
   *
   * @param in The underlying input stream.
   * @param out The underlying output stream.
   * @param config The configuration to use.
   * @param remoteVersion The remote protocol version.
   * @param amServer Should be true if we are the server.
   */
    public Sender(MultiplexedInputStream in, MultiplexedOutputStream out, Configuration config, int remoteVersion, boolean amServer) {
        this.in = in;
        this.out = out;
        if (amServer) logger.addAppender(new RsyncAppender(this.out));
        this.config = config;
        this.remoteVersion = remoteVersion;
        stats = new Statistics();
    }

    public void setStatistics(Statistics newStats) {
        if (newStats != null) stats = newStats;
    }

    public Statistics getStatistics() {
        return stats;
    }

    /**
   * Send the set of files.
   */
    public void sendFiles(List files) throws IOException {
        logger.debug("sendFiles starting");
        int i;
        int phase = 0;
        while (true) {
            int offset = 0;
            i = in.readInt();
            logger.debug("read file index " + i);
            if (i == -1) {
                if (phase == 0 && remoteVersion >= 13) {
                    phase++;
                    config.strongSumLength = SUM_LENGTH;
                    out.writeInt(-1);
                    out.flush();
                    logger.debug("sendFiles phase=" + phase);
                    continue;
                }
                break;
            }
            if (i < 0 || i >= files.size()) {
                String msg = "invalid file index " + i + " (count=" + files.size() + ")";
                logger.fatal(msg);
                throw new IOException(msg);
            }
            FileInfo finfo = (FileInfo) files.get(i);
            File file = new File(finfo.filename());
            stats.num_transferred_files++;
            stats.total_transferred_size += file.length();
            if (phase == 0) stats.total_size += file.length();
            logger.info(finfo.filename());
            List sums = receiveSums();
            out.writeInt(i);
            out.writeInt(count);
            out.writeInt(n);
            out.writeInt(remainder);
            out.flush();
            config.blockLength = n;
            MatcherStream match = new MatcherStream(config);
            match.setChecksums(sums);
            match.addListener(this);
            deltasOut = new PlainDeltaEncoder(config, out);
            DigestInputStream fin = null;
            try {
                MessageDigest md = MessageDigest.getInstance("BrokenMD4");
                md.update(config.checksumSeed);
                byte[] buf = new byte[CHUNK_SIZE];
                fin = new DigestInputStream(new FileInputStream(file), md);
                int len;
                while ((len = fin.read(buf)) != -1) {
                    logger.debug("updating matcher with " + len + " bytes");
                    match.update(buf, 0, len);
                    out.flush();
                }
                match.doFinal();
                deltasOut.doFinal();
                byte[] digest = md.digest();
                logger.debug("file_sum=" + Util.toHexString(digest));
                out.write(digest);
                out.flush();
                fin.close();
            } catch (ListenerException le) {
                throw (IOException) le.getCause();
            } catch (NoSuchAlgorithmException nsae) {
                throw new IOException("could not create message digest");
            }
        }
        out.writeInt(-1);
        out.flush();
        logger.debug("sendFiles finished");
    }

    public void update(MatcherEvent e) throws ListenerException {
        try {
            logger.debug("matched delta=" + e.getDelta());
            Delta d = e.getDelta();
            if (d instanceof Offsets) stats.matched_data += d.getBlockLength(); else stats.literal_data += d.getBlockLength();
            deltasOut.write(d);
        } catch (IOException ioe) {
            throw new ListenerException(ioe);
        }
    }

    private List receiveSums() throws IOException {
        count = in.readInt();
        n = in.readInt();
        remainder = in.readInt();
        long offset = 0;
        logger.debug("count=" + count + " n=" + n + " rem=" + remainder);
        if (count < 0) {
            throw new IOException("bad sum count " + count);
        }
        if (count == 0) return null;
        List sums = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            int weak = in.readInt();
            byte[] strong = new byte[config.strongSumLength];
            in.read(strong);
            ChecksumPair pair = null;
            if (i == count - 1 && remainder > 0) {
                pair = new ChecksumPair(weak, strong, offset, remainder, i);
                offset += remainder;
            } else {
                pair = new ChecksumPair(weak, strong, offset, n, i);
                offset += n;
            }
            logger.debug("chunk[" + i + "] " + pair);
            sums.add(pair);
        }
        return sums;
    }
}
