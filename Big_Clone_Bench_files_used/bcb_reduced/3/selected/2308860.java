package com.beendoin.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import com.beendoin.db.DBClient;
import com.beendoin.domain.Application;
import com.beendoin.domain.Authentication;
import com.beendoin.domain.BeenDoin;
import com.beendoin.domain.Category;
import com.beendoin.domain.Computer;
import com.beendoin.domain.Idle;
import com.beendoin.domain.SystemId;
import com.beendoin.domain.Timezone;
import com.beendoin.domain.Version;
import com.beendoin.util.Base64;

public class BeenDoinLogProcessor {

    private static Logger LOG = Logger.getLogger(BeenDoinLogProcessor.class.getName());

    private static final ObjectPool pool = new GenericObjectPool(new BeenDoinLogRunnerFactory(), 200, GenericObjectPool.WHEN_EXHAUSTED_BLOCK, -1, 10, 5, false, false, 3600000l, 200, 3600000l, false, 5);

    private static final ExecutorService logProcessors = new BeenDoinLogThreadPoolExecutor(pool, new ThreadFactory() {

        public Thread newThread(Runnable runner) {
            return new Thread(runner);
        }
    });

    public static void processLog(File file, Authentication authn) {
        try {
            BeenDoinLogRunner runner = (BeenDoinLogRunner) pool.borrowObject();
            LOG.finest("Beginning to process file: " + file);
            runner.setFilePath(file);
            runner.setAuthentication(authn);
            logProcessors.execute(runner);
        } catch (Exception e) {
            throw new IllegalStateException("Attempting to acquire and run a BeenDoinLogRunner failed!!", e);
        }
    }
}

class BeenDoinLogThreadPoolExecutor extends ThreadPoolExecutor {

    private static Logger LOG = Logger.getLogger(BeenDoinLogThreadPoolExecutor.class.getName());

    private ObjectPool pool;

    public BeenDoinLogThreadPoolExecutor(ObjectPool pool, ThreadFactory threadFactory) {
        super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), threadFactory);
        this.pool = pool;
    }

    protected void beforeExecute(Thread t, Runnable r) {
        LOG.finest("Attempting to process file: " + ((BeenDoinLogRunner) r).getFilePath());
        super.beforeExecute(t, r);
    }

    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        try {
            pool.returnObject(r);
        } catch (Exception e) {
            throw new IllegalStateException("Attempting to return the BeenDoinLogRunner to the pool failed!!", e);
        }
    }
}

class BeenDoinLogRunnerFactory extends BasePoolableObjectFactory {

    /**
         * Creates an instance that can be returned by the pool.
         * @return an instance that can be returned by the pool.
         */
    public Object makeObject() throws Exception {
        return new BeenDoinLogRunner();
    }

    /**
         * Uninitialize an instance to be returned to the pool.
         * @param obj the instance to be passivated
         */
    public void passivateObject(Object obj) throws Exception {
        BeenDoinLogRunner runner = (BeenDoinLogRunner) obj;
        runner.clear();
    }
}

class BeenDoinLogRunner implements Runnable {

    private static Logger LOG = Logger.getLogger(BeenDoinLogRunner.class.getName());

    public static final String FIELD_SEP = "\t";

    private File file;

    private Authentication authn;

    private long currentSystemId = -1;

    public BeenDoinLogRunner() {
        this.clear();
    }

