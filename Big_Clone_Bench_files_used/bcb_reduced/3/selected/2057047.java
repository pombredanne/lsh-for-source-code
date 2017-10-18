package exfex.common.plugins.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import exfex.common.security.AuthenticationException;
import exfex.common.security.BeanFormatException;
import exfex.common.security.IAuthenticationBean;
import exfex.common.utils.Base64;

/** Password authentication bean.
 * 
 * This bean is created by {@link exfex.common.plugins.security.PasswordAuth}
 * class. It is intended to keep all data for password authentication and logic
 * for loading and saving its content. 
 * <br>
 * Bean information:
 * <ul>
 * <li> bean's type: <b>PASSWD</b>
 * <li> persistent format: 
 * <code>PASSWD:user_name:method:method_specific:password</code>
 * <br>
 * Where:<br>
 * 	<ul>
 * 	<li><code>PASSWD</code> is identificator, that following data can be 
 * 	used by this bean.
 * 	<li><code>user_name</code> is user name.
 * 	<li><code>method</code> is method used to write password.
 * 	<li><code>method_specific</code> specific data for method. 
 * 	It has format depending on method:
 * 		<ul>
 * 		<li>with <b>DIGEST</b> method has format 
 * 		<code>ALG</code> which stands for digest algorithm name.
 * 		<li>with <b>PLAIN</b> and <b>BASE64</b> methods has format
 * 		<code>encoding</code> which standands for enconding used for
 * 		string convertion to the byte array.
 * 		</ul> 
 * 	<li><code>password</code> text representation of password created using 
 * 	previousmethod field.
 * 	</ul>
 * <li>Additional method:
 * 	<ul>
 * 	<li>{@link #getAlgortihm()} resp. {@link #setAlgortihm(String)}
 * 	<li>{@link #getEncoding()} resp. {@link #setEncoding(String)}
 * 	<li>{@link #getMethod()} resp. {@link #setMethod(Methods)}
 * 	<li>{@link #getPasswd()}, {@link #getStringPasswd()} resp.
 * 	{@link #setPasswd(byte[])}, {@link #setStringPasswd(String, String)},
 * 	{@link #setDigestPasswd(byte[], String)}
 * 	</ul>
 * </ul>
 * Class is not synchronized and so has to be synchronized externaly if used in
 * multi-thread environment.
 * 
 * <p>
 * <pre>
 * Changes:
 * 22.11.2005	msts -	created
 * </pre>
 *
 * @author msts
 */
public class PasswordAuthBean implements IAuthenticationBean {

    /** Supported methods for this bean. 
	 *
	 * Each method defines how is password converted from its original 
	 * plain format. To change byte array representation from one method
	 * to another use static methods.
	 * 
	 * @see PasswordAuthBean#fromBase642Plain(byte[])
	 * @see PasswordAuthBean#fromPlain2Base64(String)
	 * @see PasswordAuthBean#fromPlainToDigest(byte[], String)
	 * @author msts
	 */
    public static enum Methods {

        /** Password is in original (text) form. 
		 * No changes has been made to it. This option is not secure for 
		 * persisting (or send it throught untrusted environment) 
		 * because everybody, who can read data, can read password and 
		 * misuse it.
		 */
        PLAIN, /** Password is not in original form, but it is used its digets
		 * instead. TO be more general, doesn't use concrete digest
		 * mechanism. Concrete algorithm is stored in  
		 * <br>
		 * This method is rather safe 
		 */
        DIGEST
    }

    ;

    /** Algorithm name used for specified method.
	 * In this moment it is considered only if {@link Methods#DIGEST} method
	 * is used and stands for digest algorithm.
	 * <br>
	 * Default value is SHA algorithm. 
	 */
    private String algortihm = "SHA";

    /** Method used for password persisting.
	 * This identifies how was created actual form of password from original
	 * one.
	 * <br>
	 * Default value is PLAIN method.
	 */
    private Methods method = Methods.PLAIN;

    /** Encoding name.
	 * This field is used when string representation of password has to be
	 * converted to the byte array.
	 * <br>
	 * Default value is ASCII encoding.
	 */
    private String encoding = "ASCII";

    /** Type of this bean.
	 * 
	 * This bean has PASSWD type.
	 */
    private static final String type = "PASSWD";

    /** User's name. */
    private String userName;

    /** Byte array representation of Password.
	 * This form of password is created using this {@link #method method}.
	 * Byte array is used primary because of digest methods and to avoid
	 * problems with charsets. To set String password use TODO method
	 */
    private byte[] passwd;

