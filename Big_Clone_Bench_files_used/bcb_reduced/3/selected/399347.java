package com.gpfcomics.android.cryptnos;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.TigerDigest;
import org.bouncycastle.crypto.digests.WhirlpoolDigest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

/**
 * The core Application class for the Cryptnos program.  The primary purpose
 * of this class is to handle the loading and refreshing of the site list
 * for dependent Activities.  The Cryptnos site list is stored in the database
 * as encrypted text and must be decrypted before it can be used.  Originally,
 * each Activity loaded the site list on its own, which created a lot of
 * duplicated code and a lot of extra load times.  The original theory behind
 * this was security, but the caveats eventually outweighed the benefits.
 * 
 * Now each Activity that wants access to the site list can simply reference
 * this central class and implement the SiteListListener interface.  The site
 * requests the site list from the application core.  If the site list has
 * already been built, it is instantly returned, saving a lot of time and work.
 * Otherwise, this class does the grunt work of loading the sites from the
 * database, decrypting them, and storing them in a String array.  Once this
 * is complete, the class contacts the SiteListListener and passes it the array
 * to do with as it pleases.  If at any time an Activity modifies the database
 * (such as adding, deleting, or modifying a set of site parameters), it should
 * mark the list as "dirty", forcing it to be reloaded the next time it is
 * requested.
 * @author Jeffrey T. Darlington
 * @version 1.3.1
 * @since 1.0
 */
public class CryptnosApplication extends Application {

    /** A constant identifying the progress dialog used during the site
	 *  list load.  Activities implementing the SiteListListener interface
	 *  will to use this for their showDialog()/onCreateDialog() methods. */
    public static final int DIALOG_PROGRESS = 5000;

    /** A constant identifying the warning dialog displayed if the user
	 *  upgrades Cryptnos from an old version to 1.2.0, where we try to enforce
	 *  UTF-8 encoding. */
    public static final int DIALOG_UPGRADE_TO_UTF8 = 5001;

    /** This integer constant lets us define a limit beyond which cryptographic
	 *  hash generation seems to be excessive.  Anything below this should be
	 *  fine and fairly quick; anything above this may cause the application
	 *  to appear sluggish or non-responsive.  At one point, I considered
	 *  putting password generation with iterations higher than this behind a
	 *  ProgressDialog/Thread model, but for now we're using this as an upper
	 *  limit on the number of iterations the user can choose from.  There's
	 *  no science behind this number aside from casual testing, both in the
	 *  SDK emulator and on my personal Motorola Droid.  If there was a
	 *  significant pause observed, that's were I set the limit. */
    public static final int HASH_ITERATION_WARNING_LIMIT = 500;

    /** The cryptographic key factory definition.  This will be used by most
	 *  cryptography functions throughout the application (with the exception
	 *  being the new cross-platform import/export format).  Note that this
	 *  will be a "password-based encryption" (PBE) cipher (specifically 
	 *  256-bit AES as of this writing), so take that into account when
	 *  using this value. */
    public static final String KEY_FACTORY = "PBEWITHSHA-256AND256BITAES-CBC-BC";

    /** The number of iterations used for cryptographic key generation, such
	 *  as in creating an AlgorithmParameterSpec.  Ideally, this should be
	 *  fairly high, but we'll use a modest value for performance. */
    public static final int KEY_ITERATION_COUNT = 50;

    /** The length of generated encryption keys.  This will be used for
	 *  generating encryption key specs. */
    public static final int KEY_LENGTH = 32;

    /** Our encryption "salt" for site parameter data.  This value should be
	 *  unique per device and will be used repeatedly, so generating it once
	 *  and storing it in a single place has significant advantages.  Note
	 *  that this is only really used for storing parameters in the database;
	 *  import/export operations will use a salt generated from the user's
	 *  password, which is not device dependent. */
    public static byte[] PARAMETER_SALT = null;

