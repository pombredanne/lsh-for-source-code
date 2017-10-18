package rolgps;

import java.util.Vector;
import javax.swing.JOptionPane;
import java.io.*;
import java.math.*;
import java.security.*;

/**
 *
 * @author xeph
 */
public class GUI_principal extends javax.swing.JFrame {

    /** Creates new form GUI_principal */
    public GUI_principal() {
        initComponents();
        this.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage("img/myicon.gif"));
        this.setTitle("ROLGps");
        this.jButton9.setVisible(false);
        cargarConfiguracion();
        this.setResizable(false);
        npcsResetButtons();
        mapResetButtons();
        questsResetButtons();
    }

    boolean flag_download_finish = false;

    final String md5_file = "rolgps.jar.md5";

    private byte[] readFile(String name, String mode) {
        byte b[] = null;
        try {
            java.io.RandomAccessFile ra = new java.io.RandomAccessFile(name, mode);
            ra.seek(0);
            b = new byte[(int) ra.length()];
            if (ra.read(b) < 0) System.out.println("FALLO en ramdon access file!!");
        } catch (IOException e) {
            System.out.println("error in readFile");
            e.getStackTrace();
        }
        return b;
    }

    private String readFile(String name) {
        if (name.contains("../")) {
            name = name.substring(3, name.length());
        }
        String fin = null;
        try {
            java.io.File file = new java.io.File(System.getProperty("user.dir"));
            java.io.File f = new java.io.File(file.getAbsolutePath() + "/" + name);
            if (!f.exists()) {
                System.out.println("no existe el fichero " + name);
                System.out.println("DIR = " + file.getAbsolutePath() + "/" + name);
                return "PROBLEMS";
            }
            System.out.println("DIR = " + file.getAbsolutePath() + "/" + name);
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            StringBuffer sb = new StringBuffer();
            String s = br.readLine();
            while (s != null) {
                sb.append(s + "\n");
                s = br.readLine();
            }
            sb.deleteCharAt(sb.length() - 1);
            br.close();
            fr.close();
            fin = sb.toString();
        } catch (Exception e) {
            System.out.println("fallo en readFile, " + e);
        }
        return fin;
    }

    private String calculateMD5(byte[] b) throws java.security.NoSuchAlgorithmException {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(b, 0, b.length);
        return new BigInteger(1, m.digest()).toString(16);
    }

    private void npcsCanModifyOrDelete() {
        jButton2.setEnabled(false);
        jButton3.setEnabled(true);
        jButton4.setEnabled(true);
    }

    private void npcsResetButtons() {
        jButton2.setEnabled(false);
        jButton3.setEnabled(false);
        jButton4.setEnabled(false);
    }

    private void mapResetButtons() {
        jButton16.setEnabled(false);
        jButton19.setEnabled(false);
        jButton18.setEnabled(false);
    }

    private void questsResetButtons() {
        jButton11.setEnabled(false);
        jButton14.setEnabled(false);
        jButton13.setEnabled(false);
    }

    private void npcsCanAdd() {
        jButton2.setEnabled(true);
        jButton3.setEnabled(false);
        jButton4.setEnabled(false);
    }

    private void mapCanModifyOrDelete() {
        jButton16.setEnabled(false);
        jButton19.setEnabled(true);
        jButton18.setEnabled(true);
    }

    private void mapCanAdd() {
        jButton16.setEnabled(true);
        jButton19.setEnabled(false);
        jButton18.setEnabled(false);
    }

    private void questsCanModifyOrDelete() {
        jButton11.setEnabled(false);
        jButton14.setEnabled(true);
        jButton13.setEnabled(true);
    }

    private void questsCanAdd() {
        jButton11.setEnabled(true);
        jButton14.setEnabled(false);
        jButton13.setEnabled(false);
    }

    private void updateFile(String filename, String directory) {
        Mi_hilo hilo = new Mi_hilo();
        hilo.setFileName(filename);
        hilo.setDirectory(directory);
        hilo.start();
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    private int lanzarWebBrowserLinux(int i) {
        String comando = null;
        try {
            switch(i) {
                case 0:
                    comando = "firefox http://rolgps.sourceforge.net";
                    break;
                case 1:
                    comando = "opera http://rolgps.sourceforge.net";
                    break;
                case 2:
                    comando = "konqueror http://rolgps.sourceforge.net";
                    break;
            }
            Runtime.getRuntime().exec(comando);
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    private int lanzarWebBrowserWindows(int i) {
        String comando = null;
        try {
            switch(i) {
                case 0:
                    comando = "c:\\archiv~1\\Mozilla Firefox\\firefox.exe http://rolgps.sourceforge.net";
                    break;
                case 1:
                    comando = "c:\\archiv~1\\Internet Explorer\\iexplore.exe http://rolgps.sourceforge.net";
                    break;
                case 2:
            }
            Runtime.getRuntime().exec(comando);
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    private void cargarConfiguracion() {
        try {
            conf.leerArchivoDeConfiguracion();
        } catch (Exception e) {
        }
        javax.swing.JLabel[] array = this.getLabels();
        for (int i = 0; i < array.length; i++) array[i].setForeground(new java.awt.Color(Integer.parseInt(conf.getColorLetras())));
        this.jRadioButton1.setForeground(new java.awt.Color(Integer.parseInt(conf.getColorLetras())));
        this.jRadioButton2.setForeground(new java.awt.Color(Integer.parseInt(conf.getColorLetras())));
        this.jRadioButton3.setForeground(new java.awt.Color(Integer.parseInt(conf.getColorLetras())));
        this.jRadioButton1.setBackground(new java.awt.Color(0, 0, 0, 0));
        this.jRadioButton2.setBackground(new java.awt.Color(0, 0, 0, 0));
        this.jRadioButton3.setBackground(new java.awt.Color(0, 0, 0, 0));
        this.fondo_5.setIcon(new javax.swing.ImageIcon(conf.getPathFondo()));
        this.fondo_6.setIcon(new javax.swing.ImageIcon(conf.getPathFondo()));
        this.fondo_2.setIcon(new javax.swing.ImageIcon(conf.getPathFondo()));
        this.fondo_3.setIcon(new javax.swing.ImageIcon(conf.getPathFondo()));
        this.fondo_4.setIcon(new javax.swing.ImageIcon(conf.getPathFondo()));
        this.fondo_7.setIcon(new javax.swing.ImageIcon(conf.getPathFondo()));
        if (conf.getIdioma().equals("ingles")) this.jMenuItem2ActionPerformed(null);
    }

    Configuracion conf = new Configuracion();

    private String[] npcs = new String[5];

    private String[] newDataFromNpcs = new String[npcs.length];

    private String[] map = new String[5];

    private String[] newDataFromMap = new String[map.length];

    private Vector npcsResults = new Vector();

    private Vector npcsIndex = new Vector();

    private Vector questsResults = new Vector();

    private Vector questsIndex = new Vector();

    private Vector mapResults = new Vector();

    private Vector mapIndex = new Vector();

    Motor database = new Motor();

    private String[] quests = new String[6];

    private String[] newDataFromQuests = new String[quests.length];

    private String npcsFile = "npcs.txt";

    private String mapFile = "mapa.txt";

    private String questsFile = "quests.txt";

    private Mapa mapa = null;

    public void setMapa(Mapa map) {
        this.mapa = map;
        for (int i = 0; i < mapResults.size(); i++) {
            System.out.println(mapResults.get(i));
            mapa.anyadirPunto(Integer.parseInt(((String) mapResults.get(i)).split(":")[3]), Integer.parseInt(((String) mapResults.get(i)).split(":")[4]));
        }
    }

    private String[] getNpcsFields() {
        String[] aux = new String[npcs.length];
        aux[0] = jTextField1.getText();
        aux[1] = jTextField2.getText();
        aux[2] = jTextField3.getText();
        aux[3] = jTextField4.getText();
        aux[4] = jTextField5.getText();
        for (int x = 0; x < aux.length; x++) {
            if (aux[x].equals("") || aux[x].equals(null)) {
                aux[x] = "??";
            }
        }
        return aux;
    }

    private String[] getMapFields() {
        String[] aux = new String[map.length];
        aux[0] = jTextField11.getText();
        aux[1] = jTextField12.getText();
        aux[3] = jTextField13.getText();
        aux[2] = jTextField15.getText();
        aux[4] = jTextField14.getText();
        for (int x = 0; x < aux.length; x++) {
            if (aux[x].equals("") || aux[x].equals(null)) {
                aux[x] = "??";
            }
        }
        return aux;
    }

    private String[] getQuestsFields() {
        String[] aux = new String[quests.length];
        aux[0] = jTextField6.getText();
        aux[1] = jTextField7.getText();
        aux[3] = jTextField8.getText();
        aux[2] = jTextField9.getText();
        aux[4] = jTextField10.getText();
        aux[5] = jTextArea1.getText();
        for (int x = 0; x < aux.length; x++) {
            if (aux[x].equals("") || aux[x].equals(null)) {
                aux[x] = "??";
            }
        }
        return aux;
    }

    public javax.swing.JLabel[] getLabels() {
        javax.swing.JLabel[] aux = new javax.swing.JLabel[25];
        aux[0] = jLabel1;
        aux[1] = jLabel2;
        aux[2] = jLabel3;
        aux[3] = jLabel4;
        aux[4] = jLabel5;
        aux[5] = jLabel6;
        aux[6] = jLabel7;
        aux[7] = jLabel8;
        aux[8] = jLabel9;
        aux[9] = jLabel10;
        aux[10] = jLabel11;
        aux[11] = jLabel12;
        aux[12] = jLabel14;
        aux[13] = jLabel15;
        aux[14] = jLabel16;
        aux[15] = jLabel17;
        aux[16] = jLabel18;
        aux[17] = jLabel19;
        aux[18] = jLabel20;
        aux[19] = jLabel21;
        aux[20] = jLabel22;
        aux[21] = jLabel23;
        aux[22] = jLabel24;
        aux[23] = jLabel25;
        aux[24] = conexion;
        return aux;
    }

    private void showNpcs() {
        jTextField1.setText(npcs[0]);
        jTextField2.setText(npcs[1]);
        jTextField3.setText(npcs[2]);
        jTextField4.setText(npcs[3]);
        jTextField5.setText(npcs[4]);
    }

    private void showMap() {
        jTextField11.setText(map[0]);
        jTextField12.setText(map[1]);
        jTextField15.setText(map[2]);
        jTextField13.setText(map[3]);
        jTextField14.setText(map[4]);
    }

    private void showQuests() {
        jTextField6.setText(quests[0]);
        jTextField7.setText(quests[1]);
        jTextField8.setText(quests[2]);
        jTextField9.setText(quests[3]);
        jTextField10.setText(quests[4]);
        jTextArea1.setText(quests[5]);
    }

    private void clearNpcsForm() {
        jTextField1.setText("");
        jTextField2.setText("");
        jTextField3.setText("");
        jTextField4.setText("");
        jTextField5.setText("");
        npcs = getNpcsFields();
        newDataFromNpcs = getNpcsFields();
        npcsIndex.clear();
        npcsResults.clear();
        jList1.setListData(npcsIndex);
    }

    private void clearQuestsForm() {
        jTextField6.setText("");
        jTextField7.setText("");
        jTextField8.setText("");
        jTextField9.setText("");
        jTextField10.setText("");
        jTextArea1.setText("");
        quests = getQuestsFields();
        newDataFromQuests = getQuestsFields();
        questsIndex.clear();
        questsResults.clear();
        jList2.setListData(questsIndex);
    }

    private void clearMapForm() {
        jTextField11.setText("");
        jTextField12.setText("");
        jTextField13.setText("");
        jTextField14.setText("");
        jTextField15.setText("");
        map = getMapFields();
        newDataFromMap = getMapFields();
        mapIndex.clear();
        mapResults.clear();
        jList3.setListData(mapIndex);
    }

    private void initComponents() {
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jButton5 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        fondo_5 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jTextField11 = new javax.swing.JTextField();
        jTextField12 = new javax.swing.JTextField();
        jTextField13 = new javax.swing.JTextField();
        jTextField14 = new javax.swing.JTextField();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jButton19 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        jList3 = new javax.swing.JList();
        jLabel18 = new javax.swing.JLabel();
        jTextField15 = new javax.swing.JTextField();
        fondo_2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jTextField8 = new javax.swing.JTextField();
        jTextField9 = new javax.swing.JTextField();
        jTextField10 = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        fondo_3 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        jList4 = new javax.swing.JList();
        jButton21 = new javax.swing.JButton();
        jButton22 = new javax.swing.JButton();
        jButton23 = new javax.swing.JButton();
        fondo_7 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        fondo_6 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jButton20 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        conexion = new javax.swing.JLabel();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        fondo_4 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem7 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMenuItem6 = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        jTabbedPane1.setName("Localizador de NPC's");
        jTabbedPane1.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTabbedPane1MouseClicked(evt);
            }
        });
        jTabbedPane1.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTabbedPane1KeyPressed(evt);
            }
        });
        jPanel1.setLayout(null);
        jLabel1.setForeground(new java.awt.Color(255, 0, 0));
        jLabel1.setText("Nombre");
        jPanel1.add(jLabel1);
        jLabel1.setBounds(10, 10, 49, 15);
        jPanel1.add(jTextField1);
        jTextField1.setBounds(90, 10, 170, 19);
        jLabel2.setForeground(new java.awt.Color(255, 0, 0));
        jLabel2.setText("Ciudad");
        jPanel1.add(jLabel2);
        jLabel2.setBounds(10, 40, 44, 15);
        jPanel1.add(jTextField2);
        jTextField2.setBounds(90, 40, 170, 19);
        jLabel3.setForeground(new java.awt.Color(255, 0, 0));
        jLabel3.setText("Tipo");
        jPanel1.add(jLabel3);
        jLabel3.setBounds(10, 70, 28, 15);
        jPanel1.add(jTextField3);
        jTextField3.setBounds(90, 70, 170, 19);
        jLabel4.setForeground(new java.awt.Color(255, 0, 0));
        jLabel4.setText("X");
        jPanel1.add(jLabel4);
        jLabel4.setBounds(10, 100, 30, 15);
        jPanel1.add(jTextField4);
        jTextField4.setBounds(90, 100, 170, 19);
        jButton1.setText("Buscar");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });
        jPanel1.add(jButton1);
        jButton1.setBounds(10, 170, 80, 25);
        jButton2.setText("Ingresar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton2);
        jButton2.setBounds(90, 170, 90, 25);
        jButton3.setText("Borrar");
        jButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton3);
        jButton3.setBounds(390, 170, 140, 25);
        jButton4.setText("Modificar");
        jButton4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton4);
        jButton4.setBounds(270, 170, 120, 25);
        jScrollPane1.setOpaque(false);
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jList1);
        jPanel1.add(jScrollPane1);
        jScrollPane1.setBounds(270, 10, 259, 140);
        jButton5.setText("Limpiar");
        jButton5.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton5MouseClicked(evt);
            }
        });
        jPanel1.add(jButton5);
        jButton5.setBounds(180, 170, 90, 25);
        jLabel5.setForeground(new java.awt.Color(255, 0, 0));
        jLabel5.setText("Y");
        jPanel1.add(jLabel5);
        jLabel5.setBounds(10, 130, 50, 15);
        jPanel1.add(jTextField5);
        jTextField5.setBounds(90, 130, 170, 19);
        fondo_5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel1.add(fondo_5);
        fondo_5.setBounds(0, 0, 540, 230);
        jTabbedPane1.addTab("NPC's", jPanel1);
        jPanel2.setLayout(null);
        jLabel14.setText("Nombre");
        jPanel2.add(jLabel14);
        jLabel14.setBounds(10, 10, 49, 15);
        jLabel15.setText("Reino");
        jPanel2.add(jLabel15);
        jLabel15.setBounds(10, 70, 40, 15);
        jLabel16.setText("X");
        jPanel2.add(jLabel16);
        jLabel16.setBounds(10, 100, 8, 15);
        jLabel17.setText("Y");
        jPanel2.add(jLabel17);
        jLabel17.setBounds(10, 130, 8, 15);
        jPanel2.add(jTextField11);
        jTextField11.setBounds(90, 10, 170, 19);
        jPanel2.add(jTextField12);
        jTextField12.setBounds(90, 70, 170, 19);
        jPanel2.add(jTextField13);
        jTextField13.setBounds(90, 100, 170, 19);
        jPanel2.add(jTextField14);
        jTextField14.setBounds(90, 130, 170, 19);
        jButton15.setText("Buscar");
        jButton15.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton15MouseClicked(evt);
            }
        });
        jPanel2.add(jButton15);
        jButton15.setBounds(10, 170, 80, 25);
        jButton16.setText("Ingresar");
        jButton16.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton16);
        jButton16.setBounds(90, 170, 90, 25);
        jButton17.setText("Limpiar");
        jButton17.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton17);
        jButton17.setBounds(180, 170, 90, 25);
        jButton18.setText("Modificar");
        jButton18.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton18);
        jButton18.setBounds(270, 170, 120, 25);
        jButton19.setText("Borrar Entrada");
        jButton19.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton19ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton19);
        jButton19.setBounds(390, 170, 140, 25);
        jButton9.setText("Lanzar mapa de prueba");
        jButton9.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton9);
        jButton9.setBounds(300, 150, 179, 25);
        jList3.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList3MouseClicked(evt);
            }
        });
        jScrollPane4.setViewportView(jList3);
        jPanel2.add(jScrollPane4);
        jScrollPane4.setBounds(270, 10, 259, 140);
        jLabel18.setText("Tipo");
        jPanel2.add(jLabel18);
        jLabel18.setBounds(10, 40, 28, 15);
        jPanel2.add(jTextField15);
        jTextField15.setBounds(90, 40, 170, 19);
        fondo_2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel2.add(fondo_2);
        fondo_2.setBounds(0, 0, 540, 230);
        jTabbedPane1.addTab("Mapa General", jPanel2);
        jPanel3.setLayout(null);
        jLabel7.setText("NPC");
        jPanel3.add(jLabel7);
        jLabel7.setBounds(10, 12, 25, 15);
        jLabel8.setText("Quest");
        jPanel3.add(jLabel8);
        jLabel8.setBounds(10, 36, 37, 15);
        jLabel9.setText("Oro");
        jPanel3.add(jLabel9);
        jLabel9.setBounds(10, 60, 50, 15);
        jLabel10.setText("Experiencia");
        jPanel3.add(jLabel10);
        jLabel10.setBounds(10, 85, 70, 15);
        jLabel11.setText("Items");
        jPanel3.add(jLabel11);
        jLabel11.setBounds(10, 110, 35, 15);
        jLabel12.setText("Descripción");
        jPanel3.add(jLabel12);
        jLabel12.setBounds(10, 136, 73, 20);
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);
        jPanel3.add(jScrollPane2);
        jScrollPane2.setBounds(103, 140, 320, 78);
        jPanel3.add(jTextField6);
        jTextField6.setBounds(103, 12, 170, 19);
        jPanel3.add(jTextField7);
        jTextField7.setBounds(103, 36, 170, 19);
        jPanel3.add(jTextField8);
        jTextField8.setBounds(103, 60, 170, 19);
        jPanel3.add(jTextField9);
        jTextField9.setBounds(103, 85, 170, 20);
        jPanel3.add(jTextField10);
        jTextField10.setBounds(103, 110, 170, 19);
        jScrollPane3.setPreferredSize(new java.awt.Dimension(259, 140));
        jList2.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList2MouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(jList2);
        jPanel3.add(jScrollPane3);
        jScrollPane3.setBounds(280, 10, 140, 120);
        jButton10.setText("Buscar");
        jButton10.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton10MouseClicked(evt);
            }
        });
        jPanel3.add(jButton10);
        jButton10.setBounds(430, 10, 100, 25);
        jButton11.setText("Ingresar");
        jButton11.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton11);
        jButton11.setBounds(430, 40, 100, 25);
        jButton12.setText("Limpiar ");
        jButton12.setMaximumSize(new java.awt.Dimension(90, 25));
        jButton12.setMinimumSize(new java.awt.Dimension(90, 25));
        jButton12.setPreferredSize(new java.awt.Dimension(90, 25));
        jButton12.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton12MouseClicked(evt);
            }
        });
        jPanel3.add(jButton12);
        jButton12.setBounds(430, 70, 100, 25);
        jButton13.setText("Modificar");
        jButton13.setMaximumSize(new java.awt.Dimension(90, 25));
        jButton13.setMinimumSize(new java.awt.Dimension(90, 25));
        jButton13.setPreferredSize(new java.awt.Dimension(90, 25));
        jButton13.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton13);
        jButton13.setBounds(430, 100, 100, 25);
        jButton14.setText("Borrar");
        jButton14.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton14);
        jButton14.setBounds(432, 140, 100, 25);
        fondo_3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel3.add(fondo_3);
        fondo_3.setBounds(0, 0, 540, 230);
        jTabbedPane1.addTab("Quests", jPanel3);
        jPanel6.setLayout(null);
        jRadioButton1.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.light"));
        jRadioButton1.setText("Ignis");
        jRadioButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton1.setFocusPainted(false);
        jRadioButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });
        jPanel6.add(jRadioButton1);
        jRadioButton1.setBounds(30, 30, 48, 15);
        jRadioButton2.setBackground(new java.awt.Color(255, 255, 255));
        jRadioButton2.setText("Syrtis");
        jRadioButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton2.setFocusPainted(false);
        jRadioButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jRadioButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton2ActionPerformed(evt);
            }
        });
        jPanel6.add(jRadioButton2);
        jRadioButton2.setBounds(30, 60, 52, 15);
        jRadioButton3.setBackground(new java.awt.Color(255, 255, 255));
        jRadioButton3.setText("Alsius");
        jRadioButton3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton3.setFocusPainted(false);
        jRadioButton3.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jRadioButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton3ActionPerformed(evt);
            }
        });
        jPanel6.add(jRadioButton3);
        jRadioButton3.setBounds(30, 90, 56, 15);
        jScrollPane5.setViewportView(jList4);
        jPanel6.add(jScrollPane5);
        jScrollPane5.setBounds(280, 10, 250, 110);
        jButton21.setText("Actualizar seleccionado");
        jPanel6.add(jButton21);
        jButton21.setBounds(40, 140, 180, 25);
        jButton22.setText("Actualizar todos");
        jPanel6.add(jButton22);
        jButton22.setBounds(40, 180, 180, 25);
        jButton23.setText("Ver mapa");
        jPanel6.add(jButton23);
        jButton23.setBounds(360, 140, 96, 25);
        fondo_7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel6.add(fondo_7);
        fondo_7.setBounds(0, 0, 540, 230);
        jTabbedPane1.addTab("Mapas", jPanel6);
        jPanel5.setLayout(null);
        jLabel19.setText("El staff del Proyecto ROLGps agradece especialmente a:");
        jPanel5.add(jLabel19);
        jLabel19.setBounds(10, 10, 343, 15);
        jLabel20.setText("NGD, por brindarnos este maravilloso juego.");
        jPanel5.add(jLabel20);
        jLabel20.setBounds(10, 60, 277, 15);
        jLabel21.setText("RegnumZG, por proveernos de la base de datos de NPC's.");
        jPanel5.add(jLabel21);
        jLabel21.setBounds(10, 80, 356, 15);
        jLabel22.setText("Atte., Sunos, Arlick y Xephandor (rhfixer)");
        jPanel5.add(jLabel22);
        jLabel22.setBounds(242, 210, 280, 15);
        jLabel23.setText("Jugadores de Regnum Online, que mediante el foro ayudaron al desarrollo.");
        jPanel5.add(jLabel23);
        jLabel23.setBounds(10, 100, 520, 15);
        jLabel24.setText("Betatesters, sin ellos el proyecto todavía sería Beta.");
        jPanel5.add(jLabel24);
        jLabel24.setBounds(10, 120, 470, 15);
        jLabel25.setText("Y a SourceForge, quien nos provee este espacio.");
        jPanel5.add(jLabel25);
        jLabel25.setBounds(10, 140, 298, 15);
        fondo_6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel5.add(fondo_6);
        fondo_6.setBounds(0, 0, 540, 230);
        jTabbedPane1.addTab("Creditos", jPanel5);
        jPanel4.setLayout(null);
        jButton20.setText("Actualzar datos de mapa");
        jButton20.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton20ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton20);
        jButton20.setBounds(190, 120, 190, 25);
        jButton6.setText("Actualizar NPC's");
        jButton6.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton6);
        jButton6.setBounds(20, 80, 150, 25);
        jLabel6.setText("Pulsa para actualizar tu base de datos");
        jPanel4.add(jLabel6);
        jLabel6.setBounds(20, 30, 400, 15);
        conexion.setText("Desconectado...");
        jPanel4.add(conexion);
        conexion.setBounds(20, 200, 510, 15);
        jButton7.setText("Actualizar Quests");
        jButton7.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton7);
        jButton7.setBounds(20, 120, 150, 25);
        jButton8.setText("Actualizar todo");
        jButton8.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton8);
        jButton8.setBounds(190, 80, 190, 25);
        fondo_4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel4.add(fondo_4);
        fondo_4.setBounds(0, 0, 540, 230);
        jTabbedPane1.addTab("Conexion", jPanel4);
        jMenu1.setText("Idioma/Language");
        jMenuItem1.setText("Español");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuItem2.setText("English");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenuBar1.add(jMenu1);
        jMenu2.setText("Opciones");
        jMenuItem7.setText("Actualizar...");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem7);
        jMenu2.add(jSeparator2);
        jMenuItem3.setText("Abrir menu");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem3);
        jMenuBar1.add(jMenu2);
        jMenu3.setText("Ayuda");
        jMenuItem4.setText("Ayuda en Línea");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem4);
        jMenuItem5.setText("Webpage");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem5);
        jMenu3.add(jSeparator1);
        jMenuItem6.setText("Sobre el proyecto");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem6);
        jMenuBar1.add(jMenu3);
        setJMenuBar(jMenuBar1);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE));
        pack();
    }

    private void jRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        this.jRadioButton2.setSelected(false);
        this.jRadioButton3.setSelected(true);
        this.jRadioButton1.setSelected(false);
        this.jList4.setListData(new String[0]);
    }

    private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        this.jRadioButton2.setSelected(true);
        this.jRadioButton3.setSelected(false);
        this.jRadioButton1.setSelected(false);
        this.jList4.setListData(new String[0]);
    }

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        this.jRadioButton2.setSelected(false);
        this.jRadioButton3.setSelected(false);
        this.jRadioButton1.setSelected(true);
        this.jList4.setListData(new String[0]);
        String ciudades_de_ignis[] = { "Altaruk", "Allahed", "Meleketi" };
        this.jList4.setListData(ciudades_de_ignis);
    }

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {
        UpdateThread thread = new UpdateThread();
        thread.start();
    }

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {
        database.setFile(questsFile);
        quests = getQuestsFields();
        database.delete(quests);
        clearQuestsForm();
    }

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {
        database.setFile(questsFile);
        newDataFromQuests = getQuestsFields();
        int indexOfList = jList2.getSelectedIndex();
        quests = database.parseIt(questsResults.get(indexOfList).toString());
        database.modify(quests, newDataFromQuests);
        clearQuestsForm();
    }

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {
        database.setFile(questsFile);
        quests = getQuestsFields();
        database.add(quests);
        clearQuestsForm();
    }

    private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {
        database.setFile(mapFile);
        map = getMapFields();
        database.delete(map);
        clearMapForm();
    }

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {
        database.setFile(mapFile);
        newDataFromMap = getMapFields();
        int indexOfList = jList3.getSelectedIndex();
        map = database.parseIt(mapResults.get(indexOfList).toString());
        database.modify(map, newDataFromMap);
        clearMapForm();
    }

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {
        clearMapForm();
        mapResetButtons();
    }

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {
        database.setFile(mapFile);
        map = getMapFields();
        database.add(map);
        clearMapForm();
    }

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {
        database.setFile(npcsFile);
        newDataFromNpcs = getNpcsFields();
        int indexOfList = jList1.getSelectedIndex();
        npcs = database.parseIt(npcsResults.get(indexOfList).toString());
        npcsIndex.clear();
        npcsResults.clear();
        database.modify(npcs, newDataFromNpcs);
        clearNpcsForm();
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        database.setFile(npcsFile);
        npcs = getNpcsFields();
        npcsIndex.clear();
        npcsResults.clear();
        database.delete(npcs);
        clearNpcsForm();
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        database.setFile(npcsFile);
        npcs = getNpcsFields();
        npcsIndex.clear();
        npcsResults.clear();
        database.add(npcs);
        clearNpcsForm();
    }

    private void jList2MouseClicked(java.awt.event.MouseEvent evt) {
        try {
            database.setFile(questsFile);
            int indexOfList = jList2.getSelectedIndex();
            if (indexOfList == -1) return;
            quests = database.parseIt(questsResults.get(indexOfList).toString());
            showQuests();
            quests = getQuestsFields();
            database.setFile(npcsFile);
            npcsIndex.clear();
            npcsResults.clear();
            npcs[0] = quests[0];
            for (int x = 1; x < npcs.length; x++) {
                npcs[x] = "";
            }
            npcsResults = database.find(npcs);
            for (int x = 0; x < npcsResults.size(); x++) {
                String[] aux = npcsResults.get(x).toString().split(":");
                npcsIndex.addElement(aux[1] + " (NPC: " + aux[0] + ")");
            }
            jList1.setListData(npcsIndex);
        } catch (Exception ex) {
        }
    }

    private void jButton12MouseClicked(java.awt.event.MouseEvent evt) {
        clearQuestsForm();
        questsResetButtons();
    }

    private void jButton10MouseClicked(java.awt.event.MouseEvent evt) {
        questsCanAdd();
        database.setFile(questsFile);
        questsResults.clear();
        questsIndex.clear();
        quests = getQuestsFields();
        questsResults = database.find(quests);
        for (int x = 0; x < questsResults.size(); x++) {
            String[] aux = questsResults.get(x).toString().split(":");
            questsIndex.addElement(aux[0] + " (" + aux[1] + ")");
            questsCanModifyOrDelete();
        }
        jList2.setListData(questsIndex);
    }

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {
        String comando = "konqueror http://rolgps.sourceforge.net";
        int cont = 0;
        int result = 0;
        try {
            if (!conf.getPathBrowser().equals("NULL")) {
                Runtime.getRuntime().exec(conf.getPathBrowser());
            } else if (System.getProperty("os.name").equals("Linux")) {
                do {
                    result = this.lanzarWebBrowserLinux(cont);
                    if (result < 0) cont++;
                } while (result < 0);
            } else if (System.getProperty("os.name").contains("Windows")) {
                do {
                    result = this.lanzarWebBrowserWindows(cont);
                    if (result < 0) cont++;
                } while (result < 0);
            }
        } catch (Exception e) {
            JOptionPane pane = new JOptionPane();
            JOptionPane.showMessageDialog(pane, "Error, no se ha podido acceder a la direcci??n indicada.\nConfigure su explorador en las opciones.");
        }
    }

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {
        String texto = "             ROLGps" + "\n" + "     Versi?n 0.9.5 Beta" + "\n" + "http://rolgps.sourceforge.net";
        JOptionPane.showMessageDialog(new JOptionPane(), texto);
    }

    private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {
        this.updateFile("mapa.txt", "db");
    }

    private void jList3MouseClicked(java.awt.event.MouseEvent evt) {
        database.setFile(mapFile);
        int indexOfList = jList3.getSelectedIndex();
        if (indexOfList == -1) return;
        map = database.parseIt(mapResults.get(indexOfList).toString());
        showMap();
    }

    private void jButton15MouseClicked(java.awt.event.MouseEvent evt) {
        mapCanAdd();
        database.setFile(mapFile);
        mapResults.clear();
        mapIndex.clear();
        map = getMapFields();
        mapResults = database.find(map);
        for (int x = 0; x < mapResults.size(); x++) {
            String[] aux = mapResults.get(x).toString().split(":");
            mapIndex.addElement(aux[0] + " (" + aux[2] + " en " + aux[1] + ")");
            mapCanModifyOrDelete();
        }
        jList3.setListData(mapIndex);
    }

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {
        Opciones.main(null, this);
    }

    private void jTabbedPane1KeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyText(evt.getKeyCode()).equals("NumPad-5")) this.jButton9.setVisible(!this.jButton9.isVisible());
    }

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {
        Mapa.main(null, this);
    }

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.conf.getIdioma().equals("ingles")) this.conexion.setText("Stablising conexion..."); else this.conexion.setText("Estableciendo conexion...");
        this.jButton6ActionPerformed(null);
        this.jButton7ActionPerformed(null);
        this.jButton20ActionPerformed(null);
    }

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.conf.getIdioma().equals("ingles")) this.conexion.setText("Stablising conexion..."); else this.conexion.setText("Estableciendo conexion...");
        updateFile(questsFile, "db");
    }

    private void jTabbedPane1MouseClicked(java.awt.event.MouseEvent evt) {
        if (this.jTabbedPane1.getSelectedIndex() == 0) npcsFile = "npcs.txt";
        if (this.jTabbedPane1.getSelectedIndex() == 1) mapFile = "mapa.txt";
        if (this.jTabbedPane1.getSelectedIndex() == 2) questsFile = "quests.txt";
    }

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.conf.getIdioma().equals("ingles")) this.conexion.setText("Stablising conexion..."); else this.conexion.setText("Estableciendo conexion...");
        updateFile(npcsFile, "db");
    }

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {
        this.jLabel1.setText("Name");
        this.jLabel2.setText("City");
        this.jLabel3.setText("Tipe");
        this.jLabel9.setText("Gold");
        this.jLabel10.setText("Experience");
        this.jLabel12.setText("Description");
        this.jLabel14.setText("Name");
        this.jLabel15.setText("Realm");
        this.jLabel18.setText("Tipe");
        this.jLabel19.setText("The ROLGps staff would like to give special thanks to:");
        this.jLabel20.setText("NGD, which gave us this marvelous game.");
        this.jLabel21.setText("RegnumZG, which gave us the PNC's data base.");
        this.jLabel22.setText("By Sunos, Arlick and Xephandor (rhfixer).");
        this.jLabel23.setText("Regnum Online players, who contributed to the development throught the forum.");
        this.jLabel24.setText("Betatesters, without them the project would still be Beta.");
        this.jLabel25.setText("And SourceForge, providers of this space.");
        this.jButton1.setText("Search");
        this.jButton2.setText("Add");
        this.jButton3.setText("Remove");
        this.jButton4.setText("Modify");
        this.jButton5.setText("Clean");
        this.jButton10.setText("Search");
        this.jButton11.setText("Add");
        this.jButton12.setText("Clean");
        this.jButton13.setText("Modify");
        this.jButton14.setText("Remove");
        this.jButton15.setText("Search");
        this.jButton16.setText("Add");
        this.jButton19.setText("Remove");
        this.jButton18.setText("Modify");
        this.jButton17.setText("Clean");
        this.jTabbedPane1.setTitleAt(0, "NPC's Finder");
        this.jTabbedPane1.setTitleAt(1, "MAP");
        this.jTabbedPane1.setTitleAt(2, "Quests");
        this.jLabel6.setText("Press here to update your data base.");
        this.jButton6.setText("Update NPC's");
        this.jButton7.setText("Update Quests");
        this.jButton8.setText("Update All");
        this.jButton20.setText("Update map spots");
        this.conexion.setText("Disconect...");
        this.jMenu2.setText("Options");
        this.jMenuItem3.setText("Open menu");
        this.jMenu3.setText("Help");
        this.jMenuItem6.setText("About the proyect");
        conf.setIdioma("ingles");
        npcsFile = "npcs_en.txt";
    }

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {
        this.jLabel1.setText("Nombre");
        this.jLabel2.setText("Ciudad");
        this.jLabel3.setText("Tipo");
        this.jLabel9.setText("Oro");
        this.jLabel10.setText("Experiencia");
        this.jLabel12.setText("Descripcion");
        this.jLabel14.setText("Nombre");
        this.jLabel15.setText("Reino");
        this.jLabel18.setText("Tipo");
        this.jLabel19.setText("El staff del Proyecto ROLGps agradece especialmente a:");
        this.jLabel20.setText("NGD, por brindarnos este maravilloso juego.");
        this.jLabel21.setText("RegnumZG, por proveernos de la base de datos de NPC's");
        this.jLabel22.setText("Atte., Sunos, Arlick y Xephandor (rhfixer)");
        this.jLabel23.setText("Jugadores de Regnum Online, que mediante el foro ayudaron al desarrollo.");
        this.jLabel24.setText("Betatesters, sin ellos el proyecto toav??a ser??a Beta.");
        this.jLabel25.setText("Y a, SourceForge, quien nos provee este espacio.");
        this.jButton1.setText("Buscar");
        this.jButton2.setText("Ingresar");
        this.jButton3.setText("Borrar");
        this.jButton4.setText("Modificar");
        this.jButton5.setText("Limpiar");
        this.jButton10.setText("Buscar");
        this.jButton11.setText("Ingresar");
        this.jButton12.setText("Limpiar");
        this.jButton13.setText("Modificar");
        this.jButton14.setText("Borrar");
        this.jButton15.setText("Buscar");
        this.jButton16.setText("Ingresar");
        this.jButton19.setText("Borrar");
        this.jButton18.setText("Modificar");
        this.jButton17.setText("Limpiar");
        this.jTabbedPane1.setTitleAt(0, "NPC's");
        this.jTabbedPane1.setTitleAt(1, "Mapa General");
        this.jTabbedPane1.setTitleAt(2, "Quests");
        this.jLabel6.setText("Pulsa para actualizar tu base de datos.");
        this.jButton6.setText("Actualizar NPC's");
        this.jButton7.setText("Actualizar Quests");
        this.jButton8.setText("Actualizar todo");
        this.conexion.setText("Desconectado...");
        npcsFile = "npcs.txt";
    }

    private void jButton5MouseClicked(java.awt.event.MouseEvent evt) {
        clearNpcsForm();
        npcsResetButtons();
    }

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {
        try {
            database.setFile(npcsFile);
            int indexOfList = jList1.getSelectedIndex();
            if (indexOfList == -1) return;
            npcs = database.parseIt(npcsResults.get(indexOfList).toString());
            showNpcs();
            npcs = getNpcsFields();
            database.setFile(questsFile);
            questsIndex.clear();
            questsResults.clear();
            quests[0] = npcs[0];
            for (int x = 1; x < quests.length; x++) {
                quests[x] = "";
            }
            questsResults = database.find(quests);
            for (int x = 0; x < questsResults.size(); x++) {
                String[] aux = questsResults.get(x).toString().split(":");
                questsIndex.addElement(aux[1] + " (NPC: " + aux[0] + ")");
            }
            jList2.setListData(questsIndex);
        } catch (Exception ex) {
        }
    }

    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {
        npcsCanAdd();
        database.setFile(npcsFile);
        npcsIndex.clear();
        npcsResults.clear();
        npcs = getNpcsFields();
        npcsResults = database.find(npcs);
        for (int x = 0; x < npcsResults.size(); x++) {
            String[] aux = npcsResults.get(x).toString().split(":");
            npcsIndex.addElement(aux[0] + " (" + aux[2] + ")");
            npcsCanModifyOrDelete();
        }
        jList1.setListData(npcsIndex);
    }

    /**
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                GUI_principal gui = new GUI_principal();
                gui.setVisible(true);
                gui.setResizable(false);
            }
        });
    }

    private javax.swing.JLabel conexion;

    public javax.swing.JLabel fondo_2;

    public javax.swing.JLabel fondo_3;

    public javax.swing.JLabel fondo_4;

    public javax.swing.JLabel fondo_5;

    public javax.swing.JLabel fondo_6;

    public javax.swing.JLabel fondo_7;

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton10;

    private javax.swing.JButton jButton11;

    private javax.swing.JButton jButton12;

    private javax.swing.JButton jButton13;

    private javax.swing.JButton jButton14;

    private javax.swing.JButton jButton15;

    private javax.swing.JButton jButton16;

    private javax.swing.JButton jButton17;

    private javax.swing.JButton jButton18;

    private javax.swing.JButton jButton19;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton jButton20;

    private javax.swing.JButton jButton21;

    private javax.swing.JButton jButton22;

    private javax.swing.JButton jButton23;

    private javax.swing.JButton jButton3;

    private javax.swing.JButton jButton4;

    private javax.swing.JButton jButton5;

    private javax.swing.JButton jButton6;

    private javax.swing.JButton jButton7;

    private javax.swing.JButton jButton8;

    private javax.swing.JButton jButton9;

    public javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel10;

    private javax.swing.JLabel jLabel11;

    private javax.swing.JLabel jLabel12;

    private javax.swing.JLabel jLabel14;

    private javax.swing.JLabel jLabel15;

    private javax.swing.JLabel jLabel16;

    private javax.swing.JLabel jLabel17;

    private javax.swing.JLabel jLabel18;

    private javax.swing.JLabel jLabel19;

    public javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel20;

    private javax.swing.JLabel jLabel21;

    private javax.swing.JLabel jLabel22;

    private javax.swing.JLabel jLabel23;

    private javax.swing.JLabel jLabel24;

    private javax.swing.JLabel jLabel25;

    public javax.swing.JLabel jLabel3;

    public javax.swing.JLabel jLabel4;

    public javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JLabel jLabel9;

    private javax.swing.JList jList1;

    private javax.swing.JList jList2;

    private javax.swing.JList jList3;

    private javax.swing.JList jList4;

    private javax.swing.JMenu jMenu1;

    private javax.swing.JMenu jMenu2;

    private javax.swing.JMenu jMenu3;

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JMenuItem jMenuItem1;

    private javax.swing.JMenuItem jMenuItem2;

    private javax.swing.JMenuItem jMenuItem3;

    private javax.swing.JMenuItem jMenuItem4;

    private javax.swing.JMenuItem jMenuItem5;

    private javax.swing.JMenuItem jMenuItem6;

    private javax.swing.JMenuItem jMenuItem7;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JPanel jPanel6;

    private javax.swing.JRadioButton jRadioButton1;

    private javax.swing.JRadioButton jRadioButton2;

    private javax.swing.JRadioButton jRadioButton3;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JScrollPane jScrollPane4;

    private javax.swing.JScrollPane jScrollPane5;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JSeparator jSeparator2;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JTextArea jTextArea1;

    private javax.swing.JTextField jTextField1;

    private javax.swing.JTextField jTextField10;

    private javax.swing.JTextField jTextField11;

    private javax.swing.JTextField jTextField12;

    private javax.swing.JTextField jTextField13;

    private javax.swing.JTextField jTextField14;

    private javax.swing.JTextField jTextField15;

    private javax.swing.JTextField jTextField2;

    private javax.swing.JTextField jTextField3;

    private javax.swing.JTextField jTextField4;

    private javax.swing.JTextField jTextField5;

    private javax.swing.JTextField jTextField6;

    private javax.swing.JTextField jTextField7;

    private javax.swing.JTextField jTextField8;

    private javax.swing.JTextField jTextField9;

    class Mi_hilo extends Thread {

        private String file_name;

        private String directory;

        public void setFileName(String fn) {
            file_name = fn;
        }

        public void setDirectory(String fn) {
            directory = fn;
        }

        public void run() {
            flag_download_finish = false;
            if (directory == null) {
                System.out.println("ERROR: no se ha indicado el directorio a conectarse.");
                System.out.println("ERROR: directory not specified.");
                System.out.println("Contactar con los desarrolladores / Contact with the developers.");
                System.exit(0);
            }
            flag_download_finish = false;
            if (file_name != null) {
                java.io.BufferedReader br = null;
                try {
                    java.net.URL url = new java.net.URL("http", "rolgps.sourceforge.net", 80, "/" + directory + "/" + file_name);
                    java.net.URLConnection uconect = url.openConnection();
                    conexion.setText("Conectando...");
                    conexion.repaint();
                    InputStream is = uconect.getInputStream();
                    byte b[] = new byte[1];
                    java.io.BufferedInputStream bis = new java.io.BufferedInputStream(is);
                    int n = 0;
                    java.io.File f_viejo = new java.io.File(file_name);
                    f_viejo.delete();
                    java.io.FileOutputStream fo = new java.io.FileOutputStream(new File(file_name));
                    while ((n = bis.read(b, 0, 1)) >= 0) {
                        fo.write(b);
                    }
                    fo.close();
                    is.close();
                    bis.close();
                    conexion.setText("Descarga completada...");
                } catch (java.io.FileNotFoundException fnfex) {
                    conexion.setText("No se ha encontrado en la base de datos del servidor el fichero " + file_name);
                } catch (Exception e) {
                    conexion.setText("Ha habido un problema con la descarga.");
                    System.out.println(e);
                }
            }
            flag_download_finish = true;
        }
    }

    class UpdateThread extends Thread {

        public void run() {
            updateFile(md5_file, "db");
            while (!flag_download_finish) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
            try {
                System.out.println("empezando");
                byte b[] = readFile("rolgps.jar", "r");
                String md5_local = calculateMD5(b);
                String md5_importado = readFile("rolgps.jar.md5");
                System.out.println("BUENO: " + md5_local);
                System.out.println("IMPOR: " + md5_importado);
                if (md5_local.toLowerCase().equals(md5_importado.toLowerCase())) {
                    javax.swing.JOptionPane.showMessageDialog(null, "Tienes la ultima version,\nno debes preocuparte.");
                } else {
                    int option = javax.swing.JOptionPane.showConfirmDialog(null, "No est? actualizado.\nDesea bajar la nueva versi?n?", null, javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    System.out.println(option);
                    if (option == 0) {
                        File file = new File("rolgps.jar");
                        file.delete();
                        updateFile("rolgps.jar", "lastest_rolgps");
                        while (!flag_download_finish) {
                            Thread.sleep(200);
                        }
                        javax.swing.JOptionPane.showMessageDialog(null, "el programa debe reiniciarse.");
                        java.lang.Runtime.getRuntime().exec("java -jar rolgps.jar");
                        System.exit(0);
                    }
                }
            } catch (Exception e) {
            }
        }
    }
}
