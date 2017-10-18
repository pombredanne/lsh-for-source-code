package libsecondlife;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;
import libsecondlife.packets.*;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.xmlrpc.*;

public class NetworkManager implements PacketCallback {

    public LLUUID AgentID;

    public LLUUID SessionID;

    public String LoginError;

    public Simulator CurrentSim;

    public Hashtable LoginValues;

    public boolean getConnected() {
        return connected;
    }

    public SimDisconnectCallback OnSimDisconnected;

    public DisconnectCallback OnDisconnected;

    private IntHashMap Callbacks;

    private SecondLife Client;

    private Vector Simulators;

    private Timer DisconnectTimer;

    private boolean connected;

    public void packetCallback(Packet packet, Simulator simulator) throws Exception {
        switch(packet.getType()) {
            case PacketType.RegionHandshake:
                RegionHandshakeHandler(packet, simulator);
                break;
            case PacketType.StartPingCheck:
                StartPingCheckHandler(packet, simulator);
                break;
            case PacketType.ParcelOverlay:
                ParcelOverlayHandler(packet, simulator);
                break;
            case PacketType.EnableSimulator:
                EnableSimulatorHandler(packet, simulator);
                break;
            case PacketType.KickUser:
                KickUserHandler(packet, simulator);
                break;
        }
    }

    public NetworkManager(SecondLife client) throws Exception {
        Client = client;
        Simulators = new Vector();
        Callbacks = new IntHashMap();
        CurrentSim = null;
        LoginValues = null;
        RegisterCallback(PacketType.RegionHandshake, this);
        RegisterCallback(PacketType.StartPingCheck, this);
        RegisterCallback(PacketType.ParcelOverlay, this);
        RegisterCallback(PacketType.EnableSimulator, this);
        RegisterCallback(PacketType.KickUser, this);
        DisconnectTimer = new Timer();
        DisconnectTimer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                try {
                    DisconnectTimer_Elapsed();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 60000, 60000);
    }

    public void RegisterCallback(int type, PacketCallback callback) {
        if (!Callbacks.containsKey(type)) {
            Callbacks.put(type, new Vector());
        }
        Vector callbackArray = (Vector) Callbacks.get(type);
        callbackArray.addElement(callback);
    }

    public void UnregisterCallback(int type, PacketCallback callback) {
        if (!Callbacks.containsKey(type)) {
            Client.Log("Trying to unregister a callback for packet " + type + " when no callbacks are setup for that packet", Helpers.LogLevel.Info);
            return;
        }
        Vector callbackArray = (Vector) Callbacks.get(type);
        if (callbackArray.contains(callback)) {
            callbackArray.remove(callback);
        } else {
            Client.Log("Trying to unregister a non-existant callback for packet " + type, Helpers.LogLevel.Info);
        }
    }

    public void SendPacket(Packet packet) throws Exception {
        if (CurrentSim != null && CurrentSim.getConnected()) {
            CurrentSim.SendPacket(packet, true);
        }
    }

    public void SendPacket(Packet packet, Simulator simulator) throws Exception {
        if (simulator.getConnected()) {
            simulator.SendPacket(packet, true);
        }
    }

    public void SendPacket(byte[] payload) throws Exception {
        if (CurrentSim != null) {
            CurrentSim.SendPacket(payload);
        } else {
            throw new NotConnectedException();
        }
    }

    public static Hashtable DefaultLoginValues(String firstName, String lastName, String password, String userAgent, String author) throws Exception {
        return DefaultLoginValues(firstName, lastName, password, "00:00:00:00:00:00", "last", 1, 50, 50, 50, "Win", "0", userAgent, author);
    }

    public static Hashtable DefaultLoginValues(String firstName, String lastName, String password, String mac, String startLocation, String platform, String viewerDigest, String userAgent, String author) throws Exception {
        return DefaultLoginValues(firstName, lastName, password, mac, startLocation, 1, 50, 50, 50, platform, viewerDigest, userAgent, author);
    }

