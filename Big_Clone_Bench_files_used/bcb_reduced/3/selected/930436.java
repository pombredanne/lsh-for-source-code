package net.sf.cclearly.launcher.md5;

import java.io.*;

/**
 * 
 * @author	Santeri Paavolainen <santtu@cs.hut.fi>
 * @author	Timothy W Macinta (twm@alum.mit.edu) (added main() method)
 * 
 **/
public class MD5InputStream extends FilterInputStream {

    /**
	   * MD5 context
	   */
    private MD5 md5;

    /**
	   * Creates a MD5InputStream
	   * @param in	The input stream
	   */
    public MD5InputStream(InputStream in) {
        super(in);
        md5 = new MD5();
    }

    /**
	   * Read a byte of data. 
	   * @see java.io.FilterInputStream
	   */
    public int read() throws IOException {
        int c = in.read();
        if (c == -1) return -1;
        if ((c & ~0xff) != 0) {
            System.out.println("MD5InputStream.read() got character with (c & ~0xff) != 0)!");
        } else {
            md5.Update(c);
        }
        return c;
    }

    /**
	   * Reads into an array of bytes.
	   *
	   * @see java.io.FilterInputStream
	   */
    public int read(byte bytes[], int offset, int length) throws IOException {
        int r;
        if ((r = in.read(bytes, offset, length)) == -1) return r;
        md5.Update(bytes, offset, r);
        return r;
    }

    /**
	   * Returns array of bytes representing hash of the stream as
	   * finalized for the current state. 
	   * @see MD5#Final
	   */
    public byte[] hash() {
        return md5.Final();
    }

    public MD5 getMD5() {
        return md5;
    }

    /**
	   * This method is here for testing purposes only - do not rely
	   * on it being here.
	   **/
    public static void main(String[] arg) {
        try {
            String filename = arg[arg.length - 1];
            boolean use_default_md5 = false;
            boolean use_native_lib = true;
            for (int i = 0; i < arg.length - 1; i++) {
                if (arg[i].equals("--use-default-md5")) {
                    use_default_md5 = true;
                } else if (arg[i].equals("--no-native-lib")) {
                    use_native_lib = false;
                }
            }
            byte[] buf = new byte[65536];
            int num_read;
            if (use_default_md5) {
                InputStream in = new BufferedInputStream(new FileInputStream(filename));
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
                while ((num_read = in.read(buf)) != -1) {
                    digest.update(buf, 0, num_read);
                }
                System.out.println(MD5.asHex(digest.digest()) + "  " + filename);
                in.close();
            } else {
                if (!use_native_lib) {
                    MD5.initNativeLibrary(true);
                }
                MD5InputStream in = new MD5InputStream(new BufferedInputStream(new FileInputStream(filename)));
                while ((num_read = in.read(buf)) != -1) ;
                System.out.println(MD5.asHex(in.hash()) + "  " + filename);
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
