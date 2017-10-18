package mou.core.security;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.logging.Level;
import mou.Main;
import mou.Subsystem;

/**
 * @author pb
 */
public class SecuritySubsystem extends Subsystem {

    private static SecuritySubsystem instance = null;

    private MessageDigest md;

    private long serial;

    private String password = "";

    /**
	 * @param parent
	 */
    public SecuritySubsystem(Subsystem parent) {
        super(parent);
        instance = this;
    }

    public static SecuritySubsystem instance() {
        return instance;
    }

    protected Level getDefaultLoggerLevel() {
        return Level.ALL;
    }

    public String getModulName() {
        return "SecuritySubsystem";
    }

    protected void shutdownIntern() {
    }

    protected File getPreferencesFile() {
        return null;
    }

    protected void startModulIntern() throws Exception {
    }

    public long getSerialNumber() {
        return serial;
    }

    public String getPassword() {
        return password;
    }

    /**
	 * Generiert mit Hilfe von Hashes einen Long Wert. Dien in erste Linie dazu aus f�r Menschen
	 * leichter einpr�gsamer Benutzernamen den ClientID zu berechnen.
	 * 
	 * @param material
	 * @return
	 */
    synchronized long generateSerialNumber(String material) {
        md.reset();
        md.update(material.getBytes());
        byte[] digest = md.digest();
        return new BigInteger(digest).longValue();
    }
}
