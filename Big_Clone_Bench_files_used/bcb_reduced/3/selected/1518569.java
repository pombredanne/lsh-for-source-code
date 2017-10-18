package com.jcorporate.expresso.core.dataobjects.jdbc.tests;

import com.jcorporate.expresso.core.dataobjects.jdbc.JoinedDigesterBean;
import junit.framework.TestCase;
import org.apache.commons.digester.Digester;
import java.net.URL;
import java.util.List;

/** JUnitTest case for class: com.jcorporate.expresso.core.dataobjects.jdbc.JoinedDigesterBean */
public class TestJoinedDigesterBean extends TestCase {

    private static final String TEST_FILE = "/com/jcorporate/" + "expresso/core/dataobjects/jdbc/tests/DownloadMimeTypeJoin.xml";

    public TestJoinedDigesterBean(String _name) {
        super(_name);
    }

    /** test for method buildDigester(..) */
    public void testBuildDigester() {
        try {
            JoinedDigesterBean bean = new JoinedDigesterBean();
            Digester d = bean.buildDigester();
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Error building digester");
        }
    }

    /** test for method digest(..) */
    public void testDigest() {
        JoinedDigesterBean bean = new JoinedDigesterBean();
        Digester d = bean.buildDigester();
        URL file = TestJoinedDigesterBean.class.getResource(TEST_FILE);
        if (file == null) {
            throw new IllegalArgumentException("Error getting Test join xml file");
        }
        try {
            bean.digest(d, file);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Error digesting data");
        }
        runDataIntegrityTest(bean);
    }

    /** test for method loadJoinData(..) */
    public void testLoadJoinData() {
        JoinedDigesterBean bean = new JoinedDigesterBean();
        URL file = TestJoinedDigesterBean.class.getResource(TEST_FILE);
        if (file == null) {
            throw new IllegalArgumentException("Error getting Test join xml file");
        }
        try {
            bean.loadJoinData(file);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Error digesting data");
        }
        runDataIntegrityTest(bean);
    }

    private void runDataIntegrityTest(JoinedDigesterBean theBean) {
        assertTrue("Download Log View".equals(theBean.getDescription()));
        assertTrue(theBean.isDistinct() == false);
        List dataobjects = theBean.getDataObjects();
        List relations = theBean.getRelations();
        assertTrue("dataobjects should not be null", dataobjects != null);
        assertTrue("relations should not be null", relations != null);
        assertTrue("dataobjects should have elements", dataobjects.size() > 0);
        assertTrue("relations should have elements", relations.size() > 0);
    }

    /** Executes the test case */
    public static void main(String[] argv) {
        String[] testCaseList = { TestJoinedDigesterBean.class.getName() };
        junit.textui.TestRunner.main(testCaseList);
    }
}
