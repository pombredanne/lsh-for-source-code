package antiquity.rpc.impl;

import antiquity.rpc.api.RpcCall;
import antiquity.rpc.api.RpcReply;
import antiquity.rpc.api.RpcReq;
import antiquity.rpc.api.RpcResp;
import antiquity.rpc.api.OpaqueAuth;
import antiquity.rpc.api.RpcRegisterReq;
import antiquity.rpc.api.RpcRegisterResp;
import antiquity.util.ValuePair;
import antiquity.util.ValueTriple;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.ClosedChannelException;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import java.util.Map;
import java.util.Vector;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.LinkedList;
import ostore.util.NodeId;
import ostore.util.ByteUtils;
import ostore.util.CountBuffer;
import ostore.util.StandardStage;
import ostore.dispatch.Filter;
import seda.sandStorm.api.StagesInitializedSignal;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SinkException;
import seda.sandStorm.api.SinkIF;
import bamboo.lss.ASyncCore;
import bamboo.lss.ASyncCoreImpl;
import bamboo.util.XdrInputBufferDecodingStream;
import antiquity.util.XdrUtils;
import antiquity.util.XdrOutputBufferEncodingStream;
import bamboo.lss.NioMultiplePacketInputBuffer;
import org.acplt.oncrpc.XdrVoid;
import org.acplt.oncrpc.XdrAble;
import bamboo.util.XdrClone;
import bamboo.sim.Network;
import java.security.SecureRandom;
import java.lang.reflect.Constructor;
import org.apache.log4j.Level;
import static bamboo.util.Curry.Thunk1;
import static bamboo.util.Curry.Thunk2;
import static bamboo.util.Curry.Thunk3;
import static bamboo.util.Curry.Thunk5;
import static bamboo.util.Curry.curry;
import static antiquity.rpc.api.ProcInfo.ProcKey;
import static antiquity.rpc.api.ProcInfo.ProcValue;
import static antiquity.rpc.impl.RpcServerStage.Handler;

/**
 *    **** TODO: fix this stage. 
 *    **** 1) Performance can be improved measurable by creating
 *    ****    large, reusable, direct byte buffers.  We need
 *    ****    to come up with a scheme to do that.
 *    **** 2) Make sure that we read in the required number of
 *    ****    bytes, before we attempt to decode.
 *    **** 3) The auto generated rpc stub uses Rpc Fragments.
 *    ****    We need to figure out how we are going to implement
 *    ****    Rpc fragments, so that we can properly interoperate
 *    ****    with other Rpc programs.
 *
 * An event-driven rpc client stub.  Stub uses Sun RPC over TCP.
 *
 * @author Hakim Weatherspoon
 * @version $Id: RpcClientStage.java,v 1.2 2007/09/04 21:57:38 hweather Exp $
 */
public class RpcClientStage extends StandardStage {

    private static Constructor VOID_CONSTRUCTOR;

    private static Map<NodeId, RpcClientStage> instances;

    /** local {@link java.nio.ByteBuffer}. */
    private ByteBuffer _bbuf;

    /** local {@link ostore.util.CountBuffer}. */
    private CountBuffer _cbuf;

    /** Connection timeout. */
    private long CONN_TIMEOUT;

    /** Connection timeout. */
    private int REQ_OUTSTANDING_FACTOR;

    private int next_xact_id;

    /** 
   * {@link java.util.Map} associating {@link ostore.util.NodeId gatway} 
   * to {@link antiquity.rpc.impl.RpcClientStage.MyConnState}. */
    private Map<NodeId, MyConnState> _connections;

    /** 
   * {@link java.util.Map} for local rpc and simulation mode associating 
   * <code>xact_id</code> to {@link antiquity.rpc.api.RpcCall}. */
    private Map<Integer, RpcCall> _inflight;

    /** private instance of {@link bamboo.lss.ASyncCore} only for network. */
    private ASyncCore _acore, _acoreMain;

    /** Flag indicating that the RpcServerStage stage is in its own thread. */
    private boolean _ownThread;

    /** EventHandlerIF variables. Block events b/c DD was not ready */
    private Vector<QueueElementIF> _blockedEvents;

