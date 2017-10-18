package com.yubarta.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xml.serialize.HTMLdtd;
import org.w3c.dom.Element;
import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;

/**
 *   Clase con utilidades diversas
 *
 */
public class Misc {

    private static SimpleDateFormat mySQLDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static SimpleDateFormat mySQLDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static SimpleDateFormat autoSecretaryDateFormat = new SimpleDateFormat("dd/MM/yyyy");

    private static SimpleDateFormat spanishDateFormat = new SimpleDateFormat("dd-MM-yyyy");

    private static SimpleDateFormat spanishDateAndHourFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    private static SimpleDateFormat forumDateTimeFormat = new SimpleDateFormat("dd-MM-yyyy EEE HH:MM");

    private static SimpleDateFormat longForumDateTimeFormat = new SimpleDateFormat("EEEEEEEEE dd-MM-yyyy 'a las' HH:MM 'h'");

    private static SimpleDateFormat sessionLogDateTimeFormat = new SimpleDateFormat("EEEE' 'dd/MM/yyyy', 'HH:mm");

    private static SimpleDateFormat examLogDateFormat = new SimpleDateFormat("EEEE' 'dd/MM/yyyy");

    /**
     *   Calcula la diferencia entre dos fechas
     *   La diferencia entre el 2 de marzo y el 2 de marzo es 0 dias
     *   La diferencia entre el 2 de marzo y el 3 de marzo es 1 dia
     *   @param f1 fecha inicial
     *   @param f2 fecha final
     *   @return la diferencia en dias
     */
    public static int getDateInterval(Calendar f1, Calendar f2) {
        return (int) ((f2.getTime().getTime() - f1.getTime().getTime()) / (1000 * 60 * 60 * 24));
    }

    /**
     *   Calcula la diferencia entre dos fechas
     *   La diferencia entre el 2 de marzo y el 2 de marzo es 0 dias
     *   La diferencia entre el 2 de marzo y el 3 de marzo es 1 dia
     *   @param f1 fecha inicial
     *   @param f2 fecha final
     *   @return la diferencia en dias
     */
    public static int getDateInterval(Date f1, Date f2) {
        return (int) ((f2.getTime() - f1.getTime()) / (1000 * 60 * 60 * 24));
    }

    /**
     *   Calcula la diferencia en segundos entre dos fechas
     *   @param f1 fecha inicial
     *   @param f2 fecha final
     *   @return la diferencia en segundos
     */
    public static int getDateTimeInterval(Date f1, Date f2) {
        return (int) ((f2.getTime() - f1.getTime()) / (1000));
    }

    /**
	 *   Convierte una fecha en formato String de MySQL a objeto java.util.Date
	 */
    public static Date stringToDate(String mySQLString) throws java.text.ParseException {
        return mySQLDateFormat.parse(mySQLString);
    }

    /**
	 *   Convierte una fecha y hora en formato String de MySQL a objeto java.util.Date
	 */
    public static Date stringToDateTime(String mySQLString) throws java.text.ParseException {
        return mySQLDateTimeFormat.parse(mySQLString);
    }

    /**
	 *   Convierte una fecha en formato español a objeto java.util.Date
	 */
    public static Date spanishStringToDate(String spanishString) throws java.text.ParseException {
        return spanishDateFormat.parse(spanishString);
    }

    /**
	 *   Convierte una fecha en formato dd/mm/yyyy a objeto java.util.Date
	 *   Concretamente es el formato usado en las peticiones automaticas.
	 */
    public static Date autoStringToDate(String spanishString) throws java.text.ParseException {
        return autoSecretaryDateFormat.parse(spanishString);
    }

    /**
	 *   Convierte un objeto Date en un String con formato MySQL
	 */
    public static String dateToMySQLString(Date date) {
        return mySQLDateFormat.format(date);
    }

    /**
	 *   Convierte un objeto Date en un String con formato MySQL (con fecha y hora)
	 */
    public static String dateTimeToMySQLString(Date date) {
        return mySQLDateTimeFormat.format(date);
    }

    /**
	 *   Convierte un objeto Date en un String con formato español
	 */
    public static String dateToString(Date date) {
        return spanishDateFormat.format(date);
    }

    /**
	 *   Convierte un objeto Date en un String con formato español y hora
	 */
    public static String dateToFullString(Date date) {
        return spanishDateAndHourFormat.format(date);
    }

