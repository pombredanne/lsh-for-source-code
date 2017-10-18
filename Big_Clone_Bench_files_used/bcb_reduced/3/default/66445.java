import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.jdom.*;

/**
 * the Main class is a template for all actions of the program.
 * It contains the timers to execute the tests and the rssfeed updates aswell as the
 * methods to create the Gui and all other window instances.
 * 
 */
public class Main {

    static final double progVersion = 0.41;

    public static boolean validUserdata;

    public static boolean internet = false;

    public static String[] masterTest = { "ftp.kuleuven.net", "134.58.250.2" };

    private static int delay = 3600;

    private static int initialDelay = (int) Math.round(Math.random() * (delay * 1000));

    private static int rssDelay = 300;

    private static int rssInitialDelay = 30;

    private static Timer t = new Timer();

    private static Timer rss = new Timer();

    private static Timer testClock = new Timer();

    private static int nextTime = initialDelay / 1000;

    private static String nextTest = "";

    private static String user = "";

    private static ArrayList<HashMap> testParamsArray = new ArrayList();

    private static ArrayList<iTest> tests = new ArrayList();

    private static ArrayList<HashMap> resultArray = new ArrayList();

    private static boolean cfgFileExists = true;

    private static Gui gui = new Gui();

    private static SystemTray tray = null;

