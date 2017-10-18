package gr.gousios.ereceipt;

import gr.gousios.ereceipt.model.ClientApp;
import gr.gousios.ereceipt.model.Error;
import gr.gousios.ereceipt.model.User;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class ClientAppManager extends HttpServlet {

    private static final Logger log = Logger.getLogger("appmgr");

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("utf-8");
        EntityManager em = EMF.get().createEntityManager();
        String url = req.getRequestURL().toString();
        String key = req.getParameter("key");
        String format = req.getParameter("format");
        if (!isAuth(key, resp, em, format)) return;
        String[] urlParts = url.split("app/");
        if (urlParts.length < 2) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (format != null && format.equals("xml")) resp.getWriter().print(Error.noAppId(null).toXML(em)); else resp.getWriter().print(Error.noAppId(null).toJSON(em));
            em.close();
            return;
        }
        String appId = urlParts[1].split("/")[0];
        ClientApp ca = ClientApp.fromKey(em, appId);
        if (ca == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            if (format != null && format.equals("xml")) resp.getWriter().print(Error.noAppId(null).toXML(em)); else resp.getWriter().print(Error.noAppId(null).toJSON(em));
        } else {
            resp.setStatus(HttpServletResponse.SC_OK);
            if (format != null && format.equals("xml")) resp.getWriter().print(ca.toXML(em)); else resp.getWriter().print(ca.toJSON(em));
        }
        em.close();
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("utf-8");
        EntityManager em = EMF.get().createEntityManager();
        String url = req.getRequestURL().toString();
        String key = req.getParameter("key");
        String format = req.getParameter("format");
        if (key == null || !key.equals(Keys.APPREGKEY)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (format != null && format.equals("xml")) resp.getWriter().print(Error.notAuthorised("").toXML(em)); else resp.getWriter().print(Error.notAuthorised("").toJSON(em));
            em.close();
            return;
        }
        String appname = req.getParameter("name");
        if (appname == null || appname.equals("")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            if (format != null && format.equals("xml")) resp.getWriter().print(Error.noAppId(null).toXML(em)); else resp.getWriter().print(Error.noAppId(null).toJSON(em));
            em.close();
            return;
        }
        StringBuffer appkey = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            String api = System.nanoTime() + "" + System.identityHashCode(this) + "" + appname;
            algorithm.update(api.getBytes());
            byte[] digest = algorithm.digest();
            for (int i = 0; i < digest.length; i++) {
                appkey.append(Integer.toHexString(0xFF & digest[i]));
            }
        } catch (NoSuchAlgorithmException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (format != null && format.equals("xml")) resp.getWriter().print(Error.unknownError().toXML(em)); else resp.getWriter().print(Error.unknownError().toJSON(em));
            log.severe(e.toString());
            em.close();
            return;
        }
        ClientApp app = new ClientApp();
        app.setName(appname);
        app.setKey(appkey.toString());
        app.setNumreceipts(0L);
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            em.persist(app);
            tx.commit();
        } catch (Throwable t) {
            log.severe("Error persisting application " + app.getName() + ": " + t.getMessage());
            tx.rollback();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (format != null && format.equals("xml")) resp.getWriter().print(Error.unknownError().toXML(em)); else resp.getWriter().print(Error.unknownError().toJSON(em));
            em.close();
            return;
        }
        resp.setStatus(HttpServletResponse.SC_CREATED);
        if (format != null && format.equals("xml")) resp.getWriter().print(app.toXML(em)); else resp.getWriter().print(app.toJSON(em));
        em.close();
    }

    private boolean isAuth(String key, HttpServletResponse resp, EntityManager em, String format) throws IOException {
        if (key == null || User.fromApiKey(em, key) == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (format != null && format.equals("xml")) resp.getWriter().print(Error.notAuthorised(key).toXML(em)); else resp.getWriter().print(Error.notAuthorised(key).toJSON(em));
            return false;
        }
        return true;
    }
}
