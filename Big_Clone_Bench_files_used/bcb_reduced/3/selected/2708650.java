package org.cofax.cms;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.cofax.CofaxPage;
import org.cofax.DataStore;
import org.cofax.cms.login.ILoginHandler;

/**
* CofaxToolsLogin:
* Verifies user information and populates the user (session) values if verification is true. Login is called
* upon user login and will be fulfilled only once per session.
* @author Charles Harvey
* @author Lee Bolding
* @author Nicolas Richeton - Smile
**/
public class CofaxToolsLogin {

    private static ILoginHandler loginHandler = null;

    /**
	 * Verify user information and populate the user (session) values if
	 * verification is true. Login is called upon user login and will be
	 * fulfilled only once per session.
	 */
    public static String login(DataStore db, HttpServletRequest req, HttpSession session, HashMap userInfoHash, HttpServletResponse res, CofaxPage page) {
        return login(db, req, session, userInfoHash, res, page, true);
    }

    /**
	 * Verify user information and populate the user (session) values if
	 * verification is true. Login is called upon user login and will be
	 * fulfilled only once per session.
	 */
    public static String login(DataStore db, HttpServletRequest req, HttpSession session, HashMap userInfoHash, HttpServletResponse res, CofaxPage page, boolean doRedirect) {
        String tag;
        if (userInfoHash.size() >= 1) {
            CofaxToolsUser user = new CofaxToolsUser();
            session.setAttribute("user", user);
            user.userInfoHash = userInfoHash;
            session.setMaxInactiveInterval(3600);
            HashMap userPubDescPubIDHash = new HashMap();
            HashMap userPubNamePubIDHash = new HashMap();
            HashMap userGroupNameGroupIDHash = new HashMap();
            HashMap userPubNamePubDescHash = new HashMap();
            HashMap userPreviousArticlesHash = new HashMap();
            userPreviousArticlesHash.put("Previous Edits", "");
            HashMap fillReqTwo = new HashMap();
            fillReqTwo.put("USERID", (String) userInfoHash.get("USERID"));
            tag = CofaxToolsDbUtils.fillTag(db, "getVectorOfGroupsByUserID");
            Vector userGroupVector = CofaxToolsDbUtils.getPackageVector(db, fillReqTwo, tag);
            tag = CofaxToolsDbUtils.fillTag(db, "getVectorOfGroupTypesByUserID");
            Vector userGroupTypeVector = CofaxToolsDbUtils.getPackageVector(db, fillReqTwo, tag);
            user.userGroupVector = userGroupVector;
            user.userGroupTypeVector = userGroupTypeVector;
            Iterator keys = userGroupVector.iterator();
            Vector userPubsVectorOHash = new Vector();
            while (keys.hasNext()) {
                String groupIDFromUserToGroup = (String) keys.next();
                if (groupIDFromUserToGroup != null && !groupIDFromUserToGroup.equals("")) {
                    HashMap fillReqThree = new HashMap();
                    fillReqThree.put("pubNameKey", groupIDFromUserToGroup);
                    tag = CofaxToolsDbUtils.fillTag(db, "getAllFromPermGroupsByGroupID");
                    HashMap se = CofaxToolsDbUtils.getNameValuePackageHash(db, fillReqThree, tag);
                    String groupType = (String) se.get("GROUPTYPE");
                    String groupName = (String) se.get("GROUPNAME");
                    String groupID = (String) se.get("GROUPID");
                    userGroupNameGroupIDHash.put(groupID, groupName);
                    HashMap fillReqFour = new HashMap();
                    fillReqFour.put("pubNameKey", groupIDFromUserToGroup);
                    tag = CofaxToolsDbUtils.fillTag(db, "getAllFromGroupToPubByGroupID");
                    ArrayList s = (ArrayList) CofaxToolsDbUtils.getPackageData(db, fillReqFour, tag);
                    Iterator pubKeys = s.iterator();
                    while (pubKeys.hasNext()) {
                        HashMap groupIDpubID = (HashMap) pubKeys.next();
                        String pubID = (String) groupIDpubID.get("PUBID");
                        String pubName = "";
                        String pubDesc = "";
                        HashMap ht = new HashMap();
                        ht.put(pubID, groupType);
                        userPubsVectorOHash.add(ht);
                        if (!userPubDescPubIDHash.containsKey(pubID)) {
                            HashMap htt = CofaxToolsUtil.getPubNamePubDescFromID(db, pubID);
                            pubName = (String) htt.get("PUBNAME");
                            pubDesc = (String) htt.get("PUBDESC");
                            userPubDescPubIDHash.put(pubDesc, pubID);
                            if (!userPubNamePubDescHash.containsKey(pubDesc)) {
                                userPubNamePubDescHash.put(pubDesc, pubName);
                            }
                        }
                        if (!userPubNamePubIDHash.containsKey(pubID)) {
                            userPubNamePubIDHash.put(pubName, pubID);
                        }
                    }
                }
            }
            user.userPubsVectorOHash = userPubsVectorOHash;
            user.userPubDescPubIDHash = userPubDescPubIDHash;
            user.userPubNamePubIDHash = userPubNamePubIDHash;
            user.userPubNamePubDescHash = userPubNamePubDescHash;
            user.userPreviousArticlesHash = userPreviousArticlesHash;
            user.userGroupNameGroupIDHash = userGroupNameGroupIDHash;
            user.workingPub = (String) user.userInfoHash.get("HOMEPUB");
            String role = (String) user.userInfoHash.get("ROLE");
            String userFirstName = (String) user.userInfoHash.get("FIRSTNAME");
            String userLastName = (String) user.userInfoHash.get("LASTNAME");
            user.setUserWorkingPubConfigElements(db, session, user.workingPub);
            user.workingPubDesc = CofaxToolsUtil.getPubDescFromID(db, user.workingPub);
            user.workingPubName = CofaxToolsUtil.getPubNameFromID(db, user.workingPub);
            String p = CofaxToolsUtil.getPublicationSelectID(db, session, (String) userInfoHash.get("HOMEPUB"), "PUBLICATION");
            HashMap ht = new HashMap();
            ht.put("userRole", role);
            ht.put("userFirstName", userFirstName);
            ht.put("userLastName", userLastName);
            ht.put("selectPublication", p);
            String querystring = req.getParameter("querystring");
            boolean bOK = false;
            if (doRedirect) {
                if (!("".equals(querystring)) && querystring != null) {
                    try {
                        String urlRedirectTo = "" + CofaxToolsServlet.aliasPath + "/tools/?" + querystring;
                        if (!(urlRedirectTo.startsWith("/"))) urlRedirectTo = "/" + urlRedirectTo;
                        res.sendRedirect(urlRedirectTo);
                        return "";
                    } catch (IOException e) {
                        System.out.println("CofaxToolsNavigation : error while redirect");
                    }
                } else {
                    page.putGlossaryValue("system:message", CofaxToolsUtil.getI18NMessage(req.getLocale(), "welcomeMessage") + " " + CofaxToolsUtil.getI18NMessage(req.getLocale(), "pleaseMakeSelection"));
                    try {
                        String urlRedirectTo = "" + CofaxToolsServlet.aliasPath + "/tools/?mode=cofax_navigation";
                        if (!(urlRedirectTo.startsWith("/"))) urlRedirectTo = "/" + urlRedirectTo;
                        res.sendRedirect(urlRedirectTo);
                        return "";
                    } catch (IOException e) {
                        System.out.println("CofaxToolsNavigation : error while redirect to cofax_navigation mode");
                    }
                    return ("");
                }
            }
            return ("");
        } else {
            page.putGlossaryValue("system:message", CofaxToolsUtil.getI18NMessage(req.getLocale(), "incorrectPassword"));
            page.putGlossaryValue("system:highLightTab", "");
            page.putGlossaryValue("querystring", (String) req.getParameter("querystring"));
            try {
                CofaxToolsNavigation.includeResource(page, "/toolstemplates/login.jsp", req, res, session);
            } catch (Exception e) {
                System.err.println("THERE WAS AN ERROR: " + e);
            }
            return ("");
        }
    }

