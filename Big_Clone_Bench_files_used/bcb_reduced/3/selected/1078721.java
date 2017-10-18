package antiquity.client;

import static antiquity.client.ClientUtils.*;
import antiquity.util.AntiquityUtils;
import static antiquity.util.AntiquityUtils.GW_NULL_GUID;
import antiquity.util.Extent;
import antiquity.util.LruSet;
import antiquity.util.XdrUtils;
import antiquity.gw.api.*;
import antiquity.rpc.api.RpcReq;
import antiquity.rpc.api.RpcCall;
import antiquity.rpc.api.RpcReply;
import antiquity.rpc.api.RpcRegisterReq;
import antiquity.rpc.api.RpcRegisterResp;
import ostore.util.LruMap;
import ostore.util.NodeId;
import ostore.util.SecureHash;
import ostore.util.SHA1Hash;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.api.EventHandlerIF;
import bamboo.lss.DustDevil;
import bamboo.lss.ASyncCore;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import seda.sandStorm.api.ConfigDataIF;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.XdrVoid;
import static bamboo.util.Curry.*;
import static antiquity.rpc.api.ProcInfo.ProcKey;
import static antiquity.rpc.api.ProcInfo.ProcValue;
import static antiquity.util.AntiquityUtils.Procedure;

/**
*** A client library to hide much of the complexity of using the
*** Antiquity API.
***
*** @author  Patrick Eaton
*** @version $Id: ClientLib.java,v 1.2 2007/09/12 19:16:01 hweather Exp $
**/
public class ClientLib {

    private ASyncCore acore;

    private Logger logger;

    private Signature sig_engine;

    private MessageDigest hasher;

    private static long rpc_nonce_cntr = 0;

    private EventHandlerIF rpc_stage;

    private long rpc_app_id;

    private SinkIF rpc_sink;

    private GatewaySelector gw_selector;

    private long timeout_ms = 120000L;

    private Map<Object, Object> timer_tokens;

    private static final int DEFAULT_NUM_RETRIES = 4;

    private int max_num_attempts = DEFAULT_NUM_RETRIES + 1;

    private LruMap<BigInteger, gw_certificate> cert_cache;

    private static final int CERT_CACHE_CAPACITY = 256;

    public ClientLib(EventHandlerIF rpc_stage, long rpc_app_id, SinkIF rpc_sink, Signature sig_engine, MessageDigest hasher) {
        this.rpc_stage = rpc_stage;
        this.rpc_app_id = rpc_app_id;
        this.rpc_sink = rpc_sink;
        this.sig_engine = sig_engine;
        this.hasher = hasher;
        this.gw_selector = new GatewaySelector();
        this.timer_tokens = new HashMap<Object, Object>();
        cert_cache = new LruMap<BigInteger, gw_certificate>(CERT_CACHE_CAPACITY);
        this.logger = Logger.getLogger(getClass().getName());
        this.acore = DustDevil.acore_instance();
        return;
    }

    public static List<NodeId> configParseGateways(ConfigDataIF config) throws java.net.UnknownHostException {
        List<NodeId> gways = new LinkedList<NodeId>();
        int gway_port = 0;
        if (config.contains("GatewayPort")) gway_port = config.getInt("GatewayPort");
        if (config.contains("Gateways")) {
            String gways_st = config.getString("Gateways");
            StringTokenizer tokenizer = new StringTokenizer(gways_st, ",");
            while (tokenizer.hasMoreTokens()) {
                String gway_st = tokenizer.nextToken();
                InetAddress addr = InetAddress.getByName(gway_st);
                gways.add(new NodeId(gway_port, addr));
            }
        }
        if (config.contains("GatewayHosts") && (config.contains("GatewayDomain"))) {
            String gways_st = config.getString("GatewayHosts");
            String gway_domain = config.getString("GatewayDomain");
            StringTokenizer tokenizer = new StringTokenizer(gways_st, ",");
            while (tokenizer.hasMoreTokens()) {
                String host_st = tokenizer.nextToken();
                String gway_st = host_st + "." + gway_domain;
                InetAddress addr = InetAddress.getByName(gway_st);
                gways.add(new NodeId(gway_port, addr));
            }
        }
        return gways;
    }

    public void setDebugLevel(Level debug_level) {
        logger.setLevel(debug_level);
        return;
    }

    public void setResendTimeoutMs(long timeout_ms) {
        this.timeout_ms = timeout_ms;
        return;
    }

    public void setNumRetries(int retries) {
        assert (retries >= 0);
        max_num_attempts = retries + 1;
        return;
    }

    public void addGateways(List<NodeId> gws) {
        Random rnd = new Random(System.currentTimeMillis());
        while (!gws.isEmpty()) {
            int index = rnd.nextInt(gws.size());
            NodeId gw = gws.remove(index);
            addGateway(gw);
        }
        return;
    }

    public void addGateway(NodeId gw) {
        gw_selector.addGateway(gw);
        return;
    }

    public void clearGateways() {
        gw_selector.clearGateways();
        return;
    }

    public void registerRpcCommands(Runnable app_cb) {
        if (logger.isInfoEnabled()) logger.info("Initializing RPC stubs.");
        boolean client = true;
        Object nonce = getNonce();
        RpcRegisterReq rpc_reg_req = new RpcRegisterReq(AntiquityUtils.getGWProcedureMap(), client, rpc_app_id, nonce, rpc_sink);
        rpc_reg_req.cb = curry(register_rpc_commands_done, app_cb);
        Runnable timeout_action = curry(register_rpc_commands_done, app_cb, rpc_reg_req, (RpcRegisterResp) null);
        registerTimeout(rpc_reg_req, timeout_action);
        try {
            rpc_stage.handleEvent(rpc_reg_req);
        } catch (Exception e) {
            logger.fatal(e);
            assert false;
        }
        return;
    }

    protected Thunk3<Runnable, RpcRegisterReq, RpcRegisterResp> register_rpc_commands_done = new Thunk3<Runnable, RpcRegisterReq, RpcRegisterResp>() {

        public void run(Runnable app_cb, RpcRegisterReq rpc_reg_req, RpcRegisterResp rpc_reg_resp) {
            if (rpc_reg_resp == null) {
                logger.fatal("Received timeout while trying to " + "initialize Rpc system.");
                System.exit(1);
            }
            if (!cancelTimeout(rpc_reg_resp.userData)) {
                logger.fatal("Error while trying to initialize Rpc system.");
                System.exit(1);
            }
            for (Map.Entry<ProcKey, Integer> rpc_proc : rpc_reg_resp.responses.entrySet()) if (!RpcRegisterResp.SUCCESS.equals(rpc_proc.getValue())) {
                logger.fatal("Failed to register RPC command: " + rpc_proc + ".");
                System.exit(1);
            }
            if (logger.isInfoEnabled()) logger.info("Done initializing RPC stubs.");
            app_cb.run();
            return;
        }
    };

    public void nullOp(Thunk1<ClientLibResult> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        if (logger.isInfoEnabled()) logger.info("Performing null operation.");
        XdrVoid null_args = XdrVoid.XDR_VOID;
        NodeId gway = null;
        Procedure proc = AntiquityUtils.Procedure.GW_NULL;
        ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
        Object nonce = getNonce();
        RpcCall rpc_call = new RpcCall(gway, proc_key, null_args, rpc_app_id, nonce, rpc_sink);
        Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(null_op_retry_check, app_cb, lib_result);
        Thunk2<RpcCall, RpcReply> done_cb = curry(null_op_done, app_cb, lib_result);
        dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
        return;
    }

