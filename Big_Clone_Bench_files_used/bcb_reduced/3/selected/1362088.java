package com.xiaolei.android.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import org.xmlpull.v1.XmlSerializer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import com.xiaolei.android.BizTracker.BizTracker;
import com.xiaolei.android.BizTracker.R;

public final class Utility {

    public static Location LatestLocation = null;

    private static final int DAYS_OF_WEEK = 7;

    public static Date getCurrentDateTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        format.format(now);
        return now;
    }

    public static String getSqliteDateTimeString(Date datetime) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(datetime);
    }

    public static String getCurrentDateTimeString() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return format.format(now);
    }

    public static String getLocalCurrentDateTimeString() {
        Date now = new Date();
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.getDefault());
        return formatter.format(now);
    }

    public static String getLocalCurrentDateString() {
        Date now = new Date();
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
        return formatter.format(now);
    }

    public static Date convertToDate(String dateString) {
        Date result = null;
        if (TextUtils.isEmpty(dateString)) {
            return result;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            result = dateFormat.parse(dateString);
            return result;
        } catch (ParseException e) {
            return result;
        }
    }

    public static String toLocalDateTimeString(String datetime) {
        if (TextUtils.isEmpty(datetime)) {
            return "";
        }
        try {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date dt = dateFormat.parse(datetime);
            String result = formatter.format(dt);
            return result;
        } catch (ParseException e) {
            return "";
        }
    }

    public static String toLocalTimeString(String datetime) {
        if (TextUtils.isEmpty(datetime)) {
            return "";
        }
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date dt = dateFormat.parse(datetime);
            String result = toLocalTimeString(dt);
            return result;
        } catch (ParseException e) {
            return "";
        }
    }

    public static String toLocalDateTimeString(Date datetime) {
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.getDefault());
        String result = formatter.format(datetime);
        return result;
    }

    public static String toLocalTimeString(Date datetime) {
        DateFormat formatter = DateFormat.getTimeInstance(DateFormat.DEFAULT);
        String result = formatter.format(datetime);
        return result;
    }

    public static String toLocalDateString(String datetime) {
        if (TextUtils.isEmpty(datetime)) {
            return "";
        }
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date dt = dateFormat.parse(datetime);
            DateFormat formatter = DateFormat.getDateInstance(DateFormat.DEFAULT);
            String result = formatter.format(dt);
            return result;
        } catch (ParseException e) {
            return "";
        }
    }

    public static String toLocalDateString(Context context, Date date) {
        String result = DateUtils.formatDateTime(context, date.getTime(), DateUtils.FORMAT_SHOW_DATE);
        return result;
    }

    public static void hideSoftIME(Activity context) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Date parseDate(String date, Date defaultValue) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            return defaultValue;
        }
    }

    /**
	 * Format money with the specified currency code. If the currency code is
	 * not presented, use the system default local currency.
	 * 
	 * @value the money value
	 * @currencyCode the currency code you want to use to format the money
	 * */
    public static String formatCurrency(double value, String currencyCode) {
        return formatCurrency(value, currencyCode, true);
    }

    public static String formatCurrency(double value, String currencyCode, Boolean showCurrencyCode) {
        Currency currency = Currency.getInstance(Locale.getDefault());
        if (!TextUtils.isEmpty(currencyCode)) {
            currency = Currency.getInstance(currencyCode);
        }
        NumberFormat format = NumberFormat.getCurrencyInstance();
        if (format != null && (format instanceof DecimalFormat)) {
            DecimalFormat decFormat = (DecimalFormat) format;
            decFormat.setCurrency(currency);
            decFormat.setDecimalSeparatorAlwaysShown(true);
            if (value != 0) {
                decFormat.setNegativePrefix("-");
                decFormat.setPositivePrefix("+");
            } else {
                decFormat.setNegativePrefix("");
                decFormat.setPositivePrefix("");
            }
            return format.format(value) + (showCurrencyCode ? " " + currencyCode : "");
        } else {
            if (!TextUtils.isEmpty(currencyCode) && showCurrencyCode == true) {
                return (value > 0 ? "+" : "") + String.format("%.2f %s", value, currencyCode);
            } else {
                return (value > 0 ? "+" : "") + String.format("%.2f", value);
            }
        }
    }

    public static Date getStartDayOfThisWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        return cal.getTime();
    }

    public static Date getStartDayOfWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        return cal.getTime();
    }

    public static Date getEndDayOfWeek(Date date) {
        Date startDate = getStartDayOfWeek(date);
        Date result = Utility.addDays(startDate, DAYS_OF_WEEK - 1);
        return result;
    }

    public static Date getStartDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
    }

    public static Date getStartDayOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        return cal.getTime();
    }

    public static int getDaysOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int result = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
        return result;
    }

    public static Date getEndDayOfYear(Date date) {
        Date startDay = getStartDayOfYear(date);
        int days = getDaysOfYear(date);
        Date result = addDays(startDay, days - 1);
        result = getEndTimeOfDate(result);
        return result;
    }

    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + days);
        Date result = cal.getTime();
        return result;
    }

    public static Date getEndTimeOfDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date result = cal.getTime();
        return result;
    }

    public static int getDaysInMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static Date getEndDayOfMonth(Date date) {
        Date startDay = getStartDayOfMonth(date);
        int days = getDaysInMonth(date);
        return addDays(startDay, days - 1);
    }

    public static void showMessageBox(Context context, String title, String message, DialogInterface.OnClickListener okButtonClickListener) {
        new AlertDialog.Builder(context).setTitle(title).setMessage(message).setIcon(android.R.drawable.ic_dialog_info).setPositiveButton(android.R.string.yes, okButtonClickListener).show();
    }

    public static void showMessageBox(Context context, int iconResId, String title, String message, DialogInterface.OnClickListener okButtonClickListener) {
        new AlertDialog.Builder(context).setTitle(title).setMessage(message).setIcon(iconResId).setPositiveButton(android.R.string.yes, okButtonClickListener).show();
    }

    public static AlertDialog showConfirmDialog(Context context, String title, String message, DialogInterface.OnClickListener yesButtonClickListener) {
        AlertDialog result = new AlertDialog.Builder(context).setTitle(title).setMessage(message).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(android.R.string.yes, yesButtonClickListener).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        }).show();
        return result;
    }

    public static AlertDialog showConfirmDialog(Context context, String title, String message, DialogInterface.OnClickListener yesButtonClickListener, DialogInterface.OnClickListener cancelButtonClickListener) {
        if (cancelButtonClickListener == null) {
            cancelButtonClickListener = new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            };
        }
        AlertDialog result = new AlertDialog.Builder(context).setTitle(title).setMessage(message).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(android.R.string.yes, yesButtonClickListener).setNegativeButton(android.R.string.no, cancelButtonClickListener).show();
        return result;
    }

    public static String toMD5String(String plainText) {
        if (TextUtils.isEmpty(plainText)) {
            plainText = "";
        }
        StringBuilder text = new StringBuilder();
        for (int i = plainText.length() - 1; i >= 0; i--) {
            text.append(plainText.charAt(i));
        }
        plainText = text.toString();
        MessageDigest mDigest;
        try {
            mDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return plainText;
        }
        mDigest.update(plainText.getBytes());
        byte d[] = mDigest.digest();
        StringBuffer hash = new StringBuffer();
        for (int i = 0; i < d.length; i++) {
            hash.append(Integer.toHexString(0xFF & d[i]));
        }
        return hash.toString();
    }

    /**
	 * Use an one way encrypt algorithm to encrypt the plain text then returns
	 * the hex string.
	 * 
	 * @param plainText
	 *            plain text you want to encrypt
	 * @return the encrypted hex string
	 */
    public static String encrypt(String plainText) {
        if (TextUtils.isEmpty(plainText)) {
            plainText = "";
        }
        StringBuilder text = new StringBuilder();
        for (int i = plainText.length() - 1; i >= 0; i--) {
            text.append(plainText.charAt(i));
        }
        plainText = text.toString();
        MessageDigest mDigest;
        try {
            mDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return plainText;
        }
        mDigest.update(plainText.getBytes());
        byte d[] = mDigest.digest();
        StringBuffer hash = new StringBuffer();
        for (int i = 0; i < d.length; i++) {
            hash.append(Integer.toHexString(0xFF & d[i]));
        }
        return hash.toString();
    }

    @SuppressWarnings("unused")
    private void writeXml(String fullFileName, String namespace, String rootNodeName, String childNodeName, Cursor cursor) throws IOException {
        if (cursor == null) {
            return;
        }
        XmlSerializer serializer = Xml.newSerializer();
        File xmlFile = new File(fullFileName);
        FileWriter writer = new FileWriter(xmlFile);
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag(namespace, rootNodeName);
            if (cursor.moveToFirst()) {
                while (cursor.moveToNext()) {
                    int columnCount = cursor.getColumnCount();
                    serializer.startTag(namespace, childNodeName);
                    for (int i = 0; i < columnCount; i++) {
                        String columnName = cursor.getColumnName(i);
                        String cellValue = cursor.getString(i);
                        serializer.attribute(namespace, columnName, cellValue);
                    }
                    serializer.endTag(namespace, childNodeName);
                    serializer.flush();
                }
            }
            serializer.endTag(namespace, rootNodeName);
            serializer.endDocument();
        } finally {
            writer.close();
        }
    }

    public static AlertDialog showDialog(Context context, int layoutResourceId, String title, DialogInterface.OnClickListener okButtonOnClickListener, DialogInterface.OnClickListener cancelButtonOnClickListener) {
        return showDialog(context, layoutResourceId, android.R.drawable.ic_dialog_info, title, okButtonOnClickListener, cancelButtonOnClickListener, null);
    }

    public static AlertDialog showDialog(Context context, int layoutResourceId, String title, DialogInterface.OnClickListener okButtonOnClickListener, DialogInterface.OnClickListener cancelButtonOnClickListener, OnShowListener onShowListener) {
        return showDialog(context, layoutResourceId, android.R.drawable.ic_dialog_info, title, okButtonOnClickListener, cancelButtonOnClickListener, onShowListener);
    }

    public static AlertDialog showDialog(final Context context, int layoutResourceId, int iconResourceId, String title, DialogInterface.OnClickListener okButtonOnClickListener, DialogInterface.OnClickListener cancelButtonOnClickListener, OnShowListener onShowListener) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(layoutResourceId, null);
        builder.setView(layout);
        builder.setTitle(title);
        builder.setIcon(iconResourceId);
        builder.setPositiveButton(context.getString(android.R.string.ok), okButtonOnClickListener);
        if (cancelButtonOnClickListener != null) {
            builder.setNegativeButton(context.getString(android.R.string.cancel), cancelButtonOnClickListener);
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(onShowListener);
        alertDialog.show();
        return alertDialog;
    }

    public static boolean dateEquals(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.getYear() == date2.getYear() && date1.getMonth() == date2.getMonth() && date1.getDate() == date2.getDate();
    }

    public static Date replaceWithCurrentTime(Date date) {
        if (date == null) {
            return new Date();
        }
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, now.getHours());
        cal.set(Calendar.MINUTE, now.getMinutes());
        cal.set(Calendar.SECOND, now.getSeconds());
        return cal.getTime();
    }

    public static void goToAndroidMarket(Context context) {
        Uri marketUri = Uri.parse("https://market.android.com/details?id=" + context.getPackageName());
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
        context.startActivity(marketIntent);
    }

    public static String getVersion(Context context) {
        String version = context.getString(R.string.unknown);
        PackageManager manager = context.getPackageManager();
        PackageInfo info;
        try {
            info = manager.getPackageInfo(context.getPackageName(), 0);
            version = info.versionName;
        } catch (NameNotFoundException e) {
            version = context.getString(R.string.unknown);
        }
        return version;
    }

    public static Boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public static String getPhotoStoragePath() {
        String result = "";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            result = Environment.getExternalStorageDirectory() + File.separator + BizTracker.PHOTO_PATH;
        }
        return result;
    }

    public static Boolean fileExists(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        File file = new File(fileName);
        return file.exists();
    }

    public static AbsolutePhotoFileInfo getAbsolutePhotoFileName(String shortPhotoFileName) {
        AbsolutePhotoFileInfo result = new AbsolutePhotoFileInfo();
        Boolean fileExists = false;
        String finalFileName = shortPhotoFileName;
        if (fileExists(shortPhotoFileName)) {
            finalFileName = shortPhotoFileName;
            fileExists = true;
        } else {
            String fullFileName = getPhotoStoragePath() + File.separator + shortPhotoFileName;
            if (fileExists(fullFileName)) {
                finalFileName = fullFileName;
                fileExists = true;
            }
        }
        result.AbsolutePhotoFileName = finalFileName;
        result.FileExists = fileExists;
        return result;
    }

    public static Bitmap getScaledBitmap(String fullFileName, int desiredWidth, int desiredHeight) {
        if (TextUtils.isEmpty(fullFileName)) {
            return null;
        }
        File file = new File(fullFileName);
        if (file.exists() != true) {
            return null;
        }
        if (desiredWidth <= 0 || desiredHeight <= 0) {
            return null;
        }
        Bitmap result = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inTempStorage = new byte[16 * 1024];
        options.inJustDecodeBounds = true;
        result = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        double sampleSize = 1;
        Boolean scaleByHeight = Math.abs(options.outHeight - desiredHeight) >= Math.abs(options.outWidth - desiredWidth);
        sampleSize = scaleByHeight ? options.outHeight / desiredHeight : options.outWidth / desiredWidth;
        options.inSampleSize = (int) Math.pow(2d, Math.floor(Math.log(sampleSize) / Math.log(2d)));
        Log.i("DEBUG", String.format("inSampleSize: %f", sampleSize));
        options.inJustDecodeBounds = false;
        String orientation = "";
        try {
            ExifInterface exifReader = new ExifInterface(file.getAbsolutePath());
            orientation = exifReader.getAttribute(ExifInterface.TAG_ORIENTATION);
            Log.i("DEBUG", String.format("TAG_ORIENTATION: %s", orientation));
        } catch (IOException e) {
        }
        result = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (!TextUtils.isEmpty(orientation)) {
            try {
                int exifOrientation = Integer.parseInt(orientation);
                int degrees = 0;
                switch(exifOrientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degrees = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degrees = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degrees = 270;
                        break;
                }
                if (degrees > 0) {
                    result = Utility.rotate(result, degrees);
                }
            } catch (Exception ex) {
            }
        }
        return result;
    }

    public static Bitmap rotate(Bitmap bmp, int degrees) {
        if (degrees != 0 && bmp != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                if (bmp != b2) {
                    bmp.recycle();
                    bmp = b2;
                }
            } catch (OutOfMemoryError ex) {
            }
        }
        return bmp;
    }

    public static String getPhotoPathFromGallery(Activity activity, Uri photoUri) {
        if (photoUri == null) {
            return "";
        }
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = activity.managedQuery(photoUri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    /**
	 * Sets the required flags on the dialog window to enable input method
	 * window to show up.
	 */
    public static void requestInputMethod(Dialog dialog) {
        try {
            Window window = dialog.getWindow();
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        } catch (Exception ex) {
        }
    }

    public static String getDefaultPhotoFolderPath(Context context) {
        String result = Environment.DIRECTORY_PICTURES;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            result = Environment.getExternalStorageDirectory() + "/" + "BizTracker/photo";
            File folder = new File(result);
            if (!folder.exists()) {
                boolean photoPathExists = folder.mkdirs();
                if (photoPathExists == false) {
                    Toast.makeText(context, context.getString(R.string.create_photo_folder_failed), Toast.LENGTH_SHORT).show();
                }
            }
        }
        return result;
    }

    public static String getDefaultAudioNotesFolderPath(Context context) {
        String result = Environment.DIRECTORY_MUSIC;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            result = Environment.getExternalStorageDirectory() + "/" + "BizTracker/audio";
            File folder = new File(result);
            if (!folder.exists()) {
                boolean photoPathExists = folder.mkdirs();
                if (photoPathExists == false) {
                    Toast.makeText(context, "Failed to create audio notes folder: " + result, Toast.LENGTH_SHORT).show();
                }
            }
        }
        return result;
    }

    public static String getAudioFullFileName(Context context, String shortFileName) {
        String audioPath = getDefaultAudioNotesFolderPath(context);
        return audioPath + File.separator + shortFileName;
    }

    public static String getFileNameWithoutPath(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return fileName;
        }
        File file = new File(fileName);
        return file.getName();
    }
}
