package com.myJava.file.delta.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.myJava.util.log.Logger;

public class LinkedList {

    private boolean eof = false;

    private int maxSize;

    private byte[] buffer;

    private int currentIndex = 0;

    private int firstIndex = 0;

    public LinkedList(int maxSize) {
        this.maxSize = maxSize;
        this.buffer = new byte[maxSize];
    }

    public void add(byte data) {
        buffer[currentIndex] = data;
        currentIndex++;
        if (currentIndex == maxSize) {
            currentIndex = 0;
            firstIndex = 0;
            eof = true;
        } else if (eof) {
            firstIndex = currentIndex;
        }
    }

    public int getFirst() {
        return buffer[firstIndex];
    }

    public int computeQuickHash() {
        if (eof) {
            int hash = HashTool.hash(0, buffer, firstIndex, maxSize - firstIndex);
            return HashTool.hash(hash, buffer, 0, firstIndex);
        } else {
            return HashTool.hash(0, buffer, 0, currentIndex);
        }
    }

    public byte[] computeHash(String algorithm) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
            if (eof) {
                digest.update(buffer, firstIndex, maxSize - firstIndex);
                digest.update(buffer, 0, firstIndex);
            } else {
                digest.update(buffer, 0, currentIndex);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            Logger.defaultLogger().error(e);
            return null;
        }
    }

    public String toString() {
        int s;
        if (eof) {
            s = maxSize;
        } else {
            s = currentIndex;
        }
        byte[] dt = new byte[s];
        for (int i = 0; i < s; i++) {
            dt[i] = buffer[(firstIndex + i) % maxSize];
        }
        String ret = "";
        for (int i = 0; i < dt.length; i++) {
            ret += " " + (int) dt[i];
        }
        return ret;
    }

    public static void main(String[] args) {
        int maxSize = 23;
        int maxValue = 200;
        LinkedList lst = new LinkedList(maxSize);
        for (int i = 1; i < maxValue; i++) {
            lst.add((byte) i);
            System.out.println(lst.toString());
            System.out.println(lst.getFirst());
        }
    }
}
