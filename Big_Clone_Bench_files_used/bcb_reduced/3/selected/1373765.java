package com.bardsoftware.foronuvolo.data;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import com.bardsoftware.foronuvolo.server.MessageCache;
import com.google.appengine.repackaged.com.google.common.collect.Lists;

@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable = "true")
@Unique(name = "refid_constraint", members = { "refID" })
public class ForumUser implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(ForumUser.class.getName());

    @NotPersistent
    public static final ForumUser ANONYMOUS = new ForumUser("@anonymous", "Anonymous");

    @PrimaryKey
    private String id;

    @Persistent
    private String displayName;

    @Persistent
    private String refID;

    private ForumUser() {
    }

    public ForumUser(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.refID = calculateRefID(id);
    }

    public String getID() {
        return this.id;
    }

    public String getRefID() {
        return (this.refID == null) ? Integer.toString(this.id.hashCode(), 36) : this.refID;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getNickname() {
        return getDisplayName();
    }

    private PersistenceManager getPersistenceManager() {
        return PMF.getFactory().getPersistenceManager();
    }

    public void save() {
        PersistenceManager pm = getPersistenceManager();
        try {
            pm.makePersistent(this);
        } finally {
            pm.close();
        }
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public List<Discussion> getInitiatedDiscussions() {
        List<Discussion> result = Lists.newArrayList();
        PersistenceManager pm = PMF.getFactory().getPersistenceManager();
        try {
            Query q = pm.newQuery(Message.class);
            q.setFilter("myUserID == paramID");
            q.declareParameters("Long paramID");
            List<Message> authorizedMessages = (List<Message>) q.execute(this.id);
            for (Message m : authorizedMessages) {
                Discussion d = m.getDiscussion();
                if (m.equals(d.getInitialMessage())) {
                    result.add(pm.detachCopy(d));
                }
            }
            return result;
        } catch (JDOObjectNotFoundException e) {
            return null;
        } finally {
            pm.close();
        }
    }

    public static ForumUser findByRefID(String refid) {
        PersistenceManager pm = PMF.getFactory().getPersistenceManager();
        try {
            Query q = pm.newQuery(ForumUser.class);
            q.setRange(0, 1);
            q.setFilter("refID == paramID");
            q.declareParameters("Long paramID");
            List<ForumUser> result = (List<ForumUser>) q.execute(refid);
            return result.isEmpty() ? null : pm.detachCopy(result.get(0));
        } catch (JDOObjectNotFoundException e) {
            return null;
        } finally {
            pm.close();
        }
    }

    public static ForumUser find(String id) {
        PersistenceManager pm = PMF.getFactory().getPersistenceManager();
        try {
            ForumUser user = pm.getObjectById(ForumUser.class, id);
            return pm.detachCopy(user);
        } catch (JDOObjectNotFoundException e) {
            return null;
        } finally {
            pm.close();
        }
    }

    public static Collection<ForumUser> find(Collection<String> ids) {
        PersistenceManager pm = PMF.getFactory().getPersistenceManager();
        try {
            List<ForumUser> result = new ArrayList<ForumUser>();
            for (String id : ids) {
                ForumUser user;
                try {
                    user = pm.getObjectById(ForumUser.class, id);
                    result.add(pm.detachCopy(user));
                } catch (JDOObjectNotFoundException e) {
                }
            }
            return result;
        } catch (JDOObjectNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            pm.close();
        }
    }

    private static String calculateRefID(String id) {
        try {
            BigInteger digest = new BigInteger(MessageDigest.getInstance("MD5").digest(id.getBytes("UTF-8")));
            return digest.toString(Character.MAX_RADIX);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Failed to get MD5 algorithm", e);
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Failed to decode user id", e);
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isFCUser(ForumUser fu) {
        for (int i = 0; i < fu.id.length(); i++) {
            if (!Character.isDigit(fu.id.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static void patchRefids() throws CacheException {
        PersistenceManager pm = PMF.getFactory().getPersistenceManager();
        try {
            Query q = pm.newQuery(ForumUser.class);
            q.setRange(0, 20);
            List<ForumUser> result = (List<ForumUser>) q.execute();
            for (ForumUser fu : result) {
                if (isFCUser(fu)) {
                    String refID = calculateRefID(fu.id);
                    fu.refID = refID;
                    pm.makePersistent(fu);
                    List<Discussion> startedDiscussions = fu.getInitiatedDiscussions();
                    if (startedDiscussions.size() == 0) {
                        LOGGER.info("no discussions started by user=" + fu.id);
                    }
                    for (Discussion d : startedDiscussions) {
                        LOGGER.info("Updating discussion=" + d.getCreationOrder() + " started by user=" + fu.id + ". new refid=" + refID);
                        d.updateRefID();
                        pm.makePersistent(d);
                    }
                }
            }
        } catch (JDOObjectNotFoundException e) {
            LOGGER.log(Level.SEVERE, "", e);
        } finally {
            pm.close();
        }
    }

    public boolean canWrite() {
        return this != ForumUser.ANONYMOUS;
    }

    public void setDisplayName(String userName) {
        this.displayName = userName;
    }
}
