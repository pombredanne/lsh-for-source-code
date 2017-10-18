package net.turingcomplete.phosphor.shared;

import java.util.*;
import java.io.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.swing.*;
import zyh.net.*;
import java.net.*;

/**
 * Various limits that are used to determine if and how to accept connections.
 * <p>
 * REVISION HISTORY:
 * <p>
 */
public class LimitOptions {

    AlgorithmParameterSpec myCipherParams;

    Key myCipherKey;

    MessageDigest myMessageDigester;

    Cipher myPropertyFileCipher = null;

    private String socksProxyHost;

    private int socksProxyPort;

    private String socksProxyUsername;

    private String socksProxyPassword;

    private boolean isDownloadThroughProxy;

    private boolean isSaveProxyPassword;

    private int connectingInterfacePort;

    private String networkEncoding;

    private String myPropertyFileName;

    private Properties myPropertyFile = new Properties();

    private String lookAndFeelClassName;

    private String webHome;

    private String emailFeedback;

    private boolean serverConnectOnStartup;

    private int numServersAutoConnectTo;

    private boolean connectOnStartup;

    private InetAddress localHost;

    protected LimitOptions(String propertyFile) {
        myPropertyFileName = System.getProperty("user.home") + File.separatorChar + ProtocolDetails.PROGRAM_NAME + '_' + propertyFile + (ProtocolDetails.DEBUG ? '_' + ProtocolDetails.VERSION_DASHES + "_debug" : "") + ".config";
        try {
            initCrypto();
        } catch (Throwable e) {
            try {
                Security.addProvider(new com.sun.crypto.provider.SunJCE());
                initCrypto();
            } catch (Throwable ee) {
                Trace.display(ee, "Cannot encrypt password; it will not be saved");
                myPropertyFileCipher = null;
                myCipherParams = null;
                myCipherKey = null;
                myMessageDigester = null;
            }
        }
        loadMyOptions();
    }

    private void initCrypto() throws GeneralSecurityException {
        myPropertyFileCipher = Cipher.getInstance("PBEWithMD5AndDES");
        char[] pass = (ProtocolDetails.PROGRAM_NAME + ProtocolDetails.VERSION + ProtocolDetails.DEBUG + ProtocolDetails.XML_STREAM_NAMESPACE + System.getProperty("os.name", "") + System.getProperty("os.arch", "") + System.getProperty("os.version", "")).toCharArray();
        KeySpec cipherKeySpec = new PBEKeySpec(pass);
        Arrays.fill(pass, Character.MIN_VALUE);
        byte[] salt = new byte[] { ProtocolDetails.VERSION_MAJOR, ProtocolDetails.VERSION_MINOR, ProtocolDetails.VERSION_REVISION, 0, 1, 2, 3, 4 };
        Assert.that(salt.length == 8);
        myCipherParams = new PBEParameterSpec(salt, 2);
        Arrays.fill(salt, Byte.MIN_VALUE);
        SecretKeyFactory cipherKeyFactory = SecretKeyFactory.getInstance(myPropertyFileCipher.getAlgorithm());
        myCipherKey = cipherKeyFactory.generateSecret(cipherKeySpec);
        myMessageDigester = MessageDigest.getInstance("SHA");
    }

    /** seperated from loadMyOptions to allow subclasses to override loadOptions while not calling base class version in constructor */
    public void loadOptions() {
        loadMyOptions();
    }

