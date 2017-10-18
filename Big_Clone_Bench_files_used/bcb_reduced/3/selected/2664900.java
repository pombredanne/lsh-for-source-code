package org.opensrs;

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import org.jdom.*;

/**
 * we may as well try to do things the way that the perl coders did:
 * have an xml client library that communicates with a low level
 * object that takes care of the actual communication protocol
 * @author Noah Couture
 * @author Robert Dale
 */
public class OpenSRSXMLClient {

    protected String cookie;

    protected static String[] actions = { "lookup", "set", "get", "get_domain", "get_userinfo", "register", "create_nameserver", "modify_nameserver", "delete_nameserver", "set_cookie", "delete_cookie", "update_cookie", "lookup_domain", "get_price_domain", "check_transfer_domain", "quit_session" };

    private Socket _socket;

    BufferedInputStream input;

    DataOutputStream output;

    protected CBC _cipher;

    private boolean authenticated = false;

    public OpenSRSXMLClient() {
        this._cipher = new CBC(Config.KEY);
    }

    public XCP send_cmd(XCP xcp_message) {
        String action = (String) xcp_message.get_data("action");
        String object = (String) xcp_message.get_data("object");
        if (action == null || object == null) {
            System.out.println("returning null because of no action or object.");
            return null;
        }
        XCP data = null;
        if (!Arrays.asList(actions).contains(action.toLowerCase())) {
            try {
                data = new XCP();
                data.put("is_success", "0");
                data.put("response_code", "400");
                data.put("response_text", "Invalid Command: " + action + " " + object);
                return data;
            } catch (XCPException xcpe) {
                System.out.println("problem returning bad action: " + xcpe);
                return null;
            }
        }
        if (_socket == null) {
            if (!init_socket()) {
                try {
                    data = new XCP();
                    data.put("is_success", "0");
                    data.put("response_code", "400");
                    data.put("response_text", "Unable to establish socket");
                    return data;
                } catch (XCPException xcpe) {
                    System.out.println("problem returning bad socket: " + xcpe);
                    return null;
                }
            }
        }
        if (!authenticated) {
            XCP auth = _authenticate();
            String success = (String) auth.get_data("is_success");
            if (success == null || !success.equals("1")) {
                try {
                    data = new XCP();
                    data.put("is_success", "0");
                    data.put("response_code", "400");
                    return data;
                } catch (XCPException xcpe) {
                    System.out.println("problem returning bad authentication: " + xcpe);
                    return null;
                }
            }
        }
        OPS.write_data(output, _cipher.encrypt(xcp_message.toString().getBytes()));
        try {
            data = new XCP(new String(read_data(true)));
        } catch (XCPException xcpe) {
            System.out.println("" + xcpe);
        }
        return data;
    }

    protected byte[] read_data(boolean encryption) {
        byte[] buf = OPS.read_data(input);
        if (buf.length > 0 && encryption) buf = _cipher.decrypt(buf);
        return buf;
    }

    private boolean init_socket() {
        Socket s;
        InputStream is;
        OutputStream os;
        try {
            s = new Socket(InetAddress.getByName(Config.REMOTE_HOST), Config.REMOTE_PORT);
            input = new BufferedInputStream(s.getInputStream());
            output = new DataOutputStream(s.getOutputStream());
        } catch (UnknownHostException uhe) {
            System.err.println(uhe.toString());
            return false;
        } catch (IOException ioe) {
            System.err.println(ioe.toString());
            return false;
        }
        this._socket = s;
        return true;
    }

    /**
	* this assumes the socket was just opened <br>
	* since the first thing we do is look at the signature
	* which is the first thing the server sends */
    private XCP _authenticate() {
        OPS.read_data(input);
        OPS.write_data(output, getMessage(3).getBytes());
        OPS.write_data(output, getMessage(4).getBytes());
        byte[] challenge = OPS.read_data(input);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] hash = md.digest(challenge);
        OPS.write_data(output, _cipher.encrypt(hash));
        try {
            XCP response = new XCP(new String(read_data(true)));
            if (response.get_data("is_success").equals("1")) authenticated = true;
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean senddata(byte[] data) {
        int cl = data.length;
        byte[] b;
        try {
            b = ("Content-Length: " + cl + "\r\n\r\n").getBytes();
            output.write(b, 0, b.length);
            output.write(data, 0, data.length);
            output.flush();
        } catch (IOException ioe) {
            System.out.println("senddata: " + ioe);
            return false;
        }
        return true;
    }

    private String getMessage(int msgid) {
        String r = "";
        switch(msgid) {
            case 3:
                try {
                    XCP xcp = new XCP();
                    Hashtable data = new Hashtable();
                    data.put("protocol", "XCP");
                    data.put("action", "CHECK");
                    Hashtable attr = new Hashtable();
                    attr.put("state", "ready");
                    attr.put("sender", "OpenSRS CLIENT");
                    attr.put("version", "XML:2.2.2");
                    data.put("attributes", attr);
                    XMLCodec xml = new XMLCodec();
                    xcp.setBody(xml.dataToXML(data));
                    return xcp.toString();
                } catch (XCPException xcpe) {
                    return "";
                }
            case 4:
                try {
                    XCP xcp = new XCP();
                    Hashtable data = new Hashtable();
                    data.put("protocol", "XCP");
                    data.put("action", "authenticate");
                    Hashtable attr = new Hashtable();
                    attr.put("username", Config.USERNAME);
                    attr.put("password", Config.USERNAME);
                    attr.put("crypt_type", "blowfish");
                    data.put("attributes", attr);
                    XMLCodec xml = new XMLCodec();
                    xcp.setBody(xml.dataToXML(data));
                    return xcp.toString();
                } catch (XCPException xcpe) {
                    return "";
                }
            default:
                return "";
        }
    }

    public static void main(String[] args) {
        OpenSRSXMLClient client = new OpenSRSXMLClient();
        XCP msg = null;
        try {
            msg = new XCP();
        } catch (XCPException xcpe) {
            System.out.println("fuck: " + xcpe);
            System.exit(1);
        }
        msg.setItem("protocol", "XCP");
        msg.setAction("LOOKUP");
        msg.setObject("DOMAIN");
        Element e = new Element("item");
        e.setAttribute("key", "domain");
        e.setText("foobar.com");
        msg.getAttributeRoot().addContent(e);
        e = new Element("item");
        e.setAttribute("key", "affiliate_id");
        e.setText("");
        msg.getAttributeRoot().addContent(e);
        XCP results = client.send_cmd(msg);
        String success = (String) results.get_data("is_success");
        if ("0".equals(success)) {
            System.out.println("The command failed.");
            System.out.println("response code: " + (String) results.get_data("response_code"));
            System.out.println("response text: " + (String) results.get_data("response_text"));
        } else {
            System.out.println("" + results);
        }
    }
}
