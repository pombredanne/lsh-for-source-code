package org.amiwall.policy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.amiwall.AmiWall;
import org.amiwall.feedback.BasicFeedback;
import org.amiwall.feedback.Feedback;
import org.amiwall.plugin.Install;
import org.amiwall.plugin.Plugin;
import org.amiwall.user.AbstractIdName;
import org.amiwall.user.Group;
import org.amiwall.user.GroupHome;
import org.amiwall.user.User;
import org.amiwall.user.UserHome;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;

/**
 *  Description of the Class
 *
 *@author    Nick Cunnah
 */
public abstract class AbstractPolicy extends AbstractIdName implements Policy, Runnable {

    private static Logger log = Logger.getLogger("org.amiwall.policy.AbstractPolicy");

    /**
     *  Description of the Field
     */
    protected Feedback feedback = null;

    /**
     *  Description of the Field
     */
    protected Set groupRules = null;

    /**
     *  Description of the Field
     */
    protected UserHome userHome = null;

    /**
     *  Description of the Field
     */
    protected GroupHome groupHome = null;

    /**
     *  Description of the Field
     */
    protected Thread thread = null;

    /**
     *  Description of the Field
     */
    protected Periodic periodic = null;

    /**
     *  Description of the Field
     */
    protected boolean policeNow = false;

    File file = null;

    boolean stopRequested = false;

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    public void digest(Element root) {
        setFile(root.getChildTextTrim("file"));
        digestFeedback(root);
        digestPeriodic(root);
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    void digestFeedback(Element root) {
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            Feedback feedback = getFeedback(child.getName());
            ;
            if (feedback != null) {
                feedback.digest(child);
                setFeedback(feedback);
            }
        }
    }

    /**
     *  Gets the feedback attribute of the AbstractPolicy object
     *
     *@param  name  Description of the Parameter
     *@return       The feedback value
     */
    protected Feedback getFeedback(String name) {
        if (name.equals("BasicFeedback")) {
            return new BasicFeedback();
        }
        return null;
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    void digestPeriodic(Element root) {
        List periodics = AbstractRule.digestPeriodicsFactory(root);
        if (periodics.size() > 0) {
            setPeriodic((Periodic) periodics.get(0));
        }
    }

    /**
     *  Description of the Method
     *
     *@param  root  Description of the Parameter
     */
    public void digestGroupRules(Element root) {
        groupRules = new HashSet();
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            GroupRules gr = getGroupRules(child.getName());
            ;
            if (gr != null) {
                gr.digest(child);
                addGroupRules(gr);
            }
        }
    }

    /**
     *  Gets the groupRules attribute of the AbstractPolicy object
     *
     *@param  name  Description of the Parameter
     *@return       The groupRules value
     */
    protected GroupRules getGroupRules(String name) {
        if (name.equals("GroupRules")) {
            return new GroupRules();
        }
        return null;
    }

