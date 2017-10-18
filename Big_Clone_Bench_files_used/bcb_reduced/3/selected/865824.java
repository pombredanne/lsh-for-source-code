package it.unipg.bipod.applicationLogic;

import java.sql.Timestamp;
import it.unipg.bipod.dataAccess.*;
import it.unipg.bipod.dataModel.*;
import java.security.NoSuchAlgorithmException;

/**
 * Logger fornisce un metodo statico per registrare le operazioni eseguite su BiPoD.<br>
 * 
 * @author Lorenzo Porzi
 *
 */
abstract class Logger {

    /**
	 * Aggiunge un'operazione alla base di dati.
	 * 
	 * @param dm il {@link DataManager} da utilizzare per l'accesso alla base di dati.
	 * @param tipo il tipo di operazione da registrare (elencati come campi statici di {@link Operazione})
	 * @param utente l'utente che esegue l'operazione
	 * @param descrizione una breve descrizione dell'operazione
	 * @throws DataAccessException
	 * @throws NoSuchAlgorithmException
	 */
    public static Operazione log(DataManager dm, String tipo, Registrazione utente, String descrizione) throws DataAccessException, NoSuchAlgorithmException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Operazione operazione = new Operazione();
        operazione.setUtente(utente.getIdRegistrazione());
        operazione.setTipo(tipo);
        operazione.setDataEsecuzione(now);
        operazione.setDescrizione(descrizione);
        dm.insertEntity(operazione);
        String hash = operazione.getIdOperazione() + tipo + descrizione + utente.getUtente() + now.toString();
        hash = AuthManager.digest(hash);
        operazione.setHash(hash);
        dm.updateEntity(operazione);
        return operazione;
    }
}
