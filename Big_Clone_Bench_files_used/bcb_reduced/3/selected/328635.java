package eu.ict.persist.ThirdPartyServices.impl.TemperatureSensorService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.personalsmartspace.pm.prefmodel.api.pss3p.Action;
import org.personalsmartspace.pss_sm_api.impl.PssService;
import org.personalsmartspace.spm.identity.api.platform.DigitalPersonalIdentifier;
import org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier;
import org.personalsmartspace.sre.api.pss3p.PssServiceIdentifier;
import org.personalsmartspace.ui.uim.api.pss3p.IUserInteractionMonitor;
import eu.ict.persist.ThirdPartyServices.api.TemperatureSensorService.ITemperatureService;

/**
 * This class is an implementation of the ITemperatureService interface.
 * It is a test-bed for OSGi concepts and specifications.
 * It currently uses:
 * 1) Declarative Services spec
 * 2) Maven-scr plugin annotations
 * 3) Preferences Service Specification (Section 106 of the Compendium Specs) for persisting values
 * @scr.component label="Temperature Service Impl"
 * @scr.service interface="eu.ict.persist.ThirdPartyServices.api.TemperatureSensorService.ITemperatureService"
 * @scr.service interface="org.personalsmartspace.pss_sm_api.impl.PssService"
 * @scr. service interface="org.personalsmartspace.sre.slm.api.pss3p.callback.IPrivacyAware"
 */
public class TemperatureSensorServiceImpl extends PssService implements ITemperatureService {

    /** This is just to hold the DPI value */
    private IDigitalPersonalIdentifier currentDpi = null;

    public TemperatureSensorServiceImpl() {
        super(new PssServiceIdentifier("TemperatureServiceImpl", "test"), "OSGI", "ontologyDescriptionURI", "serviceURI");
    }

    /**
     * User Interaction Monitor
     * 
     * @scr.reference cardinality="0..1"  bind="setUim" unbind="unsetUim"   
     */
    private IUserInteractionMonitor uim = null;

    protected synchronized void unsetUim(IUserInteractionMonitor newUim) {
        this.uim = null;
    }

    protected synchronized void setUim(IUserInteractionMonitor newUim) {
        this.uim = newUim;
    }

    /** A reference to the preferences for this bundle*/
    private Preferences bundlePrefs = null;

    /** The bundle's symbolic name, used to retrieve its preferences*/
    private String bundleSymbolicName = "";

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

    /**
     * The preferences service instance. 
     * It is mandatory, so no need for "null" reference checking.
     * @scr.reference cardinality="1..1" bind="setPreferencesService"
     *                unbind="unsetPreferencesService"
     */
    protected PreferencesService prefService = null;

    /**
     * The average temperature property name
     * 
     * @scr.property value="15.0"
     *               description="The average temperature"
     *               type="Double"
     * */
    static final String AVG_PROPERTY_NAME = "average";

    /**
     * Average default value.
     */
    protected double average = 15.0;

    /**
     * The range of temperatures property name
     * 
     * @scr.property value="2.0"
     *               description="The range for temperatures"
     *               type="Double"
     * */
    static final String RANGE_PROPERTY_NAME = "range";

    /**
     * Default Range
     */
    protected double range = 2.0;

    /**
     * The threshold temperature property name
     * 
     * @scr.property value="15.0"
     *               description="The average temperature"
     *               type="Double"
     * */
    static final String THRESHOLD_PROPERTY_NAME = "threshold";

    /**
     * Threshold temperature value (for MER evaluation).
     */
    protected double threshold = 14.0;

    /**
     * The service PID property for ConfigAdmin purposes
     * 
     * @scr.property value="eu.ict.persist.ThirdPartyServices.impl.TemperatureSensorService.TemperatureSensorServiceImpl"
     */
    static final String SERVICE_PID_NAME = Constants.SERVICE_PID;

    protected synchronized void unsetLogService(LogService logService) {
        this.logService = null;
    }

    protected synchronized void setLogService(LogService logService) {
        this.logService = logService;
    }

    protected synchronized void unsetPreferencesService(PreferencesService prefService) {
        this.prefService = null;
    }

    protected synchronized void setPreferencesService(PreferencesService prefService) {
        this.prefService = prefService;
    }

