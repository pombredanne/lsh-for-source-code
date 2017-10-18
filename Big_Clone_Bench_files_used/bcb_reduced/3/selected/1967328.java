package edu.mit.osidimpl.filing.repository;

import edu.mit.osidimpl.manager.*;
import edu.mit.osidimpl.filing.shared.*;

/**
 *  <p>
 *  Implements a ByteStore for the Filing implementation for covering the
 *  repository OSID. The ByteStore is a type of CabinetEntry that can be
 *  read from or written to. It is not recommended to perform both
 *  read and write operations on the same ByteStore or to have multiple
 *  ByteStore objects open for writing.
 *  </p><p>
 *  CVS $Id: ByteStore.java,v 1.2 2006/10/06 18:13:04 tom Exp $
 *  </p>
 *  
 *  @author  Tom Coppeto
 *  @version $OSID: 2.0$ $Revision: 1.2 $
 */
public class ByteStore implements org.osid.filing.ByteStore {

    private Cabinet cabinet;

    FilingManager mgr;

    OsidLogger logger;

    private org.osid.repository.Asset asset;

    private boolean appendOnly;

    private org.osid.shared.Type algorithmType;

    private java.io.OutputStream outputStream;

    protected ByteStore(Cabinet cabinet, org.osid.repository.Asset asset) {
        this.cabinet = cabinet;
        this.mgr = cabinet.mgr;
        this.logger = cabinet.logger;
        this.asset = asset;
        updateStuff();
        logger.logTrace("created new byte store");
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
        if (this.asset instanceof edu.mit.osidimpl.repository.cache.Asset) {
            return (((edu.mit.osidimpl.repository.cache.Asset) this.asset).getLength());
        } else {
            return (-1);
        }
    }

