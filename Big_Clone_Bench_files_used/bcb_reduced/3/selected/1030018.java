package cn.imgdpu.fetion;

import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetionSocket implements Runnable {

    public Thread thread;

    public String sid;

    public String phone;

    public String tophone;

    public String domain = "fetion.com.cn";

    public String passwd;

    public String msg;

    public String sipc_proxy = "221.130.46.141:8080";

    public static String ssi_app_sign_in;

    public static String ssi_app_sign_out;

    public int errCount;

    public String errMsg;

    public final String argsStr = "<args><device type=\"PC\" version=\"33\" client-version=\"3.3.0370\" /><caps value=\"simple-im;im-session;temp-group;personal-group\" /><events value=\"contact;permission;system-message;personal-group\" /><user-info attributes=\"all\" /><presence><basic value=\"400\" desc=\"\" /></presence></args>";

    public Socket socket;

    public DataInputStream dis;

    public DataOutputStream dos;

    byte[] byte1 = new byte[4];

    String tempStr = "";

    private static final String hexDigits[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

    public FetionSocket() {
        errCount = 0;
        errMsg = "";
    }

    public static ArrayList<String> centerStr(String sourceStr, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(sourceStr);
        ArrayList<String> list = new ArrayList<String>();
        if (matcher.find()) for (int i = 0; i <= matcher.groupCount(); i++) list.add(matcher.group(i));
        return list;
    }

    public static String getResponseStr(String _sid, String _passwd, String _domain, String _nonce, String _cnonce) {
        String md5Str = _sid + ":" + _domain + ":" + _passwd;
        String md5Str2 = md5Str;
        md5Str = ":" + _nonce + ":" + _cnonce;
        String H1 = computeH1(md5Str2, md5Str);
        md5Str = "REGISTER:" + _sid;
        String H2 = MD5Encode(md5Str);
        md5Str = H1 + ":" + _nonce + ":" + H2;
        return MD5Encode(md5Str);
    }

    public static String byteArrayToHexString(byte b[]) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) resultSb.append(byteToHexString(b[i]));
        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return (new StringBuilder()).append(hexDigits[d1]).append(hexDigits[d2]).toString();
    }

    public static String MD5Encode(String origin) {
        String resultString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToHexString(md.digest(origin.getBytes("utf-8")));
        } catch (Exception ex) {
            cn.imgdpu.util.CatException.getMethod().catException(ex, "未知异常");
        }
        return resultString;
    }

    public static String computeH1(String s1, String s2) {
        String res = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte key[] = md.digest(s1.getBytes("utf-8"));
            byte ss[] = s2.getBytes("utf-8");
            byte t[] = new byte[key.length + ss.length];
            for (int i = 0; i < key.length; i++) t[i] = key[i];
            for (int i = 0; i < ss.length; i++) t[key.length + i] = ss[i];
            res = byteArrayToHexString(md.digest(t));
        } catch (Exception e) {
            cn.imgdpu.util.CatException.getMethod().catException(e, "未知异常");
        }
        return res;
    }

    @SuppressWarnings("deprecation")
    private void stopThread() {
        thread.stop();
    }

    public void init() throws IOException {
        ArrayList<String> loginInfo;
        loginInfo = new FetionInit().InitAction(phone, passwd);
        sipc_proxy = loginInfo.get(0);
        ssi_app_sign_in = loginInfo.get(1);
        ssi_app_sign_out = loginInfo.get(2);
        sid = loginInfo.get(3);
    }

    public void login() throws Exception {
        String[] sipc_proxyArr = sipc_proxy.split(":");
        socket = new Socket(sipc_proxyArr[0], Integer.parseInt(sipc_proxyArr[1]));
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        String[] login_request = new String[2];
        String[] login = new String[2];
        login_request[0] = "R " + domain + " SIP-C/2.0\r\nF: " + sid + "\r\nI: 1\r\nQ: 1 R\r\nL: " + argsStr.length() + "\r\n\r\n";
        login[0] = login_request[0] + argsStr;
        dos.write(login[0].getBytes());
        dos.flush();
        do {
            dis.read(byte1, 0, 4);
            tempStr += new String(byte1, "utf-8");
        } while (!tempStr.contains("\r\n\r\n"));
        String nonce = centerStr(tempStr, "nonce=\"(.*?)\"").get(1);
        String cnonce = "7036EA07568E7C4D6D49FD76141062FE";
        String response = getResponseStr(sid, passwd, domain, nonce, cnonce);
        login_request[1] = "R " + domain + " SIP-C/2.0\r\nF: " + sid + "\r\nI: 1\r\nQ: 2 R\r\nA: Digest response=\"" + response + "\",cnonce=\"" + cnonce + "\"\r\nL: " + argsStr.length() + "\r\n\r\n";
        login[1] = login_request[1] + argsStr;
        dos.write(login[1].getBytes(), 0, login[1].length());
        tempStr = "";
        do {
            dis.read(byte1, 0, 4);
            tempStr += new String(byte1, "utf-8");
        } while (!tempStr.contains("\r\n\r\n"));
        int slen = Integer.parseInt(centerStr(tempStr, "L: (\\d+)").get(1));
        byte[] byte2 = new byte[40960];
        dis.read(byte2, 0, slen);
        tempStr += new String(byte2, "utf-8");
    }

    public void sendMsg(String msg) throws Exception {
        byte[] bytemsg = msg.getBytes("utf-8");
        String sendMsg = "M " + domain + " SIP-C/2.0\r\nF: " + sid + "\r\nI: 2\r\nQ: 1 M\r\nT: tel:" + tophone + "\r\nN: SendSMS\r\nL: " + (bytemsg.length) + "\r\n\r\n";
        sendMsg = sendMsg + new String(bytemsg, "utf-8");
        dos.write(sendMsg.getBytes("utf-8"));
        tempStr = "";
        do {
            dis.read(byte1, 0, 4);
            tempStr += new String(byte1, "utf-8");
        } while (!tempStr.contains("\r\n\r\n"));
    }

    public void socketClose() throws IOException {
        dis.close();
        dos.close();
        socket.close();
    }

    @Override
    public void run() {
        try {
            init();
        } catch (Exception e) {
            errCount++;
            errMsg += "初始化出错";
            stopThread();
            cn.imgdpu.util.CatException.getMethod().catException(e, "未知异常");
        }
        try {
            login();
        } catch (Exception e) {
            errCount++;
            errMsg = "登陆失败";
            stopThread();
            cn.imgdpu.util.CatException.getMethod().catException(e, "未知异常");
        }
        try {
            sendMsg(msg);
        } catch (Exception e) {
            errCount++;
            errMsg = "发送短信失败";
            stopThread();
            cn.imgdpu.util.CatException.getMethod().catException(e, "未知异常");
        }
        try {
            socketClose();
        } catch (Exception e) {
            cn.imgdpu.util.CatException.getMethod().catException(e, "未知异常");
        }
    }
}
