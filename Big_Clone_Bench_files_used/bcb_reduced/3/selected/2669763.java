package de.haumacher.timecollect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.http.security.Credential;
import org.eclipse.jetty.http.security.Credential.MD5;
import org.eclipse.jetty.http.security.Password;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import de.haumacher.timecollect.LayoutConfig.Bounds;
import de.haumacher.timecollect.Ticket.LCState;
import de.haumacher.timecollect.UIConfig.Report;
import de.haumacher.timecollect.common.config.PropertiesUtil;
import de.haumacher.timecollect.common.config.ValueFactory;
import de.haumacher.timecollect.common.db.DBException;
import de.haumacher.timecollect.common.db.DataSourceFactory;
import de.haumacher.timecollect.common.report.HtmlReportBuilder;
import de.haumacher.timecollect.db.ActivityTable;
import de.haumacher.timecollect.db.ClientContext;
import de.haumacher.timecollect.db.ClientDB;
import de.haumacher.timecollect.db.ClientTransaction;
import de.haumacher.timecollect.db.DbConfig;
import de.haumacher.timecollect.db.LocalTicketResolver;
import de.haumacher.timecollect.db.UptimeDetector;
import de.haumacher.timecollect.db.UptimeHandler;
import de.haumacher.timecollect.db.derby.EmbeddedDerbyDatasourceFactory;
import de.haumacher.timecollect.db.internal.DBImpl;
import de.haumacher.timecollect.db.mysql.MySQLDatasourceFactory;
import de.haumacher.timecollect.plugin.Configurable;
import de.haumacher.timecollect.plugin.Plugin;
import de.haumacher.timecollect.remote.http.RemoteContol;
import de.haumacher.timecollect.remote.http.RemoteContol.Callback;
import de.haumacher.timecollect.report.ReportDef;
import de.haumacher.timecollect.report.ReportGenerator;
import de.haumacher.timecollect.ui.AbstractTabProvider;
import de.haumacher.timecollect.ui.MenuProvider;
import de.haumacher.timecollect.ui.Style;
import de.haumacher.timecollect.ui.TabProvider;
import de.haumacher.timecollect.ui.http.PingServlet;
import de.haumacher.timecollect.ui.http.SwitchActivityServlet;
import de.haumacher.timecollect.ui.internal.DefaultStyle;

public class Main implements Application, Callback, Configurable, ActivityProvider, ActivityController {

    private final class UptimeReceiver implements UptimeHandler {

        public void notifyUptime(final Ticket currentTicket, final long todayStart, final long thisDay, final long thisWeek, final long thisMonth) {
            if (item.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {

                public void run() {
                    if (item.isDisposed()) {
                        return;
                    }
                    String info = createToolTip(todayStart, thisDay, thisWeek, thisMonth);
                    String prefix;
                    if (currentTicket == null) {
                        prefix = version.getAppName();
                    } else {
                        int remoteId = currentTicket.getRemoteId();
                        prefix = (remoteId > 0 ? "#" + remoteId + " " : "") + currentTicket.getSummary();
                    }
                    item.setToolTipText(prefix + " (" + info + ")");
                }
            });
        }

        public void notifyActive(final RawActivity currentActivity) {
            if (display == null || display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {

                public void run() {
                    if (currentActivity != null) {
                        int currentTicketId = currentActivity.getTicketId();
                        for (MenuItem item : menu.getItems()) {
                            Object itemData = item.getData();
                            if (itemData instanceof Ticket) {
                                if (((Ticket) itemData).getId() == currentTicketId) {
                                    item.setSelection(true);
                                } else {
                                    item.setSelection(false);
                                }
                            }
                        }
                        closeItem.setSelection(currentTicketId == 0);
                    }
                    boolean newActive = currentActivity != null;
                    if (active == newActive) {
                        return;
                    }
                    setActive(newActive);
                }
            });
        }
    }

    public class DialogConfigImpl implements DialogConfig {

        private final Bounds bounds;

        public DialogConfigImpl(Bounds bounds) {
            this.bounds = bounds;
        }

        @Override
        public Bounds getBounds() {
            return bounds;
        }

        @Override
        public void notifyUpdate() {
            putLayoutConfig(configuration);
            writeConfig(configuration);
        }
    }

