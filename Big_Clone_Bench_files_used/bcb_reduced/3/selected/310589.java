package com.atolsystems.memop;

import com.atolsystems.atolutilities.AArrayUtilities;
import com.atolsystems.atolutilities.AStringUtilities;
import com.atolsystems.atolutilities.ProgrammingChunk;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

/**
 *
 * @author sebastien.riou
 */
public class BitUtilities {

    static void bitCopy(byte[] bitBuffer, int dstPos, int src, int srcPos, int size) {
        int byteIndex = dstPos >> 3;
        int firstByteOffset = dstPos - (byteIndex << 3);
        int srcLastPos = srcPos + size;
        int shiftLeft = Integer.SIZE - srcLastPos;
        int shiftRight = srcPos + shiftLeft;
        int data = (src << shiftLeft) >> shiftRight;
        int mask = ((-1) << shiftLeft) >> shiftRight;
        byte dst = bitBuffer[byteIndex];
        byte toAnd = (byte) (0xFF & (mask << firstByteOffset));
        byte toOr = (byte) (0xFF & (data << firstByteOffset));
        bitBuffer[byteIndex] = (byte) ((dst & toAnd) | toOr);
        int offset = (8 - firstByteOffset) & 0x07;
        size -= 8 - firstByteOffset;
        while (size > 0) {
            byteIndex++;
            mask = mask >> offset;
            data = data >> offset;
            dst = bitBuffer[byteIndex];
            toAnd = (byte) (0xFF & mask);
            toOr = (byte) (0xFF & data);
            bitBuffer[byteIndex] = (byte) ((dst & toAnd) | toOr);
            size -= 8;
        }
    }

    static short bytes2Short(byte[] bytes, int lsbIndex) {
        short out = 0;
        if (lsbIndex + 1 < bytes.length) {
            out = bytes[lsbIndex + 1];
            out = (short) (out << 8);
        }
        if (lsbIndex < bytes.length) out |= 0xFF & bytes[lsbIndex];
        return out;
    }

    public static int zip(int in[]) {
        return zip(in[0], in[1]);
    }

    public static int zip(int a, int b) {
        int ae = 0;
        int be = 0;
        int mask = 1;
        for (int i = 0; i < Integer.SIZE; i++) {
            ae |= (a & mask) << (2 * i);
            be |= (b & mask) << (2 * i);
            mask = mask << 1;
        }
        return ae | (be << 1);
    }

    public static int[] unzip(int in) {
        int a = 0;
        int b = 0;
        int mask = 1;
        for (int i = 0; i < Integer.SIZE; i += 2) {
            a |= (in & mask << (2 * i)) >> (i);
            b |= (in & mask << (2 * i + 1)) >> (i + 1);
            mask = mask << 2;
        }
        int[] out = new int[2];
        out[0] = a;
        out[1] = b;
        return out;
    }

    public static int reverseBits(int in) {
        int out = 0;
        int shiftIn = 0;
        int shiftOut = Integer.SIZE - 1;
        for (int i = 0; i < Integer.SIZE; i++) {
            out |= ((in >> shiftIn) & 0x01) << shiftOut;
            shiftIn++;
            shiftOut--;
        }
        return out;
    }

    public static int reorderBits(int in, int[] indexes) {
        int out = 0;
        for (int i = indexes.length - 1; i > -1; i--) {
            out = (out << 1) | (0x01 & (in >> indexes[i]));
        }
        return out;
    }

    public static boolean[] reorderBits(boolean[] in, int[] indexes) {
        boolean[] out = new boolean[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            out[i] = in[indexes[i]];
        }
        return out;
    }

    public static void reorderBits(boolean[] in, boolean[] out, int[] indexes) {
        if (out.length != indexes.length) throw new RuntimeException();
        for (int i = 0; i < indexes.length; i++) {
            out[i] = in[indexes[i]];
        }
    }

    public static void reorderBits(boolean[] in, boolean[] out, int offset, int[] indexes) {
        if (offset + indexes.length > out.length) throw new RuntimeException();
        for (int i = 0; i < indexes.length; i++) {
            out[offset + i] = in[indexes[i]];
        }
    }

