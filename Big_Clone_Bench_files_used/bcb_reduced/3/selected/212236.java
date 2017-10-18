package mud.core;

import mud.core.MessageQueue;
import mud.core.exceptions.move_error;
import mud.core.exceptions.system_exception;
import mud.core.members.*;
import mud.core.types.objectProperty;
import mud.data.Database;
import mud.server.*;
import mud.Log;
import mud.Util;
import mud.Control;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.lang.reflect.Array;
import static mud.core.Permission.*;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * User: Michael Keller
 * Date: Dec 3, 2004
 * Copyright by Continuum.
 * Do not use for nuclear reactors and time machines.
 */
public final class User extends object implements MessageReceiver, ConnectionErrorHandler {

    private static HashMap<String, User> connectedUsers = new HashMap<String, User>();

    public static final String KEY_PASSHASH = "passHash";

    public static final String KEY_PERMISSION = "permission";

    public static final String KEY_WANTSCOLOR = "wantsColor";

    public static final String KEY_WANTSECHO = "wantsEcho";

    public static final String KEY_BODY = "body";

    private Connection connection;

    private java.lang.reflect.Method tryCommand;

    /**
     * Creates a new user with no connection to the world yet. The user is also not inserted
	 * into the database at this stage. Only when {@link #startLife} is called will the user
	 * be saved and attached to a world object.
     * <p>
     * The sequence is as follows:<ol>
     * <li>{@link LoginProcedure} creates the new user with this constructor,</li>
     * <li>{@link LoginProcedure} calls {@link #connect()},</li>
     * <li>{@link #connect()} saves the user and
	 * inserts a call to {@link #startLife} into the message queue,</li>
     * <li>the message queue executes {@link #startLife} where the user gets attached
	 * to a world object. </li>
     * </ol>
     *
     * @param username
     * @param password
     * @param permission
     * @param conn
     */
    public User(String username, String password, int permission, Connection conn) {
        super(username, disallowed, player, admin, admin);
        Property ph = new ValueProperty(hashPassword(password), disallowed, admin);
        addProperty(KEY_PASSHASH, ph);
        Property perm = new ValueProperty(permission, disallowed, admin);
        addProperty(KEY_PERMISSION, perm);
        Property wc = new ValueProperty(true, disallowed, player);
        addProperty(KEY_WANTSCOLOR, wc);
        Property we = new ValueProperty(false, disallowed, player);
        addProperty(KEY_WANTSECHO, we);
        Property body = new objectProperty(null, disallowed, wizard);
        addProperty(KEY_BODY, body);
        this.connection = conn;
        addBuiltinMethod("setWantsEcho", player, disallowed, admin, disallowed, getClass(), object.class, boolean.class);
        addBuiltinMethod("setWantsColor", player, disallowed, admin, disallowed, getClass(), object.class, boolean.class);
        try {
            tryCommand = getClass().getMethod("tryCommand", new String[0].getClass());
        } catch (NoSuchMethodException e) {
            String msg = String.format("Cannot find method User.tryCommand():\n%s", Util.verboseException(e));
            tell(msg);
            Log.error(msg);
        }
    }

    public object findInv(String name, int n) {
        return ((object) get(KEY_BODY)).findInv(name, n);
    }

    public object findEnv(String name, int n) {
        return ((object) get(KEY_BODY)).findEnv(name, n);
    }

    public object findEnvInv(String name, int n) {
        return ((object) get(KEY_BODY)).findEnvInv(name, n);
    }

    public object findInvEnv(String name, int n) {
        return ((object) get(KEY_BODY)).findInvEnv(name, n);
    }

    public static Collection<User> getConnectedUsers() {
        return connectedUsers.values();
    }

    public void setConnection(Connection conn) {
        this.connection = conn;
        this.connection.setWantsEcho((Boolean) get(KEY_WANTSECHO));
        this.connection.setWantsColor((Boolean) get(KEY_WANTSCOLOR));
    }