    /** Returns bean type.
	 * This bean will allways return PASSWD string (stored in {@link #type}
	 * field). 
	 */
    public String getType() {
        return type;
    }

    /** Cleans all sensitive data from bean.
	 * This includes userName and password. 
	 * <br>
	 * Note that {@link #check()} will fail on bean which was cleaned.
	 */
    public void clean() {
        setUserName(null);
        setPasswd(null);
    }

    /** Checks bean.
	 * This method should be called always when bean is completed and ready
	 * to be sent back to the system.
	 * <br>
	 * Checking process depends on used method but some parts are incommon.
	 * <ul>
	 * <li> <code>user</code> must be non null and not empty.
	 * <li> <code>password</code> can't be null - empty password is enabled 
	 * but not recomended (it is array with 0 lenght).
	 * <li> <code>method</code> has to be non null.
	 * 
	 * 	<ul>Specific checking according used method
	 * 	<li><b>PLAIN</b> - <code>encoding</code> has to be non null and 
	 * 	not empty.
	 * 	<li><b>DIGEST</b> - <code>algorithm</code> has to be non null 
	 * 	and not empty.
	 * 	</ul>
	 * </ul>
	 * 
	 * @throws BeanFormatException if at least one field is missing and
	 * exception message contains all missing fields separated by space.
	 */
    public void check() throws BeanFormatException {
        StringBuffer fields = new StringBuffer();
        if (userName == null || userName.length() == 0) fields.append("userName ");
        if (passwd == null) fields.append("passwd ");
        if (method == null) {
            fields.append("method ");
            throw new BeanFormatException(fields.toString());
        }
        switch(method) {
            case PLAIN:
                if (encoding == null || encoding.length() == 0) fields.append("encoding ");
                break;
            case DIGEST:
                if (algortihm == null || algortihm.length() == 0) fields.append("algortihm ");
                break;
        }
        if (fields.length() > 0) throw new BeanFormatException(fields.toString());
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        this.userName = name;
    }

    /** Saves bean to the output stream.
	 * TODO
	 */
    public void save(OutputStream output) throws IOException {
    }

    /** Loads bean from the input stream.
	 * TODO
	 */
    public void load(InputStream input) throws IOException, AuthenticationException {
    }

    /** Converts string to Base64 byte array representation.
	 * 
	 * Creates Base64 representation of given String and converts it to the 
	 * byte array.
	 * <br>
	 * This is just helper method, if password contains nonprintable chars
	 * and have to be transfered throught pure text medium (e.g. mail).
	 * 
	 * @see exfex.common.utils.Base64 for converting details.
	 * 
	 * @param str String to convert. 
	 * @return byte array created by converting given string using 
	 * Base64 algorithm.
	 */
    public static byte[] fromPlain2Base64(String str) {
        return Base64.decode(str);
    }

    /** Converts byte array (in Base64 format) to the String representation.
	 * 
	 * This is inverse method to {@link #fromPlain2Base64(String)}. It means
	 * that <code>orig = fromBase642Plain(fromPlain2Base64(orig))</code>
	 * 
	 * @see exfex.common.utils.Base64 for converting details.
	 * @param passwd Password Base64 representation in byte array.
	 * @return String representation in String form.
	 */
    public static String fromBase642Plain(byte[] passwd) {
        return Base64.encode(passwd);
    }

    /** Creates digest of given byte array.
	 * 
	 * Uses MessageDigest from Java standard security package for digest
	 * creation.
	 * 
	 * @param passwd Byte array representation of password. 
	 * @param alg Digest algorithm name.
	 * @return Byte array which is digest of given passwd.
	 * @throws NoSuchAlgorithmException if given alg is not recognised by
	 * Java Security package.
	 */
    public static byte[] fromPlainToDigest(byte[] passwd, String alg) throws NoSuchAlgorithmException {
        MessageDigest digestAlgorithm = MessageDigest.getInstance(alg);
        byte checkSum[] = null;
        digestAlgorithm.reset();
        digestAlgorithm.update(passwd);
        checkSum = digestAlgorithm.digest();
        digestAlgorithm.reset();
        return checkSum;
    }

    /** Returns name of the digest algorithm.
	 * 
	 * This method should be used only if method is set to 
	 * {@link Methods#DIGEST}.
	 * 
	 * @return Returns the algortihm.
	 */
    public String getAlgortihm() {
        return algortihm;
    }

