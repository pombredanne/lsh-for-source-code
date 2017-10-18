package com.ericsson.xsmp.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class CommonUtil {

    public static String nullToString(String sInput) {
        if (sInput == null) return ""; else return sInput;
    }

    /**
	 * 
	 * @param timeFormatStr
	 *            eg:"yyyyMMddHHmmss","yyyyMMdd"
	 * @return
	 */
    public static String getTime(String timeFormatStr) {
        SimpleDateFormat timeFormat = new SimpleDateFormat(timeFormatStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new java.util.Date());
        Date date = calendar.getTime();
        String time = timeFormat.format(date);
        return time;
    }

    public static String createStrWithZeroFromInt(int src, int strLength, boolean preFlag) {
        String result = "";
        result = createStrWithZeroFromStr(Integer.toString(src), strLength, preFlag);
        return result;
    }

    public static String createStrWithZeroFromStr(String src, int strLength, boolean preFlag) {
        StringBuffer result = new StringBuffer();
        if (preFlag) {
            result.append(src);
        }
        int zeroNum = strLength - src.length();
        for (int i = 0; i < zeroNum; i++) {
            result.append("0");
        }
        if (!preFlag) {
            result.append(src);
        }
        return result.toString();
    }

    /**
	 * 将输入的IP转化成对应的8位16进制字符串
	 * 
	 * @param sIp
	 * @return
	 */
    public static String ipToHex(String sIp) {
        String sHexIp = "";
        if (nullToString(sIp).length() > 0) {
            String[] arrIp = sIp.split("\\.");
            if (arrIp.length > 0 && arrIp.length == 4) {
                int iIp = Integer.parseInt(arrIp[0]) * 256 * 256 * 256 + Integer.parseInt(arrIp[1]) * 256 * 256 + Integer.parseInt(arrIp[2]) * 256 + Integer.parseInt(arrIp[3]);
                sHexIp = Integer.toHexString(iIp);
                StringUtility ret = new StringUtility(sHexIp);
                sHexIp = ret.recruitString(8, '0');
            }
        }
        return sHexIp;
    }

    /**
	 * 将输入的8位16进制字符串转化成IP地址
	 * 
	 * @param sHex
	 * @return
	 */
    public static String HexToIp(String sHex) {
        String sIp = "";
        if (nullToString(sHex).length() == 8) {
            String[] arrHex = new String[4];
            for (int i = 0; i < 4; i++) {
                arrHex[i] = sHex.substring(2 * i, 2 * (i + 1));
                Integer iTmp = Integer.decode("0x" + arrHex[i]);
                sIp += iTmp.toString() + ".";
            }
        }
        return sIp.substring(0, sIp.length() - 1);
    }

    /**
	 * 将科学计数法的数字转换为自然数字(包含小数点2位)
	 * 
	 * @param decimal
	 * @return
	 */
    public static String convertDecimalToNumber(Object decimal) {
        String num = "0";
        if (null != decimal) {
            DecimalFormat df = new DecimalFormat("0.##");
            num = String.valueOf(df.format(decimal));
        }
        return num;
    }

    /**
	 * 生成MAC字符串
	 * 
	 * @param decimal
	 * @return
	 */
    public static String cryptoSHA(String _strSrc) {
        try {
            BASE64Encoder encoder = new BASE64Encoder();
            MessageDigest sha = MessageDigest.getInstance("SHA");
            sha.update(_strSrc.getBytes());
            byte[] buffer = sha.digest();
            return encoder.encode(buffer);
        } catch (Exception err) {
            System.out.println(err);
        }
        return "";
    }

    /**
     * 判断该字符串是否为数字
     * @param str
     * @return
     */
    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) return false;
        return true;
    }

    /**
     * 将string进行base64编码
     * @param _src
     * @return
     */
    public static String getBase64(String _src) {
        String _dst = "";
        if (nullToString(_src).length() > 0) _dst = (new BASE64Encoder()).encode(_src.getBytes());
        return _dst;
    }

    /**
     * 将base64编码的字符串进行解码
     * @param _src
     * @return
     */
    public static String getFromBase64(String _src) {
        String _dst = "";
        if (nullToString(_src).length() > 0) {
            try {
                BASE64Decoder decoder = new BASE64Decoder();
                byte[] tmp = decoder.decodeBuffer(_src);
                _dst = new String(tmp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return _dst;
    }

    public static String getShortMSISDN(String msisdn) {
        if (msisdn == null) return null;
        if (msisdn.startsWith("+86")) msisdn = msisdn.substring(3); else if (msisdn.startsWith("86")) msisdn = msisdn.substring(2);
        return msisdn;
    }
}
