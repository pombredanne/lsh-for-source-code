package org.wicketrad.jpa.user;

import java.math.BigInteger;
import java.security.MessageDigest;
import org.wicketrad.jpa.UUIDGenerator;
import org.wicketrad.jpa.service.DefaultBeanService;

public class UserServiceImpl extends DefaultBeanService<User, String> implements UserService {

    public UserServiceImpl() {
        super(DefaultUser.class);
    }

    public UserServiceImpl(Class<? extends User> cls) {
        super(cls);
    }

    @Override
    public User create(User user) {
        user.setPassword(digest(user.getPassword()));
        return super.create(user);
    }

    @Override
    public void update(User user) {
        User u = findById(user.getId());
        if (user.getPassword() == null || u.getPassword().equals(user.getPassword()) || u.getPassword().equals(digest(user.getPassword()))) {
            user.setPassword(u.getPassword());
        } else {
            user.setPassword(digest(user.getPassword()));
        }
        super.update(user);
    }

    public UserSession signIn(String username, String password) {
        User u = findById(username);
        if (u != null) {
            if (u.getPassword().equals(digest(password))) {
                UserSession session = new UserSession();
                session.setSessionId(UUIDGenerator.generate());
                session.setUser(u);
                SessionStore.getInstance().put(session);
                return session;
            }
        }
        return null;
    }

    public void signOut(UserSession session) {
        SessionStore.getInstance().remove(session);
    }

    public boolean validateSession(UserSession session) {
        return SessionStore.getInstance().validate(session);
    }

    private String digest(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(message.getBytes());
            BigInteger hash = new BigInteger(1, md.digest());
            String hpassword = hash.toString(16);
            return hpassword;
        } catch (Exception e) {
        }
        return null;
    }
}
