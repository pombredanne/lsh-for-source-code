package com.frostwire.updates;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Date;
import com.limegroup.gnutella.security.SHA1;
import com.limegroup.gnutella.settings.ApplicationSettings;

/** POJO to represent an UpdateMessage. */
public final class UpdateMessage extends Object implements Serializable {

    private int _hashCode = -1;

    public static final long serialVersionUID = 44L;

    private String _message;

    private String _messageInstallerReady;

    private String _url;

    private String _messageType;

    private String _version = null;

    private Date _expiration = null;

    private String _torrent = null;

    private String _os = null;

    private String _showOnce = "false";

    private String _src = "";

    private boolean _intro = false;

    private String _lang = "en";

    private String _md5 = "";

    public String getMessage() {
        return _message;
    }

    public void setMessage(String m) {
        _message = m;
    }

    public String getMessageInstallerReady() {
        return _messageInstallerReady;
    }

    public void setMessageInstallerReady(String m) {
        _messageInstallerReady = m;
    }

    public String getUrl() {
        return _url;
    }

    public void setUrl(String u) {
        _url = u;
    }

    public String getSrc() {
        return _src;
    }

    public void setSrc(String src) {
        _src = src;
    }

    public void setIntro(boolean intro) {
        _intro = intro;
    }

    public boolean isIntro() {
        return _intro;
    }

    public String getLanguage() {
        return _lang;
    }

    public void setLanguage(String lang) {
        _lang = lang;
    }

    public String getRemoteMD5() {
        return _md5;
    }

    public void setRemoteMD5(String md5) {
        _md5 = md5.toUpperCase();
    }

    public String getMessageType() {
        return _messageType;
    }

    public String getOs() {
        return _os;
    }

    /**
     * If it receives a valid os string ("windows", "mac", "linux")
     * it will set it.
     * If it receives null or *, it will set _os to null.
     * Having getOS() return null, means this message is for every OS instance.
     * @param os
     */
    public void setOs(String os) {
        _os = null;
        if (os != null) {
            os = os.trim();
            if (os.equalsIgnoreCase("windows") || os.equalsIgnoreCase("linux") || os.equalsIgnoreCase("mac")) {
                _os = os.toLowerCase();
            } else if (os.equals("*")) {
                _os = null;
            }
        }
    }

    public String getTorrent() {
        return _torrent;
    }

    public void setTorrent(String t) {
        _torrent = t;
    }

    public void setMessageType(String mt) {
        String type = mt.toLowerCase().trim();
        boolean typeIsValid = (type.equals("update") || type.equals("announcement") || type.equals("overlay") || type.equals("hostiles") || type.equals("chat_server"));
        if (mt == null || !typeIsValid) {
            _messageType = new String("update");
            return;
        }
        _messageType = mt.toLowerCase();
    }

    public Date getExpiration() {
        return _expiration;
    }

    public void setExpiration(Date exp) {
        _expiration = exp;
    }

    /** Sets the expiration date out of a string with the timestamp 
     * Pass null, and it means this message has no expiration date.
     * */
    public void setExpiration(String expTimestamp) {
        if (expTimestamp == null || expTimestamp.equals("0")) {
            _expiration = null;
            return;
        }
        try {
            _expiration = new Date(Long.parseLong(expTimestamp));
        } catch (NumberFormatException e) {
            System.out.println("Expiration passed cannot be converted to a long");
            _expiration = null;
        }
    }

    public boolean hasExpired() {
        if (getExpiration() == null) return false;
        long serverTimestamp = UpdateManager.getInstance().getServerTime().getTime();
        long myTimestamp = _expiration.getTime();
        return myTimestamp < serverTimestamp;
    }

    public String getVersion() {
        if (_version != null && _version.equals("")) _version = null;
        return _version;
    }

    public void setVersion(String v) {
        _version = v;
    }

    public boolean isShownOnce() {
        return _showOnce.equalsIgnoreCase("true");
    }

    public void setShowOnce(String s) {
        if (s != null) _showOnce = s;
    }

    public UpdateMessage() {
    }

    public UpdateMessage(String msgType, String message) {
        setMessageType(msgType);
        setMessage(message);
    }

    public static final UpdateMessage createOverlayMessage(String imageSrc, String linkUrl, boolean isIntro, String imageMD5, String torrentURL) {
        UpdateMessage overlay = new UpdateMessage();
        overlay.setSrc(imageSrc);
        overlay.setUrl(linkUrl);
        overlay.setIntro(isIntro);
        overlay.setRemoteMD5(imageMD5);
        overlay.setTorrent(torrentURL);
        return overlay;
    }

    public boolean equals(Object obj) {
        return obj.hashCode() == this.hashCode() && isIntro() == ((UpdateMessage) obj).isIntro();
    }

    public int hashCode() {
        if (_hashCode <= 0) {
            MessageDigest md = new SHA1();
            String byteString = _message + _url + _messageType + _version + _torrent + _os + _showOnce;
            md.update(byteString.getBytes());
            byte[] digest = md.digest();
            _hashCode = 0;
            for (int n : digest) {
                _hashCode += Math.abs((int) n);
            }
        }
        return _hashCode;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("\n");
        s.append("UpdateMessage @" + String.valueOf(super.hashCode()));
        s.append("{");
        s.append("_hashCode : " + String.valueOf(hashCode()) + ", \n");
        s.append("_message : " + getMessage() + ", \n");
        s.append("_url : " + getUrl() + ", \n");
        s.append("_messageType : " + getMessageType() + ", \n");
        s.append("_version : " + getVersion() + ", \n");
        s.append("_expiration : " + String.valueOf(getExpiration()) + ", \n");
        s.append("_torrent : " + getTorrent() + ", \n");
        s.append("_os : " + getOs() + ", \n");
        s.append("_language : " + getLanguage() + ", \n");
        s.append("_applanguage : " + ApplicationSettings.getLanguage() + ", \n");
        s.append("_showOnce : " + isShownOnce() + ", \n");
        s.append("_isIntro : " + isIntro() + ", \n");
        s.append("_md5 : " + getRemoteMD5() + ", \n");
        s.append("}\n");
        return s.toString();
    }
}
