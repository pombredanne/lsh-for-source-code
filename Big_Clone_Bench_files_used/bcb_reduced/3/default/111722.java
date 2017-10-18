import com.strongauth.skcengine.*;
import java.util.logging.Logger;

/**
 * This program demonstrates how an application client will use
 * the StrongKey CryptoEngine "core" Library to encrypt and decrypt
 * files, while using the StrongAuth KeyAppliance (SAKA) to escrow 
 * and recover cryptographic keys.  The library stores/retrieves 
 * encrypted files into/from public clouds, as well as from private-
 * clouds built on Eucalyptus.  It can also authenticate users against 
 * an LDAP directory (such as OpenDS or Active Directory) for service-
 * requests.
 */
public class skceDriver {

    static String operation = null;

    static String filename = null;

    static String digest = null;

    static String did = null;

    static String username = null;

    static String password = null;

    static String crypto = null;

    static SKCE skce = null;

    /** Creates a new instance of skceDriver */
    public skceDriver() {
    }

    /**
     * The main method
     * @param args the command line arguments:
     * args[0]: String – Whether to perform an authentication, encryption, decryption or message-digest
     * args[1]: String – The file containing the plaintext or ciphertext data
     * args[2]: String – Optional DID of the encryption domain or optional digest-type for digest
     * args[3]: String – Optional username for authentication to the SAKA
     * args[4]: String – Optional password for authentication to the SAKA
     */
    public static void main(String[] args) {
        Logger logger = common.getLogger("skceDriver");
        try {
            skce = new SKCEImpl();
            switch(args.length) {
                case 5:
                    operation = args[0];
                    filename = args[1];
                    did = args[2];
                    username = args[3];
                    password = args[4];
                    System.out.println("Parameters passed in:\n" + "\tenc|dec: " + operation + "\n" + "\tFilename: " + filename + "\n" + "\tDID: " + did + "\n" + "\tUsername: " + username + "\n" + "\tPassword: " + password);
                    break;
                case 4:
                    operation = args[0];
                    if (operation.equals("auth")) {
                        username = args[1];
                        password = args[2];
                        crypto = args[3];
                        System.out.println("Parameters passed in:\n" + "\tauth|enc|dec: " + operation + "\n" + "\tUsername: " + username + "\n" + "\tPassword: " + password + "\n" + "\tCrypto: " + crypto);
                    } else {
                        filename = args[1];
                        username = args[2];
                        password = args[3];
                        System.out.println("Parameters passed in:\n" + "\tauth|enc|dec: " + operation + "\n" + "\tFilename: " + filename + "\n" + "\tUsername: " + username + "\n" + "\tPassword: " + password);
                    }
                    break;
                case 3:
                    operation = args[0];
                    if (operation.equals("auth")) {
                        username = args[1];
                        password = args[2];
                        System.out.println("Parameters passed in:\n" + "\tauth|enc|dec: " + operation + "\n" + "\tUsername: " + username + "\n" + "\tPassword: " + password);
                    } else {
                        filename = args[1];
                        digest = args[2];
                        System.out.println("Parameters passed in:\n" + "\tdig: " + operation + "\n" + "\tFilename: " + filename + "\n" + "\tDigest: " + digest);
                    }
                    break;
                case 2:
                    operation = args[0];
                    filename = args[1];
                    System.out.println("Parameters passed in:\n" + "\tenc|dec|dig: " + operation + "\n" + "\tFilename: " + filename);
                    break;
                case 1:
                    break;
                default:
                    System.err.println("\nUsage: skceDriver auth  <ldap-username> <ldap-password> [ENC|DEC|CMV]\n" + "       skceDriver dec   <filename> [<saka-username> <saka-password>]\n" + "       skceDriver dig   <filename> [sha1|sha256|sha384|sha512]\n" + "       skceDriver enc   <filename> [<did> <saka-username> <saka-password>]\n");
                    return;
            }
            if (operation.equals("auth")) {
                System.out.println("Authenticating...");
                if (args.length == 4) System.out.println("Result of authentication: " + skce.authenticateUser(username, password, crypto)); else System.out.println("Result of authentication: " + skce.authenticateUser(username, password));
            } else if (operation.equals("enc")) {
                System.out.println("Encrypting...");
                if (args.length == 5) System.out.println("Result of encryption: " + skce.encrypt(did, username, password, filename, true, "DESede", 0)); else System.out.println("Encrypted output-file: " + skce.encrypt(filename));
            } else if (operation.equals("dec")) {
                System.out.println("Decrypting...");
                if (args.length == 4) System.out.println("Result of decryption: " + skce.decrypt(username, password, filename)); else System.out.println("Decrypted output-file: " + skce.decrypt(filename));
            } else if (operation.equals("dig")) {
                System.out.println("Digesting...");
                if (args.length == 3) System.out.println("Message-digest (" + digest + "): " + skce.digest(digest, filename)); else System.out.println("Message-digest (SHA-256): " + skce.digest("sha256", filename));
            }
        } catch (Exception e) {
            System.err.println("Server error message is: " + e.getLocalizedMessage());
            e.printStackTrace(System.err);
        }
    }
}
