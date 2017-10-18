package user;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import repository.SemanticRepository;
import model.Model;

public class Usermanegement {

    private static final String USERNAME_NS = "http://example.org/USER#", USERNAME_PREDICATE = "http://example.org/owlim#hasPassword";

    public static void main(String[] args) {
        System.out.println(encryptString(null));
    }

    public static String encryptString(String password) {
        if (password == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] encryptMsg = md.digest(password.getBytes());
            BigInteger temp = new BigInteger(encryptMsg);
            return temp.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean login(String username, String passkey) throws Exception {
        if (username == null || passkey == null) return false;
        String query = "CONSTRUCT {<http://example.org/USER#" + username + "> " + "<" + USERNAME_PREDICATE + "> \"" + passkey + "\"} where {<http://example.org/USER#" + username + "> " + "<" + USERNAME_PREDICATE + "> \"" + passkey + "\" . }";
        System.out.println(Model.getModel().readQuery(query));
        return Model.getModel().readQuery(query).size() == 1;
    }

    public boolean register(String username, String passkey) throws Exception {
        if (username == null || passkey == null) return false;
        SemanticRepository model = Model.getModel();
        model.writeRDFStatement(new URIImpl(USERNAME_NS + username), new URIImpl(USERNAME_PREDICATE), new ValueFactoryImpl().createLiteral(passkey));
        model.commit();
        return true;
    }
}
