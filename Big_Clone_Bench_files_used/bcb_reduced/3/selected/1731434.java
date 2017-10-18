package com.eip.yost.dao.impl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.eip.yost.dao.interfaces.IClientDao;
import com.eip.yost.entite.Client;
import com.eip.yost.entite.Souscription;

@Repository("clientDao")
@Transactional(propagation = Propagation.SUPPORTS)
public class ClientDaoImpl extends GenericDao<Client, Integer> implements IClientDao {

    public ClientDaoImpl() {
        super(Client.class);
    }

    /**
	 * {@inheritDoc}
	 */
    public boolean checkLogin(String pMail, String pMdp) {
        boolean vLoginOk = false;
        if (pMail == null || pMdp == null) {
            throw new IllegalArgumentException("Login and password are mandatory. Null values are forbidden.");
        }
        try {
            Criteria crit = ((Session) this.entityManager.getDelegate()).createCriteria(Client.class);
            crit.add(Restrictions.ilike("email", pMail));
            MessageDigest vMd5Instance;
            try {
                vMd5Instance = MessageDigest.getInstance("MD5");
                vMd5Instance.reset();
                vMd5Instance.update(pMdp.getBytes());
                byte[] vDigest = vMd5Instance.digest();
                BigInteger vBigInt = new BigInteger(1, vDigest);
                String vHashPassword = vBigInt.toString(16);
                crit.add(Restrictions.eq("mdp", vHashPassword));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            Client pClient = (Client) crit.uniqueResult();
            vLoginOk = (pClient != null);
        } catch (DataAccessException e) {
            mLogger.error("Exception - DataAccessException occurs : {} on complete checkLogin( {}, {} )", new Object[] { e.getMessage(), pMail, pMdp });
        }
        return vLoginOk;
    }

    /**
	 * {@inheritDoc}
	 */
    public Client getClient(String pMail) {
        Client vClient = null;
        if (pMail == null) {
            throw new IllegalArgumentException("Login is mandatory. Null value is forbidden.");
        }
        try {
            Criteria crit = ((Session) this.entityManager.getDelegate()).createCriteria(Client.class);
            crit.add(Restrictions.eq("email", pMail).ignoreCase());
            vClient = (Client) crit.uniqueResult();
        } catch (DataAccessException e) {
            mLogger.error("Exception - DataAccessException occurs : {} on complete getClient( {} )", e.getMessage(), pMail);
        }
        return vClient;
    }

    /**
	 * {@inheritDoc}
	 */
    public List<Client> getClientSouscriptionArrivantAEcheance(Integer pNbMois) {
        List<Client> vClientList = null;
        Date vAujourdhui = new Date();
        Calendar vCal = Calendar.getInstance();
        vCal.add(Calendar.MONTH, pNbMois);
        Date vDansXMois = vCal.getTime();
        try {
            Criteria crit = ((Session) this.entityManager.getDelegate()).createCriteria(Souscription.class);
            ProjectionList proList = Projections.projectionList();
            proList.add(Projections.property("client"));
            crit.setProjection(proList);
            crit.add(Restrictions.gt("dateResiliation", new Date()));
            crit.add(Restrictions.between("dateResiliation", vAujourdhui, vDansXMois));
            crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            crit.addOrder(Order.asc("dateSouscription"));
            vClientList = (List<Client>) crit.list();
        } catch (DataAccessException e) {
            mLogger.error("Exception - DataAccessException occurs : {} on complete getClientSouscriptionArrivantAEcheance( {} )", e.getMessage(), pNbMois);
        }
        return vClientList;
    }

    public List<Client> findByPartialMail(String pPartial, Integer pLimit) {
        List<Client> vClientList = null;
        try {
            Criteria crit = ((Session) this.entityManager.getDelegate()).createCriteria(Client.class);
            crit.add(Restrictions.like("email", "%" + pPartial + "%"));
            crit.addOrder(Order.asc("email"));
            if (pLimit != null) {
                crit.setMaxResults(pLimit);
            }
            vClientList = (List<Client>) crit.list();
        } catch (DataAccessException e) {
            mLogger.error("Exception - DataAccessException occurs : {} on complete findByPartialMail( {}, {} )", new Object[] { e.getMessage(), pPartial, pLimit });
        }
        return vClientList;
    }

    public Client getClient(Integer pId) {
        Client vClient = null;
        if (pId == null) {
            throw new IllegalArgumentException("Id is mandatory. Null value is forbidden.");
        }
        try {
            Criteria crit = ((Session) this.entityManager.getDelegate()).createCriteria(Client.class);
            crit.add(Restrictions.eq("idclient", pId));
            vClient = (Client) crit.uniqueResult();
        } catch (DataAccessException e) {
            mLogger.error("Exception - DataAccessException occurs : {} on complete getClient()", e.getMessage());
        }
        return vClient;
    }
}
