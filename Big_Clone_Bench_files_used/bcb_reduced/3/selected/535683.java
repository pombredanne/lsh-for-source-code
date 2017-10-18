package vlan.webgame.manage;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.jmantis.core.utils.RandomUtils;

public class AdminPassword {

    /**
	 * @param args
	 * @throws UnsupportedEncodingException
	 */
    public static void main(String[] args) throws UnsupportedEncodingException {
        MessageDigest md = null;
        String password = "admin!@#$" + "ZKNugmkm";
        try {
            md = MessageDigest.getInstance("SHA-512");
            md.update(password.getBytes("utf8"));
            byte[] b = md.digest();
            StringBuilder output = new StringBuilder(32);
            for (int i = 0; i < b.length; i++) {
                String temp = Integer.toHexString(b[i] & 0xff);
                if (temp.length() < 2) {
                    output.append("0");
                }
                output.append(temp);
            }
            System.out.println(output);
            System.out.println(output.length());
            System.out.println(RandomUtils.createRandomString(8));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
