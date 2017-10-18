package md5analyser.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;

/**
 *
 * @author beniaminus
 */
public class HashCalculation extends Thread {

    private List<HashedFile> data;

    private volatile boolean stop;

    public HashCalculation(List<HashedFile> data) {
        super();
        this.data = data;
    }

    public synchronized void stop(boolean s) {
        stop = s;
        notifyAll();
    }

    public void calculateFileHash(int num) {
        HashedFile hf = data.get(num);
        hf.setChecksum(calculateFileHash(hf.getFile()));
    }

    public byte[] calculateFileHash(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(bis, md5);
            md5.reset();
            while (dis.read() != -1 && !stop) ;
            if (!stop) return md5.digest();
        } catch (Exception e) {
        }
        return null;
    }
}
