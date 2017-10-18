package tw.bennu.feeler.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import junit.framework.Assert;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import tw.bennu.feeler.db.FeelerUserImpl;
import tw.bennu.feeler.db.service.DbService;
import tw.bennu.feeler.db.service.IFeelerUser;
import tw.bennu.feeler.log.service.LogService;

public class DbUserServiceImplTest {

    private DbUserServiceImpl userServ = null;

    @Before
    public void before() {
        userServ = new DbUserServiceImpl();
    }

    @Test
    public void testRegister() {
        try {
            String username = "muchu";
            String password = "123";
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            String passwordMd5 = new String(md5.digest());
            LogService logServiceMock = EasyMock.createMock(LogService.class);
            DbService dbServiceMock = EasyMock.createMock(DbService.class);
            userServ.setDbServ(dbServiceMock);
            userServ.setLogger(logServiceMock);
            IFeelerUser user = new FeelerUserImpl();
            user.setUsername(username);
            user.setPassword(passwordMd5);
            logServiceMock.info(DbUserServiceImpl.class, ">>>rigister " + username + "<<<");
            EasyMock.expect(dbServiceMock.queryFeelerUser(username)).andReturn(null);
            dbServiceMock.addFeelerUser(username, passwordMd5);
            logServiceMock.info(DbUserServiceImpl.class, ">>>identification " + username + "<<<");
            EasyMock.expect(dbServiceMock.queryFeelerUser(username)).andReturn(user);
            EasyMock.replay(dbServiceMock, logServiceMock);
            Assert.assertTrue(userServ.register(username, password));
            EasyMock.verify(dbServiceMock, logServiceMock);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIdentification() {
        try {
            String username = "muchu";
            String password = "123";
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            LogService logServiceMock = EasyMock.createMock(LogService.class);
            DbService dbServiceMock = EasyMock.createMock(DbService.class);
            userServ.setDbServ(dbServiceMock);
            userServ.setLogger(logServiceMock);
            logServiceMock.info(DbUserServiceImpl.class, ">>>identification " + username + "<<<");
            IFeelerUser user = new FeelerUserImpl();
            user.setUsername(username);
            user.setPassword(new String(md5.digest()));
            EasyMock.expect(dbServiceMock.queryFeelerUser(username)).andReturn(user);
            EasyMock.replay(logServiceMock, dbServiceMock);
            Assert.assertTrue(userServ.identification(username, password));
            EasyMock.verify(logServiceMock, dbServiceMock);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
