package org.dbe.p2p.dht;

import bamboo.dht.bamboo_get_args;
import bamboo.dht.bamboo_get_result;
import bamboo.dht.bamboo_hash;
import bamboo.dht.bamboo_key;
import bamboo.dht.bamboo_placemark;
import bamboo.dht.bamboo_put_arguments;
import bamboo.dht.bamboo_rm_arguments;
import bamboo.dht.bamboo_stat;
import bamboo.dht.bamboo_value;
import bamboo.dht.gateway_protClient;
import bamboo.lss.ASyncCore;
import bamboo.lss.DustDevil;
import bamboo.lss.PriorityQueue;
import bamboo.util.StringUtil;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.CharArrayReader;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.OncRpcProtocols;
import org.apache.log4j.Logger;
import org.dbe.p2p.dht.bootstrap.BootstrapService;

/**
 * This class is used for accessing the DHT overlay from DBE. Should be used by
 * all applications using the DHT. There should be only ONE instance of this
 * class. To get it, use getInstance() function.
 *
 * It is thread safe, which means that many threads can use its methods
 * concurrently.
 */
public class DHT {

    static short NS_HASH_STORAGE = 1;

    private static int BAMBOO_BLOCK_SIZE = 1024;

    protected static final int BRANCHING = (BAMBOO_BLOCK_SIZE - 2 - 2) / 20;

    protected static final int MAX_PARALLEL = 50;

    static Logger logger = Logger.getLogger(DHT.class);

    static int DHT_PORT = 5850;

    static int RPC_PORT = 5852;

    static boolean WEBINTERFACE = true;

    static int NUM_REPLICAS = 2;

    protected BootstrapService bootstrap;

    public Properties config;

    private boolean started = false;

    MessageDigest md;

    Random random;

    private static DHT instance = null;

    private LinkedBlockingQueue tasks_queue;

    private Hashtable rpc_conns = new Hashtable();

    public static synchronized DHT getInstance() throws Exception {
        return instance;
    }

    public DHT(Properties conf) throws Exception {
        if (instance != null) {
            throw new Exception("Instance of the DHT class already exists!");
        }
        instance = this;
        md = MessageDigest.getInstance("SHA");
        random = new Random();
        config = conf;
        bootstrap = new BootstrapService();
        if (config.getProperty("RPC_PORT") != null) RPC_PORT = Integer.parseInt(config.getProperty("RPC_PORT"));
        if (config.getProperty("DHT_PORT") != null) DHT_PORT = Integer.parseInt(config.getProperty("DHT_PORT"));
        if (config.getProperty("WEBINTERFACE") != null) WEBINTERFACE = (Integer.parseInt(config.getProperty("WEBINTERFACE")) == 1);
        tasks_queue = new LinkedBlockingQueue();
        for (int i = 0; i < MAX_PARALLEL; i++) {
            (new ConsumerThread(tasks_queue)).start();
        }
    }

