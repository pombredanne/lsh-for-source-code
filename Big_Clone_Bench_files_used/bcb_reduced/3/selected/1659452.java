package gnu.saw.server;

import gnu.saw.SAW;
import gnu.saw.exceptionhandler.SAWUncaughtExceptionHandler;
import gnu.saw.nativeutils.SAWNativeUtils;
import gnu.saw.parser.SAWArgumentParser;
import gnu.saw.server.connection.SAWServerConnector;
import gnu.saw.server.console.SAWServerConsoleReader;
import gnu.saw.server.help.SAWServerHelpManager;
import gnu.saw.terminal.SAWTerminal;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.configuration.PropertiesConfiguration;

public class SAWServer implements Runnable {

    private boolean passive;

    private boolean daemon;

    private int sessionsLimit = 1;

    private String address = "";

    private Integer port;

    private String proxyType = "DIRECT";

    private String proxyAddress = "";

    private Integer proxyPort;

    private boolean useProxySecurity = false;

    private String proxyUser = "";

    private String proxyPassword = "";

    private String encryptionType = "NONE";

    private byte[] encryptionKey;

    private final String sawURL = System.getenv("SAW_PATH");

    private MessageDigest sha256Digester;

    private File userDatabaseFile;

    private File securitySettingsFile;

    private final Map<byte[], String> userCredentials = new LinkedHashMap<byte[], String>();

    private final PropertiesConfiguration rawUserCredentials = new PropertiesConfiguration();

    private final PropertiesConfiguration rawSecuritySettings = new PropertiesConfiguration();

    private final Runtime runtime = Runtime.getRuntime();

    private Thread consoleThread;

    private Reader userCredentialsReader;

    private Reader securitySettingsReader;

    private SAWServerConnector serverConnector;

    private SAWServerConsoleReader consoleReader;

    static {
        ImageIO.setUseCache(false);
        SAWServerHelpManager.initialize();
    }

