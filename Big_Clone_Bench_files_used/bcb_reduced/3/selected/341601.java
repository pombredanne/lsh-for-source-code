package com.uk.data.ejbs;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.New;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import sun.security.provider.MD5;
import com.uk.data.entities.Afrofe;
import com.uk.data.entities.AuditEntity;
import com.uk.data.entities.AuditTypeEnum;
import com.uk.data.entities.BashkiKomuna;
import com.uk.data.entities.Fatura;
import com.uk.data.entities.Kontrata;
import com.uk.data.entities.Parameters;
import com.uk.data.entities.Perdorimi;
import com.uk.data.entities.Qarku;
import com.uk.data.entities.Rrethi;
import com.uk.data.entities.StatusEnum;
import com.uk.data.entities.Tarifa;
import com.uk.data.entities.User;
import com.uk.exceptions.NestedException;

@Stateless
@EJB(name = "ejb/FaturaBean", beanInterface = IFaturaBean.class, beanName = "FaturaBean")
@SuppressWarnings("unchecked")
public class FaturaBean implements IFaturaBean {

    @PersistenceContext
    private EntityManager em;

    public List<Kontrata> queryKontrata(String property, Object value, int numberOfRows) {
        try {
            return em.createQuery("Select o from Kontrata o where o." + property + " like :property").setParameter("property", value).setMaxResults(numberOfRows).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    public List<Kontrata> queryLatestKontrata(int numberOfRows) {
        try {
            return em.createQuery("Select o from Kontrata o order by o.dataRegjistrimit  DESC").setMaxResults(numberOfRows).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    public List<Qarku> queryQarku() {
        try {
            return em.createQuery("Select o from Qarku o ").getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    public List<Rrethi> queryRrethi(Qarku qarku) {
        try {
            if (qarku == null) return em.createQuery("Select o from Rrethi o ").getResultList(); else return em.createQuery("Select o from Rrethi o where o.qarku = :qarku ").setParameter("qarku", qarku).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    public List<BashkiKomuna> queryBashkiKomuna(Rrethi rrethi) {
        try {
            if (rrethi == null) return em.createQuery("Select o from BashkiKomuna o ").getResultList(); else return em.createQuery("Select o from BashkiKomuna o where o.rrethi = :rrethi ").setParameter("rrethi", rrethi).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    public List<Perdorimi> queryPerdorimi() {
        try {
            return em.createQuery("Select o from Perdorimi o ").getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    public Kontrata saveKontrata(Kontrata kontrata) {
        try {
            return em.merge(kontrata);
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Kontrata> queryKontrata(String emri, String mbiemri, int numberOfRows) {
        try {
            if (!"".equals(emri) && !"".equals(mbiemri)) {
                System.out.println(1);
                return em.createQuery("Select o from Kontrata o where o.emri like :emri and o.mbiemri like :mbiemri").setParameter("emri", emri).setParameter("mbiemri", mbiemri).setMaxResults(numberOfRows).getResultList();
            } else if (!"".equals(emri)) {
                System.out.println(2);
                return em.createQuery("Select o from Kontrata o where o.emri like :emri").setParameter("emri", emri).setMaxResults(numberOfRows).getResultList();
            } else if (!"".equals(mbiemri)) {
                System.out.println(3);
                return em.createQuery("Select o from Kontrata o where o.mbiemri like :mbiemri").setParameter("mbiemri", mbiemri).setMaxResults(numberOfRows).getResultList();
            }
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Tarifa> queryTarifa(StatusEnum status) {
        try {
            return em.createQuery("Select o from Tarifa o where o.status = :status").setParameter("status", status).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public Tarifa saveTarifa(Tarifa tarifa, boolean saveOldRecord) {
        try {
            if (saveOldRecord) {
                Tarifa newTarifa = new Tarifa();
                newTarifa.setAplikoTVSH(tarifa.isAplikoTVSH());
                newTarifa.setCmimi(tarifa.getCmimi());
                newTarifa.setDataRegjistrimit(new Date());
                newTarifa.setKodi(tarifa.getKodi());
                newTarifa.setNeListe(tarifa.isNeListe());
                newTarifa.setNjesia(tarifa.getNjesia());
                newTarifa.setPershkrimi(tarifa.getPershkrimi());
                newTarifa.setStatus(StatusEnum.AKTIV);
                newTarifa.setUk(tarifa.isUk());
                em.persist(newTarifa);
                Tarifa saved = this.queryTarifaById(tarifa.getId());
                saved.setStatus(StatusEnum.JOAKTIVE);
                em.merge(saved);
                return newTarifa;
            } else {
                return em.merge(tarifa);
            }
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isTarifaCodeUnique(String kodi) {
        try {
            List<Tarifa> tarifaList = em.createQuery("Select o from Tarifa o where o.kodi = :kodi").setParameter("kodi", kodi).getResultList();
            if (tarifaList == null || tarifaList.isEmpty()) return true;
            if (tarifaList.size() == 1) return true;
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void loadUser(String user) {
    }

    @Override
    public Tarifa queryTarifaById(Integer id) {
        try {
            return (Tarifa) em.createQuery("Select o from Tarifa o where o.id = :id").setParameter("id", id).getSingleResult();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public Double getAfrofe() {
        try {
            Afrofe afrofe = (Afrofe) em.createQuery("Select o from Afrofe o where o.status = :status").setParameter("status", StatusEnum.AKTIV).getSingleResult();
            return afrofe.getLeximi();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Tarifa> queryTarifa(boolean neListe) {
        try {
            return em.createQuery("Select o from Tarifa o where o.neListe = :neListe and o.status = :status").setParameter("neListe", neListe).setParameter("status", StatusEnum.AKTIV).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public Kontrata queryKontrata(Integer id) {
        try {
            return (Kontrata) em.createQuery("Select o from Kontrata o where o.id = :id").setParameter("id", id).getSingleResult();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public Fatura saveFatura(Fatura fatura) {
        try {
            fatura.setDataRegjistrimit(new Date());
            Fatura f = em.merge(fatura);
            em.flush();
            return f;
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Parameters> getParameters() {
        try {
            return em.createQuery("Select o from Parameters o").getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Fatura> queryLatestFatura(Kontrata kontrata, int maxResult) {
        try {
            return em.createQuery("Select o from Fatura o where o.kontrata = :kontrata and o.anulluar = :anulluar").setParameter("kontrata", kontrata).setParameter("anulluar", false).setMaxResults(maxResult).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Fatura> queryFatura(Kontrata kontrata, Boolean paguar) {
        try {
            if (paguar == null) {
                return em.createQuery("Select o from Fatura o where o.kontrata = :kontrata and o.anulluar = :anulluar order by o.dataLeximAktual DESC").setParameter("kontrata", kontrata).setParameter("anulluar", false).getResultList();
            } else {
                return em.createQuery("Select o from Fatura o where o.kontrata = :kontrata and o.paguar = :paguar and o.anulluar = :anulluar order by o.dataLeximAktual DESC").setParameter("kontrata", kontrata).setParameter("paguar", paguar.booleanValue()).setParameter("anulluar", false).getResultList();
            }
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Kontrata> findDebitor(Date ngaData, Date deriNe) {
        try {
            String sql = "Select DISTINCT o from Kontrata o, IN( o.faturatPapaguar) f where " + " f.dataLeximAktual >= :ngaData and f.dataLeximAktual <= :deriNe";
            return em.createQuery(sql).setParameter("ngaData", ngaData).setParameter("deriNe", deriNe).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Kontrata> listDebitor() {
        try {
            List<Kontrata> debitorList = em.createQuery("Select distinct o from Kontrata o JOIN o.faturat f where f.paguar = :paguar and f.anulluar = :anulluar").setParameter("paguar", false).setParameter("anulluar", false).getResultList();
            return debitorList;
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public int queryFaturainTotal(Kontrata kontrata, Boolean paguar) {
        try {
            List<Fatura> f;
            if (paguar == null) {
                f = em.createQuery("Select o from Fatura o where o.kontrata = :kontrata and o.anulluar = :anulluar").setParameter("kontrata", kontrata).setParameter("anulluar", false).getResultList();
            } else {
                f = em.createQuery("Select o from Fatura o where o.kontrata = :kontrata and o.paguar = :paguar and o.anulluar = :anulluar").setParameter("kontrata", kontrata).setParameter("paguar", paguar.booleanValue()).setParameter("anulluar", false).getResultList();
            }
            if (f != null) return f.size();
            return 0;
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<? extends User> queryUsers() {
        try {
            return em.createQuery("Select o from User o").getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public User saveUser(User user) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(user.getPassword().getBytes("UTF-8"));
            byte[] hash = digest.digest();
            BigInteger bigInt = new BigInteger(1, hash);
            String hashtext = bigInt.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            user.setPassword(hashtext);
            user.setDataRegjistrimit(new Date());
            return em.merge(user);
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public User login(String username, String password) {
        User user = null;
        try {
            user = (User) em.createQuery("Select o from User o where o.username = :username").setParameter("username", username).getSingleResult();
        } catch (NoResultException e) {
            throw new NestedException(e.getMessage(), e);
        }
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(password.getBytes("UTF-8"));
            byte[] hash = digest.digest();
            BigInteger bigInt = new BigInteger(1, hash);
            String hashtext = bigInt.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            if (hashtext.equals(user.getPassword())) return user;
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public User updateUser(User user) {
        try {
            user.setDataRegjistrimit(new Date());
            return em.merge(user);
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void postPersist(Object obj) {
        try {
            AuditEntity auditEntity = new AuditEntity();
            auditEntity.setAuditTime(new Date());
            auditEntity.setAuditType(AuditTypeEnum.POST_PERSIST);
            auditEntity.setEntityId(obj.toString().getBytes());
            em.persist(auditEntity);
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Kontrata> listDebitor(Date fromDate, Date toDate) {
        try {
            return em.createQuery("Select distinct o.kontrata from Fatura o where o.paguar = :paguar and o.anulluar != :anulluar  and o.dataLeximAktual >= :fromDate and o.dataLeximAktual < :toDate").setParameter("paguar", false).setParameter("anulluar", false).setParameter("fromDate", fromDate).setParameter("toDate", toDate).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }

    @Override
    public List<Kontrata> listKontrataPaFature(Date fromDate, Date toDate) {
        try {
            return em.createQuery("Select distinct o from Kontrata o where  NOT EXISTS (Select f.kontrata from Fatura f where f.kontrata = o and f.paguar = :paguar and f.anulluar != :anulluar  and f.dataLeximAktual >= :fromDate and f.dataLeximAktual < :toDate)").setParameter("paguar", false).setParameter("anulluar", false).setParameter("fromDate", fromDate).setParameter("toDate", toDate).getResultList();
        } catch (Exception e) {
            throw new NestedException(e.getMessage(), e);
        }
    }
}
