package issrg.dis;

import issrg.acm.ACCreationException;
import issrg.aef.SamplePKI;
import issrg.pba.rbac.LDAPDNPrincipal;
import issrg.pba.rbac.policies.*;
import issrg.pba.repository.*;
import java.util.Map;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import issrg.pba.rbac.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import iaik.asn1.*;
import iaik.asn1.structures.*;
import issrg.acm.IllegalInputException;
import issrg.utils.RFC2253ParsingException;
import issrg.utils.Base64;
import issrg.pba.rbac.SetOfSubsetsCredentials;
import issrg.pba.rbac.ExpirableCredentials;
import issrg.pba.rbac.AbsoluteValidityPeriod;
import javax.naming.*;
import javax.naming.directory.*;
import javax.servlet.http.*;
import issrg.ac.*;
import issrg.ac.attributes.*;

/**
 *  	This is the DIS java object. This object is used
 *	to check and issue ACs on behalf of other 
 *	managers.
 * 
 *	<p>DIS java object will use PERMIS PDP to 
 *	make sure that the issuer (manager) have enough 
 * 	privileges to issue an AC and the issuance of that AC
 *	must conform to delegation policy
 */
public class DISTest {

    private static final String config_file = "distest.cfg";

    private static final String serial_file = "serialnumber.cfg";

    private static String AC_attribute;

    private static final String SEPARATOR = "|";

    protected static final int CREDS = 0;

    private static final int RULES = 1;

    private static String PKC;

    private static String SOA;

    private static String DIS;

    private static String OID;

    private static String LDAP;

    private static String rootDN;

    private static String rootPass;

    private static boolean downgradeable;

    private static String SIGN_KEY_PATH;

    private static String SIGN_KEY_PASSWORD;

    private static String DIS_AC_LOCATION;

    private int TIME_OUT;

    private static final String DISSERIAL = "serial";

    private static final String DISRULES = "rules";

    private static issrg.utils.repository.AttributeRepository r;

    private iaik.x509.X509Certificate x509;

    private issrg.security.DefaultSecurity ds;

    private issrg.pba.rbac.SignatureVerifier sv = new SamplePKI();

    private issrg.pba.rbac.PermisRBAC pbaApi;

    private LDAPUtility ldapUtility;

    private issrg.security.DefaultSecurity signingUtility = new issrg.security.DefaultSecurity();

    private Map roleTypes;

    private static final String NO_DELEGATION = "-1";

    private boolean verbose;

    private issrg.pba.PolicyParser pp;

    Map serialAndRules = new Hashtable();

    issrg.ac.attributes.AttributeAuthorityInformationAccess aaia;

    Vector trustedProxy = new Vector();

    /**
    *	This is the constructor of the DIS java object. It
    *	read necessary parameters from config file, initialize
    *	LdapUtility object, Attribute Certificate repository and 
    *	signing utility
    */
    public DISTest() {
        try {
            readConfigFile();
            SOA = issrg.utils.RFC2253NameParser.toCanonicalDN(SOA);
            if (SOA == null) {
                System.out.println("Error with SOA's DN");
                System.exit(-1);
            }
            LDAPUtility.AC_attribute = AC_attribute;
            ldapUtility = new LDAPUtility(LDAP, rootDN, rootPass);
            r = new issrg.utils.repository.LDAPRepository(ldapUtility.getLdaps());
            x509 = new iaik.x509.X509Certificate(new java.io.FileInputStream(PKC));
            ds = new issrg.security.DefaultSecurity();
            ds.setRootCA(x509);
            ds.setPKCRepository(new issrg.security.PKCRepository(r));
            sv = new issrg.pba.rbac.SimpleSignatureVerifier(ds);
            pbaApi = new PermisRBAC(new iaik.asn1.ObjectID(OID), new LDAPDNPrincipal(SOA), r, sv, TIME_OUT);
            readRoleTypes();
            signingUtility.login(SIGN_KEY_PATH, SIGN_KEY_PASSWORD);
            DIS = issrg.utils.RFC2253NameParser.toCanonicalDN(signingUtility.getVerificationCertificate().getSubjectDN().getName()).toUpperCase();
            if (DIS == null) {
                System.out.println("Error with DIS's signing key: DN of the key's holder is in correct format");
                System.exit(-1);
            }
            getDISSerialAndRules(serialAndRules);
            SEQUENCE extnV = new SEQUENCE();
            SEQUENCE accessDesc = new SEQUENCE();
            accessDesc.addComponent(new ObjectID(issrg.ac.attributes.AttributeAuthorityInformationAccess.id_ad_caIssuer), 0);
            GeneralName name = new GeneralName(GeneralName.uniformResourceIdentifier, DIS_AC_LOCATION);
            accessDesc.addComponent(name.toASN1Object(), 1);
            extnV.addComponent(accessDesc);
            aaia = new issrg.ac.attributes.AttributeAuthorityInformationAccess(extnV);
        } catch (Throwable th) {
            if (verbose) th.printStackTrace();
        }
    }

