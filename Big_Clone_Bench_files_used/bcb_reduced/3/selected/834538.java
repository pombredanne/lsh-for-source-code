package org.amiwall.feedback;

import java.util.Map;
import org.amiwall.user.User;
import org.amiwall.plugin.AbstractPlugin;
import org.amiwall.delivery.Delivery;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import java.io.StringWriter;
import java.util.HashMap;
import org.apache.velocity.Template;
import java.util.Enumeration;
import org.apache.log4j.LogManager;
import org.amiwall.user.Group;
import org.amiwall.policy.Rule;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.LinkedList;
import org.amiwall.policy.ScoredRuleMetric;
import org.jdom.Element;

/**
 *  Description of the Class
 *
 *@author    Nick Cunnah
 */
public class BasicFeedback extends AbstractVelocityFeedback {

    private static Logger log = Logger.getLogger("org.amiwall.feedback.BasicFeedback");

    /**
     *  Description of the Field
     */
    protected VelocityContext context = null;

    /**
     *  Description of the Field
     */
    protected Template subjectTemplate = null;

    /**
     *  Description of the Field
     */
    protected Template contentTemplate = null;

    /**
     *  Description of the Field
     */
    protected Map map = new HashMap();

    /**
     *  Gets the name attribute of the BasicFeedback object
     *
     *@return    The name value
     */
    public String getName() {
        return "BasicFeedback";
    }

    /**
     *  Description of the Method
     *
     *@param  digester  Description of the Parameter
     *@param  root      Description of the Parameter
     */
    public void digest(Element root) {
        super.digest(root);
        for (Iterator i = root.getChildren("param").iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            addParam(child.getChildTextTrim("name"), child.getChildTextTrim("value"));
        }
    }

    /**
     *  Sets the map attribute of the BasicFeedback object
     *
     *@param  key    The feature to be added to the Param attribute
     *@param  value  The feature to be added to the Param attribute
     */
    public void addParam(String key, String value) {
        map.put(key, value);
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activate() throws Exception {
        super.activate();
        context = new VelocityContext(map);
        activateTemplates();
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activateTemplates() throws Exception {
        subjectTemplate = velocity.getTemplate("org/amiwall/feedback/BasicFeedback/subject.vm");
        contentTemplate = velocity.getTemplate("org/amiwall/feedback/BasicFeedback/content.vm");
    }

    /**
     *  Description of the Method
     *
     *@param  user               Description of the Parameter
     *@param  group              Description of the Parameter
     *@param  scoredRuleMetrics  Description of the Parameter
     *@exception  Exception      Description of the Exception
     */
    public void provideFeedback(User user, Group group, Collection scoredRuleMetrics) throws Exception {
        context.put("user", user);
        context.put("group", group);
        Map goodRules = new HashMap();
        Map badRules = new HashMap();
        for (Iterator i = scoredRuleMetrics.iterator(); i.hasNext(); ) {
            ScoredRuleMetric srm = (ScoredRuleMetric) i.next();
            if (srm.getScore().doubleValue() > 1.00) {
                badRules.put(srm.getRule(), srm.getMetric());
            } else {
                goodRules.put(srm.getRule(), srm.getMetric());
            }
        }
        if (log.isDebugEnabled()) log.debug("goodRules=" + goodRules.size());
        if (log.isDebugEnabled()) log.debug("badRules=" + badRules.size());
        context.put("goodRules", goodRules);
        context.put("badRules", badRules);
        StringWriter subject = new StringWriter();
        subjectTemplate.merge(context, subject);
        StringWriter content = new StringWriter();
        contentTemplate.merge(context, content);
        deliverFeedback(user, subject.toString(), content.toString());
    }

    /**
     *  Description of the Method
     *
     *@param  user           Description of the Parameter
     *@param  subject        Description of the Parameter
     *@param  content        Description of the Parameter
     *@exception  Exception  Description of the Exception
     */
    public void deliverFeedback(User user, String subject, String content) throws Exception {
        delivery.deliver(user, subject.toString(), content.toString());
    }
}
