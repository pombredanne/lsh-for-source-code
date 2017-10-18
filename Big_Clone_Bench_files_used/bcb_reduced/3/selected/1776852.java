package com.android.sdklib.internal.repository;

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.AndroidVersion.AndroidVersionException;
import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;
import com.android.sdklib.repository.SdkRepository;
import org.w3c.dom.Node;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;

/**
 * Represents a sample XML node in an SDK repository.
 */
public class SamplePackage extends MinToolsPackage implements IPackageVersion, IMinApiLevelDependency, IMinToolsDependency {

    private static final String PROP_MIN_API_LEVEL = "Sample.MinApiLevel";

    /** The matching platform version. */
    private final AndroidVersion mVersion;

    /**
     * The minimal API level required by this extra package, if > 0,
     * or {@link #MIN_API_LEVEL_NOT_SPECIFIED} if there is no such requirement.
     */
    private final int mMinApiLevel;

    /**
     * Creates a new sample package from the attributes and elements of the given XML node.
     * <p/>
     * This constructor should throw an exception if the package cannot be created.
     */
    SamplePackage(RepoSource source, Node packageNode, Map<String, String> licenses) {
        super(source, packageNode, licenses);
        int apiLevel = XmlParserUtils.getXmlInt(packageNode, SdkRepository.NODE_API_LEVEL, 0);
        String codeName = XmlParserUtils.getXmlString(packageNode, SdkRepository.NODE_CODENAME);
        if (codeName.length() == 0) {
            codeName = null;
        }
        mVersion = new AndroidVersion(apiLevel, codeName);
        mMinApiLevel = XmlParserUtils.getXmlInt(packageNode, SdkRepository.NODE_MIN_API_LEVEL, MIN_API_LEVEL_NOT_SPECIFIED);
    }

    /**
     * Creates a new sample package based on an actual {@link IAndroidTarget} (which
     * must have {@link IAndroidTarget#isPlatform()} true) from the {@link SdkManager}.
     * <p/>
     * The target <em>must</em> have an existing sample directory that uses the /samples
     * root form rather than the old form where the samples dir was located under the
     * platform dir.
     * <p/>
     * This is used to list local SDK folders in which case there is one archive which
     * URL is the actual samples path location.
     * <p/>
     * By design, this creates a package with one and only one archive.
     */
    SamplePackage(IAndroidTarget target, Properties props) {
        super(null, props, 0, null, null, null, Os.ANY, Arch.ANY, target.getPath(IAndroidTarget.SAMPLES));
        mVersion = target.getVersion();
        mMinApiLevel = Integer.parseInt(getProperty(props, PROP_MIN_API_LEVEL, Integer.toString(MIN_API_LEVEL_NOT_SPECIFIED)));
    }

    /**
     * Creates a new sample package from an actual directory path and previously
     * saved properties.
     * <p/>
     * This is used to list local SDK folders in which case there is one archive which
     * URL is the actual samples path location.
     * <p/>
     * By design, this creates a package with one and only one archive.
     *
     * @throws AndroidVersionException if the {@link AndroidVersion} can't be restored
     *                                 from properties.
     */
    SamplePackage(String archiveOsPath, Properties props) throws AndroidVersionException {
        super(null, props, 0, null, null, null, Os.ANY, Arch.ANY, archiveOsPath);
        mVersion = new AndroidVersion(props);
        mMinApiLevel = Integer.parseInt(getProperty(props, PROP_MIN_API_LEVEL, Integer.toString(MIN_API_LEVEL_NOT_SPECIFIED)));
    }

    /**
     * Save the properties of the current packages in the given {@link Properties} object.
     * These properties will later be given to a constructor that takes a {@link Properties} object.
     */
    @Override
    void saveProperties(Properties props) {
        super.saveProperties(props);
        mVersion.saveProperties(props);
        if (getMinApiLevel() != MIN_API_LEVEL_NOT_SPECIFIED) {
            props.setProperty(PROP_MIN_API_LEVEL, Integer.toString(getMinApiLevel()));
        }
    }

