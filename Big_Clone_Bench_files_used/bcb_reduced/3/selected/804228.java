package com.jujunie.project1901;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import com.jujunie.service.log.Log;

/**
 * Manager for the member entity. Manages the transactions with the data abstraction layer:
 * Hibernate
 * @author julien
 * @since 0.01.01
 */
public final class MemberManager {

    private static final Log LOG = Log.getInstance(MemberManager.class);

    /**
     * Get the member associated with the given login, password and organisation
     * @param login member login
     * @param password member password
     * @param org Organisation the member belongs to
     * @return the corresponding member if found and authenticated, null otherwise
     */
    static Member get(String login, String password, Organisation org, Exercice exe) {
        LOG.enter("get");
        LOG.debug("Login", login);
        LOG.debug("Organisation", org.getName());
        Session session = null;
        Member res = null;
        if (StringUtils.isEmpty(password)) {
            LOG.debug("Empty password provided, returning null");
            LOG.exit("get");
            return null;
        }
        session = HibernateUtil.getSessionFactory().getCurrentSession();
        res = (Member) session.createQuery("select m from Member m" + " where" + " m.login = ?" + " and m.password = ?" + " and m.organisation = ?").setString(0, login).setString(1, getMD5Sum(password)).setEntity(2, org).uniqueResult();
        if (res != null && !res.getMemberAccess(res, exe).canLogIn()) {
            LOG.warn("Cannot log in, returning null");
            res = null;
        }
        LOG.exit("get");
        return res;
    }

    /**
     * Save or update a member to the persistance layer
     * @param m the member to save or update
     * @param context Exercice context where the set occurs
     */
    static void set(Member m, Exercice context) throws Project1901UserException {
        LOG.enter("set");
        if (loginIsUniqueForOrganisation(m)) {
            LOG.debug("Force MESLinks checks");
            m.getExerciceMESLink(context);
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.saveOrUpdate(m);
        } else {
            throw new Project1901UserException("Error.loginNotUnique", "The login {0} is already used", new String[] { m.getLogin() });
        }
        LOG.exit("set");
    }

    /**
     * Reattach to persistent layer a detached member
     * @param m member to reattch
     */
    static void reattach(Member m) {
        LOG.debug("reattach");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.refresh(m);
    }

    /**
     * Checks if the given member login is unique for the member organisation
     * @param m member
     * @return true is unique, false otherwise
     */
    @SuppressWarnings("unchecked")
    static boolean loginIsUniqueForOrganisation(Member m) {
        Session session = null;
        List<Member> res = null;
        session = HibernateUtil.getSessionFactory().getCurrentSession();
        res = (List<Member>) session.createQuery("select m from Member m" + " where" + " m.login = ?" + " and m.organisation = ?").setString(0, m.getLogin()).setEntity(1, m.getOrganisation()).list();
        if (res == null || res.size() == 0) {
            return true;
        } else if (res.size() == 1) {
            return res.get(0).getIdentifier().equals(m.getIdentifier());
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Member> search(MemberSearchParameters msp) {
        LOG.enter("search");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        StringBuffer selectFrom = new StringBuffer("select distinct m from Member as m");
        StringBuffer where = new StringBuffer(" where m.organisation = ?");
        int index = 0;
        if (StringUtils.isNotEmpty(msp.getName())) {
            where.append(" and m.lastName like ?");
        }
        if (msp.getBirthYear() > 0) {
            where.append(" and (year(m.birth) = ? or m.birth is null)");
        }
        if (!msp.isIdentify()) {
            if (msp.getOrganisationRole() != null) {
                where.append(" and m.roleCode = ?");
            }
            if (msp.getExercice() != null || msp.getSection() != null || msp.getCategory() != null || msp.getShift() != null || msp.getSectionRole() != null) {
                selectFrom.append(" inner join m.mesLinks as mes");
                if (msp.getExercice() != null) {
                    where.append(" and mes.exercice = ?");
                }
                if (msp.getSection() != null) {
                    where.append(" and mes.section = ?");
                }
                if (msp.getSectionRole() != null) {
                    where.append(" and mes.roleCode = ?");
                }
                if (msp.getCategory() != null) {
                    selectFrom.append(" inner join mes.sectionCategories as msc");
                    where.append(" and msc = ?");
                }
                if (msp.getShift() != null) {
                    selectFrom.append(" inner join mes.shifts as mss");
                    where.append(" and mss = ?");
                }
            }
        } else {
            LOG.debug("Identifying process: no filters");
        }
        String hsql = selectFrom.append(where).toString();
        LOG.debug("Query", hsql);
        Query req = session.createQuery(hsql);
        req.setEntity(index++, msp.getOrganisation());
        if (StringUtils.isNotEmpty(msp.getName())) {
            req.setString(index++, '%' + msp.getName() + '%');
        }
        if (msp.getBirthYear() > 0) {
            req.setInteger(index++, msp.getBirthYear());
        }
        if (!msp.isIdentify()) {
            if (msp.getOrganisationRole() != null) {
                req.setString(index++, msp.getOrganisationRole().getCode());
            }
            if (msp.getExercice() != null) {
                req.setEntity(index++, msp.getExercice());
            }
            if (msp.getSection() != null) {
                req.setEntity(index++, msp.getSection());
            }
            if (msp.getSectionRole() != null) {
                req.setString(index++, msp.getSectionRole().getCode());
            }
            if (msp.getCategory() != null) {
                req.setEntity(index++, msp.getCategory());
            }
            if (msp.getShift() != null) {
                req.setEntity(index++, msp.getShift());
            }
        } else {
            LOG.debug("Identifying process: no filters");
        }
        List<Member> res = req.list();
        LOG.exit("search");
        return res;
    }

    static String getMD5Sum(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(source.getBytes());
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            return bigInt.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm seems to not be supported. This is a requirement!");
        }
    }

    /**
     * @param ex Exercice
     * @return the members for the given exercice
     */
    public static List<Member> getMembers(Organisation o, Exercice ex) {
        MemberSearchParameters msp = new MemberSearchParameters(o);
        msp.setExercice(ex);
        return search(msp);
    }

    public static void main(String[] args) {
        System.out.println(getMD5Sum(args[0]));
    }
}
