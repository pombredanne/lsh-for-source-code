package serveur;

import general.ProtocolException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Cette classe définit un contexte d'authentification du client. C'est le
 * contexte initial lorsqu'un client se connecte au serveur. Il s'agit de
 * vérifier l'identité du client au moyen de son mot de passe. 
 * @author Olivier
 */
public class AuthentificationState extends ServerState {

    /** Lien vers la base de données de l'application */
    private DataManager datamanager;

    public AuthentificationState(ServerThread ownerThread, DataManager datamanager, SocketChannel socket) {
        super(ownerThread, socket);
        this.datamanager = datamanager;
    }

    /**
     * Cette méthode gère les messages pendant la phase d'authentification du
     * client.
     *
     * @param message Le texte du message envoyé au serveur.
     */
    @Override
    protected void handleMessage(String message) {
        try {
            Document doc = new SAXBuilder().build(new ByteArrayInputStream(message.getBytes()));
            Element root = doc.getRootElement();
            if (!root.getName().equals("message")) {
                throw new ProtocolException("Message non valide");
            }
            String messageType = root.getAttributeValue("type");
            if ((root.getAttributes().size() == 4) && ("HELLO".equalsIgnoreCase(messageType))) {
                String login = root.getAttributeValue("login");
                String password = root.getAttributeValue("password");
                int port = Integer.parseInt(root.getAttributeValue("port"));
                System.out.println("Client pretends to be " + login);
                Account acc = null;
                try {
                    acc = datamanager.getAccount(login);
                } catch (SQLException ex) {
                    Logger.getLogger(AuthentificationState.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (acc != null) {
                    boolean isCorrect = verifyPassword(password, acc.getPasswordHash());
                    if (isCorrect) {
                        try {
                            SocketChannel prev = getOwnerThread().getSocketChannel(acc);
                            if (prev != null) {
                                getOwnerThread().closeSocket(prev);
                            }
                            System.out.println("Authentification successful");
                            String msg = "<message type=\"hello\" " + "success=\"true\" name=\"" + acc.getName() + "\" nickname=\"" + acc.getNickName() + "\"/>\n";
                            getOwnerThread().send(getSocket(), msg.getBytes("UTF-8"));
                            InetAddress addr = getSocket().socket().getInetAddress();
                            acc.setAddress(addr);
                            acc.setPort(port);
                            datamanager.updateAccount(acc);
                            getOwnerThread().setSocketChannel(acc, getSocket());
                            getOwnerThread().setSocketState(getSocket(), new NormalState(acc, getOwnerThread(), datamanager, getSocket()));
                            sendBuddiesAddress(acc);
                            sendAddressToBuddies(acc);
                        } catch (SQLException ex) {
                            Logger.getLogger(AuthentificationState.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(AuthentificationState.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    System.out.println("Authentification failed");
                    String err = "<message type=\"hello\" success=\"false\">\n";
                    getOwnerThread().send(getSocket(), err.getBytes("UTF-8"));
                }
            }
        } catch (JDOMException ex) {
            Logger.getLogger(AuthentificationState.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AuthentificationState.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Calcule le hash du mot de passe 
     * @param password le mot de passe à vérifier
     * @param hash Le hash du vrai mot de passe
     * @return true si le mot de passe correspond, false sinon.
     */
    private boolean verifyPassword(String password, byte[] hash) {
        boolean returnValue = false;
        try {
            MessageDigest msgDigest = MessageDigest.getInstance("SHA-1");
            msgDigest.update(password.getBytes("UTF-8"));
            byte[] digest = msgDigest.digest();
            returnValue = Arrays.equals(hash, digest);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(AuthentificationState.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(AuthentificationState.class.getName()).log(Level.SEVERE, null, ex);
        }
        return returnValue;
    }

    /**
     * Envoie à un compte la liste de ses contacts qui sont connectés.
     * @param acc
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */
    private void sendBuddiesAddress(Account acc) throws SQLException, IOException {
        List<Account> buddies = datamanager.getBuddies(acc);
        StringBuilder message = new StringBuilder("<message type=\"addresses\">");
        for (Account buddy : buddies) {
            Set<Account> blackList = datamanager.getHavingBlackListed(acc);
            if ((buddy.getAddress() != null) && (!blackList.contains(buddy))) {
                message.append(buddy.toXML());
            } else {
                buddy.setAddress(null);
                message.append(buddy.toXML());
            }
        }
        message.append("</message>\n");
        getOwnerThread().send(getSocket(), message.toString().getBytes("UTF-8"));
    }

    /**
     * Envoie la notification de nouvelle connection à tous les amis.
     * @param acc Le compte nouvellement connecté.
     * @throws java.sql.SQLException
     */
    private void sendAddressToBuddies(Account acc) throws SQLException, IOException {
        String message = "<message type=\"addresses\">" + acc.toXML() + "</message>\n";
        List<Account> buddies = datamanager.getHavingBuddy(acc);
        Set<Account> blackList = datamanager.getBlackList(acc);
        for (Account blacklisted : blackList) {
            buddies.remove(blacklisted);
        }
        for (Account buddy : buddies) {
            SocketChannel sc = getOwnerThread().getSocketChannel(buddy);
            if (sc != null) {
                getOwnerThread().send(sc, message.getBytes("UTF-8"));
            }
        }
    }
}
