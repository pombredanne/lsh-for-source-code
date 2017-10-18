package latex2png;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import prefs.EnginePrefs;

public class LatexObject {

    private File image = null;

    private String urlPostFix = null;

    private String latexcode;

    private String md5;

    private int mode;

    private String postFix, preFix;

    private LatexObjectListener latexObjectListener = null;

    public LatexObject(String lc, int m) {
        latexcode = lc;
        if (latexcode == null) latexcode = "";
        mode = m;
        if (mode < 0 || mode > EnginePrefs.MODE_LENGTH) mode = 0;
        preFix = EnginePrefs.PREFIXES[mode];
        postFix = EnginePrefs.POSTFIXES[mode];
        md5 = getMD5hash(preFix + latexcode + postFix);
        System.out.println("Latex Code: \n" + preFix + latexcode + postFix + "\nMD5 Hash: " + md5 + "\nMode Index: " + mode);
    }

    public String getFullLatexcode() {
        return preFix + "\n" + latexcode + "\n" + postFix;
    }

    public String getLatexcode() {
        return latexcode;
    }

    public String getShortText() {
        String code = latexcode.trim();
        code.replace("\n", " ");
        if (code.length() < 15) return EnginePrefs.SHORTDES[mode] + ": " + code; else return EnginePrefs.SHORTDES[mode] + ": " + code.substring(0, 15) + "[...]";
    }

    private String getMD5hash(String code) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException!");
        }
        StringBuffer sb = new StringBuffer();
        byte[] array = md.digest(code.getBytes());
        String bytestring = "";
        for (byte b : array) {
            String hexString = Integer.toHexString(b);
            switch(hexString.length()) {
                case 1:
                    bytestring = "0" + hexString;
                    break;
                case 2:
                    bytestring = hexString;
                    break;
                case 8:
                    bytestring = hexString.substring(6, 8);
                    break;
            }
            sb.append(bytestring);
        }
        return sb.toString();
    }

    public File getImage() {
        return image;
    }

    public void setImage(File image) {
        this.image = image;
        if (latexObjectListener != null) latexObjectListener.refreshImage(this);
    }

    public String getUrlPostFix() {
        return urlPostFix;
    }

    public void setUrlPostFix(String urlPostFix) {
        this.urlPostFix = urlPostFix;
        if (latexObjectListener != null) latexObjectListener.refreshURL(this);
    }

    public String getMd5() {
        return md5;
    }

    public int getMode() {
        return mode;
    }

    public LatexObjectListener getLatexObjectListener() {
        return latexObjectListener;
    }

    public void setLatexObjectListener(LatexObjectListener latexObjectListener) {
        this.latexObjectListener = latexObjectListener;
    }

    public void removeLatexObjectListener() {
        this.latexObjectListener = null;
    }

    public boolean isUploaded() {
        return urlPostFix != null;
    }

    public boolean imageExists() {
        return image.exists();
    }

    @Override
    public boolean equals(Object obj) {
        try {
            return md5.compareTo(((LatexObject) obj).getMd5()) == 0;
        } catch (ClassCastException cce) {
            return super.equals(obj);
        }
    }
}
