package net.sf.mareco.aaam.services.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import net.sf.mareco.aaam.authentication.bo.User;
import net.sf.mareco.aaam.services.AaaService;
import net.sf.mareco.utils.persistence.Dao;
import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication and Authorization Service Implementation.
 * 
 * @author amirk
 */
@Service("aaaService")
public class AaaServiceImpl implements AaaService {

    private static final long serialVersionUID = 9153972555991302255L;

    @Autowired(required = true)
    private Dao dao;

    @Autowired(required = false)
    private MessageDigest digester;

    /**
	 * authenticates a user
	 */
    @Override
    @Transactional(readOnly = true)
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        authentication.setAuthenticated(false);
        User user = dao.loadByQuery("from User where username=:username", Collections.singletonMap("username", authentication.getName()));
        if (user != null && user.getPassword() != null && ((digester == null && user.getPassword().equals(authentication.getCredentials())) || (digester != null && MessageDigest.isEqual(user.getPassword().getBytes(), digester.digest(((String) authentication.getCredentials()).getBytes()))))) {
            return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        List<User> users = dao.getAll(User.class);
        for (User u : users) {
            u.getRoles().size();
        }
        return users;
    }

    @Override
    @Transactional
    public User saveUser(User u) {
        return dao.save(u);
    }

    public void setAlgorithm(String algorithm) throws NoSuchAlgorithmException {
        digester = MessageDigest.getInstance(algorithm);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean supports(Class authentication) {
        return authentication.isAssignableFrom(UsernamePasswordAuthenticationToken.class);
    }
}