    /** The cryptographic hash to use to generate encryption salts.  Pass this
	 *  into MessageDigest.getInstance() to get the MessageDigest for salt
	 *  generation. */
    public static final String SALT_HASH = "SHA-512";

    /** The number of iterations used for salt generation.  All salts will be
	 *  run through the cryptographic hash specified by
	 *  CryptnosApplication.SALT_HASH this many times  before actually being
	 *  used. */
    public static final int SALT_ITERATION_COUNT = 10;

    /** The identifying string for UTF-8 text encoding. */
    public static final String TEXT_ENCODING_UTF8 = "UTF-8";

    /** The ID string for text encoding within the shared preferences file */
    public static final String PREFS_TEXT_ENCODING = "TEXT_ENCODING";

    /** The ID string for our version number within the shared preferences
	 *  file. */
    public static final String PREFS_VERSION = "VERSION";

    /** The ID string for our preferred file manager within the shared
	 *  preferences file. */
    public static final String PREFS_FILE_MANAGER = "FILE_MANAGER";

    /** The ID string for the copy to clipboard setting within the shared
	 *  preferences file. */
    public static final String PREFS_COPY_TO_CLIPBOARD = "COPY_TO_CLIPBOARD";

    /** The ID string for our preferred QR code scanner within the shared
	 *  preferences file. */
    public static final String PREFS_QRCODE_SCANNER = "QRCODE_SCANNER";

    /** The ID string for the show master passwords setting within the shared
	 *  preferences file. */
    public static final String PREFS_SHOW_MASTER_PASSWD = "SHOW_MASTER_PASSWD";

    /** The ID string for the clear passwords on focust loss setting within
	 *  the shared preferences file. */
    public static final String PREFS_CLEAR_PASSWDS_ON_FOCUS_LOSS = "CLEAR_PASSWDS_ON_FOCUS_LOSS";

    /** A random-ish string for salting ANDROID_ID.  If the device has never
	 *  been to the Market and never been assigned a unique ANDROID_ID (for
	 *  example, all of the emulators), ANDROID_ID will be null.  We will use
	 *  this string instead if ANDROID_ID is null, or concatenate ANDROID_ID
	 *  with this if it's present.  This string was generated by a little
	 *  script I have that pulls random data out of OpenSSL on Linux, so
	 *  hopefully it's random enough.  Of course, if we're using this all by
	 *  itself, PARAMETER_SALT will *NOT* be unique per device, but that's the
	 *  best we can do.*/
    private static final String SALTIER_SALT = "KnVcUpHHAB5K9HW2Vbq8D9CAk2P7sGiwhQLPeF6wI3UVSCTpJioStD4NFcrR1";

    /** Hashtables cannot have null keys or values.  While siteListHash uses
	 *  its keys to represent the site list for searching, we need some token
	 *  value to put in as the value for each key in order for the insert to
	 *  work.  This string will be inserted for each site key as the value.
	 *  since this value is never used, its actual value is irrelevant. */
    private static final String HASH_NULL = "NULL";

    /** The actual site list array.  When null, the site list is "dirty" and needs
	 *  to be fetched from the database.  If populated, the list is "clean" and can
	 *  be used directly, eliminating the expensive query operation. */
    private static String[] siteList = null;

    /** This hash table allows us to quickly search the site list for a specific
	 *  site.  Sites will be stored as hash keys; hash values are ignored. */
    private static Hashtable<String, String> siteListHash = null;

    /** A File representing the root of all import/export activities.  Files
	 *  will only be written or read from this path. */
    private static File importExportRoot = null;

    /** A common SharedPreferences object that can be used by all Cryptnos
	 *  Activities.  Use getPrefs() below to access it. */
    private static SharedPreferences prefs = null;

    /** The user's preference of text encoding.  The default will be the
	 *  system default, but ideally we want this to be UTF-8.  This value
	 *  will be stored in the Cryptnos shared preferences.  This can be read
	 *  and written to by getTextEncoding() and setTextEncoding()
	 *  respectively. */
    private static String textEncoding = TEXT_ENCODING_UTF8;

