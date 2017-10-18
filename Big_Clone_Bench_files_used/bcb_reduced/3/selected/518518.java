package com.tdcs.lords.obj;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 *
 * @author david
 */
public class Prescription implements Serializable {

    private int id = 0;

    private String key = "";

    private String physician = "";

    private String deaNum = "";

    private String patientRecord = "";

    private String lastName = "";

    private String firstName = "";

    private String ssn = "";

    private String address = "";

    private String city = "";

    private String state = "";

    private String zip = "";

    private String phone = "";

    private String qty = "";

    private String medication = "";

    private String instructions = "";

    private String prescription = "";

    private String diagnosis = "";

    private String refills = "0";

    private Date date = new Date();

    private String dateString = "";

    private String algorithm = "SHA1";

    /** Creates a new instance of Prescription */
    public Prescription() {
        date = new Date();
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address.toUpperCase();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city.toUpperCase();
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        setDateString(date.toString());
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public String getDeaNum() {
        return deaNum;
    }

    public void setDeaNum(String deaNum) {
        this.deaNum = deaNum.toUpperCase();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName.toUpperCase();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName.toUpperCase();
    }

    public String getPatientRecord() {
        return this.patientRecord;
    }

    public void setPatientRecord(String patientRecord) {
        this.patientRecord = patientRecord;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone.toUpperCase();
    }

    public String getPhysician() {
        return physician;
    }

    public void setPhysician(String physician) {
        this.physician = physician.toUpperCase();
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getDirections() {
        return instructions;
    }

    public void setDirections(String instructions) {
        this.instructions = instructions;
    }

    public String getRefills() {
        return refills;
    }

    public void setRefills(String refills) {
        this.refills = refills;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getPrescription() {
        StringBuffer sb = new StringBuffer();
        sb.append("QTY:  " + getQty() + "  \n");
        sb.append("Medication:  " + getMedication() + "  \n");
        sb.append("Directions:  " + getDirections() + "  \n");
        sb.append("Refills:  " + getRefills());
        return sb.toString();
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state.toUpperCase();
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public void setData(Patient patient) {
        setDiagnosis(patient.getCurrentDiagnosis());
        setPatientRecord(patient.getRecord());
        setFirstName(patient.getFirstName());
        setLastName(patient.getLastName());
        setSsn(patient.getSsn());
        setAddress(patient.getAddress());
        setCity(patient.getCity());
        setState(patient.getState());
        setZip(patient.getZip());
        if (patient.getPhone().length() != 0) {
            setPhone(patient.getPhone());
        } else if (patient.getCellPhone().length() != 0) {
            setPhone(patient.getCellPhone());
        }
    }

    public String calculateKey() throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
        return calculateKey(this);
    }

    public static String calculateKey(Prescription p) throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
        MessageDigest md = MessageDigest.getInstance(p.getAlgorithm());
        byte[] digest = md.digest(Prescription.convertToBytes(p));
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        String val = encoder.encode(digest);
        return val;
    }

    private static byte[] getBytes(String s) throws UnsupportedEncodingException {
        return s.getBytes("UTF-8");
    }

    public byte[] convertToBytes() throws UnsupportedEncodingException, IOException {
        return convertToBytes(this);
    }

    private static byte[] convertToBytes(Prescription p) throws UnsupportedEncodingException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(getBytes(p.getSsn()));
        baos.write(getBytes(p.getFirstName()));
        baos.write(getBytes(p.getLastName()));
        baos.write(getBytes(p.getAddress()));
        baos.write(getBytes(p.getCity()));
        baos.write(getBytes(p.getState()));
        baos.write(getBytes(p.getZip()));
        baos.write(getBytes(p.getDeaNum()));
        baos.write(getBytes(p.getPrescription()));
        baos.write(getBytes(p.getDateString()));
        return baos.toByteArray();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<html><body>");
        sb.append("<p></p>");
        sb.append("<p><b>" + getMedication() + "</b></p>");
        sb.append("<ul><li>" + getLastName() + ", " + getFirstName() + "</li>");
        sb.append("<li>" + getDateString() + "</li>");
        sb.append("</ul>");
        sb.append("</html></body>");
        return sb.toString();
    }

    public static void main(String[] args) {
        try {
            Prescription p = new Prescription();
            p.setSsn("123-45-6789");
            p.setFirstName("David");
            p.setLastName("Days");
            p.setAddress("13973 Lyck Run Lyra");
            p.setCity("South Webster");
            p.setState("OH");
            p.setZip("45682");
            p.setDeaNum("BN1234567890");
            p.setPhysician("Dr. Who, MD");
            p.setQty("30");
            p.setMedication("Motrin 400mg");
            p.setDirections("Twice Daily");
            p.setDate(new Date());
            System.out.println("Date:  " + p.getDateString());
            System.out.println("SHA1:\t" + p.calculateKey());
            p.setAlgorithm("MD5");
            System.out.println("MD5:\t" + p.calculateKey());
            p.setAddress("13973 Lick Run Lyra");
            p.setAlgorithm("SHA1");
            System.out.println("Changed Lyck to Lick");
            System.out.println("SHA1:\t" + p.calculateKey());
            p.setAlgorithm("MD5");
            System.out.println("MD5:\t" + p.calculateKey());
            System.out.println("Changing back to Lyck");
            p.setAddress("13973 Lyck Run Lyra");
            p.setAlgorithm("SHA1");
            System.out.println("SHA1:\t" + p.calculateKey());
            p.setAlgorithm("MD5");
            System.out.println("MD5:\t" + p.calculateKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
