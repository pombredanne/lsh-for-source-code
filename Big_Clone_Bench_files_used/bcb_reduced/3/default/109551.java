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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import java.util.Vector;
import javax.swing.*;

public class RegisterWindow extends JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = -224885950171501713L;

    JPanel textPanel;

    JPanel buttonPanel;

    JTextField nameUser;

    JTextField loginUser;

    JList groupUser;

    JPasswordField passwordUser1;

    JPasswordField passwordUser2;

    JTextField numPasswordOneUseUser;

    JLabel lblNameUser;

    JLabel lblLoginUser;

    JLabel lblGroupUser;

    JLabel lblPasswordUser1;

    JLabel lblPasswordUser2;

    JLabel lblNumPasswordOneUseUser;

    JButton btnRegister;

    JButton btnCancel;

    Connection theConn;

    String login;

    public RegisterWindow(String login) {
        super("Formul�rio de Cadastro");
        Container container = getContentPane();
        container.setLayout(new GridLayout(1, 2));
        this.login = login;
        buttonPanel = new JPanel();
        textPanel = new JPanel();
        textPanel.setLayout(new GridLayout(6, 2));
        lblNameUser = new JLabel("Nome do usu�rio: ");
        lblLoginUser = new JLabel("Login Name: ");
        lblGroupUser = new JLabel("Grupo: ");
        lblPasswordUser1 = new JLabel("Senha pessoal: ");
        lblPasswordUser2 = new JLabel("Confirma��o da senha pessoal: ");
        lblNumPasswordOneUseUser = new JLabel("Total de senhas de uso �nico: ");
        nameUser = new JTextField(50);
        loginUser = new JTextField(20);
        String groupList[] = { "Administrador", "Usu�rio" };
        groupUser = new JList(groupList);
        passwordUser1 = new JPasswordField(10);
        passwordUser2 = new JPasswordField(10);
        numPasswordOneUseUser = new JTextField(3);
        btnRegister = new JButton("Cadastrar");
        btnCancel = new JButton("Cancelar");
        ButtonHandler handler = new ButtonHandler();
        btnRegister.addActionListener(handler);
        btnCancel.addActionListener(handler);
        textPanel.add(lblNameUser);
        textPanel.add(nameUser);
        textPanel.add(lblLoginUser);
        textPanel.add(loginUser);
        textPanel.add(lblGroupUser);
        textPanel.add(groupUser);
        textPanel.add(lblPasswordUser1);
        textPanel.add(passwordUser1);
        textPanel.add(lblPasswordUser2);
        textPanel.add(passwordUser2);
        textPanel.add(lblNumPasswordOneUseUser);
        textPanel.add(numPasswordOneUseUser);
        buttonPanel.add(btnRegister);
        buttonPanel.add(btnCancel);
        container.add(textPanel);
        container.add(buttonPanel);
        setSize(725, 300);
        setVisible(true);
    }

    private class ButtonHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnRegister) {
                Error.log(6002, "Bot�o cadastrar pressionado por " + login + ".");
                if (nameUser.getText().compareTo("") == 0) {
                    JOptionPane.showMessageDialog(null, "Campo nome requerido");
                    nameUser.setFocusable(true);
                    return;
                }
                if (loginUser.getText().compareTo("") == 0) {
                    JOptionPane.showMessageDialog(null, "Campo login requerido");
                    loginUser.setFocusable(true);
                    return;
                }
                String group = "";
                if (groupUser.getSelectedIndex() == 0) group = "admin"; else if (groupUser.getSelectedIndex() == 1) group = "user"; else {
                    JOptionPane.showMessageDialog(null, "Campo grupo n�o selecionado");
                    return;
                }
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
                int i = 1;
                for (i = 1; i < password1.length(); i++) {
                    if (passwordUser1.getPassword()[i] != c) {
                        break;
                    }
                    c = passwordUser1.getPassword()[i];
                }
                if (i == password1.length()) {
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
                ResultSet rs;
                Statement stmt;
                String sql;
                String result = "";
                sql = "select login from Usuarios where login='" + loginUser.getText() + "'";
                try {
                    theConn = DatabaseConnection.getConnection();
                    stmt = theConn.createStatement();
                    rs = stmt.executeQuery(sql);
                    while (rs.next()) {
                        result = rs.getString("login");
                    }
                    rs.close();
                    stmt.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    try {
                        if (theConn != null) theConn.close();
                    } catch (Exception exception) {
                    }
                }
                if (result.compareTo("") != 0) {
                    JOptionPane.showMessageDialog(null, "Login " + result + " j� existe");
                    loginUser.setText("");
                    loginUser.setFocusable(true);
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
                sql = "insert into Usuarios (login,password,tries_personal,tries_one_use," + "grupo,description) values " + "('" + loginUser.getText() + "','" + outputDigest + "',0,0,'" + group + "','" + nameUser.getText() + "')";
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
                for (i = 0; i < Integer.parseInt(numPasswordOneUseUser.getText()); i++) {
                    r = rn.nextInt() % 10000;
                    if (r < 0) r = r * (-1);
                    passwordVector.add(r);
                }
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(loginUser.getText() + ".txt", false));
                    for (i = 0; i < Integer.parseInt(numPasswordOneUseUser.getText()); i++) {
                        out.append("" + i + " " + passwordVector.get(i) + "\n");
                    }
                    out.close();
                    try {
                        for (i = 0; i < Integer.parseInt(numPasswordOneUseUser.getText()); i++) {
                            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                            messageDigest.update(passwordVector.get(i).toString().getBytes());
                            BigInteger bigInt = new BigInteger(1, messageDigest.digest());
                            String digest = bigInt.toString(16);
                            sql = "insert into Senhas_De_Unica_Vez (login,key,password) values " + "('" + loginUser.getText() + "'," + i + ",'" + digest + "')";
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
                JOptionPane.showMessageDialog(null, "Usu�rio " + loginUser.getText() + " foi cadastrado com sucesso.");
                dispose();
            }
            if (e.getSource() == btnCancel) {
                Error.log(6003, "Bot�o voltar de cadastrar para o menu principal pressionado por " + login + ".");
                dispose();
            }
        }
    }
}
