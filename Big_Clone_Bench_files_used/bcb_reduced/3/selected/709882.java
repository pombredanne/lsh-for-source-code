package bg.price.comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.After;
import org.junit.Before;

/**
 * Provides initialization and cleanup of the test data for the unit tests that
 * test JPA operations in this project.
 * <p>
 * The initialization makes sure that the database connection is setup and that
 * the tables are created. It also initializes the <code>entityManager</code>
 * object that is later used by the concrete tests. It also makes sure that some
 * test data is inserted prior to executing the real tests.<br>
 * The clean up basically includes closing the database connection.
 * 
 * @author Ivan St. Ivanov
 */
public abstract class JpaTestFixture {

    private static final String DB_DIALECT = "org.hibernate.dialect.HSQLDialect";

    private static final String DB_DRIVER = "org.hsqldb.jdbcDriver";

    private static final String DB_URL = "jdbc:hsqldb:mem:testjpadb";

    private static final String DB_USER = "sa";

    private static final String DB_PASSWORD = "";

    private EntityManagerFactory emf = null;

    protected EntityManager entityManager = null;

    private List<PreparedStatement> statements = new ArrayList<PreparedStatement>();

    private List<ResultSet> resultSets = new ArrayList<ResultSet>();

    private Connection con = null;

    public JpaTestFixture() {
        final Map<String, String> entityManagerSettings = new HashMap<String, String>();
        entityManagerSettings.put("hibernate.connection.driver_class", DB_DRIVER);
        entityManagerSettings.put("hibernate.connection.url", DB_URL);
        entityManagerSettings.put("hibernate.dialect", DB_DIALECT);
        entityManagerSettings.put("hibernate.hbm2ddl.auto", "create-drop");
        entityManagerSettings.put("hibernate.connection.username", DB_USER);
        entityManagerSettings.put("hibernate.connection.password", DB_PASSWORD);
        entityManagerSettings.put("hibernate.show_sql", "true");
        emf = Persistence.createEntityManagerFactory("price.bg", entityManagerSettings);
        entityManager = emf.createEntityManager();
    }

    @Before
    public void setUp() throws Exception {
        createDatabaseConnection();
        insertTestData();
    }

