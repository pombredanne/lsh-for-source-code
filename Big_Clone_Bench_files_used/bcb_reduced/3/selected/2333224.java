package org.geogrid.aist.credential.services.security.gss.impl;

import org.geogrid.aist.credential.services.security.gss.CredentialContext;
import org.geogrid.aist.credential.services.security.gss.CredentialException;
import org.geogrid.aist.credential.services.security.gss.CredentialManagerService;
import org.gridsphere.services.core.user.User;
import org.gridsphere.portlet.service.spi.PortletServiceFactory;
import org.gridsphere.portlet.service.spi.PortletServiceConfig;
import org.gridsphere.portlet.service.spi.PortletServiceProvider;
import org.gridsphere.portlet.service.PortletServiceUnavailableException;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import java.util.Date;
import java.util.Collections;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.ServletContext;
import org.hibernate.*;

/**
 * Implements the credential manager service interface.
 */
public class CredentialManagerServiceImpl implements CredentialManagerService, PortletServiceProvider {

    private static Log log = LogFactory.getLog(CredentialManagerServiceImpl.class);

    private static Hashtable credentials = new Hashtable();

    private static Hashtable credentialFiles = new Hashtable();

    private static boolean saveCredentialsToFile = false;

    private static String activeCredentialDir = null;

    private PersistenceManager pm = null;

    private static final String MAPPING_FILE = "/WEB-INF/persistence/CredentialContextImpl.hbm.xml";

    private static final String HIBERNATE_PROPERTIES = "/WEB-INF/persistence/hibernate.properties";

    public void init(PortletServiceConfig config) throws PortletServiceUnavailableException {
        ServletContext ctx = config.getServletContext();
        try {
            String mappingFile = ctx.getRealPath(MAPPING_FILE);
            String hibernateProps = ctx.getRealPath(HIBERNATE_PROPERTIES);
            this.pm = new PersistenceManager(mappingFile, hibernateProps);
        } catch (Exception e) {
            throw new PortletServiceUnavailableException(e);
        }
        String configCredentialToFile = config.getInitParameter("SaveCredentialsToFile");
        log.debug("configCredentialToFile = " + configCredentialToFile);
        if (configCredentialToFile == null || configCredentialToFile.equals("") || configCredentialToFile.equalsIgnoreCase("false") || configCredentialToFile.equalsIgnoreCase("no")) {
            log.info("Credentials will not be saved to files");
            return;
        }
        saveCredentialsToFile = true;
        String configCredentialDir = config.getInitParameter("ActiveCredentialDir");
        if (configCredentialDir == null || configCredentialDir.equals("")) {
            try {
                File tempFile = File.createTempFile("temp", "temp");
                activeCredentialDir = tempFile.getParent();
            } catch (IOException e) {
                throw new PortletServiceUnavailableException(e.getMessage());
            }
        } else {
            activeCredentialDir = configCredentialDir;
        }
        if (!activeCredentialDir.endsWith(File.separator)) {
            activeCredentialDir += File.separator;
        }
        log.info("Credentials will be saved to files in " + activeCredentialDir);
    }

    public void destroy() {
        pm.destroy();
    }

    public List getCredentialContexts(User user) {
        StringBuffer sb = new StringBuffer("from ");
        sb.append(CredentialContextImpl.class.getName());
        sb.append(" as cred where cred.UserOid = :useroid ");
        List list = null;
        try {
            Session session = pm.getSession();
            Query q = session.createQuery(sb.toString());
            q.setString("useroid", user.getID());
            list = q.list();
        } catch (HibernateException e) {
            log.error(e);
        } finally {
            pm.closeSession();
        }
        log.debug("Query Result: " + list);
        if (list == null) {
            return Collections.emptyList();
        } else {
            return list;
        }
    }

    public CredentialContext getCredentialContext(String oid) {
        StringBuffer sb = new StringBuffer("from ");
        sb.append(CredentialContextImpl.class.getName());
        sb.append(" as cred where cred.oid = :oid ");
        List list = null;
        try {
            Session session = pm.getSession();
            Query q = session.createQuery(sb.toString());
            q.setString("oid", oid);
            list = q.list();
        } catch (HibernateException e) {
            log.error(e);
        } finally {
            pm.closeSession();
        }
        log.debug("Query Result: " + list);
        if (list != null && !list.isEmpty()) {
            return (CredentialContext) list.get(0);
        } else {
            return null;
        }
    }