    private static Image trayImg = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("palantir_aboutlogo.png"));

    private static PopupMenu popup = new PopupMenu();

    private static TrayIcon trayIcon = null;

    private static TestController testC = new TestController();

    private static RssController rssC = null;

    private static XmlRpcController xmlRpcC = null;

    private static boolean testVar = true;

    private static boolean rssVar = true;

    static final String varFile = "var.xml";

    public static String homeDir = System.getProperty("user.home");

    private static String logDir = "";

    static final LogController logC = new LogController();

    public static XMLController xmlC = new XMLController();

    public static String configFile = "";

    private static String updateServer = "";

    private static String userdataFile = "";

    private static String resultsFile = "";

    private static String xmlrpcserver = "";

    static String feedUrl = "";

    public static String rssXmlFile = "";

    private static int feedAmount = 0;

    public static void main(String[] args) {
        Debug.init();
        Debug.enable();
        Debug.log("Main", "Palantir version " + progVersion + " starting");
        Debug.logEnvironment();
        Debug.disable();
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("--debug")) {
                Debug.enable();
                delay = 120;
                initialDelay = 5;
                Debug.log("Main", "Running tests at " + delay + " seconds interval, starting in " + initialDelay + " seconds");
            }
        }
        try {
            ServerSocket s = new ServerSocket(55555);
        } catch (Exception e) {
            System.err.println("Another instance is allready running");
            System.exit(0);
        }
        initConfig();
        logC.writeToLog(0, "Starting Palantir");
        ActionListener exitListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        if (SystemTray.isSupported()) {
            gui.setVisible(false);
            Debug.log("Main", "creating trayicon");
            trayIcon = new TrayIcon(trayImg, "Palantir", popup);
            MouseAdapter mouseListener = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (gui.isVisible()) {
                            gui.setVisible(false);
                        } else {
                            gui.setVisible(true);
                        }
                    }
                }
            };
            trayIcon.addMouseListener(mouseListener);
            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(exitListener);
            popup.add(exit);
            trayIcon.setImageAutoSize(true);
            try {
                tray = SystemTray.getSystemTray();
                tray.add(trayIcon);
            } catch (Exception e) {
                logC.writeToLog(2, "Unable to add the trayicon");
            }
        } else {
            gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            gui.setVisible(true);
            logC.writeToLog(2, "Systemtray is not Supported on this platform");
        }
        validUserdata = checkCredentials();
        testClock.schedule(new TimerTask() {

            public void run() {
                Debug.log("Tooltip Updater", "Starting run");
                if (testVar) {
                    int tempTime = nextTime;
                    int hr = tempTime / 3600;
                    tempTime -= 3600 * hr;
                    int min = tempTime / 60;
                    tempTime -= 60 * min;
                    int sec = tempTime;
                    nextTest = "Next Test in " + hr + ":" + min + ":" + sec;
                } else {
                    nextTest = "Tests Disabled";
                }
                nextTime -= 5;
                if (nextTime <= 0) {
                    nextTest = "Running Tests";
                }
                getUsername();
                String dbgEnabled = Debug.isEnabled() ? " [Debug]" : "";
                String userstr = user.equalsIgnoreCase("") ? "No username" : "Username: " + user;
                trayIcon.setToolTip("Palantir - " + nextTest + " - " + userstr + dbgEnabled);
                gui.setTitle("Palantir - " + nextTest + " - " + userstr + dbgEnabled);
            }
        }, 0, 5000);
        rss.schedule(new TimerTask() {

            ArrayList<HashMap> feed = new ArrayList<HashMap>();

            ArrayList<HashMap> newFeed = new ArrayList<HashMap>();

            ArrayList<HashMap> tempFeed = new ArrayList<HashMap>();

            public void run() {
                Debug.log("RSS Updater", "Starting run");
                logC.writeToLog(0, "Attempting RSS Read");
                if (rssVar) {
                    Debug.log("RSS Updater", "rss variables read succesfully from var.xml");
                    try {
                        Debug.log("RSS Updater", "reading local rss.xml file");
                        feed = rssC.getLocalFeed(rssXmlFile);
                    } catch (Exception ex) {
                        Debug.log("RSS Updater", "no local rss feed found");
                        logC.writeToLog(0, "No local feed found, trying online.");
                        feed = tempFeed;
                    }
                    try {
                        Debug.log("RSS Updater", "reading rss feed online");
                        newFeed = rssC.getFeed();
                    } catch (Exception e) {
                        Debug.log("RSS Updater", "online rss feed read failed");
                        logC.writeToLog(2, "Unable to read online feed.");
                    }
                    Debug.log("RSS Updater", "comparing old and new feed");
                    if (!feed.equals(newFeed)) {
                        if (!newFeed.isEmpty()) {
                            trayIcon.displayMessage("News!", "There is a new newspost", TrayIcon.MessageType.INFO);
                            try {
                                Debug.log("RSS Updater", "writing new feed to local xml file");
                                rssC.feedToLocalXML(newFeed, rssXmlFile);
                            } catch (Exception exx) {
                                Debug.log("RSS Updater", "local feed file write failed, keeping copy in memory");
                                tempFeed = newFeed;
                            }
                        }
                        Debug.log("RSS Updater", "updating gui news tab");
                        gui.setNewsTab();
                    } else {
                        if (feed.isEmpty()) {
                            gui.setNewsTab("Unable to load the feed");
                        } else {
                            Debug.log("RSS Updater", "updating gui news tab");
                            gui.setNewsTab();
                        }
                    }
                }
            }
        }, rssInitialDelay * 1000, rssDelay * 1000);
        t.schedule(new TimerTask() {

            public void run() {
                Debug.log("Tests", "Starting run");
                logC.writeToLog(0, "Attempting to run tests");
                if (testVar) {
                    Debug.log("Tests", "test variables read succesfully");
                    internet = false;
                    Debug.log("Tests", "executing mastertest");
                    for (int i = 0; i < masterTest.length; i++) {
                        try {
                            Debug.log("Tests", "Mastertest: trying to connect to " + masterTest[i]);
                            Socket s = new Socket();
                            InetAddress addr = InetAddress.getByName(masterTest[i]);
                            SocketAddress sockAddr = new InetSocketAddress(addr, 21);
                            s.connect(sockAddr, 300);
                            s.close();
                            Debug.log("Tests", "Mastertest: connect to " + masterTest[i] + " succesful. Enabling other tests");
                            internet = true;
                        } catch (Exception e) {
                            Debug.log("Tests", "Mastertest to " + masterTest[i] + " Failed");
                            logC.writeToLog(1, "Mastertest to " + masterTest[i] + " Failed");
                        }
                    }
                    if (internet) {
                        double latestVersion = 0.0;
                        try {
                            Debug.log("Tests", "Checking for new client version");
                            latestVersion = Double.parseDouble(xmlRpcC.getLatestVersion());
                        } catch (Exception e) {
                            System.err.println(e.toString());
                        }
                        if (latestVersion > progVersion) {
                            Debug.log("Tests", "Current version (" + progVersion + ") is older than latest version (" + latestVersion + ")");
                            trayIcon.displayMessage("New Version!", "There is a new version available.\n Please Upgrade.", TrayIcon.MessageType.ERROR);
                        } else {
                            Debug.log("Tests", "Current version (" + progVersion + ") is up to date with latest version (" + latestVersion + ")");
                        }
                        try {
                            Debug.log("Tests", "checking for new configfile");
                            logC.writeToLog(0, "Checking for new configfile");
                            CfgUpdater cfgUpdater = new CfgUpdater();
                            cfgUpdater.update(updateServer + configFile, xmlC.readCfgVersion(homeDir + configFile));
                            if (cfgUpdater.getResult() != null) {
                                trayIcon.displayMessage("Updater", cfgUpdater.getResult(), TrayIcon.MessageType.INFO);
                            }
                        } catch (Exception e) {
                            Debug.log("Tests", "unable to retrieve new configfile, checking for existing (old) one");
                            if (new File(homeDir + configFile).exists()) {
                                logC.writeToLog(2, "Unable to retrieve a new configfile, using existing version (" + homeDir + configFile + ")");
                                cfgFileExists = true;
                            } else {
                                Debug.log("Tests", "no existing configfile found");
                                cfgFileExists = false;
                                logC.writeToLog(1, "No config file exists and unable to retrieve a new one");
                            }
                        }
                        if (cfgFileExists && validUserdata) {
                            Debug.log("Tests", "running tests");
                            logC.writeToLog(0, "Running tests");
                            Debug.log("Tests", "reading configfile for tests");
                            testParamsArray = xmlC.readTests(homeDir + configFile);
                            Debug.log("Tests", "creating array of tests");
                            tests = testC.createTests(testParamsArray);
                            Debug.log("Tests", "Created array with " + tests.size() + " tests");
                            Iterator itTests = tests.iterator();
                            int testCounter = 1;
                            Debug.log("Tests", "Looping thru " + tests.size() + " tests found in the configfile");
                            while (itTests.hasNext()) {
                                String prefix = "[" + testCounter + "/" + tests.size() + "] ";
                                iTest test = (iTest) itTests.next();
                                Debug.log("Tests", prefix + "Reading old unsent results");
                                resultArray = xmlC.readResults(homeDir + resultsFile);
                                int aResults = resultArray.size();
                                Debug.log("Tests", prefix + "Found " + aResults + " unsent results");
                                Debug.log("Tests", prefix + "Executing test with description: " + test);
                                resultArray = testC.execTest(test, resultArray);
                                int diff = resultArray.size() - aResults;
                                Debug.log("Tests", prefix + "Test resulted in " + diff + " new results, total unset now: " + resultArray.size());
                                for (int i = diff; i != 0; i--) {
                                    Debug.log("Tests", prefix + "updating gui test tab");
                                    gui.setTestTabText(resultArray.get(resultArray.size() - i));
                                }
                                Debug.log("Tests", prefix + "attempting to send the results to the server");
                                Iterator rIt = resultArray.iterator();
                                while (rIt.hasNext()) {
                                    HashMap result = (HashMap) rIt.next();
                                    try {
                                        String userdata = xmlC.readUserdata(userdataFile);
                                        Debug.log("Tests", prefix + "reading userdata to authenticate with the server");
                                        String[] udata = userdata.split("/");
                                        result.put("username", udata[0]);
                                        result.put("passhash", udata[1]);
                                        result.put("progversion", progVersion);
                                        Debug.log("Tests", prefix + "sending result to the server");
                                        xmlRpcC.storeResult(result);
                                        rIt.remove();
                                        Debug.log("Tests", prefix + "write remaining results to xml file");
                                        xmlC.writeResults(resultArray, homeDir + resultsFile);
                                        Debug.log("Tests", prefix + "call done");
                                    } catch (Exception ex) {
                                        logC.writeToLog(1, "Unable to send results, saving local");
                                        Debug.log("Tests", prefix + "sending of result failed");
                                        Debug.log("Tests", ex.toString());
                                        Debug.log("Tests", prefix + "call failed");
                                        if (ex.getMessage().toLowerCase().contains("database") || ex.getMessage().toLowerCase().contains("query")) {
                                            logC.writeToLog(1, "Database Problem, try again later");
                                            Debug.log("Tests", prefix + "Database Problem, try again later");
                                        } else {
                                            if (ex.getMessage().toLowerCase().contains("invalid userinfo")) {
                                                new File(userdataFile).delete();
                                                logC.writeToLog(2, "No valid userdata found");
                                                Debug.log("Tests", prefix + "No valid userdata found");
                                            }
                                        }
                                        Debug.log("Tests", prefix + "write results to xml file");
                                        xmlC.writeResults(resultArray, homeDir + resultsFile);
                                    }
                                }
                                cleanUp();
                                testCounter++;
                            }
                            tests.clear();
                            Debug.log("Tests", "All tests executed. Sleeping untill next invocation");
                        } else {
                            Debug.log("Tests", "unable to retrieve configfile/no existing one found. Waiting for next attempt");
                            logC.writeToLog(1, "No configuration file was found, unable to retrieve new file");
                            trayIcon.displayMessage("Configuration Error", "No configuration file was found - Unable to contact the updateserver - Try again later", TrayIcon.MessageType.ERROR);
                        }
                    }
                }
                nextTime = delay;
            }

            public void cleanUp() {
                Debug.log("Tests", "cleanup the previous tests");
                testParamsArray.clear();
                resultArray.clear();
            }
        }, initialDelay, delay * 1000);
    }

    /**
     * This method will attempt to read the values for all the needed variables.
     * This will be done in sections. If a variable for a section fails this part of the program is disabled
     * If the file does not exist or is corrupt, the program is terminated.
     */
    public static void initConfig() {
        Debug.log("Main.initConfig", "setting initial configuration variables");
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            homeDir += "\\Palantir\\";
            logDir += homeDir + "logs\\";
        } else {
            homeDir += "/.Palantir/";
            logDir += homeDir + "logs/";
        }
        Debug.log("Main.initConfig", "Creating homedir '" + homeDir + "' and logdir '" + logDir + "'");
        new File(homeDir).mkdirs();
        new File(logDir).mkdirs();
        Debug.log("Main.initConfig", "checking for var.xml file");
        if (!xmlC.varFileExists(varFile)) {
            logC.writeToLog(2, "Varfile not found");
            Debug.log("Main.initConfig", "var file not found");
            JOptionPane.showMessageDialog(null, "Unable to load the configuration\n Closing the program", "Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else {
            Debug.log("Main.initConfig", "var file found");
            try {
                configFile = xmlC.getVarValue(varFile, "tests", "configfile");
                updateServer = xmlC.getVarValue(varFile, "tests", "updateserver");
                userdataFile = homeDir + xmlC.getVarValue(varFile, "tests", "userdatafile");
                resultsFile = homeDir + xmlC.getVarValue(varFile, "tests", "resultsfile");
                xmlrpcserver = xmlC.getVarValue(varFile, "tests", "xmlrpcserver");
                xmlRpcC = new XmlRpcController(xmlrpcserver);
            } catch (Exception e) {
                testVar = false;
                logC.writeToLog(1, "Unable to load test settings, testmodule disabled");
                Debug.log("Main.initConfig", "unable to read testmodule variables from var.xml");
            }
            try {
                feedUrl = xmlC.getVarValue(varFile, "rss", "feedurl");
                feedAmount = Integer.parseInt(xmlC.getVarValue(varFile, "rss", "feedamount"));
                rssXmlFile = homeDir + xmlC.getVarValue(varFile, "rss", "rssfile");
                rssC = new RssController(feedUrl, feedAmount);
            } catch (Exception e) {
                e.printStackTrace();
                rssVar = false;
                Debug.log("Main.initConfig", "unable to read rss module variables from var.xml");
                JOptionPane.showMessageDialog(null, "Error loading rssdata", "There has been an error loading the rss data\n this module will not work", JOptionPane.ERROR_MESSAGE);
                logC.writeToLog(1, "Unable to load rss settings, rssmodule disabled");
            }
            if (!testVar && !rssVar) {
                Debug.log("Main.initConfig", "Unable to load settings, Varfile corrupt");
                logC.writeToLog(1, "Unable to load settings, Varfile corrupt");
                JOptionPane.showMessageDialog(null, "Unable to load the configuration\n Closing the program", "Fatal Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    /**
     * 
     * try to read a existing userdata file, if the read is successfull a true boolean is returned
     * else the user can provide the credentials
     * 
     * @return a boolean indicating if valid credentials were returned
     */
    public static boolean checkCredentials() {
        boolean credentials = false;
        try {
            Debug.log("Main.checkCredentials", "checking for userdata file");
            xmlC.readUserdata(userdataFile);
            credentials = true;
        } catch (Exception e) {
            Debug.log("Main.checkCredentials", "userdata file not found");
            System.err.println(e.toString());
            JOptionPane.showMessageDialog(null, "No user data was found!", "Warning", JOptionPane.WARNING_MESSAGE);
            Debug.log("Main.checkCredentials", "prompting for credentials");
            credentials = changeCredentials();
        }
        return credentials;
    }

    /**
     * attempt to get the users information, if invalid data is supplied another attempt is done
     * if the user cancels the input a boolean "credentials" with a false value is returned
     * else this boolean is true
     * 
     * @return a boolean indicating if valid credentials were found
     */
    public static boolean changeCredentials() {
        boolean passed = false;
        boolean credentials = false;
        HashMap info = null;
        Debug.log("Main.changeCredentials", "show dialog for userinfo");
        info = getUserInfo();
        if ((Boolean) info.get("submit")) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(info.get("password").toString().getBytes());
                String passHash = new BigInteger(1, md5.digest()).toString(16);
                Debug.log("Main.changeCredentials", "validate credentials with the database");
                passed = xmlRpcC.checkUser(info.get("username").toString(), passHash);
                Debug.log("Main.changeCredentials", "write the credentials to file");
                xmlC.writeUserdata(userdataFile, info.get("username").toString(), passHash);
                credentials = passed;
                testVar = true;
            } catch (Exception ex) {
                System.out.println(ex.toString());
                if (ex.getMessage().toLowerCase().contains("unable")) {
                    JOptionPane.showMessageDialog(null, "Database problem occured, please try again later", "Error", JOptionPane.ERROR_MESSAGE);
                    passed = true;
                    testVar = false;
                } else {
                    passed = Boolean.parseBoolean(ex.getMessage());
                    JOptionPane.showMessageDialog(null, "Invallid userdata, try again", "Invallid userdata", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            if (new File(userdataFile).exists()) {
                testVar = true;
                credentials = true;
            } else {
                testVar = false;
                JOptionPane.showMessageDialog(null, "No userdata was entered\nNo tests will be executed until you enter them ", "Warning", JOptionPane.ERROR_MESSAGE);
            }
            passed = true;
        }
        while (!passed) {
            Debug.log("Main.changeCredentials", "show dialog for userinfo");
            info = getUserInfo();
            if ((Boolean) info.get("submit")) {
                try {
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    md5.update(info.get("password").toString().getBytes());
                    String passHash = new BigInteger(1, md5.digest()).toString(16);
                    Debug.log("Main.changeCredentials", "validate credentials with the database");
                    passed = xmlRpcC.checkUser(info.get("username").toString(), passHash);
                    Debug.log("Main.changeCredentials", "write credentials to local xml file");
                    xmlC.writeUserdata(userdataFile, info.get("username").toString(), passHash);
                    credentials = passed;
                    testVar = true;
                } catch (Exception ex) {
                    Debug.log("Main.changeCredentials", "credential validation failed");
                    passed = Boolean.parseBoolean(ex.getMessage());
                    JOptionPane.showMessageDialog(null, "Invallid userdata, try again", "Invallid userdata", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                if (new File(userdataFile).exists()) {
                    testVar = true;
                    credentials = true;
                } else {
                    testVar = false;
                    JOptionPane.showMessageDialog(null, "No userdata was entered\nNo tests will be executed untill u enter them ", "Warning", JOptionPane.ERROR_MESSAGE);
                }
                passed = true;
            }
        }
        return credentials;
    }

    /**
     * Creates a dialog window requesting the users username and password
     * 
     * @return a hashmap containing the username/password and the name of the button that was clicked
     */
    public static HashMap getUserInfo() {
        HashMap info = new HashMap();
        UserinfoDialog dialog = new UserinfoDialog(null, true);
        if (dialog.getSubmitState()) {
            info.put("submit", dialog.getSubmitState());
            info.put("username", dialog.getUsername());
            info.put("password", dialog.getPassword());
        } else {
            info.put("submit", dialog.getSubmitState());
        }
        dialog.dispose();
        return info;
    }

    public static void getUsername() {
        try {
            String userinfo = xmlC.readUserdata(userdataFile);
            String[] udata = userinfo.split("/");
            user = udata[0];
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    public static String getHomeDir() {
        return homeDir;
    }

    public static String getLogDir() {
        return logDir;
    }

    public static String getNextTest() {
        return nextTest;
    }
}
