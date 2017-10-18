package com.google.code.sagetvaddons.sjq.server;

import gkusnick.sagetv.api.AiringAPI;
import gkusnick.sagetv.api.MediaFileAPI;
import gkusnick.sagetv.api.ShowAPI;
import gkusnick.sagetv.api.SystemMessageAPI;
import java.io.File;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.code.sagetvaddons.sjq.server.utils.SystemMessageUtils;

/**
 * The SJQ data store; stores details about what jobs have been run for which
 * media files and when
 * 
 * @version $Id: DataStore.java 1586 2011-08-29 02:32:22Z derek@battams.ca $
 */
final class DataStore {

    private static final Logger LOG = Logger.getLogger(DataStore.class);

    private static final int DB_POOL_SIZE = 5;

    private static final Queue<DataStore> DB_CONNS = new LinkedList<DataStore>();

    private static int actualPoolSize = DB_POOL_SIZE;

    private static boolean exclusiveRequestActive = false;

    private static Thread exclusiveHolder = null;

    static {
        synchronized (DataStore.class) {
            for (int i = 0; i < DB_POOL_SIZE; ++i) DB_CONNS.add(new DataStore());
        }
        LOG.info("DataStore connection pool initialized with " + DB_POOL_SIZE + " connections.");
    }

    static final synchronized DataStore getConnection(boolean exclusive) {
        String threadName = Thread.currentThread().getName();
        long start = System.currentTimeMillis();
        if (exclusive) {
            LOG.info("Thread '" + threadName + "' is seeking an exclusive DataStore connection!");
            while (DB_CONNS.size() != actualPoolSize) {
                try {
                    LOG.info("Thread '" + threadName + "' is waiting for an exclusive DataStore connection! [AVAIL=" + DB_CONNS.size() + "; MAX=" + actualPoolSize + "; WAIT=" + (System.currentTimeMillis() - start) + "ms]");
                    DataStore.class.wait();
                } catch (InterruptedException e) {
                }
            }
            exclusiveRequestActive = true;
            exclusiveHolder = Thread.currentThread();
            LOG.info("Thread '" + threadName + "' has received an exclusive DataStore connection! [WAIT=" + (System.currentTimeMillis() - start) + "ms]");
        } else {
            while (exclusiveRequestActive && !Thread.currentThread().equals(exclusiveHolder)) {
                try {
                    LOG.debug("Thread '" + threadName + "' is waiting for a non-exclusive DataStore connection! [WAIT=" + (System.currentTimeMillis() - start) + "ms]");
                    DataStore.class.wait();
                } catch (InterruptedException e) {
                }
            }
            LOG.debug("Thread '" + threadName + "' has received a non-exclusive DataStore connection! [WAIT=" + (System.currentTimeMillis() - start) + "ms]");
        }
        if (DB_CONNS.isEmpty()) {
            LOG.debug("DataStore connection pool is empty; added a new connection to the pool.");
            DB_CONNS.add(new DataStore());
            ++actualPoolSize;
        }
        return DB_CONNS.remove().startTimer();
    }

    static final DataStore getConnection() {
        return getConnection(false);
    }

    static final synchronized void returnConnection(DataStore d) {
        String threadName = Thread.currentThread().getName();
        boolean isOwnerNull;
        synchronized (d) {
            isOwnerNull = d.getOwner() == null;
        }
        if (!isOwnerNull) DB_CONNS.add(d); else LOG.warn("Thread '" + Thread.currentThread().getName() + "' attempted to return a timed out connection!  Dropping connection.");
        d.cancelTimer();
        if (DB_CONNS.size() == actualPoolSize) {
            if (exclusiveRequestActive) {
                exclusiveRequestActive = false;
                exclusiveHolder = null;
                LOG.info("'" + threadName + "' has returned its exclusive DataStore connection!");
            }
        }
        LOG.debug("Thread '" + threadName + "' returned a connection; now " + DB_CONNS.size() + " in the pool.");
        DataStore.class.notifyAll();
    }

    private static final int SCHEMA_VERSION = 18;

    private static boolean dbInitialized = false;

    private static final int MAX_BATCH_SIZE = 10240;

    private File dataStore;

    private Connection conn;

    private PreparedStatement delTask, saveClientConf, chkProcessed, markAsProcessed, clearLog, readLog, setSetting, getSetting, getClientConf, logStmt;

    private int batchSize;

