package invsys.Utilidades;

import java.awt.Color;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
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

    public static void validarnumero(JTextField campo, JLabel mensaje) {
        if (!campo.getText().matches("([0-9--])*") || campo.getText().equals("")) {
            mensaje.setText("Dato Invalido Solo Numeros");
            campo.setText("");
        } else mensaje.setText("");
    }

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

    public String Fechacodigio() {
        String fechaco = "";
        Date fecha = new Date();
        SimpleDateFormat codigo = new SimpleDateFormat("dd/MM/yy");
        String fechacodigo = codigo.format(fecha);
        StringTokenizer st = new StringTokenizer(fechacodigo, "/");
        fechaco = st.nextToken() + st.nextToken() + st.nextToken();
        return fechaco;
    }

    public long redondear(int precio) {
        String p = "" + precio;
        int c = p.length() - 1;
        char p3 = p.charAt(c);
        char p2 = p.charAt(c - 1);
        String ult = "" + p2 + p3;
        int op = Integer.parseInt(ult);
        if (op > 50) {
            op = 100 - op;
            precio = precio + op;
        } else if (op < 50 && op > 0) {
            op = 50 - op;
            precio = precio + op;
        }
        return (long) precio;
    }
}
