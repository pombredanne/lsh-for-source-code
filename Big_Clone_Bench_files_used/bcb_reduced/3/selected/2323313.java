package openjsip.proxy;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.NDC;
import javax.sip.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.sip.header.*;
import javax.sip.address.AddressFactory;
import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.address.SipURI;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import openjsip.remote.locationservice.UserNotFoundException;
import openjsip.remote.locationservice.LocationServiceInterface;
import openjsip.remote.RemoteServiceInterface;
import openjsip.SipUtils;
import openjsip.snmp.SNMPAssistant;
import openjsip.auth.DigestServerAuthenticationMethod;
import openjsip.proxy.plugins.MethodPlugin;
import openjsip.proxy.plugins.MethodPluginException;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.message.SIPResponse;
import snmp.*;

public class Proxy extends UnicastRemoteObject implements SipListener, RemoteServiceInterface, Runnable {

    /**
     * Logger
     */
    private static Logger log = Logger.getLogger(Proxy.class);

    /**
     * Main SIP stack
     */
    private SipStack sipStack;

    /**
     * Factory that constructs address fields
     */
    private AddressFactory addressFactory;

    /**
     * Factory that constructs headers
     */
    private HeaderFactory headerFactory;

    /**
     * Factory that constructs entire SIP messages
     */
    private MessageFactory messageFactory;

    /**
     * Set of responsible domains
     */
    private final HashSet<String> domains = new HashSet<String>();

    /**
     * Method plugins, such as REGISTER
     */
    private final Hashtable<String, MethodPlugin> methodPlugins = new Hashtable<String, MethodPlugin>();

    /**
     * SipProvider to ip address mapping
     * SipProvider is an interface, and Address is its IP address
     */
    private final Hashtable<SipProvider, String> providerToAddressMapping = new Hashtable<SipProvider, String>();

    /**
     * SipProvider to hostname mapping
     * SipProvider is an interface, and Hostname is its FQDN ( Fully qualified domain name )
     */
    private final Hashtable<SipProvider, String> providerToHostnameMapping = new Hashtable<SipProvider, String>();

    /**
     * Location service connection variables
     */
    private String locationServiceName;

    private String locationServiceHost;

    private int locationServicePort = 1099;

    /**
     * See RFC3261 for Timer C details
     */
    private int timercPeriod = 3 * 60 * 1000 + 1000;

    /**
     *  Authenticate subscribers ?
     */
    private boolean authenticationEnabled;

    /**
     * Digest authentication class
     */
    private DigestServerAuthenticationMethod dsam;

    /**
     * Operation mode
     */
    public static final int STATEFULL_MODE = 0;

    public static final int STATELESS_MODE = 1;

    /**
     * Not implemented yet
     */
    private final int operationMode;

    /**
     * RMI binding name
     */
    private static String RMIBindName;

    /**
     * SNMP agent engine
     */
    private SNMPv1AgentInterface agentInterface;

    /**
     * Additional class to ease the work with SNMP.
     */
    private SNMPAssistant snmpAssistant;

    /**
     * SNMP root oid. This is where all our objects reside.
     * The current value corresponds to .iso.org.dod.internet.private.enterprises.
     * 1937 is our random generated value for (OpenJSIP), but normally this number is to be
     * given by IANA.
     * The next value correspond to:
     * 1 - OpenJSIP Location service
     * 2 - OpenJSIP Registrar service
     * 3 - OpenJSIP Proxy service
     */
    protected static final String SNMP_ROOT_OID = "1.3.6.1.4.1.1937.3.";

    protected static final String SNMP_OID_NUM_REQUESTS_PROCESSED = SNMP_ROOT_OID + "1.1";

    protected static final String SNMP_OID_NUM_RESPONSES_PROCESSED = SNMP_ROOT_OID + "1.2";

    protected static final String SNMP_OID_NUM_REQUEST_PROCESSING_ERRORS = SNMP_ROOT_OID + "1.3";

    protected static final String SNMP_OID_NUM_RESPONSE_PROCESSING_ERRORS = SNMP_ROOT_OID + "1.4";

    protected static final String SNMP_OID_NUM_SERVER_TRANSACTIONS = SNMP_ROOT_OID + "1.5";

    protected static final String SNMP_OID_NUM_CLIENT_TRANSACTIONS = SNMP_ROOT_OID + "1.6";

    /**
     * SNMP database with default values.
     */
    private static final Object SNMP_DATABASE[][] = new Object[][] { { SNMP_OID_NUM_REQUESTS_PROCESSED, new SNMPCounter32(0) }, { SNMP_OID_NUM_RESPONSES_PROCESSED, new SNMPCounter32(0) }, { SNMP_OID_NUM_REQUEST_PROCESSING_ERRORS, new SNMPCounter32(0) }, { SNMP_OID_NUM_RESPONSE_PROCESSING_ERRORS, new SNMPCounter32(0) }, { SNMP_OID_NUM_SERVER_TRANSACTIONS, new SNMPGauge32(0) }, { SNMP_OID_NUM_CLIENT_TRANSACTIONS, new SNMPGauge32(0) } };

    /**
     * Entry point
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Properties props = null;
        if (args.length < 1) {
            printUsage();
            System.exit(0);
        }
        try {
            if (!new File(args[0]).exists()) {
                System.err.println("Error: Cannot open configuration file " + args[0]);
                System.exit(1);
            }
            props = new Properties();
            props.load(new FileInputStream(args[0]));
            String externalLoggingConf = props.getProperty("logging.properties");
            if (externalLoggingConf != null) PropertyConfigurator.configure(externalLoggingConf.trim()); else PropertyConfigurator.configure(props);
        } catch (IOException e) {
            System.err.println("Error: Cannot open configuration file " + args[0]);
            System.exit(1);
        }
        RemoteServiceInterface proxy = null;
        try {
            proxy = new Proxy(props);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            if (log.isTraceEnabled()) log.trace("", ex);
            System.exit(1);
        }
        String name = props.getProperty("proxy.service.rmi.objectname", "Proxy").trim();
        String host = props.getProperty("proxy.service.rmi.host", "localhost").trim();
        int port = 1099;
        try {
            port = Integer.parseInt(props.getProperty("proxy.rmi.port", "1099").trim());
        } catch (NumberFormatException ex) {
        }
        RMIBindName = "rmi://" + host + ":" + port + "/" + name;
        try {
            Naming.rebind(RMIBindName, proxy);
        } catch (RemoteException ex) {
            log.error("Cannot register within RMI registry at " + host + ":" + port, ex);
            System.exit(1);
        } catch (MalformedURLException ex) {
            log.error("Cannot register within RMI registry at " + host + ":" + port, ex);
            System.exit(1);
        }
        if (log.isInfoEnabled()) log.info("Proxy registered as \"" + name + "\" within RMI registry at " + host + ":" + port);
        if (log.isInfoEnabled()) log.info("Proxy started...");
    }

    /**
     * Prints help on how to launch this program
     */
    private static void printUsage() {
        System.out.println("\nUsage: Proxy <proxy.properties file>\n" + "where proxy.properties is the path to .properties file with settings for Proxy server.");
    }

