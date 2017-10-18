package de.uni_leipzig.lots.webfrontend.utils;

import de.uni_leipzig.lots.common.objects.Password;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class encrypts and compares passwords.
 *
 * @author Alexander Kiel
 */
public class PasswordFactory {

    private final SecureRandom random;

    private final MessageDigest digest;

    private final Lock lock = new ReentrantLock();

    public PasswordFactory() throws NoSuchAlgorithmException {
        random = new SecureRandom();
        random.setSeed(System.currentTimeMillis());
        digest = MessageDigest.getInstance("SHA-256");
    }

    @NotNull
    public Password encrypt(@NotNull String password) {
        lock.lock();
        try {
            byte[] salt = new byte[32];
            random.nextBytes(salt);
            digest.update(salt);
            digest.update(password.getBytes("UTF-8"));
            return new Password(salt, digest.digest());
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public boolean isEqual(@Nullable Password password, @Nullable String plainTextPassword) {
        if (password == null || plainTextPassword == null) {
            return false;
        }
        lock.lock();
        try {
            digest.update(password.getSalt());
            digest.update(plainTextPassword.getBytes("UTF-8"));
            return equals(password.getHash(), digest.digest());
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * This is a copy of {@link Arrays#equals(byte[],byte[])}.
     * <p/>
     * I have done this for security. Someone could hack this method in the JVM to return always
     * <tt>true</tt>.
     *
     * @param a  one array to be tested for equality.
     * @param a2 the other array to be tested for equality.
     * @return <tt>true</tt> if the two arrays are equal.
     * @see Arrays#equals(byte[],byte[])
     */
    private static boolean equals(byte[] a, byte[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }
}
