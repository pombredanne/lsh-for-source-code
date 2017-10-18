package com.stg.analytics.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.util.logging.Logger;

/**
 * @author Sparksis
 * 
 *         Generates a session token in the form of <code>MD5("<em>remote ip</em>:<em>NOW.long())</em>"</code> then populates the Event_Session Table of the database.
 * 
 */
@SuppressWarnings("serial")
public class SessionGenerator extends HttpServlet {

    private MessageDigest md;

    private Connection connection;

    public SessionGenerator() throws ServletException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Cannot find MySQL driver", cnfe);
        }
        try {
            createStatement();
        } catch (SQLException e) {
            throw new ServletException("Cannot create a connection", e);
        }
    }

    private PreparedStatement createStatement() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:mysql://02t.vps.sparksis.com/Analytics", "root", "iAmRoot");
        }
        return connection.prepareStatement("INSERT INTO Event_Session (IP,Session_Time,Token_ID,User_Agent) VALUES (? , ? , ?, ?)");
    }

    @Override
    public void destroy() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            Logger.getLogger(SessionGenerator.class.getName()).warning("Connection failed to close gracefully");
        }
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (this.md == null) {
            try {
                this.md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new ServletException(e);
            }
        }
        PrintWriter pw = resp.getWriter();
        String ip = req.getRemoteAddr();
        Timestamp time = new Timestamp(System.currentTimeMillis());
        String token = new BigInteger(this.md.digest((ip + ":" + time.getTime()).getBytes())).abs().toString(16);
        resp.setContentLength(token.length());
        pw.print(token);
        pw.close();
        try {
            PreparedStatement statement = createStatement();
            statement.setString(1, ip);
            statement.setTimestamp(2, time);
            statement.setString(3, token);
            statement.setString(4, req.getHeader("User-Agent"));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
