package de.uni_hamburg.golem.target;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.Logger;
import de.uni_hamburg.golem.model.GEnterprisePackage;
import de.uni_hamburg.golem.model.GMessage;

public class TargetUtils {

    static Logger log = Logger.getLogger(TargetUtils.class);

    /**
	 * Compresses input byte array into a zip outputstream array.
	 *
	 * @param inbuf
	 * @return
	 * @throws IOException
	 */
    public static byte[] zip(String contentlabel, byte[] inbuf) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipout = new ZipOutputStream(bout);
        ZipEntry ze = new ZipEntry(contentlabel);
        zipout.putNextEntry(ze);
        zipout.setLevel(7);
        zipout.write(inbuf);
        zipout.closeEntry();
        zipout.close();
        return bout.toByteArray();
    }

    /**
	 * Write as zipped package to "unsent" directory. Write messages to Log.
	 *
	 * @param pkg
	 * @throws Exception
	 */
    public static void fallback(GEnterprisePackage pkg) throws Exception {
        FileDevice bakTarget = new FileDevice();
        bakTarget.setFilename("unsent-ims_");
        if (!new File("unsent").exists()) new File("unsent").mkdirs();
        bakTarget.setDirectory("unsent");
        bakTarget.setZipped(true);
        bakTarget.setAppendUnique(true);
        GEnterprisePackage mpkg = bakTarget.write(pkg);
        ArrayList<GMessage> msgs = mpkg.getMessages();
        for (int i = 0; i < msgs.size(); i++) {
            log.info(msgs.get(i).toString());
        }
    }

    /**
	 * Read contents of a stream into a String.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
    public static String read(Reader in) throws IOException {
        StringBuffer out = new StringBuffer();
        char[] cbuf = new char[1024];
        int size = -1;
        do {
            size = in.read(cbuf);
            if (size != -1) out.append(new String(cbuf, 0, size));
        } while (size != -1);
        in.close();
        return out.toString();
    }

    /**
	 * Read contents of a stream into a String.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
    public static String readStream(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        int c = -1;
        do {
            c = in.read();
            if (c != -1) out.append((char) c);
        } while (c != -1);
        in.close();
        return out.toString();
    }

    /**
	 * Translates text into md5 representation.
	 *
	 * @param args
	 * @throws NoSuchAlgorithmException
	 */
    public static void main(String[] args) throws NoSuchAlgorithmException {
        if (args.length != 1) {
            System.err.println("USAGE: TargetUtils <PLAINTEXT>");
            System.exit(0);
        }
        System.out.println(getMD5(args[0]));
    }

    /**
	 * Return hex representation of md5 hash.
	 *
	 * @param plaintext
	 * @return
	 */
    public static String getMD5(String plaintext) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("md5");
            String digest_data = plaintext;
            byte[] data_bytes = md5.digest(digest_data.getBytes());
            StringBuffer hexString = new StringBuffer();
            String hex = null;
            for (int i = 0; i < data_bytes.length; i++) {
                hex = Integer.toHexString(0xFF & data_bytes[i]);
                if (hex.length() < 2) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
