package es.ua.dlsi.tradubi.main.server.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import es.ua.dlsi.tradubi.main.client.entities.UserDataEntityRPC;
import es.ua.dlsi.tradubi.main.client.exceptions.ServerErrorException;

/**
 * Server-side utility methods.
 * 
 * @author  vmsanchez
 */
public class ServerUtil {

    public static boolean mock = false;

    /**
	 * Commons-logging logger
	 */
    static Log logger = LogFactory.getLog(ServerUtil.class);

    /**
	 * Singleton instance of <code>SecureRandom</code>.
	 * Used to generate random invitation IDs and security tokens.
	 * 
	 * @uml.property  name="secureRandom"
	 */
    private static SecureRandom secureRandom = null;

    /**
	 * Removes the last resource from a URL.
	 * For example, if we apply this method to "http://foo/bar"
	 * it returns "http://foo/"
	 * 
	 * @param requestUrl URL whose last resource will be removed.
	 * @return URL without the last resource.
	 */
    public static String removelastResourceURL(String requestUrl) {
        String[] fragments = requestUrl.split("/");
        StringBuilder returnBuf = new StringBuilder();
        for (int i = 0; i < fragments.length - 1; i++) {
            returnBuf.append(fragments[i]);
            returnBuf.append("/");
        }
        return returnBuf.toString();
    }

    /**
	 * Encrypts text with SHA algorithm
	 * 
	 * @param plaintext Text to be encrypted
	 * @return SHA-encrypted text
	 * @throws EncryptionException If SHA is not supported by the system.
	 */
    public static String encrypt(String plaintext) throws EncryptionException {
        if (plaintext == null) {
            throw new EncryptionException();
        }
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(plaintext.getBytes("UTF-8"));
            return Base64.encodeBytes(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(e);
        } catch (UnsupportedEncodingException e) {
            throw new EncryptionException(e);
        }
    }

    /**
	 * Puts a error message in the HTPP request object
	 * @param error Error description
	 * @param request HTTP request object.
	 */
    public static void putErrorRequest(String error, HttpServletRequest request) {
        request.setAttribute("error", error);
    }

    /**
	 * Checks if the HTTP request has a parameter called "token", and if its value
	 * is equals to the value returned by the method <code>getToken</code> of the session
	 * object called "user".
	 * @param req HTTP request
	 * @throws ServletException If the token values don't match
	 */
    public static void checkToken(HttpServletRequest req) throws ServletException {
        String parToken = req.getParameter("token");
        UserDataEntityRPC user = (UserDataEntityRPC) req.getSession().getAttribute("user");
        if (parToken != null && user != null) {
            if (!parToken.equals(user.getToken())) throw new ServletException("Wrong token");
        } else throw new ServletException("Wrong token");
    }

    /**
	 * Gets the list of languages supported by the user interface
	 * 
	 * @return List of languages supported by the user interface
	 */
    public static List<String> getSupportedLanguages() {
        List<String> ret = new ArrayList<String>();
        try {
            InputStream is = ServerUtil.class.getResourceAsStream("/configuration.properties");
            Properties props = new Properties();
            props.load(is);
            String strlangs = props.getProperty("languages").trim();
            String[] langs = strlangs.split(",");
            if (langs != null) for (String l : langs) {
                ret.add(l.trim());
            }
            is.close();
        } catch (Exception e) {
            logger.error("Can't read supported languages from file", e);
        }
        return ret;
    }

    /**
	 * Reads property from configuration.properties
	 * 
	 * @param property
	 *            Property name
	 * @return Value of the property, or null if the property is not found
	 */
    public static String readProperty(String property) {
        String value = null;
        try {
            InputStream is = ServerUtil.class.getResourceAsStream("/configuration.properties");
            Properties props = new Properties();
            props.load(is);
            value = props.getProperty(property).trim();
        } catch (Exception e) {
        }
        return value;
    }

    /**
	 * Sends an email through the default SMTP server.
	 * 
	 * @param to To field
	 * @param from From field
	 * @param subject Subject
	 * @param body Message contents
	 * @throws ServerErrorException If there is an unexpected error
	 */
    public static void sendEmail(String to, String from, String subject, String body) throws ServerErrorException {
        if (!mock) sendEmail(readProperty("mail_smtp_server"), to, from, subject, body);
    }