    public static Hashtable DefaultLoginValues(String firstName, String lastName, String password, String mac, String startLocation, int major, int minor, int patch, int build, String platform, String viewerDigest, String userAgent, String author) throws Exception {
        Hashtable values = new Hashtable();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(password.getBytes("ASCII"), 0, password.length());
        byte[] raw_digest = md5.digest();
        String passwordDigest = Helpers.toHexText(raw_digest);
        values.put("first", firstName);
        values.put("last", lastName);
        values.put("passwd", "" + password);
        values.put("start", startLocation);
        values.put("major", major);
        values.put("minor", minor);
        values.put("patch", patch);
        values.put("build", build);
        values.put("platform", platform);
        values.put("mac", mac);
        values.put("agree_to_tos", "true");
        values.put("viewer_digest", viewerDigest);
        values.put("user-agent", userAgent + " (" + Helpers.VERSION + ")");
        values.put("author", author);
        Vector optionsArray = new Vector();
        optionsArray.addElement("inventory-root");
        optionsArray.addElement("inventory-skeleton");
        optionsArray.addElement("inventory-lib-root");
        optionsArray.addElement("inventory-lib-owner");
        optionsArray.addElement("inventory-skel-lib");
        optionsArray.addElement("initial-outfit");
        optionsArray.addElement("gestures");
        optionsArray.addElement("event_categories");
        optionsArray.addElement("event_notifications");
        optionsArray.addElement("classified_categories");
        optionsArray.addElement("buddy-list");
        optionsArray.addElement("ui-config");
        optionsArray.addElement("login-flags");
        optionsArray.addElement("global-textures");
        values.put("options", optionsArray);
        return values;
    }

    public boolean Login(Hashtable loginParams) throws Exception {
        return Login(loginParams, SecondLife.LOGIN_SERVER);
    }

    public boolean Login(Hashtable loginParams, String url) throws Exception {
        Object result;
        try {
            XmlRpcClient client = new XmlRpcClient(url);
            Vector params = new Vector();
            params.addElement(loginParams);
            System.out.println("Logging in at " + url);
            result = client.execute("login_to_simulator", params);
        } catch (Exception e) {
            e.printStackTrace();
            LoginError = e.toString();
            LoginValues = null;
            return false;
        }
        LoginValues = (Hashtable) result;
        for (Enumeration keys = LoginValues.keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            System.out.println(key.toString() + "=" + LoginValues.get(key).toString() + " (" + LoginValues.get(key).getClass().getName() + ") ");
        }
        if (LoginValues.get("login").toString().equals("indeterminate")) {
            LoginError = "Got a redirect, login with the official client to update";
            return false;
        } else if (LoginValues.get("login").toString().equals("false")) {
            LoginError = LoginValues.get("reason") + ": " + LoginValues.get("message");
            return false;
        } else if (LoginValues.get("login").toString().equals("true") == false) {
            LoginError = "Unknown error";
            return false;
        }
        try {
            this.AgentID = new LLUUID((String) LoginValues.get("agent_id"));
            this.SessionID = new LLUUID((String) LoginValues.get("session_id"));
            Client.Self.ID = this.AgentID;
            Client.Self.FirstName = (String) LoginValues.get("first_name");
            Client.Self.LastName = (String) LoginValues.get("last_name");
            Simulator simulator = new Simulator(Client, this.Callbacks, ((Integer) LoginValues.get("circuit_code")).intValue(), InetAddress.getByName((String) LoginValues.get("sim_ip")), ((Integer) LoginValues.get("sim_port")).intValue());
            if (!simulator.getConnected()) {
                System.out.println("Sim not connected");
                return false;
            }
            CurrentSim = simulator;
            Simulators.addElement(simulator);
            System.out.println("Moving our agent in to the sim to complete the connection");
            Client.Self.CompleteAgentMovement(simulator);
            System.out.println("Sending Initial Packets");
            SendInitialPackets();
            connected = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Client.Log("Login error: " + e.toString(), Helpers.LogLevel.Error);
            return false;
        }
    }

    public Simulator Connect(InetAddress ip, int port, int circuitCode, boolean setDefault) throws Exception {
        Simulator simulator = new Simulator(Client, this.Callbacks, circuitCode, ip, (int) port);
        if (!simulator.getConnected()) {
            simulator = null;
            return null;
        }
        synchronized (Simulators) {
            Simulators.addElement(simulator);
        }
        if (setDefault) {
            CurrentSim = simulator;
        }
        connected = true;
        return simulator;
    }

