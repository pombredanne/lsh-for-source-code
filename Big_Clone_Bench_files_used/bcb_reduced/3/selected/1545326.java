package fi.hip.gb.disk.info.dht;

import static org.acplt.oncrpc.OncRpcProtocols.ONCRPC_TCP;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.OncRpcTimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import bamboo.dht.bamboo_get_args;
import bamboo.dht.bamboo_get_res;
import bamboo.dht.bamboo_hash;
import bamboo.dht.bamboo_key;
import bamboo.dht.bamboo_placemark;
import bamboo.dht.bamboo_put_arguments;
import bamboo.dht.bamboo_rm_arguments;
import bamboo.dht.bamboo_value;
import bamboo.dht.gateway_protClient;

public class DhtOperation {

    private static int DHT_GW_PORT = 5852;

    private static int DHT_RETRIES = 3;

    private String DHT_SECRET = "dummy_secret_for_remove";

    private static String DHT_TTL = "360";

    private String DHT_GW_FILENAME = "dht_gateway.conf";

    private gateway_protClient client = null;

    private Vector<String> gateways = new Vector<String>();

    private static Log log = LogFactory.getLog(DhtOperation.class);

    private int originalGatewayPos = 0;

    public DhtOperation() {
        try {
            if (getClass().getClassLoader().getResource(DHT_GW_FILENAME) != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(DHT_GW_FILENAME)));
                String str;
                while ((str = in.readLine()) != null) {
                    gateways.add(str);
                }
                in.close();
            }
            if (this.gateways.size() == 0) {
                log.error("Could not read DHT infosystem gateways from file: " + DHT_GW_FILENAME);
            } else {
                Random chooser = new Random();
                this.originalGatewayPos = chooser.nextInt(gateways.size());
                InetAddress gateway = InetAddress.getByName(this.gateways.elementAt(this.originalGatewayPos));
                log.info("Using Gateway: " + gateway);
                client = new gateway_protClient(gateway, DHT_GW_PORT, ONCRPC_TCP);
            }
        } catch (UnknownHostException e) {
            log.error(e);
        } catch (OncRpcException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
    }

    public byte[] getPropertiesArray(String key) throws Exception {
        bamboo_get_args getArgs = new bamboo_get_args();
        getArgs.application = "Get";
        getArgs.client_library = "Remote Tea ONC/RPC";
        MessageDigest md = MessageDigest.getInstance("SHA");
        getArgs.key = new bamboo_key();
        getArgs.key.value = md.digest(key.getBytes());
        getArgs.maxvals = Integer.MAX_VALUE;
        getArgs.placemark = new bamboo_placemark();
        getArgs.placemark.value = new byte[0];
        bamboo_get_res res = null;
        int tries = 0;
        while (res == null && tries < DHT_RETRIES) {
            try {
                res = client.BAMBOO_DHT_PROC_GET_2(getArgs);
            } catch (OncRpcTimeoutException e) {
                tries++;
                InetAddress inetAddr = InetAddress.getByName(this.gateways.elementAt((originalGatewayPos + tries) % (gateways.size() - 1)));
                client = new gateway_protClient(inetAddr, DHT_GW_PORT, ONCRPC_TCP);
            }
        }
        if (res.values.length == 0) return null;
        log.info("** DhtInfo system replies for GET: " + key + " \n" + new String(res.values[0].value));
        return res.values[0].value;
    }

    public Properties getProperties(String key) throws Exception {
        byte[] array = getPropertiesArray(key);
        if (array != null) {
            Properties props = new Properties();
            props.load(new ByteArrayInputStream(array));
            return props;
        }
        return null;
    }

    /**
     * Tries to put a key into Planetlab open dht
     * 
     * @param key byte array, maximum 20 bytes
     * @param props byte array, maximum 1024 bytes
     * @return true if put succeeded, false if not
     */
    public boolean putProperties(String key, Properties props) throws Exception {
        bamboo_put_arguments putArgs = new bamboo_put_arguments();
        putArgs.application = "Put";
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
        log.error("HASH " + md.digest(array.toByteArray()));
        int res = -1;
        int tries = 0;
        while (res == -1 && tries < DHT_RETRIES) {
            try {
                res = client.BAMBOO_DHT_PROC_PUT_3(putArgs);
            } catch (OncRpcTimeoutException e) {
                tries++;
                InetAddress inetAddr = InetAddress.getByName(this.gateways.elementAt((originalGatewayPos + tries) % (gateways.size() - 1)));
                client = new gateway_protClient(inetAddr, DHT_GW_PORT, ONCRPC_TCP);
            }
        }
        if (res == 0) {
            log.info("DhtInfo system replies for PUT: " + key + " : BAMBOO_OK");
            return true;
        } else {
            return false;
        }
    }

    public boolean deleteProperties(String key) throws Exception {
        bamboo_rm_arguments removeArgs = new bamboo_rm_arguments();
        removeArgs.application = "Put";
        removeArgs.client_library = "Remote Tea ONC/RPC";
        MessageDigest md = MessageDigest.getInstance("SHA");
        removeArgs.key = new bamboo_key();
        removeArgs.key.value = md.digest(key.getBytes());
        removeArgs.value_hash = new bamboo_hash();
        removeArgs.value_hash.algorithm = "SHA";
        removeArgs.value_hash.hash = md.digest(getPropertiesArray(key));
        log.error("HASH " + removeArgs.value_hash.hash);
        removeArgs.ttl_sec = Integer.parseInt(DHT_TTL);
        removeArgs.secret_hash_alg = "SHA";
        removeArgs.secret = DHT_SECRET.getBytes();
        int res = -1;
        int tries = 0;
        while (res == -1 && tries < DHT_RETRIES) {
            try {
                res = client.BAMBOO_DHT_PROC_RM_3(removeArgs);
            } catch (OncRpcTimeoutException e) {
                tries++;
                InetAddress inetAddr = InetAddress.getByName(this.gateways.elementAt((originalGatewayPos + tries) % (gateways.size() - 1)));
                client = new gateway_protClient(inetAddr, DHT_GW_PORT, ONCRPC_TCP);
            }
        }
        if (res == 0) {
            log.info("DhtInfo system replies for DELETE: " + key + "  : BAMBOO_OK");
            return true;
        } else {
            return false;
        }
    }
}
