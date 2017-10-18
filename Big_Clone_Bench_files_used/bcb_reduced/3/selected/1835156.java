package business.manager.beans;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Transient;
import javax.transaction.UserTransaction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import business.domain.Conference;
import business.domain.Paper;
import business.domain.Skill;
import business.domain.actors.User;
import business.manager.interfaces.UserManager;

/**
 * @author forobert
 *
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class UserManagerBean implements UserManager {

    @PersistenceContext
    private EntityManager em;

    @Resource
    private UserTransaction ut;

    @Transient
    protected final Log logger = LogFactory.getLog(getClass());

    public boolean addSkillToUser(String login, String skillname) {
        String skillName = skillname.toUpperCase();
        logger.debug("UserManagerBean : addSkill : starting transaction");
        try {
            ut.begin();
            User u = em.find(User.class, login);
            Skill skill = em.find(Skill.class, skillName);
            skill.getName();
            u.addSkill(skill);
            em.flush();
            ut.commit();
            logger.info("UserManagerBean : addSkill : transaction complete with success");
            return true;
        } catch (Exception e) {
            logger.error("UserManagerBean : addSkill : transaction failed, rollbacking now");
            try {
                ut.rollback();
                logger.debug("UserManagerBean : addSkill : rollbackdone");
            } catch (Exception e1) {
                logger.error("UserManagerBean : addSkill : rollback failed");
            }
        }
        return false;
    }

    public boolean createUser(String login, String firstname, String surname, String email, String laboratory, String password) {
        logger.debug("UserManagerBean : createUser : starting transaction");
        try {
            ut.begin();
            String newpass = processPassword(password);
            User newUser = new User(login, firstname, surname, email, laboratory, newpass);
            em.persist(newUser);
            ut.commit();
            logger.info("UserManagerBean : createUser : transaction complete with success");
            return true;
        } catch (Exception e) {
            logger.error("UserManagerBean : createUser : transaction failed, rollbacking now");
            try {
                ut.rollback();
                logger.debug("UserManagerBean : createUser : rollbackdone");
            } catch (Exception e1) {
                logger.error("UserManagerBean : createUser : rollback failed");
            }
        }
        return false;
    }

    public boolean delegatePaper(String comityLogin, String refereeLogin, long paperId) {
        logger.debug("UserManagerBean : delegatePaper : starting transaction");
        if (comityLogin.equals(refereeLogin)) return false;
        try {
            ut.begin();
            User member = em.find(User.class, comityLogin);
            User delegate = em.find(User.class, refereeLogin);
            Paper paper = em.find(Paper.class, paperId);
            boolean res = member.removePaperToReview(paper);
            res &= member.addToDelegatedPapers(paper);
            res &= paper.removePcMember(member);
            res &= paper.addDelegate(delegate);
            res &= paper.addDeleguer(member);
            res &= delegate.addToReceivedPapers(paper);
            member.recordDelegate(delegate, paper);
            delegate.recordDeleger(member, paper);
            ut.commit();
            logger.info("UserManagerBean : delegatePaper : transaction complete with success");
            return res;
        } catch (Exception e) {
            logger.error("UserManagerBean : delegatePaper : transaction failed, rollbacking now");
            try {
                ut.rollback();
                logger.debug("UserManagerBean : delegatePaper : rollbackdone");
            } catch (Exception e1) {
                logger.error("UserManagerBean : delegatePaper : rollback failed");
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public List<User> getAllRegisteredUsers() {
        logger.debug("UserManagerBean : getAllRegisteredUsers ");
        Query query = em.createQuery("from User user ORDER BY user.surname");
        List<User> resultList = query.getResultList();
        User admin = em.find(User.class, "admin");
        resultList.remove(admin);
        return resultList;
    }

    @SuppressWarnings("unchecked")
    public List<String> getAllRegisteredUsersLogin() {
        logger.debug("UserManagerBean : getAllRegisteredUsersLogin ");
        Query query = em.createQuery("select user.login from User user ORDER BY user.login");
        List<String> resultList = query.getResultList();
        resultList.remove("admin");
        return resultList;
    }

    @SuppressWarnings("unchecked")
    public List<Paper> getConflictListForUser(String login, long confId) {
        logger.debug("UserManagerBean : getConflictListForUser");
        User user = em.find(User.class, login);
        Conference conf = em.find(Conference.class, confId);
        if (user == null) return null;
        if (conf == null) return null;
        Query query = em.createQuery("select p From Paper p join p.conflictList u where u.login=:aLogin and p.conference.id=:aConf");
        query.setParameter("aLogin", login);
        query.setParameter("aConf", confId);
        List<Paper> papers = (List<Paper>) query.getResultList();
        return papers;
    }

    @SuppressWarnings("unchecked")
    public List<Paper> getDelegatedPapers(String login, long confId) {
        logger.debug("UserManagerBean : getDelegatedPapers");
        User user = em.find(User.class, login);
        Conference conf = em.find(Conference.class, confId);
        if (conf == null) return null;
        if (user == null) return null;
        Query query = em.createQuery("select d from User u join u.delegatedPapers d where u.login=:aLogin and d.conference=:aConf");
        query.setParameter("aLogin", login);
        query.setParameter("aConf", conf);
        return (List<Paper>) query.getResultList();
    }

    public User getDelegateForPaper(String login, long paperId) {
        logger.debug("UserManagerBean : getDelegateForPaper");
        User pcmember = em.find(User.class, login);
        if (pcmember == null) return null;
        Paper paper = em.find(Paper.class, paperId);
        if (paper == null) return null;
        User user = pcmember.getDelegateForPaper(paper);
        return user;
    }

    public User getDelegerForPaper(String login, long paperId) {
        User referee = em.find(User.class, login);
        if (referee == null) return null;
        Paper paper = em.find(Paper.class, paperId);
        if (paper == null) return null;
        return referee.getDelegerForPaper(paper);
    }

    @SuppressWarnings("unchecked")
    public List<String> getLoginsByName(String firstname, String surname) {
        logger.debug("UserManagerBean : getLoginsByName ");
        List<String> result = new ArrayList<String>();
        Query query = em.createQuery("select login from User where surname=:surname and firstname=:firstname");
        query.setParameter("surname", surname);
        query.setParameter("firstname", firstname);
        result = query.getResultList();
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Paper> getPapersToReviewForDelegate(String login, long confId) {
        logger.debug("UserManagerBean : getPapersToReviewForDelegate");
        User user = em.find(User.class, login);
        Conference conf = em.find(Conference.class, confId);
        if (conf == null) return null;
        if (user == null) return null;
        Query query = em.createQuery("select p from Paper p join p.delegate d where d.login=:aLogin and p.conference=:aConf");
        query.setParameter("aLogin", login);
        query.setParameter("aConf", conf);
        return (List<Paper>) query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Paper> getPapersToReviewForPcMember(String login, long confId) {
        logger.debug("UserManagerBean : getPapersToReviewForPcMembrs");
        User user = em.find(User.class, login);
        Conference conf = em.find(Conference.class, confId);
        if (conf == null) return null;
        if (user == null) return null;
        Query query = em.createQuery("select p from Paper p join p.pcMembersReviewers r where r.login=:aLogin and p.conference=:aConf");
        query.setParameter("aLogin", login);
        query.setParameter("aConf", conf);
        return (List<Paper>) query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Paper> getPreferenceListForUser(String login, long confId) {
        logger.debug("UserManagerBean : getPreferenceListForUser");
        User user = em.find(User.class, login);
        Conference conf = em.find(Conference.class, confId);
        if (user == null || conf == null) return null;
        Query query = em.createQuery("select p From Paper p join p.preferencesList u where u.login=:aLogin and p.conference.id=:aConf");
        query.setParameter("aLogin", login);
        query.setParameter("aConf", confId);
        List<Paper> papers = (List<Paper>) query.getResultList();
        return papers;
    }

    @SuppressWarnings("unchecked")
    public List<Skill> getSkillsForUser(String login) {
        logger.debug("UserManagerBean : getSkillsForUser");
        User user = em.find(User.class, login);
        if (user == null) return null;
        Query query = em.createQuery("select skills From User u join u.skills skills where u.login=:aLogin");
        query.setParameter("aLogin", login);
        return query.getResultList();
    }

    public User getUser(String login) {
        logger.debug("UserManagerBean : getUser ");
        User u = em.find(User.class, login);
        return u;
    }

    @SuppressWarnings("unchecked")
    public List<Paper> getWatchListForUser(String login, long confId) {
        logger.debug("UserManagerBean : getWatchListForUser");
        User user = em.find(User.class, login);
        Conference conf = em.find(Conference.class, confId);
        if (user == null || conf == null) return null;
        Query query = em.createQuery("select p From Paper p join p.watchList u where u.login=:aLogin and p.conference.id=:aConf");
        query.setParameter("aLogin", login);
        query.setParameter("aConf", confId);
        List<Paper> papers = (List<Paper>) query.getResultList();
        return papers;
    }

    public boolean modifyUser(String login, String firstname, String surname, String email, String laboratory) {
        logger.debug("UserManagerBean : modifyUser : starting transaction");
        try {
            ut.begin();
            User u = em.find(User.class, login);
            u.setFirstname(firstname);
            u.setSurname(surname);
            u.setEmail(email);
            u.setLaboratory(laboratory);
            em.flush();
            ut.commit();
            logger.info("UserManagerBean : modifyUser : transaction complete with success");
            return true;
        } catch (Exception e) {
            logger.error("UserManagerBean : modifyUser : transaction failed, rollbacking now");
            try {
                ut.rollback();
                logger.debug("UserManagerBean : modifyUser : rollbackdone");
            } catch (Exception e1) {
                logger.error("UserManagerBean : modifyUser : rollback failed");
            }
        }
        return false;
    }

    public boolean modifyUser(String login, String firstname, String surname, String email, String laboratory, String password) {
        logger.debug("UserManagerBean : modifyUser : starting transaction");
        try {
            ut.begin();
            User u = em.find(User.class, login);
            u.setFirstname(firstname);
            u.setSurname(surname);
            u.setEmail(email);
            u.setLaboratory(laboratory);
            u.setPassword(this.processPassword(password));
            ut.commit();
            logger.info("UserManagerBean : modifyUser : transaction complete with success");
            return true;
        } catch (Exception e) {
            logger.error("UserManagerBean : modifyUser : transaction failed, rollbacking now");
            try {
                ut.rollback();
                logger.debug("UserManagerBean : modifyUser : rollbackdone");
            } catch (Exception e1) {
                logger.error("UserManagerBean : modifyUser : rollback failed");
            }
        }
        return false;
    }

    /**
	 * Encapsulate the method used to create password
	 * @param password the password to process
	 * @return the processed password
	 */
    private String processPassword(String password) {
        byte[] uniqueKey = password.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            throw new Error("no MD5 support in this VM");
        }
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }

    public boolean removeSkillFromUser(String login, String skillname) {
        String string = skillname.toUpperCase();
        logger.debug("UserManagerBean : removeSkillForUser : starting transaction");
        try {
            ut.begin();
            User u = em.find(User.class, login);
            Skill skill = em.find(Skill.class, string);
            u.removeSkill(skill);
            em.flush();
            ut.commit();
            logger.info("UserManagerBean : removeSkillForUser : transaction complete with success");
            return true;
        } catch (Exception e) {
            logger.error("UserManagerBean : removeSkillForUser : transaction failed, rollbacking now");
            try {
                ut.rollback();
                logger.debug("UserManagerBean : removeSkillForUser : rollbackdone");
            } catch (Exception e1) {
                logger.error("UserManagerBean : removeSkillForUser : rollback failed");
            }
            return false;
        }
    }

    public boolean removeUser(String login) {
        logger.debug("UserManagerBean : removeUser : starting transaction");
        try {
            ut.begin();
            User user = em.find(User.class, login);
            List<Paper> papers = user.getPreferencesList();
            papers.clear();
            papers = user.getConflictList();
            papers.clear();
            papers = user.getWatchList();
            papers.clear();
            Set<Paper> papersSet = user.getPapersToReview();
            papersSet.clear();
            em.remove(user);
            ut.commit();
            logger.info("UserManagerBean : removeUser : transaction complete with success");
            return true;
        } catch (Exception e) {
            logger.error("UserManagerBean : removeUser : transaction failed, rollbacking now");
            try {
                ut.rollback();
                logger.debug("UserManagerBean : removeUser : rollbackdone");
            } catch (Exception e1) {
                logger.error("UserManagerBean : removeUser : rollback failed");
            }
        }
        return false;
    }

    public boolean verifyPassword(String login, String password) {
        logger.debug("UserManagerBean : verifyPassword ");
        String passTest = processPassword(password);
        Query query = em.createQuery("select u.password from User u where u.login=:login");
        query.setParameter("login", login);
        String result = (String) query.getSingleResult();
        return passTest.equals(result);
    }
}
