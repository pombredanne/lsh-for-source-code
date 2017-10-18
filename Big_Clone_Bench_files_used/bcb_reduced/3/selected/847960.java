package com.mymoviejournal.service;

import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import com.mymoviejournal.bean.User;
import com.mymoviejournal.dao.UserDao;
import com.mymoviejournal.search.Page;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.MessagingException;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.providers.dao.SaltSource;

public class UserService {

    private UserDao userDao;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    private Properties mailProperties;

    public void setMailProperties(Properties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public Properties getMailProperties() {
        return mailProperties;
    }

    private PasswordEncoder passwordEncoder;

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    private SaltSource saltSource;

    public void setSaltSource(SaltSource saltSource) {
        this.saltSource = saltSource;
    }

    public SaltSource getSaltSource() {
        return saltSource;
    }

    public UserService() {
    }

    public void insert(User user) {
        getUserDao().insert(user);
    }

    public void update(User user) {
        getUserDao().update(user);
    }

    public void delete(User user) {
        getUserDao().delete(user);
    }

    public Page list(int page, int resultCount) {
        return getUserDao().list(page, resultCount);
    }

    public Page search(final String term, int page, int resultCount) {
        return getUserDao().search(term, page, resultCount);
    }

    public boolean verifyUser(User user, String code) throws Exception {
        String comparison = getValidationCode(user);
        return comparison.equals(code);
    }

    public void sendRegistrationEmail(User user) throws Exception {
        String code = getValidationCode(user);
        Authenticator authenticator = new Authenticator() {

            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("contact@mymoviejournal.com", "Montr3al");
            }
        };
        Session session = Session.getInstance(getMailProperties(), authenticator);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("contact@mymoviejournal.com"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
        message.setSubject("mymoviejournal Account Verification");
        message.setText("Hello " + user.getUsername() + ",\n\n" + "Thank you for registering with My Movie Journal. Please visit " + "http://mymoviejournal.com/app/user/validate?id=" + user.getId() + "&code=" + code + " to confirm your registration.\n\nThank you,\n\n\nMy Movie Journal staff");
        Transport.send(message);
    }

    private String getValidationCode(User user) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] msg = Double.valueOf(user.getJoined().getTime() + 43243242).toString().getBytes();
        md.update(msg);
        byte[] digest = md.digest();
        String code = "";
        for (int i = 0; i < digest.length; i++) code += Byte.toString(digest[i]);
        return code;
    }

    public boolean userExists(String username) {
        User user = getUserDao().findByUsername(username);
        return (user != null);
    }

    public User addSuperAdmin(String username, String email, String password, Boolean enabled, Boolean accountNonExpired, Boolean credentialsNonExpired, Boolean accountNonLocked, Date joined) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(getPasswordEncoder().encodePassword(password, getSaltSource().getSalt(null)));
        user.setEnabled(enabled);
        user.setAccountNonExpired(accountNonExpired);
        user.setCredentialsNonExpired(credentialsNonExpired);
        user.setAccountNonLocked(accountNonLocked);
        user.setRoles("ROLE_USER ROLE_SUPER_ADMIN");
        user.setJoined(joined);
        getUserDao().insert(user);
        return user;
    }

    public User addAdmin(String username, String email, String password, Boolean enabled, Boolean accountNonExpired, Boolean credentialsNonExpired, Boolean accountNonLocked, Date joined) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(getPasswordEncoder().encodePassword(password, getSaltSource().getSalt(null)));
        user.setEnabled(enabled);
        user.setAccountNonExpired(accountNonExpired);
        user.setCredentialsNonExpired(credentialsNonExpired);
        user.setAccountNonLocked(accountNonLocked);
        user.setRoles("ROLE_USER ROLE_ADMIN");
        user.setJoined(joined);
        getUserDao().insert(user);
        return user;
    }

    public User addRegularUser(String username, String email, String password, Boolean enabled, Boolean accountNonExpired, Boolean credentialsNonExpired, Boolean accountNonLocked, Date joined) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(getPasswordEncoder().encodePassword(password, getSaltSource().getSalt(null)));
        user.setEnabled(enabled);
        user.setAccountNonExpired(accountNonExpired);
        user.setCredentialsNonExpired(credentialsNonExpired);
        user.setAccountNonLocked(accountNonLocked);
        user.setRoles("ROLE_USER");
        user.setJoined(joined);
        getUserDao().insert(user);
        return user;
    }
}
