package org.apache.nutch.crawl;

import org.apache.hadoop.io.MD5Hash;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.protocol.Content;

/**
 * Default implementation of a page signature. It calculates an MD5 hash
 * of the raw binary content of a page. In case there is no content, it
 * calculates a hash from the page's URL.
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
public class MD5Signature extends Signature {

    public byte[] calculate(Content content, Parse parse) {
        byte[] data = content.getContent();
        if (data == null) data = content.getUrl().getBytes();
        return MD5Hash.digest(data).getDigest();
    }
}
