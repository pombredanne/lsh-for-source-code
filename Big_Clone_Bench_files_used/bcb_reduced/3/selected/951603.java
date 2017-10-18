package ru.adv.test.security.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.adv.security.filter.ADVPrincipal;
import ru.adv.security.filter.AuthConfig;
import ru.adv.security.filter.JDBCRealm;
import ru.adv.security.filter.Realm;
import ru.adv.security.filter.XMLAuthConfig;
import ru.adv.test.AbstractTest;
import ru.adv.xml.parser.Parser;

public class JDBCRealmTest extends AbstractTest {

    private static final String PASSWORD = "";

    private static final String USER = "sa";

    private static final String JDBC_MEM_TESTEDB_URL = "jdbc:hsqldb:mem:testedb";

    private static final String HSQLDB_JDBC_DRIVER = "org.hsqldb.jdbcDriver";

    private static final String HEADER = "<adv-filter><uri-map>/xxx/*</uri-map><login-config pass-local-requests=\"yes\"><auth-method>FORM</auth-method></login-config><auth-constraint/>";

    private static final String FOOTER = "</adv-filter>";

    private static String CONF1 = PASSWORD + "<realm  " + " className=\"org.apache.catalina.realm.JDBCRealm\" \n" + " driverName=\"" + HSQLDB_JDBC_DRIVER + "\"\n" + " connectionURL=\"" + JDBC_MEM_TESTEDB_URL + "\"\n" + " connectionName=\"" + USER + "\" " + " connectionPassword=\"" + PASSWORD + "\"\n" + " userTable=\"users\" " + " userNameCol=\"username\" " + " userCredCol=\"passwd\"\n" + " userRoleTable=\"usegroup\" " + " roleNameCol=\"groname\" \n" + " digest=\"md5\" \n" + " cacheTTL=\"3\" \n" + " debug=\"2\" \n" + "\t/>";

    private static String AUTH_CONFIG = HEADER + CONF1 + FOOTER;

    private AuthConfig authConfig;

    /** in memory hsql connection*/
    private Connection connection;

    @Before
    public void setUp() throws Exception {
        this.authConfig = new XMLAuthConfig(logger, null, new Parser().parse(AUTH_CONFIG).getDocument().getDocumentElement());
        this.authConfig.init();
        Class.forName(HSQLDB_JDBC_DRIVER);
        this.connection = DriverManager.getConnection(JDBC_MEM_TESTEDB_URL, USER, PASSWORD);
        Statement st = this.connection.createStatement();
        st.executeUpdate("CREATE TABLE users ( id INTEGER IDENTITY, username VARCHAR(15), passwd VARCHAR(15) )");
        String passwdMD5 = Realm.md5("secret");
        st.executeUpdate("insert into users (id,username,passwd) values (1,'testrealm','" + passwdMD5 + "')");
        st.executeUpdate("CREATE TABLE usegroup ( id INTEGER IDENTITY, username VARCHAR(15), groname VARCHAR(15) )");
        st.executeUpdate("insert into usegroup (id,username,groname) values (1,'testrealm','admin')");
        st.executeUpdate("insert into usegroup (id,username,groname) values (2,'testrealm','testadmin')");
        st.executeUpdate("insert into usegroup (id,username,groname) values (3,'testrealm','adv')");
        st.execute("commit");
        st.close();
    }

    @After
    public void tearDown() throws Exception {
        authConfig = null;
        Statement st = connection.createStatement();
        st.execute("SHUTDOWN");
        connection.close();
    }

    @Test
    public void testConfig() throws Exception {
        JDBCRealmFake jdbcRealm = new JDBCRealmFake();
        jdbcRealm.init(authConfig);
        assertTrue(authConfig.isPassLocalRequests());
        assertEquals(USER, jdbcRealm.getConnectionName());
        assertEquals(PASSWORD, jdbcRealm.getConnectionPassword());
        assertEquals(JDBC_MEM_TESTEDB_URL, jdbcRealm.getConnectionURL());
        assertEquals(HSQLDB_JDBC_DRIVER, jdbcRealm.getDriverName());
        assertEquals("groname", jdbcRealm.getRoleNameCol());
        assertEquals("passwd", jdbcRealm.getUserCredCol());
        assertEquals("username", jdbcRealm.getUserNameCol());
        assertEquals("usegroup", jdbcRealm.getUserRoleTable());
        assertEquals("users", jdbcRealm.getUserTable());
        assertEquals(true, jdbcRealm.isHasMessageDigest());
        assertEquals("f52181e0b1065054f15ff683e4519085", jdbcRealm.digest("Bla-Bla-Bla"));
    }

    @Test
    public void testAuth() throws Exception {
        JDBCRealmFake jdbcRealm = new JDBCRealmFake();
        jdbcRealm.init(authConfig);
        ADVPrincipal principal = (ADVPrincipal) jdbcRealm.authenticate("testrealm", "secret");
        if (principal == null) {
            fail("Principal \"testrealm\" is not found");
        }
        assertEquals("testrealm", principal.getName());
        assertEquals(arr2set(new String[] { "admin", "testadmin", "adv" }), principal.getRoleNames());
        principal = (ADVPrincipal) jdbcRealm.authenticate("testrealm", "badpasswd");
        if (principal != null) {
            fail("Must be null here");
        }
    }

    @Test
    public void testCache() throws Exception {
        JDBCRealmFake jdbcRealm = new JDBCRealmFake();
        jdbcRealm.init(authConfig);
        ADVPrincipal principal = (ADVPrincipal) jdbcRealm.authenticate("testrealm", "secret");
        if (principal == null) {
            fail("Principal \"testrealm\" is not found");
        }
        assertEquals("testrealm", principal.getName());
        assertEquals(arr2set(new String[] { "admin", "testadmin", "adv" }), principal.getRoleNames());
        principal = (ADVPrincipal) jdbcRealm.authenticate("testrealm", "badpasswd");
        if (principal != null) {
            fail("Must be null here");
        }
        principal = (ADVPrincipal) jdbcRealm.authenticate("testrealm", "secret");
        principal = (ADVPrincipal) jdbcRealm.authenticate("testrealm", "secret");
        assertTrue(principal.isCached());
        Thread.sleep(3001);
        principal = (ADVPrincipal) jdbcRealm.authenticate("testrealm", "secret");
        assertTrue(!principal.isCached());
        principal = (ADVPrincipal) jdbcRealm.authenticate("testrealm", "secret");
        assertTrue(principal.isCached());
    }

    private Set<String> arr2set(String[] strs) {
        Set<String> s = new HashSet<String>();
        for (int i = 0; i < strs.length; i++) {
            s.add(strs[i]);
        }
        return s;
    }

    class JDBCRealmFake extends JDBCRealm {

        public JDBCRealmFake() {
            super(logger);
        }

        protected String getConnectionName() {
            return super.getConnectionName();
        }

        protected String getConnectionPassword() {
            return super.getConnectionPassword();
        }

        protected String getConnectionURL() {
            return super.getConnectionURL();
        }

        protected String getDriverName() {
            return super.getDriverName();
        }

        protected String getRoleNameCol() {
            return super.getRoleNameCol();
        }

        protected String getUserCredCol() {
            return super.getUserCredCol();
        }

        protected String getUserNameCol() {
            return super.getUserNameCol();
        }

        protected String getUserRoleTable() {
            return super.getUserRoleTable();
        }

        protected String getUserTable() {
            return super.getUserTable();
        }
    }
}
