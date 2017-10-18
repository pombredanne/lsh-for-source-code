package org.gridtrust.UcsService.Utils.createuser;

import java.security.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gridtrust.UcsService.threads.PDPThread;

/**
 * this  class is used for the creation of a dynamic user into the system
 * @author maurizio colombo
 */
public class CreateUser {

    static final Log logger = LogFactory.getLog(CreateUser.class);

    private Runtime runtime;

    /**
     * constructor for a dynamic user
     */
    public CreateUser() {
        this.runtime = Runtime.getRuntime();
    }

    /**
     * @param name DN of the user
     * @param role role of the user (can be null) 
     * @return local user name 
     */
    public String addUser(String name, String role) {
        String fin_name = null;
        if (role != null) name = name + role;
        String command = "sudo /usr/sbin/useradd -K UMASK=000 ";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] msg = name.getBytes();
            md.update(msg);
            byte[] aMessageDigest = md.digest();
            base32 b32 = new base32();
            fin_name = b32.encode(aMessageDigest);
            Process p = runtime.exec(command + fin_name);
            int exitVal = p.waitFor();
            logger.info("the local account " + fin_name + " has been created for the user " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fin_name;
    }

    /**
     * @param name DN of the user
     * @param role role of the user
     * @return local user name
     */
    public String getUserName(String name, String role) {
        String fin_name = null;
        if (role != null) name = name + role;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] msg = name.getBytes();
            md.update(msg);
            byte[] aMessageDigest = md.digest();
            base32 b32 = new base32();
            fin_name = b32.encode(aMessageDigest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fin_name;
    }
}
