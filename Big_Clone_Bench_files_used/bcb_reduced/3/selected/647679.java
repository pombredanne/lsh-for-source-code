package es.uclm.smile.cuestor;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import com.sun.org.apache.xml.internal.security.utils.Base64;

/**
 * @author Administrador
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ResumeFile {

    public static String SHA1 = "SHA1";

    private static byte[] cargarFicherodeDisco(String path) {
        try {
            File fichero = new File(path);
            byte[] b = new byte[(int) fichero.length()];
            FileInputStream fis = new FileInputStream(fichero);
            fis.read(b);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toHexString(new Byte(bytes[i]).intValue()));
            sb.append(" ");
        }
        return sb.toString();
    }

    public static String resumir(String path, String alg) throws Exception {
        try {
            MessageDigest digester = MessageDigest.getInstance(alg);
            byte fileBytes[] = cargarFicherodeDisco(path);
            if (fileBytes != null) {
                byte[] binDigest = digester.digest();
                if (binDigest != null) {
                    return Base64.encode(binDigest);
                }
            }
            return "";
        } catch (Exception e) {
            throw e;
        }
    }
}
