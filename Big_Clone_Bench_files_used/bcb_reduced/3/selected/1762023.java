package org.dbe.identity.utilities;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.dbe.identity.utilities.abstractClasses.ACharsAndBytesUtils;

/**
 * This class is a wrapper for a X509Certificate-Object. It provides
 * functionality to access easily the information stored in the given
 * certificate.<br />
 * <br />
 * This class is developed to free the user from the responsability to parse the
 * content of the given certificate. Instead, the user will get the needed
 * information as strings.
 * 
 * @see java.security.cert.X509Certificate
 * @author <a href="mailto:chatark@cs.tcd.ie">Khalid Chatar</a>
 * @author <a href="mailto:Dominik.Dahlem@cs.tcd.ie">Dominik Dahlem</a>
 */
public class X509CertificateValueParser {

    /**
     * the X509Certificate, which is to parse
     */
    protected X509Certificate m_objCertificate;

    /**
     * the DistinguishedName-Object for parsing an Issuer-DN
     */
    protected DNX500 m_objIssuerDNX500;

    /**
     * the DistinguishedName-Object for parsing an Subject-DN
     */
    protected DNX500 m_objSubjectDNX500;

    /**
     * The X509Certificate-method <b>getKeyUsage()</b> returns a boolean-field.
     * Every boolean has a special meaning.<br />
     * In this TreeMap you will find the meaning of each boolean in correct
     * order.
     * 
     * 0) Digital Signature 1) Non Repudiation 2) Key Encipherment 3) Data
     * Encipherment 4) Key Agreement 5) Key/Certificate Sign 6) Certificate
     * Revocation List Sign 7) Encipher Only 8) Decipher Only
     */
    protected TreeMap m_objKeyUsage;

    /**
     * An instance of this class needs an X509Certificate-Object. Thats why a
     * standard-constructor can not be provided.
     */
    private X509CertificateValueParser() {
    }

    /**
     * Constructor initializes the needed structures.
     * 
     * @param p_cert the X509Certificate-object
     */
    public X509CertificateValueParser(X509Certificate p_cert) {
        m_objCertificate = p_cert;
        m_objIssuerDNX500 = new DNX500(p_cert.getIssuerDN().toString());
        m_objSubjectDNX500 = new DNX500(p_cert.getSubjectDN().toString());
        m_objKeyUsage = new TreeMap();
        m_objKeyUsage.put(new Integer(0), "Digital Signature");
        m_objKeyUsage.put(new Integer(1), "Non Repudiation");
        m_objKeyUsage.put(new Integer(2), "Key Encipherment");
        m_objKeyUsage.put(new Integer(3), "Data Encipherment");
        m_objKeyUsage.put(new Integer(4), "Key Agreement");
        m_objKeyUsage.put(new Integer(5), "Key/Certificate Sign");
        m_objKeyUsage.put(new Integer(6), "Certificate Revocation List Sign");
        m_objKeyUsage.put(new Integer(7), "Encipher Only");
        m_objKeyUsage.put(new Integer(8), "Decipher Only");
    }

    /**
     * @return the value of the version of the given certificate as an <b>int</b>
     */
    public int getVersionAsInt() {
        return m_objCertificate.getVersion();
    }

    /**
     * @return the value of the version of the given certificate as a <b>String</b>
     */
    public String getVersionAsString() {
        return String.valueOf(m_objCertificate.getVersion());
    }

    /**
     * @return the serial of the given certificate as an <b>BigInteger</b>
     */
    public BigInteger getSerialAsBigInt() {
        return m_objCertificate.getSerialNumber();
    }

    /**
     * @return the serial of the given certificate as an <b>String</b>
     */
    public String getSerialAsString() {
        return m_objCertificate.getSerialNumber().toString();
    }

    /**
     * @return the OID of the signature-algorithm of the certificate
     */
    public String getSigAlgOID() {
        return m_objCertificate.getSigAlgOID();
    }

