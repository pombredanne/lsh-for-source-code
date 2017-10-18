package issrg.dis;

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
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Calendar;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import iaik.asn1.*;
import iaik.asn1.structures.*;
import issrg.pba.rbac.SetOfSubsetsCredentials;
import issrg.pba.rbac.ExpirableCredentials;
import issrg.pba.rbac.AbsoluteValidityPeriod;
import issrg.ac.*;
import issrg.ac.attributes.*;
import issrg.pba.ParsedToken;
import issrg.pba.DelegatableToken;
import issrg.pba.*;
import issrg.pba.rbac.*;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import java.io.PrintStream;

/**
 * This class contains the core functionality of DIS. Extend it to tailor
 * DIS to the execution environment.
 */
public abstract class DISCore {

    public static interface Repository {

        public void save(byte[] ac) throws Exception;

        public javax.naming.directory.Attribute loadACs(String dn) throws Exception;

        public void deleteAC(String dn, int idx) throws Exception;

        public void deleteAllACs(String dn) throws Exception;
    }

    public static class DISConfig {

        protected static final String config_file = "dis.cfg";

        protected static final String serial_file = "serialnumber.cfg";

        protected String LDAP_AC_Attribute;

        protected String RootPKC;

        protected String PolicyIssuer;

        protected String LDAP_PKC_Attribute;

        protected String PolicyLocation;

        protected String PolicyLocationUsername;

        protected String PolicyLocationPW;

        protected String DIS;

        protected String PolicyIdentifier;

        protected String CredentialLocation = null;

        protected String CredentialLocationUsername;

        protected String CredentialLocationPW;

        protected boolean downgradeable;

        protected String SigningKeyFile;

        protected String SigningKeyPW;

        protected String log_file = "dis.out";

        protected String SearchRequestor;

        protected Level level;

        private LDAPUtility ldapUtil;

        protected String priority;

        protected Vector trustedProxy = new Vector();

        public DISConfig() throws Exception {
            System.out.println("Going to read the DIS's config file");
            readConfigFile();
            System.out.println("The DIS's config file is read");
        }

        public DISConfig(String path) throws Exception {
            String separator = System.getProperties().getProperty("file.separator");
            String configPath = path.concat(separator);
            configPath = configPath.concat(config_file);
            readConfigFile(new FileInputStream(configPath));
        }

        /**
       * This method reads configuration parameters from configuration file for DIS.    
       */
        protected void readConfigFile() throws Exception {
            java.net.URL configURL = this.getClass().getResource(config_file);
            if (configURL == null) {
                System.out.println("The DIS's config file is not found " + config_file);
                throw new Exception("Config file for the DIS is not found");
            }
            readConfigFile(new FileInputStream(configURL.getFile()));
        }

        protected void readConfigFile(InputStream inputStream) throws Exception {
            try {
                System.out.println("The DIS's config file is found, going to read the config file");
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                String left, right;
                int i;
                while ((line = in.readLine()) != null) {
                    line = line.intern();
                    if (line == "") continue;
                    line = line.trim();
                    i = line.indexOf(" ");
                    if (i < 0) {
                        System.out.println("Unrecognised line ignored: " + line);
                        continue;
                    }
                    left = line.substring(0, i).toLowerCase().trim().intern();
                    right = line.substring(i + 1).trim().intern();
                    if (!process(left, right)) {
                        System.out.println("Unrecognised line ignored: " + line);
                    }
                }
            } catch (FileNotFoundException fnfe) {
                System.out.println("Error: " + fnfe.getMessage() + "\n" + fnfe.fillInStackTrace());
                throw new FileNotFoundException("Config file not found" + config_file);
            } catch (IOException ioe) {
                System.out.println("Error: " + ioe.getMessage() + "\n" + ioe.fillInStackTrace());
                throw new IOException("Error reading configuration file");
            }
        }

        /**
       * This method is used to process a pair of variable name and value read
       * from config file. DISConfig subclasses can override the method to add
       * more variables, and should return true for those variable names that 
       * they understand.
       *
       * @param left the variable name
       * @param right the variable value
       *
       * @return true, if the variable name and value is accepted
       */
        protected boolean process(String left, String right) throws Exception {
            boolean b = true;
            if (left.charAt(0) == '#') {
            } else if (left == "policyissuer") {
                PolicyIssuer = right.toUpperCase();
            } else if (left == "policyidentifier") {
                PolicyIdentifier = right;
            } else if (left == "rootpkc") {
                RootPKC = right;
            } else if (left == "credentiallocation") {
                CredentialLocation = right;
            } else if (left == "downgradeable") {
                downgradeable = right == "1";
            } else if (left == "credentiallocationusername") {
                CredentialLocationUsername = right;
            } else if (left == "credentiallocationpw") {
                CredentialLocationPW = right;
            } else if (left == "signingkeyfile") {
                SigningKeyFile = right;
            } else if (left == "signingkeypw") {
                SigningKeyPW = right;
            } else if (left == "ldap_ac_attribute") {
                LDAP_AC_Attribute = right;
            } else if (left == "debuglevel") {
                priority = right;
                level = Level.toLevel(priority.toUpperCase());
            } else if (left == "trustedproxy") {
                trustedProxy.add(issrg.utils.RFC2253NameParser.toCanonicalDN(right));
            } else if (left == "policylocation") {
                PolicyLocation = right;
            } else if (left == "policylocationusername") {
                PolicyLocationUsername = right;
            } else if (left == "policylocationpw") {
                PolicyLocationPW = right;
            } else if (left == "ldap_pkc_attribute") {
                LDAP_PKC_Attribute = right;
            } else if (left == "logfile") {
                log_file = right;
            } else if (left == "searchrequestor") {
                SearchRequestor = right;
            } else {
                b = false;
            }
            return b;
        }

        public Repository getLDAPUtility() {
            if (ldapUtil == null) {
                ldapUtil = new LDAPUtility(CredentialLocation, CredentialLocationUsername, CredentialLocationPW, level);
            }
            return ldapUtil;
        }

        public issrg.utils.repository.AttributeRepository getCredRepository() {
            return new issrg.utils.repository.LDAPRepository(((LDAPUtility) getLDAPUtility()).getLdaps());
        }

        public OutputStream getLogStream() throws Exception {
            java.net.URL configURL = this.getClass().getResource(config_file);
            File c = new File(configURL.getFile());
            String filePath = c.getCanonicalFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getAbsolutePath();
            String separator = System.getProperty("file.separator");
            return new FileOutputStream(filePath.concat(separator).concat("logs").concat(separator).concat(log_file));
        }

        /**
       * This method returns the filename where the serial number is stored.
       * For Tomcat, this is computed relative to the resource location; this 
       * must work, because the classes are expanded onto the filesystem, even
       * though I don't like this approach, but that's how Tuan Anh did it.
       */
        public File getSerialFile() {
            return new File(this.getClass().getResource("").getFile(), serial_file);
        }
    }

    protected static Logger root = Logger.getRootLogger();

    protected static Logger log = Logger.getLogger(issrg.dis.DIS.class);

    protected static org.apache.log4j.Appender appender;

    protected static org.apache.log4j.Layout layout = new org.apache.log4j.PatternLayout("%d{dd MM yyyy HH:mm:ss} %-5p %c %x - %m%n");

    private static final issrg.utils.Version version = new issrg.utils.Version("issrg/dis/version", "dis");

    private static final int CREDS = 0;

    private static final int RULES = 1;

    protected DISConfig dcfg;

    private issrg.utils.repository.AttributeRepository r;

    private issrg.utils.repository.AttributeRepository policyRepository;

    private issrg.pba.repository.AuthzTokenRepository globalAuthTokenRep;

    private iaik.x509.X509Certificate x509;

    private issrg.security.DefaultSecurity ds;

    protected issrg.pba.rbac.SignatureVerifier sv;

    private Repository ldapUtility;

    private issrg.security.DefaultSecurity signingUtility = new issrg.security.PKCS12Security();

    private Map roleTypes;

    private SetOfSubsetsCredentials emptyCreds = new SetOfSubsetsCredentials();

    private static final String SEPARATOR = "|";

    private static final String DOUBLE_SEPARATOR = "||";

    private static final String SEPARATOR_APACHE = ",";

    private static final String SPACE = " ";

    private issrg.pba.PolicyParser pp;

    private RoleHierarchyPolicy roleHierarchyPolicy;

    protected issrg.pba.AuthzTokenParser tokenParser;

    private issrg.pba.rbac.policies.AllocationPolicy allocationPolicy;

    private issrg.pba.rbac.RuleComparator comparator;

    protected issrg.pba.rbac.PolicyFinder policy;

    private java.util.Vector soas = new Vector();

    private issrg.ac.extensions.AttributeAuthorityInformationAccess aaia = null;

    private issrg.ac.attributes.AuthorityAttributeIdentifier aai = null;

    private String issuerDISCertificateDN;

    private java.security.cert.X509Certificate signerPKC;

    private GeneralNames DISGeneralNames;

    private IssuerSerial issuerDISCertificateSerial;

    private AlgorithmID signatureAlg;

    private static final String SATISFIED = "satisfied";

