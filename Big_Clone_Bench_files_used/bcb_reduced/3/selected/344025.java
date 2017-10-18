package edu.mit.osidimpl.filing.local;

import edu.mit.osidimpl.manager.*;
import edu.mit.osidimpl.filing.shared.*;

/**
 *  <p>
 *  Implements a ByteStore for the Filing implementation for standard
 *  Java File IO. The ByteStore is a type of CabinetEntry that can be
 *  read from or written to. It is not recommended to perform both
 *  read and write operations on the same ByteStore or to have multiple
 *  ByteStore objects open for writing.
 *  </p><p>
 *  CVS $Id: ByteStore.java,v 1.1 2006/04/13 20:15:00 tom Exp $
 *  </p>
 *  
 *  @author  Tom Coppeto
 *  @version $OSID: 2.0$ $Revision: 1.1 $
 */
public class ByteStore extends CabinetEntry implements org.osid.filing.ByteStore {

    private boolean appendOnly;

    private String mimeType;

    private org.osid.shared.Type algorithmType;

    private java.io.OutputStream outputStream;

    protected ByteStore(Cabinet cabinet, java.io.File file, org.osid.shared.Properties properties) throws org.osid.filing.FilingException {
        super(cabinet, file, properties);
        logger.logTrace("created new byte store for " + file.getPath());
    }

