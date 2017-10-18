package rezine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;

public class Utils {

    public static String checksum(File file) {
        return checksum(file, "SHA1");
    }

    /**
	 * Return a hash of a file;
	 * @param file the file to hash
	 * @param the hash method (SHA1,MD5, ...)
	 * @return the hash of the file or null if the file does not exist.
	 */
    public static String checksum(File file, String method) {
        if (!file.exists()) return null;
        try {
            InputStream fin = new FileInputStream(file);
            java.security.MessageDigest md5er = MessageDigest.getInstance(method);
            byte[] buffer = new byte[1024];
            int read;
            do {
                read = fin.read(buffer);
                if (read > 0) md5er.update(buffer, 0, read);
            } while (read != -1);
            fin.close();
            byte[] digest = md5er.digest();
            if (digest == null) return null;
            String strDigest = "0x";
            for (int i = 0; i < digest.length; i++) {
                strDigest += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1).toUpperCase();
            }
            return strDigest;
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * Copy a file on the disk. all data in the destination file will be overwritten.
	 * @param in from this file
	 * @param out to this new file
	 * @throws IOException
	 */
    public static void copy(File in, File out) throws IOException {
        FileChannel ic = new FileInputStream(in).getChannel();
        FileChannel oc = new FileOutputStream(out).getChannel();
        ic.transferTo(0, ic.size(), oc);
        ic.close();
        oc.close();
    }

    /**
	 * Find the name and the version of the library
	 * @param f a library
	 * @return The name and the version of this library. 
	 * 		   The version 1.0 is send if none has been found.  
	 */
    public static String[] parseLib(File f) {
        String name = f.getName();
        boolean snap = false;
        if (name.endsWith("SNAPSHOT.jar") || name.endsWith("SNAPSHOT.JAR")) {
            name = name.substring(0, name.length() - 12);
            snap = true;
        } else {
            name = name.substring(0, name.length() - 4);
        }
        int k = name.lastIndexOf('-');
        String version = "1.0";
        if (k != -1) {
            version = name.substring(k + 1);
            if (version.matches(".*\\d.*")) {
                name = name.substring(0, k);
            } else {
                version = "1.0";
            }
        }
        return new String[] { name, version, "" + snap };
    }

    /**
	 * 
	 * @param f
	 * @param repo
	 * @return the groupid, artifactid, version, snap
	 */
    public static String[] parseLib(File f, String repo) {
        String[] artifact = parseLib(f);
        String path = f.getAbsolutePath().toLowerCase();
        String result[] = new String[] { "", artifact[0], artifact[1], artifact[2] };
        repo = repo.toLowerCase();
        if (path.startsWith(repo)) {
            path = path.substring(repo.length());
            String[] res = path.split("\\" + File.separatorChar);
            String groupid = "";
            for (int i = 0; i < res.length - 3; i++) {
                groupid += res[i] + ".";
            }
            int start = 0;
            if (groupid.startsWith(".")) {
                start = 1;
            }
            result[0] = groupid.substring(start, groupid.length() - 1);
        }
        return result;
    }

    /**
	 *  Recursive list of a directory with filter
	 */
    public static List<File> rlist(File dir, Artifact[] libs, String filter) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            LinkedList<File> rfiles = new LinkedList<File>();
            for (File file : files) {
                rfiles.addAll(rlist(file, libs, filter));
            }
            return rfiles;
        }
        List<File> l = new LinkedList<File>();
        if (dir.getAbsolutePath().matches(filter)) {
            return l;
        }
        for (Artifact lib : libs) {
            if (lib != null) {
                if (dir.getAbsolutePath().matches(".*" + lib.getName() + ".*") && !l.contains(dir)) {
                    l.add(dir);
                }
            }
        }
        return l;
    }
}
