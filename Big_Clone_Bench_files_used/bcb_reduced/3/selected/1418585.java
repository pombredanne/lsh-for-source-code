package net.sourceforge.jfilecrypt;

import net.sourceforge.jfilecrypt.algorithms.BlockCipherInputStream;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import net.sourceforge.jfilecrypt.algorithms.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.zip.*;

/**
 * The model contains all settings and all main functions to do the thing for
 * which this application was made. Namely it is encrypt and decrypt. The user
 * communicates with the model using a controller (which can use e.g. a GUI for
 * inputs). So this model is independed from specified controllers, it uses the
 * controller interface only.
 */
public class Model {

    private ResourceBundle bundle;

    private AlgorithmFactory algFactory;

    private Algorithm currentAlgorithm;

    private byte cmpLevel = 0;

    private FileList fileList = new FileList("");

    private String password = null;

    private String algorithmName = "Blowfish";

    private File targetArchive = null, targetDirectory = null;

    private long progLength = 0, progNow = 0;

    private int progOld = 0;

    private Thread runningThread = null;

    private int overwriteMode = 0;

    /**
     * Create model first, register this
     *
     * @see Model to a
     * @see Controller and the Controller to this Model after that.
     *
     * Register a
     * @see Controller before you do anything else!
     */
    protected Model() {
        bundle = Application.getResourceBundle();
        algFactory = AlgorithmFactory.getDefaultAlgorithmFactory();
    }

    /**
     * Returns an algorithm as Algorithm class given by its name. This is useful
     * if you want to get more information about a algorithm which is saved in
     * the Algorithm class.
     */
    protected Algorithm getAlgorithmByName(String algname) {
        return algFactory.getAlgorithm(algname);
    }

    /**
     * Set the compression level. Does not matter if useZip is set or not.
     */
    protected void setCompressionLevel(byte cmpLevel) {
        this.cmpLevel = cmpLevel;
    }

    /**
     * Returns the current compression level, does not matter if use Zip is set
     * or not.
     */
    protected byte getCompressionLevel() {
        return cmpLevel;
    }

    /**
     * Sets the FileList with the files to en-/decrypt to the given parameter.
     */
    protected void setFileList(FileList fileList) {
        this.fileList = fileList;
    }

    /**
     * Returns a FileList of the files to en-/decrypt.
     */
    protected FileList getFileList() {
        return fileList;
    }

    /**
     * Sets the password which is to be used for en-/decryption to the given
     * password.
     */
    protected void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the current password which should be used for en-/decryption.
     */
    protected String getPassword() {
        return password;
    }

    /**
     * Sets the name of the algorithm which should be used for en-/decryption to
     * the given algorithm name.
     */
    protected void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    /**
     * Returns the current name of the algorithm which should be used for
     * en-/decryption.
     */
    protected String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Returns the algorithm which should be used for
     * en-/decryption.
     */
    protected Algorithm getAlgorithm() {
        return currentAlgorithm;
    }

    /**
     * @return the targetArchive
     */
    public File getTargetArchive() {
        if (targetArchive == null) {
            String abspath0 = fileList.get(0).getAbsolutePath();
            File def = null;
            if (abspath0.lastIndexOf('.') > 0) {
                def = new File(abspath0.substring(0, abspath0.lastIndexOf('.')) + currentAlgorithm.getSuffix());
            } else {
                def = new File(abspath0 + currentAlgorithm.getSuffix());
            }
            targetArchive = Application.getController().askForTargetArchive(def);
        }
        return targetArchive;
    }

    /**
     * @param targetArchive the targetArchive to set
     */
    public void setTargetArchive(File targetArchive) {
        this.targetArchive = targetArchive;
    }

    /**
     * call to get the directory the decrypted files should be stored in.
     * @return the targetDirectory
     */
    public File getTargetDirectory() {
        if (targetDirectory == null) {
            String abspath0 = fileList.get(0).getAbsolutePath();
            targetDirectory = Application.getController().askForTargetDirectory(new File(abspath0.substring(0, abspath0.length() - currentAlgorithm.getSuffix().length())));
        }
        return targetDirectory;
    }

