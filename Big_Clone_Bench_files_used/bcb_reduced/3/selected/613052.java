package subget;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import subget.bundles.Bundles;
import subget.exceptions.BadLoginException;
import subget.exceptions.TimeoutException;

public abstract class Napi {

    public static final String[] supportedLanguages = { "en", "pl" };

    public static enum AddUserResponse {

        NAPI_ADD_USER_OK, NAPI_ADD_USER_LOGIN_EXISTS, NAPI_ADD_USER_EMAIL_EXISTS, NAPI_ADD_USER_BAD_LOGIN, NAPI_ADD_USER_BAD_PASS, NAPI_ADD_USER_BAD_EMAIL, NAPI_ADD_USER_BAD_UNKNOWN
    }

    public static final String napiArchivePassword = "iBlm8NTigvru0Jr0";

    public static String getNapiMd5sum(File file) throws NoSuchAlgorithmException, IOException {
        int length;
        if (file.length() > 10485760) {
            length = 10485760;
        } else {
            length = (int) file.length();
        }
        byte[] buffer = new byte[length];
        FileInputStream fis = new FileInputStream(file);
        fis.read(buffer, 0, length);
        fis.close();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(buffer);
        byte[] hash = md5.digest();
        StringBuffer hexString = new StringBuffer();
        String hexPart;
        for (int i = 0; i < hash.length; i++) {
            hexPart = Integer.toHexString(0xFF & hash[i]);
            if (hexPart.length() == 1) {
                hexPart = "0" + hexPart;
            }
            hexString.append(hexPart);
        }
        return hexString.toString();
    }

    public static String getNapiHash(String napiMd5sum) throws NoSuchAlgorithmException, IOException {
        String napiHash = "";
        int IDX[] = { 0xe, 0x3, 0x6, 0x8, 0x2 };
        int MUL[] = { 2, 2, 5, 4, 3 };
        int ADD[] = { 0, 0xd, 0x10, 0xb, 0x5 };
        for (int i = 0; i < 5; ++i) {
            int a = ADD[i];
            int m = MUL[i];
            int j = IDX[i];
            int t = a + Character.digit(napiMd5sum.charAt(j), 16);
            int t2 = t + 2;
            if (t2 > 32) {
                t2 = 32;
            }
            int v = Integer.valueOf(napiMd5sum.substring(t, t2), 16).intValue();
            String str = Integer.toHexString(v * m);
            napiHash += String.valueOf(str.charAt(str.length() - 1));
        }
        return napiHash;
    }

    public static boolean napiUserCheck(String user, String pass) throws TimeoutException, InterruptedException, IOException {
        URLConnection conn = null;
        InputStream in = null;
        URL url = new URL("http://www.napiprojekt.pl/users_check.php?nick=" + user + "&pswd=" + pass);
        conn = url.openConnection(Global.getProxy());
        in = Timeouts.getInputStream(conn);
        byte[] buffer = new byte[1024];
        in.read(buffer, 0, 1024);
        if (in != null) {
            in.close();
        }
        String response = new String(buffer);
        if (response.indexOf("ok") == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static AddUserResponse napiUserAdd(String user, String pass, String email) throws TimeoutException, InterruptedException, IOException {
        if (user.matches("^[a-zA-Z0-9]{2,20}$") == false) {
            return AddUserResponse.NAPI_ADD_USER_BAD_LOGIN;
        }
        if (pass.equals("")) {
            return AddUserResponse.NAPI_ADD_USER_BAD_PASS;
        }
        if (email.matches("^[a-zA-Z0-9\\-\\_]{1,30}@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)+$") == false) {
            return AddUserResponse.NAPI_ADD_USER_BAD_EMAIL;
        }
        URLConnection conn = null;
        ClientHttpRequest httpPost = null;
        InputStreamReader responseStream = null;
        URL url = new URL("http://www.napiprojekt.pl/users_add.php");
        conn = url.openConnection(Global.getProxy());
        httpPost = new ClientHttpRequest(conn);
        httpPost.setParameter("login", user);
        httpPost.setParameter("haslo", pass);
        httpPost.setParameter("mail", email);
        httpPost.setParameter("z_programu", "true");
        responseStream = new InputStreamReader(httpPost.post(), "Cp1250");
        BufferedReader responseReader = new BufferedReader(responseStream);
        String response = responseReader.readLine();
        if (response.indexOf("login już istnieje") != -1) {
            return AddUserResponse.NAPI_ADD_USER_LOGIN_EXISTS;
        }
        if (response.indexOf("na podany e-mail") != -1) {
            return AddUserResponse.NAPI_ADD_USER_EMAIL_EXISTS;
        }
        if (response.indexOf("NPc0") == 0) {
            return AddUserResponse.NAPI_ADD_USER_OK;
        }
        return AddUserResponse.NAPI_ADD_USER_BAD_UNKNOWN;
    }

    public static boolean userLogIn() throws InterruptedException, BadLoginException, TimeoutException, IOException {
        try {
            boolean logged = false;
            if (Global.getNapiUserName().equals("") || Global.getNapiUserPass().equals("")) {
                if (Global.getNapiSessionUserName().equals("") || Global.getNapiSessionUserPass().equals("")) {
                    logged = false;
                    do {
                        String user = "";
                        if (Global.getNapiSessionUserName().equals("") == false) {
                            user = Global.getNapiSessionUserName();
                        } else {
                            user = Global.getNapiUserName();
                        }
                        String[] login = Global.dialogs.showUserPasswordDialog(Global.SubDataBase.BASE_NAPI, user);
                        if (login == null) {
                            throw (new BadLoginException("Could not log in to NAPI."));
                        } else {
                            Global.setNapiSessionUserName(login[0]);
                            logged = Napi.napiUserCheck(login[0], login[1]);
                        }
                        if (logged == false) {
                            Global.dialogs.showErrorDialog(Bundles.subgetBundle.getString("Login_failed."), Bundles.subgetBundle.getString("Wrong_user/password."));
                        } else {
                            Global.setNapiSessionUserPass(login[1]);
                        }
                    } while (logged == false);
                }
            } else {
                Global.setNapiSessionUserName(Global.getNapiUserName());
                Global.setNapiSessionUserPass(Global.getNapiUserPass());
                String user = Global.getNapiSessionUserName();
                String pass = Global.getNapiSessionUserPass();
                logged = Napi.napiUserCheck(user, pass);
                while (logged == false) {
                    Global.dialogs.showErrorDialog(Bundles.subgetBundle.getString("Login_failed."), Bundles.subgetBundle.getString("Wrong_user/password."));
                    String[] login = Global.dialogs.showUserPasswordDialog(Global.SubDataBase.BASE_NAPI, user);
                    if (login == null) {
                        throw (new BadLoginException("Could not log in to NAPI."));
                    } else {
                        Global.setNapiSessionUserName(login[0]);
                        logged = Napi.napiUserCheck(login[0], login[1]);
                    }
                    if (logged) {
                        Global.setNapiSessionUserPass(login[1]);
                    }
                }
            }
            return logged;
        } catch (TimeoutException ex) {
            throw new TimeoutException("Could not log in to NAPI, timeout.", ex.getCause());
        } catch (IOException ex) {
            throw new IOException("Could not log in to NAPI, connection error.", ex.getCause());
        }
    }
}
