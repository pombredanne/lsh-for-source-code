package org.ccnx.ccn.profiles;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.ContentVerifier;
import org.ccnx.ccn.config.SystemConfiguration;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.impl.support.Tuple;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Exclude;
import org.ccnx.ccn.protocol.ExcludeAny;
import org.ccnx.ccn.protocol.ExcludeComponent;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.PublisherID;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;

/**
 * Versions, when present, usually occupy the penultimate component of the CCN name, 
 * not counting the digest component. A name may actually incorporate multiple
 * versions, where the rightmost version is the version of "this" object, if it
 * has one, and previous (parent) versions are the versions of the objects of
 * which this object is a part. The most common location of a version, if present,
 * is in the next to last component of the name, where the last component is a
 * segment number (which is generally always present; versions themselves are
 * optional). More complicated segmentation profiles occur, where a versioned
 * object has components that are structured and named in ways other than segments --
 * and may themselves have individual versions (e.g. if the components of such
 * a composite object are written as CCNNetworkObjects and automatically pick
 * up an (unnecessary) version in their own right). Versioning operations therefore
 * take context from their caller about where to expect to find a version,
 * and attempt to ignore other versions in the name.
 * 
 * Versions may be chosen based on time.
 * The first byte of the version component is 0xFD. The remaining bytes are a
 * big-endian binary number. If based on time they are expressed in units of
 * 2**(-12) seconds since the start of Unix time, using the minimum number of
 * bytes. The time portion will thus take 48 bits until quite a few centuries
 * from now (Sun, 20 Aug 4147 07:32:16 GMT). With 12 bits of precision, it allows 
 * for sub-millisecond resolution. The client generating the version stamp 
 * should try to avoid using a stamp earlier than (or the same as) any 
 * version of the file, to the extent that it knows about it. It should 
 * also avoid generating stamps that are unreasonably far in the future.
 * 
 * Get latest version is going to exclude [B, 0xFD00FFFFFFFFFF, 0xFE000000000000,B],
 * so you need to be sure to use version numbers in those bounds.
 */
public class VersioningProfile implements CCNProfile {

    public static final byte VERSION_MARKER = (byte) 0xFD;

    public static final byte FF = (byte) 0xFF;

    public static final byte O1 = (byte) 0x01;

    public static final byte OO = (byte) 0x00;

    public static final byte[] FIRST_VERSION_MARKER = new byte[] { VERSION_MARKER };

    public static final byte[] LAST_VERSION_MARKER = new byte[] { VERSION_MARKER + 1, OO, OO, OO, OO, OO, OO };

    public static final byte[] MIN_VERSION_MARKER = new byte[] { VERSION_MARKER, O1, OO, OO, OO, OO, OO };

    public static final byte[] MAX_VERSION_MARKER = new byte[] { VERSION_MARKER, FF, FF, FF, FF, FF, FF };

    public static final byte[] BOTTOM_EXCLUDE_VERSION_MARKER = MIN_VERSION_MARKER;

    public static final byte[] TOP_EXCLUDE_VERSION_MARKER = LAST_VERSION_MARKER;

    /**
	 * Add a version field to a ContentName.
	 * @version should be a CCNTime toBinaryTimeAsLong() not getTime()
	 * @return ContentName with a version appended. Does not affect previous versions.
	 */
    public static ContentName addVersion(ContentName name, long version) {
        byte[] vcomp = null;
        if (0 == version) {
            vcomp = FIRST_VERSION_MARKER;
        } else {
            BigInteger bi = BigInteger.valueOf(version);
            byte[] varr = bi.toByteArray();
            int start = 0;
            int length = varr.length;
            if (0 == varr[0] && varr.length > 1) {
                start = 1;
                length--;
            }
            vcomp = new byte[length + 1];
            vcomp[0] = VERSION_MARKER;
            System.arraycopy(varr, start, vcomp, 1, length);
        }
        return new ContentName(name, vcomp);
    }

    /**
	 * Converts a timestamp into a fixed point representation, with 12 bits in the fractional
	 * component, and adds this to the ContentName as a version field. The timestamp is rounded
	 * to the nearest value in the fixed point representation.
	 * <p>
	 * This allows versions to be recorded as a timestamp with a 1/4096 second accuracy.
	 * <p>
	 * Get latest version is going to exclude [B, 0xFD00FFFFFFFFFF, 0xFE000000000000,B],
	 * so you need to be sure to use version numbers in those bounds.
	 * <p>
	 * @see #addVersion(ContentName, long)
	 */
    public static ContentName addVersion(ContentName name, CCNTime version) {
        if (null == version) throw new IllegalArgumentException("Version cannot be null!");
        byte[] vcomp = timeToVersionComponent(version);
        return new ContentName(name, vcomp);
    }

    /**
	 * Add a version field based on the current time, accurate to 1/4096 second.
	 * <p>
	 * Get latest version is going to exclude [B, 0xFD00FFFFFFFFFF, 0xFE000000000000,B],
	 * so you need to be sure to use version numbers in those bounds.
	 * <p>
	 * @see #addVersion(ContentName, CCNTime)
	 */
    public static ContentName addVersion(ContentName name) {
        return addVersion(name, CCNTime.now());
    }

