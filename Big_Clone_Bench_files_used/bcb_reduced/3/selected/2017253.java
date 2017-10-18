package org.amiwall.instrument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.amiwall.db.ConnectionPool;
import org.amiwall.plugin.Install;
import org.amiwall.plugin.Plugin;
import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 *  Description of the Class
 *
 *@author    Nick Cunnah
 */
public class WebMailRequests extends AbstractDbPsInstrument implements Install {

    Logger log = Logger.getLogger("org.amiwall.instrument.WebMailRequests");

    private String calcMetricSql2;

    private long requestWithinMs = 10 * 1000;

    /**
     *  Gets the name attribute of the TotalResponseBandwidth object
     *
     *@return    The name value
     */
    public String getName() {
        return "WebMailRequests";
    }

    /**
     *  Gets the description attribute of the WebMailRequests object
     *
     *@return    The description value
     */
    public String getDescription() {
        return "Web Mail";
    }

    /**
     *  Gets the units attribute of the WebMailRequests object
     *
     *@return    The units value
     */
    public String getUnits() {
        return "pages";
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    public void digest(Element root) {
        super.digest(root);
        setRequestwithinms(Long.parseLong(root.getChildTextTrim("requestwithinms")));
    }

    /**
     *  Sets the requestwithinms attribute of the WebMailRequests object
     *
     *@param  requestwithinms  The new requestwithinms value
     */
    public void setRequestwithinms(long requestwithinms) {
        this.requestWithinMs = requestwithinms;
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activate() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("activate");
        }
        super.activate();
        calcMetricSql2 = "SELECT 1" + " FROM webmail wm, http h, http h2" + " WHERE h.id=?" + " AND h.userId = h2.userId" + " AND h2.time >= h.time" + " AND h2.time <= h.time+?" + " AND h2.url LIKE wm.url";
        if (calcMetricSql2 == null) {
            throw new Exception("Cant find resource calcmetric2.sql");
        }
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void install() throws Exception {
        String sql = "CREATE TABLE webmail (" + " id BIGINT PRIMARY KEY," + " url VARCHAR(255)" + "+ );";
        String sqlData = "INSERT INTO webmail(id, url) VALUES (1, '%hotmail.com%'); " + "INSERT INTO webmail(id, url) VALUES (2, '%mail.yahoo%');";
        if (sql == null) {
            throw new NullPointerException("Failed to load webmail-create.sql");
        }
        if (sqlData == null) {
            throw new NullPointerException("Failed to load webmail-data.sql");
        }
        ConnectionPool.executeUpdate(sql);
        ConnectionPool.executeUpdate(sqlData);
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void uninstall() throws Exception {
        String sql = "DROP TABLE webmail";
        if (sql == null) {
            throw new NullPointerException("Failed to load webmail-drop.sql");
        }
        ConnectionPool.executeUpdate(sql);
    }

    /**
     *  Note that we have to do this sql the long way because sites such as
     *  yahoo use redirect on the nav bar. So if we did it simply, the redirect
     *  would register as the main page and not the actual target.
     *
     *@param  userId         Description of the Parameter
     *@param  startTime      Description of the Parameter
     *@param  endTime        Description of the Parameter
     *@return                Description of the Return Value
     *@exception  Exception  Description of the Exception
     */
    public long calcMetric(String userId, long startTime, long endTime) throws Exception {
        Connection con = null;
        PreparedStatement pstmt = null;
        PreparedStatement pstmt2 = null;
        long metric = 0;
        try {
            con = ConnectionPool.getConnection();
            pstmt = con.prepareStatement(calcMetricSql);
            pstmt2 = con.prepareStatement(calcMetricSql2);
            pstmt.setString(1, userId);
            pstmt.setLong(2, startTime);
            pstmt.setLong(3, endTime);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                if (log.isDebugEnabled()) {
                    log.debug("try id=" + id);
                }
                pstmt2.setLong(1, id);
                pstmt2.setLong(2, requestWithinMs);
                ResultSet rs2 = pstmt2.executeQuery();
                if (rs2.next()) {
                    if (rs2.getLong(1) > 0) {
                        metric++;
                        if (log.isDebugEnabled()) {
                            log.debug("hit metric=" + metric);
                        }
                    }
                }
                rs2.close();
            }
            rs.close();
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (con != null) {
                con.close();
            }
        }
        if (log.isDebugEnabled()) {
            SimpleDateFormat df = new SimpleDateFormat();
            if (log.isDebugEnabled()) {
                log.debug("userId=" + userId + " " + df.format(new Date(startTime)) + "->" + df.format(new Date(endTime)) + " metric=" + metric);
            }
        }
        return metric;
    }

    /**
     *  Gets the sql attribute of the TotalResponseBandwidth object
     *
     *@return                The sql value
     *@exception  Exception  Description of the Exception
     */
    public String getSql() throws Exception {
        return "SELECT h.id" + " FROM http h" + " WHERE h.userId=?" + "  AND h.time>=?" + "  AND h.time<=?" + "  AND h.included=0";
    }
}
