package randres.kindle.model;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

public class SHA1 {

    public static String getHash(String message) throws NoSuchAlgorithmException {
        byte[] buffer = message.getBytes();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(buffer);
        byte[] digest = md.digest();
        String hash = "";
        for (byte aux : digest) {
            int b = aux & 0xff;
            if (Integer.toHexString(b).length() == 1) hash += "0";
            hash += Integer.toHexString(b);
        }
        return hash;
    }

    public static Vector<Item> searchItem(String rootPath, String relativePath, String pattern) throws NoSuchAlgorithmException {
        Vector<Item> results = new Vector<Item>();
        String path = (relativePath.length() > 0 ? rootPath + File.separatorChar + relativePath : rootPath);
        File rootDocuments = new File(path);
        for (File file : rootDocuments.listFiles()) {
            String fileRelativePath = (relativePath.length() > 0 ? relativePath + File.separatorChar : relativePath) + file.getName();
            if (file.isDirectory() && !file.getName().contains(".")) {
                results.addAll(searchItem(rootPath, fileRelativePath, pattern));
            } else {
                String absPath = file.getAbsolutePath();
                Item item = new FileItem(absPath, fileRelativePath);
                results.add(item);
            }
        }
        return results;
    }
}
