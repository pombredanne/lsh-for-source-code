package com.submersion.jspshop.logintag;

import java.util.*;
import java.rmi.RemoteException;
import javax.servlet.jsp.*;
import javax.naming.*;
import javax.rmi.*;
import javax.servlet.http.*;
import javax.servlet.jsp.tagext.*;
import java.security.*;
import javax.ejb.*;
import com.submersion.jspshop.ejb.ValueHome;
import com.submersion.jspshop.ejb.Value;
import com.submersion.jspshop.ejb.ValueBean;

/** Checks if a email and password are correct
 * 
 * @author Jeff Davey (jeffdavey@submersion.com)
 * @see com.submersion.jspshop.classtag.LoginTEI
 * @version $Revision: 1.1.1.1 $
 * @created: September 27, 2001  
 * @changed: $Date: 2001/10/03 05:13:59 $
 * @changedBy: $Author: jeffdavey $
*/
public class LoginTag extends TagSupport {

    String email;

    String password;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int doStartTag() throws JspTagException {
        checkLogin(email, password);
        return EVAL_BODY_INCLUDE;
    }

    private void checkLogin(String email, String password) throws JspTagException {
        String cryptedPassword;
        try {
            MessageDigest crypt = MessageDigest.getInstance("MD5");
            crypt.update(password.getBytes());
            byte digest[] = crypt.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                hexString.append(hexDigit(digest[i]));
            }
            cryptedPassword = hexString.toString();
            crypt.reset();
            InitialContext context = new InitialContext();
            java.lang.Object homeRef = context.lookup("java:comp/env/ejb/Value");
            ValueHome valueHome = (ValueHome) PortableRemoteObject.narrow(homeRef, ValueHome.class);
            Value value = valueHome.findByPasswordCheck(email, cryptedPassword);
            pageContext.setAttribute("validLogin", new Boolean(true));
            HttpSession session = pageContext.getSession();
            session.setAttribute("jspShop.userID", value.getObjectID());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("jspShop: Could not get instance of MD5 algorithm. Please fix this!" + e.getMessage());
            e.printStackTrace();
            throw new JspTagException("Error crypting password!: " + e.getMessage());
        } catch (ObjectNotFoundException e) {
            pageContext.setAttribute("validLogin", new Boolean(false));
        } catch (NamingException e) {
            System.err.println("jspShop: Could not initialise context in LoginTag");
            e.printStackTrace();
        } catch (RemoteException e) {
            System.err.println("jspShop: Could not connect to container in LoginTag");
        } catch (FinderException e) {
            System.err.println("jspShop: Error using finderQuery in LoginTag");
        }
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
