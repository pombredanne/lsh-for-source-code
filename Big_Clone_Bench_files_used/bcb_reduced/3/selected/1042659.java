package de.juwimm.cms.beans.jmx;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import de.juwimm.util.Base64;

/**
 * The DatabaseUserManager manages password changes and other functions not supported by JAAS
 * for a SQL userdata source. 
 * @author <a href="sascha.kulawik@juwimm.com">Sascha-Matthias Kulawik</a>
 * @version $Id: DatabaseUserManager.java 16 2009-02-17 06:08:37Z skulawik $
 */
@ManagedResource
public class DatabaseUserManager implements UserManager {

    private static Logger log = Logger.getLogger(DatabaseUserManager.class);

    private String datasource = "java:/MySqlDS";

    private String hashAlgorithm = "SHA-1";

    private String hashEncoding = "base64";

    private String atSqlGetUser = "SELECT user_id, passwd FROM usr WHERE user_id = ?";

    private String atSqlUpdateUser = "UPDATE usr SET passwd = ? WHERE user_id = ?";

    @ManagedOperation(description = "Change password")
    public boolean changePassword(String userName, String oldPassword, String newPassword) {
        if (log.isDebugEnabled()) log.debug("begin changePassword mbean");
        Connection conn = null;
        boolean valid = false;
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup(datasource);
            conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(atSqlGetUser);
            ps.setString(1, userName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                byte[] hash = MessageDigest.getInstance(hashAlgorithm).digest(newPassword.getBytes());
                String newPasswd = Base64.encodeBytes(hash);
                ps.close();
                PreparedStatement psu = conn.prepareStatement(atSqlUpdateUser);
                psu.setString(2, userName);
                psu.setString(1, newPasswd);
                psu.execute();
                psu.close();
            }
            rs.close();
        } catch (Exception exe) {
            log.error("Error occured", exe);
        } finally {
            try {
                conn.close();
            } catch (Exception exe) {
            }
        }
        if (log.isDebugEnabled()) log.debug("end changePassword mbean");
        return valid;
    }

    private static String encryptSHA1URL(String x) throws Exception {
        java.security.MessageDigest d = null;
        d = java.security.MessageDigest.getInstance("SHA-1");
        d.reset();
        d.update(x.getBytes());
        String passwd = "";
        passwd = URLEncoder.encode(new String(d.digest()), "ISO-8859-1");
        return passwd;
    }

    public AttributeList getAttributes(String[] parm1) {
        AttributeList al = new AttributeList(7);
        al.add(new Attribute("datasource", datasource));
        al.add(new Attribute("hashAlgorithm", hashAlgorithm));
        al.add(new Attribute("hashEncoding", hashEncoding));
        return al;
    }

    @ManagedAttribute
    public final String getDatasource() {
        return this.datasource;
    }

    @ManagedAttribute
    public final void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    @ManagedAttribute
    public final String getHashAlgorithm() {
        return this.hashAlgorithm;
    }

    @ManagedAttribute
    public final void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    @ManagedAttribute
    public final String getHashEncoding() {
        return this.hashEncoding;
    }

    @ManagedAttribute
    public final void setHashEncoding(String hashEncoding) {
        this.hashEncoding = hashEncoding;
    }
}
