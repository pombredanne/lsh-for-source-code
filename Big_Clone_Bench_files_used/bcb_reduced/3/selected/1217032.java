package s.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import s.Commands;

public class DigesterUtilTest extends TestCase {

    protected void setUp() throws Exception {
    }

    public void testDigest() throws IOException {
        String root = "commands";
        Map methods = new HashMap();
        Map params = new HashMap();
        params.put("commands/command", "name");
        params.put("commands/command", "alias");
        methods.put("commands/command/note", "setNote");
        methods.put("commands/command/run", "setRun");
        DigesterUtil.DigesterUtilDetails ruleDetails = new DigesterUtil.DigesterUtilDetails(root, methods, params);
        InputStream xml = new ByteArrayInputStream(FileUtils.readFileToString(new File("src/test/resources/commands.xml")).getBytes());
        Commands cs = new Commands();
        cs = (Commands) DigesterUtil.digest(xml, cs, ruleDetails);
        System.out.println(ToStringBuilder.reflectionToString(cs));
    }
}
