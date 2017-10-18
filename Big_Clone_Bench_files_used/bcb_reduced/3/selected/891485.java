package com.yubarta.docman.admin;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import com.yubarta.docman.impl.MySesameLocalService;
import org.apache.catalina.Context;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.openrdf.model.Graph;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.config.AccessDeniedException;
import org.openrdf.sesame.config.ConfigurationException;
import org.openrdf.sesame.config.UnknownRepositoryException;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.query.QueryResultsGraphBuilder;
import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.SesameService;

public class DocmanRealm extends RealmBase implements Realm {

    private ServletContext servletContext;

    private String docmanSessionLabel;

    private String sUserBase;

    private String sGroupBase;

    private String sSesameUser;

    private String sSesamePass;

    private String sSesameUrl;

    private String sSesameRepName;

    private String sSesameConfigFile;

    private boolean bSesameLocal;

    public static final String sDOCMAN_PASSWORD = "docmanPassword";

    public static final String sVB_BASE = "http://yubarta.com/md#";

    public static final URI PROP_PASSWDMD5 = new URIImpl("http://yubarta.com/md#", "md5Password");

    public static final URI PROP_USERBELONGSTOGROUP = new URIImpl("http://yubarta.com/md#", "userBelongsToGroup");

    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    public Principal authenticate(String username, byte[] credentials) {
        return authenticate(username, new String(credentials));
    }

    public Principal authenticate(String username, String credentials) {
        if (username == null || credentials == null) {
            return null;
        }
        if (!verifyCredentials(username, credentials)) {
            System.out.println("DocmanRealm: Fallo de autentificación en authenticate()");
            return null;
        }
        return new GenericPrincipal(this, username, credentials);
    }

    public Principal authenticate(String username, String digest, String nonce, String nc, String cnonce, String qop, String realm, String md5a2) {
        throw new Error("DocmanRealm: método authenticate -digest- no implementado");
    }

    public Principal authenticate(X509Certificate[] certs) {
        throw new Error("DocmanRealm: método authenticate -X509- no implementado");
    }

    public void backgroundProcess() {
    }

    public boolean hasResourcePermission(Request request, Response response, SecurityConstraint[] constraint, Context context) throws IOException {
        HttpSession session = request.getSession();
        if (request.getUserPrincipal() != null && session.getAttribute("docmanPassword") == null) {
            if (!(request.getUserPrincipal() instanceof GenericPrincipal)) {
                throw new Error("DocmanRealm: El principal debe contener el password para autenticar");
            }
            GenericPrincipal gp = (GenericPrincipal) request.getUserPrincipal();
            session.setAttribute("docmanPassword", gp.getPassword());
        }
        return super.hasResourcePermission(request, response, constraint, context);
    }

    public String getName() {
        return "pqRealm";
    }

    public String getPassword(String username) {
        throw new Error("DocmanRealm: método getPassword no implementado");
    }

    public Principal getPrincipal(String username) {
        throw new Error("DocmanRealm: método getPrincipal no implementado");
    }

    public String getInfo() {
        return "<Realm de autenticación mediante Docman>/<1.0>";
    }

    public boolean hasRole(Principal principal, String role) {
        return userBelongsToGroup(principal.getName(), role);
    }

    private ServletContext getServletContext() {
        if (servletContext == null) {
            Context catalinaContext;
            if (getContainer() instanceof Context) {
                catalinaContext = (Context) getContainer();
            } else {
                throw new Error("DocmanRealm: El Realm debe estar dentro del Context en server.xml");
            }
            servletContext = catalinaContext.getServletContext();
        }
        return servletContext;
    }

    public String getUserBase() {
        return sUserBase;
    }

    public void setUserBase(String sss) {
        sUserBase = sss;
    }

    public String getGroupBase() {
        return sGroupBase;
    }

    public void setGroupBase(String sss) {
        sGroupBase = sss;
    }

    public String getSesameUser() {
        return sSesameUser;
    }

