import java.util.*;
import java.io.*;
import java.util.*;
import java.security.*;
import cryptix.util.core.Hex;
import cryptix.provider.md.*;

public class Coupon {

    private long _serialNo;

    private long _ID;

    private int _denomination;

    private String _status;

    private String _expirationDate;

    private boolean _transferable;

    private String _owner;

    private String _redemptionDate;

    private String _usedFor;

    private String _usedBy;

    private String _bankSignatureString;

    private String _vendorSignatureString;

    private Signature _bankSig = null;

    private Signature _vendorSig = null;

    private byte[] _bankMessageDigestBytes;

    private byte[] _bankSignatureBytes;

    private byte[] _vendorMessageDigestBytes;

    private byte[] _vendorSignatureBytes;

    private MessageDigest _bankMessageDigest;

    private MessageDigest _vendorMessageDigest;

    private PublicKey _bankPubKey;

    private PrivateKey _bankPrivateKey;

    private PublicKey _vendorPubKey;

    private PrivateKey _vendorPrivateKey;

    public Coupon(long serial, long ID, int denom, String status, String expDate, boolean transferable, String owner, String redemptionDate, String usedFor, String usedBy, String bankSig) {
        _serialNo = serial;
        _ID = ID;
        _denomination = denom;
        _status = new String(status);
        _expirationDate = new String(expDate);
        _transferable = transferable;
        _owner = new String(owner);
        _redemptionDate = new String(redemptionDate);
        _usedFor = new String(usedFor);
        _usedBy = new String(usedBy);
        _bankSignatureString = new String(bankSig);
    }

    public Coupon() {
        _serialNo = 0;
        _ID = 0;
        _denomination = 0;
        _status = null;
    }

    public Coupon(long serial, long ID, int denom, String expDate, boolean transferable, String owner) {
        _serialNo = serial;
        _ID = ID;
        _denomination = denom;
        _status = new String("OUT_OF_CIRCULATION");
        _expirationDate = new String(expDate);
        _transferable = transferable;
        _owner = new String(owner);
        _redemptionDate = new String("NA");
        _usedFor = new String("NA");
        _usedBy = new String("NA");
    }

    public byte[] getBankMessageDigestBytes() {
        return _bankMessageDigestBytes;
    }

    public byte[] getBankSignatureBytes() {
        return _bankSignatureBytes;
    }

    public String getBankSignatureString() {
        return _bankSignatureString;
    }

    public int getDenomination() {
        return _denomination;
    }

    public String getExpirationDate() {
        return _expirationDate;
    }

    public long getID() {
        return _ID;
    }

    public String getOwner() {
        return _owner;
    }

    public String getRedemptionDate() {
        return _redemptionDate;
    }

    public long getSerialNumber() {
        return _serialNo;
    }

    public String getStatus() {
        return _status;
    }

    public boolean getTransferable() {
        return _transferable;
    }

    public String getUsedBy() {
        return _usedBy;
    }

    public String getUsedFor() {
        return _usedFor;
    }

    public MessageDigest getVendorDigest() {
        return _vendorMessageDigest;
    }

    public String getVendorSignatureString() {
        return _vendorSignatureString;
    }

    public void setOwner(String ownerName) {
        _owner = ownerName;
    }

    public void setBankSecretKey(PrivateKey bsk) {
        _bankPrivateKey = bsk;
    }

    public void setVendorSecretKey(PrivateKey vsk) {
        _vendorPrivateKey = vsk;
    }

    public void createBankSignature() {
        byte b;
        try {
            _bankMessageDigest = MessageDigest.getInstance("MD5");
            _bankSig = Signature.getInstance("MD5/RSA/PKCS#1");
            _bankSig.initSign((PrivateKey) _bankPrivateKey);
            _bankMessageDigest.update(getBankString().getBytes());
            _bankMessageDigestBytes = _bankMessageDigest.digest();
            _bankSig.update(_bankMessageDigestBytes);
            _bankSignatureBytes = _bankSig.sign();
        } catch (Exception e) {
        }
        ;
    }

