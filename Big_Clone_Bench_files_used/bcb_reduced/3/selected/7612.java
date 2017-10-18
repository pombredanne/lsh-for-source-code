package de.juwimm.cms.search.xmldb;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import de.juwimm.cms.beans.foreign.CqPropertiesBeanSpring;
import de.juwimm.cms.search.vo.XmlSearchValue;

/**
 * Abstract class providing common functionality for all concrete XmlDbSpring-Implementations
 * @author <a href="mailto:carsten.schalm@juwimm.com">Carsten Schalm</a>
 * company Juwi|MacMillan Group Gmbh, Walsrode, Germany
 * @version $Id: AbstractXmlDbImpl.java 33 2009-02-20 08:48:27Z skulawik $
 */
public abstract class AbstractXmlDbImpl implements XmlDbSpring {

    private static Logger log = Logger.getLogger(AbstractXmlDbImpl.class);

    private static String xmldbDatasource = null;

    private Connection xmldbConnection = null;

    private CqPropertiesBeanSpring cqPropertiesBeanSpring;

    protected static final String DELETE_STATEMENT = "DELETE FROM XML_SEARCH_DB WHERE SITE_ID=? AND VIEW_COMPONENT_ID=?";

    protected static final String SELECT_COUNT_STATEMENT = "SELECT COUNT(SITE_ID) FROM XML_SEARCH_DB WHERE SITE_ID=? AND VIEW_COMPONENT_ID=?";

    protected static final String SELECT_COUNT_HASHCODE_STATEMENT = "SELECT COUNT(SITE_ID) FROM XML_SEARCH_DB WHERE SITE_ID=? AND VIEW_COMPONENT_ID=? AND HASHCODE=?";

    protected abstract String getInsertStatementSql();

    protected abstract PreparedStatement getSearchXmlStatement(Integer siteId, String xpathQuery) throws SQLException;

    protected abstract PreparedStatement getSearchXmlByUnitStatement(Integer unitId, Integer viewDocumentId, String xpathQuery) throws SQLException;

    public CqPropertiesBeanSpring getCqPropertiesBeanSpring() {
        return cqPropertiesBeanSpring;
    }

    @Autowired
    public void setCqPropertiesBeanSpring(CqPropertiesBeanSpring cqPropertiesBeanSpring) {
        this.cqPropertiesBeanSpring = cqPropertiesBeanSpring;
    }

    protected Connection getConnection() {
        if (log.isDebugEnabled()) log.debug("getConnection() -> begin");
        if (this.xmldbConnection == null) {
            try {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup(this.getXmldbDatasource());
                this.xmldbConnection = ds.getConnection();
            } catch (NamingException ex) {
                log.error("getConnection() -> lookup for " + this.getXmldbDatasource() + " failed: ", ex);
            } catch (SQLException ex) {
                log.error("getConnection() -> getConnection() for " + this.getXmldbDatasource() + " failed: ", ex);
            }
        }
        if (log.isDebugEnabled()) log.debug("getConnection() -> end");
        return this.xmldbConnection;
    }

    protected void releaseConnection() {
        if (log.isDebugEnabled()) log.debug("releaseConnection() -> begin");
        if (this.xmldbConnection != null) {
            try {
                this.xmldbConnection.close();
            } catch (SQLException e) {
                log.warn("releaseConnection() -> error releasing db-connection (already closed?)\n" + e.getMessage());
            } finally {
                this.xmldbConnection = null;
            }
        }
        if (log.isDebugEnabled()) log.debug("releaseConnection() -> end");
    }

    private String getXmldbDatasource() {
        return getCqPropertiesBeanSpring().getSearch().getXmlDatasource();
    }

