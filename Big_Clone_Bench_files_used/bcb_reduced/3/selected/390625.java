package Negotiation.DBconn;

import java.sql.*;
import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;

public class DBmanager {

    final String driverName = "sun.jdbc.odbc.JdbcOdbcDriver";

    Connection con;

    public void connect(String nomeDB, String user, String passwd) {
        try {
            Class.forName(driverName);
            String url = "jdbc:odbc:" + nomeDB;
            con = DriverManager.getConnection(url, user, passwd);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void disConnect() {
        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized String getLastAccess(String login) {
        String out = "";
        try {
            Object ret;
            ResultSet rs;
            Statement stmt;
            String sql = "Select last_access FROM users WHERE uid='" + login + "'";
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            rs.next();
            out = rs.getString("last_access");
            rs.close();
            stmt.close();
            return out;
        } catch (SQLException e) {
            e.printStackTrace();
            return "error";
        }
    }

    public synchronized ArrayList getVisInfo(String user_name, String ubl_type) {
        ArrayList out = new ArrayList();
        ArrayList aux;
        try {
            Object ret;
            ResultSet rs;
            Statement stmt;
            String sql = "Select date,rule_name,rule FROM rules WHERE (user_name='" + user_name + "'AND ubl_type='" + ubl_type + "')";
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            int locCount = 0;
            while (rs.next()) {
                aux = new ArrayList();
                aux.add(0, rs.getString("date"));
                aux.add(1, rs.getString("rule_name"));
                aux.add(2, rs.getString("rule"));
                out.add(locCount, aux);
                locCount++;
            }
            rs.close();
            stmt.close();
            return out;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void updateLastAccess(String login, String date) {
        try {
            Object ret;
            Statement stmt;
            String sql = "UPDATE users SET last_access='" + date + "' WHERE uid='" + login + "'";
            stmt = con.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized ArrayList getRule(String user_name) {
        ArrayList output = new ArrayList();
        int count = 0;
        try {
            Object ret;
            ResultSet rs;
            Statement stmt;
            String sql = "SELECT rule FROM rules WHERE user_name='" + user_name + "'";
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            String id = "";
            while (rs.next()) {
                output.add(count, rs.getString("rule"));
                count++;
            }
            rs.close();
            stmt.close();
            return output;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized ArrayList getRuleByUserNameAndDocType(String user_name, String ubl_type) {
        ArrayList outputRule = new ArrayList();
        ArrayList outputRuleName = new ArrayList();
        ArrayList output = new ArrayList();
        int count = 0;
        try {
            Object ret;
            ResultSet rs;
            Statement stmt;
            String sql = "SELECT rule_name,rule FROM rules WHERE (user_name='" + user_name + "'AND ubl_type='" + ubl_type + "')";
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            String id = "";
            while (rs.next()) {
                outputRule.add(count, rs.getString("rule"));
                outputRuleName.add(count, rs.getString("rule_name"));
                count++;
            }
            output.add(0, outputRule);
            output.add(1, outputRuleName);
            rs.close();
            stmt.close();
            return output;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized boolean titleChecker(String user_name, String rule_name, String ubl_type) {
        boolean out = false;
        try {
            Object ret;
            ResultSet rs;
            Statement stmt;
            String sql = "SELECT user_name FROM rules WHERE (user_name='" + user_name + "'AND rule_name='" + rule_name + "'AND ubl_type='" + ubl_type + "')";
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                rs.close();
                stmt.close();
                out = true;
            } else {
                rs.close();
                stmt.close();
                out = false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return out;
    }

    public synchronized boolean checklogin(String uid) {
        boolean out = false;
        try {
            Object ret;
            ResultSet rs;
            Statement stmt;
            String sql = "SELECT uid FROM users WHERE (uid='" + uid + "')";
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                rs.close();
                stmt.close();
                out = true;
            } else {
                rs.close();
                stmt.close();
                out = false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return out;
    }

    public synchronized boolean loginChecker(String uid, String password) {
        boolean out = false;
        String result = "";
        String encryptedString = uid + password;
        int i;
        java.security.MessageDigest md = null;
        try {
            md = java.security.MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        byte[] pw = encryptedString.getBytes();
        for (i = 0; i < pw.length; i++) {
            int vgl = pw[i];
            if (vgl < 0) vgl += 256;
            if (32 < vgl) md.update(pw[i]);
        }
        byte[] bresult = md.digest();
        result = "";
        for (i = 0; i < bresult.length; i++) {
            int counter = bresult[i];
            if (counter < 0) counter += 256;
            String counterStr = Integer.toString(counter, 16);
            while (counterStr.length() < 2) counterStr = '0' + counterStr;
            result += counterStr;
        }
        try {
            Object ret;
            ResultSet rs;
            Statement stmt;
            String sql = "SELECT uid FROM users WHERE (uid='" + uid + "'AND password='" + result + "'AND allowed=true)";
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                rs.close();
                stmt.close();
                out = true;
            } else {
                rs.close();
                stmt.close();
                out = false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return out;
    }

    public synchronized String insertRule(String user_name, String ubl_type, String date, String rule_name, String rule) {
        try {
            Statement stmt;
            String sql = "INSERT INTO rules (user_name,ubl_type,date,rule_name,rule) Values('" + user_name + "','" + ubl_type + "','" + date + "','" + rule_name + "','" + rule + "')";
            stmt = con.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
            return "ok";
        } catch (SQLException e) {
            e.printStackTrace();
            return "no";
        }
    }

    public synchronized void deleteRule(String user_name, String ubl_type, ArrayList delL) {
        int leng = delL.size();
        int count = 0;
        String rule_name;
        try {
            Object ret;
            ResultSet rs;
            Statement stmt;
            while (count < leng) {
                rule_name = (String) delL.get(count);
                String sql = "DELETE FROM rules WHERE (user_name='" + user_name + "'AND ubl_type='" + ubl_type + "'AND rule_name='" + rule_name + "')";
                stmt = con.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();
                count++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void insert_new_user(String uid, String password, String name, String surname, String address, String city, String zip, String country, String phone, String fax, String email, String web) {
        try {
            Statement stmt;
            String sql = "INSERT INTO users (uid,password,name,surname,address,city,zip,country,phone,fax,email,web,allowed,last_access) Values('" + uid + "','" + password + "','" + name + "','" + surname + "','" + address + "','" + city + "','" + zip + "','" + country + "','" + phone + "','" + fax + "','" + email + "','" + web + "','true','this is your first access')";
            stmt = con.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