    public final class OverviewHandler extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html");
            resp.setCharacterEncoding("utf-8");
            resp.getWriter().println("<html>" + "<head><title>TimeCollect " + version + "</title></head>" + "<body>" + "<h1>TimeCollect " + version + "</h1>" + "<ul>" + reports() + "</ul>" + "</body>" + "</html>");
        }

        private String reports() {
            List<Report> reports = ReportDef.getAllReports(dbConfig, ui);
            StringBuilder result = new StringBuilder();
            for (int n = 0, cnt = reports.size(); n < cnt; n++) {
                Report report = reports.get(n);
                result.append("<li><a href='/report/" + n + "'>" + encode(report.getName()) + "</a></li>");
            }
            return result.toString();
        }
    }

    public final class ReportHandler extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String uri = req.getPathInfo();
            int id = Integer.parseInt(uri.substring(1));
            final Report report = ReportDef.getAllReports(dbConfig, ui).get(id);
            StringWriter buffer = new StringWriter();
            final StringWriter error = new StringWriter();
            try {
                XMLStreamWriter out = XMLOutputFactory.newInstance().createXMLStreamWriter(buffer);
                MainContext context = new MainContext(Main.this) {

                    @Override
                    public void handleError(DBException ex) {
                        error.write("<div>");
                        error.write("<div>");
                        error.write("Database error executing query '" + report.getName() + "'.");
                        error.write("</div>");
                        error.write("<div>");
                        error.write(ex.getMessage());
                        error.write("</div>");
                        error.write("<pre>");
                        error.write(report.getSql());
                        error.write("</pre>");
                        error.write("</div>");
                    }

                    @Override
                    public void handleError(String title, String message, Throwable problem) {
                        error.write("<div>");
                        error.write("<div>");
                        error.write(title);
                        error.write("</div>");
                        error.write("<div>");
                        error.write(message);
                        error.write("</div>");
                        error.write("<pre>");
                        error.write(report.getSql());
                        error.write("</pre>");
                        error.write("</div>");
                    }
                };
                new ReportGenerator(context, new HtmlReportBuilder(out)).run(report.getSql());
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            } catch (FactoryConfigurationError e) {
                throw new RuntimeException(e);
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html");
            resp.setCharacterEncoding("utf-8");
            resp.getWriter().println("<html>" + "<head>" + "<title>Report " + encode(report.getName()) + "</title>" + "<style>" + "table {cell-spacing: 0; border-collapse: collapse; }" + "td {border-style: solid; border-width: 1px; padding: 3px; }" + "</style>" + "</head>" + "<body>" + "<h1>" + encode(report.getName()) + "</h1>" + "<p><a href='/'>TimeCollect " + version + "</a></p>" + (error.getBuffer().length() > 0 ? error.getBuffer().toString() : buffer.toString()) + "<p><a href='/'>TimeCollect " + version + "</a></p>" + "</body>" + "</html>");
        }
    }

    private static final String UI_CONFIG_PREFIX = "ui.";

    private static final String HTTP_CONFIG_PREFIX = "http.";

    private static final String DB_CONFIG_PREFIX = "db.";

    private static final String LAYOUT_CONFIG_PREFIX = "layout.";

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        new Main().run();
    }

    protected boolean active;

    private Image playImage;

    private Image pauseImage;

    protected TrayItem item;

    private Menu menu;

    private Shell shell;

    DBImpl db;

    private final Trac trac = new Trac();

    Display display;

    private MenuItem closeItem;

    private Server httpd;

    HttpConfig http = ValueFactory.newInstance(HttpConfig.class);

    UIConfig ui = ValueFactory.newInstance(UIConfig.class);

    LayoutConfig layout = ValueFactory.newInstance(LayoutConfig.class);

    DbConfig dbConfig = ValueFactory.newInstance(DbConfig.class);

    private final List<Plugin> plugins = new ArrayList<Plugin>();

    private Properties configuration = new Properties();

    private Version version;

    private boolean stopped;

    private final Callback asyncCallback = new Callback() {

        public boolean toggleActive() {
            final boolean[] result = { false };
            display.syncExec(new Runnable() {

                public void run() {
                    result[0] = Main.this.toggleActive();
                }
            });
            return result[0];
        }

        public void fetchTicket() {
            display.asyncExec(new Runnable() {

                public void run() {
                    Main.this.fetchTicket();
                }
            });
        }

        public void resyncTrac() {
            display.asyncExec(new Runnable() {

                public void run() {
                    Main.this.resyncTrac();
                }
            });
        }

        public void quit() {
            display.asyncExec(new Runnable() {

                public void run() {
                    Main.this.quit();
                }
            });
        }
    };

    private final UptimeHandler uptimeHandler = new UptimeReceiver();

    private final ClientDB dbWrapper = new ClientDB() {

        @Override
        public ClientContext context() throws DBException {
            return db.context();
        }

        @Override
        public ClientTransaction begin() throws DBException {
            return db.begin();
        }
    };

    public synchronized void addPlugin(final Plugin plugin) {
        if (display != null) {
            display.asyncExec(new Runnable() {

                public void run() {
                    internalAddPlugin(plugin);
                    try {
                        updateMenu();
                    } catch (DBException ex) {
                        LOG.log(Level.WARNING, "Error rebuilding menu.", ex);
                    }
                }
            });
        } else {
            internalAddPlugin(plugin);
        }
    }

    private void internalAddPlugin(final Plugin plugin) {
        plugin.startup(new MainContext(Main.this), dbWrapper, asyncCallback);
        Configurable configurable = plugin.getConfigurable();
        if (configurable != null) {
            configurable.loadConfig(configuration);
        }
        plugins.add(plugin);
    }

    public synchronized void removePlugin(final Plugin plugin) {
        if (display != null && !display.isDisposed()) {
            display.asyncExec(new Runnable() {

                public void run() {
                    internalRemovePlugin(plugin);
                    try {
                        updateMenu();
                    } catch (DBException ex) {
                        LOG.log(Level.WARNING, "Error rebuilding menu.", ex);
                    }
                }
            });
        } else {
            internalRemovePlugin(plugin);
        }
    }

    private void internalRemovePlugin(final Plugin plugin) {
        plugins.remove(plugin);
    }

    public void run() {
        if (!startup()) {
            return;
        }
        shell.setVisible(false);
        while (!shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) display.sleep();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Internal error.", ex);
            }
        }
        disposeUI();
        try {
            httpd.stop();
            httpd.join();
            httpd.destroy();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error stopping remote control service.", ex);
        }
    }

    private synchronized boolean startup() {
        if (stopped) {
            return false;
        }
        display = new Display();
        shell = new Shell(display);
        shell.setText("TimeCollect");
        version = new Version();
        loadConfig();
        restartDB();
        playImage = new Image(display, Main.class.getResourceAsStream("play-16.gif"));
        pauseImage = new Image(display, Main.class.getResourceAsStream("pause-16.gif"));
        final Tray tray = display.getSystemTray();
        if (tray == null) {
            LOG.severe("The system tray is not available.");
            disposeUI();
            return false;
        }
        item = new TrayItem(tray, SWT.NONE);
        item.setToolTipText(version.getAppName());
        item.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                toggleActive();
            }
        });
        item.addListener(SWT.MenuDetect, new Listener() {

            @Override
            public void handleEvent(Event event) {
                menu.setVisible(true);
            }
        });
        int activeTicket;
        Map<Integer, Ticket> activities;
        try {
            ClientContext tx = db.context();
            try {
                activeTicket = tx.loadActiveTicket();
                activities = tx.loadTickets(activeTicket);
            } finally {
                tx.close();
            }
        } catch (DBException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            activeTicket = 0;
            activities = Collections.emptyMap();
        }
        createMenu(activeTicket, activities.values());
        boolean wasActive;
        try {
            ClientTransaction tx = db.begin();
            try {
                long now = System.currentTimeMillis();
                RawActivity currentActivity = tx.updateActive(now);
                tx.commit();
                wasActive = currentActivity != null;
            } finally {
                tx.close();
            }
        } catch (DBException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            wasActive = false;
        }
        setActive(wasActive);
        if (wasActive) {
            startUptimeDetector();
        }
        startHttpd();
        return true;
    }

    private void startHttpd() {
        if (!http.getUseHttp()) {
            return;
        }
        int rcPort = http.getPort();
        String rcPassword = Long.toHexString(new SecureRandom().nextLong());
        LOG.info("Using password '" + rcPassword + "'");
        File home = new File(System.getProperty("user.home"));
        File passwordFile = new File(home, ".timecollect.passwd");
        File portFile = new File(home, ".timecollect.port");
        try {
            FileWriter out1 = new FileWriter(passwordFile);
            out1.write(rcPassword);
            out1.close();
            FileWriter out2 = new FileWriter(portFile);
            out2.write(Integer.toString(rcPort));
            out2.close();
        } catch (IOException ex) {
            handleError("Remote control service error.", "Could generate password file: " + passwordFile, ex);
        }
        try {
            httpd = new Server();
            SelectChannelConnector connector = new SelectChannelConnector();
            connector.setAcceptors(1);
            connector.setHost("127.0.0.1");
            connector.setPort(rcPort);
            connector.setName("rc");
            httpd.addConnector(connector);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            context.addServlet(new ServletHolder(new OverviewHandler()), "/");
            context.addServlet(new ServletHolder(new RemoteContol(asyncCallback)), "/ctrl/*");
            context.addServlet(new ServletHolder(new ReportHandler()), "/report/*");
            context.addServlet(new ServletHolder(new SwitchActivityServlet(trac, this, this)), "/switch");
            context.addServlet(new ServletHolder(new PingServlet()), "/ping");
            Constraint rcConstraint = new Constraint();
            rcConstraint.setName(Constraint.__BASIC_AUTH);
            ;
            rcConstraint.setRoles(new String[] { "rc" });
            rcConstraint.setAuthenticate(true);
            HashLoginService loginService = new HashLoginService("TimeCollect");
            loginService.putUser("rc", new Password(rcPassword), new String[] { "rc" });
            List<ConstraintMapping> mappings = new ArrayList<ConstraintMapping>();
            mappings.add(mkMapping(rcConstraint, "/ctrl/*"));
            String uiPasswordHash = http.getPasswordHash();
            if (uiPasswordHash.length() > 0) {
                Constraint uiConstraint = new Constraint();
                uiConstraint.setName(Constraint.__BASIC_AUTH);
                ;
                uiConstraint.setRoles(new String[] { "ui" });
                uiConstraint.setAuthenticate(true);
                mappings.add(mkMapping(uiConstraint, "/report/*"));
                mappings.add(mkMapping(uiConstraint, "/switch/*"));
                loginService.putUser(http.getUser(), Credential.getCredential(uiPasswordHash), new String[] { "ui" });
            }
            ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
            securityHandler.setRealmName("TimeCollect");
            securityHandler.setLoginService(loginService);
            securityHandler.setAuthMethod(Constraint.__BASIC_AUTH);
            securityHandler.setConstraintMappings(mappings);
            context.setHandler(securityHandler);
            httpd.setHandler(context);
            httpd.start();
        } catch (Exception ex) {
            handleError("Remote control service error.", "Could not start remote control server at port '" + rcPort + "'", ex);
        }
    }

    private void stopHttpd() {
        if (httpd == null) {
            return;
        }
        try {
            httpd.stop();
            httpd.join();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Stopping internal httpd failed.", ex);
        }
        httpd = null;
    }

    private ConstraintMapping mkMapping(Constraint constraint, String pathSpec) {
        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec(pathSpec);
        return constraintMapping;
    }

    private void disposeUI() {
        playImage.dispose();
        pauseImage.dispose();
        display.dispose();
    }

    public synchronized void stop() {
        if (display != null && !display.isDisposed()) {
            display.syncExec(new Runnable() {

                @Override
                public void run() {
                    shell.dispose();
                }
            });
        }
        stopped = true;
    }

    public static String encode(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String createToolTip(long todayStart, long thisDay, long thisWeek, long thisMonth) {
        String info;
        if (todayStart > 0) {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(todayStart);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            info = MessageFormat.format("{2} today since {0,number,00}:{1,number,00}, {3} this week, {4} this month", hour, minute, format(thisDay), format(thisWeek), format(thisMonth));
        } else {
            info = MessageFormat.format("{0} today, {1} this week, {2} this month", format(thisDay), format(thisWeek), format(thisMonth));
        }
        return info;
    }

    private static String format(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        millis -= seconds * 1000;
        seconds -= minutes * 60;
        minutes -= hours * 60;
        if (hours == 0) {
            return minutes + "min " + seconds + "s";
        } else {
            return hours + "h " + minutes + "min";
        }
    }

    private void loadConfig() {
        Properties properties = new Properties();
        File home = new File(System.getProperty("user.home"));
        if (home.isDirectory()) {
            File configFile = new File(home, ".timecollect");
            if (configFile.exists()) {
                LOG.info("Reading configuration: " + configFile);
                try {
                    properties.load(new FileInputStream(configFile));
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Could not load configuration.", e);
                    properties = new Properties();
                }
            }
        }
        applyConfig(properties);
        this.configuration = properties;
    }

    protected void saveConfig() {
        Properties properties = extractConfig();
        writeConfig(properties);
        this.configuration = properties;
    }

    private void writeConfig(Properties properties) {
        File home = new File(System.getProperty("user.home"));
        if (home.isDirectory()) {
            File configFile = new File(home, ".timecollect");
            LOG.info("Saving configuration: " + configFile);
            try {
                FileOutputStream out = new FileOutputStream(configFile);
                properties.store(out, "TimeCollect configuration");
                out.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Could not load configuration.", e);
            }
        } else {
            LOG.log(Level.WARNING, "User home is not a directory: " + home.getAbsolutePath());
        }
    }

    private void applyConfig(Properties properties) {
        this.loadConfig(properties);
        trac.loadConfig(properties);
        for (Plugin plugin : plugins) {
            Configurable configurable = plugin.getConfigurable();
            if (configurable != null) {
                configurable.loadConfig(properties);
            }
        }
    }

    private Properties extractConfig() {
        Properties properties = new Properties();
        trac.saveConfig(properties);
        this.saveConfig(properties);
        for (Plugin plugin : plugins) {
            Configurable configurable = plugin.getConfigurable();
            if (configurable != null) {
                configurable.saveConfig(properties);
            }
        }
        return properties;
    }

    public void loadConfig(Properties properties) {
        PropertiesUtil.load(properties, UI_CONFIG_PREFIX, ui);
        PropertiesUtil.load(properties, HTTP_CONFIG_PREFIX, http);
        PropertiesUtil.load(properties, LAYOUT_CONFIG_PREFIX, layout);
        PropertiesUtil.load(properties, DB_CONFIG_PREFIX, dbConfig);
    }

    private void restartDB() {
        if (db != null) {
            DBImpl oldDb = db;
            db = null;
            oldDb.shutdown();
        }
        db = new DBImpl(getDatasourceFactory(), dbConfig.getCollectGranularity(), uptimeHandler);
        try {
            db.setupDb();
        } catch (DBException e) {
            handleError(e);
        }
    }

    private DataSourceFactory getDatasourceFactory() {
        if (dbConfig.getMysql()) {
            return new MySQLDatasourceFactory(dbConfig);
        } else {
            return new EmbeddedDerbyDatasourceFactory(dbConfig);
        }
    }

    public void saveConfig(Properties properties) {
        PropertiesUtil.save(properties, UI_CONFIG_PREFIX, ui);
        PropertiesUtil.save(properties, HTTP_CONFIG_PREFIX, http);
        PropertiesUtil.save(properties, DB_CONFIG_PREFIX, this.dbConfig);
        putLayoutConfig(properties);
    }

    private void putLayoutConfig(Properties properties) {
        PropertiesUtil.save(properties, LAYOUT_CONFIG_PREFIX, layout);
    }

    private void createMenu(int activeTicket, Collection<Ticket> tickets) {
        if (menu != null) {
            menu.dispose();
        }
        menu = new Menu(shell, SWT.POP_UP);
        MenuItem exitCollectDb = new MenuItem(menu, SWT.PUSH);
        exitCollectDb.setText("Exit");
        exitCollectDb.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                shell.dispose();
            }
        });
        MenuItem reportBug = new MenuItem(menu, SWT.PUSH);
        reportBug.setText("Report a bug");
        reportBug.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                reportBug();
            }
        });
        MenuItem setupSubMenu = new MenuItem(menu, SWT.CASCADE);
        setupSubMenu.setText("Setup");
        Menu setupMenu = new Menu(setupSubMenu);
        createSetupMenu(setupMenu);
        setupSubMenu.setMenu(setupMenu);
        new MenuItem(menu, SWT.SEPARATOR);
        MenuItem manageTickets = new MenuItem(menu, SWT.PUSH);
        manageTickets.setText("Manage Tickets");
        manageTickets.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                manageTickets();
            }
        });
        MenuItem editActivities = new MenuItem(menu, SWT.PUSH);
        editActivities.setText("Edit Activities");
        editActivities.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                editActivities();
            }
        });
        {
            final MenuItem fetchTicketItem = new MenuItem(menu, SWT.PUSH);
            fetchTicketItem.setText("Fetch Ticket");
            fetchTicketItem.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event event) {
                    fetchTicket();
                }
            });
        }
        MenuItem resyncTrac = new MenuItem(menu, SWT.PUSH);
        resyncTrac.setText("Resync Trac");
        resyncTrac.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                resyncTrac();
            }
        });
        MenuItem generateReport = new MenuItem(menu, SWT.PUSH);
        generateReport.setText("Generate Report");
        generateReport.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                generateReport();
            }
        });
        for (Plugin plugin : plugins) {
            MenuProvider menuContribution = plugin.getMenuContribution();
            if (menuContribution != null) {
                menuContribution.createItem(DefaultStyle.INSTANCE, menu);
            }
        }
        new MenuItem(menu, SWT.SEPARATOR);
        final boolean groupByComponents = ui.getGroupByComponent();
        List<Ticket> sortedTickets = new ArrayList<Ticket>(tickets);
        Collections.sort(sortedTickets, new Comparator<Ticket>() {

            public int compare(Ticket a1, Ticket a2) {
                int tracId1 = a1.getRemoteId();
                int tracId2 = a2.getRemoteId();
                if (groupByComponents) {
                    int componentResult = Collator.getInstance().compare(a1.getComponent(), a2.getComponent());
                    if (componentResult != 0) {
                        return componentResult;
                    }
                } else {
                    if (tracId1 == 0) {
                        if (tracId2 == 0) {
                            int summaryCompareResult = Collator.getInstance().compare(a1.getSummary(), a2.getSummary());
                            if (summaryCompareResult != 0) {
                                return summaryCompareResult;
                            }
                        } else {
                            return -1;
                        }
                    } else {
                        if (tracId2 == 0) {
                            return 1;
                        }
                    }
                }
                if (tracId1 > tracId2) {
                    return 1;
                } else if (tracId1 < tracId2) {
                    return -1;
                } else if (a1.getId() > a2.getId()) {
                    return 1;
                } else if (a1.getId() < a2.getId()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        final int[] lastId = { activeTicket };
        final Map<Integer, MenuItem> itemsById = new HashMap<Integer, MenuItem>();
        String component = null;
        for (Ticket ticket : sortedTickets) {
            if (groupByComponents) {
                String ticketComponent = ticket.getComponent();
                if (component == null) {
                    component = ticketComponent;
                } else if (!component.equals(ticketComponent)) {
                    MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
                    separator.setText(component);
                    component = ticketComponent;
                }
            }
            final int id = ticket.getId();
            int tracId = ticket.getRemoteId();
            final MenuItem ticketItem = new MenuItem(menu, SWT.RADIO);
            itemsById.put(id, ticketItem);
            String title = ((tracId > 0) ? "#" + tracId + ": " : "") + ticket.getSummary();
            ticketItem.setText(title.replace("&", "&&"));
            ticketItem.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event event) {
                    if (ticketItem.getSelection()) {
                        try {
                            MenuItem lastItem = itemsById.get(lastId[0]);
                            if (lastItem != null) {
                                lastItem.setSelection(false);
                            }
                            lastId[0] = id;
                            switchToActivity(id, System.currentTimeMillis(), "");
                        } catch (DBException e) {
                            handleError(e);
                        }
                    }
                }
            });
            if (id == activeTicket) {
                ticketItem.setSelection(true);
            }
            ticketItem.setData(ticket);
        }
        {
            final int id = 0;
            closeItem = new MenuItem(menu, SWT.RADIO);
            itemsById.put(id, closeItem);
            closeItem.setText("Other");
            closeItem.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event event) {
                    if (closeItem.getSelection()) {
                        try {
                            MenuItem lastItem = itemsById.get(lastId[0]);
                            if (lastItem != null) {
                                lastItem.setSelection(false);
                            }
                            lastId[0] = id;
                            switchToActivity(id, System.currentTimeMillis(), "");
                        } catch (DBException e) {
                            handleError(e);
                        }
                    }
                }
            });
            if (id == activeTicket) {
                closeItem.setSelection(true);
            }
        }
    }

    protected void generateReport() {
        new ReportDialog(this, new DialogConfigImpl(layout.getReportBounds())).open();
    }

    private void createSetupMenu(Menu setupMenu) {
        MenuItem editPreferences = new MenuItem(setupMenu, SWT.PUSH);
        editPreferences.setText("Preferences");
        editPreferences.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                editPreferences();
            }
        });
        MenuItem setupDb = new MenuItem(setupMenu, SWT.PUSH);
        setupDb.setText("Setup DB");
        setupDb.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                try {
                    db.setupDb();
                } catch (DBException e) {
                    handleError(e);
                }
            }
        });
        MenuItem aboutItem = new MenuItem(setupMenu, SWT.PUSH);
        aboutItem.setText("About");
        aboutItem.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                new AboutDialog(display, new DialogConfigImpl(layout.getAboutBounds()), DefaultStyle.INSTANCE, version).open();
            }
        });
    }

    protected void reportBug() {
        new DialogBase(display, new DialogConfigImpl(layout.getBrowserBounds())) {

            {
                dialog.setText("Report a bug in TimeCollect");
                dialog.setLayout(new GridLayout(1, true));
                try {
                    Browser browser = new Browser(dialog, SWT.NONE);
                    GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
                    data.widthHint = 800;
                    data.heightHint = 600;
                    browser.setLayoutData(data);
                    browser.setUrl("http://sourceforge.net/apps/trac/timecollect/report/1");
                } catch (SWTError ex) {
                    handleError("Browser not available", "Failed to open browser widget", ex);
                    dialog.dispose();
                }
            }
        }.open();
    }

    public boolean toggleActive() {
        long now = System.currentTimeMillis();
        boolean newActive = !active;
        try {
            ClientTransaction tx = db.begin();
            try {
                if (newActive) {
                    tx.setActive(now, newActive);
                    startUptimeDetector();
                } else {
                    UptimeDetector.stopDetector();
                    tx.setActive(now, newActive);
                }
                tx.commit();
            } finally {
                tx.close();
            }
            setActive(newActive);
        } catch (DBException e) {
            handleError(e);
        }
        try {
            db.sendCollectionData(now);
        } catch (DBException e) {
            handleError(e);
        }
        return active;
    }

    private void setActive(boolean newActive) {
        active = newActive;
        updateImage();
    }

    protected void editActivities() {
        new EditActivitiesDialog(display, new DialogConfigImpl(layout.getEditActivitiesBounds()), this.dbConfig.getCollectInterval() * 2) {

            @Override
            protected void handleSave(ActivityTable table) {
                try {
                    boolean detectorActive = UptimeDetector.hasDetector();
                    if (detectorActive) {
                        UptimeDetector.stopDetector();
                        try {
                            doSave(table);
                        } finally {
                            startUptimeDetector();
                        }
                    } else {
                        doSave(table);
                    }
                } catch (DBException ex) {
                    handleError(ex);
                }
            }

            private void doSave(ActivityTable table) throws DBException {
                ClientTransaction tx = db.begin();
                try {
                    tx.saveActivities(table);
                    tx.commit();
                } catch (DBException ex) {
                    handleError(ex);
                } finally {
                    tx.close();
                }
            }

            @Override
            protected ActivityTable handleLoad(long start, long stop) {
                try {
                    ClientContext tx = db.context();
                    try {
                        return tx.loadActivities(start, stop, true);
                    } finally {
                        tx.close();
                    }
                } catch (DBException e) {
                    handleError(e);
                    return null;
                }
            }
        }.open();
    }

    private void handleError(TracException tracException) {
        handleError("Trac problem", "Trac access failed", tracException);
    }

    void handleError(DBException dbException) {
        handleError("Database problem", "Database access failed", dbException);
    }

    void handleError(String title, String message, Throwable problem) {
        MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
        box.setText(title);
        StringBuilder buffer = new StringBuilder();
        buffer.append(message);
        if (problem != null) {
            while (true) {
                buffer.append(": ");
                String exceptionMessage = problem.getMessage();
                if (exceptionMessage != null) {
                    buffer.append(exceptionMessage);
                } else {
                    buffer.append(problem.getClass().getSimpleName());
                }
                Throwable cause = problem.getCause();
                if (cause == null || cause == problem) {
                    break;
                }
                problem = cause;
            }
        }
        buffer.append('.');
        box.setMessage(buffer.toString());
        box.open();
    }

    public void resyncTrac() {
        try {
            ClientTransaction tx = db.begin();
            try {
                Set<Integer> matchingRemoteIds = new HashSet<Integer>(trac.queryTickets());
                Ticket activeTicket = tx.getActiveTicket();
                if (activeTicket != null && activeTicket.getRemoteId() > 0) {
                    matchingRemoteIds.add(activeTicket.getRemoteId());
                }
                Set<Integer> droppedRemoteIds = new HashSet<Integer>();
                tx.loadExplicitTickets(droppedRemoteIds, LCState.DROPPED);
                droppedRemoteIds.removeAll(matchingRemoteIds);
                tx.loadExplicitTickets(matchingRemoteIds, LCState.SELECTED);
                TracLookup tracLookup = new TracLookup(this, trac);
                tracLookup.lookup(matchingRemoteIds, LCState.MATCHED);
                tracLookup.lookup(droppedRemoteIds, LCState.INACTIVE);
                Map<Integer, Ticket> mergedTickets = tx.mergeActivities(tracLookup.getTickets());
                tx.commit();
                createMenu(activeTicket.getId(), mergedTickets.values());
            } finally {
                tx.close();
            }
        } catch (TracException ex) {
            handleError(ex);
        } catch (DBException ex) {
            handleError(ex);
        }
    }

    private static <T> List<T> concat(Set<T> s1, Set<T> s2) {
        ArrayList<T> result = new ArrayList<T>(s1.size() + s2.size());
        result.addAll(s1);
        result.addAll(s2);
        return result;
    }

    public void quit() {
        shell.dispose();
    }

    void updateImage() {
        item.setImage(active ? playImage : pauseImage);
    }

    public void fetchTicket() {
        DialogConfigImpl config = new DialogConfigImpl(layout.getFetchTicketBounds());
        TicketNumberDialog dialog = new TicketNumberDialog(display, config, this, this, new LocalTicketResolver(db), trac);
        dialog.open();
    }

    public Activity getCurrentActivity() throws DBException {
        ClientContext tx = db.context();
        try {
            return tx.getCurrentActivity();
        } finally {
            tx.close();
        }
    }

    @Override
    public List<Activity> getLatestActivities(int cntActivities, int lastDays) {
        try {
            ClientContext tx = db.context();
            try {
                List<Activity> activities = tx.getLatestActivities(cntActivities, lastDays);
                if ((!tx.isActive()) && (!activities.isEmpty())) {
                    Activity pause = new Activity(activities.get(0).getStop(), System.currentTimeMillis(), null);
                    activities.add(0, pause);
                }
                return activities;
            } finally {
                tx.close();
            }
        } catch (DBException ex) {
            handleError(ex);
        }
        return Collections.emptyList();
    }

    public void fetchTicket(final Ticket ticket) {
        display.syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    internalFetchTicket(ticket, now);
                    resyncTrac();
                } catch (DBException ex) {
                    handleError(ex);
                }
            }
        });
    }

    public void startTicket(final Ticket ticket, final String description) {
        display.syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    int id = internalFetchTicket(ticket, now);
                    switchToActivity(id, now, description);
                    resyncTrac();
                } catch (DBException ex) {
                    handleError(ex);
                }
            }
        });
    }

    public void changeTicket(final Ticket ticket, final String description) {
        display.syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    int id = internalFetchTicket(ticket, now);
                    boolean success;
                    ClientTransaction tx = db.begin();
                    try {
                        success = tx.changeCurrentActivity(id, now, description);
                        tx.commit();
                    } finally {
                        tx.close();
                    }
                    if (success && !active) {
                        setActive(true);
                        startUptimeDetector();
                    }
                    resyncTrac();
                } catch (DBException ex) {
                    handleError(ex);
                }
            }
        });
    }

    private int internalFetchTicket(Ticket ticket, long now) throws DBException {
        int id;
        if (ticket != null) {
            id = ticket.getId();
            if (id == 0) {
                ClientTransaction tx = db.begin();
                try {
                    id = tx.activateTracActivity(ticket.getRemoteId(), now);
                    if (id == 0) {
                        ticket.setLiveCycleState(LCState.SELECTED);
                        tx.saveTicket(ticket);
                        id = ticket.getId();
                    }
                    tx.commit();
                } finally {
                    tx.close();
                }
            }
        } else {
            id = 0;
        }
        return id;
    }

    private void manageTickets() {
        try {
            Collection<Ticket> explicitTickets;
            ClientContext tx = db.context();
            try {
                explicitTickets = tx.loadExplicitTickets(false);
            } finally {
                tx.close();
            }
            new ManageTicketsDialog(display, new DialogConfigImpl(layout.getManageTicketsBounds())) {

                @Override
                protected void handleSave(Collection<Ticket> newTickets, Collection<Ticket> removedTickets, Collection<Ticket> changedTickets) {
                    try {
                        ClientTransaction tx = db.begin();
                        try {
                            for (Ticket ticket : newTickets) {
                                tx.saveTicket(ticket);
                            }
                            for (Ticket ticket : changedTickets) {
                                tx.updateTicket(ticket);
                            }
                            for (Ticket ticket : removedTickets) {
                                ticket.setLiveCycleState(LCState.DROPPED);
                                tx.updateTicket(ticket);
                            }
                            tx.commit();
                        } finally {
                            tx.close();
                        }
                        updateMenu();
                        super.handleSave(newTickets, removedTickets, changedTickets);
                    } catch (DBException ex) {
                        handleError(ex);
                    }
                }
            }.load(explicitTickets).open();
        } catch (DBException ex) {
            handleError(ex);
        }
    }

    private void editPreferences() {
        ArrayList<TabProvider> providers = new ArrayList<TabProvider>();
        for (Plugin plugin : plugins) {
            TabProvider configEditor = plugin.getConfigEditor();
            if (configEditor != null) {
                providers.add(configEditor);
            }
        }
        providers.add(new AbstractTabProvider() {

            private Button useHttp;

            private Text port;

            private Text user;

            private Text password;

            private Button setPassword;

            @Override
            public void save() {
                boolean httpEnabled = useHttp.getSelection();
                http.setUseHttp(httpEnabled);
                if (httpEnabled) {
                    http.setPort(Integer.parseInt(port.getText()));
                    http.setUser(user.getText());
                    if (setPassword.getSelection()) {
                        String newPassword = password.getText();
                        if (newPassword.length() > 0) {
                            http.setPasswordHash(MD5.digest(newPassword));
                        } else {
                            http.setPasswordHash("");
                        }
                    }
                }
            }

            @Override
            public void load() {
                useHttp.setSelection(http.getUseHttp());
                port.setText(Integer.toString(http.getPort()));
                user.setText(http.getUser());
                setPassword.setSelection(false);
                loadPassword();
                updateEnableSelection();
                updateSetPasswordSelection();
            }

            private void loadPassword() {
                String passwordHash = http.getPasswordHash();
                password.setText(passwordHash);
            }

            @Override
            protected String getTabName() {
                return "Http";
            }

            @Override
            protected void createTabContent(Style style, Composite tab) {
                style.container(tab);
                useHttp = new Button(tab, SWT.CHECK);
                useHttp.setText("Enable HTTP");
                useHttp.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        updateEnableSelection();
                    }
                });
                label(tab, "Port");
                port = new Text(tab, SWT.BORDER);
                style.input(port);
                label(tab, "User");
                user = new Text(tab, SWT.BORDER);
                style.input(user);
                setPassword = new Button(tab, SWT.CHECK);
                setPassword.setText("Change Password");
                setPassword.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        updateSetPasswordSelection();
                    }
                });
                style.input(setPassword);
                label(tab, "Password");
                password = new Text(tab, SWT.BORDER | SWT.PASSWORD);
                password.setEnabled(false);
                style.input(password);
            }

            private void updateEnableSelection() {
                boolean enableHttp = useHttp.getSelection();
                port.setEnabled(enableHttp);
                user.setEnabled(enableHttp);
                setPassword.setEnabled(enableHttp);
            }

            private void updateSetPasswordSelection() {
                boolean shouldSetPassword = setPassword.getSelection();
                password.setEnabled(shouldSetPassword);
                if (shouldSetPassword) {
                    password.setText("");
                } else {
                    loadPassword();
                }
            }

            private void label(Composite parent, String text) {
                Label portLabel = new Label(parent, SWT.NONE);
                portLabel.setText(text);
            }
        });
        PreferencesDialog.editConfig(shell.getDisplay(), new DialogConfigImpl(layout.getPreferencesBounds()), dbConfig, trac.getConfig(), ui, new Runnable() {

            public void run() {
                handleNewConfig();
            }
        }, providers.toArray(new TabProvider[providers.size()]));
    }

    private void handleNewConfig() {
        stopHttpd();
        saveConfig();
        trac.reset();
        restartDB();
        try {
            updateMenu();
        } catch (DBException ex) {
            handleError(ex);
        }
        startHttpd();
    }

    private void switchToActivity(final int id, long now, String description) throws DBException {
        ClientTransaction tx = db.begin();
        try {
            switchToActivity(tx, id, now, description);
            tx.commit();
        } finally {
            tx.close();
        }
    }

    private void switchToActivity(ClientTransaction tx, final int id, long now, String description) throws DBException {
        tx.switchActivity(id, now, description);
        startUptimeDetector();
        setActive(true);
    }

    private void startUptimeDetector() {
        UptimeDetector.startDetector(db, dbConfig.getCollectInterval(), uptimeHandler);
    }

    private void updateMenu() throws DBException {
        ClientContext tx = db.context();
        try {
            int activeTicket = tx.loadActiveTicket();
            Collection<Ticket> allTickets = tx.loadTickets(activeTicket).values();
            createMenu(activeTicket, allTickets);
        } finally {
            tx.close();
        }
    }
}