    /** A boolean flag to indicate whether or not the UpgradeManager has run for
	 *  this particular session. */
    private static boolean upgradeManagerRan = false;

    /** A boolean flag to determine whether or not to show a warning box when the
	 *  Advanced Settings option is selected from the main menu.  This should be
	 *  shown the first time this option is selected for a given session. */
    private static boolean showAdvancedSettingsWarning = true;

    /** A global FileManager object for the entire application */
    private static FileManager fileManager = null;

    /** A global QRCodeHandler object for the entire application */
    private static QRCodeHandler qrCodeHandler = null;

    /** A boolean flag indicating whether or not we should copy generated passwords
	 *  to the system clipboard. */
    private static boolean copyPasswordsToClipboard = true;

    /** A boolean flag indicating whether or not the user has selected to display
	 *  the master password while generating passwords. */
    private static boolean showMasterPassword = false;

    /** A boolena flag indicating the user's preference of whether or not the
	 *  master and generated password boxes should be cleared if Cryptnos goes
	 *  into the background (loses focus). */
    private static boolean clearPasswordsOnFocusLoss = false;

    /** The calling activity, so we can refer back to it.  This is usually the
	 *  same as the site list listener, but doesn't necessarily have to be. */
    private Activity caller = null;

    /** A special listener class that wants to know when the site list is
	 *  ready to be used.  This is usually the same as the caller Activity,
	 *  but doesn't necessarily have to be. */
    private SiteListListener listener = null;

    /** The parameter DB adapter.  Activities should call getDBHelper() to
	 *  get a reference to this object instead of creating their own new
	 *  database objects. */
    private static ParamsDbAdapter DBHelper = null;

    /** A ProgressDialog, which will be attached to the caller Activity but
	 *  which we'll directly control. */
    private static ProgressDialog progressDialog = null;

    /** A ListBuilderThread, which does the grunt work of building the list */
    private static ListBuilderThread listBuilderThread = null;

    /** This Hashtable contains a mapping of hash algorithm names to the length
	 *  of their Base64-encoded digest strings.  This is used primarily by the
	 *  New/Edit Parameters activity, which now uses a Spinner for character length
	 *  restrictions.  When a new hash algorithm is selected, we'll query this hash
	 *  table to find out what the maximum number of characters should be in the
	 *  Spinner.  We'll populate and store this once so the list can be queried as
	 *  many times as needed.*/
    private static Hashtable<String, Integer> hashLengths = null;

    @Override
    public void onCreate() {
        super.onCreate();
        DBHelper = new ParamsDbAdapter(this);
        DBHelper.open();
        importExportRoot = Environment.getExternalStorageDirectory();
        prefs = getSharedPreferences("CryptnosPrefs", Context.MODE_PRIVATE);
        try {
            textEncoding = prefs.getString(PREFS_TEXT_ENCODING, System.getProperty("file.encoding", TEXT_ENCODING_UTF8));
        } catch (Exception e) {
            textEncoding = prefs.getString(PREFS_TEXT_ENCODING, TEXT_ENCODING_UTF8);
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFS_TEXT_ENCODING, textEncoding);
        editor.commit();
        refreshParameterSalt();
        copyPasswordsToClipboard = prefs.getBoolean(PREFS_COPY_TO_CLIPBOARD, true);
        showMasterPassword = prefs.getBoolean(PREFS_SHOW_MASTER_PASSWD, false);
        clearPasswordsOnFocusLoss = prefs.getBoolean(PREFS_CLEAR_PASSWDS_ON_FOCUS_LOSS, false);
        try {
            String[] hashes = getResources().getStringArray(R.array.hashList);
            hashLengths = new Hashtable<String, Integer>(hashes.length);
            MessageDigest internalHasher = null;
            Digest bcHasher = null;
            int byteLength = 0;
            int b64Length = 0;
            for (int i = 0; i < hashes.length; i++) {
                if (hashes[i].compareTo("MD5") == 0 || hashes[i].compareTo("SHA-1") == 0 || hashes[i].compareTo("SHA-256") == 0 || hashes[i].compareTo("SHA-384") == 0 || hashes[i].compareTo("SHA-512") == 0) {
                    internalHasher = MessageDigest.getInstance(hashes[i]);
                    byteLength = internalHasher.getDigestLength();
                } else if (hashes[i].compareTo("RIPEMD-160") == 0) {
                    bcHasher = new RIPEMD160Digest();
                    byteLength = bcHasher.getDigestSize();
                } else if (hashes[i].compareTo("Tiger") == 0) {
                    bcHasher = new TigerDigest();
                    byteLength = bcHasher.getDigestSize();
                } else if (hashes[i].compareTo("Whirlpool") == 0) {
                    bcHasher = new WhirlpoolDigest();
                    byteLength = bcHasher.getDigestSize();
                }
                b64Length = (byteLength + 2 - ((byteLength + 2) % 3)) / 3 * 4;
                hashLengths.put(hashes[i], Integer.valueOf(b64Length));
            }
        } catch (Exception e1) {
            hashLengths = null;
        }
    }

