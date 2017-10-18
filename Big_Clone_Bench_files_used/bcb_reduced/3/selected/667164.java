package issrg.SAWS;

import issrg.SAWS.util.SAWSLogWriter;
import java.util.*;
import java.io.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.security.*;
import javax.crypto.*;
import java.security.AlgorithmParameters;
import javax.crypto.spec.*;
import javax.security.auth.callback.*;
import javax.swing.*;
import issrg.SAWS.callback.SAWSGUICallbackHandler;
import issrg.SAWS.callback.SAWSTextOutputCallback;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author W.Xu
 */
public class LogFileWriter {

    public int currentRecordWriteCount = -1;

    public byte[] accumulatedHash = null;

    public byte[] finalSignature = null;

    private String currentLogFilename = null;

    private int currentLogSequence = -1;

    private File CurrentLogFile = null;

    private java.security.MessageDigest accMD = null;

    private RandomAccessFile rafCurrentLogFile = null;

    private String logFileRoot;

    private byte[] secureRandomBytes = new byte[20];

    private SecretKey symmetricKeyInLog = null;

    private byte[] baSigningPublicKeyCert = null;

    private int thisRecordLength = 0;

    private int lastRecordLength = 0;

    private PublicKey sawsEncryptionPublicKey = null;

    private PrivateKey sawsEncryptionPrivateKey = null;

    private PublicKey sawsSigningPublicKey = null;

    private PrivateKey sawsSigningPrivateKey = null;

    private PublicKey vtEncryptionPublicKey = null;

    private PublicKey rootCAPublicKey = null;

    private Map UserIDPKMap = null;

    private String hashAlgorithmName = "SHA1";

    private String signingAlgorithm = null;

    private byte[] lastAccumulatedHash = null;

    private int debugLevel = 0;

    private CallbackHandler callbackHandler = new SAWSGUICallbackHandler();

    private static SAWSLogWriter sawsDebugLog = new SAWSLogWriter(LogFileWriter.class.getName());

    /**
   * This method is the constructor of LogFileWriter.
   *
   */
    public LogFileWriter() {
    }

    /**
   * Method that sets the debug level.
   *
   * @param debugLevel indicates the different level of debug output information by SAWS.
   * Its value is from 0 to 5. When its value is 0, then no debug information is output by SAWS.
   * When its value is 5, then most debug information is output by SAWS.
   */
    public void setDebugLevel(int debugLevel) {
        this.debugLevel = debugLevel;
    }

