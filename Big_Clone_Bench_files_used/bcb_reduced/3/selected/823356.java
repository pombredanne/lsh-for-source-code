package org.opencdspowered.opencds.core.project;

import org.opencdspowered.opencds.core.download.*;
import org.opencdspowered.opencds.core.index.*;
import org.opencdspowered.opencds.core.logging.*;
import java.net.URL;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Iterator;

/**
 * This class makes downloading a project alot easier.
 *
 * @author  Lars 'Levia' Wesselius
*/
public class ProjectDownloader {

    private DownloadManager m_DownloadManager;

    public ProjectDownloader(DownloadManager mgr) {
        m_DownloadManager = mgr;
    }

    /**
     * Download a project.
     * 
     * @param   project The project to download.
    */
    public void downloadProject(Project project) {
        Executable exe = project.getLatestExecutable();
        IndexReader reader = new IndexReader();
        String fileUrl = exe.getFileUrl();
        try {
            if (fileUrl.endsWith(".xml")) {
                if (!reader.readXML(new URL(exe.getFileUrl()))) {
                    int index = exe.getName().lastIndexOf(".");
                    String fileName = exe.getName().substring(0, index);
                    ReleaseDownload download = null;
                    if (exe.getInstallTo().equals("$ProjectRoot")) {
                        String downloadToDir = "data/" + project.getName() + "/" + fileName + "/";
                        File file = new File(downloadToDir);
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        File fileTo = new File(downloadToDir + exe.getName());
                        if (!fileTo.exists()) {
                            try {
                                fileTo.createNewFile();
                            } catch (Exception ex) {
                                Logger.getInstance().logException(ex);
                            }
                        }
                        download = m_DownloadManager.addDownload(exe.getFileUrl(), downloadToDir + exe.getName(), exe);
                    } else {
                        File dir = new File(exe.getInstallTo());
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        download = m_DownloadManager.addDownload(exe.getFileUrl(), exe.getInstallTo(), exe);
                    }
                    m_DownloadManager.enqueueDownload(download);
                } else {
                    String installTo = "";
                    if (exe.getInstallTo().equals("$ProjectRoot")) {
                        int index = exe.getName().lastIndexOf(".");
                        String fileName = exe.getName().substring(0, index);
                        String downloadToDir = "data/" + project.getName() + "/" + fileName;
                        File file = new File(downloadToDir);
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        File fileTo = new File(downloadToDir + exe.getName());
                        if (!fileTo.exists()) {
                            try {
                                fileTo.createNewFile();
                            } catch (Exception ex) {
                                Logger.getInstance().logException(ex);
                            }
                        }
                        installTo = downloadToDir;
                    } else {
                        File dir = new File(exe.getInstallTo());
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        installTo = exe.getInstallTo();
                    }
                    int slashIndex = exe.getFileUrl().lastIndexOf("/");
                    String baseUrl = exe.getFileUrl().substring(0, slashIndex);
                    ReleaseDownload relDl = new ReleaseDownload(baseUrl, exe);
                    for (Iterator<Index> it = reader.getIndexes(); it.hasNext(); ) {
                        Index index = it.next();
                        if (checkHash(index, installTo)) {
                            Download dl = new Download(baseUrl + index.getURL(), installTo + index.getURL());
                            relDl.addSize(index.getFileSize());
                            System.out.println("baseURL: " + baseUrl);
                            System.out.println("IndexURL: " + index.getURL());
                            System.out.println("InstallTo: " + installTo);
                            relDl.addDownload(dl);
                        }
                    }
                    m_DownloadManager.enqueueDownload(relDl);
                }
            } else {
                int index = exe.getName().lastIndexOf(".");
                String fileName = exe.getName().substring(0, index);
                String downloadToDir = "data/" + project.getName() + "/" + fileName + "/";
                File file = new File(downloadToDir);
                if (!file.exists()) {
                    file.mkdirs();
                }
                File fileTo = new File(downloadToDir + exe.getName());
                if (!fileTo.exists()) {
                    try {
                        fileTo.createNewFile();
                    } catch (Exception ex) {
                        Logger.getInstance().logException(ex);
                    }
                }
                ReleaseDownload download = m_DownloadManager.addDownload(exe.getFileUrl(), downloadToDir + exe.getName(), exe);
                m_DownloadManager.enqueueDownload(download);
            }
            m_DownloadManager.startQueue();
        } catch (Exception e) {
            Logger.getInstance().logException(e, false);
            return;
        }
    }

    private boolean checkHash(Index index, String installTo) {
        File localFile = new File(installTo + index.getURL());
        if (!localFile.exists()) {
            return true;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            InputStream is = new FileInputStream(localFile);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            is.close();
            System.out.println("File: " + index.getURL());
            System.out.println("Index: " + index.getHash());
            System.out.println("Output: " + output);
            if (!output.equals(index.getHash())) {
                System.out.println("return true");
                return true;
            }
            return false;
        } catch (Exception e) {
            Logger.getInstance().logException(e, false);
        }
        return true;
    }
}
