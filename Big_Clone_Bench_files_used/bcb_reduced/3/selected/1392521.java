package org.makagiga.editors;

import static org.makagiga.commons.UI._;
import java.awt.Window;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.makagiga.Vars;
import org.makagiga.commons.*;
import org.makagiga.fs.MetaInfo;
import org.makagiga.search.Hit;
import org.makagiga.search.Query;
import org.makagiga.tabs.Tabs;
import org.makagiga.tree.Tree;
import org.makagiga.tree.version.VersionControl;
import org.makagiga.tree.version.VersionException;

/**
 * @since 2.0
 */
public final class EditorPassword {

    public static InputStream createInputStream(final MetaInfo metaInfo, final File file) throws Exception {
        InputStream stream = new FS.BufferedFileInput(file);
        if (!isSet(metaInfo)) return stream;
        Cipher cipher = createCipher(metaInfo, Cipher.DECRYPT_MODE);
        return new CipherInputStream(stream, cipher);
    }

    public static OutputStream createOutputStream(final MetaInfo metaInfo, final File file) throws Exception {
        OutputStream stream = new FS.BufferedFileOutput(file);
        if (!isSet(metaInfo)) return stream;
        Cipher cipher = createCipher(metaInfo, Cipher.ENCRYPT_MODE);
        if (cipher == null) return null;
        return new DigestOutputStream(new CipherOutputStream(stream, cipher), MessageDigest.getInstance("SHA1"));
    }

    public static void deletePrivateData() {
        for (Hit i : Query.all()) i.get().clearProperty("cipher.info");
    }

    /**
	 * Returns @c true if file is password-based encrypted.
	 */
    public static boolean isSet(final Config config) {
        return config.read("encrypted", false);
    }

    /**
	 * Returns @c true if @p file is password-based encrypted.
	 */
    public static boolean isSet(final MetaInfo file) {
        return file.getConfig().read("encrypted", false);
    }

    public static void saveConfig(final MetaInfo metaInfo, final OutputStream output) {
        Config config = metaInfo.getConfig();
        if (output instanceof DigestOutputStream) {
            MessageDigest digest = DigestOutputStream.class.cast(output).getMessageDigest();
            config.write("encrypted", true);
            byte[] hash = digest.digest();
            config.write("Cipher.MessageDigest.value", toString(hash));
            try {
                VersionControl.deleteAllVersions(metaInfo, true);
            } catch (VersionException exception) {
                MLogger.exception(exception);
            }
        } else {
            config.removeBoolean("encrypted");
            config.removeString("Cipher.MessageDigest.value");
            config.removeString("Cipher.salt");
            config.removeString("Cipher.transformation");
        }
        config.save();
    }

    public static void set(final Window parent, final MetaInfo file) {
        if (true) {
            MMessage.info(parent, "This function is not available yet");
            return;
        }
        final Tabs tabs = Tabs.getInstance();
        int index = tabs.findEditor(file);
        if (index != -1) tabs.closeEditorAt(index, Tabs.REMOVE_TAB | Tabs.SAVE_TO_FILE);
        file.clearProperty("cipher.info");
        Tree.getInstance().open(file);
        Editor editor = tabs.getTabAt(file);
        if (editor == null) {
            MMessage.error(parent, _("Could not set password"));
            return;
        }
        Info info = newInfo(parent, file);
        if (info != null) {
            file.setProperty("cipher.info", info);
            Config config = file.getConfig();
            config.write("encrypted", true);
            config.save();
            try {
                editor.saveToFile();
            } catch (Exception exception) {
                MMessage.error(parent, exception);
            }
        }
    }

