package net.sf.buildbox.redir;

import net.sf.buildbox.buildrobot.api.ContactsDao;
import net.sf.buildbox.buildrobot.model.Contact;
import net.sf.buildbox.util.BbxStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Redirects to configured avatar source.
 * Interesting links:
 * - http://opensocial-resources.googlecode.com/svn/spec/2.0/OpenSocial-Specification.xml
 * - http://portablecontacts.net/
 * - http://en.gravatar.com/site/implement/profiles/xml/
 * <p/>
 * TODO: Desired url support:
 * http://.../avatar?contactId=pkozelka
 * - by contactId
 * http://.../avatar?realm=uid:twitter.com&account=pkozelka
 * - by realm + account (support legacy frontends)
 * http://.../avatar?email=pkozelka@gmail.com
 * - tries contact db by email realms (specialized above case)
 * <p/>
 * always:
 * - if contact found uses its avatar
 * -   default is gravatar by primaryEmail; for null primaryEmail default is gravatar:monsterid
 * - if no contact found, uses gravatar for email(if given) with default=mysteryman
 */
@Controller
public class AvatarRedirHandler {

    private ContactsDao contactsDao;

    private String avatarUrlTemplate = "http://www.gravatar.com/avatar/${hash}?s=200&d=mm";

    private String mysteryManUrl = "http://www.gravatar.com/avatar/00000000000000000000000000000000?d=mm";

    @Autowired
    @Required
    public void setContactsDao(ContactsDao contactsDao) {
        this.contactsDao = contactsDao;
    }

    public void setAvatarUrlTemplate(String avatarUrlTemplate) {
        this.avatarUrlTemplate = avatarUrlTemplate;
    }

    public void setMysteryManUrl(String mysteryManUrl) {
        this.mysteryManUrl = mysteryManUrl;
    }

    @RequestMapping(value = "/avatar", method = RequestMethod.GET)
    public void avatar(HttpServletResponse response, @RequestParam(required = false) String contactId, @RequestParam(required = false) String email, @RequestParam(required = false) String realm, @RequestParam(defaultValue = "nobody") String account, @RequestParam(defaultValue = "") String defaultUrl) throws IOException {
        Contact contact = null;
        if (contactId != null) {
            contact = contactsDao.findContactByContactId(contactId);
        } else if (email != null) {
            contact = contactsDao.findContactByOtherId(Contact.getEmailRealm(email), Contact.getEmailAccount(email));
        } else if (realm != null) {
            contact = contactsDao.findContactByOtherId(realm, account);
        }
        String avatarUrl;
        if (contact == null) {
            if (email != null) {
                avatarUrl = emailToGravatarUrl(email);
            } else if ("".equals(defaultUrl)) {
                avatarUrl = mysteryManUrl;
            } else {
                avatarUrl = defaultUrl;
            }
        } else {
            avatarUrl = contact.getAvatarUrl();
            if (avatarUrl == null) {
                avatarUrl = emailToGravatarUrl(contact.getPrimaryEmail() == null ? email : contact.getPrimaryEmail());
            }
        }
        response.setContentType("image/*");
        response.sendRedirect(avatarUrl);
    }

    public static String hex(byte[] array) {
        final StringBuffer sb = new StringBuffer();
        for (byte b : array) {
            sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public static String md5Hex(String message) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String emailToGravatarUrl(String email) {
        if (email == null) return mysteryManUrl;
        final String lowercaseEmail = email.toLowerCase();
        final String hash = md5Hex(lowercaseEmail);
        final Map<String, String> props = new HashMap<String, String>();
        props.put("email", email);
        props.put("emailLowerCase", lowercaseEmail);
        props.put("hash", hash);
        System.out.println("props = " + props);
        return BbxStringUtils.expandProps(avatarUrlTemplate, props);
    }
}
