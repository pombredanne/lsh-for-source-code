package org.continuent.myosotis.protocol.mysql;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.continuent.myosotis.App;
import org.continuent.myosotis.ServerThread;
import org.continuent.myosotis.configuration.Configuration;
import org.continuent.myosotis.configuration.UserMapItem;
import org.continuent.myosotis.protocol.ProtocolHandler;
import com.continuent.tungsten.commons.mysql.MySQLConstants;
import com.continuent.tungsten.commons.mysql.MySQLIOs;
import com.continuent.tungsten.commons.mysql.MySQLPacket;
import com.continuent.tungsten.commons.mysql.Utils;

/**
 * This class handle all the MySQL protocol logic.
 * 
 * @author <a href="mailto:csaba.simon@continuent.com">Csaba Simon</a>
 * @author <a href="gilles.rayrat@continuent.com">Gilles Rayrat</a>
 */
public class MySQLProtocolHandler extends ProtocolHandler {

    private static final Logger logger = Logger.getLogger(MySQLProtocolHandler.class);

    private static final int WAITING = 0;

    private static final int SENTSERVERGREETING = 1;

    private static final int COMMANDPROCESSING = 2;

    private int state = WAITING;

    private int clientFlags;

    private int maximumPacketLength;

    private int characterSet;

    private byte[] password;

    private short serverStatus = MySQLConstants.SERVER_STATUS_AUTOCOMMIT;

    private byte packetSequence;

    private final OutputStream out;

    private final String randomSeed;

    /** This is where prepared statements are kept between two calls */
    private HashMap<Long, PreparedStatementWithParameterInfo> preparedStatements;

    /** Pass-through mode: keep track of connections given a statement id */
    private HashMap<Long, Connection> ptPreparedStatementConnections;

    /** Index for preparedStatements map */
    private long pstmtIndex;

    private String lastQuery = null;

    private final int signMask = 1 << 15;

    private final int typeMask = ~(signMask);

    private static final String ZERO_DATE = new String("0000-00-00");

    private static final String ZERO_TIMESTAMP = new String("0000-00-00 00:00:00");

    private static final String SQL_STATE_ILLEGAL_ARGUMENT = new String("S1009");

    private static final String BEGIN_PATTERN_STRING = "^(begin|start\\s+transaction).*";

    private static final String COMMIT_PATTERN_STRING = "^commit.*";

    private static final String ROLLBACK_PATTERN_STRING = "^rollback.*";

    private static final String SET_AUTOCOMMIT_1_PATTERN_STRING = "^set\\s+autocommit\\s*=\\s*(1|true|on).*";

    private static final String SET_AUTOCOMMIT_0_PATTERN_STRING = "^set\\s+autocommit\\s*=\\s*(0|false|off).*";

    private static final String IMPLICIT_COMMIT_PATTERN_STRING = "^((create|alter|drop|rename|truncate\\s+table|user)|(create|alter|drop\\s+function|procedure|view)|(create|drop\\s+database|trigger)|((un)?lock\\s+tables)|(create|drop\\s+index)|(load\\s+(master\\s+data|data\\s+infile))|(begin|start\\s+transaction)|(set\\s+autocommit\\s*(=\\s*)?(0|false|off))).*";

    private static final String CREATE_TEMPORARY_TABLE_PATTERN_STRING = "^create\\s+temporary\\s+table.*";

    private static final String LOAD_DATA_LOCAL_INFILESTRING = "^load\\s+data\\s+local\\s+infile.*";

    private static final Pattern BEGIN_PATTERN = Pattern.compile(BEGIN_PATTERN_STRING, Pattern.DOTALL);

    private static final Pattern COMMIT_PATTERN = Pattern.compile(COMMIT_PATTERN_STRING, Pattern.DOTALL);

    private static final Pattern ROLLBACK_PATTERN = Pattern.compile(ROLLBACK_PATTERN_STRING, Pattern.DOTALL);

    private static final Pattern SET_AUTOCOMMIT_1 = Pattern.compile(SET_AUTOCOMMIT_1_PATTERN_STRING, Pattern.DOTALL);

    private static final Pattern SET_AUTOCOMMIT_0 = Pattern.compile(SET_AUTOCOMMIT_0_PATTERN_STRING, Pattern.DOTALL);

    private static final Pattern IMPLICIT_COMMIT_PATTERN = Pattern.compile(IMPLICIT_COMMIT_PATTERN_STRING, Pattern.DOTALL);

    private static final Pattern CREATE_TEMPORARY_TABLE_PATTERN = Pattern.compile(CREATE_TEMPORARY_TABLE_PATTERN_STRING, Pattern.DOTALL);