    public static byte[] timeToVersionComponent(CCNTime version) {
        byte[] varr = version.toBinaryTime();
        byte[] vcomp = new byte[varr.length + 1];
        vcomp[0] = VERSION_MARKER;
        System.arraycopy(varr, 0, vcomp, 1, varr.length);
        return vcomp;
    }

    public static String printAsVersionComponent(CCNTime version) {
        byte[] vcomp = timeToVersionComponent(version);
        return ContentName.componentPrintURI(vcomp);
    }

    /**
	 * Adds a version to a ContentName; if there is a terminal version there already,
	 * first removes it.
	 */
    public static ContentName updateVersion(ContentName name, long version) {
        return addVersion(cutTerminalVersion(name).first(), version);
    }

    /**
	 * Adds a version to a ContentName; if there is a terminal version there already,
	 * first removes it.
	 */
    public static ContentName updateVersion(ContentName name, CCNTime version) {
        return addVersion(cutTerminalVersion(name).first(), version);
    }

    /**
	 * Add updates the version field based on the current time, accurate to 1/4096 second.
	 * @see #updateVersion(ContentName, Timestamp)
	 */
    public static ContentName updateVersion(ContentName name) {
        return updateVersion(name, CCNTime.now());
    }

    /**
	 * Finds the last component that looks like a version in name.
	 * @param name
	 * @return the index of the last version component in the name, or -1 if there is no version
	 *					component in the name
	 */
    public static int findLastVersionComponent(ContentName name) {
        int i = name.count();
        for (; i >= 0; i--) if (isVersionComponent(name.component(i))) return i;
        return -1;
    }

    /**
	 * Checks to see if this name has a validly formatted version field anywhere in it.
	 */
    public static boolean containsVersion(ContentName name) {
        return findLastVersionComponent(name) != -1;
    }

    /**
	 * Checks to see if this name has a validly formatted version field either in final
	 * component or in next to last component with final component being a segment marker.
	 */
    public static boolean hasTerminalVersion(ContentName name) {
        if ((name.count() > 0) && ((isVersionComponent(name.lastComponent()) || ((name.count() > 1) && SegmentationProfile.isSegment(name) && isVersionComponent(name.component(name.count() - 2)))))) {
            return true;
        }
        return false;
    }

    /**
	 * Check a name component to see if it is a valid version field
	 */
    public static boolean isVersionComponent(byte[] nameComponent) {
        return (null != nameComponent) && (0 != nameComponent.length) && (VERSION_MARKER == nameComponent[0]) && ((nameComponent.length == 1) || (nameComponent[1] != 0));
    }

    public static boolean isBaseVersionComponent(byte[] nameComponent) {
        return (isVersionComponent(nameComponent) && (1 == nameComponent.length));
    }

    /**
	 * Remove a terminal version marker (one that is either the last component of name, or
	 * the next to last component of name followed by a segment marker) if one exists, otherwise
	 * return name as it was passed in.
	 * @param name
	 * @return
	 */
    public static Tuple<ContentName, byte[]> cutTerminalVersion(ContentName name) {
        if (name.count() > 0) {
            if (isVersionComponent(name.lastComponent())) {
                return new Tuple<ContentName, byte[]>(name.parent(), name.lastComponent());
            } else if ((name.count() > 2) && SegmentationProfile.isSegment(name) && isVersionComponent(name.component(name.count() - 2))) {
                return new Tuple<ContentName, byte[]>(name.cut(name.count() - 2), name.component(name.count() - 2));
            }
        }
        return new Tuple<ContentName, byte[]>(name, null);
    }

    /**
	 * Take a name which may have one or more version components in it,
	 * and strips the last one and all following components. If no version components
	 * present, returns the name as handed in.
	 */
    public static ContentName cutLastVersion(ContentName name) {
        int offset = findLastVersionComponent(name);
        return (offset == -1) ? name : new ContentName(offset, name.components());
    }

    /**
	 * Function to get the version field as a long.  Starts from the end and checks each name component for the version marker.
	 * @param name
	 * @return long
	 * @throws VersionMissingException
	 */
    public static long getLastVersionAsLong(ContentName name) throws VersionMissingException {
        int i = findLastVersionComponent(name);
        if (i == -1) throw new VersionMissingException();
        return getVersionComponentAsLong(name.component(i));
    }

    public static byte[] getLastVersionComponent(ContentName name) throws VersionMissingException {
        int i = findLastVersionComponent(name);
        if (i == -1) throw new VersionMissingException();
        return name.component(i);
    }

    public static long getVersionComponentAsLong(byte[] versionComponent) {
        byte[] versionData = new byte[versionComponent.length - 1];
        System.arraycopy(versionComponent, 1, versionData, 0, versionComponent.length - 1);
        if (versionData.length == 0) return 0;
        return new BigInteger(1, versionData).longValue();
    }

    public static CCNTime getVersionComponentAsTimestamp(byte[] versionComponent) {
        if (null == versionComponent) return null;
        return versionLongToTimestamp(getVersionComponentAsLong(versionComponent));
    }

