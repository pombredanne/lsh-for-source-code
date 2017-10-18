package com.doculibre.intelligid.securite.authentification.hibernate;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import org.apache.commons.codec.binary.Base64;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import com.doculibre.intelligid.entites.UtilisateurIFGD;
import com.doculibre.intelligid.entites.ddv.StatutUtilisateur;
import com.doculibre.intelligid.securite.authentification.GestionnaireAuthentification;
import cryptix.jce.provider.CryptixCrypto;

public class GestionnaireAuthentificationHibernate implements GestionnaireAuthentification {

    /**
	 * Objet reçu de Spring.
	 */
    private SessionFactory sessionFactory;

    /**
	 * @return Objet reçu de Spring.
	 */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
	 * Méthode appelée par Spring.
	 * 
	 * @param sessionFactory
	 */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public boolean authentifier(String nomUtilisateur, String motDePasse) {
        boolean authentifie;
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria(UtilisateurIFGD.class);
        criteria.add(Restrictions.eq("nomUtilisateur", nomUtilisateur).ignoreCase());
        criteria.createAlias("statut", "statut");
        criteria.add(Restrictions.eq("statut.code", StatutUtilisateur.CODE_ACTIF).ignoreCase());
        UtilisateurIFGD utilisateur = (UtilisateurIFGD) criteria.uniqueResult();
        String motDePasseEncode = encoder(motDePasse);
        if (utilisateur != null && utilisateur.getMotPasse().equals(motDePasseEncode)) {
            authentifie = true;
        } else {
            authentifie = false;
        }
        return authentifie;
    }

    @Override
    public boolean isMotDePasseEncodePersiste() {
        return true;
    }

    @Override
    public String encoder(String motDePasse) {
        return getHash(motDePasse);
    }

    /**
	 * Generates a hash for password using salt from
	 * AuthDataApplication.getSalt() and returns the hash encoded as a Base64
	 * String.
	 * 
	 * @see AuthDataApplication.getSalt();
	 * @param password
	 *            to encode
	 * @return base64 encoded SHA hash, 28 characters
	 */
    public static String getHash(String password) {
        if (password != null) {
            try {
                MessageDigest md4 = MessageDigest.getInstance("MD4", new CryptixCrypto());
                byte[] pwdBytes = password.getBytes("UTF-8");
                md4.update(pwdBytes);
                String hash = new String(Base64.encodeBase64(md4.digest()));
                return hash;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    /**
	 * Performs hash on given password and compares it to the correct hash.
	 * 
	 * @true if hashed password is correct
	 */
    public static boolean checkPassword(String motDePasseNonEncode, String motDePasseEncode) {
        return motDePasseEncode.equals(getHash(motDePasseNonEncode));
    }

    public static void main(String[] args) {
        Provider pd = new CryptixCrypto();
        Security.addProvider(pd);
        for (int i = 0; i < 5; i++) {
            System.out.println(getHash("password"));
        }
    }
}