    private static final Pattern LOAD_DATA_LOCAL_INFILE_PATTERN = Pattern.compile(LOAD_DATA_LOCAL_INFILESTRING, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final String TUNGSTEN_COMMAND_INTRODUCER = "tungsten";

    /**
     * Creates a new <code>MySQLProtocolHandler</code> object
     * 
     * @param connectedFromHost host from which the client initiated connection
     * @param config the configuration file
     * @param out the output stream where we write the response
     */
    public MySQLProtocolHandler(String connectedFromHost, Configuration config, OutputStream out, ServerThread serverThread) {
        super(connectedFromHost, config, serverThread);
        this.out = out;
        randomSeed = Utils.generateRandomString(20);
        preparedStatements = new HashMap<Long, PreparedStatementWithParameterInfo>();
        ptPreparedStatementConnections = new HashMap<Long, Connection>();
        pstmtIndex = 0;
    }

    /**
     * The state machine. Returns true if the connection must be closed.
     * 
     * @param packet the MySQL packet to process
     * @return true if the connection must be closed, false otherwise
     */
    public boolean processClientPacket(MySQLPacket packet) {
        boolean finishing = false;
        try {
            if (state == WAITING) {
                sendGreetingPacket();
                state = SENTSERVERGREETING;
            } else if (state == SENTSERVERGREETING) {
                sendAuthenticationPacket(packet);
                state = COMMANDPROCESSING;
            } else if (state == COMMANDPROCESSING) {
                finishing = processCommand(packet);
                if (finishing) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Client properly closed connection - quitting");
                    }
                    close();
                }
            }
        } catch (IOException e) {
            finishing = true;
            logger.warn("Closing connection due to IOException", e);
            if (lastQuery != null) {
                String message = String.format("Query in progress prior to exception:\n%s", lastQuery);
                logger.warn(message);
            }
            close();
        } finally {
            try {
                if (out != null) {
                    out.flush();
                }
            } catch (IOException e) {
            }
        }
        return finishing;
    }

    /**
     * Based on the 'command' field select what kind of processing needs to be
     * done.
     * 
     * @param packet the MySQL packet
     * @return true to close the connection (after command QUIT)
     * @throws IOException if an input/output error occurs sending the response
     *             back to client
     */
    private boolean processCommand(MySQLPacket packet) throws IOException {
        byte command = packet.getByte();
        packetSequence = packet.getPacketNumber();
        if (logger.isDebugEnabled()) {
            logger.debug("Received MySQL command " + MySQLConstants.commandToString(command));
        }
        switch(command) {
            case MySQLConstants.COM_SLEEP:
                sendErrorPacket(MySQLConstants.ER_NOT_SUPPORTED_YET, "42000", "Command not supported!");
                break;
            case MySQLConstants.COM_QUIT:
                sendOkPacket(0, 0, 0);
                return true;
            case MySQLConstants.COM_INIT_DB:
                processChangeUser(userName, password, packet.getString());
                break;
            case MySQLConstants.COM_CHANGE_USER:
                processChangeUser(packet.getString(), packet.getLenEncodedBytes(), packet.getString());
                break;
            case MySQLConstants.COM_QUERY:
                processQuery(packet.getString(), false, packet, config.isAutoReconnect());
                break;
            case MySQLConstants.COM_FIELD_LIST:
                processQuery("SELECT * FROM " + packet.getString() + " LIMIT 0", true, packet, config.isAutoReconnect());
                break;
            case MySQLConstants.COM_STATISTICS:
                processStatistics();
                break;
            case MySQLConstants.COM_PING:
                sendPong();
                break;
            case MySQLConstants.COM_PROCESS_INFO:
                processQuery("show processlist", true, packet, config.isAutoReconnect());
                break;
            case MySQLConstants.COM_CREATE_DB:
            case MySQLConstants.COM_DROP_DB:
            case MySQLConstants.COM_REFRESH:
            case MySQLConstants.COM_SHUTDOWN:
            case MySQLConstants.COM_CONNECT:
            case MySQLConstants.COM_PROCESS_KILL:
            case MySQLConstants.COM_DEBUG:
            case MySQLConstants.COM_TIME:
            case MySQLConstants.COM_DELAYED_INSERT:
            case MySQLConstants.COM_BINLOG_DUMP:
            case MySQLConstants.COM_TABLE_DUMP:
            case MySQLConstants.COM_CONNECT_OUT:
            case MySQLConstants.COM_REGISTER_SLAVE:
                logger.warn("Unsupported command #" + command + ".");
                sendErrorPacket(MySQLConstants.ER_NOT_SUPPORTED_YET, "42000", "Command not supported!");
                break;
            case MySQLConstants.COM_STMT_PREPARE:
                prepareStatement(packet.getString(), packet);
                break;
            case MySQLConstants.COM_STMT_EXECUTE:
                preparedStatementExecute(packet);
                break;
            case MySQLConstants.COM_STMT_RESET:
                resetPreparedStatement(packet.getUnsignedInt32(), true, packet);
                break;
            case MySQLConstants.COM_STMT_CLOSE:
                closePreparedStatement(packet.getUnsignedInt32(), packet);
                break;
            case MySQLConstants.COM_STMT_SEND_LONG_DATA:
                storeLongData(packet);
                break;
            case MySQLConstants.COM_STMT_FETCH:
                fetchResultRows(packet);
                break;
            case MySQLConstants.COM_SET_OPTION:
                logger.warn("Command SET_OPTION has no effect - you should fix the datasource url option \"allowMultipleQueries\" instead");
                sendEofMessage(0);
                break;
        }
        return false;
    }

    /**
     * Responds to a ping command. Answers OK if select(1) could be successfully
     * executed on the cluster, error otherwise
     */
    private void sendPong() throws IOException {
        if (connection == null) {
            sendErrorPacket(MySQLConstants.ER_NO, "HY000", "Connection is null");
        } else {
            try {
                Statement st = connection.createStatement();
                if (st == null) sendErrorPacket(MySQLConstants.ER_NO, "HY000", "Could not create test statement"); else {
                    st.executeQuery("SELECT 1");
                    st.close();
                    sendOkPacket(0, 0, 0);
                }
            } catch (SQLException e) {
                sendErrorPacket(e.getErrorCode(), e.getSQLState(), getOriginalExceptionFrom(e).getMessage());
            }
        }
    }

    /**
     * Handle the COM_INIT_DB command. Connects to the database with the
     * specified user name. It checks the password.
     * 
     * @param userName the user name
     * @param password the password
     * @param databaseName the database name
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void processChangeUser(String userName, byte[] password, String databaseName) throws IOException {
        if (logger.isTraceEnabled()) logger.trace("Change user: " + userName + " database: " + databaseName);
        if (userName.length() == 0) {
            userName = this.userName;
        }
        if (databaseName.length() == 0) {
            databaseName = this.databaseName;
        }
        verifyUsernameAndPassword(userName, password);
        try {
            connectToDatabase(userName, databaseName, true);
            serverStatus = MySQLConstants.SERVER_STATUS_AUTOCOMMIT;
            sendOkPacket(0, 0, 0);
        } catch (SQLException s) {
            try {
                connectToDatabase(this.userName, this.databaseName, true);
            } catch (SQLException ignored) {
            }
            logger.error("Connecting to " + databaseName + " with user " + userName + " failed.");
            sendErrorPacket(MySQLConstants.ER_BAD_DB_ERROR, "42000", "Unknown database '" + databaseName + "'");
        }
    }

    /**
     * Handle the COM_STATISTICS command. Currently only the uptime field is
     * filled with data.
     * 
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void processStatistics() throws IOException {
        String s = String.format("Uptime: %d  Threads: %d  Questions: %d  Slow queries: %d  Opens: %d  Flush tables: %d  Open tables: %d  Queries per second avg: %.3f", (System.currentTimeMillis() - App.startTime) / 1000, 0, 0, 0, 0, 0, 0, 0.0);
        MySQLPacket buf = new MySQLPacket(128, ++packetSequence);
        buf.putString(s);
        buf.write(out);
    }

    /**
     * Handles the COM_QUERY command. The query will be pre-processed to detect
     * whether special handling must be made. Otherwise, the query will be
     * executed on the database server
     * 
     * @param query the query to process
     * @param fieldListOnly is true if we are sending only the field
     *            descriptions, is false if we are sending the data also
     * @param reconnectIfNeeded whether to reconnect upon I/O error
     * @return the connection used for running this query
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private Connection processQuery(String query, boolean fieldListOnly, MySQLPacket incomingPacket, boolean reconnectIfNeeded) throws IOException {
        boolean largePacket = incomingPacket.getDataLength() >= MySQLPacket.MAX_LENGTH;
        String queryForDisplay = null;
        if (query.length() > REQUEST_DISPLAY_MAX_LENGTH) queryForDisplay = query.substring(0, REQUEST_DISPLAY_MAX_LENGTH); else queryForDisplay = query;
        if (logger.isDebugEnabled()) {
            logger.debug("Processing query: " + queryForDisplay);
        }
        lastQuery = queryForDisplay;
        Connection connectionToUse = connection;
        if (!largePacket && processEscapes(query) == true) {
            if (logger.isTraceEnabled()) {
                logger.trace("Escape was processed. No need for more processing. Returning.");
            }
            return connectionToUse;
        }
        connectionToUse = getConnectionForRequest(query, largePacket);
        if (connectionToUse == null) {
            String message = "Tungsten detected that MySQL Server has gone away. No connection available.";
            sendErrorPacket(MySQLConstants.ER_SERVER_GONE_AWAY, "HY000", message);
            logger.warn("Connection failed because no connection is available.");
            throw new IOException(message);
        }
        Statement statement = null;
        try {
            if (config.getIgnoreSQLComments()) query = Utils.removeComments(query);
            if ("".equals(query)) {
                sendOkPacket(0, 0, 0);
                return connectionToUse;
            }
            if (preprocessQuery(query) == true) {
                connectionToUse = connection;
                logger.trace("Query was preprocessed. No need for more processing. Returning.");
                return connectionToUse;
            }
            if (config.isPassThroughMode() && MySQLIOs.connectionIsCompatible(connectionToUse)) {
                if (logger.isTraceEnabled()) logger.trace("passThrough mode on - executing query on server");
                boolean isLoadDataLocalInfile = LOAD_DATA_LOCAL_INFILE_PATTERN.matcher(query).matches();
                execQueryOnServerAndStreamResult(fieldListOnly, incomingPacket, connectionToUse, false, isLoadDataLocalInfile);
            } else {
                if (largePacket) {
                    incomingPacket.readRemainingPackets();
                    incomingPacket.reset();
                    incomingPacket.getByte();
                    incomingPacket.getPacketNumber();
                    query = incomingPacket.getString();
                }
                if (logger.isTraceEnabled()) logger.trace("passThrough mode off - executing jdbc query");
                statement = connectionToUse.createStatement();
                statement.setEscapeProcessing(false);
                statement.setFetchSize(config.getFetchSize());
                udpateServerStatus(connectionToUse.getAutoCommit());
                if (statement.execute(query, Statement.RETURN_GENERATED_KEYS)) {
                    sendAllResultSets(statement, fieldListOnly, false);
                } else {
                    udpateServerStatus(connectionToUse.getAutoCommit());
                    int updateCount = statement.getUpdateCount();
                    if (updateCount == -1) {
                        updateCount = 0;
                    }
                    sendOkPacket(updateCount, getLastInsertId(statement), 0);
                }
            }
            if (connectionToUse.getClass() == theTSRConnectionClazz) {
                try {
                    updateHighWaterMethod.invoke(connectionToUse);
                } catch (InvocationTargetException ite) {
                }
            }
        } catch (SQLException e) {
            if (reconnectIfNeeded && MySQLConstants.SQL_STATE_COMMUNICATION_ERROR.equals(e.getSQLState())) {
                connectionToUse = reconnectAndRetryProcessQuery(query, fieldListOnly, incomingPacket);
            } else {
                sendErrorPacket(e.getErrorCode(), e.getSQLState(), getOriginalExceptionFrom(e).getMessage());
                logger.error("Error processing query: " + queryForDisplay, e);
            }
        } catch (SocketTimeoutException s) {
            if (connectionToUse != null) {
                String message = "Tungsten detected that MySQL server has gone away. Socket timeout.";
                sendErrorPacket(MySQLConstants.ER_SERVER_GONE_AWAY, "HY000", message);
                logger.warn(String.format("Exception caused close of connection: %s", s));
                throw new IOException(message);
            }
        } catch (IOException ioe) {
            if (reconnectIfNeeded) {
                connectionToUse = reconnectAndRetryProcessQuery(query, fieldListOnly, incomingPacket);
            } else {
                throw ioe;
            }
        } catch (Exception e) {
            String message = "Unexpected exception: " + e.getMessage();
            sendErrorPacket(MySQLConstants.ER_SERVER_GONE_AWAY, "HY000", message);
            logger.error("Error processing query: " + queryForDisplay, e);
            throw new IOException(message);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                    statement = null;
                }
            } catch (SQLException e) {
                statement = null;
            }
            if (connection != null && connectionToUse != connection) {
                if (directConnections.elementAt(directConnectionsIndex) != connectionToUse) logger.error("Big issue here: direct connection used is not the expected one!"); else {
                    try {
                        connectionToUse.close();
                    } catch (SQLException e) {
                    }
                    if (logger.isTraceEnabled()) logger.trace("Connection #" + directConnectionsIndex + " closed");
                }
            }
        }
        return connectionToUse;
    }

    private Connection reconnectAndRetryProcessQuery(String query, boolean fieldListOnly, MySQLPacket incomingPacket) throws IOException {
        Connection connectionToUse;
        if (!tryReconnect()) {
            logger.warn("Reconnection failed - giving up");
            throw new IOException("Unable to reconnect to server");
        }
        connectionToUse = processQuery(query, fieldListOnly, incomingPacket, false);
        return connectionToUse;
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

    /**
     * Retrieves MySQL server I/Os to execute a query directly on it, then
     * streams the output to the client application without constructing JDBC
     * structures
     * 
     * @param fieldListOnly whether or not the send only the list of fields
     * @param incomingPacket full query packet as received from the client
     * @param connectionToServer jdbc connection to the mysql server
     * @param wasPrepStmt whether this query comes from a prepared statement
     * @throws IOException upon server communication error
     */
    private void execQueryOnServerAndStreamResult(boolean fieldListOnly, MySQLPacket incomingPacket, Connection connectionToServer, boolean wasPrepStmt, boolean isLoadDataLocalInfile) throws SQLException, IOException {
        MySQLIOs ios = MySQLIOs.getMySQLIOs(connectionToServer);
        InputStream mysqlInput = ios.getInput();
        BufferedOutputStream mysqlOutput = ios.getOutput();
        InputStream clientInput = incomingPacket.getInputStream();
        if (mysqlInput == null || mysqlOutput == null) {
            String msg = new String("Unable to get MySQL server I/Os");
            logger.error(msg);
            throw new IOException(msg);
        }
        incomingPacket.preparePacketForStreaming();
        if (logger.isTraceEnabled()) logger.trace("sending packet " + incomingPacket);
        incomingPacket.write(mysqlOutput);
        if (incomingPacket.getDataLength() == MySQLPacket.MAX_LENGTH) {
            while (incomingPacket.getDataLength() == MySQLPacket.MAX_LENGTH) {
                incomingPacket = MySQLPacket.readPacket(clientInput);
                incomingPacket.preparePacketForStreaming();
                incomingPacket.write(mysqlOutput);
            }
        }
        mysqlOutput.flush();
        MySQLPacket streamedPacket = null;
        boolean finished = false;
        int numberOfNoMoreResultsEofs = 0;
        boolean wasMultiRS = false;
        boolean wasRS = false;
        while (!finished) {
            streamedPacket = MySQLPacket.readPacket(mysqlInput);
            if (streamedPacket == null) {
                throw new IOException("Failed to read a packet from the server.");
            }
            streamedPacket.preparePacketForStreaming();
            streamedPacket.write(out);
            if (logger.isTraceEnabled()) {
                logger.trace("Streaming packet:" + streamedPacket);
                if (streamedPacket.isOK()) logger.trace("isOK=" + streamedPacket.isOK()); else if (streamedPacket.isEOF()) logger.trace("isEOF=" + streamedPacket.isEOF());
            }
            if (streamedPacket.getDataLength() == MySQLPacket.MAX_LENGTH) {
                streamedPacket = MySQLPacket.readPacket(mysqlInput);
                if (streamedPacket == null) {
                    throw new IOException("Failed to read a packet from the server.");
                }
                while (streamedPacket.getDataLength() == MySQLPacket.MAX_LENGTH) {
                    streamedPacket.preparePacketForStreaming();
                    streamedPacket.write(out);
                    streamedPacket = MySQLPacket.readPacket(mysqlInput);
                    if (streamedPacket == null) {
                        throw new IOException("Failed to read a packet from the server.");
                    }
                }
                streamedPacket.preparePacketForStreaming();
                streamedPacket.write(out);
                continue;
            }
            if (streamedPacket.isError()) {
                if (logger.isTraceEnabled()) logger.trace("Got error packet - terminating");
                finished = true;
            } else if (streamedPacket.isOK() && (wasMultiRS || !wasRS)) {
                streamedPacket.reset();
                streamedPacket.getByte();
                streamedPacket.readFieldLength();
                streamedPacket.readFieldLength();
                if ((streamedPacket.getShort() & MySQLConstants.SERVER_MORE_RESULTS_EXISTS) == 0) {
                    if (logger.isTraceEnabled()) logger.trace("Got OK packet - terminating");
                    finished = true;
                } else if (logger.isTraceEnabled()) logger.trace("Got OK packet but more results exist");
            } else if (streamedPacket.isEOF()) {
                wasRS = true;
                if (!streamedPacket.hasMoreResults()) {
                    if (wasPrepStmt) {
                        if (streamedPacket.hasCursor()) {
                            logger.fatal("CURSOR EXISTS");
                            finished = true;
                        }
                        if (streamedPacket.hasLastRowSent()) {
                            logger.fatal("LAST ROW SENT");
                            finished = true;
                        }
                    }
                    numberOfNoMoreResultsEofs++;
                    if (logger.isTraceEnabled()) logger.trace("Got EOF packet #" + numberOfNoMoreResultsEofs);
                    if ((fieldListOnly && numberOfNoMoreResultsEofs > 0) || numberOfNoMoreResultsEofs > 1) {
                        if (logger.isTraceEnabled()) logger.trace("fieldListOnly=" + fieldListOnly + "#eof=" + numberOfNoMoreResultsEofs + "- finished");
                        finished = true;
                    }
                } else {
                    if (logger.isTraceEnabled()) logger.trace("Got EOF packet with multiRS - waiting for an OK packet");
                    wasMultiRS = true;
                }
            } else if (isLoadDataLocalInfile) {
                out.flush();
                streamedPacket = MySQLPacket.readPacket(clientInput);
                while (streamedPacket.getDataLength() != 0) {
                    if (logger.isTraceEnabled()) logger.trace("LDLI Streaming packet [client->mysql]:" + streamedPacket);
                    streamedPacket.preparePacketForStreaming();
                    streamedPacket.write(mysqlOutput);
                    streamedPacket = MySQLPacket.readPacket(clientInput);
                }
                if (logger.isTraceEnabled()) logger.trace("LDLI Streaming packet [client->mysql]:" + streamedPacket);
                streamedPacket.preparePacketForStreaming();
                streamedPacket.write(mysqlOutput);
                mysqlOutput.flush();
                streamedPacket = MySQLPacket.readPacket(mysqlInput);
                streamedPacket.preparePacketForStreaming();
                streamedPacket.write(out);
                if (logger.isTraceEnabled()) logger.trace("LDLI Streaming packet [client<-mysql]:" + streamedPacket);
                finished = true;
            }
        }
        out.flush();
    }

    private void fetchResultRows(MySQLPacket incomingPacket) throws IOException {
        long stmtId = incomingPacket.getUnsignedInt32();
        Connection conn = ptPreparedStatementConnections.get(stmtId);
        if (config.isPassThroughMode() && MySQLIOs.connectionIsCompatible(conn)) {
            try {
                execQueryOnServerAndStreamResult(false, incomingPacket, conn, true, false);
            } catch (SQLException e) {
                sendErrorPacket(e.getErrorCode(), e.getSQLState(), getOriginalExceptionFrom(e).getMessage());
                logger.error("Error in fetch() of statement id " + stmtId, e);
            }
        } else {
            if (logger.isTraceEnabled()) logger.trace("STMT_FETCH not implemented yet");
            sendErrorPacket(1295, "HY100", "Prepared statement not supported!");
        }
    }

    /**
     * Iterates through the list of results and send them to client.
     * 
     * @param statement statement used to run the request
     * @param fieldListOnly whether or not the send only the list of fields
     * @param binary whether or not to use binary protocol
     * @throws SQLException if a database error occurs while retrieving the
     *             results
     * @throws IOException if an error occurs on the network (client side)
     */
    private void sendAllResultSets(Statement statement, boolean fieldListOnly, boolean binary) throws SQLException, IOException {
        boolean multiRS = false;
        ResultSet rs = statement.getResultSet();
        boolean finished = statement.getMoreResults(Statement.KEEP_CURRENT_RESULT) == false && statement.getUpdateCount() == -1;
        while (!finished) {
            serverStatus |= MySQLConstants.SERVER_MORE_RESULTS_EXISTS;
            if (rs != null) {
                sendResultSet(rs, fieldListOnly, binary);
            }
            rs = statement.getResultSet();
            finished = statement.getMoreResults(Statement.KEEP_CURRENT_RESULT) == false && statement.getUpdateCount() == -1;
            multiRS = true;
        }
        if (multiRS) {
            if (rs != null) sendResultSet(rs, fieldListOnly, binary);
            serverStatus &= ~(MySQLConstants.SERVER_MORE_RESULTS_EXISTS);
            sendOkPacket(0, 0, 0);
        } else {
            if (rs != null) sendResultSet(rs, fieldListOnly, binary);
        }
    }

    /**
     * Set the server status flag according to the previous state and to the
     * given autocommit parameter
     * 
     * @param isAutocommit whether or not the current connection is in
     *            autocommit mode
     */
    private void udpateServerStatus(boolean isAutocommit) {
        if (serverStatus == MySQLConstants.SERVER_STATUS_AUTOCOMMIT) {
            if (!isAutocommit) {
                serverStatus = (short) 0;
            }
        } else if (serverStatus == MySQLConstants.SERVER_STATUS_IN_TRANS) {
            if (isAutocommit) {
                serverStatus = MySQLConstants.SERVER_STATUS_AUTOCOMMIT;
            }
        } else if (serverStatus == 0) {
            if (isAutocommit) {
                serverStatus = MySQLConstants.SERVER_STATUS_AUTOCOMMIT;
            } else {
                serverStatus = MySQLConstants.SERVER_STATUS_IN_TRANS;
            }
        }
    }

    /**
     * Serializes the given result set onto the network the mysql way.<br>
     * <b>Warning:</b> this function closes the ResultSet after serialization
     * 
     * @param resultSet object to write to the stream
     * @param fieldListOnly when true, only writes the list of fields (used by
     *            COM_FIELD_LIST command)
     * @throws SQLException
     * @throws IOException
     */
    private void sendResultSet(ResultSet resultSet, boolean fieldListOnly, boolean binary) throws SQLException, IOException {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columns = resultSetMetaData.getColumnCount();
        if (!fieldListOnly) {
            sendResultSetHeaderMessage(columns);
        }
        for (int i = 1; i <= columns; i++) {
            int type = resultSetMetaData.getColumnType(i);
            short charset = JDBCMySQLTypeConverter.JDBCTypeToCharset(type);
            short flags = JDBCMySQLTypeConverter.getMySQLFlagsForColumn(resultSetMetaData, i);
            sendResultSetFieldMessage(resultSetMetaData.getTableName(i), resultSetMetaData.getColumnLabel(i), resultSetMetaData.getColumnName(i), charset, resultSetMetaData.getColumnDisplaySize(i), JDBCMySQLTypeConverter.getMySQLType(type), flags, (byte) resultSetMetaData.getScale(i));
        }
        sendEofMessage(0);
        if (!fieldListOnly) {
            while (resultSet.next()) {
                if (binary) sendResultSetRowMessageBinary(columns, resultSet); else sendResultSetRowMessage(columns, resultSet);
            }
            sendEofMessage(0);
        }
        resultSet.close();
    }

    /**
     * Pre-processes a query by analyzing SQL statement and takes appropriate
     * actions when needed. If an action is taken and no further processing is
     * needed, tell it by returning 'true'<br>
     * <b>Warning:</b> the {@link #connection} must be valid!
     * 
     * @param queryToPreprocess the query to preprocess
     * @return true if no need for further processing, false if need further
     *         processing
     * @throws IOException if an error happens when sending the OK packet
     */
    private boolean preprocessQuery(String queryToPreprocess) throws IOException {
        String queryTrimmed = queryToPreprocess.trim();
        String queryTrimmedLowerCase = queryTrimmed.toLowerCase();
        boolean result = false;
        if (queryTrimmedLowerCase.startsWith("use")) {
            int i = queryTrimmed.indexOf(" ");
            int j = queryTrimmed.lastIndexOf(";");
            if (i != -1) {
                String databaseName;
                if (j == -1) {
                    databaseName = queryTrimmed.substring(i + 1);
                } else {
                    databaseName = queryTrimmed.substring(i + 1, j);
                }
                try {
                    connectToDatabase(this.userName, databaseName, true);
                } catch (SQLException e) {
                    try {
                        connectToDatabase(this.userName, this.databaseName, true);
                    } catch (SQLException ignored) {
                    }
                    String errorMessage = String.format("User %s is unable to connect to database %s\n" + "Reason=%s", this.userName, databaseName, e.getLocalizedMessage());
                    logger.error(errorMessage, e);
                    sendErrorPacket(MySQLConstants.ER_BAD_DB_ERROR, "42000", errorMessage);
                }
                serverStatus = MySQLConstants.SERVER_STATUS_AUTOCOMMIT;
                result = true;
                sendOkPacket(0, 0, 0);
            }
        } else if (CREATE_TEMPORARY_TABLE_PATTERN.matcher(queryTrimmedLowerCase).matches()) {
            if (logger.isTraceEnabled()) logger.trace("Temp table creation detected. Setting useMaster=true");
            setUseMaster(true);
        } else if (config.getManageTransactionsLocally() || config.getUseSmartScale()) {
            try {
                if (IMPLICIT_COMMIT_PATTERN.matcher(queryTrimmedLowerCase).matches() && connection.getAutoCommit() == false) {
                    if (logger.isTraceEnabled()) logger.trace("Commiting transaction after implicit commit");
                    connection.commit();
                    setInTransactionBlock(false);
                    connection.setAutoCommit(isAutoCommitMode());
                }
                if (BEGIN_PATTERN.matcher(queryTrimmedLowerCase).matches()) {
                    if (logger.isTraceEnabled()) logger.trace("Begining transaction");
                    connection.setAutoCommit(false);
                    setInTransactionBlock(true);
                    sendOkPacket(0, 0, 0);
                    result = true;
                } else if (COMMIT_PATTERN.matcher(queryTrimmedLowerCase).matches()) {
                    if (logger.isTraceEnabled()) logger.trace("Commiting transaction");
                    if (connection.getAutoCommit() == false) connection.commit(); else if (logger.isDebugEnabled()) logger.debug("Attempt to commit a transaction on an autocommit=true connection. Ignoring");
                    setInTransactionBlock(false);
                    connection.setAutoCommit(isAutoCommitMode());
                    sendOkPacket(0, 0, 0);
                    result = true;
                } else if (ROLLBACK_PATTERN.matcher(queryTrimmedLowerCase).matches()) {
                    if (logger.isTraceEnabled()) logger.trace("Rolling-back transaction");
                    if (connection.getAutoCommit() == false) connection.rollback(); else if (logger.isDebugEnabled()) logger.debug("Attempt to rollback a transaction on an autocommit=true connection. Ignoring");
                    setInTransactionBlock(false);
                    connection.setAutoCommit(isAutoCommitMode());
                    sendOkPacket(0, 0, 0);
                    result = true;
                } else if (SET_AUTOCOMMIT_0.matcher(queryTrimmedLowerCase).matches()) {
                    if (logger.isTraceEnabled()) logger.trace("Setting connection autocommit=0");
                    connection.setAutoCommit(false);
                    setInTransactionBlock(true);
                    setAutoCommitMode(false);
                    sendOkPacket(0, 0, 0);
                    result = true;
                } else if (SET_AUTOCOMMIT_1.matcher(queryTrimmedLowerCase).matches()) {
                    if (logger.isTraceEnabled()) logger.trace("Setting connection autocommit=1");
                    connection.setAutoCommit(true);
                    setInTransactionBlock(false);
                    setAutoCommitMode(true);
                    sendOkPacket(0, 0, 0);
                    result = true;
                }
            } catch (SQLException e) {
                sendErrorPacket(e.getErrorCode(), e.getSQLState(), getOriginalExceptionFrom(e).getMessage());
                logger.error("Error doing " + queryTrimmedLowerCase + " transaction", e);
            }
        }
        return result;
    }

    /**
     * Returns the last insert ID, by using generated keys
     * 
     * @return last insert ID.
     */
    private long getLastInsertId(Statement statement) {
        ResultSet rs;
        long lastId = 0;
        try {
            rs = statement.getGeneratedKeys();
            if (rs != null && rs.next()) {
                lastId = rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.warn("Can't get last insert id.", e);
        }
        return lastId;
    }

    /**
     * Analyzes the given request and takes appropriate action before executing
     * it: when using smart scale, the appropriate read or write channel will be
     * requested. When using direct connection r/w splitting, delegates the
     * analysis to the super class method. This function always returns either a
     * valid connection or null
     * 
     * @param request SQL statement to analyze
     * @param truncatedRequest whether this request is potentially truncated,
     *            which can occur with large packets > 16megs. In such case, the
     *            request will not be fully analyzed and considered as a write
     * @return when using smart scale, the connection with appropriate channel
     *         being set. Otherwise, returns the super class method's result
     */
    protected Connection getConnectionForRequest(String request, boolean truncatedRequest) {
        if (config.getUseSmartScale() == false || (connection != null && connection.getClass() != theTSRConnectionClazz)) {
            return super.getConnectionForRequest(request, truncatedRequest);
        }
        if (!isValid(connection)) {
            if (!config.isAutoReconnect() || !tryReconnect()) return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Using Smart Scale with RW-Splitting mode=" + config.getRWSplittingMode());
            if (request.length() > REQUEST_DISPLAY_MAX_LENGTH) logger.debug("Identifying connection for request '" + request.substring(0, REQUEST_DISPLAY_MAX_LENGTH) + "'"); else logger.debug("Identifying connection for request '" + request + "'");
        }
        Connection connectionToUse = connection;
        try {
            if (inMasterStatementBlock() == true) {
                return connectionToUse;
            }
            String cleanQuery = request.trim().toLowerCase();
            if (cleanQuery.startsWith("show ")) {
                useReadChannelMethod.invoke(connectionToUse);
            } else if (cleanQuery.startsWith("select ")) {
                if (config.getRWSplittingMode() == Configuration.RWSplittingMode.DONT_CHECK_SELECT_FOR_UPDATE) {
                    useReadChannelMethod.invoke(connectionToUse);
                } else if (truncatedRequest) {
                    if (truncatedRequest) logger.debug("Got truncated packet, forcing write channel");
                    useWriteChannelMethod.invoke(connectionToUse);
                } else {
                    if (cleanQuery.contains("for update")) {
                        useWriteChannelMethod.invoke(connectionToUse);
                        if (config.getRWSplittingMode() == Configuration.RWSplittingMode.TEST) {
                            logger.fatal("R/W SPLITTING CHECK MODE - found select for update in the following request: " + request + " DO NOT USE NO-SFU MODE!!!");
                        }
                    } else {
                        useReadChannelMethod.invoke(connectionToUse);
                    }
                }
            } else {
                useWriteChannelMethod.invoke(connectionToUse);
            }
        } catch (Exception e) {
            logger.error("Unable to invoke sql-router channel selection method Cause: ", e);
        }
        return connectionToUse;
    }

    /**
     * Prepares a statement in response to COM_STMT_PREPARE command, then send
     * back to the client:
     * <ol>
     * <li>OK
     * <li>Prepared statement parameters infos
     * <li>ResultSet infos
     * </ol>
     * 
     * @param request statement to prepare
     * @throws IOException if an error occurs while trying to prepare the
     *             statement
     */
    private void prepareStatement(String request, MySQLPacket incomingPacket) throws IOException {
        boolean largePacket = incomingPacket.getDataLength() >= MySQLPacket.MAX_LENGTH;
        if (logger.isDebugEnabled()) {
            logger.info("PREPARE_STATEMENT: " + request);
        }
        Connection connectionToUse = getConnectionForRequest(request, largePacket);
        if (connectionToUse == null) {
            sendErrorPacket(MySQLConstants.ER_NO_DB_ERROR, "3D000", "No database selected");
            return;
        }
        PreparedStatementWithParameterInfo pstmtwpi = null;
        try {
            if (config.isPassThroughMode() && MySQLIOs.connectionIsCompatible(connectionToUse)) {
                MySQLIOs ios = MySQLIOs.getMySQLIOs(connectionToUse);
                InputStream mysqlInput = ios.getInput();
                BufferedOutputStream mysqlOutput = ios.getOutput();
                if (mysqlInput == null || mysqlOutput == null) {
                    String msg = new String("Unable to get MySQL server I/Os");
                    logger.error(msg);
                    throw new IOException(msg);
                }
                incomingPacket.preparePacketForStreaming();
                if (logger.isTraceEnabled()) logger.trace("sending packet " + incomingPacket);
                incomingPacket.write(mysqlOutput);
                while (incomingPacket.getDataLength() == MySQLPacket.MAX_LENGTH) {
                    incomingPacket.preparePacketForStreaming();
                    incomingPacket.write(mysqlOutput);
                }
                mysqlOutput.flush();
                MySQLPacket response = MySQLPacket.readPacket(mysqlInput);
                if (response.getDataLength() >= MySQLPacket.MAX_LENGTH) {
                    logger.error("Received large packet as OK packet");
                }
                byte OK = response.getByte();
                if (OK != 0) {
                    logger.error("Mysql server returned an error while preparing statement " + request + " packet was " + response);
                    response.preparePacketForStreaming();
                    response.write(out);
                    return;
                }
                long statementHandlerId = response.getUnsignedInt32();
                int numberOfColumns = response.getUnsignedShort();
                int numberOfParameters = response.getUnsignedShort();
                ptPreparedStatementConnections.put(statementHandlerId, connectionToUse);
                if (logger.isTraceEnabled()) logger.trace("Prepared new statement id: " + statementHandlerId);
                response.preparePacketForStreaming();
                response.write(out);
                boolean gotError = false;
                if (numberOfParameters > 0) {
                    if (logger.isTraceEnabled()) logger.trace("Prepared statement " + statementHandlerId + " has " + numberOfParameters + " parameters. Sending parameter info");
                    do {
                        response = MySQLPacket.readPacket(mysqlInput);
                        if (response.getDataLength() >= MySQLPacket.MAX_LENGTH) {
                            logger.error("Received large packet as parameter info");
                        }
                        if (logger.isTraceEnabled()) logger.trace("Streaming packet " + response);
                        response.preparePacketForStreaming();
                        response.write(out);
                        if (response.isError()) {
                            gotError = true;
                            logger.error("Mysql server returned an error while preparing statement " + request);
                            break;
                        }
                    } while (!response.isEOF());
                }
                if (!gotError && numberOfColumns > 0) {
                    if (logger.isTraceEnabled()) logger.trace("Prepared statement " + statementHandlerId + " has " + numberOfColumns + " columns. Sending column description");
                    do {
                        response = MySQLPacket.readPacket(mysqlInput);
                        if (logger.isTraceEnabled()) logger.trace("Streaming packet " + response);
                        response.preparePacketForStreaming();
                        response.write(out);
                        if (response.isError()) {
                            logger.error("Mysql server returned an error while preparing statement " + request);
                            break;
                        }
                    } while (!response.isEOF());
                }
            } else {
                PreparedStatement pstmt = connectionToUse.prepareStatement(request, Statement.RETURN_GENERATED_KEYS);
                pstmt.setEscapeProcessing(false);
                pstmt.setFetchSize(config.getFetchSize());
                ParameterMetaData pmd = pstmt.getParameterMetaData();
                ResultSetMetaData rsmd = pstmt.getMetaData();
                Long idx = getNextPreparedStatementIndex();
                if (logger.isTraceEnabled()) logger.trace("New statement id: " + idx);
                short numberOfColumns = (rsmd == null) ? 0 : (short) rsmd.getColumnCount();
                short numberOfParameters = (short) pmd.getParameterCount();
                pstmtwpi = new PreparedStatementWithParameterInfo(pstmt, numberOfParameters);
                preparedStatements.put(idx, pstmtwpi);
                short warningCount = 0;
                sendOkForPreparedStatementPacket(idx.longValue(), numberOfColumns, numberOfParameters, warningCount);
                if (logger.isTraceEnabled()) logger.trace("Statement " + request + " is now prepared: " + numberOfParameters + " parameters, " + numberOfColumns + " columns.");
                if (numberOfParameters > 0) {
                    for (int i = 0; i < numberOfParameters; i++) {
                        sendResultSetFieldMessage(null, null, null, MySQLConstants.CHARSET_BINARY, 0, JDBCMySQLTypeConverter.getMySQLType(Types.VARCHAR), MySQLConstants.BINARY_FLAG, (byte) 0);
                    }
                    sendEofMessage(warningCount);
                }
                if (numberOfColumns > 0) {
                    for (int i = 1; i <= numberOfColumns; i++) {
                        int type = rsmd.getColumnType(i);
                        short charset = JDBCMySQLTypeConverter.JDBCTypeToCharset(type);
                        short flags = JDBCMySQLTypeConverter.getMySQLFlagsForColumn(rsmd, i);
                        sendResultSetFieldMessage(rsmd.getTableName(i), rsmd.getColumnLabel(i), rsmd.getColumnName(i), charset, rsmd.getColumnDisplaySize(i), JDBCMySQLTypeConverter.getMySQLType(type), flags, (byte) rsmd.getScale(i));
                    }
                    sendEofMessage(warningCount);
                }
            }
        } catch (SQLException e) {
            sendErrorPacket(e.getErrorCode(), e.getSQLState(), getOriginalExceptionFrom(e).getMessage());
            logger.error("Error preparing statement: " + request, e);
        }
    }

    /**
     * Handles COM_STMT_EXECUTE command. Reads parameter information from
     * stream, sets the previously created prepared statement parameters and
     * executes the statement. Finally returns the request result back to the
     * client TODO: prepareStatement definition.
     * 
     * @param request
     * @throws IOException
     */
    private void preparedStatementExecute(MySQLPacket packet) throws IOException {
        long stmtId = packet.getUnsignedInt32();
        byte flags = 0;
        int iterCnt = 0;
        int paramCount = 0;
        PreparedStatementWithParameterInfo pswpi = null;
        if (!config.isPassThroughMode() || !MySQLIOs.connectionIsCompatible(ptPreparedStatementConnections.get(stmtId))) {
            flags = packet.getByte();
            iterCnt = packet.getInt32();
            paramCount = 0;
            pswpi = preparedStatements.get(new Long(stmtId));
            if (pswpi == null) {
                logger.error("Statement id " + stmtId + " not found in the list of prepared statements");
                sendErrorPacket(MySQLConstants.ER_UNKNOWN_STMT_HANDLER, "HY000", "Unknown prepared statement handler (" + stmtId + ") given to PreparedStatementExecute");
                return;
            }
            paramCount = pswpi.getNumberOfParameters();
            if (logger.isTraceEnabled()) logger.trace("Statement id=" + stmtId + " Flags=" + flags + " IterCnt=" + iterCnt + " " + paramCount + " parameters");
        }
        try {
            if (config.isPassThroughMode() && MySQLIOs.connectionIsCompatible(ptPreparedStatementConnections.get(stmtId))) {
                Connection conn = ptPreparedStatementConnections.get(stmtId);
                if (conn == null) {
                    logger.error("Statement id " + stmtId + " not found in the list of prepared statements");
                    sendErrorPacket(MySQLConstants.ER_UNKNOWN_STMT_HANDLER, "HY000", "Unknown prepared statement handler (" + stmtId + ") given to PreparedStatementExecute");
                    return;
                }
                execQueryOnServerAndStreamResult(false, packet, conn, true, false);
            } else {
                if (!isValid(pswpi.getPreparedStatement().getConnection())) {
                    sendErrorPacket(MySQLConstants.ER_NO_DB_ERROR, "3D000", "No database selected");
                    return;
                }
                if (paramCount > 0) {
                    byte[] nullBitMap = new byte[(paramCount + 7) / 8];
                    for (int i = 0; i < nullBitMap.length; i++) {
                        nullBitMap[i] = packet.getByte();
                    }
                    pswpi.setNulls(nullBitMap);
                    byte newParameterBoundFlag = packet.getByte();
                    if (newParameterBoundFlag != 0) {
                        for (int i = 0; i < paramCount; i++) {
                            int typeCode = packet.getUnsignedShort();
                            pswpi.setUnsigned(i, (typeCode & signMask) != 0);
                            pswpi.setType(i, (byte) (typeCode & typeMask));
                            if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + ": " + ((typeCode & signMask) != 0 ? "unsigned" : "signed") + " type=" + (typeCode & typeMask));
                        }
                    }
                    int i = 0;
                    for (i = 0; i < paramCount; i++) {
                        if (pswpi.isNull(i)) {
                            if (logger.isTraceEnabled()) logger.trace("parameter " + (i + 1) + " is null");
                            pswpi.getPreparedStatement().setNull(i + 1, JDBCMySQLTypeConverter.getJDBCType(pswpi.getType(i)));
                        } else if (pswpi.hasLongData(i)) {
                            pswpi.getPreparedStatement().setBytes(i + 1, pswpi.getLongData(i));
                        } else {
                            switch(pswpi.getType(i)) {
                                case MySQLConstants.MYSQL_TYPE_TINY:
                                    if (pswpi.isUnsigned(i)) {
                                        short paramVal = packet.getUnsignedByte();
                                        pswpi.getPreparedStatement().setShort(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Unsigned Byte, decimal value: " + paramVal);
                                    } else {
                                        byte paramVal = packet.getByte();
                                        pswpi.getPreparedStatement().setByte(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Byte, value: 0x" + Utils.byteToHexString(paramVal));
                                    }
                                    break;
                                case MySQLConstants.MYSQL_TYPE_SHORT:
                                    if (pswpi.isUnsigned(i)) {
                                        int paramVal = packet.getUnsignedShort();
                                        pswpi.getPreparedStatement().setInt(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Unsigned Short, value: " + paramVal);
                                    } else {
                                        short paramVal = packet.getShort();
                                        pswpi.getPreparedStatement().setShort(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Short, value: " + paramVal);
                                    }
                                    break;
                                case MySQLConstants.MYSQL_TYPE_LONG:
                                    if (pswpi.isUnsigned(i)) {
                                        long paramVal = packet.getUnsignedInt32();
                                        pswpi.getPreparedStatement().setLong(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Unsigned Int32, value: " + paramVal);
                                    } else {
                                        int paramVal = packet.getInt32();
                                        pswpi.getPreparedStatement().setInt(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Int32, value: " + paramVal);
                                    }
                                    break;
                                case MySQLConstants.MYSQL_TYPE_LONGLONG:
                                    if (true) {
                                        long paramVal = packet.getLong();
                                        pswpi.getPreparedStatement().setLong(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Int64, value: " + paramVal);
                                    }
                                    break;
                                case MySQLConstants.MYSQL_TYPE_FLOAT:
                                    {
                                        float paramVal = packet.getFloat();
                                        pswpi.getPreparedStatement().setFloat(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Float, value: " + paramVal);
                                    }
                                    break;
                                case MySQLConstants.MYSQL_TYPE_DOUBLE:
                                    {
                                        double paramVal = packet.getDouble();
                                        pswpi.getPreparedStatement().setDouble(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Float, value: " + paramVal);
                                        break;
                                    }
                                case MySQLConstants.MYSQL_TYPE_DECIMAL:
                                case MySQLConstants.MYSQL_TYPE_NEWDECIMAL:
                                    {
                                        String valAsString = packet.getLenEncodedString(true);
                                        BigDecimal paramVal = new BigDecimal(0);
                                        try {
                                            paramVal = new BigDecimal(valAsString);
                                        } catch (NumberFormatException e) {
                                            if (logger.isInfoEnabled()) logger.info("Could not convert " + valAsString + " to BigDecimal, inserting zero instead");
                                        }
                                        pswpi.getPreparedStatement().setBigDecimal(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Decimal, value: " + paramVal);
                                        break;
                                    }
                                case MySQLConstants.MYSQL_TYPE_VARCHAR:
                                case MySQLConstants.MYSQL_TYPE_VAR_STRING:
                                case MySQLConstants.MYSQL_TYPE_STRING:
                                case MySQLConstants.MYSQL_TYPE_TINY_BLOB:
                                case MySQLConstants.MYSQL_TYPE_MEDIUM_BLOB:
                                case MySQLConstants.MYSQL_TYPE_LONG_BLOB:
                                case MySQLConstants.MYSQL_TYPE_BLOB:
                                    {
                                        byte[] paramVal = packet.getLenEncodedBytes();
                                        pswpi.getPreparedStatement().setBytes(i + 1, paramVal);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type String or Blob, value (hex dump): " + Utils.byteArrayToHexString(paramVal));
                                        break;
                                    }
                                case MySQLConstants.MYSQL_TYPE_TIME:
                                    {
                                        long llength = packet.readFieldLength();
                                        if (llength > Integer.MAX_VALUE) logger.warn("Time length bug here!");
                                        int length = (int) llength;
                                        if (logger.isTraceEnabled()) logger.trace("remaining=" + length + " - " + llength);
                                        Time t = packet.getTime(length);
                                        pswpi.getPreparedStatement().setTime(i + 1, t);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Time, value: " + t);
                                        break;
                                    }
                                case MySQLConstants.MYSQL_TYPE_DATE:
                                    {
                                        long llength = packet.readFieldLength();
                                        if (llength > Integer.MAX_VALUE) logger.warn("Date length bug here!");
                                        int length = (int) llength;
                                        if (logger.isTraceEnabled()) logger.trace("DATE remaining bytes=" + length + " - " + llength);
                                        Date d = packet.getDate(length);
                                        pswpi.getPreparedStatement().setDate(i + 1, d);
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Date, value: " + d);
                                        break;
                                    }
                                case MySQLConstants.MYSQL_TYPE_DATETIME:
                                case MySQLConstants.MYSQL_TYPE_TIMESTAMP:
                                    {
                                        long llength = packet.readFieldLength();
                                        if (llength > Integer.MAX_VALUE) logger.warn("Datetime length bug here!");
                                        int length = (int) llength;
                                        if (logger.isTraceEnabled()) logger.trace("DATETIME remaining bytes=" + length + " - " + llength);
                                        long millis = 0;
                                        if (length >= 4) {
                                            millis = packet.getDate(length).getTime();
                                            if (length > 4) {
                                                millis += packet.getHourMinSec();
                                            }
                                            if (length > 7) {
                                                int mil = packet.getInt32();
                                                if (logger.isTraceEnabled()) logger.trace("millis =" + mil % 1000);
                                                millis += mil % 1000;
                                            }
                                        }
                                        pswpi.getPreparedStatement().setTimestamp(i + 1, new Timestamp(millis));
                                        if (logger.isTraceEnabled()) logger.trace("parameter #" + (i + 1) + " is of type Timestamp, value: " + new Timestamp(millis));
                                        break;
                                    }
                                default:
                                    logger.error("Unsupported type " + pswpi.getType(i) + " for parameter #" + (i + 1));
                                    break;
                            }
                        }
                    }
                    if (pswpi.hadError()) {
                        sendErrorPacket(pswpi.getError(), "HY000", "Incorrect arguments to execute()");
                        return;
                    }
                }
                if (logger.isTraceEnabled()) logger.trace("Executing prepared statement " + stmtId);
                if (pswpi.getPreparedStatement().execute()) {
                    if (logger.isTraceEnabled()) logger.trace("Successfully executed prepared statement " + stmtId + ". Sending result...");
                    sendAllResultSets(pswpi.getPreparedStatement(), false, true);
                } else {
                    if (logger.isTraceEnabled()) logger.trace("Successfully executed prepared statement " + stmtId + " " + " - " + pswpi.getPreparedStatement().getUpdateCount() + " rows updated");
                    udpateServerStatus(pswpi.getPreparedStatement().getConnection().getAutoCommit());
                    int updateCount = pswpi.getPreparedStatement().getUpdateCount();
                    if (updateCount == -1) {
                        updateCount = 0;
                    }
                    sendOkPacket(updateCount, getLastInsertId(pswpi.getPreparedStatement()), 0);
                }
                if (logger.isTraceEnabled()) logger.trace("Prepared Statement execute completed successfully");
            }
        } catch (SQLException e) {
            sendErrorPacket(e.getErrorCode(), e.getSQLState(), getOriginalExceptionFrom(e).getMessage());
            logger.error("Error in execute() of statement id " + stmtId, e);
            resetPreparedStatement(stmtId, false, null);
        }
    }

    private void storeLongData(MySQLPacket packet) throws IOException {
        Long idKey = new Long(packet.getUnsignedInt32());
        if (config.isPassThroughMode() && MySQLIOs.connectionIsCompatible(ptPreparedStatementConnections.get(idKey))) {
            MySQLIOs ios = MySQLIOs.getMySQLIOs(ptPreparedStatementConnections.get(idKey));
            InputStream mysqlInput = ios.getInput();
            BufferedOutputStream mysqlOutput = ios.getOutput();
            if (mysqlInput == null || mysqlOutput == null) {
                String msg = new String("Unable to get MySQL server I/Os");
                logger.error(msg);
                throw new IOException(msg);
            }
            packet.preparePacketForStreaming();
            if (logger.isTraceEnabled()) logger.trace("sending packet " + packet);
            packet.write(mysqlOutput);
            mysqlOutput.flush();
        } else {
            if (packet.getRemainingBytes() < MySQLConstants.MYSQL_LONG_DATA_HEADER) {
                logger.error("Long data packet is only " + packet.getRemainingBytes() + ", it should be > " + MySQLConstants.MYSQL_LONG_DATA_HEADER);
                sendErrorPacket(MySQLConstants.ER_WRONG_ARGUMENTS, "HY000", "Incorrect arguments to storeLongData()");
            }
            PreparedStatementWithParameterInfo pswpi = preparedStatements.get(idKey);
            if (pswpi == null) {
                logger.error("Statement id " + idKey + " not found in the list of prepared statements");
                sendErrorPacket(MySQLConstants.ER_UNKNOWN_STMT_HANDLER, "HY000", "Unknown prepared statement handler (" + idKey + ") given to storeLongData");
                return;
            }
            int parameterNumber = packet.getUnsignedShort();
            if (parameterNumber >= pswpi.getNumberOfParameters()) {
                logger.error("Statement id " + idKey + " only has " + pswpi.getNumberOfParameters() + " parameters, cannot set parameter number " + parameterNumber);
                pswpi.setError(MySQLConstants.ER_WRONG_ARGUMENTS);
                return;
            }
            try {
                pswpi.setLongData(parameterNumber, packet);
            } catch (OutOfMemoryError e) {
                logger.error("Out of Memory while setting Statement id " + idKey + " parameter #" + parameterNumber, e);
                pswpi.setError(MySQLConstants.ER_OUTOFMEMORY);
            }
        }
    }

    /**
     * Resets the jdbc prepared statement to its state right after prepare.<br>
     * 
     * @param statementId id of the statement to close
     * @param sendOK whether to send ok packet after reset
     * @throws IOException
     */
    private void resetPreparedStatement(long statementId, boolean sendOK, MySQLPacket incomingPacket) throws IOException {
        Long idKey = new Long(statementId);
        if (config.isPassThroughMode() && incomingPacket != null && MySQLIOs.connectionIsCompatible(ptPreparedStatementConnections.get(idKey))) {
            Connection conn = ptPreparedStatementConnections.get(idKey);
            if (conn == null) {
                logger.error("Statement id " + idKey + " not found in the list of prepared statements");
                sendErrorPacket(MySQLConstants.ER_UNKNOWN_STMT_HANDLER, "HY000", "Unknown prepared statement handler (" + idKey + ") given to PreparedStatementExecute");
                return;
            }
            MySQLIOs ios = MySQLIOs.getMySQLIOs(conn);
            InputStream mysqlInput = ios.getInput();
            BufferedOutputStream mysqlOutput = ios.getOutput();
            if (mysqlInput == null || mysqlOutput == null) {
                String msg = new String("Unable to get MySQL server I/Os");
                logger.error(msg);
                throw new IOException(msg);
            }
            incomingPacket.preparePacketForStreaming();
            if (logger.isTraceEnabled()) logger.trace("sending packet " + incomingPacket);
            incomingPacket.write(mysqlOutput);
            mysqlOutput.flush();
            MySQLPacket response = MySQLPacket.readPacket(mysqlInput);
            if (!response.isOK()) {
                logger.error("Mysql server returned an error while reseting statement " + statementId);
            }
            response.preparePacketForStreaming();
            response.write(out);
        } else {
            PreparedStatementWithParameterInfo pswpi = preparedStatements.get(idKey);
            if (pswpi == null) {
                logger.error("Statement id " + statementId + " not found in the list of prepared statements");
                sendErrorPacket(MySQLConstants.ER_UNKNOWN_STMT_HANDLER, "HY000", "Unknown prepared statement handler (" + statementId + ") given to resetPreparedStatement");
                return;
            }
            try {
                pswpi.clearParameters();
            } catch (SQLException e) {
                sendErrorPacket(e.getErrorCode(), e.getSQLState(), getOriginalExceptionFrom(e).getMessage());
                logger.error("Statement id " + statementId + " could not be reset: " + e.getLocalizedMessage());
            }
            if (logger.isTraceEnabled()) logger.trace("Statement " + statementId + " reset.");
            if (sendOK) sendOkPacket(0, 0, 0);
        }
    }

    /**
     * Closes the jdbc prepared statement corresponding to given id and removes
     * it from the list of prepared statements
     * 
     * @param statementId id of the statement to close
     * @throws IOException
     */
    private void closePreparedStatement(long statementId, MySQLPacket incomingPacket) throws IOException {
        Long idKey = new Long(statementId);
        if (config.isPassThroughMode() && MySQLIOs.connectionIsCompatible(ptPreparedStatementConnections.get(idKey))) {
            Connection conn = ptPreparedStatementConnections.get(idKey);
            if (conn == null) {
                logger.error("Statement id " + idKey + " not found in the list of prepared statements");
                sendErrorPacket(MySQLConstants.ER_UNKNOWN_STMT_HANDLER, "HY000", "Unknown prepared statement handler (" + idKey + ") given to PreparedStatementExecute");
                return;
            }
            MySQLIOs ios = MySQLIOs.getMySQLIOs(conn);
            InputStream mysqlInput = ios.getInput();
            BufferedOutputStream mysqlOutput = ios.getOutput();
            if (mysqlInput == null || mysqlOutput == null) {
                String msg = new String("Unable to get MySQL server I/Os");
                logger.error(msg);
                throw new IOException(msg);
            }
            ptPreparedStatementConnections.remove(idKey);
            if (logger.isTraceEnabled()) logger.trace("Removed statement " + idKey + " from connector statement list");
            incomingPacket.preparePacketForStreaming();
            if (logger.isTraceEnabled()) logger.trace("sending packet " + incomingPacket);
            incomingPacket.write(mysqlOutput);
            mysqlOutput.flush();
        } else {
            PreparedStatementWithParameterInfo pswpi = preparedStatements.get(idKey);
            if (pswpi == null) {
                logger.error("Statement id " + statementId + " not found in the list of prepared statements");
                sendErrorPacket(MySQLConstants.ER_UNKNOWN_STMT_HANDLER, "HY000", "Unknown prepared statement handler (" + statementId + ") given to closePreparedStatement");
                return;
            }
            try {
                pswpi.getPreparedStatement().close();
            } catch (SQLException ignored) {
                logger.warn("Statement id " + statementId + " could not be closed: " + ignored.getLocalizedMessage());
            }
            preparedStatements.remove(idKey);
            if (logger.isTraceEnabled()) logger.trace("Statement " + statementId + " successfully closed.");
        }
    }

    /**
     * Extracts the master exception generated by mysql from a sequoia-wrapped
     * one. <b>Warning:</b> argument must not be null!
     * 
     * @param wrapped sequoia-style encapsulation of the original exception
     * @return the mysql master exception
     */
    private Exception getOriginalExceptionFrom(SQLException wrapped) {
        Exception last = wrapped;
        Throwable next = wrapped.getCause();
        while (next != null) {
            last = (Exception) next;
            next = next.getCause();
        }
        return last;
    }

    /**
     * Send a greeting packet back to the client.
     * 
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void sendGreetingPacket() throws IOException {
        MySQLPacket buf = new MySQLPacket(64, (byte) 0);
        int flags = MySQLConstants.CLIENT_LONG_FLAG | MySQLConstants.CLIENT_CONNECT_WITH_DB | MySQLConstants.CLIENT_PROTOCOL_41 | MySQLConstants.CLIENT_TRANSACTIONS | MySQLConstants.CLIENT_LONG_PASSWORD | MySQLConstants.CLIENT_SECURE_CONNECTION;
        buf.putByte((byte) 10);
        buf.putString(config.getServerVersion());
        buf.putInt32((int) Thread.currentThread().getId());
        buf.putString(randomSeed.substring(0, 8));
        buf.putInt16((short) flags);
        buf.putByte((byte) 8);
        buf.putInt16(MySQLConstants.SERVER_STATUS_AUTOCOMMIT);
        buf.putBytes(new byte[13]);
        buf.putString(randomSeed.substring(8, 20));
        buf.write(out);
    }

    /**
     * Sends a access denied error packet
     */
    void denyAccess(String reason) throws IOException {
        sendErrorPacket(MySQLConstants.ER_DBACCESS_DENIED_ERROR, "28000", reason);
    }

    /**
     * Special function to set packet sequence manually from outside this
     * handler - to be used with care!
     * 
     * @param packetNumber new packet sequence to set
     */
    void setPacketSequence(byte packetNumber) {
        packetSequence = packetNumber;
    }

    /**
     * Send an authentication packet to the client. It checks if the user with
     * the given password can connect.
     * 
     * @param packet the MySQL packet
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void sendAuthenticationPacket(MySQLPacket packet) throws IOException {
        packetSequence = packet.getPacketNumber();
        if (packet.getDataLength() >= MySQLPacket.MAX_LENGTH) {
            String message = "Unexpected large authentication packet (>16M)";
            logger.error(message);
            sendErrorPacket(MySQLConstants.CLIENT_CONNECT_WITH_DB, "HY000", message);
            throw new IOException(message);
        }
        clientFlags = packet.getInt32();
        maximumPacketLength = packet.getInt32();
        characterSet = packet.getByte();
        packet.getBytes(23);
        userName = packet.getString();
        password = packet.getLenEncodedBytes();
        databaseName = packet.getString();
        verifyUsernameAndPassword(userName, password);
        try {
            connectToDatabase(userName, databaseName, true);
        } catch (SQLException s) {
            String message = String.format("Could not authorize the user '%s'\n" + "Attempt to connect to the server failed.\n" + "%s\n" + "Contact your Tungsten administrator.", userName, s.getLocalizedMessage());
            logger.warn(message, s);
            sendErrorPacket(MySQLConstants.CLIENT_CONNECT_WITH_DB, "HY000", message);
            throw new IOException(s.getLocalizedMessage());
        }
        sendOkPacket(0L, 0L, 0);
    }

    /**
     * Send an OK packet to the client.
     * 
     * @param affectedRows the affected rows
     * @param insertId the insert ID
     * @param warningCount the warning count
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void sendOkPacket(long affectedRows, long insertId, int warningCount) throws IOException {
        MySQLPacket buf = new MySQLPacket(64, ++packetSequence);
        buf.putByte((byte) 0);
        buf.putFieldLength(affectedRows);
        buf.putFieldLength(insertId);
        buf.putInt16(serverStatus);
        buf.putInt16(warningCount);
        buf.write(out);
    }

    /**
     * Send an error packet to the client.
     * 
     * @param errorNumber the error number
     * @param sqlState the sql error
     * @param errorMessage the human readable error message
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void sendErrorPacket(int errorNumber, String sqlState, String errorMessage) throws IOException {
        MySQLPacket buf = new MySQLPacket(64, ++packetSequence);
        if (sqlState == null) {
            sqlState = "S0000";
        }
        buf.putByte((byte) 255);
        buf.putInt16(errorNumber);
        buf.putByte((byte) '#');
        buf.putStringNoNull(sqlState);
        buf.putString(errorMessage == null ? "unknown" : errorMessage);
        buf.write(out);
    }

    /**
     * Send the ResultSet header packet to the client.
     * 
     * @param columns number of columns in the ResultSet
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void sendResultSetHeaderMessage(long columns) throws IOException {
        MySQLPacket buf = new MySQLPacket(16, ++packetSequence);
        buf.putFieldLength(columns);
        buf.write(out);
    }

    /**
     * Send the metadata information of the ResultSet to the client.
     * 
     * @param tableName the table name
     * @param columnName the column name (after the AS statement: select col AS
     *            othercol from ...)
     * @param columnOrigName the column original name
     * @param characterSet the character set
     * @param columnDisplaySize the length of the column
     * @param columnType the SQL type of the column
     * @param columnFlags different flags
     * @param decimalPlaces the number of decimal places
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void sendResultSetFieldMessage(String tableName, String columnName, String columnOrigName, int characterSet, int columnDisplaySize, byte columnType, int columnFlags, byte decimalPlaces) throws IOException {
        MySQLPacket buf = new MySQLPacket(64, ++packetSequence);
        buf.putLenString("def");
        buf.putLenString(null);
        buf.putLenString(tableName);
        buf.putLenString(tableName);
        buf.putLenString(columnName);
        buf.putLenString(columnOrigName);
        buf.putByte((byte) 12);
        buf.putInt16(characterSet);
        buf.putInt32(columnDisplaySize);
        buf.putByte(columnType);
        buf.putInt16(columnFlags);
        buf.putByte(decimalPlaces);
        buf.putInt16(0);
        buf.write(out);
    }

    /**
     * Send the actual row data to the client.
     * 
     * @param columns the number of columns
     * @param rs the ResultSet
     * @throws SQLException if an errors happens when retrieving the data
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void sendResultSetRowMessage(int columns, ResultSet rs) throws SQLException, IOException {
        MySQLPacket buf = new MySQLPacket(64, ++packetSequence);
        ResultSetMetaData rsmd = rs.getMetaData();
        for (int i = 1; i <= columns; i++) {
            int columnType = rsmd.getColumnType(i);
            if (!config.getMirrorZeroDateTimeNo() && columnType == Types.TIMESTAMP || columnType == Types.DATE) {
                Object col = null;
                boolean sendZeroDateTime = false;
                boolean isNull = false;
                boolean nullSent = false;
                try {
                    col = rs.getObject(i);
                    if (col == null) isNull = true;
                } catch (SQLException sqle) {
                    if (config.getMirrorZeroDateTimeException() && SQL_STATE_ILLEGAL_ARGUMENT.equals(sqle.getSQLState())) sendZeroDateTime = true; else throw sqle;
                }
                if (isNull) {
                    if (config.getMirrorZeroDateTimeConvertToNull()) sendZeroDateTime = true; else {
                        buf.putByte((byte) 251);
                        nullSent = true;
                    }
                }
                if (config.getMirrorZeroDateTimeRound()) {
                    if (logger.isTraceEnabled()) {
                        if (Types.TIMESTAMP == columnType && rs.getTimestamp(i) != null) logger.trace("ZeroDateTimeBehavior=round mirroring. Found timestamp " + rs.getTimestamp(i) + "=" + rs.getTimestamp(i).getTime() + " comparing to rounded timestamp" + (isRoundedZeroTimestamp(rs.getTimestamp(i)) ? ": MATCH!" : ": no match")); else if (Types.DATE == columnType && rs.getDate(i) != null) logger.trace("ZeroDateTimeBehavior=round mirroring. Found date " + rs.getDate(i) + "=" + rs.getDate(i).getTime() + " comparing to rounded date " + (Types.DATE == columnType && isRoundedZeroDate(rs.getDate(i)) ? ": MATCH!" : ": no match"));
                    }
                    if ((Types.TIMESTAMP == columnType && isRoundedZeroTimestamp(rs.getTimestamp(i))) || (Types.DATE == columnType && isRoundedZeroDate(rs.getDate(i)))) {
                        sendZeroDateTime = true;
                    }
                }
                if (!nullSent) {
                    if (sendZeroDateTime) {
                        String zeroDatetime = null;
                        if (Types.TIMESTAMP == columnType) zeroDatetime = ZERO_TIMESTAMP; else zeroDatetime = ZERO_DATE;
                        buf.putLenBytes(zeroDatetime.getBytes());
                    } else {
                        if (Types.TIMESTAMP == columnType) sendTimestamp(rs.getTimestamp(i), buf); else {
                            if (col instanceof Short) {
                                sendYear((Short) col, buf);
                            } else {
                                sendDate(rs.getDate(i), buf);
                            }
                        }
                    }
                }
            } else {
                Object col = rs.getObject(i);
                if (col == null) {
                    buf.putByte((byte) 251);
                } else {
                    switch(columnType) {
                        case Types.TIMESTAMP:
                            sendTimestamp(rs.getTimestamp(i), buf);
                            break;
                        case Types.DATE:
                            sendDate(rs.getDate(i), buf);
                            break;
                        case Types.BIT:
                            sendBit(rs.getBoolean(i), buf);
                            break;
                        case Types.VARBINARY:
                        case Types.LONGVARBINARY:
                        case Types.BLOB:
                        case Types.ARRAY:
                        case Types.BINARY:
                        case Types.CLOB:
                        case Types.DATALINK:
                            buf.putLenBytes(rs.getBytes(i));
                            if (logger.isTraceEnabled()) logger.trace("Sent binary value");
                            break;
                        case Types.REAL:
                        case Types.FLOAT:
                            {
                                float f = rs.getFloat(i);
                                String valueAsString = javaFPValueToMySQLFormat(String.valueOf(f));
                                buf.putLenBytes(valueAsString.getBytes());
                                if (logger.isTraceEnabled()) logger.trace("Sent float value:" + valueAsString);
                                break;
                            }
                        case Types.DOUBLE:
                            {
                                double d = rs.getDouble(i);
                                String valueAsString = javaFPValueToMySQLFormat(String.valueOf(d));
                                buf.putLenBytes(valueAsString.getBytes());
                                if (logger.isTraceEnabled()) logger.trace("Sent double value:" + valueAsString);
                                break;
                            }
                        default:
                            buf.putLenBytes(rs.getString(i).getBytes());
                            if (logger.isTraceEnabled()) logger.trace("Sent misc. type " + columnType + " value=" + rs.getString(i));
                    }
                }
            }
        }
        buf.write(out);
    }

    /**
     * Converts a java style floating point value to use MySQL formatting
     * 
     * @param valueAsString the floating point value to convert
     * @return a corresponding MySQL-styled string
     */
    private String javaFPValueToMySQLFormat(String valueAsString) {
        if (valueAsString.endsWith(".0")) valueAsString = valueAsString.substring(0, valueAsString.length() - 2);
        if (valueAsString.indexOf("E") > -1) {
            if (valueAsString.indexOf("E-") > -1) {
                valueAsString = valueAsString.replace('E', 'e');
            } else {
                valueAsString = valueAsString.replace("E", "e+");
            }
        }
        return valueAsString;
    }

    /**
     * Puts a bit value to into the buffer. As MySQL bit type will be returned
     * as a boolean by mysql connector, we have to get this boolean (the
     * parameter) and transform it into a 1 byte long byte array (MYO-45)
     * 
     * @param b the bit to send, as a boolean value
     * @param buf I/O buffer to send to
     */
    private void sendBit(Boolean b, MySQLPacket buf) {
        byte[] toSend = { 0 };
        if (b) toSend[0] = 1;
        buf.putLenBytes(toSend);
        if (logger.isTraceEnabled()) logger.trace("Sent bit value=" + toSend[0]);
    }

    private void sendYear(Short year, MySQLPacket buf) {
        buf.putLenBytes(year.toString().getBytes());
        if (logger.isTraceEnabled()) logger.trace("Sent year value=" + year);
    }

    private void sendDate(Date d, MySQLPacket buf) {
        buf.putLenBytes(d.toString().getBytes());
        if (logger.isTraceEnabled()) logger.trace("Sent date value=" + d);
    }

    private void sendTimestamp(Timestamp ts, MySQLPacket buf) {
        buf.putLenBytes((new SimpleDateFormat(config.getTimestampFormat())).format(ts).getBytes());
        if (logger.isTraceEnabled()) logger.trace("Sent timestamp value=" + ts);
    }

    static boolean isRoundedZeroTimestamp(Timestamp s) {
        if (s == null) return false;
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(s.getTime());
        return c.get(Calendar.SECOND) == 0 && c.get(Calendar.MINUTE) == 0 && c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.DAY_OF_MONTH) == 1 && c.get(Calendar.MONTH) == 0 && c.get(Calendar.YEAR) == 1;
    }

    static boolean isRoundedZeroDate(Date s) {
        if (s == null) return false;
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(s.getTime());
        return c.get(Calendar.DAY_OF_MONTH) == 1 && c.get(Calendar.MONTH) == 0 && c.get(Calendar.YEAR) == 1;
    }

    private void sendResultSetRowMessageBinary(int columns, ResultSet rs) throws SQLException, IOException {
        MySQLPacket buf = new MySQLPacket(64, ++packetSequence);
        ResultSetMetaData rsmd = rs.getMetaData();
        int nullCount = (columns + 9) / 8;
        byte[] nullBitMap = new byte[nullCount];
        for (int i = 0; i < nullCount; i++) {
            nullBitMap[i] = 0;
        }
        int bitInByte = 4;
        int byteInArray = 0;
        for (int i = 0; i < columns; i++) {
            if (rs.getObject(i + 1) == null) nullBitMap[byteInArray] |= (byte) (bitInByte);
            bitInByte <<= 1;
            if ((bitInByte & 255) == 0) {
                bitInByte = 1;
                byteInArray++;
            }
        }
        buf.putByte((byte) 0);
        buf.putBytes(nullBitMap);
        for (int i = 1; i <= columns; i++) {
            if (rs.getObject(i) != null) {
                switch(rsmd.getColumnType(i)) {
                    case Types.NULL:
                        break;
                    case Types.TINYINT:
                        buf.putByte(rs.getByte(i));
                        break;
                    case Types.SMALLINT:
                        buf.putInt16(rs.getShort(i));
                        break;
                    case Types.INTEGER:
                        buf.putInt32(rs.getInt(i));
                        break;
                    case Types.BIGINT:
                        buf.putLong(rs.getLong(i));
                        break;
                    case Types.FLOAT:
                    case Types.REAL:
                        buf.putFloat(rs.getFloat(i));
                        break;
                    case Types.DOUBLE:
                        buf.putDouble(rs.getDouble(i));
                        break;
                    case Types.TIME:
                        buf.putByte((byte) 8);
                        buf.putTime(rs.getTime(i));
                        break;
                    case Types.DATE:
                        buf.putByte((byte) 4);
                        buf.putDate(rs.getDate(i));
                        break;
                    case Types.TIMESTAMP:
                        buf.putByte((byte) 7);
                        Timestamp ts = rs.getTimestamp(i);
                        long remainingMillis = ts.getTime();
                        remainingMillis = remainingMillis - buf.putDate(new Date(remainingMillis));
                        remainingMillis = remainingMillis - buf.putHourMinSec(new Time(remainingMillis).getTime());
                        break;
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.CHAR:
                    case Types.DECIMAL:
                    case Types.NUMERIC:
                    case Types.BOOLEAN:
                        buf.putLenString(rs.getString(i));
                        break;
                    case Types.BIT:
                        sendBit(rs.getBoolean(i), buf);
                        break;
                    case Types.VARBINARY:
                    case Types.LONGVARBINARY:
                    case Types.BLOB:
                    case Types.ARRAY:
                    case Types.BINARY:
                    case Types.CLOB:
                        buf.putLenBytes(rs.getBytes(i));
                        break;
                    case Types.DATALINK:
                    case Types.DISTINCT:
                    case Types.JAVA_OBJECT:
                    case Types.OTHER:
                    case Types.REF:
                    case Types.STRUCT:
                    default:
                        logger.warn("Not sure how to serialize jdbc type #" + rsmd.getColumnType(i) + " will send it as string data");
                        buf.putLenBytes(rs.getBytes(i));
                }
            }
        }
        buf.write(out);
    }

    /**
     * Send the Eof packet to the client.
     * 
     * @param warningCount the warning count
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void sendEofMessage(int warningCount) throws IOException {
        MySQLPacket buf = new MySQLPacket(16, ++packetSequence);
        buf.putByte((byte) 254);
        buf.putInt16(warningCount);
        buf.putInt16(serverStatus);
        buf.write(out);
    }

    /**
     * Send an OK for Prepared Statement Initialization packet to the client.
     * 
     * @param handlerId statement handler id
     * @param numberOfColumns number of columns in result set
     * @param numberOfParameters number of parameters in query
     * @param warningCount number of warnings
     * @throws IOException if an error happens when sending the response to the
     *             client
     */
    private void sendOkForPreparedStatementPacket(long handlerId, int numberOfColumns, int numberOfParameters, int warningCount) throws IOException {
        MySQLPacket buf = new MySQLPacket(64, ++packetSequence);
        buf.putByte((byte) 0);
        buf.putUnsignedInt32(handlerId);
        buf.putInt16(numberOfColumns);
        buf.putInt16(numberOfParameters);
        buf.putByte((byte) 0);
        buf.putInt16(warningCount);
        buf.write(out);
    }

    /**
     * Check if the password matches.
     * 
     * @param plainPassword this is the plain password that will be encrypted
     * @param scrambledPassword this is the already encrypted password
     * @return true if the encrypt(plainPassword) == scrambledPassword else
     *         return false
     */
    private boolean passwordMatches(String plainPassword, byte[] scrambledPassword) {
        if (plainPassword.length() == 0 && scrambledPassword.length == 0) {
            if (logger.isTraceEnabled()) logger.trace("Password length both zero - OK");
            return true;
        }
        if (plainPassword.length() != 0 && scrambledPassword.length == 0) {
            if (logger.isTraceEnabled()) logger.trace("Declared password length non zero, actual password length zero - KO");
            return false;
        }
        if (plainPassword.length() == 0 && scrambledPassword.length != 0) {
            if (logger.isTraceEnabled()) logger.trace("Declared password length = zero, actual password length non zero - KO");
            return false;
        }
        if (plainPassword.length() != 0 && scrambledPassword.length != 0) {
            try {
                byte[] result = scramblePassword(plainPassword, randomSeed);
                for (int i = 0; i < result.length; i++) {
                    if (result[i] != scrambledPassword[i]) {
                        if (logger.isTraceEnabled()) logger.trace("Declared password doesn't match actual password - KO");
                        return false;
                    }
                }
                return true;
            } catch (NoSuchAlgorithmException e) {
                logger.error("SHA-1 algorithm not found.", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Scramble password using the seed.
     * 
     * @param password the password to scramble
     * @param seed the seed which will be used to scramble the password
     * @return the scrambled password
     * @throws NoSuchAlgorithmException if SHA-1 algorithm is not available
     */
    public byte[] scramblePassword(String password, String seed) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] stage1 = md.digest(password.getBytes());
        md.reset();
        byte[] stage2 = md.digest(stage1);
        md.reset();
        md.update(seed.getBytes());
        md.update(stage2);
        byte[] result = md.digest();
        for (int i = 0; i < result.length; i++) {
            result[i] ^= stage1[i];
        }
        return result;
    }

    /**
     * Computes a new index for prepared statement.<br>
     * Make sure the index is not in the preparedStatements keys while taking
     * care of maxint.
     * 
     * @return an Integer usable as a new index to add an entry in
     *         preparedStatements hashtable
     */
    private Long getNextPreparedStatementIndex() {
        if (pstmtIndex == Long.MAX_VALUE) pstmtIndex = 0; else pstmtIndex++;
        Long key = new Long(pstmtIndex);
        while (preparedStatements.containsKey(key)) {
            if (pstmtIndex == Long.MAX_VALUE) pstmtIndex = 0; else pstmtIndex++;
            key = new Long(pstmtIndex);
        }
        return key;
    }

    private boolean verifyUsernameAndPassword(String submittedUserName, byte[] submittedPassword) throws IOException {
        String hasPassword = (submittedPassword.length == 0 ? "with no password." : "using a password.");
        UserMapItem item = config.getUserMap().getConfigItem(submittedUserName, getConnectedFromHost());
        if (item.getUserName().length() == 0) {
            String reason = String.format("Authorization failed for user '%s',  %s\n" + "There is currently no entry in the user.map for this user.\n" + "Contact your Tungsten system administrator.", submittedUserName, hasPassword);
            denyAccess(reason);
            logger.warn(reason);
            throw new IOException(reason);
        }
        String userMapPassword = item.getPassword();
        if (!passwordMatches(userMapPassword, submittedPassword)) {
            String reason = String.format("Authorization failed for user '%s'@'%s',  %s\n" + "The password for this user@host, in the user.map, file does not match this password.\n" + "Contact your Tungsten system administrator.", submittedUserName, getConnectedFromHost(), hasPassword);
            denyAccess(reason);
            logger.warn(reason);
            throw new IOException(reason);
        }
        return true;
    }

    /**
     * Sends a single-column, single row result set.
     * 
     * @param message text to display in result
     * @throws IOException upon communication error with client
     */
    protected void sendTextMessage(String message) throws IOException {
        sendResultSetHeaderMessage(1);
        int type = MySQLConstants.MYSQL_TYPE_STRING;
        short charset = JDBCMySQLTypeConverter.JDBCTypeToCharset(type);
        short flags = 0;
        sendResultSetFieldMessage("tungsten_message", "message", "message", charset, 80, JDBCMySQLTypeConverter.getMySQLType(type), flags, (byte) 0);
        sendEofMessage(0);
        MySQLPacket buf = new MySQLPacket(64, ++packetSequence);
        buf.putLenBytes(message.getBytes());
        buf.write(out);
        sendEofMessage(0);
    }
}
