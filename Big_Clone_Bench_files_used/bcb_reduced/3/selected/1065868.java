package org.openthinclient.console.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.apache.log4j.Logger;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openthinclient.console.ConsoleFrame;
import org.openthinclient.console.Messages;
import org.openthinclient.ldap.auth.CachingCallbackHandler;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A CallbackHandler which provides a fixed combination of username and
 * password.
 * 
 * @author levigo
 */
public class UsernamePasswordCallbackHandler implements CachingCallbackHandler {

    private static final Preferences prefs = ConsoleFrame.PREFERENCES_ROOT.node("credentials");

    private static final Logger logger = Logger.getLogger(UsernamePasswordCallbackHandler.class);

    private class PasswordEntryPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private final JPasswordField passwordField;

        private final JCheckBox savePasswordBox;

        private final JTextField userField;

        PasswordEntryPanel(boolean askForUsername) throws IOException {
            final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout("p, 3dlu, f:p:g"), Messages.getBundle(), this);
            dfb.setDefaultDialogBorder();
            dfb.append(new JLabel(Messages.getString("UsernamePassword.subtitle")), 3);
            dfb.nextLine();
            final JLabel pd = new JLabel(protectionDomain);
            dfb.append("", pd);
            dfb.nextLine();
            dfb.appendUnrelatedComponentsGapRow();
            dfb.nextLine();
            userField = new JTextField(username);
            userField.setEnabled(true);
            dfb.appendI15d("UsernamePassword.username", userField);
            dfb.nextLine();
            passwordField = new JPasswordField();
            dfb.appendI15d("UsernamePassword.password", passwordField);
            dfb.nextLine();
            savePasswordBox = new JCheckBox(Messages.getString("UsernamePassword.savePassword"));
            dfb.append(savePasswordBox, 3);
        }

        char[] getPassword() {
            return passwordField.getPassword();
        }

        boolean getSavePassword() {
            return savePasswordBox.isSelected();
        }

        String getUsername() {
            return userField.getText();
        }
    }

    private String protectionDomain;

    private Key key;

    private String username;

    private SealedObject password;

    private boolean savePassword;

    public UsernamePasswordCallbackHandler(String protectionDomain, String username, char password[], boolean savePassword) {
        this.protectionDomain = protectionDomain;
        this.savePassword = savePassword;
        try {
            this.key = KeyGenerator.getInstance("DES").generateKey();
            this.username = username;
            this.password = scramble(password);
            saveCredentials(savePassword);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Crypto prerequisites not met", e);
        } catch (final IOException e) {
            throw new RuntimeException("Can't scramble credentials", e);
        }
    }

    public UsernamePasswordCallbackHandler(String protectionDomain) {
        this.protectionDomain = protectionDomain;
        try {
            this.key = KeyGenerator.getInstance("DES").generateKey();
            this.username = null;
            this.password = null;
            final String sn = getStoreName();
            if (prefs.nodeExists(sn)) {
                final Preferences p = prefs.node(sn);
                username = p.get("username", "");
                final byte[] scrambledPassword = p.getByteArray("password", null);
                if (scrambledPassword != null) try {
                    final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(scrambledPassword));
                    this.password = (SealedObject) ois.readObject();
                    this.key = (Key) ois.readObject();
                } catch (final Exception e) {
                    logger.error("Can't load saved credentials - will query user");
                }
            }
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Crypto prerequisites not met", e);
        } catch (final BackingStoreException e) {
            throw new RuntimeException("Can't access credentials", e);
        }
    }

    /**
	 * @param transientPassword
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
    private void saveCredentials(boolean savePassword) throws IOException {
        final Preferences p = prefs.node(getStoreName());
        p.put("username", username);
        if (savePassword) try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(password);
            oos.writeObject(key);
            oos.flush();
            p.putByteArray("password", baos.toByteArray());
        } catch (final IOException ioe) {
            ErrorManager.getDefault().notify(ioe);
        }
    }

    /**
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
    private String getStoreName() {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(protectionDomain.getBytes());
            final byte[] bs = digest.digest();
            final StringBuffer sb = new StringBuffer(bs.length * 2);
            for (int i = 0; i < bs.length; i++) {
                final String s = Integer.toHexString(bs[i] & 0xff);
                if (s.length() < 2) sb.append('0');
                sb.append(s);
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Can't save credentials: digest method MD5 unavailable.");
        }
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            final Callback callback = callbacks[i];
            if (callback instanceof NameCallback) {
                if (null == username) queryForPassword();
                ((NameCallback) callback).setName(username);
            } else if (callback instanceof PasswordCallback) {
                if (null == password) queryForPassword();
                ((PasswordCallback) callback).setPassword((char[]) unscramble(password));
            }
        }
    }

    /**
	 * @throws IOException
	 * 
	 */
    private void queryForPassword() throws IOException {
        final PasswordEntryPanel pep = new PasswordEntryPanel(false);
        final DialogDescriptor dd = new DialogDescriptor(pep, Messages.getString("UsernamePassword.titleEnterPassword"), true, null);
        DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
        if (dd.getValue() == DialogDescriptor.CANCEL_OPTION) throw new IOException("User cancelled password entry");
        username = pep.getUsername();
        password = scramble(pep.getPassword());
        saveCredentials(pep.getSavePassword());
    }

    /**
	 * @throws IOException
	 * 
	 */
    public void purgeCache() throws IOException {
        final PasswordEntryPanel pep = new PasswordEntryPanel(true);
        final DialogDescriptor dd = new DialogDescriptor(pep, Messages.getString("UsernamePassword.titleLoginFailed"), true, null);
        DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
        if (dd.getValue() == DialogDescriptor.CANCEL_OPTION) throw new IOException("User cancelled password entry");
        username = pep.getUsername();
        password = scramble(pep.getPassword());
        saveCredentials(pep.getSavePassword());
    }

    /**
	 * @param password2
	 * @return
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws IOException
	 * @throws IllegalBlockSizeException
	 */
    private SealedObject scramble(Serializable clear) throws IOException {
        try {
            final Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return new SealedObject(clear, cipher);
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            throw new IOException("Can't scramble: " + e);
        }
    }

    /**
	 * @param password2
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
    private Object unscramble(SealedObject scrambled) throws IOException {
        try {
            return scrambled.getObject(key);
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            throw new IOException("Can't unscramble: " + e);
        }
    }

    public String getProtectionDomain() {
        return protectionDomain;
    }

    public void setProtectionDomain(String protectionDomain) throws IOException {
        try {
            this.deleteCredentials();
        } catch (BackingStoreException e) {
            throw new RuntimeException("Can't access credentials", e);
        }
        this.protectionDomain = protectionDomain;
        saveCredentials(savePassword);
    }

    public void deleteCredentials() throws BackingStoreException {
        final Preferences p = prefs.node(getStoreName());
        p.removeNode();
    }
}
