package repokeep.dataitems;

import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import org.w3c.dom.*;

/**
 *
 * @author JLA
 */
public class RKPackage implements java.io.Serializable {

    private static final long serialVersionUID = -4912371530886820780L;

    public static final String KEY_BUNDLE_ID = "bundleIdentifier";

    public static final String KEY_CATEGORY = "category";

    public static final String KEY_NAME = "name";

    public static final String KEY_DESCRIPTION = "description";

    public static final String KEY_VERSION = "version";

    public static final String KEY_LOCATION = "location";

    public static final String KEY_HASH = "hash";

    public static final String KEY_MOREINFO_URL = "url";

    public static final String KEY_SCRIPTS = "scripts";

    public static final String KEY_DATE = "date";

    public static final String KEY_SPONSOR = "sponsor";

    public static final String KEY_SIZE = "size";

    public static final String KEY_RESTARTSPRINGBOARD = "RestartSpringBoard";

    public static final String KEY_SCRIPT_INSTALL = "install";

    public static final String KEY_SCRIPT_UNINSTALL = "uninstall";

    public static final String KEY_SCRIPT_UPDATE = "update";

    public static final String KEY_SCRIPT_PREFLIGHT = "preflight";

    public static final String KEY_SCRIPT_POSTFLIGHT = "postflight";

    private File myLocalFile = null;

    private boolean myRestartSpringBoard = true;

    private java.util.Map<Object, String> myValueMap = new java.util.HashMap<Object, String>();

    private java.util.Map<String, RKScript> myScriptMap = new java.util.HashMap<String, RKScript>();

    public RKPackage() {
        myValueMap.put(KEY_NAME, "(name)");
        long zCurrentDate = System.currentTimeMillis() / 1000;
        this.setDate(zCurrentDate);
        myScriptMap.put(KEY_SCRIPT_INSTALL, new RKScript(KEY_SCRIPT_INSTALL));
        myScriptMap.put(KEY_SCRIPT_UNINSTALL, new RKScript(KEY_SCRIPT_UNINSTALL));
        myScriptMap.put(KEY_SCRIPT_UPDATE, new RKScript(KEY_SCRIPT_UPDATE));
        myScriptMap.put(KEY_SCRIPT_PREFLIGHT, new RKScript(KEY_SCRIPT_PREFLIGHT));
        myScriptMap.put(KEY_SCRIPT_POSTFLIGHT, new RKScript(KEY_SCRIPT_POSTFLIGHT));
    }

    public RKPackage(Node aNode) {
        this();
        java.util.List<RepoKeepXMLUtil.InstallerKeyValuePair> zKVList = RepoKeepXMLUtil.getChildInstallerKeyValueList(aNode);
        for (RepoKeepXMLUtil.InstallerKeyValuePair zKVPair : zKVList) {
            importKeyValuePair(zKVPair);
        }
    }

    public java.util.Map<String, RKScript> getScriptMap() {
        return myScriptMap;
    }

    public java.util.Map<Object, String> getValueMap() {
        return myValueMap;
    }

    public String getPackageName() {
        return myValueMap.get(KEY_NAME);
    }

    public void setPackageName(String PackageName) {
        myValueMap.put(KEY_NAME, PackageName);
    }

    public long getPackageSize() {
        long zReturn = 0;
        String zSizeString = myValueMap.get(KEY_SIZE);
        if (zSizeString != null) {
            try {
                zReturn = Long.parseLong(zSizeString);
            } catch (NumberFormatException e) {
                zReturn = 0;
            }
        }
        return zReturn;
    }

    public void setPackageSize(long PackageSize) {
        getValueMap().put(KEY_SIZE, "" + PackageSize);
    }

    public String getMD5() {
        return myValueMap.get(KEY_HASH);
    }

    public void setMD5(String MD5) {
        myValueMap.put(KEY_HASH, MD5);
    }

    public long getDate() {
        String zDateString = myValueMap.get(KEY_DATE);
        long zReturn = 0;
        try {
            zReturn = Long.parseLong(zDateString);
        } catch (NumberFormatException e) {
            zReturn = 0;
        }
        return zReturn;
    }

    public void setDate(long Date) {
        String zDateString = "" + Date;
        myValueMap.put(KEY_DATE, zDateString);
    }

    /**
 * This returns the local file, which is a file that the user has specified from
 * which this application calculates the MD5, etc.
 * @return
 */
    public File getLocalFile() {
        return myLocalFile;
    }

    public void setLocalFile(File LocalFile) {
        this.myLocalFile = LocalFile;
    }

    public String toString() {
        return getPackageName();
    }

    public static String calculateMD5(byte[] aData) {
        String zReturn = "";
        try {
            java.security.MessageDigest zMD = java.security.MessageDigest.getInstance("MD5");
            zMD.update(aData);
            byte[] zSum = zMD.digest();
            java.math.BigInteger zBig = new java.math.BigInteger(1, zSum);
            zReturn = zBig.toString(16);
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return zReturn;
    }

    /**
      * This method is used to import key-value pairs when parsing an Installer XML
      * @param aKVPair
      */
    public void importKeyValuePair(RepoKeepXMLUtil.InstallerKeyValuePair aKVPair) {
        String zKey = aKVPair.getKey();
        if (aKVPair.getValueElement().getTagName().equalsIgnoreCase("string")) {
            getValueMap().put(zKey, aKVPair.getValueElement().getValue());
        } else {
            if (zKey.equals(KEY_SCRIPTS)) {
                java.util.List<RepoKeepXMLUtil.InstallerKeyValuePair> zKVList = RepoKeepXMLUtil.getChildInstallerKeyValueList(aKVPair.getValueNode());
                int zSubKeyCount = zKVList.size();
                for (int i = 0; i < zSubKeyCount; i++) {
                    RepoKeepXMLUtil.InstallerKeyValuePair zSubKVPair = zKVList.get(i);
                    String zScriptName = zSubKVPair.getKey();
                    RKScript zScript = new RKScript(zScriptName, zSubKVPair.getValueNode());
                    myScriptMap.put(zScriptName, zScript);
                }
            } else if (zKey.equalsIgnoreCase(KEY_RESTARTSPRINGBOARD)) {
                if (aKVPair.getValueElement().getTagName().equalsIgnoreCase("false")) {
                    myRestartSpringBoard = false;
                } else myRestartSpringBoard = true;
            }
        }
    }

    /**
     * This returns the object as an Installer XML string.  
     * @return the installer XML string
     */
    public String toXMLString() {
        StringBuffer zSB = new StringBuffer();
        zSB.append("\t\t<dict>\n");
        zSB.append("\t\t\t<key>" + KEY_RESTARTSPRINGBOARD + "</key>\n");
        if (myRestartSpringBoard == false) zSB.append("\t\t\t<false/>\n"); else zSB.append("\t\t\t<true/>\n");
        java.util.List zKeyList = new java.util.ArrayList(myValueMap.keySet());
        for (Object zKey : zKeyList) {
            zSB.append("\t\t\t<key>" + zKey + "</key>\n\t\t\t<string>" + RepoKeepXMLUtil.escapeHTML(myValueMap.get(zKey)) + "</string>\n");
        }
        zSB.append("\t\t\t<key>scripts</key>\n\t\t\t<dict>\n");
        java.util.List<RKScript> zScriptList = new java.util.ArrayList<RKScript>(myScriptMap.values());
        for (RKScript zScript : zScriptList) {
            zSB.append(zScript.toXMLString());
        }
        zSB.append("\t\t\t</dict>\n\t\t</dict>\n");
        return zSB.toString();
    }
}
