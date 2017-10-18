package concrete;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import cspfj.Solver;
import cspfj.constraint.Constraint;
import cspfj.problem.Variable;

public final class SQLResultWriter extends AbstractResultWriter {

    private Connection connection;

    private final URI connectionUri;

    private long executionId = -1;

    private final long configId;

    private final long versionId;

    private static final Logger logger = Logger.getLogger(SQLResultWriter.class.getName());

    public SQLResultWriter(URI jdbcUri, String allOptions) throws SQLException, ClassNotFoundException {
        super(false);
        if (jdbcUri.isOpaque()) {
            throw new SQLException("Opaque connection URI");
        }
        if ("mysql".equals(jdbcUri.getScheme())) {
            Class.forName("com.mysql.jdbc.Driver");
        }
        this.connectionUri = jdbcUri;
        connect();
        if (!controlTables(new String[] { "EXECUTIONS", "PROBLEMS", "PROBLEMTAGS", "STATISTICS", "TRACE", "VERSIONS" })) {
            createTables();
        }
        configId = config(allOptions);
        versionId = version(Concrete.version());
        disconnect();
        final Handler handler = new SQLLogger();
        Logger.getLogger("").addHandler(handler);
    }

    private boolean controlTables(final String[] tables) throws SQLException {
        ResultSet results = connection.getMetaData().getTables(null, null, "%", null);
        int found = 0;
        while (results.next()) {
            if (Arrays.binarySearch(tables, results.getString(3)) >= 0) {
                found++;
            }
        }
        return found == tables.length;
    }

    private long version(final String version) throws SQLException {
        Statement stmt = connection.createStatement();
        final ResultSet rst = stmt.executeQuery("SELECT versionId FROM versions WHERE version='" + version + "'");
        final long versionId;
        if (rst.next()) {
            versionId = rst.getInt(1);
        } else {
            stmt.executeUpdate("INSERT INTO versions(version, date) VALUES ('" + version + "','" + new SimpleDateFormat().format(new Date()) + "')");
            final ResultSet aiRst = stmt.getGeneratedKeys();
            if (aiRst.next()) {
                versionId = aiRst.getInt(1);
            } else {
                throw new SQLException("Could not get generated key");
            }
        }
        stmt.close();
        return versionId;
    }

