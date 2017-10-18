package org.amiwall.feedback;

import java.util.Map;
import org.amiwall.user.User;
import org.amiwall.plugin.AbstractPlugin;
import org.amiwall.delivery.Delivery;
import org.amiwall.policy.Policy;
import org.jdom.Element;
import org.amiwall.delivery.EmailDelivery;
import java.util.Iterator;

/**
 *  Description of the Class
 *
 *@author    Nick Cunnah
 */
public abstract class AbstractFeedback extends AbstractPlugin implements Feedback {

    /**
     *  Description of the Field
     */
    protected Delivery delivery = null;

    protected Policy policy = null;

    /**
     *  Sets the delivery attribute of the AbstractFeedback object
     *
     *@param  delivery  The new delivery value
     */
    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    /**
     *  Description of the Method
     *
     *@param  digester  Description of the Parameter
     *@param  root      Description of the Parameter
     */
    public void digest(Element root) {
        digestDelivery(root);
    }

    void digestDelivery(Element root) {
        for (Iterator i = root.getChildren().iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            Delivery delivery = getDelivery(child.getName());
            if (delivery != null) {
                delivery.digest(child);
                setDelivery(delivery);
                break;
            }
        }
    }

    protected Delivery getDelivery(String name) {
        if (name.equals("EmailDelivery")) {
            return new EmailDelivery();
        }
        return null;
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activate() throws Exception {
        if (delivery == null) {
            throw new NullPointerException("delivery is NULL, this needs to be configured.");
        }
        delivery.activate();
        if (policy == null) {
            throw new NullPointerException("policy is NULL, this needs to be configured.");
        }
    }

    /**
     *  Description of the Method
     */
    public void deactivate() {
        delivery.deactivate();
    }
}
