package com.luzan.common.util;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;

/**
 * Abstract from IETF Specification
 * (http://ftp.ics.uci.edu/pub/ietf/webdav/uuid-guid/draft-leach-uuids-guids-01.txt):
 * <p/>
 * <i>This specification defines the format of UUIDs (Universally Unique
 * IDentifier), also known as GUIDs (Globally Unique IDentifier). A UUID is 128
 * bits long, and if generated according to the one of the mechanisms in this
 * document, is either guaranteed to be different from all other UUIDs/GUIDs
 * generated until 3400 A.D. or extremely likely to be different (depending on
 * the mechanism chosen). UUIDs were originally used in the Network Computing
 * System (NCS) [1] and later in the Open Software Foundation's (OSF)
 * Distributed Computing Environment [2].
 * <p/>
 * This specification is derived from the latter specification with the kind
 * permission of the OSF.
 * <p/>
 * References:
 * <ul>
 * <li>Lisa Zahn, et. al., Network Computing Architecture, Prentice Hall,
 * Englewood Cliffs, NJ, 1990</li>
 * <li>DCE: Remote Procedure Call, Open Group CAE Specification C309 ISBN
 * 1-85912-041-5 28cm. 674p. pbk. 1,655g. 8/94</li>
 * <li>R. Rivest, RFC 1321, "The MD5 Message-Digest Algorithm", 04/16/1992.
 * </li>
 * <li>NIST FIPS PUB 180-1, "Secure Hash Standard," National Institute of
 * Standards and Technology, U.S. Department of Commerce, DRAFT, May 31, 1994.
 * </li>
 * </ul>
 * <p/>
 * Copyright (c) 1990- 1993, 1996 Open Software Foundation, Inc. <br>
 * Copyright (c) 1989 by Hewlett-Packard Company, Palo Alto, Ca. & Digital
 * Equipment Corporation, Maynard, Mass. <br>
 * Copyright (c) 1998 Microsoft. <br>
 * <p/>
 * To anyone who acknowledges that this file is provided "AS IS" without any
 * express or implied warranty: permission to use, copy, modify, and distribute
 * this file for any purpose is hereby granted without fee, provided that the
 * above copyright notices and this notice appears in all source code copies,
 * and that none of the names of Open Software Foundation, Inc., Hewlett-Packard
 * Company, or Digital Equipment Corporation be used in advertising or publicity
 * pertaining to distribution of the software without specific, written prior
 * permission. Neither Open Software Foundation, Inc., Hewlett-Packard Company,
 * Microsoft, nor Digital Equipment Corporation makes any representations about
 * the suitability of this software for any purpose. </i>
 * <p/>
 * This class requires creating and deleting locking file (e.g. <default temp
 * file path>/guid.lock) to ensure the atomic generation of unique time stamps
 * between several JVM processes on the same system. Users with Java Security
 * enabled and without the required explicitly granted
 * <code>java.io.FilePermission</code> s (e.g. read, create and delete) are
 * not guaranteed to receive unique GUIDs between several JVM processes on the
 * same system.
 * <p/>
 * GUIDs are 128 bits in length. As such, the hexadecimal string representation
 * of the 128 bit <code>Guid</code> (e.g. see <code>toString()</code>) is
 * 32 characters long.
 * <p/>
 * The hexadecimal string representation of GUIDs generated with this class are
 * compliant with XML Schema <i>ID </i> primitive datatype
 * (http://www.w3.org/TR/2000/CR-xmlschema-2-20001024/#ID). As such, the most
 * significant hexadecimal character is guaranteed to be a non-numeric
 * character.
 * <p/>
 *
 * @version 1.0.1
 * @since 1.0
 */
public final class Guid {

    /**
     * Hexadecimal characters.
     */
    private static final char[] HEXADECIMAL_CHARACTERS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * Offset of time bewteen January 1, 1970 and October 15, 1582 in
     * 100-nanosecond resolution (0x01B21DD213814000L == 122192928000000000
     * 100-ns).
     */
    private static final long UTC_OFFSET = 0x01B21DD213814000L;

    private static final short GUID_VERSION_1 = 0x1000;

    private static final byte GUID_RESERVED = (byte) 0xC0;

    private static final int MAX_CLOCK_SEQ_ADJUST = 9999;

    private static final long MAX_WAIT_TIME = 1000;

    private static final Object lockObject = new Object();