    /**
     * @param targetDirectory the targetDirectory to set
     */
    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    /**
     * Start encryption.
     */
    protected void encrypt() {
        doEncrypt();
    }

    /**
     * Start decryption.
     */
    protected void decrypt() {
        doDecrypt();
    }

    /**
     * Stops the currently running algorithm.
     */
    protected void stop() {
        if (runningThread != null && runningThread.isAlive()) {
            runningThread.interrupt();
        }
    }

    /**
     * Returns all algorithms available as an Algorithm array.
     */
    protected Algorithm[] getAlgorithms() {
        return algFactory.getAlgorithms();
    }

    private void updateProgress() {
        double dprog = progNow / (double) progLength * 100.0;
        int prog = (int) Math.round(dprog);
        if (prog > progOld) {
            Application.getController().notifyProgressUpdated(prog);
            progOld = prog;
        }
    }

    /**
     * Starts encryption without compression.
     */
    private void doEncrypt() {
        runningThread = new Thread() {

            @Override
            public void run() {
                Application.getController().notifyProgressStarted();
                Algorithm alg = algFactory.getAlgorithm(algorithmName);
                currentAlgorithm = alg;
                if (!alg.initEncrypt(password)) {
                    Application.getController().displayError(bundle.getString("enc_init_fail_title"), bundle.getString("enc_init_fail_text"));
                    return;
                }
                File tA = getTargetArchive();
                if (tA == null) {
                    Application.getController().notifyProgressFinished();
                    resetModel(true);
                    return;
                }
                if (tA.exists()) {
                    if (!mayOverwrite(tA)) {
                        Application.getController().notifyProgressFinished();
                        resetModel(true);
                        return;
                    }
                }
                try {
                    ZipArchiveOutputStream zos = null;
                    OutputStream os = null;
                    if (EncryptionMode.getBestEncryptionMode(alg.getEncryptionMode()) == EncryptionMode.MODE_STREAM) {
                        os = alg.getEncryptionStream(new FileOutputStream(tA));
                        if (os == null) {
                            Application.getController().displayError(bundle.getString("enc_init_fail_title"), bundle.getString("enc_init_fail_text"));
                            return;
                        }
                    } else if (EncryptionMode.getBestEncryptionMode(alg.getEncryptionMode()) == EncryptionMode.MODE_BLOCK) {
                        os = new BlockCipherOutputStream(new FileOutputStream(tA), alg);
                        if (os == null) {
                            Application.getController().displayError(bundle.getString("enc_init_fail_title"), bundle.getString("enc_init_fail_text"));
                            return;
                        }
                    }
                    zos = new ZipArchiveOutputStream(os);
                    if (zos == null) {
                        Application.getController().displayError(bundle.getString("enc_init_fail_title"), bundle.getString("enc_init_fail_text"));
                        return;
                    }
                    zos.setLevel(getCompressionLevel());
                    for (int i = 0; i < fileList.size(); i++) {
                        progLength += computeFileSize(fileList.get(i));
                    }
                    for (int i = 0; i < fileList.size(); i++) {
                        File file = fileList.get(i);
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        copyFileToArchive(zos, file, "");
                    }
                    zos.close();
                    Application.getController().notifyProgressFinished();
                    resetModel(true);
                } catch (FileNotFoundException ex) {
                    Application.getController().displayError(bundle.getString("error_file_not_exist"), ex.getLocalizedMessage());
                } catch (IOException ex) {
                    Application.getController().displayError(bundle.getString("error_generic_io"), ex.getLocalizedMessage());
                }
            }
        };
        runningThread.start();
    }

    private boolean mayOverwrite(File file) {
        if (overwriteMode == Controller.OVERWRITE_ALL_NO) {
            return false;
        }
        if (overwriteMode == Controller.OVERWRITE_ALL_YES) {
            return true;
        }
        overwriteMode = Application.getController().shallOverwriteFile(file);
        if (overwriteMode == Controller.OVERWRITE_ALL_YES || overwriteMode == Controller.OVERWRITE_ONCE_YES) {
            return true;
        } else {
            return false;
        }
    }

