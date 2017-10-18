package ru.adv.test.security.filter;

import org.junit.Assert;
import org.junit.Test;
import ru.adv.security.filter.ADVPrincipal;
import ru.adv.security.filter.AuthConfig;
import ru.adv.security.filter.Realm;
import ru.adv.test.AbstractTest;

public class MD5Test extends AbstractTest {

    @Test
    public void testMD5WithPostgres() {
        Realm realm = new SimpleRealm();
        String md5expected = "038d2263a60439fb7821eb792e58e8e4";
        logger.info(realm.digest("22uCxYT"));
        Assert.assertEquals(md5expected, realm.digest("22uCxYT"));
    }

    class SimpleRealm extends Realm {

        public SimpleRealm() {
            super(logger);
        }

        @Override
        public ADVPrincipal authenticate(String username, String credentials) {
            return null;
        }

        @Override
        protected String getName() {
            return null;
        }

        @Override
        protected void parseConfig(AuthConfig config) {
        }
    }
}
