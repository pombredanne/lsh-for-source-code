package net.taylor.audit.entity;

import java.util.Date;
import javax.persistence.EntityManager;
import junit.framework.Test;
import junit.framework.TestSuite;
import net.taylor.embedded.Bootstrap;
import net.taylor.testing.JpaCrudTest;

/**
 * Unit tests for the Activity.
 *
 * @author jgilbert
 * @generated
 */
public class ActivityTest extends JpaCrudTest<Activity> {

    /** @generated */
    public ActivityTest(String name) {
        super(name);
    }

    /** @generated */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new ActivityTest("testCrud"));
        return new Bootstrap(suite);
    }

    /** @generated */
    protected void initData(EntityManager em) throws Exception {
    }

    /** @generated */
    protected void prePersist(EntityManager em) throws Exception {
        ActivityBuilder builder = ActivityBuilder.instance();
        builder.type("type");
        builder.entityHandle("entityHandle");
        builder.user("user");
        builder.timestamp(new Date());
        builder.title("title");
        builder.digest("digest");
        builder.action(null);
        builder.url("url");
        builder.partition("partition");
        builder.dimension1("dimension1");
        builder.dimension2("dimension2");
        entity = builder.build();
    }

    /** @generated */
    protected void preMerge(EntityManager em) throws Exception {
    }

    /** @generated */
    protected void postMerge(EntityManager em) throws Exception {
    }

    /** @generated */
    protected void preRemove(EntityManager em) throws Exception {
    }
}
