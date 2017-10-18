package org.qtitools.assessr.cayenne;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.qtitools.assessr.cayenne.auto._Tutor;
import org.qtitools.assessr.util.HexString;

public class Tutor extends _Tutor {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public List<Assessment> getOwnedAssessments() {
        DataContext context = DataContext.getThreadDataContext();
        Expression qualifier = ExpressionFactory.matchExp(Assessment.OWNER_PROPERTY, this);
        SelectQuery select = new SelectQuery(Assessment.class, qualifier);
        return context.performQuery(select);
    }

    @SuppressWarnings("unchecked")
    public List<Assessment> getSharedAssessments() {
        DataContext context = DataContext.getThreadDataContext();
        Expression qualifier = ExpressionFactory.matchExp("groups.tutors", this);
        SelectQuery select = new SelectQuery(Assessment.class, qualifier);
        return context.performQuery(select);
    }

    public Group getPrimaryGroup() {
        DataContext context = DataContext.getThreadDataContext();
        final Expression qualifier = Expression.fromString("name = $name and tutors = $tutor");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", getUsername());
        params.put("tutors", this);
        SelectQuery select = new SelectQuery(Group.class, qualifier.expWithParameters(params));
        List groups = context.performQuery(select);
        if (groups.size() == 0) return null;
        if (groups.size() > 1) throw new RuntimeException("invalid database contents -- multiple groups with same name!");
        return (Group) groups.get(0);
    }

    protected static String encodePassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes());
            return HexString.bufferToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPassword(String password) {
        super.setPassword(encodePassword(password));
    }

    public static Tutor getTutor(String user, String pass) {
        DataContext context = DataContext.getThreadDataContext();
        final Expression qualifier = Expression.fromString("username = $user and password = $pass");
        Map<String, String> params = new HashMap<String, String>();
        params.put("user", user);
        params.put("pass", encodePassword(pass));
        SelectQuery select = new SelectQuery(Tutor.class, qualifier.expWithParameters(params));
        List tutors = context.performQuery(select);
        if (tutors.size() == 0) return null;
        if (tutors.size() > 1) throw new RuntimeException("invalid database contents -- multiple users with same username!");
        return (Tutor) tutors.get(0);
    }

    @SuppressWarnings("unchecked")
    public static List<Tutor> findByUsername(String user) {
        DataContext context = DataContext.getThreadDataContext();
        final Expression qualifier = Expression.fromString("username = $user");
        Map<String, String> params = new HashMap<String, String>();
        params.put("user", user);
        SelectQuery select = new SelectQuery(Tutor.class, qualifier.expWithParameters(params));
        List tutors = context.performQuery(select);
        return tutors;
    }

    public static boolean usernameExists(String user) {
        return (findByUsername(user).size() == 1);
    }

    @SuppressWarnings("unchecked")
    public static List<Tutor> findByEmail(String email) {
        DataContext context = DataContext.getThreadDataContext();
        final Expression qualifier = Expression.fromString("email = $email");
        Map<String, String> params = new HashMap<String, String>();
        params.put("email", email);
        SelectQuery select = new SelectQuery(Tutor.class, qualifier.expWithParameters(params));
        List tutors = context.performQuery(select);
        return tutors;
    }

    public static boolean emailExists(String email) {
        return (findByEmail(email).size() == 1);
    }
}
