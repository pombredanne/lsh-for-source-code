package net.sf.imageCave;

import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Date;
import java.util.StringTokenizer;
import java.security.*;

public class Client {

    Socket socketData;

    Socket socketCtrl;

    String socketHost;

    public boolean connect(String host) {
        System.out.println("Trying host " + host);
        socketCtrl = null;
        socketHost = new String(host);
        try {
            socketCtrl = new Socket(host, 8888);
            socketData = new Socket(host, 9999);
            socketCtrl.setSoTimeout(30000);
            socketData.setSoTimeout(30000);
        } catch (Exception e) {
            System.out.println("Failed to connect: " + e);
            return false;
        }
        return true;
    }

    public boolean login(String username, String password) {
        boolean authenticated = false;
        try {
            BufferedReader inCtrl;
            inCtrl = new BufferedReader(new InputStreamReader(socketCtrl.getInputStream()));
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            PrintWriter outCtrl;
            outCtrl = new PrintWriter(socketCtrl.getOutputStream(), true);
            outCtrl.println("LOGIN " + username);
            String response = new String(inCtrl.readLine());
            if (!response.trim().equals("INVALID")) {
                String salt = new String(inCtrl.readLine());
                String salts[] = null;
                salts = salt.split("\\.");
                String data = new String(salts[0] + password + salts[1]);
                byte buf[] = data.getBytes();
                md.update(buf);
                byte hash[] = md.digest();
                for (int i = 0; i < hash.length; i++) {
                    outCtrl.print((int) hash[i]);
                }
                outCtrl.print("\n");
                outCtrl.flush();
                if (inCtrl.readLine().trim().equals("Access Granted")) {
                    authenticated = true;
                    this.download("");
                } else {
                    authenticated = false;
                }
            } else {
                authenticated = false;
            }
        } catch (Exception e) {
            System.out.println("Failed to login " + e);
            return false;
        }
        return authenticated;
    }

    public boolean quit() {
        try {
            PrintWriter outCtrl;
            outCtrl = new PrintWriter(socketCtrl.getOutputStream(), true);
            outCtrl.println("QUIT");
        } catch (Exception e) {
            System.out.println("Failed to quit: " + e);
            return false;
        }
        return true;
    }

    public boolean changePassword(String newPass) {
        try {
            BufferedReader inCtrl;
            inCtrl = new BufferedReader(new InputStreamReader(socketCtrl.getInputStream()));
            PrintWriter outCtrl;
            outCtrl = new PrintWriter(socketCtrl.getOutputStream(), true);
            outCtrl.println("CHPASS " + newPass);
            String response = new String(inCtrl.readLine());
            if (response.trim().equals("SUCCESS")) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Failed to change password: " + e);
            return false;
        }
    }

    public boolean addFriend(String album, String username) {
        try {
            BufferedReader inCtrl;
            inCtrl = new BufferedReader(new InputStreamReader(socketCtrl.getInputStream()));
            PrintWriter outCtrl;
            outCtrl = new PrintWriter(socketCtrl.getOutputStream(), true);
            outCtrl.println("ADDFRIEND " + album + " " + username);
            String response = new String(inCtrl.readLine());
            if (response.trim().equals("SUCCESS")) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Failed to add friend: " + e);
            return false;
        }
    }

    public boolean rmFriend(String album, String username) {
        try {
            BufferedReader inCtrl;
            inCtrl = new BufferedReader(new InputStreamReader(socketCtrl.getInputStream()));
            PrintWriter outCtrl;
            outCtrl = new PrintWriter(socketCtrl.getOutputStream(), true);
            outCtrl.println("RMFRIEND " + album + " " + username);
            String response = new String(inCtrl.readLine());
            if (response.trim().equals("SUCCESS")) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Failed to remove friend: " + e);
            return false;
        }
    }

    public boolean delete(String path) {
        try {
            BufferedReader inCtrl;
            inCtrl = new BufferedReader(new InputStreamReader(socketCtrl.getInputStream()));
            PrintWriter outCtrl;
            outCtrl = new PrintWriter(socketCtrl.getOutputStream(), true);
            outCtrl.println("DELETE " + path);
            String response = new String(inCtrl.readLine());
            if (response.trim().equals("SUCCESS")) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Failed to delete path: " + e);
            return false;
        }
    }

