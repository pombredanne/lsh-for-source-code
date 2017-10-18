package com.solido.objectkitchen.data;

import java.io.*;
import java.security.*;

public class DataElement {

    public static int CHAR = 1000;

    public static int VARCHAR = 1001;

    public static int BITSET = 1002;

    public static int VARBITSET = 1003;

    public static int NUMERIC = 1004;

    public static int DECIMAL = 1005;

    public static int INTEGER = 1006;

    public static int SMALLINT = 1007;

    public static int FLOAT = 1008;

    public static int DATE = 1009;

    public static int TIME = 1010;

    public static int TIMESTAMP = 1011;

    public static int INTERVAL = 1012;

    public static int BIGINT = 2000;

    public static int SERIAL = 2001;

    public static int BIGSERIAL = 2002;

    public static int TEXT = 3000;

    public static int GUID = 3001;

    public static int BLOB = 4000;

    public static int BOOLEAN = 4001;

    public static int INETADR = 6000;

    public static int MACADR = 6001;

    public static int POINTER = 7000;

    int type;

    boolean isnull;

    String string_data;

    int int_data;

    long long_data;

    float float_data;

    double double_data;

    boolean boolean_data;

    private static final char[] base64table = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };

    private static final char[] hextable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private static final long firsttime = System.currentTimeMillis();

    private static long guidcount = 0;

    private static String guidcount_sync = "GUID Sync Dummy";

    public DataElement(int type) {
        this.type = type;
        isnull = true;
    }

    public DataElement(int type, String data) {
        this.type = type;
        isnull = false;
        string_data = data;
    }

    public DataElement(int type, int data) {
        this.type = type;
        isnull = false;
        int_data = data;
    }

    public DataElement(int type, long data) {
        this.type = type;
        isnull = false;
        long_data = data;
    }

    public DataElement(int type, float data) {
        this.type = type;
        isnull = false;
        float_data = data;
    }

    public DataElement(int type, double data) {
        this.type = type;
        isnull = false;
        double_data = data;
    }

    public DataElement(int type, boolean data) {
        this.type = type;
        isnull = false;
        boolean_data = data;
    }

    private static String base64Encode(byte[] data) {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < data.length / 3; i++) {
            int buf = data[i] << 16;
            buf += data[i + 1] << 8;
            buf += data[i + 2];
            str.append(base64table[buf & '?']);
            str.append(base64table[(buf >> 6) & '?']);
            str.append(base64table[(buf >> 12) & '?']);
            str.append(base64table[(buf >> 18) & '?']);
        }
        if (data.length % 3 > 0) {
            if (data.length % 3 == 1) {
                int buf = data[data.length - 2];
                str.append(base64table[buf & '?']);
                str.append(base64table[(buf >> 6) & '?']);
                str.append("==");
            } else {
                int buf = data[data.length - 3] << 8;
                buf += data[data.length - 2];
                str.append(base64table[buf & '?']);
                str.append(base64table[(buf >> 6) & '?']);
                str.append(base64table[(buf >> 12) & '?']);
                str.append("=");
            }
        }
        return str.toString();
    }

    private static String hexEncode(byte[] data) {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            str.append(hextable[(data[i] + 128) / 16]);
            str.append(hextable[(data[i] + 128) % 16]);
        }
        return str.toString();
    }

    public static DataElement createMD5Sum(int type, String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data.getBytes());
            byte[] dt = md.digest();
            return new DataElement(type, hexEncode(dt));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DataElement(type);
    }

    public static DataElement createGUID() {
        StringBuffer buf = new StringBuffer();
        long time = System.currentTimeMillis();
        String ln = new StringBuffer(Long.toHexString(time)).reverse().toString() + "000000000000";
        long cnt = 0;
        synchronized (guidcount_sync) {
            cnt = guidcount++;
        }
        String cn = new StringBuffer(Long.toHexString(cnt)).reverse().toString() + "000000000000";
        String ci = new StringBuffer(Long.toHexString(firsttime)).reverse().toString() + "000000000000";
        buf.append(ln.substring(0, 8));
        buf.append("-");
        buf.append(ln.substring(8, 12));
        buf.append("-");
        buf.append(cn.substring(0, 4));
        buf.append("-");
        buf.append(cn.substring(4, 8));
        buf.append("-");
        buf.append(cn.substring(8, 12));
        buf.append(ci.substring(0, 8));
        return new DataElement(GUID, buf.toString());
    }

    public static DataElement parseStream(DataInputStream stream) {
        return null;
    }

    public static DataElement parseBytes(int type, byte[] data) {
        return null;
    }

    public static DataElement parseString(int type, String data) {
        try {
            if (type < 2000) {
                if (type == TIMESTAMP) return new DataElement(type, Long.parseLong(data));
                if (type == INTEGER) return new DataElement(type, Integer.parseInt(data));
            } else if ((type >= 2000) && (type < 3000)) {
            } else if ((type >= 3000) && (type < 4000)) {
                return new DataElement(type, data);
            } else if ((type >= 4000) && (type < 5000)) {
                if (type == BOOLEAN) {
                    if (data.toUpperCase().equals("TRUE")) {
                        return new DataElement(type, true);
                    } else {
                        return new DataElement(type, false);
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public String toString() {
        if (isnull) return "";
        if (type < 2000) {
            if (type == TIMESTAMP) return "" + long_data;
            if (type == INTEGER) return "" + int_data;
        } else if ((type >= 2000) && (type < 3000)) {
        } else if ((type >= 3000) && (type < 4000)) {
            return string_data;
        } else if ((type >= 4000) && (type < 5000)) {
            if (type == BOOLEAN) {
                if (boolean_data) {
                    return "TRUE";
                } else {
                    return "FALSE";
                }
            }
        }
        return "";
    }

    public byte[] toBytes() {
        return new byte[0];
    }

    public boolean toStream(DataOutputStream stream) {
        return false;
    }

    public int size() {
        return 0;
    }

    public int stringSize() {
        return 0;
    }

    public int getType() {
        return type;
    }

    public boolean isNull() {
        return isnull;
    }

    public int getIntValue() {
        return 0;
    }

    public long getLongValue() {
        return 0;
    }

    public float getFloatValue() {
        return 0;
    }

    public double getDoubleValue() {
        return 0;
    }

    public String getStringValue() {
        return "";
    }

    public boolean getBooleanValue() {
        return false;
    }

    public boolean getBitValue(int num) {
        return false;
    }

    public char getCharValue(int num) {
        return ' ';
    }

    public boolean equals(DataElement dt) {
        return false;
    }

    public boolean lessThan(DataElement dt) {
        return false;
    }

    public boolean moreThan(DataElement dt) {
        return false;
    }

    public boolean comparableTo(DataElement dt) {
        return false;
    }
}
