package com.tylerhjones.boip.client1;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

public class Common {

    /** Application constants ************************************************************ */
    public static final String APP_AUTHOR = "@string/author";

    public static final String APP_VERSION = "@string/versionnum";

    /** Database constants *************************************************************** */
    public static final String DB_NAME = "servers";

    public static final int DB_VERSION = 1;

    public static final String TABLE_SERVERS = "servers";

    public static final String S_FIELD_INDEX = "idx";

    public static final String S_FIELD_NAME = "name";

    public static final String S_FIELD_HOST = "host";

    public static final String S_FIELD_PORT = "port";

    public static final String S_FIELD_PASS = "pass";

    public static final String S_INDEX = "idx";

    public static final String S_NAME = "name";

    public static final String S_HOST = "host";

    public static final String S_PORT = "port";

    public static final String S_PASS = "pass";

    public static final String PREFS = "boip_client";

    public static final String PREF_VERSION = "version";

    public static final String PREF_CURSRV = "curserver";

    /** Default value constants *********************************************************** */
    public static final int DEFAULT_PORT = 41788;

    public static final String DEFAULT_HOST = "0.0.0.0";

    public static final String DEFAULT_PASS = "none";

    public static final String DEFAULT_NAME = "Untitled";

    /** Network communication message constants ******************************************** */
    public static final String OK = "OK";

    public static final String NOPE = "NOPE";

    public static final String THANKS = "THANKS";

    public static final String SMC = ";";

    public static final String DSEP = "||";

    public static final String CHECK = "CHECK";

    public static final String ERR = "ERR";

    /** Constants for keeping track of activities ****************************************** */
    public static final int ADD_SREQ = 91;

    public static final int EDIT_SREQ = 105;

    public static final int BARCODE_SREQ = 11;

    public static Hashtable<String, String> errorCodes() {
        Hashtable<String, String> errors = new Hashtable<String, String>(13);
        errors.put("ERR1", "Invalid data and/or request syntax!");
        errors.put("ERR2", "Invalid data, possible missing data separator.");
        errors.put("ERR3", "Invalid data/syntax, could not parse data.");
        errors.put("ERR4", "Missing/Empty Command Argument(s) Recvd.");
        errors.put("ERR5", "Invalid command syntax!");
        errors.put("ERR6", "Invalid Auth Syntax!");
        errors.put("ERR7", "Access Denied!");
        errors.put("ERR8", "Server Timeout, Too Busy to Handle Request!");
        errors.put("ERR9", "Incorrect Password.");
        errors.put("ERR14", "Invalid Login Command Syntax.");
        errors.put("ERR19", "Unknown Auth Error");
        errors.put("ERR99", "Unknown exception occured.");
        errors.put("ERR100", "Invalid Host/IP.");
        errors.put("ERR101", "Cannont connect to server.");
        return errors;
    }

    public static void showMsgBox(Context c, String title, String msg) {
        AlertDialog ad = new AlertDialog.Builder(c).create();
        ad.setCancelable(false);
        ad.setMessage(msg);
        ad.setTitle(title);
        ad.setButton(OK, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.show();
    }

    public static String getAppVersion(Context c, @SuppressWarnings("rawtypes") Class cls) {
        try {
            ComponentName comp = new ComponentName(c, cls);
            PackageInfo pinfo = c.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
            return pinfo.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static void showAbout(Context c) {
        final TextView message = new TextView(c);
        final SpannableString s = new SpannableString(c.getText(R.string.about_msg_body));
        Linkify.addLinks(s, Linkify.WEB_URLS);
        message.setText(s);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        AlertDialog adialog = new AlertDialog.Builder(c).setTitle(R.string.about_msg_title).setCancelable(true).setIcon(android.R.drawable.ic_dialog_info).setPositiveButton(R.string.close, null).setView(message).create();
        adialog.show();
        ((TextView) message).setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static boolean isNetworked(Context c) {
        ConnectivityManager mManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo current = mManager.getActiveNetworkInfo();
        if (current == null) {
            return false;
        }
        return (current.getState() == NetworkInfo.State.CONNECTED);
    }

    public static boolean isValidPort(String port) {
        try {
            int p = Integer.parseInt(port);
            if (p < 1 || p > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static String convertToHex_better(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        int length = data.length;
        for (int i = 0; i < length; ++i) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (++two_halfs < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes());
        byte byteData[] = md.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
