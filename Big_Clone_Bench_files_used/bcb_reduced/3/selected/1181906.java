package com.entelience.probe.httplog;

import com.entelience.sql.UsesDbObject;
import com.entelience.probe.Todo;
import com.entelience.probe.net.NetImport;
import com.entelience.sql.Db;
import com.entelience.sql.DbHelper;
import com.entelience.util.Config;
import com.entelience.util.DateHelper;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This is the main import class for the data as part of the Portal
 * module.
 **/
public class DbHttp extends UsesDbObject {

    protected PreparedStatement st_insert_temp_table;

    private static final String HTTP_REPORT = "com.entelience.report.http.HttpReport";

    protected static final org.apache.log4j.Logger _logger = com.entelience.util.Logs.getProbeLogger();

    protected static final Pattern p_slash = Pattern.compile("/");

    public static final int HTTP_PROTOCOL = 1;

    public static final int FTP_PROTOCOL = 2;

    public static final int HTTPS_PROTOCOL = 3;

    public static final int DEFAULT_PROTOCOL = 4;

    public static final String ALLOWED = "ALLOWED";

    public static final String DENIED = "DENIED";

    private DbMimeTypes mime;

    PreparedStatement pstGetDomain;

    PreparedStatement pstGetSite;

    PreparedStatement pstGetCategory;

    PreparedStatement pstAddCategory;

    PreparedStatement pstAddSite;

    PreparedStatement pstAddDomain;

    PreparedStatement pstGetPath;

    PreparedStatement pstAddPath;

    PreparedStatement pstGetUser;

    PreparedStatement pstAddUser;

    PreparedStatement pstGetInvalidUrl;

    PreparedStatement pstAddInvalidUrl;

    PreparedStatement pstAddInvalidUrlDaily;

    PreparedStatement pstUpdInvalidUrlDaily;

    NetImport net;

    public void prepare() throws Exception {
        Db db = getDb();
        mime = new DbMimeTypes();
        mime.setDb(db);
        pstAddCategory = db.prepareStatement("INSERT INTO http.t_domain_category (category_name) VALUES (?) RETURNING t_domain_category_id");
        pstGetCategory = db.prepareStatement("SELECT t_domain_category_id FROM http.t_domain_category WHERE lower(category_name) = lower(?)");
        pstAddDomain = db.prepareStatement("INSERT INTO http.t_domain (domain_name) VALUES (?) RETURNING t_domain_id");
        pstGetDomain = db.prepareStatement("SELECT t_domain_id FROM http.t_domain WHERE lower(domain_name) = lower(?)");
        pstAddSite = db.prepareStatement("INSERT INTO http.t_domain_site (site_name, t_domain_category_id, t_domain_id, intranet) VALUES (?, ?, ?, ?) RETURNING t_domain_site_id");
        pstGetSite = db.prepareStatement("SELECT t_domain_site_id FROM http.t_domain_site WHERE lower(site_name) = lower(?) AND t_domain_id = ?");
        pstGetPath = db.prepareStatement("SELECT t_site_path_id FROM http.t_site_path WHERE t_domain_site_id = ? AND path = ?");
        pstAddPath = db.prepareStatement("INSERT INTO http.t_site_path (t_domain_site_id, path) VALUES (?, ?) RETURNING t_site_path_id");
        pstGetUser = db.prepareStatement("SELECT t_user_id FROM http.t_user WHERE user_name = ? ");
        pstAddUser = db.prepareStatement("INSERT INTO http.t_user (user_name) VALUES (?) RETURNING t_user_id");
        pstAddInvalidUrl = db.prepareStatement("INSERT INTO http.t_invalid_url (invalid_url) VALUES (?) RETURNING t_invalid_url_id");
        pstGetInvalidUrl = db.prepareStatement("SELECT t_invalid_url_id FROM http.t_invalid_url WHERE lower(invalid_url) = lower(?)");
        pstAddInvalidUrlDaily = db.prepareStatement("INSERT INTO http.t_invalid_url_daily_user (nb_events, calc_day, t_invalid_url_id, t_ip_id, t_user_id) VALUES (?, ?, ?, ?, ?)");
        pstUpdInvalidUrlDaily = db.prepareStatement("UPDATE http.t_invalid_url_daily_user SET nb_events = nb_events+? WHERE calc_day = ? AND t_invalid_url_id = ? AND t_ip_id IS NOT DISTINCT FROM ? AND t_user_id IS NOT DISTINCT FROM ?");
        net = new NetImport();
        net.setDb(db);
    }

