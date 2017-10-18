package vavi.net.xmpp;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Iq.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 040117 nsano initial version <br>
 */
public class Iq extends Xmpp {

    /** */
    protected String getXmlName() {
        return "iq";
    }

    /** */
    private String to;

    /** */
    public void setTo(String to) {
        this.to = to;
    }

    /** */
    public String getTo() {
        return to;
    }

    /** */
    private String type;

    /** */
    public void setType(String type) {
        this.type = type;
    }

    /** */
    public String getType() {
        return type;
    }

    /** */
    private String from;

    /** */
    public void setFrom(String from) {
        this.from = from;
    }

    /** */
    public String getFrom() {
        return from;
    }

    /** */
    private String id;

    /** */
    public void setId(String id) {
        this.id = id;
    }

    /** */
    public String getId() {
        return id;
    }

    /** */
    private String error;

    /** */
    public void setError(String error) {
        this.error = error;
    }

    /** */
    public String getError() {
        return error;
    }

    /** */
    private Xmpp child;

    /** */
    public void setChild(Xmpp child) {
        this.child = child;
    }

    /** */
    public Xmpp getChild() {
        return child;
    }

    /** */
    public static String getRetrieveVCardString(String jid) {
        Iq iq = new Iq();
        iq.setTo(jid);
        iq.setType("get");
        iq.setId("GT-VCARD-REQ");
        VCard vCard = new VCard();
        vCard.setXmlns("vcard-temp");
        iq.setChild(vCard);
        return iq.toString();
    }

    /** */
    public static String getJid(String userName, String server) {
        return userName + "@" + server;
    }

    /** */
    public static String getRetrievePersVCardString(String server, String userName) {
        Iq iq = new Iq();
        iq.setTo(getJid(userName, server));
        iq.setType("get");
        iq.setId("GT-PERS-VCARD-REQ");
        VCard vCard = new VCard();
        vCard.setXmlns("vcard-temp");
        iq.setChild(vCard);
        return iq.toString();
    }

    /** */
    public static String getSaveVCardStrig(String userName, String server, VCard v) {
        Iq iq = new Iq();
        iq.setTo(getJid(userName, server));
        iq.setType("set");
        iq.setChild(v);
        return iq.toString();
    }

    /** */
    public static String getSubscriptionRequestString(String server, String userName, String to, String cname, String group, String reason) {
        Iq iq = new Iq();
        iq.setId("SUBSCR");
        iq.setType("set");
        Query query = new Query();
        query.setXmlns("jabber:iq:roster");
        Item item = new Item();
        item.setName(cname);
        item.setSubscription("remove");
        item.setGroup(group);
        query.addItem(item);
        iq.setChild(query);
        Presence presence = new Presence();
        presence.setFrom(userName + "@" + server);
        presence.setTo(to);
        presence.setType("subscribe");
        presence.setStatus(reason);
        return iq.toString() + presence.toString();
    }

    /** */
    public static String getRemoveSubscriptionString(String to) {
        Iq iq = new Iq();
        iq.setId("DEL");
        iq.setType("set");
        Query query = new Query();
        query.setXmlns("jabber:iq:roster");
        Item item = new Item();
        item.setJid(to);
        item.setSubscription("remove");
        query.addItem(item);
        iq.setChild(query);
        return iq.toString();
    }

    /** */
    public static String getRegisterString(String server, String email, String password, String userName) {
        Iq iq = new Iq();
        iq.setId("reg2");
        iq.setType("set");
        iq.setTo(server);
        Query query = new Query();
        query.setXmlns("jabber:iq:register");
        query.setEmail(email);
        query.setPassword(password);
        query.setUserName(userName);
        iq.setChild(query);
        System.err.println("Transmitting account information.");
        return iq.toString();
    }

    /** */
    public static String getQueryRegistrationString(String server) {
        Iq iq = new Iq();
        iq.setId("registration");
        iq.setType("get");
        iq.setTo(server);
        Query query = new Query();
        query.setXmlns("jabber:iq:register");
        iq.setChild(query);
        System.err.println("Querying reg. info.");
        return iq.toString();
    }

    /** */
    public static String getDisoString(String jid, String id) {
        Iq iq = new Iq();
        iq.setId(id);
        iq.setType("get");
        iq.setTo(jid);
        Query query = new Query();
        query.setXmlns("http://www.jabber.org/protocol/disco#info");
        iq.setChild(query);
        return iq.toString();
    }

    /** */
    public static String getRetrieveAvatarString(String jid) {
        return null;
    }

