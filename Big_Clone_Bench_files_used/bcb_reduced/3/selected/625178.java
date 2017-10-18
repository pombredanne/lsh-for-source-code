package de.plugmail.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Message {

    public static int UNREAD = 0;

    public static int READ = 1;

    public static int ANSWERED = 2;

    public static int FORWARDED = 4;

    public static String PLAINTEXT = "text/plain; ";

    public static String HTMLTEXT = "text/html; ";

    public static String MIMETEXT = "multipart/mixed; ";

    public static String MIMEALTERNATIV = "multipart/alternative";

    public static int PRIORITY_HIGHEST = 1;

    public static int PRIORITY_HIGH = 2;

    public static int PRIORITY_NORMAL = 3;

    public static int PRIORITY_LOW = 4;

    public static int PRIORITY_LOWEST = 5;

    private long storeId;

    private String messageId;

    private String contentId;

    private Contact from;

    private Contact[] to;

    private Contact[] cc;

    private Contact[] bcc;

    private Date sendingTime;

    private Date receivingTime;

    private Date expiryDate;

    private String inReplyTo;

    private String contentType;

    private String bountary;

    private String charset;

    private String contentTransferEncoding;

    private String mimeversion;

    private long size = 0l;

    private String useragent;

    private int state = Message.UNREAD;

    private String subject;

    private String content;

    private String contentMD5;

    private String signature;

    private List<Attachment> attachments;

    private byte[] rawData;

    private String allHeader;

    private String priority;

    private String importance;

    private int xpriority;

    private String xmailer;

    private String xMsMailPriority;

    private String xMimeOLE;

    private Contact readingNotificationTo;

    private Contact receiptNotificationTo;

    public Message(String mid) {
        this();
        this.setMessageId(mid);
    }

    public Message() {
        contentType = Message.PLAINTEXT;
        setPriority(Message.PRIORITY_NORMAL);
    }

    public long getStoreId() {
        return storeId;
    }

    public void setStoreId(long id) {
        storeId = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String id) {
        messageId = id;
    }

    public String getContentID() {
        return contentId;
    }

    public void setContentID(String id) {
        contentId = id;
    }

    public Contact getFrom() {
        return from;
    }

    public void setFrom(Contact f) {
        from = f;
    }

    public Contact[] getTo() {
        return to;
    }

    public void setTo(Contact[] t) {
        to = t;
    }

    public Contact[] getCc() {
        return cc;
    }

    public void setCc(Contact[] c) {
        cc = c;
    }

    public Contact[] getBcc() {
        return bcc;
    }

    public void setBcc(Contact[] b) {
        bcc = b;
    }

    public Date getSendingTime() {
        return sendingTime;
    }

    public void setSendingTime(Date time) {
        sendingTime = time;
    }

    public Date getReceivingTime() {
        return receivingTime;
    }

    public void setReceivingTime(Date time) {
        receivingTime = time;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date date) {
        expiryDate = date;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(String to) {
        inReplyTo = to;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String typ) {
        contentType = typ;
    }

    public String getBountary() {
        return bountary;
    }

    public void setBountary(String bound) {
        bountary = bound;
    }

    public String getContentTransferEncoding() {
        return contentTransferEncoding;
    }

    public void setContentTransferEncoding(String encoding) {
        contentTransferEncoding = encoding;
    }

    public String getMimeversion() {
        return mimeversion;
    }

    public void setMimeversion(String version) {
        mimeversion = version;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long siz) {
        size = siz;
    }

    public String getUseragent() {
        return useragent;
    }

    public void setUseragent(String a) {
        useragent = a;
    }

    public int getState() {
        return state;
    }

    public void setState(int s) {
        state = s;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subj) {
        subject = subj;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String c) {
        if (c.endsWith("\r\n")) {
        } else if (c.endsWith("\n")) {
            c = c.substring(0, c.length() - 1) + "\r\n";
        } else c = c + "\r\n";
        content = c;
    }

    public String getContentMD5() {
        setContentMD5();
        return contentMD5;
    }

    public void setContentMD5() {
        MessageDigest messagedigest = null;
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            contentMD5 = null;
        }
        messagedigest.update(content.getBytes());
        byte digest[] = messagedigest.digest();
        String chk = "";
        for (int i = 0; i < digest.length; i++) {
            String s = Integer.toHexString(digest[i] & 0xFF);
            chk += ((s.length() == 1) ? "0" + s : s);
        }
        contentMD5 = chk;
    }

    public void clrContentMD5() {
        contentMD5 = null;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String set) {
        charset = set;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String sig) {
        signature = sig;
    }

    public Attachment[] getAttachments() {
        if (attachments == null) return null;
        if (attachments.isEmpty()) return null;
        return (Attachment[]) attachments.toArray(new Attachment[] {});
    }

    public void setAttachments(Attachment[] att) {
        attachments = (List<Attachment>) Arrays.asList(att);
    }

    public void addAttachment(Attachment att) {
        if (attachments == null) attachments = new ArrayList<Attachment>();
        attachments.add((Attachment) att);
    }

    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] data) {
        rawData = data;
    }

    public String getAllHeader() {
        return allHeader;
    }

    public void setAllHeader(String head) {
        allHeader = head;
    }

    public int getXpriority() {
        return xpriority;
    }

    public String getPriority() {
        return priority;
    }

    public String getImportance() {
        return importance;
    }

    public String getXMsMailPriority() {
        return xMsMailPriority;
    }

    public void setPriority(int prio) throws IllegalArgumentException {
        xpriority = prio;
        switch(prio) {
            case 1:
                this.xMsMailPriority = "Highest";
                this.priority = "urgent";
                this.importance = "high";
                break;
            case 2:
                xMsMailPriority = "High";
                this.priority = "urgent";
                this.importance = "high";
                break;
            case 3:
                xMsMailPriority = "Normal";
                this.priority = "normal";
                this.importance = "normal";
                break;
            case 4:
                xMsMailPriority = "Low";
                this.priority = "non-urgent";
                this.importance = "low";
                break;
            case 5:
                xMsMailPriority = "Lowest";
                this.priority = "non-urgent";
                this.importance = "low";
                break;
            default:
                throw new IllegalArgumentException("Priortiy " + prio + " not allowed");
        }
    }

    public void setPriority(String prio) {
        if (prio.equalsIgnoreCase("urgent")) setPriority(1); else if (prio.equalsIgnoreCase("high")) setPriority(2); else if (prio.equalsIgnoreCase("highest")) setPriority(1); else if (prio.equalsIgnoreCase("normal")) setPriority(3); else if (prio.equalsIgnoreCase("low")) setPriority(4); else if (prio.equalsIgnoreCase("lowest")) setPriority(5); else setPriority(3);
    }

    public String getXmailer() {
        return xmailer;
    }

    public void setXmailer(String m) {
        xmailer = m;
    }

    public String getXMimeOLE() {
        return xMimeOLE;
    }

    public void setXMimeOLE(String ole) {
        xMimeOLE = ole;
    }

    public Contact getReadingNotificatoionTo() {
        return readingNotificationTo;
    }

    public void setReadingNotificationTo(Contact con) {
        readingNotificationTo = con;
    }

    public Contact getReceiptNotificationTo() {
        return receiptNotificationTo;
    }

    public void setReceiptNotificationTo(Contact con) {
        receiptNotificationTo = con;
    }

    /**
	  * Get Headerlines
	  * @return headers Returns all Headerlines as String
	  */
    public String getHeader() {
        String header = "Message-ID: " + messageId + "\r\n";
        if (contentId != null) header += "Content-ID: " + contentId + "\r\n";
        header += "Date: " + sendingTime.toString() + "\r\n";
        if (expiryDate != null) header += "Expiry-Date: " + expiryDate.toString() + "\r\n";
        if (from != null) {
            header += "From: \"" + from.getDisplayName() + "\" <" + from.getMailadress() + "> \r\n";
            header += "Return-Path: \"" + from.getDisplayName() + "\" <" + from.getMailadress() + "> \r\n";
            header += "Sender: \"" + from.getDisplayName() + "\" <" + from.getMailadress() + "> \r\n";
            header += "X-Sender: \"" + from.getDisplayName() + "\" <" + from.getMailadress() + "> \r\n";
            if (from.getCompany() != null) header += "Organization: " + from.getCompany() + "\r\n";
        }
        if (to != null) {
            header += "To: ";
            for (int x = 0; x < to.length; x++) {
                if (x > 0) header += ", ";
                header += to[x].getMailadress();
            }
            header += "\r\n";
        }
        if (cc != null) {
            if (cc.length > 0) {
                header += "CC: ";
                for (int x = 0; x < cc.length; x++) {
                    if (x > 0) header += ", ";
                    header += cc[x].getMailadress() + " ";
                }
                header += "\r\n";
            }
        }
        if (bcc != null) {
            if (bcc.length > 0) {
                header += "BCC: ";
                for (int x = 0; x < bcc.length; x++) {
                    if (x > 0) header += ", ";
                    header += bcc[x].getMailadress() + ", ";
                }
                header += "\r\n";
            }
        }
        if (inReplyTo != null) header += "In-Reply-To: " + inReplyTo.toString() + "\r\n" + "References: " + inReplyTo.toString() + "\r\n";
        header += "Content-Typ: " + contentType + " ";
        if (charset != null) header += charset;
        header += "\r\n";
        header += "Content-Transfer-Encoding: " + contentTransferEncoding + "\r\n";
        header += "MIME-Version: " + mimeversion + "\r\n";
        if (useragent != null) header += "Useragent: " + useragent + "\r\n";
        header += "X-Priority: " + xpriority + " (" + xMsMailPriority + ")\r\n";
        header += "X-MsMailPriority: " + xMsMailPriority + "\r\n";
        header += "X-Mailer: " + xmailer + "\r\n";
        if (xMimeOLE != null) header += "X-MimeOLE: " + xMimeOLE + "\r\n";
        if (readingNotificationTo != null) header += "X-Confirm-Reading-To: " + readingNotificationTo.getMailadress() + "\r\nDisposition-Notification-To:" + readingNotificationTo.getMailadress() + "\r\n";
        if (receiptNotificationTo != null) header += "Return-receipt-to: " + receiptNotificationTo.getMailadress() + "\r\n";
        header += "Subject: " + subject + "\r\n";
        return header;
    }

    /**
	  * Returns Messagetext
	  * @return string completed Message including Signature
	  */
    public String getMessage() {
        String m = "";
        if (content != null) m += content;
        if (signature != null) m += "\r\n-- \r\n" + signature;
        return m;
    }

    public String toString() {
        return getHeader() + "\r\n" + getMessage() + "\r\n\r\n";
    }

    public static String plainToHtml(String text) {
        text = text.replaceAll("&", "&amp;");
        text = text.replaceAll("\n", "<br>\n");
        text = text.replaceAll("<", "&lgt;");
        text = text.replaceAll(">", "&rgt;");
        text = text.replaceAll("   ", "&nbps;&nbsp;&nbsp;");
        text = text.replaceAll("\t", "&nbps;&nbsp;&nbsp;");
        text = text.replaceAll("�", "&auml;");
        text = text.replaceAll("�", "&Auml;");
        text = text.replaceAll("�", "&ouml;");
        text = text.replaceAll("�", "&�uml;");
        text = text.replaceAll("�", "&uuml;");
        text = text.replaceAll("�", "&Uuml;");
        return text;
    }

    public static String htmlToPlain(String text) {
        text = text.replaceAll("<br>\n", "\n");
        text = text.replaceAll("<br>", "\n");
        text = text.replaceAll("&auml;", "�");
        text = text.replaceAll("&Auml;", "�");
        text = text.replaceAll("&uuml;", "�");
        text = text.replaceAll("&Uuml;", "�");
        text = text.replaceAll("&ouml;", "�");
        text = text.replaceAll("&Ouml;", "�");
        text = text.replaceAll("&nbps;&nbsp;&nbsp;", "\t");
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&lgt;", "<");
        text = text.replaceAll("&rgt;", ">");
        text = text.replaceAll("&amp;", "&");
        return text;
    }
}
