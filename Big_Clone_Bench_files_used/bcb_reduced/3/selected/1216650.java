package org.openconcerto.sql.ui;

import static java.util.Collections.singletonList;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.utils.Base64;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.Tuple2;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Login using USER_COMMON and log success or failure in CONNEXION.
 * 
 * @author Sylvain
 */
public class Login {

    /**
     * A reason returned by {@link #connectClear(String, String)}.
     */
    public static final String UNKNOWN_USER = "Utilisateur inconnu";

    private final DBRoot root;

    private final SQLTable userT;

    /**
     * Create a panel to log with a user/pass.
     * 
     * @param root the db root.
     */
    public Login(final DBRoot root) {
        this.root = root;
        this.userT = root.findTable("USER_COMMON");
        if (this.userT == null) {
            final SQLCreateTable ct = new SQLCreateTable(root, "USER_COMMON");
            ct.addVarCharColumn("LOGIN", 45).addVarCharColumn("PASSWORD", 45);
            ct.addVarCharColumn("NOM", 45).addVarCharColumn("PRENOM", 45);
            ct.addVarCharColumn("SURNOM", 45);
            throw new IllegalStateException("Table " + ct.getName() + " missing :\n" + ct.asString());
        }
    }

    public final SQLTable getUserTable() {
        return this.userT;
    }

    public final Tuple2<String, String> connectEnc(final String login, final String pass) {
        return this.connect(login, singletonList(pass), true);
    }

    /**
     * Try to connect.
     * 
     * @param login the login, e.g. "ilm".
     * @param pass the password in plain, e.g. "secret".
     * @return the tuple consisting of the reason the connection failed, i.e. <code>null</code> for
     *         a success ; and the encrypted form of <code>password</code>.
     */
    public final Tuple2<String, String> connectClear(final String login, final String pass) {
        return connectClear(login, Arrays.asList(pass));
    }

    final Tuple2<String, String> connectClear(final String login, final String pass, final String pass2) {
        return connectClear(login, Arrays.asList(pass, pass2));
    }

    private final Tuple2<String, String> connectClear(final String login, final List<String> pass) {
        return this.connect(login, pass, false);
    }

    private final Tuple2<String, String> connect(final String login, final List<String> passwords, final boolean encoded) {
        final Tuple2<String, String> res;
        final List<SQLRow> users = this.findUser(login);
        if (users.size() == 1) {
            final SQLRow userRow = users.get(0);
            final int size = passwords.size();
            if (size == 0) throw new IllegalArgumentException("No passwords");
            String encPass = null;
            for (int i = 0; i < size && encPass == null; i++) {
                final String pass = passwords.get(i);
                encPass = this.connect(userRow, pass, encoded);
            }
            res = Tuple2.create(encPass == null ? "Mot de passe erronné" : null, encPass);
        } else if (users.size() > 1) {
            res = Tuple2.create("Plusieurs utilisateurs nommés " + login, null);
        } else {
            assert users.size() == 0;
            res = Tuple2.create(UNKNOWN_USER, null);
        }
        try {
            logConnection(login, res.get0() == null, passwords);
        } catch (final SQLException e1) {
            e1.printStackTrace();
        }
        return res;
    }

    private final List<SQLRow> findUser(final String login) {
        final SQLSelect selUser = new SQLSelect(this.userT.getBase());
        selUser.addSelect(this.userT.getField("ID"));
        selUser.addSelect(this.userT.getField("PASSWORD"));
        final SQLName name = this.userT.getField("LOGIN").getSQLName(this.userT);
        final String req = selUser.toString() + " AND LOWER(" + name.quote() + ")=LOWER('" + login + "')";
        @SuppressWarnings("unchecked") final List<SQLRow> users = (List) this.root.getDBSystemRoot().getDataSource().execute(req, SQLRowListRSH.createFromSelect(selUser));
        return users;
    }

    private final String connect(final SQLRow userRow, final String pass, final boolean encoded) {
        if (pass == null) throw new NullPointerException("No password");
        final String encPass = encoded ? pass : encodePassword(pass);
        final String dbPass = userRow.getString("PASSWORD");
        final String res;
        if (dbPass == null || dbPass.equals(encPass)) {
            UserManager.getInstance().setCurrentUser(userRow.getID());
            res = encPass;
            assert res != null;
        } else {
            res = null;
        }
        return res;
    }

    private final void logConnection(final String login, final boolean succeeded, final List<String> passwords) throws SQLException {
        if (!this.root.contains("CONNEXION")) return;
        final SQLRowValues rowValsConnect = new SQLRowValues(this.root.getTable("CONNEXION"));
        rowValsConnect.put("LOGIN", login);
        rowValsConnect.put("DATE", new java.util.Date());
        try {
            final InetAddress Ip = InetAddress.getLocalHost();
            rowValsConnect.put("IP", Ip.getHostName() + " On " + Ip.getHostAddress());
        } catch (final UnknownHostException e1) {
            e1.printStackTrace();
        }
        rowValsConnect.put("FAILED", !succeeded);
        if (!succeeded) {
            rowValsConnect.put("MDP", CollectionUtils.join(passwords, " || "));
        }
        rowValsConnect.commit();
    }

    private static MessageDigest md;

    private static synchronized MessageDigest getMessageDigest() {
        if (md == null) try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("no SHA1", e);
        }
        return md;
    }

    public static String encodePassword(String clearPassword) {
        final byte[] s;
        synchronized (getMessageDigest()) {
            getMessageDigest().reset();
            getMessageDigest().update(clearPassword.getBytes());
            s = getMessageDigest().digest();
        }
        return Base64.encodeBytes(s);
    }
}
