package edu.univalle.lingweb.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import com.csvreader.CsvReader;
import edu.univalle.lingweb.Common;
import edu.univalle.lingweb.persistence.CoCourse;
import edu.univalle.lingweb.persistence.CoCourseDAO;
import edu.univalle.lingweb.persistence.CoCourseUser;
import edu.univalle.lingweb.persistence.CoUserExer2Group;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaRoleDAO;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.MaUserDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'ma_user'( Usuarios )
 * 
 * @author Jose Aricapa
 */
public class DataManagerUser extends DataManager {

    /**
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerUser.class);

    /**
	 * Constructor de la clase
	 */
    public DataManagerUser() {
        super();
        DOMConfigurator.configure(DataManagerUser.class.getResource("/log4j.xml"));
    }

    /**
	 * Manejar una petici�n de acceso de usuario.
	 * 
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult}
	 * 
	 * @param session
	 *            La {@link session} de usuario
	 * @param result
	 *            El {@link RestServiceResult} que contendrán los mensajes
	 *            localizados y estado SQL .
	 * @param
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operación.
	 */
    public RestServiceResult login(HttpSession session, RestServiceResult result, String sLogin, String sPassword) {
        MaUserDAO maUserDAO = null;
        MaUser maUser = null;
        Long nLogin = null;
        try {
            nLogin = Long.parseLong(sLogin);
        } catch (NumberFormatException e) {
            Object[] args = { sLogin };
            result.setMessage(MessageFormat.format(bundle.getString("user.login.unknownUserName"), args));
            result.setError(true);
            log.error("Login es incorrecto:'" + sLogin + "'. Debe ser num�rico: ");
            return result;
        }
        if (nLogin != null && nLogin.longValue() > 0) {
            maUserDAO = new MaUserDAO();
            List<MaUser> list = maUserDAO.findByIdentification(nLogin);
            if (list.size() == 0) {
                list = maUserDAO.findByCodStudent(nLogin);
                if (list.size() == 0) {
                    if (session != null) session.removeAttribute("user");
                    result.setError(true);
                    Object[] args = { nLogin };
                    result.setMessage(MessageFormat.format(bundle.getString("user.login.unknownUserName"), args));
                }
            }
            if (list.size() > 0) maUser = list.get(0);
            if (maUser != null) if (this.authorize(sPassword, maUser)) {
                Date dtLastLogin = maUser.getLastLogin();
                if (session != null) session.setAttribute("user", maUser);
                result.setError(false);
                result.setMessage(bundle.getString("user.login.success"));
                log.debug("Usuario guardado en la sesion");
                maUser.setLastLogin(new Date());
                update(new RestServiceResult(), maUser);
                maUser.setLastLogin(dtLastLogin);
                result.setObjResult(maUser);
                session.setAttribute("user", maUser);
            } else {
                log.warn("Contrase;a incorrecta para el usuario: " + maUser.getUserName());
                session.setAttribute("user", null);
                result.setError(true);
                result.setMessage(bundle.getString("user.login.wrongPassword"));
            }
        } else {
            result.setMessage(bundle.getString("user.login.missingData"));
            result.setError(true);
            log.error("Missing parameters! Called User.login() without specifiying the login and/or pass!");
        }
        return result;
    }

