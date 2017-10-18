package org.apache.fop.pdf;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.NoSuchPaddingException;
import java.util.Random;

/**
 * class representing a /Filter /Standard object.
 *
 */
public class PDFEncryption extends PDFObject {

    private class EncryptionFilter extends PDFFilter {

        PDFEncryption encryption;

        int number;

        int generation;

        /** The constructor for the internal PDFEncryption filter
         * @param encryption The encryption object to use
         * @param number The number of the object to be encrypted
         * @param generation The generation of the object to be encrypted
         */
        public EncryptionFilter(PDFEncryption encryption, int number, int generation) {
            super();
            this.encryption = encryption;
            this.number = number;
            this.generation = generation;
        }

        /** return a PDF string representation of the filter. In this
         * case no filter name is passed.
         * @return The filter name, blank in this case
         */
        public String getName() {
            return "";
        }

        /** return a parameter dictionary for this filter, or null
         * @return The parameter dictionary. In this case, null.
         */
        public String getDecodeParms() {
            return null;
        }

        /** encode the given data with the filter
         * @param data The data to be encrypted
         * @return The encrypted data
         */
        public byte[] encode(byte[] data) {
            return encryption.encryptData(data, number, generation);
        }
    }

    static final char[] pad = { 0x28, 0xBF, 0x4E, 0x5E, 0x4E, 0x75, 0x8A, 0x41, 0x64, 0x00, 0x4E, 0x56, 0xFF, 0xFA, 0x01, 0x08, 0x2E, 0x2E, 0x00, 0xB6, 0xD0, 0x68, 0x3E, 0x80, 0x2F, 0x0C, 0xA9, 0xFE, 0x64, 0x53, 0x69, 0x7A };

    static final char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /** Value of PRINT permission
                                   */
    public static final int PERMISSION_PRINT = 4;

    /** Value of content editting permission
     */
    public static final int PERMISSION_EDIT_CONTENT = 8;

    /** Value of content extraction permission
     */
    public static final int PERMISSION_COPY_CONTENT = 16;

    /** Value of annotation editting permission
     */
    public static final int PERMISSION_EDIT_ANNOTATIONS = 32;

    MessageDigest digest = null;

    Cipher cipher = null;

    Random random = new Random();

    String userPassword = "";

    String ownerPassword = "";

    boolean allowPrint = true;

    boolean allowCopyContent = true;

    boolean allowEditContent = true;

    boolean allowEditAnnotations = true;

    byte[] fileID = null;

    byte[] encryptionKey = null;

    String dictionary = null;

