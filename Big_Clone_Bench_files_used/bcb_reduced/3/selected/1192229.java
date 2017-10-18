package wdc.utils;

import java.sql.*;
import java.util.*;
import wdc.dbaccess.ConnectionPool;
import wdc.settings.Settings;
import com.oreilly.servlet.Base64Encoder;

public class Encrypt {

    public static String getBase64(String s) throws Exception {
        return (Base64Encoder.encode(getBytes(s)));
    }

    public static byte[] getBytes(String s) throws Exception {
        java.security.MessageDigest d = null;
        d = java.security.MessageDigest.getInstance("SHA-1");
        d.reset();
        d.update(s.getBytes());
        return d.digest();
    }

    public static void main(String[] args) throws Exception {
        Connection cn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Settings settings = Settings.getInstance();
            settings.load(args[0]);
            cn = ConnectionPool.getConnection("users");
            Statement st1 = cn.createStatement();
            String sql1 = "SELECT login, password FROM users";
            Hashtable<String, String> dict = new Hashtable<String, String>();
            ResultSet rs = st1.executeQuery(sql1);
            while (rs.next()) {
                dict.put(rs.getString(1), rs.getString(2));
            }
            rs.close();
            st1.close();
            String sql2 = "UPDATE users SET password = ? WHERE login = ?";
            PreparedStatement st2 = cn.prepareStatement(sql2);
            Iterator<String> i = dict.keySet().iterator();
            while (i.hasNext()) {
                String login = i.next();
                String password = dict.get(login);
                if (password != null) {
                    System.out.println(login + ":" + password);
                    st2.setString(1, getBase64(password));
                    st2.setString(2, login);
                    st2.execute();
                }
            }
            st2.close();
        } finally {
            try {
                cn.close();
            } catch (Exception e) {
            }
        }
    }
}
