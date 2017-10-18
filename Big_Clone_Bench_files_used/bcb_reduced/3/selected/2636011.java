package jxl.enclosure.security;

import java.security.CodeSigner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.naming.InvalidNameException;
import jxl.combination.CombinationMap;
import jxl.util.Base64;
import jxl.util.NamingUtils;

/**
 * This class hides the details of mapping and recovering permission policy
 * data from persistent store.
 * @author Alex Lynch
 */
public final class PolicyData {

    private static PolicyData one = null;

    /**
     * Get a singleton instance of PolicyData.
     * @throws java.lang.SecurityException if the caller does not pass
     * <CODE>jxl.enclosure.security.DynSecurityPolicy.checkEnclosure()</CODE>.
     */
    public static synchronized PolicyData getInstance() throws SecurityException {
        DynSecurityPolicy.checkEnclosure();
        if (one == null) {
            one = new PolicyData();
        }
        return one;
    }

    private final Map<CodeSigner, CombinationMap<Permission, PermissionState>> policies;

    private final MessageDigest digester;

    /** Creates a new instance of PolicyDataObject */
    private PolicyData() {
        try {
            policies = new CombinationMap<CodeSigner, CombinationMap<Permission, PermissionState>>(NamingUtils.asName(this.getClass()).add("codeSignerMap"), DynSecurityPolicy.ENCLOSURE_PERM);
            digester = MessageDigest.getInstance("MD5");
        } catch (InvalidNameException ine) {
            throw new RuntimeException(ine);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }

    public void clearTestingPolicies() {
        DynSecurityPolicy.checkEnclosure();
        policies.remove(null);
    }

    private Map<Permission, PermissionState> getPolicies(CodeSigner signer) {
        CodeSigner sig = null;
        for (CodeSigner s : policies.keySet()) {
            if (s == signer || s.equals(signer)) {
                sig = s;
            }
        }
        CombinationMap<Permission, PermissionState> perm = policies.get(sig);
        if (perm == null) {
            try {
                String name = Base64.encodeBytes(digester.digest((signer == null ? "null" : signer.toString()).getBytes()));
                perm = new CombinationMap<Permission, PermissionState>(NamingUtils.asName(this.getClass()).add(name), DynSecurityPolicy.ENCLOSURE_PERM);
                policies.put(signer, perm);
            } catch (InvalidNameException ine) {
                throw new RuntimeException(ine);
            }
        }
        return perm;
    }

    /**
     * Grant <CODE>p</CODE> to <CODE>signer</CODE>
     */
    public void grant(CodeSigner signer, Permission p) {
        setState(signer, p, PermissionState.GRANTED);
    }

    /**
     * Deny <CODE>p</CODE> to <CODE>signer</CODE>
     */
    public void deny(CodeSigner signer, Permission p) {
        setState(signer, p, PermissionState.DENIED);
    }

    /**
     * Is <CODE>p</CODE> granted to <CODE>signer</CODE>
     */
    public boolean isGranted(CodeSigner signer, Permission p) {
        return getState(signer, p).equals(PermissionState.GRANTED);
    }

    /**
     * Is <CODE>p</CODE> denied to <CODE>signer</CODE>
     */
    public boolean isDenied(CodeSigner signer, Permission p) {
        return getState(signer, p).equals(PermissionState.DENIED);
    }

    /**
     * Get state of permissoin <CODE>p</CODE> for <CODE>signer</CODE>
     */
    public PermissionState getState(CodeSigner signer, Permission p) {
        DynSecurityPolicy.checkEnclosure();
        PermissionState s = getPolicies(signer).get(p);
        if (s == null) {
            return PermissionState.UNDEFINED;
        } else {
            return s;
        }
    }

    /**
     * Get an unmodifiable view of the security policy data
     */
    public Map<CodeSigner, Map<Permission, PermissionState>> getPolicies() {
        DynSecurityPolicy.checkEnclosure();
        Map<CodeSigner, Map<Permission, PermissionState>> m = new HashMap<CodeSigner, Map<Permission, PermissionState>>();
        for (CodeSigner c : policies.keySet()) {
            m.put(c, Collections.unmodifiableMap(policies.get(c)));
        }
        return Collections.unmodifiableMap(m);
    }

    /**
     * Set the state of permissoin <CODE>p</CODE> for <CODE>signer</CODE>
     */
    public void setState(CodeSigner signer, Permission p, PermissionState state) {
        DynSecurityPolicy.checkEnclosure();
        Map<Permission, PermissionState> map = getPolicies(signer);
        map.put(p, state);
    }
}
