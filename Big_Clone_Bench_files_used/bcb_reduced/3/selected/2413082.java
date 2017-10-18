package masc.View;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import mas.common.Digest;
import mas.common.UserLogic;

public class CredentialsDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -2677095288058463162L;

    JTextField email;

    JPasswordField oldpw, newpw, newpw2;

    JButton okbutton, cancelbutton;

    JLabel userlabel;

    UserLogic userlogic;

    public CredentialsDialog(UserLogic userlogic, JFrame parent) {
        super(parent, true);
        this.userlogic = userlogic;
        Insets insets = new Insets(2, 4, 2, 4);
        GridBagConstraints c;
        JPanel p = new JPanel();
        this.setTitle("Passwort / E-mail ändern");
        this.setIconImage(new ImageIcon(this.getClass().getResource("/Icon.png")).getImage());
        this.setLayout(new GridBagLayout());
        this.setResizable(false);
        this.setMinimumSize(new Dimension(512, 0));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHEAST;
        userlabel = new JLabel(userlogic.getUsername());
        userlabel.setFont(userlabel.getFont().deriveFont(Font.BOLD, 18));
        this.add(userlabel, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(new JLabel("E-Mail:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        email = new JTextField();
        email.setText(userlogic.getMailAddress());
        this.add(email, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        this.add(new JLabel("aktuelles Passswort:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        oldpw = new JPasswordField();
        this.add(oldpw, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        this.add(new JLabel("neues Passswort:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        newpw = new JPasswordField();
        this.add(newpw, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        this.add(new JLabel("neues Passwort bestätigen:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        newpw2 = new JPasswordField();
        this.add(newpw2, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        c.insets = insets;
        c.anchor = GridBagConstraints.SOUTHEAST;
        okbutton = new JButton("OK");
        okbutton.addActionListener(this);
        cancelbutton = new JButton("Abbrechen");
        cancelbutton.addActionListener(this);
        p.add(okbutton);
        p.add(cancelbutton);
        this.add(p, c);
        this.pack();
        Dimension parentSize = parent.getSize();
        setLocation((int) (parent.getX() + parentSize.getWidth() / 2 - this.getWidth() / 2), (int) (parent.getY() + parentSize.getHeight() / 2 - this.getHeight() / 2));
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.err.println("button clicked: " + e.getSource());
        if (e.getSource() == cancelbutton) {
            this.setVisible(false);
            this.dispose();
            return;
        }
        if (!email.getText().equals(userlogic.getMailAddress())) {
            userlogic.setEmail(email.getText());
        }
        try {
            if (oldpw.getPassword().length != 0) {
                if (newpw.getPassword().length != 0 && newpw2.getPassword().length != 0) {
                    if (Arrays.equals(newpw.getPassword(), newpw2.getPassword())) {
                        String oldPassword = Digest.digest(new String(oldpw.getPassword()).getBytes());
                        String newPassword = Digest.digest(new String(newpw.getPassword()).getBytes());
                        if (userlogic.setPassword(oldPassword, newPassword)) {
                            JOptionPane.showMessageDialog(this, "Das Passwort wurde erfolgreich geändert.");
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Die Passwörter stimmen nicht überein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                        newpw.setText("");
                        newpw2.setText("");
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Das neue Passwort darf nicht leer sein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        } catch (NoSuchAlgorithmException e1) {
            JOptionPane.showMessageDialog(this, "Das Passwort konnte nicht geändert werden.", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
        this.setVisible(false);
        this.dispose();
    }
}
