package funkcije;

import baza.Rad_baza;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Klasa sa Nekim globalnim funkcijama koje se mogu koristit u unosu i ETL-u
 * @author Matija Novak
 */
public class Globalne_funkcije {

    /**
     * Funkcija koja vrši md_5 naredbu nad nekim tekstom
     * @param tekst
     * @return Vraća md5(tekst)
     */
    public String md5_funk(String tekst) {
        MessageDigest m = null;
        byte[] data = tekst.getBytes();
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("Greška u kreiranju md5 sažetka");
        }
        m.update(data, 0, data.length);
        return new BigInteger(1, m.digest()).toString(16);
    }

    /**
         * Samo testna funkcija za slanje maila treba zamjeniti sa fukcijom posalji_mail
         * @deprecated
         * @param mail
         */
    @Deprecated
    public void salji_mail(String mail) {
        System.out.println("Mail poslan");
    }

    /**
     * Šalje mail nekom kroisniku
     * @param poruka - poruka
     * @param sifra_projekta - šifra projekta
     * @return vraća true ako je sve ok, inače false
     */
    public synchronized boolean posalji_mail(String poruka, String sifra_projekta) {
        String from_g = "matnovak@foi.hr";
        String to_g;
        Rad_baza rad_baza = new Rad_baza();
        to_g = rad_baza.daj_string_podatak(1, "projekti", "mail", "sifra_projekta=" + sifra_projekta);
        try {
            String from = from_g;
            String to = to_g;
            String text = poruka;
            java.util.Properties properties = System.getProperties();
            properties.put("mail.smtp.host", "localhost");
            Session sessionL = Session.getInstance(properties, null);
            MimeMessage message = new MimeMessage(sessionL);
            Address fromAddress = new InternetAddress(from);
            message.setFrom(fromAddress);
            Address[] toAddresses = InternetAddress.parse(to);
            message.setRecipients(Message.RecipientType.TO, toAddresses);
            message.setSubject("poruka_za_obradu");
            message.setText(text);
            Transport.send(message);
        } catch (Exception e) {
            System.out.println(e.toString());
            return false;
        }
        return true;
    }
}