    /**
	 * Verify user information and populate the user (session) values if
	 * verification is true. Login is called upon user login and will be
	 * fulfilled only once per session.
	 */
    public static String login(DataStore db, HttpServletRequest req, HttpSession session, String login, String password, HttpServletResponse res, CofaxPage page) {
        HashMap userInfoHash = null;
        if (loginHandler != null) {
            userInfoHash = loginHandler.getUserHash(db, login, password);
        } else {
            userInfoHash = new HashMap();
        }
        return login(db, req, session, userInfoHash, res, page);
    }

    /**
	 * helper method for MD5 functions 
	 ***/
    private static String toHex(byte[] digest) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            buf.append(Integer.toHexString(0x0100 + (digest[i] & 0x00ff)).substring(1));
        }
        return buf.toString();
    }

    private static String md5Password(String login, String password) {
        String hash = "";
        String md5Password = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            hash = login + ":" + password;
            byte[] rawPass = hash.getBytes();
            try {
                md.update(rawPass);
            } catch (Exception e) {
                CofaxToolsUtil.log("CofaxToolsLogin login : " + e);
            }
            md5Password = toHex(md.digest());
        } catch (NoSuchAlgorithmException nsae) {
            CofaxToolsUtil.log("CofaxToolsLogin login : " + nsae);
        }
        return md5Password;
    }

    /**
	 * @return
	 */
    public static ILoginHandler getLoginHandler() {
        return loginHandler;
    }

    /**
	 * @param login
	 */
    public static void setLoginHandler(ILoginHandler login) {
        loginHandler = login;
    }
}
