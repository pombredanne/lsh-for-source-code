package util.gen;

/**
 * <p>This class defines a method,
 * {@link MD5Crypt#crypt(java.lang.String, java.lang.String) crypt()}, which
 * takes a password and a salt text and generates an OpenBSD/FreeBSD/Linux-compatible
 * md5-encoded password entry.</p>
 *
 * <p>Created: 3 November 1999</p>
 * <p>Release: $Name:  $</p>
 * <p>Version: $Revision: 1.1 $</p>
 * <p>Last Mod Date: $Date: 2004/02/04 08:10:35 $</p>
 * <p>Java Code By: Jonathan Abbey, jonabbey@arlut.utexas.edu</p>
 * <p>Original C Version:<pre>
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <phk@login.dknet.dk> wrote this file.  As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp
 * ----------------------------------------------------------------------------
 * </pre></p>
 * Modified by Nix April 2005,  just call MD5Crypt.crypt("password","salt"); returns crypted password, not magic or salt stuff.
 */
public final class MD5Crypt {

    public static void main(String args[]) {
        if (args.length == 0) System.out.println("\nEnter a password to encrypt and a salt.\n"); else System.out.println(MD5Crypt.crypt(args[0], args[1]));
    }

    public static final String crypt(String password, String salt) {
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
}