    /** EventHAndlerIF variables. All local stages have been initialized */
    private boolean _stagesInitialized;

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
        try {
            VOID_CONSTRUCTOR = XdrVoid.class.getConstructor(new Class[] {});
        } catch (NoSuchMethodException e) {
            System.err.println(e);
            System.exit(1);
        }
        instances = new HashMap<NodeId, RpcClientStage>();
    }

    /** Constructor: Creates a new <code>RpcClientStage</code> stage. */
    public RpcClientStage() throws Exception {
        event_types = new Class[] { seda.sandStorm.api.StagesInitializedSignal.class };
        _blockedEvents = new Vector<QueueElementIF>();
        _procedures = new TreeMap<ProcKey, ProcValue>();
        _handlers = new HashMap<ProcKey, Handler>();
        _connections = new HashMap<NodeId, MyConnState>();
        _reuse = new LinkedList<ByteBuffer>();
        try {
            _md = java.security.MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            logger.fatal("exception", e);
            System.exit(1);
        }
        logger.info("Initializing RpcClientStage...");
    }

    public static RpcClientStage getInstance(NodeId node_id) {
        return instances.get(node_id);
    }

    /**
   * <code>getTimeout</code> returns the time (in ms) used to timeout
   * a connection <i>with</i> outstanding requests. 
   *
   * @return the time (in ms) used to timeout a connection <i>with</i>
   * outstanding requests. */
    public long getTimeout() {
        return REQ_OUTSTANDING_FACTOR * CONN_TIMEOUT;
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void init(ConfigDataIF config) throws Exception {
        super.init(config);
        String tag = config.getStage().getName();
        instances.put(my_node_id, this);
        assert config.contains("ConnTimeout") : "Need to define ConnTimeout in cfg file";
        CONN_TIMEOUT = config_get_long(config, "ConnTimeout");
        assert config.contains("ReqOutstandingFactor") : "Need to define ReqOutstandingFactor in cfg file";
        REQ_OUTSTANDING_FACTOR = config_get_int(config, "ReqOutstandingFactor");
        _ownThread = config_get_boolean(config, "rpcThread");
        if (!_ownThread || sim_running) {
            _acore = _acoreMain = bamboo.lss.DustDevil.acore_instance();
        } else {
            _acoreMain = bamboo.lss.DustDevil.acore_instance();
            try {
                _acore = new ASyncCoreImpl();
            } catch (IOException e) {
                assert false : "could not open selector.  " + e;
            }
        }
        Class[] rpc_msg_types = new Class[] { antiquity.rpc.api.RpcCall.class, antiquity.rpc.api.RpcRegisterReq.class };
        for (Class clazz : rpc_msg_types) {
            Filter filter = new Filter();
            if (!filter.requireType(clazz)) BUG(tag + ": could not require type " + clazz.getName());
            if (antiquity.rpc.api.RpcCall.class.isAssignableFrom(clazz)) {
                if (!filter.requireValue("inbound", new Boolean(false))) BUG(tag + ": could not require inbound = false for " + clazz.getName());
            } else if (antiquity.rpc.api.RpcRegisterReq.class.isAssignableFrom(clazz)) {
                if (!filter.requireValue("client", new Boolean(true))) BUG(tag + ": could not require client = false for " + clazz.getName());
            } else {
                assert false;
            }
            if (logger.isDebugEnabled()) logger.debug(tag + ": subscribing to " + clazz);
            classifier.subscribe(filter, my_sink);
        }
        if (!config.contains("numFragsCache")) BUG("need to define numFragsCache in cfg file");
        MAX_FRAGS_CACHE = config_get_int(config, "numFragsCache");
        if (!config.contains("fragmentSize")) BUG("need to define fragmentSize in cfg file");
        FRAGMENT_SIZE = config_get_int(config, "fragmentSize");
        if (!config.contains("allocateDirect")) BUG("need to define allocateDirect in cfg file");
        DIRECT = config_get_boolean(config, "allocateDirect");
        SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
        next_xact_id = rand.nextInt();
        if (!sim_running && _ownThread) {
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
            t.setName("RpcClientStage-" + my_node_id);
            t.start();
        }
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void handleEvent(QueueElementIF item) {
        if (logger.isDebugEnabled()) logger.debug("got " + item);
        if (!_ownThread || sim_running) {
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
            handleRpcRegisterReq((RpcRegisterReq) item);
            return;
        } else if (!_stagesInitialized) {
            if (logger.isDebugEnabled()) logger.debug(": Queueing event " + item + " until stages initialized.");
            _blockedEvents.add(item);
            return;
        } else if (item instanceof RpcCall) {
            handleRpcCall((RpcCall) item);
        } else BUG("unexpected event: " + item);
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
    private void handleRpcRegisterReq(RpcRegisterReq req) {
        final String method_tag = tag + ".handleRpcRegisterReq";
        if ((req.sink == null) && (req.cb == null)) {
            logger.warn(method_tag + ": Dropping request " + req + ".  No way to respond.");
            return;
        }
        Map<ProcKey, Integer> responses = new HashMap<ProcKey, Integer>();
        for (Map.Entry<ProcKey, ProcValue> entry : req.procedures.entrySet()) {
            ProcKey key = (ProcKey) entry.getKey();
            ProcValue proc = (ProcValue) entry.getValue();
            if (!_procedures.containsKey(key)) {
                Handler handler = new Handler(req.appId, req.sink, (Thunk1<RpcCall>) null);
                _procedures.put(key, proc);
                _handlers.put(key, handler);
                responses.put(key, RpcRegisterResp.SUCCESS);
            } else {
                assert _handlers.containsKey(key) : "handlers.containsKey(" + key + ") is null for " + req;
                Handler handler = (Handler) _handlers.get(key);
                if (handler.appId == req.appId && handler.sink == req.sink) responses.put(key, RpcRegisterResp.SUCCESS); else {
                    logger.warn(method_tag + ": more than one stage has registered " + key + "/" + proc + ". first handler=" + handler + " new handler=" + (new Handler(req.appId, req.sink, (Thunk1<RpcCall>) null)));
                    responses.put(key, RpcRegisterResp.SUCCESS);
                }
            }
        }
        RpcRegisterResp resp = new RpcRegisterResp(responses, req.client, null, req.appId, req.userData);
        applicationEnqueue(req, resp, 0L);
        return;
    }

    private void handleRpcCall(final RpcCall req) {
        if ((req.sink == null) && (req.cb == null)) {
            logger.warn("handleRpcCall: Dropping request " + req + ".  No way to respond.");
            return;
        }
        RpcServerStage server_stub = RpcServerStage.getInstance(req.peer);
        if (server_stub == null) {
            MyConnState conn = (MyConnState) _connections.get(req.peer);
            if (conn == null) {
                conn = new MyConnState(req.peer);
                _connections.put(req.peer, conn);
            }
            conn.send_req(req);
        } else {
            send_local_rpc(server_stub, req);
        }
    }

    /**********************************************************************/
    private void send_local_rpc(RpcServerStage server_stub, final RpcCall req) {
        assert server_stub != null : "server stub is null for " + req;
        if (logger.isDebugEnabled()) logger.debug("local gateway nodeid " + req.peer);
        final Long start_time_us = now_us();
        Handler handler = server_stub.getHandler(req.proc);
        assert handler != null : "handler null for proc " + req.proc + " of " + req;
        int xact_id = next_xact_id++;
        req.xact_id = xact_id;
        if (_inflight == null) _inflight = new HashMap<Integer, RpcCall>();
        _inflight.put(xact_id, req);
        if (_cbuf == null) _cbuf = new CountBuffer();
        _cbuf.reset();
        XdrUtils.serialize(_cbuf, req.args);
        if (_bbuf == null || (_bbuf.limit() < _cbuf.size() + ByteUtils.SIZE_INT)) _bbuf = ByteBuffer.allocate(_cbuf.size() + ByteUtils.SIZE_INT);
        _bbuf.clear();
        XdrAble clone_args = null;
        try {
            clone_args = XdrClone.xdr_clone(req.args, _bbuf);
        } catch (Exception e) {
            send_error_reply("(1) error " + e, start_time_us, ((now_us() - start_time_us) / 1000.0), req);
        }
        int size = _bbuf.position();
        long lat = (!sim_running ? 0L : Network.msg_latency_us(my_node_id, req.peer, size) / 1000L);
        final Double waiting_time_ms = ((now_us() - start_time_us) / 1000.0);
        final Thread thread = Thread.currentThread();
        final Object token = _acore.register_timer(REQ_OUTSTANDING_FACTOR * CONN_TIMEOUT, curry(local_rpc_timeout_cb, start_time_us, waiting_time_ms, req));
        final RpcCall outb = new RpcCall(my_node_id, req.proc, clone_args, req.appId, null, (SinkIF) null);
        outb.xact_id = xact_id;
        outb.inbound = true;
        outb.server_stub_cb = new Thunk1<RpcReply>() {

            public void run(RpcReply resp) {
                if (thread != Thread.currentThread()) {
                    _acore.register_timer(0L, curry(local_rpc_done_cb, start_time_us, waiting_time_ms, token, req, resp));
                    return;
                }
                local_rpc_done_cb.run(start_time_us, waiting_time_ms, token, req, resp);
            }
        };
        log_req(false, req, waiting_time_ms, size);
        handlerEnqueue(handler, outb, lat);
    }

    private Thunk5<Long, Double, Object, RpcCall, RpcReply> local_rpc_done_cb = new Thunk5<Long, Double, Object, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Double waiting_time_ms, Object token, RpcCall req, RpcReply reply) {
            if (logger.isDebugEnabled()) logger.debug("op_local_rpc_done from " + req.peer);
            try {
                _acore.cancel_timer(token);
            } catch (Throwable e) {
            }
            RpcCall req2 = _inflight.remove(req.xact_id);
            if (req2 != null) {
                assert req2 == req : "req=" + req + " != req2=" + req2;
                _cbuf.reset();
                XdrUtils.serialize(_cbuf, reply.reply);
                if (_bbuf == null || (_bbuf.limit() < _cbuf.size() + ByteUtils.SIZE_INT)) _bbuf = ByteBuffer.allocate(_cbuf.size() + ByteUtils.SIZE_INT);
                _bbuf.clear();
                XdrAble clone_reply = XdrUtils.clone(reply.reply);
                try {
                    clone_reply = XdrClone.xdr_clone(reply.reply, _bbuf);
                } catch (Exception e) {
                    send_error_reply("(2) error " + e, start_time_us, waiting_time_ms, req);
                }
                int size = _bbuf.position();
                long lat = (!sim_running ? 0L : Network.msg_latency_us(req.peer, my_node_id, size) / 1000L);
                RpcReply resp = new RpcReply(clone_reply, req);
                resp.inbound = true;
                resp.xact_id = req.xact_id;
                resp.reply_stat = RpcReply.ReplyStat.MSG_ACCEPTED;
                resp.msg_accepted = RpcReply.AcceptStat.SUCCESS;
                resp.msg_rejected = null;
                resp.auth_accepted = RpcReply.AuthStat.AUTH_OK;
                log_resp(false, start_time_us, 0.0, req, resp, size);
                applicationEnqueue(req, resp, lat);
            } else {
                logger.warn("handling stage sent response for non-existent" + " xact_id 0x" + Integer.toHexString(req.xact_id) + " to send to local client.  Dropping resp");
            }
        }
    };

    private Thunk3<Long, Double, RpcCall> local_rpc_timeout_cb = new Thunk3<Long, Double, RpcCall>() {

        public void run(Long start_time_us, Double waiting_time_ms, RpcCall req) {
            RpcCall req2 = _inflight.remove(req.xact_id);
            assert req2 == req : "local_rpc_timeout_cb: req=" + req + " != req2=" + req2;
            if (logger.isDebugEnabled()) logger.debug("local_rpc_timeout " + req + ". There are " + (_inflight.size() - 1) + " local reqs infligh.");
            send_error_reply("timeout", start_time_us, waiting_time_ms, req);
        }
    };

    private void send_error_reply(String msg, Long start_time_us, Double waiting_time_ms, RpcCall req) {
        if (true) {
            StringBuffer buf = new StringBuffer(45 + msg.length());
            buf.append("error sending req to " + req.peer);
            buf.append(": ");
            buf.append(msg);
            buf.append(" xact_id=");
            buf.append(Integer.toHexString(req.xact_id));
            logger.fatal(buf);
        }
        RpcReply resp = new RpcReply(XdrVoid.XDR_VOID, req);
        resp.inbound = true;
        resp.reply_stat = RpcReply.ReplyStat.MSG_DENIED;
        log_resp(true, start_time_us, waiting_time_ms, req, resp, 0L);
        applicationEnqueue(req, resp, 0L);
    }

    /**
   * <code>log_req</code> logs the {@link antiquity.rpc.api.RpcCall}. */
    private void log_req(boolean failure, RpcCall req, Double waiting_time_ms, long size) {
        if (logger.isInfoEnabled()) {
            String call = XdrUtils.toString(req.args);
            StringBuffer buf = new StringBuffer(200 + call.length());
            buf.append("LOG");
            if (failure) buf.append(" FAILURE");
            buf.append(" req client=");
            buf.append(req.peer.address().getHostAddress());
            buf.append(":");
            buf.append(req.peer.port());
            buf.append(" xact_id=0x");
            buf.append(Integer.toHexString(req.xact_id));
            buf.append(" caused_by_xact_id=0x");
            buf.append(Integer.toHexString(req.caused_by_xact_id));
            buf.append(" size=");
            buf.append(size);
            buf.append(" call=");
            buf.append(call);
            buf.append(". queue time=");
            buf.append(waiting_time_ms);
            buf.append(" ms");
            logger.info(buf);
        }
    }

    /**
   * Log the {@link antiquity.rpc.api.RpcReply}. 
   * @param xact_id  Transaction id.
   * @param req      {@link antiquity.rpc.api.RpcCall}
   * @param resp     {@link antiquity.rpc.api.RpcReply}
   * @param client   {@link ostore.util.NodeId} of caller.
   * @param start_us Start time of RPC call (in us). */
    public void log_resp(boolean failure, Long start_time_us, Double waiting_time_ms, RpcCall req, RpcReply resp, long size) {
        if (logger.isInfoEnabled()) {
            String call = XdrUtils.toString(req.args);
            String rv = XdrUtils.toString(resp.reply);
            StringBuffer buf = new StringBuffer(200 + call.length() + rv.length());
            buf.append("LOG");
            if (failure) buf.append(" FAILURE");
            buf.append(" resp client=");
            buf.append(resp.peer.address().getHostAddress());
            buf.append(":");
            buf.append(resp.peer.port());
            buf.append(" xact_id=0x");
            buf.append(Integer.toHexString(resp.xact_id));
            buf.append(" caused_by_xact_id=0x");
            buf.append(Integer.toHexString(req.caused_by_xact_id));
            buf.append(" size=");
            buf.append(size);
            buf.append(" call=");
            buf.append(call);
            buf.append(" resp=");
            buf.append(rv);
            buf.append(". queue time=");
            buf.append(waiting_time_ms);
            buf.append(" ms");
            buf.append(", lat=");
            buf.append((now_us() - start_time_us) / 1000.0);
            buf.append(" ms");
            logger.info(buf);
        }
    }

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
    private Thunk2<QueueElementIF, SinkIF> main_loop_enqueue = new Thunk2<QueueElementIF, SinkIF>() {

        /** Call {@link seda.sandStorm.api.SinkIF#enqueue} from the main loop*/
        public void run(QueueElementIF item, SinkIF sink) {
            try {
                sink.enqueue(item);
            } catch (SinkException e) {
                BUG("couldn't enqueue " + e);
            }
            return;
        }
    };

    /** method to send event from rpc thread to app via main thread */
    private <T1 extends RpcReq<T1, T2>, T2 extends RpcResp> void applicationEnqueue(T1 req, T2 resp, long delay) {
        if (req.cb != null) {
            if (logger.isDebugEnabled()) logger.debug("executing callback for " + resp);
            _acoreMain.register_timer(delay, curry(req.cb, req, resp));
        } else if (req.sink != null) {
            if (logger.isDebugEnabled()) logger.debug("enqueuing " + resp + " to " + req.sink);
            _acoreMain.register_timer(delay, curry(main_loop_enqueue, resp, req.sink));
        } else {
            BUG("either cb or sink should be defined for " + req);
        }
        return;
    }

    /** method to send event from rpc thread to event handler via main thread */
    private void handlerEnqueue(Handler handler, RpcCall req, long delay) {
        if (handler.cb != null) {
            if (logger.isDebugEnabled()) logger.debug("invoking handler for " + req);
            _acoreMain.register_timer(delay, curry(handler.cb, req));
        } else if (handler.sink != null) {
            if (logger.isDebugEnabled()) logger.debug("enqueuing " + req + " to handler sink " + handler.sink);
            _acoreMain.register_timer(delay, curry(main_loop_enqueue, req, handler.sink));
        } else {
            BUG("either cb or sink should be defined for handler " + handler + " req=" + req);
        }
    }

    /**********************************************************************/
    private class MyConnState {

        private SocketChannel sc;

        private NodeId gnid;

        private LinkedList<ValuePair<Long, RpcCall>> waiting;

        private Map<Integer, ValueTriple<Long, Double, RpcCall>> inflight;

        private NioMultiplePacketInputBuffer ib;

        private NioMultiplePacketOutputBuffer ob;

        private ConnectionStream is;

        private long last_activity_us;

        private ByteBuffer[] write_bufs;

        private long write_bufs_position, write_bufs_limit;

        private int write_bufs_length, write_xact_id;

        private boolean connection_open;

        private Object timer_token;

        /** 
     * Constructor: Creates a new <code>MyConnState</code>.
     * @param sc {@link java.nio.channels.ServerSocketChannel}. */
        public MyConnState(NodeId gnid) {
            this.gnid = gnid;
            connection_open = true;
            if (logger.isInfoEnabled()) logger.info("Trying to connect to gateway " + gnid);
            try {
                sc = SocketChannel.open();
                sc.configureBlocking(false);
                sc.connect(gnid);
                _acore.register_selectable(sc, OP_CONNECT, connect_cb);
            } catch (IOException e) {
                BUG(e);
            } catch (java.nio.channels.CancelledKeyException cke) {
                BUG(cke);
            }
            waiting = new LinkedList<ValuePair<Long, RpcCall>>();
            inflight = new HashMap<Integer, ValueTriple<Long, Double, RpcCall>>();
            ib = new NioMultiplePacketInputBuffer();
            ob = new NioMultiplePacketOutputBuffer(FRAGMENT_SIZE, DIRECT, ByteUtils.SIZE_INT, _reuse);
            is = new ConnectionStream(logger.getLevel());
            write_bufs_position = write_bufs_limit = 0L;
            write_bufs_length = 0;
            write_xact_id = -1;
            last_activity_us = now_us();
            timer_token = _acore.register_timer(CONN_TIMEOUT, timeout_cb);
        }

        /** 
     * Timeout callback. Closes connection if time since last
     * activitity is longer than {@link #CONN_TIMEOUT}. */
        private Runnable timeout_cb = new Runnable() {

            /** 
       * Closes connection if time since last activitity is longer
       * than {@link #CONN_TIMEOUT}. */
            public void run() {
                assert connection_open : "timeout_cb " + gnid + ": connection already closed";
                if (sc.isOpen()) {
                    long now_us = now_us();
                    if ((inflight.isEmpty() && (now_us - last_activity_us) > (CONN_TIMEOUT * 1000L)) || (!inflight.isEmpty() && (now_us - last_activity_us) > (REQ_OUTSTANDING_FACTOR * CONN_TIMEOUT * 1000L))) error_close_connection("timeout. " + inflight.size() + " pending reqs" + " and " + waiting.size() + " reqs waiting" + waiting.size() + " to be sent."); else timer_token = _acore.register_timer(CONN_TIMEOUT, timeout_cb);
                }
            }
        };

        private Runnable connect_cb = new Runnable() {

            public void run() {
                assert connection_open : "connec_cb " + gnid + ": connection already closed";
                if (logger.isInfoEnabled()) logger.info("op_connect " + gnid);
                try {
                    if (!sc.isOpen()) {
                        error_close_connection("channel to " + gnid + " no longer open");
                    } else if (sc.finishConnect()) {
                        if (waiting.isEmpty() || inflight.size() > 0) {
                            logger.warn("connect_cb for " + gnid + ": waiting list is empty, inflight size is " + inflight.size() + " " + (inflight.size() > 0 ? "inflight msg's will be resent since inflight.size > 0" : "") + ". Something went wrong with the connection." + (write_xact_id != -1 ? " currently sending write_xact_id 0x" + Integer.toHexString(write_xact_id) : ""));
                            return;
                        }
                        try {
                            _acore.unregister_selectable(sc, OP_CONNECT);
                        } catch (ClosedChannelException e) {
                            conn_closed();
                        } catch (java.nio.channels.CancelledKeyException cke) {
                            BUG(cke);
                        }
                        try {
                            _acore.register_selectable(sc, OP_WRITE, write_cb);
                        } catch (ClosedChannelException e) {
                            conn_closed();
                        } catch (java.nio.channels.CancelledKeyException cke) {
                            BUG(cke);
                        }
                    }
                } catch (Exception e) {
                    error_close_connection("connection not initiated to " + gnid + ". " + e);
                    return;
                }
                last_activity_us = now_us();
            }
        };

        private Runnable write_cb = new Runnable() {

            public void run() {
                assert connection_open : "write_cb " + gnid + ": connection already closed";
                if (logger.isDebugEnabled()) logger.debug("op_write to " + gnid + (write_xact_id == -1 ? "" : ", write_xact_id 0x" + Integer.toHexString(write_xact_id)));
                while (true) {
                    if (write_bufs != null && (write_bufs_position < write_bufs_limit)) {
                        if (logger.isDebugEnabled()) {
                            String str = "";
                            for (ByteBuffer write_buf : write_bufs) {
                                if (write_buf != null) {
                                    str += "fragment:\n" + (write_buf.hasArray() ? ByteUtils.print_bytes(write_buf.array(), write_buf.arrayOffset() + write_buf.position(), write_buf.limit() - write_buf.position()) : "no backing array to show bytes");
                                }
                            }
                            if (logger.isDebugEnabled()) logger.debug("sending " + write_bufs_length + " fragments" + " to " + gnid + " , write_xact_id " + Integer.toHexString(write_xact_id) + ":\n" + str);
                        }
                        long n = 0;
                        try {
                            n = sc.write(write_bufs, 0, write_bufs_length);
                        } catch (IOException e) {
                            error_close_connection("error while writing to " + gnid + " write_xact_id 0x" + Integer.toHexString(write_xact_id) + ". " + e);
                            return;
                        }
                        write_bufs_position += n;
                        if (logger.isDebugEnabled()) logger.debug("sent " + n + " bytes; " + (write_bufs_limit - write_bufs_position) + " bytes remaining to send to " + gnid + ", write_xact_id 0x" + Integer.toHexString(write_xact_id));
                        if (write_bufs_position < write_bufs_limit) break;
                        ValueTriple<Long, Double, RpcCall> triple = inflight.get(write_xact_id);
                        RpcCall rpc_call_sent = triple.getThird();
                        if (rpc_call_sent.send_cb != null) {
                            Runnable cb = curry(rpc_call_sent.send_cb, rpc_call_sent);
                            _acoreMain.register_timer(0L, cb);
                        }
                    }
                    for (int i = 0; i < write_bufs_length; i++) {
                        if (_reuse.size() < MAX_FRAGS_CACHE) _reuse.addFirst(write_bufs[i]);
                        write_bufs[i] = null;
                    }
                    write_bufs_position = write_bufs_limit = 0L;
                    write_bufs_length = 0;
                    write_xact_id = -1;
                    if (waiting.isEmpty()) {
                        try {
                            _acore.unregister_selectable(sc, OP_WRITE);
                        } catch (ClosedChannelException e) {
                            conn_closed();
                        } catch (java.nio.channels.CancelledKeyException cke) {
                            BUG(cke);
                        }
                        break;
                    }
                    ValuePair<Long, RpcCall> pair = waiting.removeFirst();
                    Long waiting_start_time_us = pair.getFirst();
                    RpcCall req = pair.getSecond();
                    int xact_id = next_xact_id++;
                    ob.add(xact_id);
                    ob.add(0);
                    ob.add(2);
                    ob.add(req.proc.getProgNum());
                    ob.add(req.proc.getProgVer());
                    ob.add(req.proc.getProcNum());
                    for (int i = 0; i < 2; ++i) {
                        ob.add(0);
                        ob.add(0);
                    }
                    XdrOutputBufferEncodingStream es = new XdrOutputBufferEncodingStream(ob);
                    try {
                        req.args.xdrEncode(es);
                    } catch (Exception e) {
                        error_close_connection("error serializing rpc" + " write_xact_id 0x" + Integer.toHexString(write_xact_id) + " call=" + req + ". error=" + e + ", write_bufs=" + ob);
                        return;
                    }
                    LinkedList<ByteBuffer> list = ob.getList();
                    int size = ob.size();
                    ob.flip();
                    if (write_bufs == null || write_bufs.length < list.size()) write_bufs = new ByteBuffer[list.size()];
                    list.toArray(write_bufs);
                    write_bufs_length = list.size();
                    write_bufs_position = 0;
                    write_bufs_limit = 0;
                    write_xact_id = xact_id;
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
                            logger.debug("write_cb to " + gnid + " for xact_id 0x" + Integer.toHexString(xact_id) + ": frag " + i + " hash: 0x" + ByteUtils.print_bytes(digest, 0, 4));
                        }
                    }
                    if (logger.isDebugEnabled()) logger.debug("For xact_id 0x" + Integer.toHexString(xact_id) + " to " + gnid + ", total serialized size " + size + ", total write bufs size=" + write_bufs_limit + ", in " + write_bufs_length + " packets.");
                    if (inflight.size() == 0) {
                        try {
                            _acore.register_selectable(sc, OP_READ, read_cb);
                        } catch (ClosedChannelException e) {
                            conn_closed();
                        } catch (java.nio.channels.CancelledKeyException cke) {
                            BUG(cke);
                        }
                    }
                    ValueTriple<Long, Double, RpcCall> triple = new ValueTriple<Long, Double, RpcCall>(now_us(), ((now_us() - waiting_start_time_us) / 1000.0), req);
                    req.xact_id = xact_id;
                    log_req(false, req, triple.getSecond(), write_bufs_limit);
                    inflight.put(xact_id, triple);
                }
                last_activity_us = now_us();
            }
        };

        private Runnable read_cb = new Runnable() {

            public void run() {
                assert connection_open : "read_cb " + gnid + ": connection already closed";
                if (logger.isDebugEnabled()) logger.debug("op_read from " + gnid);
                while (true) {
                    ByteBuffer read_buf = null;
                    if (_reuse.isEmpty()) read_buf = (DIRECT ? ByteBuffer.allocateDirect(FRAGMENT_SIZE) : ByteBuffer.allocate(FRAGMENT_SIZE)); else {
                        read_buf = _reuse.removeFirst();
                        read_buf.clear();
                    }
                    int count = 0;
                    try {
                        count = sc.read(read_buf);
                    } catch (IOException e) {
                        error_close_connection("problem reading from " + gnid + ". " + e);
                        return;
                    }
                    if (count < 0) {
                        error_close_connection("read from " + gnid + " returned " + count + " bytes. " + inflight.size() + " inflight pending reqs" + " and " + waiting.size() + " reqs waiting" + " to be sent.");
                        return;
                    }
                    if (count > 0) {
                        if (logger.isDebugEnabled()) logger.debug("read from " + gnid + " packet of " + count + " bytes");
                        read_buf.flip();
                        if (logger.isDebugEnabled()) {
                            logger.debug("received from " + gnid + ":\n" + (read_buf.hasArray() ? ByteUtils.print_bytes(read_buf.array(), read_buf.arrayOffset() + read_buf.position(), read_buf.limit() - read_buf.position()) : "no backing array to show bytes"));
                            ;
                        }
                        is.add_packet(read_buf);
                    }
                    if (!is.message_available()) break;
                    int inbound_size = is.next_message_size();
                    is.get_next_message_buffer(ib);
                    assert ib.size() == inbound_size : "ib.size=" + ib.size() + " != inbound_size=" + inbound_size;
                    ib.limit(inbound_size);
                    int xact_id = ib.nextInt();
                    if (logger.isDebugEnabled()) logger.debug("resp from " + gnid + " xact_id 0x" + Integer.toHexString(xact_id) + " is " + inbound_size + " bytes");
                    if (logger.isDebugEnabled()) logger.debug("decoding resp from " + gnid + " xact_id=0x" + Integer.toHexString(xact_id));
                    ValueTriple<Long, Double, RpcCall> triple = inflight.remove(xact_id);
                    if (triple == null) {
                        error_close_connection("req is null for xact_id=0x" + Integer.toHexString(xact_id));
                        return;
                    }
                    Long start_time_us = triple.getFirst();
                    Double waiting_time_ms = triple.getSecond();
                    RpcCall req = triple.getThird();
                    ProcValue procedure = (ProcValue) _procedures.get(req.proc);
                    if (procedure == null) {
                        error_close_connection("procedure is null for " + req.proc + " req " + req);
                        return;
                    }
                    Constructor constructor = null;
                    RpcReply resp = new RpcReply(null, req);
                    resp.inbound = true;
                    resp.xact_id = xact_id;
                    int msg_type = ib.nextInt();
                    if (msg_type != 1) {
                        error_close_connection("msg_type=" + msg_type + ", instead of REPLY=1. " + " xact_id 0x" + Integer.toHexString(xact_id));
                        return;
                    }
                    int reply_stat = ib.nextInt();
                    switch(reply_stat) {
                        case 0:
                            resp.reply_stat = RpcReply.ReplyStat.MSG_ACCEPTED;
                            break;
                        case 1:
                            resp.reply_stat = RpcReply.ReplyStat.MSG_DENIED;
                            break;
                        default:
                            error_close_connection("reply_stat=" + reply_stat + ", instead of MSG_ACCEPTED=1 or " + "MSG_DENIED=1. " + " xact_id 0x" + Integer.toHexString(xact_id));
                            return;
                    }
                    int auth_flavor = ib.nextInt();
                    if (auth_flavor < 0) {
                        error_close_connection("auth_flavor=" + auth_flavor + " < 0" + " xact_id 0x" + Integer.toHexString(xact_id));
                        return;
                    }
                    int auth_len = ib.nextInt();
                    if (auth_len < 0) {
                        error_close_connection("auth_len=" + auth_len + " < 0" + " xact_id 0x" + Integer.toHexString(xact_id));
                        return;
                    }
                    if (auth_len % 4 != 0) {
                        error_close_connection("auth_len%4=" + (auth_len % 4) + " != 0" + " xact_id 0x" + Integer.toHexString(xact_id));
                        return;
                    }
                    byte[] auth_data = (auth_len <= 0 ? null : new byte[auth_len]);
                    for (int i = 0; i < auth_len; i++) auth_data[i] = ib.nextByte();
                    resp.auth = new OpaqueAuth(auth_flavor, auth_data);
                    if (resp.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) {
                        resp.auth_accepted = RpcReply.AuthStat.AUTH_OK;
                        int msg_accepted = ib.nextInt();
                        switch(msg_accepted) {
                            case 0:
                                resp.msg_accepted = RpcReply.AcceptStat.SUCCESS;
                                constructor = procedure.getReplyConstructor();
                                break;
                            case 1:
                                resp.msg_accepted = RpcReply.AcceptStat.PROG_UNAVAIL;
                                constructor = VOID_CONSTRUCTOR;
                                break;
                            case 2:
                                resp.msg_accepted = RpcReply.AcceptStat.PROG_MISMATCH;
                                resp.mismatch_info_low = ib.nextInt();
                                resp.mismatch_info_high = ib.nextInt();
                                break;
                            case 3:
                                resp.msg_accepted = RpcReply.AcceptStat.PROC_UNAVAIL;
                                constructor = VOID_CONSTRUCTOR;
                                break;
                            case 4:
                                resp.msg_accepted = RpcReply.AcceptStat.GARBAGE_ARGS;
                                constructor = VOID_CONSTRUCTOR;
                                break;
                            case 5:
                                resp.msg_accepted = RpcReply.AcceptStat.SYSTEM_ERR;
                                constructor = VOID_CONSTRUCTOR;
                                break;
                            default:
                                error_close_connection("msg_accepted=" + msg_accepted + " is not valid." + " xact_id 0x" + Integer.toHexString(xact_id));
                                return;
                        }
                    } else {
                        int msg_rejected = ib.nextInt();
                        switch(msg_rejected) {
                            case 0:
                                resp.msg_rejected = RpcReply.RejectStat.RPC_MISMATCH;
                                resp.mismatch_info_low = ib.nextInt();
                                resp.mismatch_info_high = ib.nextInt();
                                break;
                            case 1:
                                resp.msg_rejected = RpcReply.RejectStat.AUTH_ERROR;
                                int auth_stat = ib.nextInt();
                                switch(auth_stat) {
                                    case 0:
                                        resp.auth_accepted = RpcReply.AuthStat.AUTH_OK;
                                        break;
                                    default:
                                        error_close_connection("auth_stat=" + auth_stat + " is not valid. " + " xact_id 0x" + Integer.toHexString(xact_id));
                                        return;
                                }
                                break;
                            default:
                                error_close_connection("msg_rejected=" + msg_rejected + " is not valid." + " xact_id 0x" + Integer.toHexString(xact_id));
                                return;
                        }
                    }
                    if (constructor != null) {
                        if (logger.isDebugEnabled()) logger.debug("remaining ib.size=" + ib.size() + " from " + gnid + " xact_id 0x" + Integer.toHexString(xact_id));
                        XdrInputBufferDecodingStream ds = new XdrInputBufferDecodingStream(ib, ib.limit_remaining());
                        try {
                            ds.beginDecoding();
                            resp.reply = (XdrAble) constructor.newInstance(new Object[] {});
                            resp.reply.xdrDecode(ds);
                        } catch (Exception e) {
                            error_close_connection("error deserializing rpc resp" + " xact_id 0x" + Integer.toHexString(xact_id) + " call=" + req + ". error=" + e);
                            return;
                        }
                    }
                    if (ib.limit_remaining() != 0) {
                        error_close_connection("For xact_id=0x" + Integer.toHexString(xact_id) + " ib.remaining=" + ib.limit_remaining());
                        return;
                    }
                    for (ByteBuffer reuse : is.get_reuse_buffers()) {
                        reuse.clear();
                        assert reuse.capacity() == FRAGMENT_SIZE : "wrong buffer=" + reuse;
                        if (_reuse.size() < MAX_FRAGS_CACHE) _reuse.addFirst(reuse);
                    }
                    is.clear_reuse_buffers();
                    log_resp(false, start_time_us, waiting_time_ms, req, resp, inbound_size);
                    applicationEnqueue(req, resp, 0L);
                }
                last_activity_us = now_us();
            }
        };

        private void send_req(RpcCall req) {
            assert connection_open : "send_req " + gnid + ": connection already closed";
            if (sc.isOpen()) {
                waiting.addLast(new ValuePair<Long, RpcCall>(now_us(), req));
                try {
                    _acore.register_selectable(sc, OP_WRITE, write_cb);
                } catch (ClosedChannelException e) {
                    conn_closed();
                } catch (java.nio.channels.CancelledKeyException cke) {
                    BUG(cke);
                }
                last_activity_us = now_us();
            } else {
                waiting.addLast(new ValuePair<Long, RpcCall>(now_us(), req));
                send_rpc_error_reply();
            }
        }

        private void conn_closed() {
            assert _connections.remove(gnid) == this : "gateway " + gnid + " already removed from connections";
            connection_open = false;
            try {
                _acore.unregister_selectable(sc);
            } catch (java.nio.channels.CancelledKeyException cke) {
            }
            try {
                sc.close();
            } catch (IOException e) {
            }
            try {
                _acore.cancel_timer(timer_token);
            } catch (Throwable e) {
            }
            if (logger.isInfoEnabled()) {
                StringBuffer buf = new StringBuffer(45);
                buf.append("connection closed by " + gnid);
                logger.info(buf);
            }
            send_rpc_error_reply();
        }

        private void error_close_connection(String msg) {
            assert _connections.remove(gnid) == this : "gateway " + gnid + " already removed from connections";
            connection_open = false;
            try {
                _acore.unregister_selectable(sc);
            } catch (java.nio.channels.CancelledKeyException cke) {
            }
            try {
                sc.close();
            } catch (IOException e) {
            }
            try {
                _acore.cancel_timer(timer_token);
            } catch (Throwable e) {
            }
            if (true) {
                StringBuffer buf = new StringBuffer(45 + msg.length());
                buf.append("closing connection to " + gnid);
                buf.append(": ");
                buf.append(msg);
                logger.fatal(buf);
            }
            send_rpc_error_reply();
        }

        private void send_rpc_error_reply() {
            for (ValuePair<Long, RpcCall> pair : waiting) {
                Long waiting_start_time_us = pair.getFirst();
                RpcCall req = pair.getSecond();
                RpcReply resp = new RpcReply(XdrVoid.XDR_VOID, req);
                resp.inbound = true;
                resp.reply_stat = RpcReply.ReplyStat.MSG_DENIED;
                Double waiting_time_ms = ((now_us() - waiting_start_time_us) / 1000.0);
                log_req(true, req, waiting_time_ms, write_bufs_limit);
                applicationEnqueue(req, resp, 0L);
            }
            for (Map.Entry<Integer, ValueTriple<Long, Double, RpcCall>> entry : inflight.entrySet()) {
                Integer xact_id = entry.getKey();
                ValueTriple<Long, Double, RpcCall> triple = entry.getValue();
                Long start_time_us = triple.getFirst();
                Double waiting_time_ms = triple.getSecond();
                RpcCall req = triple.getThird();
                RpcReply resp = new RpcReply(XdrVoid.XDR_VOID, req);
                resp.inbound = true;
                resp.xact_id = xact_id;
                resp.reply_stat = RpcReply.ReplyStat.MSG_DENIED;
                log_resp(true, start_time_us, waiting_time_ms, req, resp, 0L);
                applicationEnqueue(req, resp, 0L);
            }
            waiting.clear();
            inflight.clear();
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
            }
            for (int i = 0; i < 4; i++) System.gc();
        }
    }
}
