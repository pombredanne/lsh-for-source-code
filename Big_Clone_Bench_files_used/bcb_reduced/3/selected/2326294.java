package issrg.test.ptb;

import issrg.pba.*;
import issrg.pba.rbac.*;
import issrg.pba.rbac.x509.RepositoryACPolicyFinder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.math.BigInteger;
import java.net.URL;
import java.io.File;

/**
 * This is the class for the Permis Test Bench Program. This application can be used for two purposes:
 * <p>
 * <nl>
 *  <li> It can generate authorisation decisions based on the following inputs:
 *     <ul>
 *     <li> Name of the file containing the information about the identity and attribute
 *       certificates to be used during the decision process
 *     <li> Name of the file containing the set of requests (user, target, action, and arguments)
 *     </ul>
 *  <li> It is able to compare two different authorisation decision files, previously generated,
 *     in order to check whether there are differences between the decision obtained for the
 *     same input request.
 * </nl>
 * @author O Canovas
 * @author O Otenko
 * @version 0.1
 */
public class PermisTestBench {

    protected static java.io.PrintStream out = System.out;

    protected static java.io.PrintStream err = System.err;

    protected String SOA = "";

    protected String oID = "";

    protected PBAAPI pbaApi = null;

    protected PTBClock clock;

    protected PermisAction permisAction;

    protected PermisTarget permisTarget;

    protected String userDN;

    protected String rqNumber;

    protected issrg.pba.rbac.SignatureVerifier sv;

    protected issrg.utils.repository.VirtualRepository vr;

    protected PolicyFinder pfinder;

    protected boolean isXML = false;

    public static void main(String[] args) {
        System.setProperty("line.separator", "\r\n");
        try {
            PermisTestBench bench = new PermisTestBench();
            try {
                if (args.length >= 5) {
                    java.io.PrintStream ps = new java.io.PrintStream(new java.io.FileOutputStream(args[args.length - 1]));
                    System.setOut(ps);
                    System.setErr(ps);
                }
                if (args.length < 5) {
                    System.out.println(args.length);
                    printUsage();
                } else if (args[0].intern() == "-generate") {
                    boolean check;
                    if ((args.length == 6) && (args[4]).intern() == "-ignore-signatures") check = false; else check = true;
                    if (!bench.loadRepositoryFile(args[1], check)) return;
                    if (!bench.initialisePBAAPI()) return;
                    bench.loadRequestsAndGenerateDecisions(args[2], args[3]);
                } else if (args[0].intern() == "-check") {
                    bench.checkDecisionFiles(args[1], args[2], args[3]);
                } else {
                    printUsage();
                }
            } catch (Throwable th) {
                System.out.println("Failed to initialise: " + th.getMessage());
                th.printStackTrace();
            }
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }

    /**
     * Prints how to use this application
     *
     */
    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  1. To generate an output file containing decisions");
        System.out.println("    PermisTestBench -generate <repository_spec_file> <rq_spec_file> <decision_file> [-ignore-signatures] <stdio and stderr redirect_file>");
        System.out.println("  2. To compare two decision files containing decisions");
        System.out.println("    PermisTestBench -check <decision_file1> <decision_file2> <output_diff_file> <stdio and stderr redirect_file>");
    }

