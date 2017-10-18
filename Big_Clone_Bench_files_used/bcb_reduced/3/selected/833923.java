package org.nightlabs.nightlybuild.formerscripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;
import org.nightlabs.nightlybuild.BuildException;
import org.nightlabs.nightlybuild.NightlyBuild;
import org.nightlabs.nightlybuild.VersionQualifier;
import org.nightlabs.nightlybuild.config.NightlyConfig;
import org.nightlabs.nightlybuild.mail.Mail;
import org.nightlabs.nightlybuild.tools.Logger;
import org.nightlabs.util.Utils;

/**
 * @author Marc Klinger - marc[at]nightlabs[dot]de
 */
public class NightlyScript {

    private static final String MD5_FILE_CHARSET = "UTF-8";

    private static final String VERSION_FILE_CHARSET = "UTF-8";

    private NightlyConfig config;

    private PrintStream logOut;

    private File workingDirBase;

    private File projectWorkingDir;

    private String product;

    private String productVersion;

    private String versionFilename;

    private boolean cleanWorkingDirBeforeBuild;

    private boolean cleanWorkingDirAfterBuild;

    private PrintStream oldLogOut = null;

    /**
	 * Target file base name => Source directories
	 */
    private Map<String, Collection<String>> publishPackages;

    /**
	 * Target file => Source file
	 */
    private Map<String, String> publishFiles;

    private File logFile;

    private File tarBallsPublishDir;

    private File modulesPublishDir;

    private File publishProductDeclarationDir;

    private File publishLogFile;

    private String versionFileContent;

    public NightlyScript() {
        this(new NightlyConfig());
    }

    public NightlyScript(String configFilename) {
        this(new NightlyConfig(configFilename));
    }

    private static Collection<Matcher> getMatches(Properties properties, Pattern pattern) {
        Collection<Matcher> matches = new ArrayList<Matcher>();
        for (Iterator<Object> iter = properties.keySet().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            Matcher m = pattern.matcher(key);
            if (m.matches()) matches.add(m);
        }
        return matches;
    }

    public NightlyScript(NightlyConfig config) {
        this.config = new NightlyConfig(config);
        workingDirBase = new File(config.get("workingDir"));
        product = config.get("product", "default");
        productVersion = config.get("productVersion", null);
        if (productVersion != null && productVersion.equalsIgnoreCase("HEAD")) productVersion = null;
        versionFilename = config.get("versionFilename", null);
        cleanWorkingDirBeforeBuild = config.getBoolean("cleanWorkingDirBeforeBuild");
        cleanWorkingDirAfterBuild = config.getBoolean("cleanWorkingDirAfterSuccessfulBuild");
        if (config.getBoolean("createVersionQualifier") && !config.exists("versionQualifier")) config.set("versionQualifier", VersionQualifier.create());
        Collection<Matcher> archiveMatches = getMatches(config.getConfig(), Pattern.compile("^publishPackage\\.([^.]+)\\.Archive$"));
        for (Matcher m : archiveMatches) {
            String key = m.group(0);
            String identifier = m.group(1);
            String archive = config.get(key);
            Collection<Matcher> dirMatches = getMatches(config.getConfig(), Pattern.compile("^publishPackage\\." + Pattern.quote(identifier) + "\\.Dir.*$"));
            Collection<String> dirs = new ArrayList<String>(dirMatches.size());
            for (Matcher m2 : dirMatches) dirs.add(config.get(m2.group(0)));
            if (publishPackages == null) publishPackages = new HashMap<String, Collection<String>>();
            Logger.debug("Have publish package: " + dirs + " => " + archive);
            publishPackages.put(archive, dirs);
        }
        Collection<Matcher> sourceMatches = getMatches(config.getConfig(), Pattern.compile("^publishFile\\.([^.]+)\\.Source$"));
        for (Matcher m : sourceMatches) {
            String key = m.group(0);
            String identifier = m.group(1);
            String source = config.get(key);
            String targetKey = "publishFile." + identifier + ".Target";
            String target = config.getConfig().getProperty(targetKey);
            if (target == null) {
                Logger.error("No target found for publishFile " + key + ". Missing property: " + targetKey);
                continue;
            }
            if (publishFiles == null) publishFiles = new HashMap<String, String>();
            Logger.debug("Have publish file: " + source + " => " + target);
            publishFiles.put(target, source);
        }
    }