    /** */
    public static String getLoginWithPlainPasswordString(String streamId, String userName, String resource, String password) {
        Iq iq = new Iq();
        iq.setId(streamId);
        iq.setType("set");
        Query query = new Query();
        query.setXmlns("jabber:iq:auth");
        query.setUserName(userName);
        query.setResource(resource);
        query.setPassword(password);
        iq.setChild(query);
        return iq.toString();
    }

    /** */
    public static String getLoginWithDigestPasswordString(String streamId, String userName, String resource, String password) {
        Iq iq = new Iq();
        iq.setId(streamId);
        iq.setType("set");
        Query query = new Query();
        query.setXmlns("jabber:iq:auth");
        query.setUserName(userName);
        query.setResource(resource);
        query.setDigest(getDigestPassword(streamId, password));
        iq.setChild(query);
        return iq.toString();
    }

    /** */
    private static String getDigestPassword(String streamId, String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
        md.update((streamId + password).getBytes());
        byte[] uid = md.digest();
        int length = uid.length;
        StringBuilder digPass = new StringBuilder();
        for (int i = 0; i < length; ) {
            int k = uid[i++];
            int iint = k & 0xff;
            String buf = Integer.toHexString(iint);
            if (buf.length() == 1) {
                buf = "0" + buf;
            }
            digPass.append(buf);
        }
        return digPass.toString();
    }

    /** */
    private void parsePrivateParameters() {
    }

    /**
     * @throws IOException */
    public void handle(JabberConnection jc) throws IOException {
        if (type.equals("set")) {
        } else if (type.equals("get")) {
            if (getChild() != null) {
                String nameSpace = ((Query) getChild()).getXmlns();
                if (nameSpace.equals("jabber:iq:version")) {
                    Iq newIq = new Iq();
                    if (id != null) {
                        newIq.setId(id);
                    }
                    newIq.setTo(from);
                    newIq.setType("result");
                    Query query = new Query();
                    query.setXmlns("jabber:iq:version");
                    query.setName("vavi alpha, http://www.vavisoft.com");
                    query.setVersion(jc.version);
                    query.setOs("java 1.4.2");
                    newIq.setChild(query);
                    jc.writeLine(newIq.toString());
                } else if (nameSpace.equals("http://www.jabber.org/protocol/disco#info")) {
                    Iq newIq = new Iq();
                    if (id != null) {
                        newIq.setId(id);
                    }
                    newIq.setTo(from);
                    newIq.setType("result");
                    Query query = new Query();
                    query.setXmlns("http://www.jabber.org/protocol/disco#info");
                    Query.Feature feature = new Query.Feature();
                    feature.setVar("http://www.jabber.org/protocol/bytestreams/gsm");
                    query.setFeature(feature);
                    setChild(query);
                    System.out.println("Disco request recieved.");
                    jc.writeLine(toString());
                } else if (nameSpace.equals("http://www.jabber.org/protocol/bytestream/gsm")) {
                    handle(jc);
                }
            }
        } else if (type.equals("result")) {
            if (id.equals("GT-VCARD-REQ")) {
                VCard vc = (VCard) getChild();
                vc.addFrom(getFrom());
                Contact c = new Contact(vc);
                jc.fireEvent(this, "updateContact", c);
            } else if (id.equals("GT-PERS-VCARD-REQ")) {
                VCard vc = (VCard) getChild();
                vc.addFrom(getFrom());
                VCard vcard = new VCard(vc);
            } else if (getChild() != null) {
                String id2 = getId();
                if (id2.equals("LOAD")) {
                    parsePrivateParameters();
                } else if (id2.equals("registration")) {
                    jc.writeLine(getRegisterString(jc.getServer(), jc.email, jc.password, jc.getUserName()));
                }
                String namespace = ((Query) getChild()).getXmlns();
                if (namespace.equals("jabber:iq:roster")) {
                    jc.roster = new Roster((Query) getChild());
                }
            } else {
                if (getId().equals("reg2")) {
                    System.err.println("Registration successfull.");
                    jc.newA = false;
                    jc.fireEvent(this, "disableNewAccount");
                    jc.fireEvent(this, "connectPressed");
                } else if (getId().equals("SUBSCR")) {
                    System.err.println("SUBSCR");
                }
            }
        }
        if (type.equals("error")) {
            System.err.println("ERROR During auth: " + toString());
            String error = getError();
            if (error != null) {
                System.err.println(error);
            }
        }
        jc.fireEvent(this, "handleIQ", this);
    }
}
