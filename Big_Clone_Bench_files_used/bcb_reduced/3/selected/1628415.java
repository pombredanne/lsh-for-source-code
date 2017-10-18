package bg.price.comparator.dao.jpa;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Before;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

@ContextConfiguration(locations = { "/META-INF/spring/applicatonContext.xml", "file:./src/test/resources/META-INF/spring/pricebg-test-datasource.xml" })
public abstract class SpringTestFixture extends AbstractTransactionalJUnit4SpringContextTests {

    @PersistenceContext
    protected EntityManager entityManager;

    @Before
    public void insertTestData() throws Exception {
        insertCategory(1000001, "photo", "photo cameras");
        insertCategory(1000002, "computers", "all kinds of computers");
        insertCategory(1000003, "laptops", "portable computers", 1000002);
        insertEshop(1234, "happyshop", "A very happy shop", "solunska_and_angel_kanchev", "offers@happyshop.bg");
        insertEshop(1235, "crazyshop", "A crazy shop", "slatina_blok_20", "offers@crazyshop.bg");
        insertProduct(10001, "IBM Lenovo", "1.6 GHz dual core", 1000003);
        insertProduct(10002, "Fujitsu Siemens", "My computer", 1000003);
        insertProduct(10003, "Cannon", "2 megapixels", 1000001);
        insertProduct(10004, "Image", "Wales", 1000001, getPictureFile());
        insertOffer(6900001, 1234, 10002, 2000);
        insertOffer(6900002, 1234, 10003, 800);
        insertOffer(6900003, 1235, 10002, 1900);
        insertUser("ivko", getHash("ivko"), "ivko@price.bg");
    }

    private static final String INSERT_CATEGORY_STMT = "INSERT INTO CATEGORY (ID, NAME, DESCRIPTION, PARENT) VALUES (?, ?, ?, ?)";

    protected void insertCategory(int id, String name, String description) {
        this.insertCategory(id, name, description, null);
    }

    protected void insertCategory(int id, String name, String description, Integer parent) {
        simpleJdbcTemplate.update(INSERT_CATEGORY_STMT, id, name, description, parent);
    }

    private static final String INSERT_ESHOP_STMT = "INSERT INTO ESHOP (ID, NAME, DESCRIPTION, ADDRESS, EMAIL) VALUES (?, ?, ?, ?, ?)";

    protected void insertEshop(int id, String name, String description, String address, String email) {
        simpleJdbcTemplate.update(INSERT_ESHOP_STMT, id, name, description, address, email);
    }

    private static final String INSERT_PRODUCT_STMT = "INSERT INTO PRODUCT (ID, NAME, DESCRIPTION, CATEGORY) VALUES (?, ?, ?, ?)";

    protected void insertProduct(int id, String name, String description, int category) {
        simpleJdbcTemplate.update(INSERT_PRODUCT_STMT, id, name, description, category);
    }

    private static final String INSERT_PICTURE_STMT = "INSERT INTO PICTURES (ID, PICTURE, PRODUCT_ID) VALUES (?, ?, ?)";

    protected void insertProduct(int id, String name, String description, int category, File picture) throws IOException {
        simpleJdbcTemplate.update(INSERT_PRODUCT_STMT, id, name, description, category);
        InputStream pictureStream = new FileInputStream(picture);
        Map<String, Object> parameters = new HashMap<String, Object>(3);
        parameters.put("ID", id);
        parameters.put("PICTURE", pictureStream);
        parameters.put("PRODUCT_ID", id);
        simpleJdbcTemplate.update(INSERT_PICTURE_STMT, id, getBytesFromFile(picture), id);
        System.out.println(simpleJdbcTemplate.queryForList("SELECT * FROM PICTURES", new Object[] {}));
    }

    public byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        is.close();
        return bytes;
    }

    private static final String INSERT_OFFER_STMT = "INSERT INTO OFFER (ID, ESHOP_ID, PRODUCT_ID, PRICE) VALUES (?, ?, ?, ?)";

    protected void insertOffer(int id, int eshop, int product, double price) {
        simpleJdbcTemplate.update(INSERT_OFFER_STMT, id, eshop, product, price);
    }

    static final String INSERT_USER_STMT = "INSERT INTO USER (USER_NAME, PASSWORD, EMAIL) VALUES (?, ?, ?)";

    protected void insertUser(String userName, String password, String email) {
        simpleJdbcTemplate.update(INSERT_USER_STMT, userName, password, email);
    }

    protected void assertObjectExistance(String sqlStmt, String errorMessage, boolean shouldExist, Object... paramList) {
        int result = simpleJdbcTemplate.queryForInt(sqlStmt, paramList);
        assertEquals(shouldExist, result != 0);
    }

    private static final String COUNT_STMT = "SELECT COUNT(*) FROM ";

    protected int getTotalEntries(String tableName) {
        return simpleJdbcTemplate.queryForInt(COUNT_STMT + tableName);
    }

    protected static String getHash(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        return new String(digest.digest(password.getBytes("UTF-8")));
    }

    protected File getResourceDir() {
        return new File("./src/test/resources/test-res");
    }

    protected File getPictureFile() {
        return new File(getResourceDir(), "dsktp.jpg");
    }
}
