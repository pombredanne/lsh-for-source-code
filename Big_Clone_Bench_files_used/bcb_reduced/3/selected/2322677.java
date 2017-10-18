package fi.hiit.cutehip.packet;

import java.net.InetAddress;
import java.util.Arrays;
import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.security.PrivateKey;
import org.savarese.vserv.tcpip.IPPacket;
import fi.hiit.cutehip.utils.HostIdentityTag;
import fi.hiit.cutehip.utils.Puzzle;
import fi.hiit.framework.crypto.HMACSHA1Digest;
import fi.hiit.framework.crypto.SignatureAlgo;
import fi.hiit.framework.crypto.DigestAlgo;
import fi.hiit.framework.utils.Helpers;

/**
 * This class is a building block for all HIP packets, such as I1, R1, I2, R2,
 * UPDATE, etc.
 * <p>
 * This class is inherited from the IPPacket class from the vserv-tcpip library.
 * See {@link IPPacket}
 */
public class HipPacket extends IPPacket {

    public static final short HIP_PROTO = 0x8B;

    public static final short IPPROTO_NONE = 0x3B;

    public static final byte HIP_PACKET_I1 = 0x1;

    public static final byte HIP_PACKET_R1 = 0x2;

    public static final byte HIP_PACKET_I2 = 0x3;

    public static final byte HIP_PACKET_R2 = 0x4;

    public static final byte HIP_PACKET_UPDATE = 0x10;

    public static final byte HIP_PACKET_NOTIFY = 0x11;

    public static final byte HIP_PACKET_CLOSE = 0x12;

    public static final byte HIP_PACKET_CLOSE_ACK = 0x13;

    public static final byte HIP_VERSION = 0x1;

    public static final int HIP_NXT_HDR_OFFSET = 0x0;

    public static final int HIP_NXT_HDR_LEN_OFFSET = 0x1;

    public static final int HIP_PACKET_TYPE_OFFSET = 0x2;

    public static final int HIP_VERSION_OFFSET = 0x3;

    public static final int HIP_CHECKSUM_OFFSET = 0x4;

    public static final int HIP_CONTROLS_OFFSET = 0x6;

    public static final int HIP_SOURCE_HIT_OFFSET = 0x08;

    public static final int HIP_DEST_HIT_OFFSET = 0x18;

    public static final int HIP_PARAMETERS_OFFSET = 0x28;

    public static final short HIP_CONTROLS_NONE = 0x0;

    public static final short HIP_ZERO_CHKSUM = 0x0;

    public static final int HIT_LENGTH = 0x10;

    public static final int HIP_COMMN_HEADER_LEN = 0x28;

    public static final byte HIP_I1_LENGTH = 0x4;

    /**
	 * Creates a new HIP packet of a given size.
	 * 
	 * @param size
	 *            The number of bytes in the packet.
	 */
    public HipPacket(int size) {
        super(size);
        __offset = 0;
    }

    /**
	 * Creates a new HIP packet that is a copy of a given packet. cd
	 * 
	 * @param packet
	 *            The packet to replicate.
	 */
    public HipPacket(HipPacket packet) {
        super(packet.size());
        copy(packet);
        __offset = packet.__offset;
    }

    /**
	 * Copies the contents of a HipPacket. If the current data array is of
	 * insufficient length to store the contents, a new array is allocated.
	 * 
	 * @param packet
	 *            The HIPPacket to copy.
	 */
    public final void copyData(HipPacket packet) {
        if (_data_.length < packet._data_.length) {
            byte[] data = new byte[packet._data_.length];
            _data_ = data;
        }
    }

    /**
	 * Copies the contents of a IPPacket. If the current data array is of
	 * insufficient length to store the contents, a new array is allocated.
	 * Basically this is handy for prepending the HIP packet with the IP header
	 * 
	 * @param packet
	 *            The IPPacket to copy.
	 */
    public final void copyData(IPPacket packet) {
        if (_data_.length < packet.size()) {
            return;
        }
        byte[] buff = new byte[packet.size()];
        packet.getData(buff);
        System.arraycopy(buff, 0, _data_, 0, buff.length);
        setIPHeaderLength(packet.getIPHeaderLength());
    }

    public void setData(byte[] data) {
        super.setData(data);
        __offset = getIPHeaderByteLength();
    }

    public void setIPHeaderLength(int length) {
        super.setIPHeaderLength(length);
        __offset = getIPHeaderByteLength();
    }

    public int getHipNextHeader() {
        return (_data_[__offset + HIP_NXT_HDR_OFFSET] & 0xFF);
    }

