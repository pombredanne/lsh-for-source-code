package pmp.macro.str;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import pmp.AbstractMacro;
import pmp.ConfigDirectives;
import pmp.Macroprocessor;

/**
 * Macro for calculating MD5 checksum.
 * <p>
 * Title: PMP: Macroprocessor
 * </p>
 * <p>
 * Description: Java macroprocessor
 * </p>
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * <p>
 * Company:
 * </p>
 * <a href="http://www.faqs.org/rfcs/rfc1321">RFC 1321</a>
 * 
 * @author Luděk Hlaváček
 * @version 1.0
 */
public class MD5 extends AbstractMacro {

    private static final char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private void appendHexByte(int b, StringBuilder str) {
        int bh = ((b >>> 4) & 0x0F);
        int bl = (b & 0x0F);
        str.append(digits[bh]).append(digits[bl]);
    }

    private MessageDigest md = null;

    public MD5() {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
        }
    }

    public static final int PARAMS_REQUIRED = 1;

    @Override
    public String run(Macroprocessor mp, String[] params) {
        String encoding = mp.getMPMacros().getString(ConfigDirectives.INPUT_ENCODING);
        byte[] str;
        if (params.length < 2) {
            str = new byte[0];
        } else {
            if ("".equals(encoding)) {
                str = params[1].getBytes();
            } else {
                try {
                    str = params[1].getBytes(encoding);
                } catch (UnsupportedEncodingException ex) {
                    mp.warning("Unsupported encoding" + encoding);
                    str = params[1].getBytes();
                }
            }
        }
        if (md == null) {
            mp.error("Initialization of MD5 digest algorithm failed.");
            return "";
        }
        md.reset();
        md.update(str);
        byte[] result = md.digest();
        StringBuilder buffer = new StringBuilder(2 * result.length);
        for (int i = 0; i < result.length; i++) {
            appendHexByte(result[i], buffer);
        }
        return buffer.toString();
    }
}
