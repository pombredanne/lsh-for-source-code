package issrg.ac;

import iaik.asn1.structures.GeneralNames;
import issrg.utils.EnvironmentalVariables;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.math.BigInteger;

/**
 * This class contains various utility routines for converting things from one
 * representation into another.
 *
 * @author A Otenko
 * @version 1.0
 */
public class Util {

    /**
   * This variable sets the attribute type for the serial number in the DN, when
   * constructing it for the IssuerSerial case.
   */
    public static final String SN_ATTRIBUTE_TYPE = "SN";

    /**
   * Converts the given general Name in iaik representation into a String.
   *
   * @param gns  the GeneralNames construct to convert; must be an X.500
   *            directory name.
   * @return the string representation of a General Name in reverse
   *          order; for example, "cn=Sassa, ou=ISI, o=Salford University, c=GB"
   */
    public static String generalNamesToString(iaik.asn1.structures.GeneralNames gns) {
        try {
            iaik.asn1.structures.Name name = new iaik.asn1.structures.Name();
            if (gns != null) {
                for (java.util.Enumeration e = gns.getNames(); e.hasMoreElements(); ) {
                    iaik.asn1.structures.GeneralName gn = (iaik.asn1.structures.GeneralName) e.nextElement();
                    if (gn.getType() == iaik.asn1.structures.GeneralName.directoryName) {
                        iaik.asn1.structures.Name n = (iaik.asn1.structures.Name) gn.getName();
                        for (java.util.Enumeration e_rdns = n.elements(); e_rdns.hasMoreElements(); ) {
                            name.addRDN((iaik.asn1.structures.RDN) e_rdns.nextElement());
                        }
                    }
                }
            }
            return name.getRFC2253String();
        } catch (iaik.utils.RFC2253NameParserException re) {
            return null;
        }
    }

    /**
   * This is the universal way for constructing the LDAP DN for the entry, whose
   * name is constructed out of the PKC Issuer DN and PKC SN.
   *
   * @param issuerDN - the DN of the issuer
   * @param serialNumber - the serial number of the PKC issued by that issuer
   *
   * @return the DN combining the serial number and the issuer DN
   */
    public static String issuerSerialToDN(String issuerDN, java.math.BigInteger serialNumber) {
        if (issuerDN == null || serialNumber == null) {
            return null;
        }
        return SN_ATTRIBUTE_TYPE + "=" + serialNumber.toString() + ((issuerDN.intern() == "") ? "" : ("," + issuerDN));
    }

    /**
   * Returns the string representation of the Issuer General Name, if V1Form or
   * IssuerName of the V2Form is present. Otherwise, returns null.
   *
   * @param aci - the AttCertIssuer structure
   * 
   * @return String of the Issuer GeneralName, or null, if the name is not 
   *   present
   *   in the given AttCertIssuer structure.
   */
    public static String issuerToString(AttCertIssuer aci) {
        String issuer = null;
        if (aci.getV1Form() != null) issuer = generalNamesToString(aci.getV1Form()); else if (aci.getV2Form() != null) {
            if (aci.getV2Form().getIssuerName() != null) issuer = generalNamesToString(aci.getV2Form().getIssuerName());
        }
        return issuer;
    }

