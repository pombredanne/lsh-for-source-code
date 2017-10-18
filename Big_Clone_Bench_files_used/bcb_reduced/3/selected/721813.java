package antiquity.cr.impl;

import antiquity.util.XdrUtils;
import antiquity.util.KeyTriple;
import antiquity.util.ValuePair;
import antiquity.util.AntiquityUtils;
import static antiquity.util.AntiquityUtils.computeGuid;
import static antiquity.util.AntiquityUtils.readPublicKey;
import static antiquity.util.AntiquityUtils.readPrivateKey;
import static antiquity.util.AntiquityUtils.createSignature;
import static antiquity.util.AntiquityUtils.verifySignature;
import static antiquity.util.AntiquityUtils.nodeIdToAddress;
import static antiquity.util.AntiquityUtils.guidToBigInteger;
import static antiquity.util.AntiquityUtils.bigIntegerToGuid;
import static antiquity.util.AntiquityUtils.secureHashToGuid;
import static antiquity.util.AntiquityUtils.guidToSecureHash;
import static antiquity.util.AntiquityUtils.getCRProcedureMap;
import static antiquity.util.AntiquityUtils.checkSoundnessProof;
import static antiquity.util.AntiquityUtils.byteArrayToBigInteger;
import static antiquity.util.AntiquityUtils.CR_NULL_GUID;
import static antiquity.util.AntiquityUtils.CR_NULL_PUBLIC_KEY;
import static antiquity.util.AntiquityUtils.CR_NULL_SIGNED_CERT;
import static antiquity.util.AntiquityUtils.CR_NULL_SIGNED_CONFIG;
import static antiquity.util.AntiquityUtils.CR_NULL_SOUNDNESS_PROOF;
import antiquity.cr.api.cr_id;
import antiquity.cr.api.cr_guid;
import antiquity.cr.api.cr_status;
import antiquity.cr.api.cr_address;
import antiquity.cr.api.cr_public_key;
import antiquity.cr.api.cr_certificate;
import antiquity.cr.api.cr_soundness_proof;
import antiquity.cr.api.cr_ss_set_config;
import antiquity.cr.api.cr_signed_certificate;
import antiquity.cr.api.cr_signed_ss_set_config;
import antiquity.cr.api.cr_get_latest_config_args;
import antiquity.cr.api.cr_get_latest_config_result;
import antiquity.cr.api.cr_renew_args;
import antiquity.cr.api.cr_renew_result;
import antiquity.cr.api.cr_repair_args;
import antiquity.cr.api.cr_repair_result;
import antiquity.cr.api.cr_create_ss_set_args;
import antiquity.cr.api.cr_create_ss_set_result;
import antiquity.cr.api.cr_get_latest_config_args;
import antiquity.cr.api.cr_get_latest_config_result;
import static antiquity.cr.api.cr_api.CR_API;
import static antiquity.cr.api.cr_api.CR_GUID_SIZE;
import static antiquity.cr.api.cr_api.CR_API_VERSION;
import static antiquity.cr.api.cr_api.CR_KEY_VERIFIED;
import static antiquity.cr.api.cr_api.CR_HASH_VERIFIED;
import static antiquity.cr.api.cr_api.CR_REDUNDANCY_TYPE_REPL;
import static antiquity.cr.api.cr_api.cr_null_1;
import static antiquity.cr.api.cr_api.cr_get_latest_config_1;
import static antiquity.cr.api.cr_api.cr_renew_1;
import static antiquity.cr.api.cr_api.cr_repair_1;
import static antiquity.cr.api.cr_api.cr_create_ss_set_1;
import static antiquity.ss.impl.RepairStage.verifyRepairSignature;
import antiquity.rpc.api.RpcKey;
import antiquity.rpc.api.RpcCall;
import antiquity.rpc.api.RpcReply;
import antiquity.rpc.api.RpcRegisterReq;
import antiquity.rpc.api.RpcRegisterResp;
import static antiquity.rpc.api.ProcInfo.ProcKey;
import static antiquity.rpc.api.ProcInfo.ProcValue;
import dd.dissemination.DisseminationSetPredicate;
import dd.dissemination.RequestDisseminationSetMsg;
import dd.dissemination.RequestDisseminationSetResponseMsg;
import dd.api.DDReadyMsg;
import dd.host.api.HostInfo;
import dd.host.api.GetHostReq;
import dd.host.api.GetHostResp;
import dd.host.api.HostAppFilter;
import dd.kbr.api.KbrAppRegReq;
import dd.kbr.api.KbrAppRegResp;
import dd.kbr.api.KbrNeighborInfo;
import dd.kbr.api.KbrRootSetChanged;
import ostore.util.NodeId;
import ostore.util.SHA1Hash;
import ostore.util.SecureHash;
import ostore.util.StandardStage;
import ostore.dispatch.Filter;
import bamboo.util.GuidTools;
import bamboo.lss.ASyncCore;
import static bamboo.util.Curry.curry;
import static bamboo.util.Curry.Thunk1;
import static bamboo.util.Curry.Thunk2;
import static bamboo.util.Curry.Thunk4;
import static bamboo.util.Curry.Thunk5;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.XdrVoid;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandlerException;
import seda.sandStorm.api.StagesInitializedSignal;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.math.BigInteger;
import java.util.Set;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.apache.log4j.Logger;

/**
 * The <code>StorageSetCreator</code> acts a <i>storage provider</i>
 * that creates a storage set, set of nodes tasked storing an extent.
 * After allocating some nodes to be the storage set, the 
 * <code>StorageSetCreator</code> then signs a storage set config 
 * that includes all the names of the storage servers.  This config 
 * can be used to prove to the storage server that it needs to store 
 * the extent.  Furthermore, this config can be used to prove to 
 * the client that that storage provider has assigned nodes to store 
 * the extent.
 *
 * @author Hakim Weatherspoon
 * @version $Id: StorageSetCreator.java,v 1.3 2007/07/12 18:56:54 hweather Exp $
 */
public class StorageSetCreator extends ostore.util.StandardStage {

    /** Unique identifier for <code>this</code> stage. */
    private static final long appId = bamboo.router.Router.app_id(StorageSetCreator.class);

    private static final int MIN_TTL = 120;

    /** 
   * Flag indicating if the nodes that are currently down should be
   * "remembered" in the next configuration. This only affect hash-verified
   * extents where the extents are replicated (not erasure coded). */
    private boolean REMEMBER;

    /** Flag indicating need to send message to admin to request signature. */
    private boolean SEND_FOR_ADMIN_SIG;

    /** {@link ostore.util.NodeId} of admin that signs configs. */
    private NodeId ADMIN;

    /** Flag indicating if the DHT root set should be used as the storage set. */
    private boolean USE_DHT_ROOT_SET;

    /** Max number of attempts to retrieve dissemination sets. */
    private int MAX_ATTEMPTS;

    /** Max time (in ms) or a request to retrieve a dissemination set. */
    private long TIMEOUT_MS, PING_TIMEOUT, ADMIN_TIMEOUT;

    /** Root set in terms of predecessor and successors. */
    private cr_id[] _rootset;

    /** Root set in terms of predecessor and successors. */
    private Map<BigInteger, NodeId> _rootsetMap;

    /** {@link bamboo.lss.ASyncCore} used for timeouts. */
    private ASyncCore _acore;

    /** DD is ready */
    private boolean _ddReady;

    /** identifier of local node. */
    private SecureHash _self_guid;

    /** identifier of local node. */
    private BigInteger _self_guid_sha1;

    /** identifier of local node. */
    private cr_guid _self_guid_cr;

    /**
   * Both {@link antiquity.cr.api.cr_guid} and 
   * {@link antiquity.cr.api.cr_address} for <code>self</code>.
   * Note: <code>_self_cr_id.addr.port==my_node_id.port+1</code> */
    private cr_id _self_cr_id;

    /** Block events b/c DD was not ready */
    private Map<RpcKey, RpcCall> _pendingRpcCalls;

    private Map<BigInteger, LinkedList<ReqState>> _waitingReqs;

    private Map<BigInteger, ReqState> _hostDbRequests;

    private Map<BigInteger, ReqState> _pings;

    /** Block events b/c DD was not ready */
    private Vector<QueueElementIF> _blockedEvents;

    /** All local stages have been initialized */
    private boolean _stagesInitialized;

    /** 
   * All remote procedure calls have been registered and 
   * {@link dd.kbr.api.KbrAppRegReq} */
    private boolean _registeredRpcServer, _registeredRpcClient, _registeredKbr;

    /** {@link java.security.MessageDigest} instance. */
    private MessageDigest _md;

    private Random _rand;

    private Signature _sigEngine;

    private Signature _verifyEngine;

    private KeyFactory _keyFactory;

    private PrivateKey _keyPair_skey;

    private PublicKey _keyPair_pkey;

    private cr_public_key _pkey;

    /** Constructor: Creates a new <code>StorageSetCreator</code> stage. */
    public StorageSetCreator() throws Exception {
        event_types = new Class[] { dd.api.DDReadyMsg.class, seda.sandStorm.api.StagesInitializedSignal.class, dd.kbr.api.KbrRootSetChanged.class };
        inb_msg_types = new Class[] { dd.dissemination.RequestDisseminationSetResponseMsg.class, antiquity.cr.impl.StorageSetPing.class, antiquity.cr.impl.StorageSetPong.class };
        _acore = bamboo.lss.DustDevil.acore_instance();
        _pendingRpcCalls = new HashMap<RpcKey, RpcCall>();
        _waitingReqs = new HashMap<BigInteger, LinkedList<ReqState>>();
        _hostDbRequests = new HashMap<BigInteger, ReqState>();
        _pings = new HashMap<BigInteger, ReqState>();
        _blockedEvents = new Vector<QueueElementIF>();
        _md = MessageDigest.getInstance("SHA");
        if (logger.isInfoEnabled()) logger.info("StorageSetCreator.<init>: Creating signature engine...");
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
        if (logger.isInfoEnabled()) logger.info("StorageSetCreator.<init>: done.");
        if (logger.isInfoEnabled()) logger.info("Initializing StorageSetCreator...");
    }

