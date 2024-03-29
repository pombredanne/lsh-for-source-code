package net.sourceforge.freejava.security;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.sourceforge.freejava.collection.iterator.ImmediateIteratorX;
import net.sourceforge.freejava.io.resource.IStreamInputSource;
import net.sourceforge.freejava.util.exception.RuntimizedException;
import net.sourceforge.freejava.util.exception.UnexpectedException;

public class Cryptos {

    static MessageDigest newDigest(String alg) {
        try {
            MessageDigest digest = MessageDigest.getInstance(alg);
            return digest;
        } catch (NoSuchAlgorithmException e) {
            throw new UnexpectedException(String.format("Message digest %s isn\'t installed", alg));
        }
    }

    public static MessageDigest getMD5() {
        return newDigest("MD5");
    }

    public static MessageDigest getSHA1() {
        return newDigest("SHA1");
    }

    static byte[] calc(IStreamInputSource in, MessageDigest digest) throws IOException {
        ImmediateIteratorX<byte[], ? extends IOException> blocks = in.forRead().byteBlocks(true);
        byte[] block;
        try {
            while ((block = blocks.next()) != null || !blocks.isEnded()) digest.update(block);
        } catch (RuntimizedException e) {
            e.rethrow(IOException.class);
        }
        return digest.digest();
    }

    public static byte[] md5(byte[] bin) {
        MessageDigest md5 = getMD5();
        return md5.digest(bin);
    }

    public static byte[] md5(IStreamInputSource in) throws IOException {
        MessageDigest md5 = getMD5();
        return calc(in, md5);
    }

    public static byte[] sha1(byte[] bin) {
        MessageDigest sha1 = getSHA1();
        return sha1.digest(bin);
    }

    public static byte[] sha1(IStreamInputSource in) throws IOException {
        MessageDigest sha1 = getSHA1();
        return calc(in, sha1);
    }
}
