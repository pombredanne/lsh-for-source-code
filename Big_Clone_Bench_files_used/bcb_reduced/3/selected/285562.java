package com.andrewj.parachute.network;

import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;
import java.math.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import com.andrewj.parachute.ui.UserInterface;

/**
 * This abstract class contains properties and methods common to either type of Synchronizer object:<br>
 * <li>{@link SynchronizerMaster}<br>
 * <li>{@link SynchronizerSlave}<br>
 * When user A connects to user B with parachute, user A then becomes the {@link SynchronizerMaster}, and B becomes the {@link SynchronizerSlave}.<br><br>
 * 
 * Both of these child classes use methods in this superclass to build up an XML representation of their local sharing folder that looks like this:<br>
 * &ltsf&gt<br>
 * &ltFile deleted="false" modtime="1289041166000" name="file1.exe" size="104336414"/&gt<br>
 * &ltFile deleted="false" modtime="1289041166000" name="folder1/file2.zip" size="104336414"/&gt<br>
 * &ltFolder deleted="false" name="folder1/pics/"/&gt<br>
 * &ltFile deleted="false" modtime="1289041166000" name="folder1/pics/file3.jpg" size="104336414"/&gt<br>
 * &lt/sf&gt<br>
 * The Master then receives this XML from the slave, and generates a list of actions based on the content of the XML - taking into account new files, updated files, and files that have been detected as having been deleted ('deleted' attribute set to true)<br>
 * These lists of actions are ArrayLists of Strings, with an action and a relative pathname separated by a semicolon:<br>
 * <b>"send;/pictures/picture1.jpg"</b><br>
 * means "I need to 'send' /pictures/picture1.jpg to you"
 */
public abstract class Synchronizer {

    protected int iLocalPort;

    protected static final byte UPDATE_XML = 1;

    protected static final byte SEND_XML = 2;

    protected static final byte ADD_ACTION = 3;

    protected static final byte START_RECEIVER = 4;

    protected static final byte REFRESH_SR = 5;

    protected static final byte START_SENDER = 6;

    protected static final byte START_TRANSFERERS = 7;

    protected static final byte KEEP_ALIVE = 8;

    protected static final byte SET_SO_TIMEOUT = 9;

    protected final int SO_TIMEOUT = 10000;

    protected Sender tSender;

    protected Receiver tReceiver;

    protected Document sfxml;

    protected boolean bDisconnected;

    protected UserInterface uiGUI;

    protected Thread tSyncer;

    protected Socket sPartner;

    protected DataInputStream in;

    protected DataOutputStream out;

    protected File fFolder;

    public Synchronizer(Socket p, File f, UserInterface g) throws IOException {
        this.uiGUI = g;
        this.fFolder = f;
        this.sPartner = p;
        this.bDisconnected = false;
    }