    public CredentialContext getCredentialContextByDn(String dn) {
        StringBuffer sb = new StringBuffer("from ");
        sb.append(CredentialContextImpl.class.getName());
        sb.append(" as cred where cred.Dn = :dn ");
        List list = null;
        try {
            Session session = pm.getSession();
            Query q = session.createQuery(sb.toString());
            q.setString("dn", dn);
            list = q.list();
        } catch (HibernateException e) {
            log.error(e);
        } finally {
            pm.closeSession();
        }
        log.debug("Query Result: " + list);
        if (list != null && !list.isEmpty()) {
            return (CredentialContext) list.get(0);
        } else {
            return null;
        }
    }

    public CredentialContext createCredentialContext(User user, String dn) throws CredentialException {
        return new CredentialContextImpl(user, dn);
    }

    public void saveCredentialContext(CredentialContext context) throws CredentialException {
        CredentialContextImpl contextImpl = (CredentialContextImpl) context;
        Date now = new Date();
        if (context.getOid() == null) {
            log.debug("Creating credential context for " + context.getDn());
            contextImpl.setDateCreated(now);
            saveObject(context);
            GSSCredential credential = context.getCredential();
            if (credential != null) {
                setCredential(context.getDn(), credential);
            }
        } else {
            log.debug("Updating credential context for " + context.getDn());
            contextImpl.setDateLastUpdated(now);
            updateObject(context);
        }
    }

    private void saveObject(Object obj) {
        try {
            pm.beginTransaction();
            Session session = pm.getSession();
            session.saveOrUpdate(obj);
            pm.commitTransaction();
        } catch (HibernateException e) {
            log.error(e);
        } finally {
            pm.closeSession();
        }
    }

    private void updateObject(Object obj) {
        try {
            pm.beginTransaction();
            Session session = pm.getSession();
            session.update(obj);
            pm.commitTransaction();
        } catch (HibernateException e) {
            log.error(e);
        } finally {
            pm.closeSession();
        }
    }

    private void deleteObject(Object obj) {
        try {
            pm.beginTransaction();
            Session session = pm.getSession();
            session.delete(obj);
            pm.commitTransaction();
        } catch (HibernateException e) {
            log.error(e);
        } finally {
            pm.closeSession();
        }
    }

    public void deleteCredentialContext(CredentialContext context) {
        deleteObject(context);
    }

    public CredentialContext activateCredentialContext(GSSCredential credential) throws CredentialException {
        String credDn = null;
        try {
            credDn = credential.getName().toString();
        } catch (GSSException e) {
            log.warn(e.getMessage());
            throw new CredentialException("Unknown error occured", e);
        }
        CredentialContext context = getCredentialContextByDn(credDn);
        if (context == null) {
            throw new CredentialException("No credentialContext context found with dn " + credDn);
        }
        context.activate(credential);
        saveCredentialContext(context);
        return context;
    }

    public void deactivateCredentialContexts(User user) {
        List userContexts = getCredentialContexts(user);
        for (Iterator contexts = userContexts.iterator(); contexts.hasNext(); ) {
            CredentialContextImpl contextImpl = (CredentialContextImpl) contexts.next();
            contextImpl.deactivate();
        }
    }

    public void deactivateCredentialContext(CredentialContext context) {
        CredentialContextImpl contextImpl = (CredentialContextImpl) context;
        contextImpl.deactivate();
    }

    public List getActiveCredentialContexts(User user) {
        List userContexts = getCredentialContexts(user);
        List activeContexts = new Vector(1);
        for (Iterator contexts = userContexts.iterator(); contexts.hasNext(); ) {
            CredentialContext context = (CredentialContext) contexts.next();
            if (context.isActive()) {
                activeContexts.add(context);
            }
        }
        return activeContexts;
    }

    public List getActiveCredentials(User user) {
        List userContexts = getCredentialContexts(user);
        List activeCredentials = new Vector(1);
        for (Iterator contexts = userContexts.iterator(); contexts.hasNext(); ) {
            CredentialContext context = (CredentialContext) contexts.next();
            if (context.isActive()) {
                GSSCredential credential = context.getCredential();
                activeCredentials.add(credential);
            }
        }
        return activeCredentials;
    }

    public CredentialContext getDefaultCredentialContext(User user) {
        List activeCredentials = getActiveCredentialContexts(user);
        if (activeCredentials.size() > 0) {
            return (CredentialContext) activeCredentials.get(0);
        }
        return null;
    }

    public GSSCredential getDefaultCredential(User user) {
        List activeCredentials = getActiveCredentials(user);
        if (activeCredentials.size() > 0) {
            return (GSSCredential) activeCredentials.get(0);
        }
        return null;
    }

