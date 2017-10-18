package antiquity.rpc.impl;

import antiquity.util.XdrUtils;
import antiquity.rpc.api.RpcReq;
import antiquity.rpc.api.RpcResp;
import antiquity.rpc.api.RpcCall;
import antiquity.rpc.api.RpcReply;
import antiquity.rpc.api.OpaqueAuth;
import antiquity.rpc.api.RpcRegisterReq;
import antiquity.rpc.api.RpcRegisterResp;
import bamboo.db.StorageManager;
import bamboo.lss.ASyncCore;
import bamboo.lss.ASyncCoreImpl;
import bamboo.lss.NioMultiplePacketInputBuffer;
import bamboo.util.GuidTools;
import bamboo.util.XdrByteBufferEncodingStream;
import bamboo.util.XdrClone;
import bamboo.util.XdrInputBufferDecodingStream;
import antiquity.util.XdrOutputBufferEncodingStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.LinkedList;
import java.util.Vector;
import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.XdrDecodingStream;
import org.acplt.oncrpc.XdrEncodingStream;
import org.acplt.oncrpc.XdrInt;
import org.acplt.oncrpc.XdrVoid;
import ostore.dispatch.Filter;
import ostore.util.InputBuffer;
import ostore.util.ByteUtils;
import ostore.util.NodeId;
import ostore.util.Pair;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.SinkException;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.EventHandlerException;
import seda.sandStorm.api.StagesInitializedSignal;
import seda.sandStorm.api.SingleThreadedEventHandlerIF;
import bamboo.dht.Dht;
import org.apache.log4j.Level;
import static bamboo.util.Curry.Thunk1;
import static bamboo.util.Curry.Thunk2;
import static bamboo.util.Curry.Thunk4;
import static bamboo.util.Curry.Thunk5;
import static bamboo.util.Curry.Thunk6;
import static bamboo.util.Curry.Thunk7;
import static bamboo.util.Curry.Function0;
import static bamboo.util.Curry.Function1;
import static bamboo.util.Curry.Function4;
import static bamboo.util.Curry.curry;
import static bamboo.util.StringUtil.bytes_to_sbuf;
import static antiquity.rpc.api.ProcInfo.ProcKey;
import static antiquity.rpc.api.ProcInfo.ProcValue;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.nio.channels.SelectionKey.OP_ACCEPT;

/**
 * An event-driven rpc server stub.  Stub uses Sun RPC over TCP.
 *
 * @author Hakim Weatherspoon
 * @version $Id: RpcServerStage.java,v 1.2 2007/06/19 14:47:02 hweather Exp $
 */
public class RpcServerStage extends ostore.util.StandardStage implements SingleThreadedEventHandlerIF {

    /** Connection timeout. */
    private long CONN_TIMEOUT;

    /** Connection timeout. */
    private int REQ_OUTSTANDING_FACTOR;

    /** 
   * {@link java.util.Map} of {@link ostore.util.NodeId} to
   * <code>RpcServerStage</code> instances used by local rpc calls. */
    private static Map<NodeId, RpcServerStage> instances;

    /** private instance of {@link bamboo.lss.ASyncCore} only for network. */
    private ASyncCore _acore, _acoreMain;

    /** Flag indicating that the RpcServerStage stage is in its own thread. */
    private boolean _ownThread;

    /** EventHandlerIF variables. Block events b/c DD was not ready */
    private Vector<QueueElementIF> _blockedEvents;

    /** EventHAndlerIF variables. All local stages have been initialized */
    private boolean _stagesInitialized;

    /** Accept loop variables.  ServerSocket. */
    private ServerSocket _ssock;

    /** Accept loop variables.  ServerSocketChannel. */
    private ServerSocketChannel _ssock_channel;

    /** Accept loop variables.  Server port. */
    private int _server_port;

    /** Size of an rpc fragment (in bytes) and max number of frags to cache. */
    private int FRAGMENT_SIZE, MAX_FRAGS_CACHE;

    /** allocate direct {@link java.nio.ByteBuffer}s. */
    private boolean DIRECT;

    /**
   * {@link java.util.LinkedList} of {@link java.nio.ByteBuffer}s, where each
   * {@link java.nio.ByteBuffer} can be reused as a packet
   * when (de)serializing an object.  Note that each reused 
   * {@link java.nio.ByteBuffer} is {@link java.util.LinkedList#remove removed}
   * from this {@link java.util.LinkedList} while in use. */
    private LinkedList<ByteBuffer> _reuse;

    /** 
   * {@link java.util.Map} of {@link antiquity.rpc.api.ProcInfo.ProcKey} to
   * {@link antiquity.rpc.api.ProcInfo.ProcValue}.  That is,
   * this map is used to marshall and unmarshall remote procedure call
   * arguements and return values. */
    private SortedMap<ProcKey, ProcValue> _procedures;