    /** Random number generator. Used in the getTemperature method.*/
    private Random r = new Random(System.currentTimeMillis());

    public double getTemperature() {
        this.setAction("GetTemperature");
        return (range * Math.sin(r.nextLong()) + average);
    }

    /** This method is called when this component is activated.
     * Retrieves the persisted preferences from the OSGi container.
     */
    protected void activate(ComponentContext context) {
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Updating TemperatureSensorService Configuration");
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            byte[] resultingBytes = md5.digest(this.uuId.getBytes());
            this.merId = Base64.encodeBase64URLSafeString(resultingBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        bundleSymbolicName = context.getBundleContext().getBundle().getSymbolicName();
        Preferences pref = this.prefService.getSystemPreferences();
        bundlePrefs = pref.node(bundleSymbolicName);
        this.setAverage(bundlePrefs.getDouble(TemperatureSensorServiceImpl.AVG_PROPERTY_NAME, this.average));
        this.setRange(bundlePrefs.getDouble(TemperatureSensorServiceImpl.RANGE_PROPERTY_NAME, this.range));
        this.setThreshold(bundlePrefs.getDouble(TemperatureSensorServiceImpl.THRESHOLD_PROPERTY_NAME, this.threshold));
    }

    protected void deactivate(ComponentContext context) {
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Deactivating Temperature Service Component");
        }
    }

    private void sendAction(Action a) {
        DigitalPersonalIdentifier dpi = new DigitalPersonalIdentifier();
        if (this.uim != null) {
            this.uim.monitor(this.getServiceId(), this.getServiceType(), dpi, a);
        }
    }

    private void setAction(String param, double value) {
        Action a = new Action(param, String.valueOf(value));
        this.sendAction(a);
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Sending Action. Parameter Name=" + param + ", Parameter Value=" + String.valueOf(value));
        }
        ;
    }

    private void setAction(String param) {
        Action a = new Action(param, null);
        this.sendAction(a);
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Sending Action. Parameter Name=" + param + ", Parameter Value= null");
        }
        ;
    }

    /** Change the value of the 'Average' property at runtime, and persist it. */
    public void setAverage(double newAverage) {
        this.average = newAverage;
        bundlePrefs.putDouble(AVG_PROPERTY_NAME, this.average);
        try {
            bundlePrefs.sync();
        } catch (BackingStoreException e) {
            if (this.logService != null) {
                this.logService.log(LogService.LOG_WARNING, "Unable to persist preferences! " + e.getLocalizedMessage());
            }
            ;
        }
        this.setAction("SetAverage", newAverage);
    }

    /** Change the value of the 'Range' property at runtime, and persist it. */
    public void setRange(double newRange) {
        this.range = newRange;
        bundlePrefs.putDouble(RANGE_PROPERTY_NAME, this.range);
        try {
            bundlePrefs.sync();
        } catch (BackingStoreException e) {
            if (this.logService != null) {
                this.logService.log(LogService.LOG_WARNING, "Unable to persist preferences! " + e.getLocalizedMessage());
            }
            ;
        }
        this.setAction("SetRange", newRange);
    }

    /** Change the value of the 'threshold' property at runtime, and persist it. */
    public void setThreshold(double newThreshold) {
        this.threshold = newThreshold;
        bundlePrefs.putDouble(THRESHOLD_PROPERTY_NAME, this.threshold);
        try {
            bundlePrefs.sync();
        } catch (BackingStoreException e) {
            if (this.logService != null) {
                this.logService.log(LogService.LOG_WARNING, "Unable to persist preferences! " + e.getLocalizedMessage());
            }
            ;
        }
        this.setAction("SetThreshold", newThreshold);
    }

    /** Change the value of the 'threshold' property at runtime, and persist it. */
    public double getThreshold() {
        return this.threshold;
    }

    /** Playing with this method. Should be invoked when a service property
     * is changed, according to the 'Configuration Admin' Service Specification (from compendium).*/
    public synchronized void updated(Dictionary<String, Object> np) {
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Updating");
        }
        ;
    }

    @Override
    public String getServiceDescription() {
        return "A service for polling temperature from a sensor";
    }

    @Override
    public String getServiceType() {
        return "Service Type Here";
    }

    @Override
    public String getVersionNumber() {
        return "Version Number Here";
    }
}
