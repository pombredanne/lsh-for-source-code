package br.org.articulus.server;

/**
 * Class to generate a String into MD5 crypt
 * @author anderson
 * @version 1.0
 */
public final class MD5 {

    /**
	 * Method to create a MD5 crypt
	 * @param value Set the value for convert
	 * @return Return MD5
	 * @author anderson
	 * @version 1.0
	 */
    public static final String generate(String value) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(value.getBytes());
            byte[] hash = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                if ((0xff & hash[i]) < 0x10) {
                    hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
                } else {
                    hexString.append(Integer.toHexString(0xFF & hash[i]));
                }
            }
            value = hexString.toString();
        } catch (Exception nsae) {
            nsae.printStackTrace();
        }
        return value;
    }
}
