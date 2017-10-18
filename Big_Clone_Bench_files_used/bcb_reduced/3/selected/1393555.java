package com.googlecode.semrs.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.apache.log4j.Logger;

/**
 * @author Roger Marin
 *
 * Clase Utilitaria que contiene diferentes metodos para la manipulaci�n
 * de imagenes, conversion de fechas, calculo de edades, generaci�n de passwords
 * y encriptaci�n entre otras.
 */
public class Util {

    public static final String READER = "reader";

    public static final String WRITER = "writer";

    public static final String DB_URL = "com.googlecode.semrs.jdbc.url";

    public static final String DB_USER = "com.googlecode.semrs.jdbc.username";

    public static final String DB_PASSWD = "com.googlecode.semrs.jdbc.password";

    public static final String DB_TYPE = "com.googlecode.semrs.jdbc.db.type";

    public static final String DB_DRIVER = "com.googlecode.semrs.jdbc.driver.name";

    public static final String RELOAD = "com.googlecode.semrs.jdbc.reload";

    public static final String NAMESPACE = "com.googlecode.semrs.namespace";

    public static final String SMTP_HOST = "com.googlecode.semrs.smtp.host";

    public static final String SMTP_USER = "com.googlecode.semrs.smtp.user";

    public static final String SMTP_PASSWORD = "com.googlecode.semrs.smtp.password";

    public static final String SMTP_EMAIL = "com.googlecode.semrs.smtp.email";

    private static final Logger log = Logger.getLogger(Util.class);

