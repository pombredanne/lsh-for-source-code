package gui;

import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import exception.MyHibernateException;
import bean.user.AccessLevel;
import bean.user.User;
import facade.Facade;
import run.Main;
import util.CriptUtil;
import util.Util;
import org.jdesktop.swingx.JXLoginDialog;
import org.jdesktop.swingx.JXLoginPanel.SaveMode;
import org.jdesktop.swingx.auth.LoginService;

public class LoginDialogFactory {

    private static JXLoginDialog dialog;

    public static JXLoginDialog createLoginDialog() {
        dialog = new JXLoginDialog(new MySQLLoginService(), null, null);
        dialog.setTitle("Acessar o sistema");
        dialog.getPanel().setBannerText(Main.info.getProperty("banner.text"));
        dialog.setDefaultCloseOperation(JXLoginDialog.DISPOSE_ON_CLOSE);
        dialog.setAlwaysOnTop(true);
        dialog.getPanel().setSaveMode(SaveMode.NONE);
        dialog.setModal(true);
        dialog.setVisible(true);
        return dialog;
    }

    public static AccessLevel getUserAcessLevel() {
        return dialog != null ? ((MySQLLoginService) dialog.getPanel().getLoginService()).getAcessLevel() : null;
    }

    private static final class MySQLLoginService extends LoginService {

        private User user;

        public MySQLLoginService() {
        }

        @Override
        public void startAuthentication(String login, char[] pass, String server) throws Exception {
            super.startAuthentication(login, pass, server);
        }

        @Override
        public boolean authenticate(String login, char[] pass, String server) {
            try {
                byte[] b = new String(pass).getBytes();
                try {
                    b = CriptUtil.digest(b, CriptUtil.SHA);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                this.user = Facade.getInstance().loadUser(login, CriptUtil.byteArrayToHexString(b));
                if (this.user != null) {
                    if (login.equals(this.user.getLogin())) {
                        dialog.setVisible(false);
                        MainFrame.getInstance().setUserLogged(this.user);
                        return true;
                    }
                }
            } catch (MyHibernateException ex) {
                Logger.getLogger(LoginDialogFactory.class.getName()).log(Level.SEVERE, null, ex);
                Util.errorSQLPane(MainFrame.getInstance(), ex);
            }
            return false;
        }

        public AccessLevel getAcessLevel() {
            return user.getAccessLevel();
        }
    }
}
