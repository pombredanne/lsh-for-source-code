package com.crypticbit.ipa.licence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import com.crypticbit.ipa.central.LogFactory;
import com.crypticbit.ipa.entity.status.Info;

public class WebValidator implements LicenceValidator {

    private static final String BASE_URL = "http://www.crypticbit.com/reg/device.php";

    public static String convertToSha1(final String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    private static String convertToHex(final byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (byte element : data) {
            int halfbyte = (element >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = element & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    private ValidatorUi ui;

    public WebValidator(final ValidatorUi ui) {
        this.ui = ui;
    }

    @Override
    public void checkLicence(final Info info) throws NotLicencedException {
        try {
            URL onlineValidator;
            String serialNumber = info.getSerialNumber();
            if (serialNumber == null) {
                throw new NotLicencedException("Unable to determin device serial number for this backup");
            }
            String customerNumber = this.ui.getCustomerNumber();
            if (customerNumber == null) {
                throw new NotLicencedException("No Licence Key Provided");
            }
            try {
                onlineValidator = new URL(BASE_URL + "?" + encodeParams(serialNumber, "1", customerNumber));
            } catch (NoSuchAlgorithmException e) {
                throw new NotLicencedException("The key to anonymously validate your iPhone details could not be generated.");
            }
            HttpURLConnection connection = (HttpURLConnection) onlineValidator.openConnection();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new NotLicencedException("We were unable to connect the licencing server to validate your key. Please ensure you are connected to the internet.");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            boolean licenceAccepted = false;
            while (((inputLine = in.readLine()) != null) && !licenceAccepted) {
                if (inputLine.equals("EXISTS")) {
                    System.out.println("Server response: exists (already registered)");
                    licenceAccepted = true;
                } else if (inputLine.equals("NEW")) {
                    LogFactory.getLogger().log(Level.INFO, "Server response: new (now registered)");
                    licenceAccepted = true;
                } else if (inputLine.equals("FULL")) throw new NotLicencedException("You have used all available entitlements associated with this Customer Number. Entitlements to view additional devices can be purchased from http://www.crypticbit.com"); else {
                    LogFactory.getLogger().log(Level.INFO, "Unexpected Server response: " + inputLine);
                }
            }
            if (!licenceAccepted) throw new NotLicencedException("The licence server rejected your application to use this iphone");
            in.close();
        } catch (MalformedURLException e) {
            throw new NotLicencedException("Can't validate licence because of a connectivity problem", e);
        } catch (IOException e) {
            throw new NotLicencedException("Can't validate licence because of a connectivity problem", e);
        }
    }

    private String encodeParam(final String key, final String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
    }

    private String encodeParams(final String id, final String version, final String licenseKey) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return encodeParam("id", convertToSha1(id)) + "&" + encodeParam("v", version) + "&" + encodeParam("c", licenseKey);
    }
}
