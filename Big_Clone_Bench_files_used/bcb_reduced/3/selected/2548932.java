package antiquity.gw.impl;

import antiquity.util.Extent;
import antiquity.util.XdrUtils;
import antiquity.util.AntiquityUtils;
import antiquity.util.ValuePair;
import antiquity.util.ValueTriple;
import static antiquity.util.AntiquityUtils.convert;
import static antiquity.util.AntiquityUtils.Procedure;
import static antiquity.util.AntiquityUtils.computeGuid;
import static antiquity.util.AntiquityUtils.readPublicKey;
import static antiquity.util.AntiquityUtils.readPrivateKey;
import static antiquity.util.AntiquityUtils.addressToNodeId;
import static antiquity.util.AntiquityUtils.getProcedureKey;
import static antiquity.util.AntiquityUtils.createSignature;
import static antiquity.util.AntiquityUtils.verifySignature;
import static antiquity.util.AntiquityUtils.guidToSecureHash;
import static antiquity.util.AntiquityUtils.secureHashToGuid;
import static antiquity.util.AntiquityUtils.guidToBigInteger;
import static antiquity.util.AntiquityUtils.computeVerifier;
import static antiquity.util.AntiquityUtils.checkSoundnessProof;
import static antiquity.util.AntiquityUtils.CR_NULL_PUBLIC_KEY;
import antiquity.client.ClientLib;
import antiquity.client.ClientLibResult;
import antiquity.gw.api.gw_create_args;
import antiquity.gw.api.gw_append_args;
import antiquity.gw.api.gw_truncate_args;
import antiquity.gw.api.gw_snapshot_args;
import antiquity.gw.api.gw_put_args;
import antiquity.gw.api.gw_renew_args;
import antiquity.gw.api.gw_repair_args;
import antiquity.gw.api.gw_stop_config_args;
import antiquity.gw.api.gw_get_blocks_args;
import antiquity.gw.api.gw_get_extent_args;
import antiquity.gw.api.gw_get_certificate_args;
import antiquity.gw.api.gw_create_result;
import antiquity.gw.api.gw_append_result;
import antiquity.gw.api.gw_truncate_result;
import antiquity.gw.api.gw_snapshot_result;
import antiquity.gw.api.gw_put_result;
import antiquity.gw.api.gw_renew_result;
import antiquity.gw.api.gw_repair_result;
import antiquity.gw.api.gw_stop_config_result;
import antiquity.gw.api.gw_soundness_proof;
import antiquity.gw.api.gw_get_blocks_result;
import antiquity.gw.api.gw_get_extent_result;
import antiquity.gw.api.gw_get_certificate_result;
import antiquity.gw.api.gw_api;
import antiquity.gw.api.gw_guid;
import antiquity.gw.api.gw_status;
import antiquity.gw.api.gw_public_key;
import antiquity.gw.api.gw_data_block;
import antiquity.gw.api.gw_certificate;
import antiquity.gw.api.gw_signed_signature;
import antiquity.gw.api.gw_signed_certificate;
import antiquity.gw.api.gw_signed_ss_set_config;
import static antiquity.gw.api.gw_api.GW_API;
import static antiquity.gw.api.gw_api.GW_GUID_SIZE;
import static antiquity.gw.api.gw_api.GW_API_VERSION;
import static antiquity.gw.api.gw_api.GW_KEY_VERIFIED;
import static antiquity.gw.api.gw_api.GW_HASH_VERIFIED;
import static antiquity.gw.api.gw_api.gw_null_1;
import static antiquity.gw.api.gw_api.gw_put_1;
import static antiquity.gw.api.gw_api.gw_create_1;
import static antiquity.gw.api.gw_api.gw_append_1;
import static antiquity.gw.api.gw_api.gw_get_extent_1;
import static antiquity.gw.api.gw_api.gw_get_blocks_1;
import static antiquity.gw.api.gw_api.gw_get_certificate_1;
import antiquity.ss.api.ss_id;
import antiquity.ss.api.ss_guid;
import antiquity.ss.api.ss_status;
import antiquity.ss.api.ss_append_args;
import antiquity.ss.api.ss_append_result;
import antiquity.ss.api.ss_renew_args;
import antiquity.ss.api.ss_stop_config_args;
import antiquity.ss.api.ss_renew_result;
import antiquity.ss.api.ss_stop_config_result;
import antiquity.ss.api.ss_create_args;
import antiquity.ss.api.ss_create_result;
import antiquity.ss.api.ss_soundness_proof;
import antiquity.ss.api.ss_signed_certificate;
import antiquity.ss.api.ss_signed_ss_set_config;
import static antiquity.ss.impl.StorageServer.Tag;
import antiquity.cr.api.cr_id;
import antiquity.cr.api.cr_guid;
import antiquity.cr.api.cr_status;
import antiquity.cr.api.cr_ss_set_config;
import antiquity.cr.api.cr_soundness_proof;
import antiquity.cr.api.cr_signed_certificate;
import antiquity.cr.api.cr_signed_ss_set_config;
import antiquity.cr.api.cr_repair_args;
import antiquity.cr.api.cr_create_ss_set_args;
import antiquity.cr.api.cr_create_ss_set_result;
import static antiquity.cr.api.cr_api.CR_REDUNDANCY_TYPE_REPL;
import antiquity.cr.impl.StorageSetCreator;
import antiquity.rpc.api.RpcCall;
import antiquity.rpc.api.RpcReply;
import antiquity.rpc.api.RpcRegisterReq;
import antiquity.rpc.api.RpcRegisterResp;
import static antiquity.rpc.api.ProcInfo.ProcKey;
import static antiquity.rpc.api.ProcInfo.ProcValue;
import antiquity.rpc.impl.RpcClientStage;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Vector;
import java.util.Random;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.LinkedHashSet;
import java.math.BigInteger;
import dd.api.DDThresholdTag;
import dd.api.DDRepairObjReq;
import dd.api.DDRepairObjResp;
import ostore.util.NodeId;
import ostore.util.ByteUtils;
import ostore.util.SecureHash;
import ostore.util.SHA1Hash;
import ostore.util.QSTreeSet;
import ostore.util.StandardStage;
import ostore.dispatch.Filter;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.XdrVoid;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandlerException;
import seda.sandStorm.api.StagesInitializedSignal;
import bamboo.lss.DustDevil;
import bamboo.lss.ASyncCore;
import bamboo.util.GuidTools;
import static bamboo.util.Curry.Thunk1;
import static bamboo.util.Curry.Thunk4;
import static bamboo.util.Curry.Thunk5;
import static bamboo.util.Curry.Thunk6;
import static bamboo.util.Curry.Thunk7;
import static bamboo.util.Curry.curry;

/**
 * An event-driven client that uses the two-level naming api.
 *
 * @author Hakim Weatherspoon
 * @version $Id: ClientASyncExample.java,v 1.1.1.1 2006/12/15 22:45:37 hweather Exp $
 */
public class ClientASyncExample extends ostore.util.StandardStage {

    private static final int MAX_TTL = gw_api.GW_MAX_TTL;

    private static final long TIMEOUT_MS = 420000L;

    private static final int MAX_RENEW_ATTEMPTS = 2;

    /** Unique identifier for <code>this</code> stage. */
    private static final long appId = bamboo.router.Router.app_id(ClientASyncExample.class);

    private boolean TRIGGER_REPAIR;

    private Map<BigInteger, Extent<gw_public_key, gw_guid, gw_signed_certificate>> _extents;

    private Map<BigInteger, gw_soundness_proof> _proofs;

    private boolean _firstRepair;

    private RpcCall dummy_req;

    private ClientLib _client_lib;

    private RpcClientStage _rpc_stage;

    private Signature _sig_engine;

    private Signature _verify_engine;

    private KeyFactory _key_factory;

    private PrivateKey _skey;

    private PublicKey _pkey;

    private gw_public_key _ant_pkey;