    /**
	 * Crea un nuevo usuario en la base de datos
	 * 
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult}
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, final MaUser maUser) {
        MaUserDAO maUserDAO = null;
        log.debug("Ejecutando el comando DataManagerUser.create()");
        maUserDAO = new MaUserDAO();
        maUser.setUserId(getSequence("sq_ma_user"));
        if (maUser.getPassword() == null || maUser.getPassword().trim().equals("")) if (maUser.getMaRole().getRoleId() == 2) maUser.setPassword(maUser.getCodStudent().toString()); else if (maUser.getMaRole().getRoleId() == 3) maUser.setPassword(maUser.getIdentification().toString());
        try {
            EntityManagerHelper.beginTransaction();
            maUserDAO.save(maUser);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maUser);
            Object[] args = { maUser.getUserName() };
            result.setMessage(MessageFormat.format(bundle.getString("user.create.success"), args));
        } catch (PersistenceException e) {
            if (e.getMessage().indexOf("idx_cod_student") > 0) {
                Object[] args = { maUser.getCodStudent().toString() };
                result.setError(true);
                result.setMessage(MessageFormat.format(bundle.getString("user.create.userAlreadyExistsCodStudent"), args));
            } else if (e.getMessage().indexOf("idx_identification") > 0) {
                Object[] args = { maUser.getIdentification() };
                result.setError(true);
                result.setMessage(MessageFormat.format(bundle.getString("user.create.userAlreadyExistsIdentification"), args));
            } else {
                result.setError(true);
                result.setMessage(e.getMessage());
            }
            EntityManagerHelper.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * Actualiza los datos de los usuarios *
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult}
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param maUser
	 *            usuario a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult result, final MaUser maUser) {
        MaUserDAO maUserDAO = null;
        log.debug("Executing command User.update()");
        maUserDAO = new MaUserDAO();
        try {
            EntityManagerHelper.beginTransaction();
            maUserDAO.update(maUser);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maUser);
            Object[] args = { maUser.getUserName() };
            result.setMessage(MessageFormat.format(bundle.getString("user.update.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            if (e.getMessage().indexOf("idx_cod_student") > 0) {
                Object[] args = { maUser.getCodStudent().toString() };
                result.setMessage(MessageFormat.format(bundle.getString("user.create.userAlreadyExistsCodStudent"), args));
            } else if (e.getMessage().indexOf("idx_identification") > 0) {
                Object[] args = { maUser.getIdentification() };
                result.setMessage(MessageFormat.format(bundle.getString("user.create.userAlreadyExistsIdentification"), args));
            }
            log.error("Error al actualizar el usuario: " + e.getMessage());
        }
        return result;
    }

    /**
	 * Elimina un usuario por su nombre de usuario ( n�mero de identificaci�n)
	 * 
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult}
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nUserId
	 *            c�digo de usuario
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, MaUser maUser) {
        MaUserDAO maUserDAO = new MaUserDAO();
        try {
            log.info("Eliminando el usuario: " + maUser.getUserName());
            EntityManagerHelper.beginTransaction();
            maUserDAO.delete(maUser);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maUser);
            Object[] arrayParam = { maUser.getUserName() };
            log.info("Usuario eliminado con �xito: " + maUser.getUserName());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("user.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la secuencia: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { maUser.getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("user.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Elimina un usuario por su nombre de usuario ( n�mero de identificaci�n)
	 * 
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult}
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nUserId
	 *            c�digo de usuario
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult deleteMasive(RestServiceResult serviceResult, String sArrayUserId) {
        try {
            String sSql = Statements.DELETE_MASIVE_USER;
            sSql = sSql.replaceFirst("v1", sArrayUserId);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(sSql);
            int nDeleted = query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { nDeleted };
            log.info(" # Usuarios eliminados => " + nDeleted);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("user.deleteMasive.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la secuencia: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("user.delete.error") + e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un usuario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nUserId
	 *            C�digo de la actividad
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nUserId) {
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        if (maUser == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("user.search.notFound"));
        } else {
            List<MaUser> list = new ArrayList<MaUser>();
            list.add(maUser);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("user.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un usuario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nIdentification
	 *            N�mero de identificaci�n
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult searchIdentification(RestServiceResult serviceResult, Long nIdentification) {
        List<MaUser> list = new MaUserDAO().findByIdentification(nIdentification);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("user.search.notFound"));
        } else {
            MaUser maUser = list.get(0);
            if (maUser.getMaRole().getRoleId() == Common.ROLE_ID_ADMIN) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("user.search.notFound"));
            } else {
                Object[] arrayParam = { list.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.search.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            }
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un usuario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nCodStudent
	 *            N�mero de identificaci�n
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult searchCodStudent(RestServiceResult serviceResult, Long nCodStudent) {
        List<MaUser> list = new MaUserDAO().findByCodStudent(nCodStudent);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("user.search.notFound"));
        } else {
            MaUser maUser = list.get(0);
            if (maUser.getMaRole().getRoleId() == Common.ROLE_ID_ADMIN) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("user.search.notFound"));
            } else {
                Object[] arrayParam = { list.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.search.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            }
        }
        return serviceResult;
    }

    /**
	 * M�todo que permite la actualización de la foto de un usuario. Solo es
	 * utilizada en la p�gina principal.
	 * 
	 * @param nUserId
	 *            c�digo del usuario
	 * @param sPhotoFile
	 *            nombre del archivo
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult updatePhoto(RestServiceResult result, Long nUserId, String sPhotoFile) {
        if (nUserId == null || nUserId.longValue() == 0 || sPhotoFile == null || sPhotoFile.trim().equals("")) {
            log.error("ERROR: No se puede actualizar la foto. Los parámetros presentar error: \n" + "UserId = " + nUserId + ", Archivo-foto = " + sPhotoFile);
            Object[] args = { nUserId, sPhotoFile };
            result.setMessage(MessageFormat.format(bundle.getString("user.update.errorPhoto"), args));
            return result;
        } else {
            MaUserDAO maUserDAO = new MaUserDAO();
            MaUser maUser = maUserDAO.findById(nUserId);
            maUser.setPhotoFile(sPhotoFile);
            return update(result, maUser);
        }
    }

    /**
	 * M�todo que permite guardar usuarios a partir de un archivos CSV
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param archivo
	 *            a importar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult createUserMasive(RestServiceResult result, File fileImport) {
        Vector<String> vecError = new Vector<String>();
        Vector<String> vecSuccess = new Vector<String>();
        Vector<String> vecWarning = new Vector<String>();
        CsvReader cvsReader = null;
        FileReader freader = null;
        try {
            freader = new FileReader(fileImport);
            cvsReader = new CsvReader(freader, ";".charAt(0));
            String[] headers = null;
            int nCount = 1;
            if (cvsReader.readHeaders()) {
                headers = cvsReader.getHeaders();
            }
            while (cvsReader.readRecord()) {
                int nColumnCount = cvsReader.getColumnCount();
                if (nColumnCount != 4) {
                    Object[] args = { 1, cvsReader.getValues()[0] };
                    vecError.add(MessageFormat.format(bundle.getString("user.uploadFile.errorParameter"), args));
                    result.setObjResult(vecError);
                    result.setError(true);
                    return result;
                }
                String sResult = validateLine(cvsReader, headers, nCount);
                if (sResult != null) {
                    vecError.add((nCount - 1), sResult);
                    result.setError(true);
                } else {
                    vecSuccess.add(cvsReader.getRawRecord());
                    vecError.add((nCount - 1), bundle.getString("user.uploadFile.validateSuccess"));
                }
                nCount++;
            }
            if (cvsReader != null) {
                cvsReader.close();
            }
            if (result.isError()) {
                result.setObjResult(vecError);
                log.info("Registros error: " + vecError.size());
                result.setMessage(bundle.getString("user.update.failureUpload"));
            } else {
                freader = new FileReader(new File(fileImport.getAbsoluteFile().toString()));
                cvsReader = new CsvReader(freader, ";".charAt(0));
                nCount = 0;
                if (cvsReader.readHeaders()) {
                    headers = cvsReader.getHeaders();
                }
                while (cvsReader.readRecord()) {
                    Long nIdentificationOrCodStudent = new Long(cvsReader.get(headers[0]));
                    Long nRoleId = new Long(cvsReader.get(headers[1]));
                    String sUserName = cvsReader.get(headers[2]);
                    String sMail = cvsReader.get(headers[3]);
                    MaUser maUser = new MaUser();
                    if (nRoleId.equals(Common.ROLE_ID_TEACHER)) {
                        if (new MaUserDAO().findByIdentification(nIdentificationOrCodStudent).size() == 0) {
                            maUser.setIdentification(nIdentificationOrCodStudent);
                            maUser.setPassword(nIdentificationOrCodStudent.toString());
                        } else {
                            Object[] args = { (nCount + 1), nIdentificationOrCodStudent };
                            vecWarning.add(nCount, MessageFormat.format(bundle.getString("user.uploadFile.warningIdentification"), args));
                            nCount++;
                            continue;
                        }
                    }
                    if (nRoleId.equals(Common.ROLE_ID_STUDENT)) {
                        if (new MaUserDAO().findByCodStudent(nIdentificationOrCodStudent).size() == 0) {
                            maUser.setCodStudent(nIdentificationOrCodStudent);
                            maUser.setPassword(nIdentificationOrCodStudent.toString());
                        } else {
                            Object[] args = { (nCount + 1), nIdentificationOrCodStudent };
                            vecWarning.add(nCount, MessageFormat.format(bundle.getString("user.uploadFile.warningCodStudent"), args));
                            nCount++;
                            continue;
                        }
                    }
                    vecWarning.add(nCount, bundle.getString("user.uploadFile.success"));
                    maUser.setUserName(sUserName.replaceAll("\"", ""));
                    maUser.setMaRole(new MaRoleDAO().findById(nRoleId));
                    maUser.setEmail(sMail.replaceAll("\"", ""));
                    result = create(result, maUser);
                    nCount++;
                }
                result.setObjResult(vecWarning);
                log.info("Usuarios Exitentes: " + vecWarning.size());
                Object[] args = { nCount };
                result.setMessage(MessageFormat.format(bundle.getString("user.update.successUpload"), args));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (cvsReader != null) {
                cvsReader.close();
            }
            cvsReader.close();
        } finally {
            if (cvsReader != null) {
                cvsReader.close();
            }
        }
        return result;
    }

    /**
	 * Realiza la validaci�n de una l�nea proveniente de un archivo CSV con datos de usuarios. Par�metros: Identificaci�n,C�digo,Nombre,Role (Si no tiene c�digo � Identificaci�n se agrega cero ( 0 ))
	 * 
	 * @param sLine
	 *            l�nea con 4 p�ramtros separados por comas
	 * @return {@link String } con el mensaje de error � null si no encontr� errores
	 * @throws IOException
	 */
    private String validateLine(CsvReader cvsReader, String[] headers, int nNumLine) throws IOException {
        Long nIdentifOrCodStudent = null;
        String sIdentifOrCodStudent = null;
        String sName = null;
        String sRoleId = null;
        Long nRoleId = null;
        String sLine = cvsReader.getRawRecord();
        Object[] args = { nNumLine, sLine };
        sIdentifOrCodStudent = cvsReader.get(headers[0]);
        if (sIdentifOrCodStudent == null || sIdentifOrCodStudent.trim().equals("")) return MessageFormat.format(bundle.getString("user.uploadFile.errorCod_Ident"), args);
        if (sIdentifOrCodStudent.trim().length() < 5) return MessageFormat.format(bundle.getString("user.uploadFile.errorCod_Ident"), args);
        try {
            nIdentifOrCodStudent = Long.parseLong(sIdentifOrCodStudent);
            if (sIdentifOrCodStudent.trim().length() < 5 || nIdentifOrCodStudent < 0) return MessageFormat.format(bundle.getString("user.uploadFile.errorCod_Ident"), args);
        } catch (NumberFormatException e) {
            return MessageFormat.format(bundle.getString("user.uploadFile.errorCod_Ident"), args);
        }
        sRoleId = cvsReader.get(headers[1]);
        if (sRoleId == null || sRoleId.trim().equals("") || sRoleId.trim().length() > 1) return MessageFormat.format(bundle.getString("user.uploadFile.errorRole"), args);
        try {
            nRoleId = Long.parseLong(sRoleId);
            if (nRoleId.equals(Common.ROLE_ID_STUDENT) || nRoleId.equals(Common.ROLE_ID_TEACHER)) {
            } else return MessageFormat.format(bundle.getString("user.uploadFile.errorRole"), args);
        } catch (NumberFormatException e) {
            return MessageFormat.format(bundle.getString("user.uploadFile.errorRole"), args);
        }
        sName = cvsReader.get(headers[2]);
        if (sName == null || sName.trim().equals("") || sName.trim().length() <= 4) return MessageFormat.format(bundle.getString("user.uploadFile.errorName"), args);
        String sMail = cvsReader.get(headers[3]);
        System.out.println("sMail => " + sMail);
        if (sMail == null || sMail.trim().equals("")) return MessageFormat.format(bundle.getString("user.uploadFile.errorMail"), args);
        return null;
    }

