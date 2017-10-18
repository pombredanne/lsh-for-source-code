package org.jdmp.sigmen.client.login;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import org.jdmp.sigmen.client.Alertes;
import org.jdmp.sigmen.client.Fenetre;
import org.jdmp.sigmen.client.Main;
import org.jdmp.sigmen.client.Waiter;
import org.jdmp.sigmen.messages.Constantes;
import org.jdmp.sigmen.messages.Convert;
import org.jdmp.sigmen.messages.Constantes.Envoi;

public class Login extends JPanel implements ActionListener, KeyListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = -5649936642064391061L;

    private JTextField tLog;

    private JPasswordField tMDP;

    public Login() {
        super();
        this.setBackground(Color.black);
        this.setLayout(new GridBagLayout());
        JLabel lLog = new JLabel("Login :");
        lLog.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel lMDP = new JLabel("Mot de passe :");
        lMDP.setHorizontalAlignment(SwingConstants.RIGHT);
        tLog = new JTextField();
        tMDP = new JPasswordField();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 0, 5);
        this.add(lLog, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 5, 0, 0);
        c.ipadx = 120;
        this.add(tLog, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.insets = new Insets(5, 0, 5, 5);
        c.anchor = GridBagConstraints.EAST;
        this.add(lMDP, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.ipadx = 120;
        c.insets = new Insets(5, 5, 5, 0);
        c.anchor = GridBagConstraints.WEST;
        this.add(tMDP, c);
        JButton bouton = new JButton("Connexion");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridheight = 1;
        c.gridwidth = 2;
        c.insets = new Insets(5, 0, 0, 0);
        c.anchor = GridBagConstraints.CENTER;
        this.add(bouton, c);
        bouton.addActionListener(this);
        bouton.addKeyListener(this);
        tMDP.addKeyListener(this);
        tMDP.setEchoChar('\\');
        tLog.addKeyListener(this);
        this.validate();
    }

    public void initFocus() {
        tLog.requestFocusInWindow();
    }

    private void loguer(byte[] data, int type) {
        Waiter.addLock(Waiter.LOGIN);
        Main.sender().send(Envoi.LOGIN, data, Convert.toByteArray(type));
        if (!Waiter.lock(Waiter.LOGIN)) {
            Main.fenetre().erreur(Fenetre.CONNEXION_PERDUE);
        } else {
            switch(Waiter.completeAns(Waiter.LOGIN)) {
                case Waiter.TRUE:
                    Main.fenetre().connecte();
                    break;
                case Waiter.FALSE:
                    Alertes.afficher(Alertes.BAD_LOGIN);
                    Main.fenetre().ecranLogin();
                    break;
                case Waiter.ALREADY_CONNECTED:
                    if (JOptionPane.showConfirmDialog(this, "Ce compte est déjà connecté. Souhaitez-vous le déconnecter ?", "Connexion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                        loguer(data, Constantes.Login.DISCONNECT_OTHER);
                    } else {
                        Main.fenetre().ecranLogin();
                    }
                    break;
                default:
                    Main.fenetre().erreur(Fenetre.ERREUR_DONNEES_RECUES);
                    break;
            }
        }
    }

    private void loguer() {
        if (tMDP.getPassword().length == 0 || tLog.getText().equals("")) {
            Alertes.afficher(Alertes.CHAMP_NON_REMPLI);
        } else {
            Main.verifyConnection();
            Main.fenetre().connect();
            byte[] login = tLog.getText().getBytes(Constantes.UTF8);
            try {
                byte[] mdp = MessageDigest.getInstance("SHA-512").digest(new String(tMDP.getPassword()).getBytes(Constantes.UTF8));
                byte[] data = new byte[login.length + 64];
                System.arraycopy(mdp, 0, data, 0, 64);
                System.arraycopy(login, 0, data, 64, login.length);
                loguer(data, Constantes.Login.VERIFY);
            } catch (NoSuchAlgorithmException e) {
                Main.fenetre().erreur(Fenetre.HASH_IMPOSSIBLE, e);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new Thread() {

            @Override
            public void run() {
                loguer();
            }
        }.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            new Thread() {

                @Override
                public void run() {
                    loguer();
                }
            }.start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}
