package de.plugmail.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * <p>Default Appointment Bean</p>
 * 
 * @author Lucifer002
 * @version 1.0
 * @since 23.03.2006
 */
public class Appointment {

    static final int LABEL_NONE = 0;

    static final int LABEL_IMPORTANT = 1;

    static final int LABEL_UNIMPORTANT = 2;

    static final int LABEL_BUSINESS = 3;

    static final int LABEL_PRIVATE = 4;

    static final int LABEL_VOCATION = 5;

    static final int LABEL_INCLUDE_ARRIVAL = 6;

    static final int LABEL_PHONECALL = 7;

    static final int LABEL_SHEDULING_NEEDED = 8;

    static final int DISPLAY_FREE = 0;

    static final int DISPLAY_PROVISO = 1;

    static final int DISPLAY_BOOKED = 2;

    static final int DISPLAY_AWAY = 3;

    private long aid;

    private String sid;

    private String subject;

    private String location;

    private int label;

    private int display;

    private Date start;

    private Date end;

    private boolean reminder;

    private Date reminderTime;

    private String notice;

    private Contact[] contacts;

    private Attachment[] attachments;

    /**
	 * <p>With obj it should be possible to 
	 * expand this class with any data from a PlugIn;<br>
	 * <br>
	 * Obj should implemend toString() for Data save?
	 * </p>
	 */
    private Object obj;

    public Appointment(long a) {
        aid = a;
    }

    public long getAid() {
        return aid;
    }

    public String getSid() {
        return sid;
    }

    public void setSid() {
        sid = mkSid();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String sub) {
        subject = sub;
        setSid();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String l) {
        location = l;
        setSid();
    }

    public int getLabel() {
        return label;
    }

    public void setLabel(int lab) {
        label = lab;
        setSid();
    }

    public int getDisplay() {
        return display;
    }

    public void setDisplay(int d) {
        display = d;
        setSid();
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date s) {
        start = s;
        setSid();
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date e) {
        end = e;
        setSid();
    }

    public boolean getReminder() {
        return reminder;
    }

    public void setReminder(boolean rem) {
        reminder = rem;
        setSid();
    }

    public Date getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(Date rem) {
        reminderTime = rem;
        setSid();
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String note) {
        notice = note;
        setSid();
    }

    public Contact[] getContacts() {
        return contacts;
    }

    public void setContacts(Contact[] con) {
        contacts = con;
        setSid();
    }

    public Attachment[] getAttachments() {
        return attachments;
    }

    public void setAttachment(Attachment[] att) {
        attachments = att;
        setSid();
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object o) {
        obj = o;
        setSid();
    }

    public String toString() {
        String s = "";
        s += Long.toString(aid) + ";";
        if (subject != null) s += subject + ";"; else s += " ;";
        if (location != null) s += location + ";"; else s += " ;";
        s += label + ";";
        s += display + ";";
        if (start != null) s += start.toString() + ";"; else s += " ;";
        if (end != null) s += end.toString() + ";"; else s += " ;";
        s += Boolean.toString(reminder) + ";";
        if (reminderTime != null) s += reminderTime.toString(); else s += " ;";
        if (notice != null) s += notice + ";"; else s += " ;";
        if (contacts != null) {
            s += "{";
            for (int x = 0; x < contacts.length; x++) {
                if (x > 0) s += "}{";
                contacts[x].toString();
            }
            s += "};";
        } else s += " ;";
        if (attachments != null) {
            s += "{";
            for (int x = 0; x < attachments.length; x++) {
                if (x > 0) s += "}{";
                attachments[x].toString();
            }
            s += "};";
        } else s += " ;";
        if (obj != null) s += obj.toString() + ";"; else s += " ;";
        return s;
    }

    /**
	 * Generate a new SID
	 * 
	 * @return SHA-Checksumm
	 */
    private String mkSid() {
        String temp = toString();
        MessageDigest messagedigest = null;
        try {
            messagedigest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        messagedigest.update(temp.getBytes());
        byte digest[] = messagedigest.digest();
        String chk = "";
        for (int i = 0; i < digest.length; i++) {
            String s = Integer.toHexString(digest[i] & 0xFF);
            chk += ((s.length() == 1) ? "0" + s : s);
        }
        return chk.toString();
    }

    public boolean equals(Appointment app) {
        if (sid.equals(app.getSid()) == true) return true; else return false;
    }
}
