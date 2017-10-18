package edu.upmc.opi.caBIG.caTIES.installer.manifest;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

public class CaTIES_CheckSumGenerator {

    public static void main(String args[]) {
        String datafile = "e:\\caties_5_0\\nwu\\tomcatPub\\webapps\\wsrf\\WEB-INF\\lib\\jug.jar";
        CaTIES_CheckSumGenerator checkSumGenerator = new CaTIES_CheckSumGenerator();
        String checkSum = checkSumGenerator.getCheckSum(datafile);
        System.out.println("SHA1 Digest(in hex format):: " + checkSum);
        checkSum = checkSumGenerator.getCheckSumStream(datafile);
        System.out.println("Adler32 Digest(in long format):: " + checkSum);
    }

    public String getCheckSum(String datafile) {
        String response = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            FileInputStream fis = new FileInputStream(datafile);
            byte[] dataBytes = new byte[1024];
            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            byte[] mdbytes = md.digest();
            StringBuffer sb = new StringBuffer("");
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            response = sb.toString();
        } catch (Exception x) {
            response = null;
        }
        return response;
    }

    public String getCheckSumStream(String datafile) {
        long checksum = Long.MAX_VALUE;
        try {
            CheckedInputStream cis = new CheckedInputStream(new FileInputStream(datafile), new Adler32());
            byte[] tempBuf = new byte[128];
            while (cis.read(tempBuf) >= 0) {
            }
            checksum = cis.getChecksum().getValue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return checksum + "";
    }
}
