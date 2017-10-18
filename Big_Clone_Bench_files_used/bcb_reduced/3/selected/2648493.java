package org.hourglassstudios.tempuspre.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import org.hourglassstudios.tempuspre.library.swing.BackgroundPane;
import org.hourglassstudios.tempuspre.library.files.ApplicationPath;
import org.hourglassstudios.tempuspre.library.swing.centerWindow;
import org.hourglassstudios.tempuspre.library.net.HttpConnectionFactory;

public class LauncherGUI extends javax.swing.JFrame {

    protected final String PATH = ApplicationPath.getApplicationPath("TempusPreLauncher");

    public LauncherGUI() {
        initComponents();
        this.setLocation(centerWindow.centerWindowLeft(616), centerWindow.centerWindowTop(444));
        try {
            jTextPane1.setPage("http://tempuspre.sourceforge.net/launcher/news.html");
        } catch (Exception e) {
            jTextPane1.setText("Es ist ein Fehler beim Holen der News aufgetreten. \r\n " + e + "\r\n");
        }
    }

    private void initComponents() {
        jPanel1 = new BackgroundPane(PATH + "images/launcher.png");
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jProgressBar2 = new javax.swing.JProgressBar();
        jProgressBar1 = new javax.swing.JProgressBar();
        jButton4 = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(616, 444));
        setResizable(false);
        setUndecorated(true);
        jLabel1.setFont(new java.awt.Font("DejaVu Sans", 1, 16));
        jLabel1.setForeground(new java.awt.Color(254, 254, 254));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("TempusPre Launcher");
        jScrollPane1.setBorder(null);
        jTextPane1.setBackground(new java.awt.Color(0, 0, 0));
        jTextPane1.setBorder(null);
        jTextPane1.setEditable(false);
        jTextPane1.setForeground(new java.awt.Color(254, 254, 254));
        jScrollPane1.setViewportView(jTextPane1);
        jScrollPane2.setBorder(null);
        jTextPane2.setBackground(new java.awt.Color(0, 0, 0));
        jTextPane2.setBorder(null);
        jTextPane2.setEditable(false);
        jTextPane2.setForeground(new java.awt.Color(254, 254, 254));
        jScrollPane2.setViewportView(jTextPane2);
        jButton1.setText("Start");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jButton2.setText("Beenden");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jButton3.setText("Über");
        jButton3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jLabel2.setForeground(new java.awt.Color(254, 254, 254));
        jLabel2.setText("Gesamt:");
        jLabel3.setForeground(new java.awt.Color(254, 254, 254));
        jLabel3.setText("Datei:");
        jProgressBar2.setStringPainted(true);
        jProgressBar1.setStringPainted(true);
        jButton4.setText("Update");
        jButton4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel2).addComponent(jLabel3)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jProgressBar2, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE).addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE))).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jButton1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jButton4).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jButton2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jButton3))).addContainerGap()).addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 604, Short.MAX_VALUE))));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addContainerGap().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel3)).addGap(10, 10, 10).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jProgressBar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel2)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jButton3).addComponent(jButton2).addComponent(jButton4).addComponent(jButton1))).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)).addContainerGap()));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        pack();
    }

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Thread t = new Thread(new Download(this));
            t.start();
        } catch (Exception ex) {
            jTextPane2.setText(jTextPane2.getText() + "Fehler: \r\n" + ex + "\r\n");
            ex.printStackTrace();
        }
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        AboutBox aboutbox = new AboutBox();
        aboutbox.setVisible(true);
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        LoginWindow loginwindow = new LoginWindow();
        loginwindow.setVisible(true);
        this.setVisible(false);
    }

    public javax.swing.JButton getJButton1() {
        return jButton1;
    }

    public javax.swing.JButton getJButton2() {
        return jButton2;
    }

    public javax.swing.JButton getJButton3() {
        return jButton3;
    }

    public javax.swing.JButton getJButton4() {
        return jButton4;
    }

    public javax.swing.JProgressBar getJProgressBar1() {
        return jProgressBar1;
    }

    public javax.swing.JProgressBar getJProgressBar2() {
        return jProgressBar2;
    }

    public javax.swing.JTextPane getJTextPane1() {
        return jTextPane1;
    }

    public javax.swing.JTextPane getJTextPane2() {
        return jTextPane2;
    }

    public void disableAllButtons() {
        jButton1.setEnabled(false);
        jButton1.setFocusable(false);
        jButton3.setEnabled(false);
        jButton3.setFocusable(false);
        jButton4.setEnabled(false);
        jButton4.setFocusable(false);
    }

    public void enableAllButtons() {
        jButton1.setEnabled(true);
        jButton1.setFocusable(true);
        jButton3.setEnabled(true);
        jButton3.setFocusable(true);
        jButton4.setEnabled(true);
        jButton4.setFocusable(true);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new LauncherGUI().setVisible(true);
            }
        });
    }

    public javax.swing.JButton jButton1;

    public javax.swing.JButton jButton2;

    public javax.swing.JButton jButton3;

    public javax.swing.JButton jButton4;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JPanel jPanel1;

    public javax.swing.JProgressBar jProgressBar1;

    public javax.swing.JProgressBar jProgressBar2;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    public javax.swing.JTextPane jTextPane1;

    public javax.swing.JTextPane jTextPane2;
}

