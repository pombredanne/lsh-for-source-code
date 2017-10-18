package nacad.lemm.web.io;

import java.io.FileNotFoundException;
import javax.xml.parsers.ParserConfigurationException;
import nacad.lemm.web.links.UserLink;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import nacad.lemm.parser.FileParser;
import nacad.lemm.parser.UserParser;
import org.xml.sax.SAXException;

/**
 * This class retrieves user information from XML file and check if the
 * login is OK.
 * @author Jonas Dias
 */
public class UserRetriever {

    String filename = "user.list";

    List<UserLink> linkList = null;

    public static String md5(String pwd) {
        String sen = "";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(pwd.getBytes()));
        sen = hash.toString(16);
        return sen;
    }

    public List<UserLink> getLinkList() {
        return linkList;
    }

    /**
     * Get the code of the user stored on the XML file that should correspond
     * to its password coded.
     * @param user The user login
     * @return The code of the user
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws URISyntaxException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public String retrieveUserCode(String user) throws ParserConfigurationException, SAXException, URISyntaxException, FileNotFoundException, IOException {
        this.fillUserList();
        Map<String, String> map = this.getUsersMap();
        String code = md5(user + map.get(user));
        return code;
    }

    public Map<String, String> getUsersMap() throws ParserConfigurationException, SAXException, URISyntaxException, FileNotFoundException, IOException {
        if (linkList == null) {
            fillUserList();
        }
        Map<String, String> usersMap = new HashMap<String, String>();
        Iterator<UserLink> i = linkList.iterator();
        while (i.hasNext()) {
            UserLink ul = i.next();
            usersMap.put(ul.getLogin(), ul.getPassword());
        }
        return usersMap;
    }

    private void fillUserList() throws ParserConfigurationException, SAXException, URISyntaxException, FileNotFoundException, IOException {
        URL url = UserRetriever.class.getClassLoader().getResource(filename);
        File linkListFile;
        linkListFile = new File(url.toURI());
        UserParser parser = new UserParser();
        FileInputStream fis = new FileInputStream(linkListFile);
        FileParser.parseDocument(fis, parser);
        linkList = parser.getUserList();
    }
}
