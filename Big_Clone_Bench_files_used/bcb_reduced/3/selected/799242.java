package au.edu.uq.itee.eresearch.dimer.webapp.app.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import au.edu.uq.itee.eresearch.dimer.core.util.DigestUtils;

public class UserUtils {

    public static Node createUser(Node parent, String name, String prefix, String firstName, String otherNames, String lastName, String suffix, String email, String password, String[] supervisors) throws RepositoryException {
        if (name.equals("anonymous") || name.equals("admin")) {
            throw new IllegalArgumentException("Reserved user name");
        }
        Node user = parent.addNode(name, "user");
        return updateUser(user, prefix, firstName, otherNames, lastName, suffix, email, password, supervisors);
    }

    public static Node updateUser(Node user, String prefix, String firstName, String otherNames, String lastName, String suffix, String email, String password, String[] supervisors) throws RepositoryException {
        user.setProperty("prefix", prefix);
        user.setProperty("firstName", firstName);
        user.setProperty("otherNames", otherNames);
        user.setProperty("lastName", lastName);
        user.setProperty("suffix", suffix);
        user.setProperty("email", email);
        user.setProperty("supervisors", supervisors);
        if (password != null && !password.equals("")) {
            user.setProperty("password", DigestUtils.digest(password));
        }
        return user;
    }

    public static String getFullNameFirst(Node user) throws RepositoryException {
        StringBuilder builder = new StringBuilder();
        if (user.hasProperty("prefix") && user.getProperty("prefix").getLength() > 0) {
            builder.append(user.getProperty("prefix").getString());
            builder.append(" ");
        }
        builder.append(user.getProperty("firstName").getString());
        builder.append(" ");
        if (user.hasProperty("otherNames") && user.getProperty("otherNames").getLength() > 0) {
            builder.append(user.getProperty("otherNames").getString());
            builder.append(" ");
        }
        builder.append(user.getProperty("lastName").getString());
        if (user.hasProperty("suffix") && user.getProperty("suffix").getLength() > 0) {
            builder.append(" ");
            builder.append(user.getProperty("suffix").getString());
        }
        return builder.toString();
    }

    public static String getFullNameLast(Node user) throws RepositoryException {
        StringBuilder builder = new StringBuilder();
        builder.append(user.getProperty("lastName").getString() + ", ");
        builder.append(user.getProperty("firstName").getString());
        if (user.hasProperty("otherNames") && user.getProperty("otherNames").getLength() > 0) {
            builder.append(" " + user.getProperty("otherNames").getString());
        }
        if (user.hasProperty("suffix") && user.getProperty("suffix").getLength() > 0) {
            builder.append(" " + user.getProperty("suffix").getString());
        }
        return builder.toString();
    }

    public static Node getUser(Session session) throws RepositoryException {
        String path = "/users/" + session.getUserID();
        return session.itemExists(path) ? (Node) session.getItem(path) : null;
    }

    public static QueryResult getAllUsers(Session session) throws RepositoryException {
        @SuppressWarnings("deprecation") Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/users/element(*, user)\n" + "order by @lastName, @firstName, @otherNames, @prefix, @suffix", Query.XPATH);
        return query.execute();
    }

    public static String generateRandomString(int length) {
        String str = "";
        for (int i = 0; i < length; i++) {
            str += Long.toString(Math.round(Math.random() * 32), 32);
        }
        return str;
    }
}
