package org.pachyderm.authoring;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import org.pachyderm.apollo.core.CXDefaults;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSPathUtilities;

public class PachyUtilities {

    @SuppressWarnings("unchecked")
    public static NSArray<String> supportedImageExtensions = (NSArray<String>) CXDefaults.sharedDefaults().getObject("SupportedImageExtensions");

    @SuppressWarnings("unchecked")
    public static NSArray<String> supportedAudioExtensions = (NSArray<String>) CXDefaults.sharedDefaults().getObject("SupportedAudioExtensions");

    @SuppressWarnings("unchecked")
    public static NSArray<String> SupportedVideoExtensions = (NSArray<String>) CXDefaults.sharedDefaults().getObject("SupportedVideoExtensions");

    @SuppressWarnings("unchecked")
    public static NSArray<String> supportedOtherExtensions = (NSArray<String>) CXDefaults.sharedDefaults().getObject("SupportedOtherExtensions");

    public static Boolean isSupportedExtension(String extension) {
        for (String supportedExtension : supportedImageExtensions) {
            if (supportedExtension.equalsIgnoreCase(extension)) return true;
        }
        for (String supportedExtension : supportedAudioExtensions) {
            if (supportedExtension.equalsIgnoreCase(extension)) return true;
        }
        for (String supportedExtension : SupportedVideoExtensions) {
            if (supportedExtension.equalsIgnoreCase(extension)) return true;
        }
        return false;
    }

    public static Boolean isOtherExtension(String extension) {
        for (String supportedExtension : supportedOtherExtensions) {
            if (supportedExtension.equalsIgnoreCase(extension)) return true;
        }
        return false;
    }

    public static String uniqueFilePath(String filePath) {
        Random random = new Random();
        String pathBaseName = NSPathUtilities.stringByDeletingPathExtension(filePath);
        String pathXtension = NSPathUtilities.pathExtension(filePath);
        if (pathXtension.length() > 0) {
            filePath = NSPathUtilities.stringByAppendingPathExtension(pathBaseName + "-0000", pathXtension);
            File randomFile = new File(filePath);
            if (randomFile.exists()) {
                filePath = NSPathUtilities.stringByAppendingPathExtension(pathBaseName + "-" + (Integer.toHexString(random.nextInt()) + "0000").substring(0, 4), pathXtension);
                randomFile = new File(filePath);
            }
        } else {
            filePath = pathBaseName + "-0000";
            File randomFile = new File(filePath);
            if (randomFile.exists()) {
                filePath = pathBaseName + "-" + (Integer.toHexString(random.nextInt()) + "0000").substring(0, 4);
                randomFile = new File(filePath);
            }
        }
        return filePath;
    }

    private static Integer levelOne, levelTwo;

    public static void reset() {
        levelOne = levelTwo = 1;
    }

    public static void bumpOne() {
        levelOne += 1;
    }

    public static void bumpTwo() {
        levelTwo += 1;
    }

    public static String show() {
        return "";
    }

    @SuppressWarnings("unchecked")
    public static String getFirstAuthenticationRealm() {
        NSArray<String> authenticatorArray = (NSArray<String>) CXDefaults.sharedDefaults().getObject("AuthenticationRealms");
        return authenticatorArray.objectAtIndex(0);
    }

    private static MessageDigest _md;

    public static void passwordDigestInit() {
        try {
            _md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new NSForwardException(nsae, "passwordDigestInit: Could not obtain MessageDigest instance with MD5 algorithm.");
        }
    }

    public static NSData passwordDigest(String password) {
        _md.reset();
        try {
            _md.update(password.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            throw new NSForwardException(uee, "passwordDigest: Password is indigestable.");
        }
        return new NSData(_md.digest());
    }

    public static boolean isValidEmailAddress(String aEmailAddress) {
        return (aEmailAddress == null) ? false : java.util.regex.Pattern.matches("^[^@ ]+@[^@ ]+\\.[^@ \\.]+$", aEmailAddress);
    }

    private static StringBuffer webImages = new StringBuffer("images").append(File.separator);

    private static StringBuffer webScreenLegends = new StringBuffer(webImages).append("screen-legends").append(File.separator);

    private static StringBuffer webScreenThumbnails = new StringBuffer(webImages).append("screen-thumbnails").append(File.separator);

    public static String webRezImages(String resource) {
        return webImages.toString() + resource;
    }

    public static String webScreenLegend(String resource) {
        return webScreenLegends.toString() + resource;
    }

    public static String webScreenThumb(String resource) {
        return webScreenThumbnails.toString() + resource;
    }
}