    @Override
    public void onLowMemory() {
        siteList = null;
        siteListHash = null;
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        siteList = null;
        siteListHash = null;
        super.onTerminate();
    }

    /**
	 * Regenerate the encryption salt for site parameter data.
	 */
    public void refreshParameterSalt() {
        String uniqueID = null;
        try {
            AndroidID id = AndroidID.newInstance(this);
            uniqueID = id.getAndroidID();
        } catch (Exception e1) {
        }
        if (uniqueID == null) uniqueID = SALTIER_SALT; else uniqueID = uniqueID.concat(SALTIER_SALT);
        try {
            PARAMETER_SALT = uniqueID.getBytes(textEncoding);
        } catch (Exception e) {
            PARAMETER_SALT = uniqueID.getBytes();
        }
        try {
            MessageDigest hasher = MessageDigest.getInstance(SALT_HASH);
            for (int i = 0; i < SALT_ITERATION_COUNT; i++) PARAMETER_SALT = hasher.digest(PARAMETER_SALT);
        } catch (Exception e) {
        }
    }

    /**
	 * Request the current site list.  The site will be returned to the
	 * SiteListListener specified by call its onSiteListReady() method.
	 * @param caller The Activity requesting the site list.  This may be the
	 * same as the SiteListListener, but it doesn't have to be.
	 * @param listener The SiteListListener that will receive the list when
	 * it's ready.  This may be the same as the calling Activity, but it
	 * doesn't have to be.
	 */
    public void requestSiteList(Activity caller, SiteListListener listener) {
        if (siteList != null) listener.onSiteListReady(siteList); else {
            this.caller = caller;
            this.listener = listener;
            caller.showDialog(DIALOG_PROGRESS);
        }
    }

    /**
	 * Checks to see if the site list currently "dirty" and needs to be
	 * rebuilt 
	 * @return True of the list is "dirty", false otherwise
	 */
    public boolean isSiteListDirty() {
        return siteList == null;
    }

    /**
	 * Set the site list as "dirty" and in need of being rebuilt.  This
	 * should be called by any Activity that may modify the database, which
	 * subsequently requires the site list to be refreshed.  Note that an
	 * activity cannot declare the list to be "clean"; only the application
	 * itself can do that.
	 */
    public void setSiteListDirty() {
        siteList = null;
        siteListHash = null;
    }

    /**
	 * Check to see if a specific site token is currently in the site list.  Note
	 * that if the site list has not been built by calling requestSiteList(), this
	 * method will artificially return false.
	 * @param siteName The site token to search for
	 * @return True if the site is in the list, false if it isn't or if the site
	 * list has not been built yet.
	 */
    public boolean siteListContainsSite(String siteName) {
        if (siteList != null && siteListHash != null && siteListHash.containsKey(siteName)) return true; else return false;
    }

