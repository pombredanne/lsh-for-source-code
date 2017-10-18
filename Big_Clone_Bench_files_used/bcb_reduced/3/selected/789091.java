package biut.test;

import biut.Document;
import biut.Membre;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import com.google.appengine.api.datastore.KeyFactory.Builder;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class Tests_Membre {

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    @Before
    public void setUp() {
        helper.setUp();
    }

    @After
    public void tearDown() {
        helper.tearDown();
    }

    @SuppressWarnings("deprecation")
    private void testConstructeurGet() {
        Membre M1;
        Date dat = new Date(1992, 06, 15);
        byte[] pwdByte = null;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            pwdByte = md5.digest("mdp1".getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        M1 = new Membre("ba900450", "Boursier", "Alexandre", "S4T", "Informatique", dat, "IUT Nice", "Nice", "etudiant", pwdByte, "alexthebry@hotmail.fr");
        assertNotNull(M1);
        assertEquals(new String("Alexandre"), M1.getPrenom());
        assertEquals("Boursier", M1.getNom());
        assertEquals("ba900450", M1.getNumeroEtudiant());
        assertEquals("S4T", M1.getPromotion());
        assertEquals("Informatique", M1.getFiliere());
        assertEquals(new Date(1992, 06, 15), M1.getDateNaissanceSimple());
        assertEquals("IUT Nice", M1.getEcoleActuelle());
        assertEquals("Nice", M1.getVille());
        assertEquals("etudiant", M1.getStatut());
        assertEquals(pwdByte, M1.getMdp());
        assertEquals("alexthebry@hotmail.fr", M1.getEmail());
    }

    private void testConstructeurSimple() {
        Membre M2 = new Membre("ba9004500");
        assertNotNull(M2);
        assertEquals("ba9004500", M2.getNumeroEtudiant());
    }

    @SuppressWarnings("deprecation")
    private void testConstructeurSet() {
        Membre M1;
        Date dat = new Date(1992, 06, 15);
        byte[] pwdByte = null;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            pwdByte = md5.digest("mdp1".getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        M1 = new Membre("ba9004", "Boursier", "Alexandre", "S4T", "Informatique", dat, "IUT Nice", "Nice", "etudiant", pwdByte, "alexthebry@hotmail.fr");
        assertNotNull(M1);
        M1.setNumeroEtudiant("bk900424");
        M1.setNom("Bogo");
        M1.setPrenom("Kevin");
        M1.setFiliere("Info");
        M1.setPromotion("S5");
        M1.setEcoleActuelle("IUP");
        M1.setDateNaissance(new Date(1991, 9, 05));
        try {
            md5 = MessageDigest.getInstance("MD5");
            pwdByte = md5.digest("goulougoulou".getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        M1.setMdp(pwdByte);
        M1.setStatut("dieu");
        M1.setVille("Grasse");
        M1.setEmail("KBP@hotmail.fr");
        assertEquals("bk900424", M1.getNumeroEtudiant());
        assertEquals(new String("Kevin"), M1.getPrenom());
        assertEquals("Bogo", M1.getNom());
        assertEquals("S5", M1.getPromotion());
        assertEquals("Info", M1.getFiliere());
        assertEquals(new Date(1991, 9, 5), M1.getDateNaissanceSimple());
        assertEquals("IUP", M1.getEcoleActuelle());
        assertEquals("Grasse", M1.getVille());
        assertEquals("dieu", M1.getStatut());
        assertEquals(pwdByte, M1.getMdp());
        assertEquals("KBP@hotmail.fr", M1.getEmail());
    }

    @SuppressWarnings("deprecation")
    private void testReadBDD() {
        Membre M2 = Membre.readMembre(new String("ba900450"));
        assertNotNull(M2);
        assertEquals(new String("Alexandre"), M2.getPrenom());
        assertEquals("Boursier", M2.getNom());
        assertEquals("ba900450", M2.getNumeroEtudiant());
        assertEquals("S4T", M2.getPromotion());
        assertEquals("Informatique", M2.getFiliere());
        assertEquals(new Date(1992, 06, 15), M2.getDateNaissanceSimple());
        assertEquals("IUT Nice", M2.getEcoleActuelle());
        assertEquals("Nice", M2.getVille());
        assertEquals("etudiant", M2.getStatut());
        assertEquals("alexthebry@hotmail.fr", M2.getEmail());
    }

    @SuppressWarnings("deprecation")
    private void testUpdateBDD() {
        Membre M6;
        Date dat = new Date(1992, 06, 15);
        byte[] pwdByte = null;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            pwdByte = md5.digest("mdp1".getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        M6 = new Membre("ba90045", "Boursie", "Andre", "S4", "Iique", dat, "IT ", "ce", "eant", pwdByte, "alexhbyhmi.r");
        assertNotNull(M6);
        try {
            md5 = MessageDigest.getInstance("MD5");
            pwdByte = md5.digest("goulougoulou".getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        M6.updateMembre("nom", "prenom", "email", "ecole", new Date(1925, 6, 15), "filiere", "promotion", "statut", "ville", pwdByte);
        Membre M2 = Membre.readMembre("ba90045");
        assertEquals("ba90045", M2.getNumeroEtudiant());
        assertEquals(new String("prenom"), M2.getPrenom());
        assertEquals("nom", M2.getNom());
        assertEquals("promotion", M2.getPromotion());
        assertEquals("filiere", M2.getFiliere());
        assertEquals(new Date(1925, 6, 15), M2.getDateNaissanceSimple());
        assertEquals("ecole", M2.getEcoleActuelle());
        assertEquals("ville", M2.getVille());
        assertEquals("statut", M2.getStatut());
        assertEquals("email", M2.getEmail());
    }

    @SuppressWarnings("deprecation")
    private void testDeleteBDD() {
        Membre M1;
        Date dat = new Date(1992, 06, 15);
        byte[] pwdByte = null;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            pwdByte = md5.digest("mdp1".getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        M1 = new Membre("ba900458", "Boursier", "Alexandre", "S4T", "Informatique", dat, "IUT Nice", "Nice", "etudiant", pwdByte, "alexthebry@hotmail.fr");
        Membre M4 = Membre.readMembre("ba900458");
        assertNotNull(M4);
        M1.deleteMembre();
        M4 = Membre.readMembre("ba900458");
        assertNull(M4);
    }

    private void testClone() {
        Membre M1;
        Date dat = new Date(1992, 06, 15);
        byte[] pwdByte = null;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            pwdByte = md5.digest("mdp1".getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        M1 = new Membre("ba900450", "Boursier", "Alexandre", "S4T", "Informatique", dat, "IUT Nice", "Nice", "etudiant", pwdByte, "alexthebry@hotmail.fr");
        Membre clone = M1.clone();
        assertEquals(new String("Alexandre"), clone.getPrenom());
        assertEquals("Boursier", clone.getNom());
        assertEquals("ba900450", clone.getNumeroEtudiant());
        assertEquals("S4T", clone.getPromotion());
        assertEquals("Informatique", clone.getFiliere());
        assertEquals(new Date(1992, 06, 15), clone.getDateNaissanceSimple());
        assertEquals("IUT Nice", clone.getEcoleActuelle());
        assertEquals("Nice", clone.getVille());
        assertEquals("etudiant", clone.getStatut());
        assertEquals(pwdByte, clone.getMdp());
        assertEquals("alexthebry@hotmail.fr", M1.getEmail());
    }

    private void testEquals() {
        Membre M1;
        Date dat = new Date(1992, 06, 15);
        byte[] pwdByte = null;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            pwdByte = md5.digest("mdp1".getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        M1 = new Membre("ba900450", "Boursier", "Alexandre", "S4T", "Informatique", dat, "IUT Nice", "Nice", "etudiant", pwdByte, "alexthebry@hotmail.fr");
        assertNotNull(M1);
        Membre clone = M1.clone();
        assertNotNull(clone);
        assertTrue(M1.equals(clone));
        clone.setNom("Bogo");
        assertFalse(M1.equals(clone));
    }

    private void testGetAll() {
        List<Membre> membre = Membre.getAll();
        Iterator<Membre> iterator = membre.iterator();
        while (iterator.hasNext()) {
            Membre M = (Membre) iterator.next();
            Membre MTemp = Membre.readMembre(M.getNumeroEtudiant());
            assertEquals(M.getDateNaissance(), MTemp.getDateNaissance());
            assertEquals(M.getEcoleActuelle(), MTemp.getEcoleActuelle());
            assertEquals(M.getEmail(), MTemp.getEmail());
            assertEquals(M.getNom(), MTemp.getNom());
            assertEquals(M.getPrenom(), MTemp.getPrenom());
            assertEquals(M.getNom(), MTemp.getNom());
            assertEquals(M.getFiliere(), MTemp.getFiliere());
            assertEquals(M.getPromotion(), MTemp.getPromotion());
            assertEquals(M.getVille(), MTemp.getVille());
        }
    }

    private void testGetAllBy() {
        List<Membre> membre = Membre.getAllBy(1, 5);
        Iterator<Membre> iterator = membre.iterator();
        while (iterator.hasNext()) {
            Membre M = (Membre) iterator.next();
            Membre MTemp = Membre.readMembre(M.getNumeroEtudiant());
            assertEquals(M.getDateNaissance(), MTemp.getDateNaissance());
            assertEquals(M.getEcoleActuelle(), MTemp.getEcoleActuelle());
            assertEquals(M.getEmail(), MTemp.getEmail());
            assertEquals(M.getNom(), MTemp.getNom());
            assertEquals(M.getPrenom(), MTemp.getPrenom());
            assertEquals(M.getNom(), MTemp.getNom());
            assertEquals(M.getFiliere(), MTemp.getFiliere());
            assertEquals(M.getPromotion(), MTemp.getPromotion());
            assertEquals(M.getVille(), MTemp.getVille());
        }
    }

    private void testGetAllWhere() {
        List<Object> membre = Membre.getAllWhere(new String("ville"));
        Iterator<Object> iterator = membre.iterator();
        while (iterator.hasNext()) {
            Membre M = (Membre) iterator.next();
            Membre MTemp = Membre.readMembre(M.getNumeroEtudiant());
            assertEquals(M.getDateNaissance(), MTemp.getDateNaissance());
            assertEquals(M.getEcoleActuelle(), MTemp.getEcoleActuelle());
            assertEquals(M.getEmail(), MTemp.getEmail());
            assertEquals(M.getNom(), MTemp.getNom());
            assertEquals(M.getPrenom(), MTemp.getPrenom());
            assertEquals(M.getNom(), MTemp.getNom());
            assertEquals(M.getFiliere(), MTemp.getFiliere());
            assertEquals(M.getPromotion(), MTemp.getPromotion());
            assertEquals(M.getVille(), MTemp.getVille());
        }
    }

    @Test
    public void T_Constructeur_BDD() {
        testConstructeurGet();
        testConstructeurSimple();
        testConstructeurSet();
        testReadBDD();
        testUpdateBDD();
        testDeleteBDD();
    }

    @Test
    public void T_Clone() {
        testClone();
    }

    @Test
    public void T_Equals() {
        testEquals();
    }

    @Test
    public void T_getAllWhere() {
        testGetAllWhere();
    }

    @Test
    public void T_getAll() {
        testGetAll();
    }

    @Test
    public void T_getAllBy() {
        testGetAllBy();
    }
}
