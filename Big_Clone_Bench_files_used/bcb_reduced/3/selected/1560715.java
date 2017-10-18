package org.continuent.myosotis.protocol.postgresql;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import org.apache.log4j.Logger;
import org.continuent.myosotis.ServerThread;
import org.continuent.myosotis.configuration.Configuration;
import org.continuent.myosotis.configuration.UserMapItem;
import org.continuent.myosotis.protocol.ProtocolHandler;
import com.continuent.tungsten.commons.mysql.Utils;

/**
 * This class handle all the PostgreSQL protocol logic.
 * 
 * @author <a href="mailto:csaba.simon@continuent.com">Csaba Simon</a>
 * @author <a href="mailto:gilles.rayrat@continuent.com">Gilles Rayrat</a>
 * @author <a href="mailto:stephane.giron@continuent.com">Stephane Giron</a>
 * @version $Id: $
 */
public class PostgreSQLProtocolHandler extends ProtocolHandler {

    private static final Logger logger = Logger.getLogger(PostgreSQLProtocolHandler.class);

    private static final int INITIALIZING = 0;

    private static final int AUTHENTICATING = 1;

    private static final int COMMAND_PROCESSING = 2;

    private static final int SYNCHRONIZING = 3;

    private int state = INITIALIZING;

    private static final int PROTOCOL_V3 = 196608;

    private static final int CANCEL_REQUEST = 80877102;

    private static final int SSL_REQUEST = 80877103;

    private static final long JDBC_DATE_INFINITY = 9223372036825200000l;

    private static final long JDBC_DATE_MINUS_INFINITY = -9223372036832400000l;

    private static final String POSTGRES_DATE_INFINITY = "infinity";

    private static final String POSTGRES_DATE_MINUS_INFINITY = "-infinity";

    private final Socket socket;

    DataInputStream input;

    BufferedOutputStream output;

    private String salt;

    private int secretKey;

    private String user = "";

    private String database = "";

    private boolean wasErrorInTransaction = false;

    private PostgreSQLPreparedStatement preparedStatement;

    /**
     * Creates a new <code>PostgreSQLProtocolHandler</code> object
     * 
     * @param socket the socket for communication to the frontend
     * @param config the configuration
     * @throws IOException if a read or write operation fails
     */
    public PostgreSQLProtocolHandler(Socket socket, Configuration config, ServerThread serverThread) throws IOException {
        super(socket.getInetAddress().getHostAddress(), config, serverThread);
        this.socket = socket;
        input = new DataInputStream(socket.getInputStream());
        output = new BufferedOutputStream(socket.getOutputStream());
        salt = Utils.generateRandomString(4);
        Random random = new Random();
        secretKey = random.nextInt();
        preparedStatement = new PostgreSQLPreparedStatement();
    }

