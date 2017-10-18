package jeeobserver;

import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import jeeobserver.logger.LoggerFormatter;

/**
 * The Class JeeObserverServerContext.
 *
 * <p>
 * Main Class of jeeObserver server. <br/>
 * Use <code>createInstance</code> method to start listening for requests and
 * <code>close</close> to stop listening.
 * </p>
 *
 * @author Luca Mingardi
 * @version 3.1
 */
public class JeeObserverServerContext {

    /** The Constant VERSION. */
    public static final int[] VERSION = { 3, 1, 1 };

    /** The Constant DATABASE_HANDLER_PARAMETER. */
    public static final String DATABASE_HANDLER_PARAMETER = "JEEOBSERVER_SERVER_DATABASE_HANDLER";

    /** The Constant DATABASE_DRIVER_PARAMETER. */
    public static final String DATABASE_DRIVER_PARAMETER = "JEEOBSERVER_SERVER_DATABASE_DRIVER";

    /** The Constant DATABASE_URL_PARAMETER. */
    public static final String DATABASE_URL_PARAMETER = "JEEOBSERVER_SERVER_DATABASE_URL";

    /** The Constant DATABASE_USER_PARAMETER. */
    public static final String DATABASE_USER_PARAMETER = "JEEOBSERVER_SERVER_DATABASE_USER";

    /** The Constant DATABASE_PASSWORD_PARAMETER. */
    public static final String DATABASE_PASSWORD_PARAMETER = "JEEOBSERVER_SERVER_DATABASE_PASSWORD";

    /** The Constant DATABASE_SCHEMA_PARAMETER. */
    public static final String DATABASE_SCHEMA_PARAMETER = "JEEOBSERVER_SERVER_DATABASE_SCHEMA";

    /** The Constant DATABASE_CONNECTION_POOL_SIZE_PARAMETER. */
    public static final String DATABASE_CONNECTION_POOL_SIZE_PARAMETER = "JEEOBSERVER_SERVER_DATABASE_CONNECTION_POOL_SIZE";

    /** The Constant SERVER_PORT_PARAMETER. */
    public static final String SERVER_PORT_PARAMETER = "JEEOBSERVER_SERVER_SERVER_PORT";

    /** The Constant LOGGER_LEVEL_PARAMETER. */
    public static final String LOGGER_LEVEL_PARAMETER = "JEEOBSERVER_SERVER_LOGGER_LEVEL";

    /** The Constant NOTIFICATOR_TASK_NAME. */
    public static final String NOTIFICATOR_TASK_NAME = "jeeobserverNotificatorTask";

    /** The Constant DATABASE_HANDLER_TASK_NAME. */
    public static final String DATABASE_HANDLER_TASK_NAME = "jeeobserverDatabaseHandlerTask";

    /** The Constant DEFAULT_DATABASE_HANDLER. */
    public static final String DEFAULT_DATABASE_HANDLER = "jeeobserver.DerbyDatabaseHandler";

    /** The Constant DEFAULT_DATABASE_DRIVER. */
    public static final String DEFAULT_DATABASE_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    /** The Constant DEFAULT_DATABASE_SCHEMA. */
    public static final String DEFAULT_DATABASE_SCHEMA = "JEEOBSERVER_" + JeeObserverServerContext.VERSION[0] + "_" + JeeObserverServerContext.VERSION[1];

    /** The Constant DEFAULT_DATABASE_URL. */
    public static final String DEFAULT_DATABASE_URL = "jdbc:derby:./jeeobserver_db/" + JeeObserverServerContext.DEFAULT_DATABASE_SCHEMA;

    /** The Constant DEFAULT_DATABASE_USER. */
    public static final String DEFAULT_DATABASE_USER = "SA";

    /** The Constant DEFAULT_DATABASE_PASSWORD. */
    public static final String DEFAULT_DATABASE_PASSWORD = "";

    /** The Constant DEFAULT_SERVER_PORT. */
    public static final int DEFAULT_SERVER_PORT = 5688;

    /** The Constant DEFAULT_LOGGER_LEVEL. */
    public static final Level DEFAULT_LOGGER_LEVEL = Level.INFO;

