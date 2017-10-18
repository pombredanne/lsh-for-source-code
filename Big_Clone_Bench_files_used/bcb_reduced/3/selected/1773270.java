package org.async.web;

import org.async.web.HttpServer.Controller;
import org.async.web.HttpServer.Actor;
import org.protocols.IRTD2;
import org.simple.Strings;

/**
 * ...
 * 
 * @pre Authority authorize = new Authority("localhost", "/");
 * authorize.salts = new byte[][]{'s', 'a', 'l', 't'};
 * authorize.timeout = 600;
 * authorize.identified(handler);
 * 
 */
public class Authority {

    public byte[][] salts = new byte[][] { Strings.random(10, Strings.ALPHANUMERIC).getBytes() };

    public int timeout = 600;

    private String _qualifier;

    public Authority() {
        _qualifier = "; expires=Sun, 17-Jan-2038 19:14:07 GMT";
    }

    public Authority(String domain, String path) {
        _qualifier = ("; expires=Sun, 17-Jan-2038 19:14:07 GMT; path=" + path + "; domain=" + domain);
    }

    public final void identify(Actor http, long time) {
        if (http.identity == null || http.identity.length() == 0) {
            http.identity = Strings.random(10, Strings.ALPHANUMERIC);
        }
        String[] irtd2 = new String[] { http.identity, http.rights, String.valueOf(time), http.digested };
        http.digest = IRTD2.digest(irtd2, salts[0]);
        StringBuilder sb = new StringBuilder();
        sb.append(irtd2[0]);
        sb.append(' ');
        sb.append(irtd2[1]);
        sb.append(' ');
        sb.append(irtd2[2]);
        sb.append(' ');
        if (irtd2[3] != null) {
            sb.append(irtd2[3]);
        }
        sb.append(' ');
        sb.append(http.digest);
        sb.append(_qualifier);
        http.setCookie("IRTD2", sb.toString());
    }

    public final String unidentify(Actor http) {
        http.setCookie("IRTD2", _qualifier);
        return http.identity;
    }

    protected static class Identified implements Controller {

        private HttpServer.Controller _wrapped;

        private Authority _authority;

        public Identified(Controller handler, Authority authority) {
            _wrapped = handler;
            _authority = authority;
        }

        public final boolean handleRequest(Actor http) throws Throwable {
            String irtd2 = http.getCookie("IRTD2");
            if (irtd2 != null) {
                String[] parsed = IRTD2.parse(irtd2);
                long time = http.when() / 1000;
                int error = IRTD2.digested(parsed, time, _authority.timeout, _authority.salts);
                if (error == 0) {
                    http.identity = parsed[0];
                    http.rights = parsed[1];
                    http.digested = parsed[4];
                    _authority.identify(http, time);
                    return _wrapped.handleRequest(http);
                } else {
                    http.channel().log("IRTD2 error " + error);
                }
            }
            http.error(401);
            return false;
        }

        public final void handleBody(Actor http) {
            throw new Error("unexpected call");
        }
    }

    public final Controller identified(Controller handler) {
        return new Identified(handler, this);
    }
}