    public void run() throws BuildException {
        Logger.info("Starting nightly build for product " + product + "...");
        try {
            projectWorkingDir = new File(addProductSubDirs(workingDirBase.getAbsolutePath()));
            if (!projectWorkingDir.exists()) projectWorkingDir.mkdirs();
            if (cleanWorkingDirBeforeBuild) cleanWorkingDirBeforeBuild();
            initialize();
            build();
            publishTarBalls();
            publishMetaData();
            if (cleanWorkingDirAfterBuild) cleanWorkingDirAfterSuccessfulBuild();
        } catch (Throwable e) {
            Logger.error("Error in " + getClass().getSimpleName(), e);
            sendErrorMail(e);
            if (e instanceof BuildException) throw (BuildException) e; else throw new RuntimeException("Error in " + getClass().getSimpleName(), e);
        } finally {
            try {
                publishLog();
            } catch (Exception e) {
                Logger.error("Publishing log failed", e);
            }
            Logger.debug("Nightly build for product " + product + " done.");
            logOut.close();
            if (oldLogOut != null) Logger.setOut(oldLogOut);
        }
    }

    /**
	 * Delete the workingDir for the current version.
	 */
    private void cleanWorkingDirBeforeBuild() {
        Logger.debug("Cleaning up working dir (before build)...");
        Utils.deleteDirectoryRecursively(projectWorkingDir);
    }

    /**
	 * Delete the workingDir for the current version.
	 */
    private void cleanWorkingDirAfterSuccessfulBuild() {
        Logger.debug("Cleaning up working dir (after successful build)...");
        Utils.deleteDirectoryRecursively(projectWorkingDir);
    }

    private String addProductSubDirs(String dir) {
        return addProductSubDirs(new File(dir)).getAbsolutePath();
    }

    private File addProductSubDirs(File dir) {
        return Utils.getFile(dir, product, (this.productVersion == null ? "HEAD" : this.productVersion));
    }

    protected void initialize() throws FileNotFoundException {
        Logger.debug("initializing...");
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String logFilename = "build-" + today + "." + this.product + "-" + (this.productVersion == null ? "HEAD" : this.productVersion) + ".log";
        logFile = new File(workingDirBase, logFilename);
        Logger.info("Logging to file: " + logFile.getAbsolutePath());
        oldLogOut = Logger.getOut();
        logOut = new PrintStream(logFile);
        Logger.setOut(logOut);
        File tmp = Utils.getFile(config.getFile("publishBaseDir"), config.get("publishBuildSubDir"), product);
        if (productVersion != null) this.tarBallsPublishDir = new File(tmp, productVersion); else this.tarBallsPublishDir = Utils.getFile(tmp, "HEAD", today);
        tarBallsPublishDir.mkdirs();
        modulesPublishDir = addProductSubDirs(new File(config.get("publishBaseDir"), config.get("publishModuleSubDir")));
        modulesPublishDir.mkdirs();
        publishLogFile = Utils.getFile(config.getFile("publishBaseDir"), config.get("publishLogSubDir"), logFilename);
        publishLogFile.getParentFile().mkdirs();
        publishProductDeclarationDir = addProductSubDirs(new File(config.get("publishBaseDir"), config.get("publishProductDeclarationSubDir")));
        publishProductDeclarationDir.mkdirs();
        versionFileContent = product + " - " + (productVersion == null ? "HEAD" : productVersion) + " - " + today;
        config.set("workingDir", projectWorkingDir.getAbsolutePath());
        config.set("publishDir", modulesPublishDir.getAbsolutePath());
        String nightlyXMLDir = config.get("product.writeNightlyXml.dir", null);
        if (nightlyXMLDir != null) {
            config.set("product.writeNightlyXml.dir", addProductSubDirs(nightlyXMLDir));
        }
        config.set("j2ee.deploy.dir", new File(projectWorkingDir, config.get("j2ee.deploy.subdir")).getAbsolutePath());
        config.set("rcp.deploy.allplatforms.dir", new File(projectWorkingDir, config.get("rcp.deploy.allplatforms.subdir")).getAbsolutePath());
        config.set("rcp.deploy.win32.dir", new File(projectWorkingDir, config.get("rcp.deploy.win32.subdir")).getAbsolutePath());
        config.set("rcp.deploy.linux-gtk32.dir", new File(projectWorkingDir, config.get("rcp.deploy.linux-gtk32.subdir")).getAbsolutePath());
        config.set("rcp.deploy.linux-gtk64.dir", new File(projectWorkingDir, config.get("rcp.deploy.linux-gtk64.subdir")).getAbsolutePath());
        if (Boolean.parseBoolean(config.get("product.writeNightlyXml.enable", "false"))) config.set("product.writeNightlyXml.dir", addProductSubDirs(new File(config.get("publishBaseDir"), config.get("product.writeNightlyXml.subDir"))).getAbsolutePath());
        config.set("cleanWorkingDirAfterSuccessfulBuild", "false");
        config.set("cleanWorkingDirBeforeBuild", "false");
        config.set("cleanWorkingDirAfterSuccessfulBuild", "false");
        Logger.debug("initializing done.");
    }

