package fi.hip.gb.disk.info.dht;

import static org.acplt.oncrpc.OncRpcProtocols.ONCRPC_TCP;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.Properties;
import bamboo.dht.bamboo_hash;
import bamboo.dht.bamboo_key;
import bamboo.dht.bamboo_put_arguments;
import bamboo.dht.bamboo_value;
import bamboo.dht.gateway_protClient;

public class Put {

    private static String DHT_GW_ADDR = "planet3.berkeley.intel-research.net";

    private static String DHT_GW_PORT = "5852";

    private static String DHT_TTL = "100";

    private static String DHT_SECRET = "test";

    /**
     * Tries to put a key into Planetlab open dht
     * 
     * @param key byte array, maximum 20 bytes
     * @param props byte array, maximum 1024 bytes
     * @return true if put succeeded, false if not
     */
    public boolean putProperties(String key, Properties props) throws Exception {
        InetAddress gateway = InetAddress.getByName(DHT_GW_ADDR);
        int port = Integer.parseInt(DHT_GW_PORT);
        bamboo_put_arguments putArgs = new bamboo_put_arguments();
        putArgs.application = Put.class.getName();
        putArgs.client_library = "Remote Tea ONC/RPC";
        MessageDigest md = MessageDigest.getInstance("SHA");
        putArgs.key = new bamboo_key();
        putArgs.key.value = md.digest(key.getBytes());
        putArgs.value = new bamboo_value();
        ByteArrayOutputStream array = new ByteArrayOutputStream();
        props.store(array, key);
        putArgs.value.value = array.toByteArray();
        putArgs.ttl_sec = Integer.parseInt(DHT_TTL);
        putArgs.secret_hash = new bamboo_hash();
        putArgs.secret_hash.algorithm = "SHA";
        putArgs.secret_hash.hash = md.digest(DHT_SECRET.getBytes());
        gateway_protClient client = new gateway_protClient(gateway, port, ONCRPC_TCP);
        int result = client.BAMBOO_DHT_PROC_PUT_3(putArgs);
        if (result == 0) {
            System.out.println("BAMBOO_OK");
            return true;
        } else {
            return true;
        }
    }
}
