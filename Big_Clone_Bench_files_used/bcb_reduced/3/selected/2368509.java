package antiquity.cr.impl;

import antiquity.util.XdrUtils;
import static antiquity.util.XdrUtils.serialize;
import antiquity.util.ValueTriple;
import antiquity.util.AntiquityUtils;
import static antiquity.util.AntiquityUtils.Procedure;
import static antiquity.util.AntiquityUtils.readPublicKey;
import static antiquity.util.AntiquityUtils.readPrivateKey;
import static antiquity.util.AntiquityUtils.createSignature;
import static antiquity.util.AntiquityUtils.verifySignature;
import static antiquity.util.AntiquityUtils.getProcedureKey;
import static antiquity.util.AntiquityUtils.guidToBigInteger;
import static antiquity.util.AntiquityUtils.getCRProcedureMap;
import static antiquity.util.AntiquityUtils.byteArrayToBigInteger;
import static antiquity.util.AntiquityUtils.CR_NULL_GUID;
import static antiquity.util.AntiquityUtils.CR_NULL_PUBLIC_KEY;
import antiquity.cr.api.cr_id;
import antiquity.cr.api.cr_api;
import antiquity.cr.api.cr_guid;
import antiquity.cr.api.cr_status;
import antiquity.cr.api.cr_address;
import antiquity.cr.api.cr_public_key;
import antiquity.cr.api.cr_certificate;
import antiquity.cr.api.cr_soundness_proof;
import antiquity.cr.api.cr_signed_signature;
import antiquity.cr.api.cr_ss_set_config;
import antiquity.cr.api.cr_signed_certificate;
import antiquity.cr.api.cr_signed_ss_set_config;
import antiquity.cr.api.cr_create_ss_set_args;
import antiquity.cr.api.cr_create_ss_set_result;
import antiquity.cr.api.cr_renew_args;
import antiquity.cr.api.cr_renew_result;
import antiquity.cr.api.cr_repair_args;
import antiquity.cr.api.cr_repair_result;
import antiquity.cr.api.cr_get_latest_config_args;
import antiquity.cr.api.cr_get_latest_config_result;
import static antiquity.cr.api.cr_api.CR_API;
import static antiquity.cr.api.cr_api.CR_API_VERSION;
import static antiquity.cr.api.cr_api.CR_KEY_VERIFIED;
import static antiquity.cr.api.cr_api.CR_HASH_VERIFIED;
import static antiquity.cr.api.cr_api.CR_REDUNDANCY_TYPE_REPL;
import static antiquity.cr.api.cr_api.CR_REDUNDANCY_TYPE_CAUCHY;
import static antiquity.cr.api.cr_api.cr_null_1;
import static antiquity.cr.api.cr_api.cr_create_ss_set_1;
import static antiquity.cr.api.cr_api.cr_renew_1;
import static antiquity.cr.api.cr_api.cr_repair_1;
import antiquity.ss.impl.StorageServer;
import static antiquity.ss.impl.RepairStage.createRepairSignature;
import antiquity.rpc.api.RpcCall;
import antiquity.rpc.api.RpcReply;
import antiquity.rpc.api.RpcRegisterReq;
import antiquity.rpc.api.RpcRegisterResp;
import static antiquity.rpc.api.ProcInfo.ProcKey;
import static antiquity.rpc.api.ProcInfo.ProcValue;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.Signature;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;
import ostore.util.NodeId;
import ostore.util.ByteUtils;
import ostore.util.SecureHash;
import ostore.util.SHA1Hash;
import ostore.util.StandardStage;
import ostore.dispatch.Filter;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.XdrVoid;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandlerException;
import seda.sandStorm.api.StagesInitializedSignal;
import bamboo.lss.DustDevil;
import bamboo.lss.ASyncCore;
import bamboo.util.GuidTools;
import static bamboo.util.Curry.Thunk1;
import static bamboo.util.Curry.curry;

/**
 * An event-driven client that uses the two-level naming api.
 *
 * @author Hakim Weatherspoon
 * @version $Id: ExampleClient.java,v 1.1.1.1 2006/12/15 22:45:36 hweather Exp $
 */
public class ExampleClient extends ostore.util.StandardStage {

    private static final int MAX_TTL = cr_api.CR_MAX_TTL;

