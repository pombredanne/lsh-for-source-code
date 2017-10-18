package Modele;

import java.lang.management.GarbageCollectorMXBean;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gère un enregistrement dans un processus séparé
 * @author Mathieu PASSENAUD
 * @version 1.0
 */
public class Enregistrement extends Thread {

    int id_fichier;

    int id_code;

    int sit;

    String requete;

    String a;

    String MD5 = "";

    byte[] b;

    /**
     * Transforme les données bytes en chaine, calcule le MD5 et envoie les données dans le cache Donnees
     * @param données à écrire
     * @param numéro du fichier
     * @param position des données dans le fichier
     */
    public Enregistrement(byte[] b, int id, int sit) {
        this.id_fichier = id;
        this.sit = sit;
        this.b = b;
    }

    @Override
    public void run() {
        System.gc();
        byte[] md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5").digest(b);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Enregistrement.class.getName()).log(Level.SEVERE, null, ex);
        }
        MD5 = "";
        for (int i = 0; i < md5.length; i++) {
            MD5 = MD5 + md5[i];
        }
        id_code = Donnees.rechercherDonnee(b, MD5);
        new Constituer(id_fichier, id_code, sit);
        try {
            this.finalize();
        } catch (Throwable ex) {
            Logger.getLogger(Enregistrement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
