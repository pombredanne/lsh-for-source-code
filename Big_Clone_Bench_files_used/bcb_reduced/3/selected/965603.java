package ee.fctwister.wc2010.pages;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.tapestry.annotations.InjectState;
import org.apache.tapestry.html.BasePage;
import ee.fctwister.wc2010.generic.Mail;
import ee.fctwister.wc2010.generic.Validator;
import ee.fctwister.wc2010.DAO.UserDAO;
import ee.fctwister.wc2010.DTO.UserDTO;

public abstract class Register extends BasePage {

    public abstract String getFirstName();

    public abstract String getLastName();

    public abstract String getEmail();

    public abstract String getPassword();

    public abstract String getRegisterHash();

    public abstract String getStatusMessage();

    public abstract void setStatusMessage(String msg);

    @InjectState("user")
    public abstract UserDTO getUser();

    public String onRegister() {
        int inserted = 0;
        Validator validator = new Validator();
        try {
            if (validator.emailIsValid(getEmail())) {
                String password = calculateMD5(getPassword());
                inserted = UserDAO.insertUser(getFirstName(), getLastName(), getEmail(), password);
                if (inserted == 1) {
                    if (sendEmail(getFirstName(), getEmail(), password)) {
                        setStatusMessage("Registreerimine edukas. Email saadetud. Palun kinnita oma konto.");
                    } else {
                        setStatusMessage("Registreerimine luhtus. Emaili ei suudetud saata!");
                    }
                } else if (inserted == 2) {
                    setStatusMessage("Sellise emailiga kasutaja eksisteerib juba. Sisesta uus!");
                } else {
                    setStatusMessage("Registreerimine luhtus. Proovi uuesti!");
                }
            } else {
                setStatusMessage("Registreerimine luhtus. Ebakorrektne email!");
            }
        } catch (Exception e) {
            setStatusMessage("Registreerimine luhtus. Proovi uuesti!");
            e.printStackTrace();
            return null;
        }
        return "register";
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

    private boolean sendEmail(String firstName, String email, String password) {
        Validator validator = new Validator();
        Mail mail = new Mail();
        boolean mailSent = false;
        if (validator.emailIsValid(email)) {
            try {
                mail.postMail(new String[] { email }, "FC Twister MM ennustuse kasutaja registreerimine", getEmailContent(email, password), "admin@fctwister.ee");
                mailSent = true;
            } catch (Exception e) {
                e.printStackTrace();
                mailSent = false;
            }
        }
        return mailSent;
    }

    private String getEmailContent(String email, String password) {
        return "Tere tulemast! Palun kinnita oma osalemine vajutades allolevat linki.<br><br><a href=\"http://www.fctwister.ee/wc2010/login,confirmRegistration.direct?sp=S" + email + "&sp=S" + password + "\">Kinnita registreerimine</a>";
    }
}
