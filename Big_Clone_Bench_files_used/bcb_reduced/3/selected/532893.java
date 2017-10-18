package net.spamcomplaint.whois;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import net.spamcomplaint.App;
import net.spamcomplaint.util.StringUtils;

public class WhoisMsgStruct {

    public int id;

    private Set complaintEmails = null;

    private String msg, uniqueSha1;

    private MessageDigest md;

    /** Indicates that contact has abuse, spam, etc... in the name */
    private boolean preferredContacts = false;

    /** Non-SPAM complaint email domains */
    private final String[] EMAIL_DOMAIN_FILTERS = { "arin.net", "apnic.net", "lacnic.net", "icann.org", "ripe.net", "iana.net", "iana.org", "afrinic.net", "twnic.net", "hinet.net", "nic.ad.jp", "nic.or.kr", "nida.or.kr" };

    /** Exclude these emails from spam complaints */
    private final String[] EMAIL_FILTERS = { "abuse-net-does-not-handle-ip-addresses@example.net" };

    public WhoisMsgStruct() {
    }

    public WhoisMsgStruct(String msg) {
        this.msg = msg;
    }

    public WhoisMsgStruct(int id, String msg, String uniqueSha1, Date lastUpdate) {
        this.id = id;
        this.msg = msg;
        this.uniqueSha1 = uniqueSha1;
    }

    private String emailsStr(Set emails) {
        StringBuffer sb = new StringBuffer();
        Iterator it = emails.iterator();
        while (it.hasNext()) {
            String email = it.next().toString();
            sb.append(email + '\n');
        }
        return sb.toString();
    }

    public String toString() {
        try {
            Set emails = getComplaintEmails();
            if (emails != null) return emailsStr(emails); else return super.toString();
        } catch (Exception e) {
            App.log.log(Level.SEVERE, e.getMessage(), e);
            return e.toString();
        }
    }

    public String getUniqueSha1() throws NoSuchAlgorithmException {
        if (uniqueSha1 == null) {
            if (md == null) md = MessageDigest.getInstance("SHA1");
            md.reset();
            byte[] uniqueKey;
            uniqueKey = msg.getBytes();
            uniqueSha1 = StringUtils.hexEncode(md.digest(uniqueKey));
        }
        return uniqueSha1;
    }

    public String getMsg() {
        return msg;
    }

    /**
     * Saves message
     * 
     * @param msg
     * @throws IOException
     * @throws InterruptedException
     */
    public void setMsg(String msg) throws NoSuchAlgorithmException, IOException, InterruptedException {
        this.msg = msg;
        uniqueSha1 = null;
        complaintEmails = null;
        preferredContacts = false;
    }

    public boolean hasComplaintEmails() throws IOException, InterruptedException {
        return getComplaintEmails() != null && !complaintEmails.isEmpty();
    }

    /**
     * @return EmailStruct objects
     * @throws IOException
     * @throws InterruptedException
     */
    public Set getComplaintEmails() throws IOException, InterruptedException {
        if (complaintEmails == null) complaintEmails = parseComplaintEmails(getEmailAddys(msg));
        return complaintEmails;
    }

    /**
     * It is common for ISP's to mark emails with record like OrgAbuseEmail and 
  abuse-mailbox.  In this case, it is very important that the other emails are 
  not contacted.
  
  <p>Checks for lines that have the word 'abuse' (any case) on them AND 
  contain an email address.  Even if the email address does not 
  contain the word abuse, that email or emails on that line are used.  If 
  that does not yield anything the other emails are used...
  
     * @return EmailStruct objects 
     * @throws IOException
     * @throws InterruptedException
     */
    private Set parseComplaintEmails(Set emailSet) throws IOException, InterruptedException {
        Set abuseSet = new HashSet();
        Object[] emails = emailSet.toArray();
        for (int i = 0; i < emails.length; i++) {
            EmailStruct emailStruct = (EmailStruct) emails[i];
            if (emailStruct.isAbuseMailbox()) {
                log(emailStruct.line);
                abuseSet = new HashSet();
                abuseSet.add(emailStruct);
                break;
            }
            if (emailStruct.isSubstringMatch("bitbucket")) continue;
            abuseSet.add(emailStruct);
        }
        if (!abuseSet.isEmpty()) return abuseSet;
        return null;
    }

