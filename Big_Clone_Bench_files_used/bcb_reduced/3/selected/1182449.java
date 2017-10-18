package eu.venusc.storagepassword;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author mmacias
 */
public class PasswordChanges extends HttpServlet {

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        synchronized (this) {
            Properties prop = new Properties();
            try {
                prop.load(getClass().getResourceAsStream("/config.properties"));
                String passwordsFile = prop.getProperty("passwords.file");
                BufferedReader bur = new BufferedReader(new InputStreamReader(new FileInputStream(passwordsFile)));
                String line = null;
                Map<String, String> users = new TreeMap<String, String>();
                do {
                    line = bur.readLine();
                    if (line != null && line.contains(":")) {
                        line = line.trim();
                        int idx = line.indexOf(':');
                        String name = line.substring(0, idx);
                        String md5 = line.substring(idx + 1);
                        users.put(name, md5);
                    }
                } while (line != null);
                bur.close();
                String user = request.getParameter("userName");
                if (!users.containsKey(user)) throw new Exception("User or password incorrect");
                String oldPassword = request.getParameter("currentPassword");
                String md5OldPassword = md5Digest(oldPassword);
                System.out.println("md5 passwords\n\tdb\t" + users.get(user) + "\n\tinput\t" + md5OldPassword);
                if (!users.get(user).equalsIgnoreCase(md5OldPassword)) throw new Exception("User or password incorrect");
                String np1 = request.getParameter("newPassword1");
                String np2 = request.getParameter("newPassword2");
                if (np1 == null || np2 == null || np1.trim().equals("") || np2.trim().equals("") || !np1.equals(np2)) {
                    throw new Exception("You must supply a new password, and must be the same in the two fields.");
                }
                users.put(user, md5Digest(np1));
                PrintWriter pw = new PrintWriter(new FileOutputStream(passwordsFile, false));
                for (String u : users.keySet()) {
                    pw.print(u);
                    pw.print(":");
                    pw.println(users.get(u));
                }
                pw.close();
            } catch (Exception e) {
                e.printStackTrace();
                request.getSession().setAttribute("message", e.getMessage());
                response.sendRedirect("index.jsp");
                return;
            }
            response.sendRedirect("ok.jsp");
        }
    }

    private String md5Digest(String plain) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(plain.trim().getBytes());
        byte pwdDigest[] = digest.digest();
        StringBuilder md5buffer = new StringBuilder();
        for (int i = 0; i < pwdDigest.length; i++) {
            int number = 0xFF & pwdDigest[i];
            if (number <= 0xF) {
                md5buffer.append('0');
            }
            md5buffer.append(Integer.toHexString(number));
        }
        return md5buffer.toString();
    }
}
