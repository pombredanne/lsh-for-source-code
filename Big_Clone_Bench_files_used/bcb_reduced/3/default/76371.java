import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

public class DoInstallationTasks extends Thread {

    public static final boolean DEBUG_DB = false;

    /**
	 * The lines for the ApolloProperties.plist file, with template macros
	 * inside that will be replaced Edited out DBPORT part of template to see if
	 * that fixes installation problems
	 */
    public static final String[] APOLLO_PLIST = { "{", "\"DefaultsDB\" = \"mysql://%DBUSER%:%DBPASS%@%DBHOST%/%DBNAME%\";", "\"DirectoryDB\" = \"mysql://%DBUSER%:%DBPASS%@%DBHOST%/%DBNAME%\";", "\"PXBundleFormat\" = \"Dev1\";", "\"ManagedObjectsDB\" = " + "\"mysql://%DBUSER%:%DBPASS%@%DBHOST%/%DBNAME%\";", "}" };

    public static final boolean DEBUG = true;

    /** Number of steps in each of the tasks. These have to be updated when you
	 * update the installpack files. */
    public static final int[] FULL_STEPS_PER_TASK = { 1, 1, 201, 219, 14, 13287 };

    public static final String[] FULL_STEP_ACTIONS = { "database", "copymysqldriver", "copyappsup", "copywebdocs", "copyresources", "copywebapp" };

    static String MANIFEST = "META-INF/MANIFEST.MF";

    public String[] fullStepTitles = { Pachyderm21Installer.ISTRINGS.getString("dit.fullsteptitles0"), Pachyderm21Installer.ISTRINGS.getString("dit.fullsteptitles1"), Pachyderm21Installer.ISTRINGS.getString("dit.fullsteptitles2"), Pachyderm21Installer.ISTRINGS.getString("dit.fullsteptitles3"), Pachyderm21Installer.ISTRINGS.getString("dit.fullsteptitles4"), Pachyderm21Installer.ISTRINGS.getString("dit.fullsteptitles5") };

    JProgressBar progressBar;

    JLabel progressMsg;

    ProgressScreen progressScreen;

    private PachydermDoInstallation installer = null;

    private Hashtable<String, String> templateVariables = null;

    /**
	 * Creates a new DoInstallationTasks object.
	 *
	 * @param progressMsg Reference to JLabel that displays with progress bar
	 * @param progressBar Reference to JProgressBar that displays while
	 * installing
	 * @param progressScreen Reference to ProgressScreen
	 * @param installer Reference to controller class
	 */
    public DoInstallationTasks(JLabel progressMsg, JProgressBar progressBar, ProgressScreen progressScreen, PachydermDoInstallation installer) {
        this.progressMsg = progressMsg;
        this.progressBar = progressBar;
        this.progressScreen = progressScreen;
        this.installer = installer;
    }

    /**
	 * Delete the files used by the installer.  Used when done or when
	 * cancelled.
	 *
	 * @param path Directory where installer files have been unpacked to.
	 */
    public static void deleteInstallationFiles(File path) {
        try {
            if (!path.exists()) {
                return;
            }
            if (!path.isDirectory()) {
                System.out.println("Delete: " + path.getCanonicalPath());
                path.delete();
            } else {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; ++i) {
                    deleteInstallationFiles(files[i]);
                }
                path.delete();
            }
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    /**
	 * Open a connection to the database
	 *
	 * @param dbHost Hostname of database server
	 * @param dbPort Port of database server
	 * @param dbName Name of the database to use
	 * @param dbUser Username to use to connect to database
	 * @param dbPassword Password to use to connect to database
	 * @return Connection object that holds the connection to the database
	 */
    public Connection getDBConnection(String dbHost, int dbPort, String dbName, String dbUser, String dbPassword) {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String jdbcURL = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
            System.out.println("jdbcURL = \"" + jdbcURL + "\"");
            Connection conn = DriverManager.getConnection(jdbcURL, dbUser, dbPassword);
            return conn;
        } catch (SQLException sqe) {
            System.out.println("SQLException: " + sqe.getMessage());
            System.out.println("SQLState: " + sqe.getSQLState());
            System.out.println("VendorError: " + sqe.getErrorCode());
        } catch (Exception ex) {
        }
        return null;
    }

