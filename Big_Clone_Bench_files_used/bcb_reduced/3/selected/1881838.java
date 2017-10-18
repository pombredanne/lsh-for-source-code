package net.teamclerks.kain.irc.bot.helperbot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.teamclerks.kain.irc.bot.utils.tags.TagHandler;
import net.teamclerks.kain.utils.Logger;
import net.teamclerks.kain.utils.NoSuchURLException;
import net.teamclerks.kain.utils.URLUtils;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;

public class Helper extends PircBot {

    public static final URL PIRATE_BAY = getBayImgURL();

    public static final URL PIRATE_BAY_CLOUD = getBayImgCloudURL();

    public static final File LOG_DIR = new File("logs");

    public static final File CONFIG = new File("config");

    public static final File VERSION_NOTES = new File("version-notes");

    public static final double BRACKET_2V2_MULTIPLICAN = 0.76d;

    public static final double BRACKET_3V3_MULTIPLICAN = 0.88d;

    public static final double BRACKET_5V5_MULTIPLICAN = 1.0d;

    public static final String ADMIN_LOGIN_KEY = "admin-login";

    public static final String ADMIN_HOST_KEY = "admin-host";

    public static final String DB_CONNECT_KEY = "db-connect-string";

    public static final String SERVER_KEY = "server";

    public static final String CHANNEL_KEY = "channel";

    public static final String NUMBER_OF_RESULTS_KEY = "number-of-results";

    public static final String BOT_NAME_KEY = "bot-name";

    public static final String USE_TOR_PROXY_KEY = "use-tor-proxy";

    public static final String TOR_PROXY_HOST_KEY = "tor-proxy-host";

    public static final String TOR_PROXY_PORT_KEY = "tor-proxy-port";

    public static final String MESSAGE_REPONSE_TIME_KEY = "message-response-time";

    public static final String CMD_HELP = "help";

    public static final String CMD_SAY = "say";

    public static final String CMD_HIDE = "hide";

    public static final String CMD_JOIN = "join";

    public static final String CMD_DIE = "die";

    public static final String CMD_PING = "ping";

    public static final String CMD_2V2 = "2v2";

    public static final String CMD_3V3 = "3v3";

    public static final String CMD_5V5 = "5v5";

    public static final String CMD_TIME = "time";

    public static final String CMD_ROLL = "roll";

    public static final String CMD_FLIP = "flip";

    public static final String CMD_SIXES = "sixes";

    public static final String CMD_SHA1 = "sha1";

    public static final String CMD_MD5 = "md5";

    public static final String CMD_SEARCH = "search";

    public static final String CMD_FULL_SEARCH = "fullsearch";

    public static final String CMD_TAGS_FOR = "tagsfor";

    public static final String CMD_COUNT = "count";

    public static final String CMD_VERSION = "version";

    public static final String CMD_VERSION_NOTES = "versionnotes";

    public static final String CMD_RANDOM = "random";

    public static final String CMD_QQ = "qq";

    public static final String CMD_EMOTE = "emote";

    public static final String CMD_PLAY = "play";

    public static final String CMD_UPTIME = "uptime";

    public static final String CMD_REVERSE = "reverse";

    private ArrayList<String> adminHostnames;

    private String dbConnectString;

    private String server;

    private String channel;

    private ArrayList<String> rollers;

    private TagHandler taghandler;

    private Map<String, ArrayList<String>> versionNotes;

    private String lastMessage;

    private Random random;

    private Logger logger;

    private int numberOfResults;

    private String version;

    private String botName;

    private boolean useTorProxy;

    private Proxy torProxy;

    private String torHost;

    private int torPort;

    private Calendar startDate;

    private int messageDelayTime;

