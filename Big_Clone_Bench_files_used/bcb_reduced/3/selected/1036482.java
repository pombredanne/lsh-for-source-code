package com.jjcp;

import java.sql.*;
import ij.IJ;
import ij.ImageJ;
import ij.Prefs;
import java.awt.Color;
import java.awt.Component;
import java.security.NoSuchAlgorithmException;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.jdesktop.application.Application.ExitListener;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.HighlightPredicate.OrHighlightPredicate;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.decorator.SearchPredicate;
import org.quickserver.net.AppException;
import org.quickserver.net.server.ClientHandler;
import org.quickserver.net.server.QuickServer;
import sun.misc.BASE64Encoder;

/**
 * The application's main frame.
 */
public class SelectorView extends FrameView implements ExitListener {

    private String nodo;

    private String direccion;

    private String puerto;

    private MyDCM dcm;

    private MyDCMRemote dcmRemote;

    private int puntoFijoX, puntoFijoY, refFijoX, refFijoY;

    private String codigoServer = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ASDERT.IOT");

    private String codigoClientWeb = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("OGLPEF.LNA");

    private String codigoClient = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HJBRTY.OPQ");

    private String code = null;

    private QuickServer myServer;

    static SelectorView meYo;

    private String login = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("PACSSELEC");

    private String pass = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("PACSSELEC");

    private String url = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("JDBC:MYSQL://127.0.0.1/PACSDB");

    private String loginDB = "";

    private String passDB = "";

    private String urlDB = "jdbc:mysql://";

    private String idUser = "";

    private String userName = "";

    private Connection conn;

    private ConfigFile configFile = null;

    private static MyTable jTable1;

    private static MyTable2 jTable2;

    private static MyTable3 jTable3;

    private static MyTableUsers jTableUsers;

    private static MyTableStudies jTableStudies;

    private final Timer messageTimer;

    private final Timer busyIconTimer;

    private final Icon idleIcon;

    private final Icon[] busyIcons = new Icon[15];

    private int busyIconIndex = 0;

    private JDialog aboutBox;

    private ImageViewer im1 = null;

    private ImageViewer im2 = null;

    private ColorHighlighter matchHighlighter;

