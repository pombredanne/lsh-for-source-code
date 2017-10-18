package org.paoo.ianus.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.paoo.ianus.exceptions.UtenteNonValidoException;
import org.paoo.ianus.utils.HibernateUtils;

/**
 * Classe persistente che modella un utente
 * @author Paolo Turello
 *
 */
public class Utente extends Persistent {

    protected String userId;

    protected Short ultimoEsercizioId;

    protected Esercitazione ultimaEsercitazione;

    protected String passwordMD5;

    /**
	 * Costruttore senza parametri (per Hibernate)
	 *
	 */
    public Utente() {
    }

    /**
	 * Costruttore con parametro per l'inizializzazione
	 * @param user
	 */
    public Utente(String user, String password) throws UtenteNonValidoException {
        if (!exists(user)) {
            userId = user;
            ultimaEsercitazione = null;
            ultimoEsercizioId = 0;
            passwordMD5 = getMD5(password);
        } else {
            throw new UtenteNonValidoException(user);
        }
    }

    /**
	 * Metodo statico che genera l'hash (MD5) di una stringa
	 * @param password
	 * @return l'MD5
	 */
    public static String getMD5(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte[] digest = md.digest();
            String out = "";
            for (int i = 0; i < digest.length; i++) {
                out += digest[i];
            }
            return out;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Manca l'MD5 (piuttosto strano)");
        }
        return "";
    }

    /**
	 * Controlla che l'utente non sia gi� presente
	 * @param user
	 * @return true se l'utente esiste, false altrimenti
	 */
    private boolean exists(String user) {
        Session s = HibernateUtils.getSessionFactory().getCurrentSession();
        Transaction tx2 = s.beginTransaction();
        List users = s.createQuery("select a from Utente as a where a.userId = '" + user + "'").list();
        tx2.commit();
        if (users.size() > 0) return true;
        return false;
    }

    /**
	 * Assegna l'ultimo esercizio svolto e l'ultima esercitazione visualizzabile dall'utente
	 * @param es
	 * @param ex
	 */
    public void setLast(Esercitazione es, Short exId) {
        ultimoEsercizioId = exId;
        ultimaEsercitazione = es;
    }

    public boolean equals(Object o) {
        Utente tmp = (Utente) o;
        if (!userId.equals(tmp.getUserId())) return false;
        if (!ultimoEsercizioId.equals(tmp.getUltimoEsercizioId())) return false;
        if (!ultimaEsercitazione.equals(tmp.getUltimaEsercitazione())) return false;
        if (!passwordMD5.equals(tmp.getPasswordMD5())) return false;
        return true;
    }

    public String toString() {
        return "id = " + userId + ", lastEs = " + (ultimaEsercitazione == null ? "null" : ultimaEsercitazione.getIndexEsercitazione()) + ", lastEx = " + ultimoEsercizioId;
    }

    public Short getUltimoEsercizioId() {
        return ultimoEsercizioId;
    }

    protected void setUltimoEsercizioId(Short ultimoEsercizioId) {
        this.ultimoEsercizioId = ultimoEsercizioId;
    }

    public Esercitazione getUltimaEsercitazione() {
        return ultimaEsercitazione;
    }

    protected void setUltimaEsercitazione(Esercitazione ultimaEsercitazione) {
        this.ultimaEsercitazione = ultimaEsercitazione;
    }

    public String getUserId() {
        return userId;
    }

    protected void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPasswordMD5() {
        return passwordMD5;
    }

    protected void setPasswordMD5(String passwordMD5) {
        this.passwordMD5 = passwordMD5;
    }
}