    /**
   * <code>checkReq</code> returns the
   * {@link antiquity.cr.api.cr_guid extent_key}
   * Additionally, <code>checkReq</code> verifies that the 
   * {@link antiquity.cr.api.cr_guid extent_key} can be computed from the
   * arguements of the <code>put, create, append, truncate, snapshot,
   * renew,</code> and <code>repair</code> calls.
   *
   * @param logger {@link org.apache.log4j.Logger}.
   * @param req {@link antiquity.rpc.api.RpcCall}.
   * @param md {@link java.security.MessageDigest} used to verify extent name.
   * @param keyFactory {@link java.security.KeyFactory}  used to generate
   *                     {@link java.security.PublicKey} from 
   *                     {@link antiquity.cr.api.cr_public_key}.
   * @param verifyEngine {@link java.secuirty.Singature engine} used to verify
   *                      signatures.
   *
   * @return {@link antiquity.cr.api.cr_guid extent_key}. */
    public static ValuePair<cr_guid, cr_soundness_proof> checkReq(Logger logger, RpcCall req, long start_time_us, MessageDigest md, KeyFactory keyFactory, Signature verifyEngine) {
        final String method_tag = StorageSetCreator.class.getName() + ".checkReq";
        cr_guid extent_key = null;
        cr_soundness_proof latest_proof = null;
        switch(req.proc.getProcNum()) {
            case cr_get_latest_config_1:
                {
                    cr_get_latest_config_args args = (cr_get_latest_config_args) req.args;
                    extent_key = args.extent_key;
                }
                break;
            case cr_renew_1:
                {
                    cr_renew_args args = (cr_renew_args) req.args;
                    cr_signed_certificate cert = args.cert;
                    cr_signed_ss_set_config latest_config = args.latest_config;
                    assert cert.cert.expire_time > (start_time_us / 1000L) : "cert.expire_time=" + cert.cert.expire_time + "ms < now=" + (start_time_us / 1000L) + "ms";
                    assert latest_config.config.expire_time > (start_time_us / 1000L) : "latest_config.config.expire_time=" + latest_config.config.expire_time + "ms < now=" + (start_time_us / 1000L) + "ms";
                    extent_key = latest_config.config.extent_key;
                    Map<BigInteger, cr_soundness_proof> sig_proofs = null;
                    SortedMap<KeyTriple<Long, Long, BigInteger>, cr_soundness_proof> latest_proofs = null;
                    assert !AntiquityUtils.equals(latest_config.config.public_key, CR_NULL_PUBLIC_KEY) && !AntiquityUtils.equals(latest_config.config.extent_key, CR_NULL_GUID) && !AntiquityUtils.equals(latest_config.config.client_id, CR_NULL_GUID) && verifySignature(latest_config.config, latest_config.signature, latest_config.config.public_key, keyFactory, verifyEngine) : method_tag + ": Verification failed for latest_config" + XdrUtils.toString(latest_config);
                    for (cr_soundness_proof proof : args.proof) {
                        assert XdrUtils.equals(extent_key, proof.config.config.extent_key) : "Extent key=" + XdrUtils.toString(extent_key) + " does not match proof.config.extent_key=" + XdrUtils.toString(proof.config.config.extent_key);
                        assert checkSoundnessProof(proof, true, false, md, keyFactory, verifyEngine) : "Invalid proof " + XdrUtils.toString(proof);
                        md.update(XdrUtils.serialize(proof.cert.cert));
                        md.update(XdrUtils.serialize(proof.config.config));
                        BigInteger hash = byteArrayToBigInteger(md.digest());
                        if (sig_proofs == null) {
                            sig_proofs = new LinkedHashMap<BigInteger, cr_soundness_proof>();
                            latest_proofs = new TreeMap<KeyTriple<Long, Long, BigInteger>, cr_soundness_proof>();
                        }
                        if (!sig_proofs.containsKey(hash)) {
                            sig_proofs.put(hash, proof);
                            KeyTriple<Long, Long, BigInteger> key = new KeyTriple<Long, Long, BigInteger>(proof.config.config.seq_num, proof.cert.cert.seq_num, hash);
                            assert !latest_proofs.containsKey(key) : method_tag + ": proofs map already contains key " + key + ".  value=" + XdrUtils.toString(latest_proofs.get(key)) + ", but want to put " + XdrUtils.toString(proof);
                            latest_proofs.put(key, proof);
                        }
                    }
                    int num_repair_sigs = 0;
                    for (int i = 0; i < args.sig.length; i++) {
                        BigInteger hash = guidToBigInteger(args.sig_proof_hash[i]);
                        cr_soundness_proof proof = sig_proofs.get(hash);
                        assert proof != null : "proof is null for" + " repair_args.sig[" + i + "]=" + XdrUtils.toString(args.sig[i]) + " repair_args.sig_proof_hash[" + i + "]=0x" + GuidTools.guid_to_string(hash);
                        assert verifyRepairSignature(logger, args.sig[i], proof, latest_config, md, keyFactory, verifyEngine) : "Invalid repair_args.sig[" + i + "]=" + XdrUtils.toString(args.sig[i]) + " repair_args.sig_proof_hash[" + i + "]=0x" + GuidTools.guid_to_string(hash) + " proof=" + XdrUtils.toString(proof);
                        num_repair_sigs++;
                    }
                    assert (latest_config.config.threshold - 1) % 2 == 0 : "low_watermark=" + latest_config.config.threshold + " is not an odd number";
                    int f = (latest_config.config.threshold - 1) / 2;
                    assert num_repair_sigs >= (f + 1) : method_tag + ":not enough valid repair signatures. " + num_repair_sigs + " < (f=" + f + "+1)=" + (f + 1);
                    KeyTriple<Long, Long, BigInteger> latestProofKey = latest_proofs.lastKey();
                    latest_proof = latest_proofs.get(latestProofKey);
                    cr_signed_certificate cert_cloned = XdrUtils.clone(cert);
                    cert_cloned.cert.timestamp = latest_proof.cert.cert.timestamp;
                    cert_cloned.cert.expire_time = latest_proof.cert.cert.expire_time;
                    cert_cloned.cert.seq_num = latest_proof.cert.cert.seq_num;
                    cert_cloned.cert.user_data = latest_proof.cert.cert.user_data;
                    cert_cloned.cert.holes = latest_proof.cert.cert.holes;
                    cert_cloned.signature = latest_proof.cert.signature;
                    assert XdrUtils.equals(cert_cloned, latest_proof.cert) : "(modified) cloned_cert=" + XdrUtils.toString(cert_cloned) + " != latest_proof.cert=" + XdrUtils.toString(latest_proof.cert) + ". cloned_cert is a mixture of new_cert=" + XdrUtils.toString(cert) + " and latest_proof.cert";
                    assert (cert.cert.seq_num > latest_proof.cert.cert.seq_num) && (cert.cert.timestamp > latest_proof.cert.cert.timestamp) && (cert.cert.expire_time >= latest_proof.cert.cert.expire_time) : " new cert " + XdrUtils.toString(cert) + " is not later than old cert" + XdrUtils.toString(latest_proof.cert);
                }
                break;
            case cr_repair_1:
                {
                    cr_repair_args args = (cr_repair_args) req.args;
                    cr_signed_ss_set_config latest_config = args.latest_config;
                    assert latest_config.config.expire_time > (start_time_us / 1000L) : "latest_config.config.expire_time=" + latest_config.config.expire_time + "ms < now=" + (start_time_us / 1000L) + "ms";
                    extent_key = latest_config.config.extent_key;
                    Map<BigInteger, cr_soundness_proof> sig_proofs = null;
                    SortedMap<KeyTriple<Long, Long, BigInteger>, cr_soundness_proof> latest_proofs = null;
                    assert !AntiquityUtils.equals(latest_config.config.public_key, CR_NULL_PUBLIC_KEY) && !AntiquityUtils.equals(latest_config.config.extent_key, CR_NULL_GUID) && !AntiquityUtils.equals(latest_config.config.client_id, CR_NULL_GUID) && verifySignature(latest_config.config, latest_config.signature, latest_config.config.public_key, keyFactory, verifyEngine) : method_tag + ": Verification failed for latest_config" + XdrUtils.toString(latest_config);
                    for (cr_soundness_proof proof : args.proof) {
                        assert XdrUtils.equals(extent_key, proof.config.config.extent_key) : "Extent key=" + XdrUtils.toString(extent_key) + " does not match proof.config.extent_key=" + XdrUtils.toString(proof.config.config.extent_key);
                        assert checkSoundnessProof(proof, true, false, md, keyFactory, verifyEngine) : "Invalid proof " + XdrUtils.toString(proof);
                        md.update(XdrUtils.serialize(proof.cert.cert));
                        md.update(XdrUtils.serialize(proof.config.config));
                        BigInteger hash = byteArrayToBigInteger(md.digest());
                        if (sig_proofs == null) {
                            sig_proofs = new LinkedHashMap<BigInteger, cr_soundness_proof>();
                            latest_proofs = new TreeMap<KeyTriple<Long, Long, BigInteger>, cr_soundness_proof>();
                        }
                        if (!sig_proofs.containsKey(hash)) {
                            sig_proofs.put(hash, proof);
                            KeyTriple<Long, Long, BigInteger> key = new KeyTriple<Long, Long, BigInteger>(proof.config.config.seq_num, proof.cert.cert.seq_num, hash);
                            assert !latest_proofs.containsKey(key) : method_tag + ": proofs map already contains key " + key + ".  value=" + XdrUtils.toString(latest_proofs.get(key)) + ", but want to put " + XdrUtils.toString(proof);
                            latest_proofs.put(key, proof);
                        }
                    }
                    int num_repair_sigs = 0;
                    for (int i = 0; i < args.sig.length; i++) {
                        BigInteger hash = guidToBigInteger(args.sig_proof_hash[i]);
                        cr_soundness_proof proof = sig_proofs.get(hash);
                        assert proof != null : "proof is null for" + " repair_args.sig[" + i + "]=" + XdrUtils.toString(args.sig[i]) + " repair_args.sig_proof_hash[" + i + "]=0x" + GuidTools.guid_to_string(hash);
                        assert verifyRepairSignature(logger, args.sig[i], proof, latest_config, md, keyFactory, verifyEngine) : "Invalid repair_args.sig[" + i + "]=" + XdrUtils.toString(args.sig[i]) + " repair_args.sig_proof_hash[" + i + "]=0x" + GuidTools.guid_to_string(hash) + " proof=" + XdrUtils.toString(proof);
                        num_repair_sigs++;
                    }
                    assert (latest_config.config.threshold - 1) % 2 == 0 : "low_watermark=" + latest_config.config.threshold + " is not an odd number";
                    int f = (latest_config.config.threshold - 1) / 2;
                    assert num_repair_sigs >= (f + 1) : method_tag + ": not enough valid repair signatures. " + num_repair_sigs + " < (f=" + f + "+1)=" + (f + 1);
                    KeyTriple<Long, Long, BigInteger> latestProofKey = latest_proofs.lastKey();
                    latest_proof = latest_proofs.get(latestProofKey);
                }
                break;
            case cr_create_ss_set_1:
                {
                    cr_create_ss_set_args args = (cr_create_ss_set_args) req.args;
                    cr_signed_certificate cert = args.cert;
                    assert cert.cert.expire_time > (start_time_us / 1000L) : "cert.expire_time=" + cert.cert.expire_time + "ms < now=" + (start_time_us / 1000L) + "ms";
                    assert verifySignature(args.cert.cert, args.cert.signature, args.cert.cert.public_key, keyFactory, verifyEngine) : method_tag + ": Verification failed for " + XdrUtils.toString(args.cert);
                    extent_key = (cert.cert.type == CR_KEY_VERIFIED ? computeGuid(cert.cert.public_key, cr_guid.class, md) : cert.cert.verifier);
                }
                break;
            default:
                assert false : "checkReq: received an unregistered remote procedured call.";
                break;
        }
        return new ValuePair<cr_guid, cr_soundness_proof>(extent_key, latest_proof);
    }