    private Thunk5<Thunk1<ClientLibResult>, ClientLibResult, RpcCall, RpcReply, Thunk1<Boolean>> null_op_retry_check = new Thunk5<Thunk1<ClientLibResult>, ClientLibResult, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(Thunk1<ClientLibResult> app_cb, ClientLibResult lib_result, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            boolean retry = false;
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk4<Thunk1<ClientLibResult>, ClientLibResult, RpcCall, RpcReply> null_op_done = new Thunk4<Thunk1<ClientLibResult>, ClientLibResult, RpcCall, RpcReply>() {

        public void run(Thunk1<ClientLibResult> app_cb, ClientLibResult lib_result, RpcCall rpc_call, RpcReply rpc_reply) {
            if (!rpcReplySuccess(rpc_reply)) {
                if (logger.isInfoEnabled()) logger.info("null() failed: rpc_stat=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
            } else {
                if (logger.isInfoEnabled()) logger.info("null() succeeded");
                XdrVoid call_args = (XdrVoid) rpc_call.args;
                XdrVoid reply_result = (XdrVoid) rpc_reply.reply;
                lib_result.setResult(ClientLibResult.OK);
            }
            app_cb.run(lib_result);
            return;
        }
    };

    public void createOp(PublicKey pkey, PrivateKey skey, byte[] cert_user_data, int ttl, Thunk1<ClientLibResult> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        gw_public_key gw_pkey = convertPublicKey(pkey);
        if (logger.isInfoEnabled()) logger.info("Performing create operation: pkey=" + XdrUtils.toString(gw_pkey));
        gw_signed_certificate create_cert = createCert(pkey, cert_user_data, ttl);
        lib_result.markSignStartTime();
        signCertificate(create_cert, skey);
        lib_result.markSignEndTime();
        if (logger.isDebugEnabled()) logger.debug("create_cert=" + XdrUtils.toString(create_cert.cert));
        gw_create_args create_args = new gw_create_args();
        create_args.cert = create_cert;
        NodeId gway = null;
        Procedure proc = AntiquityUtils.Procedure.GW_CREATE;
        ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
        Object nonce = getNonce();
        RpcCall rpc_call = new RpcCall(gway, proc_key, create_args, rpc_app_id, nonce, rpc_sink);
        Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(create_op_retry_check, app_cb, lib_result);
        Thunk2<RpcCall, RpcReply> done_cb = curry(create_op_done, app_cb, lib_result);
        dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
        return;
    }

    private Thunk5<Thunk1<ClientLibResult>, ClientLibResult, RpcCall, RpcReply, Thunk1<Boolean>> create_op_retry_check = new Thunk5<Thunk1<ClientLibResult>, ClientLibResult, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(Thunk1<ClientLibResult> app_cb, ClientLibResult lib_result, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_create_result reply_result = (gw_create_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if (status == gw_status.GW_STATUS_OK) retry = false;
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk4<Thunk1<ClientLibResult>, ClientLibResult, RpcCall, RpcReply> create_op_done = new Thunk4<Thunk1<ClientLibResult>, ClientLibResult, RpcCall, RpcReply>() {

        public void run(Thunk1<ClientLibResult> app_cb, ClientLibResult lib_result, RpcCall rpc_call, RpcReply rpc_reply) {
            gw_create_args call_args = (gw_create_args) rpc_call.args;
            gw_certificate cert = call_args.cert.cert;
            gw_public_key pkey = cert.public_key;
            gw_guid verifier = cert.verifier;
            String msg = "pkey=" + XdrUtils.toString(pkey) + ", verifier=" + guidToString(verifier) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("create() failed: " + msg + ", rpc_stat=" + rpc_reply.reply_stat);
            } else {
                gw_create_result reply_result = (gw_create_result) rpc_reply.reply;
                int status = reply_result.status;
                if (status != gw_status.GW_STATUS_OK) {
                    logger.fatal("create() failed: " + msg + ", status=" + status);
                    lib_result.setResult(ClientLibResult.ANT_FAILED);
                    lib_result.setErrorCode(status);
                } else {
                    gw_guid pkey_hash = pkeyToGuid(pkey);
                    cert_cache.put(guidToBigInt(pkey_hash), cert);
                    lib_result.setResult(ClientLibResult.OK);
                    if (logger.isInfoEnabled()) logger.info("create() succeeded: " + msg + ", proof=" + XdrUtils.toString(reply_result.proof));
                }
            }
            app_cb.run(lib_result);
            return;
        }
    };

    public static class AppendState {

        public PublicKey pkey;

        public PrivateKey skey;

        public gw_data_block[] blocks;

        public byte[] cert_user_data;

        public Thunk1<ClientLibResult> app_cb;

        public ClientLibResult lib_result;

        public gw_certificate prev_cert;
    }

    public void appendOp(PublicKey pkey, PrivateKey skey, gw_data_block[] blocks, byte[] cert_user_data, Thunk1<ClientLibResult> app_cb) {
        assert (blocks.length > 0) : ("Cannot append 0 blocks.");
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        AppendState state = new AppendState();
        state.pkey = pkey;
        state.skey = skey;
        state.blocks = blocks;
        state.cert_user_data = cert_user_data;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        gw_public_key ant_pkey = convertPublicKey(state.pkey);
        gw_guid pkey_hash = pkeyToGuid(ant_pkey);
        gw_certificate prev_cert = cert_cache.get(guidToBigInt(pkey_hash));
        if (prev_cert == null) {
            boolean latest = true;
            boolean verbose = false;
            Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> cb = curry(append_cert_cb, state);
            getCertOp(state.pkey, latest, verbose, cb);
        } else {
            append_cert_cb.run(state, (ClientLibResult) null, prev_cert, (gw_guid[]) null, (gw_soundness_proof) null);
        }
        return;
    }

    private Thunk5<AppendState, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> append_cert_cb = new Thunk5<AppendState, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof>() {

        public void run(AppendState state, ClientLibResult get_cert_lib_result, gw_certificate prev_cert, gw_guid[] get_cert_blocks, gw_soundness_proof proof) {
            ClientLibResult lib_result = state.lib_result;
            state.prev_cert = prev_cert;
            if (state.prev_cert == null) {
                logger.fatal("append() failed: " + "cannot find previous certificate");
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(gw_status.GW_STATUS_ERR_NOT_EXISTS);
                lib_result.markTotalEndTime();
                lib_result.finalize();
                state.app_cb.run(lib_result);
                return;
            }
            gw_public_key ant_pkey = convertPublicKey(state.pkey);
            if (logger.isInfoEnabled()) logger.info("Performing append operation: " + "pkey=" + XdrUtils.toString(ant_pkey) + ", num_blocks=" + state.blocks.length);
            if (logger.isDebugEnabled()) {
                String s = "";
                for (gw_data_block block : state.blocks) {
                    gw_data_block[] b = new gw_data_block[] { block };
                    gw_guid[] bname = AntiquityUtils.computeBlockNames(b, gw_guid.class, hasher);
                    s += guidToString(bname[0]) + "-> length=" + block.value.length + " / ";
                }
                logger.debug("Appending blocks:" + s);
            }
            gw_signed_certificate append_cert = appendCert(state.prev_cert, state.blocks, state.cert_user_data);
            lib_result.markSignStartTime();
            signCertificate(append_cert, state.skey);
            lib_result.markSignEndTime();
            if (logger.isDebugEnabled()) logger.debug("append_cert=" + XdrUtils.toString(append_cert.cert));
            gw_append_args append_args = new gw_append_args();
            append_args.cert = append_cert;
            append_args.data = state.blocks;
            append_args.prev_verifier = state.prev_cert.verifier;
            NodeId gway = null;
            Procedure proc = AntiquityUtils.Procedure.GW_APPEND;
            ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
            Object nonce = getNonce();
            RpcCall rpc_call = new RpcCall(gway, proc_key, append_args, rpc_app_id, nonce, rpc_sink);
            Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(append_op_retry_check, state);
            Thunk2<RpcCall, RpcReply> done_cb = curry(append_op_done, state);
            dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
            return;
        }
    };

    private Thunk4<AppendState, RpcCall, RpcReply, Thunk1<Boolean>> append_op_retry_check = new Thunk4<AppendState, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(AppendState state, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_append_result reply_result = (gw_append_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if ((status == gw_status.GW_STATUS_OK) || (status == gw_status.GW_STATUS_ERR_PRED_FAILED) || (status == gw_status.SS_STATUS_ERR_PRED_FAILED)) {
                retry = false;
            }
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk3<AppendState, RpcCall, RpcReply> append_op_done = new Thunk3<AppendState, RpcCall, RpcReply>() {

        public void run(AppendState state, RpcCall rpc_call, RpcReply rpc_reply) {
            ClientLibResult lib_result = state.lib_result;
            gw_append_args call_args = (gw_append_args) rpc_call.args;
            gw_certificate cert = call_args.cert.cert;
            gw_public_key pkey = cert.public_key;
            gw_guid verifier = cert.verifier;
            String msg = "pkey=" + XdrUtils.toString(pkey) + ", verifier=" + guidToString(verifier) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("append() failed: " + msg + ", rpc_stat=" + rpc_reply.reply_stat);
                state.app_cb.run(lib_result);
                return;
            }
            gw_append_result reply_result = (gw_append_result) rpc_reply.reply;
            int status = reply_result.status;
            if (status == gw_status.GW_STATUS_OK) {
                if (logger.isInfoEnabled()) logger.info("append() succeeded: " + msg + ", proof=" + XdrUtils.toString(reply_result.proof));
                lib_result.setResult(ClientLibResult.OK);
                gw_guid pkey_hash = pkeyToGuid(state.pkey);
                cert_cache.put(guidToBigInt(pkey_hash), cert);
            } else if ((status == gw_status.GW_STATUS_ERR_PRED_FAILED) || (status == gw_status.SS_STATUS_ERR_PRED_FAILED)) {
                if (logger.isInfoEnabled()) logger.info("append() failed due to " + "invalid predicate.  Attempting " + "recovery via renew() and retry: " + msg);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
                long ttl_s = 10 + (cert.expire_time - System.currentTimeMillis()) / 1000;
                Thunk1<ClientLibResult> renew_cb = curry(append_renew_cb, state);
                renewOp(state.pkey, state.skey, ttl_s, renew_cb);
                return;
            } else {
                logger.fatal("append() failed: " + msg + ", status=" + status);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
            }
            state.app_cb.run(lib_result);
            return;
        }
    };

    private Thunk2<AppendState, ClientLibResult> append_renew_cb = new Thunk2<AppendState, ClientLibResult>() {

        public void run(AppendState state, ClientLibResult renew_lib_result) {
            if (renew_lib_result.getResult() == ClientLibResult.OK) {
                if (logger.isInfoEnabled()) logger.info("Recovered from invalid predicate.  " + "Retrying append: pkey=" + printPkey(state.pkey));
                appendOp(state.pkey, state.skey, state.blocks, state.cert_user_data, state.app_cb);
            } else {
                logger.fatal("Failed to recover from an invalid " + "predicate.  Returning failure to app: " + "pkey=" + printPkey(state.pkey));
                ClientLibResult lib_result = state.lib_result;
                lib_result.markTotalEndTime();
                lib_result.finalize();
                state.app_cb.run(lib_result);
            }
            return;
        }
    };

    public static class SnapshotState {

        public PublicKey pkey;

        public PrivateKey skey;

        public byte[] cert_user_data;

        public int ttl_s;

        public Thunk1<ClientLibResult> app_cb;

        public ClientLibResult lib_result;

        public gw_certificate prev_cert;
    }

    public void snapshotOp(PublicKey pkey, PrivateKey skey, byte[] cert_user_data, int ttl_s, Thunk1<ClientLibResult> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        SnapshotState state = new SnapshotState();
        state.pkey = pkey;
        state.skey = skey;
        state.cert_user_data = cert_user_data;
        state.ttl_s = ttl_s;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        gw_public_key ant_pkey = convertPublicKey(state.pkey);
        gw_guid pkey_hash = pkeyToGuid(ant_pkey);
        gw_certificate prev_cert = cert_cache.get(guidToBigInt(pkey_hash));
        if (prev_cert == null) {
            boolean latest = true;
            boolean verbose = false;
            Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> cb = curry(snapshot_cert_cb, state);
            getCertOp(state.pkey, latest, verbose, cb);
        } else {
            snapshot_cert_cb.run(state, (ClientLibResult) null, prev_cert, (gw_guid[]) null, (gw_soundness_proof) null);
        }
        return;
    }

    private Thunk5<SnapshotState, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> snapshot_cert_cb = new Thunk5<SnapshotState, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof>() {

        public void run(SnapshotState state, ClientLibResult get_cert_lib_result, gw_certificate prev_cert, gw_guid[] get_cert_blocks, gw_soundness_proof proof) {
            ClientLibResult lib_result = state.lib_result;
            state.prev_cert = prev_cert;
            if (state.prev_cert == null) {
                logger.fatal("snapshot() failed: " + "cannot find previous certificate");
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(gw_status.GW_STATUS_ERR_NOT_EXISTS);
                lib_result.markTotalEndTime();
                lib_result.finalize();
                state.app_cb.run(lib_result);
                return;
            }
            gw_public_key ant_pkey = convertPublicKey(state.pkey);
            if (logger.isInfoEnabled()) logger.info("Performing snapshot() operation: " + "pkey=" + XdrUtils.toString(ant_pkey));
            gw_signed_certificate snapshot_cert = snapshotCert(state.prev_cert, state.cert_user_data, state.ttl_s);
            lib_result.markSignStartTime();
            signCertificate(snapshot_cert, state.skey);
            lib_result.markSignEndTime();
            if (logger.isDebugEnabled()) logger.debug("snapshot_cert=" + XdrUtils.toString(snapshot_cert.cert));
            gw_snapshot_args snapshot_args = new gw_snapshot_args();
            snapshot_args.cert = snapshot_cert;
            snapshot_args.prev_verifier = state.prev_cert.verifier;
            NodeId gway = null;
            Procedure proc = AntiquityUtils.Procedure.GW_SNAPSHOT;
            ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
            Object nonce = getNonce();
            RpcCall rpc_call = new RpcCall(gway, proc_key, snapshot_args, rpc_app_id, nonce, rpc_sink);
            Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(snapshot_op_retry_check, state);
            Thunk2<RpcCall, RpcReply> done_cb = curry(snapshot_op_done, state);
            dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
            return;
        }
    };

    private Thunk4<SnapshotState, RpcCall, RpcReply, Thunk1<Boolean>> snapshot_op_retry_check = new Thunk4<SnapshotState, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(SnapshotState state, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_snapshot_result reply_result = (gw_snapshot_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if ((status == gw_status.GW_STATUS_OK) || (status == gw_status.GW_STATUS_ERR_ALREADY_EXISTS) || (status == gw_status.GW_STATUS_ERR_PRED_FAILED) || (status == gw_status.SS_STATUS_ERR_PRED_FAILED)) {
                retry = false;
            }
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk3<SnapshotState, RpcCall, RpcReply> snapshot_op_done = new Thunk3<SnapshotState, RpcCall, RpcReply>() {

        public void run(SnapshotState state, RpcCall rpc_call, RpcReply rpc_reply) {
            ClientLibResult lib_result = state.lib_result;
            gw_snapshot_args call_args = (gw_snapshot_args) rpc_call.args;
            gw_certificate cert = call_args.cert.cert;
            gw_public_key pkey = cert.public_key;
            gw_guid verifier = cert.verifier;
            String msg = "pkey=" + XdrUtils.toString(pkey) + ", verifier=" + guidToString(verifier) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("snapshot() failed: " + msg + ", rpc_stat=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
                state.app_cb.run(lib_result);
                return;
            }
            gw_snapshot_result reply_result = (gw_snapshot_result) rpc_reply.reply;
            int status = reply_result.status;
            if (status == gw_status.GW_STATUS_OK) {
                if (logger.isInfoEnabled()) logger.info("snapshot() succeeded: " + msg + ", proof=" + XdrUtils.toString(reply_result.proof));
                lib_result.setResult(ClientLibResult.OK);
                cert_cache.put(guidToBigInt(verifier), cert);
            } else if (status == gw_status.GW_STATUS_ERR_ALREADY_EXISTS) {
                if (logger.isInfoEnabled()) logger.info("snapshot() failed but extent already " + "exists.  Treating as success.");
                lib_result.setResult(ClientLibResult.OK);
            } else if ((status == gw_status.GW_STATUS_ERR_PRED_FAILED) || (status == gw_status.SS_STATUS_ERR_PRED_FAILED)) {
                if (logger.isInfoEnabled()) logger.info("snapshot() failed due to " + "invalid predicate.  Attempting " + "recovery via renew() and retry: " + msg);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
                Thunk1<ClientLibResult> renew_cb = curry(snapshot_renew_cb, state);
                renewOp(state.pkey, state.skey, state.ttl_s, renew_cb);
                return;
            } else {
                logger.fatal("snapshot() failed: " + msg + ", status=" + status);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
            }
            state.app_cb.run(lib_result);
            return;
        }
    };

    private Thunk2<SnapshotState, ClientLibResult> snapshot_renew_cb = new Thunk2<SnapshotState, ClientLibResult>() {

        public void run(SnapshotState state, ClientLibResult renew_lib_result) {
            if (renew_lib_result.getResult() == ClientLibResult.OK) {
                if (logger.isInfoEnabled()) logger.info("Recovered from invalid predicate.  " + "Retrying snapshot: pkey=" + printPkey(state.pkey));
                snapshotOp(state.pkey, state.skey, state.cert_user_data, state.ttl_s, state.app_cb);
            } else {
                logger.fatal("Failed to recover from an invalid " + "predicate.  Returning failure to app: " + "pkey=" + printPkey(state.pkey));
                ClientLibResult lib_result = state.lib_result;
                lib_result.markTotalEndTime();
                lib_result.finalize();
                state.app_cb.run(lib_result);
            }
            return;
        }
    };

    public static class TruncateState {

        public PublicKey pkey;

        public PrivateKey skey;

        public byte[] cert_user_data;

        public Thunk1<ClientLibResult> app_cb;

        public ClientLibResult lib_result;

        public gw_certificate prev_cert;
    }

    public void truncateOp(PublicKey pkey, PrivateKey skey, byte[] cert_user_data, Thunk1<ClientLibResult> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        TruncateState state = new TruncateState();
        state.pkey = pkey;
        state.skey = skey;
        state.cert_user_data = cert_user_data;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        gw_public_key ant_pkey = convertPublicKey(state.pkey);
        gw_guid pkey_hash = pkeyToGuid(ant_pkey);
        gw_certificate prev_cert = cert_cache.get(guidToBigInt(pkey_hash));
        if (prev_cert == null) {
            boolean latest = true;
            boolean verbose = false;
            Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> cb = curry(truncate_cert_cb, state);
            getCertOp(state.pkey, latest, verbose, cb);
        } else {
            truncate_cert_cb.run(state, (ClientLibResult) null, prev_cert, (gw_guid[]) null, (gw_soundness_proof) null);
        }
        return;
    }

    private Thunk5<TruncateState, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> truncate_cert_cb = new Thunk5<TruncateState, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof>() {

        public void run(TruncateState state, ClientLibResult get_cert_lib_result, gw_certificate prev_cert, gw_guid[] get_cert_blocks, gw_soundness_proof proof) {
            ClientLibResult lib_result = state.lib_result;
            state.prev_cert = prev_cert;
            if (state.prev_cert == null) {
                logger.fatal("truncate() failed: " + "cannot find previous certificate");
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(gw_status.GW_STATUS_ERR_NOT_EXISTS);
                lib_result.markTotalEndTime();
                lib_result.finalize();
                state.app_cb.run(lib_result);
                return;
            }
            gw_public_key ant_pkey = convertPublicKey(state.pkey);
            if (logger.isInfoEnabled()) logger.info("Performing truncate operation: pkey=" + XdrUtils.toString(ant_pkey));
            gw_signed_certificate truncate_cert = truncateCert(state.prev_cert, state.cert_user_data);
            lib_result.markSignStartTime();
            signCertificate(truncate_cert, state.skey);
            lib_result.markSignEndTime();
            if (logger.isDebugEnabled()) logger.debug("truncate_cert=" + XdrUtils.toString(truncate_cert.cert));
            gw_truncate_args truncate_args = new gw_truncate_args();
            truncate_args.cert = truncate_cert;
            truncate_args.prev_verifier = state.prev_cert.verifier;
            NodeId gway = null;
            Procedure proc = AntiquityUtils.Procedure.GW_TRUNCATE;
            ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
            Object nonce = getNonce();
            RpcCall rpc_call = new RpcCall(gway, proc_key, truncate_args, rpc_app_id, nonce, rpc_sink);
            Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(truncate_op_retry_check, state);
            Thunk2<RpcCall, RpcReply> done_cb = curry(truncate_op_done, state);
            dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
            return;
        }
    };

    private Thunk4<TruncateState, RpcCall, RpcReply, Thunk1<Boolean>> truncate_op_retry_check = new Thunk4<TruncateState, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(TruncateState state, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_truncate_result reply_result = (gw_truncate_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if ((status == gw_status.GW_STATUS_OK) || (status == gw_status.GW_STATUS_ERR_PRED_FAILED) || (status == gw_status.SS_STATUS_ERR_PRED_FAILED)) {
                retry = false;
            }
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk3<TruncateState, RpcCall, RpcReply> truncate_op_done = new Thunk3<TruncateState, RpcCall, RpcReply>() {

        public void run(TruncateState state, RpcCall rpc_call, RpcReply rpc_reply) {
            ClientLibResult lib_result = state.lib_result;
            gw_truncate_args call_args = (gw_truncate_args) rpc_call.args;
            gw_certificate cert = call_args.cert.cert;
            gw_public_key pkey = cert.public_key;
            gw_guid verifier = cert.verifier;
            String msg = "pkey=" + XdrUtils.toString(pkey) + ", verifier=" + guidToString(verifier) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("truncate() failed: " + msg + ", rpc_stat=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
                state.app_cb.run(lib_result);
                return;
            }
            gw_truncate_result reply_result = (gw_truncate_result) rpc_reply.reply;
            int status = reply_result.status;
            if (status == gw_status.GW_STATUS_OK) {
                if (logger.isInfoEnabled()) logger.info("truncate() succeeded: " + msg + ", proof=" + XdrUtils.toString(reply_result.proof));
                lib_result.setResult(ClientLibResult.OK);
                gw_guid pkey_hash = pkeyToGuid(state.pkey);
                cert_cache.put(guidToBigInt(pkey_hash), cert);
            } else if ((status == gw_status.GW_STATUS_ERR_PRED_FAILED) || (status == gw_status.SS_STATUS_ERR_PRED_FAILED)) {
                if (logger.isInfoEnabled()) logger.info("truncate() failed due to " + "invalid predicate.  Attepting " + "recovery via renew() and retry: " + msg);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
                long ttl_s = 10 + (cert.expire_time - System.currentTimeMillis()) / 1000;
                Thunk1<ClientLibResult> renew_cb = curry(truncate_renew_cb, state);
                renewOp(state.pkey, state.skey, ttl_s, renew_cb);
                return;
            } else {
                logger.fatal("truncate() failed: " + msg + ", status=" + status);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
            }
            state.app_cb.run(lib_result);
            return;
        }
    };

    private Thunk2<TruncateState, ClientLibResult> truncate_renew_cb = new Thunk2<TruncateState, ClientLibResult>() {

        public void run(TruncateState state, ClientLibResult renew_lib_result) {
            if (renew_lib_result.getResult() == ClientLibResult.OK) {
                if (logger.isInfoEnabled()) logger.info("Recovered from invalid predicate.  " + "Retrying truncate: pkey=" + printPkey(state.pkey));
                truncateOp(state.pkey, state.skey, state.cert_user_data, state.app_cb);
            } else {
                logger.fatal("Failed to recover from an invalid " + "predicate.  Returning failure to app: " + "pkey=" + printPkey(state.pkey));
                ClientLibResult lib_result = state.lib_result;
                lib_result.markTotalEndTime();
                lib_result.finalize();
                state.app_cb.run(lib_result);
            }
            return;
        }
    };

    public static class PutState {

        public PublicKey pkey;

        public PrivateKey skey;

        public byte[] cert_user_data;

        public Thunk1<ClientLibResult> app_cb;

        public ClientLibResult lib_result;

        public gw_data_block[] data_blocks;

        public int ttl;
    }

    public void putOp(PublicKey pkey, PrivateKey skey, gw_data_block[] data_blocks, byte[] cert_user_data, int ttl, Thunk1<ClientLibResult> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        PutState state = new PutState();
        state.pkey = pkey;
        state.skey = skey;
        state.cert_user_data = cert_user_data;
        state.ttl = ttl;
        state.data_blocks = data_blocks;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        if (logger.isInfoEnabled()) logger.info("Performing put operation: num_blocks=" + data_blocks.length);
        gw_public_key ant_pkey = convertPublicKey(pkey);
        gw_signed_certificate put_cert = putCert(ant_pkey, data_blocks, cert_user_data, ttl);
        lib_result.markSignStartTime();
        signCertificate(put_cert, skey);
        lib_result.markSignEndTime();
        if (logger.isDebugEnabled()) logger.debug("put_cert=" + XdrUtils.toString(put_cert));
        gw_put_args put_args = new gw_put_args();
        put_args.cert = put_cert;
        put_args.data = data_blocks;
        NodeId gway = null;
        Procedure proc = AntiquityUtils.Procedure.GW_PUT;
        ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
        Object nonce = getNonce();
        RpcCall rpc_call = new RpcCall(gway, proc_key, put_args, rpc_app_id, nonce, rpc_sink);
        Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(put_op_retry_check, state);
        Thunk2<RpcCall, RpcReply> done_cb = curry(put_op_done, state);
        dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
        return;
    }

    private Thunk4<PutState, RpcCall, RpcReply, Thunk1<Boolean>> put_op_retry_check = new Thunk4<PutState, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(PutState state, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_put_result reply_result = (gw_put_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if (status == gw_status.GW_STATUS_OK) retry = false;
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk3<PutState, RpcCall, RpcReply> put_op_done = new Thunk3<PutState, RpcCall, RpcReply>() {

        public void run(PutState state, RpcCall rpc_call, RpcReply rpc_reply) {
            ClientLibResult lib_result = state.lib_result;
            gw_put_args call_args = (gw_put_args) rpc_call.args;
            gw_certificate cert = call_args.cert.cert;
            gw_public_key pkey = cert.public_key;
            gw_guid verifier = cert.verifier;
            String msg = "pkey=" + XdrUtils.toString(pkey) + ", verifier=" + guidToString(verifier) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("put() failed: " + msg + ", rpc_stat=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
                state.app_cb.run(lib_result);
                return;
            }
            gw_put_result reply_result = (gw_put_result) rpc_reply.reply;
            int status = reply_result.status;
            if (status == gw_status.GW_STATUS_OK) {
                if (logger.isInfoEnabled()) logger.info("put() succeeded: " + msg + ", proof=" + XdrUtils.toString(reply_result.proof));
                lib_result.setResult(ClientLibResult.OK);
                cert_cache.put(guidToBigInt(verifier), cert);
            } else {
                logger.fatal("put() failed: " + msg + ", status=" + status);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
            }
            state.app_cb.run(lib_result);
            return;
        }
    };

    public static class RenewState {

        public PublicKey pkey;

        public PrivateKey skey;

        public long ttl_s;

        public gw_guid verifier;

        public boolean key_verified;

        public Thunk1<ClientLibResult> app_cb;

        public ClientLibResult lib_result;

        public gw_certificate prev_cert;
    }

    public void renewOp(PublicKey pkey, PrivateKey skey, long ttl_s, Thunk1<ClientLibResult> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        gw_public_key ant_pkey = convertPublicKey(pkey);
        gw_guid pkey_hash = pkeyToGuid(ant_pkey);
        RenewState state = new RenewState();
        state.pkey = pkey;
        state.skey = skey;
        state.ttl_s = ttl_s;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        state.verifier = pkey_hash;
        state.key_verified = true;
        Thunk2<ClientLibResult, gw_stop_config_result> cb = curry(renew_stopped_cb, state);
        stopConfigOp(pkey, skey, pkey_hash, state.key_verified, cb);
        return;
    }

    public void renewOp(PublicKey pkey, PrivateKey skey, gw_guid verifier, long ttl_s, Thunk1<ClientLibResult> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        RenewState state = new RenewState();
        state.pkey = pkey;
        state.skey = skey;
        state.ttl_s = ttl_s;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        state.verifier = verifier;
        state.key_verified = false;
        gw_public_key ant_pkey = convertPublicKey(pkey);
        gw_guid pkey_hash = pkeyToGuid(ant_pkey);
        if (XdrUtils.equals(pkey_hash, verifier)) state.key_verified = true;
        Thunk2<ClientLibResult, gw_stop_config_result> cb = curry(renew_stopped_cb, state);
        stopConfigOp(pkey, skey, verifier, state.key_verified, cb);
        return;
    }

    private Thunk3<RenewState, ClientLibResult, gw_stop_config_result> renew_stopped_cb = new Thunk3<RenewState, ClientLibResult, gw_stop_config_result>() {

        public void run(RenewState state, ClientLibResult stop_lib_result, gw_stop_config_result stop_result) {
            ClientLibResult lib_result = state.lib_result;
            int stop_status = stop_lib_result.getResult();
            if (stop_status != ClientLibResult.OK) {
                logger.fatal("renew() failed - " + "could not executue stop_config(): " + "pkey=" + printPkey(state.pkey) + " verifier=" + XdrUtils.toString(state.verifier) + " key_verified=" + state.key_verified);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(stop_lib_result.getErrorCode());
                lib_result.markTotalEndTime();
                lib_result.finalize();
                state.app_cb.run(lib_result);
                return;
            }
            if (logger.isInfoEnabled()) logger.info("Performing renew operation: " + "pkey=" + printPkey(state.pkey) + " verifier=" + XdrUtils.toString(state.verifier) + " key_verified=" + state.key_verified);
            gw_signed_certificate renew_cert = renewCert(stop_result.proof[0].cert.cert, state.ttl_s);
            lib_result.markSignStartTime();
            signCertificate(renew_cert, state.skey);
            lib_result.markSignEndTime();
            if (logger.isDebugEnabled()) logger.debug("renew_cert=" + XdrUtils.toString(renew_cert));
            gw_renew_args renew_args = new gw_renew_args();
            renew_args.cert = renew_cert;
            renew_args.sig = stop_result.sig;
            renew_args.sig_proof_hash = stop_result.sig_proof_hash;
            renew_args.latest_config = stop_result.latest_config;
            ;
            renew_args.proof = stop_result.proof;
            NodeId gway = null;
            Procedure proc = AntiquityUtils.Procedure.GW_RENEW;
            ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
            Object nonce = getNonce();
            RpcCall rpc_call = new RpcCall(gway, proc_key, renew_args, rpc_app_id, nonce, rpc_sink);
            Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(renew_op_retry_check, state);
            Thunk2<RpcCall, RpcReply> done_cb = curry(renew_op_done, state);
            dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
            return;
        }
    };

    private Thunk4<RenewState, RpcCall, RpcReply, Thunk1<Boolean>> renew_op_retry_check = new Thunk4<RenewState, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(RenewState state, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_renew_result reply_result = (gw_renew_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if (status == gw_status.GW_STATUS_OK) retry = false;
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk3<RenewState, RpcCall, RpcReply> renew_op_done = new Thunk3<RenewState, RpcCall, RpcReply>() {

        public void run(RenewState state, RpcCall rpc_call, RpcReply rpc_reply) {
            ClientLibResult lib_result = state.lib_result;
            gw_renew_args call_args = (gw_renew_args) rpc_call.args;
            gw_certificate cert = call_args.cert.cert;
            gw_public_key pkey = cert.public_key;
            gw_guid verifier = cert.verifier;
            String msg = "pkey=" + XdrUtils.toString(pkey) + ", verifier=" + guidToString(verifier) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("renew() failed: " + msg + ", rpc_stat=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
                state.app_cb.run(lib_result);
                return;
            }
            gw_renew_result reply_result = (gw_renew_result) rpc_reply.reply;
            int status = reply_result.status;
            if (status == gw_status.GW_STATUS_OK) {
                if (logger.isInfoEnabled()) logger.info("renew() succeeded: " + msg + ", proof=" + XdrUtils.toString(reply_result.proof));
                lib_result.setResult(ClientLibResult.OK);
                gw_guid extent_key = verifier;
                if (state.key_verified) {
                    gw_public_key xdr_pkey = cert.public_key;
                    extent_key = pkeyToGuid(xdr_pkey);
                }
                cert_cache.put(guidToBigInt(extent_key), cert);
            } else {
                logger.fatal("renew() failed: " + msg + ", status=" + status);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
            }
            state.app_cb.run(lib_result);
            return;
        }
    };

    public static final class StopConfigState {

        public PublicKey pkey;

        public PrivateKey skey;

        public gw_guid verifier;

        public boolean key_verified;

        public Thunk2<ClientLibResult, gw_stop_config_result> app_cb;

        public ClientLibResult lib_result;
    }

    private void stopConfigOp(PublicKey pkey, PrivateKey skey, gw_guid verifier, boolean key_verified, Thunk2<ClientLibResult, gw_stop_config_result> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        StopConfigState state = new StopConfigState();
        state.pkey = pkey;
        state.skey = skey;
        state.verifier = verifier;
        state.key_verified = key_verified;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        boolean latest = true;
        boolean verbose = false;
        Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> cb = curry(stop_cert_cb, state);
        if (state.key_verified) getCertOp(pkey, latest, verbose, cb); else getCertOp(verifier, latest, verbose, cb);
        return;
    }

    private Thunk5<StopConfigState, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> stop_cert_cb = new Thunk5<StopConfigState, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof>() {

        public void run(StopConfigState state, ClientLibResult cert_lib_result, gw_certificate cert, gw_guid[] bnames, gw_soundness_proof proof) {
            ClientLibResult lib_result = state.lib_result;
            if ((cert == null) || (proof == null)) {
                logger.fatal("stop_config() failed: " + "cannot find previous certificate");
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(gw_status.GW_STATUS_ERR_NOT_EXISTS);
                lib_result.markTotalEndTime();
                lib_result.finalize();
                state.app_cb.run(lib_result, (gw_stop_config_result) null);
                return;
            }
            if (logger.isInfoEnabled()) logger.info("Performing stop_config() operation: " + "pkey=" + printPkey(state.pkey) + " verifier=" + XdrUtils.toString(state.verifier));
            gw_public_key ant_pkey = convertPublicKey(state.pkey);
            gw_signed_signature signed_config = stopConfig(ant_pkey, proof);
            lib_result.markSignStartTime();
            signData(signed_config, state.skey);
            lib_result.markSignEndTime();
            if (logger.isDebugEnabled()) logger.debug("stop_config=" + XdrUtils.toString(signed_config));
            gw_stop_config_args stop_config_args = new gw_stop_config_args();
            stop_config_args.latest_config = proof.config;
            stop_config_args.sig = signed_config;
            NodeId gway = null;
            Procedure proc = AntiquityUtils.Procedure.GW_STOP_CONFIG;
            ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
            Object nonce = getNonce();
            RpcCall rpc_call = new RpcCall(gway, proc_key, stop_config_args, rpc_app_id, nonce, rpc_sink);
            Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(stop_config_op_retry_check, state);
            Thunk2<RpcCall, RpcReply> done_cb = curry(stop_config_op_done, state);
            dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
            return;
        }
    };

    private Thunk4<StopConfigState, RpcCall, RpcReply, Thunk1<Boolean>> stop_config_op_retry_check = new Thunk4<StopConfigState, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(StopConfigState state, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_stop_config_result reply_result = (gw_stop_config_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if (status == gw_status.GW_STATUS_OK) retry = false;
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk3<StopConfigState, RpcCall, RpcReply> stop_config_op_done = new Thunk3<StopConfigState, RpcCall, RpcReply>() {

        public void run(StopConfigState state, RpcCall rpc_call, RpcReply rpc_reply) {
            ClientLibResult lib_result = state.lib_result;
            gw_stop_config_args call_args = (gw_stop_config_args) rpc_call.args;
            boolean success = false;
            String msg = "pkey=" + printPkey(state.pkey) + ", verifier=" + guidToString(state.verifier) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("stop_config() failed: " + msg + ", rpc_stat=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
                state.app_cb.run(lib_result, (gw_stop_config_result) null);
                return;
            }
            gw_stop_config_result reply_result = (gw_stop_config_result) rpc_reply.reply;
            int status = reply_result.status;
            if (status == gw_status.GW_STATUS_OK) {
                if (logger.isInfoEnabled()) logger.info("stop_config() succeeded: " + msg + ", proof=" + XdrUtils.toString(reply_result.proof));
                lib_result.setResult(ClientLibResult.OK);
            } else {
                logger.fatal("stop_config() failed: " + msg + ", status=" + status);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
            }
            state.app_cb.run(lib_result, reply_result);
            return;
        }
    };

    public static class GetCertState {

        public PublicKey pkey;

        public gw_guid extent_key;

        public boolean latest;

        public boolean verbose;

        public boolean key_verified;

        public Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> app_cb;

        public ClientLibResult lib_result;

        public gw_certificate cert;

        public gw_guid[] bnames;

        public gw_soundness_proof proof;
    }

    /** @Deprecated */
    public void getCertOp(PublicKey pkey, boolean latest, boolean verbose, Thunk3<ClientLibResult, gw_certificate, gw_guid[]> app_cb) {
        Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> cb = curry(get_cert_op_cb, app_cb);
        getCertOp(pkey, latest, verbose, cb);
        return;
    }

    /** @Deprecated */
    public void getCertOp(gw_guid extent_key, boolean latest, boolean verbose, Thunk3<ClientLibResult, gw_certificate, gw_guid[]> app_cb) {
        Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> cb = curry(get_cert_op_cb, app_cb);
        getCertOp(extent_key, latest, verbose, cb);
        return;
    }

    private Thunk5<Thunk3<ClientLibResult, gw_certificate, gw_guid[]>, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> get_cert_op_cb = new Thunk5<Thunk3<ClientLibResult, gw_certificate, gw_guid[]>, ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof>() {

        public void run(Thunk3<ClientLibResult, gw_certificate, gw_guid[]> app_cb, ClientLibResult lib_result, gw_certificate cert, gw_guid[] bnames, gw_soundness_proof proof) {
            app_cb.run(lib_result, cert, bnames);
            return;
        }
    };

    /** @Deprecated */
    public void getCertOp(gw_guid extent_key, boolean latest, boolean verbose, Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        if (logger.isInfoEnabled()) logger.info("Performing hash-verified get_cert() operation: " + "hash_name=" + guidToString(extent_key));
        PublicKey pkey = null;
        boolean key_verified = false;
        _getCertOp(pkey, extent_key, latest, verbose, key_verified, app_cb, lib_result);
        return;
    }

    public void getCertOp(PublicKey pkey, boolean latest, boolean verbose, Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> app_cb) {
        gw_public_key xdr_pkey = convertPublicKey(pkey);
        gw_guid extent_key = pkeyToGuid(xdr_pkey);
        getCertOp(pkey, extent_key, latest, verbose, app_cb);
        return;
    }

    public void getCertOp(PublicKey pkey, gw_guid extent_key, boolean latest, boolean verbose, Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        gw_public_key pkey_xdr = convertPublicKey(pkey);
        gw_guid pkey_hash = pkeyToGuid(pkey_xdr);
        boolean key_verified = pkey_hash.equals(extent_key);
        if (logger.isInfoEnabled()) logger.info("Performing get_cert() operation: " + "pkey=" + printPkey(pkey) + " extent_key=" + guidToString(extent_key) + " key_verified=" + key_verified);
        _getCertOp(pkey, extent_key, latest, verbose, key_verified, app_cb, lib_result);
        return;
    }

    private void _getCertOp(PublicKey pkey, gw_guid extent_key, boolean latest, boolean verbose, boolean key_verified, Thunk4<ClientLibResult, gw_certificate, gw_guid[], gw_soundness_proof> app_cb, ClientLibResult lib_result) {
        if (logger.isInfoEnabled()) logger.info("Performing get_cert() operation: pkey=" + (pkey == null ? "null" : printPkey(pkey)) + " extent_key=" + guidToString(extent_key));
        GetCertState state = new GetCertState();
        state.pkey = pkey;
        state.extent_key = extent_key;
        state.latest = latest;
        state.verbose = verbose;
        state.key_verified = key_verified;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        gw_get_certificate_args get_cert_args = new gw_get_certificate_args();
        get_cert_args.extent_key = extent_key;
        get_cert_args.latest = (byte) (latest ? 0x1 : 0x0);
        get_cert_args.verbose = (byte) (verbose ? 0x1 : 0x0);
        get_cert_args.use_admin = (byte) (false ? 0x1 : 0x0);
        get_cert_args.client_id = (pkey != null ? pkeyToGuid(pkey) : GW_NULL_GUID);
        NodeId gway = null;
        Procedure proc = AntiquityUtils.Procedure.GW_GET_CERT;
        ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
        Object nonce = getNonce();
        RpcCall rpc_call = new RpcCall(gway, proc_key, get_cert_args, rpc_app_id, nonce, rpc_sink);
        Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(get_cert_op_retry_check, state);
        Thunk2<RpcCall, RpcReply> done_cb = curry(get_cert_op_done, state);
        dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
        return;
    }

    private Thunk4<GetCertState, RpcCall, RpcReply, Thunk1<Boolean>> get_cert_op_retry_check = new Thunk4<GetCertState, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(GetCertState state, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_get_certificate_result reply_result = (gw_get_certificate_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if (status == gw_status.GW_STATUS_OK) retry = false;
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk3<GetCertState, RpcCall, RpcReply> get_cert_op_done = new Thunk3<GetCertState, RpcCall, RpcReply>() {

        public void run(GetCertState state, RpcCall rpc_call, RpcReply rpc_reply) {
            ClientLibResult lib_result = state.lib_result;
            gw_get_certificate_args call_args = (gw_get_certificate_args) rpc_call.args;
            gw_certificate cert = null;
            gw_guid[] bnames = null;
            gw_soundness_proof proof = null;
            String msg = "pkey=" + (state.pkey == null ? "null" : printPkey(state.pkey)) + ", extent_key=" + guidToString(state.extent_key) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("get_cert() failed: " + msg + ", rpc_error=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
                state.app_cb.run(lib_result, cert, bnames, proof);
                return;
            }
            gw_get_certificate_result reply_result = (gw_get_certificate_result) rpc_reply.reply;
            int status = reply_result.status;
            if (status == gw_status.GW_STATUS_OK) {
                if (logger.isInfoEnabled()) {
                    if (call_args.latest != 0x0) msg += ", proof=" + XdrUtils.toString(reply_result.proof);
                    logger.info("get_cert() succeeded: " + msg);
                }
                lib_result.setResult(ClientLibResult.OK);
                cert = reply_result.cert.cert;
                bnames = reply_result.block_names;
                proof = reply_result.proof;
                cert_cache.put(guidToBigInt(state.extent_key), cert);
            } else {
                logger.fatal("get_cert() failed: " + msg + ", status=" + status);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
            }
            state.app_cb.run(lib_result, cert, bnames, proof);
            return;
        }
    };

    public static class GetExtentState {

        public gw_guid extent_key;

        public Thunk3<ClientLibResult, gw_certificate, gw_data_block[]> app_cb;

        public ClientLibResult lib_result;

        public gw_certificate cert;

        public gw_data_block[] data;
    }

    public void getExtentOp(gw_guid extent_key, Thunk3<ClientLibResult, gw_certificate, gw_data_block[]> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        GetExtentState state = new GetExtentState();
        state.extent_key = extent_key;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        if (logger.isInfoEnabled()) logger.info("Performing get_extent() operation: " + guidToString(extent_key) + ".");
        gw_get_extent_args get_extent_args = new gw_get_extent_args();
        get_extent_args.extent_key = extent_key;
        get_extent_args.predicate_verifier = AntiquityUtils.GW_NULL_GUID;
        NodeId gway = null;
        Procedure proc = AntiquityUtils.Procedure.GW_GET_EXTENT;
        ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
        Object nonce = getNonce();
        RpcCall rpc_call = new RpcCall(gway, proc_key, get_extent_args, rpc_app_id, nonce, rpc_sink);
        Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(get_extent_op_retry_check, state);
        Thunk2<RpcCall, RpcReply> done_cb = curry(get_extent_op_done, state);
        dispatchRpcCall(rpc_call, lib_result, retry_cb, done_cb);
        return;
    }

    private Thunk4<GetExtentState, RpcCall, RpcReply, Thunk1<Boolean>> get_extent_op_retry_check = new Thunk4<GetExtentState, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(GetExtentState state, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_get_extent_result reply_result = (gw_get_extent_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if (status == gw_status.GW_STATUS_OK) retry = false;
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk3<GetExtentState, RpcCall, RpcReply> get_extent_op_done = new Thunk3<GetExtentState, RpcCall, RpcReply>() {

        public void run(GetExtentState state, RpcCall rpc_call, RpcReply rpc_reply) {
            ClientLibResult lib_result = state.lib_result;
            gw_get_extent_args call_args = (gw_get_extent_args) rpc_call.args;
            gw_certificate cert = null;
            gw_data_block[] data = null;
            String msg = "extent_key=" + guidToString(state.extent_key) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("get_extent() failed: " + msg + ", rpc_error=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
                state.app_cb.run(lib_result, cert, data);
                return;
            }
            gw_get_extent_result reply_result = (gw_get_extent_result) rpc_reply.reply;
            int status = reply_result.status;
            msg = "pkey=" + XdrUtils.toString(cert.public_key) + ", " + msg;
            if (status == gw_status.GW_STATUS_OK) {
                if (logger.isInfoEnabled()) logger.info("get_extent() succeeded: " + msg);
                lib_result.setResult(ClientLibResult.OK);
                cert = cert;
                data = data;
            } else {
                logger.fatal("get_extent() failed: " + msg + ", status=" + status);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
            }
            state.app_cb.run(lib_result, cert, data);
            return;
        }
    };

    public static class GetBlocksState {

        public gw_guid extent_key;

        public gw_guid[] bnames;

        public Thunk2<ClientLibResult, gw_data_block[]> app_cb;

        public ClientLibResult lib_result;

        public gw_data_block[] data;

        public Map<BigInteger, gw_data_block> block_map;
    }

    public void getBlocksOp(gw_guid extent_key, gw_guid[] block_names, Thunk2<ClientLibResult, gw_data_block[]> app_cb) {
        ClientLibResult lib_result = new ClientLibResult();
        lib_result.markTotalStartTime();
        GetBlocksState state = new GetBlocksState();
        state.extent_key = extent_key;
        state.bnames = block_names;
        state.app_cb = app_cb;
        state.lib_result = lib_result;
        if (logger.isInfoEnabled()) {
            String s = "";
            for (gw_guid b : block_names) {
                s += guidToString(b) + "/";
            }
            logger.info("Performing get_blocks() request: " + "extent_key=" + guidToString(extent_key) + ", numblocks=" + block_names.length + ", blocks=" + s);
        }
        state.block_map = new HashMap<BigInteger, gw_data_block>();
        _getBlocksOp(state);
        return;
    }

    private void _getBlocksOp(GetBlocksState state) {
        Set<BigInteger> fetch_set = new HashSet<BigInteger>();
        for (gw_guid bname : state.bnames) {
            BigInteger guid = guidToBigInt(bname);
            if (!state.block_map.containsKey(guid)) {
                fetch_set.add(guid);
            }
        }
        BigInteger[] blocks_to_fetch_bi = new BigInteger[fetch_set.size()];
        blocks_to_fetch_bi = fetch_set.toArray(blocks_to_fetch_bi);
        gw_guid[] blocks_to_fetch = bigIntsToGuids(blocks_to_fetch_bi);
        if (logger.isInfoEnabled()) {
            String s = "";
            for (gw_guid b : blocks_to_fetch) {
                s += guidToString(b) + "/";
            }
            logger.info("Performing get_blocks() request: " + "extent_key=" + guidToString(state.extent_key) + ", numblocks=" + blocks_to_fetch.length + "/" + state.bnames.length + ", blocks=" + s);
        }
        gw_get_blocks_args get_blocks_args = new gw_get_blocks_args();
        get_blocks_args.extent_key = state.extent_key;
        get_blocks_args.block_names = blocks_to_fetch;
        NodeId gway = null;
        Procedure proc = AntiquityUtils.Procedure.GW_GET_BLOCKS;
        ProcKey proc_key = AntiquityUtils.getProcedureKey(proc);
        Object nonce = getNonce();
        RpcCall rpc_call = new RpcCall(gway, proc_key, get_blocks_args, rpc_app_id, nonce, rpc_sink);
        Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb = curry(get_blocks_op_retry_check, state);
        Thunk2<RpcCall, RpcReply> done_cb = curry(get_blocks_op_done, state);
        dispatchRpcCall(rpc_call, state.lib_result, retry_cb, done_cb);
        return;
    }

    private Thunk4<GetBlocksState, RpcCall, RpcReply, Thunk1<Boolean>> get_blocks_op_retry_check = new Thunk4<GetBlocksState, RpcCall, RpcReply, Thunk1<Boolean>>() {

        public void run(GetBlocksState state, RpcCall rpc_call, RpcReply rpc_reply, Thunk1<Boolean> retry_cb) {
            assert (rpcReplySuccess(rpc_reply));
            gw_get_blocks_result reply_result = (gw_get_blocks_result) rpc_reply.reply;
            int status = reply_result.status;
            boolean retry = true;
            if ((status == gw_status.GW_STATUS_OK) || (status == gw_status.SS_STATUS_ERR_PARTIAL_GET)) {
                retry = false;
            }
            retry_cb.run(retry);
            return;
        }
    };

    private Thunk3<GetBlocksState, RpcCall, RpcReply> get_blocks_op_done = new Thunk3<GetBlocksState, RpcCall, RpcReply>() {

        public void run(GetBlocksState state, RpcCall rpc_call, RpcReply rpc_reply) {
            ClientLibResult lib_result = state.lib_result;
            gw_get_blocks_args call_args = (gw_get_blocks_args) rpc_call.args;
            String msg = "extent_key=" + guidToString(state.extent_key) + ", gway=" + rpc_call.peer + ", xact_id=" + Integer.toHexString(rpc_reply.xact_id);
            if (!rpcReplySuccess(rpc_reply)) {
                logger.fatal("get_blocks() failed: " + msg + ", rpc_error=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
                state.app_cb.run(lib_result, state.data);
                return;
            }
            gw_get_blocks_result reply_result = (gw_get_blocks_result) rpc_reply.reply;
            int status = reply_result.status;
            if (status == gw_status.GW_STATUS_OK) {
                lib_result.setResult(ClientLibResult.OK);
                for (gw_data_block b : reply_result.data) {
                    SecureHash hash = new SHA1Hash(b.value);
                    BigInteger guid = hashToBigInt(hash);
                    state.block_map.put(guid, b);
                }
                state.data = new gw_data_block[state.bnames.length];
                for (int i = 0; i < state.bnames.length; ++i) {
                    BigInteger guid = guidToBigInt(state.bnames[i]);
                    state.data[i] = state.block_map.get(guid);
                    assert (state.data[i] != null) : ("Fetch failed.");
                }
                if (logger.isInfoEnabled()) {
                    String s = "";
                    for (gw_guid b : state.bnames) s += guidToString(b) + "/";
                    logger.info("get_blocks() succeeded: " + msg + ", blocks=" + s);
                }
            } else if (status == gw_status.SS_STATUS_ERR_PARTIAL_GET) {
                for (gw_data_block b : reply_result.data) {
                    SecureHash hash = new SHA1Hash(b.value);
                    BigInteger guid = hashToBigInt(hash);
                    state.block_map.put(guid, b);
                }
                if (logger.isInfoEnabled()) logger.info("get_block() " + "partial result: " + msg + ", fetch=" + reply_result.data.length + ", total_fetch=" + state.block_map.size() + ", total_requested=" + state.bnames.length);
                _getBlocksOp(state);
                return;
            } else {
                logger.fatal("get_block() failed: " + msg + ", status=" + status);
                lib_result.setResult(ClientLibResult.ANT_FAILED);
                lib_result.setErrorCode(status);
            }
            state.app_cb.run(lib_result, state.data);
            return;
        }
    };

    /**
    ***  @param retry_cb
    ***      A callback into operation-specific logic to determine if
    ***      the operation needs to be retried.  The
    ***      operation-specific logic returns back to the dispatch
    ***      logic a boolean indicating if retry is required and a
    ***      continuation object which can be passed to the done_cb.
    ***  @param done_cb
    ***      A callback into operation-specific logic when the
    ***      operation has been completed.  The operation may have
    ***      succeeded or failed.  Failure could be due to timeouts,
    ***      faulty requests, or because compensating operations are
    ***      required.  The operation-specific logic can record this
    ***      state in the continuation logic.
    **/
    private void dispatchRpcCall(RpcCall rpc_call, ClientLibResult lib_result, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb, Thunk2<RpcCall, RpcReply> done_cb) {
        lib_result.markNetworkStartTime();
        int attempt = 1;
        dispatch_rpc.run(rpc_call, lib_result, retry_cb, done_cb, attempt);
        return;
    }

    private Thunk5<RpcCall, ClientLibResult, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>>, Thunk2<RpcCall, RpcReply>, Integer> dispatch_rpc_retry = new Thunk5<RpcCall, ClientLibResult, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>>, Thunk2<RpcCall, RpcReply>, Integer>() {

        public void run(RpcCall rpc_call, ClientLibResult lib_result, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb, Thunk2<RpcCall, RpcReply> done_cb, Integer attempt) {
            gw_selector.recordGatewaySlow(rpc_call.peer);
            dispatch_rpc.run(rpc_call, lib_result, retry_cb, done_cb, attempt);
            return;
        }
    };

    private Thunk5<RpcCall, ClientLibResult, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>>, Thunk2<RpcCall, RpcReply>, Integer> dispatch_rpc = new Thunk5<RpcCall, ClientLibResult, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>>, Thunk2<RpcCall, RpcReply>, Integer>() {

        public void run(RpcCall rpc_call, ClientLibResult lib_result, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb, Thunk2<RpcCall, RpcReply> done_cb, Integer attempt) {
            Thunk2<RpcCall, RpcReply> rpc_cb = curry(rpc_call_cb, lib_result, retry_cb, done_cb, attempt);
            rpc_call.cb = rpc_cb;
            rpc_call.peer = gw_selector.getGateway();
            assert (rpc_call.peer != null) : "No more valid gateways.  All gateways " + "have been blacklisted due to failures.";
            if (logger.isDebugEnabled()) logger.debug("Sending RPC call: attempt=" + attempt + " msg_id=" + rpc_call.userData + " payload=" + rpc_call.args.getClass().getName() + " gway=" + rpc_call.peer);
            Runnable timeout_action = curry(dispatch_rpc_retry, rpc_call, lib_result, retry_cb, done_cb, attempt + 1);
            registerTimeout(rpc_call, timeout_action);
            try {
                rpc_stage.handleEvent(rpc_call);
            } catch (Exception e) {
                logger.fatal(e);
                assert false;
            }
            return;
        }
    };

    private Thunk6<ClientLibResult, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>>, Thunk2<RpcCall, RpcReply>, Integer, RpcCall, RpcReply> rpc_call_cb = new Thunk6<ClientLibResult, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>>, Thunk2<RpcCall, RpcReply>, Integer, RpcCall, RpcReply>() {

        public void run(ClientLibResult lib_result, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb, Thunk2<RpcCall, RpcReply> done_cb, Integer attempt, RpcCall rpc_call, RpcReply rpc_reply) {
            if (!cancelTimeout(rpc_reply.userData)) return;
            lib_result.markNetworkEndTime();
            if (logger.isDebugEnabled()) logger.debug("Received RPC reply: attempt=" + attempt + " msg_id=" + rpc_call.userData + " payload=" + rpc_call.args.getClass().getName());
            if (rpcReplySuccess(rpc_reply) || (rpc_reply.xact_id != -1L)) gw_selector.recordGatewaySuccess(rpc_call.peer); else gw_selector.recordGatewayFailure(rpc_call.peer);
            if (rpcReplySuccess(rpc_reply)) {
                Thunk1<Boolean> cb = curry(rpc_call_status_cb, lib_result, retry_cb, done_cb, attempt, rpc_call, rpc_reply);
                retry_cb.run(rpc_call, rpc_reply, cb);
            } else {
                logger.fatal("Message failed due to Rpc error: " + "msg_id=" + rpc_call.userData + " rpc_stat=" + rpc_reply.reply_stat);
                lib_result.setResult(ClientLibResult.RPC_FAILED);
                boolean retry = true;
                rpc_call_status_cb.run(lib_result, retry_cb, done_cb, attempt, rpc_call, rpc_reply, retry);
            }
            return;
        }
    };

    private Thunk7<ClientLibResult, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>>, Thunk2<RpcCall, RpcReply>, Integer, RpcCall, RpcReply, Boolean> rpc_call_status_cb = new Thunk7<ClientLibResult, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>>, Thunk2<RpcCall, RpcReply>, Integer, RpcCall, RpcReply, Boolean>() {

        public void run(ClientLibResult lib_result, Thunk3<RpcCall, RpcReply, Thunk1<Boolean>> retry_cb, Thunk2<RpcCall, RpcReply> done_cb, Integer attempt, RpcCall rpc_call, RpcReply rpc_reply, Boolean retry) {
            lib_result.rpc_calls.addFirst(rpc_call);
            lib_result.rpc_replies.addFirst(rpc_reply);
            lib_result.markTotalEndTime();
            lib_result.finalize();
            if (retry) {
                if ((attempt >= max_num_attempts) || (isPermanentFailure(lib_result))) {
                    if (logger.isDebugEnabled()) logger.debug("Request failed, propagating to " + "app: attempt=" + attempt + " msg_id=" + rpc_call.userData + " payload=" + rpc_call.args.getClass().getName());
                    done_cb.run(rpc_call, rpc_reply);
                } else {
                    if (logger.isDebugEnabled()) logger.debug("Request failed, retrying: " + "attempt=" + attempt + " msg_id=" + rpc_call.userData + " payload=" + rpc_call.args.getClass().getName());
                    int next_attempt = attempt + 1;
                    dispatch_rpc.run(rpc_call, lib_result, retry_cb, done_cb, next_attempt);
                }
            } else {
                done_cb.run(rpc_call, rpc_reply);
            }
            return;
        }
    };

    private Object getNonce() {
        return new Long(rpc_nonce_cntr++);
    }

    private String printTokens() {
        String s = new String();
        for (Object n : timer_tokens.keySet()) s += n + " ";
        return s;
    }

    private void registerTimeout(RpcReq rpc_req, Runnable action) {
        Object nonce = rpc_req.userData;
        if (logger.isDebugEnabled()) logger.debug("Registering timeout for message: msg_id=" + nonce);
        Object cancel_token = acore.register_timer(timeout_ms, curry(timeout_cb, rpc_req, action));
        timer_tokens.put(nonce, cancel_token);
        if (logger.isDebugEnabled()) logger.debug("Token set: " + printTokens());
        return;
    }

    private boolean cancelTimeout(Object nonce) {
        if (logger.isDebugEnabled()) logger.debug("Cancelling timeout for message: msg_id=" + nonce);
        boolean cancelled = true;
        Object cancel_token = timer_tokens.remove(nonce);
        if (cancel_token == null) {
            if (logger.isDebugEnabled()) logger.debug("Cannot find timer token for message: " + "msg_id=" + nonce);
            cancelled = false;
        } else {
            acore.cancel_timer(cancel_token);
        }
        return cancelled;
    }

    private Thunk2<RpcReq, Runnable> timeout_cb = new Thunk2<RpcReq, Runnable>() {

        public void run(RpcReq rpc_req, Runnable action) {
            if (cancelTimeout(rpc_req.userData)) {
                if (logger.isInfoEnabled()) logger.info("Rpc timed out; triggering compensating " + "action: msg_id=" + rpc_req.userData);
                action.run();
            } else {
                logger.fatal("Rpc timed out, but it appears message " + "has already been handled: msg_id=" + rpc_req.userData);
            }
            return;
        }
    };

    private boolean isPermanentFailure(ClientLibResult lib_result) {
        boolean permanent = false;
        if (lib_result.getResult() == ClientLibResult.ANT_FAILED) {
            int err_code = lib_result.getErrorCode();
            if ((err_code == gw_status.GW_STATUS_ERR_INVALID_REQ) || (err_code == gw_status.GW_STATUS_ERR_INVALID_CERT) || (err_code == gw_status.GW_STATUS_ERR_TOO_BIG) || (err_code == gw_status.GW_STATUS_ERR_PRED_FAILED) || (err_code == gw_status.GW_STATUS_ERR_NOT_EXISTS) || (err_code == gw_status.GW_STATUS_ERR_ALREADY_EXISTS) || (err_code == gw_status.SS_STATUS_ERR_ALREADY_EXISTS) || (err_code == gw_status.CR_STATUS_ERR_ALREADY_EXIST) || (err_code == gw_status.GW_STATUS_ERR)) {
                permanent = true;
            }
        }
        return permanent;
    }

    private boolean rpcReplySuccess(RpcReply rpc_reply) {
        boolean success;
        if ((rpc_reply.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (rpc_reply.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (rpc_reply.auth_accepted == RpcReply.AuthStat.AUTH_OK)) {
            success = true;
        } else {
            logger.warn("RPC called failed:" + " reply_stat=" + rpc_reply.reply_stat + " accept_stat=" + rpc_reply.msg_accepted + " msg_rejected=" + rpc_reply.msg_rejected + " auth_accepted=" + rpc_reply.auth_accepted);
            success = false;
        }
        return success;
    }

    private gw_signed_certificate createCert(PublicKey pkey, byte[] cert_user_data, int ttl_s) {
        gw_public_key gw_pkey = convertPublicKey(pkey);
        gw_guid pkey_guid = pkeyToGuid(pkey);
        gw_certificate cert = new gw_certificate();
        cert.type = gw_api.GW_KEY_VERIFIED;
        cert.public_key = gw_pkey;
        cert.verifier = pkey_guid;
        cert.extent_size = 0;
        cert.num_blocks = 0;
        cert.timestamp = System.currentTimeMillis();
        cert.user_data = cert_user_data;
        cert.holes = new byte[0];
        cert.expire_time = cert.timestamp + (long) ttl_s * 1000L;
        cert.seq_num = 0;
        gw_signed_certificate signed_cert = new gw_signed_certificate();
        signed_cert.cert = cert;
        return signed_cert;
    }

    private gw_signed_certificate appendCert(gw_certificate prev_cert, gw_data_block[] blocks, byte[] cert_user_data) {
        gw_guid[] block_names = AntiquityUtils.computeBlockNames(blocks, gw_guid.class, hasher);
        int cum_append_size = 0;
        for (int i = 0; i < blocks.length; ++i) {
            cum_append_size += blocks[i].value.length;
        }
        gw_guid prev_ext_name = prev_cert.verifier;
        gw_guid new_ext_name = AntiquityUtils.computeVerifier(prev_ext_name, block_names, false, hasher);
        gw_certificate new_cert = (gw_certificate) XdrUtils.clone(prev_cert);
        new_cert.timestamp = System.currentTimeMillis();
        new_cert.verifier = new_ext_name;
        new_cert.extent_size += cum_append_size;
        new_cert.num_blocks += blocks.length;
        new_cert.user_data = cert_user_data;
        new_cert.holes = new byte[0];
        new_cert.seq_num++;
        gw_signed_certificate signed_cert = new gw_signed_certificate();
        signed_cert.cert = new_cert;
        return signed_cert;
    }

    private gw_signed_certificate snapshotCert(gw_certificate key_cert, byte[] cert_user_data, int ttl_s) {
        gw_certificate hash_cert = (gw_certificate) XdrUtils.clone(key_cert);
        hash_cert.type = gw_api.GW_HASH_VERIFIED;
        hash_cert.timestamp = System.currentTimeMillis();
        hash_cert.user_data = cert_user_data;
        hash_cert.holes = new byte[0];
        hash_cert.expire_time = hash_cert.timestamp + (long) ttl_s * 1000L;
        hash_cert.seq_num++;
        gw_signed_certificate signed_cert = new gw_signed_certificate();
        signed_cert.cert = hash_cert;
        return signed_cert;
    }

    private gw_signed_certificate truncateCert(gw_certificate prev_cert, byte[] cert_user_data) {
        gw_guid new_ext_name = AntiquityUtils.computeGuid(prev_cert.public_key, gw_guid.class, hasher);
        gw_certificate new_cert = (gw_certificate) XdrUtils.clone(prev_cert);
        new_cert.timestamp = System.currentTimeMillis();
        new_cert.verifier = new_ext_name;
        new_cert.extent_size = 0;
        new_cert.num_blocks = 0;
        new_cert.user_data = cert_user_data;
        new_cert.holes = new byte[0];
        new_cert.seq_num++;
        gw_signed_certificate signed_cert = new gw_signed_certificate();
        signed_cert.cert = new_cert;
        return signed_cert;
    }

    private gw_signed_certificate putCert(gw_public_key pkey, gw_data_block[] blocks, byte[] cert_user_data, int ttl_s) {
        int i = 0;
        int extent_size = 0;
        gw_guid[] block_names = new gw_guid[blocks.length];
        for (gw_data_block b : blocks) {
            extent_size += b.value.length;
            block_names[i++] = hashToGuid(new SHA1Hash(b.value));
        }
        boolean hash_first = true;
        gw_guid verifier = AntiquityUtils.computeVerifier(pkey, block_names, hash_first, hasher);
        gw_certificate cert = new gw_certificate();
        cert.type = gw_api.GW_HASH_VERIFIED;
        cert.public_key = pkey;
        cert.verifier = verifier;
        cert.extent_size = extent_size;
        cert.num_blocks = blocks.length;
        cert.timestamp = System.currentTimeMillis();
        cert.user_data = cert_user_data;
        cert.holes = new byte[0];
        cert.expire_time = cert.timestamp + (long) ttl_s * 1000L;
        cert.seq_num++;
        gw_signed_certificate signed_cert = new gw_signed_certificate();
        signed_cert.cert = cert;
        return signed_cert;
    }

    private gw_signed_certificate renewCert(gw_certificate prev_cert, long ttl_s) {
        gw_certificate new_cert = (gw_certificate) XdrUtils.clone(prev_cert);
        new_cert.seq_num++;
        new_cert.timestamp = System.currentTimeMillis();
        new_cert.expire_time = new_cert.timestamp + ttl_s * 1000L;
        gw_signed_certificate signed_cert = new gw_signed_certificate();
        signed_cert.cert = new_cert;
        return signed_cert;
    }

    private gw_signed_signature stopConfig(gw_public_key pkey, gw_soundness_proof proof) {
        gw_signed_signature signed_config = new gw_signed_signature();
        signed_config.public_key = pkey;
        hasher.update(XdrUtils.serialize(proof.config.config));
        signed_config.datahash = hasher.digest();
        return signed_config;
    }

    private void signCertificate(gw_signed_certificate signed_cert, PrivateKey skey) {
        if (signed_cert.cert.user_data == null) signed_cert.cert.user_data = new byte[0];
        if (signed_cert.cert.holes == null) signed_cert.cert.holes = new byte[0];
        try {
            sig_engine.initSign(skey);
            signed_cert.signature = AntiquityUtils.createSignature(signed_cert.cert, skey, sig_engine);
        } catch (Exception e) {
            logger.fatal("Signature exception: " + e);
        }
        return;
    }

    private void signData(gw_signed_signature data, PrivateKey skey) {
        try {
            sig_engine.initSign(skey);
            data.signature = AntiquityUtils.createSignature(data.datahash, skey, sig_engine);
        } catch (Exception e) {
            logger.fatal("Signature exception: " + e);
        }
        return;
    }

    protected class GatewaySelector {

        private static final double BLACKLIST_RETRY_CHANCE = 0.05;

        private List<NodeId> blacklist;

        private List<NodeId> whitelist;

        private Map<NodeId, Integer> whitelist_in_use;

        private Map<NodeId, Integer> blacklist_in_use;

        private Random rnd;

        public GatewaySelector() {
            long seed = 1;
            rnd = new Random(seed);
            blacklist = new LinkedList<NodeId>();
            blacklist_in_use = new HashMap<NodeId, Integer>();
            whitelist = new LinkedList<NodeId>();
            whitelist_in_use = new HashMap<NodeId, Integer>();
            return;
        }

        public void addGateway(NodeId gway) {
            whitelist.add(gway);
            return;
        }

        public void clearGateways() {
            blacklist.clear();
            blacklist_in_use.clear();
            whitelist.clear();
            whitelist_in_use.clear();
            return;
        }

        /**
        ***  @return
        ***     the <code>NodeId</code> of a gateway.  Can return
        ***     <code>null</code> if no working gateways are known
        **/
        public NodeId getGateway() {
            NodeId gway = null;
            double chance = rnd.nextDouble();
            if ((chance < BLACKLIST_RETRY_CHANCE) && (!blacklist.isEmpty())) {
                gway = blacklist.remove(0);
                blacklist_in_use.put(gway, 1);
            } else if (!whitelist.isEmpty()) {
                gway = whitelist.remove(0);
                whitelist_in_use.put(gway, 1);
            } else if (!whitelist_in_use.isEmpty()) {
                Set<NodeId> gway_set = whitelist_in_use.keySet();
                int pos = rnd.nextInt(gway_set.size());
                Iterator<NodeId> iter = gway_set.iterator();
                do {
                    gway = iter.next();
                } while (pos-- > 0);
                whitelist_in_use.put(gway, whitelist_in_use.get(gway) + 1);
            }
            return gway;
        }

        public void recordGatewayFailure(NodeId gway) {
            if (whitelist_in_use.containsKey(gway)) {
                whitelist_in_use.remove(gway);
                blacklist.add(gway);
            } else if (blacklist_in_use.containsKey(gway)) {
                blacklist_in_use.remove(gway);
                blacklist.add(gway);
            }
            return;
        }

        public void recordGatewaySlow(NodeId gway) {
            if (whitelist_in_use.containsKey(gway)) {
                Integer num_uses = whitelist_in_use.remove(gway);
                assert (num_uses != null);
                num_uses--;
                if (num_uses == 0) whitelist.add(gway); else whitelist_in_use.put(gway, num_uses);
            } else if (blacklist_in_use.containsKey(gway)) {
                Integer num_uses = blacklist_in_use.remove(gway);
                assert (num_uses != null);
                num_uses--;
                if (num_uses == 0) blacklist.add(gway); else blacklist_in_use.put(gway, num_uses);
            }
            return;
        }

        public void recordGatewaySuccess(NodeId gway) {
            if (whitelist_in_use.containsKey(gway)) {
                Integer num_uses = whitelist_in_use.remove(gway);
                assert (num_uses != null);
                num_uses--;
                if (num_uses == 0) whitelist.add(0, gway); else whitelist_in_use.put(gway, num_uses);
            } else if (blacklist_in_use.containsKey(gway)) {
                Integer num_uses = blacklist_in_use.remove(gway);
                assert (num_uses != null);
                num_uses--;
                if (num_uses == 0) whitelist.add(0, gway); else whitelist_in_use.put(gway, num_uses);
            }
            return;
        }
    }
}