    public void setHipNextHeader(int nextHdr) {
        _data_[__offset + HIP_NXT_HDR_OFFSET] = (byte) (nextHdr & 0xFF);
    }

    public final int getHIPLength() {
        return (_data_[__offset + HIP_NXT_HDR_LEN_OFFSET] & 0xFF);
    }

    public void setHIPLength(byte nextHdrLen) {
        _data_[__offset + HIP_NXT_HDR_LEN_OFFSET] = (byte) (nextHdrLen & 0xFF);
    }

    public int getPacketType() {
        return (_data_[__offset + HIP_PACKET_TYPE_OFFSET] & 0x7F);
    }

    public void setPacketType(byte type) {
        _data_[__offset + HIP_PACKET_TYPE_OFFSET] = (byte) (type & 0x7F);
    }

    public int getHIPVersion() {
        return ((_data_[__offset + HIP_VERSION_OFFSET] & 0xF0) >> 4);
    }

    public void setHIPVersion(byte version) {
        _data_[__offset + HIP_VERSION_OFFSET] |= (byte) (((version & 0x0F) << 4) | (0x1));
    }

    public int getHIPChecksum() {
        return (((_data_[__offset + HIP_CHECKSUM_OFFSET] & 0xFF) << 8) | (_data_[__offset + HIP_CHECKSUM_OFFSET + 1] & 0xFF));
    }

    public void setHIPChecksum(int checksum) {
        _data_[__offset + HIP_CHECKSUM_OFFSET] = (byte) ((checksum >> 8) & 0xFF);
        _data_[__offset + HIP_CHECKSUM_OFFSET + 1] = (byte) (checksum & 0xFF);
    }

    public void setHIPChecksum() {
        setHIPChecksum(__computeChecksum());
    }

    public short getControls() {
        return (short) (((_data_[__offset + HIP_CONTROLS_OFFSET] & 0xFF) << 8) | (_data_[__offset + HIP_CONTROLS_OFFSET + 1] & 0xFF));
    }

    public void setControls(short controls) {
        _data_[__offset + HIP_CONTROLS_OFFSET] = (byte) ((controls >> 8) & 0xFF);
        _data_[__offset + HIP_CONTROLS_OFFSET + 1] = (byte) (controls & 0xFF);
    }

    public void getSourceHit(byte[] address) {
        if (address == null) return;
        if (address.length < HIT_LENGTH) return;
        if (_data_.length < (__offset + HIP_SOURCE_HIT_OFFSET + HIT_LENGTH)) return;
        System.arraycopy(_data_, __offset + HIP_SOURCE_HIT_OFFSET, address, 0, HIT_LENGTH);
    }

    public void setSourceHit(byte[] address) {
        if (address == null) return;
        if (address.length < HIT_LENGTH) return;
        System.arraycopy(address, 0, _data_, __offset + HIP_SOURCE_HIT_OFFSET, HIT_LENGTH);
    }

    public void getDestinationHit(byte[] address) {
        if (address == null) return;
        if (address.length < HIT_LENGTH) return;
        if (_data_.length < (__offset + HIP_DEST_HIT_OFFSET + HIT_LENGTH)) return;
        System.arraycopy(_data_, __offset + HIP_DEST_HIT_OFFSET, address, 0, HIT_LENGTH);
    }

    public void setDestinationHit(byte[] address) {
        if (address == null) return;
        if (address.length < HIT_LENGTH) return;
        System.arraycopy(address, 0, _data_, __offset + HIP_DEST_HIT_OFFSET, HIT_LENGTH);
    }

    public HipParameter getParameter(int type) {
        int param = 0;
        int length = 0;
        if (__offset == 0) __offset = getIPHeaderByteLength();
        int offset = __offset + HIP_COMMN_HEADER_LEN;
        HipParameter parameter = null;
        while ((parameter = getNextParameter(offset)) != null) {
            if (parameter.getType() == type) break;
            offset += parameter.size();
        }
        if (parameter == null) return null;
        byte[] data = new byte[parameter.size()];
        return parameter;
    }

    public HipParameter getNextParameter(int offset) {
        int param = 0;
        int length = 0;
        HipParameter parameter = null;
        if (offset >= _data_.length) return null;
        param = (((_data_[offset] & 0xFF) << 8) | (_data_[offset + 1] & 0xFF));
        length = (((_data_[offset + HipParameter.LENGTH_OFFSET] & 0xFF) << 8) | (_data_[offset + HipParameter.LENGTH_OFFSET + 1] & 0xFF));
        length += HipParameter.PARAM_COMMON_HDR_LEN;
        if (length % HipParameter.MULTIPLE > 0) length += (HipParameter.MULTIPLE - length % HipParameter.MULTIPLE);
        parameter = HipParameterFactoryImpl.createHipParameter((param & 0xFFFF));
        if (parameter == null) return null;
        byte[] data = new byte[length];
        System.arraycopy(_data_, offset, data, 0, length);
        parameter.setData(data);
        return parameter;
    }

