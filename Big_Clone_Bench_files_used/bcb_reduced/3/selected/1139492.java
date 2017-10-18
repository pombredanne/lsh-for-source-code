package com.carbonfive.flashgateway.security.config;

import javax.servlet.http.*;
import junit.framework.*;
import org.apache.commons.logging.*;
import com.carbonfive.flashgateway.security.*;
import com.carbonfive.flashgateway.security.config.*;

public class ConfigDigesterTest extends TestCase {

    private static final Log log = LogFactory.getLog(ConfigDigesterTest.class);

    public ConfigDigesterTest(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ConfigDigesterTest.class);
        return suite;
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testDigest() throws Exception {
        Config config = ConfigDigester.digest("flashgatekeeper.xml");
        assertNotNull(config);
        log.info("Created \n" + config);
    }
}
