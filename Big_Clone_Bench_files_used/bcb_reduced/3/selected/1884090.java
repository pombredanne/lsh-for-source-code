package hu.rsc.svnAdmin.engine;

import java.security.NoSuchAlgorithmException;

/**
 *	Password is a class to implement password encryption as used
 *	on Unix systems. It is compatible with the crypt(3c) system function.
 *	This version is a based on the DES encryption algorithm in
 *	Andrew Tanenbaum's book "Computer Networks". It was rewritten
 *	in C and used in Perl release 4.035. This version was rewritten
 *	in Java by David Scott, Siemens Ltd., Australia.
 *
 *	For further details on the methods in this class, refer to the
 *	Unix man pages for crypt(3c).
 */
public class Password {

    public static String crypt(String passwd) throws NoSuchAlgorithmException {
        return "{SHA}" + new sun.misc.BASE64Encoder().encode(java.security.MessageDigest.getInstance("SHA1").digest(passwd.getBytes()));
    }
}
