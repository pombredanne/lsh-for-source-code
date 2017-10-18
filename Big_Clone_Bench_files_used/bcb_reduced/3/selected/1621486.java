package demo.pkcs.pkcs11.wrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import iaik.pkcs.pkcs11.wrapper.CK_ATTRIBUTE;
import iaik.pkcs.pkcs11.wrapper.CK_INFO;
import iaik.pkcs.pkcs11.wrapper.CK_MECHANISM;
import iaik.pkcs.pkcs11.wrapper.CK_MECHANISM_INFO;
import iaik.pkcs.pkcs11.wrapper.CK_SESSION_INFO;
import iaik.pkcs.pkcs11.wrapper.CK_SLOT_INFO;
import iaik.pkcs.pkcs11.wrapper.CK_TOKEN_INFO;
import iaik.pkcs.pkcs11.wrapper.Functions;
import iaik.pkcs.pkcs11.wrapper.PKCS11;
import iaik.pkcs.pkcs11.wrapper.PKCS11Connector;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;

/**
 * This is a simple class for testing the implementation.
 * Notice that this is an sample that may not run as is with many tokens.
 * It may be required to exclude some test methods in the main method.
 */
public class SimpleTest {

    protected static final String CERTIFICATE_FILE = "tokenCertificate.der";

    protected static final String SIGNATURE_FILE = "signature.bin";

    protected static final String DIGEST_FILE = "digest.dat";

    protected PKCS11 myPKCS11Module_;

    protected String userPin_;

    protected long token_ = -1L;

    protected long session_;

    protected long[] objects_;

    protected long signatureKeyHandle_;

    protected long certificateHandle_;

    protected byte[] derEncodedCertificate_;

    protected File file_;

    protected CK_MECHANISM signatureMechanism_;

    protected CK_MECHANISM digestMechanism_;

    protected MessageDigest messageDigest_;

    protected byte[] signature_;

    protected byte[] digest_;

    public SimpleTest(String pkcs11Module, String userPin, File file) throws IOException, PKCS11Exception {
        System.out.print("trying to connect to PKCS#11 module: " + pkcs11Module);
        myPKCS11Module_ = PKCS11Connector.connectToPKCS11Module(pkcs11Module);
        userPin_ = userPin;
        file_ = file;
        signatureMechanism_ = new CK_MECHANISM();
        signatureMechanism_.mechanism = PKCS11Constants.CKM_SHA1_RSA_PKCS;
        signatureMechanism_.pParameter = null;
        digestMechanism_ = new CK_MECHANISM();
        digestMechanism_.mechanism = PKCS11Constants.CKM_SHA_1;
        digestMechanism_.pParameter = null;
        System.out.println(" FINISHED");
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            printUsage();
            System.exit(1);
        }
        try {
            SimpleTest test = new SimpleTest(args[0], args[1], new File(args[2]));
            test.initialize();
            test.getInfo();
            test.getSlotInfo();
            test.getTokenInfo();
            test.getMechanismInfo();
            test.openROSession();
            test.getSessionInfo();
            test.findAllObjects();
            test.printAllObjects();
            test.loginUser();
            test.getSessionInfo();
            test.findAllObjects();
            test.printAllObjects();
            test.findSignatureKey();
            test.findCertificate();
            test.readCertificate();
            test.writeCertificateToFile();
            test.signData();
            test.writeSignatureToFile();
            test.digestData();
            test.writeDigestToFile();
            test.logout();
            test.closeSession();
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
    }

    public static void printUsage() {
        System.out.println("Usage: SimepleTest <PKCS#11 module> <userPIN> <file to be signed>");
        System.out.println(" e.g.: SimpleTest pk2priv.dll password data.dat");
        System.out.println("The given DLL must be in the search path of the system.");
    }

    public void initialize() throws PKCS11Exception {
        System.out.print("initializing... ");
        myPKCS11Module_.C_Initialize(null);
        System.out.println("FINISHED\n");
    }