    /**
     *  Description of the Method
     *
     *@param  inputStream        Description of the Parameter
     *@exception  IOException    Description of the Exception
     *@exception  SAXException   Description of the Exception
     *@exception  JDOMException  Description of the Exception
     */
    public void configGroupRules(InputStream inputStream) throws IOException, SAXException, JDOMException {
        if (log.isDebugEnabled()) log.debug("configGroupRules");
        if (inputStream == null) {
            throw new IOException("NULL inputStream");
        }
        SAXBuilder builder = new SAXBuilder();
        InputStreamReader configReader = new InputStreamReader(inputStream);
        Document config = builder.build(configReader);
        configReader.close();
        digestGroupRules(config.getRootElement());
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void install() throws Exception {
        for (Iterator i = groupRules.iterator(); i.hasNext(); ) {
            Object obj = i.next();
            if (obj instanceof Install) {
                ((Install) obj).install();
            }
        }
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void uninstall() throws Exception {
        for (Iterator i = groupRules.iterator(); i.hasNext(); ) {
            Object obj = i.next();
            if (obj instanceof Install) {
                ((Install) obj).uninstall();
            }
        }
    }

    /**
     *  Sets the grouprules attribute of the Policy object
     *
     *@param  file  The new file value
     */
    public void setFile(String file) {
        if (log.isDebugEnabled()) log.debug("setFile with " + file);
        try {
            this.file = new File(AmiWall.CONF, file);
            InputStream is = null;
            is = new FileInputStream(this.file);
            configGroupRules(is);
        } catch (IOException e) {
            log.error("IOException - Failed to configUsers with " + file, e);
        } catch (Exception e) {
            log.error("Exception - Failed to configUsers with " + file, e);
        }
    }

    /**
     *  Gets the file attribute of the AbstractPolicy object
     *
     *@return    The file value
     */
    public String getFile() {
        return file.getAbsolutePath();
    }

    /**
     *  Sets the userHome attribute of the Policy object
     *
     *@param  userHome  The new userHome value
     */
    public void setUserHome(UserHome userHome) {
        if (log.isDebugEnabled()) log.debug("setUserHome");
        this.userHome = userHome;
    }

    /**
     *  Gets the userHome attribute of the AbstractPolicy object
     *
     *@return    The userHome value
     */
    public UserHome getUserHome() {
        return userHome;
    }

    /**
     *  Sets the groupHome attribute of the Policy object
     *
     *@param  groupHome  The new groupHome value
     */
    public void setGroupHome(GroupHome groupHome) {
        if (log.isDebugEnabled()) log.debug("setGroupHome");
        this.groupHome = groupHome;
    }

    /**
     *  Gets the groupHome attribute of the AbstractPolicy object
     *
     *@return    The groupHome value
     */
    public GroupHome getGroupHome() {
        return groupHome;
    }

    /**
     *  Sets the groupRules attribute of the Policy object
     *
     *@param  groupRules  The new groupRules value
     */
    public void addGroupRules(GroupRules groupRules) {
        this.groupRules.add(groupRules);
    }

    /**
     *  Gets the groupRules attribute of the Policy object
     *
     *@return    The groupRules value
     */
    public Set getGroupRules() {
        return groupRules;
    }

    /**
     *  Sets the feedback attribute of the Policy object
     *
     *@param  feedback  The new feedback value
     */
    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
        feedback.setPolicy(this);
    }

    /**
     *  Gets the feedback attribute of the AbstractPolicy object
     *
     *@return    The feedback value
     */
    public Feedback getFeedback() {
        return feedback;
    }

    /**
     *  Sets the start attribute of the Policy object
     *
     *@param  periodic  The new periodic value
     */
    public void setPeriodic(Periodic periodic) {
        this.periodic = periodic;
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activate() throws Exception {
        if (groupRules == null) {
            throw new NullPointerException("groupRules is NULL - this needs to be configured");
        }
        for (Iterator i = groupRules.iterator(); i.hasNext(); ) {
            GroupRules gr = (GroupRules) i.next();
            gr.activate();
        }
        if (feedback == null) {
            throw new NullPointerException("feedback is NULL - this needs to be configured");
        }
        feedback.activate();
        if (periodic == null) {
            throw new NullPointerException("periodic is NULL - this needs to be configured");
        }
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     *  Description of the Method
     */
    public void deactivate() {
        if (groupRules != null) {
            for (Iterator i = groupRules.iterator(); i.hasNext(); ) {
                GroupRules gr = (GroupRules) i.next();
                gr.deactivate();
            }
            groupRules = null;
        }
        if (feedback != null) {
            feedback.deactivate();
            feedback = null;
        }
        userHome = null;
        groupHome = null;
        periodic = null;
        stopRequested = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     *  Description of the Method
     */
    public void policeNow() {
        policeNow = true;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    /**
     *  Main processing method for the Policy object
     */
    public void run() {
        if (log.isDebugEnabled()) log.debug("run");
        while (!stopRequested) {
            long startOfPeriod = periodic.getStartOfCurrentPeriod() + periodic.getStart();
            long endOfPeriod = periodic.getStartOfCurrentPeriod() + periodic.getEnd();
            if (endOfPeriod < System.currentTimeMillis()) {
                startOfPeriod += periodic.getDuration();
                endOfPeriod += periodic.getDuration();
            }
            if (policeNow) {
                log.info("policeNow is set running now");
                police(startOfPeriod, endOfPeriod);
                policeNow = false;
            }
            if (log.isInfoEnabled()) {
                SimpleDateFormat df = new SimpleDateFormat();
                log.info("Waiting until " + df.format(new Date(endOfPeriod)) + " to police");
            }
            try {
                Thread.sleep(endOfPeriod - System.currentTimeMillis());
            } catch (InterruptedException e) {
                continue;
            }
            if (endOfPeriod < System.currentTimeMillis()) {
                if (log.isDebugEnabled()) log.debug("Ok running police now");
                police(startOfPeriod, endOfPeriod);
            } else {
                if (log.isDebugEnabled()) log.debug("Havent wait long enough, must have been interrupted");
            }
        }
        log.info("finished run");
    }

    /**
     *  Description of the Method
     *
     *@param  start  Description of the Parameter
     *@param  end    Description of the Parameter
     */
    public void police(long start, long end) {
        if (log.isInfoEnabled()) {
            SimpleDateFormat df = new SimpleDateFormat();
            log.info("police between " + df.format(new Date(start)) + " and " + df.format(new Date(end)));
        }
        try {
            for (Iterator i = groupRules.iterator(); i.hasNext(); ) {
                GroupRules gr = (GroupRules) i.next();
                police(gr, start, end);
            }
        } catch (Exception e) {
            log.error("police has a problem", e);
        }
    }

    /**
     *  Description of the Method
     *
     *@param  gr             Description of the Parameter
     *@param  startOfPeriod  Description of the Parameter
     *@param  endOfPeriod    Description of the Parameter
     *@exception  Exception  Description of the Exception
     */
    protected void police(GroupRules gr, long startOfPeriod, long endOfPeriod) throws Exception {
        Group group = gr.getGroup(groupHome);
        if (group == null) {
            log.error("Cant policy groupId=" + gr.getGroupId() + " because I cant find it in " + groupHome.getName());
        } else {
            for (Iterator u = group.getUsers(userHome).iterator(); u.hasNext(); ) {
                User user = (User) u.next();
                police(user, group, gr.getRules(), startOfPeriod, endOfPeriod);
            }
        }
    }

    /**
     *  Description of the Method
     *
     *@param  user           Description of the Parameter
     *@param  group          Description of the Parameter
     *@param  rules          Description of the Parameter
     *@param  startOfPeriod  Description of the Parameter
     *@param  endOfPeriod    Description of the Parameter
     *@exception  Exception  Description of the Exception
     */
    protected abstract void police(User user, Group group, Set rules, long startOfPeriod, long endOfPeriod) throws Exception;
}
