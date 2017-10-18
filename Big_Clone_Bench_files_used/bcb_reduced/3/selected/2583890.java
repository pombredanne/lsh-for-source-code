package antiquity.gw.impl;

import antiquity.util.AntiquityUtils;
import static antiquity.util.AntiquityUtils.getSSIndex;
import static antiquity.util.AntiquityUtils.guidToBigInteger;
import static antiquity.util.AntiquityUtils.createProofFromLog;
import static antiquity.util.AntiquityUtils.byteArrayToBigInteger;
import antiquity.util.XdrUtils;
import antiquity.util.KeyTriple;
import antiquity.util.ValuePair;
import antiquity.util.ValueTriple;
import antiquity.gw.api.gw_guid;
import antiquity.gw.api.gw_log_entry;
import antiquity.gw.api.gw_repair_args;
import antiquity.gw.api.gw_soundness_proof;
import antiquity.gw.api.gw_signed_signature;
import antiquity.gw.api.gw_signed_ss_set_config;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.SortedMap;
import java.math.BigInteger;
import java.security.MessageDigest;
import org.apache.log4j.Logger;

/**
 * <code>RepairProofState</code>
 *
 * @author Hakim Weatherspoon
 * @version $Id: RepairProofState.java,v 1.1.1.1 2006/12/15 22:45:38 hweather Exp $
 **/
public class RepairProofState {

    /** {@link java.security.MessageDigest}. */
    private MessageDigest _md;

    /** {@link org.apache.log4j.Logger}. */
    private Logger _logger;

    /** {@link antiquity.gw.api.gw_guid extent_key} of RepairProofState. */
    public gw_guid extent_key;

    /**
   * {@link java.util.Map} associating <code>latest config key</code> 
   * (config.seq_num, config.timestamp, and H(config)) to 
   * {@link antiquity.gw.api.gw_signed_ss_set_config}. */
    private SortedMap<KeyTriple<Long, Long, BigInteger>, gw_signed_ss_set_config> _latestConfigMap;

    /**
   * {@link java.util.Map} associating <code>latest config key</code> 
   * (config.seq_num, config.timestamp, and H(config)) to 
   * {@link java.util.Map} of repair
   * {@link antiquity.gw.api.gw_signed_signature} and latest
   * {@link antiquity.gw.api.gw_soundness_proof}. */
    private SortedMap<KeyTriple<Long, Long, BigInteger>, Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>>> _repairSignatureMap;

    /**
   * {@link java.util.Map} associating <code>version key</code> 
   * (cert.seq_num, config.seq_num, and H(cert|config)) to 
   * {@link antiquity.gw.api.gw_soundness_proof}. */
    private SortedMap<KeyTriple<Long, Long, BigInteger>, gw_soundness_proof> _versionMapProof;

    /**
   * {@link java.util.Map} associating <code>version key</code> 
   * (cert.seq_num, config.seq_num, and H(cert|config)) to 
   * {@link java.util.Collection} of 
   * {@link antiquity.gw.api.gw_log_entry}. */
    private SortedMap<KeyTriple<Long, Long, BigInteger>, Map<BigInteger, gw_log_entry>> _versionMapLog;

    /** Constructor: Creates a new <code>RepairProofState</code>. */
    public RepairProofState(gw_guid extent_key, MessageDigest md, Logger logger) {
        this.extent_key = extent_key;
        this._md = md;
        this._logger = logger;
    }

    /**
   * <code>isRepairProofExist</code> returns true if a repair proof
   * exists; otherwise, false. 
   * @return true if a repair proof exists; otherwise, false. */
    public boolean isRepairProofExist() {
        boolean rv = false;
        if (_repairSignatureMap != null && !_repairSignatureMap.isEmpty()) {
            gw_signed_ss_set_config latest_config = getLatestConfig();
            Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>> repairSignatureMap = getLatestRepairSignatures();
            assert (latest_config.config.threshold - 1) % 2 == 0 : "low_watermark=" + latest_config.config.threshold + " is not an odd number";
            int f = (latest_config.config.threshold - 1) / 2;
            rv = (repairSignatureMap.size() >= (f + 1));
            rv = (latest_config.config.seq_num >= _versionMapProof.lastKey().getSecond() && rv);
        }
        return rv;
    }

