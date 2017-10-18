package net.sf.buildbox.worker.localjobs;

import java.io.File;
import java.io.FileFilter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;
import net.sf.buildbox.scheduler.model.WorkerJobParams;
import net.sf.buildbox.scheduler.model.WorkerJob;
import net.sf.buildbox.util.BbxStringUtils;
import net.sf.buildbox.util.PushingDirectoryTraversal;
import net.sf.buildbox.util.StreamingMultiDigester;
import net.sf.buildbox.worker.specialist.Specialist;
import net.sf.buildbox.worker.specialist.SpecialistBuilder;

public class DirChecksumSpecialistBuilder extends SpecialistBuilder {

    protected Specialist create(WorkerJob workerJob, WorkerJobParams workerJobParams) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final DirChecksumSpecialist instance = new DirChecksumSpecialist();
        instance.init(workerJob, workerJobParams);
        return instance;
    }
}

class DirChecksumSpecialist extends Specialist {

    protected boolean executeJob() throws Exception {
        final DirChecksumParams params = workerJobParams.viewAs(DirChecksumParams.class);
        final AtomicInteger fileCount = new AtomicInteger();
        final File dir = params.getDir();
        if (!dir.isDirectory()) {
            logger.debug("Not a directory: %s", dir);
            if (dir.exists()) {
                setFinalMessage("path exists but is not a directory: %s", dir);
                return false;
            }
            setFinalMessage("Skipping, directory does not exist: %s", dir);
            return true;
        }
        final PrintWriter pw = new PrintWriter(dir.getAbsolutePath() + ".txt");
        final PushingDirectoryTraversal t = new PushingDirectoryTraversal(new FileFilter() {

            private static final long LOGGING_INTERVAL = 25000;

            private long nextLogTime = System.currentTimeMillis() + LOGGING_INTERVAL;

            public boolean accept(File srcFile) {
                if (srcFile.isDirectory()) {
                    return true;
                }
                computeFile(srcFile);
                fileCount.incrementAndGet();
                updateProgress();
                return true;
            }

            private void computeFile(File srcFile) {
                try {
                    final MessageDigest digest = MessageDigest.getInstance(params.getAlgorithm());
                    StreamingMultiDigester.compute(srcFile, digest);
                    pw.println(String.format("%s %s", BbxStringUtils.hexEncodeBytes(digest.digest()), srcFile));
                } catch (Exception e) {
                    logger.debug(e, "failed to compute checksum for %s", srcFile);
                    pw.println(String.format("-------------------------------- %s", srcFile));
                }
                pw.flush();
            }

            private void updateProgress() {
                if (System.currentTimeMillis() > nextLogTime) {
                    nextLogTime = System.currentTimeMillis() + LOGGING_INTERVAL;
                    reportProgress(" ... computed %d files so far", fileCount.get());
                }
            }
        });
        t.setWantDirs(false);
        t.setWantFiles(true);
        final long startTime = System.currentTimeMillis();
        t.traverse(dir);
        pw.close();
        logger.debug("Computed checksum for %d files in %d millis", fileCount.get(), System.currentTimeMillis() - startTime);
        return true;
    }
}