    /**
     * This function retrieves all the ACs of DIS and constructs RARs for DIS and a vector of serial number of DIS's AC
     */
    private void getDISSerialAndRules(Map serialAndRules) {
        Vector serial, rules;
        serial = new Vector();
        rules = new Vector();
        serialAndRules.put(DISSERIAL, serial);
        serialAndRules.put(DISRULES, rules);
        try {
            javax.naming.directory.DirContext[] ldaps = new javax.naming.directory.DirContext[1];
            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, DIS_AC_LOCATION);
            env.put("java.naming.ldap.version", "3");
            env.put("java.naming.ldap.attributes.binary", issrg.pba.repository.ACRepository.ATTRIBUTE_CERTIFICATE_ID);
            env.put(Context.SECURITY_AUTHENTICATION, "none");
            ldaps[0] = new InitialDirContext(env);
            issrg.utils.repository.LDAPRepository repository = new issrg.utils.repository.LDAPRepository(ldaps);
            issrg.security.DefaultSecurity dstemp = new issrg.security.DefaultSecurity();
            dstemp.setRootCA(x509);
            dstemp.setPKCRepository(new issrg.security.PKCRepository(repository));
            issrg.pba.rbac.SimpleSignatureVerifier simpleVerifier = new issrg.pba.rbac.SimpleSignatureVerifier(dstemp);
            AuthTokenRepository optionalACR = new issrg.pba.rbac.ACRepositoryWithPKI(repository, sv);
            javax.naming.directory.Attribute dISAttrs = ((issrg.pba.rbac.ACRepositoryWithPKI) optionalACR).getACs(new LDAPDNPrincipal(DIS));
            int num = dISAttrs.size();
            for (int i = 0; i < num; i++) {
                byte[] acObject = (byte[]) dISAttrs.get(i);
                issrg.ac.AttributeCertificate ac = issrg.ac.AttributeCertificate.guessEncoding(acObject);
                String issuer = issrg.ac.Util.generalNamesToString(ac.getACInfo().getIssuer().getV1Form() == null ? ac.getACInfo().getIssuer().getV2Form().getIssuerName() : ac.getACInfo().getIssuer().getV1Form()).toLowerCase();
                if (!issrg.utils.RFC2253NameParser.toCanonicalDN(issuer).toUpperCase().equals(SOA)) continue;
                serial.add(ac.getACInfo().getSerialNumber());
                issrg.pba.Subject dis = pbaApi.getCreds(new LDAPDNPrincipal(DIS), new Object[] { acObject }, null);
                Vector a = (Vector) ((PermisSubject) dis).exportRules();
                if ((a != null) && (a.size() > 0)) {
                    rules.addAll(a);
                }
            }
        } catch (Exception e) {
            if (verbose) e.printStackTrace();
        }
    }

    /** 
     * This function read roletypes from the policy. The roleTypes map is used for reference later.
     */
    private void readRoleTypes() {
        pp = ((PermisRBAC) pbaApi).getParsedPolicy();
        RoleHierarchyPolicy roleHierarchyPolicy = (issrg.pba.rbac.xmlpolicy.XMLPolicyParser.RoleHierarchyPolicyNode) ((issrg.pba.rbac.xmlpolicy.XMLPolicyParser) pp).getAuthTokenParsingRules().get(issrg.pba.rbac.RoleHierarchyPolicy.class);
        roleTypes = new Hashtable(roleHierarchyPolicy.getTypeOid());
        String[] oids = (String[]) roleTypes.values().toArray(new String[0]);
        for (int i = 0; i < oids.length; i++) {
            issrg.ac.attributes.PermisRole.registerMe(oids[i]);
        }
    }

    /**
     * This method reads configuration parameters from configuration file for DIS.     
     */
    private void readConfigFile() throws Exception {
        java.net.URL configURL = this.getClass().getResource(config_file);
        if (configURL == null) {
            System.out.println("Config file not found " + config_file);
            System.exit(-1);
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(configURL.getFile())));
            String line;
            String left, right;
            int i;
            while ((line = in.readLine()) != null) {
                line = line.intern();
                if (line == "") continue;
                i = line.indexOf(" ");
                if (i < 0) {
                    System.out.println("Unrecognised line ignored: " + line);
                    continue;
                }
                left = line.substring(0, i).toLowerCase().trim().intern();
                right = line.substring(i + 1).trim().intern();
                if (left.charAt(0) == '#') {
                    continue;
                } else if (left == "soa") {
                    SOA = right.toUpperCase();
                } else if (left == "policyoid") {
                    OID = right;
                } else if (left == "pkc") {
                    PKC = right;
                } else if (left == "ldap") {
                    LDAP = right;
                } else if (left == "downgradeable") {
                    downgradeable = right == "1";
                } else if (left == "rootldap") {
                    rootDN = right;
                } else if (left == "passwordldap") {
                    rootPass = right;
                } else if (left == "signkeypath") {
                    SIGN_KEY_PATH = right;
                } else if (left == "signkeypassword") {
                    SIGN_KEY_PASSWORD = right;
                } else if (left == "ldapattributecertificateattribute") {
                    AC_attribute = right;
                } else if (left == "attributelocation") {
                    DIS_AC_LOCATION = right;
                } else if (left == "verbose") {
                    verbose = (right == "true");
                } else if (left == "trusted") {
                    trustedProxy.add(issrg.utils.RFC2253NameParser.toCanonicalDN(right));
                } else if (left == "timeout") {
                    TIME_OUT = (new Integer(right)).intValue();
                } else {
                    System.out.println("Unrecognised line ignored: " + line);
                }
            }
        } catch (FileNotFoundException fnfe) {
            throw new FileNotFoundException("Config file not found" + config_file);
        } catch (IOException ioe) {
            throw new IOException("Error reading configuration file");
        }
    }

    /**
    *	This function will receive request of issuing AC from Apache Webserver
    *	
    *	@param issuerUID is the UID of the issuer and it is used when checking 
    *	privileges of issuer compare to privileges of the AC to-be-sign
    *	@param holderDN is the intended holder of the AC to-be-sign
    *	@param roleValues is the array of role values that the issuer
    *	wants to issue to the holder
    *	@param roleType is the type of role that the issuer wants to delegate to
    *	the holder
    *	@param from is the starting time of validity period of the AC
    *	@param to is the ending time of validity period of the AC
    *	@param assertion states whether or not the holder of the AC can assert 
    *	privileges in this AC. If this parameter equals to "can" then the holder
    *	can assert privileges in this AC. Otherwise, the holder can not assert
    *	privileges in this AC.
    *	@param deep is capability of further delegation for the holder. The value "-1"
    *	means the holder can not delegate privileges in this AC to anyone. The value
    *	"1" means the holder can delegate privileges in this AC just downto one level.
    *	"2" means the holder can delegate privileges in this AC just downto two level.
    *	"0" means the holder can delegate privileges in this AC downto unlimited level.
    *
    *	<p> Note that the above parameters mean the issuer intend to issue one AC
    * 	with these properties. These properties could be changed by the DIS java object
    *	when checking the issuer's privileges and checking with delegation policy.
    */
    public String[] signForMe(String issuerDN, String holderDN, String[] roleValues, String roleType, String from, String to, String assertion, String deep) {
        try {
            ValidityPeriod vp;
            issrg.ac.Generalized_Time notBf = issrg.acm.Util.buildGeneralizedTime(from);
            issrg.ac.Generalized_Time notAt = issrg.acm.Util.buildGeneralizedTime(to);
            Date notBefore = notBf.getTime().getTime();
            Date notAfter = notAt.getTime().getTime();
            vp = new AbsoluteValidityPeriod(notBefore, notAfter);
            String message = new String();
            Vector roles = new Vector();
            int len = roleValues.length;
            for (int i = 0; i < len; i++) {
                roles.add(roleValues[i]);
            }
            boolean canAssert;
            canAssert = assertion.equals("can");
            try {
                int temp;
                temp = Integer.parseInt(deep);
                if (temp <= -1) {
                    deep = NO_DELEGATION;
                }
            } catch (NumberFormatException nfe) {
                deep = NO_DELEGATION;
            }
            Map roleTypesValues = new Hashtable();
            roleTypesValues.put(roleType, roles);
            Map holderDetails = new Hashtable();
            holderDetails.put(HolderDetails.ISSUER_DN, issuerDN);
            holderDetails.put(HolderDetails.HOLDER_DN, holderDN);
            holderDetails.put(HolderDetails.ROLE_TYPES_VALUES, roleTypesValues);
            holderDetails.put(HolderDetails.VALIDITY_PERIOD, vp);
            holderDetails.put(HolderDetails.ASSERTION, new Boolean(canAssert));
            holderDetails.put(HolderDetails.DELEGATION_DEPTH, deep);
            holderDetails.put(HolderDetails.HOLDER_CREDS, new SetOfSubsetsCredentials());
            holderDetails.put(HolderDetails.NOT_BEFORE, notBf);
            holderDetails.put(HolderDetails.NOT_AFTER, notAt);
            holderDetails.put(HolderDetails.MESSAGE, message);
            holderDetails.put(HolderDetails.ISSUER_NAME, new Boolean(true));
            holderDetails.put(HolderDetails.ISSUER_BCID, new Boolean(false));
            return delegateAndPublish(holderDetails, false);
        } catch (ACCreationException ace) {
            return new String[] { Comm.ACCREATION_ERROR, new String() };
        }
    }

    /**
     * This function checks whether the proxy signing is trusted by the DIS. When DIS initializes, it reads all the trusted proxy
     * signing and stores in a vector. The requested proxy signing must be one of the trusted proxy signing.
     */
    private boolean checkProxy(String proxyDN) {
        for (int i = 0; i < trustedProxy.size(); i++) {
            String proxy = ((String) trustedProxy.get(i)).toUpperCase();
            if (proxy.equals(proxyDN.toUpperCase())) return true;
        }
        return false;
    }

    /**
     * This method will check if delegation is possible, constrain the values, if it can, and publish the AC, if requested. 
     * This method is called from two places after the holder Details have been collected - either from the AC, or from the PHP script.
     * 
     * @param holderDetails - the Map of details of the requested delegation
     * @param autoSave - the flag requesting the DIS to publish the AC; if true, the DIS will publish it into the LDAP directory that has been
     *    configured in; if false, the DIS will not publish it, and return the Base64-encoded AC as the second string
     *
     * @return an array of strings of size 1 or 2. The first string is always a message - either an approval of the delegation, or 
     *  an error diagnostic message; the second String is a Base64-encoded AC, and is only present if autoSave is false (no publishing requested).
     */
    private String[] delegateAndPublish(Map holderDetails, boolean autoSave) {
        if (checkIssuer(holderDetails) && checkAgainstRAP(holderDetails)) {
            if (publish(holderDetails, !autoSave)) {
                String done = prepareReturn(holderDetails);
                String[] result = new String[] { done };
                if (!autoSave) {
                    result = new String[] { result[0], Base64.encodeBytes((byte[]) holderDetails.get(HolderDetails.ATTRIBUTE_CERTIFICATE)) };
                }
                return result;
            }
        }
        return new String[] { (String) holderDetails.get(HolderDetails.MESSAGE) };
    }

    /**
     * This method try to generate AC and publish it
     */
    private boolean publish(Map holderDetails, boolean autoSave) {
        try {
            if (generateAC(holderDetails)) {
                if (autoSave) storeToLDAP(holderDetails);
                return true;
            }
        } catch (ACCreationException acce) {
            if (verbose) acce.printStackTrace();
        }
        return false;
    }

    /**
     * This function read the a string from configuration file and create a 
     * serial number from that string and create another string and write to
     * configuration file. This method needs to be synchonized because there
     * are many threat trying to issue AC so the serial number needs to be unique.
     *
     * @return a string represented for serial number. If there is an error, it will return null.
     */
    private synchronized String getSerialNumber() {
        java.net.URL file = this.getClass().getResource(serial_file);
        if (file == null) {
            if (verbose) System.out.println("Config file not found " + serial_file);
        }
        try {
            BufferedReader in = new BufferedReader(new java.io.FileReader(file.getFile()));
            String serial = in.readLine();
            if (serial == null) serial = "" + System.currentTimeMillis();
            String s;
            MessageDigest md = MessageDigest.getInstance("SHA");
            BigInteger bi = new BigInteger(serial, 16);
            md.update(bi.toByteArray());
            byte[] result = md.digest();
            BigInteger bir = new BigInteger(result);
            bir = bir.abs();
            s = bir.toString(16);
            File outfile = new File(file.getFile());
            FileOutputStream f = new FileOutputStream(outfile);
            f.write(s.getBytes());
            f.close();
            return s;
        } catch (FileNotFoundException fnfe) {
            if (verbose) System.out.println("Config file not found " + serial_file);
        } catch (IOException ioe) {
            if (verbose) ioe.printStackTrace();
        } catch (NoSuchAlgorithmException nsae) {
            if (verbose) nsae.printStackTrace();
        }
        return null;
    }

    /**
     * This function generate an AC from holderDetail map.
     * @param holderDetails is one hashtable that stores all information of the AC after constrains with issuer's rules and DIS's rules.
     *
     * @return true if the function can create an AC out of the map. Otherwise, it will return false.
     */
    private boolean generateAC(Map holderDetails) throws ACCreationException {
        BigInteger ACSerialNumber;
        ACSerialNumber = new BigInteger(this.getSerialNumber(), 16);
        ACSerialNumber = ACSerialNumber.abs();
        Boolean issuer_name = (Boolean) holderDetails.get(HolderDetails.ISSUER_NAME);
        Boolean issuer_bcid = (Boolean) holderDetails.get(HolderDetails.ISSUER_BCID);
        if ((!issuer_name.booleanValue() && !issuer_bcid.booleanValue())) {
            throw new IllegalInputException("Please select one of the optional parameters for the Issuer for inclusion");
        }
        ValidityPeriod vp = (ValidityPeriod) holderDetails.get(issrg.dis.HolderDetails.VALIDITY_PERIOD);
        GregorianCalendar nb = new GregorianCalendar();
        nb.setTime(vp.getNotBefore());
        GregorianCalendar na = new GregorianCalendar();
        na.setTime(vp.getNotAfter());
        issrg.ac.Generalized_Time notBf = new issrg.ac.Generalized_Time(nb);
        issrg.ac.Generalized_Time notAf = new issrg.ac.Generalized_Time(na);
        issrg.ac.AttCertValidityPeriod validity_period = new issrg.ac.AttCertValidityPeriod(notBf, notAf);
        Map roleTypesAndValues = (Hashtable) holderDetails.get(HolderDetails.ROLE_TYPES_VALUES);
        Enumeration e = ((Hashtable) roleTypesAndValues).keys();
        Vector attributes = new Vector();
        while (e.hasMoreElements()) {
            String roleType = e.nextElement().toString();
            Vector roles = (Vector) roleTypesAndValues.get(roleType);
            Vector permisRoles = new Vector();
            int numberOfRole = roles.size();
            for (int i = 0; i < numberOfRole; i++) {
                permisRoles.add(new issrg.ac.attributes.PermisRole((String) roles.get(i)));
            }
            String roleTypeID = (String) roleTypes.get(roleType);
            issrg.ac.Attribute attr = new issrg.ac.Attribute(roleTypeID, permisRoles);
            attributes.add(attr);
        }
        if (attributes.size() == 0) {
            holderDetails.put(HolderDetails.MESSAGE, "Attribute size equal to zero");
            throw new issrg.acm.IllegalInputException("Attribute set cannot be empty");
        }
        Vector extensionCollection = new Vector();
        if (((Boolean) holderDetails.get(HolderDetails.ASSERTION)).booleanValue() == false) extensionCollection.add(new issrg.ac.attributes.NoAssertion());
        int depth = (((Integer) holderDetails.get(HolderDetails.DELEGATION_DEPTH))).intValue();
        if (depth > -1) {
            extensionCollection.add(new issrg.ac.attributes.BasicAttConstraint(false, depth - 1));
        }
        try {
            String DN = issrg.utils.RFC2253NameParser.toCanonicalDN(issrg.utils.RFC2253NameParser.distinguishedName((String) holderDetails.get(HolderDetails.ISSUER_DN)));
            GeneralName issuerGeneralName = new GeneralName(GeneralName.directoryName, new iaik.utils.RFC2253NameParser(DN).parse());
            extensionCollection.add(new issrg.ac.attributes.IssuedOnBehalfOf(false, issuerGeneralName));
            java.security.cert.X509Certificate signerPKC = signingUtility.getVerificationCertificate();
            String subjectDN;
            String issuerDN;
            if (signerPKC instanceof iaik.x509.X509Certificate) {
                try {
                    subjectDN = ((iaik.asn1.structures.Name) signerPKC.getSubjectDN()).getRFC2253String();
                    issuerDN = ((iaik.asn1.structures.Name) signerPKC.getIssuerDN()).getRFC2253String();
                } catch (iaik.utils.RFC2253NameParserException rnpe) {
                    holderDetails.put(HolderDetails.MESSAGE, "Failed to decode DNs of issuer or holder");
                    throw new ACCreationException("Failed to decode DNs", rnpe);
                }
            } else {
                subjectDN = signerPKC.getSubjectDN().getName();
                issuerDN = signerPKC.getIssuerDN().getName();
            }
            GeneralName dISGeneralName = new GeneralName(GeneralName.directoryName, new iaik.utils.RFC2253NameParser(subjectDN).parse());
            GeneralNames generalNames = new GeneralNames(dISGeneralName);
            SEQUENCE AAISyntax = new SEQUENCE();
            Vector serials = (Vector) holderDetails.get(HolderDetails.DIS_SERIAL_NUMBER);
            int numberOfSerials = serials.size();
            if (numberOfSerials > 0) {
                for (int ii = 0; ii < numberOfSerials; ii++) {
                    SEQUENCE issuerSerial = new SEQUENCE();
                    issuerSerial.addComponent(generalNames.toASN1Object());
                    INTEGER certificateSerialNumber = new INTEGER((java.math.BigInteger) serials.get(ii));
                    issuerSerial.addComponent(certificateSerialNumber);
                    AAISyntax.addComponent(issuerSerial);
                }
                extensionCollection.add(new issrg.ac.attributes.AuthorityAttributeIdentifier(AAISyntax));
            }
            extensionCollection.add(aaia);
            issrg.ac.Extensions extensions = new issrg.ac.Extensions(extensionCollection);
            iaik.asn1.structures.GeneralNames hn = issrg.ac.Util.buildGeneralNames((String) holderDetails.get(HolderDetails.HOLDER_DN));
            issrg.ac.Holder holder = new issrg.ac.Holder(null, hn, null);
            issrg.ac.AttCertIssuer issuer;
            issrg.ac.V2Form signer = new issrg.ac.V2Form(issrg.ac.Util.buildGeneralNames(subjectDN), new issrg.ac.IssuerSerial(issrg.ac.Util.buildGeneralNames(issuerDN), signerPKC.getSerialNumber(), null), null);
            if (!issuer_name.booleanValue()) signer.setIssuerName(null);
            if (!issuer_bcid.booleanValue()) signer.setBaseCertificateID(null);
            signer.setObjectDigestInfo(null);
            issuer = new issrg.ac.AttCertIssuer(null, signer);
            byte[] bt = signerPKC.getSigAlgParams();
            ASN1Object algParams = bt == null ? null : iaik.asn1.DerCoder.decode(bt);
            AlgorithmID signatureAlg = new AlgorithmID(new iaik.asn1.ObjectID(signerPKC.getSigAlgOID()), algParams);
            issrg.ac.AttributeCertificateInfo aci = new issrg.ac.AttributeCertificateInfo(new issrg.ac.AttCertVersion(issrg.ac.AttCertVersion.V2), holder, issuer, signatureAlg, ACSerialNumber, validity_period, attributes, null, extensions);
            try {
                byte[] b = aci.getEncoded();
                byte[] ac = new issrg.ac.AttributeCertificate(aci, signatureAlg, new BIT_STRING(signingUtility.sign(b))).getEncoded();
                holderDetails.put(HolderDetails.ATTRIBUTE_CERTIFICATE, ac);
                return true;
            } catch (Throwable tha) {
                holderDetails.put(HolderDetails.MESSAGE, "Can not create AC - Error with sign AC or encode AC");
                if (verbose) throw new ACCreationException(tha.getMessage(), tha);
            }
        } catch (iaik.asn1.CodingException ce) {
            holderDetails.put(HolderDetails.MESSAGE, "Error when creating AC");
            throw new ACCreationException(ce.getMessage(), ce);
        } catch (issrg.security.SecurityException se) {
            holderDetails.put(HolderDetails.MESSAGE, "Error when creating AC");
            throw new ACCreationException(se.getMessage(), se);
        } catch (ACCreationException acce) {
            holderDetails.put(HolderDetails.MESSAGE, "Error when creating AC");
            if (verbose) acce.printStackTrace();
        } catch (RFC2253ParsingException rfcpe) {
            holderDetails.put(HolderDetails.MESSAGE, "Error when creating AC");
            throw new ACCreationException(rfcpe.getMessage(), rfcpe);
        } catch (iaik.utils.RFC2253NameParserException pe) {
            holderDetails.put(HolderDetails.MESSAGE, "Error when creating AC");
            throw new ACCreationException(pe.getMessage(), pe);
        }
        return false;
    }

    /** 
     * This method gets the AC from the Map (holderDetails) and stores it in LDAP 
     **/
    private void storeToLDAP(Map holderDetails) {
        byte[] ac = (byte[]) holderDetails.get(HolderDetails.ATTRIBUTE_CERTIFICATE);
        try {
            ldapUtility.save(ac);
        } catch (ACCreationException acce) {
            holderDetails.put(HolderDetails.MESSAGE, Comm.ERROR_WRITING_LDAP);
        }
    }

    /**
     * This method checks the privileges of the issuer and constrains the validity period and delegation depth of the issued AC.
     *
     * @param holderDetails is the details of AC to-be-sign request
     *
     *  @return true if the issuer has enough privileges to issue that AC to the holder (validity time and delegation depth will be constrained in this method);
     *  false if he has not.
     **/
    private boolean checkIssuer(Map holderDetails) {
        String issuerDN = (String) holderDetails.get(issrg.dis.HolderDetails.ISSUER_DN);
        String holderDN = (String) holderDetails.get(issrg.dis.HolderDetails.HOLDER_DN);
        Map roleTypesValues = (Hashtable) holderDetails.get(issrg.dis.HolderDetails.ROLE_TYPES_VALUES);
        ValidityPeriod vp = (ValidityPeriod) holderDetails.get(issrg.dis.HolderDetails.VALIDITY_PERIOD);
        String deep = (String) holderDetails.get(issrg.dis.HolderDetails.DELEGATION_DEPTH);
        int deepInt = Integer.parseInt(deep);
        holderDetails.put(issrg.dis.HolderDetails.DELEGATION_DEPTH, new Integer(NO_DELEGATION));
        if (!vp.getNotBefore().before(vp.getNotAfter())) {
            holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ERROR_WITH_VALIDITY_TIME);
            return false;
        }
        if (!(issrg.utils.RFC2253NameParser.toCanonicalDN(issuerDN).toUpperCase().equals(SOA)) && !(issrg.utils.RFC2253NameParser.toCanonicalDN(issuerDN).toUpperCase().equals(DIS))) {
            Subtree subjectPol = ((issrg.pba.rbac.xmlpolicy.XMLPolicyParser) pp).getSubjectDomains();
            try {
                if (!subjectPol.contains(new UserEntry(new LDAPDNPrincipal(issuerDN)))) {
                    holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ISSUER_OUT_OF_DOMAIN);
                    return false;
                }
            } catch (issrg.utils.RFC2253ParsingException pe) {
                holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ISSUER_OUT_OF_DOMAIN);
                return false;
            }
        }
        if (roleTypesValues.isEmpty()) {
            holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.NO_PERMIS_ROLE_IN_REQUESTED_AC);
            return false;
        }
        if (issrg.utils.RFC2253NameParser.toCanonicalDN(holderDN).toUpperCase().equals(DIS) || issrg.utils.RFC2253NameParser.toCanonicalDN(holderDN).toUpperCase().equals(SOA)) {
            holderDetails.put(HolderDetails.MESSAGE, Comm.HOLDER_CAN_NOT_BE_DIS_OR_SOA);
            return false;
        }
        Vector creds = new Vector();
        SetOfSubsetsCredentials holderCreds;
        RoleHierarchyPolicy roleHierarchyPolicy = (issrg.pba.rbac.xmlpolicy.XMLPolicyParser.RoleHierarchyPolicyNode) ((issrg.pba.rbac.xmlpolicy.XMLPolicyParser) pp).getAuthTokenParsingRules().get(issrg.pba.rbac.RoleHierarchyPolicy.class);
        Enumeration types = ((Hashtable) roleTypesValues).keys();
        while (types.hasMoreElements()) {
            String type = types.nextElement().toString();
            String oid = (String) roleTypes.get(type);
            if (oid == null) {
                holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ROLETYPE_IS_NOT_SUPPORTED_IN_POLICY);
                return false;
            }
            Vector roles = (Vector) roleTypesValues.get(type);
            int numberOfRoles = roles.size();
            for (int j = 0; j < numberOfRoles; j++) {
                RoleHierarchyNode node = roleHierarchyPolicy.getRole(type, (String) roles.elementAt(j));
                if (node == null) {
                    holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ERROR_ROLEVALUE_DO_NOT_EXIST);
                    return false;
                } else {
                    creds.add(new issrg.pba.rbac.ExpirableCredentials(new PermisCredentials(node), vp));
                }
            }
        }
        holderCreds = new SetOfSubsetsCredentials(creds);
        if (issrg.utils.RFC2253NameParser.toCanonicalDN(issuerDN).toUpperCase().equals(DIS)) {
            holderDetails.put(issrg.dis.HolderDetails.HOLDER_CREDS, holderCreds);
            holderDetails.put(issrg.dis.HolderDetails.DELEGATION_DEPTH, new Integer(deepInt));
            return true;
        }
        try {
            Vector rules;
            if (issrg.utils.RFC2253NameParser.toCanonicalDN(issuerDN).toUpperCase().equals(SOA)) {
                rules = (Vector) pp.getAssignmentRules().get(SOA);
            } else {
                String[] locations = (String[]) holderDetails.get(HolderDetails.ISSUER_AC_LOCATIONS);
                Object[] a = null;
                if (locations != null) {
                    javax.naming.directory.DirContext[] ldaps = new javax.naming.directory.DirContext[locations.length];
                    for (int i = 0; i < locations.length; i++) {
                        Hashtable env = new Hashtable();
                        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                        env.put(Context.PROVIDER_URL, locations[i]);
                        env.put("java.naming.ldap.version", "3");
                        env.put("java.naming.ldap.attributes.binary", issrg.pba.repository.ACRepository.ATTRIBUTE_CERTIFICATE_ID);
                        env.put(Context.SECURITY_AUTHENTICATION, "none");
                        ldaps[i] = new InitialDirContext(env);
                    }
                    issrg.utils.repository.LDAPRepository repository = new issrg.utils.repository.LDAPRepository(ldaps);
                    issrg.security.DefaultSecurity dstemp = new issrg.security.DefaultSecurity();
                    dstemp.setRootCA(x509);
                    dstemp.setPKCRepository(new issrg.security.PKCRepository(repository));
                    issrg.pba.rbac.SimpleSignatureVerifier simpleVerifier = new issrg.pba.rbac.SimpleSignatureVerifier(dstemp);
                    AuthTokenRepository optionalACR = new issrg.pba.rbac.ACRepositoryWithPKI(repository, sv);
                    javax.naming.directory.Attribute attrs = ((issrg.pba.rbac.ACRepositoryWithPKI) optionalACR).getACs(new LDAPDNPrincipal(issuerDN));
                    a = new Object[attrs.size()];
                    for (int i = 0; i < attrs.size(); i++) a[i] = attrs.get(i);
                }
                PermisSubject issuerSubject = (PermisSubject) pbaApi.getCreds(new LDAPDNPrincipal(issuerDN), a);
                rules = (Vector) issuerSubject.exportRules();
            }
            if (rules.isEmpty()) {
                holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ISSUER_DONOT_HAVE_ENOUGH_PRIVILEGES);
                return false;
            }
            Vector credsAfterConstrains = new Vector();
            Vector rulesForHolder = new Vector();
            boolean found = false;
            for (int i = 0; i < rules.size(); i++) {
                AssignmentRule rule = (AssignmentRule) rules.get(i);
                if (rule.getSubjectDomain().contains(new UserEntry(new LDAPDNPrincipal(holderDN)))) {
                    found = true;
                    SetOfSubsetsCredentials t = (SetOfSubsetsCredentials) rule.getCredentials().intersection(holderCreds);
                    ValidityPeriod vl = ((ExpirableCredentials) t.getValue().get(0)).getValidityPeriod();
                    if (vl.getNotBefore().compareTo(vl.getNotAfter()) <= 0) {
                        rulesForHolder.add(rule);
                        credsAfterConstrains.add(t);
                    }
                }
            }
            if (rulesForHolder.isEmpty() && !found) {
                holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.HOLDER_OUT_OF_SUBJECTDOMAIN);
                return false;
            }
            if (rulesForHolder.isEmpty() && found) {
                holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ERROR_WITH_VALIDITY_TIME);
                return false;
            }
            int index = 0;
            SetOfSubsetsCredentials mostPriorityOne = (SetOfSubsetsCredentials) credsAfterConstrains.get(0);
            int sizeOfRules = credsAfterConstrains.size();
            if (sizeOfRules > 1) {
                for (int i = 1; i < sizeOfRules; i++) {
                    SetOfSubsetsCredentials temp = (SetOfSubsetsCredentials) credsAfterConstrains.get(i);
                    if (!morePriority(mostPriorityOne, temp)) {
                        index = i;
                        mostPriorityOne = temp;
                    }
                }
            }
            if ((!mostPriorityOne.equals(holderCreds)) && (!downgradeable)) {
                holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.CANNOT_DOWNGRADE_PRIVILEGES);
                return false;
            }
            holderDetails.put(issrg.dis.HolderDetails.HOLDER_CREDS, mostPriorityOne);
            holderDetails.put(issrg.dis.HolderDetails.VALIDITY_PERIOD, ((ExpirableCredentials) mostPriorityOne.getValue().get(0)).getValidityPeriod());
            if (deepInt >= 0) {
                int depthForRule = ((AssignmentRule) rulesForHolder.get(index)).getDelegationDepth();
                if (depthForRule >= 0 && (depthForRule < deepInt || deepInt == 0)) {
                    if (!downgradeable) {
                        holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.CANNOT_DOWNGRADE_PRIVILEGES);
                        return false;
                    }
                    deepInt = depthForRule;
                    if (deepInt == 0) deepInt = -1;
                }
                holderDetails.put(issrg.dis.HolderDetails.DELEGATION_DEPTH, new Integer(deepInt));
            }
            return true;
        } catch (Exception pe) {
            holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ERROR_WHEN_CREATE_USER_ENTRY);
            return false;
        }
    }

    /**
     * This function compare two SetOfSubsetCredentials object to select one object according to
     * higher priority rule. Higher priority means the RAR is closer to the signing request.
     *
     * @param current is the current object with higher priority
     * @param cred is the compared object
     *
     * @return true if the current object has higher or equal priority, otherwise return false
     */
    private boolean morePriority(SetOfSubsetsCredentials current, SetOfSubsetsCredentials cred) {
        Vector t = (Vector) current.getValue().clone();
        Vector currentMax = new Vector();
        for (int i = 0; i < t.size(); i++) currentMax.add(((ExpirableCredentials) t.get(i)).getExpirable());
        Vector currentMaxBackup = (Vector) currentMax.clone();
        t = (Vector) cred.getValue().clone();
        Vector compare = new Vector();
        for (int i = 0; i < t.size(); i++) compare.add(((ExpirableCredentials) t.get(i)).getExpirable());
        Vector compareBackup = (Vector) compare.clone();
        for (int i = 0; i < currentMax.size(); i++) {
            PermisCredentials a = (PermisCredentials) currentMax.get(i);
            for (java.util.Iterator ite = compare.iterator(); ite.hasNext(); ) {
                if (a.contains((PermisCredentials) ite.next())) {
                    ite.remove();
                }
            }
        }
        if (compare.isEmpty()) return true;
        for (int i = 0; i < compareBackup.size(); i++) {
            PermisCredentials a = (PermisCredentials) compareBackup.get(i);
            for (java.util.Iterator ite = currentMaxBackup.iterator(); ite.hasNext(); ) {
                if (a.contains((PermisCredentials) ite.next())) {
                    ite.remove();
                }
            }
        }
        if (currentMaxBackup.isEmpty()) return false;
        for (int i = 0; i < compare.size(); i++) {
            PermisCredentials a = (PermisCredentials) compare.get(i);
            for (java.util.Iterator ite = currentMax.iterator(); ite.hasNext(); ) if (a.contains((PermisCredentials) ite.next())) ite.remove();
        }
        if (currentMax.isEmpty()) return false;
        String currentRemains = "";
        String compareRemains = "";
        for (int i = 0; i < currentMax.size(); i++) currentRemains = currentRemains.concat(((PermisCredentials) currentMax.get(i)).getRoleValueAsString());
        for (int i = 0; i < compare.size(); i++) compareRemains = compareRemains.concat(((PermisCredentials) compare.get(i)).getRoleValueAsString());
        return (currentRemains.compareToIgnoreCase(compareRemains) <= 0) ? true : false;
    }

    private boolean checkAgainstRAP(Map holderDetails) {
        holderDetails.put(HolderDetails.DIS_SERIAL_NUMBER, (Vector) serialAndRules.get(DISSERIAL));
        Vector rules = (Vector) serialAndRules.get(DISRULES);
        int numberOfRules = rules.size();
        if (numberOfRules == 0) {
            holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.DIS_DO_NOT_HAVE_ENOUGH_PRIVILEGE);
            return false;
        }
        String holderDN = (String) holderDetails.get(issrg.dis.HolderDetails.HOLDER_DN);
        SetOfSubsetsCredentials assumedCreds = (SetOfSubsetsCredentials) holderDetails.get(issrg.dis.HolderDetails.HOLDER_CREDS);
        try {
            Vector canBeUsedRules = new Vector();
            Vector credsAfterConstrains = new Vector();
            boolean found = false;
            for (int i = 0; i < numberOfRules; i++) {
                AssignmentRule rule = (AssignmentRule) rules.get(i);
                if (rule.getSubjectDomain().contains(new UserEntry(new LDAPDNPrincipal(holderDN)))) {
                    found = true;
                    SetOfSubsetsCredentials t = (SetOfSubsetsCredentials) rule.getCredentials().intersection(assumedCreds);
                    ValidityPeriod vl = ((ExpirableCredentials) t.getValue().get(0)).getValidityPeriod();
                    if (vl.getNotBefore().compareTo(vl.getNotAfter()) <= 0) {
                        canBeUsedRules.add(rule);
                        credsAfterConstrains.add(t);
                    }
                }
            }
            if (canBeUsedRules.isEmpty() && !found) {
                holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.HOLDER_OUT_OF_SUBJECTDOMAIN);
                return false;
            }
            if (canBeUsedRules.isEmpty() && found) {
                holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ERROR_WITH_VALIDITY_TIME);
                return false;
            }
            int index = 0;
            SetOfSubsetsCredentials mostPriorityOne = (SetOfSubsetsCredentials) credsAfterConstrains.get(0);
            int sizeOfRules = credsAfterConstrains.size();
            if (sizeOfRules > 1) {
                for (int i = 1; i < sizeOfRules; i++) {
                    SetOfSubsetsCredentials temp = (SetOfSubsetsCredentials) credsAfterConstrains.get(i);
                    if (!morePriority(mostPriorityOne, temp)) {
                        index = i;
                        mostPriorityOne = temp;
                    }
                }
            }
            if (assumedCreds.equals(mostPriorityOne)) {
                prepareRoleTypesAndRoleValues(holderDetails);
                int depthForRule = ((AssignmentRule) canBeUsedRules.get(index)).getDelegationDepth();
                int requestedDepth = ((Integer) holderDetails.get(HolderDetails.DELEGATION_DEPTH)).intValue();
                if ((requestedDepth < 0) || (depthForRule == -1)) return true;
                if ((depthForRule >= requestedDepth) && (requestedDepth > 0)) return true;
                if (!downgradeable) {
                    holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.CANNOT_DOWNGRADE_PRIVILEGES);
                    return false;
                } else {
                    if (depthForRule == 0) {
                        holderDetails.put(HolderDetails.DELEGATION_DEPTH, new Integer(-1));
                    } else holderDetails.put(HolderDetails.DELEGATION_DEPTH, new Integer(depthForRule));
                    return true;
                }
            } else {
                if (!downgradeable) {
                    holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.CANNOT_DOWNGRADE_PRIVILEGES);
                    return false;
                } else {
                    holderDetails.put(HolderDetails.HOLDER_CREDS, mostPriorityOne);
                    holderDetails.put(HolderDetails.VALIDITY_PERIOD, ((ExpirableCredentials) mostPriorityOne.getValue().get(0)).getValidityPeriod());
                    prepareRoleTypesAndRoleValues(holderDetails);
                    int depthForRule = ((AssignmentRule) canBeUsedRules.get(index)).getDelegationDepth();
                    int requestedDepth = ((Integer) holderDetails.get(HolderDetails.DELEGATION_DEPTH)).intValue();
                    if ((requestedDepth < 0) || (depthForRule == -1)) return true;
                    if ((depthForRule >= requestedDepth) && (requestedDepth > 0)) return true;
                    if (depthForRule == 0) {
                        holderDetails.put(HolderDetails.DELEGATION_DEPTH, new Integer(-1));
                    } else holderDetails.put(HolderDetails.DELEGATION_DEPTH, new Integer(depthForRule));
                    return true;
                }
            }
        } catch (RFC2253ParsingException e) {
            holderDetails.put(issrg.dis.HolderDetails.MESSAGE, Comm.ERROR_WHEN_CREATE_USER_ENTRY);
            return false;
        }
    }

    /**
     * Process the Map holderDetails, to create a map (roleTypesAndValues) from holderCreds. This map will be used in generateAC function
     **/
    private void prepareRoleTypesAndRoleValues(Map holderDetails) {
        SetOfSubsetsCredentials set = (SetOfSubsetsCredentials) holderDetails.get(HolderDetails.HOLDER_CREDS);
        Vector expirables = set.getValue();
        Map roleTypesAndValues = new Hashtable();
        Vector roleValues;
        for (int i = 0; i < expirables.size(); i++) {
            ExpirableCredentials cred = (ExpirableCredentials) expirables.get(i);
            PermisCredentials permisCred = (PermisCredentials) cred.getExpirable();
            String roleType = permisCred.getRoleType();
            roleValues = (Vector) roleTypesAndValues.get(roleType);
            if (roleValues == null) {
                roleValues = new Vector();
                roleTypesAndValues.put(roleType, roleValues);
            }
            String roleValue = permisCred.getRoleValueAsString();
            if (!roleValues.contains(roleValue)) roleValues.add(roleValue);
        }
        holderDetails.put(HolderDetails.ROLE_TYPES_VALUES, roleTypesAndValues);
    }

    public static void main(String[] args) {
        DIS testDIS = new DIS();
        String[] roles = new String[] { "Researcher", "Professor" };
        testDIS.signForMe("cn=aa2,ou=staff, o=permis,c=gb", "cn=admin1,ou=admin,o=permis,c=gb", roles, "permisRole", "2000.06.01 12:00:00", "2009.08.27 12:00:00", "can", "2");
    }
}
