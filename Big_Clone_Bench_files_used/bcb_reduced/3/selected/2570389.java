package com.volantis.charset;

import com.volantis.charset.configuration.Alias;
import com.volantis.charset.configuration.Charset;
import com.volantis.charset.configuration.Charsets;
import com.volantis.charset.configuration.xml.CharsetDigesterDriver;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.mcs.localization.LocalizationFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.io.UnsupportedEncodingException;

/**
 * Class to manage a map of Encodings.
 */
public class EncodingManager {

    /**
     * Used for logging
     */
    private static LogDispatcher logger = LocalizationFactory.createLogger(EncodingManager.class);

    /** 
     * Map to hold the encodings created.
     */
    private HashMap createdEncodingMap;

    /**
     * Map to hold the alias to canonical mapping.
     */
    private HashMap charsetNameAliasMap;

    /**
     * Map of the preloaded encodings.
     */
    private HashMap preloadedEncodingMap;

    /**
     * Map of the charsets from the config file.
     */
    private HashMap charsetMap;

    private HashSet unsupportedCharsetNameSet;

    /** Creates a new instance of EncodingManager */
    public EncodingManager() {
        initialiseManager();
    }

    /**
     * Parse the charset-config file to get a list of character sets
     * and their aliases.
     * The aliases are stored in a map with a value of the canononical
     * charset name.  The canononical name is the only one which is 
     * used to store encoding information.
     */
    public void initialiseManager() {
        CharsetDigesterDriver dd = new CharsetDigesterDriver();
        Charsets css = dd.digest();
        createdEncodingMap = new HashMap();
        charsetNameAliasMap = new HashMap();
        preloadedEncodingMap = new HashMap();
        unsupportedCharsetNameSet = new HashSet();
        charsetMap = new HashMap();
        if (css != null) {
            ArrayList charsets = css.getCharsets();
            Iterator i = charsets.iterator();
            while (i.hasNext()) {
                Charset cs = (Charset) i.next();
                String name = cs.getName();
                this.charsetMap.put(name, cs);
                ArrayList aliases = cs.getAlias();
                if (aliases != null) {
                    Iterator aliasIterator = aliases.iterator();
                    while (aliasIterator.hasNext()) {
                        Alias a = (Alias) aliasIterator.next();
                        charsetNameAliasMap.put(a.getName(), name);
                    }
                }
                if (cs.isPreload()) {
                    try {
                        preloadedEncodingMap.put(name, createEncoding(cs));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Loaded '" + cs + "'");
                }
            }
        }
    }

    /**
     * Get an Encoding instance for the character set name provided.
     * <p>
     * If the charset is configured within MCS, then Encoding returned will 
     * have the MIBEnum value as defined in the configuration. If not, it will 
     * a MIBEnum value of {@link Encoding#MIBENUM_NOT_CONFIGURED}.   
     * <p>
     * If the charset is not known to the underlying platform, then an 
     * exception is thrown.
     *
     * @param charsetName The name of the charset.
     * @return An encoding for this name, or null if unsupported.
     */
    public Encoding getEncoding(String charsetName) {
        Encoding encoding = null;
        String aliasName = charsetName.toLowerCase();
        String canonicalName = (String) charsetNameAliasMap.get(aliasName);
        if (canonicalName == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No alias found for '" + charsetName + "'");
            }
            canonicalName = aliasName;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Using charset '" + canonicalName + " for alias '" + charsetName + "'");
            }
        }
        encoding = searchPreloaded(canonicalName);
        return encoding;
    }

    /**
     * Search for Encodings which are in the config file and were marked for 
     * preloading. These have already been created at startup.
     * 
     * @param charsetName
     * @return An encoding for this name, or null if unsupported.
     */
    private Encoding searchPreloaded(String charsetName) {
        Encoding encoding = (Encoding) preloadedEncodingMap.get(charsetName);
        if (encoding != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found preloaded encoding for '" + charsetName + "'");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No preloaded encoding for '" + charsetName + "'");
            }
            encoding = searchCreated(charsetName);
        }
        return encoding;
    }

    /**
     * Search for Encodings which are in the config file but were not marked 
     * for preloading. These will be created on the fly.
     * 
     * @param charsetName
     * @return An encoding for this name, or null if unsupported.
     */
    private Encoding searchCreated(String charsetName) {
        Encoding encoding;
        synchronized (charsetName.intern()) {
            encoding = (Encoding) createdEncodingMap.get(charsetName);
            if (encoding != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found previously created encoding for '" + charsetName + "'");
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("No previously created encoding for '" + charsetName + "'");
                }
                encoding = searchSupported(charsetName);
            }
        }
        return encoding;
    }

    /**
     * Search for Encodings which are supported by the platform.
     * @param charsetName
     * @return An encoding for this name, or null if unsupported.
     */
    private Encoding searchSupported(String charsetName) {
        if (isSupportedCharset(charsetName)) {
            try {
                return searchCharset(charsetName);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    private Encoding searchCharset(String charsetName) throws UnsupportedEncodingException {
        Encoding encoding;
        Charset charset = (Charset) charsetMap.get(charsetName);
        if (charset != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("A charset configuration was found for '" + charsetName + "', creating encoding from " + "configuration " + charset);
            }
            encoding = createEncoding(charset);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No charset configuration was found for '" + charsetName + "', creating encoding from " + "Java charset name only (no MIBEnum)");
            }
            encoding = new BitSetEncoding(charsetName, Encoding.MIBENUM_NOT_CONFIGURED);
        }
        createdEncodingMap.put(charsetName, encoding);
        return encoding;
    }

    private boolean isSupportedCharset(String charsetName) {
        boolean supported;
        if (unsupportedCharsetNameSet.contains(charsetName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Encoding '" + charsetName + "' not supported (cached)");
            }
            supported = false;
        } else {
            try {
                "test".getBytes(charsetName);
                if (logger.isDebugEnabled()) {
                    logger.debug("Encoding '" + charsetName + "' is supported");
                }
                supported = true;
            } catch (UnsupportedEncodingException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Encoding '" + charsetName + "' not supported");
                }
                unsupportedCharsetNameSet.add(charsetName);
                supported = false;
            } catch (RuntimeException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Encoding '" + charsetName + "' invalid", e);
                }
                unsupportedCharsetNameSet.add(charsetName);
                supported = false;
            }
        }
        return supported;
    }

    /**
     * Private helper method to create an Encoding from a Charset 
     * configuration.
     *  
     * @param charset
     * @return the created encoding
     * @throws UnsupportedEncodingException
     */
    private Encoding createEncoding(Charset charset) throws UnsupportedEncodingException {
        Encoding encoding;
        if (charset.isComplete()) {
            encoding = new NoEncoding(charset.getName(), charset.getMIBenum());
        } else {
            encoding = new BitSetEncoding(charset.getName(), charset.getMIBenum());
        }
        return encoding;
    }

    /** Getter for property aliasesMap.
    * @return Value of property aliasesMap.
    *
     */
    HashMap getCharsetNameAliasMap() {
        return charsetNameAliasMap;
    }

    /** Getter for property preloadMap.
    * @return Value of property preloadMap.
    *
     */
    HashMap getPreloadedEncodingMap() {
        return preloadedEncodingMap;
    }

    /** Getter for property charsetMap.
    * @return Value of property charsetMap.
    *
     */
    HashMap getCharsetMap() {
        return charsetMap;
    }
}
