import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import java.util.Vector;
import javax.swing.*;

public class PasswordTableWindow extends JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    JButton btnNumber[];

    JPanel buttonPanel;

    JButton btnOK;

    JButton btnClear;

    JLabel labelKey;

    JPasswordField passwordField;

    private Connection theConn;

    String login;

    int key;

    PasswordTableWindow(String login) {
        super(login + ", tecle a senha de uso �nico");
        this.login = login;
        Error.log(4001, "Autentica��o etapa 3 iniciada.");
        Container container = getContentPane();
        container.setLayout(new FlowLayout());
        btnNumber = new JButton[10];
        btnOK = new JButton("OK");
        btnClear = new JButton("Limpar");
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 10));
        ResultSet rs;
        Statement stmt;
        String sql;
        Vector<Integer> result = new Vector<Integer>();
        sql = "select key from Senhas_De_Unica_Vez where login='" + login + "'";
        try {
            theConn = DatabaseConnection.getConnection();
            stmt = theConn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                result.add(rs.getInt("key"));
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
        Random rn = new Random();
        int r = rn.nextInt();
        if (result.size() == 0) {
            rn = new Random();
            Vector<Integer> passwordVector = new Vector<Integer>();
            Vector<String> hashVector = new Vector<String>();
            for (int i = 0; i < 10; i++) {
                r = rn.nextInt() % 10000;
                if (r < 0) r = r * (-1);
                passwordVector.add(r);
            }
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(login + ".txt", false));
                for (int i = 0; i < 10; i++) {
                    out.append("" + i + " " + passwordVector.get(i) + "\n");
                }
                out.close();
                try {
                    for (int i = 0; i < 10; i++) {
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            JOptionPane.showMessageDialog(null, "nova tabela de senhas criada para o usu�rio " + login + ".");
            Error.log(1002, "Sistema encerrado");
            System.exit(0);
        }
        if (r < 0) r = r * (-1);
        int index = r % result.size();
        if (index > result.size()) index = 0;
        key = result.get(index);
        labelKey = new JLabel("Chave n�mero " + key + " ");
        passwordField = new JPasswordField(12);
        ButtonHandler handler = new ButtonHandler();
        for (int i = 0; i < 10; i++) {
            btnNumber[i] = new JButton("" + i);
            buttonPanel.add(btnNumber[i]);
            btnNumber[i].addActionListener(handler);
        }
        btnOK.addActionListener(handler);
        btnClear.addActionListener(handler);
        container.add(buttonPanel);
        container.add(passwordField);
        container.add(labelKey);
        container.add(btnOK);
        container.add(btnClear);
        setSize(325, 200);
        setVisible(true);
    }

    private class ButtonHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnClear) {
                passwordField.setText("");
            }
            for (int i = 0; i < 10; i++) {
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
                sql = "select password from Senhas_De_Unica_Vez where login='" + login + "'" + " and key=" + key + " ";
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
                        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                        messageDigest.update(password.getBytes());
                        BigInteger bigInt = new BigInteger(1, messageDigest.digest());
                        String output = bigInt.toString(16);
                        if (output.compareTo(result) == 0) checkPassword = true; else checkPassword = false;
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
                    sql = "delete from Senhas_De_Unica_Vez where login='" + login + "'" + " and key=" + key + " ";
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
                    setVisible(false);
                    setTries(0);
                    Error.log(4003, "Senha de uso �nico verificada positivamente.");
                    Error.log(4002, "Autentica��o etapa 3 encerrada.");
                    ManagerWindow mw = new ManagerWindow(login);
                    mw.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else {
                    JOptionPane.showMessageDialog(null, "senha incorreta!");
                    int tries = getTries();
                    if (tries == 0) {
                        Error.log(4004, "Primeiro erro da senha de uso �nico contabilizado.");
                    } else if (tries == 1) {
                        Error.log(4005, "Segundo erro da senha de uso �nico contabilizado.");
                    } else if (tries == 2) {
                        Error.log(4006, "Terceiro erro da senha de uso �nico contabilizado.");
                        Error.log(4007, "Acesso do usuario " + login + " bloqueado pela autentica��o etapa 3.");
                        Error.log(4002, "Autentica��o etapa 3 encerrada.");
                        Error.log(1002, "Sistema encerrado.");
                        setTries(++tries);
                        System.exit(1);
                    }
                    setTries(++tries);
                }
            }
        }
    }

    public int getTries() {
        ResultSet rs;
        Statement stmt;
        String sql;
        int result = 0;
        sql = "select tries_one_use from Usuarios where login='" + login + "'";
        try {
            theConn = DatabaseConnection.getConnection();
            stmt = theConn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                result = rs.getInt("tries_one_use");
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
        sql = "update Usuarios set tries_one_use =" + tries + " where login='" + login + "'";
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
}
