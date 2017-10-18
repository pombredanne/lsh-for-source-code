package gnu.javax.crypto.kwa;

import gnu.java.security.Registry;
import gnu.java.security.hash.Sha160;
import gnu.javax.crypto.assembly.Assembly;
import gnu.javax.crypto.assembly.Cascade;
import gnu.javax.crypto.assembly.Direction;
import gnu.javax.crypto.assembly.Stage;
import gnu.javax.crypto.assembly.Transformer;
import gnu.javax.crypto.assembly.TransformerException;
import gnu.javax.crypto.cipher.IBlockCipher;
import gnu.javax.crypto.cipher.TripleDES;
import gnu.javax.crypto.mode.IMode;
import gnu.javax.crypto.mode.ModeFactory;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The GNU implementation of the Triple DES Key Wrap Algorithm as described in
 * [1].
 * <p>
 * <b>IMPORTANT</b>: This class is NOT thread safe.
 * <p>
 * References:
 * <ol>
 * <li><a href="http://www.rfc-archive.org/getrfc.php?rfc=3217">Triple-DES and
 * RC2 Key Wrapping</a>.</li>
 * <li><a href="http://www.w3.org/TR/xmlenc-core/">XML Encryption Syntax and
 * Processing</a>.</li>
 * </ol>
 */
public class TripleDESKeyWrap extends BaseKeyWrappingAlgorithm {

    private static final byte[] DEFAULT_IV = new byte[] { (byte) 0x4A, (byte) 0xDD, (byte) 0xA2, (byte) 0x2C, (byte) 0x79, (byte) 0xE8, (byte) 0x21, (byte) 0x05 };

    private Assembly asm;

    private HashMap asmAttributes = new HashMap();

    private HashMap modeAttributes = new HashMap();

    private Sha160 sha = new Sha160();

    private SecureRandom rnd;

    public TripleDESKeyWrap() {
        super(Registry.TRIPLEDES_KWA);
    }

    protected void engineInit(Map attributes) throws InvalidKeyException {
        rnd = (SecureRandom) attributes.get(IKeyWrappingAlgorithm.SOURCE_OF_RANDOMNESS);
        IMode des3CBC = ModeFactory.getInstance(Registry.CBC_MODE, new TripleDES(), 8);
        Stage des3CBCStage = Stage.getInstance(des3CBC, Direction.FORWARD);
        Cascade cascade = new Cascade();
        Object modeNdx = cascade.append(des3CBCStage);
        asmAttributes.put(modeNdx, modeAttributes);
        asm = new Assembly();
        asm.addPreTransformer(Transformer.getCascadeTransformer(cascade));
        modeAttributes.put(IBlockCipher.KEY_MATERIAL, attributes.get(KEY_ENCRYPTION_KEY_MATERIAL));
        asmAttributes.put(Assembly.DIRECTION, Direction.FORWARD);
    }

    protected byte[] engineWrap(byte[] in, int inOffset, int length) {
        if (length != 16 && length != 24) throw new IllegalArgumentException("Only 2- and 3-key Triple DES keys are alowed");
        byte[] CEK = new byte[24];
        if (length == 16) {
            System.arraycopy(in, inOffset, CEK, 0, 16);
            System.arraycopy(in, inOffset, CEK, 16, 8);
        } else System.arraycopy(in, inOffset, CEK, 0, 24);
        TripleDES.adjustParity(CEK, 0);
        sha.update(CEK);
        byte[] hash = sha.digest();
        byte[] ICV = new byte[8];
        System.arraycopy(hash, 0, ICV, 0, 8);
        byte[] CEKICV = new byte[CEK.length + ICV.length];
        System.arraycopy(CEK, 0, CEKICV, 0, CEK.length);
        System.arraycopy(ICV, 0, CEKICV, CEK.length, ICV.length);
        byte[] IV = new byte[8];
        nextRandomBytes(IV);
        modeAttributes.put(IMode.IV, IV);
        asmAttributes.put(Assembly.DIRECTION, Direction.FORWARD);
        byte[] TEMP1;
        try {
            asm.init(asmAttributes);
            TEMP1 = asm.lastUpdate(CEKICV);
        } catch (TransformerException x) {
            throw new RuntimeException(x);
        }
        byte[] TEMP2 = new byte[IV.length + TEMP1.length];
        System.arraycopy(IV, 0, TEMP2, 0, IV.length);
        System.arraycopy(TEMP1, 0, TEMP2, IV.length, TEMP1.length);
        byte[] TEMP3 = new byte[TEMP2.length];
        for (int i = 0, j = TEMP2.length - 1; i < TEMP2.length; i++, j--) TEMP3[j] = TEMP2[i];
        modeAttributes.put(IMode.IV, DEFAULT_IV);
        asmAttributes.put(Assembly.DIRECTION, Direction.FORWARD);
        byte[] result;
        try {
            asm.init(asmAttributes);
            result = asm.lastUpdate(TEMP3);
        } catch (TransformerException x) {
            throw new RuntimeException(x);
        }
        return result;
    }

    protected byte[] engineUnwrap(byte[] in, int inOffset, int length) throws KeyUnwrappingException {
        if (length != 40) throw new IllegalArgumentException("length MUST be 40");
        modeAttributes.put(IMode.IV, DEFAULT_IV);
        asmAttributes.put(Assembly.DIRECTION, Direction.REVERSED);
        byte[] TEMP3;
        try {
            asm.init(asmAttributes);
            TEMP3 = asm.lastUpdate(in, inOffset, 40);
        } catch (TransformerException x) {
            throw new RuntimeException(x);
        }
        byte[] TEMP2 = new byte[40];
        for (int i = 0, j = 40 - 1; i < 40; i++, j--) TEMP2[j] = TEMP3[i];
        byte[] IV = new byte[8];
        byte[] TEMP1 = new byte[32];
        System.arraycopy(TEMP2, 0, IV, 0, 8);
        System.arraycopy(TEMP2, 8, TEMP1, 0, 32);
        modeAttributes.put(IMode.IV, IV);
        asmAttributes.put(Assembly.DIRECTION, Direction.REVERSED);
        byte[] CEKICV;
        try {
            asm.init(asmAttributes);
            CEKICV = asm.lastUpdate(TEMP1, 0, 32);
        } catch (TransformerException x) {
            throw new RuntimeException(x);
        }
        byte[] CEK = new byte[24];
        byte[] ICV = new byte[8];
        System.arraycopy(CEKICV, 0, CEK, 0, 24);
        System.arraycopy(CEKICV, 24, ICV, 0, 8);
        sha.update(CEK);
        byte[] hash = sha.digest();
        byte[] computedICV = new byte[8];
        System.arraycopy(hash, 0, computedICV, 0, 8);
        if (!Arrays.equals(ICV, computedICV)) throw new KeyUnwrappingException("ICV and computed ICV MUST match");
        if (!TripleDES.isParityAdjusted(CEK, 0)) throw new KeyUnwrappingException("Triple-DES key parity MUST be adjusted");
        return CEK;
    }

    /**
   * Fills the designated byte array with random data.
   * 
   * @param buffer the byte array to fill with random data.
   */
    private void nextRandomBytes(byte[] buffer) {
        if (rnd != null) rnd.nextBytes(buffer); else getDefaultPRNG().nextBytes(buffer);
    }
}
