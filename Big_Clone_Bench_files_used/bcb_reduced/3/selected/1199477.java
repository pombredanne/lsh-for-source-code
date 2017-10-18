package n2hell.torrent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;

public class Torrent {

    private final Map torrentMap;

    private String hash = null;

    public Torrent(File torrent) throws IOException {
        FileInputStream fs = null;
        BufferedInputStream is = null;
        try {
            fs = new FileInputStream(torrent);
            is = new BufferedInputStream(fs);
            torrentMap = BDecoder.decode(is);
        } finally {
            if (fs != null) fs.close();
            if (is != null) is.close();
        }
    }

    public Torrent(byte[] torrent) throws IOException {
        torrentMap = BDecoder.decode(torrent);
    }

    public static String bin2hex(byte[] digestBits) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 20; i++) {
            char c1, c2;
            c1 = (char) ((digestBits[i] >>> 4) & 0xf);
            c2 = (char) (digestBits[i] & 0xf);
            c1 = (char) ((c1 > 9) ? 'a' + (c1 - 10) : '0' + c1);
            c2 = (char) ((c2 > 9) ? 'a' + (c2 - 10) : '0' + c2);
            sb.append(c1);
            sb.append(c2);
        }
        return sb.toString();
    }

    /**
	 * @return the hash
	 * @throws NoSuchAlgorithmException 
	 * @throws IOException 
	 */
    public String getHash() throws NoSuchAlgorithmException, IOException {
        if (hash == null) {
            Map info = (Map) torrentMap.get("info");
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            hash = Torrent.bin2hex(md.digest(BEncoder.encode(info))).toUpperCase();
        }
        return hash;
    }
}
