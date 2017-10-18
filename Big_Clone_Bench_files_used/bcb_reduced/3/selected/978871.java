package eu.ict.persist.ThirdPartyServices.RoleMatcher.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.personalsmartspace.cm.api.pss3p.ContextException;
import org.personalsmartspace.cm.broker.api.pss3p.ICtxBroker;
import org.personalsmartspace.cm.model.api.pss3p.CtxAttributeTypes;
import org.personalsmartspace.cm.model.api.pss3p.CtxModelType;
import org.personalsmartspace.cm.model.api.pss3p.ICtxAttribute;
import org.personalsmartspace.cm.model.api.pss3p.ICtxIdentifier;
import org.personalsmartspace.log.impl.PSSLog;
import org.personalsmartspace.psm.groupmgmt.api.pss3p.IPssGroupMembershipEvaluator;
import org.personalsmartspace.pss_psm_pssmanager.api.platform.IPssManager;
import org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier;

/**
 * This class implements a Membership Evaluation Rule that is satisfied whenever the role
 * of the requester is the same as the role of the requestee.
  */
@Component(name = "Role Matcher", factory = RoleMatcherServiceImpl.cfPropertyValue)
@Service(value = IPssGroupMembershipEvaluator.class)
public class RoleMatcherServiceImpl implements IPssGroupMembershipEvaluator {

    public RoleMatcherServiceImpl() {
        super();
        if (this.componentFactory != null) {
            this.componentFactory.newInstance(null);
        }
    }

    static final String cfPropertyValue = "factory.mer.atomic.role";

    private static final String roleAttributeType = CtxAttributeTypes.ROLE;

    private String myRole = "Unknown";

    private String uuId = UUID.randomUUID().toString();

    private static final long serialVersionUID = 1L;

    /**
     * Component Factory Service
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "setFactory", unbind = "unsetFactory", target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + RoleMatcherServiceImpl.cfPropertyValue + ")")
    private ComponentFactory componentFactory = null;

    protected synchronized void unsetFactory(ComponentFactory newFactory) {
        if (this.componentFactory == newFactory) this.componentFactory = null;
    }

    protected synchronized void setFactory(ComponentFactory newFactory) {
        this.componentFactory = newFactory;
    }

    /**
     * The Log Service instance.
     * 
     */
    protected Log logService = new PSSLog(this);

    /** The public DPI of my PSS */
    private IDigitalPersonalIdentifier myPublicDPI = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.DYNAMIC)
    protected ICtxBroker ctxBroker = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.DYNAMIC)
    private IPssManager pssMgr = null;

    private String merId = "";

    /** This method is called when this component is activated.
     * Retrieves the persisted preferences from the OSGi container.
     */
    protected void activate(ComponentContext context) {
        this.myPublicDPI = this.pssMgr.getDefaultDPI();
        try {
            List<ICtxIdentifier> ctxIds = this.ctxBroker.lookup(this.myPublicDPI, this.myPublicDPI, CtxModelType.ATTRIBUTE, RoleMatcherServiceImpl.roleAttributeType);
            if (ctxIds.size() > 0) {
                ICtxAttribute roleAttr = (ICtxAttribute) this.ctxBroker.retrieve(this.myPublicDPI, ctxIds.get(0));
                this.myRole = roleAttr.getStringValue();
                if (logService.isDebugEnabled()) logService.debug("Retrieving my role: " + this.myRole);
            }
        } catch (ContextException e1) {
            this.logService.error("Unable to retrieve the 'Role' attribute for the current PSS. Cause:" + e1.getLocalizedMessage());
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            byte[] resultingBytes = md5.digest(this.uuId.getBytes());
            this.merId = Base64.encodeBase64URLSafeString(resultingBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    protected void deactivate(ComponentContext context) {
    }

    @Override
    public ComponentFactory getMerFactory() {
        return this.componentFactory;
    }

    @Override
    public boolean evaluate(IDigitalPersonalIdentifier targetDPI) {
        boolean result = false;
        String otherRoleStringValue = null;
        if (logService.isDebugEnabled()) logService.debug("Evaluating RoleMatcher for: " + targetDPI);
        if (!this.isValid()) return false;
        try {
            List<ICtxIdentifier> ctxIds = this.ctxBroker.lookup(this.myPublicDPI, this.myPublicDPI, CtxModelType.ATTRIBUTE, RoleMatcherServiceImpl.roleAttributeType);
            if (ctxIds.size() > 0) {
                ICtxAttribute roleAttr = (ICtxAttribute) this.ctxBroker.retrieve(this.myPublicDPI, ctxIds.get(0));
                this.myRole = roleAttr.getStringValue();
                if (logService.isDebugEnabled()) logService.debug("Retrieving my role: " + this.myRole);
            }
        } catch (ContextException e1) {
            this.logService.error("Unable to retrieve the 'Role' attribute for the current PSS. Cause:" + e1.getLocalizedMessage());
        }
        try {
            List<ICtxIdentifier> ctxIds = this.ctxBroker.lookup(this.myPublicDPI, targetDPI, CtxModelType.ATTRIBUTE, RoleMatcherServiceImpl.roleAttributeType);
            if (ctxIds.size() > 0) {
                ICtxAttribute otherRoleAttr = (ICtxAttribute) this.ctxBroker.retrieve(this.myPublicDPI, ctxIds.get(0));
                if (otherRoleAttr != null) {
                    otherRoleStringValue = otherRoleAttr.getStringValue();
                    if (logService.isDebugEnabled()) logService.debug("Role of " + targetDPI + " is " + otherRoleStringValue);
                } else {
                    this.logService.warn("Unable to retrieve the 'Role' attribute from DPI " + targetDPI.toString() + ". Null value returned.");
                }
            }
        } catch (ContextException e1) {
            this.logService.error("Unable to retrieve the 'Role' attribute from DPI " + targetDPI.toString() + ". Cause:" + e1.getLocalizedMessage());
        }
        if (otherRoleStringValue != null) {
            result = this.myRole.equals(otherRoleStringValue);
            if (logService.isDebugEnabled()) logService.debug("My role is " + this.myRole + " and the other role is " + otherRoleStringValue + ", so this MER returns : " + result);
        }
        return result;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getFactoryId() {
        return RoleMatcherServiceImpl.cfPropertyValue;
    }

    @Override
    public String getMerDescription() {
        return "Role Matcher Rule";
    }

    @Override
    public String getMerId() {
        return this.merId;
    }

    @Override
    public void setMerDescription(String arg0) {
    }

    @Override
    public void setMerId(String arg0) {
    }
}
