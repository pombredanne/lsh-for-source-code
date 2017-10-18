package test.es.alvsanand.webpage.services;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import es.alvsanand.webpage.model.User;
import es.alvsanand.webpage.model.UserState;
import es.alvsanand.webpage.services.admin.UserAdminService;
import es.alvsanand.webpage.services.admin.UserAdminServiceImpl;
import es.alvsanand.webpage.services.security.CryptographyService;
import es.alvsanand.webpage.services.security.CryptographyServiceImpl;
import es.alvsanand.webpage.services.security.LoginService;
import es.alvsanand.webpage.services.security.LoginServiceImpl;

public class LoginServiceTest {

    private final LocalServiceTestHelper helper;

    public LoginServiceTest() {
        LocalDatastoreServiceTestConfig localServiceTestConfig = new LocalDatastoreServiceTestConfig();
        localServiceTestConfig.setNoStorage(true);
        helper = new LocalServiceTestHelper(localServiceTestConfig);
    }

    private CryptographyService cryptographyService;

    private UserAdminService userAdminService;

    private LoginService loginService;

    @Before
    public void setUp() throws Exception {
        helper.setUp();
        cryptographyService = new CryptographyServiceImpl();
        userAdminService = new UserAdminServiceImpl();
        loginService = new LoginServiceImpl();
    }

    @After
    public void tearDown() {
        helper.tearDown();
    }

    @Test
    public void testLoginByUsername() throws Exception {
        String loginName = "loginName";
        String password = new String(Base64.encodeBase64(cryptographyService.digest("password".getBytes())));
        User user = new User();
        user.setLoginName(loginName);
        user.setPassword(password);
        user.setState(UserState.ACCEPTED.ordinal());
        userAdminService.saveOrUpdateUser(user);
        loginService.loginByUsername(loginName, password);
    }
}