    /** Sets name of the digest algorithm.
	 * 
	 * This method should be used only if method is set to 
	 * {@link Methods#DIGEST} and stands for algorithm used to produce 
	 * digest which is in the {@link #passwd} field.
	 *  
	 * @param algortihm The algortihm to set.
	 */
    public void setAlgortihm(String algortihm) {
        this.algortihm = algortihm;
    }

    /** Returns method used for password.
	 * 
	 * This field returns value of {@link #method} field. Verificator has
	 * to check this value to find out how was the password created.
	 * <br>
	 * Default value is PLAIN. 	  
	 * <p>
	 * Lets see following example:
	 * <br>
	 * Client is trying to login, he fills this bean and gives password in
	 * original (text) form. Verifier has to compare it with data from local
	 * (safe) storage. So it will find file (according configuration) and
	 * gets data (also in bean form - using {@link #load(InputStream)} 
	 * method). But this password is encrypted - or changed to prevent from
	 * reconstructing of original password. In this situation verifier has
	 * to know methods of both of them to compare them (e.g. change one 
	 * given from user to form gotten from configuration).
	 * 
	 * @return method name.
	 */
    public Methods getMethod() {
        return method;
    }

    /** Sets method which was used to create password representation.
	 * 
	 * This method should be called immediately after {@link #setPasswd} is
	 * called and {@link Methods method} used for password transformation
	 * should be supplied. 
	 * <br>
	 * Default value is PLAIN. 
	 * 
	 * @param method Method used for password.
	 */
    public void setMethod(Methods method) {
        this.method = method;
    }

    /** Returns password.
	 * 
	 * Controlling mechanism has to consider method used for password 
	 * transfomation.
	 * 
	 * @return Returns the passwd byte array.
	 */
    public byte[] getPasswd() {
        return passwd;
    }

    /** Returns string representation of password.
	 * 
	 * Uses {@link #passwd} and {@link #encoding} fields to reconstruct
	 * string from byte array internal password representation.
	 * 
	 * @return String representation of password or null if password is not
	 * set or encoding is null.
	 * @throws UnsupportedEncodingException If encoding is not supported.
	 */
    public String getStringPasswd() throws UnsupportedEncodingException {
        if (passwd == null || encoding == null) return null;
        return new String(passwd, encoding);
    }

    /** Sets password.
	 * Password is considered to be byte array.
	 * 
	 * @param passwd The passwd to set.
	 */
    public void setPasswd(byte[] passwd) {
        this.passwd = passwd;
    }

    /** Sets password from String.
	 * 
	 * Converts given password using given encoding and sets both passwd and
	 * encoding fields. Finaly sets method to the PLAIN.
	 * <br>
	 * NOTE: doesn't check if given passwd is non null.
	 * <br>
	 * Use this method rathar than specialized methods for setting each
	 * field separately (setPasswd and setMethod).
	 * 
	 * @param passwd String representation of password.
	 * @param encoding Enconding to be used for string to byte array 
	 * conversion.
	 * @throws UnsupportedEncodingException if given encoding couldn't be
	 * recognized.
	 */
    public void setStringPasswd(String passwd, String encoding) throws UnsupportedEncodingException {
        this.passwd = passwd.getBytes(encoding);
        this.encoding = encoding;
        this.method = Methods.PLAIN;
    }

    /** Sets password from digested byte array.
	 * 
	 * Sets password and algortihm fields according given parameters and
	 * sets method to the DIGEST.
	 * <br>
	 * Use this method rathar than specialized methods for setting each
	 * field separately (setPasswd, setMethod and setAlgortihm).
	 * 
	 * @param passwd byte array representation of digested password.
	 * @param alg Digest algorithm name used.
	 */
    public void setDigestPasswd(byte[] passwd, String alg) {
        this.passwd = passwd;
        this.algortihm = alg;
        this.method = Methods.DIGEST;
    }

    /** Returns encoding name used for string converting to the byte array.
	 * 
	 * This method should be used when method is {@link Methods#PLAIN} to 
	 * reconstruct correct form of password from byte array to String.
	 * <br>
	 * Default value is ASCII.
	 * 
	 * @return Returns the encoding.
	 */
    public String getEncoding() {
        return encoding;
    }

    /** Sets encoding used for String password representation to byte array.
	 * Use this method always when following methods {@link Methods#PLAIN} 
	 * are used. Use rather {@link #setStringPasswd(String, String)}. 
	 * <br>
	 * Default value is ASCII.
	 * 
	 * @param encoding The encoding to set.
	 */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