    /**
     * Proxy constructor
     * @param props Proxy server configuration
     * @throws PeerUnavailableException
     * @throws ObjectInUseException
     * @throws TooManyListenersException
     * @throws TransportNotSupportedException
     * @throws InvalidArgumentException
     */
    private Proxy(Properties props) throws PeerUnavailableException, ObjectInUseException, TooManyListenersException, TransportNotSupportedException, InvalidArgumentException, RemoteException {
        if (log.isInfoEnabled()) log.info("Starting Proxy v" + SipUtils.OPENJSIP_VERSION + "...");
        if (System.getSecurityManager() == null) System.setSecurityManager(new SecurityManager());
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        props.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "off");
        sipStack = sipFactory.createSipStack(props);
        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();
        messageFactory = sipFactory.createMessageFactory();
        if (log.isInfoEnabled()) log.info("Configuring interfaces...");
        try {
            if (props.getProperty("proxy.interface.1.addr") == null) {
                int index = 1;
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface netInt = nets.nextElement();
                    Enumeration<InetAddress> inetAdresses = netInt.getInetAddresses();
                    while (inetAdresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAdresses.nextElement();
                        props.setProperty("proxy.interface." + index + ".addr", inetAddress.getHostAddress());
                        index++;
                    }
                }
            }
        } catch (SocketException e) {
        }
        int index = 1;
        String proxyHost = null;
        while ((proxyHost = props.getProperty("proxy.interface." + index + ".addr")) != null) {
            proxyHost = proxyHost.trim();
            try {
                if (log.isTraceEnabled()) log.trace("Configuring interface #" + index + ": " + proxyHost);
                InetAddress inetAddress = InetAddress.getByName(proxyHost);
                String proxyPortStr = props.getProperty("proxy.interface." + index + ".port");
                if (proxyPortStr != null) proxyPortStr = proxyPortStr.trim();
                int proxyPort = 5060;
                try {
                    proxyPort = Integer.parseInt(proxyPortStr);
                } catch (NumberFormatException e) {
                }
                String[] transports = props.getProperty("proxy.interface." + index + ".transport", "udp, tcp").split(",");
                SipProvider sipProvider = null;
                for (int i = 0; i < transports.length; i++) {
                    transports[i] = transports[i].trim();
                    try {
                        if (log.isTraceEnabled()) log.trace("Creating ListeningPoint for " + inetAddress.getHostAddress() + ":" + proxyPort + " " + transports[i]);
                        ListeningPoint lp = sipStack.createListeningPoint(inetAddress.getHostAddress(), proxyPort, transports[i]);
                        if (sipProvider == null) {
                            if (log.isTraceEnabled()) log.trace("Creating new SipProvider.");
                            sipProvider = sipStack.createSipProvider(lp);
                        } else {
                            if (log.isTraceEnabled()) log.trace("Adding ListeningPoint to SipProvider.");
                            sipProvider.addListeningPoint(lp);
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to create listening point " + inetAddress.getHostAddress() + ":" + proxyPort + " " + transports[i] + " ( " + ex.getMessage() + " )");
                        if (log.isTraceEnabled()) log.trace("", ex);
                    }
                }
                if (sipProvider != null && sipProvider.getListeningPoints().length > 0) {
                    providerToAddressMapping.put(sipProvider, inetAddress.getHostAddress());
                    providerToHostnameMapping.put(sipProvider, inetAddress.getCanonicalHostName());
                    sipProvider.addSipListener(this);
                }
            } catch (UnknownHostException ex) {
                log.warn("Interface #" + index + ": " + ex.getMessage());
            } finally {
                index++;
            }
        }
        if (providerToHostnameMapping.size() == 0) {
            log.error("There are no properly configured interfaces. Proxy cannot be started.");
            System.exit(1);
        }
        Iterator sipProviders = sipStack.getSipProviders();
        index = 1;
        while (sipProviders.hasNext()) {
            SipProvider sipProvider = (SipProvider) sipProviders.next();
            ListeningPoint[] lps = sipProvider.getListeningPoints();
            StringBuffer sb = new StringBuffer();
            sb.append("Interface #" + index + ": " + getHostname(sipProvider) + " (");
            for (int i = 0; i < lps.length; i++) {
                if (i == 0) sb.append(lps[i].getIPAddress() + ") via ");
                sb.append(lps[i].getTransport());
                if (i < lps.length - 1) sb.append(", ");
            }
            if (log.isInfoEnabled()) log.info(sb.toString());
            index++;
        }
        locationServiceName = props.getProperty("proxy.location.service.rmi.objectname", "LocationService").trim();
        locationServiceHost = props.getProperty("proxy.location.service.rmi.host", "localhost").trim();
        try {
            locationServicePort = Integer.parseInt(props.getProperty("proxy.location.service.rmi.port", "1099").trim());
        } catch (NumberFormatException ex) {
        }
        if (log.isInfoEnabled()) log.info("Connecting to Location Service server at " + locationServiceHost + ":" + locationServicePort + " ...");
        LocationServiceInterface locationService = getLocationService();
        if (locationService != null && locationService.isAlive()) {
            if (log.isInfoEnabled()) log.info("Successfully connected.");
        } else throw new RemoteException("Cannot connect to Location Service server.");
        String domainsStr = props.getProperty("proxy.domains");
        if (domainsStr != null) {
            String[] domainsArray = domainsStr.trim().split(",");
            for (String domain : domainsArray) {
                domain = domain.trim();
                if (domain.length() > 0) domains.add(domain);
            }
        }
        if (domains.isEmpty()) {
            log.warn("No domains configured. Retreiving the domain list from Location Service...");
            domains.addAll(locationService.getDomains());
        }
        if (domains.isEmpty()) {
            log.error("No domains configured. Proxy cannot be started.");
            System.exit(1);
        }
        if (log.isInfoEnabled()) {
            StringBuffer sb = new StringBuffer();
            Iterator it = domains.iterator();
            while (it.hasNext()) {
                sb.append((String) it.next());
                if (it.hasNext()) sb.append(", ");
            }
            log.info("Proxy is responsible for domains: " + sb.toString());
        }
        authenticationEnabled = props.getProperty("proxy.authentication.enabled", "no").trim().equalsIgnoreCase("yes");
        if (log.isInfoEnabled()) {
            if (authenticationEnabled) log.info("Authentication enabled."); else log.info("Authentication disabled.");
        }
        String operationModeStr = props.getProperty("proxy.operation.mode", "stateless").trim().toLowerCase();
        if (operationModeStr.equals("statefull")) operationMode = STATEFULL_MODE; else operationMode = STATELESS_MODE;
        if (log.isInfoEnabled()) {
            if (operationMode == STATEFULL_MODE) log.info("Proxy operation mode: statefull."); else if (operationMode == STATELESS_MODE) log.info("Proxy operation mode: stateless."); else log.info("Proxy operation mode: unknown.");
        }
        try {
            dsam = new DigestServerAuthenticationMethod(domains.iterator().next(), new String[] { "MD5" });
        } catch (NoSuchAlgorithmException ex) {
            log.error("Cannot create authentication method. Some algorithm is not implemented: " + ex.getMessage());
            if (log.isTraceEnabled()) log.trace(null, ex);
            System.exit(1);
        }
        if (log.isInfoEnabled()) log.info("Loading method plugins...");
        index = 1;
        String pluginClass;
        while ((pluginClass = props.getProperty("proxy.method.plugin." + index + ".classname")) != null) {
            pluginClass = pluginClass.trim();
            String pluginEnabled = props.getProperty("proxy.method.plugin." + index + ".enabled", "true");
            if (!pluginEnabled.equals("true") && !pluginEnabled.equals("yes")) {
                index++;
                continue;
            }
            if (log.isInfoEnabled()) log.info("Loading " + pluginClass);
            try {
                Class c = Class.forName(pluginClass);
                MethodPlugin methodPlugin = (MethodPlugin) c.newInstance();
                String pathToPropertiesFile = props.getProperty("proxy.method.plugin." + index + ".properties");
                Properties pluginProperties = props;
                if (pathToPropertiesFile != null) {
                    pluginProperties = new Properties();
                    pluginProperties.load(new FileInputStream(pathToPropertiesFile.trim()));
                }
                methodPlugin.initialize(pluginProperties, this);
                if (methodPlugin.isInitialized()) methodPlugins.put(methodPlugin.getMethod(), methodPlugin);
            } catch (ClassNotFoundException ex) {
                log.error("Cannot load plugin class.", ex);
            } catch (InstantiationException ex) {
                log.error("Cannot load plugin class.", ex);
            } catch (IllegalAccessException ex) {
                log.error("Cannot load plugin class.", ex);
            } catch (IOException ex) {
                log.error("Cannot load .properties file. " + ex.getMessage());
            } catch (Exception ex) {
                log.error("Plugin failed to initialize. " + ex.getMessage());
            }
            index++;
        }
        boolean isSnmpEnabled = props.getProperty("proxy.snmp.agent.enabled", "yes").trim().equalsIgnoreCase("yes");
        if (isSnmpEnabled) {
            int snmpPort = 1163;
            try {
                snmpPort = Integer.parseInt(props.getProperty("proxy.snmp.agent.port", "1163").trim());
            } catch (NumberFormatException e) {
            }
            String communityName = props.getProperty("proxy.snmp.agent.community", "public").trim();
            snmpAssistant = new SNMPAssistant(communityName, SNMP_DATABASE);
            try {
                agentInterface = new SNMPv1AgentInterface(0, snmpPort, null);
                agentInterface.addRequestListener(snmpAssistant);
                agentInterface.setReceiveBufferSize(5120);
                agentInterface.startReceiving();
                if (log.isInfoEnabled()) log.info("SNMP agent started at port " + snmpPort + " with community " + communityName);
            } catch (SocketException ex) {
                log.error("Cannot start SNMP agent at port " + snmpPort + ": " + ex.getMessage());
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this));
    }

    /**
     * Shutdown hook
     */
    public void run() {
        if (log != null && log.isInfoEnabled()) log.info("Shutting down...");
        try {
            if (agentInterface != null) agentInterface.stopReceiving();
        } catch (SocketException ex) {
        }
        try {
            Naming.unbind(RMIBindName);
        } catch (Exception e) {
        }
    }

    /**
     * Returns remote Location Service instance. Do not cache this instance,
     * because once Location Service restarted, it cannot be contacted without reconnecting.
     * @return Remote Location Service instance.
     */
    public LocationServiceInterface getLocationService() {
        try {
            Registry registry = LocateRegistry.getRegistry(locationServiceHost, locationServicePort);
            LocationServiceInterface locationService = (LocationServiceInterface) registry.lookup(locationServiceName);
            return locationService;
        } catch (RemoteException ex) {
            return null;
        } catch (NotBoundException ex) {
            return null;
        }
    }

    /**
     * @return SNMP assistant.
     */
    public SNMPAssistant getSnmpAssistant() {
        return snmpAssistant;
    }

    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        CallIdHeader callidHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        NDC.push(callidHeader != null ? callidHeader.getCallId() : Long.toString(System.currentTimeMillis()));
        try {
            processIncomingRequest(requestEvent);
            snmpAssistant.incrementSnmpInteger(SNMP_OID_NUM_REQUESTS_PROCESSED);
        } catch (Exception ex) {
            snmpAssistant.incrementSnmpInteger(SNMP_OID_NUM_REQUEST_PROCESSING_ERRORS);
            if (log.isDebugEnabled()) log.debug("Exception: " + ex.getMessage());
            if (log.isTraceEnabled()) log.trace("Exception dump: ", ex);
        }
        NDC.remove();
    }

    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        CallIdHeader callidHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        NDC.push(callidHeader != null ? callidHeader.getCallId() : Long.toString(System.currentTimeMillis()));
        try {
            processIncomingResponse(responseEvent);
            snmpAssistant.incrementSnmpInteger(SNMP_OID_NUM_RESPONSES_PROCESSED);
        } catch (Exception ex) {
            snmpAssistant.incrementSnmpInteger(SNMP_OID_NUM_RESPONSE_PROCESSING_ERRORS);
            if (log.isDebugEnabled()) log.debug("Exception: " + ex.getMessage());
            if (log.isTraceEnabled()) log.trace("Exception dump: ", ex);
        }
        NDC.remove();
    }

    public void processTimeout(TimeoutEvent event) {
        ClientTransaction clientTransaction = event.getClientTransaction();
        if (clientTransaction != null) {
            TransactionsMapping transactionsMapping = (TransactionsMapping) clientTransaction.getApplicationData();
            if (transactionsMapping != null) {
                transactionsMapping.cancelTimerC(clientTransaction);
                checkResponseContext(transactionsMapping);
            }
        }
        if (log.isTraceEnabled()) log.trace("Timeout occured at " + getHostname((SipProvider) event.getSource()) + ". CT = " + event.getClientTransaction() + "  ST = " + event.getServerTransaction());
    }

    public void processIOException(IOExceptionEvent event) {
        if (log.isTraceEnabled()) log.trace("IOException occured at " + getHostname((SipProvider) event.getSource()) + ". Host = " + event.getHost() + "  Port = " + event.getPort() + " Transport = " + event.getTransport());
    }

    public void processTransactionTerminated(TransactionTerminatedEvent event) {
        if (event.isServerTransaction()) snmpAssistant.decrementSnmpInteger(SNMP_OID_NUM_SERVER_TRANSACTIONS); else snmpAssistant.decrementSnmpInteger(SNMP_OID_NUM_CLIENT_TRANSACTIONS);
        if (log.isTraceEnabled()) log.trace("Transaction terminated at " + getHostname((SipProvider) event.getSource()) + ". CT = " + event.getClientTransaction() + "  ST = " + event.getServerTransaction());
        ClientTransaction clientTransaction = event.getClientTransaction();
        if (clientTransaction != null) {
            TransactionsMapping transactionsMapping = (TransactionsMapping) clientTransaction.getApplicationData();
            if (transactionsMapping != null) {
                transactionsMapping.cancelTimerC(clientTransaction);
                checkResponseContext(transactionsMapping);
            }
        }
    }

    /**
     * Cannot be called in proxies
     * @param event DialogTerminatedEvent object
     */
    public void processDialogTerminated(DialogTerminatedEvent event) {
    }

    /**
     * Returns whether proxy is responsible for domain
     * @param domain Domain name. Cannot be IP or hostname. See addrMatchesInterface() function instead.
     * @return true if proxy is responsible for <i>domain</i>, false otherwise.
     */
    public boolean isDomainServed(String domain) {
        return domains.contains(domain);
    }

    public Iterator getSipProviders() {
        return sipStack.getSipProviders();
    }

    public HashSet getDomains() {
        return domains;
    }

    public String getDefaultDomain() {
        return domains.iterator().next();
    }

    public int getOperationMode() {
        return operationMode;
    }

    /**
     * Returns whether <i>addr</i> matches any interface's IP or hostname.
     * @param addr Address. Can be IP or hostname.
     * @return true if <i>addr</i> matches any interface's IP or hostname.
     */
    public boolean addrMatchesInterface(String addr) {
        return getProviderByAddr(addr) != null;
    }

    /**
     * Returns SipProvider instance, whose associated network interface IP or hostname equals <i>addr</i>
     * @param addr Address. Can be IP or hostname.
     * @return returns SipProvider instance, whose associated network interface IP or hostname equals <i>addr</i>
     */
    public SipProvider getProviderByAddr(String addr) {
        Iterator iterator = sipStack.getSipProviders();
        while (iterator.hasNext()) {
            SipProvider sipProvider = (SipProvider) iterator.next();
            if (addr.equals(getIPAddress(sipProvider))) return sipProvider;
            if (addr.equals(getHostname(sipProvider))) return sipProvider;
        }
        return null;
    }

    /**
     * Returns the IP address of proxy interface
     * @param sipProvider SipProvider object
     * @return IP address
     */
    public String getIPAddress(SipProvider sipProvider) {
        return providerToAddressMapping.get(sipProvider);
    }

    /**
     * Returns the FQDN of proxy interface
     * @param sipProvider SipProvider object
     * @return FQDN
     */
    public String getHostname(SipProvider sipProvider) {
        return providerToHostnameMapping.get(sipProvider);
    }

    /**
     * Processes incoming requests and forwards them if necessary
     * @param requestEvent Request event
     * @throws InvalidArgumentException
     * @throws ParseException
     * @throws SipException
     */
    private void processIncomingRequest(final RequestEvent requestEvent) throws InvalidArgumentException, ParseException, SipException {
        Request request = requestEvent.getRequest();
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        ServerTransaction serverTransaction = (operationMode == STATEFULL_MODE ? requestEvent.getServerTransaction() : null);
        String method = request.getMethod();
        if (log.isDebugEnabled()) {
            log.debug("-------------------");
            log.debug("Incoming " + method + " request " + request.getRequestURI().toString().trim());
        }
        if (log.isTraceEnabled()) log.trace("\n" + request.toString());
        LocationServiceInterface locationService = getLocationService();
        if (locationService == null) {
            log.error("Cannot connect to Location Service server. Check if server is running and registered within RMI registry at target host.");
            SipUtils.sendResponse(Response.SERVER_INTERNAL_ERROR, sipProvider, messageFactory, request, serverTransaction);
            return;
        }
        if (!validateRequest(request, sipProvider, serverTransaction, locationService)) {
            if (log.isDebugEnabled()) log.debug("Request is not valid.");
            return;
        }
        URI requestURI = request.getRequestURI();
        SipURI requestSipURI = requestURI.isSipURI() ? (SipURI) requestURI : null;
        if (log.isTraceEnabled()) log.trace("Inspecting request for Strict Routing mechanism...");
        ListIterator routes = request.getHeaders(RouteHeader.NAME);
        if (routes != null && routes.hasNext()) {
            ListeningPoint[] lps = sipProvider.getListeningPoints();
            SipURI sipURI_form1 = addressFactory.createSipURI(null, getIPAddress(sipProvider));
            sipURI_form1.setPort(lps[0].getPort());
            SipURI sipURI_form2 = addressFactory.createSipURI(null, getHostname(sipProvider));
            sipURI_form2.setPort(lps[0].getPort());
            if (requestURI.equals(sipURI_form1) || requestURI.equals(sipURI_form2)) {
                RouteHeader lastRouteHeader;
                do lastRouteHeader = (RouteHeader) routes.next(); while (routes.hasNext());
                request.setRequestURI(lastRouteHeader.getAddress().getURI());
                request.removeLast(RouteHeader.NAME);
                if (log.isDebugEnabled()) log.debug("Strict routing detected ! Request was modified and dispatched again for processing.");
                if (method.equals("BYE")) {
                    int a = 5;
                }
                Thread thread = new Thread() {

                    public void run() {
                        processRequest(requestEvent);
                    }
                };
                thread.start();
                return;
            }
            if (log.isTraceEnabled()) log.trace("Strict routing was not detected, Request-URI is not this proxy");
        } else if (log.isTraceEnabled()) log.trace("Strict routing was not detected, because there are no Route headers.");
        if (requestSipURI != null) {
            if (requestSipURI.getMAddrParam() != null) {
                if (log.isTraceEnabled()) log.trace("Request contains maddr parameter " + requestSipURI.getMAddrParam() + ".");
                if (isDomainServed(requestSipURI.getMAddrParam()) || getProviderByAddr(requestSipURI.getMAddrParam()) != null) {
                    String transport = requestSipURI.getTransportParam();
                    if (transport == null) transport = "udp";
                    int port = requestSipURI.getPort();
                    if (port == -1) port = 5060;
                    ListeningPoint lp = sipProvider.getListeningPoint(transport);
                    if (lp != null && port == lp.getPort()) {
                        if (log.isTraceEnabled()) log.trace("The maddr contains a domain " + requestSipURI.getMAddrParam() + " we are responsible for," + " we remove the mAddr, non-default port and transport parameters from the original request");
                        requestSipURI.removeParameter("maddr");
                        if (requestSipURI.getPort() != 5060 && requestSipURI.getPort() != -1) requestSipURI.removeParameter("port");
                        requestSipURI.removeParameter("transport");
                    } else if (log.isTraceEnabled()) log.trace("The maddr contains a domain " + requestSipURI.getMAddrParam() + " we are responsible for," + " but listening point for specified port and transport cannot be found.");
                } else {
                    if (log.isTraceEnabled()) log.trace("Maddr parameter " + requestSipURI.getMAddrParam() + " is not domain we have to take care. Continue processing the request.");
                }
            } else {
                if (log.isTraceEnabled()) log.trace("Request doesn't contain maddr parameter.");
            }
        } else {
            if (log.isTraceEnabled()) log.trace("RequestURI " + requestURI + " is not SIP URI. Continue processing the request.");
        }
        routes = request.getHeaders(RouteHeader.NAME);
        if (routes != null && routes.hasNext()) {
            RouteHeader routeHeader = (RouteHeader) routes.next();
            Address routeAddress = routeHeader.getAddress();
            URI routeURI = routeAddress.getURI();
            if (routeURI.isSipURI()) {
                SipURI routeSipURI = (SipURI) routeURI;
                String routeHost = routeSipURI.getHost();
                int routePort = routeSipURI.getPort();
                if (routePort == -1) routePort = 5060;
                ListeningPoint[] lps = sipProvider.getListeningPoints();
                if (addrMatchesInterface(routeHost) && routePort == lps[0].getPort()) {
                    if (log.isTraceEnabled()) log.trace("Removing the first route " + routeSipURI + " from the RouteHeader: matches the proxy " + routeHost + ":" + routePort);
                    request.removeFirst(RouteHeader.NAME);
                }
            } else if (log.isTraceEnabled()) log.trace("Route Header value " + routeURI + " is not a SIP URI");
        } else if (log.isTraceEnabled()) log.trace("Request doesn't contain Route header.");
        serverTransaction = (operationMode == STATEFULL_MODE ? checkServerTransaction(sipProvider, request, serverTransaction) : null);
        if (log.isTraceEnabled()) log.trace("Server transaction for request is: " + serverTransaction);
        if (log.isTraceEnabled()) log.trace("Checking if Request-URI contains an maddr parameter...");
        if (requestSipURI != null && requestSipURI.getMAddrParam() != null) {
            if (log.isTraceEnabled()) log.trace("The only target is the Request-URI (mAddr parameter). Forwarding request.");
            forwardRequest(requestURI, request, sipProvider, serverTransaction, serverTransaction != null);
            return;
        }
        if (log.isTraceEnabled()) log.trace("No, Request-URI is not SIP URI or doesn't contain an maddr parameter.");
        if (log.isTraceEnabled()) log.trace("Checking if proxy is responsible for the domain...");
        if (requestSipURI != null) {
            if (!isDomainServed(requestSipURI.getHost()) && !addrMatchesInterface(requestSipURI.getHost())) {
                if (log.isTraceEnabled()) log.trace("No, so forwarding request...");
                forwardRequest(requestURI, request, sipProvider, serverTransaction, serverTransaction != null);
                return;
            }
        }
        if (log.isTraceEnabled()) log.trace("Yes, proxy is responsible for the domain.");
        if (log.isTraceEnabled()) log.trace("Handling special requests like CANCEL and REGISTER...");
        boolean requestMustBeProcessedStatelessly = false;
        if (operationMode == STATEFULL_MODE && method.equals(Request.CANCEL)) {
            SIPServerTransaction sipServerTransaction = (SIPServerTransaction) serverTransaction;
            SIPServerTransaction serverTransactionToTerminate = sipServerTransaction.getCanceledInviteTransaction();
            if (serverTransactionToTerminate != null) {
                TransactionsMapping transactionsMapping = (TransactionsMapping) serverTransactionToTerminate.getApplicationData();
                if (transactionsMapping != null) {
                    Response okResponse = messageFactory.createResponse(Response.OK, request);
                    serverTransaction.sendResponse(okResponse);
                    if (log.isTraceEnabled()) log.trace("OK replied back. \n" + okResponse + "\nTerminating pending ClientTransactions.");
                    if (log.isDebugEnabled()) log.debug("OK replied back.");
                    ClientTransaction[] clientTransactions = transactionsMapping.getClientTransactionsArray();
                    cancelPendingTransactions(clientTransactions, sipProvider);
                    return;
                }
            }
            requestMustBeProcessedStatelessly = true;
        }
        MethodPlugin methodPlugin = methodPlugins.get(request.getMethod());
        if (methodPlugin != null) {
            if (log.isDebugEnabled()) log.debug("Processing request via " + methodPlugin.getClass() + " method plugin.");
            try {
                Response response = methodPlugin.processRequest(request);
                if (response != null) {
                    if (serverTransaction != null) serverTransaction.sendResponse(response); else sipProvider.sendResponse(response);
                    if (log.isDebugEnabled()) log.debug("Replied " + response.getReasonPhrase() + " (" + response.getStatusCode() + ")");
                    if (log.isTraceEnabled()) log.trace("\n" + response);
                    return;
                } else if (log.isDebugEnabled()) log.debug("Plugin " + methodPlugin.getClass() + " didn't processed request. Response is NULL. Continue processing...");
            } catch (MethodPluginException ex) {
                log.warn("Plugin " + methodPlugin.getClass() + " failed to process request. Request dropped. Internal Server Error replied. " + ex.getMessage());
                if (log.isTraceEnabled()) log.trace("Plugin " + methodPlugin.getClass() + " failed to process request. Request dropped. Internal Server Error replied. ", ex);
                SipUtils.sendResponse(Response.SERVER_INTERNAL_ERROR, sipProvider, messageFactory, request, serverTransaction);
                return;
            }
        }
        if (log.isTraceEnabled()) log.trace("This is not special message or it was not processed. ");
        if (log.isTraceEnabled()) log.trace("Determining targets for request.");
        Vector<ContactHeader> targetURIList = null;
        String key = null;
        URI toURI = ((ToHeader) request.getHeader(ToHeader.NAME)).getAddress().getURI();
        if (toURI.isSipURI()) {
            SipURI toSipURI = (SipURI) toURI;
            if (addrMatchesInterface(toSipURI.getHost())) {
                SipURI fixedURI = (SipURI) toSipURI.clone();
                fixedURI.setHost(getDefaultDomain());
                key = SipUtils.getKeyToLocationService(fixedURI);
            }
        }
        if (key == null) key = SipUtils.getKeyToLocationService(request);
        try {
            targetURIList = locationService.getContactHeaders(key);
        } catch (RemoteException ex) {
            SipUtils.sendResponse(Response.SERVER_INTERNAL_ERROR, sipProvider, messageFactory, request, serverTransaction);
            return;
        } catch (UserNotFoundException ex) {
            if (log.isDebugEnabled()) log.debug("User " + key + " not found. " + Response.NOT_FOUND + " replied.");
            SipUtils.sendResponse(Response.NOT_FOUND, sipProvider, messageFactory, request, serverTransaction);
            return;
        }
        if (targetURIList != null && !targetURIList.isEmpty()) {
            if (operationMode == STATELESS_MODE && targetURIList.size() > 1) {
                ContactHeader ch = targetURIList.firstElement();
                targetURIList.removeAllElements();
                targetURIList.add(ch);
            }
            for (ContactHeader ch : targetURIList) {
                URI targetURI = ch.getAddress().getURI();
                forwardRequest(targetURI, request, sipProvider, serverTransaction, serverTransaction != null & !requestMustBeProcessedStatelessly);
            }
        } else {
            SipUtils.sendResponse(Response.TEMPORARILY_UNAVAILABLE, sipProvider, messageFactory, request, serverTransaction);
            if (log.isDebugEnabled()) log.debug("Target cannot be determined. " + Response.TEMPORARILY_UNAVAILABLE + " ( Temporarily Unavailable ) replied.");
        }
    }

    /**
     * Validates incoming requests. See section 16.3 RFC 3261.
     * Sends error responses if request is not valid.
     * @param request Request to validate
     * @param sipProvider SipProvider object
     * @param serverTransaction Associated server transaction if any
     * @return true - request is valid, false - otherwise (error response is also sent).
     * @throws InvalidArgumentException
     * @throws SipException
     * @throws ParseException
     */
    public boolean validateRequest(Request request, SipProvider sipProvider, ServerTransaction serverTransaction, LocationServiceInterface locationService) throws InvalidArgumentException, SipException, ParseException {
        String uriScheme = request.getRequestURI().getScheme();
        if (!(uriScheme.equals("sip") || uriScheme.equals("sips") || uriScheme.equals("tel"))) {
            if (log.isDebugEnabled()) log.debug("Unsupported URI scheme: " + uriScheme);
            SipUtils.sendResponse(Response.UNSUPPORTED_URI_SCHEME, sipProvider, messageFactory, request, serverTransaction);
            return false;
        }
        MaxForwardsHeader mf = (MaxForwardsHeader) request.getHeader(MaxForwardsHeader.NAME);
        if (mf != null && mf.getMaxForwards() <= 0) {
            SipUtils.sendResponse(Response.TOO_MANY_HOPS, sipProvider, messageFactory, request, serverTransaction);
            if (log.isDebugEnabled()) log.debug("Too many hops.");
            return false;
        }
        if (checkLoopDetection(request, sipProvider)) {
            SipUtils.sendResponse(Response.LOOP_DETECTED, sipProvider, messageFactory, request, serverTransaction);
            if (log.isDebugEnabled()) log.debug("Loop detected. " + Response.LOOP_DETECTED + " replied.");
            return false;
        }
        if (!checkProxyRequire(request)) {
            if (log.isDebugEnabled()) log.debug("SIP extensions are not supported.");
            Response response = messageFactory.createResponse(Response.BAD_EXTENSION, request);
            ProxyRequireHeader prh = (ProxyRequireHeader) request.getHeader(ProxyRequireHeader.NAME);
            if (prh != null) {
                UnsupportedHeader unsupportedHeader = headerFactory.createUnsupportedHeader(prh.getOptionTag());
                response.setHeader(unsupportedHeader);
            }
            if (serverTransaction != null) serverTransaction.sendResponse(response); else sipProvider.sendResponse(response);
            return false;
        }
        if (authenticationEnabled) {
            Request fixedRequest = null;
            URI requestURI = request.getRequestURI();
            if (requestURI.isSipURI()) {
                String host = ((SipURI) requestURI).getHost();
                if (addrMatchesInterface(host)) {
                    fixedRequest = (Request) request.clone();
                    ((SipURI) fixedRequest.getRequestURI()).setHost(getDefaultDomain());
                    URI toURI = ((ToHeader) fixedRequest.getHeader(ToHeader.NAME)).getAddress().getURI();
                    if (toURI.isSipURI()) ((SipURI) toURI).setHost(getDefaultDomain());
                }
            }
            boolean requestAuthorized;
            try {
                requestAuthorized = checkProxyAuthorization(fixedRequest == null ? request : fixedRequest, dsam, locationService);
            } catch (UserNotFoundException ex) {
                if (log.isDebugEnabled()) log.debug(ex.getMessage());
                requestAuthorized = false;
            } catch (RemoteException ex) {
                if (log.isDebugEnabled()) log.debug("Connection to Location Service lost.");
                SipUtils.sendResponse(Response.SERVER_INTERNAL_ERROR, sipProvider, messageFactory, request, serverTransaction);
                return false;
            }
            if (!requestAuthorized) {
                if (log.isDebugEnabled()) log.debug("Request rejected ( Unauthorized )");
                Response response = messageFactory.createResponse(Response.PROXY_AUTHENTICATION_REQUIRED, request);
                ProxyAuthenticateHeader proxyAuthenticateHeader = headerFactory.createProxyAuthenticateHeader("Digest");
                proxyAuthenticateHeader.setParameter("realm", dsam.getDefaultRealm());
                proxyAuthenticateHeader.setParameter("nonce", dsam.generateNonce(dsam.getPreferredAlgorithm()));
                proxyAuthenticateHeader.setParameter("opaque", "");
                proxyAuthenticateHeader.setParameter("stale", "FALSE");
                proxyAuthenticateHeader.setParameter("algorithm", dsam.getPreferredAlgorithm());
                response.setHeader(proxyAuthenticateHeader);
                if (serverTransaction != null) serverTransaction.sendResponse(response); else sipProvider.sendResponse(response);
                return false;
            }
        }
        return true;
    }

    /**
     * Perfoms authorization on request
     * @param request Request
     * @param dsam
     * @return true if request has passed authorization
     * @throws openjsip.remote.locationservice.UserNotFoundException If specified subscriber in request was not found in location service database
     * @throws RemoteException Location Service connection troubles
     */
    private boolean checkProxyAuthorization(Request request, DigestServerAuthenticationMethod dsam, LocationServiceInterface locationService) throws UserNotFoundException, RemoteException {
        ProxyAuthorizationHeader proxyAuthorizationHeader = (ProxyAuthorizationHeader) request.getHeader(ProxyAuthorizationHeader.NAME);
        if (proxyAuthorizationHeader == null) {
            if (log.isDebugEnabled()) log.debug("Authentication failed: ProxyAuthorization header missing!");
            return false;
        } else {
            String key = SipUtils.getKeyToLocationService(request);
            String username = locationService.getUsername(key);
            String password = locationService.getPassword(key);
            if (password == null) password = "";
            String username_h = proxyAuthorizationHeader.getParameter("username");
            if (username_h == null) return false;
            if (username_h.indexOf('@') != -1) username_h = username_h.substring(0, username_h.indexOf('@'));
            if (!username.equals(username_h)) return false;
            return dsam.doAuthenticate(request, proxyAuthorizationHeader, username_h, password);
        }
    }

    /**
     * Returns whether loop detected
     * @param request Request
     * @return true if request is looped, false otherwise
     */
    private boolean checkLoopDetection(Request request, SipProvider sipProvider) {
        ListIterator viaList = request.getHeaders(ViaHeader.NAME);
        if (viaList != null && viaList.hasNext()) {
            ViaHeader viaHeader = (ViaHeader) viaList.next();
            ListeningPoint[] lps = sipProvider.getListeningPoints();
            String viaHost = viaHeader.getHost();
            int viaPort = viaHeader.getPort();
            if ((viaHost.equals(lps[0].getIPAddress()) || viaHost.equalsIgnoreCase(getHostname(sipProvider))) && viaPort == lps[0].getPort()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this proxy supports required extensions
     * @param request Request
     * @return true / false
     */
    private boolean checkProxyRequire(Request request) {
        ProxyRequireHeader prh = (ProxyRequireHeader) request.getHeader(ProxyRequireHeader.NAME);
        if (prh == null) return true; else {
            return false;
        }
    }

    /**
     * Creates a new ServerTransaction object that will handle the request if necessary and if request type is to be
     * handled by transactions.
     * @param sipProvider SipProvider object
     * @param request Incoming request
     * @param st ServerTransaction that was retrieved from RequestEvent
     * @return
     */
    private ServerTransaction checkServerTransaction(SipProvider sipProvider, Request request, ServerTransaction st) {
        ServerTransaction serverTransaction = st;
        if (operationMode == STATEFULL_MODE && serverTransaction == null && !request.getMethod().equals(Request.ACK)) {
            try {
                serverTransaction = sipProvider.getNewServerTransaction(request);
                snmpAssistant.incrementSnmpInteger(SNMP_OID_NUM_SERVER_TRANSACTIONS);
                TransactionsMapping transactionsMapping = (TransactionsMapping) serverTransaction.getApplicationData();
                if (transactionsMapping == null) {
                    transactionsMapping = new TransactionsMapping(serverTransaction, sipProvider);
                    serverTransaction.setApplicationData(transactionsMapping);
                }
            } catch (TransactionAlreadyExistsException ex) {
                if (log.isTraceEnabled()) log.trace(ex + " is a retransmission.");
            } catch (TransactionUnavailableException ex) {
                if (log.isTraceEnabled()) log.trace("ServerTransaction cannot be created for this request: " + ex);
            }
        }
        return serverTransaction;
    }

    /**
     * Forwards request to specified target.
     * @param targetURI Target to forward request. If it is null, target will be existing RequestURI of request or taken from Route header if exists.
     * @param request Request to forward
     * @param sipProvider SipProvider object
     * @param serverTransaction ServerTransaction that handles this request. If null, the request will be forwarded statelessly.
     * @param statefullForwarding If true and serverTransaction != null, the request will be forwarded statefully, otherwise, it will be forwarded statelessly.
     * @throws InvalidArgumentException
     * @throws ParseException
     * @throws SipException
     */
    public void forwardRequest(URI targetURI, Request request, SipProvider sipProvider, ServerTransaction serverTransaction, boolean statefullForwarding) throws InvalidArgumentException, ParseException, SipException {
        statefullForwarding = statefullForwarding & (serverTransaction != null);
        URI requestURI = request.getRequestURI();
        if (log.isTraceEnabled()) log.trace("Forwarding request to " + targetURI + (statefullForwarding ? " statefully" : " statelessly"));
        Request clonedRequest = (Request) request.clone();
        if (requestURI.isSipURI()) {
            if (targetURI != null) {
                clonedRequest.setRequestURI(SipUtils.getCanonicalizedURI(targetURI));
                if (log.isTraceEnabled()) log.trace("RequestURI replaced with " + clonedRequest.getRequestURI());
            }
        } else {
            if (log.isDebugEnabled()) log.debug("Forwarding not SIP requests is currently not implemented.");
            return;
        }
        MaxForwardsHeader mf = (MaxForwardsHeader) clonedRequest.getHeader(MaxForwardsHeader.NAME);
        if (mf == null) {
            mf = headerFactory.createMaxForwardsHeader(70);
            clonedRequest.addHeader(mf);
            if (log.isTraceEnabled()) log.trace("Max-Forwards header is missing. Created and added to the cloned request.");
        } else {
            mf.setMaxForwards(mf.getMaxForwards() - 1);
            if (log.isTraceEnabled()) log.trace("Max-Forwards value decremented by one. It is now: " + mf.getMaxForwards());
        }
        ListeningPoint[] lps = sipProvider.getListeningPoints();
        SipURI sipURI = addressFactory.createSipURI(null, getHostname(sipProvider));
        sipURI.setPort(lps[0].getPort());
        Address address = addressFactory.createAddress(null, sipURI);
        RecordRouteHeader recordRouteHeader = headerFactory.createRecordRouteHeader(address);
        recordRouteHeader.setParameter("lr", null);
        clonedRequest.addFirst(recordRouteHeader);
        if (log.isTraceEnabled()) log.trace("Added Record-Route header: " + recordRouteHeader);
        if (log.isTraceEnabled()) log.trace("Postprocessing routing information...");
        ListIterator routes = clonedRequest.getHeaders(RouteHeader.NAME);
        if (routes != null && routes.hasNext()) {
            RouteHeader routeHeader = (RouteHeader) routes.next();
            Address routeAddress = routeHeader.getAddress();
            URI routeURI = routeAddress.getURI();
            if (routeURI.isSipURI() && (!((SipURI) routeURI).hasLrParam())) {
                RouteHeader routeHeaderToAdd = headerFactory.createRouteHeader(addressFactory.createAddress(clonedRequest.getRequestURI()));
                clonedRequest.addLast(routeHeaderToAdd);
                clonedRequest.setRequestURI(routeURI);
                clonedRequest.removeFirst(RouteHeader.NAME);
                if (log.isTraceEnabled()) log.trace("RequestURI placed to the end of Route headers, and first Route header " + routeURI + " was set as RequestURI");
            } else if (log.isTraceEnabled()) log.trace("First Route header " + routeHeader + " is not SIP URI or it doesn't contain lr parameter");
        } else {
            if (log.isTraceEnabled()) log.trace("No postprocess routing information to do (No routes detected).");
        }
        if (log.isTraceEnabled()) log.trace("Postprocessing finished.");
        String branchId = SipUtils.generateBranchId();
        if (operationMode == STATELESS_MODE) {
            try {
                ViaHeader topmostViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                if (topmostViaHeader != null) {
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                    String branch = topmostViaHeader.getBranch();
                    if (branch.startsWith(SipUtils.BRANCH_MAGIC_COOKIE)) {
                        byte[] bytes = messageDigest.digest(Integer.toString(branch.hashCode()).getBytes());
                        branchId = SipUtils.toHexString(bytes);
                    } else {
                        String via = topmostViaHeader.toString().trim();
                        String toTag = ((ToHeader) request.getHeader(ToHeader.NAME)).getTag();
                        String fromTag = ((FromHeader) request.getHeader(FromHeader.NAME)).getTag();
                        String callid = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();
                        long cseq = ((CSeqHeader) request.getHeader(CSeqHeader.NAME)).getSeqNumber();
                        String requestUri = requestURI.toString().trim();
                        byte[] bytes = messageDigest.digest((via + toTag + fromTag + callid + cseq + requestUri).getBytes());
                        branchId = SipUtils.toHexString(bytes);
                    }
                }
            } catch (NoSuchAlgorithmException ex) {
            }
        }
        ViaHeader viaHeader = headerFactory.createViaHeader(getHostname(sipProvider), lps[0].getPort(), lps[0].getTransport(), branchId);
        clonedRequest.addFirst(viaHeader);
        if (log.isTraceEnabled()) log.trace("Added Via header " + viaHeader);
        ContentLengthHeader contentLengthHeader = (ContentLengthHeader) clonedRequest.getHeader(ContentLengthHeader.NAME);
        if (contentLengthHeader == null) {
            byte[] contentData = request.getRawContent();
            contentLengthHeader = headerFactory.createContentLengthHeader(contentData == null ? 0 : contentData.length);
            clonedRequest.setContentLength(contentLengthHeader);
            if (log.isTraceEnabled()) log.trace("Added Content-Length header " + contentLengthHeader);
        } else if (log.isTraceEnabled()) log.trace("Leaving existing Content-Length header untouched.");
        if (log.isTraceEnabled()) {
            log.trace("Forwarding request " + (statefullForwarding ? "statefully" : "statelessly"));
            log.trace("Outgoing request:\n" + clonedRequest);
        }
        if (!statefullForwarding) {
            sipProvider.sendRequest(clonedRequest);
            if (log.isDebugEnabled()) log.debug("Request forwarded statelessly.");
        } else {
            ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(clonedRequest);
            if (log.isTraceEnabled()) log.trace("Client transaction for request is: " + clientTransaction);
            snmpAssistant.incrementSnmpInteger(SNMP_OID_NUM_CLIENT_TRANSACTIONS);
            TransactionsMapping transactionMapping = (TransactionsMapping) serverTransaction.getApplicationData();
            if (transactionMapping == null) {
                transactionMapping = new TransactionsMapping(serverTransaction, sipProvider);
                serverTransaction.setApplicationData(transactionMapping);
            }
            transactionMapping.addClientTransaction(clientTransaction);
            clientTransaction.setApplicationData(transactionMapping);
            if (clonedRequest.getMethod().equals(Request.INVITE)) {
                Timer timer = new Timer();
                TimerCTask timerTask = new TimerCTask(clientTransaction, sipProvider, this, log);
                transactionMapping.registerTimerC(timer, clientTransaction);
                if (log.isTraceEnabled()) log.trace("Timer C created for proxied CT " + clientTransaction);
                timer.schedule(timerTask, timercPeriod);
            }
            clientTransaction.sendRequest();
            if (log.isDebugEnabled()) log.debug("Request forwarded statefully.");
        }
    }

    /**
     * Processes incoming responses and forwards them if necessary.
     * @param responseEvent ResponseEvent object
     * @throws InvalidArgumentException
     * @throws ParseException
     * @throws SipException
     */
    private void processIncomingResponse(ResponseEvent responseEvent) throws InvalidArgumentException, ParseException, SipException {
        Response response = responseEvent.getResponse();
        SipProvider sipProvider = (SipProvider) responseEvent.getSource();
        ClientTransaction clientTransaction = (operationMode == STATEFULL_MODE ? responseEvent.getClientTransaction() : null);
        int statusCode = response.getStatusCode();
        String reason = response.getReasonPhrase();
        CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        ContactHeader contactHeader = (ContactHeader) response.getHeader(ContactHeader.NAME);
        if (log.isDebugEnabled()) {
            log.debug("-------------------");
            log.debug("Incoming " + cseqHeader.getMethod() + " response from " + (contactHeader != null ? contactHeader.getAddress() : "No Contact header"));
            log.debug("Status: " + statusCode + " reason: " + reason);
        }
        if (log.isTraceEnabled()) log.trace("\n" + response.toString());
        if (clientTransaction == null) {
            if (log.isTraceEnabled()) log.trace("ClientTransaction is null. Forwarding the response statelessly.");
            processResponseStatelessly(response, sipProvider);
            return;
        }
        TransactionsMapping transactionsMapping = (TransactionsMapping) clientTransaction.getApplicationData();
        if (transactionsMapping == null) {
            if (log.isDebugEnabled()) log.debug("Response context cannot be found for this response. Forwarding the response statelessly.");
            processResponseStatelessly(response, sipProvider);
            return;
        }
        ServerTransaction serverTransaction = transactionsMapping.getServerTransaction();
        if (serverTransaction == null) {
            if (log.isDebugEnabled()) log.debug("Server transaction cannot be found for this response. Dropped.");
            return;
        }
        if (statusCode >= 101 && statusCode <= 199 && cseqHeader.getMethod().equals(Request.INVITE)) {
            transactionsMapping.cancelTimerC(clientTransaction);
            Timer timer = new Timer();
            TimerCTask timerTask = new TimerCTask(clientTransaction, sipProvider, this, log);
            transactionsMapping.registerTimerC(timer, clientTransaction);
            timer.schedule(timerTask, timercPeriod);
            if (log.isTraceEnabled()) log.trace("Timer C updated for CT " + clientTransaction);
        }
        if (log.isTraceEnabled()) log.trace("Removing topmost Via header.");
        response.removeFirst(ViaHeader.NAME);
        ListIterator viaList = response.getHeaders(ViaHeader.NAME);
        if (viaList == null || !viaList.hasNext()) {
            if (log.isDebugEnabled()) log.debug("Response has no more Via headers. The response is for the proxy. Not forwarded.");
            checkResponseContext(transactionsMapping);
            return;
        }
        if (serverTransaction.getState().getValue() >= TransactionState._COMPLETED) {
            if (statusCode >= 200 && statusCode <= 299 && cseqHeader.getMethod().equals(Request.INVITE)) {
                sendResponseImmediately(response, transactionsMapping);
                return;
            } else {
                return;
            }
        }
        if (!((SIPResponse) response).isFinalResponse()) {
            if (statusCode == Response.TRYING) {
                if (log.isDebugEnabled()) log.debug("Response " + statusCode + " (" + response.getReasonPhrase() + ") is not forwarded.");
                return;
            } else {
                if (log.isTraceEnabled()) log.trace("Response is 1XX, so forwarding immediately.");
                sendResponseImmediately(response, transactionsMapping);
                return;
            }
        }
        transactionsMapping.getResponseContext().addFinalResponse(response);
        if (statusCode >= 200 && statusCode <= 299) {
            if (log.isTraceEnabled()) log.trace("2XX are to be forwarded immediately.");
            sendResponseImmediately(response, transactionsMapping);
            return;
        } else if (statusCode >= 600) {
            cancelPendingTransactions(transactionsMapping.getClientTransactionsArray(), sipProvider);
        }
        checkResponseContext(transactionsMapping);
    }

    /**
     * Processes response statelessly
     * @param response Response to forward
     * @param sipProvider SipProvider object
     * @throws SipException
     */
    private void processResponseStatelessly(Response response, SipProvider sipProvider) throws SipException {
        ListIterator viaList = response.getHeaders(ViaHeader.NAME);
        if (viaList != null && viaList.hasNext()) {
            ViaHeader viaHeader = (ViaHeader) viaList.next();
            String viaHost = viaHeader.getHost();
            int viaPort = viaHeader.getPort();
            if (viaPort == -1) viaPort = 5060;
            ListeningPoint[] lps = sipProvider.getListeningPoints();
            if (addrMatchesInterface(viaHost) && viaPort == lps[0].getPort()) {
                if (log.isTraceEnabled()) log.trace("Top Via header matches proxy. Removing first Via header.");
                response.removeFirst(ViaHeader.NAME);
                viaList = response.getHeaders(ViaHeader.NAME);
                if (viaList.hasNext()) {
                    sipProvider.sendResponse(response);
                    if (log.isDebugEnabled()) log.debug("Response forwarded statelessly.");
                    if (log.isTraceEnabled()) log.trace("\n" + response);
                }
            }
        } else if (log.isDebugEnabled()) log.debug("Via address doesn't match proxy or no Via headers left. Response is dropped.");
    }

    /**
     * Generates and sends CANCEL requests for all pending client transactions.
     * @param clientTransactions An array with client transactions. Each will be checked for being "pending".
     * @param sipProvider SipProvider object
     * @throws SipException
     */
    private void cancelPendingTransactions(ClientTransaction[] clientTransactions, SipProvider sipProvider) throws SipException {
        for (int i = 0; i < clientTransactions.length; i++) {
            ClientTransaction clientTransaction = clientTransactions[i];
            if (log.isTraceEnabled()) log.trace("Found " + clientTransaction.getState());
            if (clientTransaction.getState().equals(TransactionState.PROCEEDING)) {
                Request cancelRequest = clientTransaction.createCancel();
                ClientTransaction cancelTransaction = sipProvider.getNewClientTransaction(cancelRequest);
                snmpAssistant.incrementSnmpInteger(SNMP_OID_NUM_CLIENT_TRANSACTIONS);
                cancelTransaction.sendRequest();
                if (log.isTraceEnabled()) log.trace("Cancel request for transaction " + clientTransaction + " is sent.");
            }
        }
    }

    /**
     * This functions gets the best response from response context and forwards it to recipient under following circumstances:
     *   - Server transaction is not yet completed
     *   - All client transactions are completed
     * @param transactionsMapping Transactions mapping object
     */
    private void checkResponseContext(TransactionsMapping transactionsMapping) {
        try {
            ServerTransaction serverTransaction = transactionsMapping.getServerTransaction();
            if (serverTransaction.getState().getValue() >= TransactionState._COMPLETED) return;
            ClientTransaction[] clientTransactions = transactionsMapping.getClientTransactionsArray();
            for (int i = 0; i < clientTransactions.length; i++) if (clientTransactions[i].getState().getValue() < TransactionState._COMPLETED) return;
            Response bestResponse = transactionsMapping.getResponseContext().getBestResponse(messageFactory);
            if (bestResponse == null) {
                if (log.isDebugEnabled()) log.debug("Cannot determine best response (null). Code debug required.");
                return;
            }
            sendResponseImmediately(bestResponse, transactionsMapping);
        } catch (Exception ex) {
            if (log.isDebugEnabled()) log.debug("Exception raised int checkResponseContext() method: " + ex.getMessage());
            if (log.isTraceEnabled()) log.trace("", ex);
        }
    }

    /**
     * Processes steps of response immediate forwarding.
     * See section 16.7 step 5.
     *    Any response chosen for immediate forwarding MUST be processed
     *    as described in steps "Aggregate Authorization Header Field
     *    Values" through "Record-Route".
     * @param outgoingResponse Response to send
     * @param transactionsMapping Transaction mapping
     * @throws InvalidArgumentException
     * @throws SipException
     */
    private void sendResponseImmediately(Response outgoingResponse, TransactionsMapping transactionsMapping) throws InvalidArgumentException, SipException {
        ServerTransaction serverTransaction = transactionsMapping.getServerTransaction();
        if (serverTransaction.getState().getValue() < TransactionState._COMPLETED) {
            serverTransaction.sendResponse(outgoingResponse);
            if (log.isDebugEnabled()) log.debug("Response is statefully forwarded.");
        } else {
            if (log.isDebugEnabled()) log.debug("Sending response statelessly because associated server transaction's state is already " + serverTransaction.getState());
            transactionsMapping.getSipProvider().sendResponse(outgoingResponse);
            if (log.isDebugEnabled()) log.debug("Response is statelessly forwarded.");
        }
        if (log.isTraceEnabled()) log.trace("\n" + outgoingResponse);
        if (((SIPResponse) outgoingResponse).isFinalResponse()) {
            if (log.isTraceEnabled()) log.trace("Forwarded response is final. Canceling pending transactions.");
            cancelPendingTransactions(transactionsMapping.getClientTransactionsArray(), transactionsMapping.getSipProvider());
        }
    }

    public String execCmd(String cmd, String[] parameters) throws RemoteException {
        if (cmd == null) return null;
        if (cmd.equalsIgnoreCase("get") && parameters != null) {
            if (parameters.length > 0) {
                if (parameters[0].equalsIgnoreCase("numRequestsProcessed")) return snmpAssistant.getSnmpOIDValue(SNMP_OID_NUM_REQUESTS_PROCESSED).toString(); else if (parameters[0].equalsIgnoreCase("numResponsesProcessed")) return snmpAssistant.getSnmpOIDValue(SNMP_OID_NUM_RESPONSES_PROCESSED).toString(); else if (parameters[0].equalsIgnoreCase("numRequestsNotProcessed")) return snmpAssistant.getSnmpOIDValue(SNMP_OID_NUM_REQUEST_PROCESSING_ERRORS).toString(); else if (parameters[0].equalsIgnoreCase("numResponsesNotProcessed")) return snmpAssistant.getSnmpOIDValue(SNMP_OID_NUM_RESPONSE_PROCESSING_ERRORS).toString(); else if (parameters[0].equalsIgnoreCase("numServerTransactions")) return snmpAssistant.getSnmpOIDValue(SNMP_OID_NUM_SERVER_TRANSACTIONS).toString(); else if (parameters[0].equalsIgnoreCase("numClientTransactions")) return snmpAssistant.getSnmpOIDValue(SNMP_OID_NUM_CLIENT_TRANSACTIONS).toString(); else if (parameters[0].equalsIgnoreCase("vm_freememory")) return Long.toString(Runtime.getRuntime().freeMemory()); else if (parameters[0].equalsIgnoreCase("vm_maxmemory")) return Long.toString(Runtime.getRuntime().maxMemory()); else if (parameters[0].equalsIgnoreCase("vm_totalmemory")) return Long.toString(Runtime.getRuntime().totalMemory()); else return null;
            }
        }
        return "help                           - Show help.\n" + "get numRequestsProcessed       - Get the total number of successfully processed requests.\n" + "get numResponsesProcessed      - Get the total number of processed responses.\n" + "get numRequestsNotProcessed    - Get the total number of requests not being processed due to internal errors.\n" + "get numResponsesNotProcessed   - Get the total number of responses not being processed due to internal errors.\n" + "get numServerTransactions      - Get the total number of server transactions that proxy currently maintains.\n" + "get numClientTransactions      - Get the total number of client transactions that proxy currently maintains.\n" + "get vm_freememory              - Get the amount of free memory in the Java Virtual Machine.\n" + "get vm_maxmemory               - Get the maximum amount of memory that the Java virtual machine will attempt to use.\n" + "get vm_totalmemory             - Get the total amount of memory in the Java virtual machine.\n";
    }

    public boolean isAlive() throws RemoteException {
        return true;
    }
}
