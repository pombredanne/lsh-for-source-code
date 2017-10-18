package Server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import TransmitterS.ChatIntelligence;
import TransmitterS.NetworkCenter;

/**
 * Klasse um Chatnachrichten zwischen Benutzern zu verschicken.
 * Der Empfang geschieht �ber das genrische Nachrichtenkonstrukt.  
 * @see ChatIntelligenceImp
 * @author LK13
 */
public class NetworkCenterImp implements NetworkCenter {

    public static ServerImp myServer = null;

    public NetworkCenterImp(ServerImp _myServer) {
        myServer = _myServer;
    }

    public boolean sendMessage(String sender, String nachricht, Set<String> receiver) {
        ChatIntelligence new_msg = new ChatIntelligenceImp(sender, nachricht);
        if (receiver != null && receiver.size() > 0) try {
            for (String username : receiver) myServer.myUserCenter.getUser(username).getIntelligence().add(new_msg);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } else return false;
        return true;
    }

    public Set<String> avaibleParsers() throws RemoteException {
        return new HashSet<String>(Arrays.asList(new File("parsers").list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.matches(".*\\.jar");
            }
        })));
    }

    public byte[] downloadFile(String fileName) {
        try {
            File file = new File("parsers" + File.separator + fileName);
            byte buffer[] = new byte[(int) file.length()];
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
            input.read(buffer, 0, buffer.length);
            input.close();
            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] verifyFile(String fileName) throws RemoteException {
        try {
            return MessageDigest.getInstance("MD5").digest(downloadFile(fileName));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
