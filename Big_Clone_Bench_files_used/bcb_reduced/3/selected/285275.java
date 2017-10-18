package de.itar.logic.xml.crypt;

import hm.core.utils.ExceptionHelper;
import hm.core.utils.Base64.Base64;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.jdom.Element;
import de.itar.exception.ITarException;
import de.itar.logic.xml.HjDescXmlImpl;
import de.itar.logic.xml.MediumDescXmlImpl;
import de.itar.logic.xml.crypt.exception.NoConfirmAttributeException;
import de.itar.resources.Constants;

public class CryptHjDesc extends HjDescXmlImpl {

    protected static final String ATT_CONFIRM = "confirm";

    public static final int STATE_UNINIT = 0;

    public static final int STATE_INIT = 1;

    public static final int STATE_BAD_KEY = 2;

    public static final int STATE_WRONG_KEY = 3;

    public static final int VERSUCHE = 3;

    private int state;

    private int countWrongKey;

    private Cipher cipherEnc;

    private Cipher cipherDec;

    public CryptHjDesc(String name, String medium, String[] attributes, char[] password) throws Exception {
        super(name, medium, attributes, DATA_SUFFIX_CRYPT);
        countWrongKey = 0;
        state = STATE_UNINIT;
        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] key = new String(password).getBytes();
        md.update(key);
        byte[] confirmAttribute = md.digest();
        String confAttB64 = Base64.encodeBytes(confirmAttribute);
        Element root = m_document.getRootElement();
        root.setAttribute(ATT_CONFIRM, confAttB64);
        save();
        state = STATE_INIT;
        try {
            Key k = new SecretKeySpec(key, "DES");
            cipherEnc = Cipher.getInstance("DES");
            cipherEnc.init(Cipher.ENCRYPT_MODE, k);
            cipherDec = Cipher.getInstance("DES");
            cipherDec.init(Cipher.DECRYPT_MODE, k);
        } catch (InvalidKeyException e) {
            throw new ITarException(Constants.CREATE_HJ_PASS_CONFORM);
        }
    }

    public CryptHjDesc(String inputFile, boolean validation) throws Exception {
        super(inputFile, validation);
        state = STATE_UNINIT;
    }

    public int getBookCount() {
        if (state != STATE_INIT) {
            return 0;
        }
        return super.getBookCount();
    }

    public boolean isCryptDesc() {
        return true;
    }

    public Cipher getCipherDec() throws Exception {
        return cipherDec;
    }

    public Cipher getCipherEnc() throws Exception {
        return cipherEnc;
    }

    public int getState() {
        return state;
    }

    public int getCountWrongKey() {
        return countWrongKey;
    }

    public int init(byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, NoConfirmAttributeException {
        try {
            if (state == STATE_INIT) {
                return STATE_INIT;
            }
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(key);
            byte[] confirmAttribute = md.digest();
            String confAttB64 = Base64.encodeBytes(confirmAttribute);
            String pruefsumme = m_document.getRootElement().getAttributeValue(ATT_CONFIRM);
            if (pruefsumme == null) {
                throw new NoConfirmAttributeException("No \"" + ATT_CONFIRM + "\"-Attribute");
            }
            if (!pruefsumme.equals(confAttB64)) {
                state = STATE_WRONG_KEY;
                ++countWrongKey;
                return state;
            }
            Key k = new SecretKeySpec(key, "DES");
            cipherEnc = Cipher.getInstance("DES");
            cipherEnc.init(Cipher.ENCRYPT_MODE, k);
            cipherDec = Cipher.getInstance("DES");
            cipherDec.init(Cipher.DECRYPT_MODE, k);
            state = STATE_INIT;
            return state;
        } catch (InvalidKeyException e) {
            state = STATE_UNINIT;
            return state;
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (NoSuchPaddingException e) {
            throw e;
        }
    }

    public MediumDescXmlImpl getBuchDesc(int index) {
        try {
            if ((index >= 0) && (index < getBookCount())) {
                return new CryptoMediumDesc((Element) m_element.getChildren().get(index), this, index);
            } else {
                return null;
            }
        } catch (Exception ex) {
            ExceptionHelper.showErrorDialog(ex);
            return null;
        }
    }

    public String getValue(int buchIndex, int attributeIndex) {
        try {
            MediumDescXmlImpl bd = new CryptoMediumDesc((Element) m_element.getChildren().get(buchIndex), this, buchIndex);
            return bd.getAttributeValue((String) this.m_attributes.get(attributeIndex));
        } catch (Throwable t) {
            t.printStackTrace();
            return "Crypto Error";
        }
    }

    @SuppressWarnings(value = "unchecked")
    public MediumDescXmlImpl createBuchDesc(String titel) {
        try {
            Element buch = new Element(MEDIUM);
            CryptoMediumDesc buchDesc = new CryptoMediumDesc(buch, this, getBookCount());
            buchDesc.createChildren();
            this.m_element.getChildren().add(buch);
            return buchDesc;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
