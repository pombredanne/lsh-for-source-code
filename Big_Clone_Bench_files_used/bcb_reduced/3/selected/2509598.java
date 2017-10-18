package gnu.beanfactory.servlet;

import java.security.*;
import gnu.beanfactory.*;
import javax.servlet.http.*;
import java.util.*;
import org.apache.log4j.*;

/**
 * Responsible for securing URLs against tampering.
 **/
public class DigestSecurity {

    static Category log = Category.getInstance(DigestSecurity.class.getName());

    public static String FINGERPRINT_KEY = "_fp_";

    static MessageDigest root = null;

    MessageDigest digest = null;

    Set keys = new TreeSet();

    static {
    }

    public synchronized void init() throws java.security.NoSuchAlgorithmException, BeanFactoryException {
        if (root == null) {
            String key = "default";
            key = (String) Container.getBeanContext().resolve("bean:/gnu/beanfactory/servlet/SecurityConfig.digestKey");
            root = MessageDigest.getInstance("SHA");
            root.update(key.getBytes());
        }
    }

    public DigestSecurity() throws NoSuchAlgorithmException, BeanFactoryException {
        gnu.beanfactory.BeanContext.getBeanContext();
        init();
        try {
            digest = (MessageDigest) root.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Can't clone MessageDigest");
        }
    }

    public void update(String s) {
        keys.add(s);
    }

    public String getDigest() {
        Iterator t = keys.iterator();
        while (t.hasNext()) {
            String key = (String) t.next();
            digest.update(key.getBytes());
        }
        String di = gnu.beanfactory.util.Base64.encode(digest.digest());
        return di;
    }

    public static void verify(HttpServletRequest request) throws NoSuchAlgorithmException, BeanFactoryException {
        String[] fp = request.getParameterValues(FINGERPRINT_KEY);
        if (fp == null) {
            fp = new String[0];
        }
        verify(request, fp);
    }

    public static void verify(HttpServletRequest request, String[] hash) throws NoSuchAlgorithmException, BeanFactoryException {
        HashSet hashSet = new HashSet();
        for (int i = 0; i < hash.length; i++) {
            hashSet.add(hash[i]);
        }
        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            if (name.startsWith("$") || name.startsWith("!")) {
                DigestSecurity ds = new DigestSecurity();
                ds.update(name);
                String dig = ds.getDigest();
                if (!hashSet.contains(dig)) {
                    throw new SecurityException();
                }
            }
        }
        String defaultAction = request.getParameter("gnu.beanfactory.defaultaction");
        if (defaultAction != null) {
            DigestSecurity ds = new DigestSecurity();
            ds.update(defaultAction);
            String dig = ds.getDigest();
            if (!hashSet.contains(dig)) {
                throw new SecurityException();
            }
        }
    }
}
