import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;
import java.util.Random;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class PasswordWindow extends JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = -1452433818805362446L;

    JButton btnNumber[];

    JPanel buttonPanel;

    JButton btnOK;

    JButton btnClear;

    JPasswordField passwordField;

    String login;

    Vector<Integer> btn_values;

    private Connection theConn;

    public PasswordWindow(String login) {
        super(login + ", tecle a senha");
        Error.log(3001, "Autentica��o etapa 2 iniciada.");
        this.login = login;
        Container container = getContentPane();
        container.setLayout(new FlowLayout());
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 6));
        ButtonHandler handler = new ButtonHandler();
        CreateButtons();
        passwordField = new JPasswordField(12);
        btnOK = new JButton("OK");
        btnClear = new JButton("Limpar");
        for (int i = 0; i < 5; i++) {
            btnNumber[i] = new JButton(btn_values.get(2 * i) + " ou " + btn_values.get(2 * i + 1));
            buttonPanel.add(btnNumber[i]);
            btnNumber[i].addActionListener(handler);
        }
        btnOK.addActionListener(handler);
        btnClear.addActionListener(handler);
        container.add(buttonPanel);
        container.add(passwordField);
        container.add(btnOK);
        container.add(btnClear);
        setSize(325, 150);
        setVisible(true);
    }

    private class ButtonHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < 5; i++) {
                if (e.getSource() == btnNumber[i]) {
                    String password = new String((passwordField.getPassword()));
                    passwordField.setText(password + i);
                }
            }
            if (e.getSource() == btnOK) {
                String password = new String((passwordField.getPassword()));
                ResultSet rs;
                Statement stmt;
                String sql;
                String result = "";
                boolean checkPassword = false;
                boolean checkPassword1 = false;
                boolean checkPassword2 = false;
                sql = "select password from Usuarios where login='" + login + "'";
                try {
                    theConn = DatabaseConnection.getConnection();
                    stmt = theConn.createStatement();
                    rs = stmt.executeQuery(sql);
                    while (rs.next()) {
                        result = rs.getString("password");
                    }
                    rs.close();
                    stmt.close();
                    try {
                        Tree tree1 = CreateTree(password, 0);
                        Tree tree2 = CreateTree(password, 1);
                        tree1.enumerateTree(tree1.root);
                        tree2.enumerateTree(tree2.root);
                        for (int i = 0; i < tree1.passwdVector.size(); i++) {
                            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                            messageDigest.update(tree1.passwdVector.get(i).getBytes());
                            BigInteger bigInt = new BigInteger(1, messageDigest.digest());
                            String output = bigInt.toString(16);
                            if (output.compareTo(result) == 0) {
                                checkPassword1 = true;
                                break;
                            } else checkPassword1 = false;
                        }
                        for (int i = 0; i < tree2.passwdVector.size(); i++) {
                            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                            messageDigest.update(tree2.passwdVector.get(i).getBytes());
                            BigInteger bigInt = new BigInteger(1, messageDigest.digest());
                            String output = bigInt.toString(16);
                            if (output.compareTo(result) == 0) {
                                checkPassword2 = true;
                                break;
                            } else checkPassword2 = false;
                        }
                        if (checkPassword1 == true || checkPassword2 == true) checkPassword = true; else checkPassword = false;
                    } catch (NoSuchAlgorithmException exception) {
                        exception.printStackTrace();
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    try {
                        if (theConn != null) theConn.close();
                    } catch (Exception exception) {
                    }
                }
                if (checkPassword == true) {
                    JOptionPane.showMessageDialog(null, "senha correta!");
                    setTries(0);
                    setVisible(false);
                    Error.log(3003, "Senha pessoal verificada positivamente.");
                    Error.log(3002, "Autentica��o etapa 2 encerrada.");
                    PasswordTableWindow ptw = new PasswordTableWindow(login);
                    ptw.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else {
                    JOptionPane.showMessageDialog(null, "senha incorreta!");
                    Error.log(3004, "Senha pessoal verificada negativamente.");
                    int tries = getTries();
                    if (tries == 0) {
                        Error.log(3005, "Primeiro erro da senha pessoal contabilizado.");
                    } else if (tries == 1) {
                        Error.log(3006, "Segundo erro da senha pessoal contabilizado.");
                    } else if (tries == 2) {
                        Error.log(3007, "Terceiro erro da senha pessoal contabilizado.");
                        Error.log(3008, "Acesso do usuario " + login + " bloqueado pela autentica��o etapa 2.");
                        Error.log(3002, "Autentica��o etapa 2 encerrada.");
                        Error.log(1002, "Sistema encerrado.");
                        setTries(++tries);
                        System.exit(1);
                    }
                    setTries(++tries);
                }
            }
            if (e.getSource() == btnClear) {
                passwordField.setText("");
            }
        }
    }

    public int getTries() {
        ResultSet rs;
        Statement stmt;
        String sql;
        int result = 0;
        sql = "select tries_personal from Usuarios where login='" + login + "'";
        try {
            theConn = DatabaseConnection.getConnection();
            stmt = theConn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                result = rs.getInt("tries_personal");
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (theConn != null) theConn.close();
            } catch (Exception e) {
            }
        }
        return result;
    }

    public void setTries(int tries) {
        Statement stmt;
        String sql;
        sql = "update Usuarios set tries_personal =" + tries + " where login='" + login + "'";
        try {
            theConn = DatabaseConnection.getConnection();
            stmt = theConn.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (theConn != null) theConn.close();
            } catch (Exception e) {
            }
        }
        return;
    }

    private void CreateButtons() {
        btnNumber = new JButton[10];
        btn_values = new Vector<Integer>(10);
        Random rnd = new Random();
        Vector<Integer> temp = new Vector<Integer>(10);
        for (int i = 0; i < temp.capacity(); i++) {
            temp.add(i);
        }
        while (temp.size() != 1) {
            int rndNumber = rnd.nextInt(temp.size() - 1);
            btn_values.add(temp.get(rndNumber));
            temp.removeElementAt(rndNumber);
        }
        btn_values.add(temp.get(0));
    }

    private Tree CreateTree(String str, int arv) {
        int nButton = Integer.parseInt(str.substring(0, 1));
        Tree tree = new Tree(btn_values.get(2 * nButton + arv));
        for (int i = 1; i < str.length(); i++) {
            nButton = Integer.parseInt(str.substring(i, i + 1));
            tree.insertNode(btn_values.get(2 * nButton), btn_values.get(2 * nButton + 1));
        }
        return tree;
    }
}
