package net.sf.karatasi.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import net.sf.karatasi.desktop.KaratasiDesktopPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

/** This class handles updating karatasi.java.
 *
 * @author <a href="mailto:cher@riedquat.de">Christian.Hujer</a>
 * @author <a href="mailto:kussinger@sourceforge.net">Mathias Kussinger</a>
 */
public class Updater implements Runnable, PreferenceChangeListener {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger("net.sf.japi.net.rest");

    /** The notification handler. If null no notifications are created. */
    private UpdaterNotificationHandlerInterface notificationHandler = null;

    /** Preferences. */
    @NotNull
    private final Preferences prefs;

    /** The versions file URL. */
    @NotNull
    private final String versionsFileUrl;

    /** The version data for the 'Java' module from the version file.
     * They have to be reloaded before every update run. */
    private Versions versionData;

    /** The directory for downloaded files. */
    @NotNull
    private final String downloadDirectoryName;

    /** The update thread. */
    @Nullable
    private Thread thread = null;

    /** The update period value.
     * <0 means never, 0 at every start of the program, >0 is the cycle time in ms.
     */
    private long updatePeriod = KaratasiDesktopPreferences.PREFS_DEFAULT_AUTO_UPDATE_INTERVALL;

    /** The version to check against for updates. Either the version of the binary, or a newer one if we did skip an update. */
    private VersionNumber referenceVersion = null;

    /** The last time of the last check for updates, in ms since 1/1/1970. */
    private long timeOfLastCheckForUpdates = 0;

    /** The name of the last successfully down-loaded file */
    private String lastSuccessfullDownloadFilename = null;

    /** The object to manage the sleeping. */
    private Object delayObject = null;

    /** Flag that signals to look for an update at start up. */
    private boolean checkForUpdateAtStart = false;

    /** Flag that signals the starting thread to look for an update immediately. */
    private boolean checkForUpdateNow = false;

    /** Flag to stop the thread. */
    private boolean stopThread = false;

    /** Flag to interrupt a download. */
    private boolean stopDownload = false;

    /** Size of the download buffer in Byte. */
    private int bufferSize = 4096;

    /** Construct an Updater.
     * The updater gets not started automatically, that's done by the start() method.
     * @param versionsFileUrl the url of the versions file
     * @param downloadDirectoryName the directory to store the downloaded file, will be created
     */
    public Updater(@NotNull final String versionsFileUrl, @NotNull final String downloadDirectoryName) {
        prefs = KaratasiDesktopPreferences.create();
        this.versionsFileUrl = versionsFileUrl;
        versionData = null;
        this.downloadDirectoryName = downloadDirectoryName;
        referenceVersion = calculateReferenceVersionNumber();
        updatePeriod = prefs.getLong(KaratasiDesktopPreferences.PREFS_KEY_AUTO_UPDATE_INTERVALL, KaratasiDesktopPreferences.PREFS_DEFAULT_AUTO_UPDATE_INTERVALL);
        timeOfLastCheckForUpdates = prefs.getLong(KaratasiDesktopPreferences.KEEP_KEY_AUTOUPDATE_LAST_CHECK, 0);
    }

    /** Setter for the update period cycle value.
     * @param newValue the new cycle value: <0 means never, 0 at every start of the program, >0 is the cycle time in ms.
     */
    public synchronized void setUpdatePeriode(final long newValue) {
        updatePeriod = newValue;
        interruptThread();
    }

    /** Setter for the notification handler.
     * @param handler the notification handler, or null
     */
    public synchronized void setNotificationHandler(final UpdaterNotificationHandlerInterface handler) {
        notificationHandler = handler;
    }

    /** Getter for the reference version number.
     * @return the new reference version
     */
    public synchronized VersionNumber getReferenceVersionNumber() {
        return referenceVersion;
    }

    /** Setter for the reference version number.
     * This method is mainly intended for testing.
     * The class determines the reference version by itself.
     * The new reference version is stored.
     * @param versionNumber the new reference version
     */
    public synchronized void setReferenceVersionNumber(@NotNull final VersionNumber versionNumber) {
        referenceVersion = versionNumber;
        prefs.put(KaratasiDesktopPreferences.KEEP_KEY_AUTOUPDATE_LAST_VERSION, referenceVersion.toString());
    }

