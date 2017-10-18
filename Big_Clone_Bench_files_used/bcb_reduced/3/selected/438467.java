package net.sf.tomcatdeployer.service.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsoleStream;
import net.sf.tomcatdeployer.service.HelperService;

public class SyncClient {

    private String endPoint;

    private String contextRoot;

    private String baseDir;

    private ArrayList filesUploaded = new ArrayList();

    private ArrayList filesRemoved = new ArrayList();

    private List modifiedWebContent = new ArrayList();

    private List remoteToDelete = new ArrayList();

    private List modifiedOutput = new ArrayList();

    public List getModifiedWebContent() {
        return modifiedWebContent;
    }

    public Date testConnection() {
        HelperService c;
        log("Context = " + contextRoot);
        c = null;
        try {
            c = ClientFactory.getService(endPoint);
            return c.getServerTime();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void setModifiedWebContent(List modifiedWebContent) {
        this.modifiedWebContent = modifiedWebContent;
    }

    public List getRemoteToDelete() {
        return remoteToDelete;
    }

    public void setRemoteToDelete(List remoteToDelete) {
        this.remoteToDelete = remoteToDelete;
    }

    public List getModifiedOutput() {
        return modifiedOutput;
    }

    public void setModifiedOutput(List modifiedOutput) {
        this.modifiedOutput = modifiedOutput;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getClassesDir() {
        return classesDir;
    }

    public void setClassesDir(String classesDir) {
        this.classesDir = classesDir;
    }

    private String classesDir;

    public SyncClient() {
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public void detect() {
        this.modifiedOutput.clear();
        this.modifiedWebContent.clear();
        this.remoteToDelete.clear();
        HelperService c;
        log("Context = " + contextRoot);
        c = null;
        try {
            c = ClientFactory.getService(endPoint);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Date remoteDate = c.getServerTime();
        Date localDate = new Date();
        long remoteFastOffset = remoteDate.getTime() - localDate.getTime();
        Map remote = toMap(c.listFiles(contextRoot));
        Collection values = remote.values();
        log("Listing remote files.");
        for (Iterator iter = values.iterator(); iter.hasNext(); ) {
            log(iter.next().toString());
        }
        List webContent = FileLister.listFiles(baseDir);
        List output = FileLister.listFiles(classesDir);
        log("Listing local web content folder");
        for (Iterator iter = webContent.iterator(); iter.hasNext(); ) {
            log(iter.next().toString());
        }
        log("Listing local classes folder");
        for (Iterator iter = output.iterator(); iter.hasNext(); ) {
            log(iter.next().toString());
        }
        String name;
        for (Iterator iter = output.iterator(); iter.hasNext(); ) {
            Map nextOutput = (Map) iter.next();
            name = (String) nextOutput.get("name");
            name = "/WEB-INF/classes" + name;
            nextOutput.put("name", name);
        }
        remoteToDelete = new ArrayList();
        modifiedOutput = new ArrayList();
        for (Iterator iter = webContent.iterator(); iter.hasNext(); ) {
            Map nextFile = (Map) iter.next();
            name = (String) nextFile.get("name");
            System.out.println("Checking next local file name: " + name);
            String type = (String) nextFile.get("type");
            long lastModified = ((Long) nextFile.get("lastModified")).longValue();
            long size = ((Long) nextFile.get("size")).longValue();
            if (!remote.containsKey(name)) {
                log("Local file " + name + " not found in remote.");
                modifiedWebContent.add(nextFile);
            } else {
                Map remoteDetails = (Map) remote.get(name);
                String remoteType = (String) nextFile.get("type");
                if (!remoteType.equals(type)) {
                    log("Local type " + type + " , remote type " + remoteType);
                    remoteToDelete.add(remoteDetails);
                    modifiedWebContent.add(nextFile);
                } else {
                    long remoteLastModified = ((Long) remoteDetails.get("lastModified")).longValue();
                    long remoteSize = ((Long) remoteDetails.get("size")).longValue();
                    log("Local last modified: " + lastModified + ", remote last modified: " + remoteLastModified);
                    if (!"D".equals(type)) {
                        if (size != remoteSize) {
                            log("Local size: " + size + ", remote size: " + remoteSize);
                            modifiedWebContent.add(nextFile);
                        } else if (remoteLastModified - lastModified < remoteFastOffset) {
                            String remoteSum = c.checkSum(contextRoot, name);
                            String localSum = checkSum(baseDir, name);
                            log("Local sum: " + localSum);
                            log("Remote sum: " + remoteSum);
                            if (!localSum.equals(remoteSum)) modifiedWebContent.add(nextFile);
                        }
                    }
                }
            }
        }
        for (Iterator iter = output.iterator(); iter.hasNext(); ) {
            Map nextFile = (Map) iter.next();
            System.out.println("Checking next output file:" + nextFile);
            name = (String) nextFile.get("name");
            String type = (String) nextFile.get("type");
            long lastModified = ((Long) nextFile.get("lastModified")).longValue();
            long size = ((Long) nextFile.get("size")).longValue();
            if (!remote.containsKey(name)) {
                log("Local file " + name + " not found in remote.");
                modifiedOutput.add(nextFile);
            } else {
                Map remoteDetails = (Map) remote.get(name);
                String remoteType = (String) nextFile.get("type");
                if (!remoteType.equals(type)) {
                    log("Local type " + type + " , remote type " + remoteType);
                    remoteToDelete.add(remoteDetails);
                    modifiedOutput.add(nextFile);
                } else {
                    long remoteLastModified = ((Long) remoteDetails.get("lastModified")).longValue();
                    long remoteSize = ((Long) remoteDetails.get("size")).longValue();
                    log("Local last modified: " + lastModified + ", remote last modified: " + remoteLastModified);
                    if (!"D".equals(type)) {
                        if (size != remoteSize) {
                            log("Local size: " + size + ", remote size: " + remoteSize);
                            modifiedOutput.add(nextFile);
                        } else if (remoteLastModified - lastModified < remoteFastOffset - 1000L) {
                            String remoteSum = c.checkSum(contextRoot, name);
                            String localSum = checkSum(baseDir, name);
                            log("Local sum: " + localSum);
                            log("Remote sum: " + remoteSum);
                            if (!localSum.equals(remoteSum)) modifiedOutput.add(nextFile);
                        }
                    }
                }
            }
        }
        System.out.println("Modified web content: " + modifiedWebContent);
        System.out.println("Modified output: " + modifiedOutput);
        System.out.println("CHecking remote extra file, delete if necessary.");
        for (Iterator iter = webContent.iterator(); iter.hasNext(); ) {
            Map next = (Map) iter.next();
            if (remote.containsKey(next.get("name"))) {
                System.out.println("Remote contains " + next.get("name"));
                remote.remove(next.get("name"));
            } else {
                System.out.println("Remote do not have " + next.get("name"));
            }
        }
        for (Iterator iter = output.iterator(); iter.hasNext(); ) {
            Map next = (Map) iter.next();
            if (remote.containsKey(next.get("name"))) {
                System.out.println("Remote contains " + next.get("name"));
                remote.remove(next.get("name"));
            } else {
                System.out.println("Remote do not have " + next.get("name"));
            }
        }
        for (Iterator iter = remote.entrySet().iterator(); iter.hasNext(); ) {
            java.util.Map.Entry next = (java.util.Map.Entry) iter.next();
            if (!"/WEB-INF/classes".equalsIgnoreCase((String) next.getKey())) remoteToDelete.add(next.getValue());
        }
    }

    public void notify(Object x, String message) {
        if (x instanceof MessageConsoleStream) {
            ((MessageConsoleStream) x).println(message);
        } else if (x instanceof IProgressMonitor) {
            IProgressMonitor monitor = (IProgressMonitor) x;
            monitor.subTask(message);
        }
    }

    public void notify(Object x, int amount) {
        if (x instanceof IProgressMonitor) {
            IProgressMonitor monitor = (IProgressMonitor) x;
            monitor.worked(amount);
        }
    }

    @SuppressWarnings("unchecked")
    public void doSync(Object monitor) {
        filesUploaded.clear();
        filesRemoved.clear();
        HelperService c;
        log("Context = " + contextRoot);
        c = null;
        try {
            c = ClientFactory.getService(endPoint);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        for (Iterator iter = remoteToDelete.iterator(); iter.hasNext(); ) {
            System.out.println("Remote to delete: " + iter.next());
        }
        for (Iterator iter = modifiedWebContent.iterator(); iter.hasNext(); ) {
            if (monitor instanceof IProgressMonitor) {
                if (((IProgressMonitor) monitor).isCanceled()) {
                    break;
                }
            }
            FileInputStream fis;
            Map next = (Map) iter.next();
            System.out.println("Uploading " + toOsString(contextRoot + next.get("name")));
            if ("D".equals(next.get("type"))) {
                c.mkdir(contextRoot, (String) next.get("name"));
                notify(monitor, 1);
                continue;
            }
            fis = null;
            try {
                fis = new FileInputStream(toOsString(baseDir + next.get("name")));
                notify(monitor, "Uploading " + next.get("name"));
                c.saveAs(contextRoot, (String) next.get("name"), fis);
                notify(monitor, 1);
                notify(monitor, "Uploaded " + next.get("name"));
                filesUploaded.add(next.get("name"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                continue;
            }
        }
        for (Iterator iter = modifiedOutput.iterator(); iter.hasNext(); ) {
            if (monitor instanceof IProgressMonitor) {
                if (((IProgressMonitor) monitor).isCanceled()) {
                    break;
                }
            }
            FileInputStream fis;
            Map next = (Map) iter.next();
            System.out.println("Uploading " + toOsString(classesDir + ((String) next.get("name")).substring(16)));
            if ("D".equals(next.get("type"))) {
                c.mkdir(contextRoot, (String) next.get("name"));
                notify(monitor, 1);
                continue;
            }
            fis = null;
            try {
                fis = new FileInputStream(toOsString(classesDir + ((String) next.get("name")).substring(16)));
                notify(monitor, "Uploading " + next.get("name"));
                c.saveAs(contextRoot, (String) next.get("name"), fis);
                notify(monitor, 1);
                notify(monitor, "Uploaded " + next.get("name"));
                filesUploaded.add(next.get("name"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                continue;
            }
        }
        for (Iterator iter = remoteToDelete.iterator(); iter.hasNext(); ) {
            if (monitor instanceof IProgressMonitor) {
                if (((IProgressMonitor) monitor).isCanceled()) {
                    break;
                }
            }
            Map next = (Map) iter.next();
            System.out.println("Deleting " + next.get("name"));
            notify(monitor, "Removing from server " + next.get("name"));
            c.removeFile(contextRoot, (String) next.get("name"));
            notify(monitor, "Removed from server " + next.get("name"));
            notify(monitor, 1);
            filesRemoved.add(next.get("name"));
        }
        System.out.println("Reloading context");
        notify(monitor, "Reloading context:" + contextRoot);
        System.out.println("Result: " + c.reloadContext(contextRoot));
        notify(monitor, "Reloaded context:" + contextRoot);
        notify(monitor, "Done");
        notify(monitor, 1);
        return;
    }

    @SuppressWarnings("unchecked")
    public List getFilesUploaded() {
        return Collections.unmodifiableList(filesUploaded);
    }

    @SuppressWarnings("unchecked")
    public List getFilesRemoved() {
        return Collections.unmodifiableList(filesRemoved);
    }

    public String toOsString(String x) {
        x = x.replace('/', File.separatorChar);
        x = x.replace('\\', File.separatorChar);
        return x;
    }

    @SuppressWarnings("unchecked")
    public Map toMap(List remote) {
        Map remoteFileMap = new HashMap();
        Map next;
        for (Iterator iter = remote.iterator(); iter.hasNext(); remoteFileMap.put(next.get("name"), next)) next = (Map) iter.next();
        return remoteFileMap;
    }

    private void log(Object x) {
        System.out.println(x);
    }

    private String byteArrayToHexString(byte in[]) {
        byte ch = 0;
        int i = 0;
        if (in == null || in.length <= 0) return null;
        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        StringBuffer out = new StringBuffer(in.length * 2);
        for (; i < in.length; i++) {
            ch = (byte) (in[i] & 0xf0);
            ch >>>= 4;
            ch &= 0xf;
            out.append(pseudo[ch]);
            ch = (byte) (in[i] & 0xf);
            out.append(pseudo[ch]);
        }
        String rslt = new String(out);
        return rslt;
    }

    public String checkSum(String rootPath, String path) {
        File f;
        if (rootPath.endsWith(File.separator)) rootPath = rootPath.substring(0, rootPath.length() - 1);
        rootPath = rootPath + path;
        f = new File(rootPath);
        if (f.isDirectory() || !f.exists() || !f.canRead()) return "0";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(f);
            byte buffer[] = new byte[4096];
            do {
                int read = fis.read(buffer);
                if (read == -1) break;
                if (read != 0) md.update(buffer, 0, read);
            } while (true);
            fis.close();
        } catch (Exception ex) {
            return "0";
        }
        return byteArrayToHexString(md.digest());
    }

    public boolean reload() {
        HelperService c;
        log("Context = " + contextRoot);
        c = null;
        try {
            c = ClientFactory.getService(endPoint);
            return c.reloadContext(this.getContextRoot());
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
