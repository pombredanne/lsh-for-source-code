package concrete;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class SQLExecutionController {

    private Connection connection;

    private final URI connectionUri;

    private static final Logger logger = Logger.getLogger(SQLExecutionController.class.getName());

    public SQLExecutionController(URI jdbcUri) throws SQLException, ClassNotFoundException {
        if (jdbcUri.isOpaque()) {
            throw new SQLException("Opaque connection URI");
        }
        if ("mysql".equals(jdbcUri.getScheme())) {
            Class.forName("com.mysql.jdbc.Driver");
        }
        this.connectionUri = jdbcUri;
    }

    private void connect() throws SQLException {
        final String[] userInfo;
        if (connectionUri.getUserInfo() == null) {
            userInfo = new String[0];
        } else {
            userInfo = connectionUri.getUserInfo().split(":");
        }
        int tries = 0;
        while (true) {
            try {
                connection = DriverManager.getConnection("jdbc:" + connectionUri.getScheme() + "://" + connectionUri.getHost() + connectionUri.getPath(), userInfo.length > 0 ? userInfo[0] : null, userInfo.length > 1 ? userInfo[1] : null);
                break;
            } catch (SQLException e) {
                if (tries++ > 4) {
                    logger.severe("Could not connect to database, giving up");
                    throw e;
                }
            }
            logger.warning("Could not connect to database, retry in 3 seconds");
            try {
                Thread.sleep(3);
            } catch (InterruptedException e1) {
                logger.throwing(SQLExecutionController.class.getSimpleName(), "connect", e1);
            }
        }
    }

    private void disconnect() throws SQLException {
        connection.close();
    }

    public String control(final String allOptions) throws SQLException {
        connect();
        final Statement stmt = connection.createStatement();
        final ResultSet rst1 = stmt.executeQuery("SELECT versionId FROM versions WHERE version='" + Concrete.version() + "'");
        final long versionId;
        if (rst1.next()) {
            versionId = rst1.getInt(1);
        } else {
            disconnect();
            return "";
        }
        rst1.close();
        final MessageDigest msgDigest;
        try {
            msgDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e1) {
            logger.throwing(SQLExecutionController.class.getSimpleName(), "control", e1);
            disconnect();
            return "";
        }
        msgDigest.update(allOptions.getBytes());
        final ResultSet rst2 = stmt.executeQuery("SELECT configId FROM configs WHERE md5='" + Concrete.md5(msgDigest.digest()) + "'");
        final long configId;
        if (rst2.next()) {
            configId = rst2.getInt(1);
        } else {
            disconnect();
            return "";
        }
        rst2.close();
        final ResultSet rst4 = stmt.executeQuery("SELECT problems.md5 FROM executions " + "LEFT JOIN problems ON executions.problemId = problems.problemId WHERE " + "configId=" + configId + " AND versionId=" + versionId);
        final StringBuilder stb = new StringBuilder();
        while (rst4.next()) {
            stb.append(rst4.getString(1)).append('\n');
        }
        rst4.close();
        stmt.close();
        return stb.toString();
    }

    public static void main(final String[] args) throws IOException, SQLException, URISyntaxException, ClassNotFoundException {
        final Concrete cspfj = new Concrete(args);
        final SQLExecutionController controller = new SQLExecutionController(new URI(cspfj.getCommandLine().getOptionValue("sql")));
        final PrintStream out = System.out;
        out.println(controller.control(cspfj.optionsDigest()));
    }
}