    public DISCore(DISConfig dcfg) throws Exception {
        try {
            this.dcfg = dcfg;
            System.setOut(new PrintStream(dcfg.getLogStream()));
            appender = new org.apache.log4j.WriterAppender(layout, System.out);
            root.removeAllAppenders();
            BasicConfigurator.configure(appender);
            System.setErr(System.out);
            log.setLevel(dcfg.level);
            log.info("Finish reading the config file");
            log.info("Set the debug level " + dcfg.priority);
            log.info("The DIS version: " + version.getVersion());
            dcfg.PolicyIssuer = issrg.utils.RFC2253NameParser.toCanonicalDN(dcfg.PolicyIssuer);
            log.debug("Going to login to the signing key");
            signingUtility.login(dcfg.SigningKeyFile, dcfg.SigningKeyPW.toCharArray());
            log.info("Login to the signing key successful");
            signerPKC = signingUtility.getVerificationCertificate();
            dcfg.DIS = issrg.utils.RFC2253NameParser.toCanonicalDN(signerPKC.getSubjectDN().getName());
            if (dcfg.DIS == null) {
                log.error("Error with DIS's signing key: DN of the key's holder is in correct format");
                throw new Exception("Error with DIS's signing key: DN of the key's holder is in correct format");
            }
            dcfg.DIS = dcfg.DIS.toUpperCase();
            log.debug("Trying to get the issuer of the DIS's certificate");
            try {
                issuerDISCertificateDN = ((iaik.asn1.structures.Name) signerPKC.getIssuerDN()).getRFC2253String();
            } catch (iaik.utils.RFC2253NameParserException rnpe) {
                log.error("Failed to get issuer of the DIS's certificate");
                throw new Exception("Failed to issuer of the DIS's certificate", rnpe);
            }
            log.info("Got the DN of the issuer of the DIS's certificate");
            LDAPUtility.AC_attribute = dcfg.LDAP_AC_Attribute;
            log.debug("Going to get rootca's PKC");
            try {
                x509 = new iaik.x509.X509Certificate(new java.io.FileInputStream(dcfg.RootPKC));
            } catch (IOException ioe) {
                log.error("Can not get the rootca's PKC");
                throw ioe;
            }
            log.info("Got the rootca's PKC");
            ds = new issrg.security.PKCS12Security();
            ds.setRootCA(x509);
            CustomisePERMIS.setAttributeCertificateAttribute(dcfg.LDAP_AC_Attribute);
            CustomisePERMIS.setUserCertificateAttribute(dcfg.LDAP_PKC_Attribute);
            CustomisePERMIS.configureX509Flavour();
            log.debug("Going to get the default repository that contains ACs");
            ldapUtility = dcfg.getLDAPUtility();
            log.debug("Got the default repository that contains the ACs");
            r = dcfg.getCredRepository();
            log.debug("Going to set the security on the repository");
            ds.setPKCRepository(new issrg.security.PKCRepository(r));
            log.debug("Setting security to the repository is done");
            sv = new issrg.pba.rbac.SimpleSignatureVerifier(ds);
            log.debug("Going to get a repository that contains the policy");
            policyRepository = null;
            if (dcfg.PolicyLocation.trim().substring(0, 4).intern() == "ldap".intern()) {
                log.info("The policy is stored in a LDAP entry");
                if (dcfg.PolicyIssuer == null) {
                    log.error("Error with PolicyIssuer's DN");
                    System.exit(-1);
                }
                LDAPUtility ldapUtilityPolicyRepository = new LDAPUtility(dcfg.PolicyLocation, dcfg.PolicyLocationUsername, dcfg.PolicyLocationPW, dcfg.level);
                policyRepository = new issrg.utils.repository.LDAPRepository(ldapUtilityPolicyRepository.getLdaps());
                log.debug("Repository that stores the policy is initialized");
                log.debug("Calling DISRAPandParser to get the policy and others important objects");
                policy = new DISRAPandParser(policyRepository, dcfg.PolicyIdentifier, new LDAPDNPrincipal(dcfg.PolicyIssuer), sv, dcfg.level);
                log.info("Policy readin sucsessfully");
            } else if (dcfg.PolicyLocation.trim().substring(0, 4).toLowerCase().intern() == "file".intern()) {
                log.info("The policy is stored in a file");
                String policyPath = dcfg.PolicyLocation.trim().substring(6, dcfg.PolicyLocation.length());
                if (policyPath.endsWith("ace")) {
                    if (dcfg.PolicyIssuer == null) {
                        log.error("Error with PolicyIssuer's DN");
                        System.exit(-1);
                    }
                    java.io.InputStream io = new java.io.FileInputStream(policyPath);
                    byte[] ac = new byte[io.available()];
                    io.read(ac);
                    policyRepository = new issrg.utils.repository.VirtualRepository();
                    ((issrg.utils.repository.VirtualRepository) policyRepository).populate(dcfg.PolicyIssuer, CustomisePERMIS.getAttributeCertificateAttribute(), ac);
                    javax.naming.directory.Attribute cert = r.getAttribute(new LDAPDNPrincipal(dcfg.PolicyIssuer), dcfg.LDAP_PKC_Attribute);
                    if (cert != null) {
                        for (int i = 0; i < cert.size(); i++) ((issrg.utils.repository.VirtualRepository) policyRepository).populate(dcfg.PolicyIssuer, CustomisePERMIS.getUserCertificateAttribute(), cert.get(i));
                    } else {
                        log.error("PolicyIssuer has to have at least one certificate for verifying the policy AC");
                        throw new Throwable("PolicyIssuer has to have at least one certificate for verifying the policy AC");
                    }
                    log.debug("Repository that stores the policy is initialized");
                    log.debug("Calling DISRAPandParser to get the policy and others important objects");
                    policy = new DISRAPandParser(policyRepository, dcfg.PolicyIdentifier, new LDAPDNPrincipal(dcfg.PolicyIssuer), sv, dcfg.level);
                    log.info("Policy readin sucsessfully");
                } else {
                    log.debug("Calling DISRAPandParser to get the policy and others important objects");
                    policy = new SimpleDISRAPandParser(policyPath, sv, dcfg.level);
                    log.info("Policy readin sucsessfully");
                }
            } else {
                log.info("The policy location is not supported");
            }
            log.debug("Going to get the tokenParser, comparator and allocationPolicy objects");
            if (policy instanceof DISRAPandParser) {
                pp = ((DISRAPandParser) policy).getPolicyPaser();
            } else {
                pp = ((SimpleDISRAPandParser) policy).getPolicyPaser();
            }
            if (policy instanceof DISRAPandParser) {
                tokenParser = ((DISRAPandParser) policy).getAuthzTokenParser();
            } else {
                tokenParser = ((SimpleDISRAPandParser) policy).getAuthzTokenParser();
            }
            if (policy instanceof DISRAPandParser) {
                comparator = ((DISRAPandParser) policy).getComparator();
            } else {
                comparator = ((SimpleDISRAPandParser) policy).getComparator();
            }
            allocationPolicy = policy.getAllocationPolicy();
            roleHierarchyPolicy = (issrg.pba.rbac.xmlpolicy.XMLPolicyParser.RoleHierarchyPolicyNode) ((issrg.pba.rbac.xmlpolicy.XMLPolicyParser) pp).getAuthzTokenParsingRules().get(issrg.pba.rbac.RoleHierarchyPolicy.class);
            log.debug("Got the tokenParser, comparator and allocationPolicy objects");
            readRoleTypes();
            globalAuthTokenRep = new issrg.simplePERMIS.SimplePERMISAuthzTokenRepository(r, tokenParser);
            java.util.Map mapSOAs = ((issrg.pba.rbac.xmlpolicy.XMLPolicyParser) pp).getSOAs();
            boolean foundDISAsSOA = false;
            java.util.Collection collectionSOAs = mapSOAs.values();
            for (java.util.Iterator ite = collectionSOAs.iterator(); ite.hasNext(); ) {
                soas.add(ite.next());
            }
            LDAPDNPrincipal disPrincipal = new LDAPDNPrincipal(dcfg.DIS);
            for (int i = 0; i < soas.size(); i++) {
                LDAPDNPrincipal soaPrincipal = (LDAPDNPrincipal) soas.get(i);
                if (soaPrincipal.equals(disPrincipal)) {
                    foundDISAsSOA = true;
                    break;
                }
            }
            if (!foundDISAsSOA) {
                log.info("DIS is not an SOA. Going to get the DIS's ACs");
                getDISSerials();
                log.debug("Finish retrieving the DIS's ACs");
                if (aai == null) {
                    log.error("The DIS does not have any AC that can be used for delegation");
                    throw new Exception("The DIS does not have any AC that can be used for delegation");
                }
                log.debug("Going to create AAIA extension");
                String DIS_AC_LOCATION = new String(dcfg.CredentialLocation).concat("/").concat(dcfg.DIS);
                aaia = new issrg.ac.extensions.AttributeAuthorityInformationAccess(new String[] { DIS_AC_LOCATION });
                log.debug("The AAIA extension is created");
            } else log.info("DIS is an SOA in the policy");
            log.debug("Going to get issuerDISCertificateSerial object and the signature algorithms");
            DISGeneralNames = issrg.ac.Util.buildGeneralNames(dcfg.DIS);
            issuerDISCertificateSerial = new issrg.ac.IssuerSerial(issrg.ac.Util.buildGeneralNames(issuerDISCertificateDN), signerPKC.getSerialNumber(), null);
            byte[] bt = signerPKC.getSigAlgParams();
            ASN1Object algParams = bt == null ? null : iaik.asn1.DerCoder.decode(bt);
            signatureAlg = new iaik.asn1.structures.AlgorithmID(new iaik.asn1.ObjectID(signingUtility.getSigningAlgorithmID()));
            log.debug("Got the issuerDISCertificateSerial object and the signature algorithms. The DIS is initialized");
            log.info("The DIS is initialized. Log is enable at level " + dcfg.priority + ", policy ID: " + dcfg.PolicyIdentifier + " obtained from " + dcfg.PolicyLocation);
        } catch (Throwable th) {
            log.error("Error when initializing the DIS and the error message is : " + th.getMessage());
            Exception ex = new Exception("Error when initializing the Delegation Service");
            ex.initCause(th);
            throw ex;
        }
    }

