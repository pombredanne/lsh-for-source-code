package uk.co.koeth.brotzeit.server;

import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.restlet.data.Form;
import org.restlet.engine.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class UserRecordRepresentation {

    public static final String NS = "http://www.koeth.co.uk/brotzeit/user";

    public static Element toXmlListRepresentation(Document document, List<UserRecord> lUserRecords) {
        Element eUserRecords = document.createElementNS(NS, "user:userRecords");
        Iterator<UserRecord> iUserRecords = lUserRecords.iterator();
        while (iUserRecords.hasNext()) {
            eUserRecords.appendChild(UserRecordRepresentation.toXmlRepresentation(document, iUserRecords.next()));
        }
        return eUserRecords;
    }

    public static Element toXmlRepresentation(Document document, UserRecord userRecord) {
        String uniqueId = null;
        String username = null;
        String password = null;
        String userModification = null;
        String dateModification = null;
        Element eUserRecord = document.createElementNS(NS, "user:userRecord");
        if (userRecord.getUniqueId() != null) {
            uniqueId = KeyFactory.keyToString(userRecord.getUniqueId());
            Element eUniqueId = document.createElementNS(NS, "user:uniqueId");
            eUniqueId.appendChild(document.createTextNode(uniqueId));
            eUserRecord.appendChild(eUniqueId);
        }
        if (userRecord.getUsername() != null) {
            username = userRecord.getUsername();
            Element eUsername = document.createElementNS(NS, "user:username");
            eUsername.appendChild(document.createTextNode(username));
            eUserRecord.appendChild(eUsername);
        }
        if (userRecord.getPassword() != null) {
            password = userRecord.getPassword();
            Element ePassword = document.createElementNS(NS, "user:password");
            ePassword.appendChild(document.createTextNode(password));
            eUserRecord.appendChild(ePassword);
        }
        if (userRecord.getUserModification() != null) {
            userModification = userRecord.getUserModification();
            Element eUserModification = document.createElementNS(NS, "user:userModification");
            eUserModification.appendChild(document.createTextNode(userModification));
            eUserRecord.appendChild(eUserModification);
        }
        if (userRecord.getDateModification() != null) {
            dateModification = new SimpleDateFormat(UserRecord.DATE_MODIFICATION_FORMAT).format(userRecord.getDateModification());
            Element eDateModification = document.createElementNS(NS, "user:dateModification");
            eDateModification.appendChild(document.createTextNode(dateModification));
            eUserRecord.appendChild(eDateModification);
        }
        return eUserRecord;
    }

    public static UserRecord fromXmlRepresentation(Element eUserRecord) throws ParseException {
        Key uniqueId = null;
        String username = null;
        String password = null;
        String userModification = null;
        Date dateModification = null;
        NodeList nlUniqueId = eUserRecord.getElementsByTagNameNS(NS, "uniqueId");
        if (nlUniqueId.getLength() == 1) {
            uniqueId = KeyFactory.stringToKey(nlUniqueId.item(0).getTextContent());
        }
        NodeList nlUsername = eUserRecord.getElementsByTagNameNS(NS, "username");
        if (nlUsername.getLength() == 1) {
            username = nlUsername.item(0).getTextContent();
        }
        NodeList nlPassword = eUserRecord.getElementsByTagNameNS(NS, "password");
        if (nlPassword.getLength() == 1) {
            password = nlPassword.item(0).getTextContent();
        }
        NodeList nlUserModification = eUserRecord.getElementsByTagNameNS(NS, "userModification");
        if (nlUserModification.getLength() == 1) {
            userModification = nlUserModification.item(0).getTextContent();
        }
        NodeList nlDateModification = eUserRecord.getElementsByTagNameNS(NS, "dateModification");
        if (nlDateModification.getLength() == 1) {
            dateModification = new SimpleDateFormat(UserRecord.DATE_MODIFICATION_FORMAT).parse(nlDateModification.item(0).getTextContent());
        }
        UserRecord userRecord = new UserRecord(username, password);
        userRecord.setUniqueId(uniqueId);
        userRecord.setUserModification(userModification);
        userRecord.setDateModification(dateModification);
        return userRecord;
    }

    public static Form toFormRepresentation(UserRecord userRecord) {
        Form form = new Form();
        if (userRecord.getUniqueId() != null) {
            String uniqueId = KeyFactory.keyToString(userRecord.getUniqueId());
            form.add("uniqueId", uniqueId);
        }
        if (userRecord.getUsername() != null) {
            String user = userRecord.getUsername();
            form.add("username", user);
        }
        if (userRecord.getPassword() != null) {
            String password = userRecord.getPassword();
            form.add("password", password);
        }
        if (userRecord.getUserModification() != null) {
            String userModification = userRecord.getUserModification();
            form.add("userModification", userModification);
        }
        if (userRecord.getDateModification() != null) {
            String dateModification = new SimpleDateFormat(UserRecord.DATE_MODIFICATION_FORMAT).format(userRecord.getDateModification());
            form.add("dateModification", dateModification);
        }
        return form;
    }

    public static UserRecord fromFormRepresentation(Form form) throws ParseException {
        Key uniqueId = null;
        String user = null;
        String password = null;
        String userModification = null;
        Date dateModification = null;
        if (form.getFirstValue("uniqueId") != null) {
            uniqueId = KeyFactory.stringToKey(form.getFirstValue("uniqueId"));
        }
        if (form.getFirstValue("username") != null) {
            user = form.getFirstValue("username");
        }
        if (form.getFirstValue("password") != null) {
            password = form.getFirstValue("password");
        }
        if (form.getFirstValue("userModification") != null) {
            userModification = form.getFirstValue("userModification");
        }
        if (form.getFirstValue("dateModification") != null) {
            dateModification = new SimpleDateFormat(UserRecord.DATE_MODIFICATION_FORMAT).parse(form.getFirstValue("dateModification"));
        }
        UserRecord userRecord = new UserRecord(user, password);
        userRecord.setUniqueId(uniqueId);
        userRecord.setUserModification(userModification);
        userRecord.setDateModification(dateModification);
        return userRecord;
    }

    public static String encryptPassword(String password) {
        String hash = null;
        try {
            MessageDigest md = null;
            md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes("UTF-8"));
            byte raw[] = md.digest();
            hash = Base64.encode(raw, false);
        } catch (Exception e) {
        }
        return hash;
    }
}
