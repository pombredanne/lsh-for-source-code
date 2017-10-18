package quizgame.server.usertypes;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import quizgame.server.AccountManager;
import quizgame.server.IniEditor;

/**
 *
 * @author Dukken
 */
public class AccountDatabase {

    Map<String, String> adminAccounts = new HashMap<String, String>();

    Map<String, String> pulpitAccounts = new HashMap<String, String>();

    Map<String, String> mainscreenAccounts = new HashMap<String, String>();

    Map<String, String> typerAccounts = new HashMap<String, String>();

    IniEditor ini;

    String[] sectionNames = { "admins", "pulpits", "mainscreens", "typers" };

    public AccountDatabase() {
        initIniEditor();
    }

    private void initIniEditor() {
        char[] commentDelim = { '#' };
        ini = new IniEditor(commentDelim, false);
        checkSections();
    }

    /**
     *  Add accounts from the specified inputstream.
     *  Accouns already in the database will not be removed.
     *  If an account read by this function already exists in the databaes,
     *  the old account will be replaced by this method.
     *  @param inStream The stream to read from.
     *  @param isEncrypted true if the file to be loaded has SHA encrypted passwords, otherwise false.
     *  @throws IOException if an IO-error occurs when reading from the stream, or if the format of the input is wrong.
     */
    public void readAccounts(InputStreamReader inStream, boolean isEncrypted) throws IOException {
        ini.load(inStream);
        checkSections();
        loadAllAccounts(isEncrypted);
    }

    /**
     *  Removes all accounts from this AccountDatabase.
     */
    public void removeAll() {
        adminAccounts.clear();
        mainscreenAccounts.clear();
        pulpitAccounts.clear();
        typerAccounts.clear();
    }

    /**
     *  Saves the current accounts to the specified account file, passwords will be SHA encrypted.
     *  @param outStream the outputstream to write to.
     */
    public void writeAccounts(OutputStreamWriter outStream) throws IOException {
        prepareIni(adminAccounts, "admins");
        prepareIni(mainscreenAccounts, "mainscreens");
        prepareIni(pulpitAccounts, "pulpits");
        prepareIni(typerAccounts, "typers");
        ini.save(outStream);
    }

    private void prepareIni(Map<String, String> accounts, String section) {
        for (Map.Entry<String, String> elem : accounts.entrySet()) {
            if (!ini.hasOption(section, elem.getKey())) {
                ini.set(section, elem.getKey(), elem.getValue());
            }
        }
    }

    private void checkSections() {
        for (String elem : sectionNames) {
            if (!ini.hasSection(elem)) ini.addSection(elem);
        }
    }

    private void loadAllAccounts(boolean isEncrypted) {
        fillAccountSet(adminAccounts, "admins", isEncrypted);
        fillAccountSet(pulpitAccounts, "pulpits", isEncrypted);
        fillAccountSet(mainscreenAccounts, "mainscreens", isEncrypted);
        fillAccountSet(typerAccounts, "typers", isEncrypted);
    }

    private void fillAccountSet(Map<String, String> accounts, String section, boolean isEncrypted) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        for (String name : ini.optionNames(section)) {
            String password = ini.get(section, name);
            if (!isEncrypted) {
                password = new String(md.digest(password.getBytes()));
            }
            accounts.put(name, password);
        }
    }

    /**
     *  Checks whether a account name exist in the accountDatabase.
     *  @return true if user exist, otherwist false.
     */
    public boolean hasAccount(String name, AccountManager.ClientRole role) {
        switch(role) {
            case ADMIN:
                return adminAccounts.containsKey(name);
            case PULPIT:
                return pulpitAccounts.containsKey(name);
            case MAIN_SCREEN:
                return mainscreenAccounts.containsKey(name);
            case TYPER:
                return typerAccounts.containsKey(name);
        }
        return false;
    }

    /**
     *  Checks whether the supplied password equals the stored password for an account.
     *  @param name The account name.
     *  @param password The password to compare with.
     *  @param role Type of account
     *  @return true if and only if the account exists and the account password equals the supplied password. Otherwise false.
     */
    public boolean checkPassword(String name, String password, AccountManager.ClientRole role) {
        String accountPassword = null;
        switch(role) {
            case ADMIN:
                accountPassword = adminAccounts.get(name);
                break;
            case PULPIT:
                accountPassword = pulpitAccounts.get(name);
                break;
            case MAIN_SCREEN:
                accountPassword = mainscreenAccounts.get(name);
                break;
            case TYPER:
                accountPassword = typerAccounts.get(name);
                break;
        }
        if (password.equals(accountPassword)) {
            return true;
        }
        return false;
    }

    /**
     *  Adds an account in this account database if username does not exist in the database for the specified role.
     *  @param name The username.
     *  @param password The password.
     *  @param role The role.
     *  @return true if account was added, otherwise false.
     */
    public boolean addAccount(String name, String password, AccountManager.ClientRole role) {
        if (hasAccount(name, role)) {
            return false;
        }
        switch(role) {
            case ADMIN:
                adminAccounts.put(name, password);
                return true;
            case MAIN_SCREEN:
                mainscreenAccounts.put(name, password);
                return true;
            case PULPIT:
                pulpitAccounts.put(name, password);
                return true;
            case TYPER:
                typerAccounts.put(name, password);
                return true;
            default:
                return false;
        }
    }
}