    /**
     * @return the name of the used signature-algorithm
     */
    public String getSigAlgName() {
        return m_objCertificate.getSigAlgName();
    }

    /**
     * @return the Distinguished Name of the Issuer
     */
    public String getIssuerDN() {
        return m_objIssuerDNX500.getDistinguishedName();
    }

    /**
     * Method parses the Issuer-DN and returns the attribute value or null. ","
     * is used as the delimiter between attributes.
     * 
     * @param p_attribute a special attribute you want to extract e.g.: "CN" for
     *            CommonName
     * 
     * @return the value of the given attribute or null if attribute not found
     */
    public String getAttributeOfIssuerDN(String p_attribute) {
        return DNX500.getPartOfDistinguishedName(m_objIssuerDNX500.getDistinguishedName(), p_attribute, ',');
    }

    /**
     * Method parses the Issuer-DN and returns the attribute value or null.
     * 
     * @param p_attribute a special attribute you want to extract e.g.: "CN" for
     *            CommonName
     * @param p_delimiter the delimiter, which is used to separate the
     *            attributes
     * 
     * @return the value of the given attribute or null if attribute not found
     */
    public String getAttributeOfIssuerDN(String p_attribute, char p_delimiter) {
        return DNX500.getPartOfDistinguishedName(m_objIssuerDNX500.getDistinguishedName(), p_attribute, p_delimiter);
    }

    /**
     * Method parses the Issuer-DN and returns the attribute value or null. ","
     * is used as the delimiter between attributes.<br />
     * <b>Use the constants in the class DNX500Constants!</b>
     * 
     * @see DNX500Constants
     * @param p_attribute a special attribute you want to extract, e.g.:
     *            DNX500Constants.CN for CommonName
     * 
     * @return the value of the given attribute or null if attribute not found
     */
    public String getAttributeOfIssuerDN(int p_attribute) {
        return DNX500.parseDistinguishedName(m_objIssuerDNX500.getDistinguishedName(), p_attribute, ',');
    }

    /**
     * Method parses the Issuer-DN and returns the attribute value or null.
     * <b>Use the constants in the class DNX500Constants!</b>
     * 
     * @see DNX500Constants
     * @param p_attribute a special attribute you want to extract, e.g.:
     *            DNX500Constants.CN for CommonName
     * 
     * @return the value of the given attribute or null if attribute not found
     */
    public String getAttributeOfIssuerDN(int p_attribute, char p_delimiter) {
        return DNX500.parseDistinguishedName(m_objIssuerDNX500.getDistinguishedName(), p_attribute, p_delimiter);
    }

    /**
     * @return the Distinguished Name of the Subject
     */
    public String getSubjectDN() {
        return m_objSubjectDNX500.getDistinguishedName();
    }

    /**
     * Method parses the Subject-DN and returns the attribute value or null. ","
     * is used as the delimiter between attributes.
     * 
     * @param p_attribute a special attribute you want to extract e.g.: "CN" for
     *            CommonName
     * 
     * @return the value of the given attribute or null if attribute not found
     */
    public String getAttributeOfSubjectDN(String p_attribute) {
        return DNX500.getPartOfDistinguishedName(m_objSubjectDNX500.getDistinguishedName(), p_attribute, ',');
    }

    /**
     * Method parses the Subject-DN and returns the attribute value or null.
     * 
     * @param p_attribute a special attribute you want to extract e.g.: "CN" for
     *            CommonName
     * @param p_delimiter the delimiter, which is used to separate the
     *            attributes
     * 
     * @return the value of the given attribute or null if attribute not found
     */
    public String getAttributeOfSubjectDN(String p_attribute, char p_delimiter) {
        return DNX500.getPartOfDistinguishedName(m_objSubjectDNX500.getDistinguishedName(), p_attribute, p_delimiter);
    }

