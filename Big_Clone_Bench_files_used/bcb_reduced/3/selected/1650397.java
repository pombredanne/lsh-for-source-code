package jp.ne.nifty.iga.midori.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;

public class JMdFileUtil {

    public static byte[] getMd5(File fileLook) throws IOException {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            DigestInputStream digestIn = new DigestInputStream(new BufferedInputStream(new FileInputStream(fileLook)), md5);
            digestIn.on(true);
            for (; ; ) {
                int iRead = digestIn.read();
                if (iRead < 0) {
                    break;
                }
            }
            digestIn.close();
            return md5.digest();
        } catch (IOException ex) {
            System.out.println(ex.toString());
            ex.printStackTrace();
        } catch (java.security.NoSuchAlgorithmException ex) {
            System.out.println(ex.toString());
            ex.printStackTrace();
        }
        return null;
    }

    /**
	 * test is implemented.
	 */
    public static String getUriDirectory(String strUri) {
        int iFind = strUri.indexOf("://");
        if (iFind > 0) {
            strUri = strUri.substring(iFind + 3);
        }
        iFind = strUri.indexOf('/');
        if (iFind > 0) {
            strUri = strUri.substring(iFind);
        } else {
            strUri = ".";
        }
        return strUri;
    }

    /**
	 * test is implemented.
	 */
    public static String getUriServer(String strUri) {
        int iFind = strUri.indexOf("//");
        if (iFind > 0) {
            strUri = strUri.substring(iFind + 2);
        }
        iFind = strUri.indexOf('/');
        if (iFind > 0) {
            strUri = strUri.substring(0, iFind);
        }
        return strUri;
    }

    public static void listDirectory(ArrayList listFiles, String strDirectory) {
        listDirectoryInternal(listFiles, strDirectory, ".");
    }

    protected static void listDirectoryInternal(ArrayList listFiles, String strBaseDirectory, String strDirectory) {
        File fileCheckDirectory = new File(strBaseDirectory + "/" + strDirectory);
        File[] fileList = fileCheckDirectory.listFiles();
        if (fileList == null) {
            return;
        }
        for (int index = 0; index < fileList.length; index++) {
            File fileCheckFile = fileList[index];
            if (fileCheckFile.isDirectory() == true) {
                String strNextDirectory = strDirectory + "/";
                if (strNextDirectory.equals("./")) {
                    strNextDirectory = "";
                }
                strNextDirectory += fileCheckFile.getName();
                listDirectoryInternal(listFiles, strBaseDirectory, strNextDirectory);
            } else {
                listFiles.add(new JMdFileItem(fileCheckFile, strDirectory));
            }
        }
    }
}
