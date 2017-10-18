import java.io.*;
import java.util.Vector;
import java.security.MessageDigest;
import java.math.BigInteger;

public class PatchCreator implements ProcessMaster {

    private String dirSource;

    private String dirModif;

    private String dirDest;

    private String zipName;

    private PrintWriter log;

    private PrintWriter md5File;

    private PrintWriter filesToDelete;

    private String tempDir;

    private PatcherWindow myWindow = null;

    private int numFilesToProcess = 0;

    private int threadRunning = 0;

    private int maxThreadRunning = 20;

    public PatchCreator(String s, String m, String d, String z) {
        dirSource = s;
        dirModif = m;
        dirDest = d;
        zipName = z + ".zip";
        if (!dirSource.endsWith(File.separator)) dirSource = dirSource.concat(File.separator);
        if (!dirModif.endsWith(File.separator)) dirModif = dirModif.concat(File.separator);
        if (!dirDest.endsWith(File.separator)) dirDest = dirDest.concat(File.separator);
        tempDir = PatchManager.getFreeFolderName("dirDest", "tmp");
        dirDest += tempDir + File.separator;
        new File(dirDest).mkdir();
        try {
            log = new PrintWriter(new BufferedWriter(new FileWriter(dirDest + "log.txt")));
            md5File = new PrintWriter(new BufferedWriter(new FileWriter(dirDest + "md5.check")));
            filesToDelete = new PrintWriter(new BufferedWriter(new FileWriter(dirDest + "todelete.patch")));
        } catch (Exception e) {
            System.out.println("Can't create log file... " + e);
            throw new Error("Can't create log file...");
        }
        try {
            DirectoryManager s1 = new DirectoryManager(dirSource);
            DirectoryManager m1 = new DirectoryManager(dirModif);
            DirectoryManager d1 = new DirectoryManager(dirDest);
            Vector<String> fileName = new Vector<String>();
            s1.getAllMyFileNames(fileName, true, false);
            m1.getAllMyFileNames(fileName, true, false);
            d1.getAllMyFileNames(fileName, true, false);
            numFilesToProcess = fileName.size();
        } catch (Error e) {
            throw new Error("error creating patch : " + e.getMessage());
        }
        writeOnLog("Num files to process : " + numFilesToProcess);
    }

    public void setMyWindow(PatcherWindow j) {
        myWindow = j;
    }

    public void setProcessSpeed(int newSpeed) {
        if (newSpeed < 1) newSpeed = 1;
        maxThreadRunning = newSpeed;
    }

    public int getNumFilesToProcess() {
        return numFilesToProcess;
    }

    public void setProgress(int addValue) {
        if (myWindow != null) {
            myWindow.progress.setValue(myWindow.progress.getValue() + addValue);
            myWindow.repaint();
        }
    }

    public synchronized void writeOnLog(String message) {
        if (!PatchManager.mute) {
            log.println(message);
            System.out.println(message);
        }
    }

    public boolean getToken() {
        if (threadRunning < maxThreadRunning) {
            threadRunning++;
            return true;
        }
        return false;
    }

