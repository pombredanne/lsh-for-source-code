package com.csaba.connector.axa;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import com.csaba.connector.DummyServer;
import com.csaba.connector.I18NServiceException;
import com.csaba.connector.ServiceException;
import com.csaba.connector.axa.model.AXABank;
import com.csaba.connector.axa.model.AXASession;
import com.csaba.connector.model.Account;
import com.csaba.connector.model.Session;
import com.csaba.connector.service.AbstractBankService;
import com.csaba.util.Base64Coder;

public class AXAAccountLoginService extends AbstractBankService implements Codes {

    public static final int TIMESTAMP_WINDOW = 10;

    public static final int SALT_LENGTH = 8;

    public static final String SALTED_PREFIX = "Salted__";

    public static final int PREFIX_LENGTH = SALTED_PREFIX.length();

    private static final String POSITIVE_RESULT = "\"OK\"";

    private char[] pinDigits;

    public void setPinDigits(final char[] pinDigits) {
        this.pinDigits = pinDigits;
    }

    @Override
    public void execute(final Session session) throws ServiceException {
        if (!(session instanceof AXASession)) throw new ServiceException("Incompatible session " + AXABank.class + " != " + session.getClass().getName());
        if (pinDigits == null) throw new ServiceException("Service is not yet inicialized.");
        if (!session.isRemotePropertySet(RP_SELECTED_ACCOUNT)) {
            throw new I18NServiceException(AXABank.getInstance(), "err.noAccountSelected");
        }
        final AXASession axa = (AXASession) session;
        final String account = ((Account) session.getRemoteProperty(RP_SELECTED_ACCOUNT)).getNumber();
        String loginData = null;
        try {
            final Calendar cal = Calendar.getInstance();
            loginData = encode(cal, pinDigits, account);
        } catch (final Exception e) {
            throw new I18NServiceException(AXABank.getInstance(), "err.encryptionFailed", e);
        }
        final NameValuePair[] accountLoginRequest = new NameValuePair[] { new BasicNameValuePair("LoginData", loginData), new BasicNameValuePair("AccountNumber", "") };
        final String selectAccountResult = axa.doPost(AXASession.getRequestURL(REQUEST_ACCOUNT_LOGIN), accountLoginRequest);
        axa.debugFile("accountLoginResult", selectAccountResult);
        if (!selectAccountResult.equals(POSITIVE_RESULT)) {
            throw AXAUtil.serverError(selectAccountResult);
        }
        DummyServer.getInstance().addSession(session);
    }

    public static String encode(final Calendar timestamp, final char[] pins, final String accountNumber) throws GeneralSecurityException, UnsupportedEncodingException {
        final String pinMask = getPINMask(pins);
        final String plainText = "{ \"AccountNumber\" : \"" + accountNumber + "\", \"PINMask\" : \"" + pinMask + "\" }\n";
        final byte[] randomSalt = new byte[SALT_LENGTH];
        (new Random()).nextBytes(randomSalt);
        final byte[][] passwords = getPassword(timestamp, pinMask, randomSalt);
        final SecretKeySpec skeySpec = new SecretKeySpec(passwords[0], "AES");
        final IvParameterSpec ivSpec = new IvParameterSpec(passwords[1]);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
        final byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF8"));
        final byte[] result = new byte[PREFIX_LENGTH + SALT_LENGTH + encrypted.length];
        System.arraycopy(SALTED_PREFIX.getBytes("8859_1"), 0, result, 0, PREFIX_LENGTH);
        System.arraycopy(randomSalt, 0, result, PREFIX_LENGTH, SALT_LENGTH);
        System.arraycopy(encrypted, 0, result, PREFIX_LENGTH + SALT_LENGTH, encrypted.length);
        return new String(Base64Coder.encode(result));
    }

    private static String getPINMask(final char[] pins) {
        final StringBuilder sPINMask = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (pins[i] == '*') sPINMask.append(i + 1); else sPINMask.append(pins[i]);
        }
        return sPINMask.toString();
    }

    private static byte[][] getPassword(final Calendar timestamp, final String pinMask, final byte[] salt) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        final long lTimestamp = getTimeStamp(timestamp, 10);
        final String password = pinMask + String.valueOf(lTimestamp);
        final byte[] fullPassword = concatArrays(password.getBytes("8859_1"), salt);
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update(fullPassword);
        final byte[] digest1 = md5.digest();
        md5.reset();
        md5.update(concatArrays(digest1, fullPassword));
        final byte[] digest2 = md5.digest();
        md5.reset();
        md5.update(concatArrays(digest2, fullPassword));
        final byte[] digest3 = md5.digest();
        final byte[][] result = new byte[2][];
        result[0] = concatArrays(digest1, digest2);
        result[1] = digest3;
        return result;
    }

    private static byte[] concatArrays(final byte[] left, final byte[] right) {
        final byte[] result = new byte[left.length + right.length];
        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }

    public static long getTimeStamp(final Calendar cal, final int window) {
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        final long day = cal.get(Calendar.DATE);
        final long hour = cal.get(Calendar.HOUR_OF_DAY);
        final long minutes = cal.get(Calendar.MINUTE);
        long temp = (day * 1440) + (hour * 60) + (minutes);
        final long mod = temp % window;
        if (mod < Math.floor(window / 2)) {
            temp -= mod;
        } else {
            temp += (window - mod);
        }
        return temp;
    }

    public static void main(final String[] args) throws Exception {
        final String accountNumber = args[0];
        final char[] pins = args[1].toCharArray();
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(args[2]));
        final String encoded = encode(timestamp, pins, accountNumber);
        System.out.println(URLEncoder.encode(encoded, "UTF-8"));
        System.out.println(getTimeStamp(timestamp, TIMESTAMP_WINDOW) + "?=" + getTimeStamp(Calendar.getInstance(), TIMESTAMP_WINDOW));
    }
}
