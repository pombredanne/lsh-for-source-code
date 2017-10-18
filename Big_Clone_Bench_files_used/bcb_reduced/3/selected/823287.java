package rath.nateon;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.SocketException;
import java.util.*;
import java.security.MessageDigest;

/**
 * 네이트온 메신져 클래스
 * 
 * @author Jang-Ho Hwang, rath@xrath.com
 * @version 1.0.000, 2005/05/20
 */
public class NotificationChannel extends NateOnChannel {

    private final NateOnMessenger nateon;

    private ArrayList friends = new ArrayList();

    private NateUser own;

    private String ticket;

    private Hashtable chatMap = new Hashtable();

    public NotificationChannel(NateOnMessenger nateon, String host, int port) {
        super(host, port);
        this.nateon = nateon;
    }

    protected void processMessage(NateOnMessage msg) throws IOException {
        String head = msg.getHeader();
        if (head.equals("PING")) {
            NateOnMessage out = new NateOnMessage("PING");
            writeMessage(out);
            out.setHeader("PONG");
            writeMessage(out);
        } else if (head.equals("ADDB")) {
            processAdded(msg);
        } else if (head.equals("CTOC")) {
            processCTOC(msg);
        } else if (head.equals("INVT")) {
            processInvite(msg);
        } else if (head.equals("NTFY")) {
            processNotify(msg);
        } else if (head.equals("GWBP")) {
            processGWBP(msg);
        }
    }

    protected void channelConnected() throws IOException {
        MessageDigest md = null;
        String digest = "";
        try {
            String userid = nateon.getUserId();
            if (userid.endsWith("@nate.com")) userid = userid.substring(0, userid.lastIndexOf('@'));
            md = MessageDigest.getInstance("MD5");
            md.update(nateon.getPassword().getBytes());
            md.update(userid.getBytes());
            byte[] bData = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bData) {
                int v = (int) b;
                v = v < 0 ? v + 0x100 : v;
                String s = Integer.toHexString(v);
                if (s.length() == 1) sb.append('0');
                sb.append(s);
            }
            digest = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        NateOnMessage out = new NateOnMessage("LSIN");
        out.add(nateon.getUserId()).add(digest).add("MD5").add("3.615");
        out.setCallback("processAuth");
        writeMessage(out);
    }

    public void processAuth(NateOnMessage msg) throws IOException {
        nateon.isLoginProgress = false;
        String head = msg.getHeader();
        if (head.equals("301")) {
            nateon.isLoggedIn = false;
            nateon.fireLoginFailed();
            return;
        }
        nateon.isLoggedIn = true;
        long uid = msg.getLong(0);
        String name = msg.get(1);
        String nick = msg.get(2);
        String phone = msg.get(3);
        String email = msg.get(4);
        this.ticket = msg.get(5);
        NateUser user = new NateUser();
        user.setId(uid);
        user.setEmail(email);
        user.setName(name);
        user.setNickname(nick);
        this.own = user;
        NateOnMessage out = new NateOnMessage("CONF");
        out.add(0).add(0);
        out.setCallback("processConfigure");
        writeMessage(out);
    }

    public String getAuthenticatedTicket() {
        return this.ticket;
    }

    public NateUser getOwner() {
        return this.own;
    }

    public void processConfigure(NateOnMessage msg) throws IOException {
        String serial = msg.get(0);
        int len = msg.getInt(1);
        String info = new String(readBytes(len));
        NateOnMessage out = new NateOnMessage("GLST");
        out.add(0);
        out.setCallback("processGroupList");
        writeMessage(out);
    }

    public void processGroupList(NateOnMessage msg) throws IOException {
        if (msg.size() == 1) {
            return;
        }
        if (msg.size() >= 5) {
            int cur = msg.getInt(0);
            int max = msg.getInt(1);
            String flag = msg.get(2);
            String un = msg.get(3);
            String name = msg.get(4);
            if (max - 1 == cur) {
                NateOnMessage out = new NateOnMessage("LIST");
                out.setCallback("processUserList");
                writeMessage(out);
            }
        }
    }

    public void processUserList(NateOnMessage msg) throws IOException {
        int cur = msg.getInt(0);
        int max = msg.getInt(1);
        if (msg.size() == 2) {
            friends.clear();
        }
        if (msg.size() > 5) {
            String flag = msg.get(2);
            String email = msg.get(3);
            long uid = msg.getLong(4);
            String name = msg.get(5);
            String nick = msg.get(6);
            String phone = msg.get(7);
            NateUser user = new NateUser();
            user.setEmail(email);
            user.setId(uid);
            user.setName(name);
            user.setNickname(nick);
            friends.add(user);
            if (flag.equals("0001")) {
                if (nateon.isAutoAccept()) {
                    acceptUser(user);
                } else {
                    nateon.fireWhoAddMe(user);
                }
            }
        }
        if (max == 0 || max - 1 == cur) {
            NateOnMessage out = new NateOnMessage("ONST");
            out.add("O").add("0").add("%00").add(1);
            out.setCallback("processOnlineState");
            writeMessage(out);
        }
    }

    public void processOnlineState(NateOnMessage msg) throws IOException {
        nateon.fireLoginComplete(own);
    }

    public void processAdded(NateOnMessage msg) throws IOException {
        String list = msg.get(0);
        if (list.equals("RL")) {
            long uid = msg.getLong(1);
            String email = msg.get(2);
            String inviteMsg = msg.get(3);
            NateUser user = new NateUser();
            user.setId(uid);
            user.setEmail(email);
            if (nateon.isAutoAccept()) {
                acceptUser(user);
            } else {
                nateon.fireWhoAddMe(user);
            }
        }
    }

