package org.gjt.universe;

import java.security.MessageDigest;

public final class Util {

    public static final String[] ZeroStringArray = new String[0];

    public static final int[] ZeroIntArray = new int[0];

    private Util() {
    }

    public static String Roman(int value) {
        int tens = value / 10;
        int ones = value % 10;
        String returnstring = "NAN";
        switch(tens) {
            case 0:
                returnstring = "";
                break;
            case 1:
                returnstring = "X";
                break;
            case 2:
                returnstring = "XX";
                break;
            case 3:
                returnstring = "XXX";
                break;
            case 4:
                returnstring = "XL";
                break;
            case 5:
                returnstring = "L";
                break;
            case 6:
                returnstring = "LX";
                break;
            case 7:
                returnstring = "LXX";
                break;
            case 8:
                returnstring = "LXXX";
                break;
            case 9:
                returnstring = "XC";
                break;
            case 10:
                returnstring = "C";
                break;
        }
        switch(ones) {
            case 1:
                returnstring += "I";
                break;
            case 2:
                returnstring += "II";
                break;
            case 3:
                returnstring += "III";
                break;
            case 4:
                returnstring += "IV";
                break;
            case 5:
                returnstring += "V";
                break;
            case 6:
                returnstring += "VI";
                break;
            case 7:
                returnstring += "VII";
                break;
            case 8:
                returnstring += "VIII";
                break;
            case 9:
                returnstring += "IX";
                break;
        }
        return returnstring;
    }

    private static String[] greekChars = { "NAN", "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda", "mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega" };

    public static String Greek(int value) {
        String returnstring = "NAN";
        if (value > 0 && value < greekChars.length) returnstring = greekChars[value];
        return returnstring;
    }

    /**********************************************************
	 *
	 *	Gets the civ names for every civ, including
	 *	computer players.
	 *
	 **********************************************************/
    public static String[] getCivNames() {
        int size = CivList.size();
        String names[] = new String[size];
        for (int i = 0; i < CivList.size(); i++) {
            Civ civ = CivList.get(new CivID(i));
            names[i] = civ.getName();
        }
        return names;
    }

    /**********************************************************
	 *
	 *	Gets the race names (not object!) for every civ,
	 *	including computer players.
	 *
	 **********************************************************/
    public static String[] getCivRaceNames() {
        int size = CivList.size();
        String names[] = new String[size];
        for (int i = 0; i < CivList.size(); i++) {
            Civ civ = CivList.get(new CivID(i));
            RaceBase rb = civ.getRace();
            names[i] = rb.getName();
        }
        return names;
    }

    /**********************************************************
	 *
	 *	Capitalizes a string. If the string is made of
	 *	many words, only the first letter of the first
	 *	word is capitalized.
	 *
	 **********************************************************/
    public static String toTitleCase(String in) {
        char chs[] = in.toCharArray();
        Character.toTitleCase(chs[0]);
        return new String(chs);
    }

    /**********************************************************
	 *
	 *	Gets a hash, using a defined algorithm.
	 *	if the algorithm is unavailable, a run-time exception 
	 *	is thrown.
	 *
	 **********************************************************/
    public static String getHash(String text, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            byte temp[] = md.digest(text.getBytes());
            md = null;
            return new String(temp);
        } catch (Exception e) {
            throw new RuntimeException("Hash Algorithm \"" + algorithm + "\" not found!");
        }
    }

    /**********************************************************
	 *
	 *	Returns the MD5 hash of the given text.
	 *	MD5 hashes are 16 bytes (128 bits) in length.
	 *
	 **********************************************************/
    public static String getHashMD5(String text) {
        return getHash(text, "MD5");
    }

    /**********************************************************
	 *
	 *	Returns the SHA hash of the given text.
	 *	SHA hashes are 20 bytes (160 bits) in length.
	 *
	 **********************************************************/
    public static String getHashSHA(String text) {
        return getHash(text, "SHA");
    }

    /**
	 * This function is to replace the Format class in 1.1
	 */
    public static String fillBlanks(String str, int size) {
        String newstr = new String(str);
        int numfill = size - str.length();
        if (numfill > 0) {
            for (int cnt = 0; cnt < numfill; cnt++) {
                newstr += " ";
            }
        }
        return newstr;
    }
}
