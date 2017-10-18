package org.suli.kozosprojekt.brt.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.suli.kozosprojekt.brt.ICommonValues;

/**
 * Copyright (C) 2010 Szabolcs Rugina <a
 * href="mailto:ruginaszabolcsREMOVEME@gmail.com">ruginaszabolcsREMOVEME@gmail.com</a>
 * <p/>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
public class ProjektUtil {

    /** The LOG. */
    private static Logger LOG = Logger.getLogger(ProjektUtil.class);

    private static Map<String, String> EXTENSION_MAP;

    /**
     * Checks if is matching as password.
     * 
     * @param password
     *            the password
     * @param amd5Password
     *            the amd5 password
     * @return true, if is matching as password
     */
    public static boolean isMatchingAsPassword(final String password, final String amd5Password) {
        boolean response = false;
        try {
            final MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(password.getBytes());
            final byte[] md5Byte = algorithm.digest();
            final StringBuffer buffer = new StringBuffer();
            for (final byte b : md5Byte) {
                if ((b <= 15) && (b >= 0)) {
                    buffer.append("0");
                }
                buffer.append(Integer.toHexString(0xFF & b));
            }
            response = (amd5Password != null) && amd5Password.equals(buffer.toString());
        } catch (final NoSuchAlgorithmException e) {
            ProjektUtil.LOG.error("No digester MD5 found in classpath!", e);
        }
        return response;
    }

    /**
     * Gets the md5 password.
     * 
     * @param password
     *            the password
     * @return the md5 password
     */
    public static String getMd5Password(final String password) {
        String response = null;
        try {
            final MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(password.getBytes());
            final byte[] md5Byte = algorithm.digest();
            final StringBuffer buffer = new StringBuffer();
            for (final byte b : md5Byte) {
                if ((b <= 15) && (b >= 0)) {
                    buffer.append("0");
                }
                buffer.append(Integer.toHexString(0xFF & b));
            }
            response = buffer.toString();
        } catch (final NoSuchAlgorithmException e) {
            ProjektUtil.LOG.error("No digester MD5 found in classpath!", e);
        }
        return response;
    }

    public static Map<String, String> getExtensionMap() {
        if (ProjektUtil.EXTENSION_MAP == null) {
            final Properties prop = new Properties();
            try {
                final ServletContext servletContext = ServletActionContext.getServletContext();
                final InputStream stream = servletContext.getResourceAsStream(ICommonValues.EXTENSIONS_FILE_NAME);
                if (stream != null) {
                    prop.load(stream);
                    ProjektUtil.EXTENSION_MAP = new HashMap<String, String>();
                    for (final Object key : prop.keySet()) {
                        ProjektUtil.EXTENSION_MAP.put(key.toString(), prop.getProperty(key.toString()));
                    }
                }
            } catch (final IOException ignored) {
            }
        }
        return ProjektUtil.EXTENSION_MAP;
    }
}