    /**
	 * Extract the version from this name as a Timestamp.
	 * @throws VersionMissingException 
	 */
    public static CCNTime getLastVersionAsTimestamp(ContentName name) throws VersionMissingException {
        long time = getLastVersionAsLong(name);
        return CCNTime.fromBinaryTimeAsLong(time);
    }

    /**
	 * Returns null if no version, otherwise returns the last version in the name. 
	 * @param name
	 * @return
	 */
    public static CCNTime getLastVersionAsTimestampIfVersioned(ContentName name) {
        int versionComponent = findLastVersionComponent(name);
        if (versionComponent < 0) return null;
        return getVersionComponentAsTimestamp(name.component(versionComponent));
    }

    public static CCNTime getTerminalVersionAsTimestampIfVersioned(ContentName name) {
        if (!hasTerminalVersion(name)) return null;
        int versionComponent = findLastVersionComponent(name);
        if (versionComponent < 0) return null;
        return getVersionComponentAsTimestamp(name.component(versionComponent));
    }

    public static CCNTime versionLongToTimestamp(long version) {
        return CCNTime.fromBinaryTimeAsLong(version);
    }

    /**
	 * Control whether versions start at 0 or 1.
	 * @return
	 */
    public static final int baseVersion() {
        return 0;
    }

    /**
	 * Compares terminal version (versions at the end of, or followed by only a segment
	 * marker) of a name to a given timestamp.
	 * @param left
	 * @param right
	 * @return
	 */
    public static int compareVersions(CCNTime left, ContentName right) {
        if (!hasTerminalVersion(right)) {
            throw new IllegalArgumentException("Both names to compare must be versioned!");
        }
        try {
            return left.compareTo(getLastVersionAsTimestamp(right));
        } catch (VersionMissingException e) {
            throw new IllegalArgumentException("Name that isVersioned returns true for throws VersionMissingException!: " + right);
        }
    }

    public static int compareVersionComponents(byte[] left, byte[] right) throws VersionMissingException {
        if ((null == left) || (null == right)) throw new VersionMissingException("Must compare two versions!");
        return getVersionComponentAsTimestamp(left).compareTo(getVersionComponentAsTimestamp(right));
    }

    /**
	 * See if version is a version of parent (not commutative).
	 * @return
	 */
    public static boolean isVersionOf(ContentName version, ContentName parent) {
        Tuple<ContentName, byte[]> versionParts = cutTerminalVersion(version);
        if (!parent.equals(versionParts.first())) {
            return false;
        }
        if (null == versionParts.second()) return false;
        return true;
    }

    /**
	 * This compares two names, with terminal versions, and determines whether one is later than the other.
	 * @param laterVersion
	 * @param earlierVersion
	 * @return
	 * @throws VersionMissingException
	 */
    public static boolean isLaterVersionOf(ContentName laterVersion, ContentName earlierVersion) throws VersionMissingException {
        Log.warning("SEMANTICS CHANGED: if experiencing unexpected behavior, check to see if you want to call isLaterVerisionOf or startsWithLaterVersionOf");
        Tuple<ContentName, byte[]> earlierVersionParts = cutTerminalVersion(earlierVersion);
        Tuple<ContentName, byte[]> laterVersionParts = cutTerminalVersion(laterVersion);
        if (!laterVersionParts.first().equals(earlierVersionParts.first())) {
            return false;
        }
        return (compareVersionComponents(laterVersionParts.second(), earlierVersionParts.second()) > 0);
    }

    /**
	 * Finds out if you have a versioned name, and a ContentObject that might have a versioned name which is 
	 * a later version of the given name, even if that CO name might not refer to a segment of the original name.
	 * For example, given a name /parc/foo.txt/<version1> or /parc/foo.txt/<version1>/<segment>
	 * and /parc/foo.txt/<version2>/<stuff>, return true, whether <stuff> is a segment marker, a whole
	 * bunch of repo write information, or whatever. 
	 * @param newName Will check to see if this name begins with something which is a later version of previousVersion.
	 * @param previousVersion The name to compare to, must have a terminal version or be unversioned.
	 * @return
	 */
    public static boolean startsWithLaterVersionOf(ContentName newName, ContentName previousVersion) {
        Tuple<ContentName, byte[]> previousVersionParts = cutTerminalVersion(previousVersion);
        if (!previousVersionParts.first().isPrefixOf(newName)) return false;
        if (null == previousVersionParts.second()) {
            return ((newName.count() > previousVersionParts.first().count()) && VersioningProfile.isVersionComponent(newName.component(previousVersionParts.first().count())));
        }
        try {
            return (compareVersionComponents(newName.component(previousVersionParts.first().count()), previousVersionParts.second()) > 0);
        } catch (VersionMissingException e) {
            return false;
        }
    }

    public static int compareTerminalVersions(ContentName laterVersion, ContentName earlierVersion) throws VersionMissingException {
        Tuple<ContentName, byte[]> earlierVersionParts = cutTerminalVersion(earlierVersion);
        Tuple<ContentName, byte[]> laterVersionParts = cutTerminalVersion(laterVersion);
        if (!laterVersionParts.first().equals(earlierVersionParts.first())) {
            throw new IllegalArgumentException("Names not versions of the same name!");
        }
        return (compareVersionComponents(laterVersionParts.second(), earlierVersionParts.second()));
    }

