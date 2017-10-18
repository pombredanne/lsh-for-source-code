package grimace.client;

import grimace.common.ContactList;
import grimace.common.Contact;
import grimace.common.Account;
import grimace.common.FileData;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import grimace.common.Command;
import java.security.MessageDigest;

/**
 * ServerHandler facilitates communication with the Wernicke server.
 * 
 * @author Vineet Sharma
 */
public final class ServerHandler {

    public static final String DEF_SERVER_HOSTNAME = "localhost";

    public static final int DEF_SERVER_PORT = 6373;

    private static Thread listen;

    private static Socket socket;

    private static ObjectOutputStream out;

    private static ObjectInputStream in;

    private static Command fromServer;

    private static boolean run = false;

    /**
     * Connects to the server on the SERVER_PORT.
     *
     * @throws java.net.UnknownHostException
     * @throws java.io.IOException
     */
    private static void connect() throws UnknownHostException, IOException {
        socket = new Socket(ProgramController.getServerAddress(), ProgramController.getServerPort());
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Sends a command to the server.
     *
     * @param cmd   The command to send.
     * @throws java.lang.Exception
     */
    private static void sendCommand(Command cmd) throws Exception {
        if (!socket.isConnected()) {
            throw new Exception("Not connected");
        }
        out.writeObject(cmd);
    }

    /**
     * Creates and sends a command to the server.
     *
     * @param cmdName   The name of the command to send.
     * @param args      The arguments for the command.
     * @throws java.lang.Exception
     */
    private static void sendCommand(String cmdName, String... args) throws Exception {
        if (!socket.isConnected()) {
            throw new Exception("Not connected");
        }
        try {
            out.writeObject(new Command(cmdName, args));
        } catch (IOException e) {
            e.printStackTrace();
            ProgramController.showMessage("The connection to the server was closed. You will be logged out.");
            logout();
        }
    }

    /**
     * Merges a set of arguments and appends an array of arguments
     * to the resulting array.
     *
     * @param append    Arguments to append to the merged array.
     * @param args      Arguments to merge.
     * @return          A String array containing all arguments.
     */
    public static String[] mergeStrings(String[] append, String... args) {
        String[] merged = new String[append.length + args.length];
        for (int i = 0; i < args.length; i++) {
            merged[i] = args[i];
        }
        for (int i = args.length; i < args.length + append.length; i++) {
            merged[i] = append[i - args.length];
        }
        return merged;
    }

    public static String[] getStringArrayFromList(ArrayList<String> strings) {
        int len = strings.size();
        String[] sArr = new String[len];
        for (int i = 0; i < len; i++) {
            sArr[i] = strings.get(i);
        }
        return sArr;
    }

    /**
     * Returns a hash of the given password string using the SHA-1 algorithm.
     *
     * @param password  The string to hash.
     * @return  A string of hex digits representing the hashed value.
     */
    public static synchronized String getPasswordHash(String password) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            return "";
        }
        StringBuffer hash;
        try {
            byte[] hashBytes = md.digest(password.getBytes("UTF-8"));
            hash = new StringBuffer(hashBytes.length * 2);
            for (byte b : hashBytes) {
                String hex = String.format("%02X", b);
                hash.append(hex);
            }
        } catch (Exception e) {
            return "";
        }
        return hash.toString();
    }

    /**
     * Sends a request to register a new user.
     *
     * @param userName  The userName to register with.
     * @param password  The password to register with.
     * @throws java.lang.Exception
     */
    public static Command sendRegisterRequest(String userName, String password, String displayName) {
        try {
            connect();
            sendCommand(Command.REGISTER, userName.toLowerCase(), getPasswordHash(password), displayName);
            Command response = (Command) in.readObject();
            out.close();
            in.close();
            socket.close();
            return response;
        } catch (Exception e) {
            return new Command(Command.REGISTER_FAILURE, Command.SERVER_CONNECT_FAILURE);
        }
    }

    /**
     * Sends a request to login a user.
     *
     * @param userName The name of the user to login as.
     * @param password  The password for the user's account.
     * @return  True if the login was successful, false otherwise.
     * @throws java.lang.Exception
     */
    public static boolean sendLoginRequest(String userName, String password, String status) {
        try {
            connect();
            sendCommand(Command.LOGIN, userName.toLowerCase(), getPasswordHash(password));
            Command response = (Command) in.readObject();
            if (!response.getCommandName().equals(Command.LOGIN_SUCCESS)) {
                out.close();
                in.close();
                socket.close();
                ProgramController.showMessage("Login Failed: " + "You have entered an invalid username/password pair.");
                return false;
            } else {
                Account acc = (Account) in.readObject();
                ProgramController.setAccount(acc);
                ProgramController.setAccountStatus(status);
                listen = new Thread(new Runnable() {

                    public void run() {
                        listen();
                    }
                });
                run = true;
                listen.start();
            }
            return true;
        } catch (Exception e) {
            ProgramController.showMessage("Login Failed: " + "Unable to connect to the server.");
            return false;
        }
    }

    /**
     * Waits for commands from the server and executes them.
     */
    private static void listen() {
        while (run) {
            try {
                Thread.sleep(100);
                fromServer = (Command) in.readObject();
                if (fromServer.getCommandName().equals(Command.DISPLAY_NOTIFICATION)) {
                    ProgramController.showMessage(fromServer.getCommandArg(0));
                }
                if (fromServer.getCommandName().equals(Command.CONTACT_REQUEST)) {
                    int resp = ProgramController.showRequestDialog("You have a contact request from " + fromServer.getCommandArg(0) + ". What would you like to do?\n" + "If you choose to ignore this message your contact " + "requests will be displayed on your next login.");
                    switch(resp) {
                        case 0:
                            sendContactRequestResponse(fromServer.getCommandArg(0), ProgramController.getUserName(), Command.ACCEPT);
                            break;
                        case 1:
                            sendContactRequestResponse(fromServer.getCommandArg(0), ProgramController.getUserName(), Command.REJECT);
                            break;
                        case 2:
                            break;
                    }
                }
                if (fromServer.getCommandName().equals(Command.UPDATE_CONTACT_LIST)) {
                    ContactList cList = (ContactList) in.readObject();
                    ProgramController.getAccount().setContactList(cList);
                    ProgramController.updateContactList();
                }
                if (fromServer.getCommandName().equals(Command.START_CONVERSATION)) {
                    int conId = Integer.valueOf(fromServer.getCommandArg(0)).intValue();
                    ContactList cList = (ContactList) in.readObject();
                    Contact user = cList.getContact(ProgramController.getUserName());
                    if (user != null) {
                        cList.removeContact(user);
                    }
                    ProgramController.openNewConvo(conId, cList);
                }
                if (fromServer.getCommandName().equals(Command.SEND_MESSAGE)) {
                    int conId = Integer.valueOf(fromServer.getCommandArg(2)).intValue();
                    String message = fromServer.getCommandArg(1);
                    String sender = fromServer.getCommandArg(0);
                    ProgramController.postMessage(conId, message, sender);
                }
                if (fromServer.getCommandName().equals(Command.UPDATE_CONTACT)) {
                    Contact newcon = (Contact) in.readObject();
                    Contact con = ProgramController.getContactList().getContact(newcon.getUserName());
                    con.setDisplayName(newcon.getDisplayName());
                    con.setStatus(newcon.getStatus());
                    ProgramController.updateContactList();
                }
                if (fromServer.getCommandName().equals(Command.UPDATE_CONVO_CONTACT)) {
                    int conId = Integer.valueOf(fromServer.getCommandArg(1)).intValue();
                    Contact newcon = (Contact) in.readObject();
                    ChatPanel chat = ProgramController.getChatPanel(conId);
                    Contact con = chat.getClientConversation().getList().getContact(newcon.getUserName());
                    con.setDisplayName(newcon.getDisplayName());
                    con.setStatus(newcon.getStatus());
                    chat.getContactListBox().updateModel();
                }
                if (fromServer.getCommandName().equals(Command.FILE_TRANSFER_REQUEST)) {
                    String sender = fromServer.getCommandArg(0);
                    String filePath = fromServer.getCommandArg(1);
                    String file = new File(filePath).getName();
                    int resp = javax.swing.JOptionPane.showConfirmDialog(ProgramController.getWindow(), "You have a file transfer request from " + sender + ".\n" + sender + " wants to send you the file \'" + file + "\'.\n" + "Accept transfer?\n", "File Transfer Request", javax.swing.JOptionPane.YES_NO_OPTION);
                    switch(resp) {
                        case javax.swing.JOptionPane.YES_OPTION:
                            sendFileTransferResponse(sender, filePath, ProgramController.getUserName(), Command.ACCEPT);
                            break;
                        case javax.swing.JOptionPane.NO_OPTION:
                            sendFileTransferResponse(sender, filePath, ProgramController.getUserName(), Command.REJECT);
                            break;
                    }
                }
                if (fromServer.getCommandName().equals(Command.FILE_TRANSFER_RESPONSE)) {
                    String fileName = fromServer.getCommandArg(1);
                    String contact = fromServer.getCommandArg(2);
                    String confirm = fromServer.getCommandArg(3);
                    if (confirm.equals(Command.ACCEPT)) {
                        File file = new File(fileName);
                        if (file.exists()) {
                            sendCommand(Command.TRANSFER_FILE, ProgramController.getUserName(), fileName, contact);
                            try {
                                FileData fileData = new FileData(new File(fileName));
                                out.writeObject(fileData);
                                ProgramController.showMessage("Request to transfer file \'" + new File(fileName).getName() + "\' was accepted by " + contact + ".");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            ProgramController.showMessage("The file \'" + file.getName() + "\' could not be send because it does not exist.");
                        }
                    }
                }
                if (fromServer.getCommandName().equals(Command.REMOVE_FROM_CONVERSATION)) {
                    int conId = Integer.valueOf(fromServer.getCommandArg(1)).intValue();
                    String contact = fromServer.getCommandArg(0);
                    ProgramController.removeFromConversation(contact, conId);
                }
                if (fromServer.getCommandName().equals(Command.ADD_TO_CONVERSATION)) {
                    int conId = Integer.valueOf(fromServer.getCommandArg(1)).intValue();
                    Contact contact = (Contact) in.readObject();
                    ProgramController.addToConversation(contact, conId);
                }
                if (fromServer.getCommandName().equals(Command.TRANSFER_FILE)) {
                    String userName = fromServer.getCommandArg(0);
                    String fileName = fromServer.getCommandArg(1);
                    String fname = new File(fileName).getName();
                    try {
                        FileData fileData = (FileData) in.readObject();
                        int i = 0;
                        File file;
                        do {
                            file = new File(ProgramController.RECEIVED_FOLDER + "/" + String.valueOf(i++) + "_" + fname);
                        } while (file.exists());
                        fileData.saveFileData(file);
                        ProgramController.showMessage("Received file \'" + file.getName() + "\' from " + userName + ".\n" + "\nFile location: " + file.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                        ProgramController.showMessage("There was a problem receiving the file \'" + fname + "\' from " + userName + ".");
                    }
                }
            } catch (EOFException e) {
            } catch (SocketException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a request to the server to add a contact to an account.
     *
     * @param contactName The name of the contact being requested.
     * @throws java.lang.Exception
     */
    public static void sendAddContactRequest(String contactName) throws Exception {
        if (contactName.equals(ProgramController.getUserName())) {
            ProgramController.showMessage("You cannot add yourself to" + "your own contact list.");
            return;
        }
        sendCommand(Command.CONTACT_REQUEST, ProgramController.getUserName(), contactName.toLowerCase());
    }

    /**
     * Sends a response to a contact request given by the server.
     *
     * @param userName The name of the user initiating the request.
     * @param contactName The name of the user responding.
     * @param response Whether or not the request is accepted.
     * @throws java.lang.Exception
     */
    public static void sendContactRequestResponse(String userName, String contactName, String response) throws Exception {
        sendCommand(Command.CONTACT_REQUEST_RESPONSE, userName, contactName, response);
    }

    /**
     * Sends a request to the server to delete a contact from an account.
     *
     * @param contactName   The name of the contact to delete.
     * @throws java.lang.Exception
     */
    public static void sendDeleteContactRequest(String contactName) throws Exception {
        sendCommand(Command.DELETE_CONTACT, ProgramController.getUserName(), contactName);
    }

    /**
     * Sends a request to start a conversation with one or more contacts.
     *
     * @param userName      The user initiating the conversation.
     * @param contacts  The names of the contacts requested.
     * @throws java.lang.Exception
     */
    public static void sendConversationRequest(String userName, Contact[] contacts) throws Exception {
        String[] names = new String[contacts.length];
        for (int i = 0; i < contacts.length; i++) {
            names[i] = contacts[i].getUserName();
        }
        String[] args = mergeStrings(names, userName);
        sendCommand(Command.START_CONVERSATION, args);
    }

    /**
     * Sends a request to the server to post a message to a conversation.
     *
     * @param message   The message being sent.
     * @param conId     An integer identifying a target conversation.
     * @throws java.lang.Exception
     */
    public static void sendMessagePostRequest(String message, int conId) throws Exception {
        sendCommand(Command.SEND_MESSAGE, ProgramController.getUserName(), message, String.valueOf(conId));
    }

    /**
     * Sends a notification to the server that a contact has left a
     * conversation.
     *
     * @param conId An integer identifying the conversation.
     * @throws java.lang.Exception
     */
    public static void sendQuitConversationNotification(int conId) throws Exception {
        sendCommand(Command.REMOVE_FROM_CONVERSATION, ProgramController.getUserName(), String.valueOf(conId));
    }

    /**
     * Sends a request to the server to add a contact to a converation.
     *
     * @param contactName The contact to add.
     * @param conId An integer identifying the conversation.
     * @throws java.lang.Exception
     */
    public static void sendAddToConversationRequest(String contactName, int conId) throws Exception {
        sendCommand(Command.ADD_TO_CONVERSATION, contactName, String.valueOf(conId), ProgramController.getUserName());
    }

    /**
     * Sends a request to the server to transfer a file to one or more contacts.
     *
     * @param fileName The name of the file to be sent.
     * @param contactNames The names of contacts receiving the file.
     * @throws java.lang.Exception
     */
    public static void sendFileTransferRequest(String fileName, String contactName) throws Exception {
        sendCommand(Command.FILE_TRANSFER_REQUEST, ProgramController.getUserName(), fileName, contactName);
    }

    /**
     * Sends a response to a file transfer request given by the server.
     *
     * @param userName  The name of the user initiating the request.
     * @param contactName The name of the user responding.
     * @param response  Whether or not the request is accepted.
     * @throws java.lang.Exception
     */
    public static void sendFileTransferResponse(String userName, String fileName, String contactName, String response) throws Exception {
        sendCommand(Command.FILE_TRANSFER_RESPONSE, userName, fileName, contactName, response);
    }

    /**
     * Sends a request to the server to update an account display name.
     *
     * @param account The account to update.
     * @throws java.lang.Exception
     */
    public static void sendDisplayNameUpdateRequest() throws Exception {
        int[] idList = ProgramController.getConIdList();
        String[] ids = new String[idList.length];
        for (int i = 0; i < idList.length; i++) {
            ids[i] = String.valueOf(idList[i]);
        }
        String[] args = mergeStrings(ids, ProgramController.getUserName(), ProgramController.getDisplayName());
        sendCommand(Command.UPDATE_DISPLAY_NAME, args);
    }

    /**
     * Sends a request to the server to update an account.
     *
     * @param account The account to update.
     * @throws java.lang.Exception
     */
    public static void sendStatusUpdateRequest() throws Exception {
        sendCommand(Command.UPDATE_STATUS, ProgramController.getUserName(), ProgramController.getAccountStatus());
    }

    /**
     * Sends a request to the server to update an account.
     *
     * @param account The account to update.
     * @throws java.lang.Exception
     */
    public static void sendFontUpdateRequest() throws Exception {
        Account account = ProgramController.getAccount();
        sendCommand(Command.UPDATE_FONT, ProgramController.getUserName(), account.getFont().getFontName(), String.valueOf(account.getFont().getSize()), String.valueOf(account.getFontColour().getRGB()), String.valueOf(account.getFont().isItalic()), String.valueOf(account.getFont().isBold()));
    }

    public static void sendLogoutRequest() {
        try {
            sendCommand(Command.LOGOUT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logout();
    }

    public static void logout() {
        try {
            run = false;
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ProgramController.showLoginForm();
    }
}
