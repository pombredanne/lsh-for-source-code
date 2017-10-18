package antiquity.ss.impl;

import antiquity.util.XdrUtils;
import static antiquity.util.XdrUtils.serialize;
import antiquity.util.AntiquityUtils;
import static antiquity.util.AntiquityUtils.convert;
import static antiquity.util.AntiquityUtils.Procedure;
import static antiquity.util.AntiquityUtils.getSSIndex;
import static antiquity.util.AntiquityUtils.readPublicKey;
import static antiquity.util.AntiquityUtils.readPrivateKey;
import static antiquity.util.AntiquityUtils.createSignature;
import static antiquity.util.AntiquityUtils.verifySignature;
import static antiquity.util.AntiquityUtils.getProcedureKey;
import static antiquity.util.AntiquityUtils.guidToSecureHash;
import static antiquity.util.AntiquityUtils.bigIntegerToGuid;
import static antiquity.util.AntiquityUtils.guidToBigInteger;
import static antiquity.util.AntiquityUtils.num_bytes_to_print;
import static antiquity.util.AntiquityUtils.checkSoundnessProof;
import static antiquity.util.AntiquityUtils.SS_NULL_SOUNDNESS_PROOF;
import static antiquity.util.AntiquityUtils.SS_NULL_SIGNED_CERT;
import static antiquity.util.AntiquityUtils.SS_NULL_SIGNED_CONFIG;
import static antiquity.util.AntiquityUtils.SS_NULL_SIGNED_SIG;
import static antiquity.util.AntiquityUtils.SS_NULL_GUID;
import static antiquity.util.AntiquityUtils.GW_NULL_SOUNDNESS_PROOF;
import static antiquity.util.AntiquityUtils.GW_NULL_SIGNED_CONFIG;
import static antiquity.util.AntiquityUtils.GW_NULL_SIGNED_SIG;
import static antiquity.util.AntiquityUtils.GW_NULL_GUID;
import static antiquity.ss.impl.StorageServer.ProofState;
import static antiquity.ss.impl.DbHelper.DbError;
import static antiquity.ss.impl.DbHelper.inspectPrevProof;
import static antiquity.ss.impl.DbHelper.deserializeHeadBlock;
import static antiquity.ss.impl.DbHelper.db_get_cert_and_blocks;
import antiquity.ss.api.ss_guid;
import antiquity.ss.api.ss_status;
import antiquity.ss.api.ss_log_entry;
import antiquity.ss.api.ss_public_key;
import antiquity.ss.api.ss_repair_args;
import antiquity.ss.api.ss_repair_result;
import antiquity.ss.api.ss_repair_audit_args;
import antiquity.ss.api.ss_repair_audit_result;
import antiquity.ss.api.ss_stop_config_args;
import antiquity.ss.api.ss_stop_config_result;
import antiquity.ss.api.ss_soundness_proof;
import antiquity.ss.api.ss_signed_signature;
import antiquity.ss.api.ss_signed_certificate;
import antiquity.ss.api.ss_get_certificate_args;
import antiquity.ss.api.ss_get_certificate_result;
import antiquity.ss.api.ss_signed_ss_set_config;
import antiquity.gw.api.gw_id;
import antiquity.gw.api.gw_guid;
import antiquity.gw.api.gw_status;
import antiquity.gw.api.gw_log_entry;
import antiquity.gw.api.gw_public_key;
import antiquity.gw.api.gw_soundness_proof;
import antiquity.gw.api.gw_signed_signature;
import antiquity.gw.api.gw_signed_certificate;
import antiquity.gw.api.gw_signed_ss_set_config;
import antiquity.gw.api.gw_repair_args;
import antiquity.gw.api.gw_repair_result;
import antiquity.gw.api.gw_get_certificate_args;
import antiquity.gw.api.gw_get_certificate_result;
import antiquity.gw.impl.SoundnessProofTag;
import antiquity.gw.impl.GatewayHelper;
import static antiquity.gw.impl.GatewayHelper.isInconsistentState;
import antiquity.rpc.api.RpcKey;
import antiquity.rpc.api.RpcCall;
import antiquity.rpc.api.RpcReply;
import antiquity.rpc.api.RpcRegisterReq;
import antiquity.rpc.api.RpcRegisterResp;
import static antiquity.rpc.api.ProcInfo.ProcKey;
import static antiquity.rpc.api.ProcInfo.ProcValue;
import antiquity.rpc.impl.RpcServerStage;
import ostore.db.api.DbKey;
import ostore.db.api.DbIterateByGuidReq;
import ostore.db.api.DbIterateByGuidResp;
import ostore.db.impl.GenericStorageManager;
import ostore.util.NodeId;
import ostore.util.ByteUtils;
import ostore.util.SecureHash;
import ostore.util.CountBuffer;
import ostore.util.StandardStage;
import ostore.dispatch.Filter;
import dd.api.DDReadyMsg;
import dd.api.DDPublishReq;
import dd.api.DDPublishResp;
import dd.api.DDRepairObjReq;
import dd.api.DDRepairObjResp;
import bamboo.util.GuidTools;
import bamboo.lss.ASyncCore;
import static bamboo.util.Curry.curry;
import static bamboo.util.Curry.Thunk1;
import static bamboo.util.Curry.Thunk2;
import static bamboo.util.Curry.Thunk3;
import static bamboo.util.Curry.Thunk4;
import static bamboo.util.Curry.Thunk5;
import org.acplt.oncrpc.XdrLong;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.XdrVoid;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.EventHandlerIF;
import seda.sandStorm.api.EventHandlerException;
import seda.sandStorm.api.StagesInitializedSignal;
import java.util.Set;
import java.util.Map;
import java.util.Arrays;
import java.util.Vector;
import java.util.Random;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.math.BigInteger;
import java.lang.reflect.Field;
import java.security.Signature;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.MessageDigest;
import org.apache.log4j.Logger;

/**
 * The <code>RepairStage</code> <i>triggers</i> 
 * {@link antiquity.ss.api.gw_repair_args repair} after receiving a 
 * {@link dd.api.DDRepairObjReq}.
 *
 * @author Hakim Weatherspoon
 * @version $Id: RepairStage.java,v 1.1.1.1 2006/12/15 22:45:38 hweather Exp $
 */
public class RepairStage extends ostore.util.StandardStage {

    /** Unique identifier for <code>this</code> stage. */
    private static final long appId = bamboo.router.Router.app_id(RepairStage.class);

    private static Map<NodeId, RepairStage> _instances;

    private Thread _my_thread;

    /** timeouts. */
    private long REPAIR_TIMEOUT, RECLAIM_TIMEOUT, DB_TIMEOUT, HELPER_TIMEOUT, PUBLISH_TIMEOUT, PERIODIC_REPAIR_TIME, STOP_CONFIG_TIMEOUT;

    private int DEFAULT_PUBLISH_TTL;

    private double EXTENT_REPUB_FREQ;

    /** local {@link ostore.util.CountBuffer}. */
    private CountBuffer _cbuf;

    private int MAX_REPAIR_ATTEMPTS;

    private EventHandlerIF _dbStage;

    private Random _rand;

    private BigInteger _nextGuid, MAX_GUID, MODULUS;

    private ASyncCore _acore;

    private MessageDigest _md;

    private Signature _sigEngine, _verifyEngine;

    private KeyFactory _keyFactory;

    private PrivateKey _keyPair_skey;

    private PublicKey _keyPair_pkey;

    private ss_public_key _pkey_ss;

    private gw_public_key _pkey_gw;

    private Set<Integer> _inflight_xact_id;

    /** 
   * {@link java.util.Map} of {@link antiquity.util.Extent extents}
   *  being repaired. */
    private Map<BigInteger, RepairState> _repair;

    /** Block events b/c DD was not ready */
    private Vector<QueueElementIF> _blockedEvents;

    /** All local stages have been initialized */
    private boolean _stagesInitialized;

    /** All remote procedure calls have been registered */
    private boolean _registeredRpcClient, _registeredRpcServer;

    /** DD is ready */
    private boolean _ddReady;

    /** identifier of local node. */
    private SecureHash _self_guid;

    /** identifier of local node. */
    private BigInteger _self_guid_sha1;

    /** identifier of local node. */
    private gw_guid _self_guid_gw;

    /** identifier of local node. */
    private ss_guid _self_guid_ss;

    /** {@link ostore.util.NodeId} of local gateway. */
    private NodeId gw_node_id;

    static {
        _instances = new HashMap<NodeId, RepairStage>();
    }

    /** Constructor: Creates a new <code>RepairStage</code> stage. */
    public RepairStage() throws Exception {
        event_types = new Class[] { dd.api.DDReadyMsg.class, seda.sandStorm.api.StagesInitializedSignal.class };
        inb_msg_types = new Class[] { dd.api.DDRepairObjReq.class };
        serializable_types = new Class[] { antiquity.ss.impl.StorageServer.Tag.class };
        _cbuf = new CountBuffer();
        _acore = bamboo.lss.DustDevil.acore_instance();
        _repair = new HashMap<BigInteger, RepairState>();
        _inflight_xact_id = new HashSet<Integer>();
        _blockedEvents = new Vector<QueueElementIF>();
        _md = MessageDigest.getInstance("SHA");
        _my_thread = Thread.currentThread();
        if (logger.isInfoEnabled()) logger.info("RepairStage.<init>: Creating signature engine...");
        String sig_alg = "SHA1withRSA";
        if (ostore.security.NativeRSAAlgorithm.available) {
            java.security.Security.addProvider(new ostore.security.NativeRSAProvider());
            _sigEngine = Signature.getInstance("SHA-1/RSA/PKCS#1", "NativeRSA");
            _verifyEngine = Signature.getInstance("SHA-1/RSA/PKCS#1", "NativeRSA");
        } else {
            _sigEngine = Signature.getInstance(sig_alg);
            _verifyEngine = Signature.getInstance(sig_alg);
        }
        _keyFactory = KeyFactory.getInstance("RSA");
        if (logger.isInfoEnabled()) logger.info("RepairStage.<init>: done.");
        if (logger.isInfoEnabled()) logger.info("Initializing RepairStage...");
    }

    /**
   * <code>getInstance</code> returns a reference to the 
   * <code>RepairStage</code>.
   * @return a reference to the <code>RepairStage</code>. */
    public static RepairStage getInstance(NodeId node_id) {
        return _instances.get(node_id);
    }

