package org.tripcom.kerneltests.api2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

/**
 * This class creates unique IDs which are used to uniquely designate
 * Gridella objects.<BR>
 * <B>Remark:</B> java.security.SecureRandom was not used since it is only
 * available with JDK 1.2.
 * This class implements the <code>Singleton</code> pattern as defined by
 * Gamma et.al. As there could only exist one instance of this class, other
 * clients must use the <code>sharedInstance</code> function to use this class.
 *
 * @author <a href=mailto:"manfred.hauswirth@epfl.ch">Manfred Hauswirth</a>
 * @version 1.0.0
 */
class GUIDGenerator {

    /**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
    private static GUIDGenerator mSharedInstance = new GUIDGenerator();

    /**
	 * The random string <host>/<ip adress><date><random number> that will
	 * be digested.
	 */
    private String seed = null;

    /**
	 * The digester used to generate the unique ID.
	 */
    private MessageDigest md = null;

    /**
	 * the digest algorithm for generating the unique ID.
	 */
    private String algorithm = null;

    /**
	 * the random number generator.
	 */
    private SecureRandom rnd = new SecureRandom();

    private String host = null;

    /**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate GUIDGenerator directly will get an error at compile-time.
	 */
    private GUIDGenerator() {
        algorithm = "SHA";
        rnd.setSeed(System.currentTimeMillis());
    }

    /**
	 * Called by the public generation methods; generates the seed and
	 * digests it (using the algorithm defined in this.algorithm);
	 * the digest is the unique ID.<BR>
	 * The seed is a random string of the form
	 * <host>/<ip adress><date><random number> and generated anew with
	 * every call.
	 *
	 * @return the newly created byte array.
	 * @throws java.security.NoSuchAlgorithmException
	 *          if the algorithm is not available
	 *          in the caller's environment.
	 */
    private byte[] _generate() throws NoSuchAlgorithmException {
        if (host == null) {
            try {
                seed = InetAddress.getLocalHost().toString();
            } catch (UnknownHostException e) {
                seed = "localhost/127.0.0.1";
            } catch (SecurityException e) {
                seed = "localhost/127.0.0.1";
            }
            host = seed;
        } else {
            seed = host;
        }
        seed = seed + new Date().toString();
        seed = seed + Long.toString(rnd.nextLong());
        md = MessageDigest.getInstance(algorithm);
        md.update(seed.getBytes());
        return md.digest();
    }

    /**
	 * Generates a new unique ID using the default algorithm.
	 *
	 * @return the newly created uniqueID bytes.
	 */
    byte[] generate() {
        byte[] uid = null;
        try {
            uid = _generate();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return uid;
    }

    /**
	 * Generates a new unique ID using the specified algorithm.
	 *
	 * @param algorithm the digest algorithm to use. See Appendix A in the
	 *                  <a href="http://java.sun.com/products/jdk/1.2/docs/guide/security/CryptoSpec.html#AppA">
	 *                  Java Cryptography Architecture API Specification &
	 *                  Reference</a> for information about standard algorithm
	 *                  names.
	 * @return the newly created uniqueID bytes.
	 * @throws java.security.NoSuchAlgorithmException
	 *          if the algorithm is not available
	 *          in the caller's environment.
	 */
    byte[] generate(String algorithm) throws NoSuchAlgorithmException {
        this.algorithm = algorithm;
        return _generate();
    }

    /**
	 * Returns the seed that was used to generate the last unique ID.
	 *
	 * @return the seed used for generating the last unique ID.
	 */
    String getSeed() {
        return seed;
    }

    /**
	 * Returns the current digest algorithm that is used to generate
	 * unique IDs.
	 *
	 * @return the name of the current digest algorithm. See Appendix A in
	 *         the <a href="http://java.sun.com/products/jdk/1.2/docs/guide/security/CryptoSpec.html#AppA">
	 *         Java Cryptography Architecture API Specification &
	 *         Reference</a> for information about standard algorithm
	 *         names.
	 */
    String getAlgorithm() {
        return algorithm;
    }

    /**
	 * Sets the digest algorithm to be used for further calls of generate().
	 *
	 * @param algorithm the name of the digest algorithm. See Appendix A in
	 *                  the <a href="http://java.sun.com/products/jdk/1.2/docs/guide/security/CryptoSpec.html#AppA">
	 *                  Java Cryptography Architecture API Specification &
	 *                  Reference</a> for information about standard algorithm
	 *                  names.
	 */
    void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
	 * This creates the only instance of this class. This differs from the
	 * C++ standard implementation by Gamma et.al. since Java ensures the
	 * order of static initialization at runtime.
	 *
	 * @return the shared instance of Config.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
    public static GUIDGenerator sharedInstance() {
        return mSharedInstance;
    }
}