    /** 
   * <code>checkProposedConfig</code> sanity checks the 
   * {@link antiquity.cr.api.cr_ss_set_config proposed_config}. 
   * @param proposed_config The
   *            {@link antiquity.cr.api.cr_ss_set_config proposed_config}. 
   * @return <code>true</code> if the 
   * {@link antiquity.cr.api.cr_ss_set_config proposed_config} checks out;
   * <code>otherwise</code>, false. */
    public static boolean checkProposedConfig(Logger logger, cr_ss_set_config proposed_config) {
        if (proposed_config == null) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " proposed_config is null");
            return false;
        }
        if ((proposed_config.threshold - 1) % 2 != 0) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " low_watermark=" + proposed_config.threshold + " is not an odd number");
            return false;
        }
        int f = (proposed_config.threshold - 1) / 2;
        if (proposed_config.ss_set == null) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " ss_set is null");
            return false;
        }
        if (proposed_config.ss_set.length != proposed_config.ss_set_size) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " ss_set.len=" + proposed_config.ss_set.length + " != ss_set_size=" + proposed_config.ss_set_size);
            return false;
        }
        if (proposed_config.threshold > proposed_config.ss_set_size) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " threshold=" + proposed_config.threshold + " > ss_set_size=" + proposed_config.ss_set_size);
            return false;
        }
        if ((proposed_config.type == CR_KEY_VERIFIED && proposed_config.ss_set_size != (3 * f + 1)) || (proposed_config.type == CR_HASH_VERIFIED && proposed_config.redundancy_type == CR_REDUNDANCY_TYPE_REPL && proposed_config.ss_set_size < (3 * f + 1)) || (proposed_config.type == CR_HASH_VERIFIED && proposed_config.redundancy_type != CR_REDUNDANCY_TYPE_REPL && proposed_config.ss_set_size != (3 * f + 1))) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " inconsistent type=" + proposed_config.type + ", ss_set_size=" + proposed_config.ss_set_size + ", redundancy_type=" + proposed_config.redundancy_type + ", and (3*f=" + f + " + 1)=" + (3 * f + 1));
            return false;
        }
        if ((int) (Math.ceil((double) proposed_config.ss_set_size / proposed_config.inv_rate)) > f && (f != 0 && proposed_config.ss_set_size != 1 && proposed_config.inv_rate != 1)) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " (threshold=" + proposed_config.threshold + "), but" + " f=" + f + " <= (ss_set_size=" + proposed_config.ss_set_size + "/inv_rate=" + proposed_config.inv_rate + ")=" + (int) (Math.ceil((double) proposed_config.ss_set_size / proposed_config.inv_rate)));
            return false;
        }
        if (proposed_config.client_id == null || XdrUtils.equals(proposed_config.client_id, CR_NULL_GUID)) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " client_id is null " + XdrUtils.toString(proposed_config.client_id));
            return false;
        }
        if (proposed_config.extent_key == null || XdrUtils.equals(proposed_config.extent_key, CR_NULL_GUID) || (proposed_config.type == CR_KEY_VERIFIED && !XdrUtils.equals(proposed_config.extent_key, proposed_config.client_id)) || (proposed_config.type == CR_HASH_VERIFIED && XdrUtils.equals(proposed_config.extent_key, proposed_config.client_id))) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " inconsistent between" + " extent_key=" + XdrUtils.toString(proposed_config.extent_key) + " and type=" + proposed_config.type);
            return false;
        }
        if (proposed_config.timestamp >= proposed_config.expire_time) {
            if (logger.isDebugEnabled()) logger.debug("StorageSetCreator.checkProposedConfig: " + " timestamp=" + proposed_config.timestamp + " >= expire_time=" + proposed_config.expire_time);
            return false;
        }
        return true;
    }

    /**
   * <code>getProposedConfig</code> returns the 
   * {@link antiquity.cr.api.cr_signed_ss_set_config} from a 
   * {@link antiquity.rpc.api.ProcKey procedure} and 
   * {@link antiquity.cr.api.cr_api args} pair.
   * @param args {@link antiquity.cr.api.cr_api args}.
   * @param proc {@link antiquity.rpc.api.ProcKey procedure}. */
    public static cr_signed_ss_set_config getProposedConfig(XdrAble args, ProcKey proc) {
        cr_ss_set_config config = null;
        switch(proc.getProcNum()) {
            case cr_get_latest_config_1:
                assert false : "not supported";
                break;
            case cr_renew_1:
                config = ((cr_renew_args) args).proposed_config;
                break;
            case cr_repair_1:
                config = ((cr_repair_args) args).proposed_config;
                break;
            case cr_create_ss_set_1:
                config = ((cr_create_ss_set_args) args).proposed_config;
                break;
            default:
                assert false : "getProposedConfig: received an unregistered remote procedured call.";
                break;
        }
        cr_signed_ss_set_config proposed_config = new cr_signed_ss_set_config();
        proposed_config.config = config;
        proposed_config.signature = new byte[0];
        return proposed_config;
    }

    /**
   * <code>setProposedConfig</code> sets the proposed
   * {@link antiquity.cr.api.cr_signed_ss_set_config} from a 
   * {@link antiquity.rpc.api.ProcKey procedure} and 
   * {@link antiquity.cr.api.cr_api args} pair.
   * @param proposed_config proposed 
   *         {@link antiquity.cr.api.cr_signed_ss_set_config}/
   * @param args {@link antiquity.cr.api.cr_api args}.
   * @param proc {@link antiquity.rpc.api.ProcKey procedure}. */
    public static void setProposedConfig(cr_signed_ss_set_config config, XdrAble args, ProcKey proc) {
        switch(proc.getProcNum()) {
            case cr_get_latest_config_1:
                assert false : "not supported";
                break;
            case cr_renew_1:
                ((cr_renew_args) args).proposed_config = config.config;
                break;
            case cr_repair_1:
                ((cr_repair_args) args).proposed_config = config.config;
                break;
            case cr_create_ss_set_1:
                ((cr_create_ss_set_args) args).proposed_config = config.config;
                break;
            default:
                assert false : "setProposedConfig: received an unregistered remote procedured call.";
                break;
        }
    }

    /**
   * <code>getResultConfig</code> returns the 
   * {@link antiquity.cr.api.cr_signed_ss_set_config} from a 
   * {@link antiquity.rpc.api.ProcKey procedure} and 
   * {@link antiquity.cr.api.cr_api result} pair.
   * @param result {@link antiquity.cr.api.cr_api result}.
   * @param proc {@link antiquity.rpc.api.ProcKey procedure}. */
    public static cr_signed_ss_set_config getResultConfig(XdrAble result, ProcKey proc) {
        cr_signed_ss_set_config config = null;
        switch(proc.getProcNum()) {
            case cr_get_latest_config_1:
                config = ((cr_get_latest_config_result) result).config;
                break;
            case cr_renew_1:
                config = ((cr_renew_result) result).config;
                break;
            case cr_repair_1:
                config = ((cr_repair_result) result).config;
                break;
            case cr_create_ss_set_1:
                config = ((cr_create_ss_set_result) result).config;
                break;
            default:
                assert false : "getResultConfig: received an unregistered remote procedured call.";
                break;
        }
        return config;
    }

    /**
   * <code>getResultStatus</code> returns the 
   * {@link antiquity.cr.api.cr_status} from a 
   * {@link antiquity.rpc.api.ProcKey procedure} and 
   * {@link antiquity.cr.api.cr_api result} pair.
   * @param result {@link antiquity.cr.api.cr_api result}.
   * @param proc {@link antiquity.rpc.api.ProcKey procedure}. */
    public static int getResultStatus(XdrAble result, ProcKey proc) {
        int status = -1;
        switch(proc.getProcNum()) {
            case cr_get_latest_config_1:
                status = ((cr_get_latest_config_result) result).status;
                break;
            case cr_renew_1:
                status = ((cr_renew_result) result).status;
                break;
            case cr_repair_1:
                status = ((cr_repair_result) result).status;
                break;
            case cr_create_ss_set_1:
                status = ((cr_create_ss_set_result) result).status;
                break;
            default:
                assert false : "getResultStatus: received an unregistered remote procedured call." + proc;
                break;
        }
        return status;
    }

    /**
   * <code>getNormalResult</code> returns the 
   * {@link antiquity.cr.api.cr_api normal_result} for a corresponding
   * {@link antiquity.rpc.api.ProcKey procedure} and 
   * {@link antiquity.cr.api.cr_api arguement} pair.
   *
   * @param args {@link antiquity.cr.api.ss_api arguement}.
   * @param proc {@link antiquity.rpc.api.ProcKey procedure}. 
   * @param config {@link antiquity.cr.api.cr_signed_ss_set_config}. */
    public static XdrAble getNormalResult(XdrAble args, ProcKey proc, cr_signed_ss_set_config config, cr_signed_certificate cert, cr_soundness_proof prev_proof) {
        XdrAble cr_result = null;
        switch(proc.getProcNum()) {
            case cr_get_latest_config_1:
                {
                    cr_get_latest_config_args renew_args = (cr_get_latest_config_args) args;
                    cr_get_latest_config_result result = new cr_get_latest_config_result();
                    result.status = cr_status.STATUS_OK;
                    result.config = config;
                    result.cert = cert;
                    result.prev_proof = (prev_proof != null ? prev_proof : CR_NULL_SOUNDNESS_PROOF);
                    cr_result = result;
                }
                break;
            case cr_renew_1:
                {
                    cr_renew_args renew_args = (cr_renew_args) args;
                    cr_renew_result result = new cr_renew_result();
                    result.status = cr_status.STATUS_OK;
                    result.config = config;
                    cr_result = result;
                }
                break;
            case cr_repair_1:
                {
                    cr_repair_args repair_args = (cr_repair_args) args;
                    cr_repair_result result = new cr_repair_result();
                    result.status = cr_status.STATUS_OK;
                    result.config = config;
                    cr_result = result;
                }
                break;
            case cr_create_ss_set_1:
                {
                    cr_create_ss_set_args create_args = (cr_create_ss_set_args) args;
                    cr_create_ss_set_result result = new cr_create_ss_set_result();
                    result.status = cr_status.STATUS_OK;
                    result.config = config;
                    cr_result = result;
                }
                break;
            default:
                assert false : "getNormalResult: received an unregistered remote procedured call." + proc;
                break;
        }
        return cr_result;
    }

    /**
   * <code>getErrorResult</code> returns the 
   * {@link antiquity.cr.api.cr_api error_result} for a corresponding
   * {@link antiquity.rpc.api.ProcKey procedure} and 
   * {@link antiquity.cr.api.cr_api arguement} pair.
   *
   * @param error {@link antiquity.cr.api.cr_status error_status}. 
   * @param args {@link antiquity.cr.api.cr_api arguement}.
   * @param proc {@link antiquity.rpc.api.ProcKey procedure}. */
    public static XdrAble getErrorResult(int error, XdrAble args, ProcKey proc, cr_signed_ss_set_config config, cr_signed_certificate cert, cr_soundness_proof prev_proof) {
        XdrAble cr_result = null;
        switch(proc.getProcNum()) {
            case cr_get_latest_config_1:
                {
                    cr_get_latest_config_args get_args = (cr_get_latest_config_args) args;
                    cr_get_latest_config_result result = new cr_get_latest_config_result();
                    result.status = error;
                    result.config = (config != null ? config : CR_NULL_SIGNED_CONFIG);
                    result.cert = (cert != null ? cert : CR_NULL_SIGNED_CERT);
                    result.prev_proof = (prev_proof != null ? prev_proof : CR_NULL_SOUNDNESS_PROOF);
                    cr_result = result;
                }
                break;
            case cr_renew_1:
                {
                    cr_renew_args renew_args = (cr_renew_args) args;
                    cr_renew_result result = new cr_renew_result();
                    result.status = error;
                    result.config = (config != null ? config : CR_NULL_SIGNED_CONFIG);
                    cr_result = result;
                }
                break;
            case cr_repair_1:
                {
                    cr_repair_args repair_args = (cr_repair_args) args;
                    cr_repair_result result = new cr_repair_result();
                    result.status = error;
                    result.config = (config != null ? config : CR_NULL_SIGNED_CONFIG);
                    cr_result = result;
                }
                break;
            case cr_create_ss_set_1:
                {
                    cr_create_ss_set_args renew_args = (cr_create_ss_set_args) args;
                    cr_create_ss_set_result result = new cr_create_ss_set_result();
                    result.status = error;
                    result.config = (config != null ? config : CR_NULL_SIGNED_CONFIG);
                    cr_result = result;
                }
                break;
            default:
                assert false : "getErrorResult: received an unregistered remote procedured call." + proc;
                break;
        }
        return cr_result;
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void init(ConfigDataIF config) throws Exception {
        final String method_tag = tag + ".init";
        super.init(config);
        assert config.contains("useDhtRootSet") : "Need to define useDhtRootSet in cfg";
        USE_DHT_ROOT_SET = config_get_boolean(config, "useDhtRootSet");
        assert config.contains("remember") : "Need to define remember in cfg";
        REMEMBER = config_get_boolean(config, "remember");
        assert config.contains("maxAttempts") : "Need to define maxAttempts in cfg";
        MAX_ATTEMPTS = config_get_int(config, "maxAttempts");
        assert config.contains("timeoutMs") : "Need to define timeoutMs in cfg";
        TIMEOUT_MS = config_get_long(config, "timeoutMs");
        assert config.contains("pingTimeoutMs") : "Need to define pingTimeoutMs in cfg";
        PING_TIMEOUT = config_get_long(config, "pingTimeoutMs");
        assert config.contains("sendForAdminSig") : "Need to define sendForAdminSig in cfg";
        SEND_FOR_ADMIN_SIG = config_get_boolean(config, "sendForAdminSig");
        if (SEND_FOR_ADMIN_SIG) {
            assert config.contains("admin") : "Need to define admin in cfg";
            ADMIN = new NodeId(config_get_string(config, "admin"));
            assert config.contains("adminTimeoutMs") : "Need to define adminTimeoutMs in cfg";
            ADMIN_TIMEOUT = config_get_long(config, "adminTimeoutMs");
        }
        if (config.contains("UseFakeSignatures")) AntiquityUtils.USE_FAKE_SIGNATURES = config_get_boolean(config, "UseFakeSignatures");
        assert config.contains("PkeyFilename") : "Need to define PkeyFilename in cfg file";
        assert config.contains("SkeyFilename") : "Need to define SkeyFilename in cfg file";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": Reading keys from disk...");
        String pkey_filename = config_get_string(config, "PkeyFilename");
        String skey_filename = config_get_string(config, "SkeyFilename");
        _keyPair_pkey = readPublicKey(pkey_filename, _keyFactory);
        _keyPair_skey = readPrivateKey(skey_filename, _keyFactory);
        if ((_keyPair_pkey == null) || (_keyPair_skey == null)) throw new Exception(method_tag + ": Failed to read key pair from disk: " + "pkey=" + pkey_filename + ", skey=" + skey_filename);
        _pkey = new cr_public_key(_keyPair_pkey.getEncoded());
        _sigEngine.initSign(_keyPair_skey);
        if (logger.isInfoEnabled()) logger.info(method_tag + ": Reading keys from disk...done.");
        _rand = new Random(new SHA1Hash("" + my_node_id + now_us()).lower64bits());
        Class[] rpc_msg_types = new Class[] { dd.kbr.api.KbrAppRegResp.class };
        for (Class clazz : rpc_msg_types) {
            Filter filter = new Filter();
            if (!filter.requireType(clazz)) BUG(tag + ": could not require type " + clazz.getName());
            if (antiquity.rpc.api.RpcCall.class.isAssignableFrom(clazz)) {
                if (!filter.requireValue("inbound", new Boolean(true))) BUG(tag + ": could not require inbound = true for " + clazz.getName());
            }
            if (!filter.requireValue("appId", appId)) BUG(tag + ": could not require appId = " + appId + " for " + clazz.getName());
            if (logger.isInfoEnabled()) logger.info(tag + ": subscribing to " + clazz);
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
        } else if (item instanceof KbrAppRegResp) {
            handleKbrAppRegResp((KbrAppRegResp) item);
            return;
        } else if (!_stagesInitialized || !_ddReady || !_registeredRpcServer || !_registeredRpcClient || !_registeredKbr) {
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + ": Queueing event " + item + " until stage initialized and registered.");
            _blockedEvents.add(item);
            return;
        } else if (item instanceof KbrRootSetChanged) {
            handleKbrRootSetChanged((KbrRootSetChanged) item);
        } else if (item instanceof RequestDisseminationSetResponseMsg) {
            handleRequestDisseminationSetResponseMsg((RequestDisseminationSetResponseMsg) item);
        } else if (item instanceof StorageSetPing) {
            handlePing((StorageSetPing) item);
        } else if (item instanceof StorageSetPong) {
            handlePong((StorageSetPong) item);
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
        RpcRegisterReq rpcServerReq = new RpcRegisterReq(getCRProcedureMap(), false, appId, Boolean.FALSE, (SinkIF) null);
        Thunk1<RpcCall> handler = new Thunk1<RpcCall>() {

            public void run(RpcCall rpcCall) {
                handleRpcCall(rpcCall);
            }
        };
        rpcServerReq.handlers = new LinkedHashMap<ProcKey, Thunk1<RpcCall>>();
        for (ProcKey key : rpcServerReq.procedures.keySet()) rpcServerReq.handlers.put(key, handler);
        rpcServerReq.cb = new Thunk2<RpcRegisterReq, RpcRegisterResp>() {

            public void run(RpcRegisterReq rpcCall, RpcRegisterResp rpcResp) {
                handleRpcRegisterResp(rpcResp);
            }
        };
        dispatch(rpcServerReq);
        RpcRegisterReq rpcClientReq = new RpcRegisterReq(getCRProcedureMap(), true, appId, Boolean.TRUE, my_sink);
        rpcClientReq.cb = new Thunk2<RpcRegisterReq, RpcRegisterResp>() {

            public void run(RpcRegisterReq rpcCall, RpcRegisterResp rpcResp) {
                handleRpcRegisterResp(rpcResp);
            }
        };
        dispatch(rpcClientReq);
        KbrAppRegReq kbrReq = new KbrAppRegReq(appId, true, false, false, my_sink, null);
        dispatch(kbrReq);
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
            if (!RpcRegisterResp.SUCCESS.equals((Integer) entry.getValue())) BUG(method_tag + ": proc " + entry + " was not registered.");
        }
        assert resp.userData instanceof Boolean : "unknown userData " + resp.userData;
        if ((Boolean) resp.userData.equals(Boolean.TRUE)) _registeredRpcClient = true; else _registeredRpcServer = true;
        if (_stagesInitialized && _ddReady && _registeredRpcServer && _registeredRpcClient && _registeredKbr) initializeStorageSetCreator();
    }

    /**
   * <CODE>handleKbrAppRegResp</CODE> registered to receive 
   * {@link dd.kbr.api.KbrRootSetChanged} events.
   *
   * @param signal {@link dd.kbr.api.KbrAppRegResp} */
    private void handleKbrAppRegResp(KbrAppRegResp resp) {
        final String method_tag = tag + ".handleKbrAppRegResp";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": called " + resp);
        _registeredKbr = true;
        if (_stagesInitialized && _ddReady && _registeredRpcServer && _registeredRpcClient && _registeredKbr) initializeStorageSetCreator();
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
        _self_guid_cr = bigIntegerToGuid(_self_guid_sha1, cr_guid.class);
        if (logger.isInfoEnabled()) logger.debug(method_tag + ": " + "called " + msg);
        _ddReady = true;
        if (_stagesInitialized && _ddReady && _registeredRpcServer && _registeredRpcClient && _registeredKbr) initializeStorageSetCreator();
    }

    /**
   * <CODE>initializeStorageSetCreator</CODE> unblocks all blocked events.
   * That is, <I>all systems go</I>!
   * <I>INVARIANT</I> All stages initialized and rpc handlers registered. */
    private void initializeStorageSetCreator() {
        final String method_tag = tag + ".initializeStorageSetCreator";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": called\n");
        _self_cr_id = new cr_id();
        NodeId nodeId = new NodeId(my_node_id.port() + 1, my_node_id.address());
        _self_cr_id.addr = nodeIdToAddress(nodeId, cr_address.class);
        _self_cr_id.guid = _self_guid_cr;
        if (logger.isInfoEnabled()) logger.info(method_tag + ": " + "Accepting requests...");
        while (_blockedEvents.size() > 0) {
            try {
                handleEvent((QueueElementIF) _blockedEvents.remove(0));
            } catch (EventHandlerException ehe) {
                BUG(ehe);
            }
        }
    }

    /** 
   * <code>handleKbrRootSetChanged</code> stores the new root set locally.
   *
   * @param msg {@link dd.kbr.api.KbrRootSetChanged}. */
    private void handleKbrRootSetChanged(KbrRootSetChanged msg) {
        final String method_tag = tag + ".handleKbrRootSetChanged";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": called " + msg);
        _rootsetMap = new LinkedHashMap<BigInteger, NodeId>();
        int i = 0;
        for (int j = msg.preds.length - 1; j >= 0; j--, i++) {
            _rootsetMap.put(GuidTools.secure_hash_to_big_integer(msg.preds[j].guid), new NodeId(msg.preds[j].node_id.port() + 1, msg.preds[j].node_id.address()));
        }
        _rootsetMap.put(_self_guid_sha1, new NodeId(my_node_id.port() + 1, my_node_id.address()));
        for (int j = 0; j < msg.succs.length; ++j, i++) {
            _rootsetMap.put(GuidTools.secure_hash_to_big_integer(msg.succs[j].guid), new NodeId(msg.succs[j].node_id.port() + 1, msg.succs[j].node_id.address()));
        }
        _rootset = new cr_id[_rootsetMap.size()];
        int j = 0;
        for (Map.Entry<BigInteger, NodeId> entry : _rootsetMap.entrySet()) {
            BigInteger guid = entry.getKey();
            NodeId nodeId = entry.getValue();
            _rootset[j] = new cr_id();
            _rootset[j].guid = bigIntegerToGuid(guid, cr_guid.class);
            _rootset[j].addr = nodeIdToAddress(nodeId, cr_address.class);
            j++;
        }
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + printRootSet());
    }

    private cr_id kbrNeighborInfoToCrId(KbrNeighborInfo info) {
        cr_id id = new cr_id();
        NodeId nodeId = new NodeId(info.node_id.port() + 1, info.node_id.address());
        id.addr = nodeIdToAddress(nodeId, cr_address.class);
        id.guid = secureHashToGuid(info.guid, cr_guid.class);
        return id;
    }

    private String printRootSet() {
        String str = new String();
        str += "{ preds=(";
        int i = 0;
        for (; _rootset != null && i < _rootset.length; i++) {
            if (AntiquityUtils.equals(_self_guid_cr, _rootset[i].guid)) break;
            str += XdrUtils.toString(_rootset[i]) + " ";
        }
        str += ")";
        str += " self=" + XdrUtils.toString(_self_cr_id);
        str += " succs=(";
        for (i = i + 1; _rootset != null && i < _rootset.length; i++) str += XdrUtils.toString(_rootset[i]) + ((i == _rootset.length - 1) ? "" : " ");
        return str + ") }";
    }

    /**********************************************************************/
    private void handleRpcCall(RpcCall req) {
        final String method_tag = tag + ".handleRpcCall";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called " + req);
        if (req.proc.getProcNum() == cr_null_1) {
            handle_cr_null(req);
            return;
        }
        RpcKey key = new RpcKey(req.xact_id, req.peer);
        if (_pendingRpcCalls.containsKey(key)) {
            if (logger.isInfoEnabled()) logger.info(method_tag + " already processing request " + req);
            return;
        }
        _pendingRpcCalls.put(key, req);
        ReqState state = null;
        try {
            ValuePair<cr_guid, cr_soundness_proof> pair = checkReq(logger, req, now_us(), _md, _keyFactory, _verifyEngine);
            cr_guid extent_key = pair.getFirst();
            cr_soundness_proof latest_proof = pair.getSecond();
            state = new ReqState(req, latest_proof);
        } catch (Throwable e) {
            logger.warn(method_tag + ": req check failed. " + e + ".  Returning error to client.");
            _pendingRpcCalls.remove(key);
            XdrAble errorReply = getErrorResult(cr_status.STATUS_ERR_INVALID_REQ, req.args, req.proc, null, (cr_signed_certificate) null, (cr_soundness_proof) null);
            RpcReply resp = new RpcReply(errorReply, req);
            dispatch(resp);
            return;
        }
        if (_waitingReqs.containsKey(state.extent_key_sha1)) {
            if (logger.isInfoEnabled()) logger.info(method_tag + ": Queueing req " + req);
            LinkedList<ReqState> waitingReqs = _waitingReqs.get(state.extent_key_sha1);
            if (waitingReqs == null) {
                waitingReqs = new LinkedList<ReqState>();
                _waitingReqs.put(state.extent_key_sha1, waitingReqs);
            }
            waitingReqs.addLast(state);
            return;
        } else {
            _waitingReqs.put(state.extent_key_sha1, (LinkedList<ReqState>) null);
        }
        processRpcCall(state);
    }

    private void processRpcCall(ReqState state) {
        final String method_tag = tag + ".processRpcCall";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": " + "called " + state);
        if (state.req.proc.getProcNum() == cr_get_latest_config_1) {
            if (SEND_FOR_ADMIN_SIG) send_for_admin_signature(state, ADMIN, null); else _send_resp(state, cr_status.STATUS_ERR_NO_REMOTE_ADMIN, null);
        } else {
            if (!USE_DHT_ROOT_SET) send_local_create_ss_set_req(state, 0L); else send_remote_create_ss_set_req(state);
        }
    }

    private void handle_cr_null(RpcCall req) {
        final String method_tag = tag + ".handle_cr_null";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called " + req);
        XdrVoid args = (XdrVoid) req.args;
        RpcReply resp = new RpcReply(args, req);
        dispatch(resp);
        return;
    }

    /**********************************************************************/
    private void send_resp(ReqState state, cr_id[] ss_set) {
        final String method_tag = tag + ".send_resp";
        RpcKey key = new RpcKey(state.req.xact_id, state.req.peer);
        assert _pendingRpcCalls.get(key) == state.req : "removed request did not match " + state;
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called ss_set=" + XdrUtils.toString(ss_set) + ". " + state);
        assert ss_set != null && (state.proposed_config.type == CR_KEY_VERIFIED && ss_set.length == state.proposed_config.ss_set_size) || (state.proposed_config.type == CR_HASH_VERIFIED && ss_set.length >= state.proposed_config.ss_set_size) : "requested ss_set.len" + state.proposed_config.ss_set_size + " != actual ss_set.len=" + (ss_set == null ? -1 : ss_set.length) + ". type=" + state.proposed_config.type;
        byte inv_rate = state.proposed_config.inv_rate;
        if (REMEMBER && state.req != null && state.req.proc != null && (state.req.proc.getProcNum() == cr_renew_1 || state.req.proc.getProcNum() == cr_repair_1) && state.proposed_config.type == CR_HASH_VERIFIED && state.proposed_config.redundancy_type == CR_REDUNDANCY_TYPE_REPL) inv_rate = (byte) ss_set.length;
        long now_ms = now_ms();
        long expire_time_ms = state.cert.cert.expire_time;
        long ttl = (expire_time_ms - now_ms) / 1000L;
        if (ttl < MIN_TTL) {
            if (logger.isInfoEnabled()) logger.info(method_tag + ": returning error resp b/c computed ttl=" + ttl + " < MIN_TTL=" + MIN_TTL);
            _send_resp(state, cr_status.STATUS_ERR_INVALID_CERT, null);
            return;
        }
        long seq_num = -1L;
        if (state.req.proc.getProcNum() == cr_renew_1 || state.req.proc.getProcNum() == cr_repair_1) {
            seq_num = state.latest_config.config.seq_num + 1L;
        } else {
            assert state.req.proc.getProcNum() == cr_create_ss_set_1;
            seq_num = Math.abs(_rand.nextLong() % Long.MAX_VALUE / 2L);
        }
        cr_signed_ss_set_config config = AntiquityUtils.createStorageSetConfig(_pkey, state.extent_key, state.client_id, ss_set, state.proposed_config.type, state.proposed_config.redundancy_type, (byte) ss_set.length, inv_rate, state.proposed_config.threshold, seq_num, now_ms, expire_time_ms, cr_signed_ss_set_config.class, _md);
        if (!checkProposedConfig(logger, config.config)) {
            if (logger.isInfoEnabled()) logger.info(method_tag + ": returning error b/c proposed config is " + " invalid " + XdrUtils.toString(config.config));
            _send_resp(state, cr_status.STATUS_ERR_INVALID_CONFIG, null);
            return;
        }
        if (SEND_FOR_ADMIN_SIG) {
            config.signature = new byte[0];
            send_for_admin_signature(state, ADMIN, config);
        } else {
            try {
                config.signature = createSignature(config.config, _keyPair_skey, _sigEngine);
            } catch (Exception e) {
                BUG(method_tag + ": could not create signature. " + e);
            }
            _send_resp(state, cr_status.STATUS_OK, config);
        }
    }

    private void _send_resp(ReqState state, int status, cr_signed_ss_set_config config) {
        final String method_tag = tag + "._send_resp";
        _send_resp(state, status, config, (cr_signed_certificate) null, (cr_soundness_proof) null);
    }

    private void _send_resp(ReqState state, int status, cr_signed_ss_set_config config, cr_signed_certificate cert, cr_soundness_proof prev_proof) {
        final String method_tag = tag + "._send_resp";
        RpcKey key = new RpcKey(state.req.xact_id, state.req.peer);
        assert _pendingRpcCalls.remove(key) == state.req : "removed request did not match " + state;
        logger.warn(method_tag + ": called " + state);
        XdrAble result = null;
        if (status == cr_status.STATUS_OK) {
            assert config != null : "config is null";
            result = getNormalResult(state.req.args, state.req.proc, config, cert, prev_proof);
        } else {
            result = getErrorResult(status, state.req.args, state.req.proc, config, cert, prev_proof);
        }
        RpcReply reply = new RpcReply(result, state.req);
        state.req.server_stub_cb.run(reply);
        LinkedList<ReqState> waitingReqs = _waitingReqs.get(state.extent_key_sha1);
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": For extent 0x" + GuidTools.guid_to_string(state.extent_key_sha1) + ", there are " + ((waitingReqs == null || waitingReqs.isEmpty()) ? "no" : "" + waitingReqs.size()) + " procedure calls waiting to be processed.");
        if (waitingReqs == null || waitingReqs.isEmpty()) {
            _waitingReqs.remove(state.extent_key_sha1);
        } else {
            LinkedList<ValuePair<RpcCall, RpcReply>> rv = null;
            boolean read = state.req.proc.getProcNum() == cr_get_latest_config_1;
            for (Iterator<ReqState> i = waitingReqs.iterator(); i.hasNext(); ) {
                ReqState state2 = i.next();
                boolean read2 = state2.req.proc.getProcNum() == cr_get_latest_config_1;
                if (read == read2) {
                    i.remove();
                    if (status == cr_status.STATUS_OK) {
                        assert config != null : "config is null";
                        result = getNormalResult(state2.req.args, state2.req.proc, config, cert, prev_proof);
                    } else {
                        result = getErrorResult(status, state2.req.args, state2.req.proc, config, cert, prev_proof);
                    }
                    reply = new RpcReply(result, state2.req);
                    if (rv == null) rv = new LinkedList<ValuePair<RpcCall, RpcReply>>();
                    ValuePair<RpcCall, RpcReply> pair = new ValuePair<RpcCall, RpcReply>(state2.req, reply);
                    rv.add(pair);
                    assert state2.req != null && _pendingRpcCalls.remove(key = new RpcKey(state2.req.xact_id, state2.req.peer)) == state2.req;
                }
            }
            if (!waitingReqs.isEmpty()) {
                ReqState nextReq = waitingReqs.removeFirst();
                assert XdrUtils.equals(state.extent_key, nextReq.extent_key) : "extent_key=" + XdrUtils.toString(state.extent_key) + " != nextReq.extent_key=" + XdrUtils.toString(nextReq.extent_key);
                processRpcCall(nextReq);
            } else {
                if (logger.isDebugEnabled()) logger.debug(method_tag + ": no more requests for extent " + XdrUtils.toString(state.extent_key));
                _waitingReqs.remove(state.extent_key_sha1);
            }
            if (rv != null) {
                for (ValuePair<RpcCall, RpcReply> pair : rv) {
                    RpcCall req = pair.getFirst();
                    RpcReply resp = pair.getSecond();
                    req.server_stub_cb.run(resp);
                }
            }
        }
    }

    /**********************************************************************/
    private void send_for_admin_signature(ReqState state, NodeId admin, cr_signed_ss_set_config proposed_config) {
        final String method_tag = tag + ".send_for_admin_signature";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": for extent 0x" + GuidTools.guid_to_string(state.extent_key_sha1) + " sending request for admin to sign proposed config=" + XdrUtils.toString(proposed_config));
        XdrAble args = XdrUtils.clone(state.req.args);
        if (state.req.proc.getProcNum() != cr_get_latest_config_1) setProposedConfig(proposed_config, args, state.req.proc);
        RpcCall req = new RpcCall(admin, state.req.proc, args, appId, null, (SinkIF) null);
        Object token = _acore.register_timer(ADMIN_TIMEOUT, curry(handle_admin_signature_timeout_cb, state));
        state.token = token;
        req.caused_by_xact_id = state.req.xact_id;
        req.cb = curry(handle_admin_signature_cb, state, token);
        dispatch(req);
    }

    /**
   * <code>handle_admin_signature_cb</code> receives 
   * {@link antiquity.cr.api.cr_signed_ss_set_config} signed by a
   * designated administrator. */
    private Thunk4<ReqState, Object, RpcCall, RpcReply> handle_admin_signature_cb = new Thunk4<ReqState, Object, RpcCall, RpcReply>() {

        public void run(ReqState state, Object token, RpcCall req, RpcReply reply) {
            final String method_tag = tag + ".handle_admin_signature_cb";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": " + "called for extent 0x" + GuidTools.guid_to_string(state.extent_key_sha1) + " xact_id=" + Integer.toHexString(req.xact_id) + " caused_by_xact_id=" + Integer.toHexString(req.caused_by_xact_id) + " peer=" + req.peer + " reply=" + reply);
            if (!isCurrentReq(state, token, null, null, method_tag)) return;
            if (!((reply.reply_stat == RpcReply.ReplyStat.MSG_ACCEPTED) && (reply.msg_accepted == RpcReply.AcceptStat.SUCCESS) && (reply.auth_accepted == RpcReply.AuthStat.AUTH_OK))) {
                logger.warn(method_tag + ": " + " rpc stage returned error. " + " reply=" + reply + ".  call=" + state.req);
                _send_resp(state, cr_status.STATUS_ERR_REMOTE_ADMIN_SIG_FAILED, null);
                return;
            }
            cr_signed_certificate cert = null;
            cr_soundness_proof prev_proof = null;
            if (req.proc.getProcNum() == cr_get_latest_config_1) {
                cr_get_latest_config_result get_latest_result = (cr_get_latest_config_result) reply.reply;
                cert = get_latest_result.cert;
                prev_proof = get_latest_result.prev_proof;
            }
            _send_resp(state, getResultStatus(reply.reply, reply.proc), getResultConfig(reply.reply, reply.proc), cert, prev_proof);
        }
    };

    private Thunk1<ReqState> handle_admin_signature_timeout_cb = new Thunk1<ReqState>() {

        public void run(ReqState state) {
            final String method_tag = tag + ".handle_admin_signature_timeout_cb";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": called " + state);
            logger.warn(method_tag + ": admin signature failed. state=" + state);
            _send_resp(state, cr_status.STATUS_ERR_REMOTE_ADMIN_SIG_TIMEOUT, null);
        }
    };

    /**********************************************************************/
    private void send_local_create_ss_set_req(ReqState state, long delay) {
        final String method_tag = tag + ".send_local_cr_create_ss_set_req";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": " + "called " + state);
        HostAppFilter filter = new HostAppFilter("StorageServer");
        DisseminationSetPredicate pred = new DisseminationSetPredicate(state.proposed_config.ss_set_size, state.proposed_config.ss_set_size, 0.0, 1, DisseminationSetPredicate.CONSTRAINT_DISTINCT, DisseminationSetPredicate.CONSTRAINT_NONE, DisseminationSetPredicate.CONSTRAINT_NONE, DisseminationSetPredicate.CONSTRAINT_NONE, filter);
        SecureHash setCreatorGuid = new SHA1Hash("DisseminationSetCreator");
        RequestDisseminationSetMsg rdsm = new RequestDisseminationSetMsg(setCreatorGuid, GuidTools.big_integer_to_secure_hash(state.extent_key_sha1), pred, true, true, false, dd.api.DDLocateMsg.INTERMEDIATE_PTRS_DISK, dd.api.DDLocateMsg.VC_SRC, null);
        rdsm.inbound = true;
        rdsm.peer = _self_guid;
        state.token = _acore.register_timer(TIMEOUT_MS / MAX_ATTEMPTS, curry(host_db_timeout_cb, state.extent_key_sha1));
        assert _hostDbRequests.put(state.extent_key_sha1, state) == null : "outstanding host db req for " + state;
        if (delay > 0L) classifier.dispatch_later(rdsm, delay); else dispatch(rdsm);
    }

    /**
   * <CODE>handleRequestDisseminationSetResponseMsg</CODE> 
   * handles the reception of a
   * {@link dd.dissemination.RequestDisseminationSetResponseMsg}
   *
   * @param resp 
   *   {@link dd.dissemination.RequestDisseminationSetResponseMsg}.
   **/
    private void handleRequestDisseminationSetResponseMsg(RequestDisseminationSetResponseMsg resp) {
        final String method_tag = tag + ".handleRequestDisseminationSetResponseMsg";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called " + resp);
        if (!isCurrentReq(null, null, GuidTools.secure_hash_to_big_integer(resp.getIdentifier()), null, method_tag)) return;
        BigInteger extent_key_sha1 = GuidTools.secure_hash_to_big_integer(resp.getIdentifier());
        ReqState state = (ReqState) _hostDbRequests.remove(extent_key_sha1);
        if (state == null) {
            if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "NOT ours " + resp);
            return;
        }
        if (resp.getDisseminationSets() == null || resp.getDisseminationSets().size() <= 0) {
            if (logger.isInfoEnabled()) logger.info(method_tag + ": " + "No dissemination sets for resp=" + resp + " state=" + state);
            state.attempts++;
            if (state.attempts < MAX_ATTEMPTS) {
                send_local_create_ss_set_req(state, 5000L);
            } else {
                logger.warn(method_tag + ": dissemination_sets.len=" + (resp.getDisseminationSets() == null ? -1 : resp.getDisseminationSets().size()) + " < " + state.proposed_config.ss_set_size);
                _send_resp(state, cr_status.STATUS_ERR_NOT_ENOUGH_SS_SERVERS, null);
            }
            return;
        }
        assert extent_key_sha1.equals(state.extent_key_sha1) : " state.extent_key=" + GuidTools.guid_to_string(state.extent_key_sha1) + " != resp.id=" + GuidTools.guid_to_string(extent_key_sha1) + " state=" + state;
        Set storageServerSet = (Set) resp.getDisseminationSets().iterator().next();
        assert storageServerSet.size() == state.proposed_config.ss_set_size : "storageServerSet.size=" + storageServerSet.size() + " < " + state.proposed_config.ss_set_size;
        int j = 0;
        cr_id[] ss_set = new cr_id[storageServerSet.size()];
        Map<BigInteger, NodeId> ss_set_map = new HashMap<BigInteger, NodeId>();
        for (Iterator i = storageServerSet.iterator(); i.hasNext(); ) {
            HostInfo hinfo = (HostInfo) i.next();
            NodeId ssNodeId = new NodeId(hinfo.nodeId.port() + 1, hinfo.nodeId.address());
            ss_set_map.put(GuidTools.secure_hash_to_big_integer(hinfo.guid), ssNodeId);
            ss_set[j] = new cr_id();
            ss_set[j].addr = nodeIdToAddress(ssNodeId, cr_address.class);
            ss_set[j].guid = secureHashToGuid(hinfo.guid, cr_guid.class);
            j++;
        }
        prepare_ss_set(state, ss_set_map);
    }

    /**
   * <code>prepare_ss_set</code> does exactly that and calls 
   * <code>send_resp</code> when done. 
   *
   * @param state {@link antiquity.cr.impl.StorageSetCreator.ReqState}
   * @param ss_set {@link java.util.Map} of new storage nodes. */
    private void prepare_ss_set(ReqState state, Map<BigInteger, NodeId> ss_set) {
        final String method_tag = tag + ".prepare_ss_set";
        if (REMEMBER && state.req != null && state.req.proc != null && (state.req.proc.getProcNum() == cr_renew_1 || state.req.proc.getProcNum() == cr_repair_1) && state.proposed_config.type == CR_HASH_VERIFIED && state.proposed_config.redundancy_type == CR_REDUNDANCY_TYPE_REPL) {
            send_get_host_db(state, ss_set);
        } else {
            prepare_ss_set(state, ss_set, (Set<BigInteger>) null, (Set<BigInteger>) null, (Set<BigInteger>) null, (Set<BigInteger>) null, (Set<BigInteger>) null);
        }
    }

    /**
   * <code>prepare_ss_set</code> does exactly that and calls 
   * <code>send_resp</code> when done. 
   *
   * @param state {@link antiquity.cr.impl.StorageSetCreator.ReqState}
   * @param ss_set {@link java.util.Map} of new storage nodes. 
   * @param dead {@link java.util.Set} of 
   *               {@link dd.host.api.HostInfo.STATE_DEAD dead} nodes from old
   *               {@link antiquity.cr.api.cr_signed_ss_set_configuration}.
   * @param hibernating {@link java.util.Set} of 
   *               {@link dd.host.api.HostInfo.STATE_HIBERNATING hibernating}
   *               nodes from old
   *               {@link antiquity.cr.api.cr_signed_ss_set_configuration}. */
    private void prepare_ss_set(ReqState state, Map<BigInteger, NodeId> ss_set, Set<BigInteger> dead, Set<BigInteger> hibernating, Set<BigInteger> unknown, Set<BigInteger> reviving, Set<BigInteger> normal) {
        final String method_tag = tag + ".prepare_ss_set";
        assert ss_set != null && ss_set.size() >= state.proposed_config.ss_set_size : "using method incorrectly.  rootset.len=" + (ss_set == null ? -1 : ss_set.size()) + " < target=" + state.proposed_config.ss_set_size;
        Set<BigInteger> uniqueNodes = new HashSet<BigInteger>(ss_set.keySet());
        cr_id[] prev_config = null;
        if (state.latest_config != null && state.latest_config.config.ss_set.length > 0) {
            prev_config = state.latest_config.config.ss_set;
        } else if (state.proposed_config.ss_set.length > 0) {
            prev_config = state.proposed_config.ss_set;
        }
        int target_ss_size = state.proposed_config.ss_set_size;
        if (prev_config != null) {
            if (REMEMBER && state.req != null && state.req.proc != null && (state.req.proc.getProcNum() == cr_renew_1 || state.req.proc.getProcNum() == cr_repair_1) && state.proposed_config.type == CR_HASH_VERIFIED && state.proposed_config.redundancy_type == CR_REDUNDANCY_TYPE_REPL) {
                assert (state.proposed_config.threshold - 1) % 2 == 0 : "proposed.low_watermark=" + state.proposed_config.threshold + " is not an odd number";
                int f = (state.proposed_config.threshold - 1) / 2;
                int max_nodes = Math.min(2 * (3 * f) + 1, state.latest_config.config.ss_set_size + 1);
                Set<BigInteger> prev_set = new HashSet<BigInteger>();
                for (cr_id id : prev_config) {
                    BigInteger guid = guidToBigInteger(id.guid);
                    uniqueNodes.add(guid);
                    prev_set.add(guid);
                }
                Set<BigInteger> new_set = ss_set.keySet();
                for (int i = 0; i < 6 && uniqueNodes.size() > max_nodes; i++) {
                    Set<BigInteger> old_set = null;
                    switch(i) {
                        case 0:
                            old_set = dead;
                            break;
                        case 1:
                            old_set = hibernating;
                            break;
                        case 2:
                            old_set = unknown;
                            break;
                        case 3:
                            old_set = reviving;
                            break;
                        case 4:
                            old_set = normal;
                            break;
                        case 5:
                            old_set = prev_set;
                            break;
                        default:
                            BUG(method_tag + ": unknown set=" + i);
                            break;
                    }
                    if (old_set != null) shaveNodesOffTop(old_set, new_set, uniqueNodes, max_nodes);
                }
                assert uniqueNodes.size() <= max_nodes || uniqueNodes.size() == ss_set.size() : method_tag + ": uniqueNodes.len=" + uniqueNodes.size() + " >  max_nodes=" + max_nodes + ". uniqueNodes=" + uniqueNodes + " new_ss_set.len=" + ss_set.size() + " new_ss_set=" + ss_set.keySet();
                target_ss_size = Math.min(max_nodes, uniqueNodes.size());
            }
        }
        cr_id[] target_ss_set = new cr_id[target_ss_size];
        Set<BigInteger> unused_targets = new HashSet<BigInteger>(ss_set.keySet());
        for (int i = 0; prev_config != null && i < prev_config.length; i++) {
            BigInteger guid = guidToBigInteger(prev_config[i].guid);
            if (ss_set.containsKey(guid) || uniqueNodes.contains(guid)) {
                target_ss_set[i] = prev_config[i];
                unused_targets.remove(guid);
            }
        }
        {
            int i = 0, unassigned = 0;
            for (BigInteger guid : unused_targets) {
                NodeId nodeId = ss_set.get(guid);
                assert nodeId != null : method_tag + ": nodeId is null for guid=0x" + GuidTools.guid_to_string(guid);
                boolean assigned = false;
                for (; i < target_ss_set.length; i++) {
                    if (target_ss_set[i] == null) {
                        target_ss_set[i] = new cr_id();
                        target_ss_set[i].guid = bigIntegerToGuid(guid, cr_guid.class);
                        target_ss_set[i].addr = nodeIdToAddress(nodeId, cr_address.class);
                        i++;
                        assigned = true;
                        break;
                    }
                }
                if (!assigned) unassigned++;
                assert assigned || ss_set.size() > target_ss_size : method_tag + ": 0x" + GuidTools.guid_to_string(guid) + "/" + nodeId + " was not assigned to a position in the target_ss_set " + " eventhough ss_set.len=" + ss_set.size() + " and target_ss_size=" + target_ss_size + ". ss_set=" + ss_set.keySet() + ". target_ss_set=" + XdrUtils.toString(target_ss_set);
            }
            assert unassigned == 0 || unassigned == (ss_set.size() - target_ss_size) : method_tag + ": unassigned=" + unassigned + " != (ss_set.len=" + ss_set.size() + " + target_ss_size=" + target_ss_size + ")=" + (ss_set.size() - target_ss_size) + ". ss_set=" + ss_set.keySet() + ". target_ss_set=" + XdrUtils.toString(target_ss_set);
        }
        for (int i = 0; i < target_ss_set.length; i++) {
            assert target_ss_set[i] != null : method_tag + ": target_ss_set[" + i + "] is null";
        }
        send_resp(state, target_ss_set);
    }

    /**
   * <code>shaveNodesOffTop</code> if new_set does not contain old node 
   * (from old_set), then remove the old node from the union_set.  
   * Continue until union_set.size <= max_nodes_in_union or no more old nodes
   * (from old_set) to remove from union_set. */
    private void shaveNodesOffTop(Set<BigInteger> old_set, Set<BigInteger> new_set, Set<BigInteger> union_set, int max_nodes_in_union) {
        if (old_set != null && union_set.size() > max_nodes_in_union) {
            for (Iterator<BigInteger> i = old_set.iterator(); i.hasNext() && union_set.size() > max_nodes_in_union; ) {
                BigInteger guid = i.next();
                if (!new_set.contains(guid)) union_set.remove(guid);
            }
        }
    }

    /**
   * <code>send_get_host_db</code> receives the next
   * {@link dd.ptr.api.GetHostResp} values.
   *
   * @param state {@link antiquity.cr.impl.StorageSetCreator.ReqState}
   * @param ss_set {@link java.util.Map} of new storage nodes. */
    private void send_get_host_db(ReqState state, Map<BigInteger, NodeId> ss_set) {
        final String method_tag = tag + ".handle_get_host_db";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": called " + state + " ss_set=" + ss_set);
        Set<SecureHash> hostguids_list = new HashSet<SecureHash>();
        for (BigInteger guid : ss_set.keySet()) hostguids_list.add(GuidTools.big_integer_to_secure_hash(guid));
        if (state.latest_config.config.ss_set != null) {
            for (cr_id id : state.latest_config.config.ss_set) hostguids_list.add(guidToSecureHash(id.guid));
        }
        if (state.proposed_config.ss_set != null) {
            for (cr_id id : state.proposed_config.ss_set) hostguids_list.add(guidToSecureHash(id.guid));
        }
        SecureHash hostguids[] = new SecureHash[hostguids_list.size()];
        hostguids_list.toArray(hostguids);
        Object token = _acore.register_timer(TIMEOUT_MS, curry(get_host_db_timeout_cb, state));
        GetHostReq hostReq = new GetHostReq(hostguids, appId, null, my_sink);
        hostReq.cb = curry(handle_get_host_db_cb, state, ss_set, token);
        dispatch(hostReq);
    }

    /**
   * <code>handle_get_host_db_resp</code> receives the
   * {@link dd.ptr.api.GetHostResp} values.
   *
   * @param state {@link antiquity.cr.impl.StorageSetCreator.ReqState}
   * @param ss_set {@link java.util.Map} of new storage nodes. 
   * @param req {@link dd.ptr.api.GetHostReq}. 
   * @param resp {@link dd.ptr.api.GetHostResp}. */
    Thunk5<ReqState, Map<BigInteger, NodeId>, Object, GetHostReq, GetHostResp> handle_get_host_db_cb = new Thunk5<ReqState, Map<BigInteger, NodeId>, Object, GetHostReq, GetHostResp>() {

        public void run(ReqState state, Map<BigInteger, NodeId> ss_set, Object token, GetHostReq req, GetHostResp resp) {
            final String method_tag = tag + ".handle_get_host_db_cb";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": called " + state + " hostresp=" + resp);
            if (!isCurrentReq(state, token, null, null, method_tag)) return;
            Set<BigInteger> dead = null;
            Set<BigInteger> hibernating = null;
            Set<BigInteger> unknown = null;
            Set<BigInteger> reviving = null;
            Set<BigInteger> normal = null;
            for (int i = 0; i < resp.hostguids.length; i++) {
                if (resp.hinfos[i] == null) {
                    if (unknown == null) unknown = new HashSet<BigInteger>();
                    unknown.add(GuidTools.secure_hash_to_big_integer(resp.hostguids[i]));
                    break;
                } else {
                    switch(resp.hinfos[i].state) {
                        case HostInfo.STATE_DEAD:
                            if (dead == null) dead = new HashSet<BigInteger>();
                            dead.add(GuidTools.secure_hash_to_big_integer(resp.hostguids[i]));
                            break;
                        case HostInfo.STATE_HIBERNATING:
                            if (dead == null) dead = new HashSet<BigInteger>();
                            dead.add(GuidTools.secure_hash_to_big_integer(resp.hostguids[i]));
                            break;
                        case HostInfo.STATE_NOEXIST:
                            if (unknown == null) unknown = new HashSet<BigInteger>();
                            unknown.add(GuidTools.secure_hash_to_big_integer(resp.hostguids[i]));
                            break;
                        case HostInfo.STATE_REVIVING:
                            if (reviving == null) reviving = new HashSet<BigInteger>();
                            reviving.add(GuidTools.secure_hash_to_big_integer(resp.hostguids[i]));
                            break;
                        case HostInfo.STATE_NORMAL:
                            if (normal == null) normal = new HashSet<BigInteger>();
                            normal.add(GuidTools.secure_hash_to_big_integer(resp.hostguids[i]));
                            break;
                        default:
                            BUG(method_tag + ": unknown state=" + resp.hinfos[i]);
                            break;
                    }
                }
            }
            prepare_ss_set(state, ss_set, dead, hibernating, unknown, reviving, normal);
        }
    };

    private Thunk1<ReqState> get_host_db_timeout_cb = new Thunk1<ReqState>() {

        public void run(ReqState state) {
            final String method_tag = tag + ".get_host_db_timeout_cb";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": get host db failed. state=" + state);
            _send_resp(state, cr_status.STATUS_ERR_TIMEOUT, null);
        }
    };

    /**********************************************************************/
    private void send_remote_create_ss_set_req(ReqState state) {
        final String method_tag = tag + ".send_remote_cr_create_ss_set_req";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": called " + state);
        boolean root = is_owner(state.extent_key_sha1);
        if (root) {
            if (_rootset == null || _rootset.length < state.proposed_config.ss_set_size) {
                logger.warn(method_tag + ": rootset.len=" + (_rootset == null ? -1 : _rootset.length) + " < " + state.proposed_config.ss_set_size);
                _send_resp(state, cr_status.STATUS_ERR_NOT_ENOUGH_SS_SERVERS, null);
                return;
            }
            if (logger.isInfoEnabled()) logger.info(method_tag + ": " + "we are the root for " + GuidTools.guid_to_string(state.extent_key_sha1) + ".  Sending response.");
            prepare_ss_set(state, _rootsetMap);
        } else {
            send_root_ping_req(state);
        }
    }

    private cr_id[] get_correct_ss_set_size(cr_id[] rootset, cr_guid root, int target_ss_size) {
        final String method_tag = tag + ".get_correct_ss_set_size";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": " + "called rootset.len=" + (rootset == null ? -1 : rootset.length) + ", target_ss_size=" + target_ss_size);
        assert rootset != null && rootset.length >= target_ss_size : "using method incorrectly.  rootset.len=" + (rootset == null ? -1 : rootset.length) + " < target=" + target_ss_size;
        cr_id[] ss_set = new cr_id[target_ss_size];
        int numToRemove = rootset.length - ss_set.length;
        int removed = 0;
        int j = 0;
        for (int i = 0; i < rootset.length; i++) {
            int index = (i % 2 == 0 ? i / 2 : rootset.length - 1 - i / 2);
            if (AntiquityUtils.equals(root, rootset[index].guid) || removed == numToRemove) {
                int index2 = (j % 2 == 0 ? j / 2 : ss_set.length - 1 - j / 2);
                assert ss_set[index2] == null : "index2=" + index2 + " j=" + j + " index=" + index + " i=" + i + " ss_set.len=" + ss_set.length + " rootset.len=" + rootset.length;
                ss_set[index2] = rootset[index];
                j++;
            } else removed++;
        }
        assert removed == numToRemove && j == ss_set.length : "removed=" + removed + " !=  numToRemoved=" + numToRemove + ".  Or j=" + j + " != ss_set.len=" + ss_set.length + ".  rootset.len=" + _rootset.length;
        return ss_set;
    }

    private void send_root_ping_req(ReqState state) {
        final String method_tag = tag + ".send_root_ping_req";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": sending ping to find the root for extent 0x" + GuidTools.guid_to_string(state.extent_key_sha1));
        StorageSetPing ping = new StorageSetPing(state.extent_key_sha1, true, true, false);
        assert _pings.put(state.extent_key_sha1, state) == null : "outstanding ping req for " + state;
        Object token = _acore.register_timer(PING_TIMEOUT / MAX_ATTEMPTS, curry(handle_ping_timeout_cb, state));
        state.token = token;
        dispatch(ping);
    }

    /**
   * <code>handlePong</code> receives {@link antiquity.cr.impl.StorageSetPong}
   * from the {@link antiquity.cr.impl.StorageSetCreator} that is the 
   * <code>root</code> of the id space for the requested extent guid. */
    private void handlePong(StorageSetPong pong) {
        final String method_tag = tag + ".handlePong";
        if (logger.isInfoEnabled()) logger.info(method_tag + ": " + "called " + pong);
        if (!isCurrentReq(null, null, null, pong.guid, method_tag)) return;
        BigInteger extent_key_sha1 = pong.guid;
        ReqState state = (ReqState) _pings.remove(extent_key_sha1);
        assert state != null;
        if (pong.rootset == null || pong.rootset.size() < state.proposed_config.ss_set_size) {
            logger.warn(method_tag + ": rootset.len=" + (pong.rootset == null ? -1 : pong.rootset.size()) + " < " + state.proposed_config.ss_set_size);
            _send_resp(state, cr_status.STATUS_ERR_NOT_ENOUGH_SS_SERVERS, null);
            return;
        }
        prepare_ss_set(state, pong.rootset);
    }

    private Thunk1<ReqState> handle_ping_timeout_cb = new Thunk1<ReqState>() {

        public void run(ReqState state) {
            final String method_tag = tag + ".handle_ping_timeout_cb";
            if (logger.isInfoEnabled()) logger.info(method_tag + ": called " + state);
            assert _pings.remove(state.extent_key_sha1) == state : method_tag + ": no ping info for " + state;
            if (state.attempts < MAX_ATTEMPTS) {
                if (logger.isDebugEnabled()) logger.debug(method_tag + ": resending ping request for " + state.req);
                state.attempts++;
                send_root_ping_req(state);
            } else {
                logger.warn(method_tag + ": serializer ping failed. state=" + state);
                _send_resp(state, cr_status.STATUS_ERR_TIMEOUT, null);
            }
        }
    };

    /**
   * <code>handlePing</code> receives {@link antiquity.cr.impl.StorageSetPing}
   * from another 
   * {@link antiquity.cr.impl.StorageSetCreator} and responds
   * with a {@link antiquity.cr.impl.StorageSetPong}.
   *
   * @param ping  {@link antiquity.cr.impl.StorageSetPing}. */
    private void handlePing(StorageSetPing ping) {
        final String method_tag = tag + ".handlePing";
        if (logger.isDebugEnabled()) logger.debug(method_tag + ": called " + ping);
        boolean root = is_owner(ping.guid);
        if (!root) {
            if (logger.isInfoEnabled()) logger.info(method_tag + ": NOT root for extent 0x" + GuidTools.guid_to_string(ping.guid) + ". dropping req");
            return;
        }
        NodeId nodeId = new NodeId(my_node_id.port() + 1, my_node_id.address());
        StorageSetPong pong = new StorageSetPong(ping.peer, ping.guid, _rootsetMap, true, true, false);
        dispatch(pong);
    }

    /**********************************************************************/
    private Thunk1<BigInteger> host_db_timeout_cb = new Thunk1<BigInteger>() {

        public void run(BigInteger extent_key_sha1) {
            final String method_tag = tag + ".timeout_cb";
            ReqState state = (ReqState) _hostDbRequests.remove(extent_key_sha1);
            assert state != null : method_tag + ": state==null for extent_key=0x" + GuidTools.guid_to_string(extent_key_sha1);
            if (logger.isInfoEnabled()) logger.info(method_tag + ": extent_key=0x" + GuidTools.guid_to_string(extent_key_sha1) + " state=" + state);
            _send_resp(state, cr_status.STATUS_ERR_TIMEOUT, null);
        }
    };

    /**********************************************************************/
    private static final Set<BigInteger> ignore = new HashSet<BigInteger>();

    private boolean is_owner(BigInteger guid) {
        bamboo.router.Router router = bamboo.router.Router.instance(my_node_id);
        assert router != null : "no router registered for node_id " + my_node_id;
        boolean rv = false;
        bamboo.router.LeafSet leaf_set = router.leafSet();
        if (leaf_set != null && leaf_set.within_leaf_set(guid)) {
            bamboo.router.NeighborInfo result = leaf_set.closest_leaf(guid, ignore);
            rv = result.node_id.equals(my_node_id);
            if (rv) {
                assert result.guid.equals(_self_guid_sha1);
            }
        }
        return rv;
    }

    private boolean isCurrentReq(ReqState state, Object token, BigInteger host_extent_key_sha1, BigInteger ping_extent_key_sha1, String caller) {
        final String method_tag = caller + ".isCurrentReq";
        if (host_extent_key_sha1 != null || ping_extent_key_sha1 != null) {
            if (!_pings.containsKey(ping_extent_key_sha1) && !_hostDbRequests.containsKey(host_extent_key_sha1)) {
                logger.warn(method_tag + ": state is null for extent " + (ping_extent_key_sha1 == null ? "" : "0x" + GuidTools.guid_to_string(ping_extent_key_sha1)) + (ping_extent_key_sha1 == null || host_extent_key_sha1 == null ? "" : " and ") + (host_extent_key_sha1 == null ? "" : GuidTools.guid_to_string(host_extent_key_sha1)));
                return false;
            }
            if (state == null && host_extent_key_sha1 != null) state = _hostDbRequests.get(host_extent_key_sha1); else if (state == null && ping_extent_key_sha1 != null) state = _pings.get(ping_extent_key_sha1); else {
                assert state == _hostDbRequests.get(host_extent_key_sha1) || state == _pings.get(ping_extent_key_sha1) : method_tag + ": state=" + state + " != hostDb.get(" + (host_extent_key_sha1 == null ? "" : "0x" + GuidTools.guid_to_string(host_extent_key_sha1)) + ")=" + _hostDbRequests.get(host_extent_key_sha1) + " or ping.get(" + (ping_extent_key_sha1 == null ? "" : "0x" + GuidTools.guid_to_string(ping_extent_key_sha1)) + ")=" + _pings.get(ping_extent_key_sha1);
            }
        }
        if (token != null || state.token != null) {
            assert (token == null && state.token != null) || (token != null && state.token == null) || state.token == token : method_tag + ": state.token=" + state.token + "!= token=" + token;
            try {
                _acore.cancel_timer(token != null ? token : state.token);
            } catch (Throwable e) {
                logger.warn(method_tag + ": cancel_timer failed " + e + " for token=" + token + " state=" + state + ". Must have already timed out since entire operation" + " took " + ((now_us() - state.start_time_us) / 1000.0) + "ms.");
                RpcKey key = null;
                assert state == null || state.req == null || !_pendingRpcCalls.containsKey(key = new RpcKey(state.req.xact_id, state.req.peer)) : "pending rpc calls should be null since timeout for " + state;
                return false;
            }
            state.token = null;
        }
        RpcKey key = null;
        if (state == null || state.req == null || !_pendingRpcCalls.containsKey(key = new RpcKey(state.req.xact_id, state.req.peer))) {
            if (logger.isInfoEnabled()) logger.info(method_tag + " must have already finished with " + state);
            return false;
        }
        return true;
    }

    /** 
   * Simple class to store state required to process a 
   * {@link antiquity.cr.api.cr_create_ss_set_args cr_create_ss_set} 
   * request. */
    private class ReqState {

        /** The {@link antiquity.cr.api.cr_create_ss_set_args cr_create_ss_set}. */
        public RpcCall req;

        /** The {@link antiquity.cr.api.cr_guid client_id}. */
        public cr_guid client_id;

        /** The {@link java.math.BigInteger cient_id}. */
        public BigInteger client_id_sha1;

        /** The {@link antiquity.cr.api.cr_guid extent_key}. */
        public cr_guid extent_key;

        /** The {@link java.math.BigInteger extent_key}. */
        public BigInteger extent_key_sha1;

        /** The {@link antiquity.cr.api.cr_soundness_proof latest_proof}. */
        public cr_soundness_proof latest_proof;

        /** The {@link antiquity.cr.api.cr_signed_ss_set_config latest_config}.*/
        public cr_signed_ss_set_config latest_config;

        /** The {@link antiquity.cr.api.cr_ss_set_config proposed_config}.*/
        public cr_ss_set_config proposed_config;

        /** The {@link antiquity.cr.api.cr_signed_certificate cert}.*/
        public cr_signed_certificate cert;

        /** Number of request to the {@link dd.dissemination.SetCreator}. */
        public int attempts;

        /** timeout token for {@link bamboo.lss.ASyncCore}. */
        public Object token;

        /** start time (in us). */
        public long start_time_us;

        /** Constructor: Creates a new <code>ReqState</code>. */
        public ReqState(RpcCall req, cr_soundness_proof latest_proof) {
            this.req = req;
            this.latest_proof = latest_proof;
            switch(req.proc.getProcNum()) {
                case cr_get_latest_config_1:
                    client_id = ((cr_get_latest_config_args) req.args).client_id;
                    extent_key = ((cr_get_latest_config_args) req.args).extent_key;
                    break;
                case cr_renew_1:
                    cert = ((cr_renew_args) req.args).cert;
                    latest_config = ((cr_renew_args) req.args).latest_config;
                    client_id = latest_config.config.client_id;
                    extent_key = latest_config.config.extent_key;
                    proposed_config = ((cr_renew_args) req.args).proposed_config;
                    break;
                case cr_repair_1:
                    cert = latest_proof.cert;
                    latest_config = ((cr_repair_args) req.args).latest_config;
                    client_id = latest_config.config.client_id;
                    extent_key = latest_config.config.extent_key;
                    proposed_config = ((cr_repair_args) req.args).proposed_config;
                    break;
                case cr_create_ss_set_1:
                    cert = ((cr_create_ss_set_args) req.args).cert;
                    client_id = computeGuid(cert.cert.public_key, cr_guid.class, _md);
                    extent_key = (cert.cert.type == CR_KEY_VERIFIED ? client_id : cert.cert.verifier);
                    proposed_config = ((cr_create_ss_set_args) req.args).proposed_config;
                    break;
                default:
                    assert false : "unknown procedure call " + req;
                    break;
            }
            client_id_sha1 = guidToBigInteger(client_id);
            extent_key_sha1 = guidToBigInteger(extent_key);
            attempts = 0;
            token = null;
            start_time_us = now_us();
        }

        /** Specified by java.lang.Object */
        public String toString() {
            return new String("(ReqState " + req.args.getClass().getSimpleName() + " client_id=0x" + GuidTools.guid_to_string(client_id_sha1) + " extent=0x" + GuidTools.guid_to_string(extent_key_sha1) + " attempts=" + attempts + " xact_id=0x" + Integer.toHexString(req.xact_id) + " peer=" + req.peer + " elapsed time=" + ((now_us() - start_time_us) / 1000.0) + "ms)");
        }
    }
}
