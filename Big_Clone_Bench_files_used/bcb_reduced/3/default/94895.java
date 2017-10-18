import java.util.*;
import java.io.UnsupportedEncodingException;
import java.security.*;

public class UserList implements java.io.Serializable {

    /**
		 * List of all users in the Group Server, as well as their passwords.
		 * Users are stored in a hash table in the form username => User
		 * Passwords are stored in a hash table in the form username => password
		 */
    private static final long serialVersionUID = 7600343803563417992L;

    private Hashtable<String, User> list = new Hashtable<String, User>();

    private Hashtable<String, String> passwords = new Hashtable<String, String>();

    public synchronized void addUser(String username) {
        User newUser = new User();
        list.put(username, newUser);
    }

    public synchronized void addUser(String username, String password) throws UnsupportedEncodingException {
        User newUser = new User();
        list.put(username, newUser);
        passwords.put(username, hash(password));
    }

    public synchronized boolean checkPassword(String username, String password) throws UnsupportedEncodingException {
        String passHash = hash(password);
        String storedVal = passwords.get(username);
        if (passHash.equals(storedVal)) return true; else return false;
    }

    public synchronized void deleteUser(String username) {
        list.remove(username);
    }

    public synchronized boolean checkUser(String username) {
        if (list.containsKey(username)) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized ArrayList<String> getUserGroups(String username) {
        return list.get(username).getGroups();
    }

    public synchronized ArrayList<String> getUserOwnership(String username) {
        return list.get(username).getOwnership();
    }

    public synchronized void addGroup(String user, String groupname) {
        list.get(user).addGroup(groupname);
    }

    public synchronized void removeGroup(String user, String groupname) {
        list.get(user).removeGroup(groupname);
    }

    public synchronized void addOwnership(String user, String groupname) {
        list.get(user).addOwnership(groupname);
    }

    public synchronized void removeOwnership(String user, String groupname) {
        list.get(user).removeOwnership(groupname);
    }

    private String hash(String password) throws UnsupportedEncodingException {
        final StringBuilder hashString = new StringBuilder();
        byte[] plaintext = password.getBytes("UTF-8");
        byte[] ciphertext;
        try {
            MessageDigest hashAlgo = MessageDigest.getInstance("MD5");
            hashAlgo.update(plaintext);
            ciphertext = hashAlgo.digest();
            for (byte block : ciphertext) {
                hashString.append(Character.forDigit((block >> 4) & 0xf, 16));
                hashString.append(Character.forDigit(block & 0xf, 16));
            }
            return hashString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Could not find hashing algorithm. " + e.getMessage());
        }
        return null;
    }

    class User implements java.io.Serializable {

        /**
		 * 
		 */
        private static final long serialVersionUID = -6699986336399821598L;

        private ArrayList<String> groups;

        private ArrayList<String> ownership;

        public User() {
            groups = new ArrayList<String>();
            ownership = new ArrayList<String>();
        }

        public ArrayList<String> getGroups() {
            return groups;
        }

        public ArrayList<String> getOwnership() {
            return ownership;
        }

        public void addGroup(String group) {
            groups.add(group);
        }

        public void removeGroup(String group) {
            if (!groups.isEmpty()) {
                if (groups.contains(group)) {
                    groups.remove(groups.indexOf(group));
                }
            }
        }

        public void addOwnership(String group) {
            ownership.add(group);
        }

        public void removeOwnership(String group) {
            if (!ownership.isEmpty()) {
                if (ownership.contains(group)) {
                    ownership.remove(ownership.indexOf(group));
                }
            }
        }
    }
}
