package websrvl;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author Arnaud
 * On stocke les informations de l'utilisateur enregistré
 */
public class UserInfo {

    private String u_name;

    private String u_mdp;

    private String u_right;

    private String u_sessionID;

    public UserInfo() {
    }

    public void setValue(String name, String mdp, String right, Date first) {
        try {
            this.u_sessionID = getSessionId(name, mdp);
            this.u_mdp = mdp;
            this.u_name = name;
            this.u_right = right;
        } catch (Exception ex) {
        }
    }

    public String getMdp() {
        return u_mdp;
    }

    public String getRight() {
        return u_right;
    }

    public String getName() {
        return u_name;
    }

    public String getSession() {
        return u_sessionID;
    }

    private String getSessionId(String name, String mdp) {
        Calendar c = Calendar.getInstance();
        @SuppressWarnings("static-access") String date = c.get(c.DAY_OF_MONTH) + "/" + c.get(c.MONTH) + "/" + c.get(c.YEAR) + " " + c.getTime();
        String str = name + ":" + mdp + ":" + date;
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(str.getBytes());
            StringBuilder hashString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(hash[i]);
                if (hex.length() == 1) {
                    hashString.append('0');
                    hashString.append(hex.charAt(hex.length() - 1));
                } else hashString.append(hex.substring(hex.length() - 2));
            }
            return hashString.toString();
        } catch (Exception ex) {
        }
        return "123";
    }
}
