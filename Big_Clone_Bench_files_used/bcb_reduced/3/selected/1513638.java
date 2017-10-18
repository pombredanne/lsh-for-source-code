package org.eclipse.equinox.internal.security.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.osgi.util.NLS;

public class PasswordManagement {

    /**
	 * Algorithm used to digest passwords
	 */
    private static final String DIGEST_ALGORITHM = "MD5";

    /**
	 * Node used to store encrypted password for the password recovery
	 */
    private static final String PASSWORD_RECOVERY_NODE = "/org.eclipse.equinox.secure.storage/recovery";

    /**
	 * Pseudo-module ID to use when encryption is done with the default password.
	 */
    protected static final String RECOVERY_PSEUDO_ID = "org.eclipse.equinox.security.recoveryModule";

    /**
	 * Key used to store encrypted password for the password recovery
	 */
    private static final String PASSWORD_RECOVERY_KEY = "org.eclipse.equinox.security.internal.recovery.password";

    /**
	 * Key used to store questions for the password recovery
	 */
    private static final String PASSWORD_RECOVERY_QUESTION = "org.eclipse.equinox.security.internal.recovery.question";

    public static void setupRecovery(String[][] challengeResponse, String moduleID, IPreferencesContainer container) {
        SecurePreferencesRoot root = ((SecurePreferencesContainer) container).getRootData();
        SecurePreferences node = recoveryNode(root, moduleID);
        if (challengeResponse == null) {
            node.remove(PASSWORD_RECOVERY_KEY);
            for (int i = 0; i < 2; i++) {
                String key = PASSWORD_RECOVERY_QUESTION + Integer.toString(i + 1);
                node.remove(key);
            }
            root.markModified();
            return;
        }
        String internalPassword = mashPassword(challengeResponse[1]);
        PasswordExt internalPasswordExt = new PasswordExt(new PBEKeySpec(internalPassword.toCharArray()), RECOVERY_PSEUDO_ID);
        PasswordExt password;
        try {
            password = root.getPassword(moduleID, container, false);
        } catch (StorageException e) {
            AuthPlugin.getDefault().logError(SecAuthMessages.failedCreateRecovery, e);
            return;
        }
        try {
            byte[] data = new String(password.getPassword().getPassword()).getBytes();
            CryptoData encryptedValue = root.getCipher().encrypt(internalPasswordExt, data);
            node.internalPut(PASSWORD_RECOVERY_KEY, encryptedValue.toString());
            root.markModified();
        } catch (StorageException e) {
            AuthPlugin.getDefault().logError(SecAuthMessages.failedCreateRecovery, e);
            return;
        }
        for (int i = 0; i < challengeResponse[0].length; i++) {
            String key = PASSWORD_RECOVERY_QUESTION + Integer.toString(i + 1);
            try {
                node.put(key, challengeResponse[0][i], false, (SecurePreferencesContainer) container);
            } catch (StorageException e) {
            }
        }
    }

    public static String[] getPasswordRecoveryQuestions(SecurePreferencesRoot root, String moduleID) {
        List questions = new ArrayList();
        SecurePreferences node = recoveryNode(root, moduleID);
        for (int i = 0; ; i++) {
            String key = PASSWORD_RECOVERY_QUESTION + Integer.toString(i + 1);
            if (!node.hasKey(key)) break;
            try {
                String question = node.get(key, null, null);
                if (question == null) break;
                questions.add(question);
            } catch (StorageException e) {
            }
        }
        String[] result = new String[questions.size()];
        return (String[]) questions.toArray(result);
    }

    public static String recoverPassword(String[] answers, SecurePreferencesRoot root, String moduleID) {
        String internalPassword = mashPassword(answers);
        SecurePreferences node = recoveryNode(root, moduleID);
        PasswordExt internalPasswordExt = new PasswordExt(new PBEKeySpec(internalPassword.toCharArray()), RECOVERY_PSEUDO_ID);
        try {
            CryptoData encryptedData = new CryptoData(node.internalGet(PASSWORD_RECOVERY_KEY));
            byte[] data = root.getCipher().decrypt(internalPasswordExt, encryptedData);
            return new String(data);
        } catch (IllegalStateException e) {
            return null;
        } catch (IllegalBlockSizeException e) {
            return null;
        } catch (BadPaddingException e) {
            return null;
        } catch (StorageException e) {
            return null;
        }
    }

    private static SecurePreferences recoveryNode(SecurePreferences root, String moduleID) {
        return root.node(PASSWORD_RECOVERY_NODE).node(moduleID);
    }

    /**
	 * Produces password from a list of answers:
	 * - all answers are put into one string
	 * - characters from alternating ends of the string are taken to form "mashed up" recovery 
	 * password
	 * - the secure digest of the "mashed up" string is created
	 * 
	 * This procedure should improve quality of the recovery password - even if answers 
	 * are dictionary words, digested "mashed up" password should be of a reasonable good quality 
	 */
    private static String mashPassword(String[] answers) {
        StringBuffer tmp = new StringBuffer();
        for (int i = 0; i < answers.length; i++) {
            tmp.append(answers[i].trim());
        }
        StringBuffer mix = new StringBuffer();
        int pos = tmp.length() - 1;
        for (int i = 0; i <= pos; i++) {
            mix.append(tmp.charAt(i));
            if (i < pos) mix.append(tmp.charAt(pos));
            pos--;
        }
        String internalPassword;
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] digested = digest.digest(mix.toString().getBytes());
            internalPassword = EncodingUtils.encodeBase64(digested);
        } catch (NoSuchAlgorithmException e) {
            String msg = NLS.bind(SecAuthMessages.noDigest, DIGEST_ALGORITHM);
            AuthPlugin.getDefault().logMessage(msg);
            internalPassword = mix.toString();
        }
        return internalPassword;
    }
}
