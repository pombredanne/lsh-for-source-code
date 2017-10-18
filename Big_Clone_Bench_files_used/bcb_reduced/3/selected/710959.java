package sunlabs.brazil.handler;

import java.io.IOException;
import java.util.Vector;
import java.util.Properties;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Base64;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.StringMap;
import sunlabs.brazil.util.regexp.Regexp;

/**
 * Handler for creating browser sessions based
 * on information found in the http request.
 * This handler provides a single session-id that may be used by
 * other handlers.
 * <p>
 * The following server properties are used:
 * <dl class=props>
 * <dt><code>prefix, suffix, glob, match</code>
 * <dd>Specify the URL that triggers this handler
 * (See {@link MatchString}).
 * <dt> <code>session</code>
 * <dd> The name of the request property that the Session ID will be stored
 *	in, to be passed to downstream handlers.  The default value is
 *	"SessionID".  If the property already exists, and is not empty, 
 *      no session will be defined (unless force=true).
 * <dt> <code>extract</code>
 * <dd> If specified, a string to use as the session-id.  ${...} values will
 *      be searched for first in the HTTP header values, and then
 *	in the request properties.
 *      <p>
 *      In addition to the actual HTTP headers, 
 *      the pseudo http headers <code>ipaddress, url, method, and query</code>
 *	are made available for ${...} substitutions.
 * <dt> <code>re</code>
 * <dd> If specified, a regular expression that the extracted data must match.
 *      if it doesn't match, no session id is installed.
 *      The default is ".", which matches any non-empty string.
 *	If the first character is "!" then the sense of the match is inverted,
 *	But only for determining whether a match "succeeded" or not.
 *	no sub-matches may be used in computing the key value in this case.
 * <dt> <code>value</code>
 * <dd> The value of the session ID.  May contain &amp; or \n (n=0,1,2...) 
 *	constructs to substitute
 *	matched sub-expressions of <code>re</code>.  The default is "&amp;" , which
 *	uses the entire string  "extract" as the <code>session</code> id.
 *	${...} are substituted (but not \'s) for <code>value</code> before
 *	looking
 *	for '\n' sequences that are part of the regular expression matches.
 * <dt> <code>digest</code>
 * <dd> If set, the "value" is replaced by the base64 encoding of the
 *	MD5 checksum of <code>value</code>.
 * <dt> <code>force</code>
 * <dd> If set (to anything), a session ID is set even if one already 
 * 	exists.
 * </dl>
 * If no options are provided, the client's IP address is used as the
 * session ID.
 * <p>
 * Examples:
 * <dl>
 * <dt>Pick the session based on the browser
 * <dd><pre>
 * [prefix].extract=${user-agent}
 * [prefix].re=.*(Netscape|Lynx|MSIE).*
 * [prefix].value=\\1
 * </pre>
 * <dt>This is similar to the "old" behavior.
 * <dd><pre>
 * [prefix].extract=${user-agent}${ipaddress}
 * [prefix].digest=true
 * </pre>
 * <dt>Look for a special authorization token, and set a request property
 *     to the value
 * <dd><pre>
 * [prefix].extract=${Authorization}
 * [prefix].re=code:([0-9]+)
 * [prefix].value=id\\1
 * </pre>
 * </dl> 
 *
 * @author		Stephen Uhler
 * @version		@(#)SimpleSessionHandler.java	2.3
 */
public class SimpleSessionHandler implements Handler {

    public String value;

    public Regexp regexp;

    MatchString isMine;

    String session;

    String extract;

    boolean force;

    boolean invert = false;

    MessageDigest digest = null;

    public boolean init(Server server, String prefix) {
        isMine = new MatchString(prefix, server.props);
        session = server.props.getProperty(prefix + "session", "SessionID");
        extract = server.props.getProperty(prefix + "extract", "${ipaddress}");
        force = (server.props.getProperty(prefix + "force") != null);
        value = server.props.getProperty(prefix + "value", "&");
        if (server.props.getProperty(prefix + "digest") != null) {
            try {
                digest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                server.log(Server.LOG_WARNING, prefix, "Can't find SHA implementation");
                digest = null;
            }
        }
        String match = server.props.getProperty(prefix + "re", ".+");
        if (match.startsWith("!")) {
            match = match.substring(1);
            invert = true;
        }
        try {
            regexp = new Regexp(match);
        } catch (Exception e) {
            regexp = null;
            server.log(Server.LOG_WARNING, prefix, "Bad expression:" + e);
            return false;
        }
        return true;
    }

    public boolean respond(Request request) throws IOException {
        if (!isMine.match(request.url)) {
            return false;
        }
        String current = request.props.getProperty(session);
        if (!force && current != null && !current.equals("")) {
            request.log(Server.LOG_INFORMATIONAL, isMine.prefix(), session + " already exists, skipping");
            return false;
        }
        Props props = new Props(request.headers, request.props);
        props.extra("ipaddress", request.getSocket().getInetAddress().getHostAddress());
        props.extra("url", request.url);
        props.extra("query", request.query);
        props.extra("method", request.method);
        String key = Format.subst(props, extract);
        String id;
        value = Format.subst(request.props, value, true);
        if (invert) {
            if (regexp.match(key) == null) {
                id = value;
            } else {
                id = null;
            }
        } else {
            id = regexp.sub(key, value);
        }
        if (id == null) {
            request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "(" + key + ") doesn't match re, not set");
            return false;
        }
        if (digest != null) {
            digest.reset();
            id = Base64.encode(digest.digest(id.getBytes()));
        }
        request.props.put(session, id);
        request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "Using (" + id + ") as session id");
        return false;
    }

    /**
     * Add a few name/value pairs in front of a dictionary so Format.subst()
     * will find them when it calls get().
     * Sigh!
     */
    static class Props extends StringMap {

        StringMap map;

        Properties defaults;

        Vector extra;

        Props(StringMap map, Properties defaults) {
            this.map = map;
            this.defaults = defaults;
            extra = new Vector();
        }

        void extra(Object name, Object value) {
            extra.addElement(name);
            extra.addElement(value);
        }

        public String get(String key) {
            for (int i = 0; i < extra.size(); i += 2) {
                if (key.equals(extra.elementAt(i))) {
                    return (String) extra.elementAt(i + 1);
                }
            }
            String result = map.get(key);
            if (result == null && defaults != null) {
                result = defaults.getProperty(key);
            }
            return result;
        }
    }
}
