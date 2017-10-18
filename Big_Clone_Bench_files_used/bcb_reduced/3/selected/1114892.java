package fr.gouv.defense.terre.esat.formathlon.metier.uc007;

import fr.gouv.defense.terre.esat.formathlon.entity.News;
import fr.gouv.defense.terre.esat.formathlon.entity.Utilisateur;
import fr.gouv.defense.terre.esat.formathlon.metier.exception.MetierException;
import fr.gouv.defense.terre.esat.formathlon.persistence.exception.LdapConnexionException;
import fr.gouv.defense.terre.esat.formathlon.persistence.exception.LdapNameNotFoundException;
import fr.gouv.defense.terre.esat.formathlon.persistence.ldap.LdapPersistence;
import fr.gouv.defense.terre.esat.formathlon.persistence.sessionbean.NewsPersistence;
import fr.gouv.defense.terre.esat.formathlon.persistence.sessionbean.UtilisateurPersistence;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import org.springframework.security.crypto.codec.Base64;

/**
 *
 * @author maxime.guinchard
 */
@Stateless
@LocalBean
public class Uc007Metier {

    @EJB
    private UtilisateurPersistence utilisateurPersistence;

    @EJB
    private LdapPersistence ldapPersistence;

    @EJB
    private NewsPersistence newsPersistence;

    /**
     * Retourne les 5 dernières news.
     * @return List < News >
     */
    public List<News> read5LastNews() {
        return newsPersistence.readXLastNews(5);
    }

    /**
     * Methode qui permet de recuperer les infos de l'utilisateur en fonction
     * de son login.
     * 
     * @param login login
     * @return Utilisateur utilisateur
     */
    public Utilisateur getUtilisateurFromBdd(String login) {
        return utilisateurPersistence.read(login);
    }

    /**
     * Methode qui recupere les informations fourni par le ldap pour un
     * utilisateur.
     * 
     * 
     * @param login login
     * @return Utilisateur utilisateur
     */
    public Utilisateur getUtilisateurFromLdap(String login) {
        Utilisateur u = null;
        try {
            u = ldapPersistence.findByPrimaryKey(login);
        } catch (LdapConnexionException e) {
            throw new MetierException("uc_007_connexionLdapImpossible");
        } catch (LdapNameNotFoundException e) {
            throw new MetierException("uc_007_loginLdapNonTrouve");
        }
        return u;
    }

    /**
     * Methode qui permet de hasher le mot de passe avec SHA-1.
     * 
     * @param mdp mdp
     * @return String mdp
     */
    public String hasheMotDePasse(String mdp) {
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
        }
        sha.reset();
        sha.update(mdp.getBytes());
        byte[] digest = sha.digest();
        String pass = new String(Base64.encode(digest));
        pass = "{SHA}" + pass;
        return pass;
    }
}
