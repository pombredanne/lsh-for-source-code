package phex.share;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import phex.common.AltLocContainer;
import phex.common.URN;
import phex.common.log.NLogger;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.prefs.core.LibraryPrefs;
import phex.servent.Servent;
import phex.utils.IOUtil;
import com.bitzi.util.Base32;
import com.bitzi.util.SHA1;

class UrnCalculationWorker implements Runnable {

    private final SwarmingManager downloadService;

    private final SharedFilesService sharedFilesService;

    private final ShareFile shareFile;

    UrnCalculationWorker(ShareFile shareFile, SharedFilesService sharedFilesService) {
        this.shareFile = shareFile;
        this.sharedFilesService = sharedFilesService;
        this.downloadService = Servent.getInstance().getDownloadService();
    }

    public void run() {
        boolean succ = calculateURN();
        if (succ) {
            sharedFilesService.addUrn2FileMapping(shareFile);
            sharedFilesService.triggerSaveSharedFiles();
        }
    }

    /**
     * Calculates the URN of the file for HUGE support. This method can take
     * some time for large files. For URN calculation a SHA-1 digest is created
     * over the complete file and the SHA-1 digest is translated into a Base32
     * representation.
     */
    private boolean calculateURN() {
        int urnCalculationMode = LibraryPrefs.UrnCalculationMode.get().intValue();
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(shareFile.getSystemFile());
            MessageDigest messageDigest = new SHA1();
            byte[] buffer = new byte[64 * 1024];
            int length;
            long start = System.currentTimeMillis();
            long start2 = System.currentTimeMillis();
            while ((length = inStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, length);
                long end2 = System.currentTimeMillis();
                try {
                    Thread.sleep((end2 - start2) * urnCalculationMode);
                } catch (InterruptedException exp) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                start2 = System.currentTimeMillis();
            }
            inStream.close();
            byte[] shaDigest = messageDigest.digest();
            long end = System.currentTimeMillis();
            URN urn = new URN("urn:sha1:" + Base32.encode(shaDigest));
            shareFile.setURN(urn);
            if (NLogger.isDebugEnabled(UrnCalculationWorker.class)) {
                NLogger.debug(UrnCalculationWorker.class, "SHA1 time: " + (end - start) + " size: " + shareFile.getSystemFile().length());
            }
            SWDownloadFile file = downloadService.getDownloadFileByURN(urn);
            if (file != null) {
                AltLocContainer altCont = file.getGoodAltLocContainer();
                shareFile.getAltLocContainer().addContainer(altCont);
            }
            return true;
        } catch (IOException exp) {
            NLogger.debug(UrnCalculationWorker.class, exp, exp);
            return false;
        } finally {
            IOUtil.closeQuietly(inStream);
        }
    }
}
