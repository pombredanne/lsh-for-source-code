package masc.View;

import java.awt.Dimension;
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

public class NewUserDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -6400063432751318479L;

    JTextField email, forename, surname, username;

    JPasswordField newpw, newpw2;

    JButton okbutton, cancelbutton;

    UserLogic userlogic;

    public NewUserDialog(UserLogic userlogic, JFrame parent) {
        super(parent, true);
        this.userlogic = userlogic;
        Insets insets = new Insets(2, 4, 2, 4);
        GridBagConstraints c;
        JPanel p = new JPanel();
        this.setTitle("neueR BenutzerIn");
        this.setIconImage(new ImageIcon(this.getClass().getResource("/Icon.png")).getImage());
        this.setLayout(new GridBagLayout());
        this.setResizable(false);
        this.setMinimumSize(new Dimension(512, 0));
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = insets;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(new JLabel("Mit diesem Dialog können Sie eineN neueN BenutzerIn anlegen."), c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(new JLabel("BenutzerInnenname:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        username = new JTextField();
        this.add(username, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(new JLabel("Vorname:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        forename = new JTextField();
        this.add(forename, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(new JLabel("Nachname:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        surname = new JTextField();
        this.add(surname, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        this.add(new JLabel("E-Mail-Adresse:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        email = new JTextField();
        this.add(email, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        this.add(new JLabel("Passswort:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        newpw = new JPasswordField();
        this.add(newpw, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        this.add(new JLabel("Passwort bestätigen:"), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 6;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        newpw2 = new JPasswordField();
        this.add(newpw2, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 7;
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
        if (e.getSource() == cancelbutton) {
            this.setVisible(false);
            this.dispose();
            return;
        }
        try {
            if (!(username.getText().length() == 0 || forename.getText().length() == 0 || surname.getText().length() == 0)) {
                if (newpw.getPassword().length != 0 && newpw2.getPassword().length != 0) {
                    if (Arrays.equals(newpw.getPassword(), newpw2.getPassword())) {
                        String password = Digest.digest(new String(newpw.getPassword()).getBytes());
                        if (!userlogic.addUser(username.getText(), forename.getText(), surname.getText(), password, email.getText())) {
                            JOptionPane.showMessageDialog(this, "BenutzerIn konnte nicht angelegt werden. Bitte versuchen sie es mit einem anderen BenutzerInnennamen erneut.");
                            return;
                        }
                        JOptionPane.showMessageDialog(this, "NeueR BenutzerIn angelegt. Sie können sich jetzt mit der eingegebenen Kombination aus BenutzerInnnenname und Passwort anmelden.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Die Passwörter stimmen nicht überein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                        newpw.setText("");
                        newpw2.setText("");
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Das Passwort darf nicht leer sein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(this, "Die Felder BenutzerInnenname, Vorname und Nachname müssen ausgefüllt sein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NoSuchAlgorithmException e1) {
            JOptionPane.showMessageDialog(this, "Konnte keineN neueN BenutzerIn anlegen.", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
        this.setVisible(false);
        this.dispose();
    }
}