    /**
     * This function tries to get DIS's ACs for creating AAI extension
     */
    private void getDISSerials() throws Exception {
        javax.naming.directory.Attribute attr;
        try {
            attr = ldapUtility.loadACs(dcfg.DIS);
            if (attr != null) {
                log.debug("The DIS's ACs are retrieved. The number of the DIS's ACs is: " + attr.size());
            } else {
                log.warn("The DIS does not have any attribute");
                return;
            }
            int num = attr.size();
            log.debug("Going to get the serialnumber of the DIS's AC for creating the AAI extension");
            Vector disIssuerSerials = new Vector();
            for (int i = 0; i < num; i++) {
                log.debug("Process the AC number " + i);
                byte[] acObject = (byte[]) attr.get(i);
                issrg.ac.AttributeCertificate ac = issrg.ac.AttributeCertificate.guessEncoding(acObject);
                log.debug(ac.toString());
                LDAPDNPrincipal holder = new LDAPDNPrincipal(issrg.ac.Util.generalNamesToString(ac.getACInfo().getHolder().getEntityName()));
                if (!holder.equals(new LDAPDNPrincipal(dcfg.DIS))) continue;
                String issuer = issrg.ac.Util.generalNamesToString(ac.getACInfo().getIssuer().getV1Form() == null ? ac.getACInfo().getIssuer().getV2Form().getIssuerName() : ac.getACInfo().getIssuer().getV1Form()).toLowerCase();
                LDAPDNPrincipal issuerPrincipal = new LDAPDNPrincipal(issuer);
                boolean foundSOAAsIssuer = false;
                for (int j = 0; j < soas.size(); j++) {
                    LDAPDNPrincipal soaPrincipal = (LDAPDNPrincipal) soas.get(j);
                    if (soaPrincipal.equals(issuerPrincipal)) {
                        foundSOAAsIssuer = true;
                        break;
                    }
                }
                if (!foundSOAAsIssuer) continue;
                log.debug("Going to decode the byte array of the DIS's AC for getting a token");
                ParsedToken token = tokenParser.decode(acObject);
                if (!(token instanceof DelegatableToken)) continue;
                DelegatableToken tokenD = (DelegatableToken) token;
                AssignmentRule assertRule = new AssignmentRule(tokenD.getSubjectDomain(), tokenD.getDepth(), tokenD.getDelegateableCredentials());
                Vector holders = new Vector();
                holders.add(tokenD.getHolder());
                log.debug("Going to validate the token");
                Vector rules = allocationPolicy.validate(token.getHolder(), token.getIssuerTokenLocator(), assertRule, globalAuthTokenRep, holders);
                if (rules.size() == 0) {
                    log.warn("This token does not give the DIS any rule (RAR), ignore it");
                    continue;
                }
                log.debug("Going to create the SOAGeneralName and SOAGeneralNames");
                GeneralName SOAGeneralName = new GeneralName(GeneralName.directoryName, new iaik.utils.RFC2253NameParser(issuer.toUpperCase()).parse());
                GeneralNames SOAGeneralNames = new GeneralNames(SOAGeneralName);
                log.debug("Got the generalName and generalNames");
                disIssuerSerials.add(new IssuerSerial(SOAGeneralNames, ac.getACInfo().getSerialNumber(), null));
            }
            int numberOfIssuerSerials = disIssuerSerials.size();
            log.debug("Number of the DIS's ACs: " + numberOfIssuerSerials);
            if (numberOfIssuerSerials > 0) {
                IssuerSerial[] a = new IssuerSerial[0];
                a = (IssuerSerial[]) disIssuerSerials.toArray(a);
                aai = new issrg.ac.attributes.AuthorityAttributeIdentifier((IssuerSerial[]) a);
            }
            if (aai != null) log.debug("AAI extension: " + aai.toString());
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            throw new Exception("Error when reading the DIS's ACs");
        }
    }

    /** 
     * This function read roletypes from the policy. The roleTypes map is used for reference later.
     */
    private void readRoleTypes() {
        roleTypes = new Hashtable(roleHierarchyPolicy.getTypeOid());
        String[] oids = (String[]) roleTypes.values().toArray(new String[0]);
        for (int i = 0; i < oids.length; i++) {
            issrg.ac.attributes.PermisRole.registerMe(oids[i]);
        }
    }

