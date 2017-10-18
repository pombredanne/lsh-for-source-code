package client.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import client.model.ClientManager;
import client.view.ClientGUI;
import client.view.LoginDialog;

public class LoginBtnListener implements ActionListener {

    private Object view;

    private ClientManager model;

    private LoginDialog login;

    public LoginBtnListener(ClientGUI view, ClientManager model, LoginDialog l) {
        this.view = view;
        this.model = model;
        this.login = l;
    }

    public void actionPerformed(ActionEvent e) {
        String digest = null;
        try {
            MessageDigest m = MessageDigest.getInstance("sha1");
            m.reset();
            String pw = String.copyValueOf(this.login.getPassword());
            m.update(pw.getBytes());
            byte[] digestByte = m.digest();
            BigInteger bi = new BigInteger(digestByte);
            digest = bi.toString();
            System.out.println(digest);
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        this.model.login(this.login.getHost(), this.login.getPort(), this.login.getUser(), digest);
    }
}
