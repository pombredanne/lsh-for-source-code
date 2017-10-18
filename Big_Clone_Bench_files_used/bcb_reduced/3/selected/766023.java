package com.jxml.qare.qhome;

import java.io.*;
import java.util.*;
import com.jxml.quick.*;
import com.jxml.quick.config.*;
import com.jxml.quick.ocm.*;
import com.jxml.quick.qmap.*;
import org.xml.sax.*;
import com.jxml.qare.qhome.db.*;
import java.math.*;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;
import java.util.zip.*;

public class EncodingDecoding {

    public static ESDO decode(String input) throws Exception {
        Parser parser = new Parser(input);
        String version = parser.nextString();
        if (!Setup.qareVersion.equals(version)) throw new Exception("Expecting version " + Setup.qareVersion + ", not " + version);
        String contentNbr = parser.nextString();
        if (!"".equals(contentNbr) && !Setup.dbContentIdentifier.isNew(contentNbr)) return null;
        String from = parser.nextString().toLowerCase();
        if (from.indexOf("localhost") > -1) throw new Exception("return address of message is localhost");
        String dhB64Pub = parser.nextString();
        String message;
        boolean encryptedMessage = false;
        DBSystem.Row fromSystem = Setup.dbSystem.get(from);
        DBRegisterInfo.Row info = Setup.dbRegisterInfo.get(fromSystem);
        byte[] msgData;
        msgData = parser.nextBytes();
        int bLength = Integer.parseInt(parser.nextString());
        String dsaB64Pub = parser.nextString();
        byte[] dsaPub = Base64.ascToBin(dsaB64Pub.getBytes());
        byte[] sig = parser.nextBytes();
        boolean signed;
        if (dsaPub.length == 0) signed = false; else {
            if (info != null && !info.isUnconfirmed()) {
                String dsapk = info.dsaPublicKey;
                if (!"".equals(dsapk) && !dsapk.equals(dsaB64Pub)) throw new Exception("DSA Key from " + from + " changed");
            }
            signed = true;
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(dsaPub);
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            PublicKey dsaPubKey = keyFactory.generatePublic(pubKeySpec);
            Signature dsa = Signature.getInstance("SHA1withDSA");
            dsa.initVerify(dsaPubKey);
            dsa.update(secureHash(msgData));
            if (!dsa.verify(sig)) throw new Exception("Invalid Signature for " + from);
        }
        byte[] cESDO;
        if ("".equals(dhB64Pub)) {
            cESDO = msgData;
        } else {
            encryptedMessage = true;
            String dhAYS = "";
            if (info != null) {
                dhAYS = info.dhPublicKey;
            }
            if ("".equals(dhAYS)) {
                if ("".equals(dhB64Pub)) throw new Exception("no DHPK for " + from);
                dhAYS = dhB64Pub;
            } else if (!dhAYS.equals(dhB64Pub) && info.isUnconfirmed()) {
                dhAYS = dhB64Pub;
            } else if (!dhAYS.equals(dhB64Pub) && !info.isUnconfirmed()) {
                throw new Exception("DHPK has changed for " + from);
            }
            BigInteger dhAY = new BigInteger(Base64.ascToBin(dhAYS.getBytes()));
            String dhXS = Setup.dbKeyPair.get().getDHPrivateKey();
            BigInteger dhX = new BigInteger(Base64.ascToBin(dhXS.getBytes()));
            BigInteger dhP = DBKeyPair.dhP;
            BigInteger dhAXY = dhAY.modPow(dhX, dhP);
            byte[] secretKey = dhAXY.toByteArray();
            DESKeySpec desKeySpec = new DESKeySpec(secretKey);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES", "CryptixCrypto");
            SecretKey secretDesKey = secretKeyFactory.generateSecret(desKeySpec);
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding", "CryptixCrypto");
            cipher.init(Cipher.DECRYPT_MODE, secretDesKey);
            cESDO = cipher.doFinal(msgData);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(cESDO);
        GZIPInputStream gis = new GZIPInputStream(bais);
        byte bESDO[] = new byte[bLength];
        int l;
        int off = 0;
        int read = 0;
        do {
            off += read;
            l = bLength - off;
            read = gis.read(bESDO, off, l);
        } while (read > 0);
        gis.close();
        message = new String(bESDO);
        QDoc esdoSchema = CreateEsdo.createSchema();
        QDoc esdoDoc = Quick.parseString(esdoSchema, message);
        ESDO esdo = (ESDO) esdoDoc.getRoot();
        if (!from.equals(esdo.fromSystem)) {
            throw new Exception("Wrapper says " + from + ", while ESDO says " + esdo.fromSystem);
        }
        boolean exceedsLimit = Setup.dbRegisterInfo.webServiceLimitExceeded();
        if (encryptedMessage && (info == null || info.isUnconfirmed())) {
            info = Setup.dbRegisterInfo.create(Setup.dbSystem.get(from), dhB64Pub, dsaB64Pub);
        }
        esdo.contentNbr = contentNbr;
        esdo.signed = signed;
        return esdo;
    }

    public static String encode(ESDO esdo) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("\t");
        append(sb, Setup.qareVersion);
        append(sb, esdo.contentNbr);
        DBSystem.Row toSystem = Setup.dbSystem.get(esdo.toSystem);
        DBRegisterInfo.Row info = Setup.dbRegisterInfo.get(toSystem);
        String dhAYS = "";
        if (info != null) {
            dhAYS = info.dhPublicKey;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(baos);
        byte[] bESDO = esdo.toXML().getBytes();
        gos.write(bESDO, 0, bESDO.length);
        gos.close();
        byte[] cESDO = baos.toByteArray();
        append(sb, QConfig.returnAddress());
        DBKeyPair.Row keyPair = Setup.dbKeyPair.get();
        byte[] msgData;
        if ("".equals(dhAYS)) {
            if (!"none".equals(esdo.password)) {
                System.out.println("Bad message: " + esdo.toXML());
                throw new Exception("Missing DHPK for " + esdo.toSystem);
            }
            append(sb, "");
            msgData = cESDO;
            append(sb, msgData);
        } else {
            append(sb, Setup.dhPublicKey);
            BigInteger dhAY = new BigInteger(Base64.ascToBin(dhAYS.getBytes()));
            String dhXS = keyPair.getDHPrivateKey();
            BigInteger dhX = new BigInteger(Base64.ascToBin(dhXS.getBytes()));
            BigInteger dhP = DBKeyPair.dhP;
            BigInteger dhAXY = dhAY.modPow(dhX, dhP);
            byte[] secretKey = dhAXY.toByteArray();
            DESKeySpec desKeySpec = new DESKeySpec(secretKey);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES", "CryptixCrypto");
            SecretKey secrectDesKey = secretKeyFactory.generateSecret(desKeySpec);
            String message = esdo.toXML();
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding", "CryptixCrypto");
            cipher.init(Cipher.ENCRYPT_MODE, secrectDesKey);
            msgData = cipher.doFinal(cESDO);
            append(sb, msgData);
        }
        append(sb, "" + bESDO.length);
        if (!esdo.signed) {
            append(sb, "");
            append(sb, "");
        } else {
            String dsaB64Pub = keyPair.getDSAPublicKey();
            append(sb, "" + dsaB64Pub);
            String dsaB64Pri = keyPair.getDSAPrivateKey();
            byte[] dsaPri = Base64.ascToBin(dsaB64Pri.getBytes());
            PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(dsaPri);
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            PrivateKey dsaPriKey = keyFactory.generatePrivate(priKeySpec);
            Signature dsa = Signature.getInstance("SHA1withDSA");
            dsa.initSign(dsaPriKey);
            dsa.update(secureHash(msgData));
            byte[] sig = dsa.sign();
            append(sb, sig);
        }
        return sb.toString();
    }

    public static byte[] secureHash(byte[] data) throws Exception {
        MessageDigest shaDigest = MessageDigest.getInstance("SHA");
        return shaDigest.digest(data);
    }

    public static void append(StringBuffer sb, String part) {
        sb.append(part);
        sb.append(" :::\n\r\t");
    }

    public static void append(StringBuffer sb, byte[] part) {
        byte[] bin = Base64.binToAsc(part);
        sb.append(new String(bin));
        sb.append(" :::\n\r\t");
    }

    public static class Parser {

        String input;

        int offset;

        public Parser(String in) {
            input = in;
            offset = 0;
        }

        public byte[] nextBytes() throws Exception {
            int j = input.indexOf(":::", offset);
            String rv = input.substring(offset, j);
            offset = j + 3;
            return Base64.ascToBin(rv.getBytes());
        }

        public String nextString() throws Exception {
            int j = input.indexOf(":::", offset);
            String ex = input.substring(offset, j);
            offset = j + 3;
            StringBuffer sb = new StringBuffer();
            String ws = " \n\r\t";
            int i, s;
            s = ex.length();
            for (i = 0; i < s; ++i) {
                char x = ex.charAt(i);
                if (ws.indexOf(x) == -1) sb.append(x);
            }
            return sb.toString();
        }
    }
}
