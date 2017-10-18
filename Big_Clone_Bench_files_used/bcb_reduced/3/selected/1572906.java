package org.amiwall;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import org.amiwall.db.ConnectionPool;
import org.amiwall.listener.Listener;
import org.amiwall.listener.SquidListener;
import org.amiwall.logger.DbLogger;
import org.amiwall.logger.Logger;
import org.amiwall.plugin.Install;
import org.amiwall.plugin.Plugin;
import org.amiwall.plugin.PluginSet;
import org.amiwall.policy.DefaultPolicy;
import org.amiwall.policy.Policy;
import org.amiwall.user.GroupHome;
import org.amiwall.user.UserHome;
import org.amiwall.user.XmlGroupHome;
import org.amiwall.user.XmlUserHome;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;

/**
 *  Description of the Class
 *
 *@author    Nick Cunnah
 */
public class AmiWall {

    /**
     *  Description of the Field
     */
    public static File HOME = null;

    /**
     *  Description of the Field
     */
    public static File CONF = null;

    PluginSet listeners = null;

    Logger logger = null;

    UserHome userHome = null;

    GroupHome groupHome = null;

    /**
     *  Description of the Field
     */
    protected Policy policy = null;

    File configFile = null;

    /**
     *  Description of the Field
     */
    public static final String VERSION = "$Id: AmiWall.java,v 1.10 2004/08/16 20:51:17 lonnc Exp $";

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger("org.amiwall.AmiWall");

    /**
     *  Constructor for the AmiWall object
     */
    public AmiWall() {
    }

    /**
     *  Sets the configFile attribute of the AmiWall object
     *
     *@param  configFile        The new configFile value
     *@exception  IOException   Description of the Exception
     *@exception  SAXException  Description of the Exception
     */
    public void setConfigFile(File configFile) throws IOException, SAXException {
        this.configFile = configFile;
    }

