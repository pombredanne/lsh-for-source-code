package jkad.controller.handlers.response;

import java.math.BigInteger;
import java.security.MessageDigest;
import jkad.facades.storage.DataManagerFacade;
import jkad.protocol.rpc.request.StoreRPC;
import jkad.structures.kademlia.RPCInfo;
import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;

public class StoreResponseHandlerTest extends TestCase {

    MessageDigest digester;

    public void setUp() throws Exception {
        super.setUp();
        digester = MessageDigest.getInstance("SHA-1");
        BasicConfigurator.configure();
    }

    public void testRun() {
        String ip = "10.0.0.1";
        Integer port = 5000;
        StoreRPC storeRPC = new StoreRPC();
        try {
            storeRPC.setKey(new BigInteger(digester.digest("0".getBytes())));
            storeRPC.setValue(new BigInteger("polaco!".getBytes()));
            RPCInfo<StoreRPC> rpcInfo = new RPCInfo(storeRPC, ip, port);
            StoreResponseHandler handler = new StoreResponseHandler();
            handler.setRPCInfo(rpcInfo);
            handler.run();
            DataManagerFacade<String> storage = DataManagerFacade.getDataManager();
            assertEquals("polaco!", storage.get(new BigInteger(digester.digest("0".getBytes()))));
        } catch (Exception e) {
            fail();
        }
    }
}
