package GUI;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.security.MessageDigest;
import java.util.Set;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import Diff.Sync;
import TransmitterS.Teacher;

/**
 * @author mschneider
 */
public class LogInListeners {

    public static final int Connect = 0;

    public static final int WorkOffline = 1;

    public static Listener select(int id) {
        switch(id) {
            case Connect:
                {
                    return new Listener() {

                        public void handleEvent(Event event) {
                            Sett.ings().serverAddress = LogInWindow.comboServer.getText();
                            Sett.ings().hosts.add(LogInWindow.comboServer.getText());
                            Sett.ings().userName = LogInWindow.textUsername.getText();
                            Sett.ings().passWord = LogInWindow.textPassword.getText();
                            try {
                                if (Startup.server().userCenter().authN(Sett.ings().userName, Sett.ings().passWord)) {
                                    Startup.isOffline = false;
                                    Startup.userID = Startup.server().userCenter().getUser(Sett.ings().userName).getUserID();
                                    Startup.isTeacher = Startup.server().userCenter().getUser(Startup.userID) instanceof Teacher;
                                    Startup.groupID = null;
                                    if (!Sett.ings().serverAddress.equals("localhost")) {
                                        Set<String> remoteParsers = Startup.server().networkCenter().avaibleParsers();
                                        for (String parserJar : new File("parsers").list(new FilenameFilter() {

                                            public boolean accept(File dir, String name) {
                                                return name.matches(".*\\.jar");
                                            }
                                        })) {
                                            File parserFile = new File("parsers" + File.separator + parserJar);
                                            byte[] buffer = new byte[(int) parserFile.length()];
                                            BufferedInputStream input = new BufferedInputStream(new FileInputStream(parserFile));
                                            input.read(buffer, 0, buffer.length);
                                            input.close();
                                            byte[] localhash = MessageDigest.getInstance("MD5").digest(buffer);
                                            if (remoteParsers.contains(parserJar)) {
                                                if (Startup.server().networkCenter().verifyFile(parserJar) != localhash) new FileOutputStream(parserFile).write(Startup.server().networkCenter().downloadFile(parserJar));
                                                remoteParsers.remove(parserJar);
                                            }
                                        }
                                        for (String newParser : remoteParsers) new FileOutputStream(new File("parsers" + File.separator + newParser)).write(Startup.server().networkCenter().downloadFile(newParser));
                                    }
                                    Sync.initSync();
                                    LogInWindow.close(false);
                                    EditWindow.run();
                                }
                            } catch (Exception e) {
                                Startup.clearServer();
                                System.err.println("Login failed");
                                System.err.println(e);
                                e.printStackTrace();
                            }
                        }
                    };
                }
            case WorkOffline:
                {
                    return new Listener() {

                        public void handleEvent(Event event) {
                            LogInWindow.close(false);
                            Startup.isOffline = true;
                            EditWindow.run();
                        }
                    };
                }
            default:
                return new Listener() {

                    public void handleEvent(Event event) {
                        System.err.println("bad Listener");
                    }
                };
        }
    }

    public static KeyListener key(int id) {
        switch(id) {
            case Connect:
                return new KeyListener() {

                    public void keyPressed(KeyEvent e) {
                        return;
                    }

                    public void keyReleased(KeyEvent e) {
                        if (e.keyCode == 13 || e.keyCode == 16777296) {
                            select(Connect).handleEvent(null);
                        }
                    }
                };
            default:
                return new KeyListener() {

                    public void keyPressed(KeyEvent e) {
                        System.out.println("Bad Key!");
                    }

                    public void keyReleased(KeyEvent e) {
                        System.out.println("Bad Key released!");
                    }
                };
        }
    }
}