    /**
   * <code>getRepairProof</code> returns the 
   * {@link antiquity.gw.api.gw_signed_ss_set_config latest_config},
   *  the {@link antiquity.gw.api.gw_soundness_proof latest_proof}, and
   *  {@link java.util.Map} of
   *  {@link antiquity.gw.api.gw_signed_signature repair_signatures} from
   * storage servers contained in the 
   * {@link antiquity.gw.api.gw_signed_ss_set_config latest_config}. 
   *
   * @return the 
   *    {@link antiquity.gw.api.gw_signed_ss_set_config latest_config},
   *    the {@link antiquity.gw.api.gw_soundness_proof latest_proof}, and
   *    {@link java.util.Map} of
   *    {@link antiquity.gw.api.gw_signed_signature repair_signatures} from
   *    storage servers contained in the 
   *    {@link antiquity.gw.api.gw_signed_ss_set_config latest_config}. */
    public ValueTriple<gw_signed_ss_set_config, gw_soundness_proof, Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>>> getRepairProof() {
        assert isRepairProofExist() : "repair proof does not exist.";
        gw_signed_ss_set_config latest_config = getLatestConfig();
        gw_soundness_proof latest_proof = getLatestProof();
        Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>> repairSignatureMap = getLatestRepairSignatures();
        ValueTriple<gw_signed_ss_set_config, gw_soundness_proof, Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>>> rv = null;
        rv = new ValueTriple<gw_signed_ss_set_config, gw_soundness_proof, Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>>>(latest_config, latest_proof, repairSignatureMap);
        return rv;
    }

    /**
   * <code>getLatestRepairSignatures</code> returns the 
   * {@link antiquity.gw.api.gw_soundness_proof latest_proof}.
   *
   * @return the 
   * {@link antiquity.gw.api.gw_soundness_proof latest_proof}. */
    public Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>> getLatestRepairSignatures() {
        KeyTriple<Long, Long, BigInteger> latestConfigKey = (_latestConfigMap != null ? _latestConfigMap.lastKey() : null);
        Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>> repairSignatureMap = (latestConfigKey != null ? _repairSignatureMap.get(latestConfigKey) : null);
        return repairSignatureMap;
    }

    /**
   * <code>getLatestProof</code> returns the 
   * {@link antiquity.gw.api.gw_soundness_proof latest_proof}.
   *
   * @return the 
   * {@link antiquity.gw.api.gw_soundness_proof latest_proof}. */
    public gw_soundness_proof getLatestProof() {
        gw_signed_ss_set_config latest_config = getLatestConfig();
        if (_versionMapLog != null) {
            for (Map<BigInteger, gw_log_entry> logEntries : _versionMapLog.values()) {
                if (logEntries.size() >= latest_config.config.threshold) {
                    gw_soundness_proof proof = createProofFromLog(extent_key, logEntries.values(), gw_soundness_proof.class, _md);
                    if (proof != null) {
                        _md.update(XdrUtils.serialize(proof.cert.cert));
                        _md.update(XdrUtils.serialize(proof.config.config));
                        BigInteger hash = byteArrayToBigInteger(_md.digest());
                        KeyTriple<Long, Long, BigInteger> proofKey = new KeyTriple<Long, Long, BigInteger>(proof.config.config.seq_num, proof.cert.cert.seq_num, hash);
                        if (!_versionMapProof.containsKey(proofKey)) _versionMapProof.put(proofKey, proof);
                    }
                }
            }
        }
        KeyTriple<Long, Long, BigInteger> latestProofKey = (_versionMapProof != null ? _versionMapProof.lastKey() : null);
        gw_soundness_proof latest_proof = (latestProofKey != null ? _versionMapProof.get(latestProofKey) : null);
        return latest_proof;
    }

    /**
   * <code>getLatestConfig</code> returns the 
   * {@link antiquity.gw.api.gw_signed_ss_set_config latest_config}.
   *
   * @return the 
   * {@link antiquity.gw.api.gw_signed_ss_set_config latest_config}. */
    public gw_signed_ss_set_config getLatestConfig() {
        KeyTriple<Long, Long, BigInteger> latestConfigKey = (_latestConfigMap != null ? _latestConfigMap.lastKey() : null);
        gw_signed_ss_set_config latest_config = (latestConfigKey != null ? _latestConfigMap.get(latestConfigKey) : null);
        return latest_config;
    }

    /** 
   * <code>add</code> {@link antiquity.gw.api.gw_repair_args} to 
   * internal <code>RepairProofState</code>. 
   *
   * @param repair {@link antiquity.gw.api.gw_repair_args}
   * @return <code>true</code> if any fields added; otherwise, 
   *   <code>false</code>. */
    public boolean add(gw_repair_args repair) {
        boolean rv = false;
        rv = (addRepairProofSig(repair) || rv);
        rv = (addProof(repair.proof) || rv);
        rv = (addLog(repair.log) || rv);
        return rv;
    }

