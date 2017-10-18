package bg.price.comparator.dao.jpa;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.persistence.EntityManager;

/**
 * Provides helper methods for the JPA data access object implementations.
 * <p>
 * The class contains static methods only, with default access modifier.
 * 
 * @author Ivan St. Ivanov
 */
public final class JpaDaoUtils {

    /**
     * Prepends and appends % signs to a parameter so that it is included like
     * this in the LIKE clause of a query.
     * 
     * @param param The string which should be decorated.
     * @return The passed string prepended and appended with % signs.
     */
    public static String compileLikeClause(String param) {
        return "%" + param + "%";
    }

    /**
     * Executes a JPA query in order to find whether an entity with a given
     * class and ID is handled by the given entity manager.
     * <p>
     * The methods executes the <code>find</code> method on the entity manager
     * providing the entity class and its ID as parameters.
     * 
     * @param <T> The type of the entity that should be looked for.
     * @param entityManager The entity manager which checks whether the instance
     *            exists in the persistence context.
     * @param entityClass The class of the entity which existence is checked.
     * @param id The ID of the entity which existence is checked.
     * @return <code>true</code> if an entity with the given ID exists in the
     *         persistence context.
     */
    public static <T> boolean exists(EntityManager entityManager, Class<T> entityClass, int id) {
        return entityManager.find(entityClass, id) != null;
    }

    /**
     * Calculates the hash of a given string that should be encrypted.
     * <p>
     * Used just before persisting user passwords or other sensible data. The
     * algorithm used to encrypt the string is one way. This means that it can
     * just be validated once stored in the database and not restored from
     * there.
     * 
     * @param password The string that should be encrypted.
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String getHash(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (password == null) {
            password = "";
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        return new String(digest.digest(password.getBytes("UTF-8")));
    }
}
