import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.HashMap;

/**
 * Integrity is test that will retrieve a file ( image or other file ) from
 * a predefined target and compare the hash of that file to a given hash.
 * This determines if the file was retrieved succesfully.
 * 
 */
public class Integrity implements iTest {

    private String target, hash = null;

    private HashMap result = new HashMap();

    public Integrity(HashMap paramMap) {
        this.target = (String) paramMap.get("target");
        this.hash = (String) paramMap.get("hash");
    }

    /**
     * Executes the test
     */
    public void run() {
        try {
            Debug.log("Integrity test", "Getting MD5 instance");
            MessageDigest m = MessageDigest.getInstance("MD5");
            Debug.log("Integrity test", "Creating URL " + target);
            URL url = new URL(this.target);
            Debug.log("Integrity test", "Setting up connection");
            URLConnection urlConnection = url.openConnection();
            InputStream in = urlConnection.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            int fileSize = 0;
            Debug.log("Integrity test", "Reading file");
            while ((numRead = in.read(buffer)) != -1) {
                m.update(buffer, 0, numRead);
                fileSize += numRead;
            }
            in.close();
            Debug.log("Integrity test", "File read: " + fileSize + " bytes");
            Debug.log("Integrity test", "calculating Hash");
            String fileHash = new BigInteger(1, m.digest()).toString(16);
            if (fileHash.equals(this.hash)) {
                Debug.log("Integrity test", "Test OK");
                this.result.put("Integrity", "OK");
            } else {
                Debug.log("Integrity test", "Test failed: different hashes (" + fileHash + " but expected " + hash + ")");
                this.result.put("Integrity", "FAIL");
            }
        } catch (Exception e) {
            Debug.log("Integrity test", "Test failed");
            this.result.put("Integrity", "FAIL");
        }
    }

    /**
     * @return HashMap result contains the testresult where the key is the name of the test
     */
    public HashMap getResult() {
        return result;
    }

    /**
     * 
     * @return a String containing the target for the test
     */
    public String getTarget() {
        return target;
    }

    public String toString() {
        return "[Integrity test " + target + " (" + hash + ")]";
    }
}
