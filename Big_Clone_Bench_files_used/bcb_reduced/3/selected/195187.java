package flickr;

import Composite.Dossier;
import Composite.JPEG;
import Composite.Photo;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Administrateur
 */
public class FlickrUzine {

    private static String api_key = "50df53609314e029b3d817eeccfd000e";

    private static String secretKey = "54957b9610184212";

    private String auth_token;

    private String frob;

    private FlickrPanel leFlickrPanel;

    public FlickrUzine() {
    }

    public void setApi_key(String api_key) {
        this.api_key = api_key;
    }

    public void setFlickrPanel(FlickrPanel leFlickrPanel) {
        this.leFlickrPanel = leFlickrPanel;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getApi_key() {
        return api_key;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void envoyerImage(Photo laPhoto) {
        try {
            leFlickrPanel.ajouterConsole("Envoi de la photo " + laPhoto.toString() + " en cours ...\n");
            String tagsFlicker = "";
            Iterator it = laPhoto.getTags().iterator();
            while (it.hasNext()) {
                String suiv = (String) it.next();
                tagsFlicker = tagsFlicker + suiv + " ";
            }
            String api_sigPreparation = secretKey + "api_key" + api_key + "auth_token" + auth_token + "tags" + tagsFlicker + "title" + laPhoto.toString();
            String api_sig = md5(api_sigPreparation);
            URL url = new URL("http://api.flickr.com/services/upload/");
            String boundary = MultiPartFormOutputStream.createBoundary();
            URLConnection urlConn = MultiPartFormOutputStream.createConnection(url);
            urlConn.setRequestProperty("Accept", "*/*");
            urlConn.setRequestProperty("Content-Type", MultiPartFormOutputStream.getContentType(boundary));
            urlConn.setRequestProperty("Connection", "Keep-Alive");
            urlConn.setRequestProperty("Cache-Control", "no-cache");
            MultiPartFormOutputStream out = new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
            out.writeField("api_key", api_key);
            out.writeField("auth_token", auth_token);
            out.writeField("api_sig", api_sig);
            out.writeField("tags", tagsFlicker);
            out.writeField("title", laPhoto.toString());
            out.writeFile("photo", "image/jpeg", new File(laPhoto.getFichierCourant().getPath()));
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line = "";
            String reponse = "";
            while ((line = in.readLine()) != null) {
                reponse += line;
            }
            in.close();
            String numero = dirtyFlickrParser(reponse, "photoid");
            leFlickrPanel.ajouterConsole("Image upload�e. Voici l'adresse : " + "http://www.flickr.com/tools/uploader_edit.gne?ids=" + numero + "\n");
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void downloadImage(String adresse, String nom, Dossier pere) {
        try {
            URL url = new URL(adresse);
            InputStream in = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            File photo = new File("receptionFlickr/" + nom + ".jpg");
            FileOutputStream fo = new FileOutputStream(photo);
            byte[] buffer = new byte[4096];
            for (int read = 0; (read = in.read(buffer)) != -1; out.write(buffer, 0, read)) ;
            fo.write(out.toByteArray());
            fo.close();
            JPEG nouv = new JPEG(pere, photo, nom);
            int emplacement = pere.getIndex(nouv);
            pere.addChild(nouv, emplacement);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void checkToken() {
        String method = "flickr.auth.checkToken";
        String api_sigPreparation = secretKey + "api_key" + api_key + "auth_token" + auth_token + "method" + method;
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&auth_token=" + auth_token + "&api_sig=" + api_sig;
        String reponse = envoiGET(adresse);
    }

    public String createLoginLinkWrite() {
        String perms = "write";
        String api_sigPreparation = secretKey + "api_key" + api_key + "frob" + frob + "perms" + perms;
        String api_sig = md5(api_sigPreparation);
        String adresse = new String();
        try {
            URL u = new URL("http://www.flickr.com/services/auth/?api_key=" + api_key + "&perms=" + perms + "&frob=" + frob + "&api_sig=" + api_sig);
            adresse = u.toString();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
        }
        return adresse;
    }

    public void getUploadStatus() {
        String method = "flickr.people.getUploadStatus";
        String api_sigPreparation = secretKey + "api_key" + api_key + "auth_token" + auth_token + "method" + method;
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&auth_token=" + auth_token + "&api_sig=" + api_sig;
        String reponse = envoiGET(adresse);
        System.out.println(reponse);
    }

    public void recupererLesAdresses(String leXMLdeGetUnTagged) {
        final ArrayList lesElementsFlickr = new ArrayList();
        DefaultHandler handler = new DefaultHandler() {

            public void startElement(String namespaceURI, String lname, String qname, Attributes attrs) {
                if ((lname.equalsIgnoreCase("photo")) && (attrs.getLength() > 4)) {
                    ElementFlickr unElementFlickr = new ElementFlickr();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        String id = new String();
                        String title = new String();
                        String attribuEnCours = attrs.getLocalName(i);
                        if (attribuEnCours.equalsIgnoreCase("id")) {
                            id = attrs.getValue(i);
                            unElementFlickr.setId(id);
                            String urlImageTailleMax = FlickrUzine.this.getUrlImageTailleMax(id);
                            unElementFlickr.setAdresse(urlImageTailleMax);
                        } else if (attribuEnCours.equalsIgnoreCase("title")) {
                            title = attrs.getValue(i);
                            unElementFlickr.setTitle(title);
                        }
                    }
                    leFlickrPanel.getFlickrPanelDownload().getJTableDownloadModel().ajouterLigne(unElementFlickr);
                }
            }
        };
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            ByteArrayInputStream str = new ByteArrayInputStream(leXMLdeGetUnTagged.getBytes());
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(str, handler);
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getUntagged() {
        String method = "flickr.photos.getUntagged";
        String api_sigPreparation = secretKey + "api_key" + api_key + "auth_token" + auth_token + "method" + method + "per_page" + "500";
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&auth_token=" + auth_token + "&api_sig=" + api_sig + "&per_page=" + "500";
        String reponse = envoiGET(adresse);
        return reponse;
    }

    public String getUrlImageTailleMax(String photo_id) {
        String method = "flickr.photos.getSizes";
        String api_sigPreparation = secretKey + "api_key" + api_key + "method" + method + "photo_id" + photo_id;
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&api_sig=" + api_sig + "&photo_id=" + photo_id;
        String reponse = envoiGET(adresse);
        String adresseImg = new String();
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            ByteArrayInputStream str = new ByteArrayInputStream(reponse.getBytes());
            Document doc = builder.parse(str);
            Element root = doc.getDocumentElement();
            Node n = root.getFirstChild();
            while (n != null && !n.getNodeName().equals("sizes")) {
                n = n.getNextSibling();
            }
            Node size = n.getLastChild();
            while (size != null && !size.getNodeName().equals("size")) {
                size = size.getPreviousSibling();
            }
            NamedNodeMap attributes = size.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                String name = attribute.getNodeName();
                String value = attribute.getNodeValue();
                if (name.equals("source")) {
                    return value;
                }
            }
        } catch (DOMException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void getToken() {
        String method = "flickr.auth.getToken";
        String api_sigPreparation = secretKey + "api_key" + api_key + "frob" + frob + "method" + method;
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&frob=" + frob + "&api_sig=" + api_sig;
        String reponse = envoiGET(adresse);
        auth_token = dirtyFlickrParser(reponse, "token");
    }

    public void getFrob() {
        String method = "flickr.auth.getFrob";
        String api_sigPreparation = secretKey + "api_key" + api_key + "method" + method;
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&api_sig=" + api_sig;
        String reponse = envoiGET(adresse);
        this.frob = dirtyFlickrParser(reponse, "frob");
    }

    public String dirtyFlickrParser(String leXML, String balise) {
        System.out.println(leXML);
        String[] result2 = null;
        String retour = null;
        try {
            String[] result = leXML.split("<" + balise + ">");
            result2 = result[1].split("</" + balise + ">");
        } catch (ArrayIndexOutOfBoundsException e) {
            leFlickrPanel.ajouterConsole("ERREUR dans la recuperation de : " + balise + " recommencez depuis l'Etape 1 ...\n");
        }
        try {
            retour = result2[0];
        } catch (Exception e) {
            leFlickrPanel.ajouterConsole("ERREUR dans la recuperation de : " + balise + " recommencez depuis l'Etape 1 ...\n");
        }
        return retour;
    }

    public String envoiGET(String url) {
        String leXML = new String();
        try {
            URL u = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(u.openStream())));
            String lineSep = System.getProperty("line.separator");
            String nextLine = "";
            StringBuffer sb = new StringBuffer();
            while ((nextLine = in.readLine()) != null) {
                sb.append(nextLine);
                sb.append(lineSep);
            }
            leXML = sb.toString();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return leXML;
    }

    public String md5(String phrase) {
        MessageDigest m;
        String coded = new String();
        try {
            m = MessageDigest.getInstance("MD5");
            m.update(phrase.getBytes(), 0, phrase.length());
            coded = (new BigInteger(1, m.digest()).toString(16)).toString();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        if (coded.length() < 32) {
            coded = "0" + coded;
        }
        return coded;
    }
}
