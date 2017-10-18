package com.ssg.db;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Properties;

public class DBConnector implements Serializable {

    private String user;

    private String password;

    private String dataSource;

    private String driverName;

    private transient Connection connection;

    private transient DatabaseMetaData metaData;

    private transient Properties clientInfo;

    public DBConnector() {
    }

    public DBConnector(String driverName, String dataSource, String user, String password) {
        this.dataSource = dataSource;
        this.driverName = driverName;
        this.user = user;
        this.password = password;
    }

    protected void connect() throws SQLException {
        try {
            Driver driver = (Driver) Class.forName(driverName).newInstance();
            Properties connectionProperties = new Properties();
            if (user != null) {
                connectionProperties.put("user", user);
            }
            if (password != null) {
                connectionProperties.put("password", password);
            }
            connection = driver.connect(dataSource, connectionProperties);
            connection.setAutoCommit(false);
            metaData = connection.getMetaData();
            clientInfo = connection.getClientInfo();
        } catch (Exception e) {
            throw new SQLException("Problem opening DB connection: " + e.getMessage());
        }
    }

    protected void disconnect() throws SQLException {
        try {
            if (connection == null) {
                return;
            }
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException("Disconnection from DB failed: " + e.getMessage());
        }
        connection = null;
    }

    public Connection getConnection(boolean force) throws SQLException {
        if (connection == null && force) {
            connect();
        }
        return connection;
    }

    public Connection getConnection() throws SQLException {
        return getConnection(true);
    }

    public void closeConnection() throws SQLException {
        if (connection != null) {
            disconnect();
        }
    }

    public Connection reConnect() throws SQLException {
        closeConnection();
        return getConnection();
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public DatabaseMetaData getMetaData() {
        if (metaData == null) {
            try {
                getConnection();
            } catch (SQLException sqlex) {
            }
        }
        return metaData;
    }

    public Properties getClientInfo() {
        if (clientInfo == null) {
            try {
                getConnection();
            } catch (SQLException sqlex) {
            }
        }
        return clientInfo;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordMD5() {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(password.getBytes());
            byte messageDigest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
        }
        return null;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