    /**
	 * Get the common ParamsDbAdapter object for the entire application.  All
	 * activities should call this to get access to the database rather than
	 * opening their own independent connections.
	 * @return A ParamsDbAdapter to access the Cryptnos database
	 */
    public ParamsDbAdapter getDBHelper() {
        return DBHelper;
    }

    /**
	 * Get the application's SharedPreferences object.  If any editing occurs
	 * while this object is in your possession, make sure to commit your
	 * changes!
	 * @return The application's SharedPreferences object
	 */
    public SharedPreferences getPrefs() {
        return prefs;
    }

    /**
	 * Get the application's FileManager object.
	 * @return The application's FileManager object
	 */
    public FileManager getFileManager() {
        if (fileManager == null) fileManager = new FileManager(this, prefs.getInt(PREFS_FILE_MANAGER, FileManager.APP_NO_FILE_MANAGER));
        return fileManager;
    }

    /**
	 * Get the application's QRCodeHandler object.
	 * @return The application's QRCodeHandler object.
	 */
    public QRCodeHandler getQRCodeHandler() {
        if (qrCodeHandler == null) qrCodeHandler = new QRCodeHandler(this, prefs.getInt(PREFS_QRCODE_SCANNER, QRCodeHandler.APP_NONE_SELECTED));
        return qrCodeHandler;
    }

    /**
	 * Get the user's preferred text encoding (or the default if no
	 * preference has been set).  Use this for all String.getBytes()
	 * operations.
	 * @return
	 */
    public String getTextEncoding() {
        return textEncoding;
    }