    public void getInfo() throws PKCS11Exception {
        System.out.println("getting info");
        CK_INFO moduleInfo = myPKCS11Module_.C_GetInfo();
        System.out.println("Module Info: ");
        System.out.println(moduleInfo);
        System.out.println("FINISHED\n");
    }

    public void getSlotInfo() throws PKCS11Exception {
        System.out.println("getting slot list");
        long[] slotIDs = myPKCS11Module_.C_GetSlotList(false);
        CK_SLOT_INFO slotInfo;
        for (int i = 0; i < slotIDs.length; i++) {
            System.out.println("Slot Info: ");
            slotInfo = myPKCS11Module_.C_GetSlotInfo(slotIDs[i]);
            System.out.println(slotInfo);
        }
        System.out.println("FINISHED\n");
    }

    public void getTokenInfo() throws PKCS11Exception {
        System.out.println("getting token list");
        long[] tokenIDs = myPKCS11Module_.C_GetSlotList(true);
        CK_TOKEN_INFO tokenInfo;
        for (int i = 0; i < tokenIDs.length; i++) {
            System.out.println("Token Info: ");
            tokenInfo = myPKCS11Module_.C_GetTokenInfo(tokenIDs[i]);
            System.out.println(tokenInfo);
            if (token_ == -1L) {
                token_ = tokenIDs[i];
            }
        }
        System.out.println("FINISHED\n");
    }

    public void getMechanismInfo() throws PKCS11Exception {
        CK_MECHANISM_INFO mechanismInfo;
        System.out.println("getting mechanism list");
        System.out.println("getting slot list");
        long[] slotIDs = myPKCS11Module_.C_GetSlotList(true);
        for (int i = 0; i < slotIDs.length; i++) {
            System.out.println("getting mechanism list for slot " + slotIDs[i]);
            long[] mechanismIDs = myPKCS11Module_.C_GetMechanismList(slotIDs[i]);
            for (int j = 0; j < mechanismIDs.length; j++) {
                System.out.println("mechanism info for mechanism " + Functions.mechanismCodeToString(mechanismIDs[j]) + ": ");
                mechanismInfo = myPKCS11Module_.C_GetMechanismInfo(slotIDs[i], mechanismIDs[j]);
                System.out.println(mechanismInfo);
            }
        }
        System.out.println("FINISHED\n");
    }

    public void initToken() throws PKCS11Exception {
        String label = "The Label!                      ";
        String pin = "password";
        System.out.println("init token");
        long[] slotIDs = myPKCS11Module_.C_GetSlotList(false);
        myPKCS11Module_.C_InitToken(slotIDs[0], pin.toCharArray(), label.toCharArray());
        System.out.println("FINISHED");
    }

    public void openROSession() throws PKCS11Exception {
        System.out.println("open RO session");
        session_ = myPKCS11Module_.C_OpenSession(token_, PKCS11Constants.CKF_SERIAL_SESSION, null, null);
        System.out.println("FINISHED\n");
    }

    public void getSessionInfo() throws PKCS11Exception {
        System.out.println("get session info");
        CK_SESSION_INFO sessionInfo;
        System.out.println("Session Info: ");
        sessionInfo = myPKCS11Module_.C_GetSessionInfo(session_);
        System.out.println(sessionInfo);
        System.out.println("FINISHED\n");
    }

    public void findAllObjects() throws PKCS11Exception {
        System.out.println("find all objects");
        myPKCS11Module_.C_FindObjectsInit(session_, null);
        objects_ = myPKCS11Module_.C_FindObjects(session_, 100);
        if (objects_ == null) {
            System.out.println("null returned - no objects found");
        } else {
            System.out.println("found " + objects_.length + " objects");
        }
        myPKCS11Module_.C_FindObjectsFinal(session_);
        System.out.println("FINISHED\n");
    }

