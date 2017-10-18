package com.entelience.provider;

import org.apache.log4j.Logger;
import com.entelience.util.Logs;
import com.entelience.objects.CnilLevel;
import com.entelience.sql.Db;
import com.entelience.util.Config;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.entelience.util.StringHelper;

/**
 * CnilLevel 
 */
public class DbCnilLevel {

    private static final String digest = "SHA";

    private static CnilLevel currentLevel;

    private DbCnilLevel() {
    }

    protected static final Logger _logger = Logs.getLogger();

    /**
     * Returns the currently configured CnilLevel
     */
    public static CnilLevel getCnilLevel(Db db) throws Exception {
        int cnilLevel = Config.getProperty(db, "com.entelience.esis.cnilLevel", 0);
        CnilLevel level = CnilLevel.getValueOf(cnilLevel);
        _logger.info("CNIL Level : " + level.getDescription());
        if (currentLevel == null) currentLevel = level;
        return level;
    }

    /**
     * Returns the current <b>cache</b> level. This is null until
     * a call to getCnilLevel.
     */
    public static CnilLevel getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Reset the cached level
     */
    public static void resetCurrentLevel() {
        currentLevel = null;
    }

    /**
     * Returns the current message digest */
    public static MessageDigest getAnonymizer() throws Exception {
        try {
            return MessageDigest.getInstance(digest);
        } catch (NoSuchAlgorithmException nsae) {
            _logger.error("Could not retrieve the message digest (" + digest + ")", nsae);
            return null;
        }
    }

    /**
     * Wrapper, use the currentLevel (cache from previous call to getCnilLevel)
     */
    public static String obfuscateUsername(String user) throws Exception {
        return obfuscateUsername(currentLevel, user);
    }

    /**
     * Obfuscate user's emails or not according to the cnilLevel. 0 is no obfuscation.
     * 1 is hash of e-mail. all other is no user info.
     */
    public static String obfuscateUsername(CnilLevel level, String user) throws Exception {
        MessageDigest anonymizer = getAnonymizer();
        switch(level) {
            case LEVEL_0:
                return StringHelper.nullify(user);
            case LEVEL_1:
                if (StringHelper.nullify(user) == null) return null;
                anonymizer.update(StringHelper.nullify(user).getBytes());
                java.math.BigInteger hash = new java.math.BigInteger(1, anonymizer.digest());
                return hash.toString(16);
            case LEVEL_2:
                return "Anonymized user";
            default:
                _logger.fatal("Unknown CnilLevel (" + level + ")");
                return null;
        }
    }
}
