import baza.Kreiranje_baza_inicjalizacija;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pajdo
 */
public class nekaj {

    public static void main(String[] args) {
        String tekst = "matnovak_2412matnovak_etl";
        MessageDigest m = null;
        byte[] data = tekst.getBytes();
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Kreiranje_baza_inicjalizacija.class.getName()).log(Level.SEVERE, null, ex);
        }
        m.update(data, 0, data.length);
        System.out.println("MD5: " + new BigInteger(1, m.digest()).toString(16));
        BigInteger i = new BigInteger(1, m.digest());
        System.out.println("MD5: " + String.format("%1$032X", i));
        kljuc_att_izvora("10,1,dnevnik2,datum");
    }

    public static java.util.Properties kljuc_att_izvora(String kljuc) {
        System.out.println(kljuc);
        String polje[] = kljuc.split(",");
        System.out.println(polje.length);
        java.util.Properties i = new java.util.Properties();
        i.setProperty("vrsta", polje[0].trim());
        i.setProperty("sifra", polje[1].trim());
        i.setProperty("tabela", polje[2].trim());
        i.setProperty("kolona", polje[3].trim());
        return i;
    }
}