    /** The Constant DEFAULT_DATABASE_CONNECTION_POOL_SIZE. */
    public static final int DEFAULT_DATABASE_CONNECTION_POOL_SIZE = 10;

    /** The instance. */
    private static JeeObserverServerContext instance;

    /** The session id. */
    private String sessionId;

    /** The start timestamp. */
    private Date startTimestamp;

    /** The ip. */
    private String ip;

    /** The operating system name. */
    private final String operatingSystemName;

    /** The operating system version. */
    private final String operatingSystemVersion;

    /** The operating system architecture. */
    private final String operatingSystemArchitecture;

    /** The java version. */
    private final String javaVersion;

    /** The java vendor. */
    private final String javaVendor;

    /** The notifications sent. */
    private long notificationsSent = 0;

    /** The enabled. */
    private boolean enabled = false;

    /** The database handler timer. */
    private final Timer databaseHandlerTimer;

    /** The database handler. */
    private DatabaseHandler databaseHandler;

    /** The server. */
    private final JeeObserverServer server;

    /** The properties. */
    private final JeeObserverServerContextProperties properties;

    /** The Constant logger. */
    public static final Logger logger = JeeObserverServerContext.createLogger(JeeObserverServerContext.DEFAULT_LOGGER_LEVEL);

    /**
     * Instantiates a new jee observer server context.
     *
     * @param properties the properties
     * @throws DatabaseException the database exception
     * @throws ServerException the server exception
     */
    private JeeObserverServerContext(JeeObserverServerContextProperties properties) throws DatabaseException, ServerException {
        super();
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(("JE" + System.currentTimeMillis()).getBytes());
            final BigInteger hash = new BigInteger(1, md5.digest());
            this.sessionId = hash.toString(16).toUpperCase();
        } catch (final Exception e) {
            this.sessionId = "JE" + System.currentTimeMillis();
            JeeObserverServerContext.logger.log(Level.WARNING, "JeeObserver Server session ID MD5 error: {0}", this.sessionId);
            JeeObserverServerContext.logger.log(Level.FINEST, e.getMessage(), e);
        }
        try {
            @SuppressWarnings("unchecked") final Class<DatabaseHandler> databaseHandlerClass = (Class<DatabaseHandler>) Class.forName(properties.getDatabaseHandler());
            final Constructor<DatabaseHandler> handlerConstructor = databaseHandlerClass.getConstructor(new Class<?>[] { String.class, String.class, String.class, String.class, String.class, Integer.class });
            this.databaseHandler = handlerConstructor.newInstance(new Object[] { properties.getDatabaseDriver(), properties.getDatabaseUrl(), properties.getDatabaseUser(), properties.getDatabasePassword(), properties.getDatabaseSchema(), new Integer(properties.getDatabaseConnectionPoolSize()) });
        } catch (final Exception e) {
            throw new ServerException("Database handler loading exception.", e);
        }
        this.databaseHandlerTimer = new Timer(JeeObserverServerContext.DATABASE_HANDLER_TASK_NAME, true);
        this.server = new JeeObserverServer(properties.getServerPort());
        this.enabled = true;
        this.properties = properties;
        this.startTimestamp = new Date();
        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
        } catch (final UnknownHostException e) {
            JeeObserverServerContext.logger.log(Level.SEVERE, e.getMessage(), e);
        }
        this.operatingSystemName = System.getProperty("os.name");
        this.operatingSystemVersion = System.getProperty("os.version");
        this.operatingSystemArchitecture = System.getProperty("os.arch");
        this.javaVersion = System.getProperty("java.version");
        this.javaVendor = System.getProperty("java.vendor");
    }

    /**
     * The main method.
     *
     * @param arguments the arguments
     * @throws DatabaseException the database exception
     * @throws ServerException the server exception
     */
    public static void main(String[] arguments) throws DatabaseException, ServerException {
        JeeObserverServerContext.logger.log(Level.INFO, "JeeObserver Server v {0}.{1}.{2}", new String[] { String.valueOf(JeeObserverServerContext.VERSION[0]), String.valueOf(JeeObserverServerContext.VERSION[1]), String.valueOf(JeeObserverServerContext.VERSION[2]) });
        if ((arguments.length > 0) && (arguments[0].equals("-h") || arguments[0].equals("--help"))) {
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "Usage: JeeObserverServerContext [OPTIONS]...");
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "Start jeeObserver server on the specified port and using database located in the specified path.");
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "List of available options:");
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "   -dh, --dbhandler:   Database handler class.");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Default value: " + JeeObserverServerContext.DEFAULT_DATABASE_HANDLER);
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "   -dd, --dbdriver:    JDBC Driver class of database.");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Default value: " + JeeObserverServerContext.DEFAULT_DATABASE_DRIVER);
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "   -dr, --dburl:       Database url. Using embedded database like Derby you can specify the directory where save data.");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Default value: {0}", JeeObserverServerContext.DEFAULT_DATABASE_URL);
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "   -du, --dbuser:      Database user.");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Default value: " + JeeObserverServerContext.DEFAULT_DATABASE_USER);
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "   -dp, --dbpassword:  Database password.");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Default value: (?)");
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "   -ds, --dbschema:    Database schema.");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Default value: {0}", JeeObserverServerContext.DEFAULT_DATABASE_SCHEMA);
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "   -dc, --dbpoolsize:  Database connection pool maximum size.");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Default value: {0}", String.valueOf(JeeObserverServerContext.DEFAULT_DATABASE_CONNECTION_POOL_SIZE));
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "   -p, --port:         Listening port of server.");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Default value: {0}", String.valueOf(JeeObserverServerContext.DEFAULT_SERVER_PORT));
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "   -l, --loggerlevel:  Level of logger information displayed.");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Possible values: OFF | SEVERE | WARNING | INFO | FINE | FINER | FINEST | ALL");
            JeeObserverServerContext.logger.log(Level.INFO, "                       Default value: {0}", JeeObserverServerContext.DEFAULT_LOGGER_LEVEL.getName());
            JeeObserverServerContext.logger.log(Level.INFO, "");
            JeeObserverServerContext.logger.log(Level.INFO, "Visit http:\\\\www.jeeobserver.com\\");
            JeeObserverServerContext.logger.log(Level.INFO, "");
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
            JeeObserverServerContext.logger.log(Level.INFO, "Copyright 2009 - {0} Luca Mingardi.", simpleDateFormat.format(new Date()));
            JeeObserverServerContext.logger.log(Level.INFO, "");
        } else {
            JeeObserverServerContext.createInstance(arguments);
        }
    }

    /**
     * Gets the single instance of JeeObserverServerContext.
     *
     * @return single instance of JeeObserverServerContext
     */
    public static JeeObserverServerContext getInstance() {
        if (JeeObserverServerContext.instance == null) {
            JeeObserverServerContext.logger.log(Level.WARNING, "JeeObserver server context not yet created.");
        }
        return JeeObserverServerContext.instance;
    }

    /**
     * Calculate properties.
     *
     * @param servletContext the servlet context
     * @param arguments the arguments
     * @return the jee observer server context properties
     */
    private static JeeObserverServerContextProperties calculateProperties(ServletContext servletContext, String[] arguments) {
        final Map<String, String> parameters = new HashMap<String, String>();
        if (System.getenv().containsKey(JeeObserverServerContext.SERVER_PORT_PARAMETER)) {
            parameters.put(JeeObserverServerContext.SERVER_PORT_PARAMETER, System.getenv().get(JeeObserverServerContext.SERVER_PORT_PARAMETER));
        }
        if (System.getenv().containsKey(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER)) {
            parameters.put(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER, System.getenv().get(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER));
        }
        if (System.getenv().containsKey(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER)) {
            parameters.put(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER, System.getenv().get(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER));
        }
        if (System.getenv().containsKey(JeeObserverServerContext.DATABASE_URL_PARAMETER)) {
            parameters.put(JeeObserverServerContext.DATABASE_URL_PARAMETER, System.getenv().get(JeeObserverServerContext.DATABASE_URL_PARAMETER));
        }
        if (System.getenv().containsKey(JeeObserverServerContext.DATABASE_USER_PARAMETER)) {
            parameters.put(JeeObserverServerContext.DATABASE_USER_PARAMETER, System.getenv().get(JeeObserverServerContext.DATABASE_USER_PARAMETER));
        }
        if (System.getenv().containsKey(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER)) {
            parameters.put(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER, System.getenv().get(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER));
        }
        if (System.getenv().containsKey(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER)) {
            parameters.put(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER, System.getenv().get(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER));
        }
        if (System.getenv().containsKey(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER)) {
            parameters.put(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER, System.getenv().get(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER));
        }
        if (System.getenv().containsKey(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER)) {
            parameters.put(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER, System.getenv().get(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER));
        }
        ResourceBundle resourceBundle = null;
        try {
            resourceBundle = ResourceBundle.getBundle("jeeobserver-server");
            Set<String> keysSet = new HashSet<String>();
            Enumeration<String> keys = resourceBundle.getKeys();
            while (keys.hasMoreElements()) {
                keysSet.add(keys.nextElement());
            }
            if (keysSet.contains("server.port")) {
                parameters.put(JeeObserverServerContext.SERVER_PORT_PARAMETER, resourceBundle.getString("server.port"));
            }
            if (keysSet.contains("server.database.handler")) {
                parameters.put(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER, resourceBundle.getString("server.database.handler"));
            }
            if (keysSet.contains("server.database.driver")) {
                parameters.put(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER, resourceBundle.getString("server.database.driver"));
            }
            if (keysSet.contains("server.database.url")) {
                parameters.put(JeeObserverServerContext.DATABASE_URL_PARAMETER, resourceBundle.getString("server.database.url"));
            }
            if (keysSet.contains("server.database.user")) {
                parameters.put(JeeObserverServerContext.DATABASE_USER_PARAMETER, resourceBundle.getString("server.database.user"));
            }
            if (keysSet.contains("server.database.password")) {
                parameters.put(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER, resourceBundle.getString("server.database.password"));
            }
            if (keysSet.contains("server.database.schema")) {
                parameters.put(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER, resourceBundle.getString("server.database.schema"));
            }
            if (keysSet.contains("server.database.connectionPoolSize")) {
                parameters.put(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER, resourceBundle.getString("server.database.connectionPoolSize"));
            }
            if (keysSet.contains("server.logger.level")) {
                parameters.put(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER, resourceBundle.getString("server.logger.level"));
            }
            if (keysSet.contains(JeeObserverServerContext.SERVER_PORT_PARAMETER)) {
                parameters.put(JeeObserverServerContext.SERVER_PORT_PARAMETER, resourceBundle.getString(JeeObserverServerContext.SERVER_PORT_PARAMETER));
            }
            if (keysSet.contains(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER)) {
                parameters.put(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER, resourceBundle.getString(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER));
            }
            if (keysSet.contains(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER)) {
                parameters.put(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER, resourceBundle.getString(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER));
            }
            if (keysSet.contains(JeeObserverServerContext.DATABASE_URL_PARAMETER)) {
                parameters.put(JeeObserverServerContext.DATABASE_URL_PARAMETER, resourceBundle.getString(JeeObserverServerContext.DATABASE_URL_PARAMETER));
            }
            if (keysSet.contains(JeeObserverServerContext.DATABASE_USER_PARAMETER)) {
                parameters.put(JeeObserverServerContext.DATABASE_USER_PARAMETER, resourceBundle.getString(JeeObserverServerContext.DATABASE_USER_PARAMETER));
            }
            if (keysSet.contains(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER)) {
                parameters.put(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER, resourceBundle.getString(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER));
            }
            if (keysSet.contains(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER)) {
                parameters.put(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER, resourceBundle.getString(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER));
            }
            if (keysSet.contains(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER)) {
                parameters.put(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER, resourceBundle.getString(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER));
            }
            if (keysSet.contains(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER)) {
                parameters.put(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER, resourceBundle.getString(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER));
            }
        } catch (final MissingResourceException e) {
            JeeObserverServerContext.logger.log(Level.INFO, "Properties file \"jeeobserver-server.properties\" not found.");
            JeeObserverServerContext.logger.log(Level.FINEST, e.getMessage(), e);
        }
        if (servletContext != null) {
            if (servletContext.getInitParameter(JeeObserverServerContext.SERVER_PORT_PARAMETER) != null) {
                parameters.put(JeeObserverServerContext.SERVER_PORT_PARAMETER, servletContext.getInitParameter(JeeObserverServerContext.SERVER_PORT_PARAMETER));
            }
            if (servletContext.getInitParameter(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER) != null) {
                parameters.put(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER, servletContext.getInitParameter(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER));
            }
            if (servletContext.getInitParameter(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER) != null) {
                parameters.put(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER, servletContext.getInitParameter(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER));
            }
            if (servletContext.getInitParameter(JeeObserverServerContext.DATABASE_URL_PARAMETER) != null) {
                parameters.put(JeeObserverServerContext.DATABASE_URL_PARAMETER, servletContext.getInitParameter(JeeObserverServerContext.DATABASE_URL_PARAMETER));
            }
            if (servletContext.getInitParameter(JeeObserverServerContext.DATABASE_USER_PARAMETER) != null) {
                parameters.put(JeeObserverServerContext.DATABASE_USER_PARAMETER, servletContext.getInitParameter(JeeObserverServerContext.DATABASE_USER_PARAMETER));
            }
            if (servletContext.getInitParameter(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER) != null) {
                parameters.put(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER, servletContext.getInitParameter(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER));
            }
            if (servletContext.getInitParameter(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER) != null) {
                parameters.put(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER, servletContext.getInitParameter(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER));
            }
            if (servletContext.getInitParameter(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER) != null) {
                parameters.put(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER, servletContext.getInitParameter(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER));
            }
            if (servletContext.getInitParameter(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER) != null) {
                parameters.put(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER, servletContext.getInitParameter(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER));
            }
        }
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                if ((arguments[i] != null) && !arguments[i].trim().equals("")) {
                    if (arguments[i].equals("-p") || arguments[i].equals("--port")) {
                        i = i + 1;
                        parameters.put(JeeObserverServerContext.SERVER_PORT_PARAMETER, arguments[i]);
                    } else if (arguments[i].equals("-l") || arguments[i].equals("--loggerlevel")) {
                        i = i + 1;
                        parameters.put(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER, arguments[i]);
                    } else if (arguments[i].equals("-dd") || arguments[i].equals("--dbdriver")) {
                        i = i + 1;
                        parameters.put(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER, arguments[i]);
                    } else if (arguments[i].equals("-dr") || arguments[i].equals("--dburl")) {
                        i = i + 1;
                        parameters.put(JeeObserverServerContext.DATABASE_URL_PARAMETER, arguments[i]);
                    } else if (arguments[i].equals("-du") || arguments[i].equals("--dbuser")) {
                        i = i + 1;
                        parameters.put(JeeObserverServerContext.DATABASE_USER_PARAMETER, arguments[i]);
                    } else if (arguments[i].equals("-dp") || arguments[i].equals("--dbpassword")) {
                        i = i + 1;
                        parameters.put(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER, arguments[i]);
                    } else if (arguments[i].equals("-dh") || arguments[i].equals("--dbhandler")) {
                        i = i + 1;
                        parameters.put(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER, arguments[i]);
                    } else if (arguments[i].equals("-ds") || arguments[i].equals("--dbschema")) {
                        i = i + 1;
                        parameters.put(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER, arguments[i]);
                    } else if (arguments[i].equals("-dc") || arguments[i].equals("--dbpoolsize")) {
                        i = i + 1;
                        parameters.put(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER, arguments[i]);
                    }
                }
            }
        }
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            entry.setValue(entry.getValue().trim());
        }
        final JeeObserverServerContextProperties properties = new JeeObserverServerContextProperties();
        if (parameters.containsKey(JeeObserverServerContext.SERVER_PORT_PARAMETER)) {
            try {
                properties.setServerPort(Integer.parseInt(parameters.get(JeeObserverServerContext.SERVER_PORT_PARAMETER)));
            } catch (final NumberFormatException e) {
                JeeObserverServerContext.logger.log(Level.SEVERE, "Parameter " + JeeObserverServerContext.SERVER_PORT_PARAMETER + " = {0} is not a number.", parameters.get(JeeObserverServerContext.SERVER_PORT_PARAMETER));
                JeeObserverServerContext.logger.log(Level.FINEST, e.getMessage(), e);
            }
        } else {
            JeeObserverServerContext.logger.log(Level.FINE, "Parameter " + JeeObserverServerContext.SERVER_PORT_PARAMETER + " not found.");
        }
        if (parameters.containsKey(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER)) {
            properties.setDatabaseHandler(parameters.get(JeeObserverServerContext.DATABASE_HANDLER_PARAMETER));
        } else {
            JeeObserverServerContext.logger.log(Level.FINE, "Parameter " + JeeObserverServerContext.DATABASE_HANDLER_PARAMETER + " not found.");
        }
        if (parameters.containsKey(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER)) {
            properties.setDatabaseDriver(parameters.get(JeeObserverServerContext.DATABASE_DRIVER_PARAMETER));
        } else {
            JeeObserverServerContext.logger.log(Level.FINE, "Parameter " + JeeObserverServerContext.DATABASE_DRIVER_PARAMETER + " not found.");
        }
        if (parameters.containsKey(JeeObserverServerContext.DATABASE_URL_PARAMETER)) {
            properties.setDatabaseUrl(parameters.get(JeeObserverServerContext.DATABASE_URL_PARAMETER));
        } else {
            JeeObserverServerContext.logger.log(Level.FINE, "Parameter " + JeeObserverServerContext.DATABASE_URL_PARAMETER + " not found.");
        }
        if (parameters.containsKey(JeeObserverServerContext.DATABASE_USER_PARAMETER)) {
            properties.setDatabaseUser(parameters.get(JeeObserverServerContext.DATABASE_USER_PARAMETER));
        } else {
            JeeObserverServerContext.logger.log(Level.FINE, "Parameter " + JeeObserverServerContext.DATABASE_USER_PARAMETER + " not found.");
        }
        if (parameters.containsKey(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER)) {
            properties.setDatabasePassword(parameters.get(JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER));
        } else {
            JeeObserverServerContext.logger.log(Level.FINE, "Parameter " + JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER + " not found.");
        }
        if (parameters.containsKey(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER)) {
            properties.setDatabaseSchema(parameters.get(JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER));
        } else {
            JeeObserverServerContext.logger.log(Level.FINE, "Parameter " + JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER + " not found.");
        }
        if (parameters.containsKey(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER)) {
            try {
                properties.setDatabaseConnectionPoolSize(Integer.parseInt(parameters.get(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER)));
            } catch (final NumberFormatException e) {
                JeeObserverServerContext.logger.log(Level.SEVERE, "Parameter " + JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER + " = {0} is not a number.", parameters.get(JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER));
            }
        } else {
            JeeObserverServerContext.logger.log(Level.FINE, "Parameter " + JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER + " not found.");
        }
        if (parameters.containsKey(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER)) {
            try {
                properties.setLoggerLevel(Level.parse(parameters.get(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER)));
            } catch (final Exception e) {
                JeeObserverServerContext.logger.log(Level.SEVERE, "Parameter " + JeeObserverServerContext.LOGGER_LEVEL_PARAMETER + " = {0} is not a valid Level. (Available values: OFF | SEVERE | WARNING | INFO | FINE | FINER | FINEST | ALL)", parameters.get(JeeObserverServerContext.LOGGER_LEVEL_PARAMETER));
                JeeObserverServerContext.logger.log(Level.FINEST, e.getMessage(), e);
            }
        } else {
            JeeObserverServerContext.logger.log(Level.FINE, "Parameter " + JeeObserverServerContext.LOGGER_LEVEL_PARAMETER + " not found.");
        }
        return properties;
    }

    /**
     * Creates the instance.
     *
     * @param properties the properties
     * @return the jee observer server context
     * @throws DatabaseException the database exception
     * @throws ServerException the server exception
     */
    public static JeeObserverServerContext createInstance(JeeObserverServerContextProperties properties) throws DatabaseException, ServerException {
        JeeObserverServerContext.logger.setLevel(properties.getLoggerLevel());
        final JeeObserverServerContext newInstance = new JeeObserverServerContext(properties);
        newInstance.getDatabaseHandlerTimer().schedule(new DatabaseHandlerTimerTask(), DatabaseHandlerTimerTask.TIMER_INTERVAL, DatabaseHandlerTimerTask.TIMER_INTERVAL);
        newInstance.getServer().start();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    if (JeeObserverServerContext.getInstance() != null) {
                        JeeObserverServerContext.getInstance().close();
                    }
                } catch (final DatabaseException e) {
                    JeeObserverServerContext.logger.log(Level.SEVERE, e.getMessage(), e);
                } catch (final ServerException e) {
                    JeeObserverServerContext.logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        });
        JeeObserverServerContext.logger.log(Level.INFO, "JeeObserver server context instance created.");
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server operating system: {0} {1} - {2}", new Object[] { newInstance.getOperatingSystemName(), newInstance.getOperatingSystemVersion(), newInstance.getOperatingSystemArchitecture() });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server java: {0} {1}", new Object[] { newInstance.getJavaVendor(), newInstance.getJavaVersion() });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server parameter: {0} = {1}", new Object[] { JeeObserverServerContext.LOGGER_LEVEL_PARAMETER, properties.getLoggerLevel().getName() });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server parameter: {0} = {1}", new Object[] { JeeObserverServerContext.SERVER_PORT_PARAMETER, String.valueOf(properties.getServerPort()) });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server parameter: {0} = {1}", new Object[] { JeeObserverServerContext.DATABASE_HANDLER_PARAMETER, properties.getLoggerLevel().getName() });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server parameter: {0} = {1}", new Object[] { JeeObserverServerContext.DATABASE_DRIVER_PARAMETER, properties.getDatabaseDriver() });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server parameter: {0} = {1}", new Object[] { JeeObserverServerContext.DATABASE_URL_PARAMETER, properties.getDatabaseUrl() });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server parameter: {0} = {1}", new Object[] { JeeObserverServerContext.DATABASE_USER_PARAMETER, properties.getDatabaseUser() });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server parameter: {0} = {1}", new Object[] { JeeObserverServerContext.DATABASE_PASSWORD_PARAMETER, properties.getDatabasePassword() });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server parameter: {0} = {1}", new Object[] { JeeObserverServerContext.DATABASE_SCHEMA_PARAMETER, properties.getDatabaseSchema() });
        JeeObserverServerContext.logger.log(Level.FINE, "JeeObserver server parameter: {0} = {1}", new Object[] { JeeObserverServerContext.DATABASE_CONNECTION_POOL_SIZE_PARAMETER, String.valueOf(properties.getDatabaseConnectionPoolSize()) });
        JeeObserverServerContext.instance = newInstance;
        return newInstance;
    }

    /**
     * Creates the instance.
     *
     * @return the jee observer server context
     * @throws DatabaseException the database exception
     * @throws ServerException the server exception
     */
    public static JeeObserverServerContext createInstance() throws DatabaseException, ServerException {
        return JeeObserverServerContext.createInstance(JeeObserverServerContext.calculateProperties(null, null));
    }

    /**
     * Creates the instance.
     *
     * @param servletContext the servlet context
     * @return the jee observer server context
     * @throws DatabaseException the database exception
     * @throws ServerException the server exception
     */
    public static JeeObserverServerContext createInstance(ServletContext servletContext) throws DatabaseException, ServerException {
        return JeeObserverServerContext.createInstance(JeeObserverServerContext.calculateProperties(servletContext, null));
    }

    /**
     * Creates the instance.
     *
     * @param arguments the arguments
     * @return the jee observer server context
     * @throws DatabaseException the database exception
     * @throws ServerException the server exception
     */
    public static JeeObserverServerContext createInstance(String[] arguments) throws DatabaseException, ServerException {
        return JeeObserverServerContext.createInstance(JeeObserverServerContext.calculateProperties(null, arguments));
    }

    /**
     * Close.
     *
     * @throws DatabaseException the database exception
     * @throws ServerException the server exception
     */
    public void close() throws DatabaseException, ServerException {
        if (this.enabled == true) {
            this.enabled = false;
            this.databaseHandlerTimer.cancel();
            this.server.setEnabled(false);
            this.server.close();
            this.getDatabaseHandler().stopDatabase();
            this.startTimestamp = null;
            JeeObserverServerContext.logger.log(Level.INFO, "JeeObserver server context instance destroyed.");
        }
    }

    /**
     * Creates the logger.
     *
     * @param loggerLevel the logger level
     * @return the logger
     */
    private static Logger createLogger(Level loggerLevel) {
        final Logger newLogger = Logger.getLogger("jeeobserver_server");
        newLogger.setUseParentHandlers(false);
        newLogger.setLevel(loggerLevel);
        final Handler handlerArray[] = newLogger.getHandlers();
        for (final Handler element : handlerArray) {
            newLogger.removeHandler(element);
        }
        final Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new LoggerFormatter());
        newLogger.addHandler(handler);
        return newLogger;
    }

    /**
     * Checks if is enabled.
     *
     * @return true, if is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Increment notification.
     *
     * @return the long
     */
    long incrementNotification() {
        this.notificationsSent = this.notificationsSent + 1;
        return this.notificationsSent;
    }

    /**
     * Gets the active requests.
     *
     * @return the active requests
     */
    public long getActiveRequests() {
        return this.server.getActiveRequests();
    }

    /**
     * Gets the total requests.
     *
     * @return the total requests
     */
    public long getTotalRequests() {
        return this.server.getTotalRequests();
    }

    /**
     * Gets the executed requests.
     *
     * @return the executed requests
     */
    public long getExecutedRequests() {
        return this.server.getTotalRequests() - this.server.getActiveRequests();
    }

    /**
     * Gets the total database requests.
     *
     * @return the total database requests
     */
    public long getTotalDatabaseRequests() {
        return this.getDatabaseHandler().getTotalInserts();
    }

    /**
     * Gets the database pool size.
     *
     * @return the database pool size
     */
    public long getDatabasePoolSize() {
        return this.getDatabaseHandler().getPoolSize();
    }

    /**
     * Gets the database handler name.
     *
     * @return the database handler name
     */
    public String getDatabaseHandlerName() {
        return this.getDatabaseHandler().getHandler();
    }

    /**
     * Gets the database url.
     *
     * @return the database url
     */
    public String getDatabaseUrl() {
        return this.getDatabaseHandler().getUrl();
    }

    /**
     * Gets the database driver.
     *
     * @return the database driver
     */
    public String getDatabaseDriver() {
        return this.getDatabaseHandler().getDriver();
    }

    /**
     * Gets the notifications sent.
     *
     * @return the notifications sent
     */
    public long getNotificationsSent() {
        return this.notificationsSent;
    }

    /**
     * Gets the start timestamp.
     *
     * @return the start timestamp
     */
    public Date getStartTimestamp() {
        return this.startTimestamp;
    }

    /**
     * Gets the java vendor.
     *
     * @return the java vendor
     */
    public String getJavaVendor() {
        return this.javaVendor;
    }

    /**
     * Gets the session id.
     *
     * @return the session id
     */
    public String getSessionId() {
        return this.sessionId;
    }

    /**
     * Gets the java version.
     *
     * @return the java version
     */
    public String getJavaVersion() {
        return this.javaVersion;
    }

    /**
     * Gets the operating system name.
     *
     * @return the operating system name
     */
    public String getOperatingSystemName() {
        return this.operatingSystemName;
    }

    /**
     * Gets the operating system architecture.
     *
     * @return the operating system architecture
     */
    public String getOperatingSystemArchitecture() {
        return this.operatingSystemArchitecture;
    }

    /**
     * Gets the operating system version.
     *
     * @return the operating system version
     */
    public String getOperatingSystemVersion() {
        return this.operatingSystemVersion;
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public JeeObserverServerContextProperties getProperties() {
        return this.properties;
    }

    /**
     * Gets the ip.
     *
     * @return the ip
     */
    public String getIp() {
        return this.ip;
    }

    /**
     * Gets the database handler.
     *
     * @return the database handler
     */
    public DatabaseHandler getDatabaseHandler() {
        return this.databaseHandler;
    }

    /**
     * Gets the database handler timer.
     *
     * @return the database handler timer
     */
    private Timer getDatabaseHandlerTimer() {
        return this.databaseHandlerTimer;
    }

    /**
     * Gets the server.
     *
     * @return the server
     */
    private JeeObserverServer getServer() {
        return this.server;
    }
}
