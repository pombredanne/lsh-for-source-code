package com.Localytics.android;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.provider.Settings.System;

/**
 * Provides a number of static functions to aid in the collection and formatting
 * of datapoints.
 * @author Localytics
 */
public final class DatapointHelper {

    private DatapointHelper() {
    }

    private static final String LOG_PREFIX = "(DatapointHelper) ";

    private static final String DROID2_ANDROID_ID = "9774d56d682e549c";

    public static final String CONTROLLER_SESSION = "- c: se\n";

    public static final String CONTROLLER_EVENT = "- c: ev\n";

    public static final String CONTROLLER_OPT = "- c: optin\n";

    public static final String ACTION_CREATE = "  a: c\n";

    public static final String ACTION_UPDATE = "  a: u\n";

    public static final String ACTION_OPTIN = "  a: optin\n";

    public static final String OBJECT_SESSION_DP = "  se:\n";

    public static final String OBJECT_EVENT_DP = "  ev:\n";

    public static final String OBJECT_OPT = "  optin:\n";

    public static final String EVENT_ATTRIBUTE = "   attrs:\n";

    public static final String PARAM_UUID = "u";

    public static final String PARAM_APP_UUID = "au";

    public static final String PARAM_APP_VERSION = "av";

    public static final String PARAM_SESSION_UUID = "su";

    public static final String PARAM_DEVICE_UUID = "du";

    public static final String PARAM_DEVICE_PLATFORM = "dp";

    public static final String PARAM_DEVICE_MAKE = "dma";

    public static final String PARAM_DEVICE_MODEL = "dmo";

    public static final String PARAM_OS_VERSION = "dov";

    public static final String PARAM_DEVICE_COUNTRY = "dc";

    public static final String PARAM_LOCALE_COUNTRY = "dlc";

    public static final String PARAM_LOCALE_LANGUAGE = "dll";

    public static final String PARAM_LOCALE = "dl";

    public static final String PARAM_NETWORK_COUNTRY = "nc";

    public static final String PARAM_NETWORK_CARRIER = "nca";

    public static final String PARAM_NETWORK_MNC = "mnc";

    public static final String PARAM_NETWORK_MCC = "mcc";

    public static final String PARAM_DATA_CONNECTION = "dac";

    public static final String PARAM_LIBRARY_VERSION = "lv";

    public static final String PARAM_LOCATION_SOURCE = "ls";

    public static final String PARAM_LOCATION_LAT = "lat";

    public static final String PARAM_LOCATION_LNG = "lng";

    public static final String PARAM_CLIENT_TIME = "ct";

    public static final String PARAM_CLIENT_CLOSED_TIME = "ctc";

    public static final String PARAM_EVENT_NAME = "n";

    public static final String PARAM_OPT_VALUE = "optin";

    /**
     * Returns the given key/value pair as a YAML string.  This string is intended to be
     * used to define values for the first level of data in the YAML file.  This is
     * different from the datapoints which belong another level in. 
     * @param paramName The name of the parameter 
     * @param paramValue The value of the parameter
     * @param paramIndent The indent level of the parameter
     * @return a YAML string which can be dumped to the YAML file
     */
    public static String formatYAMLLine(String paramName, String paramValue, int paramIndent) {
        if (paramName.length() > LocalyticsSession.MAX_NAME_LENGTH) {
            Log.v(DatapointHelper.LOG_PREFIX, "Parameter name exceeds " + LocalyticsSession.MAX_NAME_LENGTH + " character limit.  Truncating.");
            paramName = paramName.substring(0, LocalyticsSession.MAX_NAME_LENGTH);
        }
        if (paramValue.length() > LocalyticsSession.MAX_NAME_LENGTH) {
            Log.v(DatapointHelper.LOG_PREFIX, "Parameter value exceeds " + LocalyticsSession.MAX_NAME_LENGTH + " character limit.  Truncating.");
            paramValue = paramValue.substring(0, LocalyticsSession.MAX_NAME_LENGTH);
        }
        StringBuffer formattedString = new StringBuffer();
        for (int currentIndent = 0; currentIndent < paramIndent; currentIndent++) {
            formattedString.append(" ");
        }
        formattedString.append(escapeString(paramName));
        formattedString.append(": ");
        formattedString.append(escapeString(paramValue));
        formattedString.append("\n");
        return formattedString.toString();
    }

    /**
     * Gets a 1-way hashed value of the device's unique ID.  This value is encoded using a SHA-256
     * one way hash and cannot be used to determine what device this data came from.
     * @param appContext The context used to access the settings resolver
     * @return An 1-way hashed identifier unique to this device or null if an ID, or the hashing
     * algorithm is not available. 
     */
    public static String getGlobalDeviceId(final Context appContext) {
        String systemId = System.getString(appContext.getContentResolver(), System.ANDROID_ID);
        if (systemId == null || systemId.toLowerCase().equals(DROID2_ANDROID_ID)) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(systemId.getBytes());
            BigInteger hashedNumber = new BigInteger(1, digest);
            return new String(hashedNumber.toString(16));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Determines the type of network this device is connected to.
     * @param appContext the context used to access the device's WIFI
     * @param telephonyManager The manager used to access telephony info
     * @return The type of network, or unknown if the information is unavailable
     */
    public static String getNetworkType(final Context appContext, TelephonyManager telephonyManager) {
        WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
        try {
            if (wifiManager.isWifiEnabled()) {
                return "wifi";
            }
        } catch (Exception e) {
        }
        switch(telephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "edge";
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "unknown";
        }
        return "none";
    }

    /**
     * Gets the pretty string for this application's version.
     * @param appContext The context used to examine packages
     * @return The application's version as a pretty string
     */
    public static String getAppVersion(final Context appContext) {
        PackageManager pm = appContext.getPackageManager();
        try {
            return pm.getPackageInfo(appContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    /**
     * Gets the current time, along with local timezone, formatted as a DateTime for the webservice. 
     * @return a DateTime of the current local time and timezone.
     */
    public static String getTimeAsDatetime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-00:00");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    /**
     * Escapes strings for YAML parser
     * @param rawString The string we want to escape.
     * @return An escaped string ready for YAML
     */
    private static String escapeString(String rawString) {
        StringBuffer parseString = new StringBuffer("\"");
        int startRead = 0;
        int stopRead = 0;
        int bufferLength = rawString.length();
        while (stopRead < bufferLength) {
            if (rawString.charAt(stopRead) == '\"' || rawString.charAt(stopRead) == '\\') {
                parseString.append(rawString.substring(startRead, stopRead));
                parseString.append('\\');
                startRead = stopRead;
            } else if (rawString.charAt(stopRead) == '\0') {
                parseString.append(rawString.substring(startRead, stopRead));
                startRead = stopRead + 1;
            }
            stopRead++;
        }
        parseString.append(rawString.substring(startRead, stopRead));
        parseString.append('\"');
        return parseString.toString();
    }
}