    public List getActiveCredentialFileNames(User user) {
        List activeCredentialFileNames = new Vector(1);
        if (saveCredentialsToFile) {
            List userContexts = getCredentialContexts(user);
            for (Iterator contexts = userContexts.iterator(); contexts.hasNext(); ) {
                CredentialContext context = (CredentialContext) contexts.next();
                if (context.isActive()) {
                    String fileName = getCredentialFileName(context.getDn());
                    if (fileName != null) {
                        activeCredentialFileNames.add(fileName);
                    }
                }
            }
        }
        return activeCredentialFileNames;
    }

    public boolean getSaveActiveCredentialsToFile() {
        return saveCredentialsToFile;
    }

    public void setSaveActiveCredentialsToFile(boolean flag) {
        saveCredentialsToFile = flag;
    }

    public String getActiveCredentialDirectory() {
        return activeCredentialDir;
    }

    public void setActiveCredentialDirectory(String dir) {
        activeCredentialDir = dir;
    }

    public String getActiveCredentialFileName(String dn) {
        if (saveCredentialsToFile) {
            CredentialContext context = getCredentialContextByDn(dn);
            if (context.isActive()) {
                return getCredentialFileName(context.getDn());
            }
        }
        return null;
    }

    /**
     * Returns the credential in our hash table with the given DN as the key.
     * @param dn The DN.
     * @return The credential, null if not found.
     */
    protected static GSSCredential getCredential(String dn) {
        return (GSSCredential) credentials.get(dn);
    }

    /**
     * Stores the credential in our hash table with the given DN as the key.
     * @param dn The DN.
     * @param credential The credential.
     */
    protected static void setCredential(String dn, GSSCredential credential) throws CredentialException {
        if (dn == null) {
            log.error("argment DN is NULL");
            return;
        }
        if (credential == null) {
            log.error("argment credential is NULL");
            return;
        }
        if (saveCredentialsToFile) {
            try {
                String fileName = createFile(dn, credential);
                if (fileName != null) {
                    credentialFiles.put(dn, fileName);
                }
            } catch (IOException e) {
                log.error("Unable to save credential to file", e);
                throw new CredentialException("Unable to save credential to file");
            }
        }
        credentials.put(dn, credential);
    }

    /**
     * Stores the credential from our hash table with the DN.
     * @param dn The DN.
     */
    protected static void removeCredential(String dn) {
        if (saveCredentialsToFile) {
            String filename = getCredentialFileName(dn);
            if (filename != null) {
                File file = new File(filename);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        credentials.remove(dn);
        credentialFiles.remove(dn);
    }

    /**
     * Returns the name of the file to which credentials with the given 
     * dn are stored.
     * @param dn The DN.
     * @return The file name, null if not found.
     */
    protected static String getCredentialFileName(String dn) {
        return (String) credentialFiles.get(dn);
    }

    protected static String createFile(String dn, GSSCredential credential) throws IOException {
        if (!(credential instanceof GlobusGSSCredentialImpl)) {
            log.error("Could not create credential file.");
            return null;
        }
        GlobusGSSCredentialImpl gssCredential = (GlobusGSSCredentialImpl) credential;
        String fileName = getCredentialFileName(dn);
        File file = null;
        if (fileName == null) {
            fileName = activeCredentialDir + DN2MD5Filename(gssCredential.getGlobusCredential().getIdentityCertificate());
            file = new File(fileName);
        } else {
            file = new File(fileName);
        }
        if (file.exists()) {
            file.delete();
        }
        log.info("Saving credential [" + dn + "] to file " + fileName);
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(file);
            gssCredential.getGlobusCredential().save(fileOut);
        } finally {
            if (fileOut != null) {
                fileOut.close();
            }
        }
        return fileName;
    }

    protected static String DN2MD5Filename(X509Certificate cert) {
        byte[] issuer = cert.getIssuerDN().getName().getBytes();
        byte[] user = cert.getSubjectDN().getName().getBytes();
        String token = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(issuer);
            md5.update(user);
            token = toHex(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("No such algorithm " + e.getMessage(), e);
        }
        return token;
    }

    /**
     * Return an 8 byte representation of the 32 byte MD5 digest
     *
     * @param digest the message digest
     * @return String 8 byte hexadecimal
     */
    protected static String toHex(byte[] digest) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            buf.append(Integer.toHexString((int) digest[i] & 0x00FF));
        }
        return buf.toString();
    }
}
