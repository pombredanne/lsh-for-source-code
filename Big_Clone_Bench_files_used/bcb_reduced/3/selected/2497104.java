package com.sun.sgs.impl.auth;

import java.io.FileOutputStream;
import java.security.MessageDigest;

/**
 * This is a simple utility program used to create the password files that
 * are consumed by <code>NamePasswordAuthenticator</code>. The password
 * files consist of one entry per line, where each entry has a name, some
 * whitespace, a SHA-256 hashed password encoded via a call to
 * <code>NamePasswordAuthenticator.encodeBytes</code>, and finally a newline.
 */
public final class PasswordFileEditor {

    /**
     * This class should not be instantiated
     */
    private PasswordFileEditor() {
    }

    /**
     * Main-line for this utility. This utility takes three arguments on
     * the command line. The first argument is the file to update, the second
     * argument is the user name, and the third argument is that user's
     * password.
     *
     * @param args the arguments for this utility
     *
     * @throws Exception if anything fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: password_file name password");
            return;
        }
        byte[] pass = MessageDigest.getInstance("SHA-256").digest(args[2].getBytes("UTF-8"));
        byte[] encodedPass = NamePasswordAuthenticator.encodeBytes(pass);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(args[0], true);
            out.write(args[1].getBytes("UTF-8"));
            out.write("\t".getBytes("UTF-8"));
            out.write(encodedPass);
            out.write("\n".getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }
}
