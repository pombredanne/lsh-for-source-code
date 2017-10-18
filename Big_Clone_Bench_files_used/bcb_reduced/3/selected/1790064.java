package org.jives.implementors.network.jxse.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.jives.implementors.network.jxse.JXSEImplementor;
import org.jives.utils.IOManager;
import org.jives.utils.Log;

/**
 * Class which includes all the tools needed to manage the network
 * functionalities.
 * 
 * @author simonesegalini
 */
public class Tools {

    private static final String ipv4Pattern = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    public static final String ipv6Pattern = "\\A(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\z";

    private static Pattern patternipv4;

    private static Matcher matcheripv4;

    private static Pattern patternipv6;

    private static Matcher matcheripv6;

    private static String ipv4;

    private static String ipv6;

    /**
	 * Static method used to convert a hex into a string.
	 * 
	 * @param hex
	 *            the hex to be converted
	 * 
	 * @return the string corresponding to the original input hex
	 */
    public static String convertHexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            String output = hex.substring(i, (i + 2));
            int decimal = Integer.parseInt(output, 16);
            sb.append((char) decimal);
        }
        return sb.toString();
    }

    /**
	 * Static method used to convert a string into a hex.
	 * 
	 * @param str
	 *            the string to be converted
	 * 
	 * @return the string converted
	 */
    public static String convertStringToHex(String str) {
        char[] chars = str.toCharArray();
        StringBuffer hex = new StringBuffer();
        for (char c : chars) {
            hex.append(Integer.toHexString(c));
        }
        return hex.toString();
    }

    /**
	 * Static method used to decode a string.
	 * 
	 * @param passwordtodecode
	 *            the string to be decoded
	 * 
	 * @return the string decoded
	 * 
	 * @see EncodeDecodeString#decrypt(String)
	 */
    public static String decodePassword(String passwordtodecode) {
        String passworddecoded;
        if (passwordtodecode != null && !passwordtodecode.isEmpty()) {
            EncodeDecodeString crypt = new EncodeDecodeString();
            passworddecoded = crypt.decrypt(passwordtodecode);
            passworddecoded = convertHexToString(passworddecoded);
        } else {
            passworddecoded = "";
        }
        return passworddecoded;
    }

    /**
	 * Deletes network platform files
	 * 
	 * @param peerName
	 * 					the name of the Peer to be stopped
	 */
    public static void deletePlatformFiles(String peerName) {
        File actual = IOManager.resolveFile(JXSEImplementor.class, JXSEImplementor.networkPlatformPath + peerName);
        if (actual.listFiles() != null) {
            for (File f : actual.listFiles()) {
                delete(f);
            }
            for (File f : actual.listFiles()) {
                delete(f);
            }
        }
        actual.delete();
    }

    /**
	 * Deletes recursively if f is a directory,
	 * or deletes the file otherwise
	 * 
	 * @param f
	 * 			the file or directory to be deleted
	 */
    static void delete(File f) {
        if (f.isDirectory()) {
            if (f.listFiles().length > 0) {
                for (File c : f.listFiles()) {
                    delete(c);
                }
                f.delete();
            } else {
                f.delete();
            }
        } else {
            f.delete();
        }
    }

    /**
	 * Static method used to encode a string.
	 * 
	 * @param passwordtoencode
	 *            the string to be encoded
	 * 
	 * @return the string encoded
	 * 
	 * @see EncodeDecodeString#encrypt(String)
	 */
    public static String encodePassword(String passwordtoencode) {
        String passwordencoded = "";
        if (passwordtoencode != "") {
            EncodeDecodeString crypt = new EncodeDecodeString();
            String hexpasswordtoencode = convertStringToHex(passwordtoencode);
            passwordencoded = crypt.encrypt(hexpasswordtoencode);
        }
        return passwordencoded;
    }

    /**
	 * Static method used to get the current date and time.
	 * 
	 * @return a string with date and time informations
	 */
    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return dateFormat.format(date);
    }

    /**
	 * Static method used to get network interfaces of the hardware machine.
	 * 
	 * @return an array of strings with all the network interfaces names
	 */
    public static String[] getInterfaces() {
        @SuppressWarnings("rawtypes") Enumeration e;
        List<String> interfaces = new ArrayList<String>();
        String[] interfacesnames = null;
        try {
            e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) e.nextElement();
                interfaces.add(ni.getName().toString());
            }
            interfacesnames = new String[interfaces.size()];
            @SuppressWarnings("rawtypes") Iterator iter = interfaces.iterator();
            int i = 0;
            while (iter.hasNext()) {
                interfacesnames[i] = iter.next().toString();
                i++;
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        return interfacesnames;
    }

    /**
	 * Static method used to get IPv4 of a specific network interface.
	 * 
	 * @param netInterface
	 *            the network interface of which we want to know the IP address
	 * 
	 * @return the IPv4 address
	 */
    public static String getInterfacesIPv4(String netInterface) {
        String networkInterfaceIP = null;
        try {
            @SuppressWarnings("rawtypes") Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) e.nextElement();
                if (ni.getName().toString().equals(netInterface)) {
                    NetworkLog.logMsg(NetworkLog.LOG_INFO, Tools.class, "Your network interface is: " + ni.getName().toString());
                    @SuppressWarnings("rawtypes") Enumeration e2 = ni.getInetAddresses();
                    patternipv4 = Pattern.compile(ipv4Pattern);
                    while (e2.hasMoreElements()) {
                        InetAddress ip = (InetAddress) e2.nextElement();
                        ipv4 = ip.toString().replace("/", "");
                        matcheripv4 = patternipv4.matcher(ipv4);
                        if (matcheripv4.matches()) {
                            networkInterfaceIP = ipv4;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error(Tools.class, e.getMessage());
        }
        return networkInterfaceIP;
    }

    /**
	 * Static method used to get IPv6 of a specific network interface.
	 * 
	 * @param netInterface
	 *            the network interface of which we want to know the IP address
	 * 
	 * @return the IPv6 address
	 */
    public static String getInterfacesIPv6(String netInterface) {
        String networkInterfaceIP = null;
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) e.nextElement();
                if (ni.getName().toString().equals(netInterface)) {
                    NetworkLog.logMsg(NetworkLog.LOG_INFO, Tools.class, "Your network interface is: " + ni.getName().toString());
                    Enumeration<InetAddress> e2 = ni.getInetAddresses();
                    patternipv6 = Pattern.compile(ipv6Pattern);
                    while (e2.hasMoreElements()) {
                        InetAddress ip = (InetAddress) e2.nextElement();
                        if (ip instanceof Inet6Address) {
                            String temp = EncodeDecodeString.toString(ip.getAddress());
                            ipv6 = temp.substring(0, 4) + ":" + temp.substring(4, 8) + ":" + temp.substring(8, 12) + ":" + temp.substring(12, 16) + ":" + temp.substring(16, 20) + ":" + temp.substring(20, 24) + ":" + temp.substring(24, 28) + ":" + temp.substring(28, 32);
                            matcheripv6 = patternipv6.matcher(ipv6);
                            if (matcheripv6.matches()) {
                                networkInterfaceIP = ipv6;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error(Tools.class, e.getMessage());
        }
        return networkInterfaceIP;
    }

    /**
	 * Static method used to get the public IP of a rendezvous/relay.
	 * 
	 * @param proxy
	 *            the boolean value indicating if it is a free or a proxy
	 *            connection
	 * 
	 * @return the public IP
	 */
    public static String getPublicIP(Boolean proxy) {
        String IP = null;
        if (!proxy) {
            try {
                URL url = new URL(XMLConfigParser.urlHost + "getPublicIp.php");
                HttpURLConnection Conn = (HttpURLConnection) url.openConnection();
                InputStream InStream = Conn.getInputStream();
                InputStreamReader Isr = new java.io.InputStreamReader(InStream);
                BufferedReader Br = new java.io.BufferedReader(Isr);
                IP = Br.readLine();
                NetworkLog.logMsg(NetworkLog.LOG_INFO, Tools.class, "Your public IP address is " + IP);
            } catch (Exception e) {
                Log.error(Tools.class, e.getMessage());
            }
        } else {
            XMLConfigParser.readProxyConfiguration();
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyHost", XMLConfigParser.proxyHost);
            System.getProperties().put("proxyPort", XMLConfigParser.proxyPort);
            URL url;
            try {
                url = new URL(XMLConfigParser.urlHost + "getPublicIp.php");
                URLConnection urlConn = url.openConnection();
                String password = XMLConfigParser.proxyUsername + ":" + XMLConfigParser.proxyPassword;
                String encoded = Base64.encodeBase64String(password.getBytes());
                urlConn.setRequestProperty("Proxy-Authorization", encoded);
                InputStream InStream = urlConn.getInputStream();
                InputStreamReader Isr = new java.io.InputStreamReader(InStream);
                BufferedReader Br = new java.io.BufferedReader(Isr);
                IP = Br.readLine();
                NetworkLog.logMsg(NetworkLog.LOG_INFO, Tools.class, "Your public IP address is " + IP);
            } catch (MalformedURLException e) {
                Log.error(Tools.class, e.getMessage());
            } catch (IOException e) {
                Log.error(Tools.class, e.getMessage());
            }
        }
        return IP;
    }

    /**
	 * Static method used to convert a string into a md5.
	 * 
	 * @param text
	 *            the text to be transformed
	 * 
	 * @return the md5 corresponding to the input string
	 */
    public static String md5Converter(String text) {
        String md5val = "";
        MessageDigest algorithm = null;
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            NetworkLog.logMsg(NetworkLog.LOG_FATAL, Tools.class, "Cannot find digest algorithm");
            System.exit(1);
        }
        byte[] defaultBytes = text.getBytes();
        algorithm.reset();
        algorithm.update(defaultBytes);
        byte messageDigest[] = algorithm.digest();
        StringBuffer hexString = new StringBuffer();
        for (byte element : messageDigest) {
            String hex = Integer.toHexString(0xFF & element);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        md5val = hexString.toString();
        NetworkLog.logMsg(NetworkLog.LOG_INFO, Tools.class, "MD5 of " + text + ": " + md5val);
        return md5val;
    }

    /**
	 * The static method used to sleep a thread.
	 * 
	 * @param Duration
	 *            the time in milliseconds the thread should sleep
	 */
    public static final void sleep(long Duration) {
        long Delay = System.currentTimeMillis() + Duration;
        while (System.currentTimeMillis() < Delay) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.debug(Tools.class, e.getMessage());
            }
        }
    }
}
