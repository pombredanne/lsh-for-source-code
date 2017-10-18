package exfex.common.security;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import exfex.common.remote.security.IRemoteIdentity;

/** Identity class.
 * 
 * <h3>Overview</h3>
 * This class describes complete identity of client. It implements 
 * {@link exfex.common.security.ILocalIdentity} and 
 * {@link exfex.common.remote.security.IRemoteIdentity} interfaces to enable
 * local management (hidden from clients) of identity and its 
 * <code>UnicastRemoteObject</code> to remote distribution of methods from
 * remote view interface.
 * <br> 
 * All methods in this class which are not part of the local and remote view 
 * interfaces are intended for internal processes needed by identity to perform
 * all operations. 
 * <br>
 * Instance of this type is created by IdentityManager (TODO link) and further
 * manipulation can be done just using local resp. remote view interfaces 
 * methods.
 * <br>
 * To create instances of this class use static factory method 
 * {@link #getIdentity(Integer, User, String)}. It can't be created directly to 
 * prevent unintialized instances creation.
 * 
 * <h4>Security and checkSum generating</h4>
 * Checksum is used for remote identity authenticity examination. Without this
 * user could produce its own remote identity with fake numbers and so it could
 * guess e.g. loged admin identity and so crash the system. Checksum is 
 * generated from identity <code>id</code> and random <code>seed</code> set
 * in constructor. Form more details see {@link #generateIdentity()}. When 
 * remote identity is checked (using {@link #checkIntegrity(byte[])} method)
 * new checkSum is created and compared to one from given remote view checksum.
 * Attacer has to know <code>seed</code> value to crack the identity.
 * <br>
 * To prevent attacers from sniffing this information, encryption of 
 * comunication should be used. To prevent guess attacs, IdentityManager can
 * change internals and hashcode using {@link #generateIdentity()} method (this
 * doesn't affect client, because he should use remote reference with no data
 * and calling getCheckSum should return local value).      
 *  
 * <h3>Local view</h3>
 * Local view is intended for local manipulation made from trusted environment - 
 * typicaly from server process. This can for example be controlling of identity 
 * from IdentityManager or whoever can get instance of this type. 
 * IdentityManager keeps <code>ILocalIdentity</code> types as values and 
 * <code>id</code> as kyes in its map.
 * 
 * <h3>Remote view</h3>
 * This part of this class is used to be exported for remote clients using RMI. 
 * {@link exfex.common.remote.security.IRemoteIdentity} class extends Remote
 * interface and so RMI mechanism will send just stub of this object and so
 * client will have no chance to change values on local instance (remote view
 * interface doesn't permit it). 
 * It has just two purposes to inform about identity id number and its checkSum.
 * <code>id</code> is used to find this instance from IdentityManager and 
 * <code>checkSum</code> to examine correctness of remote view 
 * ({@link exfex.common.security.ILocalIdentity#checkIntegrity(byte[]) see}), 
 * to prevent from generated remote views from user.
 * <br>
 * Class has synchronized methods and can be used safely in multi thread
 * environment.
 * 
 * <p>
 * <pre>
 * Changes:
 * 27.10.2005	msts -	created
 * </pre>
 *
 * @author msts
 */
public class Identity extends UnicastRemoteObject implements ILocalIdentity, IRemoteIdentity {

    private static final long serialVersionUID = 1L;

    /** Id number of this identity.
	 * This value is used as the identificator of this instance.
	 */
    private final Integer id;

    /** Profile of the user symbolized by this identity.
	 * This value is used when authorization is examined.
	 */
    private final User profile;

    /** Digest algorithm used to created checkSum.
	 * 
	 * @see #generateIdentity() method for more information about checksum 
	 * generating.
	 */
    private final MessageDigest digestAlgorithm;

    /** Checksum of this instance.
	 * This value is used to check integrity of remote view to avoid 
	 * exploiting of identity.
	 * 
	 * @see #checkIntegrity(byte[]) Method for more details.
	 */
    private byte[] checkSum = null;

    /** Generator of random numbers.
	 * This generator is used to created number used for checkSum creation.
	 * 
	 * @see #generateIdentity() for details of generating.  
	 */
    private Random rndGen = new Random(System.nanoTime());

    /** Size of the seed number in bits. 
	 * This seed is used to byte source for digest creating.
	 */
    public static final int SEED_BITS_COUNT = 1024;

