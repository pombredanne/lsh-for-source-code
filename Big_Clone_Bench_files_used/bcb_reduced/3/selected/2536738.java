package ro.k.jstore;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import ro.k.jstore.beans.UserJBean;
import com.google.gson.Gson;

public class LoginService extends Service {

    private static final String file = "c:\\userKey.json";

    private static LoginService inst = null;

    public static LoginService getInstance() {
        if (inst == null) {
            inst = new LoginService();
        }
        return inst;
    }

    public boolean validateUser(String name, String pass) {
        String secretPass = getCoded(pass);
        UserJBean ub = readUser();
        if (ub.getName().equals(name) && ub.getPass().equals(secretPass)) {
            return true;
        }
        return false;
    }

    private String getCoded(String pass) {
        String passSecret = "";
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(pass.getBytes("UTF8"));
            byte s[] = m.digest();
            for (int i = 0; i < s.length; i++) {
                passSecret += Integer.toHexString((0x000000ff & s[i]) | 0xffffff00).substring(6);
            }
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return passSecret;
    }

    private UserJBean readUser() {
        Gson gson = new Gson();
        UserJBean ub = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                ub = gson.fromJson(str, UserJBean.class);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ub;
    }
}