    /**
   * This method is the constructor of LogFileWriter.
   *
   * @param root The log root, the place where the log files are stored.
   * @param cLogFilename The log file name to be written.
   * @param vt The SAWS VT public key.
   * @param sawsEncPK The SAWS encryption public key.
   * @param sawsSignPIK The SAWS signing private key.
   * @param ba The binary array of the SAWS signing public key certificate to be written into
   * the log file.
   * @param hashAlgorithm The hash algorithm name, e.g. MD5.
   * @param secureBytes The secure random number used for secure hashing.
   * @param UserIDPKMap A map with the user's IDs and the respective public key.
   * @param signingAlgorithm The signing algorithm name, e.g. MD5withRSA.
   */
    public LogFileWriter(String root, String cLogFilename, PublicKey vt, PublicKey sawsEncPK, PrivateKey sawsSignPIK, byte[] ba, String hashAlgorithm, byte[] secureBytes, Map UserIDPKMap, String signingAlgorithm) {
        logFileRoot = root;
        currentLogFilename = cLogFilename;
        vtEncryptionPublicKey = vt;
        sawsEncryptionPublicKey = sawsEncPK;
        sawsSigningPrivateKey = sawsSignPIK;
        baSigningPublicKeyCert = ba;
        this.hashAlgorithmName = hashAlgorithm;
        this.signingAlgorithm = signingAlgorithm;
        if (secureBytes != null) secureRandomBytes = secureBytes;
        try {
            accMD = java.security.MessageDigest.getInstance(this.hashAlgorithmName);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.UserIDPKMap = UserIDPKMap;
    }

    /**
      * This method is the constructor of LogFileWriter.
      *
      * @param root The log root, the place where the log files are stored.
      * @param cLogFilename The log file name to be written.
      * @param vt The SAWS VT public key.
      * @param sawsEncPK The SAWS encryption public key.
      * @param sawsSignPIK The SAWS signing private key.
      * @param ba The binary array of the SAWS signing public key certificate to be written into
      * the log file.
      * @param hashAlgorithm The hash algorithm name, e.g. MD5.
      * @param secureBytes The secure random number used for secure hashing.
      * @param UserIDPKMap A map with the user's IDs and the respective public key.
      * @param signingAlgorithm The signing algorithm name, e.g. MD5withRSA.
      * @param ch The callback hadler.
      */
    public LogFileWriter(String root, String cLogFilename, PublicKey vt, PublicKey sawsEncPK, PrivateKey sawsSignPIK, byte[] ba, String hashAlgorithm, byte[] secureBytes, Map UserIDPKMap, String signingAlgorithm, CallbackHandler ch) {
        this(root, cLogFilename, vt, sawsEncPK, sawsSignPIK, ba, hashAlgorithm, secureBytes, UserIDPKMap, signingAlgorithm);
        this.callbackHandler = ch;
    }

    /**
     * Method that sets the callback handler for the class. If the handler
     * is null, the class will keep using the default callback handler.
     *
     * @param ch The callback handler.
     */
    public void setCallbackHandler(CallbackHandler ch) {
        if (ch != null) {
            this.callbackHandler = ch;
        }
    }

    /**
     * This method is to set the hashing algorithm name. SHA1 is the default algorithm.
     * @param hashAlgorithm The hash algorithm name. Please consult the user guide for the supported algorithms.
     */
    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithmName = hashAlgorithm;
    }

    /**
     * This method returns the disgest algorithm name.
     * @return The digest algorithm name.
     */
    public String getHashAlgorithm() {
        return this.hashAlgorithmName;
    }

    /**
   * This method is to get the final signature.
   *
   * @return The final signature.
   */
    public byte[] getSignature() {
        return finalSignature;
    }

    /**
   * This method is to get the final accumualted hash.
   *
   * @return The accumulated hash.
   */
    public byte[] getAccHash() {
        return accumulatedHash;
    }

    /**
   * This method is to get the current writing record number in the current log file.
   *
   * @return The current record write count.
   */
    public int getCurrentRecordWriteCount() {
        return currentRecordWriteCount;
    }

