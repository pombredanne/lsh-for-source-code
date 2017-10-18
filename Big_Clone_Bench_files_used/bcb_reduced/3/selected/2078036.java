package cluster5.shared.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class HashCalculator {

    public static enum Algorithms {

        MD2, MD5, SHA1, SHA256, SHA512
    }

    ;

    private static Map<Algorithms, String> algorithmNames = new Hashtable<Algorithms, String>();

    static {
        algorithmNames.put(Algorithms.MD2, "MD2");
        algorithmNames.put(Algorithms.MD5, "MD5");
        algorithmNames.put(Algorithms.SHA1, "SHA1");
        algorithmNames.put(Algorithms.SHA256, "SHA-256");
        algorithmNames.put(Algorithms.SHA512, "SHA-512");
    }

    private List<HashCalculatorProgressListener> listeners = new ArrayList<HashCalculatorProgressListener>();

    /**
	 * Adds a listener which will be updated on the progress of hashing a file/stream.
	 * 
	 * @param listener a listener to be added
	 */
    public void addListener(HashCalculatorProgressListener listener) {
        listeners.add(listener);
    }

    /**
	 * @param listener a listener to be removed
	 */
    public void removeListener(HashCalculator listener) {
        listeners.remove(listener);
    }

    /**
	 * Notifies the listeners.
	 * 
	 * @param progress
	 */
    private void notifyListeners(int progress) {
        for (HashCalculatorProgressListener listener : listeners) {
            listener.setProgress(progress);
        }
    }

    /**
	 * Formats the hash into a string.
	 * 
	 * @param digest
	 * @return formatted hash
	 */
    private String formatHash(byte[] digest) {
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            int b = digest[i] & 0xff;
            if (Integer.toHexString(b).length() == 1) hexValue = hexValue.append("0");
            hexValue.append(Integer.toHexString(b));
        }
        return hexValue.toString();
    }

    /**
	 * Calculates hash of the message using the specified algorithm.
	 * 
	 * @param message
	 * @param algorithm
	 * @return hash of the message or null if something went wrong
	 */
    public String getHash(String message, Algorithms algorithm) {
        try {
            byte[] buffer = message.getBytes();
            MessageDigest md = MessageDigest.getInstance(algorithmNames.get(algorithm));
            md.update(buffer);
            byte[] digest = md.digest();
            String hash = formatHash(digest);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Calculates hash of the data passed in the input stream using the specified algorithm.
	 * Does not notify registered listeners on hash calculation progress.
	 * 
	 * @param inputStream
	 * @param algorithm
	 * @return hash of the data or null if something went wrong.
	 */
    public String getHash(InputStream inputStream, Algorithms algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithmNames.get(algorithm));
            DigestInputStream dis = new DigestInputStream(inputStream, md);
            try {
                while (dis.available() > 0) dis.read();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return formatHash(dis.getMessageDigest().digest());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
	 * Calculates hash of the specified file using the algorithm. While the hash is
	 * calculated, the registered listeners are notified on the progress.
	 * 
	 * @param file
	 * @param algorithm
	 * @return hash of the file or null if something went wrong.
	 */
    public String getHash(File file, Algorithms algorithm) {
        int fileSize = (int) file.length();
        try {
            MessageDigest md = MessageDigest.getInstance(algorithmNames.get(algorithm));
            DigestInputStream dis = new DigestInputStream(new FileInputStream(file), md);
            int numRead = 0;
            int prevPercentComplete = 0;
            try {
                while (dis.available() > 0) {
                    dis.read();
                    numRead++;
                    int percentComplete = Math.round(((float) numRead) / fileSize * 100);
                    if (percentComplete > prevPercentComplete) {
                        prevPercentComplete = percentComplete;
                        notifyListeners(percentComplete);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return formatHash(dis.getMessageDigest().digest());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) throws IOException {
        HashCalculator hc = new HashCalculator();
        hc.addListener(new HashCalculatorProgressListener() {

            public void setProgress(int percentComplete) {
                if (percentComplete % 10 == 0) System.out.println("complete=" + percentComplete);
            }
        });
        String filename = "/home/ibisek/wqz/temp/airspace2008.exe";
        File f = new File(filename);
        String hash = hc.getHash(new FileInputStream(f), Algorithms.SHA1);
        System.out.println("SHA1 = " + hash);
        hash = hc.getHash(new FileInputStream(f), Algorithms.MD5);
        System.out.println(" MD5 = " + hash);
    }
}
