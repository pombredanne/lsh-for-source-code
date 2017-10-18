package de.laidback.racoon.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.util.Random;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;
import de.laidback.racoon.exceptions.RacoonFileAccessException;

/**
 * Sammlung von n�tzlichen Methoden, (Singleton)
 * 
 * @author Thomas Berger
 * 
 */
public class Tools {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(Tools.class);

    private static Charset charset = Charset.forName("ISO-8859-15");

    private static CharsetDecoder decoder = charset.newDecoder();

    /**
	 * Eigene Singleton Instanz
	 */
    private static Tools instance;

    private Random rnd;

    /**
	 * Singleton Konstruktor
	 * 
	 * @return
	 */
    public static Tools getInstance() {
        if (instance == null) {
            instance = new Tools();
        }
        return instance;
    }

    /**
	 * 
	 * Prvater Konstruktor
	 */
    private Tools() {
        rnd = new Random();
    }

    /**
	 * Parst einen String als int und f�ngt
	 * dabei alle Excpetions ab. Im Falle eines
	 * Fehlers wird -1 zur�ckgeliefert.
	 * 
	 * @param value Der Wert
	 * @return der wert als int oder -1
	 */
    public int parseInt(String value) {
        if (isEmpty(value)) return -1;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.error("parseInt()", e);
            return -1;
        }
    }

    /**
	 * Parst einen String als long und f�ngt
	 * dabei alle Excpetions ab. Im Falle eines
	 * Fehlers wird -1 zur�ckgeliefert.
	 * 
	 * @param value Der Wert
	 * @return der wert als int oder -1
	 */
    public long parselong(String value) {
        if (isEmpty(value)) return -1;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.error("parseInt()", e);
            return -1;
        }
    }

    /**
	 * Parst einen String als long und f�ngt
	 * dabei alle Excpetions ab. Im Falle eines
	 * Fehlers wird null zur�ckgeliefert.
	 * 
	 * @param value Der Wert
	 * @return der wert als int oder -1
	 */
    public Long parseLong(String value) {
        if (isEmpty(value)) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.error("parseInt()", e);
            return null;
        }
    }

    /**
	 * Liefert den Dateipfad einer Ressource.
	 * 
	 * @param resource
	 * @return
	 */
    public String getResourceFilePath(String resource) {
        ClassLoader loader = Tools.class.getClassLoader();
        URL url = loader.getResource(resource);
        if (url != null) {
            return url.getFile();
        } else {
            return null;
        }
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.error("delay method was interrupted !");
        }
    }

    /**
	 * L�scht eine Datei.
	 * 
	 * @param filename
	 * @return
	 */
    public boolean deleteFile(String filename) {
        File f = new File(filename);
        return f.delete();
    }

    /**
	 * Berechnet die MD5 Pr�fsumme eines Strings. Gibt null
	 * zur�ck im Falle eines Fehlers.
	 * 
	 * @param input Der String von dem der MD5 Hashcode ermittelt werden soll
	 * @return Die Pr�fsumme oder null
	 */
    public String md5(String input) {
        StringBuffer sb = new StringBuffer();
        String s = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte digest[] = md.digest(input.getBytes());
            for (int i = 0; i < digest.length; i++) {
                s = Integer.toHexString(digest[i] & 0xFF);
                if (s.length() == 1) {
                    sb.append("0");
                    sb.append(s);
                } else sb.append(s);
            }
        } catch (NoSuchAlgorithmException e) {
            logger.error("md5(String)", e);
            return null;
        }
        return sb.toString();
    }

    /**
	 * Liest eine Datei in einen String ein, die Datei wird dabei �ber
	 * einen NIO Channel komplett in den Speicher gemappt, aus dem �ber
	 * Unter Zuhilfenahme des definierten Charsets den String erstellt wird.
	 * 
	 * @param filename Der Pfad zu Datei.
	 * @return Gibt einen String mit dem Dateiinhalt zur�ck.
	 */
    public String readFile(String filename) {
        if (logger.isTraceEnabled()) {
            logger.trace("readFile(" + filename + ") - start");
        }
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(filename);
            FileChannel fChannel = fstream.getChannel();
            int size = (int) fChannel.size();
            CharBuffer cb = decoder.decode(fChannel.map(FileChannel.MapMode.READ_ONLY, 0, size));
            String returnString = cb.toString();
            if (logger.isTraceEnabled()) {
                logger.trace("readFile(" + filename + ") - end");
            }
            return returnString;
        } catch (FileNotFoundException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("readFile(" + filename + ") - end");
        }
        return "";
    }

    /**
	 * Schreibt den Inhalt des �bergebenen Strings in eine Datei.
	 * 
	 * @param fileName Der Dateiname mit Pfad
	 * @param content Der zu schreibende String.
	 * @return Gibt true zur�ck, wenn die Operation erfolgreich war.
	 */
    public boolean writeFile(String fileName, String content) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(fileName));
            out.write(content);
        } catch (IOException e) {
            logger.error("writeFile(String, String)", e);
            return false;
        }
        closeStream(out);
        return true;
    }

    /**
	 * Erzeugt einen gepufferten FileWriter f�r die angegebene Datei. Schl�gt dies fehl,
	 * wird NULL zur�ckgegeben.
	 * 
	 * @param fileName Der Dateiname
	 * @return Der gepufferte FileWriter
	 */
    public Writer getFileWriter(String fileName) {
        try {
            return new BufferedWriter(new FileWriter(fileName));
        } catch (IOException e) {
            logger.error("Error getting Filewriter for file " + fileName, e);
            return null;
        }
    }

    /**
	 * Liest eine Datei in einen ByteArray ein, die Datei wird dabei �ber
	 * einen NIO Channel komplett in den Speicher gemappt. Bei Auftreten einer
	 * FileNotFoundExceptions oder IOException wird eine RacoonFileAccessException
	 * geworfen.
	 * 
	 * @param filename Der Pfad zu Datei.
	 * @return Gibt einen ByteArray mit dem Dateiinhalt zur�ck.
	 */
    public byte[] readFileIntoByteArray(String filename) throws RacoonFileAccessException {
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(filename);
            FileChannel fChannel = fstream.getChannel();
            int size = (int) fChannel.size();
            return fChannel.map(FileChannel.MapMode.READ_ONLY, 0, size).array();
        } catch (FileNotFoundException e) {
            logger.error("readFileIntoByteArray(String)", e);
            throw new RacoonFileAccessException("File not found", filename);
        } catch (IOException e) {
            logger.error("readFileIntoByteArray(String)", e);
            throw new RacoonFileAccessException("File not found", filename);
        }
    }

    /**
	 * Liefert alle Dateien mit der angegebenen Extension im angegebenen
	 * Verzeichnis zur�ck.
	 * 
	 * @param path
	 * @param filterExtension
	 * @return
	 */
    public File[] getFiles(String path, String filterExtension) {
        if (!dirExists(path)) return null;
        File dir = new File(path);
        return dir.listFiles(new MyFileFilter(filterExtension));
    }

    /**
	 * Liefert die absoltuten Pfade der in einem Verzeichnis
	 * enthaltenen Dateien in einem String Array zur�ck.
	 * 
	 * @param path
	 * @param filterExtension
	 * @return
	 */
    public String[] getFileNames(String path, String filterExtension) {
        File[] files = getFiles(path, filterExtension);
        if (files == null) {
            return new String[0];
        }
        String[] s = new String[files.length];
        int i = 0;
        for (File f : files) {
            if (f.isFile()) s[i++] = f.getAbsolutePath();
        }
        return trimStringArray(s);
    }

    public Format getDateFormat(String format) {
        return FastDateFormat.getInstance(format);
    }

    public String getDateFormatted(long date, String format) {
        return getDateFormat(format).format(date);
    }

    /**
	 * Pr�ft ob ein Verzeichnis existiert. Alle
	 * Exceptions werden abgefangen.
	 * 
	 * @param path Der Pfad zum Verzeichnis
	 * @return Liefert true wenn das Verzeichnis existiert
	 */
    public boolean dirExists(String path) {
        if (isEmpty(path)) return false;
        File dir = new File(path);
        return (dir.exists() && dir.isDirectory());
    }

    /**
	 * �berpr�ft ob ein String null oder leer ist
	 * 
	 * @param value Der zu p�rufende String
	 * @return Gibt true zur�ck, wenn der String null oder leer ist.
	 */
    public boolean isEmpty(String value) {
        return (value == null || value.length() == 0);
    }

    /**
	 * Schliesst einen Stream und setzt das Object auf null.
	 * Dabei werden alle m�glichen Exceptions abgefangen und
	 * geloggt.
	 * 
	 * @param writer
	 */
    public void closeStream(Writer writer) {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException e) {
            logger.error("closeStream(Writer)", e);
        }
        writer = null;
    }

    /**
	 * Schliesst einen Stream und setzt das Object auf null.
	 * Dabei werden alle m�glichen Exceptions abgefangen und
	 * geloggt.
	 * 
	 * @param writer
	 */
    public void closeStream(Reader reader) {
        if (reader == null) return;
        try {
            reader.close();
        } catch (IOException e) {
            logger.error("closeStream(Reader)", e);
        }
        reader = null;
    }

    /**
	 * Schliesst einen Stream und setzt das Object auf null.
	 * Dabei werden alle m�glichen Exceptions abgefangen und
	 * geloggt.
	 * 
	 * @param writer
	 */
    public void closeStream(InputStream is) {
        if (is == null) return;
        try {
            is.close();
        } catch (IOException e) {
            logger.error("closeStream(InputStream)", e);
        }
        is = null;
    }

    /**
     * Strips an array at the first position where
     * one string is null.
     * 
     * Attention: Use carefully because the array
     * is also trimmed when there are not empty
     * values behind the null entry...
     * 
     * @param array The array
     * @return the trimmed array.
     */
    public Object[] trimArray(Object[] array) {
        if (array == null || array.length == 0) return array;
        Object[] newArray = null;
        int c = 0;
        try {
            while (array[c] != null) c++;
        } catch (Exception e) {
        }
        try {
            newArray = new Object[c];
            System.arraycopy(array, 0, newArray, 0, c);
        } catch (Exception e) {
            logger.error("Error at trimArray(Object[]): c=" + c + ", source array length is " + array.length, e);
            return array;
        }
        return newArray;
    }

    /**
     * Liefert das aktuelle Arbeitsverzeichnis der Anwendung.
     * 
     * @return Absoluter Pfad
     */
    public String getWorkingDir() {
        return new File(".").getAbsolutePath();
    }

    /**
     * Dimensioniert einen String Array neu, so dass keine
     * null-Werte enthalten sind.
     * 
     * @param array
     * @return
     */
    public String[] trimStringArray(String[] array) {
        if (array == null || array.length == 0) return array;
        String[] newArray = null;
        int c = 0;
        try {
            while (array[c] != null) c++;
        } catch (Exception e) {
        }
        try {
            newArray = new String[c];
            System.arraycopy(array, 0, newArray, 0, c);
        } catch (Exception e) {
            logger.error("Error at trimArray(String[]): c=" + c + ", source array length is " + array.length, e);
            return array;
        }
        return newArray;
    }

    /**
     * Gibt den Stack Trace einer Exception als String zur�ck.
     */
    public final String getStackTrace(Throwable ex) {
        String result = "";
        if (ex != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                pw.close();
                sw.close();
                result = sw.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Generiert eine Zufallszahl vom Typ Int.
     * @return die zufallszahl
     */
    public int generateRandomInt() {
        return rnd.nextInt();
    }
}
