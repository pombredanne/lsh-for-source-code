package jkad.controller.handlers;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import jkad.builders.SHA1Digester;
import jkad.controller.ThreadGroupLocal;
import jkad.controller.handlers.Handler.Status;
import jkad.controller.handlers.request.FindNodeHandler;
import jkad.controller.handlers.request.FindValueHandler;
import jkad.controller.handlers.request.LoginHandler;
import jkad.controller.handlers.request.StoreHandler;
import jkad.controller.handlers.request.PingHandler;
import jkad.controller.handlers.response.FindNodeResponseHandler;
import jkad.controller.handlers.response.FindValueResponseHandler;
import jkad.controller.handlers.response.PingResponseHandler;
import jkad.controller.handlers.response.StoreResponseHandler;
import jkad.controller.io.JKadDatagramSocket;
import jkad.controller.io.SingletonSocket;
import jkad.controller.threads.CyclicThread;
import jkad.facades.storage.DataManagerFacade;
import jkad.facades.user.NetLocation;
import jkad.facades.user.UserFacade;
import jkad.protocol.KadProtocol;
import jkad.protocol.rpc.RPC;
import jkad.structures.buffers.RPCBuffer;
import jkad.structures.kademlia.KadNode;
import jkad.structures.kademlia.RPCInfo;
import jkad.tools.ToolBox;
import jkad.structures.kademlia.ContactsKAD;
import java.util.List;

public class Controller extends CyclicThread implements UserFacade {

    private static ThreadGroupLocal<BigInteger> myID;

    public static BigInteger getMyID() {
        if (myID == null) {
            myID = new ThreadGroupLocal<BigInteger>() {

                public BigInteger initialValue() {
                    JKadDatagramSocket socket = SingletonSocket.getInstance();
                    InetAddress ip = socket.getInetAddress() != null ? socket.getInetAddress() : socket.getLocalAddress();
                    Integer port = socket.getPort() != -1 ? socket.getPort() : socket.getLocalPort();
                    String idString = ip.getHostAddress() + ":" + port;
                    linea = (Thread.currentThread().getThreadGroup().getName() + " : ID generado para " + Thread.currentThread().getThreadGroup().getName() + " con direcci�n " + idString);
                    fich.writelog(linea);
                    System.out.println(linea);
                    BigInteger id = SHA1Digester.hash(idString);
                    linea = (Thread.currentThread().getThreadGroup().getName() + " : ID generado " + id);
                    fich.writelog(linea);
                    System.out.println(linea);
                    return id;
                }
            };
        }
        return myID.get();
    }

    public static BigInteger generateRPCID() {
        Long currentTime = System.currentTimeMillis();
        String myID = Controller.getMyID().toString(16);
        String rpcID = myID + currentTime;
        return SHA1Digester.hash(rpcID);
    }

    private ContactsKAD contacts;

    private RPCBuffer inputBuffer;

    private HashMap<BigInteger, RequestHandler> rpcIDMap;

    public Controller() {
        super(ToolBox.getReflectionTools().generateThreadName(Controller.class));
        contacts = new ContactsKAD(Controller.getMyID());
        inputBuffer = RPCBuffer.getReceivedBuffer();
        rpcIDMap = new HashMap<BigInteger, RequestHandler>();
        super.setRoundWait(50);
    }

    public void run() {
        contacts.run();
        super.run();
    }

