package de.uni_hamburg.golem.target;

import java.security.MessageDigest;
import java.util.ArrayList;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.uni_hamburg.golem.Utils;
import de.uni_hamburg.golem.model.GAbstractRecord;
import de.uni_hamburg.golem.model.GEnterprisePackage;
import de.uni_hamburg.golem.model.GHandlerDevice;
import de.uni_hamburg.golem.model.GMessage;
import de.uni_hamburg.golem.model.GPerson;

public class WebCT4 extends BlackboardDevice implements GHandlerDevice {

    private Log log = LogFactory.getLog(this.getClass());

    /**
	 * Default (emtpy) constructor.
	 */
    public WebCT4() {
        super();
    }

    /**
	 * Set connection parameters.
	 *
	 * @param secret
	 * @param host
	 * @param port
	 */
    public WebCT4(String proto, String host, int port, String secret) {
        super(proto, host, port, (String) null, secret);
    }

    /**
	 * @param person
	 * @return
	 * @throws Exception
	 */
    protected GMessage handlePersonRequest(GPerson person) throws Exception {
        GetMethod get = new GetMethod(getProtocol() + "://" + getHost() + ":" + getPort() + "/webct/public/serve_webctdb");
        int recstatus = person.getOperation();
        String action = null;
        switch(recstatus) {
            case 1:
                action = "add";
                break;
            case 2:
                action = "update";
                break;
            case 3:
                action = "delete";
                break;
            default:
                action = "add";
                break;
        }
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(12);
        params.add(new NameValuePair("OPERATION", action));
        params.add(new NameValuePair("DB", "global"));
        params.add(new NameValuePair("COURSE", "xxxx"));
        params.add(new NameValuePair("WebCT ID", person.getUserid()));
        params.add(new NameValuePair("Password", person.getPassword()));
        params.add(new NameValuePair("Prefix", person.getPrefix()));
        params.add(new NameValuePair("First Name", person.getGiven()));
        params.add(new NameValuePair("Last Name", person.getFamily()));
        params.add(new NameValuePair("ID", person.getExternalref()));
        params.add(new NameValuePair("Birthdate", person.getBirthdate()));
        params.add(new NameValuePair("EMail", person.getEmail()));
        params.add(new NameValuePair("Institution", person.getInstitution()));
        params.add(new NameValuePair("Faculty", person.getFaculty()));
        params.add(new NameValuePair("Comment", person.getDescription()));
        params.add(new NameValuePair("Role", "" + person.getRoletype()));
        StringBuffer parbuf = new StringBuffer();
        for (int i = 0; i < params.size(); i++) {
            NameValuePair p = params.get(i);
            parbuf.append(p.getValue());
        }
        String mac = null;
        params.add(new NameValuePair("AUTH", mac));
        NameValuePair[] nvp = params.toArray(new NameValuePair[] {});
        get.setQueryString(nvp);
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        int status = client.executeMethod(get);
        if (status == HttpStatus.SC_OK) {
            String response = Utils.readStream(get.getResponseBodyAsStream());
            if (response.indexOf("Error") > -1 || response.startsWith("Fatal")) {
                get.releaseConnection();
                return new GMessage(GMessage.CODE_ERROR, GMessage.TARGET_RETURN, getID(), GMessage.REF_PKG, response);
            } else {
                get.releaseConnection();
                return new GMessage(GMessage.CODE_SUCCESS, GMessage.TARGET_RETURN, getID(), GMessage.REF_PKG, response);
            }
        } else {
            String response = get.getResponseBodyAsString();
            get.releaseConnection();
            return new GMessage(GMessage.CODE_ERROR, GMessage.TARGET_RETURN, getID(), GMessage.REF_PKG, response);
        }
    }

    /**
	 * Create WebCT MAC from IMSPersonRecord XML representation.
	 *
	 * @param imsPerson
	 * @return
	 */
    protected String getMAC(String s) {
        int i = 0;
        for (int j = 0; j < s.length(); j++) {
            i += (byte) s.charAt(j);
        }
        s = String.valueOf(i);
        s += getSecret();
        String s2 = "";
        byte b[];
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            b = md.digest(s.getBytes());
            for (i = 0; i < b.length; i++) {
                if ((b[i] & 0xFF) < 16) s2 += "0";
                s2 += Integer.toHexString(b[i] & 0xFF).toUpperCase();
            }
        } catch (Exception e) {
            log.error(e);
        }
        return s2;
    }

    /**
	 * Dispatches Person requests locally, Group requests to superclass.
	 * @see de.uni_hamburg.golem.target.GolemOperation#add(de.uni_hamburg.golem.model.AbstractRecord)
	 */
    public GMessage add(GAbstractRecord record) throws Exception {
        record.setADD();
        return dispatch(record);
    }

    /**
	 * Dispatches Person requests locally, Group requests to superclass.
	 * @see de.uni_hamburg.golem.target.GolemOperation#edit(de.uni_hamburg.golem.model.AbstractRecord)
	 */
    public GMessage edit(GAbstractRecord record) throws Exception {
        record.setEDIT();
        return dispatch(record);
    }

    /**
	 * Dispatches Person requests locally, Group requests to superclass.
	 * @see de.uni_hamburg.golem.target.GolemOperation#delete(de.uni_hamburg.golem.model.AbstractRecord)
	 */
    public GMessage delete(GAbstractRecord record) throws Exception {
        record.setDELETE();
        return dispatch(record);
    }

    /**
	 * @param record
	 * @return
	 * @throws Exception
	 */
    protected GMessage dispatch(GAbstractRecord record) throws Exception {
        if (record.getContext().equals(GEnterprisePackage.CTXPERSON)) {
            return handlePersonRequest((GPerson) record);
        } else {
            GEnterprisePackage pkg = new GEnterprisePackage();
            pkg.add(record);
            String pkg$ = pkg.toXML(this);
            return super.handleRequest(pkg$);
        }
    }

    @Override
    public GEnterprisePackage write(GEnterprisePackage pkg) throws Exception {
        GEnterprisePackage msgs = new GEnterprisePackage();
        ArrayList<GAbstractRecord> content = pkg.getRecords();
        for (int i = 0; i < content.size(); i++) {
            GAbstractRecord rec = content.get(i);
            switch(rec.getOperation()) {
                case GAbstractRecord.ADD:
                    msgs.add(add(rec));
                    break;
                case GAbstractRecord.EDIT:
                    msgs.add(edit(rec));
                    break;
                case GAbstractRecord.DELETE:
                    msgs.add(delete(rec));
                    break;
                default:
                    msgs.add(add(rec));
                    break;
            }
        }
        return msgs;
    }

    @Override
    public String getID() {
        return this.getId();
    }
}