    public static <T1 extends XdrAble, T2 extends XdrAble, T3 extends XdrAble, T4 extends XdrAble> T4 createRepairSignature(T1 proof, T2 latest_config, T3 pkey, Class<T4> sig_clazz, PrivateKey skey, MessageDigest md, KeyFactory keyFactory, Signature sigEngine) {
        XdrAble cert = null;
        XdrAble config = null;
        XdrAble cert_cert = null;
        XdrAble config_config = null;
        XdrAble config_config_extent_key = null;
        XdrAble latest_config_config = null;
        XdrAble latest_config_config_extent_key = null;
        XdrAble[] latest_config_config_ss_set = null;
        try {
            Field field = proof.getClass().getField("cert");
            cert = (XdrAble) field.get(proof);
            field = proof.getClass().getField("config");
            config = (XdrAble) field.get(proof);
            assert cert != null && config != null : "RepairStage.createRepairSignature: proof has null fields " + XdrUtils.toString(proof);
            field = cert.getClass().getField("cert");
            cert_cert = (XdrAble) field.get(cert);
            field = config.getClass().getField("config");
            config_config = (XdrAble) field.get(config);
            assert cert_cert != null && config_config != null : "RepairStage.createRepairSignature: one of the following is null" + " cert.cert=" + XdrUtils.toString(cert_cert) + " or config.config=" + XdrUtils.toString(config_config);
            field = latest_config.getClass().getField("config");
            latest_config_config = (XdrAble) field.get(latest_config);
            assert latest_config_config != null : "RepairStage.createRepairSignature: latest_config has null fields " + XdrUtils.toString(latest_config);
            field = config_config.getClass().getField("extent_key");
            config_config_extent_key = (XdrAble) field.get(config_config);
            assert config_config_extent_key != null : "RepairStage.createRepairSignature: config.extent_key is null for config " + XdrUtils.toString(config_config);
            field = latest_config_config.getClass().getField("extent_key");
            latest_config_config_extent_key = (XdrAble) field.get(latest_config_config);
            field = latest_config_config.getClass().getField("ss_set");
            latest_config_config_ss_set = (XdrAble[]) field.get(latest_config_config);
            assert latest_config_config_extent_key != null && latest_config_config_ss_set != null : "RepairStage.createRepairSignature:" + " latest_config.{ss_set or extent_key} is null for latest_config " + XdrUtils.toString(latest_config_config);
            assert XdrUtils.equals(latest_config_config_extent_key, config_config_extent_key) : "RepairStage.createRepairSignature:" + " latest_config.extent_key=" + XdrUtils.toString(latest_config_config_extent_key) + " != proof.config.extent_key=" + XdrUtils.toString(config_config_extent_key);
            field = pkey.getClass().getField("value");
            byte[] pkey_value = (byte[]) field.get(pkey);
            assert pkey_value != null : "RepairStage.createRepairSignature:" + " pkey has null value field " + XdrUtils.toString(pkey);
            md.update(pkey_value);
            byte[] pkey_hash = md.digest();
            XdrAble pkey_guid = latest_config_config_extent_key.getClass().newInstance();
            field = pkey_guid.getClass().getField("value");
            field.set(pkey_guid, pkey_hash);
            int index = getSSIndex(pkey_guid, latest_config_config_ss_set, true);
            assert index >= 0 : "RepairStage.createRepairSignatue:" + "H(pkey)=" + XdrUtils.toString(pkey_guid) + " not contained in latest_config.ss_set=" + XdrUtils.toString(latest_config_config_ss_set) + ".  pkey=" + XdrUtils.toString(pkey);
        } catch (Exception e) {
            assert false : "Could not obtain public fields from proof=" + XdrUtils.toString(proof) + " or latest_config=" + XdrUtils.toString(latest_config) + ". " + e;
        }
        md.update(serialize(cert_cert));
        md.update(serialize(config_config));
        md.update(serialize(latest_config_config));
        byte[] sig_datahash = md.digest();
        byte[] sig_signature = null;
        try {
            sig_signature = createSignature(sig_datahash, skey, sigEngine);
        } catch (Exception e) {
            assert false : "RepairStage.createSignature: " + e;
        }
        T4 sig = null;
        try {
            sig = sig_clazz.newInstance();
            Field field = sig_clazz.getField("public_key");
            field.set(sig, pkey);
            field = sig_clazz.getField("datahash");
            field.set(sig, sig_datahash);
            field = sig_clazz.getField("signature");
            field.set(sig, sig_signature);
        } catch (Exception e) {
            assert false : "No value field in " + sig_clazz.getName() + ". " + e;
        }
        return sig;
    }

