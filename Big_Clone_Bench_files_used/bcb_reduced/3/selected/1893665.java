package br.com.visualmidia.update;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import br.com.visualmidia.business.FileDescriptorMD5;
import br.com.visualmidia.core.Constants;

public class ListFilestoDownloadThroughCompareofMD5 {

    private ArrayList<FileDescriptorMD5> listOfFilesThatNeedToBeDownloaded;

    private List<File> listOfFiles;

    public ListFilestoDownloadThroughCompareofMD5() {
        listOfFilesThatNeedToBeDownloaded = new ArrayList<FileDescriptorMD5>();
        listOfFiles = new ArrayList<File>();
    }

    public ArrayList<FileDescriptorMD5> getOfFilesThatNeedToBeDownloaded() {
        return listOfFilesThatNeedToBeDownloaded;
    }

    public ArrayList<FileDescriptorMD5> getListofFilesThatHasDifferentMD5tobeDownloaded(ArrayList<FileDescriptorMD5> listofFilesfromServer, String path) {
        try {
            getFilesFromDirectory(path);
            if (listOfFiles.size() > 0) {
                for (FileDescriptorMD5 filefromServer : listofFilesfromServer) {
                    boolean filenotexistintheclient = true;
                    for (File fileInTheClient : listOfFiles) {
                        if (fileInTheClient.getName().equals(filefromServer.getName())) {
                            MessageDigest md = MessageDigest.getInstance("MD5");
                            BigInteger hash = new BigInteger(1, md.digest(fileInTheClient.toString().getBytes()));
                            if (!filefromServer.getMD5().equals(hash.toString(16))) {
                                filenotexistintheclient = false;
                                listOfFilesThatNeedToBeDownloaded.add(filefromServer);
                            }
                        }
                    }
                    if (filenotexistintheclient) {
                        listOfFilesThatNeedToBeDownloaded.add(filefromServer);
                    }
                }
            } else {
                listOfFilesThatNeedToBeDownloaded.addAll(listofFilesfromServer);
            }
            for (FileDescriptorMD5 filechec : listOfFilesThatNeedToBeDownloaded) {
                System.out.println(filechec.getLocation() + " " + filechec.getName());
            }
            return listOfFilesThatNeedToBeDownloaded;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getFilesFromDirectory(String path) {
        File filePath = new File(path);
        File[] filesInTheUpdate = filePath.listFiles();
        for (File file : filesInTheUpdate) {
            if (file.isDirectory()) {
                if (!((boolean) file.getPath().toLowerCase().contains("data"))) {
                    getFilesFromDirectory(path + Constants.FILE_SEPARATOR + file.getName());
                }
            } else {
                if (!((boolean) file.getPath().toLowerCase().contains("data"))) {
                    listOfFiles.add(file);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public ArrayList<FileDescriptorMD5> getListOfFilesFromServer(String path) {
        ArrayList<FileDescriptorMD5> listOfFilesFromTheServer = new ArrayList<FileDescriptorMD5>();
        getFilesFromDirectory(path);
        for (File file : listOfFiles) {
            System.out.println("name " + file.getName() + "  " + file.getAbsolutePath());
            listOfFilesFromTheServer.add(new FileDescriptorMD5(file));
        }
        return listOfFilesFromTheServer;
    }
}
