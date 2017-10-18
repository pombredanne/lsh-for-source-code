package mhk.function;

import java.security.MessageDigest;

public class Coding {

    public byte[] getCoded(String name, String pass) {
        byte[] digest = null;
        if (pass != null && 0 < pass.length()) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                md.update(name.getBytes());
                md.update(pass.getBytes());
                digest = md.digest();
            } catch (Exception e) {
                e.printStackTrace();
                digest = null;
            }
        }
        return digest;
    }

    public boolean checkCode(String name, String pass, byte[] digest) {
        if (digest == null) {
            return false;
        }
        byte[] org = getCoded(name, pass);
        if (org == null || org.length != digest.length) {
            return false;
        }
        for (int i = 0; i < digest.length; i++) {
            if (org[i] != digest[i]) {
                return false;
            }
        }
        return true;
    }
}
