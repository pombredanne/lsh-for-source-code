package antiquity.cr.impl;

import ostore.util.QSException;
import ostore.util.OutputBuffer;
import ostore.util.InputBuffer;
import ostore.util.QuickSerializable;
import bamboo.util.GuidTools;
import java.util.Map;
import java.util.LinkedHashMap;
import java.math.BigInteger;
import java.security.MessageDigest;
import static antiquity.util.AntiquityUtils.byteArrayToBigInteger;
import java.io.IOException;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.XdrEncodingStream;
import org.acplt.oncrpc.XdrDecodingStream;

/**
 * <CODE>ClientInfo</CODE> is contsints maximum number of extents a client
 * can store and all existing extents ({@link java.math.BigInteger extent_key}
 * and <code>expire_time</code> [in ms]).
 *
 * @author Hakim Weatherspoon
 * @version $Id: ClientInfo.java,v 1.1.1.1 2006/12/15 22:45:36 hweather Exp $
 **/
public class ClientInfo implements QuickSerializable, XdrAble, Comparable {

    /** {@link java.math.BigInteger client_id}. */
    private BigInteger _client_id;

    /** <code>client_pkey</code> as an array of bytes. */
    private byte[] _client_pkey_value;

    /** Current and Maximum number of extents. */
    private int _num_extents, _max_num_extents;

    /** Minimum and Maximum extent expire times. */
    private long _min_expire_time, _max_expire_time;

    /** Constructor: Creates a new <code>ClientInfo</code>. */
    public ClientInfo(byte[] client_pkey_value, int num_extents, int max_num_extents, long min_expire_time, long max_expire_time, MessageDigest md) {
        _client_pkey_value = client_pkey_value;
        md.update(client_pkey_value);
        _client_id = byteArrayToBigInteger(md.digest());
        _num_extents = num_extents;
        _max_num_extents = _max_num_extents;
        _min_expire_time = min_expire_time;
        _max_expire_time = max_expire_time;
    }

    /**
   * Constructs a <CODE>ClientInfo</CODE> from its
   * {@link ostore.util.QuickSerializable#serialize serialized} form.
   *
   * @param buffer {@link ostore.util.QuickSerializable#serialize serialized}
   *                form of object.  */
    public ClientInfo(InputBuffer buffer) throws QSException {
        int len = buffer.nextInt();
        _client_pkey_value = new byte[len];
        buffer.nextBytes(_client_pkey_value, 0, len);
        _client_id = buffer.nextBigInteger();
        _num_extents = buffer.nextInt();
        _max_num_extents = buffer.nextInt();
        _min_expire_time = buffer.nextLong();
        _max_expire_time = buffer.nextLong();
    }

    /**
   * Constructs a <CODE>ClientInfo</CODE> from its
   * {@link org.acplt.oncrpc.XdrAble serialized} form.
   *
   * @param buffer {@link org.acplt.oncrpc.XdrDecodingStream buffer} containing
   *             {@link org.acplt.oncrpc.XdrAble serialized} form of object. */
    public ClientInfo(XdrDecodingStream buffer) throws OncRpcException, IOException {
        xdrDecode(buffer);
    }

    /** 
   * Return the <code>client pkey</code> (as an array of bytes).
   * @return the <code>client pkey</code> (as an array of bytes). */
    public byte[] getClientPkey() {
        return _client_pkey_value;
    }

    /** 
   * Return the {@link java.math.BigInteger client_id}.
   * @return the {@link java.math.BigInteger client_id}. */
    public BigInteger getClientId() {
        return _client_id;
    }

    /** 
   * Return the number of extents for this client. 
   * @return the number of extents for this client. */
    public int getNumExtents() {
        return _num_extents;
    }

    /** 
   * Return the maximum number of extents for this client. 
   * @return the maximum number of extents for this client. */
    public int getMaxNumExtents() {
        return _max_num_extents;
    }

    /** 
   * Return the minimum expire time (in ms) of extents for this client. 
   * @return the minimum expire time (in ms) of extents for this client. */
    public long getMinExpireTime() {
        return _min_expire_time;
    }

    /** 
   * Return the maximum expire time (in ms) of extents for this client. 
   * @return the maximum expire time (in ms) of extents for this client. */
    public long getMaxExpireTime() {
        return _max_expire_time;
    }

    /** Specified by ostore.util.QuickSerializable */
    public void serialize(OutputBuffer buffer) {
        buffer.add(_client_pkey_value.length);
        buffer.add(_client_pkey_value);
        buffer.add(_client_id);
        buffer.add(_num_extents);
        buffer.add(_max_num_extents);
        buffer.add(_min_expire_time);
        buffer.add(_max_expire_time);
    }

    /** Specified by org.acplt.oncrpc.XdrAble */
    public void xdrEncode(XdrEncodingStream buffer) throws OncRpcException, IOException {
        buffer.xdrEncodeDynamicOpaque(_client_pkey_value);
        buffer.xdrEncodeDynamicOpaque(_client_id.toByteArray());
        buffer.xdrEncodeInt(_num_extents);
        buffer.xdrEncodeInt(_max_num_extents);
        buffer.xdrEncodeLong(_min_expire_time);
        buffer.xdrEncodeLong(_max_expire_time);
    }

    /** Specified by org.acplt.oncrpc.XdrAble */
    public void xdrDecode(XdrDecodingStream buffer) throws OncRpcException, IOException {
        _client_pkey_value = buffer.xdrDecodeDynamicOpaque();
        _client_id = new BigInteger(buffer.xdrDecodeDynamicOpaque());
        _num_extents = buffer.xdrDecodeInt();
        _max_num_extents = buffer.xdrDecodeInt();
        _min_expire_time = buffer.xdrDecodeLong();
        _max_expire_time = buffer.xdrDecodeLong();
    }

    /** Specified by java.lang.Object */
    public boolean equals(Object o) {
        return compareTo(o) == 0;
    }

    /** Specified by java.lang.Comparable */
    public int compareTo(Object o) {
        if (o == null) throw new NullPointerException(); else if (!(o instanceof ClientInfo)) throw new ClassCastException("cannot compareTo non-ClientInfo"); else if (o == this) return 0; else {
            ClientInfo rhs = (ClientInfo) o;
            return _client_id.compareTo(rhs._client_id);
        }
    }

    /**
   * Calculates a Java hash code for this Tag object
   * (by returning its type_code)
   *
   * @return the Java hash code for this Tag. */
    public int hashCode() {
        return (int) (_client_id.hashCode());
    }

    /** Specified by java.lang.Object */
    public String toString() {
        String str = new String("(ClientInfo");
        str += " client_id=0x" + GuidTools.guid_to_string(_client_id);
        str += " num_extents=" + _max_num_extents;
        str += " max_num_extents=" + _max_num_extents;
        str += " min_expire_time=" + _min_expire_time + "us";
        str += " max_expire_time=" + _max_expire_time + "us";
        str += ")";
        return str;
    }
}
