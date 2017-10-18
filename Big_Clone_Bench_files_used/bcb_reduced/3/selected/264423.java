package test.es.alvsanand.webpage.services;

import java.util.Properties;
import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import es.alvsanand.webpage.common.AlvsanandProperties;
import es.alvsanand.webpage.model.User;
import es.alvsanand.webpage.services.security.CryptographyService;
import es.alvsanand.webpage.services.security.CryptographyServiceImpl;
import es.alvsanand.webpage.services.session.UserService;
import es.alvsanand.webpage.services.session.UserServiceImpl;

public class UserServiceTest {

    private final LocalServiceTestHelper helper;

    public UserServiceTest() {
        LocalDatastoreServiceTestConfig localServiceTestConfig = new LocalDatastoreServiceTestConfig();
        localServiceTestConfig.setNoStorage(true);
        helper = new LocalServiceTestHelper(localServiceTestConfig);
    }

    private UserService userService;

    private transient CryptographyService cryptographyService;

    @Before
    public void setUp() throws Exception {
        helper.setUp();
        userService = new UserServiceImpl();
        cryptographyService = new CryptographyServiceImpl();
        Properties properties = new Properties();
        properties.loadFromXML(ImageAdminServiceTest.class.getResourceAsStream("/" + AlvsanandProperties.CONFIG_FILE_NAME));
        AlvsanandProperties.setProperties(properties);
    }

    @After
    public void tearDown() {
        helper.tearDown();
    }

    @Test
    public void sendActivationEmail() throws Exception {
        User user = new User();
        user.setName("UserTest");
        user.setEmail("foo@foo.com");
        user.setPassword("FOO");
        String registrationHash = es.alvsanand.webpage.common.StringUtils.getValidName(new String(Base64.encodeBase64(cryptographyService.digest((user.getPassword() + Math.random()).getBytes()))));
        user.setRegistrationHash(registrationHash);
        userService.saveUser(user);
        userService.sendActivationEmail(user);
    }

    @Test
    public void sendResetPasswordEmail() throws Exception {
        User user = new User();
        user.setName("UserTest");
        user.setEmail("foo@foo.com");
        user.setPassword("FOO");
        String plainPassword = es.alvsanand.webpage.common.StringUtils.generateRandomString();
        userService.saveUser(user);
        userService.sendResetPasswordEmail(user, plainPassword);
    }
}
