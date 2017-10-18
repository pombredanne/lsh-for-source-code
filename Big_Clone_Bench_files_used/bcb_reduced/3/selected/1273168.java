package ro.k.startUP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import ro.k.jstore.beans.ManoperaBaseJBean;
import ro.k.web.beans.UserBean;
import com.google.gson.Gson;

public class InitDataFiles {

    private static final String fileMan = "c:\\manopera.json";

    private final String file = "c:\\userKey.json";

    public void initUsers() {
        UserBean ub = new UserBean("name", getCoded("pass1"));
        Gson gson = new Gson();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(gson.toJson(ub));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void readUsersTest() {
        Gson gson = new Gson();
        UserBean ub = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                ub = gson.fromJson(str, UserBean.class);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(ub.getName());
    }

    public void initManopera() {
        List<ManoperaBaseJBean> mans = new ArrayList<ManoperaBaseJBean>();
        mans.add(new ManoperaBaseJBean("nume 2", "10223", "2"));
        mans.add(new ManoperaBaseJBean("nume 3", "10323", "2"));
        mans.add(new ManoperaBaseJBean("nume 4", "10423", "2"));
        mans.add(new ManoperaBaseJBean("nume 5", "10523", "2"));
        mans.add(new ManoperaBaseJBean("nume 6", "10623", "2"));
        mans.add(new ManoperaBaseJBean("nume 7", "10723", "2"));
        mans.add(new ManoperaBaseJBean("nume 8", "10823", "2"));
        mans.add(new ManoperaBaseJBean("nume 9", "10923", "2"));
        mans.add(new ManoperaBaseJBean("nume 10", "11223", "2"));
        mans.add(new ManoperaBaseJBean("nume 11", "12223", "2"));
        mans.add(new ManoperaBaseJBean("nume 12", "13223", "2"));
        mans.add(new ManoperaBaseJBean("nume 13", "14223", "2"));
        mans.add(new ManoperaBaseJBean("nume 14", "15223", "2"));
        mans.add(new ManoperaBaseJBean("nume 15", "16223", "2"));
        mans.add(new ManoperaBaseJBean("nume 16", "17223", "2"));
        mans.add(new ManoperaBaseJBean("nume 17", "18223", "2"));
        mans.add(new ManoperaBaseJBean("nume 18", "19223", "2"));
        mans.add(new ManoperaBaseJBean("nume 19", "11323", "2"));
        mans.add(new ManoperaBaseJBean("nume 20", "11423", "2"));
        Gson gson = new Gson();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileMan));
            out.write(gson.toJson(mans));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] arg) {
        InitDataFiles i = new InitDataFiles();
        i.initUsers();
        i.initManopera();
    }
}