    private synchronized void loadMyOptions() {
        socksProxyHost = "";
        socksProxyPort = 0;
        socksProxyUsername = "";
        socksProxyPassword = "";
        isDownloadThroughProxy = true;
        isSaveProxyPassword = false;
        networkEncoding = "UTF-8";
        lookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
        webHome = "phosphor.sourceforge.net";
        emailFeedback = "phosphor@turingcomplete.net";
        serverConnectOnStartup = true;
        numServersAutoConnectTo = 3;
        connectOnStartup = true;
        connectingInterfacePort = 0;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            Trace.display(e, "Fatal error: unable to find local host");
            System.exit(ExitCodes.UNRECOVERABLE_ERROR);
        }
        myPropertyFile.clear();
        try {
            myPropertyFile.load(new FileInputStream(myPropertyFileName));
        } catch (IOException e) {
            Trace.display(e, "Config file not found - using defaults.");
        }
        String workingDir = myPropertyFile.getProperty("workingDir");
        if (workingDir == null) {
            if (System.getProperty("javawebstart.version") != null) {
                File dir = new File(System.getProperty("user.home") + File.separatorChar + ProtocolDetails.PROGRAM_NAME + (ProtocolDetails.DEBUG ? ('_' + ProtocolDetails.VERSION_DASHES) : "") + File.separatorChar);
                if (dir.exists() || dir.mkdirs()) {
                    setWorkingDir(dir.getAbsoluteFile().getPath());
                } else {
                    Trace.display("Unable to create working directory");
                    setWorkingDir(System.getProperty("user.home"));
                }
            } else {
                setWorkingDir(System.getProperty("user.dir"));
            }
        }
        readTracing();
        if (myPropertyFileCipher != null) {
            String encPass = myPropertyFile.getProperty("socksProxyPassword");
            if (encPass != null) {
                try {
                    myPropertyFileCipher.init(Cipher.DECRYPT_MODE, myCipherKey, myCipherParams);
                    byte[] pass = myPropertyFileCipher.doFinal(getBytes(encPass));
                    String digest = getString(myMessageDigester.digest(pass));
                    if (digest.equals(get("socksProxyPasswordHash", ""))) {
                        socksProxyPassword = getString(pass);
                    } else {
                        Trace.display("socksProxyPassword not correct");
                    }
                } catch (GeneralSecurityException e) {
                    Trace.display(e, "Unable to decrypt socks password.");
                } catch (IllegalStateException e) {
                    Trace.display(e, "Unable to decrypt socks password.");
                }
            }
        }
        try {
            initSocksProxy();
        } catch (IOException e) {
            Trace.display(e, "Cannot initialize socks proxy");
        }
    }

    public void store() {
        try {
            writeTracing();
            if (!isSaveProxyPassword()) {
                socksProxyPassword = "";
                if (myPropertyFile.get("socksProxyPassword") != null && !isSaveProxyPassword()) {
                    set("socksProxyPassword", "");
                    set("socksProxyPasswordHash", "");
                }
            }
            myPropertyFile.store(new FileOutputStream(myPropertyFileName), "Configuration file for " + ProtocolDetails.PROGRAM_NAME);
        } catch (IOException e) {
            Trace.display(e, "Cannot save settings: file " + myPropertyFileName + " not found");
        }
    }

    /** set up proxy server support, socks4 or socsk5 if username & password 
	 * If proxyHost is empty, no sock support
	 * If username is empty, socks4
	 * if host & username not empty then socks5
	 * if no proxy then sockeyImplFactory is set to null
	 */
    public void initSocksProxy() throws IOException {
        if (getSocksProxyHost().length() > 0) {
            if (socksProxyUsername.length() > 0) {
                Properties properties = new Properties();
                properties.put(SocksSocket.USER, getSocksProxyUsername());
                properties.put(SocksSocket.PASSWD, getSocksProxyPassword());
                SocksSocketImplFactory factory = new SocksSocketImplFactory(getSocksProxyHost(), getSocksProxyPort(), properties);
                SocksSocket.setSocketImplFactory(factory);
            } else {
                SocksSocketImplFactory factory = new SocksSocketImplFactory(getSocksProxyHost(), getSocksProxyPort());
                SocksSocket.setSocketImplFactory(factory);
            }
        } else {
            SocksSocket.setSocketImplFactory(null);
        }
    }

    protected int get(String key, int defaultValue) {
        try {
            return Integer.parseInt(myPropertyFile.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            Trace.display(e, "Config file not valid, cannot parse " + key);
            return defaultValue;
        }
    }

    protected boolean get(String key, boolean defaultValue) {
        return Boolean.valueOf(myPropertyFile.getProperty(key, String.valueOf(defaultValue))).booleanValue();
    }

    protected String get(String key, String defaultValue) {
        return myPropertyFile.getProperty(key, defaultValue);
    }

    protected void set(String key, int value) {
        myPropertyFile.setProperty(key, String.valueOf(value));
    }

    protected void set(String key, boolean value) {
        myPropertyFile.setProperty(key, String.valueOf(value));
    }

    protected void set(String key, String value) {
        myPropertyFile.setProperty(key, value);
    }

    public static String getString(byte[] b) {
        StringBuffer s = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; ++i) {
            s.append(Character.forDigit((b[i] & 0xF0) >>> 4, 16));
            s.append(Character.forDigit(b[i] & 0x0F, 16));
        }
        return s.toString();
    }

    public static String getString(ByteArrayOutputStream ba) {
        return getString(ba.toByteArray());
    }

    public static byte[] getBytes(String s) {
        byte[] r = new byte[s.length() / 2];
        for (int i = 0; i < r.length; ++i) {
            r[i] = (byte) ((Character.digit(s.charAt(i * 2), 16) << 4) | Character.digit(s.charAt(i * 2 + 1), 16));
        }
        return r;
    }

    public String getPropertyFileName() {
        return myPropertyFileName;
    }

    public String getWorkingDir() {
        return get("workingDir", "");
    }

    public void setWorkingDir(String workingDir) {
        set("workingDir", workingDir);
    }

    public void setSocksProxyPassword(String val) {
        if (get("isSaveProxyPassword", isSaveProxyPassword) && myPropertyFileCipher != null) {
            try {
                myPropertyFileCipher.init(Cipher.ENCRYPT_MODE, myCipherKey, myCipherParams);
                set("socksProxyPassword", getString(myPropertyFileCipher.doFinal(getBytes(val))));
                set("socksProxyPasswordHash", getString(myMessageDigester.digest(getBytes(val))));
            } catch (GeneralSecurityException e) {
                Trace.display(e, "Cannot encrypt password; it will not be saved");
            } catch (IllegalStateException e) {
                Trace.display(e, "Cannot encrypt password; it will not be saved");
            }
        }
        socksProxyPassword = val;
    }

    public String getSocksProxyPassword() {
        return socksProxyPassword;
    }

    public String getSocksProxyHost() {
        return get("socksProxyHost", socksProxyHost);
    }

    public void setSocksProxyHost(String val) {
        set("socksProxyHost", val.trim());
    }

    public boolean isSaveProxyPassword() {
        return get("isSaveProxyPassword", isSaveProxyPassword);
    }

    public void setSaveProxyPassword(boolean val) {
        set("isSaveProxyPassword", val);
    }

    public int getSocksProxyPort() {
        return get("socksProxyPort", socksProxyPort);
    }

    public void setSocksProxyPort(int val) {
        set("socksProxyPort", val);
    }

    public String getSocksProxyUsername() {
        return get("socksProxyUsername", socksProxyUsername);
    }

    public void setSocksProxyUsername(String val) {
        set("socksProxyUsername", val.trim());
    }

    public boolean isDownloadThroughProxy() {
        return get("isDownloadThroughProxy", isDownloadThroughProxy);
    }

    public void setDownloadThroughProxy(boolean value) {
        set("isDownloadThroughProxy", value);
    }

    /** the default look and feel */
    public String getLookAndFeelClassName() {
        return get("lookAndFeelClassName", lookAndFeelClassName);
    }

    public void setLookAndFeelClassName(String val) {
        set("lookAndFeelClassName", val);
    }

    public String getNetworkEncoding() {
        return get("networkEncoding", networkEncoding);
    }

    public void setNetworkEncoding(String val) {
        set("networkEncoding", val);
    }

    public String getWebHome() {
        return get("webHome", webHome);
    }

    public void setWebHome(String val) {
        set("webHome", val);
    }

    public String getEmailFeedback() {
        return get("emailFeedback", emailFeedback);
    }

    public void setEmailFeedback(String val) {
        set("emailFeedback", val);
    }

    public boolean isConnectOnStartup() {
        return get("connectOnStartup", connectOnStartup);
    }

    public void setConnectOnStartup(boolean value) {
        set("connectOnStartup", value);
    }

    public void writeTracing() {
        writeObject("tracing", Trace.getTracing());
    }

    public void readTracing() {
        try {
            Trace.setTracing((BitSet) readObject("tracing"));
        } catch (Exception e) {
            if (ProtocolDetails.DEBUG) {
                Trace.setTracingAll();
                Trace.clearBit(Trace.READ);
                Trace.clearBit(Trace.EMPTY_READ);
                Trace.clearBit(Trace.WROTE);
            } else Trace.setTracingNone();
        }
    }

    /** serializes and writes object */
    protected void writeObject(String key, Object value) {
        if (value != null) try {
            ByteArrayOutputStream outString = new ByteArrayOutputStream();
            new ObjectOutputStream(outString).writeObject(value);
            set(key, getString(outString));
        } catch (IOException e) {
        } else set(key, "");
    }

    protected Object readObject(String key) throws Exception {
        return readObject(key, false);
    }

    /** @throws exception if object could not be reserialized */
    protected Object readObject(String key, boolean isCanReturnNull) throws Exception {
        String value = myPropertyFile.getProperty(key);
        if (value != null) {
            if (value.length() <= 0 && isCanReturnNull) return null;
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(getBytes(value)));
            return in.readObject();
        } else throw new Exception("Cannot reserialize");
    }

    protected void writeComboItems(String key, JEditableComboBox cbox, int maxItemsSaved) {
        List items = cbox.getRecentItems(maxItemsSaved);
        items.add(0, cbox.getItem());
        writeObject(key, items);
    }

    protected void readComboItems(String key, JEditableComboBox cbox) {
        readComboItems(key, cbox, null);
    }

    /** @param defSelection == null means don't setSelectedItem, leave combo editor blank */
    protected void readComboItems(String key, JEditableComboBox cbox, String defSelection) {
        cbox.clear();
        try {
            List items = (List) readObject(key);
            if (items.size() > 0) {
                String selection = (String) items.remove(0);
                cbox.addItem(items);
                if (defSelection != null) {
                    if (selection.length() > 0) cbox.setItem(selection); else cbox.setItem(defSelection);
                }
            } else cbox.setItem(defSelection);
        } catch (Exception e) {
            cbox.setItem(defSelection);
        }
    }

    public void setComboServerItems(JEditableComboBox cbox) {
        writeComboItems("comboServerItems", cbox, 20);
    }

    public void getComboServerItems(JEditableComboBox cbox) {
        readComboItems("comboServerItems", cbox);
    }

    public void setComboLoadServerItems(JEditableComboBox cbox) {
        writeComboItems("comboLoadServerItems", cbox, 10);
    }

    public void getComboLoadServerItems(JEditableComboBox cbox) {
        readComboItems("comboLoadServerItems", cbox, "bluedungeon.myip.org");
        cbox.addItem(new String[] { "turingcomplete.net", "bluedungeon.myip.org", "co-sun10.trentu.ca", "152.16.243.212" });
    }

    public void setBlockedClientsList(Set val) {
        writeObject("blockedClientsList", val);
    }

    public Set getBlockedClientsList() {
        try {
            return (Set) readObject("blockedClientsList");
        } catch (Exception e) {
        }
        return Collections.synchronizedSet(new TreeSet(new InetAddressComparator()));
    }

    public InetAddress getLocalHost() {
        return localHost;
    }

    public boolean isLocalHost(InetAddress inet) {
        return inet.getHostName().trim().equalsIgnoreCase("localhost") || Arrays.equals(localHost.getAddress(), inet.getAddress());
    }

    /** @return null if no interface is necessary */
    public InetAddress getConnectingInterfaceInetAddress() {
        try {
            return (InetAddress) readObject("connectingInterfaceInetAddress");
        } catch (Exception e) {
            return null;
        }
    }

    public void setConnectingInterfaceInetAddress(InetAddress inet) {
        writeObject("connectingInterfaceInetAddress", inet);
    }

    public int getConnectingInterfacePort() {
        return get("connectingInterfacePort", connectingInterfacePort);
    }

    public void setConnectingInterfacePort(int val) {
        set("connectingInterfacePort", val);
    }

    /** @return null if no binding server is specified */
    public InetAddress getBindingServerInet() {
        try {
            return (InetAddress) readObject("bindingServerInet");
        } catch (Exception e) {
            return null;
        }
    }

    public void setBindingServerInet(InetAddress inet) {
        writeObject("bindingServerInet", inet);
    }
}
