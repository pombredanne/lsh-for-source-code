package utils;

import java.security.MessageDigest;

/**
 * Esta clase permite calcular el hash MD5 de un string.
 * Arroja una excepcion si recibe un texto nulo o si
 * la version de java utilizada no puede obtener una instancia
 * MD5 de la clase MessageDigest.
 *
 * @author 
 */
public class MD5 {

    private MessageDigest md5;

    private String texto;

    private byte[] byTexto;

    private String hash;

    /**
     * Crea una nueva instancia del objeto MD5.
     *
     * @param texto Texto original cuyo hash MD5 se desea calcular
     * @throws java.lang.Exception 
     */
    public MD5(String texto) throws Exception {
        this.md5 = MessageDigest.getInstance("MD5");
        this.texto = texto;
        this.byTexto = texto.getBytes();
        calcMD5();
    }

    /**
     * Retorna el valor del texto claro
     * @return 
     */
    public String getTexto() {
        return texto;
    }

    /**
     * Retorna el hash asociado al texto claro
     * @return 
     */
    public String getMD5() {
        return hash;
    }

    /**
     * Permite modificar el valor del texto claro.
     * @param texto El nuevo valor del texto
     */
    public void setTexto(String texto) {
        this.texto = texto;
        byTexto = texto.getBytes();
        calcMD5();
    }

    private void calcMD5() {
        md5.reset();
        md5.update(byTexto);
        byte[] dig = md5.digest();
        hash = dumpBytes(dig);
    }

    /**
     * Este metodo auxiliar sirve para hacer legible la salida
     * del algoritmo, de lo contrario vemos car�cteres muy raros.
     * Autor: Jon Howell <jonh@cs.dartmouth.edu>
     * @param bytes Array de bytes arrojado como salida del algoritmo.
     * @return El valor del array como String.
     */
    private static String dumpBytes(byte[] bytes) {
        int i;
        StringBuffer sb = new StringBuffer();
        for (i = 0; i < bytes.length; i++) {
            if (i % 32 == 0 && i != 0) {
                sb.append("\n");
            }
            String s = Integer.toHexString(bytes[i]);
            if (s.length() < 2) {
                s = "0" + s;
            }
            if (s.length() > 2) {
                s = s.substring(s.length() - 2);
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
