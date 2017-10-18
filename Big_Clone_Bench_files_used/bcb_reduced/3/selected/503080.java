package org.openliberty.arisidbeans;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import org.openliberty.arisid.ArisIdService;
import org.openliberty.arisid.ArisIdServiceFactory;
import org.openliberty.arisid.AttributeFilter;
import org.openliberty.arisid.AttributeFilterValue;
import org.openliberty.arisid.AttributeValue;
import org.openliberty.arisid.Filter;
import org.openliberty.arisid.IAddInteraction;
import org.openliberty.arisid.IAttributeValue;
import org.openliberty.arisid.ICompareInteraction;
import org.openliberty.arisid.IDeleteInteraction;
import org.openliberty.arisid.IDigitalSubject;
import org.openliberty.arisid.IFindInteraction;
import org.openliberty.arisid.IGFException;
import org.openliberty.arisid.IInteraction;
import org.openliberty.arisid.IModifyInteraction;
import org.openliberty.arisid.IReadInteraction;
import org.openliberty.arisid.ISearchInteraction;
import org.openliberty.arisid.InvalidFilterException;
import org.openliberty.arisid.PrincipalIdentifier;
import org.openliberty.arisid.SchemaManager;
import org.openliberty.arisid.log.ILogger;
import org.openliberty.arisid.log.LogHandler;
import org.openliberty.arisid.policy.IPolicy;
import org.openliberty.arisid.policy.PolicyHandler;
import org.openliberty.arisid.schema.AttributeDef;
import org.openliberty.arisid.schema.PredicateDef;
import org.openliberty.arisid.schema.RoleDef;
import org.openliberty.arisid.stack.ConnectionException;
import org.openliberty.arisid.stack.DeclarationException;
import org.openliberty.arisid.stack.IPrincipalIdentifier;
import org.openliberty.arisid.stack.IResultSet;
import org.openliberty.arisid.stack.MappingException;
import org.openliberty.arisid.stack.NoSuchContextException;
import org.openliberty.arisid.stack.NoSuchSubjectException;
import org.openliberty.arisid.stack.PolicyException;
import org.openliberty.arisid.stack.SchemaException;
import org.openliberty.arisid.stack.SubjectNotUniqueException;
import org.openliberty.arisid.stack.AuthenticationException;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * IGFObjectManager is an abstract class containing methods that are generic to
 * any CARML declaration and this class is inherited by the BeanManager class
 * generated from a CARML file.
 */
public abstract class IGFObjectManager {

    protected ArisIdService asvc = null;

    private PolicyHandler polHandler = null;

    private static Map<String, ArisIdService> asvcMap = new HashMap<String, ArisIdService>();

    private static Map<ArisIdService, Integer> asvcRefCntMap = new HashMap<ArisIdService, Integer>();

    private static ILogger logr = LogHandler.getLogger(IGFObjectManager.class);

    /**
     * @deprecated This will be removed in the future. 
     * Use {@link ArisIdConstants#APP_CTX_AUTHUSER} instead
     */
    public static final String APP_CTX_AUTHUSER = ArisIdConstants.APP_CTX_AUTHUSER;

    /**
     * @deprecated This will be removed in the future. 
     * Use {@link ArisIdConstants#APP_CTX_PAGESIZE} instead
     */
    public static final String APP_CTX_PAGE = ArisIdConstants.APP_CTX_PAGESIZE;

    /**
     * @deprecated This will be removed in the future. 
     * Use {@link ArisIdConstants#APP_CTX_LOCALE} instead
     */
    public static final String APP_CTX_LOCALE = ArisIdConstants.APP_CTX_LOCALE;

    /**
     * @deprecated This will be removed in the future. 
     * Use {@link ArisIdConstants#SECURITY_PRINCIPAL} instead
     */
    public static final String SECURITY_PRINCIPAL = ArisIdConstants.SECURITY_PRINCIPAL;

    /**
     * @deprecated This will be removed in the future. 
     * Use {@link ArisIdConstants#SECURITY_CREDENTIALS} instead
     */
    public static final String SECURITY_CREDENTIALS = ArisIdConstants.SECURITY_CREDENTIALS;

    /**
     * @deprecated This will be removed in the future. 
     */
    public static final String LOGGER_NAME = "igf.arisidbeans.logger";

    /**
     * @deprecated This will be removed in the future. 
     */
    public static final String LOG_LEVEL = "igf.arisidbeans.loglevel";

