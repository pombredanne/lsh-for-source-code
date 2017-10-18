package eu.ict.persist.ThirdPartyServices.TempSensor.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.log.LogService;
import org.personalsmartspace.psm.groupmgmt.api.pss3p.IPssGroupMembershipEvaluator;
import org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier;
import eu.ict.persist.ThirdPartyServices.TempSensor.api.ITemperatureService;

/**
 * This class is a Membership evaluator that relies on a Temperature Sensor Service
 */
@Component(name = "Temperature Service Evaluator", factory = TemperatureSensorEvaluator.cfPropertyValue)
@Service
public class TemperatureSensorEvaluator implements IPssGroupMembershipEvaluator {

    @Property(value = TemperatureSensorEvaluator.cfPropertyValue)
    static final String cfPropertyName = ComponentConstants.COMPONENT_FACTORY;

    static final String cfPropertyValue = "factory.mer.atomic.temperature";

    /**
     * Component Factory Service
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "setFactory", unbind = "unsetFactory", target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + TemperatureSensorEvaluator.cfPropertyValue + ")")
    private ComponentFactory componentFactory = null;

    protected synchronized void unsetFactory(ComponentFactory newFactory) {
        if (this.componentFactory == newFactory) this.componentFactory = null;
    }

    protected synchronized void setFactory(ComponentFactory newFactory) {
        this.componentFactory = newFactory;
    }

    /**
     * Temperature Sensor Service instance
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setTService", unbind = "unsetTService")
    private ITemperatureService tService = null;

    protected synchronized void unsetTService(ITemperatureService newTService) {
        if (this.tService == newTService) this.tService = null;
    }

    protected synchronized void setTService(ITemperatureService newTService) {
        this.tService = newTService;
    }

    public ComponentFactory getMerFactory() {
        return this.componentFactory;
    }

    private String merId = "";

    private String uuId = UUID.randomUUID().toString();

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The LogService injected instance.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "setLogService", unbind = "unsetLogService")
    protected LogService logService = null;

    protected synchronized void unsetLogService(LogService logService) {
        this.logService = null;
    }

    protected synchronized void setLogService(LogService logService) {
        this.logService = logService;
    }

    /** 
     * This method is called when this component is activated.
     * Retrieves the persisted preferences from the OSGi container.
     */
    protected void activate(ComponentContext context) {
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Activating Temperature Sensor Evaluator");
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
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Deactivating Temperature Service Evaluator");
        }
    }

    @Override
    public boolean evaluate(IDigitalPersonalIdentifier dpi) {
        return this.tService.getThreshold() <= this.tService.getTemperature();
    }

    @Override
    public String getMerDescription() {
        return "Polled temperature must be greater than or equal to " + this.tService.getThreshold();
    }

    @Override
    public String getMerId() {
        return this.merId;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void setMerDescription(String arg0) {
    }

    @Override
    public void setMerId(String arg0) {
    }

    @Override
    public String getFactoryId() {
        return TemperatureSensorEvaluator.cfPropertyValue;
    }
}
