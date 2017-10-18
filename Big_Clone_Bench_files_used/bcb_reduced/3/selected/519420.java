package newgen.administration;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;
import sun.misc.CharacterEncoder;

/**
 *
 * @author root
 */
public class EncryptionPassword {

    private static EncryptionPassword instance;

    /** Creates a new instance of EncryptionPassword */
    public EncryptionPassword() {
    }

    public synchronized String encrypt(String plaintext) throws Exception {
        return plaintext;
    }

    public synchronized String decrypt(String plaintext) throws Exception {
        MessageDigest md = null;
        String strhash = new String((new BASE64Decoder()).decodeBuffer(plaintext));
        System.out.println("strhash1122  " + strhash);
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte raw[] = md.digest();
        try {
            md.update(new String(raw).getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("plain text  " + strhash);
        String strcode = new String(raw);
        System.out.println("strcode.." + strcode);
        return strcode;
    }

    public static synchronized EncryptionPassword getInstance() {
        if (instance == null) {
            instance = new EncryptionPassword();
        }
        return instance;
    }

    public static void main(String[] args) {
        try {
            String str = "abc";
            String encoded = "";
            encoded = EncryptionPassword.getInstance().encrypt(str);
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <record> <datafield tag=\"100\"> <subfield code=\"a\">Stevens, Richard</subfield></datafield></record>";
            encoded = (new BASE64Encoder()).encode(xml.getBytes());
            System.out.println("encoded :  " + encoded);
            encoded = new String((new BASE64Decoder()).decodeBuffer(encoded));
            System.out.println("dencoded :  " + encoded);
            encoded = EncryptionPassword.getInstance().encrypt("abc");
            System.out.println("enc: " + EncryptionPassword.getInstance().encrypt("abc"));
            System.out.println("dec:  " + EncryptionPassword.getInstance().decrypt(encoded));
        } catch (Exception e) {
        }
    }
}
