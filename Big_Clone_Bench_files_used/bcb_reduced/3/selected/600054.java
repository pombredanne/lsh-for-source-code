package mobiledesktopserver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

/**
 *
 * @author rockanjan
 */
public class Authorize {

    private RandomAccessFile file;

    /** Creates a new instance of Authorize */
    public Authorize() {
    }

    public void open(String filename) {
        System.out.println("filename" + filename);
        File path = new File(filename);
        if (!path.exists()) {
            try {
                path.createNewFile();
            } catch (Exception e) {
            }
        }
        try {
            file = new RandomAccessFile(path, "rw");
            System.out.println("im successful");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void close() {
        if (file != null) {
            try {
                file.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean authorize(String username, String password, String filename) {
        open(filename);
        boolean isAuthorized = false;
        StringBuffer encPasswd = null;
        try {
            MessageDigest mdAlgorithm = MessageDigest.getInstance("MD5");
            mdAlgorithm.update(password.getBytes());
            byte[] digest = mdAlgorithm.digest();
            encPasswd = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                password = Integer.toHexString(255 & digest[i]);
                if (password.length() < 2) {
                    password = "0" + password;
                }
                encPasswd.append(password);
            }
        } catch (NoSuchAlgorithmException ex) {
        }
        String encPassword = encPasswd.toString();
        String tempPassword = getPassword(username);
        System.out.println("epass" + encPassword);
        System.out.println("pass" + tempPassword);
        if (tempPassword.equals(encPassword)) {
            isAuthorized = true;
        } else {
            isAuthorized = false;
        }
        close();
        return isAuthorized;
    }

    public String getPassword(String username) {
        String user = "";
        String pass = "";
        String line = "";
        StringTokenizer token = null;
        try {
            file.seek(0);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        do {
            try {
                line = file.readLine();
                System.out.println("line is " + line);
            } catch (IOException ex) {
                return "";
            }
            try {
                token = new StringTokenizer(line);
                while (token.hasMoreTokens()) {
                    user = token.nextToken();
                    pass = token.nextToken();
                }
            } catch (NullPointerException e) {
                return "";
            }
        } while (!username.equals(user));
        return pass;
    }

    public void addUser(String username, String password, String filename) {
        String data = "";
        try {
            open(filename);
            MessageDigest mdAlgorithm = MessageDigest.getInstance("MD5");
            mdAlgorithm.update(password.getBytes());
            byte[] digest = mdAlgorithm.digest();
            StringBuffer encPasswd = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                password = Integer.toHexString(255 & digest[i]);
                if (password.length() < 2) {
                    password = "0" + password;
                }
                encPasswd.append(password);
                data = username + " " + encPasswd + "\r\n";
            }
            try {
                long length = file.length();
                file.seek(length);
                file.write(data.getBytes());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            close();
        } catch (NoSuchAlgorithmException ex) {
        }
    }

    public void updateUser(String username, String password, String filename) {
        try {
            open(filename);
            String user = "";
            String pass = "";
            String line = "";
            StringTokenizer token = null;
            long prevFilePointer = 0;
            long currentFilePointer = 0;
            do {
                prevFilePointer = currentFilePointer;
                line = file.readLine();
                currentFilePointer = file.getFilePointer();
                try {
                    token = new StringTokenizer(line);
                    while (token.hasMoreTokens()) {
                        user = token.nextToken();
                        pass = token.nextToken();
                    }
                } catch (NullPointerException e) {
                }
            } while (!username.equals(user));
            file.seek(prevFilePointer);
            line = username + " " + encryptPassword(password) + "\n";
            file.writeBytes(line);
            close();
        } catch (IOException ex) {
        }
    }

    public String encryptPassword(String password) {
        StringBuffer encPasswd = new StringBuffer();
        try {
            MessageDigest mdAlgorithm = MessageDigest.getInstance("MD5");
            mdAlgorithm.update(password.getBytes());
            byte[] digest = mdAlgorithm.digest();
            for (int i = 0; i < digest.length; i++) {
                password = Integer.toHexString(255 & digest[i]);
                if (password.length() < 2) {
                    password = "0" + password;
                }
                encPasswd.append(password);
            }
        } catch (NoSuchAlgorithmException ex) {
        }
        return encPasswd.toString();
    }

    public void removeUser(String username, String password, String filename) {
        try {
            open(filename);
            String user = "";
            String pass = "";
            String line = "";
            StringTokenizer token = null;
            long prevFilePointer = 0;
            long currentFilePointer = 0;
            String lastData = "";
            long length = 0;
            do {
                prevFilePointer = currentFilePointer;
                line = file.readLine();
                currentFilePointer = file.getFilePointer();
            } while (currentFilePointer != file.length());
            System.out.println("prev" + prevFilePointer + " current" + currentFilePointer);
            file.seek(prevFilePointer);
            lastData = file.readLine();
            length = prevFilePointer;
            file.seek(0);
            currentFilePointer = 0;
            prevFilePointer = 0;
            do {
                prevFilePointer = currentFilePointer;
                line = file.readLine();
                currentFilePointer = file.getFilePointer();
                try {
                    token = new StringTokenizer(line);
                    while (token.hasMoreTokens()) {
                        user = token.nextToken();
                        pass = token.nextToken();
                    }
                } catch (NullPointerException e) {
                }
            } while (!username.equals(user));
            file.seek(prevFilePointer);
            file.writeBytes(lastData);
            file.setLength(length);
            close();
        } catch (IOException ex) {
        }
    }
}