    /**
     * Extracts the email addresses from a multi-line message.  
     * 
     * @param message
     * @param paragraph
     * @return Set of EmailStruct objects
     * @throws IOException
     * @throws InterruptedException
     */
    public Set getEmailAddys(String message) {
        Set emailList = new LinkedHashSet();
        StringTokenizer st = new StringTokenizer(message, "\n", true);
        while (st.hasMoreTokens()) {
            String ln = st.nextToken().trim();
            if (ln.length() == 0) {
                continue;
            }
            ln = ' ' + ln + ' ';
            int atIndex = ln.indexOf("@");
            if (atIndex != -1) {
                StringBuffer email = new StringBuffer();
                int pos = atIndex;
                while (--pos >= 0) {
                    char ch = ln.charAt(pos);
                    if (isValidLocalChar(ch)) email.insert(0, ch); else break;
                }
                if (email.length() == 0) continue;
                email.append("@");
                pos = atIndex;
                int lnLen = ln.length();
                while (++pos < lnLen) {
                    char ch = ln.charAt(pos);
                    if (isValidDomainChar(ch)) email.append(ch); else {
                        String s = validateEmail(email.toString());
                        if (s != null) {
                            EmailStruct es = new EmailStruct(s, ln);
                            if (es.isAbuseMailbox()) emailList.remove(es);
                            emailList.add(es);
                        }
                        email.setLength(0);
                        Set emails = getEmailAddys(ln.substring(pos));
                        Iterator it = emails.iterator();
                        while (it.hasNext()) {
                            EmailStruct es = (EmailStruct) it.next();
                            if (es.isAbuseMailbox()) emailList.remove(es);
                            emailList.add(es);
                        }
                        break;
                    }
                }
            }
        }
        return emailList;
    }

    /**
     * @param ch
     * @return true if character before the @ is valid in e-mail address
     */
    private boolean isValidLocalChar(char ch) {
        return Character.isLetter(ch) || Character.isDigit(ch) || ".!#$%*/?|^{}`~&'+-=_".indexOf(ch) != -1;
    }

    private boolean isValidDomainChar(char ch) {
        return Character.isLetter(ch) || Character.isDigit(ch) || ch == '-' || ch == '.';
    }

    /**
     * A '.' is valid if it is not first or last in name portion 
     * of the e-mail.  (ex: my.local.portion@).  The name part can 
     * be quoted.
     * 
     * @see http://en.wikipedia.org/wiki/E-mail_address
     * 
     * @param email having local and domain characters validated
     * @return null if email appears invalid or is a non-complaint address
     */
    private String validateEmail(String email) {
        if (email.length() < 6) return null;
        if (email.endsWith(".")) email = email.substring(0, email.length() - 1);
        int atIndex = email.indexOf("@");
        if (email.indexOf(".", atIndex) == -1) return null;
        String local = email.substring(0, atIndex);
        if (local.startsWith(".") || local.endsWith(".")) return null;
        if (!(local.startsWith("\"") && local.endsWith("\""))) if (local.indexOf(' ') != -1) return null;
        if (emailFilter(email)) {
            log("Skipping RIR domain " + email);
            return null;
        }
        return email;
    }

    /**
     *  Check if the email is part of Internet Assigned Numbers Authority
     *  or Regional Internet Registries (RIRs) domain.  These are "whois" 
     *  maintainers and can not assist in tracking down abuse.
     *  
     *  @return <b>true</b> if email address can not handle abuse complaints
     */
    private boolean emailFilter(String email) {
        for (int i = 0; i < EMAIL_FILTERS.length; i++) if (email.equalsIgnoreCase(EMAIL_FILTERS[i])) return true;
        int indexOfAt = email.indexOf("@");
        int lenEmail = email.length();
        if (indexOfAt == -1 || indexOfAt == lenEmail - 1) return false;
        String domain = email.substring(indexOfAt).toLowerCase();
        for (int i = 0; i < EMAIL_DOMAIN_FILTERS.length; i++) if (domain.endsWith(EMAIL_DOMAIN_FILTERS[i])) return true;
        return false;
    }

    private static final String log(String s) {
        App.log.info("WhoisMsgStruct: " + s);
        return s;
    }

    public boolean isPreferredContacts() throws IOException, InterruptedException {
        getComplaintEmails();
        return preferredContacts;
    }

    /**
     * Lookup and parse a whois entry
     * @param args IP address
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 1) {
            System.out.println("Specify IP address");
            System.exit(1);
        }
        String ip = args[0];
        System.out.println("whois " + ip + '\n');
        Whois w = new Whois();
        String whoisStr = w.lookupByIp(ip);
        System.out.println(whoisStr);
        System.out.println("--------------------");
        System.out.println("Complaint Emails:");
        WhoisMsgStruct s = new WhoisMsgStruct(whoisStr);
        Set emails = s.getComplaintEmails();
        Iterator it = emails.iterator();
        while (it.hasNext()) {
            EmailStruct es = (EmailStruct) it.next();
            System.out.println(es.line);
        }
    }
}
