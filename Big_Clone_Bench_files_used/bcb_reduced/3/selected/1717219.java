package framework.core.client.hashtools;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Vector;

/**
 * @author noname
 * 
 */
public class HashThread extends Thread {

    private boolean run = true;

    private Vector<ShareFile> coda = new Vector<ShareFile>();

    protected HashThread() {
    }

    public synchronized void addFile(ShareFile s) {
        if (s.getTTHash() == null) this.coda.addElement(s);
    }

    public void run() {
        while (run) {
            this.consume();
        }
    }

    private synchronized void consume() {
        while (coda.size() > 0) {
            try {
                MessageDigest tt = new TigerTree();
                FileInputStream fis = new FileInputStream(((ShareFile) coda.firstElement()).getFullPath());
                int read;
                byte[] in = new byte[1024];
                while ((read = fis.read(in)) > -1) {
                    tt.update(in, 0, read);
                }
                fis.close();
                byte[] digest = tt.digest();
                String hash = new BigInteger(1, digest).toString(16);
                while (hash.length() < 48) {
                    hash = "0" + hash;
                }
                coda.firstElement().setDigest(Base32.encode(digest));
                coda.firstElement().notifyMe();
                tt.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
            coda.removeElementAt(0);
        }
    }

    public void kill() {
        this.run = false;
    }
}