    /**
     * Method parses the Subject-DN and returns the attribute value or null. ","
     * is used as the delimiter between attributes.<br />
     * <b>Use the constants in the class DNX500Constants!</b>
     * 
     * @see DNX500Constants
     * @param p_attribute a special attribute you want to extract, e.g.:
     *            DNX500Constants.CN for CommonName
     * 
     * @return the value of the given attribute or null if attribute not found
     */
    public String getAttributeOfSubjectDN(int p_attribute) {
        return DNX500.parseDistinguishedName(m_objSubjectDNX500.getDistinguishedName(), p_attribute, ',');
    }

    /**
     * Method parses the Subject-DN and returns the attribute value or null.
     * <b>Use the constants in the class DNX500Constants!</b>
     * 
     * @see DNX500Constants
     * @param p_attribute a special attribute you want to extract, e.g.:
     *            DNX500Constants.CN for CommonName
     * 
     * @return the value of the given attribute or null if attribute not found
     */
    public String getAttributeOfSubjectDN(int p_attribute, char p_delimiter) {
        return DNX500.parseDistinguishedName(m_objSubjectDNX500.getDistinguishedName(), p_attribute, p_delimiter);
    }

    /**
     * Returns the date (NotBefore) as a String-Value in this format:
     * yyyy-MM-dd. Uses the SimpleDateFormat-Class.
     * 
     * @return the date (NotBefore) as a String-Value
     */
    public String getNotBefore() {
        return getNotBefore(Validator.m_strDateFormat);
    }

    /**
     * Returns the date (NotBefore) as a String-Value in this format:
     * <b>p_dateFormat</b>. Uses the SimpleDateFormat-Class.
     * 
     * @param p_dateFormat the format string, which is used for the
     *            SimpleDateFormat-class to format the date
     * 
     * @return the date (NotBefore) as a String-Value
     */
    public String getNotBefore(String p_dateFormat) {
        return new SimpleDateFormat(p_dateFormat).format(m_objCertificate.getNotBefore());
    }

    /**
     * Returns the date (NotAfter) as a String-Value in this format: yyyy-MM-dd.
     * Uses the SimpleDateFormat-Class.
     * 
     * @return the date (NotAfter) as a String-Value
     */
    public String getNotAfter() {
        return getNotAfter(Validator.m_strDateFormat);
    }

    /**
     * Returns the date (NotAfter) as a String-Value in this format:
     * <b>p_dateFormat</b>. Uses the SimpleDateFormat-Class.
     * 
     * @param p_dateFormat the format string, which is used for the
     *            SimpleDateFormat-class to format the date
     * 
     * @return the date (NotAfter) as a String-Value
     */
    public String getNotAfter(String p_dateFormat) {
        return new SimpleDateFormat(p_dateFormat).format(m_objCertificate.getNotAfter());
    }

    /**
     * @return the algorithm name, which is used for this Public Key
     */
    public String getPublicKeyAlgorithm() {
        return m_objCertificate.getPublicKey().getAlgorithm();
    }

    /**
     * Returns the key in its primary encoding format, or null if this key does
     * not support encoding
     * 
     * @return the encoded key, or null if the key does not support encoding
     */
    public byte[] getPublicKeyAsBytes() {
        return m_objCertificate.getPublicKey().getEncoded();
    }

    /**
     * Turns the byte-field into it's String-representation. E.g.: 10011100
     * 11001001 ---> 9C:C9 ":" is used for separating. After <b>p_bytesPerLine</b>
     * bytes, a new String will begin. This means, that in every String in the
     * String-Array, you will find at most <b>p_bytesPerLine</b> bytes.
     * 
     * @param p_bytesPerLine how much bytes per String
     * 
     * @return a String field, with the string-representation of the byte Array
     *         of the Public Key
     */
    public String[] getPublicKeyAsStringLines(int p_bytesPerLine) {
        return ACharsAndBytesUtils.bytesToStringRepresentation(m_objCertificate.getPublicKey().getEncoded(), ':', p_bytesPerLine);
    }