    /**
     * Metodo que recibe un password y calcula su Hash utilizando el
     * Algoritmo SHA
     * @param password
     * @return
     */
    public static String hashString(String password) {
        String hashword = null;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA");
            sha.update(password.getBytes("UTF-8"));
            BigInteger hash = new BigInteger(1, sha.digest());
            hashword = hash.toString(16);
        } catch (NoSuchAlgorithmException nsae) {
            log.error(nsae);
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }
        return pad(hashword, 32, '0');
    }

    /**
	 * Metodo utilitario para agregar padding a un string
	 * @param s
	 * @param length
	 * @param pad
	 * @return
	 */
    private static String pad(String s, int length, char pad) {
        StringBuffer buffer = new StringBuffer(s);
        while (buffer.length() < length) {
            buffer.insert(0, pad);
        }
        return buffer.toString();
    }

    /**
	 * Metodo que genera un password aleatorio
	 * @param length
	 * @return
	 */
    public static String getRandomPassword(int length) {
        StringBuffer buffer = new StringBuffer();
        Random random = new Random();
        char[] chars = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' };
        for (int i = 0; i < length; i++) {
            buffer.append(chars[random.nextInt(chars.length)]);
        }
        return buffer.toString();
    }

    /**
	 * Metodo que calcula la edad en base a una fecha de nacimiento
	 * @param dob
	 * @return
	 */
    public static int getAge(Date dob) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String[] dateSpl = format.format(dob).split("-");
        Calendar cal = new GregorianCalendar(Integer.parseInt(dateSpl[0]), Integer.parseInt(dateSpl[1]) - 1, Integer.parseInt(dateSpl[2]));
        Calendar now = new GregorianCalendar();
        int res = now.get(Calendar.YEAR) - cal.get(Calendar.YEAR);
        if ((cal.get(Calendar.MONTH) > now.get(Calendar.MONTH)) || (cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.DAY_OF_MONTH) > now.get(Calendar.DAY_OF_MONTH))) {
            res--;
        }
        return res;
    }

    /**
	 * Metodo que calcula la edad, en formato A�os, Meses y Dias en base a una fecha de 
	 * Nacimiento
	 * @param dob
	 * @return
	 */
    public static Map getAgeInYearsDaysOrMonths(Date dob) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String[] dateSpl = format.format(dob).split("-");
        int day = 1, month = 0, year = 1, ageYears = 0, ageMonths = 0, ageDays = 0;
        Calendar bd = new GregorianCalendar(Integer.parseInt(dateSpl[0]), Integer.parseInt(dateSpl[1]) - 1, Integer.parseInt(dateSpl[2]));
        Calendar cd = Calendar.getInstance();
        ageYears = cd.get(Calendar.YEAR) - bd.get(Calendar.YEAR);
        if (cd.before(new GregorianCalendar(cd.get(Calendar.YEAR), month, day))) {
            ageYears--;
            ageMonths = (12 - (bd.get(Calendar.MONTH) + 1)) + (bd.get(Calendar.MONTH));
            if (day > cd.get(Calendar.DAY_OF_MONTH)) {
                ageDays = day - cd.get(Calendar.DAY_OF_MONTH);
            } else if (day < cd.get(Calendar.DAY_OF_MONTH)) {
                ageDays = cd.get(Calendar.DAY_OF_MONTH) - day;
            } else {
                ageDays = 0;
            }
        } else if (cd.after(new GregorianCalendar(cd.get(Calendar.YEAR), month, day))) {
            ageMonths = (cd.get(Calendar.MONTH) - (bd.get(Calendar.MONTH)));
            if (day > cd.get(Calendar.DAY_OF_MONTH)) ageDays = day - cd.get(Calendar.DAY_OF_MONTH) - day; else if (day < cd.get(Calendar.DAY_OF_MONTH)) {
                ageDays = cd.get(Calendar.DAY_OF_MONTH) - day;
            } else ageDays = 0;
        } else {
            ageYears = cd.get(Calendar.YEAR) - bd.get(Calendar.YEAR);
            ageMonths = 0;
            ageDays = 0;
        }
        Map ageMap = new HashMap();
        ageMap.put("years", ageYears);
        if (ageYears > 1) {
            ageMap.put("months", 0);
            ageMap.put("days", 0);
        } else {
            ageMap.put("months", ageMonths);
            ageMap.put("days", ageDays);
        }
        return ageMap;
    }

    /**
	 * Metodo que formatea una representacion String de una Fecha al Formato XSD
	 * @param date
	 * @return
	 */
    public static String toXSDString(String date) {
        String dateString = "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat xsdFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        try {
            dateString = xsdFormat.format(sdf.parse(date)) + "Z";
        } catch (ParseException e) {
            log.error(e);
        }
        return dateString;
    }

    /**
	 * Metodo que Recibe una Fecha como String, un entero y un string representando
	 * el mes, dia o A�o y agrega o quita la cantidad suministrada a la fecha destino
	 * @param dmy
	 * @param amount
	 * @param date
	 * @return
	 */
    public static String addStringDate(String dmy, int amount, String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String[] dateSpl = null;
        try {
            dateSpl = format.format(sdf.parse(date)).split("-");
        } catch (ParseException e) {
            log.error(e);
        }
        int year = Integer.parseInt(dateSpl[0]);
        int month = Integer.parseInt(dateSpl[1]) - 1;
        int day = Integer.parseInt(dateSpl[2]);
        Calendar cal = new GregorianCalendar(year, month, day);
        if (dmy.equals("Y")) {
            cal.add(Calendar.YEAR, amount);
        } else if (dmy.equals("M")) {
            cal.add(Calendar.MONTH, amount);
        } else if (dmy.equals("D")) {
            cal.add(Calendar.DAY_OF_MONTH, amount);
        }
        String dateref = sdf.format(cal.getTime()).toString();
        return dateref;
    }

    /**
	 * Metodo utilitario para copiar bytes de un Stream a Otro
	 * @param sourceStream
	 * @param destinationStream
	 * @return
	 * @throws IOException
	 */
    public static int copyStream(InputStream sourceStream, OutputStream destinationStream) throws IOException {
        int bytesRead = 0;
        int totalBytes = 0;
        byte[] buffer = new byte[1024];
        while (bytesRead >= 0) {
            bytesRead = sourceStream.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                destinationStream.write(buffer, 0, bytesRead);
            }
            totalBytes += bytesRead;
        }
        return totalBytes;
    }

    /**
	 * Metodo Utilitario para crear una copia de una imagen con un tama�o nuevo
	 * @param originalImage
	 * @param scaledWidth
	 * @param scaledHeight
	 * @return
	 */
    public static BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight) {
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledBI.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
    }
}