    /**
    *	This function will receive a request of storing an AC from a client (ACM tool).
     *  This AC is already signed by requestor but the DIS will check 
     *  the content of the AC to make sure it is comply to the issuing policy.
     *  If the checking process is succesful, the AC will be stored by the DIS.
     *  If not, error message will be return to the requestor.
    *	
    *	@param base64AC is the AC in base64 format sent to the DIS
    *	@return a string that reports to issuer about the result of the checking and storing process.
    *	    
    */
    protected String storeACForMe(byte[] ac, String requestor) {
        try {
            issrg.ac.AttributeCertificate acc = issrg.ac.AttributeCertificate.guessEncoding(ac);
            log.info("Request comes from user: " + requestor + " for storing the AC: " + acc.toString());
            String issuer = issrg.utils.RFC2253NameParser.toCanonicalDN(issrg.ac.Util.generalNamesToString(acc.getACInfo().getIssuer().getV1Form() == null ? acc.getACInfo().getIssuer().getV2Form().getIssuerName() : acc.getACInfo().getIssuer().getV1Form()));
            LDAPDNPrincipal issuerPrincipal = new LDAPDNPrincipal(issuer);
            LDAPDNPrincipal requestorPrincipal = new LDAPDNPrincipal(requestor);
            if (!issuerPrincipal.equals(requestorPrincipal)) {
                log.warn("requestor is not the issuer and the issuer is: " + issuer + ". Request is rejected.");
                return Comm.YOU_DID_NOT_ISSUE_THIS_AC;
            } else log.debug("requestor is the issuer of the AC: " + issuer);
            if (!checkSignature(ac)) {
                log.warn("Checking the signature failed. The request is rejected");
                return Comm.CHECK_SIGNATURE_FAILED;
            } else log.debug("Checking the signature ok");
            log.debug("Decode the AC, get the issuer, credentials, assertion and delegation depth");
            issrg.pba.ParsedToken tok = tokenParser.decode(ac);
            String issuerDN = tok.getIssuerTokenLocator().getEntry().getEntryName().getName();
            issrg.pba.Credentials creds1 = tok.getCredentials();
            String assertion = "can";
            if (creds1.equals(emptyCreds)) assertion = "cannot";
            int depth1 = -2;
            if (tok instanceof DelegatableToken) {
                DelegatableToken tokenD = (DelegatableToken) tok;
                depth1 = tokenD.getDepth();
                creds1 = tokenD.getDelegateableCredentials();
            }
            log.debug("Got the issuer, credentials, assertion and delegation depth");
            String holderDN = tok.getHolder().getEntryName().getName();
            log.debug("Holder of the AC is : " + holderDN);
            DepthsCreds depthsCreds = new DepthsCreds();
            depthsCreds.setDepth1(depth1);
            depthsCreds.setCreds1(creds1);
            log.debug("Going to check and constrain the request");
            String resultAfterCheckAndConstrain = checkAndConstrain(issuerDN, holderDN, assertion, depthsCreds);
            log.debug("Result is: " + resultAfterCheckAndConstrain);
            if (!resultAfterCheckAndConstrain.equals(SATISFIED)) return resultAfterCheckAndConstrain;
            if ((depth1 != depthsCreds.getDepth2()) || (!creds1.equals(depthsCreds.getCreds2()))) {
                log.warn("The request is rejected because the issuer does not have enough privileges to issue the requested AC");
                return Comm.CANNOT_DOWNGRADE_PRIVILEGES;
            }
            Vector v = ((SetOfSubsetsCredentials) creds1).getValue();
            StringBuffer sBuffer = new StringBuffer();
            for (int i = 0; i < v.size(); i++) {
                sBuffer.append(((PermisCredentials) ((ExpirableCredentials) v.get(i)).getExpirable()).getRoleValueAsString()).append(" ");
            }
            log.debug("Going to store the AC to LDAP");
            try {
                storeToLDAP(ac);
                log.info("The AC is stored in LDAP entry " + holderDN + " and the AC is:");
                log.info(acc.toString());
            } catch (Exception ee) {
                log.error("Error when writing to LDAP");
                return Comm.ERROR_WRITING_LDAP;
            }
            return prepareReturn(ac, assertion, depthsCreds.getDepth2());
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return Comm.CAN_NOT_DECODE_AC_TO_BE_SIGN;
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
       *
       *@return a string that represents the result of the delegation
    */
    protected String delegateForMe(String issuerDN, String holderDN, String[] roleValues, String roleType, String from, String to, String assertion, int deep) {
        log.debug("Going to create credentials from the request's parameters");
        String roleTypeValues = roleType + ":";
        for (int i = 0; i < roleValues.length; i++) roleTypeValues = roleTypeValues + roleValues[i] + " ";
        roleTypeValues = roleTypeValues.substring(0, roleTypeValues.length() - 1);
        SetOfSubsetsCredentials creds1 = createSet(roleTypeValues, from, to);
        log.debug("Credentials from the request's parameters is created");
        log.debug("Requested credentials: " + creds1.toString());
        if (creds1.getValue().isEmpty()) {
            log.warn("Role type and/or role values are not supported by policy");
            return Comm.ROLETYPE_OR_ROLE_VALUE_IS_NOT_SUPPORTED_IN_POLICY;
        }
        int depth1;
        Integer depthInt = new Integer(deep);
        if (depthInt.intValue() < 0) depth1 = -2; else if (depthInt.intValue() == 0) depth1 = -1; else depth1 = depthInt.intValue() - 1;
        DepthsCreds depthsCreds = new DepthsCreds();
        depthsCreds.setDepth1(depth1);
        depthsCreds.setCreds1(creds1);
        log.debug("Going to check and constrain the request");
        String resultAfterCheckAndConstrain = checkAndConstrain(issuerDN, holderDN, assertion, depthsCreds);
        log.debug("Result after checking and constraining the request: " + resultAfterCheckAndConstrain);
        if (!resultAfterCheckAndConstrain.equals(SATISFIED)) return resultAfterCheckAndConstrain;
        try {
            log.debug("Going to generate AC");
            byte[] ac = generateAC(issuerDN, holderDN, (SetOfSubsetsCredentials) depthsCreds.getCreds2(), assertion, depthsCreds.getDepth2());
            if (ac.length == 0) {
                log.warn("AC is in wrong format, length = 0");
                return Comm.ACCREATION_ERROR;
            }
            try {
                log.debug("Going to store the AC to the LDAP");
                storeToLDAP(ac);
                log.info("The AC is stored in the LDAP entry " + holderDN + " and the AC is:");
                log.info(issrg.ac.AttributeCertificate.guessEncoding(ac).toString());
            } catch (Exception ee) {
                log.error("Error when writing to LDAP: " + ee.getMessage());
                return Comm.ERROR_WRITING_LDAP;
            }
            return prepareReturn(ac, assertion, depthsCreds.getDepth2());
        } catch (Exception e) {
            log.error("Error when creating the AC" + e.getMessage() + "\n" + e.fillInStackTrace());
            return Comm.ACCREATION_ERROR;
        }
    }

    /**
 * This function checks the signature on the Attribute Certificate. issuerDN parameter comes from authentication/authorization
 *process. This function will check the signature of the AC against certificates of the issuer (issuerDN), not from issuer in 
 *the AC. 
 */
    private boolean checkSignature(byte[] ac) {
        try {
            log.debug("Checking the signature of the AC");
            byte[] data = issrg.ac.AttributeCertificate.getToBeSignedByteArray(ac);
            byte[] sign = (byte[]) issrg.ac.AttributeCertificate.guessEncoding(ac).getSignatureValue().getValue();
            AttributeCertificate acc = issrg.ac.AttributeCertificate.guessEncoding(ac);
            String algorithid = acc.getSignatureAlgorithm().getAlgorithm().getID();
            String issuerDN = issrg.ac.Util.generalNamesToString(acc.getACInfo().getIssuer().getV1Form() == null ? acc.getACInfo().getIssuer().getV2Form().getIssuerName() : acc.getACInfo().getIssuer().getV1Form());
            LDAPDNPrincipal issuerPrincipal = new LDAPDNPrincipal(issuerDN);
            EntryLocator issuerLocator = new EntryLocator(new UserEntry(issuerPrincipal), issuerPrincipal, r, null);
            java.security.cert.X509Certificate[] certs = ds.getVerificationCertificates(issuerLocator);
            boolean checkResult = ds.verify(data, sign, algorithid, certs);
            log.debug("Result of the checking process: " + checkResult);
            return checkResult;
        } catch (Exception e) {
            log.error("Error when checking the signature of the AC: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return false;
        }
    }

    /**
     * This method prepare the return string to caller for displaying purposes. The fields are separated by "|" sign. 
     * This return string contains all the relevant information of the signed AC. 
     *@param ac is the AC
     *@param assertion is a string says whether the holder can assert privilege contained in the AC. 
     *@param depth is the delegation depth.
     *Note: the last two parameters are redundant but we can ignore the extension processing step with these two parameters
     *@return a string that presents the result of the whole process
     */
    private String prepareReturn(byte[] ac, String assertion, int depth) {
        try {
            log.debug("Start creating the string that represents the result of the issuing process");
            String done = Comm.PUBLISH;
            issrg.ac.AttributeCertificate acc = issrg.ac.AttributeCertificate.guessEncoding(ac);
            String holder = issrg.ac.Util.generalNamesToString(acc.getACInfo().getHolder().getEntityName());
            done = done + SEPARATOR + holder + SEPARATOR;
            Vector attributes = acc.getACInfo().getAttributes();
            for (int i = 0; i < attributes.size(); i++) {
                issrg.ac.Attribute att = (issrg.ac.Attribute) attributes.get(i);
                String typeOID = att.getType();
                String roleType = roleHierarchyPolicy.getTypeByOID(typeOID);
                done = done + roleType + ":";
                Vector roles = att.getValues();
                for (int j = 0; j < roles.size(); j++) {
                    PermisRole r = (PermisRole) roles.get(j);
                    done = done + r.getRoleValue() + ",";
                }
                done = done.substring(0, done.length() - 1);
                done = done + "+";
            }
            done = done.substring(0, done.length() - 1);
            done = done + SEPARATOR;
            done = done + acc.getACInfo().getValidityPeriod().getNotBefore().getTime().getTime().toString() + SEPARATOR;
            done = done + acc.getACInfo().getValidityPeriod().getNotAfter().getTime().getTime().toString() + SEPARATOR;
            done = done + (assertion.equals("can") ? Comm.CAN_ASSERT : Comm.CAN_NOT_ASSERT) + SEPARATOR;
            depth++;
            done = done + new Integer(depth).toString();
            log.debug("Done, the string is : " + done);
            return done;
        } catch (iaik.asn1.CodingException ce) {
            log.error("Error: " + ce.getMessage() + "\n" + ce.fillInStackTrace());
            return Comm.CAN_NOT_DECODE_AC_TO_BE_SIGN;
        }
    }

    /**
     * This function check and constrain the requested token.
     *@param token is the requested token
     *@param depthsCreds is used for storing the credentials and delegation depth
     *during the process
     *@return a boolean value, it tells the requested token is accepted or not
     */
    private boolean checkAndConstrain(ParsedToken token, DepthsCreds depthsCreds) {
        try {
            Vector holders = new Vector();
            holders.add(token.getHolder());
            if (!(token instanceof DelegatableToken)) {
                log.debug("Token is non-delegateable token. Going to validate the credentials");
                Credentials credsCon = allocationPolicy.validate(token.getHolder(), token.getIssuerTokenLocator(), token.getCredentials(), globalAuthTokenRep, holders);
                log.debug("Credentials after checking and constraining is: " + credsCon.toString());
                if (credsCon.equals(emptyCreds)) {
                    log.debug("Valid credentials is empty. Return false!");
                    return false;
                }
                if (credsCon.contains(depthsCreds.getCreds1())) {
                    depthsCreds.setCreds2((Credentials) depthsCreds.getCreds1().clone());
                    depthsCreds.setDepth2(-2);
                    log.debug("Valid credentials equals to request credentials. Return true.");
                    return true;
                }
                if (!dcfg.downgradeable) {
                    log.debug("Can not downgrade the request. Return false!");
                    return false;
                }
                depthsCreds.setCreds2(credsCon);
                depthsCreds.setDepth2(-2);
                log.debug("Credentials is constrained. Return true.");
                return true;
            } else {
                log.debug("Token is a delegateable token, going to validate the asserted RAR");
                DelegatableToken tokenD = (DelegatableToken) token;
                AssignmentRule asRAR = new AssignmentRule(tokenD.getSubjectDomain(), tokenD.getDepth(), tokenD.getDelegateableCredentials());
                Vector vRars = allocationPolicy.validate(tokenD.getHolder(), tokenD.getIssuerTokenLocator(), asRAR, globalAuthTokenRep, holders);
                StringBuffer rarsString = new StringBuffer();
                for (int i = 0; i < vRars.size(); i++) rarsString.append(((AssignmentRule) vRars.get(i)).toString()).append("   ");
                log.debug("Valid RARs are: " + rarsString.toString());
                if (vRars.isEmpty()) {
                    if (!dcfg.downgradeable) {
                        log.debug("There is no valid RAR and can not downgrade the request. Return false!");
                        return false;
                    } else {
                        log.debug("Because there is no valid RAR, the token is downgraded to a non-delegateable token. Validate the credentials");
                        Credentials vCreds = allocationPolicy.validate(token.getHolder(), token.getIssuerTokenLocator(), token.getCredentials(), globalAuthTokenRep, holders);
                        log.debug("Valid credentials: " + vCreds.toString());
                        if (vCreds.equals(emptyCreds)) {
                            log.debug("Valid credentials is empty. Return false!");
                            return false;
                        }
                        depthsCreds.setCreds2(vCreds);
                        depthsCreds.setDepth2(-2);
                        return true;
                    }
                }
                log.debug("Going to find the best valid RAR of the issuer");
                Vector tokens = new Vector();
                for (int i = 0; i < vRars.size(); i++) {
                    AssignmentRule vRar = (AssignmentRule) vRars.get(i);
                    DelegatableToken t = new DefaultDelegatableToken(token.getHolder(), token.getIssuerTokenLocator(), emptyCreds, vRar.getCredentials(), vRar.getSubjectDomain(), vRar.getDelegationDepth());
                    tokens.add(t);
                }
                ParsedToken[] tokensSorted = comparator.predict(asRAR, tokens, token.getHolder());
                DelegatableToken bestOne = (DelegatableToken) tokensSorted[0];
                log.debug("The best valid RAR of the issuer is: " + bestOne.getDelegateableCredentials() + " " + bestOne.getDepth() + " " + bestOne.getSubjectDomain());
                Credentials vCreds = bestOne.getDelegateableCredentials();
                if (vCreds.equals(emptyCreds)) {
                    log.debug("Delegateable credentials is the best RAR is empty. Return false!");
                    return false;
                }
                log.debug("Going to constrain the request...");
                int vdepth = bestOne.getDepth();
                if ((vCreds.contains(depthsCreds.getCreds1())) && (vdepth == depthsCreds.getDepth1())) {
                    depthsCreds.setCreds2((Credentials) depthsCreds.getCreds1().clone());
                    depthsCreds.setDepth2(vdepth);
                    log.debug("The request is constrained and the constrained result is: " + depthsCreds.getCreds2().toString() + " and delegation depth is: " + depthsCreds.getDepth2());
                    return true;
                } else {
                    if (!dcfg.downgradeable) {
                        log.debug("Can not downgrade the request. Return false!");
                        return false;
                    }
                    depthsCreds.setCreds2(vCreds);
                    depthsCreds.setDepth2(vdepth);
                    log.debug("The request is constrained and the constrained result is: " + depthsCreds.getCreds2().toString() + " and delegation depth is: " + depthsCreds.getDepth2());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return false;
        }
    }

    /**
    * This function checks whether issuer can issue a requested AC
    *@param issuerDN is the issuer of the AC
    *@param holderDN is the holder of the AC
    *@assertion indicates whether holder can assert privilege in AC    
    *@param depthsCreds stores requested credentials, requested delegation depth, constrained credentials, constrained delegation depth
    *@return a String that signals the request is satisfied or not 
    */
    private String checkAndConstrain(String issuerDN, String holderDN, String assertion, DepthsCreds depthsCreds) {
        issrg.pba.ParsedToken token1 = createParsedToken(issuerDN, holderDN, depthsCreds.getCreds1(), assertion, depthsCreds.getDepth1());
        log.debug("Going to check and constrain the token with the issuer's RARs");
        if (checkAndConstrain(token1, depthsCreds)) {
            log.debug("Checking with the issuer's RAR is done. Before checking with the DIS, the request is: " + depthsCreds.getCreds2() + " " + depthsCreds.getDepth2());
            issrg.pba.ParsedToken token2 = createParsedToken(dcfg.DIS, holderDN, depthsCreds.getCreds2(), assertion, depthsCreds.getDepth2());
            log.debug("Going to check and constrain the token with the DIS's RARs");
            if (checkAndConstrain(token2, depthsCreds)) {
                log.debug("After checking with the DIS, the valid credentials and delegation depth are: " + depthsCreds.getCreds2() + " " + depthsCreds.getDepth2());
                return SATISFIED;
            } else {
                log.debug("The DIS does not have enough privileges to issue the requested AC");
                return Comm.DIS_DO_NOT_HAVE_ENOUGH_PRIVILEGE;
            }
        } else {
            log.debug("Issuer does not have enough privileges to issue the requested AC");
            return Comm.ISSUER_DONOT_HAVE_ENOUGH_PRIVILEGES_OR_CAN_NOT_DOWNGRADE_PRIVILEGE_OR_WRONG_REQUEST;
        }
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
        log.debug("Start getting the serialnumber");
        try {
            byte[] serial;
            java.net.URL file = null;
            file = this.getClass().getResource(DISConfig.serial_file);
            if (file == null) {
                String tmp = "" + System.currentTimeMillis();
                serial = tmp.getBytes();
            } else {
                java.io.InputStream is = new FileInputStream(file.getFile());
                serial = new byte[is.available()];
                is.read(serial);
                if (serial.length == 0) {
                    String tmp = "" + System.currentTimeMillis();
                    serial = tmp.getBytes();
                }
            }
            String s;
            MessageDigest md = MessageDigest.getInstance("SHA");
            BigInteger bi = new BigInteger(serial);
            md.update(bi.toByteArray());
            byte[] result = md.digest();
            BigInteger bir = new BigInteger(result);
            bir = bir.abs();
            s = bir.toString(16);
            log.debug("Serial number is : " + s);
            File outfile = dcfg.getSerialFile();
            FileOutputStream f = new FileOutputStream(outfile);
            f.write(s.getBytes());
            f.close();
            return s;
        } catch (IOException ioe) {
            log.error("File not found: " + ioe.getMessage() + "\n" + ioe.fillInStackTrace());
        } catch (NoSuchAlgorithmException nsae) {
            log.error("Algorithm is not found: " + nsae.getMessage() + "\n" + nsae.fillInStackTrace());
        }
        return null;
    }

    /**
     * This function creates an AC from parameters.
     *@param issuerDN is the DN of the issuer of the AC
     *@param holderDN is the DN of the holder of the AC
     *@param creds2 is the credentials stored in the AC
     *@param assertion is the string indicates that holder of the AC can or can not assert privilege 
     *@depth is the delegation depth
     *@return is the AC stored in a byte array
     */
    private byte[] generateAC(String issuerDN, String holderDN, SetOfSubsetsCredentials creds2, String assertion, int depth2) throws Exception {
        log.debug("Start generating AC");
        try {
            BigInteger ACSerialNumber;
            ACSerialNumber = new BigInteger(this.getSerialNumber(), 16);
            ACSerialNumber = ACSerialNumber.abs();
            log.debug("AC credentials is: " + creds2.toString());
            ValidityPeriod vp = ((ExpirableCredentials) creds2.getValue().get(0)).getValidityPeriod();
            Calendar nb = CustomisePERMIS.getCalendar();
            nb.setTime(vp.getNotBefore());
            Calendar na = CustomisePERMIS.getCalendar();
            na.setTime(vp.getNotAfter());
            issrg.ac.Generalized_Time notBf = new issrg.ac.Generalized_Time(nb);
            issrg.ac.Generalized_Time notAf = new issrg.ac.Generalized_Time(na);
            log.debug("Validity period for the AC is: " + nb.getTime().toString() + "  " + na.getTime().toString());
            issrg.ac.AttCertValidityPeriod validity_period = new issrg.ac.AttCertValidityPeriod(notBf, notAf);
            log.debug("Going to generate roles for the AC");
            Vector r = creds2.getValue();
            Hashtable roleTypesValues = new Hashtable();
            Vector attributes = new Vector();
            for (int i = 0; i < r.size(); i++) {
                ExpirableCredentials exp = (ExpirableCredentials) r.get(i);
                PermisCredentials permisCredentials = (PermisCredentials) exp.getExpirable();
                String type = permisCredentials.getRoleType();
                String roleTypeID = (String) roleTypes.get(type);
                Vector a = (Vector) roleTypesValues.get(roleTypeID);
                if (a == null) {
                    roleTypesValues.put(roleTypeID, a = new Vector());
                    attributes.add(new issrg.ac.Attribute(roleTypeID, a));
                }
                a.add(new issrg.ac.attributes.PermisRole(permisCredentials.getRoleValueAsString()));
            }
            if (attributes.size() == 0) {
                log.debug("No role for the AC!");
                return new byte[0];
            }
            for (int i = 0; i < attributes.size(); i++) {
                log.debug("The AC contains: ");
                log.debug(attributes.get(i).toString());
            }
            log.debug("Going to create extensions for the AC");
            Vector extensionCollection = new Vector();
            if (!assertion.equals("can")) extensionCollection.add(new issrg.ac.attributes.NoAssertion());
            if (depth2 > -2) {
                extensionCollection.add(new issrg.ac.attributes.BasicAttConstraint(false, depth2));
            }
            String DN = issrg.utils.RFC2253NameParser.toCanonicalDN(issrg.utils.RFC2253NameParser.distinguishedName(issuerDN));
            GeneralName issuerGeneralName = new GeneralName(GeneralName.directoryName, new iaik.utils.RFC2253NameParser(DN).parse());
            extensionCollection.add(new issrg.ac.attributes.IssuedOnBehalfOf(false, issuerGeneralName));
            if (aai != null) extensionCollection.add(aai);
            if (aaia != null) extensionCollection.add(aaia);
            issrg.ac.Extensions extensions = new issrg.ac.Extensions(extensionCollection);
            for (int i = 0; i < extensionCollection.size(); i++) {
                log.debug("The extension contains: ");
                log.debug(extensionCollection.get(i).toString());
            }
            iaik.asn1.structures.GeneralNames hn = issrg.ac.Util.buildGeneralNames(holderDN);
            issrg.ac.Holder holder = new issrg.ac.Holder(null, hn, null);
            issrg.ac.AttCertIssuer issuer;
            issrg.ac.V2Form signer = new issrg.ac.V2Form(DISGeneralNames, issuerDISCertificateSerial, null);
            signer.setObjectDigestInfo(null);
            issuer = new issrg.ac.AttCertIssuer(null, signer);
            log.debug("Going to create attribute certificate info for the AC");
            issrg.ac.AttributeCertificateInfo aci = new issrg.ac.AttributeCertificateInfo(new issrg.ac.AttCertVersion(issrg.ac.AttCertVersion.V2), holder, issuer, signatureAlg, ACSerialNumber, validity_period, attributes, null, extensions);
            log.debug("The AC info is: " + aci.toString());
            log.debug("Going to create the AC");
            byte[] b = aci.getEncoded();
            byte[] ac = new issrg.ac.AttributeCertificate(aci, signatureAlg, new BIT_STRING(signingUtility.sign(b))).getEncoded();
            log.debug("AC is created");
            return ac;
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            throw new Exception("Error when generating requested AC");
        }
    }

    /** 
     * This method stores the AC to LDAP 
     *@param ac is a byte array. It is the AC needs to be stored
     **/
    private void storeToLDAP(byte[] ac) throws Exception {
        try {
            issrg.ac.AttributeCertificate acc = issrg.ac.AttributeCertificate.guessEncoding(ac);
            log.debug("The AC is going to be stored in LDAP right now...");
            synchronized (ldapUtility) {
                ldapUtility.save(ac);
                log.debug("AC is stored in LDAP");
            }
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            throw new Exception("Error when writing to LDAP");
        }
    }

    /**
     * This function creates a parsedToken out of information of an AC and a string indicates
     * who you want to be the issuer of this AC: issuer or issuedOnBehalfOf
     */
    private issrg.pba.ParsedToken createParsedToken(AttributeCertificate ac, String issuerDN) {
        try {
            log.debug("Going to create a parsedToken object from an AC...");
            ParsedToken token = tokenParser.decode(ac.getEncoded());
            LDAPDNPrincipal issuerPrincipal = new LDAPDNPrincipal(token.getIssuerTokenLocator().getEntry().getEntryName().getName());
            LDAPDNPrincipal issuerDNPricipal = new LDAPDNPrincipal(issuerDN);
            if (issuerPrincipal.equals(issuerDNPricipal)) {
                {
                    log.debug("A token is created: " + token.toString());
                    return token;
                }
            } else {
                ParsedToken tok;
                EntryLocator entry = new EntryLocator(new UserEntry(issuerDNPricipal), issuerDNPricipal, r, null);
                if (token instanceof DefaultDelegatableToken) {
                    DefaultDelegatableToken del = (DefaultDelegatableToken) token;
                    tok = new DefaultDelegatableToken(del.getHolder(), entry, del.getCredentials(), del.getDelegateableCredentials(), del.getSubjectDomain(), del.getDepth());
                } else {
                    tok = new DefaultParsedToken(token.getHolder(), entry, token.getCredentials());
                }
                log.debug("A token is created: " + tok.toString());
                return tok;
            }
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return null;
        }
    }

    /**
     * This function creates a ParsedToken object from string parameters.
     *@param issuerDN is the DN of issuer
     *@param holderDN is the DN of the holder
     *@param creds is the credentials stored in the token
     *@param assertion is the flag that indicates the existance of ASSERTION extension 
     *@param deep is the delegation depth     
     *@return a parsed token
     */
    private issrg.pba.ParsedToken createParsedToken(String issuerDN, String holderDN, Credentials creds, String assertion, int deep) {
        try {
            log.debug("Going to create a parsedToken object from parameters issuerDN " + issuerDN + ", holder " + holderDN + ", credentials " + creds.toString() + ", delegation depth" + deep);
            issrg.utils.repository.TokenLocator issuerLocator;
            LDAPDNPrincipal issuerPrincipal = new LDAPDNPrincipal(issrg.utils.RFC2253NameParser.toCanonicalDN(issuerDN));
            issuerLocator = new EntryLocator(new UserEntry(issuerPrincipal), issuerPrincipal, r, null);
            Credentials assertableCredentials = creds;
            Credentials delegateableCredentials = creds;
            if (!assertion.equals("can")) assertableCredentials = new SetOfSubsetsCredentials();
            if (deep < -1) {
                ParsedToken token = new issrg.pba.DefaultParsedToken(new UserEntry(new LDAPDNPrincipal(holderDN)), issuerLocator, assertableCredentials);
                log.debug("Created token is: " + token.toString());
                return token;
            }
            ParsedToken token = new issrg.pba.DefaultDelegatableToken(new UserEntry(new LDAPDNPrincipal(holderDN)), issuerLocator, assertableCredentials, delegateableCredentials, new issrg.pba.rbac.policies.DITSubtree(LDAPDNPrincipal.WHOLE_WORLD_DN, 0, -1, null, null), deep);
            log.debug("Created token is: " + token.toString());
            return token;
        } catch (Exception pe) {
            log.error("Error: " + pe.getMessage() + "\n" + pe.fillInStackTrace());
            return null;
        }
    }

    /**
     * This function creates Credentials from strings.
     *@param roleTypesValues is a String and stores role types and values of each type
     *@param from is a string that tells the NotBefore time instance
     *@param to is a string that tells the NotAfter time instance
     *@return a credentials with SetOfSubsetsCredentials type
     */
    private SetOfSubsetsCredentials createSet(String roleTypesValues, String from, String to) {
        log.debug("Create a credentials from strings: " + roleTypesValues + " from: " + from + " to: " + to);
        Date nb, na;
        java.text.DateFormat dateformat = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
        try {
            nb = dateformat.parse(from);
            na = dateformat.parse(to);
        } catch (java.text.ParseException pe) {
            try {
                nb = issrg.dis.Util.buildGeneralizedTime(from).getTime().getTime();
                na = issrg.dis.Util.buildGeneralizedTime(to).getTime().getTime();
            } catch (Exception e) {
                log.error("Error with date representation: " + e.getMessage());
                return null;
            }
        }
        ValidityPeriod vp = new AbsoluteValidityPeriod(nb, na);
        Vector rTypesValues = new Vector();
        Enumeration tok1 = new java.util.StringTokenizer(roleTypesValues, SEPARATOR_APACHE);
        Vector vector1 = new Vector();
        while (tok1.hasMoreElements()) vector1.add(tok1.nextElement());
        for (int i = 0; i < vector1.size(); i++) {
            String typeValues = (String) vector1.get(i);
            int j = typeValues.indexOf(":");
            if (j > 0) {
                String type = typeValues.substring(0, j);
                Enumeration enumeration = ((Hashtable) roleTypes).keys();
                boolean found = false;
                while (enumeration.hasMoreElements()) {
                    String o = (String) enumeration.nextElement();
                    if (type.equals(o)) {
                        found = true;
                        break;
                    }
                }
                if (!found) continue;
                String values = typeValues.substring(j + 1, typeValues.length()).trim();
                Enumeration tok2 = new java.util.StringTokenizer(values, SPACE);
                while (tok2.hasMoreElements()) {
                    issrg.pba.rbac.RoleHierarchyNode rhn = roleHierarchyPolicy.getRole(type, (String) tok2.nextElement());
                    if (rhn == null) continue;
                    rTypesValues.add(new ExpirableCredentials(new PermisCredentials(rhn), vp));
                }
            }
        }
        SetOfSubsetsCredentials set = new SetOfSubsetsCredentials(rTypesValues);
        log.debug("Created credentials: " + set.toString());
        return set;
    }

    /**
     * This function searchs for ACs of an user and returns information about these ACs. The ACs that violate 
     *  the policy of either the issuer or issuedOnBehalfOf will be marked.
     *
     *@param holderDN is the DN of user that one wants to retrieve his/her ACs
     *@param requestorDN is the DN of the requestor
     *@return array of String. This array stores information about all user's ACs
     */
    protected String[] searchForMe(String requestorDN, String holderDN) {
        try {
            javax.naming.directory.Attribute attrs = ldapUtility.loadACs(holderDN);
            if (attrs == null) {
                log.warn("Holder does not have any AC");
                return null;
            }
            int num = attrs.size();
            log.debug("Number of user's ACs: " + num);
            if (num == 0) {
                log.warn("Holder does not have any AC");
                return null;
            }
            Vector r = new Vector();
            Vector viewable = new Vector();
            for (int i = 0; i < num; i++) {
                byte[] acObject = (byte[]) attrs.get(i);
                try {
                    issrg.ac.AttributeCertificate ac = issrg.ac.AttributeCertificate.guessEncoding(acObject);
                    String issuer = issrg.utils.RFC2253NameParser.toCanonicalDN(issrg.ac.Util.generalNamesToString(ac.getACInfo().getIssuer().getV1Form() == null ? ac.getACInfo().getIssuer().getV2Form().getIssuerName() : ac.getACInfo().getIssuer().getV1Form()));
                    LDAPDNPrincipal issuerPrincipal = new LDAPDNPrincipal(issuer);
                    LDAPDNPrincipal requestorPrincipal = new LDAPDNPrincipal(requestorDN);
                    LDAPDNPrincipal holderPrincipal = new LDAPDNPrincipal(holderDN);
                    String iobo = null;
                    Vector extensions = ac.getACInfo().getExtensions().getValues();
                    for (int j = 0; j < extensions.size(); j++) {
                        Extension e = (Extension) extensions.get(j);
                        if (e instanceof IssuedOnBehalfOf) {
                            iobo = ((IssuedOnBehalfOf) e).getIssuerDN();
                            break;
                        }
                    }
                    LDAPDNPrincipal ioboPrincipal = null;
                    if (iobo != null) ioboPrincipal = new LDAPDNPrincipal(iobo);
                    if ((!dcfg.SearchRequestor.equals("anyone") && ((!requestorPrincipal.equals(issuerPrincipal)) && (!requestorPrincipal.equals(holderPrincipal)) && (!requestorPrincipal.equals(ioboPrincipal))))) {
                        if (!issueable(ac, requestorDN)) continue;
                    }
                    log.debug("going to create ac message");
                    r.add(createACMessage(ac));
                    viewable.add(ac);
                } catch (Exception ce) {
                    log.warn("Found a broken AC");
                    r.add(Comm.BROKEN_AC);
                }
            }
            String[] ret = new String[0];
            ret = (String[]) r.toArray(ret);
            if (viewable.size() == 0) {
                log.info("Requestor either is not allowed to view the holder's attributes or the holder does not have any attribute");
            } else {
                log.info("All the holder's ACs that can be viewed are:");
                for (int i = 0; i < viewable.size(); i++) {
                    AttributeCertificate ac = (AttributeCertificate) viewable.get(i);
                    log.info(ac.toString());
                }
            }
            return ret;
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return null;
        }
    }

    /**
     * This function returns a message about an AC. This message contains information about the AC
     * and a flag that indicates whether the AC violates the RARs of 
     * its issuer and issuedOnBehalfOf entity or not
     */
    private String createACMessage(issrg.ac.AttributeCertificate ac) {
        log.debug("Creating AC message. This message show the AC's content and tells the validity of the AC. The message will be returned to user's webbrowser");
        StringBuffer ret = new StringBuffer();
        String issuerDN = issrg.ac.Util.generalNamesToString(ac.getACInfo().getIssuer().getV1Form() == null ? ac.getACInfo().getIssuer().getV2Form().getIssuerName() : ac.getACInfo().getIssuer().getV1Form()).toUpperCase();
        ret.append(issuerDN).append(SEPARATOR);
        ret.append(issrg.ac.Util.generalNamesToString(ac.getACInfo().getHolder().getEntityName()).toUpperCase()).append(SEPARATOR);
        ret.append(ac.getACInfo().getSerialNumber().toString()).append(SEPARATOR);
        boolean policy_found = false;
        String roleTypesValues = "";
        Vector attrs = ac.getACInfo().getAttributes();
        for (int i = 0; i < attrs.size(); i++) {
            issrg.ac.Attribute attr = (issrg.ac.Attribute) attrs.get(i);
            String oid = attr.getType();
            String type = "";
            if (oid.equals(issrg.ac.attributes.PMIXMLPolicy.PMI_XML_POLICY_ATTRIBUTE_OID)) {
                roleTypesValues = roleTypesValues + Comm.PMI_XML_POLICY + SEPARATOR_APACHE;
                policy_found = true;
                break;
            }
            Enumeration enumeration = ((Hashtable) roleTypes).keys();
            while (enumeration.hasMoreElements()) {
                Object o = enumeration.nextElement();
                String OID = (String) roleTypes.get(o);
                if (OID.equals(oid)) {
                    type = (String) o;
                    break;
                }
            }
            roleTypesValues = roleTypesValues + type + ":";
            Vector values = attr.getValues();
            for (int j = 0; j < values.size() - 1; j++) {
                PermisRole value = (PermisRole) values.get(j);
                roleTypesValues = roleTypesValues + value.getRoleValue() + SPACE;
            }
            PermisRole value = (PermisRole) values.get(values.size() - 1);
            roleTypesValues = roleTypesValues + value.getRoleValue();
            roleTypesValues = roleTypesValues + SEPARATOR_APACHE;
        }
        roleTypesValues = roleTypesValues.substring(0, roleTypesValues.length() - 1);
        ret.append(roleTypesValues).append(SEPARATOR);
        ret.append(ac.getACInfo().getValidityPeriod().getNotBefore().getTime().getTime().toString()).append(SEPARATOR);
        ret.append(ac.getACInfo().getValidityPeriod().getNotAfter().getTime().getTime().toString()).append(SEPARATOR);
        if (!policy_found) {
            Extensions exts = ac.getACInfo().getExtensions();
            int depth = -1;
            String assertion = Comm.CAN_ASSERT;
            String iobo = Comm.ISSUED_ON_BEHALF_OF_NOT_PRESENT.intern();
            if (exts != null) {
                Vector extensions = exts.getValues();
                for (int i = 0; i < extensions.size(); i++) {
                    Extension e = (Extension) extensions.get(i);
                    if (e instanceof BasicAttConstraint) {
                        depth = ((BasicAttConstraint) e).getDepth() + 1;
                        continue;
                    }
                    if (e instanceof NoAssertion) {
                        assertion = Comm.CAN_NOT_ASSERT;
                        continue;
                    }
                    if (e instanceof IssuedOnBehalfOf) {
                        iobo = ((IssuedOnBehalfOf) e).getIssuerDN().intern();
                        continue;
                    }
                }
            }
            ret.append(new Integer(depth).toString()).append(SEPARATOR);
            ret.append(assertion).append(SEPARATOR);
            ret.append(iobo).append(DOUBLE_SEPARATOR);
            log.debug("Content of the AC: " + ret.toString());
            boolean valid1, valid2;
            valid2 = true;
            valid1 = issueable(ac, issuerDN);
            if (iobo != Comm.ISSUED_ON_BEHALF_OF_NOT_PRESENT) valid2 = issueable(ac, iobo);
            if (valid1 && valid2) ret.append("1"); else ret.append("0");
            log.debug("Content of the AC and its validity (if the last character is 1 then the AC is valid, otherwise, the AC is not valid):" + ret.toString());
            return ret.toString();
        } else {
            log.debug("The AC is a policy");
            return ret.substring(0, ret.length() - 1);
        }
    }

    /**
     * This method checks whether an issuer can issue an AC with the same credentials and
     * delegation depth as of the input AC. If the RAR from the AC is the delegateable one, the function will
     * validate it and get the set of valid RARs. The best RAR is choosen to compare with the 
     * RAR from the AC. If the delegateable credentials of the best RAR contains the delegateable 
     * credentials of the AC and the delegation depth of the best RAR equals to the AC's 
     * delegation depth then the function will return true. Otherwise, the function will
     * return false.
     *
     * If the AC is not delegateable then its credentials will be validated. If 
     * the valid credentials contains (or equal to) the AC's credentials then
     * the function will return true; otherwise it will return false.
     *@param ac is the AC
     *@param issuerDN is the intended DN of the issuer of the above AC
     *@return a boolean value that tells whether the AC can be issued by the issuer
     * 
     */
    private boolean issueable(AttributeCertificate ac, String issuerDN) {
        try {
            log.debug("Going to check if one can issue a particular AC or not");
            ParsedToken tok = tokenParser.decode(ac.getEncoded());
            ParsedToken token;
            EntryLocator entry = new EntryLocator(new UserEntry(new LDAPDNPrincipal(issuerDN)), new LDAPDNPrincipal(issuerDN), r, null);
            if (tok instanceof DelegatableToken) {
                DelegatableToken tokD = (DelegatableToken) tok;
                token = new DefaultDelegatableToken(tokD.getHolder(), entry, tokD.getCredentials(), tokD.getDelegateableCredentials(), tokD.getSubjectDomain(), tokD.getDepth());
            } else {
                token = new DefaultParsedToken(tok.getHolder(), entry, tok.getCredentials());
            }
            log.debug("The token comes from the AC is: " + token.toString());
            Vector holders = new Vector();
            holders.add(token.getHolder());
            if (token instanceof DelegatableToken) {
                DelegatableToken tokenD = (DelegatableToken) token;
                AssignmentRule assRule = new AssignmentRule(tokenD.getSubjectDomain(), tokenD.getDepth(), tokenD.getDelegateableCredentials());
                log.debug("Asserted rule is: " + assRule.toString());
                Vector vRules = new Vector();
                vRules = allocationPolicy.validate(tokenD.getHolder(), tokenD.getIssuerTokenLocator(), assRule, globalAuthTokenRep, holders);
                StringBuffer rarsString = new StringBuffer();
                for (int i = 0; i < vRules.size(); i++) rarsString.append(((AssignmentRule) vRules.get(i)).toString()).append("   ");
                log.debug("Valid issuer's rules are: " + rarsString.toString());
                if (vRules.isEmpty()) return false;
                Vector tokens = new Vector();
                for (int i = 0; i < vRules.size(); i++) {
                    AssignmentRule vRule = (AssignmentRule) vRules.get(i);
                    DelegatableToken t = new DefaultDelegatableToken(token.getHolder(), token.getIssuerTokenLocator(), emptyCreds, vRule.getCredentials(), new DITSubtree((LDAPDNPrincipal) token.getHolder().getEntryName(), 0, -1, null, null), vRule.getDelegationDepth());
                    tokens.add(t);
                }
                log.debug("Going to get the best RAR of the issuer");
                ParsedToken[] tokensSorted = comparator.predict(assRule, tokens, token.getHolder());
                DelegatableToken bestOne = (DelegatableToken) tokensSorted[0];
                log.debug("The best RAR of the issuer is: " + bestOne.toString());
                if (bestOne.getDelegateableCredentials().contains(tokenD.getDelegateableCredentials()) && (bestOne.getDepth() == tokenD.getDepth())) {
                    log.debug("The issuer can issue the AC");
                    return true;
                } else {
                    log.debug("The issuer can not issue the AC");
                    return false;
                }
            } else {
                log.debug("The token is not delegateable. Going to validate the credentials...");
                Credentials vCreds = allocationPolicy.validate(token.getHolder(), token.getIssuerTokenLocator(), token.getCredentials(), globalAuthTokenRep, holders);
                log.debug("The validated credentials is: " + vCreds.toString());
                if (vCreds.contains(token.getCredentials())) {
                    log.debug("The issuer can issue the AC");
                    return true;
                } else {
                    log.debug("The issuer can not issue the AC");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return false;
        }
    }

    /**
     *  This function checks whether an AC can be issued by one authority indicated by DN.
     *  If the client is either the holder, issuer or iobo then the client can issue the AC. 
     *Otherwise, replace the issuer by the client and check.
     *
     *@param ac is the requested AC that needs to be checked
     *@param clientDN is the DN of the issuer.
     *@return a boolean value. If the issuer can issue the AC, function returns true. Otherwise, it returns false
     */
    private boolean canBeIssued(AttributeCertificate ac, String clientDN) {
        if (ac == null) return false;
        try {
            String issuerDN = issrg.ac.Util.generalNamesToString(ac.getACInfo().getIssuer().getV1Form() == null ? ac.getACInfo().getIssuer().getV2Form().getIssuerName() : ac.getACInfo().getIssuer().getV1Form()).toUpperCase();
            String holderDN = issrg.ac.Util.generalNamesToString(ac.getACInfo().getHolder().getEntityName()).toUpperCase();
            String issuedOnBehalfOf = "";
            Extensions exts = ac.getACInfo().getExtensions();
            if (exts != null) {
                Vector extensions = exts.getValues();
                for (int i = 0; i < extensions.size(); i++) {
                    issrg.ac.Extension e = (issrg.ac.Extension) extensions.get(i);
                    if (e instanceof issrg.ac.attributes.IssuedOnBehalfOf) {
                        issuedOnBehalfOf = ((issrg.ac.attributes.IssuedOnBehalfOf) e).getIssuerDN().toUpperCase();
                        break;
                    }
                }
            }
            log.debug("Checking whether the client can issue (hence, he can revoke) the AC");
            LDAPDNPrincipal clientPrincipal = new LDAPDNPrincipal(clientDN);
            LDAPDNPrincipal holderPrincipal = new LDAPDNPrincipal(holderDN);
            LDAPDNPrincipal issuerPrincipal = new LDAPDNPrincipal(issuerDN);
            LDAPDNPrincipal ioboPrincipal = new LDAPDNPrincipal(issuedOnBehalfOf);
            if (clientPrincipal.equals(issuerPrincipal) || clientPrincipal.equals(holderPrincipal) || clientPrincipal.equals(ioboPrincipal)) return true;
            ParsedToken token1 = tokenParser.decode(ac.getEncoded());
            ParsedToken token2 = createParsedToken(ac, clientDN);
            boolean ret = checkCanBeIssued(token1, token2);
            String s;
            if (ret) s = "Client can issue the AC"; else s = "Client can not issue the AC";
            log.debug("The result of the checking process is: " + s);
            return ret;
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return false;
        }
    }

    /**
     * This function checks whether one issuer can issue a particular certificate.
     *@param token1 is the original certificate (parsedtoken)
     *@param token2 comes from the token1 but the issuer field in token1 is replaced by the requestor (issuer).
     *@return a boolean value. It indicates the relation between the two tokens.
     *We validate the token2. If valid credentials of token2 contains credentials of token1
     *and the set of valid rules of token2 contains the rule of token1 then the
     *requested issuer can issue that certificate, hence, he should be able to revoke the AC.
     */
    private boolean checkCanBeIssued(ParsedToken token1, ParsedToken token2) {
        try {
            Vector holders2 = new Vector();
            holders2.add(token2.getHolder());
            Credentials vCreds2 = allocationPolicy.validate(token2.getHolder(), token2.getIssuerTokenLocator(), token2.getCredentials(), globalAuthTokenRep, holders2);
            log.debug("Validated credentials (with the issuer field is replaced by the requestor) is: " + vCreds2.toString());
            Vector vRules2 = new Vector();
            if (token2 instanceof DelegatableToken) {
                DelegatableToken tokenD = (DelegatableToken) token2;
                AssignmentRule assRule = new AssignmentRule(tokenD.getSubjectDomain(), tokenD.getDepth(), tokenD.getDelegateableCredentials());
                vRules2 = allocationPolicy.validate(tokenD.getHolder(), tokenD.getIssuerTokenLocator(), assRule, globalAuthTokenRep, holders2);
                StringBuffer rarsString = new StringBuffer();
                for (int i = 0; i < vRules2.size(); i++) rarsString.append(((AssignmentRule) vRules2.get(i)).toString()).append("   ");
                log.debug("Validated rules of the token in which the issuer field is replaced by the requestor are: " + rarsString.toString());
            }
            Credentials vCreds1;
            Vector vRules1 = new Vector();
            if (token1 instanceof DelegatableToken) {
                DelegatableToken tokenD = (DelegatableToken) token1;
                vCreds1 = tokenD.getDelegateableCredentials();
                vRules1.add(new AssignmentRule(tokenD.getSubjectDomain(), tokenD.getDepth(), tokenD.getDelegateableCredentials()));
            } else {
                vCreds1 = token1.getCredentials();
            }
            if ((vCreds2.contains(vCreds1)) && contains(vRules2, vRules1)) {
                log.debug("The issuer can issue the AC");
                return true;
            } else {
                log.debug("The issuer can not issue the AC");
                return false;
            }
        } catch (Exception e) {
            log.error("Error: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return false;
        }
    }

    /**
     * This method checks whether the vector of rules contains another vector of rules
     *@ param vRules2 is the first vector
     *@param vRules1 is the second vector
     *@return a boolean value. If the first vector contains the second one then it returns true. Otherwise, false 
     *is return.
     */
    private boolean contains(Vector vRules2, Vector vRules1) {
        for (java.util.Iterator ite = vRules1.iterator(); ite.hasNext(); ) {
            AssignmentRule vRule1 = (AssignmentRule) ite.next();
            for (int i = 0; i < vRules2.size(); i++) {
                AssignmentRule vRule2 = (AssignmentRule) vRules2.get(i);
                if ((vRule2.getCredentials().contains(vRule1.getCredentials())) && (vRule2.getDelegationDepth() >= vRule1.getDelegationDepth())) {
                    ite.remove();
                    break;
                }
            }
        }
        if (vRules1.isEmpty()) return true; else return false;
    }

    /**
     * This function is used for revoking one AC. The requested AC is encoded in Base64 format.
     * The function takes the request from a client (the ACM) and will check whether the 
     * requestor can revoke the requested AC.
     *
     *@param base64AC is the encoded AC in Base64 format
     *@return is a String that tells the client about the result of the request
     */
    protected String revokeACForMe(byte[] ac, String requestor) {
        requestor = requestor.toUpperCase();
        try {
            issrg.ac.AttributeCertificate acc = issrg.ac.AttributeCertificate.guessEncoding(ac);
            log.info("Request for revoking AC comes from user " + requestor + " and the requested AC is " + acc.toString());
            String holderDN = issrg.ac.Util.generalNamesToString(acc.getACInfo().getHolder().getEntityName()).toUpperCase();
            Vector indexes = new Vector();
            log.debug("Going to check and revoke the AC");
            synchronized (ldapUtility) {
                if (canBeIssued(checkRevoke(acc, indexes), requestor) && revoke(holderDN, indexes)) {
                    log.info("The requestor is allowed to revoke the AC and the AC is revoked");
                    return Comm.REVOKE_SUCCESS;
                } else {
                    log.warn("The requestor is not allowed to revoke the AC and the request is rejected.");
                    return Comm.REVOCATION_REJECT;
                }
            }
        } catch (Exception e) {
            log.error("Bad AC: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return Comm.REVOCATION_REJECT;
        }
    }

    /**
     * This function is used for revoking one or more ACs. It uses strings for representing the requested ACs.
     * The client for this function is an Apache.
     *
     *@param requestorDN is the DN of the requestor
     *@param holderDN is the DN of the ACs' holder
     *@param issuerDN is an array of strings that represents the list of issuers of the requested ACs
     *@param serials is an array of strings that represents the list of serialnumbers of the requested ACs
     
     *@return an array of string. Each string reports the revoking result of each AC in the requested list. 
     */
    protected String[] revokeForMe(String requestorDN, String holderDN, String[] issuerDN, String[] serials) {
        if (issuerDN.length == 0) {
            log.warn("There is no revocation request.");
            return null;
        }
        StringBuffer is = new StringBuffer();
        for (int i = 0; i < issuerDN.length; i++) is.append(issuerDN[i]).append(";");
        StringBuffer se = new StringBuffer();
        for (int i = 0; i < serials.length; i++) se.append(serials[i]).append(";");
        log.info(" The requestor is " + requestorDN + ", the holder of ACs is " + holderDN + ", the issuers of the ACs are " + is.toString() + ", and the serial number of the ACs are " + se.toString());
        String[] ret = new String[issuerDN.length];
        int count = 0;
        for (int i = 0; i < issuerDN.length; i++) {
            log.debug("Checking and revoking the AC number :" + i);
            try {
                Vector indexes = new Vector();
                if (issuerDN[i].equals(Comm.BROKEN_AC)) {
                    log.warn("The AC number " + i + " is a broken AC");
                    String idx = serials[i];
                    Integer itg = new Integer(new Integer(idx).intValue() - count);
                    count++;
                    indexes.add(itg);
                    log.debug("Going to revoke the broken AC");
                    revoke(holderDN, indexes);
                    log.info("The broken AC is revoked");
                    ret[i] = Comm.REVOKE_SUCCESS;
                    continue;
                }
                LDAPDNPrincipal iss = new LDAPDNPrincipal(issuerDN[i]);
                LDAPDNPrincipal hol = new LDAPDNPrincipal(holderDN);
                boolean foundSOAAsIssuer = false;
                for (int j = 0; j < soas.size(); j++) if (((LDAPDNPrincipal) soas.get(j)).equals(iss)) {
                    foundSOAAsIssuer = true;
                    break;
                }
                LDAPDNPrincipal dis = new LDAPDNPrincipal(dcfg.DIS);
                if (hol.equals(dis) && foundSOAAsIssuer) {
                    ret[i] = Comm.USE_ACM_TOOL_TO_REVOKE;
                    log.warn("Can not revoke DIS's AC issued by an SOA");
                    continue;
                }
                synchronized (ldapUtility) {
                    AttributeCertificate ac = checkRevoke(holderDN, issuerDN[i], new BigInteger(serials[i]), indexes);
                    if (canBeIssued(ac, requestorDN) && revoke(holderDN, indexes)) {
                        ret[i] = Comm.REVOKE_SUCCESS;
                        log.info("The AC with the following infomation has revoked");
                        log.info(ac.toString());
                        count++;
                    } else {
                        ret[i] = Comm.REVOCATION_REJECT;
                        log.info("Requestor does not have enough privileges to revoke the requested AC. The following AC is not revoked:");
                        log.info(ac.toString());
                    }
                }
            } catch (Exception e) {
                log.error("Revocation is rejected because of error: " + e.getMessage() + "\n" + e.fillInStackTrace());
                ret[i] = Comm.REVOCATION_REJECT;
            }
        }
        return ret;
    }

    private AttributeCertificate checkRevoke(AttributeCertificate ac, Vector indexes) {
        try {
            log.debug("The requested AC is : " + ac.toString());
            String holderDN = issrg.ac.Util.generalNamesToString(ac.getACInfo().getHolder().getEntityName()).toUpperCase();
            String issuerDN = issrg.ac.Util.generalNamesToString(ac.getACInfo().getIssuer().getV1Form() == null ? ac.getACInfo().getIssuer().getV2Form().getIssuerName() : ac.getACInfo().getIssuer().getV1Form()).toUpperCase();
            BigInteger serialRequested = ac.getACInfo().getSerialNumber();
            return checkRepository(holderDN, issuerDN, serialRequested, indexes);
        } catch (Exception e) {
            log.error("Error when checking requested AC: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return null;
        }
    }

    private AttributeCertificate checkRevoke(String holderDN, String issuerDN, BigInteger serial, Vector indexes) {
        log.debug("The requested AC with holderDN is: " + holderDN + " issuerDN is: " + issuerDN + " and serial is: " + serial.toString());
        return checkRepository(holderDN, issuerDN, serial, indexes);
    }

    /**
     * This function fetchs ACs of a holder from a repository an compare with the request (issuer and serial).
     *It returns an AC that matchs the request. Otherwise, null is returned.
     */
    private AttributeCertificate checkRepository(String holderDN, String issuerDN, BigInteger serialRequested, Vector indexes) {
        try {
            log.debug("Going to check whether the holder has the requested AC, holderDN: " + holderDN + " issuerDN: " + issuerDN + " serialnumber: " + serialRequested.toString());
            javax.naming.directory.Attribute attrs = ldapUtility.loadACs(holderDN);
            int num = attrs.size();
            log.debug("The holder " + holderDN + " has " + num + " ACs");
            if (num == 0) return null;
            for (int i = 0; i < num; i++) {
                try {
                    byte[] acObject = (byte[]) attrs.get(i);
                    issrg.ac.AttributeCertificate ac = issrg.ac.AttributeCertificate.guessEncoding(acObject);
                    log.debug("The AC number " + i + " is: " + ac.toString());
                    AttributeCertificateInfo info = ac.getACInfo();
                    String issuer = issrg.ac.Util.generalNamesToString(info.getIssuer().getV1Form() == null ? ac.getACInfo().getIssuer().getV2Form().getIssuerName() : ac.getACInfo().getIssuer().getV1Form()).toUpperCase();
                    BigInteger serial = info.getSerialNumber();
                    if ((issuer.toUpperCase().equals(issuerDN.toUpperCase())) && (serial.compareTo(serialRequested) == 0)) {
                        log.debug("IssuerDN and serial number are matched");
                        indexes.add(new Integer(i));
                        return ac;
                    } else {
                        log.debug("IssuerDN or serialnumber is not matched");
                        continue;
                    }
                } catch (Exception e) {
                    log.error("Error when decoding the AC: " + e.getMessage());
                    continue;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error when checking the AC: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return null;
        }
    }

    /**
     * This function tries to revoke an AC from LDAP
     *@param holderDN is the holder of requested AC
     *@indexes stores the index of ACs that need to be revoked
     */
    private boolean revoke(String holderDN, Vector indexes) {
        try {
            if (indexes.size() > 0) {
                for (int i = 0; i < indexes.size(); i++) {
                    Integer index = (Integer) indexes.get(i);
                    log.debug("Going to revoke the ACs...");
                    ldapUtility.deleteAC(holderDN, index.intValue());
                    log.debug("The ACs are revoked");
                }
                return true;
            } else return true;
        } catch (Exception e) {
            log.error("Error when revoking ACs: " + e.getMessage() + "\n" + e.fillInStackTrace());
            return false;
        }
    }

    /**
     *This class is used for storing credentials and delegation depths
     *before and during the checking/constraining process
     */
    public class DepthsCreds {

        /** 
     * Creates a new instance of Parameters.
     */
        public DepthsCreds() {
        }

        private int depth1;

        private int depth2;

        private issrg.pba.Credentials creds1;

        private issrg.pba.Credentials creds2;

        /**
     *This function sets the delegation depth before the checking/constraining process
     *@param i is the delegation depth going to be set
     */
        public void setDepth1(int i) {
            depth1 = i;
        }

        /**
     *This function sets the delegation depth during the checking/constraining process
     *@param i is the delegation depth going to be set
     */
        public void setDepth2(int i) {
            depth2 = i;
        }

        /**
     *This function sets the credentials before the checking/constraning process
     *@param creds is the credentials going to be set
     */
        public void setCreds1(issrg.pba.Credentials creds) {
            creds1 = creds;
        }

        /**
     *This function sets the credentials during the checking/constraning process
     *@param creds is the credentials going to be set
     */
        public void setCreds2(issrg.pba.Credentials creds) {
            creds2 = creds;
        }

        /**
     *This method returns the delegation depth from the resquest
     *or after constraining with requestor's policy
     *@return an integer, which is the delegation depth
     */
        public int getDepth1() {
            return depth1;
        }

        /**
     *This method returns the delegation depth 
     *after constraining with the DIS's policy
     *@return an integer, which is the delegation depth
     */
        public int getDepth2() {
            return depth2;
        }

        /**
     *This method returns the credentials from the resquest
     *or after constraining with requestor's policy
     *@return a credentials
     */
        public issrg.pba.Credentials getCreds1() {
            return creds1;
        }

        /**
     *This method returns the credentials 
     *after constraining with DIS's policy
     *@return a credentials
     */
        public issrg.pba.Credentials getCreds2() {
            return creds2;
        }

        public String toString() {
            StringBuffer ret = new StringBuffer();
            ret.append("Creds1 is: ");
            if (creds1 == null) ret.append("null "); else ret.append(creds1.toString());
            ret.append(" delegation depth1 is: ");
            ret.append(depth1);
            ret.append(" Creds2 is: ");
            if (creds2 == null) ret.append("null"); else ret.append(creds2.toString());
            ret.append(" delegation depth2 is: ");
            ret.append(depth2);
            return ret.toString();
        }
    }
}
