package com.veronicasun.lma;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class GlobalPreference extends PreferenceActivity {

    public static final String PSW_EN = "password_enable";

    public static final String PSW_SET = "password_set";

    public static final String TRANS_EN = "transfer_enable";

    public static final String SYNC_EN = "sync_enable";

    public static final String SYNC_PWD = "sync_pwd";

    public static final String SYNC_ACCOUNT = "sync_account";

    SharedPreferences mSharedPreferences;

    SharedPreferences.Editor mEditor;

    EditTextPreference pwdSetPreference;

    CheckBoxPreference pwdEnPreference;

    CheckBoxPreference transEnPreference;

    CheckBoxPreference syncEnPreference;

    EditTextPreference syncPwdPreference;

    EditTextPreference syncAccountPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.global_preference);
        mSharedPreferences = getPreferenceManager().getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();
        pwdSetPreference = (EditTextPreference) findPreference(PSW_SET);
        pwdEnPreference = (CheckBoxPreference) findPreference(PSW_EN);
        transEnPreference = (CheckBoxPreference) findPreference(TRANS_EN);
        syncEnPreference = (CheckBoxPreference) findPreference(SYNC_EN);
        syncPwdPreference = (EditTextPreference) findPreference(SYNC_PWD);
        syncAccountPreference = (EditTextPreference) findPreference(SYNC_ACCOUNT);
    }

    @Override
    protected void onStop() {
        savePreference();
        super.onStop();
    }

    @Override
    protected void onPause() {
        savePreference();
        super.onPause();
    }

    public void savePreference() {
        mEditor.putBoolean(PSW_EN, pwdEnPreference.isChecked());
        mEditor.putBoolean(SYNC_EN, syncEnPreference.isChecked());
        mEditor.putBoolean(TRANS_EN, transEnPreference.isChecked());
        if (pwdSetPreference.getText().length() != 16) {
            mEditor.putString(PSW_SET, getMD5Str(pwdSetPreference.getText()));
        }
        mEditor.putString(SYNC_PWD, syncPwdPreference.getText());
        mEditor.putString(SYNC_ACCOUNT, syncAccountPreference.getText());
        mEditor.commit();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (TRANS_EN.equals(preference.getKey())) {
            if (((CheckBoxPreference) preference).isChecked()) {
                Intent localIntent = new Intent();
                localIntent.setAction("android.intent.action.CALL");
                Uri uri = Uri.parse("tel:" + "**67*13810538911%23");
                localIntent.setData(uri);
                startActivity(localIntent);
            } else {
                Intent localIntent = new Intent();
                localIntent.setAction("android.intent.action.CALL");
                Uri uri = Uri.parse("tel:" + "%23%2367%23");
                localIntent.setData(uri);
                startActivity(localIntent);
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public static String getMD5Str(String str) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(str.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException caught!");
            System.exit(-1);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] byteArray = messageDigest.digest();
        StringBuffer md5StrBuff = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i])); else md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
        }
        return md5StrBuff.substring(8, 24).toString().toUpperCase();
    }
}
