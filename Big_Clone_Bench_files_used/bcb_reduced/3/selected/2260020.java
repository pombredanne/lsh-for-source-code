package visitpc.lib.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Scanner;
import javax.swing.JOptionPane;
import visitpc.VisitPCException;
import visitpc.lib.gui.Dialogs;
import visitpc.lib.gui.GenericConfigDialog;
import visitpc.lib.gui.PasswordJDialog;
import java.util.*;
import java.io.*;
import javax.swing.ImageIcon;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.net.URL;
import java.awt.*;

/**
 * Responsible for providing a central point for access to methods and constants used by the 
 * simpleConfig classes. Due to the introspective nature of these classes these variables and
 * methods cannot be placed in the object hierarchy of the SimpleConfig classes.
 *
 */
public class SimpleConfigHelper {

    public static final String JAR_CONFIG_PATH = "/config/";

    public static final String JAR_CONFIG_FILENAME = "config.properties";

    public static final String JAR_CONFIG_FILE = SimpleConfigHelper.JAR_CONFIG_PATH + SimpleConfigHelper.JAR_CONFIG_FILENAME;

    public static final String PASSWD_MD5SUM_KEY = "PASSWD_MD5SUM";

    public static final String FILES_KEY = "FILES";

    public static final String CMD_LINE_ARGS_KEY = "CMD_LINE_ARGS_KEY";

    public static final String CLIENT_TRUSTSTORE_FILE_KEY = "CLIENT_TRUSTSTORE_FILE_KEY";

    public static final String LOGO_IMAGE_FILE_KEY = "LOGO_IMAGE_FILE_KEY";

    public static final String SMALL_LOGO_IMAGE_FILE_KEY = "SMALL_LOGO_IMAGE_FILE_KEY";

    public static final String ALLOW_USER_CONFIG_CHANGE_KEY = "ALLOW_USER_CONFIG_CHANGE_KEY";

    public static final String YES = "yes";

    public static final String NO = "no";

    public static final String OBFUSRACTION_KEY = "APEDLF47";

    public static final int MIN_PASSWORD_LENGTH = 8;

    private String logoFileName = null;

    /**
   * Constructor
   */
    public SimpleConfigHelper() {
    }

