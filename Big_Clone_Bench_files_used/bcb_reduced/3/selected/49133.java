package metso.paradigma.core.business.model;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

/** 
 * Rappresenta l'utente del portale a cui e' associato un operatore sanitario. 
 */
public class Utente implements Serializable {

    private static final long serialVersionUID = 3441209325602039408L;

    private int id;

    private String username;

    private String password;

    private Ruolo ruolo;

    private Operatore operatore;

    public static final String GUEST_USERNAME = "guest";

    /**
	 * @return l'id assegnato all'utente in fase di salvataggio nella base dati
	 */
    public int getId() {
        return id;
    }

    /**
	 * @param id l'id da assegnare all'utente
	 */
    public void setId(int id) {
        this.id = id;
    }

    /**
	 * @return lo username dell'utente
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * @param username lo username da assegnare all'utente
	 */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
	 * Assegna la password all'utente, prima pero' ne calcola l'hash in modo che sulla
	 * base dati sia salvato quest'ultimo e non la password in chiaro.
	 * @param password la password da assegnare all'utente
	 */
    public void setPassword(String password) {
        this.setPasswordHash(calculateHash(password));
    }

    protected String getPasswordHash() {
        return password;
    }

    protected void setPasswordHash(String passwordHash) {
        this.password = passwordHash;
    }

    protected int getRuoloInt() {
        return this.getRuolo().ruoloInt;
    }

    protected void setRuoloInt(int ruoloInt) {
        this.setRuolo(Ruolo.get(ruoloInt));
    }

    /**
	 * @return il ruolo dell'utente
	 */
    public Ruolo getRuolo() {
        return ruolo;
    }

    /**
	 * @param ruolo il ruolo da assegnare all'utente
	 */
    public void setRuolo(Ruolo ruolo) {
        this.ruolo = ruolo;
    }

    /**
	 * @return l'operatore associato all'utente
	 */
    public Operatore getOperatore() {
        return operatore;
    }

    /**
	 * @param operatore l'operatore da assegnare all'utente
	 */
    public void setOperatore(Operatore operatore) {
        this.operatore = operatore;
    }

    /**
	 * Verifica che il ruolo dell'utente sia quello di amministratore.
	 * @return true se l'utente e' amministratore
	 */
    public boolean isAdmin() {
        return Ruolo.Amministratore.equals(this.ruolo);
    }

    /**
	 * Verifica che il ruolo dell'utente sia quello di capo sala.
	 * @return true se l'utente e' capo sala
	 */
    public boolean isCaposala() {
        return Ruolo.Caposala.equals(this.ruolo);
    }

    /**
	 * Verifica che il ruolo dell'utente sia quello di infermiere.
	 * @return true se l'utente e' infermiere
	 */
    public boolean isInfermiere() {
        return Ruolo.Infermiere.equals(this.ruolo);
    }

    /**
	 * Verifica che l'utente sia un tecnico
	 * @return true se l'utente e' un tecnico
	 */
    public boolean isTecnico() {
        return Ruolo.Tecnico.equals(this.ruolo);
    }

    /**
	 * Calcola l'hash della password.
	 * @param password la password di cui calcolare l'hash
	 * @return l'hash della password
	 */
    public static String calculateHash(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            md.reset();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        md.update(password.getBytes());
        return byteToBase64(md.digest());
    }

    private static String byteToBase64(byte[] data) {
        BASE64Encoder endecoder = new BASE64Encoder();
        return endecoder.encode(data);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Utente other = (Utente) obj;
        if (id != other.id) {
            return false;
        }
        if (username == null && other.username != null) {
            return false;
        } else if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }
}