    /**
   * This method creates a new log file, prepares this file for adding new log records to it in the future.
   *
   * @return 0 if success or -1 if fails.
   */
    public int prepareNewLog() {
        try {
            accMD.reset();
            accMD.update(currentLogFilename.getBytes());
        } catch (Exception e) {
            this.showMessage("Updating accumulated hash error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write(e.toString());
            }
            return -1;
        }
        if (debugLevel > SAWSConstant.NoInfo) {
            sawsDebugLog.write("Now creating a new log.");
        }
        currentRecordWriteCount = -1;
        CurrentLogFile = new File(logFileRoot, currentLogFilename);
        try {
            rafCurrentLogFile = new RandomAccessFile(CurrentLogFile, "rw");
        } catch (Exception e) {
            this.showMessage("Open file " + currentLogFilename + " error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write(e.toString());
            }
            return -1;
        }
        SecureRandom random = null;
        try {
            random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            random.nextBytes(secureRandomBytes);
        } catch (Exception e1) {
            this.showMessage("Generating random number error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write(e1.toString());
            }
            return -1;
        }
        try {
            this.createSAWSRecord(this.hashAlgorithmName.getBytes(), SAWSConstant.SAWSHashAlgorithmType, SAWSConstant.NoEncryptionFlag);
            KeyGenerator kgen = null;
            try {
                Security.addProvider(new BouncyCastleProvider());
                kgen = KeyGenerator.getInstance("AES", "BC");
            } catch (Exception ex) {
                kgen = KeyGenerator.getInstance("AES");
            }
            sawsDebugLog.write("Provider: " + kgen.getProvider().getName());
            kgen.init(128);
            symmetricKeyInLog = kgen.generateKey();
            byte[] rawKey = symmetricKeyInLog.getEncoded();
            createSAWSRecord(secureRandomBytes, SAWSConstant.SAWSSecretRandomNumberType, SAWSConstant.AsymmetricEncryptionFlag);
            createSAWSRecord(rawKey, SAWSConstant.SymmetricEncryptionKeyType, SAWSConstant.AsymmetricEncryptionFlag);
            createSAWSRecord(rawKey, SAWSConstant.SymmetricEncryptionKeyType, (byte) SAWSConstant.USERVT, SAWSConstant.AsymmetricEncryptionFlag, vtEncryptionPublicKey);
            Iterator idpkIt = UserIDPKMap.entrySet().iterator();
            while (idpkIt.hasNext()) {
                Map.Entry userIDPK = (Map.Entry) idpkIt.next();
                byte uID = ((Byte) userIDPK.getKey()).byteValue();
                PublicKey pk = (PublicKey) userIDPK.getValue();
                createSAWSRecord(rawKey, SAWSConstant.SymmetricEncryptionKeyType, (byte) uID, SAWSConstant.AsymmetricEncryptionFlag, pk);
            }
            createSAWSRecord(baSigningPublicKeyCert, SAWSConstant.SAWSCertificateType, SAWSConstant.NoEncryptionFlag);
            createSAWSRecord(this.signingAlgorithm.getBytes(), SAWSConstant.SAWSSigningAlgorithmType, SAWSConstant.NoEncryptionFlag);
            writeSignatureRecord(SAWSConstant.SAWSHeaderSignatureType);
        } catch (Exception e3) {
            this.showMessage("Prepare new log error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write(e3.toString());
            }
            return -1;
        }
        return 0;
    }

