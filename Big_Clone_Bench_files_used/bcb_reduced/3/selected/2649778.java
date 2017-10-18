package antiquity.rpc.impl;

import bamboo.lss.NioMultiplePacketInputBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.nio.ByteBuffer;
import ostore.util.ByteUtils;
import ostore.util.InputBuffer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * <code>ConnectionStream</code> is used to read 
 * {@link java.nio.ByteBuffer packets} off the wire from a stream oriented 
 * protocol like TCP.  In particular, this class is used to reassemble
 * messages that are sent over a RPC over TCP protocol where packets are 
 * sent reliably, in order, and large messages may be broken into
 * smaller fragments.  In essence, if {@link #message_available} is 
 * <code>true</code>, then an entire message is available to be
 * deserialized and a subsequent call to {@link #get_next_message_buffer} 
 * will return an {@link ostore.util.InputBuffer} used to deserialize the 
 * message.  <code>ConnectStream</code> works in coordination with the
 * {@link antiquity.rpc.impl.RpcClientStage} and 
 * {@link antiquity.rpc.impl.RpcServerStage}.
 *
 * @author Hakim Weatherspoon
 * @version $Id: ConnectionStream.java,v 1.3 2007/09/04 21:54:29 hweather Exp $
 */
public class ConnectionStream {

    /** {@link java.util.LinkedList} of packets (in order). */
    private LinkedList<ByteBuffer> in_order_packets;

    /** Number of total bytes available. */
    private int _bytes_available;

    /** {@link org.apache.log4j.Logger}. */
    private Logger logger;

    /** 
   * {@link java.util.LinkedList} of msg fragments made up of 
   * {@link java.nio.ByteBuffer packets} (in order). */
    private LinkedList<ByteBuffer> _frags;

    /** 
   * Total number of bytes of all msg fragments stored in the 
   * {@link #_frags} {@link java.util.LinkedList}. */
    private int _frags_size;

    /** 
   * Total number of fragments stored in the 
   * {@link #_frags} {@link java.util.LinkedList}. */
    private int _frags_num;

    /** 
   * <code>index</code> of {@link java.nio.ByteBuffer packets} that make 
   * up fragments. */
    private int _frags_packet_index;

    /** <code>index</code> of start of {@link java.nio.ByteBuffer fragment}. */
    private LinkedList<Integer> _frags_end_index;

    /** 
   * {@link java.util.LinkedList} of {@link java.nio.ByteBuffer packets}
   * that can be reused.  In particular, when {@link #get_next_message_buffer}
   * is called, <code>reuse</code> contains the same 
   * {@link java.nio.ByteBuffer packets}. */
    private LinkedList<ByteBuffer> _reuse;

    /** 
   * <code>true</code> if {@link java.nio.ByteBuffer} was 
   * {@link java.nio.ByteBuffer#slice sliced}. */
    private LinkedList<Boolean> _spliced;

    /** {@link java.nio.ByteBuffer#slice sliced} {@link java.nio.ByteBuffer}s. */
    private LinkedList<ByteBuffer> _splicedPackets;

    /** Flag indicating current {@link java.nio.ByteBuffer} is being spliced. */
    private boolean _splicing;

    /** Flag indicating first sign that a new message is being received. */
    private boolean _first_sighting;

    private java.security.MessageDigest _md;

    /** Constructor: Creates a new <code>Connectionstream</code>. */
    public ConnectionStream(Level debug_level) {
        in_order_packets = new LinkedList<ByteBuffer>();
        _frags = new LinkedList<ByteBuffer>();
        _frags_size = 0;
        _frags_num = 0;
        _frags_end_index = new LinkedList<Integer>();
        _reuse = new LinkedList<ByteBuffer>();
        _spliced = new LinkedList<Boolean>();
        _splicedPackets = new LinkedList<ByteBuffer>();
        _splicing = false;
        _first_sighting = false;
        logger = Logger.getLogger(getClass().getName());
        logger.setLevel(debug_level);
        try {
            _md = java.security.MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            logger.fatal("exception", e);
            System.exit(1);
        }
    }

    /**
   * <code>get_next_message_buffer</code> returns a 
   * {@link bamboo.lss.NioMultiplePacketInputBuffer} object that contains 
   * the next message. 
   *
   * @return {@link bamboo.lss.NioMultiplePacketInputBuffer} object that 
   *           contains the next message. */
    public InputBuffer get_next_message_buffer() {
        NioMultiplePacketInputBuffer result = new NioMultiplePacketInputBuffer();
        get_next_message_buffer(result);
        return result;
    }

    /**
   * <code>get_next_message_buffer</code> fills the 
   * {@link bamboo.lss.NioMultiplePacketInputBuffer} parameter so that it
   * contains the next message. 
   *
   * @param result {@link bamboo.lss.NioMultiplePacketInputBuffer} object that 
   *          will contain the next message. */
    public void get_next_message_buffer(NioMultiplePacketInputBuffer result) {
        int record_marker = peek_int();
        int size = record_marker & 0x7fffffff;
        boolean last_frag = (record_marker & 0x80000000) != 0;
        assert _bytes_available >= size && last_frag : "bytes_avail=" + _bytes_available + " < size=" + size + ". last_frag=" + last_frag;
        get_frags(_frags);
        int start_index = 0;
        int index = 0;
        int msg_size = 0;
        int xact_id = -1;
        for (int end_index : _frags_end_index) {
            List<ByteBuffer> frags = _frags.subList(start_index, end_index);
            assert frags != null && !frags.isEmpty() : "No packets (start_index=" + start_index + ", end_index=" + end_index + ") for frag " + index + ": xact_id 0x" + Integer.toHexString(xact_id) + " msg_size so far " + msg_size + " bytes.";
            int frag_size = 0;
            for (ByteBuffer frag : frags) frag_size += frag.remaining();
            msg_size += frag_size;
            assert frag_size >= ByteUtils.SIZE_INT : "frag_size=" + " < SIZE_INT=" + ByteUtils.SIZE_INT + ". (start_index=" + start_index + ", end_index=" + end_index + ") for frag " + index + " msg_size so far " + msg_size + " bytes.";
            int tmp_xact_id = -1;
            int second_int = -1;
            if (frag_size >= ByteUtils.SIZE_LONG) {
                long xact_id_and_second_int = peek(frags, ByteUtils.SIZE_LONG);
                tmp_xact_id = (int) (xact_id_and_second_int >> 32);
                if (xact_id == -1) xact_id = tmp_xact_id;
                second_int = (int) xact_id_and_second_int;
            } else {
                tmp_xact_id = (int) peek(frags, ByteUtils.SIZE_INT);
            }
            if (logger.isDebugEnabled()) logger.debug("frag " + index + ": xact_id 0x" + Integer.toHexString(tmp_xact_id) + (second_int != -1 ? " second_byte 0x" + Integer.toHexString(second_int) : "") + "; frag_size=" + frag_size + " start_index=" + start_index + " num_packets=" + (end_index - start_index));
            start_index = end_index;
            index++;
        }
        assert msg_size == _frags_size : "msg_size=" + msg_size + " != frags_size=" + _frags_size;
        int next_frag_size = -1;
        int next_xact_id = -1;
        boolean next_frag_last_frag = false;
        if (_bytes_available >= ByteUtils.SIZE_LONG) {
            long len_and_xact_id = peek(in_order_packets, ByteUtils.SIZE_LONG);
            int next_record_marker = (int) (len_and_xact_id >> 32);
            next_frag_size = next_record_marker & 0x7fffffff;
            next_frag_last_frag = (next_record_marker & 0x80000000) != 0;
            next_xact_id = (int) len_and_xact_id;
        }
        if (logger.isDebugEnabled()) logger.debug("For xact_id 0x" + Integer.toHexString(xact_id) + " msg_size=" + msg_size + " num_frags=" + _frags_end_index.size() + " num_packets=" + _frags.size() + "; bytes_avail=" + _bytes_available + " in_order_packets.size=" + in_order_packets.size() + "; splicing=" + _splicing + " splicedPackets.size=" + _splicedPackets.size() + (next_xact_id == -1 ? "" : "; next_frag_size=" + next_frag_size + " next_frag_last_frag=" + next_frag_last_frag + " next_xact_id=" + Integer.toHexString(next_xact_id)));
        _reuse.clear();
        assert _frags.size() == _spliced.size() : "frags.size=" + _frags.size() + " spliced=" + _spliced.size();
        Iterator<ByteBuffer> i = _frags.iterator();
        Iterator<Boolean> j = _spliced.iterator();
        for (; i.hasNext(); ) {
            ByteBuffer frag = i.next();
            result.add_packet(frag);
            Boolean spliced = j.next();
            if (!spliced.booleanValue() && _splicedPackets.isEmpty()) {
                _reuse.addLast(frag);
            } else if (!spliced.booleanValue() && !_splicedPackets.isEmpty()) {
                ByteBuffer splicedPacket = _splicedPackets.removeFirst();
                _reuse.addLast(splicedPacket);
            } else if (!spliced.booleanValue() && in_order_packets.isEmpty()) {
                assert !_splicedPackets.isEmpty() : "spliced is empty";
                ByteBuffer splicedPacket = _splicedPackets.removeFirst();
                _reuse.addLast(splicedPacket);
            } else {
                if (logger.isDebugEnabled()) logger.debug("packet " + frag + " has been spliced so will not be" + " added to reuse collection");
            }
        }
        _frags.clear();
        _frags_size = 0;
        _frags_num = 0;
        _frags_packet_index = 0;
        _frags_end_index.clear();
        _spliced.clear();
        _first_sighting = false;
    }

    /** 
   * <code>get_reuse_buffers</code> returns a {@link java.util.LinkedList} 
   * of {@link java.nio.ByteBuffer packets} that can be reused.  
   * In particular, when {@link #get_next_message_buffer}
   * is called, the return value of  <code>get_reuse_buffers</code>
   * contains the same {@link java.nio.ByteBuffer packets}.
   *
   * @return {@link java.util.LinkedList} of
   * {@link java.nio.ByteBuffer packets} that can be reused. */
    public LinkedList<ByteBuffer> get_reuse_buffers() {
        return new LinkedList<ByteBuffer>();
    }

    /** 
   * <code>clear_reuse_buffers</code> clears the internal
   * data structure that stores {@link java.nio.ByteBuffer packets} 
   * that can be reused.  */
    public void clear_reuse_buffers() {
        _reuse.clear();
    }

    /**
   * <code>get_frags</code> returns a msg fragment represented as
   * a {@link java.util.LinkedList} of {@link java.nio.ByteBuffer packets} 
   * (in order).
   *
   * @return a msg fragment represented as a {@link java.util.LinkedList} of 
   *      {@link java.nio.ByteBuffer packets} (in order). */
    private LinkedList<ByteBuffer> get_frags() {
        LinkedList<ByteBuffer> result = new LinkedList<ByteBuffer>();
        get_frags(result);
        return result;
    }

    /**
   * <code>get_frags</code> returns a msg fragment represented as
   * a {@link java.util.LinkedList} of {@link java.nio.ByteBuffer packets} 
   * (in order).
   *
   * @param result A data structure that will be filled with a msg fragment 
   *          represented as a {@link java.util.LinkedList} of 
   *          {@link java.nio.ByteBuffer packets} (in order). */
    private void get_frags(LinkedList<ByteBuffer> result) {
        int record_marker = read_int();
        int size = record_marker & 0x7fffffff;
        boolean last_frag = (record_marker & 0x80000000) != 0;
        assert _bytes_available >= size : "bytes_avail=" + _bytes_available + " < size=" + size;
        ByteBuffer packet = in_order_packets.getFirst();
        int remaining = size;
        while (remaining > 0 && remaining > packet.remaining()) {
            if (logger.isDebugEnabled() && packet.hasArray()) _md.update(packet.array(), packet.arrayOffset(), packet.limit());
            _frags_packet_index++;
            in_order_packets.removeFirst();
            result.addLast(packet);
            remaining -= packet.remaining();
            ByteBuffer packet2 = in_order_packets.getFirst();
            if (logger.isDebugEnabled()) logger.debug("get_frags: (1) size=" + size + " remaining=" + remaining + " bytes_avail=" + _bytes_available + " splicing=" + _splicing + ". packet=" + packet + " packet2=" + packet2);
            packet = packet2;
            _spliced.addLast(_splicing ? Boolean.TRUE : Boolean.FALSE);
            if (_splicing) _splicing = false;
        }
        {
            ByteBuffer packet2 = in_order_packets.removeFirst();
            if (logger.isDebugEnabled()) logger.debug("get_frags: (2) size=" + size + " remaining=" + remaining + " bytes_avail=" + _bytes_available + " splicing=" + _splicing + ". packet=" + packet + " packet2=" + packet2);
        }
        if (remaining > 0) {
            if (remaining == packet.remaining()) {
                if (logger.isDebugEnabled() && packet.hasArray()) _md.update(packet.array(), packet.arrayOffset(), packet.limit());
                _frags_packet_index++;
                result.addLast(packet);
                remaining -= packet.remaining();
                if (logger.isDebugEnabled()) logger.debug("get_frags: (3) size=" + size + " remaining=" + remaining + " bytes_avail=" + _bytes_available + " splicing=" + _splicing + ". packet=" + packet);
                _spliced.addLast(_splicing ? Boolean.TRUE : Boolean.FALSE);
                if (_splicing) _splicing = false;
            } else {
                packet.mark();
                packet.position(packet.position() + remaining);
                ByteBuffer packet2 = packet.slice();
                packet.reset();
                packet.limit(packet.position() + remaining);
                if (logger.isDebugEnabled() && packet.hasArray()) _md.update(packet.array(), packet.arrayOffset(), packet.limit());
                _frags_packet_index++;
                result.addLast(packet);
                remaining -= packet.remaining();
                in_order_packets.addFirst(packet2);
                if (logger.isDebugEnabled()) logger.debug("get_frags: (4) size=" + size + " remaining=" + remaining + " bytes_avail=" + _bytes_available + " splicing=" + _splicing + ". packet=" + packet + " packet2=" + packet2 + " packet[len-8..len]=" + (packet.hasArray() ? ostore.util.ByteUtils.print_bytes(packet.array(), (packet.limit() > 8 ? packet.arrayOffset() + packet.limit() - 8 : packet.arrayOffset()), (packet.limit() > 8 ? 8 : packet.limit())) : "no backing array") + " packet2[0..8]=" + (packet2.hasArray() ? ostore.util.ByteUtils.print_bytes(packet2.array(), packet2.arrayOffset() + packet2.position(), (packet2.remaining() < 8 ? packet2.remaining() : 8)) : "no backing array"));
                _spliced.addLast(Boolean.TRUE);
                _splicing = true;
            }
        }
        assert remaining == 0 : "remaining=" + remaining + ". should be 0.";
        _bytes_available -= size;
        _frags_size += size;
        _frags_num++;
        _frags_end_index.addLast(_frags_packet_index);
        if (logger.isDebugEnabled()) {
            if (packet.hasArray()) {
                byte[] digest = _md.digest();
                logger.debug("get_frags: frag " + (_frags_num - 1) + " hash: 0x" + ostore.util.ByteUtils.print_bytes(digest, 0, 4));
            }
        }
    }

    /**
   * <code>message_available</code> returns <code>true</code> if and only if 
   * a message is available, meaning that {@link #get_next_message_buffer}
   * will succeed.
   *
   * @return <code>true</code> if and only if a message is available. */
    public boolean message_available() {
        if (_bytes_available < ByteUtils.SIZE_INT) {
            if (logger.isDebugEnabled()) logger.debug("message_available: only " + _bytes_available + " bytes available");
            return false;
        }
        if (_bytes_available < ByteUtils.SIZE_LONG) {
            int record_marker = peek_int();
            int size = record_marker & 0x7fffffff;
            boolean last_frag = (record_marker & 0x80000000) != 0;
            if (logger.isDebugEnabled()) logger.debug("message_available: msg size is " + size + " bytes, but" + " only " + (_bytes_available - ByteUtils.SIZE_INT) + " avail");
            return false;
        }
        long len_and_xact_id = peek_long();
        int record_marker = (int) (len_and_xact_id >> 32);
        long size = record_marker & 0x7fffffff;
        boolean last_frag = (record_marker & 0x80000000) != 0;
        int xact_id = (int) len_and_xact_id;
        if (logger.isInfoEnabled() && !_first_sighting) {
            _first_sighting = true;
            logger.info("message_available: First sighting of msg xact_id 0x" + Integer.toHexString(xact_id) + ". Current bytes availabile " + _bytes_available + " out of " + size);
        }
        if (logger.isDebugEnabled()) logger.debug("message_available: msg xact_id 0x" + Integer.toHexString(xact_id) + " size is " + size + " bytes, " + (_bytes_available - ByteUtils.SIZE_INT) + " avail");
        if (_bytes_available - ByteUtils.SIZE_INT < size) {
            if (logger.isDebugEnabled()) logger.debug("message_available: msg xact_id 0x" + Integer.toHexString(xact_id) + " only " + (_bytes_available - ByteUtils.SIZE_INT) + " bytes available");
            return false;
        }
        if (!last_frag) {
            if (logger.isDebugEnabled()) logger.debug("message_available: a fragment of msg 0x" + Integer.toHexString(xact_id) + " is available. " + " current frag size is " + size + " bytes, " + " previous aggregate frag size is " + _frags_size + " bytes" + " stored in " + _frags_num + " fragments, " + (_bytes_available - ByteUtils.SIZE_INT) + " avail");
            get_frags(_frags);
            return message_available();
        }
        return true;
    }

    /**
   * <code>next_message_size</code> returns <code>size</code> of the next
   * message, meaning that {@link #get_next_message_buffer} will return
   * an {@link ostore.util.InputBuffer} containing an object of size
   * <code>next_message_size</code>.
   *
   * @return <code>size</code> of the next message. */
    public int next_message_size() {
        assert message_available() : "message_available not available";
        int record_marker = peek_int();
        int size = record_marker & 0x7fffffff;
        boolean last_frag = (record_marker & 0x80000000) != 0;
        return size + _frags_size;
    }

    /**
   * <code>peek_int</code> returns <code>size</code> of the next
   * message without advancing any of the packet buffers position.
   *
   * @return <code>size</code> of the next message. */
    private int peek_int() {
        assert _bytes_available >= ByteUtils.SIZE_INT : "peek_int: must have at least four bytes to peek_int";
        return (int) peek(in_order_packets, ByteUtils.SIZE_INT);
    }

    /**
   * <code>peek_long</code> returns <code>size</code> and <code>xact_id</code>
   * of the next message without advancing any of the packet buffers position.
   *
   * @return <code>size</code> of the next message. */
    private long peek_long() {
        assert _bytes_available >= ByteUtils.SIZE_LONG : "peek_long: must have at least eight bytes to peek_long";
        return peek(in_order_packets, ByteUtils.SIZE_LONG);
    }

    /**
   * <code>peek</code> returns <code>num_peek_bytes</code> of the next
   * message without advancing any of the packet buffers position.
   *
   * @param packets {@link java.util.LinkedList} of packets to peek into.
   * @param num_peek_bytes Number of bytes to peek into.
   * @return <code>size</code> of the next message. */
    private long peek(List<ByteBuffer> packets, int num_peek_bytes) {
        assert num_peek_bytes <= ByteUtils.SIZE_LONG : "peek can only peek at most eight bytes";
        assert packets != null && !packets.isEmpty() : "packets is null or empty";
        long rv = 0L;
        ByteBuffer packet = packets.get(0);
        if (packet.remaining() >= num_peek_bytes && (num_peek_bytes == ByteUtils.SIZE_INT || num_peek_bytes == ByteUtils.SIZE_LONG)) {
            packet.mark();
            rv = (num_peek_bytes == ByteUtils.SIZE_INT ? packet.getInt() : packet.getLong());
            packet.reset();
        } else {
            Iterator<ByteBuffer> i = packets.iterator();
            assert i.hasNext();
            packet = i.next();
            packet.mark();
            int remaining = num_peek_bytes;
            while (remaining > 0) {
                rv = (rv << 8) | (0xff & (long) packet.get());
                remaining--;
                if (!packet.hasRemaining()) {
                    assert i.hasNext() : "no more packets. num_peek_bytes=" + num_peek_bytes + " remaining=" + remaining + ". last packet=" + packet;
                    packet.reset();
                    packet = i.next();
                    packet.mark();
                }
            }
            packet.reset();
        }
        return rv;
    }

    /**
   * <code>read_int</code> returns <code>size</code> of the next
   * message.  packet buffer(s) position will be advanced.
   *
   * @return <code>size</code> of the next message. */
    private int read_int() {
        assert _bytes_available >= ByteUtils.SIZE_INT : "read_int: not enough data available to read_int";
        int rv = 0;
        ByteBuffer packet = in_order_packets.getFirst();
        if (packet.remaining() >= ByteUtils.SIZE_INT) {
            rv = packet.getInt();
            if (!packet.hasRemaining()) in_order_packets.removeFirst();
        } else {
            int remaining = ByteUtils.SIZE_INT;
            while (remaining > 0) {
                rv = (rv << 8) | (0xff & (int) packet.get());
                remaining--;
                if (!packet.hasRemaining()) in_order_packets.removeFirst();
            }
        }
        _bytes_available -= ByteUtils.SIZE_INT;
        return rv;
    }

    /**
   * <code>add_packet</code> adds a {@link java.nio.ByteBuffer packet} to 
   * <code>ConnectionStream</code>'s internal data structures.
   *
   * @param bb {@link java.nio.ByteBuffer packet} to add. */
    public void add_packet(ByteBuffer packet) {
        if (!packet.hasRemaining()) throw new IllegalArgumentException();
        in_order_packets.addLast(packet);
        _bytes_available += packet.remaining();
    }

    /** Specified by java.lang.Object */
    public void finalize() {
        if (in_order_packets != null) in_order_packets.clear();
        in_order_packets = null;
        if (_frags != null) _frags.clear();
        _frags = null;
        if (_frags_end_index != null) _frags_end_index.clear();
        _frags_end_index = null;
        if (_reuse != null) _reuse.clear();
        _reuse = null;
        if (_spliced != null) _spliced.clear();
        _spliced = null;
        if (_splicedPackets != null) _splicedPackets.clear();
        _splicedPackets = null;
        _splicing = false;
        _first_sighting = false;
    }
}
