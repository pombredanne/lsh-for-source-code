package com.itth.ttraq;

import com.itth.ttraq.actionforms.TimeStampForm;
import com.itth.ttraq.om.Project;
import com.itth.ttraq.om.TimeStamp;
import com.itth.ttraq.om.User;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.apache.struts.action.DynaActionForm;
import org.hibernate.SessionFactory;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * Helpers for TTraq
 */
public class TTraqService {

    public static final int ONEDAY = 86400000;

    public static Logger logger = Logger.getLogger(TTraqService.class);

    /**
    * This keeps the current version string. This will be replaced with the value specified in the build.xml file.
    */
    private static final String VERSION = "0.9.2";

    /**
    * global configuration of TTraq
    *
    * @see TTraqServlet
    */
    private static Configuration configuration;

    /**
    * this is for determining if TTraq has inited in AUTOSETUP mode
    */
    private static boolean init;

    /**
    * this is for determining if TTraq should be AUTOSETUP via the web interface by the user
    */
    private static boolean autoSetup;

    private static Preferences preferences;

    public static SessionFactory sessionFactory;

    private static org.hibernate.cfg.Configuration configurationHibernate;

    private TTraqService() {
    }

    public static void init(Configuration configuration) {
        TTraqService.configuration = configuration;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
    * Diese Methode versucht das als String übergebene Datum zu Formatieren und meldet einen
    * Fehler wenn dies nicht möglich ist.
    * Die Methode wird im Rahmen der Validierung von TimeStampForm aufgerufen.
    *
    * @param source String der getestet werden soll, ob er ein Datum ist
    * @return wahr, wenn source ein gültiges Datum ist
    */
    public static boolean isParseableDate(String source) {
        try {
            TTraqPreferences.getFormatDate().parse(source);
            return true;
        } catch (ParseException pe) {
            return false;
        }
    }

    public static ParsedDate parseDate(String source, long offset) throws ParseException {
        Date date = TTraqPreferences.getFormatDate().parse(source);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime() + offset);
        return new ParsedDate() {

            public String getYear() {
                return String.valueOf(calendar.get(Calendar.YEAR));
            }

            public String getMonth() {
                return String.valueOf(calendar.get(Calendar.MONTH));
            }

            public String getDay() {
                return String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
            }

            public Date getDate() {
                return calendar.getTime();
            }
        };
    }

    /**
    * Parsed das in einem TimeStampForm enthaltenen Datum und kombiniert jeweils die Start-
    * und Anfangszeiten um am Ende zwei Datumsangaben zu liefern.
    */
    public static ParsedCalendars parseCalendars(TimeStampForm timeStampForm) throws ParseException {
        Date date = TTraqPreferences.getFormatDate().parse(timeStampForm.getDateBegin());
        Date dateEnd = timeStampForm.getDateEnd().trim().equals("") ? TTraqPreferences.getFormatDate().parse(timeStampForm.getDateBegin()) : TTraqPreferences.getFormatDate().parse(timeStampForm.getDateEnd());
        int hoursBegin = Integer.parseInt(timeStampForm.getHoursBegin());
        int minBegin = Integer.parseInt(timeStampForm.getMinBegin());
        int hoursEnd = Integer.parseInt(timeStampForm.getHoursEnd());
        int minEnd = Integer.parseInt(timeStampForm.getMinEnd());
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(date);
        startCalendar.set(Calendar.HOUR, hoursBegin);
        startCalendar.set(Calendar.MINUTE, minBegin);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(dateEnd);
        endCalendar.set(Calendar.HOUR, hoursEnd);
        endCalendar.set(Calendar.MINUTE, minEnd);
        return new ParsedCalendars(startCalendar, endCalendar);
    }

    public static String formatDate(Date date) {
        return TTraqPreferences.getFormatDate().format(date);
    }

    public static String formatDateFull(Date date) {
        return TTraqPreferences.getFormatDateFull().format(date);
    }

    public static double roundedCurrency(double value) {
        return Math.round(value * 100) / 100;
    }

    public static String formatCurrency(double value) {
        return TTraqPreferences.getFormatCurrency().format(value);
    }

    public static String formatTime(Date date) {
        return TTraqPreferences.getFormatTime().format(date);
    }

