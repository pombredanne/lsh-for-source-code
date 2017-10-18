package com.submersion.jspshop.propertytag;

import java.util.*;
import javax.servlet.jsp.*;
import javax.servlet.http.*;
import javax.servlet.jsp.tagext.*;
import javax.ejb.*;
import java.security.*;
import com.submersion.jspshop.rae.*;

/** Modifies a property value
 * 
 * @author Jeff Davey (jeffdavey@submersion.com)
 * @version $Revision: 1.1.1.1 $
 * @created: September 26, 2001  
 * @changed: $Date: 2001/10/03 05:14:01 $
 * @changedBy: $Author: jeffdavey $
*/
public class ModifyTag extends TagSupport {

    String value;

    public void setValue(String value) {
        this.value = value;
    }

    public int doStartTag() throws JspTagException {
        HttpSession session = pageContext.getSession();
        Long userID = (Long) session.getAttribute("jspShop.userID");
        if (userID == null) {
            throw new JspTagException("You are not logged in.");
        }
        modifyProperty(getId(), this.value, userID);
        return EVAL_BODY_INCLUDE;
    }

    private void modifyProperty(String valueID, String value, Long userID) throws JspTagException {
        Property property = new Property(new Long(valueID), userID);
        String newValue = value;
        System.out.println(property.getName());
        if (property.getName().equals("Password")) {
            try {
                MessageDigest crypt = MessageDigest.getInstance("MD5");
                crypt.update(value.getBytes());
                byte digest[] = crypt.digest();
                StringBuffer hexString = new StringBuffer();
                for (int i = 0; i < digest.length; i++) {
                    hexString.append(hexDigit(digest[i]));
                }
                newValue = hexString.toString();
                crypt.reset();
            } catch (NoSuchAlgorithmException e) {
                System.err.println("jspShop: Could not get instance of MD5 algorithm. Please fix this!" + e.getMessage());
                e.printStackTrace();
                throw new JspTagException("Error crypting password!: " + e.getMessage());
            }
        }
        property.setValue(newValue);
    }

    private String hexDigit(byte x) {
        StringBuffer sb = new StringBuffer();
        char c;
        c = (char) ((x >> 4) & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        sb.append(c);
        c = (char) (x & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        sb.append(c);
        return sb.toString();
    }
}
