package org.openremote.android.console;

import org.openremote.android.console.model.UserCache;
import android.net.http.AndroidHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import android.util.Base64;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.FileReader;
import java.io.BufferedReader;
import android.content.Intent;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;

public class ServerlessConfigurator {

    private String username, password;

    private final String beehiveRoot = "http://beehive.openremote.org/3.0/alpha5/rest/user/";

    private Activity activity;

    public ServerlessConfigurator(final Activity activity) {
        this.activity = activity;
    }

    public void configure() {
        username = UserCache.getUsername(activity);
        if (username == "") {
            getCredentials();
            return;
        }
        new Thread(new Runnable() {

            public void run() {
                downloadOpenremoteZipFromBeehiveAndUnzip();
            }
        }).start();
    }

    private void downloadOpenremoteZipFromBeehiveAndUnzip() {
        AndroidHttpClient httpClient = AndroidHttpClient.newInstance("OpenRemote");
        username = UserCache.getUsername(activity);
        password = UserCache.getPassword(activity);
        HttpGet httpGet = new HttpGet(beehiveRoot + username + "/openremote.zip");
        httpGet.setHeader("Authorization", "Basic " + encode(username, password));
        InputStream inputStream = null;
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (200 == response.getStatusLine().getStatusCode()) {
                Log.i("ServerlessConfigurator", httpGet.getURI() + " is available.");
                inputStream = response.getEntity().getContent();
                writeZipAndUnzip(inputStream);
            } else if (400 == response.getStatusLine().getStatusCode()) {
                Log.e("Serverless Configurator", "Not found", new Exception("400 Malformed"));
            } else if (401 == response.getStatusLine().getStatusCode() || 404 == response.getStatusLine().getStatusCode()) {
                Log.e("Serverless Configurator", "Not found", new Exception("401 Not authorized"));
                getCredentials();
            } else {
                Log.e("Serverless Configurator", "failed to download resources for template, The status code is: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            Log.e("SeverlessConfigurator", "failed to connect to Beehive.");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("ServerlessConfigurator", "failed to close input stream while downloading " + httpGet.getURI());
                }
            }
            httpClient.close();
        }
        Log.i("ServerlessConfigurator", "Done downloading and unzipping files from OR.org");
    }

    private String encode(String username, String password) {
        try {
            String mergedCredentials = password + "{" + username + "}";
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(mergedCredentials.getBytes());
            BigInteger number = new BigInteger(1, digest);
            String md5 = number.toString(16);
            while (md5.length() < 32) md5 = "0" + md5;
            return Base64.encodeToString((username + ":" + md5).getBytes(), Base64.DEFAULT).trim();
        } catch (Exception e) {
            Log.e("ServerConfigurator", "couldn't encode with MD5", e);
            return null;
        }
    }

    private void writeZipAndUnzip(InputStream inputStream) {
        FileOutputStream fos = null;
        FileOutputStream fos2 = null;
        ZipInputStream zis = null;
        try {
            fos = activity.openFileOutput("openremote.zip", activity.MODE_WORLD_READABLE);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            Log.i("OpenRemote/DOWNLOAD", "Done downloading file from internet");
            zis = new ZipInputStream(activity.openFileInput("openremote.zip"));
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                Log.i("OpenRemote/DOWNLOAD", "new File in ZIP" + zipEntry.getName());
                fos2 = activity.openFileOutput(zipEntry.getName(), activity.MODE_WORLD_READABLE);
                byte[] buffer2 = new byte[1024];
                int len2 = 0;
                while ((len2 = zis.read(buffer2)) != -1) {
                    fos2.write(buffer2, 0, len2);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                activity.deleteFile("openremote.zip");
                zis.closeEntry();
                fos.close();
                fos2.close();
            } catch (IOException e) {
                Log.e("ServerConfigurator", "Error while closing stream.", e);
            }
        }
    }

    private void getCredentials() {
        Intent i = new Intent();
        i.setClassName(activity.getClass().getPackage().getName(), LoginViewActivity.class.getName());
        activity.startActivityForResult(i, Constants.REQUEST_CODE_LOGIN_VIEW);
    }
}
