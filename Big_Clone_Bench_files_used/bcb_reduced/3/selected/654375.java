package org.lucrative.lucre;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Ben Laurie
 * @author patrick@lfcgate.com
 */
public class UnsignedCoin {

    private BigInteger m_biCoinID;

    public UnsignedCoin() {
    }

    public UnsignedCoin(final String id) {
        m_biCoinID = new BigInteger(id, 16);
    }

    public UnsignedCoin(final BigInteger id) {
        m_biCoinID = id;
    }

    public void generateRandomID(final int nCoinLength) {
        m_biCoinID = new BigInteger(nCoinLength * 8, Util.randomGenerator());
    }

    public void setID(final BigInteger biCoinID) {
        m_biCoinID = biCoinID;
    }

    public BigInteger getID() {
        return m_biCoinID;
    }

    public BigInteger generateCoinNumber(final PublicBank bank) throws NoSuchAlgorithmException {
        int nCoinLength = (m_biCoinID.bitLength() + 7) / 8;
        int nDigestIterations = (bank.getPrimeLength() - nCoinLength) / PublicBank.DIGEST_LENGTH;
        int n;
        if (nCoinLength > bank.getCoinLength()) {
            return null;
        }
        byte[] xplusd = new byte[bank.getPrimeLength()];
        for (n = 0; n < (bank.getCoinLength() - nCoinLength); ++n) xplusd[n] = 0;
        Util.byteCopy(xplusd, n, m_biCoinID.toByteArray(), 0, nCoinLength);
        nCoinLength += n;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        for (n = 0; n < nDigestIterations; ++n) {
            sha1.update(xplusd, 0, nCoinLength + (PublicBank.DIGEST_LENGTH * n));
            Util.byteCopy(xplusd, nCoinLength + (PublicBank.DIGEST_LENGTH * n), sha1.digest(), 0, PublicBank.DIGEST_LENGTH);
        }
        BigInteger bi = new BigInteger(xplusd);
        return bi;
    }

    public void read(final BufferedReader reader) throws IOException {
        m_biCoinID = Util.readNumber(reader, "id=");
    }

    public void write(final java.io.PrintWriter str) {
        Util.dumpNumber(str, "id=", m_biCoinID);
    }
}
