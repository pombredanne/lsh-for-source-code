package ca.ubc.icapture.genapha.beans;

import icapture.SQLMgr;
import icapture.beans.DB.Gene;
import icapture.beans.DB.GeneSet;
import icapture.beans.DB.SNP;
import icapture.beans.DB.SnpSet;
import icapture.beans.DB.User;
import icapture.beans.DB.Values;
import icapture.beans.KeggGeneBean;
import icapture.beans.KeggPathwayBean;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author cjones
 */
public class Login {

    public static final String DATA_OWNER = "data_owner";

    public static final String PERMISSION_WRITE = "permission_write";

    public static final String PERMISSION_WIZARD = "permission_wizard";

    public static final String LOGGED_IN = "logged_in";

    public static final String USER_NAME = "user_name";

    public static final String USER = "user";

    public static final String DATA_FULLACCESS = "data_fullaccess";

    public static void loginUser(String id, String pw, HttpSession session, HttpServletResponse response) throws Exception {
        logout(session, response);
        if (Validation.isBlank(id) || Validation.isBlank(pw)) {
            return;
        }
        try {
            User user = SQLMgr.getUser(id, criptString(pw));
            if (user != null) {
                if (Integer.lowestOneBit(user.getDataRights()) == 1) {
                    session.setAttribute(LOGGED_IN, "true");
                    session.setAttribute(USER_NAME, user.getUserName());
                    session.setAttribute(USER, user);
                    Cookie sessionCookie = new Cookie("loggedin", "true");
                    sessionCookie.setMaxAge(-1);
                    sessionCookie.setPath("/");
                    response.addCookie(sessionCookie);
                    ArrayList<User> userList = SQLMgr.getSubordinates(user);
                    if (userList.size() > 0 || SQLMgr.isCohortOwner(user) || SQLMgr.isSnpOwner(user)) {
                        session.setAttribute(DATA_OWNER, "true");
                    }
                    if (Integer.lowestOneBit(user.getDataRights() >> 1) == 1) {
                        session.setAttribute(DATA_FULLACCESS, "true");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception while trying to authenticate: " + e);
            e.printStackTrace();
            throw e;
        }
    }

    public static boolean getLoggedIn(HttpSession session) {
        String status;
        status = (String) session.getAttribute(LOGGED_IN);
        return (!Validation.isBlank(status) && Boolean.valueOf(status).booleanValue());
    }

    public static String getUserName(HttpSession session) {
        return (String) session.getAttribute(USER_NAME);
    }

    public static User getUser(HttpSession session) {
        return (User) session.getAttribute(USER);
    }

    public static void logout(HttpSession session, HttpServletResponse response) {
        session.setAttribute(LOGGED_IN, "false");
        session.removeAttribute(USER_NAME);
        session.removeAttribute(USER);
        session.removeAttribute(DATA_OWNER);
        Cookie sessionCookie = new Cookie("loggedin", "false");
        sessionCookie.setMaxAge(-1);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
    }

    public static Boolean isSnpVisible(HttpSession session, SNP snp) {
        ArrayList<SNP> snps = new ArrayList<SNP>();
        snps.add(snp);
        Boolean isVisible = LimitSnpsToVisable(session, snps);
        if (isVisible.booleanValue() == false) {
            return null;
        } else if (snps.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * LimitSnpsToVisable
     *
     * removes from the collection of snps any that the user doesnt have permission to view
     * @param session
     * @param snps
     * @return false if there was a problem getting the users credentials.
     */
    public static boolean LimitSnpsToVisable(HttpSession session, Collection<SNP> snps) {
        SnpSet publicSet = SQLMgr.getSnpSet("Public");
        Iterator<SNP> snpIter = snps.iterator();
        if (snps == null) {
            return false;
        }
        if (Login.getLoggedIn(session)) {
            User user = Login.getUser(session);
            if (user == null) {
                return false;
            }
            while (snpIter.hasNext()) {
                SNP snp = snpIter.next();
                if (!(SQLMgr.isSnpInSet(snp, publicSet) || SQLMgr.isSnpInUserSet(snp, user))) {
                    snpIter.remove();
                }
            }
        } else {
            while (snpIter.hasNext()) {
                SNP snp = snpIter.next();
                if (!SQLMgr.isSnpInSet(snp, publicSet)) {
                    snpIter.remove();
                }
            }
        }
        return true;
    }

    public static Boolean isGeneVisible(HttpSession session, Gene gene) {
        ArrayList<Gene> genes = new ArrayList<Gene>();
        genes.add(gene);
        Boolean isVisible = LimitGenesToVisable(session, genes);
        if (isVisible.booleanValue() == false) {
            return null;
        } else if (genes.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * LimitGenesToVisable
     * 
     * removes from the collection of genes any that the user doesnt have permission to view
     * @param session
     * @param genes
     * @return false if there was a problem getting the users credentials.
     */
    public static boolean LimitGenesToVisable(HttpSession session, Collection<Gene> genes) {
        GeneSet publicSet = SQLMgr.getGeneSet("Public");
        Iterator<Gene> geneIter = genes.iterator();
        if (genes == null) {
            return false;
        }
        if (Login.getLoggedIn(session)) {
            User user = Login.getUser(session);
            if (user == null) {
                return false;
            }
            while (geneIter.hasNext()) {
                Gene gene = geneIter.next();
                if (!(SQLMgr.isGeneInSet(gene, publicSet) || SQLMgr.isGeneInUserSet(gene, user))) {
                    geneIter.remove();
                }
            }
        } else {
            while (geneIter.hasNext()) {
                Gene gene = geneIter.next();
                if (!SQLMgr.isGeneInSet(gene, publicSet)) {
                    geneIter.remove();
                }
            }
        }
        return true;
    }

    /**
     * LimitPathwaysToVisable
     *
     * removes from the collection of pathways any that the user doesnt have permission to view
     * @param session
     * @param pathways
     * @return false if there was a problem getting the users credentials.
     */
    public static boolean LimitPathwaysToVisable(HttpSession session, Collection<KeggPathwayBean> pathways) {
        GeneSet publicSet = SQLMgr.getGeneSet("Public");
        Iterator<KeggPathwayBean> pathIter = pathways.iterator();
        if (Login.getLoggedIn(session)) {
            User user = Login.getUser(session);
            if (user == null) {
                return false;
            }
            while (pathIter.hasNext()) {
                KeggPathwayBean kpb = pathIter.next();
                Iterator<KeggGeneBean> geneIter = kpb.getGeneList().iterator();
                while (geneIter.hasNext()) {
                    KeggGeneBean gene = geneIter.next();
                    if (!(SQLMgr.isGeneInSet(gene, publicSet) || SQLMgr.isGeneInUserSet(gene, user))) {
                        geneIter.remove();
                    }
                    if (kpb.getGeneList().isEmpty()) {
                        pathIter.remove();
                    }
                }
            }
        } else {
            while (pathIter.hasNext()) {
                KeggPathwayBean kpb = pathIter.next();
                Iterator<KeggGeneBean> geneIter = kpb.getGeneList().iterator();
                while (geneIter.hasNext()) {
                    KeggGeneBean gene = geneIter.next();
                    if (!(SQLMgr.isGeneInSet(gene, publicSet))) {
                        geneIter.remove();
                    }
                    if (kpb.getGeneList().isEmpty()) {
                        pathIter.remove();
                    }
                }
            }
        }
        return true;
    }

    /**
     * LimitValuessToVisable
     *
     * removes from the collection of pathways any that the user doesnt have permission to view
     * @param session
     * @param values
     * @return false if there was a problem getting the users credentials.
     */
    public static boolean LimitValuesToVisable(HttpSession session, Collection<Values> values) {
        SnpSet publicSet = SQLMgr.getSnpSet("Public");
        Iterator<Values> valuesIter = values.iterator();
        if (Login.getLoggedIn(session)) {
            User user = Login.getUser(session);
            if (user == null) {
                return false;
            }
            while (valuesIter.hasNext()) {
                Values value = valuesIter.next();
                if (!(SQLMgr.isSnpInSet(value.getSnp(), publicSet) || SQLMgr.isSnpInUserSet(value.getSnp(), user))) {
                    valuesIter.remove();
                }
            }
        } else {
            while (valuesIter.hasNext()) {
                Values value = valuesIter.next();
                if (!SQLMgr.isSnpInSet(value.getSnp(), publicSet)) {
                    valuesIter.remove();
                }
            }
        }
        return true;
    }

    public static String criptString(String pswd) {
        StringBuilder xyz = new StringBuilder();
        try {
            byte inarray[] = pswd.getBytes();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(inarray, 0, inarray.length);
            byte outarray[] = md5.digest();
            for (int i = 0; i < outarray.length; i++) {
                xyz.append(outarray[i]);
            }
        } catch (NoSuchAlgorithmException ex) {
        }
        return xyz.toString();
    }
}
