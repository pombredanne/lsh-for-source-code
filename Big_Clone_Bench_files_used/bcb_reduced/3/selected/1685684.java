package onepoint.project.modules.external_applications.MindMeister;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import onepoint.project.modules.external_applications.OpWebApplicationConnection;
import onepoint.project.modules.external_applications.MindMeister.generated.Rsp;
import onepoint.project.modules.external_applications.exceptions.OpExternalApplicationException;
import onepoint.util.Pair;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.util.EncodingUtil;

public class OpMindMeisterConnection extends OpWebApplicationConnection {

    private static final String METHOD = "method";

    private static final String METHOD_GET_FROB = "mm.auth.getFrob";

    private static final String METHOD_GET_TOKEN = "mm.auth.getToken";

    private static final String METHOD_CHECK_TOKEN = "mm.auth.checkToken";

    private static final String METHOD_GET_MAPS = "mm.maps.getList";

    private static final String METHOD_GET_MAP = "mm.maps.getMap";

    private static final String API_SIG = "api_sig";

    private static final String API_KEY = "api_key";

    private static final String FROB = "frob";

    private static final String PERMS = "perms";

    private static final String AUTH_TOKEN = "auth_token";

    private static final String MAP_ID = "map_id";

    private static final String DEFAULT_PERMS = "write";

    private static final String ATTACHMENT_ID = "id";

    private static final String STAT_FAIL = "fail";

    private static final String STAT_OK = "ok";

    private static final String URL_QUERY_DELIMITER = "?";

    private static final String URL_PARAMETER_DELIMITER = "&";

    private static final String URL_VALUE_DELIMITER = "=";

    private static final String JAXB_CLASS_PACKAGE = "onepoint.project.modules.external_applications.MindMeister.generated";

    private static final String CALLBACK_URL = "callback_url";

    private static final int MINDMEISTER_HTTP_TIMEOUT = 60000;

    private String url;

    private String attachmentUrl;

    private String authUrl;

    private String apiKey;

    private String secret;

    private String token;

    public OpMindMeisterConnection(String url, String attachmentUrl, String authUrl, String apiKey, String secret, String token) {
        this.url = url;
        this.attachmentUrl = attachmentUrl;
        this.authUrl = authUrl;
        this.apiKey = apiKey;
        this.secret = secret;
        this.token = token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getFrob() throws OpExternalApplicationException {
        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        Rsp rsp = sendRequest(METHOD_GET_FROB, parameters);
        if (STAT_FAIL.equals(rsp.getStat())) {
            throw new OpExternalApplicationException(OpExternalApplicationException.APPLICATION_ERROR_EXCEPTION, OpMindMeisterApplication.APP_KIND, rsp.getErr().getMsg());
        }
        return rsp.getFrob();
    }

    public String getToken(String frob) throws OpExternalApplicationException {
        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>(FROB, frob));
        Rsp rsp = sendRequest(METHOD_GET_TOKEN, parameters);
        if (STAT_FAIL.equals(rsp.getStat())) {
            throw new OpExternalApplicationException(OpExternalApplicationException.APPLICATION_ERROR_EXCEPTION, OpMindMeisterApplication.APP_KIND, rsp.getErr().getMsg());
        }
        token = rsp.getAuth().getToken();
        return token;
    }

