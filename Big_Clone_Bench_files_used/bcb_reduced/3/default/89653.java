import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Random;
import java.util.Vector;
import javax.swing.*;

public class ChangeWindow extends JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = -224885950171501713L;

    JPanel textPanel;

    JPanel buttonPanel;

    JLabel lblPasswordUser1;

    JLabel lblPasswordUser2;

    JLabel lblNumPasswordOneUseUser;

    JButton btnChange;

    JButton btnCancel;

    JPasswordField passwordUser1;

    JPasswordField passwordUser2;

    JTextField numPasswordOneUseUser;

    String login;

    Connection theConn;

    public ChangeWindow(String login) {
        super("Formul�rio de Altera��o");
        this.login = login;
        Container container = getContentPane();
        container.setLayout(new GridLayout(1, 2));
        buttonPanel = new JPanel();
        textPanel = new JPanel();
        textPanel.setLayout(new GridLayout(3, 2));
        lblPasswordUser1 = new JLabel("Senha pessoal: ");
        lblPasswordUser2 = new JLabel("Confirma��o da senha pessoal: ");
        lblNumPasswordOneUseUser = new JLabel("Total de senhas de uso �nico: ");
        passwordUser1 = new JPasswordField(10);
        passwordUser2 = new JPasswordField(10);
        numPasswordOneUseUser = new JTextField(3);
        btnChange = new JButton("Alterar");
        btnCancel = new JButton("Voltar");
        ButtonHandler handler = new ButtonHandler();
        btnChange.addActionListener(handler);
        btnCancel.addActionListener(handler);
        textPanel.add(lblPasswordUser1);
        textPanel.add(passwordUser1);
        textPanel.add(lblPasswordUser2);
        textPanel.add(passwordUser2);
        textPanel.add(lblNumPasswordOneUseUser);
        textPanel.add(numPasswordOneUseUser);
        buttonPanel.add(btnChange);
        buttonPanel.add(btnCancel);
        container.add(textPanel);
        container.add(buttonPanel);
        setSize(730, 300);
        setVisible(true);
    }

    private class ButtonHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (event.getSource() == btnChange) {
                Error.log(7002, "Bot�o alterar pressionado por " + login + ".");
                if (new String(passwordUser1.getPassword()).compareTo("") == 0) {
                    JOptionPane.showMessageDialog(null, "Campo senha requerido");
                    passwordUser1.setFocusable(true);
                    return;
                }
                String password1 = new String(passwordUser1.getPassword());
                String password2 = new String(passwordUser2.getPassword());
                if (password1.compareTo(password2) != 0) {
                    JOptionPane.showMessageDialog(null, "Senhas n�o casam");
                    passwordUser1.setText("");
                    passwordUser2.setText("");
                    passwordUser1.setFocusable(true);
                    return;
                }
                char c = passwordUser1.getPassword()[0];
                int j = 1;
                for (j = 1; j < password1.length(); j++) {
                    if (passwordUser1.getPassword()[j] != c) {
                        break;
                    }
                    c = passwordUser1.getPassword()[j];
                }
                if (j == password1.length()) {
                    JOptionPane.showMessageDialog(null, "Senha fraca");
                    return;
                }
                if (password1.length() < 6) {
                    JOptionPane.showMessageDialog(null, "Senha deve ter mais que 6 digitos");
                    return;
                }
                if (numPasswordOneUseUser.getText().compareTo("") == 0) {
                    JOptionPane.showMessageDialog(null, "Campo n�mero de senhas de uso �nico requerido");
                    return;
                }
                if (!(Integer.parseInt(numPasswordOneUseUser.getText()) > 0 && Integer.parseInt(numPasswordOneUseUser.getText()) < 41)) {
                    JOptionPane.showMessageDialog(null, "N�mero de senhas de uso �nico entre 1 e 40");
                    return;
                }
                String outputDigest = "";
                try {
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                    messageDigest.update(password1.getBytes());
                    BigInteger bigInt = new BigInteger(1, messageDigest.digest());
                    outputDigest = bigInt.toString(16);
                } catch (NoSuchAlgorithmException exception) {
                    exception.printStackTrace();
                }
                Statement stmt;
                String sql;
                sql = "update Usuarios set password = '" + outputDigest + "' where login='" + login + "'";
                try {
                    theConn = DatabaseConnection.getConnection();
                    stmt = theConn.createStatement();
                    stmt.executeUpdate(sql);
                    stmt.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    try {
                        if (theConn != null) theConn.close();
                    } catch (Exception exception) {
                    }
                }
                sql = "delete from Senhas_De_Unica_Vez where login='" + login + "'";
                try {
                    theConn = DatabaseConnection.getConnection();
                    stmt = theConn.createStatement();
                    stmt.executeUpdate(sql);
                    stmt.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    try {
                        if (theConn != null) theConn.close();
                    } catch (Exception exception) {
                    }
                }
                Random rn = new Random();
                int r;
                Vector<Integer> passwordVector = new Vector<Integer>();
                for (int i = 0; i < Integer.parseInt(numPasswordOneUseUser.getText()); i++) {
                    r = rn.nextInt() % 10000;
                    if (r < 0) r = r * (-1);
                    passwordVector.add(r);
                }
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(login + ".txt", false));
                    for (int i = 0; i < Integer.parseInt(numPasswordOneUseUser.getText()); i++) {
                        out.append("" + i + " " + passwordVector.get(i) + "\n");
                    }
                    out.close();
                    try {
                        for (int i = 0; i < Integer.parseInt(numPasswordOneUseUser.getText()); i++) {
                            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                            messageDigest.update(passwordVector.get(i).toString().getBytes());
                            BigInteger bigInt = new BigInteger(1, messageDigest.digest());
                            String digest = bigInt.toString(16);
                            sql = "insert into Senhas_De_Unica_Vez (login,key,password) values " + "('" + login + "'," + i + ",'" + digest + "')";
                            try {
                                theConn = DatabaseConnection.getConnection();
                                stmt = theConn.createStatement();
                                stmt.executeUpdate(sql);
                                stmt.close();
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            } finally {
                                try {
                                    if (theConn != null) theConn.close();
                                } catch (Exception exception) {
                                }
                            }
                        }
                    } catch (NoSuchAlgorithmException exception) {
                        exception.printStackTrace();
                    }
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                JOptionPane.showMessageDialog(null, "Usu�rio " + login + " foi alterado com sucesso.");
                dispose();
            }
            if (event.getSource() == btnCancel) {
                Error.log(7003, "Bot�o voltar de alterar para o menu principal pressionado por " + login + ".");
                dispose();
            }
        }
    }
}