    public boolean download(String path) {
        try {
            if (socketData.isClosed()) {
                socketData = new Socket(socketHost, 9999);
                socketData.setSoTimeout(30000);
            }
        } catch (Exception e) {
            System.out.println("Socket error: " + e);
        }
        byte inByte = 0;
        FileOutputStream file = null;
        BufferedOutputStream buff = null;
        DataOutputStream data = null;
        InputStream in = null;
        BufferedInputStream bis = null;
        DataInputStream dataIn = null;
        try {
            if (!path.endsWith("\\") && !path.equals("")) {
                path = path + "\\";
            }
            BufferedReader inCtrl;
            inCtrl = new BufferedReader(new InputStreamReader(socketCtrl.getInputStream()));
            PrintWriter outCtrl;
            outCtrl = new PrintWriter(socketCtrl.getOutputStream(), true);
            File basePath = new File("clientImages\\" + path);
            basePath.mkdirs();
            outCtrl.println("GET " + path);
            String inCtrlString = new String(inCtrl.readLine());
            if (inCtrlString.trim().equals("!DONE")) {
                return false;
            }
            while (!inCtrlString.trim().equals("!DONE")) {
                try {
                    if (socketData.isClosed()) {
                        socketData = new Socket(socketHost, 9999);
                    }
                } catch (Exception e) {
                    System.out.println("Socket error: " + e);
                }
                if (inCtrlString.endsWith("\\")) {
                    String tempDir = new String("clientImages\\" + path + inCtrlString);
                    File tempPath = new File(tempDir);
                    tempPath.mkdirs();
                } else {
                    File tempFile = new File("clientImages\\" + path + inCtrlString);
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                    file = new FileOutputStream(tempFile.getPath());
                    buff = new BufferedOutputStream(file);
                    data = new DataOutputStream(buff);
                    in = socketData.getInputStream();
                    bis = new BufferedInputStream(in);
                    dataIn = new DataInputStream(bis);
                    try {
                        while (true) {
                            inByte = dataIn.readByte();
                            data.write(inByte);
                        }
                    } catch (EOFException eof) {
                        buff.flush();
                        buff.close();
                        dataIn.close();
                        data.flush();
                        data.close();
                    }
                }
                inCtrlString = inCtrl.readLine();
            }
        } catch (Exception e) {
            System.out.println("Failed to GET path: " + e);
            return false;
        }
        return true;
    }

    public boolean upload(String filename) {
        try {
            if (socketData.isClosed()) {
                socketData = new Socket(socketHost, 9999);
                socketData.setSoTimeout(30000);
            }
        } catch (Exception e) {
            System.out.println("Socket error: " + e);
            return false;
        }
        BufferedReader inCtrl;
        PrintWriter outCtrl;
        String inCtrlString;
        DataOutputStream dataOut;
        OutputStream out;
        FileInputStream fileIn;
        BufferedInputStream buffIn;
        try {
            inCtrl = new BufferedReader(new InputStreamReader(socketCtrl.getInputStream()));
            outCtrl = new PrintWriter(socketCtrl.getOutputStream(), true);
            out = socketData.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(out);
            dataOut = new DataOutputStream(bos);
            fileIn = new FileInputStream("clientImages\\" + filename);
            buffIn = new BufferedInputStream(fileIn);
            try {
                outCtrl.println("PUT " + filename);
                inCtrlString = inCtrl.readLine();
                StringTokenizer st1 = new StringTokenizer(inCtrlString, " ");
                String firstToken = new String(st1.nextToken());
                int in = 0;
                if (!firstToken.matches("Error") && !firstToken.matches("Failure")) {
                    while (true && in != -1) {
                        in = buffIn.read();
                        if (in != -1) {
                            dataOut.write(in);
                        }
                    }
                    buffIn.close();
                    dataOut.flush();
                    dataOut.close();
                    fileIn.close();
                } else {
                    return false;
                }
            } catch (EOFException eof) {
                dataOut.flush();
                dataOut.close();
                return false;
            }
        } catch (Exception e) {
            System.out.println("Failed to connect: " + e);
            return false;
        }
        return true;
    }
}
