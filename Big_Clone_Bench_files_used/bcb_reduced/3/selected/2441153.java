package com.coltrane.dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import com.coltrane.domain.Account;
import com.coltrane.domain.ComboBoxOption;

public class AccountDAO implements DAO<Account> {

    private SimpleJdbcTemplate simpleJdbcTemplate;

    public void setDataSource(DataSource dataSource) {
        this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public long create(Account account) {
        String query = "INSERT INTO Account (`Code`, `Password`) VALUES (:Code, :Password)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("Code", account.getCode().toUpperCase().trim());
        parameters.addValue("Password", digestPassword(account.getPassword().trim()));
        simpleJdbcTemplate.update(query, parameters);
        return simpleJdbcTemplate.queryForLong("SELECT LAST_INSERT_ID()");
    }

    @Override
    public void deleteById(long id) {
        String query = "DELETE FROM Account WHERE Id = ?";
        simpleJdbcTemplate.update(query, id);
    }

    @Override
    public Account getById(long id) {
        String query = "SELECT * FROM Account WHERE Id = ?";
        RowMapper<Account> rowMapper = new RowMapper<Account>() {

            @Override
            public Account mapRow(ResultSet resultSet, int rowNum) throws SQLException {
                Account account = new Account();
                account.setId(resultSet.getLong("Id"));
                account.setCode(resultSet.getString("Code"));
                account.setPassword(resultSet.getString("Password"));
                return account;
            }
        };
        return simpleJdbcTemplate.queryForObject(query, rowMapper, id);
    }

    @Override
    public void update(MapSqlParameterSource parameters) {
        String query = "UPDATE Account SET Password = :Password WHERE Id = :Id";
        simpleJdbcTemplate.update(query, parameters);
    }

    @Override
    public List<Account> search(String condition, String order, int page, int recordsPerPage) {
        String query = "SELECT * FROM Account";
        if (!condition.equals("")) {
            query += " WHERE " + condition;
        }
        if (!order.equals("")) {
            query += " ORDER BY " + order;
        }
        query += " LIMIT " + page * recordsPerPage + ", " + recordsPerPage;
        RowMapper<Account> rowMapper = new RowMapper<Account>() {

            @Override
            public Account mapRow(ResultSet resultSet, int rowNum) throws SQLException {
                Account account = new Account();
                account.setId(resultSet.getLong("Id"));
                account.setCode(resultSet.getString("Code"));
                account.setPassword(resultSet.getString("Password"));
                return account;
            }
        };
        return simpleJdbcTemplate.query(query, rowMapper);
    }

    @Override
    public int countSearchResult(String condition) {
        String query = "SELECT COUNT(*) FROM Account";
        if (!condition.equals("")) {
            query += " WHERE " + condition;
        }
        return simpleJdbcTemplate.queryForInt(query);
    }

    public int getValidAccountCount(Account login, boolean isPortal) {
        String query = "";
        if (isPortal) {
            query = "SELECT COUNT(*) FROM Customer WHERE PortalAccount = :Code AND PortalPassword = :Password";
        } else {
            query = "SELECT COUNT(*) FROM Account WHERE Code = :Code AND Password = :Password";
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("Code", login.getCode().toUpperCase().trim());
        parameters.addValue("Password", digestPassword(login.getPassword().trim()));
        return simpleJdbcTemplate.queryForInt(query, parameters);
    }

    public int getIdByCode(String code) {
        String query = "SELECT Id FROM Account WHERE Code = :Code";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("Code", code.toUpperCase().trim());
        return simpleJdbcTemplate.queryForInt(query, parameters);
    }

    public String digestPassword(String password) {
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(password.getBytes());
            byte[] messageDigest = algorithm.digest();
            for (byte b : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & b));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }

    @Override
    public List<ComboBoxOption> getOptions(String condition, String order) {
        String query = "SELECT Id, Code FROM Account";
        if (!condition.equals("")) {
            query += " WHERE " + condition;
        }
        if (order.equals("")) {
            order = "Id";
        }
        query += " ORDER BY " + order;
        RowMapper<ComboBoxOption> rowMapper = new RowMapper<ComboBoxOption>() {

            @Override
            public ComboBoxOption mapRow(ResultSet resultSet, int rowNum) throws SQLException {
                ComboBoxOption option = new ComboBoxOption();
                option.setValue(resultSet.getString("Id"));
                option.setText(resultSet.getString("Code"));
                return option;
            }
        };
        return simpleJdbcTemplate.query(query, rowMapper);
    }
}