    public static <T1 extends XdrAble, T2 extends XdrAble, T3 extends XdrAble> boolean verifyRepairSignature(Logger logger, T1 sig, T2 proof, T3 latest_config, MessageDigest md, KeyFactory keyFactory, Signature verifyEngine) {
        XdrAble cert = null;
        XdrAble config = null;
        XdrAble cert_cert = null;
        XdrAble config_config = null;
        XdrAble config_config_extent_key = null;
        XdrAble latest_config_config = null;
        XdrAble latest_config_config_extent_key = null;
        XdrAble[] latest_config_config_ss_set = null;
        byte[] sig_datahash = null;
        byte[] sig_signature = null;
        XdrAble sig_pk = null;
        byte[] sig_pk_value = null;
        try {
            Field field = proof.getClass().getField("cert");
            cert = (XdrAble) field.get(proof);
            field = proof.getClass().getField("config");
            config = (XdrAble) field.get(proof);
            if (cert == null || config == null) {
                if (logger.isInfoEnabled()) logger.info("RepairStage.verifyRepairSignature:" + " proof has null fields " + XdrUtils.toString(proof));
                return false;
            }
            field = cert.getClass().getField("cert");
            cert_cert = (XdrAble) field.get(cert);
            field = config.getClass().getField("config");
            config_config = (XdrAble) field.get(config);
            if (cert_cert == null || config_config == null) {
                if (logger.isInfoEnabled()) logger.info("RepairStage.verifyRepairSignature:" + " one of the following is null" + " cert.cert=" + XdrUtils.toString(cert_cert) + " or config.config=" + XdrUtils.toString(config_config));
                return false;
            }
            field = latest_config.getClass().getField("config");
            latest_config_config = (XdrAble) field.get(latest_config);
            if (latest_config_config == null) {
                if (logger.isInfoEnabled()) logger.info("RepairStage.verifyRepairSignature:" + " latest_config has null fields " + XdrUtils.toString(latest_config));
                return false;
            }
            field = config_config.getClass().getField("extent_key");
            config_config_extent_key = (XdrAble) field.get(config_config);
            if (config_config_extent_key == null) {
                if (logger.isInfoEnabled()) logger.info("RepairStage.verifyRepairSignature:" + " config.extent_key is null for config " + XdrUtils.toString(config_config));
                return false;
            }
            field = latest_config_config.getClass().getField("extent_key");
            latest_config_config_extent_key = (XdrAble) field.get(latest_config_config);
            field = latest_config_config.getClass().getField("ss_set");
            latest_config_config_ss_set = (XdrAble[]) field.get(latest_config_config);
            if (latest_config_config_extent_key == null || latest_config_config_ss_set == null) {
                if (logger.isInfoEnabled()) logger.info("RepairStage.verifyRepairSignature:" + " latest_config.{ss_set or extent_key} is null for" + " latest_config " + XdrUtils.toString(latest_config));
                return false;
            }
            if (!XdrUtils.equals(latest_config_config_extent_key, config_config_extent_key)) {
                if (logger.isInfoEnabled()) logger.info("RepairStage.verifyRepairSignature:" + " latest_config.extent_key=" + XdrUtils.toString(latest_config_config_extent_key) + " != proof.config.extent_key=" + XdrUtils.toString(config_config_extent_key));
                return false;
            }
            field = sig.getClass().getField("datahash");
            sig_datahash = (byte[]) field.get(sig);
            field = sig.getClass().getField("signature");
            sig_signature = (byte[]) field.get(sig);
            field = sig.getClass().getField("public_key");
            sig_pk = (XdrAble) field.get(sig);
            if (sig_datahash == null || sig_signature == null || sig_pk == null) {
                if (logger.isInfoEnabled()) logger.info("RepairStage.verifyRepairSignature:" + " sig object has null fields " + XdrUtils.toString(sig));
                return false;
            }
            field = sig_pk.getClass().getField("value");
            sig_pk_value = (byte[]) field.get(sig_pk);
            md.update(sig_pk_value);
            byte[] sig_pk_hash = md.digest();
            XdrAble sig_pk_guid = latest_config_config_extent_key.getClass().newInstance();
            field = sig_pk_guid.getClass().getField("value");
            field.set(sig_pk_guid, sig_pk_hash);
            int index = getSSIndex(sig_pk_guid, latest_config_config_ss_set, true);
            if (index < 0) {
                if (logger.isInfoEnabled()) logger.info("RepairStage.createRepairSignatue:" + "H(sig_pk)=" + XdrUtils.toString(sig_pk_guid) + " not contained in latest_config.ss_set=" + XdrUtils.toString(latest_config_config_ss_set) + ".  sig_pk=" + XdrUtils.toString(sig_pk));
                return false;
            }
        } catch (Exception e) {
            if (logger.isInfoEnabled()) logger.info("RepairStage.verifyRepairSignature" + "Could not obtain public fields from proof=" + XdrUtils.toString(proof) + " or latest_config=" + XdrUtils.toString(latest_config) + ". " + e);
            return false;
        }
        md.update(serialize(cert_cert));
        md.update(serialize(config_config));
        md.update(serialize(latest_config_config));
        byte[] datahash = md.digest();
        if (!ByteUtils.equals(sig_datahash, datahash)) {
            if (logger.isInfoEnabled()) logger.info("RepairStage.verifyRepairSignature: datahash in sig=" + XdrUtils.toString(sig) + " does not match datahash=" + ByteUtils.print_bytes(datahash, 0, num_bytes_to_print));
            return false;
        }
        boolean rv = false;
        try {
            rv = verifySignature(sig_datahash, sig_signature, sig_pk, keyFactory, verifyEngine);
        } catch (Exception e) {
        }
        return rv;
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void init(ConfigDataIF config) throws Exception {
        final String method_tag = tag + ".init";
        super.init(config);
        _instances.put(my_node_id, this);
        gw_node_id = new NodeId(my_node_id.port() + 1, my_node_id.address());
        _rand = new Random(new ostore.util.SHA1Hash(RepairStage.class.getName() + " " + my_node_id + now_us()).lower64bits());
        _nextGuid = GuidTools.random_guid(_rand);
        assert config.contains("MaxRepairAttempts") : "Need to define MaxRepairAttempts in cfg file";
        MAX_REPAIR_ATTEMPTS = config_get_int(config, "MaxRepairAttempts");
        assert config.contains("PeriodicRepairTime") : "Need to define PeriodicRepairTime in cfg file";
        PERIODIC_REPAIR_TIME = config_get_long(config, "PeriodicRepairTime");
        assert config.contains("RepairTimeout") : "Need to define RepairTimeout in cfg file";
        REPAIR_TIMEOUT = config_get_long(config, "RepairTimeout");
        STOP_CONFIG_TIMEOUT = REPAIR_TIMEOUT;
        assert config.contains("HelperTimeout") : "Need to define HelperTimeout in cfg file";
        HELPER_TIMEOUT = config_get_long(config, "HelperTimeout");
        assert config.contains("ReclaimTimeout") : "Need to define ReclaimTimeout in cfg file";
        RECLAIM_TIMEOUT = config_get_long(config, "ReclaimTimeout");
        assert config.contains("DbTimeout") : "Need to define DbTimeout in cfg file";
        DB_TIMEOUT = config_get_long(config, "DbTimeout");
        assert config.contains("PublishTimeout") : "Need to define PublishTimeout in cfg file";
        PUBLISH_TIMEOUT = config_get_long(config, "PublishTimeout");
        assert config.contains("DefaultPublishTTL") : "Need to define DefaultPublishTTL in cfg file";
        DEFAULT_PUBLISH_TTL = config_get_int(config, "DefaultPublishTTL");
        assert config.contains("ExtentRepubFreq") : "Need to define ExtentRepubFreq in cfg file";
        EXTENT_REPUB_FREQ = config_get_double(config, "ExtentRepubFreq");
        assert EXTENT_REPUB_FREQ >= 0.0 && EXTENT_REPUB_FREQ <= 1.0 : "ExtentRepubFreq=" + EXTENT_REPUB_FREQ + " is not a probability.";
        assert config.contains("PkeyFilename") : "Need to define PkeyFilename in cfg file";
        assert config.contains("SkeyFilename") : "Need to define SkeyFilename in cfg file";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": Reading keys from disk...");
        String pkey_filename = config_get_string(config, "PkeyFilename");
        String skey_filename = config_get_string(config, "SkeyFilename");
        _keyPair_pkey = readPublicKey(pkey_filename, _keyFactory);
        _keyPair_skey = readPrivateKey(skey_filename, _keyFactory);
        if ((_keyPair_pkey == null) || (_keyPair_skey == null)) throw new Exception(method_tag + ": Failed to read key pair from disk: " + "pkey=" + pkey_filename + ", skey=" + skey_filename);
        _pkey_gw = new gw_public_key(_keyPair_pkey.getEncoded());
        _pkey_ss = AntiquityUtils.convert(_pkey_gw, ss_public_key.class);
        _sigEngine.initSign(_keyPair_skey);
        if (logger.isInfoEnabled()) logger.info(method_tag + ": Reading keys from disk...done.");
        Class[] rpc_msg_types = new Class[] { antiquity.rpc.api.RpcCall.class, antiquity.rpc.api.RpcReply.class, antiquity.rpc.api.RpcRegisterResp.class };
        for (Class clazz : rpc_msg_types) {
            Filter filter = new Filter();
            if (!filter.requireType(clazz)) BUG(tag + ": could not require type " + clazz.getName());
            if (antiquity.rpc.api.RpcCall.class.isAssignableFrom(clazz) || antiquity.rpc.api.RpcReply.class.isAssignableFrom(clazz)) {
                if (!filter.requireValue("inbound", new Boolean(true))) BUG(tag + ": could not require inbound = true for " + clazz.getName());
            }
            if (!filter.requireValue("appId", appId)) BUG(tag + ": could not require appId = " + appId + " for " + clazz.getName());
            if (logger.isDebugEnabled()) logger.debug(tag + ": subscribing to " + clazz);
            classifier.subscribe(filter, my_sink);
        }
        if (logger.isInfoEnabled()) logger.info(method_tag + ": " + "init done");
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void handleEvent(QueueElementIF item) throws EventHandlerException {
        final String method_tag = tag + ".handleEvent";
        if (item instanceof StagesInitializedSignal) {
            handleStagesInitializedSignal((StagesInitializedSignal) item);
            return;
        } else if (item instanceof DDReadyMsg) {
            handleDDReadyMsg((DDReadyMsg) item);
            return;
        } else if (!_stagesInitialized || !_ddReady || !_registeredRpcClient || !_registeredRpcServer) {
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + ": Queueing event " + item + " until stage initialized and registered.");
            _blockedEvents.add(item);
            return;
        } else if (item instanceof DDRepairObjReq) {
            handleDDRepairObjReq((DDRepairObjReq) item);
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
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called " + signal);
        _stagesInitialized = true;
        _dbStage = GenericStorageManager.getInstance(my_node_id, 0L);
        assert _dbStage != null : "dbStage is null for " + my_node_id + " type=0";
        MODULUS = bamboo.router.Router.instance(my_node_id).modulus();
        MAX_GUID = MODULUS.subtract(BigInteger.ONE);
        Map<ProcKey, ProcValue> serverProcedures = new HashMap<ProcKey, ProcValue>();
        ProcKey serverProcKey = AntiquityUtils.getProcedureKey(AntiquityUtils.Procedure.SS_REPAIR_AUDIT);
        ProcValue serverProcVal = AntiquityUtils.getProcedure(serverProcKey);
        serverProcedures.put(serverProcKey, serverProcVal);
        RpcRegisterReq rpcServerReq = new RpcRegisterReq(serverProcedures, false, appId, Boolean.FALSE, my_sink);
        rpcServerReq.handlers = new LinkedHashMap<ProcKey, Thunk1<RpcCall>>();
        rpcServerReq.handlers.put(serverProcKey, handle_ss_repair_audit);
        rpcServerReq.cb = new Thunk2<RpcRegisterReq, RpcRegisterResp>() {

            public void run(RpcRegisterReq rpcCall, RpcRegisterResp rpcResp) {
                handleRpcRegisterResp(rpcResp);
            }
        };
        dispatch(rpcServerReq);
        Map<ProcKey, ProcValue> clientProcedures = new HashMap<ProcKey, ProcValue>();
        ProcKey clientProcKey = AntiquityUtils.getProcedureKey(AntiquityUtils.Procedure.GW_REPAIR);
        ProcValue clientProcVal = AntiquityUtils.getProcedure(clientProcKey);
        clientProcedures.put(clientProcKey, clientProcVal);
        RpcRegisterReq rpcClientReq = new RpcRegisterReq(clientProcedures, true, appId, Boolean.TRUE, my_sink);
        rpcClientReq.cb = new Thunk2<RpcRegisterReq, RpcRegisterResp>() {

            public void run(RpcRegisterReq rpcCall, RpcRegisterResp rpcResp) {
                handleRpcRegisterResp(rpcResp);
            }
        };
        dispatch(rpcClientReq);
    }

    /**
   * <CODE>handleRpcRegisteredResp</CODE> verifies that all remote
   * procedure calls were registered.
   *
   * @param resp {@link antiquity.rpc.api.RpcRegisterResp} */
    private void handleRpcRegisterResp(RpcRegisterResp resp) {
        final String method_tag = tag + ".handleRpcRegisterResp";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called " + resp);
        for (Map.Entry<ProcKey, Integer> entry : resp.responses.entrySet()) {
            if (!RpcRegisterResp.SUCCESS.equals(entry.getValue())) BUG(method_tag + ": proc " + entry + " was not registered.");
        }
        assert resp.userData instanceof Boolean : "unknown userData " + resp.userData;
        if ((Boolean) resp.userData.equals(Boolean.TRUE)) _registeredRpcClient = true; else _registeredRpcServer = true;
        if (_stagesInitialized && _ddReady && _registeredRpcClient && _registeredRpcServer) initializeRepairStage();
    }

    /**
   * <CODE>handleDDReadyMsg</CODE> assigns the
   * {@link dd.api.DDReadyMsg#node_guid self_guid}.
   *
   * @param signal {@link dd.api.DDReadyMsg} */
    private void handleDDReadyMsg(DDReadyMsg msg) {
        final String method_tag = tag + ".handleDDReadyMsg";
        _self_guid = msg.node_guid;
        _self_guid_sha1 = GuidTools.secure_hash_to_big_integer(_self_guid);
        _self_guid_gw = AntiquityUtils.secureHashToGuid(_self_guid, gw_guid.class);
        _self_guid_ss = AntiquityUtils.secureHashToGuid(_self_guid, ss_guid.class);
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called " + msg);
        _ddReady = true;
        if (_stagesInitialized && _ddReady && _registeredRpcClient && _registeredRpcServer) initializeRepairStage();
    }

    /**
   * <CODE>initializeStorageServer</CODE> unblocks all blocked events.
   * That is, <I>all systems go</I>!
   * <I>INVARIANT</I> All stages initialized and rpc handlers registered. */
    private void initializeRepairStage() {
        final String method_tag = tag + ".initializeStorageServer";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": called\n");
        if (logger.isInfoEnabled()) logger.info(method_tag + ": " + "Accepting requests...");
        while (_blockedEvents.size() > 0) {
            try {
                handleEvent((QueueElementIF) _blockedEvents.remove(0));
            } catch (EventHandlerException ehe) {
                BUG(ehe);
            }
        }
        long periodic_repair_time_ms = (PERIODIC_REPAIR_TIME / 2L) + (Math.abs(_rand.nextLong()) % (PERIODIC_REPAIR_TIME / 2L));
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": set next periodic repair " + " time in " + (periodic_repair_time_ms / 1000.0) + " s from now.  Will search in range 0x" + GuidTools.guid_to_string(_nextGuid) + " to 0x" + GuidTools.guid_to_string(MAX_GUID) + ".");
        _acore.register_timer(periodic_repair_time_ms, periodic_repair);
    }

    /**
   * <CODE>handleDDRepairObjReq</CODE> receives 
   * {@link dd.api.DDRepairObjReq trigger} from <CODE>tapestry</CODE> 
   * indicating that the redundancy needs to be refreshed.
   * 
   * @param req {@link dd.api.DDRepairObjReq trigger}. */
    private void handleDDRepairObjReq(DDRepairObjReq req) {
        final String method_tag = tag + ".handleDDRepairObjReq";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": called " + req);
        if (!(req.getTag() instanceof StorageServer.Tag)) {
            logger.warn(method_tag + ": tag " + req.getTag() + " not instanceof " + StorageServer.Tag.class.getName() + ". Dropping " + req);
            return;
        }
        BigInteger key = GuidTools.secure_hash_to_big_integer(req.getObjguid());
        if (_repair.containsKey(key)) {
            RepairState state = _repair.get(key);
            if (!state.reclaimed && state.audit_result == null) {
                logger.warn(method_tag + ": already processing repair for extent 0x" + GuidTools.guid_to_string(key) + " add req=" + req + " to state=" + state);
                if (state.failure_detectors == null) state.failure_detectors = new LinkedList<DDRepairObjReq>();
                state.failure_detectors.add(req);
            } else {
                logger.warn(method_tag + ": already processed repair for extent 0x" + GuidTools.guid_to_string(key) + " send response immediately" + ". state=" + state);
                dispatch(new DDRepairObjResp(req.peer, req.getObjguid(), req.getTag(), true, true, false));
            }
            return;
        }
        RepairState state = new RepairState(req);
        _repair.put(state.extent_key_sha1, state);
        Thunk3<ByteBuffer, Map<BigInteger, ByteBuffer>, DbError> cb = curry(handle_db_lookup_cb, state);
        db_get_cert_and_blocks(_dbStage, method_tag, cb, state.extent_key_ss, (ss_guid[]) null, now_us());
    }

    /**
   * <CODE>handleDDRepairObjReq</CODE> receives 
   * {@link dd.api.DDRepairObjReq trigger} from <CODE>tapestry</CODE> 
   * indicating that the redundancy needs to be refreshed.
   * 
   * @param req {@link dd.api.DDRepairObjReq trigger}. */
    private Thunk1<RpcCall> handle_ss_repair_audit = new Thunk1<RpcCall>() {

        public void run(RpcCall req) {
            final String method_tag = tag + ".handle_ss_repair_audit";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": called " + req);
            if (!(req.args instanceof ss_repair_audit_args)) {
                logger.warn(method_tag + ": args " + XdrUtils.toString(req.args) + " not instanceof " + ss_repair_audit_args.class.getName() + ". Dropping " + req);
                return;
            }
            ss_repair_audit_args repair_audit_args = (ss_repair_audit_args) req.args;
            BigInteger key = guidToBigInteger(repair_audit_args.extent_key);
            if (_repair.containsKey(key)) {
                RepairState state = _repair.get(key);
                if (!state.reclaimed && state.audit_result == null) {
                    logger.warn(method_tag + ": already processing repair for extent 0x" + GuidTools.guid_to_string(key) + " add req=" + req + " to state=" + state);
                    if (state.audits == null) state.audits = new LinkedList<RpcCall>();
                    state.audits.add(req);
                } else {
                    logger.warn(method_tag + ": already processed repair for extent 0x" + GuidTools.guid_to_string(key) + " send response immediately" + ". state=" + state);
                    assert state.audit_result != null : "state.audit result is null for req=" + req + " state=" + state;
                    RpcReply resp = new RpcReply(state.audit_result, req);
                    req.server_stub_cb.run(resp);
                }
                return;
            }
            RepairState state = new RepairState(req);
            _repair.put(state.extent_key_sha1, state);
            Thunk3<ByteBuffer, Map<BigInteger, ByteBuffer>, DbError> cb = curry(handle_db_lookup_cb, state);
            db_get_cert_and_blocks(_dbStage, method_tag, cb, state.extent_key_ss, (ss_guid[]) null, now_us());
        }
    };

    /** 
   * <code>triggerCollectProof</code> initiates a 
   * process that collects a {@link antiquity.ss.api.ss_soundness_proof}. */
    public static void triggerCollectProof(NodeId node_id, Logger logger, String caller, BigInteger extent_key_sha1, BigInteger client_id_sha1) {
        final String method_tag = caller + ".triggerCollectProof";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": called for extent 0x" + GuidTools.guid_to_string(extent_key_sha1) + " client_id 0x" + GuidTools.guid_to_string(client_id_sha1));
        RepairStage stage = getInstance(node_id);
        assert stage != null;
        stage.handleCollectProof(extent_key_sha1, client_id_sha1);
    }

    /**
   * <CODE>handleCollectProof</CODE> collects responses from the 
   * other storage servers in the
   * {@link antiquity.ss.api.ss_signed_ss_set_config} to create a 
   * {@link antiquity.ss.api.ss_soundness_proof}.
   * 
   * @param extent_key_sha1 {@link java.math.BigInteger extent_key}. */
    private void handleCollectProof(BigInteger extent_key_sha1, BigInteger client_id_sha1) {
        final String method_tag = tag + ".handleCollectProof";
        assert _my_thread == Thread.currentThread() : "my_thread==" + _my_thread + " != currentThread=" + Thread.currentThread();
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": called for extent 0x" + GuidTools.guid_to_string(extent_key_sha1) + " client_id 0x" + GuidTools.guid_to_string(client_id_sha1));
        if (_repair.containsKey(extent_key_sha1)) {
            logger.warn(method_tag + ": already processing repair for extent 0x" + GuidTools.guid_to_string(extent_key_sha1));
            return;
        }
        gw_guid extent_key = bigIntegerToGuid(extent_key_sha1, gw_guid.class);
        ss_guid extent_key_ss = bigIntegerToGuid(extent_key_sha1, ss_guid.class);
        gw_guid client_id = bigIntegerToGuid(client_id_sha1, gw_guid.class);
        ss_guid client_id_ss = bigIntegerToGuid(client_id_sha1, ss_guid.class);
        RepairState state = new RepairState(false, extent_key, extent_key_ss, extent_key_sha1, client_id, client_id_ss, client_id_sha1);
        _repair.put(state.extent_key_sha1, state);
        Thunk3<ByteBuffer, Map<BigInteger, ByteBuffer>, DbError> cb = curry(handle_db_lookup_cb, state);
        db_get_cert_and_blocks(_dbStage, method_tag, cb, state.extent_key_ss, (ss_guid[]) null, now_us());
    }

    /**********************************************************************/
    Runnable periodic_repair = new Runnable() {

        public void run() {
            final String method_tag = tag + ".periodic_repair";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": called");
            Long start_time_us = now_us();
            Object token = _acore.register_timer(DB_TIMEOUT, periodic_repair_db_lookup_timeout_cb);
            DbIterateByGuidReq dbiteratereq = new DbIterateByGuidReq(_nextGuid, MAX_GUID, 1, (DbKey) null, true, -1L, (Object) null, (SinkIF) null, 0L);
            dbiteratereq.cb = curry(periodic_repair_db_lookup_cb, start_time_us, token);
            dispatch(dbiteratereq);
        }
    };

    Thunk4<Long, Object, DbIterateByGuidReq, DbIterateByGuidResp> periodic_repair_db_lookup_cb = new Thunk4<Long, Object, DbIterateByGuidReq, DbIterateByGuidResp>() {

        public void run(Long start_time_us, Object token, DbIterateByGuidReq dbiteratereq, DbIterateByGuidResp dbiterateresp) {
            final String method_tag = tag + ".periodic_repair_db_lookup_cb";
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": called");
            if (!isCurrentReq(null, null, token, method_tag)) return;
            if (dbiterateresp.keys != null && dbiterateresp.keys.length > 0 && dbiterateresp.keys[0].secondaryGuid.equals(BigInteger.ZERO)) {
                BigInteger extent_key_sha1 = dbiterateresp.keys[0].guid;
                gw_guid extent_key = bigIntegerToGuid(extent_key_sha1, gw_guid.class);
                ss_guid extent_key_ss = bigIntegerToGuid(extent_key_sha1, ss_guid.class);
                _nextGuid = extent_key_sha1.add(BigInteger.ONE);
                if (_nextGuid.compareTo(MODULUS) >= 0) _nextGuid = BigInteger.ZERO;
                if (_repair.containsKey(extent_key_sha1)) {
                    logger.warn(method_tag + ": already processing extent 0x" + GuidTools.guid_to_string(extent_key_sha1) + " look for another extent to periodically repair");
                    periodic_repair.run();
                    return;
                }
                HeadBlock headBlock = deserializeHeadBlock(method_tag, extent_key_ss, _self_guid_ss, dbiterateresp.data[0], _md, _keyFactory, _verifyEngine);
                RepairState state = new RepairState(true, extent_key, extent_key_ss, extent_key_sha1, convert(headBlock.config_db.config.client_id, gw_guid.class), headBlock.config_db.config.client_id, guidToBigInteger(headBlock.config_db.config.client_id));
                _repair.put(state.extent_key_sha1, state);
                state.headBlock = headBlock;
                if (state.headBlock.repair_sig_db == null) {
                    if (logger.isInfoEnabled()) logger.info(method_tag + ": start periodic repair process for" + " extent 0x" + GuidTools.guid_to_string(extent_key_sha1));
                    send_gateway_helper_req(state);
                } else {
                    state.audit_result = getNormalResult(state);
                    send_repair(state, false);
                }
            } else if (dbiterateresp.keys != null && dbiterateresp.keys.length > 0 && !dbiterateresp.keys[0].secondaryGuid.equals(BigInteger.ZERO)) {
                _nextGuid = dbiterateresp.keys[0].guid.add(BigInteger.ONE);
                if (_nextGuid.compareTo(MODULUS) >= 0) _nextGuid = BigInteger.ZERO;
                logger.warn(method_tag + ": No headblock for extent 0x" + GuidTools.guid_to_string(dbiterateresp.keys[0].guid) + " look for another extent to periodically repair");
                periodic_repair.run();
            } else {
                assert dbiterateresp.cont == null : method_tag + ": There should not be more blocks since iterate.resp=" + dbiterateresp + " for ireatereq=" + dbiteratereq + ", but cont is not null " + dbiterateresp.cont;
                if (logger.isInfoEnabled()) logger.info(method_tag + ": No extents to periodically repair " + " between extent guid 0x" + GuidTools.guid_to_string(_nextGuid) + " and 0x" + GuidTools.guid_to_string(MAX_GUID) + ". start next sweep from beginning, 0x" + GuidTools.guid_to_string(BigInteger.ZERO) + ".");
                _nextGuid = BigInteger.ZERO;
                long periodic_repair_time_ms = (PERIODIC_REPAIR_TIME / 2L) + (Math.abs(_rand.nextLong()) % (PERIODIC_REPAIR_TIME / 2L));
                if (logger.isDebugEnabled()) logger.debug(method_tag + ": set next periodic repair " + " time in " + (periodic_repair_time_ms / 1000.0) + " s from now.  Will search in range 0x" + GuidTools.guid_to_string(_nextGuid) + " to 0x" + GuidTools.guid_to_string(MAX_GUID) + ".");
                _acore.register_timer(periodic_repair_time_ms, periodic_repair);
            }
        }
    };

    Runnable periodic_repair_db_lookup_timeout_cb = new Runnable() {

        public void run() {
            final String method_tag = tag + ".periodic_repair_db_lookup_timeout_cb";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": called");
        }
    };

    /**
   * <code>handle_db_lookup_cb</code> retrieves from the db, for a given 
   * {@link antiquity.gw.api.gw_guid extent_key}, the 
   * {@link antiquity.gw.api.gw_signed_certificate certificate}.
   * If a certificate exists, this method sends a 
   * {@link antiquity.gw.api.gw_repair} to the extent <code>coordinator</code>.
   *
   * @param state {@link antiquity.ss.impl.RepairStage.RepairState}.
   * @param cert_buf {@link java.nio.ByteBuffer} of 
   *     {@link antiquity.ss.api.ss_signed_certificate certificate} from db. */
    private Thunk4<RepairState, ByteBuffer, Map<BigInteger, ByteBuffer>, DbError> handle_db_lookup_cb = new Thunk4<RepairState, ByteBuffer, Map<BigInteger, ByteBuffer>, DbError>() {

        public void run(RepairState state, ByteBuffer cert_buf, Map<BigInteger, ByteBuffer> blocks, DbError dbError) {
            final String method_tag = tag + ".handle_db_lookup_cb";
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called " + state + ". " + ((now_us() - state.start_time_us) / 1000.0) + "ms have passed so far.");
            if (!isCurrentReq(state, null, null, method_tag)) return;
            if (dbError != null && dbError != DbError.NONE) {
                logger.warn(method_tag + ": dberror " + dbError + " for " + XdrUtils.toString(state.extent_key) + ". state=" + state);
                state.audit_result = getErrorResult(state, ss_status.STATUS_ERR);
                send_reclaim_resource_timer(state, method_tag);
                return;
            }
            if (cert_buf != null) {
                state.headBlock = deserializeHeadBlock(method_tag, state.extent_key_ss, _self_guid_ss, cert_buf, _md, _keyFactory, _verifyEngine);
                if (XdrUtils.equals(state.client_id, GW_NULL_GUID)) {
                    state.client_id_ss = state.headBlock.config_db.config.client_id;
                    state.client_id = convert(state.client_id_ss, gw_guid.class);
                    state.client_id_sha1 = guidToBigInteger(state.client_id_ss);
                }
                if (state.headBlock.repair_sig_db == null) {
                    send_gateway_helper_req(state);
                } else {
                    state.audit_result = getNormalResult(state);
                    send_reclaim_resource_timer(state, method_tag);
                }
            } else {
                send_get_cert(state);
            }
        }
    };

    /**********************************************************************/
    private void send_get_cert(RepairState state) {
        final String method_tag = tag + ".send_get_cert";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": sending get_cert request to Gateway for " + " extent_key=0x" + XdrUtils.toString(state.extent_key) + " state=" + state);
        gw_get_certificate_args get_args = new gw_get_certificate_args();
        get_args.extent_key = state.extent_key;
        get_args.latest = 1;
        get_args.verbose = 0;
        get_args.use_admin = 1;
        get_args.client_id = state.client_id;
        ProcKey get_proc = getProcedureKey(Procedure.GW_GET_CERT);
        RpcCall get_req = new RpcCall(gw_node_id, get_proc, get_args, appId, null, (SinkIF) null);
        get_req.xact_id = _rand.nextInt();
        get_req.caused_by_xact_id = state.caused_by_xact_id;
        _inflight_xact_id.add(get_req.xact_id);
        Object token = _acore.register_timer(HELPER_TIMEOUT, curry(handle_gateway_helper_timeout_cb, state, get_req, method_tag));
        get_req.server_stub_cb = curry(handle_get_cert_cb, state, token, get_req);
        RpcServerStage.Handler handler = RpcServerStage.getInstance(gw_node_id).getHandler(get_proc);
        assert handler != null : "handler is null for " + get_proc;
        _cbuf.reset();
        XdrUtils.serialize(_cbuf, get_req.args);
        log_req(false, get_req, 0.0, _cbuf.size());
        handlerEnqueue(handler, get_req);
    }

    private Thunk4<RepairState, Object, RpcCall, RpcReply> handle_get_cert_cb = new Thunk4<RepairState, Object, RpcCall, RpcReply>() {

        public void run(RepairState state, Object token, RpcCall req, RpcReply reply) {
            final String method_tag = tag + ".handle_get_cert_cb";
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": called extent_key=0x" + XdrUtils.toString(state.extent_key) + " reply=" + reply + ". state=" + state);
            if (!isCurrentReq(state, req.xact_id, token, method_tag)) return;
            _cbuf.reset();
            XdrUtils.serialize(_cbuf, reply.reply);
            log_resp(false, state.start_time_us, 0.0, req, reply, _cbuf.size());
            gw_get_certificate_result get_result = null;
            if (!(reply.reply instanceof XdrVoid)) get_result = (gw_get_certificate_result) reply.reply;
            if (get_result != null && get_result.status == gw_status.GW_STATUS_OK) {
                ss_soundness_proof proof_ss = convert(get_result.proof, ss_soundness_proof.class);
                state.get_result_proof_ss = proof_ss;
                ss_signed_signature sig = null;
                int index = getSSIndex(_self_guid_ss, proof_ss.config.config.ss_set, true);
                if (index >= 0) sig = proof_ss.sigs[index];
                state.headBlock = new HeadBlock((ss_guid[]) null, (ss_guid[]) null, (ss_guid[]) null, proof_ss.cert.cert.verifier, (ss_guid) null, proof_ss.cert.cert.verifier, (ss_log_entry[]) null, proof_ss, proof_ss.cert, proof_ss.config, sig, null);
                send_repair(state, true);
            } else {
                int index = getSSIndex(_self_guid_gw, get_result.proof.config.config.ss_set, true);
                if (get_result == null || get_result.proof == null || XdrUtils.equals(get_result.proof.cert.cert.verifier, GW_NULL_GUID) || XdrUtils.equals(get_result.proof.config.config.extent_key, GW_NULL_GUID) || index < 0 || !checkSoundnessProof(get_result.proof, true, false, _md, _keyFactory, _verifyEngine)) {
                    state.audit_result = getErrorResult(state, ss_status.STATUS_ERR_NOT_EXIST);
                    send_reclaim_resource_timer(state, method_tag);
                } else {
                    state.get_result_proof_ss = convert(get_result.proof, ss_soundness_proof.class);
                    state.headBlock = new HeadBlock((ss_guid[]) null, (ss_guid[]) null, (ss_guid[]) null, state.get_result_proof_ss.cert.cert.verifier, (ss_guid) null, state.get_result_proof_ss.cert.cert.verifier, (ss_log_entry[]) null, state.get_result_proof_ss, state.get_result_proof_ss.cert, state.get_result_proof_ss.config, state.get_result_proof_ss.sigs[index], (ss_signed_signature) null);
                    send_gateway_helper_req(state);
                }
            }
        }
    };

    /**********************************************************************/
    private void send_gateway_helper_req(RepairState state) {
        final String method_tag = tag + ".send_gateway_helper_req";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": sending request to GatewayHelper for " + " extent_key=0x" + XdrUtils.toString(state.extent_key) + " state=" + state);
        gw_get_certificate_args get_args = new gw_get_certificate_args();
        get_args.extent_key = state.extent_key;
        get_args.latest = 1;
        get_args.verbose = 0;
        get_args.use_admin = 0;
        get_args.client_id = state.client_id;
        ProcKey get_proc = getProcedureKey(Procedure.GW_GET_CERT);
        RpcCall get_req = new RpcCall(gw_node_id, get_proc, get_args, appId, null, (SinkIF) null);
        get_req.xact_id = _rand.nextInt();
        get_req.caused_by_xact_id = state.caused_by_xact_id;
        _inflight_xact_id.add(get_req.xact_id);
        Object token = _acore.register_timer(HELPER_TIMEOUT, curry(handle_gateway_helper_timeout_cb, state, get_req, method_tag));
        assert state.headBlock != null : "headBlock is null for " + state;
        gw_signed_certificate cert_db = convert(state.headBlock.cert_db, gw_signed_certificate.class);
        gw_signed_ss_set_config config_db = convert(state.headBlock.config_db, gw_signed_ss_set_config.class);
        gw_soundness_proof proof_db = convert(state.headBlock.proof_db, gw_soundness_proof.class);
        if (proof_db == null) proof_db = GW_NULL_SOUNDNESS_PROOF;
        SoundnessProofTag proofTag = new SoundnessProofTag(now_ms(), proof_db);
        GatewayHelper.Req newReq = new GatewayHelper.Req(get_req.peer, get_req.xact_id, get_req.caused_by_xact_id, state.extent_key, get_req.proc, get_req.args, false, false, proofTag, config_db, appId, null, (SinkIF) null);
        newReq.cb = curry(handle_gateway_helper_cb, state, token, get_req);
        _cbuf.reset();
        XdrUtils.serialize(_cbuf, get_req.args);
        log_req(false, get_req, 0.0, _cbuf.size());
        dispatch(newReq);
    }

    private Thunk5<RepairState, Object, RpcCall, GatewayHelper.Req, GatewayHelper.Resp> handle_gateway_helper_cb = new Thunk5<RepairState, Object, RpcCall, GatewayHelper.Req, GatewayHelper.Resp>() {

        public void run(RepairState state, Object token, RpcCall req, GatewayHelper.Req gwReq, GatewayHelper.Resp reply) {
            final String method_tag = tag + ".handle_gateway_helper_cb";
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": called extent_key=0x" + XdrUtils.toString(state.extent_key) + " reply=" + reply + ". state=" + state);
            if (!isCurrentReq(state, gwReq.xact_id, token, method_tag)) return;
            _cbuf.reset();
            XdrUtils.serialize(_cbuf, reply.reply);
            log_resp(false, state.start_time_us, 0.0, req, new RpcReply(reply.reply, req), _cbuf.size());
            gw_get_certificate_result get_result = null;
            if (!(reply.reply instanceof XdrVoid)) get_result = (gw_get_certificate_result) reply.reply;
            if (get_result != null && get_result.status == gw_status.GW_STATUS_OK) {
                ProofState prevProofCase = ProofState.NEW;
                state.get_result_proof_ss = convert(get_result.proof, ss_soundness_proof.class);
                Map<NodeId, RpcReply> ss_replies = reply.ss_replies;
                Map<NodeId, ProofState> ss_proof_state = reply.ss_proof_state;
                assert state.headBlock != null : "headBlock is null for " + state;
                if (!isResponsible(state.get_result_proof_ss, state)) {
                    state.audit_result = getNormalResult(state);
                    send_reclaim_resource_timer(state, method_tag);
                    return;
                }
                boolean useLogs = true;
                prevProofCase = inspectPrevProof(method_tag, state.extent_key_ss, state.get_result_proof_ss, state.headBlock, true, _md, _keyFactory, _verifyEngine);
                if (prevProofCase == ProofState.OLD) {
                    prevProofCase = inspectPrevProof(method_tag, state.extent_key_ss, state.get_result_proof_ss, state.headBlock, useLogs, _md, _keyFactory, _verifyEngine);
                    if (logger.isInfoEnabled()) logger.info(method_tag + ": for extent " + XdrUtils.toString(state.extent_key) + " prevProofState=" + prevProofCase + " when logs ignored " + " (after including logs returned " + ProofState.OLD + ").");
                }
                if (!reply.ss_replies.containsKey(gw_node_id)) {
                    ss_get_certificate_args ss_get_args = (ss_get_certificate_args) GatewayHelper.getSSArgs(gwReq.args, (gw_signed_ss_set_config) null, (gw_signed_ss_set_config) null, GW_NULL_SOUNDNESS_PROOF, gwReq.proc);
                    ProcKey ss_proc = GatewayHelper.getSSProcKey(gwReq.proc);
                    ss_get_certificate_result result = (ss_get_certificate_result) StorageServer.getNormalResult(ss_get_args, ss_proc, state.extent_key_ss, state.headBlock.config_db, state.headBlock.cert_db, null, null, state.headBlock.sig_db, (state.headBlock.proof_block_names_db != null ? state.headBlock.proof_block_names_db : new ss_guid[0]), (state.headBlock.proof_db != null ? state.headBlock.proof_db : SS_NULL_SOUNDNESS_PROOF), (useLogs && state.headBlock.log_block_names_db != null ? state.headBlock.log_block_names_db : new ss_guid[0]), (useLogs && state.headBlock.logs_db != null ? state.headBlock.logs_db : new ss_log_entry[0]), (ss_signed_signature) null, _md);
                    RpcReply ss_resp = new RpcReply(result, gwReq.peer, gwReq.xact_id, ss_proc, gwReq.appId, (Object) null, (Thunk1<RpcReply>) null);
                    ss_replies = new TreeMap<NodeId, RpcReply>();
                    ss_replies.putAll(reply.ss_replies);
                    ss_replies.put(gw_node_id, ss_resp);
                    ss_proof_state = new TreeMap<NodeId, ProofState>();
                    ss_proof_state.putAll(reply.ss_proof_state);
                    ss_proof_state = GatewayHelper.getSSProofState(ss_replies, state.get_result_proof_ss);
                    if (logger.isInfoEnabled()) logger.info(method_tag + ": adding self to ss_proof_state." + " new ss_proof_state=" + ss_proof_state);
                }
                assert (prevProofCase != ProofState.NULL && prevProofCase != ProofState.INVALID) : method_tag + " (1) proof state is " + prevProofCase + " for extent " + XdrUtils.toString(state.extent_key_ss) + ", get_result=" + XdrUtils.toString(get_result) + ". headBlock=" + state.headBlock;
                if (prevProofCase == ProofState.EQUAL_PROOF || prevProofCase == ProofState.OLD) {
                    if (!isInconsistentState(ss_replies, ss_proof_state, state.get_result_proof_ss, _md, logger)) {
                        state.audit_result = getNormalResult(state);
                        send_reclaim_resource_timer(state, method_tag);
                    } else {
                        assert state.headBlock.repair_sig_db == null : method_tag + ": calling stop_config, but repair_sig is not" + " null " + XdrUtils.toString(state.headBlock.repair_sig_db) + ". state=" + state;
                        send_stop_config(state);
                    }
                } else {
                    assert (prevProofCase == ProofState.EQUAL_LOG || prevProofCase == ProofState.NEW) : method_tag + " (2) proof state is " + prevProofCase + " for extent " + XdrUtils.toString(state.extent_key_ss) + ", get_result=" + XdrUtils.toString(get_result) + ". headBlock=" + state.headBlock;
                    send_repair(state, true);
                }
            } else {
                if (!isResponsible(null, state) || state.headBlock == null || state.headBlock.proof_db == null) {
                    state.audit_result = getNormalResult(state);
                    send_reclaim_resource_timer(state, method_tag);
                    return;
                }
                send_stop_config(state);
            }
        }
    };

    /**********************************************************************/
    private void send_repair(RepairState state, boolean ss) {
        final String method_tag = tag + ".send_" + (ss ? "ss" : "gw") + "_repair";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": called for extent_key=0x" + XdrUtils.toString(state.extent_key) + ". state=" + state);
        XdrAble repair_args = null;
        if (ss) {
            ss_repair_args args = new ss_repair_args();
            args.config = state.get_result_proof_ss.config;
            args.prev_proof = state.get_result_proof_ss;
            repair_args = args;
        } else {
            assert state.audit_result != null : method_tag + ": state.audit_result is null for " + state;
            gw_repair_args args = convert(state.audit_result, gw_repair_args.class);
            repair_args = args;
        }
        ProcKey repair_proc = (ss ? getProcedureKey(Procedure.SS_REPAIR) : getProcedureKey(Procedure.GW_REPAIR));
        RpcCall repair_req = new RpcCall(gw_node_id, repair_proc, repair_args, appId, null, (SinkIF) null);
        repair_req.xact_id = _rand.nextInt();
        repair_req.caused_by_xact_id = state.caused_by_xact_id;
        repair_req.inbound = true;
        _inflight_xact_id.add(repair_req.xact_id);
        Object token = _acore.register_timer(REPAIR_TIMEOUT, curry(handle_repair_timeout_cb, state, repair_req, ss));
        repair_req.server_stub_cb = curry(handle_repair_cb, state, ss, token, repair_req);
        RpcServerStage.Handler handler = RpcServerStage.getInstance(gw_node_id).getHandler(repair_proc);
        assert handler != null : " handler is null for " + repair_proc;
        _cbuf.reset();
        XdrUtils.serialize(_cbuf, repair_req.args);
        log_req(false, repair_req, 0.0, _cbuf.size());
        handlerEnqueue(handler, repair_req);
    }

    private Thunk5<RepairState, Boolean, Object, RpcCall, RpcReply> handle_repair_cb = new Thunk5<RepairState, Boolean, Object, RpcCall, RpcReply>() {

        public void run(RepairState state, Boolean ss, Object token, RpcCall req, RpcReply reply) {
            final String method_tag = tag + ".handle_" + (ss ? "ss" : "gw") + "_repair_cb";
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": called for extent_key=0x" + XdrUtils.toString(state.extent_key) + ". state=" + state);
            if (!isCurrentReq(state, req.xact_id, token, method_tag)) return;
            _cbuf.reset();
            XdrUtils.serialize(_cbuf, reply.reply);
            log_resp(false, state.start_time_us, 0.0, req, reply, _cbuf.size());
            int reply_status = gw_status.GW_STATUS_ERR;
            if (!(reply.reply instanceof XdrVoid)) reply_status = (ss ? ((ss_repair_result) reply.reply).status : ((gw_repair_result) reply.reply).status);
            if (reply_status != ss_status.STATUS_OK && reply_status != gw_status.GW_STATUS_OK) logger.warn(method_tag + ": " + " rpc stage returned error. " + " reply=" + reply + ".  state=" + state);
            if (reply_status == ss_status.STATUS_OK || reply_status == gw_status.GW_STATUS_OK || state.attempts >= MAX_REPAIR_ATTEMPTS) {
                if (state.audit_result == null) {
                    assert ss : method_tag + ": state.audit_result is null," + " but attempted gw_repair " + state;
                    state.audit_result = getNormalResult(state);
                }
                if (!ss && reply_status == gw_status.GW_STATUS_OK && ((gw_repair_result) reply.reply).proof.config.config.seq_num > state.audit_result.latest_config.config.seq_num) {
                    ss_repair_audit_result old_audit_result = state.audit_result;
                    state.audit_result = new ss_repair_audit_result();
                    state.audit_result.status = old_audit_result.status;
                    state.audit_result.proof = convert(((gw_repair_result) reply.reply).proof, ss_soundness_proof.class);
                    state.audit_result.latest_config = state.audit_result.proof.config;
                    state.audit_result.log = new ss_log_entry[0];
                    state.audit_result.sig = SS_NULL_SIGNED_SIG;
                    if (logger.isInfoEnabled()) logger.info(method_tag + ": gw_repair succeeded. " + " Replacing old repair_audit_result with new. " + " new repair_audit_result=" + XdrUtils.toString(state.audit_result) + ".  old repair_audit_result=" + XdrUtils.toString(old_audit_result));
                }
                send_reclaim_resource_timer(state, method_tag);
            } else {
                if (logger.isInfoEnabled()) logger.info(method_tag + ": resending " + (ss ? "ss" : "gw") + "_repair" + " for extent " + XdrUtils.toString(state.extent_key) + ".  state=" + state);
                state.attempts++;
                send_repair(state, ss);
            }
        }
    };

    private Thunk3<RepairState, RpcCall, Boolean> handle_repair_timeout_cb = new Thunk3<RepairState, RpcCall, Boolean>() {

        public void run(RepairState state, RpcCall req, Boolean ss) {
            final String method_tag = tag + ".handle_" + (ss ? "ss" : "gw") + "_timeout_repair_cb";
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": called for extent_key=0x" + XdrUtils.toString(state.extent_key) + ". state=" + state);
            if (!isCurrentReq(state, req.xact_id, null, method_tag)) return;
            RpcReply resp = new RpcReply(XdrVoid.XDR_VOID, req);
            log_resp(true, state.start_time_us, 0.0, req, resp, 0L);
            if (state.attempts < MAX_REPAIR_ATTEMPTS) {
                if (logger.isInfoEnabled()) logger.info(method_tag + ": resending " + (ss ? "ss" : "gw") + "_repair" + " for extent " + XdrUtils.toString(state.extent_key) + ".  state=" + state);
                state.attempts++;
                send_repair(state, ss);
                return;
            } else {
                if (logger.isInfoEnabled()) logger.info(method_tag + ": attempted " + (ss ? "ss" : "gw") + "_repair" + MAX_REPAIR_ATTEMPTS + " times for extent " + XdrUtils.toString(state.extent_key) + " and all failed." + "  state=" + state);
                if (state.audit_result == null) {
                    assert ss : method_tag + ": state.audit_result is null," + " but attempted gw_repair " + state;
                    state.audit_result = getNormalResult(state);
                }
                send_reclaim_resource_timer(state, method_tag);
            }
        }
    };

    /**
   * <code>send_stop_config</code> is used to request that the
   * {@link antiquity.ss.impl.StorageServer} stop processing requests by
   * presenting it with {@link antiquity.ss.api.ss_stop_config_args}.
   * When we receive the {@link antiquity.ss.api.ss_stop_config_result}
   * we either immediately send a response if 
   * <code>{@link antiquity.ss.impl.RepairStage.RepairState#audits}!=null</code>
   * or initiate a {@link antiquity.gw.api.gw_repair_args} if 
   * <code>{@link antiquity.ss.impl.RepairStage.RepairState#audits}==null</code>.
   * Of course, if the {@link antiquity.ss.api.ss_stop_config_result} returns
   * an error then we respond immediately as well.
   *
   * @param state The {@link antiquity.ss.impl.RepairStage.RepairState}. */
    private void send_stop_config(RepairState state) {
        final String method_tag = tag + ".send_stop_config";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": called for extent_key=0x" + XdrUtils.toString(state.extent_key) + ". state=" + state);
        ss_stop_config_args stop_config_args = new ss_stop_config_args();
        stop_config_args.sig = SS_NULL_SIGNED_SIG;
        if (state.get_result_proof_ss != null) {
            stop_config_args.latest_config = state.get_result_proof_ss.config;
            stop_config_args.piggyback_prev_proof = state.get_result_proof_ss;
        } else {
            stop_config_args.latest_config = state.headBlock.config_db;
            stop_config_args.piggyback_prev_proof = state.headBlock.proof_db;
        }
        if (getSSIndex(_self_guid_ss, stop_config_args.latest_config.config.ss_set, true) < 0) {
            if (logger.isInfoEnabled()) logger.info(method_tag + ": for extent 0x" + GuidTools.guid_to_string(state.extent_key_sha1) + " client_id 0x" + GuidTools.guid_to_string(state.client_id_sha1) + ", self node " + GuidTools.guid_to_string(_self_guid_sha1) + " not contained in latest ss set config " + XdrUtils.toString(stop_config_args.latest_config));
            state.audit_result = getNormalResult(state);
            send_reclaim_resource_timer(state, method_tag);
            return;
        }
        ProcKey stop_config_proc = getProcedureKey(Procedure.SS_STOP_CONFIG);
        RpcCall stop_config_req = new RpcCall(gw_node_id, stop_config_proc, stop_config_args, appId, null, (SinkIF) null);
        stop_config_req.xact_id = _rand.nextInt();
        stop_config_req.caused_by_xact_id = state.caused_by_xact_id;
        stop_config_req.inbound = true;
        _inflight_xact_id.add(stop_config_req.xact_id);
        Object token = _acore.register_timer(STOP_CONFIG_TIMEOUT, curry(handle_stop_config_timeout_cb, state, stop_config_req, now_us()));
        stop_config_req.server_stub_cb = curry(handle_stop_config_cb, state, now_us(), token, stop_config_req);
        _cbuf.reset();
        XdrUtils.serialize(_cbuf, stop_config_req.args);
        log_req(false, stop_config_req, 0.0, _cbuf.size());
        StorageServer stage = StorageServer.getInstance(my_node_id);
        stage.triggerStopConfig(my_node_id, logger, method_tag, stop_config_req);
    }

    private Thunk5<RepairState, Long, Object, RpcCall, RpcReply> handle_stop_config_cb = new Thunk5<RepairState, Long, Object, RpcCall, RpcReply>() {

        public void run(RepairState state, Long start_time_us, Object token, RpcCall req, RpcReply reply) {
            final String method_tag = tag + ".handle_stop_config_cb";
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": called for extent_key=0x" + XdrUtils.toString(state.extent_key) + ". state=" + state);
            if (!isCurrentReq(state, req.xact_id, token, method_tag)) return;
            _cbuf.reset();
            XdrUtils.serialize(_cbuf, reply.reply);
            log_resp(false, start_time_us, 0.0, req, reply, _cbuf.size());
            assert state.audit_result == null : method_tag + ": state.audit_result is not null" + state;
            int stop_config_status = ss_status.STATUS_ERR_DB_ERROR;
            if (!(reply.reply instanceof XdrVoid)) stop_config_status = ((ss_stop_config_result) reply.reply).status;
            if (stop_config_status == ss_status.STATUS_OK) {
                ss_stop_config_result stop_config_result = (ss_stop_config_result) reply.reply;
                state.audit_result = convert(stop_config_result, ss_repair_audit_result.class);
                if (state.audits == null) {
                    send_repair(state, false);
                } else {
                    send_reclaim_resource_timer(state, method_tag);
                }
            } else {
                logger.warn(method_tag + ": " + " storage server returned error. " + " reply=" + reply + ".  state=" + state);
                state.audit_result = getErrorResult(state, stop_config_status);
                send_reclaim_resource_timer(state, method_tag);
            }
        }
    };

    private Thunk3<RepairState, RpcCall, Long> handle_stop_config_timeout_cb = new Thunk3<RepairState, RpcCall, Long>() {

        public void run(RepairState state, RpcCall req, Long start_time_us) {
            final String method_tag = tag + ".handle__timeout_stop_config_cb";
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": called for extent_key=0x" + XdrUtils.toString(state.extent_key) + ". state=" + state);
            if (!isCurrentReq(state, req.xact_id, null, method_tag)) return;
            RpcReply resp = new RpcReply(XdrVoid.XDR_VOID, req);
            log_resp(true, start_time_us, 0.0, req, resp, 0L);
            state.audit_result = getErrorResult(state, ss_status.STATUS_ERR_DB_TIMEOUT);
            send_reclaim_resource_timer(state, method_tag);
        }
    };

    /**********************************************************************/
    private void send_reclaim_resource_timer(RepairState state, String caller) {
        final String method_tag = caller + ".reclaim_repair_resource_timer";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": called for extent " + XdrUtils.toString(state.extent_key) + ", state=" + state + ". Took " + ((now_us() - state.start_time_us) / 1000.0) + "ms");
        assert !state.reclaimed : "state already reclaimed " + state;
        state.reclaimed = true;
        Object token2 = _acore.register_timer(RECLAIM_TIMEOUT, curry(reclaim_repair_resources_cb, state));
        if (state.failure_detectors != null) {
            for (DDRepairObjReq req : state.failure_detectors) dispatch(new DDRepairObjResp(req.peer, req.getObjguid(), req.getTag(), true, true, false));
        }
        if (state.audits != null) {
            for (RpcCall req : state.audits) {
                RpcReply resp = new RpcReply(state.audit_result, req);
                req.server_stub_cb.run(resp);
            }
        }
        if (state.periodic_repair) {
            long periodic_repair_time_ms = (PERIODIC_REPAIR_TIME / 2L) + (Math.abs(_rand.nextLong()) % (PERIODIC_REPAIR_TIME / 2L));
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": set next periodic repair " + " time in " + (periodic_repair_time_ms / 1000.0) + " s from now.  Will search in range 0x" + GuidTools.guid_to_string(_nextGuid) + " to 0x" + GuidTools.guid_to_string(MAX_GUID) + ".");
            _acore.register_timer(periodic_repair_time_ms, periodic_repair);
        }
        if (_rand.nextDouble() <= EXTENT_REPUB_FREQ && state.periodic_repair && isResponsible(state.get_result_proof_ss, state)) {
            send_publish(state);
        }
        state.finalize();
    }

    /**
   * <code>reclaim_repair_resources_cb</code> method called after repair
   * completed or timed out.
   *
   * @param state {@link antiquity.ss.impl.RepairState.RepairState}. */
    private Thunk1<RepairState> reclaim_repair_resources_cb = new Thunk1<RepairState>() {

        public void run(RepairState state) {
            final String method_tag = tag + ".reclaim_repair_resources_cb";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": called for " + state + ". Took " + ((now_us() - state.start_time_us) / 1000.0) + "ms");
            assert _repair.remove(state.extent_key_sha1) == state : "No longer repairing " + state;
        }
    };

    /**
   * <code>handle_gateway_helper_timeout_cb</code> method called when a
   * procedure timeout.  Method sends back an error status to requestor.
   *
   * @param state {@link antiquity.ss.impl.RepairStage.RepairState}.
   * @param method {@link java.lang.String} of calling method. */
    private Thunk3<RepairState, RpcCall, String> handle_gateway_helper_timeout_cb = new Thunk3<RepairState, RpcCall, String>() {

        public void run(RepairState state, RpcCall req, String method) {
            final String method_tag = tag + ".handle_gateway_helper_timeout_cb";
            if (logger.isInfoEnabled()) logger.info(method_tag + "." + method + ": timeout xact_id=0x" + Integer.toHexString(req.xact_id) + " state=" + state + ". Took " + ((now_us() - state.start_time_us) / 1000.0) + "ms");
            if (!isCurrentReq(state, req.xact_id, null, method_tag)) return;
            RpcReply resp = new RpcReply(XdrVoid.XDR_VOID, req);
            log_resp(true, state.start_time_us, 0.0, req, resp, 0L);
            state.audit_result = getErrorResult(state, ss_status.STATUS_ERR_TIMEOUT);
            send_reclaim_resource_timer(state, method_tag);
        }
    };

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
   * <code>send_publish</code> stores into the distributed directory, for a 
   * given {@link antiquity.ss.api.ss_guid extent_key}, a 
   * {@link antiquity.ss.impl.StorageServer.Tag location-pointer}.
   *
   * @param state {@link antiquity.ss.impl.RepairStage.RepairState state}. */
    private void send_publish(RepairState state) {
        final String method_tag = tag + ".send_publish";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": sending publish for extent key=" + XdrUtils.toString(state.extent_key));
        Object token = _acore.register_timer(PUBLISH_TIMEOUT, curry(publish_timeout_cb, state));
        ss_signed_certificate cert = state.headBlock.cert_db;
        ss_signed_ss_set_config config = state.headBlock.config_db;
        long timestamp = Math.max(cert.cert.timestamp, config.config.timestamp);
        int cert_ttl_s = (int) ((cert.cert.expire_time - timestamp) / 1000L);
        int default_ttl_s = (int) ((now_ms() - timestamp) / 1000L) + DEFAULT_PUBLISH_TTL + _rand.nextInt(DEFAULT_PUBLISH_TTL);
        int ttl_s = Math.min(cert_ttl_s, default_ttl_s);
        DDPublishReq publishreq = new DDPublishReq(guidToSecureHash(state.extent_key), new StorageServer.Tag((short) (config.config.threshold - 1)), true, timestamp * 1000L, ttl_s, ttl_s, ttl_s, null, (SinkIF) null);
        publishreq.cb = curry(publish_cb, state, now_us(), token);
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": sending " + (publishreq.publish ? "" : "un") + "publish for" + " guid=" + publishreq.guid + " tag=" + publishreq.tag.getClass().getName() + " with ts=" + publishreq.time_usec + "us and ttl=" + publishreq.root_ttl + "s (ttl options were  " + cert_ttl_s + "s and " + default_ttl_s + "s).");
        dispatch(publishreq);
    }

    /**
   * <code>publish_cb</code> is the callback method that is
   * called after the distributed directory has returned from storing a
   * {@link antiquity.ss.impl.StorageServer.Tag location-pointer}, for a given 
   * {@link antiquity.ss.api.ss_guid extent_key}.
   *
   * @param state {@link antiquity.ss.impl.RepairStage.RepairState state}.
   * @param start_time_us start time of publish operation (in us).
   * @param token Opaque token to cancel timer.
   * @param publishreq {@link dd.api.DDPublishReq}. 
   * @param publishresp {@link dd.api.DDPublishResp}. */
    private Thunk5<RepairState, Long, Object, DDPublishReq, DDPublishResp> publish_cb = new Thunk5<RepairState, Long, Object, DDPublishReq, DDPublishResp>() {

        public void run(RepairState state, Long start_time_us, Object token, DDPublishReq publishreq, DDPublishResp publishresp) {
            final String method_tag = tag + ".publish_cb";
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": called for extent " + XdrUtils.toString(state.extent_key) + " resp=" + publishresp + ".  Entire put operation took " + ((now_us() - start_time_us) / 1000.0) + "ms.");
            if (!isCurrentReq(null, null, token, method_tag)) return;
        }
    };

    /**
   * <code>publish_timeout_cb</code> is the callback method that is
   * called after the distributed directory has failed to return from storing a
   * {@link antiquity.ss.impl.StorageServer.Tag location-pointer}, for a given 
   * {@link antiquity.ss.api.ss_guid extent_key}.
   *
   * @param state {@link antiquity.ss.impl.RepairStage.RepairState state}. */
    Thunk1<RepairState> publish_timeout_cb = new Thunk1<RepairState>() {

        public void run(RepairState state) {
            final String method_tag = tag + ".publish_timeout_cb";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": called " + state);
        }
    };

    /**********************************************************************/
    private void handlerEnqueue(RpcServerStage.Handler handler, RpcCall req) {
        if (handler.cb != null) {
            if (logger.isDebugEnabled()) logger.debug("invoking handler for " + req);
            handler.cb.run(req);
        } else if (handler.sink != null) {
            if (logger.isDebugEnabled()) logger.debug("enqueuing " + req + " to handler sink " + handler.sink);
            enqueue(req, handler.sink);
        } else {
            BUG("either cb or sink should be defined for handler " + handler + " req=" + req);
        }
    }

    private boolean isCurrentReq(RepairState state, Integer xact_id, Object token, String caller) {
        final String method_tag = caller + ".isCurrentReq";
        if (state != null && (state.reclaimed || !_repair.containsKey(state.extent_key_sha1))) {
            if (logger.isInfoEnabled()) logger.info(method_tag + ": already reclaiming resources for " + state);
            return false;
        }
        if (xact_id != null && !_inflight_xact_id.remove(xact_id)) {
            if (logger.isInfoEnabled()) logger.info(method_tag + ": already removed xact_id=0x" + Integer.toHexString(xact_id) + ". state=" + state);
            return false;
        }
        if (token != null) {
            try {
                _acore.cancel_timer(token);
            } catch (Throwable e) {
                logger.warn(method_tag + ": cancel_timer failed " + e + " for token=" + token + ", state=" + state + ". Must have already timed out since entire operation took " + ((now_us() - state.start_time_us) / 1000.0) + "ms.");
                return false;
            }
        }
        return true;
    }

    private ss_repair_audit_result getNormalResult(RepairState state) {
        ss_repair_audit_result result = new ss_repair_audit_result();
        result.status = ss_status.STATUS_OK;
        if (state.headBlock.config_db != null) {
            result.latest_config = convert(state.headBlock.config_db, ss_signed_ss_set_config.class);
        } else {
            result.latest_config = SS_NULL_SIGNED_CONFIG;
        }
        if (state.headBlock.proof_db != null) {
            result.proof = convert(state.headBlock.proof_db, ss_soundness_proof.class);
            if (state.headBlock.repair_sig_db != null) {
                result.sig = state.headBlock.repair_sig_db;
            } else {
                result.sig = SS_NULL_SIGNED_SIG;
            }
        } else {
            assert state.headBlock.repair_sig_db == null : "state.repair_sig_db!=null, but state.proof_db is null.";
            result.proof = SS_NULL_SOUNDNESS_PROOF;
            result.sig = SS_NULL_SIGNED_SIG;
        }
        if (state.headBlock.logs_db != null && state.headBlock.logs_db.length > 0) {
            result.log = new ss_log_entry[state.headBlock.logs_db.length];
            for (int i = 0; i < result.log.length; i++) result.log[i] = convert(state.headBlock.logs_db[i], ss_log_entry.class);
        } else {
            result.log = new ss_log_entry[0];
        }
        return result;
    }

    private ss_repair_audit_result getErrorResult(RepairState state, int error) {
        ss_repair_audit_result audit_result = new ss_repair_audit_result();
        audit_result.status = error;
        audit_result.sig = SS_NULL_SIGNED_SIG;
        audit_result.latest_config = (state.headBlock == null || state.headBlock.config_db == null ? SS_NULL_SIGNED_CONFIG : state.headBlock.config_db);
        audit_result.proof = (state.headBlock == null || state.headBlock.proof_db == null ? SS_NULL_SOUNDNESS_PROOF : state.headBlock.proof_db);
        audit_result.log = (state.headBlock == null || state.headBlock.logs_db == null ? new ss_log_entry[0] : state.headBlock.logs_db);
        return audit_result;
    }

    private boolean isResponsible(ss_soundness_proof proof, RepairState state) {
        return (proof != null && getSSIndex(_self_guid_ss, proof.config.config.ss_set, true) >= 0) || (state.headBlock.logs_db != null && state.headBlock.logs_db.length > 0 && getSSIndex(_self_guid_ss, state.headBlock.logs_db[0].config.config.ss_set, true) >= 0) || (state.headBlock.proof_db != null && getSSIndex(_self_guid_ss, state.headBlock.proof_db.config.config.ss_set, true) >= 0);
    }

    /** 
   * Simple class to store state required to process a
   * {@link dd.api.DDRepairObjReq}. */
    private class RepairState {

        /** The {@link dd.api.DDRepairObjReq}. */
        public LinkedList<DDRepairObjReq> failure_detectors;

        /** The {@link dd.api.DDRepairObjReq}. */
        public LinkedList<RpcCall> audits;

        /** Flag that <code>RepairState</code> is due to periodic repair. */
        public boolean periodic_repair;

        /** <code>RepairState</code> is due to locally triggered process. */
        public boolean locally_triggered;

        /** {@link ss_repair_audit_result}. */
        public ss_repair_audit_result audit_result;

        /** caused_by_xact_id */
        public int caused_by_xact_id;

        /** 
     * {@link antiquity.gw.api.gw_guid extent_key} of the original
     * {@link antiquity.rpc.api.RpcCall}. */
        public gw_guid extent_key;

        /** 
     * {@link antiquity.gw.api.gw_guid extent_key} of the original
     * {@link antiquity.rpc.api.RpcCall}. */
        public ss_guid extent_key_ss;

        /** <code>extent_key</code> of type {@link java.math.BigInteger}. */
        public BigInteger extent_key_sha1;

        /** 
     * {@link antiquity.gw.api.gw_guid client_id} of the original
     * {@link antiquity.rpc.api.RpcCall}. */
        public gw_guid client_id;

        /** 
     * {@link antiquity.ss.api.ss_guid client_id} of the original
     * {@link antiquity.rpc.api.RpcCall}. */
        public ss_guid client_id_ss;

        /** <code>client_id</code> of type {@link java.math.BigInteger}. */
        public BigInteger client_id_sha1;

        /** start times used to process req.*/
        public long start_time_us;

        /** Number of repair attempts. */
        public int attempts;

        /** Flag indicated that state has already been marked to be reclaimed. */
        public boolean reclaimed;

        /** {@link antiquity.ss.impl.HeadBlock} of extent. */
        public HeadBlock headBlock;

        /** 
     * {@link antiquity.gw.api.ss_soundnes_proof} contained in
     * {@link antiquity.gw.impl.GatewayHelper.Resp} 
     * {@link antiquity.util.AntiquityUtils.convert converted} to a
     * {@link antiquity.gw.api.ss_soundnes_proof}. */
        public ss_soundness_proof get_result_proof_ss;

        /** Constructor: Creates a new <code>GatewayState</code>. */
        public RepairState(DDRepairObjReq req) {
            failure_detectors = new LinkedList<DDRepairObjReq>();
            audits = (LinkedList<RpcCall>) null;
            periodic_repair = false;
            locally_triggered = false;
            caused_by_xact_id = _rand.nextInt();
            failure_detectors.add(req);
            extent_key = AntiquityUtils.secureHashToGuid(req.getObjguid(), gw_guid.class);
            extent_key_ss = AntiquityUtils.convert(extent_key, ss_guid.class);
            extent_key_sha1 = GuidTools.secure_hash_to_big_integer(req.getObjguid());
            client_id = GW_NULL_GUID;
            client_id_ss = SS_NULL_GUID;
            client_id_sha1 = BigInteger.ZERO;
            start_time_us = now_us();
            attempts = 0;
            reclaimed = false;
            headBlock = null;
            audit_result = null;
        }

        /** Constructor: Creates a new <code>GatewayState</code>. */
        public RepairState(RpcCall req) {
            assert req.args instanceof ss_repair_audit_args : "req.args not instanceof ss_repair_audit_args. req=" + req;
            ss_repair_audit_args args = (ss_repair_audit_args) req.args;
            failure_detectors = (LinkedList<DDRepairObjReq>) null;
            audits = new LinkedList<RpcCall>();
            periodic_repair = false;
            locally_triggered = false;
            caused_by_xact_id = req.xact_id;
            audits.add(req);
            extent_key = convert(args.extent_key, gw_guid.class);
            extent_key_ss = args.extent_key;
            extent_key_sha1 = guidToBigInteger(extent_key);
            client_id = convert(args.client_id, gw_guid.class);
            client_id_ss = args.client_id;
            client_id_sha1 = guidToBigInteger(args.client_id);
            start_time_us = now_us();
            attempts = 0;
            reclaimed = false;
            headBlock = null;
            audit_result = null;
        }

        /** Constructor: Creates a new <code>GatewayState</code>. */
        public RepairState(boolean periodic, gw_guid extent_key, ss_guid extent_key_ss, BigInteger extent_key_sha1, gw_guid client_id, ss_guid client_id_ss, BigInteger client_id_sha1) {
            this.failure_detectors = (LinkedList<DDRepairObjReq>) null;
            this.audits = (LinkedList<RpcCall>) null;
            this.extent_key = extent_key;
            this.extent_key_ss = extent_key_ss;
            this.extent_key_sha1 = extent_key_sha1;
            this.client_id = (client_id != null ? client_id : GW_NULL_GUID);
            this.client_id_ss = (client_id_ss != null ? client_id_ss : SS_NULL_GUID);
            this.client_id_sha1 = (client_id_sha1 != null ? client_id_sha1 : BigInteger.ZERO);
            periodic_repair = periodic;
            locally_triggered = !periodic;
            caused_by_xact_id = _rand.nextInt();
            start_time_us = now_us();
            attempts = 0;
            reclaimed = false;
            headBlock = null;
            audit_result = null;
        }

        /** Specified by java.lang.Object */
        public void finalize() {
            failure_detectors = null;
            audits = null;
            attempts = 0;
            headBlock = null;
        }

        /** Specified by java.lang.Object */
        public String toString() {
            return new String("(RepairState for extent_key=0x" + GuidTools.guid_to_string(extent_key_sha1) + " client_id=0x" + GuidTools.guid_to_string(client_id_sha1) + " caused_by_xact_id=0x" + Integer.toHexString(caused_by_xact_id) + " periodic_repair=" + periodic_repair + " locally_triggered=" + locally_triggered + " attempts=" + attempts + " reclaimed=" + reclaimed + " failure_detectors=" + failure_detectors + " audits=" + audits + ". Elapsed time = " + ((now_us() - start_time_us) / 1000.0) + "ms)");
        }
    }
}
