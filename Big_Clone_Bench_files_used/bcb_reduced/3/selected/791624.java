package growl.delegate.growltalk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MD5GrowlAuthentication implements GrowlAuthentication {

    private byte[] password;

    public MD5GrowlAuthentication(String password) {
        this.password = Util.toUTF8(password);
    }

    @Override
    public GrowlTalkVersion getProtocolVersion() {
        return GrowlTalkVersion.PLAIN;
    }

    @Override
    public GrowlTalkPacketType getRegistrationType() {
        return GrowlTalkPacketType.REGISTRATION;
    }

    @Override
    public GrowlTalkPacketType getNotificationType() {
        return GrowlTalkPacketType.NOTIFICATION;
    }

    @Override
    public byte[] generateChecksum(byte[] message) {
        try {
            byte[] input = Arrays.copyOf(message, message.length + password.length);
            System.arraycopy(password, 0, input, message.length, password.length);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return md5.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
