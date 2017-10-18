package au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml;

import static au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLUtils.xhtml;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.jdom2.Content;
import org.jdom2.Element;
import au.edu.uq.itee.eresearch.dimer.core.util.DigestUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.EmailUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.NodeUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.UserUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.ViewContext;

public class PasswordResetXHTMLPage extends XHTMLPage {

    private String email;

    private String linkReference;

    private Node user;

    public PasswordResetXHTMLPage(ViewContext context, String email, String linkReference) throws RepositoryException {
        super(context, "Password Reset");
        this.email = email;
        this.linkReference = linkReference;
        user = null;
        if (!email.isEmpty()) {
            List<Node> results = NodeUtils.getList(getUserByEmail(context, email));
            if (results.size() == 1) {
                user = results.get(0);
            }
        } else if (!linkReference.isEmpty()) {
            List<Node> results = NodeUtils.getList(getUserByLinkReference(context, linkReference));
            if (results.size() == 1) {
                user = results.get(0);
            }
        }
        addTab(new Tab("view", "View", buildViewContent()));
        if (linkReference.isEmpty() && !email.isEmpty() && !(user == null)) {
            String link = UserUtils.generateRandomString(50);
            EmailUtils.sendMail(email, "DIMER Password Reset", "You can reset your password here, " + context.getAppURL("/resetPassword", ViewContext.Format.HTML) + "?linkReference=" + link + ".");
            user.setProperty("passwordLinkReference", link);
            Calendar timestamp = Calendar.getInstance();
            timestamp.add(Calendar.MINUTE, 30);
            user.setProperty("passwordLinkExpiration", timestamp);
        } else if (!linkReference.isEmpty() && email.isEmpty() && !(user == null) && user.hasProperty("passwordLinkExpiration") && user.getProperty("passwordLinkExpiration").getDate().after(Calendar.getInstance())) {
            String newPassword = UserUtils.generateRandomString(8);
            EmailUtils.sendMail(user.getProperty("email").getString(), "DIMER Password Reset", "New password for DIMER account " + user.getName() + " - " + newPassword);
            user.setProperty("password", DigestUtils.digest(newPassword));
            user.setProperty("passwordLinkReference", "");
            Calendar timestamp = Calendar.getInstance();
            timestamp.add(Calendar.YEAR, -1);
            user.setProperty("passwordLinkExpiration", timestamp);
        }
        context.getSession().save();
    }

    private Collection<Content> buildViewContent() throws RepositoryException {
        Element content = new Element("div", xhtml).setAttribute("class", "content");
        content.addContent(new Element("h3", xhtml).setText("Password Reset"));
        if (linkReference.isEmpty() && email.isEmpty()) {
            content.addContent(new Element("form", xhtml).setAttribute("method", "post")).addContent(new Element("p", xhtml).setText("Please enter the email address of the account").addContent(new Element("br", xhtml)).addContent(new Element("input", xhtml).setAttribute("id", "email").setAttribute("name", "email").setAttribute("type", "text").setAttribute("size", "18")).addContent(new Element("input", xhtml).setAttribute("type", "submit").setAttribute("value", "Submit")));
        } else if (!email.isEmpty() && linkReference.isEmpty()) {
            if (user == null) {
                content.addContent(new Element("p", xhtml).setText("Unable to locate account."));
            } else {
                content.addContent(new Element("p", xhtml).setText("Password reset link sent to " + email));
            }
        } else if (email.isEmpty() && !linkReference.isEmpty()) {
            if (!(user == null) && user.hasProperty("passwordLinkExpiration") && user.getProperty("passwordLinkExpiration").getDate().after(Calendar.getInstance())) {
                content.addContent(new Element("p", xhtml).setText("New password has been sent to " + user.getProperty("email").getString()));
            } else {
                content.addContent(new Element("p", xhtml).setText("Invalid or expired link."));
            }
        }
        return Arrays.asList((Content) content);
    }

    public static QueryResult getUserByEmail(ViewContext context, String email) throws RepositoryException {
        @SuppressWarnings("deprecation") Query query = context.getSession().getWorkspace().getQueryManager().createQuery("/jcr:root/users/element(*, user) [\n" + "@email = \'" + email + "\']\n" + "order by @lastName, @firstName, @otherNames, @prefix, @suffix", Query.XPATH);
        return query.execute();
    }

    public static QueryResult getUserByLinkReference(ViewContext context, String passwordLinkReference) throws RepositoryException {
        @SuppressWarnings("deprecation") Query query = context.getSession().getWorkspace().getQueryManager().createQuery("/jcr:root/users/element(*, user) [\n" + "@passwordLinkReference = \'" + passwordLinkReference + "\']\n" + "order by @lastName, @firstName, @otherNames, @prefix, @suffix", Query.XPATH);
        return query.execute();
    }

    @SuppressWarnings("deprecation")
    public static QueryResult getUsers(ViewContext context, String passwordLinkReference) throws RepositoryException {
        return context.getSession().getWorkspace().getQueryManager().createQuery("/jcr:root/users/element(*, user) [\n" + "@lastName = \'McNaughton\']\n" + "order by @lastModified descending", Query.XPATH).execute();
    }
}
