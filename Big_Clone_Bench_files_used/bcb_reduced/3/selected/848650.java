package de.ibk.ods.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.asam.ods.AoException;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValueUnit;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;
import de.ibk.ods.core.sql.ArrayHandler;
import de.ibk.ods.core.sql.ArrayListHandler;
import de.ibk.ods.core.sql.SqlHelper;

/**
 * @author Reinhard Kessler, Ingenieurbüro Kessler
 * @version 5.0.0
 */
public class InstElem {

    /**
	 * 
	 */
    private Logger log = LogManager.getLogger("de.ibk.ods.openaos");

    /**
	 * 
	 */
    private ApplElem applElem;

    /**
	 * @return Returns the applElem.
	 */
    public ApplElem getApplElem() {
        return applElem;
    }

    /**
	 * @param applElem The applElem to set.
	 */
    public void setApplElem(ApplElem applElem) {
        this.applElem = applElem;
    }

    /**
	 * 
	 */
    private SvcVal svcVal;

    /**
	 * @return Returns the svcVal.
	 */
    public SvcVal getSvcVal() {
        if (svcVal == null) {
            svcVal = new SvcVal(this);
        }
        return svcVal;
    }

    /**
	 * 
	 */
    private Kernel kernel;

    /**
	 * 
	 */
    private String name = null;

    /**
	 * 
	 */
    private long iid;

    /**
	 * @return Returns the iid.
	 */
    public long getIid() {
        return iid;
    }

    /**
	 * @param iid The iid to set.
	 */
    public void setIid(long iid) {
        this.iid = iid;
    }

    /**
	 * @param iid
	 */
    public InstElem(Kernel kernel, ApplElem applElem, long iid) {
        super();
        this.kernel = kernel;
        this.applElem = applElem;
        this.iid = iid;
    }

