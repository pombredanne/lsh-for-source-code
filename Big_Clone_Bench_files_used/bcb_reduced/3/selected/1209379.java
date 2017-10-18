package de.tum.in.eist.poll.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.ShortBlob;
import de.tum.in.eist.poll.shared.Lecturer;
import de.tum.in.eist.poll.shared.Poll;
import de.tum.in.eist.poll.shared.Student;
import de.tum.in.eist.poll.shared.User;
import de.tum.in.eist.poll.shared.exceptions.DuplicateUserException;
import de.tum.in.eist.poll.shared.exceptions.UserNotFoundException;

public class UserDatabaseManager {

    private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    public UserDatabaseManager() {
        Lecturer dummy = new Lecturer("foo", new byte[0], new byte[32]);
        try {
            addUser(dummy);
            changePassword(dummy, "bar");
        } catch (Exception e) {
        }
    }

    /**
	 * Returns the User object of the specified user stored in the database
	 * 
	 * @param login
	 *            identifier of the user
	 * @return User object of the user stored in the database
	 * @throws UserNotFoundException
	 *             if the specified user does not exist in the database
	 */
    public User getUser(String login) throws UserNotFoundException {
        Key userKey = KeyFactory.createKey("User", login);
        try {
            return asUser(datastore.get(userKey));
        } catch (EntityNotFoundException e) {
            throw new UserNotFoundException();
        }
    }

    /**
	 * Returns A list of all the Student (!) IDs currently in the database
	 * 
	 * @return List of strings representing the student IDs
	 */
    public List<String> getStudentIDList() {
        Query userq = new Query("User");
        List<String> studentIDList = new ArrayList<String>();
        for (Entity user : datastore.prepare(userq).asIterable()) {
            if (!(Boolean) user.getProperty("Lecturer")) {
                studentIDList.add((String) user.getKey().getName());
            }
        }
        return studentIDList;
    }

    /**
	 * adds a new uniquely identified user into the database
	 * 
	 * @param user
	 *            the user object to add
	 * @throws DuplicateUserException
	 *             if the database already contains a user with the same login
	 */
    public void addUser(User user) throws DuplicateUserException {
        if (this.contains(user)) {
            throw new DuplicateUserException();
        }
        datastore.put(asEntity(user));
    }

    /**
	 * Updates the users properties in the database.
	 * 
	 * @param user
	 *            the updated user object
	 * @throws UserNotFoundException
	 *             if the user does not exist in the database
	 */
    public void updateUser(User user) throws UserNotFoundException {
        if (!this.contains(user)) {
            throw new UserNotFoundException();
        }
        datastore.put(asEntity(user));
    }

    /**
	 * Deletes the specified user from the database
	 * 
	 * @param studentID
	 *            the identifier of the user
	 * @throws UserNotFoundException
	 *             if the specified user does not exist in the database
	 */
    public void deleteUser(String login) {
        Key userKey = KeyFactory.createKey("User", login);
        datastore.delete(userKey);
    }

    public boolean contains(User user) {
        Key userKey = KeyFactory.createKey("User", user.getLogin());
        try {
            datastore.get(userKey);
            return true;
        } catch (EntityNotFoundException e) {
            return false;
        }
    }

    public User validate(String login, String password) throws UserNotFoundException {
        User user = getUser(login);
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
        }
        digest.reset();
        digest.update(user.getSalt());
        byte[] pwHash = digest.digest(password.getBytes());
        if (!Arrays.equals(pwHash, user.getPassHash())) {
            throw new UserNotFoundException();
        }
        return user;
    }

    public void changePassword(User u, String newPassword) throws UserNotFoundException {
        Random r = new SecureRandom();
        r.nextBytes(u.getSalt());
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
        }
        digest.reset();
        digest.update(u.getSalt());
        u.setPassHash(digest.digest(newPassword.getBytes()));
        updateUser(u);
    }

    /**
	 * Converts an User object to an Entity object
	 * 
	 * @param user
	 *            the User to convert
	 * 
	 * @return the User as an Entity
	 */
    private static Entity asEntity(User user) {
        Entity userEntity = new Entity("User", user.getLogin());
        userEntity.setProperty("PassHash", new ShortBlob(user.getPassHash()));
        userEntity.setProperty("Salt", new ShortBlob(user.getSalt()));
        userEntity.setProperty("Lecturer", (Boolean) (user instanceof Lecturer));
        if (user instanceof Student) {
            List<Date> submittedPolls = new ArrayList<Date>();
            for (Poll poll : ((Student) user).getSubmittedPolls()) {
                submittedPolls.add(poll.getDate());
            }
            userEntity.setProperty("SubmittedPolls", submittedPolls);
        }
        return userEntity;
    }

    /**
	 * Converts an Entity object to an User object
	 * 
	 * @param e
	 *            the Entity to convert
	 * 
	 * @return the Entity as an User
	 */
    private static User asUser(Entity e) {
        String login = e.getKey().getName();
        byte[] passHash = ((ShortBlob) e.getProperty("PassHash")).getBytes();
        byte[] salt = ((ShortBlob) e.getProperty("Salt")).getBytes();
        if ((Boolean) e.getProperty("Lecturer")) {
            return new Lecturer(login, passHash, salt);
        }
        Student student = new Student(login, passHash, salt);
        List<Date> pollDateList = (List<Date>) e.getProperty("SubmittedPolls");
        if (null != pollDateList) {
            student.setSubmittedPolls(Database.pollDatabase.getPolls(pollDateList));
        }
        return student;
    }
}