    /**
	 * Start the work in a separate thread.
	 */
    public void run() {
        Vector<TaskObject> tasks = setupTasks();
        setupTemplateVariables();
        for (Enumeration<TaskObject> e = tasks.elements(); e.hasMoreElements(); ) {
            TaskObject task = (TaskObject) e.nextElement();
            if (task != null) {
                if (!doTask(task, progressMsg, progressBar)) {
                    progressMsg.setText(Pachyderm21Installer.ISTRINGS.getString("dit.errorwhile") + " " + task.title);
                    progressBar.setEnabled(false);
                    break;
                } else if (!e.hasMoreElements()) {
                    progressMsg.setText(Pachyderm21Installer.ISTRINGS.getString("dit." + "installationcompleted"));
                    progressBar.setValue(progressBar.getMaximum());
                }
            }
        }
        deleteInstallationFiles(Pachyderm21Installer.INSTALL_FILES_PATH);
        progressScreen.installationDone();
    }

    /**
     * Given a string with the path using forward slashes as the separator, get
     * just the last name in the path. That is, the filename if it is a file and
     * the directory name if it is a directory.
	 *
     * @param path String holding the path with forward slashes as the
     *             separator.
	 * @return The last name in the path.
	 */
    private String getLastNameInPath(String path) {
        if (path == null || path.length() < 1) {
            return path;
        }
        if (path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            return path;
        }
        return path.substring(lastSlash + 1);
    }