    /** Constructor without initialization.
	 * This constructor is forbiden because we do want prevent from 
	 * uninitialized instance creation.
	 *
	 */
    private Identity() throws RemoteException {
        this.id = null;
        this.profile = null;
        this.digestAlgorithm = null;
    }

    /** Initializing constructor.
	 * 
	 * Calls supertype constructor to initialize RMI stuff. Sets internal
	 * fields and Calls <code>generateIdentity</code> to initialize all 
	 * security stuff.
	 * <br>
	 * NOTE:
	 * Constructor can't be used directly because of sanity checking of 
	 * parameters (initialization of supertype has to be done at the 
	 * begining of constructor and if values were wrong, it will be hard to
	 * destroy running supertype). To create instance use static factory 
	 * method {@link #getIdentity(Integer, User, String)}.
	 * 
	 * @param id Identificator of identity. This value should be produced
	 * by IdentityManager.
	 * @param digestAlg Digest algorithm to be used to create seed. 
	 * @param profile All information about user symbolized by this identity
	 * @throws RemoteException If RMI problem occures.
	 */
    private Identity(Integer id, User profile, MessageDigest digestAlg) throws RemoteException {
        super();
        this.id = id;
        this.profile = profile;
        this.digestAlgorithm = digestAlg;
        generateIdentity();
    }

    /** Factory method for Identity instances creation.
	 * 
	 * Checks given parameters and if they are ok, creates new instance
	 * of Identity class.
	 *  
	 * @param id Identificator of identity. This value should be produced
	 * by IdentityManager.
	 * @param profile All information about user symbolized by this identity
	 * @param digestAlg Digest algorithm to be used when created seed.
	 * @return Identity instance.
	 * @throws RemoteException If RMI problem occures.
	 * @throws NullPointerException If at least on of the parameters is null
	 * @throws NoSuchAlgorithmException If given algorithm is not supported.
	 */
    public static Identity getIdentity(Integer id, User profile, String digestAlg) throws RemoteException, NullPointerException, NoSuchAlgorithmException {
        if (id == null || profile == null || digestAlg == null) throw new NullPointerException("Parameter null");
        MessageDigest digestAlgorithm = MessageDigest.getInstance(digestAlg);
        return new Identity(id, profile, digestAlgorithm);
    }

    /** Generates checkSum from randomly generated big number.
	 * 
	 * Creates random number with SEED_BITS_COUNT bits size. Uses 
	 * digestAlgorithm implementation of MessageDigest to produce digest 
	 * from bytes from this number. Output of the algorithm is used as 
	 * checkSum. If everythimg goes well, sets <code>checkSum</code> 
	 * internal field.
	 */
    public synchronized void generateIdentity() {
        BigInteger seed = new BigInteger(SEED_BITS_COUNT, rndGen);
        byte checkSum[] = null;
        digestAlgorithm.reset();
        digestAlgorithm.update(seed.toByteArray());
        checkSum = digestAlgorithm.digest();
        digestAlgorithm.reset();
        this.checkSum = checkSum;
    }

    /** Returns user profile instance field.
	 * 
	 * This value is set in constructor.
	 * @return Instance of user profile.
	 */
    public synchronized User getUser() {
        return profile;
    }

    /** Checks if given byte array is same as checkSum.
	 * 
	 * Given checkSum is same as internal one iff:
	 * <ul>
	 * <li>if internal checksum is null, given also has to be null.
	 * <li>both arrays has same size.
	 * <li>all bytes in array are same.
	 * </ul>
	 * 
	 * @param checkSum Checksum candidate.
	 * @return true if given checkSum is same as internal one. 
	 */
    public synchronized boolean checkIntegrity(byte[] checkSum) {
        if (this.checkSum == null) return (checkSum == null) ? true : false;
        if (this.checkSum.length != checkSum.length) return false;
        for (int i = 0; i < this.checkSum.length; i++) if (this.checkSum[i] != checkSum[i]) return false;
        return true;
    }

    /** Returns checkSum of this identity.
	 * 
	 * @return copy of the checkSum field value.
	 * @throws RemoteException If RMI problem's occured.
	 */
    public synchronized byte[] getCheckSum() throws RemoteException {
        return checkSum;
    }

    /** Returns id of this identity.
	 * 
	 * @return Copy of id field value.
	 * @throws RemoteException If RMI problem's occured.
	 */
    public Integer getId() throws RemoteException {
        return id;
    }

    @Override
    public String toString() {
        return "Identity[user=" + profile + "; id=" + id + "]";
    }
}