    /**
	 * Builds an Exclude filter that excludes components before or @ start, and components after
	 * the last valid version.
	 * @param startingVersionComponent The latest version component we know about. Can be null or
	 * 			VersioningProfile.isBaseVersionComponent() == true to indicate that we want to start
	 * 			from 0 (we don't have a known version we're trying to update). This exclude filter will
	 * 			find versions *after* the version represented in startingVersionComponent.
	 * @return An exclude filter.
	 */
    public static Exclude acceptVersions(byte[] startingVersionComponent) {
        byte[] start = null;
        if ((null == startingVersionComponent) || VersioningProfile.isBaseVersionComponent(startingVersionComponent)) {
            start = new byte[] { VersioningProfile.VERSION_MARKER, VersioningProfile.OO, VersioningProfile.FF, VersioningProfile.FF, VersioningProfile.FF, VersioningProfile.FF, VersioningProfile.FF };
        } else {
            start = startingVersionComponent;
        }
        ArrayList<Exclude.Element> ees = new ArrayList<Exclude.Element>();
        ees.add(new ExcludeAny());
        ees.add(new ExcludeComponent(start));
        ees.add(new ExcludeComponent(LAST_VERSION_MARKER));
        ees.add(new ExcludeAny());
        return new Exclude(ees);
    }

    /**
	 * Generate an interest that will find the leftmost child of the latest version. It
	 * will ensure that the next to last segment is a version, and the last segment (excluding
	 * digest) is the leftmost child available. But it can't guarantee that the latter is
	 * a segment. Because most data is segmented, length constraints will make it very
	 * likely, however.
	 * @param startingVersion
	 * @return
	 */
    public static Interest firstBlockLatestVersionInterest(ContentName startingVersion, PublisherPublicKeyDigest publisher) {
        return latestVersionInterest(startingVersion, 3, publisher);
    }

    /**
	 * Generate an interest that will find a descendant of the latest version of startingVersion,
	 * after any existing version component. If additionalNameComponents is non-null, it will
	 * find a descendant with exactly that many name components after the version (including
	 * the digest). The latest version is the rightmost child of the desired prefix, however,
	 * this interest will find leftmost descendants of that rightmost child. With appropriate
	 * length limitations, can be used to find segments of the latest version (though that
	 * will work more effectively with appropriate segment numbering).
	 */
    public static Interest latestVersionInterest(ContentName startingVersion, Integer additionalNameComponents, PublisherPublicKeyDigest publisher) {
        if (hasTerminalVersion(startingVersion)) {
            startingVersion = SegmentationProfile.segmentRoot(startingVersion);
        } else {
            ContentName firstVersionName = addVersion(startingVersion, baseVersion());
            startingVersion = firstVersionName;
        }
        byte[] versionComponent = startingVersion.lastComponent();
        Interest constructedInterest = Interest.last(startingVersion, acceptVersions(versionComponent), startingVersion.count() - 1, additionalNameComponents, additionalNameComponents, null);
        if (null != publisher) {
            constructedInterest.publisherID(new PublisherID(publisher));
        }
        return constructedInterest;
    }

    /**
	 * Function to (best effort) get the latest version. There may be newer versions available
	 * if you ask again passing in the version found (i.e. each response will be the latest version
	 * a given responder knows about. Further queries will move past that responder to other responders,
	 * who may have newer information.)
	 *  
	 * @param name If the name ends in a version then this method explicitly looks for a newer version
	 * than that, and will time out if no such later version exists. If the name does not end in a 
	 * version then this call just looks for the latest version.
	 * @param publisher Currently unused, will limit query to a specific publisher.
	 * @param timeout  This is the time to wait until you get any response.  If nothing is returned, this method will return null.
	 * @param verifier Used to verify the returned content objects
	 * @param handle CCNHandle used to get the latest version
	 * @return A ContentObject with the latest version, or null if the query timed out. 
	 * @result Returns a matching ContentObject, verified.
	 * @throws IOException
	 */
    public static ContentObject getLatestVersion(ContentName startingVersion, PublisherPublicKeyDigest publisher, long timeout, ContentVerifier verifier, CCNHandle handle) throws IOException {
        return getLatestVersion(startingVersion, publisher, timeout, verifier, handle, null, false);
    }