    public void printAllObjects() throws PKCS11Exception {
        System.out.println("print all objects");
        for (int i = 0; i < objects_.length; i++) {
            System.out.println("object No. " + i);
            CK_ATTRIBUTE[] template = new CK_ATTRIBUTE[1];
            template[0] = new CK_ATTRIBUTE();
            template[0].type = PKCS11Constants.CKA_CLASS;
            myPKCS11Module_.C_GetAttributeValue(session_, objects_[i], template);
            System.out.println("CKA_CLASS: " + Functions.classTypeToString(((Long) template[0].pValue).longValue()));
        }
        System.out.println("FINISHED\n");
    }

    public void loginUser() throws PKCS11Exception {
        System.out.println("login user to session with password \"" + userPin_ + "\"");
        myPKCS11Module_.C_Login(session_, PKCS11Constants.CKU_USER, userPin_.toCharArray());
        System.out.println("FINISHED\n");
    }

    public void findSignatureKey() throws PKCS11Exception {
        System.out.println("find signature key");
        CK_ATTRIBUTE[] attributeTemplateList = new CK_ATTRIBUTE[2];
        attributeTemplateList[0] = new CK_ATTRIBUTE();
        attributeTemplateList[0].type = PKCS11Constants.CKA_CLASS;
        attributeTemplateList[0].pValue = new Long(PKCS11Constants.CKO_PRIVATE_KEY);
        attributeTemplateList[1] = new CK_ATTRIBUTE();
        attributeTemplateList[1].type = PKCS11Constants.CKA_SIGN;
        attributeTemplateList[1].pValue = new Boolean(PKCS11Constants.TRUE);
        myPKCS11Module_.C_FindObjectsInit(session_, attributeTemplateList);
        long[] availableSignatureKeys = myPKCS11Module_.C_FindObjects(session_, 100);
        if (availableSignatureKeys == null) {
            System.out.println("null returned - no signature key found");
        } else {
            System.out.println("found " + availableSignatureKeys.length + " signature keys");
            for (int i = 0; i < availableSignatureKeys.length; i++) {
                if (i == 0) {
                    signatureKeyHandle_ = availableSignatureKeys[i];
                    System.out.print("for signing we use ");
                }
                System.out.println("signature key " + i);
            }
        }
        myPKCS11Module_.C_FindObjectsFinal(session_);
        System.out.println("FINISHED\n");
    }

    public void findCertificate() throws PKCS11Exception {
        System.out.println("find certificate");
        CK_ATTRIBUTE[] attributeTemplateList = new CK_ATTRIBUTE[1];
        attributeTemplateList[0] = new CK_ATTRIBUTE();
        attributeTemplateList[0].type = PKCS11Constants.CKA_ID;
        myPKCS11Module_.C_GetAttributeValue(session_, signatureKeyHandle_, attributeTemplateList);
        byte[] keyAndCertificateID = (byte[]) attributeTemplateList[0].pValue;
        System.out.println("ID of siganture key: " + Functions.toHexString(keyAndCertificateID));
        attributeTemplateList = new CK_ATTRIBUTE[2];
        attributeTemplateList[0] = new CK_ATTRIBUTE();
        attributeTemplateList[0].type = PKCS11Constants.CKA_CLASS;
        attributeTemplateList[0].pValue = new Long(PKCS11Constants.CKO_CERTIFICATE);
        attributeTemplateList[1] = new CK_ATTRIBUTE();
        attributeTemplateList[1].type = PKCS11Constants.CKA_ID;
        attributeTemplateList[1].pValue = keyAndCertificateID;
        myPKCS11Module_.C_FindObjectsInit(session_, attributeTemplateList);
        long[] availableCertificates = myPKCS11Module_.C_FindObjects(session_, 100);
        if (availableCertificates == null) {
            System.out.println("null returned - no certificate found");
        } else {
            System.out.println("found " + availableCertificates.length + " certificates with matching ID");
            for (int i = 0; i < availableCertificates.length; i++) {
                if (i == 0) {
                    certificateHandle_ = availableCertificates[i];
                    System.out.print("for verification we use ");
                }
                System.out.println("certificate " + i);
            }
        }
        myPKCS11Module_.C_FindObjectsFinal(session_);
        System.out.println("FINISHED\n");
    }

