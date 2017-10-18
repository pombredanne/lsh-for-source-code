package blasar.Services.Com.vms;

import blasar.Config;
import blasar.Services.Exceptions.IllegalStatement;
import blasar.Services.SocketTools;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

/**
 *
 * @author Jesus Navalon i Pastor <jnavalon at redhermes dot net>
 */
public class AddStorage {

    private static final int BUFFER_SIZE = 1024;

    String uuid, type, controller;

    boolean cache;

    VMCommands intercom;

    AddStorage(String uuid, String type, String controller, boolean cache, VMCommands intercom) {
        this.uuid = uuid;
        this.type = type;
        this.controller = controller;
        this.cache = cache;
        this.intercom = intercom;
    }

    public boolean addStorageCtl() {
        return intercom.storageCtl(uuid, type, controller, cache);
    }

    public void addImages(SocketTools st) throws SocketException, IOException, IllegalStatement {
        st.sendLine(Config.CMD.CHARS.QUESTION + "addimage?");
        String cmd = st.readLine(Config.CMD.ANSWER);
        if (!cmd.equals("y")) {
            st.sendLine(Config.CMD.CHARS.INFO + "END");
            return;
        }
        do {
            st.sendLine(Config.CMD.CHARS.QUESTION + "data?");
            saveImage(new StringTokenizer(st.readLine(Config.CMD.ANSWER)), st);
            st.sendLine(Config.CMD.CHARS.QUESTION + "addimage?");
            cmd = st.readLine(Config.CMD.ANSWER);
        } while (cmd.equals("y"));
        st.sendLine(Config.CMD.CHARS.INFO + "END");
    }

    private void saveImage(StringTokenizer cmd, SocketTools st) throws SocketException, IOException {
        if (cmd.countTokens() != 6) {
            st.sendLine(Config.CMD.CHARS.INFO + "BADARGS");
            return;
        }
        String filename = st.getFullArguments(cmd);
        String extension = filename.substring(filename.lastIndexOf('.'));
        filename = filename.substring(0, filename.lastIndexOf('.'));
        String imgType = getType(cmd.nextToken());
        int port = Integer.parseInt(cmd.nextToken());
        int device = 0;
        if (type.equals("ide")) {
            device = port;
            port = 0;
            if (device > 1) {
                device = port - 2;
                port = 1;
            }
        }
        boolean passthrough = Boolean.getBoolean(cmd.nextToken());
        String sha1 = cmd.nextToken();
        if (imgType == null) {
            st.sendLine(Config.CMD.CHARS.INFO + "BADARGS");
            return;
        }
        long total, size;
        total = intercom.getFreeSpace();
        if (total == -1) {
            st.sendLine(Config.CMD.CHARS.INFO + "ERROR");
            return;
        }
        try {
            size = Long.parseLong(cmd.nextToken());
        } catch (NumberFormatException ex) {
            st.sendLine(Config.CMD.CHARS.INFO + "BADARGS");
            return;
        }
        if (size == -1) {
            if (intercom.storageattach(uuid, type.toUpperCase() + " Module", port, device, imgType, passthrough)) {
                st.sendLine(Config.CMD.CHARS.INFO + "OK");
            } else {
                st.sendLine(Config.CMD.CHARS.INFO + "ERROR");
            }
            return;
        }
        if (total < size) {
            st.sendLine(Config.CMD.CHARS.INFO + "NOSPACE");
            return;
        }
        String path = intercom.getImagePath();
        File file = new File(path + File.separator + uuid);
        if (!file.exists()) {
            if (!file.mkdir()) {
                st.sendLine(Config.CMD.CHARS.INFO + "ERROR");
                return;
            }
        }
        File outputFile = new File(file.getAbsolutePath() + File.separator + filename + extension);
        int i = 0;
        while (outputFile.exists()) {
            outputFile = new File(file.getAbsolutePath() + File.separator + filename + "_" + i++ + extension);
        }
        st.sendLine(Config.CMD.CHARS.QUESTION + "BINARY");
        long tsize = 0;
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(outputFile));
            while (tsize < size) {
                byte[] buffer = new byte[(size - tsize) > 1024 ? 1024 : (int) (size - tsize)];
                buffer = st.readBytes(buffer.length);
                bos.write(buffer);
                tsize += buffer.length;
            }
            bos.close();
        } catch (FileNotFoundException ex) {
            st.sendLine(Config.CMD.CHARS.INFO + "ERROR");
            if (bos != null) bos.close();
            if (outputFile.exists()) if (!outputFile.delete()) outputFile.deleteOnExit();
            return;
        }
        String hash = getSHA1(outputFile);
        if (hash != null) {
            if (hash.equals(sha1)) {
                st.sendLine(Config.CMD.CHARS.ANSWER + outputFile.getName());
            } else {
                st.sendLine(Config.CMD.CHARS.INFO + "BADHASH");
            }
        } else {
            st.sendLine(Config.CMD.CHARS.INFO + "ERROR");
        }
        if (intercom.storageattach(uuid, type.toUpperCase() + " Module", port, device, imgType, outputFile.getAbsolutePath(), passthrough)) {
            st.sendLine(Config.CMD.CHARS.INFO + "OK");
        } else {
            st.sendLine(Config.CMD.CHARS.INFO + "ERROR");
        }
    }

    private String getSHA1(File outputFile) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(outputFile));
            byte[] digest;
            String hash = "";
            long size = outputFile.length();
            byte[] data = new byte[size > BUFFER_SIZE ? BUFFER_SIZE : (int) (size)];
            long i = 0;
            while (bis.read(data) > 0) {
                md.update(data);
                i += data.length;
                if (size - i < BUFFER_SIZE) data = new byte[(int) (size - i)];
            }
            digest = md.digest();
            for (byte aux : digest) {
                int b = aux & 0xff;
                if (Integer.toHexString(b).length() == 1) {
                    hash += "0";
                }
                hash += Integer.toHexString(b);
            }
            return hash;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (NoSuchAlgorithmException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    private String getType(String id) {
        short num;
        try {
            num = Short.parseShort(id);
        } catch (NumberFormatException ex) {
            return null;
        }
        switch(num) {
            case 0:
                return "dvddrive";
            case 1:
                return "hdd";
            case 2:
                return "floppy";
            default:
                return null;
        }
    }
}
