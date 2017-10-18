package jkad.controller.handlers.request;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Properties;
import jkad.facades.storage.DataManagerFacade;
import jkad.protocol.rpc.response.FindNodeResponse;
import jkad.structures.buffers.RPCBuffer;
import jkad.structures.kademlia.KadNode;
import jkad.structures.kademlia.KnowContacts;
import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;

public class FindNodeHandlerTest extends TestCase {

    private MessageDigest digester;

    private RPCBuffer buffer;

    private String[][] values;

    public void setUp() throws Exception {
        super.setUp();
        BasicConfigurator.configure();
        Properties props = new Properties();
        props.load(new FileInputStream("jkad.properties"));
        System.setProperties(props);
        digester = MessageDigest.getInstance("SHA-1");
        buffer = RPCBuffer.getSentBuffer();
        values = new String[][] { { "Polaco", "Bruno Penteado" }, { "smile", ":)" }, { "teste", "teste ok!" }, { "terra", "fogo" } };
    }

    public void testRun() {
    }

    public void testAddResult() {
        fail("Not yet implemented");
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