    private MessageDigest _md;

    private ASyncCore _acore;

    private Map<Long, RpcCall> _pending_events;

    private Map<Long, DDRepairObjReq> _pending_events_dd;

    private long _next_seq;

    private long _next_cert_seq;

    private Random _rand;

    private Map<SecureHash, ValueTriple<Set<SecureHash>, Extent<gw_public_key, gw_guid, gw_signed_certificate>, ss_soundness_proof>> _ddRepairs;

    private Map<SecureHash, ValuePair<Long, Long>> _ddRepairTokens;

    private Extent<gw_public_key, gw_guid, gw_signed_certificate> _create_extent;

    private Extent<gw_public_key, gw_guid, gw_signed_certificate> _snapshot_extent;

    private int _num_append_operations;

    private gw_data_block[] _append_blocks;

    private gw_append_result _append_result;

    private gw_truncate_result _truncate_result;

    private gw_snapshot_result _snapshot_result;

    /** Gateway {@link ostore.util.NodeId}. */
    protected NodeId gway;

    /** Gateway {@link ostore.util.NodeId NodeId's}. */
    protected LinkedList<NodeId> _gateways;

    /** Block events until all stages ready. */
    private Vector<QueueElementIF> _blockedEvents;

    /** All local stages have been initialized */
    private boolean _stagesInitialized;

    /** All remote procedure calls have been registered */
    private boolean _registered;

    /** Constructor: Creates a new <code>ClientASyncExample</code> stage. */
    public ClientASyncExample() throws Exception {
        final String method_tag = tag + ".<init>";
        event_types = new Class[] { seda.sandStorm.api.StagesInitializedSignal.class };
        inb_msg_types = new Class[] { dd.api.DDRepairObjResp.class };
        _ddRepairs = new HashMap<SecureHash, ValueTriple<Set<SecureHash>, Extent<gw_public_key, gw_guid, gw_signed_certificate>, ss_soundness_proof>>();
        _ddRepairTokens = new HashMap<SecureHash, ValuePair<Long, Long>>();
        _extents = new HashMap<BigInteger, Extent<gw_public_key, gw_guid, gw_signed_certificate>>();
        _proofs = new HashMap<BigInteger, gw_soundness_proof>();
        _pending_events = new HashMap<Long, RpcCall>();
        _pending_events_dd = new HashMap<Long, DDRepairObjReq>();
        _gateways = new LinkedList<NodeId>();
        _firstRepair = true;
        _next_seq = 0;
        _next_cert_seq = -1;
        _num_append_operations = 0;
        _acore = DustDevil.acore_instance();
        _blockedEvents = new Vector<QueueElementIF>();
        _md = MessageDigest.getInstance("SHA");
        logger.info(method_tag + ": Creating signature engine...");
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
        logger.info(method_tag + ": " + "done.");
        logger.info("Initializing Client...");
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void init(ConfigDataIF config) throws Exception {
        final String method_tag = tag + ".init";
        super.init(config);
        dummy_req = new RpcCall(my_node_id, null, null, appId, null, (SinkIF) null);
        assert config.contains("TriggerRepair") : "Need to define TriggerRepair in cfg file";
        TRIGGER_REPAIR = config_get_boolean(config, "TriggerRepair");
        assert config.contains("PkeyFilename") : "Need to define PkeyFilename in cfg file";
        assert config.contains("SkeyFilename") : "Need to define SkeyFilename in cfg file";
        logger.info(method_tag + ": Reading keys from disk...");
        String pkey_filename = config_get_string(config, "PkeyFilename");
        String skey_filename = config_get_string(config, "SkeyFilename");
        _pkey = readPublicKey(pkey_filename, _key_factory);
        _skey = readPrivateKey(skey_filename, _key_factory);
        if ((_pkey == null) || (_skey == null)) throw new Exception(method_tag + ": Failed to read key pair from disk: " + "pkey=" + pkey_filename + ", skey=" + skey_filename);
        _ant_pkey = new gw_public_key(_pkey.getEncoded());
        _sig_engine.initSign(_skey);
        logger.info(method_tag + ": Reading keys from disk...done.");
        _rand = new Random(new SHA1Hash("" + my_node_id + now_us()).lower64bits());
        _next_cert_seq = _rand.nextInt(Integer.MAX_VALUE / 2);
        int cnt = config_get_int(config, "gateway_count");
        if (cnt == -1) {
            assert config.contains("gateway") : "Need to define gateway in cfg file";
            gway = new NodeId(config_get_string(config, "gateway"));
            _gateways.addLast(gway);
        } else {
            for (int i = 0; i < cnt; ++i) {
                assert config.contains("gateway_" + i) : "Need to define gateway_" + i + " in cfg file";
                gway = new NodeId(config_get_string(config, "gateway_" + i));
                _gateways.addLast(gway);
            }
        }
        _rpc_stage = RpcClientStage.getInstance(my_node_id);
        assert _rpc_stage != null : "rpc_stage is null for " + my_node_id;
        SinkIF rpc_sink = null;
        _client_lib = new ClientLib(_rpc_stage, appId, rpc_sink, _sig_engine, _md);
        _client_lib.addGateway(gway);
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
        } else if (item instanceof DDRepairObjResp) {
            handleDDRepairObjResp((DDRepairObjResp) item);
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
        RpcRegisterReq req = new RpcRegisterReq(AntiquityUtils.getGWProcedureMap(), true, appId, null, my_sink);
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
        if (_stagesInitialized && _registered) initializeClientASyncExample();
    }

    /**
   * <CODE>initializeClientASyncExample</CODE> unblocks all blocked events.
   * That is, <I>all systems go</I>!
   * <I>INVARIANT</I> All stages initialized and rpc handlers registered. */
    private void initializeClientASyncExample() {
        final String method_tag = tag + ".initializeClientASyncExample";
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
        logger.info(method_tag + ": " + "Calling null procedure...");
        XdrVoid null_args = XdrVoid.XDR_VOID;
        dispatch(gway, null_args, getProcedureKey(Procedure.GW_NULL), null_done);
    }

    private Thunk4<Long, Long, RpcCall, RpcReply> null_done = new Thunk4<Long, Long, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".null_done.call";
            XdrVoid null_args = (XdrVoid) call.args;
            XdrVoid null_result = (XdrVoid) res.reply;
            logger.info(method_tag + ": " + "null done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + null_args + ", but is not.";
            if (TRIGGER_REPAIR) {
                send_put();
            } else {
                send_put();
            }
        }
    };

