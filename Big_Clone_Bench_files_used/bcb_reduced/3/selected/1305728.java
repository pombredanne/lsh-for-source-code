package de.ibk.ods.implementation;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.asam.ods.AoException;
import org.asam.ods.AoSessionPOA;
import org.asam.ods.ApplAttr;
import org.asam.ods.ApplElem;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.ApplElemAccessHelper;
import org.asam.ods.ApplRel;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.ApplicationStructureHelper;
import org.asam.ods.ApplicationStructureValue;
import org.asam.ods.BaseStructure;
import org.asam.ods.Blob;
import org.asam.ods.BlobHelper;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameIterator;
import org.asam.ods.NameIteratorHelper;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueIterator;
import org.asam.ods.NameValueIteratorHelper;
import org.asam.ods.NameValueUnit;
import org.asam.ods.QueryEvaluator;
import org.asam.ods.QueryEvaluatorHelper;
import org.asam.ods.RelationRange;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.omg.CORBA.Object;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.Current;
import org.omg.PortableServer.CurrentHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.CurrentPackage.NoContext;
import org.omg.PortableServer.POAPackage.AdapterNonExistent;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import de.ibk.ods.Global;
import de.ibk.ods.basemodel.BaseModel;
import de.ibk.ods.core.Kernel;

/**
 * @author Reinhard Kessler, Ingenieurbüro Kessler
 * @version 5.0.0
 */
public class AoSessionImpl extends AoSessionPOA {

    /**
	 * 
	 */
    private Logger log = LogManager.getLogger("de.ibk.ods.openaos");

    private Hashtable context = new Hashtable();

    /**
	 * 
	 */
    private Kernel kernel;

    /**
	 * 
	 */
    private ApplElemAccess applElemAccess = null;

    /**
	 * 
	 */
    private ApplicationStructure applicationStructure = null;

    /**
	 * 
	 */
    private QueryEvaluator queryEvaluator = null;

    /**
	 * 
	 * @param dataSource
	 * @param auth
	 */
    public AoSessionImpl(Kernel kernel, String auth) {
        super();
        log.debug("Enter AoSessionImpl::AoSessionImpl()");
        this.kernel = kernel;
        this.context.put("SECURITY", "1");
        String[] ci = auth.split(",");
        for (int i = 0; i < ci.length; i++) {
            String item = ci[i].trim();
            String[] cs = item.split("=");
            if (cs.length == 2) {
                try {
                    this.setContextString(cs[0].toUpperCase(), cs[1]);
                } catch (AoException e) {
                    log.debug(e.reason);
                }
            }
        }
        log.debug("Exit AoSessionImpl::AoSessionImpl()");
    }

    public void abortTransaction() throws AoException {
        log.debug("Enter AoSessionImpl::abortTransaction()");
        try {
            kernel.getConnection().rollback();
            kernel.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            log.fatal(e.getMessage());
        }
        log.debug("Exit AoSessionImpl::abortTransaction()");
    }

    public void close() throws AoException {
        log.debug("Enter AoSessionImpl::close()");
        Current current = null;
        byte[] oid = null;
        POA adapter = null;
        try {
            Object obj = this._orb().resolve_initial_references("POACurrent");
            current = CurrentHelper.narrow(obj);
            oid = current.get_object_id();
            adapter = current.get_POA();
        } catch (InvalidName e) {
            log.fatal(e.getMessage());
        } catch (NoContext e) {
            log.fatal(e.getMessage());
        }
        if (this.applicationStructure != null) {
            try {
                adapter.the_parent().find_POA("ApplicationStructure", false).deactivate_object(oid);
            } catch (AdapterNonExistent e) {
                log.fatal(e.getMessage());
            } catch (ObjectNotActive e) {
                log.fatal(e.getMessage());
            } catch (WrongPolicy e) {
                log.fatal(e.getMessage());
            }
        }
        if (this.applElemAccess != null) {
            try {
                adapter.the_parent().find_POA("ApplElemAccess", false).deactivate_object(oid);
            } catch (AdapterNonExistent e) {
                log.fatal(e.getMessage());
            } catch (ObjectNotActive e) {
                log.fatal(e.getMessage());
            } catch (WrongPolicy e) {
                log.fatal(e.getMessage());
            }
        }
        if (this.queryEvaluator != null) {
            try {
                adapter.the_parent().find_POA("QueryEvaluator", false).deactivate_object(oid);
            } catch (AdapterNonExistent e) {
                log.fatal(e.getMessage());
            } catch (ObjectNotActive e) {
                log.fatal(e.getMessage());
            } catch (WrongPolicy e) {
                log.fatal(e.getMessage());
            }
        }
        try {
            adapter.deactivate_object(oid);
        } catch (ObjectNotActive e) {
            log.fatal(e.getMessage());
        } catch (WrongPolicy e) {
            log.fatal(e.getMessage());
        }
        kernel.close();
        Global.KernelMap.remove(new String(oid));
        log.debug("Exit AoSessionImpl::close()");
    }

