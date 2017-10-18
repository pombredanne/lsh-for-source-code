package org.net2map.netlib;

import java.net.*;
import java.io.*;
import java.security.*;
import java.util.*;
import net.jxta.id.*;
import net.jxta.peergroup.*;
import net.jxta.pipe.*;
import net.jxta.endpoint.*;
import net.jxta.endpoint.Message.ElementIterator;
import net.jxta.protocol.*;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;

/**
 * Various tools used by other classes in the P2P package
 * @author  Philippe MOULIN
 */
public class ToolBox {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToolBox.class.getName());

    /** Creates a new instance of ToolBox */
    public ToolBox() {
    }

    /**
     * Converts a String to a PipeID.
     * @param paramPeerGroupID The PeerGroup in which this PipeID will be used.
     *
     * @param paramString The "Seed" string.
     * @return The PipeID
     */
    public static PipeID StringToPipeID(final PeerGroupID paramPeerGroupID, final String paramString) {
        MessageDigest sha = null;
        StringBuffer theStringBuffer = new StringBuffer(paramString);
        byte[] theSeed = new byte[theStringBuffer.length()];
        for (int iterator = 0; (iterator < 1024) && (iterator < theStringBuffer.length()); iterator++) {
            theSeed[iterator] = (byte) (theStringBuffer.charAt(iterator));
        }
        PipeID result;
        try {
            sha = MessageDigest.getInstance("SHA-1");
            sha.update(theSeed);
            result = (PipeID) IDFactory.newPipeID(paramPeerGroupID, sha.digest());
            logger.warn("TOOLBOX:\t " + result.toString());
            return result;
        } catch (java.security.NoSuchAlgorithmException theException) {
            System.out.println("SHA-1 is not available");
            result = (PipeID) IDFactory.newPipeID(paramPeerGroupID, theSeed);
            logger.warn("TOOLBOX:\t " + result.toString());
            return result;
        }
    }

    /**
     * Converts a String into an Jxta ID string
     * @param paramString The string to convert
     *
     * @return The resulting Jxta ID String
     */
    public static String StringToIDString(final String paramString) {
        String hash = Integer.toHexString(paramString.hashCode());
        System.out.println("TOOLBOX:\t " + paramString + "'s hashcode = " + hash);
        StringBuffer theStringBuffer = new StringBuffer(hash);
        if (0 != theStringBuffer.length() % 2) {
            theStringBuffer.append("2");
        }
        while (theStringBuffer.length() < 16 + 34) {
            theStringBuffer.append("02");
        }
        String uniqid = "jxta:uuid-cafedeca123456789a" + theStringBuffer.toString();
        logger.warn("TOOLBOX:\t " + uniqid);
        return (uniqid);
    }

    /**
     * Converts A String into a Jxta ID
     * @param paramString The String to convert
     *
     * @return An URL suitable for Jxta ID creation.
     */
    public static java.net.URL StringToID(final String paramString) {
        java.net.URL result = null;
        try {
            result = new java.net.URL("urn", "", StringToIDString(paramString));
        } catch (MalformedURLException theException) {
            System.out.println("TOOLBOX:\t Failed to create URL" + theException);
        }
        System.out.println("TOOLBOX:\t " + paramString + "'s URL = " + result.toString());
        return (result);
    }

    /**
     * Retrieves a string from a Jxta message
     * @param paramMessage The Jxta Message
     * @param paramElementName The requested String's element name.
     * @return The requested string, (or null if it is not found)
     */
    public static String extractStringFromMessage(final Message paramMessage, final String paramElementName) {
        ElementIterator theElementIterator = paramMessage.getMessageElements(paramElementName);
        String result;
        result = (String) theElementIterator.next().toString();
        logger.warn("TOOLBOX:\t " + result);
        return result;
    }

    /**
     * Creates a Jxta String Message
     * @param paramContent The content string.
     *
     * @param paramElementName this string's name.
     *
     * @return The resulting Jxta message, ready to be sent!
     */
    public static Message makeStringMessage(final String paramContent, final String paramElementName) {
        Message theMessage = new Message();
        theMessage.addMessageElement(new StringMessageElement(paramElementName, paramContent, null));
        logger.warn("TOOLBOX:\t " + theMessage.toString());
        return (theMessage);
    }

    /**
     * Downloads the web page specified by the given <code>URL</code>
     * object.
     *
     * @param url The <code>URL</code> object that the page will be
     * downloaded from.
     *
     * @return A <code>String</code> containing the contents of the
     * page.  No extra parsing work is done on the page.  */
    public static String getWebPage(final URL url) {
        StringBuffer page = new StringBuffer();
        try {
            URLConnection connection = url.openConnection();
            String line;
            BufferedReader in;
            if (connection.getContentEncoding() == null) {
                in = new BufferedReader(new InputStreamReader(url.openStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(url.openStream(), connection.getContentEncoding()));
            }
            while ((line = in.readLine()) != null) {
                page.append(line).append('\n');
            }
            in.close();
        } catch (UnsupportedEncodingException e) {
            System.err.println("WebPage.getWebPage(): " + e);
        } catch (IOException e) {
            System.err.println("WebPage.getWebPage(): " + e);
        }
        logger.warn("TOOLBOX:\t " + page.toString());
        return page.toString();
    }

    /**
     * Retrieves a named string on a web page.
     * For example, if you have a PHP web page that says "Your IP address is:123.123.123.123"
     * calling getStringOnWebPage("Your IP address is:","www.yoursite.org/getIP.php") will give you your IP address.
     * The requested string should be followed by a BR html tag for this to work correctly.
     * @param paramStringName The text which is just before the string you want to retrieve.
     * @param paramWebPage The web page's address
     * @return The string, or null if the string could not be retrieved.
     */
    public static String getStringInWebPage(final String paramStringName, final String paramWebPage) {
        String result = null;
        int namePosition = paramWebPage.indexOf(paramStringName);
        if (-1 != namePosition) {
            int endOfStringPosition = paramWebPage.indexOf("<br>", namePosition + paramStringName.length());
            if (-1 != endOfStringPosition) {
                result = paramWebPage.substring(namePosition + paramStringName.length(), endOfStringPosition);
            } else {
                logger.fatal("Couldn't find end of string!");
            }
        }
        logger.warn("TOOLBOX:\t " + result);
        return (result);
    }

    /**
     * Displays a Peer advertisement on the log output.
     * @param thePeerAdvertisement The advertisement
     */
    public static void logPeerAdvertisement(final PeerAdvertisement thePeerAdvertisement) {
        logger.warn("PEER ADVERTISEMENT:");
        if (null != thePeerAdvertisement.getName()) {
            logger.warn("Name: \"" + thePeerAdvertisement.getName() + "\"");
        }
        if (null != thePeerAdvertisement.getPeerGroupID()) {
            logger.warn("PeerGroupID: " + thePeerAdvertisement.getPeerGroupID());
        }
        if (null != thePeerAdvertisement.getServiceParams()) {
            Hashtable theServiceParams = thePeerAdvertisement.getServiceParams();
            Enumeration theServiceParamsEnumeration = theServiceParams.elements();
            while (theServiceParamsEnumeration.hasMoreElements()) {
                Object theElement = theServiceParamsEnumeration.nextElement();
                logger.warn("ELEMENT:" + theElement.toString());
            }
        }
    }

    public static final long getDateFromDescription(final String paramDescription) {
        try {
            Long result = new Long(paramDescription.substring(0, 13));
            return (result);
        } catch (Exception theException) {
            return (0);
        }
    }

    public static final long currentTimeSegment(final long paramSegmentDurationInMillis) {
        return (System.currentTimeMillis() / paramSegmentDurationInMillis);
    }

    /**
     * Generates an SHA-1 digest hash of the string: clearTextID+"-"+function or: clearTextID if function was blank.<p>
     *
     * Note that the SHA-1 used only creates a 20 byte hash.<p>
     *
     * @param clearTextID A string that is to be hashed. This can be any string used for hashing or hiding data.
     * @param function A function related to the clearTextID string. This is used to create a hash associated with clearTextID so that it is a uique code.
     *
     * @return array of bytes containing the hash of the string: clearTextID+"-"+function or clearTextID if function was blank. Can return null if SHA-1 does not exist on platform.
     */
    public static final byte[] generateHash(final String clearTextID) {
        String id;
        id = clearTextID;
        byte[] buffer = id.getBytes();
        MessageDigest algorithm = null;
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            logger.fatal("Cannot load selected Digest Hash implementation", e);
            return null;
        }
        algorithm.reset();
        algorithm.update(buffer);
        try {
            byte[] digest1 = algorithm.digest();
            return digest1;
        } catch (Exception de) {
            logger.fatal("Failed to creat a digest.", de);
            return null;
        }
    }

    public static final net.jxta.peergroup.PeerGroupID createInfrastructurePeerGroupID(final String clearTextID) {
        logger.warn("Creating peer group ID =  clearText:'" + clearTextID);
        byte[] digest = generateHash(clearTextID);
        net.jxta.peergroup.PeerGroupID peerGroupID = IDFactory.newPeerGroupID(digest);
        return peerGroupID;
    }

    /**
     * For unit testiong only
     *
     * @param   args the command-line arguments. Ignored by this app.
     */
    public static void main(final String[] args) {
        System.out.println("Current time segment: " + currentTimeSegment(10000));
        StringToID("coucou");
        StringToID("a");
        StringToID("A very, very long string.........................");
        System.out.println("Infrastructure PeerGroupID: " + createInfrastructurePeerGroupID("net2map"));
        ModuleClassID aRandomModuleClassID = IDFactory.newModuleClassID();
        System.out.println("A random ModuleClassID: " + aRandomModuleClassID.toString());
        ModuleSpecID aRandomModuleSpecID = IDFactory.newModuleSpecID(aRandomModuleClassID);
        System.out.println("A random ModuleSpecID: " + aRandomModuleSpecID.toString());
        try {
            System.out.println("Retrieving Seed Nodes:\t " + getWebPage(new URL("http://www.net2map.org/SeedNodes.xml")));
        } catch (Exception theException) {
            System.out.println("TOOLBOX:\t Exception " + theException);
            theException.printStackTrace();
        }
        System.out.println("Current time segment: " + currentTimeSegment(10000));
    }
}