    /**
     * create a /Filter /Standard object.
     *
     * @param number the object's number
     */
    public PDFEncryption(int number) {
        super(number);
        try {
            digest = MessageDigest.getInstance("MD5");
            cipher = Cipher.getInstance("RC4");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (NoSuchPaddingException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /** This method allows the setting of the user password
     * @param value The string to use as the user password. It may be blank but not null.
     */
    public void setUserPassword(String value) {
        this.userPassword = value;
    }

    /** Returns the current user password
     * @return The user password
     */
    public String getUserPassword() {
        return this.userPassword;
    }

    /** Sets the owner password for the PDF
     * @param value The owner password
     */
    public void setOwnerPassword(String value) {
        this.ownerPassword = value;
    }

    /** Returns the owner password for the PDF
     * @return The owner password
     */
    public String getOwnerPassword() {
        return this.ownerPassword;
    }

    /** Set whether the document will allow printing.
     * @param value The new permision value
     */
    public void setAllowPrint(boolean value) {
        this.allowPrint = value;
    }

    /** Set whether the document will allow the content to be extracted
     * @param value The new permission value
     */
    public void setAllowCopyContent(boolean value) {
        this.allowCopyContent = value;
    }

    /** Set whether the document will allow content editting
     * @param value The new permission value
     */
    public void setAllowEditContent(boolean value) {
        this.allowEditContent = value;
    }

    /** Set whether the document will allow annotation modificcations
     * @param value The new permission value
     */
    public void setAllowEditAnnotation(boolean value) {
        this.allowEditAnnotations = value;
    }

    private byte[] prepPassword(String password) {
        byte[] obuffer = new byte[32];
        byte[] pbuffer = password.getBytes();
        int i = 0;
        int j = 0;
        while (i < obuffer.length && i < pbuffer.length) {
            obuffer[i] = pbuffer[i];
            i++;
        }
        while (i < obuffer.length) {
            obuffer[i++] = (byte) pad[j++];
        }
        return obuffer;
    }

    private String toHex(byte[] value) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < value.length; i++) {
            buffer.append(digits[(value[i] >>> 4) & 0x0F]);
            buffer.append(digits[value[i] & 0x0F]);
        }
        return buffer.toString();
    }

    /** Returns the document file ID
     * @return The file ID
     */
    public byte[] getFileID() {
        if (fileID == null) {
            fileID = new byte[16];
            random.nextBytes(fileID);
        }
        return fileID;
    }

    /** This method returns the indexed file ID
     * @param index The index to access the file ID
     * @return The file ID
     */
    public String getFileID(int index) {
        if (index == 1) {
            return toHex(getFileID());
        }
        byte[] id = new byte[16];
        random.nextBytes(id);
        return toHex(id);
    }

    private byte[] encryptWithKey(byte[] data, byte[] key) {
        try {
            SecretKeySpec keyspec = new SecretKeySpec(key, "RC4");
            cipher.init(Cipher.ENCRYPT_MODE, keyspec);
            return cipher.doFinal(data);
        } catch (IllegalBlockSizeException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (BadPaddingException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private byte[] encryptWithHash(byte[] data, byte[] hash, int size) {
        hash = digest.digest(hash);
        byte[] key = new byte[size];
        for (int i = 0; i < size; i++) {
            key[i] = hash[i];
        }
        return encryptWithKey(data, key);
    }

    /** This method initializes the encryption algorithms and values
     */
    public void init() {
        byte[] oValue;
        if (ownerPassword.length() > 0) {
            oValue = encryptWithHash(prepPassword(userPassword), prepPassword(ownerPassword), 5);
        } else {
            oValue = encryptWithHash(prepPassword(userPassword), prepPassword(userPassword), 5);
        }
        int permissions = -4;
        if (!allowPrint) {
            permissions -= PERMISSION_PRINT;
        }
        if (!allowCopyContent) {
            permissions -= PERMISSION_COPY_CONTENT;
        }
        if (!allowEditContent) {
            permissions -= PERMISSION_EDIT_CONTENT;
        }
        if (!allowEditAnnotations) {
            permissions -= PERMISSION_EDIT_ANNOTATIONS;
        }
        digest.update(prepPassword(userPassword));
        digest.update(oValue);
        digest.update((byte) (permissions >>> 0));
        digest.update((byte) (permissions >>> 8));
        digest.update((byte) (permissions >>> 16));
        digest.update((byte) (permissions >>> 24));
        digest.update(getFileID());
        byte[] hash = digest.digest();
        this.encryptionKey = new byte[5];
        for (int i = 0; i < 5; i++) {
            this.encryptionKey[i] = hash[i];
        }
        byte[] uValue = encryptWithKey(prepPassword(""), this.encryptionKey);
        this.dictionary = this.number + " " + this.generation + " obj\n<< /Filter /Standard\n" + "/V 1" + "/R 2" + "/Length 40" + "/P " + permissions + "\n" + "/O <" + toHex(oValue) + ">\n" + "/U <" + toHex(uValue) + ">\n" + ">>\n" + "endobj\n";
    }

    /** This method encrypts the passed data using the generated keys.
     * @param data The data to be encrypted
     * @param number The block number
     * @param generation The block generation
     * @return The encrypted data
     */
    public byte[] encryptData(byte[] data, int number, int generation) {
        if (this.encryptionKey == null) {
            throw new IllegalStateException("PDF Encryption has not been initialized");
        }
        byte[] hash = new byte[this.encryptionKey.length + 5];
        int i = 0;
        while (i < this.encryptionKey.length) {
            hash[i] = this.encryptionKey[i];
            i++;
        }
        hash[i++] = (byte) (number >>> 0);
        hash[i++] = (byte) (number >>> 8);
        hash[i++] = (byte) (number >>> 16);
        hash[i++] = (byte) (generation >>> 0);
        hash[i++] = (byte) (generation >>> 8);
        ;
        return encryptWithHash(data, hash, hash.length);
    }

    /** Creates PDFFilter for the encryption object
     * @param number The object number
     * @param generation The objects generation
     * @return The resulting filter
     */
    public PDFFilter makeFilter(int number, int generation) {
        return new EncryptionFilter(this, number, generation);
    }

    /**
     * represent the object in PDF
     *
     * @return the PDF
     */
    public byte[] toPDF() throws IllegalStateException {
        if (this.dictionary == null) {
            throw new IllegalStateException("PDF Encryption has not been initialized");
        }
        try {
            return this.dictionary.getBytes(PDFDocument.ENCODING);
        } catch (UnsupportedEncodingException ue) {
            return this.dictionary.getBytes();
        }
    }

    public static boolean encryptionAvailable() {
        return true;
    }
}