    public void setSesameUser(String sss) {
        sSesameUser = sss;
    }

    public String getSesamePass() {
        return sSesamePass;
    }

    public void setSesamePass(String sss) {
        sSesamePass = sss;
    }

    public String getSesameUrl() {
        return sSesameUrl;
    }

    public void setSesameUrl(String sss) {
        sSesameUrl = sss;
    }

    public String getSesameRepName() {
        return sSesameRepName;
    }

    public void setSesameRepName(String sss) {
        sSesameRepName = sss;
    }

    public String getSesameConfigFile() {
        return sSesameConfigFile;
    }

    public void setSesameConfigFile(String sss) {
        sSesameConfigFile = sss;
    }

    public String getSesameLocal() {
        return new StringBuilder().append("").append(bSesameLocal).toString();
    }

    public void setSesameLocal(String sss) {
        bSesameLocal = "true".equals(sss);
    }

    public boolean verifyCredentials(String sUser, String sPaswd) {
        URI uuu = buildUserUri(sUser);
        Graph gugu = getUserMd(uuu);
        org.openrdf.model.Literal liPaswd = gugu.getValueFactory().createLiteral(md5Encode(sPaswd));
        return gugu.contains(uuu, PROP_PASSWDMD5, liPaswd);
    }

    public boolean userBelongsToGroup(String sUser, String sGroup) {
        URI uuu = buildUserUri(sUser);
        Graph gugu = getUserMd(uuu);
        URI uGroup = gugu.getValueFactory().createURI(buildGroupUri(sGroup).toString());
        return gugu.contains(uuu, PROP_USERBELONGSTOGROUP, uGroup);
    }

    private Graph getUserMd(URI uuu) {
        String sSerql = new StringBuilder().append("CONSTRUCT DISTINCT * FROM  {pp1:").append(uuu.getLocalName()).append("} prop {value}, ").append(" {pp1:").append(uuu.getLocalName()).append("} rdf:type {pp2:User} ").append("USING NAMESPACE ").append(" pp2=<http://yubarta.com/md#>, ").append(" pp1=<").append(uuu.getNamespace()).append(">").toString();
        QueryResultsGraphBuilder myList = new QueryResultsGraphBuilder();
        Graph graph;
        try {
            SesameRepository mdRep = connectToSesame();
            mdRep.performGraphQuery(QueryLanguage.SERQL, sSerql, myList);
            graph = myList.getGraph();
        } catch (Exception eee) {
            throw new RuntimeException("Error conectando a sesame", eee);
        }
        return graph;
    }

    protected SesameRepository connectToSesame() throws IOException, UnknownRepositoryException, ConfigurationException, AccessDeniedException {
        String sBase = "";
        String sConf = getSesameConfigFile();
        if (!sConf.startsWith("/")) {
            sBase = new StringBuilder().append(getServletContext().getRealPath("")).append("/WEB-INF/").toString();
        }
        SesameService service = (bSesameLocal ? (SesameService) MySesameLocalService.getService(sBase, sConf) : Sesame.getService(new URL(sSesameUrl)));
        service.login(sSesameUser, sSesamePass);
        return service.getRepository(sSesameRepName);
    }

    private URI buildGroupUri(String sGroup) {
        return new URIImpl(sGroupBase, sGroup);
    }

    private URI buildUserUri(String sUser) {
        return new URIImpl(sUserBase, sUser);
    }

    private static String md5Encode(String pass) {
        String string;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(pass.getBytes());
            byte[] result = md.digest();
            string = bytes2hexStr(result);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("La libreria java.security no implemente MD5");
        }
        return string;
    }

    private static String bytes2hexStr(byte[] arr) {
        char[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        int len = arr.length;
        StringBuffer sb = new StringBuffer(len * 2);
        for (int i = 0; i < len; i++) {
            int hi = arr[i] >>> 4 & 0xf;
            sb.append(hex[hi]);
            int low = arr[i] & 0xf;
            sb.append(hex[low]);
        }
        return sb.toString();
    }
}
