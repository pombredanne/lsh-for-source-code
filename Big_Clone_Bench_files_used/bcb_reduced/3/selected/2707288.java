package FGMP_Hotel_Management.GUI;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import FGMP_Hotel_Management.Datenbank.*;
import FGMP_Hotel_Management.Language.ErrorMsg;
import FGMP_Hotel_Management.Meldungen;

/**
 * repräsentiert einen Nutzer der Software
 *
 * @author Daniel Fischer, Martin Meyer
 */
public class User {

    private String nutzer_id;

    private String nutzer_id_old;

    private String pw;

    private boolean buchung_bearbeiten = false;

    private boolean gast_bearbeiten = false;

    private boolean zimmer_bearbeiten = false;

    private boolean kategorie_bearbeiten = false;

    private boolean nutzer_bearbeiten = false;

    private boolean konfig_bearbeiten = false;

    private boolean accepted = false;

    public static ArrayList Benutzer_IDs = new ArrayList();

    public static boolean test;

    public User(String name, String uncrypt_pw) {
        this.nutzer_id = name;
        if (this.getDatenAusDB()) {
            if (!this.isCorrect(uncrypt_pw)) {
                Meldungen.show_Dialog(ErrorMsg.msg[44], "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            Meldungen.show_Dialog(ErrorMsg.msg[45], "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public User(String iD, Boolean init) {
        this.nutzer_id = iD;
        if (init) {
            this.getDatenAusDB();
        }
    }

    public User(String iD) {
        this.nutzer_id = iD;
    }

    public User() {
    }

    /**
     * prüft, ob das eingegebene Passwort korrekt ist;
     * dafür wird das eingegebene Passwort gehasht und mit dem in der Datenbank gespeicherten Hashwert verglichen
     *
     * @param kt_pw     das Passwort
     * @return          true, wenn korrekt
     */
    private boolean isCorrect(String kt_pw) {
        if (this.pw.equals(getHash(kt_pw))) {
            this.accepted = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * berechnet den SHA1-Hash eines Strings
     *
     * @param text      der zu hashende String
     * @return          der Hash des Strings
     */
    public static String getHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(text.getBytes());
            byte[] array = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; i++) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public boolean getDatenAusDB() {
        boolean nutzer_vorhanden = false;
        try {
            Statement stmt = DB_Backend.getConnection().createStatement();
            ResultSet ress = stmt.executeQuery("SELECT * FROM nutzer WHERE nutzer_id='" + this.nutzer_id + "'");
            while (ress.next()) {
                this.pw = ress.getString("passwort");
                if (ress.getString("buchung_bearbeiten").equals("1")) {
                    this.buchung_bearbeiten = true;
                }
                if (ress.getString("gast_bearbeiten").equals("1")) {
                    this.gast_bearbeiten = true;
                }
                if (ress.getString("nutzer_bearbeiten").equals("1")) {
                    this.nutzer_bearbeiten = true;
                }
                if (ress.getString("zimmer_bearbeiten").equals("1")) {
                    this.zimmer_bearbeiten = true;
                }
                if (ress.getString("kategorie_bearbeiten").equals("1")) {
                    this.kategorie_bearbeiten = true;
                }
                if (ress.getString("konfig_bearbeiten").equals("1")) {
                    this.konfig_bearbeiten = true;
                }
                nutzer_vorhanden = true;
            }
            return nutzer_vorhanden;
        } catch (SQLException ex) {
            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            Meldungen.show_Dialog(ErrorMsg.msg[45], "Error", JOptionPane.ERROR_MESSAGE);
            return nutzer_vorhanden;
        }
    }

    public boolean isAccepted() {
        return this.accepted;
    }

    /**
     * aktualisiert die GUI je nach Rechten des Nutzers;
     * dabei werden die einzelnen Rechte des Nutzers überprüft und die zugehörigen
     * Schaltflächen und Tabs der GUI aktiviert bzw. deaktiviert
     */
    public void setVisibleItems() {
        GUI_main.jTabbedPane_Main.setEnabledAt(1, true);
        GUI_main.jTabbedPane_Main.setEnabledAt(2, true);
        GUI_main.jTabbedPane_Main.setEnabledAt(3, true);
        GUI_main.jButton_Buchung_bearbeiten.setEnabled(this.buchung_bearbeiten);
        GUI_main.jButton_Buchung_stornieren.setEnabled(this.buchung_bearbeiten);
        GUI_main.jButton_Gast_bearbeiten.setEnabled(this.gast_bearbeiten);
        GUI_main.jTabbedPane_Konfiguration.setEnabledAt(2, this.kategorie_bearbeiten);
        GUI_main.jTabbedPane_Konfiguration.setEnabledAt(1, this.nutzer_bearbeiten);
        GUI_main.jTabbedPane_Konfiguration.setEnabledAt(3, this.zimmer_bearbeiten);
        GUI_main.jTabbedPane_Konfiguration.setEnabledAt(0, this.konfig_bearbeiten);
        GUI_main.jTabbedPane_Konfiguration.setEnabledAt(4, this.konfig_bearbeiten);
        GUI_main.jTabbedPane_Konfiguration.setEnabledAt(5, this.konfig_bearbeiten);
        GUI_main.jMenu_saveConfig.setEnabled(this.konfig_bearbeiten);
        if (!GUI_main.jTabbedPane_Konfiguration.isEnabledAt(0) && !GUI_main.jTabbedPane_Konfiguration.isEnabledAt(1) && !GUI_main.jTabbedPane_Konfiguration.isEnabledAt(2) && !GUI_main.jTabbedPane_Konfiguration.isEnabledAt(3) && !GUI_main.jTabbedPane_Konfiguration.isEnabledAt(4) && !GUI_main.jTabbedPane_Konfiguration.isEnabledAt(5)) {
            GUI_main.jTabbedPane_Main.setEnabledAt(4, false);
        } else {
            GUI_main.jTabbedPane_Main.setEnabledAt(4, true);
        }
        GUI_main.jTabbedPane_Konfiguration.setSelectedIndex(-1);
    }

    public static void refreshData() {
        User.test = false;
        DB_Helpers.getComboItems((DefaultComboBoxModel) GUI_main.jCombo_Admin_Benutzer.getModel(), User.Benutzer_IDs, "nutzer", "nutzer_id", "nutzer_id");
        GUI_main.jCombo_Admin_Benutzer.setSelectedIndex(-1);
        test = true;
    }

    public int insertAtDB() {
        ArrayList list = new ArrayList();
        list.add(this.nutzer_id);
        list.add(this.pw);
        if (this.buchung_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        if (this.gast_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        if (this.zimmer_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        if (this.kategorie_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        if (this.nutzer_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        if (this.konfig_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        return DB_insert.insertAt("nutzer", list);
    }

    public int updateDB(boolean neues_pw) {
        ArrayList list = new ArrayList();
        list.add("nutzer_id");
        list.add(this.nutzer_id);
        if (neues_pw) {
            list.add("passwort");
            list.add(this.pw);
        }
        list.add("buchung_bearbeiten");
        if (this.buchung_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        list.add("gast_bearbeiten");
        if (this.gast_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        list.add("zimmer_bearbeiten");
        if (this.zimmer_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        list.add("kategorie_bearbeiten");
        if (this.kategorie_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        list.add("nutzer_bearbeiten");
        if (this.nutzer_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        list.add("konfig_bearbeiten");
        if (this.konfig_bearbeiten) {
            list.add('1');
        } else {
            list.add('0');
        }
        return DB_insert.update("nutzer", list, "nutzer_id", this.nutzer_id_old);
    }

    public int delDB() {
        if (Meldungen.show_confirm_Dialog(ErrorMsg.msg[46], "?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            return DB_Helpers.delEntry("nutzer", "nutzer_id", this.nutzer_id);
        } else return 0;
    }

    public void setBenutzer_ID(String Benutzer_ID) {
        this.nutzer_id = Benutzer_ID;
    }

    public void setBenutzer_ID_old(String Benutzer_ID) {
        this.nutzer_id_old = Benutzer_ID;
    }

    public void setPasswort(String Passwort) {
        this.pw = getHash(Passwort);
    }

    public void setBuchung_bearbeiten(boolean Buchung_bearbeiten) {
        this.buchung_bearbeiten = Buchung_bearbeiten;
    }

    public void setGast_bearbeiten(boolean Gast_bearbeiten) {
        this.gast_bearbeiten = Gast_bearbeiten;
    }

    public void setZimmer_bearbeiten(boolean Zimmer_bearbeiten) {
        this.zimmer_bearbeiten = Zimmer_bearbeiten;
    }

    public void setKategorie_bearbeiten(boolean Kategorie_bearbeiten) {
        this.kategorie_bearbeiten = Kategorie_bearbeiten;
    }

    public void setNutzer_bearbeiten(boolean Nutzer_bearbeiten) {
        this.nutzer_bearbeiten = Nutzer_bearbeiten;
    }

    public void setKonfig_bearbeiten(boolean Konfig_bearbeiten) {
        this.konfig_bearbeiten = Konfig_bearbeiten;
    }

    public String getBenutzer_ID() {
        return this.nutzer_id;
    }

    public boolean getBuchung_bearbeiten() {
        return this.buchung_bearbeiten;
    }

    public boolean getGast_bearbeiten() {
        return this.gast_bearbeiten;
    }

    public boolean getZimmer_bearbeiten() {
        return this.zimmer_bearbeiten;
    }

    public boolean getKategorie_bearbeiten() {
        return this.kategorie_bearbeiten;
    }

    public boolean getNutzer_bearbeiten() {
        return this.nutzer_bearbeiten;
    }

    public boolean getKonfig_bearbeiten() {
        return this.konfig_bearbeiten;
    }
}
