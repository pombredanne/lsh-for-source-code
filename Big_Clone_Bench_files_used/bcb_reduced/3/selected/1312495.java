package masc.View;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.NoSuchAlgorithmException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import mas.common.AuthenticationException;
import mas.common.Digest;
import mas.common.LogicFactory;
import mas.common.Login;

public class LoginDialog extends JFrame implements ActionListener {

    private static final long serialVersionUID = 730002584680199564L;

    JTextField userfield;

    JPasswordField passwordfield;

    JButton okbutton, cancelbutton;

    Login login;

    public LoginDialog(Login login) {
        JLabel userlabel, passwordlabel, picturelabel;
        Insets insets = new Insets(2, 4, 2, 4);
        GridBagConstraints c;
        JPanel p = new JPanel();
        this.login = login;
        setTitle("Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(new ImageIcon(this.getClass().getResource("/Icon.png")).getImage());
        setLayout(new GridBagLayout());
        setResizable(false);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        picturelabel = new JLabel(new ImageIcon(this.getClass().getResource("/Logo.png")));
        add(picturelabel, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        userlabel = new JLabel("BenutzerIn:");
        add(userlabel, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        userfield = new JTextField();
        userfield.addActionListener(this);
        add(userfield, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.insets = insets;
        c.anchor = GridBagConstraints.WEST;
        passwordlabel = new JLabel("Passswort:");
        add(passwordlabel, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        c.anchor = GridBagConstraints.NORTHWEST;
        passwordfield = new JPasswordField();
        passwordfield.addActionListener(this);
        add(passwordfield, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.insets = insets;
        c.anchor = GridBagConstraints.SOUTHEAST;
        okbutton = new JButton("OK");
        okbutton.addActionListener(this);
        cancelbutton = new JButton("Abbrechen");
        cancelbutton.addActionListener(this);
        p.add(okbutton);
        p.add(cancelbutton);
        add(p, c);
        pack();
        Toolkit kit = getToolkit();
        Dimension d = kit.getScreenSize();
        setLocation((int) (d.getWidth() - getWidth()) / 2, (int) (d.getHeight() - getHeight()) / 2);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        LogicFactory logicfactory = null;
        System.err.print("Anmelden: ");
        if (arg0.getSource() == cancelbutton) {
            System.exit(0);
        }
        try {
            logicfactory = login.authenticate(userfield.getText(), Digest.digest(new String(passwordfield.getPassword()).getBytes()));
        } catch (AuthenticationException e) {
            passwordfield.setText("");
            JOptionPane.showMessageDialog(this, "BenutzerInnenname und/oder Passwort falsch!", "Fehler", JOptionPane.ERROR_MESSAGE);
            System.err.println("Wrong password");
            return;
        } catch (NoSuchAlgorithmException e) {
            JOptionPane.showMessageDialog(this, "Kann Passwort nicht verschlüsseln. MAS-C wird beendet.", "Kritischer Fehler", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        System.err.println("[OK]");
        this.setVisible(false);
        this.dispose();
        Init.initGUI(logicfactory.getMeetingLogic(), logicfactory.getUserLogic());
    }
}
