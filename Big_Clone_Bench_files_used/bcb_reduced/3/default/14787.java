import java.sql.*;
import java.util.*;
import java.io.*;
import java.security.*;
import borknet_services.core.*;

/**
 * The database communication class of the Q IRC Bot.
 * @author Ozafy - ozafy@borknet.org - http://www.borknet.org
 */
public class DBControl {

    /** Database connection */
    private Connection con;

    /** Main bot */
    private Core C;

    private CoreDBControl dbc;

    private Q Bot;

    /**
	 * Constructs a Database connection.
	 * @param server		Database server
	 * @param user			Database user
	 * @param pass			Database password
	 * @param db			Database
	 * @param debug			Are we debugging?
	 * @param B				Main bot
	 */
    public DBControl(Core C, Q Bot) {
        try {
            this.C = C;
            this.Bot = Bot;
            this.dbc = C.get_dbc();
            this.con = dbc.getCon();
            PreparedStatement pstmt = con.prepareStatement("DELETE FROM q_glines WHERE oper = 'burst/other server'");
            pstmt.execute();
        } catch (Exception e) {
            C.printDebug("Database error!");
            System.exit(0);
        }
    }

    /**
	 * Check if a channel exists.
	 * @param chan		channel to check
	 *
	 * @return			true or false
	 */
    public boolean chanExists(String chan) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT name FROM q_channels WHERE name = ?");
            pstmt.setString(1, chan);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            String channel = rs.getString("name");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Check if an auth exists.
	 * @param auth		auth to check
	 *
	 * @return			true or false
	 */
    public boolean authExists(String auth) {
        return dbc.authExists(auth);
    }

    /**
	 * Check if an auth is online
	 * @param auth		auth to check
	 *
	 * @return			true or false
	 */
    public boolean authOnline(String auth) {
        return dbc.authOnline(auth);
    }

    public String getNumViaAuth(String auth) {
        return dbc.getNumViaAuth(auth);
    }

    /**
	 * Check if a nick is reserved.
	 * @param auth		nick to check
	 *
	 * @return			true or false
	 */
    public boolean isReservedNick(String auth) {
        return dbc.isReservedNick(auth);
    }

    /**
	 * Check if a nick is reserved.
	 * @param auth		nick to check
	 *
	 * @return			true or false
	 */
    public boolean isService(String numeric) {
        return dbc.isService(numeric);
    }

