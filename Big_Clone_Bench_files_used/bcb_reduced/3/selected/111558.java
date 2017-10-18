package jimagick.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import jimagick.utils.ImageListCell.ElementType;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ImageListCellTest extends TestCase {

    private static final Logger logger = Logger.getLogger(ImageListCellTest.class);

    private static final File photo = new File(JIConfig.class.getResource("/img/folder1/cows.jpg").getFile());

    private ImageListCell ilc;

    private String md5String;

    private File thumbFile;

    @Before
    public void setUp() throws Exception {
        PropertyConfigurator.configure(JIConfig.configFileName);
        ilc = new ImageListCell(photo);
        JIConfigurator.setWorkingDir(System.getProperty("user.dir") + "/.jimagick/");
        MessageDigest digest = MessageDigest.getInstance("MD5");
        md5String = null;
        FileInputStream is = new FileInputStream(photo);
        byte[] buffer = new byte[8192];
        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        byte[] md5sum = digest.digest();
        BigInteger bigInt = new BigInteger(1, md5sum);
        md5String = bigInt.toString(16);
        String thumbName = JIConfigurator.instance().getProperties().getProperty(JIConfigurator.JIMAGICK_WORKING_DIR, System.getProperty("user.home") + "/.jimagick/") + "thumbs/" + md5String + ".jpg";
        thumbFile = new File(thumbName);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testImageListCell() {
        assertNotNull(ilc);
        assertEquals(photo, ilc.getSource());
    }

    @Test
    public final void testSetParam() {
        ilc.setParam(thumbFile, md5String);
        assertEquals(md5String, ilc.getMd5());
        assertEquals(thumbFile, ilc.getThumbFile());
    }

    @Test
    public final void testCreateImageElement() {
        ilc.setParam(thumbFile, md5String);
        ilc.createImageElement();
        Document doc = DomXml.instance().getXmlDoc();
        NodeList nl = doc.getElementsByTagName("img");
        assertNotNull(nl);
        String md5Attr = new String();
        String filenameAttr = new String();
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            if (e.getAttribute("md5") == md5String) {
                md5Attr = e.getAttribute("md5");
                filenameAttr = e.getAttribute("filename");
                break;
            }
        }
        assertEquals(md5String, md5Attr);
        assertEquals(photo.getAbsolutePath(), filenameAttr);
    }

    @Test
    public final void testLoadXmlTag() {
        ilc.setParam(thumbFile, md5String);
        ilc.createImageElement();
        Document doc = DomXml.instance().getXmlDoc();
        NodeList nl = doc.getElementsByTagName("img");
        assertNotNull(nl);
        String md5Attr = new String();
        String filenameAttr = new String();
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            if (e.getAttribute("md5") == md5String) {
                md5Attr = e.getAttribute("md5");
                filenameAttr = e.getAttribute("filename");
                break;
            }
        }
        ilc.LoadXmlTag(doc);
        assertEquals(md5String, md5Attr);
        assertEquals(photo.getAbsolutePath(), filenameAttr);
    }

    @Test
    public final void testAddTag() {
        ilc.setParam(thumbFile, md5String);
        ilc.createImageElement();
        ilc.addTag("tag1", new Rectangle(0, 0, 10, 10));
        ilc.addTag("tag2", new Rectangle(10, 10, 20, 20));
        ilc.addTag("tag3", new Rectangle(20, 20, 30, 30));
        Element e1 = ilc.getTagElement("tag1");
        Element e2 = ilc.getTagElement("tag2");
        Element e3 = ilc.getTagElement("tag3");
        assertEquals(e1.getAttribute("name"), "tag1");
        assertEquals(e1.getAttribute("x"), "0");
        assertEquals(e1.getAttribute("y"), "0");
        assertEquals(e1.getAttribute("width"), "10");
        assertEquals(e1.getAttribute("height"), "10");
        assertEquals(e2.getAttribute("name"), "tag2");
        assertEquals(e2.getAttribute("x"), "10");
        assertEquals(e2.getAttribute("y"), "10");
        assertEquals(e2.getAttribute("width"), "20");
        assertEquals(e2.getAttribute("height"), "20");
        assertEquals(e3.getAttribute("name"), "tag3");
        assertEquals(e3.getAttribute("x"), "20");
        assertEquals(e3.getAttribute("y"), "20");
        assertEquals(e3.getAttribute("width"), "30");
        assertEquals(e3.getAttribute("height"), "30");
    }

    @Test
    public final void testRemoveTag() {
        ilc.setParam(thumbFile, md5String);
        ilc.createImageElement();
        ilc.addTag("tag1", new Rectangle(0, 0, 10, 10));
        ilc.addTag("tag2", new Rectangle(10, 10, 20, 20));
        ilc.addTag("tag3", new Rectangle(20, 20, 30, 30));
        assertEquals(3, ilc.getTags().size());
        ilc.removeTag("tag1");
        assertEquals(2, ilc.getTags().size());
        ilc.removeTag("tag2");
        assertEquals(1, ilc.getTags().size());
        ilc.removeTag("tag3");
        assertEquals(null, ilc.getTags());
    }

    @Test
    public final void testModifyTag() {
        ilc.setParam(thumbFile, md5String);
        ilc.createImageElement();
        ilc.addTag("tag1", new Rectangle(0, 0, 10, 10));
        ilc.addTag("tag2", new Rectangle(10, 10, 20, 20));
        ilc.addTag("tag3", new Rectangle(20, 20, 30, 30));
        ilc.modifyTag("tag_a", "tag1");
        ilc.modifyTag("tag_b", "tag2");
        ilc.modifyTag("tag_c", "tag3");
        Element e1 = ilc.getTagElement("tag_a");
        assertNotNull(e1);
        assertEquals(e1.getAttribute("name"), "tag_a");
        Element e2 = ilc.getTagElement("tag_b");
        assertNotNull(e2);
        assertEquals(e2.getAttribute("name"), "tag_b");
        Element e3 = ilc.getTagElement("tag_c");
        assertNotNull(e3);
        assertEquals(e3.getAttribute("name"), "tag_c");
    }

    @Test
    public final void testGetTags() {
        ilc.setParam(thumbFile, md5String);
        ilc.createImageElement();
        ilc.addTag("tag1", new Rectangle(0, 0, 10, 10));
        ilc.addTag("tag2", new Rectangle(10, 10, 20, 20));
        ilc.addTag("tag3", new Rectangle(20, 20, 30, 30));
        ArrayList<String> tags = ilc.getTags();
        assertNotNull(tags);
        assertEquals(3, tags.size());
    }

    @Test
    public final void testGetCategories() {
        ilc.setParam(thumbFile, md5String);
        ilc.createImageElement();
        ilc.addCategory("categoria1");
        ilc.addCategory("categoria2");
        Element e = ilc.getElement();
        NodeList nl = e.getElementsByTagName("category");
        assertEquals(2, nl.getLength());
        for (int i = 0; i < nl.getLength(); i++) {
            Element category = (Element) nl.item(i);
            assertEquals(true, category.getAttribute("name").startsWith("cat"));
        }
        ArrayList<String> cats = ilc.getCategories();
        assertEquals(true, cats.contains("categoria1"));
        assertEquals(true, cats.contains("categoria2"));
    }

    @Test
    public final void testRemoveCategory() {
        ilc.addCategory("categoria1");
        ilc.addCategory("categoria2");
        assertEquals(true, ilc.exists("categoria1", ElementType.Category));
        assertEquals(true, ilc.exists("categoria2", ElementType.Category));
        ilc.removeCategory("categoria1");
        ilc.removeCategory("categoria2");
        assertEquals(false, ilc.exists("categoria1", ElementType.Category));
        assertEquals(false, ilc.exists("categoria2", ElementType.Category));
    }
}
