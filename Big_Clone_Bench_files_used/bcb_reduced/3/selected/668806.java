package jkad.controller.handlers.response;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import jkad.facades.storage.DataManagerFacade;
import jkad.protocol.rpc.request.FindValueRPC;
import jkad.protocol.rpc.response.FindNodeResponse;
import jkad.protocol.rpc.response.FindValueResponse;
import jkad.structures.buffers.RPCBuffer;
import jkad.structures.kademlia.KadNode;
import jkad.structures.kademlia.KnowContacts;
import jkad.structures.kademlia.RPCInfo;
import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;

public class FindValueResponseHandlerTest extends TestCase {

    private String[][] values;

    private MessageDigest digester;

    private RPCBuffer buffer;

    public void setUp() throws Exception {
        super.setUp();
        BasicConfigurator.configure();
        Properties props = new Properties();
        props.load(new FileInputStream("jkad.properties"));
        System.setProperties(props);
        values = new String[][] { { "Polaco", "Bruno Penteado" }, { "smile", ":)" }, { "teste", "teste ok!" }, { "terra", "fogo" } };
        digester = MessageDigest.getInstance("SHA-1");
        buffer = RPCBuffer.getSentBuffer();
    }

    public void testRun() {
        String ip = "10.0.0.1";
        Integer port = 5000;
        fillStorage();
        KnowContacts contacts = getFilledContacts();
        try {
            for (int i = 0; i < values.length; i++) {
                BigInteger randomHash = new BigInteger(digester.digest(new byte[] { (byte) System.currentTimeMillis() }));
                FindValueRPC findValueRPC = new FindValueRPC();
                byte[] hashed = digester.digest(values[i][0].getBytes());
                findValueRPC.setKey(new BigInteger(hashed));
                findValueRPC.setSenderNodeID(new BigInteger(digester.digest(new byte[] { 1 })));
                findValueRPC.setRPCID(randomHash);
                RPCInfo<FindValueRPC> rpcInfo = new RPCInfo<FindValueRPC>(findValueRPC, ip, port);
                FindValueResponseHandler handler = new FindValueResponseHandler();
                handler.setRPCInfo(rpcInfo);
                handler.run();
                RPCInfo removed = buffer.remove();
                assertEquals(ip, removed.getIP());
                assertEquals((int) port, removed.getPort());
                assertEquals(FindValueResponse.class, removed.getRPC().getClass());
                FindValueResponse response = (FindValueResponse) removed.getRPC();
                assertEquals(randomHash, response.getRPCID());
                assertEquals(new BigInteger(values[i][1].getBytes()), response.getValue());
                assertEquals(values[i][1], new String(response.getValue().toByteArray()));
            }
            int findKey = 100;
            BigInteger randomHash = new BigInteger(digester.digest(new byte[] { (byte) System.currentTimeMillis() }));
            FindValueRPC findValueRPC = new FindValueRPC();
            findValueRPC.setKey(new BigInteger(normalizeArray(BigInteger.valueOf(findKey).toByteArray(), 20)));
            findValueRPC.setSenderNodeID(new BigInteger(digester.digest(new byte[] { 1 })));
            findValueRPC.setRPCID(randomHash);
            RPCInfo<FindValueRPC> rpcInfo = new RPCInfo<FindValueRPC>(findValueRPC, ip, port);
            FindValueResponseHandler handler = new FindValueResponseHandler();
            handler.setRPCInfo(rpcInfo);
            handler.setContacts(contacts);
            handler.run();
            assertEquals(20, buffer.size());
            List<FindNodeResponse> responses = new ArrayList<FindNodeResponse>();
            while (!buffer.isEmpty()) {
                RPCInfo<FindNodeResponse> removed = buffer.remove();
                assertEquals(ip, removed.getIP());
                assertEquals((int) port, removed.getPort());
                assertEquals(FindNodeResponse.class, removed.getRPC().getClass());
                FindNodeResponse response = (FindNodeResponse) removed.getRPC();
                responses.add(response);
            }
            Collections.sort(responses, new FindNodeResponseComparator());
            Iterator<FindNodeResponse> it = responses.iterator();
            for (int i = findKey - 90; i <= findKey + 100; i += 10) assertEquals(BigInteger.valueOf(i), it.next().getFoundNodeID());
        } catch (Exception e) {
            fail();
        }
    }

    private void fillStorage() {
        DataManagerFacade<String> storage = DataManagerFacade.getDataManager();
        for (int i = 0; i < values.length; i++) {
            byte[] hashed = digester.digest(values[i][0].getBytes());
            storage.put(new BigInteger(hashed), values[i][1]);
        }
    }

    private KnowContacts getFilledContacts() {
        KnowContacts knowContacts = new KnowContacts();
        for (int i = 0; i < 500; i++) {
            String ip = "192.168." + ((i / 256) % 256) + "." + (i % (256));
            KadNode node = new KadNode();
            try {
                node.setIpAddress(InetAddress.getByName(ip));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            node.setPort(i);
            node.setNodeID(new BigInteger(normalizeArray(BigInteger.valueOf(i * 10).toByteArray(), 20)));
            knowContacts.addContact(node);
        }
        return knowContacts;
    }

    private byte[] normalizeArray(byte[] array, int size) {
        byte[] normalized = new byte[size];
        System.arraycopy(array, 0, normalized, size - array.length, array.length);
        return normalized;
    }
}

class FindNodeResponseComparator implements Comparator<FindNodeResponse> {

    public int compare(FindNodeResponse o1, FindNodeResponse o2) {
        return o1.getFoundNodeID().compareTo(o2.getFoundNodeID());
    }
}