    /**
     * First calls {@link ProtocolHandler#close()}, then closes the socket, its
     * input and output streams as well as all prepared Statements if any.
     */
    public void close() {
        super.close();
        try {
            output.close();
        } catch (IOException ignored) {
        }
        try {
            input.close();
        } catch (IOException ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        preparedStatement.close();
    }

    /**
     * The state machine.
     * 
     * @return true to keep the connection and false to close the connection
     * @throws IOException if a read or write operation fails
     */
    public boolean handleClientRequest() throws IOException {
        boolean result;
        if (state == INITIALIZING) {
            result = handleStartUp();
        } else if (state == AUTHENTICATING) {
            result = handleAuthentication();
        } else if (state == COMMAND_PROCESSING) {
            result = handleCommand();
        } else if (state == SYNCHRONIZING) {
            result = handleSync();
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Handle the startup packet. For history reason this packet does not have
     * the command field in the first byte. Packet format: length (4 bytes) +
     * request code (4 bytes) + other data (x bytes)
     * 
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleStartUp() throws IOException {
        boolean result;
        PostgreSQLPacket packet = new PostgreSQLPacket(input, false);
        int request = packet.getInt32();
        if (request == CANCEL_REQUEST) {
            result = handleCancelRequest(packet);
        } else if (request == SSL_REQUEST) {
            result = handleSSLRequest();
        } else if (request == PROTOCOL_V3) {
            result = handleProtocolV3(packet);
        } else {
            logger.error("Only protocol version 3.0 is supported!");
            result = false;
        }
        return result;
    }

    /**
     * Handle the cancel request. Read the process ID and the secret key. If the
     * secret key is equal with our secret key then kill the specified process.
     * 
     * @param packet the PostgreSQL client packet
     * @return false to close the connection and true to keep it
     * @throws EOFException if the packet does not contain enough data
     */
    private boolean handleCancelRequest(PostgreSQLPacket packet) throws EOFException {
        packet.getInt32();
        packet.getInt32();
        return true;
    }

    /**
     * Handle the SSL request. As it is implemented now just send back a message
     * that is not supported.
     * 
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleSSLRequest() throws IOException {
        output.write('N');
        output.flush();
        return true;
    }

    /**
     * Handle protocol version 3.0. Read the user and database name and request
     * MD5 password for authentication.
     * 
     * @param packet the PostgreSQL client package.
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleProtocolV3(PostgreSQLPacket packet) throws IOException {
        String s;
        while ((s = packet.getStringNullTerminated()).length() != 0) {
            if ("user".equals(s)) {
                user = packet.getStringNullTerminated();
            } else if ("database".equals(s)) {
                database = packet.getStringNullTerminated();
            } else {
                packet.getStringNullTerminated();
            }
        }
        logger.debug("User = " + user + " database = " + database);
        PostgreSQLPacket buffer = new PostgreSQLPacket(16);
        buffer.setCommand('R');
        buffer.putInt32(5);
        buffer.putString(salt);
        buffer.write(output);
        output.flush();
        state = AUTHENTICATING;
        return true;
    }

    /**
     * Handle the MD5 password authentication. Send some server parameters to
     * the frontend. Send the secret key needed for the CancelRequest. Will try
     * to connect to the database with the user credentials.
     * 
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleAuthentication() throws IOException {
        try {
            PostgreSQLPacket auth = new PostgreSQLPacket(input, true);
            String scrambledPassword = auth.getStringNullTerminated();
            verifyUsernameAndPassword(user, scrambledPassword);
            try {
                connectToDatabase(user, database, false);
            } catch (SQLException s) {
                logger.error("Connecting to database failed.\n" + s.getLocalizedMessage());
                sendErrorResponse("3D000", String.format("Connecting to database failed.\n%s", s.getLocalizedMessage()));
                return false;
            }
        } catch (EOFException e) {
            return false;
        } catch (IOException i) {
            return false;
        }
        PostgreSQLPacket buffer = new PostgreSQLPacket(16);
        buffer.setCommand('R');
        buffer.putInt32(0);
        buffer.write(output);
        buffer.setCommand('S');
        buffer.putStringNullTerminated("server_version");
        buffer.putStringNullTerminated(config.getServerVersion());
        buffer.write(output);
        buffer.setCommand('S');
        buffer.putStringNullTerminated("client_encoding");
        buffer.putStringNullTerminated("UNICODE");
        buffer.write(output);
        buffer.setCommand('S');
        buffer.putStringNullTerminated("server_encoding");
        buffer.putStringNullTerminated("UNICODE");
        buffer.write(output);
        buffer.setCommand('K');
        buffer.putInt32((int) Thread.currentThread().getId());
        buffer.putInt32(secretKey);
        buffer.write(output);
        buffer.setCommand('Z');
        buffer.putChar('I');
        buffer.write(output);
        output.flush();
        state = COMMAND_PROCESSING;
        return true;
    }

    /**
     * Handle a command. For not (yet) handled command the connection will be
     * closed.
     * 
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleCommand() throws IOException {
        boolean result;
        PostgreSQLPacket packet = new PostgreSQLPacket(input, true);
        byte command = packet.getCommand();
        switch(command) {
            case 'Q':
                result = handleQueryCommand(packet.getStringNullTerminated());
                break;
            case 'X':
                result = false;
                break;
            case 'P':
                result = handleParseCommand(packet);
                break;
            case 'B':
                result = handleBindCommand(packet);
                break;
            case 'E':
                result = handleExecuteCommand(packet);
                break;
            case 'D':
                result = handleDescribeCommand(packet);
                break;
            case 'S':
                sendReadyForQuery();
                result = true;
                break;
            case 'H':
                result = true;
                output.flush();
                break;
            default:
                logger.error("Command '" + (char) command + "' not handled!");
                result = false;
                break;
        }
        return result;
    }

    /**
     * Discard any command until a sync is found.
     * 
     * @return false to close the connection and true to keep it
     * @throws IOException if the packet read operation fails
     */
    private boolean handleSync() throws IOException {
        PostgreSQLPacket packet = new PostgreSQLPacket(input, true);
        byte command = packet.getCommand();
        switch(command) {
            case 'S':
                sendReadyForQuery();
                state = COMMAND_PROCESSING;
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * Handle the bind command.
     * 
     * @param packet that contains the bind command fields
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleBindCommand(PostgreSQLPacket packet) throws IOException {
        String portalName = packet.getStringNullTerminated().toLowerCase();
        String statementName = packet.getStringNullTerminated().toLowerCase();
        if (logger.isTraceEnabled()) logger.trace("BIND: portal = " + portalName + " statement = " + statementName);
        preparedStatement.createPortal(statementName, portalName);
        PostgreSQLStatement statement = preparedStatement.getPortal(portalName);
        PreparedStatement pStmt = statement.getStatement();
        if (pStmt == null) {
            logger.error("No prepared statement portal named " + portalName);
            return true;
        }
        int paramFormatCount = packet.getInt16();
        for (int i = 1; i <= paramFormatCount; i++) {
            packet.getInt16();
        }
        ParameterMetaData metaData = null;
        try {
            metaData = pStmt.getParameterMetaData();
        } catch (SQLException e) {
            logger.error("Error getting parameter metadata.", e);
        }
        int numberOfParameters = packet.getInt16();
        for (int i = 1; i <= numberOfParameters; i++) {
            String param = packet.getStringLenEncoded();
            try {
                if (param == null) {
                    pStmt.setNull(i, Types.NULL);
                    if (logger.isTraceEnabled()) logger.trace("Set parameter #" + i + " to NULL");
                } else {
                    if (metaData == null) {
                        pStmt.setString(i, param);
                        if (logger.isTraceEnabled()) logger.trace("Set parameter #" + i + " sql type UNKNOWN value=" + param);
                    } else {
                        int sqlType = metaData.getParameterType(i);
                        if (sqlType == Types.NUMERIC || sqlType == Types.DECIMAL) {
                            pStmt.setBigDecimal(i, new BigDecimal(param));
                        } else if (sqlType == Types.TIMESTAMP) {
                            Timestamp ts = null;
                            try {
                                ts = Timestamp.valueOf(param);
                            } catch (IllegalArgumentException e) {
                                if (logger.isTraceEnabled()) logger.trace("'" + param + "' timestamp has unusual format. Trying postgres ones");
                                DateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS Z");
                                java.util.Date d = null;
                                try {
                                    d = format.parse(param);
                                } catch (ParseException e2) {
                                    try {
                                        int colonInTZ = param.lastIndexOf(':');
                                        if (colonInTZ > 0) {
                                            String cleanTs = param.substring(0, colonInTZ) + param.substring(colonInTZ + 1);
                                            d = format.parse(cleanTs);
                                        }
                                    } catch (ParseException e3) {
                                        logger.error("Could not convert '" + param + "' into jdbc timestamp");
                                        return true;
                                    }
                                    ts = new Timestamp(d.getTime());
                                }
                            }
                            pStmt.setTimestamp(i, ts);
                        } else if (sqlType == Types.BINARY) {
                            pStmt.setBytes(i, param.getBytes());
                        } else {
                            pStmt.setObject(i, param, metaData.getParameterType(i));
                        }
                        if (logger.isTraceEnabled()) logger.trace("Set parameter #" + i + " sql type=" + sqlType + " value=" + param);
                    }
                }
            } catch (SQLException e) {
                logger.error("Error binding parameters.", e);
            }
        }
        int resultFormatCount = packet.getInt16();
        for (int i = 1; i <= resultFormatCount; i++) {
            packet.getInt16();
        }
        PostgreSQLPacket buffer = new PostgreSQLPacket(16);
        buffer.setCommand('2');
        buffer.write(output);
        return true;
    }

    /**
     * Handle the execute command.
     * 
     * @param packet that contains the bind command fields
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleExecuteCommand(PostgreSQLPacket packet) throws IOException {
        String portalName = packet.getStringNullTerminated().toLowerCase();
        int maxRows = packet.getInt32();
        logger.trace("EXECUTE: portal = " + portalName);
        PostgreSQLStatement statement = preparedStatement.getPortal(portalName);
        PreparedStatement pStmt = statement.getStatement();
        try {
            pStmt.setFetchSize(maxRows);
            boolean done;
            int updateCount;
            boolean isResultSet = pStmt.execute();
            do {
                if (isResultSet) {
                    updateCount = 0;
                    ResultSet resultSet = pStmt.getResultSet();
                    sendResultSet(resultSet);
                    resultSet.close();
                } else {
                    updateCount = pStmt.getUpdateCount();
                    sendUpdateCount(updateCount, Utils.removeComments(statement.getQuery()).toUpperCase());
                }
                isResultSet = pStmt.getMoreResults();
                done = !isResultSet && (updateCount == -1);
            } while (!done);
        } catch (SQLException e) {
            sendAndLogError("Error executing prepared statements.", e);
            wasErrorInTransaction = true;
            state = SYNCHRONIZING;
        }
        return true;
    }

    /**
     * Handle the describe command.
     * 
     * @param packet that contains the describe command fields
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleDescribeCommand(PostgreSQLPacket packet) throws IOException {
        char type = packet.getChar();
        String name = packet.getStringNullTerminated().toLowerCase();
        logger.trace("DESCRIBE: type = " + type + " name = " + name);
        if (type == 'S') {
            ResultSetMetaData resultSetMetaData;
            try {
                PreparedStatement pStmt = preparedStatement.getStatement(name);
                ParameterMetaData parameterMetaData = pStmt.getParameterMetaData();
                int parameterCount = parameterMetaData.getParameterCount();
                PostgreSQLPacket buffer = new PostgreSQLPacket(16);
                buffer.setCommand('t');
                buffer.putInt16(parameterCount);
                for (int i = 1; i <= parameterCount; i++) {
                    buffer.putInt32(JDBCToPostgreSQLType.getPostgreSQLType(parameterMetaData.getParameterTypeName(i)));
                }
                buffer.write(output);
                resultSetMetaData = pStmt.getMetaData();
                int columns = 0;
                if (resultSetMetaData == null) {
                    String error = "Unable to get resultset metadata while fetching prepared statement metadata: " + name;
                    sendErrorResponse(null, error);
                    logger.error(error);
                    state = SYNCHRONIZING;
                    return true;
                }
                columns = resultSetMetaData.getColumnCount();
                buffer.setCommand('T');
                buffer.putInt16(columns);
                for (int i = 1; i <= columns; i++) {
                    buffer.putStringNullTerminated(resultSetMetaData.getColumnLabel(i));
                    buffer.putInt32(0);
                    buffer.putInt16(i);
                    buffer.putInt32(JDBCToPostgreSQLType.getPostgreSQLType(resultSetMetaData.getColumnTypeName(i)));
                    buffer.putInt16(resultSetMetaData.getColumnDisplaySize(i));
                    buffer.putInt32(-1);
                    buffer.putInt16(0);
                }
                buffer.write(output);
                sendReadyForQuery();
            } catch (SQLException e) {
                sendAndLogError("Error fetching prepared statement metadata: " + name, e);
                state = SYNCHRONIZING;
            }
        }
        return true;
    }

    /**
     * Parse an extended query.
     * 
     * @param packet that contains the prepared statement and the parameters
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleParseCommand(PostgreSQLPacket packet) throws IOException {
        String name = packet.getStringNullTerminated().toLowerCase();
        String statement = packet.getStringNullTerminated();
        packet.getInt16();
        logger.trace("Parsing query: " + statement);
        Connection connectionToUse = getConnectionForRequest(statement, false);
        if (connectionToUse == null) {
            sendInvalidConnectionError();
            return false;
        }
        try {
            preparedStatement.createPreparedStatement(connectionToUse, name, Utils.replaceParametersWithQuestionMarks(statement));
        } catch (SQLException e) {
            sendAndLogError("Error preparing statement: " + statement, e);
            return true;
        }
        PostgreSQLPacket buffer = new PostgreSQLPacket(16);
        buffer.setCommand('1');
        buffer.write(output);
        return true;
    }

    /**
     * Preprocess a query.
     * 
     * @param queryToPreprocess the query to preprocess
     * @return true if no need for further processing, false if need further
     *         processing
     * @throws IOException if an error happens when sending the result packet
     */
    private boolean preprocessQuery(String queryToPreprocess) throws IOException {
        if (!isValid(connection)) {
            if (!config.isAutoReconnect() || !tryReconnect()) {
                sendInvalidConnectionError();
                return true;
            }
        }
        String query = queryToPreprocess.trim().toUpperCase();
        if (query.startsWith("DEALLOCATE")) {
            String name = query.substring(11).trim();
            if (name.startsWith("PREPARE")) name = name.substring(8).trim();
            name = name.toLowerCase();
            if (!preparedStatement.deletePreparedStatement(name)) logger.warn("DEALLOCATE failed: prepared statement name \"" + name + "\" not found!");
            sendUpdateCount(0, query);
            sendReadyForQuery();
            return true;
        }
        if (query.startsWith("BEGIN") || query.startsWith("START TRANSACTION")) {
            try {
                if (config.getManageTransactionsLocally()) {
                    if (logger.isDebugEnabled()) logger.debug("Begining transaction (autocommit was:" + connection.getAutoCommit() + ")");
                    connection.setAutoCommit(false);
                }
                sendUpdateCount(0, query);
                sendReadyForQuery();
            } catch (SQLException e) {
                sendAndLogError("Error while begining transaction", e);
            }
            return true;
        }
        if (query.startsWith("UNLISTEN") || query.startsWith("NOTIFY") || query.startsWith("LISTEN")) {
            sendUpdateCount(0, query);
            sendReadyForQuery();
            return true;
        }
        if (config.getManageTransactionsLocally()) {
            if (query.startsWith("COMMIT") || query.startsWith("END")) {
                try {
                    if (logger.isDebugEnabled()) logger.debug("Commiting transaction and switching back to autocommit (autocommit was:" + connection.getAutoCommit() + ")");
                    connection.commit();
                    connection.setAutoCommit(true);
                    sendUpdateCount(0, query);
                    sendReadyForQuery();
                } catch (SQLException e) {
                    sendAndLogError("Error while commiting transaction", e);
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException ignored) {
                    }
                }
                return true;
            }
            if (query.startsWith("ROLLBACK") && !query.matches("ROLLBACK\\s+TO.*")) {
                try {
                    if (logger.isDebugEnabled()) logger.debug("Rolling-back transaction and switching back to autocommit (autocommit was:" + connection.getAutoCommit() + ")");
                    connection.rollback();
                    connection.setAutoCommit(true);
                    sendUpdateCount(0, query);
                    sendReadyForQuery();
                } catch (SQLException e) {
                    sendAndLogError("Error while rolling-back transaction", e);
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException ignored) {
                    }
                }
                return true;
            }
        }
        if (query.startsWith("PREPARE")) {
            query = query.substring(7).trim();
            int parenthesisPos = query.indexOf('(');
            int whiteSpacePos = query.length() + 1;
            for (int i = 0; i < query.length(); i++) {
                if (Character.isWhitespace(query.charAt(i))) {
                    whiteSpacePos = i;
                    break;
                }
            }
            String pStmtName = query.substring(0, (parenthesisPos < whiteSpacePos) ? parenthesisPos : whiteSpacePos).toLowerCase();
            query = query.substring(query.indexOf(" AS ") + 4).trim();
            try {
                preparedStatement.createPreparedStatement(connection, pStmtName, Utils.replaceParametersWithQuestionMarks(query));
            } catch (SQLException e) {
                sendAndLogError("Error preparing statement: " + query, e);
            }
            PostgreSQLPacket buffer = new PostgreSQLPacket(8);
            buffer.setCommand('C');
            buffer.putStringNullTerminated("PREPARE");
            buffer.write(output);
            sendReadyForQuery();
            return true;
        }
        return false;
    }

    /**
     * Process a query. Execute the request and send back to the frontend the
     * result set.
     * 
     * @param query the query to execute
     * @return false to close the connection and true to keep it
     * @throws IOException if a read or write operation fails
     */
    private boolean handleQueryCommand(String query) throws IOException {
        logger.trace("Processing query: " + query);
        if (processEscapes(query)) {
            return true;
        }
        if (config.getIgnoreSQLComments()) query = Utils.removeComments(query);
        if (preprocessQuery(query) == true) {
            logger.trace("Query was preprocessed. No need for more processing. Returning.");
            return true;
        }
        Connection connectionToUse = getConnectionForRequest(query, false);
        if (connectionToUse == null) {
            sendInvalidConnectionError();
            return false;
        }
        Statement statement = null;
        try {
            statement = connectionToUse.createStatement();
            statement.setFetchSize(config.getFetchSize());
            boolean done;
            int updateCount;
            boolean isResultSet = statement.execute(query);
            do {
                if (isResultSet) {
                    updateCount = 0;
                    ResultSet resultSet = statement.getResultSet();
                    sendResultSet(resultSet);
                    resultSet.close();
                } else {
                    updateCount = statement.getUpdateCount();
                    sendUpdateCount(updateCount, query.trim().toUpperCase());
                }
                isResultSet = statement.getMoreResults();
                done = !isResultSet && (updateCount == -1);
            } while (!done);
        } catch (SQLException e) {
            sendAndLogError("Error processing query: " + query, e);
            wasErrorInTransaction = true;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                    statement = null;
                }
            } catch (SQLException e) {
                statement = null;
            }
            if (connectionToUse != connection) {
                if (directConnections.elementAt(directConnectionsIndex) != connectionToUse) logger.error("Big issue here: direct connection used is not the expected one!"); else {
                    try {
                        connectionToUse.close();
                    } catch (SQLException e) {
                    }
                    logger.debug("Connection #" + directConnectionsIndex + " closed");
                }
            }
        }
        sendReadyForQuery();
        return true;
    }

    /**
     * Send to the client the update count.
     * 
     * @param updateCount the update count
     * @param uppercaseQuery the query in upper case that generated this update
     *            count
     * @throws IOException if writing to the buffer fails
     */
    private void sendUpdateCount(int updateCount, String uppercaseQuery) throws IOException {
        if (updateCount == -1) {
            return;
        }
        String command;
        if (uppercaseQuery.startsWith("INSERT")) {
            command = "INSERT 0 " + updateCount;
        } else if (uppercaseQuery.startsWith("DELETE")) {
            command = "DELETE " + updateCount;
        } else if (uppercaseQuery.startsWith("UPDATE")) {
            command = "UPDATE " + updateCount;
        } else if (uppercaseQuery.startsWith("MOVE")) {
            command = "MOVE " + updateCount;
        } else if (uppercaseQuery.startsWith("FETCH")) {
            command = "FETCH " + updateCount;
        } else if (uppercaseQuery.startsWith("COPY")) {
            command = "COPY " + updateCount;
        } else if (uppercaseQuery.startsWith("BEGIN")) {
            command = "BEGIN";
            wasErrorInTransaction = false;
        } else if (uppercaseQuery.startsWith("END")) {
            if (!wasErrorInTransaction) {
                command = "COMMIT";
            } else {
                command = "ROLLBACK";
            }
        } else {
            int i = uppercaseQuery.indexOf(" ");
            if (i == -1) {
                command = uppercaseQuery;
            } else {
                int j = uppercaseQuery.indexOf(" ", i + 1);
                if (j == -1) {
                    command = uppercaseQuery.substring(0, i);
                } else {
                    command = uppercaseQuery.substring(0, j);
                }
            }
        }
        PostgreSQLPacket buffer = new PostgreSQLPacket(16);
        buffer.setCommand('C');
        buffer.putStringNullTerminated(command);
        buffer.write(output);
    }

    /**
     * Send to the client the result set.
     * 
     * @param resultSet the result set
     * @throws SQLException if retrieving result set meta data fails
     * @throws IOException if writing to the buffer fails
     */
    private void sendResultSet(ResultSet resultSet) throws SQLException, IOException {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columns = resultSetMetaData.getColumnCount();
        PostgreSQLPacket buffer = new PostgreSQLPacket(16);
        buffer.setCommand('T');
        buffer.putInt16(columns);
        for (int i = 1; i <= columns; i++) {
            buffer.putStringNullTerminated(resultSetMetaData.getColumnLabel(i));
            buffer.putInt32(0);
            buffer.putInt16(i);
            buffer.putInt32(JDBCToPostgreSQLType.getPostgreSQLType(resultSetMetaData.getColumnTypeName(i)));
            buffer.putInt16(resultSetMetaData.getColumnDisplaySize(i));
            buffer.putInt32(-1);
            buffer.putInt16(0);
        }
        buffer.write(output);
        while (resultSet.next()) {
            buffer.setCommand('D');
            buffer.putInt16(columns);
            for (int i = 1; i <= columns; i++) {
                String returnValue = null;
                switch(resultSetMetaData.getColumnType(i)) {
                    case Types.TINYINT:
                        returnValue = Byte.toString(resultSet.getByte(i));
                        if (resultSet.wasNull()) returnValue = null;
                        break;
                    case Types.SMALLINT:
                        returnValue = Short.toString(resultSet.getShort(i));
                        if (resultSet.wasNull()) returnValue = null;
                        break;
                    case Types.INTEGER:
                        returnValue = Integer.toString(resultSet.getInt(i));
                        if (resultSet.wasNull()) returnValue = null;
                        break;
                    case Types.BIGINT:
                        returnValue = Long.toString(resultSet.getLong(i));
                        if (resultSet.wasNull()) returnValue = null;
                        break;
                    case Types.REAL:
                        returnValue = Float.toString(resultSet.getFloat(i));
                        if (resultSet.wasNull()) returnValue = null;
                        break;
                    case Types.FLOAT:
                        returnValue = Double.toString(resultSet.getDouble(i));
                        if (resultSet.wasNull()) returnValue = null; else if (returnValue.endsWith(".0")) returnValue = returnValue.substring(0, returnValue.length() - 2);
                        break;
                    case Types.DOUBLE:
                        returnValue = Double.toString(resultSet.getDouble(i));
                        if (resultSet.wasNull()) returnValue = null; else if (returnValue.endsWith(".0")) returnValue = returnValue.substring(0, returnValue.length() - 2);
                        break;
                    case Types.DECIMAL:
                    case Types.NUMERIC:
                        BigDecimal bigDecimal = resultSet.getBigDecimal(i);
                        if (bigDecimal != null) returnValue = bigDecimal.toPlainString();
                        break;
                    case Types.BIT:
                        if ("bool".equalsIgnoreCase((resultSetMetaData.getColumnTypeName(i)))) {
                            Boolean b = resultSet.getBoolean(i);
                            returnValue = resultSet.wasNull() ? null : b ? "t" : "f";
                        } else {
                            returnValue = resultSet.getString(i);
                        }
                        break;
                    case Types.DATE:
                        java.sql.Date jdbcDate = resultSet.getDate(i);
                        if (jdbcDate != null) {
                            returnValue = formatDate(new Date(jdbcDate.getTime()), "yyyy-MM-dd");
                        }
                        break;
                    case Types.TIMESTAMP:
                        Timestamp ts = resultSet.getTimestamp(i);
                        if (ts != null) {
                            if (ts.getTime() == JDBC_DATE_INFINITY) returnValue = POSTGRES_DATE_INFINITY; else if (ts.getTime() == JDBC_DATE_MINUS_INFINITY) returnValue = POSTGRES_DATE_MINUS_INFINITY; else {
                                returnValue = formatDate(new Date(ts.getTime()), config.getTimestampFormat());
                            }
                        }
                        break;
                    default:
                        returnValue = resultSet.getString(i);
                        break;
                }
                buffer.putStringLenEncoded(returnValue);
            }
            buffer.write(output);
        }
        buffer.setCommand('C');
        buffer.putStringNullTerminated("SELECT");
        buffer.write(output);
    }

    /**
     * Formats the given date into a string following the given pattern, adding
     * era if before common
     * 
     * @param d the date to convert
     * @param formatPattern the desired pattern to apply
     * @return a string representing the given date formatted according to the
     *         given pattern
     */
    private String formatDate(Date d, String formatPattern) {
        String returnValue;
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(d);
        if (c.get(Calendar.ERA) == GregorianCalendar.BC) formatPattern += " G";
        DateFormat format = new java.text.SimpleDateFormat(formatPattern);
        returnValue = format.format(d);
        return returnValue;
    }

    /**
     * Send to the frontend ready for query packet.
     * 
     * @throws IOException if the write operation fails
     */
    private void sendReadyForQuery() throws IOException {
        boolean inTransaction = false;
        try {
            if (connection != null) {
                inTransaction = !connection.getAutoCommit();
            }
        } catch (SQLException e) {
        }
        PostgreSQLPacket buffer = new PostgreSQLPacket(16);
        buffer.setCommand('Z');
        buffer.putChar(inTransaction ? 'T' : 'I');
        buffer.write(output);
        output.flush();
    }

    /**
     * To be called when a connection is found broken - sends appropriate error
     */
    private void sendInvalidConnectionError() throws IOException {
        String error = "Data source connection is invalid. Aborting all communications";
        logger.error(error);
        sendErrorResponse(null, error);
        close();
    }

    /**
     * Send the error message back to the frontend and log it.
     * 
     * @param msg the message to log
     * @param e the exception occurred
     * @throws IOException if failing to write to the socket the error message
     */
    private void sendAndLogError(String msg, SQLException e) throws IOException {
        Exception last = e;
        Throwable next = e.getCause();
        while (next != null) {
            last = (Exception) next;
            next = next.getCause();
        }
        logger.error(msg, e);
        sendErrorResponse(e.getSQLState(), last.getMessage());
    }

    /**
     * Sends an error message to the frontend.<br>
     * Severity will be parsed from the message. If the message is null or does
     * not specify a severity, the default "ERROR" severity is sent
     * 
     * @param sqlState the 5 digit SQL state
     * @param message the human readable error message
     * @throws IOException if a read or write operation fails
     */
    void sendErrorResponse(String sqlState, String message) throws IOException {
        String severity = "ERROR";
        if ((message != null)) {
            if (message.startsWith("ERROR:")) {
                message = message.substring(message.indexOf(" ") + 1);
            } else if (message.startsWith("FATAL:")) {
                message = message.substring(message.indexOf(" ") + 1);
                severity = "FATAL";
            }
        }
        PostgreSQLPacket error = new PostgreSQLPacket(16);
        error.setCommand('E');
        error.putChar('S');
        error.putStringNullTerminated(severity);
        error.putChar('C');
        error.putStringNullTerminated(sqlState == null ? "XX000" : sqlState);
        error.putChar('M');
        error.putStringNullTerminated(message == null ? "..." : message);
        error.putChar((char) 0);
        error.write(output);
        output.flush();
        if (config.getManageTransactionsLocally()) {
            try {
                if (connection != null && !connection.isClosed()) connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Check if the passwords matches. Encode the plain password with MD5 and
     * compare it with the scrambled password. This code was copied and adapted
     * from the PostgreSQL JDBC driver code (MD5Digest.java)
     * 
     * @param user the user
     * @param plainPassword the plain password
     * @param scrambledPassword the scrambled password
     * @return true if match otherwise false
     */
    private boolean passwordMatches(String user, String plainPassword, String scrambledPassword) {
        MessageDigest md;
        byte[] temp_digest, pass_digest;
        byte[] hex_digest = new byte[35];
        byte[] scrambled = scrambledPassword.getBytes();
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(plainPassword.getBytes("US-ASCII"));
            md.update(user.getBytes("US-ASCII"));
            temp_digest = md.digest();
            Utils.bytesToHex(temp_digest, hex_digest, 0);
            md.update(hex_digest, 0, 32);
            md.update(salt.getBytes());
            pass_digest = md.digest();
            Utils.bytesToHex(pass_digest, hex_digest, 3);
            hex_digest[0] = (byte) 'm';
            hex_digest[1] = (byte) 'd';
            hex_digest[2] = (byte) '5';
            for (int i = 0; i < hex_digest.length; i++) {
                if (scrambled[i] != hex_digest[i]) {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return true;
    }

    private boolean verifyUsernameAndPassword(String submittedUserName, String submittedPassword) throws IOException {
        String hasPassword = (submittedPassword.length() == 0 ? "with no password." : "using a password.");
        UserMapItem item = config.getUserMap().getConfigItem(submittedUserName, getConnectedFromHost());
        if (item.getUserName().length() == 0) {
            String reason = String.format("Authorization failed for user '%s',  %s\n" + "There is currently no entry in the user.map for this user.\n" + "Contact your Tungsten system administrator.", submittedUserName, hasPassword);
            logger.warn(reason);
            throw new IOException(reason);
        }
        String userMapPassword = item.getPassword();
        if (!passwordMatches(submittedUserName, userMapPassword, submittedPassword)) {
            String reason = String.format("Authorization failed for user '%s',  %s\n" + "The password for this user, in the user.map, file does not match this password.\n" + "Contact your Tungsten system administrator.", submittedUserName, hasPassword);
            logger.warn(reason);
            logger.error("Password does NOT match!");
            sendErrorResponse("08004", reason);
            throw new IOException(reason);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.continuent.myosotis.protocol.ProtocolHandler#sendTextMessage(java.lang.String)
     */
    protected void sendTextMessage(String message) throws IOException {
        PostgreSQLPacket buffer = new PostgreSQLPacket(16);
        buffer.setCommand('T');
        buffer.putInt16(1);
        buffer.putStringNullTerminated("message");
        buffer.putInt32(0);
        buffer.putInt16(1);
        buffer.putInt32(JDBCToPostgreSQLType.getPostgreSQLType("varchar"));
        buffer.putInt16(message.length());
        buffer.putInt32(-1);
        buffer.putInt16(0);
        buffer.write(output);
        buffer.setCommand('D');
        buffer.putInt16(1);
        buffer.putStringLenEncoded(message);
        buffer.write(output);
        buffer.setCommand('C');
        buffer.putStringNullTerminated("TUNGSTEN");
        buffer.write(output);
        sendReadyForQuery();
    }

    @Override
    protected boolean tryReconnect() {
        logger.warn("Data source connection lost - reconnecting");
        try {
            connectToDatabase(userName, databaseName, true);
        } catch (SQLException sqle) {
            return false;
        }
        if (!isValid(connection)) {
            return false;
        }
        return true;
    }
}