    public int addOrGetInvalidUrl(String url) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstGetInvalidUrl.setString(1, url);
            Integer id = DbHelper.getKey(pstGetInvalidUrl);
            if (id != null) return id;
            pstAddInvalidUrl.setString(1, url);
            id = DbHelper.getIntKey(pstAddInvalidUrl);
            _logger.info("Invalid URL " + url + " added");
            return id;
        } finally {
            db.exit();
        }
    }

    public void invalidUrlDaily(String url, Date d, String ip, String user) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            int urlId = addOrGetInvalidUrl(url);
            Integer ipId = null;
            if (ip != null) ipId = net.addOrGetIpIdFromIp(ip);
            Integer userId = null;
            if (user != null) userId = addOrGetUserId(user);
            pstUpdInvalidUrlDaily.setLong(1, 1);
            pstUpdInvalidUrlDaily.setDate(2, DateHelper.sqld(d));
            pstUpdInvalidUrlDaily.setInt(3, urlId);
            pstUpdInvalidUrlDaily.setObject(4, ipId);
            pstUpdInvalidUrlDaily.setObject(5, userId);
            int res = db.executeUpdate(pstUpdInvalidUrlDaily);
            if (res != 1) {
                pstAddInvalidUrlDaily.setLong(1, 1);
                pstAddInvalidUrlDaily.setDate(2, DateHelper.sqld(d));
                pstAddInvalidUrlDaily.setInt(3, urlId);
                pstAddInvalidUrlDaily.setObject(4, ipId);
                pstAddInvalidUrlDaily.setObject(5, userId);
                db.executeUpdate(pstAddInvalidUrlDaily);
            }
        } finally {
            db.exit();
        }
    }

    public void sendIncidentForNewIPsWithoutLocations(boolean sendIncidentForNewIPsWithoutLocations, String probe) throws Exception {
        net.sendIncidentForNewIPsWithoutLocations(sendIncidentForNewIPsWithoutLocations, probe);
    }

    protected int addOrGetSitePathId(int siteId, String path) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstGetPath.setInt(1, siteId);
            pstGetPath.setString(2, path);
            Integer pathId = DbHelper.getKey(pstGetPath);
            if (pathId == null) {
                pstAddPath.setInt(1, siteId);
                pstAddPath.setString(2, path);
                pathId = DbHelper.getIntKey(pstAddPath);
            }
            return pathId;
        } finally {
            db.exit();
        }
    }

    protected int addOrGetUserId(String user) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstGetUser.setString(1, obfuscateUsername(user));
            Integer userId = DbHelper.getKey(pstGetUser);
            if (userId == null) {
                pstAddUser.setString(1, obfuscateUsername(user));
                userId = DbHelper.getIntKey(pstAddUser);
            }
            return userId;
        } finally {
            db.exit();
        }
    }

    protected int addOrGetSiteId(String site, String domain) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstGetDomain.setString(1, domain);
            Integer domainId = DbHelper.getKey(pstGetDomain);
            if (domainId == null) {
                pstAddDomain.setString(1, domain);
                domainId = DbHelper.getIntKey(pstAddDomain);
            }
            pstGetSite.setString(1, site);
            pstGetSite.setInt(2, domainId);
            Integer siteId = DbHelper.getKey(pstGetSite);
            if (siteId == null) {
                pstGetCategory.setString(1, "Unavailable");
                Integer catId = DbHelper.getKey(pstGetCategory);
                if (catId == null) {
                    pstAddCategory.setString(1, "Unavailable");
                    catId = DbHelper.getIntKey(pstAddCategory);
                }
                pstAddSite.setString(1, site);
                pstAddSite.setInt(2, catId);
                pstAddSite.setInt(3, domainId);
                pstAddSite.setBoolean(4, false);
                siteId = DbHelper.getIntKey(pstAddSite);
            }
            return siteId;
        } finally {
            db.exit();
        }
    }

    protected void init() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            cnilLevel = Config.getProperty(db, "com.entelience.esis.cnilLevel", 2);
            if (cnilLevel == 0) _logger.info("CNIL Level : No obfuscation of users names"); else if (cnilLevel == 1) _logger.info("CNIL Level : SHA obfuscation of users names"); else _logger.info("CNIL Level : No informations by users");
            anonymizer = MessageDigest.getInstance("SHA");
            mime.initCaches();
        } finally {
            db.exit();
        }
    }

    private final Map<String, Integer> authCache = new HashMap<String, Integer>();

    MessageDigest anonymizer;

    protected Map<String, Date> affectedDays = new HashMap<String, Date>();

    protected Map<Integer, String> domains = new HashMap<Integer, String>();

    protected Set<Date> datesTodo = new HashSet<Date>();

    protected Map<String, Set<Integer>> pathCache;

    public boolean addNewPaths = false;

    private int cnilLevel = 2;

    protected Integer lastMaxSiteId = null;

    /**
    *   add a day in the list of the days
    **/
    protected void addDay(Date date) throws Exception {
        Calendar cal = DateHelper.newCalendarUTC();
        cal.setTime(date);
        String datecode = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
        affectedDays.put(datecode, date);
        Date rDate = DateHelper.roundDownLocal(date);
        datesTodo.add(rDate);
    }

    protected void setTodo(Db db) throws Exception {
        if (datesTodo == null || datesTodo.size() == 0) {
            return;
        }
        Date firstDate = null;
        Date lastDate = null;
        Iterator<Date> it = datesTodo.iterator();
        while (it.hasNext()) {
            Date d = it.next();
            if (firstDate == null) firstDate = d;
            if (lastDate == null) lastDate = d;
            if (d.before(firstDate)) firstDate = d;
            if (d.after(lastDate)) lastDate = d;
        }
        Todo.setIntervalTodo(db, HTTP_REPORT, firstDate, lastDate);
    }

    private String obfuscateUsername(String user) {
        if (user == null) return null;
        if (cnilLevel == 0) {
            return user;
        } else if (cnilLevel == 1) {
            anonymizer.update(user.getBytes());
            java.math.BigInteger hash = new java.math.BigInteger(1, anonymizer.digest());
            return hash.toString(16);
        } else {
            return "Anonymized user";
        }
    }

    /**
    * import a line in the DB
    **/
    protected void importIntoTable(HttpLogData data) throws Exception {
        Db db = getDb();
        st_insert_temp_table.setString(1, data.domain_name);
        st_insert_temp_table.setString(2, data.category);
        st_insert_temp_table.setString(3, data.site_name);
        st_insert_temp_table.setObject(4, null);
        st_insert_temp_table.setTimestamp(5, DateHelper.sql(data.datetime));
        st_insert_temp_table.setLong(6, data.time_taken);
        st_insert_temp_table.setInt(7, getOrCreateAuthorizationId(data.authorization));
        st_insert_temp_table.setLong(8, data.volume_client_to_server);
        st_insert_temp_table.setLong(9, data.volume_server_to_client);
        st_insert_temp_table.setInt(10, data.http_status);
        st_insert_temp_table.setString(11, data.user_agent);
        st_insert_temp_table.setString(12, data.agent_version);
        st_insert_temp_table.setInt(13, data.type);
        st_insert_temp_table.setString(14, data.ip_client);
        st_insert_temp_table.setString(15, obfuscateUsername(data.user_name));
        st_insert_temp_table.setString(16, data.mime_type);
        st_insert_temp_table.setBoolean(17, data.intranet);
        st_insert_temp_table.setString(18, data.path);
        st_insert_temp_table.setInt(19, data.specificFileWithoutReferer ? 1 : 0);
        st_insert_temp_table.setInt(20, data.robotsTxt ? 1 : 0);
        db.executeUpdate(st_insert_temp_table);
    }

    /**
    * compute Stats
    **/
    protected void computeTables() throws Exception {
        _logger.info("Start computing stats");
        fillCategories();
        _logger.info("categories computation done");
        fillUsers();
        _logger.info("Users computation done");
        fillIps();
        _logger.info("IPs computation done");
        fillIpToUser();
        _logger.info("IP to User computation done");
        fillDomains();
        _logger.info("domains computation done");
        fillSites();
        _logger.info("sites computation done");
        fillSiteDaily();
        _logger.info("day sites stats computation done");
        fillHttpDay();
        _logger.info("day http stats computation done");
        fillHttpsDay();
        _logger.info("day https stats computation done");
        fillFtpDay();
        _logger.info("day ftp stats computation done");
        fillOtherDay();
        _logger.info("day other stats computation done");
        removeDuplicateUserAgents();
        _logger.info("duplicates user agents chek done");
        fillHttpAgent();
        _logger.info("http agent computation done");
        fillHttpAgentDaily();
        _logger.info("http agent daily computation done");
        fillUserDaily();
        _logger.info("http user computation done");
        fillIPDaily();
        _logger.info("http ip computation done");
        fillPath();
        _logger.info("paths updated");
        fillPathDaily();
        _logger.info("paths/daily updated");
        fillPathPerIp();
        _logger.info("paths/ip/daily updated");
        fillPathPerUser();
        _logger.info("paths/user/daily updated");
        computeSiteAnomalies();
        _logger.info("site anomalies/daily updated");
        _logger.info("computation end");
        affectedDays = new HashMap<String, Date>();
        domains = new HashMap<Integer, String>();
    }

    private void computeSiteAnomalies() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("Computing site anomalies/daily");
            PreparedStatement pstSel = db.prepareStatement("SELECT SUM(specific_file_without_referer), SUM(robots_txt), date(log.datetime), log.site_id FROM t_http_log_data log GROUP BY 3,4");
            PreparedStatement pstUpd = db.prepareStatement("UPDATE http.t_site_anomalies_daily SET count_special_files_without_referer = count_special_files_without_referer+?, count_robots_txt = count_robots_txt+? WHERE calc_day = ? AND t_domain_site_id = ?");
            PreparedStatement pstIns = db.prepareStatement("INSERT INTO http.t_site_anomalies_daily (count_special_files_without_referer, count_robots_txt, calc_day, t_domain_site_id) VALUES (?, ?, ?, ?)");
            ResultSet rs = db.executeQuery(pstSel);
            if (rs.next()) {
                do {
                    int countSpec = rs.getInt(1);
                    int countRobots = rs.getInt(2);
                    Date d = DateHelper.toDate(rs.getDate(3));
                    int siteId = rs.getInt(4);
                    pstUpd.setInt(1, countSpec);
                    pstUpd.setInt(2, countRobots);
                    pstUpd.setDate(3, DateHelper.sqld(d));
                    pstUpd.setInt(4, siteId);
                    int res = db.executeUpdate(pstUpd);
                    if (res != 1) {
                        pstIns.setInt(1, countSpec);
                        pstIns.setInt(2, countRobots);
                        pstIns.setDate(3, DateHelper.sqld(d));
                        pstIns.setInt(4, siteId);
                        db.executeUpdate(pstIns);
                    }
                } while (rs.next());
            } else {
                _logger.debug("No site anomalies/daily to insert");
            }
        } finally {
            db.exit();
        }
    }

    private void fillPathPerIp() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("Computing paths / ip daily");
            PreparedStatement pstSel = db.prepareStatement("SELECT COUNT(*), SUM(volume_s_to_c), date(log.datetime), log.path, i.t_ip_id, log.site_id FROM t_http_log_data log INNER JOIN net.t_ip i ON host(i.ip) = log.client_ip GROUP BY date(log.datetime), log.path, i.t_ip_id, log.site_id");
            PreparedStatement pstUpd = db.prepareStatement("UPDATE http.t_path_daily_ip SET occurences = occurences + ?, volume = volume + ? WHERE calc_day = ? AND t_site_path_id = ? AND t_ip_id = ?");
            PreparedStatement pstIns = db.prepareStatement("INSERT INTO http.t_path_daily_ip (occurences, volume, calc_day, t_site_path_id, t_ip_id) VALUES (?, ?, ?, ?, ?)");
            ResultSet rs = db.executeQuery(pstSel);
            if (rs.next()) {
                do {
                    String path = rs.getString(4);
                    int site = rs.getInt(6);
                    Set<Integer> pathIds = pathCache.get(site + "-" + path);
                    if (pathIds != null) {
                        for (Iterator<Integer> it = pathIds.iterator(); it.hasNext(); ) {
                            int pathId = it.next();
                            pstUpd.setInt(1, rs.getInt(1));
                            pstUpd.setLong(2, rs.getLong(2));
                            pstUpd.setDate(3, rs.getDate(3));
                            pstUpd.setInt(4, pathId);
                            pstUpd.setInt(5, rs.getInt(5));
                            if (db.executeUpdate(pstUpd) == 0) {
                                pstIns.setInt(1, rs.getInt(1));
                                pstIns.setLong(2, rs.getLong(2));
                                pstIns.setDate(3, rs.getDate(3));
                                pstIns.setInt(4, pathId);
                                pstIns.setInt(5, rs.getInt(5));
                                db.executeUpdate(pstIns);
                            }
                        }
                    }
                } while (rs.next());
            } else {
                _logger.debug("No IP daily to insert");
            }
        } finally {
            db.exit();
        }
    }

    private void fillPathPerUser() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("Computing paths / user daily");
            PreparedStatement pstSel = db.prepareStatement("SELECT COUNT(*), SUM(volume_s_to_c), date(log.datetime), log.path, u.t_user_id, log.site_id FROM t_http_log_data log INNER JOIN http.t_user u ON u.user_name = log.username GROUP BY date(log.datetime), log.path, u.t_user_id, log.site_id");
            PreparedStatement pstUpd = db.prepareStatement("UPDATE http.t_path_daily_user SET occurences = occurences + ?, volume = volume + ? WHERE calc_day = ? AND t_site_path_id = ? AND t_user_id = ?");
            PreparedStatement pstIns = db.prepareStatement("INSERT INTO http.t_path_daily_user (occurences, volume, calc_day, t_site_path_id, t_user_id) VALUES (?, ?, ?, ?, ?)");
            ResultSet rs = db.executeQuery(pstSel);
            if (rs.next()) {
                do {
                    String path = rs.getString(4);
                    int site = rs.getInt(6);
                    Set<Integer> pathIds = pathCache.get(site + "-" + path);
                    if (pathIds != null) {
                        for (Iterator<Integer> it = pathIds.iterator(); it.hasNext(); ) {
                            int pathId = it.next();
                            pstUpd.setInt(1, rs.getInt(1));
                            pstUpd.setLong(2, rs.getLong(2));
                            pstUpd.setDate(3, rs.getDate(3));
                            pstUpd.setInt(4, pathId);
                            pstUpd.setInt(5, rs.getInt(5));
                            if (db.executeUpdate(pstUpd) == 0) {
                                pstIns.setInt(1, rs.getInt(1));
                                pstIns.setLong(2, rs.getLong(2));
                                pstIns.setDate(3, rs.getDate(3));
                                pstIns.setInt(4, pathId);
                                pstIns.setInt(5, rs.getInt(5));
                                db.executeUpdate(pstIns);
                            }
                        }
                    }
                } while (rs.next());
            } else {
                _logger.debug("No user daily to insert");
            }
        } finally {
            db.exit();
        }
    }

    private void fillPathDaily() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("Computing paths daily");
            String sqlSel1 = "SELECT DISTINCT date(log.datetime), log.request_statuscode, log.path, log.auth, log.site_id FROM t_http_log_data log WHERE log.path IS NOT NULL";
            String sqlSel2 = "SELECT d.t_site_path_id FROM http.t_site_path_daily d WHERE calc_day = ? AND request_statuscode = ? AND d.t_site_path_id = ? AND t_authorization_id = ?";
            String sqlInsert = "INSERT INTO http.t_site_path_daily (calc_day, request_statuscode, count_http, volume_in_http, volume_out_http, count_ftp, volume_in_ftp, volume_out_ftp, count_https, volume_in_https, volume_out_https, t_site_path_id, t_authorization_id)  VALUES (?, ?, 0, 0, 0, 0, 0, 0, 0, 0, 0, ?, ?)";
            String[] sqlSelUpd = new String[12];
            String[] sqlUpdate = new String[12];
            sqlUpdate[0] = "UPDATE http.t_site_path_daily SET count_http = count_http + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[0] = "SELECT COUNT(*), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=1 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id";
            sqlUpdate[1] = "UPDATE http.t_site_path_daily SET volume_in_http = volume_in_http + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[1] = "SELECT SUM(log.volume_c_to_s), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=1 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[2] = "UPDATE http.t_site_path_daily SET volume_out_http = volume_out_http + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[2] = "SELECT SUM(log.volume_s_to_c), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=1 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[3] = "UPDATE http.t_site_path_daily SET count_https = count_https + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[3] = "SELECT COUNT(*), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=3 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[4] = "UPDATE http.t_site_path_daily SET volume_in_https = volume_in_https + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[4] = "SELECT SUM(log.volume_c_to_s), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=3 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[5] = "UPDATE http.t_site_path_daily SET volume_out_https = volume_out_https + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[5] = "SELECT SUM(log.volume_s_to_c), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=3 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[6] = "UPDATE http.t_site_path_daily SET count_ftp = count_ftp + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[6] = "SELECT COUNT(*), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=2 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[7] = "UPDATE http.t_site_path_daily SET volume_in_ftp = volume_in_ftp + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[7] = "SELECT SUM(log.volume_c_to_s),log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=2 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[8] = "UPDATE http.t_site_path_daily SET volume_out_ftp = volume_out_ftp + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[8] = "SELECT SUM(log.volume_s_to_c), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=2 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[9] = "UPDATE http.t_site_path_daily SET count_other = count_other + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[9] = "SELECT COUNT(*), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=4 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[10] = "UPDATE http.t_site_path_daily SET volume_in_other = volume_in_other + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[10] = "SELECT SUM(log.volume_c_to_s), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=4 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            sqlUpdate[11] = "UPDATE http.t_site_path_daily SET volume_out_other = volume_out_other + coalesce(?, 0) WHERE t_site_path_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[11] = "SELECT SUM(log.volume_s_to_c), log.path, date(log.datetime), log.request_statuscode, auth, log.site_id FROM t_http_log_data log WHERE log.type=4 GROUP BY log.path, date(log.datetime), log.request_statuscode, auth, log.site_id ";
            PreparedStatement pstSel = db.prepareStatement(sqlSel1);
            PreparedStatement pstChk = db.prepareStatement(sqlSel2);
            PreparedStatement pstInsert = db.prepareStatement(sqlInsert);
            ResultSet rs = db.executeQuery(pstSel);
            int tmpRes = 0;
            if (rs.next()) {
                do {
                    String path = rs.getString(3);
                    int site = rs.getInt(5);
                    Set<Integer> pathIds = pathCache.get(site + "-" + path);
                    if (pathIds != null) {
                        for (Iterator<Integer> it = pathIds.iterator(); it.hasNext(); ) {
                            int pathId = it.next();
                            pstChk.setDate(1, rs.getDate(1));
                            pstChk.setInt(2, rs.getInt(2));
                            pstChk.setInt(3, pathId);
                            pstChk.setInt(4, rs.getInt(4));
                            if (DbHelper.noRows(pstChk)) {
                                pstInsert.setDate(1, rs.getDate(1));
                                pstInsert.setInt(2, rs.getInt(2));
                                pstInsert.setInt(3, pathId);
                                pstInsert.setInt(4, rs.getInt(4));
                                tmpRes += db.executeUpdate(pstInsert);
                            }
                        }
                    }
                } while (rs.next());
            } else {
                _logger.debug("fillPathDaily 1 - no rows found");
            }
            _logger.debug(tmpRes + " new path daily inserted");
            for (int i = 0; i < sqlUpdate.length; i++) {
                try {
                    db.enter();
                    PreparedStatement pstUpdate = db.prepareStatement(sqlUpdate[i]);
                    PreparedStatement pstSelUpd = db.prepareStatement(sqlSelUpd[i]);
                    ResultSet rset = db.executeQuery(pstSelUpd);
                    if (rset.next()) {
                        do {
                            String path = rset.getString(2);
                            int site = rset.getInt(6);
                            Set<Integer> pathIds = pathCache.get(site + "-" + path);
                            if (pathIds != null) {
                                for (Iterator<Integer> it = pathIds.iterator(); it.hasNext(); ) {
                                    int pathId = it.next();
                                    pstUpdate.setLong(1, rset.getLong(1));
                                    pstUpdate.setInt(2, pathId);
                                    pstUpdate.setDate(3, rset.getDate(3));
                                    pstUpdate.setInt(4, rset.getInt(4));
                                    pstUpdate.setInt(5, rset.getInt(5));
                                    db.executeUpdate(pstUpdate);
                                }
                            }
                        } while (rset.next());
                    } else {
                        _logger.debug("fillPathDaily 2[" + i + "] - no rows found");
                    }
                } finally {
                    db.exit();
                }
            }
        } finally {
            db.exit();
        }
    }

    private void fillPath() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("Computing paths");
            PreparedStatement pstChk = db.prepareStatement("SELECT t_site_path_id FROM http.t_site_path WHERE path = ? AND t_domain_site_id = ?");
            PreparedStatement pstSel = db.prepareStatement("SELECT MIN(datetime), MAX(datetime), path, site_id FROM t_http_log_data WHERE path IS NOT NULL GROUP BY path, site_id");
            PreparedStatement pstInsert = db.prepareStatement("INSERT INTO http.t_site_path (first_occurence, last_occurence, path, t_domain_site_id) VALUES (?, ?, ?, ?)");
            PreparedStatement pstUpdate = db.prepareStatement("UPDATE http.t_site_path SET first_occurence = timestamp_smaller(first_occurence, ?), last_occurence = timestamp_larger(last_occurence, ?) WHERE t_site_path_id = ?");
            ResultSet rs = db.executeQuery(pstSel);
            if (rs.next()) {
                do {
                    String path = rs.getString(3);
                    String[] parts = p_slash.split(path, 1);
                    StringBuffer currPath = new StringBuffer();
                    if (parts == null || parts.length == 0) {
                        _logger.info("Do nothing for " + path);
                        continue;
                    }
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i] == null) {
                            _logger.info("Abandon for path " + path + "(" + parts[i] + ")");
                            continue;
                        }
                        currPath.append(parts[i]);
                        pstChk.setString(1, currPath.toString());
                        pstChk.setInt(2, rs.getInt(4));
                        Integer pathId = DbHelper.getKey(pstChk);
                        if (pathId == null) {
                            if (addNewPaths) {
                                pstInsert.setTimestamp(1, rs.getTimestamp(1));
                                pstInsert.setTimestamp(2, rs.getTimestamp(2));
                                pstInsert.setString(3, currPath.toString());
                                pstInsert.setInt(4, rs.getInt(4));
                                db.executeUpdate(pstInsert);
                                _logger.info("Path " + currPath + " added");
                            } else {
                                _logger.info("Path " + currPath + " unknown and will not be added due to probe configuration");
                            }
                        } else {
                            pstUpdate.setTimestamp(1, rs.getTimestamp(1));
                            pstUpdate.setTimestamp(2, rs.getTimestamp(2));
                            pstUpdate.setInt(3, pathId.intValue());
                            db.executeUpdate(pstUpdate);
                        }
                    }
                } while (rs.next());
            } else {
                _logger.debug("No paths to insert");
            }
            _logger.info("Caching the paths");
            pathCache = new HashMap<String, Set<Integer>>();
            PreparedStatement pstCache = db.prepareStatement("SELECT DISTINCT p.t_site_path_id, log.path, p.t_domain_site_id FROM t_http_log_data log INNER JOIN http.t_site_path p ON p.t_domain_site_id = log.site_id AND log.path LIKE p.path || '%' WHERE log.type=1 ORDER BY 2,1");
            ResultSet rs2 = db.executeQuery(pstCache);
            if (rs2.next()) {
                do {
                    int id = rs2.getInt(1);
                    String path = rs2.getString(2);
                    int site = rs2.getInt(3);
                    Set<Integer> ids = pathCache.get(site + "-" + path);
                    if (ids == null) ids = new HashSet<Integer>();
                    ids.add(id);
                    pathCache.put(site + "-" + path, ids);
                } while (rs2.next());
            } else {
                _logger.debug("no path to cache");
            }
        } finally {
            db.exit();
        }
    }

    /**
     *  fill the user daily table.
     * this table is not meant to be directly used by webservices, but it will be used by reports
     */
    private void fillUserDaily() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            String sqlSelect = "SELECT COUNT(*), SUM(volume_s_to_c), date(datetime), u.t_user_id, site_id, type, mime_type, auth, request_statuscode, COALESCE(a.t_http_agent_id, 0) FROM t_http_log_data l LEFT JOIN http.t_http_agent a ON (a.agent_name = l.user_agent AND ((a.version IS NULL AND l.agent_version IS NULL) OR a.version = l.agent_version)) INNER JOIN http.t_user u ON u.user_name = username WHERE NOT l.intranet GROUP BY date(datetime), u.t_user_id, site_id, type, mime_type, site_name, auth, request_statuscode, a.t_http_agent_id";
            String sqlInsert = "INSERT INTO http.t_user_daily (occurences, volume, calc_day, t_user_id, t_domain_site_id, trafic_type, mime_type_id, t_authorization_id, request_statuscode, t_http_agent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String sqlUpdate = "UPDATE http.t_user_daily SET occurences = occurences + ?, volume = volume + ? WHERE calc_day = ? AND t_user_id = ? AND t_domain_site_id = ? AND trafic_type = ? AND mime_type_id = ? AND t_authorization_id = ? AND request_statuscode = ? AND t_http_agent_id = ?";
            PreparedStatement pstSelect = db.prepareStatement(sqlSelect);
            PreparedStatement pstInsert = db.prepareStatement(sqlInsert);
            PreparedStatement pstUpdate = db.prepareStatement(sqlUpdate);
            ResultSet rs = db.executeQuery(pstSelect);
            if (rs.next()) {
                do {
                    Integer mimeId = mime.getMimeTypeId(rs.getString(7));
                    pstUpdate.setInt(1, rs.getInt(1));
                    pstUpdate.setLong(2, rs.getLong(2));
                    pstUpdate.setDate(3, rs.getDate(3));
                    pstUpdate.setInt(4, rs.getInt(4));
                    pstUpdate.setInt(5, rs.getInt(5));
                    pstUpdate.setInt(6, rs.getInt(6));
                    pstUpdate.setObject(7, mimeId);
                    pstUpdate.setInt(8, rs.getInt(8));
                    pstUpdate.setInt(9, rs.getInt(9));
                    pstUpdate.setInt(10, rs.getInt(10));
                    if (db.executeUpdate(pstUpdate) == 0) {
                        pstInsert.setInt(1, rs.getInt(1));
                        pstInsert.setLong(2, rs.getLong(2));
                        pstInsert.setDate(3, rs.getDate(3));
                        pstInsert.setInt(4, rs.getInt(4));
                        pstInsert.setInt(5, rs.getInt(5));
                        pstInsert.setInt(6, rs.getInt(6));
                        pstInsert.setObject(7, mimeId);
                        pstInsert.setInt(8, rs.getInt(8));
                        pstInsert.setInt(9, rs.getInt(9));
                        pstInsert.setInt(10, rs.getInt(10));
                        db.executeUpdate(pstInsert);
                    }
                } while (rs.next());
            } else {
                _logger.debug("No user daily to insert");
            }
        } finally {
            db.exit();
        }
    }

    /**
     *  fill the ip daily table.
     * this table is not meant to be directly used by webservices, but it will be used by reports.
     */
    private void fillIPDaily() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            String sqlSelect = "SELECT COUNT(*), SUM(volume_s_to_c), date(datetime), i.t_ip_id, site_id, type, mime_type, auth, request_statuscode, COALESCE(a.t_http_agent_id, 0) FROM t_http_log_data l LEFT JOIN http.t_http_agent a ON (a.agent_name = l.user_agent AND ((a.version IS NULL AND l.agent_version IS NULL) OR a.version = l.agent_version)) INNER JOIN net.t_ip i ON client_ip = host(i.ip) WHERE NOT l.intranet GROUP BY date(datetime), t_ip_id, site_id, type, mime_type, site_name, auth, request_statuscode, a.t_http_agent_id";
            String sqlInsert = "INSERT INTO http.t_ip_daily (occurences, volume, calc_day, t_ip_id, t_domain_site_id, trafic_type, mime_type_id, t_authorization_id, request_statuscode, t_http_agent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String sqlUpdate = "UPDATE http.t_ip_daily SET occurences = occurences + ?, volume = volume + ? WHERE calc_day = ? AND t_ip_id = ? AND t_domain_site_id = ? AND trafic_type = ? AND mime_type_id = ? AND t_authorization_id = ? AND request_statuscode = ? AND t_http_agent_id = ?";
            PreparedStatement pstSelect = db.prepareStatement(sqlSelect);
            PreparedStatement pstInsert = db.prepareStatement(sqlInsert);
            PreparedStatement pstUpdate = db.prepareStatement(sqlUpdate);
            ResultSet rs = db.executeQuery(pstSelect);
            if (rs.next()) {
                do {
                    Integer mimeId = mime.getMimeTypeId(rs.getString(7));
                    pstUpdate.setInt(1, rs.getInt(1));
                    pstUpdate.setLong(2, rs.getLong(2));
                    pstUpdate.setDate(3, rs.getDate(3));
                    pstUpdate.setInt(4, rs.getInt(4));
                    pstUpdate.setInt(5, rs.getInt(5));
                    pstUpdate.setInt(6, rs.getInt(6));
                    pstUpdate.setObject(7, mimeId);
                    pstUpdate.setInt(8, rs.getInt(8));
                    pstUpdate.setInt(9, rs.getInt(9));
                    pstUpdate.setInt(10, rs.getInt(10));
                    if (db.executeUpdate(pstUpdate) == 0) {
                        pstInsert.setInt(1, rs.getInt(1));
                        pstInsert.setLong(2, rs.getLong(2));
                        pstInsert.setDate(3, rs.getDate(3));
                        pstInsert.setInt(4, rs.getInt(4));
                        pstInsert.setInt(5, rs.getInt(5));
                        pstInsert.setInt(6, rs.getInt(6));
                        pstInsert.setObject(7, mimeId);
                        pstInsert.setInt(8, rs.getInt(8));
                        pstInsert.setInt(9, rs.getInt(9));
                        pstInsert.setInt(10, rs.getInt(10));
                        db.executeUpdate(pstInsert);
                    }
                } while (rs.next());
            } else {
                _logger.debug("No IP daily to insert");
            }
        } finally {
            db.exit();
        }
    }

    /**
    * fill t_http_agent_daily table
    **/
    private void fillHttpAgentDaily() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fill http agent daily");
            String sqlSelect = "SELECT date(log.datetime), COUNT(*), ag.t_http_agent_id FROM t_http_log_data log, http.t_http_agent ag WHERE log.user_agent = ag.agent_name AND log.agent_version=ag.version AND (date(log.datetime), ag.t_http_agent_id) NOT IN (SELECT calc_day, t_http_agent_id FROM http.t_http_agent_daily) GROUP BY date(log.datetime), ag.t_http_agent_id";
            String sqlInsert = "INSERT INTO http.t_http_agent_daily (calc_day, count, t_http_agent_id) VALUES (?, ?, ?)";
            String sqlUpdSel = "SELECT count(*), date(datetime), user_agent, agent_version, agent.t_http_agent_id FROM t_http_log_data log, http.t_http_agent_daily dai, http.t_http_agent agent WHERE log.user_agent = agent.agent_name AND log.agent_version = agent.version AND date(log.datetime) = dai.calc_day AND dai.t_http_agent_id = agent.t_http_agent_id GROUP BY date(datetime), user_agent, agent_version, agent.t_http_agent_id";
            String sqlUpdate = "UPDATE http.t_http_agent_daily SET count = count + coalesce(0, ?) FROM http.t_http_agent WHERE  http.t_http_agent_daily.calc_day = ? AND http.t_http_agent.agent_name = ? AND http.t_http_agent.version = ? AND http.t_http_agent_daily.t_http_agent_id = http.t_http_agent.t_http_agent_id AND http.t_http_agent.t_http_agent_id = ?";
            PreparedStatement pstSelect = db.prepareStatement(sqlSelect);
            PreparedStatement pstInsert = db.prepareStatement(sqlInsert);
            ResultSet rs = db.executeQuery(pstSelect);
            if (rs.next()) {
                do {
                    pstInsert.setTimestamp(1, rs.getTimestamp(1));
                    pstInsert.setInt(2, rs.getInt(2));
                    pstInsert.setInt(3, rs.getInt(3));
                    db.executeUpdate(pstInsert);
                } while (rs.next());
            } else {
                _logger.debug("fillHttpAgentDaily 1 - no rows found.");
            }
            _logger.debug("new agents daily inserted");
            PreparedStatement pstSelUpd = db.prepareStatement(sqlUpdSel);
            PreparedStatement pstUpdate = db.prepareStatement(sqlUpdate);
            ResultSet rset = db.executeQuery(pstSelUpd);
            if (rset.next()) {
                do {
                    pstUpdate.setInt(1, rset.getInt(1));
                    pstUpdate.setTimestamp(2, rset.getTimestamp(2));
                    pstUpdate.setString(3, rset.getString(3));
                    pstUpdate.setString(4, rset.getString(4));
                    pstUpdate.setInt(5, rset.getInt(5));
                    db.executeUpdate(pstUpdate);
                } while (rset.next());
            } else {
                _logger.debug("fillHttpAgentDaily 2 - no rows found.");
            }
        } finally {
            db.exit();
        }
    }

    /**
    * fill t_http_agent table
    */
    private void fillHttpAgent() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fill http agent");
            String sqlInsert = "INSERT INTO http.t_http_agent (agent_name, version, first_occurence, last_occurence, authorized) VALUES ( ?, ?, ?, ?, 'ALLOWED')";
            String sqlSelect = "SELECT user_agent, agent_version, MIN(datetime), MAX(datetime) FROM t_http_log_data WHERE (user_agent, agent_version) NOT IN (SELECT agent_name, version FROM http.t_http_agent) AND user_agent IS NOT NULL GROUP BY user_agent, agent_version";
            String sqlUpdSel = "SELECT MIN(datetime), MAX(datetime), user_agent, agent_version FROM t_http_log_data WHERE user_agent IS NOT NULL GROUP BY user_agent, agent_version";
            String sqlUpdate = "UPDATE http.t_http_agent SET first_occurence = coalesce(timestamp_smaller(first_occurence, ?), first_occurence), last_occurence = coalesce(timestamp_larger(last_occurence, ?),last_occurence) WHERE agent_name = ? AND version = ? ";
            PreparedStatement pstSelect = db.prepareStatement(sqlSelect);
            PreparedStatement pstInsert = db.prepareStatement(sqlInsert);
            ResultSet rs = db.executeQuery(pstSelect);
            if (rs.next()) {
                do {
                    pstInsert.setString(1, rs.getString(1));
                    pstInsert.setString(2, rs.getString(2));
                    pstInsert.setTimestamp(3, rs.getTimestamp(3));
                    pstInsert.setTimestamp(4, rs.getTimestamp(4));
                    db.executeUpdate(pstInsert);
                } while (rs.next());
            } else {
                _logger.debug("fillHttpAgent 1 - no rows found.");
            }
            _logger.debug("new agents inserted");
            PreparedStatement pstSelUpd = db.prepareStatement(sqlUpdSel);
            PreparedStatement pstUpdate = db.prepareStatement(sqlUpdate);
            ResultSet rset = db.executeQuery(pstSelUpd);
            if (rset.next()) {
                do {
                    pstUpdate.setTimestamp(1, rset.getTimestamp(1));
                    pstUpdate.setTimestamp(2, rset.getTimestamp(2));
                    pstUpdate.setString(3, rset.getString(3));
                    pstUpdate.setString(4, rset.getString(4));
                    db.executeUpdate(pstUpdate);
                } while (rset.next());
            } else {
                _logger.debug("fillHttpAgent 2 - no rows found.");
            }
        } finally {
            db.exit();
        }
    }

    /**
    * remove duplicates user agents
    */
    private void removeDuplicateUserAgents() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            Map<Integer, Integer> dups = new HashMap<Integer, Integer>();
            _logger.info("checking duplicate user agents");
            String sqlSelectDupUA = "select a1.t_http_agent_id, a2.t_http_agent_id FROM http.t_http_agent a1, http.t_http_agent a2 WHERE a1.t_http_agent_id <>  a2.t_http_agent_id AND a1.agent_name = a2.agent_name AND a1.version = a2.version AND a1.authorized = 'ALLOWED' ORDER BY a1.t_http_agent_id";
            PreparedStatement pstSelDups = db.prepareStatement(sqlSelectDupUA);
            ResultSet rsDups = db.executeQuery(pstSelDups);
            if (rsDups.next()) {
                do {
                    dups.put(Integer.valueOf(rsDups.getInt(1)), Integer.valueOf(rsDups.getInt(2)));
                } while (rsDups.next());
            } else {
                _logger.debug("removeDuplicateUserAgents 1 - no rows found.");
            }
            _logger.debug(dups.size() + " user agents duplicated");
            if (dups.size() == 0) return;
            String sqlSelUAD = "SELECT count, calc_day FROM http.t_http_agent_daily WHERE t_http_agent_id = ?";
            String sqlUpdUAD = "UPDATE http.t_http_agent_daily SET count = count + ? WHERE calc_day = ? AND t_http_agent_id = ?";
            String sqlDelUAD = "DELETE FROM http.t_http_agent_daily WHERE t_http_agent_id = ?";
            PreparedStatement pstSelUAD = db.prepareStatement(sqlSelUAD);
            PreparedStatement pstUpdUAD = db.prepareStatement(sqlUpdUAD);
            PreparedStatement pstDelUAD = db.prepareStatement(sqlDelUAD);
            for (Iterator<Map.Entry<Integer, Integer>> it = dups.entrySet().iterator(); it.hasNext(); ) {
                try {
                    db.enter();
                    Map.Entry<Integer, Integer> kv = it.next();
                    Integer key = kv.getKey();
                    Integer dupToRemove = kv.getValue();
                    pstSelUAD.setInt(1, dupToRemove.intValue());
                    ResultSet rsSelUAD = db.executeQuery(pstSelUAD);
                    if (rsSelUAD.next()) {
                        do {
                            pstUpdUAD.setInt(1, rsSelUAD.getInt(1));
                            pstUpdUAD.setDate(2, rsSelUAD.getDate(2));
                            pstUpdUAD.setInt(3, key.intValue());
                            db.executeUpdate(pstUpdUAD);
                        } while (rsSelUAD.next());
                    } else {
                        _logger.debug("removeDuplicateUserAgents 2 - no rows found.");
                    }
                    pstDelUAD.setInt(1, dupToRemove.intValue());
                    db.executeUpdate(pstDelUAD);
                } finally {
                    db.exit();
                }
            }
            _logger.debug("Finished removing duplicate rows in http_agent_daily");
            String sqlSelectUADates = "SELECT first_occurence, last_occurence FROM http.t_http_agent WHERE t_http_agent_id = ?";
            String sqlUpdateUA = "UPDATE http.t_http_agent SET first_occurence = timestamp_smaller(first_occurence, ?), last_occurence = timestamp_larger(last_occurence, ?) WHERE t_http_agent_id = ?";
            String sqlDeleteUA = "DELETE FROM http.t_http_agent WHERE t_http_agent_id = ?";
            PreparedStatement pstSelDates = db.prepareStatement(sqlSelectUADates);
            PreparedStatement pstUpdUA = db.prepareStatement(sqlUpdateUA);
            PreparedStatement pstDelUA = db.prepareStatement(sqlDeleteUA);
            for (Iterator<Map.Entry<Integer, Integer>> it = dups.entrySet().iterator(); it.hasNext(); ) {
                try {
                    db.enter();
                    Map.Entry<Integer, Integer> kv = it.next();
                    Integer key = kv.getKey();
                    Integer dupToRemove = kv.getValue();
                    pstSelDates.setInt(1, dupToRemove.intValue());
                    ResultSet rsSelUA = db.executeQuery(pstSelDates);
                    if (rsSelUA.next()) {
                        pstUpdUA.setTimestamp(1, rsSelUA.getTimestamp(1));
                        pstUpdUA.setTimestamp(2, rsSelUA.getTimestamp(2));
                        pstUpdUA.setInt(3, key.intValue());
                        db.executeUpdate(pstUpdUA);
                    }
                    pstDelUA.setInt(1, dupToRemove.intValue());
                    db.executeUpdate(pstDelUA);
                } finally {
                    db.exit();
                }
            }
            _logger.debug("Finished removing duplicate rows in http_agent");
        } finally {
            db.exit();
        }
    }

    /**
    * fill t_other_day table
    */
    private void fillOtherDay() throws Exception {
        _logger.info("fill other day");
        fillProtocolDay(DEFAULT_PROTOCOL);
    }

    /**
    * fill t_https_day table
    */
    private void fillHttpsDay() throws Exception {
        _logger.info("fill https day");
        fillProtocolDay(HTTPS_PROTOCOL);
    }

    /**
    * fill t_ftp_day table
    */
    private void fillFtpDay() throws Exception {
        _logger.info("fill ftp day");
        fillProtocolDay(FTP_PROTOCOL);
    }

    private void fillProtocolDay(int protocolId) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            String tableName;
            if (protocolId == HTTP_PROTOCOL) tableName = "http.t_http_day"; else if (protocolId == FTP_PROTOCOL) tableName = "http.t_ftp_day"; else if (protocolId == HTTPS_PROTOCOL) tableName = "http.t_https_day"; else if (protocolId == DEFAULT_PROTOCOL) tableName = "http.t_other_day"; else tableName = "http.t_other_day";
            StringBuffer sqlSelect = new StringBuffer("SELECT DISTINCT date(log.datetime) FROM t_http_log_data log WHERE date(log.datetime) NOT IN (SELECT calc_day FROM ");
            sqlSelect.append(tableName).append(')');
            StringBuffer sqlInsert = new StringBuffer("INSERT INTO ");
            sqlInsert.append(tableName).append(" (calc_day, first_request_hour, last_request_hour, count, volume_in, volume_out, total_time, count_intranet, count_internet, volume_in_internet, volume_out_internet, volume_in_intranet, volume_out_intranet) VALUES (?, time '23:59:59', time '00:00:01', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)");
            String sqlSelUpd = "SELECT MIN(pg_catalog.time (datetime)), MAX(pg_catalog.time (datetime)), count(*), SUM(volume_s_to_c) AS volume_in, SUM(volume_c_to_s) AS volume_out, SUM(time_taken), date(datetime) FROM t_http_log_data WHERE type=? GROUP BY date(datetime)";
            String sqlSelUpdInter = "SELECT count(*), SUM(volume_s_to_c) AS volume_in, SUM(volume_c_to_s) AS volume_out, date(datetime) FROM t_http_log_data WHERE type=? AND NOT intranet GROUP BY date(datetime)";
            String sqlSelUpdIntra = "SELECT count(*), SUM(volume_s_to_c) AS volume_in, SUM(volume_c_to_s) AS volume_out, date(datetime) FROM t_http_log_data WHERE type=? AND intranet GROUP BY date(datetime)";
            StringBuffer sqlUpdate = new StringBuffer("UPDATE ");
            sqlUpdate.append(tableName).append(" SET first_request_hour = time_smaller(first_request_hour,coalesce(?, time '23:59:59')), last_request_hour = time_larger(last_request_hour,coalesce( ?, time '00:00:01')), count = count + coalesce(?, 0), volume_in = volume_in + coalesce(?, 0), volume_out = volume_out + coalesce(?, 0), total_time = total_time + coalesce(? ,0) WHERE calc_day = ?");
            StringBuffer sqlUpdateInter = new StringBuffer("UPDATE ");
            sqlUpdateInter.append(tableName).append(" SET count_internet = count_internet + coalesce(?, 0), volume_in_internet = volume_in_internet + coalesce(?, 0), volume_out_internet = volume_out_internet + coalesce(?, 0) WHERE calc_day = ?");
            StringBuffer sqlUpdateIntra = new StringBuffer("UPDATE ");
            sqlUpdateIntra.append(tableName).append(" SET count_intranet = count_intranet + coalesce(?, 0), volume_in_intranet = volume_in_intranet + coalesce(?, 0), volume_out_intranet = volume_out_intranet + coalesce(?, 0) WHERE calc_day = ?");
            PreparedStatement pstSelect = db.prepareStatement(sqlSelect.toString());
            PreparedStatement pstInsert = db.prepareStatement(sqlInsert.toString());
            ResultSet rs = db.executeQuery(pstSelect);
            if (rs.next()) {
                do {
                    pstInsert.setDate(1, rs.getDate(1));
                    db.executeUpdate(pstInsert);
                } while (rs.next());
            } else {
                _logger.debug("fillProtocolDay(" + protocolId + ") 1 - no rows found.");
            }
            _logger.debug("new protocol " + protocolId + " inserted");
            {
                PreparedStatement pstSelUpd = db.prepareStatement(sqlSelUpd);
                pstSelUpd.setInt(1, protocolId);
                PreparedStatement pstUpdate = db.prepareStatement(sqlUpdate.toString());
                ResultSet rss = db.executeQuery(pstSelUpd);
                if (rss.next()) {
                    do {
                        pstUpdate.setTime(1, rss.getTime(1));
                        pstUpdate.setTime(2, rss.getTime(2));
                        pstUpdate.setInt(3, rss.getInt(3));
                        pstUpdate.setLong(4, rss.getLong(4));
                        pstUpdate.setLong(5, rss.getLong(5));
                        pstUpdate.setLong(6, rss.getLong(6));
                        pstUpdate.setDate(7, rss.getDate(7));
                        db.executeUpdate(pstUpdate);
                    } while (rss.next());
                } else {
                    _logger.debug("fillProtocolDay(" + protocolId + ") 2 - no rows found.");
                }
            }
            {
                PreparedStatement pstSelUpd = db.prepareStatement(sqlSelUpdInter);
                pstSelUpd.setInt(1, protocolId);
                PreparedStatement pstUpdate = db.prepareStatement(sqlUpdateInter.toString());
                ResultSet rss = db.executeQuery(pstSelUpd);
                if (rss.next()) {
                    do {
                        pstUpdate.setInt(1, rss.getInt(1));
                        pstUpdate.setLong(2, rss.getLong(2));
                        pstUpdate.setLong(3, rss.getLong(3));
                        pstUpdate.setDate(4, rss.getDate(4));
                        db.executeUpdate(pstUpdate);
                    } while (rss.next());
                } else {
                    _logger.debug("fillProtocolDay(" + protocolId + ") 3 - no rows found.");
                }
            }
            {
                PreparedStatement pstSelUpd = db.prepareStatement(sqlSelUpdIntra);
                pstSelUpd.setInt(1, protocolId);
                PreparedStatement pstUpdate = db.prepareStatement(sqlUpdateIntra.toString());
                ResultSet rss = db.executeQuery(pstSelUpd);
                if (rss.next()) {
                    do {
                        pstUpdate.setInt(1, rss.getInt(1));
                        pstUpdate.setLong(2, rss.getLong(2));
                        pstUpdate.setLong(3, rss.getLong(3));
                        pstUpdate.setDate(4, rss.getDate(4));
                        db.executeUpdate(pstUpdate);
                    } while (rss.next());
                } else {
                    _logger.debug("fillProtocolDay(" + protocolId + ") 4 - no rows found.");
                }
            }
        } finally {
            db.exit();
        }
    }

    /**
    * fill t_http_day table
    */
    private void fillHttpDay() throws Exception {
        _logger.info("fill http day");
        fillProtocolDay(HTTP_PROTOCOL);
    }

    /**
    * fill t_domain_site_daily table
    */
    private void fillSiteDaily() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fill http site daily");
            String sqlSel1 = "SELECT DISTINCT date(log.datetime), log.request_statuscode, log.site_id, log.auth FROM t_http_log_data log WHERE NOT intranet";
            String sqlSel2 = "SELECT t_domain_site_id FROM http.t_domain_site_daily WHERE calc_day = ? AND request_statuscode = ? AND t_domain_site_id = ? AND t_authorization_id = ? ";
            String sqlInsert = "INSERT INTO http.t_domain_site_daily (calc_day, request_statuscode, count_http, volume_in_http, volume_out_http, count_ftp, volume_in_ftp, volume_out_ftp, count_https, volume_in_https, volume_out_https, t_domain_site_id, t_authorization_id)  VALUES (?, ?, 0, 0, 0, 0, 0, 0, 0, 0, 0, ?, ?)";
            String[] sqlSelUpd = new String[12];
            String[] sqlUpdate = new String[12];
            sqlUpdate[0] = "UPDATE http.t_domain_site_daily SET count_http = count_http + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[0] = "SELECT COUNT(*), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=1  AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[1] = "UPDATE http.t_domain_site_daily SET volume_in_http = volume_in_http + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[1] = "SELECT SUM(log.volume_c_to_s), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=1 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[2] = "UPDATE http.t_domain_site_daily SET volume_out_http = volume_out_http + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[2] = "SELECT SUM(log.volume_s_to_c), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=1 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[3] = "UPDATE http.t_domain_site_daily SET count_https = count_https + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[3] = "SELECT COUNT(*), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=3 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[4] = "UPDATE http.t_domain_site_daily SET volume_in_https = volume_in_https + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[4] = "SELECT SUM(log.volume_c_to_s), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=3 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[5] = "UPDATE http.t_domain_site_daily SET volume_out_https = volume_out_https + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[5] = "SELECT SUM(log.volume_s_to_c), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=3 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[6] = "UPDATE http.t_domain_site_daily SET count_ftp = count_ftp + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[6] = "SELECT COUNT(*), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=2 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[7] = "UPDATE http.t_domain_site_daily SET volume_in_ftp = volume_in_ftp + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[7] = "SELECT SUM(log.volume_c_to_s), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=2 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[8] = "UPDATE http.t_domain_site_daily SET volume_out_ftp = volume_out_ftp + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[8] = "SELECT SUM(log.volume_s_to_c), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=2 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[9] = "UPDATE http.t_domain_site_daily SET count_other = count_other + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[9] = "SELECT COUNT(*), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=4 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[10] = "UPDATE http.t_domain_site_daily SET volume_in_other = volume_in_other + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[10] = "SELECT SUM(log.volume_c_to_s), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=4 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            sqlUpdate[11] = "UPDATE http.t_domain_site_daily SET volume_out_other = volume_out_other + coalesce(?, 0) WHERE t_domain_site_id = ? AND calc_day = ? AND request_statuscode = ? AND t_authorization_id = ?";
            sqlSelUpd[11] = "SELECT SUM(log.volume_s_to_c), log.site_id, date(log.datetime), log.request_statuscode, auth FROM t_http_log_data log WHERE log.type=4 AND NOT intranet GROUP BY log.site_id, date(log.datetime), log.request_statuscode, auth";
            PreparedStatement pstSel = db.prepareStatement(sqlSel1);
            PreparedStatement pstChk = db.prepareStatement(sqlSel2);
            PreparedStatement pstInsert = db.prepareStatement(sqlInsert);
            ResultSet rs = db.executeQuery(pstSel);
            int tmpRes = 0;
            if (rs.next()) {
                do {
                    pstChk.setDate(1, rs.getDate(1));
                    pstChk.setInt(2, rs.getInt(2));
                    pstChk.setInt(3, rs.getInt(3));
                    pstChk.setInt(4, rs.getInt(4));
                    if (DbHelper.noRows(pstChk)) {
                        pstInsert.setDate(1, rs.getDate(1));
                        pstInsert.setInt(2, rs.getInt(2));
                        pstInsert.setInt(3, rs.getInt(3));
                        pstInsert.setInt(4, rs.getInt(4));
                        tmpRes += db.executeUpdate(pstInsert);
                    }
                } while (rs.next());
            } else {
                _logger.debug("fillSiteDaily 1 - no rows found");
            }
            _logger.debug(tmpRes + "new site daily inserted");
            for (int i = 0; i < sqlUpdate.length; i++) {
                try {
                    db.enter();
                    PreparedStatement pstUpdate = db.prepareStatement(sqlUpdate[i]);
                    PreparedStatement pstSelUpd = db.prepareStatement(sqlSelUpd[i]);
                    ResultSet rset = db.executeQuery(pstSelUpd);
                    if (rset.next()) {
                        do {
                            pstUpdate.setLong(1, rset.getLong(1));
                            pstUpdate.setLong(2, rset.getLong(2));
                            pstUpdate.setDate(3, rset.getDate(3));
                            pstUpdate.setInt(4, rset.getInt(4));
                            pstUpdate.setInt(5, rset.getInt(5));
                            db.executeUpdate(pstUpdate);
                        } while (rset.next());
                    } else {
                        _logger.debug("fillSiteDaily 2[" + i + "] - no rows found");
                    }
                } finally {
                    db.exit();
                }
            }
        } finally {
            db.exit();
        }
    }

    /**
    * add site_id in the temp table. VERY USEFUL for the computations of stats
    * since we re not forced to always do JOINS between lot of tables
    */
    private void fillTempTableWithId() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fill temp table with id");
            StringBuffer sqlSelect = new StringBuffer("SELECT DISTINCT t_domain_site_id, sit.site_name, dom.domain_name FROM http.t_domain_site sit INNER JOIN http.t_domain dom ON sit.t_domain_id = dom.t_domain_id INNER JOIN t_http_log_data log ON (log.site_name = sit.site_name AND log.domain_name = dom.domain_name) ");
            PreparedStatement pstSelect = db.prepareStatement(sqlSelect.toString());
            PreparedStatement pstUpdate = db.prepareStatement("UPDATE t_http_log_data SET site_id = ? WHERE site_id IS NULL AND site_name = ? AND domain_name = ?");
            int i = 0;
            ResultSet rs = db.executeQuery(pstSelect);
            if (rs.next()) {
                do {
                    pstUpdate.setInt(1, rs.getInt(1));
                    pstUpdate.setString(2, rs.getString(2));
                    pstUpdate.setString(3, rs.getString(3));
                    i += db.executeUpdate(pstUpdate);
                } while (rs.next());
            } else {
                _logger.debug("fillTempTableWithId - no rows found.");
            }
            _logger.debug(i + " temp table site_id updated");
            if (DbHelper.indexExists(db, "t_http_log_site_id")) {
                _logger.warn("Index t_http_log_site_id already exists. Wont be created");
            } else {
                PreparedStatement createIndex = db.prepareStatement(create_index4);
                db.executeUpdate(createIndex);
                _logger.debug("temp index recreated");
            }
        } finally {
            db.exit();
        }
    }

    /**
    * fill t_domain_site table
    */
    private void fillSites() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fill sites ");
            StringBuffer sqlSelect = new StringBuffer("SELECT distinct log.site_name, MIN(log.datetime), MAX(log.datetime), MIN(cat.t_domain_category_id), dom.t_domain_id, log.intranet FROM t_http_log_data log INNER JOIN http.t_domain dom ON dom.domain_name = log.domain_name INNER JOIN http.t_domain_category cat ON cat.category_name=log.category WHERE (log.site_name, log.domain_name) NOT IN ( SELECT sit.site_name, dom.domain_name FROM http.t_domain_site sit INNER JOIN http.t_domain dom ON sit.t_domain_id = dom.t_domain_id WHERE true ");
            String sqlSelectEnd = ") GROUP BY log.site_name, dom.t_domain_id, log.intranet";
            String sqlInsert = "INSERT INTO http.t_domain_site (site_name, first_occurence, last_occurence, t_domain_category_id, t_domain_id, intranet) VALUES (?, ?, ?, ?, ?, ?) ";
            String sqlSelectUpd = "select min(datetime), max(datetime), site_id from t_http_log_data GROUP BY site_id";
            String sqlUpdate = "UPDATE http.t_domain_site set first_occurence = coalesce(timestamp_smaller(?, first_occurence), first_occurence), last_occurence = coalesce(timestamp_larger(?, last_occurence), last_occurence) WHERE t_domain_site_id = ?";
            StringBuffer tmpSql = null;
            for (Iterator<Integer> it = domains.keySet().iterator(); it.hasNext(); ) {
                String tmp = it.next().toString();
                if (tmpSql == null) tmpSql = new StringBuffer(" AND dom.t_domain_id IN ("); else tmpSql.append(", ");
                tmpSql.append(tmp);
            }
            if (tmpSql != null) {
                tmpSql.append(')');
                sqlSelect.append(tmpSql);
            }
            sqlSelect.append(sqlSelectEnd);
            PreparedStatement pstSelect = db.prepareStatement(sqlSelect.toString());
            PreparedStatement pstInsert = db.prepareStatement(sqlInsert);
            ResultSet rs = db.executeQuery(pstSelect);
            _logger.debug("Selection done");
            int nbInserted = 0;
            if (rs.next()) {
                do {
                    pstInsert.setString(1, rs.getString(1));
                    pstInsert.setTimestamp(2, rs.getTimestamp(2));
                    pstInsert.setTimestamp(3, rs.getTimestamp(3));
                    pstInsert.setInt(4, rs.getInt(4));
                    pstInsert.setInt(5, rs.getInt(5));
                    pstInsert.setBoolean(6, rs.getBoolean(6));
                    nbInserted += db.executeUpdate(pstInsert);
                } while (rs.next());
            } else {
                _logger.debug("fillSites 1 - no rows found.");
            }
            _logger.debug(nbInserted + " new sites inserted");
            fillTempTableWithId();
            PreparedStatement pstSelectUpd = db.prepareStatement(sqlSelectUpd);
            PreparedStatement pstUpdate = db.prepareStatement(sqlUpdate);
            ResultSet resss = db.executeQuery(pstSelectUpd);
            if (resss.next()) {
                do {
                    pstUpdate.setTimestamp(1, resss.getTimestamp(1));
                    pstUpdate.setTimestamp(2, resss.getTimestamp(2));
                    pstUpdate.setInt(3, resss.getInt(3));
                    db.executeUpdate(pstUpdate);
                } while (resss.next());
            } else {
                _logger.debug("fillSites 2 - no rows found.");
            }
        } finally {
            db.exit();
        }
    }

    /**
    * fill t_domain table
    */
    private void fillDomains() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fill domains");
            String sqlInsert = "INSERT INTO http.t_domain (domain_name, first_occurence, last_occurence) VALUES (?, ?, ?)";
            String sqlSelectUpd = "SELECT MIN(datetime), MAX(datetime), domain_name FROM t_http_log_data GROUP BY domain_name";
            String sqlUpdate = "update http.t_domain set first_occurence = coalesce(timestamp_smaller(?, first_occurence), first_occurence), last_occurence =  coalesce(timestamp_larger(?, last_occurence), last_occurence) WHERE domain_name = ?";
            String sqlSelectLog = "SELECT log.domain_name, MIN(log.datetime), MAX(log.datetime) FROM t_http_log_data log GROUP BY domain_name";
            String sqlSelectExisting = "SELECT domain_name FROM http.t_domain WHERE domain_name = ?";
            PreparedStatement pstSelect = db.prepareStatement(sqlSelectLog);
            PreparedStatement pstFilterExisting = db.prepareStatement(sqlSelectExisting);
            PreparedStatement pstInsert = db.prepareStatement(sqlInsert);
            int nbDomsInserted = 0;
            ResultSet rs = db.executeQuery(pstSelect);
            if (rs.next()) {
                do {
                    pstFilterExisting.setString(1, rs.getString(1));
                    if (DbHelper.noRows(pstFilterExisting)) {
                        pstInsert.setString(1, rs.getString(1));
                        pstInsert.setTimestamp(2, rs.getTimestamp(2));
                        pstInsert.setTimestamp(3, rs.getTimestamp(3));
                        nbDomsInserted += db.executeUpdate(pstInsert);
                    }
                } while (rs.next());
            } else {
                _logger.debug("fillDomains 1 - no rows found.");
            }
            _logger.debug(nbDomsInserted + " new domains inserted");
            PreparedStatement pstSelectUpd = db.prepareStatement(sqlSelectUpd);
            PreparedStatement pstUpdate = db.prepareStatement(sqlUpdate);
            int nbDoms = 0;
            ResultSet resss = db.executeQuery(pstSelectUpd);
            if (resss.next()) {
                do {
                    pstUpdate.setTimestamp(1, resss.getTimestamp(1));
                    pstUpdate.setTimestamp(2, resss.getTimestamp(2));
                    pstUpdate.setString(3, resss.getString(3));
                    nbDoms += db.executeUpdate(pstUpdate);
                } while (resss.next());
            } else {
                _logger.debug("fillDomains 2 - no rows found");
            }
            _logger.debug(nbDoms + " domains updated");
            PreparedStatement pst = db.prepareStatement("select distinct t_domain_id, domain_name FROM http.t_domain WHERE domain_name IN (SELECT DISTINCT domain_name FROM t_http_log_data)");
            ResultSet rsss = db.executeQuery(pst);
            if (rsss.next()) {
                do {
                    domains.put(Integer.valueOf(rsss.getInt(1)), rsss.getString(2));
                } while (rsss.next());
            } else {
                _logger.debug("fillDomains 3 - no rows found");
            }
        } finally {
            db.exit();
        }
    }

    private int getOrCreateAuthorizationId(String auth) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            Integer authId = authCache.get(auth);
            if (authId != null) return authId.intValue();
            PreparedStatement pst = db.prepareStatement("SELECT t_authorization_id FROM http.t_authorization WHERE lower(authorization_name) = lower(?)");
            pst.setString(1, auth);
            authId = DbHelper.getKey(pst);
            if (authId != null) {
                authCache.put(auth, authId);
                return authId.intValue();
            }
            pst = db.prepareStatement("INSERT INTO http.t_authorization(authorization_name)  VALUES (?) RETURNING t_authorization_id");
            pst.setString(1, auth);
            authId = DbHelper.getKey(pst);
            if (authId == null) throw new Exception("Error when adding a new authorization");
            authCache.put(auth, authId);
            return authId;
        } finally {
            db.exit();
        }
    }

    private void fillUsers() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fill users");
            String sql = "INSERT INTO http.t_user (user_name) SELECT DISTINCT username FROM t_http_log_data WHERE username IS NOT NULL AND username NOT IN (SELECT user_name FROM http.t_user)";
            PreparedStatement pst = db.prepareStatement(sql);
            db.executeUpdate(pst);
        } finally {
            db.exit();
        }
    }

    private void fillIps() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fill IPs");
            String sql = "SELECT DISTINCT client_ip FROM t_http_log_data WHERE client_ip IS NOT NULL AND client_ip NOT IN (SELECT host(ip) FROM net.t_ip)";
            PreparedStatement pst = db.prepareStatement(sql);
            ResultSet rs = db.executeQuery(pst);
            if (rs.next()) {
                do {
                    net.addOrGetIpIdFromIp(rs.getString(1));
                } while (rs.next());
            } else {
                _logger.debug("No new IP to insert");
            }
        } finally {
            db.exit();
        }
    }

    private void fillIpToUser() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fillIpToUser");
            PreparedStatement pstSel = db.prepareStatement("SELECT DISTINCT date(d.datetime), i.t_ip_id, u.t_user_id FROM t_http_log_data d INNER JOIN net.t_ip i ON host(i.ip) = d.client_ip INNER JOIN http.t_user u ON u.user_name = d.username");
            ResultSet rs = db.executeQuery(pstSel);
            PreparedStatement pstIns = db.prepareStatement("INSERT INTO http.t_ip_users_daily(calc_day, t_ip_id, t_user_id) VALUES (?, ?, ?)");
            PreparedStatement pstChk = db.prepareStatement("SELECT t_ip_id FROM http.t_ip_users_daily WHERE calc_day = ? AND t_ip_id = ? AND t_user_id = ?");
            if (rs.next()) {
                do {
                    pstChk.setDate(1, rs.getDate(1));
                    pstChk.setInt(2, rs.getInt(2));
                    pstChk.setInt(3, rs.getInt(3));
                    if (DbHelper.noRows(pstChk)) {
                        pstIns.setDate(1, rs.getDate(1));
                        pstIns.setInt(2, rs.getInt(2));
                        pstIns.setInt(3, rs.getInt(3));
                        db.executeUpdate(pstIns);
                    }
                } while (rs.next());
            } else {
                _logger.debug("fillIpToUser no row found");
            }
        } finally {
            db.exit();
        }
    }

    /**
    * fill t_domain_category table
    */
    private void fillCategories() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("fill categories");
            String sql = "INSERT INTO http.t_domain_category (category_name) SELECT DISTINCT category FROM t_http_log_data WHERE category IS NOT NULL AND category NOT IN (SELECT category_name FROM http.t_domain_category)";
            PreparedStatement pst = db.prepareStatement(sql);
            db.executeUpdate(pst);
        } finally {
            db.exit();
        }
    }

    protected String sql_insert_temp_table = "INSERT INTO t_http_log_data (domain_name, category, site_name, site_id, datetime, time_taken, auth, volume_c_to_s, volume_s_to_c, request_statuscode, user_agent, agent_version, type, client_ip, username, mime_type, intranet, path, specific_file_without_referer, robots_txt) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    /**
     * For this probe, we drop and recreate the temp table, for gaining performance, before and after each import
     */
    public void createTempTable() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("Creating temporary table t_http_log_data");
            PreparedStatement createTable = db.prepareStatement(create_temp_table);
            db.executeUpdate(createTable);
        } catch (Exception e) {
            _logger.fatal("Error when creating temporary table t_http_log_data", e);
            throw e;
        } finally {
            db.exit();
        }
        st_insert_temp_table = db.prepareStatement(sql_insert_temp_table);
    }

    public void dropTempTable() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            dropTempIndexes();
            _logger.info("Dropping temporary table t_http_log_data");
            PreparedStatement dropTable = db.prepareStatement(drop_temp_table);
            db.executeUpdate(dropTable);
        } finally {
            db.exit();
        }
    }

    public void dropTempIndexes() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("Drop temp table indexes");
            PreparedStatement dropIndex;
            if (DbHelper.indexExists(db, "t_http_log_domain_name")) {
                dropIndex = db.prepareStatement(drop_index1);
                db.executeUpdate(dropIndex);
            } else {
                _logger.warn("Index t_http_log_domain_name does not exists.");
            }
            if (DbHelper.indexExists(db, "t_http_log_site_name")) {
                dropIndex = db.prepareStatement(drop_index2);
                db.executeUpdate(dropIndex);
            } else {
                _logger.warn("Index t_http_log_site_name does not exists.");
            }
            if (DbHelper.indexExists(db, "t_http_log_user_agent")) {
                dropIndex = db.prepareStatement(drop_index3);
                db.executeUpdate(dropIndex);
            } else {
                _logger.warn("Index t_http_log_user_agent does not exists.");
            }
            if (DbHelper.indexExists(db, "t_http_log_site_id")) {
                dropIndex = db.prepareStatement(drop_index4);
                db.executeUpdate(dropIndex);
            } else {
                _logger.warn("Index t_http_log_site_id does not exists.");
            }
            if (DbHelper.indexExists(db, "t_http_log_type")) {
                dropIndex = db.prepareStatement(drop_index5);
                db.executeUpdate(dropIndex);
            } else {
                _logger.warn("Index t_http_log_type does not exists.");
            }
            _logger.debug("indexes removed");
        } finally {
            db.exit();
        }
    }

    public void recreateTempTableIndexes() throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("recreation of indexes");
            PreparedStatement createIndex;
            if (DbHelper.indexExists(db, "t_http_log_domain_name")) {
                _logger.warn("Index t_http_log_domain_name already exists. Wont be created");
            } else {
                createIndex = db.prepareStatement(create_index1);
                db.executeUpdate(createIndex);
            }
            if (DbHelper.indexExists(db, "t_http_log_site_name")) {
                _logger.warn("Index t_http_log_site_name already exists. Wont be created");
            } else {
                createIndex = db.prepareStatement(create_index2);
                db.executeUpdate(createIndex);
            }
            if (DbHelper.indexExists(db, "t_http_log_user_agent")) {
                _logger.warn("Index t_http_log_user_agent already exists. Wont be created");
            } else {
                createIndex = db.prepareStatement(create_index3);
                db.executeUpdate(createIndex);
            }
            if (DbHelper.indexExists(db, "t_http_log_type")) {
                _logger.warn("Index t_http_log_type already exists. Wont be created");
            } else {
                createIndex = db.prepareStatement(create_index5);
                db.executeUpdate(createIndex);
            }
            _logger.debug("new indexes created");
        } finally {
            db.exit();
        }
    }

    private static final String drop_temp_table = "DROP TABLE t_http_log_data";

    private static final String create_temp_table = "CREATE TEMPORARY TABLE t_http_log_data (domain_name text, category text, site_name text, site_id int, datetime timestamp, time_taken bigint, auth int, volume_c_to_s bigint, volume_s_to_c bigint, request_statuscode int, user_agent text, agent_version text, type int, client_ip text, username text, mime_type text, intranet boolean, path text, specific_file_without_referer int, robots_txt int)";

    private static final String drop_index1 = "DROP INDEX t_http_log_domain_name";

    private static final String drop_index2 = "DROP INDEX t_http_log_site_name";

    private static final String drop_index3 = "DROP INDEX t_http_log_user_agent";

    private static final String drop_index4 = "DROP INDEX t_http_log_site_id";

    private static final String drop_index5 = "DROP INDEX t_http_log_type";

    private static final String create_index1 = "CREATE INDEX t_http_log_domain_name ON t_http_log_data (domain_name)";

    private static final String create_index2 = "CREATE INDEX t_http_log_site_name ON t_http_log_data (site_name)";

    private static final String create_index3 = "CREATE INDEX t_http_log_user_agent ON t_http_log_data (user_agent)";

    private static final String create_index4 = "CREATE INDEX t_http_log_site_id ON t_http_log_data (site_id)";

    private static final String create_index5 = "CREATE INDEX t_http_log_type ON t_http_log_data (type)";
}