    /**
     * Returns the minimal API level required by this extra package, if > 0,
     * or {@link #MIN_API_LEVEL_NOT_SPECIFIED} if there is no such requirement.
     */
    public int getMinApiLevel() {
        return mMinApiLevel;
    }

    /** Returns the matching platform version. */
    public AndroidVersion getVersion() {
        return mVersion;
    }

    /** Returns a short description for an {@link IDescription}. */
    @Override
    public String getShortDescription() {
        String s = String.format("Samples for SDK API %1$s%2$s, revision %3$d%4$s", mVersion.getApiString(), mVersion.isPreview() ? " Preview" : "", getRevision(), isObsolete() ? " (Obsolete)" : "");
        return s;
    }

    /**
     * Returns a long description for an {@link IDescription}.
     *
     * The long description is whatever the XML contains for the &lt;description&gt; field,
     * or the short description if the former is empty.
     */
    @Override
    public String getLongDescription() {
        String s = getDescription();
        if (s == null || s.length() == 0) {
            s = getShortDescription();
        }
        if (s.indexOf("revision") == -1) {
            s += String.format("\nRevision %1$d%2$s", getRevision(), isObsolete() ? " (Obsolete)" : "");
        }
        return s;
    }

    /**
     * Computes a potential installation folder if an archive of this package were
     * to be installed right away in the given SDK root.
     * <p/>
     * A sample package is typically installed in SDK/samples/android-"version".
     * However if we can find a different directory that already has this sample
     * version installed, we'll use that one.
     *
     * @param osSdkRoot The OS path of the SDK root folder.
     * @param suggestedDir A suggestion for the installation folder name, based on the root
     *                     folder used in the zip archive.
     * @param sdkManager An existing SDK manager to list current platforms and addons.
     * @return A new {@link File} corresponding to the directory to use to install this package.
     */
    @Override
    public File getInstallFolder(String osSdkRoot, String suggestedDir, SdkManager sdkManager) {
        File samplesRoot = new File(osSdkRoot, SdkConstants.FD_SAMPLES);
        for (IAndroidTarget target : sdkManager.getTargets()) {
            if (target.isPlatform() && target.getVersion().equals(mVersion)) {
                String p = target.getPath(IAndroidTarget.SAMPLES);
                File f = new File(p);
                if (f.isDirectory()) {
                    if (f.getParentFile().equals(samplesRoot)) {
                        return f;
                    }
                }
            }
        }
        File folder = new File(samplesRoot, String.format("android-%s", getVersion().getApiString()));
        for (int n = 1; folder.exists(); n++) {
            folder = new File(samplesRoot, String.format("android-%s_%d", getVersion().getApiString(), n));
        }
        return folder;
    }

    @Override
    public boolean sameItemAs(Package pkg) {
        if (pkg instanceof SamplePackage) {
            SamplePackage newPkg = (SamplePackage) pkg;
            return newPkg.getVersion().equals(this.getVersion());
        }
        return false;
    }