    protected void cycleOperation() throws InterruptedException {
        while (!inputBuffer.isEmpty()) {
            RPCInfo rpcInfo = inputBuffer.remove();
            RPC rpc = rpcInfo.getRPC();
            String ip = rpcInfo.getIP();
            Integer port = rpcInfo.getPort();
            try {
                linea = (Thread.currentThread().getThreadGroup().getName() + " : Llegada de un nuevo paquete RPC del nodo con ID " + rpc.getSenderNodeID() + " y con IP y puerto: " + ip + " : " + port);
                fich.writelog(linea);
                System.out.println(linea);
                BigInteger node = rpc.getSenderNodeID();
                BigInteger ID = Controller.getMyID();
                BigInteger aux = node.xor(ID);
                int cont = 159;
                while (aux.compareTo(BigInteger.ONE) != 0) {
                    aux = aux.shiftRight(1);
                    cont--;
                }
                linea = (Thread.currentThread().getThreadGroup().getName() + " : El nodo que envio el paqutete RPC con ID " + rpc.getSenderNodeID() + " y con IP y puerto: " + ip + " : " + port + " debe ir en el kbucket " + cont);
                fich.writelog(linea);
                System.out.println(linea);
                KadNode senderNode = new KadNode(rpc.getSenderNodeID(), ip, port);
                KadNode result = contacts.addcontact(cont, senderNode);
                if (result != null) {
                    PingHandler handlerping = new PingHandler(Controller.getMyID());
                    BigInteger rpcID = generateRPCID();
                    handlerping.setRpcID(rpcID);
                    handlerping.setpingNode(result);
                    synchronized (rpcIDMap) {
                        rpcIDMap.put(rpcID, handlerping);
                    }
                    long maxWait = Long.parseLong(System.getProperty("jkad.findnode.maxwait"));
                    long startTime = System.currentTimeMillis();
                    boolean find = false;
                    handlerping.run();
                    while (!(find) && (System.currentTimeMillis() - startTime < maxWait)) {
                        if (!inputBuffer.isEmpty()) {
                            RPCInfo rpcInfo2 = inputBuffer.remove();
                            RPC rpc2 = rpcInfo2.getRPC();
                            BigInteger valor;
                            valor = rpc2.getRPCID();
                            if ((rpcID.compareTo(valor)) == 0) {
                                handlerping.addResult(rpcInfo2);
                                find = handlerping.getResponse();
                            } else inputBuffer.add(rpcInfo2);
                        }
                    }
                    if (!find) {
                        contacts.addandremove(cont, senderNode, result);
                        linea = (Thread.currentThread().getThreadGroup().getName() + ": Contacto con ID " + rpc.getSenderNodeID() + " y con IP y puerto: " + ip + " : " + port + "  ha podido ser a�adido");
                        fich.writelog(linea);
                        System.out.println(linea);
                    } else {
                        linea = (Thread.currentThread().getThreadGroup().getName() + " : Contacto con ID " + rpc.getSenderNodeID() + " y con IP y puerto: " + ip + " : " + port + "  descartado por el nodo");
                        fich.writelog(linea);
                    }
                    synchronized (rpcIDMap) {
                        rpcIDMap.remove(rpcID);
                    }
                }
                linea = (Thread.currentThread().getThreadGroup().getName() + " : Procesando tipo de paquete RPC " + rpc.getClass().getSimpleName() + " del nodo con ID " + rpc.getSenderNodeID() + " y con IP y puerto " + rpcInfo.getIPAndPort());
                fich.writelog(linea);
                System.out.println(linea);
                if (rpc.isRequest()) {
                    switch(rpc.getType()) {
                        case KadProtocol.PING:
                            PingResponseHandler pingHandler = new PingResponseHandler();
                            pingHandler.setRPCInfo(rpcInfo);
                            pingHandler.run();
                            break;
                        case KadProtocol.STORE:
                            StoreResponseHandler storeHandler = new StoreResponseHandler();
                            storeHandler.setRPCInfo(rpcInfo);
                            storeHandler.run();
                            break;
                        case KadProtocol.FIND_NODE:
                            FindNodeResponseHandler findNodeHandler = new FindNodeResponseHandler();
                            findNodeHandler.setRPCInfo(rpcInfo);
                            findNodeHandler.setContacts(contacts);
                            findNodeHandler.run();
                            break;
                        case KadProtocol.FIND_VALUE:
                            FindValueResponseHandler findValueHandler = new FindValueResponseHandler();
                            findValueHandler.setRPCInfo(rpcInfo);
                            findValueHandler.setContacts(contacts);
                            findValueHandler.run();
                            break;
                    }
                } else {
                    RequestHandler handler;
                    synchronized (rpcIDMap) {
                        handler = rpcIDMap.get(rpc.getRPCID());
                    }
                    if (handler != null) {
                        handler.addResult(rpcInfo);
                    } else {
                        linea = ("WARN : " + Thread.currentThread().getThreadGroup().getName() + " : Recibida respuesta  para un controlador que ya no existe. ID del paquete: " + rpc.getRPCID());
                        fich.writelog(linea);
                    }
                }
            } catch (UnknownHostException e) {
                linea = ("WARN. UNKNOWN EXCEPTION : " + Thread.currentThread().getThreadGroup().getName() + " : Recibido paquete de una direcci�n IP no valida " + ip + ", . Descartar paquete");
                fich.writelog(linea);
            }
        }
    }