    /**
   * This method builds a General Names construct out of the string
   * representation of an LDAP DN that should be RFC2253 compliant. Note that 
   * the
   * attribute names are case insensitive, even though RFC2253 specifies 
   * otherwise.
   *
   * @param DN is the String with LDAP DN; if a parse error occures, a 
   *    GeneralNames
   *    corresponding to the null DN will be constructed
   *
   * @return a GeneralNames construct; is never null
   */
    public static iaik.asn1.structures.GeneralNames buildGeneralNames(String DN) {
        try {
            DN = issrg.utils.RFC2253NameParser.toCanonicalDN(issrg.utils.RFC2253NameParser.distinguishedName(DN));
            iaik.asn1.structures.GeneralNames result = new iaik.asn1.structures.GeneralNames(new iaik.asn1.structures.GeneralName(iaik.asn1.structures.GeneralName.directoryName, new iaik.utils.RFC2253NameParser(DN).parse()));
            if (result == null) {
                throw new iaik.utils.RFC2253NameParserException();
            }
            return result;
        } catch (issrg.utils.RFC2253ParsingException npe) {
            return new iaik.asn1.structures.GeneralNames(new iaik.asn1.structures.GeneralName(iaik.asn1.structures.GeneralName.directoryName, new iaik.asn1.structures.Name()));
        } catch (iaik.utils.RFC2253NameParserException re) {
            return new iaik.asn1.structures.GeneralNames(new iaik.asn1.structures.GeneralName(iaik.asn1.structures.GeneralName.directoryName, new iaik.asn1.structures.Name()));
        }
    }

    /**
  * This method converts the hash byte array to string. Use this method,
  * if you are looking for filenames, so the filenames are created in a
  * uniform fashion.
  *
  * @param hash - the byte array of the hash
  *
  * @return the String of the hash in hexadecimal form
  */
    public static String hashToString(byte[] hash) {
        BigInteger bi = new BigInteger(hash);
        if (bi.signum() < 0) bi = bi.xor(BigInteger.ONE.negate().shiftLeft(hash.length << 3));
        return bi.toString(16);
    }