    private synchronized gateway_protClient getRpcConnection() {
        gateway_protClient rpc = (gateway_protClient) rpc_conns.get(Thread.currentThread());
        int gateways = bootstrap.getNumberOfGateways();
        for (int i = 0; i < gateways; i++) {
            InetAddress relay = null;
            try {
                relay = bootstrap.getRelay();
                logger.info((new StringBuilder()).append("relay: ").append(relay).toString());
                rpc = new gateway_protClient(relay, RPC_PORT, 6);
                if (rpc != null) {
                    logger.info("Associate the RPC gateway with a separate connection.");
                    rpc_conns.put(Thread.currentThread(), rpc);
                    return rpc;
                } else {
                    logger.warn((new StringBuilder()).append("The RPC connection ").append(relay.toString()).append(" is null!").toString());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                logger.info(ex);
                bootstrap.changeRelay(relay);
            } catch (OncRpcException ex) {
                ex.printStackTrace();
                logger.info(ex);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.info(ex);
            }
        }
        return null;
    }

    class ConsumerThread extends Thread {

        LinkedBlockingQueue queue;

        public ConsumerThread(LinkedBlockingQueue q) {
            queue = q;
        }

        public void run() {
            while (true) {
                Runnable r = null;
                while (r == null) {
                    try {
                        r = (Runnable) queue.take();
                    } catch (InterruptedException ex) {
                    }
                }
                r.run();
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
        }
    }

    public void stop() {
        bootstrap.stop();
        for (int i = 0; i < MAX_PARALLEL; i++) {
            tasks_queue.add(new Runnable() {

                public void run() {
                    Thread.currentThread().interrupt();
                }
            });
        }
        Enumeration elements = rpc_conns.elements();
        while (elements.hasMoreElements()) {
            gateway_protClient rpc = (gateway_protClient) elements.nextElement();
            try {
                rpc.close();
            } catch (Exception ex) {
            }
        }
    }

    class Triple {

        public byte[] first;

        public byte[] second;

        public Integer third;

        public Triple(byte[] f, byte[] s, Integer t) {
            first = f;
            second = s;
            third = t;
        }
    }

    class Pair {

        public Integer first;

        public byte[] second;

        public Pair(Integer t, byte[] f) {
            first = t;
            second = f;
        }
    }

    private int putNS(short appId, byte[] keyHash, byte[] value, int ttl, byte[] secretHash) {
        if (value.length > BAMBOO_BLOCK_SIZE - 2) return bamboo_stat.BAMBOO_CAP;
        bamboo_put_arguments putArgs = new bamboo_put_arguments();
        putArgs.application = DHT.class.getName();
        putArgs.client_library = "Remote Tea ONC/RPC";
        putArgs.key = new bamboo_key();
        putArgs.key.value = keyHash;
        putArgs.value = new bamboo_value();
        byte[] v = new byte[value.length + 2];
        ByteBuffer bb = ByteBuffer.wrap(v);
        bb.putShort(appId);
        bb.put(value);
        putArgs.value.value = bb.array();
        putArgs.ttl_sec = ttl;
        putArgs.secret_hash = new bamboo_hash();
        if (secretHash != null) {
            putArgs.secret_hash.algorithm = "SHA";
            putArgs.secret_hash.hash = secretHash;
        } else {
            putArgs.secret_hash.algorithm = "";
            putArgs.secret_hash.hash = new byte[0];
        }
        gateway_protClient client = getRpcConnection();
        if (client == null) {
            return bamboo_stat.BAMBOO_AGAIN;
        }
        try {
            return client.BAMBOO_DHT_PROC_PUT_3(putArgs);
        } catch (IOException ex) {
            bootstrap.changeRelay(client.getClient().getHost());
            try {
                client.close();
            } catch (Exception ex2) {
            }
            return bamboo_stat.BAMBOO_AGAIN;
        } catch (OncRpcException ex) {
            return bamboo_stat.BAMBOO_AGAIN;
        }
    }

    private int getNS(short appId, byte[] keyHash, int maxvals, ArrayList valuesList) {
        bamboo_get_args getArgs = new bamboo_get_args();
        getArgs.application = DHT.class.getName();
        getArgs.client_library = "Remote Tea ONC/RPC";
        getArgs.key = new bamboo_key();
        getArgs.key.value = keyHash;
        getArgs.maxvals = maxvals;
        getArgs.placemark = new bamboo_placemark();
        getArgs.placemark.value = new byte[0];
        gateway_protClient client = null;
        try {
            client = getRpcConnection();
            if (client == null) {
                return bamboo_stat.BAMBOO_AGAIN;
            }
            while (true) {
                bamboo_get_result res = client.BAMBOO_DHT_PROC_GET_3(getArgs);
                for (int i = 0; i < res.values.length; ++i) {
                    ByteBuffer bb = ByteBuffer.wrap(res.values[i].value.value);
                    short id = bb.getShort();
                    StringBuffer sb = new StringBuffer(2000);
                    StringUtil.bytes_to_sbuf(keyHash, 0, keyHash.length, false, sb);
                    logger.info("found " + res.values.length + " values for key: " + sb.toString());
                    logger.info("value[" + i + "] appId=" + id + " size=" + (bb.limit() - 2));
                    if (id == appId) {
                        byte[] value = new byte[bb.limit() - 2];
                        bb.get(value);
                        valuesList.add(new Triple(value, res.values[i].secret_hash.hash, new Integer(res.values[i].ttl_sec_rem)));
                    }
                }
                if (res.placemark.value.length == 0) {
                    break;
                }
                getArgs.placemark = res.placemark;
            }
        } catch (IOException ex) {
            logger.info(ex);
            bootstrap.changeRelay(client.getClient().getHost());
            try {
                client.close();
            } catch (Exception ex2) {
            }
            return bamboo_stat.BAMBOO_AGAIN;
        } catch (OncRpcException ex) {
            logger.info(ex);
            return bamboo_stat.BAMBOO_AGAIN;
        }
        logger.info("found " + valuesList.size() + " values");
        return bamboo_stat.BAMBOO_OK;
    }

    private int removeNS(short appId, byte[] key, byte[] value, int ttl, byte[] secret) {
        if (value.length > BAMBOO_BLOCK_SIZE - 2) {
            return bamboo_stat.BAMBOO_CAP;
        }
        bamboo_rm_arguments rmArgs = new bamboo_rm_arguments();
        rmArgs.application = DHT.class.getName();
        rmArgs.client_library = "Remote Tea ONC/RPC";
        rmArgs.key = new bamboo_key();
        rmArgs.key.value = key;
        byte[] v = new byte[value.length + 2];
        ByteBuffer bb = ByteBuffer.wrap(v);
        bb.putShort(appId);
        bb.put(value);
        rmArgs.value_hash = new bamboo_hash();
        rmArgs.value_hash.algorithm = "SHA";
        rmArgs.value_hash.hash = md.digest(bb.array());
        rmArgs.ttl_sec = ttl;
        if (secret != null) {
            rmArgs.secret_hash_alg = "SHA";
            rmArgs.secret = secret;
        } else {
            rmArgs.secret_hash_alg = "";
            rmArgs.secret = new byte[0];
        }
        gateway_protClient client = getRpcConnection();
        if (client == null) {
            return bamboo_stat.BAMBOO_AGAIN;
        }
        try {
            return client.BAMBOO_DHT_PROC_RM_3(rmArgs);
        } catch (IOException ex) {
            bootstrap.changeRelay(client.getClient().getHost());
            try {
                client.close();
            } catch (Exception ex2) {
            }
            return bamboo_stat.BAMBOO_AGAIN;
        } catch (OncRpcException ex) {
            return bamboo_stat.BAMBOO_AGAIN;
        }
    }

    private void fetchBlock(final short appId, final byte[] key, final int pos, final int maxval, final LinkedBlockingQueue blocks_queue, final byte[] secretToMatch, final Status stat) {
        tasks_queue.add(new Runnable() {

            public void run() {
                if (stat.isError()) {
                    return;
                }
                ArrayList values = new ArrayList();
                int res;
                do {
                    res = getNS(appId, key, maxval, values);
                    if (res == bamboo_stat.BAMBOO_AGAIN) {
                        try {
                            wait(1000);
                        } catch (Exception ex) {
                        }
                    }
                } while (res == bamboo_stat.BAMBOO_AGAIN);
                if (res != bamboo_stat.BAMBOO_OK) {
                    logger.info("requestBlock() failed");
                    stat.error();
                    return;
                }
                boolean found = false;
                Iterator it = values.iterator();
                while (it.hasNext()) {
                    Triple v = (Triple) it.next();
                    if (secretToMatch == null || Arrays.equals(v.second, secretToMatch)) {
                        try {
                            blocks_queue.put(new Pair(new Integer(pos), v.first));
                            found = true;
                        } catch (InterruptedException ex) {
                            logger.info("thread interrupted in requestBlock()!");
                            return;
                        }
                        break;
                    }
                }
                if (!found) {
                    stat.error();
                    return;
                }
            }
        });
    }

    private int largeGet(short appId, byte[] key, int maxvals, ArrayList values) {
        StringBuffer sb = new StringBuffer(100);
        StringUtil.bytes_to_sbuf(getDigest(key), 0, getDigest(key).length, false, sb);
        logger.info("root key = 0x" + sb.toString());
        ArrayList vs = new ArrayList();
        int res;
        do {
            res = getNS(appId, getDigest(key), maxvals, vs);
        } while (res == bamboo_stat.BAMBOO_AGAIN);
        if (res != bamboo_stat.BAMBOO_OK) {
            return -1;
        }
        logger.info("found " + vs.size() + " root blocks");
        Iterator it = vs.iterator();
        while (it.hasNext()) {
            Triple v = (Triple) it.next();
            byte[] singleval = largeGetSingleValue(appId, v.first, v.second);
            if (singleval == null) {
                return -1;
            }
            values.add(new Triple(singleval, v.second, v.third));
        }
        return 0;
    }

    class Status {

        boolean error = false;

        public synchronized void error() {
            error = true;
        }

        public synchronized boolean isError() {
            return error;
        }
    }

    private byte[] largeGetSingleValue(short appId, byte[] rootBlock, byte[] secret) {
        Status stat = new Status();
        LinkedBlockingQueue blocks_queue = new LinkedBlockingQueue();
        logger.info("root block size=" + rootBlock.length);
        try {
            blocks_queue.put(new Pair(new Integer(0), rootBlock));
        } catch (Exception e) {
            logger.error("largeGetSingle() thread interrupted");
            return null;
        }
        long requested = 1;
        bamboo.lss.PriorityQueue resultQ = new PriorityQueue(10);
        int total = 0;
        while (requested > 0) {
            Pair pair = null;
            try {
                pair = (Pair) blocks_queue.take();
                if (stat.isError()) {
                    logger.error("couldn't fetch a block!");
                    return null;
                }
            } catch (Exception e) {
                logger.error("largeGetSingle() thread interrupted");
                return null;
            }
            requested--;
            int position = pair.first.intValue();
            ByteBuffer bb = ByteBuffer.wrap(pair.second);
            short level = bb.getShort();
            if (level > 0) {
                logger.info("level=" + level);
                int p = BRANCHING * position;
                while (bb.position() < bb.limit()) {
                    byte[] k = new byte[20];
                    bb.get(k);
                    StringBuffer sb = new StringBuffer(100);
                    StringUtil.bytes_to_sbuf(k, 0, k.length, false, sb);
                    logger.info("key found is 0x" + sb.toString());
                    fetchBlock(appId, k, p++, 1, blocks_queue, secret, stat);
                    requested++;
                }
            } else {
                logger.info("found a data block");
                byte[] v = new byte[bb.limit() - 2];
                bb.get(v);
                total += v.length;
                resultQ.add(v, position);
            }
        }
        byte[] result = new byte[total];
        ByteBuffer bb = ByteBuffer.wrap(result);
        while (!resultQ.isEmpty()) {
            bb.put((byte[]) resultQ.removeFirst());
        }
        if (stat.isError()) {
            return null;
        }
        return result;
    }

    private synchronized byte[] getDigest(byte[] val) {
        return md.digest(val);
    }

    class Semaphore {

        private int outstanding = 0;

        public synchronized void inc() {
            outstanding++;
            notifyAll();
        }

        public synchronized void dec() {
            outstanding--;
            notifyAll();
        }

        public synchronized void waitUntilFinished() {
            while (outstanding > 0) {
                try {
                    wait();
                } catch (Exception ex) {
                }
                ;
            }
        }
    }

    private int largePut(short appId, byte[] key, byte[] value, int ttl, byte[] secretHash) {
        Semaphore sem = new Semaphore();
        Status stat = new Status();
        LinkedList blocks = new LinkedList();
        ByteBuffer bb = ByteBuffer.wrap(value);
        while (bb.hasRemaining()) {
            int count = Math.min(bb.remaining(), BAMBOO_BLOCK_SIZE - 4);
            byte[] datablock = new byte[count + 2];
            ByteBuffer datablock_bb = ByteBuffer.wrap(datablock);
            datablock_bb.putShort((short) 0);
            bb.get(datablock, 2, count);
            blocks.add(datablock);
        }
        while (blocks.size() > 1) {
            Iterator it = blocks.iterator();
            short level = ByteBuffer.wrap((byte[]) blocks.getFirst()).getShort();
            ByteBuffer indexb = ByteBuffer.wrap(new byte[BAMBOO_BLOCK_SIZE - 2]);
            indexb.putShort((short) (level + 1));
            while (it.hasNext() && (indexb.remaining() > 0)) {
                int l = ByteBuffer.wrap((byte[]) blocks.getFirst()).getShort();
                if (l != level) {
                    break;
                }
                byte[] b = (byte[]) blocks.removeFirst();
                byte[] dig = getDigest(b);
                if (stat.isError()) {
                    return -1;
                }
                sendBlock(appId, dig, b, ttl, secretHash, sem, stat);
                indexb.put(dig);
            }
            byte[] index;
            if (indexb.position() == indexb.array().length) {
                index = indexb.array();
            } else {
                index = new byte[indexb.position()];
                indexb.flip();
                indexb.get(index);
            }
            blocks.add(index);
        }
        logger.info("root block size=" + ((byte[]) blocks.get(0)).length);
        sendBlock(appId, getDigest(key), (byte[]) blocks.removeFirst(), ttl, secretHash, sem, stat);
        sem.waitUntilFinished();
        if (stat.isError()) {
            return -1;
        }
        return 0;
    }

    private void sendBlock(final short appId, final byte[] blockkey, final byte[] value, final int ttl, final byte[] secretHash, final Semaphore sem, final Status stat) {
        sem.inc();
        tasks_queue.add(new Runnable() {

            public void run() {
                if (stat.isError()) {
                    sem.dec();
                    return;
                }
                int res;
                do {
                    StringBuffer sb = new StringBuffer(100);
                    StringUtil.bytes_to_sbuf(blockkey, 0, blockkey.length, false, sb);
                    logger.info("putting block size=" + value.length + " key=0x" + sb.toString());
                    res = putNS(appId, blockkey, value, ttl, secretHash);
                    if (res != bamboo_stat.BAMBOO_OK) {
                        sb = new StringBuffer(100);
                        StringUtil.bytes_to_sbuf(blockkey, 0, blockkey.length, false, sb);
                        logger.debug("got response " + res + " for 0x" + sb);
                    }
                    if (res == bamboo_stat.BAMBOO_AGAIN) {
                        try {
                            wait(1000);
                        } catch (Exception ex) {
                        }
                    }
                } while (res == bamboo_stat.BAMBOO_AGAIN);
                if (res != bamboo_stat.BAMBOO_OK) {
                    logger.error("sendBlock() failed");
                    stat.error();
                }
                sem.dec();
            }
        });
    }

    public int get(short appId, byte[] key, int maxvals, ArrayList values) {
        ArrayList vals = new ArrayList();
        if (largeGet(appId, key, maxvals, vals) < 0) {
            return -1;
        }
        Iterator it = vals.iterator();
        while (it.hasNext()) {
            Triple v = (Triple) it.next();
            values.add(v.first);
        }
        return 0;
    }

    public int put(short appId, byte[] key, byte[] value, int ttl, byte[] secret) {
        if (secret == null) {
            if (largePut(appId, key, value, ttl, null) < 0) return -1;
        } else {
            md.update(secret);
            byte[] token = new byte[20];
            random.nextBytes(token);
            md.update(token);
            byte[] secretKey = md.digest();
            byte[] hashSecretKey = md.digest(secretKey);
            if (putNS(NS_HASH_STORAGE, hashSecretKey, token, ttl, hashSecretKey) != bamboo_stat.BAMBOO_OK) {
                return -1;
            }
            if (largePut(appId, key, value, ttl, hashSecretKey) < 0) {
                return -1;
            }
        }
        return 0;
    }

    public int remove(short appId, byte[] key, byte[] secret) {
        ArrayList rootValues = new ArrayList();
        if (getNS(appId, getDigest(key), Integer.MAX_VALUE, rootValues) != bamboo_stat.BAMBOO_OK) return -1;
        Iterator it = rootValues.iterator();
        while (it.hasNext()) {
            Triple value = (Triple) it.next();
            int ttl = ((Integer) value.third).intValue();
            if (secret == null) {
                if (removeNS(appId, getDigest(key), value.first, ttl, null) < 0) return -1;
            } else {
                byte[] hashSecretKey = value.second;
                ArrayList tokens = new ArrayList();
                if (getNS(NS_HASH_STORAGE, hashSecretKey, Integer.MAX_VALUE, tokens) != bamboo_stat.BAMBOO_OK) {
                    return -1;
                }
                if (tokens.size() == 0) {
                    return -1;
                }
                byte[] token = ((Triple) tokens.get(0)).first;
                md.update(secret);
                md.update(token);
                byte[] secretKey = md.digest();
                if (!Arrays.equals(md.digest(secretKey), hashSecretKey)) {
                    logger.error("something is wrong... hash of a secret key is incorrect!");
                    return -1;
                }
                if (removeNS(appId, getDigest(key), value.first, ttl, secretKey) < 0) {
                    return -1;
                }
                if (removeNS(NS_HASH_STORAGE, hashSecretKey, token, ttl, secretKey) < 0) {
                    return -1;
                }
            }
        }
        return 0;
    }

    public int updateLease(short appId, byte[] key, int ttl) {
        ArrayList vals = new ArrayList();
        if (largeGet(appId, key, Integer.MAX_VALUE, vals) < 0) {
            return -1;
        }
        if (vals.size() == 0) {
            return -1;
        }
        Iterator it = vals.iterator();
        while (it.hasNext()) {
            Triple v = (Triple) it.next();
            if (v.second != null) {
                ArrayList tokens = new ArrayList();
                if (getNS(NS_HASH_STORAGE, v.second, Integer.MAX_VALUE, tokens) != bamboo_stat.BAMBOO_OK) {
                    return -1;
                }
                if (tokens.size() == 0) {
                    return -1;
                }
                if (putNS(NS_HASH_STORAGE, ((Triple) tokens.get(0)).first, ((Triple) tokens.get(0)).second, ttl, ((Triple) tokens.get(0)).first) != bamboo_stat.BAMBOO_OK) {
                    return -1;
                }
            }
            if (largePut(appId, key, v.first, ttl, v.second) < 0) {
                return -1;
            }
        }
        return 0;
    }

    public void join() throws Exception {
        started = true;
        StringBuffer sbuf = new StringBuffer(1000);
        sbuf.append("<sandstorm>\n");
        sbuf.append("<global>\n");
        sbuf.append("<initargs>\n");
        sbuf.append("node_id " + InetAddress.getByName(config.getProperty("hostname")).getHostName() + ":" + DHT.DHT_PORT + "\n");
        sbuf.append("</initargs>\n");
        sbuf.append("</global>\n");
        sbuf.append("<stages>\n");
        sbuf.append("<Network>\n");
        sbuf.append("class bamboo.network.Network\n");
        sbuf.append("<initargs>\n");
        sbuf.append("</initargs>\n");
        sbuf.append("</Network>\n");
        sbuf.append("<Router>\n");
        sbuf.append("class bamboo.router.Router\n");
        sbuf.append("<initargs>\n");
        Iterator it = bootstrap.getAllPeers().iterator();
        int i = 0;
        for (i = 0; it.hasNext(); i++) {
            InetAddress address = (InetAddress) it.next();
            sbuf.append("gateway_" + i + "     " + (address.getHostAddress()) + ":" + DHT.DHT_PORT + "\n");
        }
        sbuf.append("gateway_count " + i + "\n");
        sbuf.append("immediate_join true\n");
        sbuf.append("</initargs>\n");
        sbuf.append("</Router>\n");
        sbuf.append("<DataManager>\n");
        sbuf.append("class bamboo.dmgr.DataManager\n");
        sbuf.append("<initargs>\n");
        sbuf.append("merkle_tree_expansion        2\n");
        sbuf.append("desired_replicas        " + DHT.NUM_REPLICAS + "\n");
        sbuf.append("</initargs>\n");
        sbuf.append("</DataManager>\n");
        sbuf.append("<Rpc>\n");
        sbuf.append("class bamboo.lss.Rpc\n");
        sbuf.append("<initargs>\n");
        sbuf.append("</initargs>\n");
        sbuf.append("</Rpc>\n");
        sbuf.append("<StorageManager>\n");
        sbuf.append("class bamboo.db.StorageManager\n");
        sbuf.append("<initargs>\n");
        sbuf.append("homedir       " + config.getProperty("storage") + "\n");
        sbuf.append("</initargs>\n");
        sbuf.append("</StorageManager>\n");
        sbuf.append("<Dht>\n");
        sbuf.append("class bamboo.dht.Dht\n");
        sbuf.append("<initargs>\n");
        sbuf.append("storage_manager_stage StorageManager\n");
        sbuf.append("min_replica_count     1\n");
        sbuf.append("</initargs>\n");
        sbuf.append("</Dht>\n");
        sbuf.append("<Gateway>\n");
        sbuf.append("class bamboo.dht.Gateway\n");
        sbuf.append("<initargs>\n");
        sbuf.append("port           " + DHT.RPC_PORT + "\n");
        sbuf.append("</initargs>\n");
        sbuf.append("</Gateway>\n");
        if (WEBINTERFACE) {
            sbuf.append("<WebInterface>\n");
            sbuf.append("class bamboo.www.WebInterface\n");
            sbuf.append("<initargs>\n");
            sbuf.append("storage_manager_stage StorageManager\n");
            sbuf.append("</initargs>\n");
            sbuf.append("</WebInterface>\n");
        }
        sbuf.append("</stages>\n");
        sbuf.append("</sandstorm>\n");
        ASyncCore acore = new bamboo.lss.ASyncCoreImpl();
        DustDevil dd = new DustDevil();
        dd.set_acore_instance(acore);
        dd.main(new CharArrayReader(sbuf.toString().toCharArray()));
        acore.async_main();
    }

    public boolean isStarted() {
        return started;
    }
}
