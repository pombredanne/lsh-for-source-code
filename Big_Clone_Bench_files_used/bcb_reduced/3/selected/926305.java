package jpar2.utility;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Md5 extends Checksum {

    private MessageDigest calculator;

    private byte[] checksum;

    public Md5() {
        setupCalculator();
    }

    public Md5(byte[] checksum) {
        setChecksum(checksum);
        setupCalculator();
    }

    private void setupCalculator() {
        try {
            calculator = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] getChecksum() {
        if (checksum != null) return checksum;
        if (calculator == null) throw new IllegalStateException("MD5 calculator was not setup correctly and is null");
        return checksum = calculator.digest();
    }

    private void setChecksum(byte[] checksum) {
        if (checksum == null) throw new NullPointerException("Pre calculated checksum cannot be null");
        this.checksum = Arrays.copyOf(checksum, checksum.length);
    }

    @Override
    public void updateChecksum(byte[] data) {
        calculator.update(data);
    }
}