    private static boolean _internalsSet = false;

    private static long _lastTimestamp = 0;

    private static int _clock_seq_adjust = 0;

    private static int _osProcessId = 0;

    private static int _rand_ia = 0;

    private static int _rand_ib = 0;

    private static int _rand_irand = 0;

    private static int _rand_m = 0;

    private static long clockSeq = 0;

    /**
     * The low order 32 bits of the adjusted timestamp.
     */
    private int time_low = 0;

    /**
     * The middle order 16 bits of the adjusted timestamp.
     */
    private short time_mid = 0;

    /**
     * The high order 12 bits of the adjusted timestamp multiplexed with the
     * version number. The version number is in the most significant 4 bits.
     * <p/>
     * The following table lists currently defined versions of the UUID.
     * <p/>
     * <blockquote>
     * <p/>
     * <pre>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     *           Msb0  Msb1  Msb2  Msb3  Version  Description
     * <p/>
     *            0     0     0     1       1     The IEEE 802 address, time-based version.
     * <p/>
     *            0     0     1     0       2     Reserved for DCE Security version, with
     *                                            embedded POSIX UIDs.
     * <p/>
     *            0     0     1     1       3     The name-based version.
     * <p/>
     *            0     1     0     0       4     The pseudo-randomly generated version
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * </pre>
     * <p/>
     * </blockquote>
     * <p/>
     */
    private short time_hi_and_version = 0;

    /**
     * The high order 14 bits of the clock sequence multiplexed with two
     * reserved bits.
     * <p/>
     * For Guid version 1, the clock sequence is used to help avoid duplicates
     * that could arise when the clock is set backwards in time or if the node
     * ID changes.
     * <p/>
     * If the clock is set backwards, or even might have been set backwards
     * (e.g., while the system was powered off), and the Guid generator can not
     * be sure that no Guids were generated with timestamps larger than the
     * value to which the clock was set, then the clock sequence has to be
     * changed. If the previous value of the clock sequence is known, it can be
     * just incremented; otherwise it should be set to a random or high-quality
     * pseudo random value.
     * <p/>
     * Similarly, if the node ID changes (e.g. because a network card has been
     * moved between machines), setting the clock sequence to a random number
     * minimizes the probability of a duplicate due to slight differences in the
     * clock settings of the machines. (If the value of clock sequence
     * associated with the changed node ID were known, then the clock sequence
     * could just be incremented, but that is unlikely.)
     * <p/>
     * The clock sequence MUST be originally (i.e., once in the lifetime of a
     * system) initialized to a random number to minimize the correlation across
     * systems. This provides maximum protection against node identifiers that
     * may move or switch from system to system rapidly. The initial value MUST
     * NOT be correlated to the node identifier.
     * <p/>
     * For Guid version 3, it is a 14 bit value constructed from a name.
     * <p/>
     * For Guid version 4, it is a randomly or pseudo-randomly generated 14 bit
     * value.
     * <p/>
     */
    private byte clock_seq_hi_and_reserved = 0;

    /**
     * The low order 16 bits of the clock sequence.
     *
     * @see #clock_seq_hi_and_reserved
     */
    private byte clock_seq_low = 0;

    /**
     * For Guid version 1, the node field consists of the IEEE address, usually
     * the host address. For systems with multiple IEEE 802 addresses, any
     * available address can be used. The lowest addressed octet (octet number
     * 10) contains the global/local bit and the unicast/multicast bit, and is
     * the first octet of the address transmitted on an 802.3 LAN.
     * <p/>
     * For systems with no IEEE address, a randomly or pseudo-randomly generated
     * value may be used (see section 4). The multicast bit must be set in such
     * addresses, in order that they will never conflict with addresses obtained
     * from network cards.
     * <p/>
     * For Guid version 3, the node field is a 48 bit value constructed from a
     * name.
     * <p/>
     * For Guid version 4, the node field is a randomly or pseudo-randomly
     * generated 48 bit value.
     * <p/>
     */
    private static byte[] _ieee802Addr = new byte[6];