    public SAWServer() throws NoSuchAlgorithmException {
        this.sha256Digester = MessageDigest.getInstance("SHA-256");
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public Map<byte[], String> getUserCredentials() {
        return userCredentials;
    }

    public SAWServerConnector getServerConnector() {
        return serverConnector;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public void setSessionsLimit(int sessionsLimit) {
        this.sessionsLimit = sessionsLimit;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public void setProxyAddress(String proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setUseProxySecurity(boolean useProxySecurity) {
        this.useProxySecurity = useProxySecurity;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public void setEncryptionType(String encryptionType) {
        this.encryptionType = encryptionType;
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public void addUserCredential(String login, String password) throws UnsupportedEncodingException {
        byte[] credential = new byte[64];
        System.arraycopy(sha256Digester.digest(login.getBytes("UTF-8")), 0, credential, 0, 32);
        System.arraycopy(sha256Digester.digest(password.getBytes("UTF-8")), 0, credential, 32, 32);
        userCredentials.put(credential, login);
    }

    @SuppressWarnings("unchecked")
    public synchronized void initialize() {
        SAWNativeUtils.initialize();
        if (SAWTerminal.isGraphical()) {
            SAWTerminal.initialize();
            SAWTerminal.setTitle("SATAN-ANYWHERE V:" + SAW.SAW_VERSION + " - Console Terminal");
        } else {
            SAWTerminal.initialize();
            SAWTerminal.setTitle("SATAN-ANYWHERE V:" + SAW.SAW_VERSION + " - Console Terminal");
        }
        SAWTerminal.clear();
        try {
            if (sawURL != null) {
                System.setProperty("java.library.path", sawURL);
            }
            if (userCredentials.isEmpty()) {
                if (sawURL != null) {
                    userDatabaseFile = new File(sawURL + File.separatorChar + "user-database.properties");
                    if (!userDatabaseFile.exists()) {
                        userDatabaseFile = new File("user-database.properties");
                    }
                } else {
                    userDatabaseFile = new File("user-database.properties");
                }
                userCredentialsReader = new InputStreamReader(new FileInputStream(userDatabaseFile), "UTF-8");
                rawUserCredentials.load(userCredentialsReader);
                userCredentialsReader.close();
                Iterator<String> keys = rawUserCredentials.getKeys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    addUserCredential(key, rawUserCredentials.getString(key));
                }
                rawUserCredentials.clear();
            }
        } catch (Exception e) {
            SAWTerminal.print("SAW>SAWSERVER:File 'user-database.properties' not usable on startup!\n");
            if (!daemon) {
                try {
                    SAWTerminal.print("SAW>SAWSERVER:Configure a user account on server?(Y/N, default:N):");
                    String line = SAWTerminal.readLine(true);
                    if (line == null || !line.toUpperCase().startsWith("Y")) {
                        System.exit(0);
                    }
                    SAWTerminal.print("SAW>SAWSERVER:Enter the login:");
                    String login = SAWTerminal.readLine(false);
                    if (login == null) {
                        System.exit(0);
                    }
                    SAWTerminal.print("SAW>SAWSERVER:Enter the password:");
                    String password = SAWTerminal.readLine(false);
                    if (password == null) {
                        System.exit(0);
                    }
                    addUserCredential(login, password);
                } catch (InterruptedException e1) {
                    System.exit(0);
                } catch (UnsupportedEncodingException e1) {
                    System.exit(0);
                }
            } else {
                System.exit(0);
            }
        }
        try {
            if (encryptionType == null || encryptionKey == null) {
                if (sawURL != null) {
                    securitySettingsFile = new File(sawURL + File.separatorChar + "security-settings.properties");
                    if (!securitySettingsFile.exists()) {
                        securitySettingsFile = new File("security-settings.properties");
                    }
                } else {
                    securitySettingsFile = new File("security-settings.properties");
                }
                securitySettingsReader = new InputStreamReader(new FileInputStream(securitySettingsFile), "UTF-8");
                rawSecuritySettings.load(securitySettingsReader);
                securitySettingsReader.close();
                encryptionType = rawSecuritySettings.getString("saw.security.encryption.type");
                encryptionKey = rawSecuritySettings.getString("saw.security.encryption.passphrase").getBytes("UTF-8");
            }
        } catch (Exception e) {
            SAWTerminal.print("SAW>SAWSERVER:File 'security-settings.properties' not usable on startup!\n");
            if (!daemon) {
                try {
                    SAWTerminal.print("SAW>SAWSERVER:Configure encryption on server?(Y/N, default:N):");
                    String line = SAWTerminal.readLine(true);
                    if (line == null) {
                        System.exit(0);
                    }
                    if (line.toUpperCase().startsWith("Y")) {
                        SAWTerminal.print("SAW>SAWSERVER:Enter the encryption type(RC4 as R, AES as A, default:R):");
                        line = SAWTerminal.readLine(false);
                        if (line == null) {
                            System.exit(0);
                        }
                        if (line.toUpperCase().startsWith("A")) {
                            encryptionType = "A";
                        } else {
                            encryptionType = "R";
                        }
                        SAWTerminal.print("SAW>SAWSERVER:Enter the encryption passphrase:");
                        line = SAWTerminal.readLine(false);
                        if (line == null) {
                            System.exit(0);
                        }
                        encryptionKey = line.getBytes("UTF-8");
                    } else {
                        encryptionType = "N";
                    }
                } catch (InterruptedException e1) {
                    System.exit(0);
                } catch (UnsupportedEncodingException e1) {
                    System.exit(0);
                }
            } else {
            }
        }
        SAWTerminal.clear();
        SAWTerminal.print("SAW>SAWSERVER:SATAN-ANYWHERE SERVER V:" + SAW.SAW_VERSION + " - COPYRIGHT (C) " + SAW.SAW_YEAR + "\n");
        SAWTerminal.print("SAW>SAWSERVER:This software is under GPL license, see license.txt for details!\n");
        SAWTerminal.print("SAW>SAWSERVER:This software comes with no warranty, use at your own risk!\n");
    }

    public void configure() {
        while (address == null || port == null || sessionsLimit < 1) {
            SAWTerminal.print("SAW>SAWSERVER:Enter the mode(active as A or passive as P, default:P):");
            try {
                String line = SAWTerminal.readLine(true);
                if (line == null) {
                    System.exit(0);
                }
                if (line.toUpperCase().startsWith("A")) {
                    passive = false;
                    SAWTerminal.print("SAW>SAWSERVER:Enter the host address(default:localhost):");
                    line = SAWTerminal.readLine(true);
                    if (line == null) {
                        System.exit(0);
                    }
                    address = line;
                    SAWTerminal.print("SAW>SAWSERVER:Enter the host port(number from 1 to 65535, default:9999):");
                    line = SAWTerminal.readLine(true);
                    if (line == null) {
                        System.exit(0);
                    }
                    if (line.length() > 0) {
                        port = Integer.parseInt(line);
                    } else {
                        port = 9999;
                    }
                    if (port > 65535 || port < 1) {
                        SAWTerminal.print("SAW>SAWSERVER:Invalid port!\n");
                        port = null;
                    }
                    if (port != null) {
                        SAWTerminal.print("SAW>SAWSERVER:Use proxy for connecting?(Y/N, default:N):");
                        line = SAWTerminal.readLine(true);
                        if (line == null) {
                            System.exit(0);
                        }
                        if (line.toUpperCase().startsWith("Y")) {
                            SAWTerminal.print("SAW>SAWSERVER:Enter the proxy type(Socks as S, HTTP as H, default:S):");
                            line = SAWTerminal.readLine(true);
                            if (line == null) {
                                System.exit(0);
                            }
                            if (line.toUpperCase().startsWith("H")) {
                                proxyType = "HTTP";
                            } else {
                                proxyType = "SOCKS";
                            }
                            SAWTerminal.print("SAW>SAWSERVER:Enter the proxy host address(default:localhost):");
                            line = SAWTerminal.readLine(true);
                            if (line == null) {
                                System.exit(0);
                            }
                            proxyAddress = line;
                            if (proxyType.equals("SOCKS")) {
                                SAWTerminal.print("SAW>SAWSERVER:Enter the proxy port(number from 1 to 65535, default:1080):");
                                line = SAWTerminal.readLine(true);
                                if (line == null) {
                                    System.exit(0);
                                }
                                if (line.length() > 0) {
                                    proxyPort = Integer.parseInt(line);
                                } else {
                                    proxyPort = 1080;
                                }
                            } else if (proxyType.equals("HTTP")) {
                                SAWTerminal.print("SAW>SAWSERVER:Enter the proxy port(number from 1 to 65535, default:8080):");
                                line = SAWTerminal.readLine(true);
                                if (line == null) {
                                    System.exit(0);
                                }
                                if (line.length() > 0) {
                                    proxyPort = Integer.parseInt(line);
                                } else {
                                    proxyPort = 8080;
                                }
                            }
                            if (proxyPort > 65535 || proxyPort < 1) {
                                SAWTerminal.print("SAW>SAWSERVER:Invalid port!\n");
                                proxyPort = null;
                                useProxySecurity = false;
                                port = null;
                            }
                            if (proxyPort != null && port != null) {
                                SAWTerminal.print("SAW>SAWSERVER:Use authentication for proxy?(Y/N, default:N):");
                                line = SAWTerminal.readLine(true);
                                if (line == null) {
                                    System.exit(0);
                                }
                                if (line.toUpperCase().startsWith("Y")) {
                                    SAWTerminal.print("SAW>SAWSERVER:Enter the proxy username:");
                                    line = SAWTerminal.readLine(true);
                                    if (line == null) {
                                        System.exit(0);
                                    }
                                    proxyUser = line;
                                    SAWTerminal.print("SAW>SAWSERVER:Enter the proxy password:");
                                    line = SAWTerminal.readLine(true);
                                    if (line == null) {
                                        System.exit(0);
                                    }
                                    proxyPassword = line;
                                } else {
                                    useProxySecurity = false;
                                }
                            } else {
                                useProxySecurity = false;
                            }
                        } else {
                            proxyType = "DIRECT";
                        }
                    }
                } else {
                    passive = true;
                    sessionsLimit = 0;
                    address = "";
                    SAWTerminal.print("SAW>SAWSERVER:Enter the listen port(number from 1 to 65535, default:6666):");
                    line = SAWTerminal.readLine(true);
                    if (line == null) {
                        System.exit(0);
                    }
                    if (line.length() > 0) {
                        port = Integer.parseInt(line);
                    } else {
                        port = 6666;
                    }
                    if (port > 65535 || port < 1) {
                        SAWTerminal.print("SAW>SAWSERVER:Invalid port!\n");
                        port = null;
                        sessionsLimit = 1;
                    } else {
                        try {
                            SAWTerminal.print("SAW>SAWSERVER:Enter the sessions limit(number from 1 to 65535, default:1):");
                            line = SAWTerminal.readLine(true);
                            if (line == null) {
                                System.exit(0);
                            }
                            if (!line.trim().equals("")) {
                                sessionsLimit = Integer.parseInt(line);
                                if (sessionsLimit > 65535) {
                                    sessionsLimit = 0;
                                }
                            } else {
                                sessionsLimit = 1;
                            }
                        } catch (NumberFormatException e) {
                            sessionsLimit = 0;
                        } catch (Exception e) {
                            sessionsLimit = 0;
                        }
                    }
                    if (sessionsLimit < 1) {
                        SAWTerminal.print("SAW>SAWSERVER:Invalid limit!\n");
                    }
                }
            } catch (NumberFormatException e) {
                SAWTerminal.print("SAW>SAWSERVER:Invalid port!\n");
                port = null;
                proxyPort = null;
                useProxySecurity = false;
            } catch (Exception e) {
            }
            if (address == null || port == null || sessionsLimit < 1) {
                SAWTerminal.print("SAW>SAWSERVER:Try configuring again?(Y/N, default:N):");
                try {
                    String line = SAWTerminal.readLine(true);
                    if (line == null || !line.toUpperCase().startsWith("Y")) {
                        System.exit(0);
                    }
                } catch (Exception e) {
                    System.exit(0);
                }
            }
        }
    }

    public void parseMainParameter(String parameter) throws Exception {
        String[] subParameters = SAWArgumentParser.parseParameter(parameter, ':');
        if (subParameters.length > 1) {
            this.passive = false;
            this.address = subParameters[0];
            this.port = Integer.parseInt(subParameters[1]);
        } else if (subParameters.length == 1) {
            this.passive = true;
            this.port = Integer.parseInt(subParameters[0]);
        }
    }

    public void parseOptionalParameter(String parameter) throws Exception {
        String[] subParameters;
        int colonCount = SAWArgumentParser.countDelimiterInParameter(parameter, ':');
        int slashCount = SAWArgumentParser.countDelimiterInParameter(parameter, '/');
        if (colonCount > 0) {
            if (slashCount > 0) {
                subParameters = SAWArgumentParser.parseParameter(parameter, ':');
                this.proxyPort = Integer.parseInt(subParameters[1]);
                subParameters = SAWArgumentParser.parseParameter(subParameters[0], '/');
                if (subParameters.length == 2) {
                    this.proxyType = subParameters[0];
                    this.proxyAddress = subParameters[1];
                } else if (subParameters.length == 4) {
                    this.proxyType = subParameters[0];
                    this.proxyUser = subParameters[1];
                    this.proxyPassword = subParameters[2];
                    this.proxyAddress = subParameters[3];
                }
            } else {
                subParameters = SAWArgumentParser.parseParameter(parameter, ':');
                addUserCredential(subParameters[0], subParameters[1]);
            }
        } else if (slashCount > 0) {
            subParameters = SAWArgumentParser.parseParameter(parameter, '/');
            this.encryptionType = subParameters[0];
            this.encryptionKey = subParameters[1].getBytes("UTF-8");
        } else {
            this.sessionsLimit = Integer.parseInt(parameter);
        }
    }

    public void start() {
        Thread.setDefaultUncaughtExceptionHandler(new SAWUncaughtExceptionHandler());
        Thread.currentThread().setName("SAWServer");
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        run();
    }

    public void run() {
        serverConnector = new SAWServerConnector(this);
        serverConnector.setPassive(passive);
        serverConnector.setAddress(address);
        serverConnector.setPort(port);
        serverConnector.setProxyType(proxyType);
        serverConnector.setProxyAddress(proxyAddress);
        serverConnector.setProxyPort(proxyPort);
        serverConnector.setUseProxySecurity(useProxySecurity);
        serverConnector.setProxyUser(proxyUser);
        serverConnector.setProxyPassword(proxyPassword);
        serverConnector.setSessionsLimit(sessionsLimit);
        if (encryptionType != null && encryptionKey != null) {
            serverConnector.setEncryptionType(encryptionType);
            serverConnector.setEncryptionKey(encryptionKey);
        } else {
            serverConnector.setEncryptionType("NONE");
            serverConnector.setEncryptionKey(encryptionKey);
        }
        if (!daemon) {
            consoleReader = new SAWServerConsoleReader(this);
            consoleThread = new Thread(consoleReader, "SAWServerConsoleReader");
            consoleThread.setPriority(Thread.NORM_PRIORITY);
            consoleThread.start();
        }
        serverConnector.run();
    }
}