    /**
   * This method is to write the log record into the log file.
   *
   * @param logRecord record to be written.  
   * 
   */
    private void writeLogRecord(byte[] logRecord) {
        try {
            rafCurrentLogFile.write(logRecord);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write("Log file written. \nFile name: " + this.CurrentLogFile.getAbsolutePath() + "\nChannel size: " + rafCurrentLogFile.getChannel().size());
            }
        } catch (Exception e) {
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write(e.toString());
            }
        }
        long currentTime = System.currentTimeMillis();
    }

    /**
   * This method is to generate the record body of a last log file record.
   * 
   * @param lastFilename the file name of the previous log file
   * @param lastAccHash the accumulated hash of the previous log file
   * @param lastSignature the signature of the previous log file
   * 
   */
    public void createLastFileRecord(String lastFilename, byte[] lastAccHash, byte[] lastSignature) {
        byte[] b1 = lastFilename.getBytes();
        byte[] all = new byte[b1.length + lastAccHash.length + lastSignature.length + 4 * 3];
        System.arraycopy(utility.intToByteArray(b1.length), 0, all, 0, 4);
        System.arraycopy(b1, 0, all, 4, b1.length);
        System.arraycopy(utility.intToByteArray(lastAccHash.length), 0, all, 4 + b1.length, 4);
        System.arraycopy(lastAccHash, 0, all, 4 + b1.length + 4, lastAccHash.length);
        System.arraycopy(utility.intToByteArray(lastSignature.length), 0, all, 4 + b1.length + 4 + lastAccHash.length, 4);
        System.arraycopy(lastSignature, 0, all, 4 + b1.length + 4 + lastAccHash.length + 4, lastSignature.length);
        if (debugLevel > SAWSConstant.NoInfo) {
            sawsDebugLog.write("\n last log file record created, length = " + all.length);
        }
        createSAWSRecord(all, SAWSConstant.SAWSLastFileType, SAWSConstant.NoEncryptionFlag);
    }

    /**
   * This method is to create a Record header. count(SN): 4 bytes, UID: 1 byte !!, recordType: 1 byte, 
   * lastRecordLength: 4 bytes, thisRecordLength: 4 bytes, encryptionj: 1 byte !!. 
   * Between recordType and lastRecordLength, another parameter: (long)timeStamp (8 bytes) is 
   * automatically inserted into the byte array. Before the count, a 4 byte "0xf0" leading is 
   * inserted. So the total length for a header is 27!!. 
   *
   * @param countN the Sequence number of log record.
   * @param recordType the record type
   * @param UserID user ID ( it is 0x00 when SAWS is invoked by API interface)
   * @param encrytionFlag the encryption flag (symmetric, asymmetric and no encryption). 
   * @param lastLength last record length
   * @param thisLength this record length
   * 
   * @return bytes of the record header.
   */
    private byte[] createRecordHeader(int countN, byte recordType, byte UserID, byte encryptionFlag, int lastLength, int thisLength) {
        byte recordHeader[] = new byte[SAWSConstant.HeaderLength];
        recordHeader[0] = (byte) 0xf0;
        recordHeader[1] = (byte) 0xf0;
        recordHeader[2] = (byte) 0xf0;
        recordHeader[3] = (byte) 0xf0;
        if (debugLevel > SAWSConstant.NoInfo) {
            sawsDebugLog.write("\nSAWSWriter:createRecordHeader: count = " + countN);
            sawsDebugLog.write("SAWSWriter:createRecordHeader: recordType = " + SAWSConstant.getRecordTypeString(recordType));
            sawsDebugLog.write("SAWSWriter:createRecordHeader: LastLength = " + lastLength);
            sawsDebugLog.write("SAWSWriter:createRecordHeader: thisLength = " + thisLength);
        }
        byte[] ba1 = utility.intToByteArray(countN);
        System.arraycopy(ba1, 0, recordHeader, 4, 4);
        recordHeader[8] = recordType;
        recordHeader[9] = UserID;
        recordHeader[10] = encryptionFlag;
        sawsDebugLog.write("SAWSWriter:createRecordHeader: encryption = " + encryptionFlag);
        long ltemp = System.currentTimeMillis();
        byte[] ba2 = utility.longToByteArray(ltemp);
        if (debugLevel > SAWSConstant.NoInfo) {
            sawsDebugLog.write("SAWSWriter:createRecordHeader: current time is: " + ltemp);
            sawsDebugLog.write("SAWSWriter:createRecordHeader: length of ba2 is: " + ba2.length);
            sawsDebugLog.write("SAWSWriter:createRecordHeader: ba2 is: " + utility.toHexString(ba2));
        }
        System.arraycopy(ba2, 0, recordHeader, 11, 8);
        if (debugLevel > SAWSConstant.NoInfo) {
            sawsDebugLog.write("SAWSWriter:createRecordHeader: after transmision: " + utility.byteArrayToLong(ba2));
        }
        ba1 = utility.intToByteArray(lastLength);
        System.arraycopy(ba1, 0, recordHeader, 19, 4);
        ba1 = utility.intToByteArray(thisLength);
        System.arraycopy(ba1, 0, recordHeader, 23, 4);
        return (recordHeader);
    }

    /**
   * This method is to create a SAWS Record . It is a simplied invoke method of the following method.
   *
   * @param messageBlock log data block (here it is the secret random number to be wrapped into a record. 
   * @param recordType record type
   * @param encryptionFlag encryption flag to indicate the encryption requirement. 
   */
    public void createSAWSRecord(byte[] messageBlock, byte recordType, byte encryptionFlag) {
        createSAWSRecord(messageBlock, recordType, (byte) 0x00, encryptionFlag);
    }

    /**
   * This method is to create a SAWS Record . It is a simplied invoke method of the following method.
   *
   * @param messageBlock log data block (here it is the secret random number to be wrapped into a record. 
   * @param recordType record type
   * @param userID user ID
   * @param encryptionFlag encryption flag to indicate the encryption requirement. 
   */
    public void createSAWSRecord(byte[] messageBlock, byte recordType, byte userID, byte encryptionFlag) {
        createSAWSRecord(messageBlock, recordType, userID, encryptionFlag, sawsEncryptionPublicKey);
    }

    /**
   * This method is to create a SAWS Record .
   *
   * @param messageBlock log data block (here it is the secret random number to be wrapped into a record.
   * @param recordType record type
   * @param userID user ID
   * @param encryptionFlag encryption flag to indicate the encryption requirement.
   * @param encryptionPublicKey the encryption public key used for assymmetric encryption.
   *            This could be the VT enc public key, or the SAWS enc public key
   *
   */
    public void createSAWSRecord(byte[] messageBlock, byte recordType, byte userID, byte encryptionFlag, PublicKey encryptionPublicKey) {
        byte[] bodyTemp = null;
        int thisRecordLength;
        if (debugLevel > SAWSConstant.NoInfo) {
            sawsDebugLog.write("\nSAWSWriter:createSAWSRecord: currentCount = " + (currentRecordWriteCount + 1));
        }
        java.security.MessageDigest md = null;
        try {
            md = java.security.MessageDigest.getInstance(this.hashAlgorithmName);
        } catch (Exception e) {
            this.showMessage("Message digest error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write(e.toString());
            }
            System.exit(-1);
        }
        byte record[] = null;
        if (encryptionFlag == SAWSConstant.AsymmetricEncryptionFlag) {
            int messageLength = messageBlock.length;
            byte[] cipherText = null, c2 = null;
            try {
                javax.crypto.Cipher c1 = javax.crypto.Cipher.getInstance(encryptionPublicKey.getAlgorithm());
                c1.init(Cipher.ENCRYPT_MODE, encryptionPublicKey);
                cipherText = c1.doFinal(messageBlock);
                thisRecordLength = cipherText.length + SAWSConstant.HeaderLength + this.accMD.getDigestLength();
                record = new byte[thisRecordLength];
                ++currentRecordWriteCount;
                System.arraycopy(createRecordHeader(currentRecordWriteCount, recordType, userID, encryptionFlag, lastRecordLength, thisRecordLength), 0, record, 0, SAWSConstant.HeaderLength);
                lastRecordLength = thisRecordLength;
                System.arraycopy(cipherText, 0, record, SAWSConstant.HeaderLength, cipherText.length);
                bodyTemp = new byte[SAWSConstant.HeaderLength + cipherText.length];
                System.arraycopy(record, 0, bodyTemp, 0, SAWSConstant.HeaderLength + cipherText.length);
                md.reset();
                md.update(bodyTemp);
                md.update(secureRandomBytes);
                byte[] digest = md.digest();
                System.arraycopy(digest, 0, record, SAWSConstant.HeaderLength + cipherText.length, this.accMD.getDigestLength());
            } catch (Exception e) {
                e.printStackTrace();
                this.showMessage("Asymmetric encryption error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
                if (debugLevel > SAWSConstant.NoInfo) {
                    sawsDebugLog.write(e.toString());
                }
                System.exit(-1);
            }
        }
        if (encryptionFlag == SAWSConstant.SymmetricEncryptionFlag) {
            int messageLength = messageBlock.length;
            byte[] encrypted = null;
            try {
                Cipher cipher = null;
                try {
                    Security.addProvider(new BouncyCastleProvider());
                    cipher = Cipher.getInstance("AES", "BC");
                } catch (Exception ex) {
                    cipher = Cipher.getInstance("AES");
                }
                sawsDebugLog.write("Provider: " + cipher.getProvider().getName());
                cipher.init(Cipher.ENCRYPT_MODE, symmetricKeyInLog);
                encrypted = cipher.doFinal(messageBlock);
                thisRecordLength = encrypted.length + SAWSConstant.HeaderLength + this.accMD.getDigestLength();
                record = new byte[thisRecordLength];
                ++currentRecordWriteCount;
                System.arraycopy(createRecordHeader(currentRecordWriteCount, recordType, userID, encryptionFlag, lastRecordLength, thisRecordLength), 0, record, 0, SAWSConstant.HeaderLength);
                lastRecordLength = thisRecordLength;
                System.arraycopy(encrypted, 0, record, SAWSConstant.HeaderLength, encrypted.length);
                bodyTemp = new byte[SAWSConstant.HeaderLength + encrypted.length];
                System.arraycopy(record, 0, bodyTemp, 0, SAWSConstant.HeaderLength + encrypted.length);
                md.reset();
                md.update(bodyTemp);
                md.update(secureRandomBytes);
                byte[] digest = md.digest();
                System.arraycopy(digest, 0, record, SAWSConstant.HeaderLength + encrypted.length, this.accMD.getDigestLength());
            } catch (Exception e) {
                this.showMessage("Symmetric encryption error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
                if (debugLevel > SAWSConstant.NoInfo) {
                    sawsDebugLog.write(e.toString());
                }
                System.exit(-1);
            }
        }
        if (encryptionFlag == SAWSConstant.NoEncryptionFlag) {
            int messageLength = messageBlock.length;
            if (recordType != SAWSConstant.SAWSHashAlgorithmType) {
                thisRecordLength = messageLength + SAWSConstant.HeaderLength + this.accMD.getDigestLength();
                record = new byte[thisRecordLength];
                ++currentRecordWriteCount;
                System.arraycopy(createRecordHeader(currentRecordWriteCount, recordType, userID, encryptionFlag, lastRecordLength, thisRecordLength), 0, record, 0, SAWSConstant.HeaderLength);
                lastRecordLength = thisRecordLength;
                System.arraycopy(messageBlock, 0, record, SAWSConstant.HeaderLength, messageBlock.length);
                bodyTemp = new byte[SAWSConstant.HeaderLength + messageBlock.length];
                System.arraycopy(record, 0, bodyTemp, 0, SAWSConstant.HeaderLength + messageBlock.length);
                try {
                    md.reset();
                    md.update(bodyTemp);
                    md.update(secureRandomBytes);
                    byte[] digest = md.digest();
                    System.arraycopy(digest, 0, record, SAWSConstant.HeaderLength + messageBlock.length, this.accMD.getDigestLength());
                } catch (Exception e) {
                    this.showMessage("No-encryption record creating error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
                    if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e.toString());
                    System.exit(-1);
                }
            } else {
                thisRecordLength = messageLength + SAWSConstant.HeaderLength;
                record = new byte[thisRecordLength];
                ++currentRecordWriteCount;
                System.arraycopy(createRecordHeader(currentRecordWriteCount, recordType, userID, encryptionFlag, lastRecordLength, thisRecordLength), 0, record, 0, SAWSConstant.HeaderLength);
                lastRecordLength = thisRecordLength;
                System.arraycopy(messageBlock, 0, record, SAWSConstant.HeaderLength, messageBlock.length);
            }
        }
        if ((recordType != SAWSConstant.SAWSAccumulatedHashType) && (recordType != SAWSConstant.SAWSLogFileSignatureType)) {
            try {
                accMD.update(record);
                java.security.MessageDigest tc1 = (java.security.MessageDigest) accMD.clone();
                if (this.accumulatedHash == null) {
                    this.accumulatedHash = new byte[this.accMD.getDigestLength()];
                }
                accumulatedHash = tc1.digest();
            } catch (Exception e) {
                if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e.toString());
            }
        }
        writeLogRecord(record);
    }

    /**
   * This method is to repair a log file. Its main input is logReadingException e. 
   *
   * @param logRoot the log root
   * @param logFilename the log filename to be repaired. 
   * @param secureRandomB byte[] is the secure random bytes for this log file
   * @param logReadingException e is the exception class containing all the necessary information for the repair.
   * 
   * @return int. 0 for success, -1 for failure.
   */
    public int repairLog(String logRoot, String logFilename, byte[] secureRandomB, logReadingException e) {
        File logFile = new File(logRoot, logFilename);
        if (e.getErrorCode() != SAWSConstant.LogFileIncompleteErrCode) {
            this.showMessage("This broken log file " + logFilename + " cannot be recovered. ", SAWSTextOutputCallback.WARNING);
            return -1;
        }
        try {
            rafCurrentLogFile = new RandomAccessFile(logFile, "rw");
            rafCurrentLogFile.seek(rafCurrentLogFile.length());
        } catch (Exception e2) {
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e2.toString());
            return -1;
        }
        accMD = e.getAccMD();
        if (this.accumulatedHash == null) {
            accumulatedHash = new byte[this.accMD.getDigestLength()];
        }
        accumulatedHash = e.getAccumulatedHash();
        currentRecordWriteCount = e.getSequence();
        secureRandomBytes = secureRandomB;
        String s1 = new String("" + SAWSConstant.LogFileIncompleteErrCode + "; log file is recovered here.");
        try {
            createSAWSRecord(s1.getBytes(), SAWSConstant.SysAuditorNotificationType, (byte) SAWSConstant.USERSAWS, SAWSConstant.NoEncryptionFlag, null);
            finalizeLogFile();
        } catch (Exception e3) {
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e3.toString());
            return -1;
        }
        return 0;
    }

    /**
   * This method is called to finalize the log file, i.e. write the accumulated hash and
   * the signature to the log file.
   */
    public void finalizeLogFile() {
        if (debugLevel > 0) sawsDebugLog.write("\n\t ****************SAWSWriter:finalizeLogfile()******************* \n");
        createSAWSRecord(accumulatedHash, SAWSConstant.SAWSAccumulatedHashType, SAWSConstant.NoEncryptionFlag);
        writeSignatureRecord(SAWSConstant.SAWSLogFileSignatureType);
    }

    /**
   * This method is called to write a signature record to the log file after initialisation
   * is finished.
   * 
   * @param signatureType The type of signature record: SAWSConstant.SAWSHeaderSignatureType for the 
   * signature of the log file header; or SAWSConstant.SAWSSigningAlgorithmType for the signature
   * of the complete log file.
   */
    public void writeSignatureRecord(byte signatureType) {
        if (debugLevel >= SAWSConstant.VerboseInfo) sawsDebugLog.write("\n*********** SAWSWriter:writeSignatureRecord() ******************* \n");
        try {
            Signature sig = Signature.getInstance(signingAlgorithm);
            sig.initSign(sawsSigningPrivateKey);
            sig.update(accumulatedHash);
            finalSignature = sig.sign();
            createSAWSRecord(finalSignature, signatureType, SAWSConstant.NoEncryptionFlag);
        } catch (Exception e) {
            if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e.toString());
        }
    }

    /**
     * Method to create the callback (SAWSTextOutputCallback) with the message to be 
     * presented to the user and send it to the callback handler.
     * 
     * @param message The message to be presented.
     * @param type The type of the message (SAWSTextOutputCallback.WARNING, 
     * SAWSTextOutputCallback.ERROR, SAWSTextOutputCallback.INFORMATION)
     */
    private void showMessage(String message, int type) {
        Callback[] cbs = new Callback[1];
        cbs[0] = new SAWSTextOutputCallback(type, message);
        try {
            this.callbackHandler.handle(cbs);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            sawsDebugLog.write(e);
        }
    }
}
