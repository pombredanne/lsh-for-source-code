package terica.firmas;

import java.io.*;
import java.security.*;

/**

* <table border="0" width="600" cellpadding="5" cellspacing="0">
* <tr><td bgcolor="#CCCCCC"><font color="#006600">
* Esta clase genera el MD5, tanto de un String de entrada como de un fichero.<br>
* Devuelve un String, cadena hexadecimal que representa el MD5 de la entrada.
*<br>
* </font></tr></td>
* </table>

*
* @throws java.lang.exception

* @see <a href="http://www.luis.criado.org">�qui�n es Luis Criado?</a> 
* <br>Esta clase se utiliza en la herramienta
* @see <a href="http://sw2sws.sourceforge.net/">sw2sws</a>
* <br>basado en el 
* @see <a href="http://e-spacio.uned.es/fez/view.php?pid=tesisuned:IngInf-Lcriado">procedimiento semi-autom�tico para transformar la web en web sem�ntica</a> 
* <br>Licencia cedida con arreglo a:
* @see <a href="http://ec.europa.eu/idabc/eupl">EUPL V.1.1.1</a> 
*     
* @author Luis Criado Fern�ndez
* @since version 1.0 
* @version 1.1.2 
*/
public class md5 {

    /**
     * <pre>
     * Atributo de la clase.
     * </pre>
     * <pre>
     *        Contiene la cadena para procesar con el algoritmo de Firma Digital.
     *        Si se trata de un fichero debe contener el camino completo.
     *
     * </pre>
     */
    protected String cadenaParaProcesar = "";

    /**
     * <pre>
     * Atributo de la clase.
     * </pre>
     * <pre>
     *       Este atributo identifica si el Firma digital se realiza sobre un String o un Fichero.
     *          Si es true se procesa un archivo (por defecto).
     *          Si es false seprocesa un String.
     * </pre>
     */
    protected boolean tipoProceso = true;

    /**
     * <pre>
     * Atributo de la clase.
     * </pre>
     * <pre>
     *        Algoritmo de firma digital
     *
     * </pre>
     */
    protected String algoritmo = "MD5";

    /**
     * constructor 1
     */
    public md5(String cadena) {
        this.cadenaParaProcesar = cadena;
        this.tipoProceso = true;
    }

    /**
     * constructor 2
     */
    public md5(String cadena, boolean tipo) {
        this.cadenaParaProcesar = cadena;
        this.tipoProceso = tipo;
    }

    public String toString() {
        int BUFFER_SIZE = 32 * 1024;
        String digestString = "";
        int i = 0;
        if (tipoProceso) {
            try {
                FileInputStream fichero = new FileInputStream(this.cadenaParaProcesar);
                BufferedInputStream file = new BufferedInputStream(fichero);
                MessageDigest mdfichero = MessageDigest.getInstance(this.algoritmo);
                DigestInputStream in = new DigestInputStream(file, mdfichero);
                byte[] buffer = new byte[BUFFER_SIZE];
                do {
                    i = in.read(buffer, 0, BUFFER_SIZE);
                } while (i == BUFFER_SIZE);
                mdfichero = in.getMessageDigest();
                in.close();
                byte[] digest = mdfichero.digest();
                digestString = hexString(digest);
                file.close();
                fichero.close();
            } catch (SecurityException e) {
                System.out.println("Error de seguridad.");
            } catch (Exception e) {
                System.err.println("Error: " + e);
            }
        } else {
            try {
                MessageDigest md = MessageDigest.getInstance(this.algoritmo);
                byte[] digest = new byte[md.getDigestLength()];
                digest = md.digest(cadenaParaProcesar.getBytes());
                digestString = hexString(digest);
            } catch (SecurityException e) {
                System.out.println("Error de seguridad.");
            } catch (Exception e) {
                System.err.println("Error: " + e);
            }
        }
        return digestString;
    }

    private static final String hex = "0123456789ABCDEF";

    private static String hexString(byte[] vb) {
        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < vb.length; j++) {
            sb.append(hex.charAt((int) (vb[j] >> 4) & 0xf));
            sb.append(hex.charAt((int) (vb[j]) & 0xf));
        }
        return sb.toString();
    }
}
