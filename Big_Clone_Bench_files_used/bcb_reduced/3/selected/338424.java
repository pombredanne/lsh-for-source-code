package org.jtorrent.torrent;

import org.jtorrent.torrent.type.TorrentType;
import org.jtorrent.torrent.type.TorrentFileInfo;
import org.jtorrent.Constants;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Ryan Thomas
 * r.n.thomas@gmail.com
 *
 * @author ryan
 * @version $Revision$ $Author$ $Date$
 * @since Jun 20, 2009
 */
public class TorrentData {

    private TorrentType<Map<String, TorrentType<?>>> data;

    private byte[] infoPacket;

    public TorrentData(final TorrentType<Map<String, TorrentType<?>>> data, byte[] infoPacket) {
        this.data = data;
        this.infoPacket = infoPacket;
    }

    public byte[] getInfoPacket() {
        return infoPacket;
    }

    public byte[] computeInfoHash() throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        return sha1.digest(infoPacket);
    }

    public boolean isMultiFile() {
        return data.getPayload().containsKey(Constants.KEY_INFO) && ((Map) data.getPayload().get(Constants.KEY_INFO).getPayload()).containsKey(Constants.KEY_INFO_FILES);
    }

    public String getAnnounceUrl() {
        return data.getPayload().get(Constants.KEY_ANNOUNCE).toString();
    }

    public String getComment() {
        return data.getPayload().get(Constants.KEY_COMMENT).toString();
    }

    public long getCreationDate() {
        return Long.valueOf(data.getPayload().get(Constants.KEY_CREATION_DATE).toString() + "000");
    }

    public int getPieceLength() {
        return Integer.valueOf(((Map) data.getPayload().get(Constants.KEY_INFO).getPayload()).get(Constants.KEY_INFO_PIECE_LENGTH).toString());
    }

    public String[] getPieces() {
        int hashLength = 20;
        String pieceStr = ((Map) data.getPayload().get(Constants.KEY_INFO).getPayload()).get(Constants.KEY_INFO_PIECES).toString();
        int pieceCount = pieceStr.length() / hashLength;
        String[] pieces = new String[pieceCount];
        for (int a = 0; a < pieceCount; a++) {
            pieces[a] = pieceStr.substring(a * hashLength, (a + 1) * hashLength);
        }
        return pieces;
    }

    public String getName() {
        return ((Map) data.getPayload().get(Constants.KEY_INFO).getPayload()).get(Constants.KEY_INFO_NAME).toString();
    }

    public int getLength() {
        if (isMultiFile()) {
            return -1;
        } else {
            return Integer.parseInt(((Map) data.getPayload().get(Constants.KEY_INFO).getPayload()).get(Constants.KEY_INFO_LENGTH).toString());
        }
    }

    public List<TorrentFileInfo> getFiles() {
        List<TorrentFileInfo> files = new ArrayList<TorrentFileInfo>();
        if (isMultiFile()) {
            List fileList = (List) ((TorrentType) ((Map) data.getPayload().get(Constants.KEY_INFO).getPayload()).get(Constants.KEY_INFO_FILES)).getPayload();
            for (Object aFileList : fileList) {
                TorrentType fileEntry = (TorrentType) aFileList;
                int length = Integer.parseInt(((Map) fileEntry.getPayload()).get(Constants.KEY_INFO_LENGTH).toString());
                String md5sum = null;
                if (((Map) fileEntry.getPayload()).containsKey(Constants.KEY_INFO_MD5SUM)) {
                    md5sum = ((Map) fileEntry.getPayload()).get(Constants.KEY_INFO_MD5SUM).toString();
                }
                StringBuffer path = new StringBuffer();
                for (Object obj : ((List) ((TorrentType) ((Map) fileEntry.getPayload()).get(Constants.KEY_INFO_PATH)).getPayload())) {
                    path.append(File.separator).append(obj);
                }
                files.add(new TorrentFileInfo(length, md5sum, path.toString()));
            }
        } else {
            files.add(new TorrentFileInfo(getLength(), getMd5sum(), getName()));
        }
        return files;
    }

    public String getMd5sum() {
        if (!isMultiFile()) {
            if (((Map) data.getPayload().get(Constants.KEY_INFO).getPayload()).containsKey(Constants.KEY_INFO_MD5SUM)) {
                return ((Map) data.getPayload().get(Constants.KEY_INFO).getPayload()).get(Constants.KEY_INFO_MD5SUM).toString();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(Constants.KEY_COMMENT).append(": ").append(getComment()).append("\n");
        buf.append(Constants.KEY_ANNOUNCE).append(": ").append(getAnnounceUrl()).append("\n");
        buf.append(Constants.KEY_CREATION_DATE).append(": ").append(new Date(getCreationDate())).append("\n");
        buf.append(Constants.KEY_INFO_PIECE_LENGTH).append(": ").append(getPieceLength()).append("\n");
        buf.append(Constants.KEY_INFO_PIECES).append(": (").append(getPieces().length).append(" pieces)\n");
        if (isMultiFile()) {
            buf.insert(0, "MULTI FILE TORRENT\n");
            buf.append(Constants.KEY_INFO_NAME).append(": ").append(getName()).append("\n");
            for (TorrentFileInfo info : getFiles()) {
                buf.append("\t").append(Constants.KEY_INFO_PATH).append(": ").append(info.getPath()).append("\n");
                buf.append("\t").append(Constants.KEY_INFO_LENGTH).append(": ").append(info.getLength()).append("\n");
                buf.append("\t").append(Constants.KEY_INFO_MD5SUM).append(": ").append(info.getMd5sum()).append("\n");
            }
        } else {
            buf.insert(0, "SINGLE FILE TORRENT\n");
            buf.append(Constants.KEY_INFO_NAME).append(": ").append(getName()).append("\n");
            buf.append(Constants.KEY_INFO_LENGTH).append(": ").append(getLength()).append("\n");
            buf.append(Constants.KEY_INFO_MD5SUM).append(": ").append(getMd5sum()).append("\n");
        }
        return buf.toString();
    }
}
