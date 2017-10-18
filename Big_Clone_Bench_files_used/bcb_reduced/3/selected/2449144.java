package com.mobiwebinc.compconn.client;

import com.mobiwebinc.compconn.communication.IOHandler;
import com.mobiwebinc.compconn.configuration.Constant;
import com.mobiwebinc.compconn.events.command.Command;
import com.mobiwebinc.compconn.configuration.ServerConfiguration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author suraj
 */
public class Authenticator {

    IOHandler io;

    private boolean writeOnly;

    ServerConfiguration serverConfiguration;

    public Authenticator(IOHandler io) {
        this.io = io;
        this.serverConfiguration = ServerConfiguration.getInstance();
    }

    public static String md5It(String data) {
        MessageDigest digest;
        String output = "";
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(data.getBytes());
            byte[] hash = digest.digest();
            for (byte b : hash) {
                output = output + String.format("%02X", b);
            }
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Authenticator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return output;
    }

    public boolean authenticate() {
        String rndString = Constant.VERSION + System.currentTimeMillis();
        io.sendMsg(rndString);
        String password = io.readMsg();
        String svrPwd = serverConfiguration.getPassword() + rndString;
        if (md5It(svrPwd).equals(password)) {
            grant();
            writeOnly = true;
            return true;
        }
        if (md5It(svrPwd + "\nread").equals(password)) {
            grant();
            return true;
        }
        deny();
        try {
            Thread.sleep(Constant.TERMINATE_TIME);
        } catch (InterruptedException ex) {
            Logger.getLogger(Authenticator.class.getName()).log(Level.SEVERE, null, ex);
        }
        io.disconnect();
        return false;
    }

    private void grant() {
        io.sendMsg(Command.GRANT + ServerConfiguration.getInstance().getOSCode() + ServerConfiguration.getInstance().getMonitors());
        System.out.println("code: " + Command.GRANT + ServerConfiguration.getInstance().getOSCode());
    }

    private void deny() {
        io.sendMsg(Command.DENIED);
        System.out.println("code: " + Command.DENIED);
    }

    /**
     * @return the writeOnly
     */
    public boolean isWriteOnly() {
        return writeOnly;
    }
}
