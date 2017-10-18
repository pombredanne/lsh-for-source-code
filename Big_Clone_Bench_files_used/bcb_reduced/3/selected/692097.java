package com.mrroman.linksender.passdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.PasswordAuthentication;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import com.mrroman.linksender.Configuration;
import com.mrroman.linksender.ioc.In;
import com.mrroman.linksender.ioc.Log;
import com.mrroman.linksender.ioc.Name;
import com.mrroman.linksender.settings.Settings;

@Name("passdb.FilePasswordStorage")
public class FilePasswordStorage extends Settings<FilePasswordStorage> implements PasswordStorage {

    @Log
    private Logger logger;

    @In
    private Configuration configuration;

    private BufferedReader reader;

    private PrintWriter writer;

    public Map<String, PasswordAuthentication> readDatabase() {
        Map<String, PasswordAuthentication> resultMap = new HashMap<String, PasswordAuthentication>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(new File(this.settingsDirectory(), "passwords.txt")));
            String line;
            while ((line = in.readLine()) != null) {
                String[] fields = line.split(" ");
                if (fields.length != 3) continue;
                String domainName = URLDecoder.decode(fields[0], "UTF-8");
                String userName = URLDecoder.decode(fields[1], "UTF-8");
                char[] password = null;
                try {
                    password = unsignPassword(URLDecoder.decode(fields[2], "UTF-8"));
                    resultMap.put(domainName, new PasswordAuthentication(userName, password));
                } catch (InvalidPasswordException e) {
                    logger.severe(e.getMessage());
                }
            }
            in.close();
        } catch (IOException e) {
            logger.severe("Cannot read password file");
        }
        return resultMap;
    }

    public void writeDatabase(Map<String, PasswordAuthentication> database) {
        try {
            PrintWriter out = new PrintWriter(new File(this.settingsDirectory(), "passwords.txt"));
            for (Map.Entry<String, PasswordAuthentication> entry : database.entrySet()) {
                out.printf("%s %s %s", URLEncoder.encode(entry.getKey(), "UTF-8"), URLEncoder.encode(entry.getValue().getUserName(), "UTF-8"), URLEncoder.encode(signPassword(entry.getValue().getPassword()), "UTF-8"));
            }
            out.close();
        } catch (FileNotFoundException e) {
        } catch (UnsupportedEncodingException e) {
        }
    }

    static String signPassword(char[] password) {
        try {
            char[] hashedPassword = hashPassword(password);
            StringBuilder sb = new StringBuilder(String.valueOf(hashedPassword));
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            sb.append(String.format("%032x", new BigInteger(1, messageDigest.digest(String.valueOf(hashedPassword).getBytes()))));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static char[] unsignPassword(String text) throws InvalidPasswordException {
        try {
            String password = text.substring(0, text.length() - 32);
            String digest = text.substring(text.length() - 32);
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            if (!new BigInteger(1, messageDigest.digest(password.getBytes())).equals(new BigInteger(digest, 16))) {
                throw new InvalidPasswordException("Password is invalid");
            }
            return hashPassword(password.toCharArray());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static char[] hashPassword(char[] password) {
        char[] newPass = password.clone();
        for (int i = 0; i < password.length; i++) newPass[i] = (char) (password[i] ^ 0x7);
        return newPass;
    }

    @Override
    public String applicationName() {
        return configuration.applicationName();
    }
}
