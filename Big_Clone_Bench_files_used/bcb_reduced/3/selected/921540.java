package de.lerneffekt;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Date;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import com.webct.platform.sdk.security.authentication.module.AuthenticationModule;
import com.webct.platform.sdk.proxytool.common.ProcessCallback;
import com.webct.platform.sdk.context.gen.*;
import com.webct.platform.sdk.context.client.*;
import org.apache.log4j.Logger;
import java.security.MessageDigest;

public class PMWikiIntegrationModule extends AuthenticationModule {

    private static final org.apache.log4j.Logger sdk = org.apache.log4j.Logger.getLogger("VistaSDK");

    private static final String AUTOSIGNON_URL = "AUTOSIGNONURL";

    private static final String SECRET = "SECRET";

    private static final String INSTITUTIONNAME = "INSTITUTIONNAME";

    private static final String SECTIONID = "SECTIONID";

    private static final String SECTIONNAME = "SECTIONNAME";

    private static final String USERNAME = "USERNAME";

    private static final String READPASSWORD = "READPASSWORD";

    private static final String WRITEPASSWORD = "WRITEPASSWORD";

    private static final String UPLOADPASSWORD = "UPLOADPASSWORD";

    private static final String ATTRPASSWORD = "ATTRPASSWORD";

    private static final String WIKI_GROUP = "WIKIGROUP";

    private static final String STUDENT = "STUDENT";

    private static final String SKIN = "SKIN";

    public PMWikiIntegrationModule() {
        super(new Hashtable());
    }

    public PMWikiIntegrationModule(Hashtable componentHash) {
        super(componentHash);
    }

    public void initialize(Subject arg0, CallbackHandler arg1, Map arg2, Map arg3) {
        super.initialize(arg0, arg1, arg2, arg3);
    }

    public boolean abort() throws LoginException {
        return super.abort();
    }

    public boolean login() throws LoginException {
        return true;
    }

    public boolean logout() throws LoginException {
        return super.logout();
    }

    public boolean commit() throws LoginException {
        Map settings = super.getSettings();
        if (super.getCurrentMode().equals(AuthenticationModule.OUTGOING_MODE)) {
            String wikiGroup = (String) settings.get(WIKI_GROUP);
            String autoSignonUrl = (String) settings.get(AUTOSIGNON_URL);
            String secret = (String) settings.get(SECRET);
            String institutionName = (String) settings.get(INSTITUTIONNAME);
            String sectionName = (String) settings.get(SECTIONNAME);
            String userName = (String) settings.get(USERNAME);
            String readPassword = (String) settings.get(READPASSWORD);
            String writePassword = (String) settings.get(WRITEPASSWORD);
            String uploadPassword = (String) settings.get(UPLOADPASSWORD);
            String attrPassword = (String) settings.get(ATTRPASSWORD);
            String studentRight = (String) settings.get(STUDENT);
            String skin = (String) settings.get(SKIN);
            String sectionId = this.getCurrentLearningContextId().toString();
            if (autoSignonUrl == null || secret == null || autoSignonUrl.trim().length() == 0 || secret.trim().length() == 0) {
                throw new LoginException("A required setting is missing (AUTOSIGNONURL, SECRET)");
            }
            try {
                ContextSDK context = new ContextSDK();
                SessionVO session = context.getCurrentSession();
                Long lcid = super.getCurrentLearningContextId();
                LearningCtxtVO lc = context.getLearningContext(session, lcid.longValue());
                SubjectVO subject = session.getSubject();
                long[] lcids = context.getLearningContextIDs(session);
                String[] roles = context.getRoleIDs(session, lcids[0]);
                RoleVO role = context.getRoleDefinition(session, roles[0]);
                String roleName = role.getRoleDefinitionName();
                String password = "";
                if (roleName.equals("SDES") || roleName.equals("SINS")) {
                    password = uploadPassword;
                } else if (studentRight.equals("rw")) {
                    password = writePassword;
                } else if (studentRight.equals("rwu")) {
                    password = uploadPassword;
                } else {
                    password = readPassword;
                }
                String[] paramArray = { userName, roleName, sectionId, sectionName, skin, password, readPassword, writePassword, uploadPassword, attrPassword };
                String mac = calculateMac(paramArray, secret);
                Hashtable params = new Hashtable();
                params.put("username", userName);
                params.put("role", roleName);
                params.put("course_id", sectionId);
                params.put("course_name", sectionName);
                params.put("rp", readPassword);
                params.put("wp", writePassword);
                params.put("up", uploadPassword);
                params.put("ap", attrPassword);
                params.put("password", password);
                params.put("skin", skin);
                params.put("wiki_group", wikiGroup);
                params.put("MAC", mac);
                super.setUrlParameters(params);
                super.setRedirectUrl(autoSignonUrl);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return false;
        }
        return false;
    }

    private static String calculateMac(String[] values, String secret) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException nsae) {
            System.out.println("MD5 not found!\n");
        }
        int asciiValue = 0;
        int size = values.length;
        String paramString = "";
        for (int i = 0; i < size; i++) {
            paramString += values[i];
        }
        int strSize = paramString.length();
        for (int j = 0; j < strSize; j++) {
            asciiValue += paramString.charAt(j);
        }
        byte[] hashBytes = md.digest((asciiValue + secret + "").getBytes());
        md.reset();
        String mac = "";
        String hexByte;
        for (int k = 0; k < hashBytes.length; k++) {
            hexByte = Integer.toHexString(hashBytes[k] < 0 ? hashBytes[k] + 256 : hashBytes[k]);
            mac += (hexByte.length() == 1) ? "0" + hexByte : hexByte;
        }
        return mac;
    }
}