    /**
	 * Function to (best effort) get the latest version. There may be newer versions available
	 * if you ask again passing in the version found (i.e. each response will be the latest version
	 * a given responder knows about. Further queries will move past that responder to other responders,
	 * who may have newer information.)
	 *  
	 * @param name If the name ends in a version then this method explicitly looks for a newer version
	 * than that, and will time out if no such later version exists. If the name does not end in a 
	 * version then this call just looks for the latest version.
	 * @param publisher Currently unused, will limit query to a specific publisher.
	 * @param timeout  This is the time to wait to retrieve any version.  If nothing is returned, this method will return null.
	 * @param verifier Used to verify the returned content objects.
 	 * @param handle CCNHandle used to get the latest version.
 	 * @param startingSegmentNumber If we are requiring content to be a segment, what segment number
 	 *    do we want. If null, and findASegment is true, uses SegmentationProfile.baseSegment().
 	 * @param findASegment are we requiring returned content to be a segment of this version
	 * @return A ContentObject with the latest version, or null if the query timed out. 
	 * @result Returns a matching ContentObject, verified.
	 * @throws IOException
	 */
    private static ContentObject getLatestVersion(ContentName startingVersion, PublisherPublicKeyDigest publisher, long timeout, ContentVerifier verifier, CCNHandle handle, Long startingSegmentNumber, boolean findASegment) throws IOException {
        if (Log.isLoggable(Level.FINE)) {
            Log.fine("getFirstBlockOfLatestVersion: getting version later than {0} called with timeout: {1}", startingVersion, timeout);
        }
        if (null == verifier) {
            verifier = handle.keyManager().getDefaultVerifier();
        }
        long startTime = System.currentTimeMillis();
        long interestTime = 0;
        long elapsedTime = 0;
        long respondTime;
        long remainingTime = timeout;
        long attemptTimeout = SystemConfiguration.GLV_ATTEMPT_TIMEOUT;
        boolean noTimeout = false;
        if (timeout == SystemConfiguration.NO_TIMEOUT) {
            Log.finest("gLV called with NO_TIMEOUT");
            noTimeout = true;
        } else if (timeout == 0) {
            Log.finest("gLV called with timeout = 0, should just return the first thing we get");
        }
        ContentName prefix = startingVersion;
        if (hasTerminalVersion(prefix)) {
            prefix = startingVersion.parent();
        }
        int versionedLength = prefix.count() + 1;
        ContentObject result = null;
        ContentObject lastResult = null;
        ArrayList<byte[]> excludeList = new ArrayList<byte[]>();
        while ((remainingTime > 0 && elapsedTime < timeout) || (noTimeout || timeout == 0)) {
            Log.finer("gLV timeout: {0} remainingTime: {1} attemptTimeout: {2}", timeout, remainingTime, attemptTimeout);
            lastResult = result;
            Interest getLatestInterest = null;
            if (findASegment) {
                getLatestInterest = firstBlockLatestVersionInterest(startingVersion, publisher);
            } else {
                getLatestInterest = latestVersionInterest(startingVersion, null, publisher);
            }
            if (excludeList.size() > 0) {
                byte[][] e = new byte[excludeList.size()][];
                excludeList.toArray(e);
                getLatestInterest.exclude().add(e);
            }
            Log.finer("timeout {0} startTime: {1} elapsedTime: {2} remainingTime: {3} new elapsedTime = {4}", timeout, startTime, elapsedTime, remainingTime, (System.currentTimeMillis() - startTime));
            interestTime = System.currentTimeMillis();
            long tempT;
            if (noTimeout) {
                tempT = timeout;
            } else if (timeout == 0) {
                tempT = attemptTimeout;
            } else {
                if (remainingTime < timeout - elapsedTime) {
                    tempT = remainingTime;
                } else {
                    tempT = timeout - elapsedTime;
                }
            }
            result = handle.get(getLatestInterest, tempT);
            elapsedTime = System.currentTimeMillis() - startTime;
            respondTime = System.currentTimeMillis() - interestTime;
            if (result == null && respondTime == 0) {
                Log.warning("gLV: handle.get returned null and did not wait the full timeout time for the object (timeout: {0} responseTime: {1}", timeout, respondTime);
                return null;
            }
            remainingTime = timeout - elapsedTime;
            if (Log.isLoggable(Level.FINE)) {
                Log.fine("gLV INTEREST: {0}", getLatestInterest);
                Log.fine("gLV trying handle.get with timeout: {0}", tempT);
                Log.fine("gLVTime sending Interest from gLV at {0} started at: {1}", System.currentTimeMillis(), startTime);
                Log.fine("gLVTime returned from handle.get in {0} ms", respondTime);
                Log.fine("gLV remaining time is now {0} ms", remainingTime);
            }
            if (null != result) {
                if (Log.isLoggable(Level.INFO)) Log.info("gLV getLatestVersion: retrieved latest version object {0} type: {1}", result.name(), result.signedInfo().getTypeName());
                if (!verifier.verify(result)) {
                    Log.fine("gLV result did not verify, trying to find a verifiable answer");
                    excludeList = addVersionToExcludes(excludeList, result.name());
                    Interest retry = new Interest(result.name(), publisher);
                    boolean verifyDone = false;
                    while (!verifyDone) {
                        if (retry.exclude() == null) retry.exclude(new Exclude());
                        retry.exclude().add(new byte[][] { result.digest() });
                        if (Log.isLoggable(Level.FINE)) {
                            Log.fine("gLV result did not verify!  doing retry!! {0}", retry);
                            Log.fine("gLVTime sending retry interest at {0}", System.currentTimeMillis());
                        }
                        result = handle.get(retry, attemptTimeout);
                        if (result != null) {
                            if (Log.isLoggable(Level.FINE)) Log.fine("gLV we got something back: {0}", result.name());
                            if (verifier.verify(result)) {
                                Log.fine("gLV the returned answer verifies");
                                verifyDone = true;
                            } else {
                                Log.fine("gLV this answer did not verify either...  try again");
                            }
                        } else {
                            Log.fine("gLV did not get a verifiable answer back");
                            verifyDone = true;
                        }
                    }
                    if (Log.isLoggable(Level.FINE)) {
                        Log.fine("the latest version did not verify and we might not have anything to send back...");
                        if (lastResult == null) Log.fine("lastResult is null...  we have nothing to send back"); else Log.fine("lastResult is NOT null, we have something to send back!");
                    }
                }
                if (result != null) {
                    if (findASegment) {
                        if (VersioningProfile.isVersionedFirstSegment(prefix, result, startingSegmentNumber)) {
                            if (Log.isLoggable(Level.FINE)) Log.fine("getFirstBlockOfLatestVersion: got first block on first try: " + result.name());
                        } else {
                            ContentName notFirstBlockVersion = result.name().cut(versionedLength);
                            Log.info("CHILD SELECTOR FAILURE: getFirstBlockOfLatestVersion: Have version information, now querying first segment of " + startingVersion);
                            result = SegmentationProfile.getSegment(notFirstBlockVersion, startingSegmentNumber, null, timeout - elapsedTime, verifier, handle);
                            if (result == null) {
                                Log.fine("gLV could not get the first segment of the version we just found...  should exclude the version");
                                excludeList = addVersionToExcludes(excludeList, notFirstBlockVersion);
                            }
                        }
                    } else {
                    }
                    if (result != null) {
                        lastResult = result;
                        if (noTimeout) {
                            timeout = attemptTimeout;
                            remainingTime = attemptTimeout;
                        } else if (timeout == 0) {
                            Log.fine("gLV we got an answer and the caller wants the first thing we found, returning");
                            return result;
                        } else if (remainingTime > 0) {
                            if (remainingTime > attemptTimeout) {
                                remainingTime = attemptTimeout;
                            } else {
                            }
                            Log.fine("gLV we still have time to try for a better answer: remaining time = {0}", remainingTime);
                        } else {
                            Log.fine("gLV time is up, return what we have");
                        }
                    } else {
                    }
                }
            }
            if (result == null) {
                if (respondTime == 0) {
                    Log.warning("gLV: handle.get returned null and did not wait the full timeout time for the object (timeout: {0} responseTime: {1}", timeout, respondTime);
                    return null;
                }
                Log.fine("gLV we didn't get anything");
                Log.info("getFirstBlockOfLatestVersion: no block available for later version of {0}", startingVersion);
                if (lastResult != null) {
                    if (Log.isLoggable(Level.FINE)) {
                        Log.fine("gLV returning the last result that wasn't null... ");
                        Log.fine("gLV returning: {0}", lastResult.name());
                    }
                    return lastResult;
                } else {
                    Log.fine("gLV we didn't get anything, and we haven't had anything at all... try with remaining long timeout");
                    if (remainingTime > 0) {
                        Log.fine("we did not get anything back from our interest, but we still have time remaining.  timeout: {0} elapsedTime {1} remainingTime {2}", timeout, elapsedTime, remainingTime);
                        timeout = remainingTime;
                    }
                }
            }
            if (result != null) startingVersion = SegmentationProfile.segmentRoot(result.name());
        }
        if (result != null) {
            if (Log.isLoggable(Level.FINE)) Log.fine("gLV returning: {0}", result.name());
        }
        return result;
    }