    public void setParameter(HipParameter parameter) {
        byte[] data = new byte[_data_.length + parameter.size()];
        byte[] paramData = new byte[parameter.size()];
        System.arraycopy(_data_, 0, data, 0, _data_.length);
        parameter.getData(paramData);
        System.arraycopy(paramData, 0, data, _data_.length, paramData.length);
        setData(data);
        setHIPLength((byte) (getHIPLength() + parameter.size() / 8));
    }

    public void setParameter(HipParameter parameter, boolean update) {
        if (!update) {
            setParameter(parameter);
        } else {
            HipParameter next = null;
            int offset = __offset + HIP_COMMN_HEADER_LEN;
            while ((next = getNextParameter(offset)) != null) {
                if (next.getType() == parameter.getType()) break;
                offset += next.size();
            }
            if (next == null) {
                setParameter(parameter);
            } else {
                if (next.size() == parameter.size()) {
                    System.arraycopy(parameter.__data, 0, _data_, offset, parameter.size());
                } else {
                    byte[] __buf = new byte[_data_.length + (parameter.size() - next.size())];
                    System.arraycopy(_data_, 0, __buf, 0, offset);
                    System.arraycopy(parameter.__data, 0, __buf, offset, parameter.size());
                    System.arraycopy(_data_, offset + next.size(), __buf, offset + parameter.size(), _data_.length - offset - next.size());
                    setData(__buf);
                    setHIPLength((byte) ((_data_.length - __offset - 8) / 8));
                }
            }
        }
    }

    private final int __getVirtualHeaderTotal() {
        int sum = 0;
        if (getIPVersion() == IP_VERSION4) {
            sum = ((_data_[OFFSET_SOURCE_ADDRESS] & 0xff) << 8) | (_data_[OFFSET_SOURCE_ADDRESS + 1] & 0xff);
            sum += ((_data_[OFFSET_SOURCE_ADDRESS + 2] & 0xff) << 8) | (_data_[OFFSET_SOURCE_ADDRESS + 3] & 0xff);
            sum += ((_data_[OFFSET_DESTINATION_ADDRESS] & 0xff) << 8) | (_data_[OFFSET_DESTINATION_ADDRESS + 1] & 0xff);
            sum += ((_data_[OFFSET_DESTINATION_ADDRESS + 2] & 0xff) << 8) | (_data_[OFFSET_DESTINATION_ADDRESS + 3] & 0xff);
            sum += getProtocol();
            sum += (getHIPLength() + 1) * 8;
        } else {
            sum += ((_data_[OFFSET_SOURCE_ADDRESS6] & 0xff) << 8) | (_data_[OFFSET_SOURCE_ADDRESS6 + 1] & 0xff);
            sum += ((_data_[OFFSET_SOURCE_ADDRESS6 + 2] & 0xff) << 8) | (_data_[OFFSET_SOURCE_ADDRESS6 + 3] & 0xff);
            sum += ((_data_[OFFSET_SOURCE_ADDRESS6 + 4] & 0xff) << 8) | (_data_[OFFSET_SOURCE_ADDRESS6 + 5] & 0xff);
            sum += ((_data_[OFFSET_SOURCE_ADDRESS6 + 6] & 0xff) << 8) | (_data_[OFFSET_SOURCE_ADDRESS6 + 7] & 0xff);
            sum += ((_data_[OFFSET_DESTINATION_ADDRESS6] & 0xff) << 8) | (_data_[OFFSET_DESTINATION_ADDRESS6 + 1] & 0xff);
            sum += ((_data_[OFFSET_DESTINATION_ADDRESS6 + 2] & 0xff) << 8) | (_data_[OFFSET_DESTINATION_ADDRESS6 + 3] & 0xff);
            sum += ((_data_[OFFSET_DESTINATION_ADDRESS6 + 4] & 0xff) << 8) | (_data_[OFFSET_DESTINATION_ADDRESS6 + 5] & 0xff);
            sum += ((_data_[OFFSET_DESTINATION_ADDRESS6 + 6] & 0xff) << 8) | (_data_[OFFSET_DESTINATION_ADDRESS6 + 7] & 0xff);
            sum += (getHIPLength() + 1) * 8;
            sum += getHipNextHeader();
        }
        return sum;
    }

