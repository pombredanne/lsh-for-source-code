package com.sdi.pws.codec;

import com.sdi.crypto.blowfish.*;
import com.sdi.crypto.sha.SHA;
import com.sdi.crypto.sha.SHAModified;
import com.sdi.pws.db.*;
import com.sdi.pws.gui.compo.db.change.ChangeViewDatabase;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.Iterator;

public class CodecUtil {

    public static boolean checkPassphrase(PwsFileHeader aHeader, String aPassphrase) {
        final byte[] lCheck = calculatePassphraseHash(aHeader.getRndData(), aPassphrase);
        return Arrays.equals(lCheck, aHeader.getRndCheck());
    }

    public static byte[] calculatePassphraseHash(long aData, String aPassphrase) {
        final byte[] lPassphrasebytes = aPassphrase.getBytes();
        final SHA lDigester = new SHA();
        final byte[] lRndDataBytes = new byte[8];
        BlowfishUtil.unpackLE(aData, lRndDataBytes, 0);
        lDigester.update(lRndDataBytes, 0, 8);
        lDigester.update((byte) 0);
        lDigester.update((byte) 0);
        lDigester.update(lPassphrasebytes, 0, lPassphrasebytes.length);
        final byte[] lCalcKey = lDigester.digest();
        final Blowfish lFish = new BlowfishLE(new BlowfishBasic(lCalcKey));
        long lBlock = aData;
        for (int i = 0; i < 1000; i++) lBlock = lFish.encipher(lBlock);
        final byte[] lBlockBytes = new byte[8];
        BlowfishUtil.unpackLE(lBlock, lBlockBytes, 0);
        final SHAModified lSpecialDigester = new SHAModified();
        lSpecialDigester.update(lBlockBytes, 0, 8);
        lSpecialDigester.update((byte) 0);
        lSpecialDigester.update((byte) 0);
        return lSpecialDigester.digest();
    }

    public static Blowfish initBlowfish(PwsFileHeader aHeader, String aPassphrase) {
        SHA lDigester = new SHA();
        lDigester.update(aPassphrase.getBytes(), 0, aPassphrase.getBytes().length);
        lDigester.update(aHeader.getPwSalt(), 0, aHeader.getPwSalt().length);
        final byte[] lOperationalPwd = lDigester.digest();
        return new BlowfishCBC(new BlowfishLE(new BlowfishBasic(lOperationalPwd)), aHeader.getCbc());
    }

    public static PwsFileHeader initPwsHeader(String aPassphrase) {
        PwsFileHeader lPwsHeader = new PwsFileHeader();
        Random lRnd = new Random();
        lRnd.setSeed(System.currentTimeMillis());
        lPwsHeader.setRndData(lRnd.nextLong());
        final byte[] lCheck = calculatePassphraseHash(lPwsHeader.getRndData(), aPassphrase);
        lPwsHeader.setRndCheck(lCheck);
        lRnd.nextBytes(lPwsHeader.getPwSalt());
        lPwsHeader.setCbc(lRnd.nextLong());
        return lPwsHeader;
    }

    public static PwsFieldImpl readValue(FileInputStream aStream, ByteBuffer aBuf, Blowfish aFish) throws IOException, CodecException {
        checkReadSpace(aStream, aBuf);
        long lValueByteLength = aBuf.getLong();
        long lDeciphered = aFish.decipher(lValueByteLength);
        lValueByteLength = lDeciphered & 0xffffffffL;
        final byte lType = (byte) ((lDeciphered & 0xff00000000L) >> 32);
        if (lValueByteLength > 2000) {
            final String lMsg = "codec03";
            throw new CodecException(lMsg);
        }
        final long lRestLength = (lValueByteLength == 0) ? 8 : (lValueByteLength % 8);
        final long lValueBlockLen = lRestLength > 0 ? ((lValueByteLength + (8 - lValueByteLength % 8)) / 8) : (lValueByteLength / 8);
        final byte[] lBlockBuf = new byte[8];
        final byte[] lValueBuf = new byte[(int) lValueByteLength];
        int lBufPtr = 0;
        for (int i = 0; i < lValueBlockLen; i++) {
            checkReadSpace(aStream, aBuf);
            long lBlock = aBuf.getLong();
            lBlock = aFish.decipher(lBlock);
            BlowfishUtil.unpackLE(lBlock, lBlockBuf, 0);
            int lBytesToCopy = lValueBuf.length - lBufPtr;
            if (lBytesToCopy > lBlockBuf.length) lBytesToCopy = lBlockBuf.length;
            System.arraycopy(lBlockBuf, 0, lValueBuf, lBufPtr, lBytesToCopy);
            lBufPtr += lBytesToCopy;
        }
        return new PwsFieldImpl(new Byte(lType), lValueBuf);
    }