    /**
     * Constructs a Permis Test Bench. It has no parameters, and
     * its main function is to initialise the Virtual Repository
     */
    public PermisTestBench() {
        vr = new issrg.utils.repository.VirtualRepository();
        try {
            CustomisePERMIS.setSystemClock("issrg.test.ptb.PTBClock");
            clock = (PTBClock) CustomisePERMIS.getSystemClock();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads attribute certificates in order to insert them into the
     * virtual repository
     *
     * @param vr is the virtual repository
     * @param filename is the name of the file containing the attribute certificate
     * @return true if the certificate was successfully inserted
     */
    protected boolean loadAC(issrg.utils.repository.VirtualRepository vr, String filename) {
        try {
            java.io.InputStream io = new java.io.FileInputStream(filename);
            byte[] ac = new byte[io.available()];
            io.read(ac);
            issrg.ac.AttributeCertificate acd = issrg.ac.AttributeCertificate.guessEncoding(ac);
            vr.populate(issrg.ac.Util.generalNamesToString(acd.getACInfo().getHolder().getEntityName()).toUpperCase(), CustomisePERMIS.getAttributeCertificateAttribute(), ac);
            return true;
        } catch (Throwable th) {
            System.out.println("Failed to load AC from [" + filename + "]");
            th.printStackTrace();
            return false;
        }
    }

    /**
     * Reads the repository specification file. That file must have the
     * following format (the number of entries, NOE, specifies the minimun and maximun number):
     * <p>
     * <ul>
     *   <li> <code>SOA_DN="distinguished name of the SOA"; NOE [1,1]</code>
     *   <li> <code>POLICY_OID="OID of the policy to enforce"; NOE [1,1]</code>
     *   <li> <code>CA="name of the file containing the identity certificate of the CA"; NOE [1,1]</code>
     *   <li> <code>SOA_CERT="name of the file containing the identity certificate of a SOA"; NOE [1,N]</code>
     *   <li> <code>POLICY_AC="name of the file containing the AC including the XML policy"; NOE [1,1]</code>
     *   <li> <code>AC="name of the file containing an AC to be stored"; NOE [0,N]</code>
     *   <li> <code>CURRENT_TIME="YYYY-MM-DD HH:MM:SS"; NOE [0,1]</code>
     * </ul>
     * <p>
     * If a line starts with #, it will be considered as a comment (it is ignored).
     *
     * <p>
     * Moreover, this method also initialises the SignatureVerifier implementation to be used
     * during the decision process.
     *
     * @param filename is the name of the file specifying the repository
     * @param check indicates whether the digital signatures will be verified
     * @return true if the <code>AttributeRepository</code> and the <code>SignatureVerifier</code> were initialised
     */
    public boolean loadRepositoryFile(String filename, boolean check) {
        try {
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(filename));
            String s;
            String varName = null;
            String[] varValue = null;
            PTBSignatureVerifier ptbSV = null;
            if (!check) sv = new SamplePKI(); else {
                sv = new PTBSignatureVerifier();
                ptbSV = (PTBSignatureVerifier) sv;
            }
            while ((varValue = loadVarValue(in)) != null) {
                s = varValue[1];
                varName = varValue[0];
                if (varName == "SOA_DN") {
                    SOA = s;
                } else if (varName == "POLICY_OID") {
                    oID = s;
                } else if (varName == "CA") {
                    if (check) if (!ptbSV.setCACertificate(s)) return false;
                } else if (varName == "SOA_CERT") {
                    if (check) if (!ptbSV.addSOACertificate(s)) return false;
                } else if (varName == "POLICY_AC") {
                    loadAC(vr, s);
                } else if (varName == "CURRENT_TIME") {
                    clock.setTime(s);
                } else if (varName == "AC") {
                    loadAC(vr, s);
                } else if (varName == "POLICY_XML") {
                    loadXML(s);
                } else System.out.println("Unrecognised line; ignored: " + varName + "=" + s);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reads (attribute,value) pairs from a buffered reader. This method
     * processes each line of the buffered reader looking for the <code>PARAMETER=VALUE</code> pattern.
     * Once that pattern is found, it returns an array of Strings containing the name of the
     * parameter in the first element and the value in the second element. On the other hand,
     * when the end of the buffered reader is reached, it returns <code>null</code>.
     *
     * @param in is the buffered reader
     * @return String[2]: <code>String[0]</code> is the name of the parameter;
     * <code>String[1]</code> is the value; <code>null if EOF</code>
     *
     */
    protected String[] loadVarValue(BufferedReader in) {
        String s = "";
        try {
            while (true) {
                s = in.readLine();
                if (s == null) return null;
                if (s.startsWith("#")) continue;
                int i = s.indexOf('=');
                String varName = null;
                if (i >= 0) {
                    varName = s.substring(0, i).intern();
                    s = s.substring(i + 1);
                    String[] result = new String[2];
                    result[0] = varName;
                    result[1] = s;
                    return result;
                } else continue;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Initialises the <code>PBA API</code>, that is, specifies the policy's OID,
     * the distinguished name of the SOA, the attribute repository to be used, and the
     * implementation of the SignatureInterface interface that is responsible for
     * verifying the digital signatures of the attribute certificates
     *
     * @return boolean indicating whether the <code>PBA API</code> was successfully initialised
     */
    public boolean initialisePBAAPI() {
        issrg.utils.repository.AttributeRepository r = vr;
        try {
            CustomisePERMIS.configureX509Flavour();
            if (!isXML) {
                pbaApi = new PermisRBAC(new RepositoryACPolicyFinder(r, oID, new LDAPDNPrincipal(SOA), sv), r, null);
            } else {
                pbaApi = new PermisRBAC(pfinder, r, null);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reads a request contained in the request specification file.
     * Each request must have the following format (the number of entries, NOE, specifies
     * the minimun and maximun number):
     * <p>
     * <ul>
     *   <li> <code>RQ_NUMBER="number of the request being processed"; NOE [1,1]</code>
     *   <li> <code>(USER_DN || USER)="distinguished name of the requestor"; NOE [1,1]</code>
     *   <li> <code>(TARGET_DN || TARGET)="name of the requested resource (DN or URI)"; NOE [1,1]</code>
     *   <li> <code>ACTION="action being requested"; NOE [1,1]</code>
     *   <li> <code>ARG_TYPE="type of the argument"; NOE [0,N]</code>
     *   <li> <code>ARG_VALUE="value of the argument"; NOE [0,N]</code>
     * </ul>
     * <p>
     * If a line starts with #, it will be considered as a comment (it is ignored).
     * <p>
     *
     * Those field must appear in the order above specified.
     * <p>
     *
     * @param in is buffered reader related to the request file
     * @return true if the request is well-formed and it has been successfully read
     */
    protected boolean loadRequest(BufferedReader in) {
        String[] varValue;
        String targetDN = "";
        String action = "";
        String value = "";
        String type = "";
        int argN;
        ArrayList arguments = new ArrayList();
        varValue = loadVarValue(in);
        if (varValue == null) return false;
        if (varValue[0].intern() == "RQ_NUMBER") {
            rqNumber = varValue[1];
        } else return false;
        varValue = loadVarValue(in);
        if (varValue == null) return false;
        if ((varValue[0].intern() == "USER_DN") || (varValue[0].intern() == "USER")) {
            userDN = varValue[1];
        } else return false;
        varValue = loadVarValue(in);
        if (varValue == null) return false;
        if ((varValue[0].intern() == "TARGET_DN") || (varValue[0].intern() == "TARGET")) {
            targetDN = varValue[1];
        } else return false;
        varValue = loadVarValue(in);
        if (varValue == null) return false;
        if (varValue[0].intern() == "ACTION") {
            action = varValue[1];
        } else return false;
        argN = -1;
        do {
            argN++;
            try {
                in.mark(1024);
            } catch (Exception e) {
                return false;
            }
            varValue = loadVarValue(in);
            if (varValue == null) break;
            if (varValue[0].intern() == "RQ_NUMBER") {
                try {
                    in.reset();
                    break;
                } catch (Exception e) {
                    return false;
                }
            }
            if (varValue[0].intern() == "ARG_TYPE") {
                type = varValue[1];
                varValue = loadVarValue(in);
                if (varValue == null) return false;
                if (varValue[0].intern() == "ARG_VALUE") {
                    value = varValue[1];
                } else return false;
            }
            arguments.add(argN, new PermisArgument(type, value));
        } while (true);
        if (argN > 0) {
            PermisArgument[] permisArguments = new PermisArgument[arguments.size()];
            permisArguments = (PermisArgument[]) arguments.toArray(permisArguments);
            permisAction = new PermisAction(action, permisArguments);
        } else permisAction = new PermisAction(action);
        try {
            boolean isAURL;
            try {
                new URL(targetDN);
                isAURL = true;
            } catch (Exception e) {
                isAURL = false;
            }
            if (!isAURL) permisTarget = new PermisTarget(targetDN, null); else permisTarget = new PermisTarget(targetDN);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Writes the decision header. These are the fields filled by
     * this method:
     * <p>
     * <ul>
     *   <li> <code>REQUEST_FILE="name of the file containing the requests"; NOE [1,1]</code>
     *   <li> <code>REQUEST_FILE_HASH="hash value of the request file"; NOE [1,1]</code>
     *   <li> <code>EVALUATION_DATE="date on which the decisions were taken"; NOE [1,1]</code>
     * </ul>
     *
     * @param out is buffered writer related to the decision file
     * @param rqFile the name of the file of the input requests
     * @return true if the decision header was written
     */
    protected boolean writeDecisionHeader(java.io.BufferedWriter out, String rqFile) {
        try {
            out.write("REQUEST_FILE=");
            out.newLine();
            out.write("REQUEST_FILE_HASH=");
            byte[] hash = getRequestHash(rqFile);
            BigInteger bi = new BigInteger(hash);
            bi = bi.abs();
            String s = bi.toString(16);
            out.write(s);
            out.newLine();
            out.write("EVALUATION_DATE=");
            out.newLine();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Writes the decision information related to a particular request.
     * These are the fields filled by this method:
     * <p>
     * <ul>
     *   <li> <code>RQ_NUMBER="number of the request"; NOE [1,1]</code>
     *   <li> <code>RESULT_CODE="0: allowed; 1: not allowed; 2: Invalid input; 3: Run-time error"; NOE [1,1]</code>
     *   <li> <code>RESULT_INFO="code description"; NOE [1,1]</code>
     *   <li> <code>ADDITIONAL_INFO="additional info about exceptions or errors"; NOE [1,1]</code>
     * </ul>
     *
     * @param out is buffered writer related to the decision file
     * @param rqNumber is the number of the request
     * @param code represents the decision code taken by the PDP
     * @param info contains a verbose interpretation of the decision code
     * @param additionalInfo provides data related to exceptions or malformed requests
     * @return true if the decision was written
     */
    protected boolean writeDecisionData(java.io.BufferedWriter out, String rqNumber, String code, String info, String additionalInfo) {
        try {
            out.write("RQ_NUMBER=");
            out.write(rqNumber);
            out.newLine();
            out.write("RESULT_CODE=");
            out.write(code);
            out.newLine();
            out.write("RESULT_INFO=");
            out.write(info);
            out.newLine();
            out.write("ADDITIONAL_INFO=");
            out.write(additionalInfo);
            out.newLine();
            out.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Coordinates the rest of protected methods in order to
     * read all the requests contained in <code>rqFile</code> and to generate the authositaion decisions that will
     * be stored in decisionFile
     *
     * @param rqFile is the name of the file containing the requests
     * @param decisionFile is the name of the file that is going to contain the decisions
     */
    public void loadRequestsAndGenerateDecisions(String rqFile, String decisionFile) {
        try {
            java.io.BufferedWriter out = new java.io.BufferedWriter(new java.io.FileWriter(decisionFile));
            int code;
            String resultInfo;
            String additionalInfo;
            if (!writeDecisionHeader(out, rqFile)) return;
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(rqFile));
            while (loadRequest(in)) {
                resultInfo = "";
                additionalInfo = "";
                try {
                    java.security.Principal user = new LDAPDNPrincipal(userDN);
                    authenticate(user);
                    Subject s = pbaApi.getCreds(user, null);
                    Hashtable env = new Hashtable();
                    env.put(((PermisRBAC) pbaApi).TIME_VARIABLE, clock);
                    if (!pbaApi.decision(s, permisAction, permisTarget, env)) {
                        code = 1;
                        resultInfo = "The action is not allowed";
                    } else {
                        code = 0;
                        resultInfo = "Action succeeded";
                    }
                } catch (PbaException pe) {
                    code = 2;
                    resultInfo = "Invalid input";
                    additionalInfo = pe.getMessage();
                } catch (Throwable th) {
                    code = 3;
                    resultInfo = "Run-time error";
                    additionalInfo = th.getMessage();
                }
                if (!writeDecisionData(out, rqNumber, new Integer(code).toString(), resultInfo, additionalInfo)) break;
            }
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the decision header. These are the fields read by
     * this method:
     * <p>
     * <ul>
     *   <li> <code>REQUEST_FILE="name of the file containing the requests"; NOE [1,1]</code>
     *   <li> <code>REQUEST_FILE_HASH="hash value of the request file"; NOE [1,1]</code>
     *   <li> <code>EVALUATION_DATE="date on which the decisions were taken"; NOE [1,1]</code>
     * </ul>
     *
     * @param in is buffered reader related to the decision file
     * @return String[3] containing: [0] name of the request file, [1] hash value, [2] date; null if EOF
     */
    protected String[] loadDecisionHeader(java.io.BufferedReader in) {
        String[] varValue;
        String requestFile = "";
        String requestHash = "";
        String evaluationDate = "";
        varValue = loadVarValue(in);
        if (varValue == null) return null;
        if (varValue[0].intern() == "REQUEST_FILE") {
            requestFile = varValue[1];
        }
        varValue = loadVarValue(in);
        if (varValue == null) return null;
        if (varValue[0].intern() == "REQUEST_FILE_HASH") {
            requestHash = varValue[1];
        }
        varValue = loadVarValue(in);
        if (varValue == null) return null;
        if (varValue[0].intern() == "EVALUATION_DATE") {
            evaluationDate = varValue[1];
        }
        String[] result = new String[3];
        result[0] = requestFile;
        result[1] = requestHash;
        result[2] = evaluationDate;
        return result;
    }

    /**
     * Reads the information related to a decision contained in a decision file.
     * These are the fields read by this method:
     * <p>
     * <ul>
     *   <li> <code>RQ_NUMBER="number of the request"; NOE [1,1]</code>
     *   <li> <code>RESULT_CODE="0: allowed; 1: not allowed; 2: Invalid input; 3: Run-time error"; NOE [1,1]</code>
     *   <li> <code>RESULT_INFO="code description"; NOE [1,1]</code>
     *   <li> <code>ADDITIONAL_INFO="additional info about exceptions or errors"; NOE [1,1]</code>
     * </ul>
     *
     * @param in is buffered reader related to the decision file
     * @return String[4]: [0] request number; [1] result code; [2] info; [3] additional info; null if EOF
     */
    protected String[] loadDecision(BufferedReader in) {
        String[] varValue;
        String rqNumber = "";
        String resultCode = "";
        String resultInfo = "";
        String additionalInfo = "";
        varValue = loadVarValue(in);
        if (varValue == null) return null;
        if (varValue[0].intern() == "RQ_NUMBER") {
            rqNumber = varValue[1];
        } else return null;
        varValue = loadVarValue(in);
        if (varValue == null) return null;
        if (varValue[0].intern() == "RESULT_CODE") {
            resultCode = varValue[1];
        } else return null;
        varValue = loadVarValue(in);
        if (varValue == null) return null;
        if (varValue[0].intern() == "RESULT_INFO") {
            resultInfo = varValue[1];
        } else return null;
        varValue = loadVarValue(in);
        if (varValue == null) return null;
        if (varValue[0].intern() == "ADDITIONAL_INFO") {
            additionalInfo = varValue[1];
        }
        String[] result = new String[4];
        result[0] = rqNumber;
        result[1] = resultCode;
        result[2] = resultInfo;
        result[3] = additionalInfo;
        return result;
    }

    /**
     * Compares two decision files. As a result, a diff file is generated
     * according to the following format:
     * <p>
     * HEADER: (see writeDiffHeader)
     * <p>
     * FOR EACH DECISION:
     * <p>
     * <ul>
     *   <li> <code>[Checking request "number of request"]</code>
     *   <li> <code>  [(OK) Decision codes are equal || (WN) Decision codes differ "code1" VS "code2"]</code>
     *   <li> <code>  [(OK) Messages are the same || (WN) Messages Differ]</code>
     *   <li> <code>  [(OK) No additional information provided || (OK) Additional notes are the same || (WN) Notes differ]</code>
     * </ul>
     *
     * @param f1 is one of the decision files
     * @param f2 is the other decision file
     * @param diff is the name of the file which is going to contain the differences
     */
    public void checkDecisionFiles(String f1, String f2, String diff) {
        try {
            java.io.BufferedReader in1 = new java.io.BufferedReader(new java.io.FileReader(f1));
            java.io.BufferedReader in2 = new java.io.BufferedReader(new java.io.FileReader(f2));
            java.io.BufferedWriter out = new java.io.BufferedWriter(new java.io.FileWriter(diff));
            String[] header1 = loadDecisionHeader(in1);
            String[] header2 = loadDecisionHeader(in2);
            if (header1 == null || header2 == null) return;
            String[] decision1 = null;
            String[] decision2 = null;
            decision1 = loadDecision(in1);
            decision2 = loadDecision(in2);
            while ((decision1 != null) && (decision2 != null)) {
                if (decision1[0].intern() == decision2[0].intern()) {
                    out.write("[Checking request " + decision1[0] + "]");
                    out.newLine();
                    if (decision1[1].intern() == decision2[1].intern()) {
                        out.write("  (OK) Decision codes are equal.");
                        out.newLine();
                    } else {
                        out.write("  (WN) Decision codes differ (" + decision1[1] + " VS " + decision2[1] + ")");
                        out.newLine();
                    }
                    if (decision1[2].intern() == decision2[2].intern()) {
                        out.write("  (OK) Messages are the same.");
                        out.newLine();
                    } else {
                        out.write("  (WN) Messages differ:");
                        out.newLine();
                        out.write("    1. " + decision1[2]);
                        out.newLine();
                        out.write("    2. " + decision2[2]);
                        out.newLine();
                    }
                    if ((decision1[3].intern() == "") && (decision2[3].intern() == "")) {
                        out.write("  (OK) No additional information provided.");
                        out.newLine();
                    } else if (decision1[3].intern() == decision2[3].intern()) {
                        out.write("  (OK) Additional notes are the same.");
                        out.newLine();
                    } else {
                        out.write("  (WN) Additional notes differ:");
                        out.newLine();
                        out.write("    1. " + decision1[3]);
                        out.newLine();
                        out.write("    2. " + decision2[3]);
                        out.newLine();
                    }
                } else break;
                decision1 = loadDecision(in1);
                decision2 = loadDecision(in2);
            }
            if ((decision1 != null) && (decision2 != null)) {
                out.write("**ERROR**: Check the sequences of request numbers in both files, they differ.");
                out.newLine();
            } else if ((decision1 != null) || (decision2 != null)) {
                if (decision1 == null) {
                    out.write("**WARNING**: The first file is shorter than the second one");
                    out.newLine();
                } else {
                    out.write("**WARNING**: The second file is shorter than the first one");
                    out.newLine();
                }
            }
            in1.close();
            in2.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtains the hash value of a request file
     *
     * @param rqFile is name of the request file
     * @return byte[] containing the SHA-1 hash of the file
     */
    protected byte[] getRequestHash(String rqFile) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            FileInputStream fis = new FileInputStream(rqFile);
            byte[] content = new byte[fis.available()];
            int nb = fis.read(content);
            while (nb != -1) {
                md.update(content, 0, nb);
                nb = fis.read(content);
            }
            byte[] result = md.digest();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads the decision header. These are the fields read by
     * this method:
     * <p>
     * <ul>
     *   <li> <code>REQUEST_FILE="name of the file containing the requests"; NOE [1,1]</code>
     *   <li> <code>REQUEST_FILE_HASH="hash value of the request file"; NOE [1,1]</code>
     *   <li> <code>EVALUATION_DATE="date on which the decisions were taken"; NOE [1,1]</code>
     * </ul>
     *
     * @param out is buffered writer related to the diff file
     * @param h1 contains the 3 fields of the header included in the first decision file
     * @param h2 contains the 3 fields of the header included in the second decision file
     * @param f1 is the name of the first decision file
     * @param f2 is the name of the second decision file
     * @return true if the diff header was successfully written
     */
    protected boolean writeDiffHeader(java.io.BufferedWriter out, String[] h1, String[] h2, String f1, String f2) {
        try {
            out.write("CHECKING " + f1 + " against " + f2);
            out.newLine();
            if (h1[1].intern() == h2[1].intern()) {
                out.write("Both files are related to the same requests, i.e. same hash: <" + h1[1] + ">");
                out.newLine();
            } else {
                out.write("WARNING: Decisions correspond to different request files");
                out.newLine();
                out.write("  Hash of File 1 <" + h1[1] + ">");
                out.newLine();
                out.write("  Hash of File 2 <" + h2[1] + ">");
                out.newLine();
            }
            java.util.Date date1 = new java.text.SimpleDateFormat().parse(h1[2]);
            java.util.Date date2 = new java.text.SimpleDateFormat().parse(h2[2]);
            if (date1.before(date2)) {
                out.write("File " + f2 + " is newer than file " + f1);
                out.newLine();
            } else {
                out.write("File " + f1 + " is newer than file " + f2);
                out.newLine();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void loadXML(String filename) {
        try {
            try {
                if (filename.endsWith(".xml") || filename.endsWith(".XML")) {
                    isXML = true;
                    pfinder = new issrg.simplePERMIS.SimplePERMISPolicyFinder(new File(filename));
                } else {
                    throw new Exception("Failed to load XML Policy from [" + filename + "]");
                }
            } catch (java.io.FileNotFoundException e) {
                System.err.println(e.getMessage());
            }
        } catch (Throwable th) {
            System.err.println("Failed to load XML from [" + filename + "]");
            th.printStackTrace();
        }
    }

    protected void authenticate(java.security.Principal user) {
    }
}