    public static String formatTimefilter(Date date) {
        return TTraqPreferences.getFormatTimefilter().format(date);
    }

    /**
    * sends out an email message with the TimeStamp to the user and all project admin
    *
    * @param receiver array list of receivers of this message
    * @param ts       TimeStamp which is send out
    * @throws MessagingException
    * @throws NamingException
    */
    public static void sendMail(User[] receiver, TimeStamp ts) throws Exception {
        String from = ts.getUser().getEmail() != null && !ts.getUser().getEmail().equals("") ? ts.getUser().getEmail() : configuration.getString("mail.reply");
        if (from == null) {
            from = "noreply";
        }
        long hours = (ts.getEnd().getTime() - ts.getBegin().getTime()) / 3600000;
        long minutes = (ts.getEnd().getTime() - ts.getBegin().getTime() - (hours * 3600000)) / 60000;
        String summaryView = (hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes;
        String subject = configuration.getString("mail.subject", "[ TTRAQ ]") + " " + ts.getProject().getName() + " - " + ts.getUser().getLogin() + " - " + formatDate(ts.getBegin());
        String content = formatDate(ts.getBegin()) + ": " + summaryView + "h " + formatTime(ts.getBegin()) + " - " + formatTime(ts.getEnd()) + "; " + ts.getUser().getLastName() + ", " + ts.getUser().getFirstName() + "\n" + ts.getDescription();
        Session msession = getMailSession();
        Message message = new MimeMessage(msession);
        message.setFrom(new InternetAddress(from));
        InternetAddress dests[] = new InternetAddress[receiver.length];
        for (int i = 0; i < receiver.length; i++) {
            dests[i] = new InternetAddress(receiver[i].getEmail());
        }
        message.setRecipients(Message.RecipientType.TO, dests);
        message.setSubject(subject);
        message.setContent(content, "text/plain");
        Transport.send(message);
    }

    public static boolean olderThan(Date a, Date b, long time) {
        long timea = a.getTime();
        long timeb = b.getTime();
        return timea < (timeb - time);
    }

    public static boolean olderThan(Date date, long time) {
        long timea = date.getTime();
        long timeb = System.currentTimeMillis();
        return timea < (timeb - time);
    }

    public static void setConfiguration(Configuration configuration) {
        TTraqService.configuration = configuration;
    }

    public static void setInit(boolean init) {
        TTraqService.init = init;
    }

    public static org.hibernate.cfg.Configuration getConfigurationHibernate() {
        return configurationHibernate;
    }

    public static void setConfigurationHibernate(org.hibernate.cfg.Configuration configurationHibernate) {
        TTraqService.configurationHibernate = configurationHibernate;
    }

    public static String returnTTraqSettingAsString(String property) {
        return getPreferences().get(property, getConfigurationHibernate().getProperty(property));
    }

    public static Boolean returnTTraqSettingAsBoolean(String property) {
        return getPreferences().getBoolean(property, getConfiguration().getBoolean(property, false));
    }

    public static boolean isSMTPDisabled() {
        return getPreferences().getBoolean("smtpDisable", false);
    }

    public static String getVersion() {
        return VERSION;
    }

    private static class MailSender implements Runnable {

        private Message message;

        private Exception e;

        MailSender(Message aMessage) {
            message = aMessage;
        }

        public void run() {
            try {
                Transport.send(message);
            } catch (MessagingException e) {
                this.e = e;
            }
        }
    }

    /**
    * Liefert das Session-Objekt um eine Mail zu versenden. Dabei wird die momentane Konfiguration der Anwendung
    * eingesetzt um die Art der Erstellung zu beeinflussen (siehe TTraq.properties).
    *
    * @return gültiges Session-Objekt oder null, falls
    * @throws NamingException Wenn der
    */
    private static Session getMailSession() throws NamingException {
        String service = configuration.getString("mail.service");
        if (service == null) {
            return null;
        }
        if (service.equals("jndi")) {
            Context initCtx = new InitialContext();
            String jndiPath = configuration.getString("mail.jndi.path");
            return (jndiPath != null) ? (Session) initCtx.lookup("java:comp/env" + jndiPath) : null;
        } else if (service.equals("direct")) {
            Properties props = new Properties();
            props.setProperty("mail.smtp.host", configuration.getString("mail.smtp.host"));
            String user = configuration.getString("mail.username");
            String pass = configuration.getString("mail.password");
            Authenticator auth = null;
            if (user == null || pass == null) {
                final PasswordAuthentication authentication = new PasswordAuthentication(user, pass);
                auth = new Authenticator() {

                    protected PasswordAuthentication getPasswordAuthentication() {
                        return authentication;
                    }
                };
            }
            return Session.getInstance(props, auth);
        } else {
            logger.error("invalid service string in TTraq.properties. please correct!");
            return null;
        }
    }

    public static class ParsedCalendars {

        private Calendar startCalendar, endCalendar;

        ParsedCalendars(Calendar aStartCalendar, Calendar aEndCalendar) {
            startCalendar = aStartCalendar;
            endCalendar = aEndCalendar;
        }

        public Calendar getStartCalendar() {
            return startCalendar;
        }

        public Calendar getEndCalendar() {
            return endCalendar;
        }
    }

    /**
    * Diese Methode prüft, ob der Angestellte privilegierten Zugang zum angegeben TimeStamp hat. Dies
    * ist nur dann der Fall, falls er Admin oder Projektleiter des Projektes ist, zudem der TimeStamp
    * gehört.
    * Privilegierter Zugang bedeutet, dass Änderungen und Löschungen ohne Beachtung des Alters des TimeStamps
    * durchgeführt werden dürfen.
    * Die Methode wird von verschiedenen actions benutzt die Änderungen an TimeStamps durchführen.
    *
    * @param logonUser Der Angestellte für den die Überprüfung stattfinden soll.
    * @param timestamp Der TimeStamp des Projektes für den geprüft werden soll.
    * @return Ob der Angestellte berechtigt ist weitergehende Änderungen vorzunehmen.
    */
    public static boolean hasPrivilegedAccess(User logonUser, TimeStamp timestamp) {
        return logonUser.getSuperuser() || logonUser.isManagerInProject(timestamp.getProject());
    }

    /**
    * This checks if a user has access rights to a specified user and project.
    *
    * @param userLogin
    * @param userView
    * @param project
    * @return
    */
    public static boolean hasPrivilegedAccess(User userLogin, User userView, Project project) {
        return userLogin.isSuperuser() || (userLogin.isManagerInProject(project) && userView.isAssignedTo(project));
    }

    public static String getMD5(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());
        byte[] b = md.digest();
        StringBuffer sb = new StringBuffer();
        for (byte aB : b) {
            sb.append((Integer.toHexString((aB & 0xFF) | 0x100)).substring(1, 3));
        }
        return sb.toString();
    }

