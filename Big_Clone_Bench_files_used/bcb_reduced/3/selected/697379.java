package es.uned.dia.pfcdbenito6.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Formatter;
import es.uned.dia.pfcdbenito6.utils.StreamUtils;

/**
 * Clase para generar Checksums
 * @author David Benito Le&oacute;n
 */
public class CheckSum {

    private static boolean enabled = true;

    public static void setEnabled(boolean enabled) {
        CheckSum.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private MessageDigest digest;

    /** Inicializaci�n de algoritmo MD5*/
    public CheckSum() throws Exception {
        this("MD5");
    }

    /** Inicializaci�n con otro algoritmo */
    public CheckSum(String provider) throws Exception {
        digest = MessageDigest.getInstance(provider);
    }

    /**
     * Actualiza la firma con una nueva porci�n de datos.
	 * 
	 * @param input Buffer de entrada
	 * @param iOffset Byte inicial
	 * @param length N�mero de bytes a considerar
	 */
    public void update(byte[] input, int iOffset, int length) {
        digest.update(input, iOffset, length);
    }

    /**
	 * @return Devuelve la firma (lowercase)
	 */
    public String print() {
        return print(digest.digest());
    }

    /**M�todo de utilidad para convertir a hexadecimal un array de bytes*/
    public static String print(byte[] digest) {
        StringBuilder sOut = new StringBuilder();
        Formatter fmt = new Formatter(sOut);
        for (int i = 0; i < digest.length; i++) {
            fmt.format("%02x", digest[i]);
        }
        return sOut.toString();
    }

    /**
     * Calcula la firma asociada a una cadena de caracteres.
     * @param str Cadena cuya firma calcular
     * @return La firma o null si hay problemas
     */
    public static String createCheckSum(String str) throws Exception {
        CheckSum chk = new CheckSum();
        byte[] buffer = str.getBytes();
        chk.update(buffer, 0, buffer.length);
        return chk.print();
    }

    /**
	 * Calcula la firma MD5 de un fichero.
	 * 
	 * @param f Fichero
	 * @return la firma MD5 (lowercase)
	 * @throws BackupException ante fallos extra�os
     * @throws FileNotFoundException al abrir el fichero
	 */
    public static String createCheckSum(File f) throws FileNotFoundException, BackupException {
        if (CheckSum.isEnabled() == false) {
            return "";
        }
        try {
            CheckSum chk = new CheckSum();
            InputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[1024 * StreamUtils.BLOCK];
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    chk.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();
            return chk.print();
        } catch (FileNotFoundException fnf) {
            throw fnf;
        } catch (Throwable th) {
            throw new BackupException("Error calculando la firma de: " + f.getAbsolutePath(), th);
        }
    }

    /**Test method*/
    public static void main(String args[]) {
        try {
            System.out.println(createCheckSum(new File(args[0])));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