    public void readCertificate() throws PKCS11Exception {
        System.out.println("read certificate");
        CK_ATTRIBUTE[] template = new CK_ATTRIBUTE[1];
        template[0] = new CK_ATTRIBUTE();
        template[0].type = PKCS11Constants.CKA_VALUE;
        myPKCS11Module_.C_GetAttributeValue(session_, certificateHandle_, template);
        derEncodedCertificate_ = (byte[]) template[0].pValue;
        System.out.println("DER encoded certificate (" + derEncodedCertificate_.length + " bytes):");
        System.out.println(Functions.toHexString(derEncodedCertificate_));
        System.out.println("FINISHED\n");
    }

    public void writeCertificateToFile() throws IOException, PKCS11Exception {
        System.out.println("write certificate to file: " + CERTIFICATE_FILE);
        FileOutputStream fos = new FileOutputStream(CERTIFICATE_FILE);
        fos.write(derEncodedCertificate_);
        fos.flush();
        fos.close();
        System.out.println("FINISHED\n");
    }

    public void signData() throws IOException, PKCS11Exception {
        byte[] buffer = new byte[1024];
        byte[] helpBuffer;
        int bytesRead;
        InputStream dataInput = new FileInputStream(file_);
        myPKCS11Module_.C_SignInit(session_, signatureMechanism_, signatureKeyHandle_);
        while ((bytesRead = dataInput.read(buffer, 0, buffer.length)) >= 0) {
            helpBuffer = new byte[bytesRead];
            System.arraycopy(buffer, 0, helpBuffer, 0, bytesRead);
            myPKCS11Module_.C_SignUpdate(session_, helpBuffer);
            Arrays.fill(helpBuffer, (byte) 0);
        }
        Arrays.fill(buffer, (byte) 0);
        signature_ = myPKCS11Module_.C_SignFinal(session_);
    }

    public void writeSignatureToFile() throws IOException, PKCS11Exception {
        System.out.println("write signature to file: " + SIGNATURE_FILE);
        FileOutputStream fos = new FileOutputStream(SIGNATURE_FILE);
        fos.write(signature_);
        fos.flush();
        fos.close();
        System.out.println("FINISHED");
    }

    public void digestData() throws IOException, PKCS11Exception {
        byte[] buffer = new byte[1024];
        byte[] helpBuffer, testDigest;
        int bytesRead;
        System.out.println("Digest Data");
        myPKCS11Module_.C_DigestInit(session_, digestMechanism_);
        try {
            messageDigest_ = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            System.out.println(e);
        }
        InputStream dataInput = new FileInputStream(file_);
        while ((bytesRead = dataInput.read(buffer, 0, buffer.length)) >= 0) {
            helpBuffer = new byte[bytesRead];
            System.arraycopy(buffer, 0, helpBuffer, 0, bytesRead);
            myPKCS11Module_.C_DigestUpdate(session_, helpBuffer);
            messageDigest_.update(helpBuffer);
            Arrays.fill(helpBuffer, (byte) 0);
        }
        Arrays.fill(buffer, (byte) 0);
        digest_ = myPKCS11Module_.C_DigestFinal(session_);
        testDigest = messageDigest_.digest();
        System.out.println("PKCS11digest:" + Functions.toHexString(digest_));
        System.out.println("TestDigest  :" + Functions.toHexString(testDigest));
        System.out.println("FINISHED\n");
    }

    public void writeDigestToFile() throws IOException, PKCS11Exception {
        System.out.println("write digest to file: " + DIGEST_FILE);
        FileOutputStream fos = new FileOutputStream(DIGEST_FILE);
        fos.write(digest_);
        fos.flush();
        fos.close();
        System.out.println("FINISHED\n");
    }

    public void logout() throws PKCS11Exception {
        System.out.println("logout session");
        myPKCS11Module_.C_Logout(session_);
        System.out.println("FINISHED\n");
    }

    public void closeSession() throws PKCS11Exception {
        System.out.println("close session");
        myPKCS11Module_.C_CloseSession(session_);
        System.out.println("FINISHED\n");
    }
}