    /** 
   * <code>addRepairProofSig</code> adds
   * {@link antiquity.gw.api.gw_soundness_proof repair.proof},
   * {@link antiquity.gw.api.gw_signed_ss_set_config repair.latest_config},
   * and {@link antiquity.gw.api.gw_signed_sig repair.sig},  to 
   * internal <code>RepairProofState</code> in a consistent fashion. 
   *
   * @param repair {@link antiquity.gw.api.gw_repair_args}
   * @return <code>true</code> if added; otherwise, 
   *   <code>false</code>. */
    private boolean addRepairProofSig(gw_repair_args repair) {
        _md.update(XdrUtils.serialize(repair.latest_config.config));
        BigInteger hash = byteArrayToBigInteger(_md.digest());
        KeyTriple<Long, Long, BigInteger> latestConfigKey = new KeyTriple<Long, Long, BigInteger>(repair.latest_config.config.seq_num, repair.latest_config.config.timestamp, hash);
        if (_latestConfigMap == null) {
            _latestConfigMap = new TreeMap<KeyTriple<Long, Long, BigInteger>, gw_signed_ss_set_config>();
            _repairSignatureMap = new TreeMap<KeyTriple<Long, Long, BigInteger>, Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>>>();
        }
        gw_signed_ss_set_config latest_config = _latestConfigMap.get(latestConfigKey);
        Map<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>> repairSignatureMap = _repairSignatureMap.get(latestConfigKey);
        if (latest_config == null) {
            latest_config = repair.latest_config;
            _latestConfigMap.put(latestConfigKey, latest_config);
            repairSignatureMap = new HashMap<BigInteger, ValuePair<gw_soundness_proof, gw_signed_signature>>();
            _repairSignatureMap.put(latestConfigKey, repairSignatureMap);
        }
        assert XdrUtils.equals(latest_config, repair.latest_config) : "latest_config=" + XdrUtils.toString(latest_config) + " != repair.latest_config=" + XdrUtils.toString(repair.latest_config);
        _md.update(repair.sig.public_key.value);
        gw_guid sig_guid = new gw_guid(_md.digest());
        BigInteger sig_guid_sha1 = guidToBigInteger(sig_guid);
        assert getSSIndex(sig_guid, latest_config.config.ss_set, true) >= 0 : "H(sig.pk)=" + XdrUtils.toString(sig_guid) + " not contained in latest_config=" + XdrUtils.toString(latest_config);
        ValuePair<gw_soundness_proof, gw_signed_signature> repairSignature = repairSignatureMap.get(sig_guid_sha1);
        if (repairSignature == null) {
            repairSignature = new ValuePair<gw_soundness_proof, gw_signed_signature>(repair.proof, repair.sig);
            repairSignatureMap.put(sig_guid_sha1, repairSignature);
        } else {
            assert XdrUtils.equals(repair.proof, repairSignature.getFirst()) : "repair.proof=" + XdrUtils.toString(repair.proof) + " != repairSignature.proof=" + XdrUtils.toString(repairSignature.getFirst());
            assert XdrUtils.equals(repair.sig, repairSignature.getSecond()) : "repair.sig=" + XdrUtils.toString(repair.sig) + " != repairSignature.sig=" + XdrUtils.toString(repairSignature.getSecond());
        }
        return true;
    }

    /** 
   * <code>addProof</code> {@link antiquity.gw.api.gw_soundness_proof} to 
   * internal <code>RepairProofState</code>. 
   *
   * @param proof {@link antiquity.gw.api.gw_soundness_proof}
   * @return <code>true</code> if {@link antiquity.gw.api.gw_soundness_proof}
   *    added; otherwise, <code>false</code>. */
    public boolean addProof(gw_soundness_proof proof) {
        if (proof == null || !XdrUtils.equals(extent_key, proof.config.config.extent_key)) {
            if (_logger.isInfoEnabled()) _logger.info("RepairProofState.addProof: Extent key=" + XdrUtils.toString(extent_key) + " does not match repair_args.proof.config.extent_key=" + (proof == null ? null : XdrUtils.toString(proof.config.config.extent_key)));
            return false;
        }
        _md.update(XdrUtils.serialize(proof.cert.cert));
        _md.update(XdrUtils.serialize(proof.config.config));
        BigInteger hash = byteArrayToBigInteger(_md.digest());
        KeyTriple<Long, Long, BigInteger> versionKey = new KeyTriple<Long, Long, BigInteger>(proof.config.config.seq_num, proof.cert.cert.seq_num, hash);
        if (_versionMapProof == null) _versionMapProof = new TreeMap<KeyTriple<Long, Long, BigInteger>, gw_soundness_proof>();
        if (!_versionMapProof.containsKey(versionKey)) _versionMapProof.put(versionKey, proof);
        return true;
    }