    /**
     * Makes sure the base /samples folder exists before installing.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean preInstallHook(Archive archive, ITaskMonitor monitor, String osSdkRoot, File installFolder) {
        File samplesRoot = new File(osSdkRoot, SdkConstants.FD_SAMPLES);
        if (!samplesRoot.isDirectory()) {
            samplesRoot.mkdir();
        }
        if (installFolder != null && installFolder.isDirectory()) {
            String storedHash = readContentHash(installFolder);
            if (storedHash != null && storedHash.length() > 0) {
                String currentHash = computeContentHash(installFolder);
                if (!storedHash.equals(currentHash)) {
                    String pkgName = archive.getParentPackage().getShortDescription();
                    String msg = String.format("-= Warning ! =-\n" + "You are about to replace the content of the folder:\n " + "  %1$s\n" + "by the new package:\n" + "  %2$s.\n" + "\n" + "However it seems that the content of the existing samples " + "has been modified since it was last installed. Are you sure " + "you want to DELETE the existing samples? This cannot be undone.\n" + "Please select YES to delete the existing sample and replace them " + "by the new ones.\n" + "Please select NO to skip this package. You can always install it later.", installFolder.getAbsolutePath(), pkgName);
                    return monitor.displayPrompt("SDK Manager: overwrite samples?", msg);
                }
            }
        }
        return super.preInstallHook(archive, monitor, osSdkRoot, installFolder);
    }

    /**
     * Computes a hash of the installed content (in case of successful install.)
     *
     * {@inheritDoc}
     */
    @Override
    public void postInstallHook(Archive archive, ITaskMonitor monitor, File installFolder) {
        super.postInstallHook(archive, monitor, installFolder);
        if (installFolder == null) {
            return;
        }
        String h = computeContentHash(installFolder);
        saveContentHash(installFolder, h);
    }

    /**
     * Reads the hash from the properties file, if it exists.
     * Returns null if something goes wrong, e.g. there's no property file or
     * it doesn't contain our hash. Returns an empty string if the hash wasn't
     * correctly computed last time by {@link #saveContentHash(File, String)}.
     */
    private String readContentHash(File folder) {
        Properties props = new Properties();
        FileInputStream fis = null;
        try {
            File f = new File(folder, SdkConstants.FN_CONTENT_HASH_PROP);
            if (f.isFile()) {
                fis = new FileInputStream(f);
                props.load(fis);
                return props.getProperty("content-hash", null);
            }
        } catch (Exception e) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    /**
     * Saves the hash using a properties file
     */
    private void saveContentHash(File folder, String hash) {
        Properties props = new Properties();
        props.setProperty("content-hash", hash == null ? "" : hash);
        FileOutputStream fos = null;
        try {
            File f = new File(folder, SdkConstants.FN_CONTENT_HASH_PROP);
            fos = new FileOutputStream(f);
            props.store(fos, "## Android - hash of this archive.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Computes a hash of the files names and sizes installed in the folder
     * using the SHA-1 digest.
     * Returns null if the digest algorithm is not available.
     */
    private String computeContentHash(File installFolder) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
        }
        if (md != null) {
            hashDirectoryContent(installFolder, md);
            return getDigestHexString(md);
        }
        return null;
    }

    /**
     * Computes a hash of the *content* of this directory. The hash only uses
     * the files names and the file sizes.
     */
    private void hashDirectoryContent(File folder, MessageDigest md) {
        if (folder == null || md == null || !folder.isDirectory()) {
            return;
        }
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                hashDirectoryContent(f, md);
            } else {
                String name = f.getName();
                if (name == null || SdkConstants.FN_CONTENT_HASH_PROP.equals(name)) {
                    continue;
                }
                try {
                    md.update(name.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                }
                try {
                    long len = f.length();
                    md.update((byte) (len & 0x0FF));
                    md.update((byte) ((len >> 8) & 0x0FF));
                    md.update((byte) ((len >> 16) & 0x0FF));
                    md.update((byte) ((len >> 24) & 0x0FF));
                } catch (SecurityException e) {
                }
            }
        }
    }

    /**
     * Returns a digest as an hex string.
     */
    private String getDigestHexString(MessageDigest digester) {
        byte[] digest = digester.digest();
        int n = digest.length;
        String hex = "0123456789abcdef";
        char[] hexDigest = new char[n * 2];
        for (int i = 0; i < n; i++) {
            int b = digest[i] & 0x0FF;
            hexDigest[i * 2 + 0] = hex.charAt(b >>> 4);
            hexDigest[i * 2 + 1] = hex.charAt(b & 0x0f);
        }
        return new String(hexDigest);
    }
}