    /**
	 * 친구 추가 요청을 수락한다.
	 * ADSB 38 ACCPT 914490909 xrath@lycos.co.kr 0
	 * ADSB 38 ACCPT 914490909 ...... 2
	 */
    public void acceptUser(NateUser user) throws IOException {
        NateOnMessage out = new NateOnMessage("ADSB");
        out.add("ACCPT");
        out.add(user.getId());
        out.add(user.getEmail());
        out.add(0);
        writeMessage(out);
    }

    public void rejectUser(NateUser user) throws IOException {
    }

    protected void processCTOC(NateOnMessage msg) throws IOException {
        String target = msg.get(0);
        int len = msg.getInt(1);
        NateOnMessage invite = NateOnMessage.create(readBytes(len));
        processMessage(invite);
    }

    protected void processGWBP(NateOnMessage msg) throws IOException {
        int len = msg.getInt(4);
        byte[] b = readBytes(len);
        String smsdata = new String(b, "MS949");
        String[] lines = smsdata.split("\r\n");
        if (lines[0].indexOf("RECVSMS") != -1) {
            String[] smscontent = lines[4].split(" ");
            if (smscontent.length >= 3) {
                String from = smscontent[0];
                String to = smscontent[1];
                String content = URLDecoder.decode(smscontent[2], "MS949");
                nateon.fireSMSReceived(from, to, content);
            } else {
                System.out.println("ERROR: lines[4] = " + lines[4]);
            }
        }
    }

    /**
	 * 인증된 네이트온 채널을 통해 건당 30원(2007.03.15 현재)이 과금되는
	 * SMS를 보낸다. 유무선 연동된 네이트온 채널을 통해 보내는 것이므로 
	 * From 번호를 지정할 수 없다. From 번호는 로그인한 사용자(당신)의 번호다.
	 * <p>
	 * @param to  받는 사람의 휴대폰 번호
	 * @param msg 80자 체크를 하지 않으므로 주의한다.
	 */
    public void sendSMS(String to, String msg) throws IOException {
        if (ticket == null) throw new IllegalStateException("Authenticate ticket is null");
        String sms = ticket + " " + to + " " + msg.replaceAll(" ", "%20");
        int smslen = sms.getBytes("EUC-KR").length;
        StringBuffer sb = new StringBuffer();
        sb.append("cmd:SENDSMS\r\n");
        sb.append("type:RQST\r\n");
        sb.append("length:");
        sb.append(smslen);
        sb.append("\r\n\r\n");
        sb.append(sms);
        String packet = sb.toString();
        int pktlen = packet.getBytes("EUC-KR").length;
        NateOnMessage out = new NateOnMessage("GWBP");
        out.add("%00");
        out.add(nateon.getUserId());
        out.add("A");
        out.add("talk:D3D97AF7-1B96-4992-BFF1-BF2D36852DE4");
        out.add(pktlen);
        writeMessage(out);
        this.out.write(packet.getBytes("EUC-KR"));
        this.out.flush();
        System.out.println(packet);
    }

    protected void processInvite(NateOnMessage msg) throws IOException {
        String email = msg.get(0);
        String host = msg.get(1);
        int port = msg.getInt(2);
        String sessionId = msg.get(3);
        ChatChannel chat = new ChatChannel(nateon, host, port);
        chat.setSessionId(sessionId);
        chat.start();
        chatMap.put(sessionId, chat);
    }

    public void createChannel() throws IOException {
        NateOnMessage out = new NateOnMessage("RESS");
        out.setCallback("processCreatedChannel");
        writeMessage(out);
    }

    public void processCreatedChannel(NateOnMessage msg) throws IOException {
        String host = msg.get(0);
        int port = msg.getInt(1);
        String sessionId = msg.get(2);
        ChatChannel chat = new ChatChannel(nateon, host, port);
        chat.setSessionId(sessionId);
        chat.start();
        chatMap.put(sessionId, chat);
        nateon.fireChannelCreated(chat);
    }

    public void invite(ChatChannel chat, String email) throws IOException {
        InetSocketAddress addr = chat.getTargetAddress();
        StringBuffer sb = new StringBuffer();
        sb.append("INVT ");
        sb.append(email);
        sb.append(' ');
        sb.append(addr.getAddress().getHostAddress());
        sb.append(' ');
        sb.append(addr.getPort());
        sb.append(' ');
        sb.append(chat.getSessionId());
        byte[] b = sb.toString().getBytes();
        NateOnMessage out = new NateOnMessage("CTOC");
        out.add(email);
        out.add("N");
        out.add(b.length);
        out.setCallback("checkInvite", chat.getSessionId());
        writeMessage(out);
        writeBytes(b);
    }

    public void checkInvite(NateOnMessage msg, Object param) throws IOException {
        String head = msg.getHeader();
        if (head.equals("PNAK")) {
            String sessionId = (String) param;
            ChatChannel chat = findChatChannel(sessionId);
            chat.cancel();
            nateon.fireChannelCancel(chat);
        }
    }

    public ChatChannel findChatChannel(String sessionId) {
        return (ChatChannel) chatMap.get(sessionId);
    }

    public ChatChannel removeChatChannel(String sessionId) {
        return (ChatChannel) chatMap.remove(sessionId);
    }

    protected void processNotify(NateOnMessage msg) throws IOException {
        String email = msg.get(0);
        String state = msg.get(1);
    }

    protected void channelClosed() {
        nateon.isLoggedIn = false;
        nateon.isLoginProgress = false;
        nateon.fireServerClosed();
    }

    protected void channelError(Exception e) {
        if (!(e instanceof SocketException)) e.printStackTrace();
        nateon.fireServerAborted(e);
    }

    protected void connectFailed(Exception e) {
        nateon.fireConnectFailed(e);
    }
}