    /**
	 * temporary debug method to dump the XML to a file
	 */
    protected void dumpXML(Document d, String f) {
        try {
            new File(f).delete();
            DOMSource source = new DOMSource(d);
            StreamResult result = new StreamResult(new PrintStream(f));
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Initialise this Synchronizer's Sender and Receiver
	 */
    protected void initSendReceive() {
        tReceiver = new Receiver(in, fFolder, uiGUI);
        tSender = new Sender(out, fFolder, uiGUI);
    }

    /**
	 * wait for the sender and receiver to be inactive
	 * @throws IOException if the transfer becomes disconnected
	 */
    protected void waitForCompletion() throws IOException {
        while ((tReceiver.isAlive() || tSender.isAlive()) && !bDisconnected) {
            pause(250);
        }
        if (bDisconnected) {
            tSender.stopTransferer();
            tReceiver.stopTransferer();
            throw new IOException();
        }
    }

    /**
	 * Set this synchronizer as disconnected. Calls {@link Thread.join} to wait for the Thread to finish
	 */
    public void disconnect() {
        bDisconnected = true;
        tSyncer.interrupt();
        try {
            in.close();
            out.close();
        } catch (IOException e) {
        }
        try {
            tSyncer.join();
        } catch (InterruptedException e) {
        }
    }

    protected void setGUIDisconnected() {
        uiGUI.setStatusBarText("Disconnected");
        uiGUI.setSFLabelText("");
        uiGUI.addInfoMessage("Disconnected");
        uiGUI.setMenusDisconnected();
    }

    /**
	 * start this synchronizer
	 */
    public void start() {
        tSyncer.start();
    }

    /**
	 * Initialises a DOM Document that represents the local sharing folder. Also calls getFiles to populate the new XML. The XML's DTD is as follows:<br>
	 * &lt?xml version="1.0"?&gt<br> 
	 * &lt!DOCTYPE sf [<br>
	 * &lt!ELEMENT sf (Folder*, File*)&gt<br>
	 * &lt!ELEMENT File EMPTY&gt<br> 
	 * &lt!ELEMENT Folder EMPTY&gt<br>
	 * &lt!ATTLIST Folder name CDATA #REQUIRED<br> 
	 *     deleted CDATA #REQUIRED&gt<br>
	 * &lt!ATTLIST File name CDATA #REQUIRED<br>
	 *     size CDATA #REQUIRED<br>
	 *     modtime CDATA #REQUIRED<br>
	 *     deleted CDATA #REQUIRED&gt]&gt<br>
	 *     &ltsf&gt&lt/sf&gt
	 * 
	 * @throws XMLException
	 */
    protected void createNewXML() throws XMLException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            sfxml = builder.parse(new InputSource(new StringReader("<?xml version=\"1.0\"?>" + "<!DOCTYPE sf [" + "<!ELEMENT sf (Folder*, File*)>" + "<!ELEMENT File EMPTY>" + "<!ELEMENT Folder EMPTY>" + "<!ATTLIST Folder name CDATA #REQUIRED " + "deleted CDATA #REQUIRED>" + "<!ATTLIST File name CDATA #REQUIRED " + "size CDATA #REQUIRED " + "modtime CDATA #REQUIRED " + "deleted CDATA #REQUIRED " + "hash CDATA #REQUIRED>]>" + "<sf></sf>")));
            getFiles(fFolder);
        } catch (Exception e) {
            throw new XMLException("createNewXML");
        }
    }