    /**
  * Returns MD5 hash of the given string. Returns null, if the string is null.
  *
  * @param s - the string of which the hash has to be calculated.
  *
  * @return the MD5 hash, or null, if the string is null, or MD5 provider was 
  *   not found
  */
    public static byte[] hashString(String s) {
        if (s == null) return null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return md5.digest(s.getBytes());
        } catch (java.security.NoSuchAlgorithmException nse) {
            return null;
        }
    }

    /**
  * This method converts the given string to a canonical DN, then calculates its
  * hash using hashString. If the string is not a canonical DN, null will be
  * hashed (see hashString).
  *
  * @param name - the DN to canonicalise and hash
  *
  * @return the MD5 hash of the canonical RFC2253 DN
  */
    public static byte[] hashDN(String name) {
        try {
            name = issrg.utils.RFC2253NameParser.toCanonicalDN(issrg.utils.RFC2253NameParser.distinguishedName(name), false).toUpperCase();
        } catch (issrg.utils.RFC2253ParsingException rpe) {
            name = null;
        }
        return hashString(name);
    }

    /**
   * This is a utility method that returns the hash of the DN contained in the
   * GeneralNames.
   *
   * @param gn - the GeneralNames of which the directoryName will be converted
   *   to a canonical RFC2253-compliant LDAP distinguished name and then hashed
   */
    public static byte[] hashName(GeneralNames gn) {
        String name = Util.generalNamesToString(gn);
        return hashDN(name);
    }

    /**
     * Builds a date out of a string representation of it in date.
     *
     * @param date the string representation of the date in form
     *             "yyyy.mm.dd hh:mm:ss". The separators between numbers are defined by
     *            DATE_SEPARATOR and TIME_SEPARATOR respectively, the space between
     *      date and time is not redefinable. The lengths of numbers and ranges of
     *      their values are not checked (unless Generalized_Time or GregorianCalendar
     *    constructor does. More specific time values can be omitted. That is,
     *    "yyyy" is a valid value, and "yyyy.mm", and "yyyy.mm.dd hh", etc; but "yyyy.mm hh" is not:
     *    day must be specified first.
     * @return returns Generalized_Time object, which contains the date.
     * @throws ACCreationException if the string representation does not comply with
     *      the rules specified above.
     */
    public static issrg.ac.Generalized_Time buildGeneralizedTime(String date) throws ACCreationException {
        int yr = 0, mon = 1, day = 1;
        int hour = 0, min = 0, sec = 0;
        int i;
        String d = date + Util.DATE_SEPARATOR;
        try {
            i = d.indexOf(Util.DATE_SEPARATOR);
            yr = Integer.parseInt(d.substring(0, i));
            if ((d = d.substring(i + Util.DATE_SEPARATOR.length())).intern() != "") {
                i = d.indexOf(Util.DATE_SEPARATOR);
                mon = Integer.parseInt(d.substring(0, i));
                if ((d = d.substring(i + Util.DATE_SEPARATOR.length())).intern() != "") {
                    d = d.substring(0, d.length() - Util.DATE_SEPARATOR.length()) + " ";
                    i = d.indexOf(" ");
                    day = Integer.parseInt(d.substring(0, i));
                    if ((d = d.substring(i + 1)).intern() != "") {
                        d = d.substring(0, d.length() - 1) + Util.TIME_SEPARATOR;
                        i = d.indexOf(Util.TIME_SEPARATOR);
                        hour = Integer.parseInt(d.substring(0, i));
                        if ((d = d.substring(i + Util.TIME_SEPARATOR.length())).intern() != "") {
                            i = d.indexOf(Util.TIME_SEPARATOR);
                            min = Integer.parseInt(d.substring(0, i));
                            if ((d = d.substring(i + Util.TIME_SEPARATOR.length())).intern() != "") {
                                i = d.indexOf(Util.TIME_SEPARATOR);
                                sec = Integer.parseInt(d.substring(0, i));
                                if ((d = d.substring(i + Util.TIME_SEPARATOR.length())).intern() != "") {
                                    throw new NumberFormatException();
                                }
                            }
                        }
                    }
                }
            }
            java.util.GregorianCalendar gc = new java.util.GregorianCalendar(yr, mon - 1, day, hour, min, sec);
            gc.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
            return new issrg.ac.Generalized_Time(gc);
        } catch (NumberFormatException nfe) {
            throw new ACCreationException("Invalid date format in [" + date + "].\n[yyyy" + Util.DATE_SEPARATOR + "mm" + Util.DATE_SEPARATOR + "dd hh" + Util.TIME_SEPARATOR + "mm" + Util.TIME_SEPARATOR + "ss] was expected.");
        }
    }

    /**
   * This is the separator between the times. You can change it at runtime
   */
    public static String TIME_SEPARATOR = ":";

    /**
   * This is the separator between the serial number and the DN of the Issuer. You can change it at runtime
   */
    public static String SN_NAME_SEPARATOR = ";";

    /**
   * This is the separator between the dates. You can change it at runtime
   */
    public static String DATE_SEPARATOR = ".";

    /**
   * Converts the given date into internal format "ccyy.mm.dd hh:mm:ss". Actual
   * date and time separators are taken from the corresponding constants
   * DATE_SEPARATOR and TIME_SEPARATOR.
   *
   * @param date  the date to convert.
   * @return      returns the string representation of the date in format
   *              "ccyy.mm.dd hh:mm:ss"
   */
    public static String timeToString(java.util.Calendar date) {
        return date.get(date.YEAR) + Util.DATE_SEPARATOR + (date.get(date.MONTH) + 1) + Util.DATE_SEPARATOR + date.get(date.DAY_OF_MONTH) + " " + date.get(date.HOUR_OF_DAY) + Util.TIME_SEPARATOR + date.get(date.MINUTE) + Util.TIME_SEPARATOR + date.get(date.SECOND);
    }

    public static issrg.ac.IssuerSerial buildIssuerSerial(String what) throws ACCreationException {
        if (what.equals("")) return null;
        int i = what.indexOf(Util.SN_NAME_SEPARATOR);
        if (i < 1) {
            throw new ACCreationException("Illegal separator between Serial Number and Issuer Name: \"" + Util.SN_NAME_SEPARATOR + "\" was expected");
        }
        try {
            return new issrg.ac.IssuerSerial(buildGeneralNames(what.substring(i + 1)), new java.math.BigInteger(what.substring(0, i)), null);
        } catch (NumberFormatException nfe) {
            throw new ACCreationException("Issuer's Certificate Serial Number must be a valid Integer value");
        }
    }
}
