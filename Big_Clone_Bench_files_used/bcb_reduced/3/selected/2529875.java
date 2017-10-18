package com.ericdaugherty.mail.server.configuration;

import java.security.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class PasswordFactory {

    /** Logger Category for this class. */
    private static Log log = LogFactory.getLog(PasswordFactory.class);

    private static PasswordFactory instance;

    public static void instantiate(BackEndTypeEnum backEndType) {
        if (instance == null) {
            instance = new PasswordFactory(backEndType);
        }
    }

    public static void shutdown() {
        instance = null;
    }

    public static PasswordFactory getInstance() {
        return instance;
    }

    private final PasswordHasherGetter peg;

    private PasswordFactory(BackEndTypeEnum backEndType) {
        if (backEndType == BackEndTypeEnum.FILE) {
            peg = new FilePasswordHasherGetter();
        } else {
            peg = new DbPasswordHasherGetter();
        }
    }

    public final PasswordHasher getPasswordHasher() {
        return peg.getHasher();
    }

    private final class DbPasswordHasherGetter extends PasswordHasherGetter {

        public final PasswordHasher getHasher() {
            return new DbPasswordHasher();
        }
    }

    private final class FilePasswordHasherGetter extends PasswordHasherGetter {

        public final PasswordHasher getHasher() {
            return new FilePasswordHasher();
        }
    }

    private abstract class PasswordHasherGetter {

        public abstract PasswordHasher getHasher();
    }

    private final class DbPasswordHasher extends AbstractPasswordHasher {

        private byte[] salt;

        @Override
        public final void setSalt(String salt) {
            this.salt = stringToByte(salt, 16);
        }

        @Override
        public final String getSalt() {
            return byteToString(salt, 48);
        }

        public final String hashPassword(final String password) {
            try {
                if (salt == null) {
                    salt = new byte[16];
                    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
                    sr.setSeed(System.currentTimeMillis());
                    sr.nextBytes(salt);
                }
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(salt);
                md.update(password.getBytes("UTF-8"));
                byte[] hash = md.digest();
                for (int i = 0; i < (1999); i++) {
                    md.reset();
                    hash = md.digest(hash);
                }
                return byteToString(hash, 60);
            } catch (Exception exception) {
                log.error(exception);
                return null;
            }
        }

        public final boolean passwordMatches(String storedPassword, String password) {
            if (salt == null) {
                return false;
            }
            return storedPassword.equals(hashPassword(password));
        }
    }

    private final class FilePasswordHasher extends AbstractPasswordHasher {

        public final void setSalt(String salt) {
        }

        public final String getSalt() {
            return null;
        }

        public final String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(password.getBytes());
                byte[] hash = md.digest();
                return "{SHA}" + byteToString(hash, 60);
            } catch (NoSuchAlgorithmException nsae) {
                log.error("Error getting password hash - " + nsae.getMessage());
                return null;
            }
        }

        public final boolean passwordMatches(String storedPassword, String password) {
            return storedPassword.equals(hashPassword(password));
        }
    }

    private abstract class AbstractPasswordHasher implements PasswordHasher {

        public abstract void setSalt(String salt);

        public abstract String getSalt();

        public abstract String hashPassword(String password);

        public final String hashRealmPassword(String username, String realm, String password) throws GeneralSecurityException {
            MessageDigest md = null;
            md = MessageDigest.getInstance("MD5");
            md.update(username.getBytes());
            md.update(":".getBytes());
            md.update(realm.getBytes());
            md.update(":".getBytes());
            md.update(password.getBytes());
            byte[] hash = md.digest();
            return toHex(hash, hash.length);
        }

        public abstract boolean passwordMatches(String storedPassword, String password);

        protected final byte[] stringToByte(String string, int size) {
            byte[] bytes = new byte[size];
            for (int i = 0; i < string.length(); i += 3) {
                bytes[i / 3] = Byte.parseByte("" + (Integer.valueOf(string.substring(i, i + 3)) - 128));
            }
            return bytes;
        }

        protected final String byteToString(byte[] hash, int size) {
            StringBuilder hashStringBuf = new StringBuilder(size);
            String byteString;
            int byteLength;
            for (int index = 0; index < hash.length; index++) {
                byteString = String.valueOf(hash[index] + 128);
                byteLength = byteString.length();
                switch(byteLength) {
                    case 1:
                        byteString = "00" + byteString;
                        break;
                    case 2:
                        byteString = "0" + byteString;
                        break;
                }
                hashStringBuf.append(byteString);
            }
            return hashStringBuf.toString();
        }

        private String toHex(byte[] b, int len) {
            if (b == null) {
                return "";
            }
            StringBuilder s = new StringBuilder(96);
            int i;
            for (i = 0; i < len; i++) {
                s.append(toHex(b[i]));
            }
            return s.toString();
        }

        private String toHex(byte b) {
            int i = new Integer((((int) b) << 24) >>> 24).intValue();
            if (i < (byte) 16) {
                return "0" + Integer.toString(i, 16);
            } else {
                return Integer.toString(i, 16);
            }
        }
    }
}
