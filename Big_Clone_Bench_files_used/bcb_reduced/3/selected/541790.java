package gnu.saw.server.authentication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import gnu.saw.server.SAWServer;
import gnu.saw.server.connection.SAWServerConnection;
import gnu.saw.server.session.SAWServerSession;
import gnu.saw.terminal.SAWTerminal;

public class SAWServerAuthenticator {

    private String login;

    private byte[] digestedLogin;

    private String password;

    private byte[] digestedPassword;

    private byte[] usedCredential;

    private byte[] localNonce = new byte[512];

    private byte[] remoteNonce = new byte[512];

    private MessageDigest sha256Digester;

    private SAWServer server;

    private SAWServerConnection connection;

    public SAWServerAuthenticator(SAWServerSession session) {
        this.server = session.getServer();
        this.connection = session.getConnection();
        this.localNonce = session.getConnection().getLocalNonce();
        this.remoteNonce = session.getConnection().getRemoteNonce();
        try {
            this.sha256Digester = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public String getLogin() {
        return login;
    }

    public boolean tryAuthentication() {
        try {
            connection.getConnectionSocket().setSoTimeout(300000);
            connection.getAuthenticationWriter().write("SAW>SAWSERVER:Starting authentication...\n");
            connection.getAuthenticationWriter().flush();
            SAWTerminal.print("\rSAW>SAWSERVER:Starting authentication...\nSAW>");
            connection.getAuthenticationWriter().write("SAW>SAWSERVER:Enter the login:\n");
            connection.getAuthenticationWriter().flush();
            login = connection.getAuthenticationReader().readLine();
            if (login == null) {
                return false;
            }
            digestedLogin = Base64.decodeBase64(login.getBytes("UTF-8"));
            connection.getAuthenticationWriter().write("SAW>SAWSERVER:Enter the password:\n");
            connection.getAuthenticationWriter().flush();
            password = connection.getAuthenticationReader().readLine();
            if (password == null) {
                return false;
            }
            digestedPassword = Base64.decodeBase64(password.getBytes("UTF-8"));
            usedCredential = new byte[64];
            System.arraycopy(digestedLogin, 0, usedCredential, 0, 32);
            System.arraycopy(digestedPassword, 0, usedCredential, 32, 32);
            for (byte[] storedCredential : server.getUserCredentials().keySet()) {
                byte[] digestedCredential = new byte[storedCredential.length];
                System.arraycopy(storedCredential, 0, digestedCredential, 0, storedCredential.length);
                sha256Digester.update(digestedCredential, 0, 32);
                sha256Digester.update(localNonce);
                System.arraycopy(sha256Digester.digest(remoteNonce), 0, digestedCredential, 0, 32);
                sha256Digester.update(digestedCredential, 32, 32);
                sha256Digester.update(localNonce);
                System.arraycopy(sha256Digester.digest(remoteNonce), 0, digestedCredential, 32, 32);
                if (Arrays.equals(digestedCredential, usedCredential)) {
                    connection.getAuthenticationWriter().write("SAW>SAWSERVER:Authentication OK!\n");
                    SAWTerminal.print("\rSAW>SAWSERVER:Authentication OK!\nSAW>");
                    connection.getAuthenticationWriter().flush();
                    connection.getConnectionSocket().setSoTimeout(0);
                    login = server.getUserCredentials().get(storedCredential);
                    return true;
                }
            }
            SAWTerminal.print("\rSAW>SAWSERVER:Authentication failed!\nSAW>");
            connection.getAuthenticationWriter().write("SAW>SAWSERVER:Authentication failed!\n");
            connection.getAuthenticationWriter().flush();
            return false;
        } catch (Exception e) {
            SAWTerminal.print("\rSAW>SAWSERVER:Authentication failed!\nSAW>");
            try {
                connection.getAuthenticationWriter().write("SAW>SAWSERVER:Authentication failed!\n");
                connection.getAuthenticationWriter().flush();
                return false;
            } catch (Exception e1) {
                return false;
            }
        }
    }
}
