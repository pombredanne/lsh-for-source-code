package gg.de.sbmp3.common;

import gg.de.sbmp3.backend.BEFactory;
import gg.de.sbmp3.backend.ViewBE;
import gg.de.sbmp3.backend.data.PlaylistTreeBean;
import gg.de.sbmp3.backend.data.PlaylistTreeNodeBean;
import gg.de.sbmp3.backend.data.SelectOptionBean;
import gg.de.sbmp3.backend.data.UserBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created: 20.05.2004  19:35:10
 */
public class Util {

    private static Log log = LogFactory.getLog(Util.class);

    /**
	 * escapes potentially harmful characters in input
	 *
	 * @param str string to escape
	 * @return escaped string
	 */
    public static String sqlEscape(String str) {
        str = str.replaceAll("'", "\\\\'");
        str = str.replaceAll("\\\\", "\\\\\\\\");
        return str;
    }

    /**
	 * takes a map and converts it's keys into a comma seperated string.
	 * i.e. => "1,2,3,4,5"
	 *
	 * @param data
	 * @return comma seperated string of map key elements
	 */
    public static String convertToString(Map data) {
        StringBuffer buf = new StringBuffer();
        for (Iterator i = data.keySet().iterator(); i.hasNext(); ) {
            Object next = i.next();
            String nextStr;
            if (next instanceof String) nextStr = (String) next; else if (next instanceof Integer) nextStr = Integer.toString(((Integer) next).intValue()); else throw new RuntimeException("Invalid data type in Util.convertToString() - " + next.toString());
            buf.append(nextStr);
            if (i.hasNext()) buf.append(",");
        }
        return buf.toString();
    }

    /**
	 * simplifies a given string by applying all kinds of filters to it.<br>
	 * this is used to allow generic matches of names i.e. in GAACache.<br>
	 * <br>
	 * current filters:<br>
	 * <ul>
	 * <li>trim string</li>
	 * <li>convert to lowercase</li>
	 * <li>strip spaces</li>
	 * <li>strip underscores</li>
	 * </ul>
	 *
	 * @param data string to parse
	 * @return result string
	 */
    public static String simplifyString(String data) {
        data = data.toLowerCase();
        data = data.replaceAll("[^a-z0-9]+", "");
        return data;
    }

    /**
	 * removes unwanted special characters from the input string.<br>
	 * i.e. null bytes, leading/trailing spaces, ...
	 *
	 * @param data string to parse
	 * @return result string
	 */
    public static String cleanString(String data) {
        data = data.replaceAll("\0", "");
        data = data.trim();
        return data;
    }

    /**
	 * gets the directory part of a path
	 *
	 * @param data path string
	 * @return directory part of path with trailing /
	 */
    public static String getDirPart(String data) {
        return getDirPart(data.split("/"));
    }

    public static String getDirPart(String[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < (data.length - 1); i++) {
            buf.append(data[i]);
            buf.append("/");
        }
        return buf.toString();
    }

    public static String getFilePart(String data) {
        return getFilePart(data.split("/"));
    }

    public static String getFilePart(String[] data) {
        return data[data.length - 1];
    }

    /**
	 * builds a 32char, hex encoded MD5 digest
	 *
	 * @param data string to digest
	 * @return 32char hex representation of the digest
	 */
    public static String digestMD5(String data) {
        String MD5hex = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(data.getBytes());
            MD5hex = new BigInteger(1, digest).toString(16);
            while (MD5hex.length() < 32) MD5hex = "0" + MD5hex;
        } catch (NoSuchAlgorithmException e) {
            if (log.isErrorEnabled()) log.error("NoSuchAlgorithmException while creating MD5 digest", e);
        }
        return MD5hex;
    }

    /**
	 * shortens any given string to maxLength + 3 ("..." is added)
	 *
	 * @param data
	 * @param maxLength
	 * @return
	 */
    public static String forceMaxLength(String data, int maxLength) {
        if (data.length() <= maxLength) return data;
        return data.substring(0, maxLength - 1) + "...";
    }

    /**
	 * repeates the given string multiple times
	 *
	 * @param count number of repititions
	 * @param str string to repeat
	 * @return result string
	 */
    public static String repeat(int count, String str) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < count; i++) buf.append(str);
        return buf.toString();
    }

    public static List getPlaylistFolderDropdownList(UserBean user, boolean showOnlyPlaylists) {
        ViewBE viewBe = BEFactory.getViewBE(user);
        PlaylistTreeBean tree = viewBe.getPlaylistTree(true);
        List folders = new LinkedList();
        for (Iterator i = tree.getNodeList().iterator(); i.hasNext(); ) {
            PlaylistTreeNodeBean node = (PlaylistTreeNodeBean) i.next();
            folders.addAll(handleNode(node, showOnlyPlaylists, ""));
        }
        return folders;
    }

    private static List handleNode(PlaylistTreeNodeBean node, boolean showOnlyPlaylists, String pathPrefix) {
        List res = new LinkedList();
        if (!showOnlyPlaylists && node.isTypeDirectory()) {
            res.add(new SelectOptionBean((Util.repeat(node.getLevel(), "..") + node.getDirectory().getName()), node.getDirectory().getId()));
        }
        if (node.isTypeDirectory()) {
            if (node.hasChildren()) {
                for (Iterator i = node.getChildren().iterator(); i.hasNext(); ) {
                    PlaylistTreeNodeBean newNode = (PlaylistTreeNodeBean) i.next();
                    res.addAll(handleNode(newNode, showOnlyPlaylists, pathPrefix + "/" + node.getDirectory().getName()));
                }
            }
        }
        if (showOnlyPlaylists && node.isTypeFile()) {
            res.add(new SelectOptionBean((pathPrefix + "/" + node.getFile().getName()), node.getFile().getId()));
        }
        return res;
    }

    public static String date2String(Date d) {
        if (d == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(d);
    }

    public static Date string2Date(String s) {
        if (s == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        try {
            return sdf.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }
}