    protected void build() throws BuildException {
        NightlyBuild nightlyBuild = new NightlyBuild(config);
        nightlyBuild.run();
    }

    protected void publishTarBalls() throws Exception {
        Logger.info("Publishing tar balls...");
        if (publishPackages != null) {
            for (Map.Entry<String, Collection<String>> packageEntry : publishPackages.entrySet()) {
                String targetBaseName = packageEntry.getKey();
                File tmpDir = Utils.createUniqueIncrementalFolder(Utils.getTempDir(), ".nightlytmp");
                Collection<String> dirs = packageEntry.getValue();
                for (String dir : dirs) {
                    File source = new File(projectWorkingDir, dir);
                    if (!source.exists()) {
                        Logger.warning("PublishPackage " + source + " does not exist.");
                        continue;
                    }
                    File target = new File(tmpDir, source.getName());
                    Utils.copyDirectory(source, target);
                    createVersionFile(target);
                }
                if (tmpDir.list().length > 0) {
                    if (Boolean.parseBoolean(config.get("enableZip"))) createMD5File(packZip(tmpDir, targetBaseName));
                    if (Boolean.parseBoolean(config.get("enableTar"))) createMD5File(packTar(tmpDir, targetBaseName));
                    if (Boolean.parseBoolean(config.get("enableGZ"))) createMD5File(packGz(tmpDir, targetBaseName));
                    if (Boolean.parseBoolean(config.get("enableBZ2"))) createMD5File(packBz2(tmpDir, targetBaseName));
                }
                Utils.deleteDirectoryRecursively(tmpDir);
            }
        }
        if (publishFiles != null) {
            for (Map.Entry<String, String> fileEntry : publishFiles.entrySet()) {
                File source = new File(projectWorkingDir, fileEntry.getValue());
                File target = new File(tarBallsPublishDir, fileEntry.getKey());
                if (!source.exists()) Logger.warning("Source does not exist: " + source.getAbsolutePath()); else {
                    target.getParentFile().mkdirs();
                    Utils.copyFile(source, target);
                    createMD5File(target);
                }
            }
        }
        if (productVersion == null) {
            Logger.debug("Copying tar balls to latest directory...");
            File latestDir = new File(tarBallsPublishDir.getParentFile(), "latest");
            if (latestDir.exists() && latestDir.isDirectory()) Utils.deleteDirectoryRecursively(latestDir);
            Utils.copyDirectory(tarBallsPublishDir, latestDir);
        }
        Logger.debug("Publishing balls done.");
    }

    private File createMD5File(File sourceFile) throws NoSuchAlgorithmException, IOException {
        if (sourceFile == null) return null;
        Logger.debug("Creating MD5 file for " + sourceFile + "...");
        try {
            String hashword = getMD5Sum(sourceFile);
            File md5File = new File(sourceFile.getAbsolutePath() + ".md5");
            FileOutputStream out = new FileOutputStream(md5File);
            out.write(hashword.getBytes(MD5_FILE_CHARSET));
            out.write(("  " + sourceFile.getName() + "\n").getBytes(MD5_FILE_CHARSET));
            out.close();
            return md5File;
        } finally {
            Logger.debug("Creating MD5 file for " + sourceFile + " done.");
        }
    }