    /**
     * No-argument constructor
     * <p/>
     * Creates a variant #1 style identifier and returns the instance.
     * <p/>
     * The adjusted time stamp is split into three fields, and the clockSeq is
     * split into two fields.
     * <p/>
     * <p/>
     * <pre>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     *         |&lt;------------------------- 32 bits --------------------------&gt;|
     * <p/>
     *         +--------------------------------------------------------------+
     *         |                     low 32 bits of time                      |  0-3  .time_low
     *         +-------------------------------+-------------------------------
     *         |     mid 16 bits of time       |                                 4-5  .time_mid
     *         +-------+-----------------------+
     *         | vers. |   hi 12 bits of time  |                                 6-7  .time_hi_and_version
     *         +-------+-------+---------------+
     *         |Res|  clkSeqHi |                                                   8   .clock_seq_hi_and_reserved
     *         +---------------+
     *         |   clkSeqLow   |                                                   9   .clock_seq_low
     *         +---------------+----------...-----+
     *         |            node ID               |                             8-16   .node
     *         +--------------------------...-----+
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * </pre>
     * <p/>
     * <p/>
     */
    public Guid() {
        long adjustedTimestamp = 0;
        synchronized (lockObject) {
            if (_internalsSet == false) {
                getPseudoIEEE802Address(_ieee802Addr);
                _osProcessId = getPseudoOSProcessId();
                initTrueRandom(getAdjustedTimestamp());
                clockSeq = getTrueRandom();
                _internalsSet = true;
            }
            boolean timeIsValid = true;
            do {
                adjustedTimestamp = getAdjustedTimestamp();
                if (adjustedTimestamp < _lastTimestamp) {
                    clockSeq = getTrueRandom();
                    _clock_seq_adjust = 0;
                }
                if (adjustedTimestamp > _lastTimestamp) {
                    _clock_seq_adjust = 0;
                }
                if (adjustedTimestamp == _lastTimestamp) {
                    if (_clock_seq_adjust < MAX_CLOCK_SEQ_ADJUST) {
                        _clock_seq_adjust++;
                    } else {
                        timeIsValid = false;
                    }
                }
            } while (!timeIsValid);
            _lastTimestamp = adjustedTimestamp;
            if (_clock_seq_adjust != 0) {
                adjustedTimestamp += _clock_seq_adjust;
            }
            long tempValue = adjustedTimestamp & 0xFFFFFFFF;
            time_low = (int) tempValue;
            tempValue = (adjustedTimestamp >>> 32) & 0xFFFF;
            time_mid = (short) tempValue;
            tempValue = (adjustedTimestamp >>> 48) & 0x0FFF;
            time_hi_and_version = (short) tempValue;
            time_hi_and_version |= GUID_VERSION_1;
            tempValue = clockSeq & 0xFF;
            clock_seq_low = (byte) tempValue;
            tempValue = (clockSeq & 0x3F00) >>> 8;
            clock_seq_hi_and_reserved = (byte) tempValue;
            clock_seq_hi_and_reserved |= GUID_RESERVED;
        }
    }

    /**
     * Initializes true number values generator.
     */
    private synchronized void initTrueRandom(long adjustedTimestamp) {
        _rand_m = 971;
        _rand_ia = 11113;
        _rand_ib = 104322;
        _rand_irand = 4181;
        int seed = ((int) (adjustedTimestamp >>> 48)) ^ ((int) (adjustedTimestamp >>> 32)) ^ ((int) (adjustedTimestamp >>> 16)) ^ ((int) (adjustedTimestamp & 0x000000000000FFFF));
        _rand_irand = _rand_irand + seed + _osProcessId;
    }

    /**
     * Get true random value.
     */
    private synchronized short getTrueRandom() {
        _rand_m = _rand_m + 7;
        _rand_ia = _rand_ia + 1907;
        _rand_ib = _rand_ib + 73939;
        if (_rand_m >= 9973) {
            _rand_m = _rand_m - 9871;
        }
        if (_rand_ia >= 99991) {
            _rand_ia = _rand_ia - 89989;
        }
        if (_rand_ib >= 224729) {
            _rand_ib = _rand_ib - 96233;
        }
        _rand_irand = (_rand_irand * _rand_m) + _rand_ia + _rand_ib;
        _rand_irand = (_rand_irand >>> 16) ^ (_rand_irand & 0x00003FFF);
        return ((short) _rand_irand);
    }

    /**
     * This call originally would use JNI to return the process ID of the java
     * program. Because we do not want to support JNI on all the platforms we
     * will simply get the time stamp. Because the value that is being returned
     * should be an int and not a long we modulus by 1000000000 and return the
     * lowest significant 9 numbers.
     *
     * @return the current time in seconds
     */
    private synchronized int getPseudoOSProcessId() {
        return ((int) (getUniqueTimeStamp() % 1000000000));
    }

