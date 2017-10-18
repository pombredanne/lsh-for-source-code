package utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.StringTokenizer;
import javax.swing.JOptionPane;

/**
 *
 * @author Fernando Dettoni
 */
public class Funcoes {

    /** Creates a new instance of Funcoes */
    public static String criptografaSenha(String pass) {
        BigInteger hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            hash = new BigInteger(1, md.digest(pass.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String s = hash.toString(16);
        if (s.length() % 2 != 0) s = "0" + s;
        return s;
    }

    public static String trataData(String dt) {
        if (dt != null) {
            StringTokenizer st = new StringTokenizer(dt, "-");
            String ano = st.nextToken();
            String mes = st.nextToken();
            String dia = st.nextToken();
            return dia + "/" + mes + "/" + ano;
        } else return "";
    }

    public static void mensagemErro(String msg) {
        JOptionPane.showMessageDialog(null, msg + "Verifique log.", "Erro geral.", JOptionPane.ERROR_MESSAGE);
    }
}