    /**
     * Creates an ArisId Service instance based on CARML declaration document
     *
     * @param carmlFile
     *            A CARML declaration document used to initialize the attribute
     *            service
     * @param properties
     *            Map with IdentiyBeans parameters SECURITY_PRINCIPAL,
     *            SECURITY_CREDENTIALS
     * @throws FileNotFoundException
     *             Thrown when the CARML document cannot be located through the
     *             provided URI.
     * @throws URISyntaxException
     *             Thrown when the referenced external CARML URI is invalid
     * @throws IllegalAccessException
     *             An Exception has occurred with XML parser
     * @throws IDBeanException
     *             General Identity Bean Exception
     */
    protected void initialize(String carmlFile, Map<String, Object> properties) throws FileNotFoundException, URISyntaxException, IllegalAccessException, IDBeanException {
        initialize(getCarmlURI(carmlFile), properties);
    }

    /**
     * Creates an ArisId Service instance based on the URI of CARML
     * declaration document
     *
     * @param carmlURI
     *            URI of external CARML declaration document used to initialize
     *            the ArisId service
     * @param properties
     *            Map with IdentiyBeans parameters SECURITY_PRINCIPAL,
     *            SECURITY_CREDENTIALS
     * @throws FileNotFoundException
     *             Thrown when the CARML document cannot be located through the
     *             provided URI.
     * @throws IllegalAccessException
     *             An Exception has occurred with XML parser
     * @throws IDBeanException
     *             General Identity Bean Exception
     */
    protected synchronized void initialize(URI carmlURI, Map<String, Object> properties) throws FileNotFoundException, IDBeanException, IllegalAccessException {
        String appCredStr = null;
        Subject appCredential = null;
        if (System.getProperty(ArisIdConstants.BASE64_CLASS) == null) {
            System.setProperty(ArisIdConstants.BASE64_CLASS, ArisIdConstants.DEFAULT_BASE64_CLASS);
        }
        if (properties != null) {
            String user = (String) properties.get(ArisIdConstants.SECURITY_PRINCIPAL);
            Object p = properties.get(ArisIdConstants.SECURITY_CREDENTIALS);
            byte[] pwd = null;
            if (p != null) {
                if (p instanceof byte[]) pwd = (byte[]) p; else {
                    try {
                        pwd = ((String) p).getBytes("UTF-8");
                    } catch (java.io.UnsupportedEncodingException uee) {
                        pwd = ((String) p).getBytes();
                    }
                }
            }
            if (user != null & pwd != null) {
                appCredential = new Subject();
                appCredential.getPrincipals().add(new PrincipalIdentifier(user));
                appCredential.getPrivateCredentials().add(pwd);
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(pwd);
                    byte raw[] = md.digest();
                    String encPwd = AttributeValue.base64Encode(raw);
                    appCredStr = user + encPwd;
                } catch (NoSuchAlgorithmException e) {
                    throw new IDBeanException(e);
                }
            }
            if (properties.get(ArisIdConstants.ATTRIBUTE_SERVICE_PROVIDER) != null) {
                System.setProperty(ArisIdConstants.ATTRIBUTE_SERVICE_PROVIDER, (String) properties.get(ArisIdConstants.ATTRIBUTE_SERVICE_PROVIDER));
            }
        }
        if (System.getProperty(ArisIdConstants.ATTRIBUTE_SERVICE_PROVIDER) == null) {
            System.setProperty(ArisIdConstants.ATTRIBUTE_SERVICE_PROVIDER, ArisIdConstants.DEFAULT_ATTRIBUTE_SERVICE_PROVIDER);
        }
        if (System.getProperty(ArisIdConstants.WS_POLICY_CLASS) == null) {
            System.setProperty(ArisIdConstants.WS_POLICY_CLASS, ArisIdConstants.DEFAULT_WS_POLICY_CLASS);
        }
        if (System.getProperty(DOMImplementationRegistry.PROPERTY) == null) System.setProperty(DOMImplementationRegistry.PROPERTY, ArisIdConstants.DEFAULT_DOMIMPLEMENTATIONSOURCE);
        try {
            String key = carmlURI.toString() + appCredStr;
            if (asvcMap.containsKey(key)) {
                logr.debug("Getting ArisId service from map for CARML file: " + carmlURI);
                asvc = asvcMap.get(key);
                Integer refCnt = asvcRefCntMap.get(asvc);
                asvcRefCntMap.put(asvc, ++refCnt);
                logr.trace("Incremented ArisId reference count to " + refCnt);
            } else {
                logr.debug("Creating ArisId service for CARML file: " + carmlURI);
                asvc = ArisIdServiceFactory.createAttributeService(appCredential, carmlURI);
                asvcMap.put(key, asvc);
                asvcRefCntMap.put(asvc, 1);
                logr.trace("Set ArisId reference count to 1");
                logr.debug("ArisId service created for CARML file: " + carmlURI);
            }
        } catch (AuthenticationException e) {
            logr.debug(e.toString());
            throw new IDBeanException("Authentication Failed", e);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (NoSuchSubjectException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SubjectNotUniqueException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (ClassNotFoundException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (InstantiationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (IGFException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } finally {
            appCredential = null;
        }
        polHandler = PolicyHandler.getInstance();
    }

    private URI getCarmlURI(String carmlFile) throws IDBeanException {
        URI uri = null;
        File file = new File(carmlFile);
        if (file.exists()) {
            uri = file.toURI();
        } else {
            URL carmlfileURL = null;
            carmlfileURL = Thread.currentThread().getContextClassLoader().getResource("/META-INF/CARML/" + carmlFile);
            if (carmlfileURL == null) {
                carmlfileURL = Thread.currentThread().getContextClassLoader().getResource("/CARML/" + carmlFile);
            }
            if (carmlfileURL != null) {
                try {
                    uri = new URI(carmlfileURL.toString().replaceAll(" ", "%20"));
                } catch (URISyntaxException e) {
                    throw new IDBeanException("Carml URISyntaxException : " + carmlfileURL, e);
                }
            }
            if (uri == null) {
                String carmlLoc = (String) System.getProperty(ArisIdConstants.IGF_CARML_LOC);
                if (carmlLoc != null) {
                    String carmlFilePath = carmlLoc + File.separator + carmlFile;
                    file = new File(carmlFilePath);
                    if (file.exists()) uri = file.toURI();
                }
            }
        }
        if (uri == null) {
            throw new IDBeanException("carmlFile not found: " + carmlFile);
        } else {
            return uri;
        }
    }

    protected int getPageSize(Map<String, Object> appCtxMap) {
        int pagesize = 0;
        if (appCtxMap != null && !appCtxMap.isEmpty()) {
            Object page = appCtxMap.get(ArisIdConstants.APP_CTX_PAGESIZE);
            try {
                if (page != null) {
                    if (page instanceof Integer) pagesize = (Integer) page; else pagesize = Integer.parseInt((String) page);
                }
            } catch (java.lang.NumberFormatException ne) {
            }
            if (pagesize < 0) pagesize = 0;
        }
        return pagesize;
    }

    protected IDigitalSubject readDigitalSubject(IPrincipalIdentifier principal, String interactionName, Map<String, Object> appCtxMap) throws NoSuchSubjectException, ConnectionException, PolicyException, SubjectNotUniqueException, IDBeanException {
        Principal user = null;
        String langID = null;
        if (appCtxMap != null) {
            user = (Principal) appCtxMap.get(ArisIdConstants.APP_CTX_AUTHUSER);
            langID = getLocaleValue(appCtxMap.get(ArisIdConstants.APP_CTX_LOCALE));
        }
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        IDigitalSubject subj = null;
        IReadInteraction readInteraction = (IReadInteraction) asvc.getInteraction(interactionName);
        if (readInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Read Interaction " + interactionName);
            if (langID != null) {
                Map<String, IPolicy> map = polHandler.addLocalityConstraint(langID, interactionName, null);
                subj = readInteraction.doGet(principal, map, authUser);
            } else {
                subj = readInteraction.doGet(principal, authUser);
            }
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
        return subj;
    }

    protected IDigitalSubject readDigitalSubject(IPrincipalIdentifier principal, String interactionName, Principal user) throws NoSuchSubjectException, ConnectionException, PolicyException, SubjectNotUniqueException, IDBeanException {
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        IDigitalSubject subj = null;
        IReadInteraction readInteraction = (IReadInteraction) asvc.getInteraction(interactionName);
        if (readInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Read Interaction " + interactionName);
            subj = readInteraction.doGet(principal, authUser);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
        return subj;
    }

    protected IDigitalSubject readDigitalSubject(HttpServletRequest request, String interactionName, Map<String, Object> appCtxMap) throws NoSuchSubjectException, ConnectionException, PolicyException, SubjectNotUniqueException, IDBeanException {
        Principal user = null;
        String langID = null;
        if (appCtxMap != null) {
            user = (Principal) appCtxMap.get(ArisIdConstants.APP_CTX_AUTHUSER);
            langID = getLocaleValue(appCtxMap.get(ArisIdConstants.APP_CTX_LOCALE));
        }
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        IDigitalSubject subj = null;
        IReadInteraction readInteraction = (IReadInteraction) asvc.getInteraction(interactionName);
        if (readInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Read Interaction " + interactionName);
            if (langID != null) {
                Map<String, IPolicy> map = polHandler.addLocalityConstraint(langID, interactionName, null);
                subj = readInteraction.doGetByRequest(request, map, authUser);
            } else subj = readInteraction.doGetByRequest(request, authUser);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
        return subj;
    }

    protected IDigitalSubject readDigitalSubject(HttpServletRequest request, String interactionName, Principal user) throws NoSuchSubjectException, ConnectionException, PolicyException, SubjectNotUniqueException, IDBeanException {
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        IDigitalSubject subj = null;
        IReadInteraction readInteraction = (IReadInteraction) asvc.getInteraction(interactionName);
        if (readInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Read Interaction " + interactionName);
            subj = readInteraction.doGetByRequest(request, authUser);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
        return subj;
    }

    protected IDigitalSubject findDigitalSubject(List<PropertyFilterValue> attrFiltersList, String interactionName, Map<String, Object> appCtxMap) throws IDBeanException, NoSuchSubjectException, SubjectNotUniqueException, InvalidFilterException, ConnectionException, PolicyException {
        Principal user = null;
        String langID = null;
        if (appCtxMap != null) {
            user = (Principal) appCtxMap.get(ArisIdConstants.APP_CTX_AUTHUSER);
            langID = getLocaleValue(appCtxMap.get(ArisIdConstants.APP_CTX_LOCALE));
        }
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        IDigitalSubject subj = null;
        IFindInteraction findInteraction = (IFindInteraction) asvc.getInteraction(interactionName);
        if (findInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Find Interaction " + interactionName);
            if (langID != null) {
                Map<String, IPolicy> map = polHandler.addLocalityConstraint(langID, interactionName, null);
                subj = findInteraction.doFind(getFilter(attrFiltersList, findInteraction.getFilter()), map, authUser);
            } else {
                subj = findInteraction.doFind(getFilter(attrFiltersList, findInteraction.getFilter()), authUser);
            }
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
        return subj;
    }

    protected IDigitalSubject findDigitalSubject(List<PropertyFilterValue> attrFiltersList, String interactionName, Principal user) throws IDBeanException, NoSuchSubjectException, SubjectNotUniqueException, InvalidFilterException, ConnectionException, PolicyException {
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        IDigitalSubject subj = null;
        IFindInteraction findInteraction = (IFindInteraction) asvc.getInteraction(interactionName);
        if (findInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Find Interaction " + interactionName);
            subj = findInteraction.doFind(getFilter(attrFiltersList, findInteraction.getFilter()), authUser);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
        return subj;
    }

    protected IPrincipalIdentifier createDigitalSubject(List<PropertyValue> attrVals, String[] roles, String interactionName, Map<String, Object> appCtxMap) throws IDBeanException, ConnectionException, PolicyException, SubjectNotUniqueException, NoSuchSubjectException {
        if (appCtxMap != null) {
            return createDigitalSubject(attrVals, roles, interactionName, (Principal) appCtxMap.get(ArisIdConstants.APP_CTX_AUTHUSER));
        } else {
            return createDigitalSubject(attrVals, roles, interactionName, (Principal) null);
        }
    }

    protected IPrincipalIdentifier createDigitalSubject(List<PropertyValue> attrVals, String[] roles, String interactionName, Principal user) throws IDBeanException, ConnectionException, PolicyException, SubjectNotUniqueException, NoSuchSubjectException {
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        int numAttrVals = (attrVals == null) ? 0 : attrVals.size();
        AttributeValue[] newAttrVals = new AttributeValue[numAttrVals];
        if (attrVals != null) {
            for (int i = 0; i < numAttrVals; i++) {
                newAttrVals[i] = new AttributeValue(attrVals.get(i).getName(), attrVals.get(i).getStringValues());
                String locale = attrVals.get(i).getLocale();
                if (locale != null) polHandler.addLocalityConstraint(locale, newAttrVals[i]);
            }
        }
        IAddInteraction addInteraction = (IAddInteraction) asvc.getInteraction(interactionName);
        if (addInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        IPrincipalIdentifier principal = null;
        try {
            logr.trace("Calling the Add Interaction " + interactionName);
            principal = addInteraction.doAdd(newAttrVals, roles, authUser);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
        return principal;
    }

    protected void modifyDigitalSubject(IPrincipalIdentifier principal, String attrName, String attrValue, String[] roles, String interactionName, Principal user) throws ConnectionException, PolicyException, NoSuchSubjectException, SubjectNotUniqueException, IDBeanException {
        List<ModPropertyValue> attrVals = new ArrayList<ModPropertyValue>();
        attrVals.add(new ModPropertyValue(attrName, attrValue));
        modifyDigitalSubject(principal, attrVals, roles, interactionName, user);
    }

    protected void modifyDigitalSubject(IPrincipalIdentifier principal, String attrName, String attrValue, String[] roles, String interactionName, Map<String, Object> appCtxMap) throws ConnectionException, PolicyException, NoSuchSubjectException, SubjectNotUniqueException, IDBeanException {
        List<ModPropertyValue> attrVals = new ArrayList<ModPropertyValue>();
        attrVals.add(new ModPropertyValue(attrName, attrValue));
        if (appCtxMap != null) {
            modifyDigitalSubject(principal, attrVals, roles, interactionName, (Principal) appCtxMap.get(ArisIdConstants.APP_CTX_AUTHUSER));
        } else {
            modifyDigitalSubject(principal, attrVals, roles, interactionName, (Principal) null);
        }
    }

    protected void modifyDigitalSubject(IPrincipalIdentifier principal, List<ModPropertyValue> attrVals, String[] roles, String interactionName, Principal user) throws IDBeanException, ConnectionException, PolicyException, NoSuchSubjectException, SubjectNotUniqueException {
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        int numAttrVals = (attrVals == null) ? 0 : attrVals.size();
        AttributeValue[] newAttrVals = null;
        if (numAttrVals > 0) {
            newAttrVals = new AttributeValue[numAttrVals];
            for (int i = 0; i < numAttrVals; i++) {
                ModPropertyValue modProp = attrVals.get(i);
                newAttrVals[i] = new AttributeValue(modProp.getName(), modProp.getStringValues());
                if (modProp.getModOperation() == ModPropertyValue.ADD_VALUE) polHandler.setAddValuesOnModifyConstraint(newAttrVals[i]); else if (modProp.getModOperation() == ModPropertyValue.DELETE_VALUE) polHandler.setDeleteValuesOnModifyConstraint(newAttrVals[i]);
                String locale = attrVals.get(i).getLocale();
                if (locale != null) polHandler.addLocalityConstraint(locale, newAttrVals[i]);
            }
        }
        IModifyInteraction modifyInteraction = (IModifyInteraction) asvc.getInteraction(interactionName);
        if (modifyInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Modify Interaction " + interactionName);
            modifyInteraction.doModify(principal, newAttrVals, roles, authUser);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
    }

    protected void modifyDigitalSubject(IPrincipalIdentifier principal, List<ModPropertyValue> attrVals, String[] roles, String interactionName, Map<String, Object> appCtxMap) throws IDBeanException, ConnectionException, PolicyException, NoSuchSubjectException, SubjectNotUniqueException {
        if (appCtxMap != null) {
            modifyDigitalSubject(principal, attrVals, roles, interactionName, (Principal) appCtxMap.get(ArisIdConstants.APP_CTX_AUTHUSER));
        } else {
            modifyDigitalSubject(principal, attrVals, roles, interactionName, (Principal) null);
        }
    }

    protected void deleteDigitalSubject(IPrincipalIdentifier principal, String interactionName, Map<String, Object> appCtxMap) throws IDBeanException, ConnectionException, PolicyException, NoSuchSubjectException, SubjectNotUniqueException {
        if (appCtxMap != null) {
            deleteDigitalSubject(principal, interactionName, (Principal) appCtxMap.get(ArisIdConstants.APP_CTX_AUTHUSER));
        } else {
            deleteDigitalSubject(principal, interactionName, (Principal) null);
        }
    }

    protected void deleteDigitalSubject(IPrincipalIdentifier principal, String interactionName, Principal user) throws IDBeanException, ConnectionException, PolicyException, NoSuchSubjectException, SubjectNotUniqueException {
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        IDeleteInteraction deleteInteraction = (IDeleteInteraction) asvc.getInteraction(interactionName);
        if (deleteInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Delete Interaction " + interactionName);
            deleteInteraction.doDelete(principal, authUser);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
    }

    protected IResultSet searchDigitalSubject(List<PropertyFilterValue> attrFiltersList, String interactionName, Map<String, Object> appCtxMap) throws IDBeanException, ConnectionException, PolicyException, NoSuchSubjectException, SubjectNotUniqueException, InvalidFilterException {
        Principal user = null;
        String langID = null;
        if (appCtxMap != null) {
            user = (Principal) appCtxMap.get(ArisIdConstants.APP_CTX_AUTHUSER);
            langID = getLocaleValue(appCtxMap.get(ArisIdConstants.APP_CTX_LOCALE));
        }
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        ISearchInteraction searchInteraction = (ISearchInteraction) asvc.getInteraction(interactionName);
        if (searchInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Search Interaction " + interactionName);
            if (langID != null) {
                Map<String, IPolicy> map = polHandler.addLocalityConstraint(langID, interactionName, null);
                return searchInteraction.doSearch(getFilter(attrFiltersList, searchInteraction.getFilter()), map, authUser);
            } else return searchInteraction.doSearch(getFilter(attrFiltersList, searchInteraction.getFilter()), authUser);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
    }

    protected IResultSet searchDigitalSubject(List<PropertyFilterValue> attrFiltersList, String interactionName, Principal user) throws IDBeanException, ConnectionException, PolicyException, NoSuchSubjectException, SubjectNotUniqueException, InvalidFilterException {
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        ISearchInteraction searchInteraction = (ISearchInteraction) asvc.getInteraction(interactionName);
        if (searchInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Search Interaction " + interactionName);
            return searchInteraction.doSearch(getFilter(attrFiltersList, searchInteraction.getFilter()), authUser);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
    }

    protected int getSearchInteractionPageSize(String interactionName) throws IDBeanException {
        ISearchInteraction searchInteraction = (ISearchInteraction) asvc.getInteraction(interactionName);
        if (searchInteraction == null) throw new IDBeanException("CARML file does not have the method " + interactionName);
        return searchInteraction.getPageSize();
    }

    protected boolean compareDigitalSubject(IPrincipalIdentifier principal, List<PropertyFilterValue> attrFiltersList, String interactionName, Principal user) throws IDBeanException, ConnectionException, PolicyException, NoSuchSubjectException, SubjectNotUniqueException, InvalidFilterException {
        Subject authUser = null;
        if (user != null) {
            authUser = new Subject();
            authUser.getPrincipals().add(user);
        }
        ICompareInteraction compareInteraction = (ICompareInteraction) asvc.getInteraction(interactionName);
        if (compareInteraction == null) throw new IDBeanException("CARML file does not have the interaction " + interactionName);
        try {
            logr.trace("Calling the Compare Interaction " + interactionName);
            return compareInteraction.doCompare(principal, getFilter(attrFiltersList, compareInteraction.getFilter()), authUser);
        } catch (NoSuchContextException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (SchemaException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (MappingException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (DeclarationException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        } catch (IGFException e) {
            logr.debug(e.toString());
            throw new IDBeanException(e);
        }
    }

    /**
     * Return TRUE if the given property name is an Attribute
     *
     * @param name
     *            property name
     * @return true if the property is an Attribute, otherwise return false
     */
    public boolean isAttribute(String name) {
        if (null != asvc.getSchemaManager().getAttribute(name)) return true; else return false;
    }

    /**
     * Return TRUE if the given property name is a Predicate
     *
     * @param name
     *            property name
     * @return true if the property is a Predicate, otherwise return false
     */
    public boolean isPredicate(String name) {
        if (null != asvc.getSchemaManager().getPredicate(name)) return true; else return false;
    }

    /**
     * Return TRUE if the given property name is a Role
     *
     * @param name
     *            property name
     * @return true if the property is a Role, otherwise return false
     */
    public boolean isRole(String name) {
        if (null != asvc.getSchemaManager().getRole(name)) return true; else return false;
    }

    /**
     * Gets the Attribute Definition as specified in the CARML declaration file
     *
     * @param  attributeName
     *           Name of the attribute for which the Attribute Definition
     *           to be returned
     * @return Attribue Definition
     */
    public AttributeDef getAttributeDef(String attributeName) {
        SchemaManager sm = asvc.getSchemaManager();
        return sm.getAttribute(attributeName);
    }

    /**
     * Gets the Predicate Definition as specified in the CARML declaration file
     *
     * @param  predicateName
     *           Name of the predicate for which the Predicate Definition
     *           to be returned
     * @return Predicate Definition
     */
    public PredicateDef getPredicateDef(String predicateName) {
        SchemaManager sm = asvc.getSchemaManager();
        return sm.getPredicate(predicateName);
    }

    /**
     * Gets the Role Definition as specified in the CARML declaration file
     *
     * @param  roleName
     *           Name of the role for which the Role Definition
     *           to be returned
     * @return Role Definition
     */
    public RoleDef getRoleDef(String roleName) {
        SchemaManager sm = asvc.getSchemaManager();
        return sm.getRole(roleName);
    }

    /**
     * Gets list of all attributes' names referred in all CARML interactions
     *
     * @return list of attribute names
     */
    public List<String> getAllAttributeNames() {
        return getAllAttributeNames(null);
    }

    /**
     * Gets list of all attributes' name referred in a given interaction
     *
     * @param interactionName
     *            Interaction in the CARML file
     * @return list of attribute names
     */
    public List<String> getAllAttributeNames(String interactionName) {
        List<String> attrNames = new ArrayList<String>();
        if (interactionName == null || interactionName.equals("")) {
            SchemaManager sm = asvc.getSchemaManager();
            Collection<AttributeDef> attrs = sm.getAttributes();
            Iterator<AttributeDef> iter = attrs.iterator();
            while (iter.hasNext()) {
                AttributeDef attr = iter.next();
                attrNames.add(attr.getNameId());
            }
        } else {
            IInteraction interaction = asvc.getInteraction(interactionName);
            if (interaction != null) {
                Set<String> ids = interaction.getAttributeIds();
                Iterator<String> iter = ids.iterator();
                while (iter.hasNext()) {
                    String id = iter.next();
                    attrNames.add(id);
                }
            }
        }
        return attrNames;
    }

    /**
     * Gets list of all predicates' names referred in all CARML interactions
     *
     * @return list of predicate names
     */
    public List<String> getAllPredicateNames() {
        return getAllPredicateNames(null);
    }

    /**
     * Gets list of all predicates' name referred in a given interaction
     *
     * @param interactionName
     *            Interaction in the CARML file
     * @return list of predicate names
     */
    public List<String> getAllPredicateNames(String interactionName) {
        List<String> predicateNames = new ArrayList<String>();
        if (interactionName == null || interactionName.equals("")) {
            SchemaManager sm = asvc.getSchemaManager();
            Collection<PredicateDef> attrs = sm.getPredicates();
            Iterator<PredicateDef> iter = attrs.iterator();
            while (iter.hasNext()) {
                PredicateDef attr = iter.next();
                predicateNames.add(attr.getNameId());
            }
        } else {
            IInteraction interaction = asvc.getInteraction(interactionName);
            if (interaction != null) {
                Set<String> ids = interaction.getPredicateIds();
                Iterator<String> iter = ids.iterator();
                while (iter.hasNext()) {
                    String id = iter.next();
                    predicateNames.add(id);
                }
            }
        }
        return predicateNames;
    }

    /**
     * Gets list of all roles referred in all CARML interactions
     *
     * @return list of roles
     */
    public List<String> getAllRoles() {
        return getAllRoles(null);
    }

    /**
     * Gets list of all roles referred in a given interaction
     *
     * @param interactionName
     *            Interaction in the CARML file
     * @return list of roles
     */
    public List<String> getAllRoles(String interactionName) {
        List<String> roleNames = new ArrayList<String>();
        if (interactionName == null || interactionName.equals("")) {
            SchemaManager sm = asvc.getSchemaManager();
            Collection<RoleDef> attrs = sm.getRoles();
            Iterator<RoleDef> iter = attrs.iterator();
            while (iter.hasNext()) {
                RoleDef attr = iter.next();
                roleNames.add(attr.getNameId());
            }
        } else {
            IInteraction interaction = asvc.getInteraction(interactionName);
            if (interaction != null) {
                Set<String> ids = interaction.getRoleIds();
                Iterator<String> iter = ids.iterator();
                while (iter.hasNext()) {
                    String id = iter.next();
                    roleNames.add(id);
                }
            }
        }
        return roleNames;
    }

    /**
     * Gets list of all properties' (attributes, predicates and roles) names
     * referred in all CARML interactions
     *
     * @return list of property names
     */
    public List<String> getAllPropertyNames() {
        return getAllPropertyNames(null);
    }

    /**
     * Gets list of all properties' (attributes, predicates and roles) names
     * referred in a given interaction
     *
     * @param interactionName
     *            Interaction in the CARML file
     * @return list of property names
     */
    public List<String> getAllPropertyNames(String interactionName) {
        List<String> propertyNames = new ArrayList<String>();
        propertyNames.addAll(getAllAttributeNames(interactionName));
        propertyNames.addAll(getAllPredicateNames(interactionName));
        propertyNames.addAll(getAllRoles(interactionName));
        return propertyNames;
    }

    /**
     * Gets all interaction names in the CARML file
     *
     * @return list of interaction names
     */
    public List<String> getAllInteractions() {
        return getAllInteractions(null);
    }

    /**
     * Gets list of all interaction names in a CARML file for requested
     * interaction Type
     *
     * @param interactionType
     *            Interaction Type (READ, FIND, SEARCH, COMPARE, ADD, MODIFY or
     *            DELETE)
     * @return list of interaction names
     */
    public List<String> getAllInteractions(String interactionType) {
        List<String> interactionNames = new ArrayList<String>();
        Collection<IInteraction> interactions = asvc.getCarmlDoc().getInteractions();
        Iterator<IInteraction> iter = interactions.iterator();
        while (iter.hasNext()) {
            IInteraction interaction = iter.next();
            if ((interactionType == null) || (interactionType.equals("")) || (interactionType.equalsIgnoreCase("ADD") && interaction.isAdd()) || (interactionType.equalsIgnoreCase("FIND") && interaction.isFind()) || (interactionType.equalsIgnoreCase("COMPARE") && interaction.isCompare()) || (interactionType.equalsIgnoreCase("DELETE") && interaction.isDelete()) || (interactionType.equalsIgnoreCase("MODIFY") && interaction.isModify()) || (interactionType.equalsIgnoreCase("SEARCH") && interaction.isSearch()) || (interactionType.equalsIgnoreCase("READ") && interaction.isRead())) {
                interactionNames.add(interaction.getNameId());
            } else {
            }
        }
        return interactionNames;
    }

    private List<IAttributeValue> getFilter(List<PropertyFilterValue> attrFilters, Filter filter) throws InvalidFilterException {
        logr.trace("Converting the filter to ArisId filter");
        List<IAttributeValue> outAttrFilters = null;
        if (filter == null || (outAttrFilters = filter.getCompareAttrValues()) == null) return null;
        List<PropertyFilterValue> inAttrFilters = new ArrayList<PropertyFilterValue>(attrFilters);
        Iterator<IAttributeValue> iter = outAttrFilters.iterator();
        while (iter.hasNext()) {
            IAttributeValue attrFilterVal = iter.next();
            for (int i = 0; inAttrFilters != null && i < inAttrFilters.size(); i++) {
                String attrName = inAttrFilters.get(i).getName();
                if (attrFilterVal.getNameIdRef().equals(attrName)) {
                    PropertyFilterValue propValue = inAttrFilters.remove(i);
                    List<String> values = propValue.getStringValues();
                    for (int j = 0; j < values.size(); j++) attrFilterVal.add(values.get(j));
                    if (attrFilterVal instanceof AttributeFilterValue) {
                        AttributeFilterValue fval = (AttributeFilterValue) attrFilterVal;
                        if (fval.isDynamic()) {
                            if (propValue.getCompareOperator() != null) fval.setCompareOperator(propValue.getCompareOperator()); else fval.setCompareOperator(AttributeFilter.OP_CONTAINS);
                        }
                    }
                    break;
                }
            }
        }
        return outAttrFilters;
    }

    /**
     * Releases the ArisId service handle
     */
    public synchronized void dispose() {
        if (asvc == null) return;
        Integer refCnt = asvcRefCntMap.get(asvc);
        asvcRefCntMap.put(asvc, --refCnt);
        logr.trace("Decremented ArisId reference count to " + refCnt);
        if (refCnt == 0) {
            asvcRefCntMap.remove(asvc);
            for (String key : asvcMap.keySet()) {
                if (asvcMap.get(key) == asvc) {
                    try {
                        asvc.close();
                    } catch (Exception e) {
                    }
                    asvcMap.remove(key);
                    logr.trace("Released ArisId service instance");
                    break;
                }
            }
        }
        asvc = null;
        polHandler = null;
    }

    private String getLocaleValue(Object locale) {
        String localeStr = null;
        if (locale != null) {
            if (locale instanceof Locale) {
                Locale loc = (Locale) locale;
                localeStr = getLocaleString(loc);
            } else {
                localeStr = locale.toString();
            }
        }
        return localeStr;
    }

    protected static String getLocaleString(Locale locale) {
        String localeStr = null;
        String country = locale.getCountry();
        String lang = locale.getLanguage();
        if (lang != null && !lang.equals("")) {
            if (country != null && !country.equals("")) localeStr = lang + "-" + country; else localeStr = lang;
        }
        return localeStr;
    }

    @Override
    public void finalize() throws Throwable {
        dispose();
        super.finalize();
    }
}
