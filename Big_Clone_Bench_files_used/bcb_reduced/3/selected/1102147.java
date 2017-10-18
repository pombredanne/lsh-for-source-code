package net.sourceforge.acacia.security;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;

/**
  * Utilities to set/get configurable locations of various security related files.
  * <p>
  * Under the gridsecurity root directory, the files expected are in the following locations.
  * <pre>
  * gridsecurityroot -
  *                  |-- certificates/                          need all CA root certs of any certificates that will need to be verified
  *                  |              |-- checked_overlaps.xml    needed to be able to use CAs with two certificates due to rollover
  *                  |-- hostcert.pem                           relevant if hosting a service
  *                  |-- hostkey.pem                            relevant if hosting a service
  *                  |-- x509_proxy.pem                         if created
  *                  |-- x509_vomsproxy.pem                     if created
  *                  |-- vomsdir/                               all the VOMS server public certificates
  *                                 |-- voms.xml                essential for the voms systems to work
  * </pre>
  * The default directory can be changed either programatically or by an environment variable ACACIA_SECURITY_ROOT
  * <p>
  * The defualt directory is based on the CoGProperties.getCaCertLocations() parent directory on a unix system - i.e. /etc/grid-security
  *
  * @author	Gidon Moont / Imperial College London / GridPP Portal Project
  * @version	1.00 (7 March 2007)
  */
public class GridSecurityConfiguration {

    static Logger logger = Logger.getLogger("net.sourceforge.acacia.security");

    /**
  * Sets the location of the gridsecurity directory.
  * <p>
  * The default is /etc/grid-security/ - this may not be useable by a client...
  *
  * @param gridsecurityRoot	the location of the gridsecurity directory
  */
    public static void setGridSecrityRoot(String gridsecurityRoot) {
        try {
            System.setProperty("ACACIA_SECURITY_ROOT", gridsecurityRoot);
        } catch (SecurityException se) {
            logger.fatal("Unable to set the ACACIA_SECURITY_ROOT environment variable");
        }
    }

    /**
  * Returns the location of the gridsecurity directory.
  * <p>
  * This value can be set either using the {@linkplain #setGridSecrityRoot} method, or by setting the environment variable ACACIA_SECURITY_ROOT.
  * <p>
  * If neither of these are done, then this method returns a default location of /etc/grid-security
  *
  * @return the location of the gridsecurity directory
  */
    public static String getGridSecrityRoot() {
        String gridsecurityRoot = new String("/etc/grid-security");
        try {
            try {
                gridsecurityRoot = System.getenv("ACACIA_SECURITY_ROOT");
            } catch (Error wrong) {
                logger.warn("this system is incorrectly complaining about the use of System.getenv - which is deprecated but allowed.");
            }
            gridsecurityRoot = System.getProperty("ACACIA_SECURITY_ROOT", gridsecurityRoot);
        } catch (Exception e) {
            logger.warn("Unable to get the ACACIA_SECURITY_ROOT environment variable, so presuming a value of /etc/grid-security/");
            gridsecurityRoot = new String("/etc/grid-security");
        }
        return gridsecurityRoot;
    }

    public static String getGridSecrityCACertsLocations() {
        return new String(getGridSecrityRoot() + File.separator + "certificates" + File.separator);
    }

    public static String getGridSecrityCACertsOverlaps() {
        return new String(getGridSecrityCACertsLocations() + "checked_overlaps.xml");
    }

    public static String getGridSecrityHostCert() {
        return new String(getGridSecrityRoot() + File.separator + "hostcert.pem");
    }

    public static String getGridSecrityHostKey() {
        return new String(getGridSecrityRoot() + File.separator + "hostkey.pem");
    }

    public static String getGridSecrityVOMSCertsLocations() {
        return new String(getGridSecrityRoot() + File.separator + "vomsdir" + File.separator);
    }

    public static String getGridSecrityVOMSConfigLocation() {
        return new String(getGridSecrityVOMSCertsLocations() + "voms.xml");
    }

    public static String getGridSecrityProxyLocation() {
        return new String(getGridSecrityRoot() + File.separator + "x509_proxy.pem");
    }

    public static String getGridSecrityVomsProxyLocation() {
        return new String(getGridSecrityRoot() + File.separator + "x509_vomsproxy.pem");
    }

    /**
  * Utility to calculate the openSSL style c_hash of a DN - in this case that of the Issuer
  * <p>
  * Quick way to target the correct signing CA of a given certificate
  * 
  * @param certificate	X509 certificate
  * @return c_hash	the hashed DN of the certificate's Issuer
  */
    public static String hashIssuer(X509Certificate certificate) {
        String c_hash = new String("00000000");
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] derEncodedIssuer = certificate.getIssuerX500Principal().getEncoded();
            md5.update(derEncodedIssuer);
            byte[] messageDigest = md5.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 3; i >= 0; i--) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            c_hash = hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
            logger.error("No MD5 message digest algorithm available?", nsae);
        }
        return c_hash;
    }
}