    private void resetModel(boolean fully) {
        progOld = 0;
        progLength = 0;
        progNow = 0;
        overwriteMode = 0;
        if (fully) {
            targetDirectory = null;
            targetArchive = null;
            runningThread = null;
            password = null;
        }
    }

    private long computeFileSize(File file) {
        if (file.isFile()) {
            return file.length();
        } else if (file.isDirectory()) {
            File subs[] = file.listFiles();
            long s = 0;
            for (int i = 0; i < subs.length; i++) {
                s += computeFileSize(subs[i]);
            }
            return s;
        } else {
            return 0;
        }
    }

    private void copyFileToArchive(final ZipArchiveOutputStream zos, final File file, final String dir) {
        try {
            if (file.isFile()) {
                ZipArchiveEntry zae = new ZipArchiveEntry(dir + file.getName());
                zae.setSize(file.length());
                zos.putArchiveEntry(zae);
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                DigestInputStream in = new DigestInputStream(new FileInputStream(file), md);
                byte[] buf = new byte[16384];
                int l = 0;
                while ((l = in.read(buf)) > 0) {
                    zos.write(buf, 0, l);
                    progNow += l;
                    if (Thread.currentThread().isInterrupted()) {
                        zos.closeArchiveEntry();
                        return;
                    }
                    updateProgress();
                }
                Application.getController().displayVerbose("Hash of " + zae.getName() + ": " + new Base64().encodeToString(md.digest()));
                zae.setComment(new Base64().encodeToString(md.digest()));
                zos.closeArchiveEntry();
            } else if (file.isDirectory()) {
                File subs[] = file.listFiles();
                for (int i = 0; i < subs.length; i++) {
                    copyFileToArchive(zos, subs[i], dir + file.getName() + "/");
                }
            }
        } catch (IOException ex) {
            Application.getController().displayError(bundle.getString("error_generic_io"), ex.getLocalizedMessage());
        } catch (NoSuchAlgorithmException ex) {
            Application.getController().displayError(bundle.getString("unknown_alg_text"), ex.getLocalizedMessage());
        }
    }

