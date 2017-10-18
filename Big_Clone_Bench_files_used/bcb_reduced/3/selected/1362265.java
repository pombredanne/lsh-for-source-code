package net.spamcomplaint.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import net.spamcomplaint.App;
import net.spamcomplaint.util.NetworkUtil;
import net.spamcomplaint.util.StringUtils;

/**
 * 
 * @author jcalfee
 * @see http://www.w3.org/Protocols/rfc1341/7_2_Multipart.html
 */
public class MimeMessageParser {

    public static SimpleDateFormat dateFormat_1 = new SimpleDateFormat("EEE, d MMM yy HH:mm:ss Z");

    public static SimpleDateFormat dateFormat_2 = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

    public static SimpleDateFormat dateFormat_3 = new SimpleDateFormat("d MMM yyyy HH:mm:ss Z");

    static int minReceivedHdrWidth = "Received: 0.0.0.0".length();

    private MessageDigest md = null;

    static final String VALID_DOMAIN_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789-.";

    private static final void log(String s) {
        App.log.info("MimeMessageParser: " + s);
    }

    /**
     * Get The Hash value as a Hex-String. Calling this method resets the object's state to initial state.
     * @return String representing the Hashvalue in hexadecimal format.
     */
    public String digout() {
        byte[] digest = md.digest();
        if (digest != null) return StringUtils.hexEncode(digest); else return null;
    }

