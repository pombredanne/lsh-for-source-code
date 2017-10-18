package issrg.SAWS;

import issrg.SAWS.callback.SAWSGUICallbackHandler;
import issrg.SAWS.util.SAWSLogWriter;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.math.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import org.apache.soap.util.xml.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.AlgorithmParameters;
import java.security.interfaces.*;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import issrg.SAWS.callback.SAWSTextOutputCallback;
import issrg.SAWS.util.LogRecordInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

/**
 * This is the log file reading class
 *
 * @author W. Xu 
 * @version 0.1, Oct. 2005
 */
public class LogFileReader {

    private SecretKey sawsSecretKeyFromLog = null;

    private PrivateKey userPrivateKey = null;

    private PrivateKey sawsPrivateKey = null;

    private boolean logEndFlag = false;

    private boolean accumulatedRecordMetFlag = false;

    private int currentSNCheck = 0;

    private String errorMsg = null;

    private byte[] accumulatedHashFromLog = null;

    private byte[] signatureFromLog = null;

    private byte[] secureRandomBytes = null;

    private java.security.cert.Certificate certFromLog = null;

    private byte[] accumulatedHashByCalc = null;

    private java.security.MessageDigest accMD = null;

    private File CurrentLogFile = null;

    private String logFileRoot = null;

    private String logFileNameForLastLog = null;

    private byte[] accumulatedHashForLastLog = null;

    private byte[] signatureForLastLog = null;

    Vector recordBlockList = new Vector();

    private StringBuffer allReadingResult = new StringBuffer();

    private byte[] accumulatedHashForLogHeader = null;

    private byte[] headerSignature = null;

    private int debugLevel = 0;

    private byte userID;

    private CallbackHandler callbackHandler = new SAWSGUICallbackHandler();

    private static SAWSLogWriter sawsDebugLog = new SAWSLogWriter(LogFileReader.class.getName());

    String hashAlgorithmName = null;

    private String signingAlgorithm = null;

    private String sn, rt, et, ts, rl, lrl, sa, ha, data;

    public Vector getRecordBlockList() {
        return recordBlockList;
    }

    public String getAllCheckingResult() {
        return allReadingResult.toString();
    }

    public String getPreviousLogfileName() {
        return logFileNameForLastLog;
    }

    /**
   * This method is the constructor of LogFileReader.
   * 
   */
    public LogFileReader(int debugLevel) {
        this(debugLevel, (byte) 0x00, new SAWSGUICallbackHandler());
    }

    public LogFileReader(int debugLevel, byte userID) {
        this(debugLevel, userID, new SAWSGUICallbackHandler());
    }

    public LogFileReader(int debugLevel, CallbackHandler ch) {
        this(debugLevel, (byte) 0x00, ch);
    }

    public LogFileReader(int debugLevel, byte userID, CallbackHandler ch) {
        this.debugLevel = debugLevel;
        this.userID = userID;
        this.callbackHandler = ch;
        this.readProperties();
    }

