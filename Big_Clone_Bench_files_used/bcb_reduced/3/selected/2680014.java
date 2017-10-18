package org.openconcerto.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Backup {

    public static final Logger getLogger() {
        return Logger.getLogger("org.openconcerto.backup");
    }

    private final File dest;

    public Backup(File dest) {
        this.dest = dest;
    }

    static final int bufferSize = 512 * 1024;

    /**
     * Copy of File
     * 
     * @param sourceFile
     * @param destFile
     * @param bar
     */
    private int copy(final File sourceFile, File destFile) {
        int failed = 0;
        if (sourceFile.isDirectory()) {
            destFile.mkdirs();
            return 0;
        }
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            destFile.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            FileOutputStream bufOut = new FileOutputStream(destFile);
            FileInputStream bufIn = new FileInputStream(sourceFile);
            try {
                byte buffer[] = new byte[bufferSize];
                int nbLecture;
                while ((nbLecture = bufIn.read(buffer)) != -1) {
                    bufOut.write(buffer, 0, nbLecture);
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Copy Failed for file : " + sourceFile);
                System.err.println("Failed : " + sourceFile);
                failed = 1;
                e.printStackTrace();
            } finally {
                try {
                    bufIn.close();
                    bufOut.close();
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "Error to close Stream in copy." + sourceFile);
                    failed = 1;
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            getLogger().log(Level.SEVERE, "Copy Failed, File not found " + sourceFile);
            failed = 1;
            e.printStackTrace();
        }
        destFile.setLastModified(sourceFile.lastModified());
        return failed;
    }

    /**
     * Recherche les fichiers à créer dans le répertoire passé en parametre
     * 
     * @param dir
     * @return le nombre d'erreurs
     */
    public int applyTo(final File dir) {
        getLogger().log(Level.INFO, "Copy start from " + dir + " to " + this.dest);
        return applyTo(dir, dir);
    }

    private int applyTo(final File origine, final File dir) {
        int failed = 0;
        if (dir.exists()) {
            File file = getFile(origine, dir);
            if (!file.exists()) {
                file.mkdirs();
            }
            File[] list = dir.listFiles();
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    File f = list[i];
                    File f2 = getFile(origine, f);
                    if (f.isDirectory()) {
                        if (!f2.exists()) {
                            failed += copy(f, f2);
                        }
                        applyTo(origine, f);
                    } else {
                        if (f2.exists()) {
                            boolean dateModif = f.lastModified() == f2.lastModified();
                            if (!dateModif || f.length() != f2.length()) {
                                failed += copy(f, f2);
                            } else {
                                if (!dateModif && f.length() == f2.length()) {
                                    String MD5f = getMD5(f);
                                    String MD5f2 = getMD5(f2);
                                    boolean md5 = MD5f.equalsIgnoreCase(MD5f2);
                                    if (!(MD5f.length() > 0 && MD5f2.length() > 0)) {
                                        if (md5) {
                                            f2.setLastModified(f.lastModified());
                                        } else {
                                            failed += copy(f, f2);
                                        }
                                    } else {
                                        failed += copy(f, f2);
                                    }
                                }
                            }
                        } else {
                            failed += copy(f, f2);
                        }
                    }
                }
            } else {
                failed++;
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(null, "Impossible de lister le répertoire " + dir + ".\n Vous n'avez pas les droits suffisants!");
                    }
                });
            }
        }
        return failed;
    }

    /**
     * Get the new path for the saved file
     * 
     * @param f File to save
     * @return
     */
    private File getFile(File origine, File f) {
        File base = (origine.getParentFile() == null) ? origine : origine.getParentFile();
        String s2 = base.toURI().relativize(f.toURI()).getPath();
        return new File(this.dest, s2);
    }

    /**
     * 
     * @param f File
     * @return the md5 of the File
     */
    private String getMD5(File f) {
        MessageDigest digest;
        String output = "";
        InputStream is;
        try {
            is = new FileInputStream(f);
            try {
                digest = MessageDigest.getInstance("MD5");
                is = new FileInputStream(f);
                byte buffer[] = new byte[512 * 1024];
                int read = 0;
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
                byte[] md5sum = digest.digest();
                BigInteger bigInt = new BigInteger(1, md5sum);
                output = bigInt.toString(16);
                is.close();
            } catch (IOException e) {
                getLogger().log(Level.INFO, "Unable to process file for MD5. " + f);
                throw new RuntimeException("Unable to process file for MD5", e);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    getLogger().log(Level.INFO, "Unable to close file at the end of MD5. " + f);
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e1) {
            getLogger().log(Level.INFO, "File not found for MD5. " + f);
            e1.printStackTrace();
        }
        return output;
    }

    public void close() {
    }
}
