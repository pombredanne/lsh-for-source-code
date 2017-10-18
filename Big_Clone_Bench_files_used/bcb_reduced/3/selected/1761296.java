package ee.fctwister.wc2010.pages;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.tapestry.annotations.InjectState;
import org.apache.tapestry.html.BasePage;
import ee.fctwister.wc2010.DAO.UserDAO;
import ee.fctwister.wc2010.DTO.UserDTO;

public abstract class Login extends BasePage {

    public abstract String getEmail();

    public abstract String getPassword();

    public abstract String getStatusMessage();

    public abstract void setStatusMessage(String msg);

    @InjectState("user")
    public abstract UserDTO getUser();

    public String onLogin() {
        String password = null;
        try {
            password = calculateMD5(getPassword());
            UserDTO user = UserDAO.getUser(getEmail(), password);
            if (user.isRegistered()) {
                getUser().copyFrom(user);
                return "Home";
            } else {
                setStatusMessage("Kasutaja konto on kinnitamata. Kontrolli oma emaili.");
                return null;
            }
        } catch (Exception e) {
            System.out.println("Illegal user/password (" + getEmail() + " / " + password + ")");
            setStatusMessage("Kasutaja/parool on vale!");
            return null;
        }
    }

    private String calculateMD5(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.reset();
        digest.update(input.getBytes());
        byte[] md5 = digest.digest();
        String tmp = "";
        String res = "";
        for (int i = 0; i < md5.length; i++) {
            tmp = (Integer.toHexString(0xFF & md5[i]));
            if (tmp.length() == 1) {
                res += "0" + tmp;
            } else {
                res += tmp;
            }
        }
        return res;
    }

    public void registerUser(String email, String password) {
        try {
            UserDAO.registerUser(email, password);
            setStatusMessage("Kasutaja konto on kinnitatud. Logi sisse!");
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Kasutaja konto kinnitamine luhtus!");
        }
    }
}
