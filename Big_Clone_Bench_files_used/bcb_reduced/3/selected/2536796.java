package eu.ict.persist.ThirdPartyServices.TempSensor.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.personalsmartspace.pm.prefmodel.api.pss3p.Action;
import org.personalsmartspace.sre.api.pss3p.IDigitalPersonalIdentifier;
import org.personalsmartspace.sre.api.pss3p.IServiceIdentifier;
import org.personalsmartspace.sre.slm.api.pss3p.callback.IService;
import org.personalsmartspace.ui.uim.api.pss3p.IUserInteractionMonitor;
import eu.ict.persist.ThirdPartyServices.TempSensor.api.ITemperatureService;

/**
 * This class is an implementation of the ITemperatureService interface.
 * It is a test-bed for OSGi concepts and specifications.
 * It currently uses:
 * 1) Declarative Services spec
 * 2) Maven-scr plugin annotations
 * 3) Preferences Service Specification (Section 106 of the Compendium Specs) for persisting values
  */
@Component(name = "Temperature Service Impl", immediate = true)
@Services(value = { @Service(value = ITemperatureService.class), @Service(value = IService.class) })
@Properties({ @Property(name = Constants.SERVICE_PID, value = TemperatureSensorServiceImpl.SERVICE_PID_VALUE), @Property(name = TemperatureSensorServiceImpl.AVG_PROPERTY_NAME, doubleValue = 15.0, description = "The average temperature"), @Property(name = TemperatureSensorServiceImpl.RANGE_PROPERTY_NAME, doubleValue = 2.0, description = "The range for polled temperatures"), @Property(name = TemperatureSensorServiceImpl.THRESHOLD_PROPERTY_NAME, doubleValue = 15.0, description = "The threshold for membership evaluation") })
public class TemperatureSensorServiceImpl implements ITemperatureService, IService {

    private IServiceIdentifier sid = null;

    /**
     * User Interaction Monitor 
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private IUserInteractionMonitor uim = null;

    /** A reference to the preferences for this bundle*/
    private Preferences bundlePrefs = null;

    /** The bundle's symbolic name, used to retrieve its preferences*/
    private String bundleSymbolicName = "";

    private String merId = "";

    private String uuId = UUID.randomUUID().toString();

    private IDigitalPersonalIdentifier consumerDPI = null;

    private IDigitalPersonalIdentifier providerDPI = null;

    private String sessionId = null;

    private static final long serialVersionUID = 1L;

    /**
     * The LogService injected instance.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    protected LogService logService = null;

    /**
     * The preferences service instance. 
     * It is mandatory, so no need for "null" reference checking.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PreferencesService prefService = null;

    /**
     * The average temperature property name
     */
    static final String AVG_PROPERTY_NAME = "average";

    /**
     * Average default value.
     */
    protected double average = 15.0;

    /**
     * The range of temperatures property name
     */
    static final String RANGE_PROPERTY_NAME = "range";

    /**
     * Default Range
     */
    protected double range = 2.0;

    /**
     * The threshold temperature property name
     */
    static final String THRESHOLD_PROPERTY_NAME = "threshold";

    /**
     * Threshold temperature value (for MER evaluation).
     */
    protected double threshold = 14.0;

    /**
     * The service PID property for ConfigAdmin purposes
     */
    static final String SERVICE_PID_VALUE = "eu.ict.persist.ThirdPartyServices.impl.TemperatureSensorService.TemperatureSensorServiceImpl";

    /** Random number generator. Used in the getTemperature method.*/
    private Random r = new Random(System.currentTimeMillis());

    public double getTemperature() {
        this.setAction("GetTemperature");
        double result = (range * Math.sin(r.nextLong()) + average);
        if (this.logService != null) {
            this.logService.log(LogService.LOG_DEBUG, "Polled Temperature is:'" + result + "'");
        }
        ;
        return result;
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
        if (this.uim != null && this.consumerDPI != null) {
            this.uim.monitor(this.getID(), this.getClass().getName(), this.consumerDPI, a);
        }
    }

    private void setAction(String param, double value) {
        Action a = new Action(param, String.valueOf(value));
        a.setServiceID(sid);
        a.setServiceType(ITemperatureService.class.getSimpleName());
        this.sendAction(a);
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Sending Action. Parameter Name=" + param + ", Parameter Value=" + String.valueOf(value));
        }
        ;
    }

    private void setAction(String param) {
        Action a = new Action(param, "");
        a.setServiceID(sid);
        a.setServiceType(ITemperatureService.class.getSimpleName());
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
    public IServiceIdentifier getID() {
        return this.sid;
    }

    @Override
    public void setID(IServiceIdentifier iserviceidentifier) {
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Setting the Temperature Service Id to:'" + iserviceidentifier + "'");
        }
        ;
        this.sid = iserviceidentifier;
    }

    @Override
    public boolean startUserSession(String sid, IDigitalPersonalIdentifier consumerDPI, IDigitalPersonalIdentifier publicProviderDPI) {
        boolean success = true;
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Starting Temperature Service Session with DPI:'" + consumerDPI + "'");
        }
        ;
        this.consumerDPI = consumerDPI;
        this.providerDPI = publicProviderDPI;
        this.sessionId = sid;
        return success;
    }

    @Override
    public boolean stopUserSession(String sid, IDigitalPersonalIdentifier consumerDPI, IDigitalPersonalIdentifier publicProviderDPI) {
        boolean success = true;
        if (this.logService != null) {
            this.logService.log(LogService.LOG_INFO, "Stopping Temperature Service Session with DPI:'" + consumerDPI + "'");
        }
        ;
        this.consumerDPI = consumerDPI;
        this.providerDPI = publicProviderDPI;
        this.sessionId = sid;
        return success;
    }

    @Override
    public void setControllerDPI(IDigitalPersonalIdentifier arg0) {
    }
}
