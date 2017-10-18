package emailvis.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import emailvis.data.io.*;
import emailvis.hci.visualization.HistoryVisualizationData;

public class Mbox {

    private String mboxFile;

    private String xmlFile;

    private Vector<Contact> listContact;

    private Vector<Message> listMsg;

    private HashMap<String, Message> keyMessages;

    private HashMap<String, Contact> keyContactMail;

    private HashMap<String, Contact> keyContactAnonymized;

    public Mbox() {
        listMsg = new Vector<Message>();
        listContact = new Vector<Contact>();
        keyMessages = null;
        xmlFile = null;
        mboxFile = null;
        keyContactMail = new HashMap<String, Contact>();
        keyContactAnonymized = new HashMap<String, Contact>();
        keyMessages = new HashMap<String, Message>();
    }

    /**
	 * nombre de messages envoy�s par contact, tableau de type objet avec dans la premi�re
	 * colonne le contact et la deuxi�me colonne son nombre de messages
	 * @return
	 */
    public Object[][] getNbMsgSPerContact() {
        int ind = 0;
        Object[][] tabS = new Object[listContact.size()][2];
        for (Iterator i = listContact.iterator(); i.hasNext(); ) {
            Contact c = (Contact) i.next();
            Vector<Message> listMsg = c.getListMsgSent();
            tabS[ind][0] = c;
            tabS[ind][1] = new Integer(listMsg.size());
        }
        return (tabS);
    }

    public Vector<HistoryVisualizationData> getNbMsgRSPerDatePerContact() {
        Vector<HistoryVisualizationData> data = new Vector<HistoryVisualizationData>();
        for (Iterator i = listContact.iterator(); i.hasNext(); ) {
            Contact c = (Contact) i.next();
            data.addAll(c.getNbMsgRSPerDate());
        }
        return data;
    }

    /**
	 * nombre de messages re�us et envoyes par contact, tableau de type objet avec dans la premi�re
	 * colonne le nombre de messages envoyes, la deuxi�me colonne son nombre de messages re�us et la troisieme son nom
	 * @return
	 */
    public Object[][] getNbMsgRSPerContact() {
        int ind = 0;
        Object[][] tabRS = new Object[listContact.size()][3];
        for (Iterator i = listContact.iterator(); i.hasNext(); ) {
            Contact c = (Contact) i.next();
            Vector<Message> listMsgSent = c.getListMsgSent();
            Vector listMsgReceived = c.getListMsgReceived();
            tabRS[ind][2] = c.getId();
            Integer nbReceived = new Integer(listMsgReceived.size());
            if (nbReceived != null) tabRS[ind][1] = nbReceived; else tabRS[ind][1] = 0;
            Integer nbSent = new Integer(listMsgSent.size());
            if (nbSent != null) tabRS[ind][0] = nbSent; else tabRS[ind][0] = 0;
            ind++;
        }
        return (tabRS);
    }

    /**
	 * nombre de messages re�us par contact, tableau de type objet avec dans la premi�re
	 * colonne le contact et la deuxi�me colonne son nombre de messages re�us
	 * @return
	 */
    public Object[][] getNbMsgRPerContact() {
        int ind = 0;
        Object[][] tabR = new Object[listContact.size()][2];
        for (Iterator i = listContact.iterator(); i.hasNext(); ) {
            Contact c = (Contact) i.next();
            Vector listMsg = c.getListMsgReceived();
            tabR[ind][0] = c;
            tabR[ind][1] = new Integer(listMsg.size());
            ind++;
        }
        return (tabR);
    }

    /**
	 * Searchs for the contact with the specified id within the contact list listContact
	 * of this class, and returns it. If it wasn't found, returns null.
	 * 
	 * @param id : the id of the seeked contact.
	 * 
	 * @return the contact if found, null otherwise.
	 */
    public Contact getContact(String id) {
        boolean notFound = true;
        Contact theContact = new Contact();
        Iterator i = listContact.iterator();
        while (i.hasNext() && notFound) {
            theContact = (Contact) i.next();
            if (theContact.getId().equalsIgnoreCase(id)) {
                notFound = false;
            }
        }
        if (notFound) return null; else {
            return theContact;
        }
    }

    /**
	 * Searchs for the message with the specified id within the message list listMsg
	 * of this class, and returns it. If it wasn't found, returns null.
	 * 
	 * @param id : the id of the seeked message.
	 * 
	 * @return the message if found, null otherwise.
	 */
    public Message getMessage(String id) {
        boolean notFound = true;
        Message theMessage = new Message();
        Iterator i = listMsg.iterator();
        while (i.hasNext() && notFound) {
            theMessage = (Message) i.next();
            if (theMessage.getId().equalsIgnoreCase(id)) {
                notFound = false;
            }
        }
        if (notFound) return null; else {
            return theMessage;
        }
    }

