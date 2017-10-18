package data;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author nadine
 */
public class User {

    private final String nick, nachname, vorname, klasse, pwHash, ip;

    private int koop, nKoop, punkteG, punkte5, punkte10, punkte20, punkte50;

    private boolean online;

    public User(String nick, String nachname, String vorname, String klasse, String pw, int koop, int nKoop, int punkteG, int punkte5, int punkte10, int punkte20, int punkte50, boolean online, String ip) throws Exception {
        this.nick = nick;
        this.nachname = nachname;
        this.vorname = vorname;
        this.klasse = klasse;
        this.pwHash = toHash(pw);
        this.koop = koop;
        this.nKoop = nKoop;
        this.punkteG = punkteG;
        this.punkte5 = punkte5;
        this.punkte10 = punkte10;
        this.punkte20 = punkte20;
        this.punkte50 = punkte50;
        this.online = online;
        this.ip = ip;
        if (nick.length() < 3) {
            throw new Exception("Nickname muss mindestens 3 Zeichen lang sein!");
        }
        if (pw.length() < 3) {
            throw new Exception("Passwort muss mindestens 3 Zeichen lang sein!");
        }
        if (nachname.length() < 1) {
            throw new Exception("Nachname darf nicht leer sein!");
        }
        if (vorname.length() < 1) {
            throw new Exception("Nachname darf nicht leer sein!");
        }
    }

    public User(ResultSet res) throws SQLException {
        this.nick = res.getString("nick");
        this.nachname = res.getString("nachname");
        this.vorname = res.getString("vorname");
        this.klasse = res.getString("klasse");
        this.pwHash = res.getString("passwort");
        this.koop = res.getInt("kooperieren");
        this.nKoop = res.getInt("nichtkooperieren");
        this.punkteG = res.getInt("punktegesamt");
        this.punkte5 = res.getInt("punkte5");
        this.punkte10 = res.getInt("punkte10");
        this.punkte20 = res.getInt("punkte20");
        this.punkte50 = res.getInt("punkte50");
        this.online = res.getBoolean("online");
        this.ip = res.getString("ip");
    }

    public static String toHash(String pw) throws Exception {
        final MessageDigest md5 = MessageDigest.getInstance("md5");
        md5.update(pw.getBytes("utf-8"));
        final byte[] result = md5.digest();
        return toHexString(result);
    }

    public static String toHexString(byte[] b) {
        final BigInteger number = new BigInteger(1, b);
        String hex = number.toString(16);
        while (hex.length() < 32) {
            hex = "0" + hex;
        }
        return hex;
    }

    public String getNachname() {
        return nachname;
    }

    public String getVorname() {
        return vorname;
    }

    public String getNick() {
        return nick;
    }

    public String getPwHash() {
        return pwHash;
    }

    public String getKlasse() {
        return klasse;
    }

    public int getKoop() {
        return koop;
    }

    public int getNKoop() {
        return nKoop;
    }

    public int getPunkteG() {
        return punkteG;
    }

    public int getPunkte5() {
        return punkte5;
    }

    public int getPunkte10() {
        return punkte10;
    }

    public int getPunkte20() {
        return punkte20;
    }

    public int getPunkte50() {
        return punkte50;
    }

    public boolean getOnline() {
        return online;
    }

    public void setKoop(int koop) {
        this.koop = koop;
    }

    public void setNKoop(int nKoop) {
        this.nKoop = nKoop;
    }

    public void setPunkteG(int punkteG) {
        this.punkteG = punkteG;
    }

    public void setPunkte5(int punkte5) {
        this.punkte5 = punkte5;
    }

    public void setPunkte10(int punkte10) {
        this.punkte10 = punkte10;
    }

    public void setPunkte20(int punkte20) {
        this.punkte20 = punkte20;
    }

    public void setPunkte50(int punkte50) {
        this.punkte50 = punkte50;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return nick + " " + nachname + " " + vorname + " " + pwHash + " " + klasse + " " + koop + " " + nKoop + " " + punkteG + " " + punkte5 + " " + punkte10 + " " + punkte20 + " " + punkte50 + " " + online + " " + ip;
    }
}