    /**
     * Implements the decrypt procedure, consisting of determining the used algorithm,
     * opening the archive, reading the stored files and decrypting them.
     */
    private void doDecrypt() {
        runningThread = new Thread() {

            @Override
            public void run() {
                Application.getController().notifyProgressStarted();
                Application.getController().notifyProgressUpdated(-1);
                for (int f = 0; f < fileList.size(); f++) {
                    File archive = fileList.get(f);
                    String arname = archive.getName();
                    String arsuf = arname.substring(arname.lastIndexOf('.'), arname.length());
                    Algorithm alg = null;
                    Algorithm algs[] = algFactory.getAlgorithms();
                    for (int i = 0; i < algs.length; i++) {
                        if (algs[i].getSuffix().equalsIgnoreCase(arsuf)) {
                            alg = algs[i];
                            break;
                        }
                    }
                    if (alg == null) {
                        Application.getController().displayError(bundle.getString("unknown_alg_title"), bundle.getString("unknown_alg_text"));
                        return;
                    }
                    currentAlgorithm = alg;
                    if (!alg.initDecrypt(password)) {
                        Application.getController().displayError(bundle.getString("dec_init_fail_title"), bundle.getString("dec_init_fail_text"));
                        return;
                    }
                    try {
                        ZipArchiveInputStream zis = null;
                        InputStream is = null;
                        if (EncryptionMode.getBestEncryptionMode(alg.getEncryptionMode()) == EncryptionMode.MODE_STREAM) {
                            is = alg.getDecryptionStream(new FileInputStream(archive));
                            if (is == null) {
                                Application.getController().displayError(bundle.getString("dec_init_fail_title"), bundle.getString("dec_init_fail_text"));
                                return;
                            }
                        } else if (EncryptionMode.getBestEncryptionMode(alg.getEncryptionMode()) == EncryptionMode.MODE_BLOCK) {
                            is = new BlockCipherInputStream(new FileInputStream(archive), alg);
                            if (is == null) {
                                Application.getController().displayError(bundle.getString("dec_init_fail_title"), bundle.getString("dec_init_fail_text"));
                                return;
                            }
                        }
                        zis = new ZipArchiveInputStream(is);
                        if (zis == null) {
                            Application.getController().displayError(bundle.getString("dec_init_fail_title"), bundle.getString("dec_init_fail_text"));
                            return;
                        }
                        File outputDir = getTargetDirectory();
                        if (outputDir == null) {
                            return;
                        }
                        if (!outputDir.exists()) {
                            if (!outputDir.mkdir()) {
                                Application.getController().displayError(bundle.getString("output_dir_fail_title"), outputDir.getAbsolutePath() + " " + bundle.getString("output_dir_fail_text"));
                                return;
                            }
                        }
                        ZipArchiveEntry zae = null;
                        boolean gotEntries = false;
                        while ((zae = zis.getNextZipEntry()) != null) {
                            gotEntries = true;
                            File out = new File(outputDir, zae.getName());
                            if (out.exists()) {
                                if (!mayOverwrite(out)) {
                                    continue;
                                }
                            }
                            Application.getController().displayVerbose("writing to file: " + out.getAbsolutePath());
                            if (!out.getParentFile().exists()) {
                                out.getParentFile().mkdirs();
                            }
                            if (zae.isDirectory()) {
                                out.mkdir();
                                continue;
                            }
                            FileOutputStream os = new FileOutputStream(out);
                            long length = zae.getCompressedSize(), counter = 0;
                            Application.getController().displayVerbose("Length of zip entry " + zae.getName() + " is " + length + "b");
                            byte[] buffer = new byte[16384];
                            MessageDigest md = MessageDigest.getInstance("SHA-1");
                            DigestInputStream in = new DigestInputStream(zis, md);
                            while ((counter = in.read(buffer)) > 0) {
                                if (Thread.currentThread().isInterrupted()) {
                                    os.close();
                                    zis.close();
                                    Application.getController().notifyProgressFinished();
                                    resetModel(true);
                                    return;
                                }
                                os.write(buffer, 0, (int) counter);
                            }
                            os.close();
                            if (zae.getComment() != null && zae.getComment().length() > 0) {
                                if (Arrays.equals(md.digest(), new Base64().decode(zae.getComment()))) {
                                    Application.getController().displayVerbose("Hash of " + zae.getName() + ": " + new Base64().encodeToString(md.digest()));
                                    Application.getController().displayError("Hash Error", "The stored hash of the original file and the hash of the decrypted data do not match. Normally, this means that your data has been manipulated/damaged, but it can also happen if your Java Runtime has a bug in his hash functions.\nIT IS VERY IMPORTANT TO CHECK THE INTEGRITY OF YOUR DECRYPTED DATA!");
                                } else {
                                    Application.getController().displayVerbose("the hash of " + zae.getName() + " was verified succesfully");
                                }
                            }
                        }
                        if (!gotEntries) {
                            Application.getController().displayError(bundle.getString("error_no_entries_title"), bundle.getString("error_no_entries_text"));
                            outputDir.delete();
                        }
                        zis.close();
                        resetModel(false);
                    } catch (FileNotFoundException ex) {
                        Application.getController().displayError(bundle.getString("error_file_not_exist"), ex.getLocalizedMessage());
                    } catch (IOException ex) {
                        Application.getController().displayError(bundle.getString("error_generic_io"), ex.getLocalizedMessage());
                    } catch (NoSuchAlgorithmException ex) {
                        Application.getController().displayError(bundle.getString("unknown_alg_text"), ex.getLocalizedMessage());
                    }
                }
                Application.getController().notifyProgressFinished();
                resetModel(true);
            }
        };
        runningThread.start();
    }

    /**
     * Use this method to check the settings.
     */
    @Override
    public String toString() {
        String rtr = "";
        rtr += "cmpLevel: " + cmpLevel + "\n";
        rtr += "password: " + password + "\n";
        rtr += "algorithm: " + algorithmName + "\n";
        rtr += "filenames: \n";
        Iterator it = fileList.getIterator();
        while (it.hasNext()) {
            File f = (File) it.next();
            rtr += f.getPath() + "\n";
        }
        return rtr;
    }
}