    public static void pBox(boolean[] in, boolean[] out, int offset, int length, long seed) {
        int[] indexes = generateRandomOrder(in.length, length, seed);
        reorderBits(in, out, offset, indexes);
    }

    public static void pBox(boolean[] in, boolean[] out, long seed) {
        int[] indexes = generateRandomOrder(in.length, out.length, seed);
        reorderBits(in, out, indexes);
    }

    public static String generatePboxVerilog(int inLength, int outLength, long seed) {
        int[] indexes = generateRandomOrder(inLength, outLength, seed);
        return generatePboxVerilog(inLength, indexes);
    }

    public static String generatePboxVerilog(int inLength, int[] indexes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);
        int[] frequences = new int[inLength];
        for (int i = 0; i < indexes.length; i++) frequences[indexes[i]]++;
        int minF = Integer.MAX_VALUE;
        int maxF = Integer.MIN_VALUE;
        for (int i = 0; i < inLength; i++) {
            if ((frequences[i] < minF) && (frequences[i] != 0)) minF = frequences[i];
            if (frequences[i] > maxF) maxF = frequences[i];
        }
        writer.print("module Pbox_");
        writer.print(inLength);
        writer.append("_");
        writer.print(indexes.length);
        writer.append("_");
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        md.update(AArrayUtilities.ints2Bytes(indexes), 0, indexes.length * 4);
        byte[] hash = md.digest();
        byte[] smallHash = AArrayUtilities.xor(hash, 0, hash, 10, 10);
        writer.append(AStringUtilities.bytesToHex(smallHash));
        writer.print("(input wire [" + (inLength - 1) + ":0] in,");
        writer.println("output wire [" + (indexes.length - 1) + ":0] out);");
        writer.print("//unused bits: ");
        for (int i = 0; i < inLength; i++) {
            if (frequences[i] == 0) {
                writer.print(i);
                writer.print(" ");
            }
        }
        writer.println();
        writer.print("//bits used the least (");
        writer.print(minF);
        writer.print(" times): ");
        for (int i = 0; i < inLength; i++) {
            if (frequences[i] == minF) {
                writer.print(i);
                writer.print(" ");
            }
        }
        writer.println();
        writer.print("//bits used the most (");
        writer.print(maxF);
        writer.print(" times): ");
        for (int i = 0; i < inLength; i++) {
            if (frequences[i] == maxF) {
                writer.print(i);
                writer.print(" ");
            }
        }
        writer.println();
        writer.append("   assign out = {");
        for (int i = indexes.length - 1; i > 0; i--) {
            writer.append("in[");
            writer.print(indexes[i]);
            writer.print("],");
        }
        writer.append("in[");
        writer.print(indexes[0]);
        writer.println("]};");
        writer.println("endmodule");
        writer.println();
        writer.close();
        return out.toString();
    }

    static HashSet<Long> seedsMixBitsBool = new HashSet<Long>();

    public static byte[] mixBits_byte(byte[] in, long seed) {
        boolean[] inBits = AArrayUtilities.bytes2Bools(in);
        return AArrayUtilities.bools2Bytes(mixBits(inBits, 0, inBits.length, seed));
    }

    public static boolean[] mixBits(byte[] in, long seed) {
        boolean[] inBits = AArrayUtilities.bytes2Bools(in);
        return mixBits(inBits, 0, inBits.length, seed);
    }

    public static boolean[] mixBits(boolean[] in, long seed) {
        return mixBits(in, 0, in.length, seed);
    }

    public static int[] generateRandomOrder(int indexesRange, int length, long seed) {
        int order[] = new int[length];
        Random r = new Random(seed);
        LinkedList<Integer> available = new LinkedList<Integer>();
        int nNumbers = Math.max(length, indexesRange);
        for (int i = 0; i < nNumbers; i++) available.add(i % indexesRange);
        for (int i = 0; i < length; i++) order[i] = available.remove(Math.abs(r.nextInt()) % available.size());
        return order;
    }

    public static int[][] generateRandomOrderSet(int nSet, int indexesRange, int length, long seed) {
        int[][] set = new int[nSet][length];
        Random r = new Random(seed);
        LinkedList<Integer> available = new LinkedList<Integer>();
        int nNumbers = Math.max(length * nSet, indexesRange);
        for (int i = 0; i < nNumbers; i++) available.add(i % indexesRange);
        for (int iSet = 0; iSet < nSet; iSet++) {
            for (int i = 0; i < length; i++) set[iSet][i] = available.remove(Math.abs(r.nextInt()) % available.size());
        }
        return set;
    }

    public static String writeMixBitAndInverseVerilog(String name, int inputLength, int length, long seed) {
        int order[] = seedToOrder(inputLength, 0, length, seed);
        return writeMixBitAndInverseVerilog(name, length, seed, order);
    }

    public static String writeMixBitAndInverseVerilog(String name, int length, long seed, int[] order) {
        String out = writeMixBitVerilog(name, length, seed, order);
        int[] orderInv = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            for (int j = 0; j < order.length; j++) {
                if (order[j] == i) {
                    orderInv[i] = j;
                    break;
                }
            }
        }
        out += writeMixBitVerilog(name + "Inv", length, seed, orderInv);
        return out;
    }

    public static boolean verilog95 = false;

    public static String writeMixBitVerilog(String name, int length, long seed, int[] order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);
        writer.print("module MixBits");
        writer.append(name);
        writer.append("_");
        writer.print(length);
        writer.append("_");
        writer.append(Long.toHexString(seed).toUpperCase());
        if (verilog95) {
            writer.println("(in,out);");
            writer.println("input wire [" + (length - 1) + ":0] in;");
            writer.println("output wire [" + (length - 1) + ":0] out;");
        } else {
            writer.print("(input wire [" + (length - 1) + ":0] in, ");
            writer.println("output wire [" + (length - 1) + ":0] out);");
        }
        writer.append("   assign out = {");
        for (int i = length - 1; i > 0; i--) {
            writer.append("in[");
            writer.print(order[i]);
            writer.print("],");
        }
        writer.append("in[");
        writer.print(order[0]);
        writer.println("]};");
        writer.println("endmodule");
        writer.println();
        writer.close();
        return out.toString();
    }

    public static int[] seedToOrder(int inputLength, int offset, int length, long seed) {
        int order[] = new int[inputLength];
        Random r = new Random(seed);
        LinkedList<Integer> available = new LinkedList<Integer>();
        for (int i = 0; i < order.length; i++) order[i] = i;
        for (int i = offset; i < length; i++) available.add(i + offset);
        for (int i = offset; i < length; i++) order[i] = available.remove(Math.abs(r.nextInt()) % available.size());
        return order;
    }

    public static boolean[] mixBits(boolean[] in, int offset, int length, long seed) {
        int order[] = seedToOrder(in.length, offset, length, seed);
        if (seeds.contains(seed) == false) {
            seeds.add(seed);
            if (null != verilogOut) {
                verilogOut.append(writeMixBitAndInverseVerilog("", length, seed, order));
            }
        }
        return reorderBits(in, order);
    }

    static HashSet<Long> seeds = new HashSet<Long>();

    public static PrintWriter verilogOut;

    public static int mixBits(int in, int nIn, long seed) {
        int order[] = new int[nIn];
        Random r = new Random(seed);
        LinkedList<Integer> available = new LinkedList<Integer>();
        for (int i = 0; i < nIn; i++) available.add(i);
        for (int i = 0; i < nIn; i++) order[i] = available.remove(Math.abs(r.nextInt()) % available.size());
        if (seeds.contains(seed) == false) {
            seeds.add(seed);
            if (null != verilogOut) {
                verilogOut.print("module MixBits_");
                verilogOut.print(nIn);
                verilogOut.append("_");
                verilogOut.append(Long.toHexString(seed).toUpperCase());
                verilogOut.println("(in,out);");
                verilogOut.println("input [" + (nIn - 1) + ":0] in;");
                verilogOut.println("output [" + (nIn - 1) + ":0] out;");
                verilogOut.append("   assign out = {");
                for (int i = nIn - 1; i > 0; i--) {
                    verilogOut.append("in[");
                    verilogOut.print(order[i]);
                    verilogOut.print("],");
                }
                verilogOut.append("in[");
                verilogOut.print(order[0]);
                verilogOut.println("]};");
                verilogOut.println("endmodule");
                verilogOut.println();
            }
        }
        int out = reorderBits(in, order);
        return out;
    }

    public static int mixBits23To16(int in, long seed) {
        return 0xFFFF & mixBits(in, 23, seed);
    }

    public static int mixBits23To8(int in, long seed) {
        return 0xFF & mixBits(in, 23, seed);
    }

    public static boolean combineBits(boolean[] a, boolean[] b, boolean[] c, boolean[] d, boolean[] e, boolean[] f, boolean[] g, boolean[] h, int index) {
        int i = index;
        return (a[i] & b[i]) | (!(c[i] | d[i])) ^ (e[i] & f[i]) | !(g[i] | h[i]);
    }

    public static boolean combineBits2(boolean[] a, boolean[] b, boolean[] c, boolean[] d, boolean[] e, boolean[] f, boolean[] g, boolean[] h, int index) {
        int i = index;
        return a[i] ^ b[i] ^ c[i] ^ (d[i] & e[i]) ^ f[i] ^ g[i] ^ h[i];
    }

    public static boolean combineBits3(boolean[] a, boolean[] b, boolean[] c, boolean[] d, boolean[] e, boolean[] f, boolean[] g, boolean[] h, int index) {
        int i = index;
        return (a[i] & b[i]) ^ (!(c[i] & d[i])) ^ (e[i] & f[i]) ^ !(g[i] & h[i]);
    }

    public static int andSetBit(int in, int nIn, int a, int b, int bitToSet) {
        a = Math.abs(a) % nIn;
        b = Math.abs(b) % nIn;
        bitToSet = Math.abs(bitToSet) % nIn;
        int out = in | ((0x01 & ~((in >> a) & (in >> b))) << bitToSet);
        return out;
    }

    public static int and4SetBit(int in, int nIn, int a, int b, int c, int d, int bitToSet) {
        a = Math.abs(a) % nIn;
        b = Math.abs(b) % nIn;
        c = Math.abs(c) % nIn;
        d = Math.abs(d) % nIn;
        bitToSet = Math.abs(bitToSet) % nIn;
        int out = in | ((0x01 & ~((in >> a) & (in >> b) & (in >> c) & (in >> d))) << bitToSet);
        return out;
    }

    public static int conditionalSetBit(int in, int nIn, int a, int b, int c, int d, int bitToSet) {
        a = Math.abs(a) % nIn;
        b = Math.abs(b) % nIn;
        c = Math.abs(c) % nIn;
        d = Math.abs(d) % nIn;
        bitToSet = Math.abs(bitToSet) % nIn;
        int out = in | ((0x01 & ~((in >> a) & ~(in >> b) & (in >> c) & (in >> d))) << bitToSet);
        return out;
    }

    public static int orClearBit(int in, int nIn, int a, int b, int bitToClear) {
        a = Math.abs(a) % nIn;
        b = Math.abs(b) % nIn;
        bitToClear = Math.abs(bitToClear) % nIn;
        int out = in & ~((0x01 & ((in >> a) | (in >> b))) << bitToClear);
        return out;
    }

    public static boolean arrayAnd(boolean[] in, int a, int b) {
        return in[a % in.length] & in[b % in.length];
    }

    public static boolean[] xorReduction(boolean[] in, int outputWidth) {
        return xorReduction(in, 0, in.length, outputWidth);
    }

    public static boolean[] xorReduction(boolean[] in, int offset, int length, int outputWidth) {
        boolean[] out = new boolean[outputWidth];
        for (int i = 0; i < length; i += outputWidth) {
            for (int j = 0; j < outputWidth; j++) {
                if (offset + i * outputWidth + j > in.length) return out;
                if (i * outputWidth + j > out.length) return out;
                out[i * outputWidth + j] ^= in[offset + i * outputWidth + j];
            }
        }
        return out;
    }
}
