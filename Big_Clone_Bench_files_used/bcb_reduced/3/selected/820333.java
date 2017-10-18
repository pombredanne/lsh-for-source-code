package photobook.data.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <p>Cette classe permet de faire le cryptage en SHA1 sur 160 bits.<p>
 * @author Pierre-Eric OUDIN
 * @version 0.2
 *
 */
public class CryptSHA1 {

    /**
	 * Cette m�thode convertit en Hexad�cimal la chaine entr�e. 
	 * @param Tableau de bytes des donn�es � convertir
	 * @return Cha�ne en hexa 
	 */
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
	 * Cette m�thode fait appel a convertToHex afin de produire la cha�en SHA1 voulue. 
	 * @param Cha�ne de caract�res � convertir
	 * @return Cha�ne encod�e en SHA1
	 * @throws UnsupportedEncodingException 
     * @throws NoSuchAlgorithmException
	 */
    private static String simpleCompute(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("utf-8"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    /**
	 * Cette m�thode fait appel a simpleCOmpute afin de produire la cha�en SHA1 voulue en suivant un algorithme de cryprtage
	 * pour l'authentification � partir du login et du password. 
	 * @param String login
	 * @param String password
	 * @return Cha�ne encod�e en SHA1
     * @throws UnsupportedEncodingException 
     * @throws NoSuchAlgorithmException 
	 */
    public static String authCompute(String login, String passwd) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String text = login + ":" + passwd;
        text.toUpperCase();
        return simpleCompute(text).toUpperCase();
    }

    /**
     * Cette m�thode cr�e un identifiant unique pour un utilisateur. Cette m�thode ne peut �tre appel�e
     *  qu'une fois lors de la cr�ation d'un nouvel utilisateur
     * @param String login
     * @return String, Identifiant unique SHA1 pour un utilisateur
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String genUserID(String login) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return simpleCompute(login + ":" + System.currentTimeMillis()).toUpperCase();
    }

    /**
     * Cette m�thode g�n�re un identifiant unique pour une photo.Cette m�thode ne peut �tre appel�e
     *  qu'une fois lors de la cr�ation d'une nouvelle photo.
     * @param String login (le login du propri�atire de la photo)
     * @param String photoName
     * @return String, Identifiant unique SHA1 pour une photo.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String genPhotoID(String login, String photoName) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return simpleCompute(photoName + ":" + login + ":" + System.currentTimeMillis()).toUpperCase();
    }

    /**
     * Cette m�thode ne peut �tre appel�e
     *  qu'une fois lors de la cr�ation d'un nouvel album.
     * @param String login (le login du propri�atire de l'album)
     * @param String albumName
     * @return String, Identifiant unique SHA1 pour une photo.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String genAlbumID(String login, String albumName) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return simpleCompute(albumName + ":" + login + ":" + System.currentTimeMillis()).toUpperCase();
    }

    /**
     * 
     * @param login (le login du propri�taire du groupe)
     * @param groupName 
     * @return String, Identifiant unique SHA1 pour un Album.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String genGroupID(String login, String groupName) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return simpleCompute(groupName + ":" + login + ":" + System.currentTimeMillis()).toUpperCase();
    }

    /**
     * 
     * @param user (utilisateur qui ajoute la note)
     * @param photoId (identifiant de la photo) 
     * @return String, Identifiant unique SHA1 pour une note.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String genMarkId(String user, String photoId) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return simpleCompute(photoId + ":" + user + ":" + System.currentTimeMillis()).toUpperCase();
    }

    /**
     * 
     * @param user (utilisateur qui ajoute la note)
     * @param photoId (identifiant de la photo) 
     * @return String, Identifiant unique SHA1 pour une note.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String genCommentId(String user, String photoId) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return simpleCompute(photoId + ":" + user + ":" + System.currentTimeMillis()).toUpperCase();
    }
}
