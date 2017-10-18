package jcpcotizaciones.vista;

import java.awt.Color;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 *
 * @author David
 */
public final class ControlValidacion {

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
        String clave = ControlValidacion.desordenarCadena(caracteres);
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
}