    /** Setter for the size of the download buffer.
     * This method is mainly intended for testing, a small size reduces the download speed.
     * @param versionNumber the new reference version
     */
    public synchronized void setBufferSize(@NotNull final int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /** Getter for the time of the last update.
     * In ms since 1/1/1970.
     * @return the time of the last check
     */
    public synchronized long getTimeOfTheLastCheckForUpdates() {
        return timeOfLastCheckForUpdates;
    }

    /** Getter for the filename of the last file that was downloaded successfully.
     * @return filename of the last download, or null if no filename is available
     */
    public synchronized String getLastDownloadedFilename() {
        return lastSuccessfullDownloadFilename;
    }

    /** Start the updater.
     * Bind it to the gui component, and run the timed thread.
     */
    public synchronized void start() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this);
        thread.start();
    }

    /** {@inheritDoc} */
    public void run() {
        stopThread = false;
        delayObject = new Object();
        if (updatePeriod == 0) {
            checkForUpdateAtStart = true;
        } else {
            checkForUpdateAtStart = false;
        }
        while (!stopThread) {
            final long now = Calendar.getInstance().getTimeInMillis();
            try {
                stopDownload = false;
                if (!checkForUpdateAtStart && !checkForUpdateNow && !(updatePeriod > 0 && now >= timeOfLastCheckForUpdates + updatePeriod)) {
                    long delay = 0L;
                    if (updatePeriod > 0) {
                        delay = updatePeriod - (now - timeOfLastCheckForUpdates);
                        if (delay <= 0) {
                            delay = 1;
                        }
                    }
                    synchronized (delayObject) {
                        delayObject.wait(delay);
                    }
                    continue;
                }
                if (notificationHandler != null) {
                    notificationHandler.setVerbosity(checkForUpdateNow);
                }
                checkForUpdateNow = false;
                checkForUpdateAtStart = false;
                timeOfLastCheckForUpdates = now;
                prefs.putLong(KaratasiDesktopPreferences.KEEP_KEY_AUTOUPDATE_LAST_CHECK, timeOfLastCheckForUpdates);
                final String downloadedFilename = doCheckAndLoadUpdate(selectBundleType());
                stopDownload = false;
                if (downloadedFilename != null) {
                    lastSuccessfullDownloadFilename = downloadedFilename;
                }
            } catch (final InterruptedException e) {
                stopThread = true;
            }
            if (notificationHandler != null) {
                notificationHandler.setVerbosity(false);
            }
        }
        delayObject = null;
    }

    /** {@inheritDoc} */
    public synchronized void preferenceChange(final PreferenceChangeEvent evt) {
        if (KaratasiDesktopPreferences.PREFS_KEY_AUTO_UPDATE_INTERVALL.equals(evt.getKey())) {
            setUpdatePeriode(prefs.getLong(KaratasiDesktopPreferences.PREFS_KEY_AUTO_UPDATE_INTERVALL, KaratasiDesktopPreferences.PREFS_DEFAULT_AUTO_UPDATE_INTERVALL));
        }
    }

    /** Stop the thread now.
     * @throws InterruptedException
     */
    public synchronized void stopThreadNow() throws InterruptedException {
        stopThread = true;
        stopDownload = true;
        interruptThread();
        thread.join();
    }

    /** Check for updates now.
     */
    public synchronized void checkForUpdateNow() {
        checkForUpdateNow = true;
        stopDownload = true;
        interruptThread();
    }

    /** Interrupt the thread if it is running to check the flags and period value.
     */
    private void interruptThread() {
        if (delayObject != null) {
            synchronized (delayObject) {
                delayObject.notify();
            }
        }
    }

    /** Check for newer version and load it if available.
     * To be called from the update thread.
     * @param bundleType TODO
     * @return the filename of the bundle, or null if the operation fails
     * @throws InterruptedException
     */
    public String doCheckAndLoadUpdate(final String bundleType) throws InterruptedException {
        String downloadTargetFileName = null;
        try {
            LOG.log(Level.INFO, "updater: checking for updates");
            loadVersionData();
            if (stopDownload) {
                throw new InterruptedException("thead stop has been flagged");
            }
            final VersionNumber latestVersion = checkForNewerVersion(bundleType);
            if (latestVersion == null) {
                if (notificationHandler != null) {
                    notificationHandler.notifyNoUpdateAvailable();
                }
                return null;
            }
            final String description = versionData.getLaterDescriptions(getBundleVersion(), latestVersion, Locale.getDefault().getLanguage());
            LOG.log(Level.INFO, "updater: found later version " + latestVersion.toString());
            int answer = 2;
            if (notificationHandler != null) {
                answer = notificationHandler.notifyUpdateAvailable(latestVersion.toString(), description);
                if (answer == 0) {
                    referenceVersion = latestVersion;
                    prefs.put(KaratasiDesktopPreferences.KEEP_KEY_AUTOUPDATE_LAST_VERSION, referenceVersion.toString());
                }
            }
            if (answer != 2) {
                return null;
            }
            if (stopDownload) {
                throw new InterruptedException("thead stop has been flagged");
            }
            LOG.log(Level.INFO, "updater: preparing for download of version " + latestVersion.toString());
            prepareDownloadDirectory();
            if (stopDownload) {
                throw new InterruptedException("thead stop has been flagged");
            }
            downloadTargetFileName = downloadDirectoryName + File.separator + "karatasi-" + latestVersion.toString() + "." + bundleType;
            final Versions.Bundle bundle = versionData.getBundle(latestVersion, bundleType);
            downloadFile(bundle, downloadTargetFileName);
            if (stopDownload) {
                throw new InterruptedException("thead stop has been flagged");
            }
            LOG.log(Level.INFO, "updater: successfull download of version " + latestVersion.toString());
            if (notificationHandler != null) {
                notificationHandler.notifyDownloadCompleted(latestVersion.toString(), downloadTargetFileName, description);
            }
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "updater: checking for update failed: " + e.getMessage());
            if (notificationHandler != null) {
                notificationHandler.notifyException(e);
            }
            if (downloadTargetFileName != null) {
                new File(downloadTargetFileName).delete();
            }
            return null;
        }
        return downloadTargetFileName;
    }

    /** Load the version data from the versions URL of the instance.
     * @throws IOException
     * @throws SAXException
     */
    public void loadVersionData() throws SAXException, IOException {
        versionData = new Versions();
        final InputStream versionsFileStrem = getStreamFromVersionsFile(versionsFileUrl);
        versionData.read(versionsFileStrem, "Java");
    }

    /** Perform the check for an update.
     * To be called from the update thread.
     * @param versionData the version data for this module
     * @param referenceVersion the actual version number
     * @param bundleType the type string of the bundle
     * @return the version number of the latest version, or null if no newer version exists.
     */
    public VersionNumber checkForNewerVersion(final String bundleType) {
        final VersionNumber highestVersionNumber = versionData.highestVersionNumber(bundleType);
        if (highestVersionNumber != null && highestVersionNumber.compareTo(referenceVersion) > 0) {
            return highestVersionNumber;
        }
        return null;
    }

    /** Prepare the download directory.
     * @throws IOException
     */
    public void prepareDownloadDirectory() throws IOException {
        final File downloadDirectory = new File(downloadDirectoryName);
        downloadDirectory.getCanonicalFile();
        if (!downloadDirectory.exists()) {
            if (!downloadDirectory.mkdirs()) {
                throw new IOException("Creation of download directory \"" + downloadDirectoryName + "\" failed");
            }
        }
    }

    /** Download the update file and check hash if available.
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InterruptedException
     *
     */
    public void downloadFile(final Versions.Bundle updateFileBundle, final String targetFileName) throws NoSuchAlgorithmException, IOException, InterruptedException {
        final File targetFile = new File(targetFileName);
        MessageDigest md = null;
        String hash = null;
        if (updateFileBundle.getSha256Hash() != null) {
            md = MessageDigest.getInstance("SHA-256");
            hash = updateFileBundle.getSha256Hash();
        } else if (updateFileBundle.getMd5Hash() != null) {
            md = MessageDigest.getInstance("MD5");
            hash = updateFileBundle.getMd5Hash();
        }
        final URL sourceUrl = new URL(updateFileBundle.getUrl());
        sourceUrl.openConnection();
        final InputStream in = sourceUrl.openStream();
        final FileOutputStream out = new FileOutputStream(targetFile);
        final byte[] buffer = new byte[bufferSize];
        int bytesRead = 0;
        while ((bytesRead = in.read(buffer)) > 0) {
            if (stopDownload) {
                throw new InterruptedException("thead stop has been flagged");
            }
            if (md != null) {
                md.update(buffer, 0, bytesRead);
            }
            out.write(buffer, 0, bytesRead);
        }
        out.close();
        in.close();
        if (stopDownload) {
            throw new InterruptedException("thead stop has been flagged");
        }
        if (md != null && hash != null) {
            final byte[] mdbytes = md.digest();
            for (int i = 0; i < mdbytes.length; i++) {
                final String hex = hash.substring(2 * i, 2 * i + 2);
                final byte subVal = (byte) Integer.valueOf(hex, 16).intValue();
                if (mdbytes[i] != subVal) {
                    targetFile.delete();
                    throw new IOException("file from URL \"" + updateFileBundle.getUrl() + "\" has wrong hash");
                }
            }
        }
    }

    /** Prepare an input stream from the versions file.
     * This method handles site redirect (like from karatasi.org to karatasi.sourceforge.net).
     * @param versionsUrl the URL of the versions file
     * @return the inputStream input stream from the versions file
     * @throws IOException
     */
    private InputStream getStreamFromVersionsFile(@NotNull final String versionsUrl) throws IOException {
        URL inUrl = new URL(versionsUrl);
        if (!inUrl.getProtocol().equals("http")) {
            return inUrl.openStream();
        }
        final String urlFile = inUrl.getFile();
        HttpURLConnection connection = (HttpURLConnection) inUrl.openConnection();
        InputStream inputStream = connection.getInputStream();
        if (!connection.getURL().getFile().endsWith(urlFile)) {
            inputStream.close();
            inUrl = new URL(connection.getURL().toExternalForm() + urlFile);
            connection = (HttpURLConnection) inUrl.openConnection();
            inputStream = connection.getInputStream();
        }
        return inputStream;
    }

    /** Get the reference version number.
     * Either the version number of this binary, or if newer the stored reference version.
     * @return the reference version number.
     */
    private VersionNumber calculateReferenceVersionNumber() {
        final VersionNumber myVersion = getBundleVersion();
        final VersionNumber storedVersion = new VersionNumber(prefs.get(KaratasiDesktopPreferences.KEEP_KEY_AUTOUPDATE_LAST_VERSION, "0"));
        if (myVersion.compareTo(storedVersion) < 0) {
            return storedVersion;
        }
        return myVersion;
    }

    /** Read the version number of this bundle.
     * @return the version number of the bundle
     */
    private VersionNumber getBundleVersion() {
        String buildNumber = "0";
        try {
            final ResourceBundle bundle = ResourceBundle.getBundle("build");
            buildNumber = bundle.getString("build.number");
        } catch (final Exception ignore) {
            LOG.log(Level.WARNING, "updater: can't read version number");
        }
        LOG.log(Level.INFO, "this is build has version " + buildNumber);
        final String[] buildNumberComponents = buildNumber.split("[- ]+");
        VersionNumber versionNumber = null;
        try {
            versionNumber = new VersionNumber(buildNumberComponents[0]);
        } catch (final Exception ignore) {
            versionNumber = new VersionNumber("0");
        }
        return versionNumber;
    }

    /** Find the right bundle type for this environment.
     * @return the bundle type name
     */
    private String selectBundleType() {
        final String osName = System.getProperty("os.name");
        if ("Mac OS X".equals(osName)) {
            return "dmg";
        }
        if ("Windows".equals(osName)) {
            return "zip";
        }
        return "tgz";
    }
}