    public void commitTransaction() throws AoException {
        log.debug("Enter AoSessionImpl::commitTransaction()");
        try {
            kernel.getConnection().commit();
            kernel.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            log.fatal(e.getMessage());
        }
        log.debug("Exit AoSessionImpl::commitTransaction()");
    }

    public ApplicationStructure getApplicationStructure() throws AoException {
        log.debug("Enter AoSessionImpl::getApplicationStructure()");
        if (applicationStructure == null) {
            try {
                POA poa = this._poa().the_parent().find_POA("ApplicationStructure", false);
                ApplicationStructureImpl as = new ApplicationStructureImpl(poa, kernel);
                poa.activate_object_with_id(this._object_id(), as);
                org.omg.CORBA.Object obj = poa.servant_to_reference(as);
                applicationStructure = ApplicationStructureHelper.narrow(obj);
            } catch (ServantAlreadyActive e) {
                log.fatal(e.getMessage());
            } catch (WrongPolicy e) {
                log.fatal(e.getMessage());
            } catch (ObjectAlreadyActive e) {
                log.fatal(e.getMessage());
            } catch (ServantNotActive e) {
                log.fatal(e.getMessage());
            } catch (AdapterNonExistent e) {
                log.fatal(e.getMessage());
            }
            if (applicationStructure == null) {
                log.error("ApplicationStructure==NULL");
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "AoSession::getApplicationStructure()");
            }
        }
        log.debug("Exit AoSessionImpl::getApplicationStructure()");
        return applicationStructure;
    }

    public ApplicationStructureValue getApplicationStructureValue() throws AoException {
        log.debug("Enter AoSessionImpl::getApplicationStructureValue()");
        ApplElem[] applElems = this.getApplElems();
        ApplRel[] applRels = this.getApplRels();
        ApplicationStructureValue retval = new ApplicationStructureValue(applElems, applRels);
        log.debug("Exit AoSessionImpl::getApplicationStructureValue()");
        return retval;
    }

    public BaseStructure getBaseStructure() throws AoException {
        log.debug("Enter AoSessionImpl::getBaseStructure()");
        BaseStructure retval = BaseModel.getBaseStructure();
        log.debug("Exit AoSessionImpl::getBaseStructure()");
        return retval;
    }

    public NameValueIterator getContext(String varPattern) throws AoException {
        log.debug("Enter AoSessionImpl::getContext()");
        Vector v = new Vector();
        for (Enumeration e = this.context.elements(); e.hasMoreElements(); ) {
            java.lang.Object obj = e.nextElement();
            NameValue namevalue = (NameValue) obj;
            if (Global.patternMatches(varPattern, namevalue.valName)) {
                v.add(namevalue);
            }
        }
        NameValue[] namevalues = new NameValue[v.size()];
        v.toArray(namevalues);
        NameValueIteratorImpl namevalueiterator = new NameValueIteratorImpl(namevalues);
        try {
            POA poa = this._poa().the_parent().find_POA("NameValueIterator", false);
            org.omg.CORBA.Object obj = poa.servant_to_reference(namevalueiterator);
            log.debug("Exit AoSessionImpl::getContext()");
            return NameValueIteratorHelper.narrow(obj);
        } catch (ServantNotActive e) {
            log.fatal(e.getMessage());
        } catch (WrongPolicy e) {
            log.fatal(e.getMessage());
        } catch (AdapterNonExistent e) {
            log.fatal(e.getMessage());
        }
        throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "AoSession::getContext()");
    }

    public NameValue getContextByName(String varName) throws AoException {
        log.debug("Enter AoSessionImpl::getContextByName()");
        java.lang.Object obj = this.context.get(varName);
        if (obj == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "AoSession::getContextByName()");
        }
        log.debug("Exit AoSessionImpl::getContextByName()");
        return (NameValue) obj;
    }

    public NameIterator listContext(String varPattern) throws AoException {
        log.debug("Enter AoSessionImpl::listContext()");
        Vector v = new Vector();
        for (Enumeration e = this.context.elements(); e.hasMoreElements(); ) {
            NameValue namevalue = (NameValue) e.nextElement();
            if (Global.patternMatches(varPattern, namevalue.valName)) {
                v.add(namevalue.valName);
            }
        }
        String[] namelist = new String[v.size()];
        v.toArray(namelist);
        try {
            NameIteratorImpl nameiterator = new NameIteratorImpl(namelist);
            POA poa = this._poa().the_parent().find_POA("NameIterator", false);
            poa.activate_object_with_id(Global.getUniqueId().getBytes(), nameiterator);
            org.omg.CORBA.Object obj = poa.servant_to_reference(nameiterator);
            log.debug("Exit AoSessionImpl::listContext()");
            return NameIteratorHelper.narrow(obj);
        } catch (ServantNotActive e) {
            log.fatal(e.getMessage());
        } catch (WrongPolicy e) {
            log.fatal(e.getMessage());
        } catch (ServantAlreadyActive e) {
            log.fatal(e.getMessage());
        } catch (ObjectAlreadyActive e) {
            log.fatal(e.getMessage());
        } catch (AdapterNonExistent e) {
            log.fatal(e.getMessage());
        }
        throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "AoSession::listContext()");
    }

    public void removeContext(String varPattern) throws AoException {
        log.debug("Enter AoSessionImpl::removeContext()");
        for (Enumeration e = this.context.elements(); e.hasMoreElements(); ) {
            java.lang.Object obj = e.nextElement();
            NameValue namevalue = (NameValue) obj;
            if (Global.patternMatches(varPattern, namevalue.valName)) {
                this.context.remove(namevalue.valName);
            }
        }
        log.debug("Exit AoSessionImpl::removeContext()");
    }

    public void setContext(NameValue contextVariable) throws AoException {
        log.debug("Enter AoSessionImpl::setContext()");
        String key = contextVariable.valName;
        if (this.context.containsKey(key)) {
            this.context.remove(key);
        }
        if ("PASSWORD".equals(key)) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                contextVariable.value.u.stringVal(new String(md.digest(contextVariable.value.u.stringVal().getBytes())));
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage());
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "AoSessionImpl::setContext()");
            }
        }
        this.context.put(key, contextVariable);
        log.debug("Exit AoSessionImpl::setContext()");
    }

    public void setContextString(String varName, String value) throws AoException {
        log.debug("Enter AoSessionImpl::setContextString()");
        NameValue namevalue = new NameValue();
        namevalue.valName = varName;
        namevalue.value = new TS_Value();
        namevalue.value.flag = 15;
        namevalue.value.u = new TS_Union();
        namevalue.value.u.stringVal(value);
        this.setContext(namevalue);
        log.debug("Exit AoSessionImpl::setContextString()");
    }

    public void startTransaction() throws AoException {
        log.debug("Enter AoSessionImpl::startTransaction()");
        try {
            kernel.getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            log.fatal(e.getMessage());
        }
        log.debug("Exit AoSessionImpl::startTransaction()");
    }

    public void flush() throws AoException {
        log.debug("Enter AoSessionImpl::flush()");
        this.commitTransaction();
        log.debug("Exit AoSessionImpl::flush()");
    }

    public void setCurrentInitialRights(InitialRight[] irlEntries, boolean set) throws AoException {
        log.debug("Enter AoSessionImpl::setCurrentInitialRights()");
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "AoSession::setCurrentInitialRights()");
    }

    public short getLockMode() throws AoException {
        log.debug("Enter AoSessionImpl::getLockMode()");
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "AoSession::getLockMode()");
    }

    public void setLockMode(short lockMode) throws AoException {
        log.debug("Enter AoSessionImpl::setLockMode()");
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "AoSession::setLockMode()");
    }

    public ApplElemAccess getApplElemAccess() throws AoException {
        log.debug("Enter AoSessionImpl::getApplElemAccess()");
        if (this.applElemAccess == null) {
            try {
                POA poa = this._poa().the_parent().find_POA("ApplElemAccess", false);
                ApplElemAccessImpl aea = new ApplElemAccessImpl(poa, kernel);
                poa.activate_object_with_id(this._object_id(), aea);
                org.omg.CORBA.Object obj = poa.servant_to_reference(aea);
                this.applElemAccess = ApplElemAccessHelper.narrow(obj);
            } catch (ServantAlreadyActive e) {
                log.fatal(e.getMessage());
            } catch (WrongPolicy e) {
                log.fatal(e.getMessage());
            } catch (ObjectAlreadyActive e) {
                log.fatal(e.getMessage());
            } catch (ServantNotActive e) {
                log.fatal(e.getMessage());
            } catch (AdapterNonExistent e) {
                log.fatal(e.getMessage());
            }
            if (this.applElemAccess == null) {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "AoSession::getApplElemAccess()");
            }
        }
        log.debug("Exit AoSessionImpl::getApplElemAccess()");
        return this.applElemAccess;
    }

    public void setPassword(String username, String oldPassword, String newPassword) throws AoException {
        log.debug("Enter AoSessionImpl::setPassword()");
        if ((username == null) || ("".equals(username))) {
            NameValue nv = getContextByName("USER");
            username = nv.value.u.stringVal();
        }
        ApplicationElement elem1 = this.getApplicationStructure().getElements("AoUser")[0];
        InstanceElement ieuser = elem1.getInstanceByName(username);
        if (ieuser == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "AoSession::setPassword()");
        }
        boolean su = false;
        ApplicationElement elem2 = getApplicationStructure().getElements("AoUserGroup")[0];
        ApplicationRelation ar = getApplicationStructure().getRelations(elem1, elem2)[0];
        InstanceElementIterator iei = ieuser.getRelatedInstances(ar, "*");
        for (int i = 0; i < iei.getCount(); i++) {
            InstanceElement ie = iei.nextOne();
            NameValueUnit nvu = ie.getValueByBaseName("superuser_flag");
            if (nvu.value.u.shortVal() == 1) {
                su = true;
            }
        }
        iei.destroy();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "AoSession::setPassword()");
        }
        oldPassword = new String(md.digest(oldPassword.getBytes()));
        NameValueUnit nvu = ieuser.getValueByBaseName("password");
        if (!su) {
            String currpassword = nvu.value.u.stringVal();
            if (!currpassword.equals(oldPassword)) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "AoSession::setPassword()");
            }
        }
        nvu.value.u.stringVal(newPassword);
        ieuser.setValue(nvu);
        setContextString("PASSWORD", newPassword);
        log.debug("Exit AoSessionImpl::setPassword()");
    }

    public String getDescription() throws AoException {
        log.debug("Enter AoSessionImpl::getDescription()");
        String retval = "NA";
        ApplicationElement[] elems = getApplicationStructure().getElementsByBaseType("AoEnvironment");
        if (elems.length == 1) {
            InstanceElementIterator it = elems[0].getInstances("*");
            if (it.getCount() == 1) {
                InstanceElement inst = it.nextOne();
                try {
                    NameValueUnit nvu = inst.getValueByBaseName("description");
                    retval = nvu.value.u.stringVal();
                } catch (AoException e) {
                }
            }
            it.destroy();
        }
        log.debug("Exit AoSessionImpl::getDescription()");
        return retval;
    }

    public String getName() throws AoException {
        log.debug("Enter AoSessionImpl::getName()");
        String retval = "NA";
        ApplicationElement[] elems = getApplicationStructure().getElementsByBaseType("AoEnvironment");
        if (elems.length == 1) {
            InstanceElementIterator it = elems[0].getInstances("*");
            if (it.getCount() == 1) {
                InstanceElement inst = it.nextOne();
                try {
                    NameValueUnit nvu = inst.getValueByBaseName("name");
                    retval = nvu.value.u.stringVal();
                } catch (AoException e) {
                }
            }
            it.destroy();
        }
        log.debug("Exit AoSessionImpl::getName()");
        return retval;
    }

    public String getType() throws AoException {
        log.debug("Enter AoSessionImpl::getType()");
        String retval = "NA";
        ApplicationElement[] elems = getApplicationStructure().getElementsByBaseType("AoEnvironment");
        if (elems.length == 1) {
            InstanceElementIterator it = elems[0].getInstances("*");
            if (it.getCount() == 1) {
                InstanceElement inst = it.nextOne();
                try {
                    NameValueUnit nvu = inst.getValueByBaseName("application_model_type");
                    retval = nvu.value.u.stringVal();
                } catch (AoException e) {
                }
            }
            it.destroy();
        }
        log.debug("Exit AoSessionImpl::getType()");
        return retval;
    }

    public QueryEvaluator createQueryEvaluator() throws AoException {
        log.debug("Enter AoSessionImpl::createQueryEvaluator()");
        if (this.queryEvaluator == null) {
            try {
                POA poa = this._poa().the_parent().find_POA("QueryEvaluator", false);
                QueryEvaluatorImpl qe = new QueryEvaluatorImpl(poa, kernel);
                poa.activate_object_with_id(this._object_id(), qe);
                org.omg.CORBA.Object obj = poa.servant_to_reference(qe);
                this.queryEvaluator = QueryEvaluatorHelper.narrow(obj);
            } catch (ServantAlreadyActive e) {
                log.fatal(e.getMessage());
            } catch (WrongPolicy e) {
                log.fatal(e.getMessage());
            } catch (ObjectAlreadyActive e) {
                log.fatal(e.getMessage());
            } catch (ServantNotActive e) {
                log.fatal(e.getMessage());
            } catch (AdapterNonExistent e) {
                log.fatal(e.getMessage());
            }
            if (this.queryEvaluator == null) {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "AoSession::getApplElemAccess()");
            }
        }
        log.debug("Exit AoSessionImpl::createQueryEvaluator()");
        return this.queryEvaluator;
    }

    public Blob createBlob() throws AoException {
        log.debug("Enter AoSessionImpl::createBlob()");
        Blob retval = null;
        try {
            BlobImpl blob = new BlobImpl();
            POA poa = this._poa().the_parent().find_POA("Blob", false);
            poa.activate_object_with_id(Global.getUniqueId().getBytes(), blob);
            org.omg.CORBA.Object obj = poa.servant_to_reference(blob);
            retval = BlobHelper.narrow(obj);
        } catch (WrongPolicy e) {
            log.fatal(e.getMessage());
        } catch (ServantAlreadyActive e) {
            log.fatal(e.getMessage());
        } catch (ObjectAlreadyActive e) {
            log.fatal(e.getMessage());
        } catch (ServantNotActive e) {
            log.fatal(e.getMessage());
        } catch (AdapterNonExistent e) {
            log.fatal(e.getMessage());
        }
        if (retval == null) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "AoSession::createBlob()");
        }
        log.debug("Exit AoSessionImpl::createBlob()");
        return retval;
    }

    /**
	 * 
	 * @return
	 */
    private ApplElem[] getApplElems() {
        de.ibk.ods.core.ApplElem[] elems = kernel.getApplicationStructureValue().getApplElems();
        ApplElem[] retval = new ApplElem[elems.length];
        for (int i = 0; i < retval.length; i++) {
            retval[i] = new ApplElem();
            retval[i].aid = new T_LONGLONG(0, elems[i].getAid());
            retval[i].aeName = elems[i].getAename();
            retval[i].beName = elems[i].getBename();
            de.ibk.ods.core.ApplAttr[] attrs = elems[i].getAttributes();
            retval[i].attributes = new ApplAttr[attrs.length];
            for (int ii = 0; ii < attrs.length; ii++) {
                retval[i].attributes[ii] = new ApplAttr(new String(attrs[ii].getAaName()), new String(attrs[ii].getBaName()), attrs[ii].getDType(), attrs[ii].getLength(), attrs[ii].isObligatory(), attrs[ii].isUnique(), new T_LONGLONG(0, attrs[ii].getUnitId()));
            }
        }
        return retval;
    }

    /**
	 * 
	 * @return
	 */
    private ApplRel[] getApplRels() {
        de.ibk.ods.core.ApplRel[] relations = kernel.getApplicationStructureValue().getApplRels();
        ApplRel[] retval = new ApplRel[relations.length];
        for (int i = 0; i < relations.length; i++) {
            RelationRange relrange = relations[i].getArrelationrange();
            RelationRange irelrange = relations[i].getInvrelationrange();
            retval[i] = new ApplRel(new T_LONGLONG(0, relations[i].getElem1()), new T_LONGLONG(0, relations[i].getElem2()), relations[i].getArname(), relations[i].getInvname(), relations[i].getBrname(), relations[i].getInvbrname(), relations[i].getArrelationtype(), new RelationRange(relrange.min, relrange.max), new RelationRange(irelrange.min, relrange.max));
        }
        return retval;
    }
}
