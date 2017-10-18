package net.sourceforge.iwii.db.dev.security.impl;

import java.math.BigInteger;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.iwii.db.dev.bo.user.UserBO;
import net.sourceforge.iwii.db.dev.common.enumerations.LoginAttemptTypes;
import net.sourceforge.iwii.db.dev.common.utils.ServiceInjector;
import net.sourceforge.iwii.db.dev.context.api.IContextRepository;
import net.sourceforge.iwii.db.dev.context.api.IModelContext;
import net.sourceforge.iwii.db.dev.persistence.converters.api.IConverter;
import net.sourceforge.iwii.db.dev.persistence.converters.api.IConverterFactory;
import net.sourceforge.iwii.db.dev.persistence.dao.api.IDAOFactory;
import net.sourceforge.iwii.db.dev.persistence.entities.data.user.LoginAttemptEntity;
import net.sourceforge.iwii.db.dev.persistence.entities.data.user.UserEntity;
import net.sourceforge.iwii.db.dev.security.api.ISecurityProvider;

/**
 * Implementation of ISecurityProvider
 * 
 * @author Grzegorz 'Gregor736' Wolszczak
 * @version 1.00
 */
public class SecurityProvider implements ISecurityProvider {

    private static final Logger logger = Logger.getLogger(SecurityProvider.class.getName());

    private IDAOFactory daoFactory;

    private IConverterFactory converterFactory;

    private IContextRepository contextRepository;

    public SecurityProvider() {
        long start = System.currentTimeMillis();
        SecurityProvider.logger.log(Level.INFO, "Initializating...");
        this.daoFactory = ServiceInjector.injectSingletonService(IDAOFactory.class);
        this.contextRepository = ServiceInjector.injectSingletonService(IContextRepository.class);
        this.converterFactory = ServiceInjector.injectSingletonService(IConverterFactory.class);
        SecurityProvider.logger.log(Level.INFO, "Initialization done in " + String.valueOf(System.currentTimeMillis() - start) + " [ms]");
    }

    @Override
    public Boolean isLoginInformationCorrect(String userLogin, String password) {
        UserEntity user = this.daoFactory.getUserDAO().findByLoginAndPassword(userLogin, password);
        return user != null;
    }

    @Override
    public void performLoginOperation(String userLogin) {
        UserEntity entity = this.daoFactory.getUserDAO().findByLogin(userLogin);
        IConverter<UserEntity, UserBO> converter = this.converterFactory.createConverter(UserEntity.class, UserBO.class);
        UserBO user = converter.convertToBusinessObject(entity);
        IModelContext modelContext = this.contextRepository.getModelContext();
        this.contextRepository.initRepositoryForUser(user);
        this.contextRepository.getModelContext().populateWithModel(modelContext);
    }

    @Override
    public String hashString(String toHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            byte[] buffer = toHash.getBytes();
            digest.update(buffer);
            byte[] sha1Hash = digest.digest();
            BigInteger converter = new BigInteger(sha1Hash);
            return converter.toString(16);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SecurityProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public boolean isUserLogged() {
        return this.contextRepository.getContextOwner() != null;
    }

    @Override
    public void logLoginAttemp(String login, boolean success) {
        LoginAttemptEntity attempt = new LoginAttemptEntity(null, login, new Date(), success ? LoginAttemptTypes.Success : LoginAttemptTypes.Failure);
        this.daoFactory.getLoginAttemptDAO().save(attempt);
    }
}