    public void Logout() throws Exception {
        if (CurrentSim == null || !connected) {
            return;
        }
        Client.Log("Logging out", Helpers.LogLevel.Info);
        DisconnectTimer.cancel();
        connected = false;
        LogoutRequestPacket logout = new LogoutRequestPacket();
        logout.AgentData.AgentID = AgentID;
        logout.AgentData.SessionID = SessionID;
        CurrentSim.SendPacket(logout, true);
        Shutdown();
        if (OnDisconnected != null) {
            OnDisconnected.disconnectCallback(DisconnectType.ClientInitiated, "");
        }
    }

    public void DisconnectSim(Simulator sim) throws Exception {
        sim.Disconnect();
        if (OnSimDisconnected != null) OnSimDisconnected.callback(sim, DisconnectType.NetworkTimeout);
        synchronized (Simulators) {
            Simulators.remove(sim);
        }
    }

    private void Shutdown() throws Exception {
        synchronized (Simulators) {
            for (int i = 0; i < Simulators.size(); i++) {
                Simulator simulator = (Simulator) Simulators.elementAt(i);
                if (simulator != CurrentSim) {
                    simulator.Disconnect();
                    if (OnSimDisconnected != null) OnSimDisconnected.callback(simulator, DisconnectType.NetworkTimeout);
                }
            }
            Simulators.clear();
        }
        CurrentSim.Disconnect();
        CurrentSim = null;
    }

