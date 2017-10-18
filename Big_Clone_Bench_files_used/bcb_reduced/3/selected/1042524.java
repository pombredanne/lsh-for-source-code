package org.hld.avg;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class NScripterMap {

    public static final String TRUE = "1";

    public static final String FALSE = "0";

    Map<String, String> labelMap = new HashMap<String, String>();

    private int number = 0;

    private int text = 0;

    private int music = 0;

    private int image = 0;

    Map<String, String> numberVariableMap = new HashMap<String, String>();

    Map<String, String> textVariableMap = new HashMap<String, String>();

    Map<String, Integer> musicVariableMap = new HashMap<String, Integer>();

    Map<String, Integer> imageVariableMap = new HashMap<String, Integer>();

    public String addLabel(String filename, String labelname) {
        String name = labelname == null ? filename : (filename + "_" + labelname);
        String label = createLabel(name);
        labelMap.put(name, label);
        return label;
    }

    public String getLabel(String filename, String labelname) {
        String name = labelname == null ? filename : (filename + "_" + labelname);
        String label = labelMap.get(name);
        if (label == null) {
            label = createLabel(name);
            labelMap.put(name, label);
        }
        return label;
    }

    private String createLabel(String label) {
        return "*" + md5(label);
    }

    public boolean addNumberVariable(String variablename) {
        String s = numberVariableMap.get(variablename);
        if (s == null) {
            numberVariableMap.put(variablename, createNumberVariable());
            return true;
        }
        return false;
    }

    public String getNumberVariable(String variablename) {
        String s = numberVariableMap.get(variablename);
        if (s == null) {
            s = createNumberVariable();
            numberVariableMap.put(variablename, s);
        }
        return s;
    }

    private String createNumberVariable() {
        return "%" + (number++);
    }

    public boolean addTextVariable(String variablename) {
        String s = textVariableMap.get(variablename);
        if (s == null) {
            textVariableMap.put(variablename, createTextVariable());
            return true;
        }
        return false;
    }

    public String getTextVariable(String variablename) {
        String s = textVariableMap.get(variablename);
        if (s == null) {
            s = createTextVariable();
            textVariableMap.put(variablename, s);
        }
        return s;
    }

    private String createTextVariable() {
        return "$" + (text++);
    }

    public boolean addMusicVariable(String variablename) {
        Integer i = musicVariableMap.get(variablename);
        if (i == null) {
            musicVariableMap.put(variablename, music++);
            return true;
        }
        return false;
    }

    public Integer getMusicVariable(String variablename) {
        Integer i = musicVariableMap.get(variablename);
        if (i == null) {
            i = music++;
            musicVariableMap.put(variablename, i);
        }
        return i;
    }

    public boolean addImageVariable(String variablename) {
        Integer i = imageVariableMap.get(variablename);
        if (i == null) {
            imageVariableMap.put(variablename, image++);
            return true;
        }
        return false;
    }

    public Integer getImageVariable(String variablename) {
        Integer i = imageVariableMap.get(variablename);
        if (i == null) {
            i = image++;
            imageVariableMap.put(variablename, i);
        }
        return i;
    }

    public static String getNumber(String num) {
        if ("true".equals(num)) return TRUE;
        if ("false".equals(num)) return FALSE;
        return num;
    }

    public static String md5(String strSrc) {
        byte[] digest = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            digest = md5.digest(strSrc.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(byteHEX(digest[i]));
        }
        return sb.toString();
    }

    private static String byteHEX(byte byte0) {
        char ac[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        char ac1[] = new char[2];
        ac1[0] = ac[byte0 >>> 4 & 0xf];
        ac1[1] = ac[byte0 & 0xf];
        String s = new String(ac1);
        return s;
    }
}