    /** 
   * {@link java.util.Map} of {@link antiquity.rpc.api.ProcInfo.ProcKey} to
   * {@link antiquity.rpc.impl.RpcServerStage.Handler}.  That is,
   * stage is used to map a procedure with the proper handling stage. */
    private Map<ProcKey, Handler> _handlers;

    private java.security.MessageDigest _md;

    static {
        instances = new HashMap<NodeId, RpcServerStage>();
    }

    /** Constructor: Creates a new <code>RpcServerStage</code> stage. */
    public RpcServerStage() throws Exception {
        event_types = new Class[] { seda.sandStorm.api.StagesInitializedSignal.class };
        _blockedEvents = new Vector<QueueElementIF>();
        _procedures = new TreeMap<ProcKey, ProcValue>();
        _handlers = new HashMap<ProcKey, Handler>();
        _reuse = new LinkedList<ByteBuffer>();
        try {
            _md = java.security.MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            logger.fatal("exception", e);
            System.exit(1);
        }
        logger.info("Initializing RpcServerStage...");
    }

    /** 
   * <code>getInstance</code> returns a 
   * {@link antiquity.rpc.impl.RpcServerStage} for a given 
   * {@link ostore.util.NodeId}.
   *
   * @param {@link ostore.util.NodeId} to lookup.
   * @return {@link antiquity.rpc.impl.RpcServerStage}. */
    public static RpcServerStage getInstance(NodeId node_id) {
        return instances.get(node_id);
    }