    /**
     *  Tests whether the Manager Owner may append to this ByteStore.
     *
     *  @return <code>true</code> if and only if the Manager Owner is allowed 
     *          to append to this ByteStore, <code>false</code> otherwise.
     *  @throws org.osid.filing.FilingException
     */
    public boolean canAppend() throws org.osid.filing.FilingException {
        logger.logMethod();
        return (false);
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
        if (this.asset instanceof edu.mit.osidimpl.repository.cache.Asset) {
            return (((edu.mit.osidimpl.repository.cache.Asset) this.asset).getMimeType());
        } else {
            return ("");
        }
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
        return (true);
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
        return (false);
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
            org.osid.shared.ByteValueIterator bvi = read(0);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] b = new byte[1];
            while (bvi.hasNextByteValue()) {
                b[0] = bvi.nextByteValue();
                md.update(b, 0, 1);
            }
            hash = new String(md.digest());
        } catch (Exception e) {
            logger.logError("unable to get md5 hash", e);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.IO_ERROR);
        } catch (org.osid.shared.SharedException se) {
            logger.logError("unable to get data", se);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.IO_ERROR);
        }
        logger.logTrace("md5 hash is " + hash);
        return (hash);
    }

    /**
     *  Returns the Digest algorithm types supported by the implementation,
     *  such as md5 or crc.
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
            Object object = this.asset.getContent();
            if (object instanceof String) {
                String s = (String) object;
                if (s.startsWith("http://")) {
                    java.net.URL url = new java.net.URL(s);
                    return new ByteValueIterator(url.openStream());
                } else {
                    logger.logError("unknown asset content");
                    throw new org.osid.filing.FilingException(org.osid.filing.FilingException.OPERATION_FAILED);
                }
            } else if (object instanceof edu.mit.osidimpl.repository.shared.SerializableInputStream) {
                return new ByteValueIterator(((edu.mit.osidimpl.repository.shared.SerializableInputStream) object).getInputStream());
            } else {
                logger.logError("unknown asset content");
                throw new org.osid.filing.FilingException(org.osid.filing.FilingException.OPERATION_FAILED);
            }
        } catch (java.io.IOException ie) {
            logger.logError("cannot get data", ie);
            throw new org.osid.filing.FilingException(org.osid.filing.FilingException.OPERATION_FAILED);
        } catch (org.osid.repository.RepositoryException re) {
            logger.logError("cannot get asset content", re);
            throw this.mgr.getException(re);
        }
    }

    /**
     *  Writes b.length bytes to this ByteStore.
     *
     *  @param b the byte array to write
     *  @throws org.osid.filing.FilingException
     */
    public void write(byte[] b) throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
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
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
    }

    /**
     *  Writes the specified byte to this ByteStore.
     *
     *  @param b the byte to write
     *  @throws org.osid.filing.FilingException
     */
    public void writeByte(int b) throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
    }

    /**
     *  Closes this Output Object and releases any system resources associated
     *  with it.
     *
     *  @throws org.osid.filing.FilingException
     */
    public void commit() throws org.osid.filing.FilingException {
        logger.logMethod();
        return;
    }

    /**
     *  Returns true or false depending on whether this CabinetEntry exists in
     *  the file system.
     *
     *  @return boolean true if CabinetEntry exists, false otherwise
     *  @throws org.osid.filing.FilingException An exception with one of the
     *         following messages defined in org.osid.filing.FilingException
     *         may be thrown: {@link
     *         org.osid.filing.FilingException#PERMISSION_DENIED
     *         PERMISSION_DENIED}
     */
    public boolean exists() throws org.osid.filing.FilingException {
        logger.logMethod();
        return (true);
    }

    /**
     *  Returns the Cabinet in which this CabinetEntry is an entry, or null if
     *  it has no parent (for example is the root Cabinet).
     *
     *  @return Cabinet the parent Cabinet of this entry, or null if it has no
     *          parent (e.g. is the root Cabinet)
     *  @throws org.osid.filing.FilingException
     */
    public org.osid.filing.Cabinet getParent() throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        return (this.cabinet);
    }

    /**
     *  Get all the Property Types for CabinetEntry.
     *
     *  @return org.osid.shared.TypeIterator
     *  @throws org.osid.filing.FilingException
     */
    public org.osid.shared.TypeIterator getPropertyTypes() throws org.osid.filing.FilingException {
        logger.logMethod();
        return (new TypeIterator(new java.util.Vector()));
    }

    /**
     *  Get the Properties of this Type associated with this CabinetEntry.
     *
     *  @return org.osid.shared.Properties
     *  @throws org.osid.filing.FilingException An exception with one of the
     *          following messages defined in org.osid.filing.FilingException
     *          may be thrown: {@link
     *          org.osid.filing.FilingException#UNKNOWN_TYPE UNKNOWN_TYPE}
     */
    public org.osid.shared.Properties getPropertiesByType(org.osid.shared.Type propertiesType) throws org.osid.filing.FilingException {
        logger.logMethod(propertiesType);
        return (new Properties(new java.util.HashMap()));
    }

    /**
     *  Get the Properties associated with this CabinetEntry.
     *
     *  @return PropertiesIterator
     *  @throws org.osid.filing.FilingException
     */
    public org.osid.shared.PropertiesIterator getProperties() throws org.osid.filing.FilingException {
        logger.logMethod();
        return (new PropertiesIterator(new java.util.ArrayList()));
    }

    /**
     *  Return the name of this CabinetEntry in its parent Cabinet.
     *
     *  @return name
     *  @throws org.osid.filing.FilingException
     */
    public String getDisplayName() throws org.osid.filing.FilingException {
        logger.logMethod();
        try {
            return (this.asset.getDisplayName());
        } catch (org.osid.repository.RepositoryException re) {
            throw this.mgr.getException(re);
        }
    }

    /**
     * Get Id of this CabinetEntry
     *
     * @return Id
     *
     * @throws org.osid.filing.FilingException
     */
    public org.osid.shared.Id getId() throws org.osid.filing.FilingException {
        logger.logMethod();
        try {
            return (this.asset.getId());
        } catch (org.osid.repository.RepositoryException re) {
            throw this.mgr.getException(re);
        }
    }

    /**
     *  Returns when this Cabinet was last modified.
     *
     *  @return long The time this cabinet was last modified (the number of
     *          milliseconds since January 1, 1970, 00:00:00 GMT)
     *  @throws org.osid.filing.FilingException An exception with one of the
     *         following messages defined in org.osid.filing.FilingException
     *         may be thrown: 
     *         {@link org.osid.filing.FilingException#PERMISSION_DENIED
     *         PERMISSION_DENIED}
     */
    public long getLastModifiedTime() throws org.osid.filing.FilingException {
        logger.logMethod();
        if (this.asset instanceof edu.mit.osidimpl.repository.cache.Asset) {
            return (((edu.mit.osidimpl.repository.cache.Asset) this.asset).getLastModified());
        } else {
            return (0);
        }
    }

    /**
     *  Returns all the times that this Cabinet was modified.
     *
     *  @return org.osid.shared.LongValueInterator The times this cabinet was
     *          modified
     *  @throws org.osid.filing.FilingException An exception with one of the
     *          following messages defined in org.osid.filing.FilingException
     *          may be thrown:  
     *          {@link org.osid.filing.FilingException#UNIMPLEMENTED
     *          UNIMPLEMENTED}
     */
    public org.osid.shared.LongValueIterator getAllModifiedTimes() throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
    }

    /**
     *  Sets the last-modified time to the current time for this CabinetEntry.
     *
     *  @throws org.osid.filing.FilingException An exception with one of the
     *          following messages defined in org.osid.filing.FilingException
     *          may be thrown: {@link
     *          org.osid.filing.FilingException#PERMISSION_DENIED
     *          PERMISSION_DENIED}, {@link
     *          org.osid.filing.FilingException#IO_ERROR IO_ERROR}
     */
    public void touch() throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
    }

    /**
     *  Returns when this Cabinet was last accessed. Not all implementations
     *  will record last access times accurately, due to caching and
     *  performance.  The value returned will be at least the last modified
     *  time, the actual time when a read was performed may be later.
     *
     *  @return long The time the file was last accessed (the number of
     *          milliseconds since January 1, 1970, 00:00:00 GMT).
     *  @throws org.osid.filing.FilingException An exception with one of the
     *          following messages defined in org.osid.filing.FilingException
     *          may be thrown: 
     *          {@link org.osid.filing.FilingException#UNIMPLEMENTED
     *          UNIMPLEMENTED}
     */
    public long getLastAccessedTime() throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
    }

    /**
     *  Returns when this CabinetEntry was created. Not all implementations 
     *  will record the time of creation accurately.  The value returned will
     *  be at least the last modified time, the actual creation time may be 
     *  earlier.
     *
     *  @return long The time the file was created (the number of milliseconds
     *          since January 1, 1970, 00:00:00 GMT).
     *  @throws org.osid.filing.FilingException An exception with one of the
     *          following messages defined in org.osid.filing.FilingException
     *          may be thrown: 
     *          {@link org.osid.filing.FilingException#UNIMPLEMENTED
     *          UNIMPLEMENTED}
     */
    public long getCreatedTime() throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
    }

    /**
     *  Return the Id of the Agent that owns this CabinetEntry.
     *
     *  @return org.osid.shared.Id
     *  @throws org.osid.filing.FilingException An exception with one of the
     *          following messages defined in org.osid.filing.FilingException
     *          may be thrown: {@link
     *          org.osid.filing.FilingException#UNIMPLEMENTED UNIMPLEMENTED}
     */
    public org.osid.shared.Id getCabinetEntryAgentId() throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
    }

    /**
     *  Change the name of this CabinetEntry to <code>displayName</code>
     *
     *  @param displayName the new name for the entry
     *  @throws org.osid.filing.FilingException
     */
    public void updateDisplayName(String displayName) throws org.osid.filing.FilingException {
        logger.logMethod("UNIMPLEMENTED");
        throw new org.osid.filing.FilingException(org.osid.filing.FilingException.UNIMPLEMENTED);
    }

    protected void updateStuff() {
        Object object;
        try {
            object = this.asset.getContent();
        } catch (org.osid.repository.RepositoryException re) {
            logger.logWarning("cannot get asset content", re);
            return;
        }
        return;
    }
}
