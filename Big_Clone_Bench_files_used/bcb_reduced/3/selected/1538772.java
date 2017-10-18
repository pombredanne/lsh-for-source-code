package org.oclc.mana.rsudo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.oclc.mana.util.MaskingPasswordReader;
import ru.net.romikk.keepass.Database;
import ru.net.romikk.keepass.Entry;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.StreamGobbler;

/**
 * runs remote commands as another user (generally root) using sudo
 * 
 * @author Clifton L. Snyder
 * @created July 6, 2006
 * 
 */
public class RemoteSudo implements Callable<Object> {

    public enum Status {

        INIT, START, CONNECTED, PUT_COMPLETE, CMD_COMPLETE, FINISH
    }

    private static Properties defaults;

    public static Properties getDefaults() {
        if (defaults == null) {
            defaults = new Properties();
            defaults.setProperty("rsudo.sudo", "/usr/bin/sudo");
            defaults.setProperty("rsudo.help", "false");
            defaults.setProperty("rsudo.execute", "true");
            defaults.setProperty("rsudo.dumpprops", "false");
            defaults.setProperty("rsudo.delay", "-1");
            defaults.setProperty("rsudo.synchronous", "false");
            defaults.setProperty("rsudo.prompt", "rsudopass");
            defaults.setProperty("rsudo.timeout", "0");
            defaults.setProperty("rsudo.put", "false");
            defaults.setProperty("rsudo.test", "false");
            defaults.setProperty("printer.quiet", "false");
            defaults.setProperty("printer.loud", "false");
            defaults.setProperty("printer.prepend", "false");
            defaults.setProperty("printer.lines", "false");
            defaults.setProperty("user.name", System.getProperties().getProperty("user.name"));
            defaults.setProperty("sudo.name", "root");
        }
        return defaults;
    }