    /**
	 * Check if a mail is blocked.
	 * @param mail		mail to check
	 *
	 * @return			true or false
	 */
    public boolean isMailBlocked(String mail) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT mail FROM q_mails");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String bad = rs.getString("mail");
                if (mail.contains(bad)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Check if a numeric exists.
	 * @param numer		numeric to check
	 *
	 * @return			true or false
	 */
    public boolean isNumUsed(String numer) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT numer FROM q_fakeusers WHERE BINARY numer = ?");
            pstmt.setString(1, numer);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            String lev = rs.getString("numer");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Check if a numeric exists.
	 * @param numer		numeric to check
	 *
	 * @return			true or false
	 */
    public boolean isNickUsed(String nick) {
        return dbc.isNickUsed(nick);
    }

    /**
	 * Check if a snumeric exists.
	 * @param numer		numeric to check
	 *
	 * @return			true or false
	 */
    public boolean isServerNumeric(String numer) {
        return dbc.isServerNumeric(numer);
    }

    /**
	 * Check if a snumeric exists.
	 * @param numer		numeric to check
	 *
	 * @return			true or false
	 */
    public boolean isJupeNumeric(String numer) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT numer FROM q_jupes WHERE BINARY numer = ?");
            pstmt.setString(1, numer);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            String lev = rs.getString("numer");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Check if a numeric has op on a channel
	 * @param user		numeric to check
	 * @param channel	channel to check
	 *
	 * @return			true or false
	 */
    public boolean isOpChan(String user, String channel) {
        return dbc.isOpChan(user, channel);
    }

    /**
	 * Check if a numeric is on a channel
	 * @param user		numeric to check
	 * @param channel	channel to check
	 *
	 * @return			true or false
	 */
    public boolean isOnChan(String user, String channel) {
        return dbc.isOnChan(user, channel);
    }

    /**
	 * Check if a host is a known op on a channel
	 * @param host		host to check
	 * @param channel	channel to check
	 *
	 * @return			true or false
	 */
    public boolean isKnownOpChan(String host, String channel) {
        return dbc.isKnownOpChan(host, channel);
    }

    /**
	 * Check if a host has a chanfix level
	 * @param user		numeric to check
	 * @param channel	channel to check
	 *
	 * @return			true or false
	 */
    public boolean hasChanfix(String user, String channel) {
        return dbc.hasChanfix(user, channel);
    }

    /**
	 * Check if a channel has ops
	 * @param channel	channel to check
	 *
	 * @return			true or false
	 */
    public boolean chanHasOps(String channel) {
        return dbc.chanHasOps(channel);
    }

    /**
	 * Check if a channel has known ops
	 * @param channel	channel to check
	 *
	 * @return			true or false
	 */
    public boolean chanfixHasOps(String channel) {
        return dbc.chanfixHasOps(channel);
    }

    /**
	 * Check if an ip has a trust
	 * @param host		ip to check
	 *
	 * @return			true or false
	 */
    public boolean hostHasTrust(String host) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT host FROM q_trusts WHERE host = ?");
            pstmt.setString(1, host);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            String trusthost = rs.getString("host");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Check if an auth has a trust
	 * @param auth		auth to check
	 *
	 * @return			true or false
	 */
    public boolean authHasTrust(String auth) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT auth FROM q_trusts WHERE auth = ?");
            pstmt.setString(1, auth);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            String trustauth = rs.getString("auth");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Check if an ip needs an ident
	 * @param ip		ip to check
	 *
	 * @return			true or false
	 */
    public boolean hostNeedsIdent(String ip) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT need-ident FROM q_trusts WHERE host = ?");
            pstmt.setString(1, ip);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            boolean b = rs.getBoolean("need-ident");
            return b;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Get the number of users on a channel
	 * @param channel	channel to check
	 *
	 * @return			the number of users on a channel
	 */
    public int getChanUsers(String channel) {
        return dbc.getChanUsers(channel);
    }

    /**
	 * Get the number of users authed
	 * @param auth		auth to check
	 *
	 * @return			the number of users authed
	 */
    public int getAuthUsers(String auth) {
        return dbc.getAuthUsers(auth);
    }

    /**
	 * Get the number of users connected from the same host
	 * @param host		host to check
	 *
	 * @return			the number of users connected from the same host
	 */
    public int getHostCount(String host) {
        return dbc.getHostCount(host);
    }

    /**
	 * Get the number of users connected from the same host
	 * @param host		host to check
	 *
	 * @return			the number of users connected from the same host
	 */
    public int getIpCount(String ip) {
        return dbc.getIpCount(ip);
    }

    /**
	 * Get the number of allowed connections from an ip
	 * @param host		ip to check
	 *
	 * @return			the number of allowed connections from an ip
	 */
    public int getTrustCount(String host) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT users FROM q_trusts WHERE host = ?");
            pstmt.setString(1, host);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            int users = rs.getInt("users");
            rs.close();
            return users;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
	 * Get the number of users authed
	 * @param auth		auth to check
	 *
	 * @return			the number of users authed
	 */
    public String getChallenge(String user) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT challenge,time FROM q_challenge WHERE user = ?");
            pstmt.setString(1, user);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            String chall = rs.getString("challenge");
            Calendar cal = Calendar.getInstance();
            long l = (cal.getTimeInMillis() / 1000);
            if (rs.getLong("time") >= l - 60) {
                rs.close();
                return chall;
            } else {
                rs.close();
                return "0";
            }
        } catch (Exception e) {
            C.debug(e);
            return "0";
        }
    }

    /**
	 * Get a numeric's user row
	 * @param numer		numeric of the user to fetch
	 *
	 * @return			an array of all fields
	 */
    public String[] getUserRow(String numer) {
        return dbc.getUserRow(numer);
    }

    /**
	 * Get an auth's user row
	 * @param auth		auth of the user to fetch
	 *
	 * @return			an array of all fields
	 */
    public String[] getUserRowViaAuth(String auth) {
        return dbc.getUserRowViaAuth(auth);
    }

    /**
	 * Get a hosts's user row
	 * @param host		host of the user to fetch
	 *
	 * @return			an array of all fields
	 */
    public String[] getUserRowViaHost(String host) {
        return dbc.getUserRowViaHost(host);
    }

    /**
	 * Get a nick's user row
	 * @param nick		nick of the user to fetch
	 *
	 * @return			an array of all fields
	 */
    public String[] getNickRow(String nick) {
        return dbc.getNickRow(nick);
    }

    /**
	 * Get a channel's row
	 * @param channel	channel to fetch
	 *
	 * @return			an array of all fields
	 */
    public String[] getChanRow(String channel) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT * FROM q_channels WHERE name = ?");
            pstmt.setString(1, channel);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            return new String[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11) };
        } catch (Exception e) {
            return new String[] { "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0" };
        }
    }

    /**
	 * Get an auth's row
	 * @param nick		auth to fetch
	 *
	 * @return			an array of all fields
	 */
    public String[] getAuthRow(String nick) {
        return dbc.getAuthRow(nick);
    }

    /**
	 * Get an auth's access on a channel
	 * @param nick		auth to fetch
	 * @param channel	channel to fetch
	 *
	 * @return			an array of all fields
	 */
    public String[] getAccRow(String nick, String channel) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT * FROM q_access WHERE user = ? AND channel = ?");
            pstmt.setString(1, nick);
            pstmt.setString(2, channel);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            return new String[] { rs.getString(1), rs.getString(2), rs.getString(3) };
        } catch (Exception e) {
            return new String[] { "0", "0", "0", "0", "0", "0", "0", "0", "0", "0" };
        }
    }

    /**
	 * Get a user's channels
	 * @param user		user's numeric
	 *
	 * @return			an array of all channels
	 */
    public String[] getUserChans(String user) {
        return dbc.getUserChans(user);
    }

    /**
	 * Get a channel's users
	 * @param chan		channel to fetch
	 *
	 * @return			an array of all users
	 */
    public String[] getChannelUsers(String chan) {
        return dbc.getChannelUsers(chan);
    }

    /**
	 * Get a channel's bans
	 * @param channel	channel to fetch
	 *
	 * @return			an array of all bans
	 */
    public String[] getBanList(String channel) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT host FROM q_bans WHERE name = ?");
            pstmt.setString(1, channel);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString("host"));
            }
            if (a.size() > 0) {
                String[] r = (String[]) a.toArray(new String[a.size()]);
                return r;
            } else {
                return new String[] { "0", "0", "0", "0", "0", "0", "0", "0", "0", "0" };
            }
        } catch (Exception e) {
            return new String[] { "0", "0", "0", "0", "0", "0", "0", "0", "0", "0" };
        }
    }

    /**
	 * Get all registerd channels
	 * @return			an array of all registerd channels
	 */
    public String[] getChanTable() {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT name FROM q_channels");
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString("name"));
            }
            if (a.size() > 0) {
                String[] r = (String[]) a.toArray(new String[a.size()]);
                return r;
            } else {
                return new String[] { "0", "0", "0", "0", "0", "0", "0", "0", "0", "0" };
            }
        } catch (Exception e) {
            return new String[] { "0", "0", "0", "0", "0", "0", "0", "0", "0", "0" };
        }
    }

    /**
	 * Get all channels
	 * @return			an array of all channels
	 */
    public String[] getUserChanTable() {
        return dbc.getUserChanTable();
    }

    /**
	 * Get all numerics
	 * @return			an array of all numerics
	 */
    public String[] getNumericTable() {
        return dbc.getNumericTable();
    }

    /**
	 * Get all numerics
	 * @return			an array of all numerics
	 */
    public String[] getNumericTable(String server) {
        return dbc.getNumericTable(server);
    }

    /**
	 * Get all staff members
	 * @return			an array of all staff members
	 */
    public ArrayList<String> getStaffList() {
        return dbc.getStaffList();
    }

    /**
	 * Get all user rows connected to an auth
	 * @param auth		auth to fetch
	 *
	 * @return			a double array of all users
	 */
    public ArrayList<String[]> getUserRowsViaAuth(String auth) {
        return dbc.getUserRowsViaAuth(auth);
    }

    /**
	 * Get a user's full access
	 * @param user		auth to fetch
	 *
	 * @return			a double array of all access lines
	 */
    public String[][] getAccessTable(String user) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT channel,flags FROM q_access WHERE user = ?");
            pstmt.setString(1, user);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            ArrayList<String> b = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString("channel"));
                b.add(rs.getString("flags"));
            }
            String[][] r = new String[a.size()][2];
            if (a.size() > 0) {
                for (int n = 0; n < r.length; n++) {
                    r[n][0] = a.get(n);
                    r[n][1] = b.get(n);
                }
                return r;
            } else {
                return new String[][] { { "0", "0" }, { "0", "0" } };
            }
        } catch (Exception e) {
            return new String[][] { { "0", "0" }, { "0", "0" } };
        }
    }

    /**
	 * Get a common access list of two auths
	 * @param user		auth to fetch
	 * @param userinfo	auth to fetch
	 *
	 * @return			a double array of all access lines
	 */
    public String[][] getCommonAccessTable(String user, String userinfo) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT channel,flags FROM q_access WHERE user = ?");
            pstmt.setString(1, userinfo);
            ResultSet rs = pstmt.executeQuery();
            pstmt = con.prepareStatement("SELECT channel FROM q_access WHERE user = ?");
            pstmt.setString(1, user);
            ResultSet rs2 = pstmt.executeQuery();
            ArrayList<String> c = new ArrayList<String>();
            while (rs2.next()) {
                c.add(rs2.getString("channel").toLowerCase());
            }
            ArrayList<String> a = new ArrayList<String>();
            ArrayList<String> b = new ArrayList<String>();
            while (rs.next()) {
                if (c.indexOf(rs.getString("channel").toLowerCase()) != -1) {
                    a.add(rs.getString("channel"));
                    b.add(rs.getString("flags"));
                }
            }
            String[][] r = new String[a.size()][2];
            if (a.size() > 0) {
                for (int n = 0; n < r.length; n++) {
                    r[n][0] = a.get(n);
                    r[n][1] = b.get(n);
                }
                return r;
            } else {
                return new String[][] { { "0", "0" }, { "0", "0" } };
            }
        } catch (Exception e) {
            return new String[][] { { "0", "0" }, { "0", "0" } };
        }
    }

    /**
	 * Get a all users with access to a channel
	 * @param channel	channel to fetch
	 *
	 * @return			a double array of all access lines
	 */
    public String[][] getChanlev(String channel) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT user,flags FROM q_access WHERE channel = ?");
            pstmt.setString(1, channel);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            ArrayList<String> b = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString("user"));
                b.add(rs.getString("flags"));
            }
            String[][] r = new String[a.size()][2];
            if (a.size() > 0) {
                for (int n = 0; n < r.length; n++) {
                    r[n][0] = a.get(n);
                    r[n][1] = b.get(n);
                }
                return r;
            } else {
                return new String[][] { { "0", "0" }, { "0", "0" } };
            }
        } catch (Exception e) {
            return new String[][] { { "0", "0" }, { "0", "0" } };
        }
    }

    /**
	 * Get a list of all glines matching a host
	 * @param host		host to fetch
	 *
	 * @return			a double array of all glines
	 */
    public String[][] getGlist(String host) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT * FROM q_glines WHERE gline like ?");
            pstmt.setString(1, host);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            ArrayList<Integer> b = new ArrayList<Integer>();
            ArrayList<String> c = new ArrayList<String>();
            ArrayList<String> d = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString(1));
                b.add(Integer.parseInt(rs.getString(2)) + Integer.parseInt(rs.getString(3)) - Integer.parseInt(C.get_time()));
                c.add(rs.getString(5));
                d.add(rs.getString(4));
            }
            String[][] r = new String[a.size()][4];
            if (a.size() > 0) {
                for (int n = 0; n < r.length; n++) {
                    r[n][0] = a.get(n);
                    r[n][1] = b.get(n) + "";
                    r[n][2] = c.get(n);
                    r[n][3] = d.get(n);
                }
                return r;
            } else {
                return new String[][] { { "0", "0" }, { "0", "0" } };
            }
        } catch (Exception e) {
            return new String[][] { { "0", "0" }, { "0", "0" } };
        }
    }

    /**
	 * Get a list of all jupes matching a host
	 * @param host		host to fetch
	 *
	 * @return			a double array of all jupes
	 */
    public String[][] getJupelist(String host) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT * FROM q_jupes WHERE jupe like ?");
            pstmt.setString(1, host);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            ArrayList<String> b = new ArrayList<String>();
            ArrayList<Integer> c = new ArrayList<Integer>();
            ArrayList<String> d = new ArrayList<String>();
            ArrayList<String> e = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString(1));
                b.add(rs.getString(2));
                c.add(Integer.parseInt(rs.getString(3)) + Integer.parseInt(rs.getString(4)) - Integer.parseInt(C.get_time()));
                d.add(rs.getString(5));
                e.add(rs.getString(6));
            }
            String[][] r = new String[a.size()][5];
            if (a.size() > 0) {
                for (int n = 0; n < r.length; n++) {
                    r[n][0] = a.get(n);
                    r[n][1] = b.get(n);
                    r[n][2] = c.get(n) + "";
                    r[n][3] = d.get(n);
                    r[n][4] = e.get(n);
                }
                return r;
            } else {
                return new String[][] { { "0", "0" }, { "0", "0" } };
            }
        } catch (Exception e) {
            return new String[][] { { "0", "0" }, { "0", "0" } };
        }
    }

    /**
	 * Get a list of all trusts matching a host
	 * @param host		host to fetch
	 *
	 * @return			a double array of all trusts
	 */
    public String[][] getTrustList(String host) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT * FROM q_trusts WHERE host like ?");
            pstmt.setString(1, host);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            ArrayList<String> b = new ArrayList<String>();
            ArrayList<String> c = new ArrayList<String>();
            ArrayList<String> d = new ArrayList<String>();
            ArrayList<String> e = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString(1));
                b.add(rs.getString(2));
                c.add(rs.getString(3));
                d.add(rs.getString(4));
                e.add(rs.getString(5));
            }
            String[][] r = new String[a.size()][5];
            if (a.size() > 0) {
                for (int n = 0; n < r.length; n++) {
                    r[n][0] = a.get(n);
                    r[n][1] = b.get(n);
                    r[n][2] = c.get(n);
                    r[n][3] = d.get(n);
                    r[n][4] = e.get(n);
                }
                return r;
            } else {
                return new String[][] { { "0", "0" }, { "0", "0" } };
            }
        } catch (Exception e) {
            return new String[][] { { "0", "0" }, { "0", "0" } };
        }
    }

    /**
	 * Get a list of all fakeusers matching a nick
	 * @param nick		nick to fetch
	 *
	 * @return			a double array of all fakeusers
	 */
    public String[][] getFakeList(String nick) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT * FROM q_fakeusers WHERE nick like ?");
            pstmt.setString(1, nick);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            ArrayList<String> b = new ArrayList<String>();
            ArrayList<String> c = new ArrayList<String>();
            ArrayList<String> d = new ArrayList<String>();
            ArrayList<String> e = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString(1));
                b.add(rs.getString(2));
                c.add(rs.getString(3));
                d.add(rs.getString(4));
                e.add(rs.getString(5));
            }
            String[][] r = new String[a.size()][5];
            if (a.size() > 0) {
                for (int n = 0; n < r.length; n++) {
                    r[n][0] = a.get(n);
                    r[n][1] = b.get(n) + "";
                    r[n][2] = c.get(n);
                    r[n][3] = d.get(n);
                    r[n][4] = e.get(n);
                }
                return r;
            } else {
                return new String[][] { { "0", "0" }, { "0", "0" } };
            }
        } catch (Exception e) {
            return new String[][] { { "0", "0" }, { "0", "0" } };
        }
    }

    /**
	 * Get a list of all blocked e-mails matching mail
	 * @param mail		mail to fetch
	 *
	 * @return			a double array of all fakeusers
	 */
    public String[][] getMailList(String mail) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT mail FROM q_mails WHERE mail like ?");
            pstmt.setString(1, mail);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString("mail"));
            }
            String[][] r = new String[a.size()][5];
            if (a.size() > 0) {
                for (int n = 0; n < r.length; n++) {
                    r[n][0] = a.get(n);
                }
                return r;
            } else {
                return new String[][] { { "0", "0" }, { "0", "0" } };
            }
        } catch (Exception e) {
            return new String[][] { { "0", "0" }, { "0", "0" } };
        }
    }

    /**
	 * Check if a mail is blocked.
	 * @param mail		mail to check
	 *
	 * @return			true or false
	 */
    public String getInfoLine() {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT info FROM q_variables LIMIT 1");
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            return rs.getString("info");
        } catch (Exception e) {
            return "0";
        }
    }

    /**
	 * Check if a mail is blocked.
	 * @param mail		mail to check
	 *
	 * @return			true or false
	 */
    public void setInfoLine(String info) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("UPDATE q_variables SET info = ?");
            pstmt.setString(1, info);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Set a userline field to a new value
	 * @param numer		numeric of user to adapt
	 * @param colum		colum to change
	 * @param info		new info to insert
	 */
    public void setUserField(String numer, int colum, String info) {
        dbc.setUserField(numer, colum, info);
    }

    /**
	 * Set an authline field to a new value
	 * @param auth		auth of user to adapt
	 * @param colum		colum to change
	 * @param info		new info to insert
	 */
    public void setAuthField(String auth, int colum, String info) {
        dbc.setAuthField(auth, colum, info);
    }

    /**
	 * Set a chanline field to a new value
	 * @param chan		chan to adapt
	 * @param colum		colum to change
	 * @param info		new info to insert
	 */
    public void setChanField(String chan, int colum, String info) {
        try {
            String set[] = new String[] { "name", "flags", "modes", "welcome", "topic", "last", "chanlimit", "suspended", "chankey", "level", "owner" };
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("UPDATE q_channels SET " + set[colum] + " = ? WHERE name = ?");
            if (colum == 6 || colum == 9) {
                pstmt.setInt(1, Integer.parseInt(info));
            } else if (colum == 7) {
                pstmt.setBoolean(1, Boolean.parseBoolean(info));
            } else if (colum == 5) {
                pstmt.setLong(1, Long.parseLong(info));
            } else {
                pstmt.setString(1, info);
            }
            pstmt.setString(2, chan);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Set an accessline field to a new value
	 * @param auth		auth of user to adapt
	 * @param channel	channel to adapt
	 * @param access	new info to insert
	 */
    public void setAccessRow(String auth, String channel, String access) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("UPDATE q_access SET flags = ? WHERE user = ? AND channel = ?");
            pstmt.setString(1, access);
            pstmt.setString(2, auth);
            pstmt.setString(3, channel);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Move the bot from one channel to another
	 * @param oldchan		channel where the bot is
	 * @param newchan		channel where the bot will go to
	 */
    public void moveChan(String oldchan, String newchan) {
        try {
            String chan[] = getChanRow(oldchan);
            addChan(newchan, chan[1], chan[2], chan[3], chan[4], Long.parseLong(chan[5]), Integer.parseInt(chan[6]), Boolean.parseBoolean(chan[7]), chan[8], Integer.parseInt(chan[9]), chan[10]);
            String bans[] = getBanList(oldchan);
            if (!bans[0].equals("0")) {
                for (int n = 0; n < bans.length; n++) {
                    addBan(newchan, bans[n]);
                }
            }
            String acc[][] = getChanlev(oldchan);
            for (int n = 0; n < acc.length; n++) {
                addAccess(acc[n][0], newchan, acc[n][1]);
            }
            delChan(oldchan);
        } catch (Exception e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete a channel
	 * @param channel		channel to delete
	 */
    public void delChan(String channel) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("DELETE FROM q_channels WHERE name = ? LIMIT 1");
            pstmt.setString(1, channel);
            pstmt.executeUpdate();
            pstmt = con.prepareStatement("DELETE FROM q_access WHERE channel = ?");
            pstmt.setString(1, channel);
            pstmt.executeUpdate();
            pstmt = con.prepareStatement("DELETE FROM q_bans WHERE name = ?");
            pstmt.setString(1, channel);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete an auth
	 * @param auth		auth to delete
	 */
    public void delAuth(String auth) {
        try {
            dbc.delAuth(auth);
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("DELETE FROM q_access WHERE user = ?");
            pstmt.setString(1, auth);
            pstmt.executeUpdate();
            pstmt = con.prepareStatement("DELETE FROM q_pwrequest WHERE user = ?");
            pstmt.setString(1, auth);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete a ban
	 * @param channel		channel where the ban needs removal
	 * @param nr			id of the ban to delete
	 */
    public void delBan(String channel, int nr) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT host FROM q_bans WHERE name = ? LIMIT " + nr + ",1");
            pstmt.setString(1, channel);
            ResultSet rs = pstmt.executeQuery();
            String host = "";
            while (rs.next()) {
                C.cmd_mode_me(Bot.get_num(), Bot.get_corenum(), rs.getString("host"), channel, "-b");
                host = rs.getString("host");
            }
            pstmt = con.prepareStatement("DELETE FROM q_bans WHERE name = ? AND host = ?");
            pstmt.setString(1, channel);
            pstmt.setString(2, host);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete an accessrow
	 * @param auth		auth of user to delete
	 * @param channel	channel where access should be removed
	 */
    public void delAccessRow(String auth, String channel) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("DELETE FROM q_access WHERE user = ? AND channel = ?");
            pstmt.setString(1, auth);
            pstmt.setString(2, channel);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete a gline
	 * @param host		host to be removed
	 */
    public void delGline(String host) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT gline FROM q_glines WHERE gline like ?");
            pstmt.setString(1, host);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                C.cmd_ungline(Bot.get_num(), rs.getString("gline"));
            }
            pstmt = con.prepareStatement("DELETE FROM q_glines WHERE gline like ?");
            pstmt.setString(1, host);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete a gline
	 * @param host		host to be removed
	 */
    public void delJupe(String host, String numer) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT jupe,timeset FROM q_jupes WHERE jupe like ?");
            pstmt.setString(1, host);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                C.cmd_unjupe(numer, rs.getString("jupe"), rs.getString("timeset"));
            }
            pstmt = con.prepareStatement("DELETE FROM q_jupes WHERE jupe like ?");
            pstmt.setString(1, host);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete a trust
	 * @param host		host to be removed
	 */
    public void delTrust(String host) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("DELETE FROM q_trusts WHERE host like ?");
            pstmt.setString(1, host);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete a fakeuser
	 * @param numer		numeric of the fakeuser to be removed
	 */
    public void delFakeUser(String numer) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("DELETE FROM q_fakeusers WHERE BINARY numer = ? ");
            pstmt.setString(1, numer);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete a mail
	 * @param mail		mail to be removed
	 */
    public void delMail(String mail) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("DELETE FROM q_mails WHERE mail like ? ");
            pstmt.setString(1, mail);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
	 * Delete an auth
	 * @param auth		auth to delete
	 */
    public void delChallenge(String user) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("DELETE FROM q_challenge WHERE user = ?");
            pstmt.setString(1, user);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addFakeUser(String nume, String nick, String ident, String host, String desc) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("INSERT INTO q_fakeusers VALUES (?,?,?,?,?)");
            pstmt.setString(1, nume);
            pstmt.setString(2, nick);
            pstmt.setString(3, ident);
            pstmt.setString(4, host);
            pstmt.setString(5, desc);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addMail(String mail) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("INSERT INTO q_mails VALUES (?)");
            pstmt.setString(1, mail);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addAuth(String auth, String pass, String mail1, int lev, boolean suspended, Long time, String info, String userflags, String vhost) {
        dbc.addAuth(auth, pass, mail1, lev, suspended, time, info, userflags, vhost);
    }

    public void addChan(String channel, String flags, String modes, String welcome, String topic, Long time, int limit, boolean suspended, String key, int level, String owner) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("INSERT INTO q_channels VALUES (?,?,?,?,?,?,?,?,?,?,?)");
            pstmt.setString(1, channel);
            pstmt.setString(2, flags);
            pstmt.setString(3, modes);
            pstmt.setString(4, welcome);
            pstmt.setString(5, topic);
            pstmt.setLong(6, time);
            pstmt.setInt(7, limit);
            pstmt.setBoolean(8, suspended);
            pstmt.setString(9, key);
            pstmt.setInt(10, level);
            pstmt.setString(11, owner);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addAccess(String user, String channel, String flags) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("INSERT INTO q_access VALUES (?,?,?)");
            pstmt.setString(1, user);
            pstmt.setString(2, channel);
            pstmt.setString(3, flags);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addPwRequest(String user, String pass, String code) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("DELETE FROM q_pwrequest WHERE user = ? ");
            pstmt.setString(1, user);
            pstmt.executeUpdate();
            pstmt = con.prepareStatement("INSERT INTO q_pwrequest VALUES (?,?,?)");
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            pstmt.setString(3, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public String getPwRequest(String user, String code) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT pass FROM q_pwrequest WHERE user = ? AND code = ?");
            pstmt.setString(1, user);
            pstmt.setString(2, code);
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            String pass = rs.getString("pass");
            pstmt = con.prepareStatement("DELETE FROM q_pwrequest WHERE user = ? ");
            pstmt.setString(1, user);
            pstmt.executeUpdate();
            return pass;
        } catch (SQLException e) {
            return "0";
        }
    }

    public void addBan(String channel, String ban) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("INSERT INTO q_bans VALUES (?,?)");
            pstmt.setString(1, channel);
            pstmt.setString(2, ban);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addGline(String host, String timeset, String timeexp, String reason, String oper) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("INSERT IGNORE INTO q_glines VALUES (?,?,?,?,?)");
            pstmt.setString(1, host);
            pstmt.setString(2, timeset);
            pstmt.setString(3, timeexp);
            pstmt.setString(4, reason);
            pstmt.setString(5, oper);
            pstmt.executeUpdate();
            if (!oper.equals("burst/other server")) {
                C.cmd_gline(Bot.get_num(), host, timeexp, reason);
            }
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addJupe(String host, String numeric, Long timeset, Long timeexp, String reason, String oper, String nume) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("INSERT INTO q_jupes VALUES (?,?,?,?,?,?)");
            pstmt.setString(1, host);
            pstmt.setString(2, numeric);
            pstmt.setLong(3, timeset);
            pstmt.setLong(4, timeexp);
            pstmt.setString(5, reason);
            pstmt.setString(6, oper);
            pstmt.executeUpdate();
            C.cmd_jupe(nume, host, numeric);
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addTrust(String host, int users, String auth, Long time, boolean ident) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("INSERT IGNORE INTO q_trusts VALUES (?,?,?,?,?)");
            pstmt.setString(1, host);
            pstmt.setInt(2, users);
            pstmt.setString(3, auth);
            pstmt.setLong(4, time);
            pstmt.setBoolean(5, ident);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void addChallenge(String user, String challenge, Long time) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("DELETE FROM q_challenge WHERE user = ? ");
            pstmt.setString(1, user);
            pstmt.executeUpdate();
            pstmt = con.prepareStatement("INSERT INTO q_challenge VALUES (?,?,?)");
            pstmt.setString(1, user);
            pstmt.setString(2, challenge);
            pstmt.setLong(3, time);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void unBanAll(String channel) {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT host FROM q_bans WHERE name = ?");
            pstmt.setString(1, channel);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                C.cmd_mode_me(Bot.get_num(), Bot.get_corenum(), rs.getString("host"), channel, "-b");
            }
            pstmt = con.prepareStatement("DELETE FROM q_bans WHERE name = ?");
            pstmt.setString(1, channel);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void clean() {
    }

    public String encrypt(String plaintext) {
        byte[] defaultBytes = plaintext.getBytes();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error encrypting password.");
            e.printStackTrace();
            System.exit(0);
            return "0";
        }
    }
}
