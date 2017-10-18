package org.ugue.bittorrent.utils.bencoding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.ugue.bittorrent.utils.SHA1;
import org.ugue.bittorrent.utils.bencoding.types.*;

/**
 * This class will generate a SHA1 byte vector from a BEncType. The utility of
 * this class is to generate for example the SHA1 from the 'info' field to be
 * used as info_hash in BitTorrent
 * 
 * @author fpreto
 *
 */
public class BEncSHA1 {

    private MessageDigest md;

    public BEncSHA1() {
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("This class requires that MessageDigest has the SHA-1 algorithm");
        }
    }

    public SHA1 digest(BEncType o) {
        md.reset();
        encode(o);
        return new SHA1(md.digest());
    }

    private void encode(BEncType o) {
        if (o instanceof BEncInteger) {
            encode((BEncInteger) o);
        } else if (o instanceof BEncString) {
            encode((BEncString) o);
        } else if (o instanceof BEncList) {
            encode((BEncList) o);
        } else if (o instanceof BEncDictionary) {
            encode((BEncDictionary) o);
        }
    }

    private void encode(BEncInteger i) {
        String s = "i" + i.toString() + "e";
        md.update(s.getBytes());
    }

    private void encode(BEncString s) {
        String str = Long.toString(s.length()) + ":";
        md.update(str.getBytes());
        md.update(s.getBytes());
    }

    private void encode(BEncList l) {
        md.update("l".getBytes());
        for (BEncType o : l) {
            encode(o);
        }
        md.update("e".getBytes());
    }

    private void encode(BEncDictionary d) {
        md.update("d".getBytes());
        Set<BEncString> keys = d.keySet();
        List<BEncString> keylist = new ArrayList<BEncString>(keys);
        Collections.sort(keylist);
        for (BEncString key : keylist) {
            encode(key);
            encode(d.get(key));
        }
        md.update("e".getBytes());
    }
}
