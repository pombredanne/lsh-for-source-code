package com.excilys.sugadroid.tasks;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.util.Log;
import com.excilys.sugadroid.activities.interfaces.IAuthenticatingActivity;
import com.excilys.sugadroid.beans.interfaces.ISessionBean;
import com.excilys.sugadroid.services.exceptions.InvalidResponseException;
import com.excilys.sugadroid.services.exceptions.LoginFailedException;
import com.excilys.sugadroid.services.exceptions.ServiceException;
import com.excilys.sugadroid.services.interfaces.ILoginServices;

public class LoginInTask extends LoadingTask<IAuthenticatingActivity> {

    private static final String TAG = LoginInTask.class.getSimpleName();

    private final String username;

    private final String password;

    private final ILoginServices loginServices;

    private final ISessionBean sessionBean;

    public LoginInTask(IAuthenticatingActivity activity, String username, String password, ILoginServices loginServices, ISessionBean sessionBean) {
        super(activity);
        this.username = username;
        this.password = password;
        this.activity = activity;
        this.loginServices = loginServices;
        this.sessionBean = sessionBean;
    }

    @Override
    public void doRunLoadingTask() {
        String sessionId;
        String userId;
        String version;
        try {
            sessionId = loginServices.login(username, password);
            userId = loginServices.getUserId(sessionId);
            version = loginServices.getServerVersion();
            sessionBean.setLoggedIn(sessionId, userId, username, loginServices.getEntryPoint(), version);
            activity.onLoginSuccessful();
        } catch (LoginFailedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            sessionBean.logout();
            activity.onLoginFailedBadCredentials();
        } catch (InvalidResponseException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            sessionBean.logout();
            activity.onLoginFailedNoNetwork();
        } catch (ServiceException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            sessionBean.logout();
            activity.onLoginFailed(e.getMessage());
        }
    }

    /**
	 * This method could be helpful for some SugarCRM setups that use MD5
	 * passwords
	 * 
	 * @param value
	 * @return
	 */
    @SuppressWarnings("unused")
    private String getMD5(String value) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        md5.reset();
        md5.update(value.getBytes());
        byte[] messageDigest = md5.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        }
        String hashedPassword = hexString.toString();
        return hashedPassword;
    }
}