    /**
	 * Find a particular segment of the latest version of a name
	 * 		- if no version given, gets the desired segment of the latest version
	 * 		- if a starting version given, gets the latest version available *after* that version or times out
	 *    Will ensure that what it returns is a segment of a version of that object.
	 *    Also makes sure to return the latest version with a SegmentationProfile.baseSegment() marker.
	 *	 * @param desiredName The name of the object we are looking for the first segment of.
	 * 					  If (VersioningProfile.hasTerminalVersion(desiredName) == false), will get latest version it can
	 * 							find of desiredName.
	 * 					  If desiredName has a terminal version, will try to find the first block of content whose
	 * 						    version is *after* desiredName (i.e. getLatestVersion starting from desiredName).
	 * @param startingSegmentNumber The desired block number, or SegmentationProfile.baseSegment() if null.
	 * @param publisher, if one is specified.
	 * @param timeout
	 * @return The first block of a stream with a version later than desiredName, or null if timeout is reached.
	 *   		This block is verified.
	 * @throws IOException
	 */
    public static ContentObject getFirstBlockOfLatestVersion(ContentName startingVersion, Long startingSegmentNumber, PublisherPublicKeyDigest publisher, long timeout, ContentVerifier verifier, CCNHandle handle) throws IOException {
        return getLatestVersion(startingVersion, publisher, timeout, verifier, handle, startingSegmentNumber, true);
    }

