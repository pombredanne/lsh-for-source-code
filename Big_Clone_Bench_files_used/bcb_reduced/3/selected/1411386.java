package com.yxl.project.license.server.encryp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <p>Title: 对字符串进行加密
 * <p>Copyright: Copyright 2010 Shenzhen Taiji SoftwareCorparation
 * <p>Company: 深圳太极软件有限公司
 * <p>CreateTime: Dec 7, 2010
 * @author 杨雪令
 * @version 1.0
 */
public class Encryption {

    protected int EN_LENGTH = 14;

    protected int PASSWORD = 1366560;

    protected long passwordContextTime = 0;

    protected boolean isNewPassword = true;

    public Encryption() throws IOException {
        initPassword();
    }

    /**
	 * <p>Description: 获取16位MD5编码
	 * @param str 要处理的字符
	 * @return 经过MD5处理过的字符
	 * <p>Copyright　深圳太极软件公司
	 * @author  杨雪令
	 */
    public String getMd5CodeOf16(String str) {
        StringBuffer buf = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte b[] = md.digest();
            int i;
            buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) i += 256;
                if (i < 16) buf.append("0");
                buf.append(Integer.toHexString(i));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            return buf.toString().substring(8, 24);
        }
    }

    /**
	 * <p>Description: 获取32位MD5编码
	 * @param str 要处理的字符
	 * @return 经过MD5处理过的字符
	 * <p>Copyright　深圳太极软件公司
	 * @author  杨雪令
	 */
    public static String getMd5CodeOf32(String str) {
        MD5Code md5 = new MD5Code();
        String code = md5.getMD5ofStr(str);
        return code;
    }

    /**
	 * Description:通过算法加密
	 * @param str 需要加密的字符串
	 * @param var 变量
	 * Copyright　深圳太极软件公司
	 * @author  杨雪令
	 * @throws IOException 
	 */
    protected String beEnCodeVerify(String str, int var) throws IOException {
        if (EN_LENGTH <= ((PASSWORD + "").length())) EN_LENGTH = ((PASSWORD + "").length()) + 1;
        String newStr = "";
        byte[] b = null;
        try {
            b = str.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < b.length; i++) {
            int temp = b[i] + (i % var + 1) * PASSWORD;
            String temps = temp + "";
            while (temps.length() < EN_LENGTH) {
                temps = "0" + temps;
            }
            newStr += temps;
        }
        String chars = "";
        for (int i = 0; i < newStr.length(); i++) {
            int j = Integer.parseInt(newStr.substring(i, i + 1));
            char c = (char) j;
            chars += c;
        }
        return chars;
    }

    /**
	 * Description: 通过算法解密
	 * @param str 需要加密的字符串
	 * @param var 变量
	 * Copyright　深圳太极软件公司
	 * @author  杨雪令
	 * @throws IOException 
	 */
    public String deCodeVerify(String str, int var) throws IOException {
        if (EN_LENGTH <= ((PASSWORD + "").length())) EN_LENGTH = ((PASSWORD + "").length()) + 1;
        String deStr = "";
        for (int i = 0; i < str.length(); i++) {
            char j = str.substring(i, i + 1).toCharArray()[0];
            int pInt = j;
            deStr += pInt;
        }
        String newStr = "";
        byte[] b = new byte[deStr.length() / EN_LENGTH];
        for (int i = 0; i * EN_LENGTH < deStr.length(); i++) {
            String param = deStr.substring(i * EN_LENGTH, (i + 1) * EN_LENGTH);
            int iParam;
            try {
                iParam = Integer.parseInt(param);
            } catch (NumberFormatException e) {
                System.out.println("文件格式有误！请检查...");
                return "false";
            }
            iParam -= b[i] + (i % var + 1) * PASSWORD;
            b[i] = (byte) iParam;
        }
        try {
            newStr = new String(b, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return newStr;
    }

    /**
	 * <p>Description:加载密码
	 * <p>Copyright　深圳太极软件公司 
	 * @author  杨雪令
	 * @throws IOException 
	 */
    protected int initPassword() throws IOException {
        String path = getClassUrl(this, ".jar", 0);
        path += "verify";
        File passwordfile = new File(path);
        InputStream ins = null;
        if (!passwordfile.exists()) {
            ins = this.getClass().getResourceAsStream("verify");
            if (ins == null) return PASSWORD;
        } else {
            if (passwordContextTime == passwordfile.lastModified()) {
                return PASSWORD;
            }
            ins = new FileInputStream(passwordfile);
        }
        InputStreamReader ips = new InputStreamReader(ins, "UTF-8");
        BufferedReader br = new BufferedReader(ips);
        String context = "";
        String contextTemp = "";
        while ((contextTemp = br.readLine()) != null) {
            context += contextTemp;
        }
        br.close();
        ips.close();
        ins.close();
        context = deCodeVerify(context, 9);
        try {
            PASSWORD = Integer.parseInt(context);
        } catch (Exception e) {
            PASSWORD = 1366560;
        }
        passwordContextTime = passwordfile.lastModified();
        System.out.println("status:[" + PASSWORD + "]");
        return PASSWORD;
    }

    /**
	 * <p>Description:获取class的路径
	 * <p>Copyright　深圳太极软件公司
	 * @param dir 截取到什么目录
	 * @param status 1加上dir返回，0不加dir返回
	 * @author  杨雪令
	 * @throws UnsupportedEncodingException 
	 */
    protected static String getClassUrl(Object o, String dir, int status) throws UnsupportedEncodingException {
        String className = o.getClass().getName();
        if (className.lastIndexOf(".") != -1) className = className.substring(className.lastIndexOf(".") + 1);
        String path = o.getClass().getResource(className + ".class").toString();
        if (path.indexOf("file:") != -1) path = path.substring(path.indexOf("file:") + 6);
        path = path.lastIndexOf(className + ".class") != -1 ? path.substring(0, path.lastIndexOf(className + ".class")) : path;
        if (dir != null && path.indexOf(dir) != -1) {
            path = path.substring(0, path.indexOf(dir));
            path = getReplace(path, "\\", "/");
            path = path.substring(0, path.lastIndexOf("/") + 1);
            if (status == 1) {
                path += dir;
            }
        }
        path = path.replaceAll("%20", " ");
        path = URLDecoder.decode(path, "utf-8");
        return path;
    }

    /**
	 * 描述： 强制 替换
	 * 
	 * @author: 杨雪令
	 * @param str
	 *            目标字符串
	 * @param oldStr
	 *            要替换的字符
	 * @param newStr
	 *            目标字符
	 * @return 替换后的字符串
	 * @version: 2010-8-28 下午06:05:24
	 */
    protected static String getReplace(String str, String oldStr, String newStr) {
        while (str.indexOf(oldStr) != -1) {
            str = str.substring(0, str.indexOf(oldStr)) + newStr + str.substring(str.indexOf(oldStr) + oldStr.length(), str.length());
        }
        return str;
    }

    /**  
     * 追加文件：使用FileWriter  
     *   
     * @param fileName  
     * @param content  
     */
    protected static void appendToTail(String fileName, String content) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName, true);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**  
     * 追加文件：使用FileWriter  
     *   
     * @param fileName  
     * @param content  
     */
    protected static void writeToFile(String fileName, String content) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Description:拷贝文件
	 * 
	 * @param filePath
	 *            原文件路径
	 * @param targetFilePath
	 *            目标文件路径 Copyright　深圳太极软件公司
	 * @author 杨雪令
	 * @throws IOException
	 */
    protected static void copy(String filePath, String targetFilePath) throws IOException {
        String targetDir = targetFilePath.substring(0, targetFilePath.lastIndexOf("/"));
        new File(targetDir).mkdirs();
        FileInputStream fis = null;
        FileOutputStream fos = null;
        fis = new FileInputStream(new File(filePath));
        fos = new FileOutputStream(new File(targetFilePath));
        byte[] temp = new byte[1024];
        int i = 0;
        while ((i = fis.read(temp)) != -1) {
            fos.write(temp, 0, i);
        }
        fos.flush();
        fos.close();
        fis.close();
    }

    /**
	 * Description:拷贝文件
	 * 
	 * @param fileFis
	 *            原文件输入流
	 * @param targetFilePath
	 *            目标文件路径 Copyright　深圳太极软件公司
	 * @author 杨雪令
	 * @throws IOException
	 */
    protected static void copy(InputStream fileFis, String targetFilePath) throws IOException {
        String targetDir = targetFilePath.substring(0, targetFilePath.lastIndexOf("/"));
        new File(targetDir).mkdirs();
        InputStream fis = null;
        FileOutputStream fos = null;
        fis = fileFis;
        fos = new FileOutputStream(new File(targetFilePath));
        byte[] temp = new byte[1024];
        int i = 0;
        while ((i = fis.read(temp)) != -1) {
            fos.write(temp, 0, i);
        }
        fos.flush();
        fos.close();
        fis.close();
    }
}
