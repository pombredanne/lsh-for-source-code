package net.sf.dsorapart;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import oracle.jdbc.driver.OracleResultSet;
import oracle.jdbc.pool.OracleDataSource;
import oracle.sql.BLOB;
import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerBinaryValue;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerSearchResult;
import org.apache.directory.server.core.entry.ServerStringValue;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.AbstractPartition;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.thoughtworks.xstream.XStream;
import eforce.util.config.BindVariable;
import eforce.util.config.Config;
import eforce.util.config.EntityConfig;
import eforce.util.config.jdbc.DBConnection;
import eforce.util.config.jdbc.DBFetchableData;
import eforce.util.config.jdbc.DBStatement;

public class OraclePartition extends AbstractPartition {

    public static final Logger log = LoggerFactory.getLogger(OraclePartition.class);

    private static final ThreadLocal<DBConnection> connection = new ThreadLocal<DBConnection>();

    private static OracleDataSource ds;

    private static XStream xstream;

    private EntityConfig pconfig;

    private Config config;

    private String id;

    private String suffix;

    private ServerEntry contextEntry;

    private Registries registries;

    public static synchronized DBConnection getConnection() throws Exception {
        DBConnection conn = connection.get();
        if (conn == null || conn.getConnection().isClosed()) {
            conn = new DBConnection(ds.getConnection());
            conn.getConnection().setAutoCommit(false);
            connection.set(conn);
        }
        return conn;
    }

    public void sync() throws NamingException {
    }

    private static void configureDS(EntityConfig dbconfig, OracleDataSource ds) throws Exception {
        ds.setUser(dbconfig.getParameter("user"));
        ds.setPassword(dbconfig.getParameter("password"));
        ds.setDriverType("thin");
        ds.setPortNumber(dbconfig.getIntParameter("port"));
        ds.setServerName(dbconfig.getParameter("host"));
        ds.setDatabaseName(dbconfig.getParameter("sid"));
        ds.setImplicitCachingEnabled(dbconfig.getBooleanParameter("implicit-caching"));
        ds.setExplicitCachingEnabled(dbconfig.getBooleanParameter("explicit-caching"));
        ds.setConnectionCachingEnabled(false);
        ds.setConnectionProperties(dbconfig.getPropertiesParameter("connection-properties"));
    }

    public EntityConfig getConfig() {
        return pconfig;
    }

    public OracleDataSource getDataSource() {
        return ds;
    }

    public OraclePartition(Registries registries, Config config) {
        this.registries = registries;
        this.config = config;
    }

