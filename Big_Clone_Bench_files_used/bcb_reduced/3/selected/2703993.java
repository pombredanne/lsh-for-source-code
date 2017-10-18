package gov.lanl.locator;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp.AbandonedConfig;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DataSourceConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

public class BrokerUtils {

    static Logger log = Logger.getLogger(BrokerUtils.class.getName());

    public synchronized byte[] getDigest(String aKey, MessageDigest algorithm) throws java.io.UnsupportedEncodingException {
        byte[] key = aKey.getBytes("UTF-8");
        algorithm.reset();
        algorithm.update(key);
        byte[] digest = algorithm.digest();
        return digest;
    }

    public int getNumOfTable(int tablenum, byte[] digest) {
        return Math.abs(readInt(digest, digest.length - 4)) % tablenum;
    }

    public static final int readInt(byte buf[], int offset) {
        return buf[offset] << 24 | (0x00ff & buf[offset + 1]) << 16 | (0x00ff & buf[offset + 2]) << 8 | (0x00ff & buf[offset + 3]);
    }

    public Integer[] dedup(Integer[] a) {
        Arrays.sort(a);
        int n = a.length;
        int i, k;
        k = 0;
        for (i = 1; i < n; i++) {
            if (!a[k].equals(a[i])) {
                a[k + 1] = a[i];
                k++;
            }
        }
        Integer[] deduped = new Integer[k + 1];
        for (int j = 0; j < k + 1; j++) {
            deduped[j] = a[j];
        }
        return deduped;
    }

    public PoolingDataSource configDataSource(String dbid, Properties conf) {
        String url = conf.getProperty(dbid + ".url");
        String driver = conf.getProperty(dbid + ".driver");
        String login = conf.getProperty(dbid + ".login");
        String pwd = conf.getProperty(dbid + ".pwd");
        log.debug(url + ";" + driver + ";" + login + ";" + pwd);
        PoolingDataSource ds = setupDataSource(url, driver, login, pwd);
        return ds;
    }

    public static PoolingDataSource setupDataSource(String connectURI, String jdbcDriverName, String username, String password) {
        try {
            java.lang.Class.forName(jdbcDriverName).newInstance();
        } catch (Exception e) {
            log.error("Error when attempting to obtain DB Driver: " + jdbcDriverName + " on " + new Date().toString(), e);
        }
        GenericObjectPool connectionPool = new GenericObjectPool(null, 50, GenericObjectPool.WHEN_EXHAUSTED_BLOCK, 3000, 10, false, false, 60000, 5, 30000, true);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI, username, password);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
        return dataSource;
    }

    public Map getRepoUrls(PoolingDataSource ds) {
        Map map = new HashMap();
        try {
            Connection conn = ds.getConnection();
            String sql = "select repo_id,repo_uri from  repo_id_map";
            Statement s = null;
            try {
                s = conn.createStatement();
                s.executeQuery(sql);
                ResultSet r = s.getResultSet();
                while (r.next()) {
                    Integer id = r.getInt(1);
                    String repouri = r.getString(2);
                    map.put((Object) id, (Object) repouri);
                }
            } catch (SQLException e) {
            } finally {
                try {
                    if (s != null) s.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    throw e;
                }
            }
        } catch (Exception ex) {
            log.error("problem to select from repo_id_map:" + ex.getMessage());
            throw new RuntimeException(ex);
        }
        return map;
    }

    public Map getRepoIds(PoolingDataSource ds) {
        Map map = new HashMap();
        try {
            Connection conn = ds.getConnection();
            String sql = "select repo_id,repo_uri from  repo_id_map";
            Statement s = null;
            try {
                s = conn.createStatement();
                s.executeQuery(sql);
                ResultSet r = s.getResultSet();
                while (r.next()) {
                    Integer id = r.getInt(1);
                    String repouri = r.getString(2);
                    map.put((Object) repouri, (Object) id);
                }
            } catch (SQLException e) {
            } finally {
                try {
                    if (s != null) s.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    throw e;
                }
            }
        } catch (Exception ex) {
            log.error("problem to select ids from repo_id_map:" + ex.getMessage());
            throw new RuntimeException(ex);
        }
        return map;
    }

    public Map getDates(PoolingDataSource ds, Integer[] ids) {
        Map map = new HashMap();
        try {
            Connection conn = ds.getConnection();
            String sql = "select repo_uri,sdate from repo_id_map  " + "where repo_id in (";
            int i;
            for (i = 0; i < ids.length - 1; i++) {
                sql = sql + ids[i] + ",";
            }
            sql = sql + ids[i] + ")";
            Statement s = null;
            try {
                s = conn.createStatement();
                s.executeQuery(sql);
                ResultSet r = s.getResultSet();
                while (r.next()) {
                    String repouri = r.getString(1);
                    String sdate = r.getString(2);
                    map.put(repouri, sdate);
                }
            } catch (SQLException e) {
            } finally {
                try {
                    if (s != null) s.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    throw e;
                }
            }
        } catch (Exception ex) {
            log.error("problem to select dates from repo_id_map:" + ex.getMessage());
            throw new RuntimeException(ex);
        }
        return map;
    }

    public String getRepoUrl(PoolingDataSource ds, Integer repo_id) {
        String repouri = "";
        try {
            Connection conn = ds.getConnection();
            String sql = "select repo_uri from  repo_id_map" + " where repo_id=" + repo_id;
            Statement s = null;
            try {
                s = conn.createStatement();
                s.executeQuery(sql);
                ResultSet r = s.getResultSet();
                while (r.next()) {
                    repouri = r.getString(1);
                }
            } catch (SQLException e) {
            } finally {
                try {
                    if (s != null) s.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    throw e;
                }
            }
        } catch (Exception ex) {
            log.error("problem to select repouri from repo_id_map:" + ex.getMessage());
            throw new RuntimeException(ex);
        }
        return repouri;
    }

    public Integer getRepoId(PoolingDataSource ds, String repo_uri) {
        Integer repo_id = new Integer(0);
        try {
            Connection conn = ds.getConnection();
            Statement s = null;
            try {
                s = conn.createStatement();
                s.executeQuery("select ifnull(max(repo_id),0)+1  from repo_id_map");
                ResultSet r = s.getResultSet();
                while (r.next()) {
                    repo_id = r.getInt(1);
                }
                String sql = "insert into repo_id_map(repo_id,repo_uri) values (" + repo_id + ",'" + repo_uri + "')";
                s.executeUpdate(sql);
            } catch (SQLException e) {
            } finally {
                try {
                    if (s != null) s.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    throw e;
                }
            }
        } catch (Exception ex) {
            log.error("problem to insert repouri to repo_id_map:" + ex.getMessage());
            throw new RuntimeException(ex);
        }
        return repo_id;
    }

    public String[] mapUrls(Integer[] a, Map map, PoolingDataSource ds) {
        String[] tmp = new String[a.length];
        try {
            for (int i = 0; i < a.length; i++) {
                if (map.containsKey((Object) a[i])) {
                    tmp[i] = (String) map.get((Object) a[i]);
                } else {
                    tmp[i] = getRepoUrl(ds, a[i]);
                    map.put(a[i], tmp[i]);
                }
            }
        } catch (Exception ex) {
            log.error("problem to map repouris:" + ex.getMessage());
            throw new RuntimeException(ex);
        }
        return tmp;
    }
}