    /**
	 * Validates password for the specified input stream.
	 *
	 * @param file The file loaded by editor plugin
	 * @param input The input stream
	 */
    public static void validate(final MetaInfo metaInfo, final InputStream input) throws Exception {
        if (!(input instanceof CipherInputStream)) return;
        CipherInputStream cis = null;
        try {
            Info info = metaInfo.getProperty("cipher.info", null);
            Cipher cipher = info.createCipher(Cipher.DECRYPT_MODE);
            cis = new CipherInputStream(new FS.BufferedFileInput(metaInfo.getFile()), cipher);
            Checksum.Result result = Checksum.get("SHA1", cis);
            byte[] hash = result.getDigest();
            String digestString = toString(hash);
            String configString = metaInfo.getConfig().read("Cipher.MessageDigest.value", null);
            if (!digestString.equals(configString)) {
                metaInfo.clearProperty("cipher.info");
                if (Vars.passwordIgnoreBadHash.get()) {
                    MMessage.error(null, _("Invalid password"));
                    return;
                }
                throw new Exception(_("Invalid password"));
            }
        } finally {
            FS.close(cis);
        }
    }

    private static Cipher createCipher(final MetaInfo metaInfo, final int mode) throws Exception {
        Config config = metaInfo.getConfig();
        Info info = metaInfo.getProperty("cipher.info", null);
        if (mode == Cipher.DECRYPT_MODE) {
            if (info == null) {
                info = new Info();
                info.read(config);
                info.password = MMessage.enterPassword(null, _("Password for \"{0}\"", metaInfo));
                if (info.password == null) throw new Exception(_("Invalid password"));
            }
        } else if (mode == Cipher.ENCRYPT_MODE) {
            if (info == null) {
                if (isSet(config)) {
                    info = new Info();
                    info.read(config);
                    info.password = MMessage.enterPassword(null, _("Password for \"{0}\"", metaInfo));
                    if (info.password == null) throw new Exception(_("Invalid password"));
                } else {
                    info = newInfo(null, metaInfo);
                    if (info == null) throw new Exception(_("Invalid password"));
                }
            }
        }
        try {
            Cipher cipher = info.createCipher(mode);
            if (mode == Cipher.DECRYPT_MODE) metaInfo.setProperty("cipher.info", info);
            return cipher;
        } finally {
            if (info != null) info.clear();
        }
    }

    private static byte[] fromString(final String string) throws Exception {
        if (string == null) return new byte[0];
        String[] values = string.split(",");
        byte[] result = new byte[values.length];
        try {
            for (int i = 0; i < result.length; i++) result[i] = Byte.parseByte(values[i]);
        } catch (NumberFormatException exception) {
            throw new Exception(exception);
        }
        return result;
    }

    private static Info newInfo(final Window parent, final MetaInfo file) {
        char[] password = MMessage.newPassword(parent, _("Password for \"{0}\"", file));
        if (password == null) return null;
        Info info = new Info();
        info.init();
        info.password = password;
        info.save(file.getConfig());
        file.clearProperty("cipher.info");
        return info;
    }

    private static String toString(final byte[] array) {
        String result = "";
        for (byte i : array) {
            if (!result.isEmpty()) result += ",";
            result += Byte.toString(i);
        }
        return result;
    }

    @Uninstantiable
    private EditorPassword() {
    }

    private static final class Info {

        private byte[] saltArray;

        private char[] password;

        private PBEParameterSpec paramSpec;

        private SecretKey key;

        private String transformation;

        private void clear() {
            MPasswordField.clear(password);
            if (saltArray != null) Arrays.fill(saltArray, (byte) 0);
        }

        private Cipher createCipher(final int mode) throws Exception {
            if ((key == null) || (paramSpec == null)) {
                paramSpec = new PBEParameterSpec(saltArray, 20);
                PBEKeySpec keySpec = new PBEKeySpec(password);
                key = SecretKeyFactory.getInstance(transformation).generateSecret(keySpec);
                keySpec.clearPassword();
            }
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(mode, key, paramSpec);
            return cipher;
        }

        private void init() {
            clear();
            transformation = "PBEWithSHA1AndDESede";
            saltArray = new byte[9];
            new SecureRandom().nextBytes(saltArray);
        }

        private void read(final Config config) throws Exception {
            transformation = config.read("Cipher.transformation", "PBEWithSHA1AndDESede");
            saltArray = fromString(config.read("Cipher.salt", null));
        }

        private void save(final Config config) {
            config.write("Cipher.salt", EditorPassword.toString(saltArray));
            config.write("Cipher.transformation", transformation);
            config.save();
        }
    }
}