    private short __computeChecksum() {
        long sum = __getVirtualHeaderTotal();
        int index = __offset;
        while (index < _data_.length) {
            sum += ((_data_[index] & 0xff) << 8) | (_data_[index + 1] & 0xff);
            index += 2;
        }
        while ((sum >> 16) != 0) {
            sum = (sum & 0xffff) + (sum >> 16);
        }
        return (short) (~sum);
    }

    public static boolean verifySignature(HipPacket packet, SignatureAlgo algo, PublicKey key) {
        byte[] signature = null;
        byte[] sigData = null;
        byte[] puzzle = new byte[PuzzleParameter.PUZZLE_LENGTH];
        byte[] hit = new byte[HostIdentityTag.TAGLENGTH];
        short opaque = 0;
        int length = 0;
        int offset = packet.__offset + HIP_COMMN_HEADER_LEN;
        boolean signature2 = false;
        HipParameter param = null;
        PuzzleParameter puzzleParam = null;
        length = packet.getHIPLength();
        packet.setHIPChecksum(0);
        while ((param = packet.getNextParameter(offset)) != null) {
            if (param instanceof SignatureParameter) {
                if (((SignatureParameter) param).getType() == SignatureParameter.TYPE_SIGNATURE) {
                    signature = new byte[((SignatureParameter) param).getLength() - 1];
                    ((SignatureParameter) param).getSignature(signature);
                    break;
                } else if (((SignatureParameter) param).getType() == Signature2Parameter.TYPE_SIGNATURE2) {
                    signature = new byte[((Signature2Parameter) param).getLength() - 1];
                    ((Signature2Parameter) param).getSignature(signature);
                    packet.getDestinationHit(hit);
                    packet.setDestinationHit(HostIdentityTag.NULL_HIT);
                    puzzleParam = (PuzzleParameter) packet.getParameter(PuzzleParameter.TYPE_PUZZLE);
                    puzzleParam.getPuzzle(puzzle);
                    opaque = puzzleParam.getOpaque();
                    puzzleParam.setOpaque((short) 0);
                    puzzleParam.setPuzzle((short) 0);
                    packet.setParameter(puzzleParam, true);
                    signature2 = true;
                    break;
                }
            }
            offset += param.size();
        }
        packet.setHIPLength((byte) ((offset - 8 - packet.__offset) / 8));
        sigData = new byte[offset - packet.__offset];
        System.arraycopy(packet._data_, packet.__offset, sigData, 0, offset - packet.__offset);
        packet.setHIPLength((byte) length);
        if (signature2) {
            packet.setSourceHit(hit);
            puzzleParam.setOpaque(opaque);
            puzzleParam.setPuzzle(puzzle);
            packet.setParameter(puzzleParam, true);
        }
        return algo.verify(signature, sigData, key);
    }

    public static byte[] computeSignature(HipPacket packet, SignatureAlgo algo, PrivateKey key) {
        int length = 0;
        short opaque = 0;
        int offset = packet.__offset + HIP_COMMN_HEADER_LEN;
        byte[] sigData;
        byte[] hit = new byte[HostIdentityTag.TAGLENGTH];
        byte[] puzzle = new byte[PuzzleParameter.PUZZLE_LENGTH];
        boolean signature2 = false;
        PuzzleParameter puzzleParam = null;
        HipParameter param = null;
        length = packet.getHIPLength();
        packet.setHIPChecksum(0);
        while ((param = packet.getNextParameter(offset)) != null) {
            if (param instanceof SignatureParameter) {
                if (((SignatureParameter) param).getType() == SignatureParameter.TYPE_SIGNATURE) {
                    break;
                } else if (((SignatureParameter) param).getType() == Signature2Parameter.TYPE_SIGNATURE2) {
                    packet.getDestinationHit(hit);
                    packet.setDestinationHit(HostIdentityTag.NULL_HIT);
                    puzzleParam = (PuzzleParameter) packet.getParameter(PuzzleParameter.TYPE_PUZZLE);
                    puzzleParam.getPuzzle(puzzle);
                    opaque = puzzleParam.getOpaque();
                    puzzleParam.setOpaque((short) 0);
                    puzzleParam.setPuzzle((short) 0);
                    packet.setParameter(puzzleParam, true);
                    signature2 = true;
                    break;
                }
            }
            offset += param.size();
        }
        packet.setHIPLength((byte) ((offset - 8 - packet.__offset) / 8));
        sigData = new byte[offset - packet.__offset];
        System.arraycopy(packet._data_, packet.__offset, sigData, 0, offset - packet.__offset);
        packet.setHIPLength((byte) length);
        if (signature2) {
            packet.setDestinationHit(hit);
            puzzleParam.setOpaque(opaque);
            puzzleParam.setPuzzle(puzzle);
            packet.setParameter(puzzleParam, true);
        }
        return algo.sign(sigData, key);
    }

