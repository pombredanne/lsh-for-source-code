package mains;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Map;
import com.googlecode.torrent4j.bencoding.BEDecoder;
import com.googlecode.torrent4j.bencoding.BEEncoder;
import com.googlecode.torrent4j.bencoding.BEValue;

public class ReadTorrent {

    private static String hexencode(byte[] bs) {
        StringBuffer sb = new StringBuffer(bs.length * 2);
        for (byte element : bs) {
            int c = element & 0xFF;
            if (c < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(c));
        }
        return sb.toString();
    }

    /**
	 * @param args
	 * @throws Exception 
	 */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new Exception("Falta archivo");
        String path = args[0];
        InputStream stream = null;
        try {
            File torrentFile = new File(path);
            stream = new FileInputStream(torrentFile);
            BEDecoder decoder = new BEDecoder(stream);
            BEValue torrent = decoder.decode();
            Map<String, BEValue> metainfo = torrent.mapValue();
            BEValue info = metainfo.get("info");
            MessageDigest digest = MessageDigest.getInstance("SHA");
            byte[] infohash = digest.digest(new BEEncoder().encode(info));
            System.out.println("FILENAME: " + torrentFile.getName());
            System.out.println(metainfo);
            System.out.println("INFOHASH: " + hexencode(infohash));
        } finally {
            if (stream != null) stream.close();
        }
    }
}
