package validadores;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import model.Usuario;
import dao.DAOUsuario;

public class AutenticaValidator implements Validator {

    @Override
    public void validate(FacesContext fc, UIComponent uic, Object val) throws ValidatorException {
        DAOUsuario daouser = new DAOUsuario();
        String login = (String) val;
        UIInput inp = (UIInput) uic.getAttributes().get("senha");
        String senha_temp = (String) inp.getSubmittedValue();
        Usuario u = daouser.findByLogin(login);
        Locale locale = fc.getViewRoot().getLocale();
        Application app = fc.getApplication();
        ResourceBundle bundle = ResourceBundle.getBundle(app.getMessageBundle(), locale);
        String msg = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(senha_temp.getBytes()));
        String pass = hash.toString(16);
        FacesMessage fm = null;
        if (u != null) {
            if (!(u.getSenha().equals(pass))) {
                msg = bundle.getString("erro_autentica_pass");
                fm = new FacesMessage(msg);
                throw new ValidatorException(fm);
            }
        } else {
            msg = bundle.getString("erro_autentica_user");
            fm = new FacesMessage(msg);
            throw new ValidatorException(fm);
        }
    }
}
