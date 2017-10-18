package com.hisham.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.struts.action.*;

/**
 *
 * <p>Title: Web Services for Parking</p>
 *
 * <p>Description: </p>
 *
 * @author Ali Hisham Malik
 * @version 2.0
 */
public class UtilityFunctions {

    /**
   * Clears the time values from the Date object
   * @param d Date
   * @return Date
   */
    public static final java.util.Date clearTime(java.util.Date d) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(d);
        calendar.clear(java.util.Calendar.MILLISECOND);
        calendar.clear(java.util.Calendar.SECOND);
        calendar.clear(java.util.Calendar.MINUTE);
        calendar.clear(java.util.Calendar.HOUR_OF_DAY);
        calendar.clear(java.util.Calendar.HOUR);
        calendar.clear(java.util.Calendar.AM_PM);
        return calendar.getTime();
    }

    /**
   * 
   * @param date1
   * @param date2
   * @return
   */
    public static final int compareTimeofDay(java.util.Date date1, java.util.Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        cal1.set(Calendar.DAY_OF_YEAR, 1);
        cal1.set(Calendar.YEAR, 0);
        cal2.set(Calendar.DAY_OF_YEAR, 1);
        cal2.set(Calendar.YEAR, 0);
        return cal1.compareTo(cal2);
    }

    /**
   *
   * @param smtpHost String
   * @param fromAddress String
   * @param toAddress String
   * @param subject String
   * @param body String
   * @return boolean
   */
    public static final boolean sendSMTPEmail(String smtpHost, int smtpPort, String fromAddress, String toAddress, String subject, String body) {
        try {
            java.net.Socket smtpSocket;
            java.io.BufferedReader reader;
            java.io.BufferedWriter writer;
            StringBuffer toLog;
            String toWrite = new String();
            String sep = "\r\n";
            toLog = new StringBuffer();
            smtpSocket = new java.net.Socket(smtpHost, smtpPort);
            reader = new java.io.BufferedReader(new java.io.InputStreamReader(smtpSocket.getInputStream()));
            writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(smtpSocket.getOutputStream()));
            String whoami = java.net.InetAddress.getLocalHost().getHostName();
            toWrite = "HELO " + whoami + sep;
            toLog.append("\t" + toWrite);
            writer.write(toWrite);
            writer.flush();
            toLog.append("Email being sent.\r\n");
            toLog.append("\t" + reader.readLine() + "\r\n");
            toLog.append("\t" + reader.readLine() + "\r\n");
            toWrite = "MAIL FROM: <" + fromAddress + ">" + sep;
            toLog.append("\t" + toWrite);
            writer.write(toWrite);
            writer.flush();
            toLog.append("\t" + reader.readLine() + "\r\n");
            toWrite = "RCPT TO: <" + toAddress + ">" + sep;
            toLog.append("\t" + toWrite);
            writer.write(toWrite);
            writer.flush();
            toLog.append("\t" + reader.readLine() + "\r\n");
            toWrite = "DATA" + sep;
            toLog.append("\t" + toWrite);
            writer.write(toWrite);
            writer.flush();
            toLog.append("\t" + reader.readLine() + "\r\n");
            toWrite = "TO: " + toAddress + sep;
            toLog.append("\t" + toWrite);
            writer.write(toWrite);
            writer.flush();
            toWrite = "FROM: " + fromAddress + sep;
            toLog.append("\t" + toWrite);
            writer.write(toWrite);
            writer.flush();
            toWrite = "SUBJECT: " + subject + sep;
            toLog.append("\t" + toWrite);
            writer.write(toWrite);
            writer.flush();
            toWrite = body + sep;
            writer.write(toWrite);
            toLog.append("\tBody Size: " + toWrite.length() + sep);
            writer.flush();
            toWrite = "\r\n.\r\n";
            toLog.append("\t" + "." + "\r\n");
            writer.write(toWrite);
            writer.flush();
            toLog.append("\t" + reader.readLine() + "\r\n");
            toWrite = "QUIT" + sep;
            toLog.append("\t" + toWrite);
            writer.write(toWrite);
            writer.flush();
            toLog.append("\t" + reader.readLine() + "\r\n");
            smtpSocket.close();
            System.out.println(toLog.toString());
        } catch (java.io.IOException ioe) {
            System.out.println("An error has occured: " + ioe.toString() + ".  Please try again later.");
            return false;
        }
        return true;
    }

    /**
   *
   * @param smtpHost String
   * @param fromName String
   * @param fromEmail String
   * @param recipientEmails String[]
   * @param recipientNames String[]
   * @param subject String
   * @param message String
   * @return boolean
   */
    public static boolean sendPlainEMail(String smtpHost, String fromName, String fromEmail, String recipientEmails[], String recipientNames[], String subject, String message) {
        try {
            boolean debug = false;
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(debug);
            Message msg = new MimeMessage(session);
            InternetAddress addressFrom = new InternetAddress(fromEmail, fromName);
            msg.setFrom(addressFrom);
            InternetAddress[] addressTo = new InternetAddress[recipientEmails.length];
            for (int i = 0; i < recipientEmails.length; i++) {
                addressTo[i] = new InternetAddress(recipientEmails[i], recipientNames[i]);
            }
            msg.setRecipients(Message.RecipientType.TO, addressTo);
            msg.addHeader("MyHeaderName", "myHeaderValue");
            msg.setSubject(subject);
            msg.setContent(message, "text/plain");
            Transport.send(msg);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean sendPlainEMail(String smtpHost, String fromEmail, String fromName, String recipientEmail, String recipientName, String subject, String message) {
        try {
            boolean debug = false;
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(debug);
            Message msg = new MimeMessage(session);
            InternetAddress addressFrom = new InternetAddress(fromEmail, fromName);
            msg.setFrom(addressFrom);
            InternetAddress addressTo = new InternetAddress(recipientEmail, recipientName);
            msg.setRecipient(Message.RecipientType.TO, addressTo);
            msg.addHeader("MyHeaderName", "myHeaderValue");
            msg.setSubject(subject);
            msg.setContent(message, "text/plain");
            Transport.send(msg);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
   * looks for matching method by recursively searching the class and
   * its superclass
   * @param klass Class
   * @param methodName String
   * @param parameterTypes Class[]
   * @throws NoSuchMethodException
   * @return Method
   */
    public static final Method findMethod(Class klass, String methodName, Class[] parameterTypes) throws NoSuchMethodException {
        try {
            return klass.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class superClass = klass.getSuperclass();
            if (superClass != null) {
                return findMethod(superClass, methodName, parameterTypes);
            }
            throw e;
        }
    }

    public static final Method findMethodDFS(Class klass, String methodName, Class[] parameterTypes) throws NoSuchMethodException {
        try {
            return findMethod(klass, methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class[] tmpParameterTypes = (Class[]) parameterTypes.clone();
            Class parameterSuperClass;
            for (int i = 0; i < tmpParameterTypes.length; i++) {
                parameterSuperClass = parameterTypes[i].getSuperclass();
                if (parameterSuperClass != null) {
                    tmpParameterTypes[i] = parameterSuperClass;
                    try {
                        return findMethodDFS(klass, methodName, tmpParameterTypes);
                    } catch (Exception e3) {
                        tmpParameterTypes[i] = parameterTypes[i];
                    }
                }
            }
            throw e;
        }
    }

    public static final String removeSpaces(String value) {
        String[] result = value.split("\\s");
        StringBuffer newValue = new StringBuffer();
        for (int i = 0; i < result.length; i++) newValue.append(result[i]);
        return newValue.toString();
    }

    public static ActionMessages getActionMessages(Map messages) {
        Iterator i = messages.keySet().iterator();
        ActionMessages aMessages = new ActionMessages();
        String errorKey;
        while (i.hasNext()) {
            errorKey = (String) i.next();
            aMessages.add(errorKey, new ActionMessage((String) messages.get(errorKey)));
        }
        return aMessages;
    }

    /**
   * Generates a simple random temporary password
   * @param passwordLength int
   * @return String
   */
    public static String generateRandomPassword(int passwordLength) {
        StringBuffer randomPass = new StringBuffer();
        String keylist = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < passwordLength; i++) randomPass.append(keylist.charAt((int) Math.floor(Math.random() * keylist.length())));
        return randomPass.toString();
    }

    /**
   * Gets Tomcat-Compatible Hashed Password
   * @param password String
   * @throws NoSuchAlgorithmException
   * @return String Tomcat-Compatible Hashed Password
   */
    public static String getHashedPasswordTc(String password) throws java.security.NoSuchAlgorithmException {
        java.security.MessageDigest d = java.security.MessageDigest.getInstance("MD5");
        d.reset();
        d.update(password.getBytes());
        byte[] buf = d.digest();
        char[] cbf = new char[buf.length * 2];
        for (int jj = 0, kk = 0; jj < buf.length; jj++) {
            cbf[kk++] = "0123456789abcdef".charAt((buf[jj] >> 4) & 0x0F);
            cbf[kk++] = "0123456789abcdef".charAt(buf[jj] & 0x0F);
        }
        return new String(cbf);
    }

    public static String getHashedStringMD5(String value) throws java.security.NoSuchAlgorithmException {
        java.security.MessageDigest d = java.security.MessageDigest.getInstance("MD5");
        d.reset();
        d.update(value.getBytes());
        byte[] buf = d.digest();
        return new String(buf);
    }
}
