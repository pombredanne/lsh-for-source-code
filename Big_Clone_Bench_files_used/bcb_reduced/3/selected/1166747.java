package info.noahcampbell.meter.management.plugin;

import info.noahcampbell.meter.management.reader.RraConfiguredGraph;
import info.noahcampbell.meter.management.reader.RraReference;
import info.noahcampbell.meter.messenger.accumulator.Accumulator;
import info.noahcampbell.meter.messenger.accumulator.AccumulatorManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.relation.InvalidRelationTypeException;
import javax.management.relation.RelationService;
import javax.management.relation.RelationServiceMBean;
import javax.management.relation.RoleInfo;
import org.apache.commons.jxpath.JXPathIntrospector;
import org.jrobin.annotations.Arc;
import org.jrobin.annotations.Ds;
import org.jrobin.annotations.Rrd;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.RrdDef;

/**
 * RrdManager is responsible for managing the RrdPools and the associated files
 * associated with that pool.
 * 
 * @author Noah Campbell
 * @version 1.0
 */
public class RrdManager {

    /** The JROBIN_TYPE_RELATION_SERVICE. */
    private static final String JROBIN_TYPE_RELATION_SERVICE = "jrobin:type=RelationService";

    /** The server. */
    private static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    /**
     * Construct a new <code>RrdManager</code>.
     *
     * @param databaseDirectory
     * @throws IOException
     * @throws Exception 
     */
    private RrdManager(File databaseDirectory) throws IOException, Exception {
        initializeJDO(databaseDirectory);
        startRelationshipService();
        JXPathIntrospector.registerDynamicClass(ObjectName.class, ObjectNamePropertyHandler.class);
        JXPathIntrospector.registerDynamicClass(CompositeData.class, CompositeDataPropertyHandler.class);
        String systemOverride = System.getProperty("org.jivesoftware.messenger.plugin.monitor.overrides");
        InputStream is = null;
        if (systemOverride != null && systemOverride.length() > 0) {
            is = new FileInputStream(systemOverride);
        } else {
            is = this.getClass().getClassLoader().getResourceAsStream("typeoverrides.config");
        }
        try {
            Properties p = new Properties();
            p.load(is);
            Enumeration names = p.propertyNames();
            for (; names.hasMoreElements(); ) {
                String name = (String) names.nextElement();
                try {
                    AccumulatorManager.registerOverride(new ObjectName(name), Class.forName(p.getProperty(name)));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unable to register override: {0}", e.getLocalizedMessage());
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "rrdmanager.unabletoloadoverride");
        }
    }

    /**
     * Initialize JDO.
     * 
     * @param databaseDirectory
     */
    private void initializeJDO(File databaseDirectory) {
        Properties properties = new Properties();
        properties.setProperty("javax.jdo.PersistenceManagerFactoryClass", "org.jpox.PersistenceManagerFactoryImpl");
        properties.setProperty("javax.jdo.option.ConnectionDriverName", "org.hsqldb.jdbcDriver");
        properties.setProperty("javax.jdo.option.ConnectionURL", "jdbc:hsqldb:file:" + databaseDirectory.getAbsolutePath());
        properties.setProperty("javax.jdo.option.ConnectionUserName", "SA");
        properties.setProperty("javax.jdo.option.ConnectionPassword", "");
        properties.setProperty("javax.jdo.option.NontransactionalRead", "true");
        properties.setProperty("org.jpox.autoCreateSchema", "true");
        pmf = JDOHelper.getPersistenceManagerFactory(properties);
    }

    /**
     */
    private void teardown() {
        pmf.close();
    }

    /** The INSTANCE. */
    private static RrdManager INSTANCE = null;

    /**
     * @return rrdManager An instance of RrdManager.
     * @throws Exception
     */
    public static synchronized RrdManager instance() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new RrdManager(new File("./db"));
        }
        return INSTANCE;
    }

    /**
     * @return accumulators A list of accumulators.
     */
    public static List<Accumulator> getGraphableObjectNames() {
        return Collections.unmodifiableList(new ArrayList<Accumulator>(accumulators.values()));
    }

    /**
     * @throws MalformedObjectNameException
     * @throws InstanceAlreadyExistsException
     * @throws MBeanRegistrationException
     * @throws NotCompliantMBeanException
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws InvalidRelationTypeException
     */
    private synchronized void startRelationshipService() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, IllegalArgumentException, ClassNotFoundException, InvalidRelationTypeException {
        ObjectName relSvcName = new ObjectName(JROBIN_TYPE_RELATION_SERVICE);
        if (server.isRegistered(relSvcName)) {
            return;
        }
        RelationService relSvcObject = new RelationService(true);
        server.registerMBean(relSvcObject, relSvcName);
        RelationServiceMBean relSvc = (RelationServiceMBean) MBeanServerInvocationHandler.newProxyInstance(server, relSvcName, RelationServiceMBean.class, false);
        RoleInfo[] roles = new RoleInfo[] { new RoleInfo("override", Object.class.getName()), new RoleInfo("target", Object.class.getName()), new RoleInfo("archive", String.class.getName()) };
        relSvc.createRelationType("graph", roles);
    }

    /** The logger. */
    private static final Logger logger = Logger.getLogger("rrdmanager", "resources");

    /**
     * Returns a file name that is unique for the ObjectName.  
     * 
     * This method also creates a new file path if it doesn't exist.  If the
     * name exists, but the file is missing, then it is flagged as missing.
     * 
     * @param name
     * @return fileName
     * @throws FileNotFoundException 
     * @throws Exception 
     */
    public String getFileName(ObjectName name) throws FileNotFoundException, Exception {
        PersistenceManager pm = pmf.getPersistenceManager();
        Query q = pm.newQuery(RraReference.class);
        q.declareParameters("String objectName");
        q.setFilter("name == objectName");
        String canonicalName = name.getCanonicalName();
        List results = (List) q.execute(canonicalName);
        if (results != null && results.size() >= 1) {
            RraReference ref = (RraReference) results.get(0);
            String location = ref.getLocation();
            File f = new File(location);
            if (!f.exists()) {
                try {
                    pm.currentTransaction().begin();
                    ref.setMissing(true);
                    pm.currentTransaction().commit();
                } catch (Exception e) {
                    pm.currentTransaction().rollback();
                }
                throw new FileNotFoundException(location);
            }
            return location;
        } else {
            PersistenceManager pm2 = pmf.getPersistenceManager();
            pm2.currentTransaction().begin();
            try {
                RraReference ref = new RraReference();
                pm2.makePersistent(ref);
                ref.setName(canonicalName);
                String newFileName = digest(canonicalName) + ".rra";
                ref.setLocation(new File(newFileName).getAbsolutePath());
                ref.setMissing(false);
                pm2.currentTransaction().commit();
                return newFileName;
            } catch (Exception e) {
                pm2.currentTransaction().rollback();
                throw new Exception(e);
            }
        }
    }

    /** The key. */
    private static byte[] key = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x04, 0x03, 0x02, 0x01 };

    /**
     * @param buffer
     * @return digest A Base64 encoded MD5 digest.
     */
    private static String digest(String buffer) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(buffer.getBytes());
            return new String(encodeHex(md5.digest(key)));
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }

    /** 
     * Used to build output as Hex 
     */
    private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Encode a byte[] as a char[]
     * 
     * @param data
     * @return hexValue A char[].
     */
    public static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }
        return out;
    }

    /** The accumulators. */
    public static Map<ObjectName, Accumulator> accumulators = new HashMap<ObjectName, Accumulator>();

    /** The pmf. */
    private PersistenceManagerFactory pmf;

    /**
     * Create an accumulator for the Rrd annotation and the ObjectName.
     * 
     * @param objectName
     * @return accumulator
     * @throws Exception
     */
    public Accumulator create(ObjectName objectName) throws Exception {
        if (accumulators.containsKey(objectName)) {
            return accumulators.get(objectName);
        } else {
            String fileName = getFileName(objectName);
            if (exists(objectName)) {
                Accumulator acc = new Accumulator(objectName, fileName);
                accumulators.put(objectName, acc);
                return acc;
            } else {
                Accumulator acc = internalCreate(objectName, fileName);
                accumulators.put(objectName, acc);
                return acc;
            }
        }
    }

    /**
     * @param name
     * @param template
     */
    public void storeTemplate(String name, String template) {
        PersistenceManager pm = pmf.getPersistenceManager();
        pm.currentTransaction().begin();
        try {
            RraConfiguredGraph rcg = new RraConfiguredGraph();
            rcg.setName(name);
            rcg.setTemplate(template);
            pm.makePersistent(rcg);
            pm.currentTransaction().commit();
        } catch (Exception e) {
            pm.currentTransaction().rollback();
        } finally {
            if (!pm.isClosed()) {
                pm.close();
            }
        }
    }

    /**
     * @param objectName
     * @param fileName 
     * @return accumulator
     * @throws Exception 
     * @throws ClassNotFoundException 
     */
    private Accumulator internalCreate(ObjectName objectName, String fileName) throws ClassNotFoundException, Exception {
        RrdDbPool pool = RrdDbPool.getInstance();
        Class<?> forName = findClass(objectName);
        Rrd rrd = forName.getAnnotation(Rrd.class);
        RrdDef def = new RrdDef(fileName);
        def.setStep(rrd.step());
        for (Arc arc : rrd.archives()) {
            def.addArchive(arc.consolidationFunction().toString(), arc.xff(), arc.steps(), arc.rows());
            logger.log(Level.FINE, "rrdmanager.archiveadded", arc.consolidationFunction());
        }
        List<Ds> anonDs = findDs(forName);
        for (Ds ds : anonDs) {
            def.addDatasource(ds.name(), ds.type().toString(), ds.heartbeat(), ds.minValue(), ds.maxValue());
            logger.log(Level.FINE, "rrdmanager.datasourceadded", new Object[] { ds.expr(), ds.name() });
        }
        RrdDb db = pool.requestRrdDb(def);
        pool.release(db);
        return new Accumulator(objectName, db.getPath());
    }

    /**
     * This method will locate the interface for a particular ObjectName.  It'll
     * search the registered overrides if no annontation is present on the 
     * interface provided from MBeanInfo.
     * 
     * @param objectName
     * @return cls A Class for the objectName that contains a the graphing information.
     * @throws ClassNotFoundException
     * @throws Exception
     */
    private Class<?> findClass(ObjectName objectName) throws ClassNotFoundException, Exception {
        Class<?> forName = Class.forName(server.getMBeanInfo(objectName).getClassName());
        if (!forName.isAnnotationPresent(Rrd.class)) {
            forName = AccumulatorManager.getOverride(objectName);
            logger.log(Level.INFO, "rrdmanager.createoverride", forName.getName());
        }
        return forName;
    }

    /**
     * @param forName
     * @return dss A list of Ds attributes.
     */
    private List<Ds> findDs(Class<?> forName) {
        List<Ds> anonDs = new ArrayList<Ds>();
        Method[] methods = forName.getMethods();
        for (Method m : methods) {
            Ds ds = m.getAnnotation(Ds.class);
            if (ds != null) {
                anonDs.add(ds);
            }
        }
        return anonDs;
    }

    /**
     * @param objectName
     * @return boolean Does the rrd exist or not.
     */
    public boolean exists(ObjectName objectName) {
        try {
            return new File(getFileName(objectName)).exists();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "rrdmanager.existsfailed");
            return false;
        }
    }

    /**
     * @return list All the paths for all the accumulators.
     */
    public static List<String> getRraPaths() {
        ArrayList<String> results = new ArrayList<String>();
        for (Accumulator a : accumulators.values()) {
            results.add(a.getPath());
        }
        return results;
    }

    /**
     * @return tempaltes All the templates;
     */
    @SuppressWarnings("unchecked")
    public List<RraConfiguredGraph> listTemplates() {
        PersistenceManager pm = pmf.getPersistenceManager();
        return (List<RraConfiguredGraph>) pm.newQuery(RraConfiguredGraph.class).execute();
    }

    /**
     * @param templateName
     * @return graphTemplate
     * @throws UnableToFindTemplateException 
     */
    public RraConfiguredGraph getTemplate(String templateName) throws UnableToFindTemplateException {
        PersistenceManager pm = pmf.getPersistenceManager();
        Query q = pm.newQuery(RraConfiguredGraph.class);
        q.declareParameters("String templateName");
        q.setFilter("name == templateName");
        List results = (List) q.execute(templateName);
        if (results != null && results.size() > 0) {
            return (RraConfiguredGraph) results.iterator().next();
        } else {
            throw new UnableToFindTemplateException();
        }
    }

    /**
     * Release the current instance of the RrdManager.  This is useful for 
     * servlets that get redeployed during development and need to release
     * the singleton.
     */
    public static void release() {
        INSTANCE.teardown();
        INSTANCE = null;
    }
}
