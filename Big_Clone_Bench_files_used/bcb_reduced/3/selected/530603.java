package org.ala.spatial.services.dao;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.ala.spatial.services.dto.Application;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 *
 * @author ajay
 */
@org.springframework.stereotype.Service("applicationDao")
public class ApplicationDAOImpl implements ApplicationDAO {

    private static final Logger logger = Logger.getLogger(ApplicationDAOImpl.class);

    private SimpleJdbcTemplate jdbcTemplate;

    private SimpleJdbcInsert insertApplication;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
        this.insertApplication = new SimpleJdbcInsert(dataSource).withTableName("applications").usingGeneratedKeyColumns("id");
    }

    @Override
    public List<Application> findApplications() {
        logger.info("Getting a list of all applications");
        String sql = "select * from applications";
        return jdbcTemplate.query(sql, new ApplicationMapper());
    }

    @Override
    public List<Application> findApplicationsByName(String name) {
        logger.info("Getting a list of all applications by name");
        String sql = "select * from applications where name = ?";
        return jdbcTemplate.query(sql, new ApplicationMapper(), name);
    }

    @Override
    public List<Application> findApplicationsByEmail(String email) {
        logger.info("Getting a list of all applications by email");
        String sql = "select * from applications where email = ?";
        return jdbcTemplate.query(sql, new ApplicationMapper(), email);
    }

    @Override
    public List<Application> findApplicationsByOrganisation(String org) {
        logger.info("Getting a list of all applications by organisation");
        String sql = "select * from applications where organisation = ?";
        return jdbcTemplate.query(sql, new ApplicationMapper(), org);
    }

    @Override
    public Application findApplicationByAppId(String appid) {
        logger.info("Getting a list of all applications by appid");
        String sql = "select * from applications where appid = ?";
        return jdbcTemplate.query(sql, new ApplicationMapper(), appid).get(0);
    }

    @Override
    public void addApplication(Application app) {
        logger.info("Adding a new application " + app.getName() + " by " + app.getOrganisation() + " (" + app.getEmail() + ") ");
        app.setRegtime(new Timestamp(new Date().getTime()));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update((app.getName() + app.getEmail() + app.getRegtime()).getBytes());
            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            app.setAppid(sb.toString());
        } catch (NoSuchAlgorithmException ex) {
            java.util.logging.Logger.getLogger(ApplicationDAOImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(app.toString());
        SqlParameterSource parameters = new BeanPropertySqlParameterSource(app);
        Number appUid = insertApplication.executeAndReturnKey(parameters);
        app.setId(appUid.longValue());
    }

    @Override
    public void removeApplication(String appid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateApplication(Application app) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static final class ApplicationMapper implements RowMapper<Application> {

        public Application mapRow(ResultSet rs, int rowNum) throws SQLException {
            Application app = new Application();
            app.setAppid(rs.getString("appid"));
            app.setDescription(rs.getString("description"));
            app.setEmail(rs.getString("email"));
            app.setId(rs.getLong("id"));
            app.setName(rs.getString("name"));
            app.setOrganisation(rs.getString("organisation"));
            app.setRegtime(rs.getTimestamp("regtime"));
            app.setStatus(rs.getString("status"));
            app.setUrl(rs.getString("url"));
            app.setContact(rs.getString("contact"));
            app.setClientip(rs.getString("clientip"));
            return app;
        }
    }
}