    private void send_put() {
        final String method_tag = tag + ".send_put";
        logger.info(method_tag + ": " + "Calling put procedure...");
        gw_data_block[] put_blocks = createDataBlocks(0, 300, 1024);
        Extent<gw_public_key, gw_guid, gw_signed_certificate> put_extent = new Extent<gw_public_key, gw_guid, gw_signed_certificate>(_ant_pkey, gw_signed_certificate.class, _md);
        put_extent.appendBlocks(put_blocks);
        _extents.put(guidToBigInteger(put_extent.getHashName()), put_extent);
        gw_signed_certificate put_cert = put_extent.createCertificate(GW_HASH_VERIFIED, _rand.nextInt(Integer.MAX_VALUE / 2), now_ms(), now_ms() + (long) MAX_TTL * 1000L, new byte[0], new byte[0]);
        try {
            put_cert.signature = createSignature(put_cert.cert, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        put_extent.setCertificate(put_cert);
        gw_put_args put_args = new gw_put_args();
        put_args.cert = put_cert;
        put_args.data = put_blocks;
        dispatch(gway, put_args, getProcedureKey(Procedure.GW_PUT), put_done);
    }

    private Thunk4<Long, Long, RpcCall, RpcReply> put_done = new Thunk4<Long, Long, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".put_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_put_args put_args = (gw_put_args) call.args;
            gw_put_result put_result = (gw_put_result) res.reply;
            logger.info(method_tag + ": " + "put done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            RpcCall tmp = null;
            assert call == (tmp = (RpcCall) _pending_events.remove(seq)) : "Event seq=" + seq + " should be " + call + ", but is not." + tmp;
            if (put_result.status != gw_status.GW_STATUS_OK) BUG(method_tag + ": " + "returned error.");
            if (!checkSoundnessProof(put_result.proof, true, false, _md, _key_factory, _verify_engine)) BUG(method_tag + ": check failed for " + XdrUtils.toString(put_result));
            _proofs.put(guidToBigInteger(put_result.verifier), put_result.proof);
            logger.info(method_tag + ": " + "  ExtentKey = Verifier = " + XdrUtils.toString(put_result.verifier));
            gw_signed_certificate cert = null;
            {
                Extent<gw_public_key, gw_guid, gw_signed_certificate> ext = new Extent<gw_public_key, gw_guid, gw_signed_certificate>(_ant_pkey, gw_signed_certificate.class, _md);
                cert = createCreateCert(ext);
            }
            if (TRIGGER_REPAIR) {
                send_cr_create(convert(cert, cr_signed_certificate.class));
            } else {
                send_create(cert);
            }
        }
    };

    private void send_create(gw_signed_certificate cert) {
        final String method_tag = tag + ".send_create";
        logger.info(method_tag + ": " + "Calling create procedure...");
        Extent<gw_public_key, gw_guid, gw_signed_certificate> ext = new Extent<gw_public_key, gw_guid, gw_signed_certificate>(cert.cert.public_key, gw_signed_certificate.class, _md);
        ext.setCertificate(cert);
        if (XdrUtils.equals(cert.cert.public_key, _ant_pkey)) _create_extent = ext;
        gw_create_args create_args = new gw_create_args();
        create_args.cert = cert;
        _extents.put(guidToBigInteger(ext.getHashName()), ext);
        dispatch(gway, create_args, getProcedureKey(Procedure.GW_CREATE), curry(create_done, ext));
    }

    private Thunk5<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply> create_done = new Thunk5<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply>() {

        public void run(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".create_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_create_args create_args = (gw_create_args) call.args;
            gw_create_result create_result = (gw_create_result) res.reply;
            if (create_result.status != gw_status.GW_STATUS_OK) BUG(method_tag + ": " + "returned error.");
            if (!checkSoundnessProof(create_result.proof, true, false, _md, _key_factory, _verify_engine)) BUG(method_tag + ": check failed for " + XdrUtils.toString(create_result));
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + create_args + ", but is not.";
            _proofs.put(guidToBigInteger(create_result.extent_key), create_result.proof);
            logger.info(method_tag + ": " + "create done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(create_result.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(create_result.verifier));
            send_append(ext);
        }
    };

    private void send_cr_create(cr_signed_certificate cert) {
        final String method_tag = tag + ".send_cr_create";
        logger.info(method_tag + ": " + "Calling cr_create procedure...");
        cr_create_ss_set_args create_args = new cr_create_ss_set_args();
        create_args.cert = cert;
        int f = 1;
        create_args.proposed_config = new cr_ss_set_config();
        create_args.proposed_config.type = create_args.cert.cert.type;
        create_args.proposed_config.redundancy_type = CR_REDUNDANCY_TYPE_REPL;
        create_args.proposed_config.ss_set_size = (byte) (3 * f + 1);
        create_args.proposed_config.inv_rate = create_args.proposed_config.ss_set_size;
        create_args.proposed_config.threshold = (byte) (2 * f + 1);
        _md.update(cert.cert.public_key.value);
        create_args.proposed_config.client_id = new cr_guid(_md.digest());
        create_args.proposed_config.extent_key = create_args.proposed_config.client_id;
        create_args.proposed_config.public_key = CR_NULL_PUBLIC_KEY;
        create_args.proposed_config.ss_set = new cr_id[0];
        dispatch(gway, create_args, getProcedureKey(Procedure.CR_CREATE_SS_SET), create_cr_done);
    }

    private Thunk4<Long, Long, RpcCall, RpcReply> create_cr_done = new Thunk4<Long, Long, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".create_cr_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            cr_create_ss_set_args create_args = (cr_create_ss_set_args) call.args;
            cr_create_ss_set_result create_result = (cr_create_ss_set_result) res.reply;
            if (create_result.status != cr_status.STATUS_OK) BUG(method_tag + ": " + "returned error.");
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + create_args + ", but is not.";
            logger.info(method_tag + ": " + "cr_create done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(create_result.config.config.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(create_args.cert.cert.verifier));
            send_ss_create(convert(create_args.cert, ss_signed_certificate.class), convert(create_result.config, ss_signed_ss_set_config.class));
        }
    };

    private void send_ss_create(ss_signed_certificate cert, ss_signed_ss_set_config config) {
        final String method_tag = tag + ".send_ss_create";
        logger.info(method_tag + ": " + "Calling ss_create procedure...");
        ss_create_args create_args = new ss_create_args();
        create_args.cert = cert;
        create_args.config = config;
        int index = _rand.nextInt(config.config.ss_set_size);
        NodeId ssGway = addressToNodeId(config.config.ss_set[index].addr);
        dispatch(ssGway, create_args, getProcedureKey(Procedure.SS_CREATE), create_ss_done);
    }

    private Thunk4<Long, Long, RpcCall, RpcReply> create_ss_done = new Thunk4<Long, Long, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".create_ss_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            ss_create_args create_args = (ss_create_args) call.args;
            ss_create_result create_result = (ss_create_result) res.reply;
            if (create_result.status != cr_status.STATUS_OK) BUG(method_tag + ": " + "returned error.");
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + create_args + ", but is not.";
            logger.info(method_tag + ": " + "ss_create done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(create_result.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(create_result.verifier));
            send_get_certificate_req(convert(create_args.config.config.extent_key, gw_guid.class), convert(create_args.config.config.client_id, gw_guid.class), start_create_after_get_cert_done);
        }
    };