    /**
     * Turns the byte-field into it's String-representation. E.g.: 10011100
     * 11001001 ---> 9C:C9 <b>p_delimiter</B> is used for separating. After
     * <b>p_bytesPerLine</b> bytes, a new String will begin. This means, that
     * in every String in the String-Array, you will find at most
     * <b>p_bytesPerLine</b> bytes.
     * 
     * @param p_delimiter the delimiter for separating the bytes
     * @param p_bytesPerLine how much bytes per String
     * 
     * @return a String field, with the string-representation of the byte Array
     *         of the Public Key
     */
    public String[] getPublicKeyAsStringLines(char p_delimiter, int p_bytesPerLine) {
        return ACharsAndBytesUtils.bytesToStringRepresentation(m_objCertificate.getPublicKey().getEncoded(), p_delimiter, p_bytesPerLine);
    }

    /**
     * @return the signature as a byte-array
     */
    public byte[] getSignatureAsBytes() {
        return m_objCertificate.getSignature();
    }

    /**
     * Turns the byte-field into it's String-representation. E.g.: 10011100
     * 11001001 ---> 9C:C9 ":" is used for separating. After <b>p_bytesPerLine</b>
     * bytes, a new String will begin. This means, that in every String in the
     * String-Array, you will find at most <b>p_bytesPerLine</b> bytes.
     * 
     * @param p_bytesPerLine how much bytes per String
     * 
     * @return a String field, with the string-representation of the byte Array
     *         of the Signature
     */
    public String[] getSignatureAsStringLines(int p_bytesPerLine) {
        return getSignatureAsStringLines(':', p_bytesPerLine);
    }

    /**
     * Turns the byte-field into it's String-representation. E.g.: 10011100
     * 11001001 ---> 9C:C9 <b>p_delimiter</B> is used for separating. After
     * <b>p_bytesPerLine</b> bytes, a new String will begin. This means, that
     * in every String in the String-Array, you will find at most
     * <b>p_bytesPerLine</b> bytes.
     * 
     * @param p_delimiter the delimiter for separating the bytes
     * @param p_bytesPerLine how much bytes per String
     * 
     * @return a String field, with the string-representation of the byte Array
     *         of the Signature
     */
    public String[] getSignatureAsStringLines(char p_delimiter, int p_bytesPerLine) {
        return ACharsAndBytesUtils.bytesToStringRepresentation(m_objCertificate.getSignature(), p_delimiter, p_bytesPerLine);
    }