    public void login(NetLocation anotherNode) {
        LoginHandler handler = new LoginHandler();
        BigInteger rpcID = generateRPCID();
        handler.setRpcID(rpcID);
        handler.setBootstrapNode(anotherNode);
        handler.setSearchedNode(getMyID());
        synchronized (rpcIDMap) {
            rpcIDMap.put(rpcID, handler);
        }
        long maxWait = Long.parseLong(System.getProperty("jkad.findnode.maxwait"));
        handler.run();
        while (System.currentTimeMillis() - handler.getLastAccess() < maxWait) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                linea = ("WARN: " + e);
                fich.writelog(linea);
            }
        }
        synchronized (rpcIDMap) {
            rpcIDMap.remove(rpcID);
        }
    }

    public void store(String key, String data) {
        this.store(key.getBytes(), data.getBytes());
    }

    public void store(byte[] key, byte[] data) {
        BigInteger bKey = SHA1Digester.digest(key);
        BigInteger bData = new BigInteger(data);
        FindNodeHandler handler = new FindNodeHandler();
        BigInteger rpcID = generateRPCID();
        handler.setRpcID(rpcID);
        handler.setContacts(contacts);
        handler.setSearchedNode(bKey);
        synchronized (rpcIDMap) {
            rpcIDMap.put(rpcID, handler);
        }
        long maxWait = Long.parseLong(System.getProperty("jkad.findnode.maxwait"));
        handler.run();
        while (System.currentTimeMillis() - handler.getLastAccess() < maxWait) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                linea = ("WARN: " + e);
                fich.writelog(linea);
            }
        }
        synchronized (rpcIDMap) {
            rpcIDMap.remove(rpcID);
        }
        storeValueOnNodes(handler.getResults(), bKey, bData);
    }

    public String findValue(String key) {
        byte[] result = this.findValue(key.getBytes());
        return result != null ? new String(result) : null;
    }

    public byte[] findValue(byte[] data) {
        FindValueHandler handler = new FindValueHandler();
        BigInteger rpcID = generateRPCID();
        handler.setRpcID(rpcID);
        handler.setStorage(DataManagerFacade.getDataManager());
        handler.setContacts(contacts);
        BigInteger dig = SHA1Digester.digest(data);
        handler.setValueKey(dig);
        synchronized (rpcIDMap) {
            rpcIDMap.put(rpcID, handler);
        }
        long maxWait = Long.parseLong(System.getProperty("jkad.findvalue.maxwait"));
        handler.run();
        while (handler.getStatus() != Status.ENDED && System.currentTimeMillis() - handler.getLastAccess() < (maxWait)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                linea = ("WARN : " + e);
                fich.writelog(linea);
            }
        }
        synchronized (rpcIDMap) {
            rpcIDMap.remove(rpcID);
        }
        if ((handler.getResult() != null) && (handler.getClosestNode() != null)) {
            storeValueOnNodes(handler.getClosestNode(), dig, handler.getResult());
        }
        return handler.getResult() != null ? handler.getResult().toByteArray() : null;
    }

    private void storeValueOnNodes(Collection<KadNode> nodes, BigInteger key, BigInteger value) {
        StoreHandler storeHandler = new StoreHandler();
        for (KadNode node : nodes) {
            storeHandler.clear();
            storeHandler.setKey(key);
            storeHandler.setNode(node);
            storeHandler.setRpcID(generateRPCID());
            storeHandler.setValue(value);
            storeHandler.run();
        }
    }

    public ContactsKAD getcontacts() {
        return contacts;
    }

    public List<KadNode> getKnowContacts() {
        return contacts.getKnowContacts();
    }

    protected void finalize() {
    }
}
