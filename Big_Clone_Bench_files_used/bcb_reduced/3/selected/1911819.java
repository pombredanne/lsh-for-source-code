package org.one.stone.soup.wiki.access.control;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import org.one.stone.soup.wiki.controller.WikiControllerInterface;
import org.one.stone.soup.wiki.jar.manager.SystemAPI;
import org.one.stone.soup.wiki.processor.WikiHelper;

public class DefaultRegistrationService extends SystemAPI implements WikiAdministration, WikiAuthenticator, WikiRegistration {

    private int confirmationCount;

    private int combinerCount;

    private SessionStore sessionStore;

    private Hashtable<String, SignInChallenge> signInChallenges = new Hashtable<String, SignInChallenge>();

    private class SignInChallenge {

        private String combiner;

        private long created = System.currentTimeMillis();
    }

    public DefaultRegistrationService(WikiControllerInterface builder) throws Throwable {
        setBuilder(builder);
        sessionStore = new SessionStore();
    }

    public void init() throws Exception {
    }

    public boolean memberAvailable(String alias, String emailAddress) {
        try {
            if (getBuilder().getFileManager().pageExists("/Admin/Users/alias", getBuilder().getSystemLogin()) == true) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String authenticateUser(String sessionId) {
        return sessionStore.authenticateUser(sessionId);
    }

    public String[] getAllMembers() throws Exception {
        WikiHelper helper = new WikiHelper(getBuilder(), getBuilder().getSystemLogin());
        Hashtable membersTable = helper.getPageAsTable("/Admin/Users");
        Enumeration keys = membersTable.keys();
        ArrayList<String> members = new ArrayList<String>();
        while (keys.hasMoreElements()) {
            members.add((String) keys.nextElement());
        }
        return members.toArray(new String[] {});
    }

    public String[] getAllGroups() throws Exception {
        WikiHelper helper = new WikiHelper(getBuilder(), getBuilder().getSystemLogin());
        Hashtable groupsTable = helper.getPageAsTable("/Admin/Groups");
        Enumeration keys = groupsTable.keys();
        ArrayList<String> groups = new ArrayList<String>();
        while (keys.hasMoreElements()) {
            groups.add((String) keys.nextElement());
        }
        return groups.toArray(new String[] {});
    }

    public String[] getGroupMembers(String groupAlias) throws Exception {
        WikiHelper helper = new WikiHelper(getBuilder(), getBuilder().getSystemLogin());
        Hashtable membersTable = helper.getPageAsTable("/Admin/Groups/" + groupAlias);
        Enumeration keys = membersTable.keys();
        ArrayList<String> members = new ArrayList<String>();
        while (keys.hasMoreElements()) {
            members.add((String) keys.nextElement());
        }
        return members.toArray(new String[] {});
    }

    public String[] getMembersGroups(String memberAlias) throws Exception {
        ArrayList<String> membersGroups = new ArrayList<String>();
        String[] groups = getAllGroups();
        for (int loop = 0; loop < groups.length; loop++) {
            WikiHelper helper = new WikiHelper(getBuilder(), getBuilder().getSystemLogin());
            Hashtable membersTable = helper.getPageAsTable("/Admin/Groups/" + groups[loop]);
            if (membersTable.get(memberAlias) != null) {
                membersGroups.add(groups[loop]);
            }
        }
        return membersGroups.toArray(new String[] {});
    }

    public boolean memberIsInGroup(String memberAlias, String groupAlias) throws Exception {
        WikiHelper helper = new WikiHelper(getBuilder(), getBuilder().getSystemLogin());
        String[][] membersTable = helper.getPageAsList("/Admin/Groups/" + groupAlias);
        for (int loop = 0; loop < membersTable.length; loop++) {
            if (membersTable[loop][0].equals(memberAlias)) {
                return true;
            }
        }
        return false;
    }

    public void createGroup(String ownerAlias, String groupAlias) throws Exception {
    }

    public void addMemberToGroup(String memberAlias, String groupAlias) throws Exception {
    }

    public void invalidateSession(String sessionId) {
        sessionStore.invalidateSession(sessionId);
    }

    public String signIn(String userId, String challenge, String challengeResponse) {
        String password = null;
        try {
            password = getPassword(userId);
            if (password == null) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        if (signInChallenges.get(challenge) == null) {
            return null;
        } else {
            signInChallenges.remove(challenge);
        }
        String testResponse = getMD5(password + challenge);
        if (testResponse.equals(challengeResponse)) {
            return createSession(userId);
        }
        return null;
    }

    public String getChallenge() {
        String combiner = getCombinerString();
        SignInChallenge signInChallenge = new SignInChallenge();
        signInChallenge.combiner = combiner;
        signInChallenges.put(combiner, signInChallenge);
        return combiner;
    }

    public String[] checkAvailability(String emailAddress, String alias, String firstName, String secondName) throws Exception {
        ArrayList<String> messages = new ArrayList<String>();
        if (getBuilder().getFileManager().pageExists("/Admin/Users/" + alias, getBuilder().getSystemLogin()) == true) {
            messages.add("The alias " + alias + " is already in use by another member.");
        }
        return messages.toArray(new String[] {});
    }

    public void registerUser(String host, String emailAddress, String alias, String firstName, String secondName) throws Exception {
    }

    public boolean retrievePassword(String emailAddress) throws Exception {
        return false;
    }

    public String confirmRegistration(String confirmationString, String password) throws Exception {
        return null;
    }

    private String getPassword(String userId) throws Exception {
        return getBuilder().getFileManager().getPageAttachmentAsString("/Admin/Users/" + userId + "/private", "password.txt", getBuilder().getSystemLogin());
    }

    private String createSession(String userId) {
        return sessionStore.createSession(userId);
    }

    private synchronized String getSessionId() {
        return sessionStore.getSessionId();
    }

    private synchronized String getCombinerString() {
        combinerCount++;
        String data = combinerCount + " " + Math.random() * Integer.MAX_VALUE;
        return getMD5(data);
    }

    private String getMD5(String data) {
        try {
            MessageDigest md5Algorithm = MessageDigest.getInstance("MD5");
            md5Algorithm.update(data.getBytes(), 0, data.length());
            byte[] digest = md5Algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            String hexDigit = null;
            for (int i = 0; i < digest.length; i++) {
                hexDigit = Integer.toHexString(0xFF & digest[i]);
                if (hexDigit.length() < 2) {
                    hexDigit = "0" + hexDigit;
                }
                hexString.append(hexDigit);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ne) {
            return data;
        }
    }
}
