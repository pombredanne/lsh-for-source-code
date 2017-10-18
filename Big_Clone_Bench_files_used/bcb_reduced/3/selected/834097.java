package commands.jsoncommands;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import constants.ComConstants;
import packaging.jsoncommands.PacLogin;
import helperobjects.DefinedException;
import helperobjects.StatusCodesV2;
import helperobjects.UserSession;
import interfaces.DoesNotRequireLogin;
import interfaces.JSONCommand;
import interfaces.PacLoginInterface;

/**
 * This command can be called without requiring the user to be logged in as it has the @DoesNotRequireLogin annotation
 * This handles requests when the user is trying to log in.
 * Session dependent data is restored in the PacLogin class that is called.
 * @author Matthew D. Cummings
 *
 */
@DoesNotRequireLogin
public class Login implements JSONCommand {

    private static Logger log = Logger.getLogger(Login.class);

    private PacLoginInterface pli;

    public Login() {
        pli = new PacLogin();
    }

    public Login(PacLoginInterface pacClass) {
        pli = pacClass;
    }

    /**
	 * Gets the user name and password from the client, looks up there account
	 * and stores the user information in the session
	 * 
	 * @author Matthew D. Cummings
	 * @throws JSONException: When a key can't be put into the return JSONObject (Never exspected to happen)
	 * @throws NoSuchAlgorithmException 
	 * @Date Oct 16, 2010
	 */
    @Override
    public JSONObject runCommand(JSONObject payload, HttpSession session) throws DefinedException {
        String sessionId = session.getId();
        log.debug("Login -> runCommand SID: " + sessionId);
        JSONObject toReturn = new JSONObject();
        boolean isOK = true;
        String username = null;
        try {
            username = payload.getString(ComConstants.LogIn.Request.USERNAME);
        } catch (JSONException e) {
            log.error("SessionId=" + sessionId + ", Missing username parameter", e);
            throw new DefinedException(StatusCodesV2.PARAMETER_ERROR);
        }
        String password = null;
        if (isOK) {
            try {
                password = payload.getString(ComConstants.LogIn.Request.PASSWORD);
            } catch (JSONException e) {
                log.error("SessionId=" + sessionId + ", Missing password parameter", e);
                throw new DefinedException(StatusCodesV2.PARAMETER_ERROR);
            }
        }
        if (isOK) {
            MessageDigest m = null;
            try {
                m = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                log.error("SessionId=" + sessionId + ", MD5 algorithm does not exist", e);
                e.printStackTrace();
                throw new DefinedException(StatusCodesV2.INTERNAL_SYSTEM_FAILURE);
            }
            m.update(password.getBytes(), 0, password.length());
            password = new BigInteger(1, m.digest()).toString(16);
            UserSession userSession = pli.login(username, password);
            try {
                if (userSession != null) {
                    session.setAttribute("user", userSession);
                    toReturn.put(ComConstants.Response.STATUS_CODE, StatusCodesV2.LOGIN_OK.getStatusCode());
                    toReturn.put(ComConstants.Response.STATUS_MESSAGE, StatusCodesV2.LOGIN_OK.getStatusMessage());
                } else {
                    log.error("SessionId=" + sessionId + ", Login failed: username=" + username + " not found");
                    toReturn.put(ComConstants.Response.STATUS_CODE, StatusCodesV2.LOGIN_USER_OR_PASSWORD_INCORRECT.getStatusCode());
                    toReturn.put(ComConstants.Response.STATUS_MESSAGE, StatusCodesV2.LOGIN_USER_OR_PASSWORD_INCORRECT.getStatusMessage());
                }
            } catch (JSONException e) {
                log.error("SessionId=" + sessionId + ", JSON exception occured in response", e);
                e.printStackTrace();
                throw new DefinedException(StatusCodesV2.INTERNAL_SYSTEM_FAILURE);
            }
        }
        log.debug("Login <- runCommand SID: " + sessionId);
        return toReturn;
    }
}
