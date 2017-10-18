package db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Trieda sluziaca na enkrypciu dat
 * @author marek
 */
public class Security {

    /**
     * 
     * @param retazec Dodany retazec kodovany pomocou sifrovacie algoritmu
     * @return Vracia hea podobu zakodovaneho retazca
     * @throws java.security.NoSuchAlgorithmException
     */
    public String encrypt(String retazec) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(retazec.getBytes());
        return HexString.bufferToHex(md.digest());
    }

    /**
     * 
     * @param retazec Dodany retazec pre poravnanie
     * @param hexCrypt Encryptovany povodny retazec
     * @return Vracia true ak sa retazce zhoduju, inak false
     * @throws java.security.NoSuchAlgorithmException
     */
    public boolean match(String retazec, String hexCrypt) throws NoSuchAlgorithmException {
        String temp = encrypt(retazec);
        return (temp.equals(hexCrypt));
    }
}