    /**
   * Basic Constructor
   */
    public Helper() {
        this.useTorProxy = false;
        torProxy = Proxy.NO_PROXY;
        this.adminHostnames = new ArrayList<String>();
        this.configure();
        if (useTorProxy) {
            torProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(torHost, torPort));
        }
        this.log(this.botName + " configured correctly; initializing...");
        this.startDate = Calendar.getInstance();
        this.setName(this.botName);
        this.setVerbose(true);
        this.rollers = new ArrayList<String>();
        this.taghandler = new TagHandler(this.dbConnectString);
        this.versionNotes = new HashMap<String, ArrayList<String>>();
        this.lastMessage = "";
        this.setupVersionNotes();
        this.random = new Random(System.currentTimeMillis());
        this.setMessageDelay(this.messageDelayTime);
    }

    /**
   * Simple HelperBot runner.
   * @param args
   */
    public static void main(String[] args) {
        new Helper().connectNow();
    }

    /**
   * Connector, uses the local server and channel info to connect.
   */
    public void connectNow() {
        try {
            this.connect(this.server);
        } catch (IrcException irce) {
            System.out.println("IrcException thrown when trying to connect to server: " + this.server);
            irce.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("IOException thrown when trying to connect to server: " + this.server);
            ioe.printStackTrace();
        }
        this.joinChannel(this.channel);
    }

    @Override
    public void onNotice(String channel, String sender, String login, String hostname, String message) {
        message = message.replaceAll("[\\s]+", " ");
        ArrayList<String> pm = new ArrayList<String>(Arrays.asList(message.split(" ")));
        for (int i = 0; i < pm.size(); i++) {
            if (pm.get(i).length() == 0) {
                pm.remove(i);
            }
        }
        this.findURLs(sender, login, hostname, pm);
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        message = message.replaceAll("[\\s]+", " ");
        ArrayList<String> pm = new ArrayList<String>(Arrays.asList(message.split(" ")));
        for (int i = 0; i < pm.size(); i++) {
            if (pm.get(i).length() == 0) {
                pm.remove(i);
            }
        }
        if (this.getName().equalsIgnoreCase(pm.get(0).replaceAll("[^A-Za-z0-9]", ""))) {
            pm.remove(0);
            if (pm.size() > 0) {
                log("Received public message: " + sender + ":" + login + ":" + hostname + ":" + message);
                handlePublicCommand(pm, sender, login, hostname);
            }
        } else {
            handleAmbiguousCommand(pm, sender, login, hostname);
        }
        this.findURLs(sender, login, hostname, pm);
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message) {
        log("Received private message: " + sender + ":" + login + ":" + hostname + ":" + message);
        message = message.replaceAll("[\\s]+", " ");
        ArrayList<String> pm = new ArrayList<String>(Arrays.asList(message.split(" ")));
        for (int i = 0; i < pm.size(); i++) {
            if (pm.get(i).length() == 0) {
                pm.remove(i);
            }
        }
        if (this.isAdmin(hostname) && pm.size() > 0) handleAdminCommand(pm, sender, login, hostname); else if (pm.size() > 0) handleCommand(pm, sender, login, hostname);
        this.findURLs(sender, login, hostname, pm);
    }

    @Override
    public void onServerPing(String response) {
        super.onServerPing(response);
    }

    @Override
    public void onDisconnect() {
        while (!this.isConnected()) {
            try {
                this.reconnect();
                this.joinChannel(channel);
            } catch (NickAlreadyInUseException naiue) {
                try {
                    wait(60000);
                } catch (InterruptedException iee) {
                    this.log("Nick in use, trying again in 1 minute.");
                }
            } catch (IrcException ircfe) {
                log("IRC Exception thrown: ");
                ircfe.printStackTrace();
                break;
            } catch (IOException ioe) {
                log("IO Exception thrown: ");
                ioe.printStackTrace();
                break;
            }
        }
    }

    @Override
    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equalsIgnoreCase(this.botName)) {
            this.joinChannel(channel);
            this.sendNotice(this.channel, "Well, fuck you too, " + kickerNick + ", why did you kick me!?");
        }
    }

    @Override
    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
        if (sourceNick.equalsIgnoreCase(this.botName)) {
            this.disconnect();
            this.connectNow();
        }
    }

    @Override
    public void log(String message) {
        if (!LOG_DIR.exists()) {
            LOG_DIR.mkdir();
        }
        if (this.logger == null) {
            this.logger = new Logger();
        }
        try {
            File logfile = new File(LOG_DIR + "/" + Calendar.getInstance().get(Calendar.YEAR) + "-" + (Calendar.getInstance().get(Calendar.MONTH) + 1) + "-" + Calendar.getInstance().get(Calendar.DATE) + ".txt");
            if (!logfile.exists()) {
                logfile.createNewFile();
                this.logger.setOutputStream(logfile);
            }
            if (!this.logger.canWrite()) {
                this.logger.setOutputStream(logfile);
            }
            this.logger.log(message);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
   * Sets up the Pirate Bay image hosting URL.
   * @return URL - the Pirate Bay image pages.
   */
    private static URL getBayImgURL() {
        try {
            return new URL("http://bayimg.com/ajax_tags.php");
        } catch (MalformedURLException e) {
        }
        return null;
    }

    /**
   * Sets up the Pirate Bay tag-cloud URL.
   * @return URL - the Pirate Bay tag-cloud.
   */
    private static URL getBayImgCloudURL() {
        try {
            return new URL("http://bayimg.com/ajax_tagcloud.php");
        } catch (MalformedURLException e) {
        }
        return null;
    }

    /**
   * Simple configuration method. Will read CONFIG and parse out
   * data points pertaining to HelperBot.
   */
    private void configure() {
        if (!CONFIG.exists()) {
            System.err.println("The required configuration file \"config\" could not be found.");
            System.exit(0);
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(CONFIG));
            String line = reader.readLine();
            while (line != null) {
                line = line.replaceAll(" ", "");
                String[] args = line.split("=");
                if (args[0].equalsIgnoreCase(ADMIN_HOST_KEY)) {
                    this.adminHostnames.add(args[1]);
                } else if (args[0].equalsIgnoreCase(DB_CONNECT_KEY)) {
                    this.dbConnectString = args[1];
                } else if (args[0].equalsIgnoreCase(SERVER_KEY)) {
                    this.server = args[1];
                } else if (args[0].equalsIgnoreCase(CHANNEL_KEY)) {
                    this.channel = args[1];
                } else if (args[0].equalsIgnoreCase(NUMBER_OF_RESULTS_KEY)) {
                    try {
                        this.numberOfResults = new Integer(args[1]).intValue();
                    } catch (NumberFormatException nfe) {
                        this.numberOfResults = -1;
                    }
                } else if (args[0].equalsIgnoreCase(BOT_NAME_KEY)) {
                    this.botName = args[1];
                } else if (args[0].equalsIgnoreCase(USE_TOR_PROXY_KEY)) {
                    this.useTorProxy = new Boolean(args[1]);
                } else if (args[0].equalsIgnoreCase(TOR_PROXY_HOST_KEY)) {
                    this.torHost = args[1];
                } else if (args[0].equalsIgnoreCase(TOR_PROXY_PORT_KEY)) {
                    try {
                        this.torPort = new Integer(args[1]).intValue();
                    } catch (NumberFormatException nfe) {
                        this.torPort = -1;
                    }
                } else if (args[0].equalsIgnoreCase(MESSAGE_REPONSE_TIME_KEY)) {
                    try {
                        this.messageDelayTime = new Integer(args[1]).intValue();
                    } catch (NumberFormatException nfe) {
                        this.messageDelayTime = -1;
                    }
                }
                line = reader.readLine();
            }
            if (this.adminHostnames == null || this.dbConnectString == null || this.server == null || this.channel == null || this.numberOfResults == -1) {
                System.err.println("Your config file is malformed:");
                if (this.adminHostnames == null) {
                    System.err.println("\"" + ADMIN_HOST_KEY + "\" malformed or missing.");
                }
                if (this.dbConnectString == null) {
                    System.err.println("\"" + DB_CONNECT_KEY + "\" malformed or missing.");
                }
                if (this.server == null) {
                    System.err.println("\"" + SERVER_KEY + "\" malformed or missing.");
                }
                if (this.channel == null) {
                    System.err.println("\"" + CHANNEL_KEY + "\" malformed or missing.");
                }
                if (this.numberOfResults == -1) {
                    System.err.println("\"" + NUMBER_OF_RESULTS_KEY + "\" malformed or missing.");
                }
                if (this.useTorProxy && (this.torHost == null || this.torHost.equals(""))) {
                    System.err.println("\"" + TOR_PROXY_HOST_KEY + "\" malformed or missing.");
                }
                if (this.useTorProxy && this.torPort == -1) {
                    System.err.println("\"" + TOR_PROXY_PORT_KEY + "\" malformed or missing.");
                }
                if (this.messageDelayTime == -1) {
                    System.err.println("\"" + MESSAGE_REPONSE_TIME_KEY + "\" malformed or missing.");
                }
                System.exit(0);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
   * This method handles the administration functions. If an admin sends a message, this method
   * will control what happens. 
   * 
   * These functions should be considered secure.
   * 
   * If a command is issued that is not recognized by this method, it will be passed down the
   * chain of command. I.e. handleCommand.
   * 
   * @param pm
   * @param sender
   * @param login
   * @param hostname
   */
    private void handleAdminCommand(ArrayList<String> pm, String sender, String login, String hostname) {
        if (pm.size() == 0) return;
        if (pm.get(0).equalsIgnoreCase(CMD_SAY)) {
            String toBeSaid = "";
            for (String s : pm) {
                if (s != pm.get(0)) {
                    toBeSaid += s + " ";
                }
            }
            log("About to send message to channel: " + this.channel);
            this.sendMessage(this.channel, toBeSaid);
        } else if (pm.get(0).equalsIgnoreCase(CMD_EMOTE)) {
            String toBe = "";
            for (String s : pm) {
                if (s != pm.get(0)) {
                    toBe += s + " ";
                }
            }
            this.sendAction(this.channel, toBe);
        } else if (pm.get(0).equalsIgnoreCase(CMD_HIDE)) {
            log("Parting channel: " + channel);
            this.partChannel(channel);
        } else if (pm.get(0).equalsIgnoreCase(CMD_JOIN)) {
            if (pm.get(1) != null) {
                log("Attempting to join channel: " + pm.get(1));
                this.joinChannel(pm.get(1));
                this.channel = pm.get(1);
                log("Joinned channel: " + this.channel);
            } else {
                log("Attempting to join channel: " + this.channel);
                this.joinChannel(this.channel);
                log("Joinned channel: " + this.channel);
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_DIE)) {
            log("Received die command.");
            this.disconnect();
            log("Disconnected.");
            System.exit(0);
        } else if (pm.get(0).equalsIgnoreCase(CMD_PLAY) && rollers.size() > 0) {
            log("Starting the game of sixes");
            ArrayList<Integer> score = new ArrayList<Integer>();
            ArrayList<String> winners = new ArrayList<String>();
            for (String s : rollers) {
                ArrayList<Integer> tempscore = new ArrayList<Integer>();
                int first = this.random.nextInt(6) + 1;
                int second = this.random.nextInt(6) + 1;
                int third = this.random.nextInt(6) + 1;
                tempscore.add(first);
                tempscore.add(second);
                tempscore.add(third);
                if (score.isEmpty() || this.isBetterScore(tempscore, score) == 1) {
                    winners.removeAll(winners);
                    winners.add(s);
                    score = tempscore;
                } else if (this.isBetterScore(tempscore, score) == 0) {
                    winners.add(s);
                }
                this.sendNotice(this.channel, s + " rolls: " + (first + "," + second + "," + third));
            }
            String winner = "";
            for (String win : winners) {
                if (!winner.equals("")) winner += "and ";
                winner += win + " ";
            }
            String pre = "";
            if (winners.size() > 1) {
                pre = "The tied winners are ";
            } else {
                pre = "The winner is ";
            }
            this.sendNotice(this.channel, pre + winner + "with a roll of: " + (score.get(0) + "," + score.get(1) + "," + score.get(2)));
            this.rollers = new ArrayList<String>();
        } else {
            this.handleCommand(pm, sender, login, hostname);
        }
    }

    /**
   * This method handles all the 'public' commands. That is, if a command comes in from someone
   * who isn't an admin, it will be routed here.
   * 
   * Each of the functions in this method are to be considered insecure and should not freely
   * give information out.
   * 
   * @param pm
   * @param sender
   * @param login
   * @param hostname
   */
    private void handleCommand(ArrayList<String> pm, String sender, String login, String hostname) {
        if (pm.get(0).equalsIgnoreCase(CMD_PING)) {
            log("Pinged: " + sender + ":" + login + ":" + hostname);
            this.sendNotice(sender, "PONG");
        } else if (pm.get(0).equalsIgnoreCase(CMD_REVERSE)) {
            log("Reversing last message.");
            this.sendNotice(this.channel, this.reverseMessage(this.lastMessage));
        } else if (pm.size() >= 2 && (pm.get(0).equalsIgnoreCase(CMD_2V2) || pm.get(0).equalsIgnoreCase(CMD_3V3) || pm.get(0).equalsIgnoreCase(CMD_5V5))) {
            try {
                this.handleArenaRating(pm.get(0), Integer.parseInt(pm.get(1)), sender);
            } catch (NumberFormatException nfe) {
                log("Failed parsing 'rating' for function '" + pm.get(0) + "'");
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_TIME)) {
            log("Queried for the time.");
            this.sendNotice(sender, (new Date(System.currentTimeMillis())).toString());
        } else if (pm.get(0).equalsIgnoreCase(CMD_SHA1)) {
            if (pm.size() >= 2) {
                log("Returning a sha1 hash of: " + pm.get(1));
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA1");
                    md.reset();
                    byte[] digest;
                    digest = md.digest(pm.get(1).getBytes());
                    StringBuffer hexString = new StringBuffer();
                    for (int i = 0; i < digest.length; i++) {
                        hexString.append(hexDigit(digest[i]));
                    }
                    this.sendNotice(sender, hexString.toString());
                } catch (NoSuchAlgorithmException e) {
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_MD5)) {
            if (pm.size() >= 2) {
                log("Returning a md5 hash of: " + pm.get(1));
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.reset();
                    byte[] digest;
                    digest = md.digest(pm.get(1).getBytes());
                    StringBuffer hexString = new StringBuffer();
                    for (int i = 0; i < digest.length; i++) {
                        hexString.append(hexDigit(digest[i]));
                    }
                    this.sendNotice(sender, hexString.toString());
                } catch (NoSuchAlgorithmException e) {
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_RANDOM) && pm.size() == 2) {
            try {
                this.log(sender + "!" + login + "@" + hostname + " issued random for: " + pm.get(1));
                this.findRandom(sender, sender, login, hostname, pm.get(1));
            } catch (NoSuchURLException nste) {
                this.sendNotice(sender, "Sorry, nothing found for tag: " + pm.get(1));
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_RANDOM) && pm.size() == 1) {
            boolean foundOne = false;
            while (!foundOne) {
                try {
                    this.log(sender + "!" + login + "@" + hostname + " issued random with no tag.");
                    String tag = this.taghandler.getRandomTag();
                    this.findRandom(sender, sender, login, hostname, tag);
                    foundOne = true;
                } catch (NoSuchURLException nste) {
                    this.log(nste.getMessage());
                } catch (SQLException sqle) {
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_QQ) && pm.size() > 1) {
            String output = "";
            for (String string : pm) {
                if (!string.equalsIgnoreCase(CMD_QQ)) output += string + " ";
            }
            this.sendNotice(sender, output.replaceAll("[A-Z]", "Q").replaceAll("[a-z]", "q"));
        } else {
            this.handlePublicCommand(pm, sender, login, hostname);
        }
    }

    /**
   * This method handles all the 'public' commands that are said in message rather than pm'd
   * 
   * @param pm
   * @param sender
   * @param login
   * @param hostname
   */
    private void handlePublicCommand(ArrayList<String> pm, String sender, String login, String hostname) {
        if (pm.get(0).equalsIgnoreCase(CMD_HELP)) {
            if (pm.size() > 1) {
                this.handleHelp(pm.get(1), sender, login, hostname);
            } else if (pm.size() == 1) {
                this.handleHelp(sender, login, hostname);
            }
        } else if (pm.size() >= 2 && (pm.get(0).equalsIgnoreCase(CMD_2V2) || pm.get(0).equalsIgnoreCase(CMD_3V3) || pm.get(0).equalsIgnoreCase(CMD_5V5))) {
            try {
                this.handleArenaRating(pm.get(0), Integer.parseInt(pm.get(1)), this.channel);
            } catch (NumberFormatException nfe) {
                log("Failed parsing 'rating' for function '" + pm.get(0) + "'");
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_TIME)) {
            log("Queried for the time.");
            this.sendNotice(this.channel, (new Date(System.currentTimeMillis())).toString());
        } else if (pm.get(0).equalsIgnoreCase(CMD_ROLL)) {
            if (pm.size() == 1) {
                log("Queried normal roll.");
                this.sendNotice(this.channel, sender + " has rolled: " + this.random.nextInt(100));
            } else if (pm.size() == 2) {
                log("Queried for upper-bound roll.");
                try {
                    log("Trying to parse second argument as int");
                    int top = Integer.parseInt(pm.get(1));
                    if (top >= 0) {
                        this.sendNotice(this.channel, sender + " has rolled (0-" + top + "): " + this.random.nextInt(top));
                    }
                } catch (NumberFormatException nfe) {
                    log("Couldn't parse second arg as int, defaulting to normal roll.");
                    this.sendNotice(this.channel, sender + " has rolled: " + this.random.nextInt(100));
                }
            } else if (pm.size() == 3) {
                log("Queried for special roll.");
                try {
                    int bottom = Integer.parseInt(pm.get(1));
                    int top = Integer.parseInt(pm.get(2));
                    if (top >= bottom) {
                        this.sendNotice(this.channel, sender + " has rolled (" + bottom + "-" + top + "): " + (this.random.nextInt(top - bottom) + bottom));
                    }
                } catch (NumberFormatException nfe) {
                    log("Couldn't parse second (or third possibly) arg as int, defaulting to normal roll.");
                    this.sendNotice(this.channel, sender + " has rolled: " + this.random.nextInt(100));
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_FLIP)) {
            log("Queried for a flip.");
            this.sendNotice(this.channel, sender + " has requested a flip: " + (this.random.nextBoolean() ? "HEADS" : "TAILS"));
        } else if (pm.get(0).equalsIgnoreCase(CMD_SIXES)) {
            if (this.rollers.isEmpty()) {
                log("Starting new roll");
                this.rollers.add(sender);
                this.sendNotice(this.channel, sender + " has entered a game of sixes.");
            }
            if (!this.rollers.contains(sender)) {
                this.rollers.add(sender);
                this.sendNotice(this.channel, sender + " has entered a game of sixes.");
            }
            log("Queried for a sixes roll.");
        } else if (pm.get(0).equalsIgnoreCase(CMD_SHA1)) {
            if (pm.size() >= 2) {
                log("Returning a sha1 hash of: " + pm.get(1));
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA1");
                    md.reset();
                    byte[] digest;
                    digest = md.digest(pm.get(1).getBytes());
                    StringBuffer hexString = new StringBuffer();
                    for (int i = 0; i < digest.length; i++) {
                        hexString.append(hexDigit(digest[i]));
                    }
                    this.sendNotice(this.channel, hexString.toString());
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_MD5)) {
            if (pm.size() >= 2) {
                log("Returning a md5 hash of: " + pm.get(1));
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.reset();
                    byte[] digest;
                    digest = md.digest(pm.get(1).getBytes());
                    StringBuffer hexString = new StringBuffer();
                    for (int i = 0; i < digest.length; i++) {
                        hexString.append(hexDigit(digest[i]));
                    }
                    this.sendNotice(this.channel, hexString.toString());
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_FULL_SEARCH)) {
            if (pm.size() > 2) {
                log("Requesting all urls for multiple tags.");
                try {
                    pm.remove(0);
                    ArrayList<String> urls = this.taghandler.getUrlsForTags(pm);
                    if (urls.size() > 0) {
                        String tags = "";
                        for (String s : pm) {
                            if (tags.equalsIgnoreCase("")) {
                                tags += s;
                            } else tags += ", " + s;
                        }
                        this.sendNotice(sender, "Here are the results for tags: " + tags);
                        for (String s : urls) {
                            this.sendNotice(sender, s);
                        }
                    } else {
                        this.sendNotice(sender, "There were no urls for that tag.");
                    }
                } catch (SQLException sqle) {
                    log("Error searching for urls with multiple tags.");
                    sqle.printStackTrace();
                }
            } else if (pm.size() == 2) {
                log("Requesting all urls for tag: " + pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                try {
                    ArrayList<String> passingTags = new ArrayList<String>();
                    passingTags.add(pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                    ArrayList<String> urls = this.taghandler.getUrlsForTags(passingTags);
                    if (urls.size() > 0) {
                        this.sendNotice(sender, "Here are the results for tag: " + pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                        for (String s : urls) {
                            this.sendNotice(sender, s);
                        }
                    } else {
                        this.sendNotice(sender, "There were no urls for that tag.");
                    }
                } catch (SQLException sqle) {
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_SEARCH)) {
            if (pm.size() > 2) {
                log("Requesting all urls for multiple tags.");
                try {
                    pm.remove(0);
                    ArrayList<String> urls = this.taghandler.getUrlsForTags(pm, this.numberOfResults);
                    if (urls.size() > 0) {
                        String tags = "";
                        for (String s : pm) {
                            if (tags.equalsIgnoreCase("")) {
                                tags += s;
                            } else tags += ", " + s;
                        }
                        this.sendNotice(sender, "Here are the results for tags: " + tags);
                        for (String s : urls) {
                            this.sendNotice(sender, s);
                        }
                    } else {
                        this.sendNotice(sender, "There were no urls for that tag.");
                    }
                } catch (SQLException sqle) {
                    log("Error searching for urls with multiple tags.");
                    sqle.printStackTrace();
                }
            } else if (pm.size() == 2) {
                log("Requesting all urls for tag: " + pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                try {
                    ArrayList<String> passingTags = new ArrayList<String>();
                    passingTags.add(pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                    ArrayList<String> urls = this.taghandler.getUrlsForTags(passingTags, this.numberOfResults);
                    if (urls.size() > 0) {
                        this.sendNotice(sender, "Here are the results for tag: " + pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                        for (String s : urls) {
                            this.sendNotice(sender, s);
                        }
                    } else {
                        this.sendNotice(sender, "There were no urls for that tag.");
                    }
                } catch (SQLException sqle) {
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_TAGS_FOR)) {
            if (pm.size() >= 2) {
                log("Requesting all tags for url: " + pm.get(1));
                try {
                    ArrayList<String> tags = this.taghandler.getTagsForUrl(pm.get(1));
                    if (tags.size() > 0) {
                        this.sendNotice(sender, "Here are the tags for url: " + pm.get(1));
                        for (String s : tags) {
                            this.sendNotice(sender, s);
                        }
                    } else {
                        this.sendNotice(sender, "There were no tags for that url.");
                    }
                } catch (SQLException sqle) {
                    this.sendNotice(sender, "There were no tags for that url.");
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_COUNT)) {
            if (pm.size() > 2) {
                log("Counting all urls for multiple tags.");
                try {
                    pm.remove(0);
                    int count = this.taghandler.getCountForTags(pm);
                    String tags = "";
                    for (int i = 0; i < pm.size(); i++) {
                        if (i == pm.size() - 1) {
                            tags += pm.get(i).replaceAll("[^A-Za-z0-9]", "");
                        } else {
                            tags += pm.get(i).replaceAll("[^A-Za-z0-9]", "") + ", ";
                        }
                    }
                    this.sendNotice(sender, "There are " + count + " urls for tags: " + tags);
                } catch (SQLException sqle) {
                    log("An error occurred while counting urls for tag: " + pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                    log(sqle.getLocalizedMessage());
                }
            } else if (pm.size() == 2) {
                log("Counting all urls for tag: " + pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                try {
                    int count = this.taghandler.getCountForTag(pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                    this.sendNotice(sender, "There are " + count + " urls for tag: " + pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                } catch (SQLException sqle) {
                    log("An error occurred while counting urls for tag: " + pm.get(1).replaceAll("[^A-Za-z0-9]", ""));
                    log(sqle.getLocalizedMessage());
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_VERSION_NOTES)) {
            log("Dispatching the version notes.");
            for (String note : this.versionNotes.get(this.version)) {
                this.sendNotice(sender, note);
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_VERSION)) {
            log("Dispatching the version number.");
            this.sendNotice(sender, this.version);
        } else if (pm.get(0).equalsIgnoreCase(CMD_RANDOM) && pm.size() == 2) {
            try {
                this.log(sender + "!" + login + "@" + hostname + " issued random for: " + pm.get(1));
                this.findRandom(this.channel, sender, login, hostname, pm.get(1));
            } catch (NoSuchURLException nste) {
                this.sendNotice(this.channel, "Sorry, nothing found with tag: " + pm.get(1));
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_RANDOM) && pm.size() == 1) {
            boolean foundOne = false;
            while (!foundOne) {
                try {
                    this.log(sender + "!" + login + "@" + hostname + " issued random with no tag.");
                    String tag = this.taghandler.getRandomTag();
                    this.findRandom(this.channel, sender, login, hostname, tag);
                    foundOne = true;
                } catch (NoSuchURLException nste) {
                    this.log(nste.getMessage());
                } catch (SQLException sqle) {
                }
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_UPTIME)) {
            this.sendNotice(sender, "Started: " + this.startDate.get(Calendar.YEAR) + "-" + (this.startDate.get(Calendar.MONTH) + 1) + "-" + this.startDate.get(Calendar.DATE) + " " + this.startDate.get(Calendar.HOUR) + ":" + this.startDate.get(Calendar.MINUTE) + ":" + this.startDate.get(Calendar.SECOND) + " " + (this.startDate.get(Calendar.AM_PM) == 1 ? "PM" : "AM"));
        }
    }

    private void handleAmbiguousCommand(ArrayList<String> pm, String sender, String login, String hostname) {
        if (pm.get(0).equalsIgnoreCase(CMD_QQ) && pm.size() == 1) {
            if (!this.lastMessage.equalsIgnoreCase("")) {
                this.sendNotice(this.channel, this.lastMessage.replaceAll("[A-Z]", "Q").replaceAll("[a-z]", "q"));
            }
        } else if (pm.get(0).equalsIgnoreCase(CMD_REVERSE)) {
            log("Reversing last message.");
            this.sendNotice(this.channel, this.reverseMessage(this.lastMessage));
        }
        String nextMessage = "";
        for (int i = 0; i < pm.size(); i++) {
            if (i == 0 && pm.get(i).equalsIgnoreCase(CMD_QQ)) {
                break;
            }
            nextMessage += pm.get(i) + " ";
        }
        if (!nextMessage.equalsIgnoreCase("")) this.lastMessage = nextMessage;
    }

    /**
   * Helper method for handling the various functions' help responses.
   * @param sender
   * @param login
   * @param hostname
   */
    private void handleHelp(String sender, String login, String hostname) {
        this.sendNotice(sender, "You have asked for help (alternatively, you can ask for help with one of these commands by asking for it; example: help say), as a user level [user] you have access to the following commands:");
        if (this.isAdmin(hostname)) {
            this.sendNotice(sender, CMD_SAY);
            this.sendNotice(sender, CMD_HIDE);
            this.sendNotice(sender, CMD_JOIN);
            this.sendNotice(sender, CMD_DIE);
        }
        this.sendNotice(sender, CMD_PING);
        this.sendNotice(sender, CMD_2V2);
        this.sendNotice(sender, CMD_3V3);
        this.sendNotice(sender, CMD_5V5);
        this.sendNotice(sender, CMD_TIME);
        this.sendNotice(sender, CMD_ROLL);
        this.sendNotice(sender, CMD_FLIP);
        this.sendNotice(sender, CMD_SIXES);
        this.sendNotice(sender, CMD_SHA1);
        this.sendNotice(sender, CMD_MD5);
        this.sendNotice(sender, CMD_SEARCH);
        this.sendNotice(sender, CMD_FULL_SEARCH);
        this.sendNotice(sender, CMD_TAGS_FOR);
        this.sendNotice(sender, CMD_COUNT);
        this.sendNotice(sender, CMD_VERSION);
        this.sendNotice(sender, CMD_VERSION_NOTES);
        this.sendNotice(sender, CMD_RANDOM);
    }

    /**
   * Helper method for handling the various functions' help responses.
   * @param command
   * @param sender
   * @param login
   * @param hostname
   */
    private void handleHelp(String command, String sender, String login, String hostname) {
        if (this.isAdmin(hostname) && command.equalsIgnoreCase(CMD_SAY)) {
            this.sendNotice(sender, "SAY: a simple script that will force Helper to speak in his current channel.");
            this.sendNotice(sender, "usage: /msg Helper say [text] ...");
        } else if (this.isAdmin(hostname) && command.equalsIgnoreCase(CMD_HIDE)) {
            this.sendNotice(sender, "HIDE: a script that will force Helper to leave his current channel, but remain connected to the server.");
            this.sendNotice(sender, "usage: /msg Helper hide");
        } else if (this.isAdmin(hostname) && command.equalsIgnoreCase(CMD_JOIN)) {
            this.sendNotice(sender, "JOIN: a script that will force Helper to join a specified channel.");
            this.sendNotice(sender, "usage: /msg Helper join [channel]");
        } else if (this.isAdmin(hostname) && command.equalsIgnoreCase(CMD_DIE)) {
            this.sendNotice(sender, "DIE: terminates Helper.");
            this.sendNotice(sender, "usage: /msg Helper die");
        } else if (command.equalsIgnoreCase(CMD_PING)) {
            this.sendNotice(sender, "PING: a simple script to verify Helper is active.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_PING);
        } else if (command.equalsIgnoreCase(CMD_2V2) || command.equalsIgnoreCase(CMD_3V3) || command.equalsIgnoreCase(CMD_5V5)) {
            this.sendNotice(sender, "2v2/3v3/5v5: a simple script to determine arena points from a rating.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_2V2 + " 1500");
        } else if (command.equalsIgnoreCase(CMD_TIME)) {
            this.sendNotice(sender, "TIME: a simple script to display the current time.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_TIME);
        } else if (command.equalsIgnoreCase(CMD_ROLL)) {
            this.sendNotice(sender, "ROLL: forces Helper to make a random roll for you.");
            this.sendNotice(sender, "usage: [/msg] Helper " + CMD_ROLL + " [x [y]]");
            this.sendNotice(sender, "example: Helper " + CMD_ROLL + " 50 120");
        } else if (command.equalsIgnoreCase(CMD_FLIP)) {
            this.sendNotice(sender, "FLIP: forces Helper to randomly flip a coin for you.");
            this.sendNotice(sender, "usage: [/msg] Helper " + CMD_FLIP);
        } else if (command.equalsIgnoreCase(CMD_SIXES)) {
            this.sendNotice(sender, "SIXES: forces Helper to randomly roll three dice for you in the game of sixes.");
            this.sendNotice(sender, "usage: [/msg] Helper " + CMD_SIXES);
        } else if (command.equalsIgnoreCase(CMD_SHA1)) {
            this.sendNotice(sender, "SHA1: forces Helper to create a sha1 hash of the given string.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_SHA1 + " somestring");
        } else if (command.equalsIgnoreCase(CMD_MD5)) {
            this.sendNotice(sender, "MD5: forces Helper to create a md5 hash of the given string.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_MD5 + " somestring");
        } else if (command.equalsIgnoreCase(CMD_SEARCH)) {
            this.sendNotice(sender, "search: forces Helper to recite the " + this.numberOfResults + " most recent urls for a given tag.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_SEARCH + " someTag");
        } else if (command.equalsIgnoreCase(CMD_FULL_SEARCH)) {
            this.sendNotice(sender, "search: forces Helper to recite all known urls for a given tag.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_FULL_SEARCH + " someTag");
            this.sendNotice(sender, "WARNING: depending on the number of results, can be a bit spammy with no way to stop it.");
        } else if (command.equalsIgnoreCase(CMD_TAGS_FOR)) {
            this.sendNotice(sender, "TAGSFOR: forces Helper to recite all known tags for a given url.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_TAGS_FOR + " http://www.example.com");
        } else if (command.equalsIgnoreCase(CMD_COUNT)) {
            this.sendNotice(sender, "COUNT: forces Helper to count the number of urls for a given tag.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_COUNT + " someTag");
        } else if (command.equalsIgnoreCase(CMD_VERSION)) {
            this.sendNotice(sender, "VERSION: forces Helper to recite his current version.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_VERSION);
        } else if (command.equalsIgnoreCase(CMD_VERSION_NOTES)) {
            this.sendNotice(sender, "VERSION: forces Helper to recite his notes on the current release version.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_VERSION_NOTES);
        } else if (command.equalsIgnoreCase(CMD_RANDOM)) {
            this.sendNotice(sender, "RANDOM: forces Helper to fetch a random image from the PirateBay by a given tag.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_RANDOM + " someTag");
        } else if (command.equalsIgnoreCase(CMD_REVERSE)) {
            this.sendNotice(sender, "REVERSE: forces Helper to reverse the last message in the channel.");
            this.sendNotice(sender, "usage: /msg Helper " + CMD_REVERSE);
        }
    }

    /**
   * Helper method for finding hashes.
   * @param x
   * @return
   */
    private String hexDigit(byte x) {
        StringBuffer sb = new StringBuffer();
        char c;
        c = (char) ((x >> 4) & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        sb.append(c);
        c = (char) (x & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        sb.append(c);
        return sb.toString();
    }

    /**
   * Helper method that finds urls and tags in an array of strings.
   * @param sender
   * @param message
   */
    private void findURLs(String sender, String login, String hostname, ArrayList<String> message) {
        ArrayList<String> uris = new ArrayList<String>();
        ArrayList<String> tags = new ArrayList<String>();
        for (String s : message) {
            try {
                new URL(s);
                uris.add(s);
                for (String urlImpliedTag : URLUtils.chunk(s)) {
                    if (!URLUtils.isHtmlKeyword(urlImpliedTag)) {
                        tags.add(urlImpliedTag);
                    }
                }
            } catch (MalformedURLException murle) {
                if (!s.replaceAll("[^A-Za-z0-9]", "").equals("") && !s.equalsIgnoreCase(CMD_TAGS_FOR) && !s.equalsIgnoreCase(CMD_SEARCH)) {
                    tags.add(s.replaceAll("[^A-Za-z0-9]", "").toLowerCase());
                }
            }
        }
        for (String uri : uris) {
            try {
                log("Adding tags to url: " + uri);
                this.taghandler.addTagsToUrl(uri, tags, sender, login, hostname);
            } catch (SQLException sqle) {
                log("Error thrown when adding mappings.");
                log(sqle.getLocalizedMessage());
            }
        }
    }

    /**
   * Returns whether the new score is better, worse, or tied with the old one.
   * @param temp
   * @param old
   * @return
   */
    private int isBetterScore(ArrayList<Integer> temp, ArrayList<Integer> old) {
        if (hasTrips(temp) && !hasTrips(old)) {
            return 1;
        }
        if (!hasTrips(temp) && hasTrips(old)) {
            return -1;
        }
        if (hasTrips(temp) && hasTrips(old)) {
            if (temp.get(0) > old.get(0)) return 1;
            if (temp.get(0) < old.get(0)) return -1;
            return 0;
        }
        if (hasRow(temp) && !hasRow(old)) {
            return 1;
        }
        if (!hasRow(temp) && hasRow(old)) {
            return -1;
        }
        if (hasRow(temp) && hasRow(old)) {
            int newer = 0;
            for (Integer i : temp) {
                newer += i;
            }
            int older = 0;
            for (Integer i : old) {
                older += i;
            }
            if (newer > older) return 1;
            if (older > newer) return -1;
            return 0;
        }
        if (hasPair(temp) && !hasPair(old)) {
            return 1;
        }
        if (!hasPair(temp) && hasPair(old)) {
            return -1;
        }
        if (hasPair(temp) && hasPair(old)) {
            int temppair = this.getPair(temp);
            int oldpair = this.getPair(old);
            if (temppair > oldpair) return 1;
            if (temppair < oldpair) return -1;
            int newer = 0;
            for (Integer i : temp) {
                newer += i;
            }
            int older = 0;
            for (Integer i : old) {
                older += i;
            }
            if (newer > older) return 1;
            if (older > newer) return -1;
            return 0;
        }
        int newer = 0;
        for (Integer i : temp) {
            newer += i;
        }
        int older = 0;
        for (Integer i : old) {
            older += i;
        }
        if (newer > older) return 1;
        if (older > newer) return -1;
        return 0;
    }

    /**
   * Returns whether there is a triple-pair in score.
   * @param score
   * @return whether score has three of a kind
   */
    private boolean hasTrips(ArrayList<Integer> score) {
        return (score.get(0) == score.get(1) && score.get(1) == score.get(2));
    }

    /**
   * Returns whether there is a pair in score.
   * @param score
   * @return whether there is a pair in score.
   */
    private boolean hasPair(ArrayList<Integer> score) {
        return ((score.get(0) == score.get(1) && score.get(0) != score.get(2)) || (score.get(0) == score.get(2) && score.get(0) != score.get(1)) || (score.get(1) == score.get(2) && score.get(1) != score.get(0)));
    }

    /**
   * Determines whether a row exists in score:<br/>
   * <tt>1,2,3</tt>
   * @param score
   * @return whether there is a row in score
   */
    private boolean hasRow(ArrayList<Integer> score) {
        Collections.sort(score);
        return (score.get(2) - score.get(1) == 1 && score.get(1) - score.get(0) == 1);
    }

    /**
   * Returns the value that is paired in score
   * @param score
   * @return the value paired in score
   */
    private int getPair(ArrayList<Integer> score) {
        if (score.get(0) == score.get(1)) return score.get(0);
        if (score.get(1) == score.get(2)) return score.get(1);
        return score.get(2);
    }

    /**
   * Helper method for dynamically handling arena rating calculations.
   * @param bracket
   * @param rating
   * @param sender
   */
    private void handleArenaRating(String bracket, int rating, String sender) {
        double multiplican = BRACKET_2V2_MULTIPLICAN;
        if (bracket.equalsIgnoreCase(CMD_3V3)) {
            multiplican = BRACKET_3V3_MULTIPLICAN;
        } else if (bracket.equalsIgnoreCase(CMD_5V5)) {
            multiplican = BRACKET_5V5_MULTIPLICAN;
        }
        if (rating < 1500) {
            double points = Math.floor((0.22 * rating + 14) * multiplican);
            this.sendNotice(sender, "The points earned in " + bracket + " for a " + rating + " rating are: " + points);
        } else {
            double points = Math.floor((1511.26 / (1 + 1639.28 * Math.pow(Math.E, (-0.00412 * rating)))) * multiplican);
            this.sendNotice(sender, "The points earned in " + bracket + " for a " + rating + " rating are: " + points);
        }
    }

    /**
   * Determines whether the given login+hostname is an admin account for this bot.
   * @param login
   * @param hostname
   * @return whether the given login+hostname is an admin
   */
    private boolean isAdmin(String hostname) {
        for (String admin : adminHostnames) {
            if (hostname.equalsIgnoreCase(admin)) {
                return true;
            }
        }
        return false;
    }

    /**
   * Sends a message to sendTo with a URL to an image randomly gotten from
   * the Pirate Bay with by tag. Also, a record is saved to the database
   * with the pertaining information.
   * @param sendTo
   * @param sender
   * @param login
   * @param hostname
   * @param tag
   */
    private void findRandom(String sendTo, String sender, String login, String hostname, String tag) throws NoSuchURLException {
        this.log("Trying to find random for tag: " + tag);
        try {
            String data = URLEncoder.encode("tag", "UTF-8") + "=" + URLEncoder.encode(tag, "UTF-8");
            long startTime = System.currentTimeMillis();
            URLConnection connection = PIRATE_BAY.openConnection(torProxy);
            connection.setDoOutput(true);
            connection.setRequestProperty("Cookie", "images_per_page=1000000; __utmz=10087590.1196981748.1.1.utmccn=(direct)|utmcsr=(direct)|utmcmd=(none); __utma=10087590.1221366766.1196981748.1196983933.1196985818.3; country=US; show_offensive=1;");
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(data);
            wr.flush();
            wr.close();
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            String output = "";
            while ((line = rd.readLine()) != null) {
                output += line;
            }
            rd.close();
            this.log("Retrieving html (" + output.length() + " byte(s)) for tag \"" + tag + "\" took " + (System.currentTimeMillis() - startTime) + " milliseconds.");
            String[] tags = output.split("<");
            ArrayList<String> links = new ArrayList<String>();
            for (String s : tags) {
                if (s.length() > 1 && s.substring(0, 1).equalsIgnoreCase("a") && !s.substring(0, 12).equalsIgnoreCase("a href=\"/tag")) {
                    String addition = s.substring(0, s.length() - 2).substring(s.indexOf("/") + 1);
                    links.add(addition);
                }
            }
            if (links.size() <= 0) {
                throw new NoSuchURLException("No elements for tag: " + tag);
            } else if (links.size() == 1) {
                this.log("There was " + links.size() + " image with the tag \"" + tag + "\"");
            } else {
                this.log("There were " + links.size() + " images with the tag \"" + tag + "\"");
            }
            String page = "http://bayimg.com/" + links.get(this.random.nextInt(links.size()));
            URL randomImage = new URL(page);
            startTime = System.currentTimeMillis();
            URLConnection randomConnection = randomImage.openConnection(torProxy);
            randomConnection.setRequestProperty("Cookie", "images_per_page=1000000; __utmz=10087590.1196981748.1.1.utmccn=(direct)|utmcsr=(direct)|utmcmd=(none); __utma=10087590.1221366766.1196981748.1196983933.1196985818.3; country=US; show_offensive=1;");
            rd = new BufferedReader(new InputStreamReader(randomConnection.getInputStream()));
            output = "";
            while ((line = rd.readLine()) != null) {
                output += line;
            }
            rd.close();
            this.log("Retrieving the image page's html (" + output.length() + " byte(s)) took " + (System.currentTimeMillis() - startTime) + " milliseconds.");
            String subImgUrl = output.substring(output.indexOf("<a href=\"\" onclick=\"toggleResize(") + 33);
            subImgUrl = subImgUrl.substring(subImgUrl.indexOf("); return false;\"><img src=\"") + 28);
            String imgUrl = "http://bayimg.com" + subImgUrl.substring(0, subImgUrl.indexOf("\""));
            String randomTags = output.substring(output.indexOf("<h2>Tags</h2>"), output.indexOf("<p align="));
            randomTags = randomTags.replaceAll("<p>", "");
            randomTags = randomTags.replaceAll("</p>", "");
            String[] pruned = randomTags.split("\">");
            ArrayList<String> addTags = new ArrayList<String>();
            addTags.add(tag);
            for (String s : pruned) {
                if (s.contains("</a>") && !addTags.contains(s.substring(0, s.indexOf("</a>")))) {
                    addTags.add(s.substring(0, s.indexOf("</a>")));
                }
            }
            ArrayList<String> ourTags = this.taghandler.getTagsForUrl(imgUrl);
            for (String s : ourTags) {
                if (!addTags.contains(s)) {
                    addTags.add(s);
                }
            }
            if (links.size() == 1) {
                this.sendNotice(sendTo, "There was " + links.size() + " image with the tag \"" + tag + "\"");
            } else {
                this.sendNotice(sendTo, "There were " + links.size() + " images with the tag \"" + tag + "\"");
            }
            String pageOutput = imgUrl;
            for (String aTag : addTags) {
                pageOutput += " " + aTag;
            }
            this.sendNotice(sendTo, pageOutput);
        } catch (IOException ioe) {
        } catch (SQLException sqle) {
        }
    }

    /**
   * Reverses a single string message the slow way.
   * @param message
   * @return the reversed string
   */
    private String reverseMessage(String message) {
        String reversed = "";
        for (int i = 0; i < message.length(); i++) {
            reversed = message.charAt(i) + reversed;
        }
        return reversed;
    }

    /**
   * Sets up the version notes of this Bot
   */
    private void setupVersionNotes() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(VERSION_NOTES));
            String line = reader.readLine();
            this.version = "";
            this.versionNotes = new HashMap<String, ArrayList<String>>();
            while (line != null) {
                String[] mapping = line.split("=");
                String tempVersion = mapping[0].replace(" ", "");
                if (!tempVersion.equalsIgnoreCase(this.version)) {
                    ArrayList<String> notes = new ArrayList<String>();
                    notes.add(mapping[1]);
                    this.versionNotes.put(tempVersion, notes);
                } else {
                    this.versionNotes.get(tempVersion).add(mapping[1]);
                }
                this.version = tempVersion;
                line = reader.readLine();
            }
        } catch (IOException ioe) {
            this.log(ioe.getMessage());
        }
    }
}
