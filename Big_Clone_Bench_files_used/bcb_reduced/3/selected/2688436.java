package org.jdamico.jhu.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.jdamico.jhu.components.Joiner;
import org.jdamico.jhu.dataobjects.FileMap;
import org.jdamico.jhu.dataobjects.NetworkInterfaceObject;
import org.jdamico.jhu.dataobjects.PartialFile;
import org.jdamico.jhu.xml.ObjConverter;
import org.vikulin.utils.Constants;

public class Helper {

    private static final Logger log = Logger.getLogger(Helper.class);

    private static Helper INSTANCE = null;

    public static Helper getInstance() {
        if (INSTANCE == null) INSTANCE = new Helper();
        return INSTANCE;
    }

    public String formatDecimalCurrency(Float value, String format) {
        NumberFormat formatter = new DecimalFormat(format);
        String f = formatter.format(value);
        return f;
    }

    public ArrayList<NetworkInterfaceObject> getMyIPs() throws Exception {
        ArrayList<NetworkInterfaceObject> ifacesArray = new ArrayList<NetworkInterfaceObject>();
        String ifaceName = null;
        String ifaceIPv6 = null;
        String ifaceIPv4 = null;
        Enumeration<NetworkInterface> enumNetworkInterface = NetworkInterface.getNetworkInterfaces();
        while (enumNetworkInterface.hasMoreElements()) {
            NetworkInterface ni = (NetworkInterface) enumNetworkInterface.nextElement();
            ifaceName = ni.getName();
            Enumeration<InetAddress> enumInetAddress = ni.getInetAddresses();
            while (enumInetAddress.hasMoreElements()) {
                try {
                    InetAddress ia = (InetAddress) enumInetAddress.nextElement();
                    ifaceIPv6 = ia.toString().replaceAll("/", "");
                    ia = (InetAddress) enumInetAddress.nextElement();
                    ifaceIPv4 = ia.toString().replaceAll("/", "");
                } catch (NoSuchElementException e) {
                    ifaceIPv4 = ifaceIPv6;
                    ifaceIPv6 = null;
                }
            }
            ifacesArray.add(new NetworkInterfaceObject(ifaceName, ifaceIPv6, ifaceIPv4));
            LoggerManager.getInstance().logAtDebugTime(super.getClass().getName(), ifaceName + " : [" + ifaceIPv6 + "," + ifaceIPv4 + "]");
        }
        return ifacesArray;
    }

    public static String getStringFromFile(String fileName) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
            in.close();
        } catch (IOException e) {
            System.out.println("File not found: " + fileName);
        }
        return sb.toString();
    }

    public static void consolidateFile(String fileMapXml) throws Exception {
        ObjConverter converter = new ObjConverter();
        FileMap fileMap = converter.convertToFileMap(getStringFromFile(fileMapXml));
        Joiner joiner = new Joiner();
        File firstFile = new File(Constants.conf.getFileDirectory() + Constants.PATH_SEPARATOR + (new File(fileMap.getPartialFileList()[0].getName())).getName());
        joiner.join(firstFile, true);
        String uploadedMd5 = getFileMD5(Constants.conf.getFileDirectory() + Constants.PATH_SEPARATOR + fileMap.getSourceFile().getName());
        if (uploadedMd5.equals(fileMap.getSourceFile().getMd5())) {
            log.info(fileMap.getSourceFile().getName() + " | " + uploadedMd5 + " [ok]");
            File rmF = new File(Constants.conf.getFileDirectory() + Constants.PATH_SEPARATOR + fileMap.getSourceFile().getName() + ".xml");
            rmF.delete();
        } else {
            throw new Exception("Accempled file (" + fileMap.getSourceFile().getName() + ") is corrupted!");
        }
    }

    public byte[] convertByteArray2byteArray(Byte[] source) {
        Byte[] ByteElement = source;
        byte[] byteElement = new byte[ByteElement.length];
        for (int k = 0; k < ByteElement.length; k++) {
            byteElement[k] = ByteElement[k];
        }
        return byteElement;
    }

    public Byte[] convertbyteArray2ByteArray(byte[] source) {
        byte[] byteElement = source;
        Byte[] ByteElement = new Byte[byteElement.length];
        for (int k = 0; k < byteElement.length; k++) {
            ByteElement[k] = byteElement[k];
        }
        return ByteElement;
    }

    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String MD5(byte[] data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String text = convertToHex(data);
        MessageDigest md;
        md = MessageDigest.getInstance("MD5");
        byte[] md5hash = new byte[32];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        md5hash = md.digest();
        return convertToHex(md5hash);
    }

    public byte[] createChecksum(byte[] fileByte) throws Exception {
        MessageDigest complete = MessageDigest.getInstance("MD5");
        complete.digest(fileByte);
        return complete.digest();
    }

    public static String getFileMD5(String fileName) throws Exception {
        byte[] b = createChecksum(fileName);
        return MD5(b);
    }

    public String getFileMD5(byte[] fileByte) throws Exception {
        byte[] b = createChecksum(fileByte);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static String updateFileMap(String fileMapXmlFileName, File oPartFile, Integer fileIndex) throws Exception {
        ObjConverter converter = new ObjConverter();
        String corruptedPartsList = "";
        Helper.getInstance();
        FileMap fileMap = converter.convertToFileMap(Helper.getStringFromFile(Constants.conf.getFileDirectory() + Constants.PATH_SEPARATOR + fileMapXmlFileName));
        PartialFile partFile = fileMap.getPartialFileList()[fileIndex];
        String uploadedMd5 = null;
        if (oPartFile.exists()) {
            uploadedMd5 = Helper.getFileMD5(oPartFile.getAbsolutePath());
            partFile.setUploaded(true);
            if (!uploadedMd5.equals(partFile.getMd5())) {
                log.error("Error! " + partFile + " | " + uploadedMd5);
                corruptedPartsList = corruptedPartsList + fileIndex + "&";
            } else {
                log.info(partFile.getName() + " | " + uploadedMd5 + " [ok]");
            }
        }
        writeFileMapXML(fileMap, Constants.conf.getFileDirectory());
        if (corruptedPartsList.length() > 0) return corruptedPartsList; else return null;
    }

    public static void writeFileMapXML(FileMap fileMap, String folder) throws IOException {
        String sourceFileName = fileMap.getSourceFile().getName();
        BufferedWriter out = new BufferedWriter(new FileWriter(folder + Constants.PATH_SEPARATOR + sourceFileName + ".xml"));
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<filemap>\n" + "<sourcefile name=\"" + sourceFileName + "\" md5=\"" + fileMap.getSourceFile().getMd5() + "\" offset=\"" + fileMap.getSourceFile().getOffset() + "\"/>\n" + "<pfiles>\n");
        for (int j = 0; j < fileMap.getPartialFileList().length; j++) {
            out.write("<pfile name=\"" + fileMap.getPartialFileList()[j].getName() + "\" md5=\"" + fileMap.getPartialFileList()[j].getMd5() + "\" uploaded=\"" + fileMap.getPartialFileList()[j].isUploaded() + "\"/>\n");
        }
        out.write("</pfiles>\n" + "</filemap>");
        out.close();
    }

    public static String getServerAddress() {
        if (Constants.conf.isUseSsl()) {
            return "https://" + Constants.conf.getServerHost() + ":" + Constants.conf.getSslPort();
        } else {
            return "http://" + Constants.conf.getServerHost() + ":" + Constants.conf.getServerPort();
        }
    }
}
