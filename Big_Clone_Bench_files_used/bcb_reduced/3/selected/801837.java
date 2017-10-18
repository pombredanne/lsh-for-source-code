package sunlabs.brazil.filter;

import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Base64;
import sunlabs.brazil.util.http.MimeHeaders;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sunlabs.brazil.handler.MatchString;

/**
 * Filter to compute the MD5 checksum of the content, and
 * generate the appropriate "Content-MD5" http header.
 * As md5 checksum generation can be expensive, care should be
 * taken as to which types of content are digested.
 * <p>
 * The following server properties are used:
 * <dl class=props>
 * <dt>prefix, suffix, glob, match
 * <dd>Specify the URLs that trigger this filter
 * (See {@link sunlabs.brazil.handler.MatchString}).
 * </dl>
 *
 * @author		Stephen Uhler
 * @version		2.2
 */
public class MD5Filter implements Filter {

    MatchString isMine;

    /**
     * Make sure MD5 is available in this VM, or don't start.
     */
    public boolean init(Server server, String prefix) {
        isMine = new MatchString(prefix, server.props);
        try {
            MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            server.log(Server.LOG_WARNING, prefix, "Can't find MD5 implementation");
            return false;
        }
        return true;
    }

    /**
     * This is the request object before the content was fetched.
     */
    public boolean respond(Request request) {
        return false;
    }

    /**
     * Only filter url's that match.
     */
    public boolean shouldFilter(Request request, MimeHeaders headers) {
        return (isMine.match(request.url));
    }

    /**
     * Compute digest, add to header.
     */
    public byte[] filter(Request request, MimeHeaders headers, byte[] content) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            String md5 = Base64.encode(digest.digest(content));
            request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "Digest for " + request.url + " " + md5);
            request.addHeader("Content-MD5", md5);
        } catch (NoSuchAlgorithmException e) {
        }
        return content;
    }
}