    public static void main(String[] args) {
        Properties props = parseArgs(args, RemoteSudo.getDefaults());
        if (props == null) {
            System.err.println("Unable to parse properties!");
            RemoteSudo.usage();
            System.exit(1);
        }
        if ("true".equals(props.getProperty("rsudo.help"))) {
            RemoteSudo.usage();
            System.exit(0);
        }
        if (props.getProperty("rsudo.hosts") == null) {
            System.err.println("you must specify at least one host!");
            RemoteSudo.usage();
            System.exit(2);
        }
        if ((props.getProperty("rsudo.remotecmd") == null) && ("false".equals(props.getProperty("rsudo.put")))) {
            System.err.println("you must specify a command/script to run or a file to put");
            RemoteSudo.usage();
            System.exit(3);
        }
        if ("true".equals(props.getProperty("rsudo.dumpprops"))) {
            try {
                props.store(System.out, "# RemoteSudo properties file");
                System.exit(0);
            } catch (IOException ioe) {
                System.err.println("unable to dump properties file");
                System.exit(1);
            }
        }
        String password = null;
        String tmp = props.getProperty("stdin");
        if (tmp != null && "true".equals(tmp)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            try {
                password = in.readLine();
                in.close();
            } catch (IOException ioe) {
                System.err.println("error reading password");
                System.exit(1);
            }
        } else {
            password = MaskingPasswordReader.readPassword();
        }
        props.setProperty("rsudo.password", password);
        Hashtable<String, Entry> creds = null;
        tmp = props.getProperty("keepass.kdb");
        if (tmp != null) {
            Database db = new Database(tmp);
            MessageDigest sha256 = null;
            try {
                sha256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e2) {
                e2.printStackTrace();
            }
            db.setMasterKey(sha256.digest(password.getBytes()));
            db.setPasswordHash(sha256.digest(password.getBytes()));
            try {
                db.decrypt();
            } catch (InvalidCipherTextException e1) {
                e1.printStackTrace();
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            creds = new Hashtable<String, Entry>(db.getEntries().length);
            for (Entry e : db.getEntries()) {
                creds.put(e.getUrl(), e);
            }
        }
        StringTokenizer st = new StringTokenizer(props.getProperty("rsudo.hosts"), ",:");
        int hostCount = st.countTokens();
        if (hostCount == 0) {
            System.err.println("you must specify at least one host!");
            RemoteSudo.usage();
            System.exit(1);
        }
        int delay = -1;
        try {
            delay = Integer.parseInt(props.getProperty("rsudo.delay"));
        } catch (Exception e) {
            System.err.println("rsudo: warning: unable to parse delay; continuing without one");
            delay = -1;
        }
        int timeout = 0;
        try {
            timeout = Integer.parseInt(props.getProperty("rsudo.timeout"));
        } catch (Exception e) {
            System.err.println("rsudo: warning: unable to parse timeout; continuing without one");
            timeout = 0;
        }
        timeout = ((timeout >= 0) ? (timeout * 1000) : 0);
        boolean sync = "true".equals(props.getProperty("rsudo.synchronous")) ? true : false;
        long checkTimer = 0;
        if (props.getProperty("rsudo.check") != null) {
            try {
                checkTimer = Long.parseLong(props.getProperty("rsudo.check"));
            } catch (Exception e) {
                System.err.println("rsudo: warning: unable to parse check timer; continuing without one");
                checkTimer = 0;
            }
        }
        int threadCount = hostCount;
        if (props.getProperty("rsudo.threads") != null) {
            if (sync) {
                System.err.println("rsudo: warning: --sync and --threads both specified; ignoring --threads argument");
            } else {
                try {
                    threadCount = Integer.parseInt(props.getProperty("rsudo.threads"));
                } catch (Exception e) {
                    System.err.println("rsudo: warning: unable to parse thread count; continuing using thread count of " + threadCount);
                }
            }
        }
        if (sync) threadCount = 1;
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        RemoteSudo[] rsudos = new RemoteSudo[hostCount];
        for (int i = 0; i < hostCount; i++) {
            String host = st.nextToken();
            rsudos[i] = (creds != null) ? new RemoteSudo(host, props, creds) : new RemoteSudo(host, props);
            service.submit(rsudos[i]);
            if (checkTimer > 0) {
                new Thread(new RemoteSudoMonitor(rsudos[i], (checkTimer * 1000))).start();
            }
            if (delay > 0) {
                try {
                    Thread.sleep(delay * 1000);
                } catch (InterruptedException e) {
                    System.err.println("rsudo: warning: unable to delay; continuing without delay to next host");
                }
            }
        }
        service.shutdown();
        if (timeout > 0) {
            try {
                service.awaitTermination(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        } else {
            try {
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException e) {
            }
        }
        System.exit(0);
    }

    protected static Properties parseArgs(String[] args, Properties defaults) {
        Properties props = new Properties(defaults);
        for (int i = 0; i < args.length; i++) {
            if ("-h".equals(args[i])) {
                props.setProperty("rsudo.help", "true");
                return props;
            } else if ("-p".equals(args[i])) {
                try {
                    FileInputStream propsFile = new FileInputStream(args[++i]);
                    props.load(propsFile);
                    return props;
                } catch (Exception e) {
                    System.err.println("error reading from properties file");
                    return null;
                }
            } else if ("--hosts".equals(args[i])) {
                try {
                    props.setProperty("rsudo.hosts", args[++i]);
                } catch (Exception e) {
                    System.err.println("error reading hosts");
                    return null;
                }
            } else if ("--threads".equals(args[i])) {
                try {
                    props.setProperty("rsudo.threads", args[++i]);
                } catch (Exception e) {
                    System.err.println("error reading threads argument");
                    return null;
                }
            } else if ("--dump".equals(args[i])) {
                props.setProperty("rsudo.dumpprops", "true");
            } else if ("--brady".equals(args[i])) {
                System.err.println("brady is a wanker.");
            } else if ("--rayray".equals(args[i])) {
                System.err.println("\"Yes, it's a good name for a flag, Raymond...but what will it *do*?\"");
            } else if ("--taint".equals(args[i])) {
                System.err.println("TP wanted a --taint option...");
            } else if ("-f".equals(args[i])) {
                String hosts = "";
                try {
                    RandomAccessFile raf = new RandomAccessFile(args[++i], "r");
                    String line = null;
                    while ((line = raf.readLine()) != null) {
                        StringTokenizer st = new StringTokenizer(line, " \t\n,");
                        while (st.hasMoreTokens()) {
                            hosts += st.nextToken() + ',';
                        }
                    }
                    hosts = hosts.substring(0, hosts.length() - 1);
                } catch (Exception e) {
                    System.err.println("error reading host file");
                    return null;
                }
                props.setProperty("rsudo.hosts", hosts);
            } else if ("--put".equals(args[i])) {
                props.setProperty("rsudo.put", "true");
                String localFile = "";
                String remoteDir = "";
                try {
                    localFile = args[++i];
                    File file = new File(localFile);
                    if (!file.exists()) {
                        System.err.println("error: local file " + localFile + " does not exist!");
                        return null;
                    } else {
                        props.setProperty("rsudo.put.localfile", localFile);
                        props.setProperty("rsudo.put.remotefile", file.getName());
                    }
                } catch (Exception e) {
                    System.err.println("error reading local filename for put");
                    return null;
                }
                try {
                    remoteDir = args[++i];
                    props.setProperty("rsudo.put.remotedir", remoteDir);
                } catch (Exception e) {
                    System.err.println("error reading remote filename for put");
                    return null;
                }
            } else if ("--pipe".equals(args[i])) {
                try {
                    props.setProperty("rsudo.pipe.args", args[++i]);
                } catch (Exception e) {
                    System.err.println("error reading pipe args");
                    return null;
                }
            } else if ("--script".equals(args[i])) {
                try {
                    props.setProperty("rsudo.localcmd", args[++i]);
                    int index = args[i].lastIndexOf('/');
                    String scriptName = args[i].substring(index + 1);
                    props.setProperty("rsudo.remotecmd", scriptName + "." + new Date().getTime());
                } catch (Exception e) {
                    System.err.println("error reading script name");
                    return null;
                }
            } else if ("--".equals(args[i])) {
                String cmd = "";
                for (++i; i < args.length; i++) {
                    cmd += args[i] + " ";
                }
                props.setProperty("rsudo.remotecmd", cmd);
                return props;
            } else if ("-u".equals(args[i])) {
                try {
                    props.setProperty("user.name", args[++i]);
                } catch (Exception e) {
                    System.err.println("error reading username");
                    return null;
                }
            } else if ("-k".equals(args[i])) {
                props.setProperty("keepass.kdb", args[++i]);
            } else if ("-su".equals(args[i])) {
                try {
                    props.setProperty("sudo.name", args[++i]);
                } catch (Exception e) {
                    System.err.println("error reading username");
                    return null;
                }
            } else if ("--delay".equals(args[i])) {
                try {
                    props.setProperty("rsudo.delay", args[++i]);
                } catch (Exception e) {
                    System.err.println("error reading delay");
                    return null;
                }
            } else if ("--timeout".equals(args[i])) {
                try {
                    props.setProperty("rsudo.timeout", args[++i]);
                } catch (Exception e) {
                    System.err.println("error reading delay");
                    return null;
                }
            } else if ("--check".equals(args[i])) {
                try {
                    props.setProperty("rsudo.check", args[++i]);
                } catch (Exception e) {
                    System.err.println("error reading check time");
                    return null;
                }
            } else if ("--sync".equals(args[i])) {
                props.setProperty("rsudo.synchronous", "true");
            } else if ("--stdin".equals(args[i])) {
                props.setProperty("stdin", "true");
            } else if ("--prepend".equals(args[i])) {
                props.setProperty("printer.prepend", "true");
            } else if ("--loud".equals(args[i])) {
                props.setProperty("printer.loud", "true");
            } else if ("--lines".equals(args[i])) {
                props.setProperty("printer.lines", "true");
            } else {
                System.err.println("rsudo: warning: unknown argument '" + args[i] + "'; skipping it");
            }
        }
        return props;
    }

    /**
	 * print a brief usage message
	 */
    private static void usage() {
        System.err.println(" usage: rsudo [OPTIONS] -- command                                ");
        System.err.println("            | [OPTIONS] --script scriptfile                       ");
        System.err.println("            | [OPTIONS] --put localfile remotedir                 ");
        System.err.println("            | -p propertiesfile                                   ");
        System.err.println("------------------------------------------------------------------");
        System.err.println(" mandatory command-line parameter (hosts)                         ");
        System.err.println("------------------------------------------------------------------");
        System.err.println("  --hosts host1,host2,hostN  | colon- or comma-separated host list");
        System.err.println("| -f hostfile                | whitespace-delimited file          ");
        System.err.println("| -p properties              | Java properties file               ");
        System.err.println("------------------------------------------------------------------");
        System.err.println(" optional command-line parameters                                 ");
        System.err.println("------------------------------------------------------------------");
        System.err.println(" -h                          | give a short help message and exit ");
        System.err.println(" -u username                 | provide a different ssh username   ");
        System.err.println(" -su username                | provide a different sudo username  ");
        System.err.println(" --delay delay               | minimum delay (s) between hosts    ");
        System.err.println(" --timeout timeout           | global timeout (s)                 ");
        System.err.println(" --sync                      | wait for previous host to complete ");
        System.err.println("                             |   before executing on next host    ");
        System.err.println(" --check n                   | monitor execution every n seconds  ");
        System.err.println(" --threads n                 | only start n threads at a time     ");
        System.err.println(" --prepend                   | prepend hostname to lines of output");
        System.err.println(" --put localfile remotedir   | copy a file into a remote directory");
        System.err.println(" --pipe cmd                  | pipes remote command into another  ");
        System.err.println(" --lines                     | prepend line # to lines of output  ");
        System.err.println(" --loud                      | echo hostname before output        ");
        System.err.println(" --dump                      | dump args in properties file format");
        System.err.println(" --stdin                     | read password from stdin           ");
        System.err.println("------------------------------------------------------------------");
    }

    private Status status;

    private Hashtable<String, Entry> creds;

    private Properties props;

    private String host;

    private Result result;

    public RemoteSudo(String host, Properties props) {
        this.host = host;
        this.props = props;
        this.creds = null;
        this.result = null;
        this.status = Status.INIT;
    }

    public RemoteSudo(String host, Properties props, Hashtable<String, Entry> creds) {
        this.host = host;
        this.props = props;
        this.creds = creds;
        this.result = null;
        this.status = Status.INIT;
    }

    public String getHostname() {
        return host;
    }

    public Result getResult() {
        return result;
    }

    public Status getStatus() {
        return status;
    }

    public Status call() {
        status = Status.START;
        Entry entry = (creds != null) ? creds.get(host) : null;
        String sudo = props.getProperty("rsudo.sudo");
        String username = (entry != null) ? entry.getUsername() : props.getProperty("user.name");
        String sudoname = props.getProperty("sudo.name");
        String prompt = props.getProperty("rsudo.prompt");
        String localcmd = props.getProperty("rsudo.localcmd");
        String remotecmd = props.getProperty("rsudo.remotecmd");
        String fullremotecmd = remotecmd;
        Connection conn = new Connection(host);
        boolean cont = false;
        String password = (entry != null) ? entry.getPassword() : props.getProperty("rsudo.password");
        try {
            conn.connect();
            cont = true;
        } catch (IOException e) {
            System.err.println("error: unable to connect to " + host);
            cont = false;
        }
        if (cont) {
            boolean isAuth = false;
            String[] authMethods = new String[0];
            try {
                authMethods = conn.getRemainingAuthMethods(username);
            } catch (IOException e) {
                System.err.println("error: unable to get authentication methods for " + host);
                isAuth = true;
                cont = false;
            }
            for (int i = 0; ((i < authMethods.length) && (!isAuth)); i++) {
                if ("password".equals(authMethods[i])) {
                    try {
                        cont = conn.authenticateWithPassword(username, password);
                        if (!cont) {
                            System.err.println("unable to authenticate to " + host + " using password method");
                        } else {
                            isAuth = true;
                        }
                    } catch (IOException e) {
                        cont = false;
                    }
                } else if ("keyboard-interactive".equals(authMethods[i])) {
                    try {
                        cont = conn.authenticateWithKeyboardInteractive(username, new PasswordCallback(password));
                        if (!cont) {
                            System.err.println("unable to authenticate to " + host + " using keyboard-interactive");
                        } else {
                            isAuth = true;
                        }
                    } catch (IOException e) {
                        cont = false;
                    }
                }
            }
        }
        if (!cont) System.err.println("warning: unable to authenticate to " + host);
        status = Status.CONNECTED;
        List<String> outs = new ArrayList<String>();
        if (cont && "true".equals(props.getProperty("rsudo.put"))) {
            String localFile = props.getProperty("rsudo.put.localfile");
            String remoteFile = props.getProperty("rsudo.put.remotefile");
            String remoteDir = props.getProperty("rsudo.put.remotedir");
            String result = "putting " + localFile + " to " + host + ":" + remoteDir + "/" + remoteFile + "...";
            try {
                SCPClient scp = conn.createSCPClient();
                scp.put(localFile, remoteFile, remoteDir, "0700");
                result += "successful!";
                status = Status.PUT_COMPLETE;
            } catch (IOException e) {
                System.err.println("error: unable to put " + localFile + " to " + host + ":" + remoteDir + "/" + remoteFile);
                result += "failed!";
                cont = false;
            }
            outs.add(result);
        }
        if (cont && (localcmd != null)) {
            try {
                SCPClient scp = conn.createSCPClient();
                scp.put(localcmd, remotecmd, "/tmp", "0700");
                fullremotecmd = "/tmp/" + remotecmd;
            } catch (IOException e) {
                System.err.println("error: unable to send " + localcmd + " to " + host);
                cont = false;
            }
        }
        Session sess = null;
        BufferedReader br = null;
        if (cont && (remotecmd != null)) {
            try {
                String tmp = props.getProperty("rsudo.pipe.args");
                String fullcmd = sudo + " -p '" + prompt + "' -u " + sudoname + " -S " + fullremotecmd + " 2>&1" + ((tmp != null) ? " | " + tmp : "");
                sess = conn.openSession();
                PrintWriter out = new PrintWriter(sess.getStdin());
                sess.execCommand(fullcmd);
                InputStream stdout = new StreamGobbler(sess.getStdout());
                br = new BufferedReader(new InputStreamReader(stdout));
                out.println(password);
                out.close();
                status = Status.CMD_COMPLETE;
            } catch (IOException e) {
                System.err.println("error: unable to execute remote command on " + host);
                cont = false;
            }
        }
        int exitStatus = 0;
        if (cont && (remotecmd != null)) {
            String inline = null;
            try {
                inline = br.readLine();
                if ((inline != null) && (inline.length() >= prompt.length()) && prompt.equals(inline.substring(0, prompt.length()))) {
                    if (inline.length() > prompt.length()) outs.add(inline.substring(prompt.length()));
                } else {
                    outs.add(inline);
                }
            } catch (IOException e) {
                System.err.println("error: couldn't read first line of output from " + host);
            }
            try {
                while (true) {
                    inline = br.readLine();
                    if (inline == null) break;
                    outs.add(inline);
                }
            } catch (IOException e) {
                System.err.println("error: couldn't read output from " + host);
            }
            try {
                br.close();
            } catch (IOException e) {
            }
            sess.close();
            try {
                exitStatus = sess.getExitStatus();
            } catch (Exception e) {
                exitStatus = -1;
            }
        }
        try {
            conn.close();
        } catch (Exception e) {
        }
        Result result = new Result(host, exitStatus, outs);
        ResultPrinter.print(result, props, System.out);
        status = Status.FINISH;
        return status;
    }
}

class RemoteSudoMonitor implements Runnable {

    private RemoteSudo rsudo;

    private long delay;

    public RemoteSudoMonitor(RemoteSudo rsudo, long delay) {
        this.rsudo = rsudo;
        this.delay = delay;
    }

    public void run() {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
        }
        while (rsudo.getStatus() != RemoteSudo.Status.FINISH) {
            System.err.println(rsudo.getHostname() + ": still running...");
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
        }
    }
}