    private void readProperties() {
        ResourceBundle rb = null;
        try {
            rb = new PropertyResourceBundle(new FileInputStream("saws.properties"));
        } catch (Exception ex) {
            this.showMessage("Properties file 'saws.properties' could not be found. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write(ex.toString());
            }
            System.exit(-1);
        }
        sn = rb.getString("sequenceNumber");
        rt = rb.getString("recordType");
        et = rb.getString("encryptionType");
        ts = rb.getString("timestamp");
        rl = rb.getString("recordLength");
        lrl = rb.getString("lastRecordLength");
        sa = rb.getString("signingAlgorithm");
        ha = rb.getString("hashAlgorithm");
        data = rb.getString("data");
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
   * This method is to initialise the MessageDigest for accumulated hash computation. 
   *
   * @param null
   * 
   * @return null. 
   */
    public void setLogFilename(String logRoot, String logFilename) throws logReadingException {
        if (debugLevel > SAWSConstant.NoInfo) {
            sawsDebugLog.write("\nIn initAccMD in LogFileReader: The log root is " + logRoot + "; the log Filename is " + logFilename);
        }
        logFileRoot = logRoot;
        CurrentLogFile = new File(logRoot, logFilename);
        if (debugLevel > SAWSConstant.NoInfo) {
            sawsDebugLog.write("\nIn initAccMD in LogFileReader: The whole log name is " + logRoot + File.pathSeparator + logFilename);
        }
        logEndFlag = false;
        accumulatedRecordMetFlag = false;
        this.hashAlgorithmName = this.getAccumulatedHashAlgorithm(this.CurrentLogFile);
        try {
            accMD = java.security.MessageDigest.getInstance(this.hashAlgorithmName);
        } catch (Exception e) {
            this.showMessage("Accumulated hash error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write(e.toString());
            }
            System.exit(-1);
        }
        accMD.update(logFilename.getBytes());
    }

    /**
   * This method is to reset the MessageDigest for accumulated hash computation. 
   *
   */
    public void resetAccMD() {
        try {
            accMD.reset();
        } catch (Exception e) {
            this.showMessage("Accumulated hash reset error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e.toString() + "\nAccumulated hash reset error.");
            System.exit(-1);
        }
        logEndFlag = false;
        accumulatedRecordMetFlag = false;
    }

    /**
   * This method is to asymmetric-decrypt a Record block
   * 
   * @param body is the log record block
   * @param privateKey is the private key
   * 
   * @return decrypted byte []
   */
    public byte[] ADecryptRecordBodyByPrivateKey(byte[] body, PrivateKey privateKey) throws logReadingException {
        byte[] decipherText = null;
        try {
            javax.crypto.Cipher c1 = javax.crypto.Cipher.getInstance(privateKey.getAlgorithm());
            c1.init(Cipher.DECRYPT_MODE, privateKey);
            decipherText = c1.doFinal(body);
        } catch (Exception e) {
            if (debugLevel > SAWSConstant.VerboseInfo) sawsDebugLog.write(e.toString());
            throw new logReadingException(SAWSConstant.ADecryptErrCode);
        }
        return (decipherText);
    }

    /**
   * This method is to symmetric-decrypt a Record block: log message . 
   * sawsSecretKeyFromLog is used within this method. 
   * 
   * @param body is the encrypted log record block with the symmetric key
   * 
   * @return decrypted byte []
   */
    public byte[] SDecryptRecordBody(byte[] body) {
        byte[] decipherText = null;
        try {
            Cipher cipher = Cipher.getInstance("AES");
            sawsDebugLog.write("Provider: " + cipher.getProvider().getName());
            cipher.init(Cipher.DECRYPT_MODE, sawsSecretKeyFromLog);
            decipherText = cipher.doFinal(body);
        } catch (Exception e) {
            e.printStackTrace();
            this.showMessage("Error decrypting data. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e + "\nDecryption error.");
            System.exit(-1);
        }
        return (decipherText);
    }

    /**
   * This method is to read a Record block (only body): log message . 
   * 
   * @param raf is the random access file to be read
   * @param offset is offset
   * @param len is the length to be read
   * 
   * @return bytes of the record body.
   */
    public byte[] readRecordBodyFromRAF(RandomAccessFile raf, int offset, int len) throws logReadingException {
        byte[] thisRecord = new byte[len];
        try {
            long pos = raf.getFilePointer();
            raf.seek(pos + offset);
            int lenRead = raf.read(thisRecord, 0, len);
            if (lenRead < len) {
                throw new logReadingException(pos, currentSNCheck, accMD, accumulatedHashByCalc, SAWSConstant.LogFileBodyReadingErrCode);
            }
        } catch (Exception e) {
            this.showMessage("Reading record body error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e + "\nReading record body error.");
            System.exit(-1);
        }
        return (thisRecord);
    }

    /**
   * This method is to move the RAF pointer by a distance of len
   * 
   * @param raf is the RAF file
   * @param offset is the offset
   * 
   * @return null
   */
    public void shiftRAFPointer(RandomAccessFile raf, int offset) {
        try {
            raf.seek(raf.getFilePointer() + offset);
        } catch (Exception e) {
            this.showMessage("Shifting log file pointer error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e + "\nShifting log file pointer error.");
            System.exit(-1);
        }
        return;
    }

    /**
   * This method is to read a Record Hash: 20 bytes  
   * 
   * @param raf is the random access file
   * 
   * @return bytes of the record hash (20 bytes).
   */
    public byte[] readRecordHashFromRAF(RandomAccessFile raf) throws logReadingException {
        byte[] thisHash = null;
        int hashLength = this.accMD.getDigestLength();
        try {
            long pos = raf.getFilePointer();
            thisHash = new byte[hashLength];
            int length = raf.read(thisHash, 0, hashLength);
            if (length < hashLength) {
                throw new logReadingException(pos, currentSNCheck, accMD, accumulatedHashByCalc, SAWSConstant.LogFileHashReadingErrCode);
            }
        } catch (Exception e) {
            this.showMessage("Reading log record hash error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e + "\nReading log record hash error.");
            System.exit(-1);
        }
        return (thisHash);
    }

    /**
   * This method is to get SN from a Record header. 
   * 
   * @param recordHeader record header 
   * 
   * @return byte SN of the record header.
   */
    public int getSNFromRecordHeader(byte[] recordHeader) {
        byte[] snBytes = new byte[4];
        System.arraycopy(recordHeader, 4, snBytes, 0, 4);
        return (utility.byteArrayToInt(snBytes));
    }

    /**
   * This method is to get record type from a Record header. 
   * 
   * @param recordHeader record header 
   * 
   * @return byte of the record type. 
   */
    public byte getRecordTypeFromRecordHeader(byte[] recordHeader) {
        return recordHeader[8];
    }

    /**
   * This method is to get the user ID from a Record header. 
   * 
   * @param recordHeader record header 
   * 
   * @return byte of the user id. 
   */
    public byte getUserIDFromRecordHeader(byte[] recordHeader) {
        return recordHeader[9];
    }

    /**
   * This method is to get encryption flag from a Record header. 
   * 
   * @param recordHeader record header 
   * 
   * @return byte of the encryption flag 
   */
    public byte getEncryptionFlagFromRecordHeader(byte[] recordHeader) {
        return recordHeader[10];
    }

    /**
   * This method is to get timestamp from a Record header. 
   * 
   * @param recordHeader record header 
   * 
   * @return bytes of the timestamp (8 bytes)
   */
    public long getTimestampFromRecordHeader(byte[] recordHeader) {
        byte[] tsBytes = new byte[8];
        System.arraycopy(recordHeader, 11, tsBytes, 0, 8);
        return (utility.byteArrayToLong(tsBytes));
    }

    /**
   * This method is to get LastRecordLength from a Record header. 
   * 
   * @param recordHeader record header 
   * 
   * @return length of the LastRecordLength 
   */
    public int getLastRecordLengthFromRecordHeader(byte[] recordHeader) {
        byte[] tsBytes = new byte[4];
        System.arraycopy(recordHeader, 19, tsBytes, 0, 4);
        return (utility.byteArrayToInt(tsBytes));
    }

    /**
   * This method is to get this record length from a Record header. 
   * 
   * @param recordHeader record header 
   * 
   * @return length of this record 
   */
    public int getThisRecordLengthFromRecordHeader(byte[] recordHeader) {
        byte[] tsBytes = new byte[4];
        System.arraycopy(recordHeader, 23, tsBytes, 0, 4);
        return (utility.byteArrayToInt(tsBytes));
    }

    /**
   * This method is to read one complete log record from a RAF. This is used by SAWS VT who does not
   * have the secure random number.
   * 
   * @param raf1 is the RAF file
   *
   * @return String result. Return null if end of file is reached. 
   */
    public String readOneRecordFromRAF(RandomAccessFile raf1) throws logReadingException {
        return readOneRecordFromRAF(raf1, null);
    }

    /**
   * This method is to read one complete log record from a RAF. 
   * 
   * @param raf1 is the RAF file
   * @param secureRandomBytes is the secure number used for secure hash   
   * 
   * @return String result. Return null if end of file is reached. 
   */
    public String readOneRecordFromRAF(RandomAccessFile raf1, byte[] secureRandomBytes) throws logReadingException {
        java.security.MessageDigest md = null;
        try {
            md = java.security.MessageDigest.getInstance(this.hashAlgorithmName);
        } catch (Exception e) {
            this.showMessage("Message Digest error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e.toString());
            System.exit(-1);
        }
        byte[] h1 = readRecordHeaderFromRAF(raf1);
        if (h1 == null) return null;
        int thisRecordLength = getThisRecordLengthFromRecordHeader(h1);
        StringBuffer temp = new StringBuffer();
        currentSNCheck = getSNFromRecordHeader(h1);
        temp.append("\n\n" + sn + ": " + currentSNCheck);
        byte recordType = (byte) getRecordTypeFromRecordHeader(h1);
        temp.append("\n" + rt + ": " + SAWSConstant.getRecordTypeString(recordType));
        temp.append("\n" + et + ": " + getEncryptionTypeString(getEncryptionFlagFromRecordHeader(h1)));
        temp.append("\n" + ts + ": " + (new Date(getTimestampFromRecordHeader(h1))).toString());
        temp.append("\n" + rl + ": " + thisRecordLength + " bytes");
        temp.append("\n" + lrl + ": " + getLastRecordLengthFromRecordHeader(h1) + " bytes");
        byte[] body = null;
        byte[] hash1 = null;
        if (recordType != SAWSConstant.SAWSHashAlgorithmType) {
            body = readRecordBodyFromRAF(raf1, 0, thisRecordLength - this.accMD.getDigestLength() - SAWSConstant.HeaderLength);
            hash1 = readRecordHashFromRAF(raf1);
        } else {
            body = readRecordBodyFromRAF(raf1, 0, thisRecordLength - SAWSConstant.HeaderLength);
        }
        byte[] decryptedBody = null;
        byte[] newAccumulatedHashByCalc = null;
        if (recordType == SAWSConstant.SAWSAccumulatedHashType) {
            accumulatedRecordMetFlag = true;
            accumulatedHashFromLog = body;
        } else if (recordType == SAWSConstant.SAWSLogFileSignatureType) {
            signatureFromLog = body;
            if (accumulatedRecordMetFlag) logEndFlag = true;
        } else {
            if (recordType == SAWSConstant.SAWSCertificateType) {
                ByteArrayInputStream bais = new ByteArrayInputStream(body);
                java.security.cert.CertificateFactory cf = null;
                try {
                    cf = java.security.cert.CertificateFactory.getInstance("X.509");
                    certFromLog = cf.generateCertificate(bais);
                } catch (Exception e3) {
                    e3.printStackTrace(System.err);
                }
            }
            if ((sawsSecretKeyFromLog == null) && (recordType == SAWSConstant.SymmetricEncryptionKeyType) && (getUserIDFromRecordHeader(h1) != SAWSConstant.USERSAWS) && (userPrivateKey != null)) {
                byte[] rawKey1 = ADecryptRecordBodyByPrivateKey(body, userPrivateKey);
                if (rawKey1 != null) {
                    sawsSecretKeyFromLog = new SecretKeySpec(rawKey1, "AES");
                }
                logFileNameForLastLog = null;
            }
            if ((recordType == SAWSConstant.SymmetricEncryptionKeyType) && (getUserIDFromRecordHeader(h1) == SAWSConstant.USERSAWS) && (sawsPrivateKey != null)) {
                byte[] rawKey1 = ADecryptRecordBodyByPrivateKey(body, sawsPrivateKey);
                sawsSecretKeyFromLog = new SecretKeySpec(rawKey1, "AES");
                logFileNameForLastLog = null;
            }
            byte[] clearMessageBlock = null;
            if (getEncryptionFlagFromRecordHeader(h1) == SAWSConstant.SymmetricEncryptionFlag) {
                clearMessageBlock = SDecryptRecordBody(body);
            } else {
                clearMessageBlock = body;
            }
            if (recordType == SAWSConstant.SAWSLastFileType) {
                extractLastLogInfo(clearMessageBlock);
            }
            if (recordType == SAWSConstant.SAWSClientLogDataType) {
                temp.append("\n" + data + ":\n" + new String(clearMessageBlock));
                RecordBlock rb = new RecordBlock(clearMessageBlock, getUserIDFromRecordHeader(h1));
                recordBlockList.addElement(rb);
            }
            if (recordType == SAWSConstant.SysAuditorNotificationType) {
                temp.append("\n" + data + ":\n" + new String(clearMessageBlock));
            }
            int thisRecordLength2 = 0;
            byte[] thisLogRecord2 = null;
            if (recordType == SAWSConstant.SAWSSigningAlgorithmType) {
                this.signingAlgorithm = new String(body);
                temp.append("\n" + sa + ": " + this.signingAlgorithm);
            }
            if (recordType == SAWSConstant.SAWSHashAlgorithmType) {
                temp.append("\n" + ha + ": " + this.hashAlgorithmName);
                thisRecordLength2 = h1.length + body.length;
                thisLogRecord2 = new byte[thisRecordLength2];
                System.arraycopy(h1, 0, thisLogRecord2, 0, h1.length);
                System.arraycopy(body, 0, thisLogRecord2, h1.length, body.length);
            } else {
                thisRecordLength2 = h1.length + body.length + hash1.length;
                thisLogRecord2 = new byte[thisRecordLength2];
                System.arraycopy(h1, 0, thisLogRecord2, 0, h1.length);
                System.arraycopy(body, 0, thisLogRecord2, h1.length, body.length);
                System.arraycopy(hash1, 0, thisLogRecord2, h1.length + body.length, hash1.length);
            }
            if (!accumulatedRecordMetFlag) {
                accMD.update(thisLogRecord2);
                try {
                    java.security.MessageDigest tc1 = (java.security.MessageDigest) accMD.clone();
                    accumulatedHashByCalc = tc1.digest();
                } catch (Exception e4) {
                    this.showMessage("Accumulated hash error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
                    if (debugLevel > SAWSConstant.NoInfo) {
                        sawsDebugLog.write(e4.toString());
                    }
                    System.exit(-1);
                }
                if (recordType == SAWSConstant.SAWSHeaderSignatureType) {
                    this.accumulatedHashForLogHeader = this.accumulatedHashByCalc;
                    this.headerSignature = body;
                }
            }
        }
        if ((recordType != SAWSConstant.SAWSHashAlgorithmType) && (secureRandomBytes != null)) {
            md.reset();
            md.update(h1);
            md.update(body);
            md.update(secureRandomBytes);
            byte[] digest = md.digest();
            String s1 = new String(Base64.encode(digest));
            String s2 = new String(Base64.encode(hash1));
            if (s1.compareTo(s2) == 0) {
                temp.append("\nThe secure hash of this log record is verified.\n");
            } else {
                throw new logReadingException(SAWSConstant.SecureHashNotCorrect, currentSNCheck);
            }
        }
        try {
            if (raf1.getFilePointer() < raf1.length() - 1) {
                return temp.toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            this.showMessage("Log file get file pointer error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e.toString());
            System.exit(-1);
        }
        return temp.toString();
    }

    /**
   * This method is to read a Record header from a random access file. 
   * 
   * @param raf1 is the RAF file
   * 
   * @return bytes of the record header.
   */
    public byte[] readRecordHeaderFromRAF(RandomAccessFile raf1) throws logReadingException {
        long currentPos = 0, length;
        byte[] thisRecordHeader = null;
        boolean flag = false;
        try {
            currentPos = raf1.getFilePointer();
            length = raf1.length();
            thisRecordHeader = new byte[SAWSConstant.HeaderLength];
            int length1 = raf1.read(thisRecordHeader, 0, SAWSConstant.HeaderLength);
            if ((length1 == -1) || (length1 == 0)) {
                throw new logReadingException(currentPos, currentSNCheck, accMD, accumulatedHashByCalc, SAWSConstant.LogFileIncompleteErrCode);
            }
            if ((length1 < SAWSConstant.HeaderLength)) {
                throw new logReadingException(currentPos, currentSNCheck, accMD, accumulatedHashByCalc, SAWSConstant.LogFileFormatErrCode);
            }
        } catch (IOException e4) {
            try {
                raf1.close();
            } catch (Exception e2) {
                e2.printStackTrace(System.err);
            }
            throw new logReadingException(currentPos, currentSNCheck, accMD, accumulatedHashByCalc, SAWSConstant.LogFileReadingErrCode);
        }
        return (thisRecordHeader);
    }

    /**
   * This method is to set VT private key.
   *
   * @param pk is the VT private key
   */
    public void setUserPrivateKey(PrivateKey pk) {
        userPrivateKey = pk;
    }

    /**
   * This method is to set saws private key.
   *
   * @param pk is the saws private key
   */
    public void setSAWSPrivateKey(PrivateKey pk) {
        sawsPrivateKey = pk;
    }

    /**
   * This method is to extract LastLogRecord information
   *
   * @param body is the LastLogRecord body
   */
    private void extractLastLogInfo(byte[] body) {
        byte[] baLen = new byte[4];
        byte[] temp = null;
        System.arraycopy(body, 0, baLen, 0, 4);
        int iLen1 = utility.byteArrayToInt(baLen);
        temp = new byte[iLen1];
        System.arraycopy(body, 4, temp, 0, iLen1);
        logFileNameForLastLog = new String(temp);
        System.arraycopy(body, 4 + iLen1, baLen, 0, 4);
        int iLen2 = utility.byteArrayToInt(baLen);
        temp = new byte[iLen2];
        System.arraycopy(body, 4 + iLen1 + 4, temp, 0, iLen2);
        accumulatedHashForLastLog = temp;
        System.arraycopy(body, 4 + iLen1 + 4 + iLen2, baLen, 0, 4);
        int iLen3 = utility.byteArrayToInt(baLen);
        temp = new byte[iLen3];
        System.arraycopy(body, 4 + iLen1 + 4 + iLen2 + 4, temp, 0, iLen3);
        signatureForLastLog = temp;
        String s1 = new String(accumulatedHashForLastLog);
        if (s1.compareTo("null") == 0) {
            logFileNameForLastLog = null;
            if (debugLevel > 0) sawsDebugLog.write("This is the very first log file. There is no more previous log file existing. ");
        } else if (debugLevel > 0) sawsDebugLog.write("Last log file name stored in this log is: " + logFileNameForLastLog);
    }

    /**
   * This method is to return accumulated hash got by calculation during verification.
   *
   */
    public byte[] getAccumulatedHashByCalc() {
        return accumulatedHashByCalc;
    }

    /**
   * This method is to return accumulated hash stored in the log file 
   *
   */
    public byte[] getAccumulatedHashFromLog() {
        return accumulatedHashFromLog;
    }

    /**
   * This method is to return signature from the log file 
   *
   */
    public byte[] getSignatureFromLog() {
        return signatureFromLog;
    }

    /**
   * This method returns the signing algorithm from the log file.
   *
   */
    public String getSigningAlgorithmFromLog() {
        return this.signingAlgorithm;
    }

    /**
   * This method is to return the certificate in the log file 
   *
   */
    public java.security.cert.Certificate getCertFromLog() {
        return certFromLog;
    }

    /**
   * Method to check if the signature of the log file's header is valid.
   * 
   * @return 0, if the signature if valid or -1, if not.
   */
    private int checkHeaderSignature() throws logReadingException {
        int result = -1;
        try {
            Signature sig = Signature.getInstance(this.signingAlgorithm);
            sig.initVerify(this.certFromLog.getPublicKey());
            sig.update(this.accumulatedHashForLogHeader);
            boolean vr = sig.verify(this.headerSignature);
            if (vr) {
                allReadingResult.append("\nThe Log's Header signature is valid.");
                result = 0;
            }
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("\nSignature of the log's header is " + vr);
        } catch (Exception e4) {
            throw new logReadingException(SAWSConstant.HeaderSignatureIsNotValid);
        }
        return result;
    }

    /**
   * This method is to verify the signature of the log file 
   *
   * @param sawsCAPublicKey is the saws public key. 
   *
   */
    public int checkSignature(PublicKey sawsCAPublicKey) throws logReadingException {
        int result = 0;
        if (logEndFlag) {
            String accCalc = new String(Base64.encode(getAccumulatedHashByCalc()));
            String accLog = new String(Base64.encode(getAccumulatedHashFromLog()));
            if (accCalc.compareTo(accLog) == 0) {
                allReadingResult.append("\n\nThe accumulated hash by calculation is the same as the accumulated hash stored in the log file.");
                if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("The accumulated hash by calc is the same as the accumulated hash in log.");
            } else {
                int isValid = this.checkHeaderSignature();
                if (isValid == 0) {
                    if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("Header signature valid. The body of the log file must be corrupted.");
                    throw new logReadingException(SAWSConstant.CalculatedAccHashNotEqualsAccHashInLog);
                } else {
                    if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("Header signature is not valid. The header of the log file must be corrupted.");
                    throw new logReadingException(SAWSConstant.HeaderSignatureIsNotValid);
                }
            }
        }
        byte[] lastAccumulatedHash = getAccumulatedHashByCalc();
        byte[] lastSignature = getSignatureFromLog();
        java.security.cert.Certificate cert1 = getCertFromLog();
        try {
            cert1.verify(sawsCAPublicKey);
            allReadingResult.append("\nThe PKC in this log file is verified.");
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("\nYes, saws certificate is valid!\n");
        } catch (Exception e3) {
            throw new logReadingException(SAWSConstant.CertificateInLogIsNotValidAgainstRootCA);
        }
        try {
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("In the beginning:  " + this.signingAlgorithm);
            Signature sig2 = Signature.getInstance(this.signingAlgorithm);
            sig2.initVerify(cert1.getPublicKey());
            sig2.update(lastAccumulatedHash);
            boolean vr = sig2.verify(lastSignature);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("\nSignature of the log file is " + vr);
        } catch (Exception e4) {
            e4.printStackTrace();
            throw new logReadingException(SAWSConstant.SignatureIsNotValid);
        }
        return result;
    }

    public int checkLogFile() throws logReadingException {
        return checkLogFile(null);
    }

    /**
   * This method is for checking a log file.
   *
   * @param logFile is File to be checked.
   * @param secureRandomBytes is the secure number used for checking secure hashes
   * 
   * @return int 0: true, otherwise: false. 
   */
    public int checkLogFile(byte[] secureRandomBytes) throws logReadingException {
        RandomAccessFile raf1 = null;
        if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("\nNow checking log file:  " + CurrentLogFile.getPath());
        try {
            raf1 = new RandomAccessFile(CurrentLogFile, "r");
        } catch (Exception e1) {
            this.showMessage("Open log file error: " + CurrentLogFile.getPath(), SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e1.toString());
        }
        for (int i = 0; ; ++i) {
            if (logEndFlag == true) break;
            String temp = readOneRecordFromRAF(raf1, secureRandomBytes);
            if (temp != null) {
                allReadingResult.append(temp);
                if (debugLevel > SAWSConstant.NoInfo) {
                    sawsDebugLog.write(temp);
                }
            }
            if (currentSNCheck != i) {
                throw new logReadingException(SAWSConstant.SNNotCorrectInLog, i);
            }
        }
        try {
            raf1.close();
        } catch (Exception e1) {
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e1.toString());
        }
        return 0;
    }

    /**
   * This method is to find the previous log file name stored in this log file.
   *
   * @param secureRandomBytes is secure random bytes. 
   * 
   * @return String the previous log file name. 
   */
    public String findPreviousLogfileName(byte[] secureRandomBytes) throws logReadingException {
        String pLogName = null;
        RandomAccessFile raf1 = null;
        if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("\nNow checking log file:  " + CurrentLogFile.getPath());
        try {
            raf1 = new RandomAccessFile(CurrentLogFile, "r");
        } catch (Exception e1) {
            e1.printStackTrace(System.err);
        }
        for (int i = 0; ; ++i) {
            if (logEndFlag == true || logFileNameForLastLog != null) break;
            readOneRecordFromRAF(raf1, secureRandomBytes);
            if (currentSNCheck != i) {
                throw new logReadingException(SAWSConstant.SNNotCorrectInLog, i);
            }
        }
        try {
            raf1.close();
        } catch (Exception e1) {
            e1.printStackTrace(System.err);
        }
        return logFileNameForLastLog;
    }

    /**
   * This method is to return the current SN 
   */
    public int getCurrentSN() {
        return currentSNCheck;
    }

    /**
   * This method is to return the SAWS certificate stored in the log file.  
   * 
   * @param CurrentLogFile is the current log file
   * 
   * @return the certificate. 
   */
    public java.security.cert.Certificate getCert(File CurrentLogFile) throws logReadingException {
        RandomAccessFile raf = null;
        this.CurrentLogFile = CurrentLogFile;
        try {
            raf = new RandomAccessFile(this.CurrentLogFile, "r");
        } catch (Exception e1) {
            e1.printStackTrace(System.err);
        }
        byte[] body = null;
        boolean flag = true;
        do {
            byte[] hTemp = null;
            hTemp = readRecordHeaderFromRAF(raf);
            int thisRecordLength = getThisRecordLengthFromRecordHeader(hTemp);
            if (getRecordTypeFromRecordHeader(hTemp) != SAWSConstant.SAWSCertificateType) {
                shiftRAFPointer(raf, thisRecordLength - SAWSConstant.HeaderLength);
                continue;
            } else {
                body = readRecordBodyFromRAF(raf, 0, thisRecordLength - this.accMD.getDigestLength() - SAWSConstant.HeaderLength);
                break;
            }
        } while (flag == true);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(body);
        java.security.cert.CertificateFactory cf = null;
        java.security.cert.Certificate cert1 = null;
        try {
            cf = java.security.cert.CertificateFactory.getInstance("X.509");
            cert1 = cf.generateCertificate(bais);
        } catch (Exception e3) {
            e3.printStackTrace(System.err);
        }
        return cert1;
    }

    /**
   * This method is to read the secure random number from the log file 
   * with the given private key.  
   * 
   * @param CurrentLogFile is the log file 
   * @param is the private key
   * 
   * @return byte[] is the secure random number. 
   */
    public byte[] getSecureRandomNumber(PrivateKey privateKey) throws logReadingException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(this.CurrentLogFile, "r");
        } catch (Exception e1) {
            e1.printStackTrace(System.err);
        }
        byte[] decryptedBody = null;
        boolean flag = true;
        do {
            byte[] hTemp = null;
            hTemp = readRecordHeaderFromRAF(raf);
            int thisRecordLength = getThisRecordLengthFromRecordHeader(hTemp);
            if (getRecordTypeFromRecordHeader(hTemp) != SAWSConstant.SAWSSecretRandomNumberType) {
                shiftRAFPointer(raf, thisRecordLength - SAWSConstant.HeaderLength);
                continue;
            } else {
                byte[] body = readRecordBodyFromRAF(raf, 0, thisRecordLength - this.accMD.getDigestLength() - SAWSConstant.HeaderLength);
                decryptedBody = ADecryptRecordBodyByPrivateKey(body, privateKey);
                break;
            }
        } while (flag == true);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return decryptedBody;
    }

    public SecretKey getSymmetricKey() {
        return sawsSecretKeyFromLog;
    }

    /**
   * This method is to read the symmetric key (byte[]) from the log file 
   * with the given private key.  
   * 
   * @param CurrentLogFile is the log file 
   * @param is the private key
   * @return byte[] is the symmetric key. 
   */
    public byte[] getSymmetricKey(File CurrentLogFile, PrivateKey privateKey) throws logReadingException {
        RandomAccessFile raf = null;
        this.CurrentLogFile = CurrentLogFile;
        try {
            raf = new RandomAccessFile(this.CurrentLogFile, "r");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        byte[] decryptedBody = null;
        boolean flag = true;
        do {
            byte[] hTemp = null;
            hTemp = readRecordHeaderFromRAF(raf);
            int thisRecordLength = getThisRecordLengthFromRecordHeader(hTemp);
            if (getRecordTypeFromRecordHeader(hTemp) != SAWSConstant.SymmetricEncryptionKeyType) {
                shiftRAFPointer(raf, thisRecordLength - SAWSConstant.HeaderLength);
                continue;
            } else {
                byte[] body = readRecordBodyFromRAF(raf, 0, thisRecordLength - this.accMD.getDigestLength() - SAWSConstant.HeaderLength);
                decryptedBody = ADecryptRecordBodyByPrivateKey(body, privateKey);
                break;
            }
        } while (flag == true);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return decryptedBody;
    }

    /**
   * This method is to read the AccumulatedHash (byte[]) from the log file 
   * 
   * @param CurrentLogFile is the log file 
   * @param is the private key
   * 
   * @return byte[] the accumualted hash. 
   */
    public byte[] getAccumulatedHash(File CurrentLogFile) throws logReadingException {
        RandomAccessFile raf = null;
        this.CurrentLogFile = CurrentLogFile;
        try {
            raf = new RandomAccessFile(this.CurrentLogFile, "r");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        byte[] accumulatedHash1 = null;
        boolean flag = true;
        do {
            byte[] hTemp = null;
            hTemp = readRecordHeaderFromRAF(raf);
            int thisRecordLength = getThisRecordLengthFromRecordHeader(hTemp);
            if (getRecordTypeFromRecordHeader(hTemp) != SAWSConstant.SAWSAccumulatedHashType) {
                shiftRAFPointer(raf, thisRecordLength - SAWSConstant.HeaderLength);
                continue;
            } else {
                byte[] body = readRecordBodyFromRAF(raf, 0, thisRecordLength - this.accMD.getDigestLength() - SAWSConstant.HeaderLength);
                accumulatedHash1 = body;
                break;
            }
        } while (flag == true);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return accumulatedHash1;
    }

    /**
     * This method is to read the hash algorithm from the log file 
     * 
     * @param CurrentLogFile is the log file 
     * 
     * @return String the accumualted hash algorithm. 
     */
    public String getAccumulatedHashAlgorithm(File CurrentLogFile) throws logReadingException {
        RandomAccessFile raf = null;
        this.CurrentLogFile = CurrentLogFile;
        try {
            raf = new RandomAccessFile(this.CurrentLogFile, "r");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        String accumulatedHashAlg = null;
        boolean flag = true;
        do {
            byte[] hTemp = null;
            hTemp = readRecordHeaderFromRAF(raf);
            int thisRecordLength = getThisRecordLengthFromRecordHeader(hTemp);
            if (getRecordTypeFromRecordHeader(hTemp) != SAWSConstant.SAWSHashAlgorithmType) {
                shiftRAFPointer(raf, thisRecordLength - SAWSConstant.HeaderLength);
                continue;
            } else {
                byte[] body = readRecordBodyFromRAF(raf, 0, thisRecordLength - SAWSConstant.HeaderLength);
                accumulatedHashAlg = new String(body);
                break;
            }
        } while (flag == true);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return accumulatedHashAlg;
    }

    /**
     * This method is to read the signing algorithm from the log file.
     * 
     * @param CurrentLogFile is the log file 
     * 
     * @return String the signing algorithm. 
     */
    public String getSigningAlgorithm(File CurrentLogFile) throws logReadingException {
        RandomAccessFile raf = null;
        this.CurrentLogFile = CurrentLogFile;
        try {
            raf = new RandomAccessFile(this.CurrentLogFile, "r");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        String sigAlg = null;
        boolean flag = true;
        do {
            byte[] hTemp = null;
            hTemp = readRecordHeaderFromRAF(raf);
            int thisRecordLength = getThisRecordLengthFromRecordHeader(hTemp);
            if (getRecordTypeFromRecordHeader(hTemp) != SAWSConstant.SAWSSigningAlgorithmType) {
                shiftRAFPointer(raf, thisRecordLength - SAWSConstant.HeaderLength);
                continue;
            } else {
                byte[] body = readRecordBodyFromRAF(raf, 0, thisRecordLength - this.accMD.getDigestLength() - SAWSConstant.HeaderLength);
                sigAlg = new String(body);
                break;
            }
        } while (flag == true);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return sigAlg;
    }

    /**
   * This method is to read the signature of the complete log (byte[]) from the log file 
   * 
   * @param CurrentLogFile is the log file 
   * @param is the private key
   * 
   * @return byte[] is the signature. 
   */
    public byte[] getLogFileSignature(File CurrentLogFile) throws logReadingException {
        RandomAccessFile raf = null;
        this.CurrentLogFile = CurrentLogFile;
        try {
            raf = new RandomAccessFile(this.CurrentLogFile, "r");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        byte[] sigBody = null;
        boolean flag = true;
        do {
            byte[] hTemp = null;
            hTemp = readRecordHeaderFromRAF(raf);
            int thisRecordLength = getThisRecordLengthFromRecordHeader(hTemp);
            if (getRecordTypeFromRecordHeader(hTemp) != SAWSConstant.SAWSLogFileSignatureType) {
                shiftRAFPointer(raf, thisRecordLength - SAWSConstant.HeaderLength);
                continue;
            } else {
                byte[] body = readRecordBodyFromRAF(raf, 0, thisRecordLength - this.accMD.getDigestLength() - SAWSConstant.HeaderLength);
                sigBody = body;
                break;
            }
        } while (flag == true);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return sigBody;
    }

    /**
     * This method is to read the signature of the log file's header (byte[]) from the log file.
     * 
     * @param CurrentLogFile is the log file 
     * @param is the private key
     * 
     * @return byte[] is the signature. 
     */
    public byte[] getHeaderSignature(File CurrentLogFile) throws logReadingException {
        RandomAccessFile raf = null;
        this.CurrentLogFile = CurrentLogFile;
        try {
            raf = new RandomAccessFile(this.CurrentLogFile, "r");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        byte[] sigBody = null;
        boolean flag = true;
        do {
            byte[] hTemp = null;
            hTemp = readRecordHeaderFromRAF(raf);
            int thisRecordLength = getThisRecordLengthFromRecordHeader(hTemp);
            if (getRecordTypeFromRecordHeader(hTemp) != SAWSConstant.SAWSHeaderSignatureType) {
                shiftRAFPointer(raf, thisRecordLength - SAWSConstant.HeaderLength);
                continue;
            } else {
                byte[] body = readRecordBodyFromRAF(raf, 0, thisRecordLength - this.accMD.getDigestLength() - SAWSConstant.HeaderLength);
                sigBody = body;
                break;
            }
        } while (flag == true);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return sigBody;
    }

    /**
   * This method is to give the encrytion type according to the type value
   * 
   * @param 
   * 
   * @return String of the encryption type. 
   */
    private String getEncryptionTypeString(byte encryptionTypeIn) {
        String enType = null;
        switch(encryptionTypeIn) {
            case SAWSConstant.AsymmetricEncryptionFlag:
                enType = new String("Asymmetric Encryption");
                break;
            case SAWSConstant.SymmetricEncryptionFlag:
                enType = new String("Symmetric Encryption");
                break;
            case SAWSConstant.NoEncryptionFlag:
                enType = new String("No Encryption");
                break;
            default:
                enType = new String("Wrong type");
        }
        return enType;
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

    /**
     * This method is to read one complete log record from a RAF. 
     * 
     * @param raf1 is the RAF file
     * @param secureRandomBytes is the secure number used for secure hash   
     * 
     * @return String result. Return null if end of file is reached. 
     */
    public LogRecordInfo getOneRecordInfoFromRAF(RandomAccessFile raf1, byte[] secureRandomBytes) throws logReadingException {
        java.security.MessageDigest md = null;
        try {
            md = java.security.MessageDigest.getInstance(this.hashAlgorithmName);
        } catch (Exception e) {
            this.showMessage("Message Digest error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e.toString());
            System.exit(-1);
        }
        byte[] h1 = readRecordHeaderFromRAF(raf1);
        if (h1 == null) return null;
        int thisRecordLength = getThisRecordLengthFromRecordHeader(h1);
        LogRecordInfo recordInf = new LogRecordInfo();
        currentSNCheck = getSNFromRecordHeader(h1);
        recordInf.setSn(currentSNCheck);
        byte recordType = (byte) getRecordTypeFromRecordHeader(h1);
        recordInf.setRecordType(recordType);
        recordInf.setEncryptionFlag(getEncryptionTypeString(getEncryptionFlagFromRecordHeader(h1)));
        recordInf.setTimeStamp(new Date(getTimestampFromRecordHeader(h1)));
        recordInf.setLastRecordLength(getLastRecordLengthFromRecordHeader(h1));
        recordInf.setRecordLength(thisRecordLength);
        byte[] body = null;
        byte[] hash1 = null;
        body = readRecordBodyFromRAF(raf1, 0, thisRecordLength - this.accMD.getDigestLength() - SAWSConstant.HeaderLength);
        hash1 = readRecordHashFromRAF(raf1);
        byte[] decryptedBody = null;
        byte[] newAccumulatedHashByCalc = null;
        if (recordType == SAWSConstant.SAWSAccumulatedHashType) {
            accumulatedRecordMetFlag = true;
            accumulatedHashFromLog = body;
        } else if (recordType == SAWSConstant.SAWSLogFileSignatureType) {
            signatureFromLog = body;
            if (accumulatedRecordMetFlag) logEndFlag = true;
        } else {
            if (recordType == SAWSConstant.SAWSCertificateType) {
                ByteArrayInputStream bais = new ByteArrayInputStream(body);
                java.security.cert.CertificateFactory cf = null;
                try {
                    cf = java.security.cert.CertificateFactory.getInstance("X.509");
                    certFromLog = cf.generateCertificate(bais);
                } catch (Exception e3) {
                    e3.printStackTrace(System.err);
                }
            }
            if ((sawsSecretKeyFromLog == null) && (recordType == SAWSConstant.SymmetricEncryptionKeyType) && (getUserIDFromRecordHeader(h1) != SAWSConstant.USERSAWS) && (userPrivateKey != null)) {
                byte[] rawKey1 = ADecryptRecordBodyByPrivateKey(body, userPrivateKey);
                if (rawKey1 != null) {
                    sawsSecretKeyFromLog = new SecretKeySpec(rawKey1, "AES");
                }
                logFileNameForLastLog = null;
            }
            if ((recordType == SAWSConstant.SymmetricEncryptionKeyType) && (getUserIDFromRecordHeader(h1) == SAWSConstant.USERSAWS) && (sawsPrivateKey != null)) {
                byte[] rawKey1 = ADecryptRecordBodyByPrivateKey(body, sawsPrivateKey);
                sawsSecretKeyFromLog = new SecretKeySpec(rawKey1, "AES");
                logFileNameForLastLog = null;
            }
            byte[] clearMessageBlock = null;
            if (getEncryptionFlagFromRecordHeader(h1) == SAWSConstant.SymmetricEncryptionFlag) {
                clearMessageBlock = SDecryptRecordBody(body);
            } else {
                clearMessageBlock = body;
            }
            if (recordType == SAWSConstant.SAWSLastFileType) {
                extractLastLogInfo(clearMessageBlock);
            }
            if (recordType == SAWSConstant.SAWSClientLogDataType) {
                recordInf.setData(new String(clearMessageBlock));
                RecordBlock rb = new RecordBlock(clearMessageBlock, getUserIDFromRecordHeader(h1));
                recordBlockList.addElement(rb);
            }
            if (recordType == SAWSConstant.SysAuditorNotificationType) {
                recordInf.setData(new String(clearMessageBlock));
            }
            int thisRecordLength2 = 0;
            byte[] thisLogRecord2 = null;
            if (recordType == SAWSConstant.SAWSHashAlgorithmType) {
                recordInf.setData(this.hashAlgorithmName);
            }
            if (recordType == SAWSConstant.SAWSSigningAlgorithmType) {
                this.signingAlgorithm = new String(body);
                recordInf.setData(this.signingAlgorithm);
            }
            thisRecordLength2 = h1.length + body.length + hash1.length;
            thisLogRecord2 = new byte[thisRecordLength2];
            System.arraycopy(h1, 0, thisLogRecord2, 0, h1.length);
            System.arraycopy(body, 0, thisLogRecord2, h1.length, body.length);
            System.arraycopy(hash1, 0, thisLogRecord2, h1.length + body.length, hash1.length);
            if (!accumulatedRecordMetFlag) {
                accMD.update(thisLogRecord2);
                try {
                    java.security.MessageDigest tc1 = (java.security.MessageDigest) accMD.clone();
                    accumulatedHashByCalc = tc1.digest();
                } catch (Exception e4) {
                    this.showMessage("Accumulated hash error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
                    if (debugLevel > SAWSConstant.NoInfo) {
                        sawsDebugLog.write(e4.toString());
                    }
                    System.exit(-1);
                }
                if (recordType == SAWSConstant.SAWSHeaderSignatureType) {
                    this.accumulatedHashForLogHeader = this.accumulatedHashByCalc;
                    this.headerSignature = body;
                }
            }
        }
        if ((secureRandomBytes != null)) {
            md.reset();
            md.update(h1);
            md.update(body);
            md.update(secureRandomBytes);
            byte[] digest = md.digest();
            String s1 = new String(Base64.encode(digest));
            String s2 = new String(Base64.encode(hash1));
            if (s1.compareTo(s2) == 0) {
                this.showMessage("The secure hash of this log record is verified.", SAWSTextOutputCallback.INFORMATION);
            } else {
                throw new logReadingException(SAWSConstant.SecureHashNotCorrect, currentSNCheck);
            }
        }
        try {
            if (raf1.getFilePointer() < raf1.length() - 1) {
                return recordInf;
            } else {
                return null;
            }
        } catch (Exception e) {
            this.showMessage("Log file get file pointer error. SAWS will stop.", SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e.toString());
            System.exit(-1);
        }
        return recordInf;
    }

    public String getXMLClientLogData(byte[] secureRandomBytes) throws logReadingException {
        RandomAccessFile raf1 = null;
        StringBuffer result = new StringBuffer();
        if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("\nNow checking log file:  " + CurrentLogFile.getPath());
        try {
            raf1 = new RandomAccessFile(CurrentLogFile, "r");
        } catch (Exception e1) {
            this.showMessage("Open log file error: " + CurrentLogFile.getPath(), SAWSTextOutputCallback.ERROR);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write(e1.toString());
            }
        }
        result.append("<results>\n");
        for (int i = 0; ; ++i) {
            if (logEndFlag == true) break;
            LogRecordInfo info = this.getOneRecordInfoFromRAF(raf1, secureRandomBytes);
            if (info == null) {
                break;
            }
            if (info.getRecordType() == SAWSConstant.SAWSClientLogDataType) {
                result.append("\t<result>\n");
                result.append(info.toXML());
                result.append("\t</result>\n");
            }
            if (info.getSn() != i) {
                throw new logReadingException(SAWSConstant.SNNotCorrectInLog, i);
            }
        }
        result.append("</results>");
        try {
            raf1.close();
        } catch (Exception e1) {
            if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write(e1.toString());
        }
        return result.toString();
    }
}