class Download implements Runnable {

    protected final String PATH = ApplicationPath.getApplicationPath("TempusPreLauncher");

    protected LauncherGUI ui;

    public Download(LauncherGUI ui) {
        this.ui = ui;
    }

    public void run() {
        String filename = "update/files.lst";
        String s;
        Integer i = 0;
        Integer total = 0;
        Integer length = -1;
        Float done = (float) 0.0;
        Float totallength = (float) 0.0;
        Float completed = (float) 0;
        Float speed, percent, percent2 = (float) 0.0;
        Long start, now, diff;
        URL url;
        HttpURLConnection conn;
        InputStream sin;
        OutputStream fout;
        File updatePath;
        byte[] buffer = new byte[4096];
        ui.disableAllButtons();
        ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "Hole Fileliste... ");
        try {
            url = new URL("http://tempuspre.sourceforge.net/updates/" + filename);
            conn = HttpConnectionFactory.createConnection(url);
            conn.connect();
            sin = conn.getInputStream();
            updatePath = new File(PATH + "update");
            if (!updatePath.exists()) {
                updatePath.mkdirs();
            }
            fout = new FileOutputStream(new File(PATH + "/" + filename));
            total = conn.getContentLength();
            start = System.currentTimeMillis();
            now = (long) 0;
            diff = (long) 0;
            length = sin.read(buffer, 0, buffer.length);
            while (length != -1) {
                fout.write(buffer, 0, length);
                completed += (float) length;
                percent = completed / total * 100;
                now = System.currentTimeMillis();
                diff = now - start;
                speed = completed / diff;
                speed *= 1000;
                speed /= 1024.0f;
                speed = (((float) Math.round(speed * 10)) / 10);
                ui.getJProgressBar1().setValue(Math.round(percent));
                ui.getJProgressBar2().setValue(Math.round(percent));
                ui.getJProgressBar1().setString("" + ((float) Math.round(percent * 10)) / 10 + "% - " + speed + "kb/s");
                ui.getJProgressBar2().setString("" + ((float) Math.round(percent * 10)) / 10 + "%");
                ui.getJProgressBar1().setStringPainted(true);
                ui.getJProgressBar2().setStringPainted(true);
                ui.getJProgressBar1().paint(ui.getJProgressBar1().getGraphics());
                ui.getJProgressBar2().paint(ui.getJProgressBar2().getGraphics());
                length = sin.read(buffer, 0, buffer.length);
            }
            fout.flush();
            fout.close();
            ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "ok \r\n");
            ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "Prüfe auf Updates... ");
            totallength = (float) 0.0;
            FileWriter fw = new FileWriter(new File(PATH + "update/updates.lst"));
            BufferedReader fin = new BufferedReader(new FileReader(PATH + "update/files.lst"));
            while ((s = fin.readLine()) != null) {
                String[] arr = s.split("\\|");
                File updateFile = new File(PATH + arr[0]);
                if (updateFile.exists() == true) {
                    MessageDigest messagedigest = MessageDigest.getInstance("md5");
                    DigestInputStream dis = new DigestInputStream(new BufferedInputStream(new FileInputStream(PATH + arr[0])), messagedigest);
                    StringBuffer hash = new StringBuffer();
                    byte md[] = new byte[8192];
                    int n = 0;
                    while ((n = dis.read(md)) > -1) {
                        messagedigest.update(md, 0, n);
                    }
                    byte[] digest = messagedigest.digest();
                    for (int j = 0; j < digest.length; j++) {
                        hash.append(Integer.toHexString(0xff & digest[j]));
                    }
                    String checksum = hash.toString();
                    if (!checksum.equals(arr[2])) {
                        fw.write(arr[0] + "\r\n");
                        totallength += Float.valueOf(arr[1]);
                    }
                } else {
                    fw.write(arr[0] + "\r\n");
                    totallength += Float.valueOf(arr[1]);
                }
                i++;
            }
            fw.close();
            fin.close();
            if (totallength != 0.0) {
                ui.getJProgressBar2().setValue(0);
                ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "ok \r\n");
                ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "Hole Updates... \r\n");
                s = "";
                BufferedReader in = new BufferedReader(new FileReader(PATH + "update/updates.lst"));
                while ((s = in.readLine()) != null) {
                    ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "Hole " + s + " ");
                    url = new URL("http://tempuspre.sourceforge.net/updates/" + s);
                    conn = HttpConnectionFactory.createConnection(url);
                    conn.connect();
                    sin = conn.getInputStream();
                    fout = new FileOutputStream(new File(PATH + "/" + s));
                    total = conn.getContentLength();
                    completed = (float) 0.0;
                    done = (float) ui.getJProgressBar2().getValue();
                    length = -1;
                    start = System.currentTimeMillis();
                    now = (long) 0;
                    diff = (long) 0;
                    speed = (float) 0.0;
                    percent = (float) 0.0;
                    percent2 = (float) 0.0;
                    length = sin.read(buffer, 0, buffer.length);
                    while (length != -1) {
                        fout.write(buffer, 0, length);
                        completed += (float) length;
                        percent = completed / total * 100;
                        now = System.currentTimeMillis();
                        diff = now - start;
                        speed = completed / diff;
                        speed *= 1000;
                        speed /= 1024.0f;
                        speed = (((float) Math.round(speed * 10)) / 10);
                        ui.getJProgressBar1().setValue(Math.round(percent));
                        ui.getJProgressBar1().setString("" + ((float) Math.round(percent * 10)) / 10 + "% - " + speed + "kb/s");
                        ui.getJProgressBar1().setStringPainted(true);
                        ui.getJProgressBar1().paint(ui.getJProgressBar1().getGraphics());
                        if (totallength != 0.0) {
                            percent2 = done + (completed / totallength * 100);
                        }
                        ui.getJProgressBar2().setValue(Math.round(percent2));
                        ui.getJProgressBar2().setString("" + ((float) Math.round(percent2 * 10)) / 10 + "%");
                        ui.getJProgressBar2().setStringPainted(true);
                        ui.getJProgressBar2().paint(ui.getJProgressBar2().getGraphics());
                        length = sin.read(buffer, 0, buffer.length);
                    }
                    fout.flush();
                    fout.close();
                    ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "ok \r\n");
                }
                ui.getJProgressBar1().setValue(ui.getJProgressBar1().getMaximum());
                ui.getJProgressBar1().setString("100%");
                ui.getJProgressBar1().setStringPainted(true);
                ui.getJProgressBar1().paint(ui.getJProgressBar1().getGraphics());
                ui.getJProgressBar2().setValue(ui.getJProgressBar2().getMaximum());
                ui.getJProgressBar2().setString("100%");
                ui.getJProgressBar2().setStringPainted(true);
                ui.getJProgressBar2().paint(ui.getJProgressBar2().getGraphics());
                ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "Updates fertig. \r\n");
            } else {
                ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "ok \r\nKeine neuen Updates vorhanden. \r\n");
            }
        } catch (Exception e) {
            ui.getJTextPane2().setText(ui.getJTextPane2().getText() + "Fehler: \r\n" + e + "\r\n");
            e.printStackTrace();
        }
        ui.enableAllButtons();
    }
}