    private void SendInitialPackets() throws Exception {
        SendPacket(new EconomyDataRequestPacket());
        AgentThrottlePacket throttle = new AgentThrottlePacket();
        throttle.AgentData.AgentID = this.AgentID;
        throttle.AgentData.SessionID = this.SessionID;
        throttle.AgentData.CircuitCode = this.CurrentSim.getCircuitCode();
        throttle.Throttle.GenCounter = 0;
        throttle.Throttle.setThrottles(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x96, (byte) 0x47, (byte) 0x00, (byte) 0x00, (byte) 0xAA, (byte) 0x47, (byte) 0x00, (byte) 0x00, (byte) 0x88, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0x88, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0x5F, (byte) 0x48, (byte) 0x00, (byte) 0x00, (byte) 0x5F, (byte) 0x48, (byte) 0x00, (byte) 0x00, (byte) 0xDC, (byte) 0x47 });
        SendPacket(throttle);
        Client.Self.SetHeightWidth((short) 676, (short) 909);
        Client.Self.UpdateCamera(true);
        SetAlwaysRunPacket run = new SetAlwaysRunPacket();
        run.AgentData.AgentID = AgentID;
        run.AgentData.SessionID = SessionID;
        run.AgentData.AlwaysRun = false;
        SendPacket(run);
        MuteListRequestPacket mute = new MuteListRequestPacket();
        mute.AgentData.AgentID = AgentID;
        mute.AgentData.SessionID = SessionID;
        mute.MuteData.MuteCRC = 0;
        SendPacket(mute);
        MoneyBalanceRequestPacket money = new MoneyBalanceRequestPacket();
        money.AgentData.AgentID = AgentID;
        money.AgentData.SessionID = SessionID;
        money.MoneyData.TransactionID = new LLUUID();
        SendPacket(money);
    }

    private void DisconnectTimer_Elapsed() throws Exception {
        if (CurrentSim != null && CurrentSim.DisconnectCandidate) {
            Client.Log("Network timeout for the current simulator (" + CurrentSim.Region.Name + "), logging out", Helpers.LogLevel.Warning);
            DisconnectTimer.cancel();
            connected = false;
            Shutdown();
            if (OnDisconnected != null) {
                OnDisconnected.disconnectCallback(DisconnectType.NetworkTimeout, "");
            }
            return;
        }
        Vector disconnectedSims = null;
        synchronized (Simulators) {
            for (int i = 0; i < Simulators.size(); i++) {
                Simulator sim = (Simulator) Simulators.elementAt(i);
                if (sim.DisconnectCandidate) {
                    if (disconnectedSims == null) {
                        disconnectedSims = new Vector();
                    }
                    disconnectedSims.addElement(sim);
                } else {
                    sim.DisconnectCandidate = true;
                }
            }
        }
        if (disconnectedSims != null) {
            for (int i = 0; i < Simulators.size(); i++) {
                Simulator sim = (Simulator) disconnectedSims.elementAt(i);
                Client.Log("Network timeout for simulator " + sim.Region.Name + ", disconnecting", Helpers.LogLevel.Warning);
                DisconnectSim(sim);
            }
        }
    }

    private void StartPingCheckHandler(Packet packet, Simulator simulator) throws Exception {
        StartPingCheckPacket incomingPing = (StartPingCheckPacket) packet;
        CompletePingCheckPacket ping = new CompletePingCheckPacket();
        ping.PingID.PingID = incomingPing.PingID.PingID;
        SendPacket((Packet) ping, simulator);
    }

    private void RegionHandshakeHandler(Packet packet, Simulator simulator) throws Exception {
        RegionHandshakeReplyPacket reply = new RegionHandshakeReplyPacket();
        reply.AgentData.AgentID = AgentID;
        reply.AgentData.SessionID = SessionID;
        reply.RegionInfo.Flags = 0;
        SendPacket(reply, simulator);
        RegionHandshakePacket handshake = (RegionHandshakePacket) packet;
        simulator.Region.ID = handshake.RegionInfo.CacheID;
        simulator.Region.IsEstateManager = handshake.RegionInfo.IsEstateManager;
        simulator.Region.Name = Helpers.FieldToString(handshake.RegionInfo.getSimName());
        simulator.Region.SimOwner = handshake.RegionInfo.SimOwner;
        simulator.Region.TerrainBase0 = handshake.RegionInfo.TerrainBase0;
        simulator.Region.TerrainBase1 = handshake.RegionInfo.TerrainBase1;
        simulator.Region.TerrainBase2 = handshake.RegionInfo.TerrainBase2;
        simulator.Region.TerrainBase3 = handshake.RegionInfo.TerrainBase3;
        simulator.Region.TerrainDetail0 = handshake.RegionInfo.TerrainDetail0;
        simulator.Region.TerrainDetail1 = handshake.RegionInfo.TerrainDetail1;
        simulator.Region.TerrainDetail2 = handshake.RegionInfo.TerrainDetail2;
        simulator.Region.TerrainDetail3 = handshake.RegionInfo.TerrainDetail3;
        simulator.Region.TerrainHeightRange00 = handshake.RegionInfo.TerrainHeightRange00;
        simulator.Region.TerrainHeightRange01 = handshake.RegionInfo.TerrainHeightRange01;
        simulator.Region.TerrainHeightRange10 = handshake.RegionInfo.TerrainHeightRange10;
        simulator.Region.TerrainHeightRange11 = handshake.RegionInfo.TerrainHeightRange11;
        simulator.Region.TerrainStartHeight00 = handshake.RegionInfo.TerrainStartHeight00;
        simulator.Region.TerrainStartHeight01 = handshake.RegionInfo.TerrainStartHeight01;
        simulator.Region.TerrainStartHeight10 = handshake.RegionInfo.TerrainStartHeight10;
        simulator.Region.TerrainStartHeight11 = handshake.RegionInfo.TerrainStartHeight11;
        simulator.Region.WaterHeight = handshake.RegionInfo.WaterHeight;
        Client.Log("Received a region handshake for " + simulator.Region.Name, Helpers.LogLevel.Info);
    }

    private void ParcelOverlayHandler(Packet packet, Simulator simulator) {
        ParcelOverlayPacket overlay = (ParcelOverlayPacket) packet;
        if (overlay.ParcelData.SequenceID >= 0 && overlay.ParcelData.SequenceID <= 3) {
            Array.Copy(overlay.ParcelData.getData(), 0, simulator.Region.ParcelOverlay, overlay.ParcelData.SequenceID * 1024, 1024);
            simulator.Region.ParcelOverlaysReceived++;
            if (simulator.Region.ParcelOverlaysReceived > 3) {
                Client.Log("Finished building the " + simulator.Region.Name + " parcel overlay", Helpers.LogLevel.Info);
            }
        } else {
            Client.Log("Parcel overlay with sequence ID of " + overlay.ParcelData.SequenceID + " received from " + simulator.Region.Name, Helpers.LogLevel.Warning);
        }
    }

    private void EnableSimulatorHandler(Packet packet, Simulator simulator) {
    }

    private void KickUserHandler(Packet packet, Simulator simulator) throws Exception {
        String message = Helpers.FieldToString(((KickUserPacket) packet).UserInfo.getReason());
        Shutdown();
        if (OnDisconnected != null) {
            OnDisconnected.disconnectCallback(DisconnectType.ServerInitiated, message);
        }
    }
}
