package net.sf.traser.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.sf.traser.utils.NamespaceRemovingFilterXMLStreamWriter;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axis2.util.XMLUtils;
import org.w3c.dom.Element;

/**
 *
 * @author Marcell Szathmári
 */
public class StorageUtils {

    static void setBlob(PreparedStatement stmt, int i, byte[] value) throws SQLException {
        stmt.setBinaryStream(i, new ByteArrayInputStream(value), value.length);
    }

    static void setBlob(PreparedStatement stmt, int i, OMElement value) throws XMLStreamException, SQLException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(4096);
        value.serializeAndConsume(new NamespaceRemovingFilterXMLStreamWriter(StAXUtils.createXMLStreamWriter(os), true));
        byte[] blob = os.toByteArray();
        int length = blob.length;
        stmt.setBinaryStream(i, new ByteArrayInputStream(os.toByteArray()), length);
    }

    /**
     * Utility class should not be instantiated.
     */
    private StorageUtils() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    /**
     * 
     * @param stmt
     */
    public static final void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException _) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "Could not close statement", _);
            }
        }
    }

    /**
     * 
     * @param conn
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException _) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "Could not close connection", _);
            }
        }
    }

    /**
     * 
     * @param rs
     */
    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException _) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "Could not close result set", _);
            }
        }
    }

    /**
     * 
     * @param el
     * @return
     * @throws javax.xml.stream.XMLStreamException 
     */
    static InputStream getInputStream(OMElement el) throws XMLStreamException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(4096);
        el.serializeAndConsume(new NamespaceRemovingFilterXMLStreamWriter(StAXUtils.createXMLStreamWriter(os), true));
        return new ByteArrayInputStream(os.toByteArray());
    }

    private static OMElement getOMElement(InputStream is) throws XMLStreamException {
        return new StAXOMBuilder(is).getDocumentElement();
    }

    static OMElement getOMElement(ResultSet rs, int i) throws XMLStreamException, SQLException {
        return getOMElement(rs.getBinaryStream(i));
    }

    static OMElement getOMElement(ResultSet rs, String c) throws XMLStreamException, SQLException {
        return getOMElement(rs.getBinaryStream(c));
    }

    /**
     * Compares two XML nodes for equality.
     * @param value
     * @param el
     * @return
     * @throws XMLStreamException
     */
    public static boolean compareXML(OMElement value, OMElement el) throws XMLStreamException {
        return value.toString().equals(el.toString());
    }

    /**
     * Computes a hash of the XML node.
     * @param value
     * @return
     * @throws XMLStreamException
     * @throws NoSuchAlgorithmException 
     */
    public static long computeXMLHash(OMElement value) throws XMLStreamException, NoSuchAlgorithmException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(4096);
        value.serializeAndConsume(new NamespaceRemovingFilterXMLStreamWriter(StAXUtils.createXMLStreamWriter(os), true));
        return computeHash(os.toByteArray());
    }

    public static final long computeHash(byte[] bytes) throws XMLStreamException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(bytes);
        BigInteger number = new BigInteger(1, messageDigest);
        return number.longValue();
    }
}