    public SelectorView(SingleFrameApplication app) {
        super(app);
        initComponents();
        meYo = this;
        jTabbedPane1.setSelectedIndex(0);
        configFile = new ConfigFile();
        configFile.readFile();
        setTextFields();
        reset();
        this.getFrame().setLocation(0, 0);
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STATUSBAR.MESSAGETIMEOUT"));
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STATUSBAR.BUSYANIMATIONRATE"));
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STATUSBAR.BUSYICONS[") + i + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("]"));
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STATUSBAR.IDLEICON"));
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STARTED").equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if (java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DONE").equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if (java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("MESSAGE").equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if (java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("PROGRESS").equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
        jCheckBox2.setSelected(true);
        readApplicationType();
        jTable3.setToolTipText(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HTMLCLICK"));
        jScrollPaneImagenes.setToolTipText(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HTMLCLICK"));
        jRadioButton1.setSelected(true);
        connectDB();
    }

    private Map<String, HighlightPredicate> map;

    public void search(String name, boolean bhighlight, JXTable jxTable) {
        if (matchHighlighter == null) {
            matchHighlighter = new ColorHighlighter(HighlightPredicate.NEVER, null, Color.MAGENTA);
            jxTable.addHighlighter(matchHighlighter);
            map = new HashMap<String, HighlightPredicate>();
        }
        if (bhighlight) {
            String search = name;
            Pattern pattern = Pattern.compile(search);
            HighlightPredicate predicate = new SearchPredicate(pattern, -1, -1);
            map.put(name, predicate);
        } else {
            map.remove(name);
        }
        if (name.equals("")) {
            map.clear();
        }
        HighlightPredicate predicate = new OrHighlightPredicate(map.values());
        matchHighlighter.setHighlightPredicate(predicate);
    }

    private void createHighlighters() {
        jTable1.addHighlighter(HighlighterFactory.createSimpleStriping(HighlighterFactory.CLASSIC_LINE_PRINTER));
        jTable1.addHighlighter(HighlighterFactory.createSimpleStriping(HighlighterFactory.LINE_PRINTER));
    }

    static MyTable getTable1() {
        if (jTable1 != null) {
            return jTable1;
        } else {
            return null;
        }
    }

    static SelectorView getSelectorView() {
        return meYo;
    }

    public void newReportReceived() {
        File myFile = new File(MyDCM.userDir + System.getProperty(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("FILE.SEPARATOR")) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("REPORT") + System.getProperty(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("FILE.SEPARATOR")) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("REPORT.DCM"));
        showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("FICHERO:_") + myFile.getAbsolutePath());
        dcm.doSND1(myFile.getAbsolutePath());
    }

    private void addStudy() {
        int[] rows = jTable1.getSelectedRows();
        if (rows != null) {
            int rowUser = jTableUsers.getSelectedRow();
            if (rowUser > -1) {
                try {
                    for (int i = 0; i < rows.length; i++) {
                        String study = (String) jTable1.getValueAt(rows[i], 1);
                        String patient = (String) jTable1.getValueAt(rows[i], 0);
                        int id_User = Integer.parseInt((String) jTableUsers.getValueAt(rowUser, 0));
                        Statement stmt = conn.createStatement();
                        String query = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("INSERT_INTO_TURYON_WEBUSERS_STUDIES_VALUES_(") + id_User + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(",'") + study + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("','") + patient + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("')");
                        stmt.executeUpdate(query);
                        stmt.close();
                        getStudiesFromUser();
                    }
                } catch (SQLException ex) {
                    System.err.println(ex);
                }
            }
        }
    }

    private void delStudy() {
        int[] rows = jTableStudies.getSelectedRows();
        if (rows != null) {
            int rowUser = jTableUsers.getSelectedRow();
            if (rowUser > -1) {
                String user = (String) jTableUsers.getValueAt(rowUser, 1);
                int value = JOptionPane.showConfirmDialog(this.getFrame(), java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ARE_YOU_SURE_YOU_WANT_TO_DELETE_THE_SELECTED_STUDY_ASSIGNED_TO_USER_") + user + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("?"), java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("WARNING"), JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.YES_OPTION) {
                    for (int i = 0; i < rows.length; i++) {
                        try {
                            String study = (String) jTableStudies.getValueAt(rows[0], 1);
                            Statement stmt = conn.createStatement();
                            String query = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DELETE_FROM_TURYON_WEBUSERS_STUDIES_WHERE_TURYON_WEBUSERS_STUDIES.STUDY_IUID='") + study + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("'");
                            stmt.executeUpdate(query);
                            getStudiesFromUser();
                        } catch (SQLException ex) {
                            System.err.println(ex);
                        }
                    }
                }
            }
        }
    }

    private void getStudiesFromUser() {
        resetTableStudies();
        int row = jTableUsers.getSelectedRow();
        if (row > -1) {
            int id_User = Integer.parseInt((String) jTableUsers.getValueAt(row, 0));
            try {
                Statement stmt = conn.createStatement();
                String query = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECT_*_FROM_TURYON_WEBUSERS_STUDIES_WHERE_TURYON_WEBUSERS_STUDIES.ID_WEBUSER=") + id_User;
                ResultSet res = stmt.executeQuery(query);
                while (res.next()) {
                    Vector data = new Vector();
                    data.addElement(res.getString("id_webuser"));
                    data.addElement(res.getString("study_iuid"));
                    data.addElement(res.getString("patient_name"));
                    addRowTableStudies(data);
                }
                res.close();
                stmt.close();
            } catch (SQLException ex) {
                System.err.println(ex);
            }
        }
    }

    private void connectDB() {
        try {
            if (conn == null) {
                Class.forName(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("COM.MYSQL.JDBC.DRIVER")).newInstance();
                if (configFile == null) {
                    configFile = new ConfigFile();
                    configFile.readFile();
                    setTextFields();
                }
                loginDB = configFile.getKeyValue(ConfigFile.LOGINDB);
                passDB = configFile.getKeyValue(ConfigFile.PASSDB);
                urlDB = urlDB + configFile.getKeyValue(ConfigFile.MACHINEDB) + "/" + configFile.getKeyValue(ConfigFile.NAMEDB);
                conn = DriverManager.getConnection(urlDB, loginDB, passDB);
            }
            if (conn != null) {
                Statement stmt = conn.createStatement();
                String query = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SELECT_*_FROM_TURYON_WEBUSERS");
                ResultSet res = stmt.executeQuery(query);
                while (res.next()) {
                    Vector row = new Vector();
                    row.addElement(res.getString("id_webuser"));
                    row.addElement(res.getString("name_webuser"));
                    row.addElement(res.getString("key_webuser"));
                    int accessType = res.getInt(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ACCESSTYPE"));
                    String accType = "";
                    if (accessType == 0) accType = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ALL"); else if (accessType == 1) accType = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("AUTHORIZED"); else if (accessType == 2) accType = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("NONE");
                    row.addElement(accType);
                    addRowTableUsers(row);
                }
                res.close();
                stmt.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex);
            showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("WARNING"), java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ERROR_CONNECTING_DATA_BASE."));
        } catch (Exception ex) {
            System.out.println(ex);
            showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("EXCEPTION_IN_DATA_BASE_CONNECTION_OR_AUTHENTICATION_ERROR."));
        }
    }

    private void editUser() {
        if (jTableUsers.getSelectedRow() > -1) {
            int row = jTableUsers.getSelectedRow();
            int id_User = Integer.parseInt((String) jTableUsers.getValueAt(row, 0));
            PonerUser pu = new PonerUser(this.getFrame(), true, (String) jTableUsers.getValueAt(row, 1));
            if (pu.getReturnStatus() == PonerUser.RET_OK) {
                try {
                    String clave = pu.obtenerPassword();
                    String newUser = pu.obtenerUser();
                    int accessType = pu.obtenerAccessType();
                    String claveNueva = encipherAMessage(clave);
                    Statement stmt = conn.createStatement();
                    String query = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("UPDATE_TURYON_WEBUSERS_SET_TURYON_WEBUSERS.KEY_WEBUSER='") + claveNueva + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("'_WHERE_TURYON_WEBUSERS.ID_WEBUSER=") + id_User;
                    stmt.executeUpdate(query);
                    query = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("UPDATE_TURYON_WEBUSERS_SET_TURYON_WEBUSERS.NAME_WEBUSER='") + newUser + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("'_WHERE_TURYON_WEBUSERS.ID_WEBUSER=") + id_User;
                    stmt.executeUpdate(query);
                    query = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("UPDATE_TURYON_WEBUSERS_SET_TURYON_WEBUSERS.ACCESSTYPE='") + accessType + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("'_WHERE_TURYON_WEBUSERS.ID_WEBUSER=") + id_User;
                    stmt.executeUpdate(query);
                    refreshUsersTable();
                } catch (SQLException ex) {
                    System.err.println(ex);
                }
            }
        }
    }

    private void addUser() {
        int row = jTableUsers.getSelectedRow();
        PonerUser pu = new PonerUser(this.getFrame(), true, java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("NEW_USER_NAME"));
        if (pu.getReturnStatus() == PonerUser.RET_OK) {
            try {
                String clave = pu.obtenerPassword();
                String newUser = pu.obtenerUser();
                int accessType = pu.obtenerAccessType();
                String claveNueva = encipherAMessage(clave);
                Statement stmt = conn.createStatement();
                String query = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("INSERT_INTO_TURYON_WEBUSERS_(NAME_WEBUSER,KEY_WEBUSER,ACCESSTYPE)_VALUES_('") + newUser + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("','") + claveNueva + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("','") + accessType + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("')");
                stmt.executeUpdate(query);
                refreshUsersTable();
            } catch (SQLException ex) {
                System.err.println(ex);
            }
        }
    }

    private void delUser() {
        if (jTableUsers.getSelectedRow() > -1) {
            int row = jTableUsers.getSelectedRow();
            int id_User = Integer.parseInt((String) jTableUsers.getValueAt(row, 0));
            try {
                int value = JOptionPane.showConfirmDialog(this.getFrame(), java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ARE_YOU_SURE_YOU_WANT_TO_DELETE_THE_SELECTED_USER?"), java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("WARNING"), JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.YES_OPTION) {
                    Statement stmt = conn.createStatement();
                    String query = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DELETE_FROM_TURYON_WEBUSERS_WHERE_TURYON_WEBUSERS.ID_WEBUSER=") + id_User;
                    stmt.executeUpdate(query);
                    refreshUsersTable();
                }
            } catch (SQLException ex) {
                System.err.println(ex);
            }
        }
    }

    private void connectToServer() {
        BufferedReader br = null;
        ObjectInputStream oi = null;
        ObjectOutputStream oo = null;
        PrintWriter out = null;
        Socket socket = null;
        String address = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("127.0.0.1");
        int port = 4123;
        System.out.print(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("CONECCTING.._"));
        try {
            socket = new Socket(address, port);
            System.out.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("CONECTED_TO_") + address + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(":") + port + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("\n"));
            oi = new ObjectInputStream(socket.getInputStream());
            oo = new ObjectOutputStream(socket.getOutputStream());
            oo.writeObject(TuryonCommands.ID_LIST);
            System.out.println(oi.readObject() + "");
            System.out.println(oi.readObject() + "");
            System.out.println(oi.readObject() + "");
            System.out.println(oi.readObject() + "");
            int lines = Integer.parseInt(oi.readObject() + "");
            for (int i = 0; i < lines; i++) {
                Vector vector = new Vector();
                for (int j = 0; j < 6; j++) {
                    String str = oi.readObject() + "";
                    vector.add(str);
                    System.out.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("RECEIVED:_") + str);
                }
                addRowTable1(vector);
            }
        } catch (Exception e) {
            System.out.println(TuryonCommands.SOCKET_ERROR);
            System.err.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ERROR_") + e);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception er) {
                System.err.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ERROR_WHEN_CLOSING:_") + er);
            }
        }
    }

    private void connectionServer(boolean b) throws AppException {
        if (b) {
            if (myServer == null) {
                myServer = new QuickServer(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("COM.JJCP.DICOMCOMMANDHANDLER"));
                myServer.setClientObjectHandler(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("COM.JJCP.DICOMOBJECTHANDLER"));
                myServer.setPort(4123);
                myServer.setTimeout(0);
                myServer.setName(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("TURYON_DICOM_OBJECT_SERVER_V_1.0"));
            }
            try {
                myServer.startServer();
            } catch (AppException e) {
                System.err.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ERROR_IN_SERVER_:_") + e);
            }
        } else {
            try {
                Iterator iter = myServer.findAllClient();
                while (iter.hasNext()) {
                    ((ClientHandler) iter.next()).closeConnection();
                }
                myServer.stopServer();
            } catch (AppException e) {
                System.err.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ERROR_IN_SERVER_:_") + e);
            }
        }
    }

    private void readApplicationType() {
        try {
            String codeFile = configFile.getKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("CODE"));
            if (codeFile.equals(codigoServer)) {
                code = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SERVIDOR");
                System.out.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("MODO:_") + code);
                connectionServer(true);
                return;
            }
            if (codeFile.equals(codigoClient)) {
                code = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("CLIENTE");
                System.out.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("MODO:_") + code);
                setCliente();
                return;
            }
            if (codeFile.equals(codigoClientWeb)) {
                code = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("CLIENTEWEB");
                System.out.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("MODO:_") + code);
                setClienteWeb();
                return;
            }
        } catch (Exception e) {
            System.out.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HA_HABIDO_UNA_EXCEPCION_DE_TIPO_DE_APLICACION."));
            code = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("CLIENTEWEB");
            setClienteWeb();
        }
    }

    private void refreshUsersTable() {
        resetTableUsers();
        connectDB();
    }

    private void setServer() {
        System.out.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("EXECUTING_IN_SERVER_MODE"));
        jTextFieldDireccionExterna.setVisible(true);
        jTextFieldDireccionExterna1.setVisible(true);
        jCheckBox1.setVisible(true);
        jCheckBox3.setVisible(true);
        botonEstudios.setVisible(true);
        botonSeries.setVisible(true);
        botonImagenes.setVisible(true);
        botonExportar.setVisible(true);
        jLabel3.setVisible(true);
        jLabel4.setVisible(true);
        jLabel5.setVisible(true);
        jLabel6.setVisible(true);
        jLabel7.setVisible(true);
        jTextFieldNodo.setVisible(true);
        jTextFieldDireccion.setVisible(true);
        jTextFieldPuerto.setVisible(true);
    }

    private void setClienteWeb() {
        jTextFieldDireccionExterna.setVisible(false);
        jTextFieldDireccionExterna1.setVisible(false);
        jCheckBox1.setVisible(false);
        jCheckBox3.setVisible(false);
        botonEstudios.setVisible(false);
        botonSeries.setVisible(false);
        botonImagenes.setVisible(false);
        botonExportar.setVisible(false);
        jLabel3.setVisible(false);
        jLabel4.setVisible(false);
        jLabel5.setVisible(false);
        jLabel6.setVisible(false);
        jLabel7.setVisible(false);
        jTextFieldNodo.setVisible(false);
        jTextFieldDireccion.setVisible(false);
        jTextFieldPuerto.setVisible(false);
        jTabbedPane1.remove(0);
        jTabbedPane1.remove(0);
    }

    private void setCliente() {
        jTextFieldDireccionExterna.setVisible(false);
        jTextFieldDireccionExterna1.setVisible(false);
        botonEstudios.setVisible(false);
        botonSeries.setVisible(false);
        botonImagenes.setVisible(false);
        botonExportar.setVisible(false);
        jLabel6.setVisible(false);
        jLabel7.setVisible(false);
        connectToServer();
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = SelectorApp.getApplication().getMainFrame();
            aboutBox = new SelectorAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        SelectorApp.getApplication().show(aboutBox);
    }

    private void cambiarCoordenadas() {
        puntoFijoX = (refFijoX + this.getFrame().getWidth() + 5);
        puntoFijoY = refFijoY;
    }

    private void cambiarOrigen() {
        refFijoX = ((int) this.getFrame().getLocation().getX());
        refFijoY = (int) this.getFrame().getLocation().getY();
    }

    private void exportarFichero() {
        try {
            MyFilter kdo = new MyFilter(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("TURYON_DICOM_OBJECT_FILES"), java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".TDO"));
            JFileChooser chooser = new JFileChooser(MyDCM.userDir + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("//.."));
            chooser.setFileFilter((javax.swing.filechooser.FileFilter) kdo);
            if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
                this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("CANCELED_DESTINATION_FOLDER"));
                return;
            }
            String destino = chooser.getSelectedFile().getAbsolutePath();
            if (!destino.endsWith(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("TDO"))) {
                destino = destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".TDO");
            }
            FileOutputStream fos = new FileOutputStream(destino);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            Vector vector = new Vector();
            if (jRadioButton1.isSelected()) {
                for (int i = 0; i < jTable3.getRowCount(); i++) {
                    vector.addElement(jTable3.getRow(i));
                }
            }
            if (jRadioButton2.isSelected()) {
                int[] index = jTable3.getSelectedRows();
                for (int i = 0; i < index.length; i++) {
                    vector.addElement(jTable3.getRow(index[i]));
                }
            }
            String pass = "";
            PonerPassword pp = new PonerPassword(this.getFrame(), true);
            if (pp.getReturnStatus() == PonerPassword.RET_OK) {
                pass = encipherAMessage(pp.obtenerPassword());
            }
            out.writeObject(pass);
            out.writeObject(vector);
            out.writeObject(jTextFieldDireccionExterna.getText());
            this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("EXPORTING_IMAGES_FILE_IN:_") + destino);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String encipherAMessage(String message) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SHA1"));
            sha1.update(message.getBytes(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("UTF-16LE")));
            byte[] digest = sha1.digest();
            BASE64Encoder base64encoder = new BASE64Encoder();
            String cipherTextB64 = base64encoder.encode(digest);
            return cipherTextB64;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SelectorView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SelectorView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    private void guardarDICOM() {
        this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HA_ELEGIDO_DESCARGAR_FICHEROS_DEL_SERVIDOR_REMOTO_DE_TURYON_SYSTEMS,"));
        this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("TENGA_EN_CUENTA_QUE_SI_EXISTEN_FICHEROS_CON_EL_MISMO_NOMBRE_EN_DESTINO_SE_SOBREESCRIBIR�N"));
        JFileChooser chooser = new JFileChooser(MyDCM.userDir + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("//.."));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SE_HA_CANCELADO_LA_SELECCI�N_DEL_DIRECTORIO_DESTINO"));
            return;
        }
        String destino = chooser.getSelectedFile().getAbsolutePath();
        File fileDestino = new File(destino);
        fileDestino.mkdir();
        this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("GUARDADAS_IM�GENES_DICOM_EN:_") + fileDestino.getAbsolutePath());
        Vector urls = new Vector();
        String url1 = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HTTP://") + jTextFieldDireccionExterna.getText() + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(":8080/WADO?REQUESTTYPE=WADO&CONTENTTYPE=APPLICATION/DICOM&STUDYUID=");
        if (jRadioButton1.isSelected()) {
            for (int i = 0; i < jTable3.getRowCount(); i++) {
                String address = url1 + jTable3.getValueAt(i, 4) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&SERIESUID=") + jTable3.getValueAt(i, 5) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&OBJECTUID=") + jTable3.getValueAt(i, 6);
                urls.addElement(address);
                FileDownload fd = new FileDownload();
                DecimalFormat df = new DecimalFormat(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("0000"));
                String number = df.format(i);
                boolean result = fd.download(address, destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("//IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".DCM"));
                if (result) {
                    this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DESCARGADO_FICHERO_EN:_") + destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("/IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".DCM"));
                } else {
                    this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ERROR_AL_DESCARGAR_FICHERO_EN:_") + destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("/IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".DCM"));
                }
            }
        }
        if (jRadioButton2.isSelected()) {
            int[] index = jTable3.getSelectedRows();
            for (int i = 0; i < index.length; i++) {
                String address = url1 + jTable3.getValueAt(index[i], 4) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&SERIESUID=") + jTable3.getValueAt(index[i], 5) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&OBJECTUID=") + jTable3.getValueAt(index[i], 6);
                urls.addElement(address);
                FileDownload fd = new FileDownload();
                DecimalFormat df = new DecimalFormat(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("0000"));
                String number = df.format(i);
                boolean result = fd.download(address, destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("//IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".DCM"));
                if (result) {
                    this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DESCARGADO_FICHERO_EN:_") + destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("/IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".DCM"));
                } else {
                    this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ERROR_AL_DESCARGAR_FICHERO_EN:_") + destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("/IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".DCM"));
                }
            }
        }
        if (jCheckBox1.isSelected()) {
            recogerDatos();
            dcm.doSND1(fileDestino.getAbsolutePath());
        }
        if (jCheckBox3.isSelected()) {
            ImageJ ij = new ImageJ(null);
            IJ.doCommand(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("IMAGE_SEQUENCE..."));
            Prefs.set(Prefs.DIR_IMAGE, "");
        }
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DONE"), 0, 100);
    }

    private void guardarJPEG() {
        this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HA_ELEGIDO_DESCARGAR_FICHEROS_DEL_SERVIDOR_REMOTO_DE_TURYON_SYSTEMS,"));
        this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("TENGA_EN_CUENTA_QUE_SI_EXISTEN_FICHEROS_CON_EL_MISMO_NOMBRE_EN_DESTINO_SE_SOBREESCRIBIR�N"));
        JFileChooser chooser = new JFileChooser(MyDCM.userDir + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("//.."));
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SE_HA_CANCELADO_LA_SELECCI�N_DEL_DIRECTORIO_DESTINO"));
            return;
        }
        String destino = chooser.getSelectedFile().getAbsolutePath();
        File fileDestino = new File(destino);
        fileDestino.mkdir();
        this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("GUARDADAS_IM�GENES_JPEGF_EN:_") + fileDestino.getAbsolutePath());
        Vector urls = new Vector();
        String url1 = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HTTP://") + jTextFieldDireccionExterna.getText() + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(":8080/WADO?REQUESTTYPE=WADO&STUDYUID=");
        if (jRadioButton1.isSelected()) {
            int total = jTable3.getRowCount();
            for (int i = 0; i < jTable3.getRowCount(); i++) {
                String address = url1 + jTable3.getValueAt(i, 4) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&SERIESUID=") + jTable3.getValueAt(i, 5) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&OBJECTUID=") + jTable3.getValueAt(i, 6);
                urls.addElement(address);
                FileDownload fd = new FileDownload();
                DecimalFormat df = new DecimalFormat(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("0000"));
                String number = df.format(i);
                boolean result = fd.download(address, destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("//IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".JPG"));
                if (result) {
                    this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DESCARGADO_FICHERO_EN:_") + destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("/IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".JPG"));
                } else {
                    this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ERROR_AL_DESCARGAR_FICHERO_EN:_") + destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("/IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".JPG"));
                }
            }
        }
        if (jRadioButton2.isSelected()) {
            int[] index = jTable3.getSelectedRows();
            for (int i = 0; i < index.length; i++) {
                String address = url1 + jTable3.getValueAt(index[i], 4) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&SERIESUID=") + jTable3.getValueAt(index[i], 5) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&OBJECTUID=") + jTable3.getValueAt(index[i], 6);
                urls.addElement(address);
                FileDownload fd = new FileDownload();
                DecimalFormat df = new DecimalFormat(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("0000"));
                String number = df.format(i);
                boolean result = fd.download(address, destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("//IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".JPG"));
                if (result) {
                    this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DESCARGADO_FICHERO_EN:_") + destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("/IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".JPG"));
                } else {
                    this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("ERROR_AL_DESCARGAR_FICHERO_EN:_") + destino + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("/IMG_") + number + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".JPG"));
                }
            }
        }
        if (jCheckBox3.isSelected()) {
            ImageJ ij = new ImageJ(null);
            IJ.doCommand(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("IMAGE_SEQUENCE..."));
            Prefs.set(Prefs.DIR_IMAGE, "");
        }
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DONE"), 0, 100);
    }

    private void importarFichero() {
        try {
            MyFilter kdo = new MyFilter(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("FICHEROS_TURYON_DICOM_OBJECT"), java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(".KDO"));
            JFileChooser chooser = new JFileChooser(MyDCM.userDir + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("//.."));
            chooser.setFileFilter(kdo);
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SE_HA_CANCELADO_LA_SELECCI�N_DEL_FICHERO_A_IMPORTAR"));
                return;
            }
            FileInputStream fis = new FileInputStream(chooser.getSelectedFile());
            ObjectInputStream in = new ObjectInputStream(fis);
            resetTable3();
            String password = (String) in.readObject();
            if (!password.equals("")) {
                PonerPassword ppD = new PonerPassword(this.getFrame(), true);
                if (ppD.getReturnStatus() == PonerPassword.RET_OK) {
                    String pass = encipherAMessage(ppD.obtenerPassword());
                    if (!password.equals(pass)) {
                        showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("EL_FICHERO_EST�_PROTEGIDO_CON_CONTRASE�A_Y_LA_QUE_HA_INTRODUCIDO_NO_ES_CORRECTA."));
                        showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("PASS:_") + pass + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("_HASH:_") + password);
                        return;
                    }
                } else {
                    return;
                }
            }
            Vector vector = (Vector) in.readObject();
            for (int i = 0; i < vector.size(); i++) {
                jTable3.addRow((Vector) vector.elementAt(i));
            }
            String address = (String) in.readObject();
            jTextFieldDireccionExterna.setText(address);
            if (jTable3.getRowCount() > 0) {
                ponerOpciones(true);
                if (jTabbedPane1.getTabCount() > 1) {
                    jTabbedPane1.setSelectedIndex(2);
                }
            }
            this.showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("IMPORTADO_FICHERO:_") + chooser.getSelectedFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        mainPanel = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel4 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        botonEstudios = new javax.swing.JButton();
        botonSeries = new javax.swing.JButton();
        botonImagenes = new javax.swing.JButton();
        botonImportar = new javax.swing.JButton();
        botonExportar = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jCheckBox3 = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        botonJPEG = new javax.swing.JButton();
        botonDICOM = new javax.swing.JButton();
        jCheckBox2 = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldNodo = new javax.swing.JTextField();
        jTextFieldDireccion = new javax.swing.JTextField();
        jTextFieldPuerto = new javax.swing.JTextField();
        jTextFieldDireccionExterna = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldDireccionExterna1 = new javax.swing.JTextField();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jXPanel1 = new org.jdesktop.swingx.JXPanel();
        jScrollPaneEstudios = new javax.swing.JScrollPane();
        jScrollPaneEstudios.setViewportView(getJTable1());
        textoBuscar = new javax.swing.JTextField();
        jScrollPaneSeries = new javax.swing.JScrollPane();
        jScrollPaneSeries.setViewportView(getJTable2());
        jScrollPaneImagenes = new javax.swing.JScrollPane();
        jScrollPaneImagenes.setViewportView(getJTable3());
        jSplitPane3 = new javax.swing.JSplitPane();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jSplitPane4 = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jButtonDelStudy = new javax.swing.JButton();
        jButtonAddStudy = new javax.swing.JButton();
        jPanel13 = new javax.swing.JPanel();
        jScrollPaneStudies = new javax.swing.JScrollPane();
        jScrollPaneStudies.setViewportView(getJTableStudies());
        jPanel2 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jScrollPaneUsers = new javax.swing.JScrollPane();
        jScrollPaneUsers.setViewportView(getJTableUsers());
        jPanel9 = new javax.swing.JPanel();
        jButtonAddUser = new javax.swing.JButton();
        jButtonEditUser = new javax.swing.JButton();
        jButtonDelUser = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jScrollPane2 = new javax.swing.JScrollPane();
        jLabelImageView = new javax.swing.JLabel();
        mainPanel.setMinimumSize(new java.awt.Dimension(400, 300));
        mainPanel.setName("mainPanel");
        mainPanel.setPreferredSize(new java.awt.Dimension(900, 700));
        jSplitPane1.setDividerLocation(500);
        jSplitPane1.setDividerSize(11);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setName("jSplitPane1");
        jSplitPane1.setOneTouchExpandable(true);
        jSplitPane2.setDividerLocation(440);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane2.setContinuousLayout(true);
        jSplitPane2.setName("jSplitPane2");
        jSplitPane2.setOneTouchExpandable(true);
        jPanel4.setMaximumSize(new java.awt.Dimension(32000, 32000));
        jPanel4.setName("jPanel4");
        jPanel4.setPreferredSize(new java.awt.Dimension(200, 100));
        jPanel8.setName("jPanel8");
        jPanel8.setLayout(new java.awt.GridLayout(5, 1, 1, 1));
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.jjcp.SelectorApp.class).getContext().getResourceMap(SelectorView.class);
        botonEstudios.setText(resourceMap.getString("botonEstudios.text"));
        botonEstudios.setName("botonEstudios");
        botonEstudios.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonEstudiosActionPerformed(evt);
            }
        });
        jPanel8.add(botonEstudios);
        botonSeries.setText(resourceMap.getString("botonSeries.text"));
        botonSeries.setName("botonSeries");
        botonSeries.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonSeriesActionPerformed(evt);
            }
        });
        jPanel8.add(botonSeries);
        botonImagenes.setText(resourceMap.getString("botonImagenes.text"));
        botonImagenes.setName("botonImagenes");
        botonImagenes.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonImagenesActionPerformed(evt);
            }
        });
        jPanel8.add(botonImagenes);
        botonImportar.setText(resourceMap.getString("botonImportar.text"));
        botonImportar.setName("botonImportar");
        botonImportar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonImportarActionPerformed(evt);
            }
        });
        jPanel8.add(botonImportar);
        botonExportar.setText(resourceMap.getString("botonExportar.text"));
        botonExportar.setName("botonExportar");
        botonExportar.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonExportarActionPerformed(evt);
            }
        });
        jPanel8.add(botonExportar);
        jPanel4.add(jPanel8);
        jPanel5.setMaximumSize(new java.awt.Dimension(270, 170));
        jPanel5.setMinimumSize(new java.awt.Dimension(270, 170));
        jPanel5.setName("jPanel5");
        jPanel5.setPreferredSize(new java.awt.Dimension(270, 170));
        jCheckBox3.setForeground(resourceMap.getColor("jCheckBox3.foreground"));
        jCheckBox3.setText(resourceMap.getString("jCheckBox3.text"));
        jCheckBox3.setToolTipText(resourceMap.getString("jCheckBox3.toolTipText"));
        jCheckBox3.setName("jCheckBox3");
        jLabel2.setFont(resourceMap.getFont("jLabel2.font"));
        jLabel2.setForeground(resourceMap.getColor("jLabel2.foreground"));
        jLabel2.setText(resourceMap.getString("jLabel2.text"));
        jLabel2.setName("jLabel2");
        jCheckBox1.setForeground(resourceMap.getColor("jCheckBox1.foreground"));
        jCheckBox1.setText(resourceMap.getString("jCheckBox1.text"));
        jCheckBox1.setName("jCheckBox1");
        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setText(resourceMap.getString("jRadioButton1.text"));
        jRadioButton1.setName("jRadioButton1");
        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText(resourceMap.getString("jRadioButton2.text"));
        jRadioButton2.setName("jRadioButton2");
        botonJPEG.setFont(resourceMap.getFont("botonJPEG.font"));
        botonJPEG.setForeground(resourceMap.getColor("botonJPEG.foreground"));
        botonJPEG.setText(resourceMap.getString("botonJPEG.text"));
        botonJPEG.setMaximumSize(new java.awt.Dimension(84, 29));
        botonJPEG.setMinimumSize(new java.awt.Dimension(84, 29));
        botonJPEG.setName("botonJPEG");
        botonJPEG.setPreferredSize(new java.awt.Dimension(84, 29));
        botonJPEG.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonJPEGActionPerformed(evt);
            }
        });
        botonDICOM.setForeground(resourceMap.getColor("botonDICOM.foreground"));
        botonDICOM.setText(resourceMap.getString("botonDICOM.text"));
        botonDICOM.setName("botonDICOM");
        botonDICOM.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonDICOMActionPerformed(evt);
            }
        });
        jCheckBox2.setForeground(resourceMap.getColor("jCheckBox2.foreground"));
        jCheckBox2.setText(resourceMap.getString("jCheckBox2.text"));
        jCheckBox2.setName("jCheckBox2");
        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel5Layout.createSequentialGroup().addContainerGap().add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jLabel2).add(jRadioButton1).add(jRadioButton2).add(jPanel5Layout.createSequentialGroup().add(botonJPEG, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(botonDICOM)).add(jCheckBox1).add(jCheckBox2).add(jCheckBox3)).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel5Layout.setVerticalGroup(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel5Layout.createSequentialGroup().add(jLabel2).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jRadioButton1).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jRadioButton2).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(botonJPEG, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(botonDICOM)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jCheckBox1).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jCheckBox2).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jCheckBox3).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel4.add(jPanel5);
        jPanel6.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel6.setName("jPanel6");
        jLabel3.setText(resourceMap.getString("jLabel3.text"));
        jLabel3.setName("jLabel3");
        jLabel4.setText(resourceMap.getString("jLabel4.text"));
        jLabel4.setName("jLabel4");
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText(resourceMap.getString("jLabel5.text"));
        jLabel5.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel5.setName("jLabel5");
        jLabel6.setText(resourceMap.getString("jLabel6.text"));
        jLabel6.setName("jLabel6");
        jTextFieldNodo.setText(resourceMap.getString("textoAE.text"));
        jTextFieldNodo.setName("textoAE");
        jTextFieldDireccion.setText(resourceMap.getString("textoIP.text"));
        jTextFieldDireccion.setName("textoIP");
        jTextFieldPuerto.setText(resourceMap.getString("textoPuerto.text"));
        jTextFieldPuerto.setName("textoPuerto");
        jTextFieldPuerto.setPreferredSize(new java.awt.Dimension(45, 28));
        jTextFieldDireccionExterna.setText(resourceMap.getString("textoNombre.text"));
        jTextFieldDireccionExterna.setName("textoNombre");
        jLabel7.setText(resourceMap.getString("jLabel7.text"));
        jLabel7.setName("jLabel7");
        jTextFieldDireccionExterna1.setText(resourceMap.getString("jTextFieldDireccionExterna1.text"));
        jTextFieldDireccionExterna1.setName("jTextFieldDireccionExterna1");
        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel6Layout.createSequentialGroup().add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel6Layout.createSequentialGroup().add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false).add(jPanel6Layout.createSequentialGroup().add(jLabel3).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(jTextFieldNodo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(jPanel6Layout.createSequentialGroup().add(jLabel4).add(18, 18, 18).add(jTextFieldDireccion))).add(10, 10, 10).add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel6Layout.createSequentialGroup().add(11, 11, 11).add(jLabel5)).add(jTextFieldPuerto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))).add(jPanel6Layout.createSequentialGroup().add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(jLabel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(jTextFieldDireccionExterna, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 23, Short.MAX_VALUE).add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jLabel7).add(jTextFieldDireccionExterna1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 105, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))).addContainerGap()));
        jPanel6Layout.setVerticalGroup(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel6Layout.createSequentialGroup().add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jTextFieldNodo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jLabel5)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel4).add(jTextFieldPuerto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jTextFieldDireccion, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel7).add(jLabel6)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jTextFieldDireccionExterna, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jTextFieldDireccionExterna1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel4.add(jPanel6);
        jSplitPane2.setRightComponent(jPanel4);
        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setMinimumSize(new java.awt.Dimension(0, 0));
        jTabbedPane1.setName("jTabbedPane1");
        jXPanel1.setName("jXPanel1");
        jXPanel1.setLayout(new java.awt.BorderLayout());
        jScrollPaneEstudios.setName("jScrollPaneEstudios");
        jXPanel1.add(jScrollPaneEstudios, java.awt.BorderLayout.CENTER);
        textoBuscar.setFont(resourceMap.getFont("textoBuscar.font"));
        textoBuscar.setForeground(resourceMap.getColor("textoBuscar.foreground"));
        textoBuscar.setName("textoBuscar");
        textoBuscar.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(java.awt.event.KeyEvent evt) {
                textoBuscarKeyReleased(evt);
            }
        });
        jXPanel1.add(textoBuscar, java.awt.BorderLayout.SOUTH);
        jTabbedPane1.addTab(resourceMap.getString("jXPanel1.TabConstraints.tabTitle"), jXPanel1);
        jScrollPaneSeries.setName("jScrollPaneSeries");
        jTabbedPane1.addTab(resourceMap.getString("jScrollPaneSeries.TabConstraints.tabTitle"), jScrollPaneSeries);
        jScrollPaneImagenes.setName("jScrollPaneImagenes");
        jTabbedPane1.addTab(resourceMap.getString("jScrollPaneImagenes.TabConstraints.tabTitle"), jScrollPaneImagenes);
        jSplitPane2.setLeftComponent(jTabbedPane1);
        jSplitPane1.setLeftComponent(jSplitPane2);
        jSplitPane3.setDividerLocation(410);
        jSplitPane3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane3.setContinuousLayout(true);
        jSplitPane3.setName("jSplitPane3");
        jSplitPane3.setOneTouchExpandable(true);
        jPanel7.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel7.setName("jPanel7");
        jPanel7.setPreferredSize(new java.awt.Dimension(300, 300));
        jPanel7.setLayout(new java.awt.BorderLayout());
        jScrollPane1.setName("jScrollPane1");
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setName("jTextArea1");
        jScrollPane1.setViewportView(jTextArea1);
        jPanel7.add(jScrollPane1, java.awt.BorderLayout.CENTER);
        jSplitPane3.setBottomComponent(jPanel7);
        jPanel1.setName("jPanel1");
        jPanel1.setPreferredSize(new java.awt.Dimension(100, 150));
        jPanel1.setLayout(new java.awt.BorderLayout());
        jSplitPane4.setDividerLocation(200);
        jSplitPane4.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane4.setContinuousLayout(true);
        jSplitPane4.setLastDividerLocation(150);
        jSplitPane4.setName("jSplitPane4");
        jSplitPane4.setOneTouchExpandable(true);
        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("jPanel3.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 13), resourceMap.getColor("jPanel3.border.titleColor")));
        jPanel3.setName("jPanel3");
        jPanel3.setLayout(new java.awt.BorderLayout());
        jPanel11.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel11.setName("jPanel11");
        jPanel11.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        jPanel12.setName("jPanel12");
        jButtonDelStudy.setText(resourceMap.getString("jButtonDelStudy.text"));
        jButtonDelStudy.setName("jButtonDelStudy");
        jButtonDelStudy.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDelStudyActionPerformed(evt);
            }
        });
        jButtonAddStudy.setText(resourceMap.getString("jButtonAddStudy.text"));
        jButtonAddStudy.setName("jButtonAddStudy");
        jButtonAddStudy.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddStudyActionPerformed(evt);
            }
        });
        org.jdesktop.layout.GroupLayout jPanel12Layout = new org.jdesktop.layout.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel12Layout.createSequentialGroup().add(jButtonDelStudy).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 10, Short.MAX_VALUE).add(jButtonAddStudy)));
        jPanel12Layout.setVerticalGroup(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jButtonDelStudy).add(jButtonAddStudy)));
        jPanel11.add(jPanel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 160, 30));
        jPanel3.add(jPanel11, java.awt.BorderLayout.SOUTH);
        jPanel13.setName("jPanel13");
        jPanel13.setLayout(new java.awt.BorderLayout());
        jScrollPaneStudies.setName("jScrollPaneStudies");
        jPanel13.add(jScrollPaneStudies, java.awt.BorderLayout.CENTER);
        jPanel3.add(jPanel13, java.awt.BorderLayout.CENTER);
        jSplitPane4.setRightComponent(jPanel3);
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("jPanel2.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 13), resourceMap.getColor("jPanel2.border.titleColor")));
        jPanel2.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel2.setName("jPanel2");
        jPanel2.setPreferredSize(new java.awt.Dimension(150, 150));
        jPanel2.setLayout(new java.awt.BorderLayout());
        jPanel10.setName("jPanel10");
        jPanel10.setLayout(new java.awt.BorderLayout());
        jScrollPaneUsers.setName("jScrollPaneUsers");
        jPanel10.add(jScrollPaneUsers, java.awt.BorderLayout.CENTER);
        jPanel2.add(jPanel10, java.awt.BorderLayout.CENTER);
        jPanel9.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel9.setName("jPanel9");
        jPanel9.setPreferredSize(new java.awt.Dimension(80, 50));
        jPanel9.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        jButtonAddUser.setText(resourceMap.getString("jButtonAddUser.text"));
        jButtonAddUser.setName("jButtonAddUser");
        jButtonAddUser.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddUserActionPerformed(evt);
            }
        });
        jPanel9.add(jButtonAddUser, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 120, -1));
        jButtonEditUser.setText(resourceMap.getString("jButtonEditUser.text"));
        jButtonEditUser.setName("jButtonEditUser");
        jButtonEditUser.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonEditUserActionPerformed(evt);
            }
        });
        jPanel9.add(jButtonEditUser, new org.netbeans.lib.awtextra.AbsoluteConstraints(115, 0, 120, -1));
        jButtonDelUser.setText(resourceMap.getString("jButtonDelUser.text"));
        jButtonDelUser.setName("jButtonDelUser");
        jButtonDelUser.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDelUserActionPerformed(evt);
            }
        });
        jPanel9.add(jButtonDelUser, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 0, 120, -1));
        jPanel2.add(jPanel9, java.awt.BorderLayout.SOUTH);
        jSplitPane4.setLeftComponent(jPanel2);
        jPanel1.add(jSplitPane4, java.awt.BorderLayout.CENTER);
        jSplitPane3.setLeftComponent(jPanel1);
        jSplitPane1.setRightComponent(jSplitPane3);
        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 802, Short.MAX_VALUE));
        mainPanelLayout.setVerticalGroup(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(mainPanelLayout.createSequentialGroup().addContainerGap().add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)));
        menuBar.setName("menuBar");
        fileMenu.setText(resourceMap.getString("fileMenu.text"));
        fileMenu.setName("fileMenu");
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.jjcp.SelectorApp.class).getContext().getActionMap(SelectorView.class, this);
        exitMenuItem.setAction(actionMap.get("quit"));
        exitMenuItem.setText(resourceMap.getString("exitMenuItem.text"));
        exitMenuItem.setName("exitMenuItem");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
        helpMenu.setText(resourceMap.getString("helpMenu.text"));
        helpMenu.setName("helpMenu");
        aboutMenuItem.setAction(actionMap.get("showAboutBox"));
        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text"));
        aboutMenuItem.setName("aboutMenuItem");
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);
        statusPanel.setName("statusPanel");
        statusPanelSeparator.setName("statusPanelSeparator");
        statusMessageLabel.setFont(resourceMap.getFont("statusMessageLabel.font"));
        statusMessageLabel.setForeground(resourceMap.getColor("statusMessageLabel.foreground"));
        statusMessageLabel.setText(resourceMap.getString("statusMessageLabel.text"));
        statusMessageLabel.setName("statusMessageLabel");
        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel");
        progressBar.setForeground(resourceMap.getColor("progressBar.foreground"));
        progressBar.setDoubleBuffered(true);
        progressBar.setName("progressBar");
        progressBar.setStringPainted(true);
        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 802, Short.MAX_VALUE).add(statusPanelLayout.createSequentialGroup().addContainerGap().add(statusMessageLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 616, Short.MAX_VALUE).add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(statusAnimationLabel).addContainerGap()));
        statusPanelLayout.setVerticalGroup(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(statusPanelLayout.createSequentialGroup().add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(statusMessageLabel).add(statusAnimationLabel).add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(3, 3, 3)));
        jScrollPane2.setMinimumSize(new java.awt.Dimension(0, 0));
        jScrollPane2.setName("jScrollPane2");
        jScrollPane2.setPreferredSize(new java.awt.Dimension(200, 200));
        jLabelImageView.setBackground(resourceMap.getColor("jLabelImage.background"));
        jLabelImageView.setForeground(resourceMap.getColor("jLabelImage.foreground"));
        jLabelImageView.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelImageView.setIcon(resourceMap.getIcon("jLabelImage.icon"));
        jLabelImageView.setText(resourceMap.getString("jLabelImage.text"));
        jLabelImageView.setDisabledIcon(resourceMap.getIcon("jLabelImage.disabledIcon"));
        jLabelImageView.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabelImageView.setMaximumSize(new java.awt.Dimension(32000, 32000));
        jLabelImageView.setName("jLabelImage");
        jLabelImageView.setPreferredSize(new java.awt.Dimension(256, 256));
        jLabelImageView.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jScrollPane2.setViewportView(jLabelImageView);
        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
        addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                propiedad(evt);
            }
        });
    }

    private void botonSeriesActionPerformed(java.awt.event.ActionEvent evt) {
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STARTED"), 0, 100);
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    listarSeries();
                } catch (Exception ex) {
                    Logger.getLogger(SelectorView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        thread.start();
    }

    private void botonEstudiosActionPerformed(java.awt.event.ActionEvent evt) {
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STARTED"), 0, 100);
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    listarEstudios();
                } catch (Exception ex) {
                    Logger.getLogger(SelectorView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        thread.start();
        cambiarCoordenadas();
    }

    private void botonImagenesActionPerformed(java.awt.event.ActionEvent evt) {
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STARTED"), 0, 100);
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    listarImagenes();
                } catch (Exception ex) {
                    Logger.getLogger(SelectorView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        thread.start();
    }

    private void botonImportarActionPerformed(java.awt.event.ActionEvent evt) {
        importarFichero();
    }

    private void botonExportarActionPerformed(java.awt.event.ActionEvent evt) {
        exportarFichero();
    }

    private void botonJPEGActionPerformed(java.awt.event.ActionEvent evt) {
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STARTED"), 0, 100);
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    guardarJPEG();
                } catch (Exception ex) {
                    Logger.getLogger(SelectorView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        thread.start();
    }

    private void botonDICOMActionPerformed(java.awt.event.ActionEvent evt) {
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STARTED"), 0, 100);
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    guardarDICOM();
                } catch (Exception ex) {
                    Logger.getLogger(SelectorView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        thread.start();
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        setPreferences();
    }

    private void propiedad(java.beans.PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("STARTED").equals(propertyName)) {
            if (!busyIconTimer.isRunning()) {
                statusAnimationLabel.setIcon(busyIcons[0]);
                busyIconIndex = 0;
                busyIconTimer.start();
            }
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
        } else if (java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DONE").equals(propertyName)) {
            busyIconTimer.stop();
            statusAnimationLabel.setIcon(idleIcon);
            progressBar.setVisible(false);
            progressBar.setValue(0);
        }
    }

    private void jButtonEditUserActionPerformed(java.awt.event.ActionEvent evt) {
        editUser();
    }

    private void jButtonAddUserActionPerformed(java.awt.event.ActionEvent evt) {
        addUser();
    }

    private void jButtonDelUserActionPerformed(java.awt.event.ActionEvent evt) {
        delUser();
    }

    private void jButtonDelStudyActionPerformed(java.awt.event.ActionEvent evt) {
        delStudy();
    }

    private void jButtonAddStudyActionPerformed(java.awt.event.ActionEvent evt) {
        addStudy();
    }

    private void textoBuscarKeyReleased(java.awt.event.KeyEvent evt) {
        search("", true, jTable1);
        search(textoBuscar.getText(), true, jTable1);
    }

    private void listarEstudios() {
        reset();
        restoreLabelImage();
        jTabbedPane1.setSelectedIndex(0);
        resetTable1();
        resetTable2();
        resetTable1();
        resetTable3();
        resetTable1();
        recogerDatos();
        resetTable1();
        dcm.doQRPatient();
        if (jTable1.getRowCount() > 0) {
            ponerEnabled(botonSeries, true);
            ponerEnabled(botonImagenes, false);
        }
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DONE"), 0, 100);
    }

    public Vector listarEstudiosRemoto() {
        recogerDatos();
        return dcmRemote.doQRPatient();
    }

    private void reset() {
        restoreLabelImage();
        jTextArea1.setText("");
        resetTable1();
        resetTable2();
        resetTable3();
        ponerEnabled(botonEstudios, true);
        ponerEnabled(botonSeries, false);
        ponerEnabled(botonImagenes, false);
        ponerEnabled(botonExportar, false);
        ponerEnabled(botonImportar, true);
        ponerOpciones(false);
    }

    public void ponerEnabled(Component comp, boolean enabled) {
        if (enabled) {
            comp.setEnabled(true);
        } else {
            comp.setEnabled(false);
        }
    }

    public void ponerOpciones(boolean boo) {
        ponerEnabled(jRadioButton1, boo);
        ponerEnabled(jRadioButton2, boo);
        ponerEnabled(botonJPEG, boo);
        ponerEnabled(botonDICOM, boo);
        ponerEnabled(jCheckBox1, boo);
        ponerEnabled(jCheckBox2, boo);
        ponerEnabled(jCheckBox3, boo);
    }

    private void resetTable1() {
        if (jTable1 != null) {
            while (jTable1.getRowCount() > 0) {
                jTable1.deleteRow(0);
            }
            jTable1.repaint();
            jScrollPaneEstudios.repaint();
        }
    }

    private void resetTable2() {
        if (jTable2 != null) {
            while (jTable2.getRowCount() > 0) {
                jTable2.deleteRow(0);
            }
            jTable2.repaint();
            jScrollPaneSeries.repaint();
        }
    }

    private void resetTable3() {
        if (jTable3 != null) {
            int i = 0;
            while (jTable3.getRowCount() > 0) {
                jTable3.deleteRow(0);
            }
            jTable3.repaint();
            jScrollPaneImagenes.repaint();
        }
    }

    private void resetTableUsers() {
        if (jTableUsers != null) {
            while (jTableUsers.getRowCount() > 0) {
                jTableUsers.deleteRow(0);
            }
            jTableUsers.repaint();
            jScrollPaneUsers.repaint();
        }
    }

    private void resetTableStudies() {
        if (jTableStudies != null) {
            while (jTableStudies.getRowCount() > 0) {
                jTableStudies.deleteRow(0);
            }
            jTableStudies.repaint();
        }
    }

    private void setTextFields() {
        jTextFieldDireccion.setText(configFile.getKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("IP")));
        jTextFieldDireccionExterna.setText(configFile.getKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("NAME_EXTERNAL")));
        jTextFieldNodo.setText(configFile.getKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("AE")));
        jTextFieldPuerto.setText(configFile.getKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("PORT")));
        jTextFieldDireccionExterna1.setText(configFile.getKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("NAME_INTERNAL")));
    }

    public void setPreferences() {
        configFile.setKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("IP"), jTextFieldDireccion.getText());
        configFile.setKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("NAME_EXTERNAL"), jTextFieldDireccionExterna.getText());
        configFile.setKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("AE"), jTextFieldNodo.getText());
        configFile.setKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("PORT"), jTextFieldPuerto.getText());
        configFile.setKeyValue(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("NAME_INTERNAL"), jTextFieldDireccionExterna1.getText());
    }

    private void recogerDatos() {
        nodo = jTextFieldNodo.getText();
        direccion = jTextFieldDireccion.getText();
        puerto = jTextFieldPuerto.getText();
        if (nodo == null || nodo.equals("")) {
            showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("NODE_DATA_NOT_VALID._UNABLE_TO_MAKE_QUERY."));
            return;
        }
        if (direccion == null || direccion.equals("")) {
            showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("NODE_DATA_NOT_VALID._UNABLE_TO_MAKE_QUERY."));
            return;
        }
        if (puerto == null || puerto.equals("")) {
            showMessage(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("NODE_DATA_NOT_VALID._UNABLE_TO_MAKE_QUERY."));
            return;
        } else {
            dcm = new MyDCM();
            dcm.setParent(this);
            dcm.setData(nodo, direccion, puerto);
            dcmRemote = new MyDCMRemote();
            dcmRemote.setParent(this);
            dcmRemote.setData(nodo, direccion, puerto);
            return;
        }
    }

    private void listarImagenes() {
        jTabbedPane1.setSelectedIndex(2);
        resetTable3();
        restoreLabelImage();
        recogerDatos();
        int[] index = jTable2.getSelectedRows();
        for (int i = 0; i < index.length; i++) {
            String studyId = (String) jTable2.getValueAt(index[i], 5);
            String serieId = (String) jTable2.getValueAt(index[i], 6);
            String patientName = (String) jTable2.getValueAt(index[i], 0);
            String desEstudio = (String) jTable2.getValueAt(index[i], 2);
            String desSerie = (String) jTable2.getValueAt(index[i], 3);
            String modalidad = (String) jTable2.getValueAt(index[i], 1);
            dcm.doQR5_2(patientName, studyId, serieId, desEstudio, desSerie, modalidad);
        }
        jTabbedPane1.setSelectedIndex(2);
        if (jTable3.getRowCount() > 0) {
            ponerEnabled(botonExportar, true);
            ponerOpciones(true);
            jRadioButton1.setSelected(true);
        }
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DONE"), 0, 100);
    }

    public Vector listarImagenesRemoto(Vector data) {
        recogerDatos();
        Vector tabla = new Vector();
        for (int i = 1; i < data.size(); i++) {
            Vector rows = (Vector) data.elementAt(i);
            String studyId = (String) rows.elementAt(1);
            String serieId = (String) rows.elementAt(2);
            String patientName = (String) rows.elementAt(0);
            String desEstudio = (String) rows.elementAt(3);
            String desSerie = (String) rows.elementAt(4);
            String modalidad = (String) rows.elementAt(5);
            tabla.addElement(dcmRemote.doQR5_2(patientName, studyId, serieId, desEstudio, desSerie, modalidad));
        }
        return tabla;
    }

    private void verImagenLabel() {
        int row = jTable3.getSelectedRow();
        String serverAddress = "";
        if (code.equals(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SERVIDOR"))) serverAddress = jTextFieldDireccionExterna1.getText(); else serverAddress = jTextFieldDireccionExterna.getText();
        String url1 = "http://" + serverAddress + ":8080/wado?requestType=WADO&studyUID=";
        final String address = url1 + jTable3.getValueAt(row, 4) + "&seriesUID=" + jTable3.getValueAt(row, 5) + "&objectUID=" + jTable3.getValueAt(row, 6);
        Thread thread = new Thread() {

            public void run() {
                try {
                    im2 = new ImageViewer(address);
                    im2.setVisible(false);
                    jScrollPane2.setViewportView(im2.getJScrollPane());
                } catch (Exception ex) {
                    Logger.getLogger(SelectorView.class.getName()).log(Level.SEVERE, null, ex);
                    restoreLabelImage();
                }
            }
        };
        thread.start();
    }

    private void verImagenVentana() {
        int row = jTable3.getSelectedRow();
        System.out.println(code);
        String serverAddress = "";
        if (code.equals(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SERVIDOR"))) serverAddress = jTextFieldDireccionExterna1.getText(); else serverAddress = jTextFieldDireccionExterna.getText();
        String url1 = java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("HTTP://") + serverAddress + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString(":8080/WADO?REQUESTTYPE=WADO&STUDYUID=");
        final String address = url1 + jTable3.getValueAt(row, 4) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&SERIESUID=") + jTable3.getValueAt(row, 5) + java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("&OBJECTUID=") + jTable3.getValueAt(row, 6);
        if (im1 != null) {
            if (!jCheckBox2.isSelected()) {
                im1.setVisible(false);
                im1.dispose();
            } else {
                puntoFijoX = puntoFijoX + 20;
                puntoFijoY = puntoFijoY + 20;
            }
        }
        if (comprobarOrigen()) cambiarCoordenadas();
        final int puntoX = puntoFijoX;
        final int puntoY = puntoFijoY;
        Thread thread = new Thread() {

            public void run() {
                im1 = new ImageViewer(address);
                im1.setLocationXY(puntoX, puntoY);
                im1.setVisible(true);
            }
        };
        thread.start();
    }

    private boolean comprobarOrigen() {
        if (refFijoX != ((int) this.getFrame().getX())) {
            cambiarOrigen();
            return true;
        }
        if (refFijoY != ((int) this.getFrame().getY())) {
            cambiarOrigen();
            return true;
        }
        return false;
    }

    private void restoreLabelImage() {
        jLabelImageView = new javax.swing.JLabel();
        ResourceMap resourceMap = getResourceMap();
        jLabelImageView.setBackground(resourceMap.getColor("jLabelImage.background"));
        jLabelImageView.setForeground(resourceMap.getColor("jLabelImage.foreground"));
        jLabelImageView.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelImageView.setIcon(resourceMap.getIcon("jLabelImage.icon"));
        jLabelImageView.setText(resourceMap.getString("jLabelImage.text"));
        jLabelImageView.setDisabledIcon(resourceMap.getIcon("jLabelImage.disabledIcon"));
        jLabelImageView.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabelImageView.setMaximumSize(new java.awt.Dimension(32000, 32000));
        jLabelImageView.setName("jLabelImage");
        jLabelImageView.setPreferredSize(new java.awt.Dimension(256, 256));
        jLabelImageView.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jLabelImageView.repaint();
        jScrollPane2.setViewportView(jLabelImageView);
        jScrollPane2.repaint();
    }

    private void listarSeries() {
        resetTable2();
        resetTable3();
        jTabbedPane1.setSelectedIndex(1);
        ponerOpciones(false);
        recogerDatos();
        int[] index = jTable1.getSelectedRows();
        for (int i = 0; i < index.length; i++) {
            String id = (String) jTable1.getValueAt(index[i], 1);
            String patientName = (String) jTable1.getValueAt(index[i], 0);
            String descEstudio = (String) jTable1.getValueAt(index[i], 2);
            dcm.doQR6_2(patientName, descEstudio, id);
        }
        jTabbedPane1.setSelectedIndex(1);
        if (jTable2.getRowCount() > 0) {
            ponerEnabled(botonImagenes, true);
        }
        firePropertyChange(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("DONE"), 0, 100);
    }

    public Vector listarSeriesRemoto(Vector data) {
        recogerDatos();
        Vector tabla = new Vector();
        for (int i = 1; i < data.size(); i++) {
            Vector rows = (Vector) data.elementAt(i);
            String patientName = (String) rows.elementAt(0);
            String descEstudio = (String) rows.elementAt(1);
            String id = (String) rows.elementAt(2);
            tabla.addElement(dcmRemote.doQR6_2(patientName, descEstudio, id));
        }
        return tabla;
    }

    public static void showMessage(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje, titulo, JOptionPane.WARNING_MESSAGE, null);
    }

    public void showMessage(String str) {
        jTextArea1.append(str + "\n");
        jTextArea1.setCaretPosition(jTextArea1.getDocument().getLength());
    }

    public void addRowTableUsers(Vector vector) {
        jTableUsers.addRow(vector);
    }

    public void addRowTableStudies(Vector vector) {
        jTableStudies.addRow(vector);
    }

    public void addRowTable1(Vector vector) {
        jTable1.addRow(vector);
    }

    public void addRowTable2(Vector vector) {
        jTable2.addRow(vector);
    }

    public void addRowTable3(Vector vector) {
        jTable3.addRow(vector);
    }

    private MyTable getJTable1() {
        if (jTable1 == null) {
            jTable1 = new MyTable();
            jTable1.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent me) {
                    me.getModifiers();
                    if (me.getClickCount() == 2) {
                        listarSeries();
                    }
                }
            });
        }
        return jTable1;
    }

    private MyTableUsers getJTableUsers() {
        if (jTableUsers == null) {
            jTableUsers = new MyTableUsers();
            jTableUsers.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent me) {
                    me.getModifiers();
                    if (me.getClickCount() == 2) {
                        getStudiesFromUser();
                    }
                }
            });
        }
        return jTableUsers;
    }

    private MyTableStudies getJTableStudies() {
        if (jTableStudies == null) {
            jTableStudies = new MyTableStudies();
            jTableStudies.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent me) {
                    me.getModifiers();
                    if (me.getClickCount() == 2) {
                    }
                }
            });
        }
        return jTableStudies;
    }

    private MyTable2 getJTable2() {
        if (jTable2 == null) {
            jTable2 = new MyTable2();
            jTable2.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent me) {
                    me.getModifiers();
                    if (me.getClickCount() == 2) {
                        listarImagenes();
                    }
                }
            });
        }
        return jTable2;
    }

    private MyTable3 getJTable3() {
        if (jTable3 == null) {
            jTable3 = new MyTable3();
            jTable3.addKeyListener(new java.awt.event.KeyAdapter() {

                public void keyPressed(java.awt.event.KeyEvent evt) {
                    verImagenLabel();
                }
            });
            jTable3.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent me) {
                    me.getModifiers();
                    if (me.getClickCount() == 1) {
                        restoreLabelImage();
                        verImagenLabel();
                    }
                    if (me.getClickCount() == 2) {
                        verImagenVentana();
                    }
                }
            });
        }
        return jTable3;
    }

    public boolean canExit(EventObject arg0) {
        setPreferences();
        try {
            if (conn != null) conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(SelectorView.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            if ((code != null) && code.equals(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("SERVIDOR"))) connectionServer(false);
        } catch (AppException ex) {
        }
        System.out.println(java.util.ResourceBundle.getBundle("com/jjcp/resources/Strings").getString("EJECUCION_FINALIZADA_CORRECTA."));
        return true;
    }

    public void willExit(EventObject arg0) {
    }

    private javax.swing.JButton botonDICOM;

    private javax.swing.JButton botonEstudios;

    private javax.swing.JButton botonExportar;

    private javax.swing.JButton botonImagenes;

    private javax.swing.JButton botonImportar;

    private javax.swing.JButton botonJPEG;

    private javax.swing.JButton botonSeries;

    private javax.swing.ButtonGroup buttonGroup1;

    private javax.swing.JButton jButtonAddStudy;

    private javax.swing.JButton jButtonAddUser;

    private javax.swing.JButton jButtonDelStudy;

    private javax.swing.JButton jButtonDelUser;

    private javax.swing.JButton jButtonEditUser;

    private javax.swing.JCheckBox jCheckBox1;

    private javax.swing.JCheckBox jCheckBox2;

    private javax.swing.JCheckBox jCheckBox3;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabelImageView;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel10;

    private javax.swing.JPanel jPanel11;

    private javax.swing.JPanel jPanel12;

    private javax.swing.JPanel jPanel13;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JPanel jPanel6;

    private javax.swing.JPanel jPanel7;

    private javax.swing.JPanel jPanel8;

    private javax.swing.JPanel jPanel9;

    private javax.swing.JRadioButton jRadioButton1;

    private javax.swing.JRadioButton jRadioButton2;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPaneEstudios;

    private javax.swing.JScrollPane jScrollPaneImagenes;

    private javax.swing.JScrollPane jScrollPaneSeries;

    private javax.swing.JScrollPane jScrollPaneStudies;

    private javax.swing.JScrollPane jScrollPaneUsers;

    private javax.swing.JSplitPane jSplitPane1;

    private javax.swing.JSplitPane jSplitPane2;

    private javax.swing.JSplitPane jSplitPane3;

    private javax.swing.JSplitPane jSplitPane4;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JTextArea jTextArea1;

    private javax.swing.JTextField jTextFieldDireccion;

    private javax.swing.JTextField jTextFieldDireccionExterna;

    private javax.swing.JTextField jTextFieldDireccionExterna1;

    private javax.swing.JTextField jTextFieldNodo;

    private javax.swing.JTextField jTextFieldPuerto;

    private org.jdesktop.swingx.JXPanel jXPanel1;

    private javax.swing.JPanel mainPanel;

    private javax.swing.JMenuBar menuBar;

    private javax.swing.JProgressBar progressBar;

    private javax.swing.JLabel statusAnimationLabel;

    private javax.swing.JLabel statusMessageLabel;

    private javax.swing.JPanel statusPanel;

    private javax.swing.JTextField textoBuscar;
}
