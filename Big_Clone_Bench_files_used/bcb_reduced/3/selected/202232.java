package de.plugmail.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Default Task Bean
 * 
 * @author Lucifer002
 * @version 1.0
 * @since 23.03.2006
 */
public class Task {

    private long tid;

    private String sid;

    private String subject;

    private int state;

    private int priority;

    private int done;

    private Date end;

    private Date start;

    private boolean reminder;

    private Date reminderTime;

    private String notice;

    private Date isEnd;

    private float totalTime;

    private float isTime;

    public Task(long id) {
        tid = id;
        setSid();
    }

    public long getTid() {
        return tid;
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

    public int getState() {
        return state;
    }

    public void setState(int sta) {
        state = sta;
        setSid();
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int prio) {
        priority = prio;
        setSid();
    }

    public int getDone() {
        return done;
    }

    public void setDone(int don) {
        done = don;
        setSid();
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date e) {
        end = e;
        setSid();
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date star) {
        start = star;
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

    public void setReminderTime(Date time) {
        reminderTime = time;
        setSid();
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String note) {
        notice = note;
        setSid();
    }

    public Date getIsEnd() {
        return isEnd;
    }

    public void setIsEnd(Date end) {
        isEnd = end;
        setSid();
    }

    public float getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(float time) {
        totalTime = time;
        setSid();
    }

    public float getIsTime() {
        return isTime;
    }

    public void setIsTime(float time) {
        isTime = time;
        setSid();
    }

    public String toString() {
        String s = "" + tid + ";";
        if (subject != null) s += subject.toString() + ";"; else s += " ;";
        s += state + ";";
        s += priority + ";";
        s += done + ";";
        if (end != null) s += end.toString() + ";"; else s += " ;";
        if (start != null) s += start.toString() + ";"; else s += " ;";
        s += "reminder";
        if (reminderTime != null) s += reminderTime.toString() + ";"; else s += " ;";
        if (notice != null) s += notice.toString() + ";"; else s += " ;";
        if (isEnd != null) s += isEnd.toString() + ";"; else s += " ;";
        s += totalTime + ";";
        s += isTime + ";";
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
}
