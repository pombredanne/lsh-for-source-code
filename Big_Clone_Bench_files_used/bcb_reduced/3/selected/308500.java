package hu.sztaki.lpds.pgportal.services.utils;

import java.io.*;
import java.net.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import hu.sztaki.lpds.monitor.tracefile.*;
import hu.sztaki.lpds.monitor.*;
import hu.sztaki.lpds.pgportal.services.pgrade.SZGJob;
import hu.sztaki.lpds.pgportal.services.pgrade.SZGWorkflow;
import hu.sztaki.lpds.pgportal.services.utils.timeoutExecutor.*;

public class MiscUtils {

    private MiscUtils() {
    }

    private static final Runtime runtime = Runtime.getRuntime();

    /**
	 * Creates zip file containing the output files of the workflow.
	 * 
	 * @param username
	 * @param workflow
	 *            workflow id
	 * @return False if creation failed, or when non of the output files are
	 *         available.
	 */
    public static String createOutputsZip(SZGWorkflow wf) {
        System.out.println("MiscUtils.createOutputsZip(" + wf.getUserId() + ", " + wf.getId() + ")");
        String prefix = PropertyLoader.getPrefixDir();
        File outputsZipDir = null;
        try {
            outputsZipDir = new File(PropertyLoader.getPrefixDir() + "/tmp/" + wf.getUserId() + "/" + wf.getId());
            if (!outputsZipDir.exists()) {
                outputsZipDir.mkdirs();
            }
            List<String> filesToInclude = MiscUtils.getOutputFilesList(wf.getUserId().toString(), wf.getId().toString());
            if (filesToInclude == null) return null;
            File zipToCreate = File.createTempFile("wf_out", ".zip", outputsZipDir);
            File toBeZipped = new File(prefix + "/users/" + wf.getUserId() + "/" + wf.getId() + "_files");
            boolean success = MiscUtils.zipRecursively(toBeZipped, zipToCreate, filesToInclude);
            if (success) {
                return zipToCreate.getAbsolutePath();
            } else {
                return null;
            }
        } catch (Exception ex) {
            System.out.println("ex = " + ex);
            return null;
        }
    }

    /**
	 * Creates zip file containing the output files of the job.
	 * 
	 * @param username
	 * @param workflow
	 *            workflow id
	 * @return False if creation failed, or when non of the output files are
	 *         available.
	 */
    public static String createJobsOutputsZip(SZGJob job) {
        System.out.println("MiscUtils.createOutputsZip(" + job.getUserId() + ", " + job.getWorkflowId() + "," + job.getName() + ")");
        String prefix = PropertyLoader.getPrefixDir();
        File outputsZipDir = null;
        try {
            outputsZipDir = new File(PropertyLoader.getPrefixDir() + "/tmp/" + job.getUserId() + "/" + job.getWorkflowId() + "/" + job.getId());
            if (!outputsZipDir.exists()) {
                outputsZipDir.mkdirs();
            }
            List<String> filesToInclude = job.getOutputFileList();
            if (filesToInclude == null) return null;
            File zipToCreate = File.createTempFile("job_out", ".zip", outputsZipDir);
            File toBeZipped = new File(prefix + "/users/" + job.getUserId() + "/" + job.getWorkflowId() + "_files/" + job.getName());
            boolean success = MiscUtils.zipRecursively(toBeZipped, zipToCreate, filesToInclude);
            if (success) {
                return zipToCreate.getAbsolutePath();
            } else {
                return null;
            }
        } catch (Exception ex) {
            System.out.println("ex = " + ex);
            return null;
        }
    }

