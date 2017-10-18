import java.sql.*;
import java.security.*;

public class Users {

    private Log log = new Log();

    public Users() {
    }

    public int createUser(String nuser, String passwd, long phone, String first, String last, String birth, String email, Statement state) {
        try {
            int uid;
            byte one = checks(nuser, email, state);
            System.out.println("Password: " + passwd);
            System.out.println("one = " + one);
            log.appendLog("Code = " + one);
            userError(one);
            if (one == 1) {
                MessageDigest sha = MessageDigest.getInstance("sha1");
                byte[] bytepasswd = passwd.getBytes();
                sha.update(bytepasswd);
                byte[] encpass = sha.digest();
                String hash = Base64.encodeBytes(encpass);
                log.appendLog("INSERT INTO rides.authentication (username, password, phone, first, last, birth, email) VALUES ('" + nuser + "', '" + hash + "'," + phone + ", '" + first + "', '" + last + "', '" + birth + "', '" + email + "');");
                state.executeUpdate("INSERT INTO rides.authentication (username, password, phone, first, last, birth, email) VALUES ('" + nuser + "', '" + hash + "'," + phone + ", '" + first + "', '" + last + "', '" + birth + "', '" + email + "');");
                log.appendLog("SELECT uid FROM rides.authentication where username='" + nuser + "';");
                ResultSet userid = state.executeQuery("SELECT uid FROM rides.authentication where username='" + nuser + "';");
                userid.next();
                uid = userid.getInt("uid");
                log.appendLog("UID = " + uid);
                return uid;
            }
            return one;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            log.appendLog("Error -1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int authUser(String username, String password, Statement state) throws NoSuchAlgorithmException, Exception {
        int user = checksUser(username, password, state);
        log.appendLog("Login of user " + username + " returned " + user);
        if (user == 2) return -1;
        if (user == -2) return -2;
        if (user > 0) return 1; else return -3;
    }

    public int alterUser(int uid, String nuser, String passwd, long phone, String first, String last, String birth, String email, Statement state) {
        try {
            int one = checksUser(nuser, email, state);
            System.out.println("Password: " + passwd);
            System.out.println("one = " + one);
            log.appendLog("Code = " + one);
            userError(one);
            if (one == 1) {
                MessageDigest sha = MessageDigest.getInstance("sha1");
                byte[] bytepasswd = passwd.getBytes();
                sha.update(bytepasswd);
                byte[] encpass = sha.digest();
                String hash = Base64.encodeBytes(encpass);
                log.appendLog("INSERT INTO rides.authentication (username, password, phone, first, last, birth, email) VALUES ('" + nuser + "', '" + hash + "'," + phone + ", '" + first + "', '" + last + "', '" + birth + "', '" + email + "');");
                state.executeUpdate("INSERT INTO rides.authentication (username, password, phone, first, last, birth, email) VALUES ('" + nuser + "', '" + hash + "'," + phone + ", '" + first + "', '" + last + "', '" + birth + "', '" + email + "');");
                log.appendLog("SELECT uid FROM rides.authentication where username='" + nuser + "';");
                ResultSet userid = state.executeQuery("SELECT uid FROM rides.authentication where username='" + nuser + "';");
                userid.next();
                uid = userid.getInt("uid");
                log.appendLog("UID = " + uid);
                return uid;
            }
            return one;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            log.appendLog("Error -1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public byte checks(String nuser, String email, Statement state) {
        byte user = checkUserExist(nuser, state);
        byte eemail = checkEmailExist(email, state);
        byte emily_rocks = (byte) (user * eemail);
        if ((user == 1) && (eemail == 1)) return 1; else return emily_rocks;
    }

    public int checksUser(String nuser, String password, Statement state) throws NoSuchAlgorithmException {
        int pass = -2;
        byte user = checkUserExist(nuser, state);
        if (user == -1) pass = checkPasswordExists(nuser, password, state);
        int emily_rocks = (user * pass);
        if ((user == -1) && (pass != -2)) return pass; else return emily_rocks;
    }

    public byte checkUserExist(String nuser, Statement state) {
        try {
            ResultSet set = state.executeQuery("SELECT * FROM rides.authentication where username='" + nuser + "';");
            log.appendLog("Checking for preexisting user: " + nuser);
            set.next();
            int setrow = set.getRow();
            System.out.println("set Row = " + setrow);
            if (setrow > 0) System.out.println("Output: " + set.getString(2));
            if (setrow == 0) return 1;
            if (setrow > 0) return -1; else return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -3;
    }

    public byte checkEmailExist(String email, Statement state) {
        try {
            ResultSet eset = state.executeQuery("SELECT * FROM rides.authentication where email='" + email + "';");
            log.appendLog("Checking for preexisting email address: " + email);
            eset.next();
            System.out.println("eset Row = " + eset.getRow());
            if (eset.getRow() > 0) System.out.println("Output: " + eset.getString(1));
            if (eset.getRow() > 0) return -5; else return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -7;
    }

    public int checkPasswordExists(String username, String password, Statement state) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("sha1");
        byte[] pass = password.getBytes();
        try {
            ResultSet passwd = state.executeQuery("SELECT password,uid FROM rides.authentication where username='" + username + "';");
            passwd.next();
            String encpasswd = passwd.getString("password");
            log.appendLog("Found password: " + encpasswd);
            sha.update(pass);
            byte[] encpass = sha.digest();
            String hash = Base64.encodeBytes(encpass);
            log.appendLog("Compares to '" + hash + "'?");
            System.out.println(" Compare two passwords: (" + hash + ") (" + encpasswd + ")");
            if (encpasswd.equals(hash)) {
                int uid = passwd.getInt("uid");
                log.appendLog("Password MATCH. return uid " + uid);
                return uid;
            }
            log.appendLog("Passwords did NOT match. Return -2");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -2;
    }

    public void userError(int one) {
        String output;
        switch(one) {
            case -5:
                output = "Please use a different email address. The one provided is already in use.";
                System.out.println(output);
                break;
            case -7:
                output = "An unknown error occurred. Please use a different email address. The one provided is already in use.";
                System.out.println(output);
                break;
            case -1:
                output = "Please select a different username. The one provided is already in use.";
                System.out.println(output);
                break;
            case 5:
                output = "Please use a different email address and select a different username. The ones provided are already in use.";
                System.out.println(output);
                break;
            case 7:
                output = "An unknown error occurred. Please use a different email address and select a different username. The username is already in use.";
                System.out.println(output);
                break;
            case -3:
                output = "An unknown error occurred. Please select a different username.";
                System.out.println(output);
                break;
            case 15:
                output = "An unknown error occurred. Please select a different username and provide a different email address. The email address provided is already in use.";
                System.out.println(output);
                break;
            case 27:
                output = "An unknown error occurred. Please select a different username and provide a different email address.";
                System.out.println(output);
                break;
            default:
                output = "ALL GOOOOOOD! =)";
                System.out.println(output);
                break;
        }
        try {
            log.appendLog(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int privileges(int uid, int rideid, Statement state) {
        try {
            ResultSet query = state.executeQuery("SELECT * FROM rides.subrides where uid=" + uid + ", rideid=" + rideid + ";");
            query.next();
            if (query.getRow() > 0) {
                return 1;
            } else return -1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int submitRide(int uid, Statement state, String driver, String riders, String music, boolean food, String vehicle, int seats, int cost, String date, String time, String oc, String os, String dc, String ds, String sc, String ss) {
        char acceptfood;
        if (food) acceptfood = 't'; else acceptfood = 'f';
        try {
            ResultSet query = state.executeQuery("SELECT uid,username FROM rides.authentication where uid=" + uid + ";");
            query.next();
            if (query.getRow() == 0) {
                log.appendLog("uid " + uid + " does not exist. Can not submit request");
                return -1;
            } else {
                state.executeUpdate("INSERT INTO rides.subrides (uid, driver, riders, music, food, vehicle, seats, cost, date, time, origincity, originstate, destcity, deststate, stopscity, stopsstate) VALUES (" + uid + ", '" + driver + "', '" + riders + "', '" + music + "', '" + acceptfood + "', '" + vehicle + "', " + seats + ", " + cost + ", '" + date + "', '" + time + "', '" + oc + "','" + os + "', '" + dc + "', '" + ds + "', '" + sc + "', '" + ss + "');");
                query = state.executeQuery("SELECT rideid FROM rides.subrides where uid=" + uid + ";");
                query.last();
                int rideid = query.getInt("rideid");
                log.appendLog("Ride submitted for uid=" + uid + " with rideid=" + rideid);
                return rideid;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int submitRequest(int uid, Statement state, String driver, String riders, String music, boolean food, String vehicle, int seats, int cost, String date, String time, String oc, String os, String dc, String ds, String sc, String ss) {
        char acceptfood;
        String listofriders;
        int riderusername;
        if (food) acceptfood = 't'; else acceptfood = 'f';
        riderusername = findRiders(riders, state);
        if (riderusername == -1) return -1; else try {
            log.appendLog("On to stage Two.");
            ResultSet query = state.executeQuery("SELECT uid,username FROM rides.authentication where uid=" + uid + ";");
            query.next();
            if (query.getRow() == 0) {
                log.appendLog("uid " + uid + " does not exist. Can not submit request");
                return -1;
            } else {
                String username = query.getString("username");
                if (riders.contains("none")) listofriders = username; else listofriders = username + ", " + riders;
                log.appendLog("Submitting request for username " + riderusername + "...");
                state.executeUpdate("INSERT INTO rides.reqrides (uid, driver, riders, music, food, vehicle, seats, cost, date, time, origincity, originstate, destcity, deststate, stopscity, stopsstate) VALUES (" + uid + ", '" + driver + "', '" + listofriders + "', '" + music + "', '" + acceptfood + "', '" + vehicle + "', " + seats + ", " + cost + ", '" + date + "', '" + time + "', '" + oc + "','" + os + "', '" + dc + "', '" + ds + "', '" + sc + "', '" + ss + "');");
                query = state.executeQuery("SELECT rideid FROM rides.reqrides where uid=" + uid + ";");
                query.last();
                int rideid = query.getInt("rideid");
                log.appendLog("Ride Request submitted for uid=" + uid + " with rideid=" + rideid);
                return rideid;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void deleteRide(int uid, int rideid, Statement state) {
        try {
            ResultSet query = state.executeQuery("SELECT rideid,uid FROM rides.subrides WHERE rideid=" + rideid + " AND uid=" + uid + ";");
            System.out.println("SELECT rideid,uid FROM rides.subrides WHERE rideid=" + rideid + " AND uid=" + uid + ";");
            query.next();
            System.out.println("query: " + query.getRow());
            if (query.getRow() == 0) log.appendLog("No Listing found for user " + uid + " for ride " + rideid); else {
                if (query.getInt("rideid") > 0) {
                    state.executeUpdate("DELETE FROM rides.subrides WHERE uid=" + uid + " AND rideid =" + rideid + ";");
                    log.appendLog("Deleting Listing rideid=" + rideid + " for user=" + uid);
                } else log.appendLog("No Listing found for user " + uid + " for ride " + rideid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteRequest(int uid, int rideid, Statement state) {
        try {
            ResultSet query = state.executeQuery("SELECT rideid,uid FROM rides.reqrides WHERE rideid=" + rideid + " AND uid=" + uid + ";");
            System.out.println("SELECT rideid,uid FROM rides.reqrides WHERE rideid=" + rideid + " AND uid=" + uid + ";");
            query.next();
            System.out.println("query: " + query.getRow());
            if (query.getRow() == 0) log.appendLog("No Request Listing found for user " + uid + " for ride " + rideid); else {
                if (query.getInt("rideid") > 0) {
                    state.executeUpdate("DELETE FROM rides.reqrides WHERE uid=" + uid + " AND rideid =" + rideid + ";");
                    log.appendLog("Deleting Request Listing rideid=" + rideid + " for user=" + uid);
                } else log.appendLog("No Request Listing found for user " + uid + " for ride " + rideid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int findRiders(String riders, Statement state) {
        String rider;
        int oldcomma;
        System.out.println("riders = " + riders);
        int length = riders.length() - 1;
        int comma = riders.indexOf(",");
        System.out.println("one: length = " + length + " comma = " + comma);
        oldcomma = 0;
        while (comma != -1) {
            rider = riders.substring(oldcomma, comma);
            System.out.println("two: length = " + length + " comma = " + comma);
            System.out.println("rider = " + rider);
            int exists = checkRider(state, rider);
            if (exists == -1) return -1;
            oldcomma = comma + 2;
            comma = riders.indexOf(",", comma + 1);
        }
        rider = riders.substring(oldcomma, length + 1);
        System.out.println("rider = " + rider);
        System.out.println("four: length = " + length + " comma = " + comma);
        int exists = checkRider(state, rider);
        if (exists == -1) return -1;
        return 1;
    }

    public int checkRider(Statement state, String rider) {
        try {
            ResultSet query = state.executeQuery("SELECT uid,username FROM rides.authentication where username='" + rider + "';");
            query.next();
            if (query.getRow() == 0) {
                log.appendLog("username " + rider + " does not exist. Can not submit request");
                return -1;
            }
            log.appendLog("username " + rider + " appears to exist. Continuing with the loop..... " + " " + rider.length() + " characters long");
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int logIn(String nuser, String passwd, Statement state) {
        int code = -1;
        try {
            log.appendLog("**********LOGIN**********");
            log.appendLog("Login User: " + nuser);
            code = authUser(nuser, passwd, state);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Login Code: " + code);
        return code;
    }
}
