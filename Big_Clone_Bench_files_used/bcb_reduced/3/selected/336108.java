package biut.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import biut.Annotation;
import biut.Commentaire;
import biut.Membre;
import com.google.appengine.api.datastore.KeyFactory.Builder;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class Tests_Annotation {

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    @Before
    public void setUp() {
        helper.setUp();
    }

    @After
    public void tearDown() {
        helper.tearDown();
    }

    private void testConstructeurGet() {
        Commentaire C = new Commentaire("ba900", new Long(1), "Tests sur la classe commentaire");
        assertNotNull(C);
        assertNotNull(C.getDate());
    }

    @SuppressWarnings("deprecation")
    private void testConstructeurSet() {
        Commentaire C2 = new Commentaire("ba900", new Long(1), "Tests 2 sur la classe commentaire");
        assertNotNull(C2);
        C2.setDate(new Date(1990, 5, 6));
        assertEquals(new Date(1990, 5, 6), C2.getDate());
    }

    @SuppressWarnings("deprecation")
    private void testReadBDD() {
        Key key = new Builder("Commentaire", 1).getKey();
        Commentaire C3 = Commentaire.readCommentaire(key);
        assertNotNull(C3);
        assertNotNull(C3.getDate());
    }

    private void testGetmembre() {
        Date dat = new Date(1992, 06, 15);
        byte[] pwdByte = null;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            pwdByte = md5.digest("mdp1".getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Membre M1 = new Membre("bk900424", "Boursier", "Alexandre", "S4T", "Informatique", dat, "IUT Nice", "Nice", "etudiant", pwdByte, "alexthebry@hotmail.fr");
        String idMembre = M1.getNumeroEtudiant();
        Key key = new Builder("Commentaire", 4).getKey();
        Commentaire C4 = new Commentaire("bk900424", new Long(1), "Tests 4 sur la classe commentaire");
        assertEquals(idMembre, C4.getMembre().getNumeroEtudiant());
        assertEquals(M1.getNom(), C4.getMembre().getNom());
        assertEquals(M1.getPrenom(), C4.getMembre().getPrenom());
    }

    @Test
    public void T_ConstructeurRead() {
        testConstructeurGet();
        testReadBDD();
        testGetmembre();
    }

    @Test
    public void T_ConstructeurDelete() {
    }

    @Test
    public void T_ConstructeurSET() {
        testConstructeurSet();
    }
}
