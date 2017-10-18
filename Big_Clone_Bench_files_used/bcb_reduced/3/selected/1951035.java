package net.anydigit.jiliu.balance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.anydigit.jiliu.ProxyRequest;
import net.anydigit.jiliu.ServerNode;
import net.anydigit.jiliu.hash.IntegerHash;
import net.anydigit.jiliu.hash.NoSuchAlgorithmException;

/**
 * @author xingfei [xingfei0831 AT gmail.com]
 * 
 */
public abstract class AbstractBalance implements Balance {

    protected String name;

    protected String hashAlgo;

    protected int virtualNodeNumPerServer = 200;

    protected int virtualNodeNum = 0;

    protected long[] virtualNodeKeys;

    protected Map<Long, ServerNode> nodeMap = new HashMap<Long, ServerNode>();

    private int current;

    public AbstractBalance() {
        super();
    }

    public AbstractBalance(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void initialize(List<ServerNode> servers, String hashAlgo) {
        this.hashAlgo = hashAlgo;
        this.calculateVirtualNodesCircle(servers);
        postInit();
    }

    private void calculateVirtualNodesCircle(List<ServerNode> servers) {
        int j = 0;
        this.virtualNodeNum = virtualNodeNumPerServer * servers.size();
        this.virtualNodeKeys = new long[this.virtualNodeNum];
        for (ServerNode node : servers) {
            String hashKey = node.hashKey();
            for (int i = 0; i < virtualNodeNumPerServer; i++) {
                long h = hash(String.format("virtual node key %d for %s", i, hashKey));
                this.nodeMap.put(h, node);
                this.virtualNodeKeys[j] = h;
                j++;
            }
        }
        Arrays.sort(this.virtualNodeKeys);
    }

    protected long hash(String key) {
        IntegerHash ih = IntegerHash.getInstance(this.hashAlgo);
        ih.update(key.getBytes());
        return ih.digest();
    }

    public static int binarySearchCeil(long k, long[] src) throws NoSuchAlgorithmException {
        int m = 0;
        int end = src.length - 1;
        if (k <= src[end]) {
            int start = 0;
            while (end - start > 1) {
                int t = start + end;
                if (t % 2 == 0) {
                    m = t / 2;
                } else {
                    m = (t + 1) / 2;
                }
                if (src[m] > k) {
                    end = m;
                } else if (src[m] < k) {
                    start = m;
                } else {
                    break;
                }
            }
        }
        return m;
    }

    protected abstract String getKey(ProxyRequest request);

    protected void postInit() {
    }

    private Object lock = new Object();

    @Override
    public ServerNode getServer(ProxyRequest request) {
        String key = getKey(request);
        int n = -1;
        if (key == null) {
            synchronized (lock) {
                this.current++;
                if (this.current == this.virtualNodeKeys.length) {
                    this.current = 0;
                }
                n = this.current;
            }
        } else {
            long h = hash(key);
            n = binarySearchCeil(h, this.virtualNodeKeys);
        }
        return this.nodeMap.get(this.virtualNodeKeys[n]);
    }
}