    private Timer timer;

    private Thread owner;

    private DataStore() {
        batchSize = 0;
        try {
            Class.forName("org.sqlite.JDBC");
            dataStore = new File("sjq.sqlite");
            openConnection();
            synchronized (DataStore.class) {
                if (!dbInitialized) {
                    loadDDL();
                    upgradeSchema();
                    dbInitialized = true;
                    prepStatements();
                    checkDefaultSettings();
                    LOG.info("SQLite driver implementation: '" + getImplDetails() + "'");
                } else prepStatements();
            }
        } catch (ClassNotFoundException e) {
            LOG.fatal("Class not found", e);
            throw new RuntimeException("JDBC driver not found!", e);
        } catch (SQLException e) {
            LOG.fatal("Fatal error", e);
            throw new RuntimeException("Unable to prepare log statement", e);
        }
        timer = null;
        owner = Thread.currentThread();
    }

    private DataStore startTimer() {
        synchronized (this) {
            owner = Thread.currentThread();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                synchronized (DataStore.class) {
                    --actualPoolSize;
                    synchronized (DataStore.this) {
                        if (exclusiveHolder != null && exclusiveHolder.getName().equals(owner.getName())) {
                            exclusiveRequestActive = false;
                            exclusiveHolder = null;
                        }
                        owner = null;
                    }
                }
                LOG.warn("DataStore pool timeout expired! '" + owner.getName() + "' did not return connection within 30 seconds!  Adding a replacement connection to the pool.");
                DataStore.class.notifyAll();
            }
        }, 120000);
        return this;
    }

    private DataStore cancelTimer() {
        if (timer != null) timer.cancel();
        synchronized (this) {
            owner = null;
        }
        return this;
    }

    private Thread getOwner() {
        return owner;
    }

    private final void prepStatements() throws SQLException {
        logStmt = conn.prepareStatement("INSERT INTO log (mediaid, taskid, msg, ts, objtype) VALUES (?, ?, ?, ?, ?)");
        chkProcessed = conn.prepareStatement("SELECT COUNT(*) FROM recordings WHERE id = ? AND type = ? AND objtype = ?");
        markAsProcessed = conn.prepareStatement("INSERT INTO recordings (id, type, start, finish, host, state, airing, objtype) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        clearLog = conn.prepareStatement("DELETE FROM log WHERE mediaid = ? AND taskid = ? AND objtype = ?");
        readLog = conn.prepareStatement("SELECT msg FROM log WHERE mediaid = ? AND taskid = ? AND objtype = ? ORDER BY ts ASC");
        setSetting = conn.prepareStatement("REPLACE INTO settings (var,val) VALUES (?,?)");
        getSetting = conn.prepareStatement("SELECT val FROM settings WHERE var = ?");
        getClientConf = conn.prepareStatement("SELECT conf FROM client WHERE host = ?");
        saveClientConf = conn.prepareStatement("REPLACE INTO client (host, conf) VALUES (?, ?)");
        delTask = conn.prepareStatement("DELETE FROM recordings WHERE id = ? AND type = ? AND state = ? AND objtype = ?");
    }

    /**
	 * 
	 */
    @Override
    protected void finalize() {
        try {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace(System.out);
                }
            }
        } finally {
            try {
                super.finalize();
            } catch (Throwable t) {
            }
        }
        return;
    }

    private void checkDefaultSettings() {
        if (getSetting("MaxSleep") == null) setSetting("MaxSleep", "360");
        String runType = getSetting("RunType");
        if (runType == null || runType.equals("AfterRec")) setSetting("RunType", "2"); else if (runType.equals("DuringRec")) setSetting("RunType", "1"); else if (!runType.matches("\\d+")) setSetting("RunType", "0");
        if (getSetting("RunDelay") == null) setSetting("RunDelay", "10");
        if (getSetting("IgnoreFailedTasks") == null) setSetting("IgnoreFailedTasks", "false");
        if (getSetting("ScanTv") == null) setSetting("ScanTv", "true");
        if (getSetting("ScanMusic") == null) setSetting("ScanMusic", "false");
        if (getSetting("ScanVideos") == null) setSetting("ScanVideos", "false");
        if (getSetting("ScanDVDs") == null) setSetting("ScanDVDs", "false");
        if (getSetting("ScanPictures") == null) setSetting("ScanPictures", "false");
        if (getSetting("Debug") == null) setSetting("Debug", "false");
        if (getSetting("ClearServerLogs") == null) setSetting("ClearServerLogs", "Weekly");
        if (getSetting("ClearClientLogs") == null) setSetting("ClearClientLogs", "Weekly");
        if (getSetting("ClearCompletedTaskLogs") == null) setSetting("ClearCompletedTaskLogs", "Weekly");
        if (getSetting("IgnoreTaskOutput") == null) setSetting("IgnoreTaskOutput", "false");
        if (getSetting("ValidClients") == null) setSetting("ValidClients", "");
        if (getSetting("SageAlertUrl") == null) setSetting("SageAlertUrl", "");
        if (getSetting("SageAlertUser") == null) setSetting("SageAlertUser", "");
        if (getSetting("SageAlertPwd") == null) setSetting("SageAlertPwd", "");
    }

    private void openConnection() {
        try {
            if (conn != null && !conn.isClosed()) return;
            String connUrl = "jdbc:sqlite:" + dataStore;
            LOG.debug("Using JDBC URL: '" + connUrl + "'");
            conn = DriverManager.getConnection(connUrl);
        } catch (SQLException e) {
            LOG.fatal("SQL error", e);
            throw new RuntimeException(e);
        }
        return;
    }

    private void loadDDL() {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            stmt.executeQuery("SELECT * FROM recordings").close();
        } catch (SQLException e) {
            if (!e.getMessage().startsWith("no such table")) {
                LOG.fatal("SQL error", e);
                throw new RuntimeException(e);
            }
            String qry = "CREATE TABLE recordings (id INT NOT NULL, type VARCHAR(32) NOT NULL, PRIMARY KEY(id, type))";
            Statement tblStmt = null;
            try {
                if (stmt != null) {
                    stmt.close();
                    stmt = null;
                }
                if (rs != null) {
                    rs.close();
                    rs = null;
                }
                tblStmt = conn.createStatement();
                tblStmt.executeUpdate(qry);
            } catch (SQLException e2) {
                LOG.fatal("SQL error", e2);
                throw new RuntimeException(e2);
            } finally {
                if (tblStmt != null) {
                    try {
                        tblStmt.close();
                    } catch (SQLException e3) {
                        LOG.error("SQL error", e3);
                        throw new RuntimeException(e3);
                    }
                }
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOG.error("SQL error", e);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error("SQL error", e);
                }
            }
        }
        return;
    }

    private void upgradeSchema() {
        Statement stmt = null;
        boolean updatedSchema = false;
        try {
            int i = getSchema();
            if (i < SCHEMA_VERSION) {
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                updatedSchema = true;
            }
            while (i < SCHEMA_VERSION) {
                String qry;
                switch(i) {
                    case 0:
                        qry = "CREATE TABLE settings (var VARCHAR(32) NOT NULL, val LONG VARCHAR)";
                        stmt.executeUpdate(qry);
                        qry = "INSERT INTO settings (var, val) VALUES ('schema', '1')";
                        stmt.executeUpdate(qry);
                        qry = "ALTER TABLE recordings ADD COLUMN exe LONG VARCHAR NOT NULL DEFAULT '%UNKNOWN%'";
                        stmt.executeUpdate(qry);
                        qry = "CREATE TABLE files (id INT NOT NULL, file LONG VARCHAR NOT NULL, finished INT NOT NULL)";
                        stmt.executeUpdate(qry);
                        updateFilesTable();
                        break;
                    case 1:
                        qry = "ALTER TABLE files ADD COLUMN type VARCHAR(32) NOT NULL DEFAULT '%UNKNOWN%'";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '2' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 2:
                        qry = "CREATE UNIQUE INDEX IF NOT EXISTS recordings_history ON recordings(id,type)";
                        stmt.executeUpdate(qry);
                        qry = "CREATE INDEX IF NOT EXISTS files_history ON files(id,type)";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '3' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 3:
                        qry = "CREATE TABLE log (id INTEGER PRIMARY KEY, context VARCHAR(16) NOT NULL, level VARCHAR(16) NOT NULL, time LONG INT NOT NULL, msg LONG VARCHAR NOT NULL, parent INT)";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '4' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 4:
                        qry = "CREATE UNIQUE INDEX IF NOT EXISTS log_id ON log(id)";
                        stmt.executeUpdate(qry);
                        qry = "CREATE INDEX IF NOT EXISTS log_parent ON log(parent)";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '5' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 5:
                        qry = "CREATE TABLE tmp_settings (var varchar(32) NOT NULL PRIMARY KEY, val varchar(128) NOT NULL)";
                        stmt.executeUpdate(qry);
                        qry = "INSERT INTO tmp_settings SELECT var,val FROM settings";
                        stmt.executeUpdate(qry);
                        qry = "DROP TABLE settings";
                        stmt.executeUpdate(qry);
                        qry = "ALTER TABLE tmp_settings RENAME TO settings";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '6' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 6:
                        qry = "DROP TABLE log";
                        stmt.executeUpdate(qry);
                        qry = "CREATE TABLE log (id INTEGER PRIMARY KEY, mediaid varchar(32) NOT NULL, taskid varchar(32) NOT NULL, msg LONG VARCHAR NOT NULL)";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '7' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 7:
                        qry = "CREATE TABLE client (host varchar(255) PRIMARY KEY, conf LONG VARCHAR)";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '8' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 8:
                        qry = "DROP INDEX files_history";
                        stmt.executeUpdate(qry);
                        qry = "DROP INDEX recordings_history";
                        stmt.executeUpdate(qry);
                        qry = "DROP TABLE files";
                        stmt.executeUpdate(qry);
                        qry = "CREATE TABLE new_rec (id INT NOT NULL, type VARCHAR(32) NOT NULL, start INT NOT NULL DEFAULT 0, finish INT NOT NULL DEFAULT 0, state INT NOT NULL DEFAULT 1, PRIMARY KEY(id, type))";
                        stmt.executeUpdate(qry);
                        qry = "INSERT INTO new_rec SELECT id, type, 0, 0, 1 FROM recordings";
                        stmt.executeUpdate(qry);
                        qry = "DROP TABLE recordings";
                        stmt.executeUpdate(qry);
                        qry = "CREATE TABLE recordings (id INT NOT NULL, type VARCHAR(32) NOT NULL, start INT NOT NULL DEFAULT 0, finish INT NOT NULL DEFAULT 0, state INT NOT NULL DEFAULT 1, PRIMARY KEY(id, type))";
                        stmt.executeUpdate(qry);
                        qry = "INSERT INTO recordings SELECT * FROM new_rec";
                        stmt.executeUpdate(qry);
                        qry = "DROP TABLE new_rec";
                        stmt.executeUpdate(qry);
                        qry = "CREATE UNIQUE INDEX recordings_history on recordings(id,type)";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '9' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 9:
                        qry = "ALTER TABLE recordings ADD COLUMN host VARCHAR(255) NOT NULL DEFAULT ''";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '10' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 10:
                        qry = "ALTER TABLE recordings ADD COLUMN airing VARCHAR(64) NOT NULL DEFAULT '0'";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '11' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 11:
                        PreparedStatement pstmt = null;
                        try {
                            MessageDigest msg = MessageDigest.getInstance("MD5");
                            msg.update("sjqadmin".getBytes());
                            String pwd = new String(msg.digest());
                            pstmt = conn.prepareStatement("REPLACE INTO settings (var, val) VALUES ('password', ?)");
                            pstmt.setString(1, pwd);
                            pstmt.executeUpdate();
                        } catch (NoSuchAlgorithmException e) {
                            throw new SQLException(e);
                        } finally {
                            if (pstmt != null) pstmt.close();
                        }
                        stmt.executeUpdate("UPDATE settings SET val = '12' WHERE var = 'schema'");
                        break;
                    case 12:
                        qry = "CREATE INDEX logs_for_tasks ON log(mediaid, taskid)";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '13' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 13:
                        qry = "DELETE FROM log";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '14' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 14:
                        qry = "DROP TABLE log";
                        stmt.executeUpdate(qry);
                        qry = "CREATE TABLE log (id INTEGER PRIMARY KEY, mediaid varchar(32) NOT NULL, taskid varchar(32) NOT NULL, msg LONG VARCHAR NOT NULL, ts INTEGER NOT NULL DEFAULT 0)";
                        stmt.executeUpdate(qry);
                        qry = "CREATE INDEX logs_by_date ON log(ts)";
                        stmt.executeUpdate(qry);
                        qry = "CREATE INDEX IF NOT EXISTS logs_for_tasks ON log(mediaid, taskid)";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '15' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 15:
                        qry = "DELETE FROM log WHERE mediaid = 0 AND taskid = '0'";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '16' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 16:
                        qry = "CREATE TEMPORARY TABLE rec_tmp (objtype varchar(64) NOT NULL DEFAULT 'media', id INT NOT NULL, type VARCHAR(32) NOT NULL, start INT NOT NULL DEFAULT 0, finish INT NOT NULL DEFAULT 0, state INT NOT NULL DEFAULT 1, host VARCHAR(255) NOT NULL DEFAULT '', airing VARCHAR(64) NOT NULL DEFAULT '0', PRIMARY KEY (objtype, id, type))";
                        stmt.executeUpdate(qry);
                        qry = "INSERT INTO rec_tmp SELECT 'media', id, type, start, finish, state, host, airing FROM recordings";
                        stmt.executeUpdate(qry);
                        qry = "DROP TABLE recordings";
                        stmt.executeUpdate(qry);
                        qry = "CREATE TABLE recordings (objtype varchar(64) NOT NULL DEFAULT 'media', id INT NOT NULL, type VARCHAR(32) NOT NULL, start INT NOT NULL DEFAULT 0, finish INT NOT NULL DEFAULT 0, state INT NOT NULL DEFAULT 1, host VARCHAR(255) NOT NULL DEFAULT '', airing VARCHAR(64) NOT NULL DEFAULT '0', PRIMARY KEY (objtype, id, type))";
                        stmt.executeUpdate(qry);
                        qry = "INSERT INTO recordings SELECT * FROM rec_tmp";
                        stmt.executeUpdate(qry);
                        qry = "DROP TABLE rec_tmp";
                        stmt.executeUpdate(qry);
                        qry = "ALTER TABLE log ADD COLUMN objtype varchar(64) NOT NULL DEFAULT 'media'";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '17' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                    case 17:
                        qry = "DROP INDEX logs_for_tasks";
                        stmt.executeUpdate(qry);
                        qry = "CREATE INDEX logs_for_tasks ON log(mediaid, taskid, objtype)";
                        stmt.executeUpdate(qry);
                        qry = "UPDATE settings SET val = '18' WHERE var = 'schema'";
                        stmt.executeUpdate(qry);
                        break;
                }
                i++;
            }
            if (updatedSchema) conn.commit();
        } catch (SQLException e) {
            try {
                if (updatedSchema) conn.rollback();
            } catch (SQLException x) {
                LOG.fatal("SQL error", x);
            }
            LOG.fatal("SQL error", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (updatedSchema) conn.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.fatal("SQL error", e);
                throw new RuntimeException(e);
            }
        }
    }

    private int getSchema() {
        String qry = "SELECT val FROM settings WHERE var = 'schema'";
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(qry);
            return rs.getInt(1);
        } catch (SQLException e) {
            if (!e.getMessage().startsWith("no such table")) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
            return 0;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void updateFilesTable() {
        String qry = "INSERT INTO files (id, file, finished) VALUES (?, ?, 0)";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(qry);
            for (MediaFileAPI.MediaFile mf : Butler.SageApi.mediaFileAPI.GetMediaFiles()) {
                for (File f : mf.GetSegmentFiles()) {
                    pstmt.setString(2, f.getAbsolutePath());
                    pstmt.setString(1, Integer.toString(mf.GetMediaFileID()));
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
        }
        return;
    }

    boolean beenProcessed(final String id, final String type, final String objType) {
        ResultSet rs = null;
        try {
            chkProcessed.setString(1, id);
            chkProcessed.setString(2, type);
            chkProcessed.setString(3, objType);
            rs = chkProcessed.executeQuery();
            int val = rs.getInt(1);
            return val == 1;
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
        }
    }

    void setSettings(JSONObject o) {
        Iterator<?> keys = o.keys();
        while (keys.hasNext()) {
            String k = (String) keys.next();
            try {
                setSetting(k, o.getString(k));
            } catch (JSONException e) {
                LOG.error("JSON error", e);
                throw new RuntimeException(e);
            }
        }
    }

    JSONObject readSettings() {
        JSONObject o = new JSONObject();
        String qry = "SELECT var, val FROM settings";
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(qry);
            while (rs.next()) {
                o.put(rs.getString(1), rs.getString(2));
            }
            return o;
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        } catch (JSONException e) {
            LOG.error("JSON error", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
        }
    }

    private static final String getHistorySortInfo(String rawInfo) {
        String defaultSortOrder = "finish DESC";
        String sortOrder = null;
        if (rawInfo != null && rawInfo.length() > 0) {
            sortOrder = "";
            String[] cols = rawInfo.split(";");
            for (String c : cols) {
                boolean validCol = true;
                String[] colInfo = c.split(":");
                if (colInfo[1].equals("2")) sortOrder = sortOrder.concat("type"); else if (colInfo[1].equals("3")) sortOrder = sortOrder.concat("start"); else if (colInfo[1].equals("4")) sortOrder = sortOrder.concat("finish"); else if (colInfo[1].equals("5")) sortOrder = sortOrder.concat("host"); else validCol = false;
                if (validCol) {
                    if (colInfo[0].equals("a")) sortOrder = sortOrder.concat(" ASC,"); else sortOrder = sortOrder.concat(" DESC,");
                }
            }
        }
        if (sortOrder != null && sortOrder.length() > 0) return sortOrder.substring(0, sortOrder.length() - 1);
        return defaultSortOrder;
    }

    JSONObject getJobHistory(int type, int offset, int limit, String sortInfo) {
        sortInfo = getHistorySortInfo(sortInfo);
        String qry = "SELECT id, type, start, finish, host, airing, objType FROM recordings WHERE state = " + type + " ORDER BY " + sortInfo + " LIMIT " + limit + " OFFSET " + offset;
        JSONObject result = new JSONObject();
        JSONArray jarr = new JSONArray();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) FROM recordings WHERE state = " + type);
            if (!rs.next()) throw new RuntimeException("Did not receive record count from data store!");
            result.put("count", rs.getInt(1));
            rs.close();
            rs = stmt.executeQuery(qry);
            while (rs.next()) {
                JSONObject o = new JSONObject();
                String title = null;
                String mediaId = rs.getString(1);
                String objType = rs.getString(7);
                String subtitle = null;
                if (objType.equals("media")) {
                    MediaFileAPI.MediaFile mf = Butler.SageApi.mediaFileAPI.GetMediaFileForID(Integer.parseInt(mediaId));
                    String airingId = rs.getString(6);
                    AiringAPI.Airing airing = Butler.SageApi.airingAPI.GetAiringForID(Integer.parseInt(airingId));
                    if (mf != null) title = mf.GetMediaTitle(); else if (airing != null) title = airing.GetAiringTitle();
                    if (title == null || title.length() == 0) title = "<Unknown>";
                    if (airing != null) {
                        ShowAPI.Show s = airing.GetShow();
                        if (s != null) subtitle = s.GetShowEpisode();
                    }
                    if (subtitle == null) subtitle = "";
                } else {
                    SystemMessageAPI.SystemMessage msg = SystemMessageUtils.getSysMsg(mediaId);
                    if (msg != null) title = msg.GetSystemMessageTypeName(); else title = "[Deleted System Message]";
                    subtitle = "";
                }
                o.put("mediaId", mediaId);
                o.put("taskId", StringEscapeUtils.escapeHtml(rs.getString(2)));
                o.put("start", rs.getLong(3));
                o.put("finish", rs.getLong(4));
                o.put("title", StringEscapeUtils.escapeHtml(title));
                o.put("objType", objType);
                o.put("subtitle", StringEscapeUtils.escapeHtml(subtitle));
                o.put("host", StringEscapeUtils.escapeHtml(rs.getString(5)));
                jarr.put(o);
            }
            result.put("data", jarr);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        } catch (JSONException e) {
            LOG.error("JSON error", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    boolean markAsProcessed(final String id, final String type, final long started, final int state, final String host, final String airing, final String objType) {
        PreparedStatement recStmt = markAsProcessed;
        try {
            recStmt.setString(1, id);
            recStmt.setString(2, type);
            recStmt.setLong(3, started);
            recStmt.setLong(4, System.currentTimeMillis());
            recStmt.setString(5, host);
            recStmt.setInt(6, state);
            recStmt.setString(7, airing);
            recStmt.setString(8, objType);
            recStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        }
    }

    void clearClientLogs() {
        String qry = "DELETE FROM log WHERE mediaid = '-1'";
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(qry);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
        }
    }

    boolean clearLog(String mediaId, String taskId, String objType) {
        PreparedStatement pstmt = clearLog;
        try {
            pstmt.setString(1, mediaId);
            pstmt.setString(2, taskId);
            pstmt.setString(3, objType);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        }
    }

    void clearCompletedTaskLogs(long age, long lastRun) {
        String delQry = "DELETE FROM log WHERE mediaid <> '0' AND mediaid <> '-1' AND ts < " + (System.currentTimeMillis() - age);
        Statement delStmt = null;
        try {
            delStmt = conn.createStatement();
            delStmt.executeUpdate(delQry);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (delStmt != null) delStmt.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
        }
    }

    String readLog(final String mediaId, final String taskId, final String objType) {
        PreparedStatement stmt = readLog;
        StringWriter log = new StringWriter();
        try {
            stmt.setString(1, mediaId);
            stmt.setString(2, taskId);
            stmt.setString(3, objType);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) log.write(rs.getString(1) + "\n");
            rs.close();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        }
        if (!mediaId.equals("-1")) return StringEscapeUtils.escapeHtml(log.toString()); else return log.toString();
    }

    void setSetting(String name, String val) {
        PreparedStatement pstmt = setSetting;
        try {
            pstmt.setString(1, name);
            pstmt.setString(2, val);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        }
    }

    String getSetting(String name) {
        return getSetting(name, null);
    }

    String getSetting(String name, String defaultValue) {
        PreparedStatement pstmt = getSetting;
        ResultSet rs = null;
        try {
            pstmt.setString(1, name);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString(1);
            return defaultValue;
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
        }
    }

    String getImplDetails() {
        try {
            return conn.getMetaData().getDriverVersion();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return "unknown";
        }
    }

    String getClientConf(String id) {
        PreparedStatement pstmt = getClientConf;
        ResultSet rs = null;
        try {
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString(1);
            return "";
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new RuntimeException(e);
            }
        }
    }

    boolean saveClientConf(String id, String data) {
        PreparedStatement pstmt = saveClientConf;
        try {
            pstmt.setString(1, id);
            pstmt.setString(2, data);
            return pstmt.executeUpdate() == 1;
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new RuntimeException(e);
        }
    }

    String getMediaMask() {
        String mask = "";
        if (Boolean.parseBoolean(getSetting("ScanVideos"))) mask = mask.concat("V");
        if (Boolean.parseBoolean(getSetting("ScanTv"))) mask = mask.concat("T");
        if (Boolean.parseBoolean(getSetting("ScanMusic"))) mask = mask.concat("M");
        if (Boolean.parseBoolean(getSetting("ScanDVDs"))) mask = mask.concat("D");
        if (Boolean.parseBoolean(getSetting("ScanPictures"))) mask = mask.concat("P");
        return mask;
    }

    boolean clear(int type) {
        String qry = "DELETE FROM recordings WHERE state = " + type;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(qry);
            return true;
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return false;
        } finally {
            if (stmt != null) try {
                stmt.close();
            } catch (Exception e) {
                LOG.error("SQL error", e);
            }
        }
    }

    boolean delTask(String mediaId, String taskId, int type, String objType) {
        PreparedStatement pstmt = delTask;
        try {
            pstmt.setString(1, mediaId);
            pstmt.setString(2, taskId);
            pstmt.setInt(3, type);
            pstmt.setString(4, objType);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return false;
        }
    }

    boolean freeDiskSpace() {
        String qry = "VACUUM";
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(qry);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return false;
        } finally {
            if (stmt != null) try {
                stmt.close();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }
        setSetting("LastVacuum", Long.toString(System.currentTimeMillis()));
        return true;
    }

    boolean logForTaskClient(String mediaId, String taskId, String msg, long timeStamp, String objType) {
        try {
            logStmt.setString(1, mediaId);
            logStmt.setString(2, taskId);
            logStmt.setString(3, msg);
            if (timeStamp == 0) timeStamp = System.currentTimeMillis();
            logStmt.setLong(4, timeStamp);
            logStmt.setString(5, objType);
            logStmt.addBatch();
            if (++batchSize == MAX_BATCH_SIZE) flushLogs();
            return true;
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return false;
        }
    }

    void flushLogs() {
        try {
            if (batchSize > 0) {
                long start = System.currentTimeMillis();
                conn.setAutoCommit(false);
                logStmt.executeBatch();
                conn.commit();
                long end = System.currentTimeMillis();
                LOG.info("Flushed " + batchSize + " log message(s) in " + (end - start) + "ms");
            } else logStmt.clearBatch();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        } finally {
            batchSize = 0;
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }
    }
}
