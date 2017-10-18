package BlowfishJ;

import java.security.*;

/**
  * Simple SHA-1 test application; note that the time this package was written
  * SHA-1 hashing wasn't included officially in the Java framework; in these
  * days it could actually be replaced by the MessageDigest factory's
  * capabilities.
  *
  * @author Markus Hahn <markus_hahn@gmx.net>
  * @version September 21, 2002
  */
public class SHA1Test {

    public static void main(String[] args) {
        int nI;
        SHA1 s = new SHA1();
        System.out.print("running selftest...");
        if (!s.selfTest()) {
            System.out.println(", FAILED");
            return;
        }
        System.out.println(", done.");
        String sTest = (args.length > 0) ? args[0] : "0123456789abcdefghijklmnopqrstuvwxyz";
        s.update(sTest);
        s.finalize();
        System.out.println("\"" + sTest + "\": " + s.toString());
        s.clear();
        s = new SHA1();
        byte[] tohash = new byte[257];
        for (nI = 0; nI < tohash.length; nI++) tohash[nI] = (byte) nI;
        s.update(tohash);
        s.finalize();
        MessageDigest mds;
        try {
            mds = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("standard SHA-1 not available");
            return;
        }
        mds.update(tohash);
        byte[] dg0 = s.getDigest();
        byte[] dg1 = mds.digest();
        for (nI = 0; nI < dg0.length; nI++) {
            if (dg0[nI] != dg1[nI]) {
                System.out.println("NOT compatible to the standard!");
                return;
            }
        }
        System.out.println("compatibiliy test OK.");
    }
}
