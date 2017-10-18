package org.amiwall.policy;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import org.amiwall.user.Group;
import org.amiwall.user.GroupHome;
import org.apache.log4j.Logger;
import org.amiwall.plugin.Install;
import org.amiwall.policy.Rule;
import org.jdom.Element;

/**
 *  Description of the Class
 *
 *@author    Nick Cunnah
 */
public class GroupRules implements Install {

    private static Logger log = Logger.getLogger("org.amiwall.policy.GroupRules");

    /**
     *  Description of the Field
     */
    protected Long groupId = null;

    /**
     *  Description of the Field
     */
    protected Set rules = null;

    public String getName() {
        return "GroupRules";
    }

    public String getDescription() {
        return getName() + " for groupId=" + groupId;
    }

    public void install() throws Exception {
        for (Iterator i = rules.iterator(); i.hasNext(); ) {
            Object obj = i.next();
            if (obj instanceof Install) ((Install) obj).install();
        }
    }

    public void uninstall() throws Exception {
        for (Iterator i = rules.iterator(); i.hasNext(); ) {
            Object obj = i.next();
            if (obj instanceof Install) ((Install) obj).uninstall();
        }
    }

    /**
     *  Sets the groupId attribute of the GroupRules object
     *
     *@param  groupId  The new groupId value
     */
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    /**
     *  Gets the groupId attribute of the GroupRules object
     *
     *@return    The groupId value
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     *  Gets the group attribute of the GroupRules object
     *
     *@param  groupHome  Description of the Parameter
     *@return            The group value
     */
    public Group getGroup(GroupHome groupHome) {
        return groupHome.findById(groupId.longValue());
    }

    /**
     *  Gets the rules attribute of the GroupRules object
     *
     *@return    The rules value
     */
    public Set getRules() {
        return rules;
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activate() throws Exception {
        for (Iterator i = rules.iterator(); i.hasNext(); ) {
            Rule rule = (Rule) i.next();
            rule.activate();
        }
    }

    /**
     *  Description of the Method
     */
    public void deactivate() {
        for (Iterator i = rules.iterator(); i.hasNext(); ) {
            Rule rule = (Rule) i.next();
            rule.deactivate();
        }
    }

    /**
     *  Description of the Method
     *
     *@param  digester  Description of the Parameter
     *@param  root      Description of the Parameter
     */
    public void digest(Element root) {
        if (log.isDebugEnabled()) log.debug("digest");
        try {
            setGroupId(new Long(root.getChildTextTrim("groupId")));
        } catch (Exception e) {
        }
        digestRules(root.getChild("rules"));
    }

    void digestRules(Element root) {
        rules = new HashSet();
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            Rule rule = getRule(child.getName());
            if (rule != null) {
                rule.digest(child);
                addRule(rule);
            }
        }
    }

    protected void addRule(Rule rule) {
        rules.add(rule);
    }

    protected Rule getRule(String name) {
        if (name.equals("MaxRule")) {
            return new MaxRule();
        }
        return null;
    }
}
