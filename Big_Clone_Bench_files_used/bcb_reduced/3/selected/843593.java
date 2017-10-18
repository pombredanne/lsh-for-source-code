package org.dllearner.autosparql.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.SparqlEndpoint;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * The class is used to cache information about resources to a database.
 * Provides the connection to an H2 database in a light weight, configuration free
 * manner. 
 * 
 * Note: Currently, either select ot construct has to be used (not both).
 * 
 * @author Jens Lehmann
 *
 */
public class ExtractionDBCache {

    private String databaseDirectory = "cache";

    private String databaseName = "extraction";

    private boolean autoServerMode = true;

    private long freshnessSeconds = 15 * 24 * 60 * 60;

    private Connection conn;

    MessageDigest md5;

    private Logger logger = Logger.getLogger(ExtractionDBCache.class);

    public ExtractionDBCache(String cacheDir) {
        databaseDirectory = cacheDir;
        try {
            md5 = MessageDigest.getInstance("MD5");
            Class.forName("org.h2.Driver");
            String jdbcString = "";
            if (autoServerMode) {
                jdbcString = ";AUTO_SERVER=TRUE";
            }
            conn = DriverManager.getConnection("jdbc:h2:" + databaseDirectory + "/" + databaseName + jdbcString, "sa", "");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS QUERY_CACHE(QUERYHASH BINARY PRIMARY KEY,QUERY VARCHAR(20000), TRIPLES CLOB, STORE_TIME TIMESTAMP)");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Model executeConstructQuery(SparqlEndpoint endpoint, String query) throws SQLException, UnsupportedEncodingException {
        byte[] md5 = md5(query);
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM QUERY_CACHE WHERE QUERYHASH=? LIMIT 1");
        ps.setBytes(1, md5);
        ResultSet rs = ps.executeQuery();
        boolean readFromCache = rs.next() && (rs.getTimestamp("STORE_TIME").getTime() - System.currentTimeMillis() < freshnessSeconds);
        if (readFromCache) {
            Clob clob = rs.getClob("TRIPLES");
            Model readModel = ModelFactory.createDefaultModel();
            readModel.read(clob.getAsciiStream(), null, "N-TRIPLE");
            return readModel;
        } else {
            logger.info("Sending CONTRUCT query...");
            logger.info("Query:\n" + query);
            QueryEngineHTTP queryExecution = new QueryEngineHTTP(endpoint.getURL().toString(), query);
            for (String dgu : endpoint.getDefaultGraphURIs()) {
                queryExecution.addDefaultGraph(dgu);
            }
            for (String ngu : endpoint.getNamedGraphURIs()) {
                queryExecution.addNamedGraph(ngu);
            }
            logger.info("Link:\n" + queryExecution);
            Model m2 = queryExecution.execConstruct();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m2.write(baos, "N-TRIPLE");
            String modelStr = baos.toString("UTF-8");
            PreparedStatement ps2 = conn.prepareStatement("INSERT INTO QUERY_CACHE VALUES(?,?,?,?)");
            ps2.setBytes(1, md5);
            ps2.setString(2, query);
            ps2.setClob(3, new StringReader(modelStr));
            ps2.setTimestamp(4, new java.sql.Timestamp(new java.util.Date().getTime()));
            ps2.executeUpdate();
            return m2;
        }
    }

    public String executeSelectQuery(SparqlEndpoint endpoint, String query) {
        try {
            byte[] md5 = md5(query);
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM QUERY_CACHE WHERE QUERYHASH=? LIMIT 1");
            ps.setBytes(1, md5);
            ResultSet rs = ps.executeQuery();
            boolean readFromCache = rs.next() && (rs.getTimestamp("STORE_TIME").getTime() - System.currentTimeMillis() < freshnessSeconds);
            if (readFromCache) {
                Clob clob = rs.getClob("TRIPLES");
                return clob.getSubString(1, (int) clob.length());
            } else {
                logger.info("Sending SELECT query...");
                logger.info("Query:\n" + query);
                QueryEngineHTTP queryExecution = new QueryEngineHTTP(endpoint.getURL().toString(), query);
                for (String dgu : endpoint.getDefaultGraphURIs()) {
                    queryExecution.addDefaultGraph(dgu);
                }
                for (String ngu : endpoint.getNamedGraphURIs()) {
                    queryExecution.addNamedGraph(ngu);
                }
                logger.info("Link:\n" + queryExecution);
                com.hp.hpl.jena.query.ResultSet tmp = queryExecution.execSelect();
                ResultSetRewindable rs2 = ResultSetFactory.makeRewindable(tmp);
                String json = convertResultSetToJSON(rs2);
                PreparedStatement ps2 = conn.prepareStatement("INSERT INTO QUERY_CACHE VALUES(?,?,?,?)");
                ps2.setBytes(1, md5);
                ps2.setString(2, query);
                ps2.setClob(3, new StringReader(json));
                ps2.setTimestamp(4, new java.sql.Timestamp(new java.util.Date().getTime()));
                ps2.executeUpdate();
                return json;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void closeConnection() throws SQLException {
        conn.close();
    }

    private byte[] md5(String string) {
        md5.reset();
        md5.update(string.getBytes());
        return md5.digest();
    }

    public static String toNTriple(Model m) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.write(baos, "N-TRIPLE");
        try {
            return baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Converts Jena result set to JSON.
	 * 
	 * @param resultSet
	 *            The result set to transform, must be rewindable to prevent
	 *            errors.
	 * @return JSON representation of the result set.
	 */
    public static String convertResultSetToJSON(ResultSetRewindable resultSet) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(baos, resultSet);
        resultSet.reset();
        try {
            return baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    /**
	 * Converts from JSON to internal Jena format.
	 * 
	 * @param json
	 *            A JSON representation if a SPARQL query result.
	 * @return A Jena ResultSet.
	 */
    public static ResultSetRewindable convertJSONtoResultSet(String json) {
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(Charset.forName("UTF-8")));
        return ResultSetFactory.makeRewindable(ResultSetFactory.fromJSON(bais));
    }
}