    protected String getHashCode(String value) {
        if (log.isDebugEnabled()) log.debug("getHashCode(...) -> begin");
        String retVal = null;
        try {
            MessageDigest mdAlgorithm = MessageDigest.getInstance("MD5");
            mdAlgorithm.update(value.getBytes());
            byte[] digest = mdAlgorithm.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                sb.append(this.toHexString(digest[i]));
            }
            retVal = sb.toString();
            if (log.isDebugEnabled()) log.debug("getHashCode(...) -> hashcode = " + retVal);
        } catch (Exception e) {
            log.error("getHashCode(...) -> error occured generating hashcode ", e);
        }
        if (log.isDebugEnabled()) log.debug("getHashCode(...) -> end");
        return retVal;
    }

    private String toHexString(byte b) {
        String retVal;
        int value = (b & 0x7F) + (b < 0 ? 128 : 0);
        retVal = (value < 16 ? "0" : "");
        retVal += Integer.toHexString(value).toUpperCase();
        return retVal;
    }

    protected String clobToString(Clob clob) {
        if (log.isDebugEnabled()) log.debug("clobToString(...) -> begin");
        String retVal = "";
        if (clob != null) {
            StringBuffer sb = new StringBuffer();
            String tmp;
            try {
                BufferedReader br = new BufferedReader(clob.getCharacterStream());
                while ((tmp = br.readLine()) != null) sb.append(tmp);
            } catch (IOException ioe) {
                log.error("clobToString(...) -> error creating BufferedReader");
            } catch (SQLException sqle) {
                log.error("clobToString(...) -> error getting Clob.getCharacterStream()");
            }
            retVal = sb.toString();
        }
        if (log.isDebugEnabled()) log.debug("clobToString(...) -> end");
        return retVal;
    }

    /**
	 * Executing the search and building the XmlSearchValue[]
	 * @param pstmt
	 * @return
	 * @throws SQLException
	 */
    protected synchronized XmlSearchValue[] search(PreparedStatement pstmt) throws SQLException {
        LinkedList<XmlSearchValue> tmpList = new LinkedList<XmlSearchValue>();
        ResultSet qResult = pstmt.executeQuery();
        while (qResult.next()) {
            XmlSearchValue newEntry = new XmlSearchValue();
            newEntry.setViewComponentId(qResult.getInt(1));
            newEntry.setUnitId(qResult.getInt(2));
            newEntry.setInfoText(qResult.getString(3));
            newEntry.setText(qResult.getString(4));
            newEntry.setContent(qResult.getString(5));
            tmpList.add(newEntry);
        }
        return tmpList.toArray(new XmlSearchValue[0]);
    }

    /**
	 * 
	 * @param siteId
	 * @param viewComponentId
	 * @param contentText
	 * @param metaAttributes
	 * @return
	 */
    protected synchronized boolean doInsert(Integer siteId, Integer viewComponentId, String contentText, Map<String, String> metaAttributes, String hashcode) {
        if (log.isDebugEnabled()) log.debug("doInsert(...) -> begin");
        boolean retVal = false;
        PreparedStatement pstmt = null;
        Connection connection = null;
        String infoText = null;
        String text = null;
        int unitId = 0;
        try {
            if (contentText == null || contentText.length() == 0) {
                return true;
            }
            if (log.isDebugEnabled()) {
                log.debug("doInsert(...) -> before parsing metaAttributes");
            }
            if (!("".equals(metaAttributes.get("unitId")))) {
                unitId = Integer.parseInt(metaAttributes.get("unitId"));
            }
            infoText = metaAttributes.get("infoText");
            text = metaAttributes.get("text");
            if (log.isDebugEnabled()) {
                log.debug("doInsert(...) -> after parsing metaAttributes");
            }
            connection = this.getConnection();
            pstmt = connection.prepareStatement(this.getInsertStatementSql());
            pstmt.setInt(1, siteId.intValue());
            pstmt.setInt(2, viewComponentId.intValue());
            pstmt.setString(3, contentText);
            pstmt.setInt(4, unitId);
            pstmt.setString(5, infoText);
            pstmt.setString(6, text);
            pstmt.setString(7, hashcode);
            retVal = pstmt.execute();
            if (log.isDebugEnabled()) log.debug("doInsert(...) -> insert query executed");
        } catch (SQLException sqle) {
            log.error("doInsert(...) -> failed to excecute insert query for viewComponentId = " + viewComponentId, sqle);
            if (log.isDebugEnabled()) log.debug("Content to insert: " + contentText);
        } catch (Exception e) {
            log.error("doInsert(...) -> unkown exception occured " + e.getMessage(), e);
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                log.error("doInsert(...) -> error closing pstmt " + e.getMessage(), e);
            }
            this.releaseConnection();
        }
        if (log.isDebugEnabled()) log.debug("doInsert(...) -> end");
        return retVal;
    }

    public synchronized void deleteXml(Integer siteId, Integer viewComponentId) {
        if (log.isDebugEnabled()) log.debug("deleteXML(...) -> begin");
        PreparedStatement pstmt = null;
        try {
            pstmt = this.getConnection().prepareStatement(AbstractXmlDbImpl.DELETE_STATEMENT);
            pstmt.setInt(1, siteId.intValue());
            pstmt.setInt(2, viewComponentId.intValue());
            pstmt.executeUpdate();
            if (log.isDebugEnabled()) log.debug("deleteXML(...) -> delete query executed");
        } catch (SQLException sqle) {
            log.error("deleteXML(...) -> failed to excecute delete query " + sqle.getMessage(), sqle);
        } catch (Exception e) {
            log.error("deleteXML(...) -> unkown exception occured " + e.getMessage(), e);
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                log.error("deleteXML(...) -> error closing pstmt " + e.getMessage(), e);
            }
            this.releaseConnection();
        }
        if (log.isDebugEnabled()) log.debug("deleteXML(...) -> end");
    }

    public synchronized boolean saveXml(Integer siteId, Integer viewComponentId, String contentText, Map<String, String> metaAttributes) {
        if (log.isDebugEnabled()) log.debug("saveXml(...) -> begin");
        boolean retVal = false;
        Iterator<String> it = metaAttributes.keySet().iterator();
        String hashbase = "";
        while (it.hasNext()) {
            String key = it.next();
            hashbase += key + "=" + metaAttributes.get(key) + "\n";
        }
        hashbase += contentText;
        String hashcode = this.getHashCode(hashbase);
        PreparedStatement pstmt = null;
        try {
            pstmt = (PreparedStatement) this.getConnection().prepareStatement(AbstractXmlDbImpl.SELECT_COUNT_STATEMENT);
            pstmt.setInt(1, siteId);
            pstmt.setInt(2, viewComponentId);
            ResultSet qResult = pstmt.executeQuery();
            qResult.next();
            if (qResult.getInt(1) == 0) {
                retVal = this.doInsert(siteId, viewComponentId, contentText, metaAttributes, hashcode);
            } else {
                PreparedStatement hashPstmt = this.getConnection().prepareStatement(AbstractXmlDbImpl.SELECT_COUNT_STATEMENT);
                hashPstmt = this.getConnection().prepareStatement(AbstractXmlDbImpl.SELECT_COUNT_HASHCODE_STATEMENT);
                hashPstmt.setInt(1, siteId);
                hashPstmt.setInt(2, viewComponentId);
                hashPstmt.setString(3, hashcode);
                ResultSet hashResult = hashPstmt.executeQuery();
                hashResult.next();
                if (hashResult.getInt(1) == 0) {
                    this.deleteXml(siteId, viewComponentId);
                    retVal = this.doInsert(siteId, viewComponentId, contentText, metaAttributes, hashcode);
                } else {
                    retVal = true;
                    if (log.isDebugEnabled()) log.debug("saveXml(...) -> nothing to do, hashcodes are equal (" + hashcode + ")");
                }
                hashResult.close();
                if (hashPstmt != null) hashPstmt.close();
            }
            qResult.close();
        } catch (SQLException e) {
            log.error("saveXml(...) -> error getting row count ", e);
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                log.error("saveXml(...) -> error closing pstmt ", e);
            }
            this.releaseConnection();
        }
        if (log.isDebugEnabled()) log.debug("saveXml(...) -> end");
        return retVal;
    }

    public synchronized XmlSearchValue[] searchXml(Integer siteId, String xpathQuery) {
        if (log.isDebugEnabled()) log.debug("searchXML(...) -> begin at " + sdf.format(new java.util.Date()));
        XmlSearchValue[] retArray = null;
        PreparedStatement pstmt = null;
        try {
            pstmt = this.getSearchXmlStatement(siteId, xpathQuery);
            retArray = this.search(pstmt);
        } catch (SQLException sqle) {
            log.error("searchXML(...) -> failed to excecute query for xpathQuery = \"" + xpathQuery + "\": ", sqle);
        } catch (Exception e) {
            log.error("searchXML(...) -> unkown exception occured ", e);
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                log.error("searchXML(...) -> error closing pstmt ", e);
            }
            this.releaseConnection();
        }
        if (log.isDebugEnabled()) log.debug("searchXML(...) -> end at " + sdf.format(new java.util.Date()));
        return retArray;
    }

    /** @see de.juwimm.cms.search.xmldb.XmlDbSpring#searchXmlByUnit(java.lang.Integer, java.lang.String) */
    public synchronized XmlSearchValue[] searchXmlByUnit(Integer unitId, Integer viewDocumentId, String xpathQuery) {
        if (log.isDebugEnabled()) log.debug("searchXmlByUnit(...) -> begin at " + sdf.format(new java.util.Date()));
        XmlSearchValue[] retArray = null;
        PreparedStatement pstmt = null;
        try {
            pstmt = this.getSearchXmlByUnitStatement(unitId, viewDocumentId, xpathQuery);
            retArray = this.search(pstmt);
        } catch (SQLException sqle) {
            log.error("searchXmlByUnit(...) -> failed to excecute query for xpathQuery = \"" + xpathQuery + "\": " + sqle.getMessage(), sqle);
        } catch (Exception e) {
            log.error("searchXmlByUnit(...) -> unknown exception occured " + e.getMessage(), e);
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                log.error("searchXmlByUnit(...) -> error closing pstmt " + e.getMessage(), e);
            }
            this.releaseConnection();
        }
        if (log.isDebugEnabled()) log.debug("searchXmlByUnit(...) -> end at " + sdf.format(new java.util.Date()));
        return retArray;
    }
}
