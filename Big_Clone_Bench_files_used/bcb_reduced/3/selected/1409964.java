package com.frinika.webstart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JFrame;
import javax.swing.JProgressBar;

/**
 *
 * @author Peter Johan Salomonsen
 */
public class FrinikaLauncher {

    static final int BUFSIZE = 2048;

    static final int CONSOLESIZE = 16384;

    class HexStringToByteArray128 {

        byte[] bytes = new byte[16];

        HexStringToByteArray128(String hexString) {
            for (int n = 0; n < hexString.length(); n += 2) {
                bytes[bytes.length - (n / 2) - 1] = (byte) Integer.parseInt(hexString.substring(hexString.length() - 2 - n, hexString.length() - n), 16);
            }
        }

        byte[] getBytes() {
            return bytes;
        }
    }

    final ResourceBundle config = ResourceBundle.getBundle("launcherconfig");

    final String remoteURLPath = config.getString("remoteURLPath");

    final String frinikaFileName = config.getString("frinikaFileName");

    final int fileSize = Integer.parseInt(config.getString("fileSize"));

    final BigInteger md5 = new BigInteger(new HexStringToByteArray128(config.getString("md5")).getBytes());

    final String installDirName = ".frinikaInstaller";

    final File frinikaFile = new File(installDirName + "/" + frinikaFileName);

    ;

    JProgressBar progressBar;

    JFrame launchFrame;

    public FrinikaLauncher(JProgressBar progressBar, JFrame launchFrame) {
        this.progressBar = progressBar;
        this.launchFrame = launchFrame;
    }

    public void createInstallDir() {
        showMessage("Creating install dir");
        File installDir = new File(installDirName);
        if (!installDir.exists()) installDir.mkdir();
    }

    public void downloadFrinika() throws Exception {
        if (!frinikaFile.exists()) {
            String urlString = remoteURLPath + frinikaFileName;
            showMessage("Connecting to " + urlString);
            URLConnection uc = new URL(urlString).openConnection();
            progressBar.setIndeterminate(false);
            showMessage("Downloading from " + urlString);
            progressBar.setValue(0);
            progressBar.setMinimum(0);
            progressBar.setMaximum(fileSize);
            InputStream is = uc.getInputStream();
            FileOutputStream fos = new FileOutputStream(frinikaFile);
            byte[] b = new byte[BUFSIZE];
            int c;
            while ((c = is.read(b)) != -1) {
                fos.write(b, 0, c);
                progressBar.setValue(progressBar.getValue() + c);
            }
            fos.close();
        }
    }

    public void checkMD5() throws Exception {
        showMessage("Checking authenticy of downloaded file");
        FileInputStream fis = new FileInputStream(frinikaFile);
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(fis, md);
        progressBar.setValue(0);
        progressBar.setMinimum(0);
        progressBar.setMaximum((int) frinikaFile.length());
        byte[] b = new byte[BUFSIZE];
        int c;
        while ((c = dis.read(b)) != -1) {
            progressBar.setValue(progressBar.getValue() + c);
        }
        BigInteger calculatedMd5 = new BigInteger(md.digest());
        System.out.println("Expected MD5 sum = '" + md5.toString(16) + "'");
        System.out.println("MD5 sum = '" + calculatedMd5.toString(16) + "'");
        if (!md5.equals(calculatedMd5)) {
            frinikaFile.delete();
            throw new InvalidMD5Exception("Downloaded file is not authentic, and is now deleted - please check your download sources");
        }
    }

    public void extractFrinika() throws Exception {
        FileInputStream fis = new FileInputStream(frinikaFile);
        progressBar.setIndeterminate(true);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            showMessage("Extracting: " + ze.getName());
            File file = new File(installDirName + "/" + ze.getName());
            if (ze.isDirectory()) file.mkdir(); else {
                FileOutputStream fos = new FileOutputStream(file);
                byte[] b = new byte[BUFSIZE];
                int c;
                while ((c = zis.read(b)) != -1) fos.write(b, 0, c);
                fos.close();
            }
            ze = zis.getNextEntry();
        }
    }

    void showMessage(String text) {
        System.out.println(text);
        progressBar.setStringPainted(true);
        progressBar.setString(text);
    }

    public void launchFrinika() throws Exception {
        try {
            showMessage("Launching Frinika");
            Process p = java.lang.Runtime.getRuntime().exec("java -Xmx512m -jar frinika.jar", null, new File(installDirName));
            launchFrame.setVisible(false);
            InputStream is = p.getInputStream();
            InputStream es = p.getErrorStream();
            int c;
            while ((c = is.read()) != -1) {
                System.out.print((char) c);
                while (es.available() > 0) System.err.print(es.read());
            }
        } catch (Exception e) {
            for (StackTraceElement ste : e.getStackTrace()) System.out.println(ste.toString());
        }
        System.exit(0);
    }
}