    /** 
   * <code>setInstance</code> sets a 
   * {@link antiquity.rpc.impl.RpcServerStage} for a given 
   * {@link ostore.util.NodeId}.
   *
   * @param {@link ostore.util.NodeId} to lookup.
   * @param {@link antiquity.rpc.impl.RpcServerStage}. */
    public static void setInstance(NodeId node_id, RpcServerStage stage) {
        instances.put(node_id, stage);
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void init(ConfigDataIF config) throws Exception {
        super.init(config);
        String tag = config.getStage().getName();
        if (logger.isInfoEnabled()) logger.info("initializing gateway id=" + my_node_id);
        assert config.contains("ConnTimeout") : "Need to define ConnTimeout in cfg file";
        CONN_TIMEOUT = config_get_long(config, "ConnTimeout");
        assert config.contains("ReqOutstandingFactor") : "Need to define ReqOutstandingFactor in cfg file";
        REQ_OUTSTANDING_FACTOR = config_get_int(config, "ReqOutstandingFactor");
        assert config.contains("port") : "Need to define port in cfg file";
        _server_port = config.getInt("port");
        NodeId gid = new NodeId(_server_port, my_node_id.address());
        setInstance(gid, this);
        _ownThread = config.getBoolean("rpcThread");
        if (!_ownThread) {
            _acore = _acoreMain = bamboo.lss.DustDevil.acore_instance();
        } else {
            _acoreMain = bamboo.lss.DustDevil.acore_instance();
            try {
                _acore = new ASyncCoreImpl();
            } catch (IOException e) {
                assert false : "could not open selector.  " + e;
            }
        }
        Class[] rpc_msg_types = new Class[] { antiquity.rpc.api.RpcReply.class, antiquity.rpc.api.RpcRegisterReq.class };
        for (Class clazz : rpc_msg_types) {
            Filter filter = new Filter();
            if (!filter.requireType(clazz)) BUG(tag + ": could not require type " + clazz.getName());
            if (antiquity.rpc.api.RpcReply.class.isAssignableFrom(clazz)) {
                if (!filter.requireValue("inbound", new Boolean(false))) BUG(tag + ": could not require inbound = false for " + clazz.getName());
            } else if (antiquity.rpc.api.RpcRegisterReq.class.isAssignableFrom(clazz)) {
                if (!filter.requireValue("client", new Boolean(false))) BUG(tag + ": could not require client = false for " + clazz.getName());
            } else {
                assert false;
            }
            if (logger.isDebugEnabled()) logger.debug(tag + ": subscribing to " + clazz);
            classifier.subscribe(filter, my_sink);
        }
        if (!sim_running) {
            if (!config.contains("numFragsCache")) BUG("need to define numFragsCache in cfg file");
            MAX_FRAGS_CACHE = config_get_int(config, "numFragsCache");
            if (!config.contains("fragmentSize")) BUG("need to define fragmentSize in cfg file");
            FRAGMENT_SIZE = config_get_int(config, "fragmentSize");
            if (!config.contains("allocateDirect")) BUG("need to define allocateDirect in cfg file");
            DIRECT = config_get_boolean(config, "allocateDirect");
            _ssock_channel = ServerSocketChannel.open();
            _ssock = _ssock_channel.socket();
            _ssock.bind(new InetSocketAddress(_server_port));
            _ssock_channel.configureBlocking(false);
            _acore.register_selectable(_ssock_channel, OP_ACCEPT, accept_cb);
            if (_ownThread) {
                Thread t = new Thread() {

                    public void run() {
                        try {
                            _acore.async_main();
                        } catch (OutOfMemoryError e) {
                            System.gc();
                            logger.fatal("out of memory error", e);
                            System.exit(1);
                        } catch (Throwable e) {
                            logger.fatal("uncaught exception", e);
                            System.exit(1);
                        }
                    }
                };
                t.setName("RpcServerStage-" + gid);
                t.start();
            }
        }
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void handleEvent(QueueElementIF item) {
        if (logger.isDebugEnabled()) logger.debug("got " + item);
        if (item instanceof RpcReply) {
            RpcReply resp = (RpcReply) item;
            assert resp.server_stub_cb != null : "server_stub_cb is null for " + resp;
            resp.server_stub_cb.run(resp);
        } else if (!_ownThread) {
            theRealHandleEvent(item);
        } else {
            _acore.register_timer(0L, _handleEventCB, item);
        }
    }

    private void theRealHandleEvent(QueueElementIF item) {
        if (item instanceof StagesInitializedSignal) {
            handleStagesInitializedSignal((StagesInitializedSignal) item);
            return;
        } else if (item instanceof RpcRegisterReq) {
            RpcRegisterReq req = (RpcRegisterReq) item;
            Map<ProcKey, Thunk1<RpcCall>> handlers = req.handlers;
            Map<ProcKey, Integer> responses = new HashMap<ProcKey, Integer>();
            for (Map.Entry<ProcKey, ProcValue> entry : req.procedures.entrySet()) {
                ProcKey key = (ProcKey) entry.getKey();
                ProcValue proc = (ProcValue) entry.getValue();
                Thunk1<RpcCall> handler_cb = null;
                if (handlers != null) {
                    handler_cb = handlers.get(key);
                    if (handler_cb == null) {
                        responses.put(key, RpcRegisterResp.ERROR);
                        continue;
                    }
                }
                if (req.sink == null && handler_cb == null) {
                    responses.put(key, RpcRegisterResp.ERROR);
                    continue;
                }
                if (!_procedures.containsKey(key)) {
                    Handler handler = new Handler(req.appId, req.sink, handler_cb);
                    _procedures.put(key, proc);
                    _handlers.put(key, handler);
                    responses.put(key, RpcRegisterResp.SUCCESS);
                } else {
                    assert _handlers.containsKey(key) : "handlers.containsKey(" + key + ") is null for " + req;
                    Handler handler = (Handler) _handlers.get(key);
                    if (handler.appId == req.appId && handler.sink == req.sink && handler.cb == handler_cb) responses.put(key, RpcRegisterResp.SUCCESS); else responses.put(key, RpcRegisterResp.ERROR);
                }
            }
            NodeId gid = new NodeId(_server_port, my_node_id.address());
            RpcRegisterResp resp = new RpcRegisterResp(responses, req.client, gid, req.appId, req.userData);
            applicationEnqueue(null, req, resp);
        } else if (!_stagesInitialized) {
            if (logger.isDebugEnabled()) logger.debug(": Queueing event " + item + " until stages initialized.");
            _blockedEvents.add(item);
            return;
        } else if (item instanceof RpcReply) {
            RpcReply resp = (RpcReply) item;
            assert resp.server_stub_cb != null : "server_stub_cb is null for " + resp;
            resp.server_stub_cb.run(resp);
        } else {
            BUG("unexpected event: " + item);
        }
    }

    /**
   * <CODE>handleStagesInitializedSignal</CODE> unblocks any blocked events.
   *
   * @param signal {@link seda.sandStorm.api.StagesInitializedSignal} */
    private void handleStagesInitializedSignal(StagesInitializedSignal signal) {
        final String method_tag = ".handleStagesInitializedSignal";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called " + signal);
        _stagesInitialized = true;
        while (_blockedEvents.size() > 0) {
            handleEvent((QueueElementIF) _blockedEvents.remove(0));
        }
        return;
    }

    /**********************************************************************/
    private Runnable accept_cb = new Runnable() {

        public void run() {
            SocketChannel sc = null;
            try {
                sc = _ssock_channel.accept();
            } catch (IOException e) {
                BUG(e);
            }
            if (sc == null) return;
            if (logger.isInfoEnabled()) {
                Socket s = sc.socket();
                StringBuffer buf = new StringBuffer(45);
                buf.append("got connection from ");
                buf.append(s.getInetAddress().getHostAddress());
                buf.append(":");
                buf.append(s.getPort());
                logger.info(buf);
            }
            try {
                sc.configureBlocking(false);
            } catch (IOException e) {
                BUG(e);
            }
            MyConnState conn = new MyConnState(sc);
            try {
                _acore.register_selectable(sc, OP_READ, conn.read_cb);
            } catch (ClosedChannelException e) {
                BUG(e);
            }
        }
    };

    /**
   * <code>log_req</code> logs the {@link antiquity.rpc.api.RpcCall}. */
    private void log_req(int xact_id, RpcCall req, String client, Integer size) {
        if (logger.isInfoEnabled()) {
            String call = XdrUtils.toString(req.args);
            StringBuffer buf = new StringBuffer(100 + call.length());
            buf.append("LOG");
            buf.append(" req client=");
            buf.append(client);
            buf.append(" xact_id=0x");
            buf.append(Integer.toHexString(xact_id));
            buf.append(" size=");
            buf.append(size);
            buf.append(" call=");
            buf.append(call);
            logger.info(buf);
        }
    }

    /**
   * <code>log_resp</code> logs the {@link antiquity.rpc.api.RpcReply}. */
    private Thunk6<Integer, RpcCall, RpcReply, String, Long, Long> log_resp = new Thunk6<Integer, RpcCall, RpcReply, String, Long, Long>() {

        /**
       * Log the {@link antiquity.rpc.api.RpcReply}. 
       * @param xact_id  Transaction id.
       * @param req      {@link antiquity.rpc.api.RpcCall}
       * @param resp     {@link antiquity.rpc.api.RpcReply}
       * @param client   {@link ostore.util.NodeId} of caller.
       * @param start_us Start time of RPC call (in us). 
       * @param size     size of resp in bytes. */
        public void run(Integer xact_id, RpcCall req, RpcReply resp, String client, Long start_us, Long size) {
            if (logger.isInfoEnabled()) {
                String call = XdrUtils.toString(req.args);
                String rv = XdrUtils.toString(resp.reply);
                StringBuffer buf = new StringBuffer(200 + call.length() + rv.length());
                buf.append("LOG");
                buf.append(" resp client=");
                buf.append(client);
                buf.append(" xact_id=0x");
                buf.append(Integer.toHexString(xact_id));
                buf.append(" size=");
                buf.append(size);
                buf.append(" call=");
                buf.append(call);
                buf.append(" resp=");
                buf.append(rv);
                buf.append(" lat=");
                buf.append((now_us() - start_us.longValue()) / 1000.0);
                buf.append(" ms");
                logger.info(buf);
            }
        }
    };

    /** 
   * A {@link {@link bamboo.lss.ASyncCore.TimerCB} that calls 
   * {@link #theRealHandleEvent}. */
    private ASyncCore.TimerCB _handleEventCB = new ASyncCore.TimerCB() {

        public void timer_cb(Object userData) {
            theRealHandleEvent((QueueElementIF) userData);
        }

        ;
    };

    /** Call {@link seda.sandStorm.api.SinkIF#enqueue} from the main loop. */
    private Thunk2<QueueElementIF, SinkIF> main_loop_cb = new Thunk2<QueueElementIF, SinkIF>() {

        /** Call {@link seda.sandStorm.api.SinkIF#enqueue} from the main loop*/
        public void run(QueueElementIF item, SinkIF sink) {
            try {
                sink.enqueue(item);
            } catch (SinkException e) {
                BUG("couldn't enqueue " + e);
            }
        }
    };

    /** method to send event from Berkeley DB thread to main thread */
    private <T1 extends RpcReq<T1, T2>, T2 extends RpcResp> void applicationEnqueue(Thunk1<T1> cb, T1 req, T2 resp) {
        if (cb != null) {
            if (logger.isDebugEnabled()) logger.debug("invoking request handler for " + req);
            _acoreMain.register_timer(0L, curry(cb, req));
        } else if (req.cb != null) {
            if (logger.isDebugEnabled()) logger.debug("executing callback for " + resp);
            _acoreMain.register_timer(0L, curry(req.cb, req, resp));
        } else if (req.sink != null) {
            if (logger.isDebugEnabled()) logger.debug("enqueuing " + resp + " to " + req.sink);
            QueueElementIF event = (resp != null ? resp : req);
            _acoreMain.register_timer(0L, curry(main_loop_cb, event, req.sink));
        } else {
            BUG("either cb or sink should be defined for " + req);
        }
        return;
    }

    /** 
   * <code>getHandler</code> returns a 
   * {@link antiquity.rpc.impl.RpcServerStage.Handler} for a given 
   * {@link antiquity.rpc.api.ProcInfo.ProcKey}.
   *
   * @param {@link antiquity.rpc.api.ProcInfo.ProcKey} to lookup.
   * @return {@link antiquity.rpc.impl.RpcServerStage.Handler}. */
    public Handler getHandler(ProcKey proc) {
        return _handlers.get(proc);
    }

    /**
   * Container class that stores the {@link seda.sandStorm.api.SinkIF} 
   * and <code>appId</code> for the handling stage of a particular 
   * remote procedure call. */
    public static class Handler {

        public SinkIF sink;

        public long appId;

        public Thunk1<RpcCall> cb;

        /** 
     * Constructor: Creates a new <code>Handler</code>.
     * @param a <code>appId</code> of handler stage. 
     * @param s {@link seda.sandStorm.api.SinkIF} of handler stage.
     * @param c The callback of handler stage to use instead of sink. */
        public Handler(long a, SinkIF s, Thunk1<RpcCall> c) {
            appId = a;
            sink = s;
            cb = c;
        }

        /** Specified by java.lang.Object */
        public String toString() {
            return "(Handler appId=" + Long.toHexString(appId) + ")";
        }
    }

    private class MyConnState {

        public SocketChannel sc;

        public InetAddress addr;

        public String client_string;

        public NodeId client_node_id;

        public LinkedList<Function0<Boolean>> to_write;

        private NioMultiplePacketInputBuffer ib;

        private NioMultiplePacketOutputBuffer ob;

        private ConnectionStream is;

        private long last_activity_us;

        private int reqs_outstanding;

        private ByteBuffer[] write_bufs;

        private long write_bufs_position, write_bufs_limit;

        private int write_bufs_length, write_xact_id;

        private Set<Integer> handling;

        /** 
     * Constructor: Creates a new <code>MyConnState</code>.
     * @param sc {@link java.nio.channels.ServerSocketChannel}. */
        public MyConnState(SocketChannel sc) {
            this.sc = sc;
            addr = sc.socket().getInetAddress();
            client_string = addr.getHostAddress() + ":" + sc.socket().getPort();
            try {
                client_node_id = new NodeId(client_string);
            } catch (Exception e) {
                BUG(e);
            }
            last_activity_us = now_us();
            _acore.register_timer(CONN_TIMEOUT, timeout_cb);
            to_write = new LinkedList<Function0<Boolean>>();
            handling = new HashSet<Integer>();
            ib = new NioMultiplePacketInputBuffer();
            ob = new NioMultiplePacketOutputBuffer(FRAGMENT_SIZE, DIRECT, ByteUtils.SIZE_INT, _reuse);
            is = new ConnectionStream(logger.getLevel());
        }

        /** 
     * Timeout callback. Closes connection if time since last
     * activitity is longer than <code>CONN_TIMEOUT</code>. */
        private Runnable timeout_cb = new Runnable() {

            /** 
	 * Closes connection if time since last activitity is longer
	 * than <code>CONN_TIMEOUT</code>. */
            public void run() {
                if (sc.isOpen()) {
                    long now_us = now_us();
                    if ((reqs_outstanding <= 0 && (now_us - last_activity_us) > (CONN_TIMEOUT * 1000L)) || (reqs_outstanding > 0 && (now_us - last_activity_us) > (REQ_OUTSTANDING_FACTOR * CONN_TIMEOUT * 1000L))) error_close_connection("timeout " + reqs_outstanding + " pending reqs"); else _acore.register_timer(CONN_TIMEOUT, timeout_cb);
                }
            }
        };

        private Runnable read_cb = new Runnable() {

            public void run() {
                if (logger.isDebugEnabled()) logger.debug("op_read " + client_string);
                int count = 0;
                while (true) {
                    ByteBuffer read_buf = null;
                    if (_reuse.isEmpty()) read_buf = (DIRECT ? ByteBuffer.allocateDirect(FRAGMENT_SIZE) : ByteBuffer.allocate(FRAGMENT_SIZE)); else {
                        read_buf = _reuse.removeFirst();
                        read_buf.clear();
                    }
                    try {
                        count = sc.read(read_buf);
                    } catch (IOException e) {
                        conn_closed();
                        return;
                    }
                    if (count < 0) {
                        conn_closed();
                        return;
                    }
                    if (count == 0) break;
                    if (logger.isDebugEnabled()) logger.debug("read from " + client_string + " packet of " + count + " bytes");
                    read_buf.flip();
                    is.add_packet(read_buf);
                }
                while (true) {
                    if (!is.message_available()) break;
                    int inbound_size = is.next_message_size();
                    try {
                        is.get_next_message_buffer(ib);
                    } catch (Throwable e) {
                        BUG("message of size " + inbound_size + " is available from " + client_string + "; however, caught " + e);
                    }
                    assert ib.size() == inbound_size : "ib.size=" + ib.size() + " != inbound_size=" + inbound_size;
                    ib.limit(inbound_size);
                    int xact_id = ib.nextInt();
                    if (logger.isDebugEnabled()) logger.debug("req from " + client_string + " xact_id 0x" + Integer.toHexString(xact_id) + " is " + inbound_size + " bytes");
                    try {
                        int msg_type = ib.nextInt();
                        int rpcvers = ib.nextInt();
                        int prog = ib.nextInt();
                        int vers = ib.nextInt();
                        int proc = ib.nextInt();
                        ProcKey procKey = new ProcKey(prog, vers, proc);
                        ProcValue procedure = (ProcValue) _procedures.get(procKey);
                        if (procedure != null) {
                            Handler handler = (Handler) _handlers.get(procKey);
                            assert handler != null : "handler null for " + procKey + " " + procedure;
                            OpaqueAuth cred = null;
                            OpaqueAuth verifier = null;
                            for (int i = 0; i < 2; ++i) {
                                int flavor = ib.nextInt();
                                int len = ib.nextInt();
                                byte[] data = (len <= 0 ? null : new byte[len]);
                                for (int j = 0; j < len; ++j) data[j] = ib.nextByte();
                                if (i == 0) cred = new OpaqueAuth(flavor, data); else verifier = new OpaqueAuth(flavor, data);
                            }
                            if (logger.isDebugEnabled()) logger.debug("Decoding " + ib.limit_remaining() + " remaining bytes from " + client_string + " xact_id 0x" + Integer.toHexString(xact_id) + ".");
                            XdrInputBufferDecodingStream ds = new XdrInputBufferDecodingStream(ib, ib.limit_remaining());
                            ds.beginDecoding();
                            XdrAble args = (XdrAble) procedure.getArgsConstructor().newInstance(new Object[] {});
                            args.xdrDecode(ds);
                            assert ib.limit_remaining() == 0 : "For xact_id=0x" + Integer.toHexString(xact_id) + " ib.remaining=" + ib.limit_remaining();
                            for (ByteBuffer reuse : is.get_reuse_buffers()) {
                                reuse.clear();
                                assert reuse.capacity() == FRAGMENT_SIZE : "wrong buffer=" + reuse;
                                if (_reuse.size() < MAX_FRAGS_CACHE) _reuse.addFirst(reuse);
                            }
                            is.clear_reuse_buffers();
                            handle_rpc_req(xact_id, procKey, args, cred, verifier, handler, inbound_size);
                        } else {
                            for (ByteBuffer reuse : is.get_reuse_buffers()) {
                                reuse.clear();
                                assert reuse.capacity() == FRAGMENT_SIZE : "wrong buffer=" + reuse;
                                if (_reuse.size() < MAX_FRAGS_CACHE) _reuse.addFirst(reuse);
                            }
                            is.clear_reuse_buffers();
                            ProcKey lowKey = new ProcKey(prog, Integer.MIN_VALUE, Integer.MIN_VALUE);
                            ProcKey highKey = new ProcKey(prog, Integer.MAX_VALUE, Integer.MAX_VALUE);
                            SortedMap<ProcKey, ProcValue> map = _procedures.subMap(lowKey, highKey);
                            if (map.isEmpty()) {
                                try {
                                    _acore.unregister_selectable(sc, OP_READ);
                                } catch (ClosedChannelException e) {
                                    conn_closed();
                                }
                                send_resp(error_cb(xact_id, 1, "PROG_UNAVAIL (xact_id=" + Integer.toHexString(xact_id) + ", msg_type=" + msg_type + ", rpcvers=" + rpcvers + ", prog=" + prog + ", ver=" + vers + ", proc=" + proc + ", inbound_size=" + inbound_size + ")"));
                                break;
                            }
                            lowKey = new ProcKey(prog, vers, Integer.MIN_VALUE);
                            highKey = new ProcKey(prog, vers, Integer.MAX_VALUE);
                            map = map.subMap(lowKey, highKey);
                            if (map.isEmpty()) {
                                try {
                                    _acore.unregister_selectable(sc, OP_READ);
                                } catch (ClosedChannelException e) {
                                    conn_closed();
                                }
                                send_resp(mismatch_cb(xact_id));
                                break;
                            }
                            assert !_procedures.containsKey(procKey);
                            try {
                                _acore.unregister_selectable(sc, OP_READ);
                            } catch (ClosedChannelException e) {
                                conn_closed();
                            }
                            send_resp(error_cb(xact_id, 3, "PROC_UNAVAIL (xact_id=" + Integer.toHexString(xact_id) + ", msg_type=" + msg_type + ", rpcvers=" + rpcvers + ", prog=" + prog + ", ver=" + vers + ", proc=" + proc + ", inbound_size=" + inbound_size + ")"));
                            break;
                        }
                    } catch (Error garbage) {
                        try {
                            _acore.unregister_selectable(sc, OP_READ);
                        } catch (ClosedChannelException e) {
                            conn_closed();
                        }
                        send_resp(error_cb(xact_id, 4, "GARBAGE_ARGS (xact_id=" + Integer.toHexString(xact_id) + ", inbound_size=" + inbound_size + ")"));
                        break;
                    } catch (Exception garbage) {
                        logger.fatal(garbage);
                        try {
                            _acore.unregister_selectable(sc, OP_READ);
                        } catch (ClosedChannelException e) {
                            conn_closed();
                        }
                        send_resp(error_cb(xact_id, 4, "GARBAGE_ARGS (xact_id=" + Integer.toHexString(xact_id) + ", inbound_size=" + inbound_size + ")"));
                        break;
                    }
                }
                last_activity_us = now_us();
            }
        };

        private Runnable write_cb = new Runnable() {

            public void run() {
                if (logger.isDebugEnabled()) logger.debug("op_write " + client_string);
                while (!to_write.isEmpty()) {
                    Function0<Boolean> cb = to_write.getFirst();
                    try {
                        if (cb.run().booleanValue()) {
                            to_write.removeFirst();
                            for (int i = 0; i < write_bufs_length; i++) {
                                if (_reuse.size() < MAX_FRAGS_CACHE) _reuse.addFirst(write_bufs[i]);
                                write_bufs[i] = null;
                            }
                            write_bufs_position = write_bufs_limit = 0L;
                            write_bufs_length = 0;
                            write_xact_id = -1;
                        } else break;
                    } catch (Throwable e) {
                        BUG("error on cb=" + cb + " to " + client_string + (write_xact_id == -1 ? "" : " write_xact_id 0x" + Integer.toHexString(write_xact_id)) + ". error " + e);
                    }
                }
                if (to_write.isEmpty() && sc.isOpen()) {
                    try {
                        _acore.unregister_selectable(sc, OP_WRITE);
                    } catch (ClosedChannelException e) {
                        conn_closed();
                    }
                }
                last_activity_us = now_us();
            }
        };

        private Thunk2<String, Long> close_cb = new Thunk2<String, Long>() {

            public void run(String msg, Long size) {
                error_close_connection(msg);
            }
        };

        private Function0<Boolean> error_cb(int xact_id, int code, String m) {
            return curry(normal_resp_cb, new Integer(xact_id), new Integer(code), new XdrVoid(), curry(close_cb, m));
        }

        private Function1<Boolean, Thunk1<Long>> continue_write_cb = new Function1<Boolean, Thunk1<Long>>() {

            public Boolean run(Thunk1<Long> done_cb) {
                long n = 0;
                try {
                    n = sc.write(write_bufs, 0, write_bufs_length);
                } catch (IOException e) {
                    conn_closed();
                    return new Boolean(false);
                }
                write_bufs_position += n;
                if (logger.isDebugEnabled()) logger.debug("continue_write_cb: sent " + n + " bytes; " + (write_bufs_limit - write_bufs_position) + " bytes remaining to " + client_string + " write_xact_id 0x" + Integer.toHexString(write_xact_id));
                if (write_bufs_position == write_bufs_limit) {
                    done_cb.run(write_bufs_limit);
                    return new Boolean(true);
                } else {
                    if (logger.isDebugEnabled()) logger.debug("continue write of " + n + " bytes insufficient" + " to " + client_string + " write_xact_id 0x" + Integer.toHexString(write_xact_id) + "." + "  need to write " + (write_bufs_limit - write_bufs_position) + " bytes more of a total of " + write_bufs_limit + " bytes.");
                    return new Boolean(false);
                }
            }
        };

        private Function0<Boolean> mismatch_cb(int xact_id) {
            XdrAble resp = new XdrAble() {

                public void xdrDecode(XdrDecodingStream xdr) {
                    throw new NoSuchMethodError();
                }

                public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, java.io.IOException {
                    xdr.xdrEncodeInt(2);
                    xdr.xdrEncodeInt(2);
                }
            };
            return curry(normal_resp_cb, new Integer(xact_id), new Integer(2), resp, curry(close_cb, "PROC_MISMATCH"));
        }

        private Function4<Boolean, Integer, Integer, XdrAble, Thunk1<Long>> normal_resp_cb = new Function4<Boolean, Integer, Integer, XdrAble, Thunk1<Long>>() {

            public Boolean run(Integer xact_id, Integer code, XdrAble resp, Thunk1<Long> done_cb) {
                ob.add(xact_id.intValue());
                ob.add(1);
                ob.add(0);
                ob.add(0);
                ob.add(0);
                ob.add(code.intValue());
                XdrOutputBufferEncodingStream es = new XdrOutputBufferEncodingStream(ob);
                try {
                    resp.xdrEncode(es);
                } catch (Exception e) {
                    error_close_connection("error serializing rpc reply=" + resp + " error=" + e + " write_buf=" + ob);
                    return new Boolean(false);
                }
                LinkedList<ByteBuffer> list = ob.getList();
                int size = ob.size();
                ob.flip();
                if (write_bufs == null || write_bufs.length < list.size()) write_bufs = new ByteBuffer[list.size()];
                list.toArray(write_bufs);
                write_bufs_length = list.size();
                write_bufs_position = 0;
                write_bufs_limit = 0;
                write_xact_id = xact_id.intValue();
                ob.reset();
                for (int i = 0; i < write_bufs_length; i++) {
                    ByteBuffer packet = write_bufs[i];
                    int lastFrag = (i == write_bufs_length - 1 ? 0x80000000 : 0x00000000);
                    packet.putInt(lastFrag | (packet.limit() - 4));
                    packet.position(0);
                    write_bufs_limit += packet.limit();
                    if (logger.isDebugEnabled() && packet.hasArray()) {
                        _md.update(packet.array(), packet.arrayOffset(), packet.limit());
                        byte[] digest = _md.digest();
                        logger.debug("normal_resp to " + client_string + " for xact_id 0x" + Integer.toHexString(xact_id) + ": frag " + i + " hash: 0x" + ByteUtils.print_bytes(digest, 0, 4));
                    }
                }
                if (logger.isDebugEnabled()) logger.debug("For xact_id 0x" + Integer.toHexString(xact_id) + " to " + client_string + ", total serialized size " + size + ", total write bufs size=" + write_bufs_limit + ", in " + write_bufs_length + " packets.");
                long n = 0;
                try {
                    n = sc.write(write_bufs, 0, write_bufs_length);
                } catch (IOException e) {
                    conn_closed();
                    return new Boolean(false);
                }
                write_bufs_position += n;
                if (logger.isDebugEnabled()) logger.debug("write_cb: sent " + n + " bytes; " + (write_bufs_limit - write_bufs_position) + " bytes remaining to " + client_string + " xact_id 0x" + Integer.toHexString(write_xact_id));
                if (n == write_bufs_limit) {
                    done_cb.run(write_bufs_limit);
                    return new Boolean(true);
                } else {
                    if (logger.isDebugEnabled()) logger.debug("first write of " + n + " bytes insufficient to " + client_string + " xact_id 0x" + Integer.toHexString(write_xact_id) + ". " + " need to write " + (write_bufs_limit - write_bufs_position) + " bytes more of a total of " + write_bufs_limit + " bytes.");
                    to_write.removeFirst();
                    Function0<Boolean> func = curry(continue_write_cb, done_cb);
                    to_write.addFirst(func);
                    return new Boolean(false);
                }
            }
        };

        private void send_resp(Function0<Boolean> cb) {
            if (sc.isOpen()) {
                to_write.addLast(cb);
                try {
                    _acore.register_selectable(sc, OP_WRITE, write_cb);
                } catch (ClosedChannelException e) {
                    conn_closed();
                }
            }
        }

        private void handle_rpc_req(final Integer xact_id, ProcKey proc, XdrAble args, OpaqueAuth cred, OpaqueAuth verifier, Handler handler, Integer size) {
            last_activity_us = now_us();
            final long now = now_us();
            final Thread thread = Thread.currentThread();
            final RpcCall outb = new RpcCall(client_node_id, proc, args, handler.appId, null, handler.sink);
            outb.server_stub_cb = new Thunk1<RpcReply>() {

                public void run(RpcReply resp) {
                    if (thread != Thread.currentThread()) {
                        _acore.register_timer(0L, curry(resp.server_stub_cb, resp));
                        return;
                    }
                    rpc_done(outb, resp, xact_id, now);
                }
            };
            outb.xact_id = xact_id;
            outb.credentials = cred;
            outb.credentials = verifier;
            outb.inbound = true;
            if (!handling.contains(xact_id)) {
                handling.add(xact_id);
                log_req(xact_id, outb, client_string, size);
                reqs_outstanding++;
                applicationEnqueue(handler.cb, outb, (RpcReply) null);
            } else {
                logger.warn("rpc client stub from " + client_string + " sent duplicate requests xact_id 0x" + Integer.toHexString(xact_id) + ". Dropping duplicate req.");
            }
        }

        private void rpc_done(RpcCall req, RpcReply resp, Integer xact_id, long start_us) {
            if (handling.contains(xact_id)) {
                handling.remove(xact_id);
                reqs_outstanding--;
                Thunk1<Long> log_fn = curry(log_resp, xact_id, req, resp, client_string, new Long(start_us));
                send_resp(curry(normal_resp_cb, xact_id, new Integer(0), resp.reply, log_fn));
            } else {
                logger.warn("handling stage sent response for non-existent" + " xact_id 0x" + Integer.toHexString(xact_id) + " to send to " + client_string + ".  Dropping resp");
            }
        }

        private void conn_closed() {
            _acore.unregister_selectable(sc);
            try {
                sc.close();
            } catch (IOException e) {
            }
            if (logger.isInfoEnabled()) {
                StringBuffer buf = new StringBuffer(45);
                buf.append("connection closed by ");
                buf.append(client_string);
                logger.info(buf);
            }
            clean_up();
        }

        private void error_close_connection(String msg) {
            _acore.unregister_selectable(sc);
            try {
                sc.close();
            } catch (IOException e) {
            }
            if (true) {
                StringBuffer buf = new StringBuffer(45 + msg.length());
                buf.append("closing connection to ");
                buf.append(client_string);
                buf.append(": ");
                buf.append(msg);
                logger.fatal(buf);
            }
            clean_up();
        }

        private void clean_up() {
            if (!is.get_reuse_buffers().isEmpty()) {
                for (ByteBuffer reuse : is.get_reuse_buffers()) {
                    reuse.clear();
                    assert reuse.capacity() == FRAGMENT_SIZE : "wrong buffer=" + reuse;
                    if (_reuse.size() < MAX_FRAGS_CACHE) _reuse.addFirst(reuse);
                }
                is.clear_reuse_buffers();
            }
            is.finalize();
            is = null;
            if (write_bufs != null) {
                for (ByteBuffer write_buf : write_bufs) {
                    if (write_buf != null && _reuse.size() < MAX_FRAGS_CACHE) _reuse.addFirst(write_buf);
                }
                write_bufs = null;
                write_bufs_position = write_bufs_limit = 0L;
                write_bufs_length = 0;
                write_xact_id = -1;
            }
            for (int i = 0; i < 4; i++) System.gc();
        }
    }
}