    /**
	 * Single-attempt function to get the first version found; and if there are multiple
	 * versions at the network point where that version is found, it will retrieve
	 * the latest of them. So if your ccnd cache has multiple versions, it will return
	 * the latest of those, but won't move past them to get a later one in the repository
	 * or one hop away. This should be used if you believe there is only a single version
	 * of a piece of content available, or if you care more about fast answers than ensuring
	 * you have the latest version available.
	 * if you ask again passing in the version found, you will find a version later than that
	 * if one is available (i.e. each response will be the latest version
	 * a given responder knows about. Further queries will move past that responder to other responders,
	 * who may have newer information.)
	 *  
	 * @param name If the name ends in a version then this method explicitly looks for a newer version
	 * than that, and will time out if no such later version exists. If the name does not end in a 
	 * version then this call just looks for the latest version.
	 * @param publisher Currently unused, will limit query to a specific publisher.
	 * @param timeout  This is the time to wait until you get any response.  If nothing is returned, this method will return null.
	 * @param verifier Used to verify the returned content objects
	 * @param handle CCNHandle used to get the latest version
	 * @return A ContentObject with the latest version, or null if the query timed out. 
	 * @result Returns a matching ContentObject, verified.
	 * @throws IOException
	 */
    public static ContentObject getAnyLaterVersion(ContentName startingVersion, PublisherPublicKeyDigest publisher, long timeout, ContentVerifier verifier, CCNHandle handle) throws IOException {
        return getLocalLatestVersion(startingVersion, publisher, timeout, verifier, handle, null, false);
    }

    /**
	 * Find a particular segment of the closest version available of this object later than
	 * the version given. If there are multiple versions available at that point, will get
	 * the latest among them. See getAnyLaterVersion.
	 * 		- if no version given, gets the desired segment of the latest version
	 * 		- if a starting version given, gets the latest version available *after* that version or times out
	 *    Will ensure that what it returns is a segment of a version of that object.
	 *    Also makes sure to return the latest version with a SegmentationProfile.baseSegment() marker.
	 *	 * @param desiredName The name of the object we are looking for the first segment of.
	 * 					  If (VersioningProfile.hasTerminalVersion(desiredName) == false), will get latest version it can
	 * 							find of desiredName.
	 * 					  If desiredName has a terminal version, will try to find the first block of content whose
	 * 						    version is *after* desiredName (i.e. getLatestVersion starting from desiredName).
	 * @param startingSegmentNumber The desired block number, or SegmentationProfile.baseSegment() if null.
	 * @param publisher, if one is specified.
	 * @param timeout
	 * @return The first block of a stream with a version later than desiredName, or null if timeout is reached.
	 *   		This block is verified.
	 * @throws IOException
	 */
    public static ContentObject getFirstBlockOfAnyLaterVersion(ContentName startingVersion, Long startingSegmentNumber, PublisherPublicKeyDigest publisher, long timeout, ContentVerifier verifier, CCNHandle handle) throws IOException {
        return getLocalLatestVersion(startingVersion, publisher, timeout, verifier, handle, startingSegmentNumber, true);
    }

    protected static ContentObject getLocalLatestVersion(ContentName startingVersion, PublisherPublicKeyDigest publisher, long timeout, ContentVerifier verifier, CCNHandle handle, Long startingSegmentNumber, boolean findASegment) throws IOException {
        Log.info("getLocalLatestVersion: getting version later than {0} called with timeout: {1}", startingVersion, timeout);
        if (null == verifier) {
            verifier = handle.keyManager().getDefaultVerifier();
        }
        long attemptTimeout = SystemConfiguration.MEDIUM_TIMEOUT;
        if (timeout == SystemConfiguration.NO_TIMEOUT) {
        } else if (timeout > 0 && timeout < attemptTimeout) {
            attemptTimeout = timeout;
        }
        long stopTime = System.currentTimeMillis() + timeout;
        ContentName prefix = startingVersion;
        if (hasTerminalVersion(prefix)) {
            prefix = startingVersion.parent();
        }
        int versionedLength = prefix.count() + 1;
        ContentObject result = null;
        ArrayList<byte[]> excludeList = new ArrayList<byte[]>();
        while ((null == result) && ((timeout == SystemConfiguration.NO_TIMEOUT) || (System.currentTimeMillis() < stopTime))) {
            Interest getLatestInterest = null;
            if (findASegment) {
                getLatestInterest = firstBlockLatestVersionInterest(startingVersion, publisher);
            } else {
                getLatestInterest = latestVersionInterest(startingVersion, null, publisher);
            }
            if (excludeList.size() > 0) {
                byte[][] e = new byte[excludeList.size()][];
                excludeList.toArray(e);
                getLatestInterest.exclude().add(e);
            }
            result = handle.get(getLatestInterest, timeout);
            if (null != result) {
                if (Log.isLoggable(Level.INFO)) Log.info("gLLV getLocalLatestVersion: retrieved latest version object {0} type: {1}", result.name(), result.signedInfo().getTypeName());
                if (!verifier.verify(result)) {
                    Log.fine("gLLV result did not verify, trying to find a verifiable answer");
                    excludeList = addVersionToExcludes(excludeList, result.name());
                    Interest retry = new Interest(result.name(), publisher);
                    boolean verifyDone = false;
                    while (!verifyDone) {
                        if (retry.exclude() == null) retry.exclude(new Exclude());
                        retry.exclude().add(new byte[][] { result.digest() });
                        if (Log.isLoggable(Level.FINE)) {
                            Log.fine("gLLV result did not verify!  doing retry!! {0}", retry);
                            Log.fine("gLLVTime sending retry interest at {0}", System.currentTimeMillis());
                        }
                        result = handle.get(retry, attemptTimeout);
                        if (result != null) {
                            if (Log.isLoggable(Level.FINE)) Log.fine("gLLV we got something back: {0}", result.name());
                            if (verifier.verify(result)) {
                                Log.fine("gLLV the returned answer verifies");
                                verifyDone = true;
                            } else {
                                Log.fine("gLLV this answer did not verify either...  try again");
                            }
                        } else {
                            Log.fine("gLLV did not get a verifiable answer back");
                            verifyDone = true;
                        }
                    }
                }
                if (result != null) {
                    if (findASegment) {
                        if (VersioningProfile.isVersionedFirstSegment(prefix, result, startingSegmentNumber)) {
                            if (Log.isLoggable(Level.FINE)) Log.fine("getFirstBlockOfLatestVersion: got first block on first try: " + result.name());
                        } else {
                            ContentName notFirstBlockVersion = result.name().cut(versionedLength);
                            Log.info("CHILD SELECTOR FAILURE: getFirstBlockOfLatestVersion: Have version information, now querying first segment of " + startingVersion);
                            result = SegmentationProfile.getSegment(notFirstBlockVersion, startingSegmentNumber, null, timeout, verifier, handle);
                            if (result == null) {
                                Log.fine("gLV could not get the first segment of the version we just found...  should exclude the version");
                                excludeList = addVersionToExcludes(excludeList, notFirstBlockVersion);
                            }
                        }
                    } else {
                    }
                }
            }
        }
        if (result != null) {
            if (Log.isLoggable(Level.FINE)) Log.fine("gLLV returning: {0}", result.name());
        }
        return result;
    }

