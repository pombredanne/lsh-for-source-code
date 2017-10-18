package dutchradioscrobbler.tools;

import java.awt.Image;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import dutchradioscrobbler.Cache;
import dutchradioscrobbler.Constants;
import dutchradioscrobbler.Options;
import dutchradioscrobbler.Track;
import java.io.File;
import java.math.BigInteger;

/**
 *
 * @author Niek Haarman
 * 27-jan-2010 15:37:06
 *
 * Several tools needed in the application.
 */
public class ScrobbleTools {

    /**
     * Returns the given String s with the added param and value.
     * @param s the String to add to.
     * @param param the parameter to add.
     * @param value the value to add. Will be UTF-8 encoded.
     * @return s + "&" + param + "=" + value or return s + param + "=" +value
     */
    public static String addParam(String s, String param, String value) {
        try {
            if (s.length() > 0) {
                return s + "&" + param + "=" + URLEncoder.encode(value, "UTF-8");
            } else {
                return s + param + "=" + URLEncoder.encode(value, "UTF-8");
            }
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String getUnixTimestamp() {
        return System.currentTimeMillis() / 1000 + "";
    }

    public static String getSessionId(String handShakeResult) {
        return handShakeResult.substring(2, handShakeResult.indexOf("http"));
    }

    public static String getNowPlayingUrl(String handShakeResult) {
        return handShakeResult.substring(handShakeResult.indexOf("http:"), handShakeResult.lastIndexOf("http"));
    }

    public static String getSubmissionUrl(String handShakeResult) {
        return handShakeResult.substring(handShakeResult.lastIndexOf("http"));
    }

    public static Image createImage(String path, String description) {
        try {
            URL imageURL = new File(path).toURI().toURL();
            if (imageURL == null) {
                System.err.println("Resource not found: " + path);
                return null;
            } else {
                ImageIcon res = new ImageIcon(imageURL, description);
                return (res).getImage();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Reads the Options specified in Constants.OPTIONSFILE
     * @return new Options() if an error occurs, else the Options in the file.
     */
    public static Options readOptions() {
        Options result = new Options();
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(Constants.OPTIONSFILE));
            result = (Options) inputStream.readObject();
            result.setScrobbleCurrentNowPlayingTrack(true);
        } catch (FileNotFoundException ex) {
        } catch (ClassNotFoundException ex) {
        } catch (IOException ex) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Writes the given Options to Constants.OPTIONSFILE
     * @param options the Options to write
     */
    public static void writeOptions(Options options) {
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(Constants.OPTIONSFILE));
            output.writeObject(options);
        } catch (IOException ex) {
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Reads the cache specified in Constants.CACHEFILE
     * @return a new empty ArrayList<Track> if an error occurs, else the ArrayList<Track> in the file.
     */
    public static Cache<Track> readCache() {
        Cache<Track> result = new Cache<Track>();
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(Constants.CACHEFILE));
            result = (Cache<Track>) inputStream.readObject();
        } catch (FileNotFoundException ex) {
        } catch (ClassNotFoundException ex) {
        } catch (IOException ex) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Writes the given cache to Constants.CACHEFILE
     * @param cache the Cache<Track> representing the cache to write
     */
    public static void writeCache(Cache<Track> cache) {
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(Constants.CACHEFILE));
            output.writeObject(cache);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String MD5(String text) {
        byte[] md5hash = new byte[32];
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            md5hash = md.digest();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return convertToHex(md5hash);
    }

    public static String MD5ToString(String md5) {
        String hashword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(md5.getBytes());
            BigInteger hash = new BigInteger(1, md.digest());
            hashword = hash.toString(16);
        } catch (NoSuchAlgorithmException nsae) {
        }
        return hashword;
    }

    public static final String getBuild() {
        ResourceBundle rb = ResourceBundle.getBundle("version");
        String msg = "";
        try {
            msg = rb.getString("BUILD");
        } catch (MissingResourceException e) {
            System.err.println("Token ".concat("BUILD").concat(" not in Propertyfile!"));
        }
        return msg;
    }
}