    /**
     *  Returns the length of this ByteStore
     *
     *  @return The length, in bytes, of this ByteStore
     *  @throws org.osid.filing.FilingException An exception with one of the
     *          following messages defined in org.osid.filing.FilingException
     *          may be thrown: 
     */
    public long length() throws org.osid.filing.FilingException {
        logger.logMethod();
        long length;
        try {
            length = this.file.length();
        } catch (SecurityException se) {
            logger.logError("unable to access " + this.file.getPath(), se);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.PERMISSION_DENIED);
        }
        logger.logTrace("length of " + this.file.getPath() + " is " + length);
        return (length);
    }

    /**
     *  Tests whether the Manager Owner may append to this ByteStore.
     *
     *  @return <code>true</code> if and only if the Manager Owner is allowed to
     *          append to this ByteStore, <code>false</code> otherwise.
     *  @throws org.osid.filing.FilingException
     */
    public boolean canAppend() throws org.osid.filing.FilingException {
        logger.logMethod();
        boolean b;
        try {
            b = this.file.canWrite();
        } catch (SecurityException se) {
            logger.logError("unable to access " + this.file.getPath(), se);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.PERMISSION_DENIED);
        }
        logger.logTrace("canAppend of " + this.file.getPath() + " is " + b);
        return (b);
    }

    /**
     *  Marks this ByteStore so that only append operations are allowed.
     *
     *  @throws org.osid.filing.FilingException
     */
    public void updateAppendOnly() throws org.osid.filing.FilingException {
        logger.logMethod();
        this.appendOnly = true;
        return;
    }

    /**
     *  Gets the mime-type of this ByteStore.
     *
     *  @return the mime-type (Content-Type in a jar file manifest)
     *  @throws org.osid.filing.FilingException
     */
    public String getMimeType() throws org.osid.filing.FilingException {
        logger.logMethod();
        logger.logTrace("mimeType of " + this.file.getPath() + " is " + this.mimeType);
        return (this.mimeType);
    }

    /**
     *  Set the mime-type of this ByteStore. Returns the actual mime-type set
     *  for the ByteStore.  This may differ from the supplied mime-type for
     *  several reasons.  The implementation may not support the setting of the
     *  mime-type, in which case the default mime-type or one derived from the
     *  content bytes or file extension may be used.  Or a canonical, IANA
     *  mime-type (see <a
     *  href="http://www.iana.org/assignments/media-types/index.html">http://www.iana.org/assignments/media-types/index.html</a>)
     *  may be substituted for a vendor or experimental type.
     *
     *  @param mimeType
     *  @return String
     *  @throws org.osid.filing.FilingException
     */
    public String updateMimeType(String mimeType) throws org.osid.filing.FilingException {
        logger.logMethod(mimeType);
        if (mimeType == null) {
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.NULL_ARGUMENT);
        }
        this.mimeType = mimeType;
        logger.logTrace("new mimeType of " + this.file.getPath() + " is " + mimeType);
        return (mimeType);
    }

    /**
     *  Tests whether the Manager Owner may read this CabinetEntry.
     *
     *  @return <code>true</code> if and only if this CabinetEntry can be read
     *          by the Manager Owner, <code>false</code> otherwise
     *  @throws org.osid.filing.FilingException
     */
    public boolean isReadable() throws org.osid.filing.FilingException {
        logger.logMethod();
        boolean b;
        try {
            b = this.file.canRead();
        } catch (SecurityException se) {
            logger.logError("unable to access " + this.file.getPath(), se);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.PERMISSION_DENIED);
        }
        logger.logTrace("isReadable for " + this.file.getPath() + " is " + b);
        return (b);
    }

    /**
     *  Tests whether the Manager Owner may modify this CabinetEntry.
     *
     *  @return <code>true</code> if and only if the Manager Owner is allowed to
     *          write to this CabinetEntry, <code>false</code> otherwise.
     *  @throws org.osid.filing.FilingException
     */
    public boolean isWritable() throws org.osid.filing.FilingException {
        logger.logMethod();
        boolean b;
        try {
            b = this.file.canWrite();
        } catch (SecurityException se) {
            logger.logError("unable to access " + this.file.getPath(), se);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.PERMISSION_DENIED);
        }
        logger.logTrace("isWritable for " + this.file.getPath() + " is " + b);
        return (b);
    }

    /**
     *  Marks this ByteStore so that only read operations are allowed. After
     *  invoking this method this ByteStore is guaranteed not to change until
     *  it is either deleted or marked to allow write access. Note that whether
     *  or not a read-only ByteStore may be deleted depends upon the file
     *  system underlying the implementation.
     *
     *  @throws org.osid.filing.FilingException
     */
    public void updateReadOnly() throws org.osid.filing.FilingException {
        logger.logMethod();
        try {
            this.file.setReadOnly();
        } catch (SecurityException se) {
            logger.logError("unable to access " + this.file.getPath(), se);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.PERMISSION_DENIED);
        }
        logger.logTrace(this.file.getPath() + " is read only");
        return;
    }

    /**
     *  Marks this Cabinet so that write operations are allowed.
     *
     *  @throws org.osid.filing.FilingException
     */
    public void updateWritable() throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
    }

    /**
     *  Returns the Digest of this ByteStore using the specified algorithm used,
     *  such as md5 or crc.
     *
     *  @param algorithmType digestAlgorithmType selected from possible
     *         implementation digest algorithm types.
     *  @return String digest or null if digest is not supported for this
     *          ByteStore.
     *  @throws org.osid.filing.FilingException
     */
    public String getDigest(org.osid.shared.Type algorithmType) throws org.osid.filing.FilingException {
        logger.logMethod(algorithmType);
        if (algorithmType == null) {
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.NULL_ARGUMENT);
        }
        if (!algorithmType.isEqual(FilingType.MD5_DIGEST.getType())) {
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNKNOWN_TYPE);
        }
        String hash;
        try {
            java.io.InputStream is = new java.io.FileInputStream(this.file);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] b = new byte[10240];
            int count;
            while ((count = is.read(b)) > -1) {
                md.update(b, 0, count);
            }
            hash = new String(md.digest());
        } catch (Exception e) {
            logger.logError("unable to get md5 hash", e);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.IO_ERROR);
        }
        logger.logTrace("md5 hash of " + this.file.getPath() + " is " + hash);
        return (hash);
    }

    /**
     *  Returns the Digest algorithm types supported by the implementation, such
     *  as md5 or crc.
     *
     *  @return org.osid.shared.TypeIterator the digest algorithm types
     *          supported by this implementation.
     *  @throws org.osid.filing.FilingException
     */
    public org.osid.shared.TypeIterator getDigestAlgorithmTypes() throws org.osid.filing.FilingException {
        logger.logMethod();
        return (FilingType.getDigestAlgorithmTypes());
    }

    /**
     *  Reads the data.
     *
     *  @param version of the ByteStore to use which was current at this date
     *         (the number of milliseconds since January 1, 1970, 00:00:00
     *         GMT).
     *  @return org.osid.shared.ByteValueIterator
     *  @throws org.osid.filing.FilingException
     */
    public org.osid.shared.ByteValueIterator read(long version) throws org.osid.filing.FilingException {
        logger.logMethod(version);
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(this.file);
            return (new ByteValueIterator(fis));
        } catch (java.io.IOException ie) {
            logger.logError("error reading file " + this.file.getPath(), ie);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.OPERATION_FAILED);
        }
    }

    /**
     *  Writes b.length bytes to this ByteStore.
     *
     *  @param b the byte array to write
     *  @throws org.osid.filing.FilingException
     */
    public void write(byte[] b) throws org.osid.filing.FilingException {
        logger.logMethod(b);
        if (!isWritable()) {
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.OPERATION_FAILED);
        }
        try {
            if (this.outputStream == null) {
                this.outputStream = new java.io.FileOutputStream(this.file);
            }
            this.outputStream.write(b);
        } catch (java.io.IOException ie) {
            logger.logError("unable to write to " + this.file.getPath(), ie);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.IO_ERROR);
        }
        return;
    }

    /**
     *  Writes bytes from the specified byte array starting at offset in this
     *  ByteStore.
     *
     *  @param b bytes to write
     *  @param offset offset into file
     *  @throws org.osid.filing.FilingException
     */
    public void writeBytesAtOffset(byte[] b, long offset) throws org.osid.filing.FilingException {
        logger.logMethod(b, offset);
        if (!isWritable()) {
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.OPERATION_FAILED);
        }
        try {
            if (this.outputStream == null) {
                this.outputStream = new java.io.FileOutputStream(this.file);
            }
            this.outputStream.write(b, (int) offset, b.length);
        } catch (java.io.IOException ie) {
            logger.logError("unable to write to " + this.file.getPath(), ie);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.IO_ERROR);
        }
        return;
    }

    /**
     *  Writes the specified byte to this ByteStore.
     *
     *  @param b the byte to write
     *  @throws org.osid.filing.FilingException
     */
    public void writeByte(int b) throws org.osid.filing.FilingException {
        logger.logMethod(b);
        if (!isWritable()) {
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.OPERATION_FAILED);
        }
        try {
            if (this.outputStream == null) {
                this.outputStream = new java.io.FileOutputStream(this.file);
            }
            this.outputStream.write(b);
        } catch (java.io.IOException ie) {
            logger.logError("unable to write to " + this.file.getPath(), ie);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.IO_ERROR);
        }
        return;
    }

    /**
     *  Closes this Output Object and releases any system resources associated
     *  with it.
     *
     *  @throws org.osid.filing.FilingException
     */
    public void commit() throws org.osid.filing.FilingException {
        logger.logMethod();
        if (this.outputStream != null) {
            try {
                this.outputStream.close();
            } catch (java.io.IOException ie) {
                logger.logError("unable to close " + this.file.getPath(), ie);
                throw new org.osid.filing.FilingException(org.osid.filing.FilingException.IO_ERROR);
            } finally {
                this.outputStream = null;
            }
        }
        return;
    }
}