    public static String readFileContent(InputStream stream) throws IOException {
        BufferedInputStream bf = new BufferedInputStream(stream);
        StringBuffer sb = new StringBuffer("");
        int c;
        while ((c = bf.read()) != -1) {
            sb.append((char) c);
        }
        bf.close();
        return sb.toString();
    }

    public static void setAutoSetup(boolean _autoSetup) {
        autoSetup = _autoSetup;
    }

    public static boolean isAutoSetup() {
        return autoSetup;
    }

    public static boolean isInit() {
        return getPreferences().getBoolean("init", false);
    }

    public static boolean isDemo() {
        return getPreferences().getBoolean("demo", false);
    }

    public static void setPreferences(Preferences _preferences) {
        preferences = _preferences;
    }

    public static Preferences getPreferences() {
        return preferences;
    }

    public static void setInited(boolean b) {
        getPreferences().put("init", Boolean.toString(b));
    }

    /**
    * @param dynaForm
    * @param param
    * @return
    */
    public static boolean testParam(DynaActionForm dynaForm, String param) {
        if (dynaForm.get(param) == null) return false;
        return !dynaForm.get(param).toString().trim().equals("");
    }

    /**
    * @param dynaForm
    * @param param
    * @param compare
    * @return
    */
    public static final boolean testParam(DynaActionForm dynaForm, String param, String compare) {
        if (dynaForm.get(param) == null) return false;
        if (dynaForm.get(param).toString().equals("")) return false;
        return dynaForm.get(param).toString().trim().equalsIgnoreCase(compare);
    }
}