    /**
     * This call originally would use JNI to return the IEEE 802 addrsss from
     * the system. Because we do not want to support JNI on all the platforms we
     * be using an alternative method of generating a value for an IEEE 802 that
     * is describe in the internet draft.
     * <p/>
     * The following is the excert from the draft:
     * <p/>
     * 4. Node IDs when no IEEE 802 network card is available
     * <p/>
     * If a system wants to generate UUIDs but has no IEE 802 compliant network
     * card or other source of IEEE 802 addresses, then this section describes
     * how to generate one.
     * <p/>
     * The ideal solution is to obtain a 47 bit cryptographic quality random
     * number, and use it as the low 47 bits of the node ID, with the most
     * significant bit of the first octet of the node ID set to 1. This bit is
     * the unicast/multicast bit, which will never be set in IEEE 802 addresses
     * obtained from network cards; hence, there can never be a conflict between
     * UUIDs generated by machines with and without network cards.
     * <p/>
     * If a system does not have a primitive to generate cryptographic quality
     * random numbers, then in most systems there are usually a fairly large
     * number of sources of randomness available from which one can be
     * generated. Such sources are system specific, but often include:
     * <p>- the percent of memory in use in bytes - the size of main memory in
     * bytes - the amount of free main memory in bytes - the size of the paging
     * or swap file in bytes - free bytes of paging or swap file - the total
     * size of user virtual address space in bytes - the total available user
     * address space bytes - the size of boot disk drive in bytes - the free
     * disk space on boot drive in bytes - the current time - the amount of time
     * since the system booted - the individual sizes of files in various system
     * directories - the creation, last read, and modification times of files in
     * various system directories - the utilization factors of various system
     * resources (heap, etc.) - current mouse cursor position - current caret
     * position - current number of running processes, threads - handles or IDs
     * of the desktop window and the active window - the value of stack pointer
     * of the caller - the process and thread ID of caller - various processor
     * architecture specific performance counters (instructions executed, cache
     * misses, TLB misses)
     * <p/>
     * (Note that it precisely the above kinds of sources of randomness that are
     * used to seed cryptographic quality random number generators on systems
     * without special hardware for their construction.)
     * <p/>
     * In addition, items such as the computer's name and the name of the
     * operating system, while not strictly speaking random, will help
     * differentiate the results from those obtained by other systems.
     * <p/>
     * The exact algorithm to generate a node ID using these data is system
     * specific, because both the data available and the functions to obtain
     * them are often very system specific. However, assuming that one can
     * concatenate all the values from the randomness sources into a buffer, and
     * that a cryptographic hash function such as MD5 [3] is available, then any
     * 6 bytes of the MD5 hash of the buffer, with the multicast bit (the high
     * bit of the first byte) set will be an appropriately random node ID.
     * <p/>
     */
    private synchronized void getPseudoIEEE802Address(byte[] ieee802Addr) {
        byte[] currentTime = String.valueOf(getUniqueTimeStamp()).getBytes();
        byte[] localHostAddress = getLocalHostAddress();
        byte[] inMemObj = new Object().toString().getBytes();
        byte[] freeMemory = String.valueOf(Runtime.getRuntime().freeMemory()).getBytes();
        byte[] totalMemory = String.valueOf(Runtime.getRuntime().totalMemory()).getBytes();
        byte[] hashcode = null;
        byte[] bytes = new byte[freeMemory.length + totalMemory.length + currentTime.length + localHostAddress.length + inMemObj.length];
        int bytesPos = 0;
        System.arraycopy(currentTime, 0, bytes, bytesPos, currentTime.length);
        bytesPos += currentTime.length;
        System.arraycopy(localHostAddress, 0, bytes, bytesPos, localHostAddress.length);
        bytesPos += localHostAddress.length;
        System.arraycopy(inMemObj, 0, bytes, bytesPos, inMemObj.length);
        bytesPos += inMemObj.length;
        System.arraycopy(freeMemory, 0, bytes, bytesPos, freeMemory.length);
        bytesPos += freeMemory.length;
        System.arraycopy(totalMemory, 0, bytes, bytesPos, totalMemory.length);
        bytesPos += totalMemory.length;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            hashcode = md5.digest(bytes);
        } catch (Exception e) {
        }
        System.arraycopy(hashcode, 0, ieee802Addr, 0, 6);
        ieee802Addr[0] |= 0x80;
    }

    private synchronized byte[] getLocalHostAddress() {
        try {
            return (InetAddress.getLocalHost().getAddress());
        } catch (UnknownHostException u) {
            return (new byte[] { 127, 0, 0, 1 });
        }
    }

    /**
     * Obtains the current timestamp and adjust its value
     */
    private synchronized long getAdjustedTimestamp() {
        return ((System.currentTimeMillis() * 10000L) + UTC_OFFSET);
    }

    private synchronized long getUniqueTimeStamp() {
        File lockFile = new File(System.getProperty("java.io.tmpdir"), "guid.lock");
        long timeStamp = System.currentTimeMillis();
        try {
            long lastModified = lockFile.lastModified();
            long maxWaitTimeStamp = (System.currentTimeMillis() + MAX_WAIT_TIME);
            while (true) {
                try {
                    if (lockFile.createNewFile()) {
                        break;
                    } else if (System.currentTimeMillis() > maxWaitTimeStamp) {
                        if (lockFile.lastModified() <= lastModified) {
                            lockFile.delete();
                            lastModified = -1;
                        } else {
                            return (System.currentTimeMillis());
                        }
                    }
                } catch (IOException i) {
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException i) {
            }
            timeStamp = System.currentTimeMillis();
        } catch (SecurityException s) {
        } finally {
            try {
                if (lockFile != null) {
                    lockFile.delete();
                }
            } catch (SecurityException s) {
            }
        }
        return timeStamp;
    }

    /**
     * Returns the hexadecimal string representation of the 128 bit
     * <code>Guid</code> object.
     * <p/>
     * The <code>toString</code> method will take the binary data in each of
     * the internal components of the <code>Guid</code> object, covert each
     * byte to its hexadecimal character equivalent, and return the string.
     * <p/>
     *
     * @return The hexadecimal string representation of the 128 bit
     *         <code>Guid</code>, which is 32 characters long.
     */
    public String toString() {
        final char[] stringBuffer = new char[32];
        int pos = 0;
        stringBuffer[pos++] = HEXADECIMAL_CHARACTERS[(clock_seq_hi_and_reserved >>> 4 & 0xF)];
        stringBuffer[pos++] = HEXADECIMAL_CHARACTERS[(clock_seq_hi_and_reserved & 0xF)];
        stringBuffer[pos++] = HEXADECIMAL_CHARACTERS[(clock_seq_low >>> 4 & 0xF)];
        stringBuffer[pos++] = HEXADECIMAL_CHARACTERS[(clock_seq_low & 0xF)];
        int i = 0;
        while (pos < 16) {
            stringBuffer[pos] = HEXADECIMAL_CHARACTERS[(_ieee802Addr[i] >>> 4 & 0xF)];
            stringBuffer[pos + 1] = HEXADECIMAL_CHARACTERS[(_ieee802Addr[i] & 0xF)];
            i++;
            pos += 2;
        }
        int shift = 28;
        while (pos < 24) {
            stringBuffer[pos] = HEXADECIMAL_CHARACTERS[(time_low >>> shift & 0xF)];
            shift -= 4;
            pos++;
        }
        shift = 12;
        while (pos < 28) {
            stringBuffer[pos] = HEXADECIMAL_CHARACTERS[(time_mid >>> shift & 0xF)];
            shift -= 4;
            pos++;
        }
        shift = 12;
        while (pos < 32) {
            stringBuffer[pos] = HEXADECIMAL_CHARACTERS[(time_hi_and_version >>> shift & 0xF)];
            shift -= 4;
            pos++;
        }
        return (new String(stringBuffer).trim());
    }

    /**
     * Static convenience API for generating a new GUID.
     * <p/>
     * This API is equivalent to calling <code>new Guid().toString()</code>.
     * <p/>
     * The API returns the hexadecimal string representation of a new
     * <code>Guid</code> object.
     * <p/>
     * This API will create a new <code>Guid</code> object, take the binary
     * data in each of the internal components of the <code>Guid</code>
     * object, covert each byte to its hexadecimal character equivalent and
     * return the string.
     * <p/>
     *
     * @return The hexadecimal string representation of a new <code>Guid</code>
     *         object, which is 32 characters long.
     */
    public static String generate() {
        return (new Guid().toString());
    }
}
