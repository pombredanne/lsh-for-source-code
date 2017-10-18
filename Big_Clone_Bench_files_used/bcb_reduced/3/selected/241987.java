package gnu.saw.client.authentication;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import gnu.saw.client.SAWClient;
import gnu.saw.client.connection.SAWClientConnection;
import gnu.saw.client.session.SAWClientSession;
import gnu.saw.terminal.SAWTerminal;

public class SAWClientAuthenticator {

    private int credentialCounter;

    private byte[] localNonce = new byte[512];

    private byte[] remoteNonce = new byte[512];

    private MessageDigest sha256Digester;

    private SAWClient client;

    private SAWClientConnection connection;

    public SAWClientAuthenticator(SAWClientSession session) {
        this.client = session.getClient();
        this.connection = session.getConnection();
        this.localNonce = session.getConnection().getLocalNonce();
        this.remoteNonce = session.getConnection().getRemoteNonce();
        this.credentialCounter = 0;
        try {
            this.sha256Digester = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public boolean tryAuthentication() throws InterruptedException {
        credentialCounter = 0;
        try {
            connection.getConnectionSocket().setSoTimeout(300000);
            String warning = connection.getAuthenticationReader().readLine();
            if (warning != null) {
                SAWTerminal.print("\n" + warning);
                while (credentialCounter < 2) {
                    String prompt = connection.getAuthenticationReader().readLine();
                    if (prompt == null) {
                        return false;
                    }
                    SAWTerminal.print("\n" + prompt);
                    if (!writeCredential()) {
                        return false;
                    }
                }
                String result = connection.getAuthenticationReader().readLine();
                if (result != null) {
                    SAWTerminal.print("\n" + result);
                } else {
                    return false;
                }
                if (result.equals("SAW>SAWSERVER:Authentication OK!")) {
                    connection.getConnectionSocket().setSoTimeout(0);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public boolean writeCredential() throws IOException, InterruptedException {
        String line = null;
        if (credentialCounter == 0 && client.getLogin() != null && client.getLogin().length() > 0) {
            line = client.getLogin();
            client.setLogin(null);
            SAWTerminal.println("");
        } else if (credentialCounter == 1 && client.getPassword() != null && client.getPassword().length() > 0) {
            line = client.getPassword();
            client.setPassword(null);
            SAWTerminal.println("");
        } else {
            line = SAWTerminal.readLine(false);
        }
        if (line == null) {
            System.exit(0);
        }
        sha256Digester.update(sha256Digester.digest(line.getBytes("UTF-8")));
        sha256Digester.update(remoteNonce);
        connection.getAuthenticationWriter().write(new String(Base64.encodeBase64(sha256Digester.digest(localNonce)), "UTF-8") + "\n");
        SAWTerminal.print("SAW>SAWCLIENT:Information sent!");
        connection.getAuthenticationWriter().flush();
        credentialCounter++;
        return true;
    }
}
