package components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import main.Main;

public class Checker {

    private static final int bufsize = 100000;

    private static final int timerms = 700;

    private String resultmd5;

    private String resultsha;

    private PrintStream ps;

    protected JProgressBar bar;

    protected IntegerWrapper already;

    private boolean stop;

    private Timer timer;

    public Checker(final PrintStream p) {
        ps = p;
        stop = false;
    }

    /**
	 * We need a modifiable int object (Integer cannot be modified). 
	 */
    class IntegerWrapper {

        private int value = 0;

        public void setValue(final int v) {
            value = v;
        }

        public int getValue() {
            return value;
        }

        public void addValue(final int v) {
            value += v;
        }
    }

    /**
	 * Inner class for the progress bar updating. 
	 */
    class Updater extends Timer {

        private static final long serialVersionUID = 60323357898179044L;

        public Updater(final int interval, final JProgressBar progress, final IntegerWrapper bytesRead) {
            super(interval, new ActionListener() {

                public void actionPerformed(final ActionEvent evt) {
                    if (progress != null) progress.setValue(bytesRead.getValue());
                }
            });
        }
    }

    /**
	 * Check a file.
	 * @param file
	 * @param p Progressbar. Use null for command-line mode
	 * @param batch Is in batch mode. For command-line mode set p to null 
	 * @return "ok" if ok, error otherwise
	 */
    public String check(final File file, final JProgressBar p, final boolean batch, final boolean suppressDialogs) {
        MessageDigest md5;
        MessageDigest sha;
        bar = p;
        long filesize = file.length();
        if (bar != null) {
            bar.setMinimum(0);
            bar.setMaximum((int) filesize);
        }
        try {
            md5 = MessageDigest.getInstance("MD5");
            sha = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return Main.res.getString("digesterr");
        }
        FileInputStream fis = null;
        byte[] in = new byte[bufsize];
        int read = 0;
        already = new IntegerWrapper();
        timer = new Updater(timerms, bar, already);
        timer.start();
        try {
            fis = new FileInputStream(file);
            read = fis.read(in);
            while (read > 0) {
                already.addValue(read);
                if (read == bufsize) {
                    md5.update(in);
                    sha.update(in);
                } else {
                    md5.update(Arrays.copyOfRange(in, 0, read));
                    sha.update(Arrays.copyOfRange(in, 0, read));
                }
                if (getBreak()) {
                    fis.close();
                    timer.stop();
                    resetBarValue();
                    if (!batch) ps.println(Main.res.getString("break"));
                    return "break";
                }
                read = fis.read(in);
            }
            fis.close();
            timer.stop();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(ps);
            return Main.res.getString("fnferr");
        } catch (IOException ex) {
            ex.printStackTrace(ps);
            return Main.res.getString("ioerr");
        }
        StringBuffer res = new StringBuffer(16);
        Formatter f = new Formatter(res);
        for (byte b : md5.digest()) f.format("%02x", b);
        resultmd5 = res.toString().toLowerCase();
        res = new StringBuffer(20);
        f = new Formatter(res);
        for (byte b : sha.digest()) f.format("%02x", b);
        resultsha = res.toString().toLowerCase();
        resetBarValue();
        if (!batch) {
            ps.println(Main.res.getString("file") + ": " + file.getName() + "\n  MD-5 checksum        " + resultmd5 + "\n  SHA-1 checksum       " + resultsha);
            String checksum;
            try {
                File filemd5 = new File(file.getAbsoluteFile() + ".md5");
                FileReader fr = new FileReader(filemd5);
                LineNumberReader lnr = new LineNumberReader(fr);
                checksum = lnr.readLine();
                if (checksum == null) checksum = "";
                lnr.close();
            } catch (IOException ex) {
                return Main.res.getString("ioerr");
            }
            int spacepos = checksum.indexOf("  ");
            if (spacepos < 0) spacepos = checksum.indexOf(" *");
            if (spacepos > 0) {
                String checkname = checksum.substring(spacepos + 2, checksum.length());
                if (checkname.contains("/")) {
                    String[] tmp = checkname.split("/");
                    checkname = tmp[tmp.length - 1];
                }
                checksum = checksum.substring(0, spacepos).toLowerCase();
                if (!checkname.equals(file.getName())) userOutput(Main.res.getString("comparefilefailed"), Main.res.getString("comparetitle"), JOptionPane.WARNING_MESSAGE, suppressDialogs);
            }
            if (checksum.equals(resultmd5)) userOutput(Main.res.getString("comparesuccess.md5file"), Main.res.getString("comparetitle"), JOptionPane.INFORMATION_MESSAGE, suppressDialogs); else userOutput(Main.res.getString("comparefailed"), Main.res.getString("comparetitle"), JOptionPane.WARNING_MESSAGE, suppressDialogs);
        }
        return "ok";
    }

    public String getLastResultMD5() {
        return resultmd5;
    }

    public String getLastResultSHA() {
        return resultsha;
    }

    public synchronized void stopThread() {
        stop = true;
    }

    public synchronized boolean getBreak() {
        return stop;
    }

    private void resetBarValue() {
        if (bar != null) SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                bar.setValue(0);
            }
        });
    }

    private void userOutput(String message, String title, int mode, boolean suppressDialogs) {
        if (!suppressDialogs) JOptionPane.showMessageDialog(null, message, title, mode); else ps.println(message);
    }

    public void setResultsAfterListProcessing(String result) {
        resultmd5 = result.toLowerCase();
        resultsha = resultmd5;
    }
}