    public static String encode(String sIn) {
        try {
            return java.net.URLEncoder.encode(sIn, "utf-8");
        } catch (java.io.UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }

    /**
	 * Convierte un objeto Date en un String con el formato de fecha usado en el foro
	 */
    public static String dateToForumString(Date date) {
        return forumDateTimeFormat.format(date);
    }

    /**
	 * Convierte un objeto Date en un String con el formato de fecha usado en el registro de sesiones
	 */
    public static String dateToSessionLogString(Date date) {
        String dateStringAux = sessionLogDateTimeFormat.format(date);
        return (dateStringAux.substring(0, 1)).toUpperCase() + (dateStringAux.substring(1));
    }

    /**
	 * Convierte un objeto Date en un String con el formato de fecha usado en el registro de exámenes
	 */
    public static String dateToExamLogString(Date date) {
        String dateStringAux = examLogDateFormat.format(date);
        return (dateStringAux.substring(0, 1)).toUpperCase() + (dateStringAux.substring(1));
    }

    /**
	 * Convierte un objeto Date en un String con el formato largo de fecha usado en el foro
	 */
    public static String dateToLongForumString(Date date) {
        return longForumDateTimeFormat.format(date);
    }

    /**
	 *   Convierte "HTML" de Flash en HTML
	 */
    public static String flashToHTML(String flashHTML) {
        StringBuffer result = new StringBuffer();
        StringTokenizer tags = new StringTokenizer(flashHTML, "<", false);
        while (tags.hasMoreTokens()) {
            String currentTag = tags.nextToken();
            if (currentTag == null || currentTag.trim().length() == 0 || currentTag.indexOf("TEXTFORMAT") != -1) {
                continue;
            } else {
                if (currentTag.indexOf("FONT") != -1) {
                    currentTag = currentTag.replaceAll("_sans", "sans-serif");
                    currentTag = currentTag.replaceAll("SIZE=\"9\"", "SIZE=\"1\"");
                    currentTag = currentTag.replaceAll("SIZE=\"12\"", "SIZE=\"2\"");
                    currentTag = currentTag.replaceAll("SIZE=\"14\"", "SIZE=\"3\"");
                    currentTag = currentTag.replaceAll("SIZE=\"16\"", "SIZE=\"4\"");
                    currentTag = currentTag.replaceAll("SIZE=\"18\"", "SIZE=\"5\"");
                    currentTag = currentTag.replaceAll("SIZE=\"22\"", "SIZE=\"6\"");
                }
                currentTag = currentTag.replaceAll("&apos;", "'");
                result.append("<");
                result.append(currentTag);
            }
        }
        String res = result.toString();
        res = res.replaceAll("á", "&aacute;").replaceAll("é", "&eacute;").replaceAll("í", "&iacute;").replaceAll("ó", "&oacute;").replaceAll("ú", "&uacute;");
        res = res.replaceAll("Á", "&Aacute;").replaceAll("É", "&Eacute;").replaceAll("Í", "&Iacute;").replaceAll("Ó", "&Oacute;").replaceAll("Ú", "&Uacute;");
        res = res.replaceAll("ñ", "&ntilde;").replaceAll("Ñ", "&Ntilde;");
        return res;
    }

    public static String HTMLencode(String sIn) {
        return sIn.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
    }

    public static String xmlEnc(String sIn) {
        if (sIn == null) {
            return null;
        }
        return sIn.replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("'", "&#39;").replaceAll("\"", "&quot;");
    }

    public static String htmlEncode(String sIn) {
        return HTMLencode(sIn);
    }

    /**
	 *  Quita simbolos no permitidos en ciertos Strings
	 */
    public static String clearQuotes(String sIn) {
        return sIn.replaceAll("<", "").replaceAll(">", "").replaceAll("\"", "").replaceAll("\'", "");
    }

    public static String jsEncode(String sIn) {
        return sIn.replaceAll("\"", "\\\"").replaceAll("\'", "\\\'").replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r");
    }

    public static String sqlLikeProcess(String sIn) {
        return sIn.replaceAll("á", "_").replaceAll("é", "_").replaceAll("í", "_").replaceAll("ó", "_").replaceAll("ú", "_").replaceAll("Á", "_").replaceAll("É", "_").replaceAll("Í", "_").replaceAll("Ó", "_").replaceAll("Ú", "_");
    }

    public static String brEncode(String sIn) {
        return htmlEncode(sIn).replaceAll("\n", "<br/>").replaceAll("\t", "&nbsp;");
    }

    /**
	 *  Utiliza la libreria JTidy para limpiar el XHTML.
	 * 
	 * TODO: poner bien esto
	 * Versión cutre-marrana: usa un serializador propio para evitar
	 * que tidy genere character entities como & ntilde ; y cosas asi
	 */
    public static String tidyHtml(String content) {
        if (content.trim().length() == 0) {
            return "";
        }
        content = content.replaceAll("&apos;", "&#39;");
        Tidy tidy = new Tidy();
        tidy.setDropEmptyParas(true);
        tidy.setSmartIndent(false);
        tidy.setXHTML(true);
        tidy.setWord2000(true);
        org.w3c.dom.Document body = tidy.parseDOM(new ByteArrayInputStream(content.getBytes()), null);
        body.removeChild(body.getDoctype());
        Element elBody = (Element) body.getElementsByTagName("body").item(0);
        String sXml = XmlExplorer.getElementContent(elBody);
        return sXml;
    }

    /**
	 *   Procesa el código único de usuario para poderlos comparar
	 *   Las letras las pone en mayúsculas, los números los deja igual
	 *   y todos los demás caracteres los elimina.
	 */
    public static String processCode(String sIn) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < sIn.length(); i++) {
            char s = sIn.charAt(i);
            if (s >= '0' && s <= '9') {
                result.append(s);
            }
            if (s >= 'A' && s <= 'Z') {
                result.append(s);
            }
            if (s >= 'a' && s <= 'z') {
                result.append((s + "").toUpperCase());
            }
            if (s == 'ñ' || s == 'Ñ') {
                result.append('Ñ');
            }
        }
        return result.toString();
    }

    /**
	 *   Lee un archivo y devuelve su contenido en un String
	 *
	 *   FIXME: al final del archivo devuelve un \n aunque no lo tenga
	 */
    public static String readFile(String fileName) throws FileNotFoundException, IOException {
        BufferedReader inputFile;
        inputFile = new BufferedReader(new FileReader(fileName));
        StringWriter outputString = new StringWriter();
        while (inputFile.ready()) {
            outputString.write(inputFile.readLine());
            outputString.write("\n");
        }
        inputFile.close();
        String content = outputString.toString();
        outputString.close();
        return content;
    }

    /**
	 *   Lee un archivo y devuelve su contenido en un String
	 */
    public static String readFile(File theFile) throws FileNotFoundException, IOException {
        BufferedReader inputFile;
        inputFile = new BufferedReader(new FileReader(theFile));
        StringWriter outputString = new StringWriter();
        while (inputFile.ready()) {
            outputString.write(inputFile.readLine());
        }
        inputFile.close();
        String content = outputString.toString();
        outputString.close();
        return content;
    }

    /**
	 *   Escribe el contenido de un String en un archivo
	 */
    public static void writeFile(String fileName, String content) throws FileNotFoundException, IOException {
        BufferedWriter outputFile;
        outputFile = new BufferedWriter(new FileWriter(fileName));
        outputFile.write(content, 0, content.length());
        outputFile.close();
    }

    public static void copyFile(File source, File target) throws FileNotFoundException, IOException {
        InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(target);
        copyStream(in, out);
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte buf[] = new byte[1024];
        int nRead;
        do {
            nRead = in.read(buf);
            if (nRead > 0) {
                out.write(buf, 0, nRead);
            }
        } while (nRead > 0);
        out.close();
        in.close();
    }

    /**
	 *  Codifica un String mediante el algoritmo MD5
	 */
    public static String md5Encode(String pass) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(pass.getBytes());
            byte[] result = md.digest();
            return bytes2hexStr(result);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("La librería java.security no implemente MD5");
        }
    }

    /**
     *  Generate a hex string
     */
    private static String bytes2hexStr(byte[] arr) {
        char[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        int len = arr.length;
        StringBuffer sb = new StringBuffer(len * 2);
        for (int i = 0; i < len; i++) {
            int hi = (arr[i] >>> 4) & 0xf;
            sb.append(hex[hi]);
            int low = (arr[i]) & 0xf;
            sb.append(hex[low]);
        }
        return sb.toString();
    }

    /**
	 *  Genera un nombre de usuario a partir del nombre, según
	 *  la siguiente idea:
	 *     José Miguel Samper Sánchez ----> jmiguelss
	 *     David García Cerezo -----------> dgarciac
	 *     César Sancho Arribas ----------> csanchoa
	 */
    public static String buildLogin(String name) {
        StringBuffer sb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(name.trim().toLowerCase(), " ", false);
        if (st.countTokens() == 0) {
            throw new RuntimeException("Name is null");
        }
        if (st.countTokens() == 1) {
            sb.append(st.nextToken());
        } else {
            sb.append(st.nextToken().charAt(0));
            sb.append(st.nextToken());
            while (st.hasMoreTokens()) {
                sb.append(st.nextToken().charAt(0));
            }
        }
        String login = sb.toString();
        login = login.replaceAll("á", "a").replaceAll("é", "e").replaceAll("í", "i").replaceAll("ó", "o").replaceAll("ú", "u");
        return login;
    }

    /**
	 *  Genera un password aleatorio con el siguiente patrón:
	 *    consonante vocal consonante vocal consonante vocal
	 */
    public static String buildPassword() {
        char[] consonents = { 'b', 'c', 'd', 'f', 'g', 'h', 'j', 'l', 'm', 'n', 'ñ', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z' };
        char[] vowels = { 'a', 'e', 'i', 'o', 'u' };
        StringBuffer sb = new StringBuffer();
        sb.append(consonents[(int) Math.floor(Math.random() * consonents.length)]);
        sb.append(vowels[(int) Math.floor(Math.random() * vowels.length)]);
        sb.append(consonents[(int) Math.floor(Math.random() * consonents.length)]);
        sb.append(vowels[(int) Math.floor(Math.random() * vowels.length)]);
        sb.append(consonents[(int) Math.floor(Math.random() * consonents.length)]);
        sb.append(vowels[(int) Math.floor(Math.random() * vowels.length)]);
        return sb.toString();
    }

    /**
	 *  Genera una cadena aleatoria para poderla utilizar como parámetro en
	 *  urls para evitar caches.
	 *  Por ejemplo, en
	 *         menu.jsp?rand=sl2dk3jsld64jd2
	 */
    public static String getRandomString() {
        Random rnd = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 16; i++) {
            sb.append(Character.forDigit(rnd.nextInt(36), 36));
        }
        return sb.toString();
    }

    /**
	 *   Construye una URL a partir de
	 *     @param sPage Nombre de la página
	 *     @param hParams parámetros pasados por GET
	 *   También le añade el contextPath
	 *   Además le añade opcionalmente un parámetro aleatorio
	 *     @param random
	 *   Y codifica la URL para URL-rewriting
	 */
    public static String buildLink(String sPage, Hashtable hParams, boolean random, PageContext pageContext) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
        StringBuffer sQuery = new StringBuffer();
        sQuery.append(request.getContextPath());
        sQuery.append("/");
        sQuery.append(sPage);
        sQuery.append("?");
        if (hParams != null && hParams.size() > 0) {
            Enumeration eKeys = hParams.keys();
            while (eKeys.hasMoreElements()) {
                String sN = (String) eKeys.nextElement();
                sQuery.append(sN);
                sQuery.append("=");
                sQuery.append((String) hParams.get(sN));
                sQuery.append("&");
            }
        }
        if (random) {
            sQuery.append("rand=");
            sQuery.append(Misc.getRandomString());
        } else {
            sQuery.deleteCharAt(sQuery.length() - 1);
        }
        return response.encodeURL(sQuery.toString());
    }

    /**
     *  Devuelve un String de la forma "HH horas, MM minutos y SS segundos".
     */
    public static String secondsToHHMMSS(int seconds) {
        int hour = (int) (seconds / (60 * 60));
        int min = ((int) (seconds / 60)) - (hour * 60);
        int sec = (int) (seconds - (hour * 60 * 60) - (min * 60));
        String hourString = String.valueOf(hour);
        String minString = String.valueOf(min);
        String secString = String.valueOf(sec);
        if (hourString.length() == 1) {
            hourString = "0" + hourString;
        }
        if (minString.length() == 1) {
            minString = "0" + minString;
        }
        if (secString.length() == 1) {
            secString = "0" + secString;
        }
        return hourString + " horas, " + minString + " minutos y " + secString + " segundos";
    }

    /**
     * Devuelve un String de la forma "HHh, MMm, SSs".
     */
    public static String secondsToShortHHMMSS(int seconds) {
        int hour = (int) (seconds / (60 * 60));
        int min = ((int) (seconds / 60)) - (hour * 60);
        int sec = (int) (seconds - (hour * 60 * 60) - (min * 60));
        String hourString = String.valueOf(hour);
        String minString = String.valueOf(min);
        String secString = String.valueOf(sec);
        if (hourString.length() == 1) {
            hourString = "0" + hourString;
        }
        if (minString.length() == 1) {
            minString = "0" + minString;
        }
        if (secString.length() == 1) {
            secString = "0" + secString;
        }
        return hourString + "h, " + minString + "min, " + secString + "seg";
    }

    /**
     * Devuelve un String de la forma "HHh, MMm, SSs".
     */
    public static String secondsToTinyHHMMSS(int seconds) {
        int hour = (int) (seconds / (60 * 60));
        int min = ((int) (seconds / 60)) - (hour * 60);
        int sec = (int) (seconds - (hour * 60 * 60) - (min * 60));
        String hourString = String.valueOf(hour);
        String minString = String.valueOf(min);
        String secString = String.valueOf(sec);
        if (hourString.length() == 1) {
            hourString = "0" + hourString;
        }
        if (minString.length() == 1) {
            minString = "0" + minString;
        }
        if (secString.length() == 1) {
            secString = "0" + secString;
        }
        return hourString + "h, " + minString + "m, " + secString + "s";
    }

    /**
     * Devuelve un String de la forma "HHh, MMm".
     */
    public static String secondsToTinyHHMM(int seconds) {
        int hour = (int) (seconds / (60 * 60));
        int min = ((int) (seconds / 60)) - (hour * 60);
        String hourString = String.valueOf(hour);
        String minString = String.valueOf(min);
        if (hourString.length() == 1) {
            hourString = "0" + hourString;
        }
        if (minString.length() == 1) {
            minString = "0" + minString;
        }
        return hourString + "h, " + minString + "m";
    }

    /**
     * recoge un parámetro y lo parsea en un entero.
     **/
    public static int getInt(javax.servlet.ServletRequest req, String sParamName) {
        String sVal = req.getParameter(sParamName);
        if (sVal == null) {
            throw new RuntimeException("Error en parámetro -" + sParamName + "-, se esperaba un número entero (null).");
        }
        try {
            return Integer.parseInt(sVal);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Error en parámetro -" + sParamName + "-, se esperaba un número entero (" + sVal + ")");
        }
    }

    /**
     * Construye un nombre de fichero 'parecido' al propuesto.
     */
    public static String makeFileName(String sIn) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < sIn.length(); i++) {
            sb.append(niceCharForFileName(sIn.charAt(i)));
        }
        return sb.toString();
    }

    /**
     * Filtra caracteres no alfanuméricos y letras con tilde
     */
    private static char niceCharForFileName(char cc) {
        if (!Character.isLetterOrDigit(cc)) {
            return '_';
        }
        if (cc >= 'a' && cc <= 'z') {
            return cc;
        }
        if (cc >= 'A' && cc <= 'Z') {
            return cc;
        }
        if (cc >= '0' && cc <= '9') {
            return cc;
        }
        switch(cc) {
            case 'à':
            case 'ä':
            case 'á':
            case 'ã':
                return 'a';
            case 'è':
            case 'ë':
            case 'é':
                return 'e';
            case 'ì':
            case 'ï':
            case 'í':
                return 'i';
            case 'ò':
            case 'ö':
            case 'ó':
            case 'õ':
                return 'o';
            case 'ù':
            case 'ü':
            case 'ú':
                return 'u';
            case 'À':
            case 'Ä':
            case 'Á':
            case 'Ã':
                return 'A';
            case 'È':
            case 'Ë':
            case 'É':
                return 'E';
            case 'Ì':
            case 'Ï':
            case 'Í':
                return 'I';
            case 'Ò':
            case 'Ö':
            case 'Ó':
            case 'Õ':
                return 'O';
            case 'Ù':
            case 'Ü':
            case 'Ú':
                return 'U';
            case 'Ñ':
                return 'N';
            case 'ñ':
                return 'n';
        }
        return '_';
    }

    public static String fckToXml(String sIn) {
        String sOut = replaceHtmlEntities(sIn);
        verifyWellFormedXmlFragment(sOut);
        return sOut;
    }

    private static String replaceHtmlEntities(String sIn) {
        Pattern pp = Pattern.compile("&([a-zA-Z]+);");
        Matcher mm = pp.matcher(sIn);
        StringBuffer out = new StringBuffer();
        while (mm.find()) {
            mm.appendReplacement(out, replaceHtmlEntity(mm.group(1)));
        }
        mm.appendTail(out);
        return out.toString();
    }

    private static String replaceHtmlEntity(String sIn) {
        int nUnicode = HTMLdtd.charFromName(sIn);
        return (nUnicode == -1) ? ("&" + sIn + ";") : ("&#" + nUnicode + ";");
    }

    /**
     * Lanza una RuntimeException si la entrada no es un fragmento de XML válido
     */
    public static void verifyWellFormedXmlFragment(String sIn) {
        try {
            String sEnc = "UTF-8";
            String sXml = "<?xml version='1.0' encoding='" + sEnc + "' ?>" + "<ga>" + sIn + "</ga>";
            InputStream inDoc = new ByteArrayInputStream(sXml.getBytes(sEnc));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            docBuilder.parse(inDoc);
        } catch (ParserConfigurationException pce) {
        } catch (IOException ioe) {
        } catch (SAXException se) {
            throw new RuntimeException("Error validando XML: " + se.getMessage());
        }
    }
}