    /**
	 * 
	 * @return
	 * @throws AoException
	 */
    public String getName() throws AoException {
        if (this.name == null) {
            String dbtName = this.applElem.getSvcent().getDbtname();
            String idColName = this.applElem.getDbcnameByBaname("id");
            String nameColName = this.applElem.getDbcnameByBaname("name");
            try {
                String sql = SqlHelper.format("select %s from %s where %s=%s", new String[] { nameColName, dbtName, idColName, Long.toString(iid) });
                java.lang.Object[] result = (java.lang.Object[]) kernel.getQueryHandler().executeQuery(sql, new de.ibk.ods.core.sql.ArrayHandler());
                if (result != null) {
                    this.name = String.valueOf(result[0]).trim();
                }
            } catch (SQLException e) {
                log.fatal(e.getMessage());
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "InstElem::getName()");
            }
        }
        return this.name;
    }

    /**
	 * 
	 * @param ieName
	 * @throws AoException
	 */
    public void setName(String ieName) throws AoException {
        String dbtName = this.applElem.getSvcent().getDbtname();
        String idColName = this.applElem.getDbcnameByBaname("id");
        String nameColName = this.applElem.getDbcnameByBaname("name");
        String sql = SqlHelper.format("update %s set %s='%s' where %s=%s", new String[] { dbtName, nameColName, ieName, idColName, Long.toString(iid) });
        try {
            kernel.getQueryHandler().executeUpdate(sql);
            this.name = ieName;
        } catch (SQLException e) {
            log.fatal(e.getMessage());
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "InstElem::setName()");
        }
    }

    /**
	 * 
	 * @param aaName
	 * @return
	 * @throws AoException
	 */
    public NameValueUnit getValue(String aaName) throws AoException {
        NameValueUnit retval = null;
        ApplAttr applAttr = this.applElem.getApplAttrByAaname(aaName);
        if (applAttr == null) {
            try {
                retval = this.getInstanceAttribute(aaName);
                if (retval == null) {
                    throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "InstanceElement::getValue()");
                }
            } catch (SQLException e) {
                log.fatal(e.getMessage());
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "InstanceElement::getValue()");
            }
        }
        if ("AoLocalColumn".equals(this.applElem.getBename())) {
            String baName = applAttr.getBaName();
            if ("flags".equals(baName)) {
                retval = this.getSvcVal().getFlags();
            } else if ("generation_parameters".equals(baName)) {
                retval = this.getSvcVal().getGenerationParameters();
            } else if ("values".equals(baName)) {
                retval = this.getSvcVal().getValues();
            }
        }
        if (retval == null) {
            String dbtName = this.applElem.getSvcent().getDbtname();
            String dbcName = applAttr.getSvcAttr().getDbcname();
            String idColName = this.applElem.getDbcnameByBaname("id");
            String sql = null;
            DataType datatype = applAttr.getDType();
            switch(datatype.value()) {
                case DataType._DT_STRING:
                case DataType._DT_SHORT:
                case DataType._DT_FLOAT:
                case DataType._DT_BOOLEAN:
                case DataType._DT_BYTE:
                case DataType._DT_LONG:
                case DataType._DT_DOUBLE:
                case DataType._DT_LONGLONG:
                case DataType._DT_ID:
                case DataType._DT_DATE:
                case DataType._DT_BYTESTR:
                case DataType._DT_ENUM:
                    sql = SqlHelper.format("select %s from %s where %s=%s", new String[] { dbcName, dbtName, idColName, Long.toString(iid) });
                    break;
                case DataType._DT_BLOB:
                    sql = SqlHelper.format("select %s,BLOB from %s where %s=%s", new String[] { dbcName, dbtName, idColName, Long.toString(iid) });
                    break;
                case DataType._DT_COMPLEX:
                case DataType._DT_DCOMPLEX:
                case DataType._DS_STRING:
                case DataType._DS_SHORT:
                case DataType._DS_FLOAT:
                case DataType._DS_BOOLEAN:
                case DataType._DS_BYTE:
                case DataType._DS_LONG:
                case DataType._DS_DOUBLE:
                case DataType._DS_LONGLONG:
                case DataType._DS_COMPLEX:
                case DataType._DS_DCOMPLEX:
                case DataType._DS_ID:
                case DataType._DS_DATE:
                case DataType._DS_BYTESTR:
                case DataType._DT_EXTERNALREFERENCE:
                case DataType._DS_EXTERNALREFERENCE:
                    sql = SqlHelper.format("select %s from %s_ARRAY where IID=%s order by ORD", new String[] { dbcName, dbtName, Long.toString(iid) });
                    break;
                default:
                    break;
            }
            if (sql != null) {
                try {
                    List list = (List) kernel.getQueryHandler().executeQuery(sql, new de.ibk.ods.core.sql.ArrayListHandler());
                    retval = new NameValueUnit();
                    retval.valName = aaName;
                    retval.unit = "";
                    retval.value = new TS_Value();
                    retval.value.flag = 15;
                    retval.value.u = de.ibk.ods.util.DataType.listToU(datatype, list);
                    int funit = applAttr.getUnitId();
                    if (funit > 0) {
                    }
                } catch (SQLException e) {
                    log.fatal(e.getMessage());
                }
            }
        }
        if (retval == null) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "InstanceElement::getValue()");
        }
        return retval;
    }

    /**
	 * 
	 * @param value
	 */
    public void setValue(NameValueUnit value) throws AoException {
        String valName = value.valName;
        ApplAttr applAttr = this.applElem.getApplAttrByAaname(valName);
        if (applAttr == null) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "InstanceElement::setValue()");
        }
        if ("AoLocalColumn".equals(this.applElem.getBename())) {
            String baName = applAttr.getBaName();
            if ("flags".equals(baName)) {
                this.getSvcVal().setFlags(value);
                return;
            } else if ("generation_parameters".equals(baName)) {
                this.getSvcVal().setGenerationParameters(value);
                return;
            } else if ("independent".equals(baName)) {
                this.getSvcVal().setIndepFlag(value.value.u.shortVal());
            } else if ("values".equals(baName)) {
                this.getSvcVal().setValues(value);
                return;
            }
        }
        if (("AoUser".equals(this.applElem.getBename())) && ("password".equals(applAttr.getBaName()))) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                value.value.u.stringVal(new String(md.digest(value.value.u.stringVal().getBytes())));
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage());
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "InstanceElement::setValue()");
            }
        }
        boolean isArray = false;
        boolean isBlob = false;
        boolean isByteStr = false;
        String dbtName = this.applElem.getSvcent().getDbtname();
        String dbcName = applAttr.getSvcAttr().getDbcname();
        StringBuffer sql = new StringBuffer(SqlHelper.format("update %s set %s=", new String[] { dbtName, dbcName }));
        switch(value.value.u.discriminator().value()) {
            case DataType._DT_UNKNOWN:
                break;
            case DataType._DT_STRING:
                sql.append("'");
                sql.append(value.value.u.stringVal());
                sql.append("'");
                break;
            case DataType._DT_SHORT:
                sql.append(value.value.u.shortVal());
                break;
            case DataType._DT_FLOAT:
                sql.append(value.value.u.floatVal());
                break;
            case DataType._DT_BOOLEAN:
                sql.append(value.value.u.booleanVal());
                break;
            case DataType._DT_BYTE:
                sql.append(value.value.u.byteVal());
                break;
            case DataType._DT_LONG:
                sql.append(value.value.u.longVal());
                break;
            case DataType._DT_DOUBLE:
                sql.append(value.value.u.doubleVal());
                break;
            case DataType._DT_LONGLONG:
            case DataType._DT_ID:
                long lval = de.ibk.ods.util.DataType.longlongToLong(value.value.u.longlongVal());
                sql.append(lval);
                break;
            case DataType._DT_DATE:
                sql.append("'");
                sql.append(value.value.u.dateVal());
                sql.append("'");
                break;
            case DataType._DT_BYTESTR:
                sql.append("?");
                isByteStr = true;
                break;
            case DataType._DT_BLOB:
                String header = value.value.u.blobVal().getHeader();
                isBlob = true;
                sql.append("'");
                sql.append(header);
                sql.append("'");
                break;
            case DataType._DT_COMPLEX:
            case DataType._DT_DCOMPLEX:
            case DataType._DS_STRING:
            case DataType._DS_SHORT:
            case DataType._DS_FLOAT:
            case DataType._DS_BOOLEAN:
            case DataType._DS_BYTE:
            case DataType._DS_LONG:
            case DataType._DS_DOUBLE:
            case DataType._DS_LONGLONG:
            case DataType._DS_COMPLEX:
            case DataType._DS_DCOMPLEX:
            case DataType._DS_ID:
            case DataType._DS_DATE:
            case DataType._DS_BYTESTR:
            case DataType._DT_EXTERNALREFERENCE:
            case DataType._DS_EXTERNALREFERENCE:
            case DataType._DS_ENUM:
                sql = new StringBuffer(SqlHelper.format("delete from %s_ARRAY where %s<>NULL and IID=%s", new String[] { dbtName, dbcName, Long.toString(this.iid) }));
                try {
                    kernel.getQueryHandler().executeUpdate(sql.toString());
                } catch (SQLException e) {
                    log.info(e.getMessage());
                }
                isArray = true;
                break;
            case DataType._DT_ENUM:
                sql.append(value.value.u.enumVal());
                break;
            default:
                break;
        }
        try {
            if (isArray) {
                saveArray(Long.toString(iid), dbtName, dbcName, value.value.u);
            } else {
                String idColName = this.applElem.getDbcnameByBaname("id");
                sql.append(" where ");
                sql.append(idColName);
                sql.append("=");
                sql.append(this.iid);
                if (isByteStr) {
                    kernel.getQueryHandler().executeUpdate(sql.toString(), value.value.u.bytestrVal());
                } else {
                    kernel.getQueryHandler().executeUpdate(sql.toString());
                    if (isBlob) {
                        sql = new StringBuffer(SqlHelper.format("update %s set BLOB=? where %s=%s", new String[] { dbtName, idColName, Long.toString(iid) }));
                        int len = value.value.u.blobVal().getLength();
                        kernel.getQueryHandler().executeUpdate(sql.toString(), value.value.u.blobVal().get(0, len));
                    }
                }
            }
        } catch (SQLException e) {
            log.fatal(e.getMessage());
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "InstanceElement::setValue()");
        }
    }

    /**
	 * 
	 * @param iid
	 * @param dbtName
	 * @param dbcName
	 * @param u
	 */
    private void saveArray(String iid, String dbtName, String dbcName, TS_Union u) {
        ArrayList stmts = new ArrayList();
        int i;
        switch(u.discriminator().value()) {
            case DataType._DT_COMPLEX:
                T_COMPLEX complex = u.complexVal();
                stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,0,%s)", new String[] { dbtName, dbcName, iid, Float.toString(complex.r) }));
                stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,1,%s)", new String[] { dbtName, dbcName, iid, Float.toString(complex.i) }));
                break;
            case DataType._DT_DCOMPLEX:
                T_DCOMPLEX dcomplex = u.dcomplexVal();
                stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,0,%s)", new String[] { dbtName, dbcName, iid, Double.toString(dcomplex.r) }));
                stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,1,%s)", new String[] { dbtName, dbcName, iid, Double.toString(dcomplex.i) }));
                break;
            case DataType._DS_STRING:
                String[] strseq = u.stringSeq();
                for (i = 0; i < strseq.length; i++) {
                    stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,%s,'%s')", new String[] { dbtName, dbcName, iid, Integer.toString(i + 1), strseq[i] }));
                }
                break;
            case DataType._DS_SHORT:
                short[] shortseq = u.shortSeq();
                for (i = 0; i < shortseq.length; i++) {
                    stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,%s,%s)", new String[] { dbtName, dbcName, iid, Integer.toString(i + 1), Short.toString(shortseq[i]) }));
                }
                break;
            case DataType._DS_FLOAT:
                float[] floatseq = u.floatSeq();
                for (i = 0; i < floatseq.length; i++) {
                    stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,%s,%s)", new String[] { dbtName, dbcName, iid, Integer.toString(i + 1), Float.toString(floatseq[i]) }));
                }
                break;
            case DataType._DS_BOOLEAN:
                break;
            case DataType._DS_BYTE:
                byte[] byteseq = u.byteSeq();
                for (i = 0; i < byteseq.length; i++) {
                    stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,%s,%s)", new String[] { dbtName, dbcName, iid, Integer.toString(i + 1), Byte.toString(byteseq[i]) }));
                }
                break;
            case DataType._DS_LONG:
                int[] longseq = u.longSeq();
                for (i = 0; i < longseq.length; i++) {
                    stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,%s,%s)", new String[] { dbtName, dbcName, iid, Integer.toString(i + 1), Integer.toString(longseq[i]) }));
                }
                break;
            case DataType._DS_DOUBLE:
                double[] doubleseq = u.doubleSeq();
                for (i = 0; i < doubleseq.length; i++) {
                    stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,%s,%s)", new String[] { dbtName, dbcName, iid, Integer.toString(i + 1), Double.toString(doubleseq[i]) }));
                }
                break;
            case DataType._DS_LONGLONG:
            case DataType._DS_ID:
                T_LONGLONG[] longlongseq = u.longlongSeq();
                for (i = 0; i < longlongseq.length; i++) {
                    stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,%s,%s)", new String[] { dbtName, dbcName, iid, Integer.toString(i + 1), Long.toString(de.ibk.ods.util.DataType.longlongToLong(longlongseq[i])) }));
                }
                break;
            case DataType._DS_COMPLEX:
                break;
            case DataType._DS_DCOMPLEX:
                break;
            case DataType._DS_DATE:
                String[] dateseq = u.dateSeq();
                for (i = 0; i < dateseq.length; i++) {
                    stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,%s,'%s')", new String[] { dbtName, dbcName, iid, Integer.toString(i + 1), dateseq[i] }));
                }
                break;
            case DataType._DS_BYTESTR:
                break;
            case DataType._DT_EXTERNALREFERENCE:
                T_ExternalReference extref = u.extRefVal();
                stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,0,'%s')", new String[] { dbtName, dbcName, iid, extref.description }));
                stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,1,'%s')", new String[] { dbtName, dbcName, iid, extref.mimeType }));
                stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,2,'%s')", new String[] { dbtName, dbcName, iid, extref.location }));
                break;
            case DataType._DS_EXTERNALREFERENCE:
                break;
            case DataType._DS_ENUM:
                int[] enumseq = u.enumSeq();
                for (i = 0; i < enumseq.length; i++) {
                    stmts.add(SqlHelper.format("insert into %s_ARRAY (IID,ORD,%s) values (%s,%s,%s)", new String[] { dbtName, dbcName, iid, Integer.toString(i + 1), Integer.toString(enumseq[i]) }));
                }
                break;
            default:
                break;
        }
        for (i = 0; i < stmts.size(); i++) {
            try {
                kernel.getQueryHandler().executeUpdate((String) stmts.get(i));
            } catch (SQLException e) {
                log.fatal(e.getMessage());
            }
        }
    }

    /**
	 * 
	 * @param relName
	 * @param instElem
	 * @throws AoException 
	 */
    public void createRelation(String relName, InstElem instElem) throws AoException {
        int aid1 = this.applElem.getAid();
        ApplRel applRel = kernel.getApplicationStructureValue().getApplRel(aid1, relName);
        int aid2 = applRel.getElem2();
        String iid2 = Long.toString(instElem.getIid());
        try {
            String sql = null;
            if (applRel.getSvcattr() != null) {
                String dbcName = applRel.getSvcattr().getDbcname();
                if (!"NULL".equals(dbcName)) {
                    String dbtName = this.applElem.getSvcent().getDbtname();
                    String idColName = this.applElem.getDbcnameByBaname("id");
                    sql = SqlHelper.format("update %s set %s=%s where %s=%s", new String[] { dbtName, dbcName, iid2, idColName, Long.toString(this.iid) });
                } else {
                    ApplElem elem2 = kernel.getApplicationStructureValue().getApplElem(aid2);
                    String dbtName = elem2.getSvcent().getDbtname();
                    String invName = applRel.getSvcattr().getInvname();
                    ApplRel invRel = kernel.getApplicationStructureValue().getApplRel(aid2, aid1, invName);
                    dbcName = invRel.getSvcattr().getDbcname();
                    String idColName = elem2.getDbcnameByBaname("id");
                    sql = SqlHelper.format("update %s set %s=%s where %s=%s", new String[] { dbtName, dbcName, Long.toString(this.iid), idColName, iid2 });
                }
            } else if (applRel.getSvcref() != null) {
                SvcRef svcRef = applRel.getSvcref();
                String dbtName = svcRef.getDbtname();
                String refName = svcRef.getRefname();
                String idColName1 = this.applElem.getDbcnameByBaname("id");
                String idColName2 = kernel.getApplicationStructureValue().getApplElem(aid2).getDbcnameByBaname("id");
                String iid1 = Long.toString(this.iid);
                if (svcRef.getAid1() != aid1) {
                    idColName1 = idColName2;
                    iid1 = iid2;
                    idColName2 = this.applElem.getDbcnameByBaname("id");
                    iid2 = Long.toString(this.iid);
                }
                String query = SqlHelper.format("select * from %s where %s=%s and %s=%s", new String[] { dbtName, idColName1, iid1, idColName2, iid2 });
                Statement stmt = this.kernel.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query);
                if (!rs.next()) {
                    sql = SqlHelper.format("insert into %s values (%s,%s,'%s')", new String[] { dbtName, iid1, iid2, refName });
                }
                rs.close();
                stmt.close();
            } else {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "InstanceElement::createRelation()");
            }
            if (sql != null) {
                kernel.getQueryHandler().executeUpdate(sql);
                if ("AoLocalColumn".equals(this.applElem.getBename())) {
                    ApplElem applElem2 = this.kernel.getApplicationStructureValue().getApplElem(aid2);
                    if ("AoMeasurementQuantity".equals(applElem2.getBename())) {
                        this.getSvcVal().setMeqId(iid2);
                    } else if ("AoSubmatrix".equals(applElem2.getBename())) {
                        this.getSvcVal().setPMatNum(iid2);
                    }
                }
            }
        } catch (SQLException e) {
            log.fatal(e.getMessage());
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "InstanceElement::createRelation()");
        }
    }

    /**
	 * 
	 * @param relName
	 * @param iePattern
	 * @return
	 */
    public T_LONGLONG[] getRelatedInstances(String relName, String iePattern) {
        int aid1 = this.applElem.getAid();
        ApplRel applRel = this.kernel.getApplicationStructureValue().getApplRel(aid1, relName);
        int aid2 = applRel.getElem2();
        iePattern = iePattern.replace('*', '%');
        iePattern = iePattern.replace('?', '_');
        ApplElem elem2 = kernel.getApplicationStructureValue().getApplElem(aid2);
        String fdbtname = elem2.getSvcent().getDbtname();
        String fidcolname = elem2.getDbcnameByBaname("id");
        String fnamecolname = elem2.getDbcnameByBaname("name");
        String sql = null;
        if (applRel.getSvcattr() != null) {
            String dbcname = applRel.getSvcattr().getDbcname();
            if (!"NULL".equals(dbcname)) {
                String dbtName = this.applElem.getSvcent().getDbtname();
                String idColName = this.applElem.getDbcnameByBaname("id");
                sql = SqlHelper.format("select %s.%s from %s,%s where %s.%s=%s and %s.%s=%s.%s and %s.%s like '%s'", new String[] { dbtName, dbcname, dbtName, fdbtname, dbtName, idColName, Long.toString(this.iid), fdbtname, fidcolname, dbtName, dbcname, fdbtname, fnamecolname, iePattern });
            } else {
                String invname = applRel.getSvcattr().getInvname();
                ApplRel invrel = kernel.getApplicationStructureValue().getApplRel(aid2, aid1, invname);
                dbcname = invrel.getSvcattr().getDbcname();
                sql = SqlHelper.format("select %s from %s where %s=%s and %s like '%s'", new String[] { fidcolname, fdbtname, dbcname, Long.toString(this.iid), fnamecolname, iePattern });
            }
        } else if (applRel.getSvcref() != null) {
            String dbcname1 = this.applElem.getDbcnameByBaname("id");
            String dbcname2 = elem2.getDbcnameByBaname("id");
            SvcRef svcref = applRel.getSvcref();
            String dbtname = svcref.getDbtname();
            sql = SqlHelper.format("select %s.%s from %s,%s where %s.%s=%s and %s.%s=%s.%s and %s.%s like '%s'", new String[] { dbtname, dbcname2, dbtname, fdbtname, dbtname, dbcname1, Long.toString(this.iid), fdbtname, fidcolname, dbtname, dbcname2, fdbtname, fnamecolname, iePattern });
        }
        T_LONGLONG[] retval = null;
        try {
            Statement statement = kernel.getConnection().createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            Vector v = new Vector();
            while (resultSet.next()) {
                T_LONGLONG iid = new T_LONGLONG(0, resultSet.getInt(1));
                v.add(iid);
            }
            retval = new T_LONGLONG[v.size()];
            v.toArray(retval);
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            log.fatal(e.getMessage());
        }
        return retval;
    }

    /**
	 * 
	 * @param attribute
	 * @throws SQLException
	 */
    public void addInstanceAttribute(NameValueUnit attribute) throws SQLException {
        String aid = Integer.toString(this.applElem.getAid());
        String iid = Long.toString(this.iid);
        String valName = attribute.valName;
        TS_Union u = attribute.value.u;
        int dataType = u.discriminator().value();
        String sql = null;
        switch(dataType) {
            case DataType._DT_STRING:
                sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,NULL,%s)", new String[] { aid, iid, valName, Integer.toString(dataType), u.stringVal() });
                break;
            case DataType._DT_SHORT:
                sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,%s,NULL)", new String[] { aid, iid, valName, Integer.toString(dataType), Short.toString(u.shortVal()) });
                break;
            case DataType._DT_FLOAT:
                sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,%s,NULL)", new String[] { aid, iid, valName, Integer.toString(dataType), Float.toString(u.floatVal()) });
                break;
            case DataType._DT_BOOLEAN:
                if (u.booleanVal()) {
                    sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,%s,NULL)", new String[] { aid, iid, valName, Integer.toString(dataType), "1" });
                } else {
                    sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,%s,NULL)", new String[] { aid, iid, valName, Integer.toString(dataType), "0" });
                }
                break;
            case DataType._DT_BYTE:
                sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,%s,NULL)", new String[] { aid, iid, valName, Integer.toString(dataType), Byte.toString(u.byteVal()) });
                break;
            case DataType._DT_LONG:
                sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,%s,NULL)", new String[] { aid, iid, valName, Integer.toString(dataType), Integer.toString(u.longVal()) });
                break;
            case DataType._DT_DOUBLE:
                sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,%s,NULL)", new String[] { aid, iid, valName, Integer.toString(dataType), Double.toString(u.doubleVal()) });
                break;
            case DataType._DT_LONGLONG:
            case DataType._DT_ID:
                long lval = de.ibk.ods.util.DataType.longlongToLong(u.longlongVal());
                sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,%s,NULL)", new String[] { aid, iid, valName, Integer.toString(dataType), Long.toString(lval) });
                break;
            case DataType._DT_DATE:
                sql = SqlHelper.format("insert into SVCINST values (%s,%s,%s,%s,0,NULL,%s)", new String[] { aid, iid, valName, Integer.toString(dataType), u.dateVal() });
                break;
            default:
                break;
        }
        if (sql != null) {
            this.kernel.getQueryHandler().executeUpdate(sql);
        }
    }

    /**
	 * 
	 * @param iaPattern
	 * @return
	 * @throws SQLException
	 */
    public String[] listInstanceAttributes(String iaPattern) throws SQLException {
        iaPattern = iaPattern.replace('*', '%');
        iaPattern = iaPattern.replace('?', '_');
        String aid = Integer.toString(this.applElem.getAid());
        String iid = Long.toString(this.iid);
        String sql = SqlHelper.format("select NAME from SVCINST where AID=%s and IID=%s and NAME like '%s'", new String[] { aid, iid, iaPattern });
        List list = (List) this.kernel.getQueryHandler().executeQuery(sql, new ArrayListHandler());
        String[] retval = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object[] obj = (Object[]) list.get(i);
            retval[i] = String.valueOf(obj[0]);
        }
        return retval;
    }

    /**
	 * 
	 * @param iaName
	 * @throws SQLException
	 */
    public void removeInstanceAttribute(String iaName) throws SQLException {
        String aid = Integer.toString(this.applElem.getAid());
        String iid = Long.toString(this.iid);
        String sql = SqlHelper.format("delete from SVCINST where AID=%s and IID=%s and NAME='%s'", new String[] { aid, iid, iaName });
        this.kernel.getQueryHandler().executeUpdate(sql);
    }

    /**
	 * 
	 * @param oldName
	 * @param newName
	 * @throws SQLException
	 */
    public void renameInstanceAttribute(String oldName, String newName) throws SQLException {
        String aid = Integer.toString(this.applElem.getAid());
        String iid = Long.toString(this.iid);
        String sql = SqlHelper.format("update SVCINST set NAME='%s' where AID=%s and IID=%s and NAME='%s'", new String[] { newName, aid, iid, oldName });
        this.kernel.getQueryHandler().executeUpdate(sql);
    }

    /**
	 * 
	 * @param aaName
	 * @return
	 * @throws SQLException
	 */
    private NameValueUnit getInstanceAttribute(String aaName) throws SQLException {
        NameValueUnit retval = null;
        String aid = Integer.toString(this.applElem.getAid());
        String iid = Long.toString(this.iid);
        String sql = SqlHelper.format("select * from SVCINST where AID=%s and IID=%s and NAME='%s'", new String[] { aid, iid, aaName });
        Object[] obj = (Object[]) this.kernel.getQueryHandler().executeQuery(sql, new ArrayHandler());
        if ((obj != null) && (obj.length > 0)) {
            retval = new NameValueUnit();
            retval.unit = "";
            retval.valName = aaName;
            retval.value = new TS_Value();
            retval.value.flag = (short) 15;
            retval.value.u = new TS_Union();
            retval.value.u.__default();
            String numVal = String.valueOf(obj[5]);
            int dataType = Integer.parseInt(String.valueOf(obj[3]));
            switch(dataType) {
                case DataType._DT_STRING:
                    retval.value.u.stringVal(String.valueOf(obj[6]));
                    break;
                case DataType._DT_SHORT:
                    retval.value.u.shortVal(Short.parseShort(numVal));
                    break;
                case DataType._DT_FLOAT:
                    retval.value.u.floatVal(Float.parseFloat(numVal));
                    break;
                case DataType._DT_BOOLEAN:
                    retval.value.u.booleanVal("1".equals(numVal));
                    break;
                case DataType._DT_BYTE:
                    retval.value.u.byteVal(Byte.parseByte(numVal));
                    break;
                case DataType._DT_LONG:
                    retval.value.u.longVal(Integer.parseInt(numVal));
                    break;
                case DataType._DT_DOUBLE:
                    retval.value.u.doubleVal(Double.parseDouble(numVal));
                    break;
                case DataType._DT_LONGLONG:
                case DataType._DT_ID:
                    T_LONGLONG llval = de.ibk.ods.util.DataType.longToLonglong(Long.parseLong(numVal));
                    retval.value.u.longlongVal(llval);
                    break;
                case DataType._DT_DATE:
                    retval.value.u.dateVal(String.valueOf(obj[6]));
                    break;
                default:
                    break;
            }
        }
        return retval;
    }
}
