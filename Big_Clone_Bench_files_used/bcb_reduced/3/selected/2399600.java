package net.sourceforge.xsurvey.xscreator.service.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Resource;
import net.sourceforge.xsurvey.xscreator.dao.AppUserDao;
import net.sourceforge.xsurvey.xscreator.exception.XSServiceException;
import net.sourceforge.xsurvey.xscreator.service.AuthService;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Michal Dobrzanski
 */
@Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
public class AuthServiceImpl implements AuthService {

    private static Logger logger = Logger.getLogger(AuthServiceImpl.class);

    @Resource
    private AppUserDao appUserDao;

    @Override
    public String getAppUser(String user, String password) throws XSServiceException {
        try {
            return appUserDao.getAppUser(user, transformByMD5(password));
        } catch (DataAccessException e) {
            logger.warn("DataAccessException thrown while getting user:" + e.getMessage(), e);
            throw new XSServiceException("Database error while getting user");
        }
    }

    @Override
    public void save(String user, String password) throws XSServiceException {
        try {
            appUserDao.save(user, transformByMD5(password));
        } catch (DataAccessException e) {
            logger.warn("DataAccessException thrown while saving user:" + e.getMessage(), e);
            throw new XSServiceException("Database error while saving user");
        }
    }

    @Override
    public void update(String user, String password) throws XSServiceException {
        try {
            appUserDao.update(user, transformByMD5(password));
        } catch (DataAccessException e) {
            logger.warn("DataAccessException thrown while updating user:" + e.getMessage(), e);
            throw new XSServiceException("Database error while updating user");
        }
    }

    public String transformByMD5(String password) throws XSServiceException {
        MessageDigest md5;
        byte[] output;
        StringBuffer bufferPass;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.warn("DataAccessException thrown while getting MD5 algorithm:" + e.getMessage(), e);
            throw new XSServiceException("Database error while saving user");
        }
        md5.reset();
        md5.update(password.getBytes());
        output = md5.digest();
        bufferPass = new StringBuffer();
        for (byte b : output) {
            bufferPass.append(Integer.toHexString(0xff & b).length() == 1 ? "0" + Integer.toHexString(0xff & b) : Integer.toHexString(0xff & b));
        }
        return bufferPass.toString();
    }
}