    public boolean isPasswordCorrect(String password) {
        String passhash = hashPassword(password);
        return passhash.equals((String) get(KEY_PASSHASH));
    }

    public static void setWantsEcho(object self, boolean echo) {
        User u = (User) self;
        u.connection.setWantsEcho(echo);
        u.setDirect(KEY_WANTSECHO, echo);
    }

    public static void setWantsColor(object self, boolean color) {
        User u = (User) self;
        u.connection.setWantsColor(color);
        u.setDirect(KEY_WANTSCOLOR, color);
    }

    public void tell(String msg) {
        this.connection.write(msg);
    }

    public void prompt() {
        this.connection.prompt();
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return Base64.encode(digest.digest(password.getBytes("UTF-16")));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Connect to the world.
     * @throws IOException
     */
    public void connect() throws IOException {
        boolean alreadyConnected = false;
        synchronized (User.class) {
            if (connectedUsers.get(getName()) != null) alreadyConnected = true; else connectedUsers.put(getName(), this);
        }
        if (alreadyConnected) {
            Log.warn("There was a double login for user %s.", getName());
            connection.write("A double agent, eh?\n");
            connection.close();
            return;
        } else {
            Log.info("User %s logged in.", getName());
        }
        this.connection = new AsynchronousConnection(connection, this, this);
        if (get(KEY_BODY) == null) {
            try {
                MessageQueue.systemCall(this, this, this.getClass().getMethod("startLife", null));
            } catch (NoSuchMethodException e) {
                String msg = String.format("Cannot find method User.startLife():\n%s", Util.verboseException(e));
                tell(msg);
                Log.error(msg);
            }
        }
    }

    public void startLife() {
        int previousEffective = MessageQueue.effectivePermission;
        MessageQueue.effectivePermission = admin;
        try {
            Database db = Control.getDatabase();
            set(KEY_WANTSCOLOR, connection.getWantsColor());
            set(KEY_WANTSECHO, connection.getWantsEcho());
            object root = db.findObject(0);
            object bodyProto = (object) root.get("body");
            object startRoom = (object) root.get("start");
            object playerProto = (object) root.get("player");
            object adminProto = (object) root.get("admin");
            int perm = (Integer) get(KEY_PERMISSION);
            switch(perm) {
                case player:
                    addProto(playerProto);
                    break;
                case admin:
                    addProto(adminProto);
                    break;
                default:
                    throw new RuntimeException(String.format("User %s has permission %d in startLife()!", getName(), perm));
            }
            id = db.insert(this);
            if (perm == player) perm = wizard;
            object body = new object(Util.capitalize(getName()), null, admin, player, admin, player, bodyProto);
            body.addName(getName().toLowerCase());
            setBody(body);
            Log.info("Created body for user %s.", getName());
            body.move(startRoom);
            prompt();
        } finally {
            MessageQueue.effectivePermission = previousEffective;
        }
    }

    public void setBody(object body) {
        if (body != null) {
            User oldUser = body.getUser();
            if (oldUser != null && oldUser != this) {
                throw new move_error(String.format("Cannot assign user %s to body %s, because it is already inhabited by %s.", getName(), body.toString(), oldUser.getName()));
            }
        }
        Object oldBody = get(KEY_BODY);
        set(KEY_BODY, body);
        if (body != null) {
            body.setUser(this);
        }
        if (oldBody != null) {
            ((object) oldBody).setUser(null);
        }
    }

    public void receiveMessage(String line) {
        MessageQueue.systemCall(this, this, tryCommand, (Object) Util.split(line));
    }

    public void tryCommand(String[] words) {
        tryCommand(this, words);
        prompt();
    }

    public void connectionError(Connection c, Exception e) {
        Log.info("User %s was disconnected.", getName());
        try {
            c.close();
        } catch (IOException i) {
        }
        synchronized (User.class) {
            connectedUsers.remove(getName());
        }
    }

    public int getPermission() {
        return (Integer) get(KEY_PERMISSION);
    }
}