    private long problemId(final String problem) throws SQLException, IOException {
        MessageDigest msgDigest = null;
        try {
            msgDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        final InputStream istr = cspom.Problem.problemInputStream(problem);
        final byte[] buffer = new byte[8192];
        int read;
        while ((read = istr.read(buffer)) > 0) {
            msgDigest.update(buffer, 0, read);
        }
        final String md5sum = Concrete.md5(msgDigest.digest());
        Statement stmt = connection.createStatement();
        final ResultSet rst = stmt.executeQuery("SELECT problemId FROM problems WHERE md5 = '" + md5sum + "'");
        final long problemId;
        if (rst.next()) {
            problemId = rst.getInt(1);
        } else {
            stmt.executeUpdate("INSERT INTO problems(name, md5) VALUES ('" + new File(problem).getName() + "', '" + md5sum + "')");
            final ResultSet aiRst = stmt.getGeneratedKeys();
            if (aiRst.next()) {
                problemId = aiRst.getInt(1);
            } else {
                throw new SQLException("Could not retrieve generated key");
            }
        }
        return problemId;
    }

    private long config(final String options) throws SQLException {
        MessageDigest msgDigest = null;
        try {
            msgDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        msgDigest.update(options.getBytes());
        final String md5sum = Concrete.md5(msgDigest.digest());
        Statement stmt = connection.createStatement();
        ResultSet rst = stmt.executeQuery("SELECT configId FROM configs WHERE md5='" + md5sum + "'");
        final long configId;
        if (rst.next()) {
            configId = rst.getInt(1);
        } else {
            stmt.executeUpdate("INSERT INTO configs(config, md5) VALUES ('" + options + "', '" + md5sum + "')");
            ResultSet aiRst = stmt.getGeneratedKeys();
            if (aiRst.next()) {
                configId = aiRst.getInt(1);
            } else {
                throw new SQLException("Could not retrieve generated id");
            }
        }
        stmt.executeUpdate("UPDATE executions SET configId=" + configId + " WHERE executionId=" + executionId);
        return configId;
    }

    private void connect() throws SQLException {
        final String[] userInfo;
        if (connectionUri.getUserInfo() == null) {
            userInfo = new String[0];
        } else {
            userInfo = connectionUri.getUserInfo().split(":");
        }
        connection = DriverManager.getConnection("jdbc:" + connectionUri.getScheme() + "://" + connectionUri.getHost() + connectionUri.getPath(), userInfo.length > 0 ? userInfo[0] : null, userInfo.length > 1 ? userInfo[1] : null);
    }

    private void disconnect() throws SQLException {
        connection.close();
    }

    private void createTables() throws SQLException {
        logger.info("Creating SQL tables");
        Statement stmt = connection.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS problems (" + "problemId integer primary key not null auto_increment," + "name varchar(200) unique not null," + "md5 varchar(32) unique not null)");
        stmt.execute("CREATE TABLE IF NOT EXISTS versions (" + "versionId integer primary key not null auto_increment," + "version varchar(200) unique not null," + "date varchar(20) not null)");
        stmt.execute("CREATE TABLE IF NOT EXISTS configs (" + "configId integer primary key not null auto_increment," + "config text," + "md5 varchar(32) unique not null)");
        stmt.execute("CREATE TABLE IF NOT EXISTS executions (" + "executionId integer primary key not null auto_increment," + "versionId integer not null," + "configId integer," + "problemId integer not null, " + "result varchar(20)," + "solution text)");
        stmt.execute("CREATE TABLE IF NOT EXISTS problemTags (" + "tag varchar(32) not null," + "problemId integer not null," + "primary key (tag, problemId))");
        stmt.execute("CREATE TABLE IF NOT EXISTS statistics (" + "name varchar(200) not null," + "executionId integer not null," + "value varchar(1000) not null, " + "primary key (name, executionId))");
        stmt.execute("CREATE TABLE IF NOT EXISTS trace (" + "executionId integer not null," + "sequenceNumber integer not null," + "date timestamp not null," + "level varchar(30) not null," + "source varchar(200) not null," + "content text," + "primary key (executionId,sequenceNumber)," + "foreign key (executionId) references executions (executionId) on delete cascade)");
        stmt.close();
    }

    @Override
    public void problem(final String name) throws IOException {
        super.problem(name);
        try {
            connect();
            final long problemId = problemId(name);
            final Statement stmt = connection.createStatement();
            stmt.executeUpdate("INSERT INTO executions(problemId, configId, versionId) VALUES (" + problemId + ", " + configId + ", " + versionId + ")");
            ResultSet aiRst = stmt.getGeneratedKeys();
            if (aiRst.next()) {
                executionId = aiRst.getInt(1);
            } else {
                throw new SQLException("Could not retrieve generated id");
            }
            disconnect();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void load(final Solver solver, final long load) throws IOException {
        super.load(solver, load);
        statistics.put("load-cpu", load / 1.0e9F);
        if (solver != null) {
            statistics.put("variables", solver.getProblem().getNbVariables());
            statistics.put("domains", solver.getProblem().getMaxDomainSize());
            final Map<Class<? extends Constraint>, Integer> constraintStats = new HashMap<Class<? extends Constraint>, Integer>();
            for (Constraint c : solver.getProblem().getConstraints()) {
                if (constraintStats.containsKey(c.getClass())) {
                    constraintStats.put(c.getClass(), 1 + constraintStats.get(c.getClass()));
                } else {
                    constraintStats.put(c.getClass(), 1);
                }
            }
            for (Class<? extends Constraint> c : constraintStats.keySet()) {
                statistics.put("nb-" + c.getSimpleName(), constraintStats.get(c));
            }
            statistics.put("arity", solver.getProblem().getMaxArity());
        }
    }

    @Override
    public boolean solution(final Map<Variable, Integer> solution, final int nbSatisfied, final boolean force) throws IOException {
        if (super.solution(solution, nbSatisfied, force)) {
            statistics.put("nb-satisfied", nbSatisfied);
            final StringBuilder stb = new StringBuilder();
            for (int i = 0; i < solution.size(); i++) {
                for (Variable v : solution.keySet()) {
                    if (v.getId() == i) {
                        stb.append(solution.get(v)).append(' ');
                        break;
                    }
                }
            }
            try {
                connect();
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("UPDATE executions SET solution='" + stb.toString() + "' WHERE executionId=" + executionId);
                disconnect();
            } catch (SQLException e) {
                throw new IOException(e);
            }
            return true;
        }
        return false;
    }

    @Override
    public void result(final Result result, final Throwable thrown) throws IOException {
        super.result(result, thrown);
        try {
            connect();
            final Statement stmt = connection.createStatement();
            stmt.executeUpdate("UPDATE executions SET result='" + result + "' WHERE executionId=" + executionId);
            stmt.close();
            if (solver != null) {
                statistics.put("nb-solutions", solver.getNbSolutions());
            }
            if (thrown != null) {
                statistics.put("error", thrown);
            }
            disconnect();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void nextProblem() throws IOException {
        try {
            connect();
            final Statement stmt = connection.createStatement();
            for (String name : statistics.keySet()) {
                logger.finer(name + " : " + statistics.get(name));
                stmt.executeUpdate("INSERT INTO statistics(name, executionId, value) VALUES ('" + name + "'," + executionId + ",'" + statistics.get(name) + "')");
            }
            stmt.close();
            disconnect();
        } catch (SQLException e) {
            logger.severe(statistics.toString());
            logger.throwing(SQLResultWriter.class.getSimpleName(), "nextProblem", e);
            throw new IOException(e);
        }
        executionId = -1;
        super.nextProblem();
    }

    public String toString() {
        return "SQL output " + super.toString();
    }

    public class SQLLogger extends Handler {

        private final Deque<LogRecord> logs;

        public SQLLogger() {
            logs = new ArrayDeque<LogRecord>();
        }

        @Override
        public void close() throws SecurityException {
            flush();
        }

        @Override
        public void flush() {
            if (executionId < 0) {
                return;
            }
            try {
                connect();
                final Statement stmt = connection.createStatement();
                while (!logs.isEmpty()) {
                    final LogRecord log = logs.removeFirst();
                    final StringBuilder query = new StringBuilder();
                    query.append("INSERT INTO trace (executionId, sequenceNumber, level, source, content) VALUES (").append(executionId).append(", ").append(log.getSequenceNumber()).append(", '").append(log.getLevel().getName()).append("', '").append(log.getSourceClassName()).append('.').append(log.getSourceMethodName()).append("', '").append(log.getMessage());
                    final Throwable thrown = log.getThrown();
                    if (thrown != null) {
                        query.append('\n').append(thrown.getMessage()).append('\n').append(Arrays.toString(log.getThrown().getStackTrace()));
                    }
                    query.append("')");
                    stmt.executeUpdate(query.toString());
                }
                stmt.close();
                disconnect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void publish(LogRecord arg0) {
            logs.addLast(arg0);
            flush();
        }
    }
}
