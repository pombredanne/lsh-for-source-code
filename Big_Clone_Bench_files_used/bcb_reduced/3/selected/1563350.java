package com.google.code.openperfmon.service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.GenericJDBCException;
import com.google.code.openperfmon.dao.UserDAO;
import com.google.code.openperfmon.domain.AppUser;

public class UserService {

    private static Logger logger = Logger.getLogger(UserService.class);

    private UserDAO userDao;

    UserService(UserDAO userDao) {
        super();
        this.userDao = userDao;
    }

    public AppUser authenticate(String login, String password) {
        if (login == null || password == null) return null;
        AppUser usr = userDao.getByLogin(login);
        if ((usr == null) || !usr.getPassword().equals(md5(password))) return null;
        return usr;
    }

    public AppUser authenticate(String encoded) {
        String[] pair = encoded.split("~");
        if (pair.length != 2) return null;
        AppUser usr = userDao.getByLogin(pair[0]);
        if (usr == null || !usr.getPassword().equals(pair[1])) return null;
        return usr;
    }

    public String encode(AppUser usr) {
        return usr.getLogin() + "~" + usr.getPassword();
    }

    public void saveUser(AppUser usr, String pass) throws NonUniqueFieldException {
        if (pass != null) {
            usr.setPassword(md5(pass));
        }
        try {
            userDao.save(usr);
        } catch (ConstraintViolationException e) {
            AppUser existing = userDao.getByLogin(usr.getLogin());
            if (existing == null) {
                String msg = "Can't save user " + usr.toString() + " due to unknown reason";
                logger.error(msg, e);
                throw new IllegalStateException(msg, e);
            } else {
                throw new NonUniqueFieldException("User with login = '" + usr.getLogin() + "' already exist", e, "login");
            }
        }
    }

    public List<AdminEntityInfo<AppUser>> listUsers() {
        return listUsers(0, Integer.MAX_VALUE);
    }

    public List<AdminEntityInfo<AppUser>> listUsers(int start, int count) {
        List<AppUser> users = userDao.list(start, count);
        Collections.sort(users);
        List<AdminEntityInfo<AppUser>> inf = new ArrayList<AdminEntityInfo<AppUser>>(users.size());
        for (AppUser usr : users) {
            inf.add(new AdminEntityInfo<AppUser>(usr, true));
        }
        return inf;
    }

    public AppUser getById(Long id) {
        return userDao.getById(id);
    }

    public void deleteUser(Long id) throws ReferenceExistException {
        try {
            userDao.removeById(id);
        } catch (GenericJDBCException e) {
            String msg = "Can't delete user with id = " + id + " due to unknown reason";
            logger.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    private static String md5(String pwd) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(pwd.getBytes(), 0, pwd.length());
            return new BigInteger(1, md5.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new Error();
        }
    }

    public static void main(String[] args) {
        System.out.println(md5(args[0]));
    }
}