    /**
	 * Version of isFirstSegment that expects names to be versioned, and allows that desiredName
	 * won't know what version it wants but will want some version.
	 */
    public static boolean isVersionedFirstSegment(ContentName desiredName, ContentObject potentialFirstSegment, Long startingSegmentNumber) {
        if ((null != potentialFirstSegment) && (SegmentationProfile.isSegment(potentialFirstSegment.name()))) {
            if (Log.isLoggable(Level.FINER)) Log.finer("is " + potentialFirstSegment.name() + " a first segment of " + desiredName);
            if (!desiredName.isPrefixOf(potentialFirstSegment.name())) {
                if (Log.isLoggable(Level.FINE)) Log.fine("Desired name :" + desiredName + " is not a prefix of segment: " + potentialFirstSegment.name());
                return false;
            }
            int difflen = potentialFirstSegment.name().count() - desiredName.count();
            if (difflen > 2) {
                if (Log.isLoggable(Level.FINE)) Log.fine("Have " + difflen + " extra components between " + potentialFirstSegment.name() + " and desired " + desiredName);
                return false;
            }
            if ((difflen == 2) && (!isVersionComponent(potentialFirstSegment.name().component(potentialFirstSegment.name().count() - 2)))) {
                if (Log.isLoggable(Level.FINE)) Log.fine("The " + difflen + " extra component between " + potentialFirstSegment.name() + " and desired " + desiredName + " is not a version.");
            }
            if ((null != startingSegmentNumber) && (SegmentationProfile.baseSegment() != startingSegmentNumber)) {
                return (startingSegmentNumber.longValue() == SegmentationProfile.getSegmentNumber(potentialFirstSegment.name()));
            } else {
                return SegmentationProfile.isFirstSegment(potentialFirstSegment.name());
            }
        }
        return false;
    }

    /**
	 * Adds version components to the exclude list for the getLatestVersion method.
	 * @param excludeList current excludes
	 * @param name component to add to the exclude list
	 * @return updated exclude list
	 */
    private static ArrayList<byte[]> addVersionToExcludes(ArrayList<byte[]> excludeList, ContentName name) {
        try {
            excludeList.add(VersioningProfile.getLastVersionComponent(name));
        } catch (VersionMissingException e) {
            Log.warning("failed to exclude content object version that did not verify: {0}", name);
        }
        return excludeList;
    }

    public static byte[] versionComponentFromStripped(byte[] bs) {
        if (null == bs) return null;
        byte[] versionComponent = new byte[bs.length + 1];
        versionComponent[0] = VERSION_MARKER;
        System.arraycopy(bs, 0, versionComponent, 1, bs.length);
        return versionComponent;
    }

    public static byte[] stripVersionMarker(byte[] version) throws VersionMissingException {
        if (null == version) return null;
        if (VERSION_MARKER != version[0]) {
            throw new VersionMissingException("This is not a version component!");
        }
        byte[] stripped = new byte[version.length - 1];
        System.arraycopy(version, 1, stripped, 0, stripped.length);
        return stripped;
    }
}
