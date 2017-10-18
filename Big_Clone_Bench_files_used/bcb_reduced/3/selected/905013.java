package s.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.lf5.util.StreamUtils;
import s.Command;
import s.Commands;

public class CommandHelper {

    private static final String SYSTEM_BINARIES_EXTENSION = "system.binaries.extension";

    private static final String KEY_PROMPT = "command.prompt";

    private static final String KEY_SPLIT_CHAR = "command.split.char";

    private static final String KEY_DOS_COMMAND = "command.dos";

    private static Log logger = LogFactory.getLog(CommandHelper.class.getName());

    private static CommandHelper _instance;

    public static synchronized CommandHelper getInstance() {
        if (_instance == null) _instance = new CommandHelper();
        return _instance;
    }

    public void run(String run) {
        try {
            String commandString = PropsUtil.get(KEY_DOS_COMMAND) + Constants.SPACE + run;
            logger.info("Executing - " + commandString);
            Process exec = Runtime.getRuntime().exec(commandString);
            StreamUtils.copy(exec.getInputStream(), System.out);
            StreamUtils.copy(exec.getErrorStream(), System.err);
        } catch (IOException e) {
            logger.error(e, e);
            throw new RuntimeException(e);
        }
    }

    public void prompt() {
        SystemUtil.logNoLine(PropsUtil.get(KEY_PROMPT));
    }

    public boolean containsEncoded(String encoded, String input) {
        String[] strings = StringUtils.split(encoded, PropsUtil.get(KEY_SPLIT_CHAR));
        for (String string : strings) {
            if (string.equals(input)) {
                return true;
            }
        }
        return false;
    }

    public String getCommand(String cmd) {
        String run = null;
        List commands = getCommands().getCommandList();
        for (int i = 0; i < commands.size(); i++) {
            Command c = (Command) commands.get(i);
            logger.debug(c);
            CommandHelper helper = CommandHelper.getInstance();
            if (helper.containsEncoded(c.getAlias(), cmd) || helper.containsEncoded(c.getAlias(), cmd)) {
                run = c.getRun();
                break;
            }
        }
        if (run == null) {
            logger.warn("Command not found. Have you configured commands.xml with " + cmd + "??");
            run = getToolFromSystemPath(cmd, run);
        }
        run = run == null ? cmd : run;
        return run;
    }

    private String getToolFromSystemPath(String cmd, String run) {
        logger.debug(System.getProperties());
        String path = System.getProperty("java.library.path");
        String seperator = System.getProperty("path.separator");
        logger.debug(path + Constants.RETURN_NEW_LINE + seperator);
        String[] paths = StringUtils.split(path, seperator);
        for (int i = 0; i < paths.length; i++) {
            String p = paths[i];
            logger.debug("Finding in path " + p);
            String[] files = new File(p).list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    List extensions = PropsUtil.getList(SYSTEM_BINARIES_EXTENSION);
                    if (extensions.contains(name.toLowerCase())) return true;
                    return false;
                }
            });
            for (int j = 0; files != null && j < files.length; j++) {
                String file = files[j];
                if (file.equalsIgnoreCase(cmd)) {
                    run = p + File.separator + cmd;
                }
                logger.info(file);
            }
        }
        return run;
    }

    public Commands getCommands() {
        String root = "commands";
        Map methods = new HashMap();
        Map params = new HashMap();
        params.put("commands/command", "name");
        params.put("commands/command", "alias");
        methods.put("commands/command/note", "setNote");
        methods.put("commands/command/run", "setRun");
        DigesterUtil.DigesterUtilDetails ruleDetails = new DigesterUtil.DigesterUtilDetails(root, methods, params);
        InputStream xml = null;
        try {
            xml = new ByteArrayInputStream(FileUtils.readFileToString(new File(PropsUtil.get("command.file.path"))).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Commands cs = new Commands();
        cs = (Commands) DigesterUtil.digest(xml, cs, ruleDetails);
        return cs;
    }
}
