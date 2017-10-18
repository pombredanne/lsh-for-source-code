package de.iritgo.aktera.license;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import de.iritgo.simplelife.string.StringTools;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.NetworkInterface;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @version $Id: LicenseTools.java,v 1.14 2006/09/15 10:29:45 grappendorf Exp $
 */
public class LicenseTools {

    public static LicenseInfo licenseInfo;

    public static LicenseInfo getLicenseInfo() {
        return licenseInfo;
    }

    public static LicenseInfo getLicenseInfo(Log log) {
        return getLicenseInfo(log, System.getProperty("iritgo.license.path"));
    }

    public static LicenseInfo getLicenseInfo(Log log, String licensePath) {
        if (licenseInfo != null) {
            return licenseInfo;
        }
        synchronized (LicenseTools.class) {
            System.setProperty("iritgo.license.path", licensePath);
            licenseInfo = loadLicense(log, licensePath);
        }
        return licenseInfo;
    }

    public static void clear() {
        licenseInfo = null;
    }

    public static String machineInfo() {
        StringBuilder machineInfo = new StringBuilder();
        try {
            Enumeration networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) networkInterfaces.nextElement();
                if ("eth0".equals(networkInterface.getDisplayName())) {
                    for (byte b : networkInterface.getHardwareAddress()) {
                        StringTools.appendWithDelimiter(machineInfo, String.format("%02x", b).toUpperCase(), ":");
                    }
                    machineInfo.append("\n");
                    break;
                }
            }
        } catch (IOException x) {
            System.out.println("LicenseTools.machineInfo: " + x.getMessage());
            x.printStackTrace();
        }
        if (machineInfo.length() == 0) {
            return null;
        }
        String info = machineInfo.toString();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5", "SUN");
            messageDigest.update(info.getBytes());
            byte[] md5 = messageDigest.digest(info.getBytes());
            return new String(Base64.encodeBase64(md5));
        } catch (Exception x) {
            System.out.println("LicenseTools.machineInfo: " + x.getMessage());
            x.printStackTrace();
        }
        return null;
    }

    /**
	 * Load a license.
	 *
	 * @param path The license file path.
	 * @return The license info.
	 */
    public static LicenseInfo loadLicense(Log log, String path) {
        String keyString = null;
        try {
            keyString = ((AkteraKeyProvider) Class.forName("de.iritgo.aktera.license.AkteraKey").newInstance()).getKey();
        } catch (InstantiationException x1) {
            x1.printStackTrace();
        } catch (IllegalAccessException x1) {
            x1.printStackTrace();
        } catch (ClassNotFoundException x1) {
            x1.printStackTrace();
        }
        Properties props = new Properties();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = in.readLine()) != null) {
                if ("# SHA1 Signature".equals(line)) {
                    break;
                } else {
                    sb.append(line + "\n");
                }
            }
            StringBuffer sbSig = new StringBuffer();
            while ((line = in.readLine()) != null) {
                sbSig.append(line + "\n");
            }
            X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decodeBase64(keyString.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance("DSA", "SUN");
            PublicKey key = keyFactory.generatePublic(spec);
            Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
            sig.initVerify(key);
            sig.update(sb.toString().getBytes(), 0, sb.toString().getBytes().length);
            boolean valid = sig.verify(Base64.decodeBase64(sbSig.toString().getBytes()));
            if (!valid) {
                return null;
            }
            try {
                props.load(new StringReader(sb.toString()));
            } catch (Exception x) {
                System.out.println("LicenseTools.loadLicense: " + x.getMessage());
                x.printStackTrace();
                return null;
            }
        } catch (Exception x) {
            System.out.println("LicenseTools.loadLicense: " + x.getMessage());
            x.printStackTrace();
            return null;
        }
        return new LicenseInfo(log, props);
    }
}