    public String checkToken(String token) throws OpExternalApplicationException {
        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>(AUTH_TOKEN, token));
        Rsp rsp = sendRequest(METHOD_CHECK_TOKEN, parameters);
        if (STAT_FAIL.equals(rsp.getStat())) {
            throw new OpExternalApplicationException(OpExternalApplicationException.APPLICATION_ERROR_EXCEPTION, OpMindMeisterApplication.APP_KIND, rsp.getErr().getMsg());
        }
        token = rsp.getAuth().getToken();
        return token;
    }

    public Rsp.Maps getMaps() throws OpExternalApplicationException {
        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>(AUTH_TOKEN, token));
        Rsp rsp = sendRequest(METHOD_GET_MAPS, parameters);
        if (STAT_FAIL.equals(rsp.getStat())) {
            throw new OpExternalApplicationException(OpExternalApplicationException.APPLICATION_ERROR_EXCEPTION, OpMindMeisterApplication.APP_KIND, rsp.getErr().getMsg());
        }
        return rsp.getMaps();
    }

    public Rsp.Ideas getMap(String mapId) throws OpExternalApplicationException {
        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>(AUTH_TOKEN, token));
        parameters.add(new Pair<String, String>(MAP_ID, mapId));
        Rsp rsp = sendRequest(METHOD_GET_MAP, parameters);
        if (STAT_FAIL.equals(rsp.getStat())) {
            throw new OpExternalApplicationException(OpExternalApplicationException.APPLICATION_ERROR_EXCEPTION, OpMindMeisterApplication.APP_KIND, rsp.getErr().getMsg());
        }
        return rsp.getIdeas();
    }

    public String getAuthenticationUrl(String frob) throws OpExternalApplicationException {
        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>(API_KEY, apiKey));
        parameters.add(new Pair<String, String>(PERMS, DEFAULT_PERMS));
        parameters.add(new Pair<String, String>(FROB, frob));
        parameters.add(new Pair<String, String>(API_SIG, signRequestParameters(parameters)));
        StringBuffer url = new StringBuffer(authUrl);
        NameValuePair[] params = new NameValuePair[parameters.size()];
        Iterator<Pair<String, String>> pit = parameters.iterator();
        int i = 0;
        while (pit.hasNext()) {
            Pair<String, String> p = pit.next();
            params[i] = new NameValuePair(p.getFirst(), p.getSecond());
            i++;
        }
        String query = EncodingUtil.formUrlEncode(params, "UTF-8");
        url.append(URL_QUERY_DELIMITER);
        url.append(query);
        return url.toString();
    }

    public Rsp sendRequest(String method, List<Pair<String, String>> parameters) throws OpExternalApplicationException {
        parameters.add(0, new Pair<String, String>(METHOD, method));
        parameters.add(1, new Pair<String, String>(API_KEY, apiKey));
        String parameterSig = signRequestParameters(parameters);
        parameters.add(new Pair<String, String>(API_SIG, parameterSig));
        Rsp rsp = null;
        InputStream is = sendHttpRequest(url, parameters, MINDMEISTER_HTTP_TIMEOUT);
        try {
            JAXBContext jc = JAXBContext.newInstance(JAXB_CLASS_PACKAGE);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String response = new String();
            String line;
            while ((line = reader.readLine()) != null) {
                response += new String(line.getBytes(), "UTF-8");
                response += '\n';
            }
            is.close();
            Reader ris = new StringReader(response);
            Object res = unmarshaller.unmarshal(ris);
            ris.close();
            rsp = (Rsp) res;
        } catch (JAXBException e) {
            throw new OpExternalApplicationException(OpExternalApplicationException.INTERNAL_ERROR_EXCEPTION, OpMindMeisterApplication.APP_KIND, "jaxb", e);
        } catch (UnsupportedEncodingException e) {
            throw new OpExternalApplicationException(OpExternalApplicationException.INTERNAL_ERROR_EXCEPTION, OpMindMeisterApplication.APP_KIND, "jaxb", e);
        } catch (IOException e) {
            throw new OpExternalApplicationException(OpExternalApplicationException.INTERNAL_ERROR_EXCEPTION, OpMindMeisterApplication.APP_KIND, "jaxb", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rsp;
    }

    private String signRequestParameters(List<Pair<String, String>> parameters) {
        StringBuffer buf = new StringBuffer();
        buf.append(secret);
        SortedMap<String, String> sortedParams = new TreeMap<String, String>(new Comparator<String>() {

            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        Iterator<Pair<String, String>> pit = parameters.iterator();
        while (pit.hasNext()) {
            Pair<String, String> p = pit.next();
            sortedParams.put(p.getFirst(), p.getSecond());
        }
        Iterator<String> kit = sortedParams.keySet().iterator();
        while (kit.hasNext()) {
            String key = kit.next();
            buf.append(key);
            buf.append(sortedParams.get(key));
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        String hashBase = buf.toString();
        byte[] digestBytes = md.digest(hashBase.getBytes());
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < digestBytes.length; i++) {
            hashString.append(Integer.toString((digestBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return hashString.toString();
    }

    public InputStream loadAttachment(BigInteger id) throws OpExternalApplicationException {
        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>(API_KEY, apiKey));
        parameters.add(new Pair<String, String>(ATTACHMENT_ID, id.toString()));
        parameters.add(new Pair<String, String>(AUTH_TOKEN, token));
        parameters.add(new Pair<String, String>(API_SIG, signRequestParameters(parameters)));
        InputStream is = null;
        is = sendHttpRequest(attachmentUrl, parameters, MINDMEISTER_HTTP_TIMEOUT);
        return is;
    }
}