    public static void writeValue(FileOutputStream aStream, ByteBuffer aBuf, Blowfish aFish, PwsField aField) throws IOException {
        final byte[] lValue = aField.getValue();
        final byte lType = aField.getType().byteValue();
        final int lValueByteLength = lValue.length;
        final long lRestLength = (lValueByteLength == 0) ? 8 : (lValueByteLength % 8);
        final long lValueBlockLen = lRestLength > 0 ? ((lValueByteLength + (8 - lValueByteLength % 8)) / 8) : (lValueByteLength / 8);
        final byte[] lBlockBuf = new byte[8];
        int lBufPtr = 0;
        checkWriteSpace(aStream, aBuf);
        final long lLengthBlock = (lValueByteLength & 0xffffffffL) | (((long) lType & 0xff) << 32);
        aBuf.putLong(aFish.encipher(lLengthBlock));
        for (int i = 0; i < lValueBlockLen; i++) {
            checkWriteSpace(aStream, aBuf);
            int lBytesToCopy = lValue.length - lBufPtr;
            if (lBytesToCopy > lBlockBuf.length) lBytesToCopy = lBlockBuf.length;
            System.arraycopy(lValue, lBufPtr, lBlockBuf, 0, lBytesToCopy);
            lBufPtr += lBytesToCopy;
            long lBlock = BlowfishUtil.packLE(lBlockBuf, 0);
            lBlock = aFish.encipher(lBlock);
            aBuf.putLong(lBlock);
        }
    }

    public static void checkWriteSpace(FileOutputStream aStream, ByteBuffer aBuf) throws IOException {
        if (aBuf.remaining() < 8) {
            aBuf.flip();
            aStream.getChannel().write(aBuf);
            aBuf.flip();
        }
    }

    public static void checkReadSpace(FileInputStream aStream, ByteBuffer aBuf) throws IOException {
        if (aBuf.remaining() < 8) {
            aBuf.flip();
            aStream.getChannel().read(aBuf);
            aBuf.flip();
        }
    }

    public static void upgradeVersion(PwsDatabase aDb) {
        final Codec lCodec = aDb.getCodec();
        if (Codec1.VERSION.equals(lCodec.getVersion())) {
            aDb.setCodec(new Codec2());
        }
    }

    public static void upgradeVersion(ChangeViewDatabase aDb, String aGroup) {
        final PwsDatabase lInternal = aDb.getInternal();
        upgradeVersion(lInternal, aGroup);
        aDb.setChanged(true);
    }

    public static void upgradeVersion(PwsDatabase aDb, String aGroup) {
        upgradeVersion(aDb);
        final Iterator lIter = aDb.iterator();
        while (lIter.hasNext()) {
            final PwsRecord lRec = (PwsRecord) lIter.next();
            final PwsField lField = new PwsFieldImpl(PwsField.FIELD_GROUP, aGroup);
            lRec.put(lField);
        }
    }

    public static void downgradeVersion(PwsDatabase aDb) {
        final Codec lCodec = aDb.getCodec();
        if (Codec2.VERSION.equals(lCodec.getVersion())) {
            aDb.setCodec(new Codec1());
        }
    }
}
