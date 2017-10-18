package org.jcryptool.crypto.keystore.ui.actions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import javax.crypto.SecretKey;
import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.jcryptool.core.operations.util.ByteArrayUtils;
import org.jcryptool.crypto.certificates.CertFact;
import org.jcryptool.crypto.keys.KeyType;
import org.jcryptool.crypto.keystore.KeyStorePlugin;
import org.jcryptool.crypto.keystore.backend.KeyStoreAlias;
import org.jcryptool.crypto.keystore.backend.KeyStoreManager;
import org.jcryptool.crypto.keystore.descriptors.interfaces.INewEntryDescriptor;

/**
 * Abstract (and empty) top-level action for plug-in.
 * 
 * @author tkern
 *
 */
public abstract class AbstractKeyStoreAction extends Action {

    private static final Logger logger = KeyStorePlugin.getLogManager().getLogger(AbstractKeyStoreAction.class.getName());

    protected void addCertificate(INewEntryDescriptor descriptor, Certificate certificate) {
        KeyStoreManager.getInstance().addCertificate(certificate, new KeyStoreAlias(descriptor.getContactName(), KeyType.PUBLICKEY, certificate.getPublicKey().getAlgorithm(), descriptor.getKeyLength(), ByteArrayUtils.toHexString(getHashValue(descriptor)), certificate.getPublicKey().getClass().getName()));
    }

    protected void addSecretKey(INewEntryDescriptor descriptor, SecretKey key) {
        logger.debug("adding SecretKey");
        KeyStoreManager.getInstance().addSecretKey(key, descriptor.getPassword(), new KeyStoreAlias(descriptor.getContactName(), KeyType.SECRETKEY, descriptor.getDisplayedName(), (key.getEncoded().length * 8), ByteArrayUtils.toHexString(getHashValue(descriptor)), key.getClass().getName()));
    }

    protected void addKeyPair(INewEntryDescriptor descriptor, PrivateKey privateKey, PublicKey publicKey) {
        KeyStoreAlias privateAlias = new KeyStoreAlias(descriptor.getContactName(), KeyType.KEYPAIR_PRIVATE_KEY, descriptor.getDisplayedName(), descriptor.getKeyLength(), ByteArrayUtils.toHexString(getHashValue(descriptor)), privateKey.getClass().getName());
        KeyStoreAlias publicAlias = new KeyStoreAlias(descriptor.getContactName(), KeyType.KEYPAIR_PUBLIC_KEY, descriptor.getDisplayedName(), descriptor.getKeyLength(), ByteArrayUtils.toHexString(getHashValue(descriptor)), publicKey.getClass().getName());
        X509Certificate dummy = CertFact.getDummyCertificate(publicKey);
        KeyStoreManager.getInstance().addKeyPair(privateKey, dummy, descriptor.getPassword(), privateAlias, publicAlias);
    }

    private static byte[] getHashValue(INewEntryDescriptor descriptor) {
        String timeStamp = Calendar.getInstance().getTime().toString();
        MessageDigest sha1;
        byte[] digest = { 0 };
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(descriptor.getContactName().getBytes());
            sha1.update(descriptor.getAlgorithmName().getBytes());
            sha1.update(descriptor.getProvider().getBytes());
            return digest = sha1.digest(timeStamp.getBytes());
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException while digesting", e);
        }
        return digest;
    }
}
