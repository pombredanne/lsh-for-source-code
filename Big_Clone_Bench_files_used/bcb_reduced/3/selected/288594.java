package scotlandyard.engine.impl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import scotlandyard.engine.spec.IEngine;
import scotlandyard.engine.spec.IUser;

/**
 * engine object which implements IEngine
 * @author simon
 * @version 3.0
 */
public class Engine implements IEngine {

    private static Engine engine;

    public Map<String, Boolean> icons;

    public Map<Integer, String> chat;

    public int chatCounter = 1;

    /**
	 * game engine constructor
	 */
    private Engine() {
        chat = new LinkedHashMap<Integer, String>() {

            private static final long serialVersionUID = 2973742865364731593L;

            @Override
            protected boolean removeEldestEntry(Entry<Integer, String> eldest) {
                return this.size() > 20;
            }
        };
        icons = new HashMap<String, Boolean>();
    }

    public Map<String, Game> games = new HashMap<String, Game>();

    public Map<String, User> users = new HashMap<String, User>();

    /**
	 * inserts player into the engine with duplicate name and email provide, or third party authorisation information
	 * @param email - email of user
	 * @param name - name of user
	 * @param sid
	 * @return user object
	 * @throws Exception - throws if the input detail is invalid
	 */
    @Override
    public synchronized IUser login(String email, String name, String sid) throws Exception {
        if (this.users.get(md5(email)) != null) {
            throw new Exception("Can not login: already in the users list");
        }
        String icon = getUnusedIcon();
        if (icons.get(icon) == null) {
            throw new Exception("Can not login: icon [" + icon + "] is not in the list");
        }
        if (icons.get(icon)) {
            throw new Exception("Can not login: icon is already chosen");
        }
        final User user = new User(icon, md5(email), sid, name, email);
        this.users.put(user.hash, user);
        icons.put(icon, Boolean.TRUE);
        return user;
    }

    /**
	 * gets icon that have not been used
	 * @return - name of icon
	 */
    private synchronized String getUnusedIcon() {
        String result = null;
        for (final String icon : icons.keySet()) {
            if (!icons.get(icon)) {
                result = icon;
                break;
            }
        }
        return result;
    }

    /**
	 * removes player from the engine since the user click on the logout button,
	 * clean up his/her account session in the severlet
	 * @param hash - identifier of user
	 * @throws Exception if the user is not in the users list
	 */
    @Override
    public void logout(String hash) throws Exception {
        if (this.users.get(hash) == null) {
            throw new Exception("Can not logout: user is not in the users list");
        }
        for (Game game : this.games.values()) {
            game.removePlayer(this.users.get(hash).email);
        }
        final User user = this.users.remove(hash);
        if (user != null) {
            icons.put(user.icon, Boolean.FALSE);
        }
    }

    /**
	 * game engine instance, games, users and maps will be stored under this instance
	 * @return a singleton instance of the game engine
	 */
    public static Engine instance() {
        if (engine == null) {
            engine = new Engine();
        }
        return engine;
    }

    /**
	 * gets an user object with hash identification
	 * @param hash
	 * @return user object
	 */
    @Override
    public IUser getUser(String hash) {
        return this.users.get(hash);
    }

    /**
	 * add message in the back of the Chat List
	 * @param msg
	 */
    public void addChat(String msg) {
        chat.put(chatCounter++, msg);
    }

    /**
     * gets Chat messages
     * @return list of messages
     */
    public Collection<String> getChat() {
        return chat.values();
    }

    /**
     * converts a string to its Hash value
     * @return string hash value of input string
     */
    public static String md5(String message) throws Exception {
        final MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(message.getBytes(), 0, message.length());
        return new BigInteger(1, m.digest()).toString(16);
    }

    public static String firstWord(String words) {
        final String firstWord;
        int spaceIndex = words.indexOf(' ');
        if (spaceIndex == -1) firstWord = words; else firstWord = words.substring(0, spaceIndex);
        return firstWord;
    }
}
