package org.snipsnap.jetty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

/**
 * SnipSnap application server configuration.
 *
 * @author leo
 * @version $Id$
 */
public class Configuration extends Properties {

    public static final String VERSION = "snipsnap.server.version";

    public static final String ENCODING = "snipsnap.server.encoding";

    public static final String ADMIN_USER = "snipsnap.server.admin.user";

    public static final String ADMIN_PASS = "snipsnap.server.admin.password";

    public static final String ADMIN_URL = "snipsnap.server.admin.rpc.url";

    public static final String ADMIN_EMAIL = "snipsnap.server.admin.email";

    public static final String WEBAPP_ROOT = "snipsnap.server.webapp.root";

    /**
   * Initialize configuration by loading defaults from the jar and
   * overwriting these with locally stored configuration data.
   */
    public Configuration() {
        super();
        try {
            load(ApplicationServer.class.getResourceAsStream("/conf/server.conf"));
        } catch (Exception e) {
        }
        File configFile = new File("conf/server.conf");
        if (!configFile.exists() || configFile.length() == 0) {
            System.err.println("ApplicationServer: unconfigured server, loading preferences");
            setProperty(ADMIN_PASS, createServerKey());
            File webappRoot = new File(getProperty(WEBAPP_ROOT));
            if (!webappRoot.exists()) {
                webappRoot.mkdirs();
            }
            System.err.println(WEBAPP_ROOT + "=" + webappRoot.getAbsolutePath());
            if (!webappRoot.exists() || !webappRoot.canWrite()) {
                System.err.println("ApplicationServer: current webapp root directory non-existent or non-writable");
                webappRoot = new File(System.getProperty("user.home"), "applications");
            }
            setProperty(WEBAPP_ROOT, webappRoot.getAbsolutePath());
            try {
                OutputStream configSave = new FileOutputStream(new File("conf/server.conf"));
                store(configSave, " SnipSnap Application Server configuration");
            } catch (Exception e) {
                System.err.println("ApplicationServer: unable to store server configuation: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            try {
                load(new FileInputStream(configFile));
            } catch (IOException e) {
                System.err.println("ApplicationServer: unable to load local configuration: " + e.getMessage());
            }
        }
    }

    /**
   * Small helper method to create a somewhat random installation key
   * @return a string with the installation key
   */
    private static String createServerKey() {
        byte[] tmpKey;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            tmpKey = digest.digest(("" + new Date()).getBytes());
        } catch (NoSuchAlgorithmException e1) {
            tmpKey = new byte[3];
            new Random().nextBytes(tmpKey);
        }
        StringBuffer keyBuffer = new StringBuffer();
        for (int i = 0; i < 3; i++) {
            keyBuffer.append(Integer.toHexString(tmpKey[i] & 0xff).toLowerCase());
        }
        return keyBuffer.toString();
    }
}