    private boolean copyMysqlDriver() {
        if (CheckPreReqs.TOMCAT_LIB_PATH != null) {
            File driverFile = new File(Pachyderm21Installer.OUTPUT_DIR_STRING, "mysql-connector-java-3.1.14-bin.jar");
            File toFile = new File(CheckPreReqs.TOMCAT_LIB_PATH, "mysql-connector-java-3.1.14-bin.jar");
            if (driverFile != null && toFile != null && driverFile.exists() && !toFile.exists()) {
                try {
                    byte[] buf = new byte[1024];
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(driverFile));
                    FileOutputStream out = new FileOutputStream(toFile);
                    BufferedOutputStream bout = new BufferedOutputStream(out);
                    while (true) {
                        int nRead = in.read(buf, 0, buf.length);
                        if (nRead <= 0) {
                            break;
                        }
                        bout.write(buf, 0, nRead);
                    }
                    bout.close();
                    out.close();
                } catch (FileNotFoundException fnfe) {
                    System.err.println("FileNotFoundException copying mysql " + "driver");
                    return false;
                } catch (IOException ioe) {
                    System.err.println("IOException copying mysql driver");
                    return false;
                }
            }
        }
        return true;
    }

    /**
	 * Copy the Application Support files into the appropriate place.
	 *
	 * @return True if copied ok. False if not copied ok.
	 */
    private boolean copyAppSup() {
        File webDocsZipFile = new File(Pachyderm21Installer.OUTPUT_DIR_STRING, "pachyderm21-appsupport-installpack.zip");
        File toPath = installer.getAppSupportFilesPath();
        return unzipToDirectory(webDocsZipFile, toPath);
    }

    /**
	 * Copy the Resources files into the appropriate place.
	 *
	 * @return True if copied ok. False if not copied ok.
	 */
    private boolean copyResources() {
        File resourcesZipFile = new File(Pachyderm21Installer.OUTPUT_DIR_STRING, "pachyderm21-resources-installpack.zip");
        File toPath = installer.getResourcerootFilesPath();
        return unzipToDirectory(resourcesZipFile, toPath);
    }

    /**
	 * Copy the WAR files into the appropriate place.
	 *
	 * @return True if copied ok. False if not copied ok.
	 */
    private boolean copyWebApp() {
        File webDocsZipFile = new File(Pachyderm21Installer.OUTPUT_DIR_STRING, "pachyderm21-woa-installpack.zip");
        File toPath = installer.getWarFilesPath();
        if (!unzipToDirectory(webDocsZipFile, toPath)) {
            return false;
        }
        if (!doApolloPropertiesPlist(toPath)) {
            return false;
        }
        return doTemplateFiles(toPath);
    }

    /**
	 * Copy the static web files into the appropriate place.
	 *
	 * @return True if copied ok. False if not copied ok.
	 */
    private boolean copyWebDocs() {
        File webDocsZipFile = new File(Pachyderm21Installer.OUTPUT_DIR_STRING, "pachyderm21-wwwroot-installpack.zip");
        File toPath = installer.getWebrootFilesPath();
        return unzipToDirectory(webDocsZipFile, toPath);
    }

    /**
	 * Create the APOLLOProperties.plist file.
	 *
	 * @param webappPath Path where the WAR files have been copied to.
	 * @return True if created ok. False if it was not.
	 */
    private boolean doApolloPropertiesPlist(File webappPath) {
        File apolloPlist = new File(webappPath, "WEB-INF/Pachyderm2.woa/Contents/Resources/" + "APOLLOProperties.plist");
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(apolloPlist, false));
            for (int i = 0; i < DoInstallationTasks.APOLLO_PLIST.length; ++i) {
                String preline = DoInstallationTasks.APOLLO_PLIST[i];
                if (preline != null) {
                    pw.println(replaceTemplateVariables(preline));
                } else {
                    pw.println();
                }
            }
            pw.close();
            return true;
        } catch (IOException ioe) {
            System.err.println("Error writing new APOLLOProperties.plist");
            return false;
        }
    }

    /**
	 * Do files that have template variables to replace
	 *
	 * @param webappPath Path where the WAR files have been copied to.
	 * @return True if created ok. False if it was not.
	 */
    private boolean doTemplateFiles(File webappPath) {
        try {
            InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream("templatefiles" + ".txt"));
            LineNumberReader lnttfs = new LineNumberReader(isr);
            String templateFileName;
            while ((templateFileName = lnttfs.readLine()) != null) {
                if (templateFileName.trim().length() > 0) {
                    File templateFile = new File(webappPath, templateFileName.trim() + ".template");
                    if (templateFile.exists()) {
                        File outFile = new File(webappPath, templateFileName.trim());
                        if (DEBUG) {
                            System.out.println("template filename = \"" + outFile.getCanonicalPath() + "\"");
                        }
                        LineNumberReader lnr = new LineNumberReader(new FileReader(templateFile));
                        PrintWriter pw = new PrintWriter(new FileWriter(outFile));
                        String outLine;
                        String inLine;
                        while ((inLine = lnr.readLine()) != null) {
                            outLine = replaceTemplateVariables(inLine);
                            if (DEBUG) System.out.println("\ninLine: " + inLine + "\noutLine: " + outLine);
                            pw.println(outLine);
                        }
                        pw.close();
                        lnr.close();
                    } else {
                        System.err.println("Expected template file, " + templateFileName.trim() + ".template, does not exist.");
                        lnttfs.close();
                        return false;
                    }
                }
            }
        } catch (IOException ioe) {
            if (DEBUG) {
                System.err.println("I/O Error doing template files.\n" + ioe);
                ioe.printStackTrace();
            }
            return false;
        }
        return true;
    }

    /**
	 * Do a task.
	 *
	 * @param task TaskObject describing the task to be done.
     * @param progressMsg Reference to JLabel that will be updated with the
     * title of the task.
	 * @param progressBar Reference to the JProgressBar
	 * @return True if task completed successfully, false if it did not.
	 */
    private boolean doTask(TaskObject task, JLabel progressMsg, JProgressBar progressBar) {
        if (task != null) {
            progressMsg.setText(task.title);
            boolean stepCompleted = false;
            if (task.action.equals("copywebapp")) {
                stepCompleted = copyWebApp();
            } else if (task.action.equals("copyappsup")) {
                stepCompleted = copyAppSup();
            } else if (task.action.equals("copywebdocs")) {
                stepCompleted = copyWebDocs();
            } else if (task.action.equals("copyresources")) {
                stepCompleted = copyResources();
            } else if (task.action.equals("copymysqldriver")) {
                stepCompleted = copyMysqlDriver();
            } else if (task.action.equals("database")) {
                stepCompleted = setupDatabase();
            }
            return stepCompleted;
        }
        return false;
    }

    /**
	 * Replace any backslashes in the string with forward slashes.
	 *
	 * @param inString String that might have backslashes in it.
	 * @return String with backslashes replaced with forward slashes.
	 */
    private String replaceSlashes(String inString) {
        if (inString == null || inString.indexOf("\\") < 0) {
            return inString;
        }
        StringBuffer sb = new StringBuffer(inString);
        for (int i = 0; i < sb.length(); ++i) {
            if (sb.charAt(i) == '\\') {
                sb.setCharAt(i, '/');
            }
        }
        return sb.toString();
    }

    /**
	 * Replace the template variables within the string with the appropriate
	 * values. Template variables are things like %DBUSER%. They always begin
	 * and end with a %.
	 *
	 * @param inString The string that has template variables to replace.
	 * @return The string with the template variables replaced.
	 */
    private String replaceTemplateVariables(String inString) {
        if (inString == null || inString.indexOf('%') < 0 || templateVariables == null) {
            return inString;
        }
        StringBuffer sb = new StringBuffer(inString);
        int firstMarker = sb.indexOf("%");
        while (firstMarker >= 0) {
            int secondMarker = sb.indexOf("%", firstMarker + 1);
            if (secondMarker > firstMarker) {
                String templateVariable = sb.substring(firstMarker + 1, secondMarker);
                System.out.println("Have template variable of \"" + templateVariable + "\"");
                String replacement = (String) templateVariables.get(templateVariable);
                if (replacement != null) {
                    sb.replace(firstMarker, secondMarker + 1, replacement);
                }
                firstMarker = sb.indexOf("%", firstMarker + 1);
            } else {
                break;
            }
        }
        return sb.toString();
    }

    /**
	 * Setup the database for Pachyderm.
	 *
	 * @return True if everything was setup ok. False if it was not.
	 */
    private boolean setupDatabase() {
        if (DoInstallationTasks.DEBUG_DB) {
            System.out.println("About to setup database");
        }
        if (installer.getRootDBUsername() != null && installer.getRootDBUsername().length() > 1 && installer.getRootDBPassword() != null && installer.getRootDBPassword().length() > 1) {
            if (DoInstallationTasks.DEBUG_DB) {
                System.out.println("Going to call doDBRootPortions");
            }
            if (!doDBRootPortions(installer.getPachyDBHost(), installer.getPachyDBPort(), installer.getPachyDBName(), installer.getPachyDBUsername(), installer.getPachyDBPassword(), installer.getRootDBUsername(), installer.getRootDBPassword())) {
                System.err.println("Root work not able to be completed, " + "not continuing.");
                return false;
            }
            if (DoInstallationTasks.DEBUG_DB) {
                System.out.println("Back from call to doDBRootPortions");
            }
        }
        if (DoInstallationTasks.DEBUG_DB) {
            System.out.println("Going to open SQL files");
        }
        Connection conn = getDBConnection(installer.getPachyDBHost(), installer.getPachyDBPort(), installer.getPachyDBName(), installer.getPachyDBUsername(), installer.getPachyDBPassword());
        if (conn == null) {
            return false;
        }
        Statement stmt = null;
        boolean havePachy20 = false;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM PRESENTATION LIMIT 1");
            if (rs.next()) {
                havePachy20 = true;
            }
            if (!havePachy20) {
                rs = stmt.executeQuery("SELECT * FROM SCREEN LIMIT 1");
                if (rs.next()) {
                    havePachy20 = true;
                }
            }
        } catch (SQLException sqle) {
            System.err.println("Error doing check for presentation, means " + "2.0 doesn't exist");
            System.out.println("SQLException: " + sqle.getMessage());
            System.out.println("SQLState: " + sqle.getSQLState());
            System.out.println("VendorError: " + sqle.getErrorCode());
        } finally {
            try {
                stmt.close();
            } catch (SQLException sqlex) {
            }
        }
        if (havePachy20) {
            Object[] options = { Pachyderm21Installer.ISTRINGS.getString("dialog.abort"), Pachyderm21Installer.ISTRINGS.getString("dialog.keep"), Pachyderm21Installer.ISTRINGS.getString("dialog.overwrite") };
            int result = JOptionPane.showOptionDialog(this.installer, Pachyderm21Installer.ISTRINGS.getString("dit.pachy20msg"), Pachyderm21Installer.ISTRINGS.getString("dit.pachy20title"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (result == 0) {
                System.out.println("Aborted by user before doing database");
                return false;
            } else if (result == 2) {
                havePachy20 = false;
            }
        } else {
            Object[] options = { Pachyderm21Installer.ISTRINGS.getString("dialog.continue"), Pachyderm21Installer.ISTRINGS.getString("dialog.abort") };
            int result = JOptionPane.showOptionDialog(this.installer, Pachyderm21Installer.ISTRINGS.getString("dit.nopachy20msg"), Pachyderm21Installer.ISTRINGS.getString("dit.nopachy20title"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (result == 1) {
                System.out.println("Aborted by user before doing database");
                return false;
            }
        }
        boolean dbError = false;
        if (havePachy20) {
            try {
                java.util.Date d = new java.util.Date();
                stmt = conn.createStatement();
                stmt.executeUpdate("RENAME TABLE APDEFAULT TO APDEFAULT_2_0_" + d.getTime());
            } catch (SQLException sqle) {
                System.err.println("Error doing check for presentation, " + "means 2.0 doesn't exist");
                System.out.println("SQLException: " + sqle.getMessage());
                System.out.println("SQLState: " + sqle.getSQLState());
                System.out.println("VendorError: " + sqle.getErrorCode());
                dbError = true;
            } finally {
                try {
                    stmt.close();
                } catch (SQLException sqlex) {
                }
            }
        }
        if (dbError) {
            return false;
        }
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate("START TRANSACTION");
            InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream("apdefaults.sql"));
            LineNumberReader lnr = new LineNumberReader(isr);
            String linein;
            while ((linein = lnr.readLine()) != null) {
                if (linein.trim().length() > 0) {
                    String lineout = replaceTemplateVariables(linein);
                    stmt.executeUpdate(lineout);
                }
            }
            stmt.executeUpdate("COMMIT");
            lnr.close();
        } catch (SQLException sqle) {
            System.err.println("error doing apdefaults.sql template");
            System.out.println("sqlexception: " + sqle.getMessage());
            System.out.println("sqlstate: " + sqle.getSQLState());
            System.out.println("vendorerror: " + sqle.getErrorCode());
            if (stmt != null) {
                try {
                    stmt.executeUpdate("ROLLBACK");
                } catch (SQLException sqlex) {
                }
            }
            dbError = true;
        } catch (Exception e) {
            System.err.println("Error doing apdefaults.sql template");
            e.printStackTrace(System.err);
            if (stmt != null) {
                try {
                    stmt.executeUpdate("ROLLBACK");
                } catch (SQLException sqlex) {
                }
            }
            dbError = true;
        } finally {
            try {
                stmt.close();
            } catch (SQLException sqlex) {
            }
        }
        if (dbError) {
            return false;
        }
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate("START TRANSACTION");
            InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream("Pachyderm21.sql"));
            LineNumberReader lnr = new LineNumberReader(isr);
            String linein;
            while ((linein = lnr.readLine()) != null) {
                if (linein.trim().length() > 0) {
                    String lineout = replaceTemplateVariables(linein);
                    if (DoInstallationTasks.DEBUG) {
                        System.out.println("line #" + lnr.getLineNumber());
                    }
                    stmt.executeUpdate(lineout);
                }
            }
            stmt.executeUpdate("COMMIT");
            lnr.close();
        } catch (SQLException sqle) {
            System.err.println("error doing pachyderm21.sql template");
            System.out.println("sqlexception: " + sqle.getMessage());
            System.out.println("sqlstate: " + sqle.getSQLState());
            System.out.println("vendorerror: " + sqle.getErrorCode());
            if (stmt != null) {
                try {
                    stmt.executeUpdate("ROLLBACK");
                } catch (SQLException sqlex) {
                }
            }
            dbError = true;
        } catch (Exception e) {
            System.err.println("error doing pachyderm21.sql template");
            e.printStackTrace(System.err);
            if (stmt != null) {
                try {
                    stmt.executeUpdate("ROLLBACK");
                } catch (SQLException sqlex) {
                }
            }
            dbError = true;
        } finally {
            try {
                stmt.close();
            } catch (SQLException sqlex) {
            }
        }
        if (dbError) {
            return false;
        }
        if (!havePachy20) {
            try {
                stmt = conn.createStatement();
                stmt.executeUpdate("START TRANSACTION");
                InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream("Pachyderm" + "21new.sql"));
                LineNumberReader lnr = new LineNumberReader(isr);
                String linein;
                while ((linein = lnr.readLine()) != null) {
                    if (linein.trim().length() > 0) {
                        String lineout = replaceTemplateVariables(linein);
                        if (DoInstallationTasks.DEBUG) {
                            System.out.println("Line #" + lnr.getLineNumber());
                        }
                        stmt.executeUpdate(lineout);
                    }
                }
                stmt.executeUpdate("COMMIT");
                lnr.close();
            } catch (SQLException sqle) {
                System.err.println("Error doing Pachyderm21new.sql template");
                System.out.println("SQLException: " + sqle.getMessage());
                System.out.println("SQLState: " + sqle.getSQLState());
                System.out.println("VendorError: " + sqle.getErrorCode());
                if (stmt != null) {
                    try {
                        stmt.executeUpdate("ROLLBACK");
                    } catch (SQLException sqlex) {
                    }
                }
                dbError = true;
            } catch (Exception e) {
                System.err.println("Error doing Pachyderm21.sql template");
                e.printStackTrace(System.err);
                if (stmt != null) {
                    try {
                        stmt.executeUpdate("ROLLBACK");
                    } catch (SQLException sqlex) {
                    }
                }
                dbError = true;
            } finally {
                try {
                    stmt.close();
                } catch (SQLException sqlex) {
                }
            }
        }
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        try {
            String adminPassword = installer.getAdminPassword();
            MessageDigest _md = MessageDigest.getInstance("MD5");
            _md.update(adminPassword.getBytes("UTF-8"));
            byte[] md5 = _md.digest();
            ps = conn.prepareStatement("UPDATE AUTHRECORD set PASSWORD=? " + "WHERE USERNAME='administrator'");
            ps.setBytes(1, md5);
            int numupdates = ps.executeUpdate();
            if (DEBUG) System.out.println("Changing admin password, " + "numUpdates = " + numupdates);
            Vector<AdminData> v = installer.getAdditionalAdminAccounts();
            String customPropertiesSPFPre = "{\n \"CXMultiValueArchive\" = {" + "\n  \"class\" = " + "\"ca.ucalgary.apollo.core." + "CXMutableMultiValue\";\n  " + "\"values\" = (\n   " + "{\n    \"class\" = \"ca.ucalgary." + "apollo.core.CXMultiValue$Value\";\n" + "    \"identifier\" = \"0\";\n    " + "\"label\" = \"work\";\n    " + "\"value\" = \"";
            String customPropertiesSPFPost = "\";\n   }\n  );\n  \"identCounter\" = \"1\";\n };\n}";
            if (v.size() > 0) {
                ps = conn.prepareStatement("INSERT INTO `APPERSON` VALUES " + "(NULL,NULL,NULL,NOW(),NULL,NULL," + "?,?,NULL,NULL,NULL,NULL,NULL," + "NULL,?,NULL,NULL,NULL,NULL,NOW()," + "NULL,NULL,NULL,NULL,NULL,NULL,?," + "NULL,NULL,NULL,NULL,NULL)");
                ps1 = conn.prepareStatement("INSERT INTO `AUTHRECORD` VALUES " + "(?,'pachyderm',?,NULL)");
                ps2 = conn.prepareStatement("INSERT INTO `AUTHMAP` " + "(external_id,external_realm," + "map_id,person_id) " + "VALUES (?,'pachyderm',?,?)");
                ps3 = conn.prepareStatement("INSERT INTO `GROUPPERSONJOIN` " + "(group_id,person_id) " + "VALUES(1, ?)");
            }
            for (int i = 0; i < v.size(); ++i) {
                AdminData ad = (AdminData) v.elementAt(i);
                _md = MessageDigest.getInstance("MD5");
                _md.update(ad.getPassword().getBytes("UTF-8"));
                md5 = _md.digest();
                ps.setString(1, customPropertiesSPFPre + ad.getEmail() + customPropertiesSPFPost);
                ps.setString(2, ad.getFirstName());
                ps.setString(3, ad.getLastName());
                ps.setInt(4, i + 2);
                numupdates = ps.executeUpdate();
                if (numupdates == 1) {
                    ps1.setBytes(1, md5);
                    ps1.setString(2, ad.getUsername());
                    ps1.executeUpdate();
                    ps2.setString(1, ad.getUsername() + "@pachyderm");
                    ps2.setInt(2, i + 2);
                    ps2.setInt(3, i + 2);
                    ps2.executeUpdate();
                    ps3.setInt(1, i + 2);
                    ps3.executeUpdate();
                }
            }
        } catch (SQLException sqle) {
            System.err.println("Error doing Pachyderm21new.sql template");
            System.out.println("SQLException: " + sqle.getMessage());
            System.out.println("SQLState: " + sqle.getSQLState());
            System.out.println("VendorError: " + sqle.getErrorCode());
            dbError = true;
        } catch (Exception e) {
            System.err.println("Error doing Pachyderm21.sql template");
            e.printStackTrace(System.err);
            dbError = true;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException sqlex) {
                }
            }
            if (ps1 != null) {
                try {
                    ps1.close();
                } catch (SQLException sqlex) {
                }
            }
            if (ps2 != null) {
                try {
                    ps2.close();
                } catch (SQLException sqlex) {
                }
            }
        }
        return true;
    }

    /**
	 * Do the portions of the database setup that require root access. It
	 * creates the database for Pachyderm if it does not already exist and
	 * create the Pachyderm user giving it permission to access the newly
	 * created database.
	 *
	 * @param dbhost     Hostname of the database server.
	 * @param dbport     Port number of the database server.
	 * @param dbname     Name of the Pachyderm database.
	 * @param dbusername Username of the Pachyderm database user.
	 * @param dbpassword Password of the Pachyderm database user.
	 * @param rootusername
	 *                   Database root username.
	 * @param rootpassword
	 *                   Database root password.
	 *
	 * @return True if everything went ok, false if it did not.
	 */
    private boolean doDBRootPortions(String dbhost, int dbport, String dbname, String dbusername, String dbpassword, String rootusername, String rootpassword) {
        Connection rconn = getDBConnection(dbhost, dbport, "mysql", rootusername, rootpassword);
        if (rconn == null) {
            return false;
        }
        try {
            Statement rst = rconn.createStatement();
            rst.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbname);
            rst.executeUpdate("GRANT ALL ON " + dbname + ".* TO '" + dbusername + "'@'localhost' IDENTIFIED BY '" + dbpassword + "'");
            rst.executeUpdate("GRANT ALL ON " + dbname + ".* TO '" + dbusername + "'@'%' IDENTIFIED BY '" + dbpassword + "'");
            rst.executeUpdate("FLUSH PRIVILEGES");
            return true;
        } catch (SQLException sqle) {
            System.err.println("SQLException while doing root work\n" + sqle);
        } finally {
            if (rconn != null) {
                try {
                    rconn.close();
                } catch (SQLException fsqle) {
                    System.err.println("SQLException closing root connection");
                    return false;
                }
            }
        }
        return false;
    }

    /**
	 * Setup the list of tasks we are actually going to do and count the number
	 * of steps for the progress bar display. Steps are the number of steps that
	 * the progress bar will show, for copying files, there will be one step per
	 * file to copy.
	 *
	 * @return Vector with the list of tasks.
	 */
    private Vector<TaskObject> setupTasks() {
        int totalSteps = 0;
        Vector<TaskObject> tasks = new Vector<TaskObject>();
        for (int i = 0; i < DoInstallationTasks.FULL_STEP_ACTIONS.length; ++i) {
            boolean doThisTask = false;
            String action = DoInstallationTasks.FULL_STEP_ACTIONS[i];
            if (action.equals("copyappsup")) {
                if (PachydermDoInstallation.installApplicationSupportFiles.booleanValue()) {
                    doThisTask = true;
                }
            } else if (action.equals("copywebdocs")) {
                if (PachydermDoInstallation.installWebrootFiles.booleanValue()) {
                    doThisTask = true;
                }
            } else if (action.equals("copywebapp")) {
                if (PachydermDoInstallation.installWARFiles.booleanValue()) {
                    doThisTask = true;
                }
            } else if (action.equals("copyresources")) {
                if (PachydermDoInstallation.installResources.booleanValue()) {
                    doThisTask = true;
                }
            } else if (action.equals("copymysqldriver")) {
                if (PachydermDoInstallation.installDatabase.booleanValue()) {
                    doThisTask = true;
                }
            } else if (action.equals("database")) {
                if (PachydermDoInstallation.installDatabase.booleanValue()) {
                    doThisTask = true;
                }
            }
            if (doThisTask) {
                tasks.add(new TaskObject(fullStepTitles[i], action, DoInstallationTasks.FULL_STEPS_PER_TASK[i]));
                totalSteps += DoInstallationTasks.FULL_STEPS_PER_TASK[i];
            }
        }
        progressBar.setMaximum(totalSteps);
        return tasks;
    }

    /**
	 * Create the Hashtable holding the appopriate values for the template
	 * variables used by replaceTemplateVariables
	 *
	 * @see #replaceTemplateVariables
	 */
    private void setupTemplateVariables() {
        templateVariables = new Hashtable<String, String>();
        templateVariables.put("DBUSER", installer.getPachyDBUsername());
        templateVariables.put("DBPASS", installer.getPachyDBPassword());
        templateVariables.put("DBHOST", installer.getPachyDBHost());
        templateVariables.put("DBPORT", "" + installer.getPachyDBPort());
        templateVariables.put("DBNAME", "" + installer.getPachyDBName());
        templateVariables.put("HOSTNAME", installer.getPachyHostname());
        templateVariables.put("JPEGQUALITY", "" + installer.getJPEGQuality());
        templateVariables.put("SMTP", installer.getSMTPServer());
        templateVariables.put("ADMINEMAIL", installer.getAdminEmail());
        String webDocsFilePath = replaceSlashes(installer.getWebrootFilesPath().getAbsolutePath());
        templateVariables.put("WEBDOCSFILEPATH", webDocsFilePath);
        String webDocsWebpath = getLastNameInPath(webDocsFilePath);
        templateVariables.put("WEBDOCSWEBPATH", webDocsWebpath);
        String resourceFilePath = replaceSlashes(installer.getResourcerootFilesPath().getAbsolutePath());
        templateVariables.put("RESOURCEFILEPATH", resourceFilePath);
        String resourceWebPath = getLastNameInPath(resourceFilePath);
        templateVariables.put("RESOURCEWEBPATH", resourceWebPath);
        String appSupFilePath = replaceSlashes(installer.getAppSupportFilesPath().getAbsolutePath());
        templateVariables.put("APPSUPPORTPATH", appSupFilePath);
        String tmpDirPath = replaceSlashes(installer.getTmpDirPath().getAbsolutePath());
        templateVariables.put("TMPDIR", tmpDirPath);
        String cacheDirPath = replaceSlashes(installer.getCacheDirPath().getAbsolutePath());
        templateVariables.put("CACHEDIR", cacheDirPath);
        String convertPath = replaceSlashes(installer.getConvertPath().getAbsolutePath());
        templateVariables.put("CONVERTPATH", convertPath);
    }

    /**
	 * Unzip a zip file the to specified directory. Create the directory if it
	 * doesn't already exist.
	 *
	 * @param zipFile File object representing the zip file to unzip.
	 * @param toPath File object representing the path to unzip the zip file to.
	 * @return True if everything went ok, false if it did not.
	 */
    private boolean unzipToDirectory(File zipFile, File toPath) {
        if (zipFile == null || toPath == null) {
            return false;
        }
        ZipFile zf = null;
        FileOutputStream out = null;
        BufferedOutputStream bout = null;
        InputStream in = null;
        try {
            byte[] buf = new byte[1024];
            zf = new ZipFile(zipFile);
            int size = zf.size();
            int extracted = 0;
            Enumeration<? extends ZipEntry> entries = zf.entries();
            for (int i = 0; i < size; ++i) {
                progressBar.setValue(progressBar.getValue() + 1);
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String pathname = entry.getName();
                if (MANIFEST.equals(pathname.toUpperCase())) {
                    continue;
                }
                ++extracted;
                in = zf.getInputStream(entry);
                File outFile = new File(toPath, pathname);
                Date archiveTime = new Date(entry.getTime());
                if (!outFile.exists()) {
                    File parent = new File(outFile.getParent());
                    if (parent != null && !parent.exists()) {
                        if (!parent.mkdirs()) {
                            JOptionPane.showMessageDialog(progressScreen, Pachyderm21Installer.ISTRINGS.getString("dit." + "cantcreatedir") + "\n" + parent.getCanonicalPath(), Pachyderm21Installer.ISTRINGS.getString("dialog.error"), JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    }
                    out = new FileOutputStream(outFile);
                    bout = new BufferedOutputStream(out);
                    while (true) {
                        int nRead = in.read(buf, 0, buf.length);
                        if (nRead <= 0) {
                            break;
                        }
                        bout.write(buf, 0, nRead);
                    }
                    bout.close();
                    out.close();
                    outFile.setLastModified(archiveTime.getTime());
                }
            }
            zf.close();
            return true;
        } catch (Exception e) {
            System.err.println(e);
        }
        return false;
    }

    /**
	 * Object to hold information about a task that needs to be performed.
	 *
	 * @author David Risner
	 */
    protected class TaskObject {

        /** Action string used to determine what the task is. */
        protected String action;

        /** Number of steps in the task. */
        protected int steps;

        /** Title of the task for displaying next to the progress bar. */
        protected String title;

        /**
		 * Constructor for the TaskObject.
		 *
		 * @param aTitle   Title of the task for display in the progress bar.
		 * @param anAction Actions string used to determine which task this is.
		 * @param aSteps   Number of steps in this task.
		 */
        protected TaskObject(String aTitle, String anAction, int aSteps) {
            title = aTitle;
            action = anAction;
            steps = aSteps;
        }
    }
}