    /** Unique identifier for <code>this</code> stage. */
    private static final long appId = bamboo.router.Router.app_id(ExampleClient.class);

    private int SS_SET_SIZE;

    private long TIMEOUT_MS;

    private Signature _sig_engine;

    private Signature _verify_engine;

    private KeyFactory _key_factory;

    private PrivateKey _skey;

    private PublicKey _pkey;

    private cr_public_key _cr_pkey;

    private MessageDigest _md;

    private ASyncCore _acore;

    private Map<Long, RpcCall> _pending_events;

    private long _next_seq;

    /** Gateway {@link ostore.util.NodeId}. */
    protected NodeId gway;

    /**
   * {@link java.util.Map} associating gateway guid to 
   * {@link java.security.PublicKey}/{@link java.security.PrivateKey} pair. */
    private Map<BigInteger, ValueTriple<cr_public_key, PrivateKey, Signature>> _gwayKeys;

    /** Gateway {@link ostore.util.NodeId NodeId's}. */
    protected LinkedList<NodeId> gateways;

    /** Block events until all stages ready. */
    private Vector<QueueElementIF> _blockedEvents;

    /** All local stages have been initialized */
    private boolean _stagesInitialized;

    /** All remote procedure calls have been registered */
    private boolean _registered;

    /** Constructor: Creates a new <code>ExampleClient</code> stage. */
    public ExampleClient() throws Exception {
        event_types = new Class[] { seda.sandStorm.api.StagesInitializedSignal.class };
        _pending_events = new HashMap<Long, RpcCall>();
        gateways = new LinkedList<NodeId>();
        _gwayKeys = new HashMap<BigInteger, ValueTriple<cr_public_key, PrivateKey, Signature>>();
        _next_seq = 0;
        _acore = DustDevil.acore_instance();
        _blockedEvents = new Vector<QueueElementIF>();
        _md = MessageDigest.getInstance("SHA");
        logger.info("Client.<init>: Creating signature engine...");
        String sig_alg = "SHA1withRSA";
        if (ostore.security.NativeRSAAlgorithm.available) {
            java.security.Security.addProvider(new ostore.security.NativeRSAProvider());
            _sig_engine = Signature.getInstance("SHA-1/RSA/PKCS#1", "NativeRSA");
            _verify_engine = Signature.getInstance("SHA-1/RSA/PKCS#1", "NativeRSA");
        } else {
            _sig_engine = Signature.getInstance(sig_alg);
            _verify_engine = Signature.getInstance(sig_alg);
        }
        _key_factory = KeyFactory.getInstance("RSA");
        logger.info("Client.<init>: " + "done.");
        logger.info("Initializing Client...");
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void init(ConfigDataIF config) throws Exception {
        final String method_tag = tag + ".init";
        super.init(config);
        assert config.contains("timeout") : "Need to define timeout (in ms) in cfg file";
        TIMEOUT_MS = config_get_long(config, "timeout");
        assert config.contains("ss_set_size") : "Need to define ss_set_size in cfg";
        SS_SET_SIZE = config_get_int(config, "ss_set_size");
        assert config.contains("PkeyFilename") : "Need to define PkeyFilename in cfg file";
        assert config.contains("SkeyFilename") : "Need to define SkeyFilename in cfg file";
        logger.info(method_tag + ": Reading keys from disk...");
        String pkey_filename = config_get_string(config, "PkeyFilename");
        String skey_filename = config_get_string(config, "SkeyFilename");
        _pkey = readPublicKey(pkey_filename, _key_factory);
        _skey = readPrivateKey(skey_filename, _key_factory);
        if ((_pkey == null) || (_skey == null)) throw new Exception(method_tag + ": Failed to read key pair from disk: " + "pkey=" + pkey_filename + ", skey=" + skey_filename);
        _cr_pkey = new cr_public_key(_pkey.getEncoded());
        _sig_engine.initSign(_skey);
        logger.info(method_tag + ": Reading keys from disk...done.");
        int cnt = config_get_int(config, "gateway_count");
        if (cnt == -1) {
            gway = new NodeId(config_get_string(config, "gateway"));
            gateways.addLast(gway);
        } else {
            String regex = "0x[0-9a-fA-F]+";
            String sig_alg = "SHA1withRSA";
            for (int i = 0; i < cnt; ++i) {
                gway = new NodeId(config_get_string(config, "gateway_" + i));
                gateways.addLast(gway);
                String gway_guid_str = config_get_string(config, "gateway_guid_" + i);
                if (!gway_guid_str.matches(regex)) {
                    BUG("gway_guid_" + i + "=" + gway_guid_str + " must match " + regex);
                }
                BigInteger gway_guid = new BigInteger(gway_guid_str.substring(2), 16);
                String gway_pkey_filename = config_get_string(config, "gateway_pkey_" + i);
                String gway_skey_filename = config_get_string(config, "gateway_skey_" + i);
                PublicKey gway_pkey = readPublicKey(gway_pkey_filename, _key_factory);
                PrivateKey gway_skey = readPrivateKey(gway_skey_filename, _key_factory);
                if ((gway_pkey == null) || (gway_skey == null)) throw new Exception(method_tag + ": Failed to read gway private" + " key from disk: " + " pkey=" + gway_skey_filename + ", gway_skey=" + gway_skey_filename);
                cr_public_key gway_cr_pkey = new cr_public_key(gway_pkey.getEncoded());
                _md.update(gway_cr_pkey.value);
                BigInteger gway_guid2 = byteArrayToBigInteger(_md.digest());
                assert gway_guid.equals(gway_guid2) : "gway_guid=" + GuidTools.guid_to_string(gway_guid) + " != gway_guid2=" + GuidTools.guid_to_string(gway_guid2);
                Signature gway_sig_engine = null;
                if (ostore.security.NativeRSAAlgorithm.available) {
                    java.security.Security.addProvider(new ostore.security.NativeRSAProvider());
                    gway_sig_engine = Signature.getInstance("SHA-1/RSA/PKCS#1", "NativeRSA");
                } else {
                    gway_sig_engine = Signature.getInstance(sig_alg);
                }
                gway_sig_engine.initSign(gway_skey);
                _gwayKeys.put(gway_guid, new ValueTriple<cr_public_key, PrivateKey, Signature>(gway_cr_pkey, gway_skey, gway_sig_engine));
            }
        }
        Class[] rpc_msg_types = new Class[] { antiquity.rpc.api.RpcReply.class, antiquity.rpc.api.RpcRegisterResp.class };
        for (Class clazz : rpc_msg_types) {
            Filter filter = new Filter();
            if (!filter.requireType(clazz)) BUG(tag + ": could not require type " + clazz.getName());
            if (antiquity.rpc.api.RpcReply.class.isAssignableFrom(clazz)) {
                if (!filter.requireValue("inbound", new Boolean(true))) BUG(tag + ": could not require inbound = true for " + clazz.getName());
            }
            if (!filter.requireValue("appId", appId)) BUG(tag + ": could not require appId = " + appId + " for " + clazz.getName());
            if (DEBUG) logger.info(tag + ": subscribing to " + clazz);
            classifier.subscribe(filter, my_sink);
        }
        logger.info(method_tag + ": " + "init done");
        logger.info(method_tag + ": " + "Accepting requests...");
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void handleEvent(QueueElementIF item) throws EventHandlerException {
        final String method_tag = tag + ".handleEvent";
        if (item instanceof StagesInitializedSignal) {
            handleStagesInitializedSignal((StagesInitializedSignal) item);
            return;
        } else if (item instanceof RpcRegisterResp) {
            handleRpcRegisterResp((RpcRegisterResp) item);
            return;
        } else if (!_stagesInitialized || !_registered) {
            if (DEBUG) logger.info(method_tag + ": " + ": Queueing event " + item + " until stage initialized and registered.");
            _blockedEvents.add(item);
            return;
        } else if (item instanceof RpcReply) {
            RpcReply resp = (RpcReply) item;
            Thunk1<RpcReply> cb = (Thunk1<RpcReply>) resp.userData;
            cb.run(resp);
        } else {
            BUG(method_tag + ": " + "got unknown event: " + item);
        }
        return;
    }

    /**
   * <CODE>handleStagesInitializedSignal</CODE> does nothing.
   *
   * @param signal {@link seda.sandStorm.api.StagesInitializedSignal} */
    private void handleStagesInitializedSignal(StagesInitializedSignal signal) {
        final String method_tag = tag + ".handleStagesInitializedSignal";
        if (DEBUG) logger.info(method_tag + ": " + "called " + signal);
        _stagesInitialized = true;
        RpcRegisterReq req = new RpcRegisterReq(getCRProcedureMap(), true, appId, null, my_sink);
        dispatch(req);
        return;
    }

    /**
   * <CODE>handleRpcRegisteredResp</CODE> verifies that all remote
   * procedure calls were registered.
   *
   * @param resp {@link antiquity.rpc.api.RpcRegisterResp} */
    private void handleRpcRegisterResp(RpcRegisterResp resp) {
        final String method_tag = tag + ".handleRpcRegisterResp";
        if (DEBUG) logger.info(method_tag + ": " + "called " + resp);
        _registered = true;
        for (Map.Entry<ProcKey, Integer> entry : resp.responses.entrySet()) {
            if (!RpcRegisterResp.SUCCESS.equals((Integer) entry.getValue())) BUG(method_tag + ": proc " + entry + " was not registered.");
        }
        if (_stagesInitialized && _registered) initializeExampleClient();
    }

    /**
   * <CODE>initializeExampleClient</CODE> unblocks all blocked events.
   * That is, <I>all systems go</I>!
   * <I>INVARIANT</I> All stages initialized and rpc handlers registered. */
    private void initializeExampleClient() {
        final String method_tag = tag + ".initializeDummyASyncServer";
        if (DEBUG) logger.info(method_tag + ": called\n");
        logger.info(method_tag + ": " + "Accepting requests...");
        while (_blockedEvents.size() > 0) {
            try {
                handleEvent((QueueElementIF) _blockedEvents.remove(0));
            } catch (EventHandlerException ehe) {
                BUG(ehe);
            }
        }
        Runnable test_cb = new Runnable() {

            public void run() {
                try {
                    test();
                } catch (Exception e) {
                    BUG("test_cb.test: " + e);
                }
            }
        };
        _acore.register_timer(0L, test_cb);
    }

    /**********************************************************************/
    public void test() throws Exception {
        final String method_tag = tag + ".test";
        XdrVoid null_args = XdrVoid.XDR_VOID;
        final Long start_time_us = now_us();
        final Long seq = new Long(_next_seq++);
        final RpcCall req = new RpcCall(gway, getProcedureKey(AntiquityUtils.Procedure.CR_NULL), null_args, appId, null, my_sink);
        req.userData = new Thunk1<RpcReply>() {

            public void run(RpcReply resp) {
                null_done(req, start_time_us, seq, resp);
            }
        };
        _pending_events.put(seq, req);
        _acore.register_timer(TIMEOUT_MS, curry(timeout_cb, seq));
        logger.info(method_tag + ": " + "Calling null procedure...");
        dispatch(req);
    }

    public void null_done(RpcCall call, final Long start_time_us, final Long seq, RpcReply res) {
        final String method_tag = tag + ".null_done";
        XdrVoid null_args = (XdrVoid) call.args;
        XdrVoid null_result = (XdrVoid) res.reply;
        logger.info(method_tag + ": " + "null done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
        assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + null_args + ", but is not.";
        send_create();
    }

    private void send_create() {
        final String method_tag = tag + ".send_create";
        logger.info(method_tag + ": " + "Calling create_ss_set procedure...");
        cr_signed_certificate create_cert = null;
        byte[] create_cert_ser = null;
        byte[] create_sig = null;
        try {
            create_cert = AntiquityUtils.createCertificate(_cr_pkey, CR_KEY_VERIFIED, (cr_guid[]) null, 0, 0, 0L, now_ms(), now_ms() + (long) MAX_TTL * 1000L, new byte[0], new byte[0], cr_signed_certificate.class, _md);
            create_cert.signature = createSignature(create_cert.cert, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        cr_create_ss_set_args create_args = new cr_create_ss_set_args();
        create_args.cert = create_cert;
        create_args.proposed_config = new cr_ss_set_config();
        create_args.proposed_config.type = (byte) CR_KEY_VERIFIED;
        create_args.proposed_config.redundancy_type = (byte) CR_REDUNDANCY_TYPE_REPL;
        create_args.proposed_config.ss_set_size = (byte) SS_SET_SIZE;
        create_args.proposed_config.inv_rate = (byte) SS_SET_SIZE;
        create_args.proposed_config.threshold = (byte) (SS_SET_SIZE - 1);
        create_args.proposed_config.public_key = CR_NULL_PUBLIC_KEY;
        create_args.proposed_config.extent_key = CR_NULL_GUID;
        create_args.proposed_config.client_id = CR_NULL_GUID;
        create_args.proposed_config.ss_set = new cr_id[0];
        final Long start_time_us2 = now_us();
        final Long seq2 = new Long(_next_seq++);
        final RpcCall req = new RpcCall(gway, getProcedureKey(Procedure.CR_CREATE_SS_SET), create_args, appId, null, my_sink);
        req.userData = new Thunk1<RpcReply>() {

            public void run(RpcReply resp) {
                create_ss_set_done(req, start_time_us2, seq2, resp);
            }
        };
        _pending_events.put(seq2, req);
        _acore.register_timer(TIMEOUT_MS, curry(timeout_cb, seq2));
        dispatch(req);
    }

    public void create_ss_set_done(RpcCall call, Long start_time_us, Long seq, RpcReply res) {
        final String method_tag = tag + ".create_ss_set_done";
        if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
        cr_create_ss_set_args create_args = (cr_create_ss_set_args) call.args;
        cr_create_ss_set_result create_result = (cr_create_ss_set_result) res.reply;
        cr_signed_ss_set_config config = create_result.config;
        assert create_result.status == cr_status.STATUS_OK : method_tag + ": " + "returned error. status=" + create_result.status;
        assert config.config != null && config.config.ss_set != null && config.config.ss_set.length == SS_SET_SIZE : method_tag + ": reply contained only " + (config.config.ss_set == null ? -1 : config.config.ss_set.length) + " storager servers, instead of " + SS_SET_SIZE;
        assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + XdrUtils.toString(create_args) + ", but is not.";
        logger.info(method_tag + ": The StorageSetCreator returned the following" + " storage servers: " + XdrUtils.toString(create_result));
        logger.info(method_tag + ": " + "cr_create_ss_set done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
        if (!verifySignature(create_result.config.config, create_result.config.signature, create_result.config.config.public_key, _key_factory, _verify_engine)) BUG(method_tag + ": verification failed for log " + XdrUtils.toString(create_result));
        cr_soundness_proof prev_proof = null;
        {
            prev_proof = new cr_soundness_proof();
            prev_proof.cert = create_args.cert;
            prev_proof.config = create_result.config;
            prev_proof.sigs = new cr_signed_signature[create_result.config.config.ss_set_size];
            for (int i = 0; i < prev_proof.sigs.length; i++) {
                BigInteger gway_guid = guidToBigInteger(prev_proof.config.config.ss_set[i].guid);
                ValueTriple<cr_public_key, PrivateKey, Signature> gway_keys = _gwayKeys.get(gway_guid);
                assert gway_keys != null : "keys is null for gway_guid=0x" + GuidTools.guid_to_string(gway_guid);
                prev_proof.sigs[i] = StorageServer.createSignature(prev_proof.config, prev_proof.cert, gway_keys.getFirst(), cr_signed_signature.class, gway_keys.getSecond(), _md, _key_factory, gway_keys.getThird());
            }
        }
        send_renew_repair(true, prev_proof);
    }

    private void send_renew_repair(boolean renew, cr_soundness_proof prev_proof) {
        final String method_tag = tag + ".send_" + (renew ? "renew" : "repair");
        logger.info(method_tag + ": " + "Calling " + (renew ? "renew" : "repair") + " procedure...");
        cr_signed_certificate cert = null;
        if (renew) {
            cert = XdrUtils.clone(prev_proof.cert);
            cert.cert.seq_num++;
            cert.cert.timestamp = now_ms();
            cert.cert.expire_time = cert.cert.timestamp + MAX_TTL * 1000L;
            try {
                cert.signature = createSignature(cert.cert, _skey, _sig_engine);
            } catch (Exception e) {
                BUG(method_tag + ": " + e);
            }
        }
        cr_ss_set_config proposed_config = XdrUtils.clone(prev_proof.config.config);
        cr_signed_ss_set_config latest_config = prev_proof.config;
        cr_soundness_proof[] proofs = new cr_soundness_proof[] { prev_proof };
        _md.update(serialize(prev_proof.cert.cert));
        _md.update(serialize(prev_proof.config.config));
        cr_guid sig_proof_hash = new cr_guid(_md.digest());
        cr_signed_signature[] sigs = new cr_signed_signature[prev_proof.config.config.ss_set_size];
        cr_guid[] sig_proof_hashes = new cr_guid[sigs.length];
        for (int i = 0; i < prev_proof.sigs.length; i++) {
            BigInteger gway_guid = guidToBigInteger(prev_proof.config.config.ss_set[i].guid);
            ValueTriple<cr_public_key, PrivateKey, Signature> gway_keys = _gwayKeys.get(gway_guid);
            assert gway_keys != null : "keys is null for gway_guid=0x" + GuidTools.guid_to_string(gway_guid);
            sigs[i] = createRepairSignature(prev_proof, prev_proof.config, gway_keys.getFirst(), cr_signed_signature.class, gway_keys.getSecond(), _md, _key_factory, gway_keys.getThird());
            sig_proof_hashes[i] = sig_proof_hash;
        }
        XdrAble args = null;
        if (renew) {
            cr_renew_args renew_args = new cr_renew_args();
            renew_args.cert = cert;
            renew_args.proposed_config = proposed_config;
            renew_args.latest_config = latest_config;
            renew_args.proof = proofs;
            renew_args.sig = sigs;
            renew_args.sig_proof_hash = sig_proof_hashes;
            args = renew_args;
        } else {
            cr_repair_args repair_args = new cr_repair_args();
            repair_args.proposed_config = proposed_config;
            repair_args.latest_config = latest_config;
            repair_args.proof = proofs;
            repair_args.sig = sigs;
            repair_args.sig_proof_hash = sig_proof_hashes;
            args = repair_args;
        }
        final boolean renew2 = renew;
        final Long start_time_us2 = now_us();
        final Long seq2 = new Long(_next_seq++);
        final RpcCall req = new RpcCall(gway, getProcedureKey(renew ? Procedure.CR_RENEW : Procedure.CR_REPAIR), args, appId, null, my_sink);
        req.userData = new Thunk1<RpcReply>() {

            public void run(RpcReply resp) {
                renew_repair_done(renew2, req, start_time_us2, seq2, resp);
            }
        };
        _pending_events.put(seq2, req);
        _acore.register_timer(TIMEOUT_MS, curry(timeout_cb, seq2));
        dispatch(req);
    }

    private void renew_repair_done(boolean renew, RpcCall call, Long start_time_us, Long seq, RpcReply res) {
        final String method_tag = tag + "." + (renew ? "renew" : "repair") + "_done";
        if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
        int result_status = -1;
        cr_signed_certificate args_cert = null;
        cr_signed_ss_set_config result_config = null;
        if (res.proc.getProcNum() == cr_renew_1) {
            cr_renew_args renew_args = (cr_renew_args) call.args;
            cr_renew_result renew_result = (cr_renew_result) res.reply;
            cr_signed_ss_set_config config = renew_result.config;
            args_cert = renew_args.cert;
            result_config = renew_result.config;
            result_status = renew_result.status;
        } else {
            cr_repair_args repair_args = (cr_repair_args) call.args;
            cr_repair_result repair_result = (cr_repair_result) res.reply;
            cr_signed_ss_set_config config = repair_result.config;
            args_cert = repair_args.proof[0].cert;
            result_config = repair_result.config;
            result_status = repair_result.status;
        }
        assert result_status == cr_status.STATUS_OK : method_tag + ": " + "returned error. status=" + result_status;
        assert result_config.config != null && result_config.config.ss_set != null && result_config.config.ss_set.length == SS_SET_SIZE : method_tag + ": reply contained only " + (result_config.config.ss_set == null ? -1 : result_config.config.ss_set.length) + " storager servers, instead of " + SS_SET_SIZE;
        assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + XdrUtils.toString(call.args) + ", but is not.";
        logger.info(method_tag + ": The StorageSetCreator returned the following" + " storage servers: " + XdrUtils.toString(res.reply));
        logger.info(method_tag + ": " + "cr_renew done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
        if (!verifySignature(result_config.config, result_config.signature, result_config.config.public_key, _key_factory, _verify_engine)) BUG(method_tag + ": verification failed for log " + XdrUtils.toString(res.reply));
        cr_soundness_proof prev_proof = null;
        {
            prev_proof = new cr_soundness_proof();
            prev_proof.cert = args_cert;
            prev_proof.config = result_config;
            prev_proof.sigs = new cr_signed_signature[result_config.config.ss_set_size];
            for (int i = 0; i < prev_proof.sigs.length; i++) {
                BigInteger gway_guid = guidToBigInteger(prev_proof.config.config.ss_set[i].guid);
                ValueTriple<cr_public_key, PrivateKey, Signature> gway_keys = _gwayKeys.get(gway_guid);
                assert gway_keys != null : "keys is null for gway_guid=0x" + GuidTools.guid_to_string(gway_guid);
                prev_proof.sigs[i] = StorageServer.createSignature(prev_proof.config, prev_proof.cert, gway_keys.getFirst(), cr_signed_signature.class, gway_keys.getSecond(), _md, _key_factory, gway_keys.getThird());
            }
        }
        if (renew) {
            send_renew_repair(false, prev_proof);
        } else {
            send_get(prev_proof);
        }
    }

    private void send_get(cr_soundness_proof prev_proof) {
        final String method_tag = tag + ".send_get";
        logger.info(method_tag + ": " + "Calling get_latest_config procedure...");
        cr_get_latest_config_args get_args = new cr_get_latest_config_args();
        get_args.client_id = prev_proof.config.config.client_id;
        get_args.extent_key = prev_proof.config.config.extent_key;
        final cr_soundness_proof prev_proof2 = prev_proof;
        final Long start_time_us2 = now_us();
        final Long seq2 = new Long(_next_seq++);
        final RpcCall req = new RpcCall(gway, getProcedureKey(Procedure.CR_GET_LATEST_CONFIG), get_args, appId, null, my_sink);
        req.userData = new Thunk1<RpcReply>() {

            public void run(RpcReply resp) {
                get_done(prev_proof2, req, start_time_us2, seq2, resp);
            }
        };
        _pending_events.put(seq2, req);
        _acore.register_timer(TIMEOUT_MS, curry(timeout_cb, seq2));
        dispatch(req);
    }

    public void get_done(cr_soundness_proof prev_proof, RpcCall call, Long start_time_us, Long seq, RpcReply res) {
        final String method_tag = tag + ".get_done";
        if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
        cr_get_latest_config_args get_args = (cr_get_latest_config_args) call.args;
        cr_get_latest_config_result get_result = (cr_get_latest_config_result) res.reply;
        cr_signed_ss_set_config config = get_result.config;
        assert get_result.status == cr_status.STATUS_OK : method_tag + ": " + "returned error. status=" + get_result.status;
        assert config.config != null && config.config.ss_set != null && config.config.ss_set.length == SS_SET_SIZE : method_tag + ": reply contained only " + (config.config.ss_set == null ? -1 : config.config.ss_set.length) + " storager servers, instead of " + SS_SET_SIZE;
        assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + XdrUtils.toString(get_args) + ", but is not.";
        logger.info(method_tag + ": The StorageSetCreator returned the following" + " storage servers: " + XdrUtils.toString(get_result));
        logger.info(method_tag + ": " + "cr_get_latest_config done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
        if (!verifySignature(get_result.config.config, get_result.config.signature, get_result.config.config.public_key, _key_factory, _verify_engine)) BUG(method_tag + ": verification failed for log " + XdrUtils.toString(get_result));
        assert XdrUtils.equals(prev_proof.config.config, get_result.config.config) : method_tag + ": prev_proof.config=" + XdrUtils.toString(prev_proof.config) + " != get_result.config=" + XdrUtils.toString(get_result.config);
        logger.info("All procedures passed!  Exitting...");
        System.exit(0);
    }

    public Thunk1<Long> timeout_cb = new Thunk1<Long>() {

        public void run(Long seq) {
            final String method_tag = tag + ".timeout_cb.call";
            RpcCall call = (RpcCall) _pending_events.remove(seq);
            if (call != null) {
                BUG("timed out on call " + call);
            }
        }
    };
}
