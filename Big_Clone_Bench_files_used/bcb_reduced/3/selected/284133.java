package conversores;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

public class HashSenhaConverter implements Converter {

    public Object getAsObject(FacesContext fc, UIComponent c, String pass_temp) throws ConverterException {
        if (pass_temp.length() < 5) {
            return pass_temp;
        } else {
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            BigInteger hash = new BigInteger(1, md.digest(pass_temp.getBytes()));
            String pass = hash.toString(16);
            return pass;
        }
    }

    public String getAsString(FacesContext fc, UIComponent c, Object pass) throws ConverterException {
        if ((pass == null) || (pass.toString().trim().equals(""))) {
            return "";
        } else return pass.toString();
    }
}
