package eu.ict.persist.ThirdPartyServices.impl.TemperatureSensorService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.personalsmartspace.psm.groupmgmt.api.pss3p.IPssGroupMembershipEvaluator;
import org.personalsmartspace.pss_sm_api.impl.PssService;
import org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier;
import org.personalsmartspace.sre.api.pss3p.PssServiceIdentifier;
import eu.ict.persist.ThirdPartyServices.api.TemperatureSensorService.ITemperatureService;

/**
 * This class is a Membership evaluator that relies on a Temperature Sensor Service
 * 
 * @scr.component label="Temperature Service Evaluator" factory="factory.mer.atomic.temperature"
 * @scr.service interface="org.personalsmartspace.psm.groupmgmt.api.pss3p.IPssGroupMembershipEvaluator"
 */
public class TemperatureSensorEvaluator implements IPssGroupMembershipEvaluator {

    /**
     * Component Factory Service
     * 
     * @scr.reference cardinality="0..1"  bind="setFactory" unbind="unsetFactory"
     *   target="(component.factory=factory.mer.atomic.temperature)"
     */
    private ComponentFactory componentFactory = null;

    protected synchronized void unsetFactory(ComponentFactory newFactory) {
        if (this.componentFactory == newFactory) this.componentFactory = null;
    }

    protected synchronized void setFactory(ComponentFactory newFactory) {
        this.componentFactory = newFactory;
    }

    /**
     * Temperature Sensor Service instance
     * 
     * @scr.reference cardinality="1..1"  bind="setTService" unbind="unsetTService"     *   
     */
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
     * It is not mandatory, so it has to be checked against "null" before use.
     * @scr.reference cardinality="0..1" policy="dynamic" bind="setLogService"
     *                unbind="unsetLogService"
     */
    protected LogService logService = null;

    protected synchronized void unsetLogService(LogService logService) {
        this.logService = null;
    }

    protected synchronized void setLogService(LogService logService) {
        this.logService = logService;
    }

    /** This method is called when this component is activated.
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
    public boolean evaluate() {
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
}
