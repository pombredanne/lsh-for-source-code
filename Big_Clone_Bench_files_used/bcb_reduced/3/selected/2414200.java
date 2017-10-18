package it.unipg.bipod.applicationLogic;

import it.unipg.bipod.ResManager;
import it.unipg.bipod.dataAccess.*;
import it.unipg.bipod.dataModel.*;
import java.security.*;
import java.util.*;
import java.io.IOException;

/**
 * AuthManager fornisce metodi statici per le operazioni di autenticazione
 * al sistema e recupero e generazione delle password.<br>
 * L'autenticazione avviene tramite il metodo login, il quale restituisce
 * un oggetto {@code DocenteApplication} o {@code SegretarioApplication}
 * appropriato al tipo di utente che si è autenticato. Tutte le operazioni successive
 * vengono poi demandate ai metodi di questi oggetti.
 * 
 * @author Lorenzo Porzi
 * @see DocenteApplication
 * @see SegretarioApplication
 *
 */
public abstract class AuthManager {

    /**
	 * Il set dei caratteri da cui si attinge per la generazione automatica delle password.
	 */
    private static final String CHARSET = "0123456789abcdefghijklmnopqrstuvwxyz%!$.";

    private static DataManager dataManager;

    /**
	 * Effettua il login al sistema.<br>
	 * Questo metodo cerca nella base di dati l'utente specificato e se il login
	 * ha successo restituisce un oggetto {@link Application} appropriato al tipo
	 * di utente (Docente o Segretario).
	 * 
	 * @param nomeUtente il nome utente da autenticare.
	 * @param password la password.
	 * @return un oggetto Application associato all'utente che si sta autenticando
	 * oppure {@code null} se l'autenticazione non va a buon fine (l'utente non è presente
	 * nella base di dati condivisa oppure la password non corrisponde).
	 * @throws UtenteNonRegistratoException se un utente presente nella base di dati condivisa
	 * ma non registrato a BiPoD cerca di effettuare il login.
	 * @throws UtenteNonAttivoException se un utente non attivo tenta di eseguire il login.
	 * @throws AuthException
	 */
    public static Application login(String nomeUtente, String password) throws AuthException {
        try {
            if (dataManager == null) dataManager = new DataManager();
            Utente utente = dataManager.getUtenteFromNomeUtente(nomeUtente);
            String passwordHash = digest(password);
            if (utente == null) return null;
            if (passwordHash.equals(utente.getPassword()) && nomeUtente.equals(utente.getNomeUtente())) {
                Registrazione registrazione = dataManager.getRegistrazioneFromUtente(utente.getNomeUtente());
                if (registrazione == null) throw new UtenteNonRegistratoException(); else if (!registrazione.isAttiva()) throw new UtenteNonAttivoException();
                if (utente instanceof Docente) return new DocenteApplication(dataManager, (Docente) utente, registrazione); else if (utente instanceof Segretario) return new SegretarioApplication(dataManager, (Segretario) utente, registrazione);
            }
            return null;
        } catch (DataAccessException e) {
            throw new AuthException(e);
        } catch (IOException e) {
            throw new AuthException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new AuthException(e);
        }
    }

    /**
	 * Invia all'utente avente l'email specificata i suoi dati di accesso.<br>
	 * I dati vengono inviati via email all'indirizzo memorizzato nel sistema,
	 * viene generata una nuova password.
	 * 
	 * @param email l'email dell'utente che vuole recuperare i dati
	 * @throws UtenteNonRegistratoException se non si trova nessuna corrispondenza per l'email
	 * specificata o se la si trova ma l'utente non è registrato a BiPoD.
	 * @throws UtenteNonAttivoException se un utente disattivato richiede i suoi dati.
	 * @throws AuthException
	 */
    public static void recuperaDati(String email) throws AuthException {
        try {
            DataManager dataManager = new DataManager();
            Properties settings = ResManager.getInstance().getDataSettings();
            Utente utente = dataManager.getUtenteFromEmail(email);
            if (utente == null) throw new UtenteNonRegistratoException();
            Registrazione registrazione = dataManager.getRegistrazioneFromUtente(utente.getNomeUtente());
            if (registrazione == null) throw new UtenteNonRegistratoException();
            if (!registrazione.isAttiva()) throw new UtenteNonAttivoException();
            String password = creaPassword(Integer.parseInt(settings.getProperty("passwordLength", "8")));
            String passwordHash = digest(password);
            utente.setPassword(passwordHash);
            dataManager.updateEntity(utente);
            String testo = ResManager.getInstance().getResource("newPasswordMail");
            testo = testo.replaceAll("<password>", password);
            testo = testo.replaceAll("<user>", utente.getNomeUtente());
            String soggetto = settings.getProperty("newPasswordMailSubject");
            if (!Mailer.sendMail(testo, soggetto, utente.getEmail())) throw new AuthException("Errore nell'invio dell'email");
            Logger.log(dataManager, Operazione.MODIFICA_REGISTRAZIONE, registrazione, utente.getNomeUtente() + " richiede nuova password");
        } catch (DataAccessException e) {
            throw new AuthException("Errore di accesso ai dati");
        } catch (IOException e) {
            throw new AuthException("Errore");
        } catch (NoSuchAlgorithmException e) {
            throw new AuthException("Errore");
        }
    }

    /**
	 * Crea una password casuale di lunghezza specificata.<br>
	 * I caratteri di cui sara' composta la password vengono scelti tra quelli
	 * di CHARSET.
	 * 
	 * @param length la lunghezza della password da generare
	 * @return la password generata
	 */
    public static String creaPassword(int length) {
        Random random = new Random(System.currentTimeMillis());
        StringBuffer passwordd = new StringBuffer();
        for (int i = 0; i < length; ++i) passwordd.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        return passwordd.toString();
    }

    /**
	 * Esegue la codifica SHA-1 dell'argomento.<br>
	 * Il risultato viene fornito come concatenazione delle rappresentazioni
	 * esadecimali dei byte ottenuti dalla codifica. Ogni byte codificato
	 * viene rappresentato da 2 caratteri, quindi il risultato ha sempre una
	 * lunghezza di 40 caratteri.
	 * 
	 * @param stringa la stringa da codificare
	 * @return l'hash SHA-1 dell'argomento
	 * @throws NoSuchAlgorithmException
	 */
    public static String digest(String stringa) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        byte[] digest = messageDigest.digest(stringa.getBytes());
        StringBuffer hash = new StringBuffer();
        String tmp;
        for (byte b : digest) {
            tmp = Integer.toHexString(b + 128);
            hash.append(tmp.length() == 1 ? "0" + tmp : tmp);
        }
        return hash.toString();
    }
}
