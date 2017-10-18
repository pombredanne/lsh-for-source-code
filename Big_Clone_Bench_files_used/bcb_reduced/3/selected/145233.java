package com.beendoin.logic;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.beendoin.db.DBClient;
import com.beendoin.domain.Authentication;
import com.beendoin.domain.Nonce;
import com.beendoin.util.Base64;

public class LoginLogic {

    private static Logger LOG = Logger.getLogger(LoginLogic.class.getName());

    public static Authentication login(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        boolean failOnDoesntExist = "true".equalsIgnoreCase((String) request.getAttribute("failOnDoesntExist"));
        String auth = request.getHeader("Authorization");
        LOG.finest("The Authorization header is: " + auth);
        String decodedBytes = new String(Base64.decode(auth.substring(6)));
        LOG.finest("The decoded Authorization header is: " + decodedBytes);
        String[] nonceParts = decodedBytes.split("\r\n");
        LOG.finest("The nonceParts are: " + nonceParts[0] + " and " + nonceParts[1]);
        String[] authParts = nonceParts[0].split(":");
        String username = authParts[0];
        LOG.finest("The username is: " + username);
        String password = authParts[1];
        LOG.finest("The password is: " + password);
        LOG.finest("The nonce is: " + nonceParts[1]);
        String passwordDigest = null;
        Authentication authn = null;
        try {
            authn = (Authentication) DBClient.orm.queryForObject("getAuthentication", username);
            if (authn == null) {
                if (failOnDoesntExist) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Failed attempt to authenticate: " + username + "/" + password);
                    return null;
                } else {
                    authn = new Authentication();
                    authn.setUsername(username);
                    authn.setPassword(password);
                    authn.setUserId(-1);
                    return authn;
                }
            }
            LOG.finest("The DB password is: " + authn.getPassword());
            String hashedDBPassword = authn.getPassword() + "\r\n" + nonceParts[1];
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = md5Digest.digest(hashedDBPassword.getBytes());
            passwordDigest = Base64.encodeBytes(digestBytes);
        } catch (Exception ex) {
            if (LOG.isLoggable(Level.FINEST)) {
                StringWriter writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));
                LOG.finest(writer.toString());
            }
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            throw new ServletException(ex);
        }
        Object rowExists = null;
        Nonce nonce = new Nonce();
        try {
            nonce.setUsername(username);
            nonce.setUserId(authn.getUserId());
            nonce.setNonce(Long.parseLong(nonceParts[1]));
            nonce.setNonceAsDate(new Date(nonce.getNonce()));
            rowExists = DBClient.orm.queryForObject("getNonce", nonce);
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            throw new ServletException(ex);
        }
        if (rowExists != null || nonce.getNonce() + 1800000l < System.currentTimeMillis()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Failed attempt to authenticate: " + username + "/" + password);
            throw new ServletException("Failed attempt to authenticate: " + username + "/" + password);
        } else {
            Object txOwner = new Object();
            try {
                DBClient.startTransaction(txOwner);
                DBClient.orm.update("insertNonce", nonce);
                DBClient.commitTransaction(txOwner);
            } catch (Exception ex) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                throw new ServletException(ex);
            } finally {
                try {
                    DBClient.endTransaction(txOwner);
                } catch (Exception ex) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                    throw new ServletException(ex);
                }
            }
        }
        LOG.finest("The MD5/Base64 digest of the DB password is: " + passwordDigest);
        if (!password.equals(passwordDigest)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Failed attempt to authenticate: " + username + "/" + password);
            throw new ServletException("Failed attempt to authenticate: " + username + "/" + password);
        }
        return authn;
    }
}
