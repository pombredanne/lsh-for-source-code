package com.jtstand.gui;

import com.jtstand.Authentication;

/**
 *
 * @author  albert_kurucz
 */
public class Login extends javax.swing.JDialog {

    private Authentication at;

    /** Creates new form GTLogin */
    public Login(java.awt.Frame parent, boolean modal, Authentication at) {
        super(parent, modal);
        this.at = at;
        initComponents();
        jPasswordField.setEnabled(at.isPassword());
        jTextFieldUser.requestFocus();
        CountDownLogin cd = new CountDownLogin(this, 60);
        this.setVisible(true);
    }

    private void initComponents() {
        jPanel2 = new javax.swing.JPanel();
        jPanelUser = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldUser = new javax.swing.JTextField();
        jPanelPassword = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPasswordField = new javax.swing.JPasswordField();
        jPanelButtons = new javax.swing.JPanel();
        jButtonLogin = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Login");
        setResizable(false);
        addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                formFocusGained(evt);
            }
        });
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {

            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                formWindowGainedFocus(evt);
            }

            public void windowLostFocus(java.awt.event.WindowEvent evt) {
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));
        getContentPane().add(jPanel2);
        jPanelUser.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/jtstand/gui/Bundle");
        jLabel1.setText(bundle.getString("Username_or_Emp#:"));
        jPanelUser.add(jLabel1);
        jTextFieldUser.setPreferredSize(new java.awt.Dimension(200, 20));
        jTextFieldUser.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldUserActionPerformed(evt);
            }
        });
        jPanelUser.add(jTextFieldUser);
        getContentPane().add(jPanelUser);
        jPanelPassword.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        jLabel2.setText(bundle.getString("Password:"));
        jPanelPassword.add(jLabel2);
        jPasswordField.setPreferredSize(new java.awt.Dimension(200, 20));
        jPasswordField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPasswordFieldActionPerformed(evt);
            }
        });
        jPanelPassword.add(jPasswordField);
        getContentPane().add(jPanelPassword);
        jPanelButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 5));
        jButtonLogin.setText(bundle.getString("Login"));
        jButtonLogin.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoginActionPerformed(evt);
            }
        });
        jPanelButtons.add(jButtonLogin);
        jButtonCancel.setText(bundle.getString("Cancel"));
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });
        jPanelButtons.add(jButtonCancel);
        getContentPane().add(jPanelButtons);
        pack();
    }

    private void jButtonLoginActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            jTextFieldUser.setText(jTextFieldUser.getText().trim());
            if (jTextFieldUser.getText().length() == 0) {
                return;
            }
            at.login(jTextFieldUser.getText(), String.valueOf(jPasswordField.getPassword()));
            this.dispose();
        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
        }
        jTextFieldUser.requestFocus();
    }

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {
        Util.centerOnParent(this);
    }

    private void formFocusGained(java.awt.event.FocusEvent evt) {
    }

    private void jPasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {
        jButtonLogin.requestFocus();
        jButtonLogin.doClick();
    }

    private void jTextFieldUserActionPerformed(java.awt.event.ActionEvent evt) {
        jPasswordField.setText("");
        jPasswordField.requestFocus();
    }

    public static String encryptString(String x) throws Exception {
        return byteArrayToHexString(encrypt(x));
    }

    public static byte[] encrypt(String x) throws Exception {
        java.security.MessageDigest d = null;
        d = java.security.MessageDigest.getInstance("SHA-1");
        d.reset();
        d.update(x.getBytes());
        return d.digest();
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

    private javax.swing.JButton jButtonCancel;

    private javax.swing.JButton jButtonLogin;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanelButtons;

    private javax.swing.JPanel jPanelPassword;

    private javax.swing.JPanel jPanelUser;

    private javax.swing.JPasswordField jPasswordField;

    private javax.swing.JTextField jTextFieldUser;

    public void tick(int i) {
        jButtonCancel.setText("Cancel " + i);
    }

    public void cancel() {
        jButtonCancel.doClick();
    }
}

class CountDownLogin extends Thread {

    Login gtn = null;

    int cnt = 0;

    public CountDownLogin(Login gtn, int cnt) {
        this.gtn = gtn;
        this.cnt = cnt;
        this.start();
    }

    @Override
    public void run() {
        try {
            while (cnt != 0) {
                gtn.tick(cnt);
                sleep(1000);
                cnt--;
            }
            gtn.tick(0);
            sleep(200);
            gtn.cancel();
        } catch (InterruptedException ex) {
        }
    }
}
