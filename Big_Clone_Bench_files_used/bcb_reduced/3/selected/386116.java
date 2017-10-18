package pika;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 *
 * @author Administrateur
 */
public class Flickr extends JPanel implements ActionListener {

    Test leModel;

    Fichier monFichier;

    String api_key = "50df53609314e029b3d817eeccfd000e";

    String secretKey = "54957b9610184212";

    String token;

    String leFrob;

    boolean authorised = false;

    public Flickr(Test leModel, Fichier monFichier) {
        this.leModel = leModel;
        this.monFichier = monFichier;
        leFrob = getFrob();
        System.out.println("le frob : " + leFrob);
        String urlLogin = createLoginLinkWrite(leFrob);
        System.out.println(urlLogin);
        setLayout(new FlowLayout());
        JButton bContinuer = new JButton("c'est bon, je suis all� valider");
        bContinuer.addActionListener(this);
        add(bContinuer);
        String[] s = { "E:\\Program Files\\Mozilla Firefox\\firefox.exe", urlLogin };
        try {
            Runtime.getRuntime().exec(s);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {
        token = getToken(leFrob);
        envoyerImage(token);
    }

    public void envoyerImage(String auth_token) {
        try {
            String api_sigPreparation = secretKey + "api_key" + api_key + "auth_token" + auth_token + "title" + monFichier.getNom();
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
            out.writeField("title", monFichier.getNom());
            out.writeFile("photo", "image/jpeg", new File(monFichier.getAdresse()));
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line = "";
            String reponse = "";
            while ((line = in.readLine()) != null) {
                reponse += line;
            }
            in.close();
            String numero = dirtyFlickrParser(reponse, "photoid");
            System.out.println("voici l'adresse de l'upload : " + "http://www.flickr.com/tools/uploader_edit.gne?ids=" + numero);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void checkToken(String auth_token) {
        String method = "flickr.auth.checkToken";
        String api_sigPreparation = secretKey + "api_key" + api_key + "auth_token" + auth_token + "method" + method;
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&auth_token=" + auth_token + "&api_sig=" + api_sig;
        String reponse = envoiGET(adresse);
        System.out.println(reponse);
    }

    public String createLoginLinkWrite(String frob) {
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

    public void getUploadStatus(String auth_token) {
        String method = "flickr.people.getUploadStatus";
        String api_sigPreparation = secretKey + "api_key" + api_key + "auth_token" + auth_token + "method" + method;
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&auth_token=" + auth_token + "&api_sig=" + api_sig;
        String reponse = envoiGET(adresse);
        System.out.println(reponse);
    }

    public String getToken(String frob) {
        String token = new String();
        String method = "flickr.auth.getToken";
        String api_sigPreparation = secretKey + "api_key" + api_key + "frob" + frob + "method" + method;
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&frob=" + frob + "&api_sig=" + api_sig;
        String reponse = envoiGET(adresse);
        token = dirtyFlickrParser(reponse, "token");
        return token;
    }

    public String getFrob() {
        String frob = new String();
        String method = "flickr.auth.getFrob";
        String api_sigPreparation = secretKey + "api_key" + api_key + "method" + method;
        String api_sig = md5(api_sigPreparation);
        String adresse = "http://api.flickr.com/services/rest/?method=" + method + "&api_key=" + api_key + "&api_sig=" + api_sig;
        String reponse = envoiGET(adresse);
        frob = dirtyFlickrParser(reponse, "frob");
        return frob;
    }

    public String dirtyFlickrParser(String leXML, String balise) {
        String[] result = leXML.split("<" + balise + ">");
        String[] result2 = result[1].split("</" + balise + ">");
        return result2[0];
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
        return coded;
    }
}
