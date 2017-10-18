package fi.hip.gb.disk.info.dht;

import static org.acplt.oncrpc.OncRpcProtocols.ONCRPC_TCP;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.Properties;
import bamboo.dht.bamboo_get_args;
import bamboo.dht.bamboo_get_res;
import bamboo.dht.bamboo_key;
import bamboo.dht.bamboo_placemark;
import bamboo.dht.gateway_protClient;

public class Get {

    private static String DHT_GW_ADDR = "planet3.berkeley.intel-research.net";

    private static String DHT_GW_PORT = "5852";

    public Properties getProperties(String key) throws Exception {
        InetAddress gateway = InetAddress.getByName(DHT_GW_ADDR);
        int port = Integer.parseInt(DHT_GW_PORT);
        bamboo_get_args getArgs = new bamboo_get_args();
        getArgs.application = Get.class.getName();
        getArgs.client_library = "Remote Tea ONC/RPC";
        MessageDigest md = MessageDigest.getInstance("SHA");
        getArgs.key = new bamboo_key();
        getArgs.key.value = md.digest(key.getBytes());
        getArgs.maxvals = Integer.MAX_VALUE;
        getArgs.placemark = new bamboo_placemark();
        getArgs.placemark.value = new byte[0];
        gateway_protClient client = null;
        client = new gateway_protClient(gateway, port, ONCRPC_TCP);
        bamboo_get_res res = null;
        res = client.BAMBOO_DHT_PROC_GET_2(getArgs);
        if (res.values.length == 0) return null;
        System.out.println("** GET RESULT ** \n" + new String(res.values[0].value));
        Properties props = new Properties();
        props.load(new ByteArrayInputStream(res.values[0].value));
        return props;
    }
}
