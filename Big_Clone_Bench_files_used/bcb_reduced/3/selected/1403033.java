package org.jbrt.client;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 *
 * @author Cipov Peter
 */
public class JDefaultHashCodeCreator implements JHashCodeCreator {

    public String countHashCode(Throwable t) {
        String hash = null;
        try {
            MessageDigest hashCreator = MessageDigest.getInstance("MD5");
            countHashCode(t, hashCreator);
            hash = new BigInteger(1, hashCreator.digest()).toString(Character.MAX_RADIX);
            hashCreator.reset();
        } catch (Throwable th) {
            JBrt.commitInternal(th);
        }
        return hash;
    }

    private void countHashCode(Throwable t, MessageDigest hashCreator) {
        updateFromThrowable(t, hashCreator);
        Throwable cause = t;
        while ((cause = cause.getCause()) != null) {
            updateFromThrowable(cause, hashCreator);
        }
    }

    private void updateFromThrowable(Throwable t, MessageDigest hashCreator) {
        hashCreator.update(t.getClass().getName().getBytes());
        updateFromStackTrace(t.getStackTrace(), hashCreator);
    }

    private void updateFromStackTrace(StackTraceElement[] stack, MessageDigest hashCreator) {
        if (stack == null) {
            return;
        }
        for (StackTraceElement element : stack) {
            if (element.getClassName() != null) {
                hashCreator.update(element.getClassName().getBytes());
            }
            if (element.getFileName() != null) {
                hashCreator.update(element.getFileName().getBytes());
            }
            if (element.getMethodName() != null) {
                hashCreator.update(element.getMethodName().getBytes());
            }
            hashCreator.update(("" + element.getLineNumber()).getBytes());
        }
    }
}