    public void run() {
        if (file == null) {
            throw new IllegalStateException("The BeenDoinLogRunner.file data member is null!!");
        }
        LOG.finest("Processing the BeenDoin log file at: " + file.getAbsolutePath());
        Object txOwner = new Object();
        try {
            DBClient.startTransaction(txOwner);
            BufferedReader log = new BufferedReader(new FileReader(file));
            String logLine = null;
            while ((logLine = log.readLine()) != null) {
                insertLogLine(logLine);
            }
            DBClient.commitTransaction(txOwner);
            log.close();
            this.file.delete();
        } catch (Exception ex) {
            if (LOG.isLoggable(Level.FINEST)) {
                StringWriter writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));
                LOG.finest(writer.toString());
            }
        } finally {
            try {
                DBClient.endTransaction(txOwner);
            } catch (SQLException ex) {
                if (LOG.isLoggable(Level.FINEST)) {
                    StringWriter writer = new StringWriter();
                    ex.printStackTrace(new PrintWriter(writer));
                    LOG.finest(writer.toString());
                }
            }
        }
    }

    private void insertLogLine(String logLine) throws NoSuchAlgorithmException, SQLException {
        LOG.finest("The passwordDigest is: '" + authn.getPassword() + "'");
        int digestIndex = logLine.lastIndexOf(FIELD_SEP) + FIELD_SEP.length();
        String logLineDigest = logLine.substring(digestIndex);
        LOG.finest("The logLineDigest is: '" + logLineDigest + "'");
        String restOfLogLine = logLine.substring(0, digestIndex);
        restOfLogLine += authn.getPassword();
        LOG.finest("The string to digest is: '" + restOfLogLine + "'");
        LOG.finest("The length of the string to digest is: '" + restOfLogLine.length() + "'");
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        byte[] digestBytes = md5Digest.digest(restOfLogLine.getBytes());
        String localLogLineDigest = Base64.encodeBytes(digestBytes);
        LOG.finest("The digest of the previous string is: '" + localLogLineDigest + "'");
        LOG.finest("The resultant string is: '" + logLine.substring(0, digestIndex) + localLogLineDigest + "'");
        LOG.finest("The length of the resultant string is: '" + (logLine.substring(0, digestIndex) + localLogLineDigest).length() + "'");
        boolean validDigest = true;
        if (!logLineDigest.equals(localLogLineDigest)) {
            IllegalArgumentException ex = new IllegalArgumentException(file.getAbsolutePath() + ": the two MD5 digests do not match-> file:'" + logLineDigest + "' and calculated:'" + localLogLineDigest + "'");
            LOG.severe(ex.getMessage());
            ex.printStackTrace();
            validDigest = false;
        }
        String[] logLineParts = logLine.split(FIELD_SEP);
        if ("SYSTEM_ID".equals(logLineParts[0])) {
            LOG.finest("Need to insert a SYSTEM_ID record: " + logLine + "'");
            Version version = new Version();
            version.setName(logLineParts[2]);
            Object rowExists = DBClient.orm.queryForObject("findVersionByName", version);
            if (rowExists == null) {
                long versionRowKey = DBClient.nextRowKey("Version");
                version.setId(versionRowKey);
                DBClient.orm.insert("insertVersion", version);
            } else {
                version = (Version) rowExists;
            }
            Computer computer = new Computer();
            computer.setComputerName(logLineParts[3]);
            computer.setComputerUsername(logLineParts[4]);
            computer.setMacAddress(logLineParts[5]);
            computer.setIpAddress(logLineParts[6]);
            computer.setUserId(authn.getUserId());
            rowExists = DBClient.orm.queryForObject("findUniqueComputer", computer);
            if (rowExists == null) {
                long computerRowKey = DBClient.nextRowKey("Computer");
                computer.setId(computerRowKey);
                DBClient.orm.insert("insertComputer", computer);
            } else {
                computer = (Computer) rowExists;
            }
            Timezone timezone = new Timezone();
            timezone.setName(logLineParts[8]);
            timezone.setDaylightName(logLineParts[9]);
            timezone.setGmtOffset(null);
            timezone.setDaylightGMTOffset(null);
            rowExists = DBClient.orm.queryForObject("findTimezoneByName", timezone);
            if (rowExists == null) {
                long timezoneRowKey = DBClient.nextRowKey("Timezone");
                timezone.setId(timezoneRowKey);
                DBClient.orm.insert("insertTimezone", timezone);
            } else {
                timezone = (Timezone) rowExists;
            }
            SystemId systemId = (SystemId) DBClient.orm.queryForObject("findSystemIdByMD5Digest", logLineParts[11]);
            if (systemId == null) {
                systemId = new SystemId();
                long systemIdRowKey = DBClient.nextRowKey("SystemId");
                LOG.finest("The systemIdRowKey is: " + systemIdRowKey);
                systemId.setId(systemIdRowKey);
                systemId.setVersionId(version.getId());
                systemId.setComputerId(computer.getId());
                systemId.setTimezoneId(timezone.getId());
                systemId.setMd5Digest(logLineParts[11]);
                systemId.setValidDigest(validDigest);
                systemId.setProcessId(Long.parseLong(logLineParts[10]));
                systemId.setStartTime(new Date(Long.parseLong(logLineParts[1])));
                DBClient.orm.insert("insertSystemId", systemId);
                currentSystemId = systemIdRowKey;
            } else {
                currentSystemId = systemId.getId();
            }
        } else if ("IDLE".equals(logLineParts[0])) {
            LOG.finest("Need to insert an IDLE record: " + logLine + "'");
            if (currentSystemId < 0) {
                currentSystemId = getCurrentSystemId();
            }
            Idle idle = (Idle) DBClient.orm.queryForObject("findIdleByMd5Digest", logLineParts[4]);
            if (idle == null) {
                Category category = (Category) DBClient.orm.queryForObject("findUniqueCategory", logLineParts[3]);
                if (category == null) {
                    category = new Category();
                    long categoryRowKey = DBClient.nextRowKey("Category");
                    category.setId(categoryRowKey);
                    category.setName(logLineParts[3]);
                    DBClient.orm.insert("insertCategory", category);
                }
                idle = new Idle();
                long idleRowKey = DBClient.nextRowKey("Idle");
                idle.setId(idleRowKey);
                idle.setStartTime(new Date(Long.parseLong(logLineParts[1])));
                idle.setEndTime(new Date(Long.parseLong(logLineParts[2])));
                idle.setCategoryId(category.getId());
                idle.setMd5Digest(logLineParts[4]);
                idle.setValidDigest(validDigest);
                idle.setSystemId(currentSystemId);
                DBClient.orm.insert("insertIdle", idle);
            }
        } else {
            LOG.finest("Need to insert an application record: '" + logLine + "'");
            if (currentSystemId < 0) {
                currentSystemId = getCurrentSystemId();
            }
            BeenDoin beendoin = (BeenDoin) DBClient.orm.queryForObject("findBeenDoinByMd5Digest", logLineParts[7]);
            if (beendoin == null) {
                Category category = (Category) DBClient.orm.queryForObject("findUniqueCategory", logLineParts[6]);
                if (category == null) {
                    category = new Category();
                    long categoryRowKey = DBClient.nextRowKey("Category");
                    category.setId(categoryRowKey);
                    category.setName(logLineParts[6]);
                    DBClient.orm.insert("insertCategory", category);
                }
                Application app = new Application();
                app.setName(logLineParts[0]);
                app.setWindowType(logLineParts[1]);
                Object rowExists = DBClient.orm.queryForObject("findAppByNameAndType", app);
                if (rowExists == null) {
                    long appRowKey = DBClient.nextRowKey("Application");
                    app.setId(appRowKey);
                    DBClient.orm.insert("insertApplication", app);
                } else {
                    app = (Application) rowExists;
                }
                beendoin = new BeenDoin();
                long beenDoinRowKey = DBClient.nextRowKey("BeenDoin");
                beendoin.setId(beenDoinRowKey);
                beendoin.setStartTime(new Date(Long.parseLong(logLineParts[4])));
                beendoin.setEndTime(new Date(Long.parseLong(logLineParts[5])));
                beendoin.setCategoryId(category.getId());
                beendoin.setAppId(app.getId());
                beendoin.setMd5Digest(logLineParts[7]);
                beendoin.setValidDigest(validDigest);
                beendoin.setSystemId(currentSystemId);
                beendoin.setWindowTitle(logLineParts[2]);
                beendoin.setProcessId(Long.parseLong(logLineParts[3]));
                DBClient.orm.insert("insertBeenDoin", beendoin);
            }
        }
    }

    private long getCurrentSystemId() throws SQLException {
        LOG.warning("The currentSystemId was not initialized; initializaing from database!!!");
        SystemId sysId = (SystemId) DBClient.orm.queryForObject("findMostRecentSystemId", authn.getUsername());
        return sysId.getId();
    }

    public void clear() {
        this.file = null;
        this.authn = null;
        this.currentSystemId = -1;
    }

    public void setFilePath(File file) {
        this.file = file;
    }

    public File getFilePath() {
        return file;
    }

    public void setAuthentication(Authentication authn) {
        this.authn = authn;
    }
}
