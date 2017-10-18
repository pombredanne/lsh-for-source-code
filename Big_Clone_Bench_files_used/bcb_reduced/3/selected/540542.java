package org.hardtokenmgmt.hosting.web.util;

import java.security.MessageDigest;
import java.util.HashMap;

/**
 * Help classes to the utility that signs a jar with a given key store. Much of the code is inspired by a great
 * article by Raffi Krikorian published by ONJava.com.
 */
public class ManifestDigester {

    public static class Entry {

        public byte[] digest(MessageDigest md) {
            md.reset();
            if (oldStyle) doOldStyle(md, rawBytes, offset, lengthWithBlankLine); else md.update(rawBytes, offset, lengthWithBlankLine);
            return md.digest();
        }

        private void doOldStyle(MessageDigest md, byte bytes[], int offset, int length) {
            int i = offset;
            int start = offset;
            int max = offset + length;
            int prev = -1;
            for (; i < max; i++) {
                if (bytes[i] == 13 && prev == 32) {
                    md.update(bytes, start, i - start - 1);
                    start = i;
                }
                prev = bytes[i];
            }
            md.update(bytes, start, i - start);
        }

        public byte[] digestWorkaround(MessageDigest md) {
            md.reset();
            md.update(rawBytes, offset, length);
            return md.digest();
        }

        int offset;

        int length;

        int lengthWithBlankLine;

        byte rawBytes[];

        boolean oldStyle;

        public Entry(int offset, int length, int lengthWithBlankLine, byte rawBytes[]) {
            this.offset = offset;
            this.length = length;
            this.lengthWithBlankLine = lengthWithBlankLine;
            this.rawBytes = rawBytes;
        }
    }

    static class Position {

        int endOfFirstLine;

        int endOfSection;

        int startOfNext;

        Position() {
        }
    }

    private boolean findSection(int offset, Position pos) {
        int i = offset;
        int len = rawBytes.length;
        int last = offset;
        boolean allBlank = true;
        pos.endOfFirstLine = -1;
        for (; i < len; i++) {
            byte b = rawBytes[i];
            switch(b) {
                case 13:
                    if (pos.endOfFirstLine == -1) pos.endOfFirstLine = i - 1;
                    if (i < len && rawBytes[i + 1] == 10) i++;
                case 10:
                    if (pos.endOfFirstLine == -1) pos.endOfFirstLine = i - 1;
                    if (allBlank || i == len - 1) {
                        if (i == len - 1) pos.endOfSection = i; else pos.endOfSection = last;
                        pos.startOfNext = i + 1;
                        return true;
                    }
                    last = i;
                    allBlank = true;
                    break;
                default:
                    allBlank = false;
                    break;
            }
        }
        return false;
    }

    public ManifestDigester(byte bytes[]) {
        rawBytes = bytes;
        entries = new HashMap<String, Entry>();
        Position pos = new Position();
        if (!findSection(0, pos)) return;
        for (int start = pos.startOfNext; findSection(start, pos); start = pos.startOfNext) {
            int len = (pos.endOfFirstLine - start) + 1;
            int sectionLen = (pos.endOfSection - start) + 1;
            int sectionLenWithBlank = pos.startOfNext - start;
            if (len <= 6 || !isNameAttr(bytes, start)) continue;
            StringBuffer nameBuf = new StringBuffer();
            nameBuf.append(new String(bytes, start + 6, len - 6));
            int i = start + len;
            if (i - start < sectionLen) if (bytes[i] == 13) i += 2; else i++;
            while (i - start < sectionLen && bytes[i++] == 32) {
                int wrapStart = i;
                while (i - start < sectionLen && bytes[i++] != 10) ;
                if (bytes[i - 1] != 10) return;
                int wrapLen;
                if (bytes[i - 2] == 13) wrapLen = i - wrapStart - 2; else wrapLen = i - wrapStart - 1;
                nameBuf.append(new String(bytes, wrapStart, wrapLen));
            }
            String name = nameBuf.toString();
            entries.put(name, new Entry(start, sectionLen, sectionLenWithBlank, rawBytes));
        }
    }

    private boolean isNameAttr(byte bytes[], int start) {
        return (bytes[start] == 78 || bytes[start] == 110) && (bytes[start + 1] == 97 || bytes[start + 1] == 65) && (bytes[start + 2] == 109 || bytes[start + 2] == 77) && (bytes[start + 3] == 101 || bytes[start + 3] == 69) && bytes[start + 4] == 58 && bytes[start + 5] == 32;
    }

    public Entry get(String name, boolean oldStyle) {
        Entry e = (Entry) entries.get(name);
        if (e != null) e.oldStyle = oldStyle;
        return e;
    }

    public byte[] manifestDigest(MessageDigest md) {
        md.reset();
        md.update(rawBytes, 0, rawBytes.length);
        return md.digest();
    }

    byte rawBytes[];

    HashMap<String, Entry> entries;
}