    private String getMD5Sum(File sourceFile) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(sourceFile);
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) complete.update(buffer, 0, numRead);
        } while (numRead != -1);
        fis.close();
        byte[] b = complete.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            sb.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        while (sb.length() < 32) sb.insert(0, '0');
        return sb.toString();
    }

    private File packZip(File sourceParent, String targetBaseName) throws IOException {
        Logger.debug("Creating zip file for all files in " + sourceParent + "...");
        try {
            File zipFile = new File(tarBallsPublishDir, targetBaseName + ".zip");
            zipFile.getParentFile().mkdirs();
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
            Utils.zipFilesRecursively(out, null, sourceParent.listFiles(), sourceParent);
            out.close();
            return zipFile;
        } finally {
            Logger.debug("Creating zip file for all files in " + sourceParent + " done.");
        }
    }

    private File packGz(File sourceParent, String targetBaseName) throws IOException, InterruptedException {
        return packWithTar(sourceParent, targetBaseName, "-z", ".gz");
    }

    private File packBz2(File sourceParent, String targetBaseName) throws IOException, InterruptedException {
        return packWithTar(sourceParent, targetBaseName, "-j", ".bz2");
    }

    private File packTar(File sourceParent, String targetBaseName) throws IOException, InterruptedException {
        return packWithTar(sourceParent, targetBaseName, null, null);
    }

    private File packWithTar(File sourceParent, String targetBaseName, String additionalOption, String additionalFileSuffix) throws IOException, InterruptedException {
        String tarBin = config.get("tar.bin", null);
        if (tarBin == null) return null;
        Logger.debug("Creating tar file for all files in " + sourceParent + "...");
        try {
            String targetFilename = targetBaseName + ".tar";
            if (additionalFileSuffix != null) targetFilename += additionalFileSuffix;
            File targetFile = new File(tarBallsPublishDir, targetFilename);
            targetFile.getParentFile().mkdirs();
            ArrayList<String> cmd = new ArrayList<String>();
            boolean haveAdditionalOption = additionalOption != null && !additionalOption.equals("");
            cmd.add(tarBin);
            if (haveAdditionalOption) cmd.add(additionalOption);
            cmd.add("-cf");
            cmd.add(targetFile.getAbsolutePath());
            cmd.add("-C");
            cmd.add(sourceParent.getAbsolutePath());
            for (File f : sourceParent.listFiles()) cmd.add(f.getName());
            Process p = Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));
            int ret = p.waitFor();
            if (ret != 0) throw new RuntimeException("tar failed: return value: " + ret);
            if (!targetFile.exists()) throw new RuntimeException("tar failed: target file does not exist: " + targetFile.getAbsolutePath());
            return targetFile;
        } finally {
            Logger.debug("Creating tar file for all files in " + sourceParent + " done.");
        }
    }

    private void createVersionFile(File packageSource) throws UnsupportedEncodingException, IOException {
        if (versionFilename != null) {
            Logger.debug("Creating version file for " + packageSource + "...");
            File versionFile = new File(packageSource, versionFilename);
            FileOutputStream out = new FileOutputStream(versionFile);
            out.write(versionFileContent.getBytes(VERSION_FILE_CHARSET));
            out.close();
            Logger.debug("Creating version file for " + packageSource + " done.");
        }
    }

    private void publishLog() throws IOException {
        publishLogFile.getParentFile().mkdirs();
        Utils.copyFile(logFile, publishLogFile);
    }

    private void publishMetaDataFile(String filenameConfigEntry) throws IOException {
        if (config.exists(filenameConfigEntry)) {
            File f = new File(projectWorkingDir, config.get(filenameConfigEntry));
            if (f.exists()) Utils.copyFile(f, new File(publishProductDeclarationDir, config.get(filenameConfigEntry)));
        }
    }

    private void publishMetaData() throws IOException {
        publishMetaDataFile("classpathEntryByPathFile");
        publishMetaDataFile("builtProvidesFile");
        publishMetaDataFile("buildOrderFile");
        publishMetaDataFile("buildDependenciesFile");
        publishMetaDataFile("buildProvidesFile");
        publishMetaDataFile("javadocWarningsFile");
    }

    private void sendErrorMail(Throwable e) {
        Logger.info("Sending error mail...");
        try {
            StringBuffer err = new StringBuffer();
            err.append("NightlyBuild failed:\n");
            err.append("\tDate: ");
            err.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date()));
            err.append("\n");
            String productOverview = "";
            String product = config.get("product", null);
            if (product != null) {
                productOverview += product;
                err.append("\tproduct: ");
                err.append(product);
                err.append("\n");
            }
            String productVersion = config.get("productVersion", null);
            if (productVersion != null) {
                productOverview += " " + productVersion;
                err.append("\tproductVersion: ");
                err.append(productVersion);
                err.append("\n");
            }
            if (e instanceof BuildException) {
                BuildException be = (BuildException) e;
                String projectName = be.getProjectName();
                if (projectName != null) {
                    err.append("\tproject: ");
                    err.append(projectName);
                    err.append("\n");
                }
            }
            err.append("\n");
            err.append("\n");
            err.append(Utils.getStackTraceAsString(e));
            if (!"".equals(productOverview)) productOverview = " [" + productOverview + "]";
            new Mail(config).send("NightlyBuild failed" + productOverview, err.toString());
            Logger.debug("Sending error mail done.");
        } catch (Throwable e1) {
            Logger.error("Sending error mail failed", e1);
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                if (args[0].equals("--help") || args[0].equals("-help") || args[0].equals("-h") || args[0].equals("-?")) {
                    System.out.println("Usage: java " + NightlyScript.class.getName() + " [config file]");
                    System.exit(0);
                }
                new NightlyScript(args[0]).run();
            } else new NightlyScript().run();
        } catch (BuildException e) {
            Logger.error("Build failed", e);
            System.exit(1);
        }
    }
}