    public static String getMD5(File f) {
        String output = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            InputStream is = new FileInputStream(f);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            output = bigInt.toString(16);
            is.close();
        } catch (Exception e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        }
        return output;
    }

    public synchronized void releaseToken() {
        threadRunning--;
    }

    public String createPatch() {
        DirectoryManager s = new DirectoryManager(dirSource);
        DirectoryManager m = new DirectoryManager(dirModif);
        DirectoryManager d = new DirectoryManager(dirDest);
        String res = "";
        try {
            if (myWindow != null) {
                myWindow.progress.setValue(0);
                myWindow.progress.setMaximum(s.calcNumFiles(true, false));
                myWindow.info.setText("Patch creation : processing source->modifications");
            }
            skimThroughSource(s, m, d);
            while (threadRunning > 0) {
                writeOnLog("Waiting for " + threadRunning + " threads to terminate...");
                Thread.sleep(500);
            }
            if (myWindow != null) {
                myWindow.progress.setValue(0);
                myWindow.progress.setMaximum(m.calcNumFiles(true, false));
                myWindow.info.setText("Patch creation : processing modifications->source");
            }
            skimThroughMod(s, m, d);
        } catch (Exception e) {
            res = "error creating patch : " + e;
            writeOnLog(res);
            return res;
        }
        try {
            while (threadRunning > 0) {
                writeOnLog("Waiting for " + threadRunning + " threads to terminate...");
                Thread.sleep(500);
            }
        } catch (Exception e) {
            writeOnLog("Was waiting for " + threadRunning + " threads to terminate... and error ! " + e);
        }
        log.close();
        filesToDelete.close();
        md5File.close();
        if (myWindow != null) {
            myWindow.progress.setValue(0);
            myWindow.progress.setMaximum(d.calcNumFiles(true, false));
            myWindow.info.setText("Patch creation : creating final zip file");
        }
        ZipCreator z = new ZipCreator(this, dirDest.replace(tempDir + File.separator, "") + zipName, dirDest);
        boolean zipCreated = z.create();
        if (myWindow != null) {
            myWindow.info.setText("Deleting temporary files");
            myWindow.repaint();
        }
        d.deleteMe();
        if (zipCreated) res = "<html>Patch successfully created!<br>" + dirDest.replace(tempDir + File.separator, "") + zipName + "</html>"; else res = "error while creating patch...";
        return res;
    }

    private boolean patchFile(File oldFile, File newFile, String dest) {
        String command = PatchManager.tempDirectory + File.separator + "jdiff-w32.exe";
        String args[] = new String[] { oldFile.getPath(), newFile.getPath(), dest + newFile.getName() + "PATCHED" };
        writeOnLog("Now creating patch for " + oldFile.getPath() + " to fit " + newFile.getPath() + ". Dest :" + dest + newFile.getName());
        ConsoleEmulator console = new ConsoleEmulator(this, command, args);
        console.useCommandInterpreter(false);
        MD5Maker mm = new MD5Maker(this, oldFile);
        while (!getToken()) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
        console.start();
        while (!getToken()) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
        mm.start();
        return true;
    }

    public synchronized void writeOnMD5File(File f, String md5) {
        md5File.println(f.getPath().replace(dirSource, "") + " " + md5);
    }

    private void skimThroughSource(DirectoryManager s, DirectoryManager m, DirectoryManager d) throws IOException {
        File[] filesSource = s.listMyFiles();
        for (int i = 0; i < filesSource.length; i++) {
            if (filesSource[i].isDirectory()) {
                if (m.ownsThisDirectory(filesSource[i].getName())) {
                    String subDirPath = d.getDirectory() + filesSource[i].getName();
                    File subDir = new File(subDirPath);
                    subDir.mkdir();
                    writeOnLog("directory created:" + subDir);
                    skimThroughSource(new DirectoryManager(filesSource[i].getPath()), new DirectoryManager(m.getDirectory() + filesSource[i].getName()), new DirectoryManager(subDirPath));
                } else {
                    DirectoryManager willBeDeleted = new DirectoryManager(filesSource[i].getPath());
                    setProgress(willBeDeleted.calcNumFiles(true, false));
                    filesToDelete.println(filesSource[i].getPath().replace(dirSource, ""));
                }
            } else if (filesSource[i].isFile()) {
                setProgress(1);
                if (m.ownsThisFile(filesSource[i].getName())) patchFile(filesSource[i], new File(m.getDirectory() + filesSource[i].getName()), d.getDirectory()); else filesToDelete.println(filesSource[i].getPath().replace(dirSource, ""));
            }
        }
    }

    private void skimThroughMod(DirectoryManager s, DirectoryManager m, DirectoryManager d) throws IOException {
        File[] filesMod = m.listMyFiles();
        for (int i = 0; i < filesMod.length; i++) {
            if (filesMod[i].isDirectory()) {
                if (!s.ownsThisDirectory(filesMod[i].getName())) {
                    String subDirPath = d.getDirectory() + filesMod[i].getName();
                    DirectoryManager mSubDir = new DirectoryManager(filesMod[i].getPath());
                    mSubDir.copyMe(subDirPath);
                    setProgress(mSubDir.calcNumFiles(true, false));
                } else {
                    String subDirPath = d.getDirectory() + filesMod[i].getName();
                    File subDir = new File(subDirPath);
                    subDir.mkdir();
                    writeOnLog("directory created:" + subDir);
                    skimThroughMod(new DirectoryManager(s.getDirectory() + filesMod[i].getName()), new DirectoryManager(filesMod[i].getPath()), new DirectoryManager(subDirPath));
                }
            } else if (filesMod[i].isFile()) {
                setProgress(1);
                if (!s.ownsThisFile(filesMod[i].getName())) m.copyOneOfMyFile(filesMod[i], d.getDirectory());
            }
        }
    }
}