    protected void doDestroy() {
        try {
            ds.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void configureXStream(XStream xstream) {
        xstream.registerConverter(new FilterConverter(registries));
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("OrNode", org.apache.directory.shared.ldap.filter.OrNode.class);
        xstream.alias("AndNode", org.apache.directory.shared.ldap.filter.AndNode.class);
        xstream.alias("NotNode", org.apache.directory.shared.ldap.filter.NotNode.class);
        xstream.alias("AbstractExprNode", org.apache.directory.shared.ldap.filter.AbstractExprNode.class);
        xstream.alias("ApproximateNode", org.apache.directory.shared.ldap.filter.ApproximateNode.class);
        xstream.alias("AssertionNode", org.apache.directory.shared.ldap.filter.AssertionNode.class);
        xstream.alias("EqualityNode", org.apache.directory.shared.ldap.filter.EqualityNode.class);
        xstream.alias("ExtensibleNode", org.apache.directory.shared.ldap.filter.ExtensibleNode.class);
        xstream.alias("GreaterEqNode", org.apache.directory.shared.ldap.filter.GreaterEqNode.class);
        xstream.alias("LessEqNode", org.apache.directory.shared.ldap.filter.LessEqNode.class);
        xstream.alias("SubstringNode", org.apache.directory.shared.ldap.filter.SubstringNode.class);
        xstream.alias("PresenceNode", org.apache.directory.shared.ldap.filter.PresenceNode.class);
        xstream.alias("ScopeNode", org.apache.directory.shared.ldap.filter.ScopeNode.class);
    }

    protected synchronized void doInit() {
        log.debug("doInit()");
        try {
            pconfig = config.getEntity("partition");
            ds = new OracleDataSource();
            EntityConfig dbConfig = config.getEntity("db");
            configureDS(dbConfig, ds);
            xstream = new XStream();
            configureXStream(xstream);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public static void addAttribute(DBFetchableData data, AttributeType attribute, DefaultServerEntry e, Registries registries) throws Exception {
        byte[] bvalue = data.getBytes("bvalue");
        if (bvalue != null) {
            e.add(attribute, new ServerBinaryValue(attribute, bvalue));
        } else {
            e.add(attribute, new ServerStringValue(attribute, data.getString("value")));
        }
    }

    public static void addAttribute(DBFetchableData data, DefaultServerEntry e, Registries registries) throws Exception {
        AttributeType attribute = registries.getAttributeTypeRegistry().lookup(data.getString("name"));
        addAttribute(data, attribute, e, registries);
    }

    public static String atos(String[] a, boolean search) {
        if (a == null) return (search ? "+,*" : "");
        if (a.length == 0) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < a.length; i++) {
            sb.append(",");
            sb.append(a[i].toLowerCase());
        }
        return sb.toString().substring(1);
    }

    public static LdapDN normDn(Registries registries, LdapDN dn) throws Exception {
        StringBuffer ret = new StringBuffer();
        for (Rdn rdn : dn.getRdns()) {
            ret.append(",");
            ret.append(registries.getAttributeTypeRegistry().lookup(rdn.getNormType()).getName());
            ret.append("=");
            ret.append(rdn.getNormValue());
        }
        return new LdapDN(ret.toString().substring(1));
    }

    public static String normAtt(Registries registries, String att) throws Exception {
        return registries.getAttributeTypeRegistry().lookup(att).getName().toLowerCase();
    }

    public static String[] normAttArray(Registries registries, String[] atts) throws Exception {
        if (atts == null) return null;
        int rm = 0;
        for (int i = 0; i < atts.length; i++) if (atts[i].equals("1.1")) rm++;
        String[] ret = new String[atts.length - rm];
        for (int i = 0; i < ret.length; i++) if (atts[i].equals("+") || atts[i].equals("*")) ret[i] = atts[i]; else if (atts[i].equals("1.1")) continue; else ret[i] = registries.getAttributeTypeRegistry().lookup(atts[i]).getName();
        return ret;
    }

    public static String r(String in) {
        StringBuffer sb = new StringBuffer();
        String[] pieces = in.split(",");
        for (int i = pieces.length - 1; i > -1; i--) sb.append("," + pieces[i]);
        return sb.toString().substring(1);
    }

    public static LdapDN getLdapDN(DBFetchableData data) throws Exception {
        return new LdapDN(data.getString("rdn") + "," + r(data.getString("parentdn")));
    }

    public static DBStatement bindDNVariables(DBStatement stmt, LdapDN dn) throws Exception {
        String revdn = r(dn.toString());
        int l = revdn.lastIndexOf(",");
        stmt.bindVariable("rdn", dn.getRdn().toString());
        stmt.bindVariable("parentdn", revdn.substring(0, (l == -1 ? 0 : l)) + ",");
        return stmt;
    }

    public static String hash(byte[] val) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        DigestInputStream digestIn = new DigestInputStream(new ByteArrayInputStream(val), md);
        while (digestIn.read() != -1) ;
        byte[] digest = md.digest();
        return "{SHA}" + new String(Base64.encode(digest));
    }

    private static void runTransaction(RunnableTransaction t) {
        t.run();
    }

    private void insertAttribute(DBConnection conn, String entryid, DefaultServerAttribute attribute) throws Exception {
        DBStatement dml = new DBStatement(getConfig().getSQLStatement("INSERT_ATTRIBUTE"));
        for (Value value : attribute) {
            String type = (attribute.getAttributeType().getUsage().equals(UsageEnum.USER_APPLICATIONS) ? "u" : null);
            if (value instanceof ServerBinaryValue) {
                byte[] val = ((ServerBinaryValue) value).get();
                String hash = hash(val);
                String bvalueid = conn.getData(new DBStatement(getConfig().getSQLStatement("GET_BINARY_VALUE_ID"), new BindVariable[] { new BindVariable("hash", hash) })).getFirst();
                if (bvalueid.startsWith("-")) {
                    bvalueid = bvalueid.substring(1);
                    DBStatement bdml = new DBStatement(getConfig().getSQLStatement("INSERT_BINARY_VALUE"));
                    bdml.bindVariable("bvalueid", bvalueid);
                    bdml.bindVariable("hash", hash);
                    conn.executeDML(bdml);
                    PreparedStatement pst = conn.getConnection().prepareStatement(getConfig().getSQLstmt("UPDATE_BINARY_VALUE"));
                    pst.setString(1, bvalueid);
                    OracleResultSet rs = (OracleResultSet) pst.executeQuery();
                    rs.next();
                    BLOB b = rs.getBLOB(1);
                    b.open(BLOB.MODE_READWRITE);
                    OutputStream blob_os = b.setBinaryStream(0);
                    blob_os.write(val);
                    blob_os.flush();
                    b.close();
                    pst.close();
                }
                dml.bindVariable("entryid", entryid);
                dml.bindVariable("name", attribute.getId());
                dml.bindVariable("value", null);
                dml.bindVariable("type", type);
                dml.bindVariable("bvalueid", bvalueid);
                conn.executeDML(dml);
                dml.clearParameters();
            } else {
                dml.bindVariable("entryid", entryid);
                dml.bindVariable("name", attribute.getId());
                dml.bindVariable("value", value.toString());
                dml.bindVariable("type", type);
                dml.bindVariable("bvalueid", null);
                conn.executeDML(dml);
                dml.clearParameters();
            }
        }
    }

    private void removeAttribute(DBConnection conn, String entryid, DefaultServerAttribute attribute) throws Exception {
        DBStatement dml = new DBStatement(getConfig().getSQLStatement("DELETE_ATTRIBUTE_VALUE"));
        for (Value value : attribute) if (value instanceof ServerBinaryValue) {
            DBStatement bdml = new DBStatement(getConfig().getSQLStatement("DELETE_BINARY_ATTRIBUTE_VALUE"));
            bdml.bindVariable("entryid", entryid);
            bdml.bindVariable("name", attribute.getId());
            bdml.bindVariable("hash", hash(((ServerBinaryValue) value).get()));
            conn.executeDML(bdml);
            bdml = new DBStatement(getConfig().getSQLStatement("DELETE_BINARY_VALUE"));
            bdml.bindVariable("entryid", entryid);
            bdml.bindVariable("name", attribute.getId());
            bdml.bindVariable("hash", hash(((ServerBinaryValue) value).get()));
            conn.executeDML(bdml);
        } else {
            dml.bindVariable("entryid", entryid);
            dml.bindVariable("name", attribute.getId());
            dml.bindVariable("value", value.toString());
            conn.executeDML(dml);
            dml.clearParameters();
        }
    }

    private void replaceAttribute(DBConnection conn, String entryid, DefaultServerAttribute attribute) throws Exception {
        DBStatement dml = new DBStatement(getConfig().getSQLStatement("DELETE_ATTRIBUTE"));
        dml.bindVariable("entryid", entryid);
        dml.bindVariable("name", attribute.getId());
        conn.executeDML(dml);
        insertAttribute(conn, entryid, attribute);
    }

    private String getEntryId(DBConnection conn, Registries registries, LdapDN dn) throws Exception {
        String id = "";
        try {
            id = conn.getData(bindDNVariables(new DBStatement(getConfig().getSQLStatement("GET_DN_ID")), normDn(registries, dn))).getFirst();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        if (id.equals("")) throw new NamingException("Unknown dn: " + dn);
        return id;
    }

    public void add(final AddOperationContext ctx) throws NamingException {
        runTransaction(new RunnableTransaction() {

            public void runInTransaction(DBConnection conn) throws Throwable {
                String entryid = String.valueOf(conn.getLongNextVal("SEQ_ENTRYID"));
                DBStatement dml = bindDNVariables(new DBStatement(getConfig().getSQLStatement("INSERT_DN")), normDn(ctx.getRegistries(), ctx.getDn()));
                dml.bindVariable("entryid", entryid);
                conn.executeDML(dml);
                for (AttributeType attributeType : ctx.getEntry().getAttributeTypes()) insertAttribute(conn, entryid, (DefaultServerAttribute) ctx.getEntry().get(attributeType));
            }
        });
    }

    public void bind(BindOperationContext ctx) throws NamingException {
        try {
            if (getEntryId(getConnection(), ctx.getRegistries(), ctx.getDn()) == null) throw new NamingException("invalid dn");
        } catch (NamingException ne) {
            throw ne;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public void delete(final DeleteOperationContext ctx) throws NamingException {
        runTransaction(new RunnableTransaction() {

            public void runInTransaction(DBConnection conn) throws Throwable {
                conn.executeDML(bindDNVariables(new DBStatement(getConfig().getSQLStatement("DELETE_DN")), normDn(ctx.getRegistries(), ctx.getDn())));
            }
        });
    }

    public int getCacheSize() {
        return 0;
    }

    public ServerEntry getContextEntry() {
        return contextEntry;
    }

    public String getId() {
        return id;
    }

    public String getSuffix() {
        return suffix;
    }

    public LdapDN getSuffixDn() throws NamingException {
        return getContextEntry().getDn();
    }

    public LdapDN getUpSuffixDn() throws NamingException {
        return new LdapDN(getContextEntry().getDn().getUpName());
    }

    public NamingEnumeration<ServerSearchResult> list(final ListOperationContext ctx) throws NamingException {
        try {
            return new NamingEnumeration<ServerSearchResult>() {

                private DBConnection conn = getConnection();

                private DBFetchableData data;

                public void close() throws NamingException {
                    data.release();
                }

                public boolean hasMore() throws NamingException {
                    return hasMoreElements();
                }

                public ServerSearchResult next() throws NamingException {
                    return nextElement();
                }

                public boolean hasMoreElements() {
                    boolean retval = false;
                    try {
                        if (data == null) data = conn.getFetchableData(new DBStatement(getConfig().getSQLStatement("LIST_DN"), new BindVariable[] { new BindVariable("parentdn", r(normDn(ctx.getRegistries(), ctx.getDn()).toString()) + ",") }));
                        retval = data.next();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                    return retval;
                }

                public ServerSearchResult nextElement() {
                    try {
                        LdapDN dn = getLdapDN(data);
                        return new ServerSearchResult(dn, null, new DefaultServerEntry(registries, dn));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                }
            };
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public void modify(final ModifyOperationContext ctx) throws NamingException {
        runTransaction(new RunnableTransaction() {

            public void runInTransaction(DBConnection conn) throws Throwable {
                String entryid = getEntryId(conn, ctx.getRegistries(), normDn(ctx.getRegistries(), ctx.getDn()));
                for (Modification mod : ctx.getModItems()) {
                    switch(mod.getOperation()) {
                        case ADD_ATTRIBUTE:
                            insertAttribute(conn, entryid, (DefaultServerAttribute) mod.getAttribute());
                            break;
                        case REMOVE_ATTRIBUTE:
                            removeAttribute(conn, entryid, (DefaultServerAttribute) mod.getAttribute());
                            break;
                        case REPLACE_ATTRIBUTE:
                            replaceAttribute(conn, entryid, (DefaultServerAttribute) mod.getAttribute());
                            break;
                    }
                }
            }
        });
    }

    public void move(MoveOperationContext ctx) throws NamingException {
        try {
            DBConnection conn = getConnection();
            DBStatement dml = bindDNVariables(new DBStatement(getConfig().getSQLStatement("MOVE_DN")), normDn(ctx.getRegistries(), ctx.getDn()));
            dml.bindVariable("newparent", r(normDn(ctx.getRegistries(), ctx.getParent()).toString()));
            conn.executeDML(dml);
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public void moveAndRename(MoveAndRenameOperationContext ctx) throws NamingException {
        try {
            DBConnection conn = getConnection();
            DBStatement dml = bindDNVariables(new DBStatement(getConfig().getSQLStatement("MOVEANDRENAME_DN")), normDn(ctx.getRegistries(), ctx.getDn()));
            dml.bindVariable("newparent", r(normDn(ctx.getRegistries(), ctx.getParent()).toString()));
            dml.bindVariable("newrdn", ctx.getNewRdn().toString());
            conn.executeDML(dml);
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public void rename(RenameOperationContext ctx) throws NamingException {
        try {
            DBConnection conn = getConnection();
            DBStatement dml = bindDNVariables(new DBStatement(getConfig().getSQLStatement("RENAME_DN")), normDn(ctx.getRegistries(), ctx.getDn()));
            dml.bindVariable("newrdn", ctx.getNewRdn().toString());
            conn.executeDML(dml);
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public NamingEnumeration<ServerSearchResult> search(final SearchOperationContext ctx) throws NamingException {
        log.debug("search <---------------------------");
        try {
            return new NamingEnumeration<ServerSearchResult>() {

                private QueryBuilder qb = new QueryBuilder(getConnection(), ctx, xstream, pconfig);

                public void close() throws NamingException {
                    qb.close();
                }

                public boolean hasMore() throws NamingException {
                    return hasMoreElements();
                }

                public ServerSearchResult next() throws NamingException {
                    return nextElement();
                }

                public boolean hasMoreElements() {
                    return qb.hasNext();
                }

                public ServerSearchResult nextElement() {
                    return qb.next();
                }
            };
        } catch (NamingException ne) {
            throw ne;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public boolean hasEntry(EntryOperationContext ctx) throws NamingException {
        try {
            DBConnection conn = getConnection();
            LdapDN dn = normDn(ctx.getRegistries(), ctx.getDn());
            log.debug("hasEntry() dn: " + dn);
            return conn.getData(bindDNVariables(new DBStatement(getConfig().getSQLStatement("GET_DN_ID")), dn)).getFirst() != null;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public ServerEntry lookup(LookupOperationContext ctx) throws NamingException {
        log.debug("lookup <---------------------------");
        DefaultServerEntry e = null;
        try {
            DBConnection conn = getConnection();
            LdapDN dn = normDn(ctx.getRegistries(), ctx.getDn());
            String atidsStr = atos(normAttArray(ctx.getRegistries(), ctx.getAttrsIdArray()), false);
            log.debug("lookup() dn: " + dn + " atids " + atidsStr);
            String entryId = conn.getData(bindDNVariables(new DBStatement(getConfig().getSQLStatement("GET_DN_ID")), dn)).getFirst();
            if (entryId != null) {
                e = new DefaultServerEntry(ctx.getRegistries(), dn);
                String[] atids = ctx.getAttrsIdArray();
                DBStatement query;
                if (atids.length == 0) query = new DBStatement(getConfig().getSQLStatement("LOOKUP_ALL_ATTRIBUTES"), new BindVariable[] { new BindVariable("entryid", entryId) }); else if (atids.length == 1) query = new DBStatement(getConfig().getSQLStatement("LOOKUP_ATTRIBUTE"), new BindVariable[] { new BindVariable("entryid", entryId), new BindVariable("name", normAtt(ctx.getRegistries(), atids[0])) }); else query = new DBStatement(getConfig().getSQLStatement("LOOKUP_ATTRIBUTES"), new BindVariable[] { new BindVariable("entryid", entryId), new BindVariable("names", atidsStr) });
                DBFetchableData data = conn.getFetchableData(query);
                while (data.next()) addAttribute(data, e, ctx.getRegistries());
                data.release();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return e;
    }

    public void setCacheSize(int val) {
    }

    public void setContextEntry(ServerEntry val) {
        contextEntry = val;
    }

    public void setId(String val) {
        id = val;
    }

    public void setSuffix(String val) {
        suffix = val;
    }

    public void unbind(UnbindOperationContext ctx) throws NamingException {
    }

    abstract class RunnableTransaction {

        public abstract void runInTransaction(DBConnection conn) throws Throwable;

        public void run() {
            try {
                DBConnection conn = getConnection();
                try {
                    runInTransaction(conn);
                    conn.commit();
                } catch (Throwable e) {
                    conn.rollback();
                    throw new RuntimeException(e);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
    }
}
