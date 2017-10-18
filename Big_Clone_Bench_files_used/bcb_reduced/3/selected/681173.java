package fishs.util;

import java.net.Socket;
import java.io.*;
import java.security.MessageDigest;

public class UpdateThread implements Runnable {

    Socket socket;

    public UpdateThread(Socket sock) {
        socket = sock;
    }

    public void run() {
        try {
            long read = 0;
            byte[] chunk = new byte[65535];
            int chunkSize;
            int[] version = new int[5];
            MessageDigest md = MessageDigest.getInstance("MD5");
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            int mode = dis.readInt();
            System.out.println("Read mode as" + mode);
            for (int i = 0; i < version.length; i++) {
                version[i] = dis.readInt();
                System.out.println(i + " as " + version[i]);
            }
            System.out.println("Out");
            String updateFile = LookupVersion(version);
            System.out.println("lookedup version");
            if (updateFile == null) {
                System.out.println("No update!");
                dos.writeInt(0);
                dos.flush();
                socket.close();
                return;
            }
            dos.writeInt(1);
            System.out.println("file: " + updateFile);
            if (mode == 0) {
                dos.close();
                socket.close();
                return;
            }
            System.out.println("Update file is " + updateFile);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream("./updates/" + updateFile));
            File f = new File("./updates/" + updateFile);
            long length = f.length();
            dos.writeLong(length);
            System.out.println("Sent size: " + length);
            while (read < length) {
                chunkSize = bis.read(chunk);
                System.out.println("S: " + chunkSize);
                md.update(chunk, 0, chunkSize);
                dos.writeInt(chunkSize);
                dos.write(chunk, 0, chunkSize);
                read += chunkSize;
            }
            dos.writeInt(0);
            byte[] digest = md.digest();
            dos.writeInt(digest.length);
            dos.write(digest);
            dos.close();
            bis.close();
            System.out.println("Finished sending update");
        } catch (Exception e) {
            System.out.println("ServerThread: " + e);
        }
    }

    private String LookupVersion(int[] ver) {
        File dir = new File("./updates/");
        String[] files = dir.list();
        if (files.length == 0) return (null);
        int[] max = versionToInts(files[0]);
        String maxFile = files[0];
        for (int i = 1; i < files.length; i++) {
            int[] temp = versionToInts(files[i]);
            for (int j = 0; j < max.length; j++) {
                if (temp[j] > max[j]) {
                    max = temp;
                    maxFile = files[i];
                    break;
                }
            }
        }
        for (int i = 0; i < max.length; i++) {
            if (max[i] > ver[i]) return (maxFile); else if (max[i] < ver[i]) return (null);
        }
        return (null);
    }

    private int[] versionToInts(String ver) {
        String[] parts = ver.split("\\.");
        int[] nums = new int[parts.length];
        for (int i = 0; i < parts.length; i++) nums[i] = Integer.parseInt(parts[i]);
        return (nums);
    }
}