    public static boolean verifyHMAC(HipPacket packet, HostIDParameter hi, DigestAlgo algo) {
        int offset = packet.__offset + HIP_COMMN_HEADER_LEN;
        byte[] hmac = null;
        HipParameter param = null;
        while ((param = packet.getNextParameter(offset)) != null) {
            if (param instanceof HMAC2Parameter) {
                hmac = new byte[((HMAC2Parameter) param).getLength()];
                ((HMAC2Parameter) param).getHMAC(hmac);
                break;
            } else if (param instanceof HMACParameter) {
                hmac = new byte[((HMACParameter) param).getLength()];
                ((HMACParameter) param).getHMAC(hmac);
                break;
            }
            offset += param.size();
        }
        return Arrays.equals(hmac, computeHMAC(packet, hi, algo));
    }

    public static byte[] computeHMAC(HipPacket packet, HostIDParameter hi, DigestAlgo algo) {
        int length = 0;
        int offset = packet.__offset + HIP_COMMN_HEADER_LEN;
        boolean isHMAC2 = false;
        byte[] hmacData;
        HipParameter param = null;
        length = packet.getHIPLength();
        packet.setHIPChecksum(0);
        while ((param = packet.getNextParameter(offset)) != null) {
            if (param instanceof HMAC2Parameter) {
                offset += hi.size();
                isHMAC2 = true;
                break;
            } else if (param instanceof HMACParameter) {
                break;
            }
            offset += param.size();
        }
        packet.setHIPLength((byte) ((offset - 8 - packet.__offset) / 8));
        hmacData = new byte[offset - packet.__offset];
        System.arraycopy(packet._data_, packet.__offset, hmacData, 0, offset - packet.__offset - (isHMAC2 ? hi.size() : 0));
        if (isHMAC2) hi.getData(hmacData, hmacData.length - hi.size(), HipParameter.PARAM_HEADER_OFFSET + hi.getLength());
        packet.setHIPLength((byte) length);
        return algo.digest(hmacData);
    }

    public String dump() {
        InetAddress dst = null;
        InetAddress src = null;
        byte[] dst4 = new byte[4];
        byte[] dst6 = new byte[16];
        byte[] src4 = new byte[4];
        byte[] src6 = new byte[16];
        byte[] dstHit = new byte[16];
        byte[] srcHit = new byte[16];
        StringBuffer buff = new StringBuffer();
        try {
            if (isIPv4()) {
                getDestination(dst4);
                dst = InetAddress.getByAddress(dst4);
                getSource(src4);
                src = InetAddress.getByAddress(src4);
            } else {
                getDestination(dst6);
                dst = InetAddress.getByAddress(dst6);
                getSource(src6);
                src = InetAddress.getByAddress(src6);
            }
            getDestinationHit(dstHit);
            getSourceHit(srcHit);
            buff.append("*********DUMP********\n");
            buff.append("\t Dst : " + dst.getHostAddress() + "\n");
            buff.append("\t Src : " + src.getHostAddress() + "\n");
            buff.append("\t Ver : " + getIPVersion() + "\n");
            buff.append("\t Proto : " + getProtocol() + "\n");
            buff.append("\t Flags : " + getIPFlags() + "\n");
            buff.append("\t TTL : " + getTTL() + "\n");
            buff.append("\t Checksum : " + getIPChecksum() + "\n");
            buff.append("\t IP ID : " + getIdentification() + "\n");
            buff.append("\t Header Length (32 bit words) : " + getIPHeaderLength() + "\n");
            buff.append("\t Total Length (bytes): " + getIPPacketLength() + "\n");
            buff.append("\t HIP version : " + getHIPVersion() + "\n");
            buff.append("\t HIP header length : " + getHIPLength() + "\n");
            buff.append("\t HIP next header : " + getHipNextHeader() + "\n");
            buff.append("\t HIP packet type : " + getPacketType() + "\n");
            buff.append("\t HIP packet checksum : " + getHIPChecksum() + "\n");
            buff.append("\t Source HIT: " + InetAddress.getByAddress(srcHit).getHostAddress() + "\n");
            buff.append("\t Destination HIT: " + InetAddress.getByAddress(dstHit).getHostAddress() + "\n");
            buff.append("\t HIP controls : " + getControls() + "\n");
            buff.append("\t Offset in bytes : " + __offset + "\n");
            buff.append("*********DUMP********\n");
        } catch (Exception e) {
        }
        return buff.toString();
    }

    int __offset;
}
