package com.apbetioli.mapr.project.control;

import com.apbetioli.mapr.project.schema.CtProject;
import com.apbetioli.mapr.util.MD5String;

/**
 *
 * @author Alexandre Parra Betioli
 */
public class KeyGenerator {

    /**
     * The key is calculed by the hash MD5 of the email
     */
    public static String generateKey(final CtProject project) {
        String key = project.getKey();
        if ((key == null) || (key.trim().equals(""))) {
            String all = project.getName() + project.getEmail();
            key = MD5String.digest(all);
        }
        return key;
    }
}
