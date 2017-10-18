import java.security.*;

public class Encryption {

    private String password = null;

    public String getPassword() {
        return password;
    }

    public String new2Password(String passwd) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            String clearPassword = passwd;
            md.update(clearPassword.getBytes());
            byte[] digestedPassword = md.digest();
            return new String(digestedPassword);
        } catch (java.security.NoSuchAlgorithmException e) {
            System.out.println("MD5 doesn't exist");
            System.out.println(e.toString());
            return null;
        }
    }

    public void setPassword(String passwd) {
        try {
            MessageDigest sha = MessageDigest.getInstance("MD5");
            byte[] tmp = passwd.getBytes();
            sha.update(tmp);
            password = new String(sha.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            System.out.println("MD5 doesn't exist");
            System.out.println(e.toString());
        }
    }
}
