package net.sf.ideoreport.license;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

/**
 * Classe utilitaire de g�n�ration de cl� � partir d'un objet ; la g�n�ration de
 * cl� est bas�e sur le principe de s�rialisation, aussi il est indispensable de
 * ne l'utiliser qu'avec des objets Serializable.
 * 
 * La cl� g�n�r�e r�pond aux exigences suivantes :
 * <li>Deux objets diff�rents donnent une cl� diff�rente
 * <li>Deux objets �gaux donnent la m�me cl�.
 * <li>La cl� est une cha�ne de caract�res compos�e exclusivement des caract�res
 * 		A-Z, a-z, 0-9, '+', '=' et '_'.
 *
 * @author jbeausseron@sqli.com
 */
class KeyGenerator {

    /**
    * Logger for this class
    */
    private static final Log LOGGER = LogFactory.getLog(KeyGenerator.class);

    /**
	 * caract�res autoris�s dans la base 64
	 */
    private static final String m_strBase64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * Convertit un tableau d'octets en une cha�ne de caract�res Base 64
     * comme dans le format MIME
     * @param aValue tableau � convertir.
     */
    protected static String toBase64(byte[] aValue) {
        int byte1;
        int byte2;
        int byte3;
        int iByteLen = aValue.length;
        StringBuffer tt = new StringBuffer();
        for (int i = 0; i < iByteLen; i += 3) {
            boolean bByte2 = (i + 1) < iByteLen;
            boolean bByte3 = (i + 2) < iByteLen;
            byte1 = aValue[i] & 0xFF;
            byte2 = (bByte2) ? (aValue[i + 1] & 0xFF) : 0;
            byte3 = (bByte3) ? (aValue[i + 2] & 0xFF) : 0;
            tt.append(m_strBase64Chars.charAt(byte1 / 4));
            tt.append(m_strBase64Chars.charAt((byte2 / 16) + ((byte1 & 0x3) * 16)));
            tt.append(((bByte2) ? m_strBase64Chars.charAt((byte3 / 64) + ((byte2 & 0xF) * 4)) : '='));
            tt.append(((bByte3) ? m_strBase64Chars.charAt(byte3 & 0x3F) : '='));
        }
        return tt.toString();
    }

    /**
      * Construit une cl� � partir d'un objet pass� en entr�e ; l'objet doit
      * s�rialisable, car le processus se base sur la s�rialisation de l'objet.
      * 
      * @param pObject objet servant de base pour calculer la cl�
      * @return la cl� calcul�e
      * @throws Exception
      */
    protected static String getKey(Serializable pObject) throws Exception {
        String vRetour = "";
        try {
            ObjectOutputStream vOOS;
            ByteArrayOutputStream vBOS = new ByteArrayOutputStream();
            vOOS = new ObjectOutputStream(vBOS);
            vOOS.writeObject(pObject);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] vEncodedBytes = digest.digest(vBOS.toByteArray());
            vRetour = toBase64(vEncodedBytes).replace('/', '_');
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("generating key for object [" + pObject + "] : key = [" + vRetour + "]");
            }
        } catch (IOException e) {
            System.err.println("erreur getKey : " + e);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("erreur getKey : " + e);
        }
        return vRetour;
    }
}
