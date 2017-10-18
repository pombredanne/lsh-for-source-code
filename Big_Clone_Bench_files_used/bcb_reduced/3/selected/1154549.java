package com.viators.actions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * COLEGIO DE NOTARIOS DE LIMA - CEDETEC
 * Convenio DIGEMIN - CNL
 * @author Elvis Ruben Campos Mori
 * @since Octubre 2008
 * @version 1.0
 */
public class Utiles {

    public static String FORMATO_FECHA_LARGE = "dd/MM/yyyy hh:mm:ss";

    public static String FORMATO_FECHA_CORTA = "dd/MM/yyyy";

    public static String FORMATO_FECHA_CORTA_MYSQL = "yyyy-MM-dd";

    public static final String version = "1.0";

    /**
     * retorna una cadena vacia en caso de ser null
     */
    public static String nullToBlank(Object texto) {
        if (texto == null) {
            return "";
        }
        return texto.toString().trim();
    }

    /**
     * entrega un objetod el tipo GregorianCalendar con la fecha indicada
     * @param fecha texto a convertir en fecha
     * @param formato usar Utils.FORMATO_FECHA_CORTA o Utils.FORMATO_FECHA_LARGE
     * @return objeto gregoriancalendar con la fecha en el formato indicado
     * @throws Exception
     */
    public static GregorianCalendar stringToCalendar(String fecha, String formato) throws Exception {
        if (nullToBlank(fecha).equals("")) {
            throw new Exception("Ha enviado una fecha vacia.");
        }
        fecha = nullToBlank(fecha);
        GregorianCalendar gc = new GregorianCalendar();
        SimpleDateFormat df = new SimpleDateFormat(formato);
        gc.setTime(df.parse(fecha));
        return gc;
    }

    public static String CalendarToString(Calendar fecha, String formato) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat(formato);
        return df.format(fecha.getTime());
    }

    public static String dateToString(Date fecha, String formato) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat(formato);
        return df.format(fecha);
    }

    public static String ValidaDate(String fecha, String formato) throws Exception {
        if (nullToBlank(fecha).equals("")) {
            throw new Exception("Ha enviado una fecha vacia.");
        }
        fecha = nullToBlank(fecha);
        GregorianCalendar gc = new GregorianCalendar();
        SimpleDateFormat df = new SimpleDateFormat(formato);
        gc.setTime(df.parse(fecha));
        return df.format(gc.getTime());
    }

    public static Date stringToDate(String fecha, String formato) throws Exception {
        if (nullToBlank(fecha).equals("")) {
            throw new Exception("Ha enviado una fecha vacia.");
        }
        fecha = nullToBlank(fecha);
        GregorianCalendar gc = new GregorianCalendar();
        SimpleDateFormat df = new SimpleDateFormat(formato);
        gc.setTime(df.parse(fecha));
        return gc.getTime();
    }

    /**
     * Crea un archivo con extension jpg en la carpeta temporal del proyecto web para poder mostrarla en 
     * la pagina web
     * @param imagenBuffer InputStream que contiene la imagen
     * @param extra texto para incluir en el nombre (D: dedo; H:huella)
     * @param path ruta en donde se grabar� la imagen (ruta de la carpeta que contiene los html - webcontent / publichtml)
     * @return nombre de la imagen con ruta relativa para pintarla en el html
     * @throws Exception
     */
    public static String guardaImagenEnDisco(InputStream imagenBuffer, String extra, String path) throws Exception {
        File fichero = null;
        String nombre = "";
        try {
            Calendar fe = new GregorianCalendar();
            nombre = extra + fe.getTimeInMillis() + ".jpg";
            fichero = new File(path + "/temp/" + nombre);
            BufferedInputStream in = new BufferedInputStream(imagenBuffer);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fichero));
            byte[] bytes = new byte[8096];
            int len = 0;
            while ((len = in.read(bytes)) > 0) {
                out.write(bytes, 0, len);
            }
            out.flush();
            out.close();
            in.close();
            System.out.println("archivo grabado en : " + fichero.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("ERROR al escribir en disco " + e.getMessage());
        }
        return "temp/" + nombre;
    }

    public static String descripcionSexo(String sexo) throws Exception {
        if (nullToBlank(sexo).equals("F")) {
            return "Femenino";
        }
        if (nullToBlank(sexo).equals("M")) {
            return "Masculino";
        }
        return "";
    }

    public static Integer nullToZero(Object texto) {
        try {
            if (texto == null) {
                return 0;
            }
            return Integer.parseInt(texto.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public static String hash(String clear) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(clear.getBytes());
        int size = b.length;
        StringBuffer h = new StringBuffer(size);
        for (int i = 0; i < size; i++) {
            int u = b[i] & 255;
            if (u < 16) {
                h.append("0" + Integer.toHexString(u));
            } else {
                h.append(Integer.toHexString(u));
            }
        }
        return h.toString();
    }

    public static String decimalFormat(double decimal) {
        DecimalFormat formateador = new DecimalFormat("######.##");
        return formateador.format(decimal);
    }

    public static boolean isNumber(Object texto) {
        try {
            Integer.parseInt(nullToBlank(texto.toString()));
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
