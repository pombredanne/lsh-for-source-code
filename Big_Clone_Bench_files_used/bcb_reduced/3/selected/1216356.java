package org.com.cnc.common.android;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.view.Display;

public class CommonDeviceId {

    public static final String TYPE_ID_IMEI = "IMEI";

    public static final String TYPE_ID_IPSEUDO_UNIQUE_ID = "Pseudo_Unique_Id";

    public static final String TYPE_ID_IANDROIDID = "AndroidId";

    public static final String TYPE_ID_IWLAN_MAC_ADDRESS = "WLAN_MAC_Address";

    public static final String TYPE_ID_IBT_MAC_ADDRESS = "BT_MAC_Address";

    public static final String TYPE_ID_ICOMBINED_DEVICE_ID = "Combined_Device_ID";

    public static final int SIZE_WIDTH_Y = 240;

    public static final int SIZE_HEIGHT_Y = 320;

    public static final int SIZE_WIDTH_EMULATOR_16 = 320;

    public static final int SIZE_HEIGHT_EMULATOR_16 = 480;

    public static final int SIZE_WIDTH_S = 480;

    public static final int SIZE_HEIGHT_S = 800;

    public static final int SIZE_WIDTH_TAB = 600;

    public static final int SIZE_HEIGHT_TAB = 1024;

    public static final int SIZE_WIDTH_VIEWSONIC = 600;

    public static final int SIZE_HEIGHT_VIEWSONIC = 1024;

    public static boolean isTablet(Activity context) {
        Display display = context.getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        int min = width < height ? width : height;
        if (min > SIZE_WIDTH_S) {
            return true;
        }
        return false;
    }

    public static int getWidth(Activity context) {
        Display display = context.getWindowManager().getDefaultDisplay();
        return display.getWidth();
    }

    public static int getHeight(Activity context) {
        Display display = context.getWindowManager().getDefaultDisplay();
        return display.getHeight();
    }

    public static String deviceId(Context context, String type) {
        if (TYPE_ID_IANDROIDID.equals(type)) {
            return deviceIdFromAndroidId(context);
        } else if (TYPE_ID_IBT_MAC_ADDRESS.equals(type)) {
            return deviceIdFromBT_MAC_Address(context);
        } else if (TYPE_ID_ICOMBINED_DEVICE_ID.equals(type)) {
            return deviceIdFromCombined_Device_ID(context);
        } else if (TYPE_ID_IMEI.equals(type)) {
            return deviceIdFromIMEI(context);
        } else if (TYPE_ID_IPSEUDO_UNIQUE_ID.equals(type)) {
            return deviceIdFromIMEI(context);
        } else if (TYPE_ID_IWLAN_MAC_ADDRESS.equals(type)) {
            return deviceIdFromWLAN_MAC_Address(context);
        }
        return null;
    }

    private static String deviceIdFromIMEI(Context context) {
        try {
            TelephonyManager TelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return TelephonyMgr.getDeviceId();
        } catch (Exception e) {
            return null;
        }
    }

    private static String deviceIdFromPseudo_Unique_Id() {
        StringBuilder builder = new StringBuilder();
        builder.append("35");
        builder.append(Build.BOARD.length() % Common.SIZE_10);
        builder.append(Build.BRAND.length() % Common.SIZE_10);
        builder.append(Build.CPU_ABI.length() % Common.SIZE_10);
        builder.append(Build.DEVICE.length() % Common.SIZE_10);
        builder.append(Build.DISPLAY.length() % Common.SIZE_10);
        builder.append(Build.HOST.length() % Common.SIZE_10);
        builder.append(Build.ID.length() % Common.SIZE_10);
        builder.append(Build.MANUFACTURER.length() % Common.SIZE_10);
        builder.append(Build.MODEL.length() % Common.SIZE_10);
        builder.append(Build.PRODUCT.length() % Common.SIZE_10);
        builder.append(Build.TAGS.length() % Common.SIZE_10);
        builder.append(Build.TYPE.length() % Common.SIZE_10);
        builder.append(Build.USER.length() % Common.SIZE_10);
        return builder.toString();
    }

    private static String deviceIdFromAndroidId(Context context) {
        try {
            ContentResolver cr = context.getContentResolver();
            return Secure.getString(cr, Secure.ANDROID_ID);
        } catch (Exception e) {
            return null;
        }
    }

    private static String deviceIdFromWLAN_MAC_Address(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return wm.getConnectionInfo().getMacAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private static String deviceIdFromBT_MAC_Address(Context context) {
        try {
            BluetoothAdapter m_BluetoothAdapter = null;
            m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return m_BluetoothAdapter.getAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private static String deviceIdFromCombined_Device_ID(Context context) {
        StringBuilder builder = new StringBuilder();
        builder.append(deviceIdFromIMEI(context));
        builder.append(deviceIdFromPseudo_Unique_Id());
        builder.append(deviceIdFromAndroidId(context));
        builder.append(deviceIdFromWLAN_MAC_Address(context));
        builder.append(deviceIdFromBT_MAC_Address(context));
        String m_szLongID = builder.toString();
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        m.update(m_szLongID.getBytes(), 0, m_szLongID.length());
        byte p_md5Data[] = m.digest();
        String m_szUniqueID = new String();
        for (int i = 0; i < p_md5Data.length; i++) {
            int b = (0xFF & p_md5Data[i]);
            if (b <= 0xF) m_szUniqueID += "0";
            m_szUniqueID += Integer.toHexString(b);
        }
        return m_szUniqueID;
    }

    public static boolean canCallPhone(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getSimSerialNumber() != null) {
            if (telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
                return true;
            }
        }
        return false;
    }

    public static void rescanSdcard(Context context) {
        new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addDataScheme("file");
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
    }
}
