package sunlabs.brazil.sunlabs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.Base64;

/**
 * Compute the Base64 encoded SHA1 digest of a value
 * (so I don't have to store plain text
 * passwords).  This should probably be added to the Calculator, but
 * this is easier.
 * <br><code>
 * &lt;digest name=nnn value=vvv&gt;
 * </code>
 *
 * @author      Stephen Uhler
 * @version		2.2
 */
public class DigestTemplate extends Template {

    MessageDigest digest = null;

    public DigestTemplate() {
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public void tag_digest(RewriteContext hr) {
        String name = hr.get("name");
        String value = hr.get("value");
        debug(hr);
        hr.killToken();
        if (digest != null && name != null && value != null) {
            hr.request.props.put(name, Base64.encode(digest.digest(value.getBytes())));
        } else {
            debug(hr, "Invalid parameters or no digest available");
        }
    }
}
