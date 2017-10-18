import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PatchDecoder implements ProcessMaster {

    private String patch;

    private String dirDest;

    private String tempDir;

    private int numFilesToProcess = 0;

    private PatcherWindow myWindow = null;

    private PrintWriter log;

    private int threadRunning = 0;

    public PatchDecoder(String p, String d) {
        patch = p;
        dirDest = d;
        tempDir = "";
        if (!dirDest.endsWith(File.separator)) dirDest = dirDest.concat(File.separator);
        try {
            ZipInputStream in = new ZipInputStream(new FileInputStream(patch));
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                if (!entry.isDirectory()) numFilesToProcess++;
            }
            in.close();
        } catch (Exception e) {
        }
        try {
            log = new PrintWriter(new BufferedWriter(new FileWriter(dirDest + "log.txt", true)));
        } catch (Exception e) {
            System.out.println("can't create log file... " + e);
            throw new Error("can't create log file...");
        }
    }

    public int getNumFilesToProcess() {
        return numFilesToProcess;
    }

    public void setMyWindow(PatcherWindow j) {
        myWindow = j;
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
        if (threadRunning < 10) {
            threadRunning++;
            return true;
        }
        return false;
    }

    public synchronized void releaseToken() {
        threadRunning--;
    }

    public String getMD5(File f) {
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

    public String getOriginalFileMD5(File f) {
        String res = "";
        String fichier = "md5.check";
        String pathFile = (f.getPath().substring(0, f.getPath().length() - 7)).replace(new File(patch).getParent() + File.separator + tempDir + File.separator, "");
        try {
            InputStream ips = new FileInputStream(dirDest + fichier);
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(pathFile)) {
                    res = line.substring(pathFile.length() + 1, line.length());
                    if (res.length() == 32) break;
                }
            }
            br.close();
        } catch (Exception e) {
            writeOnLog("error while getting MD5 from md5.check of " + pathFile + " " + e);
        }
        return res;
    }

    public boolean canIApplyPatch() {
        String fichier = "md5.check";
        boolean res = true;
        try {
            InputStream ips = new FileInputStream(dirDest + fichier);
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr);
            String line;
            int nbLine = 0;
            while ((line = br.readLine()) != null) {
                nbLine++;
            }
            myWindow.progress.setMaximum(nbLine);
            InputStream ips2 = new FileInputStream(dirDest + fichier);
            InputStreamReader ipsr2 = new InputStreamReader(ips2);
            BufferedReader br2 = new BufferedReader(ipsr2);
            String pathFile = "";
            String md5 = "";
            while ((line = br2.readLine()) != null) {
                int ind = line.lastIndexOf(" ");
                pathFile = line.substring(0, ind);
                md5 = line.substring(ind + 1, line.length());
                File toCheck = new File(dirDest + File.separator + pathFile);
                writeOnLog("checking-" + pathFile + "- with md5 -" + md5 + "-");
                if (!getMD5(toCheck).equals(md5)) {
                    res = false;
                    setProgress(nbLine);
                    break;
                }
                setProgress(1);
            }
            br.close();
            ips.close();
            ipsr.close();
            br2.close();
            ips2.close();
            ipsr2.close();
        } catch (Exception e) {
            writeOnLog("error while getting MD5 from md5.check: " + e);
            res = false;
        }
        return res;
    }

    public String decodePatch() {
        File fpatch = new File(patch);
        tempDir = PatchManager.getFreeFolderName(fpatch.getParent(), "patch");
        String res = "<html>";
        writeOnLog("Num files to process : " + numFilesToProcess);
        boolean keepGoing = true;
        File newDir = new File(fpatch.getParent() + File.separator + tempDir);
        newDir.mkdir();
        if (myWindow != null) {
            myWindow.progress.setValue(0);
            myWindow.progress.setMaximum(numFilesToProcess);
            myWindow.info.setText("Unzipping patch");
        }
        ZipExtractor z = new ZipExtractor(this, patch, newDir.getPath());
        boolean extractOk = z.extract();
        DirectoryManager p = new DirectoryManager(newDir.getPath());
        if (!extractOk) {
            res += "Error while unzipping</html>";
        } else {
            DirectoryManager d = new DirectoryManager(dirDest);
            d.addMeFile(new File(p.getDirectory() + "md5.check"));
            if (myWindow != null) {
                myWindow.progress.setValue(0);
                myWindow.info.setText("Checking version compatibility");
            }
            if (!canIApplyPatch()) {
                res += "Error while applying patch : incompatible versions</html>";
            } else {
                if (myWindow != null) {
                    myWindow.progress.setValue(0);
                    myWindow.progress.setMaximum(numFilesToProcess);
                    myWindow.info.setText("Applying patch");
                }
                try {
                    skimThroughPatch(p, d);
                    while (threadRunning > 0) {
                        writeOnLog("Waiting for " + threadRunning + " threads to terminate...");
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    writeOnLog("error applying patch : " + e);
                    res += "Error while applying patch : " + e + "</html>";
                    keepGoing = false;
                }
                if (keepGoing) {
                    if (myWindow != null) {
                        myWindow.info.setText("Deleting temp patch folder");
                        myWindow.repaint();
                    }
                    String lig = "";
                    try {
                        File todel = new File(d.getDirectory() + "todelete.patch");
                        if (myWindow != null) {
                            myWindow.repaint();
                            myWindow.info.setText("Deleting obsolete files");
                        }
                        FileReader fc = new FileReader(todel);
                        BufferedReader fct = new BufferedReader(fc);
                        do {
                            lig = fct.readLine();
                            File fToDelete = new File(d.getDirectory() + lig);
                            if (fToDelete.exists()) {
                                if (fToDelete.isDirectory()) {
                                    DirectoryManager dirToDelete = new DirectoryManager(fToDelete.getPath());
                                    dirToDelete.deleteMe();
                                } else if (fToDelete.isFile()) {
                                    fToDelete.delete();
                                    writeOnLog("deleted : " + d.getDirectory() + lig);
                                }
                            }
                        } while (lig != null);
                        fc.close();
                        todel.delete();
                    } catch (Exception e) {
                        writeOnLog("error while deleting obsolete file " + d.getDirectory() + lig);
                        res += "Error while deleting obsolete file : " + d.getDirectory() + lig + "<br>";
                        keepGoing = false;
                    }
                    if (keepGoing) res += "Patch successfully applied!</html>";
                }
            }
            new File(d.getDirectory() + "md5.check").delete();
            writeOnLog("deleted md5 file " + d.getDirectory() + "md5.check");
        }
        log.close();
        p.deleteMe();
        return res;
    }

    private boolean applyPatch(File patch, File target, String dest) {
        String command = PatchManager.tempDirectory + File.separator + "jpatch-w32.exe";
        String targetTemp = dest + target.getName();
        String args[] = new String[] { target.getPath(), patch.getPath(), targetTemp };
        writeOnLog("Now applying patch " + patch.getPath() + " to file " + target.getPath());
        ConsoleEmulator console = new ConsoleEmulator(this, command, args);
        console.useCommandInterpreter(false);
        console.start();
        try {
            console.join(1000000);
        } catch (Exception e) {
            writeOnLog("Error while applying patch on " + target.getName() + " : " + e);
        }
        String targetPath = target.getPath();
        target.delete();
        File targetPatched = new File(targetTemp);
        targetPatched.renameTo(new File(targetPath));
        return true;
    }

    private void skimThroughPatch(DirectoryManager p, DirectoryManager d) throws IOException {
        File[] filesPatch = p.listMyFiles();
        if (filesPatch != null) {
            for (int i = 0; i < filesPatch.length; i++) {
                if (filesPatch[i].isDirectory()) {
                    if (!d.ownsThisDirectory(filesPatch[i].getName())) {
                        String subDirPath = d.getDirectory() + filesPatch[i].getName();
                        DirectoryManager pSubDir = new DirectoryManager(filesPatch[i].getPath());
                        writeOnLog("directory created:" + subDirPath);
                        pSubDir.copyMe(subDirPath);
                        setProgress(pSubDir.calcNumFiles(true, false));
                    } else {
                        skimThroughPatch(new DirectoryManager(filesPatch[i].getPath()), new DirectoryManager(d.getDirectory() + filesPatch[i].getName()));
                    }
                } else if (filesPatch[i].isFile()) {
                    boolean isAPatch = false;
                    String fileNameBase = "";
                    setProgress(1);
                    if (filesPatch[i].getName().endsWith("PATCHED")) {
                        isAPatch = true;
                        fileNameBase = filesPatch[i].getName().substring(0, filesPatch[i].getName().length() - 7);
                    }
                    if (isAPatch) {
                        if (d.ownsThisFile(fileNameBase)) applyPatch(filesPatch[i], new File(d.getDirectory() + fileNameBase), p.getDirectory()); else {
                            writeOnLog("Tried to patch an unexisting file: " + d.getDirectory() + fileNameBase + " (there must be a compatibility version problem...)");
                        }
                    } else {
                        d.addMeFile(filesPatch[i]);
                        writeOnLog("file added : " + filesPatch[i]);
                    }
                }
            }
        }
    }
}