    /**
	 * A method to recursively walk a directory tree and add elements to the local XML that represent the Files/Folders
	 * @param The folder to get files from
	 */
    private void getFiles(File f) {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            try {
                if (!files[i].isDirectory()) {
                    BufferedInputStream buffIn = new BufferedInputStream(new FileInputStream(files[i]));
                    buffIn.close();
                }
                addElement(files[i]);
                if (files[i].isDirectory()) {
                    getFiles(files[i]);
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    /**
	 * Looks at each element in the local DOM XML and if the file represented by that element has been deleted, set that element's deleted attribute to true
	 * @throws XMLException
	 */
    private void checkDeleted() throws XMLException {
        try {
            NodeList nl = sfxml.getDocumentElement().getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Element e = (Element) nl.item(i);
                if (!fileExists(e)) {
                    e.setAttribute("deleted", "true");
                }
            }
        } catch (Exception e) {
            throw new XMLException("checkDeleted");
        }
    }

    /**
	 * Takes a relative file/folder path as a string and if an Element exists in the given Document that represents that path, return that Element, otherwise return null
	 * @param x the input DOM document - must be of the same type as created by createNewXML()
	 * @param p the relative path
	 * @return the element that represents this path, null if nothing was found
	 */
    protected Element getElement(Document x, String p) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            XPathExpression expr = xPath.compile("/sf/*[@name='" + p + "']");
            Element e = (Element) (expr.evaluate(x, XPathConstants.NODE));
            return e;
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    /**
	 * Used to check if the file represented by the given element's 'name' attribute exists
	 * @param The element to test
	 * @return true if the file represented by this element exists, false otherwise
	 */
    private boolean fileExists(Element e) {
        File f = new File(fFolder.getPath() + File.separatorChar + e.getAttribute("name"));
        return f.exists();
    }

    /**
	 * method to update the local DOM XML that represents the shared folder. This method acts as the primary entry point for child classes of Synchronizer, and calls other private methods
	 * @throws XMLException
	 */
    protected void updateXML() throws XMLException {
        if (sfxml != null) {
            checkDeleted();
            getFiles(fFolder);
        } else {
            createNewXML();
        }
    }

    /**
	 * this method accepts the lists of recorded actions from the Sender and Receiver and adds/removes elements from the XML based on their content
	 * both the master and the slave do this at the end of each sync cycle
	 * @param s the list from the sender
	 * @param r the list from the receiver
	 */
    protected void processRecords(ArrayList<String> s, ArrayList<String> r) {
        ArrayList<ArrayList<String>> lists = new ArrayList<ArrayList<String>>();
        lists.add(s);
        lists.add(r);
        for (Iterator<ArrayList<String>> li = lists.iterator(); li.hasNext(); ) {
            for (Iterator<String> i = li.next().iterator(); i.hasNext(); ) {
                String sRecord = i.next();
                switch(sRecord.charAt(0)) {
                    case Transferer.DELETED_RECORD:
                        deleteElement(sRecord.substring(sRecord.indexOf(";") + 1));
                        break;
                    case Transferer.ADDED_RECORD:
                        addElement(new File(fFolder.getPath() + File.separator + sRecord.substring(sRecord.indexOf(";") + 1)));
                        break;
                }
            }
        }
    }

    /**
	 * calls wait with the specified timeout in milliseconds
	 * @param w timeout to wait - may be interrupted by InterruptedException
	 */
    protected void pause(long w) {
        synchronized (this) {
            try {
                wait(w);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
	 * Add an element to the local DOM XML that represents the given file/folder so long as the following conditions are met:<br>
	 * the given File object exists on disk<br>
	 * the element does not already exist in the local XML<br>
	 * Depending on the type of file passed in, the necessary attributes are created
	 * @param f The file/folder to add
	 */
    private void addElement(File f) {
        String strPath = f.getPath().replace(fFolder.getPath() + File.separatorChar, "").replace("\\", "/");
        if (f.exists()) {
            if (f.isDirectory()) {
                if (!strPath.endsWith("/")) {
                    strPath = strPath + "/";
                }
                Element eFolder = getElement(sfxml, strPath);
                if (eFolder == null) {
                    Element e = sfxml.createElement("Folder");
                    e.setAttribute("name", strPath);
                    e.setAttribute("deleted", "false");
                    sfxml.getDocumentElement().appendChild(e);
                }
            } else {
                Element eFile = getElement(sfxml, strPath);
                if (!(eFile == null)) {
                    eFile.setAttribute("size", Long.toString(f.length()));
                    eFile.setAttribute("modtime", Long.toString(f.lastModified()));
                    eFile.setAttribute("hash", getFileHash(f));
                } else {
                    Element e = sfxml.createElement("File");
                    e.setAttribute("name", strPath);
                    e.setAttribute("size", Long.toString(f.length()));
                    e.setAttribute("modtime", Long.toString(f.lastModified()));
                    e.setAttribute("deleted", "false");
                    e.setAttribute("hash", getFileHash(f));
                    sfxml.getDocumentElement().appendChild(e);
                }
            }
        }
    }

    /**
	 * Given a relative pathname to a file this will remove the element from the local XML that represents that pathname 
	 * @param s the relative path ('name' attribute) of the element to be removed
	 */
    private void deleteElement(String s) {
        Element e = getElement(sfxml, s);
        if (e != null) {
            sfxml.getDocumentElement().removeChild(e);
        }
    }

    /**
	 * Create an MD5 checksum of the given file
	 * @param f the file from which to generate the hash
	 * @return the hash as a string - empty string if there was a problem
	 */
    private String getFileHash(File f) {
        try {
            InputStream is = new FileInputStream(f);
            byte[] buf = new byte[64 * 1024];
            MessageDigest m = MessageDigest.getInstance("MD5");
            int iRead;
            do {
                iRead = is.read(buf);
                if (iRead > 0) m.update(buf, 0, iRead);
            } while (iRead != -1);
            is.close();
            BigInteger i = new BigInteger(1, m.digest());
            return (String.format("%1$032X", i));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
	 *Convenience Exception with a meaningful name
	 */
    protected class DisconnectedException extends Exception {
    }

    /**
	 * Convenience Exception used when aborting a sync cycle. A message can be set and retrieved
	 */
    protected class AbortSyncException extends Exception {

        private String strMessage;

        AbortSyncException(String m) {
            strMessage = m;
        }

        public String getMessage() {
            return "Aborting sync cycle: " + strMessage;
        }
    }

    /**
	 * XML Exceptions should be extremely rare in the application. XMLException is thrown in place of other more specific XML Exceptions (such as SAXException, DOMException etc) with a view to simplifying code
	 * 
	 */
    protected class XMLException extends Exception {

        private String strMessage;

        XMLException(String m) {
            strMessage = m;
        }

        public String getMessage() {
            return "XML error: " + strMessage;
        }
    }
}