    /**
	 * Set the user's preferred text encoding, which will be used for all
	 * String.getBytes() operations.  This value will automatically be
	 * written to the shared preferences if successful.
	 * @param encoding The text encoding ID string of the user's preferred
	 * encoding
	 * @throws UnsupportedEncodingException Thrown if the specified encoding
	 * is not supported
	 */
    public void setTextEncoding(String encoding) throws UnsupportedEncodingException {
        "test me".getBytes(encoding);
        textEncoding = encoding;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFS_TEXT_ENCODING, textEncoding);
        editor.commit();
    }

    /**
	 * Get a File representing the root of all import/export activities.
	 * Files will only be written or read from this path.
	 * @return A File representing the import/export root path.
	 */
    public File getImportExportRootPath() {
        return importExportRoot;
    }

    /**
	 * Check the external storage mechanism to make sure we can write data
	 * to it
	 * @return True if we can write to external storage, false otherwise
	 */
    public boolean canWriteToExternalStorage() {
        return Environment.getExternalStorageState().compareTo(Environment.MEDIA_MOUNTED) == 0;
    }

    /**
	 * Check the external storage mechanism to make sure we can read data
	 * from it
	 * @return True if we can read from external storage, false otherwise
	 */
    public boolean canReadFromExternalStorage() {
        return (Environment.getExternalStorageState().compareTo(Environment.MEDIA_MOUNTED) == 0) || (Environment.getExternalStorageState().compareTo(Environment.MEDIA_MOUNTED_READ_ONLY) == 0);
    }

    /**
	 * Check to see if the UpgradeManager has run for this session
	 * @return True if the UpgradeManager has run, false otherwise
	 */
    public boolean hasUpgradeManagerRun() {
        return upgradeManagerRan;
    }

    /**
	 * Run the UpgradeManager
	 * @param caller The calling Activity
	 */
    public void runUpgradeManager(Activity caller) {
        if (!upgradeManagerRan) {
            this.caller = caller;
            upgradeManagerRan = true;
            UpgradeManager um = new UpgradeManager(this, caller);
            um.performUpgradeCheck();
        }
    }

    /**
	 * Determine whether or not to show the Advanced Settings warning dialog.
	 * @return True if the dialog should be shown, false otherwise
	 */
    public boolean showAdvancedSettingsWarning() {
        return showAdvancedSettingsWarning;
    }

    /**
	 * Toggle the setting to show the Advanced Settings warning dialog.  Calling
	 * this method always turns this flag off, so showAdvancedSettingsWarning()
	 * will always return false after this.
	 */
    public void toggleShowAdvancedSettingsWarning() {
        showAdvancedSettingsWarning = false;
    }

    /**
	 * Get the Base64-encoded length of the specified cryptographic hash
	 * @param hash The name string of the hash algorithm
	 * @return The length of the hash digest Base64-encoded, or -1 if an error
	 * occurs.
	 */
    public int getEncodedHashLength(String hash) {
        if (hashLengths != null) {
            Integer i = hashLengths.get(hash);
            if (i != null) return i.intValue(); else return -1;
        } else return -1;
    }

    /**
	 * Determine whether or not we should copy generated passwords to the system
	 * clipboard.
	 * @return True if we should copy passwords, false otherwise
	 */
    public boolean copyPasswordsToClipboard() {
        return copyPasswordsToClipboard;
    }

    /**
	 * Toggle the "copy passwords to clipboard" setting and save the new value
	 * to the application preferences.
	 */
    public void toggleCopyPasswordsToClipboard() {
        copyPasswordsToClipboard = !copyPasswordsToClipboard;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFS_COPY_TO_CLIPBOARD, copyPasswordsToClipboard);
        editor.commit();
    }

    /**
	 * Determine whether or not to show master passwords while generating
	 * passwords.
	 * @return True if we should show the master password, false otherwise
	 */
    public boolean showMasterPasswords() {
        return showMasterPassword;
    }

    /**
	 * Toggle the "show master passwords" setting and save the new value
	 * to the application preferences.
	 */
    public void toggleShowMasterPasswords() {
        showMasterPassword = !showMasterPassword;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFS_SHOW_MASTER_PASSWD, showMasterPassword);
        editor.commit();
    }

    /**
	 * Determine whether or not the master and generated password boxes should
	 * be cleared whenever Cryptnos goes into the background (that is, it loses
	 * focus and is no longer the visible activity).
	 * @return True if the passwords should be cleared, false otherwise
	 */
    public boolean clearPasswordsOnFocusLoss() {
        return clearPasswordsOnFocusLoss;
    }

    /**
	 * Toggle the "clear passwords on focus loss" setting and save the new
	 * value to the application preferences.
	 */
    public void toggleClearPasswordsOnFocusLoss() {
        clearPasswordsOnFocusLoss = !clearPasswordsOnFocusLoss;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFS_CLEAR_PASSWDS_ON_FOCUS_LOSS, clearPasswordsOnFocusLoss);
        editor.commit();
    }

    /**
	 * Create and return a dialog box.  Note that Android Application classes
	 * do not ordinarily control or own individual dialogs; any dialog created
	 * by this method actually becomes the property of the Activity set by
	 * requestSiteList().  This method should be called by the Activity's
	 * own onCreateDialog() method, which should look for
	 * CryptnosApplication.DIALOG_PROGRESS as a potential dialog ID.
	 * @param id A constant specifying which dialog to create
	 * @return The Dialog
	 */
    public Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch(id) {
            case DIALOG_PROGRESS:
                progressDialog = new ProgressDialog(caller);
                progressDialog.setOwnerActivity(caller);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(100);
                progressDialog.setMessage(getResources().getString(R.string.sitelist_loading_message));
                listBuilderThread = new ListBuilderThread(this, handler);
                listBuilderThread.start();
                dialog = progressDialog;
                break;
            case DIALOG_UPGRADE_TO_UTF8:
                AlertDialog.Builder adb = new AlertDialog.Builder(caller);
                adb.setTitle(getResources().getString(R.string.mainmenu_dialog_advanced_settings_title));
                String message = getResources().getString(R.string.error_upgrader_change_encoding_warning);
                message = message.replace(getResources().getString(R.string.meta_replace_token), System.getProperty("file.encoding", "No Default"));
                adb.setMessage(message);
                adb.setCancelable(true);
                adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Intent i1 = new Intent(caller, AdvancedSettingsActivity.class);
                        startActivity(i1);
                    }
                });
                adb.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                adb.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    public void onCancel(DialogInterface dialog) {
                        try {
                            setTextEncoding(System.getProperty("file.encoding", TEXT_ENCODING_UTF8));
                            refreshParameterSalt();
                        } catch (Exception ex) {
                        }
                        caller.removeDialog(DIALOG_UPGRADE_TO_UTF8);
                    }
                });
                dialog = (Dialog) adb.create();
                break;
        }
        return dialog;
    }

    /** Define the Handler that receives messages from the list builder
     *  thread and updates the progress */
    final Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            int total = msg.getData().getInt("percent_done");
            if (total > 0) progressDialog.setProgress(total);
            if (total >= 100) {
                caller.removeDialog(DIALOG_PROGRESS);
                listBuilderThread.setState(ListBuilderThread.STATE_DONE);
                listener.onSiteListReady(siteList);
            } else if (total < 0) {
                listBuilderThread.setState(ListBuilderThread.STATE_DONE);
                siteList = null;
                siteListHash = null;
                Toast.makeText(caller, R.string.error_bad_listfetch, Toast.LENGTH_LONG).show();
            }
        }
    };

    /** This private Thread-based class builds the site list in a separate
     *  thread of execution to improved the responsiveness and perceived
     *  performance of the application.  This does the heavy lifting of
     *  reading parameters from the database, decrypting them, and building
     *  the site list array for this activity to display. */
    private class ListBuilderThread extends Thread {

        Handler mHandler;

        CryptnosApplication theApp;

        static final int STATE_DONE = 0;

        static final int STATE_RUNNING = 1;

        int mState;

        int mSiteCount = 0;

        int mCounter = 0;

        /**
         * The ListBuilderThread constructor
         * @param caller The calling Activity
         * @param handler The Handler that will catch our messages
         */
        ListBuilderThread(CryptnosApplication theApp, Handler handler) {
            this.theApp = theApp;
            mHandler = handler;
        }

        @Override
        public void run() {
            mState = STATE_RUNNING;
            Message msg = null;
            Bundle b = null;
            Cursor cursor = null;
            try {
                cursor = DBHelper.fetchAllSites();
                cursor.moveToFirst();
                mSiteCount = cursor.getCount();
                siteListHash = new Hashtable<String, String>();
                while (!cursor.isAfterLast() && mState == STATE_RUNNING) {
                    try {
                        SiteParameters params = new SiteParameters(theApp, cursor.getString(1), cursor.getString(2));
                        siteListHash.put(params.getSite(), HASH_NULL);
                    } catch (Exception e) {
                    }
                    msg = mHandler.obtainMessage();
                    b = new Bundle();
                    b.putInt("percent_done", (int) (Math.floor(((double) mCounter / (double) mSiteCount * 95.0d))));
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                    mCounter++;
                    cursor.moveToNext();
                }
                cursor.close();
                Set<String> siteSet = siteListHash.keySet();
                siteList = new String[siteSet.size()];
                siteSet.toArray(siteList);
                java.util.Arrays.sort(siteList, String.CASE_INSENSITIVE_ORDER);
                siteSet = null;
                msg = mHandler.obtainMessage();
                b = new Bundle();
                b.putInt("percent_done", 100);
                msg.setData(b);
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }
                msg = mHandler.obtainMessage();
                b = new Bundle();
                b.putInt("percent_done", -1);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        }

        /** Set the state of the thread to the given value. */
        public void setState(int state) {
            mState = state;
        }
    }

    /**
     * Check to see if the specified intent is available
     * @param context A Context
     * @param action A string specifying the intent to search for
     * @return True if the intent is available, false otherwise
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
}
