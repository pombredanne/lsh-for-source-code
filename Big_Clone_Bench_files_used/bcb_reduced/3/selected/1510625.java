package de.banh.bibo.model.provider.postgresql;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import de.banh.bibo.model.Benutzer;
import de.banh.bibo.model.Rolle;

public class PgBenutzer extends PgBenutzerinfo implements Benutzer {

    private static Logger logger = Logger.getLogger(PgVerlagManager.class.getName());

    private String benutzername;

    private String passwort;

    private String anrede;

    private String strasse;

    private String plz;

    private String ort;

    private String land;

    private String telefon;

    private String telefax;

    private String email;

    private String mobil;

    private String matrikelnummer;

    private Rolle rolle;

    public PgBenutzer() {
        super();
    }

    public PgBenutzer(String benutzername) {
        this();
        setBenutzername(benutzername);
    }

    @Override
    public String getAnrede() {
        return anrede;
    }

    @Override
    public String getBenutzername() {
        return benutzername;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getLand() {
        return land;
    }

    @Override
    public String getMatrikelnummer() {
        return matrikelnummer;
    }

    @Override
    public String getMobil() {
        return mobil;
    }

    @Override
    public String getOrt() {
        return ort;
    }

    @Override
    public String getPlz() {
        return plz;
    }

    @Override
    public Rolle getRolle() {
        return rolle;
    }

    @Override
    public String getStrasse() {
        return strasse;
    }

    @Override
    public String getTelefax() {
        return telefax;
    }

    @Override
    public String getTelefon() {
        return telefon;
    }

    @Override
    public void setAnrede(String anrede) {
        this.anrede = anrede;
    }

    @Override
    public void setBenutzername(String benutzername) {
        this.benutzername = benutzername;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void setLand(String land) {
        this.land = land;
    }

    @Override
    public void setMatrikelnummer(String matrikelnummer) {
        this.matrikelnummer = matrikelnummer;
    }

    @Override
    public void setMobil(String mobil) {
        this.mobil = mobil;
    }

    @Override
    public void setOrt(String ort) {
        this.ort = ort;
    }

    @Override
    public void setPasswort(String unverschluesseltesPasswort) {
        this.passwort = plainStringToMD5(unverschluesseltesPasswort);
    }

    @Override
    public void setPlz(String plz) {
        this.plz = plz;
    }

    @Override
    public void setRolle(Rolle rolle) {
        this.rolle = rolle;
    }

    @Override
    public void setStrasse(String strasse) {
        this.strasse = strasse;
    }

    @Override
    public void setTelefax(String telefax) {
        this.telefax = telefax;
    }

    @Override
    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    @Override
    public boolean checkPasswort(String unverschluesseltesPasswort) {
        String check_pwd = unverschluesseltesPasswort;
        if (unverschluesseltesPasswort.length() > 0) {
            check_pwd = plainStringToMD5(unverschluesseltesPasswort);
            return check_pwd.equals(passwort);
        } else {
            return passwort.length() == 0;
        }
    }

    @Override
    public String toString() {
        return getBenutzername();
    }

    protected void setVerschluesseltesPasswort(String passwort) {
        this.passwort = passwort;
    }

    protected String getPasswort() {
        return passwort;
    }

    /** Wandelt einen String in einen MD5-Wert um.
     * 
     * @param input String-Wert */
    public String plainStringToMD5(String input) {
        MessageDigest md = null;
        byte[] byteHash = null;
        StringBuffer resultString = new StringBuffer();
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.throwing(getClass().getName(), "plainStringToMD5", e);
        }
        md.reset();
        try {
            md.update(input.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
        byteHash = md.digest();
        for (int i = 0; i < byteHash.length; i++) {
            resultString.append(Integer.toHexString(0xF0 & byteHash[i]).charAt(0));
            resultString.append(Integer.toHexString(0x0F & byteHash[i]));
        }
        return (resultString.toString());
    }
}
