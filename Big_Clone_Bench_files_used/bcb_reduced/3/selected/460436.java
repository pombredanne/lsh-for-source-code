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
import com.coltrane.domain.ComboBoxOption;
import com.coltrane.domain.Customer;

public class CustomerDAO implements DAO<Customer> {

    private SimpleJdbcTemplate simpleJdbcTemplate;

    public void setDataSource(DataSource dataSource) {
        this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public long create(Customer customer) {
        String query = "INSERT INTO Customer (`Code`, `Name`, `Contact`, `Phone`, `Fax`, `Email`, `Address`, `PortalAccount`, `PortalPassword`, `Sales`, `Note`) VALUES (:Code, :Name, :Contact, :Phone, :Fax, :Email, :Address, :PortalAccount, :PortalPassword, :Sales, :Note)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("Code", customer.getCode().toUpperCase().trim());
        parameters.addValue("Name", customer.getName());
        parameters.addValue("Contact", customer.getContact());
        parameters.addValue("Phone", customer.getPhone());
        parameters.addValue("Fax", customer.getFax());
        parameters.addValue("Email", customer.getEmail());
        parameters.addValue("Address", customer.getAddress());
        parameters.addValue("PortalAccount", customer.getPortalAccount());
        parameters.addValue("PortalPassword", digestPassword(customer.getPortalPassword().trim()));
        parameters.addValue("Sales", customer.getSales());
        parameters.addValue("Note", customer.getNote());
        simpleJdbcTemplate.update(query, parameters);
        return simpleJdbcTemplate.queryForLong("SELECT LAST_INSERT_ID()");
    }

    @Override
    public void deleteById(long id) {
        String query = "DELETE FROM Customer WHERE Id = ?";
        simpleJdbcTemplate.update(query, id);
    }

    @Override
    public Customer getById(long id) {
        String query = "SELECT * FROM Customer WHERE Id = ?";
        RowMapper<Customer> rowMapper = new RowMapper<Customer>() {

            @Override
            public Customer mapRow(ResultSet resultSet, int rowNum) throws SQLException {
                Customer customer = new Customer();
                customer.setId(resultSet.getLong("Id"));
                customer.setCode(resultSet.getString("Code"));
                customer.setName(resultSet.getString("Name"));
                customer.setContact(resultSet.getString("Contact"));
                customer.setPhone(resultSet.getString("Phone"));
                customer.setFax(resultSet.getString("Fax"));
                customer.setEmail(resultSet.getString("Email"));
                customer.setAddress(resultSet.getString("Address"));
                customer.setPortalAccount(resultSet.getString("PortalAccount"));
                customer.setPortalPassword(resultSet.getString("PortalPassword"));
                customer.setSales(resultSet.getString("Sales"));
                customer.setNote(resultSet.getString("Note"));
                return customer;
            }
        };
        return simpleJdbcTemplate.queryForObject(query, rowMapper, id);
    }

    @Override
    public void update(MapSqlParameterSource parameters) {
        String query = "UPDATE Customer SET Name = :Name, Contact = :Contact, Phone = :Phone, Fax = :Fax, Email = :Email, Address = :Address, PortalAccount = :PortalAccount, PortalPassword = :PortalPassword, Sales = :Sales, Note = :Note WHERE Id = :Id";
        simpleJdbcTemplate.update(query, parameters);
    }

    @Override
    public List<Customer> search(String condition, String order, int page, int recordsPerPage) {
        String query = "SELECT * FROM Customer";
        if (!condition.equals("")) {
            query += " WHERE " + condition;
        }
        if (!order.equals("")) {
            query += " ORDER BY " + order;
        }
        query += " LIMIT " + page * recordsPerPage + ", " + recordsPerPage;
        RowMapper<Customer> rowMapper = new RowMapper<Customer>() {

            @Override
            public Customer mapRow(ResultSet resultSet, int rowNum) throws SQLException {
                Customer customer = new Customer();
                customer.setId(resultSet.getLong("Id"));
                customer.setCode(resultSet.getString("Code"));
                customer.setName(resultSet.getString("Name"));
                customer.setContact(resultSet.getString("Contact"));
                customer.setPhone(resultSet.getString("Phone"));
                customer.setFax(resultSet.getString("Fax"));
                customer.setEmail(resultSet.getString("Email"));
                customer.setAddress(resultSet.getString("Address"));
                customer.setPortalAccount(resultSet.getString("PortalAccount"));
                customer.setPortalPassword(resultSet.getString("PortalPassword"));
                customer.setSales(resultSet.getString("Sales"));
                customer.setNote(resultSet.getString("Note"));
                return customer;
            }
        };
        return simpleJdbcTemplate.query(query, rowMapper);
    }

    @Override
    public int countSearchResult(String condition) {
        String query = "SELECT COUNT(*) FROM Customer";
        if (!condition.equals("")) {
            query += " WHERE " + condition;
        }
        return simpleJdbcTemplate.queryForInt(query);
    }

    @Override
    public List<ComboBoxOption> getOptions(String condition, String order) {
        String query = "SELECT Id, Code FROM Customer";
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

    public String getCustomerByPortalAccount(String code) {
        String query = "SELECT Code FROM Customer WHERE PortalAccount = ?";
        return simpleJdbcTemplate.queryForObject(query, String.class, code);
    }
}