    public void createVendorSignature() {
        byte b;
        try {
            _vendorMessageDigest = MessageDigest.getInstance("MD5");
            _vendorSig = Signature.getInstance("MD5/RSA/PKCS#1");
            _vendorSig.initSign((PrivateKey) _vendorPrivateKey);
            _vendorMessageDigest.update(getBankString().getBytes());
            _vendorMessageDigestBytes = _vendorMessageDigest.digest();
            _vendorSig.update(_vendorMessageDigestBytes);
            _vendorSignatureBytes = _vendorSig.sign();
        } catch (Exception e) {
        }
        ;
    }

    /**
       Determines if this coupon is in circulation or not
    */
    public boolean isInCirculation() {
        if (getStatus().equals("OUT_OF_CIRCULATION")) return false; else return true;
    }

    public void save(PrintWriter out) {
        out.println("<COUPON>");
        out.println(getSerialNumber() + " ");
        out.println(getID() + " ");
        out.println(getDenomination() + " ");
        out.println('"' + getStatus() + '"' + " ");
        out.println('"' + getExpirationDate() + '"' + " ");
        out.println(getTransferable() + " ");
        out.println('"' + getOwner() + '"' + " ");
        out.println('"' + getRedemptionDate() + '"' + " ");
        out.println('"' + getUsedBy() + '"' + " ");
        out.println('"' + getUsedFor() + '"' + " ");
        out.println('"' + cryptix.util.core.Hex.toString(getBankSignatureBytes()) + '"' + " ");
        out.println("</COUPON>");
        out.println(" ");
    }

    public String getBankString() {
        return getID() + "\n" + getSerialNumber() + "\n" + getDenomination() + "\n" + getExpirationDate() + "\n" + getRedemptionDate() + "\n" + getUsedBy() + "\n" + getUsedFor();
    }

    public String getVendorString() {
        return getID() + "\n" + getSerialNumber() + "\n" + getDenomination() + "\n" + getStatus() + "\n" + getExpirationDate() + "\n" + getTransferable() + "\n" + getOwner();
    }

    public static Coupon readData(StreamTokenizer in) {
        Coupon myCoupon = null;
        long serialNo;
        long ID;
        int denomination;
        String status;
        String expirationDate;
        String owner;
        String redemptionDate;
        String usedFor;
        String usedBy;
        String bankSig;
        boolean transferable;
        try {
            if (in.nextToken() == in.TT_EOF) return null;
            in.nextToken();
            if (in.sval.equals("COUPON")) {
                in.nextToken();
                in.nextToken();
                serialNo = (long) in.nval;
                in.nextToken();
                ID = (long) in.nval;
                in.nextToken();
                denomination = (int) in.nval;
                in.nextToken();
                status = in.sval;
                in.nextToken();
                expirationDate = in.sval;
                in.nextToken();
                transferable = false;
                in.nextToken();
                owner = in.sval;
                in.nextToken();
                redemptionDate = in.sval;
                in.nextToken();
                usedBy = in.sval;
                in.nextToken();
                usedFor = in.sval;
                in.nextToken();
                bankSig = in.sval;
                in.nextToken();
                myCoupon = new Coupon(serialNo, ID, denomination, status, expirationDate, transferable, owner, redemptionDate, usedBy, usedFor, bankSig);
                System.out.println(myCoupon.toString());
                return myCoupon;
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return myCoupon;
        }
        return myCoupon;
    }

    public String toString() {
        return "COUPON - " + getSerialNumber() + " " + getID() + " " + getDenomination() + " " + getStatus() + " " + getExpirationDate() + " " + getTransferable() + " " + getOwner() + " " + getRedemptionDate() + " " + getUsedBy() + " " + getUsedFor() + " " + getBankSignatureString() + " " + getVendorSignatureString();
    }
}