    /**
     *  Gets the configFile attribute of the AmiWall object
     *
     *@return    The configFile value
     */
    public File getConfigFile() {
        return configFile;
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void config() throws Exception {
        log.info("Configuring");
        SAXBuilder builder = new SAXBuilder();
        Document config = builder.build(configFile);
        digestConstants(config.getRootElement());
        digest(config.getRootElement());
        if (log.isDebugEnabled()) {
            log.debug("Finished configuring");
        }
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    protected void digestConstants(Element root) {
        setHome(root.getChildTextTrim("home"));
        setConf(root.getChildTextTrim("conf"));
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    protected void digest(Element root) {
        if (log.isDebugEnabled()) {
            log.debug("digest");
        }
        listeners = new PluginSet();
        digestLogger(root.getChild("logger"));
        digestGroupHome(root.getChild("grouphome"));
        digestUserHome(root.getChild("userhome"));
        digestPolicy(root.getChild("policy"));
        digestListeners(root.getChild("listener"));
        Element db = root.getChild("db");
        digestDb(db);
        if (log.isDebugEnabled()) {
            log.debug("digest done");
        }
    }

    /**
     *  Description of the Method
     *
     *@param  db  Description of the Parameter
     *@return     Description of the Return Value
     */
    protected ConnectionPool digestDb(Element db) {
        ConnectionPool connectionPool = ConnectionPool.getInstance();
        connectionPool.setDriver(db.getChildTextTrim("driver"));
        connectionPool.setUser(db.getChildTextTrim("user"));
        connectionPool.setPassword(db.getChildTextTrim("password"));
        connectionPool.setUrl(db.getChildTextTrim("url"));
        return connectionPool;
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    protected void digestListeners(Element root) {
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            Listener listener = getListener(child.getName());
            if (listener != null) {
                listener.digest(child);
                addListener(listener);
            }
        }
    }

    /**
     *  Gets the listener attribute of the AmiWall object
     *
     *@param  name  Description of the Parameter
     *@return       The listener value
     */
    protected Listener getListener(String name) {
        if (name.equals("SquidListener")) {
            return new SquidListener();
        }
        return null;
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    protected void digestPolicy(Element root) {
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            Policy policy = getPolicy(child.getName());
            if (policy != null) {
                policy.digest(child);
                setPolicy(policy);
                break;
            }
        }
    }

    /**
     *  Gets the policy attribute of the AmiWall object
     *
     *@param  name  Description of the Parameter
     *@return       The policy value
     */
    protected Policy getPolicy(String name) {
        if (name.equals("DefaultPolicy")) {
            return new DefaultPolicy();
        }
        return null;
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    protected void digestLogger(Element root) {
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            Logger logger = getLogger(child.getName());
            if (logger != null) {
                logger.digest(child);
                setLogger(logger);
                break;
            }
        }
    }

    /**
     *  Gets the logger attribute of the AmiWall object
     *
     *@param  name  Description of the Parameter
     *@return       The logger value
     */
    protected Logger getLogger(String name) {
        if (name.equals("DbLogger")) {
            return new DbLogger();
        }
        return null;
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    protected void digestUserHome(Element root) {
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            UserHome userHome = getUserHome(child.getName());
            if (userHome != null) {
                userHome.digest(child);
                setUserHome(userHome);
                break;
            }
        }
    }

    /**
     *  Gets the userHome attribute of the AmiWall object
     *
     *@param  name  Description of the Parameter
     *@return       The userHome value
     */
    protected UserHome getUserHome(String name) {
        if (name.equals("XmlUserHome")) {
            return new XmlUserHome();
        }
        return null;
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    protected void digestGroupHome(Element root) {
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            GroupHome groupHome = getGroupHome(child.getName());
            if (groupHome != null) {
                groupHome.digest(child);
                setGroupHome(groupHome);
                break;
            }
        }
    }

    /**
     *  Gets the groupHome attribute of the AmiWall object
     *
     *@param  name  Description of the Parameter
     *@return       The groupHome value
     */
    protected GroupHome getGroupHome(String name) {
        if (name.equals("XmlGroupHome")) {
            return new XmlGroupHome();
        }
        return null;
    }

    /**
     *  Sets the home attribute of the AmiWall object
     *
     *@param  home  The new home value
     */
    public void setHome(String home) {
        if (home.length() == 0 || home.equals("./")) {
            HOME = new File(System.getProperty("user.dir"));
        } else {
            HOME = new File(home);
        }
        checkDirectory(HOME);
    }

    /**
     *  Sets the conf attribute of the AmiWall object
     *
     *@param  conf  The new conf value
     */
    public void setConf(String conf) {
        CONF = getDirAbsPath(conf);
    }

    /**
     *  Gets the dirAbsPath attribute of the AmiWall object
     *
     *@param  dir  Description of the Parameter
     *@return      The dirAbsPath value
     */
    protected File getDirAbsPath(String dir) {
        File c = new File(dir);
        if (!c.isAbsolute()) {
            c = new File(HOME, dir);
        }
        checkDirectory(c);
        return c;
    }

    /**
     *  Description of the Method
     *
     *@param  dir  Description of the Parameter
     */
    void checkDirectory(File dir) {
        log.info("checkDirectory: " + dir + " abs: " + dir.getAbsolutePath());
        if (!dir.exists()) {
            log.warn("Directory does not exist: " + dir + " creating...");
            dir.mkdirs();
        } else if (!dir.isDirectory()) {
            log.warn("Not a directory: " + dir.getAbsolutePath());
        }
    }

    /**
     *  Sets the logger attribute of the AmiWall object
     *
     *@param  logger  The new logger value
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     *  Gets the logger attribute of the AmiWall object
     *
     *@return    The logger value
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     *  Sets the userhome attribute of the AmiWall object
     *
     *@param  userHome  The new userHome value
     */
    public void setUserHome(UserHome userHome) {
        this.userHome = userHome;
    }

    /**
     *  Sets the grouphome attribute of the AmiWall object
     *
     *@param  groupHome  The new groupHome value
     */
    public void setGroupHome(GroupHome groupHome) {
        this.groupHome = groupHome;
    }

    /**
     *  Gets the groupHome attribute of the AmiWall object
     *
     *@return    The groupHome value
     */
    public GroupHome getGroupHome() {
        return groupHome;
    }

    /**
     *  Adds a feature to the Listener attribute of the AmiWall object
     *
     *@param  listener  The feature to be added to the Listener attribute
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
        if (logger == null) {
            throw new NullPointerException("cant add listener " + listener.getName() + " until after a logger has been set");
        }
        listener.setLogger(logger);
        if (userHome == null) {
            throw new NullPointerException("cant add listener " + listener.getName() + " until after a userHome has been set");
        }
        listener.setUserHome(userHome);
        if (policy == null) {
            throw new NullPointerException("cant add listener " + listener.getName() + " until after a policy has been set");
        }
        listener.setPolicy(policy);
    }

    /**
     *  Sets the policy attribute of the AmiWall object
     *
     *@param  policy  The new policy value
     */
    public void setPolicy(Policy policy) {
        this.policy = policy;
        if (userHome == null) {
            throw new NullPointerException("cant add policy " + policy.getName() + " until after a userHome has been set");
        }
        policy.setUserHome(userHome);
        if (groupHome == null) {
            throw new NullPointerException("cant add policy " + policy.getName() + " until after a groupHome has been set");
        }
        policy.setGroupHome(this.groupHome);
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activate() throws Exception {
        if (logger == null) {
            throw new NullPointerException("logger is NULL, this needs to be configured");
        }
        if (userHome == null) {
            throw new NullPointerException("userHome is NULL, this needs to be configured");
        }
        if (groupHome == null) {
            throw new NullPointerException("groupHome is NULL, this needs to be configured");
        }
        if (policy == null) {
            throw new NullPointerException("policy is NULL, this needs to be configured");
        }
        logger.activate();
        userHome.activate();
        groupHome.activate();
        policy.activate();
        listeners.activate();
    }

    /**
     *  Description of the Method
     */
    public void deactivate() {
        listeners.deactivate();
        listeners = null;
        policy.deactivate();
        policy = null;
        groupHome.deactivate();
        groupHome = null;
        userHome.deactivate();
        userHome = null;
        logger.deactivate();
        logger = null;
        try {
            ConnectionPool.getInstance().close();
        } catch (Exception e) {
            log.error("Problem closing connection pool", e);
        }
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void install() throws Exception {
        for (Iterator i = listeners.iterator(); i.hasNext(); ) {
            Plugin plugin = (Plugin) i.next();
            if (plugin instanceof Install) {
                install((Install) plugin);
            }
        }
        if (logger instanceof Install) {
            install((Install) logger);
        }
        if (userHome instanceof Install) {
            install((Install) userHome);
        }
        if (groupHome instanceof Install) {
            install((Install) groupHome);
        }
        if (policy instanceof Install) {
            install((Install) policy);
        }
        log.info("Completed install");
    }

    /**
     *  Description of the Method
     *
     *@param  install        Description of the Parameter
     *@exception  Exception  Description of the Exception
     */
    protected void install(Install install) throws Exception {
        try {
            log.info("install " + install.getName());
            install.install();
        } catch (Exception e) {
            log.error("Couldnt install " + install.getName(), e);
            throw e;
        }
    }

    /**
     *  Description of the Method
     */
    public void uninstall() {
        int probs = 0;
        for (Iterator i = listeners.iterator(); i.hasNext(); ) {
            Plugin plugin = (Plugin) i.next();
            if (plugin instanceof Install) {
                if (!uninstall((Install) plugin)) {
                    probs++;
                }
            }
        }
        if (logger instanceof Install) {
            if (!uninstall((Install) logger)) {
                probs++;
            }
        }
        if (userHome instanceof Install) {
            if (!uninstall((Install) userHome)) {
                probs++;
            }
        }
        if (groupHome instanceof Install) {
            if (!uninstall((Install) groupHome)) {
                probs++;
            }
        }
        if (policy instanceof Install) {
            if (!uninstall((Install) policy)) {
                probs++;
            }
        }
        log.info("Completed uninstall with " + probs + " error(s).");
    }

    /**
     *  Description of the Method
     *
     *@param  uninstall  Description of the Parameter
     *@return            Description of the Return Value
     */
    protected boolean uninstall(Install uninstall) {
        log.info("uninstall " + uninstall.getName());
        try {
            uninstall.uninstall();
        } catch (Exception e) {
            log.error("Problem uninstalling " + uninstall.getName(), e);
            return false;
        }
        return true;
    }

    /**
     *  The main program for the AmiWall class
     *
     *@param  args           The command line arguments
     *@exception  Exception  Description of the Exception
     */
    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("v", "version", false, "display version");
        options.addOption("c", "config", true, "config file");
        options.addOption("p", "police", false, "police policy file now");
        options.addOption("i", "install", false, "install AmiWall tables into database");
        options.addOption("u", "uninstall", false, "uninstall AmiWall tables from database");
        CommandLine line = parser.parse(options, args);
        if (line.hasOption("h")) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("java org.amiwall.AmiWall", options);
        } else if (line.hasOption("v")) {
            System.out.println(VERSION);
        } else {
            AmiWall amiwall = new AmiWall();
            if (line.hasOption("c")) {
                amiwall.setConfigFile(new File(line.getOptionValue("c")));
            } else {
                amiwall.setConfigFile(new File(CONF, "amiwall.xml"));
            }
            amiwall.config();
            if (line.hasOption("i")) {
                amiwall.install();
                System.exit(0);
            } else if (line.hasOption("u")) {
                amiwall.uninstall();
                System.exit(0);
            } else {
                amiwall.activate();
                if (line.hasOption("p")) {
                    amiwall.policy.policeNow();
                }
            }
        }
    }
}
