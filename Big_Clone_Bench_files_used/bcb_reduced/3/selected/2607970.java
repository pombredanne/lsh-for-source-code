package gnu.javax.crypto.jce.mac;

import gnu.javax.crypto.mac.IMac;
import gnu.javax.crypto.mac.MacFactory;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.MacSpi;

/**
 * The implementation of a generic {@link javax.crypto.Mac} adapter class to
 * wrap GNU MAC instances.
 * <p>
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>) for
 * the {@link javax.crypto.Mac} class, which provides the functionality of a
 * message authentication code algorithm, such as the <i>Hashed Message
 * Authentication Code</i> (<b>HMAC</b>) algorithms.
 */
class MacAdapter extends MacSpi implements Cloneable {

    /** Our MAC instance. */
    protected IMac mac;

    /** Our MAC attributes. */
    protected Map attributes;

    /**
   * Creates a new Mac instance for the given name.
   * 
   * @param name The name of the mac to create.
   */
    protected MacAdapter(String name) {
        mac = MacFactory.getInstance(name);
        attributes = new HashMap();
    }

    /**
   * Private constructor for cloning purposes.
   * 
   * @param mac a clone of the internal {@link IMac} instance.
   * @param attributes a clone of the current {@link Map} of attributes.
   */
    private MacAdapter(IMac mac, Map attributes) {
        super();
        this.mac = mac;
        this.attributes = attributes;
    }

    public Object clone() throws CloneNotSupportedException {
        return new MacAdapter((IMac) mac.clone(), new HashMap(attributes));
    }

    protected byte[] engineDoFinal() {
        byte[] result = mac.digest();
        engineReset();
        return result;
    }

    protected int engineGetMacLength() {
        return mac.macSize();
    }

    protected void engineInit(Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (!key.getFormat().equalsIgnoreCase("RAW")) throw new InvalidKeyException("unknown key format " + key.getFormat());
        attributes.put(IMac.MAC_KEY_MATERIAL, key.getEncoded());
        mac.reset();
        mac.init(attributes);
    }

    protected void engineReset() {
        mac.reset();
    }

    protected void engineUpdate(byte b) {
        mac.update(b);
    }

    protected void engineUpdate(byte[] in, int off, int len) {
        mac.update(in, off, len);
    }
}