    private Thunk4<Long, Long, RpcCall, RpcReply> start_create_after_get_cert_done = new Thunk4<Long, Long, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".start_create_after_get_cert_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_get_certificate_args get_cert_args = (gw_get_certificate_args) call.args;
            gw_get_certificate_result get_cert_result = (gw_get_certificate_result) res.reply;
            if (get_cert_result.cert.cert.public_key.value.length == 0 || get_cert_result.latest_config.config.public_key.value.length == 0 || (get_cert_result.status != gw_status.GW_STATUS_OK && !(verifySignature(get_cert_result.latest_config.config, get_cert_result.latest_config.signature, get_cert_result.latest_config.config.public_key, _key_factory, _verify_engine) && verifySignature(get_cert_result.cert.cert, get_cert_result.cert.signature, get_cert_result.cert.cert.public_key, _key_factory, _verify_engine)))) BUG(method_tag + ": " + "returned error.");
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + get_cert_args + ", but is not.";
            logger.info(method_tag + ": " + "get_cert done in " + ((now_us() - start_time_us) / 1000.0) + "ms. " + " Retrieved " + XdrUtils.toString(get_cert_result.cert));
            send_create(get_cert_result.cert);
        }
    };

    private void send_append(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext) {
        final String method_tag = tag + ".send_append";
        logger.info(method_tag + ": " + "Calling append procedure...");
        _num_append_operations++;
        gw_data_block[] append_blocks = createDataBlocks(0, 300, 1024);
        if (_append_blocks == null) _append_blocks = append_blocks; else {
            gw_data_block[] append_blocks2 = new gw_data_block[_append_blocks.length + append_blocks.length];
            for (int i = 0; i < append_blocks2.length; i++) {
                append_blocks2[i] = (i < _append_blocks.length ? _append_blocks[i] : append_blocks[i - _append_blocks.length]);
            }
            _append_blocks = append_blocks2;
        }
        gw_append_args append_args = createAppendArgs(ext, append_blocks);
        dispatch(gway, append_args, getProcedureKey(Procedure.GW_APPEND), curry(append_done, ext));
    }

    private Thunk5<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply> append_done = new Thunk5<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply>() {

        public void run(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".append_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_append_args append_args = (gw_append_args) call.args;
            gw_append_result append_result = (gw_append_result) res.reply;
            if (append_result.status != gw_status.GW_STATUS_OK) BUG(method_tag + ": " + "returned error.");
            if (!checkSoundnessProof(append_result.proof, true, false, _md, _key_factory, _verify_engine)) BUG(method_tag + ": check failed for " + XdrUtils.toString(append_result));
            gw_soundness_proof proof_old = _proofs.put(guidToBigInteger(append_result.extent_key), append_result.proof);
            assert proof_old != null : "old proof is null for key " + XdrUtils.toString(append_result.extent_key);
            _append_result = append_result;
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + append_args + ", but is not.";
            logger.info(method_tag + ": " + "append done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(append_result.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(append_result.verifier));
            if (_num_append_operations < 2) send_append(ext); else send_snapshot(ext);
        }
    };

    private void send_snapshot(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext) {
        final String method_tag = tag + ".send_snapshot";
        logger.info(method_tag + ": " + "Calling snapshot procedure...");
        gw_signed_certificate snapshot_cert = null;
        try {
            _snapshot_extent = (Extent<gw_public_key, gw_guid, gw_signed_certificate>) ext.clone();
            snapshot_cert = _snapshot_extent.createCertificate(GW_HASH_VERIFIED, _rand.nextInt(Integer.MAX_VALUE / 2), now_ms(), now_ms() + (long) MAX_TTL * 1000L, new byte[0], new byte[0]);
            snapshot_cert.signature = createSignature(snapshot_cert.cert, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        _snapshot_extent.setCertificate(snapshot_cert);
        _extents.put(guidToBigInteger(_snapshot_extent.getHashName()), _snapshot_extent);
        gw_snapshot_args snapshot_args = new gw_snapshot_args();
        snapshot_args.cert = snapshot_cert;
        snapshot_args.prev_verifier = ext.getHashName();
        dispatch(gway, snapshot_args, getProcedureKey(Procedure.GW_SNAPSHOT), curry(snapshot_done, ext, _snapshot_extent));
    }

    private Thunk6<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply> snapshot_done = new Thunk6<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply>() {

        public void run(Extent<gw_public_key, gw_guid, gw_signed_certificate> key_verified_ext, Extent<gw_public_key, gw_guid, gw_signed_certificate> hash_verified_ext, Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".snapshot_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_snapshot_args snapshot_args = (gw_snapshot_args) call.args;
            gw_snapshot_result snapshot_result = (gw_snapshot_result) res.reply;
            if (snapshot_result.status != gw_status.GW_STATUS_OK) BUG(method_tag + ": " + "returned error.");
            if (!checkSoundnessProof(snapshot_result.proof, true, false, _md, _key_factory, _verify_engine)) BUG(method_tag + ": check failed for " + XdrUtils.toString(snapshot_result));
            _proofs.put(guidToBigInteger(snapshot_result.verifier), snapshot_result.proof);
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + snapshot_args + ", but is not.";
            _snapshot_result = snapshot_result;
            logger.info(method_tag + ": " + "snapshot done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            logger.info(method_tag + ": " + "  ExtentKeyOld = " + XdrUtils.toString(snapshot_result.extent_key_old));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(snapshot_result.verifier));
            send_truncate(key_verified_ext);
        }
    };

    private void send_truncate(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext) {
        final String method_tag = tag + ".send_truncate";
        logger.info(method_tag + ": " + "Calling truncate procedure...");
        gw_signed_certificate prev_cert = ext.getCertificate();
        gw_guid prev_verifier = ext.getHashName();
        long now_ms = now_ms();
        long expire_time_ms = prev_cert.cert.expire_time;
        gw_signed_certificate truncate_cert = null;
        try {
            ext.truncate();
            truncate_cert = ext.createCertificate(GW_KEY_VERIFIED, prev_cert.cert.seq_num + 1L, now_ms, expire_time_ms, new byte[0], new byte[0]);
            truncate_cert.signature = createSignature(truncate_cert.cert, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        ext.setCertificate(truncate_cert);
        gw_truncate_args truncate_args = new gw_truncate_args();
        truncate_args.cert = truncate_cert;
        truncate_args.prev_verifier = prev_verifier;
        dispatch(gway, truncate_args, getProcedureKey(Procedure.GW_TRUNCATE), truncate_done);
    }

    private Thunk4<Long, Long, RpcCall, RpcReply> truncate_done = new Thunk4<Long, Long, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".truncate_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_truncate_args truncate_args = (gw_truncate_args) call.args;
            gw_truncate_result truncate_result = (gw_truncate_result) res.reply;
            if (truncate_result.status != gw_status.GW_STATUS_OK) BUG(method_tag + ": " + "returned error.");
            if (!checkSoundnessProof(truncate_result.proof, true, false, _md, _key_factory, _verify_engine)) BUG(method_tag + ": check failed for " + XdrUtils.toString(truncate_result));
            gw_soundness_proof proof_old = _proofs.put(guidToBigInteger(truncate_result.extent_key), truncate_result.proof);
            assert proof_old != null : "old proof is null for key " + XdrUtils.toString(truncate_result.extent_key);
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + truncate_args + ", but is not.";
            _truncate_result = truncate_result;
            logger.info(method_tag + ": " + "truncate done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(truncate_result.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(truncate_result.verifier));
            send_get_certificate_req(truncate_result.verifier, truncate_result.proof.config.config.client_id, get_cert_done);
        }
    };

    private void send_get_certificate_req(gw_guid extent_key, gw_guid client_id, Thunk4<Long, Long, RpcCall, RpcReply> cb) {
        final String method_tag = tag + ".send_get_certificate_req";
        logger.info(method_tag + ": " + "Calling get_certificate procedure...");
        gw_get_certificate_args get_cert_args = new gw_get_certificate_args();
        get_cert_args.extent_key = extent_key;
        get_cert_args.verbose = 1;
        get_cert_args.latest = 1;
        get_cert_args.use_admin = 1;
        get_cert_args.client_id = client_id;
        dispatch(gway, get_cert_args, getProcedureKey(Procedure.GW_GET_CERT), cb);
    }

    private Thunk4<Long, Long, RpcCall, RpcReply> get_cert_done = new Thunk4<Long, Long, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".get_cert_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_get_certificate_args get_cert_args = (gw_get_certificate_args) call.args;
            gw_get_certificate_result get_cert_result = (gw_get_certificate_result) res.reply;
            if (get_cert_result.status != gw_status.GW_STATUS_OK) BUG(method_tag + ": " + "returned error.");
            if (!checkSoundnessProof(get_cert_result.proof, true, false, _md, _key_factory, _verify_engine)) BUG(method_tag + ": check failed for " + XdrUtils.toString(get_cert_result));
            if (!XdrUtils.equals(get_cert_result.cert, get_cert_result.proof.cert)) BUG(method_tag + ": get_cert_result.cert and " + " get_cert_result.proof.cert not equal: " + XdrUtils.toString(get_cert_result));
            if (get_cert_args.verbose == 1) {
                gw_guid verifier = computeVerifier(get_cert_result.cert.cert.public_key, get_cert_result.block_names, true, _md);
                if (!XdrUtils.equals(verifier, get_cert_result.cert.cert.verifier)) BUG(method_tag + ": computed verifier " + XdrUtils.toString(verifier) + " not equals" + " get_cert_result.cert.cert.verifier " + XdrUtils.toString(get_cert_result.cert.cert.verifier) + ": " + XdrUtils.toString(get_cert_result));
            }
            gw_soundness_proof proof_old = _proofs.put(guidToBigInteger(get_cert_args.extent_key), get_cert_result.proof);
            assert proof_old != null : "old proof is null for key " + XdrUtils.toString(get_cert_args.extent_key);
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + get_cert_args + ", but is not.";
            logger.info(method_tag + ": " + "get_cert done in " + ((now_us() - start_time_us) / 1000.0) + "ms. " + " Retrieved " + XdrUtils.toString(get_cert_result.cert));
            send_get_extent_req(get_cert_args.extent_key);
        }
    };

    private void send_get_extent_req(gw_guid extent_key) {
        final String method_tag = tag + ".send_get_extent_req";
        logger.info(method_tag + ": " + "Calling get_extent procedure...");
        gw_get_extent_args get_extent_args = new gw_get_extent_args();
        get_extent_args.extent_key = extent_key;
        get_extent_args.predicate_verifier = AntiquityUtils.GW_NULL_GUID;
        dispatch(gway, get_extent_args, getProcedureKey(Procedure.GW_GET_EXTENT), get_extent_done);
    }

    private Thunk4<Long, Long, RpcCall, RpcReply> get_extent_done = new Thunk4<Long, Long, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".get_extent_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_get_extent_args get_extent_args = (gw_get_extent_args) call.args;
            gw_get_extent_result get_extent_result = (gw_get_extent_result) res.reply;
            if (get_extent_result.status != gw_status.GW_STATUS_OK) BUG(method_tag + ": " + "returned error.");
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + get_extent_args + ", but is not.";
            logger.info(method_tag + ": " + "get_extent done in " + ((now_us() - start_time_us) / 1000.0) + "ms. " + " Retrieved " + get_extent_result.data.length + " blocks from " + XdrUtils.toString(get_extent_result.cert));
            if (AntiquityUtils.equals(get_extent_args.extent_key, _snapshot_result.verifier)) send_get_blocks_req(get_extent_args.extent_key, _snapshot_extent.getBlockNames()); else send_get_certificate_req(_snapshot_result.verifier, _snapshot_result.proof.config.config.client_id, get_cert_done);
        }
    };

    private void send_get_blocks_req(gw_guid extent_key, gw_guid[] block_names) {
        final String method_tag = tag + ".send_get_blocks_req";
        logger.info(method_tag + ": " + "Calling get_blocks procedure...");
        gw_guid[] get_block_names = new gw_guid[block_names.length / 2];
        Set<Integer> block_name_indexes = new TreeSet<Integer>();
        while (block_name_indexes.size() < get_block_names.length) block_name_indexes.add(_rand.nextInt(block_names.length));
        int i = 0;
        for (Integer j : block_name_indexes) get_block_names[i++] = block_names[i];
        gw_get_blocks_args get_blocks_args = new gw_get_blocks_args();
        get_blocks_args.extent_key = extent_key;
        get_blocks_args.block_names = get_block_names;
        dispatch(gway, get_blocks_args, getProcedureKey(Procedure.GW_GET_BLOCKS), get_blocks_done);
    }

    private Thunk4<Long, Long, RpcCall, RpcReply> get_blocks_done = new Thunk4<Long, Long, RpcCall, RpcReply>() {

        public void run(Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".get_blocks_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_get_blocks_args get_blocks_args = (gw_get_blocks_args) call.args;
            gw_get_blocks_result get_blocks_result = (gw_get_blocks_result) res.reply;
            if (get_blocks_result.status != gw_status.GW_STATUS_OK) BUG(method_tag + ": " + "returned error.");
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + get_blocks_args + ", but is not.";
            logger.info(method_tag + ": " + "get_blocks done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            send_stop_config(0, 0, _create_extent);
        }
    };

    private void send_stop_config(int attempts, int renew_attempts, Extent<gw_public_key, gw_guid, gw_signed_certificate> ext) {
        final String method_tag = tag + ".send_stop_config";
        logger.info(method_tag + ": " + "Calling stop_config procedure w/ " + attempts + " attempts...");
        gw_guid extent_key = (ext.getCertificate().cert.type == GW_KEY_VERIFIED ? computeGuid(ext.getCertificate().cert.public_key, gw_guid.class, _md) : ext.getCertificate().cert.verifier);
        BigInteger extent_key_sha1 = guidToBigInteger(extent_key);
        gw_stop_config_args stop_config_args = new gw_stop_config_args();
        gw_soundness_proof proof = _proofs.get(extent_key_sha1);
        stop_config_args.latest_config = proof.config;
        try {
            stop_config_args.sig = new gw_signed_signature();
            stop_config_args.sig.public_key = _ant_pkey;
            _md.update(XdrUtils.serialize(stop_config_args.latest_config.config));
            stop_config_args.sig.datahash = _md.digest();
            stop_config_args.sig.signature = createSignature(stop_config_args.sig.datahash, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        dispatch(gway, stop_config_args, getProcedureKey(Procedure.GW_STOP_CONFIG), curry(stop_config_done, attempts, renew_attempts, ext));
    }

    private Thunk7<Integer, Integer, Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply> stop_config_done = new Thunk7<Integer, Integer, Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply>() {

        public void run(Integer attempts, Integer renew_attempts, Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".stop_config_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_stop_config_args stop_config_args = (gw_stop_config_args) call.args;
            gw_stop_config_result stop_config_result = (gw_stop_config_result) res.reply;
            if (stop_config_result.status != gw_status.GW_STATUS_OK) {
                if (attempts < MAX_RENEW_ATTEMPTS) {
                    send_get_certificate_req(stop_config_args.latest_config.config.extent_key, stop_config_args.latest_config.config.client_id, curry(repeat_stop_config_after_get_cert_done, attempts + 1, renew_attempts, ext));
                    return;
                } else BUG(method_tag + ": " + "returned error.");
            }
            cr_repair_args cr_repair = convert(stop_config_result, cr_repair_args.class);
            gw_soundness_proof latest_proof = null;
            try {
                RpcCall cr_repair_req = new RpcCall(my_node_id, getProcedureKey(Procedure.CR_REPAIR), cr_repair, appId, null, my_sink);
                ValuePair<cr_guid, cr_soundness_proof> pair = StorageSetCreator.checkReq(logger, cr_repair_req, now_us(), _md, _key_factory, _verify_engine);
                latest_proof = convert(pair.getSecond(), gw_soundness_proof.class);
            } catch (Throwable e) {
                BUG(method_tag + ": check failed. " + e + " for " + XdrUtils.toString(stop_config_result));
            }
            gw_soundness_proof proof_old = _proofs.put(guidToBigInteger(latest_proof.config.config.extent_key), latest_proof);
            assert proof_old != null : "old proof is null for key " + XdrUtils.toString(latest_proof.config.config.extent_key);
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + stop_config_args + ", but is not.";
            logger.info(method_tag + ": " + "stop_config done in " + ((now_us() - start_time_us) / 1000.0) + "ms and " + attempts + " attempts.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(latest_proof.config.config.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(latest_proof.cert.cert.verifier));
            gw_renew_args renew = convert(cr_repair, gw_renew_args.class);
            renew.cert = createRenewCert(ext);
            send_renew(renew_attempts, renew, ext);
        }
    };

    private Thunk7<Integer, Integer, Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply> repeat_stop_config_after_get_cert_done = new Thunk7<Integer, Integer, Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply>() {

        public void run(Integer attempts, Integer renew_attempts, Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".repeat_stop_config_after_get_cert_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_get_certificate_args get_cert_args = (gw_get_certificate_args) call.args;
            gw_get_certificate_result get_cert_result = (gw_get_certificate_result) res.reply;
            if (get_cert_result.cert.cert.public_key.value.length == 0 || get_cert_result.latest_config.config.public_key.value.length == 0 || (get_cert_result.status != gw_status.GW_STATUS_OK && !(verifySignature(get_cert_result.latest_config.config, get_cert_result.latest_config.signature, get_cert_result.latest_config.config.public_key, _key_factory, _verify_engine) && verifySignature(get_cert_result.cert.cert, get_cert_result.cert.signature, get_cert_result.cert.cert.public_key, _key_factory, _verify_engine)))) BUG(method_tag + ": " + "returned error.");
            if (!checkSoundnessProof(get_cert_result.proof, true, false, _md, _key_factory, _verify_engine)) BUG(method_tag + ": check failed for " + XdrUtils.toString(get_cert_result));
            if (!XdrUtils.equals(get_cert_result.cert, get_cert_result.proof.cert)) BUG(method_tag + ": get_cert_result.cert and " + " get_cert_result.proof.cert not equal: " + XdrUtils.toString(get_cert_result));
            if (get_cert_args.verbose == 1) {
                gw_guid verifier = computeVerifier(get_cert_result.cert.cert.public_key, get_cert_result.block_names, true, _md);
                if (!XdrUtils.equals(verifier, get_cert_result.cert.cert.verifier)) BUG(method_tag + ": computed verifier " + XdrUtils.toString(verifier) + " not equals" + " get_cert_result.cert.cert.verifier " + XdrUtils.toString(get_cert_result.cert.cert.verifier) + ": " + XdrUtils.toString(get_cert_result));
            }
            gw_soundness_proof proof_old = _proofs.put(guidToBigInteger(get_cert_args.extent_key), get_cert_result.proof);
            assert proof_old != null : "old proof is null for key " + XdrUtils.toString(get_cert_args.extent_key);
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + get_cert_args + ", but is not.";
            logger.info(method_tag + ": " + "get_cert done in " + ((now_us() - start_time_us) / 1000.0) + "ms. " + " Retrieved " + XdrUtils.toString(get_cert_result.cert));
            send_stop_config(attempts, renew_attempts, ext);
        }
    };

    private void send_renew(int attempts, gw_renew_args renew_args, Extent<gw_public_key, gw_guid, gw_signed_certificate> ext) {
        final String method_tag = tag + ".send_renew";
        logger.info(method_tag + ": " + "Calling renew procedure w/ " + attempts + " attempts...");
        dispatch(gway, renew_args, getProcedureKey(Procedure.GW_RENEW), curry(renew_done, attempts, ext));
    }

    private Thunk6<Integer, Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply> renew_done = new Thunk6<Integer, Extent<gw_public_key, gw_guid, gw_signed_certificate>, Long, Long, RpcCall, RpcReply>() {

        public void run(Integer attempts, Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".renew_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            gw_renew_args renew_args = (gw_renew_args) call.args;
            gw_renew_result renew_result = (gw_renew_result) res.reply;
            if (renew_result.status != gw_status.GW_STATUS_OK) {
                if (attempts < MAX_RENEW_ATTEMPTS) {
                    send_get_certificate_req(renew_args.latest_config.config.extent_key, renew_args.latest_config.config.client_id, curry(repeat_stop_config_after_get_cert_done, 1, attempts + 1, ext));
                    return;
                } else BUG(method_tag + ": " + "returned error.");
            }
            if (!checkSoundnessProof(renew_result.proof, true, false, _md, _key_factory, _verify_engine)) BUG(method_tag + ": check failed for " + XdrUtils.toString(renew_result));
            gw_soundness_proof proof_old = _proofs.put(guidToBigInteger(renew_result.proof.config.config.extent_key), renew_result.proof);
            assert proof_old != null : "old proof is null for key " + XdrUtils.toString(renew_result.proof.config.config.extent_key);
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + renew_args + ", but is not.";
            logger.info(method_tag + ": " + "renew done in " + ((now_us() - start_time_us) / 1000.0) + "ms and " + attempts + " attempts.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(renew_result.proof.config.config.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(renew_result.proof.cert.cert.verifier));
            if (renew_args.cert.cert.type == GW_KEY_VERIFIED) {
                send_stop_config(0, 0, _snapshot_extent);
            } else {
                if (TRIGGER_REPAIR) {
                    Extent<gw_public_key, gw_guid, gw_signed_certificate> ext2 = null;
                    gw_guid extent_key = null;
                    if (_firstRepair) {
                        ext2 = _create_extent;
                        extent_key = computeGuid(ext2.getPublicKey(), gw_guid.class, _md);
                        _firstRepair = false;
                    } else {
                        ext2 = _snapshot_extent;
                        extent_key = _snapshot_extent.getHashName();
                        TRIGGER_REPAIR = false;
                        _firstRepair = true;
                    }
                    BigInteger extent_key_sha1 = guidToBigInteger(extent_key);
                    gw_soundness_proof proof = _proofs.get(extent_key_sha1);
                    assert proof != null : "proof is null for key 0x" + GuidTools.guid_to_string(extent_key_sha1);
                    create_deadlock(ext2, proof);
                } else {
                    logger.info("All procedures passed!  Exitting...");
                    System.exit(0);
                }
            }
        }
    };

    public void create_deadlock(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, gw_soundness_proof proof) {
        final String method_tag = tag + ".create_deadlock";
        logger.info(method_tag + ": " + "Calling create deadlock for " + (proof.cert.cert.type == GW_KEY_VERIFIED ? "KEY_VERIFIED" : "HASH_VERIFIED") + " extent " + XdrUtils.toString(proof.config.config.extent_key) + ". firstRepair=" + _firstRepair + " TRIGGER_REPAIR=" + TRIGGER_REPAIR);
        gw_signed_certificate cert = ext.getCertificate();
        int f_plus_1 = proof.config.config.ss_set_size - proof.config.config.threshold + 1;
        Set<NodeId> ssGways = new LinkedHashSet<NodeId>();
        while (ssGways.size() < (f_plus_1 + 1)) {
            int index = _rand.nextInt(proof.config.config.ss_set_size);
            NodeId ss = addressToNodeId(proof.config.config.ss_set[index].addr);
            if (!ssGways.contains(ss)) ssGways.add(ss);
        }
        Set<NodeId> doneSSGways = new LinkedHashSet<NodeId>();
        long cert_seq_num = proof.cert.cert.seq_num;
        long config_view = proof.config.config.seq_num;
        for (NodeId ss : ssGways) {
            if (cert.cert.type == GW_KEY_VERIFIED) {
                send_ss_append(ss, ext, proof, ssGways, doneSSGways);
            } else {
                send_ss_stop_config(ss, ext, proof, ssGways, doneSSGways);
            }
        }
    }

    private void send_ss_append(NodeId ssGway, Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, gw_soundness_proof proof, Set<NodeId> ssGways, Set<NodeId> doneSSGways) {
        final String method_tag = tag + ".send_ss_append";
        logger.info(method_tag + ": " + "Calling ss_append for extent " + XdrUtils.toString(proof.config.config.extent_key));
        Extent<gw_public_key, gw_guid, gw_signed_certificate> cloned_ext2 = null;
        try {
            cloned_ext2 = (Extent<gw_public_key, gw_guid, gw_signed_certificate>) ext.clone();
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        Extent<gw_public_key, gw_guid, gw_signed_certificate> cloned_ext = cloned_ext2;
        gw_data_block[] append_blocks = createDataBlocks(_rand.nextInt(256), 300, 1024);
        gw_append_args append_args = createAppendArgs(cloned_ext, append_blocks);
        ss_append_args append_args_ss = (ss_append_args) GatewayHelper.getSSArgs(append_args, (gw_signed_ss_set_config) null, (gw_signed_ss_set_config) null, proof, getProcedureKey(Procedure.GW_APPEND));
        dispatch(ssGway, append_args_ss, getProcedureKey(Procedure.SS_APPEND), curry(ss_append_done, cloned_ext, ssGways, doneSSGways));
    }

    private Thunk7<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Set<NodeId>, Set<NodeId>, Long, Long, RpcCall, RpcReply> ss_append_done = new Thunk7<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Set<NodeId>, Set<NodeId>, Long, Long, RpcCall, RpcReply>() {

        public void run(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, Set<NodeId> ssGways, Set<NodeId> doneSSGways, Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".ss_append_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            ss_append_args append_args = (ss_append_args) call.args;
            ss_append_result append_result = (ss_append_result) res.reply;
            if (append_result.status != ss_status.STATUS_OK) BUG(method_tag + ": " + "returned error.");
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + append_args + ", but is not.";
            logger.info(method_tag + ": " + "ss_append done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(append_result.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(append_result.verifier));
            doneSSGways.add(res.peer);
            if (ssGways.size() == doneSSGways.size()) {
                deadlock_done(ext, append_args.piggyback_prev_proof);
            }
        }
    };

    private void send_ss_stop_config(NodeId ssGway, Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, gw_soundness_proof proof, Set<NodeId> ssGways, Set<NodeId> doneSSGways) {
        final String method_tag = tag + ".send_ss_stop_config";
        logger.info(method_tag + ": " + "Calling ss_stop_config for extent " + XdrUtils.toString(proof.config.config.extent_key));
        Extent<gw_public_key, gw_guid, gw_signed_certificate> cloned_ext2 = null;
        try {
            cloned_ext2 = (Extent<gw_public_key, gw_guid, gw_signed_certificate>) ext.clone();
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        Extent<gw_public_key, gw_guid, gw_signed_certificate> cloned_ext = cloned_ext2;
        gw_stop_config_args stop_config_args = new gw_stop_config_args();
        stop_config_args.latest_config = proof.config;
        try {
            stop_config_args.sig = new gw_signed_signature();
            stop_config_args.sig.public_key = _ant_pkey;
            _md.update(XdrUtils.serialize(stop_config_args.latest_config.config));
            stop_config_args.sig.datahash = _md.digest();
            stop_config_args.sig.signature = createSignature(stop_config_args.sig.datahash, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        ss_stop_config_args stop_config_args_ss = (ss_stop_config_args) GatewayHelper.getSSArgs(stop_config_args, (gw_signed_ss_set_config) null, (gw_signed_ss_set_config) null, proof, getProcedureKey(Procedure.GW_STOP_CONFIG));
        dispatch(ssGway, stop_config_args_ss, getProcedureKey(Procedure.SS_STOP_CONFIG), curry(ss_stop_config_done, cloned_ext, ssGways, doneSSGways));
    }

    private Thunk7<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Set<NodeId>, Set<NodeId>, Long, Long, RpcCall, RpcReply> ss_stop_config_done = new Thunk7<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Set<NodeId>, Set<NodeId>, Long, Long, RpcCall, RpcReply>() {

        public void run(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, Set<NodeId> ssGways, Set<NodeId> doneSSGways, Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".ss_stop_config_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            ss_stop_config_args stop_config_args = (ss_stop_config_args) call.args;
            ss_stop_config_result stop_config_result = (ss_stop_config_result) res.reply;
            if (stop_config_result.status != ss_status.STATUS_OK) BUG(method_tag + ": " + "returned error.");
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + stop_config_args + ", but is not.";
            logger.info(method_tag + ": " + "ss_stop_config done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(stop_config_args.latest_config.config.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(stop_config_args.piggyback_prev_proof.cert.cert.verifier));
            doneSSGways.add(res.peer);
            if (ssGways.size() == doneSSGways.size()) {
                deadlock_done(ext, stop_config_args.piggyback_prev_proof);
            }
        }
    };

    private void send_ss_renew(NodeId ssGway, Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, long cert_seq_num, long config_view, gw_soundness_proof proof, Set<NodeId> ssGways, Set<NodeId> doneSSGways) {
        final String method_tag = tag + ".send_ss_renew";
        logger.info(method_tag + ": " + "Calling ss_renew procedure...");
        Extent<gw_public_key, gw_guid, gw_signed_certificate> cloned_ext2 = null;
        try {
            cloned_ext2 = (Extent<gw_public_key, gw_guid, gw_signed_certificate>) ext.clone();
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        Extent<gw_public_key, gw_guid, gw_signed_certificate> cloned_ext = cloned_ext2;
        cloned_ext2.getCertificate().cert.seq_num = cert_seq_num;
        gw_renew_args renew_args = createRenewArgs(cloned_ext);
        gw_signed_certificate renew_cert = renew_args.cert;
        long now_ms = now_ms();
        long expire_time_ms = renew_cert.cert.expire_time;
        gw_signed_ss_set_config renew_config = null;
        try {
            renew_config = XdrUtils.clone(proof.config);
            renew_config.config.public_key = _ant_pkey;
            renew_config.config.seq_num = config_view;
            renew_config.config.timestamp = now_ms;
            renew_config.config.expire_time = expire_time_ms;
            renew_config.signature = createSignature(renew_config.config, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        ss_renew_args renew_args_ss = (ss_renew_args) GatewayHelper.getSSArgs(renew_args, renew_config, (gw_signed_ss_set_config) null, proof, getProcedureKey(Procedure.GW_RENEW));
        dispatch(ssGway, renew_args_ss, getProcedureKey(Procedure.SS_RENEW), curry(ss_renew_done, cloned_ext, ssGways, doneSSGways));
    }

    private Thunk7<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Set<NodeId>, Set<NodeId>, Long, Long, RpcCall, RpcReply> ss_renew_done = new Thunk7<Extent<gw_public_key, gw_guid, gw_signed_certificate>, Set<NodeId>, Set<NodeId>, Long, Long, RpcCall, RpcReply>() {

        public void run(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, Set<NodeId> ssGways, Set<NodeId> doneSSGways, Long start_time_us, Long seq, RpcCall call, RpcReply res) {
            final String method_tag = tag + ".ss_renew_done.call";
            if (!((res.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (res.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (res.auth_accepted == RpcReply.AuthStat.AUTH_OK))) BUG(method_tag + ": " + " rpc stage returned error. " + " reply=" + res + ".  call=" + call);
            ss_renew_args renew_args = (ss_renew_args) call.args;
            ss_renew_result renew_result = (ss_renew_result) res.reply;
            if (renew_result.status != ss_status.STATUS_OK) BUG(method_tag + ": " + "returned error.");
            assert call == (RpcCall) _pending_events.remove(seq) : "Event seq=" + seq + " should be " + renew_args + ", but is not.";
            logger.info(method_tag + ": " + "ss_renew done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            logger.info(method_tag + ": " + "  ExtentKey = " + XdrUtils.toString(renew_args.prev_proof.config.config.extent_key));
            logger.info(method_tag + ": " + ",  Verifier = " + XdrUtils.toString(renew_args.prev_proof.cert.cert.verifier));
            doneSSGways.add(res.peer);
            if (ssGways.size() == doneSSGways.size()) {
                deadlock_done(ext, renew_args.prev_proof);
            }
        }
    };

    private void deadlock_done(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, ss_soundness_proof proof) {
        final String method_tag = tag + ".deadlock_done";
        logger.info(method_tag + ": " + "Calling deadlock done for extent " + XdrUtils.toString(proof.config.config.extent_key));
        send_dd_repair_req(ext, proof);
    }

    private void send_dd_repair_req(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, ss_soundness_proof proof) {
        final String method_tag = tag + ".send_dd_repair_req";
        logger.info(method_tag + ": " + "Calling dd repair procedure for extent " + XdrUtils.toString(proof.config.config.extent_key));
        SecureHash objguid = guidToSecureHash(proof.config.config.extent_key);
        DDThresholdTag tag = new Tag((short) (proof.config.config.threshold - 1));
        QSTreeSet remaining = new QSTreeSet();
        QSTreeSet dead = new QSTreeSet();
        Set<SecureHash> ssGuids = new LinkedHashSet<SecureHash>();
        for (ss_id id : proof.config.config.ss_set) {
            SecureHash guid = guidToSecureHash(id.guid);
            ssGuids.add(guid);
            remaining.add(guid);
        }
        Set<SecureHash> doneSSGuids = new LinkedHashSet<SecureHash>();
        ValueTriple<Set<SecureHash>, Extent<gw_public_key, gw_guid, gw_signed_certificate>, ss_soundness_proof> triple = new ValueTriple<Set<SecureHash>, Extent<gw_public_key, gw_guid, gw_signed_certificate>, ss_soundness_proof>(doneSSGuids, ext, proof);
        _ddRepairs.put(objguid, triple);
        Long start_time_us = now_us();
        for (SecureHash ss : ssGuids) {
            DDRepairObjReq req = new DDRepairObjReq(ss, objguid, tag, remaining, dead, true, true, false);
            Long seq = new Long(_next_seq++);
            _pending_events_dd.put(seq, req);
            _acore.register_timer(TIMEOUT_MS, curry(timeout_cb, seq));
            ValuePair<Long, Long> state = new ValuePair<Long, Long>(seq, start_time_us);
            _ddRepairTokens.put(ss, state);
            dispatch(req);
        }
    }

    private void handleDDRepairObjResp(DDRepairObjResp resp) {
        final String method_tag = tag + ".handleDDRepairObjResp";
        logger.info(method_tag + ": " + "called " + resp);
        Long start_time_us = null;
        Long seq = null;
        if (!_ddRepairTokens.containsKey(resp.peer)) {
            if (logger.isInfoEnabled()) logger.info(method_tag + ": already received response from " + resp.peer + " for extent " + resp.getObjguid());
            return;
        }
        ValuePair<Long, Long> pair = _ddRepairTokens.remove(resp.peer);
        seq = pair.getFirst();
        start_time_us = pair.getSecond();
        logger.info(method_tag + ": for " + resp.getObjguid() + " received response from " + resp.peer + " in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
        assert _pending_events_dd.remove(seq) != null;
        if (_ddRepairs.containsKey(resp.getObjguid())) {
            ValueTriple<Set<SecureHash>, Extent<gw_public_key, gw_guid, gw_signed_certificate>, ss_soundness_proof> triple = _ddRepairs.get(resp.getObjguid());
            Set<SecureHash> doneSSGuids = triple.getFirst();
            Extent<gw_public_key, gw_guid, gw_signed_certificate> ext = triple.getSecond();
            ss_soundness_proof proof = triple.getThird();
            if (!doneSSGuids.contains(resp.peer)) {
                doneSSGuids.add(resp.peer);
                int f_plus_1 = proof.config.config.ss_set_size - proof.config.config.threshold + 1;
                if (doneSSGuids.size() >= (f_plus_1 + 1)) {
                    logger.info(method_tag + ": dd repair done in " + ((now_us() - start_time_us) / 1000.0) + "ms.");
                    logger.info(method_tag + ":   ExtentKey = " + resp.getObjguid());
                    logger.info(method_tag + ": doneSSGuid.size=" + doneSSGuids.size() + " >= ((f+1)+1)=" + (f_plus_1 + 1));
                    _ddRepairs.remove(resp.getObjguid());
                    gw_guid guid = secureHashToGuid(resp.getObjguid(), gw_guid.class);
                    send_get_certificate_req(guid, convert(proof.config.config.client_id, gw_guid.class), get_cert_done);
                }
            }
        }
    }

    private Thunk1<Long> timeout_cb = new Thunk1<Long>() {

        public void run(Long seq) {
            final String method_tag = tag + ".timeout_cb.call";
            RpcCall call = _pending_events.remove(seq);
            DDRepairObjReq repair = _pending_events_dd.remove(seq);
            if (call != null) {
                BUG("timed out on call " + call);
            } else if (repair != null) {
                if (_ddRepairs.containsKey(repair.getObjguid())) BUG("timed out on repair of extent " + repair.getObjguid() + ". repair=" + repair); else {
                    logger.warn("peer=" + repair.peer + " did not return repair resp for obj " + repair.getObjguid() + ". repair=" + repair);
                    assert _ddRepairTokens.remove(repair.peer) != null;
                }
            }
        }
    };

    /**********************************************************************/
    private gw_data_block[] createDataBlocks(int number_offset, int number_length, int size) {
        gw_data_block[] blocks = new gw_data_block[number_length];
        for (int i = 0; i < number_length; ++i) {
            blocks[i] = createDataBlock(size, (byte) ('a' + (i + number_offset)));
        }
        return blocks;
    }

    private gw_guid getBlockName(gw_data_block block) {
        gw_guid block_name = new gw_guid();
        SecureHash block_hash = new SHA1Hash(block.value);
        block_name.value = block_hash.bytes();
        return block_name;
    }

    private gw_data_block createDataBlock(int size, byte val) {
        byte[] data = new byte[size];
        Arrays.fill(data, val);
        gw_data_block block = new gw_data_block();
        block.value = data;
        return block;
    }

    private gw_signed_certificate createCreateCert(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext) {
        final String method_tag = tag + ".createCreateCert";
        gw_signed_certificate create_cert = null;
        try {
            create_cert = ext.createCertificate(GW_KEY_VERIFIED, _rand.nextInt(Integer.MAX_VALUE / 2), now_ms(), now_ms() + (long) MAX_TTL * 1000L, new byte[0], new byte[0]);
            create_cert.signature = createSignature(create_cert.cert, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        ext.setCertificate(create_cert);
        return create_cert;
    }

    private gw_create_args createCreateArgs(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext) {
        final String method_tag = tag + ".createCreateArgs";
        gw_create_args create_args = new gw_create_args();
        create_args.cert = createCreateCert(ext);
        return create_args;
    }

    private gw_append_args createAppendArgs(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext, gw_data_block[] append_blocks) {
        final String method_tag = tag + ".createAppendArgs";
        gw_signed_certificate prev_cert = ext.getCertificate();
        gw_guid prev_verifier = ext.getHashName();
        ext.appendBlocks(append_blocks);
        long now_ms = now_ms();
        long expire_time_ms = prev_cert.cert.expire_time;
        gw_signed_certificate append_cert = null;
        try {
            append_cert = ext.createCertificate(GW_KEY_VERIFIED, prev_cert.cert.seq_num + 1L, now_ms, expire_time_ms, new byte[0], new byte[0]);
            append_cert.signature = createSignature(append_cert.cert, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        ext.setCertificate(append_cert);
        gw_append_args append_args = new gw_append_args();
        append_args.cert = append_cert;
        append_args.data = append_blocks;
        append_args.prev_verifier = prev_verifier;
        return append_args;
    }

    private gw_signed_certificate createRenewCert(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext) {
        final String method_tag = tag + ".createRenewCert";
        gw_signed_certificate prev_cert = ext.getCertificate();
        gw_signed_certificate new_cert = null;
        try {
            new_cert = XdrUtils.clone(prev_cert);
            new_cert.cert.seq_num++;
            new_cert.cert.timestamp = now_ms();
            new_cert.cert.expire_time = now_ms() + (long) MAX_TTL * 1000L;
            new_cert.signature = createSignature(new_cert.cert, _skey, _sig_engine);
        } catch (Exception e) {
            BUG(method_tag + ": " + e);
        }
        ext.setCertificate(new_cert);
        return new_cert;
    }

    private gw_renew_args createRenewArgs(Extent<gw_public_key, gw_guid, gw_signed_certificate> ext) {
        final String method_tag = tag + ".createRenewArgs";
        gw_renew_args renew_args = new gw_renew_args();
        renew_args.cert = createRenewCert(ext);
        return renew_args;
    }

    private void dispatch(NodeId gateway, XdrAble args, ProcKey proc, Thunk4<Long, Long, RpcCall, RpcReply> cb) {
        Long seq = new Long(_next_seq++);
        RpcCall req = new RpcCall(gateway, proc, args, appId, null, my_sink);
        req.cb = curry(cb, now_us(), seq);
        _pending_events.put(seq, req);
        _acore.register_timer(TIMEOUT_MS, curry(timeout_cb, seq));
        dispatch(req);
    }
}