    /**
	 * Creates zip file recursively adding files from dir/file. Empty
	 * directories will not be added. <br>
	 * 
	 * @param toBeZipped
	 *            The file's absolute path which is to be zipped.
	 * @param zipToCreate
	 *            The zip's absolute path which is to be created. If this file
	 *            already exists, it will be renamed to '{zipToCreate}.bak'.
	 * @param filesToInclude
	 *            Absolute paths of the files to be included.
	 *            <ul>
	 *            <li>Pass 'null', if each file is to be included.</li>
	 *            <li>If {filesToInclude} is not null, each file under
	 *            toBeZipped will be added to zip file if {filesToInclude}
	 *            contains the file's absolute path.</li>
	 *            </ul>
	 * @return True on success.
	 */
    private static boolean zipRecursively(File toBeZipped, File zipToCreate, List<String> filesToInclude) {
        if (zipToCreate.exists()) {
            zipToCreate.delete();
        }
        if (!zipToCreate.getParentFile().exists()) {
            if (!zipToCreate.getParentFile().mkdirs()) {
                return false;
            }
        }
        List<String> files = new ArrayList<String>();
        MiscUtils.getAllFiles(files, toBeZipped);
        String parentDir = toBeZipped.getParentFile().getAbsolutePath();
        int entryNumber = 0;
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipToCreate));
            zos.setLevel(Deflater.BEST_COMPRESSION);
            for (ListIterator<String> li = files.listIterator(); li.hasNext(); ) {
                File toBeAdded = new File("" + li.next());
                if (filesToInclude != null) {
                    if (filesToInclude.contains(toBeAdded.getAbsolutePath())) {
                        System.out.println("MiscUtils.zipRecursively(..) -  contains, OK:" + toBeAdded.getAbsolutePath());
                        MiscUtils.addToZip(parentDir, toBeAdded, zos);
                        entryNumber++;
                    }
                } else {
                    MiscUtils.addToZip(parentDir, toBeAdded, zos);
                    entryNumber++;
                }
            }
            if (entryNumber == 0) {
                zos = null;
                zipToCreate.delete();
                System.out.println("MiscUtils.zipRecursively(..) - No entry added. Zip file have not been created.");
                return false;
            }
            zos.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean addToZip(String addFileFromThisDir, File addThis, ZipOutputStream zos) {
        if (addThis.isDirectory()) return false;
        String entryPath;
        String addThisPath = addThis.getAbsolutePath();
        if (!addThisPath.startsWith(addFileFromThisDir)) {
            System.out.println("MiscUtils.addToZip(..) -- BAJ!!");
            return false;
        } else {
            entryPath = addThisPath.substring(addFileFromThisDir.length() + 1);
        }
        try {
            zos.putNextEntry(new ZipEntry(entryPath));
            FileInputStream input = new FileInputStream(addThis);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * Adds all files' absolute path found under file to the List.
	 * 
	 * @param l
	 *            The List for storing absolute paths.
	 * @param file
	 *            The file which will be recursively processed.
	 */
    private static void getAllFiles(List<String> l, File file) {
        if (file.exists()) {
            l.add(file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                getAllFiles(l, children[i]);
            }
        }
    }

    public static boolean isOutputDefined(String username, String workflow) {
        String prefix = PropertyLoader.getPrefixDir();
        File outputData = new File(prefix + "/users/" + username + "/" + workflow + "_files/outputFilesRemotePath.dat");
        return outputData.exists() && outputData.length() > 0;
    }

    /**
	 * Eliminates multiple separators from the given path, and replaces
	 * back-slashes with slashes.
	 * 
	 * @param path
	 * @return
	 */
    protected static String correctAbsPath(String path) {
        StringBuffer res = new StringBuffer();
        path = path.replace('\\', '/');
        String[] elements = path.split("/");
        if (elements[0].equals("")) res.append("/");
        for (int i = 0; i < elements.length; i++) {
            if (!elements[i].equals("")) {
                res.append(elements[i] + "/");
            }
        }
        return res.toString();
    }

    private static List<String> getOutputFilesList(String username, String workflow) {
        String prefix = PropertyLoader.getPrefixDir();
        File outputListFile = new File(prefix + "/users/" + username + "/" + workflow + "_files/outputFilesRemotePath.dat");
        List<String> files = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(outputListFile));
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!(line.equals(""))) {
                    files.add(line);
                }
            }
        } catch (Exception ex) {
            return null;
        }
        if (files.size() == 0) return null;
        return files;
    }

    public static HashMap<String, List<String>> getJobOutputFiles(String username, String workflow) {
        HashMap<String, List<String>> retValue = null;
        List<String> fileList = getOutputFilesList(username, workflow);
        if (fileList != null) {
            retValue = new HashMap<String, List<String>>();
            for (int i = 0; i < fileList.size(); i++) {
                List<String> jobOutputList;
                String filePath = (String) fileList.get(i);
                String[] fileSplit = filePath.split("\\/");
                String jobName = fileSplit[fileSplit.length - 3];
                System.out.println("MiscUtils.getJobOutputFiles()" + jobName + ":" + filePath);
                jobOutputList = retValue.containsKey(jobName) ? retValue.get(jobName) : new ArrayList<String>();
                jobOutputList.add(filePath);
                retValue.put(jobName, jobOutputList);
            }
        }
        return retValue;
    }

    /**
	 * Generates UUID.
	 */
    public static String generateUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
	 * Checks the availability of Mercury at a given host.
	 * 
	 * @param host
	 *            The name of the host, eg: 'n0.hpcc.sztaki.hu'
	 * @return false If connection fails for any reason or a specified timeout
	 *         expires.
	 */
    public static boolean isMonitorAvailable(String host) {
        System.out.println("MiscUtils.isMonitorAvailable(" + host + ") called");
        if (!TraceFileMonitor.isMonitoringOn()) {
            return true;
        }
        String port = "";
        try {
            port = TFPropertyLoader.getInstance().getPortWithColon((String) host);
        } catch (Exception ex) {
            System.out.println("MiscUtil.isMonitorAvailable(" + host + ") - Exception: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        System.out.println("port = " + port);
        if (!MiscUtils.isHostAlive(host, port.substring(1))) {
            return false;
        }
        long timeout = 1000;
        final String hostF = "monp://" + host + port;
        System.out.println("MiscUtils.isMonitorAvailable(" + host + ") - host with port:" + hostF);
        TimeoutExecutor te = new TimeoutExecutor();
        Function f = new Function() {

            public Object execute() {
                MonitorConsumer mc = null;
                try {
                    mc = new MonitorConsumer(hostF);
                    mc.close();
                    mc = null;
                    System.out.println("MiscUtils.isMonitorAvailable(" + hostF + "): OK");
                } catch (Exception ex) {
                    System.out.println("MiscUtils.isMonitorAvailable(" + hostF + "): Exception - " + ex.getMessage());
                    mc = null;
                    return TimeoutExecutor.RESULT_AT_TIMEOUT;
                }
                System.out.println("MiscUtils.isMonitorAvailable(" + hostF + "): true");
                mc = null;
                return new Boolean(true);
            }
        };
        Object result = null;
        try {
            result = te.executeFunctionWithTimeout(f, timeout);
        } catch (FunctionTimeoutException ftex) {
            System.out.println("MiscUtils.isMonitorAbailable() - ftex:" + ftex.getMessage());
            ftex.printStackTrace();
            return false;
        }
        if (result != TimeoutExecutor.RESULT_AT_TIMEOUT) {
            return true;
        }
        return false;
    }

    private static boolean isHostAlive(String aHost, String aPort) {
        MiscUtils.printlnLog(MiscUtils.class.getName() + ".isHostAlive(" + aHost + "," + aPort + ")", "START");
        int port;
        try {
            port = Integer.parseInt(aPort);
        } catch (NumberFormatException nfe) {
            MiscUtils.printlnLog(MiscUtils.class.getName() + ".isHostAlive(" + aHost + "," + aPort + ")", "ERROR: Wrong port format. - " + nfe.getMessage());
            MiscUtils.printlnLog(MiscUtils.class.getName() + ".isHostAlive(" + aHost + "," + aPort + ")", "END");
            return false;
        }
        try {
            Socket s = new Socket();
            InetSocketAddress addr = new InetSocketAddress(aHost, port);
            s.connect(addr, 1000);
            s.close();
        } catch (IOException e) {
            MiscUtils.printlnLog(MiscUtils.class.getName() + ".isHostAlive(" + aHost + "," + aPort + ")", "ERROR: " + e.getMessage());
            MiscUtils.printlnLog(MiscUtils.class.getName() + ".isHostAlive(" + aHost + "," + aPort + ")", "END");
            return false;
        }
        MiscUtils.printlnLog(MiscUtils.class.getName() + ".isHostAlive(" + aHost + "," + aPort + ")", "END");
        return true;
    }

    public static boolean deleteFileRecursively(File f) {
        if (!f.exists()) return true;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            for (int i = 0; i < children.length; i++) {
                MiscUtils.deleteFileRecursively(children[i]);
            }
        }
        return f.delete();
    }

    /**
	 * Recursively copies file.
	 * 
	 * @param src
	 *            source
	 * @param dest
	 *            destination
	 * @param overwrite
	 *            true/false
	 * @return False if src doesn't exists, or when the copy fails.
	 */
    public static boolean copyFileRecursively(File src, File dest, boolean overwrite) {
        String destAbsPath = dest.getAbsolutePath();
        if (!src.exists()) return false;
        if (src.isDirectory()) {
            if (!dest.exists()) {
                if (!dest.mkdirs()) {
                    return false;
                }
            }
            File[] children = src.listFiles();
            for (int i = 0; i < children.length; i++) {
                String destPath = destAbsPath + "/" + children[i].getName();
                MiscUtils.copyFileRecursively(children[i], new File(destPath), overwrite);
            }
        } else {
            try {
                MiscUtils.copy(src.getAbsolutePath(), dest.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return true;
    }

    /**
	 * Copies file.
	 * 
	 * @param src
	 * @param dest
	 */
    public static void copy(String from, String to) throws IOException {
        System.out.println("MiscUtils.copy( " + from + ", " + to + " )");
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = makeBIS(from);
            File toF = new File(to);
            bos = makeBOS(toF);
            byte[] buf = new byte[4096];
            int nr;
            while ((nr = bis.read(buf, 0, buf.length)) > 0) {
                bos.write(buf, 0, nr);
            }
            bis.close();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bis != null) bis.close();
            if (bos != null) bos.close();
        }
    }

    /**
	 * This is a shorthand method for easily creating a BufferedOutputStream
	 * which is the most efficient way to write to a file - much faster than
	 * directy using a FileOutputStream.
	 * <p>
	 * Don`t forget to call close() on it when you`re done.
	 * 
	 * 
	 * @param file
	 *            the file to write to
	 * @return a buffered output stream on success
	 * @exception IOException
	 *                if there is a problem opening the file
	 * 
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see makeBIS
	 */
    private static BufferedOutputStream makeBOS(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        return new BufferedOutputStream(fos, 4096);
    }

    private static BufferedInputStream makeBIS(String fileName) throws IOException {
        FileInputStream fis = new FileInputStream(fileName);
        return new BufferedInputStream(fis, 4096);
    }

    public static String digestString(String s) {
        StringBuffer result = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes());
            for (int i = 0; i < b.length; i++) {
                result.append(b[i]);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return result.toString();
    }

    public static String readFileToStr(String filePath) {
        StringBuffer s = new StringBuffer();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null) {
                s.append(line + "\n");
            }
            br.close();
            return s.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static boolean writeStrToFile(String filePath, String s) {
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
            pw.print(s);
            pw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected static final String reg_replaceAll(final String regex, final String replacement, final String subject) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(subject);
        return matcher.replaceAll(replacement);
    }

    public static void usedMemoryPrinter(String invokedFrom) {
        long usedMem = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("MiscUtils.usedMemoryPrinter( " + invokedFrom + " ): " + usedMem);
    }

    protected static String getStaticSessionId(String username) {
        String sep = "_$._#";
        StringBuffer in = new StringBuffer("");
        for (int i = 0; i < username.length(); i++) {
            in.append(sep + username.charAt(i));
        }
        String digest = MiscUtils.digestString(in.toString());
        char[] bad = "-".toCharArray();
        char[] good = "0".toCharArray();
        for (int i = 0; i < bad.length; i++) {
            digest = digest.replace(bad[i], good[i]);
        }
        return digest;
    }

    /**
	 * Write log to the standard output (system.out).
	 * 
	 * @param path
	 *            The path of the function from where the function is called.
	 *            Format: 'package.Class.func()'
	 * 
	 * @param text
	 *            The text which is printed.
	 */
    public static void printlnLog(String path, String text) {
        java.util.Calendar c = Calendar.getInstance();
        System.out.println("[" + c.get(Calendar.YEAR) + "." + (c.get(Calendar.MONTH) + 1) + "." + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + "] " + path + "-" + text);
    }

    public static void printLog(String path, String text) {
        java.util.Calendar c = Calendar.getInstance();
        System.out.print("[" + c.get(Calendar.YEAR) + "." + (c.get(Calendar.MONTH) + 1) + "." + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + "] " + path + "-" + text);
    }

    public static String getSpaceSizeFromKB(long xKB) {
        return getSpaceSizeFromByte(xKB * 1024);
    }

    public static String getSpaceSizeFromByte(long xB) {
        String ret = "";
        DecimalFormat myFormatter = new DecimalFormat("###.###");
        double xBd = (double) xB;
        long Hi = 1;
        Hi = Hi << 60;
        long Pi = 1;
        Pi = Pi << 50;
        double Pd = (double) Pi;
        long Ti = 1;
        Ti = Ti << 40;
        double Td = (double) Ti;
        long Gi = 1;
        Gi = Gi << 30;
        double Gd = (double) Gi;
        long Mi = 1;
        Mi = Mi << 20;
        double Md = (double) Mi;
        long Ki = 1;
        Ki = Ki << 10;
        double Kd = (double) Ki;
        if ((xB >= Hi) || (xB < 0)) ret = "unreliable high"; else if (xB >= Pi) {
            ret = myFormatter.format(xBd / Pd) + " [PB]";
        } else if (xB >= Ti) {
            ret = myFormatter.format(xBd / Td) + " [TB]";
        } else if (xB >= Gi) {
            ret = myFormatter.format(xBd / Gd) + " [GB]";
        } else if (xB >= Mi) {
            ret = myFormatter.format(xBd / Md) + " [MB]";
        } else if (xB >= Ki) {
            ret = myFormatter.format(xBd / Kd) + " [KB]";
        } else ret = myFormatter.format(xB) + "  [B]";
        return ret;
    }

    public static long getDUDirSizeInBytes(File f) {
        try {
            Process p = Runtime.getRuntime().exec("du -s -b " + f.getAbsolutePath());
            p.waitFor();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String re = (r.readLine().split("\\s"))[0];
            r.close();
            return Long.parseLong(re);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    public static long getFileSizeInBytes(File f) {
        long size = 0;
        if (f.isFile()) {
            size = f.length();
        } else if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    size += files[i].length();
                } else if (files[i].isDirectory()) {
                    size += getFileSizeInBytes(files[i]);
                }
            }
        }
        return size;
    }

    public static boolean isHostValid(String host) {
        if (host == null) {
            return false;
        }
        if (host.equals("")) {
            return false;
        }
        String section = "([a-zA-Z\\d]){1}(([a-zA-Z\\d-])*([a-zA-Z\\d]){1})*";
        Pattern pattern = Pattern.compile("^(" + section + "\\." + section + ")(\\." + section + ")*$");
        Matcher matcher = pattern.matcher(host.toLowerCase());
        if (!matcher.find()) return false;
        return true;
    }

    public static boolean isPortValid(String port) {
        if (port == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("^(\\d){4}$");
        Matcher matcher = pattern.matcher(port);
        if (!matcher.find()) return false;
        return true;
    }

    public static void writeUserEmailAddress(String username, String emailAddress) {
        try {
            File emailFile = new File(PropertyLoader.getPrefixDir() + "/users/" + username, ".email");
            if (!emailFile.getParentFile().exists()) {
                emailFile.getParentFile().mkdirs();
            }
            if (writeStrToFile(emailFile.getAbsolutePath(), emailAddress)) {
                System.out.println("MiscUtils.writeUserEmailAddress(" + username + ", " + emailAddress + ") - SUCCESS");
            } else {
                System.out.println("MiscUtils.writeUserEmailAddress(" + username + ", " + emailAddress + ") - FAILED");
            }
        } catch (Exception e) {
            System.out.println("MiscUtils.writeUserEmailAddress(" + username + ", " + emailAddress + ") - ERROR:" + e.getMessage());
        }
    }
}