    public int[][] getCommMatrix() {
        boolean trouve;
        int nbContacts = listContact.size();
        int[][] matrixNbMsgSent = new int[nbContacts][nbContacts];
        int ind = -1;
        String ID;
        trouve = false;
        int cpt = 0;
        for (Iterator i = listContact.iterator(); i.hasNext(); ) {
            Contact c = (Contact) i.next();
            for (Iterator j = c.getListMsgSent().iterator(); j.hasNext(); ) {
                Message m = (Message) j.next();
                for (Iterator k = m.getListContactReceiver().iterator(); k.hasNext(); ) {
                    Contact destinataire = (Contact) k.next();
                    ID = destinataire.getId();
                    while (!trouve && ind < listContact.size()) {
                        ind++;
                        trouve = (listContact.get(ind).getId().equals(ID));
                    }
                    if (trouve) matrixNbMsgSent[cpt][ind]++;
                }
                trouve = false;
                ind = -1;
            }
            cpt++;
        }
        return matrixNbMsgSent;
    }

    public Object[] getThreadsPerContact() {
        return null;
    }

    public int getContactListMailSent() {
        return 0;
    }

    public Object[] getTabThread() {
        Vector<String> tabID = new Vector<String>();
        Vector<Integer> repondA = new Vector<Integer>();
        Vector<Vector<Integer>> sesReponses = new Vector<Vector<Integer>>();
        Object[] obj = new Object[3];
        Vector<Message> leTabMess = new Vector<Message>();
        Iterator msgI = listMsg.iterator();
        while (msgI.hasNext()) {
            Message mess = (Message) msgI.next();
            if (mess.getPreviousMsg() == null) {
                leTabMess.add(mess);
                tabID.add(mess.getSubject());
                repondA.add(-1);
                for (int caseEnTraitement = leTabMess.size() - 1; caseEnTraitement < leTabMess.size(); caseEnTraitement++) {
                    Vector<Integer> lesRep = new Vector<Integer>();
                    Vector<Message> lesMess = leTabMess.get(caseEnTraitement).getReplyList();
                    for (int i = 0; i < lesMess.size(); i++) {
                        lesRep.add(tabID.size());
                        if (!leTabMess.contains(lesMess.get(i))) leTabMess.add(lesMess.get(i));
                        tabID.add(lesMess.get(i).getSubject());
                        repondA.add(caseEnTraitement);
                    }
                    lesMess.clear();
                    sesReponses.add(new Vector<Integer>(lesRep));
                    lesRep.clear();
                }
            }
        }
        obj[0] = tabID;
        obj[1] = repondA;
        obj[2] = sesReponses;
        return obj;
    }

    public void setMailingList() {
    }

    public void setAlias(Contact c) {
    }

    public void setFilter() {
    }

    public void setSpam(Contact c) {
    }

    public void setAnon(Contact c) {
    }

    public void loadMbox() throws Exception {
        listMsg = new Vector<Message>();
        listContact = new Vector<Contact>();
        keyMessages = null;
        keyContactMail = new HashMap<String, Contact>();
        keyContactAnonymized = new HashMap<String, Contact>();
        keyMessages = new HashMap<String, Message>();
        MBoxReader mbR = new MBoxReader(mboxFile, this);
        mbR.ParseMBox();
    }

    /**
	 * Load data previously stored on a XML File.
	 */
    public void loadXml(String path) throws Exception {
        listMsg = new Vector<Message>();
        listContact = new Vector<Contact>();
        keyMessages = null;
        keyContactMail = new HashMap<String, Contact>();
        keyContactAnonymized = new HashMap<String, Contact>();
        keyMessages = new HashMap<String, Message>();
        XMLReader xmlR = new XMLReader(path, this);
        xmlR.parse();
    }

    /**
	 * Saves the date from this class into an XML File.
	 */
    public void saveXml() throws Exception {
        XMLWriter xmlR = new XMLWriter(this);
        xmlR.makeXML();
    }

    public void mergeMbox() throws Exception {
        MBoxReader mbR = new MBoxReader(mboxFile, this);
        mbR.mergeMBox();
    }

    /**
	 * @return the contact list of this Mbox.
	 */
    public Vector<Contact> getListContact() {
        return listContact;
    }

    /**
	 * Adds a contact into the contact list of this Mbox.
	 * @param c, the contact to add.
	 * @return true if the operation was successfull, false otherwise.
	 */
    public boolean addListContact(Contact c) {
        return listContact.add(c);
    }

    /**
	 * @return the message list of this Mbox.
	 */
    public Vector<Message> getListMsg() {
        return listMsg;
    }

    /**
	 * Adds a message into the message list of this Mbox.
	 * @param m, the contact to add.
	 * @return true if the operation was successfull, false otherwise.
	 */
    public boolean addListMsg(Message m) {
        return listMsg.add(m);
    }

    /**
	 * @return the path to the Mbox file.
	 */
    public String getMboxFile() {
        return mboxFile;
    }