    /**
	 * Sends an email through the given SMTP server.
	 * 
	 * @param smtpServer SMTP server.
	 * @param to To field
	 * @param from From field
	 * @param subject Subject
	 * @param body Message contents
	 * @throws ServerErrorException If there is an unexpected error
	 */
    private static void sendEmail(String smtpServer, String to, String from, String subject, String body) throws ServerErrorException {
        try {
            Properties props = System.getProperties();
            props.put("mail.smtp.host", smtpServer);
            Session session = Session.getDefaultInstance(props, null);
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            msg.setSubject(subject);
            msg.setText(body);
            msg.setHeader("X-Mailer", "LOTONtechEmail");
            msg.setSentDate(new Date());
            Transport.send(msg);
            logger.debug("Message sent OK.");
        } catch (Exception ex) {
            logger.error("Can't send email", ex);
            throw new ServerErrorException("Can't send email", ex);
        }
    }

    /**
	 * 
	 * Detects user locale
	 * 
	 * If the user has clicked in the language toolbar to change the language,
	 * it is stored in a cookie
	 * 
	 * If the user hasn't change the language, get locale from accept-language
	 * http header
	 * 
	 * @param request
	 * @return Detected language. Value is a "Primary-tag" from RFC1766. For
	 *         instance, en, es..
	 */
    public static String detectLocale(HttpServletRequest request) {
        String locale = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) {
            if (cookie.getName().equals("locale")) {
                locale = cookie.getValue();
            }
        }
        if (locale == null) {
            locale = request.getLocale().getLanguage();
        }
        return locale;
    }

    public static List<String> detectLocales(HttpServletRequest request) {
        List<String> locales = new ArrayList<String>();
        String locale = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) {
            if (cookie.getName().equals("locale")) {
                locale = cookie.getValue();
                locales.add(locale);
            }
        }
        Enumeration localEnum = request.getLocales();
        while (localEnum.hasMoreElements()) {
            Locale l = (Locale) localEnum.nextElement();
            locales.add(l.getLanguage());
        }
        return locales;
    }

    /**
	 * Converts a plain text String into HTML
	 * @param text Plain text
	 * @return HTM conversion of text
	 */
    public static String textToHTML(String text) {
        return StringEscapeUtils.escapeHtml(text).replaceAll("\n", "<br>\n");
    }

    /**
	 * Converts a HTMLt String into text
	 * @param html HTML code
	 * @return Text conversion of HTML
	 */
    public static String htmlToText(String html) {
        return StringEscapeUtils.unescapeHtml(html).replaceAll("<br>\n", "\n");
    }

    /**
	 * Copies an object and objects referenced by that object
	 * 
	 * @param orig
	 * @return
	 */
    public static Object deepCopy(Object orig) {
        Object obj = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(orig);
            out.flush();
            out.close();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            obj = in.readObject();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return obj;
    }

    /**
	 * Replaces non-ASCII character with its escape sequence
	 * 
	 * @param line
	 *            Input unicode text
	 * @return ASCII version of the text with escape sequences
	 */
    public static String encodeString(String line) {
        StringBuffer out = new StringBuffer();
        char[] chars = line.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char aChar = chars[i];
            if (aChar > 127) {
                out.append(String.format("\\u%04x", new Object[] { new Long((long) aChar) }));
            } else {
                out.append(aChar);
            }
        }
        return out.toString();
    }

    /**
	 * Gets the <code>SecureRandom</code> singleton instance
	 * 
	 * @return <code>SecureRandom</code> singleton instance
	 * @uml.property  name="secureRandom"
	 */
    private static SecureRandom getSecureRandom() {
        if (secureRandom == null) secureRandom = new SecureRandom();
        return secureRandom;
    }

    /**
	 * Returns a random integer number to use as token to avoid XSRF attacks
	 * 
	 * @return String representation of a random integer
	 */
    public static String generateRandomToken() {
        return new BigInteger(130, getSecureRandom()).toString(10);
    }
}
