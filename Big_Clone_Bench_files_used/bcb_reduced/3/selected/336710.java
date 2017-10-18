package com.increg.game.bean;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.increg.commun.exception.UnauthorisedUserException;

/**
 * @author Manu
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class SecurityBean {

    /**
     * Formatteur de la date
     */
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");

    /**
     * Phrase cl� pour le calcul du code CRC
     */
    private String passPhrase = "";

    /**
     * Constructeur qui fait la v�rification
     * @param id Identifiant de l'utilisateur
     * @param crc Code propos� � v�rifier
     * @param aPassPhrase Phrase cl� pour le calcul
     * @throws UnauthorisedUserException Si le code est invalide
     */
    public SecurityBean(String id, String crc, String aPassPhrase) throws UnauthorisedUserException {
        passPhrase = aPassPhrase;
        if (!crc.equals(getCRC(id))) {
            throw new UnauthorisedUserException();
        }
    }

    /**
     * Calcule le CRC pour l'id fourni
     * @param id pseudo du user
     * @return code crc
     */
    public String getCRC(String id) {
        String crcCalc = null;
        String msg = id + passPhrase + dateFormat.format(Calendar.getInstance().getTime());
        crcCalc = calcCRC(msg);
        return crcCalc;
    }

    /**
     * Calcule le CRC MD5 et conversion h�xa
     * @param phrase Phrase � sign�e
     * @return Chaine CRC
     */
    public static String calcCRC(String phrase) {
        StringBuffer crcCalc = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(phrase.getBytes());
            byte[] tabDigest = md.digest();
            for (int i = 0; i < tabDigest.length; i++) {
                String octet = "0" + Integer.toHexString(tabDigest[i]);
                crcCalc.append(octet.substring(octet.length() - 2));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return crcCalc.toString();
    }

    /**
     * @return Phrase cl� pour le calcul du code CRC
     */
    public String getPassPhrase() {
        return passPhrase;
    }

    /**
     * @param string Phrase cl� pour le calcul du code CRC
     */
    public void setPassPhrase(String string) {
        passPhrase = string;
    }
}
