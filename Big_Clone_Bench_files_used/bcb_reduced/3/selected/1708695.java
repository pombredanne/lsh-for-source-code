package com.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * This class supplies a lot of methods to handle default values and types conversion.
 *
 * @author Erich Roncarolo
 * @version 0.1 - 2004-08-25
 */
public class Default {

    /**
     *
     * @param o1
     * @param o2
     * @return se true i due object cono uguali
     */
    public static final boolean equals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }

    /**
     * Default for null target
     *
     * @param target the target to check
     * @param def the default
     * @return the target itself if it's not null, def otherwise
     */
    public static final Object notNull(Object target, Object def) {
        return (target != null ? target : def);
    }

    /**
     * Default for null target
     *
     * @param target the target to check
     * @return the target itself if it's not null, an empty String otherwise
     */
    public static final String notNull(String target) {
        return (target != null ? target : "");
    }

    /**
     * Default for null target
     *
     * @param target the target to check
     * @param def the default
     * @param prefix to prepend if string is not null (can be null)
     * @param suffix to append if string is not null (can be null)
     * @return the target itself if it's not null, def otherwise
     */
    public static final String notNull(String target, String def, String prefix, String suffix) {
        return (target != null ? "" + notNull(prefix, "") + target + notNull(suffix, "") : def);
    }

    /**
     *
     * @param prefix
     * @param target
     * @param suffix
     * @return
     */
    public static final String notNull(String prefix, String target, String suffix) {
        return (target != null ? "" + notNull(prefix, "") + target + notNull(suffix, "") : "");
    }

    /**
     * Default for null or empty Strings.
     * A String is empty if it's null or if contains only whitespaces.
     *
     * @param target the target to check
     * @param def the default
     * @return the target itself if it's not empty, def otherwise
     * @see StringUtil#isEmpty(String)
     */
    public static final String notEmpty(String target, String def) {
        return (!StringUtil.isEmpty(target) ? target : def);
    }

    /**
     * Default for null or empty Strings.
     * A String is empty if it's null or if contains only whitespaces.
     *
     * @param target the target to check
     * @param def the default
     * @param prefix to prepend if string is not empty (can be null)
     * @param suffix to append if string is not empty (can be null)
     * @return the target itself if it's not empty, def otherwise
     * @see StringUtil#isEmpty(String)
     */
    public static final String notEmpty(String target, String def, String prefix, String suffix) {
        return (!StringUtil.isEmpty(target) ? notNull(prefix, "") + target + notNull(suffix, "") : def);
    }

    /**
     * Returns a boolean from the Boolean, Number or String representation of the target.
     * If the target is null or empty the default is returned.
     *
     * @param target the target to handle
     * @param def the default
     * @return def if target is null or empty, true if target is true, not zero, "true", "on", "enable", "yes", "ok", "1" (case insensitive), false otherwise
     */
    public static final boolean toBoolean(Object target, boolean def) {
        if (target == null) {
            return def;
        }
        if (target.getClass().isArray()) {
            Object t[] = ((Object[]) target);
            if (t.length == 0) {
                return def;
            }
            target = t[0];
        }
        if (target instanceof Boolean) {
            return ((Boolean) target).booleanValue();
        }
        if (target instanceof Number) {
            return ((Number) target).intValue() != 0;
        }
        String s = target.toString();
        if (StringUtil.isEmpty(s)) {
            return def;
        }
        return "true".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s) || "enable".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "ok".equalsIgnoreCase(s) || "1".equalsIgnoreCase(s) || "yeah!".equalsIgnoreCase(s) || "sure!".equalsIgnoreCase(s);
    }

    /**
     *
     * @param target
     * @param def
     * @return
     */
    public static final boolean toBoolean(Object target, Boolean def) {
        return toBoolean(target, (def != null ? def.booleanValue() : false));
    }

    /**
     * Returns a boolean from the String representation of the target.
     * If the String is empty false is returned.
     *
     * @param target the target to handle
     * @return true if target is "true", "on", "ok", "yes", "1" (case insensitive), false otherwise
     */
    public static final boolean toBoolean(Object target) {
        return toBoolean(target, false);
    }

    /**
     * Returns an intr from the String representation of the target.
     *
     * @param target the target to handle
     * @param def the default
     * @return an int if the target represent an int number, def otherwise
     */
    public static final int toInt(Object target, int def) {
        if (target == null) {
            return def;
        }
        if (target instanceof Number) {
            return ((Number) target).intValue();
        }
        String s = target.toString();
        if (StringUtil.isEmpty(s)) {
            return def;
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception x) {
            return def;
        }
    }

    /**
     *
     * @param target
     * @param def
     * @return
     */
    public static final int toInt(Object target, Integer def) {
        return toInt(target, (def != null ? def.intValue() : 0));
    }

    /**
     * Returns an Integer from the String representation of the target.
     *
     * @param target the target to handle
     * @param def the default
     * @return an Integer if the target represent an int number, def otherwise
     */
    public static final Integer toInteger(Object target, int def) {
        return new Integer(toInt(target, def));
    }

    /**
     *
     * @param target
     * @param def
     * @return
     */
    public static final Integer toInteger(Object target, Integer def) {
        return new Integer(toInt(target, def));
    }

    /**
     * Returns an Integer from the String representation of the target.
     *
     * @param target the target to handle
     * @return an Integer if the target represent an int number, 0 otherwise
     */
    public static final Integer toInteger(Object target) {
        return toInteger(target, 0);
    }

    /**
     * Returns a Long from the String representation of the target.
     *
     * @param target the target to handle
     * @param def the default
     * @return a Long if the target represent a Long number, def otherwise
     */
    public static final Long toLong(Object target, Long def) {
        if (target == null) {
            return def;
        }
        if (target instanceof Number) {
            return new Long(((Number) target).longValue());
        }
        String s = target.toString();
        if (StringUtil.isEmpty(s)) {
            return def;
        }
        try {
            return Long.valueOf(s);
        } catch (Exception x) {
            return def;
        }
    }

    /**
     *
     * @param target
     * @param def
     * @return
     */
    public static final Long toLong(Object target, long def) {
        return toLong(target, new Long(def));
    }

    /**
     * Returns a Long from the String representation of the target.
     *
     * @param target the target to handle
     * @return a Long if the target represent a Long number, 0 otherwise
     */
    public static final Long toLong(Object target) {
        return toLong(target, 0);
    }

    /**
     * Returns a Float from the String representation of the target.
     *
     * @param target the target to handle
     * @param def the default
     * @return a Float if the target represent a float number, def otherwise
     */
    public static final Float toFloat(Object target, Float def) {
        if (target == null) {
            return def;
        }
        if (target instanceof Number) {
            return new Float(((Number) target).floatValue());
        }
        String s = target.toString();
        if (StringUtil.isEmpty(s)) {
            return def;
        }
        try {
            return Float.valueOf(s);
        } catch (Exception x) {
            return def;
        }
    }

    /**
     *
     * @param target
     * @param def
     * @return
     */
    public static final Float toFloat(Object target, double def) {
        return toFloat(target, new Float(def));
    }

    /**
     * Returns a Float from the String representation of the target.
     *
     * @param target the target to handle
     * @return a Float if the target represent a float number, 0 otherwise
     */
    public static final Float toFloat(Object target) {
        return toFloat(target, 0);
    }

    /**
     * Returns a Double from the String representation of the target.
     *
     * @param target the target to handle
     * @param def the default
     * @return a Double if the target represent a Double number, def otherwise
     */
    public static final Double toDouble(Object target, Double def) {
        if (target == null) {
            return def;
        }
        if (target instanceof Number) {
            return new Double(((Number) target).doubleValue());
        }
        String s = target.toString();
        if (StringUtil.isEmpty(s)) {
            return def;
        }
        try {
            return Double.valueOf(s);
        } catch (Exception x) {
            return def;
        }
    }

    /**
     *
     * @param target
     * @param def
     * @return
     */
    public static final Double toDouble(Object target, double def) {
        return toDouble(target, new Double(def));
    }

    /**
     *
     * @param target
     * @return
     */
    public static final double todouble(Object target) {
        return toDouble(target, 0).doubleValue();
    }

    /**
     *
     * @param target
     * @param def
     * @return
     */
    public static final double todouble(Object target, double def) {
        return toDouble(target, def).doubleValue();
    }

    /**
     * Returns a Double from the String representation of the target.
     *
     * @param target the target to handle
     * @return a Double if the target represent a Double number, 0 otherwise
     */
    public static final Double toDouble(Object target) {
        return toDouble(target, 0);
    }

    /**
     *
     * @param o
     * @param def
     * @return
     */
    public static final String toString(Object o, String def) {
        if (o == null) {
            return def;
        }
        return "" + o;
    }

    /**
     *
     * @param o
     * @return
     */
    public static final String toString(Object o) {
        return toString(o, null);
    }

    /**
     *
     * @param s
     * @param def
     * @return
     */
    public static final Locale toLocale(Object s, Locale def) {
        if (s == null) {
            return def;
        }
        if (s instanceof Locale) {
            return (Locale) s;
        }
        String l[] = StringUtil.tokenize("" + s, "_");
        if (l == null || l.length < 1) {
            return def;
        }
        Locale loc = null;
        if (l.length < 2) {
            loc = new Locale(l[0].toLowerCase());
        } else {
            loc = new Locale(l[0].toLowerCase(), l[1].toUpperCase());
        }
        return loc;
    }

    /**
     *
     * @param s
     * @return
     */
    public static final Locale toLocale(Object s) {
        return toLocale(s, null);
    }

    /**
     * Returns a char that is the Ascii code rappresentation of the input
     *
     * @param n An ascii code
     * @return a char
     */
    public static final char fromAscii(int n) {
        return (char) n;
    }

    /**
     *
     * @param data
     * @param separator
     * @return numero di token presenti
     */
    public static final int countTokens(String data, String separator) {
        return new StringTokenizer(data, separator).countTokens();
    }

    /**
     * Returns true if objects are equals.
     *
     * @param left the first object to compare
     * @param right the second object to compare
     * @return true if both left and right are null or left.equals(right), false otherwise
     */
    public static final boolean eq(Object left, Object right) {
        return (left == null ? right == null : left.equals(right));
    }

    /**
     *
     * @param s
     * @return ritorna il formato md5 della stringa passata
     */
    public static final String md5(final String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
