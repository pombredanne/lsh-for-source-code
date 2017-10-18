package fr.fg.server.util;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.mortbay.jetty.webapp.WebAppContext;

public class Utilities {

    private static Pattern[] accents;

    private static String[] replacements;

    private static final String HEX_DIGITS = "0123456789abcdef";

    private static final Random random = new Random();

    public static final int SEC_IN_DAY = 86400;

    static {
        accents = new Pattern[] { Pattern.compile("[ÀÁÂÃÄÅàáâãäå]"), Pattern.compile("[Ææ]"), Pattern.compile("[Çç]"), Pattern.compile("[Ðđ]"), Pattern.compile("[ÈÉÊËèéêë]"), Pattern.compile("[ÌÍÎÏìíîï]"), Pattern.compile("[Ññ]"), Pattern.compile("[Œœ]"), Pattern.compile("[ÒÓÔÕÖØðñòóôõöø]"), Pattern.compile("[ÙÚÛÜùúûü]"), Pattern.compile("[ÝŸýÿ]"), Pattern.compile("[ß]") };
        replacements = new String[] { "a", "ae", "c", "d", "e", "i", "n", "oe", "o", "u", "y", "ss" };
    }

    public static String getCreditsResourceImg() {
        return "<img class=\"resource credits\" src=\"" + Config.getMediaURL() + "images/misc/blank.gif\"/>";
    }

    public static String getResourceImg(int resource) {
        return "<img class=\"resource r" + resource + "\" src=\"" + Config.getMediaURL() + "images/misc/blank.gif\"/>";
    }

    public static String getXpImg() {
        return "<img class=\"resource xp\" src=\"" + Config.getMediaURL() + "images/misc/blank.gif\"/>";
    }

    public static long now() {
        return System.currentTimeMillis() / 1000;
    }

    /**
	 * Come now mais calé sur minuit
	 * @return
	 */
    public static long today() {
        long now = now();
        return (now + (SEC_IN_DAY - (now % SEC_IN_DAY))) - 1;
    }

    public static String encryptPassword(String password) throws NoSuchAlgorithmException {
        return encryptPassword(password, Config.getPasswordEncryption());
    }

    public static String encryptPassword(String password, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest hash = MessageDigest.getInstance(algorithm);
        return toHexString(hash.digest(password.getBytes()));
    }

    /**
	 * Retourne un nombre aléatoire avec une distribution de Gauss (loi
	 * normale).
	 *
	 * @param mean Moyenne de la loi normale.
	 * @param stdDev Ecart-type de la loi normale.
	 *
	 * @return Un nombre alétoire suivant la loi N(mean, stdDev).
	 */
    public static double randn(double mean, double stdDev) {
        return stdDev * random.nextGaussian() + mean;
    }

    /**
	 * Retourne un nombre pseudo-alétaoire compris entre deux bornes
	 * 
	 * @param lowerBound Borne inférieur
	 * @param upperBound Borne supérieur
	 * @return Un nombre pseudo-aléatoire
	 */
    public static int random(int lowerBound, int upperBound) {
        return (int) (Math.random() * (upperBound - lowerBound + 1)) + lowerBound;
    }

    public static String getDate(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        return sdf.format(date);
    }

    public static String getTime(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(date);
    }

    public static String getDateTime(long timestamp) {
        return Utilities.getDate(timestamp) + " " + Utilities.getTime(timestamp);
    }

    public static String escape(String string) {
        return string.replace("\"", "\\\"");
    }

    public static Class<?>[] getClasses(String packageName) {
        try {
            return getClassesByFileSystem(packageName);
        } catch (Exception e) {
        }
        try {
            return getClassesByWar(packageName);
        } catch (Exception e) {
        }
        return null;
    }

    public static String formatString(String string) {
        string = string.toLowerCase();
        for (int i = 0; i < accents.length; i++) string = accents[i].matcher(string).replaceAll(replacements[i]);
        return string;
    }

    private static Class<?>[] getClassesByWar(String packageName) {
        String resourceBase = WebAppContext.getCurrentWebAppContext() != null ? WebAppContext.getCurrentWebAppContext().getResourceBase() : "";
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        packageName = packageName.replaceAll("\\.", "/");
        try {
            URL url = new URL(resourceBase);
            JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
            JarFile jarFile = jarConnection.getJarFile();
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                if ((jarEntry.getName().contains(packageName)) && (jarEntry.getName().endsWith(".class"))) {
                    classes.add(Class.forName(jarEntry.getName().replaceAll("/", "\\.").substring(jarEntry.getName().indexOf(packageName), jarEntry.getName().length() - 6)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Class<?>[] classesA = new Class<?>[classes.size()];
        classes.toArray(classesA);
        return classesA;
    }

    private static Class<?>[] getClassesByFileSystem(String packageName) throws ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        File directory = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) throw new ClassNotFoundException("Can't get class loader.");
            String path = packageName.replace('.', '/');
            URL resource = classLoader.getResource(path);
            if (resource == null) throw new ClassNotFoundException("No resource for " + path);
            directory = new File(resource.getFile());
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(packageName + " (" + directory + ") does not appear to be a valid package");
        }
        if (directory.exists()) {
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".class") && !files[i].contains("$")) {
                    classes.add(Class.forName(packageName + '.' + files[i].substring(0, files[i].length() - 6)));
                }
            }
        } else {
            throw new ClassNotFoundException(packageName + " does not appear to be a valid package");
        }
        Class<?>[] classesA = new Class<?>[classes.size()];
        classes.toArray(classesA);
        return classesA;
    }

    private static String toHexString(byte[] v) {
        StringBuffer sb = new StringBuffer(v.length * 2);
        for (int i = 0; i < v.length; i++) {
            int b = v[i] & 0xFF;
            sb.append(HEX_DIGITS.charAt(b >>> 4)).append(HEX_DIGITS.charAt(b & 0xF));
        }
        return sb.toString();
    }
}
