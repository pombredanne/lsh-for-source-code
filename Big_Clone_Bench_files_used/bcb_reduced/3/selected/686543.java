package cn.sduo.app.util;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class SecurityUtils {

    private static final Log logger = LogFactory.getLog(SecurityUtils.class);

    private static final String DEFAULT_ALGORITHM = "MD5";

    public static UserDetails getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getPrincipal() != null) {
                final UserDetails details = (UserDetails) authentication.getPrincipal();
                if (logger.isDebugEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("details.Authorities=" + details.getAuthorities());
                    logger.debug(sb.toString());
                }
                return details;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String getUserID() {
        return getUser().getUsername();
    }

    public static String createPasswordHash(String password) {
        Md5PasswordEncoder md5 = new Md5PasswordEncoder();
        md5.setEncodeHashAsBase64(false);
        String pwd = md5.encodePassword(password, null);
        return pwd;
    }

    public static String createPasswordHash(String password, byte[] salt) {
        String passHash = null;
        try {
            byte[] passBytes = password.getBytes();
            MessageDigest md = MessageDigest.getInstance(DEFAULT_ALGORITHM);
            if (salt != null) {
                md.update(salt);
            }
            byte[] hashBytes = md.digest(passBytes);
            passHash = bytesToBase64(hashBytes);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Password hash calculation failed ", e);
            }
        }
        return passHash;
    }

    public static String bytesToBase64(byte[] data) {
        return Base64.encodeBase64String(data);
    }

    public static boolean isBase64StrEquals(String str1, String str2) {
        byte[] bytes1 = Base64.decodeBase64(str1);
        byte[] bytes2 = Base64.decodeBase64(str2);
        return Arrays.equals(bytes1, bytes2);
    }

    public static Locale getCurrentLocale() {
        Locale useLocale = LocaleContextHolder.getLocale();
        return useLocale;
    }

    public static String getCurrentLocaleString() {
        if (getCurrentLocale() != null) {
            return getCurrentLocale().toString();
        } else {
            return getDefaultLocale().toString();
        }
    }

    public static Locale getDefaultLocale() {
        Locale useLocale = Locale.getDefault();
        return useLocale;
    }

    public static final void main(String[] args) {
        System.out.println("createPasswordHash(admin) = " + createPasswordHash("admin"));
    }
}
