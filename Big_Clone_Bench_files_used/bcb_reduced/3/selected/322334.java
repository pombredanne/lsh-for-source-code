package net.sourceforge.exclusive.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import net.sourceforge.exclusive.client.config.ClientConf;
import net.sourceforge.exclusive.data.ListOfFriends;
import net.sourceforge.exclusive.data.SharedFile;

public class Util {

    private static SAXBuilder xmlParser;

    private static final List<String> imageFileExtensions = Arrays.asList("png;bmp;svg;jpeg;jpg;gif".split(";"));

    private static final List<String> htmlFileExtensions = Arrays.asList("html;htm".split(";"));

    private static final List<String> audioFileExtensions = Arrays.asList("mp3;ogg;wav;wma".split(";"));

    private static final List<String> videoFileExtensions = Arrays.asList("avi;divx;rm;wmv;mpeg;mp4;mpg;flv;mkv;3gp".split(";"));

    public static void initXMLParser() {
        xmlParser = new SAXBuilder();
    }

    public static boolean fileExists(String filename) {
        return new File(filename).exists();
    }

    public static boolean fileIsDir(String filename) {
        return new File(filename).isDirectory();
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(String filename) {
        if (fileExists(filename)) {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename));
                T obj = (T) in.readObject();
                in.close();
                return obj;
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Deserialization did not work!");
            }
        }
        return null;
    }

    public static void serialize(String filename, Object obj) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
            out.writeObject(obj);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Serialization did not work!");
        }
    }

    public static synchronized String calcHash(File file) {
        try {
            MessageDigest md = null;
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fileInputStream.read(data);
            fileInputStream.close();
            try {
                md = MessageDigest.getInstance("MD5");
                md.update(data);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
            return convertToHex(md.digest()) + data.length;
        } catch (Exception e) {
            return "";
        }
    }

    public static synchronized String calcHash(byte[] data) {
        byte[] buf = new byte[data.length];
        for (int i = 0; i < data.length; i++) buf[i] = data[i];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(buf);
            return convertToHex(md.digest()) + buf.length;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static synchronized String reHashPW(String pw) {
        byte[] data = pw.getBytes();
        String newPW = calcHash(data);
        List<String> pieces = new LinkedList<String>();
        for (String s : newPW.split("")) {
            if (!s.equals("")) pieces.add(s);
        }
        String finalPW = "";
        for (int i = 0; i < pieces.size(); i++) if (i % 2 == 0) finalPW += pieces.get(i);
        for (int j = 0; j < 2; j++) for (int i = 0; i < pieces.size(); i++) if (i % 2 != 0) finalPW += pieces.get(i);
        return finalPW;
    }

    public static synchronized String[] calcPiecesHash(File file, int numPieces) {
        String[] pieces = new String[numPieces];
        try {
            for (int i = 0; i < numPieces; i++) {
                byte[] buf = new byte[ClientConf.PACKETSIZE];
                RandomAccessFile rand = new RandomAccessFile(file, "r");
                rand.seek(ClientConf.PACKETSIZE * i);
                rand.read(buf);
                rand.close();
                pieces[i] = Util.calcHash(buf);
            }
        } catch (Exception e) {
            return null;
        }
        return pieces;
    }

    public static String formatByte(double s) {
        String tail = "";
        if (s < 1024) {
            tail = " B";
        } else if (s < 1024 * 1024) {
            s = s / 1024.0;
            tail = " KB";
        } else if (s < 1024 * 1024 * 1024) {
            s = s / (1024.0 * 1024.0);
            tail = " MB";
        } else if (s < 1024 * 1024 * 1024 * 1024) {
            s = s / (1024.0 * 1024.0 * 1024.0);
            tail = " GB";
        } else if (s < 1024 * 1024 * 1024 * 1024 * 1024) {
            s = s / (1024.0 * 1024.0 * 1024.0 * 1024.0);
            tail = " TB";
        } else if (s < 1024 * 1024 * 1024 * 1024 * 1024 * 1024) {
            s = s / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);
            tail = " PB";
        } else if (s < 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024) {
            s = s / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);
            tail = " EB";
        } else if (s < 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024) {
            s = s / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);
            tail = " ZB";
        } else if (s < 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024) {
            s = s / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);
            tail = " YB";
        }
        NumberFormat numFormat = NumberFormat.getInstance();
        numFormat.setMinimumFractionDigits(1);
        numFormat.setMaximumFractionDigits(1);
        String form = numFormat.format(s).replaceAll("\\.", "");
        return form.replaceFirst(",", ".") + tail;
    }

    public static SharedFile.MIMEType getMIMEType(File file) {
        String name = file.getName().toLowerCase();
        int indexOfPoint = name.lastIndexOf(".");
        if (indexOfPoint == -1) return SharedFile.MIMEType.File_Generic;
        String extension = name.substring(indexOfPoint + 1, name.length());
        if (imageFileExtensions.contains(extension)) return SharedFile.MIMEType.File_Image;
        if (htmlFileExtensions.contains(extension)) return SharedFile.MIMEType.File_HTML;
        if (audioFileExtensions.contains(extension)) return SharedFile.MIMEType.File_Audio;
        if (videoFileExtensions.contains(extension)) return SharedFile.MIMEType.File_Video;
        return SharedFile.MIMEType.File_Generic;
    }

    public static Document transformToXML(String xml) {
        StringReader inReader = new StringReader(xml);
        Document doc = null;
        try {
            doc = xmlParser.build(inReader);
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getNameFromXML(Document doc) {
        return doc.getRootElement().getName();
    }

    public static String getValueFromXML(Document doc, String field) {
        return doc.getRootElement().getAttributeValue(field);
    }

    public static String getTextFromXML(Document doc) {
        return doc.getRootElement().getText();
    }

    public static Document listOfUsers2XML(List<Hashtable<String, String>> listOfOnlineUser) {
        Document doc = null;
        Element root = new Element("users");
        doc = new Document(root);
        for (Hashtable<String, String> user : listOfOnlineUser) {
            Element u = new Element("user");
            for (String key : user.keySet()) {
                u.setAttribute(key, user.get(key));
            }
            root.addContent(u);
        }
        return doc;
    }

    @SuppressWarnings("unchecked")
    public static Hashtable<String, Hashtable<String, String>> XML2ListOfUsers(Element listOfUsersElement) {
        Hashtable<String, Hashtable<String, String>> ret = new Hashtable<String, Hashtable<String, String>>();
        if (listOfUsersElement != null) {
            List<Element> userElements = (List<Element>) listOfUsersElement.getChildren();
            for (Element e : userElements) {
                Hashtable<String, String> user = new Hashtable<String, String>();
                String name = null;
                if (e.getName().equals("user")) {
                    for (Attribute a : (List<Attribute>) e.getAttributes()) {
                        if (a.getName().equals("name")) name = a.getValue(); else user.put(a.getName(), a.getValue());
                    }
                }
                ret.put(name, user);
            }
        }
        return ret;
    }

    public static Document listOfFriends2XML(ListOfFriends lof) {
        Document doc = new Document(new Element("friends"));
        doc.getRootElement().setAttribute("lastchange", "" + lof.lastChange);
        for (String friend : lof.getFriends()) {
            Element e = new Element("friend");
            e.setAttribute("name", friend);
            doc.getRootElement().addContent(e);
        }
        return doc;
    }

    @SuppressWarnings("unchecked")
    public static ListOfFriends XML2ListOfFriends(Element listOfFriendsElement) {
        ListOfFriends ret = new ListOfFriends();
        try {
            try {
                Attribute lastchange = listOfFriendsElement.getAttribute("lastchange");
                Long lc = Long.parseLong(lastchange.getValue());
                ret.lastChange = lc;
            } catch (Exception e) {
                ret.lastChange = 0L;
            }
            if (listOfFriendsElement != null) {
                List<Element> friendElements = (List<Element>) listOfFriendsElement.getChildren();
                for (Element e : friendElements) {
                    if (e.getName().equals("friend")) ret.addFriend(e.getAttributeValue("name"));
                }
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Document getXML(String root, Hashtable<String, String> attributes, String text) {
        Document doc = null;
        try {
            doc = new Document(new Element(root));
            for (String key : attributes.keySet()) {
                doc.getRootElement().setAttribute(key, attributes.get(key));
            }
            doc.getRootElement().setText(text);
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Document addChild2XML(Document docXML, Document docChild) {
        docXML.getRootElement().addContent(docChild.getRootElement().detach());
        return docXML;
    }

    public static Element getChildFromXML(Document doc, String childname) {
        Element child = doc.getRootElement().getChild(childname);
        return child;
    }

    public static String encodeBase64(byte[] data) {
        return Base64.encodeBytes(data);
    }

    public static byte[] decodeBase64(String data) throws IOException {
        return Base64.decode(data);
    }
}