    private void createDatabaseConnection() throws Exception {
        Class.forName(DB_DRIVER);
        con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected Connection getDatabaseConnection() {
        return con;
    }

    protected File getResourceDir() {
        return new File("./src/test/resources/test-res");
    }

    private void insertTestData() throws Exception {
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

    protected void registerStatement(PreparedStatement ps) {
        statements.add(ps);
    }

    protected void registerResultSet(ResultSet rs) {
        resultSets.add(rs);
    }

    private static final String INSERT_CATEGORY_STMT = "INSERT INTO CATEGORY (ID, NAME, DESCRIPTION, PARENT) VALUES (?, ?, ?, ?)";

    protected void insertCategory(int id, String name, String description) throws SQLException {
        PreparedStatement stmt = getDatabaseConnection().prepareStatement(INSERT_CATEGORY_STMT);
        stmt.setInt(1, id);
        stmt.setString(2, name);
        stmt.setString(3, description);
        registerStatement(stmt);
        stmt.executeUpdate();
    }

    protected void insertCategory(int id, String name, String description, int parent) throws SQLException {
        PreparedStatement stmt = getDatabaseConnection().prepareStatement(INSERT_CATEGORY_STMT);
        stmt.setInt(1, id);
        stmt.setString(2, name);
        stmt.setString(3, description);
        stmt.setInt(4, parent);
        registerStatement(stmt);
        stmt.executeUpdate();
    }

    private static final String INSERT_ESHOP_STMT = "INSERT INTO ESHOP (ID, NAME, DESCRIPTION, ADDRESS, EMAIL) VALUES (?, ?, ?, ?, ?)";

    protected void insertEshop(int id, String name, String description, String address, String email) throws SQLException {
        PreparedStatement stmt = getDatabaseConnection().prepareStatement(INSERT_ESHOP_STMT);
        stmt.setInt(1, id);
        stmt.setString(2, name);
        stmt.setString(3, description);
        stmt.setString(4, address);
        stmt.setString(5, email);
        registerStatement(stmt);
        stmt.executeUpdate();
    }

    private static final String INSERT_PRODUCT_STMT = "INSERT INTO PRODUCT (ID, NAME, DESCRIPTION, CATEGORY) VALUES (?, ?, ?, ?)";

    protected void insertProduct(int id, String name, String description, int category) throws SQLException {
        PreparedStatement stmt = getDatabaseConnection().prepareStatement(INSERT_PRODUCT_STMT);
        stmt.setInt(1, id);
        stmt.setString(2, name);
        stmt.setString(3, description);
        stmt.setInt(4, category);
        registerStatement(stmt);
        stmt.executeUpdate();
    }

    private static final String INSERT_PICTURE_STMT = "INSERT INTO PICTURES (ID, PICTURE, PRODUCT_ID) VALUES (?, ?, ?)";

    protected void insertProduct(int id, String name, String description, int category, File picture) throws SQLException, FileNotFoundException {
        getDatabaseConnection().setAutoCommit(false);
        PreparedStatement stmt = getDatabaseConnection().prepareStatement(INSERT_PRODUCT_STMT);
        stmt.setInt(1, id);
        stmt.setString(2, name);
        stmt.setString(3, description);
        stmt.setInt(4, category);
        stmt.executeUpdate();
        stmt = getDatabaseConnection().prepareStatement(INSERT_PICTURE_STMT);
        stmt.setInt(1, id);
        stmt.setBinaryStream(2, new FileInputStream(picture), (int) picture.length());
        stmt.setInt(3, id);
        registerStatement(stmt);
        stmt.executeUpdate();
        getDatabaseConnection().commit();
    }

    private static final String INSERT_OFFER_STMT = "INSERT INTO OFFER (ID, ESHOP_ID, PRODUCT_ID, PRICE) VALUES (?, ?, ?, ?)";

    protected void insertOffer(int id, int eshop, int product, double price) throws SQLException {
        PreparedStatement stmt = getDatabaseConnection().prepareStatement(INSERT_OFFER_STMT);
        stmt.setInt(1, id);
        stmt.setInt(2, eshop);
        stmt.setInt(3, product);
        stmt.setDouble(4, price);
        registerStatement(stmt);
        stmt.executeUpdate();
    }

    private static final String INSERT_USER_STMT = "INSERT INTO USER (USER_NAME, PASSWORD, EMAIL) VALUES (?, ?, ?)";

    protected void insertUser(String userName, String password, String email) throws SQLException {
        PreparedStatement stmt = getDatabaseConnection().prepareStatement(INSERT_USER_STMT);
        stmt.setString(1, userName);
        stmt.setString(2, password);
        stmt.setString(3, email);
        registerStatement(stmt);
        stmt.executeUpdate();
    }

    protected void assertObjectExistance(String sqlStmt, String errorMessage, boolean shouldExist, String... paramList) {
        try {
            Connection con = getDatabaseConnection();
            PreparedStatement stm = con.prepareStatement(sqlStmt);
            for (int i = 0; i < paramList.length; i++) {
                stm.setString(i + 1, paramList[i]);
            }
            registerStatement(stm);
            ResultSet rs = stm.executeQuery();
            registerResultSet(rs);
            assertEquals(shouldExist, rs.next());
        } catch (Exception e) {
            e.printStackTrace();
            fail(errorMessage);
        }
    }

    private static final String COUNT_STMT = "SELECT COUNT(*) FROM ";

    protected int getTotalEntries(String tableName) {
        try {
            PreparedStatement ps = getDatabaseConnection().prepareStatement(COUNT_STMT + tableName);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            fail("Could not find the total number of entries in table " + tableName);
            return -1;
        }
    }

    @After
    public void tearDown() throws Exception {
        emf.close();
        for (int i = 0; i < statements.size(); i++) {
            statements.get(i).close();
        }
        for (int i = 0; i < resultSets.size(); i++) {
            resultSets.get(i).close();
        }
        getDatabaseConnection().close();
    }

    protected static String getHash(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        return new String(digest.digest(password.getBytes("UTF-8")));
    }

    protected File getPictureFile() {
        return new File(getResourceDir(), "dsktp.jpg");
    }
}
