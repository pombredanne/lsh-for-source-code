import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class MD5Maker extends Thread {

    private File myFile = null;

    private PatchCreator myMaster = null;

    public MD5Maker(PatchCreator cm, File f) {
        myFile = f;
        myMaster = cm;
    }

    public void run() {
        myMaster.writeOnMD5File(myFile, getMD5(myFile));
        myMaster.releaseToken();
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
}
