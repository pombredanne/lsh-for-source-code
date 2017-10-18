package de.outofbounds.license;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author Dr. Andreas Rau
 */
public class LicenseManager {

    public static void showRegistrationDialog(final JFrame parent, final String applicationId) {
        if (License.getCurrent().getOwner() == null) {
            final JDialog dialog = new JDialog(parent, "", true);
            JPanel panel = new JPanel();
            JLabel emailLabel = new JLabel("EMail:");
            final JTextField emailField = new JTextField(20);
            JLabel keyLabel = new JLabel("Schl�ssel:");
            final JTextField keyField = new JTextField(20);
            JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        try {
                            createLicense(applicationId, emailField.getText(), keyField.getText());
                            dialog.dispose();
                        } catch (LicenseException ex) {
                            JOptionPane.showMessageDialog(parent, "Ung�ltiger Schl�ssel!", "Fehler", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(parent, "Lizenz konnte nicht gespeichert werden", "Fehler", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            });
            JButton cancelButton = new JButton("Abbrechen");
            cancelButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            GroupLayout layout = new GroupLayout(panel);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);
            GroupLayout.ParallelGroup hgrp = layout.createParallelGroup(Alignment.CENTER);
            GroupLayout.SequentialGroup dgrp = layout.createSequentialGroup();
            dgrp.addGroup(layout.createParallelGroup().addComponent(emailLabel).addComponent(keyLabel));
            dgrp.addGroup(layout.createParallelGroup().addComponent(emailField).addComponent(keyField));
            GroupLayout.SequentialGroup bgrp = layout.createSequentialGroup();
            bgrp.addComponent(okButton).addComponent(cancelButton);
            hgrp.addGroup(dgrp).addGroup(bgrp);
            layout.setHorizontalGroup(hgrp);
            GroupLayout.SequentialGroup vgrp = layout.createSequentialGroup();
            vgrp.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(emailLabel).addComponent(emailField));
            vgrp.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(keyLabel).addComponent(keyField));
            vgrp.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(okButton).addComponent(cancelButton));
            layout.setVerticalGroup(vgrp);
            panel.setLayout(layout);
            dialog.add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(parent, "Die Software ist bereits registriert!");
        }
    }

    public static void createTimeLimitedLicense(int day, int month, int year) {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.YEAR, year);
        Date expiration = cal.getTime();
        License.current = new License(expiration);
    }

    public static void createEvaluationLicense(int days) throws LicenseException, IOException {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_YEAR, days);
        Date expiration = cal.getTime();
        License license = new License(expiration);
        LicenseFile.writeLicense(License.current);
        License.current = license;
    }

    public static void createLicense(String applicationId, String owner, String key) throws LicenseException, IOException {
        String yek = computeKey(applicationId, owner);
        if (yek.equals(key)) {
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, 2099);
            cal.set(Calendar.MONTH, 11);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            License license = new License(cal.getTime(), owner, key);
            LicenseFile.writeLicense(license);
            License.current = license;
        } else {
            throw new LicenseException("Invalid licence key");
        }
    }

    public static void installLicense(String applicationId) throws LicenseException, IOException {
        License license = LicenseFile.readLicense();
        if (license.isRegistered()) {
            String yuk = computeKey(applicationId, license.owner);
            if (!yuk.equals(license.key)) {
                throw new LicenseException("Invalid license key");
            }
        }
        License.current = license;
    }

    private static String computeKey(String applicationId, String owner) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest1 = md.digest(applicationId.getBytes());
            byte[] digest2 = md.digest(owner.getBytes());
            int[] license = new int[12];
            for (int i = 0; i < license.length; ++i) {
                license[i] = digest1[i] ^ digest2[i];
            }
            return bytesToHexString(license);
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    private static String bytesToHexString(int[] bytes) {
        char[] tokens = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            int b = bytes[i];
            sb.append(tokens[(b / 16) & 0x0f]);
            sb.append(tokens[(b % 16) & 0x0f]);
            if (i % 2 == 1 && i + 1 < bytes.length) sb.append("-");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("License Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel applicationLabel = new JLabel("Application");
        JLabel emailLabel = new JLabel("EMail");
        JLabel keyLabel = new JLabel("Key");
        final JTextField applicationField = new JTextField(20);
        final JTextField emailField = new JTextField(20);
        final JTextField keyField = new JTextField(20);
        keyField.setEditable(false);
        JButton button = new JButton("Generate Key");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                keyField.setText(LicenseManager.computeKey(applicationField.getText(), emailField.getText()));
            }
        });
        JPanel panel = new JPanel();
        GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        GroupLayout.ParallelGroup hgrp = layout.createParallelGroup(Alignment.CENTER);
        GroupLayout.SequentialGroup dgrp = layout.createSequentialGroup();
        dgrp.addGroup(layout.createParallelGroup().addComponent(applicationLabel).addComponent(emailLabel).addComponent(keyLabel));
        dgrp.addGroup(layout.createParallelGroup().addComponent(applicationField).addComponent(emailField).addComponent(keyField));
        hgrp.addGroup(dgrp);
        hgrp.addComponent(button);
        layout.setHorizontalGroup(hgrp);
        GroupLayout.SequentialGroup vgrp = layout.createSequentialGroup();
        vgrp.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(applicationLabel).addComponent(applicationField));
        vgrp.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(emailLabel).addComponent(emailField));
        vgrp.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(keyLabel).addComponent(keyField));
        vgrp.addComponent(button);
        layout.setVerticalGroup(vgrp);
        panel.setLayout(layout);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
