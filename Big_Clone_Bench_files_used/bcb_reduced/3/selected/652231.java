package net.sf.drftpd.util;

/**
 * <p>This class defines a method,
 * {@link MD5Crypt#crypt(java.lang.String, java.lang.String) crypt()}, which
 * takes a password and a salt string and generates an OpenBSD/FreeBSD/Linux-compatible
 * md5-encoded password entry.</p>
 *
 * <p>Created: 3 November 1999</p>
 * <p>Release: $Name:  $</p>
 * <p>Version: $Revision: 1.1 $</p>
 * <p>Last Mod Date: $Date: 2007-05-08 19:24:56 -0400 (Tue, 08 May 2007) $</p>
 * <p>Java Code By: Jonathan Abbey, jonabbey@arlut.utexas.edu</p>
 * <p>Original C Version:<pre>
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <phk@login.dknet.dk> wrote this file.  As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp
 * ----------------------------------------------------------------------------
 * </pre></p>
 */
public final class MD5Crypt {

    /**
	 *
	 * Command line test rig.
	 *
	 */
    public static void main(String argv[]) {
        if ((argv.length < 1) || (argv.length > 2)) {
            System.err.println("Usage: MD5Crypt password salt");
            System.exit(1);
        }
        if (argv.length == 2) {
            System.err.println(MD5Crypt.crypt(argv[0], argv[1]));
        } else {
            System.err.println(MD5Crypt.crypt(argv[0]));
        }
        System.exit(0);
    }

    private static final String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    private static final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final String to64(long v, int size) {
        StringBuffer result = new StringBuffer();
        while (--size >= 0) {
            result.append(itoa64.charAt((int) (v & 0x3f)));
            v >>>= 6;
        }
        return result.toString();
    }

    private static final void clearbits(byte bits[]) {
        for (int i = 0; i < bits.length; i++) {
            bits[i] = 0;
        }
    }

    /**
	 * convert an encoded unsigned byte value into a int
	 * with the unsigned value.
	 */
    private static final int bytes2u(byte inp) {
        return (int) inp & 0xff;
    }

    /**
	 * <p>This method actually generates a OpenBSD/FreeBSD/Linux PAM compatible
	 * md5-encoded password hash from a plaintext password and a
	 * salt.</p>
	 *
	 * <p>The resulting string will be in the form '$1$&lt;salt&gt;$&lt;hashed mess&gt;</p>
	 *
	 * @param password Plaintext password
	 *
	 * @return An OpenBSD/FreeBSD/Linux-compatible md5-hashed password field.
	 */
    public static final String crypt(String password) {
        StringBuffer salt = new StringBuffer();
        java.util.Random randgen = new java.util.Random();
        while (salt.length() < 8) {
            int index = (int) (randgen.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.substring(index, index + 1));
        }
        return MD5Crypt.crypt(salt.toString(), password);
    }

    /**
	 * <p>This method actually generates a OpenBSD/FreeBSD/Linux PAM compatible
	 * md5-encoded password hash from a plaintext password and a
	 * salt.</p>
	 *
	 * <p>The resulting string will be in the form '$1$&lt;salt&gt;$&lt;hashed mess&gt;</p>
	 *
	 * @param salt A short string to use to randomize md5.  May start with $1$, which
	 *             will be ignored.  It is explicitly permitted to pass a pre-existing
	 *             MD5Crypt'ed password entry as the salt.  crypt() will strip the salt
	 *             chars out properly.
	 * @param password Plaintext password
	 *
	 * @return An OpenBSD/FreeBSD/Linux-compatible md5-hashed password field.
	 */
    public static final String crypt(String salt, String password) {
        String magic = "$1$";
        byte finalState[];
        MD5 ctx, ctx1;
        long l;
        if (salt.startsWith(magic)) {
            salt = salt.substring(magic.length());
        }
        if (salt.indexOf('$') != -1) {
            salt = salt.substring(0, salt.indexOf('$'));
        }
        if (salt.length() > 8) {
            salt = salt.substring(0, 8);
        }
        ctx = new MD5();
        ctx.update(password.getBytes());
        ctx.update(magic.getBytes());
        ctx.update(salt.getBytes());
        ctx1 = new MD5();
        ctx1.update(password.getBytes());
        ctx1.update(salt.getBytes());
        ctx1.update(password.getBytes());
        finalState = ctx1.digest();
        for (int pl = password.length(); pl > 0; pl -= 16) {
            for (int i = 0; i < (pl > 16 ? 16 : pl); i++) ctx.update(finalState[i]);
        }
        clearbits(finalState);
        for (int i = password.length(); i != 0; i >>>= 1) {
            if ((i & 1) != 0) {
                ctx.update(finalState[0]);
            } else {
                ctx.update(password.getBytes()[0]);
            }
        }
        finalState = ctx.digest();
        for (int i = 0; i < 1000; i++) {
            ctx1 = new MD5();
            if ((i & 1) != 0) {
                ctx1.update(password.getBytes());
            } else {
                for (int c = 0; c < 16; c++) ctx1.update(finalState[c]);
            }
            if ((i % 3) != 0) {
                ctx1.update(salt.getBytes());
            }
            if ((i % 7) != 0) {
                ctx1.update(password.getBytes());
            }
            if ((i & 1) != 0) {
                for (int c = 0; c < 16; c++) ctx1.update(finalState[c]);
            } else {
                ctx1.update(password.getBytes());
            }
            finalState = ctx1.digest();
        }
        StringBuffer result = new StringBuffer();
        result.append(magic);
        result.append(salt);
        result.append("$");
        l = (bytes2u(finalState[0]) << 16) | (bytes2u(finalState[6]) << 8) | bytes2u(finalState[12]);
        result.append(to64(l, 4));
        l = (bytes2u(finalState[1]) << 16) | (bytes2u(finalState[7]) << 8) | bytes2u(finalState[13]);
        result.append(to64(l, 4));
        l = (bytes2u(finalState[2]) << 16) | (bytes2u(finalState[8]) << 8) | bytes2u(finalState[14]);
        result.append(to64(l, 4));
        l = (bytes2u(finalState[3]) << 16) | (bytes2u(finalState[9]) << 8) | bytes2u(finalState[15]);
        result.append(to64(l, 4));
        l = (bytes2u(finalState[4]) << 16) | (bytes2u(finalState[10]) << 8) | bytes2u(finalState[5]);
        result.append(to64(l, 4));
        l = bytes2u(finalState[11]);
        result.append(to64(l, 2));
        clearbits(finalState);
        return result.toString();
    }
}
