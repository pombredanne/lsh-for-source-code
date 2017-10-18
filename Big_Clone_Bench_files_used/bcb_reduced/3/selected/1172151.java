package name.emu.webapp.kos.service.trusted;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import name.emu.webapp.kos.dao.EncryptionParametersDao;
import name.emu.webapp.kos.dao.SecretAccountDao;
import name.emu.webapp.kos.domain.AccessKey;
import name.emu.webapp.kos.domain.EncryptionParameters;
import name.emu.webapp.kos.domain.KosGroup;
import name.emu.webapp.kos.domain.KosSession;
import name.emu.webapp.kos.domain.KosUser;
import name.emu.webapp.kos.domain.Permission;
import name.emu.webapp.kos.domain.SecretAccount;
import name.emu.webapp.kos.domain.SecuredObject;
import name.emu.webapp.kos.domain.SecurityEntity;
import name.emu.webapp.kos.service.data.AccountSortSpec;
import name.emu.webapp.kos.service.data.AccountWithPassword;
import name.emu.webapp.kos.service.data.AccountWithoutPassword;
import name.emu.webapp.kos.service.data.AuthData;
import name.emu.webapp.kos.service.data.DecryptedAccount;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class EncryptionService extends AbstractService {

    public static final String DEFAULT_ASYMMETRIC_CIPHER = "RSA";

    public static final String DEFAULT_SYMMETRIC_CIPHER = "AES";

    public static final int DEFAULT_ASYMMETRIC_KEYSIZE = 1024;

    public static final int DEFAULT_SYMMETRIC_KEYSIZE = 128;

    public static final int SALT_BYTES = 256;

    private SecureRandom realRandom = SecureRandom.getInstance("SHA1PRNG");

    private ExecutorService decryptionExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(), 1, TimeUnit.HOURS, new LinkedBlockingQueue<Runnable>());

    public EncryptionService() throws NoSuchAlgorithmException {
    }

    public KeyPair generateRandomAsymmetricKeys(EncryptionParameters params) throws NoSuchAlgorithmException, NoSuchPaddingException {
        KeyPairGenerator keyGen;
        KeyPair keyPair;
        Cipher cipher = Cipher.getInstance(params.getAsymmetricCipher());
        int keysize = params.getAsymmetricKeySize();
        String algorithm = cipher.getAlgorithm();
        keyGen = KeyPairGenerator.getInstance(algorithm);
        keyGen.initialize(keysize, realRandom);
        keyPair = keyGen.generateKeyPair();
        return keyPair;
    }

    public KeyPair generateKeysFromPassword(String password, byte[] salt, EncryptionParameters params) throws NoSuchAlgorithmException, NoSuchPaddingException {
        MessageDigest m;
        byte[] pwdData;
        byte[] md5Data;
        SecureRandom pseudoRandom;
        KeyPairGenerator keyGen;
        KeyPair keyPair;
        Cipher cipher = Cipher.getInstance(params.getAsymmetricCipher());
        int keysize = params.getAsymmetricKeySize();
        String algorithm = cipher.getAlgorithm();
        try {
            pwdData = password.getBytes(AdminService.CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        m = MessageDigest.getInstance("MD5");
        m.update(xor(pwdData, salt));
        md5Data = m.digest();
        pseudoRandom = SecureRandom.getInstance("SHA1PRNG");
        pseudoRandom.setSeed(md5Data);
        keyGen = KeyPairGenerator.getInstance(algorithm);
        keyGen.initialize(keysize, pseudoRandom);
        keyPair = keyGen.generateKeyPair();
        return keyPair;
    }

    private byte[] xor(byte[] data1, byte[] data2) {
        byte[] result;
        byte[] rest;
        int i;
        if (data1 == null) {
            result = data2;
        } else if (data2 == null) {
            result = data1;
        } else {
            result = new byte[Math.max(data1.length, data2.length)];
            for (i = 0; i < Math.min(data1.length, data2.length); i++) {
                result[i] = (byte) (data1[i] ^ data2[i]);
            }
            if (data1.length > data2.length) {
                rest = data1;
            } else {
                rest = data2;
            }
            for (; i < rest.length; i++) {
                result[i] = rest[i];
            }
        }
        return result;
    }

    protected KeyPair changeUserPassword(KosUser user, KeyPair oldKeyPair, String newPassword) {
        KeyPair newKeyPair;
        try {
            EncryptionParameters newEncryptionParameters = getActiveEncryptionParameters();
            byte[] newSalt = getServiceRegistry().getEncryptionService().generateSalt();
            newKeyPair = generateKeysFromPassword(newPassword, newSalt, newEncryptionParameters);
            for (AccessKey accessKey : user.getAccessKeys()) {
                byte[] plainText = decrypt(accessKey.getEncryptedSecretKey(), user.getParameters().getAsymmetricCipher(), oldKeyPair.getPrivate());
                accessKey.setEncryptedSecretKey(encrypt(plainText, Cipher.getInstance(newEncryptionParameters.getAsymmetricCipher()), newKeyPair.getPublic()));
                getServiceRegistry().getDaoRegistry().getAccessKeyDao().save(accessKey);
            }
            user.setSalt(newSalt);
            user.setPublicKey(newKeyPair.getPublic().getEncoded());
            user.setParameters(newEncryptionParameters);
            getServiceRegistry().getDaoRegistry().getUserDao().save(user);
            for (KosSession session : user.getActiveSessions()) {
                getServiceRegistry().getDaoRegistry().getSessionDao().delete(session);
            }
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return newKeyPair;
    }

    public SecretKey generateRandomSymmetricKey(EncryptionParameters params) {
        SecretKey key;
        try {
            String cipherName = params.getSymmetricCipher();
            KeyGenerator keyGen = KeyGenerator.getInstance(cipherName);
            keyGen.init(params.getSymmetricKeySize(), realRandom);
            key = keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return key;
    }

    /**
     * Get the currently active set of encryption parameters.
     */
    public EncryptionParameters getActiveEncryptionParameters() {
        EncryptionParameters encryptionParameters;
        EncryptionParametersDao dao = getServiceRegistry().getDaoRegistry().getEncryptionParametersDao();
        encryptionParameters = dao.findActiveEncryptionParameters();
        return encryptionParameters;
    }

    /**
     * Persist changes to the secret account.
     * @param account
     * @param user
     * @param userKeyPair
     */
    public void updateSecretAccount(AccountWithPassword account, KosUser user, KeyPair userKeyPair) {
        long secretAccountId = account.getSecretAccountId();
        SecretAccount secretAccount = getServiceRegistry().getDaoRegistry().getSecretAccountDao().findById(secretAccountId);
        AccessKeyChain accessKeyChain = this.findAccessKeyChainFor(user, secretAccount);
        Key secretKey;
        Cipher symmetricCipher;
        Date now;
        EncryptionParameters encryptionParameters = secretAccount.getParameters();
        if (!accessKeyChain.getPermission().equals(Permission.FULL)) {
            throw new SecurityException("User \"" + user.getUserName() + "\" is not allowed to modify secret account with id " + secretAccountId);
        }
        try {
            String cipherName = encryptionParameters.getSymmetricCipher();
            symmetricCipher = Cipher.getInstance(cipherName);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        try {
            now = new Date();
            secretKey = decryptFinalTargetKey(user, userKeyPair.getPrivate(), accessKeyChain);
            secretAccount.setLastModified(now);
            secretAccount.setEncryptedSystem(encrypt(account.getSystem(), symmetricCipher, secretKey));
            secretAccount.setEncryptedUsername(encrypt(account.getUserName(), symmetricCipher, secretKey));
            secretAccount.setEncryptedPassword(encrypt(account.getPassword(), symmetricCipher, secretKey));
            secretAccount.setEncryptedComment(encrypt(account.getComment(), symmetricCipher, secretKey));
            getServiceRegistry().getDaoRegistry().getSecretAccountDao().save(secretAccount);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteSecretAccount(DecryptedAccount account, KosUser user) {
        SecretAccountDao dao = getServiceRegistry().getDaoRegistry().getSecretAccountDao();
        SecretAccount secretAccount = dao.findById(account.getSecretAccountId());
        AccessKeyChain accessKeyChain = this.findAccessKeyChainFor(user, secretAccount);
        if (!accessKeyChain.getPermission().equals(Permission.FULL)) {
            throw new SecurityException("User \"" + user.getUserName() + "\" is not allowed to delete secret account with id " + account.getSecretAccountId());
        }
        for (AccessKey key : new ArrayList<AccessKey>(secretAccount.getAccessors())) {
            getServiceRegistry().getDaoRegistry().getAccessKeyDao().delete(key);
        }
        dao.delete(secretAccount);
    }

    /**
     * Creates a new secret account object and stores all confidential data in encrypted form in the database.
     * @param account the secret
     * @param user the user who creates the secret and needs access to it
     * @param userKeyPair the user's secret keypair
     */
    public void storeNewSecretAccount(AccountWithPassword account, KosUser user, PublicKey userPublicKey) {
        EncryptionParameters encryptionParameters = getActiveEncryptionParameters();
        SecretAccount secretAccount = new SecretAccount();
        AccessKey accessKey = new AccessKey();
        KeyGenerator keyGen;
        Key secretKey;
        Cipher asymmetricCipher;
        Cipher symmetricCipher;
        Date now;
        try {
            String cipherName = encryptionParameters.getSymmetricCipher();
            keyGen = KeyGenerator.getInstance(cipherName);
            symmetricCipher = Cipher.getInstance(cipherName);
            asymmetricCipher = Cipher.getInstance(user.getParameters().getAsymmetricCipher());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        keyGen.init(encryptionParameters.getSymmetricKeySize(), realRandom);
        secretKey = keyGen.generateKey();
        try {
            now = new Date();
            secretAccount.setLastModified(now);
            secretAccount.setParameters(encryptionParameters);
            secretAccount.setEncryptedSystem(encrypt(account.getSystem(), symmetricCipher, secretKey));
            secretAccount.setEncryptedUsername(encrypt(account.getUserName(), symmetricCipher, secretKey));
            secretAccount.setEncryptedPassword(encrypt(account.getPassword(), symmetricCipher, secretKey));
            secretAccount.setEncryptedComment(encrypt(account.getComment(), symmetricCipher, secretKey));
            accessKey.setOwner(user);
            accessKey.setPermission(Permission.FULL);
            accessKey.setTarget(secretAccount);
            accessKey.setEncryptedSecretKey(encrypt(secretKey.getEncoded(), asymmetricCipher, userPublicKey));
            getServiceRegistry().getDaoRegistry().getSecretAccountDao().save(secretAccount);
            getServiceRegistry().getDaoRegistry().getAccessKeyDao().save(accessKey);
            account.setSecretAccountId(secretAccount.getId());
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Counts the number of secrets accounts accessible by this user.
     * @param user
     * @return number of secret accounts
     */
    public int countAccountsFor(KosUser user) {
        return buildAccessKeyChainsFor(user, SecretAccount.class).size();
    }

    public List<AccountWithoutPassword> getDecryptedAccountsFor(KosUser user, KeyPair userKeyPair) {
        return getDecryptedAccountsFor(user, userKeyPair, null);
    }

    /**
     * Retrieves a list of decrypted accounts (without password) accessible by the given user.  
     * @param user
     * @param userKeyPair
     * @param sortSpec
     * @return
     */
    public List<AccountWithoutPassword> getDecryptedAccountsFor(KosUser user, KeyPair userKeyPair, AccountSortSpec sortSpec) {
        List<AccountWithoutPassword> accounts;
        List<AccessKeyChain> accessKeyChains;
        accessKeyChains = buildAccessKeyChainsFor(user, SecretAccount.class);
        accounts = decryptSecretAccountsWithoutPassword(user, userKeyPair.getPrivate(), accessKeyChains);
        if (sortSpec != null && sortSpec.getCriteria() != null) {
            if (sortSpec.getCriteria() == AccountSortSpec.SortCriteria.SYSTEM) {
                Collections.sort(accounts, new Comparator<AccountWithoutPassword>() {

                    @Override
                    public int compare(AccountWithoutPassword account1, AccountWithoutPassword account2) {
                        return account1.getSystem().compareTo(account2.getSystem());
                    }
                });
            } else {
                assert (sortSpec.getCriteria() == AccountSortSpec.SortCriteria.USER_NAME);
                Collections.sort(accounts, new Comparator<AccountWithoutPassword>() {

                    @Override
                    public int compare(AccountWithoutPassword account1, AccountWithoutPassword account2) {
                        return account1.getUserName().compareTo(account2.getUserName());
                    }
                });
            }
            if (!sortSpec.isAscending()) {
                Collections.reverse(accounts);
            }
        }
        return accounts;
    }

    private List<AccountWithoutPassword> decryptSecretAccountsWithoutPassword(KosUser user, PrivateKey privateKey, List<AccessKeyChain> accessKeyChains) {
        List<AccountWithoutPassword> accounts = new ArrayList<AccountWithoutPassword>(accessKeyChains.size());
        Collection<DecryptSecretAccountWithoutPasswordTask> tasks = new ArrayList<DecryptSecretAccountWithoutPasswordTask>(accessKeyChains.size());
        List<Future<AccountWithoutPassword>> futures;
        for (AccessKeyChain accessKeyChain : accessKeyChains) {
            tasks.add(new DecryptSecretAccountWithoutPasswordTask(user, privateKey, accessKeyChain));
        }
        try {
            futures = decryptionExecutor.invokeAll(tasks);
            for (Future<AccountWithoutPassword> future : futures) {
                accounts.add(future.get());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Decryption task interrupted.", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Decryption task exception.", e);
        }
        return accounts;
    }

    /**
     * Retrieves all account data (including password) in plain-text. This method also updates the access key's secretLastRead property.
     * @throws NoKeyChainAvailableException thrown if no key chain is found, which would allow decryption of given secret 
     */
    public AccountWithPassword getDecryptedAccountFor(KosUser user, KeyPair userKeyPair, long secretAccountId) throws NoKeyChainAvailableException {
        SecretAccount secretAccount;
        AccountWithPassword account;
        secretAccount = getServiceRegistry().getDaoRegistry().getSecretAccountDao().findById(secretAccountId);
        AccessKeyChain accessKeyChain = findAccessKeyChainFor(user, secretAccount);
        if (accessKeyChain == null) {
            throw new NoKeyChainAvailableException();
        }
        account = decryptSecretAccount(user, userKeyPair.getPrivate(), accessKeyChain);
        return account;
    }

    /**
     * Retrieves all account data (excluding the password) in plain-text. 
     */
    public AccountWithoutPassword getDecryptedAccountWithoutPasswordFor(KosUser user, KeyPair userKeyPair, long secretAccountId) {
        SecretAccount secretAccount;
        AccountWithoutPassword account;
        AccessKeyChain accessKeyChain;
        secretAccount = getServiceRegistry().getDaoRegistry().getSecretAccountDao().findById(secretAccountId);
        accessKeyChain = findAccessKeyChainFor(user, secretAccount);
        account = decryptSecretAccountWithoutPassword(user, userKeyPair.getPrivate(), accessKeyChain);
        return account;
    }

    private SecretKey decryptFinalTargetKey(SecurityEntity user, PrivateKey userPrivateKey, AccessKeyChain accessKeyChain) {
        SecurityEntity currentSecurityEntity = user;
        PrivateKey currentPrivateKey = userPrivateKey;
        SecretKey key = null;
        for (AccessKey accessKey : accessKeyChain) {
            byte[] encryptedSecretKey = accessKey.getEncryptedSecretKey();
            byte[] encodedSecretKey = decrypt(encryptedSecretKey, currentSecurityEntity.getParameters().getAsymmetricCipher(), currentPrivateKey);
            key = new SecretKeySpec(encodedSecretKey, accessKey.getTarget().getParameters().getSymmetricCipher());
            if (accessKey.getTarget() instanceof KosGroup) {
                KosGroup targetGroup = (KosGroup) accessKey.getTarget();
                currentPrivateKey = restorePrivateKey(targetGroup.getEncryptedPrivateKey(), key, targetGroup.getParameters());
                currentSecurityEntity = targetGroup;
            } else {
                assert (accessKey.getTarget() instanceof SecretAccount);
            }
        }
        return key;
    }

    private AccountWithoutPassword decryptSecretAccountWithoutPassword(KosUser user, PrivateKey userPrivateKey, AccessKeyChain accessKeyChain) {
        AccountWithoutPassword account = null;
        try {
            SecretKey key = decryptFinalTargetKey(user, userPrivateKey, accessKeyChain);
            SecretAccount secretAccount = (SecretAccount) accessKeyChain.getFinalTarget();
            Cipher cipher;
            account = new AccountWithoutPassword();
            cipher = Cipher.getInstance(secretAccount.getParameters().getSymmetricCipher());
            account.setSecretAccountId(secretAccount.getId());
            account.setSystem(decryptToString(secretAccount.getEncryptedSystem(), cipher, key));
            account.setUserName(decryptToString(secretAccount.getEncryptedUsername(), cipher, key));
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        assert (account != null);
        return account;
    }

    private AccountWithPassword decryptSecretAccount(KosUser user, PrivateKey userPrivateKey, AccessKeyChain accessKeyChain) {
        AccountWithPassword account = null;
        try {
            SecretKey key = decryptFinalTargetKey(user, userPrivateKey, accessKeyChain);
            SecretAccount secretAccount = (SecretAccount) accessKeyChain.getFinalTarget();
            Cipher cipher;
            account = new AccountWithPassword();
            cipher = Cipher.getInstance(secretAccount.getParameters().getSymmetricCipher());
            account.setSecretAccountId(secretAccount.getId());
            account.setSystem(decryptToString(secretAccount.getEncryptedSystem(), cipher, key));
            account.setUserName(decryptToString(secretAccount.getEncryptedUsername(), cipher, key));
            account.setPassword(decryptToString(secretAccount.getEncryptedPassword(), cipher, key));
            account.setComment(decryptToString(secretAccount.getEncryptedComment(), cipher, key));
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        assert (account != null);
        return account;
    }

    private byte[] encrypt(String clearTextStr, Cipher cipher, Key key) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, UnsupportedEncodingException {
        byte[] clearText = null;
        byte[] encodedText;
        if (clearTextStr != null) {
            clearText = clearTextStr.getBytes(AdminService.CHARACTER_ENCODING);
        }
        encodedText = encrypt(clearText, cipher, key);
        return encodedText;
    }

    private byte[] encrypt(byte[] clearText, Cipher cipher, Key key) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, UnsupportedEncodingException {
        byte[] encodedText = null;
        if (clearText != null) {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            encodedText = cipher.doFinal(clearText);
        }
        return encodedText;
    }

    private String decryptToString(byte[] encryptedData, Cipher cipher, Key key) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, UnsupportedEncodingException {
        byte[] decryptedData = decrypt(encryptedData, cipher, key);
        String decryptedStr = null;
        if (decryptedData != null) {
            decryptedStr = new String(decryptedData, AdminService.CHARACTER_ENCODING);
        }
        return decryptedStr;
    }

    private byte[] decrypt(byte[] encryptedData, Cipher cipher, Key key) {
        byte[] result = null;
        try {
            if (encryptedData != null) {
                cipher.init(Cipher.DECRYPT_MODE, key);
                result = cipher.doFinal(encryptedData);
            }
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private byte[] decrypt(byte[] encryptedData, String cipherName, Key key) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(cipherName);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return decrypt(encryptedData, cipher, key);
    }

    public byte[] encryptPrivateKey(PrivateKey privateKey, SecretKey sessionKey, EncryptionParameters encryptionParams) {
        byte[] privateKeyDat = privateKey.getEncoded();
        byte[] encryptedPrivateKey;
        try {
            encryptedPrivateKey = encrypt(privateKeyDat, Cipher.getInstance(encryptionParams.getSymmetricCipher()), sessionKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return encryptedPrivateKey;
    }

    private PublicKey restorePublicKey(byte[] publicKeyDat, EncryptionParameters parameters) {
        KeyFactory keyFactory;
        PublicKey publicKey;
        X509EncodedKeySpec publicKeySpec;
        try {
            keyFactory = KeyFactory.getInstance(parameters.getAsymmetricCipher());
            publicKeySpec = new X509EncodedKeySpec(publicKeyDat);
            publicKey = keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        return publicKey;
    }

    private PrivateKey restorePrivateKey(byte[] encryptedPrivateKey, SecretKey secretKey, EncryptionParameters parameters) {
        PrivateKey privateKey;
        byte[] privateKeyDat;
        PKCS8EncodedKeySpec privateKeySpec;
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(parameters.getAsymmetricCipher());
            privateKeyDat = decrypt(encryptedPrivateKey, Cipher.getInstance(parameters.getSymmetricCipher()), secretKey);
            privateKeySpec = new PKCS8EncodedKeySpec(privateKeyDat);
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        return privateKey;
    }

    public KeyPair restoreKeyPair(AuthData authData) {
        return restoreKeyPair(authData.getSession().getEncryptedPrivateKey(), authData.getSessionKey(), authData.getSession().getParameters(), authData.getUser().getPublicKey());
    }

    public KeyPair restoreKeyPair(byte[] encryptedPrivateKey, SecretKey sessionKey, EncryptionParameters parameters, byte[] publicKeyDat) {
        KeyPair keyPair;
        PrivateKey privateKey;
        PublicKey publicKey;
        privateKey = restorePrivateKey(encryptedPrivateKey, sessionKey, parameters);
        publicKey = restorePublicKey(publicKeyDat, parameters);
        keyPair = new KeyPair(publicKey, privateKey);
        return keyPair;
    }

    public SecretAccount getSecretAccount(Long id) {
        return getServiceRegistry().getDaoRegistry().getSecretAccountDao().findById(id);
    }

    public AccessKey generateGroupAccessKey(PrivateKey groupPrivateKey, KosGroup group, KosUser user, Permission permission) {
        EncryptionParameters accessKeyEncryptionParameters = getActiveEncryptionParameters();
        AccessKey accessKey = new AccessKey();
        KeyGenerator keyGen;
        Key secretAccessKey;
        Cipher userCipher;
        Cipher accessKeyCipher;
        try {
            String cipherName = accessKeyEncryptionParameters.getSymmetricCipher();
            keyGen = KeyGenerator.getInstance(cipherName);
            accessKeyCipher = Cipher.getInstance(cipherName);
            userCipher = Cipher.getInstance(user.getParameters().getAsymmetricCipher());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        keyGen.init(accessKeyEncryptionParameters.getSymmetricKeySize(), realRandom);
        secretAccessKey = keyGen.generateKey();
        try {
            group.setEncryptedPrivateKey(encrypt(groupPrivateKey.getEncoded(), accessKeyCipher, secretAccessKey));
            accessKey.setOwner(user);
            accessKey.setPermission(permission);
            accessKey.setTarget(group);
            accessKey.setEncryptedSecretKey(encrypt(secretAccessKey.getEncoded(), userCipher, restorePublicKey(user.getPublicKey(), user.getParameters())));
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return accessKey;
    }

    private void shareSecuredObject(SecuredObject securedObject, SecretKey secretKey, SecurityEntity recipient, Permission permission) {
        AccessKey newAccessKey = new AccessKey();
        Cipher recipientCipher;
        if (getServiceRegistry().getDaoRegistry().getAccessKeyDao().existsFor(recipient, securedObject)) {
            throw new IntegrityException("Given security entity already has an access key for the secured object. Creation of another key is not allowed.");
        }
        try {
            recipientCipher = Cipher.getInstance(recipient.getParameters().getAsymmetricCipher());
            newAccessKey.setOwner(recipient);
            newAccessKey.setPermission(permission);
            newAccessKey.setTarget(securedObject);
            newAccessKey.setEncryptedSecretKey(encrypt(secretKey.getEncoded(), recipientCipher, restorePublicKey(recipient.getPublicKey(), recipient.getParameters())));
            getServiceRegistry().getDaoRegistry().getAccessKeyDao().save(newAccessKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private void unshareSecuredObject(SecuredObject securedObject, SecurityEntity holder) {
        AccessKey lastAccessKey;
        AccessKeyChain accessKeyChain = findAccessKeyChainFor(holder, securedObject);
        if (accessKeyChain.getKeyList().size() > 1) {
            throw new SecurityException("Cannot unshare indirect access to secured object.");
        }
        lastAccessKey = accessKeyChain.getKeyList().get(0);
        getServiceRegistry().getDaoRegistry().getAccessKeyDao().delete(lastAccessKey);
    }

    public void unshareSecretAccount(long secretAccountId, KosUser controllingUser, KeyPair controllingUserKeyPair, SecurityEntity secretHolder) {
        SecretAccount secretAccount = getServiceRegistry().getDaoRegistry().getSecretAccountDao().findById(secretAccountId);
        AccessKeyChain controllingUserAccessKeyChain = findAccessKeyChainFor(controllingUser, secretAccount);
        AccessKeyChain accessKeyChain = findAccessKeyChainFor(secretHolder, secretAccount);
        if (Permission.FULL != controllingUserAccessKeyChain.getPermission() && Permission.FULL == accessKeyChain.getPermission()) {
            throw new SecurityException("Granting user cannot give more rights on secret account than owned.");
        }
        unshareSecuredObject(secretAccount, secretHolder);
    }

    public void shareSecretAccount(long secretAccountId, KosUser grantingUser, KeyPair grantingUserKeyPair, SecurityEntity recipient, Permission permission) {
        SecretAccount secretAccount = getServiceRegistry().getDaoRegistry().getSecretAccountDao().findById(secretAccountId);
        AccessKeyChain accessKeyChain = findAccessKeyChainFor(grantingUser, secretAccount);
        SecretKey secretKey;
        if (Permission.FULL != accessKeyChain.getPermission() && Permission.FULL == permission) {
            throw new SecurityException("Granting user cannot give more rights on secret account than owned.");
        }
        secretKey = decryptFinalTargetKey(grantingUser, grantingUserKeyPair.getPrivate(), accessKeyChain);
        shareSecuredObject(secretAccount, secretKey, recipient, permission);
    }

    public void unshareGroupMembership(long groupId, KosUser controllingUser, KeyPair controllingUserKeyPair, SecurityEntity secretHolder) {
        KosGroup kosGroup = getServiceRegistry().getDaoRegistry().getGroupDao().findById(groupId);
        AccessKeyChain controllingUserAccessKeyChain = findAccessKeyChainFor(controllingUser, kosGroup);
        AccessKeyChain accessKeyChain = findAccessKeyChainFor(secretHolder, kosGroup);
        if (Permission.FULL != controllingUserAccessKeyChain.getPermission() && Permission.FULL == accessKeyChain.getPermission()) {
            throw new SecurityException("Granting user cannot give more rights on secret account than owned.");
        }
        unshareSecuredObject(kosGroup, secretHolder);
    }

    public void shareGroupMembership(long groupId, KosUser grantingUser, KeyPair grantingUserKeyPair, SecurityEntity recipient, Permission permission) {
        KosGroup kosGroup = getServiceRegistry().getDaoRegistry().getGroupDao().findById(groupId);
        AccessKeyChain accessKeyChain = findAccessKeyChainFor(grantingUser, kosGroup);
        SecretKey secretKey;
        if (Permission.FULL != accessKeyChain.getPermission() && Permission.FULL == permission) {
            throw new SecurityException("Granting user cannot give more rights on group than owned.");
        }
        if (getServiceRegistry().getAdminService().getAllGroupsFor(kosGroup).contains(recipient)) {
            throw new IntegrityException("Cyclic group membership relations are not allowed.");
        }
        secretKey = decryptFinalTargetKey(grantingUser, grantingUserKeyPair.getPrivate(), accessKeyChain);
        shareSecuredObject(kosGroup, secretKey, recipient, permission);
    }

    public byte[] generateSalt() {
        byte salt[] = new byte[SALT_BYTES];
        realRandom.nextBytes(salt);
        return salt;
    }

    private AccessKeyChain findAccessKeyChainFor(SecurityEntity source, SecuredObject target) {
        List<AccessKeyChain> chains = buildAccessKeyChainsFor(source, target.getClass());
        AccessKeyChain accessKeyChain = null;
        Iterator<AccessKeyChain> iter;
        iter = chains.iterator();
        while (accessKeyChain == null && iter.hasNext()) {
            AccessKeyChain currentChain = iter.next();
            if (currentChain.getFinalTarget().equals(target)) {
                accessKeyChain = currentChain;
            }
        }
        return accessKeyChain;
    }

    /**
     * Returns key chains available for the given security entity. 
     * If there are several possible chains leading to the same secured objects, the one
     * with the highest permission level is kept. If there are still several chains left, all but one
     * of these are removed. 
     */
    private List<AccessKeyChain> buildAccessKeyChainsFor(SecurityEntity securityEntity, Class<? extends SecuredObject> targetClass) {
        List<AccessKeyChain> accessKeyChains;
        AccessKeyChain emptyKeyChain = new AccessKeyChain();
        Map<SecuredObject, AccessKeyChain> securedObjectKeyChain = new HashMap<SecuredObject, AccessKeyChain>();
        accessKeyChains = buildAccessKeyChainsFor(emptyKeyChain, securityEntity, targetClass);
        for (AccessKeyChain accessKeyChain : accessKeyChains) {
            SecuredObject securedObject = accessKeyChain.getFinalTarget();
            if (!securedObjectKeyChain.containsKey(securedObject) || Permission.READ_ONLY.equals(securedObjectKeyChain.get(securedObject).getPermission())) {
                securedObjectKeyChain.put(securedObject, accessKeyChain);
            }
        }
        accessKeyChains = new ArrayList<AccessKeyChain>(securedObjectKeyChain.values());
        return accessKeyChains;
    }

    /**
     * Recursively builds all the key chains for the given security entity.
     * @param origKeyChain key chain build thus far into the recursion
     * @param securityEntity entity of the current node in the key chain
     * @Param targetClass only key chain targets of this class will be kept (disable this filter by passing null)
     * @return unfiltered list of all buildable key chains
     */
    private List<AccessKeyChain> buildAccessKeyChainsFor(AccessKeyChain origKeyChain, SecurityEntity securityEntity, Class<? extends SecuredObject> targetClass) {
        List<AccessKeyChain> accessKeyChains = new ArrayList<AccessKeyChain>();
        Collection<AccessKey> accessKeys = securityEntity.getAccessKeys();
        for (AccessKey accessKey : accessKeys) {
            AccessKeyChain newKeyChain = origKeyChain.cloneAndAdd(accessKey);
            if (targetClass == null || targetClass.isAssignableFrom(accessKey.getTarget().getClass())) {
                accessKeyChains.add(newKeyChain);
            }
            if (accessKey.getTarget() instanceof KosGroup) {
                accessKeyChains.addAll(buildAccessKeyChainsFor(newKeyChain, (SecurityEntity) accessKey.getTarget(), targetClass));
            }
        }
        return accessKeyChains;
    }

    private class AccessKeyChain implements Iterable<AccessKey> {

        private List<AccessKey> accessKeys;

        public AccessKeyChain() {
            accessKeys = new ArrayList<AccessKey>();
        }

        private AccessKeyChain(AccessKeyChain original, AccessKey nextKey) {
            accessKeys = new ArrayList<AccessKey>(original.accessKeys);
            accessKeys.add(nextKey);
        }

        public AccessKeyChain cloneAndAdd(AccessKey nextKey) {
            return new AccessKeyChain(this, nextKey);
        }

        @Override
        public Iterator<AccessKey> iterator() {
            return accessKeys.iterator();
        }

        public List<AccessKey> getKeyList() {
            return new ArrayList<AccessKey>(accessKeys);
        }

        /**
         * Full permissions are only granted, if all access key objects in the chain come with permission full.
         * Otherwise only read-only permission is given by the chain.
         */
        public Permission getPermission() {
            Permission permission = Permission.FULL;
            Iterator<AccessKey> iter = iterator();
            while (Permission.FULL.equals(permission) && iter.hasNext()) {
                permission = iter.next().getPermission();
            }
            return permission;
        }

        public SecuredObject getFinalTarget() {
            SecuredObject finalTarget = null;
            if (accessKeys.size() > 0) {
                finalTarget = accessKeys.get(accessKeys.size() - 1).getTarget();
            }
            return finalTarget;
        }
    }

    /**
     * Callable task class used for concurrent decryption in password list.
     */
    private class DecryptSecretAccountWithoutPasswordTask implements Callable<AccountWithoutPassword> {

        private KosUser user;

        private PrivateKey privateKey;

        private AccessKeyChain accessKeyChain;

        private DecryptSecretAccountWithoutPasswordTask(KosUser user, PrivateKey privateKey, AccessKeyChain accessKeyChain) {
            this.user = user;
            this.privateKey = privateKey;
            this.accessKeyChain = accessKeyChain;
        }

        @Override
        public AccountWithoutPassword call() throws Exception {
            return EncryptionService.this.decryptSecretAccountWithoutPassword(user, privateKey, accessKeyChain);
        }
    }
}