    /**
	 * Set the path of the Mbox file.
	 * @param mboxFile, the String containing the path.
	 */
    public void setMboxFile(String mboxFile) {
        this.mboxFile = mboxFile;
    }

    /**
	 * @return the path to the XML saving file.
	 */
    public String getXmlFile() {
        return xmlFile;
    }

    /**
	 * Set the path of the XML file.
	 * @param xmlFile, the String containing the path.
	 */
    public void setXmlFile(String xmlFile) {
        this.xmlFile = xmlFile;
    }

    /**
	 * @return the Message with the specified messageID.
	 */
    public Message getMessageFromMap(String messID) {
        return keyMessages.get(messID);
    }

    /**
	 * Adds a message into the HashMap of this class.
	 * @param messID, the key to the message, ie it's messageID from the Mbox file.
	 * @param m, the message to link with the specified ID.
	 */
    public void putMessageInMap(String messID, Message m) {
        keyMessages.put(messID, m);
    }

    public Contact getContactMailFromMap(String email) {
        try {
            Contact c = keyContactMail.get(email);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    public void putContactMailInMap(String email, Contact c) {
        keyContactMail.put(email, c);
    }

    public Contact getContactAnonymizedFromMap(String anonymized) {
        return keyContactAnonymized.get(anonymized);
    }

    public void putMessageInMap(String anonymized, Contact c) {
        keyContactAnonymized.put(anonymized, c);
    }

    public static String getEncodedPassword(String key) {
        byte[] uniqueKey = key.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            throw new Error("no MD5 support in this VM");
        }
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }

    public void setAnonymized(Contact c) {
        String anon = new String();
        anon = getEncodedPassword(c.getEmail());
        c.setAnonymized(anon);
        keyContactAnonymized.put(anon, c);
        keyContactMail.remove(c.getEmail());
        c.setEmail("");
    }

    public void createFilter(Vector<String> emailFilter, String filterName) {
        boolean notFound = true;
        Contact ct = new Contact();
        Iterator i = listContact.iterator();
        for (int j = 0; j < emailFilter.size(); j++) {
            while (i.hasNext() && notFound) {
                ct = (Contact) i.next();
                if (ct.getEmail().equals(emailFilter.elementAt(j))) {
                    ct.setFilter(true);
                    ct.setAnonymized(filterName);
                    notFound = false;
                }
            }
            i = listContact.iterator();
            notFound = true;
        }
        System.out.println("test createFilter : " + ct.getAnonymized());
    }

    public void createMailingList(Vector<String> emailMailList, String mailListName) {
        boolean notFound = true;
        Contact ct = new Contact();
        Iterator i = listContact.iterator();
        for (int j = 0; j < emailMailList.size(); j++) {
            while (i.hasNext() && notFound) {
                ct = (Contact) i.next();
                if (ct.getEmail().equals(emailMailList.elementAt(j))) {
                    ct.setMailingList(true);
                    ct.setAnonymized(mailListName);
                    notFound = false;
                }
            }
            i = listContact.iterator();
            notFound = true;
        }
        System.out.println("test createMailingList : " + ct.getEmail());
    }

    public void createSpam(Vector<String> emailSpam, String SpamName) {
        boolean notFound = true;
        Contact ct = new Contact();
        Iterator i = listContact.iterator();
        for (int j = 0; j < emailSpam.size(); j++) {
            while (i.hasNext() && notFound) {
                ct = (Contact) i.next();
                if (ct.getEmail().equals(emailSpam.elementAt(j))) {
                    ct.setSpam(true);
                    ct.setAnonymized(SpamName);
                    notFound = false;
                }
            }
            i = listContact.iterator();
            notFound = true;
        }
        System.out.println("test createSpam : " + ct.getEmail());
    }

    public void createAlias(Vector<String> emailAlias, String AliasName) {
        boolean notFound = true;
        Contact ct = new Contact();
        Iterator i = listContact.iterator();
        for (int j = 0; j < emailAlias.size(); j++) {
            ct = (Contact) i.next();
            while (i.hasNext() && notFound) {
                System.out.println("contact : " + ct.getId());
                if (ct.getEmail().equals(emailAlias.elementAt(j))) {
                    ct.setAlias(new Contact(AliasName));
                    ct.setAnonymized(AliasName);
                    notFound = false;
                    ct = (Contact) i.next();
                }
            }
            i = listContact.iterator();
            notFound = true;
        }
        System.out.println("test createAlias : " + ct.getAlias());
    }

    public void createAnonSubject(Vector<String> subject, String subjectAnon) {
        boolean notFound = true;
        Message mess = new Message();
        Iterator i = listMsg.iterator();
        while (i.hasNext() && notFound) {
            mess = (Message) i.next();
            if (mess.getSubject().equals(subject.elementAt(0))) {
                mess.setAnonymizedMSubject(subjectAnon);
                mess.setAnonSubject(true);
                notFound = false;
            }
        }
    }
}