    /**
	 * Manejar una petici�n de salir.
	 * 
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult}
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 * 
	 */
    public RestServiceResult logout(HttpSession session, RestServiceResult serviceResult, MaUser maUser) {
        log.info("Starting logout sequence");
        session.removeAttribute("user");
        session.invalidate();
        if (maUser != null) {
            Object[] args = { maUser.getIdentification() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("user.logout.success"), args));
            log.info("Usuario " + maUser.getUserName() + " sali� de la aplicaci�n!");
        } else {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("user.logout.notLoggedIn"));
            log.info("Attempted to logout when not logged in");
        }
        return serviceResult;
    }

    /**
	 * Compare the MD5 hash passed to this method with the user's password hash.
	 * Returns true if they match, false if they don't match.
	 * 
	 * @param checksum
	 *            The MD5 hash of the entered password.
	 * @return true, if authorized; false, if not authorized
	 */
    private boolean authorize(String checksum, MaUser maUser) {
        log.debug("Intentando autorizar al usuario: " + maUser.getUserName());
        log.debug("Checksum en base de datos: " + checksum);
        log.debug("Checksum for entered password: " + maUser.getPassword());
        if (checksum.equals(maUser.getPassword())) {
            log.debug("Checksums match, user authorized");
            return true;
        } else {
            log.debug("Checksums do not match, wrong password");
            return false;
        }
    }

    /**
	 * Generate an MD5 hash for the string passed to the method.
	 * 
	 * @param pass
	 *            The string for which we need to generate the hash.
	 * @return The MD5 hash.
	 */
    private String md5(String pass) {
        StringBuffer enteredChecksum = new StringBuffer();
        byte[] digest;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(pass.getBytes(), 0, pass.length());
            digest = md5.digest();
            for (int i = 0; i < digest.length; i++) {
                enteredChecksum.append(toHexString(digest[i]));
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not create MD5 hash!");
            log.error(e.getLocalizedMessage());
            log.error(e.getStackTrace());
        }
        return enteredChecksum.toString();
    }

    /**
	 * Convert a byte into a hexadezimal string representation.
	 * 
	 * @param b
	 *            The byte to be converted.
	 * @return The hex String.
	 */
    private String toHexString(byte b) {
        int value = (b & 0x7F) + (b < 0 ? 128 : 0);
        String ret = (value < 16 ? "0" : "");
        ret += Integer.toHexString(value).toUpperCase();
        return ret;
    }

    /**
	 * Obtiene la lista de usuarios
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de usuarios
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la
	 *            consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result, int nRowStart, int nMaxResults) {
        MaUserDAO maUserDAO = new MaUserDAO();
        List<MaUser> list = maUserDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("user.list.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("user.list.success"), arrayParam));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(maUserDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Obtiene la lista de estudiantes por curso
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la
	 *            consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listStudentForCourse(RestServiceResult serviceResult, Long nCourseId) {
        CoCourse coCourse = new CoCourseDAO().findById(nCourseId);
        EntityManagerHelper.refresh(coCourse);
        if (coCourse == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("course.search.notFound"));
        } else {
            List<MaUser> list = new ArrayList<MaUser>();
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_STUDENT_COURSE, MaUser.class);
            query.setParameter(1, nCourseId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            list = query.getResultList();
            if (list.size() == 0) {
                Object[] arrayParam = { coCourse.getCourseName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.listStudentForCourse.notFound"), arrayParam));
            } else {
                Object[] arrayParam = { list.size(), coCourse.getCourseName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.listStudentForCourse.success"), arrayParam));
            }
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de estudiantes que no han sido registrados a un
	 * determinado curso
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listStudentNotInCourse(RestServiceResult result, Long nCourseId) {
        return listStudentNotInCourse(result, nCourseId, 0, 0);
    }

    /**
	 * Obtiene la lista de estudiantes que no han sido registrados a un
	 * determinado curso
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la
	 *            consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult listStudentNotInCourse(RestServiceResult serviceResult, Long nCourseId, int nRowStart, int nMaxResults) {
        CoCourse coCourse = new CoCourseDAO().findById(nCourseId);
        EntityManagerHelper.refresh(coCourse);
        if (coCourse == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("course.search.notFound"));
        } else {
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_STUDENT_NOT_IN_COURSE, MaUser.class);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            query.setParameter(1, Common.ROLE_ID_STUDENT);
            query.setParameter(2, nCourseId);
            if (nRowStart > 0) query.setFirstResult(nRowStart);
            if (nMaxResults > 0) query.setMaxResults(nMaxResults);
            List<MaUser> list = query.getResultList();
            if (list.size() == 0) {
                Object[] arrayParam = { coCourse.getCourseName() };
                serviceResult.setNumResult(list.size());
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.listStudentNotInCourse.notFound"), arrayParam));
            } else {
                Object[] arrayParam = { list.size(), coCourse.getCourseName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.listStudentNotInCourse.success"), arrayParam));
                if ((nRowStart > 0) || (nMaxResults > 0)) {
                    RestServiceResult serviceResult2 = listStudentNotInCourse(new RestServiceResult(), nCourseId);
                    int nNumStudent = serviceResult2.getNumResult();
                    serviceResult.setNumResult(nNumStudent);
                } else serviceResult.setNumResult(list.size());
            }
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de profesores
	 * 
	 * @param sLine
	 *            l�nea con 4 p�rametros separados por comas
	 * @return {@link String } con el mensaje de error � null si no encontr�
	 *         errores
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult getUserForRole(RestServiceResult result, Long nRoleId) {
        List<MaUser> list = null;
        Query query = EntityManagerHelper.createQuery(Statements.SELECT_USER_FOR_ROLE);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        query.setParameter("roleId", nRoleId);
        list = query.getResultList();
        if (list.size() == 0) {
            result.setError(true);
            if (nRoleId.equals(Common.ROLE_ID_TEACHER)) result.setMessage(bundle.getString("user.listTeacher.notFound")); else if (nRoleId.equals(Common.ROLE_ID_STUDENT)) result.setMessage(bundle.getString("user.listStudent.notFound"));
        } else {
            Object[] array = { list.size() };
            if (nRoleId.equals(Common.ROLE_ID_TEACHER)) result.setMessage(MessageFormat.format(bundle.getString("user.listTeacher.success"), array)); else if (nRoleId.equals(Common.ROLE_ID_STUDENT)) result.setMessage(MessageFormat.format(bundle.getString("user.listStudent.success"), array));
            result.setObjResult(list);
        }
        return result;
    }

    /**
	 * Actualiza la contrase�a de usuario
	 * 
	 * @param sLine
	 *            l�nea con 4 p�rametros separados por comas
	 * @return {@link String } con el mensaje de error � null si no encontr�
	 *         errores
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult changePassword(RestServiceResult serviceResult, Long nUserId, String sOldPassword, String sNewPassword) {
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        if (maUser == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("user.search.notFound"));
        } else {
            if (maUser.getMaRole().getRoleId().equals(Common.ROLE_ID_ADMIN)) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("security.accessDenied"));
            } else if (maUser.getPassword().trim().equals(sOldPassword.trim())) {
                maUser.setPassword(sNewPassword);
                update(new RestServiceResult(), maUser);
                serviceResult.setMessage(bundle.getString("user.changePassword.success"));
            } else {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("user.changePassword.errorOldPassword"));
            }
        }
        return serviceResult;
    }

    /**
	 * Actualiza el tema (entorno) del usuario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 *            
	 * @param nUserId
	 *            C�digo del usuario.           
	 *            
	 * @param sThemeName
	 *            Tema a actualizar para el usuario.
	 *            	 
	 * @return {@link String } con el mensaje de error � null si no encontr�
	 *         errores
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult changeTheme(RestServiceResult serviceResult, Long nUserId, String sThemeName) {
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        if (maUser == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("user.search.notFound"));
        } else {
            maUser.setThemeName(sThemeName);
            update(new RestServiceResult(), maUser);
            serviceResult.setMessage(bundle.getString("user.changeTheme.success"));
        }
        return serviceResult;
    }

    /**
	 * Actualiza la contrase�a de usuario
	 * 
	 * @param sLine l�nea con 4 p�rametros separados por comas
	 * @return {@link String } con el mensaje de error � null si no encontr�
	 *         errores
	 */
    @SuppressWarnings("unchecked")
    public static void updateLanguageCode(Long nUserId, String sLanguageCode) {
        try {
            EntityManagerHelper.beginTransaction();
            MaUser maUser = new MaUserDAO().findById(nUserId);
            maUser.setLanguageCode(sLanguageCode);
            new MaUserDAO().update(maUser);
            EntityManagerHelper.commit();
        } catch (Exception e) {
            e.printStackTrace();
            EntityManagerHelper.rollback();
        }
    }

    public static void main(String[] args) {
        try {
            RestServiceResult serviceResult = new RestServiceResult();
            DataManagerUser dataManagerUser = new DataManagerUser();
            dataManagerUser.setBundle(ResourceBundle.getBundle("edu.univalle.lingweb.LzTrackMessages", new Locale("es")));
            dataManagerUser.listStudentForCourse(serviceResult, new Long(188));
            System.out.println("result => " + serviceResult.getMessage());
            if (false) return;
            long ini = System.currentTimeMillis();
            File fileImport = new File("D:/Importacion_usuarios.csv");
            dataManagerUser.createUserMasive(serviceResult, fileImport);
            System.out.println("result => " + serviceResult.getMessage());
            System.out.println("objresult => " + serviceResult.getObjResult());
            System.out.println("TIEMPO EMPLEADO => " + (System.currentTimeMillis() - ini) + "ms");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
