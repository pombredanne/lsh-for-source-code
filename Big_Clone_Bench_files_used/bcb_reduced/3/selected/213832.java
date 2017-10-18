package owagate.pop;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;
import owagate.mail.MailBox;
import owagate.mail.MailMessage;
import owagate.mail.MailRepository;
import owagate.net.Stream;
import owagate.net.StreamingObjectFactory;
import owagate.util.RunningObject;

/**
 This class implements the basic mecanics of a POP3 session. It handles the
 AUTHENTICATION state (by delegating to a MailBox instance) and passes on all
 the TRANSACTION state messages to a PopTransactions object.
 */
public class PopSession extends RunningObject {

    public static StreamingObjectFactory createFactory(final MailRepository repo, final String hostNameApop, final PopTransactionsFactory factory) {
        return new StreamingObjectFactory() {

            public RunningObject newInstance(Stream stream) {
                return new PopSession(repo, hostNameApop, stream, factory);
            }
        };
    }

    /** Constructs a new instance of Pop3Session. */
    public PopSession(MailRepository repo, String hostNameApop, Stream stream, PopTransactionsFactory factory) {
        mChallenge = (hostNameApop == null) ? null : "<" + kTimestamp + "." + System.currentTimeMillis() + hostNameApop + ">";
        mFactory = factory;
        mRepository = repo;
        mStream = new PopTextStreamImpl(stream);
    }

    public void destroy() {
        super.destroy();
        mStream.destroy();
    }

    protected void run() throws Exception {
        MailBox mailbox = authenticate();
        if (mailbox == null) return;
        PopTransactions xact = mFactory.createInstance(mStream, mailbox);
        String body, line;
        while (true) {
            line = mStream.readLine();
            if ((body = isCommand(line, "QUIT")) != null) {
                xact.handleQuit(body);
                break;
            }
            long linenum = mStream.getOutputLineNumber();
            try {
                if ((body = isCommand(line, "DELE")) != null) xact.handleDele(body); else if ((body = isCommand(line, "LIST")) != null) xact.handleList(body); else if ((body = isCommand(line, "NOOP")) != null) xact.handleNoop(body); else if ((body = isCommand(line, "RETR")) != null) xact.handleRetr(body); else if ((body = isCommand(line, "RSET")) != null) xact.handleRset(body); else if ((body = isCommand(line, "STAT")) != null) xact.handleStat(body); else if ((body = isCommand(line, "TOP")) != null) xact.handleTop(body); else if ((body = isCommand(line, "UIDL")) != null) xact.handleUidl(body); else xact.unknown(line);
                long linenum2 = mStream.getOutputLineNumber();
                if (linenum == linenum2) mStream.writeOk();
            } catch (Exception e) {
                long linenum2 = mStream.getOutputLineNumber();
                if (linenum == linenum2) mStream.writeErr(e.getMessage()); else {
                    mStream.destroy();
                    break;
                }
            }
        }
    }

    private MailBox authenticate() throws Exception {
        String line = "POP3 Server Ready" + ((mChallenge == null) ? "" : (" " + mChallenge));
        MailBox mailbox = null;
        mStream.writeOk(line);
        while (mailbox == null) {
            line = mStream.readLine();
            if (isQuit(line)) break;
            String body;
            if ((body = isCommand(line, "USER")) != null) {
                String user = body;
                MailBox mb = mRepository.getMailBox(user);
                if (mb == null) mStream.writeErr("No mailbox for: " + line); else {
                    mStream.writeOk();
                    String pswd, line2 = mStream.readLine();
                    if (isQuit(line2)) break;
                    if ((pswd = isCommand(line2, "PASS")) == null) mStream.writeErr("Expected PASS but got {" + line2 + "}"); else {
                        String realPswd = mb.getPassword();
                        if ((realPswd != null) ? pswd.equals(realPswd) : mb.checkPassword(pswd)) {
                            mailbox = mb;
                            mStream.writeOk();
                        } else mStream.writeErr("Bad password for " + user);
                    }
                }
            } else if ((body = isCommand(line, "APOP")) != null) {
                if (mChallenge == null) mStream.writeErr("APOP Not Supported"); else {
                    int space = body.indexOf(' ');
                    if (space < 0) mStream.writeErr("Bad APOP command {" + line + "}"); else {
                        String user = body.substring(0, space);
                        String hash = body.substring(space + 1);
                        MailBox mb = mRepository.getMailBox(user);
                        if (mb == null) mStream.writeErr("No mailbox for: " + line); else if (mb.getPassword() == null) mStream.writeErr("User(" + user + ") cannot login using APOP"); else {
                            String secret = mChallenge + mb.getPassword();
                            MessageDigest md5 = MessageDigest.getInstance("MD5");
                            md5.update(secret.getBytes("UTF-8"));
                            byte[] digest = md5.digest();
                            String digestStr = new String(Hex.encodeHex(digest));
                            if (hash.equalsIgnoreCase(digestStr)) {
                                mailbox = mb;
                                mStream.writeOk();
                            } else mStream.writeErr("Bad password for " + user);
                        }
                    }
                }
            } else mStream.writeErr("Authentication required (got '" + line + "')");
        }
        return mailbox;
    }

    private static String isCommand(String line, String cmd) {
        final int cmdlen = cmd.length();
        if (line.length() < cmdlen) return null;
        if (line.length() == cmdlen) return cmd.equalsIgnoreCase(line) ? "" : null;
        if (line.substring(0, cmdlen + 1).equalsIgnoreCase(cmd + " ")) return line.substring(cmdlen + 1);
        return null;
    }

    private boolean isQuit(String line) {
        if (!line.equalsIgnoreCase("QUIT")) return false;
        mStream.writeOk("Goodbye!");
        return true;
    }

    private static final long kTimestamp = System.currentTimeMillis();

    private final String mChallenge;

    private final PopTransactionsFactory mFactory;

    private final MailRepository mRepository;

    private final PopTextStream mStream;
}
