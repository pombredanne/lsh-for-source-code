package jimagick.utils;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

/**
 *
 */
public class DomXmlTest extends TestCase {

    ArrayList<String> categories;

    private static final File photo1 = new File(JIConfig.class.getResource("/img/folder1/cows.jpg").getFile());

    private static final File photo2 = new File(JIConfig.class.getResource("/img/folder1/sheeps.jpg").getFile());

    /**
	 * @throws java.lang.Exception
	 */
    @Before
    public void setUp() throws Exception {
        JIConfigurator.setWorkingDir(System.getProperty("user.dir") + "/.jimagick/");
    }

    /**
	 * @throws java.lang.Exception
	 */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testInstance() {
        assertNotNull(DomXml.instance());
    }

    @Test
    public final void testReset() {
        DomXml.reset();
        assertNotNull(DomXml.instance());
    }

    @Test
    public final void testCreateDoc() {
        assertNotNull(DomXml.instance().getXmlDoc());
    }

    @Test
    public final void testAddCategory() {
        DomXml.addCategory("mare");
        DomXml.addCategory("montagna");
        categories = DomXml.loadAllCategories();
        assertNotNull(categories);
        assertEquals(true, categories.contains("mare") && categories.contains("montagna"));
    }

    @Test
    public final void testRemoveCategory() {
        DomXml.removeCategory("mare");
        DomXml.removeCategory("montagna");
        categories = DomXml.loadAllCategories();
        assertNotNull(categories);
        assertEquals(false, categories.contains("mare") || categories.contains("montagna"));
    }

    @Test
    public final void testIsJimagickSource() {
        File file = new File(System.getProperty("user.dir") + "/.jimagick/tags.xml");
        assertEquals(true, DomXml.isJimagickSource(file));
    }

    @Test
    public final void testCreateRect() {
        Element tag;
        tag = DomXml.instance().getXmlDoc().createElement("tag");
        tag.setAttribute("name", "tag1");
        tag.setAttribute("x", "0");
        tag.setAttribute("y", "0");
        tag.setAttribute("width", "200");
        tag.setAttribute("height", "200");
        Rectangle rect = new Rectangle(0, 0, 200, 200);
        Rectangle rect1 = DomXml.createRect(tag);
        assertEquals(rect.x, rect1.x);
        assertEquals(rect.y, rect1.y);
        assertEquals(rect.width, rect1.width);
        assertEquals(rect.height, rect1.height);
    }

    @Test
    public final void testSearch() {
        addPhotos();
        ArrayList<ImageListCell> list = DomXml.getTaggedORCategorizedImages();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public final void testSearchByTagAndCategory() {
        ArrayList<String> tagArray = new ArrayList<String>();
        tagArray.add("Tag2");
        tagArray.add("Tag3");
        ArrayList<String> catArray = new ArrayList<String>();
        ArrayList<ImageListCell> searchResult = DomXml.searchByTagAndCategory(tagArray, catArray);
        assertNotNull(searchResult);
        assertEquals(searchResult.size(), 2);
        tagArray.add("Tag1");
        searchResult = DomXml.searchByTagAndCategory(tagArray, catArray);
        assertNotNull(searchResult);
        assertEquals(searchResult.size(), 1);
        catArray.add("categoria1");
        searchResult = DomXml.searchByTagAndCategory(tagArray, catArray);
        assertNotNull(searchResult);
        assertEquals(searchResult.size(), 1);
    }

    public final void testLoad() {
        assertNotNull(DomXml.load());
    }

    private void addPhotos() {
        ImageListCell ilc;
        String md5;
        String thumbName;
        Element e;
        ilc = new ImageListCell(photo1);
        md5 = getMd5(photo1);
        thumbName = getThumbName(md5);
        ilc.setParam(new File(thumbName), md5);
        ilc.createImageElement();
        ilc.addTag("Tag1", new Rectangle());
        ilc.addTag("Tag2", new Rectangle());
        ilc.addTag("Tag3", new Rectangle());
        e = ilc.getElement();
        Element cat1 = (Element) DomXml.instance().getXmlDoc().createElement("category");
        cat1.setAttribute("name", "categoria1");
        e.appendChild(cat1);
        ilc = new ImageListCell(photo2);
        md5 = getMd5(photo2);
        thumbName = getThumbName(md5);
        ilc.setParam(new File(thumbName), md5);
        ilc.createImageElement();
        ilc.addTag("Tag2", new Rectangle());
        ilc.addTag("Tag3", new Rectangle());
        e = ilc.getElement();
        e.appendChild(cat1.cloneNode(true));
    }

    @Test
    public final void testGetTagFile() {
        String path = JIConfigurator.instance().getProperties().getProperty(JIConfigurator.JIMAGICK_WORKING_DIR) + "tags.xml";
        assertEquals(new File(path), DomXml.getTagFile());
    }

    @Test
    public final void testClean() {
        System.out.println("TearDown");
        String wd = JIConfigurator.instance().getProperties().getProperty(JIConfigurator.JIMAGICK_WORKING_DIR);
        DomXml.save(DomXml.instance().getXmlDoc());
        deleteDir(new File(wd));
    }

    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                System.out.println(children[i].toString());
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    private String getMd5(File photo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            String md5String = null;
            FileInputStream is = new FileInputStream(photo);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            md5String = bigInt.toString(16);
            return md5String;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getThumbName(String md5String) {
        String thumbName = JIConfigurator.instance().getProperties().getProperty(JIConfigurator.JIMAGICK_WORKING_DIR, System.getProperty("user.home") + "/.jimagick/") + "thumbs/" + md5String + ".jpg";
        return thumbName;
    }
}
