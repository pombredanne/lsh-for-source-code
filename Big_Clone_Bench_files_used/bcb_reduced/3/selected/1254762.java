package fondefitco.Controlador;

import java.awt.Color;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 *
 * @author Alex Eljach (Modificado Rodolfo Carcamo)
 */
public class ValidarorVistas {

    private static final String caracteres = "zxcvbnmñlkjhgfdsaqwertyuiop1324576890,;.:{ç}[+*]¿?=)(&%$·!¡";

    private static Properties datosServidor;

    private static String rutaPropiedades;

    private static String url;

    public static String desordenarCadena(String cadenaOrden) {
        char charCadena[] = cadenaOrden.toCharArray();
        List lista = new ArrayList();
        for (char c : charCadena) {
            lista.add(String.valueOf(c));
        }
        Collections.shuffle(lista, new Random());
        String desorden = "";
        for (Object c : lista) {
            desorden += (String) c;
        }
        return desorden;
    }

    public static String generarClave() {
        StringBuffer sb = new StringBuffer();
        String clave = ValidarorVistas.desordenarCadena(caracteres);
        return clave.substring(0, 10);
    }

    public static String encryptar(String textoOriginal) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(textoOriginal.getBytes());
        StringBuffer cadenaEncryp = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            cadenaEncryp.append(Integer.toHexString((b[i] & 0xFF)));
        }
        String textoCifrado = cadenaEncryp.toString();
        System.out.println("Texto Original: " + textoOriginal + "\nTexto Cifrado: " + textoCifrado);
        return textoCifrado;
    }

    public static boolean desencriptar(String textoOriginal, String textoCifrado) throws Exception {
        String textoCifrado2 = encryptar(textoOriginal);
        if (textoCifrado.equalsIgnoreCase(textoCifrado2)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean validaNumero(JTextField campo, int max) {
        String numero = campo.getText();
        try {
            Integer.parseInt(numero);
            return true;
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(campo, campo.getText(), "Error digite un número valido", JOptionPane.ERROR_MESSAGE);
            campo.requestFocus();
            campo.setSelectionColor(Color.RED);
            campo.selectAll();
            return false;
        }
    }

    public static boolean validaEmail(JTextField campo) {
        String email = campo.getText().toLowerCase();
        StringTokenizer token = new StringTokenizer(email, "@");
        if (token.countTokens() > 2 || token.countTokens() < 2) {
            JOptionPane.showMessageDialog(campo, campo.getText(), "Error digite un e-mail valido", JOptionPane.ERROR_MESSAGE);
            campo.requestFocus();
            campo.setSelectionColor(Color.RED);
            campo.selectAll();
            return false;
        }
        return true;
    }

    public static boolean validaVacio(JTextField campo, int max) {
        String dato = campo.getText().toLowerCase().trim();
        if (dato.equals("") || dato.length() < max) {
            JOptionPane.showMessageDialog(campo, campo.getToolTipText(), "Entrada Erronea", JOptionPane.ERROR_MESSAGE);
            campo.requestFocus();
            campo.setSelectionColor(Color.RED);
            campo.selectAll();
            return false;
        }
        return true;
    }

    public static String convertirMayuscula(String dato) {
        dato = dato.toUpperCase();
        return dato;
    }

    public static String validaVacio(String text, int i) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static String validaEmail(String text) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public boolean CamposVacios(JTextField... campos) {
        boolean sw = true;
        for (JTextField campo : campos) {
            if (campo.getText().equals("")) {
                campo.setBackground(new Color(72, 209, 204));
                sw = false;
            }
        }
        return sw;
    }

    public void RetornaColor(JTextField... campos) {
        for (JTextField campo : campos) {
            campo.setBackground(new Color(255, 255, 255));
        }
    }

    public void VaciarCampos(JTextField... campos) {
        for (JTextField campo : campos) {
            campo.setText("");
        }
    }

    public void VaciarCampos(JLabel... campos) {
        for (JLabel campo : campos) {
            campo.setText("");
        }
    }

    public boolean ValidarFecha(Date fecha) {
        int result = fecha.compareTo(new Date());
        return (result <= 0);
    }
}
