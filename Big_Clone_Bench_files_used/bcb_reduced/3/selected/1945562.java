package de.plugmail.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import com.sun.jndi.toolkit.url.Uri;

/**
 * <p>Default Contact Bean</p>
 * 
 * @author Lucifer002
 * @version 1.0
 * @since 23.03.2006
 */
public class Contact {

    private long cid;

    private String sid = "";

    private String displayName;

    private String mailadress;

    private String title;

    private String firstName;

    private String secondName;

    private String lastName;

    private String company;

    /**
	 * Adressinformation
	 */
    private String street;

    private String zipCode;

    private String city;

    private String country;

    /**
	 * Phonenumbers
	 */
    private String privatePhone;

    private String businessPhone;

    private String mobilePhone;

    private String fax;

    /**
	 * waste details
	 */
    private Date birthday;

    private String job;

    private String callName;

    private String notice;

    private Uri webpage;

    /**
	 * <p>With obj it should be possible to 
	 * expand this class with any data from a PlugIn;<br>
	 * <br>
	 * Obj should implemend toString() for Data save?
	 * </p>
	 */
    private Object obj;

    public Contact() {
        this(0l);
        setSid();
    }

    public Contact(long id) {
        cid = id;
        setSid();
    }

    public Contact(String id) {
        this(Long.parseLong(id));
    }

    public Contact(int id) {
        this((long) id);
    }

    public long getCid() {
        return cid;
    }

    public String getSid() {
        return sid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
        setSid();
    }

    public String getMailadress() {
        return mailadress;
    }

    public void setMailadress(String mail) {
        mailadress = mail;
        setSid();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        setSid();
    }

    public String getFirstname() {
        return this.firstName;
    }

    public void setFirstname(String name) {
        this.firstName = name;
        setSid();
    }

    public String getSecondname() {
        return this.secondName;
    }

    public void setSecondname(String name) {
        this.secondName = name;
        setSid();
    }

    public String getLastname() {
        return this.lastName;
    }

    public void setLastname(String name) {
        this.lastName = name;
        setSid();
    }

    public String getCompany() {
        return this.company;
    }

    public void setCompany(String company) {
        this.company = company;
        setSid();
    }

    public String getStreet() {
        return this.street;
    }

    public void setStreet(String street) {
        this.street = street;
        setSid();
    }

    public String getZipCode() {
        return this.zipCode;
    }

    public void setZipCode(String code) {
        this.zipCode = code;
        setSid();
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
        setSid();
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
        setSid();
    }

    public String getPrivatePhone() {
        return this.privatePhone;
    }

    public void setPrivatePhone(String phone) {
        this.privatePhone = phone;
        setSid();
    }

    public String getBusinuessPhone() {
        return this.businessPhone;
    }

    public void setBusinuessPhone(String phone) {
        this.businessPhone = phone;
        setSid();
    }

    public String getMobilePhone() {
        return this.mobilePhone;
    }

    public void setMobilePhone(String phone) {
        this.mobilePhone = phone;
        setSid();
    }

    public String getFax() {
        return this.fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
        setSid();
    }

    public Date getBirthday() {
        return this.birthday;
    }

    public void setBirthday(Date date) {
        this.birthday = date;
        setSid();
    }

    public String getJob() {
        return this.job;
    }

    public void setJob(String job) {
        this.job = job;
        setSid();
    }

    public String getCallname() {
        return this.callName;
    }

    public void setCallname(String name) {
        this.callName = name;
        setSid();
    }

    public String getNotice() {
        return this.notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
        setSid();
    }

    public Uri getWebpage() {
        return this.webpage;
    }

    public void setWebpage(Uri uri) {
        this.webpage = uri;
        setSid();
    }

    public Object setObj(Object obj) {
        Object o = this.obj;
        this.obj = obj;
        setSid();
        return o;
    }

    public Object getObj() {
        return obj;
    }

    /**
	 * Returns false, if data was edited not correct
	 * 
	 * @return True/False
	 */
    public boolean checkSid() {
        if (sid.equals(mkSid().toString()) == false) return false; else return true;
    }

    public String setSid() {
        return (sid = mkSid());
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

    public String toString() {
        String s = "";
        s += Long.toString(cid) + ";";
        if (displayName != null) s += displayName.toString() + ";"; else s += " ;";
        if (title != null) s += title.toString() + ";"; else s += " ;";
        if (firstName != null) s += firstName.toString() + ";"; else s += " ;";
        if (secondName != null) s += secondName.toString() + ";"; else s += " ;";
        if (lastName != null) s += lastName.toString() + ";"; else s += " ;";
        if (company != null) s += company.toString() + ";"; else s += " ;";
        if (street != null) s += street.toString() + ";"; else s += " ;";
        if (zipCode != null) s += zipCode.toString() + ";"; else s += " ;";
        if (city != null) s += city.toString() + ";"; else s += " ;";
        if (country != null) s += country.toString() + ";"; else s += " ;";
        if (privatePhone != null) s += privatePhone.toString() + ";"; else s += " ;";
        if (businessPhone != null) s += businessPhone.toString() + ";"; else s += " ;";
        if (mobilePhone != null) s += mobilePhone.toString() + ";"; else s += " ;";
        if (fax != null) s += fax.toString() + ";"; else s += " ;";
        if (birthday != null) s += birthday.toString() + ";"; else s += " ;";
        if (job != null) s += job.toString() + ";"; else s += " ;";
        if (callName != null) s += callName.toString() + ";"; else s += " ;";
        if (notice != null) s += notice.toString() + ";"; else s += " ;";
        if (webpage != null) s += webpage.toString() + ";"; else s += " ;";
        if (mailadress != null) s += mailadress.toString() + ";"; else s += " ;";
        if (obj != null) s += obj.toString() + ";"; else s += " ;";
        return s;
    }

    public boolean equals(Contact con) {
        if (con.getSid().equals(this.getSid())) return true; else return false;
    }
}