    /** 
   * <code>addLog</code> {@link antiquity.gw.api.gw_log_entry} to 
   * internal <code>RepairProofState</code>. 
   *
   * @param logs Array of {@link antiquity.gw.api.gw_log_entry}
   * @return <code>true</code> if any {@link antiquity.gw.api.gw_log_entry} 
   *   added; otherwise, <code>false</code>. */
    public boolean addLog(gw_log_entry[] logs) {
        boolean rv = false;
        for (gw_log_entry log : logs) rv = (addLog(log) || rv);
        return rv;
    }

    /** 
   * <code>addLog</code> {@link antiquity.gw.api.gw_log_entry} to 
   * internal <code>RepairProofState</code>. 
   *
   * @param log {@link antiquity.gw.api.gw_log_entry}
   * @return <code>true</code> if {@link antiquity.gw.api.gw_log_entry} 
   *   added; otherwise, <code>false</code>. */
    public boolean addLog(gw_log_entry log) {
        if (log == null || !XdrUtils.equals(extent_key, log.config.config.extent_key)) {
            if (_logger.isInfoEnabled()) _logger.info("RepairProofState.addLog: Extent key=" + XdrUtils.toString(extent_key) + " does not match log.config.extent_key=" + (log == null ? null : XdrUtils.toString(log.config.config.extent_key)));
            return false;
        }
        if (_versionMapProof != null && log.cert.cert.seq_num < _versionMapProof.lastKey().getFirst() && log.config.config.seq_num < _versionMapProof.lastKey().getSecond()) return false;
        _md.update(XdrUtils.serialize(log.cert.cert));
        _md.update(XdrUtils.serialize(log.config.config));
        BigInteger hash = byteArrayToBigInteger(_md.digest());
        KeyTriple<Long, Long, BigInteger> versionKey = new KeyTriple<Long, Long, BigInteger>(log.config.config.seq_num, log.cert.cert.seq_num, hash);
        if (_versionMapLog == null) _versionMapLog = new TreeMap<KeyTriple<Long, Long, BigInteger>, Map<BigInteger, gw_log_entry>>();
        Map<BigInteger, gw_log_entry> logEntryMap = _versionMapLog.get(versionKey);
        if (logEntryMap == null) {
            logEntryMap = new HashMap<BigInteger, gw_log_entry>();
            _versionMapLog.put(versionKey, logEntryMap);
        }
        _md.update(log.sig.public_key.value);
        gw_guid sig_guid = new gw_guid(_md.digest());
        BigInteger sig_guid_sha1 = guidToBigInteger(sig_guid);
        assert getSSIndex(sig_guid, log.config.config.ss_set, true) >= 0 : "H(log.sig.pk)=" + XdrUtils.toString(sig_guid) + " not contained in log.config=" + XdrUtils.toString(log.config);
        gw_log_entry logEntry = logEntryMap.get(sig_guid_sha1);
        if (logEntry == null) {
            logEntryMap.put(sig_guid_sha1, log);
        } else {
            assert XdrUtils.equals(log, logEntry) : "log=" + XdrUtils.toString(log) + " != logEntry=" + XdrUtils.toString(logEntry);
        }
        return true;
    }

    public void clear() {
        if (_repairSignatureMap != null) _repairSignatureMap.clear();
        if (_latestConfigMap != null) _latestConfigMap.clear();
        if (_versionMapProof != null) _versionMapProof.clear();
        if (_versionMapLog != null) _versionMapLog.clear();
    }

    /** Specified by java.lang.Object */
    public void finalize() {
        clear();
        _repairSignatureMap = null;
        _latestConfigMap = null;
        _versionMapProof = null;
        _versionMapLog = null;
    }

    /** Specified by java.lang.Object */
    public String toString() {
        String str = new String("(RepairProofState");
        str += " extent_key=0x" + XdrUtils.toString(extent_key);
        if (_latestConfigMap != null) {
            str += " latestConfig.len=" + _latestConfigMap.size() + " latestConfigKeys=" + _latestConfigMap.keySet();
        }
        if (_repairSignatureMap != null) {
            str += " repairSig.len=" + _repairSignatureMap.size() + " repairSig=" + _repairSignatureMap.keySet();
        }
        if (_versionMapProof != null) {
            str += " proof.len=" + _versionMapProof.size() + " proofKeys=" + _versionMapProof.keySet();
        }
        if (_versionMapLog != null) {
            str += " log.len=" + _versionMapLog.size() + " logKeys=" + _versionMapLog.keySet();
        }
        str += ")";
        return str;
    }
}