    /**
     * Returns the fingerprint of the Certificate.
     * 
     * @param p_algorithm the algorithm, which has to be used for the
     *            fingerprint (e.g.: SHA-1 or MD5)
     * 
     * @return the fingerprint of the certificate as a byte field
     * 
     * @throws NoSuchAlgorithmException if the algorithm is not available in the
     *             caller's environment.
     * @throws CertificateEncodingException if an encoding error occurs
     */
    public byte[] getFingerprintOfCertificate(String p_algorithm) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance(p_algorithm);
        md.update(m_objCertificate.getEncoded());
        return md.digest();
    }

    /**
     * Uses <b>getFingerprintOfCertificate(String p_algorithm)</b> to get an
     * SHA1-Fingerprint and changes the byte field to it's
     * string-representation. ":" is used for separating E.g.: 10011100 11001001
     * ---> 9C:C9
     * 
     * @param p_bytesPerLine how much bytes per String
     * 
     * @return the string representation of the SHA1-Fingerprint
     * @throws NoSuchAlgorithmException if the algorithm is not available in the
     *             caller's environment.
     * @throws CertificateEncodingException if an encoding error occurs
     */
    public String[] getSHA1FingerprintOfCertificateAsString(int p_bytesPerLine) throws CertificateEncodingException, NoSuchAlgorithmException {
        return getSHA1FingerprintOfCertificateAsString(':', p_bytesPerLine);
    }

    /**
     * Uses <b>getFingerprintOfCertificate(String p_algorithm)</b> to get an
     * SHA1-Fingerprint and changes the byte field to it's
     * string-representation. E.g.: 10011100 11001001 ---> 9C:C9
     * 
     * @param p_delimiter the delimiter for separating the bytes
     * @param p_bytesPerLine how much bytes per String
     * 
     * @return the string representation of the SHA1-Fingerprint
     * @throws NoSuchAlgorithmException if the algorithm is not available in the
     *             caller's environment.
     * @throws CertificateEncodingException if an encoding error occurs
     */
    public String[] getSHA1FingerprintOfCertificateAsString(char p_delimiter, int p_bytesPerLine) throws CertificateEncodingException, NoSuchAlgorithmException {
        return ACharsAndBytesUtils.bytesToStringRepresentation(getFingerprintOfCertificate("SHA-1"), p_delimiter, p_bytesPerLine);
    }

    /**
     * Uses <b>getFingerprintOfCertificate(String p_algorithm)</b> to get an
     * MD5-Fingerprint and changes the byte field to it's string-representation.
     * ":" is used for separating E.g.: 10011100 11001001 ---> 9C:C9
     * 
     * @param p_bytesPerLine how much bytes per String
     * 
     * @return the string representation of the MD5-Fingerprint
     * @throws NoSuchAlgorithmException if the algorithm is not available in the
     *             caller's environment.
     * @throws CertificateEncodingException if an encoding error occurs
     */
    public String[] getMD5FingerprintOfCertificateAsString(int p_bytesPerLine) throws CertificateEncodingException, NoSuchAlgorithmException {
        return getSHA1FingerprintOfCertificateAsString(':', p_bytesPerLine);
    }

    /**
     * Uses <b>getFingerprintOfCertificate(String p_algorithm)</b> to get an
     * MD5-Fingerprint and changes the byte field to it's string-representation.
     * E.g.: 10011100 11001001 ---> 9C:C9
     * 
     * @param p_delimiter the delimiter for separating the bytes
     * @param p_bytesPerLine how much bytes per String
     * 
     * @return the string representation of the MD5-Fingerprint
     * @throws NoSuchAlgorithmException if the algorithm is not available in the
     *             caller's environment.
     * @throws CertificateEncodingException if an encoding error occurs
     */
    public String[] getMD5FingerprintOfCertificateAsString(char p_delimiter, int p_bytesPerLine) throws CertificateEncodingException, NoSuchAlgorithmException {
        return ACharsAndBytesUtils.bytesToStringRepresentation(getFingerprintOfCertificate("MD5"), p_delimiter, p_bytesPerLine);
    }

    /**
     * Checks the validity of an certificate. If all is OK, than nothing
     * happens. Otherwise one of two Exceptions will be thrown.
     * 
     * @throws CertificateExpiredException Certificate expired
     * @throws CertificateNotYetValidException Certificate is not yet valid
     */
    public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
        m_objCertificate.checkValidity();
    }

    /**
     * @return true, if certificate is valid, otherwise false
     */
    public boolean isValid() {
        try {
            m_objCertificate.checkValidity();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * @return true, if the certificate is expired, otherwise false
     */
    public boolean isExpired() {
        try {
            m_objCertificate.checkValidity();
        } catch (CertificateExpiredException e) {
            return true;
        } catch (CertificateNotYetValidException e) {
        }
        return false;
    }

    /**
     * @return true, if certificate is not yet valid, otherwise false
     */
    public boolean isNotYetValid() {
        try {
            m_objCertificate.checkValidity();
        } catch (CertificateExpiredException e) {
        } catch (CertificateNotYetValidException e) {
            return true;
        }
        return false;
    }

    /**
     * For more information about the meaning of the boolean field see the
     * javadoc for java.security.cert.X509Certificate.getKeyUsage().
     * 
     * @return a boolean field, which describes, for what the key is only to use
     * @see java.security.cert.X509Certificate.getKeyUsage()
     */
    public boolean[] getKeyUsage() {
        return m_objCertificate.getKeyUsage();
    }

    /**
     * Interpret the boolean field of <b>getKeyUsage()</b> and returns the
     * strings, which describes for what the key is allowed to use.
     * 
     * @return a string field with the description of key usage
     */
    public String[] getKeyUsageAsStrings() {
        boolean[] keyUsage = getKeyUsage();
        if (keyUsage != null) {
            ArrayList result = new ArrayList();
            for (int i = 0; i < keyUsage.length; i++) {
                if (keyUsage[i]) result.add((String) m_objKeyUsage.get(new Integer(i)));
            }
            return (String[]) result.toArray(new String[] {});
        }
        return null;
    }

    /**
     * @see java.security.cert.X509Certificate.getExtendedKeyUsage()
     * 
     * @return unmodifiable List of String or null
     */
    public List getKeyUsageExt() {
        try {
            return m_objCertificate.getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            return null;
        }
    }

    /**
     * @see java.security.cert.X509Certificate.getExtendedKeyUsage()
     * 
     * @return String field of the keuUsageExtensions or null
     */
    public String[] getKeyUsageExtAsStrings() {
        List keyUsage = getKeyUsageExt();
        if (keyUsage != null) {
            String[] result = new String[keyUsage.size()];
            for (int i = 0; i < keyUsage.size(); i++) {
                result[i] = (String) keyUsage.get(i);
            }
            return result;
        }
        return null;
    }

    /**
     * @see java.security.cert.X509Certificate.getBasicConstraints()
     * 
     * @return an int, which describes the depth of pathLength
     */
    public int getBasicConstraints() {
        return m_objCertificate.getBasicConstraints();
    }

    /**
     * Interpret the int of <b>getBasicConstraints()</b> and returns a String
     * description for the BasicContraints.
     * 
     * @return string-field with the description of the BasicConstraints
     */
    public String[] getBasicConstraintsAsStrings() {
        int basicConstraint = getBasicConstraints();
        ArrayList result = new ArrayList();
        if (basicConstraint > -1) {
            result.add("Subject is a CA");
            if (basicConstraint == Integer.MAX_VALUE) result.add("No limit for certification path length"); else if (basicConstraint == 0) result.add("Only an end-entity certificate may follow in the path"); else result.add("Limit for certification path length is " + basicConstraint);
        } else {
            result.add("Subject is not a CA");
        }
        return (String[]) result.toArray(new String[] {});
    }

    public String[] getAlterNativeNames(Collection p_altNames) {
        String nameTypePattern = "1|2|4|6|7|8";
        Collection coll = new ArrayList();
        if (p_altNames != null && !p_altNames.isEmpty()) {
            for (Iterator it = p_altNames.iterator(); it.hasNext(); ) {
                List entry = (List) it.next();
                Integer first = (Integer) entry.get(0);
                Object second = entry.get(1);
                if (first.toString().matches(nameTypePattern)) coll.add((String) second); else {
                    byte[] derEncoded = (byte[]) second;
                    coll.add(ACharsAndBytesUtils.bytesToStringRepresentation(derEncoded, ':', derEncoded.length));
                }
            }
        } else return null;
        return (String[]) coll.toArray(new String[] {});
    }

    public Collection getIssuerAlternativeNames() throws CertificateParsingException {
        return m_objCertificate.getIssuerAlternativeNames();
    }

    public String[] getIssuerAlternativeNamesAsStrings() throws CertificateParsingException {
        return getAlterNativeNames(m_objCertificate.getIssuerAlternativeNames());
    }

    public Collection getSubjectAlternativeNames() throws CertificateParsingException {
        return m_objCertificate.getSubjectAlternativeNames();
    }

    public String[] getSubjectAlternativeNamesAsStrings() throws CertificateParsingException {
        return getAlterNativeNames(m_objCertificate.getSubjectAlternativeNames());
    }

    public boolean isOIDCritical(String p_oid) {
        Set oids = m_objCertificate.getCriticalExtensionOIDs();
        if (null != oids) return oids.contains(p_oid);
        return false;
    }

    public byte[] getExtensionValue(String p_oid) {
        return m_objCertificate.getExtensionValue(p_oid);
    }
}
