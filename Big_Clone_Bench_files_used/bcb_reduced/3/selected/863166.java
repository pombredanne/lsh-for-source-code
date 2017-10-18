package de.intarsys.pdf.crypt;

import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * The {@link ISecurityHandler} implementing /R 2 of the PDF spec.
 * 
 */
public class StandardSecurityHandlerR2 extends StandardSecurityHandler {

    public static final String DIGEST_ALGORITHM = "MD5";

    public static final String KEY_ALGORITHM = "RC4";

    public static final String CIPHER_ALGORITHM = "RC4";

    public StandardSecurityHandlerR2() {
        super();
    }

    @Override
    public boolean authenticateOwner(byte[] owner) throws COSSecurityException {
        try {
            byte[] preparedOwner = prepareBytes(owner);
            MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            md.update(preparedOwner);
            byte[] key = md.digest();
            int length = 5;
            byte[] encryptionKey = new byte[length];
            System.arraycopy(key, 0, encryptionKey, 0, length);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            if (cipher == null) {
                throw new COSSecurityException("RC4 cipher not found");
            }
            SecretKey skeySpec;
            byte[] encrypted = getO();
            skeySpec = new SecretKeySpec(encryptionKey, KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            encrypted = cipher.doFinal(encrypted);
            if (authenticateUser(encrypted)) {
                setActiveAccessPermissions(AccessPermissionsFull.get());
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new COSSecurityException(e);
        }
    }

    @Override
    public boolean authenticateUser(byte[] user) throws COSSecurityException {
        byte[] entryU = getU();
        byte[] tempU = createUserPassword(user);
        if (entryU.length != tempU.length) {
            return false;
        }
        for (int i = 0; i < tempU.length; i++) {
            if (entryU[i] != tempU[i]) {
                return false;
            }
        }
        setCryptKey(createCryptKey(user));
        setActiveAccessPermissions(createAccessPermissions());
        return true;
    }

    @Override
    protected IAccessPermissions createAccessPermissions() {
        return new AccessPermissionsR2(getPermissionFlags());
    }

    @Override
    protected byte[] createCryptKey(byte[] password) throws COSSecurityException {
        try {
            MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] prepared = prepareBytes(password);
            md.update(prepared);
            md.update(getO());
            md.update(getPBytes());
            byte[] fd = getPermanentFileID();
            if (fd != null) {
                md.update(fd);
            }
            byte[] key = md.digest();
            int length = 5;
            byte[] result = new byte[length];
            System.arraycopy(key, 0, result, 0, length);
            return result;
        } catch (Exception e) {
            throw new COSSecurityException(e);
        }
    }

    @Override
    protected byte[] createOwnerPassword(byte[] owner, byte[] user) throws COSSecurityException {
        try {
            byte[] preparedOwner;
            if (owner == null) {
                preparedOwner = prepareBytes(user);
            } else {
                preparedOwner = prepareBytes(owner);
            }
            MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            md.update(preparedOwner);
            byte[] key = md.digest();
            int length = 5;
            byte[] encryptionKey = new byte[length];
            System.arraycopy(key, 0, encryptionKey, 0, length);
            SecretKey skeySpec = new SecretKeySpec(encryptionKey, KEY_ALGORITHM);
            byte[] preparedUser = prepareBytes(user);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(preparedUser);
            return encrypted;
        } catch (Exception e) {
            throw new COSSecurityException(e);
        }
    }

    @Override
    protected byte[] createUserPassword(byte[] user) throws COSSecurityException {
        try {
            byte[] encryptionKey = createCryptKey(user);
            SecretKey skeySpec = new SecretKeySpec(encryptionKey, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(PADDING);
            return encrypted;
        } catch (Exception e) {
            throw new COSSecurityException(e);
        }
    }

    @Override
    public int getRevision() {
        return 2;
    }
}
