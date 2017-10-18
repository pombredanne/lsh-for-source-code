import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.swing.*;

public class Login {

    JLabel titleLabel;

    JLabel nameLabel;

    JTextField nameText;

    JLabel pwdLabel;

    JPasswordField pwdText;

    JButton issueButton;

    JButton exitButton;

    JButton helpButton;

    JFrame frame = new JFrame();

    static Mdi m = new Mdi();

    public static void main(String[] args) {
        try {
            Login login = new Login();
            login.execLogin();
        } catch (Exception e) {
            System.out.println("Invalid action");
        }
    }

    public void execLogin() {
        frame.setBounds(500, 300, 250, 200);
        titleLabel = new JLabel("Login");
        titleLabel.setBounds(100, 5, 100, 40);
        nameLabel = new JLabel("Username");
        nameLabel.setBounds(10, 50, 80, 20);
        nameText = new JTextField("engg", 30);
        nameText.setBounds(100, 50, 130, 20);
        pwdLabel = new JLabel("Password");
        pwdLabel.setBounds(10, 80, 80, 20);
        pwdText = new JPasswordField("12345678", 30);
        pwdText.setBounds(100, 80, 130, 20);
        issueButton = new JButton("Enter");
        issueButton.addActionListener(new LoginHandler());
        issueButton.setBounds(20, 120, 80, 20);
        exitButton = new JButton("Cancel");
        exitButton.addActionListener(new ExitHandler());
        exitButton.setBounds(130, 120, 80, 20);
        helpButton = new JButton("Help");
        helpButton.addActionListener(new HelpHandler());
        helpButton.setBounds(160, 10, 60, 20);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.getContentPane().add(titleLabel);
        frame.getContentPane().add(nameLabel);
        frame.getContentPane().add(nameText);
        frame.getContentPane().add(pwdLabel);
        frame.getContentPane().add(pwdText);
        frame.getContentPane().add(issueButton);
        frame.getContentPane().add(exitButton);
        frame.getContentPane().add(helpButton);
        frame.setVisible(true);
        frame.setTitle("Smart Highway Designer");
    }

    class ExitHandler implements ActionListener {

        public void actionPerformed(ActionEvent arg) {
            System.exit(0);
        }
    }

    class LoginHandler implements ActionListener {

        public void actionPerformed(ActionEvent arg0) {
            DatabaseCon d = null;
            d = DatabaseCon.createCon();
            String uName = nameText.getText();
            char[] pwd = pwdText.getPassword();
            int uLevel = checkLogin(uName, pwd);
            if (uLevel != -1) {
                {
                    frame.setVisible(false);
                    m.setEnabled(true);
                    m.setFocusable(true);
                    m.setVisible(true);
                    m.setUserPermission(uName, uLevel);
                }
            } else {
                displayMessage("Invalid Username/Password");
            }
        }
    }

    class HelpHandler implements ActionListener {

        public void actionPerformed(ActionEvent arg) {
            DatabaseCon d = null;
            d = DatabaseCon.createCon();
            HelpClass HC = new HelpClass();
            HC.createGui("Login", "Login: Administrator is having the username of �root�" + "always. The default password will be �password�." + "Administrator can enter the application by entering" + "his username and password in the login dialog box.");
        }
    }

    public static byte[] encrypt(byte[] x) throws Exception {
        java.security.MessageDigest d = null;
        d = java.security.MessageDigest.getInstance("SHA-1");
        d.reset();
        d.update(x);
        return d.digest();
    }

    public static int checkLogin(String userName, char[] password) {
        DatabaseCon d = null;
        Key holdKey = null;
        d = DatabaseCon.createCon();
        String pwd = new String(password);
        try {
            holdKey = AddLogin.generateKey();
        } catch (NoSuchAlgorithmException e) {
        }
        String encryptedPassword = AddLogin.encrypt(pwd, holdKey);
        String queryStr = "SELECT * FROM LOGIN WHERE LOGIN_ID='" + userName + "' AND LOGIN_PASSWORD='" + encryptedPassword + "'";
        d.execQuery(queryStr);
        if (d.isNotNull()) {
            d.goNext();
            return d.getInt(4);
        } else {
            return -1;
        }
    }

    public static void displayMessage(String message) {
        JFrame frame = new JFrame();
        JOptionPane.showMessageDialog(frame, message);
    }
}
