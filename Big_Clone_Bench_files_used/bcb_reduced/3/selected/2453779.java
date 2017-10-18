package de.bwb.ekp.converter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import org.jboss.seam.annotations.Name;
import de.bwb.ekp.interceptors.MeasureCalls;

@MeasureCalls
@Name(value = "passwordConverter")
@org.jboss.seam.annotations.faces.Converter(id = "bwb.ekp.PasswordConverter")
public class PasswordConverter implements Converter {

    public static final String CONVERTER_ID = "bwb.ekp.PasswordConverter";

    private static final String ENTRIES[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    public String encrypt(final String pwd) {
        return this.getAsObject(null, null, pwd).toString();
    }

    public Object getAsObject(final FacesContext fc, final UIComponent uic, final String text) {
        return PasswordConverter.convertPassword(text);
    }

    public static final String convertPassword(final String srcPwd) {
        StringBuilder out;
        MessageDigest md;
        byte[] byteValues;
        byte singleChar = 0;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(srcPwd.getBytes());
            byteValues = md.digest();
            if ((byteValues == null) || (byteValues.length <= 0)) {
                return null;
            }
            out = new StringBuilder(byteValues.length * 2);
            for (byte element : byteValues) {
                singleChar = (byte) (element & 0xF0);
                singleChar = (byte) (singleChar >>> 4);
                singleChar = (byte) (singleChar & 0x0F);
                out.append(PasswordConverter.ENTRIES[singleChar]);
                singleChar = (byte) (element & 0x0F);
                out.append(PasswordConverter.ENTRIES[singleChar]);
            }
            return out.toString();
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getAsString(final FacesContext fc, final UIComponent uic, final Object obj) {
        return obj.toString();
    }
}