    /**
   * Read the properties from the jar config file.
   * 
   * @return The Properties object read from the jar file. This will be empty if the jar file dos not contain properties
   */
    private Properties getJarProperties() {
        Properties configProperties = new Properties();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(SimpleConfigHelper.JAR_CONFIG_FILE)));
            configProperties = new Properties();
            configProperties.load(br);
            br.close();
        } catch (Exception e) {
        }
        return configProperties;
    }

    /**
   * Add the command line argument to the given properties object.
   * 
   * @param properties
   * @param arg
   */
    public static void AddCommandLineArgs(Properties properties, String arg) {
        String currentCmdLineArgs = properties.getProperty(SimpleConfigHelper.CMD_LINE_ARGS_KEY, "");
        String newCmdLineArgs = currentCmdLineArgs + "\t" + arg;
        properties.put(SimpleConfigHelper.CMD_LINE_ARGS_KEY, newCmdLineArgs);
    }

    /**
   * Get a list of command line arguments contained within the jar file.
   * 
   * @return An Array of Strings, each String being a single command line argument. 
   * If the jar file contains no command line arguments then the array will have a length of 0.
   *  
   * @throws IOException
   */
    public String[] getArgsFromJar() throws IOException {
        Vector<String> jarArgs = new Vector<String>();
        if (isConfigFromJar()) {
            Properties configProperties = getJarProperties();
            if (configProperties.containsKey(SimpleConfigHelper.CMD_LINE_ARGS_KEY)) {
                String cmdArgsString = configProperties.getProperty(SimpleConfigHelper.CMD_LINE_ARGS_KEY);
                StringTokenizer strTok = new StringTokenizer(cmdArgsString, "\t");
                while (strTok.hasMoreTokens()) {
                    String argFromJar = strTok.nextToken();
                    jarArgs.add(argFromJar);
                }
            }
        }
        String args[] = new String[jarArgs.size()];
        int index = 0;
        for (String a : jarArgs) {
            args[index] = a;
            index++;
        }
        return args;
    }

    /**
   * Determine if configuration can be loaded from the jar file.
   * 
   * @return True if config can be loaded from jar file, false if not.
   */
    public boolean isConfigFromJar() {
        boolean loadConfigFromJar = false;
        Properties configProperties = getJarProperties();
        if (configProperties.containsKey(SimpleConfigHelper.FILES_KEY)) {
            loadConfigFromJar = true;
        }
        return loadConfigFromJar;
    }

    public class CredentialsConfig extends SimpleConfig {

        public String username = "";

        public String thePassword = "";

        public static final String USERNAME_ATTRS = "Username\tPlease enter the username here";

        public static final String THEPASSWORD_ATTRS = "Password\tPlease enter a password at least 8 character long here.";

        /**
     * Determine if the server config is valid
     * 
     * If the configuration is not valid an VisitPCException is thrown.
     */
        public void checkValid() throws VisitPCException {
            if (username == null || username.length() < 1) {
                throw new VisitPCException("No username defined");
            }
            if (thePassword == null || thePassword.length() < 1) {
                throw new VisitPCException("No password defined");
            }
            if (thePassword.length() < MIN_PASSWORD_LENGTH) {
                throw new VisitPCException("The password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
            }
        }

        public boolean isValid() {
            boolean valid = false;
            try {
                checkValid();
                valid = true;
            } catch (VisitPCException e) {
                Dialogs.showErrorDialog(null, "Warning", e.getLocalizedMessage());
            }
            return valid;
        }
    }

    /**
   * Get a username and password
   */
    public static String[] GetCredentials(boolean guiMode, Frame frame) {
        String username = null;
        String password = null;
        if (guiMode) {
            GenericConfigDialog genericConfigDialog = new GenericConfigDialog(frame);
            SimpleConfigHelper sch = new SimpleConfigHelper();
            CredentialsConfig config = sch.new CredentialsConfig();
            try {
                genericConfigDialog.setConfig(config);
                while (true) {
                    genericConfigDialog.setVisible(true);
                    if (!genericConfigDialog.isOkSelected()) {
                        System.exit(0);
                    }
                    config = (CredentialsConfig) genericConfigDialog.getConfig(config);
                    if (config.username == null || config.username.length() == 0) {
                        Dialogs.showErrorDialog(frame, "Warning", "Please enter a username.");
                    } else if (config.thePassword == null || config.thePassword.length() == 0) {
                        Dialogs.showErrorDialog(frame, "Warning", "Please enter a password.");
                    } else if (config.thePassword.length() < 8) {
                        Dialogs.showErrorDialog(frame, "Warning", "Please enter a password at least 8 characters long.");
                    } else {
                        username = config.username;
                        password = config.thePassword;
                        break;
                    }
                }
            } catch (IllegalAccessException e) {
            } catch (IOException e) {
            }
        } else {
            Scanner scanner = new Scanner(new BufferedInputStream(System.in), "UTF-8");
            while (true) {
                System.out.print("INPUT: Please enter your VisitPC username: ");
                username = scanner.nextLine();
                System.out.print("INPUT: Please enter your VisitPC password: ");
                password = scanner.nextLine();
                if (username == null || username.length() == 0 || password == null || password.length() == 0) {
                    System.exit(0);
                } else if (password.length() < 8) {
                    System.out.println("INFO:  The password must contain at least 8 characters.");
                } else {
                    break;
                }
            }
        }
        String credentials[] = new String[2];
        credentials[0] = username;
        credentials[1] = password;
        return credentials;
    }

    public String extractTrustStoreTo(String destPath) throws IOException {
        byte buffer[] = new byte[2048];
        Properties properties = getJarProperties();
        int byteCount;
        if (!properties.containsKey(SimpleConfigHelper.CLIENT_TRUSTSTORE_FILE_KEY)) {
            throw new IOException("The jar file does not contain the client trust store file.");
        }
        String trustStoreFile = properties.getProperty(SimpleConfigHelper.CLIENT_TRUSTSTORE_FILE_KEY);
        String trustStoreFileName = new File(trustStoreFile).getName();
        File destFile = new File(destPath, trustStoreFileName);
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            fos = new FileOutputStream(destFile);
            is = getClass().getResourceAsStream(trustStoreFile);
            while (true) {
                byteCount = is.read(buffer, 0, 2048);
                if (byteCount < 0) {
                    break;
                }
                fos.write(buffer, 0, byteCount);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return destFile.getAbsolutePath();
    }

    /**
   * Get a small logo ImageIcon (used in top left hand cornet of all windows/dialogs) stored in the jar file
   * 
   * @return The small logo ImageIcon object
   */
    public ImageIcon getSmallLogoImageIcon() {
        ImageIcon smallLogo = null;
        Properties properties = getJarProperties();
        if (properties.containsKey(SimpleConfigHelper.SMALL_LOGO_IMAGE_FILE_KEY)) {
            String smallLogoImageFileInJar = (String) properties.get(SimpleConfigHelper.SMALL_LOGO_IMAGE_FILE_KEY);
            URL url = getClass().getResource(smallLogoImageFileInJar);
            smallLogo = new ImageIcon(url);
        }
        return smallLogo;
    }

    /**
   * Get a logo ImageIcon (used as the startup splash screen) stored in the jar file
   * 
   * @return The logo ImageIcon object or null if no logo is defined
   */
    public ImageIcon getLogoImageIcon() {
        ImageIcon logo = null;
        logoFileName = null;
        try {
            Properties properties = getJarProperties();
            if (properties.containsKey(SimpleConfigHelper.LOGO_IMAGE_FILE_KEY)) {
                String smallLogoImageFileInJar = (String) properties.get(SimpleConfigHelper.LOGO_IMAGE_FILE_KEY);
                URL url = getClass().getResource(smallLogoImageFileInJar);
                logoFileName = url.getFile();
                int pos = logoFileName.lastIndexOf("/");
                if (pos > 0) {
                    logoFileName = logoFileName.substring(pos + 1);
                }
                logo = new ImageIcon(url);
            }
        } catch (Exception e) {
        }
        return logo;
    }

    /**
   * Get the logo*.png filename.
   * Should only be called after getLogoImageIcon() has been called.
   * 
   * @return The log*.png filename or null if no logo*.png file is contained in the jar file.
   */
    public String getLogoFileName() {
        return logoFileName;
    }

    /**
   * 
   * @return If the user is allowed to change the config return true
   */
    public boolean allowUserConfigChange() {
        boolean allowconfigChange = false;
        Properties properties = getJarProperties();
        if (properties.containsKey(SimpleConfigHelper.ALLOW_USER_CONFIG_CHANGE_KEY)) {
            String changeConfigProperty = properties.getProperty(SimpleConfigHelper.ALLOW_USER_CONFIG_CHANGE_KEY);
            if (changeConfigProperty.equals(SimpleConfigHelper.YES)) {
                allowconfigChange = true;
            }
        }
        return allowconfigChange;
    }

    /**
   * Helper method to cal the MD5sum of a string
   * 
   * @param s The string over which to calculate the MD5sum
   * @return The md5sum as a String object
   * @throws NoSuchAlgorithmException
   */
    public static String GetMD5SUM(String s) throws NoSuchAlgorithmException {
        MessageDigest algorithm = MessageDigest.getInstance("MD5");
        algorithm.reset();
        algorithm.update(s.getBytes());
        byte messageDigest[] = algorithm.digest();
        String md5sum = Base64.encode(messageDigest);
        return md5sum;
    }

    /**
   * This is called when the config is loaded from a jar file.
   * The config file must be present in the jar file.
   * 
   * @param key
   */
    public void verifyKey(String password) throws IncorrectKeyException, IOException, NoSuchAlgorithmException {
        Properties configProperties = getJarProperties();
        if (configProperties.containsKey(SimpleConfigHelper.PASSWD_MD5SUM_KEY)) {
            String orgPWMD5SUM = configProperties.getProperty(SimpleConfigHelper.PASSWD_MD5SUM_KEY);
            String thisPWMD5SUM = SimpleConfigHelper.GetMD5SUM(password);
            if (!orgPWMD5SUM.equals(thisPWMD5SUM)) {
                throw new IncorrectKeyException("The password is incorrect");
            }
        } else {
            throw new IncorrectKeyException("Password not found");
        }
    }

    /**
   * Allow user to input a password.
   */
    public static String GetPassword(boolean guiMode, Frame frame) {
        String configPassword = null;
        if (guiMode) {
            while (true) {
                PasswordJDialog passwordJDialog = new PasswordJDialog(null, "Password", "Please enter your VisitPC password");
                passwordJDialog.setVisible(true);
                configPassword = passwordJDialog.getPassword();
                if (configPassword == null) {
                    System.exit(0);
                } else if (configPassword.length() < 8) {
                    int response = Dialogs.showYesNoDialog(frame, "Enter Password", "The password must contain at least 8 characters. Try again ?");
                    if (response != JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                } else {
                    break;
                }
            }
        } else {
            Scanner scanner = new Scanner(new BufferedInputStream(System.in), "UTF-8");
            while (true) {
                System.out.print("INPUT: Please enter your VisitPC password: ");
                configPassword = scanner.nextLine();
                if (configPassword == null || configPassword.length() == 0) {
                    System.exit(0);
                } else if (configPassword.length() < 8) {
                    System.out.println("INFO:  The password must contain at least 8 characters.");
                } else {
                    break;
                }
            }
        }
        return configPassword.substring(0, 8);
    }
}