    static final String[] daysInDate = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };

    public static MimeMessage getMimeMessage(String msgText) throws MessagingException {
        return new MimeMessage(null, new ByteArrayInputStream(msgText.getBytes()));
    }

    /**
     * @param mimeMsg
     * @return hex SHA1 of key fields in the header { received, from, to }
     * @throws MessagingException
     * @throws NoSuchAlgorithmException if SHA1 is not available
     */
    public String getUnique_SHA1(Enumeration mimeHeaders) throws NoSuchAlgorithmException {
        if (md == null) md = MessageDigest.getInstance("SHA1");
        md.reset();
        while (mimeHeaders.hasMoreElements()) {
            Header h = (Header) mimeHeaders.nextElement();
            String name = h.getName().trim().toLowerCase();
            String value = h.getValue();
            if (name.equals("received") || name.equals("from") || name.equals("to")) md.update(value.getBytes());
        }
        return digout();
    }

    public static String byHost(String receivedLine) {
        int byIndex = receivedLine.indexOf("by ");
        if (byIndex == -1) return null;
        byIndex += "by ".length();
        int recLen = receivedLine.length();
        int i;
        for (i = byIndex; i < recLen; i++) {
            char ch = Character.toLowerCase(receivedLine.charAt(i));
            if (VALID_DOMAIN_CHARS.indexOf(ch) == -1) break;
        }
        if (i == byIndex) return null;
        return receivedLine.substring(byIndex, i);
    }

    private static Set parseReceivedIpLn(String ln) {
        Set ips = new LinkedHashSet();
        String ip = "";
        ln += " ";
        for (int i = 0; i < ln.length(); i++) {
            char ch = ln.charAt(i);
            if (Character.isDigit(ch) || ch == '.') ip += ch; else {
                if (NetworkUtil.isIp(ip) && !NetworkUtil.isRemoteIp(ip)) log("!!! SKIPPING LOCAL IP : " + ip);
                if (NetworkUtil.isIp(ip) && !ips.contains(ip) && NetworkUtil.isRemoteIp(ip)) ips.add(ip);
                ip = "";
            }
        }
        return ips;
    }

    /** 
     * If the sender used an IP address in their non-validated 
     * HELO command, skip it since it could be fake. 
     * @param ln
     * @return
     */
    private static Set parseReceivedIpLn_skip_HELO(String ln) {
        int heloIndex = ln.toLowerCase().indexOf("helo");
        Set ipList;
        if (heloIndex == -1) return parseReceivedIpLn(ln); else {
            ipList = parseReceivedIpLn(ln);
            Iterator it = ipList.iterator();
            while (it.hasNext()) {
                String ip = (String) it.next();
                int ipIndex = ln.substring(heloIndex + 5).indexOf(ip);
                if (ipIndex > -1 && ipIndex < 3) {
                    ipIndex += heloIndex + 5;
                    ln = ln.substring(0, ipIndex) + ln.substring(ipIndex + ip.length(), ln.length());
                    log("Removed HELO IP " + ln);
                    if (ln.indexOf(ip) == -1) ipList.remove(ip);
                    break;
                }
            }
        }
        return ipList;
    }

    static String getSendersIP(String receivedLine) {
        Set ips = parseReceivedIpLn_skip_HELO(receivedLine);
        Iterator it = ips.iterator();
        while (it.hasNext()) {
            String ip = (String) it.next();
            if (receivedLine.indexOf("[" + ip + "]") != -1) return ip;
        }
        it = ips.iterator();
        while (it.hasNext()) {
            String ip = (String) it.next();
            if (receivedLine.indexOf("(" + ip + ")") != -1) return ip;
        }
        if (ips.size() == 1) return (String) ips.toArray()[0];
        return null;
    }

    public static String getSpamComplaintIP(String[] received, int lastTrustedIp) {
        String spamIp = null;
        for (int i = 0; i < received.length; i++) {
            String host = byHost(received[i]);
            if (host == null) continue;
            if (NetworkUtil.isIp(host)) {
                if (!NetworkUtil.isRemoteIp(host)) continue;
            }
            String ip = getSendersIP(received[i]);
            if (ip != null) {
                spamIp = ip;
                if (i >= lastTrustedIp) break;
            }
        }
        if (spamIp == null) {
            log("Warning, could not find sender's IP in the headers:");
            for (int i = 0; i < received.length; i++) log(" " + received[i]);
        }
        return spamIp;
    }

    /**
     * @param msgText
     * @param ra
     * @return Sender IP address, this could be <b>null</b> (undetermined)
     * @throws MessagingException
     * @throws TrustedNodeException
     */
    public static String getSpamComplaintIP(String msgText, ReceivedAnalysis ra) throws MessagingException, TrustedNodeException {
        MimeMessage msg = getMimeMessage(msgText);
        String[] received = msg.getHeader("received");
        if (received == null) {
            log("Received header missing");
            return null;
        }
        return getSpamComplaintIP(received, ra.lastCommonByHost(received));
    }

    public static String contentToString(Object content) throws IOException {
        if (content instanceof String) return (String) content;
        if (content instanceof InputStream) {
            InputStream in = (InputStream) content;
            byte[] b = new byte[in.available()];
            in.read(b);
            return new String(b);
        }
        log("unknown content object: " + content.getClass().getName());
        return content.toString();
    }

    /**
     * Attempts to parse a date using first the standard MIME date format, and fails over
     * to alternative formats.
     * 
     * @param dateStr
     * @return Date represented by dateStr
     * @throws ParseException
     */
    public static Date parseDate(String dateStr) throws ParseException {
        try {
            return dateFormat_1.parse(dateStr);
        } catch (ParseException e) {
            try {
                return dateFormat_2.parse(dateStr);
            } catch (ParseException ex) {
                return dateFormat_3.parse(dateStr);
            }
        }
    }

    /**
     * @param text with a date string at the end of the line
     * @return Fri, 23 Mar 2007 10:45:44 -0500 or <b>null</b>
     */
    public static String getDateString(String text) {
        int startIndex = -1;
        for (int i = 0; i < daysInDate.length; i++) {
            startIndex = text.lastIndexOf(daysInDate[i]);
            if (startIndex != -1) break;
        }
        if (startIndex == -1) {
            return null;
        }
        return text.substring(startIndex);
    }

    /**
     * @param MimeMessage, date will be extracted from the headers
     * @return "Date" property from header or from received line from 
     *  furthest mail servers to the closest mail server.  <b>Null</b> is returned
     *  if a date could not be found or parsed anywhere.
     *      
     * @throws MessagingException
     */
    public static Date getDate(MimeMessage msg) {
        try {
            return _getDate(msg);
        } catch (Exception e) {
            log("Error parsing date");
            try {
                log(getAllHeaders(msg.getAllHeaders()).toString());
            } catch (MessagingException ex) {
            }
        }
        return new Date();
    }

    public static Date _getDate(MimeMessage msg) throws MessagingException {
        Date today = new Date();
        String[] received = msg.getHeader("received");
        if (received == null) log("Missing 'Received' header properties!"); else for (int i = 0; i < received.length; i++) {
            log(received[i]);
            String dateStr = null;
            try {
                dateStr = getDateString(received[i]);
                if (dateStr != null) {
                    Date msgDate = parseDate(dateStr);
                    if (msgDate.after(today)) log("'Received' date is after today, skipping: " + dateStr); else return msgDate;
                }
            } catch (ParseException ex) {
                log("Unparsable 'Received' header property value:\n" + received[i]);
            }
        }
        String[] dateHeader = msg.getHeader("date");
        if (dateHeader != null) {
            String dateStr = dateHeader[0];
            try {
                Date msgDate = parseDate(dateStr);
                if (msgDate.after(today)) log("'Date' header is after today, skipping: " + dateStr); else return msgDate;
            } catch (ParseException ex) {
                log("Unparsable 'Date' header property value: " + dateHeader[0]);
            }
        }
        log("Using current time.  \n" + "Could not parse a date/time from following 'recieved' or from 'date' properties...  \n" + getAllHeaders(msg.getAllHeaders()));
        return new Date();
    }

    public static StringBuffer getAllHeaders(Enumeration msgEnum) {
        StringBuffer headers = new StringBuffer();
        while (msgEnum.hasMoreElements()) {
            Header h = (Header) msgEnum.nextElement();
            String name = h.getName();
            String value = h.getValue();
            headers.append(name + " : " + value + '\n');
        }
        return headers;
    }

    /**
     * @param one like these: 
     *   <br/>a453.domain.example.com, 61.236.8.142, 127.0.0.1
     * 
     * @return one of the following for a given parameter: 
     *   <br/>example.com, 61.236.8.142, internal_ip
     */
    public static String primaryDomain(String domain) {
        if (domain == null) return null;
        if (NetworkUtil.isIp(domain)) return NetworkUtil.isRemoteIp(domain) ? domain : "internal_ip";
        String[] domains = domain.split("\\.");
        int domainsLen = domains.length;
        if (domainsLen == 1) return domains[0];
        return domains[domainsLen - 2] + '.' + domains[domainsLen - 1];
    }
}
